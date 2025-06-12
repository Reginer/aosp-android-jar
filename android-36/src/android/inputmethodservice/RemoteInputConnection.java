/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.inputmethodservice;

import android.annotation.AnyThread;
import android.annotation.CallbackExecutor;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.HandwritingGesture;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.PreviewableHandwritingGesture;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.TextAttribute;
import android.view.inputmethod.TextBoundsInfoResult;

import com.android.internal.inputmethod.CancellationGroup;
import com.android.internal.inputmethod.CompletableFutureUtil;
import com.android.internal.inputmethod.IRemoteInputConnection;
import com.android.internal.inputmethod.ImeTracing;
import com.android.internal.inputmethod.InputConnectionProtoDumper;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Takes care of remote method invocations of {@link InputConnection} in the IME side.
 *
 * <p>This class works as a proxy to forward API calls on {@link InputConnection} to
 * {@link android.view.inputmethod.RemoteInputConnectionImpl} running on the IME client
 * (editor app) process then waits replies as needed.</p>
 *
 * <p>See also {@link IRemoteInputConnection} for the actual {@link android.os.Binder} IPC protocols
 * under the hood.</p>
 */
final class RemoteInputConnection implements InputConnection {
    private static final String TAG = "RemoteInputConnection";

    private static final int MAX_WAIT_TIME_MILLIS = 2000;

    @NonNull
    private final IRemoteInputConnectionInvoker mInvoker;

    private static final class InputMethodServiceInternalHolder {
        @NonNull
        private final WeakReference<InputMethodServiceInternal> mServiceRef;

        private InputMethodServiceInternalHolder(
                @NonNull WeakReference<InputMethodServiceInternal> ims) {
            mServiceRef = ims;
        }

        @AnyThread
        @Nullable
        public InputMethodServiceInternal getAndWarnIfNull() {
            final InputMethodServiceInternal ims = mServiceRef.get();
            if (ims == null) {
                Log.e(TAG, "InputMethodService is already destroyed.  InputConnection instances"
                        + " cannot be used beyond InputMethodService lifetime.", new Throwable());
            }
            return ims;
        }
    }

    @NonNull
    private final InputMethodServiceInternalHolder mImsInternal;

    /**
     * Signaled when the system decided to take away IME focus from the target app.
     *
     * <p>This is expected to be signaled immediately when the IME process receives
     * {@link com.android.internal.inputmethod.IInputMethod#unbindInput()}.</p>
     */
    @NonNull
    private final CancellationGroup mCancellationGroup;

    RemoteInputConnection(
            @NonNull WeakReference<InputMethodServiceInternal> inputMethodService,
            IRemoteInputConnection inputConnection, @NonNull CancellationGroup cancellationGroup) {
        mImsInternal = new InputMethodServiceInternalHolder(inputMethodService);
        mInvoker = IRemoteInputConnectionInvoker.create(inputConnection);
        mCancellationGroup = cancellationGroup;
    }

    @AnyThread
    public boolean isSameConnection(@NonNull IRemoteInputConnection inputConnection) {
        return mInvoker.isSameConnection(inputConnection);
    }

    RemoteInputConnection(@NonNull RemoteInputConnection original, int sessionId) {
        mImsInternal = original.mImsInternal;
        mInvoker = original.mInvoker.cloneWithSessionId(sessionId);
        mCancellationGroup = original.mCancellationGroup;
    }

    /**
     * See {@link InputConnection#getTextAfterCursor(int, int)}.
     */
    @Nullable
    @AnyThread
    public CharSequence getTextAfterCursor(@IntRange(from = 0) int length, int flags) {
        if (length < 0) {
            // TODO: Should we throw an InvalidParameterException() based on targetSdkVersion?
            Log.e(TAG, "length=" + length + " is invalid and always results in null result.");
        }
        if (mCancellationGroup.isCanceled()) {
            return null;
        }

        final CompletableFuture<CharSequence> value = mInvoker.getTextAfterCursor(length, flags);
        final CharSequence result = CompletableFutureUtil.getResultOrNull(
                value, TAG, "getTextAfterCursor()", mCancellationGroup, MAX_WAIT_TIME_MILLIS);

        final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
        if (imsInternal != null && ImeTracing.getInstance().isEnabled()) {
            final byte[] icProto = InputConnectionProtoDumper.buildGetTextAfterCursorProto(length,
                    flags, result);
            imsInternal.triggerServiceDump(TAG + "#getTextAfterCursor", icProto);
        }

        return result;
    }

    /**
     * See {@link InputConnection#getTextBeforeCursor(int, int)}.
     */
    @Nullable
    @AnyThread
    public CharSequence getTextBeforeCursor(@IntRange(from = 0) int length, int flags) {
        if (length < 0) {
            // TODO: Should we throw an InvalidParameterException() based on targetSdkVersion?
            Log.e(TAG, "length=" + length + " is invalid and always results in null result.");
        }
        if (mCancellationGroup.isCanceled()) {
            return null;
        }

        final CompletableFuture<CharSequence> value = mInvoker.getTextBeforeCursor(length, flags);
        final CharSequence result = CompletableFutureUtil.getResultOrNull(
                value, TAG, "getTextBeforeCursor()", mCancellationGroup, MAX_WAIT_TIME_MILLIS);

        final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
        if (imsInternal != null && ImeTracing.getInstance().isEnabled()) {
            final byte[] icProto = InputConnectionProtoDumper.buildGetTextBeforeCursorProto(length,
                    flags, result);
            imsInternal.triggerServiceDump(TAG + "#getTextBeforeCursor", icProto);
        }

        return result;
    }

    @AnyThread
    public CharSequence getSelectedText(int flags) {
        if (mCancellationGroup.isCanceled()) {
            return null;
        }

        final CompletableFuture<CharSequence> value = mInvoker.getSelectedText(flags);
        final CharSequence result = CompletableFutureUtil.getResultOrNull(
                value, TAG, "getSelectedText()", mCancellationGroup, MAX_WAIT_TIME_MILLIS);

        final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
        if (imsInternal != null && ImeTracing.getInstance().isEnabled()) {
            final byte[] icProto = InputConnectionProtoDumper.buildGetSelectedTextProto(flags,
                    result);
            imsInternal.triggerServiceDump(TAG + "#getSelectedText", icProto);
        }

        return result;
    }

    /**
     * Get {@link SurroundingText} around the current cursor, with <var>beforeLength</var>
     * characters of text before the cursor, <var>afterLength</var> characters of text after the
     * cursor, and all of the selected text.
     * @param beforeLength The expected length of the text before the cursor
     * @param afterLength The expected length of the text after the cursor
     * @param flags Supplies additional options controlling how the text is returned. May be either
     *              0 or {@link #GET_TEXT_WITH_STYLES}.
     * @return the surrounding text around the cursor position; the length of the returned text
     * might be less than requested.  It could also be {@code null} when the editor or system could
     * not support this protocol.
     */
    @AnyThread
    public SurroundingText getSurroundingText(
            @IntRange(from = 0) int beforeLength, @IntRange(from = 0) int afterLength, int flags) {
        if (beforeLength < 0) {
            // TODO: Should we throw an InvalidParameterException() based on targetSdkVersion?
            Log.e(TAG, "beforeLength=" + beforeLength
                    + " is invalid and always results in null result.");
        }
        if (afterLength < 0) {
            // TODO: Should we throw an InvalidParameterException() based on targetSdkVersion?
            Log.e(TAG, "afterLength=" + afterLength
                    + " is invalid and always results in null result.");
        }
        if (mCancellationGroup.isCanceled()) {
            return null;
        }

        final CompletableFuture<SurroundingText> value = mInvoker.getSurroundingText(beforeLength,
                afterLength, flags);
        final SurroundingText result = CompletableFutureUtil.getResultOrNull(
                value, TAG, "getSurroundingText()", mCancellationGroup, MAX_WAIT_TIME_MILLIS);

        final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
        if (imsInternal != null && ImeTracing.getInstance().isEnabled()) {
            final byte[] icProto = InputConnectionProtoDumper.buildGetSurroundingTextProto(
                    beforeLength, afterLength, flags, result);
            imsInternal.triggerServiceDump(TAG + "#getSurroundingText", icProto);
        }

        return result;
    }

    @AnyThread
    public int getCursorCapsMode(int reqModes) {
        if (mCancellationGroup.isCanceled()) {
            return 0;
        }

        final CompletableFuture<Integer> value = mInvoker.getCursorCapsMode(reqModes);
        final int result = CompletableFutureUtil.getResultOrZero(
                value, TAG, "getCursorCapsMode()", mCancellationGroup, MAX_WAIT_TIME_MILLIS);

        final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
        if (imsInternal != null && ImeTracing.getInstance().isEnabled()) {
            final byte[] icProto = InputConnectionProtoDumper.buildGetCursorCapsModeProto(
                    reqModes, result);
            imsInternal.triggerServiceDump(TAG + "#getCursorCapsMode", icProto);
        }

        return result;
    }

    @AnyThread
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        if (mCancellationGroup.isCanceled()) {
            return null;
        }

        final CompletableFuture<ExtractedText> value = mInvoker.getExtractedText(request, flags);
        final ExtractedText result = CompletableFutureUtil.getResultOrNull(
                value, TAG, "getExtractedText()", mCancellationGroup, MAX_WAIT_TIME_MILLIS);

        final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
        if (imsInternal != null && ImeTracing.getInstance().isEnabled()) {
            final byte[] icProto = InputConnectionProtoDumper.buildGetExtractedTextProto(
                    request, flags, result);
            imsInternal.triggerServiceDump(TAG + "#getExtractedText", icProto);
        }

        return result;
    }

    @AnyThread
    public boolean commitText(CharSequence text, int newCursorPosition) {
        final boolean handled = mInvoker.commitText(text, newCursorPosition);
        if (handled) {
            notifyUserActionIfNecessary();
        }
        return handled;
    }

    @AnyThread
    public boolean commitText(@NonNull CharSequence text, int newCursorPosition,
            @Nullable TextAttribute textAttribute) {
        final boolean handled =
                mInvoker.commitText(text, newCursorPosition, textAttribute);
        if (handled) {
            notifyUserActionIfNecessary();
        }
        return handled;
    }

    @AnyThread
    private void notifyUserActionIfNecessary() {
        final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
        if (imsInternal == null) {
            return;
        }
        imsInternal.notifyUserActionIfNecessary();
    }

    @AnyThread
    public boolean commitCompletion(CompletionInfo text) {
        return mInvoker.commitCompletion(text);
    }

    @AnyThread
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        return mInvoker.commitCorrection(correctionInfo);
    }

    @AnyThread
    public boolean setSelection(int start, int end) {
        return mInvoker.setSelection(start, end);
    }

    @AnyThread
    public boolean performEditorAction(int actionCode) {
        return mInvoker.performEditorAction(actionCode);
    }

    @AnyThread
    public boolean performContextMenuAction(int id) {
        return mInvoker.performContextMenuAction(id);
    }

    @AnyThread
    public boolean setComposingRegion(int start, int end) {
        return mInvoker.setComposingRegion(start, end);
    }

    @AnyThread
    public boolean setComposingRegion(int start, int end, @Nullable TextAttribute textAttribute) {
        return mInvoker.setComposingRegion(start, end, textAttribute);
    }

    @AnyThread
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        final boolean handled = mInvoker.setComposingText(text, newCursorPosition);
        if (handled) {
            notifyUserActionIfNecessary();
        }
        return handled;
    }

    @AnyThread
    public boolean setComposingText(CharSequence text, int newCursorPosition,
            @Nullable TextAttribute textAttribute) {
        final boolean handled = mInvoker.setComposingText(text, newCursorPosition, textAttribute);
        if (handled) {
            notifyUserActionIfNecessary();
        }
        return handled;
    }

    @AnyThread
    public boolean finishComposingText() {
        return mInvoker.finishComposingText();
    }

    @AnyThread
    public boolean beginBatchEdit() {
        return mInvoker.beginBatchEdit();
    }

    @AnyThread
    public boolean endBatchEdit() {
        return mInvoker.endBatchEdit();
    }

    @AnyThread
    public boolean sendKeyEvent(KeyEvent event) {
        final boolean handled = mInvoker.sendKeyEvent(event);
        if (handled) {
            notifyUserActionIfNecessary();
        }
        return handled;
    }

    @AnyThread
    public boolean clearMetaKeyStates(int states) {
        return mInvoker.clearMetaKeyStates(states);
    }

    @AnyThread
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        return mInvoker.deleteSurroundingText(beforeLength, afterLength);
    }

    @AnyThread
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        return mInvoker.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
    }

    @AnyThread
    public boolean reportFullscreenMode(boolean enabled) {
        // Nothing should happen when called from input method.
        return false;
    }

    @AnyThread
    public boolean performSpellCheck() {
        return mInvoker.performSpellCheck();
    }

    @AnyThread
    public boolean performPrivateCommand(String action, Bundle data) {
        return mInvoker.performPrivateCommand(action, data);
    }

    @AnyThread
    public void performHandwritingGesture(
            @NonNull HandwritingGesture gesture, @Nullable @CallbackExecutor Executor executor,
            @Nullable IntConsumer consumer) {
        mInvoker.performHandwritingGesture(gesture, executor, consumer);
    }

    @AnyThread
    public boolean previewHandwritingGesture(
            @NonNull PreviewableHandwritingGesture gesture,
            @Nullable CancellationSignal cancellationSignal) {
        if (cancellationSignal != null && cancellationSignal.isCanceled()) {
            return false; // cancelled.
        }

        return mInvoker.previewHandwritingGesture(gesture, cancellationSignal);
    }

    @AnyThread
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        if (mCancellationGroup.isCanceled()) {
            return false;
        }

        final InputMethodServiceInternal ims = mImsInternal.getAndWarnIfNull();
        if (ims == null) {
            return false;
        }

        final int displayId = ims.getContext().getDisplayId();
        final CompletableFuture<Boolean> value =
                mInvoker.requestCursorUpdates(cursorUpdateMode, displayId);
        return CompletableFutureUtil.getResultOrFalse(value, TAG, "requestCursorUpdates()",
                mCancellationGroup, MAX_WAIT_TIME_MILLIS);
    }

    @Override
    @AnyThread
    public boolean requestCursorUpdates(@CursorUpdateMode int cursorUpdateMode,
            @CursorUpdateFilter int cursorUpdateFilter) {
        if (mCancellationGroup.isCanceled()) {
            return false;
        }

        final InputMethodServiceInternal ims = mImsInternal.getAndWarnIfNull();
        if (ims == null) {
            return false;
        }

        final int displayId = ims.getContext().getDisplayId();
        final CompletableFuture<Boolean> value =
                mInvoker.requestCursorUpdates(cursorUpdateMode, cursorUpdateFilter, displayId);
        return CompletableFutureUtil.getResultOrFalse(value, TAG, "requestCursorUpdates()",
                mCancellationGroup, MAX_WAIT_TIME_MILLIS);
    }

    @AnyThread
    public void requestTextBoundsInfo(
            @NonNull RectF bounds, @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<TextBoundsInfoResult> consumer) {
        mInvoker.requestTextBoundsInfo(bounds, executor, consumer);
    }

    @AnyThread
    public Handler getHandler() {
        // Nothing should happen when called from input method.
        return null;
    }

    @AnyThread
    public void closeConnection() {
        // Nothing should happen when called from input method.
    }

    @AnyThread
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
        if (mCancellationGroup.isCanceled()) {
            return false;
        }

        if ((flags & InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
            final InputMethodServiceInternal imsInternal = mImsInternal.getAndWarnIfNull();
            if (imsInternal == null) {
                return false;
            }
            imsInternal.exposeContent(inputContentInfo, this);
        }

        final CompletableFuture<Boolean> value =
                mInvoker.commitContent(inputContentInfo, flags, opts);
        return CompletableFutureUtil.getResultOrFalse(
                value, TAG, "commitContent()", mCancellationGroup, MAX_WAIT_TIME_MILLIS);
    }

    /**
     * See {@link InputConnection#setImeConsumesInput(boolean)}.
     */
    @AnyThread
    public boolean setImeConsumesInput(boolean imeConsumesInput) {
        return mInvoker.setImeConsumesInput(imeConsumesInput);
    }

    /** See {@link InputConnection#replaceText(int, int, CharSequence, int, TextAttribute)}. */
    @AnyThread
    public boolean replaceText(
            int start,
            int end,
            @NonNull CharSequence text,
            int newCursorPosition,
            @Nullable TextAttribute textAttribute) {
        return mInvoker.replaceText(start, end, text, newCursorPosition, textAttribute);
    }

    @AnyThread
    @Override
    public String toString() {
        return "RemoteInputConnection{idHash=#"
                + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}
