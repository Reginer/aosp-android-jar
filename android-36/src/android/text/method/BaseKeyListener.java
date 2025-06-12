/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.text.method;

import android.graphics.Paint;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.text.Editable;
import android.text.Emoji;
import android.text.InputType;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spanned;
import android.text.method.TextKeyListener.Capitalize;
import android.text.style.ReplacementSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.android.internal.annotations.GuardedBy;

import java.text.BreakIterator;

/**
 * Abstract base class for key listeners.
 *
 * Provides a basic foundation for entering and editing text.
 * Subclasses should override {@link #onKeyDown} and {@link #onKeyUp} to insert
 * characters as keys are pressed.
 * <p></p>
 * As for all implementations of {@link KeyListener}, this class is only concerned
 * with hardware keyboards.  Software input methods have no obligation to trigger
 * the methods in this class.
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public abstract class BaseKeyListener extends MetaKeyKeyListener
        implements KeyListener {
    /* package */ static final Object OLD_SEL_START = new NoCopySpan.Concrete();

    private static final int LINE_FEED = 0x0A;
    private static final int CARRIAGE_RETURN = 0x0D;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    static Paint sCachedPaint = null;

    /**
     * Performs the action that happens when you press the {@link KeyEvent#KEYCODE_DEL} key in
     * a {@link TextView}.  If there is a selection, deletes the selection; otherwise,
     * deletes the character before the cursor, if any; ALT+DEL deletes everything on
     * the line the cursor is on.
     *
     * @return true if anything was deleted; false otherwise.
     */
    public boolean backspace(View view, Editable content, int keyCode, KeyEvent event) {
        return backspaceOrForwardDelete(view, content, keyCode, event, false);
    }

    /**
     * Performs the action that happens when you press the {@link KeyEvent#KEYCODE_FORWARD_DEL}
     * key in a {@link TextView}.  If there is a selection, deletes the selection; otherwise,
     * deletes the character before the cursor, if any; ALT+FORWARD_DEL deletes everything on
     * the line the cursor is on.
     *
     * @return true if anything was deleted; false otherwise.
     */
    public boolean forwardDelete(View view, Editable content, int keyCode, KeyEvent event) {
        return backspaceOrForwardDelete(view, content, keyCode, event, true);
    }

    // Returns true if the given code point is a variation selector.
    private static boolean isVariationSelector(int codepoint) {
        return UCharacter.hasBinaryProperty(codepoint, UProperty.VARIATION_SELECTOR);
    }

    // Returns the offset of the replacement span edge if the offset is inside of the replacement
    // span.  Otherwise, does nothing and returns the input offset value.
    private static int adjustReplacementSpan(CharSequence text, int offset, boolean moveToStart) {
        if (!(text instanceof Spanned)) {
            return offset;
        }

        ReplacementSpan[] spans = ((Spanned) text).getSpans(offset, offset, ReplacementSpan.class);
        for (int i = 0; i < spans.length; i++) {
            final int start = ((Spanned) text).getSpanStart(spans[i]);
            final int end = ((Spanned) text).getSpanEnd(spans[i]);

            if (start < offset && end > offset) {
                offset = moveToStart ? start : end;
            }
        }
        return offset;
    }

    // Returns the start offset to be deleted by a backspace key from the given offset.
    private static int getOffsetForBackspaceKey(CharSequence text, int offset) {
        if (offset <= 1) {
            return 0;
        }

        // Initial state
        final int STATE_START = 0;

        // The offset is immediately before line feed.
        final int STATE_LF = 1;

        // The offset is immediately before a KEYCAP.
        final int STATE_BEFORE_KEYCAP = 2;
        // The offset is immediately before a variation selector and a KEYCAP.
        final int STATE_BEFORE_VS_AND_KEYCAP = 3;

        // The offset is immediately before an emoji modifier.
        final int STATE_BEFORE_EMOJI_MODIFIER = 4;
        // The offset is immediately before a variation selector and an emoji modifier.
        final int STATE_BEFORE_VS_AND_EMOJI_MODIFIER = 5;

        // The offset is immediately before a variation selector.
        final int STATE_BEFORE_VS = 6;

        // The offset is immediately before an emoji.
        final int STATE_BEFORE_EMOJI = 7;
        // The offset is immediately before a ZWJ that were seen before a ZWJ emoji.
        final int STATE_BEFORE_ZWJ = 8;
        // The offset is immediately before a variation selector and a ZWJ that were seen before a
        // ZWJ emoji.
        final int STATE_BEFORE_VS_AND_ZWJ = 9;

        // The number of following RIS code points is odd.
        final int STATE_ODD_NUMBERED_RIS = 10;
        // The number of following RIS code points is even.
        final int STATE_EVEN_NUMBERED_RIS = 11;

        // The offset is in emoji tag sequence.
        final int STATE_IN_TAG_SEQUENCE = 12;

        // The state machine has been stopped.
        final int STATE_FINISHED = 13;

        int deleteCharCount = 0;  // Char count to be deleted by backspace.
        int lastSeenVSCharCount = 0;  // Char count of previous variation selector.

        int state = STATE_START;

        int tmpOffset = offset;
        do {
            final int codePoint = Character.codePointBefore(text, tmpOffset);
            tmpOffset -= Character.charCount(codePoint);

            switch (state) {
                case STATE_START:
                    deleteCharCount = Character.charCount(codePoint);
                    if (codePoint == LINE_FEED) {
                        state = STATE_LF;
                    } else if (isVariationSelector(codePoint)) {
                        state = STATE_BEFORE_VS;
                    } else if (Emoji.isRegionalIndicatorSymbol(codePoint)) {
                        state = STATE_ODD_NUMBERED_RIS;
                    } else if (Emoji.isEmojiModifier(codePoint)) {
                        state = STATE_BEFORE_EMOJI_MODIFIER;
                    } else if (codePoint == Emoji.COMBINING_ENCLOSING_KEYCAP) {
                        state = STATE_BEFORE_KEYCAP;
                    } else if (Emoji.isEmoji(codePoint)) {
                        state = STATE_BEFORE_EMOJI;
                    } else if (codePoint == Emoji.CANCEL_TAG) {
                        state = STATE_IN_TAG_SEQUENCE;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_LF:
                    if (codePoint == CARRIAGE_RETURN) {
                        ++deleteCharCount;
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_ODD_NUMBERED_RIS:
                    if (Emoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount += 2; /* Char count of RIS */
                        state = STATE_EVEN_NUMBERED_RIS;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_EVEN_NUMBERED_RIS:
                    if (Emoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount -= 2; /* Char count of RIS */
                        state = STATE_ODD_NUMBERED_RIS;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_KEYCAP:
                    if (isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_KEYCAP;
                        break;
                    }

                    if (Emoji.isKeycapBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS_AND_KEYCAP:
                    if (Emoji.isKeycapBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_EMOJI_MODIFIER:
                    if (isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_EMOJI_MODIFIER;
                        break;
                    } else if (Emoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_BEFORE_EMOJI;
                        break;
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS_AND_EMOJI_MODIFIER:
                    if (Emoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS:
                    if (Emoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_BEFORE_EMOJI;
                        break;
                    }

                    if (!isVariationSelector(codePoint) &&
                            UCharacter.getCombiningClass(codePoint) == 0) {
                        deleteCharCount += Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_EMOJI:
                    if (codePoint == Emoji.ZERO_WIDTH_JOINER) {
                        state = STATE_BEFORE_ZWJ;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_ZWJ:
                    if (Emoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint) + 1;  // +1 for ZWJ.
                        state = Emoji.isEmojiModifier(codePoint) ?
                                STATE_BEFORE_EMOJI_MODIFIER : STATE_BEFORE_EMOJI;
                    } else if (isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_ZWJ;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_VS_AND_ZWJ:
                    if (Emoji.isEmoji(codePoint)) {
                        // +1 for ZWJ.
                        deleteCharCount += lastSeenVSCharCount + 1 + Character.charCount(codePoint);
                        lastSeenVSCharCount = 0;
                        state = STATE_BEFORE_EMOJI;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_IN_TAG_SEQUENCE:
                    if (Emoji.isTagSpecChar(codePoint)) {
                        deleteCharCount += 2; /* Char count of emoji tag spec character. */
                        // Keep the same state.
                    } else if (Emoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_FINISHED;
                    } else {
                        // Couldn't find tag_base character. Delete the last tag_term character.
                        deleteCharCount = 2;  // for U+E007F
                        state = STATE_FINISHED;
                    }
                    // TODO: Need handle emoji variation selectors. Issue 35224297
                    break;
                default:
                    throw new IllegalArgumentException("state " + state + " is unknown");
            }
        } while (tmpOffset > 0 && state != STATE_FINISHED);

        return adjustReplacementSpan(text, offset - deleteCharCount, true /* move to the start */);
    }

    // Returns the end offset to be deleted by a forward delete key from the given offset.
    private static int getOffsetForForwardDeleteKey(CharSequence text, int offset, Paint paint) {
        final int len = text.length();

        if (offset >= len - 1) {
            return len;
        }

        offset = paint.getTextRunCursor(text, offset, len, false /* LTR, not used */,
                offset, Paint.CURSOR_AFTER);

        return adjustReplacementSpan(text, offset, false /* move to the end */);
    }

    private boolean backspaceOrForwardDelete(View view, Editable content, int keyCode,
            KeyEvent event, boolean isForwardDelete) {
        // Ensure the key event does not have modifiers except ALT or SHIFT or CTRL.
        if (!KeyEvent.metaStateHasNoModifiers(event.getMetaState()
                & ~(KeyEvent.META_SHIFT_MASK | KeyEvent.META_ALT_MASK | KeyEvent.META_CTRL_MASK))) {
            return false;
        }

        // If there is a current selection, delete it.
        if (deleteSelection(view, content)) {
            return true;
        }

        // MetaKeyKeyListener doesn't track control key state. Need to check the KeyEvent instead.
        boolean isCtrlActive = ((event.getMetaState() & KeyEvent.META_CTRL_ON) != 0);
        boolean isShiftActive = (getMetaState(content, META_SHIFT_ON, event) == 1);
        boolean isAltActive = (getMetaState(content, META_ALT_ON, event) == 1);

        if (isCtrlActive) {
            if (isAltActive || isShiftActive) {
                // Ctrl+Alt, Ctrl+Shift, Ctrl+Alt+Shift should not delete any characters.
                return false;
            }
            return deleteUntilWordBoundary(view, content, isForwardDelete);
        }

        // Alt+Backspace or Alt+ForwardDelete deletes the current line, if possible.
        if (isAltActive && deleteLineFromCursor(view, content, isForwardDelete)) {
            return true;
        }

        // Delete a character.
        final int start = Selection.getSelectionEnd(content);
        final int end;
        if (isForwardDelete) {
            final Paint paint;
            if (view instanceof TextView) {
                paint = ((TextView)view).getPaint();
            } else {
                synchronized (mLock) {
                    if (sCachedPaint == null) {
                        sCachedPaint = new Paint();
                    }
                    paint = sCachedPaint;
                }
            }
            end = getOffsetForForwardDeleteKey(content, start, paint);
        } else {
            end = getOffsetForBackspaceKey(content, start);
        }
        if (start != end) {
            content.delete(Math.min(start, end), Math.max(start, end));
            return true;
        }
        return false;
    }

    private boolean deleteUntilWordBoundary(View view, Editable content, boolean isForwardDelete) {
        int currentCursorOffset = Selection.getSelectionStart(content);

        // If there is a selection, do nothing.
        if (currentCursorOffset != Selection.getSelectionEnd(content)) {
            return false;
        }

        // Early exit if there is no contents to delete.
        if ((!isForwardDelete && currentCursorOffset == 0) ||
            (isForwardDelete && currentCursorOffset == content.length())) {
            return false;
        }

        WordIterator wordIterator = null;
        if (view instanceof TextView) {
            wordIterator = ((TextView)view).getWordIterator();
        }

        if (wordIterator == null) {
            // Default locale is used for WordIterator since the appropriate locale is not clear
            // here.
            // TODO: Use appropriate locale for WordIterator.
            wordIterator = new WordIterator();
        }

        int deleteFrom;
        int deleteTo;

        if (isForwardDelete) {
            deleteFrom = currentCursorOffset;
            wordIterator.setCharSequence(content, deleteFrom, content.length());
            deleteTo = wordIterator.following(currentCursorOffset);
            if (deleteTo == BreakIterator.DONE) {
                deleteTo = content.length();
            }
        } else {
            deleteTo = currentCursorOffset;
            wordIterator.setCharSequence(content, 0, deleteTo);
            deleteFrom = wordIterator.preceding(currentCursorOffset);
            if (deleteFrom == BreakIterator.DONE) {
                deleteFrom = 0;
            }
        }
        content.delete(deleteFrom, deleteTo);
        return true;
    }

    private boolean deleteSelection(View view, Editable content) {
        int selectionStart = Selection.getSelectionStart(content);
        int selectionEnd = Selection.getSelectionEnd(content);
        if (selectionEnd < selectionStart) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }
        if (selectionStart != selectionEnd) {
            content.delete(selectionStart, selectionEnd);
            return true;
        }
        return false;
    }

    private boolean deleteLineFromCursor(View view, Editable content, boolean forward) {
        if (view instanceof TextView) {
            final int selectionStart = Selection.getSelectionStart(content);
            final int selectionEnd = Selection.getSelectionEnd(content);
            final int selectionMin;
            final int selectionMax;
            if (selectionStart < selectionEnd) {
                selectionMin = selectionStart;
                selectionMax = selectionEnd;
            } else {
                selectionMin = selectionEnd;
                selectionMax = selectionStart;
            }

            final TextView textView = (TextView) view;
            final Layout layout = textView.getLayout();
            if (layout != null && !textView.isOffsetMappingAvailable()) {
                final int line = layout.getLineForOffset(Selection.getSelectionStart(content));
                final int start = layout.getLineStart(line);
                final int end = layout.getLineEnd(line);

                if (forward) {
                    content.delete(selectionMin, end);
                } else {
                    content.delete(start, selectionMax);
                }

                return true;
            }
        }
        return false;
    }

    static int makeTextContentType(Capitalize caps, boolean autoText) {
        int contentType = InputType.TYPE_CLASS_TEXT;
        switch (caps) {
            case CHARACTERS:
                contentType |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
                break;
            case WORDS:
                contentType |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
                break;
            case SENTENCES:
                contentType |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                break;
        }
        if (autoText) {
            contentType |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        }
        return contentType;
    }

    public boolean onKeyDown(View view, Editable content,
                             int keyCode, KeyEvent event) {
        boolean handled;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                handled = backspace(view, content, keyCode, event);
                break;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                handled = forwardDelete(view, content, keyCode, event);
                break;
            default:
                handled = false;
                break;
        }

        if (handled) {
            adjustMetaAfterKeypress(content);
            return true;
        }

        return super.onKeyDown(view, content, keyCode, event);
    }

    /**
     * Base implementation handles ACTION_MULTIPLE KEYCODE_UNKNOWN by inserting
     * the event's text into the content.
     */
    public boolean onKeyOther(View view, Editable content, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_MULTIPLE
                || event.getKeyCode() != KeyEvent.KEYCODE_UNKNOWN) {
            // Not something we are interested in.
            return false;
        }

        int selectionStart = Selection.getSelectionStart(content);
        int selectionEnd = Selection.getSelectionEnd(content);
        if (selectionEnd < selectionStart) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        CharSequence text = event.getCharacters();
        if (text == null) {
            return false;
        }

        content.replace(selectionStart, selectionEnd, text);
        return true;
    }
}
