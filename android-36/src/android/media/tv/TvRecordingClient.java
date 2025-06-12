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
 * limitations under the License.
 */

package android.media.tv;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.media.tv.TvInputManager;
import android.media.tv.interactive.TvInteractiveAppView;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/**
 * The public interface object used to interact with a specific TV input service for TV program
 * recording.
 */
public class TvRecordingClient {
    private static final String TAG = "TvRecordingClient";
    private static final boolean DEBUG = false;

    private final RecordingCallback mCallback;
    private final Handler mHandler;

    private final TvInputManager mTvInputManager;
    private TvInputManager.Session mSession;
    private MySessionCallback mSessionCallback;

    private boolean mIsRecordingStarted;
    private boolean mIsTuned;
    private boolean mIsPaused;
    private boolean mIsRecordingStopping;
    private final Queue<Pair<String, Bundle>> mPendingAppPrivateCommands = new ArrayDeque<>();
    private TvInteractiveAppView mTvIAppView;
    private String mRecordingId;

    /**
     * Creates a new TvRecordingClient object.
     *
     * @param context The application context to create a TvRecordingClient with.
     * @param tag A short name for debugging purposes.
     * @param callback The callback to receive recording status changes.
     * @param handler The handler to invoke the callback on.
     */
    public TvRecordingClient(Context context, String tag, @NonNull RecordingCallback callback,
            Handler handler) {
        mCallback = callback;
        mHandler = handler == null ? new Handler(Looper.getMainLooper()) : handler;
        mTvInputManager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
    }

    /**
     * Sets the related {@link TvInteractiveAppView} instance so the interactive app service can be
     * notified for recording events.
     *
     * @param view The related {@link TvInteractiveAppView} instance that is linked to this TV
     *             recording client. {@code null} to unlink the view.
     * @param recordingId The ID of the recording which is assigned by the TV application.
     *                    {@code null} if and only if the TvInteractiveAppView parameter is
     *                    {@code null}.
     * @throws IllegalArgumentException when recording ID is {@code null} and the
     *                                  TvInteractiveAppView is not {@code null}; or when recording
     *                                  ID is not {@code null} and the TvInteractiveAppView is
     *                                  {@code null}.
     * @see TvInteractiveAppView#notifyRecordingScheduled(String, String)
     * @see TvInteractiveAppView#notifyRecordingStarted(String, String)
     */
    public void setTvInteractiveAppView(
            @Nullable TvInteractiveAppView view, @Nullable String recordingId) {
        if (view != null && recordingId == null) {
            throw new IllegalArgumentException(
                    "null recordingId is not allowed only when the view is not null");
        }
        if (view == null && recordingId != null) {
            throw new IllegalArgumentException(
                    "recordingId should be null when the view is null");
        }
        mTvIAppView = view;
        mRecordingId = recordingId;
    }

    /**
     * Tunes to a given channel for TV program recording. The first tune request will create a new
     * recording session for the corresponding TV input and establish a connection between the
     * application and the session. If recording has already started in the current recording
     * session, this method throws an exception.
     *
     * <p>The application may call this method before starting or after stopping recording, but not
     * during recording.
     *
     * <p>The recording session will respond by calling
     * {@link RecordingCallback#onTuned(Uri)} if the tune request was fulfilled, or
     * {@link RecordingCallback#onError(int)} otherwise.
     *
     * @param inputId The ID of the TV input for the given channel.
     * @param channelUri The URI of a channel.
     * @throws IllegalStateException If recording is already started.
     */
    public void tune(String inputId, Uri channelUri) {
        tune(inputId, channelUri, null);
    }

    /**
     * Tunes to a given channel for TV program recording. The first tune request will create a new
     * recording session for the corresponding TV input and establish a connection between the
     * application and the session. If recording has already started in the current recording
     * session, this method throws an exception. This can be used to provide domain-specific
     * features that are only known between certain client and their TV inputs.
     *
     * <p>The application may call this method before starting or after stopping recording, but not
     * during recording.
     *
     * <p>The recording session will respond by calling
     * {@link RecordingCallback#onTuned(Uri)} if the tune request was fulfilled, or
     * {@link RecordingCallback#onError(int)} otherwise.
     *
     * @param inputId The ID of the TV input for the given channel.
     * @param channelUri The URI of a channel.
     * @param params Domain-specific data for this tune request. Keys <em>must</em> be a scoped
     *            name, i.e. prefixed with a package name you own, so that different developers will
     *            not create conflicting keys.
     * @throws IllegalStateException If recording is already started.
     */
    public void tune(String inputId, Uri channelUri, Bundle params) {
        if (DEBUG) Log.d(TAG, "tune(" + channelUri + ")");
        if (TextUtils.isEmpty(inputId)) {
            throw new IllegalArgumentException("inputId cannot be null or an empty string");
        }
        if (mIsRecordingStarted && !mIsPaused) {
            throw new IllegalStateException("tune failed - recording already started");
        }
        if (mSessionCallback != null && TextUtils.equals(mSessionCallback.mInputId, inputId)) {
            if (mSession != null) {
                mSessionCallback.mChannelUri = channelUri;
                mSession.tune(channelUri, params);
            } else {
                mSessionCallback.mChannelUri = channelUri;
                mSessionCallback.mConnectionParams = params;
            }
            mIsTuned = false;
        } else {
            if (mIsPaused) {
                throw new IllegalStateException("tune failed - inputId is changed during pause");
            }
            resetInternal();
            mSessionCallback = new MySessionCallback(inputId, channelUri, params);
            if (mTvInputManager != null) {
                mTvInputManager.createRecordingSession(inputId, mSessionCallback, mHandler);
            }
        }
    }

    /**
     * Releases the resources in the current recording session immediately. This may be called at
     * any time, however if the session is already released, it does nothing.
     */
    public void release() {
        if (DEBUG) Log.d(TAG, "release()");
        resetInternal();
    }

    private void resetInternal() {
        mSessionCallback = null;
        mPendingAppPrivateCommands.clear();
        if (mSession != null) {
            mSession.release();
            mIsTuned = false;
            mIsRecordingStarted = false;
            mIsPaused = false;
            mIsRecordingStopping = false;
            mSession = null;
        }
    }

    /**
     * Starts TV program recording in the current recording session. Recording is expected to start
     * immediately when this method is called. If the current recording session has not yet tuned to
     * any channel, this method throws an exception.
     *
     * <p>The application may supply the URI for a TV program for filling in program specific data
     * fields in the {@link android.media.tv.TvContract.RecordedPrograms} table.
     * A non-null {@code programUri} implies the started recording should be of that specific
     * program, whereas null {@code programUri} does not impose such a requirement and the
     * recording can span across multiple TV programs. In either case, the application must call
     * {@link TvRecordingClient#stopRecording()} to stop the recording.
     *
     * <p>The recording session will respond by calling {@link RecordingCallback#onError(int)} if
     * the start request cannot be fulfilled.
     *
     * @param programUri The URI for the TV program to record, built by
     *            {@link TvContract#buildProgramUri(long)}. Can be {@code null}.
     * @throws IllegalStateException If {@link #tune} request hasn't been handled yet or during
     *            pause.
     */
    public void startRecording(@Nullable Uri programUri) {
        startRecording(programUri, Bundle.EMPTY);
    }

    /**
     * Starts TV program recording in the current recording session. Recording is expected to start
     * immediately when this method is called. If the current recording session has not yet tuned to
     * any channel, this method throws an exception.
     *
     * <p>The application may supply the URI for a TV program for filling in program specific data
     * fields in the {@link android.media.tv.TvContract.RecordedPrograms} table.
     * A non-null {@code programUri} implies the started recording should be of that specific
     * program, whereas null {@code programUri} does not impose such a requirement and the
     * recording can span across multiple TV programs. In either case, the application must call
     * {@link TvRecordingClient#stopRecording()} to stop the recording.
     *
     * <p>The recording session will respond by calling {@link RecordingCallback#onError(int)} if
     * the start request cannot be fulfilled.
     *
     * @param programUri The URI for the TV program to record, built by
     *            {@link TvContract#buildProgramUri(long)}. Can be {@code null}.
     * @param params Domain-specific data for this request. Keys <em>must</em> be a scoped
     *            name, i.e. prefixed with a package name you own, so that different developers will
     *            not create conflicting keys.
     * @throws IllegalStateException If {@link #tune} request hasn't been handled yet or during
     *            pause.
     */
    public void startRecording(@Nullable Uri programUri, @NonNull Bundle params) {
        if (mIsRecordingStopping || !mIsTuned || mIsPaused) {
            throw new IllegalStateException("startRecording failed -"
                    + "recording not yet stopped or not yet tuned or paused");
        }
        if (mIsRecordingStarted) {
            Log.w(TAG, "startRecording failed - recording already started");
        }
        if (mSession != null) {
            mSession.startRecording(programUri, params);
            mIsRecordingStarted = true;
        }
    }

    /**
     * Stops TV program recording in the current recording session. Recording is expected to stop
     * immediately when this method is called. If recording has not yet started in the current
     * recording session, this method does nothing.
     *
     * <p>The recording session is expected to create a new data entry in the
     * {@link android.media.tv.TvContract.RecordedPrograms} table that describes the newly
     * recorded program and pass the URI to that entry through to
     * {@link RecordingCallback#onRecordingStopped(Uri)}.
     * If the stop request cannot be fulfilled, the recording session will respond by calling
     * {@link RecordingCallback#onError(int)}.
     */
    public void stopRecording() {
        if (!mIsRecordingStarted) {
            Log.w(TAG, "stopRecording failed - recording not yet started");
        }
        if (mSession != null) {
            mSession.stopRecording();
            if (mIsRecordingStarted) {
                mIsRecordingStopping = true;
            }
        }
    }

    /**
     * Pause TV program recording in the current recording session. Recording is expected to pause
     * immediately when this method is called. If recording has not yet started in the current
     * recording session, this method does nothing.
     *
     * <p>In pause status, the application can tune during recording. To continue recording,
     * please call {@link TvRecordingClient#resumeRecording()} to resume instead of
     * {@link TvRecordingClient#startRecording(Uri)}. Application can stop
     * the recording with {@link TvRecordingClient#stopRecording()} in recording pause status.
     *
     * <p>If the pause request cannot be fulfilled, the recording session will respond by calling
     * {@link RecordingCallback#onError(int)}.
     */
    public void pauseRecording() {
        pauseRecording(Bundle.EMPTY);
    }

    /**
     * Pause TV program recording in the current recording session. Recording is expected to pause
     * immediately when this method is called. If recording has not yet started in the current
     * recording session, this method does nothing.
     *
     * <p>In pause status, the application can tune during recording. To continue recording,
     * please call {@link TvRecordingClient#resumeRecording()} to resume instead of
     * {@link TvRecordingClient#startRecording(Uri)}. Application can stop
     * the recording with {@link TvRecordingClient#stopRecording()} in recording pause status.
     *
     * <p>If the pause request cannot be fulfilled, the recording session will respond by calling
     * {@link RecordingCallback#onError(int)}.
     *
     * @param params Domain-specific data for this request.
     */
    public void pauseRecording(@NonNull Bundle params) {
        if (!mIsRecordingStarted || mIsRecordingStopping) {
            throw new IllegalStateException(
                    "pauseRecording failed - recording not yet started or stopping");
        }
        TvInputInfo info = mTvInputManager.getTvInputInfo(mSessionCallback.mInputId);
        if (info == null || !info.canPauseRecording()) {
            throw new UnsupportedOperationException(
                    "pauseRecording failed - operation not supported");
        }
        if (mIsPaused) {
            Log.w(TAG, "pauseRecording failed - recording already paused");
        }
        if (mSession != null) {
            mSession.pauseRecording(params);
            mIsPaused  = true;
        }
    }

    /**
     * Resume TV program recording only in recording pause status in the current recording session.
     * Recording is expected to resume immediately when this method is called. If recording has not
     * yet paused in the current recording session, this method does nothing.
     *
     * <p>When record is resumed, the recording is continue and can not re-tune. Application can
     * stop the recording with {@link TvRecordingClient#stopRecording()} after record resumed.
     *
     * <p>If the pause request cannot be fulfilled, the recording session will respond by calling
     * {@link RecordingCallback#onError(int)}.
     */
    public void resumeRecording() {
        resumeRecording(Bundle.EMPTY);
    }

    /**
     * Resume TV program recording only in recording pause status in the current recording session.
     * Recording is expected to resume immediately when this method is called. If recording has not
     * yet paused in the current recording session, this method does nothing.
     *
     * <p>When record is resumed, the recording is continues and can not re-tune. Application can
     * stop the recording with {@link TvRecordingClient#stopRecording()} after record resumed.
     *
     * <p>If the resume request cannot be fulfilled, the recording session will respond by calling
     * {@link RecordingCallback#onError(int)}.
     *
     * @param params Domain-specific data for this request.
     */
    public void resumeRecording(@NonNull Bundle params) {
        if (!mIsRecordingStarted || mIsRecordingStopping || !mIsTuned) {
            throw new IllegalStateException(
                    "resumeRecording failed - recording not yet started or stopping or "
                            + "not yet tuned");
        }
        if (!mIsPaused) {
            Log.w(TAG, "resumeRecording failed - recording not yet paused");
        }
        if (mSession != null) {
            mSession.resumeRecording(params);
            mIsPaused  = false;
        }
    }

    /**
     * Sends a private command to the underlying TV input. This can be used to provide
     * domain-specific features that are only known between certain clients and their TV inputs.
     *
     * @param action The name of the private command to send. This <em>must</em> be a scoped name,
     *            i.e. prefixed with a package name you own, so that different developers will not
     *            create conflicting commands.
     * @param data An optional bundle to send with the command.
     */
    public void sendAppPrivateCommand(@NonNull String action, Bundle data) {
        if (TextUtils.isEmpty(action)) {
            throw new IllegalArgumentException("action cannot be null or an empty string");
        }
        if (mSession != null) {
            mSession.sendAppPrivateCommand(action, data);
        } else {
            Log.w(TAG, "sendAppPrivateCommand - session not yet created (action \"" + action
                    + "\" pending)");
            mPendingAppPrivateCommands.add(Pair.create(action, data));
        }
    }

    // For testing purposes only.
    /** @hide */
    public TvInputManager.SessionCallback getSessionCallback() {
        return mSessionCallback;
    }

    /**
     * Callback used to receive various status updates on the
     * {@link android.media.tv.TvInputService.RecordingSession}
     */
    public abstract static class RecordingCallback {
        /**
         * This is called when an error occurred while establishing a connection to the recording
         * session for the corresponding TV input.
         *
         * @param inputId The ID of the TV input bound to the current TvRecordingClient.
         */
        public void onConnectionFailed(String inputId) {
        }

        /**
         * This is called when the connection to the current recording session is lost.
         *
         * @param inputId The ID of the TV input bound to the current TvRecordingClient.
         */
        public void onDisconnected(String inputId) {
        }

        /**
         * This is called when the recording session has been tuned to the given channel and is
         * ready to start recording.
         *
         * @param channelUri The URI of a channel.
         */
        public void onTuned(Uri channelUri) {
        }

        /**
         * This is called when the current recording session has stopped recording and created a
         * new data entry in the {@link TvContract.RecordedPrograms} table that describes the newly
         * recorded program.
         *
         * @param recordedProgramUri The URI for the newly recorded program.
         */
        public void onRecordingStopped(Uri recordedProgramUri) {
        }

        /**
         * This is called when an issue has occurred. It may be called at any time after the current
         * recording session is created until it is released.
         *
         * @param error The error code. Should be one of the followings.
         * <ul>
         * <li>{@link TvInputManager#RECORDING_ERROR_UNKNOWN}
         * <li>{@link TvInputManager#RECORDING_ERROR_INSUFFICIENT_SPACE}
         * <li>{@link TvInputManager#RECORDING_ERROR_RESOURCE_BUSY}
         * </ul>
         */
        public void onError(@TvInputManager.RecordingError int error) {
        }

        /**
         * This is invoked when a custom event from the bound TV input is sent to this client.
         *
         * @param inputId The ID of the TV input bound to this client.
         * @param eventType The type of the event.
         * @param eventArgs Optional arguments of the event.
         * @hide
         */
        @SystemApi
        public void onEvent(String inputId, String eventType, Bundle eventArgs) {
        }
    }

    private class MySessionCallback extends TvInputManager.SessionCallback {
        final String mInputId;
        Uri mChannelUri;
        Bundle mConnectionParams;

        MySessionCallback(String inputId, Uri channelUri, Bundle connectionParams) {
            mInputId = inputId;
            mChannelUri = channelUri;
            mConnectionParams = connectionParams;
        }

        @Override
        public void onSessionCreated(TvInputManager.Session session) {
            if (DEBUG) {
                Log.d(TAG, "onSessionCreated()");
            }
            if (this != mSessionCallback) {
                Log.w(TAG, "onSessionCreated - session already created");
                // This callback is obsolete.
                if (session != null) {
                    session.release();
                }
                return;
            }
            mSession = session;
            if (session != null) {
                // Sends the pending app private commands.
                for (Pair<String, Bundle> command : mPendingAppPrivateCommands) {
                    mSession.sendAppPrivateCommand(command.first, command.second);
                }
                mPendingAppPrivateCommands.clear();
                mSession.tune(mChannelUri, mConnectionParams);
            } else {
                mSessionCallback = null;
                if (mCallback != null) {
                    mCallback.onConnectionFailed(mInputId);
                }
                if (mTvIAppView != null) {
                    mTvIAppView.notifyRecordingConnectionFailed(mRecordingId, mInputId);
                }
            }
        }

        @Override
        public void onTuned(TvInputManager.Session session, Uri channelUri) {
            if (DEBUG) {
                Log.d(TAG, "onTuned()");
            }
            if (this != mSessionCallback) {
                Log.w(TAG, "onTuned - session not created");
                return;
            }
            if (mIsTuned || !Objects.equals(mChannelUri, channelUri)) {
                Log.w(TAG, "onTuned - already tuned or not yet tuned to last channel");
                return;
            }
            mIsTuned = true;
            if (mCallback != null) {
                mCallback.onTuned(channelUri);
            }
            if (mTvIAppView != null) {
                mTvIAppView.notifyRecordingTuned(mRecordingId, channelUri);
            }
        }

        @Override
        public void onSessionReleased(TvInputManager.Session session) {
            if (DEBUG) {
                Log.d(TAG, "onSessionReleased()");
            }
            if (this != mSessionCallback) {
                Log.w(TAG, "onSessionReleased - session not created");
                return;
            }
            mIsTuned = false;
            mIsRecordingStarted = false;
            mIsPaused = false;
            mIsRecordingStopping = false;
            mSessionCallback = null;
            mSession = null;
            if (mCallback != null) {
                mCallback.onDisconnected(mInputId);
            }
            if (mTvIAppView != null) {
                mTvIAppView.notifyRecordingDisconnected(mRecordingId, mInputId);
            }
        }

        @Override
        public void onRecordingStopped(TvInputManager.Session session, Uri recordedProgramUri) {
            if (DEBUG) {
                Log.d(TAG, "onRecordingStopped(recordedProgramUri= " + recordedProgramUri + ")");
            }
            if (this != mSessionCallback) {
                Log.w(TAG, "onRecordingStopped - session not created");
                return;
            }
            if (!mIsRecordingStarted) {
                Log.w(TAG, "onRecordingStopped - recording not yet started");
                return;
            }
            mIsRecordingStarted = false;
            mIsPaused = false;
            mIsRecordingStopping = false;
            if (mCallback != null) {
                mCallback.onRecordingStopped(recordedProgramUri);
            }
            if (mTvIAppView != null) {
                mTvIAppView.notifyRecordingStopped(mRecordingId);
            }
        }

        @Override
        public void onError(TvInputManager.Session session, int error) {
            if (DEBUG) {
                Log.d(TAG, "onError(error=" + error + ")");
            }
            if (this != mSessionCallback) {
                Log.w(TAG, "onError - session not created");
                return;
            }
            if (mCallback != null) {
                mCallback.onError(error);
            }
            if (mTvIAppView != null) {
                mTvIAppView.notifyRecordingError(mRecordingId, error);
            }
        }

        @Override
        public void onSessionEvent(TvInputManager.Session session, String eventType,
                Bundle eventArgs) {
            if (DEBUG) {
                Log.d(TAG, "onSessionEvent(eventType=" + eventType + ", eventArgs=" + eventArgs
                        + ")");
            }
            if (this != mSessionCallback) {
                Log.w(TAG, "onSessionEvent - session not created");
                return;
            }
            if (mCallback != null) {
                mCallback.onEvent(mInputId, eventType, eventArgs);
            }
        }
    }
}
