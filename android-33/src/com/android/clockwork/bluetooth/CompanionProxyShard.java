package com.android.clockwork.bluetooth;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.WorkerThread;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.android.clockwork.bluetooth.proxy.ProxyNetworkAgent;
import com.android.clockwork.bluetooth.proxy.ProxyServiceHelper;
import com.android.clockwork.bluetooth.proxy.ProxyServiceVersion;
import com.android.clockwork.bluetooth.proxy.WearProxyConstants.Reason;
import com.android.clockwork.common.DebugAssert;
import com.android.clockwork.common.LogUtil;
import com.android.clockwork.common.Util;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;

/**
 * Manages connection to the companion sysproxy network
 *
 * This class handles connecting to the remote device using the
 * bluetooth network and configuring the sysproxy to setup the
 * proper network to allow IP traffic to be utilized by Android.
 *
 * Steps to connect to the companion sysproxy.
 *
 * 1. Get a bluetooth rfcomm socket.
 *      This will actually establish a bluetooth connection from the device to the companion.
 * 2. Pass this rfcomm socket to the sysproxy module.
 *      The sysproxy module will formulate the necessary network configuration to allow
 *      IP traffic to flow over the bluetooth socket connection.
 * 3. Get acknowledgement that the sysproxy module initialized.
 *      This may or may not be completed successfully as indicated by the jni callback
 *      indicating connection or failure.
 */
public class CompanionProxyShard {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;
    private static final int WHAT_START_SYSPROXY = 1;
    private static final int WHAT_JNI_ACTIVE_NETWORK_STATE = 2;
    private static final int WHAT_JNI_DISCONNECTED = 3;
    private static final int WHAT_RESET_CONNECTION = 4;

    private static final int TYPE_RFCOMM = 1;
    private static final int TYPE_L2CAP_LE = 4;
    private static final int SEC_FLAG_ENCRYPT = 1 << 0;
    private static final int SEC_FLAG_AUTH = 1 << 1;
    // Relative unitless network retry values
    private static final int BACKOFF_BASE_INTERVAL = 2;
    private static final int BACKOFF_BASE_PERIOD = 5;
    private static final int BACKOFF_MAX_INTERVAL = 300;

    private static final int IS_NOT_METERED = 0;
    private static final int IS_METERED = 1;
    private static final boolean IS_CONNECTED = true;
    private static final boolean IS_DISCONNECTED = !IS_CONNECTED;
    private static final boolean PHONE_WITH_INTERNET = true;
    private static final boolean PHONE_NO_INTERNET = !PHONE_WITH_INTERNET;

    private static final int CONNECT_RESULT_CONNECTED = 0;
    private static final int CONNECT_RESULT_TIMEOUT = 1;
    private static final int CONNECT_RESULT_FAILED = -1;

    static native void classInitNative();
    @VisibleForTesting native int connectNative(int fd, int sysproxyVersion);
    @VisibleForTesting native int continueConnectNative();
    @VisibleForTesting native boolean disconnectNative();

    static {
        try {
            System.loadLibrary("wear-bluetooth-jni");
            classInitNative();
        } catch (UnsatisfiedLinkError e) {
            // Invoked during testing
            Log.e(TAG, "Unable to load wear bluetooth sysproxy jni native"
                    + " libraries");
        }
    }

    @VisibleForTesting int mStartAttempts;
    /**
     * Current active value of {@link ConnectivityManager#TYPE} from
     * {@link NetworkInfo#getType} on phone.
     **/
    @interface NetworkType { }
    @NetworkType
    @VisibleForTesting int mNetworkType;

    private boolean mIsMetered;
    private boolean mIsConnected;
    private boolean mPhoneNoInternet;
    private int mConnectionPort;
    private ProxyServiceVersion mSysproxyVersion =
            ProxyServiceVersion.DEFAULT_PROXY_SERVICE_VERSION;

    private enum ClientState {IDLE, CONNECTING, CONNECTED, DISCONNECTING}
    private ClientState clientState = ClientState.IDLE;

    /** The sysproxy network state
     *
     * The sysproxy network may be in one of the following states:
     *
     * 1. Disconnected from phone.
     *      There is no bluetooth rfcomm socket connected to the phone.
     * 2. Connected with Internet access via phone.
     *      There is a valid rfcomm socket connected to phone and the phone has a default
     *      network that had validated access to the Internet.
     * 3. Connected without Internet access via phone.
     *      There is a vaid rfcomm socket connected to phone but the phone has no validated
     *      network to the Internet.
     */
    private enum SysproxyNetworkState {
        DISCONNECTED,
        CONNECTED_NO_INTERNET,
        CONNECTED_WITH_INTERNET,
    }

    @NonNull private final Context mContext;
    @NonNull private final ProxyServiceHelper mProxyServiceHelper;
    @NonNull private final CompanionTracker mCompanionTracker;
    private Listener mListener;

    private final MultistageExponentialBackoff mReconnectBackoff;
    @VisibleForTesting boolean mIsClosed;

    private CompanionUuidReceiver mCompanionUuidReceiver;

    public interface Listener {
        /**
         * Callback executed when the sysproxy connection changes state.
         *
         * This may send duplicate disconnect events, because failed reconnect
         * attempts are indistinguishable from actual disconnects.
         * Listeners should appropriately deduplicate these disconnect events.
         */
        void onProxyConnectionChange(boolean isConnected, int proxyScore, boolean phoneNoInternet);

        /**
         * Callback executed when the sysproxy has incoming or outgoing data on the BLE L2CAP
         * socket.
         */
        void onProxyBleData();
    }

    public CompanionProxyShard(
            @NonNull final Context context,
            @NonNull final ProxyServiceHelper proxyServiceHelper,
            @NonNull final CompanionTracker companionTracker) {
        DebugAssert.isMainThread();

        mContext = context;
        mProxyServiceHelper = proxyServiceHelper;
        mCompanionTracker = companionTracker;

        mReconnectBackoff = new MultistageExponentialBackoff(BACKOFF_BASE_INTERVAL,
                BACKOFF_BASE_PERIOD, BACKOFF_MAX_INTERVAL);

        maybeLogDebug("Created companion proxy shard");

    }

    /**
     * Completely shuts down companion proxy network.
     *
     * Disconnects sysproxy client, effectively closing BT socket and detach current listener from
     * sysproxy network updates.
     */
    @MainThread
    public void stop() {
        DebugAssert.isMainThread();
        if (mIsClosed) {
            Log.w(TAG, logInstance() + "Already closed");
            return;
        }
        removeCompanionUuidReceiver();
        mReconnectBackoff.reset();
        updateAndNotify(SysproxyNetworkState.DISCONNECTED, Reason.CLOSABLE);
        // notify mListener of our intended disconnect before setting mIsClosed to true
        mIsClosed = true;
        disconnectNativeInBackground();
        mHandler.removeMessages(WHAT_START_SYSPROXY);
        maybeLogDebug("Closed companion proxy shard");
        mListener = null;
    }

    /**
     * Start sysproxy network connecting to RFCOMM socket and sysproxy native service.
     *
     * While sysproxy is connected {@code listener} is notified about underling network
     * status changes.
     */
    @MainThread
    public void startNetwork(
            int networkScore, List<InetAddress> dnsServers, int connectionPort, Listener listener) {
        DebugAssert.isMainThread();
        LogUtil.logD(TAG, "startNetwork(%s, %s)", networkScore, connectionPort);

        if (connectionPort != mConnectionPort) {
            stop();
            mConnectionPort = connectionPort;
        }

        updateSysproxyVersion();

        mIsClosed = false;

        // TODO(b/161549960): move to list of listeners, add/remove listener API, as we refactor
        // and directly use this class from mediator
        if (mListener != null && mListener != listener) {
          Log.e(TAG, "Replacing existing NON-NULL listener");
        }

        mListener = listener;

        mProxyServiceHelper.setCompanionName(mCompanionTracker.getCompanionName());
        mProxyServiceHelper.setDnsServers(dnsServers);
        mProxyServiceHelper.setNetworkScore(networkScore);

        mHandler.sendEmptyMessage(WHAT_START_SYSPROXY);
    }

    @MainThread
    private void updateSysproxyVersion() {
        BluetoothDevice companion = mCompanionTracker.getCompanion();
        if (companion == null || !mSysproxyVersion.isUpgradeAvailable(mContext)) {
            return;
        }

        mSysproxyVersion = ProxyServiceVersion.detectVersion(mContext, companion);

        if (mCompanionUuidReceiver == null && mSysproxyVersion.isUpgradeAvailable(mContext)
                && companion.fetchUuidsWithSdp()) {
            Log.d(TAG, "[ProxyShard] started SDP");
            mCompanionUuidReceiver = new CompanionUuidReceiver();
            mContext.registerReceiver(mCompanionUuidReceiver,
                    new IntentFilter(BluetoothDevice.ACTION_UUID));
        }
    }

    @MainThread
    private void removeCompanionUuidReceiver() {
        CompanionUuidReceiver receiver = mCompanionUuidReceiver;
        mCompanionUuidReceiver = null;
        if (receiver != null) {
            mContext.unregisterReceiver(receiver);
        }
    }

    @MainThread
    public void updateNetwork(final int networkScore) {
        DebugAssert.isMainThread();
        mProxyServiceHelper.setNetworkScore(networkScore);
        notifyConnectionChange(mIsConnected, mPhoneNoInternet);
    }

    /** Update DnsServers used by proxy. */
    @MainThread
    public void updateNetwork(final List<InetAddress> dnsServers) {
        DebugAssert.isMainThread();
        mProxyServiceHelper.setDnsServers(dnsServers);
    }

    /** Serialize state change requests here */
    @VisibleForTesting
    final Handler mHandler = new Handler() {
        @MainThread
        @Override
        public void handleMessage(Message msg) {
            DebugAssert.isMainThread();
            switch (msg.what) {
                case WHAT_START_SYSPROXY:
                    mHandler.removeMessages(WHAT_START_SYSPROXY);
                    if (mIsClosed) {
                        maybeLogDebug("start sysproxy but shard closed...will bail");
                        return;
                    } else if (clientState == ClientState.DISCONNECTING) {
                        maybeLogDebug("waiting for sysproxy to disconnect...will retry");
                        mHandler.sendEmptyMessage(WHAT_RESET_CONNECTION);
                        return;
                    }
                    mStartAttempts++;
                    if (connectedWithInternet()) {
                        maybeLogDebug("start sysproxy already running set connected");
                        updateAndNotify(SysproxyNetworkState.CONNECTED_WITH_INTERNET,
                                Reason.SYSPROXY_WAS_CONNECTED);
                    } else if (connectedNoInternet()) {
                        maybeLogDebug("start sysproxy already running but with no internet access");
                        updateAndNotify(SysproxyNetworkState.CONNECTED_NO_INTERNET,
                                Reason.SYSPROXY_NO_INTERNET);
                    } else {
                        maybeLogDebug("start up new sysproxy connection");
                        connectSysproxyInBackground();
                    }
                    break;
                case WHAT_JNI_ACTIVE_NETWORK_STATE:
                    mNetworkType = msg.arg1;
                    mIsMetered = msg.arg2 == IS_METERED;
                    updateClientState(ClientState.CONNECTED);
                    mReconnectBackoff.reset();

                    if (mIsClosed) {
                        maybeLogDebug("JNI onActiveNetworkState shard closed...will bail");
                        return;
                    }

                    if (connectedWithInternet()) {
                        updateAndNotify(SysproxyNetworkState.CONNECTED_WITH_INTERNET,
                                Reason.SYSPROXY_CONNECTED);
                        mProxyServiceHelper.setMetered(mIsMetered);
                    } else if (connectedNoInternet()) {
                        updateAndNotify(SysproxyNetworkState.CONNECTED_NO_INTERNET,
                                Reason.SYSPROXY_NO_INTERNET);
                    }
                    maybeLogDebug("JNI sysproxy process complete networkType:" + mNetworkType
                            + " metered:" + mIsMetered);
                    break;
                case WHAT_JNI_DISCONNECTED:
                    final int status = msg.arg1;
                    updateClientState(ClientState.IDLE);
                    maybeLogDebug("JNI onDisconnect isClosed:" + mIsClosed + " status:" + status);
                    updateAndNotify(SysproxyNetworkState.DISCONNECTED,
                            Reason.SYSPROXY_DISCONNECTED);
                    setUpRetryIfNotClosed();
                    break;
                case WHAT_RESET_CONNECTION:
                    // Take a hammer to reset everything on sysproxy side to initial state.
                    maybeLogDebug("Reset companion proxy network connection isClosed:" + mIsClosed);
                    mHandler.removeMessages(WHAT_START_SYSPROXY);
                    mHandler.removeMessages(WHAT_RESET_CONNECTION);
                    disconnectNativeInBackground();
                    setUpRetryIfNotClosed();
                    break;
            }
        }
    };

    private void setUpRetryIfNotClosed() {
        if (!mIsClosed) {
            final int nextRetry = mReconnectBackoff.getNextBackoff();
            mHandler.sendEmptyMessageDelayed(WHAT_START_SYSPROXY, nextRetry * 1000);
            Log.w(TAG, logInstance() + "Attempting reconnect in " + nextRetry + " seconds");
        }
    }

    @MainThread
    private void updateAndNotify(final SysproxyNetworkState state, final String reason) {
        DebugAssert.isMainThread();
        if (state == SysproxyNetworkState.CONNECTED_WITH_INTERNET) {
            mProxyServiceHelper.startNetworkSession(reason,
                    /**
                     * Called when the current network agent is no longer wanted
                     * by {@link ConnectivityService}.  Try to restart the network.
                     */
                    new ProxyNetworkAgent.Listener() {
                        @Override
                        public void onNetworkAgentUnwanted(int netId) {
                            Log.d(TAG, "Network agent unwanted netId:" + netId);
                            mHandler.sendEmptyMessage(WHAT_START_SYSPROXY);
                        }
                    });
            notifyConnectionChange(IS_CONNECTED, PHONE_WITH_INTERNET);
        } else if (state == SysproxyNetworkState.CONNECTED_NO_INTERNET) {
            mProxyServiceHelper.stopNetworkSession(reason);
            notifyConnectionChange(IS_CONNECTED, PHONE_NO_INTERNET);
        } else {
            mProxyServiceHelper.stopNetworkSession(reason);
            notifyConnectionChange(IS_DISCONNECTED);
        }
    }


    /**
     * Request an rfcomm or l2cap socket to the companion device.
     *
     * Connect to the companion device with a bluetooth rfcomm or l2cap socket.
     * The integer filedescriptor portion of the {@link ParcelFileDescriptor}
     * is used to pass to the sysproxy native code via
     * {@link CompanionProxyShard#connectNativeInBackground}
     *
     * Failures in any of these steps enter into a delayed retry mode.
     */
    @MainThread
    private void connectSysproxyInBackground() {
        DebugAssert.isMainThread();

        if (clientState != ClientState.IDLE) {
            return;
        }
        updateClientState(ClientState.CONNECTING);

        maybeLogDebug("Retrieving bluetooth network socket");

        new DefaultPriorityAsyncTask<Void, Void, ParcelFileDescriptor>() {
            @Override
            protected ParcelFileDescriptor doInBackgroundDefaultPriority() {
                // TODO(218975993): redesign with non-hidden APIs
                return null;
            }

            @Override
            protected void onPostExecute(@Nullable ParcelFileDescriptor parcelFd) {
                DebugAssert.isMainThread();

                if (mIsClosed) {
                    maybeLogDebug("Shard closed after retrieving bluetooth socket");
                    Util.close(parcelFd);
                    updateClientState(ClientState.IDLE);
                    return;
                } else if (parcelFd != null) {
                    final int fd = parcelFd.detachFd();
                    maybeLogDebug("Retrieved bluetooth network socket parcelFd:" + parcelFd
                            + " fd:" + fd);
                    connectNativeInBackground(fd);
                } else {
                    Log.e(TAG, logInstance() + "Unable to request bluetooth network socket");
                    updateClientState(ClientState.IDLE);
                    mHandler.sendEmptyMessage(WHAT_RESET_CONNECTION);
                }
                Util.close(parcelFd);
            }
        }.execute();
    }

    @VisibleForTesting
    protected BluetoothSocketMonitor createBluetoothSocketMonitor(
           BluetoothSocketMonitor.Listener listener) {
        return new BluetoothSocketMonitor(listener);
    }

    /**
     * Pass connected socket to sysproxy module.
     *
     * Hand off a connected socket to the native sysproxy code to
     * provide bidirectional network connectivity for the system.
     */
    @MainThread
    private void connectNativeInBackground(Integer fd) {
        DebugAssert.isMainThread();

        new PassSocketAsyncTask() {
            @Override
            protected Integer doInBackgroundDefaultPriority(Integer fileDescriptor) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                final int fd = fileDescriptor.intValue();
                int versionCode = mSysproxyVersion.mVersionCode;
                maybeLogDebug("connectNativeInBackground fd:" + fd
                        + " version:" + versionCode);
                return connectNative(fd, versionCode);
            }

            @Override
            protected void onPostExecute(Integer result) {
                DebugAssert.isMainThread();
                if (mIsClosed) {
                    maybeLogDebug("Shard closed after sending bluetooth socket");
                }
                if (result == CONNECT_RESULT_CONNECTED) {
                    maybeLogDebug("proxy socket delivered fd:" + fd);
                } else if (result == CONNECT_RESULT_TIMEOUT) {
                    continueSysproxyConnect();
                } else {
                    Log.w(TAG, logInstance() + "Unable to deliver socket to sysproxy module");
                    updateClientState(ClientState.IDLE);
                    mHandler.sendEmptyMessage(WHAT_RESET_CONNECTION);
                }
            }
        }.execute(fd);
    }

    @MainThread
    private void continueSysproxyConnect() {
        DebugAssert.isMainThread();

        if (mIsClosed) {
            maybeLogDebug("Shard closed while connecting sysproxy");
            return;
        }

        new DefaultPriorityAsyncTask<Void, Void, Integer>() {
            @Override
            protected  Integer doInBackgroundDefaultPriority() {
                maybeLogDebug("continue sysproxy connect after timeout.");
                return continueConnectNative();
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == CONNECT_RESULT_TIMEOUT) {
                    continueSysproxyConnect();
                } else if (result == CONNECT_RESULT_FAILED)  {
                    updateClientState(ClientState.IDLE);
                    mHandler.sendEmptyMessage(WHAT_RESET_CONNECTION);
                }
            }
        }.execute();
    }


    /**
     * Disconnect the current sysproxy network session.
     *
     * Inform the native sysproxy module to teardown the current sysproxy session.
     */
    @MainThread
    private void disconnectNativeInBackground() {
        DebugAssert.isMainThread();
        if (clientState == ClientState.IDLE) {
            maybeLogDebug("JNI has already disconnected");
            return;
        }
        updateClientState(ClientState.DISCONNECTING);

        new DefaultPriorityAsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackgroundDefaultPriority() {
                maybeLogDebug("JNI Disconnect request to sysproxy module");
                return disconnectNative();
            }

            @MainThread
            @Override
            protected void onPostExecute(Boolean result) {
                DebugAssert.isMainThread();
                // Double check if sysproxy is still connected and did not
                // initiate a disconnect during this async operation.
                // see: bug 111653688
                if (!result) {
                    // Failed to talk over existing client socket: no connection exist now
                    updateClientState(ClientState.IDLE);
                }
                LogUtil.logD(TAG, "JNI Disconnect result: %s clientState: %s isClosed: %s",
                    result, clientState, mIsClosed);
            }
        }.execute();
    }

    /**
     * This method is called from JNI in a background thread when the companion proxy
     * network state changes on the phone.
     */
    @WorkerThread
    protected void onActiveNetworkState(@NetworkType final int networkType,
            final boolean isMetered) {
        mHandler.sendMessage(mHandler.obtainMessage(WHAT_JNI_ACTIVE_NETWORK_STATE, networkType,
                    isMetered ? IS_METERED : IS_NOT_METERED));
    }

    /** This method is called from JNI in a background thread when the proxy has disconnected. */
    @WorkerThread
    protected void onDisconnect(final int status) {
        mHandler.sendMessage(mHandler.obtainMessage(WHAT_JNI_DISCONNECTED, status, 0));
    }

    /**
     * This method notifies the listener about the state of the sysproxy network.
     *
     *  NOTE: CompanionProxyShard should never call onProxyConnectionChange directly!
     *       Use the notifyConnectionChange method instead.
     */
    @MainThread
    private void notifyConnectionChange(final boolean isConnected) {
        DebugAssert.isMainThread();
        notifyConnectionChange(isConnected, false);
    }

    @MainThread
    private void notifyConnectionChange(final boolean isConnected, final boolean phoneNoInternet) {
        DebugAssert.isMainThread();
        mIsConnected = isConnected;
        mPhoneNoInternet = phoneNoInternet;
        doNotifyConnectionChange(mProxyServiceHelper.getNetworkScore());
    }

    @MainThread
    private void doNotifyConnectionChange(final int networkScore) {
        DebugAssert.isMainThread();
        if (!mIsClosed && mListener != null) {
            mListener.onProxyConnectionChange(mIsConnected, networkScore, mPhoneNoInternet);
        }
    }

    private boolean connectedWithInternet() {
        return clientState == ClientState.CONNECTED && mNetworkType != ConnectivityManager.TYPE_NONE;
    }

    private boolean connectedNoInternet() {
        return clientState == ClientState.CONNECTED && mNetworkType == ConnectivityManager.TYPE_NONE;
    }

    private abstract static class DefaultPriorityAsyncTask<Params, Progress, Result>
            extends AsyncTask<Params, Progress, Result> {

            @Override
            protected Result doInBackground(Params... params) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                return doInBackgroundDefaultPriority();
            }

            protected abstract Result doInBackgroundDefaultPriority();
    }

    private abstract static class PassSocketAsyncTask extends AsyncTask<Integer, Void, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
            final Integer fileDescriptor = params[0];
            return doInBackgroundDefaultPriority(fileDescriptor);
        }

        protected abstract Integer doInBackgroundDefaultPriority(Integer fd);
    }

    private void updateClientState(ClientState newState) {
        if (clientState != newState) {
            LogUtil.logD(TAG, "updateClientState %s -> %s", clientState, newState);
            clientState = newState;
        }
    }

    private void maybeLogDebug(final String msg) {
        LogUtil.logD(TAG, msg);
    }

    private String logInstance() {
        return "CompanionProxyShard  ";
    }

    public void dump(@NonNull final IndentingPrintWriter ipw) {
        ipw.printf("Companion proxy [ %s ] %s",
                mCompanionTracker.getCompanion(), mCompanionTracker.getCompanionName());
        ipw.println();
        ipw.increaseIndent();
        ipw.printPair("isClosed", mIsClosed);
        ipw.printPair("Start attempts", mStartAttempts);
        ipw.printPair("Start connection scheduled", mHandler.hasMessages(WHAT_START_SYSPROXY));
        ipw.printPair("isMetered", mIsMetered);
        ipw.printPair("clientState", clientState);
        ipw.printPair("sysproxyVersion", mSysproxyVersion);
        ipw.println();
        mProxyServiceHelper.dump(ipw);
        ipw.decreaseIndent();
    }

    private class CompanionUuidReceiver extends BroadcastReceiver {
        @MainThread
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || mCompanionTracker.getCompanion() == null
                    || !device.getAddress().equals(mCompanionTracker.getCompanion().getAddress())) {
                LogUtil.logD(TAG, "UUID event for non-companion device: " + device.getAddress());
                return;
            }

            if (!mSysproxyVersion.isUpgradeAvailable(mContext)) {
                Log.d(TAG, "[ProxyShard] no upgrade: " + mSysproxyVersion);
                return;
            }

            Parcelable[] uuidsParcelArray =
                    intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
            if (uuidsParcelArray == null) {
                Log.e(TAG, "[ProxyShard] null UUID array; handling aborted");
                return;
            }

            ParcelUuid[] uuids = new ParcelUuid[uuidsParcelArray.length];
            StringBuilder debugUuids = new StringBuilder();
            for (int i = 0; i < uuidsParcelArray.length; i++) {
                uuids[i] = (ParcelUuid) uuidsParcelArray[i];
                debugUuids.append(' ').append(uuids[i].getUuid().toString());
            }

            LogUtil.logD(TAG, "[ProxyShard] action UUIDs: " + debugUuids);

            ProxyServiceVersion version = ProxyServiceVersion.detectVersion(mContext, uuids);

            if (mSysproxyVersion.mVersionCode != version.mVersionCode) {
                LogUtil.logD(TAG, "[ProxyShard] restart sysproxy for new version: " + version);
                mSysproxyVersion = version;
                stop();
                mIsClosed = false;
                mHandler.sendEmptyMessage(WHAT_START_SYSPROXY);
            }
            if (!version.isUpgradeAvailable(mContext)) {
                removeCompanionUuidReceiver();
            }
        }
    };
}
