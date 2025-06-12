/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.telecom;

import android.Manifest;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.ICallScreeningAdapter;
import com.android.internal.telecom.ICallScreeningService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * This service can be implemented by the default dialer (see
 * {@link TelecomManager#getDefaultDialerPackage()}) or a third party app to allow or disallow
 * incoming calls before they are shown to a user. A {@link CallScreeningService} can also see
 * outgoing calls for the purpose of providing caller ID services for those calls.
 * <p>
 * Below is an example manifest registration for a {@code CallScreeningService}.
 * <pre>
 * {@code
 * <service android:name="your.package.YourCallScreeningServiceImplementation"
 *          android:permission="android.permission.BIND_SCREENING_SERVICE">
 *      <intent-filter>
 *          <action android:name="android.telecom.CallScreeningService"/>
 *      </intent-filter>
 * </service>
 * }
 * </pre>
 * <p>
 * A CallScreeningService performs two functions:
 * <ol>
 *     <li>Call blocking/screening - the service can choose which calls will ring on the user's
 *     device, and which will be silently sent to voicemail.</li>
 *     <li>Call identification - services which provide call identification functionality can
 *     display a user-interface of their choosing which contains identifying information for a call.
 *     </li>
 * </ol>
 * <p>
 * <h2>Becoming the CallScreeningService</h2>
 * Telecom will bind to a single app chosen by the user which implements the
 * {@link CallScreeningService} API when there are new incoming and outgoing calls.
 * <p>
 * The code snippet below illustrates how your app can request that it fills the call screening
 * role.
 * <pre>
 * {@code
 * private static final int REQUEST_ID = 1;
 *
 * public void requestRole() {
 *     RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
 *     Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
 *     startActivityForResult(intent, REQUEST_ID);
 * }
 *
 * &#64;Override
 * public void onActivityResult(int requestCode, int resultCode, Intent data) {
 *     if (requestCode == REQUEST_ID) {
 *         if (resultCode == android.app.Activity.RESULT_OK) {
 *             // Your app is now the call screening app
 *         } else {
 *             // Your app is not the call screening app
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>CallScreeningService Lifecycle</h2>
 *
 * The framework binds to the {@link CallScreeningService} implemented by the user-chosen app
 * filling the {@link android.app.role.RoleManager#ROLE_CALL_SCREENING} role when incoming calls are
 * received (prior to ringing) and when outgoing calls are placed.  The platform calls the
 * {@link #onScreenCall(Call.Details)} method to provide your service with details about the call.
 * <p>
 * For incoming calls, the {@link CallScreeningService} must call
 * {@link #respondToCall(Call.Details, CallResponse)} within 5 seconds of being bound to indicate to
 * the platform whether the call should be blocked or not.  Your app must do this even if it is
 * primarily performing caller ID operations and not screening calls.  It is important to perform
 * screening operations in a timely matter as the user's device will not begin ringing until the
 * response is received (or the timeout is hit).  A {@link CallScreeningService} may choose to
 * perform local database lookups to help determine if a call should be screened or not; care should
 * be taken to ensure the timeout is not repeatedly hit, causing delays in the incoming call flow.
 * <p>
 * If your app provides a caller ID experience, it should launch an activity to show the caller ID
 * information from {@link #onScreenCall(Call.Details)}.
 */
public abstract class CallScreeningService extends Service {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE = "android.telecom.CallScreeningService";

    private static final int MSG_SCREEN_CALL = 1;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SCREEN_CALL:
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        mCallScreeningAdapter = (ICallScreeningAdapter) args.arg1;
                        Call.Details callDetails = Call.Details
                                .createFromParcelableCall((ParcelableCall) args.arg2);
                        onScreenCall(callDetails);
                        if (callDetails.getCallDirection() == Call.Details.DIRECTION_OUTGOING) {
                            mCallScreeningAdapter.onScreeningResponse(
                                    callDetails.getTelecomCallId(),
                                    new ComponentName(getPackageName(), getClass().getName()),
                                    null);
                        }
                    } catch (RemoteException e) {
                        Log.w(this, "Exception when screening call: " + e);
                    } finally {
                        args.recycle();
                    }
                    break;
            }
        }
    };

    private final class CallScreeningBinder extends ICallScreeningService.Stub {
        @Override
        public void screenCall(ICallScreeningAdapter adapter, ParcelableCall call) {
            Log.v(this, "screenCall");
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = adapter;
            args.arg2 = call;
            mHandler.obtainMessage(MSG_SCREEN_CALL, args).sendToTarget();
        }
    }

    private ICallScreeningAdapter mCallScreeningAdapter;

    /**
     * Parcelable version of {@link CallResponse} used to do IPC.
     * @hide
     */
    public static class ParcelableCallResponse implements Parcelable {
        private final boolean mShouldDisallowCall;
        private final boolean mShouldRejectCall;
        private final boolean mShouldSilenceCall;
        private final boolean mShouldSkipCallLog;
        private final boolean mShouldSkipNotification;
        private final boolean mShouldScreenCallViaAudioProcessing;

        private final int mCallComposerAttachmentsToShow;

        private ParcelableCallResponse(
                boolean shouldDisallowCall,
                boolean shouldRejectCall,
                boolean shouldSilenceCall,
                boolean shouldSkipCallLog,
                boolean shouldSkipNotification,
                boolean shouldScreenCallViaAudioProcessing,
                int callComposerAttachmentsToShow) {
            mShouldDisallowCall = shouldDisallowCall;
            mShouldRejectCall = shouldRejectCall;
            mShouldSilenceCall = shouldSilenceCall;
            mShouldSkipCallLog = shouldSkipCallLog;
            mShouldSkipNotification = shouldSkipNotification;
            mShouldScreenCallViaAudioProcessing = shouldScreenCallViaAudioProcessing;
            mCallComposerAttachmentsToShow = callComposerAttachmentsToShow;
        }

        protected ParcelableCallResponse(Parcel in) {
            mShouldDisallowCall = in.readBoolean();
            mShouldRejectCall = in.readBoolean();
            mShouldSilenceCall = in.readBoolean();
            mShouldSkipCallLog = in.readBoolean();
            mShouldSkipNotification = in.readBoolean();
            mShouldScreenCallViaAudioProcessing = in.readBoolean();
            mCallComposerAttachmentsToShow = in.readInt();
        }

        public CallResponse toCallResponse() {
            return new CallResponse.Builder()
                    .setDisallowCall(mShouldDisallowCall)
                    .setRejectCall(mShouldRejectCall)
                    .setSilenceCall(mShouldSilenceCall)
                    .setSkipCallLog(mShouldSkipCallLog)
                    .setSkipNotification(mShouldSkipNotification)
                    .setShouldScreenCallViaAudioProcessing(mShouldScreenCallViaAudioProcessing)
                    .setCallComposerAttachmentsToShow(mCallComposerAttachmentsToShow)
                    .build();
        }

        public boolean shouldDisallowCall() {
            return mShouldDisallowCall;
        }

        public boolean shouldRejectCall() {
            return mShouldRejectCall;
        }

        public boolean shouldSilenceCall() {
            return mShouldSilenceCall;
        }

        public boolean shouldSkipCallLog() {
            return mShouldSkipCallLog;
        }

        public boolean shouldSkipNotification() {
            return mShouldSkipNotification;
        }

        public boolean shouldScreenCallViaAudioProcessing() {
            return mShouldScreenCallViaAudioProcessing;
        }

        public int getCallComposerAttachmentsToShow() {
            return mCallComposerAttachmentsToShow;
        }

        public static final Creator<ParcelableCallResponse> CREATOR =
                new Creator<ParcelableCallResponse>() {
                    @Override
                    public ParcelableCallResponse createFromParcel(Parcel in) {
                        return new ParcelableCallResponse(in);
                    }

                    @Override
                    public ParcelableCallResponse[] newArray(int size) {
                        return new ParcelableCallResponse[size];
                    }
                };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeBoolean(mShouldDisallowCall);
            dest.writeBoolean(mShouldRejectCall);
            dest.writeBoolean(mShouldSilenceCall);
            dest.writeBoolean(mShouldSkipCallLog);
            dest.writeBoolean(mShouldSkipNotification);
            dest.writeBoolean(mShouldScreenCallViaAudioProcessing);
            dest.writeInt(mCallComposerAttachmentsToShow);
        }
    }

    /**
     * Information about how to respond to an incoming call. Call screening apps can construct an
     * instance of this class using {@link CallResponse.Builder}.
     */
    public static class CallResponse {
        /**
         * Bit flag indicating whether to show the picture attachment for call composer.
         *
         * Used with {@link Builder#setCallComposerAttachmentsToShow(int)}.
         */
        public static final int CALL_COMPOSER_ATTACHMENT_PICTURE = 1;

        /**
         * Bit flag indicating whether to show the location attachment for call composer.
         *
         * Used with {@link Builder#setCallComposerAttachmentsToShow(int)}.
         */
        public static final int CALL_COMPOSER_ATTACHMENT_LOCATION = 1 << 1;

        /**
         * Bit flag indicating whether to show the subject attachment for call composer.
         *
         * Used with {@link Builder#setCallComposerAttachmentsToShow(int)}.
         */
        public static final int CALL_COMPOSER_ATTACHMENT_SUBJECT = 1 << 2;

        /**
         * Bit flag indicating whether to show the priority attachment for call composer.
         *
         * Used with {@link Builder#setCallComposerAttachmentsToShow(int)}.
         */
        public static final int CALL_COMPOSER_ATTACHMENT_PRIORITY = 1 << 3;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = "CALL_COMPOSER_ATTACHMENT_", flag = true,
                value = {
                        CALL_COMPOSER_ATTACHMENT_PICTURE,
                        CALL_COMPOSER_ATTACHMENT_LOCATION,
                        CALL_COMPOSER_ATTACHMENT_SUBJECT,
                        CALL_COMPOSER_ATTACHMENT_PRIORITY
                }
        )
        public @interface CallComposerAttachmentType {}

        private static final int NUM_CALL_COMPOSER_ATTACHMENT_TYPES = 4;

        private final boolean mShouldDisallowCall;
        private final boolean mShouldRejectCall;
        private final boolean mShouldSilenceCall;
        private final boolean mShouldSkipCallLog;
        private final boolean mShouldSkipNotification;
        private final boolean mShouldScreenCallViaAudioProcessing;
        private final int mCallComposerAttachmentsToShow;

        private CallResponse(
                boolean shouldDisallowCall,
                boolean shouldRejectCall,
                boolean shouldSilenceCall,
                boolean shouldSkipCallLog,
                boolean shouldSkipNotification,
                boolean shouldScreenCallViaAudioProcessing,
                int callComposerAttachmentsToShow) {
            if (!shouldDisallowCall
                    && (shouldRejectCall || shouldSkipCallLog || shouldSkipNotification)) {
                throw new IllegalStateException("Invalid response state for allowed call.");
            }

            if (shouldDisallowCall && shouldScreenCallViaAudioProcessing) {
                throw new IllegalStateException("Invalid response state for allowed call.");
            }

            mShouldDisallowCall = shouldDisallowCall;
            mShouldRejectCall = shouldRejectCall;
            mShouldSkipCallLog = shouldSkipCallLog;
            mShouldSkipNotification = shouldSkipNotification;
            mShouldSilenceCall = shouldSilenceCall;
            mShouldScreenCallViaAudioProcessing = shouldScreenCallViaAudioProcessing;
            mCallComposerAttachmentsToShow = callComposerAttachmentsToShow;
        }

        /*
         * @return Whether the incoming call should be blocked.
         */
        public boolean getDisallowCall() {
            return mShouldDisallowCall;
        }

        /*
         * @return Whether the incoming call should be disconnected as if the user had manually
         * rejected it.
         */
        public boolean getRejectCall() {
            return mShouldRejectCall;
        }

        /*
         * @return Whether the ringtone should be silenced for the incoming call.
         */
        public boolean getSilenceCall() {
            return mShouldSilenceCall;
        }

        /*
         * @return Whether the incoming call should not be displayed in the call log.
         */
        public boolean getSkipCallLog() {
            return mShouldSkipCallLog;
        }

        /*
         * @return Whether a missed call notification should not be shown for the incoming call.
         */
        public boolean getSkipNotification() {
            return mShouldSkipNotification;
        }

        /**
         * @return Whether we should enter the {@link Call#STATE_AUDIO_PROCESSING} state to allow
         * for further screening of the call.
         * @hide
         */
        public boolean getShouldScreenCallViaAudioProcessing() {
            return mShouldScreenCallViaAudioProcessing;
        }

        /**
         * @return A bitmask of call composer attachments that should be shown to the user.
         */
        public @CallComposerAttachmentType int getCallComposerAttachmentsToShow() {
            return mCallComposerAttachmentsToShow;
        }

        /** @hide */
        public ParcelableCallResponse toParcelable() {
            return new ParcelableCallResponse(
                    mShouldDisallowCall,
                    mShouldRejectCall,
                    mShouldSilenceCall,
                    mShouldSkipCallLog,
                    mShouldSkipNotification,
                    mShouldScreenCallViaAudioProcessing,
                    mCallComposerAttachmentsToShow
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CallResponse that = (CallResponse) o;
            return mShouldDisallowCall == that.mShouldDisallowCall &&
                    mShouldRejectCall == that.mShouldRejectCall &&
                    mShouldSilenceCall == that.mShouldSilenceCall &&
                    mShouldSkipCallLog == that.mShouldSkipCallLog &&
                    mShouldSkipNotification == that.mShouldSkipNotification &&
                    mShouldScreenCallViaAudioProcessing
                            == that.mShouldScreenCallViaAudioProcessing &&
                    mCallComposerAttachmentsToShow == that.mCallComposerAttachmentsToShow;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mShouldDisallowCall, mShouldRejectCall, mShouldSilenceCall,
                    mShouldSkipCallLog, mShouldSkipNotification,
                    mShouldScreenCallViaAudioProcessing,
                    mCallComposerAttachmentsToShow);
        }

        public static class Builder {
            private boolean mShouldDisallowCall;
            private boolean mShouldRejectCall;
            private boolean mShouldSilenceCall;
            private boolean mShouldSkipCallLog;
            private boolean mShouldSkipNotification;
            private boolean mShouldScreenCallViaAudioProcessing;
            private int mCallComposerAttachmentsToShow = -1;

            /**
             * Sets whether the incoming call should be blocked.
             */
            public Builder setDisallowCall(boolean shouldDisallowCall) {
                mShouldDisallowCall = shouldDisallowCall;
                return this;
            }

            /**
             * Sets whether the incoming call should be disconnected as if the user had manually
             * rejected it. This property should only be set to true if the call is disallowed.
             */
            public Builder setRejectCall(boolean shouldRejectCall) {
                mShouldRejectCall = shouldRejectCall;
                return this;
            }

            /**
             * Sets whether ringing should be silenced for the incoming call.  When set
             * to {@code true}, the Telecom framework will not play a ringtone for the call.
             * The call will, however, still be sent to the default dialer app if it is not blocked.
             * A {@link CallScreeningService} can use this to ensure a potential nuisance call is
             * still surfaced to the user, but in a less intrusive manner.
             *
             * Setting this to true only makes sense when the call has not been disallowed
             * using {@link #setDisallowCall(boolean)}.
             */
            public @NonNull Builder setSilenceCall(boolean shouldSilenceCall) {
                mShouldSilenceCall = shouldSilenceCall;
                return this;
            }

            /**
             * Sets whether the incoming call should not be displayed in the call log. This property
             * should only be set to true if the call is disallowed.
             * <p>
             * Note: Calls will still be logged with type
             * {@link android.provider.CallLog.Calls#BLOCKED_TYPE}, regardless of how this property
             * is set.
             * <p>
             * Note: Only the carrier and system call screening apps can use this parameter;
             * this parameter is ignored otherwise.
             */
            public Builder setSkipCallLog(boolean shouldSkipCallLog) {
                mShouldSkipCallLog = shouldSkipCallLog;
                return this;
            }

            /**
             * Sets whether a missed call notification should not be shown for the incoming call.
             * This property should only be set to true if the call is disallowed.
             */
            public Builder setSkipNotification(boolean shouldSkipNotification) {
                mShouldSkipNotification = shouldSkipNotification;
                return this;
            }

            /**
             * Sets whether to request background audio processing so that the in-call service can
             * screen the call further. If set to {@code true}, {@link #setDisallowCall} should be
             * called with {@code false}, and all other parameters in this builder will be ignored.
             * <p>
             * This request will only be honored if the {@link CallScreeningService} shares the same
             * uid as the system dialer app. Otherwise, the call will go through as usual.
             * <p>
             * Apps built with SDK version {@link android.os.Build.VERSION_CODES#R} or later which
             * are using the microphone as part of audio processing should specify the
             * foreground service type using the attribute
             * {@link android.R.attr#foregroundServiceType} in the {@link CallScreeningService}
             * service element of the app's manifest file.
             * The {@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MICROPHONE} attribute should be
             * specified.
             * @see
             * <a href="https://developer.android.com/preview/privacy/foreground-service-types">
             *     the Android Developer Site</a> for more information.
             *
             * @param shouldScreenCallViaAudioProcessing Whether to request further call screening.
             * @hide
             */
            @SystemApi
            @RequiresPermission(Manifest.permission.CAPTURE_AUDIO_OUTPUT)
            public @NonNull Builder setShouldScreenCallViaAudioProcessing(
                    boolean shouldScreenCallViaAudioProcessing) {
                mShouldScreenCallViaAudioProcessing = shouldScreenCallViaAudioProcessing;
                return this;
            }

            /**
             * Sets the call composer attachments that should be shown to the user.
             *
             * Attachments that are not shown will not be passed to the in-call UI responsible for
             * displaying the call to the user.
             *
             * If this method is not called on a {@link Builder}, all attachments will be shown,
             * except pictures, which will only be shown to users if the call is from a contact.
             *
             * Setting attachments to show will have no effect if the call screening service does
             * not belong to the same package as the system dialer (as returned by
             * {@link TelecomManager#getSystemDialerPackage()}).
             *
             * @param callComposerAttachmentsToShow A bitmask of call composer attachments to show.
             */
            public @NonNull Builder setCallComposerAttachmentsToShow(
                    @CallComposerAttachmentType int callComposerAttachmentsToShow) {
                // If the argument is less than zero (meaning unset), no-op since the conversion
                // to/from the parcelable version may call with that value.
                if (callComposerAttachmentsToShow < 0) {
                    return this;
                }

                if ((callComposerAttachmentsToShow
                        & (1 << NUM_CALL_COMPOSER_ATTACHMENT_TYPES)) != 0) {
                    throw new IllegalArgumentException("Attachment types must match the ones"
                            + " defined in CallResponse");
                }
                mCallComposerAttachmentsToShow = callComposerAttachmentsToShow;
                return this;
            }

            public CallResponse build() {
                return new CallResponse(
                        mShouldDisallowCall,
                        mShouldRejectCall,
                        mShouldSilenceCall,
                        mShouldSkipCallLog,
                        mShouldSkipNotification,
                        mShouldScreenCallViaAudioProcessing,
                        mCallComposerAttachmentsToShow);
            }
       }
    }

    public CallScreeningService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(this, "onBind");
        return new CallScreeningBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(this, "onUnbind");
        return false;
    }

    /**
     * Called when a new incoming or outgoing call is added.
     * <p>
     * A {@link CallScreeningService} must indicate whether an incoming call is allowed or not by
     * calling
     * {@link CallScreeningService#respondToCall(Call.Details, CallScreeningService.CallResponse)}.
     * Your app can tell if a call is an incoming call by checking to see if
     * {@link Call.Details#getCallDirection()} is {@link Call.Details#DIRECTION_INCOMING}.
     * <p>
     * <em>Note:</em> A {@link CallScreeningService} must respond to a call within 5 seconds.  After
     * this time, the framework will unbind from the {@link CallScreeningService} and ignore its
     * response.
     * <p>
     * <em>Note:</em> The {@link Call.Details} instance provided to a call screening service will
     * only have the following properties set.  The rest of the {@link Call.Details} properties will
     * be set to their default value or {@code null}.
     * <ul>
     *     <li>{@link Call.Details#getCallDirection()}</li>
     *     <li>{@link Call.Details#getCallerNumberVerificationStatus()}</li>
     *     <li>{@link Call.Details#getConnectTimeMillis()}</li>
     *     <li>{@link Call.Details#getCreationTimeMillis()}</li>
     *     <li>{@link Call.Details#getHandle()}</li>
     * </ul>
     * <p>
     * Only calls where the {@link Call.Details#getHandle() handle} {@link Uri#getScheme() scheme}
     * is {@link PhoneAccount#SCHEME_TEL} are passed for call
     * screening.  Further, only calls which are not in the user's contacts are passed for
     * screening, unless the {@link CallScreeningService} has been granted
     * {@link Manifest.permission#READ_CONTACTS} permission by the user.  For outgoing calls, no
     * post-dial digits are passed.
     * <p>
     * Calls with a {@link Call.Details#getHandlePresentation()} of
     * {@link TelecomManager#PRESENTATION_RESTRICTED}, {@link TelecomManager#PRESENTATION_UNKNOWN},
     * {@link TelecomManager#PRESENTATION_UNAVAILABLE} or
     * {@link TelecomManager#PRESENTATION_PAYPHONE} presentation are not provided to the
     * {@link CallScreeningService}.
     *
     * @param callDetails Information about a new call, see {@link Call.Details}.
     */
    public abstract void onScreenCall(@NonNull Call.Details callDetails);

    /**
     * Responds to the given incoming call, either allowing it, silencing it or disallowing it.
     * <p>
     * The {@link CallScreeningService} calls this method to inform the system whether the call
     * should be silently blocked or not. In the event that it should not be blocked, it may
     * also be requested to ring silently.
     * <p>
     * Calls to this method are ignored unless the {@link Call.Details#getCallDirection()} is
     * {@link Call.Details#DIRECTION_INCOMING}.
     * <p>
     * For incoming calls, a {@link CallScreeningService} MUST call this method within 5 seconds of
     * {@link #onScreenCall(Call.Details)} being invoked by the platform.
     * <p>
     * Calls which are blocked/rejected will be logged to the system call log with a call type of
     * {@link android.provider.CallLog.Calls#BLOCKED_TYPE} and
     * {@link android.provider.CallLog.Calls#BLOCK_REASON_CALL_SCREENING_SERVICE} block reason.
     *
     * @param callDetails The call to allow.
     *                    <p>
     *                    Must be the same {@link Call.Details call} which was provided to the
     *                    {@link CallScreeningService} via {@link #onScreenCall(Call.Details)}.
     * @param response The {@link CallScreeningService.CallResponse} which contains information
     * about how to respond to a call.
     */
    public final void respondToCall(@NonNull Call.Details callDetails,
            @NonNull CallResponse response) {
        try {
            mCallScreeningAdapter.onScreeningResponse(
                    callDetails.getTelecomCallId(),
                    new ComponentName(getPackageName(), getClass().getName()),
                    response.toParcelable());
        } catch (RemoteException e) {
            Log.e(this, e, "Got remote exception when returning response");
        }
    }
}
