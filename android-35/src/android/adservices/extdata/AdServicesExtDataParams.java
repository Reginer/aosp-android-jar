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

package android.adservices.extdata;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Container for the data fields handled by {@link AdServicesExtDataStorageService}.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ADEXT_DATA_SERVICE_APIS_ENABLED)
public final class AdServicesExtDataParams implements Parcelable {
    /**
     * Custom tri-state boolean type to represent true, false, and unknown
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "BOOLEAN_",
            value = {BOOLEAN_TRUE, BOOLEAN_FALSE, BOOLEAN_UNKNOWN})
    public @interface TriStateBoolean {}

    /**
     * Int value to represent true.
     *
     * @hide
     */
    public static final int BOOLEAN_TRUE = 1;

    /**
     * Int value to represent false.
     *
     * @hide
     */
    public static final int BOOLEAN_FALSE = 0;

    /**
     * Int value to represent unknown.
     *
     * @hide
     */
    public static final int BOOLEAN_UNKNOWN = -1;

    /**
     * Type to represent user manual interaction state.
     *
     * @hide
     */
    @IntDef(
            prefix = "STATE_",
            value = {
                STATE_NO_MANUAL_INTERACTIONS_RECORDED,
                STATE_UNKNOWN,
                STATE_MANUAL_INTERACTIONS_RECORDED
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UserManualInteraction {}

    /**
     * Int value to represent no manual interaction recorded state.
     *
     * @hide
     */
    public static final int STATE_NO_MANUAL_INTERACTIONS_RECORDED = -1;

    /**
     * Int value to represent unknown manual interaction state.
     *
     * @hide
     */
    public static final int STATE_UNKNOWN = 0;

    /**
     * Int value to represent manual interaction reported state.
     *
     * @hide
     */
    public static final int STATE_MANUAL_INTERACTIONS_RECORDED = 1;

    @TriStateBoolean private final int mIsNotificationDisplayed;
    @TriStateBoolean private final int mIsMeasurementConsented;
    @TriStateBoolean private final int mIsU18Account;
    @TriStateBoolean private final int mIsAdultAccount;
    @UserManualInteraction private final int mManualInteractionWithConsentStatus;
    private final long mMeasurementRollbackApexVersion;

    /**
     * Init AdServicesExtDataParams.
     *
     * @param isNotificationDisplayed 1 if notification is displayed, 0 if notification not
     *     displayed, -1 to represent no data.
     * @param isMeasurementConsented 1 if measurement consented, 0 if not, -1 to represent no data.
     * @param isU18Account 1 if account is U18, 0 if not, -1 if no data.
     * @param isAdultAccount 1 if adult account, 0 if not, -1 if no data.
     * @param manualInteractionWithConsentStatus 1 if user interacted, -1 if not, 0 if unknown.
     * @param measurementRollbackApexVersion ExtServices apex version for measurement rollback
     *     handling. -1 if no data.
     */
    public AdServicesExtDataParams(
            @TriStateBoolean int isNotificationDisplayed,
            @TriStateBoolean int isMeasurementConsented,
            @TriStateBoolean int isU18Account,
            @TriStateBoolean int isAdultAccount,
            @UserManualInteraction int manualInteractionWithConsentStatus,
            long measurementRollbackApexVersion) {
        mIsNotificationDisplayed = isNotificationDisplayed;
        mIsMeasurementConsented = isMeasurementConsented;
        mIsU18Account = isU18Account;
        mIsAdultAccount = isAdultAccount;
        mManualInteractionWithConsentStatus = manualInteractionWithConsentStatus;
        mMeasurementRollbackApexVersion = measurementRollbackApexVersion;
    }

    private AdServicesExtDataParams(@NonNull Parcel in) {
        mIsNotificationDisplayed = in.readInt();
        mIsMeasurementConsented = in.readInt();
        mIsU18Account = in.readInt();
        mIsAdultAccount = in.readInt();
        mManualInteractionWithConsentStatus = in.readInt();
        mMeasurementRollbackApexVersion = in.readLong();
    }

    /** Creator for Parcelable. */
    @NonNull
    public static final Creator<AdServicesExtDataParams> CREATOR =
            new Creator<AdServicesExtDataParams>() {
                @Override
                public AdServicesExtDataParams createFromParcel(Parcel in) {
                    return new AdServicesExtDataParams(in);
                }

                @Override
                public AdServicesExtDataParams[] newArray(int size) {
                    return new AdServicesExtDataParams[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mIsNotificationDisplayed);
        out.writeInt(mIsMeasurementConsented);
        out.writeInt(mIsU18Account);
        out.writeInt(mIsAdultAccount);
        out.writeInt(mManualInteractionWithConsentStatus);
        out.writeLong(mMeasurementRollbackApexVersion);
    }

    /** Returns 1 if notification was shown on R, 0 if not shown, -1 if unknown. */
    @TriStateBoolean
    public int getIsNotificationDisplayed() {
        return mIsNotificationDisplayed;
    }

    /** Returns 1 if measurement was consented, 0 if not, -1 if unknown. */
    @TriStateBoolean
    public int getIsMeasurementConsented() {
        return mIsMeasurementConsented;
    }

    /** Returns 1 if account is U18 account, 0 if not, -1 if unknown. */
    @TriStateBoolean
    public int getIsU18Account() {
        return mIsU18Account;
    }

    /** Returns 1 if account is adult account, 0 if not, -1 if unknown. */
    @TriStateBoolean
    public int getIsAdultAccount() {
        return mIsAdultAccount;
    }

    /** Returns 1 if user interacted, -1 if not, 0 if unknown. */
    @UserManualInteraction
    public int getManualInteractionWithConsentStatus() {
        return mManualInteractionWithConsentStatus;
    }

    /**
     * Returns ExtServices apex version for handling measurement rollback. -1 is returned if no data
     * is available.
     */
    public long getMeasurementRollbackApexVersion() {
        return mMeasurementRollbackApexVersion;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format(
                "AdServicesExtDataParams{"
                        + "mIsNotificationDisplayed=%d, "
                        + "mIsMsmtConsented=%d, "
                        + "mIsU18Account=%d, "
                        + "mIsAdultAccount=%d, "
                        + "mManualInteractionWithConsentStatus=%d, "
                        + "mMsmtRollbackApexVersion=%d}",
                mIsNotificationDisplayed,
                mIsMeasurementConsented,
                mIsU18Account,
                mIsAdultAccount,
                mManualInteractionWithConsentStatus,
                mMeasurementRollbackApexVersion);
    }

    /**
     * Builder for {@link AdServicesExtDataParams} objects.
     *
     * @hide
     */
    public static final class Builder {
        @TriStateBoolean private int mNotificationDisplayed;
        @TriStateBoolean private int mMsmtConsent;
        @TriStateBoolean private int mIsU18Account;
        @TriStateBoolean private int mIsAdultAccount;
        @UserManualInteraction private int mManualInteractionWithConsentStatus;
        private long mMsmtRollbackApexVersion;

        /** Set the isNotificationDisplayed. */
        @NonNull
        public AdServicesExtDataParams.Builder setNotificationDisplayed(
                @TriStateBoolean int notificationDisplayed) {
            mNotificationDisplayed = notificationDisplayed;
            return this;
        }

        /** Set the isMeasurementConsented. */
        @NonNull
        public AdServicesExtDataParams.Builder setMsmtConsent(@TriStateBoolean int msmtConsent) {
            mMsmtConsent = msmtConsent;
            return this;
        }

        /** Set the isU18Account. */
        @NonNull
        public AdServicesExtDataParams.Builder setIsU18Account(@TriStateBoolean int isU18Account) {
            mIsU18Account = isU18Account;
            return this;
        }

        /** Set the isAdultAccount. */
        @NonNull
        public AdServicesExtDataParams.Builder setIsAdultAccount(
                @TriStateBoolean int isAdultAccount) {
            mIsAdultAccount = isAdultAccount;
            return this;
        }

        /** Set the manualInteractionWithConsentStatus. */
        @NonNull
        public AdServicesExtDataParams.Builder setManualInteractionWithConsentStatus(
                @UserManualInteraction int manualInteractionWithConsentStatus) {
            mManualInteractionWithConsentStatus = manualInteractionWithConsentStatus;
            return this;
        }

        /** Set the msmtRollbackApexVersion. */
        @NonNull
        public AdServicesExtDataParams.Builder setMsmtRollbackApexVersion(
                long msmtRollbackApexVersion) {
            mMsmtRollbackApexVersion = msmtRollbackApexVersion;
            return this;
        }

        public Builder() {}

        /** Builds a {@link AdServicesExtDataParams} instance. */
        @NonNull
        public AdServicesExtDataParams build() {
            return new AdServicesExtDataParams(
                    mNotificationDisplayed,
                    mMsmtConsent,
                    mIsU18Account,
                    mIsAdultAccount,
                    mManualInteractionWithConsentStatus,
                    mMsmtRollbackApexVersion);
        }
    }
}
