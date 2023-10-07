/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony.satellite;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.satellite.ISatelliteTransmissionUpdateCallback;
import android.telephony.satellite.PointingInfo;
import android.telephony.satellite.SatelliteManager;
import android.text.TextUtils;

import com.android.internal.R;
import com.android.internal.telephony.Phone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * PointingApp controller to manage interactions with PointingUI app.
 */
public class PointingAppController {
    private static final String TAG = "PointingAppController";
    private static final String ALLOW_MOCK_MODEM_PROPERTY = "persist.radio.allow_mock_modem";
    private static final boolean DEBUG = !"user".equals(Build.TYPE);

    @NonNull
    private static PointingAppController sInstance;
    @NonNull private final Context mContext;
    private boolean mStartedSatelliteTransmissionUpdates;
    @NonNull private String mPointingUiPackageName = "";
    @NonNull private String mPointingUiClassName = "";

    /**
     * Map key: subId, value: SatelliteTransmissionUpdateHandler to notify registrants.
     */
    private final ConcurrentHashMap<Integer, SatelliteTransmissionUpdateHandler>
            mSatelliteTransmissionUpdateHandlers = new ConcurrentHashMap<>();

    /**
     * @return The singleton instance of PointingAppController.
     */
    public static PointingAppController getInstance() {
        if (sInstance == null) {
            loge("PointingAppController was not yet initialized.");
        }
        return sInstance;
    }

    /**
     * Create the PointingAppController singleton instance.
     * @param context The Context to use to create the PointingAppController.
     * @return The singleton instance of PointingAppController.
     */
    public static PointingAppController make(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new PointingAppController(context);
        }
        return sInstance;
    }

    /**
     * Create a PointingAppController to manage interactions with PointingUI app.
     *
     * @param context The Context for the PointingUIController.
     */
    private PointingAppController(@NonNull Context context) {
        mContext = context;
        mStartedSatelliteTransmissionUpdates = false;
    }

    /**
     * Set the flag mStartedSatelliteTransmissionUpdates to true or false based on the state of
     * transmission updates
     * @param startedSatelliteTransmissionUpdates boolean to set the flag
     */
    public void setStartedSatelliteTransmissionUpdates(
            boolean startedSatelliteTransmissionUpdates) {
        mStartedSatelliteTransmissionUpdates = startedSatelliteTransmissionUpdates;
    }

    private static final class DatagramTransferStateHandlerRequest {
        public int datagramTransferState;
        public int pendingCount;
        public int errorCode;

        DatagramTransferStateHandlerRequest(int datagramTransferState, int pendingCount,
                int errorCode) {
            this.datagramTransferState = datagramTransferState;
            this.pendingCount = pendingCount;
            this.errorCode = errorCode;
        }
    }


    private static final class SatelliteTransmissionUpdateHandler extends Handler {
        public static final int EVENT_POSITION_INFO_CHANGED = 1;
        public static final int EVENT_SEND_DATAGRAM_STATE_CHANGED = 2;
        public static final int EVENT_RECEIVE_DATAGRAM_STATE_CHANGED = 3;
        public static final int EVENT_DATAGRAM_TRANSFER_STATE_CHANGED = 4;

        private final ConcurrentHashMap<IBinder, ISatelliteTransmissionUpdateCallback> mListeners;
        SatelliteTransmissionUpdateHandler(Looper looper) {
            super(looper);
            mListeners = new ConcurrentHashMap<>();
        }

        public void addListener(ISatelliteTransmissionUpdateCallback listener) {
            mListeners.put(listener.asBinder(), listener);
        }

        public void removeListener(ISatelliteTransmissionUpdateCallback listener) {
            mListeners.remove(listener.asBinder());
        }

        public boolean hasListeners() {
            return !mListeners.isEmpty();
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case EVENT_POSITION_INFO_CHANGED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    PointingInfo pointingInfo = (PointingInfo) ar.result;
                    List<IBinder> toBeRemoved = new ArrayList<>();
                    mListeners.values().forEach(listener -> {
                        try {
                            listener.onSatellitePositionChanged(pointingInfo);
                        } catch (RemoteException e) {
                            logd("EVENT_POSITION_INFO_CHANGED RemoteException: " + e);
                            toBeRemoved.add(listener.asBinder());
                        }
                    });
                    toBeRemoved.forEach(listener -> {
                        mListeners.remove(listener);
                    });
                    break;
                }

                case EVENT_DATAGRAM_TRANSFER_STATE_CHANGED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    logd("Receive EVENT_DATAGRAM_TRANSFER_STATE_CHANGED state=" + (int) ar.result);
                    break;
                }

                case EVENT_SEND_DATAGRAM_STATE_CHANGED: {
                    logd("Received EVENT_SEND_DATAGRAM_STATE_CHANGED");
                    DatagramTransferStateHandlerRequest request =
                            (DatagramTransferStateHandlerRequest) msg.obj;
                    List<IBinder> toBeRemoved = new ArrayList<>();
                    mListeners.values().forEach(listener -> {
                        try {
                            listener.onSendDatagramStateChanged(request.datagramTransferState,
                                    request.pendingCount, request.errorCode);
                        } catch (RemoteException e) {
                            logd("EVENT_SEND_DATAGRAM_STATE_CHANGED RemoteException: " + e);
                            toBeRemoved.add(listener.asBinder());
                        }
                    });
                    toBeRemoved.forEach(listener -> {
                        mListeners.remove(listener);
                    });
                    break;
                }

                case EVENT_RECEIVE_DATAGRAM_STATE_CHANGED: {
                    logd("Received EVENT_RECEIVE_DATAGRAM_STATE_CHANGED");
                    DatagramTransferStateHandlerRequest request =
                            (DatagramTransferStateHandlerRequest) msg.obj;
                    List<IBinder> toBeRemoved = new ArrayList<>();
                    mListeners.values().forEach(listener -> {
                        try {
                            listener.onReceiveDatagramStateChanged(request.datagramTransferState,
                                    request.pendingCount, request.errorCode);
                        } catch (RemoteException e) {
                            logd("EVENT_RECEIVE_DATAGRAM_STATE_CHANGED RemoteException: " + e);
                            toBeRemoved.add(listener.asBinder());
                        }
                    });
                    toBeRemoved.forEach(listener -> {
                        mListeners.remove(listener);
                    });
                    break;
                }

                default:
                    loge("SatelliteTransmissionUpdateHandler unknown event: " + msg.what);
            }
        }
    }

    /**
     * Register to start receiving updates for satellite position and datagram transfer state
     * @param subId The subId of the subscription to register for receiving the updates.
     * @param callback The callback to notify of satellite transmission updates.
     * @param phone The Phone object to unregister for receiving the updates.
     */
    public void registerForSatelliteTransmissionUpdates(int subId,
            ISatelliteTransmissionUpdateCallback callback, Phone phone) {
        SatelliteTransmissionUpdateHandler handler =
                mSatelliteTransmissionUpdateHandlers.get(subId);
        if (handler != null) {
            handler.addListener(callback);
            return;
        } else {
            handler = new SatelliteTransmissionUpdateHandler(Looper.getMainLooper());
            handler.addListener(callback);
            mSatelliteTransmissionUpdateHandlers.put(subId, handler);
            if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
                SatelliteModemInterface.getInstance().registerForSatellitePositionInfoChanged(
                        handler, SatelliteTransmissionUpdateHandler.EVENT_POSITION_INFO_CHANGED,
                        null);
                SatelliteModemInterface.getInstance().registerForDatagramTransferStateChanged(
                        handler,
                        SatelliteTransmissionUpdateHandler.EVENT_DATAGRAM_TRANSFER_STATE_CHANGED,
                        null);
            } else {
                phone.registerForSatellitePositionInfoChanged(handler,
                        SatelliteTransmissionUpdateHandler.EVENT_POSITION_INFO_CHANGED, null);
            }
        }
    }

    /**
     * Unregister to stop receiving updates on satellite position and datagram transfer state
     * If the callback was not registered before, it is ignored
     * @param subId The subId of the subscription to unregister for receiving the updates.
     * @param result The callback to get the error code in case of failure
     * @param callback The callback that was passed to {@link
     * #registerForSatelliteTransmissionUpdates(int, ISatelliteTransmissionUpdateCallback, Phone)}.
     * @param phone The Phone object to unregister for receiving the updates
     */
    public void unregisterForSatelliteTransmissionUpdates(int subId, Consumer<Integer> result,
            ISatelliteTransmissionUpdateCallback callback, Phone phone) {
        SatelliteTransmissionUpdateHandler handler =
                mSatelliteTransmissionUpdateHandlers.get(subId);
        if (handler != null) {
            handler.removeListener(callback);

            if (handler.hasListeners()) {
                result.accept(SatelliteManager.SATELLITE_ERROR_NONE);
                return;
            }

            mSatelliteTransmissionUpdateHandlers.remove(subId);
            if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
                SatelliteModemInterface.getInstance().unregisterForSatellitePositionInfoChanged(
                        handler);
                SatelliteModemInterface.getInstance().unregisterForDatagramTransferStateChanged(
                        handler);
            } else {
                if (phone == null) {
                    result.accept(SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE);
                    return;
                }
                phone.unregisterForSatellitePositionInfoChanged(handler);
            }
        }
    }

    /**
     * Start receiving satellite trasmission updates.
     * This can be called by the pointing UI when the user starts pointing to the satellite.
     * Modem should continue to report the pointing input as the device or satellite moves.
     * The transmission updates will be received via
     * {@link android.telephony.satellite.SatelliteTransmissionUpdateCallback
     * #onSatellitePositionChanged(pointingInfo)}.
     */
    public void startSatelliteTransmissionUpdates(@NonNull Message message, @Nullable Phone phone) {
        if (mStartedSatelliteTransmissionUpdates) {
            logd("startSatelliteTransmissionUpdates: already started");
            AsyncResult.forMessage(message, null, new SatelliteManager.SatelliteException(
                    SatelliteManager.SATELLITE_ERROR_NONE));
            message.sendToTarget();
            return;
        }
        if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
            SatelliteModemInterface.getInstance().startSendingSatellitePointingInfo(message);
            mStartedSatelliteTransmissionUpdates = true;
            return;
        }
        if (phone != null) {
            phone.startSatellitePositionUpdates(message);
            mStartedSatelliteTransmissionUpdates = true;
        } else {
            loge("startSatelliteTransmissionUpdates: No phone object");
            AsyncResult.forMessage(message, null, new SatelliteManager.SatelliteException(
                    SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE));
            message.sendToTarget();
        }
    }

    /**
     * Stop receiving satellite transmission updates.
     * This can be called by the pointing UI when the user stops pointing to the satellite.
     */
    public void stopSatelliteTransmissionUpdates(@NonNull Message message, @Nullable Phone phone) {
        if (SatelliteModemInterface.getInstance().isSatelliteServiceSupported()) {
            SatelliteModemInterface.getInstance().stopSendingSatellitePointingInfo(message);
            return;
        }
        if (phone != null) {
            phone.stopSatellitePositionUpdates(message);
        } else {
            loge("startSatelliteTransmissionUpdates: No phone object");
            AsyncResult.forMessage(message, null, new SatelliteManager.SatelliteException(
                    SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE));
            message.sendToTarget();
        }
    }

    /**
     * Check if Pointing is needed and Launch Pointing UI
     * @param needFullScreenPointingUI if pointing UI has to be launchd with Full screen
     */
    public void startPointingUI(boolean needFullScreenPointingUI) {
        String packageName = getPointingUiPackageName();
        if (TextUtils.isEmpty(packageName)) {
            logd("startPointingUI: config_pointing_ui_package is not set. Ignore the request");
            return;
        }

        Intent launchIntent;
        String className = getPointingUiClassName();
        if (!TextUtils.isEmpty(className)) {
            launchIntent = new Intent()
                    .setComponent(new ComponentName(packageName, className))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        }
        if (launchIntent == null) {
            loge("startPointingUI: launchIntent is null");
            return;
        }
        launchIntent.putExtra("needFullScreen", needFullScreenPointingUI);

        try {
            mContext.startActivity(launchIntent);
        } catch (ActivityNotFoundException ex) {
            loge("startPointingUI: Pointing UI app activity is not found, ex=" + ex);
        }
    }

    public void updateSendDatagramTransferState(int subId,
            @SatelliteManager.SatelliteDatagramTransferState int datagramTransferState,
            int sendPendingCount, int errorCode) {
        DatagramTransferStateHandlerRequest request = new DatagramTransferStateHandlerRequest(
                datagramTransferState, sendPendingCount, errorCode);
        SatelliteTransmissionUpdateHandler handler =
                mSatelliteTransmissionUpdateHandlers.get(subId);

        if (handler != null) {
            Message msg = handler.obtainMessage(
                    SatelliteTransmissionUpdateHandler.EVENT_SEND_DATAGRAM_STATE_CHANGED,
                    request);
            msg.sendToTarget();
        } else {
            loge("SatelliteTransmissionUpdateHandler not found for subId: " + subId);
        }
    }

    public void updateReceiveDatagramTransferState(int subId,
            @SatelliteManager.SatelliteDatagramTransferState int datagramTransferState,
            int receivePendingCount, int errorCode) {
        DatagramTransferStateHandlerRequest request = new DatagramTransferStateHandlerRequest(
                datagramTransferState, receivePendingCount, errorCode);
        SatelliteTransmissionUpdateHandler handler =
                mSatelliteTransmissionUpdateHandlers.get(subId);

        if (handler != null) {
            Message msg = handler.obtainMessage(
                    SatelliteTransmissionUpdateHandler.EVENT_RECEIVE_DATAGRAM_STATE_CHANGED,
                    request);
            msg.sendToTarget();
        } else {
            loge(" SatelliteTransmissionUpdateHandler not found for subId: " + subId);
        }
    }

    /**
     * This API can be used by only CTS to update satellite pointing UI app package and class names.
     *
     * @param packageName The package name of the satellite pointing UI app.
     * @param className The class name of the satellite pointing UI app.
     * @return {@code true} if the satellite pointing UI app package and class is set successfully,
     * {@code false} otherwise.
     */
    boolean setSatellitePointingUiClassName(
            @Nullable String packageName, @Nullable String className) {
        if (!isMockModemAllowed()) {
            loge("setSatellitePointingUiClassName: modifying satellite pointing UI package and "
                    + "class name is not allowed");
            return false;
        }

        logd("setSatellitePointingUiClassName: config_pointing_ui_package is updated, new "
                + "packageName=" + packageName
                + ", config_pointing_ui_class new className=" + className);

        if (packageName == null || packageName.equals("null")) {
            mPointingUiPackageName = "";
            mPointingUiClassName = "";
        } else {
            mPointingUiPackageName = packageName;
            if (className == null || className.equals("null")) {
                mPointingUiClassName = "";
            } else {
                mPointingUiClassName = className;
            }
        }

        return true;
    }

    @NonNull private String getPointingUiPackageName() {
        if (!TextUtils.isEmpty(mPointingUiPackageName)) {
            return mPointingUiPackageName;
        }
        return TextUtils.emptyIfNull(mContext.getResources().getString(
                R.string.config_pointing_ui_package));
    }

    @NonNull private String getPointingUiClassName() {
        if (!TextUtils.isEmpty(mPointingUiClassName)) {
            return mPointingUiClassName;
        }
        return TextUtils.emptyIfNull(mContext.getResources().getString(
                R.string.config_pointing_ui_class));
    }

    private boolean isMockModemAllowed() {
        return (DEBUG || SystemProperties.getBoolean(ALLOW_MOCK_MODEM_PROPERTY, false));
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }
    /**
     * TODO: The following needs to be added in this class:
     * - check if pointingUI crashes - then restart it
     */
}
