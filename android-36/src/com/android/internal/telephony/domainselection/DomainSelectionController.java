/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.domainselection;

import static android.telephony.DomainSelectionService.SELECTOR_TYPE_CALLING;
import static android.telephony.DomainSelectionService.SELECTOR_TYPE_SMS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.BarringInfo;
import android.telephony.DomainSelectionService;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ExponentialBackoff;
import com.android.internal.telephony.IDomainSelectionServiceController;
import com.android.internal.telephony.ITransportSelectorCallback;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.flags.Flags;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.telephony.util.WorkerThread;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Manages the connection to {@link DomainSelectionService}.
 */
public class DomainSelectionController {
    private static final String TAG = "DomainSelectionController";
    private static final boolean DBG = TelephonyUtils.IS_DEBUGGABLE;

    private static final int EVENT_SERVICE_STATE_CHANGED = 1;
    private static final int EVENT_BARRING_INFO_CHANGED = 2;

    private static final int BIND_START_DELAY_MS = 2 * 1000; // 2 seconds
    private static final int BIND_MAXIMUM_DELAY_MS = 60 * 1000; // 1 minute

    /**
     * Returns the currently defined rebind retry timeout. Used for testing.
     */
    @VisibleForTesting
    public interface BindRetry {
        /**
         * Returns a long in milliseconds indicating how long the DomainSelectionController
         * should wait before rebinding for the first time.
         */
        long getStartDelay();

        /**
         * Returns a long in milliseconds indicating the maximum time the DomainSelectionController
         * should wait before rebinding.
         */
        long getMaximumDelay();
    }

    private HandlerThread mHandlerThread; // effectively final
    private final Handler mHandler;

    // Only added or removed, never accessed on purpose.
    private final LocalLog mLocalLog = new LocalLog(30);

    protected final Object mLock = new Object();
    protected final Context mContext;

    protected final int[] mConnectionCounts;
    private final ArrayList<DomainSelectionConnection> mConnections = new ArrayList<>();

    private ComponentName mComponentName;
    private DomainSelectionServiceConnection mServiceConnection;
    private IDomainSelectionServiceController mIServiceController;
    // Binding the service is in progress or the service is bound already.
    private boolean mIsBound = false;

    private ExponentialBackoff mBackoff;
    private boolean mBackoffStarted = false;
    private boolean mUnbind = false;

    // Retry the bind to the DomainSelectionService that has died after mBindRetry timeout.
    private Runnable mRestartBindingRunnable = new Runnable() {
        @Override
        public void run() {
            bind();
        }
    };

    private BindRetry mBindRetry = new BindRetry() {
        @Override
        public long getStartDelay() {
            return BIND_START_DELAY_MS;
        }

        @Override
        public long getMaximumDelay() {
            return BIND_MAXIMUM_DELAY_MS;
        }
    };

    private class DomainSelectionServiceConnection implements ServiceConnection {
        // Track the status of whether or not the Service has died in case we need to permanently
        // unbind (see onNullBinding below).
        private boolean mIsServiceConnectionDead = false;

        /** {@inheritDoc} */
        @Override
        public void onServiceConnected(ComponentName unusedName, IBinder service) {
            if (mHandler.getLooper().isCurrentThread()) {
                onServiceConnectedInternal(service);
            } else {
                mHandler.post(() -> onServiceConnectedInternal(service));
            }
        }

        /** {@inheritDoc} */
        @Override
        public void onServiceDisconnected(ComponentName unusedName) {
            if (mHandler.getLooper().isCurrentThread()) {
                onServiceDisconnectedInternal();
            } else {
                mHandler.post(() -> onServiceDisconnectedInternal());
            }
        }

        @Override
        public void onBindingDied(ComponentName unusedName) {
            if (mHandler.getLooper().isCurrentThread()) {
                onBindingDiedInternal();
            } else {
                mHandler.post(() -> onBindingDiedInternal());
            }
        }

        @Override
        public void onNullBinding(ComponentName unusedName) {
            if (mHandler.getLooper().isCurrentThread()) {
                onNullBindingInternal();
            } else {
                mHandler.post(() -> onNullBindingInternal());
            }
        }

        private void onServiceConnectedInternal(IBinder service) {
            synchronized (mLock) {
                stopBackoffTimer();
                logi("onServiceConnected with binder: " + service);
                setServiceController(service);
            }
            notifyServiceConnected();
        }

        private void onServiceDisconnectedInternal() {
            synchronized (mLock) {
                setServiceController(null);
            }
            logi("onServiceDisconnected");
            notifyServiceDisconnected();
        }

        private void onBindingDiedInternal() {
            mIsServiceConnectionDead = true;
            synchronized (mLock) {
                mIsBound = false;
                setServiceController(null);
                unbindService();
                notifyBindFailure();
            }
            loge("onBindingDied starting retrying in "
                    + mBackoff.getCurrentDelay() + " mS");
            notifyServiceDisconnected();
        }

        private void onNullBindingInternal() {
            loge("onNullBinding serviceDead=" + mIsServiceConnectionDead);
            // onNullBinding will happen after onBindingDied. In this case, we should not
            // permanently unbind and instead let the automatic rebind occur.
            if (mIsServiceConnectionDead) return;
            synchronized (mLock) {
                mIsBound = false;
                setServiceController(null);
                unbindService();
            }
            notifyServiceDisconnected();
        }
    }

    private final class DomainSelectionControllerHandler extends Handler {
        DomainSelectionControllerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updateServiceState((Phone) ar.userObj, (ServiceState) ar.result);
                    break;
                case EVENT_BARRING_INFO_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updateBarringInfo((Phone) ar.userObj, (BarringInfo) ar.result);
                    break;
                default:
                    loge("unexpected event=" + msg.what);
                    break;
            }
        }
    }

    /**
     * Creates an instance.
     *
     * @param context Context object from hosting application.
     */
    public DomainSelectionController(@NonNull Context context) {
        this(context, null, null);
    }

    /**
     * Creates an instance.
     *
     * @param context Context object from hosting application.
     * @param looper Handles event messages.
     * @param bindRetry The {@link BindRetry} instance.
     */
    @VisibleForTesting
    public DomainSelectionController(@NonNull Context context,
            @Nullable Looper looper, @Nullable BindRetry bindRetry) {
        mContext = context;

        mHandlerThread = null;
        if (looper == null) {
            if (Flags.threadShred()) {
                looper = WorkerThread.get().getLooper();
            } else {
                mHandlerThread = new HandlerThread("DomainSelectionControllerHandler");
                mHandlerThread.start();
                looper = mHandlerThread.getLooper();
            }
        }
        mHandler = new DomainSelectionControllerHandler(looper);

        if (bindRetry != null) {
            mBindRetry = bindRetry;
        }
        mBackoff = new ExponentialBackoff(
                mBindRetry.getStartDelay(),
                mBindRetry.getMaximumDelay(),
                2, /* multiplier */
                mHandler,
                mRestartBindingRunnable);

        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        int numPhones = tm.getSupportedModemCount();
        logi("numPhones=" + numPhones);
        mConnectionCounts = new int[numPhones];
        for (int i = 0; i < numPhones; i++) {
            mConnectionCounts[i] = 0;
        }
    }

    /**
     * Returns a {@link DomainSelectionConnection} instance.
     *
     * @param phone Indicates who requests the service.
     * @param selectorType Indicates the selector type requested.
     * @param isEmergency Indicates whether this is for emergency service.
     * @return A {@link DomainSelectiionConnection} instance for the requested service.
     *         Returns {@code null} if the requested service is not supported.
     */
    public @Nullable DomainSelectionConnection getDomainSelectionConnection(
            @NonNull Phone phone,
            @DomainSelectionService.SelectorType int selectorType,
            boolean isEmergency) {
        DomainSelectionConnection c = null;

        if (selectorType == SELECTOR_TYPE_CALLING) {
            if (isEmergency) {
                c = new EmergencyCallDomainSelectionConnection(phone, this);
            } else {
                c = new NormalCallDomainSelectionConnection(phone, this);
            }
        } else if (selectorType == SELECTOR_TYPE_SMS) {
            if (isEmergency) {
                c = new EmergencySmsDomainSelectionConnection(phone, this);
            } else {
                c = new SmsDomainSelectionConnection(phone, this);
            }
        }

        addConnection(c);
        return c;
    }

    private void addConnection(@Nullable DomainSelectionConnection c) {
        if (c == null) return;
        mConnections.add(c);
        registerForStateChange(c);
    }

    /**
     * Releases resources for this connection.
     */
    public void removeConnection(@Nullable DomainSelectionConnection c) {
        if (c == null) return;
        mConnections.remove(c);
        unregisterForStateChange(c);
    }

    /**
     * Requests the domain selection.
     *
     * @param attr Attributetes required to determine the domain.
     * @param callback A callback to receive the response.
     * @return {@code true} if it requested successfully, otherwise {@code false}.
     */
    public boolean selectDomain(@NonNull DomainSelectionService.SelectionAttributes attr,
            @NonNull ITransportSelectorCallback callback) {
        if (attr == null) return false;
        if (DBG) logd("selectDomain");

        synchronized (mLock) {
            try  {
                if (mIServiceController != null) {
                    mIServiceController.selectDomain(attr, callback);
                    return true;
                }
            } catch (RemoteException e) {
                loge("selectDomain e=" + e);
            }
        }
        return false;
    }

    /**
     * Notifies the change in {@link ServiceState} for a specific slot.
     *
     * @param phone {@link Phone} which the state changed.
     * @param serviceState Updated {@link ServiceState}.
     */
    private void updateServiceState(Phone phone, ServiceState serviceState) {
        if (phone == null || serviceState == null) return;
        if (DBG) logd("updateServiceState phoneId=" + phone.getPhoneId());

        synchronized (mLock) {
            try  {
                if (mIServiceController != null) {
                    mIServiceController.updateServiceState(
                            phone.getPhoneId(), phone.getSubId(), serviceState);
                }
            } catch (RemoteException e) {
                loge("updateServiceState e=" + e);
            }
        }
    }

    /**
     * Notifies the change in {@link BarringInfo} for a specific slot.
     *
     * @param phone {@link Phone} which the state changed.
     * @param info Updated {@link BarringInfo}.
     */
    private void updateBarringInfo(Phone phone, BarringInfo info) {
        if (phone == null || info == null) return;
        if (DBG) logd("updateBarringInfo phoneId=" + phone.getPhoneId());

        synchronized (mLock) {
            try  {
                if (mIServiceController != null) {
                    mIServiceController.updateBarringInfo(
                            phone.getPhoneId(), phone.getSubId(), info);
                }
            } catch (RemoteException e) {
                loge("updateBarringInfo e=" + e);
            }
        }
    }

    /**
     * Registers for the notification of {@link ServiceState} and {@link BarringInfo}.
     *
     * @param c {@link DomainSelectionConnection} for which the registration is requested.
     */
    private void registerForStateChange(DomainSelectionConnection c) {
        Phone phone = c.getPhone();
        int count = mConnectionCounts[phone.getPhoneId()];
        if (count < 0) count = 0;

        mConnectionCounts[phone.getPhoneId()] = count + 1;
        if (count > 0) return;

        phone.registerForServiceStateChanged(mHandler, EVENT_SERVICE_STATE_CHANGED, phone);
        phone.mCi.registerForBarringInfoChanged(mHandler, EVENT_BARRING_INFO_CHANGED, phone);

        updateServiceState(phone, phone.getServiceStateTracker().getServiceState());
        updateBarringInfo(phone, phone.mCi.getLastBarringInfo());
    }

    /**
     * Unregisters for the notification of {@link ServiceState} and {@link BarringInfo}.
     *
     * @param c {@link DomainSelectionConnection} for which the unregistration is requested.
     */
    private void unregisterForStateChange(DomainSelectionConnection c) {
        Phone phone = c.getPhone();
        int count = mConnectionCounts[phone.getPhoneId()];
        if (count < 1) count = 1;

        mConnectionCounts[phone.getPhoneId()] = count - 1;
        if (count > 1) return;

        phone.unregisterForServiceStateChanged(mHandler);
        phone.mCi.unregisterForBarringInfoChanged(mHandler);
    }

    /**
     * Notifies the {@link DomainSelectionConnection} instances registered
     * of the service connection.
     */
    private void notifyServiceConnected() {
        for (DomainSelectionConnection c : mConnections) {
            c.onServiceConnected();
            Phone phone = c.getPhone();
            updateServiceState(phone, phone.getServiceStateTracker().getServiceState());
            updateBarringInfo(phone, phone.mCi.getLastBarringInfo());
        }
    }

    /**
     * Notifies the {@link DomainSelectionConnection} instances registered
     * of the service disconnection.
     */
    private void notifyServiceDisconnected() {
        for (DomainSelectionConnection c : mConnections) {
            c.onServiceDisconnected();
        }
    }

    /**
     * Sets the binder interface to communicate with {@link domainSelectionService}.
     */
    protected void setServiceController(@NonNull IBinder serviceController) {
        mIServiceController = IDomainSelectionServiceController.Stub.asInterface(serviceController);
    }

    /**
     * Sends request to bind to {@link DomainSelectionService}.
     *
     * @param componentName The {@link ComponentName} instance.
     * @return {@code true} if the service is in the process of being bound, {@code false} if it
     *         has failed.
     */
    public boolean bind(@NonNull ComponentName componentName) {
        mComponentName = componentName;
        mUnbind = false;
        return bind();
    }

    private boolean bind() {
        logd("bind isBindingOrBound=" + mIsBound);
        synchronized (mLock) {
            if (mUnbind) return false;
            if (!mIsBound) {
                mIsBound = true;
                Intent serviceIntent = new Intent(DomainSelectionService.SERVICE_INTERFACE)
                        .setComponent(mComponentName);
                mServiceConnection = new DomainSelectionServiceConnection();
                int serviceFlags = Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE
                        | Context.BIND_IMPORTANT;
                logi("binding DomainSelectionService");
                try {
                    boolean bindSucceeded = mContext.bindService(serviceIntent,
                            mServiceConnection, serviceFlags);
                    if (!bindSucceeded) {
                        loge("binding failed retrying in "
                                + mBackoff.getCurrentDelay() + " mS");
                        mIsBound = false;
                        notifyBindFailure();
                    }
                    return bindSucceeded;
                } catch (Exception e) {
                    mIsBound = false;
                    notifyBindFailure();
                    loge("binding e=" + e.getMessage() + ", retrying in "
                            + mBackoff.getCurrentDelay() + " mS");
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * Unbinds the service.
     */
    public void unbind() {
        synchronized (mLock) {
            mUnbind = true;
            stopBackoffTimer();
            mIsBound = false;
            setServiceController(null);
            unbindService();
        }
    }

    private void unbindService() {
        synchronized (mLock) {
            if (mServiceConnection != null) {
                logi("unbinding Service");
                mContext.unbindService(mServiceConnection);
                mServiceConnection = null;
            }
        }
    }

    /**
     * Gets the current delay to rebind service.
     */
    @VisibleForTesting
    public long getBindDelay() {
        return mBackoff.getCurrentDelay();
    }

    /**
     * Stops backoff timer.
     */
    @VisibleForTesting
    public void stopBackoffTimer() {
        logi("stopBackoffTimer " + mBackoffStarted);
        mBackoffStarted = false;
        mBackoff.stop();
    }

    private void notifyBindFailure() {
        logi("notifyBindFailure started=" + mBackoffStarted + ", unbind=" + mUnbind);
        if (mUnbind) return;
        if (mBackoffStarted) {
            mBackoff.notifyFailed();
        } else {
            mBackoffStarted = true;
            mBackoff.start();
        }
        logi("notifyBindFailure currentDelay=" + getBindDelay());
    }

    /**
     * Returns the Handler instance.
     */
    @VisibleForTesting
    public Handler getHandlerForTest() {
        return mHandler;
    }

    /**
     * Dumps logcal log
     */
    public void dump(@NonNull PrintWriter printWriter) {
        mLocalLog.dump(printWriter);
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void logi(String msg) {
        Log.i(TAG, msg);
        mLocalLog.log(msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
        mLocalLog.log(msg);
    }
}
