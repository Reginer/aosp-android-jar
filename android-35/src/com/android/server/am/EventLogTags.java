/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/services/core/java/com/android/server/am/EventLogTags.logtags
 */

package com.android.server.am;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 2719 configuration_changed (config mask|1|5) */
  public static final int CONFIGURATION_CHANGED = 2719;

  /** 2721 cpu (total|1|6),(user|1|6),(system|1|6),(iowait|1|6),(irq|1|6),(softirq|1|6) */
  public static final int CPU = 2721;

  /** 3040 boot_progress_ams_ready (time|2|3) */
  public static final int BOOT_PROGRESS_AMS_READY = 3040;

  /** 3050 boot_progress_enable_screen (time|2|3) */
  public static final int BOOT_PROGRESS_ENABLE_SCREEN = 3050;

  /** 30008 am_anr (User|1|5),(pid|1|5),(Package Name|3),(Flags|1|5),(reason|3) */
  public static final int AM_ANR = 30008;

  /** 30010 am_proc_bound (User|1|5),(PID|1|5),(Process Name|3) */
  public static final int AM_PROC_BOUND = 30010;

  /** 30011 am_proc_died (User|1|5),(PID|1|5),(Process Name|3),(OomAdj|1|5),(ProcState|1|5) */
  public static final int AM_PROC_DIED = 30011;

  /** 30014 am_proc_start (User|1|5),(PID|1|5),(UID|1|5),(Process Name|3),(Type|3),(Component|3) */
  public static final int AM_PROC_START = 30014;

  /** 30015 am_proc_bad (User|1|5),(UID|1|5),(Process Name|3) */
  public static final int AM_PROC_BAD = 30015;

  /** 30016 am_proc_good (User|1|5),(UID|1|5),(Process Name|3) */
  public static final int AM_PROC_GOOD = 30016;

  /** 30017 am_low_memory (Num Processes|1|1) */
  public static final int AM_LOW_MEMORY = 30017;

  /** 30023 am_kill (User|1|5),(PID|1|5),(Process Name|3),(OomAdj|1|5),(Reason|3),(Rss|2|2) */
  public static final int AM_KILL = 30023;

  /** 30024 am_broadcast_discard_filter (User|1|5),(Broadcast|1|5),(Action|3),(Receiver Number|1|1),(BroadcastFilter|1|5) */
  public static final int AM_BROADCAST_DISCARD_FILTER = 30024;

  /** 30025 am_broadcast_discard_app (User|1|5),(Broadcast|1|5),(Action|3),(Receiver Number|1|1),(App|3) */
  public static final int AM_BROADCAST_DISCARD_APP = 30025;

  /** 30030 am_create_service (User|1|5),(Service Record|1|5),(Name|3),(UID|1|5),(PID|1|5) */
  public static final int AM_CREATE_SERVICE = 30030;

  /** 30031 am_destroy_service (User|1|5),(Service Record|1|5),(PID|1|5) */
  public static final int AM_DESTROY_SERVICE = 30031;

  /** 30032 am_process_crashed_too_much (User|1|5),(Name|3),(PID|1|5) */
  public static final int AM_PROCESS_CRASHED_TOO_MUCH = 30032;

  /** 30033 am_drop_process (PID|1|5) */
  public static final int AM_DROP_PROCESS = 30033;

  /** 30034 am_service_crashed_too_much (User|1|5),(Crash Count|1|1),(Component Name|3),(PID|1|5) */
  public static final int AM_SERVICE_CRASHED_TOO_MUCH = 30034;

  /** 30035 am_schedule_service_restart (User|1|5),(Component Name|3),(Time|2|3) */
  public static final int AM_SCHEDULE_SERVICE_RESTART = 30035;

  /** 30036 am_provider_lost_process (User|1|5),(Package Name|3),(UID|1|5),(Name|3) */
  public static final int AM_PROVIDER_LOST_PROCESS = 30036;

  /** 30037 am_process_start_timeout (User|1|5),(PID|1|5),(UID|1|5),(Process Name|3) */
  public static final int AM_PROCESS_START_TIMEOUT = 30037;

  /** 30039 am_crash (User|1|5),(PID|1|5),(Process Name|3),(Flags|1|5),(Exception|3),(Message|3),(File|3),(Line|1|5),(Recoverable|1|5) */
  public static final int AM_CRASH = 30039;

  /** 30040 am_wtf (User|1|5),(PID|1|5),(Process Name|3),(Flags|1|5),(Tag|3),(Message|3) */
  public static final int AM_WTF = 30040;

  /** 30041 am_switch_user (id|1|5) */
  public static final int AM_SWITCH_USER = 30041;

  /** 30045 am_pre_boot (User|1|5),(Package|3) */
  public static final int AM_PRE_BOOT = 30045;

  /** 30046 am_meminfo (Cached|2|2),(Free|2|2),(Zram|2|2),(Kernel|2|2),(Native|2|2) */
  public static final int AM_MEMINFO = 30046;

  /** 30047 am_pss (Pid|1|5),(UID|1|5),(Process Name|3),(Pss|2|2),(Uss|2|2),(SwapPss|2|2),(Rss|2|2),(StatType|1|5),(ProcState|1|5),(TimeToCollect|2|2) */
  public static final int AM_PSS = 30047;

  /** 30050 am_mem_factor (Current|1|5),(Previous|1|5) */
  public static final int AM_MEM_FACTOR = 30050;

  /** 30051 am_user_state_changed (id|1|5),(state|1|5) */
  public static final int AM_USER_STATE_CHANGED = 30051;

  /** 30052 am_uid_running (UID|1|5) */
  public static final int AM_UID_RUNNING = 30052;

  /** 30053 am_uid_stopped (UID|1|5) */
  public static final int AM_UID_STOPPED = 30053;

  /** 30054 am_uid_active (UID|1|5) */
  public static final int AM_UID_ACTIVE = 30054;

  /** 30055 am_uid_idle (UID|1|5) */
  public static final int AM_UID_IDLE = 30055;

  /** 30056 am_stop_idle_service (UID|1|5),(Component Name|3) */
  public static final int AM_STOP_IDLE_SERVICE = 30056;

  /** 30063 am_compact (Pid|1|5),(Process Name|3),(Action|3),(BeforeRssTotal|2|2),(BeforeRssFile|2|2),(BeforeRssAnon|2|2),(BeforeRssSwap|2|2),(DeltaRssTotal|2|2),(DeltaRssFile|2|2),(DeltaRssAnon|2|2),(DeltaRssSwap|2|2),(Time|2|3),(LastAction|1|2),(LastActionTimestamp|2|3),(setAdj|1|2),(procState|1|2),(BeforeZRAMFree|2|2),(DeltaZRAMFree|2|2) */
  public static final int AM_COMPACT = 30063;

  /** 30068 am_freeze (Pid|1|5),(Process Name|3) */
  public static final int AM_FREEZE = 30068;

  /** 30069 am_unfreeze (Pid|1|5),(Process Name|3) */
  public static final int AM_UNFREEZE = 30069;

  /** 30070 uc_finish_user_unlocking (userId|1|5) */
  public static final int UC_FINISH_USER_UNLOCKING = 30070;

  /** 30071 uc_finish_user_unlocked (userId|1|5) */
  public static final int UC_FINISH_USER_UNLOCKED = 30071;

  /** 30072 uc_finish_user_unlocked_completed (userId|1|5) */
  public static final int UC_FINISH_USER_UNLOCKED_COMPLETED = 30072;

  /** 30073 uc_finish_user_stopping (userId|1|5) */
  public static final int UC_FINISH_USER_STOPPING = 30073;

  /** 30074 uc_finish_user_stopped (userId|1|5) */
  public static final int UC_FINISH_USER_STOPPED = 30074;

  /** 30075 uc_switch_user (userId|1|5) */
  public static final int UC_SWITCH_USER = 30075;

  /** 30076 uc_start_user_internal (userId|1|5),(foreground|1),(displayId|1|5) */
  public static final int UC_START_USER_INTERNAL = 30076;

  /** 30077 uc_unlock_user (userId|1|5) */
  public static final int UC_UNLOCK_USER = 30077;

  /** 30078 uc_finish_user_boot (userId|1|5) */
  public static final int UC_FINISH_USER_BOOT = 30078;

  /** 30079 uc_dispatch_user_switch (oldUserId|1|5),(newUserId|1|5) */
  public static final int UC_DISPATCH_USER_SWITCH = 30079;

  /** 30080 uc_continue_user_switch (oldUserId|1|5),(newUserId|1|5) */
  public static final int UC_CONTINUE_USER_SWITCH = 30080;

  /** 30081 uc_send_user_broadcast (userId|1|5),(IntentAction|3) */
  public static final int UC_SEND_USER_BROADCAST = 30081;

  /** 30082 ssm_user_starting (userId|1|5) */
  public static final int SSM_USER_STARTING = 30082;

  /** 30083 ssm_user_switching (oldUserId|1|5),(newUserId|1|5) */
  public static final int SSM_USER_SWITCHING = 30083;

  /** 30084 ssm_user_unlocking (userId|1|5) */
  public static final int SSM_USER_UNLOCKING = 30084;

  /** 30085 ssm_user_unlocked (userId|1|5) */
  public static final int SSM_USER_UNLOCKED = 30085;

  /** 30086 ssm_user_stopping (userId|1|5) */
  public static final int SSM_USER_STOPPING = 30086;

  /** 30087 ssm_user_stopped (userId|1|5) */
  public static final int SSM_USER_STOPPED = 30087;

  /** 30088 ssm_user_completed_event (userId|1|5),(eventFlag|1|5) */
  public static final int SSM_USER_COMPLETED_EVENT = 30088;

  /** 30091 um_user_visibility_changed (userId|1|5),(visible|1) */
  public static final int UM_USER_VISIBILITY_CHANGED = 30091;

  /** 30100 am_foreground_service_start (User|1|5),(Component Name|3),(allowWhileInUse|1),(startReasonCode|3),(targetSdk|1|1),(callerTargetSdk|1|1),(notificationWasDeferred|1),(notificationShown|1),(durationMs|1|3),(startForegroundCount|1|1),(stopReason|3),(fgsType|1) */
  public static final int AM_FOREGROUND_SERVICE_START = 30100;

  /** 30101 am_foreground_service_denied (User|1|5),(Component Name|3),(allowWhileInUse|1),(startReasonCode|3),(targetSdk|1|1),(callerTargetSdk|1|1),(notificationWasDeferred|1),(notificationShown|1),(durationMs|1|3),(startForegroundCount|1|1),(stopReason|3),(fgsType|1) */
  public static final int AM_FOREGROUND_SERVICE_DENIED = 30101;

  /** 30102 am_foreground_service_stop (User|1|5),(Component Name|3),(allowWhileInUse|1),(startReasonCode|3),(targetSdk|1|1),(callerTargetSdk|1|1),(notificationWasDeferred|1),(notificationShown|1),(durationMs|1|3),(startForegroundCount|1|1),(stopReason|3),(fgsType|1) */
  public static final int AM_FOREGROUND_SERVICE_STOP = 30102;

  /** 30103 am_foreground_service_timed_out (User|1|5),(Component Name|3),(allowWhileInUse|1),(startReasonCode|3),(targetSdk|1|1),(callerTargetSdk|1|1),(notificationWasDeferred|1),(notificationShown|1),(durationMs|1|3),(startForegroundCount|1|1),(stopReason|3),(fgsType|1) */
  public static final int AM_FOREGROUND_SERVICE_TIMED_OUT = 30103;

  /** 30104 am_cpu (Pid|2|5),(UID|2|5),(Base Name|3),(Uptime|2|3),(Stime|2|3),(Utime|2|3) */
  public static final int AM_CPU = 30104;

  /** 30110 am_intent_sender_redirect_user (userId|1|5) */
  public static final int AM_INTENT_SENDER_REDIRECT_USER = 30110;

  /** 30120 am_clear_app_data_caller (pid|1),(uid|1),(package|3) */
  public static final int AM_CLEAR_APP_DATA_CALLER = 30120;

  /** 30111 am_uid_state_changed (UID|1|5),(Seq|1|5),(UidState|1|5),(OldUidState|1|5),(Capability|1|5),(OldCapability|1|5),(Flags|1|5),(reason|3) */
  public static final int AM_UID_STATE_CHANGED = 30111;

  /** 30112 am_proc_state_changed (UID|1|5),(PID|1|5),(Seq|1|5),(ProcState|1|5),(OldProcState|1|5),(OomAdj|1|5),(OldOomAdj|1|5),(reason|3) */
  public static final int AM_PROC_STATE_CHANGED = 30112;

  /** 30113 am_oom_adj_misc (Event|1|5),(UID|1|5),(PID|1|5),(Seq|1|5),(Arg1|1|5),(Arg2|1|5),(reason|3) */
  public static final int AM_OOM_ADJ_MISC = 30113;

  public static void writeConfigurationChanged(int configMask) {
    android.util.EventLog.writeEvent(CONFIGURATION_CHANGED, configMask);
  }

  public static void writeCpu(int total, int user, int system, int iowait, int irq, int softirq) {
    android.util.EventLog.writeEvent(CPU, total, user, system, iowait, irq, softirq);
  }

  public static void writeBootProgressAmsReady(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_AMS_READY, time);
  }

  public static void writeBootProgressEnableScreen(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_ENABLE_SCREEN, time);
  }

  public static void writeAmAnr(int user, int pid, String packageName, int flags, String reason) {
    android.util.EventLog.writeEvent(AM_ANR, user, pid, packageName, flags, reason);
  }

  public static void writeAmProcBound(int user, int pid, String processName) {
    android.util.EventLog.writeEvent(AM_PROC_BOUND, user, pid, processName);
  }

  public static void writeAmProcDied(int user, int pid, String processName, int oomadj, int procstate) {
    android.util.EventLog.writeEvent(AM_PROC_DIED, user, pid, processName, oomadj, procstate);
  }

  public static void writeAmProcStart(int user, int pid, int uid, String processName, String type, String component) {
    android.util.EventLog.writeEvent(AM_PROC_START, user, pid, uid, processName, type, component);
  }

  public static void writeAmProcBad(int user, int uid, String processName) {
    android.util.EventLog.writeEvent(AM_PROC_BAD, user, uid, processName);
  }

  public static void writeAmProcGood(int user, int uid, String processName) {
    android.util.EventLog.writeEvent(AM_PROC_GOOD, user, uid, processName);
  }

  public static void writeAmLowMemory(int numProcesses) {
    android.util.EventLog.writeEvent(AM_LOW_MEMORY, numProcesses);
  }

  public static void writeAmKill(int user, int pid, String processName, int oomadj, String reason, long rss) {
    android.util.EventLog.writeEvent(AM_KILL, user, pid, processName, oomadj, reason, rss);
  }

  public static void writeAmBroadcastDiscardFilter(int user, int broadcast, String action, int receiverNumber, int broadcastfilter) {
    android.util.EventLog.writeEvent(AM_BROADCAST_DISCARD_FILTER, user, broadcast, action, receiverNumber, broadcastfilter);
  }

  public static void writeAmBroadcastDiscardApp(int user, int broadcast, String action, int receiverNumber, String app) {
    android.util.EventLog.writeEvent(AM_BROADCAST_DISCARD_APP, user, broadcast, action, receiverNumber, app);
  }

  public static void writeAmCreateService(int user, int serviceRecord, String name, int uid, int pid) {
    android.util.EventLog.writeEvent(AM_CREATE_SERVICE, user, serviceRecord, name, uid, pid);
  }

  public static void writeAmDestroyService(int user, int serviceRecord, int pid) {
    android.util.EventLog.writeEvent(AM_DESTROY_SERVICE, user, serviceRecord, pid);
  }

  public static void writeAmProcessCrashedTooMuch(int user, String name, int pid) {
    android.util.EventLog.writeEvent(AM_PROCESS_CRASHED_TOO_MUCH, user, name, pid);
  }

  public static void writeAmDropProcess(int pid) {
    android.util.EventLog.writeEvent(AM_DROP_PROCESS, pid);
  }

  public static void writeAmServiceCrashedTooMuch(int user, int crashCount, String componentName, int pid) {
    android.util.EventLog.writeEvent(AM_SERVICE_CRASHED_TOO_MUCH, user, crashCount, componentName, pid);
  }

  public static void writeAmScheduleServiceRestart(int user, String componentName, long time) {
    android.util.EventLog.writeEvent(AM_SCHEDULE_SERVICE_RESTART, user, componentName, time);
  }

  public static void writeAmProviderLostProcess(int user, String packageName, int uid, String name) {
    android.util.EventLog.writeEvent(AM_PROVIDER_LOST_PROCESS, user, packageName, uid, name);
  }

  public static void writeAmProcessStartTimeout(int user, int pid, int uid, String processName) {
    android.util.EventLog.writeEvent(AM_PROCESS_START_TIMEOUT, user, pid, uid, processName);
  }

  public static void writeAmCrash(int user, int pid, String processName, int flags, String exception, String message, String file, int line, int recoverable) {
    android.util.EventLog.writeEvent(AM_CRASH, user, pid, processName, flags, exception, message, file, line, recoverable);
  }

  public static void writeAmWtf(int user, int pid, String processName, int flags, String tag, String message) {
    android.util.EventLog.writeEvent(AM_WTF, user, pid, processName, flags, tag, message);
  }

  public static void writeAmSwitchUser(int id) {
    android.util.EventLog.writeEvent(AM_SWITCH_USER, id);
  }

  public static void writeAmPreBoot(int user, String package_) {
    android.util.EventLog.writeEvent(AM_PRE_BOOT, user, package_);
  }

  public static void writeAmMeminfo(long cached, long free, long zram, long kernel, long native_) {
    android.util.EventLog.writeEvent(AM_MEMINFO, cached, free, zram, kernel, native_);
  }

  public static void writeAmPss(int pid, int uid, String processName, long pss, long uss, long swappss, long rss, int stattype, int procstate, long timetocollect) {
    android.util.EventLog.writeEvent(AM_PSS, pid, uid, processName, pss, uss, swappss, rss, stattype, procstate, timetocollect);
  }

  public static void writeAmMemFactor(int current, int previous) {
    android.util.EventLog.writeEvent(AM_MEM_FACTOR, current, previous);
  }

  public static void writeAmUserStateChanged(int id, int state) {
    android.util.EventLog.writeEvent(AM_USER_STATE_CHANGED, id, state);
  }

  public static void writeAmUidRunning(int uid) {
    android.util.EventLog.writeEvent(AM_UID_RUNNING, uid);
  }

  public static void writeAmUidStopped(int uid) {
    android.util.EventLog.writeEvent(AM_UID_STOPPED, uid);
  }

  public static void writeAmUidActive(int uid) {
    android.util.EventLog.writeEvent(AM_UID_ACTIVE, uid);
  }

  public static void writeAmUidIdle(int uid) {
    android.util.EventLog.writeEvent(AM_UID_IDLE, uid);
  }

  public static void writeAmStopIdleService(int uid, String componentName) {
    android.util.EventLog.writeEvent(AM_STOP_IDLE_SERVICE, uid, componentName);
  }

  public static void writeAmCompact(int pid, String processName, String action, long beforersstotal, long beforerssfile, long beforerssanon, long beforerssswap, long deltarsstotal, long deltarssfile, long deltarssanon, long deltarssswap, long time, int lastaction, long lastactiontimestamp, int setadj, int procstate, long beforezramfree, long deltazramfree) {
    android.util.EventLog.writeEvent(AM_COMPACT, pid, processName, action, beforersstotal, beforerssfile, beforerssanon, beforerssswap, deltarsstotal, deltarssfile, deltarssanon, deltarssswap, time, lastaction, lastactiontimestamp, setadj, procstate, beforezramfree, deltazramfree);
  }

  public static void writeAmFreeze(int pid, String processName) {
    android.util.EventLog.writeEvent(AM_FREEZE, pid, processName);
  }

  public static void writeAmUnfreeze(int pid, String processName) {
    android.util.EventLog.writeEvent(AM_UNFREEZE, pid, processName);
  }

  public static void writeUcFinishUserUnlocking(int userid) {
    android.util.EventLog.writeEvent(UC_FINISH_USER_UNLOCKING, userid);
  }

  public static void writeUcFinishUserUnlocked(int userid) {
    android.util.EventLog.writeEvent(UC_FINISH_USER_UNLOCKED, userid);
  }

  public static void writeUcFinishUserUnlockedCompleted(int userid) {
    android.util.EventLog.writeEvent(UC_FINISH_USER_UNLOCKED_COMPLETED, userid);
  }

  public static void writeUcFinishUserStopping(int userid) {
    android.util.EventLog.writeEvent(UC_FINISH_USER_STOPPING, userid);
  }

  public static void writeUcFinishUserStopped(int userid) {
    android.util.EventLog.writeEvent(UC_FINISH_USER_STOPPED, userid);
  }

  public static void writeUcSwitchUser(int userid) {
    android.util.EventLog.writeEvent(UC_SWITCH_USER, userid);
  }

  public static void writeUcStartUserInternal(int userid, int foreground, int displayid) {
    android.util.EventLog.writeEvent(UC_START_USER_INTERNAL, userid, foreground, displayid);
  }

  public static void writeUcUnlockUser(int userid) {
    android.util.EventLog.writeEvent(UC_UNLOCK_USER, userid);
  }

  public static void writeUcFinishUserBoot(int userid) {
    android.util.EventLog.writeEvent(UC_FINISH_USER_BOOT, userid);
  }

  public static void writeUcDispatchUserSwitch(int olduserid, int newuserid) {
    android.util.EventLog.writeEvent(UC_DISPATCH_USER_SWITCH, olduserid, newuserid);
  }

  public static void writeUcContinueUserSwitch(int olduserid, int newuserid) {
    android.util.EventLog.writeEvent(UC_CONTINUE_USER_SWITCH, olduserid, newuserid);
  }

  public static void writeUcSendUserBroadcast(int userid, String intentaction) {
    android.util.EventLog.writeEvent(UC_SEND_USER_BROADCAST, userid, intentaction);
  }

  public static void writeSsmUserStarting(int userid) {
    android.util.EventLog.writeEvent(SSM_USER_STARTING, userid);
  }

  public static void writeSsmUserSwitching(int olduserid, int newuserid) {
    android.util.EventLog.writeEvent(SSM_USER_SWITCHING, olduserid, newuserid);
  }

  public static void writeSsmUserUnlocking(int userid) {
    android.util.EventLog.writeEvent(SSM_USER_UNLOCKING, userid);
  }

  public static void writeSsmUserUnlocked(int userid) {
    android.util.EventLog.writeEvent(SSM_USER_UNLOCKED, userid);
  }

  public static void writeSsmUserStopping(int userid) {
    android.util.EventLog.writeEvent(SSM_USER_STOPPING, userid);
  }

  public static void writeSsmUserStopped(int userid) {
    android.util.EventLog.writeEvent(SSM_USER_STOPPED, userid);
  }

  public static void writeSsmUserCompletedEvent(int userid, int eventflag) {
    android.util.EventLog.writeEvent(SSM_USER_COMPLETED_EVENT, userid, eventflag);
  }

  public static void writeUmUserVisibilityChanged(int userid, int visible) {
    android.util.EventLog.writeEvent(UM_USER_VISIBILITY_CHANGED, userid, visible);
  }

  public static void writeAmForegroundServiceStart(int user, String componentName, int allowwhileinuse, String startreasoncode, int targetsdk, int callertargetsdk, int notificationwasdeferred, int notificationshown, int durationms, int startforegroundcount, String stopreason, int fgstype) {
    android.util.EventLog.writeEvent(AM_FOREGROUND_SERVICE_START, user, componentName, allowwhileinuse, startreasoncode, targetsdk, callertargetsdk, notificationwasdeferred, notificationshown, durationms, startforegroundcount, stopreason, fgstype);
  }

  public static void writeAmForegroundServiceDenied(int user, String componentName, int allowwhileinuse, String startreasoncode, int targetsdk, int callertargetsdk, int notificationwasdeferred, int notificationshown, int durationms, int startforegroundcount, String stopreason, int fgstype) {
    android.util.EventLog.writeEvent(AM_FOREGROUND_SERVICE_DENIED, user, componentName, allowwhileinuse, startreasoncode, targetsdk, callertargetsdk, notificationwasdeferred, notificationshown, durationms, startforegroundcount, stopreason, fgstype);
  }

  public static void writeAmForegroundServiceStop(int user, String componentName, int allowwhileinuse, String startreasoncode, int targetsdk, int callertargetsdk, int notificationwasdeferred, int notificationshown, int durationms, int startforegroundcount, String stopreason, int fgstype) {
    android.util.EventLog.writeEvent(AM_FOREGROUND_SERVICE_STOP, user, componentName, allowwhileinuse, startreasoncode, targetsdk, callertargetsdk, notificationwasdeferred, notificationshown, durationms, startforegroundcount, stopreason, fgstype);
  }

  public static void writeAmForegroundServiceTimedOut(int user, String componentName, int allowwhileinuse, String startreasoncode, int targetsdk, int callertargetsdk, int notificationwasdeferred, int notificationshown, int durationms, int startforegroundcount, String stopreason, int fgstype) {
    android.util.EventLog.writeEvent(AM_FOREGROUND_SERVICE_TIMED_OUT, user, componentName, allowwhileinuse, startreasoncode, targetsdk, callertargetsdk, notificationwasdeferred, notificationshown, durationms, startforegroundcount, stopreason, fgstype);
  }

  public static void writeAmCpu(long pid, long uid, String baseName, long uptime, long stime, long utime) {
    android.util.EventLog.writeEvent(AM_CPU, pid, uid, baseName, uptime, stime, utime);
  }

  public static void writeAmIntentSenderRedirectUser(int userid) {
    android.util.EventLog.writeEvent(AM_INTENT_SENDER_REDIRECT_USER, userid);
  }

  public static void writeAmClearAppDataCaller(int pid, int uid, String package_) {
    android.util.EventLog.writeEvent(AM_CLEAR_APP_DATA_CALLER, pid, uid, package_);
  }

  public static void writeAmUidStateChanged(int uid, int seq, int uidstate, int olduidstate, int capability, int oldcapability, int flags, String reason) {
    android.util.EventLog.writeEvent(AM_UID_STATE_CHANGED, uid, seq, uidstate, olduidstate, capability, oldcapability, flags, reason);
  }

  public static void writeAmProcStateChanged(int uid, int pid, int seq, int procstate, int oldprocstate, int oomadj, int oldoomadj, String reason) {
    android.util.EventLog.writeEvent(AM_PROC_STATE_CHANGED, uid, pid, seq, procstate, oldprocstate, oomadj, oldoomadj, reason);
  }

  public static void writeAmOomAdjMisc(int event, int uid, int pid, int seq, int arg1, int arg2, String reason) {
    android.util.EventLog.writeEvent(AM_OOM_ADJ_MISC, event, uid, pid, seq, arg1, arg2, reason);
  }
}
