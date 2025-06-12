/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.net.wifi.rtt;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.util.Objects;

/**
 * Secure ranging configuration.
 * Refer IEEE Std 802.11az-2022, section 12. Security.
 */
@FlaggedApi(Flags.FLAG_SECURE_RANGING)
public final class SecureRangingConfig implements Parcelable {
    private final boolean mEnableSecureHeLtf;
    private final boolean mEnableRangingFrameProtection;
    private final PasnConfig mPasnConfig;


    private SecureRangingConfig(boolean enableSecureHeLtf, boolean enableRangingFrameProtection,
            @NonNull PasnConfig pasnConfig) {
        Objects.requireNonNull(pasnConfig, "pasnConfig cannot be null");
        mEnableSecureHeLtf = enableSecureHeLtf;
        mEnableRangingFrameProtection = enableRangingFrameProtection;
        mPasnConfig = pasnConfig;
    }

    private SecureRangingConfig(@NonNull Parcel in) {
        mEnableSecureHeLtf = in.readByte() != 0;
        mEnableRangingFrameProtection = in.readByte() != 0;
        mPasnConfig = (SdkLevel.isAtLeastT()) ? in.readParcelable(PasnConfig.class.getClassLoader(),
                PasnConfig.class) : in.readParcelable(PasnConfig.class.getClassLoader());
    }

    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    public static final @NonNull Creator<SecureRangingConfig> CREATOR =
            new Creator<SecureRangingConfig>() {
                @Override
                public SecureRangingConfig createFromParcel(Parcel in) {
                    return new SecureRangingConfig(in);
                }

                @Override
                public SecureRangingConfig[] newArray(int size) {
                    return new SecureRangingConfig[size];
                }
            };

    private SecureRangingConfig(Builder builder) {
        mEnableSecureHeLtf = builder.mEnableSecureHeLtf;
        mEnableRangingFrameProtection = builder.mEnableRangingFrameProtection;
        mPasnConfig = builder.mPasnConfig;
    }

    /**
     * Returns whether secure HE-LTF is enabled or not.
     */
    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    public boolean isSecureHeLtfEnabled() {
        return mEnableSecureHeLtf;
    }

    /**
     * Returns whether ranging frame protection is enabled or not.
     */
    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    public boolean isRangingFrameProtectionEnabled() {
        return mEnableRangingFrameProtection;
    }

    /**
     * Returns Pre-association security negotiation (PASN) configuration used for secure
     * ranging.
     *
     * @return {@link PasnConfig} object.
     */
    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    @NonNull
    public PasnConfig getPasnConfig() {
        return mPasnConfig;
    }

    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    @Override
    public int describeContents() {
        return 0;
    }

    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeByte((byte) (mEnableSecureHeLtf ? 1 : 0));
        dest.writeByte((byte) (mEnableRangingFrameProtection ? 1 : 0));
        dest.writeParcelable(mPasnConfig, flags);
    }

    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    @Override
    public String toString() {
        return "SecureRangingConfig{" + "mEnableSecureHeLtf=" + mEnableSecureHeLtf
                + ", mEnableRangingProtection=" + mEnableRangingFrameProtection + ", mPasnConfig="
                + mPasnConfig + '}';
    }

    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecureRangingConfig that)) return false;
        return mEnableSecureHeLtf == that.mEnableSecureHeLtf
                && mEnableRangingFrameProtection == that.mEnableRangingFrameProtection
                && Objects.equals(mPasnConfig, that.mPasnConfig);
    }

    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    @Override
    public int hashCode() {
        return Objects.hash(mEnableSecureHeLtf, mEnableRangingFrameProtection, mPasnConfig);
    }

    /**
     * Builder for {@link SecureRangingConfig}
     */
    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    public static final class Builder {
        private boolean mEnableSecureHeLtf = true;
        private boolean mEnableRangingFrameProtection = true;
        private final PasnConfig mPasnConfig;

        /**
         * Builder constructor.
         *
         * @param pasnConfig PASN configuration
         */
        @FlaggedApi(Flags.FLAG_SECURE_RANGING)
        public Builder(@NonNull PasnConfig pasnConfig) {
            Objects.requireNonNull(pasnConfig, "pasnConfig must not be null");
            mPasnConfig = pasnConfig;
        }

        /**
         * Enable or disable secure HE-LTF and returns a reference to this Builder enabling
         * method chaining. If not set, secure HE-LTF is enabled.
         *
         * @param enableSecureHeLtf the {@code enableSecureHeLtf} to set
         * @return a reference to this Builder
         */
        @FlaggedApi(Flags.FLAG_SECURE_RANGING)
        @NonNull
        public Builder setSecureHeLtfEnabled(boolean enableSecureHeLtf) {
            this.mEnableSecureHeLtf = enableSecureHeLtf;
            return this;
        }

        /**
         * Enable or disable ranging frame protection  and returns a reference to this Builder
         * enabling method chaining. If not set, ranging frame protection is enabled.
         *
         * @param enableRangingFrameProtection the {@code enableRangingFrameProtection} to set
         * @return a reference to this Builder
         */
        @FlaggedApi(Flags.FLAG_SECURE_RANGING)
        @NonNull
        public Builder setRangingFrameProtectionEnabled(boolean enableRangingFrameProtection) {
            this.mEnableRangingFrameProtection = enableRangingFrameProtection;
            return this;
        }

        /**
         * Returns a {@code SecureRangingConfig} built from the parameters previously set.
         *
         * @return a {@code SecureRangingConfig} built with parameters of this
         * {@code SecureRangingConfig.Builder}
         */
        @FlaggedApi(Flags.FLAG_SECURE_RANGING)
        @NonNull
        public SecureRangingConfig build() {
            return new SecureRangingConfig(this);
        }
    }
}
