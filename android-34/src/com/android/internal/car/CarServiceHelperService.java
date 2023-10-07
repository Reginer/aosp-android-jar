/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.internal.car;

import static com.android.car.internal.common.CommonConstants.INVALID_PID;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_CREATED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_INVISIBLE;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_POST_UNLOCKED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STARTING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STOPPED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STOPPING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_SWITCHING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_UNLOCKED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_UNLOCKING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_VISIBLE;
import static com.android.internal.util.function.pooled.PooledLambda.obtainMessage;
import static com.android.server.wm.ActivityInterceptorCallback.PRODUCT_ORDERED_ID;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManager.DevicePolicyOperation;
import android.app.admin.DevicePolicyManager.OperationSafetyReason;
import android.app.admin.DevicePolicySafetyChecker;
import android.automotive.watchdog.internal.ICarWatchdogMonitor;
import android.automotive.watchdog.internal.ProcessIdentifier;
import android.automotive.watchdog.internal.StateType;
import android.car.builtin.util.EventLogHelper;
import android.car.watchdoglib.CarWatchdogDaemonHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hidl.manager.V1_0.IServiceManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceDebugInfo;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.system.Os;
import android.system.OsConstants;
import android.util.Dumpable;
import android.util.TimeUtils;

import com.android.car.internal.common.UserHelperLite;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.car.os.Util;
import com.android.internal.os.IResultReceiver;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.am.StackTracesDumpHelper;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserManagerInternal.UserLifecycleListener;
import com.android.server.pm.UserManagerInternal.UserVisibilityListener;
import com.android.server.utils.Slogf;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.CarLaunchParamsModifier;
import com.android.server.wm.CarLaunchParamsModifierInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * System service side companion service for CarService. Starts car service and provide necessary
 * API for CarService. Only for car product.
 *
 * @hide
 */
public class CarServiceHelperService extends SystemService
        implements Dumpable, DevicePolicySafetyChecker, CarServiceHelperInterface {

    @VisibleForTesting
    static final String TAG = "CarServiceHelper";

    // TODO(b/154033860): STOPSHIP if they're still true
    private static final boolean DBG = true;
    private static final boolean VERBOSE = true;

    private static final List<String> CAR_HIDL_INTERFACES_OF_INTEREST = Arrays.asList(
            "android.hardware.automotive.audiocontrol@1.0::IAudioControl",
            "android.hardware.automotive.audiocontrol@2.0::IAudioControl",
            "android.hardware.automotive.can@1.0::ICanBus",
            "android.hardware.automotive.can@1.0::ICanController",
            "android.hardware.automotive.evs@1.0::IEvsEnumerator",
            "android.hardware.automotive.sv@1.0::ISurroundViewService",
            "android.hardware.automotive.vehicle@2.0::IVehicle"
    );

    private static final String[] CAR_AIDL_INTERFACE_PREFIXES_OF_INTEREST = new String[] {
            "android.hardware.automotive.audiocontrol.IAudioControl/",
            "android.hardware.automotive.can.ICanController/",
            "android.hardware.automotive.evs.IEvsEnumerator/",
            "android.hardware.automotive.ivn.IIvnAndroidDevice/",
            "android.hardware.automotive.occupant_awareness.IOccupantAwareness/",
            "android.hardware.automotive.remoteaccess.IRemoteAccess/",
            "android.hardware.automotive.vehicle.IVehicle/",
    };

    // Message ID representing post-processing of process dumping.
    private static final int WHAT_POST_PROCESS_DUMPING = 1;
    // Message ID representing process killing.
    private static final int WHAT_PROCESS_KILL = 2;

    private static final String CSHS_UPDATABLE_CLASSNAME_STRING =
            "com.android.internal.car.updatable.CarServiceHelperServiceUpdatableImpl";
    private static final String PROC_PID_STAT_PATTERN =
            "(?<pid>[0-9]*)\\s\\((?<name>\\S+)\\)\\s\\S\\s(?:-?[0-9]*\\s){18}"
                    + "(?<startClockTicks>[0-9]*)\\s(?:-?[0-9]*\\s)*-?[0-9]*";
    private static final String AIDL_VHAL_INTERFACE_PREFIX =
            "android.hardware.automotive.vehicle.IVehicle/";

    static  {
        // Load this JNI before other classes are loaded.
        System.loadLibrary("carservicehelperjni");
    }

    private final Context mContext;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private boolean mSystemBootCompleted;

    private final CarLaunchParamsModifier mCarLaunchParamsModifier;

    private final Handler mHandler;
    private final HandlerThread mHandlerThread = new HandlerThread("CarServiceHelperService");

    private final ProcessTerminator mProcessTerminator = new ProcessTerminator();

    private final Pattern mProcPidStatPattern = Pattern.compile(PROC_PID_STAT_PATTERN);

    private final CarWatchdogDaemonHelper mCarWatchdogDaemonHelper;
    private final ICarWatchdogMonitorImpl mCarWatchdogMonitor = new ICarWatchdogMonitorImpl(this);
    private final CarWatchdogDaemonHelper.OnConnectionChangeListener mConnectionListener =
            (connected) -> {
                if (connected) {
                    registerMonitorToWatchdogDaemon();
                }
            };

    private final CarDevicePolicySafetyChecker mCarDevicePolicySafetyChecker;

    private CarServiceHelperServiceUpdatable mCarServiceHelperServiceUpdatable;

    /**
     * End-to-end time (from process start) for unlocking the first non-system user.
     */
    private long mFirstUnlockedUserDuration;

    public CarServiceHelperService(Context context) {
        this(context,
                new CarLaunchParamsModifier(context),
                new CarWatchdogDaemonHelper(TAG),
                /* carServiceHelperServiceUpdatable= */ null,
                /* carDevicePolicySafetyChecker= */ null
        );
    }

    @VisibleForTesting
    CarServiceHelperService(
            Context context,
            CarLaunchParamsModifier carLaunchParamsModifier,
            CarWatchdogDaemonHelper carWatchdogDaemonHelper,
            @Nullable CarServiceHelperServiceUpdatable carServiceHelperServiceUpdatable,
            @Nullable CarDevicePolicySafetyChecker carDevicePolicySafetyChecker) {
        super(context);

        mContext = context;
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCarLaunchParamsModifier = carLaunchParamsModifier;
        mCarWatchdogDaemonHelper = carWatchdogDaemonHelper;
        try {
            if (carServiceHelperServiceUpdatable == null) {
                mCarServiceHelperServiceUpdatable = (CarServiceHelperServiceUpdatable) Class
                        .forName(CSHS_UPDATABLE_CLASSNAME_STRING)
                        .getConstructor(Context.class, CarServiceHelperInterface.class,
                                CarLaunchParamsModifierInterface.class)
                        .newInstance(mContext, this,
                                mCarLaunchParamsModifier.getBuiltinInterface());
                Slogf.d(TAG, "CarServiceHelperServiceUpdatable created via reflection.");
            } else {
                mCarServiceHelperServiceUpdatable = carServiceHelperServiceUpdatable;
            }
        } catch (Exception e) {
            // TODO(b/190458000): Define recovery mechanism.
            // can't create the CarServiceHelperServiceUpdatable object
            // crash the process
            Slogf.w(TAG, e, "*** CARHELPER KILLING SYSTEM PROCESS: "
                    + "Can't create CarServiceHelperServiceUpdatable.");
            Slogf.w(TAG, "*** GOODBYE!");
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
        mCarLaunchParamsModifier.setUpdatable(
                mCarServiceHelperServiceUpdatable.getCarLaunchParamsModifierUpdatable());

        UserManagerInternal umi = LocalServices.getService(UserManagerInternal.class);
        if (umi != null) {
            umi.addUserLifecycleListener(new UserLifecycleListener() {
                @Override
                public void onUserCreated(UserInfo user, Object token) {
                    if (DBG) Slogf.d(TAG, "onUserCreated(): %s", user.toFullString());
                    mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(
                            USER_LIFECYCLE_EVENT_TYPE_CREATED, /* userFrom= */ null,
                            user.getUserHandle());
                }

                @Override
                public void onUserRemoved(UserInfo user) {
                    if (DBG) Slogf.d(TAG, "onUserRemoved(): $s", user.toFullString());
                    mCarServiceHelperServiceUpdatable.onUserRemoved(user.getUserHandle());
                }
            });
            umi.addUserVisibilityListener(new UserVisibilityListener() {
                @Override
                public void onUserVisibilityChanged(@UserIdInt int userId, boolean visible) {
                    if (DBG) {
                        Slogf.d(TAG, "onUserVisibilityChanged(%d, %b)", userId, visible);
                    }
                    int eventType = visible
                            ? USER_LIFECYCLE_EVENT_TYPE_VISIBLE
                            : USER_LIFECYCLE_EVENT_TYPE_INVISIBLE;
                    mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(eventType,
                            /* userFrom= */ null, UserHandle.of(userId));
                    mCarLaunchParamsModifier.handleUserVisibilityChanged(userId, visible);
                }
            });
        } else {
            Slogf.e(TAG, "UserManagerInternal not available - should only happen on unit tests");
        }
        mCarDevicePolicySafetyChecker = carDevicePolicySafetyChecker == null
                ? new CarDevicePolicySafetyChecker(this)
                : carDevicePolicySafetyChecker;
    }

    @Override
    public void onBootPhase(int phase) {
        EventLogHelper.writeCarHelperBootPhase(phase);
        if (DBG) Slogf.d(TAG, "onBootPhase: %d", phase);

        TimingsTraceAndSlog t = newTimingsTraceAndSlog();
        if (phase == SystemService.PHASE_THIRD_PARTY_APPS_CAN_START) {
            t.traceBegin("onBootPhase.3pApps");
            mCarLaunchParamsModifier.init();
            setupAndStartUsers(t);
            t.traceEnd();
        } else if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            t.traceBegin("onBootPhase.completed");
            synchronized (mLock) {
                mSystemBootCompleted = true;
            }
            try {
                mCarWatchdogDaemonHelper.notifySystemStateChange(
                        StateType.BOOT_PHASE, phase, /* arg2= */ 0);
            } catch (RemoteException | RuntimeException e) {
                Slogf.w(TAG, "Failed to notify boot phase change: %s", e);
            }
            ActivityTaskManagerInternal activityTaskManagerInternal = getLocalService(
                    ActivityTaskManagerInternal.class);
            activityTaskManagerInternal.registerActivityStartInterceptor(
                    PRODUCT_ORDERED_ID,
                    new CarActivityInterceptor(mCarServiceHelperServiceUpdatable
                            .getCarActivityInterceptorUpdatable()));
            t.traceEnd();
        }
    }

    @Override
    public void onStart() {
        EventLogHelper.writeCarHelperStart();

        mCarWatchdogDaemonHelper.addOnConnectionChangeListener(mConnectionListener);
        mCarWatchdogDaemonHelper.connect();
        mCarServiceHelperServiceUpdatable.onStart();
    }

    @Override
    public void dump(PrintWriter pw, String[] args) {
        // Usage: adb shell dumpsys system_server_dumper --name CarServiceHelper
        if (args == null || args.length == 0 || args[0].equals("-a")) {
            pw.printf("System boot completed: %b\n", mSystemBootCompleted);
            pw.print("First unlocked user duration: ");
            TimeUtils.formatDuration(mFirstUnlockedUserDuration, pw); pw.println();
            pw.printf("Queued tasks: %d\n", mProcessTerminator.mQueuedTask);
            mCarServiceHelperServiceUpdatable.dump(pw, args);
            mCarDevicePolicySafetyChecker.dump(pw);
            return;
        }

        if ("--is-operation-safe".equals(args[0]) & args.length > 1) {
            String arg1 = args[1];
            int operation = 0;
            try {
                operation = Integer.parseInt(arg1);
            } catch (Exception e) {
                pw.printf("Invalid operation type: %s\n", arg1);
                return;

            }
            int reason = getUnsafeOperationReason(operation);
            boolean safe = reason == DevicePolicyManager.OPERATION_SAFETY_REASON_NONE;
            pw.printf("Operation %s is %s. Reason: %s\n",
                    DevicePolicyManager.operationToString(operation),
                    safe ? "SAFE" : "UNSAFE",
                    DevicePolicyManager.operationSafetyReasonToString(reason));
            return;
        }

        if ("--user-metrics-only".equals(args[0]) || "--dump-service-stacks".equals(args[0])) {
            mCarServiceHelperServiceUpdatable.dump(pw, args);
            return;
        }

        pw.printf("Invalid args: %s\n", Arrays.toString(args));
    }

    @Override
    public String getDumpableName() {
        return "CarServiceHelper";
    }

    @Override
    public void onUserUnlocking(@NonNull TargetUser user) {
        EventLogHelper.writeCarHelperUserUnlocking(user.getUserIdentifier());
        if (DBG) Slogf.d(TAG, "onUserUnlocking(%s)", user);

        mCarServiceHelperServiceUpdatable
                .sendUserLifecycleEvent(USER_LIFECYCLE_EVENT_TYPE_UNLOCKING,
                        /* userFrom= */ null, user.getUserHandle());
    }

    @Override
    public void onUserUnlocked(@NonNull TargetUser user) {
        int userId = user.getUserIdentifier();
        EventLogHelper.writeCarHelperUserUnlocked(userId);
        if (DBG) Slogf.d(TAG, "onUserUnlocked(%s)", user);

        if (mFirstUnlockedUserDuration == 0 && !UserHelperLite.isHeadlessSystemUser(userId)) {
            mFirstUnlockedUserDuration = SystemClock.elapsedRealtime()
                    - Process.getStartElapsedRealtime();
            Slogf.i(TAG, "Time to unlock 1st user(%s): %s", user,
                    TimeUtils.formatDuration(mFirstUnlockedUserDuration));
        }
        mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(USER_LIFECYCLE_EVENT_TYPE_UNLOCKED,
                /* userFrom= */ null, user.getUserHandle());
    }

    @Override
    public void onUserStarting(@NonNull TargetUser user) {
        EventLogHelper.writeCarHelperUserStarting(user.getUserIdentifier());
        if (DBG) Slogf.d(TAG, "onUserStarting(%s)", user);

        mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(USER_LIFECYCLE_EVENT_TYPE_STARTING,
                /* userFrom= */ null, user.getUserHandle());
        int userId = user.getUserIdentifier();
        mCarLaunchParamsModifier.handleUserStarting(userId);
    }

    @Override
    public void onUserStopping(@NonNull TargetUser user) {
        EventLogHelper.writeCarHelperUserStopping(user.getUserIdentifier());
        if (DBG) Slogf.d(TAG, "onUserStopping(%s)", user);

        mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(USER_LIFECYCLE_EVENT_TYPE_STOPPING,
                /* userFrom= */ null, user.getUserHandle());
        int userId = user.getUserIdentifier();
        mCarLaunchParamsModifier.handleUserStopped(userId);
    }

    @Override
    public void onUserStopped(@NonNull TargetUser user) {
        EventLogHelper.writeCarHelperUserStopped(user.getUserIdentifier());
        if (DBG) Slogf.d(TAG, "onUserStopped(%s)", user);

        mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(USER_LIFECYCLE_EVENT_TYPE_STOPPED,
                /* userFrom= */ null, user.getUserHandle());
    }

    @Override
    public void onUserSwitching(@Nullable TargetUser from, @NonNull TargetUser to) {
        EventLogHelper.writeCarHelperUserSwitching(
                from == null ? UserHandle.USER_NULL : from.getUserIdentifier(),
                to.getUserIdentifier());
        if (DBG) Slogf.d(TAG, "onUserSwitching(%s>>%s)", from, to);

        mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(
                USER_LIFECYCLE_EVENT_TYPE_SWITCHING, from.getUserHandle(),
                to.getUserHandle());
        int userId = to.getUserIdentifier();
        mCarLaunchParamsModifier.handleCurrentUserSwitching(userId);
    }

    @Override
    public void onUserCompletedEvent(TargetUser user, UserCompletedEventType eventType) {
        UserHandle handle = user.getUserHandle();
        if (eventType.includesOnUserUnlocked()) {
            mCarServiceHelperServiceUpdatable.sendUserLifecycleEvent(
                    USER_LIFECYCLE_EVENT_TYPE_POST_UNLOCKED, /* userFrom= */ null, handle);
        }
    }

    @Override // from DevicePolicySafetyChecker
    @OperationSafetyReason
    public int getUnsafeOperationReason(@DevicePolicyOperation int operation) {
        return mCarDevicePolicySafetyChecker.isDevicePolicyOperationSafe(operation)
                ? DevicePolicyManager.OPERATION_SAFETY_REASON_NONE
                : DevicePolicyManager.OPERATION_SAFETY_REASON_DRIVING_DISTRACTION;
    }

    @Override // from DevicePolicySafetyChecker
    public boolean isSafeOperation(@OperationSafetyReason int reason) {
        return mCarDevicePolicySafetyChecker.isSafe();
    }

    @Override // from DevicePolicySafetyChecker
    public void onFactoryReset(IResultReceiver callback) {
        if (DBG) Slogf.d(TAG, "onFactoryReset: %s", callback);
        if (callback != null) {
            mCarServiceHelperServiceUpdatable.onFactoryReset((resultCode, resultData) -> {
                try {
                    callback.send(resultCode, resultData);
                } catch (RemoteException e) {
                    Slogf.w(TAG, e,
                            "Callback to DevicePolicySafetyChecker threw RemoteException");
                }
            });
        }
    }

    @Override
    public boolean isUserSupported(TargetUser user) {
        boolean isPreCreated = user.isPreCreated();
        if (isPreCreated && DBG) {
            Slogf.d(TAG, "Not supporting Pre-created user %s", user);
        }
        return !isPreCreated;
    }

    private TimingsTraceAndSlog newTimingsTraceAndSlog() {
        return new TimingsTraceAndSlog(TAG, Trace.TRACE_TAG_SYSTEM_SERVER);
    }

    private void setupAndStartUsers(@NonNull TimingsTraceAndSlog t) {
        // TODO(b/156263735): decide if it should return in case the device's on Retail Mode
        t.traceBegin("setupAndStartUsers");
        mCarServiceHelperServiceUpdatable.initBootUser();
        t.traceEnd();
    }

    // Adapted from frameworks/base/services/core/java/com/android/server/Watchdog.java
    // TODO(b/131861630) use implementation common with Watchdog.java
    private static void addInterestingHidlPids(HashSet<Integer> pids) {
        try {
            IServiceManager serviceManager = IServiceManager.getService();
            ArrayList<IServiceManager.InstanceDebugInfo> dump =
                    serviceManager.debugDump();
            for (IServiceManager.InstanceDebugInfo info : dump) {
                if (info.pid == IServiceManager.PidConstant.NO_PID) {
                    continue;
                }

                if (Watchdog.HAL_INTERFACES_OF_INTEREST.contains(info.interfaceName) ||
                        CAR_HIDL_INTERFACES_OF_INTEREST.contains(info.interfaceName)) {
                    pids.add(info.pid);
                }
            }
        } catch (RemoteException e) {
            Slogf.w(TAG, "Remote exception while querying HIDL service manager", e);
        }
    }

    // Adapted from frameworks/base/services/core/java/com/android/server/Watchdog.java
    // TODO(b/131861630) use implementation common with Watchdog.java
    private static void addInterestingAidlPids(HashSet<Integer> pids) {
        ServiceDebugInfo[] infos = ServiceManager.getServiceDebugInfo();
        if (infos == null) return;

        for (ServiceDebugInfo info : infos) {
            if (matchesInterestingAidlInterfacePrefixes(
                    Watchdog.AIDL_INTERFACE_PREFIXES_OF_INTEREST, info.name)
                    || matchesInterestingAidlInterfacePrefixes(
                    CAR_AIDL_INTERFACE_PREFIXES_OF_INTEREST, info.name)) {
                pids.add(info.debugPid);
            }
        }
    }

    private static boolean matchesInterestingAidlInterfacePrefixes(String[] interfacePrefixes,
            String interfaceName) {
        for (String prefix : interfacePrefixes) {
            if (interfaceName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // Adapted from frameworks/base/services/core/java/com/android/server/Watchdog.java
    // TODO(b/131861630) use implementation common with Watchdog.java
    private static ArrayList<Integer> getInterestingNativePids() {
        HashSet<Integer> pids = new HashSet<Integer>();
        addInterestingHidlPids(pids);
        addInterestingAidlPids(pids);

        int[] nativePids = Process.getPidsForCommands(Watchdog.NATIVE_STACKS_OF_INTEREST);
        if (nativePids != null) {
            for (int i : nativePids) {
                pids.add(i);
            }
        }

        return new ArrayList<Integer>(pids);
    }

    /**
     * Dumps service stack
     */
    // Borrowed from Watchdog.java.  Create an ANR file from the call stacks.
    @Override
    @Nullable
    public File dumpServiceStacks() {
        ArrayList<Integer> pids = new ArrayList<>();
        pids.add(Process.myPid());

        // Use the long version used by Watchdog since the short version is removed by the compiler.
        return StackTracesDumpHelper.dumpStackTraces(
                pids, /* processCpuTracker= */ null, /* lastPids= */ null,
                CompletableFuture.completedFuture(getInterestingNativePids()),
                /* logExceptionCreatingFile= */ null, /* subject= */ null,
                /* criticalEventSection= */ null, Runnable::run, /* latencyTracker= */ null);
    }

    @Override
    public void setProcessGroup(int pid, int group) {
        Process.setProcessGroup(pid, group);
    }

    @Override
    public int getProcessGroup(int pid) {
        return Process.getProcessGroup(pid);
    }

    @Override
    public boolean startUserInBackgroundVisibleOnDisplay(int userId, int displayId) {
        ActivityManager am = mContext.getSystemService(ActivityManager.class);
        return am.startUserInBackgroundVisibleOnDisplay(userId, displayId);
    }

    @Override
    public void setProcessProfile(int pid, int uid, @NonNull String profile) {
        Util.setProcessProfile(pid, uid, profile);
    }

    @Override
    public int fetchAidlVhalPid() {
        ServiceDebugInfo[] infos = ServiceManager.getServiceDebugInfo();
        if (infos == null) {
            Slogf.w(TAG, "Service debug info returned by the service manager is null");
            return INVALID_PID;
        }

        for (ServiceDebugInfo info : infos) {
            if (info.name.startsWith(AIDL_VHAL_INTERFACE_PREFIX)) {
                return info.debugPid;
            }
        }
        Slogf.w(TAG, "Service manager doesn't have the AIDL VHAL service instance for interface"
                + " prefix %s", AIDL_VHAL_INTERFACE_PREFIX);
        return INVALID_PID;
    }

    private void handleClientsNotResponding(@NonNull List<ProcessIdentifier> processIdentifiers) {
        mProcessTerminator.requestTerminateProcess(processIdentifiers);
    }

    private void registerMonitorToWatchdogDaemon() {
        try {
            mCarWatchdogDaemonHelper.registerMonitor(mCarWatchdogMonitor);
            synchronized (mLock) {
                if (!mSystemBootCompleted) {
                    return;
                }
            }
            mCarWatchdogDaemonHelper.notifySystemStateChange(
                    StateType.BOOT_PHASE, SystemService.PHASE_BOOT_COMPLETED, /* arg2= */ 0);
        } catch (RemoteException | RuntimeException e) {
            Slogf.w(TAG, "Cannot register to car watchdog daemon: %s", e);
        }
    }

    private void killProcessAndReportToMonitor(ProcessIdentifier processIdentifier) {
        ProcessInfo processInfo = getProcessInfo(processIdentifier.pid);
        if (!processInfo.doMatch(processIdentifier.pid, processIdentifier.startTimeMillis)) {
            return;
        }
        String cmdline = getProcessCmdLine(processIdentifier.pid);
        Process.killProcess(processIdentifier.pid);
        Slogf.w(TAG, "carwatchdog killed %s %s", cmdline, processInfo);
        try {
            mCarWatchdogDaemonHelper.tellDumpFinished(mCarWatchdogMonitor, processIdentifier);
        } catch (RemoteException | RuntimeException e) {
            Slogf.w(TAG, "Cannot report monitor result to car watchdog daemon: %s", e);
        }
    }

    private static String getProcessCmdLine(int pid) {
        String filename = "/proc/" + pid + "/cmdline";
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine().replace('\0', ' ').trim();
            int index = line.indexOf(' ');
            if (index != -1) {
                line = line.substring(0, index);
            }
            return Paths.get(line).getFileName().toString();
        } catch (IOException | RuntimeException e) {
            Slogf.w(TAG, "Cannot read %s", filename);
            return ProcessInfo.UNKNOWN_PROCESS;
        }
    }

    private ProcessInfo getProcessInfo(int pid) {
        String filename = "/proc/" + pid + "/stat";
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine().replace('\0', ' ').trim();
            Matcher m = mProcPidStatPattern.matcher(line);
            if (m.find()) {
                int readPid = Integer.parseInt(Objects.requireNonNull(m.group("pid")));
                if (readPid == pid) {
                    return new ProcessInfo(pid, m.group("name"),
                            Long.parseLong(Objects.requireNonNull(m.group("startClockTicks"))));
                }
            }
        } catch (IOException | RuntimeException e) {
            Slogf.w(TAG, e, "Cannot read %s", filename);
        }
        return new ProcessInfo(pid, ProcessInfo.UNKNOWN_PROCESS, ProcessInfo.INVALID_START_TIME);
    }

    @Override
    public void setSafetyMode(boolean safe) {
        mCarDevicePolicySafetyChecker.setSafe(safe);
    }

    @Override
    public UserHandle createUserEvenWhenDisallowed(String name, String userType, int flags) {
        if (DBG) {
            Slogf.d(TAG, "createUserEvenWhenDisallowed(): name=%s, type=%s, flags=%s",
                    UserHelperLite.safeName(name), userType, UserInfo.flagsToString(flags));
        }
        UserManagerInternal umi = LocalServices.getService(UserManagerInternal.class);
        try {
            UserInfo user = umi.createUserEvenWhenDisallowed(name, userType, flags,
                    /* disallowedPackages= */ null, /* token= */ null);
            if (DBG) {
                Slogf.d(TAG, "User created: %s", (user == null ? "null" : user.toFullString()));
            }
            // TODO(b/172691310): decide if user should be affiliated when DeviceOwner is set
            return user.getUserHandle();
        } catch (UserManager.CheckedUserOperationException e) {
            Slogf.e(TAG, e, "Error creating user");
            return null;
        }
    }

    @Override
    public int getMainDisplayAssignedToUser(int userId) {
        UserManagerInternal umi = LocalServices.getService(UserManagerInternal.class);
        int displayId = umi.getMainDisplayAssignedToUser(userId);
        if (DBG) {
            Slogf.d(TAG, "getMainDisplayAssignedToUser(%d): %d", userId, displayId);
        }
        return displayId;
    }

    @Override
    public int getUserAssignedToDisplay(int displayId) {
        UserManagerInternal umi = LocalServices.getService(UserManagerInternal.class);
        int userId = umi.getUserAssignedToDisplay(displayId);
        if (DBG) {
            Slogf.d(TAG, "getUserAssignedToDisplay(%d): %d", displayId, userId);
        }
        return userId;
    }

    private class ICarWatchdogMonitorImpl extends ICarWatchdogMonitor.Stub {
        private final WeakReference<CarServiceHelperService> mService;

        private ICarWatchdogMonitorImpl(CarServiceHelperService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void onClientsNotResponding(List<ProcessIdentifier> processIdentifiers) {
            CarServiceHelperService service = mService.get();
            if (service == null || processIdentifiers == null || processIdentifiers.isEmpty()) {
                return;
            }
            service.handleClientsNotResponding(processIdentifiers);
        }

        @Override
        public String getInterfaceHash() {
            return ICarWatchdogMonitor.HASH;
        }

        @Override
        public int getInterfaceVersion() {
            return ICarWatchdogMonitor.VERSION;
        }
    }

    private final class ProcessTerminator {

        private static final long ONE_SECOND_MS = 1_000L;

        private final Object mProcessLock = new Object();
        private ExecutorService mExecutor;
        @GuardedBy("mProcessLock")
        private int mQueuedTask;

        public void requestTerminateProcess(@NonNull List<ProcessIdentifier> processIdentifiers) {
            synchronized (mProcessLock) {
                // If there is a running thread, we re-use it instead of starting a new thread.
                if (mExecutor == null) {
                    mExecutor = Executors.newSingleThreadExecutor();
                }
                mQueuedTask++;
            }
            mExecutor.execute(() -> {
                for (int i = 0; i < processIdentifiers.size(); i++) {
                    ProcessIdentifier processIdentifier = processIdentifiers.get(i);
                    ProcessInfo processInfo = getProcessInfo(processIdentifier.pid);
                    if (processInfo.doMatch(processIdentifier.pid,
                            processIdentifier.startTimeMillis)) {
                        dumpAndKillProcess(processIdentifier);
                    }
                }
                // mExecutor will be stopped from the main thread, if there is no queued task.
                mHandler.sendMessage(obtainMessage(ProcessTerminator::postProcessing, this)
                        .setWhat(WHAT_POST_PROCESS_DUMPING));
            });
        }

        private void postProcessing() {
            synchronized (mProcessLock) {
                mQueuedTask--;
                if (mQueuedTask == 0) {
                    mExecutor.shutdown();
                    mExecutor = null;
                }
            }
        }

        private void dumpAndKillProcess(ProcessIdentifier processIdentifier) {
            if (DBG) {
                Slogf.d(TAG, "Dumping and killing process(pid: %d)", processIdentifier.pid);
            }
            ArrayList<Integer> javaPids = new ArrayList<>(1);
            ArrayList<Integer> nativePids = new ArrayList<>();
            try {
                if (isJavaApp(processIdentifier.pid)) {
                    javaPids.add(processIdentifier.pid);
                } else {
                    nativePids.add(processIdentifier.pid);
                }
            } catch (IOException e) {
                Slogf.w(TAG, "Cannot get process information: %s", e);
                return;
            }
            nativePids.addAll(getInterestingNativePids());
            long startDumpTime = SystemClock.uptimeMillis();
            StackTracesDumpHelper.dumpStackTraces(
                    /* firstPids= */ javaPids, /* processCpuTracker= */ null, /* lastPids= */ null,
                    /* nativePids= */ CompletableFuture.completedFuture(nativePids),
                    /* logExceptionCreatingFile= */ null,
                    /* auxiliaryTaskExecutor= */ Runnable::run, /* latencyTracker= */ null);
            long dumpTime = SystemClock.uptimeMillis() - startDumpTime;
            if (DBG) {
                Slogf.d(TAG, "Dumping process took %dms", dumpTime);
            }
            // To give clients a chance of wrapping up before the termination.
            if (dumpTime < ONE_SECOND_MS) {
                mHandler.sendMessageDelayed(obtainMessage(
                        CarServiceHelperService::killProcessAndReportToMonitor,
                        CarServiceHelperService.this, processIdentifier).setWhat(WHAT_PROCESS_KILL),
                        ONE_SECOND_MS - dumpTime);
            } else {
                killProcessAndReportToMonitor(processIdentifier);
            }
        }

        private boolean isJavaApp(int pid) throws IOException {
            Path exePath = new File("/proc/" + pid + "/exe").toPath();
            String target = Files.readSymbolicLink(exePath).toString();
            // Zygote's target exe is also /system/bin/app_process32 or /system/bin/app_process64.
            // But, we can be very sure that Zygote will not be the client of car watchdog daemon.
            return target.equals("/system/bin/app_process32") ||
                    target.equals("/system/bin/app_process64");
        }
    }

    private static final class ProcessInfo {
        public static final String UNKNOWN_PROCESS = "unknown process";
        public static final int INVALID_START_TIME = -1;

        private static final long MILLIS_PER_JIFFY = 1000L / Os.sysconf(OsConstants._SC_CLK_TCK);

        public final int pid;
        public final String name;
        public final long startTimeMillis;

        ProcessInfo(int pid, String name, long startClockTicks) {
            this.pid = pid;
            this.name = name;
            this.startTimeMillis = startClockTicks != INVALID_START_TIME
                    ? startClockTicks * MILLIS_PER_JIFFY : INVALID_START_TIME;
        }

        boolean doMatch(int pid, long startTimeMillis) {
            // Start time reported by the services that monitor the process health will be either
            // the actual start time of the pid or the elapsed real time when the pid was last seen
            // alive. Thus, verify whether the given start time is at least the actual start time of
            // the pid.
            return this.pid == pid && (this.startTimeMillis == INVALID_START_TIME
                    || this.startTimeMillis <= startTimeMillis);
        }

        @Override
        public String toString() {
            return new StringBuilder("ProcessInfo { pid = ").append(pid)
                    .append(", name = ").append(name)
                    .append(", startTimeMillis = ")
                    .append(startTimeMillis != INVALID_START_TIME ? startTimeMillis : "invalid")
                    .append(" }").toString();
        }
    }
}
