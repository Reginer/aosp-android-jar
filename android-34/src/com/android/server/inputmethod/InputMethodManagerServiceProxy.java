/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_CRITICAL;
import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_NORMAL;
import static android.os.IServiceManager.DUMP_FLAG_PROTO;

import android.annotation.BinderThread;
import android.annotation.MainThread;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.annotation.WorkerThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ImeTracker;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import android.window.ImeOnBackInvokedDispatcher;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.IImeTracker;
import com.android.internal.inputmethod.IInlineSuggestionsRequestCallback;
import com.android.internal.inputmethod.IInputMethodClient;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
import com.android.internal.inputmethod.IRemoteInputConnection;
import com.android.internal.inputmethod.InlineSuggestionsRequestInfo;
import com.android.internal.inputmethod.InputBindResult;
import com.android.internal.inputmethod.SoftInputShowHideReason;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.view.IInputMethodManager;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.utils.Slogf;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Proxy used to host IMMSs per user and reroute requests to the user associated IMMS.
 *
 * @hide
 */
public final class InputMethodManagerServiceProxy extends IInputMethodManager.Stub {

    private static final String IMMS_TAG = InputMethodManagerServiceProxy.class.getSimpleName();
    private static final boolean DBG = Log.isLoggable(IMMS_TAG, Log.DEBUG);

    // System property used to disable IMMS proxy.
    // When set to true, Android Core's original IMMS will be launched instead.
    // Note: this flag only takes effects on non user builds.
    public static final String DISABLE_MU_IMMS = "persist.fw.car.test.disable_mu_imms";

    private static final ExecutorService sExecutor = Executors.newCachedThreadPool();

    private final ReentrantReadWriteLock mRwLock = new ReentrantReadWriteLock();

    @GuardedBy("mRwLock")
    private final SparseArray<CarInputMethodManagerService> mServicesForUser = new SparseArray<>();

    @GuardedBy("mRwLock")
    private final SparseArray<InputMethodManagerInternal> mLocalServicesForUser =
            new SparseArray<>();

    private final Context mContext;
    private InputMethodManagerInternalProxy mInternalProxy;

    public InputMethodManagerServiceProxy(Context context) {
        mContext = context;
        mInternalProxy = new InputMethodManagerInternalProxy();
    }

    @UserIdInt
    private int getCallingUserId() {
        final int uid = Binder.getCallingUid();
        return UserHandle.getUserId(uid);
    }

    InputMethodManagerInternal getLocalServiceProxy() {
        return mInternalProxy;
    }

    CarInputMethodManagerService createAndRegisterServiceFor(@UserIdInt int userId) {
        Slogf.d(IMMS_TAG, "Starting IMMS and IMMI for user {%d}", userId);
        CarInputMethodManagerService imms;
        try {
            mRwLock.writeLock().lock();
            if ((imms = mServicesForUser.get(userId)) != null) {
                return imms;
            }
            imms = new CarInputMethodManagerService(mContext, sExecutor);
            mServicesForUser.set(userId, imms);
            InputMethodManagerInternal localService = imms.getInputMethodManagerInternal();
            mLocalServicesForUser.set(userId, localService);
        } finally {
            mRwLock.writeLock().unlock();
        }
        imms.systemRunning();
        Slogf.d(IMMS_TAG, "Started IMMS and IMMI for user {%d}", userId);
        return imms;
    }

    CarInputMethodManagerService getServiceForUser(@UserIdInt int userId) {
        try {
            mRwLock.readLock().lock();
            return mServicesForUser.get(userId);
        } finally {
            mRwLock.readLock().unlock();
        }
    }

    InputMethodManagerInternal getLocalServiceForUser(@UserIdInt int userId) {
        try {
            mRwLock.readLock().lock();
            return mLocalServicesForUser.get(userId);
        } finally {
            mRwLock.readLock().unlock();
        }
    }

    /**
     * SystemService for CarInputMethodManagerServices.
     *
     * If {@code fw.enable_imms_proxy} system property is set to {@code false}, then it just
     * delegate to Android Core original {@link InputMethodManagerService.Lifecycle}.
     *
     * TODO(b/245798405): make Lifecycle class easier to test and add tests for it
     */
    public static class Lifecycle extends SystemService {
        private static final String LIFECYCLE_TAG =
                IMMS_TAG + "." + Lifecycle.class.getSimpleName();

        private final InputMethodManagerServiceProxy mServiceProxy;
        private final Context mContext;
        private final UserManagerInternal mUserManagerInternal;
        private HandlerThread mWorkerThread;
        private Handler mHandler;

        // Android core IMMS to be used when IMMS Proxy is disabled
        private final InputMethodManagerService.Lifecycle mCoreImmsLifecycle;

        /**
         * Initializes the system service for InputMethodManagerServiceProxy.
         */
        public Lifecycle(@NonNull Context context) {
            super(context);
            mContext = context;
            mUserManagerInternal = LocalServices.getService(UserManagerInternal.class);
            mServiceProxy = new InputMethodManagerServiceProxy(mContext);
            if (!Build.IS_USER && SystemProperties.getBoolean(
                    DISABLE_MU_IMMS, /* defaultValue= */ false)) {
                mCoreImmsLifecycle = new InputMethodManagerService.Lifecycle(mContext);
            } else {
                mCoreImmsLifecycle = null;
            }
        }

        private boolean isImmsProxyEnabled() {
            return mCoreImmsLifecycle == null;
        }

        @MainThread
        @Override
        public void onStart() {
            if (DBG) {
                Slogf.d(LIFECYCLE_TAG, "Entering #onStart (IMMS Proxy enabled={%s})",
                        isImmsProxyEnabled());
            }
            if (!isImmsProxyEnabled()) {
                mCoreImmsLifecycle.onStart();
                return;
            }
            mWorkerThread = new HandlerThread(IMMS_TAG);
            mWorkerThread.start();
            mHandler = new Handler(mWorkerThread.getLooper(), msg -> false, true);

            // Register broadcast receivers for user state changes
            IntentFilter broadcastFilterForSystemUser = new IntentFilter();
            broadcastFilterForSystemUser.addAction(Intent.ACTION_LOCALE_CHANGED);
            mContext.registerReceiver(new ImmsBroadcastReceiverForSystemUser(),
                    broadcastFilterForSystemUser);

            // Register binders
            LocalServices.addService(InputMethodManagerInternal.class,
                    mServiceProxy.getLocalServiceProxy());
            publishBinderService(Context.INPUT_METHOD_SERVICE, mServiceProxy,
                    false /*allowIsolated*/,
                    DUMP_FLAG_PRIORITY_CRITICAL | DUMP_FLAG_PRIORITY_NORMAL | DUMP_FLAG_PROTO);
        }

        @MainThread
        @Override
        public void onBootPhase(int phase) {
            if (DBG) {
                Slogf.d(LIFECYCLE_TAG,
                        "Entering #onBootPhase with phase={%d} (IMMS Proxy enabled={%s})", phase,
                        isImmsProxyEnabled());
            }
            if (!isImmsProxyEnabled()) {
                mCoreImmsLifecycle.onBootPhase(phase);
                return;
            }
            if (phase == SystemService.PHASE_ACTIVITY_MANAGER_READY) {
                mHandler.sendMessage(PooledLambda.obtainMessage(
                        Lifecycle::onBootPhaseReceived, this, phase));
            }
        }

        @WorkerThread
        private void onBootPhaseReceived(int phase) {
            if (DBG) {
                Slogf.d(LIFECYCLE_TAG, "Entering #onBootPhaseReceived with phase={%d}", phase);
            }
            int[] userIds = mUserManagerInternal.getUserIds();
            for (int i = 0; i < userIds.length; ++i) {
                mServiceProxy.createAndRegisterServiceFor(userIds[i]);
            }
        }

        @MainThread
        @Override
        public void onUserStarting(@NonNull TargetUser user) {
            if (DBG) {
                Slogf.d(LIFECYCLE_TAG,
                        "Entering #onUserStarting with user={%s} (IMMS Proxy enabled={%s})", user,
                        isImmsProxyEnabled());
            }
            if (!isImmsProxyEnabled()) {
                mCoreImmsLifecycle.onUserStarting(user);
                return;
            }
            mHandler.sendMessage(PooledLambda.obtainMessage(
                    Lifecycle::onUserStartingReceived, this, user));
        }

        @WorkerThread
        private void onUserStartingReceived(@NonNull TargetUser user) {
            // This method may be invoked under WindowManagerGlobalLock, therefore the code must be
            // run on separated thread to avoid deadlock (imms#systemRUnning and
            // imms#scheduleSwitchUserTaskLocked will try to acquire WindowManagerGlobalLock).
            sExecutor.execute(() -> {
                CarInputMethodManagerService imms = mServiceProxy.createAndRegisterServiceFor(
                        user.getUserIdentifier());
                synchronized (ImfLock.class) {
                    imms.scheduleSwitchUserTaskLocked(user.getUserIdentifier(),
                            /* clientToBeReset= */ null);
                }
            });
        }

        @MainThread
        @Override
        public void onUserUnlocking(@NonNull TargetUser user) {
            if (DBG) {
                Slogf.d(LIFECYCLE_TAG,
                        "Entering #onUserUnlockingReceived with to={%s} (IMMS Proxy enabled={%s})",
                        user, isImmsProxyEnabled());
            }
            if (!isImmsProxyEnabled()) {
                mCoreImmsLifecycle.onUserUnlocking(user);
                return;
            }
            mHandler.sendMessage(PooledLambda.obtainMessage(
                    Lifecycle::onUserUnlockingReceived, this, user));
        }

        @WorkerThread
        private void onUserUnlockingReceived(@NonNull TargetUser user) {
            CarInputMethodManagerService service = mServiceProxy.getServiceForUser(
                    user.getUserIdentifier());
            if (service != null) {
                // Called on ActivityManager thread.
                service.notifySystemUnlockUser(user.getUserIdentifier());
            }
        }

        @MainThread
        @Override
        public void onUserStopping(@NonNull TargetUser user) {
            if (DBG) {
                Slogf.d(LIFECYCLE_TAG,
                        "Entering #onUserStoppingReceived with userId={%d} (IMMS Proxy "
                                + "enabled={%s})",
                        user.getUserIdentifier(), isImmsProxyEnabled());
            }
            if (!isImmsProxyEnabled()) {
                mCoreImmsLifecycle.onUserStopping(user);
                return;
            }
            sExecutor.execute(
                    () -> mServiceProxy.removeCarInputMethodManagerServiceForUser(
                            user.getUserIdentifier()));
        }

        @MainThread
        @Override
        public void onUserSwitching(@Nullable TargetUser from, @NonNull TargetUser to) {
            if (DBG) {
                Slogf.d(LIFECYCLE_TAG,
                        "Entering #onUserSwitching with from={%d} and to={%d} (IMMS Proxy "
                                + "enabled={%s})",
                        from.getUserIdentifier(), to.getUserIdentifier(), isImmsProxyEnabled());
            }
            if (!isImmsProxyEnabled()) {
                mCoreImmsLifecycle.onUserSwitching(from, to);
            }
        }

        /**
         * {@link BroadcastReceiver} that is intended to listen to broadcasts sent to the system
         * user only.
         */
        private final class ImmsBroadcastReceiverForSystemUser extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                    mServiceProxy.onActionLocaleChanged();
                } else {
                    Slogf.w(LIFECYCLE_TAG, "Unexpected intent " + intent);
                }
            }
        }
    }

    private void removeCarInputMethodManagerServiceForUser(int userId) {
        mRwLock.writeLock().lock();
        try {
            CarInputMethodManagerService imms = mServicesForUser.get(userId);
            if (imms == null) {
                return;
            }
            mServicesForUser.remove(userId);
            mLocalServicesForUser.remove(userId);
            imms.systemShutdown();
        } finally {
            mRwLock.writeLock().unlock();
        }
        Slogf.i(IMMS_TAG, "Removed CarIMMS for user {%d}", userId);
    }

    /**
     * Dump this IMMS Proxy object. If `--user` arg is pass (along with an existing user id) then
     * it will just dump the user associated IMMS.
     */
    @BinderThread
    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(mContext, IMMS_TAG, pw)) {
            Slogf.w(IMMS_TAG, "Permission denied for #dump");
            return;
        }

        // Check if --user is set. If set, then just dump the user's IMMS.
        int userIdArg = parseUserArgIfPresent(args);
        if (userIdArg != UserHandle.USER_NULL) {
            mServicesForUser.get(userIdArg).dump(fd, pw, args);
            return;
        }
        pw.println("*InputMethodManagerServiceProxy");
        pw.println("**mServicesForUser**");
        try {
            mRwLock.readLock().lock();
            if (parseBriefArg(args)) {
                // Dump brief
                for (int i = 0; i < mServicesForUser.size(); i++) {
                    int userId = mServicesForUser.keyAt(i);
                    CarInputMethodManagerService imms = mServicesForUser.valueAt(i);
                    pw.println(" userId=" + userId + " imms=" + imms.hashCode() + " {autofill="
                            + imms.getAutofillController() + "}");
                }
                pw.println("**mLocalServicesForUser**");
                for (int i = 0; i < mLocalServicesForUser.size(); i++) {
                    int userId = mLocalServicesForUser.keyAt(i);
                    InputMethodManagerInternal immi = mLocalServicesForUser.valueAt(i);
                    pw.println(" userId=" + userId + " immi=" + immi.hashCode());
                }
            } else {
                // Dump full
                for (int i = 0; i < mServicesForUser.size(); i++) {
                    int userId = mServicesForUser.keyAt(i);
                    pw.println("**CarInputMethodManagerService for userId=" + userId);
                    CarInputMethodManagerService imms = mServicesForUser.valueAt(i);
                    imms.dump(fd, pw, args);
                }
            }
        } finally {
            mRwLock.readLock().unlock();
        }
        pw.flush();
    }

    private boolean parseBriefArg(String[] args) {
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--brief")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse the args string and returns the value of `--user` argument. Returns
     * {@link UserHandle.USER_NULL} in case of `--user` is not in args.
     *
     * @return the value of `--user` argument or UserHandle.USER_NULL if `--user` is not in args
     * @throws IllegalArgumentException if `--user` arg is not passed along a user id
     * @throws NumberFormatException    if the value passed along `--user` is not an integer
     */
    private int parseUserArgIfPresent(String[] args) {
        if (args == null) {
            return UserHandle.USER_NULL;
        }
        for (int i = 0; i < args.length; ++i) {
            if ("--user".equals(args[i])) {
                if (i == args.length - 1) {
                    throw new IllegalArgumentException("User id must be passed within --user arg");
                }
                try {
                    return Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Expected an integer value for `--user` arg, got " + args[i]);
                }
            }
        }
        return UserHandle.USER_NULL;
    }

    private void onActionLocaleChanged() {
        try {
            mRwLock.readLock().lock();
            for (int i = 0; i < mServicesForUser.size(); i++) {
                int userId = mServicesForUser.keyAt(i);
                Slogf.i(IMMS_TAG, "Updating location for user {%d} Car IMMS", userId);
                CarInputMethodManagerService imms = mServicesForUser.valueAt(i);
                imms.onActionLocaleChanged();
            }
        } finally {
            mRwLock.readLock().unlock();
        }
    }

    // Delegate methods  ///////////////////////////////////////////////////////////////////////////

    @Override
    public void addClient(IInputMethodClient client, IRemoteInputConnection inputmethod,
            int untrustedDisplayId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking addClient with untrustedDisplayId={%d}",
                    callingUserId, untrustedDisplayId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.addClient(client, inputmethod, untrustedDisplayId);
    }

    @Override
    public List<InputMethodInfo> getInputMethodList(int userId, int directBootAwareness) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking getInputMethodList with userId={%d}",
                    callingUserId, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getInputMethodList(userId, directBootAwareness);
    }

    @Override
    public List<InputMethodInfo> getEnabledInputMethodList(int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking getInputMethodList with userId={%d}",
                    callingUserId, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getEnabledInputMethodList(userId);
    }

    @Override
    public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String imiId,
            boolean allowsImplicitlyEnabledSubtypes, int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG,
                    "User {%d} invoking getEnabledInputMethodSubtypeList with imiId={%s}",
                    callingUserId, imiId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getEnabledInputMethodSubtypeList(imiId, allowsImplicitlyEnabledSubtypes,
                userId);
    }

    @Override
    public InputMethodSubtype getLastInputMethodSubtype(int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking getLastInputMethodSubtype with userId={%d}",
                    callingUserId, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getLastInputMethodSubtype(userId);
    }

    @Override
    public boolean showSoftInput(IInputMethodClient client, IBinder windowToken,
            ImeTracker.Token statsToken, int flags, int lastClickToolType,
            ResultReceiver resultReceiver, int reason) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking showSoftInput with "
                    + "windowToken={%s} and reason={%d}", callingUserId, windowToken, reason);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.showSoftInput(client, windowToken, statsToken, flags, lastClickToolType,
                resultReceiver,
                reason);
    }

    @Override
    public boolean hideSoftInput(IInputMethodClient client, IBinder windowToken,
            @Nullable ImeTracker.Token statsToken, int flags, ResultReceiver resultReceiver,
            @SoftInputShowHideReason int reason) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking hideSoftInput with "
                    + "windowToken={%s} and reason={%d}", callingUserId, windowToken, reason);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.hideSoftInput(client, windowToken, statsToken, flags, resultReceiver,
                reason);
    }

    @Override
    public InputBindResult startInputOrWindowGainedFocus(int startInputReason,
            IInputMethodClient client, IBinder windowToken, int startInputFlags,
            int softInputMode,
            int windowFlags, EditorInfo editorInfo, IRemoteInputConnection inputConnection,
            IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection,
            int unverifiedTargetSdkVersion, int userId,
            ImeOnBackInvokedDispatcher imeDispatcher) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking startInputOrWindowGainedFocus with "
                    + "windowToken={%s} and reason={%d}", callingUserId, windowToken, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        InputBindResult result = imms.startInputOrWindowGainedFocus(startInputReason,
                client, windowToken, startInputFlags, softInputMode,
                windowFlags, editorInfo, inputConnection,
                remoteAccessibilityInputConnection, unverifiedTargetSdkVersion, userId,
                imeDispatcher);
        if (DBG) {
            Slogf.d(IMMS_TAG, "Returning {%s} for startInputOrWindowGainedFocus / user {%d}",
                    result,
                    userId);
        }
        return result;
    }

    @Override
    public void showInputMethodPickerFromClient(IInputMethodClient client,
            int auxiliarySubtypeMode) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking showInputMethodPickerFromClient",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.showInputMethodPickerFromClient(client, auxiliarySubtypeMode);
    }

    @Override
    public void showInputMethodPickerFromSystem(int auxiliarySubtypeMode, int displayId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking getLastInputMethodSubtype with "
                            + " auxiliarySubtypeMode={%d}, and displayId={%d}", callingUserId,
                    auxiliarySubtypeMode, displayId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.showInputMethodPickerFromSystem(auxiliarySubtypeMode, displayId);
    }

    @Override
    public boolean isInputMethodPickerShownForTest() {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking isInputMethodPickerShownForTest",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.isInputMethodPickerShownForTest();
    }

    @Override
    public InputMethodSubtype getCurrentInputMethodSubtype(int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG,
                    "User {%d} invoking getCurrentInputMethodSubtype with userId={%d}",
                    callingUserId, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getCurrentInputMethodSubtype(userId);
    }

    @Override
    public void setAdditionalInputMethodSubtypes(String id, InputMethodSubtype[] subtypes,
            int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking setAdditionalInputMethodSubtypes with "
                    + "id={%d} and userId={%d}", callingUserId, id, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.setAdditionalInputMethodSubtypes(id, subtypes, userId);
    }

    @Override
    public void setExplicitlyEnabledInputMethodSubtypes(String imeId, int[] subtypeHashCodes,
            int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking setExplicitlyEnabledInputMethodSubtypes with "
                    + "imeId={%d} and userId={%d}", callingUserId, imeId, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.setExplicitlyEnabledInputMethodSubtypes(imeId, subtypeHashCodes, userId);
    }

    @Override
    public int getInputMethodWindowVisibleHeight(IInputMethodClient client) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking getInputMethodWindowVisibleHeight",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getInputMethodWindowVisibleHeight(client);
    }

    @Override
    public void reportVirtualDisplayGeometryAsync(IInputMethodClient parentClient,
            int childDisplayId, float[] matrixValues) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking reportVirtualDisplayGeometryAsync",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.reportVirtualDisplayGeometryAsync(parentClient, childDisplayId, matrixValues);
    }

    @Override
    public void reportPerceptibleAsync(IBinder windowToken, boolean perceptible) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking reportPerceptibleAsync",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.reportPerceptibleAsync(windowToken, perceptible);
    }

    @Override
    public void removeImeSurface() {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking removeImeSurface",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.removeImeSurface();
    }

    @Override
    public void removeImeSurfaceFromWindowAsync(IBinder windowToken) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking removeImeSurfaceFromWindowAsync "
                    + "with windowToken={%s}", callingUserId, windowToken);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.removeImeSurfaceFromWindowAsync(windowToken);
    }

    @Override
    public void startProtoDump(byte[] protoDump, int source, String where) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking startProtoDump", callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.startProtoDump(protoDump, source, where);
    }

    @Override
    public boolean isImeTraceEnabled() {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking isImeTraceEnabled", callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.isImeTraceEnabled();
    }

    @Override
    public void startImeTrace() {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking startImeTrace", callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.startImeTrace();
    }

    @Override
    public void stopImeTrace() {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking stopImeTrace", callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.stopImeTrace();
    }

    @Override
    public void startStylusHandwriting(IInputMethodClient client) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking startStylusHandwriting", callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.startStylusHandwriting(client);
    }

    @Override
    public void prepareStylusHandwritingDelegation(
            @NonNull IInputMethodClient client,
            @UserIdInt int userId,
            @NonNull String delegatePackageName,
            @NonNull String delegatorPackageName) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking prepareStylusHandwritingDelegation with"
                            + "client={%s}, userId={%d}, delegatePackageName={%s}, "
                            + "delegatorPackageName={%s}",
                    callingUserId, client, delegatePackageName, delegatorPackageName);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.prepareStylusHandwritingDelegation(client, userId, delegatePackageName,
                delegatorPackageName);
    }

    @Override
    public boolean acceptStylusHandwritingDelegation(
            @NonNull IInputMethodClient client,
            @UserIdInt int userId,
            @NonNull String delegatePackageName,
            @NonNull String delegatorPackageName) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking acceptStylusHandwritingDelegation with"
                            + "client={%s}, userId={%d}, delegatePackageName={%s}, "
                            + "delegatorPackageName={%s}",
                    callingUserId, client, delegatePackageName, delegatorPackageName);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.acceptStylusHandwritingDelegation(client, userId, delegatePackageName,
                delegatorPackageName);
    }

    @Override
    public boolean isStylusHandwritingAvailableAsUser(int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking isStylusHandwritingAvailableAsUser",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.isStylusHandwritingAvailableAsUser(userId);
    }

    @Override
    public void addVirtualStylusIdForTestSession(IInputMethodClient client) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking addVirtualStylusIdForTestSession",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.addVirtualStylusIdForTestSession(client);
    }

    @Override
    public void setStylusWindowIdleTimeoutForTest(IInputMethodClient client, long timeout) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking setStylusWindowIdleTimeoutForTest",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        imms.setStylusWindowIdleTimeoutForTest(client, timeout);
    }

    @Override
    public IImeTracker getImeTrackerService() {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking getImeTrackerService",
                    callingUserId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getImeTrackerService();
    }

    @Override
    public InputMethodInfo getCurrentInputMethodInfoAsUser(@UserIdInt int userId) {
        final int callingUserId = getCallingUserId();
        if (DBG) {
            Slogf.d(IMMS_TAG, "User {%d} invoking getCurrentInputMethodInfoAsUser with userId={%d}",
                    callingUserId, userId);
        }
        CarInputMethodManagerService imms = getServiceForUser(callingUserId);
        return imms.getCurrentInputMethodInfoAsUser(userId);
    }

    @BinderThread
    @Override
    public void onShellCommand(@Nullable FileDescriptor in, @Nullable FileDescriptor out,
            @Nullable FileDescriptor err,
            @NonNull String[] args, @Nullable ShellCallback callback,
            @NonNull ResultReceiver resultReceiver) throws RemoteException {
        checkCallerIsRootOrShell(args, resultReceiver);
        int userId;
        try {
            userId = parseUserArgIfPresent(args);
        } catch (IllegalArgumentException | SecurityException e) {
            resultReceiver.send(-1 /* FAILURE */, null);
            Slogf.e(IMMS_TAG, "Failed parsing incoming shell command", e);
            return;
        }
        if (userId == UserHandle.USER_NULL) {
            Slogf.w(IMMS_TAG, "Ignoring incoming shell command {%s}, "
                            + "no user was specified (use --user flag to specify the user id)",
                    Arrays.toString(args));
            resultReceiver.send(-1 /* FAILURE */, null);
            return;
        }
        CarInputMethodManagerService imms = getServiceForUser(userId);
        if (imms == null) {
            Slogf.e(IMMS_TAG, String.format("Ignoring incoming shell command {%s},"
                    + " there is no Car IMMS for user {%d}", Arrays.toString(args), userId));
            resultReceiver.send(-1 /* FAILURE */, null);
            return;
        }
        if (DBG) {
            Slogf.d(IMMS_TAG, "Running shell command {%s} on imms {%d}", Arrays.toString(args),
                    userId);
        }
        imms.onShellCommand(in, out, err, args, callback, resultReceiver);
        resultReceiver.send(0 /* SUCCESS */, null);
    }

    private void checkCallerIsRootOrShell(String[] args, @NonNull ResultReceiver resultReceiver)
            throws SecurityException {
        final int callingUid = Binder.getCallingUid();
        // Regular adb shell will come with process SHELL_UID and adb root shell with ROOT_UID
        if (callingUid != Process.ROOT_UID && callingUid != Process.SHELL_UID) {
            resultReceiver.send(-1 /* FAILURE */, null);
            String errorMsg = String.format("InputMethodManagerServiceProxy does not support"
                            + " shell commands from non-shell users. callingUid={%d} args={%s}",
                    callingUid, Arrays.toString(args));
            if (Process.isCoreUid(callingUid)) {
                // Let's not crash the calling process if the caller is one of core components
                // (this is the same logic adopted by Android Core's IMMS).
                Slogf.e(IMMS_TAG, errorMsg);
                return;
            }
            throw new SecurityException(errorMsg);
        }
    }

    class InputMethodManagerInternalProxy extends InputMethodManagerInternal {
        private final String mImmiTag =
                IMMS_TAG + "." + InputMethodManagerInternalProxy.class.getSimpleName();

        @Override
        public void setInteractive(boolean interactive) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking setInteractive", callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.setInteractive(interactive);
        }

        @Override
        public void hideCurrentInputMethod(int reason) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking hideCurrentInputMethod", callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.hideCurrentInputMethod(reason);
        }

        @Override
        public List<InputMethodInfo> getInputMethodListAsUser(int userId) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking getInputMethodListAsUser=%d",
                        callingUserId,
                        userId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            return immi.getInputMethodListAsUser(userId);
        }

        @Override
        public List<InputMethodInfo> getEnabledInputMethodListAsUser(int userId) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking getEnabledInputMethodListAsUser=%d",
                        callingUserId, userId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            return immi.getEnabledInputMethodListAsUser(userId);
        }

        @Override
        public void onCreateInlineSuggestionsRequest(int userId,
                InlineSuggestionsRequestInfo requestInfo,
                IInlineSuggestionsRequestCallback cb) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking onCreateInlineSuggestionsRequest=%d",
                        callingUserId, userId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.onCreateInlineSuggestionsRequest(userId, requestInfo, cb);
        }

        @Override
        public boolean switchToInputMethod(String imeId, int userId) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking switchToInputMethod=%d", callingUserId,
                        userId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            return immi.switchToInputMethod(imeId, userId);
        }

        @Override
        public boolean setInputMethodEnabled(String imeId, boolean enabled, int userId) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking setInputMethodEnabled", callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            return immi.setInputMethodEnabled(imeId, enabled, userId);
        }

        @Override
        public void registerInputMethodListListener(InputMethodListListener listener) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking registerInputMethodListListener",
                        callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.registerInputMethodListListener(listener);
        }

        @Override
        public boolean transferTouchFocusToImeWindow(
                @NonNull IBinder sourceInputToken, int displayId) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking transferTouchFocusToImeWindow",
                        callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            return immi.transferTouchFocusToImeWindow(sourceInputToken, displayId);
        }

        @Override
        public void reportImeControl(@Nullable IBinder windowToken) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking reportImeControl", callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.reportImeControl(windowToken);
        }

        @Override
        public void onImeParentChanged() {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking onImeParentChanged", callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.onImeParentChanged();
        }

        @Override
        public void removeImeSurface() {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking removeImeSurface", callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.removeImeSurface();
        }

        @Override
        public void updateImeWindowStatus(boolean disableImeIcon) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking updateImeWindowStatus", callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.updateImeWindowStatus(disableImeIcon);
        }

        @Override
        public void maybeFinishStylusHandwriting() {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking maybeFinishStylusHandwriting",
                        callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.maybeFinishStylusHandwriting();
        }

        @Override
        public void onSessionForAccessibilityCreated(int accessibilityConnectionId,
                IAccessibilityInputMethodSession session) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking onSessionForAccessibilityCreated",
                        callingUserId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.onSessionForAccessibilityCreated(accessibilityConnectionId, session);
        }

        @Override
        public void unbindAccessibilityFromCurrentClient(int accessibilityConnectionId) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking unbindAccessibilityFromCurrentClient("
                                + "accessibilityConnectionId=%d)", callingUserId,
                        accessibilityConnectionId);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.unbindAccessibilityFromCurrentClient(accessibilityConnectionId);
        }

        @Override
        public void switchKeyboardLayout(int direction) {
            final int uid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(uid);
            if (DBG) {
                Slogf.d(mImmiTag, "User {%d} invoking switchKeyboardLayout(direction=%d)",
                        callingUserId, direction);
            }
            InputMethodManagerInternal immi = getLocalServiceForUser(callingUserId);
            immi.switchKeyboardLayout(direction);
        }
    }
}
