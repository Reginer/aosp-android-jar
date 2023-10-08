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

package android.net.wifi;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.wifi.ScanResult.WifiBand;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * An Object used in {@link WifiManager#setNetworkSelectionConfig(WifiNetworkSelectionConfig)}.
 * @hide
 */
@SystemApi
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public final class WifiNetworkSelectionConfig implements Parcelable {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"ASSOCIATED_NETWORK_SELECTION_OVERRIDE_"}, value = {
            ASSOCIATED_NETWORK_SELECTION_OVERRIDE_NONE,
            ASSOCIATED_NETWORK_SELECTION_OVERRIDE_ENABLED,
            ASSOCIATED_NETWORK_SELECTION_OVERRIDE_DISABLED})
    public @interface AssociatedNetworkSelectionOverride {}
    /**
     * A constant used in {@link Builder#setAssociatedNetworkSelectionOverride(int)}
     * This is the default value which performs no override.
     */
    public static final int ASSOCIATED_NETWORK_SELECTION_OVERRIDE_NONE = 0;
    /**
     * A constant used in {{@link Builder#setAssociatedNetworkSelectionOverride(int)}
     * Overrides the config_wifi_framework_enable_associated_network_selection overlay to true to
     * allow the wifi framework to automatically select and switch to a better wifi network while
     * already connected.
     */
    public static final int ASSOCIATED_NETWORK_SELECTION_OVERRIDE_ENABLED = 1;
    /**
     * A constant used in {@link Builder#setAssociatedNetworkSelectionOverride(int)}
     * Overrides the config_wifi_framework_enable_associated_network_selection overlay to false to
     * disallow the wifi framework to automatically select and connect to another network while
     * already connected.
     */
    public static final int ASSOCIATED_NETWORK_SELECTION_OVERRIDE_DISABLED = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"FREQUENCY_WEIGHT_"}, value = {
            FREQUENCY_WEIGHT_LOW,
            FREQUENCY_WEIGHT_HIGH})
    public @interface FrequencyWeight {}

    /**
     * A constant used in {@link Builder#setFrequencyWeights(SparseArray)} to indicate a low
     * preference for the frequency it's associated with.
     */
    public static final int FREQUENCY_WEIGHT_LOW = 0;
    /**
     * A constant used in {@link Builder#setFrequencyWeights(SparseArray)} to indicate a high
     * preference for the frequency it's associated with.
     */
    public static final int FREQUENCY_WEIGHT_HIGH = 1;

    private boolean mSufficiencyCheckEnabledWhenScreenOff = true;
    private boolean mSufficiencyCheckEnabledWhenScreenOn = true;
    private boolean mUserConnectChoiceOverrideEnabled = true;
    private boolean mLastSelectionWeightEnabled = true;
    private int mAssociatedNetworkSelectionOverride = ASSOCIATED_NETWORK_SELECTION_OVERRIDE_NONE;

    /** RSSI thresholds for 2.4 GHz band (dBm) */
    private int[] mRssi2Thresholds = new int[4];

    /** RSSI thresholds for 5 GHz band (dBm) */
    private int[] mRssi5Thresholds = new int[4];

    /** RSSI thresholds for 6 GHz band (dBm) */
    private int[] mRssi6Thresholds = new int[4];

    /** Frequency weight list */
    private SparseArray<Integer> mFrequencyWeights = new SparseArray<>();

    // empty constructor
    private WifiNetworkSelectionConfig() {

    }

    // copy constructor used by Builder
    private WifiNetworkSelectionConfig(WifiNetworkSelectionConfig that) {
        mSufficiencyCheckEnabledWhenScreenOff = that.mSufficiencyCheckEnabledWhenScreenOff;
        mSufficiencyCheckEnabledWhenScreenOn = that.mSufficiencyCheckEnabledWhenScreenOn;
        mAssociatedNetworkSelectionOverride = that.mAssociatedNetworkSelectionOverride;
        mUserConnectChoiceOverrideEnabled = that.mUserConnectChoiceOverrideEnabled;
        mLastSelectionWeightEnabled = that.mLastSelectionWeightEnabled;
        mRssi2Thresholds = that.mRssi2Thresholds;
        mRssi5Thresholds = that.mRssi5Thresholds;
        mRssi6Thresholds = that.mRssi6Thresholds;
        mFrequencyWeights = that.mFrequencyWeights;
    }

    /**
     * See {@link Builder#setSufficiencyCheckEnabledWhenScreenOff(boolean)}.
     */
    public boolean isSufficiencyCheckEnabledWhenScreenOff() {
        return mSufficiencyCheckEnabledWhenScreenOff;
    }

    /**
     * See {@link Builder#setSufficiencyCheckEnabledWhenScreenOn(boolean)}.
     */
    public boolean isSufficiencyCheckEnabledWhenScreenOn() {
        return mSufficiencyCheckEnabledWhenScreenOn;
    }

    /**
     * See {@link Builder#setUserConnectChoiceOverrideEnabled(boolean)}.
     */
    public boolean isUserConnectChoiceOverrideEnabled() {
        return mUserConnectChoiceOverrideEnabled;
    }

    /**
     * See {@link Builder#setLastSelectionWeightEnabled(boolean)}.
     */
    public boolean isLastSelectionWeightEnabled() {
        return mLastSelectionWeightEnabled;
    }

    /**
     * See {@link Builder#setAssociatedNetworkSelectionOverride(int)}.
     */
    public @AssociatedNetworkSelectionOverride int getAssociatedNetworkSelectionOverride() {
        return mAssociatedNetworkSelectionOverride;
    }

    private static boolean isValidAssociatedNetworkSelectionOverride(int override) {
        return override >= ASSOCIATED_NETWORK_SELECTION_OVERRIDE_NONE
                && override <= ASSOCIATED_NETWORK_SELECTION_OVERRIDE_DISABLED;
    }

    private static boolean isValidBand(@WifiBand int band) {
        switch (band) {
            case ScanResult.WIFI_BAND_24_GHZ:
            case ScanResult.WIFI_BAND_5_GHZ:
            case ScanResult.WIFI_BAND_6_GHZ:
                return true;
            default:
                return false;
        }
    }

    private static boolean isValidRssiThresholdArray(int[] thresholds) {
        if (thresholds == null || thresholds.length != 4) return false;

        if (!isRssiThresholdResetArray(thresholds)) {
            int low = WifiInfo.MIN_RSSI - 1;
            int high = Math.min(WifiInfo.MAX_RSSI, -1);
            for (int i = 0; i < thresholds.length; i++) {
                if (thresholds[i] <= low || thresholds[i] > high) {
                    return false;
                }
                low = thresholds[i];
            }
        }
        return true;
    }

    private static boolean isValidFrequencyWeightArray(SparseArray<Integer> weights) {
        if (weights == null) return false;

        for (int i = 0; i < weights.size(); i++) {
            int value = weights.valueAt(i);
            if (value < FREQUENCY_WEIGHT_LOW || value > FREQUENCY_WEIGHT_HIGH) return false;
        }
        return true;
    }

    /**
     * Check whether the given RSSI threshold array contains all 0s.
     * @hide
     */
    public static boolean isRssiThresholdResetArray(@NonNull int[] thresholds) {
        for (int value : thresholds) {
            if (value != 0) return false;
        }
        return true;
    }

    /**
     * Check whether the current configuration is valid.
     * @hide
     */
    public boolean isValid() {
        return isValidAssociatedNetworkSelectionOverride(mAssociatedNetworkSelectionOverride)
                && isValidRssiThresholdArray(mRssi2Thresholds)
                && isValidRssiThresholdArray(mRssi5Thresholds)
                && isValidRssiThresholdArray(mRssi6Thresholds)
                && isValidFrequencyWeightArray(mFrequencyWeights);
    }

    /**
     * See {@link Builder#setRssiThresholds(int, int[])}.
     * Returns RSSI thresholds for the input band.
     *
     * @throws IllegalArgumentException if the input band is not a supported {@link WifiBand}
     */
    public @NonNull int[] getRssiThresholds(@WifiBand int band) {
        if (!isValidBand(band)) {
            throw new IllegalArgumentException("Invalid band=" + band);
        }
        switch (band) {
            case ScanResult.WIFI_BAND_24_GHZ:
                return mRssi2Thresholds;
            case ScanResult.WIFI_BAND_5_GHZ:
                return mRssi5Thresholds;
            case ScanResult.WIFI_BAND_6_GHZ:
                return mRssi6Thresholds;
        }
        throw new IllegalArgumentException("Did not find RSSI thresholds for band=" + band);
    }

    /**
     * See {@link Builder#setFrequencyWeights(SparseArray)}.
     */
    public @NonNull SparseArray<Integer> getFrequencyWeights() {
        return mFrequencyWeights;
    }

    /**
     * Used to create a {@link WifiNetworkSelectionConfig} Object.
     */
    public static final class Builder {
        WifiNetworkSelectionConfig mWifiNetworkSelectionConfig = new WifiNetworkSelectionConfig();

        public Builder() {
            mWifiNetworkSelectionConfig.mSufficiencyCheckEnabledWhenScreenOff = true;
            mWifiNetworkSelectionConfig.mSufficiencyCheckEnabledWhenScreenOn = true;
            mWifiNetworkSelectionConfig.mUserConnectChoiceOverrideEnabled = true;
            mWifiNetworkSelectionConfig.mLastSelectionWeightEnabled = true;
            mWifiNetworkSelectionConfig.mAssociatedNetworkSelectionOverride =
                    ASSOCIATED_NETWORK_SELECTION_OVERRIDE_NONE;
            mWifiNetworkSelectionConfig.mRssi2Thresholds = new int[4];
            mWifiNetworkSelectionConfig.mRssi5Thresholds = new int[4];
            mWifiNetworkSelectionConfig.mRssi6Thresholds = new int[4];
            mWifiNetworkSelectionConfig.mFrequencyWeights = new SparseArray<>();
        }

        public Builder(@NonNull WifiNetworkSelectionConfig config) {
            mWifiNetworkSelectionConfig = config;
        }

        /**
         * This setting affects wifi network selection behavior while already connected to a
         * network, and is only relevant if associated network selection
         * (see {@link #setAssociatedNetworkSelectionOverride(int)}) is enabled. Enable or disable
         * network sufficiency check when wifi is connected and the screen is off.
         * <p>
         * If the sufficiency check is enabled, multiple parameters such as the RSSI and estimated
         * throughput will be used to determine if the current network is sufficient. When the
         * current network is found sufficient, the wifi framework will not attempt a network switch
         * even if a potentially better network is available. When the current network is found
         * insufficient, the wifi framework will keep trying to score other networks against the
         * current network attempting to find and connect to a better alternative.
         * <p>
         * If the sufficiency check is disabled, then the currently connected network will always
         * be considered insufficient. See the previous paragraph on the wifi framework's behavior
         * when the current network is insufficient.
         * <p>
         * By default, network sufficiency check is enabled for both screen on and screen off cases.
         * @param enabled Set to true to enable sufficiency check, and false to disable sufficiency
         *                check.
         */
        public @NonNull Builder setSufficiencyCheckEnabledWhenScreenOff(boolean enabled) {
            mWifiNetworkSelectionConfig.mSufficiencyCheckEnabledWhenScreenOff = enabled;
            return this;
        }

        /**
         * This setting affects wifi network selection behavior while already connected to a
         * network, and is only relevant if associated network selection
         * (see {@link #setAssociatedNetworkSelectionOverride(int)}) is enabled. Enable or disable
         * network sufficiency check when wifi is connected and the screen is on.
         * <p>
         * If the sufficiency check is enabled, multiple parameters such as the RSSI and estimated
         * throughput will be used to determine if the current network is sufficient. When the
         * current network is found sufficient, the wifi framework will not attempt a network switch
         * even if a potentially better network is available. When the current network is found
         * insufficient, the wifi framework will keep trying to score other networks against the
         * current network attempting to find and connect to a better alternative.
         * <p>
         * If the sufficiency check is disabled, then the currently connected network will always
         * be considered insufficient. See the previous paragraph on the wifi framework's behavior
         * when the current network is insufficient.
         * <p>
         * By default, network sufficiency check is enabled for both screen on and screen off cases.
         * @param enabled Set to true to enable sufficiency check, and false to disable sufficiency
         *                check.
         */
        public @NonNull Builder setSufficiencyCheckEnabledWhenScreenOn(boolean enabled) {
            mWifiNetworkSelectionConfig.mSufficiencyCheckEnabledWhenScreenOn = enabled;
            return this;
        }

        /**
         * Override the value programmed by the
         * {@code config_wifi_framework_enable_associated_network_selection} overlay with one of the
         * {@code ASSOCIATED_NETWORK_SELECTION_OVERRIDE_} values. When the overlay is enabled,
         * the wifi framework is allowed to automatically select and switch to a better wifi
         * network while already connected. When the overlay is disabled, the wifi framework will
         * simply stay connected to the connected network and will not attempt to automatically
         * switch to another network.
         * <p>
         * By default, there is no override, and the framework will use the value set in the
         * overlay.
         * @param override the value to override the overlay as.
         * @throws IllegalArgumentException if the input is invalid.
         */
        public @NonNull Builder setAssociatedNetworkSelectionOverride(
                @AssociatedNetworkSelectionOverride int override) throws IllegalArgumentException {
            if (!isValidAssociatedNetworkSelectionOverride(override)) {
                throw new IllegalArgumentException("Invalid override=" + override);
            }
            mWifiNetworkSelectionConfig.mAssociatedNetworkSelectionOverride = override;
            return this;
        }

        /**
         * Enable or disable candidate override with the user connect choice.
         * <p>
         * If the override is enabled, the network selector overrides any selected candidate
         * with a network previously chosen by the user over the candidate (i.e. when the
         * candidate was connected the user explicitly selected another network), if one exists.
         * <p>
         * If the override is disabled, network selector uses the network nominator candidate
         * and does not override it with the user chosen configuration.
         * <p>
         * By default, user connect choice override is enabled.
         * @param enabled Set to true to enable candidate override with the user connect choice,
         *                and false to disable the override.
         */
        public @NonNull Builder setUserConnectChoiceOverrideEnabled(boolean enabled) {
            mWifiNetworkSelectionConfig.mUserConnectChoiceOverrideEnabled = enabled;
            return this;
        }

        /**
         * Enable or disable last selection weight.
         * <p>
         * If the last selection weight is enabled, network selector prefers the latest
         * user selected network over all other networks for a limited duration.
         * This duration is configurable via {@code config_wifiFrameworkLastSelectionMinutes}.
         * <p>
         * If the last selection weight is disabled, network selector does not prefer a
         * recently selected network over other networks.
         * <p>
         * By default, last selection weight is enabled.
         * @param enabled Set to true to enable the last selection weight,
         *                and false to disable it.
         */
        public @NonNull Builder setLastSelectionWeightEnabled(boolean enabled) {
            mWifiNetworkSelectionConfig.mLastSelectionWeightEnabled = enabled;
            return this;
        }

        /**
         * Sets the RSSI thresholds for the input band.
         * <p>
         * If the RSSI thresholds are set, network selector uses these values over the
         * following overlay configured values for the specified input band.
         * For {@code ScanResult.WIFI_BAND_24_GHZ}:
         * <ul>
         *     <li>{@code config_wifi_framework_wifi_score_bad_rssi_threshold_24GHz}</li>
         *     <li>{@code config_wifi_framework_wifi_score_entry_rssi_threshold_24GHz}</li>
         *     <li>{@code config_wifi_framework_wifi_score_low_rssi_threshold_24GHz}</li>
         *     <li>{@code config_wifi_framework_wifi_score_good_rssi_threshold_24GHz}</li>
         * </ul>
         * For {@code ScanResult.WIFI_BAND_5_GHZ}:
         * <ul>
         *     <li>{@code config_wifi_framework_wifi_score_bad_rssi_threshold_5GHz}</li>
         *     <li>{@code config_wifi_framework_wifi_score_entry_rssi_threshold_5GHz}</li>
         *     <li>{@code config_wifi_framework_wifi_score_low_rssi_threshold_5GHz}</li>
         *     <li>{@code config_wifi_framework_wifi_score_good_rssi_threshold_5GHz}</li>
         * </ul>
         * For {@code ScanResult.WIFI_BAND_6_GHZ}:
         * <ul>
         *     <li>{@code config_wifiFrameworkScoreBadRssiThreshold6ghz}</li>
         *     <li>{@code config_wifiFrameworkScoreEntryRssiThreshold6ghz}</li>
         *     <li>{@code config_wifiFrameworkScoreLowRssiThreshold6ghz}</li>
         *     <li>{@code config_wifiFrameworkScoreGoodRssiThreshold6ghz}</li>
         * </ul>
         * <p>
         * The input thresholds override the overlays listed above in the respective order
         * so it must be an int array with 4 values.
         * The values must be between -126 and -1 and the array must be strictly increasing.
         * For example, [-80, -70, -60, -50] is a valid input while [-70, -70, -60, -50] is not
         * since the array is not strictly increasing.
         * The only exception to these rules is [0, 0, 0, 0], which is used to remove any
         * RSSI thresholds set.
         * <p>
         * The input band must be one of the following {@link WifiBand}:
         * <ul>
         *     <li>{@code ScanResult.WIFI_BAND_24_GHZ}</li>
         *     <li>{@code ScanResult.WIFI_BAND_5_GHZ}</li>
         *     <li>{@code ScanResult.WIFI_BAND_6_GHZ}</li>
         * </ul>
         * <p>
         * To remove the RSSI thresholds set, pass in an array with 0s as the thresholds.
         * The network selector will go back to using the overlay configured values.
         * @param band {@link WifiBand} you want to set the RSSI thresholds for
         * @param thresholds RSSI thresholds
         * @throws IllegalArgumentException if the input is invalid.
         */
        public @NonNull Builder setRssiThresholds(@WifiBand int band, @NonNull int[] thresholds)
                throws IllegalArgumentException {
            if (!isValidRssiThresholdArray(thresholds)) {
                throw new IllegalArgumentException("Invalid RSSI thresholds="
                        + Arrays.toString(thresholds));
            }
            if (!isValidBand(band)) {
                throw new IllegalArgumentException("Invalid band=" + band);
            }
            switch (band) {
                case ScanResult.WIFI_BAND_24_GHZ:
                    mWifiNetworkSelectionConfig.mRssi2Thresholds = thresholds;
                    break;
                case ScanResult.WIFI_BAND_5_GHZ:
                    mWifiNetworkSelectionConfig.mRssi5Thresholds = thresholds;
                    break;
                case ScanResult.WIFI_BAND_6_GHZ:
                    mWifiNetworkSelectionConfig.mRssi6Thresholds = thresholds;
                    break;
            }
            return this;
        }

        /**
         * Sets the frequency weights that will be used by the network selector to provide
         * a bonus or penalty to the specified frequencies in the list.
         * <p>
         * The input SparseArray has to adhere to the following (key, value) format.
         * Key: frequency the weight needs to be applied to in MHz (ex. 5201MHz -> 5201)
         * Value: one of {@link FrequencyWeight}
         * <ul>
         *      <li>{@link #FREQUENCY_WEIGHT_LOW}</li>
         *      <li>{@link #FREQUENCY_WEIGHT_HIGH}</li>
         * </ul>
         * <p>
         * By default, all frequencies not present in the list will not have any frequency weight.
         * <p>
         * To removed the frequency weights set, pass in an empty SparseArray.
         * The network selector will go back to treating all the frequencies with
         * an equal preference.
         * @param weights frequency weights
         * @throws IllegalArgumentException if the input is invalid.
         */
        public @NonNull Builder setFrequencyWeights(@NonNull SparseArray<Integer> weights)
                throws IllegalArgumentException {
            if (!isValidFrequencyWeightArray(weights)) {
                if (weights == null) {
                    throw new IllegalArgumentException("Invalid frequency weights=null");
                }
                throw new IllegalArgumentException("Invalid frequency weights="
                        + weights.toString());
            }
            mWifiNetworkSelectionConfig.mFrequencyWeights = weights;
            return this;
        }

        /**
         * Creates a WifiNetworkSelectionConfig for use in
         * {@link WifiManager#setNetworkSelectionConfig(WifiNetworkSelectionConfig, Consumer)}
         */
        public @NonNull WifiNetworkSelectionConfig build() {
            return new WifiNetworkSelectionConfig(mWifiNetworkSelectionConfig);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSufficiencyCheckEnabledWhenScreenOff,
                mSufficiencyCheckEnabledWhenScreenOn, mAssociatedNetworkSelectionOverride,
                mUserConnectChoiceOverrideEnabled, mLastSelectionWeightEnabled,
                Arrays.hashCode(mRssi2Thresholds), Arrays.hashCode(mRssi5Thresholds),
                Arrays.hashCode(mRssi6Thresholds), mFrequencyWeights.contentHashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiNetworkSelectionConfig)) {
            return false;
        }
        WifiNetworkSelectionConfig lhs = (WifiNetworkSelectionConfig) obj;
        return mSufficiencyCheckEnabledWhenScreenOff == lhs.mSufficiencyCheckEnabledWhenScreenOff
                && mSufficiencyCheckEnabledWhenScreenOn == lhs.mSufficiencyCheckEnabledWhenScreenOn
                && mAssociatedNetworkSelectionOverride == lhs.mAssociatedNetworkSelectionOverride
                && mUserConnectChoiceOverrideEnabled == lhs.mUserConnectChoiceOverrideEnabled
                && mLastSelectionWeightEnabled == lhs.mLastSelectionWeightEnabled
                && Arrays.equals(mRssi2Thresholds, lhs.mRssi2Thresholds)
                && Arrays.equals(mRssi5Thresholds, lhs.mRssi5Thresholds)
                && Arrays.equals(mRssi6Thresholds, lhs.mRssi6Thresholds)
                && mFrequencyWeights.contentEquals(lhs.mFrequencyWeights);
    }

    public static final @NonNull Creator<WifiNetworkSelectionConfig> CREATOR =
            new Creator<WifiNetworkSelectionConfig>() {
                @Override
                public WifiNetworkSelectionConfig createFromParcel(Parcel in) {
                    WifiNetworkSelectionConfig config = new WifiNetworkSelectionConfig();
                    config.mSufficiencyCheckEnabledWhenScreenOff = in.readBoolean();
                    config.mSufficiencyCheckEnabledWhenScreenOn = in.readBoolean();
                    config.mAssociatedNetworkSelectionOverride = in.readInt();
                    config.mUserConnectChoiceOverrideEnabled = in.readBoolean();
                    config.mLastSelectionWeightEnabled = in.readBoolean();
                    in.readIntArray(config.mRssi2Thresholds);
                    in.readIntArray(config.mRssi5Thresholds);
                    in.readIntArray(config.mRssi6Thresholds);
                    config.mFrequencyWeights = in.readSparseArray(null, java.lang.Integer.class);
                    return config;
                }

                @Override
                public WifiNetworkSelectionConfig[] newArray(int size) {
                    return new WifiNetworkSelectionConfig[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBoolean(mSufficiencyCheckEnabledWhenScreenOff);
        dest.writeBoolean(mSufficiencyCheckEnabledWhenScreenOn);
        dest.writeInt(mAssociatedNetworkSelectionOverride);
        dest.writeBoolean(mUserConnectChoiceOverrideEnabled);
        dest.writeBoolean(mLastSelectionWeightEnabled);
        dest.writeIntArray(mRssi2Thresholds);
        dest.writeIntArray(mRssi5Thresholds);
        dest.writeIntArray(mRssi6Thresholds);
        dest.writeSparseArray(mFrequencyWeights);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mSufficiencyCheckEnabledWhenScreenOff=")
                .append(mSufficiencyCheckEnabledWhenScreenOff)
                .append(", mSufficiencyCheckEnabledWhenScreenOn=")
                .append(mSufficiencyCheckEnabledWhenScreenOn)
                .append(", mAssociatedNetworkSelectionOverride=")
                .append(mAssociatedNetworkSelectionOverride)
                .append(", mUserConnectChoiceOverrideEnabled=")
                .append(mUserConnectChoiceOverrideEnabled)
                .append(", mLastSelectionWeightEnabled=")
                .append(mLastSelectionWeightEnabled)
                .append(", mRssi2Thresholds=")
                .append(Arrays.toString(mRssi2Thresholds))
                .append(", mRssi5Thresholds=")
                .append(Arrays.toString(mRssi5Thresholds))
                .append(", mRssi6Thresholds=")
                .append(Arrays.toString(mRssi6Thresholds))
                .append(", mFrequencyWeights=")
                .append(mFrequencyWeights.toString());
        return sb.toString();
    }
}
