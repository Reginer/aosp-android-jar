/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/services/core/java/com/android/server/EventLogTags.logtags
 */

package com.android.server;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 2722 battery_level (level|1|6),(voltage|1|1),(temperature|1|1) */
  public static final int BATTERY_LEVEL = 2722;

  /** 2723 battery_status (status|1|5),(health|1|5),(present|1|5),(plugged|1|5),(technology|3) */
  public static final int BATTERY_STATUS = 2723;

  /** 2730 battery_discharge (duration|2|3),(minLevel|1|6),(maxLevel|1|6) */
  public static final int BATTERY_DISCHARGE = 2730;

  /** 2724 power_sleep_requested (wakeLocksCleared|1|1) */
  public static final int POWER_SLEEP_REQUESTED = 2724;

  /** 2725 power_screen_broadcast_send (wakelockCount|1|1) */
  public static final int POWER_SCREEN_BROADCAST_SEND = 2725;

  /** 2726 power_screen_broadcast_done (on|1|5),(broadcastDuration|2|3),(wakelockCount|1|1) */
  public static final int POWER_SCREEN_BROADCAST_DONE = 2726;

  /** 2727 power_screen_broadcast_stop (which|1|5),(wakelockCount|1|1) */
  public static final int POWER_SCREEN_BROADCAST_STOP = 2727;

  /** 2728 power_screen_state (offOrOn|1|5),(becauseOfUser|1|5),(totalTouchDownTime|2|3),(touchCycles|1|1),(latency|1|3) */
  public static final int POWER_SCREEN_STATE = 2728;

  /** 2729 power_partial_wake_state (releasedorAcquired|1|5),(tag|3) */
  public static final int POWER_PARTIAL_WAKE_STATE = 2729;

  /** 2731 power_soft_sleep_requested (savedwaketimems|2) */
  public static final int POWER_SOFT_SLEEP_REQUESTED = 2731;

  /** 2739 battery_saver_mode (fullPrevOffOrOn|1|5),(adaptivePrevOffOrOn|1|5),(fullNowOffOrOn|1|5),(adaptiveNowOffOrOn|1|5),(interactive|1|5),(features|3|5),(reason|1|5) */
  public static final int BATTERY_SAVER_MODE = 2739;

  /** 27390 battery_saving_stats (batterySaver|1|5),(interactive|1|5),(doze|1|5),(delta_duration|2|3),(delta_battery_drain|1|1),(delta_battery_drain_percent|1|6),(total_duration|2|3),(total_battery_drain|1|1),(total_battery_drain_percent|1|6) */
  public static final int BATTERY_SAVING_STATS = 27390;

  /** 27391 user_activity_timeout_override (override|2|3) */
  public static final int USER_ACTIVITY_TIMEOUT_OVERRIDE = 27391;

  /** 27392 battery_saver_setting (threshold|1) */
  public static final int BATTERY_SAVER_SETTING = 27392;

  /** 2737 thermal_changed (name|3),(type|1|5),(temperature|5),(sensor_status|1|5),(previous_system_status|1|5) */
  public static final int THERMAL_CHANGED = 2737;

  /** 2748 cache_file_deleted (path|3) */
  public static final int CACHE_FILE_DELETED = 2748;

  /** 2749 storage_state (uuid|3),(old_state|1),(new_state|1),(usable|2),(total|2) */
  public static final int STORAGE_STATE = 2749;

  /** 2750 notification_enqueue (uid|1|5),(pid|1|5),(pkg|3),(id|1|5),(tag|3),(userid|1|5),(notification|3),(status|1),(app_provided|1) */
  public static final int NOTIFICATION_ENQUEUE = 2750;

  /** 2751 notification_cancel (uid|1|5),(pid|1|5),(pkg|3),(id|1|5),(tag|3),(userid|1|5),(required_flags|1),(forbidden_flags|1),(reason|1|5),(listener|3) */
  public static final int NOTIFICATION_CANCEL = 2751;

  /** 2752 notification_cancel_all (uid|1|5),(pid|1|5),(pkg|3),(userid|1|5),(required_flags|1),(forbidden_flags|1),(reason|1|5),(listener|3) */
  public static final int NOTIFICATION_CANCEL_ALL = 2752;

  /** 27500 notification_panel_revealed (items|1) */
  public static final int NOTIFICATION_PANEL_REVEALED = 27500;

  /** 27501 notification_panel_hidden */
  public static final int NOTIFICATION_PANEL_HIDDEN = 27501;

  /** 27510 notification_visibility_changed (newlyVisibleKeys|3),(noLongerVisibleKeys|3) */
  public static final int NOTIFICATION_VISIBILITY_CHANGED = 27510;

  /** 27511 notification_expansion (key|3),(user_action|1),(expanded|1),(lifespan|1),(freshness|1),(exposure|1) */
  public static final int NOTIFICATION_EXPANSION = 27511;

  /** 27520 notification_clicked (key|3),(lifespan|1),(freshness|1),(exposure|1),(rank|1),(count|1) */
  public static final int NOTIFICATION_CLICKED = 27520;

  /** 27521 notification_action_clicked (key|3),(piIdentifier|3),(pendingIntent|3),(action_index|1),(lifespan|1),(freshness|1),(exposure|1),(rank|1),(count|1) */
  public static final int NOTIFICATION_ACTION_CLICKED = 27521;

  /** 27530 notification_canceled (key|3),(reason|1),(lifespan|1),(freshness|1),(exposure|1),(rank|1),(count|1),(listener|3) */
  public static final int NOTIFICATION_CANCELED = 27530;

  /** 27531 notification_visibility (key|3),(visibile|1),(lifespan|1),(freshness|1),(exposure|1),(rank|1) */
  public static final int NOTIFICATION_VISIBILITY = 27531;

  /** 27532 notification_alert (key|3),(buzz|1),(beep|1),(blink|1),(politeness|1),(mute_reason|1) */
  public static final int NOTIFICATION_ALERT = 27532;

  /** 27533 notification_autogrouped (key|3) */
  public static final int NOTIFICATION_AUTOGROUPED = 27533;

  /** 275534 notification_unautogrouped (key|3) */
  public static final int NOTIFICATION_UNAUTOGROUPED = 275534;

  /** 27535 notification_adjusted (key|3),(adjustment_type|3),(new_value|3) */
  public static final int NOTIFICATION_ADJUSTED = 27535;

  /** 27536 notification_cancel_prevented (key|3) */
  public static final int NOTIFICATION_CANCEL_PREVENTED = 27536;

  /** 27537 notification_summary_converted (key|3) */
  public static final int NOTIFICATION_SUMMARY_CONVERTED = 27537;

  /** 2802 watchdog (Service|3) */
  public static final int WATCHDOG = 2802;

  /** 2803 watchdog_proc_pss (Process|3),(Pid|1|5),(Pss|1|2) */
  public static final int WATCHDOG_PROC_PSS = 2803;

  /** 2804 watchdog_soft_reset (Process|3),(Pid|1|5),(MaxPss|1|2),(Pss|1|2),(Skip|3) */
  public static final int WATCHDOG_SOFT_RESET = 2804;

  /** 2805 watchdog_hard_reset (Process|3),(Pid|1|5),(MaxPss|1|2),(Pss|1|2) */
  public static final int WATCHDOG_HARD_RESET = 2805;

  /** 2806 watchdog_pss_stats (EmptyPss|1|2),(EmptyCount|1|1),(BackgroundPss|1|2),(BackgroundCount|1|1),(ServicePss|1|2),(ServiceCount|1|1),(VisiblePss|1|2),(VisibleCount|1|1),(ForegroundPss|1|2),(ForegroundCount|1|1),(NoPssCount|1|1) */
  public static final int WATCHDOG_PSS_STATS = 2806;

  /** 2807 watchdog_proc_stats (DeathsInOne|1|1),(DeathsInTwo|1|1),(DeathsInThree|1|1),(DeathsInFour|1|1),(DeathsInFive|1|1) */
  public static final int WATCHDOG_PROC_STATS = 2807;

  /** 2808 watchdog_scheduled_reboot (Now|2|1),(Interval|1|3),(StartTime|1|3),(Window|1|3),(Skip|3) */
  public static final int WATCHDOG_SCHEDULED_REBOOT = 2808;

  /** 2809 watchdog_meminfo (MemFree|1|2),(Buffers|1|2),(Cached|1|2),(Active|1|2),(Inactive|1|2),(AnonPages|1|2),(Mapped|1|2),(Slab|1|2),(SReclaimable|1|2),(SUnreclaim|1|2),(PageTables|1|2) */
  public static final int WATCHDOG_MEMINFO = 2809;

  /** 2810 watchdog_vmstat (runtime|2|3),(pgfree|1|1),(pgactivate|1|1),(pgdeactivate|1|1),(pgfault|1|1),(pgmajfault|1|1) */
  public static final int WATCHDOG_VMSTAT = 2810;

  /** 2811 watchdog_requested_reboot (NoWait|1|1),(ScheduleInterval|1|3),(RecheckInterval|1|3),(StartTime|1|3),(Window|1|3),(MinScreenOff|1|3),(MinNextAlarm|1|3) */
  public static final int WATCHDOG_REQUESTED_REBOOT = 2811;

  /** 2900 rescue_note (uid|1),(count|1),(window|2) */
  public static final int RESCUE_NOTE = 2900;

  /** 2901 rescue_level (level|1),(trigger_uid|1) */
  public static final int RESCUE_LEVEL = 2901;

  /** 2902 rescue_success (level|1) */
  public static final int RESCUE_SUCCESS = 2902;

  /** 2903 rescue_failure (level|1),(msg|3) */
  public static final int RESCUE_FAILURE = 2903;

  /** 2820 backup_data_changed (Package|3) */
  public static final int BACKUP_DATA_CHANGED = 2820;

  /** 2821 backup_start (Transport|3) */
  public static final int BACKUP_START = 2821;

  /** 2822 backup_transport_failure (Package|3) */
  public static final int BACKUP_TRANSPORT_FAILURE = 2822;

  /** 2823 backup_agent_failure (Package|3),(Message|3) */
  public static final int BACKUP_AGENT_FAILURE = 2823;

  /** 2824 backup_package (Package|3),(Size|1|2) */
  public static final int BACKUP_PACKAGE = 2824;

  /** 2825 backup_success (Packages|1|1),(Time|1|3) */
  public static final int BACKUP_SUCCESS = 2825;

  /** 2826 backup_reset (Transport|3) */
  public static final int BACKUP_RESET = 2826;

  /** 2827 backup_initialize */
  public static final int BACKUP_INITIALIZE = 2827;

  /** 2828 backup_requested (Total|1|1),(Key-Value|1|1),(Full|1|1) */
  public static final int BACKUP_REQUESTED = 2828;

  /** 2829 backup_quota_exceeded (Package|3) */
  public static final int BACKUP_QUOTA_EXCEEDED = 2829;

  /** 2830 restore_start (Transport|3),(Source|2|5) */
  public static final int RESTORE_START = 2830;

  /** 2831 restore_transport_failure */
  public static final int RESTORE_TRANSPORT_FAILURE = 2831;

  /** 2832 restore_agent_failure (Package|3),(Message|3) */
  public static final int RESTORE_AGENT_FAILURE = 2832;

  /** 2833 restore_package (Package|3),(Size|1|2) */
  public static final int RESTORE_PACKAGE = 2833;

  /** 2834 restore_success (Packages|1|1),(Time|1|3) */
  public static final int RESTORE_SUCCESS = 2834;

  /** 2840 full_backup_package (Package|3) */
  public static final int FULL_BACKUP_PACKAGE = 2840;

  /** 2841 full_backup_agent_failure (Package|3),(Message|3) */
  public static final int FULL_BACKUP_AGENT_FAILURE = 2841;

  /** 2842 full_backup_transport_failure */
  public static final int FULL_BACKUP_TRANSPORT_FAILURE = 2842;

  /** 2843 full_backup_success (Package|3) */
  public static final int FULL_BACKUP_SUCCESS = 2843;

  /** 2844 full_restore_package (Package|3) */
  public static final int FULL_RESTORE_PACKAGE = 2844;

  /** 2845 full_backup_quota_exceeded (Package|3) */
  public static final int FULL_BACKUP_QUOTA_EXCEEDED = 2845;

  /** 2846 full_backup_cancelled (Package|3),(Message|3) */
  public static final int FULL_BACKUP_CANCELLED = 2846;

  /** 2850 backup_transport_lifecycle (Transport|3),(Bound|1|1) */
  public static final int BACKUP_TRANSPORT_LIFECYCLE = 2850;

  /** 2851 backup_transport_connection (Transport|3),(Connected|1|1) */
  public static final int BACKUP_TRANSPORT_CONNECTION = 2851;

  /** 3010 boot_progress_system_run (time|2|3) */
  public static final int BOOT_PROGRESS_SYSTEM_RUN = 3010;

  /** 3011 system_server_start (start_count|1),(uptime|2|3),(elapse_time|2|3) */
  public static final int SYSTEM_SERVER_START = 3011;

  /** 3060 boot_progress_pms_start (time|2|3) */
  public static final int BOOT_PROGRESS_PMS_START = 3060;

  /** 3070 boot_progress_pms_system_scan_start (time|2|3) */
  public static final int BOOT_PROGRESS_PMS_SYSTEM_SCAN_START = 3070;

  /** 3080 boot_progress_pms_data_scan_start (time|2|3) */
  public static final int BOOT_PROGRESS_PMS_DATA_SCAN_START = 3080;

  /** 3090 boot_progress_pms_scan_end (time|2|3) */
  public static final int BOOT_PROGRESS_PMS_SCAN_END = 3090;

  /** 3100 boot_progress_pms_ready (time|2|3) */
  public static final int BOOT_PROGRESS_PMS_READY = 3100;

  /** 3110 unknown_sources_enabled (value|1) */
  public static final int UNKNOWN_SOURCES_ENABLED = 3110;

  /** 3120 pm_critical_info (msg|3) */
  public static final int PM_CRITICAL_INFO = 3120;

  /** 3121 pm_package_stats (manual_time|2|3),(quota_time|2|3),(manual_data|2|2),(quota_data|2|2),(manual_cache|2|2),(quota_cache|2|2) */
  public static final int PM_PACKAGE_STATS = 3121;

  /** 3130 pm_snapshot_stats (build_count|1|1),(reuse_count|1|1),(big_builds|1|1),(short_lived|1|1),(max_build_time|1|3),(cumm_build_time|2|3) */
  public static final int PM_SNAPSHOT_STATS = 3130;

  /** 3131 pm_snapshot_rebuild (build_time|1|3),(lifetime|1|3) */
  public static final int PM_SNAPSHOT_REBUILD = 3131;

  /** 3132 pm_clear_app_data_caller (pid|1),(uid|1),(package|3) */
  public static final int PM_CLEAR_APP_DATA_CALLER = 3132;

  /** 32000 imf_force_reconnect_ime (IME|4),(Time Since Connect|2|3),(Showing|1|1) */
  public static final int IMF_FORCE_RECONNECT_IME = 32000;

  /** 32001 imf_show_ime (token|3),(window|3),(reason|3),(softInputMode|3) */
  public static final int IMF_SHOW_IME = 32001;

  /** 32002 imf_hide_ime (token|3),(window|3),(reason|3),(softInputMode|3) */
  public static final int IMF_HIDE_IME = 32002;

  /** 33000 wp_wallpaper_crashed (component|3) */
  public static final int WP_WALLPAPER_CRASHED = 33000;

  /** 34000 device_idle (state|1|5), (reason|3) */
  public static final int DEVICE_IDLE = 34000;

  /** 34001 device_idle_step */
  public static final int DEVICE_IDLE_STEP = 34001;

  /** 34002 device_idle_wake_from_idle (is_idle|1|5), (reason|3) */
  public static final int DEVICE_IDLE_WAKE_FROM_IDLE = 34002;

  /** 34003 device_idle_on_start */
  public static final int DEVICE_IDLE_ON_START = 34003;

  /** 34004 device_idle_on_phase (what|3) */
  public static final int DEVICE_IDLE_ON_PHASE = 34004;

  /** 34005 device_idle_on_complete */
  public static final int DEVICE_IDLE_ON_COMPLETE = 34005;

  /** 34006 device_idle_off_start (reason|3) */
  public static final int DEVICE_IDLE_OFF_START = 34006;

  /** 34007 device_idle_off_phase (what|3) */
  public static final int DEVICE_IDLE_OFF_PHASE = 34007;

  /** 34008 device_idle_off_complete */
  public static final int DEVICE_IDLE_OFF_COMPLETE = 34008;

  /** 34009 device_idle_light (state|1|5), (reason|3) */
  public static final int DEVICE_IDLE_LIGHT = 34009;

  /** 34010 device_idle_light_step */
  public static final int DEVICE_IDLE_LIGHT_STEP = 34010;

  /** 35000 auto_brightness_adj (old_lux|5),(old_brightness|5),(new_lux|5),(new_brightness|5) */
  public static final int AUTO_BRIGHTNESS_ADJ = 35000;

  /** 39000 installer_clear_app_data_caller (pid|1),(uid|1),(package|3),(flags|1) */
  public static final int INSTALLER_CLEAR_APP_DATA_CALLER = 39000;

  /** 39001 installer_clear_app_data_call_stack (method|3),(class|3),(file|3),(line|1) */
  public static final int INSTALLER_CLEAR_APP_DATA_CALL_STACK = 39001;

  /** 50020 connectivity_state_changed (type|1),(subtype|1),(state|1) */
  public static final int CONNECTIVITY_STATE_CHANGED = 50020;

  /** 51100 netstats_mobile_sample (xt_rx_bytes|2|2),(xt_tx_bytes|2|2),(xt_rx_pkts|2|1),(xt_tx_pkts|2|1),(uid_rx_bytes|2|2),(uid_tx_bytes|2|2),(uid_rx_pkts|2|1),(uid_tx_pkts|2|1),(trusted_time|2|3) */
  public static final int NETSTATS_MOBILE_SAMPLE = 51100;

  /** 51101 netstats_wifi_sample (xt_rx_bytes|2|2),(xt_tx_bytes|2|2),(xt_rx_pkts|2|1),(xt_tx_pkts|2|1),(uid_rx_bytes|2|2),(uid_tx_bytes|2|2),(uid_rx_pkts|2|1),(uid_tx_pkts|2|1),(trusted_time|2|3) */
  public static final int NETSTATS_WIFI_SAMPLE = 51101;

  /** 51200 lockdown_vpn_connecting (egress_net|1) */
  public static final int LOCKDOWN_VPN_CONNECTING = 51200;

  /** 51201 lockdown_vpn_connected (egress_net|1) */
  public static final int LOCKDOWN_VPN_CONNECTED = 51201;

  /** 51202 lockdown_vpn_error (egress_net|1) */
  public static final int LOCKDOWN_VPN_ERROR = 51202;

  /** 51300 config_install_failed (dir|3) */
  public static final int CONFIG_INSTALL_FAILED = 51300;

  /** 51400 ifw_intent_matched (Intent Type|1|5),(Component Name|3),(Caller Uid|1|5),(Caller Pkg Count|1|1),(Caller Pkgs|3),(Action|3),(MIME Type|3),(URI|3),(Flags|1|5) */
  public static final int IFW_INTENT_MATCHED = 51400;

  /** 51500 idle_maintenance_window_start (time|2|3), (lastUserActivity|2|3), (batteryLevel|1|6), (batteryCharging|1|5) */
  public static final int IDLE_MAINTENANCE_WINDOW_START = 51500;

  /** 51501 idle_maintenance_window_finish (time|2|3), (lastUserActivity|2|3), (batteryLevel|1|6), (batteryCharging|1|5) */
  public static final int IDLE_MAINTENANCE_WINDOW_FINISH = 51501;

  /** 2755 fstrim_start (time|2|3) */
  public static final int FSTRIM_START = 2755;

  /** 2756 fstrim_finish (time|2|3) */
  public static final int FSTRIM_FINISH = 2756;

  /** 8000 job_deferred_execution (time|2|3) */
  public static final int JOB_DEFERRED_EXECUTION = 8000;

  /** 40000 volume_changed (stream|1), (prev_level|1), (level|1), (max_level|1), (caller|3) */
  public static final int VOLUME_CHANGED = 40000;

  /** 40001 stream_devices_changed (stream|1), (prev_devices|1), (devices|1) */
  public static final int STREAM_DEVICES_CHANGED = 40001;

  /** 40100 camera_gesture_triggered (gesture_on_time|2|3), (sensor1_on_time|2|3), (sensor2_on_time|2|3), (event_extra|1|1) */
  public static final int CAMERA_GESTURE_TRIGGERED = 40100;

  /** 51600 timezone_trigger_check (token|3) */
  public static final int TIMEZONE_TRIGGER_CHECK = 51600;

  /** 51610 timezone_request_install (token|3) */
  public static final int TIMEZONE_REQUEST_INSTALL = 51610;

  /** 51611 timezone_install_started (token|3) */
  public static final int TIMEZONE_INSTALL_STARTED = 51611;

  /** 51612 timezone_install_complete (token|3), (result|1) */
  public static final int TIMEZONE_INSTALL_COMPLETE = 51612;

  /** 51620 timezone_request_uninstall (token|3) */
  public static final int TIMEZONE_REQUEST_UNINSTALL = 51620;

  /** 51621 timezone_uninstall_started (token|3) */
  public static final int TIMEZONE_UNINSTALL_STARTED = 51621;

  /** 51622 timezone_uninstall_complete (token|3), (result|1) */
  public static final int TIMEZONE_UNINSTALL_COMPLETE = 51622;

  /** 51630 timezone_request_nothing (token|3) */
  public static final int TIMEZONE_REQUEST_NOTHING = 51630;

  /** 51631 timezone_nothing_complete (token|3) */
  public static final int TIMEZONE_NOTHING_COMPLETE = 51631;

  public static void writeBatteryLevel(int level, int voltage, int temperature) {
    android.util.EventLog.writeEvent(BATTERY_LEVEL, level, voltage, temperature);
  }

  public static void writeBatteryStatus(int status, int health, int present, int plugged, String technology) {
    android.util.EventLog.writeEvent(BATTERY_STATUS, status, health, present, plugged, technology);
  }

  public static void writeBatteryDischarge(long duration, int minlevel, int maxlevel) {
    android.util.EventLog.writeEvent(BATTERY_DISCHARGE, duration, minlevel, maxlevel);
  }

  public static void writePowerSleepRequested(int wakelockscleared) {
    android.util.EventLog.writeEvent(POWER_SLEEP_REQUESTED, wakelockscleared);
  }

  public static void writePowerScreenBroadcastSend(int wakelockcount) {
    android.util.EventLog.writeEvent(POWER_SCREEN_BROADCAST_SEND, wakelockcount);
  }

  public static void writePowerScreenBroadcastDone(int on, long broadcastduration, int wakelockcount) {
    android.util.EventLog.writeEvent(POWER_SCREEN_BROADCAST_DONE, on, broadcastduration, wakelockcount);
  }

  public static void writePowerScreenBroadcastStop(int which, int wakelockcount) {
    android.util.EventLog.writeEvent(POWER_SCREEN_BROADCAST_STOP, which, wakelockcount);
  }

  public static void writePowerScreenState(int offoron, int becauseofuser, long totaltouchdowntime, int touchcycles, int latency) {
    android.util.EventLog.writeEvent(POWER_SCREEN_STATE, offoron, becauseofuser, totaltouchdowntime, touchcycles, latency);
  }

  public static void writePowerPartialWakeState(int releasedoracquired, String tag) {
    android.util.EventLog.writeEvent(POWER_PARTIAL_WAKE_STATE, releasedoracquired, tag);
  }

  public static void writePowerSoftSleepRequested(long savedwaketimems) {
    android.util.EventLog.writeEvent(POWER_SOFT_SLEEP_REQUESTED, savedwaketimems);
  }

  public static void writeBatterySaverMode(int fullprevofforon, int adaptiveprevofforon, int fullnowofforon, int adaptivenowofforon, int interactive, String features, int reason) {
    android.util.EventLog.writeEvent(BATTERY_SAVER_MODE, fullprevofforon, adaptiveprevofforon, fullnowofforon, adaptivenowofforon, interactive, features, reason);
  }

  public static void writeBatterySavingStats(int batterysaver, int interactive, int doze, long deltaDuration, int deltaBatteryDrain, int deltaBatteryDrainPercent, long totalDuration, int totalBatteryDrain, int totalBatteryDrainPercent) {
    android.util.EventLog.writeEvent(BATTERY_SAVING_STATS, batterysaver, interactive, doze, deltaDuration, deltaBatteryDrain, deltaBatteryDrainPercent, totalDuration, totalBatteryDrain, totalBatteryDrainPercent);
  }

  public static void writeUserActivityTimeoutOverride(long override) {
    android.util.EventLog.writeEvent(USER_ACTIVITY_TIMEOUT_OVERRIDE, override);
  }

  public static void writeBatterySaverSetting(int threshold) {
    android.util.EventLog.writeEvent(BATTERY_SAVER_SETTING, threshold);
  }

  public static void writeThermalChanged(String name, int type, float temperature, int sensorStatus, int previousSystemStatus) {
    android.util.EventLog.writeEvent(THERMAL_CHANGED, name, type, temperature, sensorStatus, previousSystemStatus);
  }

  public static void writeCacheFileDeleted(String path) {
    android.util.EventLog.writeEvent(CACHE_FILE_DELETED, path);
  }

  public static void writeStorageState(String uuid, int oldState, int newState, long usable, long total) {
    android.util.EventLog.writeEvent(STORAGE_STATE, uuid, oldState, newState, usable, total);
  }

  public static void writeNotificationEnqueue(int uid, int pid, String pkg, int id, String tag, int userid, String notification, int status, int appProvided) {
    android.util.EventLog.writeEvent(NOTIFICATION_ENQUEUE, uid, pid, pkg, id, tag, userid, notification, status, appProvided);
  }

  public static void writeNotificationCancel(int uid, int pid, String pkg, int id, String tag, int userid, int requiredFlags, int forbiddenFlags, int reason, String listener) {
    android.util.EventLog.writeEvent(NOTIFICATION_CANCEL, uid, pid, pkg, id, tag, userid, requiredFlags, forbiddenFlags, reason, listener);
  }

  public static void writeNotificationCancelAll(int uid, int pid, String pkg, int userid, int requiredFlags, int forbiddenFlags, int reason, String listener) {
    android.util.EventLog.writeEvent(NOTIFICATION_CANCEL_ALL, uid, pid, pkg, userid, requiredFlags, forbiddenFlags, reason, listener);
  }

  public static void writeNotificationPanelRevealed(int items) {
    android.util.EventLog.writeEvent(NOTIFICATION_PANEL_REVEALED, items);
  }

  public static void writeNotificationPanelHidden() {
    android.util.EventLog.writeEvent(NOTIFICATION_PANEL_HIDDEN);
  }

  public static void writeNotificationVisibilityChanged(String newlyvisiblekeys, String nolongervisiblekeys) {
    android.util.EventLog.writeEvent(NOTIFICATION_VISIBILITY_CHANGED, newlyvisiblekeys, nolongervisiblekeys);
  }

  public static void writeNotificationExpansion(String key, int userAction, int expanded, int lifespan, int freshness, int exposure) {
    android.util.EventLog.writeEvent(NOTIFICATION_EXPANSION, key, userAction, expanded, lifespan, freshness, exposure);
  }

  public static void writeNotificationClicked(String key, int lifespan, int freshness, int exposure, int rank, int count) {
    android.util.EventLog.writeEvent(NOTIFICATION_CLICKED, key, lifespan, freshness, exposure, rank, count);
  }

  public static void writeNotificationActionClicked(String key, String piidentifier, String pendingintent, int actionIndex, int lifespan, int freshness, int exposure, int rank, int count) {
    android.util.EventLog.writeEvent(NOTIFICATION_ACTION_CLICKED, key, piidentifier, pendingintent, actionIndex, lifespan, freshness, exposure, rank, count);
  }

  public static void writeNotificationCanceled(String key, int reason, int lifespan, int freshness, int exposure, int rank, int count, String listener) {
    android.util.EventLog.writeEvent(NOTIFICATION_CANCELED, key, reason, lifespan, freshness, exposure, rank, count, listener);
  }

  public static void writeNotificationVisibility(String key, int visibile, int lifespan, int freshness, int exposure, int rank) {
    android.util.EventLog.writeEvent(NOTIFICATION_VISIBILITY, key, visibile, lifespan, freshness, exposure, rank);
  }

  public static void writeNotificationAlert(String key, int buzz, int beep, int blink, int politeness, int muteReason) {
    android.util.EventLog.writeEvent(NOTIFICATION_ALERT, key, buzz, beep, blink, politeness, muteReason);
  }

  public static void writeNotificationAutogrouped(String key) {
    android.util.EventLog.writeEvent(NOTIFICATION_AUTOGROUPED, key);
  }

  public static void writeNotificationUnautogrouped(String key) {
    android.util.EventLog.writeEvent(NOTIFICATION_UNAUTOGROUPED, key);
  }

  public static void writeNotificationAdjusted(String key, String adjustmentType, String newValue) {
    android.util.EventLog.writeEvent(NOTIFICATION_ADJUSTED, key, adjustmentType, newValue);
  }

  public static void writeNotificationCancelPrevented(String key) {
    android.util.EventLog.writeEvent(NOTIFICATION_CANCEL_PREVENTED, key);
  }

  public static void writeNotificationSummaryConverted(String key) {
    android.util.EventLog.writeEvent(NOTIFICATION_SUMMARY_CONVERTED, key);
  }

  public static void writeWatchdog(String service) {
    android.util.EventLog.writeEvent(WATCHDOG, service);
  }

  public static void writeWatchdogProcPss(String process, int pid, int pss) {
    android.util.EventLog.writeEvent(WATCHDOG_PROC_PSS, process, pid, pss);
  }

  public static void writeWatchdogSoftReset(String process, int pid, int maxpss, int pss, String skip) {
    android.util.EventLog.writeEvent(WATCHDOG_SOFT_RESET, process, pid, maxpss, pss, skip);
  }

  public static void writeWatchdogHardReset(String process, int pid, int maxpss, int pss) {
    android.util.EventLog.writeEvent(WATCHDOG_HARD_RESET, process, pid, maxpss, pss);
  }

  public static void writeWatchdogPssStats(int emptypss, int emptycount, int backgroundpss, int backgroundcount, int servicepss, int servicecount, int visiblepss, int visiblecount, int foregroundpss, int foregroundcount, int nopsscount) {
    android.util.EventLog.writeEvent(WATCHDOG_PSS_STATS, emptypss, emptycount, backgroundpss, backgroundcount, servicepss, servicecount, visiblepss, visiblecount, foregroundpss, foregroundcount, nopsscount);
  }

  public static void writeWatchdogProcStats(int deathsinone, int deathsintwo, int deathsinthree, int deathsinfour, int deathsinfive) {
    android.util.EventLog.writeEvent(WATCHDOG_PROC_STATS, deathsinone, deathsintwo, deathsinthree, deathsinfour, deathsinfive);
  }

  public static void writeWatchdogScheduledReboot(long now, int interval, int starttime, int window, String skip) {
    android.util.EventLog.writeEvent(WATCHDOG_SCHEDULED_REBOOT, now, interval, starttime, window, skip);
  }

  public static void writeWatchdogMeminfo(int memfree, int buffers, int cached, int active, int inactive, int anonpages, int mapped, int slab, int sreclaimable, int sunreclaim, int pagetables) {
    android.util.EventLog.writeEvent(WATCHDOG_MEMINFO, memfree, buffers, cached, active, inactive, anonpages, mapped, slab, sreclaimable, sunreclaim, pagetables);
  }

  public static void writeWatchdogVmstat(long runtime, int pgfree, int pgactivate, int pgdeactivate, int pgfault, int pgmajfault) {
    android.util.EventLog.writeEvent(WATCHDOG_VMSTAT, runtime, pgfree, pgactivate, pgdeactivate, pgfault, pgmajfault);
  }

  public static void writeWatchdogRequestedReboot(int nowait, int scheduleinterval, int recheckinterval, int starttime, int window, int minscreenoff, int minnextalarm) {
    android.util.EventLog.writeEvent(WATCHDOG_REQUESTED_REBOOT, nowait, scheduleinterval, recheckinterval, starttime, window, minscreenoff, minnextalarm);
  }

  public static void writeRescueNote(int uid, int count, long window) {
    android.util.EventLog.writeEvent(RESCUE_NOTE, uid, count, window);
  }

  public static void writeRescueLevel(int level, int triggerUid) {
    android.util.EventLog.writeEvent(RESCUE_LEVEL, level, triggerUid);
  }

  public static void writeRescueSuccess(int level) {
    android.util.EventLog.writeEvent(RESCUE_SUCCESS, level);
  }

  public static void writeRescueFailure(int level, String msg) {
    android.util.EventLog.writeEvent(RESCUE_FAILURE, level, msg);
  }

  public static void writeBackupDataChanged(String package_) {
    android.util.EventLog.writeEvent(BACKUP_DATA_CHANGED, package_);
  }

  public static void writeBackupStart(String transport) {
    android.util.EventLog.writeEvent(BACKUP_START, transport);
  }

  public static void writeBackupTransportFailure(String package_) {
    android.util.EventLog.writeEvent(BACKUP_TRANSPORT_FAILURE, package_);
  }

  public static void writeBackupAgentFailure(String package_, String message) {
    android.util.EventLog.writeEvent(BACKUP_AGENT_FAILURE, package_, message);
  }

  public static void writeBackupPackage(String package_, int size) {
    android.util.EventLog.writeEvent(BACKUP_PACKAGE, package_, size);
  }

  public static void writeBackupSuccess(int packages, int time) {
    android.util.EventLog.writeEvent(BACKUP_SUCCESS, packages, time);
  }

  public static void writeBackupReset(String transport) {
    android.util.EventLog.writeEvent(BACKUP_RESET, transport);
  }

  public static void writeBackupInitialize() {
    android.util.EventLog.writeEvent(BACKUP_INITIALIZE);
  }

  public static void writeBackupRequested(int total, int keyValue, int full) {
    android.util.EventLog.writeEvent(BACKUP_REQUESTED, total, keyValue, full);
  }

  public static void writeBackupQuotaExceeded(String package_) {
    android.util.EventLog.writeEvent(BACKUP_QUOTA_EXCEEDED, package_);
  }

  public static void writeRestoreStart(String transport, long source) {
    android.util.EventLog.writeEvent(RESTORE_START, transport, source);
  }

  public static void writeRestoreTransportFailure() {
    android.util.EventLog.writeEvent(RESTORE_TRANSPORT_FAILURE);
  }

  public static void writeRestoreAgentFailure(String package_, String message) {
    android.util.EventLog.writeEvent(RESTORE_AGENT_FAILURE, package_, message);
  }

  public static void writeRestorePackage(String package_, int size) {
    android.util.EventLog.writeEvent(RESTORE_PACKAGE, package_, size);
  }

  public static void writeRestoreSuccess(int packages, int time) {
    android.util.EventLog.writeEvent(RESTORE_SUCCESS, packages, time);
  }

  public static void writeFullBackupPackage(String package_) {
    android.util.EventLog.writeEvent(FULL_BACKUP_PACKAGE, package_);
  }

  public static void writeFullBackupAgentFailure(String package_, String message) {
    android.util.EventLog.writeEvent(FULL_BACKUP_AGENT_FAILURE, package_, message);
  }

  public static void writeFullBackupTransportFailure() {
    android.util.EventLog.writeEvent(FULL_BACKUP_TRANSPORT_FAILURE);
  }

  public static void writeFullBackupSuccess(String package_) {
    android.util.EventLog.writeEvent(FULL_BACKUP_SUCCESS, package_);
  }

  public static void writeFullRestorePackage(String package_) {
    android.util.EventLog.writeEvent(FULL_RESTORE_PACKAGE, package_);
  }

  public static void writeFullBackupQuotaExceeded(String package_) {
    android.util.EventLog.writeEvent(FULL_BACKUP_QUOTA_EXCEEDED, package_);
  }

  public static void writeFullBackupCancelled(String package_, String message) {
    android.util.EventLog.writeEvent(FULL_BACKUP_CANCELLED, package_, message);
  }

  public static void writeBackupTransportLifecycle(String transport, int bound) {
    android.util.EventLog.writeEvent(BACKUP_TRANSPORT_LIFECYCLE, transport, bound);
  }

  public static void writeBackupTransportConnection(String transport, int connected) {
    android.util.EventLog.writeEvent(BACKUP_TRANSPORT_CONNECTION, transport, connected);
  }

  public static void writeBootProgressSystemRun(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_SYSTEM_RUN, time);
  }

  public static void writeSystemServerStart(int startCount, long uptime, long elapseTime) {
    android.util.EventLog.writeEvent(SYSTEM_SERVER_START, startCount, uptime, elapseTime);
  }

  public static void writeBootProgressPmsStart(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_PMS_START, time);
  }

  public static void writeBootProgressPmsSystemScanStart(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, time);
  }

  public static void writeBootProgressPmsDataScanStart(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_PMS_DATA_SCAN_START, time);
  }

  public static void writeBootProgressPmsScanEnd(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_PMS_SCAN_END, time);
  }

  public static void writeBootProgressPmsReady(long time) {
    android.util.EventLog.writeEvent(BOOT_PROGRESS_PMS_READY, time);
  }

  public static void writeUnknownSourcesEnabled(int value) {
    android.util.EventLog.writeEvent(UNKNOWN_SOURCES_ENABLED, value);
  }

  public static void writePmCriticalInfo(String msg) {
    android.util.EventLog.writeEvent(PM_CRITICAL_INFO, msg);
  }

  public static void writePmPackageStats(long manualTime, long quotaTime, long manualData, long quotaData, long manualCache, long quotaCache) {
    android.util.EventLog.writeEvent(PM_PACKAGE_STATS, manualTime, quotaTime, manualData, quotaData, manualCache, quotaCache);
  }

  public static void writePmSnapshotStats(int buildCount, int reuseCount, int bigBuilds, int shortLived, int maxBuildTime, long cummBuildTime) {
    android.util.EventLog.writeEvent(PM_SNAPSHOT_STATS, buildCount, reuseCount, bigBuilds, shortLived, maxBuildTime, cummBuildTime);
  }

  public static void writePmSnapshotRebuild(int buildTime, int lifetime) {
    android.util.EventLog.writeEvent(PM_SNAPSHOT_REBUILD, buildTime, lifetime);
  }

  public static void writePmClearAppDataCaller(int pid, int uid, String package_) {
    android.util.EventLog.writeEvent(PM_CLEAR_APP_DATA_CALLER, pid, uid, package_);
  }

  public static void writeImfForceReconnectIme(Object[] ime, long timeSinceConnect, int showing) {
    android.util.EventLog.writeEvent(IMF_FORCE_RECONNECT_IME, ime, timeSinceConnect, showing);
  }

  public static void writeImfShowIme(String token, String window, String reason, String softinputmode) {
    android.util.EventLog.writeEvent(IMF_SHOW_IME, token, window, reason, softinputmode);
  }

  public static void writeImfHideIme(String token, String window, String reason, String softinputmode) {
    android.util.EventLog.writeEvent(IMF_HIDE_IME, token, window, reason, softinputmode);
  }

  public static void writeWpWallpaperCrashed(String component) {
    android.util.EventLog.writeEvent(WP_WALLPAPER_CRASHED, component);
  }

  public static void writeDeviceIdle(int state, String reason) {
    android.util.EventLog.writeEvent(DEVICE_IDLE, state, reason);
  }

  public static void writeDeviceIdleStep() {
    android.util.EventLog.writeEvent(DEVICE_IDLE_STEP);
  }

  public static void writeDeviceIdleWakeFromIdle(int isIdle, String reason) {
    android.util.EventLog.writeEvent(DEVICE_IDLE_WAKE_FROM_IDLE, isIdle, reason);
  }

  public static void writeDeviceIdleOnStart() {
    android.util.EventLog.writeEvent(DEVICE_IDLE_ON_START);
  }

  public static void writeDeviceIdleOnPhase(String what) {
    android.util.EventLog.writeEvent(DEVICE_IDLE_ON_PHASE, what);
  }

  public static void writeDeviceIdleOnComplete() {
    android.util.EventLog.writeEvent(DEVICE_IDLE_ON_COMPLETE);
  }

  public static void writeDeviceIdleOffStart(String reason) {
    android.util.EventLog.writeEvent(DEVICE_IDLE_OFF_START, reason);
  }

  public static void writeDeviceIdleOffPhase(String what) {
    android.util.EventLog.writeEvent(DEVICE_IDLE_OFF_PHASE, what);
  }

  public static void writeDeviceIdleOffComplete() {
    android.util.EventLog.writeEvent(DEVICE_IDLE_OFF_COMPLETE);
  }

  public static void writeDeviceIdleLight(int state, String reason) {
    android.util.EventLog.writeEvent(DEVICE_IDLE_LIGHT, state, reason);
  }

  public static void writeDeviceIdleLightStep() {
    android.util.EventLog.writeEvent(DEVICE_IDLE_LIGHT_STEP);
  }

  public static void writeAutoBrightnessAdj(float oldLux, float oldBrightness, float newLux, float newBrightness) {
    android.util.EventLog.writeEvent(AUTO_BRIGHTNESS_ADJ, oldLux, oldBrightness, newLux, newBrightness);
  }

  public static void writeInstallerClearAppDataCaller(int pid, int uid, String package_, int flags) {
    android.util.EventLog.writeEvent(INSTALLER_CLEAR_APP_DATA_CALLER, pid, uid, package_, flags);
  }

  public static void writeInstallerClearAppDataCallStack(String method, String class_, String file, int line) {
    android.util.EventLog.writeEvent(INSTALLER_CLEAR_APP_DATA_CALL_STACK, method, class_, file, line);
  }

  public static void writeConnectivityStateChanged(int type, int subtype, int state) {
    android.util.EventLog.writeEvent(CONNECTIVITY_STATE_CHANGED, type, subtype, state);
  }

  public static void writeNetstatsMobileSample(long xtRxBytes, long xtTxBytes, long xtRxPkts, long xtTxPkts, long uidRxBytes, long uidTxBytes, long uidRxPkts, long uidTxPkts, long trustedTime) {
    android.util.EventLog.writeEvent(NETSTATS_MOBILE_SAMPLE, xtRxBytes, xtTxBytes, xtRxPkts, xtTxPkts, uidRxBytes, uidTxBytes, uidRxPkts, uidTxPkts, trustedTime);
  }

  public static void writeNetstatsWifiSample(long xtRxBytes, long xtTxBytes, long xtRxPkts, long xtTxPkts, long uidRxBytes, long uidTxBytes, long uidRxPkts, long uidTxPkts, long trustedTime) {
    android.util.EventLog.writeEvent(NETSTATS_WIFI_SAMPLE, xtRxBytes, xtTxBytes, xtRxPkts, xtTxPkts, uidRxBytes, uidTxBytes, uidRxPkts, uidTxPkts, trustedTime);
  }

  public static void writeLockdownVpnConnecting(int egressNet) {
    android.util.EventLog.writeEvent(LOCKDOWN_VPN_CONNECTING, egressNet);
  }

  public static void writeLockdownVpnConnected(int egressNet) {
    android.util.EventLog.writeEvent(LOCKDOWN_VPN_CONNECTED, egressNet);
  }

  public static void writeLockdownVpnError(int egressNet) {
    android.util.EventLog.writeEvent(LOCKDOWN_VPN_ERROR, egressNet);
  }

  public static void writeConfigInstallFailed(String dir) {
    android.util.EventLog.writeEvent(CONFIG_INSTALL_FAILED, dir);
  }

  public static void writeIfwIntentMatched(int intentType, String componentName, int callerUid, int callerPkgCount, String callerPkgs, String action, String mimeType, String uri, int flags) {
    android.util.EventLog.writeEvent(IFW_INTENT_MATCHED, intentType, componentName, callerUid, callerPkgCount, callerPkgs, action, mimeType, uri, flags);
  }

  public static void writeIdleMaintenanceWindowStart(long time, long lastuseractivity, int batterylevel, int batterycharging) {
    android.util.EventLog.writeEvent(IDLE_MAINTENANCE_WINDOW_START, time, lastuseractivity, batterylevel, batterycharging);
  }

  public static void writeIdleMaintenanceWindowFinish(long time, long lastuseractivity, int batterylevel, int batterycharging) {
    android.util.EventLog.writeEvent(IDLE_MAINTENANCE_WINDOW_FINISH, time, lastuseractivity, batterylevel, batterycharging);
  }

  public static void writeFstrimStart(long time) {
    android.util.EventLog.writeEvent(FSTRIM_START, time);
  }

  public static void writeFstrimFinish(long time) {
    android.util.EventLog.writeEvent(FSTRIM_FINISH, time);
  }

  public static void writeJobDeferredExecution(long time) {
    android.util.EventLog.writeEvent(JOB_DEFERRED_EXECUTION, time);
  }

  public static void writeVolumeChanged(int stream, int prevLevel, int level, int maxLevel, String caller) {
    android.util.EventLog.writeEvent(VOLUME_CHANGED, stream, prevLevel, level, maxLevel, caller);
  }

  public static void writeStreamDevicesChanged(int stream, int prevDevices, int devices) {
    android.util.EventLog.writeEvent(STREAM_DEVICES_CHANGED, stream, prevDevices, devices);
  }

  public static void writeCameraGestureTriggered(long gestureOnTime, long sensor1OnTime, long sensor2OnTime, int eventExtra) {
    android.util.EventLog.writeEvent(CAMERA_GESTURE_TRIGGERED, gestureOnTime, sensor1OnTime, sensor2OnTime, eventExtra);
  }

  public static void writeTimezoneTriggerCheck(String token) {
    android.util.EventLog.writeEvent(TIMEZONE_TRIGGER_CHECK, token);
  }

  public static void writeTimezoneRequestInstall(String token) {
    android.util.EventLog.writeEvent(TIMEZONE_REQUEST_INSTALL, token);
  }

  public static void writeTimezoneInstallStarted(String token) {
    android.util.EventLog.writeEvent(TIMEZONE_INSTALL_STARTED, token);
  }

  public static void writeTimezoneInstallComplete(String token, int result) {
    android.util.EventLog.writeEvent(TIMEZONE_INSTALL_COMPLETE, token, result);
  }

  public static void writeTimezoneRequestUninstall(String token) {
    android.util.EventLog.writeEvent(TIMEZONE_REQUEST_UNINSTALL, token);
  }

  public static void writeTimezoneUninstallStarted(String token) {
    android.util.EventLog.writeEvent(TIMEZONE_UNINSTALL_STARTED, token);
  }

  public static void writeTimezoneUninstallComplete(String token, int result) {
    android.util.EventLog.writeEvent(TIMEZONE_UNINSTALL_COMPLETE, token, result);
  }

  public static void writeTimezoneRequestNothing(String token) {
    android.util.EventLog.writeEvent(TIMEZONE_REQUEST_NOTHING, token);
  }

  public static void writeTimezoneNothingComplete(String token) {
    android.util.EventLog.writeEvent(TIMEZONE_NOTHING_COMPLETE, token);
  }
}
