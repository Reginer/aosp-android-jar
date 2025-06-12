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

package android.adservices.ondevicepersonalization;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.ORIENTATION_UNDEFINED;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkCapabilities;
import android.os.Parcelable;
import android.telephony.TelephonyManager;

import com.android.ondevicepersonalization.internal.util.AnnotationValidations;
import com.android.ondevicepersonalization.internal.util.DataClass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * User data provided by the platform to an {@link IsolatedService}.
 *
 */
// This class should be updated with the Kotlin mirror
// {@link com.android.ondevicepersonalization.services.policyengine.data.UserData}.
@DataClass(genHiddenBuilder = true, genEqualsHashCode = true, genConstDefs = false)
public final class UserData implements Parcelable {
    /**
     * The device timezone +/- offset from UTC.
     *
     * @hide
     */
    int mTimezoneUtcOffsetMins = 0;

    /** @hide **/
    @IntDef(prefix = {"ORIENTATION_"}, value = {
            ORIENTATION_UNDEFINED,
            ORIENTATION_PORTRAIT,
            ORIENTATION_LANDSCAPE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Orientation {
    }

    /**
     * The device orientation. The value can be one of the constants ORIENTATION_UNDEFINED,
     * ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE defined in
     * {@link android.content.res.Configuration}.
     */
    @Orientation int mOrientation = 0;

    /** The available space on device in bytes. */
    @IntRange(from = 0) long mAvailableStorageBytes = 0;

    /** Battery percentage. */
    @IntRange(from = 0, to = 100) int mBatteryPercentage = 0;

    /** The Service Provider Name (SPN) returned by {@link TelephonyManager#getSimOperatorName()} */
    @NonNull String mCarrier = "";

    /** @hide **/
    @IntDef({
            TelephonyManager.NETWORK_TYPE_UNKNOWN,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA,
            TelephonyManager.NETWORK_TYPE_IWLAN,

            //TODO: In order for @SystemApi methods to use this class, there cannot be any
            // public hidden members.  This network type is marked as hidden because it is not a
            // true network type and we are looking to remove it completely from the available list
            // of network types.
            //TelephonyManager.NETWORK_TYPE_LTE_CA,

            TelephonyManager.NETWORK_TYPE_NR,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkType {
    }

    /**
     * A filtered subset of the Network capabilities of the device that contains upstream
     * and downstream speeds, and whether the network is metered.
     * This is an instance of {@link NetworkCapabilities} that contains the capability
     * {@link android.net.NetworkCapabilities#NET_CAPABILITY_NOT_METERED} if the network is not
     * metered, and {@link NetworkCapabilities#getLinkDownstreamBandwidthKbps()} and
     * {@link NetworkCapabilities#getLinkUpstreamBandwidthKbps()} return the downstream and
     * upstream connection speeds. All other methods of this {@link NetworkCapabilities} object
     * return empty or default values.
     */
    @Nullable NetworkCapabilities mNetworkCapabilities = null;

    /**
     * Data network type. This is the value of
     * {@link android.telephony.TelephonyManager#getDataNetworkType()}.
     */
    @NetworkType int mDataNetworkType = 0;

    /** A map from package name to app information for installed and uninstalled apps. */
    @DataClass.PluralOf("appInfo")
    @NonNull Map<String, AppInfo> mAppInfos = Collections.emptyMap();

    /** The device timezone +/- offset from UTC. */
    @NonNull public Duration getTimezoneUtcOffset() {
        return Duration.ofMinutes(mTimezoneUtcOffsetMins);
    }



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/UserData.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ UserData(
            int timezoneUtcOffsetMins,
            @Orientation int orientation,
            @IntRange(from = 0) long availableStorageBytes,
            @IntRange(from = 0, to = 100) int batteryPercentage,
            @NonNull String carrier,
            @Nullable NetworkCapabilities networkCapabilities,
            @NetworkType int dataNetworkType,
            @NonNull Map<String,AppInfo> appInfos) {
        this.mTimezoneUtcOffsetMins = timezoneUtcOffsetMins;
        this.mOrientation = orientation;
        AnnotationValidations.validate(
                Orientation.class, null, mOrientation);
        this.mAvailableStorageBytes = availableStorageBytes;
        AnnotationValidations.validate(
                IntRange.class, null, mAvailableStorageBytes,
                "from", 0);
        this.mBatteryPercentage = batteryPercentage;
        AnnotationValidations.validate(
                IntRange.class, null, mBatteryPercentage,
                "from", 0,
                "to", 100);
        this.mCarrier = carrier;
        AnnotationValidations.validate(
                NonNull.class, null, mCarrier);
        this.mNetworkCapabilities = networkCapabilities;
        this.mDataNetworkType = dataNetworkType;
        AnnotationValidations.validate(
                NetworkType.class, null, mDataNetworkType);
        this.mAppInfos = appInfos;
        AnnotationValidations.validate(
                NonNull.class, null, mAppInfos);

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * The device timezone +/- offset from UTC.
     *
     * @hide
     */
    @DataClass.Generated.Member
    public int getTimezoneUtcOffsetMins() {
        return mTimezoneUtcOffsetMins;
    }

    /**
     * The device orientation. The value can be one of the constants ORIENTATION_UNDEFINED,
     * ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE defined in
     * {@link android.content.res.Configuration}.
     */
    @DataClass.Generated.Member
    public @Orientation int getOrientation() {
        return mOrientation;
    }

    /**
     * The available space on device in bytes.
     */
    @DataClass.Generated.Member
    public @IntRange(from = 0) long getAvailableStorageBytes() {
        return mAvailableStorageBytes;
    }

    /**
     * Battery percentage.
     */
    @DataClass.Generated.Member
    public @IntRange(from = 0, to = 100) int getBatteryPercentage() {
        return mBatteryPercentage;
    }

    /**
     * The Service Provider Name (SPN) returned by {@link TelephonyManager#getSimOperatorName()}
     */
    @DataClass.Generated.Member
    public @NonNull String getCarrier() {
        return mCarrier;
    }

    /**
     * A filtered subset of the Network capabilities of the device that contains upstream
     * and downstream speeds, and whether the network is metered.
     * This is an instance of {@link NetworkCapabilities} that contains the capability
     * {@link android.net.NetworkCapabilities#NET_CAPABILITY_NOT_METERED} if the network is not
     * metered, and {@link NetworkCapabilities#getLinkDownstreamBandwidthKbps()} and
     * {@link NetworkCapabilities#getLinkUpstreamBandwidthKbps()} return the downstream and
     * upstream connection speeds. All other methods of this {@link NetworkCapabilities} object
     * return empty or default values.
     */
    @DataClass.Generated.Member
    public @Nullable NetworkCapabilities getNetworkCapabilities() {
        return mNetworkCapabilities;
    }

    /**
     * Data network type. This is the value of
     * {@link android.telephony.TelephonyManager#getDataNetworkType()}.
     */
    @DataClass.Generated.Member
    public @NetworkType int getDataNetworkType() {
        return mDataNetworkType;
    }

    /**
     * A map from package name to app information for installed and uninstalled apps.
     */
    @DataClass.Generated.Member
    public @NonNull Map<String,AppInfo> getAppInfos() {
        return mAppInfos;
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(UserData other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        UserData that = (UserData) o;
        //noinspection PointlessBooleanExpression
        return true
                && mTimezoneUtcOffsetMins == that.mTimezoneUtcOffsetMins
                && mOrientation == that.mOrientation
                && mAvailableStorageBytes == that.mAvailableStorageBytes
                && mBatteryPercentage == that.mBatteryPercentage
                && java.util.Objects.equals(mCarrier, that.mCarrier)
                && java.util.Objects.equals(mNetworkCapabilities, that.mNetworkCapabilities)
                && mDataNetworkType == that.mDataNetworkType
                && java.util.Objects.equals(mAppInfos, that.mAppInfos);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + mTimezoneUtcOffsetMins;
        _hash = 31 * _hash + mOrientation;
        _hash = 31 * _hash + Long.hashCode(mAvailableStorageBytes);
        _hash = 31 * _hash + mBatteryPercentage;
        _hash = 31 * _hash + java.util.Objects.hashCode(mCarrier);
        _hash = 31 * _hash + java.util.Objects.hashCode(mNetworkCapabilities);
        _hash = 31 * _hash + mDataNetworkType;
        _hash = 31 * _hash + java.util.Objects.hashCode(mAppInfos);
        return _hash;
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull android.os.Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        int flg = 0;
        if (mNetworkCapabilities != null) flg |= 0x20;
        dest.writeInt(flg);
        dest.writeInt(mTimezoneUtcOffsetMins);
        dest.writeInt(mOrientation);
        dest.writeLong(mAvailableStorageBytes);
        dest.writeInt(mBatteryPercentage);
        dest.writeString(mCarrier);
        if (mNetworkCapabilities != null) dest.writeTypedObject(mNetworkCapabilities, flags);
        dest.writeInt(mDataNetworkType);
        dest.writeMap(mAppInfos);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ UserData(@NonNull android.os.Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        int flg = in.readInt();
        int timezoneUtcOffsetMins = in.readInt();
        int orientation = in.readInt();
        long availableStorageBytes = in.readLong();
        int batteryPercentage = in.readInt();
        String carrier = in.readString();
        NetworkCapabilities networkCapabilities = (flg & 0x20) == 0 ? null : (NetworkCapabilities) in.readTypedObject(NetworkCapabilities.CREATOR);
        int dataNetworkType = in.readInt();
        Map<String,AppInfo> appInfos = new java.util.LinkedHashMap<>();
        in.readMap(appInfos, AppInfo.class.getClassLoader());

        this.mTimezoneUtcOffsetMins = timezoneUtcOffsetMins;
        this.mOrientation = orientation;
        AnnotationValidations.validate(
                Orientation.class, null, mOrientation);
        this.mAvailableStorageBytes = availableStorageBytes;
        AnnotationValidations.validate(
                IntRange.class, null, mAvailableStorageBytes,
                "from", 0);
        this.mBatteryPercentage = batteryPercentage;
        AnnotationValidations.validate(
                IntRange.class, null, mBatteryPercentage,
                "from", 0,
                "to", 100);
        this.mCarrier = carrier;
        AnnotationValidations.validate(
                NonNull.class, null, mCarrier);
        this.mNetworkCapabilities = networkCapabilities;
        this.mDataNetworkType = dataNetworkType;
        AnnotationValidations.validate(
                NetworkType.class, null, mDataNetworkType);
        this.mAppInfos = appInfos;
        AnnotationValidations.validate(
                NonNull.class, null, mAppInfos);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<UserData> CREATOR
            = new Parcelable.Creator<UserData>() {
        @Override
        public UserData[] newArray(int size) {
            return new UserData[size];
        }

        @Override
        public UserData createFromParcel(@NonNull android.os.Parcel in) {
            return new UserData(in);
        }
    };

    /**
     * A builder for {@link UserData}
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder {

        private int mTimezoneUtcOffsetMins;
        private @Orientation int mOrientation;
        private @IntRange(from = 0) long mAvailableStorageBytes;
        private @IntRange(from = 0, to = 100) int mBatteryPercentage;
        private @NonNull String mCarrier;
        private @Nullable NetworkCapabilities mNetworkCapabilities;
        private @NetworkType int mDataNetworkType;
        private @NonNull Map<String,AppInfo> mAppInfos;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * The device timezone +/- offset from UTC.
         *
         * @hide
         */
        @DataClass.Generated.Member
        public @NonNull Builder setTimezoneUtcOffsetMins(int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mTimezoneUtcOffsetMins = value;
            return this;
        }

        /**
         * The device orientation. The value can be one of the constants ORIENTATION_UNDEFINED,
         * ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE defined in
         * {@link android.content.res.Configuration}.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setOrientation(@Orientation int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mOrientation = value;
            return this;
        }

        /**
         * The available space on device in bytes.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setAvailableStorageBytes(@IntRange(from = 0) long value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4;
            mAvailableStorageBytes = value;
            return this;
        }

        /**
         * Battery percentage.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setBatteryPercentage(@IntRange(from = 0, to = 100) int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x8;
            mBatteryPercentage = value;
            return this;
        }

        /**
         * The Service Provider Name (SPN) returned by {@link TelephonyManager#getSimOperatorName()}
         */
        @DataClass.Generated.Member
        public @NonNull Builder setCarrier(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x10;
            mCarrier = value;
            return this;
        }

        /**
         * A filtered subset of the Network capabilities of the device that contains upstream
         * and downstream speeds, and whether the network is metered.
         * This is an instance of {@link NetworkCapabilities} that contains the capability
         * {@link android.net.NetworkCapabilities#NET_CAPABILITY_NOT_METERED} if the network is not
         * metered, and {@link NetworkCapabilities#getLinkDownstreamBandwidthKbps()} and
         * {@link NetworkCapabilities#getLinkUpstreamBandwidthKbps()} return the downstream and
         * upstream connection speeds. All other methods of this {@link NetworkCapabilities} object
         * return empty or default values.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setNetworkCapabilities(@NonNull NetworkCapabilities value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x20;
            mNetworkCapabilities = value;
            return this;
        }

        /**
         * Data network type. This is the value of
         * {@link android.telephony.TelephonyManager#getDataNetworkType()}.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setDataNetworkType(@NetworkType int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x40;
            mDataNetworkType = value;
            return this;
        }

        /**
         * A map from package name to app information for installed and uninstalled apps.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setAppInfos(@NonNull Map<String,AppInfo> value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x80;
            mAppInfos = value;
            return this;
        }

        /** @see #setAppInfos */
        @DataClass.Generated.Member
        public @NonNull Builder addAppInfo(@NonNull String key, @NonNull AppInfo value) {
            if (mAppInfos == null) setAppInfos(new java.util.LinkedHashMap());
            mAppInfos.put(key, value);
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull UserData build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x100; // Mark builder used

            if ((mBuilderFieldsSet & 0x1) == 0) {
                mTimezoneUtcOffsetMins = 0;
            }
            if ((mBuilderFieldsSet & 0x2) == 0) {
                mOrientation = 0;
            }
            if ((mBuilderFieldsSet & 0x4) == 0) {
                mAvailableStorageBytes = 0;
            }
            if ((mBuilderFieldsSet & 0x8) == 0) {
                mBatteryPercentage = 0;
            }
            if ((mBuilderFieldsSet & 0x10) == 0) {
                mCarrier = "";
            }
            if ((mBuilderFieldsSet & 0x20) == 0) {
                mNetworkCapabilities = null;
            }
            if ((mBuilderFieldsSet & 0x40) == 0) {
                mDataNetworkType = 0;
            }
            if ((mBuilderFieldsSet & 0x80) == 0) {
                mAppInfos = Collections.emptyMap();
            }
            UserData o = new UserData(
                    mTimezoneUtcOffsetMins,
                    mOrientation,
                    mAvailableStorageBytes,
                    mBatteryPercentage,
                    mCarrier,
                    mNetworkCapabilities,
                    mDataNetworkType,
                    mAppInfos);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x100) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1707172832988L,
            codegenVersion = "1.0.23",
            sourceFile = "packages/modules/OnDevicePersonalization/framework/java/android/adservices/ondevicepersonalization/UserData.java",
            inputSignatures = "  int mTimezoneUtcOffsetMins\n @android.adservices.ondevicepersonalization.UserData.Orientation int mOrientation\n @android.annotation.IntRange long mAvailableStorageBytes\n @android.annotation.IntRange int mBatteryPercentage\n @android.annotation.NonNull java.lang.String mCarrier\n @android.annotation.Nullable android.net.NetworkCapabilities mNetworkCapabilities\n @android.adservices.ondevicepersonalization.UserData.NetworkType int mDataNetworkType\n @com.android.ondevicepersonalization.internal.util.DataClass.PluralOf(\"appInfo\") @android.annotation.NonNull java.util.Map<java.lang.String,android.adservices.ondevicepersonalization.AppInfo> mAppInfos\npublic @android.annotation.NonNull java.time.Duration getTimezoneUtcOffset()\nclass UserData extends java.lang.Object implements [android.os.Parcelable]\n@com.android.ondevicepersonalization.internal.util.DataClass(genHiddenBuilder=true, genEqualsHashCode=true, genConstDefs=false)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
