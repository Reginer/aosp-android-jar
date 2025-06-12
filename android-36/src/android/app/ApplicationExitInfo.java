/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.app;

import android.annotation.CurrentTimeMillisLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager.RunningAppProcessInfo.Importance;
import android.icu.text.SimpleDateFormat;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.DebugUtils;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import android.util.proto.WireTypeMismatchException;

import com.android.internal.util.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * Describes the information of an application process's death.
 *
 * <p>
 * Application process could die for many reasons, for example {@link #REASON_LOW_MEMORY}
 * when it was killed by the system because it was running low on memory. Reason
 * of the death can be retrieved via {@link #getReason}. Besides the reason, there are a few other
 * auxiliary APIs like {@link #getStatus} and {@link #getImportance} to help the caller with
 * additional diagnostic information.
 * </p>
 *
 */
public final class ApplicationExitInfo implements Parcelable {

    /**
     * Application process died due to unknown reason.
     */
    public static final int REASON_UNKNOWN = 0;

    /**
     * Application process exit normally by itself, for example,
     * via {@link java.lang.System#exit}; {@link #getStatus} will specify the exit code.
     *
     * <p>Applications should normally not do this, as the system has a better knowledge
     * in terms of process management.</p>
     */
    public static final int REASON_EXIT_SELF = 1;

    /**
     * Application process died due to the result of an OS signal; for example,
     * {@link android.system.OsConstants#SIGKILL}; {@link #getStatus} will specify the signal
     * number.
     */
    public static final int REASON_SIGNALED = 2;

    /**
     * Application process was killed by the system low memory killer, meaning the system was
     * under memory pressure at the time of kill.
     *
     * <p class="note">
     * Not all devices support reporting {@link #REASON_LOW_MEMORY}; on a device with no such
     * support, when a process is killed due to memory pressure, the {@link #getReason} will return
     * {@link #REASON_SIGNALED} and {@link #getStatus} will return
     * the value {@link android.system.OsConstants#SIGKILL}.
     *
     * Application should use {@link android.app.ActivityManager#isLowMemoryKillReportSupported()
     * ActivityManager.isLowMemoryKillReportSupported()} to check
     * if the device supports reporting {@link #REASON_LOW_MEMORY} or not.
     * </p>
     */
    public static final int REASON_LOW_MEMORY = 3;

    /**
     * Application process died because of an unhandled exception in Java code.
     */
    public static final int REASON_CRASH = 4;

    /**
     * Application process died because of a native code crash.
     */
    public static final int REASON_CRASH_NATIVE = 5;

    /**
     * Application process was killed due to being unresponsive (ANR).
     */
    public static final int REASON_ANR = 6;

    /**
     * Application process was killed because of initialization failure,
     * for example, it took too long to attach to the system during the start,
     * or there was an error during initialization.
     */
    public static final int REASON_INITIALIZATION_FAILURE = 7;

    /**
     * Application process was killed due to a runtime permission change.
     */
    public static final int REASON_PERMISSION_CHANGE = 8;

    /**
     * Application process was killed by the system due to excessive resource usage.
     */
    public static final int REASON_EXCESSIVE_RESOURCE_USAGE = 9;

    /**
     * Application process was killed because of the user request, for example,
     * user clicked the "Force stop" button of the application in the Settings,
     * or removed the application away from Recents.
     * <p>
     * Prior to {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE}, one of the uses of this
     * reason was to indicate that an app was killed due to it being updated or any of its component
     * states have changed without {@link android.content.pm.PackageManager#DONT_KILL_APP}
     */
    public static final int REASON_USER_REQUESTED = 10;

    /**
     * Application process was killed, because the user it is running as on devices
     * with mutlple users, was stopped.
     */
    public static final int REASON_USER_STOPPED = 11;

    /**
     * Application process was killed because its dependency was going away, for example,
     * a stable content provider connection's client will be killed if the provider is killed.
     */
    public static final int REASON_DEPENDENCY_DIED = 12;

    /**
     * Application process was killed by the system for various other reasons which are
     * not by problems in apps and not actionable by apps, for example, the system just
     * finished updates; {@link #getDescription} will specify the cause given by the system.
     */
    public static final int REASON_OTHER = 13;

    /**
     * Application process was killed by App Freezer, for example, because it receives
     * sync binder transactions while being frozen.
     */
    public static final int REASON_FREEZER = 14;

    /**
     * Application process was killed because the app was disabled, or any of its
     * component states have changed without {@link android.content.pm.PackageManager#DONT_KILL_APP}
     * <p>
     * Prior to {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE},
     * {@link #REASON_USER_REQUESTED} was used to indicate that an app was updated.
     */
    public static final int REASON_PACKAGE_STATE_CHANGE = 15;

    /**
     * Application process was killed because it was updated.
     * <p>
     * Prior to {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE},
     * {@link #REASON_USER_REQUESTED} was used to indicate that an app was updated.
     */
    public static final int REASON_PACKAGE_UPDATED = 16;

    /**
     * Application process kills subreason is unknown.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_UNKNOWN = 0;

    /**
     * Application process was killed because user quit it on the "wait for debugger" dialog;
     * this would be set when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_WAIT_FOR_DEBUGGER = 1;

    /**
     * Application process was killed by the activity manager because there were too many cached
     * processes; this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_TOO_MANY_CACHED = 2;

    /**
     * Application process was killed by the activity manager because there were too many empty
     * processes; this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_TOO_MANY_EMPTY = 3;

    /**
     * Application process was killed by the activity manager because there were too many cached
     * processes and this process had been in empty state for a long time;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_TRIM_EMPTY = 4;

    /**
     * Application process was killed by the activity manager because system was on memory pressure
     * and this process took large amount of cached memory;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_LARGE_CACHED = 5;

    /**
     * Application process was killed by the activity manager because the system was on low memory
     * pressure for a significant amount of time since last idle;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_MEMORY_PRESSURE = 6;

    /**
     * Application process was killed by the activity manager due to excessive CPU usage;
     * this would be set only when the reason is {@link #REASON_EXCESSIVE_RESOURCE_USAGE}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_EXCESSIVE_CPU = 7;

    /**
     * System update has done (so the system update process should be killed);
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_SYSTEM_UPDATE_DONE = 8;

    /**
     * Kill all foreground services, for now it only occurs when enabling the quiet
     * mode for the managed profile;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_KILL_ALL_FG = 9;

    /**
     * All background processes except certain ones were killed, for now it only occurs
     * when the density of the default display is changed;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_KILL_ALL_BG_EXCEPT = 10;

    /**
     * The process associated with the UID was explicitly killed, for example,
     * it could be because of platform compatibility overrides;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_KILL_UID = 11;

    /**
     * The process was explicitly killed with its PID, typically because of
     * the low memory for surfaces;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_KILL_PID = 12;

    /**
     * The start of the process was invalid;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_INVALID_START = 13;

    /**
     * The process was killed because it's in an invalid state, typically
     * it's triggered from SHELL;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_INVALID_STATE = 14;

    /**
     * The process was killed when it's imperceptible to user, because it was
     * in a bad state;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_IMPERCEPTIBLE = 15;

    /**
     * The process was killed because it's being moved out from LRU list;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_REMOVE_LRU = 16;

    /**
     * The process was killed because it's isolated and was in a cached state;
     * this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_ISOLATED_NOT_NEEDED = 17;

    /**
     * The process was killed because it's in forced-app-standby state, and it's cached and
     * its uid state is idle; this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_CACHED_IDLE_FORCED_APP_STANDBY = 18;

    /**
     * The process was killed because it fails to freeze/unfreeze binder
     * or query binder frozen info while being frozen.
     * this would be set only when the reason is {@link #REASON_FREEZER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_FREEZER_BINDER_IOCTL = 19;

    /**
     * The process was killed because it receives sync binder transactions
     * while being frozen.
     * this would be set only when the reason is {@link #REASON_FREEZER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_FREEZER_BINDER_TRANSACTION = 20;

    /**
     * The process was killed because of force-stop, it could be due to that
     * the user clicked the "Force stop" button of the application in the Settings;
     * this would be set only when the reason is {@link #REASON_USER_REQUESTED}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_FORCE_STOP = 21;

    /**
     * The process was killed because the user removed the application away from Recents;
     * this would be set only when the reason is {@link #REASON_USER_REQUESTED}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_REMOVE_TASK = 22;

    /**
     * The process was killed because the user stopped the application from the task manager;
     * this would be set only when the reason is {@link #REASON_USER_REQUESTED}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_STOP_APP = 23;

    /**
     * The process was killed because the user stopped the application from developer options,
     * or via the adb shell commmand interface; this would be set only when the reason is
     * {@link #REASON_USER_REQUESTED}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_KILL_BACKGROUND = 24;

    /**
     * The process was killed because of package update; this would be set only when the reason is
     * {@link #REASON_USER_REQUESTED}.
     *
     * For internal use only.
     *
     * @deprecated starting {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE},
     * an app being killed due to a package update will have the reason
     * {@link #REASON_PACKAGE_UPDATED}
     *
     * @hide
     */
    public static final int SUBREASON_PACKAGE_UPDATE = 25;

    /**
     * The process was killed because of undelivered broadcasts; this would be set only when the
     * reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_UNDELIVERED_BROADCAST = 26;

    /**
     * The process was killed because its associated SDK sandbox process (where it had loaded SDKs)
     * had died; this would be set only when the reason is {@link #REASON_DEPENDENCY_DIED}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_SDK_SANDBOX_DIED = 27;

    /**
     * The process was killed because it was an SDK sandbox process that was either not usable or
     * was no longer being used; this would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_SDK_SANDBOX_NOT_NEEDED = 28;

    /**
     * The process was killed because the binder proxy limit for system server was exceeded.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_EXCESSIVE_BINDER_OBJECTS = 29;

    /**
     * The process was killed by the [kernel] Out-of-memory (OOM) killer; this
     * would be set only when the reason is {@link #REASON_LOW_MEMORY}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_OOM_KILL = 30;

    /**
     * The process was killed because its async kernel binder buffer is running out
     * while being frozen.
     * this would be set only when the reason is {@link #REASON_FREEZER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_FREEZER_BINDER_ASYNC_FULL = 31;

    /**
     * The process was killed because it was sending too many broadcasts while it is in the
     * Cached state. This would be set only when the reason is {@link #REASON_OTHER}.
     *
     * For internal use only.
     * @hide
     */
    public static final int SUBREASON_EXCESSIVE_OUTGOING_BROADCASTS_WHILE_CACHED = 32;

    // If there is any OEM code which involves additional app kill reasons, it should
    // be categorized in {@link #REASON_OTHER}, with subreason code starting from 1000.

    /**
     * @see #getPid
     */
    private int mPid;

    /**
     * @see #getRealUid
     */
    private int mRealUid;

    /**
     * @see #getPackageUid
     */
    private int mPackageUid;

    /**
     * @see #getDefiningUid
     */
    private int mDefiningUid;

    /**
     * @see #getProcessName
     */
    private String mProcessName;

    /**
     * @see #getReason
     */
    private @Reason int mReason;

    /**
     * @see #getStatus
     */
    private int mStatus;

    /**
     * @see #getImportance
     */
    private @Importance int mImportance;

    /**
     * @see #getPss
     */
    private long mPss;

    /**
     * @see #getRss
     */
    private long mRss;

    /**
     * @see #getTimestamp
     */
    private @CurrentTimeMillisLong long mTimestamp;

    /**
     * @see #getDescription
     */
    private @Nullable String mDescription;

    /**
     * @see #getSubReason
     */
    private @SubReason int mSubReason;

    /**
     * @see #getConnectionGroup
     */
    private int mConnectionGroup;

    /**
     * @see #getPackageName
     */
    private String mPackageName;

    /**
     * @see #getPackageList
     */
    private String[] mPackageList;

    /**
     * @see #getProcessStateSummary
     */
    private byte[] mState;

    /**
     * The file to the trace file in the storage;
     *
     * for system internal use only, will not retain across processes.
     *
     * @see #getTraceInputStream
     */
    private File mTraceFile;

    /**
     * The Binder interface to retrieve the file descriptor to
     * the trace file from the system.
     */
    private IAppTraceRetriever mAppTraceRetriever;

    /**
     * ParcelFileDescriptor pointing to a native tombstone.
     *
     * @see #getTraceInputStream
     */
    private IParcelFileDescriptorRetriever mNativeTombstoneRetriever;

    /**
     * Whether or not we've logged this into the statsd.
     *
     * for system internal use only, will not retain across processes.
     */
    private boolean mLoggedInStatsd;

    /**
     * Whether or not this process hosts one or more foreground services.
     *
     * for system internal use only, will not retain across processes.
     */
    private boolean mHasForegroundServices;

    /** @hide */
    @IntDef(prefix = { "REASON_" }, value = {
        REASON_UNKNOWN,
        REASON_EXIT_SELF,
        REASON_SIGNALED,
        REASON_LOW_MEMORY,
        REASON_CRASH,
        REASON_CRASH_NATIVE,
        REASON_ANR,
        REASON_INITIALIZATION_FAILURE,
        REASON_PERMISSION_CHANGE,
        REASON_EXCESSIVE_RESOURCE_USAGE,
        REASON_USER_REQUESTED,
        REASON_USER_STOPPED,
        REASON_DEPENDENCY_DIED,
        REASON_OTHER,
        REASON_FREEZER,
        REASON_PACKAGE_STATE_CHANGE,
        REASON_PACKAGE_UPDATED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Reason {}

    /** @hide */
    @IntDef(prefix = { "SUBREASON_" }, value = {
        SUBREASON_UNKNOWN,
        SUBREASON_WAIT_FOR_DEBUGGER,
        SUBREASON_TOO_MANY_CACHED,
        SUBREASON_TOO_MANY_EMPTY,
        SUBREASON_TRIM_EMPTY,
        SUBREASON_LARGE_CACHED,
        SUBREASON_MEMORY_PRESSURE,
        SUBREASON_EXCESSIVE_CPU,
        SUBREASON_SYSTEM_UPDATE_DONE,
        SUBREASON_KILL_ALL_FG,
        SUBREASON_KILL_ALL_BG_EXCEPT,
        SUBREASON_KILL_UID,
        SUBREASON_KILL_PID,
        SUBREASON_INVALID_START,
        SUBREASON_INVALID_STATE,
        SUBREASON_IMPERCEPTIBLE,
        SUBREASON_REMOVE_LRU,
        SUBREASON_ISOLATED_NOT_NEEDED,
        SUBREASON_FREEZER_BINDER_IOCTL,
        SUBREASON_FREEZER_BINDER_TRANSACTION,
        SUBREASON_FORCE_STOP,
        SUBREASON_REMOVE_TASK,
        SUBREASON_STOP_APP,
        SUBREASON_KILL_BACKGROUND,
        SUBREASON_PACKAGE_UPDATE,
        SUBREASON_UNDELIVERED_BROADCAST,
        SUBREASON_EXCESSIVE_BINDER_OBJECTS,
        SUBREASON_OOM_KILL,
        SUBREASON_FREEZER_BINDER_ASYNC_FULL,
        SUBREASON_EXCESSIVE_OUTGOING_BROADCASTS_WHILE_CACHED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SubReason {}

    /**
     * The process id of the process that died.
     */
    public int getPid() {
        return mPid;
    }

    /**
     * The kernel user identifier of the process, most of the time the system uses this
     * to do access control checks. It's typically the uid of the package where the component is
     * running from, except the case of isolated process, where this field identifies the kernel
     * user identifier that this process is actually running with, while the {@link #getPackageUid}
     * identifies the kernel user identifier that is assigned at the package installation time.
     */
    public int getRealUid() {
        return mRealUid;
    }

    /**
     * Similar to {@link #getRealUid}, it's the kernel user identifier that is assigned at the
     * package installation time.
     */
    public int getPackageUid() {
        return mPackageUid;
    }

    /**
     * Return the defining kernel user identifier, maybe different from {@link #getRealUid} and
     * {@link #getPackageUid}, if an external service has the
     * {@link android.R.styleable#AndroidManifestService_useAppZygote android:useAppZygote} set
     * to <code>true</code> and was bound with the flag
     * {@link android.content.Context#BIND_EXTERNAL_SERVICE} - in this case, this field here will
     * be the kernel user identifier of the external service provider.
     */
    public int getDefiningUid() {
        return mDefiningUid;
    }

    /**
     * The actual process name it was running with.
     */
    public @NonNull String getProcessName() {
        return mProcessName;
    }

    /**
     * The reason code of the process's death.
     */
    public @Reason int getReason() {
        return mReason;
    }

    /**
     * The exit status argument of exit() if the application calls it, or the signal
     * number if the application is signaled.
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * The importance of the process that it used to have before the death.
     */
    public @Importance int getImportance() {
        return mImportance;
    }

    /**
     * Last proportional set size of the memory that the process had used in kB.
     *
     * <p class="note">Note: This is the value from last sampling on the process,
     * it's NOT the exact memory information prior to its death; and it'll be zero
     * if the process died before system had a chance to take the sample. </p>
     */
    public long getPss() {
        return mPss;
    }

    /**
     * Last resident set size of the memory that the process had used in kB.
     *
     * <p class="note">Note: This is the value from last sampling on the process,
     * it's NOT the exact memory information prior to its death; and it'll be zero
     * if the process died before system had a chance to take the sample. </p>
     */
    public long getRss() {
        return mRss;
    }

    /**
     * The timestamp of the process's death, in milliseconds since the epoch,
     * as returned by {@link java.lang.System#currentTimeMillis() System.currentTimeMillis()}.
     */
    public @CurrentTimeMillisLong long getTimestamp() {
        return mTimestamp;
    }

    /**
     * The human readable description of the process's death, given by the system; could be null.
     *
     * <p class="note">Note: only intended to be human-readable and the system provides no
     * guarantees that the format is stable across devices or Android releases.</p>
     */
    public @Nullable String getDescription() {
        final StringBuilder sb = new StringBuilder();

        if (mSubReason != SUBREASON_UNKNOWN) {
            sb.append("[");
            sb.append(subreasonToString(mSubReason));
            sb.append("]");
        }

        if (!TextUtils.isEmpty(mDescription)) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(mDescription);
        }

        return sb.toString();
    }

    /**
     * Return the user id of the record on a multi-user system.
     */
    public @NonNull UserHandle getUserHandle() {
        return UserHandle.of(UserHandle.getUserId(mRealUid));
    }

    /**
     * Return the state data set by calling
     * {@link android.app.ActivityManager#setProcessStateSummary(byte[])
     * ActivityManager.setProcessStateSummary(byte[])} from the process before its death.
     *
     * @return The process-customized data
     * @see ActivityManager#setProcessStateSummary(byte[])
     */
    public @Nullable byte[] getProcessStateSummary() {
        return mState;
    }

    /**
     * Return the InputStream to the traces that was taken by the system
     * prior to the death of the process; typically it'll be available when
     * the reason is {@link #REASON_ANR}, though if the process gets an ANR
     * but recovers, and dies for another reason later, this trace will be included
     * in the record of {@link ApplicationExitInfo} still. Beginning with API 31,
     * tombstone traces will be returned for
     * {@link #REASON_CRASH_NATIVE}, with an InputStream containing a protobuf with
     * <a href="https://android.googlesource.com/platform/system/core/+/refs/heads/master/debuggerd/proto/tombstone.proto">this schema</a>.
     * Note that because these traces are kept in a separate global circular buffer, crashes may be
     * overwritten by newer crashes (including from other applications), so this may still return
     * null.
     *
     * @return The input stream to the traces that was taken by the system
     *         prior to the death of the process.
     */
    public @Nullable InputStream getTraceInputStream() throws IOException {
        if (mAppTraceRetriever == null && mNativeTombstoneRetriever == null) {
            return null;
        }

        try {
            if (mNativeTombstoneRetriever != null) {
                final ParcelFileDescriptor pfd = mNativeTombstoneRetriever.getPfd();
                if (pfd == null) {
                    return null;
                }

                return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            } else {
                final ParcelFileDescriptor fd = mAppTraceRetriever.getTraceFileDescriptor(
                        mPackageName, mPackageUid, mPid);
                if (fd == null) {
                    return null;
                }
                return new GZIPInputStream(new ParcelFileDescriptor.AutoCloseInputStream(fd));
            }
        } catch (RemoteException e) {
            return null;
        }
    }

    /**
     * Similar to {@link #getTraceInputStream} but return the File object.
     *
     * For internal use only.
     *
     * @hide
     */
    public @Nullable File getTraceFile() {
        return mTraceFile;
    }

    /**
     * A subtype reason in conjunction with {@link #mReason}.
     *
     * For internal use only.
     *
     * @hide
     */
    public @SubReason int getSubReason() {
        return mSubReason;
    }

    /**
     * The connection group this process belongs to, if there is any.
     * @see android.content.Context#updateServiceGroup
     *
     * For internal use only.
     *
     * @hide
     */
    public int getConnectionGroup() {
        return mConnectionGroup;
    }

    /**
     * Name of first package running in this process;
     *
     * @hide
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * List of packages running in this process;
     *
     * For system internal use only, will not retain across processes.
     *
     * @hide
     */
    public String[] getPackageList() {
        return mPackageList;
    }

    /**
     * @see #getPid
     *
     * @hide
     */
    public void setPid(final int pid) {
        mPid = pid;
    }

    /**
     * @see #getRealUid
     *
     * @hide
     */
    public void setRealUid(final int uid) {
        mRealUid = uid;
    }

    /**
     * @see #getPackageUid
     *
     * @hide
     */
    public void setPackageUid(final int uid) {
        mPackageUid = uid;
    }

    /**
     * @see #getDefiningUid
     *
     * @hide
     */
    public void setDefiningUid(final int uid) {
        mDefiningUid = uid;
    }

    /**
     * @see #getProcessName
     *
     * @hide
     */
    public void setProcessName(final String processName) {
        mProcessName = intern(processName);
    }

    /**
     * @see #getReason
     *
     * @hide
     */
    public void setReason(final @Reason int reason) {
        mReason = reason;
    }

    /**
     * @see #getStatus
     *
     * @hide
     */
    public void setStatus(final int status) {
        mStatus = status;
    }

    /**
     * @see #getImportance
     *
     * @hide
     */
    public void setImportance(final @Importance int importance) {
        mImportance = importance;
    }

    /**
     * @see #getPss
     *
     * @hide
     */
    public void setPss(final long pss) {
        mPss = pss;
    }

    /**
     * @see #getRss
     *
     * @hide
     */
    public void setRss(final long rss) {
        mRss = rss;
    }

    /**
     * @see #getTimestamp
     *
     * @hide
     */
    public void setTimestamp(final @CurrentTimeMillisLong long timestamp) {
        mTimestamp = timestamp;
    }

    /**
     * @see #getDescription
     *
     * @hide
     */
    public void setDescription(final String description) {
        mDescription = intern(description);
    }

    /**
     * @see #getSubReason
     *
     * @hide
     */
    public void setSubReason(final @SubReason int subReason) {
        mSubReason = subReason;
    }

    /**
     * @see #getConnectionGroup
     *
     * @hide
     */
    public void setConnectionGroup(final int connectionGroup) {
        mConnectionGroup = connectionGroup;
    }

    /**
     * @see #getPackageName
     *
     * @hide
     */
    public void setPackageName(final String packageName) {
        mPackageName = intern(packageName);
    }

    /**
     * @see #getPackageList
     *
     * @hide
     */
    public void setPackageList(final String[] packageList) {
        mPackageList = packageList;
    }

    /**
     * @see #getProcessStateSummary
     *
     * @hide
     */
    public void setProcessStateSummary(final byte[] state) {
        mState = state;
    }

    /**
     * @see #getTraceFile
     *
     * @hide
     */
    public void setTraceFile(final File traceFile) {
        mTraceFile = traceFile;
    }

    /**
     * @see #mAppTraceRetriever
     *
     * @hide
     */
    public void setAppTraceRetriever(final IAppTraceRetriever retriever) {
        mAppTraceRetriever = retriever;
    }

    /**
     * @see mNativeTombstoneRetriever
     *
     * @hide
     */
    public void setNativeTombstoneRetriever(final IParcelFileDescriptorRetriever retriever) {
        mNativeTombstoneRetriever = retriever;
    }

    /**
     * @see #mLoggedInStatsd
     *
     * @hide
     */
    public boolean isLoggedInStatsd() {
        return mLoggedInStatsd;
    }

    /**
     * @see #mLoggedInStatsd
     *
     * @hide
     */
    public void setLoggedInStatsd(boolean loggedInStatsd) {
        mLoggedInStatsd = loggedInStatsd;
    }

    /**
     * @see #mHasForegroundServices
     *
     * @hide
     */
    public boolean hasForegroundServices() {
        return mHasForegroundServices;
    }

    /**
     * @see #mHasForegroundServices
     *
     * @hide
     */
    public void setHasForegroundServices(boolean hasForegroundServices) {
        mHasForegroundServices = hasForegroundServices;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPid);
        dest.writeInt(mRealUid);
        dest.writeInt(mPackageUid);
        dest.writeInt(mDefiningUid);
        dest.writeString(mProcessName);
        dest.writeString(mPackageName);
        dest.writeInt(mConnectionGroup);
        dest.writeInt(mReason);
        dest.writeInt(mSubReason);
        dest.writeInt(mStatus);
        dest.writeInt(mImportance);
        dest.writeLong(mPss);
        dest.writeLong(mRss);
        dest.writeLong(mTimestamp);
        dest.writeString(mDescription);
        dest.writeByteArray(mState);
        if (mAppTraceRetriever != null) {
            dest.writeInt(1);
            dest.writeStrongBinder(mAppTraceRetriever.asBinder());
        } else {
            dest.writeInt(0);
        }
        if (mNativeTombstoneRetriever != null) {
            dest.writeInt(1);
            dest.writeStrongBinder(mNativeTombstoneRetriever.asBinder());
        } else {
            dest.writeInt(0);
        }
    }

    /** @hide */
    public ApplicationExitInfo() {
    }

    /** @hide */
    public ApplicationExitInfo(ApplicationExitInfo other) {
        mPid = other.mPid;
        mRealUid = other.mRealUid;
        mPackageUid = other.mPackageUid;
        mDefiningUid = other.mDefiningUid;
        mProcessName = other.mProcessName;
        mPackageName = other.mPackageName;
        mConnectionGroup = other.mConnectionGroup;
        mReason = other.mReason;
        mStatus = other.mStatus;
        mSubReason = other.mSubReason;
        mImportance = other.mImportance;
        mPss = other.mPss;
        mRss = other.mRss;
        mTimestamp = other.mTimestamp;
        mDescription = other.mDescription;
        mPackageName = other.mPackageName;
        mPackageList = other.mPackageList;
        mState = other.mState;
        mTraceFile = other.mTraceFile;
        mAppTraceRetriever = other.mAppTraceRetriever;
        mNativeTombstoneRetriever = other.mNativeTombstoneRetriever;
        mLoggedInStatsd = other.mLoggedInStatsd;
        mHasForegroundServices = other.mHasForegroundServices;
    }

    private ApplicationExitInfo(@NonNull Parcel in) {
        mPid = in.readInt();
        mRealUid = in.readInt();
        mPackageUid = in.readInt();
        mDefiningUid = in.readInt();
        mProcessName = intern(in.readString());
        mPackageName = intern(in.readString());
        mConnectionGroup = in.readInt();
        mReason = in.readInt();
        mSubReason = in.readInt();
        mStatus = in.readInt();
        mImportance = in.readInt();
        mPss = in.readLong();
        mRss = in.readLong();
        mTimestamp = in.readLong();
        mDescription = intern(in.readString());
        mState = in.createByteArray();
        if (in.readInt() == 1) {
            mAppTraceRetriever = IAppTraceRetriever.Stub.asInterface(in.readStrongBinder());
        }
        if (in.readInt() == 1) {
            mNativeTombstoneRetriever = IParcelFileDescriptorRetriever.Stub.asInterface(
                    in.readStrongBinder());
        }
    }

    private static String intern(@Nullable String source) {
        return source != null ? source.intern() : null;
    }

    public @NonNull static final Creator<ApplicationExitInfo> CREATOR =
            new Creator<ApplicationExitInfo>() {
        @Override
        public ApplicationExitInfo createFromParcel(Parcel in) {
            return new ApplicationExitInfo(in);
        }

        @Override
        public ApplicationExitInfo[] newArray(int size) {
            return new ApplicationExitInfo[size];
        }
    };

    /** @hide */
    public void dump(@NonNull PrintWriter pw, @Nullable String prefix, @Nullable String seqSuffix,
            @NonNull SimpleDateFormat sdf) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix)
                .append("ApplicationExitInfo ").append(seqSuffix).append(':')
                .append('\n');
        sb.append(prefix).append(' ')
                .append(" timestamp=").append(sdf.format(new Date(mTimestamp)))
                .append(" pid=").append(mPid)
                .append(" realUid=").append(mRealUid)
                .append(" packageUid=").append(mPackageUid)
                .append(" definingUid=").append(mDefiningUid)
                .append(" user=").append(UserHandle.getUserId(mPackageUid))
                .append('\n');
        sb.append(prefix).append(' ')
                .append(" process=").append(mProcessName)
                .append(" reason=").append(mReason)
                .append(" (").append(reasonCodeToString(mReason)).append(")")
                .append(" subreason=").append(mSubReason)
                .append(" (").append(subreasonToString(mSubReason)).append(")")
                .append(" status=").append(mStatus)
                .append('\n');
        sb.append(prefix).append(' ')
                .append(" importance=").append(mImportance)
                .append(" pss=");
        DebugUtils.sizeValueToString(mPss << 10, sb);
        sb.append(" rss=");
        DebugUtils.sizeValueToString(mRss << 10, sb);
        sb.append(" description=").append(mDescription)
                .append(" state=").append((ArrayUtils.isEmpty(mState)
                            ? "empty" : Integer.toString(mState.length) + " bytes"))
                .append(" trace=").append(mTraceFile)
                .append('\n');
        pw.print(sb.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ApplicationExitInfo(timestamp=");
        sb.append(new SimpleDateFormat().format(new Date(mTimestamp)));
        sb.append(" pid=").append(mPid);
        sb.append(" realUid=").append(mRealUid);
        sb.append(" packageUid=").append(mPackageUid);
        sb.append(" definingUid=").append(mDefiningUid);
        sb.append(" user=").append(UserHandle.getUserId(mPackageUid));
        sb.append(" process=").append(mProcessName);
        sb.append(" reason=").append(mReason).append(" (")
                .append(reasonCodeToString(mReason)).append(")");
        sb.append(" subreason=").append(mSubReason).append(" (")
                .append(subreasonToString(mSubReason)).append(")");
        sb.append(" status=").append(mStatus);
        sb.append(" importance=").append(mImportance);
        sb.append(" pss="); DebugUtils.sizeValueToString(mPss << 10, sb);
        sb.append(" rss="); DebugUtils.sizeValueToString(mRss << 10, sb);
        sb.append(" description=").append(mDescription);
        sb.append(" state=").append(ArrayUtils.isEmpty(mState)
                ? "empty" : Integer.toString(mState.length) + " bytes");
        sb.append(" trace=").append(mTraceFile);

        return sb.toString();
    }

    /** @hide */
    public static String reasonCodeToString(@Reason int reason) {
        switch (reason) {
            case REASON_EXIT_SELF:
                return "EXIT_SELF";
            case REASON_SIGNALED:
                return "SIGNALED";
            case REASON_LOW_MEMORY:
                return "LOW_MEMORY";
            case REASON_CRASH:
                return "APP CRASH(EXCEPTION)";
            case REASON_CRASH_NATIVE:
                return "APP CRASH(NATIVE)";
            case REASON_ANR:
                return "ANR";
            case REASON_INITIALIZATION_FAILURE:
                return "INITIALIZATION FAILURE";
            case REASON_PERMISSION_CHANGE:
                return "PERMISSION CHANGE";
            case REASON_EXCESSIVE_RESOURCE_USAGE:
                return "EXCESSIVE RESOURCE USAGE";
            case REASON_USER_REQUESTED:
                return "USER REQUESTED";
            case REASON_USER_STOPPED:
                return "USER STOPPED";
            case REASON_DEPENDENCY_DIED:
                return "DEPENDENCY DIED";
            case REASON_OTHER:
                return "OTHER KILLS BY SYSTEM";
            case REASON_FREEZER:
                return "FREEZER";
            case REASON_PACKAGE_STATE_CHANGE:
                return "STATE CHANGE";
            case REASON_PACKAGE_UPDATED:
                return "PACKAGE UPDATED";
            default:
                return "UNKNOWN";
        }
    }

    /** @hide */
    public static String subreasonToString(@SubReason int subreason) {
        switch (subreason) {
            case SUBREASON_WAIT_FOR_DEBUGGER:
                return "WAIT FOR DEBUGGER";
            case SUBREASON_TOO_MANY_CACHED:
                return "TOO MANY CACHED PROCS";
            case SUBREASON_TOO_MANY_EMPTY:
                return "TOO MANY EMPTY PROCS";
            case SUBREASON_TRIM_EMPTY:
                return "TRIM EMPTY";
            case SUBREASON_LARGE_CACHED:
                return "LARGE CACHED";
            case SUBREASON_MEMORY_PRESSURE:
                return "MEMORY PRESSURE";
            case SUBREASON_EXCESSIVE_CPU:
                return "EXCESSIVE CPU USAGE";
            case SUBREASON_SYSTEM_UPDATE_DONE:
                return "SYSTEM UPDATE_DONE";
            case SUBREASON_KILL_ALL_FG:
                return "KILL ALL FG";
            case SUBREASON_KILL_ALL_BG_EXCEPT:
                return "KILL ALL BG EXCEPT";
            case SUBREASON_KILL_UID:
                return "KILL UID";
            case SUBREASON_KILL_PID:
                return "KILL PID";
            case SUBREASON_INVALID_START:
                return "INVALID START";
            case SUBREASON_INVALID_STATE:
                return "INVALID STATE";
            case SUBREASON_IMPERCEPTIBLE:
                return "IMPERCEPTIBLE";
            case SUBREASON_REMOVE_LRU:
                return "REMOVE LRU";
            case SUBREASON_ISOLATED_NOT_NEEDED:
                return "ISOLATED NOT NEEDED";
            case SUBREASON_FREEZER_BINDER_IOCTL:
                return "FREEZER BINDER IOCTL";
            case SUBREASON_FREEZER_BINDER_TRANSACTION:
                return "FREEZER BINDER TRANSACTION";
            case SUBREASON_FORCE_STOP:
                return "FORCE STOP";
            case SUBREASON_REMOVE_TASK:
                return "REMOVE TASK";
            case SUBREASON_STOP_APP:
                return "STOP APP";
            case SUBREASON_KILL_BACKGROUND:
                return "KILL BACKGROUND";
            case SUBREASON_PACKAGE_UPDATE:
                return "PACKAGE UPDATE";
            case SUBREASON_UNDELIVERED_BROADCAST:
                return "UNDELIVERED BROADCAST";
            case SUBREASON_EXCESSIVE_BINDER_OBJECTS:
                return "EXCESSIVE BINDER OBJECTS";
            case SUBREASON_OOM_KILL:
                return "OOM KILL";
            case SUBREASON_FREEZER_BINDER_ASYNC_FULL:
                return "FREEZER BINDER ASYNC FULL";
            case SUBREASON_EXCESSIVE_OUTGOING_BROADCASTS_WHILE_CACHED:
                return "EXCESSIVE_OUTGOING_BROADCASTS_WHILE_CACHED";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Write to a protocol buffer output stream.
     * Protocol buffer message definition at {@link android.app.ApplicationExitInfoProto}
     *
     * @param proto    Stream to write the ApplicationExitInfo object to.
     * @param fieldId  Field Id of the ApplicationExitInfo as defined in the parent message
     * @hide
     */
    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        final long token = proto.start(fieldId);
        proto.write(ApplicationExitInfoProto.PID, mPid);
        proto.write(ApplicationExitInfoProto.REAL_UID, mRealUid);
        proto.write(ApplicationExitInfoProto.PACKAGE_UID, mPackageUid);
        proto.write(ApplicationExitInfoProto.DEFINING_UID, mDefiningUid);
        proto.write(ApplicationExitInfoProto.PROCESS_NAME, mProcessName);
        proto.write(ApplicationExitInfoProto.CONNECTION_GROUP, mConnectionGroup);
        proto.write(ApplicationExitInfoProto.REASON, mReason);
        proto.write(ApplicationExitInfoProto.SUB_REASON, mSubReason);
        proto.write(ApplicationExitInfoProto.STATUS, mStatus);
        proto.write(ApplicationExitInfoProto.IMPORTANCE, mImportance);
        proto.write(ApplicationExitInfoProto.PSS, mPss);
        proto.write(ApplicationExitInfoProto.RSS, mRss);
        proto.write(ApplicationExitInfoProto.TIMESTAMP, mTimestamp);
        proto.write(ApplicationExitInfoProto.DESCRIPTION, mDescription);
        proto.write(ApplicationExitInfoProto.STATE, mState);
        proto.write(ApplicationExitInfoProto.TRACE_FILE,
                mTraceFile == null ? null : mTraceFile.getAbsolutePath());
        proto.end(token);
    }

    /**
     * Read from a protocol buffer input stream.
     * Protocol buffer message definition at {@link android.app.ApplicationExitInfoProto}
     *
     * @param proto   Stream to read the ApplicationExitInfo object from.
     * @param fieldId Field Id of the ApplicationExitInfo as defined in the parent message
     * @hide
     */
    public void readFromProto(ProtoInputStream proto, long fieldId)
            throws IOException, WireTypeMismatchException {
        final long token = proto.start(fieldId);
        while (proto.nextField() != ProtoInputStream.NO_MORE_FIELDS) {
            switch (proto.getFieldNumber()) {
                case (int) ApplicationExitInfoProto.PID:
                    mPid = proto.readInt(ApplicationExitInfoProto.PID);
                    break;
                case (int) ApplicationExitInfoProto.REAL_UID:
                    mRealUid = proto.readInt(ApplicationExitInfoProto.REAL_UID);
                    break;
                case (int) ApplicationExitInfoProto.PACKAGE_UID:
                    mPackageUid = proto.readInt(ApplicationExitInfoProto.PACKAGE_UID);
                    break;
                case (int) ApplicationExitInfoProto.DEFINING_UID:
                    mDefiningUid = proto.readInt(ApplicationExitInfoProto.DEFINING_UID);
                    break;
                case (int) ApplicationExitInfoProto.PROCESS_NAME:
                    mProcessName = intern(proto.readString(ApplicationExitInfoProto.PROCESS_NAME));
                    break;
                case (int) ApplicationExitInfoProto.CONNECTION_GROUP:
                    mConnectionGroup = proto.readInt(ApplicationExitInfoProto.CONNECTION_GROUP);
                    break;
                case (int) ApplicationExitInfoProto.REASON:
                    mReason = proto.readInt(ApplicationExitInfoProto.REASON);
                    break;
                case (int) ApplicationExitInfoProto.SUB_REASON:
                    mSubReason = proto.readInt(ApplicationExitInfoProto.SUB_REASON);
                    break;
                case (int) ApplicationExitInfoProto.STATUS:
                    mStatus = proto.readInt(ApplicationExitInfoProto.STATUS);
                    break;
                case (int) ApplicationExitInfoProto.IMPORTANCE:
                    mImportance = proto.readInt(ApplicationExitInfoProto.IMPORTANCE);
                    break;
                case (int) ApplicationExitInfoProto.PSS:
                    mPss = proto.readLong(ApplicationExitInfoProto.PSS);
                    break;
                case (int) ApplicationExitInfoProto.RSS:
                    mRss = proto.readLong(ApplicationExitInfoProto.RSS);
                    break;
                case (int) ApplicationExitInfoProto.TIMESTAMP:
                    mTimestamp = proto.readLong(ApplicationExitInfoProto.TIMESTAMP);
                    break;
                case (int) ApplicationExitInfoProto.DESCRIPTION:
                    mDescription = intern(proto.readString(ApplicationExitInfoProto.DESCRIPTION));
                    break;
                case (int) ApplicationExitInfoProto.STATE:
                    mState = proto.readBytes(ApplicationExitInfoProto.STATE);
                    break;
                case (int) ApplicationExitInfoProto.TRACE_FILE:
                    final String path = proto.readString(ApplicationExitInfoProto.TRACE_FILE);
                    if (!TextUtils.isEmpty(path)) {
                        mTraceFile = new File(path);
                    }
                    break;
            }
        }
        proto.end(token);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null || !(other instanceof ApplicationExitInfo)) {
            return false;
        }
        ApplicationExitInfo o = (ApplicationExitInfo) other;
        return mPid == o.mPid && mRealUid == o.mRealUid && mPackageUid == o.mPackageUid
                && mDefiningUid == o.mDefiningUid
                && mConnectionGroup == o.mConnectionGroup && mReason == o.mReason
                && mSubReason == o.mSubReason && mImportance == o.mImportance
                && mStatus == o.mStatus && mTimestamp == o.mTimestamp
                && mPss == o.mPss && mRss == o.mRss
                && TextUtils.equals(mProcessName, o.mProcessName)
                && TextUtils.equals(mDescription, o.mDescription);
    }

    @Override
    public int hashCode() {
        int result = mPid;
        result = 31 * result + mRealUid;
        result = 31 * result + mPackageUid;
        result = 31 * result + mDefiningUid;
        result = 31 * result + mConnectionGroup;
        result = 31 * result + mReason;
        result = 31 * result + mSubReason;
        result = 31 * result + mImportance;
        result = 31 * result + mStatus;
        result = 31 * result + (int) mPss;
        result = 31 * result + (int) mRss;
        result = 31 * result + Long.hashCode(mTimestamp);
        result = 31 * result + Objects.hashCode(mProcessName);
        result = 31 * result + Objects.hashCode(mDescription);
        return result;
    }
}
