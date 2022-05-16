/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.content;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Handler;

public class ClipboardManager extends android.text.ClipboardManager {

    /**
     * Defines a listener callback that is invoked when the primary clip on the clipboard changes.
     * Objects that want to register a listener call
     * {@link android.content.ClipboardManager#addPrimaryClipChangedListener(OnPrimaryClipChangedListener)
     * addPrimaryClipChangedListener()} with an
     * object that implements OnPrimaryClipChangedListener.
     *
     */
    public interface OnPrimaryClipChangedListener {

        /**
         * Callback that is invoked by {@link android.content.ClipboardManager} when the primary
         * clip changes.
         */
        void onPrimaryClipChanged();
    }

    /** {@hide} */
    public ClipboardManager(Context context, Handler handler) { }

    /**
     * Sets the current primary clip on the clipboard.  This is the clip that
     * is involved in normal cut and paste operations.
     *
     * @param clip The clipped data item to set.
     * @see #getPrimaryClip()
     * @see #clearPrimaryClip()
     */
    public void setPrimaryClip(@NonNull ClipData clip) { }

    /**
     * Clears any current primary clip on the clipboard.
     *
     * @see #setPrimaryClip(ClipData)
     */
    public void clearPrimaryClip() { }

    /**
     * Returns the current primary clip on the clipboard.
     *
     * <em>If the application is not the default IME or does not have input focus this return
     * {@code null}.</em>
     *
     * @see #setPrimaryClip(ClipData)
     */
    public @Nullable ClipData getPrimaryClip() {
        return null;
    }

    /**
     * Returns a description of the current primary clip on the clipboard
     * but not a copy of its data.
     *
     * <em>If the application is not the default IME or does not have input focus this return
     * {@code null}.</em>
     *
     * @see #setPrimaryClip(ClipData)
     */
    public @Nullable ClipDescription getPrimaryClipDescription() {
        return null;
    }

    /**
     * Returns true if there is currently a primary clip on the clipboard.
     *
     * <em>If the application is not the default IME or the does not have input focus this will
     * return {@code false}.</em>
     */
    public boolean hasPrimaryClip() {
        return false;
    }

    public void addPrimaryClipChangedListener(OnPrimaryClipChangedListener what) { }

    public void removePrimaryClipChangedListener(OnPrimaryClipChangedListener what) { }

    /**
     * @deprecated Use {@link #getPrimaryClip()} instead.  This retrieves
     * the primary clip and tries to coerce it to a string.
     */
    @Deprecated
    public CharSequence getText() {
        return null;
    }

    /**
     * @deprecated Use {@link #setPrimaryClip(ClipData)} instead.  This
     * creates a ClippedItem holding the given text and sets it as the
     * primary clip.  It has no label or icon.
     */
    @Deprecated
    public void setText(CharSequence text) { }

    /**
     * @deprecated Use {@link #hasPrimaryClip()} instead.
     */
    @Deprecated
    public boolean hasText() {
        return false;
    }

    void reportPrimaryClipChanged() { }
}
