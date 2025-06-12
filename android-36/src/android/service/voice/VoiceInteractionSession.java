/**
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

package android.service.voice;

import static android.view.Display.DEFAULT_DISPLAY;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.TestApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.DirectAction;
import android.app.Instrumentation;
import android.app.VoiceInteractor;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.Message;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.voice.flags.Flags;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.android.internal.annotations.Immutable;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.function.pooled.PooledLambda;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * An active voice interaction session, providing a facility for the implementation
 * to interact with the user in the voice interaction layer. The user interface is
 * initially shown by default, and can be created by overriding {@link #onCreateContentView()}
 * in which the UI can be built.
 *
 * <p>A voice interaction session can be self-contained, ultimately calling {@link #finish}
 * when done. It can also initiate voice interactions with applications by calling
 * {@link #startVoiceActivity}</p>.
 */
public class VoiceInteractionSession implements KeyEvent.Callback, ComponentCallbacks2 {
    static final String TAG = "VoiceInteractionSession";
    static final boolean DEBUG = false;

    /**
     * Flag received in {@link #onShow}: originator requested that the session be started with
     * assist data from the currently focused activity.
     */
    public static final int SHOW_WITH_ASSIST = 1<<0;

    /**
     * Flag received in {@link #onShow}: originator requested that the session be started with
     * a screen shot of the currently focused activity.
     */
    public static final int SHOW_WITH_SCREENSHOT = 1<<1;

    /**
     * Flag for use with {@link #onShow}: indicates that the session has been started from the
     * system assist gesture.
     */
    public static final int SHOW_SOURCE_ASSIST_GESTURE = 1<<2;

    /**
     * Flag for use with {@link #onShow}: indicates that the application itself has invoked
     * the assistant.
     */
    public static final int SHOW_SOURCE_APPLICATION = 1<<3;

    /**
     * Flag for use with {@link #onShow}: indicates that an Activity has invoked the voice
     * interaction service for a local interaction using
     * {@link Activity#startLocalVoiceInteraction(Bundle)}.
     */
    public static final int SHOW_SOURCE_ACTIVITY = 1<<4;

    /**
     * Flag for use with {@link #onShow}: indicates that the voice interaction service was invoked
     * from a physical button.
     */
    public static final int SHOW_SOURCE_PUSH_TO_TALK = 1 << 5;

    /**
     * Flag for use with {@link #onShow}: indicates that the voice interaction service was invoked
     * from a notification.
     */
    public static final int SHOW_SOURCE_NOTIFICATION = 1 << 6;

    /**
     * Flag for use with {@link #onShow}: indicates that the voice interaction service was invoked
     * from an Android automotive system UI.
     */
    public static final int SHOW_SOURCE_AUTOMOTIVE_SYSTEM_UI = 1 << 7;

    /** @hide */
    public static final int VOICE_INTERACTION_ACTIVITY_EVENT_START = 1;
    /** @hide */
    public static final int VOICE_INTERACTION_ACTIVITY_EVENT_RESUME = 2;
    /** @hide */
    public static final int VOICE_INTERACTION_ACTIVITY_EVENT_PAUSE = 3;
    /** @hide */
    public static final int VOICE_INTERACTION_ACTIVITY_EVENT_STOP = 4;

    /** @hide */
    @IntDef(prefix = { "VOICE_INTERACTION_ACTIVITY_EVENT_" }, value = {
            VOICE_INTERACTION_ACTIVITY_EVENT_START,
            VOICE_INTERACTION_ACTIVITY_EVENT_RESUME,
            VOICE_INTERACTION_ACTIVITY_EVENT_PAUSE,
            VOICE_INTERACTION_ACTIVITY_EVENT_STOP
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface VoiceInteractionActivityEventType{}

    /**
     * Bundle key used to specify the id when the system prepares to show session. It increases for
     * each request.
     * <p>
     * Type: int
     * </p>
     * @see VoiceInteractionService#showSession(Bundle, int)
     * @see VoiceInteractionService#onPrepareToShowSession(Bundle, int)
     * @see VoiceInteractionService#onShowSessionFailed(Bundle)
     * @see #onShow(Bundle, int)
     * @see #show(Bundle, int)
     */
    public static final String KEY_SHOW_SESSION_ID = "android.service.voice.SHOW_SESSION_ID";

    /**
     * Bundle key used to specify foreground activity app components.
     * <p>
     * Type: ArrayList&ltComponentName&gt
     * </p>
     * @see #onShow(Bundle, int)
     */
    @FlaggedApi(Flags.FLAG_ALLOW_FOREGROUND_ACTIVITIES_IN_ON_SHOW)
    public static final String KEY_FOREGROUND_ACTIVITIES =
            "android.service.voice.FOREGROUND_ACTIVITIES";

    final Context mContext;
    final HandlerCaller mHandlerCaller;

    final KeyEvent.DispatcherState mDispatcherState = new KeyEvent.DispatcherState();

    IVoiceInteractionManagerService mSystemService;
    IBinder mToken;

    int mTheme = 0;
    LayoutInflater mInflater;
    TypedArray mThemeAttrs;
    View mRootView;
    FrameLayout mContentFrame;
    VoiceInteractionWindow mWindow;

    boolean mUiEnabled = true;
    boolean mInitialized;
    boolean mWindowAdded;
    boolean mWindowVisible;
    boolean mWindowWasVisible;
    boolean mInShowWindow;

    final ArrayMap<IBinder, Request> mActiveRequests = new ArrayMap<IBinder, Request>();

    final Insets mTmpInsets = new Insets();

    final WeakReference<VoiceInteractionSession> mWeakRef
            = new WeakReference<VoiceInteractionSession>(this);

    // Registry of remote callbacks pending a reply with reply handles.
    final Map<SafeResultListener, Consumer<Bundle>> mRemoteCallbacks = new ArrayMap<>();

    ICancellationSignal mKillCallback;

    private final Map<VisibleActivityCallback, Executor> mVisibleActivityCallbacks =
            new ArrayMap<>();
    private final List<VisibleActivityInfo> mVisibleActivityInfos = new ArrayList<>();

    final IVoiceInteractor mInteractor = new IVoiceInteractor.Stub() {
        @Override
        public IVoiceInteractorRequest startConfirmation(String callingPackage,
                IVoiceInteractorCallback callback, VoiceInteractor.Prompt prompt, Bundle extras) {
            ConfirmationRequest request = new ConfirmationRequest(callingPackage,
                    Binder.getCallingUid(), callback, VoiceInteractionSession.this,
                    prompt, extras);
            addRequest(request);
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageO(MSG_START_CONFIRMATION,
                    request));
            return request.mInterface;
        }

        @Override
        public IVoiceInteractorRequest startPickOption(String callingPackage,
                IVoiceInteractorCallback callback, VoiceInteractor.Prompt prompt,
                VoiceInteractor.PickOptionRequest.Option[] options, Bundle extras) {
            PickOptionRequest request = new PickOptionRequest(callingPackage,
                    Binder.getCallingUid(), callback, VoiceInteractionSession.this,
                    prompt, options, extras);
            addRequest(request);
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageO(MSG_START_PICK_OPTION,
                    request));
            return request.mInterface;
        }

        @Override
        public IVoiceInteractorRequest startCompleteVoice(String callingPackage,
                IVoiceInteractorCallback callback, VoiceInteractor.Prompt message, Bundle extras) {
            CompleteVoiceRequest request = new CompleteVoiceRequest(callingPackage,
                    Binder.getCallingUid(), callback, VoiceInteractionSession.this,
                    message, extras);
            addRequest(request);
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageO(MSG_START_COMPLETE_VOICE,
                    request));
            return request.mInterface;
        }

        @Override
        public IVoiceInteractorRequest startAbortVoice(String callingPackage,
                IVoiceInteractorCallback callback, VoiceInteractor.Prompt message, Bundle extras) {
            AbortVoiceRequest request = new AbortVoiceRequest(callingPackage,
                    Binder.getCallingUid(), callback, VoiceInteractionSession.this,
                    message, extras);
            addRequest(request);
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageO(MSG_START_ABORT_VOICE,
                    request));
            return request.mInterface;
        }

        @Override
        public IVoiceInteractorRequest startCommand(String callingPackage,
                IVoiceInteractorCallback callback, String command, Bundle extras) {
            CommandRequest request = new CommandRequest(callingPackage,
                    Binder.getCallingUid(), callback, VoiceInteractionSession.this,
                    command, extras);
            addRequest(request);
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageO(MSG_START_COMMAND,
                    request));
            return request.mInterface;
        }

        @Override
        public boolean[] supportsCommands(String callingPackage, String[] commands) {
            Message msg = mHandlerCaller.obtainMessageIOO(MSG_SUPPORTS_COMMANDS,
                    0, commands, null);
            SomeArgs args = mHandlerCaller.sendMessageAndWait(msg);
            if (args != null) {
                boolean[] res = (boolean[])args.arg1;
                args.recycle();
                return res;
            }
            return new boolean[commands.length];
        }

        @Override
        public void notifyDirectActionsChanged(int taskId, IBinder assistToken) {
            mHandlerCaller.getHandler().sendMessage(PooledLambda.obtainMessage(
                    VoiceInteractionSession::onDirectActionsInvalidated,
                    VoiceInteractionSession.this, new ActivityId(taskId, assistToken))
            );
        }

        @Override
        public void setKillCallback(ICancellationSignal callback) {
            mKillCallback = callback;
        }
    };

    final IVoiceInteractionSession mSession = new IVoiceInteractionSession.Stub() {
        @Override
        public void show(Bundle sessionArgs, int flags,
                IVoiceInteractionSessionShowCallback showCallback) {
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageIOO(MSG_SHOW,
                    flags, sessionArgs, showCallback));
        }

        @Override
        public void hide() {
            // Remove any pending messages to show the session
            mHandlerCaller.removeMessages(MSG_SHOW);
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessage(MSG_HIDE));
        }

        @Override
        public void handleAssist(final int taskId, final IBinder assistToken, final Bundle data,
                final AssistStructure structure, final AssistContent content, final int index,
                final int count) {
            // We want to pre-warm the AssistStructure before handing it off to the main
            // thread.  We also want to do this on a separate thread, so that if the app
            // is for some reason slow (due to slow filling in of async children in the
            // structure), we don't block other incoming IPCs (such as the screenshot) to
            // us (since we are a oneway interface, they get serialized).  (Okay?)
            Thread retriever = new Thread("AssistStructure retriever") {
                @Override
                public void run() {
                    Throwable failure = null;
                    if (structure != null) {
                        try {
                            structure.ensureData();
                        } catch (Throwable e) {
                            Log.w(TAG, "Failure retrieving AssistStructure", e);
                            failure = e;
                        }
                    }

                    SomeArgs args = SomeArgs.obtain();
                    args.argi1 = taskId;
                    args.arg1 = data;
                    args.arg2 = (failure == null) ? structure : null;
                    args.arg3 = failure;
                    args.arg4 = content;
                    args.arg5 = assistToken;
                    args.argi5 = index;
                    args.argi6 = count;

                    mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageO(
                            MSG_HANDLE_ASSIST, args));
                }
            };
            retriever.start();
        }

        @Override
        public void handleScreenshot(Bitmap screenshot) {
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageO(MSG_HANDLE_SCREENSHOT,
                    screenshot));
        }

        @Override
        public void taskStarted(Intent intent, int taskId) {
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageIO(MSG_TASK_STARTED,
                    taskId, intent));
        }

        @Override
        public void taskFinished(Intent intent, int taskId) {
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessageIO(MSG_TASK_FINISHED,
                    taskId, intent));
        }

        @Override
        public void closeSystemDialogs() {
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessage(MSG_CLOSE_SYSTEM_DIALOGS));
        }

        @Override
        public void onLockscreenShown() {
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessage(MSG_ON_LOCKSCREEN_SHOWN));
        }

        @Override
        public void destroy() {
            mHandlerCaller.sendMessage(mHandlerCaller.obtainMessage(MSG_DESTROY));
        }

        @Override
        public void notifyVisibleActivityInfoChanged(VisibleActivityInfo visibleActivityInfo,
                int type) {
            mHandlerCaller.sendMessage(
                    mHandlerCaller.obtainMessageIO(MSG_NOTIFY_VISIBLE_ACTIVITY_INFO_CHANGED, type,
                            visibleActivityInfo));
        }
    };

    /**
     * Base class representing a request from a voice-driver app to perform a particular
     * voice operation with the user.  See related subclasses for the types of requests
     * that are possible.
     */
    public static class Request {
        final IVoiceInteractorRequest mInterface = new IVoiceInteractorRequest.Stub() {
            @Override
            public void cancel() throws RemoteException {
                VoiceInteractionSession session = mSession.get();
                if (session != null) {
                    session.mHandlerCaller.sendMessage(
                            session.mHandlerCaller.obtainMessageO(MSG_CANCEL, Request.this));
                }
            }
        };
        final String mCallingPackage;
        final int mCallingUid;
        final IVoiceInteractorCallback mCallback;
        final WeakReference<VoiceInteractionSession> mSession;
        final Bundle mExtras;

        Request(String packageName, int uid, IVoiceInteractorCallback callback,
                VoiceInteractionSession session, Bundle extras) {
            mCallingPackage = packageName;
            mCallingUid = uid;
            mCallback = callback;
            mSession = session.mWeakRef;
            mExtras = extras;
        }

        /**
         * Return the uid of the application that initiated the request.
         */
        public int getCallingUid() {
            return mCallingUid;
        }

        /**
         * Return the package name of the application that initiated the request.
         */
        public String getCallingPackage() {
            return mCallingPackage;
        }

        /**
         * Return any additional extra information that was supplied as part of the request.
         */
        public Bundle getExtras() {
            return mExtras;
        }

        /**
         * Check whether this request is currently active.  A request becomes inactive after
         * calling {@link #cancel} or a final result method that completes the request.  After
         * this point, further interactions with the request will result in
         * {@link java.lang.IllegalStateException} errors; you should not catch these errors,
         * but can use this method if you need to determine the state of the request.  Returns
         * true if the request is still active.
         */
        public boolean isActive() {
            VoiceInteractionSession session = mSession.get();
            if (session == null) {
                return false;
            }
            return session.isRequestActive(mInterface.asBinder());
        }

        void finishRequest() {
            VoiceInteractionSession session = mSession.get();
            if (session == null) {
                throw new IllegalStateException("VoiceInteractionSession has been destroyed");
            }
            Request req = session.removeRequest(mInterface.asBinder());
            if (req == null) {
                throw new IllegalStateException("Request not active: " + this);
            } else if (req != this) {
                throw new IllegalStateException("Current active request " + req
                        + " not same as calling request " + this);
            }
        }

        /**
         * Ask the app to cancel this current request.
         * This also finishes the request (it is no longer active).
         */
        public void cancel() {
            try {
                if (DEBUG) Log.d(TAG, "sendCancelResult: req=" + mInterface);
                finishRequest();
                mCallback.deliverCancel(mInterface);
            } catch (RemoteException e) {
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            DebugUtils.buildShortClassTag(this, sb);
            sb.append(" ");
            sb.append(mInterface.asBinder());
            sb.append(" pkg=");
            sb.append(mCallingPackage);
            sb.append(" uid=");
            UserHandle.formatUid(sb, mCallingUid);
            sb.append('}');
            return sb.toString();
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            writer.print(prefix); writer.print("mInterface=");
            writer.println(mInterface.asBinder());
            writer.print(prefix); writer.print("mCallingPackage="); writer.print(mCallingPackage);
            writer.print(" mCallingUid="); UserHandle.formatUid(writer, mCallingUid);
            writer.println();
            writer.print(prefix); writer.print("mCallback=");
            writer.println(mCallback.asBinder());
            if (mExtras != null) {
                writer.print(prefix); writer.print("mExtras=");
                writer.println(mExtras);
            }
        }
    }

    /**
     * A request for confirmation from the user of an operation, as per
     * {@link android.app.VoiceInteractor.ConfirmationRequest
     * VoiceInteractor.ConfirmationRequest}.
     */
    public static final class ConfirmationRequest extends Request {
        final VoiceInteractor.Prompt mPrompt;

        ConfirmationRequest(String packageName, int uid, IVoiceInteractorCallback callback,
                VoiceInteractionSession session, VoiceInteractor.Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            mPrompt = prompt;
        }

        /**
         * Return the prompt informing the user of what will happen, as per
         * {@link android.app.VoiceInteractor.ConfirmationRequest
         * VoiceInteractor.ConfirmationRequest}.
         */
        @Nullable
        public VoiceInteractor.Prompt getVoicePrompt() {
            return mPrompt;
        }

        /**
         * Return the prompt informing the user of what will happen, as per
         * {@link android.app.VoiceInteractor.ConfirmationRequest
         * VoiceInteractor.ConfirmationRequest}.
         * @deprecated Prefer {@link #getVoicePrompt()} which allows multiple voice prompts.
         */
        @Deprecated
        @Nullable
        public CharSequence getPrompt() {
            return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
        }

        /**
         * Report that the voice interactor has confirmed the operation with the user, resulting
         * in a call to
         * {@link android.app.VoiceInteractor.ConfirmationRequest#onConfirmationResult
         * VoiceInteractor.ConfirmationRequest.onConfirmationResult}.
         * This finishes the request (it is no longer active).
         */
        public void sendConfirmationResult(boolean confirmed, Bundle result) {
            try {
                if (DEBUG) Log.d(TAG, "sendConfirmationResult: req=" + mInterface
                        + " confirmed=" + confirmed + " result=" + result);
                finishRequest();
                mCallback.deliverConfirmationResult(mInterface, confirmed, result);
            } catch (RemoteException e) {
            }
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix); writer.print("mPrompt=");
            writer.println(mPrompt);
        }
    }

    /**
     * A request for the user to pick from a set of option, as per
     * {@link android.app.VoiceInteractor.PickOptionRequest VoiceInteractor.PickOptionRequest}.
     */
    public static final class PickOptionRequest extends Request {
        final VoiceInteractor.Prompt mPrompt;
        final VoiceInteractor.PickOptionRequest.Option[] mOptions;

        PickOptionRequest(String packageName, int uid, IVoiceInteractorCallback callback,
                VoiceInteractionSession session, VoiceInteractor.Prompt prompt,
                VoiceInteractor.PickOptionRequest.Option[] options, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            mPrompt = prompt;
            mOptions = options;
        }

        /**
         * Return the prompt informing the user of what they are picking, as per
         * {@link android.app.VoiceInteractor.PickOptionRequest VoiceInteractor.PickOptionRequest}.
         */
        @Nullable
        public VoiceInteractor.Prompt getVoicePrompt() {
            return mPrompt;
        }

        /**
         * Return the prompt informing the user of what they are picking, as per
         * {@link android.app.VoiceInteractor.PickOptionRequest VoiceInteractor.PickOptionRequest}.
         * @deprecated Prefer {@link #getVoicePrompt()} which allows multiple voice prompts.
         */
        @Deprecated
        @Nullable
        public CharSequence getPrompt() {
            return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
        }

        /**
         * Return the set of options the user is picking from, as per
         * {@link android.app.VoiceInteractor.PickOptionRequest VoiceInteractor.PickOptionRequest}.
         */
        public VoiceInteractor.PickOptionRequest.Option[] getOptions() {
            return mOptions;
        }

        void sendPickOptionResult(boolean finished,
                VoiceInteractor.PickOptionRequest.Option[] selections, Bundle result) {
            try {
                if (DEBUG) Log.d(TAG, "sendPickOptionResult: req=" + mInterface
                        + " finished=" + finished + " selections=" + Arrays.toString(selections)
                        + " result=" + result);
                if (finished) {
                    finishRequest();
                }
                mCallback.deliverPickOptionResult(mInterface, finished, selections, result);
            } catch (RemoteException e) {
            }
        }

        /**
         * Report an intermediate option selection from the request, without completing it (the
         * request is still active and the app is waiting for the final option selection),
         * resulting in a call to
         * {@link android.app.VoiceInteractor.PickOptionRequest#onPickOptionResult
         * VoiceInteractor.PickOptionRequest.onPickOptionResult} with false for finished.
         */
        public void sendIntermediatePickOptionResult(
                VoiceInteractor.PickOptionRequest.Option[] selections, Bundle result) {
            sendPickOptionResult(false, selections, result);
        }

        /**
         * Report the final option selection for the request, completing the request
         * and resulting in a call to
         * {@link android.app.VoiceInteractor.PickOptionRequest#onPickOptionResult
         * VoiceInteractor.PickOptionRequest.onPickOptionResult} with false for finished.
         * This finishes the request (it is no longer active).
         */
        public void sendPickOptionResult(
                VoiceInteractor.PickOptionRequest.Option[] selections, Bundle result) {
            sendPickOptionResult(true, selections, result);
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix); writer.print("mPrompt=");
            writer.println(mPrompt);
            if (mOptions != null) {
                writer.print(prefix); writer.println("Options:");
                for (int i=0; i<mOptions.length; i++) {
                    VoiceInteractor.PickOptionRequest.Option op = mOptions[i];
                    writer.print(prefix); writer.print("  #"); writer.print(i); writer.println(":");
                    writer.print(prefix); writer.print("    mLabel=");
                    writer.println(op.getLabel());
                    writer.print(prefix); writer.print("    mIndex=");
                    writer.println(op.getIndex());
                    if (op.countSynonyms() > 0) {
                        writer.print(prefix); writer.println("    Synonyms:");
                        for (int j=0; j<op.countSynonyms(); j++) {
                            writer.print(prefix); writer.print("      #"); writer.print(j);
                            writer.print(": "); writer.println(op.getSynonymAt(j));
                        }
                    }
                    if (op.getExtras() != null) {
                        writer.print(prefix); writer.print("    mExtras=");
                        writer.println(op.getExtras());
                    }
                }
            }
        }
    }

    /**
     * A request to simply inform the user that the voice operation has completed, as per
     * {@link android.app.VoiceInteractor.CompleteVoiceRequest
     * VoiceInteractor.CompleteVoiceRequest}.
     */
    public static final class CompleteVoiceRequest extends Request {
        final VoiceInteractor.Prompt mPrompt;

        CompleteVoiceRequest(String packageName, int uid, IVoiceInteractorCallback callback,
                VoiceInteractionSession session, VoiceInteractor.Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            mPrompt = prompt;
        }

        /**
         * Return the message informing the user of the completion, as per
         * {@link android.app.VoiceInteractor.CompleteVoiceRequest
         * VoiceInteractor.CompleteVoiceRequest}.
         */
        @Nullable
        public VoiceInteractor.Prompt getVoicePrompt() {
            return mPrompt;
        }

        /**
         * Return the message informing the user of the completion, as per
         * {@link android.app.VoiceInteractor.CompleteVoiceRequest
         * VoiceInteractor.CompleteVoiceRequest}.
         * @deprecated Prefer {@link #getVoicePrompt()} which allows a separate visual message.
         */
        @Deprecated
        @Nullable
        public CharSequence getMessage() {
            return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
        }

        /**
         * Report that the voice interactor has finished completing the voice operation, resulting
         * in a call to
         * {@link android.app.VoiceInteractor.CompleteVoiceRequest#onCompleteResult
         * VoiceInteractor.CompleteVoiceRequest.onCompleteResult}.
         * This finishes the request (it is no longer active).
         */
        public void sendCompleteResult(Bundle result) {
            try {
                if (DEBUG) Log.d(TAG, "sendCompleteVoiceResult: req=" + mInterface
                        + " result=" + result);
                finishRequest();
                mCallback.deliverCompleteVoiceResult(mInterface, result);
            } catch (RemoteException e) {
            }
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix); writer.print("mPrompt=");
            writer.println(mPrompt);
        }
    }

    /**
     * A request to report that the current user interaction can not be completed with voice, as per
     * {@link android.app.VoiceInteractor.AbortVoiceRequest VoiceInteractor.AbortVoiceRequest}.
     */
    public static final class AbortVoiceRequest extends Request {
        final VoiceInteractor.Prompt mPrompt;

        AbortVoiceRequest(String packageName, int uid, IVoiceInteractorCallback callback,
                VoiceInteractionSession session, VoiceInteractor.Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            mPrompt = prompt;
        }

        /**
         * Return the message informing the user of the problem, as per
         * {@link android.app.VoiceInteractor.AbortVoiceRequest VoiceInteractor.AbortVoiceRequest}.
         */
        @Nullable
        public VoiceInteractor.Prompt getVoicePrompt() {
            return mPrompt;
        }

        /**
         * Return the message informing the user of the problem, as per
         * {@link android.app.VoiceInteractor.AbortVoiceRequest VoiceInteractor.AbortVoiceRequest}.
         * @deprecated Prefer {@link #getVoicePrompt()} which allows a separate visual message.
         */
        @Deprecated
        @Nullable
        public CharSequence getMessage() {
            return (mPrompt != null ? mPrompt.getVoicePromptAt(0) : null);
        }

        /**
         * Report that the voice interactor has finished aborting the voice operation, resulting
         * in a call to
         * {@link android.app.VoiceInteractor.AbortVoiceRequest#onAbortResult
         * VoiceInteractor.AbortVoiceRequest.onAbortResult}.  This finishes the request (it
         * is no longer active).
         */
        public void sendAbortResult(Bundle result) {
            try {
                if (DEBUG) Log.d(TAG, "sendConfirmResult: req=" + mInterface
                        + " result=" + result);
                finishRequest();
                mCallback.deliverAbortVoiceResult(mInterface, result);
            } catch (RemoteException e) {
            }
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix); writer.print("mPrompt=");
            writer.println(mPrompt);
        }
    }

    /**
     * A generic vendor-specific request, as per
     * {@link android.app.VoiceInteractor.CommandRequest VoiceInteractor.CommandRequest}.
     */
    public static final class CommandRequest extends Request {
        final String mCommand;

        CommandRequest(String packageName, int uid, IVoiceInteractorCallback callback,
                VoiceInteractionSession session, String command, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            mCommand = command;
        }

        /**
         * Return the command that is being executed, as per
         * {@link android.app.VoiceInteractor.CommandRequest VoiceInteractor.CommandRequest}.
         */
        public String getCommand() {
            return mCommand;
        }

        void sendCommandResult(boolean finished, Bundle result) {
            try {
                if (DEBUG) Log.d(TAG, "sendCommandResult: req=" + mInterface
                        + " result=" + result);
                if (finished) {
                    finishRequest();
                }
                mCallback.deliverCommandResult(mInterface, finished, result);
            } catch (RemoteException e) {
            }
        }

        /**
         * Report an intermediate result of the request, without completing it (the request
         * is still active and the app is waiting for the final result), resulting in a call to
         * {@link android.app.VoiceInteractor.CommandRequest#onCommandResult
         * VoiceInteractor.CommandRequest.onCommandResult} with false for isCompleted.
         */
        public void sendIntermediateResult(Bundle result) {
            sendCommandResult(false, result);
        }

        /**
         * Report the final result of the request, completing the request and resulting in a call to
         * {@link android.app.VoiceInteractor.CommandRequest#onCommandResult
         * VoiceInteractor.CommandRequest.onCommandResult} with true for isCompleted.
         * This finishes the request (it is no longer active).
         */
        public void sendResult(Bundle result) {
            sendCommandResult(true, result);
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix); writer.print("mCommand=");
            writer.println(mCommand);
        }
    }

    static final int MSG_START_CONFIRMATION = 1;
    static final int MSG_START_PICK_OPTION = 2;
    static final int MSG_START_COMPLETE_VOICE = 3;
    static final int MSG_START_ABORT_VOICE = 4;
    static final int MSG_START_COMMAND = 5;
    static final int MSG_SUPPORTS_COMMANDS = 6;
    static final int MSG_CANCEL = 7;

    static final int MSG_TASK_STARTED = 100;
    static final int MSG_TASK_FINISHED = 101;
    static final int MSG_CLOSE_SYSTEM_DIALOGS = 102;
    static final int MSG_DESTROY = 103;
    static final int MSG_HANDLE_ASSIST = 104;
    static final int MSG_HANDLE_SCREENSHOT = 105;
    static final int MSG_SHOW = 106;
    static final int MSG_HIDE = 107;
    static final int MSG_ON_LOCKSCREEN_SHOWN = 108;
    static final int MSG_NOTIFY_VISIBLE_ACTIVITY_INFO_CHANGED = 109;
    static final int MSG_REGISTER_VISIBLE_ACTIVITY_CALLBACK = 110;
    static final int MSG_UNREGISTER_VISIBLE_ACTIVITY_CALLBACK = 111;

    class MyCallbacks implements HandlerCaller.Callback, VoiceInteractionWindow.Callback {
        @Override
        public void executeMessage(Message msg) {
            SomeArgs args = null;
            switch (msg.what) {
                case MSG_START_CONFIRMATION:
                    if (DEBUG) Log.d(TAG, "onConfirm: req=" + msg.obj);
                    onRequestConfirmation((ConfirmationRequest) msg.obj);
                    break;
                case MSG_START_PICK_OPTION:
                    if (DEBUG) Log.d(TAG, "onPickOption: req=" + msg.obj);
                    onRequestPickOption((PickOptionRequest) msg.obj);
                    break;
                case MSG_START_COMPLETE_VOICE:
                    if (DEBUG) Log.d(TAG, "onCompleteVoice: req=" + msg.obj);
                    onRequestCompleteVoice((CompleteVoiceRequest) msg.obj);
                    break;
                case MSG_START_ABORT_VOICE:
                    if (DEBUG) Log.d(TAG, "onAbortVoice: req=" + msg.obj);
                    onRequestAbortVoice((AbortVoiceRequest) msg.obj);
                    break;
                case MSG_START_COMMAND:
                    if (DEBUG) Log.d(TAG, "onCommand: req=" + msg.obj);
                    onRequestCommand((CommandRequest) msg.obj);
                    break;
                case MSG_SUPPORTS_COMMANDS:
                    args = (SomeArgs)msg.obj;
                    if (DEBUG) Log.d(TAG, "onGetSupportedCommands: cmds=" + args.arg1);
                    args.arg1 = onGetSupportedCommands((String[]) args.arg1);
                    args.complete();
                    args = null;
                    break;
                case MSG_CANCEL:
                    if (DEBUG) Log.d(TAG, "onCancel: req=" + ((Request)msg.obj));
                    onCancelRequest((Request) msg.obj);
                    break;
                case MSG_TASK_STARTED:
                    if (DEBUG) Log.d(TAG, "onTaskStarted: intent=" + msg.obj
                            + " taskId=" + msg.arg1);
                    onTaskStarted((Intent) msg.obj, msg.arg1);
                    break;
                case MSG_TASK_FINISHED:
                    if (DEBUG) Log.d(TAG, "onTaskFinished: intent=" + msg.obj
                            + " taskId=" + msg.arg1);
                    onTaskFinished((Intent) msg.obj, msg.arg1);
                    break;
                case MSG_CLOSE_SYSTEM_DIALOGS:
                    if (DEBUG) Log.d(TAG, "onCloseSystemDialogs");
                    onCloseSystemDialogs();
                    break;
                case MSG_DESTROY:
                    if (DEBUG) Log.d(TAG, "doDestroy");
                    doDestroy();
                    break;
                case MSG_HANDLE_ASSIST:
                    args = (SomeArgs)msg.obj;
                    if (DEBUG) Log.d(TAG, "onHandleAssist: taskId=" + args.argi1
                            + "assistToken=" + args.arg5 + " data=" + args.arg1
                            + " structure=" + args.arg2 + " content=" + args.arg3
                            + " activityIndex=" + args.argi5 + " activityCount=" + args.argi6);
                    doOnHandleAssist(args.argi1, (IBinder) args.arg5, (Bundle) args.arg1,
                            (AssistStructure) args.arg2, (Throwable) args.arg3,
                            (AssistContent) args.arg4, args.argi5, args.argi6);
                    break;
                case MSG_HANDLE_SCREENSHOT:
                    if (DEBUG) Log.d(TAG, "onHandleScreenshot: " + msg.obj);
                    onHandleScreenshot((Bitmap) msg.obj);
                    break;
                case MSG_SHOW:
                    args = (SomeArgs)msg.obj;
                    if (DEBUG) Log.d(TAG, "doShow: args=" + args.arg1
                            + " flags=" + msg.arg1
                            + " showCallback=" + args.arg2);
                    doShow((Bundle) args.arg1, msg.arg1,
                            (IVoiceInteractionSessionShowCallback) args.arg2);
                    break;
                case MSG_HIDE:
                    if (DEBUG) Log.d(TAG, "doHide");
                    doHide();
                    break;
                case MSG_ON_LOCKSCREEN_SHOWN:
                    if (DEBUG) Log.d(TAG, "onLockscreenShown");
                    onLockscreenShown();
                    break;
                case MSG_NOTIFY_VISIBLE_ACTIVITY_INFO_CHANGED:
                    if (DEBUG) {
                        Log.d(TAG,
                                "doNotifyVisibleActivityInfoChanged: visibleActivityInfo=" + msg.obj
                                        + " type=" + msg.arg1);
                    }
                    doNotifyVisibleActivityInfoChanged((VisibleActivityInfo) msg.obj, msg.arg1);
                    break;
                case MSG_REGISTER_VISIBLE_ACTIVITY_CALLBACK:
                    if (DEBUG) {
                        Log.d(TAG, "doRegisterVisibleActivityCallback");
                    }
                    args = (SomeArgs) msg.obj;
                    doRegisterVisibleActivityCallback((Executor) args.arg1,
                            (VisibleActivityCallback) args.arg2);
                    break;
                case MSG_UNREGISTER_VISIBLE_ACTIVITY_CALLBACK:
                    if (DEBUG) {
                        Log.d(TAG, "doUnregisterVisibleActivityCallback");
                    }
                    doUnregisterVisibleActivityCallback((VisibleActivityCallback) msg.obj);
                    break;
            }
            if (args != null) {
                args.recycle();
            }
        }

        @Override
        public void onBackPressed() {
            VoiceInteractionSession.this.onBackPressed();
        }
    }

    final MyCallbacks mCallbacks = new MyCallbacks();

    /**
     * Information about where interesting parts of the input method UI appear.
     */
    public static final class Insets {
        /**
         * This is the part of the UI that is the main content.  It is
         * used to determine the basic space needed, to resize/pan the
         * application behind.  It is assumed that this inset does not
         * change very much, since any change will cause a full resize/pan
         * of the application behind.  This value is relative to the top edge
         * of the input method window.
         */
        public final Rect contentInsets = new Rect();

        /**
         * This is the region of the UI that is touchable.  It is used when
         * {@link #touchableInsets} is set to {@link #TOUCHABLE_INSETS_REGION}.
         * The region should be specified relative to the origin of the window frame.
         */
        public final Region touchableRegion = new Region();

        /**
         * Option for {@link #touchableInsets}: the entire window frame
         * can be touched.
         */
        public static final int TOUCHABLE_INSETS_FRAME
                = ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_FRAME;

        /**
         * Option for {@link #touchableInsets}: the area inside of
         * the content insets can be touched.
         */
        public static final int TOUCHABLE_INSETS_CONTENT
                = ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_CONTENT;

        /**
         * Option for {@link #touchableInsets}: the region specified by
         * {@link #touchableRegion} can be touched.
         */
        public static final int TOUCHABLE_INSETS_REGION
                = ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_REGION;

        /**
         * Determine which area of the window is touchable by the user.  May
         * be one of: {@link #TOUCHABLE_INSETS_FRAME},
         * {@link #TOUCHABLE_INSETS_CONTENT}, or {@link #TOUCHABLE_INSETS_REGION}.
         */
        public int touchableInsets;
    }

    final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer =
            new ViewTreeObserver.OnComputeInternalInsetsListener() {
        public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo info) {
            onComputeInsets(mTmpInsets);
            info.contentInsets.set(mTmpInsets.contentInsets);
            info.visibleInsets.set(mTmpInsets.contentInsets);
            info.touchableRegion.set(mTmpInsets.touchableRegion);
            info.setTouchableInsets(mTmpInsets.touchableInsets);
        }
    };

    public VoiceInteractionSession(Context context) {
        this(context, new Handler());
    }

    public VoiceInteractionSession(Context context, Handler handler) {
        mHandlerCaller = new HandlerCaller(context, handler.getLooper(),
                mCallbacks, true);
        mContext = createWindowContextIfNeeded(context);
    }

    public Context getContext() {
        return mContext;
    }

    private Context createWindowContextIfNeeded(Context context) {
        try {
            if (!context.isUiContext()) {
                DisplayManager displayManager = context.getSystemService(DisplayManager.class);
                if (displayManager != null) {
                    return context.createWindowContext(
                            displayManager.getDisplay(DEFAULT_DISPLAY),
                            WindowManager.LayoutParams.TYPE_VOICE_INTERACTION,
                            /* options= */ null);
                }
            }
            return context;
        } catch (RuntimeException e) {
            Log.w(TAG, "Fail to createWindowContext, Exception = " + e);
            return context;
        }
    }

    void addRequest(Request req) {
        synchronized (this) {
            mActiveRequests.put(req.mInterface.asBinder(), req);
        }
    }

    boolean isRequestActive(IBinder reqInterface) {
        synchronized (this) {
            return mActiveRequests.containsKey(reqInterface);
        }
    }

    Request removeRequest(IBinder reqInterface) {
        synchronized (this) {
            return mActiveRequests.remove(reqInterface);
        }
    }

    void doCreate(IVoiceInteractionManagerService service, IBinder token) {
        mSystemService = service;
        mToken = token;
        onCreate();
    }

    void doShow(Bundle args, int flags, final IVoiceInteractionSessionShowCallback showCallback) {
        if (DEBUG) Log.v(TAG, "Showing window: mWindowAdded=" + mWindowAdded
                + " mWindowVisible=" + mWindowVisible);

        if (mInShowWindow) {
            Log.w(TAG, "Re-entrance in to showWindow");
            return;
        }

        try {
            mInShowWindow = true;
            onPrepareShow(args, flags);
            if (!mWindowVisible) {
                ensureWindowAdded();
            }
            onShow(args, flags);
            if (!mWindowVisible) {
                mWindowVisible = true;
                if (mUiEnabled) {
                    showWindow();
                }
            }
            if (showCallback != null) {
                if (mUiEnabled) {
                    mRootView.invalidate();
                    mRootView.getViewTreeObserver().addOnPreDrawListener(
                            new ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                                    try {
                                        showCallback.onShown();
                                    } catch (RemoteException e) {
                                        Log.w(TAG, "Error calling onShown", e);
                                    }
                                    return true;
                                }
                            });
                } else {
                    try {
                        showCallback.onShown();
                    } catch (RemoteException e) {
                        Log.w(TAG, "Error calling onShown", e);
                    }
                }
            }
        } finally {
            mWindowWasVisible = true;
            mInShowWindow = false;
        }
    }

    void doHide() {
        if (mWindowVisible) {
            ensureWindowHidden();
            mWindowVisible = false;
            onHide();
        }
    }

    void doDestroy() {
        onDestroy();
        if (mKillCallback != null) {
            try {
                mKillCallback.cancel();
            } catch (RemoteException e) {
                /* ignore */
            }
            mKillCallback = null;
        }
        if (mInitialized) {
            mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(
                    mInsetsComputer);
            if (mWindowAdded) {
                mWindow.dismiss();
                mWindowAdded = false;
            }
            mInitialized = false;
        }
    }

    private void doNotifyVisibleActivityInfoChanged(VisibleActivityInfo visibleActivityInfo,
            int type) {

        if (mVisibleActivityCallbacks.isEmpty()) {
            return;
        }

        switch (type) {
            case VisibleActivityInfo.TYPE_ACTIVITY_ADDED:
                notifyVisibleActivityChanged(visibleActivityInfo, type);
                mVisibleActivityInfos.add(visibleActivityInfo);
                break;
            case VisibleActivityInfo.TYPE_ACTIVITY_REMOVED:
                notifyVisibleActivityChanged(visibleActivityInfo, type);
                mVisibleActivityInfos.remove(visibleActivityInfo);
                break;
        }
    }

    private void doRegisterVisibleActivityCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull VisibleActivityCallback callback) {
        if (mVisibleActivityCallbacks.containsKey(callback)) {
            if (DEBUG) {
                Log.d(TAG, "doRegisterVisibleActivityCallback: callback has registered");
            }
            return;
        }

        int preCallbackCount = mVisibleActivityCallbacks.size();
        mVisibleActivityCallbacks.put(callback, executor);

        if (preCallbackCount == 0) {
            try {
                mSystemService.startListeningVisibleActivityChanged(mToken);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        } else {
            for (int i = 0; i < mVisibleActivityInfos.size(); i++) {
                final VisibleActivityInfo visibleActivityInfo = mVisibleActivityInfos.get(i);
                executor.execute(() -> callback.onVisible(visibleActivityInfo));
            }
        }
    }

    private void doUnregisterVisibleActivityCallback(@NonNull VisibleActivityCallback callback) {
        mVisibleActivityCallbacks.remove(callback);

        if (mVisibleActivityCallbacks.size() == 0) {
            mVisibleActivityInfos.clear();
            try {
                mSystemService.stopListeningVisibleActivityChanged(mToken);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    private void notifyVisibleActivityChanged(VisibleActivityInfo visibleActivityInfo, int type) {
        for (Map.Entry<VisibleActivityCallback, Executor> e :
                mVisibleActivityCallbacks.entrySet()) {
            final Executor executor = e.getValue();
            final VisibleActivityCallback visibleActivityCallback = e.getKey();

            switch (type) {
                case VisibleActivityInfo.TYPE_ACTIVITY_ADDED:
                    Binder.withCleanCallingIdentity(() -> {
                        executor.execute(
                                () -> visibleActivityCallback.onVisible(visibleActivityInfo));
                    });
                    break;
                case VisibleActivityInfo.TYPE_ACTIVITY_REMOVED:
                    Binder.withCleanCallingIdentity(() -> {
                        executor.execute(() -> visibleActivityCallback.onInvisible(
                                visibleActivityInfo.getActivityId()));
                    });
                    break;
            }
        }
    }

    void ensureWindowCreated() {
        if (mInitialized) {
            return;
        }

        if (!mUiEnabled) {
            throw new IllegalStateException("setUiEnabled is false");
        }

        mInitialized = true;
        mInflater = (LayoutInflater)mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mWindow = new VoiceInteractionWindow(mContext, "VoiceInteractionSession", mTheme,
                mCallbacks, this, mDispatcherState,
                WindowManager.LayoutParams.TYPE_VOICE_INTERACTION, Gravity.BOTTOM, true);
        mWindow.getWindow().getAttributes().setFitInsetsTypes(0 /* types */);
        mWindow.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);

        mThemeAttrs = mContext.obtainStyledAttributes(android.R.styleable.VoiceInteractionSession);
        mRootView = mInflater.inflate(
                com.android.internal.R.layout.voice_interaction_session, null);
        mRootView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        mWindow.setContentView(mRootView);
        mRootView.getViewTreeObserver().addOnComputeInternalInsetsListener(mInsetsComputer);

        mContentFrame = (FrameLayout)mRootView.findViewById(android.R.id.content);

        mWindow.getWindow().setLayout(MATCH_PARENT, MATCH_PARENT);
        mWindow.setToken(mToken);
    }

    void ensureWindowAdded() {
        if (mUiEnabled && !mWindowAdded) {
            mWindowAdded = true;
            ensureWindowCreated();
            View v = onCreateContentView();
            if (v != null) {
                setContentView(v);
            }
        }
    }

    void showWindow() {
        if (mWindow != null) {
            mWindow.show();
            try {
                mSystemService.setSessionWindowVisible(mToken, true);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to notify session window shown", e);
            }
        }
    }

    void ensureWindowHidden() {
        if (mWindow != null) {
            mWindow.hide();
            try {
                mSystemService.setSessionWindowVisible(mToken, false);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to notify session window hidden", e);
            }
        }
    }

    /**
     * Equivalent to {@link VoiceInteractionService#setDisabledShowContext
     * VoiceInteractionService.setDisabledShowContext(int)}.
     */
    public void setDisabledShowContext(int flags) {
        try {
            mSystemService.setDisabledShowContext(flags);
        } catch (RemoteException e) {
        }
    }

    /**
     * Equivalent to {@link VoiceInteractionService#getDisabledShowContext
     * VoiceInteractionService.getDisabledShowContext}.
     */
    public int getDisabledShowContext() {
        try {
            return mSystemService.getDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    /**
     * Return which show context flags have been disabled by the user through the system
     * settings UI, so the session will never get this data.  Returned flags are any combination of
     * {@link VoiceInteractionSession#SHOW_WITH_ASSIST VoiceInteractionSession.SHOW_WITH_ASSIST} and
     * {@link VoiceInteractionSession#SHOW_WITH_SCREENSHOT
     * VoiceInteractionSession.SHOW_WITH_SCREENSHOT}.  Note that this only tells you about
     * global user settings, not about restrictions that may be applied contextual based on
     * the current application the user is in or other transient states.
     */
    public int getUserDisabledShowContext() {
        try {
            return mSystemService.getUserDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    /**
     * Show the UI for this session.  This asks the system to go through the process of showing
     * your UI, which will eventually culminate in {@link #onShow}.  This is similar to calling
     * {@link VoiceInteractionService#showSession VoiceInteractionService.showSession}.
     * @param args Arbitrary arguments that will be propagated {@link #onShow}.
     * @param flags Indicates additional optional behavior that should be performed. May
     * be any combination of
     * {@link VoiceInteractionSession#SHOW_WITH_ASSIST VoiceInteractionSession.SHOW_WITH_ASSIST} and
     * {@link VoiceInteractionSession#SHOW_WITH_SCREENSHOT
     * VoiceInteractionSession.SHOW_WITH_SCREENSHOT}
     * to request that the system generate and deliver assist data on the current foreground
     * app as part of showing the session UI.
     */
    public void show(Bundle args, int flags) {
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            mSystemService.showSessionFromSession(mToken, args, flags,
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
        }
    }

    /**
     * Hide the session's UI, if currently shown.  Call this when you are done with your
     * user interaction.
     */
    public void hide() {
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            mSystemService.hideSessionFromSession(mToken);
        } catch (RemoteException e) {
        }
    }

    /**
     * Control whether the UI layer for this session is enabled.  It is enabled by default.
     * If set to false, you will not be able to provide a UI through {@link #onCreateContentView()}.
     */
    public void setUiEnabled(boolean enabled) {
        if (mUiEnabled != enabled) {
            mUiEnabled = enabled;
            if (mWindowVisible) {
                if (enabled) {
                    ensureWindowAdded();
                    showWindow();
                } else {
                    ensureWindowHidden();
                }
            }
        }
    }

    /**
     * You can call this to customize the theme used by your IME's window.
     * This must be set before {@link #onCreate}, so you
     * will typically call it in your constructor with the resource ID
     * of your custom theme.
     */
    public void setTheme(int theme) {
        if (mWindow != null) {
            throw new IllegalStateException("Must be called before onCreate()");
        }
        mTheme = theme;
    }

    /**
     * Ask that a new activity be started for voice interaction.  This will create a
     * new dedicated task in the activity manager for this voice interaction session;
     * this means that {@link Intent#FLAG_ACTIVITY_NEW_TASK Intent.FLAG_ACTIVITY_NEW_TASK}
     * will be set for you to make it a new task.
     *
     * <p>The newly started activity will be displayed to the user in a special way, as
     * a layer under the voice interaction UI.</p>
     *
     * <p>As the voice activity runs, it can retrieve a {@link android.app.VoiceInteractor}
     * through which it can perform voice interactions through your session.  These requests
     * for voice interactions will appear as callbacks on {@link #onGetSupportedCommands},
     * {@link #onRequestConfirmation}, {@link #onRequestPickOption},
     * {@link #onRequestCompleteVoice}, {@link #onRequestAbortVoice},
     * or {@link #onRequestCommand}
     *
     * <p>You will receive a call to {@link #onTaskStarted} when the task starts up
     * and {@link #onTaskFinished} when the last activity has finished.
     *
     * @param intent The Intent to start this voice interaction.  The given Intent will
     * always have {@link Intent#CATEGORY_VOICE Intent.CATEGORY_VOICE} added to it, since
     * this is part of a voice interaction.
     */
    public void startVoiceActivity(Intent intent) {
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            intent.migrateExtraStreamToClipData(mContext);
            intent.prepareToLeaveProcess(mContext);
            int res = mSystemService.startVoiceActivity(mToken, intent,
                    intent.resolveType(mContext.getContentResolver()),
                    mContext.getAttributionTag());
            Instrumentation.checkStartActivityResult(res, intent);
        } catch (RemoteException e) {
        }
    }

    /**
     * <p>Ask that a new assistant activity be started.  This will create a new task in the
     * in activity manager: this means that
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK Intent.FLAG_ACTIVITY_NEW_TASK}
     * will be set for you to make it a new task.</p>
     *
     * <p>The newly started activity will be displayed on top of other activities in the system
     * in a new layer that is not affected by multi-window mode.  Tasks started from this activity
     * will go into the normal activity layer and not this new layer.</p>
     *
     * <p>By default, the system will create a window for the UI for this session.  If you are using
     * an assistant activity instead, then you can disable the window creation by calling
     * {@link #setUiEnabled} in {@link #onPrepareShow(Bundle, int)}.</p>
     *
     * NOTE: if the app would like to override some options to start the Activity,
     * use {@link #startAssistantActivity(Intent, Bundle)} instead.
     */
    public void startAssistantActivity(Intent intent) {
        startAssistantActivity(intent, ActivityOptions.makeBasic().toBundle());
    }

    /**
     * <p>Ask that a new assistant activity be started.  This will create a new task in the
     * in activity manager: this means that
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK Intent.FLAG_ACTIVITY_NEW_TASK}
     * will be set for you to make it a new task.</p>
     *
     * <p>The newly started activity will be displayed on top of other activities in the system
     * in a new layer that is not affected by multi-window mode.  Tasks started from this activity
     * will go into the normal activity layer and not this new layer.</p>
     *
     * <p>By default, the system will create a window for the UI for this session.  If you are using
     * an assistant activity instead, then you can disable the window creation by calling
     * {@link #setUiEnabled} in {@link #onPrepareShow(Bundle, int)}.</p>
     *
     * @param intent the intent used to start an assistant activity
     * @param bundle Additional options for how the Activity should be started. See
     * {@link ActivityOptions} for how to build the Bundle supplied here.
     */
    public void startAssistantActivity(@NonNull Intent intent, @NonNull Bundle bundle) {
        Objects.requireNonNull(bundle);
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            intent.migrateExtraStreamToClipData(mContext);
            intent.prepareToLeaveProcess(mContext);
            int res = mSystemService.startAssistantActivity(mToken, intent,
                    intent.resolveType(mContext.getContentResolver()),
                    mContext.getAttributionTag(), bundle);
            Instrumentation.checkStartActivityResult(res, intent);
        } catch (RemoteException e) {
        }
    }

    /**
     * Requests a list of supported actions from an app.
     *
     * @param activityId Ths activity id of the app to get the actions from.
     * @param cancellationSignal A signal to cancel the operation in progress,
     *     or {@code null} if none.
     * @param resultExecutor The handler to receive the callback.
     * @param callback The callback to receive the response.
     */
    public final void requestDirectActions(@NonNull ActivityId activityId,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull @CallbackExecutor Executor resultExecutor,
            @NonNull Consumer<List<DirectAction>> callback) {
        Objects.requireNonNull(activityId);
        Objects.requireNonNull(resultExecutor);
        Objects.requireNonNull(callback);
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }

        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }

        final RemoteCallback cancellationCallback = (cancellationSignal != null)
                ? new RemoteCallback(b -> {
                    if (b != null) {
                        final IBinder cancellation = b.getBinder(
                                VoiceInteractor.KEY_CANCELLATION_SIGNAL);
                        if (cancellation != null) {
                            cancellationSignal.setRemote(ICancellationSignal.Stub.asInterface(
                                    cancellation));
                        }
                    }
                })
                : null;

        try {
            mSystemService.requestDirectActions(mToken, activityId.getTaskId(),
                    activityId.getAssistToken(), cancellationCallback,
                    new RemoteCallback(createSafeResultListener((result) -> {
                List<DirectAction> list;
                if (result == null) {
                    list = Collections.emptyList();
                } else {
                    final ParceledListSlice<DirectAction> pls = result.getParcelable(
                            DirectAction.KEY_ACTIONS_LIST, android.content.pm.ParceledListSlice.class);
                    if (pls != null) {
                        final List<DirectAction> receivedList = pls.getList();
                        list = (receivedList != null) ? receivedList : Collections.emptyList();
                    } else {
                        list = Collections.emptyList();
                    }
                }
                resultExecutor.execute(() -> callback.accept(list));
            })));
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Called when the direct actions are invalidated.
     */
    public void onDirectActionsInvalidated(@NonNull ActivityId activityId) {

    }

    /**
     * Asks that an action be performed by the app. This will send a request to the app which
     * provided this action.
     *
     * <p> An action could take time to execute and the result is provided asynchronously
     * via a callback. If the action is taking longer and you want to cancel its execution
     * you can pass in a cancellation signal through which to notify the app to abort the
     * action.
     *
     * @param action The action to be performed.
     * @param extras Any optional extras sent to the app as part of the request
     * @param cancellationSignal A signal to cancel the operation in progress,
     *                          or {@code null} if none.
     * @param resultExecutor The handler to receive the callback.
     * @param resultListener The callback to receive the response.
     *
     * @see #requestDirectActions(ActivityId, CancellationSignal, Executor, Consumer)
     * @see Activity#onGetDirectActions(CancellationSignal, Consumer)
     */
    public final void performDirectAction(@NonNull DirectAction action, @Nullable Bundle extras,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull @CallbackExecutor Executor resultExecutor,
            @NonNull Consumer<Bundle> resultListener) {
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        Objects.requireNonNull(resultExecutor);
        Objects.requireNonNull(resultListener);

        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }

        final RemoteCallback cancellationCallback = (cancellationSignal != null)
                ? new RemoteCallback(createSafeResultListener(b -> {
                    if (b != null) {
                        final IBinder cancellation = b.getBinder(
                                VoiceInteractor.KEY_CANCELLATION_SIGNAL);
                        if (cancellation != null) {
                            cancellationSignal.setRemote(ICancellationSignal.Stub.asInterface(
                                    cancellation));
                        }
                    }
                }))
                : null;

        final RemoteCallback resultCallback = new RemoteCallback(createSafeResultListener(b -> {
            if (b != null) {
                resultExecutor.execute(() -> resultListener.accept(b));
            } else {
                resultExecutor.execute(() -> resultListener.accept(Bundle.EMPTY));
            }
        }));

        try {
            mSystemService.performDirectAction(mToken, action.getId(), extras,
                    action.getTaskId(), action.getActivityId(), cancellationCallback,
                    resultCallback);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Set whether this session will keep the device awake while it is running a voice
     * activity.  By default, the system holds a wake lock for it while in this state,
     * so that it can work even if the screen is off.  Setting this to false removes that
     * wake lock, allowing the CPU to go to sleep.  This is typically used if the
     * session decides it has been waiting too long for a response from the user and
     * doesn't want to let this continue to drain the battery.
     *
     * <p>Passing false here will release the wake lock, and you can call later with
     * true to re-acquire it.  It will also be automatically re-acquired for you each
     * time you start a new voice activity task -- that is when you call
     * {@link #startVoiceActivity}.</p>
     */
    public void setKeepAwake(boolean keepAwake) {
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            mSystemService.setKeepAwake(mToken, keepAwake);
        } catch (RemoteException e) {
        }
    }

    /**
     * Request that all system dialogs (and status bar shade etc) be closed, allowing
     * access to the session's UI.  This will <em>not</em> cause the lock screen to be
     * dismissed.
     */
    public void closeSystemDialogs() {
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            mSystemService.closeSystemDialogs(mToken);
        } catch (RemoteException e) {
        }
    }

    /**
     * Convenience for inflating views.
     */
    public LayoutInflater getLayoutInflater() {
        ensureWindowCreated();
        return mInflater;
    }

    /**
     * Retrieve the window being used to show the session's UI.
     */
    public Dialog getWindow() {
        ensureWindowCreated();
        return mWindow;
    }

    /**
     * Finish the session.  This completely destroys the session -- the next time it is shown,
     * an entirely new one will be created.  You do not normally call this function; instead,
     * use {@link #hide} and allow the system to destroy your session if it needs its RAM.
     */
    public void finish() {
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            mSystemService.finish(mToken);
        } catch (RemoteException e) {
        }
    }

    /**
     * Initiatize a new session.  At this point you don't know exactly what this
     * session will be used for; you will find that out in {@link #onShow}.
     */
    public void onCreate() {
        doOnCreate();
    }

    private void doOnCreate() {
        mTheme = mTheme != 0 ? mTheme
                : com.android.internal.R.style.Theme_DeviceDefault_VoiceInteractionSession;
    }

    /**
     * Called prior to {@link #onShow} before any UI setup has occurred.  Not generally useful.
     *
     * @param args The arguments that were supplied to
     * {@link VoiceInteractionService#showSession VoiceInteractionService.showSession}.
     * @param showFlags The show flags originally provided to
     * {@link VoiceInteractionService#showSession VoiceInteractionService.showSession}.
     */
    public void onPrepareShow(Bundle args, int showFlags) {
    }

    /**
     * Called when the session UI is going to be shown.  This is called after
     * {@link #onCreateContentView} (if the session's content UI needed to be created) and
     * immediately prior to the window being shown.  This may be called while the window
     * is already shown, if a show request has come in while it is shown, to allow you to
     * update the UI to match the new show arguments.
     *
     * @param args The arguments that were supplied to
     * {@link VoiceInteractionService#showSession VoiceInteractionService.showSession}.
     * Some example keys include :
     * <ul>
     *     <li>
     *         invocation_type
     *     </li>
     *     <li>
     *         invocation_phone_state
     *     </li>
     *     <li>
     *         {@link #KEY_SHOW_SESSION_ID}
     *     </li>
     *     <li>
     *         invocation_time_ms
     *     </li>
     *     <li>
     *         Intent.EXTRA_TIME ("android.intent.extra.TIME") indicating timing in milliseconds of
     *         the KeyEvent that triggered Assistant
     *     </li>
     *     <li>
     *         Intent.EXTRA_ASSIST_INPUT_DEVICE_ID (android.intent.extra.ASSIST_INPUT_DEVICE_ID)
     *         referring to the device that sent the request
     *     </li>
     *     <li>
     *         {@link #KEY_FOREGROUND_ACTIVITIES} provides foreground activities of up coming
     *         onHandleAssist/onHandleScreenshot calls earlier. This is only populated if session
     *         was requested with {@link VoiceInteractionSession.SHOW_WITH_ASSIST} show flag.
     *     </li>
     *     <li>
     *         Starting from Android 14, the system will add {@link #KEY_SHOW_SESSION_ID}, the
     *         Bundle is not null. But the application should handle null case before Android 14.
     *     </li>
     * </ul>
     *
     * @param showFlags The show flags originally provided to
     * {@link VoiceInteractionService#showSession VoiceInteractionService.showSession}.
     */
    public void onShow(@Nullable Bundle args, int showFlags) {
    }

    /**
     * Called immediately after stopping to show the session UI.
     */
    public void onHide() {
    }

    /**
     * Last callback to the session as it is being finished.
     */
    public void onDestroy() {
    }

    /**
     * Hook in which to create the session's UI.
     */
    public View onCreateContentView() {
        return null;
    }

    public void setContentView(View view) {
        ensureWindowCreated();
        mContentFrame.removeAllViews();
        mContentFrame.addView(view, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mContentFrame.requestApplyInsets();
    }

    void doOnHandleAssist(int taskId, IBinder assistToken, Bundle data, AssistStructure structure,
            Throwable failure, AssistContent content, int index, int count) {
        if (failure != null) {
            onAssistStructureFailure(failure);
        }
        AssistState assistState = new AssistState(new ActivityId(taskId, assistToken),
                data, structure, content, index, count);
        onHandleAssist(assistState);
    }

    /**
     * Called when there has been a failure transferring the {@link AssistStructure} to
     * the assistant.  This may happen, for example, if the data is too large and results
     * in an out of memory exception, the data has been cleared during transferring due to
     * the new incoming assist data, or the client has provided corrupt data. This will be
     * called immediately before {@link #onHandleAssist} and the AssistStructure supplied
     * there afterwards will be null.
     *
     * @param failure The failure exception that was thrown when building the
     * {@link AssistStructure}.
     */
    public void onAssistStructureFailure(Throwable failure) {
    }

    /**
     * Called to receive data from the application that the user was currently viewing when
-     * an assist session is started.  If the original show request did not specify
     * {@link #SHOW_WITH_ASSIST}, this method will not be called.
     *
     * @param data Arbitrary data supplied by the app through
     * {@link android.app.Activity#onProvideAssistData Activity.onProvideAssistData}.
     * May be null if assist data has been disabled by the user or device policy.
     * @param structure If available, the structure definition of all windows currently
     * displayed by the app.  May be null if assist data has been disabled by the user
     * or device policy; will be an empty stub if the application has disabled assist
     * by marking its window as secure.
     * @param content Additional content data supplied by the app through
     * {@link android.app.Activity#onProvideAssistContent Activity.onProvideAssistContent}.
     * May be null if assist data has been disabled by the user or device policy; will
     * not be automatically filled in with data from the app if the app has marked its
     * window as secure.
     *
     * @deprecated use {@link #onHandleAssist(AssistState)}
     */
    @Deprecated
    public void onHandleAssist(@Nullable Bundle data, @Nullable AssistStructure structure,
            @Nullable AssistContent content) {
    }

    /**
     * Called to receive data from the application that the user was currently viewing when
     * an assist session is started. If the original show request did not specify
     * {@link #SHOW_WITH_ASSIST}, {@link AssistState} parameter will only provide
     * {@link ActivityId}. If there was a failure to write the assist data to
     * {@link AssistStructure}, the {@link AssistState#getAssistStructure()} will return null.
     *
     * <p>This method is called for all activities along with an index and count that indicates
     * which activity the data is for. {@code index} will be between 0 and {@code count}-1 and
     * this method is called once for each activity in no particular order. The {@code count}
     * indicates how many activities to expect assist data for, including the top focused one.
     * The focused activity can be determined by calling {@link AssistState#isFocused()}.
     *
     * <p>To be responsive to assist requests, process assist data as soon as it is received,
     * without waiting for all queued activities to return assist data.
     *
     * @param state The state object capturing the state of an activity.
     */
    public void onHandleAssist(@NonNull AssistState state) {
        if (state.getAssistData() == null && state.getAssistStructure() == null
                && state.getAssistContent() == null) {
            return;
        } else if (state.getIndex() == 0) {
            onHandleAssist(state.getAssistData(), state.getAssistStructure(),
                    state.getAssistContent());
        } else {
            onHandleAssistSecondary(state.getAssistData(), state.getAssistStructure(),
                    state.getAssistContent(), state.getIndex(), state.getCount());
        }
    }

    /**
     * Called to receive data from other applications that the user was or is interacting with,
     * that are currently on the screen in a multi-window display environment, not including the
     * currently focused activity. This could be
     * a free-form window, a picture-in-picture window, or another window in a split-screen display.
     * <p>
     * This method is very similar to
     * {@link #onHandleAssist} except that it is called
     * for additional non-focused activities along with an index and count that indicates
     * which additional activity the data is for. {@code index} will be between 1 and
     * {@code count}-1 and this method is called once for each additional window, in no particular
     * order. The {@code count} indicates how many windows to expect assist data for, including the
     * top focused activity, which continues to be returned via {@link #onHandleAssist}.
     * <p>
     * To be responsive to assist requests, process assist data as soon as it is received,
     * without waiting for all queued activities to return assist data.
     *
     * @param data Arbitrary data supplied by the app through
     * {@link android.app.Activity#onProvideAssistData Activity.onProvideAssistData}.
     * May be null if assist data has been disabled by the user or device policy.
     * @param structure If available, the structure definition of all windows currently
     * displayed by the app.  May be null if assist data has been disabled by the user
     * or device policy; will be an empty stub if the application has disabled assist
     * by marking its window as secure.
     * @param content Additional content data supplied by the app through
     * {@link android.app.Activity#onProvideAssistContent Activity.onProvideAssistContent}.
     * May be null if assist data has been disabled by the user or device policy; will
     * not be automatically filled in with data from the app if the app has marked its
     * window as secure.
     * @param index the index of the additional activity that this data
     *        is for.
     * @param count the total number of additional activities for which the assist data is being
     *        returned, including the focused activity that is returned via
     *        {@link #onHandleAssist}.
     *
     * @deprecated use {@link #onHandleAssist(AssistState)}
     */
    @Deprecated
    public void onHandleAssistSecondary(@Nullable Bundle data, @Nullable AssistStructure structure,
            @Nullable AssistContent content, int index, int count) {
    }

    /**
     * Called to receive a screenshot of what the user was currently viewing when an assist
     * session is started.  May be null if screenshots are disabled by the user, policy,
     * or application.  If the original show request did not specify
     * {@link #SHOW_WITH_SCREENSHOT}, this method will not be called.
     */
    public void onHandleScreenshot(@Nullable Bitmap screenshot) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }

    /**
     * Called when the user presses the back button while focus is in the session UI.  Note
     * that this will only happen if the session UI has requested input focus in its window;
     * otherwise, the back key will go to whatever window has focus and do whatever behavior
     * it normally has there.  The default implementation simply calls {@link #hide}.
     */
    public void onBackPressed() {
        hide();
    }

    /**
     * Sessions automatically watch for requests that all system UI be closed (such as when
     * the user presses HOME), which will appear here.  The default implementation always
     * calls {@link #hide}.
     */
    public void onCloseSystemDialogs() {
        hide();
    }

    /**
     * Called when the lockscreen was shown.
     */
    public void onLockscreenShown() {
        hide();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }

    /**
     * Compute the interesting insets into your UI.  The default implementation
     * sets {@link Insets#contentInsets outInsets.contentInsets.top} to the height
     * of the window, meaning it should not adjust content underneath.  The default touchable
     * insets are {@link Insets#TOUCHABLE_INSETS_FRAME}, meaning it consumes all touch
     * events within its window frame.
     *
     * @param outInsets Fill in with the current UI insets.
     */
    public void onComputeInsets(Insets outInsets) {
        outInsets.contentInsets.left = 0;
        outInsets.contentInsets.bottom = 0;
        outInsets.contentInsets.right = 0;
        View decor = getWindow().getWindow().getDecorView();
        outInsets.contentInsets.top = decor.getHeight();
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME;
        outInsets.touchableRegion.setEmpty();
    }

    /**
     * Called when a task initiated by {@link #startVoiceActivity(android.content.Intent)}
     * has actually started.
     *
     * @param intent The original {@link Intent} supplied to
     * {@link #startVoiceActivity(android.content.Intent)}.
     * @param taskId Unique ID of the now running task.
     */
    public void onTaskStarted(Intent intent, int taskId) {
    }

    /**
     * Called when the last activity of a task initiated by
     * {@link #startVoiceActivity(android.content.Intent)} has finished.  The default
     * implementation calls {@link #finish()} on the assumption that this represents
     * the completion of a voice action.  You can override the implementation if you would
     * like a different behavior.
     *
     * @param intent The original {@link Intent} supplied to
     * {@link #startVoiceActivity(android.content.Intent)}.
     * @param taskId Unique ID of the finished task.
     */
    public void onTaskFinished(Intent intent, int taskId) {
        hide();
    }

    /**
     * Request to query for what extended commands the session supports.
     *
     * @param commands An array of commands that are being queried.
     * @return Return an array of booleans indicating which of each entry in the
     * command array is supported.  A true entry in the array indicates the command
     * is supported; false indicates it is not.  The default implementation returns
     * an array of all false entries.
     */
    public boolean[] onGetSupportedCommands(String[] commands) {
        return new boolean[commands.length];
    }

    /**
     * Request to confirm with the user before proceeding with an unrecoverable operation,
     * corresponding to a {@link android.app.VoiceInteractor.ConfirmationRequest
     * VoiceInteractor.ConfirmationRequest}.
     *
     * @param request The active request.
     */
    public void onRequestConfirmation(ConfirmationRequest request) {
    }

    /**
     * Request for the user to pick one of N options, corresponding to a
     * {@link android.app.VoiceInteractor.PickOptionRequest VoiceInteractor.PickOptionRequest}.
     *
     * @param request The active request.
     */
    public void onRequestPickOption(PickOptionRequest request) {
    }

    /**
     * Request to complete the voice interaction session because the voice activity successfully
     * completed its interaction using voice.  Corresponds to
     * {@link android.app.VoiceInteractor.CompleteVoiceRequest
     * VoiceInteractor.CompleteVoiceRequest}.  The default implementation just sends an empty
     * confirmation back to allow the activity to exit.
     *
     * @param request The active request.
     */
    public void onRequestCompleteVoice(CompleteVoiceRequest request) {
    }

    /**
     * Request to abort the voice interaction session because the voice activity can not
     * complete its interaction using voice.  Corresponds to
     * {@link android.app.VoiceInteractor.AbortVoiceRequest
     * VoiceInteractor.AbortVoiceRequest}.  The default implementation just sends an empty
     * confirmation back to allow the activity to exit.
     *
     * @param request The active request.
     */
    public void onRequestAbortVoice(AbortVoiceRequest request) {
    }

    /**
     * Process an arbitrary extended command from the caller,
     * corresponding to a {@link android.app.VoiceInteractor.CommandRequest
     * VoiceInteractor.CommandRequest}.
     *
     * @param request The active request.
     */
    public void onRequestCommand(CommandRequest request) {
    }

    /**
     * Called when the {@link android.app.VoiceInteractor} has asked to cancel a {@link Request}
     * that was previously delivered to {@link #onRequestConfirmation},
     * {@link #onRequestPickOption}, {@link #onRequestCompleteVoice}, {@link #onRequestAbortVoice},
     * or {@link #onRequestCommand}.
     *
     * @param request The request that is being canceled.
     */
    public void onCancelRequest(Request request) {
    }

    /**
     * Registers a callback that will be notified when visible activities have been changed.
     *
     * Note: The {@link VisibleActivityCallback#onVisible(VisibleActivityInfo)} will be called
     * immediately with current visible activities when the callback is registered for the first
     * time. If the callback is already registered, this method does nothing.
     *
     * @param executor The executor which will be used to invoke the callback.
     * @param callback The callback to receive the response.
     *
     * @throws IllegalStateException if calling this method before onCreate().
     */
    public final void registerVisibleActivityCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull VisibleActivityCallback callback) {
        if (DEBUG) {
            Log.d(TAG, "registerVisibleActivityCallback");
        }
        if (mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        mHandlerCaller.sendMessage(
                mHandlerCaller.obtainMessageOO(MSG_REGISTER_VISIBLE_ACTIVITY_CALLBACK, executor,
                        callback));
    }

    /**
     * Unregisters the callback.
     *
     * @param callback The callback to receive the response.
     */
    public final void unregisterVisibleActivityCallback(@NonNull VisibleActivityCallback callback) {
        if (DEBUG) {
            Log.d(TAG, "unregisterVisibleActivityCallback");
        }
        Objects.requireNonNull(callback);

        mHandlerCaller.sendMessage(
                mHandlerCaller.obtainMessageO(MSG_UNREGISTER_VISIBLE_ACTIVITY_CALLBACK, callback));
    }

    /**
     * Print the Service's state into the given stream.  This gets invoked by
     * {@link VoiceInteractionSessionService} when its Service
     * {@link android.app.Service#dump} method is called.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state.  This will be
     * closed for you after you return.
     * @param args additional arguments to the dump request.
     */
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.print(prefix); writer.print("mToken="); writer.println(mToken);
        writer.print(prefix); writer.print("mTheme=#"); writer.println(Integer.toHexString(mTheme));
        writer.print(prefix); writer.print("mUiEnabled="); writer.println(mUiEnabled);
        writer.print(" mInitialized="); writer.println(mInitialized);
        writer.print(prefix); writer.print("mWindowAdded="); writer.print(mWindowAdded);
        writer.print(" mWindowVisible="); writer.println(mWindowVisible);
        writer.print(prefix); writer.print("mWindowWasVisible="); writer.print(mWindowWasVisible);
        writer.print(" mInShowWindow="); writer.println(mInShowWindow);
        if (mActiveRequests.size() > 0) {
            writer.print(prefix); writer.println("Active requests:");
            String innerPrefix = prefix + "    ";
            for (int i=0; i<mActiveRequests.size(); i++) {
                Request req = mActiveRequests.valueAt(i);
                writer.print(prefix); writer.print("  #"); writer.print(i);
                writer.print(": ");
                writer.println(req);
                req.dump(innerPrefix, fd, writer, args);

            }
        }
    }

    private SafeResultListener createSafeResultListener(
            @NonNull Consumer<Bundle> consumer) {
        synchronized (this) {
            final SafeResultListener listener = new SafeResultListener(consumer, this);
            mRemoteCallbacks.put(listener, consumer);
            return listener;
        }
    }

    private Consumer<Bundle> removeSafeResultListener(@NonNull SafeResultListener listener) {
        synchronized (this) {
            return mRemoteCallbacks.remove(listener);
        }
    }

    /**
     * Callback interface for receiving visible activity changes used for assistant usage.
     */
    public interface VisibleActivityCallback {
        /** Callback to inform that an activity has become visible. */
        default void onVisible(@NonNull VisibleActivityInfo activityInfo) {}

        /** Callback to inform that a visible activity has gone. */
        default void onInvisible(@NonNull ActivityId activityId) {}
    }

    /**
     * Represents assist state captured when this session was started.
     * It contains the various assist data objects and a reference to
     * the source activity.
     */
    @Immutable
    public static final class AssistState {
        private final @NonNull ActivityId mActivityId;
        private final int mIndex;
        private final int mCount;
        private final @Nullable Bundle mData;
        private final @Nullable AssistStructure mStructure;
        private final @Nullable AssistContent mContent;

        AssistState(@NonNull ActivityId activityId, @Nullable Bundle data,
                @Nullable AssistStructure structure, @Nullable AssistContent content,
                int index, int count) {
            mActivityId = activityId;
            mIndex = index;
            mCount = count;
            mData = data;
            mStructure = structure;
            mContent = content;
        }

        /**
         * @return whether the source activity is focused.
         */
        public boolean isFocused() {
            return mIndex == 0;
        }

        /**
         * @return the index of the activity that this state is for or -1
         *     if there was no assist data captured.
         */
        public @IntRange(from = -1) int getIndex() {
            return mIndex;
        }

        /**
         * @return the total number of activities for which the assist data is
         * being returned.
         */
        public @IntRange(from = 0) int getCount() {
            return mCount;
        }

        /**
         * @return the id of the source activity
         */
        public @NonNull ActivityId getActivityId() {
            return mActivityId;
        }

        /**
         * @return Arbitrary data supplied by the app through
         * {@link android.app.Activity#onProvideAssistData Activity.onProvideAssistData}.
         * May be null if assist data has been disabled by the user or device policy; will be null
         * if the original show request did not specify {@link #SHOW_WITH_ASSIST}.
         */
        public @Nullable Bundle getAssistData() {
            return mData;
        }

        /**
         * @return If available, the structure definition of all windows currently
         * displayed by the app. May be null if assist data has been disabled by the user
         * or device policy; will be null if the original show request did not specify
         * {@link #SHOW_WITH_ASSIST} or the assist data has been corrupt when writing the data to
         * {@link AssistStructure}; will be an empty stub if the application has disabled assist
         * by marking its window as secure.
         */
        public @Nullable AssistStructure getAssistStructure() {
            return mStructure;
        }

        /**
         * @return Additional content data supplied by the app through
         * {@link android.app.Activity#onProvideAssistContent Activity.onProvideAssistContent}.
         * May be null if assist data has been disabled by the user or device policy; will be null
         * if the original show request did not specify {@link #SHOW_WITH_ASSIST}. Will not be
         * automatically filled in with data from the app if the app has marked its window as
         * secure.
         */
        public @Nullable AssistContent getAssistContent() {
            return mContent;
        }
    }

    /**
     * Represents the id of an assist source activity. You can use
     * {@link #equals(Object)} to compare instances of this class.
     */
    public static class ActivityId {
        private final int mTaskId;
        private final IBinder mAssistToken;

        ActivityId(int taskId, IBinder assistToken) {
            mTaskId = taskId;
            mAssistToken = assistToken;
        }

        /** @hide */
        @TestApi
        public int getTaskId() {
            return mTaskId;
        }

        /** @hide */
        @TestApi
        @NonNull public IBinder getAssistToken() {
            return mAssistToken;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ActivityId that = (ActivityId) o;

            if (mTaskId != that.mTaskId) {
                return false;
            }
            return mAssistToken != null
                    ? mAssistToken.equals(that.mAssistToken)
                    : that.mAssistToken == null;
        }

        @Override
        public int hashCode() {
            int result = mTaskId;
            result = 31 * result + (mAssistToken != null ? mAssistToken.hashCode() : 0);
            return result;
        }
    }

    private static class SafeResultListener implements RemoteCallback.OnResultListener {
        private final @NonNull WeakReference<VoiceInteractionSession> mWeakSession;

        SafeResultListener(@NonNull Consumer<Bundle> action,
                @NonNull VoiceInteractionSession session) {
            mWeakSession = new WeakReference<>(session);
        }

        @Override
        public void onResult(Bundle result) {
            final VoiceInteractionSession session = mWeakSession.get();
            if (session != null) {
                final Consumer<Bundle> consumer = session.removeSafeResultListener(this);
                if (consumer != null) {
                    consumer.accept(result);
                }
            }
        }
    }
}
