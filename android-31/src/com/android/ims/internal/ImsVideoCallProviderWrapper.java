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
 * limitations under the License
 */

package com.android.ims.internal;

import android.compat.annotation.UnsupportedAppUsage;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.util.Log;
import android.view.Surface;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Subclass implementation of {@link Connection.VideoProvider}. This intermediates and
 * communicates with the actual implementation of the video call provider in the IMS service; it is
 * in essence, a wrapper around the IMS's video call provider implementation.
 *
 * This class maintains a binder by which the ImsVideoCallProvider's implementation can communicate
 * its intent to invoke callbacks. In this class, the message across this binder is handled, and
 * the superclass's methods are used to execute the callbacks.
 *
 * @hide
 */
public class ImsVideoCallProviderWrapper extends Connection.VideoProvider {

    public interface ImsVideoProviderWrapperCallback {
        void onReceiveSessionModifyResponse(int status, VideoProfile requestProfile,
                VideoProfile responseProfile);
    }

    private static final String LOG_TAG = ImsVideoCallProviderWrapper.class.getSimpleName();

    private static final int MSG_RECEIVE_SESSION_MODIFY_REQUEST = 1;
    private static final int MSG_RECEIVE_SESSION_MODIFY_RESPONSE = 2;
    private static final int MSG_HANDLE_CALL_SESSION_EVENT = 3;
    private static final int MSG_CHANGE_PEER_DIMENSIONS = 4;
    private static final int MSG_CHANGE_CALL_DATA_USAGE = 5;
    private static final int MSG_CHANGE_CAMERA_CAPABILITIES = 6;
    private static final int MSG_CHANGE_VIDEO_QUALITY = 7;

    private final IImsVideoCallProvider mVideoCallProvider;
    private final ImsVideoCallCallback mBinder;
    private RegistrantList mDataUsageUpdateRegistrants = new RegistrantList();
    private final Set<ImsVideoProviderWrapperCallback> mCallbacks = Collections.newSetFromMap(
            new ConcurrentHashMap<ImsVideoProviderWrapperCallback, Boolean>(8, 0.9f, 1));
    private VideoPauseTracker mVideoPauseTracker = new VideoPauseTracker();
    private boolean mUseVideoPauseWorkaround = false;
    private int mCurrentVideoState;
    private boolean mIsVideoEnabled = true;

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            try {
                mVideoCallProvider.asBinder().unlinkToDeath(this, 0);
            } catch (NoSuchElementException nse) {
                // Already unlinked, potentially below in tearDown.
            }
        }
    };

    /**
     * IImsVideoCallCallback stub implementation.
     */
    private final class ImsVideoCallCallback extends IImsVideoCallCallback.Stub {
        @Override
        public void receiveSessionModifyRequest(VideoProfile VideoProfile) {
            mHandler.obtainMessage(MSG_RECEIVE_SESSION_MODIFY_REQUEST,
                    VideoProfile).sendToTarget();
        }

        @Override
        public void receiveSessionModifyResponse(
                int status, VideoProfile requestProfile, VideoProfile responseProfile) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = status;
            args.arg2 = requestProfile;
            args.arg3 = responseProfile;
            mHandler.obtainMessage(MSG_RECEIVE_SESSION_MODIFY_RESPONSE, args).sendToTarget();
        }

        @Override
        public void handleCallSessionEvent(int event) {
            mHandler.obtainMessage(MSG_HANDLE_CALL_SESSION_EVENT, event).sendToTarget();
        }

        @Override
        public void changePeerDimensions(int width, int height) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = width;
            args.arg2 = height;
            mHandler.obtainMessage(MSG_CHANGE_PEER_DIMENSIONS, args).sendToTarget();
        }

        @Override
        public void changeVideoQuality(int videoQuality) {
            mHandler.obtainMessage(MSG_CHANGE_VIDEO_QUALITY, videoQuality, 0).sendToTarget();
        }

        @Override
        public void changeCallDataUsage(long dataUsage) {
            mHandler.obtainMessage(MSG_CHANGE_CALL_DATA_USAGE, dataUsage).sendToTarget();
        }

        @Override
        public void changeCameraCapabilities(
                VideoProfile.CameraCapabilities cameraCapabilities) {
            mHandler.obtainMessage(MSG_CHANGE_CAMERA_CAPABILITIES,
                    cameraCapabilities).sendToTarget();
        }
    }

    public void registerForDataUsageUpdate(Handler h, int what, Object obj) {
        mDataUsageUpdateRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForDataUsageUpdate(Handler h) {
        mDataUsageUpdateRegistrants.remove(h);
    }

    public void addImsVideoProviderCallback(ImsVideoProviderWrapperCallback callback) {
        mCallbacks.add(callback);
    }

    public void removeImsVideoProviderCallback(ImsVideoProviderWrapperCallback callback) {
        mCallbacks.remove(callback);
    }

    /** Default handler used to consolidate binder method calls onto a single thread. */
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            SomeArgs args;
            switch (msg.what) {
                case MSG_RECEIVE_SESSION_MODIFY_REQUEST: {
                    VideoProfile videoProfile = (VideoProfile) msg.obj;
                    if (!VideoProfile.isVideo(mCurrentVideoState) && VideoProfile.isVideo(
                            videoProfile.getVideoState()) && !mIsVideoEnabled) {
                        // Video is disabled, reject the request.
                        Log.i(LOG_TAG, String.format(
                                "receiveSessionModifyRequest: requestedVideoState=%s; rejecting "
                                        + "as video is disabled.",
                                videoProfile.getVideoState()));
                        try {
                            mVideoCallProvider.sendSessionModifyResponse(
                                    new VideoProfile(VideoProfile.STATE_AUDIO_ONLY));
                        } catch (RemoteException e) {
                        }
                        return;
                    }
                    receiveSessionModifyRequest(videoProfile);
                }
                break;
                case MSG_RECEIVE_SESSION_MODIFY_RESPONSE:
                    args = (SomeArgs) msg.obj;
                    try {
                        int status = (int) args.arg1;
                        VideoProfile requestProfile = (VideoProfile) args.arg2;
                        VideoProfile responseProfile = (VideoProfile) args.arg3;

                        receiveSessionModifyResponse(status, requestProfile, responseProfile);

                        // Notify any local Telephony components interested in upgrade responses.
                        for (ImsVideoProviderWrapperCallback callback : mCallbacks) {
                            if (callback != null) {
                                callback.onReceiveSessionModifyResponse(status, requestProfile,
                                        responseProfile);
                            }
                        }
                    } finally {
                        args.recycle();
                    }
                    break;
                case MSG_HANDLE_CALL_SESSION_EVENT:
                    handleCallSessionEvent((int) msg.obj);
                    break;
                case MSG_CHANGE_PEER_DIMENSIONS:
                    args = (SomeArgs) msg.obj;
                    try {
                        int width = (int) args.arg1;
                        int height = (int) args.arg2;
                        changePeerDimensions(width, height);
                    } finally {
                        args.recycle();
                    }
                    break;
                case MSG_CHANGE_CALL_DATA_USAGE:
                    // TODO: We should use callback in the future.
                    setCallDataUsage((long) msg.obj);
                    mDataUsageUpdateRegistrants.notifyResult(msg.obj);
                    break;
                case MSG_CHANGE_CAMERA_CAPABILITIES:
                    changeCameraCapabilities((VideoProfile.CameraCapabilities) msg.obj);
                    break;
                case MSG_CHANGE_VIDEO_QUALITY:
                    changeVideoQuality(msg.arg1);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Instantiates an instance of the ImsVideoCallProvider, taking in the binder for IMS's video
     * call provider implementation.
     *
     * @param videoProvider
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ImsVideoCallProviderWrapper(IImsVideoCallProvider videoProvider)
            throws RemoteException {

        mVideoCallProvider = videoProvider;
        if (videoProvider != null) {
            mVideoCallProvider.asBinder().linkToDeath(mDeathRecipient, 0);

            mBinder = new ImsVideoCallCallback();
            mVideoCallProvider.setCallback(mBinder);
        } else {
            mBinder = null;
        }
    }

    @VisibleForTesting
    public ImsVideoCallProviderWrapper(IImsVideoCallProvider videoProvider,
            VideoPauseTracker videoPauseTracker)
            throws RemoteException {
        this(videoProvider);
        mVideoPauseTracker = videoPauseTracker;
    }

    /** @inheritDoc */
    public void onSetCamera(String cameraId) {
        try {
            mVideoCallProvider.setCamera(cameraId, Binder.getCallingUid());
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onSetPreviewSurface(Surface surface) {
        try {
            mVideoCallProvider.setPreviewSurface(surface);
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onSetDisplaySurface(Surface surface) {
        try {
            mVideoCallProvider.setDisplaySurface(surface);
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onSetDeviceOrientation(int rotation) {
        try {
            mVideoCallProvider.setDeviceOrientation(rotation);
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onSetZoom(float value) {
        try {
            mVideoCallProvider.setZoom(value);
        } catch (RemoteException e) {
        }
    }

    /**
     * Handles session modify requests received from the {@link android.telecom.InCallService}.
     *
     * @inheritDoc
     **/
    public void onSendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile) {
        if (fromProfile == null || toProfile == null) {
            Log.w(LOG_TAG, "onSendSessionModifyRequest: null profile in request.");
            return;
        }

        try {
            if (isResumeRequest(fromProfile.getVideoState(), toProfile.getVideoState()) &&
                    !VideoProfile.isPaused(mCurrentVideoState)) {
                // Request is to resume, but we're already resumed so ignore the request.
                Log.i(LOG_TAG, String.format(
                        "onSendSessionModifyRequest: fromVideoState=%s, toVideoState=%s; "
                                + "skipping resume request - already resumed.",
                        VideoProfile.videoStateToString(fromProfile.getVideoState()),
                        VideoProfile.videoStateToString(toProfile.getVideoState())));
                return;
            }

            toProfile = maybeFilterPauseResume(fromProfile, toProfile,
                    VideoPauseTracker.SOURCE_INCALL);

            int fromVideoState = fromProfile.getVideoState();
            int toVideoState = toProfile.getVideoState();
            Log.i(LOG_TAG, String.format(
                    "onSendSessionModifyRequest: fromVideoState=%s, toVideoState=%s; ",
                    VideoProfile.videoStateToString(fromProfile.getVideoState()),
                    VideoProfile.videoStateToString(toProfile.getVideoState())));
            mVideoCallProvider.sendSessionModifyRequest(fromProfile, toProfile);
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onSendSessionModifyResponse(VideoProfile responseProfile) {
        try {
            mVideoCallProvider.sendSessionModifyResponse(responseProfile);
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onRequestCameraCapabilities() {
        try {
            mVideoCallProvider.requestCameraCapabilities();
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onRequestConnectionDataUsage() {
        try {
            mVideoCallProvider.requestCallDataUsage();
        } catch (RemoteException e) {
        }
    }

    /** @inheritDoc */
    public void onSetPauseImage(Uri uri) {
        try {
            mVideoCallProvider.setPauseImage(uri);
        } catch (RemoteException e) {
        }
    }

    /**
     * Determines if a session modify request represents a request to pause the video.
     *
     * @param from The from video state.
     * @param to The to video state.
     * @return {@code true} if a pause was requested.
     */
    @VisibleForTesting
    public static boolean isPauseRequest(int from, int to) {
        boolean fromPaused = VideoProfile.isPaused(from);
        boolean toPaused = VideoProfile.isPaused(to);

        return !fromPaused && toPaused;
    }

    /**
     * Determines if a session modify request represents a request to resume the video.
     *
     * @param from The from video state.
     * @param to The to video state.
     * @return {@code true} if a resume was requested.
     */
    @VisibleForTesting
    public static boolean isResumeRequest(int from, int to) {
        boolean fromPaused = VideoProfile.isPaused(from);
        boolean toPaused = VideoProfile.isPaused(to);

        return fromPaused && !toPaused;
    }

    /**
     * Determines if this request includes turning the camera off (ie turning off transmission).
     * @param from the from video state.
     * @param to the to video state.
     * @return true if the state change disables the user's camera.
     */
    @VisibleForTesting
    public static boolean isTurnOffCameraRequest(int from, int to) {
        return VideoProfile.isTransmissionEnabled(from)
                && !VideoProfile.isTransmissionEnabled(to);
    }

    /**
     * Determines if this request includes turning the camera on (ie turning on transmission).
     * @param from the from video state.
     * @param to the to video state.
     * @return true if the state change enables the user's camera.
     */
    @VisibleForTesting
    public static boolean isTurnOnCameraRequest(int from, int to) {
        return !VideoProfile.isTransmissionEnabled(from)
                && VideoProfile.isTransmissionEnabled(to);
    }

    /**
     * Filters incoming pause and resume requests based on whether there are other active pause or
     * resume requests at the current time.
     *
     * Requests to pause the video stream using the {@link VideoProfile#STATE_PAUSED} bit can come
     * from both the {@link android.telecom.InCallService}, as well as via the
     * {@link #pauseVideo(int, int)} and {@link #resumeVideo(int, int)} methods.  As a result,
     * multiple sources can potentially pause or resume the video stream.  This method ensures that
     * providing any one request source has paused the video that the video will remain paused.
     *
     * @param fromProfile The request's from {@link VideoProfile}.
     * @param toProfile The request's to {@link VideoProfile}.
     * @param source The source of the request, as identified by a {@code VideoPauseTracker#SOURCE*}
     *               constant.
     * @return The new toProfile, with the pause bit set or unset based on whether we should
     *      actually pause or resume the video at the current time.
     */
    @VisibleForTesting
    public VideoProfile maybeFilterPauseResume(VideoProfile fromProfile, VideoProfile toProfile,
            int source) {
        int fromVideoState = fromProfile.getVideoState();
        int toVideoState = toProfile.getVideoState();

        // TODO: Remove the following workaround in favor of a new API.
        // The current sendSessionModifyRequest API has a flaw.  If the video is already
        // paused, it is not possible for the IncallService to inform the VideoProvider that
        // it wishes to pause due to multi-tasking.
        // In a future release we should add a new explicity pauseVideo and resumeVideo API
        // instead of a difference between two video states.
        // For now, we'll assume if the request is from pause to pause, we'll still try to
        // pause.
        boolean isPauseSpecialCase = (source == VideoPauseTracker.SOURCE_INCALL &&
                VideoProfile.isPaused(fromVideoState) &&
                VideoProfile.isPaused(toVideoState));

        boolean isPauseRequest = isPauseRequest(fromVideoState, toVideoState) || isPauseSpecialCase;
        boolean isResumeRequest = isResumeRequest(fromVideoState, toVideoState);
        if (isPauseRequest) {
            Log.i(LOG_TAG, String.format("maybeFilterPauseResume: isPauseRequest (from=%s, to=%s)",
                    VideoProfile.videoStateToString(fromVideoState),
                    VideoProfile.videoStateToString(toVideoState)));
            // Check if we have already paused the video in the past.
            if (!mVideoPauseTracker.shouldPauseVideoFor(source) && !isPauseSpecialCase) {
                // Note: We don't want to remove the "pause" in the "special case" scenario. If we
                // do the resulting request will be from PAUSED --> UNPAUSED, which would resume the
                // video.

                // Video was already paused, so remove the pause in the "to" profile.
                toVideoState = toVideoState & ~VideoProfile.STATE_PAUSED;
                toProfile = new VideoProfile(toVideoState, toProfile.getQuality());
            }
        } else if (isResumeRequest) {
            boolean isTurnOffCameraRequest = isTurnOffCameraRequest(fromVideoState, toVideoState);
            boolean isTurnOnCameraRequest = isTurnOnCameraRequest(fromVideoState, toVideoState);
            // TODO: Fix vendor code so that this isn't required.
            // Some vendors do not properly handle turning the camera on/off when the video is
            // in paused state.
            // If the request is to turn on/off the camera, it might be in the unfortunate format:
            // FROM: Audio Tx Rx Pause TO: Audio Rx
            // FROM: Audio Rx Pause TO: Audio Rx Tx
            // If this is the case, we should not treat this request as a resume request as well.
            // Ideally the IMS stack should treat a turn off camera request as:
            // FROM: Audio Tx Rx Pause TO: Audio Rx Pause
            // FROM: Audio Rx Pause TO: Audio Rx Tx Pause
            // Unfortunately, it does not. ¯\_(ツ)_/¯
            if (mUseVideoPauseWorkaround && (isTurnOffCameraRequest || isTurnOnCameraRequest)) {
                Log.i(LOG_TAG, String.format("maybeFilterPauseResume: isResumeRequest,"
                                + " but camera turning on/off so skipping (from=%s, to=%s)",
                        VideoProfile.videoStateToString(fromVideoState),
                        VideoProfile.videoStateToString(toVideoState)));
                return toProfile;
            }
            Log.i(LOG_TAG, String.format("maybeFilterPauseResume: isResumeRequest (from=%s, to=%s)",
                    VideoProfile.videoStateToString(fromVideoState),
                    VideoProfile.videoStateToString(toVideoState)));
            // Check if we should remain paused (other pause requests pending).
            if (!mVideoPauseTracker.shouldResumeVideoFor(source)) {
                // There are other pause requests from other sources which are still active, so we
                // should remain paused.
                toVideoState = toVideoState | VideoProfile.STATE_PAUSED;
                toProfile = new VideoProfile(toVideoState, toProfile.getQuality());
            }
        }

        return toProfile;
    }

    /**
     * Issues a request to pause the video using {@link VideoProfile#STATE_PAUSED} from a source
     * other than the InCall UI.
     *
     * @param fromVideoState The current video state (prior to issuing the pause).
     * @param source The source of the pause request.
     */
    public void pauseVideo(int fromVideoState, int source) {
        if (mVideoPauseTracker.shouldPauseVideoFor(source)) {
            // We should pause the video (its not already paused).
            VideoProfile fromProfile = new VideoProfile(fromVideoState);
            VideoProfile toProfile = new VideoProfile(fromVideoState | VideoProfile.STATE_PAUSED);

            try {
                Log.i(LOG_TAG, String.format("pauseVideo: fromVideoState=%s, toVideoState=%s",
                        VideoProfile.videoStateToString(fromProfile.getVideoState()),
                        VideoProfile.videoStateToString(toProfile.getVideoState())));
                mVideoCallProvider.sendSessionModifyRequest(fromProfile, toProfile);
            } catch (RemoteException e) {
            }
        } else {
            Log.i(LOG_TAG, "pauseVideo: video already paused");
        }
    }

    /**
     * Issues a request to resume the video using {@link VideoProfile#STATE_PAUSED} from a source
     * other than the InCall UI.
     *
     * @param fromVideoState The current video state (prior to issuing the resume).
     * @param source The source of the resume request.
     */
    public void resumeVideo(int fromVideoState, int source) {
        if (mVideoPauseTracker.shouldResumeVideoFor(source)) {
            // We are the last source to resume, so resume now.
            VideoProfile fromProfile = new VideoProfile(fromVideoState);
            VideoProfile toProfile = new VideoProfile(fromVideoState & ~VideoProfile.STATE_PAUSED);

            try {
                Log.i(LOG_TAG, String.format("resumeVideo: fromVideoState=%s, toVideoState=%s",
                        VideoProfile.videoStateToString(fromProfile.getVideoState()),
                        VideoProfile.videoStateToString(toProfile.getVideoState())));
                mVideoCallProvider.sendSessionModifyRequest(fromProfile, toProfile);
            } catch (RemoteException e) {
            }
        } else {
            Log.i(LOG_TAG, "resumeVideo: remaining paused (paused from other sources)");
        }
    }

    /**
     * Determines if a specified source has issued a pause request.
     *
     * @param source The source.
     * @return {@code true} if the source issued a pause request, {@code false} otherwise.
     */
    public boolean wasVideoPausedFromSource(int source) {
        return mVideoPauseTracker.wasVideoPausedFromSource(source);
    }

    public void setUseVideoPauseWorkaround(boolean useVideoPauseWorkaround) {
        mUseVideoPauseWorkaround = useVideoPauseWorkaround;
    }

    /**
     * Called by {@code ImsPhoneConnection} when there is a change to the video state of the call.
     * Informs the video pause tracker that the video is no longer paused.  This ensures that
     * subsequent pause requests are not filtered out.
     *
     * @param newVideoState The new video state.
     */
    public void onVideoStateChanged(int newVideoState) {
        if (VideoProfile.isPaused(mCurrentVideoState) && !VideoProfile.isPaused(newVideoState)) {
            // New video state is un-paused, so clear any pending pause requests.
            Log.i(LOG_TAG, String.format("onVideoStateChanged: currentVideoState=%s,"
                            + " newVideoState=%s, clearing pending pause requests.",
                    VideoProfile.videoStateToString(mCurrentVideoState),
                    VideoProfile.videoStateToString(newVideoState)));
            mVideoPauseTracker.clearPauseRequests();
        } else {
            Log.d(LOG_TAG,
                    String.format("onVideoStateChanged: currentVideoState=%s, newVideoState=%s",
                            VideoProfile.videoStateToString(mCurrentVideoState),
                            VideoProfile.videoStateToString(newVideoState)));
        }
        mCurrentVideoState = newVideoState;
    }

    /**
     * Sets whether video is enabled locally or not.
     * Used to reject incoming video requests when video is disabled locally due to data being
     * disabled on a call where video calls are metered.
     * @param isVideoEnabled {@code true} if video is locally enabled, {@code false} otherwise.
     */
    public void setIsVideoEnabled(boolean isVideoEnabled) {
        mIsVideoEnabled = isVideoEnabled;
    }

    /**
     * Tears down the ImsVideoCallProviderWrapper.
     */
    public void tearDown() {
        if (mDeathRecipient != null) {
            try {
                mVideoCallProvider.asBinder().unlinkToDeath(mDeathRecipient, 0);
            } catch (NoSuchElementException nse) {
                // Already unlinked in binderDied above.
            }
        }
    }
}
