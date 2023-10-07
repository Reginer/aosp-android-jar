package com.android.clockwork.bluetooth;

import static com.android.clockwork.bluetooth.proxy.ProxyConnection.CONNECT_RESULT_CONNECTED;
import static com.android.clockwork.bluetooth.proxy.ProxyConnection.CONNECT_RESULT_FAILED;
import static com.android.clockwork.bluetooth.proxy.ProxyConnection.CONNECT_RESULT_TIMEOUT;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.WorkerThread;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetooth;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.android.clockwork.bluetooth.proxy.ProxyConnection;
import com.android.clockwork.bluetooth.proxy.ProxyConnectionV1;
import com.android.clockwork.bluetooth.proxy.ProxyConnectionV2;
import com.android.clockwork.bluetooth.proxy.ProxyNetworkAgent;
import com.android.clockwork.bluetooth.proxy.ProxyServiceConfig;
import com.android.clockwork.bluetooth.proxy.ProxyServiceHelper;
import com.android.clockwork.bluetooth.proxy.WearProxyConstants.Reason;
import com.android.clockwork.common.DebugAssert;
import com.android.clockwork.common.LogUtil;
import com.android.clockwork.common.Util;
import com.android.clockwork.common.WearBluetoothSettings;
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
public class CompanionProxyShard implements ProxyConnection.Listener {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;
    private static final int WHAT_START_SYSPROXY = 1;
    private static final int WHAT_JNI_ACTIVE_NETWORK_STATE = 2;
    private static final int WHAT_JNI_DISCONNECTED = 3;
    private static final int WHAT_RESET_CONNECTION = 4;

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

    @VisibleForTesting int mStartAttempts;

    @ProxyConnection.NetworkType
    @VisibleForTesting int mNetworkType;

    private boolean mIsMetered;
    private boolean mIsConnected;
    private boolean mPhoneNoInternet;
    private ProxyServiceConfig mProxyServiceConfig = new ProxyServiceConfig();

    private enum ClientState {IDLE, CONNECTING, CONNECTED, DISCONNECTING}
    private ClientState mClientState = ClientState.IDLE;

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
    @VisibleForTesting boolean mIsStarted;

    private volatile ProxyConnection mProxyConnection;

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

        /**
         * Callback executed when the sysproxy fails to connect to its counterpart.
         */
        void onProxyConnectFailed(ProxyServiceConfig failedConfig);
    }

    public CompanionProxyShard(
            @NonNull final Context context,
            @NonNull final CompanionTracker companionTracker,
            final boolean isLocalEdition) {
        DebugAssert.isMainThread();

        mContext = context;
        mCompanionTracker = companionTracker;

        mProxyServiceHelper = new ProxyServiceHelper(
            mContext, new NetworkCapabilities.Builder(), isLocalEdition);

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
        if (!mIsStarted) {
            Log.w(TAG, logInstance() + "Already stopped");
            return;
        }
        mReconnectBackoff.reset();
        updateAndNotify(SysproxyNetworkState.DISCONNECTED, Reason.CLOSABLE);
        // notify mListener of our intended disconnect before setting mIsStarted to false
        mIsStarted = false;
        disconnectNativeInBackground();
        mHandler.removeMessages(WHAT_START_SYSPROXY);
        maybeLogDebug("Stopped companion proxy shard");
        mListener = null;
    }

    @MainThread
    private void restart() {
        if (!mIsStarted) {
            Log.w(TAG, logInstance() + "restart: Already stopped");
            return;
        }

        mReconnectBackoff.reset();
        disconnectNativeInBackground();
        mHandler.removeMessages(WHAT_START_SYSPROXY);
        mHandler.sendEmptyMessage(WHAT_START_SYSPROXY);
    }

    /**
     * Start sysproxy network connecting to RFCOMM socket and sysproxy native service.
     *
     * While sysproxy is connected {@code listener} is notified about underling network
     * status changes.
     */
    @MainThread
    public void startNetwork(int networkScore, List<InetAddress> dnsServers,
            ProxyServiceConfig serviceConfig, Listener listener) {
        DebugAssert.isMainThread();
        LogUtil.logD(TAG, "startNetwork(%s, %s)", networkScore, serviceConfig);

        if (!serviceConfig.equals(mProxyServiceConfig)) {
            stop();
            mProxyServiceConfig = serviceConfig;
        }

        mIsStarted = true;

        // TODO(b/161549960): move to list of listeners, add/remove listener API, as we refactor
        // and directly use this class from mediator
        if (mListener != null && mListener != listener) {
            Log.e(TAG, logInstance() + "Replacing existing NON-NULL listener");
        }

        mListener = listener;

        mProxyServiceHelper.setCompanionName(mCompanionTracker.getCompanionName());
        mProxyServiceHelper.setDnsServers(dnsServers);
        mProxyServiceHelper.setNetworkScore(networkScore);

        mHandler.sendEmptyMessage(WHAT_START_SYSPROXY);
    }

    @MainThread
    public boolean isStarted() {
        return mIsStarted;
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
                    if (!mIsStarted) {
                        maybeLogDebug("start sysproxy but shard stopped...will bail");
                        return;
                    }
                    if (mClientState == ClientState.DISCONNECTING) {
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

                    if (!mIsStarted) {
                        maybeLogDebug("JNI onActiveNetworkState shard stopped...will bail");
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
                    maybeLogDebug("JNI onDisconnect isStarted:" + mIsStarted + " status:" + status);
                    updateAndNotify(SysproxyNetworkState.DISCONNECTED,
                            Reason.SYSPROXY_DISCONNECTED);
                    setUpRetryIfNotStopped();
                    break;
                case WHAT_RESET_CONNECTION:
                    // Take a hammer to reset everything on sysproxy side to initial state.
                    maybeLogDebug(
                        "Reset companion proxy network connection isStarted:" + mIsStarted);
                    mHandler.removeMessages(WHAT_START_SYSPROXY);
                    mHandler.removeMessages(WHAT_RESET_CONNECTION);
                    disconnectNativeInBackground();
                    setUpRetryIfNotStopped();
                    break;
            }
        }
    };

    private void setUpRetryIfNotStopped() {
        if (mIsStarted) {
            final int nextRetry = mReconnectBackoff.getNextBackoff();
            mHandler.sendEmptyMessageDelayed(WHAT_START_SYSPROXY, nextRetry * 1000);
            Log.w(TAG, logInstance() + "Attempting reconnect in " + nextRetry + " seconds");
        }
    }

    @MainThread
    private void updateAndNotify(final SysproxyNetworkState state, final String reason) {
        DebugAssert.isMainThread();
        ProxyConnection proxyConnection = mProxyConnection;
        if (state == SysproxyNetworkState.CONNECTED_WITH_INTERNET && proxyConnection != null) {
            mProxyServiceHelper.startNetworkSession(
                    reason,
                    proxyConnection.getInterfaceName(),
                    proxyConnection.getMtu(),
                    /**
                     * Called when the current network agent is no longer wanted
                     * by {@link ConnectivityService}.  Try to restart the network.
                     */
                    new ProxyNetworkAgent.Listener() {
                        @Override
                        public void onNetworkAgentUnwanted(int netId) {
                            Log.d(TAG, logInstance() + "Network agent unwanted netId:" + netId);
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

        if (mClientState != ClientState.IDLE) {
            return;
        }
        updateClientState(ClientState.CONNECTING);

        maybeLogDebug("Retrieving bluetooth network socket");

        new DefaultPriorityAsyncTask<Void, Void, ParcelFileDescriptor>() {
            @Override
            protected ParcelFileDescriptor doInBackgroundDefaultPriority() {
                try {
                    final IBluetooth bluetoothProxy = getBluetoothService();
                    if (bluetoothProxy == null) {
                        Log.e(TAG, logInstance() + "Unable to get binder proxy to IBluetooth");
                        return null;
                    }
                    ParcelFileDescriptor parcelFd = bluetoothProxy.getSocketManager().connectSocket(
                            mCompanionTracker.getCompanion(),
                            mProxyServiceConfig.getAidlConnectionType(),
                            mProxyServiceConfig.serviceUuid,
                            mProxyServiceConfig.connectionPort,
                            mProxyServiceConfig.getAidlConnectionFlags());
                    maybeLogDebug("Connect with config=" + mProxyServiceConfig
                            + " parcelFd=" + fdToString(parcelFd));

                    if (mProxyServiceConfig.isIos()) {
                        // Only needed on iOS. Monitors traffic on the BT socket and pings the
                        // companion if needed.
                        return createBluetoothSocketMonitor(() -> {
                            mHandler.post(() -> {
                                if (mListener != null) {
                                    mListener.onProxyBleData();
                                }
                            });
                        }).start(parcelFd);
                    } else {
                        return parcelFd;
                    }

                } catch (RemoteException e) {
                    Log.e(TAG, logInstance() + "Unable to get bluetooth service", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(@Nullable ParcelFileDescriptor parcelFd) {
                DebugAssert.isMainThread();

                if (!mIsStarted) {
                    maybeLogDebug("Shard stopped after retrieving bluetooth socket");
                    Util.close(parcelFd);
                    updateClientState(ClientState.IDLE);
                    return;
                }

                if (parcelFd != null) {
                    maybeLogDebug("Retrieved bluetooth network socket parcelFd:"
                            + fdToString(parcelFd));
                    connectNativeInBackground(parcelFd);
                } else {
                    Log.e(TAG, logInstance() + "Unable to request bluetooth network socket");
                    updateClientState(ClientState.IDLE);
                    if (mListener != null) {
                        mListener.onProxyConnectFailed(mProxyServiceConfig);
                    }
                    mHandler.sendEmptyMessage(WHAT_RESET_CONNECTION);
                }
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
    private void connectNativeInBackground(ParcelFileDescriptor parcelFd) {
        DebugAssert.isMainThread();

        new PassSocketAsyncTask() {
            @Override
            protected Integer doInBackgroundDefaultPriority(ParcelFileDescriptor fileDescriptor) {
                maybeLogDebug("connectNativeInBackground state: " + mClientState
                        + " fd:" + fdToString(fileDescriptor)
                        + " config:" + mProxyServiceConfig);

                if (mProxyConnection != null) {
                    Log.w(TAG, logInstance()
                            + "connectNativeInBackground already has an open connection");
                    mProxyConnection.disconnect();
                    mProxyConnection = null;
                }

                if (mProxyServiceConfig.type == ProxyServiceConfig.Type.V2_ANDROID) {
                    mProxyConnection = new ProxyConnectionV2(CompanionProxyShard.this);
                } else {
                    mProxyConnection = new ProxyConnectionV1(
                        mProxyServiceConfig.getJniSysproxyVersion(), CompanionProxyShard.this);
                }

                // for sysproxy v1.x it's possible to recieve onActiveNetworkState call before
                // returning from mProxyConnection.connect(...), see b/257392974
                return handleConnectResultInBackground(mProxyConnection.connect(fileDescriptor));
            }

            @Override
            protected void onPostExecute(Integer result) {
                handleConnectResultInMain(result);
            }
        }.execute(parcelFd);
    }

    @MainThread
    private void continueSysproxyConnect() {
        DebugAssert.isMainThread();

        if (!mIsStarted) {
            maybeLogDebug("Shard stopped while continuing to connect sysproxy");
            return;
        }

        new DefaultPriorityAsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackgroundDefaultPriority() {
                if (mProxyConnection == null) {
                    maybeLogDebug("no connection to continue sysproxy connect");
                    return CONNECT_RESULT_FAILED;
                }
                maybeLogDebug("continue sysproxy connect after timeout.");
                return handleConnectResultInBackground(mProxyConnection.continueConnect());
            }

            @Override
            protected void onPostExecute(Integer result) {
                handleConnectResultInMain(result);
            }
        }.execute();
    }

    private Integer handleConnectResultInBackground(int result) {
        if (result == CONNECT_RESULT_FAILED) {
            mProxyConnection.disconnect();
            mProxyConnection = null;
        }
        return result;
    }

    private void handleConnectResultInMain(Integer result) {
        DebugAssert.isMainThread();

        if (!mIsStarted) {
            maybeLogDebug("Shard stopped after sending bluetooth socket");
            return;
        }

        if (mClientState != ClientState.CONNECTING) {
            maybeLogDebug("Not in connecting state " + mClientState
                    + ", cannot continue to connect sysproxy");
            return;
        }

        ProxyConnection proxyConnection = mProxyConnection;
        if (result == CONNECT_RESULT_CONNECTED && proxyConnection != null) {
            Log.i(TAG, logInstance() + "ProxyConnection successfully connected, if="
                    + proxyConnection.getInterfaceName() + ", mtu=" + proxyConnection.getMtu()
                    + ", config=" + mProxyServiceConfig);
            return;
        }

        if (result == CONNECT_RESULT_TIMEOUT) {
            continueSysproxyConnect();
            return;
        }

        Log.w(TAG, logInstance() + "Unable to establish sysproxy connection");

        updateClientState(ClientState.IDLE);
        if (mListener != null) {
            mListener.onProxyConnectFailed(mProxyServiceConfig);
        }
        mHandler.sendEmptyMessage(WHAT_RESET_CONNECTION);
    }

    /**
     * Disconnect the current sysproxy network session.
     *
     * Inform the native sysproxy module to teardown the current sysproxy session.
     */
    @MainThread
    private void disconnectNativeInBackground() {
        DebugAssert.isMainThread();
        if (mClientState == ClientState.IDLE) {
            maybeLogDebug("JNI has already disconnected");
            updateClientState(ClientState.IDLE);
            return;
        }
        updateClientState(ClientState.DISCONNECTING);

        new DefaultPriorityAsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackgroundDefaultPriority() {
                if (mProxyConnection == null) {
                    return false;  // Failed to talk to local sysproxy.
                }

                maybeLogDebug("JNI Disconnect request to sysproxy module");
                if (!mProxyConnection.disconnect()) {
                    mProxyConnection = null;
                    return false;  // Failed to talk to local sysproxy.
                }

                return true;
            }

            @MainThread
            @Override
            protected void onPostExecute(Boolean result) {
                DebugAssert.isMainThread();
                LogUtil.logD(TAG, "JNI Disconnect result: %s clientState: %s isStarted: %s",
                    result, mClientState, mIsStarted);
                if (!result) {
                    // Failed to talk over existing client socket: no connection exist now.
                    // "False" result signifies successful disconnect() request.
                    updateClientState(ClientState.IDLE);
                }
            }
        }.execute();
    }

    /**
     * This method is called from JNI in a background thread when the companion proxy
     * network state changes on the phone.
     */
    @WorkerThread
    @Override
    public void onActiveNetworkState(@ProxyConnection.NetworkType final int networkType,
            final boolean isMetered) {
        mHandler.sendMessage(mHandler.obtainMessage(WHAT_JNI_ACTIVE_NETWORK_STATE, networkType,
                    isMetered ? IS_METERED : IS_NOT_METERED));
    }

    /** This method is called from JNI in a background thread when the proxy has disconnected. */
    @WorkerThread
    @Override
    public void onDisconnect(final int status, final String reason) {
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
        if (mIsStarted && mListener != null) {
            mListener.onProxyConnectionChange(mIsConnected, networkScore, mPhoneNoInternet);
        }
    }

    private boolean connectedWithInternet() {
        return mClientState == ClientState.CONNECTED
            && mNetworkType != ConnectivityManager.TYPE_NONE;
    }

    private boolean connectedNoInternet() {
        return mClientState == ClientState.CONNECTED
            && mNetworkType == ConnectivityManager.TYPE_NONE;
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

    private abstract static class PassSocketAsyncTask extends AsyncTask<ParcelFileDescriptor, Void, Integer> {
        @Override
        protected Integer doInBackground(ParcelFileDescriptor... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
            final ParcelFileDescriptor fileDescriptor = params[0];
            return doInBackgroundDefaultPriority(fileDescriptor);
        }

        protected abstract Integer doInBackgroundDefaultPriority(ParcelFileDescriptor fd);
    }

    /** Returns the shared instance of IBluetooth using reflection (method is package private). */
    private static IBluetooth getBluetoothService() {
        try {
            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            final Method getBluetoothService = adapter.getClass()
                    .getDeclaredMethod("getBluetoothService");
            getBluetoothService.setAccessible(true);
            return (IBluetooth) getBluetoothService.invoke(adapter);
        } catch (Exception e) {
            Log.e(TAG, logInstance() + "CompanionProxyShard Error retrieving IBluetooth: ", e);
            return null;
        }
    }

    private void updateClientState(ClientState newState) {
        if (mClientState != newState) {
            LogUtil.logD(TAG, "updateClientState %s -> %s", mClientState, newState);
            mClientState = newState;
        }
    }

    private static String fdToString(ParcelFileDescriptor fileDescriptor) {
       if (fileDescriptor == null) {
           return "null";
       }
       try {
           return Integer.toString(fileDescriptor.getFd());
       } catch (IllegalStateException e) {
           return "closed";
       }
    }

    private void maybeLogDebug(final String msg) {
        LogUtil.logD(TAG, logInstance() + msg);
    }

    private static String logInstance() {
        return "[CompanionProxyShard] ";
    }

    public void dump(@NonNull final IndentingPrintWriter ipw) {
        ipw.printf("Companion proxy [ %s ] %s",
                mCompanionTracker.getCompanion(), mCompanionTracker.getCompanionName());
        ipw.println();
        ipw.increaseIndent();
        ipw.printPair("isStarted", mIsStarted);
        ipw.printPair("Start attempts", mStartAttempts);
        ipw.printPair("Start connection scheduled", mHandler.hasMessages(WHAT_START_SYSPROXY));
        ipw.printPair("isMetered", mIsMetered);
        ipw.printPair("clientState", mClientState);
        ipw.printPair("config", mProxyServiceConfig);
        ipw.println();
        mProxyServiceHelper.dump(ipw);

        ProxyConnection proxyConnection = mProxyConnection;
        if (proxyConnection != null) {
            proxyConnection.dump(ipw);
        }

        ipw.decreaseIndent();
    }
}
