/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.speech;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.TestApi;
import android.app.AppOpsManager;
import android.app.Service;
import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextParams;
import android.content.Intent;
import android.content.PermissionChecker;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.function.pooled.PooledLambda;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class provides a base class for recognition service implementations. This class should be
 * extended only in case you wish to implement a new speech recognizer. Please note that the
 * implementation of this service is stateless.
 */
public abstract class RecognitionService extends Service {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    @SdkConstant(SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE = "android.speech.RecognitionService";

    /**
     * Name under which a RecognitionService component publishes information about itself.
     * This meta-data should reference an XML resource containing a
     * <code>&lt;{@link android.R.styleable#RecognitionService recognition-service}&gt;</code> or
     * <code>&lt;{@link android.R.styleable#RecognitionService on-device-recognition-service}
     * &gt;</code> tag.
     */
    public static final String SERVICE_META_DATA = "android.speech";

    /** Log messages identifier */
    private static final String TAG = "RecognitionService";

    /** Debugging flag */
    private static final boolean DBG = false;

    private static final int DEFAULT_MAX_CONCURRENT_SESSIONS_COUNT = 1;

    private final Map<IBinder, SessionState> mSessions = new HashMap<>();

    /** Binder of the recognition service */
    private final RecognitionServiceBinder mBinder = new RecognitionServiceBinder(this);

    private static final int MSG_START_LISTENING = 1;

    private static final int MSG_STOP_LISTENING = 2;

    private static final int MSG_CANCEL = 3;

    private static final int MSG_RESET = 4;

    private static final int MSG_CHECK_RECOGNITION_SUPPORT = 5;

    private static final int MSG_TRIGGER_MODEL_DOWNLOAD = 6;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_LISTENING:
                    StartListeningArgs args = (StartListeningArgs) msg.obj;
                    dispatchStartListening(args.mIntent, args.mListener, args.mAttributionSource);
                    break;
                case MSG_STOP_LISTENING:
                    dispatchStopListening((IRecognitionListener) msg.obj);
                    break;
                case MSG_CANCEL:
                    dispatchCancel((IRecognitionListener) msg.obj);
                    break;
                case MSG_RESET:
                    dispatchClearCallback((IRecognitionListener) msg.obj);
                    break;
                case MSG_CHECK_RECOGNITION_SUPPORT:
                    CheckRecognitionSupportArgs checkArgs = (CheckRecognitionSupportArgs) msg.obj;
                    dispatchCheckRecognitionSupport(
                            checkArgs.mIntent, checkArgs.callback, checkArgs.mAttributionSource);
                    break;
                case MSG_TRIGGER_MODEL_DOWNLOAD:
                    ModelDownloadArgs modelDownloadArgs = (ModelDownloadArgs) msg.obj;
                    dispatchTriggerModelDownload(
                            modelDownloadArgs.mIntent,
                            modelDownloadArgs.mAttributionSource,
                            modelDownloadArgs.mListener);
                    break;
            }
        }
    };

    private void dispatchStartListening(Intent intent, final IRecognitionListener listener,
            @NonNull AttributionSource attributionSource) {
        Callback currentCallback = null;
        SessionState sessionState = mSessions.get(listener.asBinder());

        try {
            if (sessionState == null) {
                if (mSessions.size() >= getMaxConcurrentSessionsCount()) {
                    listener.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY);
                    Log.i(TAG, "#startListening received "
                            + "when the service's capacity is full - ignoring this call.");
                    return;
                }

                boolean preflightPermissionCheckPassed =
                        intent.hasExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE)
                                || checkPermissionForPreflightNotHardDenied(attributionSource);
                if (preflightPermissionCheckPassed) {
                    currentCallback = new Callback(listener, attributionSource);
                    sessionState = new SessionState(currentCallback);
                    mSessions.put(listener.asBinder(), sessionState);
                    if (DBG) {
                        Log.d(TAG, "Added a new session to the map, pending permission checks");
                    }
                    RecognitionService.this.onStartListening(intent, currentCallback);
                }

                if (!preflightPermissionCheckPassed
                        || !checkPermissionAndStartDataDelivery(sessionState)) {
                    listener.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
                    if (preflightPermissionCheckPassed) {
                        // If start listening was attempted, cancel the callback.
                        RecognitionService.this.onCancel(currentCallback);
                        mSessions.remove(listener.asBinder());
                        finishDataDelivery(sessionState);
                        sessionState.reset();
                    }
                    Log.i(TAG, "#startListening received from a caller "
                            + "without permission " + Manifest.permission.RECORD_AUDIO + ".");
                }
            } else {
                listener.onError(SpeechRecognizer.ERROR_CLIENT);
                Log.i(TAG, "#startListening received "
                        + "for a listener which is already in session - ignoring this call.");
            }
        } catch (RemoteException e) {
            Log.d(TAG, "#onError call from #startListening failed.");
        }
    }

    private void dispatchStopListening(IRecognitionListener listener) {
        SessionState sessionState = mSessions.get(listener.asBinder());
        if (sessionState == null) {
            try {
                listener.onError(SpeechRecognizer.ERROR_CLIENT);
            } catch (RemoteException e) {
                Log.d(TAG, "#onError call from #stopListening failed.");
            }
            Log.w(TAG, "#stopListening received for a listener "
                    + "which has not started a session - ignoring this call.");
        } else {
            RecognitionService.this.onStopListening(sessionState.mCallback);
        }
    }

    private void dispatchCancel(IRecognitionListener listener) {
        SessionState sessionState = mSessions.get(listener.asBinder());
        if (sessionState == null) {
            Log.w(TAG, "#cancel received for a listener which has not started a session "
                    + "- ignoring this call.");
        } else {
            RecognitionService.this.onCancel(sessionState.mCallback);
            dispatchClearCallback(listener);
        }
    }

    private void dispatchClearCallback(IRecognitionListener listener) {
        SessionState sessionState = mSessions.remove(listener.asBinder());
        if (sessionState != null) {
            if (DBG) {
                Log.d(TAG, "Removed session from the map for listener = "
                        + listener.asBinder() + ".");
            }
            finishDataDelivery(sessionState);
            sessionState.reset();
        }
    }

    private void dispatchCheckRecognitionSupport(
            Intent intent, IRecognitionSupportCallback callback,
            AttributionSource attributionSource) {
        RecognitionService.this.onCheckRecognitionSupport(
                intent,
                attributionSource,
                new SupportCallback(callback));
    }

    private void dispatchTriggerModelDownload(
            Intent intent,
            AttributionSource attributionSource,
            IModelDownloadListener listener) {
        if (listener == null) {
            RecognitionService.this.onTriggerModelDownload(intent, attributionSource);
        } else {
            RecognitionService.this.onTriggerModelDownload(
                    intent,
                    attributionSource,
                    new ModelDownloadListener() {

                        private final Object mLock = new Object();

                        @GuardedBy("mLock")
                        private boolean mIsTerminated = false;

                        @Override
                        public void onProgress(int completedPercent) {
                            synchronized (mLock) {
                                if (mIsTerminated) {
                                    return;
                                }
                                try {
                                    listener.onProgress(completedPercent);
                                } catch (RemoteException e) {
                                    throw e.rethrowFromSystemServer();
                                }
                            }
                        }

                        @Override
                        public void onSuccess() {
                            synchronized (mLock) {
                                if (mIsTerminated) {
                                    return;
                                }
                                mIsTerminated = true;
                                try {
                                    listener.onSuccess();
                                } catch (RemoteException e) {
                                    throw e.rethrowFromSystemServer();
                                }
                            }
                        }

                        @Override
                        public void onScheduled() {
                            synchronized (mLock) {
                                if (mIsTerminated) {
                                    return;
                                }
                                mIsTerminated = true;
                                try {
                                    listener.onScheduled();
                                } catch (RemoteException e) {
                                    throw e.rethrowFromSystemServer();
                                }
                            }
                        }

                        @Override
                        public void onError(int error) {
                            synchronized (mLock) {
                                if (mIsTerminated) {
                                    return;
                                }
                                mIsTerminated = true;
                                try {
                                    listener.onError(error);
                                } catch (RemoteException e) {
                                    throw e.rethrowFromSystemServer();
                                }
                            }
                        }
                    });
        }
    }

    private static class StartListeningArgs {
        public final Intent mIntent;

        public final IRecognitionListener mListener;
        @NonNull public final AttributionSource mAttributionSource;

        public StartListeningArgs(Intent intent, IRecognitionListener listener,
                @NonNull AttributionSource attributionSource) {
            this.mIntent = intent;
            this.mListener = listener;
            this.mAttributionSource = attributionSource;
        }
    }

    private static class CheckRecognitionSupportArgs {
        public final Intent mIntent;
        public final IRecognitionSupportCallback callback;
        public final AttributionSource mAttributionSource;

        private CheckRecognitionSupportArgs(
                Intent intent,
                IRecognitionSupportCallback callback,
                AttributionSource attributionSource) {
            this.mIntent = intent;
            this.callback = callback;
            this.mAttributionSource = attributionSource;
        }
    }

    private static class ModelDownloadArgs {
        final Intent mIntent;
        final AttributionSource mAttributionSource;
        @Nullable final IModelDownloadListener mListener;

        private ModelDownloadArgs(
                Intent intent,
                AttributionSource attributionSource,
                @Nullable IModelDownloadListener listener) {
            this.mIntent = intent;
            this.mAttributionSource = attributionSource;
            this.mListener = listener;
        }
    }

    /**
     * Notifies the service that it should start listening for speech.
     *
     * <p> If you are recognizing speech from the microphone, in this callback you
     * should create an attribution context for the caller such that when you access
     * the mic the caller would be properly blamed (and their permission checked in
     * the process) for accessing the microphone and that you served as a proxy for
     * this sensitive data (and your permissions would be checked in the process).
     * You should also open the mic in this callback via the attribution context
     * and close the mic before returning the recognized result. If you don't do
     * that then the caller would be blamed and you as being a proxy as well as you
     * would get one more blame on yourself when you open the microphone.
     *
     * <pre>
     * Context attributionContext = context.createContext(new ContextParams.Builder()
     *     .setNextAttributionSource(callback.getCallingAttributionSource())
     *     .build());
     *
     * AudioRecord recorder = AudioRecord.Builder()
     *     .setContext(attributionContext);
     *     . . .
     *    .build();
     *
     * recorder.startRecording()
     * </pre>
     *
     * @param recognizerIntent contains parameters for the recognition to be performed. The intent
     *        may also contain optional extras, see {@link RecognizerIntent}. If these values are
     *        not set explicitly, default values should be used by the recognizer.
     * @param listener that will receive the service's callbacks
     */
    protected abstract void onStartListening(Intent recognizerIntent, Callback listener);

    /**
     * Notifies the service that it should cancel the speech recognition.
     */
    protected abstract void onCancel(Callback listener);

    /**
     * Notifies the service that it should stop listening for speech. Speech captured so far should
     * be recognized as if the user had stopped speaking at this point. This method is only called
     * if the application calls it explicitly.
     */
    protected abstract void onStopListening(Callback listener);

    /**
     * Queries the service on whether it would support a {@link #onStartListening(Intent, Callback)}
     * for the same {@code recognizerIntent}.
     *
     * <p>The service will notify the caller about the level of support or error via
     * {@link SupportCallback}.
     *
     * <p>If the service does not offer the support check it will notify the caller with
     * {@link SpeechRecognizer#ERROR_CANNOT_CHECK_SUPPORT}.
     */
    public void onCheckRecognitionSupport(
            @NonNull Intent recognizerIntent,
            @NonNull SupportCallback supportCallback) {
        if (DBG) {
            Log.i(TAG, String.format("#onSupports [%s]", recognizerIntent));
        }
        supportCallback.onError(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT);
    }

    /**
     * Queries the service on whether it would support a {@link #onStartListening(Intent, Callback)}
     * for the same {@code recognizerIntent}.
     *
     * <p>The service will notify the caller about the level of support or error via
     * {@link SupportCallback}.
     *
     * <p>If the service does not offer the support check it will notify the caller with
     * {@link SpeechRecognizer#ERROR_CANNOT_CHECK_SUPPORT}.
     *
     * <p>Provides the calling AttributionSource to the service implementation so that permissions
     * and bandwidth could be correctly blamed.</p>
     */
    public void onCheckRecognitionSupport(
            @NonNull Intent recognizerIntent,
            @NonNull AttributionSource attributionSource,
            @NonNull SupportCallback supportCallback) {
        onCheckRecognitionSupport(recognizerIntent, supportCallback);
    }

    /**
     * Requests the download of the recognizer support for {@code recognizerIntent}.
     */
    public void onTriggerModelDownload(@NonNull Intent recognizerIntent) {
        if (DBG) {
            Log.i(TAG, String.format("#downloadModel [%s]", recognizerIntent));
        }
    }

    /**
     * Requests the download of the recognizer support for {@code recognizerIntent}.
     *
     * <p>Provides the calling AttributionSource to the service implementation so that permissions
     * and bandwidth could be correctly blamed.</p>
     */
    public void onTriggerModelDownload(
            @NonNull Intent recognizerIntent,
            @NonNull AttributionSource attributionSource) {
        onTriggerModelDownload(recognizerIntent);
    }

    /**
     * Requests the download of the recognizer support for {@code recognizerIntent}.
     *
     * <p> Provides the calling {@link AttributionSource} to the service implementation so that
     * permissions and bandwidth could be correctly blamed.
     *
     * <p> Client will receive the progress updates via the given {@link ModelDownloadListener}:
     *
     * <li> If the model is already available, {@link ModelDownloadListener#onSuccess()} will be
     * called directly. The model can be safely used afterwards.
     *
     * <li> If the {@link RecognitionService} has started the download,
     * {@link ModelDownloadListener#onProgress(int)} will be called an unspecified (zero or more)
     * number of times until the download is complete.
     * When the download finishes, {@link ModelDownloadListener#onSuccess()} will be called.
     * The model can be safely used afterwards.
     *
     * <li> If the {@link RecognitionService} has only scheduled the download, but won't satisfy it
     * immediately, {@link ModelDownloadListener#onScheduled()} will be called.
     * There will be no further updates on this listener.
     *
     * <li> If the request fails at any time due to a network or scheduling error,
     * {@link ModelDownloadListener#onError(int)} will be called.
     *
     * @param recognizerIntent contains parameters for the recognition to be performed. The intent
     *        may also contain optional extras, see {@link RecognizerIntent}.
     * @param attributionSource the attribution source of the caller.
     * @param listener on which to receive updates about the model download request.
     */
    public void onTriggerModelDownload(
            @NonNull Intent recognizerIntent,
            @NonNull AttributionSource attributionSource,
            @NonNull ModelDownloadListener listener) {
        listener.onError(SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS);
    }

    @Override
    @SuppressLint("MissingNullability")
    public Context createContext(@NonNull ContextParams contextParams) {
        if (contextParams.getNextAttributionSource() != null) {
            if (mHandler.getLooper().equals(Looper.myLooper())) {
                handleAttributionContextCreation(contextParams.getNextAttributionSource());
            } else {
                mHandler.sendMessage(
                        PooledLambda.obtainMessage(this::handleAttributionContextCreation,
                                contextParams.getNextAttributionSource()));
            }
        }
        return super.createContext(contextParams);
    }

    private void handleAttributionContextCreation(@NonNull AttributionSource attributionSource) {
        for (SessionState sessionState : mSessions.values()) {
            Callback currentCallback = sessionState.mCallback;
            if (currentCallback != null
                    && currentCallback.mCallingAttributionSource.equals(attributionSource)) {
                currentCallback.mAttributionContextCreated = true;
            }
        }
    }

    @Override
    public final IBinder onBind(final Intent intent) {
        if (DBG) Log.d(TAG, "#onBind, intent=" + intent);
        onBindInternal();
        return mBinder;
    }

    /** @hide */
    @SuppressLint("UnflaggedApi") // @TestApi without associated feature.
    @TestApi
    public void onBindInternal() { }

    @Override
    public void onDestroy() {
        if (DBG) Log.d(TAG, "#onDestroy");
        for (SessionState sessionState : mSessions.values()) {
            finishDataDelivery(sessionState);
            sessionState.reset();
        }
        mSessions.clear();
        mBinder.clearReference();
        super.onDestroy();
    }

    /**
     * Returns the maximal number of recognition sessions ongoing at the same time.
     * <p>
     * The default value is 1, meaning concurrency should be enabled by overriding this method.
     */
    public int getMaxConcurrentSessionsCount() {
        return DEFAULT_MAX_CONCURRENT_SESSIONS_COUNT;
    }

    /**
     * This class receives callbacks from the speech recognition service and forwards them to the
     * user. An instance of this class is passed to the
     * {@link RecognitionService#onStartListening(Intent, Callback)} method. Recognizers may call
     * these methods on any thread.
     */
    public class Callback {
        private final IRecognitionListener mListener;
        @NonNull private final AttributionSource mCallingAttributionSource;
        @Nullable private Context mAttributionContext;
        private boolean mAttributionContextCreated;

        private Callback(IRecognitionListener listener,
                @NonNull AttributionSource attributionSource) {
            mListener = listener;
            mCallingAttributionSource = attributionSource;
        }

        /**
         * The service should call this method when the user has started to speak.
         */
        public void beginningOfSpeech() throws RemoteException {
            mListener.onBeginningOfSpeech();
        }

        /**
         * The service should call this method when sound has been received. The purpose of this
         * function is to allow giving feedback to the user regarding the captured audio.
         *
         * @param buffer a buffer containing a sequence of big-endian 16-bit integers representing a
         *        single channel audio stream. The sample rate is implementation dependent.
         */
        public void bufferReceived(byte[] buffer) throws RemoteException {
            mListener.onBufferReceived(buffer);
        }

        /**
         * The service should call this method after the user stops speaking.
         */
        public void endOfSpeech() throws RemoteException {
            mListener.onEndOfSpeech();
        }

        /**
         * The service should call this method when a network or recognition error occurred.
         *
         * @param error code is defined in {@link SpeechRecognizer}
         */
        public void error(@SpeechRecognizer.RecognitionError int error) throws RemoteException {
            Message.obtain(mHandler, MSG_RESET, mListener).sendToTarget();
            mListener.onError(error);
        }

        /**
         * The service should call this method when partial recognition results are available. This
         * method can be called at any time between {@link #beginningOfSpeech()} and
         * {@link #results(Bundle)} when partial results are ready. This method may be called zero,
         * one or multiple times for each call to {@link SpeechRecognizer#startListening(Intent)},
         * depending on the speech recognition service implementation.
         *
         * @param partialResults the returned results. To retrieve the results in
         *        ArrayList&lt;String&gt; format use {@link Bundle#getStringArrayList(String)} with
         *        {@link SpeechRecognizer#RESULTS_RECOGNITION} as a parameter
         */
        public void partialResults(Bundle partialResults) throws RemoteException {
            mListener.onPartialResults(partialResults);
        }

        /**
         * The service should call this method when the endpointer is ready for the user to start
         * speaking.
         *
         * @param params parameters set by the recognition service. Reserved for future use.
         */
        public void readyForSpeech(Bundle params) throws RemoteException {
            mListener.onReadyForSpeech(params);
        }

        /**
         * The service should call this method when recognition results are ready.
         *
         * @param results the recognition results. To retrieve the results in {@code
         *        ArrayList<String>} format use {@link Bundle#getStringArrayList(String)} with
         *        {@link SpeechRecognizer#RESULTS_RECOGNITION} as a parameter
         */
        public void results(Bundle results) throws RemoteException {
            Message.obtain(mHandler, MSG_RESET, mListener).sendToTarget();
            mListener.onResults(results);
        }

        /**
         * The service should call this method when the sound level in the audio stream has changed.
         * There is no guarantee that this method will be called.
         *
         * @param rmsdB the new RMS dB value
         */
        public void rmsChanged(float rmsdB) throws RemoteException {
            mListener.onRmsChanged(rmsdB);
        }

        /**
         * The service should call this method for each ready segment of a long recognition session.
         *
         * @param results the recognition results. To retrieve the results in {@code
         *        ArrayList<String>} format use {@link Bundle#getStringArrayList(String)} with
         *        {@link SpeechRecognizer#RESULTS_RECOGNITION} as a parameter
         */
        @SuppressLint({"CallbackMethodName", "RethrowRemoteException"})
        public void segmentResults(@NonNull Bundle results) throws RemoteException {
            mListener.onSegmentResults(results);
        }

        /**
         * The service should call this method to end a segmented session.
         */
        @SuppressLint({"CallbackMethodName", "RethrowRemoteException"})
        public void endOfSegmentedSession() throws RemoteException {
            Message.obtain(mHandler, MSG_RESET, mListener).sendToTarget();
            mListener.onEndOfSegmentedSession();
        }

        /**
         * The service should call this method when the language detection (and switching)
         * results are available. This method can be called on any number of occasions
         * at any time between {@link #beginningOfSpeech()} and {@link #endOfSpeech()},
         * depending on the speech recognition service implementation.
         *
         * @param results the returned language detection (and switching) results.
         *        <p> To retrieve the most confidently detected language IETF tag
         *        (as defined by BCP 47, e.g., "en-US", "de-DE"),
         *        use {@link Bundle#getString(String)}
         *        with {@link SpeechRecognizer#DETECTED_LANGUAGE} as the parameter.
         *        <p> To retrieve the language detection confidence level represented by a value
         *        prefixed by {@code LANGUAGE_DETECTION_CONFIDENCE_LEVEL_} defined in
         *        {@link SpeechRecognizer}, use {@link Bundle#getInt(String)} with
         *        {@link SpeechRecognizer#LANGUAGE_DETECTION_CONFIDENCE_LEVEL} as the parameter.
         *        <p> To retrieve the alternative locales for the same language
         *        retrieved by the key {@link SpeechRecognizer#DETECTED_LANGUAGE},
         *        use {@link Bundle#getStringArrayList(String)}
         *        with {@link SpeechRecognizer#TOP_LOCALE_ALTERNATIVES} as the parameter.
         *        <p> To retrieve the language switching results represented by a value
         *        prefixed by {@code LANGUAGE_SWITCH_RESULT_}
         *        and defined in {@link SpeechRecognizer}, use {@link Bundle#getInt(String)}
         *        with {@link SpeechRecognizer#LANGUAGE_SWITCH_RESULT} as the parameter.
         */
        @SuppressLint("CallbackMethodName") // For consistency with existing methods.
        public void languageDetection(@NonNull Bundle results) {
            try {
                mListener.onLanguageDetection(results);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        /**
         * Return the Linux uid assigned to the process that sent you the current transaction that
         * is being processed. This is obtained from {@link Binder#getCallingUid()}.
         */
        public int getCallingUid() {
            return mCallingAttributionSource.getUid();
        }

        /**
         * Gets the permission identity of the calling app. If you want to attribute
         * the mic access to the calling app you can create an attribution context
         * via {@link android.content.Context#createContext(android.content.ContextParams)}
         * and passing this identity to {@link
         * android.content.ContextParams.Builder#setNextAttributionSource(AttributionSource)}.
         *
         * @return The permission identity of the calling app.
         *
         * @see android.content.ContextParams.Builder#setNextAttributionSource(
         * AttributionSource)
         */
        @SuppressLint("CallbackMethodName")
        @NonNull
        public AttributionSource getCallingAttributionSource() {
            return mCallingAttributionSource;
        }

        @NonNull Context getAttributionContextForCaller() {
            if (mAttributionContext == null) {
                mAttributionContext = createContext(new ContextParams.Builder()
                        .setNextAttributionSource(mCallingAttributionSource)
                        .build());
            }
            return mAttributionContext;
        }
    }

    /**
     * This class receives callbacks from the speech recognition service and forwards them to the
     * user. An instance of this class is passed to the
     * {@link RecognitionService#onCheckRecognitionSupport(Intent, SupportCallback)} method. Recognizers may call
     * these methods on any thread.
     */
    public static class SupportCallback {
        private final IRecognitionSupportCallback mCallback;

        private SupportCallback(
                IRecognitionSupportCallback callback) {
            this.mCallback = callback;
        }

        /** The service should call this method to notify the caller about the level of support. */
        public void onSupportResult(@NonNull RecognitionSupport recognitionSupport) {
            try {
                mCallback.onSupportResult(recognitionSupport);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        /**
         * The service should call this method when an error occurred and can't satisfy the support
         * request.
         *
         * @param errorCode code is defined in {@link SpeechRecognizer}
         */
        public void onError(@SpeechRecognizer.RecognitionError int errorCode) {
            try {
                mCallback.onError(errorCode);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /** Binder of the recognition service. */
    private static final class RecognitionServiceBinder extends IRecognitionService.Stub {
        private final WeakReference<RecognitionService> mServiceRef;

        public RecognitionServiceBinder(RecognitionService service) {
            mServiceRef = new WeakReference<>(service);
        }

        @Override
        public void startListening(Intent recognizerIntent, IRecognitionListener listener,
                @NonNull AttributionSource attributionSource) {
            Objects.requireNonNull(attributionSource);
            attributionSource.enforceCallingUid();
            if (DBG) Log.d(TAG, "startListening called by:" + listener.asBinder());
            final RecognitionService service = mServiceRef.get();
            if (service != null) {
                service.mHandler.sendMessage(Message.obtain(service.mHandler,
                        MSG_START_LISTENING, new StartListeningArgs(
                                recognizerIntent, listener, attributionSource)));
            }
        }

        @Override
        public void stopListening(IRecognitionListener listener) {
            if (DBG) Log.d(TAG, "stopListening called by:" + listener.asBinder());
            final RecognitionService service = mServiceRef.get();
            if (service != null) {
                service.mHandler.sendMessage(
                        Message.obtain(service.mHandler, MSG_STOP_LISTENING, listener));
            }
        }

        @Override
        public void cancel(IRecognitionListener listener, boolean isShutdown) {
            if (DBG) Log.d(TAG, "cancel called by:" + listener.asBinder());
            final RecognitionService service = mServiceRef.get();
            if (service != null) {
                service.mHandler.sendMessage(
                        Message.obtain(service.mHandler, MSG_CANCEL, listener));
            }
        }

        @Override
        public void checkRecognitionSupport(
                Intent recognizerIntent,
                @NonNull AttributionSource attributionSource,
                IRecognitionSupportCallback callback) {
            final RecognitionService service = mServiceRef.get();
            if (service != null) {
                service.mHandler.sendMessage(
                        Message.obtain(service.mHandler, MSG_CHECK_RECOGNITION_SUPPORT,
                                new CheckRecognitionSupportArgs(
                                        recognizerIntent, callback, attributionSource)));
            }
        }

        @Override
        public void triggerModelDownload(
                Intent recognizerIntent,
                @NonNull AttributionSource attributionSource,
                IModelDownloadListener listener) {
            final RecognitionService service = mServiceRef.get();
            if (service != null) {
                service.mHandler.sendMessage(
                        Message.obtain(
                                service.mHandler, MSG_TRIGGER_MODEL_DOWNLOAD,
                                new ModelDownloadArgs(
                                        recognizerIntent,
                                        attributionSource,
                                        listener)));
            }
        }

        public void clearReference() {
            mServiceRef.clear();
        }
    }

    private boolean checkPermissionAndStartDataDelivery(SessionState sessionState) {
        if (sessionState.mCallback.mAttributionContextCreated) {
            return true;
        }

        if (PermissionChecker.checkPermissionAndStartDataDelivery(
                RecognitionService.this,
                Manifest.permission.RECORD_AUDIO,
                sessionState.mCallback.getAttributionContextForCaller().getAttributionSource(),
                /* message */ null)
                == PermissionChecker.PERMISSION_GRANTED) {
            sessionState.mStartedDataDelivery = true;
        }

        return sessionState.mStartedDataDelivery;
    }

    private boolean checkPermissionForPreflightNotHardDenied(AttributionSource attributionSource) {
        int result = PermissionChecker.checkPermissionForPreflight(RecognitionService.this,
                Manifest.permission.RECORD_AUDIO, attributionSource);
        return result == PermissionChecker.PERMISSION_GRANTED
                || result == PermissionChecker.PERMISSION_SOFT_DENIED;
    }

    void finishDataDelivery(SessionState sessionState) {
        if (sessionState.mStartedDataDelivery) {
            sessionState.mStartedDataDelivery = false;
            final String op = AppOpsManager.permissionToOp(Manifest.permission.RECORD_AUDIO);
            PermissionChecker.finishDataDelivery(RecognitionService.this, op,
                    sessionState.mCallback.getAttributionContextForCaller().getAttributionSource());
        }
    }

    /**
     * Data class containing information about an ongoing session:
     * <ul>
     *   <li> {@link SessionState#mCallback} - callback of the client that invoked the
     *   {@link RecognitionService#onStartListening(Intent, Callback)} method;
     *   <li> {@link SessionState#mStartedDataDelivery} - flag denoting if data
     *   is being delivered to the client.
     */
    private static class SessionState {
        private Callback mCallback;
        private boolean mStartedDataDelivery;

        SessionState(Callback callback, boolean startedDataDelivery) {
            mCallback = callback;
            mStartedDataDelivery = startedDataDelivery;
        }

        SessionState(Callback currentCallback) {
            this(currentCallback, false);
        }

        void reset() {
            mCallback = null;
            mStartedDataDelivery = false;
        }
    }
}
