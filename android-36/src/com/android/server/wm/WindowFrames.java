/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.wm;

import static com.android.server.wm.WindowFramesProto.COMPAT_FRAME;
import static com.android.server.wm.WindowFramesProto.DISPLAY_FRAME;
import static com.android.server.wm.WindowFramesProto.FRAME;
import static com.android.server.wm.WindowFramesProto.PARENT_FRAME;

import android.annotation.NonNull;
import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;

import java.io.PrintWriter;

/**
 * Container class for all the window frames that affect how windows are laid out.
 *
 * TODO(b/111611553): Investigate which frames are still needed and which are duplicates
 */
public class WindowFrames {
    private static final StringBuilder sTmpSB = new StringBuilder();

    /**
     * The frame to be referenced while applying gravity and MATCH_PARENT.
     */
    public final Rect mParentFrame = new Rect();

    /**
     * The bounds that the window should fit.
     */
    public final Rect mDisplayFrame = new Rect();

    /**
     * "Real" frame that the application sees, in display coordinate space.
     */
    final Rect mFrame = new Rect();

    /**
     * The frame used to check if mFrame is changed, e.g., moved or resized.  It will be committed
     * after handling the moving or resizing windows.
     */
    final Rect mLastFrame = new Rect();

    /**
     * mFrame but relative to the parent container.
     */
    final Rect mRelFrame = new Rect();

    /**
     * mLastFrame but relative to the parent container
     */
    final Rect mLastRelFrame = new Rect();

    private boolean mFrameSizeChanged = false;

    // Frame that is scaled to the application's coordinate space when in
    // screen size compatibility mode.
    final Rect mCompatFrame = new Rect();

    /**
     * Whether the parent frame would have been different if there was no display cutout.
     */
    private boolean mParentFrameWasClippedByDisplayCutout;

    boolean mLastForceReportingResized = false;
    boolean mForceReportingResized = false;

    private boolean mContentChanged;

    private boolean mInsetsChanged;

    public void setFrames(Rect parentFrame, Rect displayFrame) {
        mParentFrame.set(parentFrame);
        mDisplayFrame.set(displayFrame);
    }

    public void setParentFrameWasClippedByDisplayCutout(
            boolean parentFrameWasClippedByDisplayCutout) {
        mParentFrameWasClippedByDisplayCutout = parentFrameWasClippedByDisplayCutout;
    }

    boolean parentFrameWasClippedByDisplayCutout() {
        return mParentFrameWasClippedByDisplayCutout;
    }

    /**
     * @return true if the width or height has changed since last updating resizing window.
     */
    boolean didFrameSizeChange() {
        return (mLastFrame.width() != mFrame.width()) || (mLastFrame.height() != mFrame.height());
    }

    /**
     * Updates info about whether the size of the window has changed since last reported.
     *
     * @return true if info about size has changed since last reported.
     */
    boolean setReportResizeHints() {
        mLastForceReportingResized |= mForceReportingResized;
        mFrameSizeChanged |= didFrameSizeChange();
        return mLastForceReportingResized || mFrameSizeChanged;
    }

    /**
     * @return true if the width or height has changed since last reported to the client.
     */
    boolean isFrameSizeChangeReported() {
        return mFrameSizeChanged || didFrameSizeChange();
    }

    /**
     * Resets the size changed flags so they're all set to false again. This should be called
     * after the frames are reported to client.
     */
    void clearReportResizeHints() {
        mLastForceReportingResized = false;
        mFrameSizeChanged = false;
    }

    /**
     * Clears factors that would cause report-resize.
     */
    void onResizeHandled() {
        mForceReportingResized = false;
    }

    /**
     * Forces the next layout pass to update the client.
     */
    void forceReportingResized() {
        mForceReportingResized = true;
    }

    /**
     * Sets whether the content has changed. This means that either the size or parent frame has
     * changed.
     */
    public void setContentChanged(boolean contentChanged) {
        mContentChanged = contentChanged;
    }

    /**
     * @see #setContentChanged(boolean)
     */
    boolean hasContentChanged() {
        return mContentChanged;
    }

    /**
     * Sets whether we need to report {@link android.view.InsetsState} to the client.
     */
    void setInsetsChanged(boolean insetsChanged) {
        mInsetsChanged = insetsChanged;
    }

    /**
     * @see #setInsetsChanged(boolean)
     */
    boolean hasInsetsChanged() {
        return mInsetsChanged;
    }

    public void dumpDebug(@NonNull ProtoOutputStream proto, long fieldId) {
        final long token = proto.start(fieldId);
        mParentFrame.dumpDebug(proto, PARENT_FRAME);
        mDisplayFrame.dumpDebug(proto, DISPLAY_FRAME);
        mFrame.dumpDebug(proto, FRAME);
        mCompatFrame.dumpDebug(proto, COMPAT_FRAME);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "Frames: parent=" + mParentFrame.toShortString(sTmpSB)
                + " display=" + mDisplayFrame.toShortString(sTmpSB)
                + " frame=" + mFrame.toShortString(sTmpSB)
                + " last=" + mLastFrame.toShortString(sTmpSB)
                + " insetsChanged=" + mInsetsChanged);
    }

    String getInsetsChangedInfo() {
        return "forceReportingResized=" + mLastForceReportingResized
                + " insetsChanged=" + mInsetsChanged;
    }
}
