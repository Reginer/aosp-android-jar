/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.telecom;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.telecom.IVideoProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a connection that is used between Telecom and the ConnectionService.
 * This is used to send initial Connection information to Telecom when the connection is
 * first created.
 * @hide
 */
public final class ParcelableConnection implements Parcelable {
    private final PhoneAccountHandle mPhoneAccount;
    private final int mState;
    private final int mConnectionCapabilities;
    private final int mConnectionProperties;
    private final int mSupportedAudioRoutes;
    private final Uri mAddress;
    private final int mAddressPresentation;
    private final String mCallerDisplayName;
    private final int mCallerDisplayNamePresentation;
    private final IVideoProvider mVideoProvider;
    private final int mVideoState;
    private final boolean mRingbackRequested;
    private final boolean mIsVoipAudioMode;
    private final long mConnectTimeMillis;
    private final long mConnectElapsedTimeMillis;
    private final StatusHints mStatusHints;
    private final DisconnectCause mDisconnectCause;
    private final List<String> mConferenceableConnectionIds;
    private final Bundle mExtras;
    private String mParentCallId;
    private @Call.Details.CallDirection int mCallDirection;
    private @Connection.VerificationStatus int mCallerNumberVerificationStatus;

    /** @hide */
    public ParcelableConnection(
            PhoneAccountHandle phoneAccount,
            int state,
            int capabilities,
            int properties,
            int supportedAudioRoutes,
            Uri address,
            int addressPresentation,
            String callerDisplayName,
            int callerDisplayNamePresentation,
            IVideoProvider videoProvider,
            int videoState,
            boolean ringbackRequested,
            boolean isVoipAudioMode,
            long connectTimeMillis,
            long connectElapsedTimeMillis,
            StatusHints statusHints,
            DisconnectCause disconnectCause,
            List<String> conferenceableConnectionIds,
            Bundle extras,
            String parentCallId,
            @Call.Details.CallDirection int callDirection,
            @Connection.VerificationStatus int callerNumberVerificationStatus) {
        this(phoneAccount, state, capabilities, properties, supportedAudioRoutes, address,
                addressPresentation, callerDisplayName, callerDisplayNamePresentation,
                videoProvider, videoState, ringbackRequested, isVoipAudioMode, connectTimeMillis,
                connectElapsedTimeMillis, statusHints, disconnectCause, conferenceableConnectionIds,
                extras, callerNumberVerificationStatus);
        mParentCallId = parentCallId;
        mCallDirection = callDirection;
    }

    /** @hide */
    public ParcelableConnection(
            PhoneAccountHandle phoneAccount,
            int state,
            int capabilities,
            int properties,
            int supportedAudioRoutes,
            Uri address,
            int addressPresentation,
            String callerDisplayName,
            int callerDisplayNamePresentation,
            IVideoProvider videoProvider,
            int videoState,
            boolean ringbackRequested,
            boolean isVoipAudioMode,
            long connectTimeMillis,
            long connectElapsedTimeMillis,
            StatusHints statusHints,
            DisconnectCause disconnectCause,
            List<String> conferenceableConnectionIds,
            Bundle extras,
            @Connection.VerificationStatus int callerNumberVerificationStatus) {
        mPhoneAccount = phoneAccount;
        mState = state;
        mConnectionCapabilities = capabilities;
        mConnectionProperties = properties;
        mSupportedAudioRoutes = supportedAudioRoutes;
        mAddress = address;
        mAddressPresentation = addressPresentation;
        mCallerDisplayName = callerDisplayName;
        mCallerDisplayNamePresentation = callerDisplayNamePresentation;
        mVideoProvider = videoProvider;
        mVideoState = videoState;
        mRingbackRequested = ringbackRequested;
        mIsVoipAudioMode = isVoipAudioMode;
        mConnectTimeMillis = connectTimeMillis;
        mConnectElapsedTimeMillis = connectElapsedTimeMillis;
        mStatusHints = statusHints;
        mDisconnectCause = disconnectCause;
        mConferenceableConnectionIds = conferenceableConnectionIds;
        mExtras = extras;
        mParentCallId = null;
        mCallDirection = Call.Details.DIRECTION_UNKNOWN;
        mCallerNumberVerificationStatus = callerNumberVerificationStatus;
    }

    public PhoneAccountHandle getPhoneAccount() {
        return mPhoneAccount;
    }

    public int getState() {
        return mState;
    }

    /**
     * Returns the current connection capabilities bit-mask.  Connection capabilities are defined as
     * {@code CAPABILITY_*} constants in {@link Connection}.
     *
     * @return Bit-mask containing capabilities of the connection.
     */
    public int getConnectionCapabilities() {
        return mConnectionCapabilities;
    }

    /**
     * Returns the current connection properties bit-mask.  Connection properties are defined as
     * {@code PROPERTY_*} constants in {@link Connection}.
     *
     * @return Bit-mask containing properties of the connection.
     */
    public int getConnectionProperties() {
        return mConnectionProperties;
    }

    public int getSupportedAudioRoutes() {
        return mSupportedAudioRoutes;
    }

    public Uri getHandle() {
        return mAddress;
    }

    public int getHandlePresentation() {
        return mAddressPresentation;
    }

    public String getCallerDisplayName() {
        return mCallerDisplayName;
    }

    public int getCallerDisplayNamePresentation() {
        return mCallerDisplayNamePresentation;
    }

    public IVideoProvider getVideoProvider() {
        return mVideoProvider;
    }

    public int getVideoState() {
        return mVideoState;
    }

    public boolean isRingbackRequested() {
        return mRingbackRequested;
    }

    public boolean getIsVoipAudioMode() {
        return mIsVoipAudioMode;
    }

    public long getConnectTimeMillis() {
        return mConnectTimeMillis;
    }

    public long getConnectElapsedTimeMillis() {
        return mConnectElapsedTimeMillis;
    }

    public final StatusHints getStatusHints() {
        return mStatusHints;
    }

    public final DisconnectCause getDisconnectCause() {
        return mDisconnectCause;
    }

    public final List<String> getConferenceableConnectionIds() {
        return mConferenceableConnectionIds;
    }

    public final Bundle getExtras() {
        return mExtras;
    }

    public final String getParentCallId() {
        return mParentCallId;
    }

    public @Call.Details.CallDirection int getCallDirection() {
        return mCallDirection;
    }

    public @Connection.VerificationStatus int getCallerNumberVerificationStatus() {
        return mCallerNumberVerificationStatus;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ParcelableConnection [act:")
                .append(mPhoneAccount)
                .append("], state:")
                .append(mState)
                .append(", capabilities:")
                .append(Connection.capabilitiesToString(mConnectionCapabilities))
                .append(", properties:")
                .append(Connection.propertiesToString(mConnectionProperties))
                .append(", extras:")
                .append(mExtras)
                .append(", parent:")
                .append(mParentCallId)
                .append(", callDirection:")
                .append(mCallDirection)
                .toString();
    }

    public static final @android.annotation.NonNull Parcelable.Creator<ParcelableConnection> CREATOR =
            new Parcelable.Creator<ParcelableConnection> () {
        @Override
        public ParcelableConnection createFromParcel(Parcel source) {
            ClassLoader classLoader = ParcelableConnection.class.getClassLoader();

            PhoneAccountHandle phoneAccount = source.readParcelable(classLoader, android.telecom.PhoneAccountHandle.class);
            int state = source.readInt();
            int capabilities = source.readInt();
            Uri address = source.readParcelable(classLoader, android.net.Uri.class);
            int addressPresentation = source.readInt();
            String callerDisplayName = source.readString();
            int callerDisplayNamePresentation = source.readInt();
            IVideoProvider videoCallProvider =
                    IVideoProvider.Stub.asInterface(source.readStrongBinder());
            int videoState = source.readInt();
            boolean ringbackRequested = source.readByte() == 1;
            boolean audioModeIsVoip = source.readByte() == 1;
            long connectTimeMillis = source.readLong();
            StatusHints statusHints = source.readParcelable(classLoader, android.telecom.StatusHints.class);
            DisconnectCause disconnectCause = source.readParcelable(classLoader, android.telecom.DisconnectCause.class);
            List<String> conferenceableConnectionIds = new ArrayList<>();
            source.readStringList(conferenceableConnectionIds);
            Bundle extras = Bundle.setDefusable(source.readBundle(classLoader), true);
            int properties = source.readInt();
            int supportedAudioRoutes = source.readInt();
            String parentCallId = source.readString();
            long connectElapsedTimeMillis = source.readLong();
            int callDirection = source.readInt();
            int callerNumberVerificationStatus = source.readInt();

            return new ParcelableConnection(
                    phoneAccount,
                    state,
                    capabilities,
                    properties,
                    supportedAudioRoutes,
                    address,
                    addressPresentation,
                    callerDisplayName,
                    callerDisplayNamePresentation,
                    videoCallProvider,
                    videoState,
                    ringbackRequested,
                    audioModeIsVoip,
                    connectTimeMillis,
                    connectElapsedTimeMillis,
                    statusHints,
                    disconnectCause,
                    conferenceableConnectionIds,
                    extras,
                    parentCallId,
                    callDirection,
                    callerNumberVerificationStatus);
        }

        @Override
        public ParcelableConnection[] newArray(int size) {
            return new ParcelableConnection[size];
        }
    };

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Writes ParcelableConnection object into a Parcel. */
    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeParcelable(mPhoneAccount, 0);
        destination.writeInt(mState);
        destination.writeInt(mConnectionCapabilities);
        destination.writeParcelable(mAddress, 0);
        destination.writeInt(mAddressPresentation);
        destination.writeString(mCallerDisplayName);
        destination.writeInt(mCallerDisplayNamePresentation);
        destination.writeStrongBinder(
                mVideoProvider != null ? mVideoProvider.asBinder() : null);
        destination.writeInt(mVideoState);
        destination.writeByte((byte) (mRingbackRequested ? 1 : 0));
        destination.writeByte((byte) (mIsVoipAudioMode ? 1 : 0));
        destination.writeLong(mConnectTimeMillis);
        destination.writeParcelable(mStatusHints, 0);
        destination.writeParcelable(mDisconnectCause, 0);
        destination.writeStringList(mConferenceableConnectionIds);
        destination.writeBundle(mExtras);
        destination.writeInt(mConnectionProperties);
        destination.writeInt(mSupportedAudioRoutes);
        destination.writeString(mParentCallId);
        destination.writeLong(mConnectElapsedTimeMillis);
        destination.writeInt(mCallDirection);
        destination.writeInt(mCallerNumberVerificationStatus);
    }
}
