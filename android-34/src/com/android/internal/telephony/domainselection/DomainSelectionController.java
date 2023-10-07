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
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.BarringInfo;
import android.telephony.DomainSelectionService;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.TransportSelectorCallback;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.util.TelephonyUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Manages the connection to {@link DomainSelectionService}.
 */
public class DomainSelectionController {
    private static final String TAG = "DomainSelectionController";
    private static final boolean DBG = TelephonyUtils.IS_DEBUGGABLE;

    private static final int EVENT_SERVICE_STATE_CHANGED = 1;
    private static final int EVENT_BARRING_INFO_CHANGED = 2;

    private final HandlerThread mHandlerThread =
            new HandlerThread("DomainSelectionControllerHandler");

    private final DomainSelectionService mDomainSelectionService;
    private final Handler mHandler;
    // Only added or removed, never accessed on purpose.
    private final LocalLog mLocalLog = new LocalLog(30);

    protected final Object mLock = new Object();
    protected final Context mContext;

    protected final int[] mConnectionCounts;
    private final ArrayList<DomainSelectionConnection> mConnections = new ArrayList<>();

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
     * @param service The {@link DomainSelectionService} instance.
     */
    public DomainSelectionController(@NonNull Context context,
            @NonNull DomainSelectionService service) {
        this(context, service, null);
    }

    /**
     * Creates an instance.
     *
     * @param context Context object from hosting application.
     * @param service The {@link DomainSelectionService} instance.
     * @param looper Handles event messages.
     */
    @VisibleForTesting
    public DomainSelectionController(@NonNull Context context,
            @NonNull DomainSelectionService service, @Nullable Looper looper) {
        mContext = context;
        mDomainSelectionService = service;

        if (looper == null) {
            mHandlerThread.start();
            looper = mHandlerThread.getLooper();
        }
        mHandler = new DomainSelectionControllerHandler(looper);

        int numPhones = TelephonyManager.getDefault().getActiveModemCount();
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
     */
    public void selectDomain(@NonNull DomainSelectionService.SelectionAttributes attr,
            @NonNull TransportSelectorCallback callback) {
        if (attr == null || callback == null) return;
        if (DBG) logd("selectDomain");

        Executor e = mDomainSelectionService.getCachedExecutor();
        e.execute(() -> mDomainSelectionService.onDomainSelection(attr, callback));
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

        Executor e = mDomainSelectionService.getCachedExecutor();
        e.execute(() -> mDomainSelectionService.onServiceStateUpdated(
                phone.getPhoneId(), phone.getSubId(), serviceState));
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

        Executor e = mDomainSelectionService.getCachedExecutor();
        e.execute(() -> mDomainSelectionService.onBarringInfoUpdated(
                phone.getPhoneId(), phone.getSubId(), info));
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
     * of the service disconnection.
     */
    private void notifyServiceDisconnected() {
        for (DomainSelectionConnection c : mConnections) {
            c.onServiceDisconnected();
        }
    }

    /**
     * Gets the {@link Executor} which executes methods of {@link DomainSelectionService.}
     * @return {@link Executor} instance.
     */
    public @NonNull Executor getDomainSelectionServiceExecutor() {
        return mDomainSelectionService.getCachedExecutor();
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
