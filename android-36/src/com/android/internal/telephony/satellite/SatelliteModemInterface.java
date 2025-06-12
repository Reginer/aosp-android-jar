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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.IBooleanConsumer;
import android.telephony.IIntegerConsumer;
import android.telephony.PersistentLogger;
import android.telephony.Rlog;
import android.telephony.satellite.NtnSignalStrength;
import android.telephony.satellite.SatelliteCapabilities;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteManager.SatelliteException;
import android.telephony.satellite.SatelliteModemEnableRequestAttributes;
import android.telephony.satellite.SystemSelectionSpecifier;
import android.telephony.satellite.stub.INtnSignalStrengthConsumer;
import android.telephony.satellite.stub.ISatellite;
import android.telephony.satellite.stub.ISatelliteCapabilitiesConsumer;
import android.telephony.satellite.stub.ISatelliteListener;
import android.telephony.satellite.stub.SatelliteModemState;
import android.telephony.satellite.stub.SatelliteService;
import android.text.TextUtils;
import android.util.Pair;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ExponentialBackoff;
import com.android.internal.telephony.flags.FeatureFlags;

import java.util.Arrays;
import java.util.List;

/**
 * Satellite modem interface to manage connections with the satellite service and HAL interface.
 */
public class SatelliteModemInterface {
    private static final String TAG = "SatelliteModemInterface";
    private static final long REBIND_INITIAL_DELAY = 2 * 1000; // 2 seconds
    private static final long REBIND_MAXIMUM_DELAY = 64 * 1000; // 1 minute
    private static final int REBIND_MULTIPLIER = 2;

    @NonNull private static SatelliteModemInterface sInstance;
    @NonNull private final Context mContext;
    @NonNull private final DemoSimulator mDemoSimulator;
    @NonNull private final SatelliteListener mVendorListener;
    @NonNull private final SatelliteListener mDemoListener;
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    @NonNull protected final ExponentialBackoff mExponentialBackoff;
    @NonNull private final Object mLock = new Object();
    @NonNull private final SatelliteController mSatelliteController;
    /**
     * {@code true} to use the vendor satellite service and {@code false} to use the HAL.
     */
    private boolean mIsSatelliteServiceSupported;
    @Nullable private ISatellite mSatelliteService;
    @Nullable private SatelliteServiceConnection mSatelliteServiceConnection;
    @NonNull private String mVendorSatellitePackageName = "";
    private boolean mIsBound;
    private boolean mIsBinding;
    @Nullable private PersistentLogger mPersistentLogger = null;

    @NonNull private final RegistrantList mSatellitePositionInfoChangedRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mDatagramTransferStateChangedRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mSatelliteModemStateChangedRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mPendingDatagramsRegistrants = new RegistrantList();
    @NonNull private final RegistrantList mSatelliteDatagramsReceivedRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mNtnSignalStrengthChangedRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mSatelliteCapabilitiesChangedRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mSatelliteSupportedStateChangedRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mSatelliteRegistrationFailureRegistrants =
            new RegistrantList();
    @NonNull private final RegistrantList mTerrestrialNetworkAvailableChangedRegistrants =
            new RegistrantList();

    private class SatelliteListener extends ISatelliteListener.Stub {

        private final boolean mIsDemoListener;

        SatelliteListener(boolean isDemoListener) {
            mIsDemoListener = isDemoListener;
        }

        @Override
        public void onSatelliteDatagramReceived(
                android.telephony.satellite.stub.SatelliteDatagram datagram, int pendingCount) {
            if (notifyResultIfExpectedListener()) {
                plogd("onSatelliteDatagramReceived: pendingCount=" + pendingCount);
                mSatelliteDatagramsReceivedRegistrants.notifyResult(new Pair<>(
                        SatelliteServiceUtils.fromSatelliteDatagram(datagram), pendingCount));
            }
        }

        @Override
        public void onPendingDatagrams() {
            if (notifyResultIfExpectedListener()) {
                plogd("onPendingDatagrams");
                mPendingDatagramsRegistrants.notifyResult(null);
            }
        }

        @Override
        public void onSatellitePositionChanged(
                android.telephony.satellite.stub.PointingInfo pointingInfo) {
            mSatellitePositionInfoChangedRegistrants.notifyResult(
                    SatelliteServiceUtils.fromPointingInfo(pointingInfo));
        }

        @Override
        public void onSatelliteModemStateChanged(int state) {
            if (notifyModemStateChanged(state)) {
                mSatelliteModemStateChangedRegistrants.notifyResult(
                        SatelliteServiceUtils.fromSatelliteModemState(state));
                int datagramTransferState =
                        SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN;
                switch (state) {
                    case SatelliteManager.SATELLITE_MODEM_STATE_IDLE:
                        datagramTransferState =
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE;
                        break;
                    case SatelliteManager.SATELLITE_MODEM_STATE_LISTENING:
                        datagramTransferState =
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING;
                        break;
                    case SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING:
                        datagramTransferState =
                                SatelliteManager.SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING;
                        break;
                    case SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_RETRYING:
                        // keep previous state as this could be retrying sending or receiving
                        break;
                }
                mDatagramTransferStateChangedRegistrants.notifyResult(datagramTransferState);
            }
        }

        @Override
        public void onNtnSignalStrengthChanged(
                android.telephony.satellite.stub.NtnSignalStrength ntnSignalStrength) {
            if (notifyResultIfExpectedListener()) {
                mNtnSignalStrengthChangedRegistrants.notifyResult(
                        SatelliteServiceUtils.fromNtnSignalStrength(ntnSignalStrength));
            }
        }

        @Override
        public void onSatelliteCapabilitiesChanged(
                android.telephony.satellite.stub.SatelliteCapabilities satelliteCapabilities) {
            mSatelliteCapabilitiesChangedRegistrants.notifyResult(
                    SatelliteServiceUtils.fromSatelliteCapabilities(satelliteCapabilities));
        }

        @Override
        public void onSatelliteSupportedStateChanged(boolean supported) {
            mSatelliteSupportedStateChangedRegistrants.notifyResult(supported);
        }

        @Override
        public void onRegistrationFailure(int causeCode) {
            mSatelliteRegistrationFailureRegistrants.notifyResult(causeCode);
        }

        @Override
        public void onTerrestrialNetworkAvailableChanged(boolean isAvailable) {
            mTerrestrialNetworkAvailableChangedRegistrants.notifyResult(isAvailable);
        }

        private boolean notifyResultIfExpectedListener() {
            // Demo listener should notify results only during demo mode
            // Vendor listener should notify result only during real mode
            return mIsDemoListener == mSatelliteController.isDemoModeEnabled();
        }

        private boolean notifyModemStateChanged(int state) {
            if (notifyResultIfExpectedListener()) {
                return true;
            }

            return state == SatelliteModemState.SATELLITE_MODEM_STATE_OFF
                    || state == SatelliteModemState.SATELLITE_MODEM_STATE_UNAVAILABLE;
        }
    }

    /**
     * @return The singleton instance of SatelliteModemInterface.
     */
    public static SatelliteModemInterface getInstance() {
        if (sInstance == null) {
            loge("SatelliteModemInterface was not yet initialized.");
        }
        return sInstance;
    }

    /**
     * Create the SatelliteModemInterface singleton instance.
     * @param context The Context to use to create the SatelliteModemInterface.
     * @param satelliteController The singleton instance of SatelliteController.
     * @param featureFlags The telephony feature flags.
     * @return The singleton instance of SatelliteModemInterface.
     */
    public static SatelliteModemInterface make(@NonNull Context context,
            SatelliteController satelliteController,
            @NonNull FeatureFlags featureFlags) {
        if (sInstance == null) {
            sInstance = new SatelliteModemInterface(
                    context, satelliteController, Looper.getMainLooper(), featureFlags);
        }
        return sInstance;
    }

    /**
     * Create a SatelliteModemInterface to manage connections to the SatelliteService.
     *
     * @param context The Context for the SatelliteModemInterface.
     * @param featureFlags The telephony feature flags.
     * @param looper The Looper to run binding retry on.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected SatelliteModemInterface(@NonNull Context context,
            SatelliteController satelliteController,
            @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags) {
        mPersistentLogger = SatelliteServiceUtils.getPersistentLogger(context);
        mContext = context;
        mDemoSimulator = DemoSimulator.make(context, satelliteController);
        mVendorListener = new SatelliteListener(false);
        mDemoListener = new SatelliteListener(true);
        mIsSatelliteServiceSupported = getSatelliteServiceSupport();
        mSatelliteController = satelliteController;
        mExponentialBackoff = new ExponentialBackoff(REBIND_INITIAL_DELAY, REBIND_MAXIMUM_DELAY,
                REBIND_MULTIPLIER, looper, () -> {
            synchronized (mLock) {
                if ((mIsBound && mSatelliteService != null) || mIsBinding) {
                    return;
                }
            }
            if (mSatelliteServiceConnection != null) {
                synchronized (mLock) {
                    mIsBound = false;
                    mIsBinding = false;
                }
                unbindService();
            }
            bindService();
        });
        mExponentialBackoff.start();
        plogd("Created SatelliteModemInterface. Attempting to bind to SatelliteService.");
        bindService();
    }

    /**
     * Get the SatelliteService interface, if it exists.
     *
     * @return The bound ISatellite, or {@code null} if it is not yet connected.
     */
    @Nullable public ISatellite getService() {
        return mSatelliteService;
    }

    @NonNull private String getSatellitePackageName() {
        if (!TextUtils.isEmpty(mVendorSatellitePackageName)) {
            return mVendorSatellitePackageName;
        }
        return TextUtils.emptyIfNull(mContext.getResources().getString(
                R.string.config_satellite_service_package));
    }

    private boolean getSatelliteServiceSupport() {
        return !TextUtils.isEmpty(getSatellitePackageName());
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected void bindService() {
        synchronized (mLock) {
            if (mIsBinding || mIsBound) return;
            mIsBinding = true;
        }
        String packageName = getSatellitePackageName();
        if (TextUtils.isEmpty(packageName)) {
            ploge("Unable to bind to the satellite service because the package is undefined.");
            // Since the package name comes from static device configs, stop retry because
            // rebind will continue to fail without a valid package name.
            synchronized (mLock) {
                mIsBinding = false;
            }
            mExponentialBackoff.stop();
            return;
        }
        Intent intent = new Intent(SatelliteService.SERVICE_INTERFACE);
        intent.setPackage(packageName);

        mSatelliteServiceConnection = new SatelliteServiceConnection();
        plogd("Binding to " + packageName);
        try {
            boolean success = mContext.bindService(
                    intent, mSatelliteServiceConnection, Context.BIND_AUTO_CREATE);
            if (success) {
                plogd("Successfully bound to the satellite service.");
            } else {
                synchronized (mLock) {
                    mIsBinding = false;
                }
                mExponentialBackoff.notifyFailed();
                ploge("Error binding to the satellite service. Retrying in "
                        + mExponentialBackoff.getCurrentDelay() + " ms.");
            }
        } catch (Exception e) {
            synchronized (mLock) {
                mIsBinding = false;
            }
            mExponentialBackoff.notifyFailed();
            ploge("Exception binding to the satellite service. Retrying in "
                    + mExponentialBackoff.getCurrentDelay() + " ms. Exception: " + e);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected void unbindService() {
        disconnectSatelliteService();
        mContext.unbindService(mSatelliteServiceConnection);
        mSatelliteServiceConnection = null;
    }

    private void disconnectSatelliteService() {
        // TODO: clean up any listeners and return failed for pending callbacks
        mSatelliteService = null;
    }

    private class SatelliteServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            plogd("onServiceConnected: ComponentName=" + name);
            synchronized (mLock) {
                mIsBound = true;
                mIsBinding = false;
            }
            mSatelliteService = ISatellite.Stub.asInterface(service);
            mExponentialBackoff.stop();
            try {
                mSatelliteService.setSatelliteListener(mVendorListener);
                mDemoSimulator.setSatelliteListener(mDemoListener);
            } catch (RemoteException e) {
                // TODO: Retry setSatelliteListener
                plogd("setSatelliteListener: RemoteException " + e);
            }
            mSatelliteController.onSatelliteServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ploge("onServiceDisconnected: Waiting for reconnect.");
            synchronized (mLock) {
                mIsBinding = false;
            }
            // Since we are still technically bound, clear the service and wait for reconnect.
            disconnectSatelliteService();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            ploge("onBindingDied: Unbinding and rebinding service.");
            synchronized (mLock) {
                mIsBound = false;
                mIsBinding = false;
            }
            unbindService();
            mExponentialBackoff.start();
        }
    }

    /**
     * Registers for satellite position info changed from satellite modem.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSatellitePositionInfoChanged(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mSatellitePositionInfoChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for satellite position info changed from satellite modem.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSatellitePositionInfoChanged(@NonNull Handler h) {
        mSatellitePositionInfoChangedRegistrants.remove(h);
    }

    /**
     * Registers for datagram transfer state changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForDatagramTransferStateChanged(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mDatagramTransferStateChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for datagram transfer state changed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForDatagramTransferStateChanged(@NonNull Handler h) {
        mDatagramTransferStateChangedRegistrants.remove(h);
    }

    /**
     * Registers for modem state changed from satellite modem.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSatelliteModemStateChanged(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mSatelliteModemStateChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for modem state changed from satellite modem.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSatelliteModemStateChanged(@NonNull Handler h) {
        mSatelliteModemStateChangedRegistrants.remove(h);
    }

    /**
     * Registers for pending datagrams indication from satellite modem.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForPendingDatagrams(@NonNull Handler h, int what, @Nullable Object obj) {
        mPendingDatagramsRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for pending datagrams indication from satellite modem.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForPendingDatagrams(@NonNull Handler h) {
        mPendingDatagramsRegistrants.remove(h);
    }

    /**
     * Registers for new datagrams received from satellite modem.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSatelliteDatagramsReceived(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mSatelliteDatagramsReceivedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for new datagrams received from satellite modem.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSatelliteDatagramsReceived(@NonNull Handler h) {
        mSatelliteDatagramsReceivedRegistrants.remove(h);
    }

    /**
     * Registers for non-terrestrial signal strength level changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForNtnSignalStrengthChanged(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mNtnSignalStrengthChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for non-terrestrial signal strength level changed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForNtnSignalStrengthChanged(@NonNull Handler h) {
        mNtnSignalStrengthChangedRegistrants.remove(h);
    }

    /**
     * Registers for satellite capabilities changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSatelliteCapabilitiesChanged(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mSatelliteCapabilitiesChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for satellite capabilities changed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSatelliteCapabilitiesChanged(@NonNull Handler h) {
        mSatelliteCapabilitiesChangedRegistrants.remove(h);
    }

    /**
     * Registers for the satellite supported state changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSatelliteSupportedStateChanged(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mSatelliteSupportedStateChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for the satellite supported state changed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSatelliteSupportedStateChanged(@NonNull Handler h) {
        mSatelliteSupportedStateChangedRegistrants.remove(h);
    }

    /**
     * Registers for the satellite registration failed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSatelliteRegistrationFailure(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mSatelliteRegistrationFailureRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for the satellite registration failed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSatelliteRegistrationFailure(@NonNull Handler h) {
        mSatelliteRegistrationFailureRegistrants.remove(h);
    }

    /**
     * Registers for the terrestrial network available changed.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForTerrestrialNetworkAvailableChanged(
            @NonNull Handler h, int what, @Nullable Object obj) {
        mTerrestrialNetworkAvailableChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregisters for the terrestrial network available changed.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForTerrestrialNetworkAvailableChanged(@NonNull Handler h) {
        mTerrestrialNetworkAvailableChangedRegistrants.remove(h);
    }

    /**
     * Request to enable or disable the satellite service listening mode.
     * Listening mode allows the satellite service to listen for incoming pages.
     *
     * @param enable True to enable satellite listening mode and false to disable.
     * @param timeout How long the satellite modem should wait for the next incoming page before
     *                disabling listening mode.
     * @param message The Message to send to result of the operation to.
     */
    public void requestSatelliteListeningEnabled(boolean enable, int timeout,
            @Nullable Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestSatelliteListeningEnabled(enable, timeout,
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("requestSatelliteListeningEnabled: " + error);
                                Binder.withCleanCallingIdentity(() -> {
                                    if (message != null) {
                                        sendMessageWithResult(message, null, error);
                                    }
                                });
                            }
                        });
            } catch (RemoteException e) {
                ploge("requestSatelliteListeningEnabled: RemoteException " + e);
                if (message != null) {
                    sendMessageWithResult(
                            message, null, SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
                }
            }
        } else {
            ploge("requestSatelliteListeningEnabled: Satellite service is unavailable.");
            if (message != null) {
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
            }
        }
    }

    /**
     * Allow cellular modem scanning while satellite mode is on.
     * @param enabled  {@code true} to enable cellular modem while satellite mode is on
     * and {@code false} to disable
     * @param message The Message to send to result of the operation to.
     */
    public void enableCellularModemWhileSatelliteModeIsOn(boolean enabled,
            @Nullable Message message) {
        if (mSatelliteService != null) {
            try {
                IIntegerConsumer errorCallback = new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("enableCellularModemWhileSatelliteModeIsOn: " + error);
                        Binder.withCleanCallingIdentity(() -> {
                            if (message != null) {
                                sendMessageWithResult(message, null, error);
                            }
                        });
                    }
                };

                if (mSatelliteController.isDemoModeEnabled()) {
                    mDemoSimulator.enableTerrestrialNetworkScanWhileSatelliteModeIsOn(
                            enabled, errorCallback);
                } else {
                    mSatelliteService.enableTerrestrialNetworkScanWhileSatelliteModeIsOn(
                            enabled, errorCallback);
                }
            } catch (RemoteException e) {
                ploge("enableTerrestrialNetworkScanWhileSatelliteModeIsOn: RemoteException " + e);
                if (message != null) {
                    sendMessageWithResult(
                            message, null, SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
                }
            }
        } else {
            ploge("enableCellularModemWhileSatelliteModeIsOn: Satellite service is unavailable.");
            if (message != null) {
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
            }
        }
    }
    /**
     * Request to enable or disable the satellite modem and demo mode. If the satellite modem
     * is enabled, this may also disable the cellular modem, and if the satellite modem is disabled,
     * this may also re-enable the cellular modem.
     *
     * @param enableAttributes info needed to allow carrier to roam to satellite.
     * @param message The Message to send to result of the operation to.
     */
    public void requestSatelliteEnabled(SatelliteModemEnableRequestAttributes enableAttributes,
            @NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestSatelliteEnabled(SatelliteServiceUtils
                        .toSatelliteModemEnableRequestAttributes(enableAttributes),
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("setSatelliteEnabled: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                });
            } catch (RemoteException e) {
                ploge("setSatelliteEnabled: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("setSatelliteEnabled: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Request to get whether the satellite modem is enabled.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void requestIsSatelliteEnabled(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestIsSatelliteEnabled(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("requestIsSatelliteEnabled: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                }, new IBooleanConsumer.Stub() {
                    @Override
                    public void accept(boolean result) {
                        // Convert for compatibility with SatelliteResponse
                        // TODO: This should just report result instead.
                        int[] enabled = new int[] {result ? 1 : 0};
                        plogd("requestIsSatelliteEnabled: " + Arrays.toString(enabled));
                        Binder.withCleanCallingIdentity(() -> sendMessageWithResult(
                                message, enabled, SatelliteManager.SATELLITE_RESULT_SUCCESS));
                    }
                });
            } catch (RemoteException e) {
                ploge("requestIsSatelliteEnabled: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestIsSatelliteEnabled: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Request to get whether the satellite service is supported on the device.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void requestIsSatelliteSupported(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestIsSatelliteSupported(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("requestIsSatelliteSupported: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                }, new IBooleanConsumer.Stub() {
                    @Override
                    public void accept(boolean result) {
                        plogd("requestIsSatelliteSupported: " + result);
                        Binder.withCleanCallingIdentity(() -> sendMessageWithResult(
                                message, result, SatelliteManager.SATELLITE_RESULT_SUCCESS));
                    }
                });
            } catch (RemoteException e) {
                ploge("requestIsSatelliteSupported: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestIsSatelliteSupported: Satellite service is unavailable.");
            sendMessageWithResult(
                    message, null, SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Request to get the SatelliteCapabilities of the satellite service.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void requestSatelliteCapabilities(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestSatelliteCapabilities(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("requestSatelliteCapabilities: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                }, new ISatelliteCapabilitiesConsumer.Stub() {
                    @Override
                    public void accept(android.telephony.satellite.stub.SatelliteCapabilities
                            result) {
                        SatelliteCapabilities capabilities =
                                SatelliteServiceUtils.fromSatelliteCapabilities(result);
                        plogd("requestSatelliteCapabilities: " + capabilities);
                        Binder.withCleanCallingIdentity(() -> sendMessageWithResult(
                                message, capabilities, SatelliteManager.SATELLITE_RESULT_SUCCESS));
                    }
                });
            } catch (RemoteException e) {
                ploge("requestSatelliteCapabilities: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestSatelliteCapabilities: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * User started pointing to the satellite.
     * The satellite service should report the satellite pointing info via
     * ISatelliteListener#onSatellitePositionChanged as the user device/satellite moves.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void startSendingSatellitePointingInfo(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.startSendingSatellitePointingInfo(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("startSendingSatellitePointingInfo: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                });
            } catch (RemoteException e) {
                ploge("startSendingSatellitePointingInfo: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("startSendingSatellitePointingInfo: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * User stopped pointing to the satellite.
     * The satellite service should stop reporting satellite pointing info to the framework.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void stopSendingSatellitePointingInfo(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.stopSendingSatellitePointingInfo(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("stopSendingSatellitePointingInfo: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                });
            } catch (RemoteException e) {
                ploge("stopSendingSatellitePointingInfo: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("stopSendingSatellitePointingInfo: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Poll the pending datagrams to be received over satellite.
     * The satellite service should check if there are any pending datagrams to be received over
     * satellite and report them via ISatelliteListener#onSatelliteDatagramsReceived.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void pollPendingSatelliteDatagrams(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.pollPendingSatelliteDatagrams(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("pollPendingDatagrams: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                });
            } catch (RemoteException e) {
                ploge("pollPendingDatagrams: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("pollPendingDatagrams: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Send datagram over satellite.
     *
     * @param datagram Datagram to send in byte format.
     * @param isEmergency Whether this is an emergency datagram.
     * @param needFullScreenPointingUI this is used to indicate pointingUI app to open in
     *                                 full screen mode.
     * @param message The Message to send to result of the operation to.
     */
    public void sendSatelliteDatagram(@NonNull SatelliteDatagram datagram, boolean isEmergency,
            boolean needFullScreenPointingUI, @NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.sendSatelliteDatagram(
                        SatelliteServiceUtils.toSatelliteDatagram(datagram), isEmergency,
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("sendDatagram: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        });
            } catch (RemoteException e) {
                ploge("sendDatagram: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("sendDatagram: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Request the current satellite modem state.
     * The satellite service should report the current satellite modem state via
     * ISatelliteListener#onSatelliteModemStateChanged.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void requestSatelliteModemState(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestSatelliteModemState(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("requestSatelliteModemState: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                }, new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        // Convert SatelliteModemState from service to frameworks definition.
                        int modemState = SatelliteServiceUtils.fromSatelliteModemState(result);
                        plogd("requestSatelliteModemState: " + modemState);
                        Binder.withCleanCallingIdentity(() -> sendMessageWithResult(
                                message, modemState, SatelliteManager.SATELLITE_RESULT_SUCCESS));
                    }
                });
            } catch (RemoteException e) {
                ploge("requestSatelliteModemState: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestSatelliteModemState: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Request to get the time after which the satellite will be visible. This is an int
     * representing the duration in seconds after which the satellite will be visible.
     * This will return 0 if the satellite is currently visible.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void requestTimeForNextSatelliteVisibility(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestTimeForNextSatelliteVisibility(
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("requestTimeForNextSatelliteVisibility: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        }, new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                // Convert for compatibility with SatelliteResponse
                                // TODO: This should just report result instead.
                                int[] time = new int[] {result};
                                plogd("requestTimeForNextSatelliteVisibility: "
                                        + Arrays.toString(time));
                                Binder.withCleanCallingIdentity(() -> sendMessageWithResult(
                                        message, time, SatelliteManager.SATELLITE_RESULT_SUCCESS));
                            }
                        });
            } catch (RemoteException e) {
                ploge("requestTimeForNextSatelliteVisibility: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestTimeForNextSatelliteVisibility: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Set the non-terrestrial PLMN with lower priority than terrestrial networks.
     * MCC/MNC broadcast by the non-terrestrial networks will not be included in OPLMNwACT file
     * on SIM profile.
     * Acquisition of satellite based system is deemed lower priority to terrestrial networks.
     * Even so, UE shall make all attempts to acquire terrestrial service prior to camping on
     * satellite LTE service.
     *
     * @param simSlot Indicates the SIM slot to which this API will be applied. The modem will use
     *                this information to determine the relevant carrier.
     * @param carrierPlmnList The list of roaming PLMN used for connecting to satellite networks
     *                        supported by user subscription.
     * @param allSatellitePlmnList Modem should use the allSatellitePlmnList to identify satellite
     *                             PLMNs that are not supported by the carrier and make sure not to
     *                             attach to them.
     * @param message The result receiver that returns whether the modem has
     *                successfully set the satellite PLMN
     */
    public void setSatellitePlmn(@NonNull int simSlot, @NonNull List<String> carrierPlmnList,
            @NonNull List<String> allSatellitePlmnList, @NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.setSatellitePlmn(simSlot, carrierPlmnList, allSatellitePlmnList,
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("setSatellitePlmn: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        });
            } catch (RemoteException e) {
                ploge("setSatellitePlmn: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("setSatellitePlmn: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Enable or disable satellite in the cellular modem associated with a carrier.
     * Refer setSatellitePlmn for the details of satellite PLMN scanning process.
     *
     * @param simSlot Indicates the SIM slot to which this API will be applied. The modem will use
     *                this information to determine the relevant carrier.
     * @param enableSatellite True to enable the satellite modem and false to disable.
     * @param message The Message to send to result of the operation to.
     */
    public void requestSetSatelliteEnabledForCarrier(@NonNull int simSlot,
            @NonNull boolean enableSatellite, @NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.setSatelliteEnabledForCarrier(simSlot, enableSatellite,
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("requestSetSatelliteEnabledForCarrier: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        });
            } catch (RemoteException e) {
                ploge("requestSetSatelliteEnabledForCarrier: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestSetSatelliteEnabledForCarrier: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED);
        }
    }

    /**
     * Check whether satellite is enabled in the cellular modem associated with a carrier.
     *
     * @param simSlot Indicates the SIM slot to which this API will be applied. The modem will use
     *                this information to determine the relevant carrier.
     * @param message The Message to send to result of the operation to.
     */
    public void requestIsSatelliteEnabledForCarrier(@NonNull int simSlot,
            @NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestIsSatelliteEnabledForCarrier(simSlot,
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("requestIsSatelliteEnabledForCarrier: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        }, new IBooleanConsumer.Stub() {
                            @Override
                            public void accept(boolean result) {
                                // Convert for compatibility with SatelliteResponse
                                // TODO: This should just report result instead.
                                int[] enabled = new int[] {result ? 1 : 0};
                                plogd("requestIsSatelliteEnabledForCarrier: "
                                        + Arrays.toString(enabled));
                                Binder.withCleanCallingIdentity(() -> sendMessageWithResult(
                                        message, enabled,
                                        SatelliteManager.SATELLITE_RESULT_SUCCESS));
                            }
                        });
            } catch (RemoteException e) {
                ploge("requestIsSatelliteEnabledForCarrier: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestIsSatelliteEnabledForCarrier: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * Request to get the signal strength of the satellite connection.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void requestNtnSignalStrength(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.requestSignalStrength(
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("requestNtnSignalStrength: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        }, new INtnSignalStrengthConsumer.Stub() {
                            @Override
                            public void accept(
                                    android.telephony.satellite.stub.NtnSignalStrength result) {
                                NtnSignalStrength ntnSignalStrength =
                                        SatelliteServiceUtils.fromNtnSignalStrength(result);
                                plogd("requestNtnSignalStrength: " + ntnSignalStrength);
                                Binder.withCleanCallingIdentity(() -> sendMessageWithResult(
                                        message, ntnSignalStrength,
                                        SatelliteManager.SATELLITE_RESULT_SUCCESS));
                            }
                        });
            } catch (RemoteException e) {
                ploge("requestNtnSignalStrength: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("requestNtnSignalStrength: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * The satellite service should report the NTN signal strength via
     * ISatelliteListener#onNtnSignalStrengthChanged when the NTN signal strength changes.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void startSendingNtnSignalStrength(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.startSendingNtnSignalStrength(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("startSendingNtnSignalStrength: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                });
            } catch (RemoteException e) {
                ploge("startSendingNtnSignalStrength: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("startSendingNtnSignalStrength: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * The satellite service should stop reporting NTN signal strength to the framework.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void stopSendingNtnSignalStrength(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.stopSendingNtnSignalStrength(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("stopSendingNtnSignalStrength: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                });
            } catch (RemoteException e) {
                ploge("stopSendingNtnSignalStrength: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("stopSendingNtnSignalStrength: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * The satellite service should abort all datagram-sending requests.
     *
     * @param message The Message to send to result of the operation to.
     */
    public void abortSendingSatelliteDatagrams(@NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.abortSendingSatelliteDatagrams(new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {
                        int error = SatelliteServiceUtils.fromSatelliteError(result);
                        plogd("abortSendingSatelliteDatagrams: " + error);
                        Binder.withCleanCallingIdentity(() ->
                                sendMessageWithResult(message, null, error));
                    }
                });
            } catch (RemoteException e) {
                ploge("abortSendingSatelliteDatagrams: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("abortSendingSatelliteDatagrams: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    public boolean isSatelliteServiceSupported() {
        return mIsSatelliteServiceSupported;
    }

    /** Check if vendor satellite service is connected */
    public boolean isSatelliteServiceConnected() {
        synchronized (mLock) {
            return (mSatelliteService != null);
        }
    }

    /**
     * Provision UUID with a satellite provider.
     */
    public void updateSatelliteSubscription(@NonNull String iccId, @NonNull Message message) {
        if (mSatelliteService != null) {
            try {
                mSatelliteService.updateSatelliteSubscription(iccId,
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("updateSatelliteSubscription: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        });
            } catch (RemoteException e) {
                ploge("updateSatelliteSubscription: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("updateSatelliteSubscription: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    /**
     * This API can be used by only CTS to update satellite vendor service package name.
     *
     * @param servicePackageName The package name of the satellite vendor service.
     * @return {@code true} if the satellite vendor service is set successfully,
     * {@code false} otherwise.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setSatelliteServicePackageName(@Nullable String servicePackageName) {
        plogd("setSatelliteServicePackageName: config_satellite_service_package is "
                + "updated, new packageName=" + servicePackageName);
        mExponentialBackoff.stop();
        if (mSatelliteServiceConnection != null) {
            synchronized (mLock) {
                mIsBound = false;
                mIsBinding = false;
            }
            unbindService();
        }

        if (servicePackageName == null || servicePackageName.equals("null")) {
            mVendorSatellitePackageName = "";
        } else {
            mVendorSatellitePackageName = servicePackageName;
        }
        mIsSatelliteServiceSupported = getSatelliteServiceSupport();
        bindService();
        mExponentialBackoff.start();
    }

    /**
     * Request to update system selection channels
     *
     * @param systemSelectionSpecifiers system selection specifiers
     * @param message The Message to send to result of the operation to.
     */
    public void updateSystemSelectionChannels(
            @NonNull List<SystemSelectionSpecifier> systemSelectionSpecifiers,
            @Nullable Message message) {
        plogd("updateSystemSelectionChannels: SystemSelectionSpecifier: "
                + systemSelectionSpecifiers.toString());
        if (mSatelliteService != null) {
            try {
                mSatelliteService.updateSystemSelectionChannels(SatelliteServiceUtils
                                .toSystemSelectionSpecifier(systemSelectionSpecifiers),
                        new IIntegerConsumer.Stub() {
                            @Override
                            public void accept(int result) {
                                int error = SatelliteServiceUtils.fromSatelliteError(result);
                                plogd("updateSystemSelectionChannels: " + error);
                                Binder.withCleanCallingIdentity(() ->
                                        sendMessageWithResult(message, null, error));
                            }
                        });
            } catch (RemoteException e) {
                ploge("updateSystemSelectionChannels: RemoteException " + e);
                sendMessageWithResult(message, null,
                        SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR);
            }
        } else {
            ploge("updateSystemSelectionChannels: Satellite service is unavailable.");
            sendMessageWithResult(message, null,
                    SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected static void sendMessageWithResult(@NonNull Message message, @Nullable Object result,
            @SatelliteManager.SatelliteResult int error) {
        SatelliteException exception = error == SatelliteManager.SATELLITE_RESULT_SUCCESS
                ? null : new SatelliteException(error);
        AsyncResult.forMessage(message, result, exception);
        message.sendToTarget();
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }

    private void plogd(@NonNull String log) {
        Rlog.d(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.debug(TAG, log);
        }
    }

    private void ploge(@NonNull String log) {
        Rlog.e(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.error(TAG, log);
        }
    }
}
