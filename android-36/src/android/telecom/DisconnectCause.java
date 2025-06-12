/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.telecom;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.media.ToneGenerator;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.Annotation;
import android.telephony.PreciseDisconnectCause;
import android.telephony.ims.ImsReasonInfo;
import android.text.TextUtils;

import androidx.annotation.IntDef;

import com.android.server.telecom.flags.Flags;

import java.lang.annotation.Retention;
import java.util.Objects;

/**
 * Describes the cause of a disconnected call. This always includes a code describing the generic
 * cause of the disconnect. Optionally, it may include a label and/or description to display to the
 * user. It is the responsibility of the {@link ConnectionService} to provide localized versions of
 * the label and description. It also may contain a reason for the disconnect, which is intended for
 * logging and not for display to the user.
 */
public final class DisconnectCause implements Parcelable {

    /** Disconnected because of an unknown or unspecified reason. */
    public static final int UNKNOWN = 0;
    /** Disconnected because there was an error, such as a problem with the network. */
    public static final int ERROR = 1;
    /** Disconnected because of a local user-initiated action, such as hanging up. */
    public static final int LOCAL = 2;
    /**
     * Disconnected because the remote party hung up an ongoing call, or because an outgoing call
     * was not answered by the remote party.
     */
    public static final int REMOTE = 3;
    /** Disconnected because it has been canceled. */
    public static final int CANCELED = 4;
    /** Disconnected because there was no response to an incoming call. */
    public static final int MISSED = 5;
    /** Disconnected because the user rejected an incoming call. */
    public static final int REJECTED = 6;
    /** Disconnected because the other party was busy. */
    public static final int BUSY = 7;
    /**
     * Disconnected because of a restriction on placing the call, such as dialing in airplane
     * mode.
     */
    public static final int RESTRICTED = 8;
    /** Disconnected for reason not described by other disconnect codes. */
    public static final int OTHER = 9;
    /**
     * Disconnected because the connection manager did not support the call. The call will be tried
     * again without a connection manager. See {@link PhoneAccount#CAPABILITY_CONNECTION_MANAGER}.
     */
    public static final int CONNECTION_MANAGER_NOT_SUPPORTED = 10;

    /**
     * Disconnected because the user did not locally answer the incoming call, but it was answered
     * on another device where the call was ringing.
     */
    public static final int ANSWERED_ELSEWHERE = 11;

    /**
     * Disconnected because the call was pulled from the current device to another device.
     */
    public static final int CALL_PULLED = 12;

    /**
     * @hide
     */
    @Retention(SOURCE)
    @FlaggedApi(Flags.FLAG_TELECOM_RESOLVE_HIDDEN_DEPENDENCIES)
    @IntDef({
            UNKNOWN,
            ERROR,
            LOCAL,
            REMOTE,
            CANCELED,
            MISSED,
            REJECTED,
            BUSY,
            RESTRICTED,
            OTHER,
            CONNECTION_MANAGER_NOT_SUPPORTED,
            ANSWERED_ELSEWHERE,
            CALL_PULLED
    })
    public @interface DisconnectCauseCode {}

    /**
     * Reason code (returned via {@link #getReason()}) which indicates that a call could not be
     * completed because the cellular radio is off or out of service, the device is connected to
     * a wifi network, but the user has not enabled wifi calling.
     */
    public static final String REASON_WIFI_ON_BUT_WFC_OFF = "REASON_WIFI_ON_BUT_WFC_OFF";

    /**
     * Reason code (returned via {@link #getReason()}), which indicates that the call was
     * disconnected because IMS access is blocked.
     */
    public static final String REASON_IMS_ACCESS_BLOCKED = "REASON_IMS_ACCESS_BLOCKED";

    /**
     * Reason code (returned via {@link #getReason()}), which indicates that the connection service
     * is setting the call's state to {@link Call#STATE_DISCONNECTED} because it is internally
     * changing the representation of an IMS conference call to simulate a single-party call.
     *
     * This reason code is only used for communication between a {@link ConnectionService} and
     * Telecom and should not be surfaced to the user.
     */
    public static final String REASON_EMULATING_SINGLE_CALL = "EMULATING_SINGLE_CALL";

    /**
     * This reason is set when a call is ended in order to place an emergency call when a
     * {@link PhoneAccount} doesn't support holding an ongoing call to place an emergency call. This
     * reason string should only be associated with the {@link #LOCAL} disconnect code returned from
     * {@link #getCode()}.
     */
    public static final String REASON_EMERGENCY_CALL_PLACED = "REASON_EMERGENCY_CALL_PLACED";

    private @DisconnectCauseCode int mDisconnectCode;
    private CharSequence mDisconnectLabel;
    private CharSequence mDisconnectDescription;
    private String mDisconnectReason;
    private int mToneToPlay;
    private int mTelephonyDisconnectCause;
    private int mTelephonyPreciseDisconnectCause;
    private ImsReasonInfo mImsReasonInfo;

    /**
     * Creates a new DisconnectCause.
     *
     * @param code The code for the disconnect cause.
     */
    public DisconnectCause(@DisconnectCauseCode int code) {
        this(code, null, null, null, ToneGenerator.TONE_UNKNOWN);
    }

    /**
     * Creates a new DisconnectCause.
     *
     * @param code The code for the disconnect cause.
     * @param reason The reason for the disconnect.
     */
    public DisconnectCause(@DisconnectCauseCode int code, String reason) {
        this(code, null, null, reason, ToneGenerator.TONE_UNKNOWN);
    }

    /**
     * Creates a new DisconnectCause.
     *
     * @param code The code for the disconnect cause.
     * @param label The localized label to show to the user to explain the disconnect.
     * @param description The localized description to show to the user to explain the disconnect.
     * @param reason The reason for the disconnect.
     */
    public DisconnectCause(@DisconnectCauseCode int code, CharSequence label,
            CharSequence description, String reason) {
        this(code, label, description, reason, ToneGenerator.TONE_UNKNOWN);
    }

    /**
     * Creates a new DisconnectCause.
     *
     * @param code The code for the disconnect cause.
     * @param label The localized label to show to the user to explain the disconnect.
     * @param description The localized description to show to the user to explain the disconnect.
     * @param reason The reason for the disconnect.
     * @param toneToPlay The tone to play on disconnect, as defined in {@link ToneGenerator}.
     */
    public DisconnectCause(@DisconnectCauseCode int code, CharSequence label,
            CharSequence description, String reason, int toneToPlay) {
        this(code, label, description, reason, toneToPlay,
                android.telephony.DisconnectCause.ERROR_UNSPECIFIED,
                PreciseDisconnectCause.ERROR_UNSPECIFIED, null /* imsReasonInfo */);
    }

    /**
     * Creates a new DisconnectCause instance. This is used by Telephony to pass in extra debug
     * info to Telecom regarding the disconnect cause.
     *
     * @param code The code for the disconnect cause.
     * @param label The localized label to show to the user to explain the disconnect.
     * @param description The localized description to show to the user to explain the disconnect.
     * @param reason The reason for the disconnect.
     * @param toneToPlay The tone to play on disconnect, as defined in {@link ToneGenerator}.
     * @param telephonyDisconnectCause The Telephony disconnect cause.
     * @param telephonyPreciseDisconnectCause The Telephony precise disconnect cause.
     * @param imsReasonInfo The relevant {@link ImsReasonInfo}, or {@code null} if not available.
     * @hide
     */
    public DisconnectCause(@DisconnectCauseCode int code, @Nullable CharSequence label,
            @Nullable CharSequence description, @Nullable String reason,
            int toneToPlay, @Annotation.DisconnectCauses int telephonyDisconnectCause,
            @Annotation.PreciseDisconnectCauses int telephonyPreciseDisconnectCause,
            @Nullable ImsReasonInfo imsReasonInfo) {
        mDisconnectCode = code;
        mDisconnectLabel = label;
        mDisconnectDescription = description;
        mDisconnectReason = reason;
        mToneToPlay = toneToPlay;
        mTelephonyDisconnectCause = telephonyDisconnectCause;
        mTelephonyPreciseDisconnectCause = telephonyPreciseDisconnectCause;
        mImsReasonInfo = imsReasonInfo;
    }

    /**
     * Returns the code for the reason for this disconnect.
     *
     * @return The disconnect code.
     */
    public @DisconnectCauseCode int getCode() {
        return mDisconnectCode;
    }

    /**
     * Returns a short label which explains the reason for the disconnect cause and is for display
     * in the user interface. If not null, it is expected that the In-Call UI should display this
     * text where it would normally display the call state ("Dialing", "Disconnected") and is
     * therefore expected to be relatively small. The {@link ConnectionService } is responsible for
     * providing and localizing this label. If there is no string provided, returns null.
     *
     * @return The disconnect label.
     */
    public CharSequence getLabel() {
        return mDisconnectLabel;
    }

    /**
     * Returns a description which explains the reason for the disconnect cause and is for display
     * in the user interface. This optional text is generally a longer and more descriptive version
     * of {@link #getLabel}, however it can exist even if {@link #getLabel} is empty. The In-Call UI
     * should display this relatively prominently; the traditional implementation displays this as
     * an alert dialog. The {@link ConnectionService} is responsible for providing and localizing
     * this message. If there is no string provided, returns null.
     *
     * @return The disconnect description.
     */
    public CharSequence getDescription() {
        return mDisconnectDescription;
    }

    /**
     * Returns an explanation of the reason for the disconnect. This is not intended for display to
     * the user and is used mainly for logging.
     *
     * @return The disconnect reason.
     */
    public String getReason() {
        return mDisconnectReason;
    }

    /**
     * Returns the telephony {@link android.telephony.DisconnectCause} for the call. This is only
     * used internally by Telecom for providing extra debug information from Telephony.
     *
     * @return The disconnect cause.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_TELECOM_RESOLVE_HIDDEN_DEPENDENCIES)
    public @Annotation.DisconnectCauses int getTelephonyDisconnectCause() {
        return mTelephonyDisconnectCause;
    }

    /**
     * Returns the telephony {@link android.telephony.PreciseDisconnectCause} for the call. This is
     * only used internally by Telecom for providing extra debug information from Telephony.
     *
     * @return The precise disconnect cause.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_TELECOM_RESOLVE_HIDDEN_DEPENDENCIES)
    public @Annotation.PreciseDisconnectCauses int getTelephonyPreciseDisconnectCause() {
        return mTelephonyPreciseDisconnectCause;
    }

    /**
     * Returns the telephony {@link ImsReasonInfo} associated with the call disconnection. This is
     * only used internally by Telecom for providing extra debug information from Telephony.
     *
     * @return The {@link ImsReasonInfo} or {@code null} if not known.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_TELECOM_RESOLVE_HIDDEN_DEPENDENCIES)
    public @Nullable ImsReasonInfo getImsReasonInfo() {
        return mImsReasonInfo;
    }

    /**
     * Returns the tone to play when disconnected.
     *
     * @return the tone as defined in {@link ToneGenerator} to play when disconnected.
     */
    public int getTone() {
        return mToneToPlay;
    }

    /**
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_TELECOM_RESOLVE_HIDDEN_DEPENDENCIES)
    public static final class Builder {
        private @DisconnectCauseCode int mDisconnectCode;
        private CharSequence mDisconnectLabel;
        private CharSequence mDisconnectDescription;
        private String mDisconnectReason;
        private int mToneToPlay = ToneGenerator.TONE_UNKNOWN;
        private int mTelephonyDisconnectCause;
        private int mTelephonyPreciseDisconnectCause;
        private ImsReasonInfo mImsReasonInfo;

        public Builder(@DisconnectCauseCode int code) {
            mDisconnectCode = code;
        }

        /**
         * Sets a label which explains the reason for the disconnect cause, used for display in the
         * user interface.
         * @param label The label to associate with the disconnect cause.
         * @return The {@link DisconnectCause} builder instance.
         */
        public @NonNull DisconnectCause.Builder setLabel(@Nullable CharSequence label) {
            mDisconnectLabel = label;
            return this;
        }

        /**
         * Sets a description which provides the reason for the disconnect cause, used for display
         * in the user interface.
         * @param description The description to associate with the disconnect cause.
         * @return The {@link DisconnectCause} builder instance.
         */
        public @NonNull DisconnectCause.Builder setDescription(
                @Nullable CharSequence description) {
            mDisconnectDescription = description;
            return this;
        }

        /**
         * Sets a reason providing explanation for the disconnect (intended for logging and not for
         * displaying in the user interface).
         * @param reason The reason for the disconnect.
         * @return The {@link DisconnectCause} builder instance.
         */
        public @NonNull DisconnectCause.Builder setReason(@NonNull String reason) {
            mDisconnectReason = reason;
            return this;
        }

        /**
         * Sets the tone to play when disconnected.
         * @param toneToPlay The tone as defined in {@link ToneGenerator} to play when disconnected.
         * @return The {@link DisconnectCause} builder instance.
         */
        public @NonNull DisconnectCause.Builder setTone(int toneToPlay) {
            mToneToPlay = toneToPlay;
            return this;
        }

        /**
         * Sets the telephony {@link android.telephony.DisconnectCause} for the call (used
         * internally by Telecom for providing extra debug information from Telephony).
         * @param telephonyDisconnectCause The disconnect cause as provided by Telephony.
         * @return The {@link DisconnectCause} builder instance.
         */
        public @NonNull DisconnectCause.Builder setTelephonyDisconnectCause(
                @Annotation.DisconnectCauses int telephonyDisconnectCause) {
            mTelephonyDisconnectCause = telephonyDisconnectCause;
            return this;
        }

        /**
         * Sets the telephony {@link android.telephony.PreciseDisconnectCause} for the call (used
         * internally by Telecom for providing extra debug information from Telephony).
         * @param telephonyPreciseDisconnectCause The precise disconnect cause as provided by
         *                                        Telephony.
         * @return The {@link DisconnectCause} builder instance.
         */

        public @NonNull DisconnectCause.Builder setTelephonyPreciseDisconnectCause(
                @Annotation.PreciseDisconnectCauses int telephonyPreciseDisconnectCause) {
            mTelephonyPreciseDisconnectCause = telephonyPreciseDisconnectCause;
            return this;
        }

        /**
         * Sets the telephony {@link ImsReasonInfo} associated with the call disconnection. This
         * is only used internally by Telecom for providing extra debug information from Telephony.
         *
         * @param imsReasonInfo The {@link ImsReasonInfo} or {@code null} if not known.
         * @return The {@link DisconnectCause} builder instance.
         */
        public @NonNull DisconnectCause.Builder setImsReasonInfo(
                @Nullable ImsReasonInfo imsReasonInfo) {
            mImsReasonInfo = imsReasonInfo;
            return this;
        }

        /**
         * Build the {@link DisconnectCause} from the provided Builder config.
         * @return The {@link DisconnectCause} instance from provided builder.
         */
        public @NonNull DisconnectCause build() {
            return new DisconnectCause(mDisconnectCode, mDisconnectLabel, mDisconnectDescription,
                    mDisconnectReason, mToneToPlay, mTelephonyDisconnectCause,
                    mTelephonyPreciseDisconnectCause, mImsReasonInfo);
        }
    }

    public static final @android.annotation.NonNull Creator<DisconnectCause> CREATOR
            = new Creator<DisconnectCause>() {
        @Override
        public DisconnectCause createFromParcel(Parcel source) {
            int code = source.readInt();
            CharSequence label = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            CharSequence description = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            String reason = source.readString();
            int tone = source.readInt();
            int telephonyDisconnectCause = source.readInt();
            int telephonyPreciseDisconnectCause = source.readInt();
            ImsReasonInfo imsReasonInfo = source.readParcelable(null, android.telephony.ims.ImsReasonInfo.class);
            return new DisconnectCause(code, label, description, reason, tone,
                    telephonyDisconnectCause, telephonyPreciseDisconnectCause, imsReasonInfo);
        }

        @Override
        public DisconnectCause[] newArray(int size) {
            return new DisconnectCause[size];
        }
    };

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeInt(mDisconnectCode);
        TextUtils.writeToParcel(mDisconnectLabel, destination, flags);
        TextUtils.writeToParcel(mDisconnectDescription, destination, flags);
        destination.writeString(mDisconnectReason);
        destination.writeInt(mToneToPlay);
        destination.writeInt(mTelephonyDisconnectCause);
        destination.writeInt(mTelephonyPreciseDisconnectCause);
        destination.writeParcelable(mImsReasonInfo, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mDisconnectCode)
                + Objects.hashCode(mDisconnectLabel)
                + Objects.hashCode(mDisconnectDescription)
                + Objects.hashCode(mDisconnectReason)
                + Objects.hashCode(mToneToPlay)
                + Objects.hashCode(mTelephonyDisconnectCause)
                + Objects.hashCode(mTelephonyPreciseDisconnectCause)
                + Objects.hashCode(mImsReasonInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DisconnectCause) {
            DisconnectCause d = (DisconnectCause) o;
            return Objects.equals(mDisconnectCode, d.getCode())
                    && Objects.equals(mDisconnectLabel, d.getLabel())
                    && Objects.equals(mDisconnectDescription, d.getDescription())
                    && Objects.equals(mDisconnectReason, d.getReason())
                    && Objects.equals(mToneToPlay, d.getTone())
                    && Objects.equals(mTelephonyDisconnectCause, d.getTelephonyDisconnectCause())
                    && Objects.equals(mTelephonyPreciseDisconnectCause,
                    d.getTelephonyPreciseDisconnectCause())
                    && Objects.equals(mImsReasonInfo, d.getImsReasonInfo());
        }
        return false;
    }

    @Override
    public String toString() {
        String code = "";
        switch (mDisconnectCode) {
            case UNKNOWN:
                code = "UNKNOWN";
                break;
            case ERROR:
                code = "ERROR";
                break;
            case LOCAL:
                code = "LOCAL";
                break;
            case REMOTE:
                code = "REMOTE";
                break;
            case CANCELED:
                code = "CANCELED";
                break;
            case MISSED:
                code = "MISSED";
                break;
            case REJECTED:
                code = "REJECTED";
                break;
            case BUSY:
                code = "BUSY";
                break;
            case RESTRICTED:
                code = "RESTRICTED";
                break;
            case OTHER:
                code = "OTHER";
                break;
            case CONNECTION_MANAGER_NOT_SUPPORTED:
                code = "CONNECTION_MANAGER_NOT_SUPPORTED";
                break;
            case CALL_PULLED:
                code = "CALL_PULLED";
                break;
            case ANSWERED_ELSEWHERE:
                code = "ANSWERED_ELSEWHERE";
                break;
            default:
                code = "invalid code: " + mDisconnectCode;
                break;
        }
        String label = mDisconnectLabel == null ? "" : mDisconnectLabel.toString();
        String description = mDisconnectDescription == null
                ? "" : mDisconnectDescription.toString();
        String reason = mDisconnectReason == null ? "" : mDisconnectReason;
        return "DisconnectCause [ Code: (" + code + ")"
                + " Label: (" + label + ")"
                + " Description: (" + description + ")"
                + " Reason: (" + reason + ")"
                + " Tone: (" + mToneToPlay + ") "
                + " TelephonyCause: " + mTelephonyDisconnectCause + "/"
                + mTelephonyPreciseDisconnectCause
                + " ImsReasonInfo: "
                + mImsReasonInfo
                + "]";
    }
}
