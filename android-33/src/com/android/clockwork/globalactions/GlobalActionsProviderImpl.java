package com.android.clockwork.globalactions;

import static com.android.wearable.resources.R.bool.config_systemTracePowerMenu;
import static com.android.wearable.resources.R.drawable.ic_emergency_dial;
import static com.android.wearable.resources.R.drawable.ic_emergency_star;
import static com.android.wearable.resources.R.drawable.ic_screen_record;
import static com.android.wearable.resources.R.drawable.ic_system_trace;
import static com.android.wearable.resources.R.drawable.ic_gm_language;
import static com.android.wearable.resources.R.string.global_action_emergency_dial;
import static com.android.wearable.resources.R.string.global_action_record_screen;
import static com.android.wearable.resources.R.string.global_action_system_trace;
import static com.android.wearable.resources.R.string.global_actions_title;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.globalactions.Action;
import com.android.internal.globalactions.LongPressAction;
import com.android.internal.globalactions.SinglePressAction;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.PowerAction;
import com.android.server.policy.RestartAction;
import com.android.server.policy.WindowManagerPolicy;

final class GlobalActionsProviderImpl implements
        DialogInterface.OnDismissListener,
        DialogInterface.OnShowListener,
        GlobalActionsProvider,
        View.OnClickListener,
        View.OnLongClickListener {
    private static final String TAG = "GlobalActionsProvider";
    private static final int MESSAGE_DISMISS = 0;
    private static final int MESSAGE_SHOW = 2;

    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    private final IDreamManager mDreamManager;
    /**
     * Whether the device is currently paired.
     *
     * <p>This is initialized when the object is constructed at boot but, if it is false, it is
     * re-evaluated each time the menu is shown, as the device might have been paired since the
     * device booted or the last time it was opened.
     */
    private boolean mSetupWizardCompleted = false;
    private boolean mDeviceProvisioned = false;
    private View mSettingsView;
    private Dialog mDialog;
    private LayoutInflater mInflater;
    private GlobalActionsProvider.GlobalActionsListener mListener;

    GlobalActionsProviderImpl(Context context,
            WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mInflater = LayoutInflater.from(mContext);
        mWindowManagerFuncs = windowManagerFuncs;

        maybeUpdateSetupWizardCompleted();

        mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.getService(DreamService.DREAM_SERVICE));

        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(mBroadcastReceiver, filter,
                Context.RECEIVER_EXPORTED);
    }

    public boolean isGlobalActionsDisabled() {
        return false; // always available on wear
    }

    public void setGlobalActionsListener(GlobalActionsProvider.GlobalActionsListener listener) {
        mListener = listener;
        mListener.onGlobalActionsAvailableChanged(true);
    }

    @Override
    public void showGlobalActions() {
        mDeviceProvisioned = Settings.Global.getInt(
                mContentResolver, Settings.Global.DEVICE_PROVISIONED, 0) != 0;
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        // Ensure it runs on correct thread. Also show delayed, so that the dismiss of the previous
        // dialog completes
        mHandler.sendEmptyMessage(MESSAGE_SHOW);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (mListener != null) {
            mListener.onGlobalActionsShown();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mListener.onGlobalActionsDismissed();
    }

    private void awakenIfNecessary() {
        if (mDreamManager != null) {
            try {
                if (mDreamManager.isDreaming()) {
                    mDreamManager.awaken();
                }
            } catch (RemoteException e) {
                // we tried
            }
        }
    }

    /**
     * Updates whether the device is currently paired.
     *
     * <p>This needs to be updated only if the device was not previously paired, since unpairing the
     * device will cause a reboot.
     */
    private void maybeUpdateSetupWizardCompleted() {
        if (!mSetupWizardCompleted) {
            // If the setup wizard was not completed when we last checked, check again now because
            // the user might have paired the device since.
            mSetupWizardCompleted =
                    Settings.System.getInt(
                            mContentResolver,
                            Settings.System.SETUP_WIZARD_HAS_RUN, 0) == 1;
        }
    }

    private void handleShow() {
        awakenIfNecessary();
        maybeUpdateSetupWizardCompleted();
        mDialog = createDialog();
        prepareDialog();

        WindowManager.LayoutParams attrs = mDialog.getWindow().getAttributes();
        attrs.setTitle("WearGlobalActions");
        mDialog.getWindow().setAttributes(attrs);
        mDialog.show();
    }

    /**
     * Create the global actions dialog.
     * @return A new dialog.
     */
    private Dialog createDialog() {
        ViewGroup scrollView = (ViewGroup) mInflater.inflate(R.layout.global_actions, null);
        applyPaddingRelativeToScreenDimensions(scrollView);

        ViewGroup actionsContainer = scrollView.findViewById(R.id.actions_container);
        Resources wearResources = WearResourceUtil.getWearableResources(mContext);

        maybeAddLanguageAction(actionsContainer, wearResources);
        addAction(actionsContainer, new PowerAction(mContext, mWindowManagerFuncs));
        addAction(actionsContainer, new RestartAction(mContext, mWindowManagerFuncs));
        // This action is to initiate a pre-configured SOS call from safety app.
        maybeAddEmergencyAction(actionsContainer, wearResources);
        // This action is to open the dialer app in emergency mode on cellular devices,
        // so that anyone who picks up this device can call emergency numbers like 911 in US.
        maybeAddEmergencyDialAction(actionsContainer, wearResources);

        if(isNonReleaseBuild()) {
            addAction(actionsContainer, new BugReportAction(mContext));
            addAction(actionsContainer, new ScreenRecordAction(mContext, wearResources));
            maybeAddSystemTraceAction(actionsContainer, wearResources);
        }

        mSettingsView = addAction(actionsContainer, new SettingsAction());

        applyScrollAnimations(scrollView, actionsContainer);

        Dialog dialog = new Dialog(mContext);
        dialog.setContentView(scrollView);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        dialog.setOnDismissListener(this);
        dialog.setOnShowListener(this);
        // though this title isn't visible, adding one will allow accessbility
        // services announce the title when the dialog is shown
        dialog.setTitle(wearResources.getString(global_actions_title));
        return dialog;
    }

    private void applyScrollAnimations(ViewGroup scrollView, ViewGroup container) {
        ViewGroupFader fader = createFader(container);
        Animator.AnimatorListener animatorListener = createAnimatorListener(scrollView, fader);
        // update fade/scale animations after a layout change
        scrollView.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> fader.updateFade());
        // add a startup animation
        scrollView.addOnAttachStateChangeListener(
                new ViewEntranceAnimationManager(animatorListener));
    }

    /**
     * Creates an animation listener that sets up fade animations and requests focus to enable RSB
     * scrolling.
     */
    private AnimatorListenerAdapter createAnimatorListener(
            ViewGroup scrollView, ViewGroupFader fader) {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scrollView.requestFocus();
                scrollView.setOnScrollChangeListener(
                        (v, scrollX, scrollY, oldScrollX, oldScrollY)
                                -> fader.updateFade());
            }
        };
    }

    /**
     * Creates a simple ViewGroupFader that animates views from both top and bottom.
     */
    private ViewGroupFader createFader(ViewGroup container) {
        return new ViewGroupFader(
                container,
                new ViewGroupFader.AnimationCallback() {
                    @Override
                    public boolean shouldFadeFromTop(View view) {
                        return true;
                    }

                    @Override
                    public boolean shouldFadeFromBottom(View view) {
                        return true;
                    }

                    @Override
                    public void viewHasBecomeFullSize(View view) {}
                },
                new ViewGroupFader.GlobalVisibleViewBoundsProvider());
    }

    private void applyPaddingRelativeToScreenDimensions(ViewGroup viewGroup) {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int verticalPadding = Math.round(mContext.getResources().getFraction(
                R.fraction.global_actions_vertical_padding_percentage,
                dm.heightPixels,
                dm.heightPixels));
        int horizontalPadding = Math.round(mContext.getResources().getFraction(
                R.fraction.global_actions_horizontal_padding_percentage,
                dm.widthPixels,
                dm.widthPixels));
        viewGroup.setPadding(
                horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
    }

    @Nullable
    private View addAction(ViewGroup parent, Action action) {
        // from a UI standpoint, showing an action that is disabled
        // provides no benefit to the user. So disabled actions are
        // not added to the dialog
        if(!action.isEnabled()) {
           return null;
        }

        View view = action.create(mContext, null, parent, mInflater);
        view.setTag(action);
        view.setOnClickListener(this);
        if (action instanceof LongPressAction) {
            view.setOnLongClickListener(this);
        }
        view.setAccessibilityDelegate(new ActionAccessibilityDelegate());
        parent.addView(view);
        return view;
    }

    /** Adds {@link EmergencySOSAction} to power menu if emergency intent can be resolved. */
    private void maybeAddEmergencyAction(ViewGroup container, Resources wearResources) {
        // The emergency SOS action should not be available if the device is not paired and device
        // lacks telephony.
        if (!mSetupWizardCompleted && !hasTelephony()) return;

        Intent emergencyIntent = new Intent(
                isInRetailMode(mContext) ? EmergencySOSAction.LAUNCH_EMERGENCY_RETAIL_ACTION
                        : EmergencySOSAction.LAUNCH_EMERGENCY_ACTION);
        PackageManager pm = mContext.getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(emergencyIntent,
                PackageManager.MATCH_SYSTEM_ONLY);
        if (resolveInfo != null) {
            addAction(container, new EmergencySOSAction(mContext, wearResources, resolveInfo));
        } else {
            Log.w(TAG, "Failed to add emergency action " + emergencyIntent.getAction());
        }
    }

    private void maybeAddEmergencyDialAction(ViewGroup container, Resources wearResources) {
        // The emergency call action should not be available if the device lacks telephony
        if (!hasTelephony()) {
            // This is not a cellular device.
            return;
        }

        // We don't do a resolution check here. Having an uninstallable dialer handling emergency
        // call is a device requirement.
        Intent emergencyDialIntent = mContext.getSystemService(TelecomManager.class)
                                           .createLaunchEmergencyDialerIntent(/* number= */ null);
        addAction(container, new EmergencyDialAction(mContext, wearResources, emergencyDialIntent));
    }

    private void maybeAddLanguageAction(ViewGroup container, Resources wearResources) {
        // There's currently no support of switching language after device has been provisioned
        if (mDeviceProvisioned) {
            return;
        }

        addAction(container, new LanguageAction(mContext, wearResources));
    }

    private static boolean isInRetailMode(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0) == 1;
    }

    private boolean hasTelephony() {
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        return telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    /** Adds a {@link SystemTraceAction} to the power menu if all conditions are met. */
    private void maybeAddSystemTraceAction(ViewGroup container, Resources wearResources) {
        boolean developerOptionsEnabled = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        boolean systemTracePowerMenuConfigEnabled =
                wearResources.getBoolean(config_systemTracePowerMenu);
        Log.d(TAG,
                String.format(
                        "developerOptionsEnabled=%b, systemTracePowerMenuConfigEnabled=%b",
                        developerOptionsEnabled, systemTracePowerMenuConfigEnabled));

        if (systemTracePowerMenuConfigEnabled && developerOptionsEnabled) {
            addAction(container, new SystemTraceAction(mContext, wearResources));
        }
    }

    @Override
    public void onClick(View v) {
        mHandler.sendEmptyMessage(MESSAGE_DISMISS);
        Object tag = v.getTag();
        if (tag instanceof Action) {
            ((Action) tag).onPress();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof LongPressAction) {
            return ((LongPressAction) tag).onLongPress();
        }
        return false;
    }

    private void prepareDialog() {
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        mSettingsView.setVisibility(mDeviceProvisioned ? View.GONE : View.VISIBLE);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)
                    || Intent.ACTION_SCREEN_OFF.equals(action)) {
                String reason = intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY);
                if (!PhoneWindowManager.SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS.equals(reason)) {
                    mHandler.sendEmptyMessage(MESSAGE_DISMISS);
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_DISMISS:
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
                break;
            case MESSAGE_SHOW:
                handleShow();
                break;
            }
        }
    };

    private class SettingsAction extends SinglePressAction {
        public SettingsAction() {
            super(R.drawable.ic_settings, R.string.global_action_settings);
        }

        @Override
        public void onPress() {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
        }

        @Override
        public boolean showDuringKeyguard() {
            return false;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }
    };

    private abstract static class EmergencyAction extends SinglePressAction {

        EmergencyAction(int iconResId, Drawable icon, CharSequence message) {
            super(iconResId, icon, message);
        }

        @Override
        public View create(Context context, View convertView, ViewGroup parent,
                LayoutInflater inflater) {
            View view = super.create(context, convertView, parent, inflater);
            view.setBackgroundResource(R.drawable.global_actions_item_red_background);
            TextView message = view.findViewById(R.id.message);
            message.setTextColor(context.getColor(R.color.black));
            return view;
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }
    }

    // This is an action to start safety app's pre-defined SOS screen.
    private final static class EmergencySOSAction extends EmergencyAction {
        private static final String LAUNCH_EMERGENCY_ACTION =
                "com.android.systemui.action.LAUNCH_EMERGENCY";
        // Intent to launch emergency in retail mode
        private static final String LAUNCH_EMERGENCY_RETAIL_ACTION =
                "com.android.systemui.action.LAUNCH_EMERGENCY_RETAIL";

        private final Context context;
        private final ResolveInfo mResolveInfo;

        EmergencySOSAction(Context context, Resources wearResources, ResolveInfo resolveInfo) {
            super(0,
                    wearResources.getDrawable(ic_emergency_star, context.getTheme()),
                    context.getString(R.string.global_action_emergency));
            this.context = context;
            mResolveInfo = resolveInfo;
        }

        @Override
        public void onPress() {
            Vibrator vibrator = context.getSystemService(Vibrator.class);
            if (vibrator != null && vibrator.hasVibrator()) {
                int duration = context.getResources().getInteger(
                        R.integer.config_mashPressVibrateTimeOnPowerButton);
                vibrator.vibrate(VibrationEffect.createOneShot(
                        duration, VibrationEffect.DEFAULT_AMPLITUDE));
            }

            Intent emergencyIntent = new Intent(
                    isInRetailMode(context) ? LAUNCH_EMERGENCY_RETAIL_ACTION
                            : LAUNCH_EMERGENCY_ACTION);
            emergencyIntent.setComponent(new ComponentName(mResolveInfo.activityInfo.packageName,
                    mResolveInfo.activityInfo.name));
            emergencyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            int userId = ActivityManager.getCurrentUser();
            context.startActivityAsUser(emergencyIntent, new UserHandle(userId));
        }
    }

    // This is an action to bring up dialer in emergency mode.
    private static final class EmergencyDialAction extends EmergencyAction {
        private final Context mContext;
        private final Intent mEmergencyDialIntent;

        EmergencyDialAction(Context context, Resources wearResources, Intent emergencyDialIntent) {
            super(/* iconResId= */ 0,
                    wearResources.getDrawable(ic_emergency_dial, context.getTheme()),
                    wearResources.getString(global_action_emergency_dial));
            this.mContext = context;
            this.mEmergencyDialIntent = emergencyDialIntent;
        }

        @Override
        public void onPress() {
            int userId = ActivityManager.getCurrentUser();
            mContext.startActivityAsUser(mEmergencyDialIntent, new UserHandle(userId));
        }
    }

    /**
     * An action to capture a bug report on the watch
     */
    @VisibleForTesting
    final static class BugReportAction extends SinglePressAction {
        private static final String WCS_PACKAGE_NAME = "com.google.android.wearable.app";
        private static final String BUG_REPORT_CLASS_NAME = "com.google.android.clockwork.wcs.bugreport.TakeReportActivity";

        private final Context context;

        public BugReportAction(Context context) {
            super(R.drawable.ic_lock_bugreport, R.string.global_action_bug_report);
            this.context = context;
        }

        @Override
        public void onPress() {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(WCS_PACKAGE_NAME, BUG_REPORT_CLASS_NAME));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        }

        @Override
        public boolean showDuringKeyguard() {
            return true; // we should be able to capture a bugreport even when watch is locked
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            // during "direct boot mode", bug reporting is not functional
            UserManager manager = context.getSystemService(UserManager.class);
            return manager.isUserUnlocked();
        }
    }

    private boolean isNonReleaseBuild() {
        return !"user".equals(Build.TYPE);
    }

    /**
     * An action to capture a record the watch screen
     */
    @VisibleForTesting
    static final class ScreenRecordAction extends SinglePressAction {

        private static final String CLOCKWORK_SYSUI_PACKAGE_NAME =
                "com.google.android.apps.wearable.systemui";
        private static final String SCREEN_RECORD_SERVICE_NAME =
                "com.google.android.clockwork.systemui.screenrecord.WearRecordingService";
        private static final String ACTION_START =
                "com.google.android.clockwork.systemui.screenrecord.START";

        private final Context context;

        public ScreenRecordAction(Context context, Resources wearResources) {
            super(0, wearResources.getDrawable(ic_screen_record, context.getTheme()),
                    wearResources.getString(global_action_record_screen));
            this.context = context;
        }

        @Override
        public void onPress() {
            Intent screenRecordIntent = new Intent();
            screenRecordIntent.setComponent(
                    new ComponentName(CLOCKWORK_SYSUI_PACKAGE_NAME, SCREEN_RECORD_SERVICE_NAME));
            screenRecordIntent.setAction(ACTION_START);
            context.startForegroundService(screenRecordIntent);
        }

        @Override
        public boolean showDuringKeyguard() {
            return true; // capture a screen recording even when watch is locked
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }
    }

    /** An action that lets user open the Traceur app to collect system traces. */
    static final class SystemTraceAction extends SinglePressAction {
        private static final String TRACEUR_PACKAGE_NAME = "com.android.traceur";
        private static final String TRACEUR_MAIN_ACTIVITY_NAME = "com.android.traceur.MainActivity";

        private final Context context;

        public SystemTraceAction(Context context, Resources wearResources) {
            super(0, wearResources.getDrawable(ic_system_trace, context.getTheme()),
                    wearResources.getString(global_action_system_trace));
            this.context = context;
        }

        @Override
        public void onPress() {
            Log.d(TAG, "SystemTraceAction.onPress");

            Intent systemTraceIntent = new Intent();
            systemTraceIntent.setComponent(
                    new ComponentName(TRACEUR_PACKAGE_NAME, TRACEUR_MAIN_ACTIVITY_NAME));
            systemTraceIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            try {
                context.startActivity(systemTraceIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error trying to open Traceur.", e);
            }
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }
    }

    /**
     *  An action that let's user change language during setup wizard
     */
    static final class LanguageAction extends SinglePressAction {

        // TODO: Make this configurable via resource overlay and add default setup wizard
        // package and class names in device/google/clockwork/overlay
        private static final String LOCALE_PACKAGE_NAME =
                "com.google.android.wearable.setupwizard";
        private static final String LOCALE_ACTIVITY_NAME =
                "com.google.android.wearable.setupwizard.steps.locale.LocaleActivity";
        private static final String INTENT_EXTRA_LANGUAGE_STANDALONE_MODE=
                "language_standalone_mode";

        private final Context context;

        public LanguageAction(Context context, Resources wearResources) {
            super(0, wearResources.getDrawable(ic_gm_language, context.getTheme()),
                    "Language");
            this.context = context;
        }

        @Override
        public void onPress() {
            Intent localeIntent = new Intent();
            localeIntent.setComponent(
                    new ComponentName(LOCALE_PACKAGE_NAME, LOCALE_ACTIVITY_NAME));
            localeIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            localeIntent.putExtra(INTENT_EXTRA_LANGUAGE_STANDALONE_MODE, true);
            context.startActivity(localeIntent);
        }

        @Override
        public boolean showDuringKeyguard() {
            return false;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }
    }

    /**
     * An accessibility delegate that sets up the accessibility announcements to match WearMaterial
     * Chip button
     */
    private static final class ActionAccessibilityDelegate extends View.AccessibilityDelegate {
        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(Button.class.getName());
        }
    }
}
