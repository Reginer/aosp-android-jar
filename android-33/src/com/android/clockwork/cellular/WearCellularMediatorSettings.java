package com.android.clockwork.cellular;

import static com.android.clockwork.cellular.WearCellularMediator.CELL_AUTO_OFF;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import com.google.android.clockwork.signaldetector.SignalDetectorSettings;

import java.util.concurrent.TimeUnit;

public class WearCellularMediatorSettings implements SignalDetectorSettings {

    private static final String MOBILE_SIGNAL_DETECTOR_QUEUE_MAX_SIZE_KEY =
            "mobile_signal_detector_queue_max_size";
    private static final int MOBILE_SIGNAL_DETECTOR_QUEUE_MAX_SIZE_DEFAULT = 50;

    private static final String MOBILE_SIGNAL_DETECTOR_INTERVAL_MS_KEY =
            "mobile_signal_detector_interval_ms";
    private static final long MOBILE_SIGNAL_DETECTOR_INTERVAL_MS_DEFAULT =
            TimeUnit.MINUTES.toMillis(12);

    private static final String MOBILE_SIGNAL_DETECTOR_BATTERY_DROP_THRESHOLD_KEY =
            "mobile_signal_detector_battery_drop_threshold";
    private static final int MOBILE_SIGNAL_DETECTOR_BATTERY_DROP_THRESHOLD_DEFAULT = 2;

    private static final String MOBILE_SIGNAL_DETECTOR_FREQUENT_EVENT_NUM_KEY =
            "mobile_signal_detector_frequent_event_num";
    private static final int MOBILE_SIGNAL_DETECTOR_FREQUENT_EVENT_NUM_DEFAULT = 20;

    private static final String CELLULAR_OFF_DURING_POWER_SAVE_KEY =
            "cellular_mediator_off_during_power_save";
    private static final int CELLULAR_OFF_DURING_POWER_SAVE_DEFAULT = 1 /* true */;

    private static final String ESIM_PROFILE_ACTIVATION_STATE_KEY =
            "cw_esim_profile_activation_state";
    static final Uri ESIM_PROFILE_ACTIVATION_SETTING_URI =
            Settings.Global.getUriFor(ESIM_PROFILE_ACTIVATION_STATE_KEY);
    private static final int ESIM_PROFILE_ACTIVATION_STATE_DEFAULT = 1 /* enabled */;
    /** Value of the twinning-related globals when they are on. */
    private static final int STATE_ON = 1;

    /** Value of the twinning-related globals when they are off. */
    private static final int STATE_OFF = 0;

    /* if voice twinning is disabled, cell auto is disabled as well */
    private static final String VOICE_TWINNING_GLOBAL_SETTINGS_KEY =
            "call_twinning_state";
    static final Uri VOICE_TWINNING_SETTING_URI =
            Settings.Global.getUriFor(VOICE_TWINNING_GLOBAL_SETTINGS_KEY);
    /* voice twinning and hence cell auto is enabled by default */
    private static final int VOICE_TWINNING_SETTING_DEFAULT = STATE_ON;
    private static final String TEXT_TWINNING_GLOBAL_SETTINGS_KEY =
            "text_message_twinning_state";
    static final Uri TEXT_TWINNING_SETTING_URI =
            Settings.Global.getUriFor(TEXT_TWINNING_GLOBAL_SETTINGS_KEY);
    private static final int TEXT_TWINNING_SETTING_DEFAULT = STATE_OFF;
    /**
     * A Global Setting to record whether the LPA is currently in Test Mode.
     * Settings currently relies on this to display the correct UI.
     */
    private static final String ESIM_TEST_MODE_GLOBAL_SETTINGS_KEY =
            "cw_esim_test_mode";
    /* may as well use the twinning constants for on/off here as well */
    private static final int ESIM_TEST_MODE_SETTING_DEFAULT = STATE_OFF;

    private static final String PRODUCT_NAME = "ro.product.name";
    private static final String CARRIER_NAME = "ro.carrier";
    private static final String VERIZON_SUFFIX = "_vz";
    private static final String VERIZON_NAME = "verizon";

    private final Context mContext;
    private final boolean mIsLocalEditionDevice;
    private final String mSimOperator;
    private final boolean mCellAutoEnabled;

    /** If this is a WearOS-provisioned eSIM device. */
    private final boolean mIsWearEsimDevice;

    /**
     * @param context     the application context.
     * @param simOperator the sim operator currently in use for the device.
     */
    public WearCellularMediatorSettings(Context context, boolean isLocalEditionDevice,
            String simOperator) {
        mContext = context;
        mIsLocalEditionDevice = isLocalEditionDevice;
        mSimOperator = simOperator;
        mCellAutoEnabled =
                SystemProperties.getBoolean("config.enable_cellmediator_cell_auto", false);
        mIsWearEsimDevice =
                context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_TELEPHONY_EUICC) &&
                        WearResourceUtil.getWearableResources(mContext).getBoolean(
                                com.android.wearable.resources.R.bool.config_wearEsimDevice);
    }

    /**
     * Get the value of Settings.System.CELL_AUTO_SETTING_KEY.
     */
    public int getCellAutoSetting() {
        return mCellAutoEnabled ?
                Settings.System.getInt(
                        mContext.getContentResolver(),
                        WearCellularMediator.CELL_AUTO_SETTING_KEY,
                        WearCellularMediator.CELL_AUTO_SETTING_DEFAULT)
                : CELL_AUTO_OFF;
    }

    public void setCellAutoSetting(int cellAutoSetting) {
        Settings.System.putInt(
                mContext.getContentResolver(),
                WearCellularMediator.CELL_AUTO_SETTING_KEY,
                cellAutoSetting);
    }

    /**
     * Initializes default twinning settings by actually committing a value
     * into Settings, to ensure that apps depending on this Setting are
     * correctly using the default values defined here.
     */
    public void initializeTwinningSettings() {
        if (!mIsWearEsimDevice) {
            return;
        }
        int voiceSetting = Settings.Global.getInt(
                mContext.getContentResolver(),
                VOICE_TWINNING_GLOBAL_SETTINGS_KEY,
                VOICE_TWINNING_SETTING_DEFAULT);
        int textSetting = Settings.Global.getInt(
                mContext.getContentResolver(),
                TEXT_TWINNING_GLOBAL_SETTINGS_KEY,
                TEXT_TWINNING_SETTING_DEFAULT);
        Settings.Global.putInt(
                mContext.getContentResolver(),
                VOICE_TWINNING_GLOBAL_SETTINGS_KEY,
                voiceSetting);
        Settings.Global.putInt(
                mContext.getContentResolver(),
                TEXT_TWINNING_GLOBAL_SETTINGS_KEY,
                textSetting);
    }

    public boolean isVoiceTwinningEnabled() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                VOICE_TWINNING_GLOBAL_SETTINGS_KEY,
                VOICE_TWINNING_SETTING_DEFAULT) == STATE_ON;
    }

    public boolean isTextTwinningEnabled() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                TEXT_TWINNING_GLOBAL_SETTINGS_KEY,
                TEXT_TWINNING_SETTING_DEFAULT) == STATE_ON;
    }

    public boolean getEsimTestModeState() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                ESIM_TEST_MODE_GLOBAL_SETTINGS_KEY,
                ESIM_TEST_MODE_SETTING_DEFAULT) == STATE_ON;
    }

    public void setEsimTestModeState(boolean isInEsimTestMode) {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                ESIM_TEST_MODE_GLOBAL_SETTINGS_KEY,
                isInEsimTestMode ? STATE_ON : STATE_OFF);
    }

    /**
     * Get the value of Settings.Global.CELL_ON.
     * The default value is CELL_ON_FLAG if not defined because we assume this service only runs on
     * the cellular-capable device (behind the config.enable_cellmediator flag in SystemServer).
     */
    public int getCellState() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.CELL_ON,
                PhoneConstants.CELL_ON_FLAG);
    }

    public boolean isLocalEditionDevice() {
        return mIsLocalEditionDevice;
    }

    public boolean isWearEsimDevice() {
        return mIsWearEsimDevice;
    }

    public boolean isEsimProfileDeactivated() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                ESIM_PROFILE_ACTIVATION_STATE_KEY,
                ESIM_PROFILE_ACTIVATION_STATE_DEFAULT) == 0;
    }

    @Override
    public boolean getMobileSignalDetectorAllowed() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.Wearable.MOBILE_SIGNAL_DETECTOR,
                0)
                == 1;
    }

    @Override
    public int getMobileSignalDetectorQueueMaxSize() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                MOBILE_SIGNAL_DETECTOR_QUEUE_MAX_SIZE_KEY,
                MOBILE_SIGNAL_DETECTOR_QUEUE_MAX_SIZE_DEFAULT);
    }

    @Override
    public long getMobileSignalDetectorIntervalMs() {
        return Settings.Global.getLong(
                mContext.getContentResolver(),
                MOBILE_SIGNAL_DETECTOR_INTERVAL_MS_KEY,
                MOBILE_SIGNAL_DETECTOR_INTERVAL_MS_DEFAULT);
    }

    @Override
    public int getMobileSignalDetectorBatteryDropThreshold() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                MOBILE_SIGNAL_DETECTOR_BATTERY_DROP_THRESHOLD_KEY,
                MOBILE_SIGNAL_DETECTOR_BATTERY_DROP_THRESHOLD_DEFAULT);
    }

    @Override
    public int getMobileSignalDetectorFrequentEventNum() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                MOBILE_SIGNAL_DETECTOR_FREQUENT_EVENT_NUM_KEY,
                MOBILE_SIGNAL_DETECTOR_FREQUENT_EVENT_NUM_DEFAULT);
    }

    @Override
    public boolean isDebugMode() {
        return false;
    }

    /**
     * Do not disable cell in power save mode for Verizon.
     * More details in b/34507932.
     */
    public boolean shouldTurnCellularOffDuringPowerSave() {
        int cellularDuringPowerSaveSetting =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        CELLULAR_OFF_DURING_POWER_SAVE_KEY,
                        CELLULAR_OFF_DURING_POWER_SAVE_DEFAULT);
        String productName = SystemProperties.get(PRODUCT_NAME);
        boolean isVerizon =
                (productName != null && productName.endsWith(VERIZON_SUFFIX))
                        || TextUtils.equals(VERIZON_NAME, SystemProperties.get(CARRIER_NAME));
        return cellularDuringPowerSaveSetting == 1 && !isVerizon;
    }

    /**
     * For some edge cases (b/35588911), radio power can be turned on inadvertently and
     * the variable to track radio power state doesn't get updated.
     * Note TelephonyManager.isRadioOn() is not used because in cases like b/35588911, it uses
     * the wrong default subscription id.
     *
     * @return RADIO_ON_STATE_UNKNOWN, RADIO_ON_STATE_ON, or RADIO_ON_STATE_OFF.
     */
    public int getRadioOnState() {
        // The trick to get subId is used in TelephonyManager#getSimOperatorNumeric().
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (!SubscriptionManager.isUsableSubIdValue(subId)) {
            subId = SubscriptionManager.getDefaultSmsSubscriptionId();
            if (!SubscriptionManager.isUsableSubIdValue(subId)) {
                subId = SubscriptionManager.getDefaultVoiceSubscriptionId();
                if (!SubscriptionManager.isUsableSubIdValue(subId)) {
                    subId = SubscriptionManager.getDefaultSubscriptionId();
                }
            }
        }

        if (!SubscriptionManager.isUsableSubIdValue(subId)) {
            return WearCellularMediator.RADIO_ON_STATE_UNKNOWN;
        }

        try {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(
                    Context.TELEPHONY_SERVICE));
            if (telephony != null) {
                boolean isRadioOn = telephony.isRadioOnForSubscriber(subId,
                        mContext.getOpPackageName());
                return isRadioOn ? WearCellularMediator.RADIO_ON_STATE_ON
                        : WearCellularMediator.RADIO_ON_STATE_OFF;
            }
        } catch (RemoteException e) {
            Log.e(WearCellularMediator.TAG, "RemoteException calling isRadioOnForSubscriber()", e);
        }

        return WearCellularMediator.RADIO_ON_STATE_UNKNOWN;
    }
}
