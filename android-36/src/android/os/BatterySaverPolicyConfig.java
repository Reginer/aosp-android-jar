/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.os;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.text.TextUtils;
import android.util.ArrayMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Config to set Battery Saver policy flags.
 *
 * @hide
 */
@SystemApi
public final class BatterySaverPolicyConfig implements Parcelable {
    private final float mAdjustBrightnessFactor;
    private final boolean mAdvertiseIsEnabled;
    private final boolean mDeferFullBackup;
    private final boolean mDeferKeyValueBackup;
    @NonNull
    private final Map<String, String> mDeviceSpecificSettings;
    private final boolean mDisableAnimation;
    private final boolean mDisableAod;
    private final boolean mDisableLaunchBoost;
    private final boolean mDisableOptionalSensors;
    private final boolean mDisableVibration;
    private final boolean mEnableAdjustBrightness;
    private final boolean mEnableDataSaver;
    private final boolean mEnableFirewall;
    private final boolean mEnableNightMode;
    private final boolean mEnableQuickDoze;
    private final boolean mForceAllAppsStandby;
    private final boolean mForceBackgroundCheck;
    private final int mLocationMode;
    private final int mSoundTriggerMode;

    private BatterySaverPolicyConfig(Builder in) {
        mAdjustBrightnessFactor = Math.max(0, Math.min(in.mAdjustBrightnessFactor, 1f));
        mAdvertiseIsEnabled = in.mAdvertiseIsEnabled;
        mDeferFullBackup = in.mDeferFullBackup;
        mDeferKeyValueBackup = in.mDeferKeyValueBackup;
        mDeviceSpecificSettings = Collections.unmodifiableMap(
                new ArrayMap<>(in.mDeviceSpecificSettings));
        mDisableAnimation = in.mDisableAnimation;
        mDisableAod = in.mDisableAod;
        mDisableLaunchBoost = in.mDisableLaunchBoost;
        mDisableOptionalSensors = in.mDisableOptionalSensors;
        mDisableVibration = in.mDisableVibration;
        mEnableAdjustBrightness = in.mEnableAdjustBrightness;
        mEnableDataSaver = in.mEnableDataSaver;
        mEnableFirewall = in.mEnableFirewall;
        mEnableNightMode = in.mEnableNightMode;
        mEnableQuickDoze = in.mEnableQuickDoze;
        mForceAllAppsStandby = in.mForceAllAppsStandby;
        mForceBackgroundCheck = in.mForceBackgroundCheck;
        mLocationMode = Math.max(PowerManager.MIN_LOCATION_MODE,
                Math.min(in.mLocationMode, PowerManager.MAX_LOCATION_MODE));
        mSoundTriggerMode = Math.max(PowerManager.MIN_SOUND_TRIGGER_MODE,
                Math.min(in.mSoundTriggerMode, PowerManager.MAX_SOUND_TRIGGER_MODE));
    }

    private BatterySaverPolicyConfig(Parcel in) {
        mAdjustBrightnessFactor = Math.max(0, Math.min(in.readFloat(), 1f));
        mAdvertiseIsEnabled = in.readBoolean();
        mDeferFullBackup = in.readBoolean();
        mDeferKeyValueBackup = in.readBoolean();

        final int size = in.readInt();
        Map<String, String> deviceSpecificSettings = new ArrayMap<>(size);
        for (int i = 0; i < size; ++i) {
            String key = TextUtils.emptyIfNull(in.readString());
            String val = TextUtils.emptyIfNull(in.readString());
            if (key.trim().isEmpty()) {
                continue;
            }
            deviceSpecificSettings.put(key, val);
        }
        mDeviceSpecificSettings = Collections.unmodifiableMap(deviceSpecificSettings);

        mDisableAnimation = in.readBoolean();
        mDisableAod = in.readBoolean();
        mDisableLaunchBoost = in.readBoolean();
        mDisableOptionalSensors = in.readBoolean();
        mDisableVibration = in.readBoolean();
        mEnableAdjustBrightness = in.readBoolean();
        mEnableDataSaver = in.readBoolean();
        mEnableFirewall = in.readBoolean();
        mEnableNightMode = in.readBoolean();
        mEnableQuickDoze = in.readBoolean();
        mForceAllAppsStandby = in.readBoolean();
        mForceBackgroundCheck = in.readBoolean();
        mLocationMode = Math.max(PowerManager.MIN_LOCATION_MODE,
                Math.min(in.readInt(), PowerManager.MAX_LOCATION_MODE));
        mSoundTriggerMode = Math.max(PowerManager.MIN_SOUND_TRIGGER_MODE,
                Math.min(in.readInt(), PowerManager.MAX_SOUND_TRIGGER_MODE));
    }

    public static final @android.annotation.NonNull Creator<BatterySaverPolicyConfig> CREATOR =
            new Creator<BatterySaverPolicyConfig>() {
                @Override
                public BatterySaverPolicyConfig createFromParcel(Parcel in) {
                    return new BatterySaverPolicyConfig(in);
                }

                @Override
                public BatterySaverPolicyConfig[] newArray(int size) {
                    return new BatterySaverPolicyConfig[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mAdjustBrightnessFactor);
        dest.writeBoolean(mAdvertiseIsEnabled);
        dest.writeBoolean(mDeferFullBackup);
        dest.writeBoolean(mDeferKeyValueBackup);

        final Set<Map.Entry<String, String>> entries = mDeviceSpecificSettings.entrySet();
        final int size = entries.size();
        dest.writeInt(size);
        for (Map.Entry<String, String> entry : entries) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }

        dest.writeBoolean(mDisableAnimation);
        dest.writeBoolean(mDisableAod);
        dest.writeBoolean(mDisableLaunchBoost);
        dest.writeBoolean(mDisableOptionalSensors);
        dest.writeBoolean(mDisableVibration);
        dest.writeBoolean(mEnableAdjustBrightness);
        dest.writeBoolean(mEnableDataSaver);
        dest.writeBoolean(mEnableFirewall);
        dest.writeBoolean(mEnableNightMode);
        dest.writeBoolean(mEnableQuickDoze);
        dest.writeBoolean(mForceAllAppsStandby);
        dest.writeBoolean(mForceBackgroundCheck);
        dest.writeInt(mLocationMode);
        dest.writeInt(mSoundTriggerMode);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : mDeviceSpecificSettings.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
        }
        return "adjust_brightness_disabled=" + !mEnableAdjustBrightness + ","
                + "adjust_brightness_factor=" + mAdjustBrightnessFactor + ","
                + "advertise_is_enabled=" + mAdvertiseIsEnabled + ","
                + "animation_disabled=" + mDisableAnimation + ","
                + "aod_disabled=" + mDisableAod + ","
                + "datasaver_disabled=" + !mEnableDataSaver + ","
                + "enable_night_mode=" + mEnableNightMode + ","
                + "firewall_disabled=" + !mEnableFirewall + ","
                + "force_all_apps_standby=" + mForceAllAppsStandby + ","
                + "force_background_check=" + mForceBackgroundCheck + ","
                + "fullbackup_deferred=" + mDeferFullBackup + ","
                + "gps_mode=" + mLocationMode + ","
                + "keyvaluebackup_deferred=" + mDeferKeyValueBackup + ","
                + "launch_boost_disabled=" + mDisableLaunchBoost + ","
                + "optional_sensors_disabled=" + mDisableOptionalSensors + ","
                + "quick_doze_enabled=" + mEnableQuickDoze + ","
                + "soundtrigger_mode=" + mSoundTriggerMode + ","
                + "vibration_disabled=" + mDisableVibration + ","
                + sb.toString();
    }

    /**
     * How much to adjust the screen brightness while in Battery Saver. This will have no effect
     * if {@link #getEnableAdjustBrightness()} is {@code false}.
     */
    public float getAdjustBrightnessFactor() {
        return mAdjustBrightnessFactor;
    }

    /**
     * Whether or not to tell the system (and other apps) that Battery Saver is currently enabled.
     */
    public boolean getAdvertiseIsEnabled() {
        return mAdvertiseIsEnabled;
    }

    /** Whether or not to defer full backup while in Battery Saver. */
    public boolean getDeferFullBackup() {
        return mDeferFullBackup;
    }

    /** Whether or not to defer key-value backup while in Battery Saver. */
    public boolean getDeferKeyValueBackup() {
        return mDeferKeyValueBackup;
    }

    /**
     * Returns the device-specific battery saver constants.
     */
    @NonNull
    public Map<String, String> getDeviceSpecificSettings() {
        return mDeviceSpecificSettings;
    }

    /** Whether or not to disable animation while in Battery Saver. */
    public boolean getDisableAnimation() {
        return mDisableAnimation;
    }

    /** Whether or not to disable Always On Display while in Battery Saver. */
    public boolean getDisableAod() {
        return mDisableAod;
    }

    /** Whether or not to disable launch boost while in Battery Saver. */
    public boolean getDisableLaunchBoost() {
        return mDisableLaunchBoost;
    }

    /** Whether or not to disable optional sensors while in Battery Saver. */
    public boolean getDisableOptionalSensors() {
        return mDisableOptionalSensors;
    }

    /**
     * Get the SoundTrigger mode while in Battery Saver.
     */
    @PowerManager.SoundTriggerPowerSaveMode
    public int getSoundTriggerMode() {
        return mSoundTriggerMode;
    }

    /**
     * Whether or not to disable {@link android.hardware.soundtrigger.SoundTrigger}
     * while in Battery Saver.
     * @deprecated Use {@link #getSoundTriggerMode()} instead.
     */
    @Deprecated
    public boolean getDisableSoundTrigger() {
        return mSoundTriggerMode == PowerManager.SOUND_TRIGGER_MODE_ALL_DISABLED;
    }

    /** Whether or not to disable vibration while in Battery Saver. */
    public boolean getDisableVibration() {
        return mDisableVibration;
    }

    /** Whether or not to enable brightness adjustment while in Battery Saver. */
    public boolean getEnableAdjustBrightness() {
        return mEnableAdjustBrightness;
    }

    /** Whether or not to enable Data Saver while in Battery Saver. */
    public boolean getEnableDataSaver() {
        return mEnableDataSaver;
    }

    /**
     * Whether or not to enable network firewall rules to restrict background network use
     * while in Battery Saver.
     */
    public boolean getEnableFirewall() {
        return mEnableFirewall;
    }

    /** Whether or not to enable night mode while in Battery Saver. */
    public boolean getEnableNightMode() {
        return mEnableNightMode;
    }

    /** Whether or not to enable Quick Doze while in Battery Saver. */
    public boolean getEnableQuickDoze() {
        return mEnableQuickDoze;
    }

    /** Whether or not to force all apps to standby mode while in Battery Saver. */
    public boolean getForceAllAppsStandby() {
        return mForceAllAppsStandby;
    }

    /**
     * Whether or not to force background check (disallow background services and manifest
     * broadcast receivers) on all apps (not just apps targeting Android
     * {@link Build.VERSION_CODES#O} and above)
     * while in Battery Saver.
     */
    public boolean getForceBackgroundCheck() {
        return mForceBackgroundCheck;
    }

    /** The location mode while in Battery Saver. */
    public int getLocationMode() {
        return mLocationMode;
    }

    /** Builder class for constructing {@link BatterySaverPolicyConfig} objects. */
    public static final class Builder {
        private float mAdjustBrightnessFactor = 1f;
        private boolean mAdvertiseIsEnabled = false;
        private boolean mDeferFullBackup = false;
        private boolean mDeferKeyValueBackup = false;
        @NonNull
        private final ArrayMap<String, String> mDeviceSpecificSettings = new ArrayMap<>();
        private boolean mDisableAnimation = false;
        private boolean mDisableAod = false;
        private boolean mDisableLaunchBoost = false;
        private boolean mDisableOptionalSensors = false;
        private boolean mDisableVibration = false;
        private boolean mEnableAdjustBrightness = false;
        private boolean mEnableDataSaver = false;
        private boolean mEnableFirewall = false;
        private boolean mEnableNightMode = false;
        private boolean mEnableQuickDoze = false;
        private boolean mForceAllAppsStandby = false;
        private boolean mForceBackgroundCheck = false;
        private int mLocationMode = PowerManager.LOCATION_MODE_NO_CHANGE;
        private int mSoundTriggerMode = PowerManager.SOUND_TRIGGER_MODE_ALL_ENABLED;

        public Builder() {
        }

        /**
         * Creates a Builder prepopulated with the values from the passed in
         * {@link BatterySaverPolicyConfig}.
         */
        public Builder(@NonNull BatterySaverPolicyConfig batterySaverPolicyConfig) {
            mAdjustBrightnessFactor = batterySaverPolicyConfig.getAdjustBrightnessFactor();
            mAdvertiseIsEnabled = batterySaverPolicyConfig.getAdvertiseIsEnabled();
            mDeferFullBackup = batterySaverPolicyConfig.getDeferFullBackup();
            mDeferKeyValueBackup = batterySaverPolicyConfig.getDeferKeyValueBackup();

            for (String key :
                    batterySaverPolicyConfig.getDeviceSpecificSettings().keySet()) {
                mDeviceSpecificSettings.put(key,
                        batterySaverPolicyConfig.getDeviceSpecificSettings().get(key));
            }

            mDisableAnimation = batterySaverPolicyConfig.getDisableAnimation();
            mDisableAod = batterySaverPolicyConfig.getDisableAod();
            mDisableLaunchBoost = batterySaverPolicyConfig.getDisableLaunchBoost();
            mDisableOptionalSensors = batterySaverPolicyConfig.getDisableOptionalSensors();
            mDisableVibration = batterySaverPolicyConfig.getDisableVibration();
            mEnableAdjustBrightness = batterySaverPolicyConfig.getEnableAdjustBrightness();
            mEnableDataSaver = batterySaverPolicyConfig.getEnableDataSaver();
            mEnableFirewall = batterySaverPolicyConfig.getEnableFirewall();
            mEnableNightMode = batterySaverPolicyConfig.getEnableNightMode();
            mEnableQuickDoze = batterySaverPolicyConfig.getEnableQuickDoze();
            mForceAllAppsStandby = batterySaverPolicyConfig.getForceAllAppsStandby();
            mForceBackgroundCheck = batterySaverPolicyConfig.getForceBackgroundCheck();
            mLocationMode = batterySaverPolicyConfig.getLocationMode();
            mSoundTriggerMode = batterySaverPolicyConfig.getSoundTriggerMode();
        }

        /**
         * Set how much to adjust the screen brightness while in Battery Saver. The value should
         * be in the [0, 1] range, where 1 will not change the brightness. This will have no
         * effect if {@link #setEnableAdjustBrightness(boolean)} is not called with {@code true}.
         */
        @NonNull
        public Builder setAdjustBrightnessFactor(float adjustBrightnessFactor) {
            mAdjustBrightnessFactor = adjustBrightnessFactor;
            return this;
        }

        /**
         * Set whether or not to tell the system (and other apps) that Battery Saver is
         * currently enabled.
         */
        @NonNull
        public Builder setAdvertiseIsEnabled(boolean advertiseIsEnabled) {
            mAdvertiseIsEnabled = advertiseIsEnabled;
            return this;
        }

        /** Set whether or not to defer full backup while in Battery Saver. */
        @NonNull
        public Builder setDeferFullBackup(boolean deferFullBackup) {
            mDeferFullBackup = deferFullBackup;
            return this;
        }

        /** Set whether or not to defer key-value backup while in Battery Saver. */
        @NonNull
        public Builder setDeferKeyValueBackup(boolean deferKeyValueBackup) {
            mDeferKeyValueBackup = deferKeyValueBackup;
            return this;
        }

        /**
         * Adds a key-value pair for device-specific battery saver constants. The supported keys
         * and values are the same as those in
         * {@link android.provider.Settings.Global#BATTERY_SAVER_DEVICE_SPECIFIC_CONSTANTS}.
         *
         * @throws IllegalArgumentException if the provided key is invalid (empty, null, or all
         * whitespace)
         */
        @NonNull
        public Builder addDeviceSpecificSetting(@NonNull String key, @NonNull String value) {
            if (key == null) {
                throw new IllegalArgumentException("Key cannot be null");
            }
            key = key.trim();
            if (TextUtils.isEmpty(key)) {
                throw new IllegalArgumentException("Key cannot be empty");
            }
            mDeviceSpecificSettings.put(key, TextUtils.emptyIfNull(value));
            return this;
        }

        /** Set whether or not to disable animation while in Battery Saver. */
        @NonNull
        public Builder setDisableAnimation(boolean disableAnimation) {
            mDisableAnimation = disableAnimation;
            return this;
        }

        /** Set whether or not to disable Always On Display while in Battery Saver. */
        @NonNull
        public Builder setDisableAod(boolean disableAod) {
            mDisableAod = disableAod;
            return this;
        }

        /** Set whether or not to disable launch boost while in Battery Saver. */
        @NonNull
        public Builder setDisableLaunchBoost(boolean disableLaunchBoost) {
            mDisableLaunchBoost = disableLaunchBoost;
            return this;
        }

        /** Set whether or not to disable optional sensors while in Battery Saver. */
        @NonNull
        public Builder setDisableOptionalSensors(boolean disableOptionalSensors) {
            mDisableOptionalSensors = disableOptionalSensors;
            return this;
        }

        /**
         * Set whether or not to disable  {@link android.hardware.soundtrigger.SoundTrigger}
         * while in Battery Saver.
         * @deprecated Use {@link #setSoundTriggerMode(int)} instead.
         */
        @Deprecated
        @NonNull
        public Builder setDisableSoundTrigger(boolean disableSoundTrigger) {
            if (disableSoundTrigger) {
                mSoundTriggerMode = PowerManager.SOUND_TRIGGER_MODE_ALL_DISABLED;
            } else {
                mSoundTriggerMode = PowerManager.SOUND_TRIGGER_MODE_ALL_ENABLED;
            }
            return this;
        }

        /**
         * Set the SoundTrigger mode while in Battery Saver.
         */
        @NonNull
        public Builder setSoundTriggerMode(
                @PowerManager.SoundTriggerPowerSaveMode int soundTriggerMode) {
            mSoundTriggerMode = soundTriggerMode;
            return this;
        }

        /** Set whether or not to disable vibration while in Battery Saver. */
        @NonNull
        public Builder setDisableVibration(boolean disableVibration) {
            mDisableVibration = disableVibration;
            return this;
        }

        /** Set whether or not to enable brightness adjustment while in Battery Saver. */
        @NonNull
        public Builder setEnableAdjustBrightness(boolean enableAdjustBrightness) {
            mEnableAdjustBrightness = enableAdjustBrightness;
            return this;
        }

        /** Set whether or not to enable Data Saver while in Battery Saver. */
        @NonNull
        public Builder setEnableDataSaver(boolean enableDataSaver) {
            mEnableDataSaver = enableDataSaver;
            return this;
        }

        /**
         * Set whether or not to enable network firewall rules to restrict background network use
         * while in Battery Saver.
         */
        @NonNull
        public Builder setEnableFirewall(boolean enableFirewall) {
            mEnableFirewall = enableFirewall;
            return this;
        }

        /** Set whether or not to enable night mode while in Battery Saver. */
        @NonNull
        public Builder setEnableNightMode(boolean enableNightMode) {
            mEnableNightMode = enableNightMode;
            return this;
        }

        /** Set whether or not to enable Quick Doze while in Battery Saver. */
        @NonNull
        public Builder setEnableQuickDoze(boolean enableQuickDoze) {
            mEnableQuickDoze = enableQuickDoze;
            return this;
        }

        /** Set whether or not to force all apps to standby mode while in Battery Saver. */
        @NonNull
        public Builder setForceAllAppsStandby(boolean forceAllAppsStandby) {
            mForceAllAppsStandby = forceAllAppsStandby;
            return this;
        }

        /**
         * Set whether or not to force background check (disallow background services and manifest
         * broadcast receivers) on all apps (not just apps targeting Android
         * {@link Build.VERSION_CODES#O} and above)
         * while in Battery Saver.
         */
        @NonNull
        public Builder setForceBackgroundCheck(boolean forceBackgroundCheck) {
            mForceBackgroundCheck = forceBackgroundCheck;
            return this;
        }

        /** Set the location mode while in Battery Saver. */
        @NonNull
        public Builder setLocationMode(@PowerManager.LocationPowerSaveMode int locationMode) {
            mLocationMode = locationMode;
            return this;
        }

        /**
         * Build a {@link BatterySaverPolicyConfig} object using the set parameters. This object
         * is immutable.
         */
        @NonNull
        public BatterySaverPolicyConfig build() {
            return new BatterySaverPolicyConfig(this);
        }
    }
}
