/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.accessibilityservice;

import static android.accessibilityservice.MagnificationConfig.MAGNIFICATION_MODE_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;

import android.accessibilityservice.GestureDescription.MotionEventGenerator;
import android.annotation.CallbackExecutor;
import android.annotation.CheckResult;
import android.annotation.ColorInt;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.TestApi;
import android.app.Service;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.ParcelableColorSpace;
import android.graphics.Region;
import android.hardware.HardwareBuffer;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityCache;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.Flags;
import android.view.inputmethod.EditorInfo;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.inputmethod.CancellationGroup;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.IAccessibilityInputMethodSessionCallback;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
import com.android.internal.inputmethod.RemoteAccessibilityInputConnection;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Accessibility services should only be used to assist users with disabilities in using
 * Android devices and apps. They run in the background and receive callbacks by the system
 * when {@link AccessibilityEvent}s are fired. Such events denote some state transition
 * in the user interface, for example, the focus has changed, a button has been clicked,
 * etc. Such a service can optionally request the capability for querying the content
 * of the active window. Development of an accessibility service requires extending this
 * class and implementing its abstract methods.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about creating AccessibilityServices, read the
 * <a href="{@docRoot}guide/topics/ui/accessibility/index.html">Accessibility</a>
 * developer guide.</p>
 * </div>
 *
 * <h3>Lifecycle</h3>
 * <p>
 * The lifecycle of an accessibility service is managed exclusively by the system and
 * follows the established service life cycle. Starting an accessibility service is triggered
 * exclusively by the user explicitly turning the service on in device settings. After the system
 * binds to a service, it calls {@link AccessibilityService#onServiceConnected()}. This method can
 * be overridden by clients that want to perform post binding setup.
 * </p>
 * <p>
 * An accessibility service stops either when the user turns it off in device settings or when
 * it calls {@link AccessibilityService#disableSelf()}.
 * </p>
 * <h3>Declaration</h3>
 * <p>
 * An accessibility is declared as any other service in an AndroidManifest.xml, but it
 * must do two things:
 * <ul>
 *     <li>
 *         Specify that it handles the "android.accessibilityservice.AccessibilityService"
 *         {@link android.content.Intent}.
 *     </li>
 *     <li>
 *         Request the {@link android.Manifest.permission#BIND_ACCESSIBILITY_SERVICE} permission to
 *         ensure that only the system can bind to it.
 *     </li>
 * </ul>
 * If either of these items is missing, the system will ignore the accessibility service.
 * Following is an example declaration:
 * </p>
 * <pre> &lt;service android:name=".MyAccessibilityService"
 *         android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.accessibilityservice.AccessibilityService" /&gt;
 *     &lt;/intent-filter&gt;
 *     . . .
 * &lt;/service&gt;</pre>
 * <h3>Configuration</h3>
 * <p>
 * An accessibility service can be configured to receive specific types of accessibility events,
 * listen only to specific packages, get events from each type only once in a given time frame,
 * retrieve window content, specify a settings activity, etc.
 * </p>
 * <p>
 * There are two approaches for configuring an accessibility service:
 * </p>
 * <ul>
 * <li>
 * Providing a {@link #SERVICE_META_DATA meta-data} entry in the manifest when declaring
 * the service. A service declaration with a meta-data tag is presented below:
 * <pre> &lt;service android:name=".MyAccessibilityService"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.accessibilityservice.AccessibilityService" /&gt;
 *     &lt;/intent-filter&gt;
 *     &lt;meta-data android:name="android.accessibilityservice" android:resource="@xml/accessibilityservice" /&gt;
 * &lt;/service&gt;</pre>
 * <p class="note">
 * <strong>Note:</strong> This approach enables setting all properties.
 * </p>
 * <p>
 * For more details refer to {@link #SERVICE_META_DATA} and
 * <code>&lt;{@link android.R.styleable#AccessibilityService accessibility-service}&gt;</code>.
 * </p>
 * </li>
 * <li>
 * Calling {@link AccessibilityService#setServiceInfo(AccessibilityServiceInfo)}. Note
 * that this method can be called any time to dynamically change the service configuration.
 * <p class="note">
 * <strong>Note:</strong> This approach enables setting only dynamically configurable properties:
 * {@link AccessibilityServiceInfo#eventTypes},
 * {@link AccessibilityServiceInfo#feedbackType},
 * {@link AccessibilityServiceInfo#flags},
 * {@link AccessibilityServiceInfo#notificationTimeout},
 * {@link AccessibilityServiceInfo#packageNames}
 * </p>
 * <p>
 * For more details refer to {@link AccessibilityServiceInfo}.
 * </p>
 * </li>
 * </ul>
 * <h3>Retrieving window content</h3>
 * <p>
 * A service can specify in its declaration that it can retrieve window
 * content which is represented as a tree of {@link AccessibilityWindowInfo} and
 * {@link AccessibilityNodeInfo} objects. Note that
 * declaring this capability requires that the service declares its configuration via
 * an XML resource referenced by {@link #SERVICE_META_DATA}.
 * </p>
 * <p>
 * Window content may be retrieved with
 * {@link AccessibilityEvent#getSource() AccessibilityEvent.getSource()},
 * {@link AccessibilityService#findFocus(int)},
 * {@link AccessibilityService#getWindows()}, or
 * {@link AccessibilityService#getRootInActiveWindow()}.
 * </p>
 * <p class="note">
 * <strong>Note</strong> An accessibility service may have requested to be notified for
 * a subset of the event types, and thus be unaware when the node hierarchy has changed. It is also
 * possible for a node to contain outdated information because the window content may change at any
 * time.
 * </p>
 * <h3>Drawing Accessibility Overlays</h3>
 * <p>Accessibility services can draw overlays on top of existing screen contents.
 * Accessibility overlays can be used to visually highlight items on the screen
 * e.g. indicate the current item with accessibility focus.
 * Overlays can also offer the user a way to interact with the service directly and quickly
 * customize the service's behavior.</p>
 * <p>Accessibility overlays can be attached to a particular window or to the display itself.
 * Attaching an overlay to a window allows the overly to move, grow and shrink as the window does.
 * The overlay will maintain the same relative position within the window bounds as the window
 * moves. The overlay will also maintain the same relative position within the window bounds if
 * the window is resized.
 * To attach an overlay to a window, use {@link #attachAccessibilityOverlayToWindow}.
 * Attaching an overlay to the display means that the overlay is independent of the active
 * windows on that display.
 * To attach an overlay to a display, use {@link #attachAccessibilityOverlayToDisplay}. </p>
 * <p> When positioning an overlay that is attached to a window, the service must use window
 * coordinates. In order to position an overlay on top of an existing UI element it is necessary
 * to know the bounds of that element in window coordinates. To find the bounds in window
 * coordinates of an element, find the corresponding {@link AccessibilityNodeInfo} as discussed
 * above and call {@link AccessibilityNodeInfo#getBoundsInWindow}. </p>
 * <h3>Notification strategy</h3>
 * <p>
 * All accessibility services are notified of all events they have requested, regardless of their
 * feedback type.
 * </p>
 * <p class="note">
 * <strong>Note:</strong> The event notification timeout is useful to avoid propagating
 * events to the client too frequently since this is accomplished via an expensive
 * interprocess call. One can think of the timeout as a criteria to determine when
 * event generation has settled down.</p>
 * <h3>Event types</h3>
 * <ul>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_CLICKED}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_LONG_CLICKED}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_FOCUSED}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_SELECTED}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_TEXT_CHANGED}</li>
 * <li>{@link AccessibilityEvent#TYPE_WINDOW_STATE_CHANGED}</li>
 * <li>{@link AccessibilityEvent#TYPE_NOTIFICATION_STATE_CHANGED}</li>
 * <li>{@link AccessibilityEvent#TYPE_TOUCH_EXPLORATION_GESTURE_START}</li>
 * <li>{@link AccessibilityEvent#TYPE_TOUCH_EXPLORATION_GESTURE_END}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_HOVER_ENTER}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_HOVER_EXIT}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_SCROLLED}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_TEXT_SELECTION_CHANGED}</li>
 * <li>{@link AccessibilityEvent#TYPE_WINDOW_CONTENT_CHANGED}</li>
 * <li>{@link AccessibilityEvent#TYPE_ANNOUNCEMENT}</li>
 * <li>{@link AccessibilityEvent#TYPE_GESTURE_DETECTION_START}</li>
 * <li>{@link AccessibilityEvent#TYPE_GESTURE_DETECTION_END}</li>
 * <li>{@link AccessibilityEvent#TYPE_TOUCH_INTERACTION_START}</li>
 * <li>{@link AccessibilityEvent#TYPE_TOUCH_INTERACTION_END}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_ACCESSIBILITY_FOCUSED}</li>
 * <li>{@link AccessibilityEvent#TYPE_WINDOWS_CHANGED}</li>
 * <li>{@link AccessibilityEvent#TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED}</li>
 * </ul>
 * <h3>Feedback types</h3>
 * <ul>
 * <li>{@link AccessibilityServiceInfo#FEEDBACK_AUDIBLE}</li>
 * <li>{@link AccessibilityServiceInfo#FEEDBACK_HAPTIC}</li>
 * <li>{@link AccessibilityServiceInfo#FEEDBACK_SPOKEN}</li>
 * <li>{@link AccessibilityServiceInfo#FEEDBACK_VISUAL}</li>
 * <li>{@link AccessibilityServiceInfo#FEEDBACK_GENERIC}</li>
 * <li>{@link AccessibilityServiceInfo#FEEDBACK_BRAILLE}</li>
 * </ul>
 * @see AccessibilityEvent
 * @see AccessibilityServiceInfo
 * @see android.view.accessibility.AccessibilityManager
 */
public abstract class AccessibilityService extends Service {

    /**
     * The user has performed a touch-exploration gesture on the touch screen without ever
     * triggering gesture detection. This gesture is only dispatched when {@link
     * AccessibilityServiceInfo#FLAG_SEND_MOTION_EVENTS} is set.
     *
     * @hide
     */
    public static final int GESTURE_TOUCH_EXPLORATION = -2;

    /**
     * The user has performed a passthrough gesture on the touch screen without ever triggering
     * gesture detection. This gesture is only dispatched when {@link
     * AccessibilityServiceInfo#FLAG_SEND_MOTION_EVENTS} is set.
     * @hide
     */
    public static final int GESTURE_PASSTHROUGH = -1;

    /**
     * The user has performed an unrecognized gesture on the touch screen. This gesture is only
     * dispatched when {@link AccessibilityServiceInfo#FLAG_SEND_MOTION_EVENTS} is set.
     */
    public static final int GESTURE_UNKNOWN = 0;

    /**
     * The user has performed a swipe up gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_UP = 1;

    /**
     * The user has performed a swipe down gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_DOWN = 2;

    /**
     * The user has performed a swipe left gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_LEFT = 3;

    /**
     * The user has performed a swipe right gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_RIGHT = 4;

    /**
     * The user has performed a swipe left and right gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_LEFT_AND_RIGHT = 5;

    /**
     * The user has performed a swipe right and left gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_RIGHT_AND_LEFT = 6;

    /**
     * The user has performed a swipe up and down gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_UP_AND_DOWN = 7;

    /**
     * The user has performed a swipe down and up gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_DOWN_AND_UP = 8;

    /**
     * The user has performed a left and up gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_LEFT_AND_UP = 9;

    /**
     * The user has performed a left and down gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_LEFT_AND_DOWN = 10;

    /**
     * The user has performed a right and up gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_RIGHT_AND_UP = 11;

    /**
     * The user has performed a right and down gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_RIGHT_AND_DOWN = 12;

    /**
     * The user has performed an up and left gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_UP_AND_LEFT = 13;

    /**
     * The user has performed an up and right gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_UP_AND_RIGHT = 14;

    /**
     * The user has performed a down and left gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_DOWN_AND_LEFT = 15;

    /**
     * The user has performed a down and right gesture on the touch screen.
     */
    public static final int GESTURE_SWIPE_DOWN_AND_RIGHT = 16;

    /**
     * The user has performed a double tap gesture on the touch screen.
     */
    public static final int GESTURE_DOUBLE_TAP = 17;

    /**
     * The user has performed a double tap and hold gesture on the touch screen.
     */
    public static final int GESTURE_DOUBLE_TAP_AND_HOLD = 18;

    /**
     * The user has performed a two-finger single tap gesture on the touch screen.
     */
    public static final int GESTURE_2_FINGER_SINGLE_TAP = 19;

    /**
     * The user has performed a two-finger double tap gesture on the touch screen.
     */
    public static final int GESTURE_2_FINGER_DOUBLE_TAP = 20;

    /**
     * The user has performed a two-finger triple tap gesture on the touch screen.
     */
    public static final int GESTURE_2_FINGER_TRIPLE_TAP = 21;

    /**
     * The user has performed a three-finger single tap gesture on the touch screen.
     */
    public static final int GESTURE_3_FINGER_SINGLE_TAP = 22;

    /**
     * The user has performed a three-finger double tap gesture on the touch screen.
     */
    public static final int GESTURE_3_FINGER_DOUBLE_TAP = 23;

    /**
     * The user has performed a three-finger triple tap gesture on the touch screen.
     */
    public static final int GESTURE_3_FINGER_TRIPLE_TAP = 24;

    /**
     * The user has performed a two-finger swipe up gesture on the touch screen.
     */
    public static final int GESTURE_2_FINGER_SWIPE_UP = 25;

    /**
     * The user has performed a two-finger swipe down gesture on the touch screen.
     */
    public static final int GESTURE_2_FINGER_SWIPE_DOWN = 26;

    /**
     * The user has performed a two-finger swipe left gesture on the touch screen.
     */
    public static final int GESTURE_2_FINGER_SWIPE_LEFT = 27;

    /**
     * The user has performed a two-finger swipe right gesture on the touch screen.
     */
    public static final int GESTURE_2_FINGER_SWIPE_RIGHT = 28;

    /**
     * The user has performed a three-finger swipe up gesture on the touch screen.
     */
    public static final int GESTURE_3_FINGER_SWIPE_UP = 29;

    /**
     * The user has performed a three-finger swipe down gesture on the touch screen.
     */
    public static final int GESTURE_3_FINGER_SWIPE_DOWN = 30;

    /**
     * The user has performed a three-finger swipe left gesture on the touch screen.
     */
    public static final int GESTURE_3_FINGER_SWIPE_LEFT = 31;

    /**
     * The user has performed a three-finger swipe right gesture on the touch screen.
     */
    public static final int GESTURE_3_FINGER_SWIPE_RIGHT = 32;

    /** The user has performed a four-finger swipe up gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_SWIPE_UP = 33;

    /** The user has performed a four-finger swipe down gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_SWIPE_DOWN = 34;

    /** The user has performed a four-finger swipe left gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_SWIPE_LEFT = 35;

    /** The user has performed a four-finger swipe right gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_SWIPE_RIGHT = 36;

    /** The user has performed a four-finger single tap gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_SINGLE_TAP = 37;

    /** The user has performed a four-finger double tap gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_DOUBLE_TAP = 38;

    /** The user has performed a four-finger triple tap gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_TRIPLE_TAP = 39;

    /** The user has performed a two-finger double tap and hold gesture on the touch screen. */
    public static final int GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD = 40;

    /** The user has performed a three-finger double tap and hold gesture on the touch screen. */
    public static final int GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD = 41;

    /** The user has performed a two-finger  triple-tap and hold gesture on the touch screen. */
    public static final int GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD = 43;

    /** The user has performed a three-finger  single-tap and hold gesture on the touch screen. */
    public static final int GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD = 44;

    /** The user has performed a three-finger  triple-tap and hold gesture on the touch screen. */
    public static final int GESTURE_3_FINGER_TRIPLE_TAP_AND_HOLD = 45;

    /** The user has performed a two-finger double tap and hold gesture on the touch screen. */
    public static final int GESTURE_4_FINGER_DOUBLE_TAP_AND_HOLD = 42;

    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE =
        "android.accessibilityservice.AccessibilityService";

    /**
     * Name under which an AccessibilityService component publishes information
     * about itself. This meta-data must reference an XML resource containing an
     * <code>&lt;{@link android.R.styleable#AccessibilityService accessibility-service}&gt;</code>
     * tag. This is a sample XML file configuring an accessibility service:
     * <pre> &lt;accessibility-service
     *     android:accessibilityEventTypes="typeViewClicked|typeViewFocused"
     *     android:packageNames="foo.bar, foo.baz"
     *     android:accessibilityFeedbackType="feedbackSpoken"
     *     android:notificationTimeout="100"
     *     android:accessibilityFlags="flagDefault"
     *     android:settingsActivity="foo.bar.TestBackActivity"
     *     android:canRetrieveWindowContent="true"
     *     android:canRequestTouchExplorationMode="true"
     *     . . .
     * /&gt;</pre>
     */
    public static final String SERVICE_META_DATA = "android.accessibilityservice";

    /**
     * Action to go back.
     */
    public static final int GLOBAL_ACTION_BACK = 1;

    /**
     * Action to go home.
     */
    public static final int GLOBAL_ACTION_HOME = 2;

    /**
     * Action to toggle showing the overview of recent apps. Will fail on platforms that don't
     * show recent apps.
     */
    public static final int GLOBAL_ACTION_RECENTS = 3;

    /**
     * Action to open the notifications.
     */
    public static final int GLOBAL_ACTION_NOTIFICATIONS = 4;

    /**
     * Action to open the quick settings.
     */
    public static final int GLOBAL_ACTION_QUICK_SETTINGS = 5;

    /**
     * Action to open the power long-press dialog.
     */
    public static final int GLOBAL_ACTION_POWER_DIALOG = 6;

    /**
     * Action to toggle docking the current app's window.
     * <p>
     * <strong>Note:</strong>  It is effective only if it appears in {@link #getSystemActions()}.
     */
    public static final int GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN = 7;

    /**
     * Action to lock the screen
     */
    public static final int GLOBAL_ACTION_LOCK_SCREEN = 8;

    /**
     * Action to take a screenshot
     */
    public static final int GLOBAL_ACTION_TAKE_SCREENSHOT = 9;

    /**
     * Action to send the KEYCODE_HEADSETHOOK KeyEvent, which is used to answer and hang up calls
     * and play and stop media. Calling takes priority. If there is an incoming call,
     * this action can be used to answer that call, and if there is an ongoing call, to hang up on
     * that call.
     */
    public static final int GLOBAL_ACTION_KEYCODE_HEADSETHOOK = 10;

    /**
     * Action to trigger the Accessibility Button
     */
    public static final int GLOBAL_ACTION_ACCESSIBILITY_BUTTON = 11;

    /**
     * Action to bring up the Accessibility Button's chooser menu
     */
    public static final int GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER = 12;

    /**
     * Action to trigger the Accessibility Shortcut. This shortcut has a hardware trigger and can
     * be activated by holding down the two volume keys.
     */
    public static final int GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT = 13;

    /**
     * Action to show Launcher's all apps.
     */
    public static final int GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS = 14;

    /**
     * Action to dismiss the notification shade
     */
    public static final int GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE = 15;

    /**
     * Action to trigger dpad up keyevent.
     */
    public static final int GLOBAL_ACTION_DPAD_UP = 16;

    /**
     * Action to trigger dpad down keyevent.
     */
    public static final int GLOBAL_ACTION_DPAD_DOWN = 17;

    /**
     * Action to trigger dpad left keyevent.
     */
    public static final int GLOBAL_ACTION_DPAD_LEFT = 18;

    /**
     * Action to trigger dpad right keyevent.
     */
    public static final int GLOBAL_ACTION_DPAD_RIGHT = 19;

    /**
     * Action to trigger dpad center keyevent.
     */
    public static final int GLOBAL_ACTION_DPAD_CENTER = 20;

    /**
     * Action to trigger menu key event.
     */
    @FlaggedApi(Flags.FLAG_GLOBAL_ACTION_MENU)
    public static final int GLOBAL_ACTION_MENU = 21;

    /**
     * Action to trigger media play/pause key event.
     */
    @FlaggedApi(Flags.FLAG_GLOBAL_ACTION_MEDIA_PLAY_PAUSE)
    public static final int GLOBAL_ACTION_MEDIA_PLAY_PAUSE = 22;

    private static final String LOG_TAG = "AccessibilityService";

    /**
     * Interface used by IAccessibilityServiceClientWrapper to call the service from its main
     * thread.
     * @hide
     */
    public interface Callbacks {
        void onAccessibilityEvent(AccessibilityEvent event);
        void onInterrupt();
        void onServiceConnected();
        void init(int connectionId, IBinder windowToken);
        /** The detected gesture information for different displays */
        boolean onGesture(AccessibilityGestureEvent gestureInfo);
        boolean onKeyEvent(KeyEvent event);
        /** Magnification changed callbacks for different displays */
        void onMagnificationChanged(int displayId, @NonNull Region region,
                MagnificationConfig config);
        /** Callbacks for receiving motion events. */
        void onMotionEvent(MotionEvent event);
        /** Callback for tuch state changes. */
        void onTouchStateChanged(int displayId, int state);
        void onSoftKeyboardShowModeChanged(int showMode);
        void onPerformGestureResult(int sequence, boolean completedSuccessfully);
        void onFingerprintCapturingGesturesChanged(boolean active);
        void onFingerprintGesture(int gesture);
        /** Accessbility button clicked callbacks for different displays */
        void onAccessibilityButtonClicked(int displayId);
        void onAccessibilityButtonAvailabilityChanged(boolean available);
        /** This is called when the system action list is changed. */
        void onSystemActionsChanged();
        /** This is called when an app requests ime sessions or when the service is enabled. */
        void createImeSession(IAccessibilityInputMethodSessionCallback callback);
        /** This is called when an app starts input or when the service is enabled. */
        void startInput(@Nullable RemoteAccessibilityInputConnection inputConnection,
                @NonNull EditorInfo editorInfo, boolean restarting);
    }

    /**
     * Annotations for Soft Keyboard show modes so tools can catch invalid show modes.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "SHOW_MODE_" }, value = {
            SHOW_MODE_AUTO,
            SHOW_MODE_HIDDEN,
            SHOW_MODE_IGNORE_HARD_KEYBOARD
    })
    public @interface SoftKeyboardShowMode {}

    /**
     * Allow the system to control when the soft keyboard is shown.
     * @see SoftKeyboardController
     */
    public static final int SHOW_MODE_AUTO = 0;

    /**
     * Never show the soft keyboard.
     * @see SoftKeyboardController
     */
    public static final int SHOW_MODE_HIDDEN = 1;

    /**
     * Allow the soft keyboard to be shown, even if a hard keyboard is connected
     * @see SoftKeyboardController
     */
    public static final int SHOW_MODE_IGNORE_HARD_KEYBOARD = 2;

    /**
     * Mask used to cover the show modes supported in public API
     * @hide
     */
    public static final int SHOW_MODE_MASK = 0x03;

    /**
     * Bit used to hold the old value of the hard IME setting to restore when a service is shut
     * down.
     * @hide
     */
    public static final int SHOW_MODE_HARD_KEYBOARD_ORIGINAL_VALUE = 0x20000000;

    /**
     * Bit for show mode setting to indicate that the user has overridden the hard keyboard
     * behavior.
     * @hide
     */
    public static final int SHOW_MODE_HARD_KEYBOARD_OVERRIDDEN = 0x40000000;

    /**
     * Annotations for error codes of taking screenshot.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "TAKE_SCREENSHOT_" }, value = {
            ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR,
            ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS,
            ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT,
            ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY,
            ERROR_TAKE_SCREENSHOT_INVALID_WINDOW
    })
    public @interface ScreenshotErrorCode {}

    /**
     * The status of taking screenshot is success.
     * @hide
     */
    public static final int TAKE_SCREENSHOT_SUCCESS = 0;

    /**
     * The status of taking screenshot is failure and the reason is internal error.
     */
    public static final int ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR = 1;

    /**
     * The status of taking screenshot is failure and the reason is no accessibility access.
     */
    public static final int ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS = 2;

    /**
     * The status of taking screenshot is failure and the reason is that too little time has
     * elapsed since the last screenshot.
     */
    public static final int ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT = 3;

    /**
     * The status of taking screenshot is failure and the reason is invalid display Id.
     */
    public static final int ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY = 4;

    /**
     * The status of taking screenshot is failure and the reason is invalid accessibility window Id.
     */
    public static final int ERROR_TAKE_SCREENSHOT_INVALID_WINDOW = 5;

    /**
     * The status of taking screenshot is failure and the reason is the window contains secure
     * content.
     * @see WindowManager.LayoutParams#FLAG_SECURE
     */
    public static final int ERROR_TAKE_SCREENSHOT_SECURE_WINDOW = 6;

    /**
     * The interval time of calling
     * {@link AccessibilityService#takeScreenshot(int, Executor, Consumer)} API.
     * @hide
     */
    @TestApi
    public static final int ACCESSIBILITY_TAKE_SCREENSHOT_REQUEST_INTERVAL_TIMES_MS = 333;

    /** @hide */
    public static final String KEY_ACCESSIBILITY_SCREENSHOT_STATUS =
            "screenshot_status";

    /** @hide */
    public static final String KEY_ACCESSIBILITY_SCREENSHOT_HARDWAREBUFFER =
            "screenshot_hardwareBuffer";

    /** @hide */
    public static final String KEY_ACCESSIBILITY_SCREENSHOT_COLORSPACE =
            "screenshot_colorSpace";

    /** @hide */
    public static final String KEY_ACCESSIBILITY_SCREENSHOT_TIMESTAMP =
            "screenshot_timestamp";


    /**
     * Annotations for result codes of attaching accessibility overlays.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @FlaggedApi("android.view.accessibility.a11y_overlay_callbacks")
    @IntDef(
            prefix = {"OVERLAY_RESULT_"},
            value = {
                OVERLAY_RESULT_SUCCESS,
                OVERLAY_RESULT_INTERNAL_ERROR,
                OVERLAY_RESULT_INVALID,
            })
    public @interface AttachOverlayResult {}

    /** Result code indicating the overlay was successfully attached. */
    @FlaggedApi("android.view.accessibility.a11y_overlay_callbacks")
    public static final int OVERLAY_RESULT_SUCCESS = 0;

    /**
     * Result code indicating the overlay could not be attached due to an internal
     * error and not
     * because of problems with the input.
     */
    @FlaggedApi("android.view.accessibility.a11y_overlay_callbacks")
    public static final int OVERLAY_RESULT_INTERNAL_ERROR = 1;

    /**
     * Result code indicating the overlay could not be attached because the
     * specified display or
     * window id was invalid.
     */
    @FlaggedApi("android.view.accessibility.a11y_overlay_callbacks")
    public static final int OVERLAY_RESULT_INVALID = 2;

    private int mConnectionId = AccessibilityInteractionClient.NO_ID;

    @UnsupportedAppUsage
    private AccessibilityServiceInfo mInfo;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private IBinder mWindowToken;

    private WindowManager mWindowManager;

    /** List of magnification controllers, mapping from displayId -> MagnificationController. */
    private final SparseArray<MagnificationController> mMagnificationControllers =
            new SparseArray<>(0);
    /**
     * List of touch interaction controllers, mapping from displayId -> TouchInteractionController.
     */
    private final SparseArray<TouchInteractionController> mTouchInteractionControllers =
            new SparseArray<>(0);

    private SoftKeyboardController mSoftKeyboardController;
    private InputMethod mInputMethod;
    private boolean mInputMethodInitialized = false;
    private final SparseArray<AccessibilityButtonController> mAccessibilityButtonControllers =
            new SparseArray<>(0);
    private BrailleDisplayController mBrailleDisplayController;

    private int mGestureStatusCallbackSequence;

    private SparseArray<GestureResultCallbackInfo> mGestureStatusCallbackInfos;

    private final Object mLock = new Object();

    private FingerprintGestureController mFingerprintGestureController;

    private int mMotionEventSources;

    /**
     * Callback for {@link android.view.accessibility.AccessibilityEvent}s.
     *
     * @param event The new event. This event is owned by the caller and cannot be used after
     * this method returns. Services wishing to use the event after this method returns should
     * make a copy.
     */
    public abstract void onAccessibilityEvent(AccessibilityEvent event);

    /**
     * Callback for interrupting the accessibility feedback.
     */
    public abstract void onInterrupt();

    /**
     * Dispatches service connection to internal components first, then the
     * client code.
     */
    private void dispatchServiceConnected() {
        synchronized (mLock) {
            for (int i = 0; i < mMagnificationControllers.size(); i++) {
                mMagnificationControllers.valueAt(i).onServiceConnectedLocked();
            }
            final AccessibilityServiceInfo info = getServiceInfo();
            if (info != null) {
                updateInputMethod(info);
                mMotionEventSources = info.getMotionEventSources();
            }
        }
        if (mSoftKeyboardController != null) {
            mSoftKeyboardController.onServiceConnected();
        }

        // The client gets to handle service connection last, after we've set
        // up any state upon which their code may rely.
        onServiceConnected();
    }

    private void updateInputMethod(AccessibilityServiceInfo info) {
        if (info != null) {
            boolean requestIme = (info.flags
                    & AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR) != 0;
            if (requestIme && !mInputMethodInitialized) {
                mInputMethod = onCreateInputMethod();
                mInputMethodInitialized = true;
            } else if (!requestIme & mInputMethodInitialized) {
                mInputMethod = null;
                mInputMethodInitialized = false;
            }
        }
    }

    /**
     * This method is a part of the {@link AccessibilityService} lifecycle and is
     * called after the system has successfully bound to the service. If is
     * convenient to use this method for setting the {@link AccessibilityServiceInfo}.
     *
     * @see AccessibilityServiceInfo
     * @see #setServiceInfo(AccessibilityServiceInfo)
     */
    protected void onServiceConnected() {

    }

    /**
     * Called by {@link #onGesture(AccessibilityGestureEvent)} when the user performs a specific
     * gesture on the default display.
     *
     * <strong>Note:</strong> To receive gestures an accessibility service must
     * request that the device is in touch exploration mode by setting the
     * {@link AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE}
     * flag.
     *
     * @param gestureId The unique id of the performed gesture.
     *
     * @return Whether the gesture was handled.
     * @deprecated Override {@link #onGesture(AccessibilityGestureEvent)} instead.
     *
     * @see #GESTURE_SWIPE_UP
     * @see #GESTURE_SWIPE_UP_AND_LEFT
     * @see #GESTURE_SWIPE_UP_AND_DOWN
     * @see #GESTURE_SWIPE_UP_AND_RIGHT
     * @see #GESTURE_SWIPE_DOWN
     * @see #GESTURE_SWIPE_DOWN_AND_LEFT
     * @see #GESTURE_SWIPE_DOWN_AND_UP
     * @see #GESTURE_SWIPE_DOWN_AND_RIGHT
     * @see #GESTURE_SWIPE_LEFT
     * @see #GESTURE_SWIPE_LEFT_AND_UP
     * @see #GESTURE_SWIPE_LEFT_AND_RIGHT
     * @see #GESTURE_SWIPE_LEFT_AND_DOWN
     * @see #GESTURE_SWIPE_RIGHT
     * @see #GESTURE_SWIPE_RIGHT_AND_UP
     * @see #GESTURE_SWIPE_RIGHT_AND_LEFT
     * @see #GESTURE_SWIPE_RIGHT_AND_DOWN
     */
    @Deprecated
    protected boolean onGesture(int gestureId) {
        return false;
    }

    /**
     * Called by the system when the user performs a specific gesture on the
     * specific touch screen.
     *<p>
     * <strong>Note:</strong> To receive gestures an accessibility service must
     * request that the device is in touch exploration mode by setting the
     * {@link AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE}
     * flag.
     *<p>
     * <strong>Note:</strong> The default implementation calls {@link #onGesture(int)} when the
     * touch screen is default display.
     *
     * @param gestureEvent The information of gesture.
     *
     * @return Whether the gesture was handled.
     *
     */
    public boolean onGesture(@NonNull AccessibilityGestureEvent gestureEvent) {
        if (gestureEvent.getDisplayId() == Display.DEFAULT_DISPLAY) {
            onGesture(gestureEvent.getGestureId());
        }
        return false;
    }

    /**
     * Callback that allows an accessibility service to observe the key events
     * before they are passed to the rest of the system. This means that the events
     * are first delivered here before they are passed to the device policy, the
     * input method, or applications.
     * <p>
     * <strong>Note:</strong> It is important that key events are handled in such
     * a way that the event stream that would be passed to the rest of the system
     * is well-formed. For example, handling the down event but not the up event
     * and vice versa would generate an inconsistent event stream.
     * </p>
     * <p>
     * <strong>Note:</strong> The key events delivered in this method are copies
     * and modifying them will have no effect on the events that will be passed
     * to the system. This method is intended to perform purely filtering
     * functionality.
     * <p>
     *
     * @param event The event to be processed. This event is owned by the caller and cannot be used
     * after this method returns. Services wishing to use the event after this method returns should
     * make a copy.
     * @return If true then the event will be consumed and not delivered to
     *         applications, otherwise it will be delivered as usual.
     */
    protected boolean onKeyEvent(KeyEvent event) {
        return false;
    }

    /**
     * Callback that allows an accessibility service to observe generic {@link MotionEvent}s.
     * <p>
     * Prefer {@link TouchInteractionController} to observe and control touchscreen events,
     * including touch gestures. If this or any enabled service is using
     * {@link AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE} then
     * {@link #onMotionEvent} will not receive touchscreen events.
     * </p>
     * <p>
     * <strong>Note:</strong> The service must first request to listen to events using
     * {@link AccessibilityServiceInfo#setMotionEventSources}.
     * {@link MotionEvent}s from sources in {@link AccessibilityServiceInfo#getMotionEventSources()}
     * are not sent to the rest of the system. To stop listening to events from a given source, call
     * {@link AccessibilityServiceInfo#setMotionEventSources} with that source removed.
     * </p>
     * @param event The event to be processed.
     */
    public void onMotionEvent(@NonNull MotionEvent event) { }

    /**
     * Gets the windows on the screen of the default display. This method returns only the windows
     * that a sighted user can interact with, as opposed to all windows.
     * For example, if there is a modal dialog shown and the user cannot touch
     * anything behind it, then only the modal window will be reported
     * (assuming it is the top one). For convenience the returned windows
     * are ordered in a descending layer order, which is the windows that
     * are on top are reported first. Since the user can always
     * interact with the window that has input focus by typing, the focused
     * window is always returned (even if covered by a modal window).
     * <p>
     * <strong>Note:</strong> In order to access the windows your service has
     * to declare the capability to retrieve window content by setting the
     * {@link android.R.styleable#AccessibilityService_canRetrieveWindowContent}
     * property in its meta-data. For details refer to {@link #SERVICE_META_DATA}.
     * Also the service has to opt-in to retrieve the interactive windows by
     * setting the {@link AccessibilityServiceInfo#FLAG_RETRIEVE_INTERACTIVE_WINDOWS}
     * flag.
     * </p>
     *
     * @return The windows if there are windows and the service is can retrieve
     *         them, otherwise an empty list.
     */
    public List<AccessibilityWindowInfo> getWindows() {
        return AccessibilityInteractionClient.getInstance(this).getWindows(mConnectionId);
    }

    /**
     * Gets the windows on the screen of all displays. This method returns only the windows
     * that a sighted user can interact with, as opposed to all windows.
     * For example, if there is a modal dialog shown and the user cannot touch
     * anything behind it, then only the modal window will be reported
     * (assuming it is the top one). For convenience the returned windows
     * are ordered in a descending layer order, which is the windows that
     * are on top are reported first. Since the user can always
     * interact with the window that has input focus by typing, the focused
     * window is always returned (even if covered by a modal window).
     * <p>
     * <strong>Note:</strong> In order to access the windows your service has
     * to declare the capability to retrieve window content by setting the
     * {@link android.R.styleable#AccessibilityService_canRetrieveWindowContent}
     * property in its meta-data. For details refer to {@link #SERVICE_META_DATA}.
     * Also the service has to opt-in to retrieve the interactive windows by
     * setting the {@link AccessibilityServiceInfo#FLAG_RETRIEVE_INTERACTIVE_WINDOWS}
     * flag.
     * </p>
     *
     * @return The windows of all displays if there are windows and the service is can retrieve
     *         them, otherwise an empty list. The key of SparseArray is display ID.
     */
    @NonNull
    public final SparseArray<List<AccessibilityWindowInfo>> getWindowsOnAllDisplays() {
        return AccessibilityInteractionClient.getInstance(this).getWindowsOnAllDisplays(
                mConnectionId);
    }

    /**
     * Gets the root node in the currently active window if this service
     * can retrieve window content. The active window is the one that the user
     * is currently touching or the window with input focus, if the user is not
     * touching any window. It could be from any logical display.
     * <p>
     * <strong>Note:</strong> In order to access the root node your service has
     * to declare the capability to retrieve window content by setting the
     * {@link android.R.styleable#AccessibilityService_canRetrieveWindowContent}
     * property in its meta-data. For details refer to {@link #SERVICE_META_DATA}.
     * </p>
     *
     * @return The root node if this service can retrieve window content.
     * @see AccessibilityWindowInfo#isActive() for more explanation about the active window.
     */
    public AccessibilityNodeInfo getRootInActiveWindow() {
        return getRootInActiveWindow(AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID);
    }

    /**
     * Gets the root node in the currently active window if this service
     * can retrieve window content. The active window is the one that the user
     * is currently touching or the window with input focus, if the user is not
     * touching any window. It could be from any logical display.
     *
     * @param prefetchingStrategy the prefetching strategy.
     * @return The root node if this service can retrieve window content.
     *
     * @see #getRootInActiveWindow()
     * @see AccessibilityNodeInfo#getParent(int) for a description of prefetching.
     */
    @Nullable
    public AccessibilityNodeInfo getRootInActiveWindow(
            @AccessibilityNodeInfo.PrefetchingStrategy int prefetchingStrategy) {
        return AccessibilityInteractionClient.getInstance(this).getRootInActiveWindow(
                mConnectionId, prefetchingStrategy);
    }

    /**
     * Disables the service. After calling this method, the service will be disabled and settings
     * will show that it is turned off.
     */
    public final void disableSelf() {
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.disableSelf();
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }
    }

    @NonNull
    @Override
    public Context createDisplayContext(Display display) {
        return new AccessibilityContext(super.createDisplayContext(display), mConnectionId);
    }

    @NonNull
    @Override
    public Context createWindowContext(int type, @Nullable Bundle options) {
        final Context context = super.createWindowContext(type, options);
        if (type != TYPE_ACCESSIBILITY_OVERLAY) {
            return context;
        }
        return new AccessibilityContext(context, mConnectionId);
    }

    @NonNull
    @Override
    public Context createWindowContext(@NonNull Display display, int type,
            @Nullable Bundle options) {
        final Context context = super.createWindowContext(display, type, options);
        if (type != TYPE_ACCESSIBILITY_OVERLAY) {
            return context;
        }
        return new AccessibilityContext(context, mConnectionId);
    }

    /**
     * Returns the magnification controller, which may be used to query and
     * modify the state of display magnification.
     * <p>
     * <strong>Note:</strong> In order to control magnification, your service
     * must declare the capability by setting the
     * {@link android.R.styleable#AccessibilityService_canControlMagnification}
     * property in its meta-data. For more information, see
     * {@link #SERVICE_META_DATA}.
     *
     * @return the magnification controller
     */
    @NonNull
    public final MagnificationController getMagnificationController() {
        return getMagnificationController(Display.DEFAULT_DISPLAY);
    }

    /**
     * Returns the magnification controller of specified logical display, which may be used to
     * query and modify the state of display magnification.
     * <p>
     * <strong>Note:</strong> In order to control magnification, your service
     * must declare the capability by setting the
     * {@link android.R.styleable#AccessibilityService_canControlMagnification}
     * property in its meta-data. For more information, see
     * {@link #SERVICE_META_DATA}.
     *
     * @param displayId The logic display id, use {@link Display#DEFAULT_DISPLAY} for
     *                  default display.
     * @return the magnification controller
     *
     * @hide
     */
    @NonNull
    public final MagnificationController getMagnificationController(int displayId) {
        synchronized (mLock) {
            MagnificationController controller = mMagnificationControllers.get(displayId);
            if (controller == null) {
                controller = new MagnificationController(this, mLock, displayId);
                mMagnificationControllers.put(displayId, controller);
            }
            return controller;
        }
    }

    /**
     * Get the controller for fingerprint gestures. This feature requires {@link
     * AccessibilityServiceInfo#CAPABILITY_CAN_REQUEST_FINGERPRINT_GESTURES}.
     *
     *<strong>Note: </strong> The service must be connected before this method is called.
     *
     * @return The controller for fingerprint gestures, or {@code null} if gestures are unavailable.
     */
    @RequiresPermission(android.Manifest.permission.USE_FINGERPRINT)
    public final @NonNull FingerprintGestureController getFingerprintGestureController() {
        if (mFingerprintGestureController == null) {
            mFingerprintGestureController = new FingerprintGestureController(
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId));
        }
        return mFingerprintGestureController;
    }

    /**
     * Dispatch a gesture to the touch screen. Any gestures currently in progress, whether from
     * the user, this service, or another service, will be cancelled.
     * <p>
     * The gesture will be dispatched as if it were performed directly on the screen by a user, so
     * the events may be affected by features such as magnification and explore by touch.
     * </p>
     * <p>
     * <strong>Note:</strong> In order to dispatch gestures, your service
     * must declare the capability by setting the
     * {@link android.R.styleable#AccessibilityService_canPerformGestures}
     * property in its meta-data. For more information, see
     * {@link #SERVICE_META_DATA}.
     * </p>
     * <p>Since many apps do not appropriately support {@link AccessibilityAction#ACTION_CLICK},
     * if this action fails on an element that should be clickable, a service that is not a screen
     * reader may send a tap directly to the element as a fallback. The example below
     * demonstrates this fallback using the gesture dispatch APIs:
     *
     * <pre class="prettyprint"><code>
     *     private void tap(PointF point) {
     *         StrokeDescription tap =  new StrokeDescription(path(point), 0,
     *         ViewConfiguration.getTapTimeout());
     *         GestureDescription.Builder builder = new GestureDescription.Builder();
     *         builder.addStroke(tap);
     *         dispatchGesture(builder.build(), null, null);
     *     }
     *</code>
     * </pre>
     * @param gesture The gesture to dispatch
     * @param callback The object to call back when the status of the gesture is known. If
     * {@code null}, no status is reported.
     * @param handler The handler on which to call back the {@code callback} object. If
     * {@code null}, the object is called back on the service's main thread.
     *
     * @return {@code true} if the gesture is dispatched, {@code false} if not.
     */
    public final boolean dispatchGesture(@NonNull GestureDescription gesture,
            @Nullable GestureResultCallback callback,
            @Nullable Handler handler) {
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection == null) {
            return false;
        }
        int sampleTimeMs = calculateGestureSampleTimeMs(gesture.getDisplayId());
        List<GestureDescription.GestureStep> steps =
                MotionEventGenerator.getGestureStepsFromGestureDescription(gesture, sampleTimeMs);
        try {
            synchronized (mLock) {
                mGestureStatusCallbackSequence++;
                if (callback != null) {
                    if (mGestureStatusCallbackInfos == null) {
                        mGestureStatusCallbackInfos = new SparseArray<>();
                    }
                    GestureResultCallbackInfo callbackInfo = new GestureResultCallbackInfo(gesture,
                            callback, handler);
                    mGestureStatusCallbackInfos.put(mGestureStatusCallbackSequence, callbackInfo);
                }
                connection.dispatchGesture(mGestureStatusCallbackSequence,
                        new ParceledListSlice<>(steps), gesture.getDisplayId());
            }
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
        return true;
    }

    /**
     * Returns the sample time in millis of gesture steps for the current display.
     *
     * <p>For gestures to be smooth they should line up with the refresh rate of the display.
     * On versions of Android before R, the sample time was fixed to 100ms.
     */
    private int calculateGestureSampleTimeMs(int displayId) {
        if (getApplicationInfo().targetSdkVersion <= Build.VERSION_CODES.Q) {
            return 100;
        }
        Display display = getSystemService(DisplayManager.class).getDisplay(
                displayId);
        if (display == null) {
            return 100;
        }
        int msPerSecond = 1000;
        int sampleTimeMs = (int) (msPerSecond / display.getRefreshRate());
        if (sampleTimeMs < 1) {
            // Should be impossible, but do not return 0.
            return 100;
        }
        return sampleTimeMs;
    }

    void onPerformGestureResult(int sequence, final boolean completedSuccessfully) {
        if (mGestureStatusCallbackInfos == null) {
            return;
        }
        GestureResultCallbackInfo callbackInfo;
        synchronized (mLock) {
            callbackInfo = mGestureStatusCallbackInfos.get(sequence);
            mGestureStatusCallbackInfos.remove(sequence);
        }
        final GestureResultCallbackInfo finalCallbackInfo = callbackInfo;
        if ((callbackInfo != null) && (callbackInfo.gestureDescription != null)
                && (callbackInfo.callback != null)) {
            if (callbackInfo.handler != null) {
                callbackInfo.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (completedSuccessfully) {
                            finalCallbackInfo.callback
                                    .onCompleted(finalCallbackInfo.gestureDescription);
                        } else {
                            finalCallbackInfo.callback
                                    .onCancelled(finalCallbackInfo.gestureDescription);
                        }
                    }
                });
                return;
            }
            if (completedSuccessfully) {
                callbackInfo.callback.onCompleted(callbackInfo.gestureDescription);
            } else {
                callbackInfo.callback.onCancelled(callbackInfo.gestureDescription);
            }
        }
    }

    private void onMagnificationChanged(int displayId, @NonNull Region region,
            MagnificationConfig config) {
        MagnificationController controller;
        synchronized (mLock) {
            controller = mMagnificationControllers.get(displayId);
        }
        if (controller != null) {
            controller.dispatchMagnificationChanged(region, config);
        }
    }

    /**
     * Callback for fingerprint gesture handling
     * @param active If gesture detection is active
     */
    private void onFingerprintCapturingGesturesChanged(boolean active) {
        getFingerprintGestureController().onGestureDetectionActiveChanged(active);
    }

    /**
     * Callback for fingerprint gesture handling
     * @param gesture The identifier for the gesture performed
     */
    private void onFingerprintGesture(int gesture) {
        getFingerprintGestureController().onGesture(gesture);
    }

    /** @hide */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public int getConnectionId() {
        return mConnectionId;
    }

    /**
     * Used to control and query the state of display magnification.
     */
    public static final class MagnificationController {
        private final AccessibilityService mService;
        private final int mDisplayId;

        /**
         * Map of listeners to their handlers. Lazily created when adding the
         * first magnification listener.
         */
        private ArrayMap<OnMagnificationChangedListener, Handler> mListeners;
        private final Object mLock;

        MagnificationController(@NonNull AccessibilityService service, @NonNull Object lock,
                int displayId) {
            mService = service;
            mLock = lock;
            mDisplayId = displayId;
        }

        /**
         * Called when the service is connected.
         */
        void onServiceConnectedLocked() {
            if (mListeners != null && !mListeners.isEmpty()) {
                setMagnificationCallbackEnabled(true);
            }
        }

        /**
         * Adds the specified change listener to the list of magnification
         * change listeners. The callback will occur on the service's main
         * thread.
         *
         * @param listener the listener to add, must be non-{@code null}
         */
        public void addListener(@NonNull OnMagnificationChangedListener listener) {
            addListener(listener, null);
        }

        /**
         * Adds the specified change listener to the list of magnification
         * change listeners. The callback will occur on the specified
         * {@link Handler}'s thread, or on the service's main thread if the
         * handler is {@code null}.
         *
         * @param listener the listener to add, must be non-null
         * @param handler the handler on which the callback should execute, or
         *        {@code null} to execute on the service's main thread
         */
        public void addListener(@NonNull OnMagnificationChangedListener listener,
                @Nullable Handler handler) {
            synchronized (mLock) {
                if (mListeners == null) {
                    mListeners = new ArrayMap<>();
                }

                final boolean shouldEnableCallback = mListeners.isEmpty();
                mListeners.put(listener, handler);

                if (shouldEnableCallback) {
                    // This may fail if the service is not connected yet, but if we
                    // still have listeners when it connects then we can try again.
                    setMagnificationCallbackEnabled(true);
                }
            }
        }

        /**
         * Removes the specified change listener from the list of magnification change listeners.
         *
         * @param listener the listener to remove, must be non-null
         * @return {@code true} if the listener was removed, {@code false} otherwise
         */
        public boolean removeListener(@NonNull OnMagnificationChangedListener listener) {
            if (mListeners == null) {
                return false;
            }

            synchronized (mLock) {
                final int keyIndex = mListeners.indexOfKey(listener);
                final boolean hasKey = keyIndex >= 0;
                if (hasKey) {
                    mListeners.removeAt(keyIndex);
                }

                if (hasKey && mListeners.isEmpty()) {
                    // We just removed the last listener, so we don't need
                    // callbacks from the service anymore.
                    setMagnificationCallbackEnabled(false);
                }

                return hasKey;
            }
        }

        private void setMagnificationCallbackEnabled(boolean enabled) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    connection.setMagnificationCallbackEnabled(mDisplayId, enabled);
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
        }

        /**
         * Dispatches magnification changes to any registered listeners. This
         * should be called on the service's main thread.
         */
        void dispatchMagnificationChanged(final @NonNull Region region,
                final MagnificationConfig config) {
            final ArrayMap<OnMagnificationChangedListener, Handler> entries;
            synchronized (mLock) {
                if (mListeners == null || mListeners.isEmpty()) {
                    Slog.d(LOG_TAG, "Received magnification changed "
                            + "callback with no listeners registered!");
                    setMagnificationCallbackEnabled(false);
                    return;
                }

                // Listeners may remove themselves. Perform a shallow copy to avoid concurrent
                // modification.
                entries = new ArrayMap<>(mListeners);
            }

            for (int i = 0, count = entries.size(); i < count; i++) {
                final OnMagnificationChangedListener listener = entries.keyAt(i);
                final Handler handler = entries.valueAt(i);
                if (handler != null) {
                    handler.post(() -> {
                        listener.onMagnificationChanged(MagnificationController.this,
                                region, config);
                    });
                } else {
                    // We're already on the main thread, just run the listener.
                    listener.onMagnificationChanged(this, region, config);
                }
            }
        }

        /**
         * Gets the {@link MagnificationConfig} of the controlling magnifier on the display.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will
         * return null.
         * </p>
         *
         * @return the magnification config that the service controls
         */
        public @Nullable MagnificationConfig getMagnificationConfig() {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationConfig(mDisplayId);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to obtain magnification config", re);
                    re.rethrowFromSystemServer();
                }
            }
            return null;
        }

        /**
         * Returns the current magnification scale.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will
         * return a default value of {@code 1.0f}.
         * </p>
         * <p>
         * <strong>Note:</strong> This legacy API gets the scale of full-screen
         * magnification. To get the scale of the current controlling magnifier,
         * use {@link #getMagnificationConfig} instead.
         * </p>
         *
         * @return the current magnification scale
         * @deprecated Use {@link #getMagnificationConfig()} instead
         */
        @Deprecated
        public float getScale() {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationScale(mDisplayId);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to obtain scale", re);
                    re.rethrowFromSystemServer();
                }
            }
            return 1.0f;
        }

        /**
         * Returns the unscaled screen-relative X coordinate of the focal
         * center of the magnified region. This is the point around which
         * zooming occurs and is guaranteed to lie within the magnified
         * region.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will
         * return a default value of {@code 0.0f}.
         * </p>
         * <p>
         * <strong>Note:</strong> This legacy API gets the center position of full-screen
         * magnification. To get the magnification center of the current controlling magnifier,
         * use {@link #getMagnificationConfig} instead.
         * </p>
         *
         * @return the unscaled screen-relative X coordinate of the center of
         *         the magnified region
         * @deprecated Use {@link #getMagnificationConfig()} instead
         */
        @Deprecated
        public float getCenterX() {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationCenterX(mDisplayId);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to obtain center X", re);
                    re.rethrowFromSystemServer();
                }
            }
            return 0.0f;
        }

        /**
         * Returns the unscaled screen-relative Y coordinate of the focal
         * center of the magnified region. This is the point around which
         * zooming occurs and is guaranteed to lie within the magnified
         * region.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will
         * return a default value of {@code 0.0f}.
         * </p>
         * <p>
         * <strong>Note:</strong> This legacy API gets the center position of full-screen
         * magnification. To get the magnification center of the current controlling magnifier,
         * use {@link #getMagnificationConfig} instead.
         * </p>
         *
         * @return the unscaled screen-relative Y coordinate of the center of
         *         the magnified region
         * @deprecated Use {@link #getMagnificationConfig()} instead
         */
        @Deprecated
        public float getCenterY() {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationCenterY(mDisplayId);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to obtain center Y", re);
                    re.rethrowFromSystemServer();
                }
            }
            return 0.0f;
        }

        /**
         * Returns the region of the screen currently active for magnification. Changes to
         * magnification scale and center only affect this portion of the screen. The rest of the
         * screen, for example input methods, cannot be magnified. This region is relative to the
         * unscaled screen and is independent of the scale and center point.
         * <p>
         * The returned region will be empty if magnification is not active. Magnification is active
         * if magnification gestures are enabled or if a service is running that can control
         * magnification.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will
         * return an empty region.
         * </p>
         * <p>
         * <strong>Note:</strong> This legacy API gets the magnification region of full-screen
         * magnification. To get the magnification region of the current controlling magnifier,
         * use {@link #getCurrentMagnificationRegion()} instead.
         * </p>
         *
         * @return the region of the screen currently active for magnification, or an empty region
         * if magnification is not active.
         * @deprecated Use {@link #getCurrentMagnificationRegion()} instead
         */
        @Deprecated
        @NonNull
        public Region getMagnificationRegion() {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationRegion(mDisplayId);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to obtain magnified region", re);
                    re.rethrowFromSystemServer();
                }
            }
            return Region.obtain();
        }

        /**
         * Returns the region of the screen currently active for magnification if the
         * controlling magnification is {@link MagnificationConfig#MAGNIFICATION_MODE_FULLSCREEN}.
         * Returns the region of screen projected on the magnification window if the
         * controlling magnification is {@link MagnificationConfig#MAGNIFICATION_MODE_WINDOW}.
         *
         * <p>
         * If the controlling mode is {@link MagnificationConfig#MAGNIFICATION_MODE_FULLSCREEN},
         * the returned region will be empty if the magnification is
         * not active. And the magnification is active if magnification gestures are enabled
         * or if a service is running that can control magnification.
         * </p><p>
         * If the controlling mode is {@link MagnificationConfig#MAGNIFICATION_MODE_WINDOW},
         * the returned region will be empty if the magnification is not activated.
         * </p><p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will
         * return an empty region.
         * </p>
         *
         * @return the magnification region of the currently controlling magnification
         */
        @NonNull
        public Region getCurrentMagnificationRegion() {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getCurrentMagnificationRegion(mDisplayId);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to obtain the current magnified region", re);
                    re.rethrowFromSystemServer();
                }
            }
            return Region.obtain();
        }

        /**
         * Resets magnification scale and center to their default (e.g. no
         * magnification) values.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will have
         * no effect and return {@code false}.
         * <p>
         * <strong>Note:</strong> This legacy API reset full-screen magnification.
         * To reset the current controlling magnifier, use
         * {@link #resetCurrentMagnification(boolean)} ()} instead.
         * </p>
         *
         * @param animate {@code true} to animate from the current scale and
         *                center or {@code false} to reset the scale and center
         *                immediately
         * @return {@code true} on success, {@code false} on failure
         */
        public boolean reset(boolean animate) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.resetMagnification(mDisplayId, animate);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to reset", re);
                    re.rethrowFromSystemServer();
                }
            }
            return false;
        }

        /**
         * Resets magnification scale and center of the controlling magnification
         * to their default (e.g. no magnification) values.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will have
         * no effect and return {@code false}.
         * </p>
         *
         * @param animate {@code true} to animate from the current scale and
         *                center or {@code false} to reset the scale and center
         *                immediately
         * @return {@code true} on success, {@code false} on failure
         */
        public boolean resetCurrentMagnification(boolean animate) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.resetCurrentMagnification(mDisplayId, animate);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to reset", re);
                    re.rethrowFromSystemServer();
                }
            }
            return false;
        }

        /**
         * Sets the {@link MagnificationConfig}. The service controls the magnification by
         * setting the config.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will have
         * no effect and return {@code false}.
         * </p>
         *
         * @param config the magnification config
         * @param animate {@code true} to animate from the current spec or
         *                {@code false} to set the spec immediately
         * @return {@code true} on success, {@code false} on failure
         */
        public boolean setMagnificationConfig(@NonNull MagnificationConfig config,
                boolean animate) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setMagnificationConfig(mDisplayId, config, animate);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to set magnification config", re);
                    re.rethrowFromSystemServer();
                }
            }
            return false;
        }

        /**
         * Sets the magnification scale.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will have
         * no effect and return {@code false}.
         * <p>
         * <strong>Note:</strong> This legacy API sets the scale of full-screen
         * magnification. To set the scale of the specified magnifier,
         * use {@link #setMagnificationConfig} instead.
         * </p>
         *
         * @param scale the magnification scale to set, must be >= 1 and <= 8
         * @param animate {@code true} to animate from the current scale or
         *                {@code false} to set the scale immediately
         * @return {@code true} on success, {@code false} on failure
         * @deprecated Use {@link #setMagnificationConfig(MagnificationConfig, boolean)} instead
         */
        @Deprecated
        public boolean setScale(float scale, boolean animate) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    final MagnificationConfig config = new MagnificationConfig.Builder()
                            .setMode(MAGNIFICATION_MODE_FULLSCREEN)
                            .setScale(scale).build();
                    return connection.setMagnificationConfig(mDisplayId, config, animate);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to set scale", re);
                    re.rethrowFromSystemServer();
                }
            }
            return false;
        }

        /**
         * Sets the center of the magnified viewport.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been
         * called) or the service has been disconnected, this method will have
         * no effect and return {@code false}.
         * </p>
         * <p>
         * <strong>Note:</strong> This legacy API sets the center of full-screen
         * magnification. To set the center of the specified magnifier,
         * use {@link #setMagnificationConfig} instead.
         * </p>
         *
         * @param centerX the unscaled screen-relative X coordinate on which to
         *                center the viewport
         * @param centerY the unscaled screen-relative Y coordinate on which to
         *                center the viewport
         * @param animate {@code true} to animate from the current viewport
         *                center or {@code false} to set the center immediately
         * @return {@code true} on success, {@code false} on failure
         * @deprecated Use {@link #setMagnificationConfig(MagnificationConfig, boolean)} instead
         */
        @Deprecated
        public boolean setCenter(float centerX, float centerY, boolean animate) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    final MagnificationConfig config = new MagnificationConfig.Builder()
                            .setMode(MAGNIFICATION_MODE_FULLSCREEN)
                            .setCenterX(centerX).setCenterY(centerY).build();
                    return connection.setMagnificationConfig(mDisplayId, config, animate);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to set center", re);
                    re.rethrowFromSystemServer();
                }
            }
            return false;
        }

        /**
         * Listener for changes in the state of magnification.
         */
        public interface OnMagnificationChangedListener {
            /**
             * Called when the magnified region, scale, or center changes.
             * <p>
             * <strong>Note:</strong> This legacy callback notifies only full-screen
             * magnification change.
             * </p>
             *
             * @param controller the magnification controller
             * @param region the magnification region
             * @param scale the new scale
             * @param centerX the new X coordinate, in unscaled coordinates, around which
             * magnification is focused
             * @param centerY the new Y coordinate, in unscaled coordinates, around which
             * magnification is focused
             * @deprecated Override
             * {@link #onMagnificationChanged(MagnificationController, Region, MagnificationConfig)}
             * instead
             */
            @Deprecated
            void onMagnificationChanged(@NonNull MagnificationController controller,
                    @NonNull Region region, float scale, float centerX, float centerY);

            /**
             * Called when the magnified region, mode, scale, or center changes of
             * all magnification modes.
             * <p>
             * <strong>Note:</strong> This method can be overridden to listen to the
             * magnification changes of all magnification modes then the legacy callback
             * would not receive the notifications.
             * Skipping calling super when overriding this method results in
             * {@link #onMagnificationChanged(MagnificationController, Region, float, float, float)}
             * not getting called.
             * </p>
             *
             * @param controller the magnification controller
             * @param region the magnification region
             *               If the config mode is
             *               {@link MagnificationConfig#MAGNIFICATION_MODE_FULLSCREEN},
             *               it is the region of the screen currently active for magnification.
             *               that is the same region as {@link #getMagnificationRegion()}.
             *               If the config mode is
             *               {@link MagnificationConfig#MAGNIFICATION_MODE_WINDOW},
             *               it is the region of screen projected on the magnification window.
             * @param config The magnification config. That has the controlling magnification
             *               mode, the new scale and the new screen-relative center position
             */
            default void onMagnificationChanged(@NonNull MagnificationController controller,
                    @NonNull Region region, @NonNull MagnificationConfig config) {
                if (config.getMode() == MAGNIFICATION_MODE_FULLSCREEN) {
                    onMagnificationChanged(controller, region,
                            config.getScale(), config.getCenterX(), config.getCenterY());
                }
            }
        }
    }

    /**
     * Returns the soft keyboard controller, which may be used to query and modify the soft keyboard
     * show mode.
     *
     * @return the soft keyboard controller
     */
    @NonNull
    public final SoftKeyboardController getSoftKeyboardController() {
        synchronized (mLock) {
            if (mSoftKeyboardController == null) {
                mSoftKeyboardController = new SoftKeyboardController(this, mLock);
            }
            return mSoftKeyboardController;
        }
    }

    /**
     * The default implementation returns our default {@link InputMethod}. Subclasses can override
     * it to provide their own customized version. Accessibility services need to set the
     * {@link AccessibilityServiceInfo#FLAG_INPUT_METHOD_EDITOR} flag to use input method APIs.
     *
     * @return the InputMethod.
     */
    @NonNull
    public InputMethod onCreateInputMethod() {
        return new InputMethod(this);
    }

    /**
     * Returns the InputMethod instance after the system calls {@link #onCreateInputMethod()},
     * which may be used to input text or get editable text selection change notifications. It will
     * return null if the accessibility service doesn't set the
     * {@link AccessibilityServiceInfo#FLAG_INPUT_METHOD_EDITOR} flag or the system doesn't call
     * {@link #onCreateInputMethod()}.
     *
     * @return the InputMethod instance
     */
    @Nullable
    public final InputMethod getInputMethod() {
        return mInputMethod;
    }

    private void onSoftKeyboardShowModeChanged(int showMode) {
        if (mSoftKeyboardController != null) {
            mSoftKeyboardController.dispatchSoftKeyboardShowModeChanged(showMode);
        }
    }

    /**
     * Used to control, query, and listen for changes to the soft keyboard show mode.
     * <p>
     * Accessibility services may request to override the decisions normally made about whether or
     * not the soft keyboard is shown.
     * <p>
     * If multiple services make conflicting requests, the last request is honored. A service may
     * register a listener to find out if the mode has changed under it.
     * <p>
     * If the user takes action to override the behavior behavior requested by an accessibility
     * service, the user's request takes precendence, the show mode will be reset to
     * {@link AccessibilityService#SHOW_MODE_AUTO}, and services will no longer be able to control
     * that aspect of the soft keyboard's behavior.
     * <p>
     * Note: Because soft keyboards are independent apps, the framework does not have total control
     * over their behavior. They may choose to show themselves, or not, without regard to requests
     * made here. So the framework will make a best effort to deliver the behavior requested, but
     * cannot guarantee success.
     *
     * @see AccessibilityService#SHOW_MODE_AUTO
     * @see AccessibilityService#SHOW_MODE_HIDDEN
     * @see AccessibilityService#SHOW_MODE_IGNORE_HARD_KEYBOARD
     */
    public static final class SoftKeyboardController {
        private final AccessibilityService mService;

        /**
         * Map of listeners to their handlers. Lazily created when adding the first
         * soft keyboard change listener.
         */
        private ArrayMap<OnShowModeChangedListener, Handler> mListeners;
        private final Object mLock;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                ENABLE_IME_SUCCESS,
                ENABLE_IME_FAIL_BY_ADMIN,
                ENABLE_IME_FAIL_UNKNOWN
        })
        public @interface EnableImeResult {}
        /**
         * Return value for {@link #setInputMethodEnabled(String, boolean)}. The action succeeded.
         */
        public static final int ENABLE_IME_SUCCESS = 0;
        /**
         * Return value for {@link #setInputMethodEnabled(String, boolean)}. The action failed
         * because the InputMethod is not permitted by device policy manager.
         */
        public static final int ENABLE_IME_FAIL_BY_ADMIN = 1;
        /**
         * Return value for {@link #setInputMethodEnabled(String, boolean)}. The action failed
         * and the reason is unknown.
         */
        public static final int ENABLE_IME_FAIL_UNKNOWN = 2;

        SoftKeyboardController(@NonNull AccessibilityService service, @NonNull Object lock) {
            mService = service;
            mLock = lock;
        }

        /**
         * Called when the service is connected.
         */
        void onServiceConnected() {
            synchronized(mLock) {
                if (mListeners != null && !mListeners.isEmpty()) {
                    setSoftKeyboardCallbackEnabled(true);
                }
            }
        }

        /**
         * Adds the specified change listener to the list of show mode change listeners. The
         * callback will occur on the service's main thread. Listener is not called on registration.
         */
        public void addOnShowModeChangedListener(@NonNull OnShowModeChangedListener listener) {
            addOnShowModeChangedListener(listener, null);
        }

        /**
         * Adds the specified change listener to the list of soft keyboard show mode change
         * listeners. The callback will occur on the specified {@link Handler}'s thread, or on the
         * services's main thread if the handler is {@code null}.
         *
         * @param listener the listener to add, must be non-null
         * @param handler the handler on which to callback should execute, or {@code null} to
         *        execute on the service's main thread
         */
        public void addOnShowModeChangedListener(@NonNull OnShowModeChangedListener listener,
                @Nullable Handler handler) {
            synchronized (mLock) {
                if (mListeners == null) {
                    mListeners = new ArrayMap<>();
                }

                final boolean shouldEnableCallback = mListeners.isEmpty();
                mListeners.put(listener, handler);

                if (shouldEnableCallback) {
                    // This may fail if the service is not connected yet, but if we still have
                    // listeners when it connects, we can try again.
                    setSoftKeyboardCallbackEnabled(true);
                }
            }
        }

        /**
         * Removes the specified change listener from the list of keyboard show mode change
         * listeners.
         *
         * @param listener the listener to remove, must be non-null
         * @return {@code true} if the listener was removed, {@code false} otherwise
         */
        public boolean removeOnShowModeChangedListener(
                @NonNull OnShowModeChangedListener listener) {
            if (mListeners == null) {
                return false;
            }

            synchronized (mLock) {
                final int keyIndex = mListeners.indexOfKey(listener);
                final boolean hasKey = keyIndex >= 0;
                if (hasKey) {
                    mListeners.removeAt(keyIndex);
                }

                if (hasKey && mListeners.isEmpty()) {
                    // We just removed the last listener, so we don't need callbacks from the
                    // service anymore.
                    setSoftKeyboardCallbackEnabled(false);
                }

                return hasKey;
            }
        }

        private void setSoftKeyboardCallbackEnabled(boolean enabled) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    connection.setSoftKeyboardCallbackEnabled(enabled);
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
        }

        /**
         * Dispatches the soft keyboard show mode change to any registered listeners. This should
         * be called on the service's main thread.
         */
        void dispatchSoftKeyboardShowModeChanged(final int showMode) {
            final ArrayMap<OnShowModeChangedListener, Handler> entries;
            synchronized (mLock) {
                if (mListeners == null || mListeners.isEmpty()) {
                    Slog.w(LOG_TAG, "Received soft keyboard show mode changed callback"
                            + " with no listeners registered!");
                    setSoftKeyboardCallbackEnabled(false);
                    return;
                }

                // Listeners may remove themselves. Perform a shallow copy to avoid concurrent
                // modification.
                entries = new ArrayMap<>(mListeners);
            }

            for (int i = 0, count = entries.size(); i < count; i++) {
                final OnShowModeChangedListener listener = entries.keyAt(i);
                final Handler handler = entries.valueAt(i);
                if (handler != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onShowModeChanged(SoftKeyboardController.this, showMode);
                        }
                    });
                } else {
                    // We're already on the main thread, just run the listener.
                    listener.onShowModeChanged(this, showMode);
                }
            }
        }

        /**
         * Returns the show mode of the soft keyboard.
         *
         * @return the current soft keyboard show mode
         *
         * @see AccessibilityService#SHOW_MODE_AUTO
         * @see AccessibilityService#SHOW_MODE_HIDDEN
         * @see AccessibilityService#SHOW_MODE_IGNORE_HARD_KEYBOARD
         */
        @SoftKeyboardShowMode
        public int getShowMode() {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getSoftKeyboardShowMode();
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to set soft keyboard behavior", re);
                    re.rethrowFromSystemServer();
                }
            }
            return SHOW_MODE_AUTO;
        }

        /**
         * Sets the soft keyboard show mode.
         * <p>
         * <strong>Note:</strong> If the service is not yet connected (e.g.
         * {@link AccessibilityService#onServiceConnected()} has not yet been called) or the
         * service has been disconnected, this method will have no effect and return {@code false}.
         *
         * @param showMode the new show mode for the soft keyboard
         * @return {@code true} on success
         *
         * @see AccessibilityService#SHOW_MODE_AUTO
         * @see AccessibilityService#SHOW_MODE_HIDDEN
         * @see AccessibilityService#SHOW_MODE_IGNORE_HARD_KEYBOARD
         */
        public boolean setShowMode(@SoftKeyboardShowMode int showMode) {
           final IAccessibilityServiceConnection connection =
                   AccessibilityInteractionClient.getInstance(mService).getConnection(
                           mService.mConnectionId);
           if (connection != null) {
               try {
                   return connection.setSoftKeyboardShowMode(showMode);
               } catch (RemoteException re) {
                   Log.w(LOG_TAG, "Failed to set soft keyboard behavior", re);
                   re.rethrowFromSystemServer();
               }
           }
           return false;
        }

        /**
         * Listener for changes in the soft keyboard show mode.
         */
        public interface OnShowModeChangedListener {
           /**
            * Called when the soft keyboard behavior changes. The default show mode is
            * {@code SHOW_MODE_AUTO}, where the soft keyboard is shown when a text input field is
            * focused. An AccessibilityService can also request the show mode
            * {@code SHOW_MODE_HIDDEN}, where the soft keyboard is never shown.
            *
            * @param controller the soft keyboard controller
            * @param showMode the current soft keyboard show mode
            */
            void onShowModeChanged(@NonNull SoftKeyboardController controller,
                    @SoftKeyboardShowMode int showMode);
        }

        /**
         * Switches the current IME for the user for whom the service is enabled. The change will
         * persist until the current IME is explicitly changed again, and may persist beyond the
         * life cycle of the requesting service.
         *
         * @param imeId The ID of the input method to make current. This IME must be installed and
         *              enabled.
         * @return {@code true} if the current input method was successfully switched to the input
         *         method by {@code imeId},
         *         {@code false} if the input method specified is not installed, not enabled, or
         *         otherwise not available to become the current IME
         *
         * @see android.view.inputmethod.InputMethodInfo#getId()
         */
        public boolean switchToInputMethod(@NonNull String imeId) {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.switchToInputMethod(imeId);
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
            return false;
        }

        /**
         * Enable or disable the specified IME for the user for whom the service is activated. The
         * IME needs to be in the same package as the service and needs to be allowed by device
         * policy, if there is one. The change will persist until the specified IME is next
         * explicitly enabled or disabled by whatever means, such as user choice, and may persist
         * beyond the life cycle of the requesting service.
         *
         * @param imeId The ID of the input method to enable or disable. This IME must be installed.
         * @param enabled {@code true} if the input method associated with {@code imeId} should be
         *                enabled.
         * @return status code for the result of enabling/disabling the input method associated
         *         with {@code imeId}.
         * @throws SecurityException if the input method is not in the same package as the service.
         *
         * @see android.view.inputmethod.InputMethodInfo#getId()
         */
        @CheckResult
        @EnableImeResult
        public int setInputMethodEnabled(@NonNull String imeId, boolean enabled)
                throws SecurityException {
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getInstance(mService).getConnection(
                            mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setInputMethodEnabled(imeId, enabled);
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
            return ENABLE_IME_FAIL_UNKNOWN;
        }
    }

    /**
     * Returns the controller for the accessibility button within the system's navigation area.
     * This instance may be used to query the accessibility button's state and register listeners
     * for interactions with and state changes for the accessibility button when
     * {@link AccessibilityServiceInfo#FLAG_REQUEST_ACCESSIBILITY_BUTTON} is set.
     * <p>
     * <strong>Note:</strong> Not all devices are capable of displaying the accessibility button
     * within a navigation area, and as such, use of this class should be considered only as an
     * optional feature or shortcut on supported device implementations.
     * </p>
     *
     * @return the accessibility button controller for this {@link AccessibilityService}
     */
    @NonNull
    public final AccessibilityButtonController getAccessibilityButtonController() {
        return getAccessibilityButtonController(Display.DEFAULT_DISPLAY);
    }

    /**
     * Returns the controller of specified logical display for the accessibility button within the
     * system's navigation area. This instance may be used to query the accessibility button's
     * state and register listeners for interactions with and state changes for the accessibility
     * button when {@link AccessibilityServiceInfo#FLAG_REQUEST_ACCESSIBILITY_BUTTON} is set.
     * <p>
     * <strong>Note:</strong> Not all devices are capable of displaying the accessibility button
     * within a navigation area, and as such, use of this class should be considered only as an
     * optional feature or shortcut on supported device implementations.
     * </p>
     *
     * @param displayId The logic display id, use {@link Display#DEFAULT_DISPLAY} for default
     *                  display.
     * @return the accessibility button controller for this {@link AccessibilityService}
     */
    @NonNull
    public final AccessibilityButtonController getAccessibilityButtonController(int displayId) {
        synchronized (mLock) {
            AccessibilityButtonController controller = mAccessibilityButtonControllers.get(
                    displayId);
            if (controller == null) {
                controller = new AccessibilityButtonController(
                    AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId));
                mAccessibilityButtonControllers.put(displayId, controller);
            }
            return controller;
        }
    }

    private void onAccessibilityButtonClicked(int displayId) {
        getAccessibilityButtonController(displayId).dispatchAccessibilityButtonClicked();
    }

    private void onAccessibilityButtonAvailabilityChanged(boolean available) {
        getAccessibilityButtonController().dispatchAccessibilityButtonAvailabilityChanged(
                available);
    }

    /** Sets the cache status.
     *
     * <p>If {@code enabled}, enable the cache and prefetching. Otherwise, disable the cache
     * and prefetching.
     * Note: By default the cache is enabled.
     * @param enabled whether to enable or disable the cache.
     * @return {@code true} if the cache and connection are not null, so the cache status is set.
     */
    public boolean setCacheEnabled(boolean enabled) {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getConnection(mConnectionId);
        if (connection == null) {
            return false;
        }
        try {
            connection.setCacheEnabled(enabled);
            cache.setEnabled(enabled);
            return true;
        } catch (RemoteException re) {
            Log.w(LOG_TAG, "Error while setting status of cache", re);
            re.rethrowFromSystemServer();
        }
        return false;
    }

    /** Invalidates {@code node} and its subtree in the cache.
     * @param node the node to invalidate.
     * @return {@code true} if the subtree rooted at {@code node} was invalidated.
     */
    public boolean clearCachedSubtree(@NonNull AccessibilityNodeInfo node) {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        return cache.clearSubTree(node);
    }

    /** Clears the cache.
     * @return {@code true} if the cache was cleared
     */
    public boolean clearCache() {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        cache.clear();
        return true;
    }

    /** Checks if {@code node} is in the cache.
     * @param node the node to check.
     * @return {@code true} if {@code node} is in the cache.
     */
    public boolean isNodeInCache(@NonNull AccessibilityNodeInfo node) {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        return cache.isNodeInCache(node);
    }

    /** Returns {@code true} if the cache is enabled. */
    public boolean isCacheEnabled() {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        return cache.isEnabled();
    }

    /** This is called when the system action list is changed. */
    public void onSystemActionsChanged() {
    }

    /**
     * Returns a list of system actions available in the system right now.
     * <p>
     * System actions that correspond to the global action constants will have matching action IDs.
     * For example, an with id {@link #GLOBAL_ACTION_BACK} will perform the back action.
     * </p>
     * <p>
     * These actions should be called by {@link #performGlobalAction}.
     * </p>
     *
     * @return A list of available system actions.
     */
    public final @NonNull List<AccessibilityAction> getSystemActions() {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                return connection.getSystemActions();
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while calling getSystemActions", re);
                re.rethrowFromSystemServer();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Performs a global action. Such an action can be performed
     * at any moment regardless of the current application or user
     * location in that application. For example going back, going
     * home, opening recents, etc.
     *
     * <p>
     * Note: The global action ids themselves give no information about the current availability
     * of their corresponding actions. To determine if a global action is available, use
     * {@link #getSystemActions()}
     *
     * @param action The action to perform.
     * @return Whether the action was successfully performed.
     *
     * Perform actions using ids like the id constants referenced below:
     * @see #GLOBAL_ACTION_BACK
     * @see #GLOBAL_ACTION_HOME
     * @see #GLOBAL_ACTION_NOTIFICATIONS
     * @see #GLOBAL_ACTION_RECENTS
     * @see #GLOBAL_ACTION_DPAD_UP
     * @see #GLOBAL_ACTION_DPAD_DOWN
     * @see #GLOBAL_ACTION_DPAD_LEFT
     * @see #GLOBAL_ACTION_DPAD_RIGHT
     * @see #GLOBAL_ACTION_DPAD_CENTER
     */
    public final boolean performGlobalAction(int action) {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                return connection.performGlobalAction(action);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while calling performGlobalAction", re);
                re.rethrowFromSystemServer();
            }
        }
        return false;
    }

    /**
     * Find the view that has the specified focus type. The search is performed
     * across all windows.
     * <p>
     * <strong>Note:</strong> In order to access the windows your service has
     * to declare the capability to retrieve window content by setting the
     * {@link android.R.styleable#AccessibilityService_canRetrieveWindowContent}
     * property in its meta-data. For details refer to {@link #SERVICE_META_DATA}.
     * Also the service has to opt-in to retrieve the interactive windows by
     * setting the {@link AccessibilityServiceInfo#FLAG_RETRIEVE_INTERACTIVE_WINDOWS}
     * flag. Otherwise, the search will be performed only in the active window.
     * </p>
     * <p>
     * <strong>Note:</strong> If the view with {@link AccessibilityNodeInfo#FOCUS_INPUT}
     * is on an embedded view hierarchy which is embedded in a {@link android.view.SurfaceView} via
     * {@link android.view.SurfaceView#setChildSurfacePackage}, there is a limitation that this API
     * won't be able to find the node for the view. It's because views don't know about
     * the embedded hierarchies. Instead, you could traverse all the nodes to find the
     * focus.
     * </p>
     *
     * @param focus The focus to find. One of {@link AccessibilityNodeInfo#FOCUS_INPUT} or
     *         {@link AccessibilityNodeInfo#FOCUS_ACCESSIBILITY}.
     * @return The node info of the focused view or null.
     *
     * @see AccessibilityNodeInfo#FOCUS_INPUT
     * @see AccessibilityNodeInfo#FOCUS_ACCESSIBILITY
     */
    public AccessibilityNodeInfo findFocus(int focus) {
        return AccessibilityInteractionClient.getInstance(this).findFocus(mConnectionId,
                AccessibilityWindowInfo.ANY_WINDOW_ID, AccessibilityNodeInfo.ROOT_NODE_ID, focus);
    }

    /**
     * Gets the an {@link AccessibilityServiceInfo} describing this
     * {@link AccessibilityService}. This method is useful if one wants
     * to change some of the dynamically configurable properties at
     * runtime.
     *
     * @return The accessibility service info.
     *
     * @see AccessibilityServiceInfo
     */
    public final AccessibilityServiceInfo getServiceInfo() {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                return connection.getServiceInfo();
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while getting AccessibilityServiceInfo", re);
                re.rethrowFromSystemServer();
            }
        }
        return null;
    }

    /**
     * Sets the {@link AccessibilityServiceInfo} that describes this service.
     * <p>
     * Note: You can call this method any time but the info will be picked up after
     *       the system has bound to this service and when this method is called thereafter.
     *
     * @param info The info.
     */
    public final void setServiceInfo(AccessibilityServiceInfo info) {
        mInfo = info;
        updateInputMethod(info);
        mMotionEventSources = info.getMotionEventSources();
        sendServiceInfo();
    }

    /**
     * Sets the {@link AccessibilityServiceInfo} for this service if the latter is
     * properly set and there is an {@link IAccessibilityServiceConnection} to the
     * AccessibilityManagerService.
     */
    private void sendServiceInfo() {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (mInfo != null && connection != null) {
            try {
                connection.setServiceInfo(mInfo);
                mInfo = null;
                AccessibilityInteractionClient.getInstance(this).clearCache(mConnectionId);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", re);
                re.rethrowFromSystemServer();
            }
        }
    }

    @Override
    public Object getSystemService(@ServiceName @NonNull String name) {
        if (getBaseContext() == null) {
            throw new IllegalStateException(
                    "System services not available to Activities before onCreate()");
        }

        // Guarantee that we always return the same window manager instance.
        if (WINDOW_SERVICE.equals(name)) {
            if (mWindowManager == null) {
                mWindowManager = (WindowManager) getBaseContext().getSystemService(name);
                final WindowManagerImpl wm = (WindowManagerImpl) mWindowManager;
                // Set e default token obtained from the connection to ensure client could use
                // accessibility overlay.
                wm.setDefaultToken(mWindowToken);
            }
            return mWindowManager;
        }
        return super.getSystemService(name);
    }

    /**
     * Takes a screenshot of the specified display and returns it via an
     * {@link AccessibilityService.ScreenshotResult}. You can use {@link Bitmap#wrapHardwareBuffer}
     * to construct the bitmap from the ScreenshotResult's payload.
     * <p>
     * <strong>Note:</strong> In order to take screenshot your service has
     * to declare the capability to take screenshot by setting the
     * {@link android.R.styleable#AccessibilityService_canTakeScreenshot}
     * property in its meta-data. For details refer to {@link #SERVICE_META_DATA}.
     * </p>
     *
     * @param displayId The logic display id, must be {@link Display#DEFAULT_DISPLAY} for
     *                  default display.
     * @param executor Executor on which to run the callback.
     * @param callback The callback invoked when taking screenshot has succeeded or failed.
     *                 See {@link TakeScreenshotCallback} for details.
     * @see #takeScreenshotOfWindow
     */
    public void takeScreenshot(int displayId, @NonNull @CallbackExecutor Executor executor,
            @NonNull TakeScreenshotCallback callback) {
        Preconditions.checkNotNull(executor, "executor cannot be null");
        Preconditions.checkNotNull(callback, "callback cannot be null");
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection == null) {
            sendScreenshotFailure(ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR, executor, callback);
            return;
        }
        try {
            connection.takeScreenshot(displayId, new RemoteCallback((result) -> {
                final int status = result.getInt(KEY_ACCESSIBILITY_SCREENSHOT_STATUS);
                if (status != TAKE_SCREENSHOT_SUCCESS) {
                    sendScreenshotFailure(status, executor, callback);
                    return;
                }
                final HardwareBuffer hardwareBuffer =
                        result.getParcelable(KEY_ACCESSIBILITY_SCREENSHOT_HARDWAREBUFFER, android.hardware.HardwareBuffer.class);
                final ParcelableColorSpace colorSpace =
                        result.getParcelable(KEY_ACCESSIBILITY_SCREENSHOT_COLORSPACE,
                                android.graphics.ParcelableColorSpace.class);
                final ScreenshotResult screenshot = new ScreenshotResult(hardwareBuffer,
                        colorSpace.getColorSpace(),
                        result.getLong(KEY_ACCESSIBILITY_SCREENSHOT_TIMESTAMP));
                sendScreenshotSuccess(screenshot, executor, callback);
            }));
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    /**
     * Takes a screenshot of the specified window and returns it via an
     * {@link AccessibilityService.ScreenshotResult}. You can use {@link Bitmap#wrapHardwareBuffer}
     * to construct the bitmap from the ScreenshotResult's payload.
     * <p>
     * <strong>Note:</strong> In order to take screenshots your service has
     * to declare the capability to take screenshot by setting the
     * {@link android.R.styleable#AccessibilityService_canTakeScreenshot}
     * property in its meta-data. For details refer to {@link #SERVICE_META_DATA}.
     * </p>
     * <p>
     * Both this method and {@link #takeScreenshot} can be used for machine learning-based visual
     * screen understanding. Use <code>takeScreenshotOfWindow</code> if your target window might be
     * visually underneath an accessibility overlay (from your or another accessibility service) in
     * order to capture the window contents without the screenshot being covered by the overlay
     * contents drawn on the screen.
     * </p>
     *
     * @param accessibilityWindowId The window id, from {@link AccessibilityWindowInfo#getId()}.
     * @param executor Executor on which to run the callback.
     * @param callback The callback invoked when taking screenshot has succeeded or failed.
     *                 See {@link TakeScreenshotCallback} for details.
     * @see #takeScreenshot
     */
    public void takeScreenshotOfWindow(int accessibilityWindowId,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull TakeScreenshotCallback callback) {
        AccessibilityInteractionClient.getInstance(this).takeScreenshotOfWindow(
                        mConnectionId, accessibilityWindowId, executor, callback);
    }

    /**
     * Sets the strokeWidth and color of the accessibility focus rectangle.
     * <p>
     * <strong>Note:</strong> This setting persists until this or another active
     * AccessibilityService changes it or the device reboots.
     * </p>
     *
     * @param strokeWidth The stroke width of the rectangle in pixels.
     *                    Setting this value to zero results in no focus rectangle being drawn.
     * @param color The color of the rectangle.
     */
    public void setAccessibilityFocusAppearance(int strokeWidth, @ColorInt int color) {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.setFocusAppearance(strokeWidth, color);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while setting the strokeWidth and color of the "
                        + "accessibility focus rectangle", re);
                re.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Implement to return the implementation of the internal accessibility
     * service interface.
     */
    @Override
    public final IBinder onBind(Intent intent) {
        return new IAccessibilityServiceClientWrapper(this, getMainExecutor(), new Callbacks() {
            @Override
            public void onServiceConnected() {
                AccessibilityService.this.dispatchServiceConnected();
            }

            @Override
            public void onInterrupt() {
                AccessibilityService.this.onInterrupt();
            }

            @Override
            public void onAccessibilityEvent(AccessibilityEvent event) {
                AccessibilityService.this.onAccessibilityEvent(event);
            }

            @Override
            public void init(int connectionId, IBinder windowToken) {
                mConnectionId = connectionId;
                mWindowToken = windowToken;

                // The client may have already obtained the window manager, so
                // update the default token on whatever manager we gave them.
                if (mWindowManager != null) {
                    final WindowManagerImpl wm = (WindowManagerImpl) mWindowManager;
                    wm.setDefaultToken(mWindowToken);
                }
            }

            @Override
            public boolean onGesture(AccessibilityGestureEvent gestureEvent) {
                return AccessibilityService.this.onGesture(gestureEvent);
            }

            @Override
            public boolean onKeyEvent(KeyEvent event) {
                return AccessibilityService.this.onKeyEvent(event);
            }

            @Override
            public void onMagnificationChanged(int displayId, @NonNull Region region,
                    MagnificationConfig config) {
                AccessibilityService.this.onMagnificationChanged(displayId, region, config);
            }

            @Override
            public void onMotionEvent(MotionEvent event) {
                AccessibilityService.this.sendMotionEventToCallback(event);
            }

            @Override
            public void onTouchStateChanged(int displayId, int state) {
                AccessibilityService.this.onTouchStateChanged(displayId, state);
            }

            @Override
            public void onSoftKeyboardShowModeChanged(int showMode) {
                AccessibilityService.this.onSoftKeyboardShowModeChanged(showMode);
            }

            @Override
            public void onPerformGestureResult(int sequence, boolean completedSuccessfully) {
                AccessibilityService.this.onPerformGestureResult(sequence, completedSuccessfully);
            }

            @Override
            public void onFingerprintCapturingGesturesChanged(boolean active) {
                AccessibilityService.this.onFingerprintCapturingGesturesChanged(active);
            }

            @Override
            public void onFingerprintGesture(int gesture) {
                AccessibilityService.this.onFingerprintGesture(gesture);
            }

            @Override
            public void onAccessibilityButtonClicked(int displayId) {
                AccessibilityService.this.onAccessibilityButtonClicked(displayId);
            }

            @Override
            public void onAccessibilityButtonAvailabilityChanged(boolean available) {
                AccessibilityService.this.onAccessibilityButtonAvailabilityChanged(available);
            }

            @Override
            public void onSystemActionsChanged() {
                AccessibilityService.this.onSystemActionsChanged();
            }

            @Override
            public void createImeSession(IAccessibilityInputMethodSessionCallback callback) {
                if (mInputMethod != null) {
                    mInputMethod.createImeSession(callback);
                }
            }

            @Override
            public void startInput(@Nullable RemoteAccessibilityInputConnection connection,
                    @NonNull EditorInfo editorInfo, boolean restarting) {
                if (mInputMethod != null) {
                    if (restarting) {
                        mInputMethod.restartInput(connection, editorInfo);
                    } else {
                        mInputMethod.startInput(connection, editorInfo);
                    }
                }
            }
        });
    }

    /**
     * Implements the internal {@link IAccessibilityServiceClient} interface to convert
     * incoming calls to it back to calls on an {@link AccessibilityService}.
     *
     * @hide
     */
    public static class IAccessibilityServiceClientWrapper extends
            IAccessibilityServiceClient.Stub {

        private final Callbacks mCallback;
        private final Context mContext;
        private final Executor mExecutor;

        private int mConnectionId = AccessibilityInteractionClient.NO_ID;

        /**
         * This is not {@code null} only between {@link #bindInput()} and {@link #unbindInput()} so
         * that {@link RemoteAccessibilityInputConnection} can query if {@link #unbindInput()} has
         * already been called or not, mainly to avoid unnecessary blocking operations.
         *
         * <p>This field must be set and cleared only from the binder thread(s), where the system
         * guarantees that {@link #bindInput()},
         * {@link #startInput(IRemoteAccessibilityInputConnection, EditorInfo, boolean)},
         * and {@link #unbindInput()} are called with the same order as the original calls
         * in {@link com.android.server.inputmethod.InputMethodManagerService}.
         * See {@link IBinder#FLAG_ONEWAY} for detailed semantics.</p>
         */
        @Nullable
        CancellationGroup mCancellationGroup = null;

        public IAccessibilityServiceClientWrapper(Context context, Executor executor,
                Callbacks callback) {
            mCallback = callback;
            mContext = context;
            mExecutor = executor;
        }

        public IAccessibilityServiceClientWrapper(Context context, Looper looper,
                Callbacks callback) {
            this(context, new HandlerExecutor(new Handler(looper)), callback);
        }

        public void init(IAccessibilityServiceConnection connection, int connectionId,
                IBinder windowToken) {
            mExecutor.execute(() -> {
                mConnectionId = connectionId;
                if (connection != null) {
                    AccessibilityInteractionClient.getInstance(mContext).addConnection(
                            mConnectionId, connection, /*initializeCache=*/true);
                    if (mContext != null) {
                        try {
                            connection.setAttributionTag(mContext.getAttributionTag());
                        } catch (RemoteException re) {
                            Log.w(LOG_TAG, "Error while setting attributionTag", re);
                            re.rethrowFromSystemServer();
                        }
                    }
                    mCallback.init(mConnectionId, windowToken);
                    mCallback.onServiceConnected();
                } else {
                    AccessibilityInteractionClient.getInstance(mContext)
                            .clearCache(mConnectionId);
                    AccessibilityInteractionClient.getInstance(mContext).removeConnection(
                            mConnectionId);
                    mConnectionId = AccessibilityInteractionClient.NO_ID;
                    mCallback.init(AccessibilityInteractionClient.NO_ID, null);
                }
                return;
            });
        }

        public void onInterrupt() {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onInterrupt();
                }
            });
        }

        public void onAccessibilityEvent(AccessibilityEvent event, boolean serviceWantsEvent) {
            mExecutor.execute(() -> {
                if (event != null) {
                    // Send the event to AccessibilityCache via AccessibilityInteractionClient
                    AccessibilityInteractionClient.getInstance(mContext).onAccessibilityEvent(
                            event, mConnectionId);
                    if (serviceWantsEvent
                            && (mConnectionId != AccessibilityInteractionClient.NO_ID)) {
                        // Send the event to AccessibilityService
                        mCallback.onAccessibilityEvent(event);
                    }
                }
                return;
            });
        }

        @Override
        public void onGesture(AccessibilityGestureEvent gestureInfo) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onGesture(gestureInfo);
                }
                return;
            });
        }

        public void clearAccessibilityCache() {
            mExecutor.execute(() -> {
                AccessibilityInteractionClient.getInstance(mContext).clearCache(mConnectionId);
                return;
            });
        }

        @Override
        public void onKeyEvent(KeyEvent event, int sequence) {
            mExecutor.execute(() -> {
                try {
                    IAccessibilityServiceConnection connection = AccessibilityInteractionClient
                            .getInstance(mContext).getConnection(mConnectionId);
                    if (connection != null) {
                        final boolean result = mCallback.onKeyEvent(event);
                        try {
                            connection.setOnKeyEventResult(result, sequence);
                        } catch (RemoteException re) {
                            /* ignore */
                        }
                    }
                } finally {
                    // Make sure the event is recycled.
                    try {
                        event.recycle();
                    } catch (IllegalStateException ise) {
                        /* ignore - best effort */
                    }
                }
                return;
            });
        }

        /** Magnification changed callbacks for different displays */
        public void onMagnificationChanged(int displayId, @NonNull Region region,
                MagnificationConfig config) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onMagnificationChanged(displayId, region, config);
                }
                return;
            });
        }

        public void onSoftKeyboardShowModeChanged(int showMode) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onSoftKeyboardShowModeChanged(showMode);
                }
                return;
            });
        }

        public void onPerformGestureResult(int sequence, boolean successfully) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onPerformGestureResult(sequence, successfully);
                }
                return;
            });
        }

        public void onFingerprintCapturingGesturesChanged(boolean active) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onFingerprintCapturingGesturesChanged(active);
                }
                return;
            });
        }

        public void onFingerprintGesture(int gesture) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onFingerprintGesture(gesture);
                }
                return;
            });
        }

        /** Accessibility button clicked callbacks for different displays */
        public void onAccessibilityButtonClicked(int displayId) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onAccessibilityButtonClicked(displayId);
                }
                return;
            });
        }

        public void onAccessibilityButtonAvailabilityChanged(boolean available) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onAccessibilityButtonAvailabilityChanged(available);
                }
                return;
            });
        }

        /** This is called when the system action list is changed. */
        public void onSystemActionsChanged() {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.onSystemActionsChanged();
                }
                return;
            });
        }

        /** This is called when an app requests ime sessions or when the service is enabled. */
        public void createImeSession(IAccessibilityInputMethodSessionCallback callback) {
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    mCallback.createImeSession(callback);
                }
            });
        }

        /**
         * This is called when InputMethodManagerService requests to set the session enabled or
         * disabled
         */
        public void setImeSessionEnabled(IAccessibilityInputMethodSession session,
                boolean enabled) {
            try {
                AccessibilityInputMethodSession ls =
                        ((AccessibilityInputMethodSessionWrapper) session).getSession();
                if (ls == null) {
                    Log.w(LOG_TAG, "Session is already finished: " + session);
                    return;
                }
                mExecutor.execute(() -> {
                    if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                        ls.setEnabled(enabled);
                    }
                    return;
                });
            } catch (ClassCastException e) {
                Log.w(LOG_TAG, "Incoming session not of correct type: " + session, e);
            }
        }

        /** This is called when an app binds input or when the service is enabled. */
        public void bindInput() {
            if (mCancellationGroup != null) {
                Log.e(LOG_TAG, "bindInput must be paired with unbindInput.");
            }
            mCancellationGroup = new CancellationGroup();
        }

        /** This is called when an app unbinds input or when the service is disabled. */
        public void unbindInput() {
            if (mCancellationGroup != null) {
                // Signal the flag then forget it.
                mCancellationGroup.cancelAll();
                mCancellationGroup = null;
            } else {
                Log.e(LOG_TAG, "unbindInput must be paired with bindInput.");
            }
        }

        /** This is called when an app starts input or when the service is enabled. */
        public void startInput(IRemoteAccessibilityInputConnection connection,
                EditorInfo editorInfo, boolean restarting) {
            if (mCancellationGroup == null) {
                Log.e(LOG_TAG, "startInput must be called after bindInput.");
                mCancellationGroup = new CancellationGroup();
            }
            mExecutor.execute(() -> {
                if (mConnectionId != AccessibilityInteractionClient.NO_ID) {
                    final RemoteAccessibilityInputConnection ic = connection == null ? null
                            : new RemoteAccessibilityInputConnection(
                                    connection, mCancellationGroup);
                    editorInfo.makeCompatible(mContext.getApplicationInfo().targetSdkVersion);
                    mCallback.startInput(ic, editorInfo, restarting);
                }
            });
        }

        @Override
        public void onMotionEvent(MotionEvent event) {
            mExecutor.execute(() -> {
                mCallback.onMotionEvent(event);
            });
        }

        @Override
        public void onTouchStateChanged(int displayId, int state) {
            mExecutor.execute(() -> {
                mCallback.onTouchStateChanged(displayId, state);
            });
        }
    }

    /**
     * Class used to report status of dispatched gestures
     */
    public static abstract class GestureResultCallback {
        /** Called when the gesture has completed successfully
         *
         * @param gestureDescription The description of the gesture that completed.
         */
        public void onCompleted(GestureDescription gestureDescription) {
        }

        /** Called when the gesture was cancelled
         *
         * @param gestureDescription The description of the gesture that was cancelled.
         */
        public void onCancelled(GestureDescription gestureDescription) {
        }
    }

    /* Object to keep track of gesture result callbacks */
    private static class GestureResultCallbackInfo {
        GestureDescription gestureDescription;
        GestureResultCallback callback;
        Handler handler;

        GestureResultCallbackInfo(GestureDescription gestureDescription,
                GestureResultCallback callback, Handler handler) {
            this.gestureDescription = gestureDescription;
            this.callback = callback;
            this.handler = handler;
        }
    }

    private void sendScreenshotSuccess(ScreenshotResult screenshot, Executor executor,
            TakeScreenshotCallback callback) {
        executor.execute(() -> callback.onSuccess(screenshot));
    }

    private void sendScreenshotFailure(@ScreenshotErrorCode int errorCode, Executor executor,
            TakeScreenshotCallback callback) {
        executor.execute(() -> callback.onFailure(errorCode));
    }

    /**
     * Interface used to report status of taking screenshot.
     */
    public interface TakeScreenshotCallback {
        /** Called when taking screenshot has completed successfully.
         *
         * @param screenshot The content of screenshot.
         */
        void onSuccess(@NonNull ScreenshotResult screenshot);

        /** Called when taking screenshot has failed. {@code errorCode} will identify the
         * reason of failure.
         *
         * @param errorCode The error code of this operation.
         */
        void onFailure(@ScreenshotErrorCode int errorCode);
    }

    /**
     * Can be used to construct a bitmap of the screenshot or any other operations for
     * {@link AccessibilityService#takeScreenshot} API.
     */
    public static final class ScreenshotResult {
        private final @NonNull HardwareBuffer mHardwareBuffer;
        private final @NonNull ColorSpace mColorSpace;
        private final long mTimestamp;

        /** @hide */
        public ScreenshotResult(@NonNull HardwareBuffer hardwareBuffer,
                @NonNull ColorSpace colorSpace, long timestamp) {
            Preconditions.checkNotNull(hardwareBuffer, "hardwareBuffer cannot be null");
            Preconditions.checkNotNull(colorSpace, "colorSpace cannot be null");
            mHardwareBuffer = hardwareBuffer;
            mColorSpace = colorSpace;
            mTimestamp = timestamp;
        }

        /**
         * Gets the {@link ColorSpace} identifying a specific organization of colors of the
         * screenshot.
         *
         * @return the color space
         */
        @NonNull
        public ColorSpace getColorSpace() {
            return mColorSpace;
        }

        /**
         * Gets the {@link HardwareBuffer} representing a memory buffer of the screenshot.
         * <p>
         * <strong>Note:</strong> The application should call {@link HardwareBuffer#close()} when
         * the buffer is no longer needed to free the underlying resources.
         * </p>
         *
         * @return the hardware buffer
         */
        @NonNull
        public HardwareBuffer getHardwareBuffer() {
            return mHardwareBuffer;
        }

        /**
         * Gets the timestamp of taking the screenshot.
         *
         * @return milliseconds of non-sleep uptime before screenshot since boot and it's from
         * {@link SystemClock#uptimeMillis()}
         */
        public long getTimestamp() {
            return mTimestamp;
        };
    }

    /**
     * When {@link AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE} is enabled, this
     * function requests that touch interactions starting in the specified region of the screen
     * bypass the gesture detector. There can only be one gesture detection passthrough region per
     * display. Requesting a new gesture detection passthrough region clears the existing one. To
     * disable this passthrough and return to the original behavior, pass in an empty region. When
     * {@link AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE} is disabled this
     * function has no effect.
     *
     * @param displayId The display on which to set this region.
     * @param region the region of the screen.
     */
    public void setGestureDetectionPassthroughRegion(int displayId, @NonNull Region region) {
        Preconditions.checkNotNull(region, "region cannot be null");
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.setGestureDetectionPassthroughRegion(displayId, region);
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }
    }

    /**
     * When {@link AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE} is enabled, this
     * function requests that touch interactions starting in the specified region of the screen
     * bypass the touch explorer and go straight to the view hierarchy. There can only be one touch
     * exploration passthrough region per display. Requesting a new touch explorationpassthrough
     * region clears the existing one. To disable this passthrough and return to the original
     * behavior, pass in an empty region. When {@link
     * AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE} is disabled this function has
     * no effect.
     *
     * @param displayId The display on which to set this region.
     * @param region the region of the screen .
     */
    public void setTouchExplorationPassthroughRegion(int displayId, @NonNull Region region) {
        Preconditions.checkNotNull(region, "region cannot be null");
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.setTouchExplorationPassthroughRegion(displayId, region);
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }
    }

    /**
     * Sets the system settings values that control the scaling factor for animations. The scale
     * controls the animation playback speed for animations that respect these settings. Animations
     * that do not respect the settings values will not be affected by this function. A lower scale
     * value results in a faster speed. A value of <code>0</code> disables animations entirely. When
     * animations are disabled services receive window change events more quickly which can reduce
     * the potential by confusion by reducing the time during which windows are in transition.
     *
     * @see AccessibilityEvent#TYPE_WINDOWS_CHANGED
     * @see AccessibilityEvent#TYPE_WINDOW_STATE_CHANGED
     * @see android.provider.Settings.Global#WINDOW_ANIMATION_SCALE
     * @see android.provider.Settings.Global#TRANSITION_ANIMATION_SCALE
     * @see android.provider.Settings.Global#ANIMATOR_DURATION_SCALE
     * @param scale The scaling factor for all animations.
     */
    public void setAnimationScale(float scale) {
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance(this).getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.setAnimationScale(scale);
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }
    }

    private static class AccessibilityContext extends ContextWrapper {
        private final int mConnectionId;

        private AccessibilityContext(Context base, int connectionId) {
            super(base);
            mConnectionId = connectionId;
            setDefaultTokenInternal(this, getDisplayId());
        }

        @NonNull
        @Override
        public Context createDisplayContext(Display display) {
            return new AccessibilityContext(super.createDisplayContext(display), mConnectionId);
        }

        @NonNull
        @Override
        public Context createWindowContext(int type, @Nullable Bundle options) {
            final Context context = super.createWindowContext(type, options);
            if (type != TYPE_ACCESSIBILITY_OVERLAY) {
                return context;
            }
            return new AccessibilityContext(context, mConnectionId);
        }

        @NonNull
        @Override
        public Context createWindowContext(@NonNull Display display, int type,
                @Nullable Bundle options) {
            final Context context = super.createWindowContext(display, type, options);
            if (type != TYPE_ACCESSIBILITY_OVERLAY) {
                return context;
            }
            return new AccessibilityContext(context, mConnectionId);
        }

        private void setDefaultTokenInternal(Context context, int displayId) {
            final WindowManagerImpl wm = (WindowManagerImpl) context.getSystemService(
                    WINDOW_SERVICE);
            final IAccessibilityServiceConnection connection =
                    AccessibilityInteractionClient.getConnection(mConnectionId);
            IBinder token = null;
            if (connection != null) {
                try {
                    token = connection.getOverlayWindowToken(displayId);
                } catch (RemoteException re) {
                    Log.w(LOG_TAG, "Failed to get window token", re);
                    re.rethrowFromSystemServer();
                }
                wm.setDefaultToken(token);
            }
        }
    }

    /**
     * Returns the touch interaction controller for the specified logical display, which may be used
     * to detect gestures and otherwise control touch interactions. If
     * {@link AccessibilityServiceInfo#FLAG_REQUEST_TOUCH_EXPLORATION_MODE} is disabled the
     * controller's methods will have no effect.
     *
     * @param displayId The logical display id, use {@link Display#DEFAULT_DISPLAY} for default
     *                      display.
     * @return the TouchExploration controller
     */
    @NonNull
    public final TouchInteractionController getTouchInteractionController(int displayId) {
        synchronized (mLock) {
            TouchInteractionController controller = mTouchInteractionControllers.get(displayId);
            if (controller == null) {
                controller = new TouchInteractionController(this, mLock, displayId);
                mTouchInteractionControllers.put(displayId, controller);
            }
            return controller;
        }
    }

    void sendMotionEventToCallback(MotionEvent event) {
        boolean sendingTouchEventToTouchInteractionController = false;
        if (event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)) {
            TouchInteractionController controller;
            synchronized (mLock) {
                int displayId = event.getDisplayId();
                controller = mTouchInteractionControllers.get(displayId);
            }
            if (controller != null) {
                sendingTouchEventToTouchInteractionController = true;
                controller.onMotionEvent(event);
            }
        }
        final int eventSourceWithoutClass = event.getSource() & ~InputDevice.SOURCE_CLASS_MASK;
        if ((mMotionEventSources & eventSourceWithoutClass) != 0
                && !sendingTouchEventToTouchInteractionController) {
            onMotionEvent(event);
        }
    }

    void onTouchStateChanged(int displayId, int state) {
        TouchInteractionController controller;
        synchronized (mLock) {
            controller = mTouchInteractionControllers.get(displayId);
        }
        if (controller != null) {
            controller.onStateChanged(state);
        }
    }

    /**
     * Attaches a {@link android.view.SurfaceControl} containing an accessibility overlay to the
     * specified display. This type of overlay should be used for content that does not need to
     * track the location and size of Views in the currently active app e.g. service configuration
     * or general service UI.
     *
     * <p>Generally speaking, an accessibility overlay will be a {@link android.view.View}. To embed
     * the View into a {@link android.view.SurfaceControl}, create a {@link
     * android.view.SurfaceControlViewHost} and attach the View using {@link
     * android.view.SurfaceControlViewHost#setView}. Then obtain the SurfaceControl by calling
     * <code> viewHost.getSurfacePackage().getSurfaceControl()</code>.
     *
     * <p>To remove this overlay and free the associated resources, use <code>
     *  new SurfaceControl.Transaction().reparent(sc, null).apply();</code>.
     *
     * <p>If the specified overlay has already been attached to the specified display this method
     * does nothing. If the specified overlay has already been attached to a previous display this
     * function will transfer the overlay to the new display. Services can attach multiple overlays.
     * Use <code> new SurfaceControl.Transaction().setLayer(sc, layer).apply();</code>. to
     * coordinate the order of the overlays on screen.
     *
     * @param displayId the display to which the SurfaceControl should be attached.
     * @param sc the SurfaceControl containing the overlay content
     *
     */
    public void attachAccessibilityOverlayToDisplay(int displayId, @NonNull SurfaceControl sc) {
        Preconditions.checkNotNull(sc, "SurfaceControl cannot be null");
        AccessibilityInteractionClient.getInstance(this)
                .attachAccessibilityOverlayToDisplay(mConnectionId, displayId, sc, null, null);
    }

    /**
     * Attaches a {@link android.view.SurfaceControl} containing an accessibility overlay to the
     * specified display. This type of overlay should be used for content that does not need to
     * track the location and size of Views in the currently active app e.g. service configuration
     * or general service UI.
     *
     * <p>Generally speaking, an accessibility overlay will be a {@link android.view.View}. To embed
     * the View into a {@link android.view.SurfaceControl}, create a {@link
     * android.view.SurfaceControlViewHost} and attach the View using {@link
     * android.view.SurfaceControlViewHost#setView}. Then obtain the SurfaceControl by calling
     * <code> viewHost.getSurfacePackage().getSurfaceControl()</code>.
     *
     * <p>To remove this overlay and free the associated resources, use <code>
     *  new SurfaceControl.Transaction().reparent(sc, null).apply();</code>.
     *
     * <p>If the specified overlay has already been attached to the specified display this method
     * does nothing. If the specified overlay has already been attached to a previous display this
     * function will transfer the overlay to the new display. Services can attach multiple overlays.
     * Use <code> new SurfaceControl.Transaction().setLayer(sc, layer).apply();</code>. to
     * coordinate the order of the overlays on screen.
     *
     * @param displayId the display to which the SurfaceControl should be attached.
     * @param sc the SurfaceControl containing the overlay content
     * @param executor Executor on which to run the callback.
     * @param callback The callback invoked when attaching the overlay has succeeded or failed. The
     *     callback is a {@link java.util.function.IntConsumer} of the result status code.
     * @see #OVERLAY_RESULT_SUCCESS
     * @see #OVERLAY_RESULT_INVALID
     * @see #OVERLAY_RESULT_INTERNAL_ERROR
     */
    @FlaggedApi(android.view.accessibility.Flags.FLAG_A11Y_OVERLAY_CALLBACKS)
    public final void attachAccessibilityOverlayToDisplay(
            int displayId,
            @NonNull SurfaceControl sc,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull IntConsumer callback) {
        Preconditions.checkNotNull(sc, "SurfaceControl cannot be null");
        AccessibilityInteractionClient.getInstance(this)
                .attachAccessibilityOverlayToDisplay(
                        mConnectionId, displayId, sc, executor, callback);
    }

    /**
     * Attaches an accessibility overlay {@link android.view.SurfaceControl} to the specified
     * window. This method should be used when you want the overlay to move and resize as the parent
     * window moves and resizes.
     *
     * <p>Generally speaking, an accessibility overlay will be a {@link android.view.View}. To embed
     * the View into a {@link android.view.SurfaceControl}, create a {@link
     * android.view.SurfaceControlViewHost} and attach the View using {@link
     * android.view.SurfaceControlViewHost#setView}. Then obtain the SurfaceControl by calling
     * <code> viewHost.getSurfacePackage().getSurfaceControl()</code>.
     *
     * <p>To remove this overlay and free the associated resources, use <code>
     *  new SurfaceControl.Transaction().reparent(sc, null).apply();</code>.
     *
     * <p>If the specified overlay has already been attached to the specified window this method
     * does nothing. If the specified overlay has already been attached to a previous window this
     * function will transfer the overlay to the new window. Services can attach multiple overlays.
     * Use <code> new SurfaceControl.Transaction().setLayer(sc, layer).apply();</code>. to
     * coordinate the order of the overlays on screen.
     *
     * @param accessibilityWindowId The window id, from {@link AccessibilityWindowInfo#getId()}.
     * @param sc the SurfaceControl containing the overlay content
     *
     */
    public void attachAccessibilityOverlayToWindow(
            int accessibilityWindowId, @NonNull SurfaceControl sc) {
        Preconditions.checkNotNull(sc, "SurfaceControl cannot be null");
        AccessibilityInteractionClient.getInstance(this)
                .attachAccessibilityOverlayToWindow(
                        mConnectionId, accessibilityWindowId, sc, null, null);
    }

    /**
     * Attaches an accessibility overlay {@link android.view.SurfaceControl} to the specified
     * window. This method should be used when you want the overlay to move and resize as the parent
     * window moves and resizes.
     *
     * <p>Generally speaking, an accessibility overlay will be a {@link android.view.View}. To embed
     * the View into a {@link android.view.SurfaceControl}, create a {@link
     * android.view.SurfaceControlViewHost} and attach the View using {@link
     * android.view.SurfaceControlViewHost#setView}. Then obtain the SurfaceControl by calling
     * <code> viewHost.getSurfacePackage().getSurfaceControl()</code>.
     *
     * <p>To remove this overlay and free the associated resources, use <code>
     *  new SurfaceControl.Transaction().reparent(sc, null).apply();</code>.
     *
     * <p>If the specified overlay has already been attached to the specified window this method
     * does nothing. If the specified overlay has already been attached to a previous window this
     * function will transfer the overlay to the new window. Services can attach multiple overlays.
     * Use <code> new SurfaceControl.Transaction().setLayer(sc, layer).apply();</code>. to
     * coordinate the order of the overlays on screen.
     *
     * @param accessibilityWindowId The window id, from {@link AccessibilityWindowInfo#getId()}.
     * @param sc the SurfaceControl containing the overlay content
     * @param executor Executor on which to run the callback.
     * @param callback The callback invoked when attaching the overlay has succeeded or failed. The
     *     callback is a {@link java.util.function.IntConsumer} of the result status code.
     * @see #OVERLAY_RESULT_SUCCESS
     * @see #OVERLAY_RESULT_INVALID
     * @see #OVERLAY_RESULT_INTERNAL_ERROR
     */
    @FlaggedApi(android.view.accessibility.Flags.FLAG_A11Y_OVERLAY_CALLBACKS)
    public final void attachAccessibilityOverlayToWindow(
            int accessibilityWindowId,
            @NonNull SurfaceControl sc,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull IntConsumer callback) {
        Preconditions.checkNotNull(sc, "SurfaceControl cannot be null");
        AccessibilityInteractionClient.getInstance(this)
                .attachAccessibilityOverlayToWindow(
                        mConnectionId, accessibilityWindowId, sc, executor, callback);
    }

    /**
     * Returns the {@link BrailleDisplayController} which may be used to communicate with
     * refreshable Braille displays that provide USB or Bluetooth Braille display HID support.
     */
    @FlaggedApi(android.view.accessibility.Flags.FLAG_BRAILLE_DISPLAY_HID)
    @NonNull
    public final BrailleDisplayController getBrailleDisplayController() {
        BrailleDisplayController.checkApiFlagIsEnabled();
        synchronized (mLock) {
            if (mBrailleDisplayController == null) {
                mBrailleDisplayController = new BrailleDisplayControllerImpl(this, mLock);
            }
            return mBrailleDisplayController;
        }
    }
}
