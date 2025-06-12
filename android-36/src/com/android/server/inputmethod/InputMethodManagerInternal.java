/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.server.inputmethod;

import static com.android.server.inputmethod.InputMethodUtils.NOT_A_SUBTYPE_INDEX;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.InlineSuggestionsRequestCallback;
import com.android.internal.inputmethod.InlineSuggestionsRequestInfo;
import com.android.internal.inputmethod.SoftInputShowHideReason;
import com.android.server.LocalServices;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

/**
 * Input method manager local system service interface.
 */
public abstract class InputMethodManagerInternal {
    /**
     * Indicates that the method is guaranteed to not require {@link ImfLock}.
     *
     * <p>You can call this method without worrying about system_server lock layering.</p>
     */
    @Retention(SOURCE)
    @Target({ElementType.METHOD})
    public @interface ImfLockFree {
    }

    /**
     * Listener for input method list changed events.
     */
    public interface InputMethodListListener {
        /**
         * Called with the list of the installed IMEs when it's updated.
         */
        void onInputMethodListUpdated(List<InputMethodInfo> info, @UserIdInt int userId);
    }

    /**
     * Called by the power manager to tell the input method manager whether it
     * should start watching for wake events.
     *
     * @param interactive the interactive mode parameter
     */
    @ImfLockFree
    public abstract void setInteractive(boolean interactive);

    /**
     * Hides the input method for the specified {@code originatingDisplayId}, if visible.
     *
     * @param reason               the reason for hiding the current input method
     * @param originatingDisplayId the display ID the request is originated
     */
    @ImfLockFree
    public abstract void hideInputMethod(@SoftInputShowHideReason int reason,
            int originatingDisplayId);

    /**
     * Returns the list of installed input methods for the specified user.
     *
     * @param userId the user ID to be queried
     * @return a list of {@link InputMethodInfo}. VR-only IMEs are already excluded
     */
    @ImfLockFree
    @NonNull
    public abstract List<InputMethodInfo> getInputMethodListAsUser(@UserIdInt int userId);

    /**
     * Returns the list of installed input methods that are enabled for the specified user.
     *
     * @param userId the user ID to be queried
     * @return a list of {@link InputMethodInfo} that are enabled for {@code userId}
     */
    @ImfLockFree
    @NonNull
    public abstract List<InputMethodInfo> getEnabledInputMethodListAsUser(@UserIdInt int userId);

    /**
     * Returns the list of installed input methods that are enabled for the specified user.
     *
     * @param imiId                           IME ID to be queried about
     * @param allowsImplicitlyEnabledSubtypes {@code true} to return the implicitly enabled subtypes
     * @param userId                          the user ID to be queried about
     * @return a list of {@link InputMethodSubtype} that are enabled for {@code userId}
     */
    @ImfLockFree
    @NonNull
    public abstract List<InputMethodSubtype> getEnabledInputMethodSubtypeListAsUser(
            String imiId, boolean allowsImplicitlyEnabledSubtypes, @UserIdInt int userId);

    /**
     * Called by the Autofill Frameworks to request an {@link InlineSuggestionsRequest} from
     * the input method.
     *
     * @param userId      the user ID to be queried
     * @param requestInfo information needed to create an {@link InlineSuggestionsRequest}.
     * @param cb          {@link InlineSuggestionsRequestCallback} used to pass back the request
     *                    object
     */
    public abstract void onCreateInlineSuggestionsRequest(@UserIdInt int userId,
            InlineSuggestionsRequestInfo requestInfo, InlineSuggestionsRequestCallback cb);

    /**
     * Force switch to the enabled input method by {@code imeId} for the current user. If the input
     * method with {@code imeId} is not enabled or not installed, do nothing.
     *
     * @param imeId  the input method ID to be switched to
     * @param userId the user ID to be queried
     * @return {@code true} if the current input method was successfully switched to the input
     * method by {@code imeId}; {@code false} the input method with {@code imeId} is not available
     * to be switched.
     */
    public boolean switchToInputMethod(@NonNull String imeId, @UserIdInt int userId) {
        return switchToInputMethod(imeId, NOT_A_SUBTYPE_INDEX, userId);
    }

    /**
     * Force switch to the enabled input method by {@code imeId} for the current user. If the input
     * method with {@code imeId} is not enabled or not installed, do nothing. If
     * {@code subtypeIndex} is also supplied (not {@link InputMethodUtils#NOT_A_SUBTYPE_INDEX}) and
     * valid, also switches to it, otherwise the system decides the most sensible default subtype to
     * use.
     *
     * @param imeId        the input method ID to be switched to
     * @param subtypeIndex the subtype to be switched to, as an index in the input method's array of
     *                     subtypes, or {@link InputMethodUtils#NOT_A_SUBTYPE_INDEX} if the system
     *                     should decide the most sensible subtype
     * @param userId       the user ID to be queried
     * @return {@code true} if the current input method was successfully switched to the input
     * method by {@code imeId}; {@code false} the input method with {@code imeId} is not available
     * to be switched.
     */
    public abstract boolean switchToInputMethod(@NonNull String imeId, int subtypeIndex,
            @UserIdInt int userId);

    /**
     * Force enable or disable the input method associated with {@code imeId} for given user. If
     * the input method associated with {@code imeId} is not installed, do nothing.
     *
     * @param imeId   the input method ID to be enabled or disabled
     * @param enabled {@code true} if the input method associated with {@code imeId} should be
     *                enabled
     * @param userId  the user ID to be queried
     * @return {@code true} if the input method associated with {@code imeId} was successfully
     * enabled or disabled, {@code false} if the input method specified is not installed
     * or was unable to be enabled/disabled for some other reason.
     */
    public abstract boolean setInputMethodEnabled(String imeId, boolean enabled,
            @UserIdInt int userId);

    /**
     * Makes the input method associated with {@code imeId} the default input method for all users
     * on displays that are owned by the virtual device with the given {@code deviceId}. If the
     * input method associated with {@code imeId} is not available, there will be no IME on the
     * relevant displays.
     *
     * <p>The caller of this method is responsible for resetting it to {@code null} after the
     * virtual device is closed.</p>
     *
     * @param deviceId the device ID on which to use the given input method as default.
     * @param imeId  the input method ID to be used as default on the given device. If {@code null},
     *               then any existing input method association with that device will be removed.
     * @throws IllegalArgumentException if a non-{@code null} input method ID is passed for a
     *                                  device ID that already has a custom input method set or if
     *                                  the device ID is not a valid virtual device.
     */
    public abstract void setVirtualDeviceInputMethodForAllUsers(
            int deviceId, @Nullable String imeId);

    /**
     * Registers a new {@link InputMethodListListener}.
     *
     * @param listener the listener to add
     */
    @ImfLockFree
    public abstract void registerInputMethodListListener(InputMethodListListener listener);

    /**
     * Transfers input focus from a given input token to that of the IME window.
     *
     * @param sourceInputToken the source token.
     * @param displayId        the display hosting the IME window
     * @param userId           the user ID this request is about
     * @return {@code true} if the transfer is successful
     */
    public abstract boolean transferTouchFocusToImeWindow(@NonNull IBinder sourceInputToken,
            int displayId, @UserIdInt int userId);

    /**
     * Reports that IME control has transferred to the given window token, or if null that
     * control has been taken away from client windows (and is instead controlled by the policy
     * or SystemUI).
     *
     * @param windowToken the window token that is now in control, or {@code null} if no client
     *                    window is in control of the IME
     */
    public abstract void reportImeControl(@Nullable IBinder windowToken);

    /**
     * Indicates that the IME window has re-parented to the new target when the IME control changed.
     *
     * @param displayId the display hosting the IME window
     */
    public abstract void onImeParentChanged(int displayId);

    /**
     * Destroys the IME surface for the given display.
     *
     * @param displayId the display hosting the IME window
     */
    @ImfLockFree
    public abstract void removeImeSurface(int displayId);

    /**
     * Called when a non-IME-focusable overlay window being the IME layering target (e.g. a
     * window with {@link android.view.WindowManager.LayoutParams#FLAG_NOT_FOCUSABLE} and
     * {@link android.view.WindowManager.LayoutParams#FLAG_ALT_FOCUSABLE_IM} flags)
     * has changed its window visibility.
     *
     * @param hasVisibleOverlay  whether such an overlay window exists or not
     * @param displayId          the display ID where the overlay window exists
     */
    public abstract void setHasVisibleImeLayeringOverlay(boolean hasVisibleOverlay, int displayId);

    /**
     * Called when the visibility of IME input target window has changed.
     *
     * @param imeInputTarget        the window token of the IME input target window
     * @param visibleAndNotRemoved  {@code true} when the new window is made visible by
     *                              {@code imeInputTarget} and the IME input target window has not
     *                              been removed. The new window is considered to be visible when
     *                              switching to the new visible IME input target window and
     *                              starting input, or the existing input target becomes visible.
     *                              In contrast, {@code false} when closing the input target, or the
     *                              existing input target becomes invisible
     * @param displayId             the display for which to update the IME window status
     */
    public abstract void onImeInputTargetVisibilityChanged(@NonNull IBinder imeInputTarget,
            boolean visibleAndNotRemoved, int displayId);

    /**
     * Updates the IME visibility, back disposition and show IME picker status for SystemUI.
     * TODO(b/189923292): Making SystemUI to be true IME icon controller vs. presenter that
     * controlled by IMMS.
     *
     * @param disableImeIcon indicates whether IME icon should be enabled or not
     * @param displayId      the display for which to update the IME window status
     */
    @ImfLockFree
    public abstract void updateImeWindowStatus(boolean disableImeIcon, int displayId);

    /**
     * Updates and reports whether the IME switcher button should be shown, regardless whether
     * SystemUI or the IME is responsible for drawing it and the corresponding navigation bar.
     *
     * @param displayId the display for which to update the IME switcher button visibility.
     * @param userId    the user for which to update the IME switcher button visibility.
     */
    public abstract void updateShouldShowImeSwitcher(int displayId, @UserIdInt int userId);

    /**
     * Finish stylus handwriting by calling {@link InputMethodService#finishStylusHandwriting()} if
     * there is an ongoing handwriting session.
     */
    @ImfLockFree
    public abstract void maybeFinishStylusHandwriting();

    /**
     * Callback when the IInputMethodSession from the accessibility service with the specified
     * accessibilityConnectionId is created.
     *
     * @param accessibilityConnectionId the connection id of the accessibility service
     * @param session                   the session passed back from the accessibility service
     * @param userId                    the user ID to be queried
     */
    public abstract void onSessionForAccessibilityCreated(int accessibilityConnectionId,
            IAccessibilityInputMethodSession session, @UserIdInt int userId);

    /**
     * Unbind the accessibility service with the specified accessibilityConnectionId from current
     * client.
     *
     * @param accessibilityConnectionId the connection id of the accessibility service
     * @param userId the user ID to be queried
     */
    public abstract void unbindAccessibilityFromCurrentClient(int accessibilityConnectionId,
            @UserIdInt int userId);

    /**
     * Switch the keyboard layout in response to a keyboard shortcut.
     *
     * @param direction         {@code 1} to switch to the next subtype, {@code -1} to switch to the
     *                          previous subtype
     * @param displayId         the display to which the keyboard layout switch shortcut is
     *                          dispatched. Note that there is no guarantee that an IME is
     *                          associated with this display. This is more or less than a hint for
     *                          cases when no IME is running for the given targetWindowToken. There
     *                          is a longstanding discussion whether we should allow users to
     *                          rotate keyboard layout even when there is no edit field, and this
     *                          displayID would be helpful for such a situation.
     * @param targetWindowToken the window token to which other keys are being sent while handling
     *                          this shortcut.
     */
    public abstract void onSwitchKeyboardLayoutShortcut(int direction, int displayId,
            IBinder targetWindowToken);

    /**
     * Fake implementation of {@link InputMethodManagerInternal}. All the methods do nothing.
     */
    private static final InputMethodManagerInternal NOP =
            new InputMethodManagerInternal() {
                @ImfLockFree
                @Override
                public void setInteractive(boolean interactive) {
                }

                @ImfLockFree
                @Override
                public void hideInputMethod(@SoftInputShowHideReason int reason,
                        int originatingDisplayId) {
                }

                @ImfLockFree
                @NonNull
                @Override
                public List<InputMethodInfo> getInputMethodListAsUser(@UserIdInt int userId) {
                    return Collections.emptyList();
                }

                @ImfLockFree
                @NonNull
                @Override
                public List<InputMethodInfo> getEnabledInputMethodListAsUser(
                        @UserIdInt int userId) {
                    return Collections.emptyList();
                }

                @ImfLockFree
                @NonNull
                @Override
                public List<InputMethodSubtype> getEnabledInputMethodSubtypeListAsUser(String imiId,
                        boolean allowsImplicitlyEnabledSubtypes, int userId) {
                    return Collections.emptyList();
                }

                @Override
                public void onCreateInlineSuggestionsRequest(@UserIdInt int userId,
                        InlineSuggestionsRequestInfo requestInfo,
                        InlineSuggestionsRequestCallback cb) {
                }

                @Override
                public boolean switchToInputMethod(@NonNull String imeId, int subtypeIndex,
                        @UserIdInt int userId) {
                    return false;
                }

                @Override
                public boolean setInputMethodEnabled(String imeId, boolean enabled,
                        @UserIdInt int userId) {
                    return false;
                }

                @Override
                public void setVirtualDeviceInputMethodForAllUsers(
                        int deviceId, @Nullable String imeId) {
                }

                @ImfLockFree
                @Override
                public void registerInputMethodListListener(InputMethodListListener listener) {
                }

                @Override
                public boolean transferTouchFocusToImeWindow(@NonNull IBinder sourceInputToken,
                        int displayId, @UserIdInt int userId) {
                    return false;
                }

                @Override
                public void reportImeControl(@Nullable IBinder windowToken) {
                }

                @Override
                public void onImeParentChanged(int displayId) {
                }

                @ImfLockFree
                @Override
                public void removeImeSurface(int displayId) {
                }

                @Override
                public void setHasVisibleImeLayeringOverlay(boolean hasVisibleOverlay,
                        int displayId) {
                }

                @Override
                public void onImeInputTargetVisibilityChanged(@NonNull IBinder imeInputTarget,
                        boolean visibleAndNotRemoved, int displayId) {
                }

                @ImfLockFree
                @Override
                public void updateImeWindowStatus(boolean disableImeIcon, int displayId) {
                }

                @Override
                public void updateShouldShowImeSwitcher(int displayId, @UserIdInt int userId) {
                }

                @Override
                public void onSessionForAccessibilityCreated(int accessibilityConnectionId,
                        IAccessibilityInputMethodSession session, @UserIdInt int userId) {
                }

                @Override
                public void unbindAccessibilityFromCurrentClient(int accessibilityConnectionId,
                        @UserIdInt int userId) {
                }

                @ImfLockFree
                @Override
                public void maybeFinishStylusHandwriting() {
                }

                @Override
                public void onSwitchKeyboardLayoutShortcut(int direction, int displayId,
                        IBinder targetWindowToken) {
                }
            };

    /**
     * @return Global instance if exists. Otherwise, a fallback no-op instance.
     */
    @NonNull
    public static InputMethodManagerInternal get() {
        final InputMethodManagerInternal instance =
                LocalServices.getService(InputMethodManagerInternal.class);
        return instance != null ? instance : NOP;
    }
}
