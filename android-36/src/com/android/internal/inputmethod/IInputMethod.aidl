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

package com.android.internal.inputmethod;

import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.InputChannel;
import android.view.MotionEvent;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ImeTracker;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodSubtype;
import android.window.ImeOnBackInvokedDispatcher;
import com.android.internal.inputmethod.IConnectionlessHandwritingCallback;
import com.android.internal.inputmethod.IInlineSuggestionsRequestCallback;
import com.android.internal.inputmethod.IInputMethodPrivilegedOperations;
import com.android.internal.inputmethod.IInputMethodSession;
import com.android.internal.inputmethod.IInputMethodSessionCallback;
import com.android.internal.inputmethod.IRemoteInputConnection;
import com.android.internal.inputmethod.InlineSuggestionsRequestInfo;

/**
 * Top-level interface to an input method component (implemented in a Service).
 */
oneway interface IInputMethod {

    parcelable InitParams {
        IBinder token;
        IInputMethodPrivilegedOperations privilegedOperations;
        int navigationBarFlags;
    }

    void initializeInternal(in InitParams params);

    void onCreateInlineSuggestionsRequest(in InlineSuggestionsRequestInfo requestInfo,
            in IInlineSuggestionsRequestCallback cb);

    void bindInput(in InputBinding binding);

    void unbindInput();

    parcelable StartInputParams {
        IBinder startInputToken;
        IRemoteInputConnection remoteInputConnection;
        EditorInfo editorInfo;
        boolean restarting;
        int navigationBarFlags;
        ImeOnBackInvokedDispatcher imeDispatcher;
    }

    void startInput(in StartInputParams params);

    void onNavButtonFlagsChanged(int navButtonFlags);

    void createSession(in InputChannel channel, IInputMethodSessionCallback callback);

    void setSessionEnabled(IInputMethodSession session, boolean enabled);

    void showSoftInput(in IBinder showInputToken, in ImeTracker.Token statsToken, int flags,
            in ResultReceiver resultReceiver);

    void hideSoftInput(in IBinder hideInputToken, in ImeTracker.Token statsToken, int flags,
            in ResultReceiver resultReceiver);

    void updateEditorToolType(int toolType);

    void changeInputMethodSubtype(in InputMethodSubtype subtype);

    void canStartStylusHandwriting(int requestId,
            in IConnectionlessHandwritingCallback connectionlessCallback,
            in CursorAnchorInfo cursorAnchorInfo, boolean isConnectionlessForDelegation);

    void startStylusHandwriting(int requestId, in InputChannel channel,
            in List<MotionEvent> events);

    void commitHandwritingDelegationTextIfAvailable();

    void discardHandwritingDelegationText();

    void initInkWindow();

    void finishStylusHandwriting();

    void removeStylusHandwritingWindow();

    void setStylusWindowIdleTimeoutForTest(long timeout);
}
