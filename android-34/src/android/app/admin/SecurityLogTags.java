/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/android/app/admin/SecurityLogTags.logtags
 */

package android.app.admin;

/**
 * @hide
 */
public class SecurityLogTags {
  private SecurityLogTags() { }  // don't instantiate

  /** 210001 security_adb_shell_interactive */
  public static final int SECURITY_ADB_SHELL_INTERACTIVE = 210001;

  /** 210002 security_adb_shell_command (command|3) */
  public static final int SECURITY_ADB_SHELL_COMMAND = 210002;

  /** 210003 security_adb_sync_recv (path|3) */
  public static final int SECURITY_ADB_SYNC_RECV = 210003;

  /** 210004 security_adb_sync_send (path|3) */
  public static final int SECURITY_ADB_SYNC_SEND = 210004;

  /** 210005 security_app_process_start (process|3),(start_time|2|3),(uid|1),(pid|1),(seinfo|3),(sha256|3) */
  public static final int SECURITY_APP_PROCESS_START = 210005;

  /** 210006 security_keyguard_dismissed */
  public static final int SECURITY_KEYGUARD_DISMISSED = 210006;

  /** 210007 security_keyguard_dismiss_auth_attempt (success|1),(method_strength|1) */
  public static final int SECURITY_KEYGUARD_DISMISS_AUTH_ATTEMPT = 210007;

  /** 210008 security_keyguard_secured */
  public static final int SECURITY_KEYGUARD_SECURED = 210008;

  /** 210009 security_os_startup (boot_state|3),(verity_mode|3) */
  public static final int SECURITY_OS_STARTUP = 210009;

  /** 210010 security_os_shutdown */
  public static final int SECURITY_OS_SHUTDOWN = 210010;

  /** 210011 security_logging_started */
  public static final int SECURITY_LOGGING_STARTED = 210011;

  /** 210012 security_logging_stopped */
  public static final int SECURITY_LOGGING_STOPPED = 210012;

  /** 210013 security_media_mounted (path|3),(label|3) */
  public static final int SECURITY_MEDIA_MOUNTED = 210013;

  /** 210014 security_media_unmounted (path|3),(label|3) */
  public static final int SECURITY_MEDIA_UNMOUNTED = 210014;

  /** 210015 security_log_buffer_size_critical */
  public static final int SECURITY_LOG_BUFFER_SIZE_CRITICAL = 210015;

  /** 210016 security_password_expiration_set (package|3),(admin_user|1),(target_user|1),(timeout|2|3) */
  public static final int SECURITY_PASSWORD_EXPIRATION_SET = 210016;

  /** 210017 security_password_complexity_set (package|3),(admin_user|1),(target_user|1),(length|1),(quality|1),(num_letters|1),(num_non_letters|1),(num_numeric|1),(num_uppercase|1),(num_lowercase|1),(num_symbols|1) */
  public static final int SECURITY_PASSWORD_COMPLEXITY_SET = 210017;

  /** 210018 security_password_history_length_set (package|3),(admin_user|1),(target_user|1),(length|1) */
  public static final int SECURITY_PASSWORD_HISTORY_LENGTH_SET = 210018;

  /** 210019 security_max_screen_lock_timeout_set (package|3),(admin_user|1),(target_user|1),(timeout|2|3) */
  public static final int SECURITY_MAX_SCREEN_LOCK_TIMEOUT_SET = 210019;

  /** 210020 security_max_password_attempts_set (package|3),(admin_user|1),(target_user|1),(num_failures|1) */
  public static final int SECURITY_MAX_PASSWORD_ATTEMPTS_SET = 210020;

  /** 210021 security_keyguard_disabled_features_set (package|3),(admin_user|1),(target_user|1),(features|1) */
  public static final int SECURITY_KEYGUARD_DISABLED_FEATURES_SET = 210021;

  /** 210022 security_remote_lock (package|3),(admin_user|1),(target_user|1) */
  public static final int SECURITY_REMOTE_LOCK = 210022;

  /** 210023 security_wipe_failed (package|3),(admin_user|1) */
  public static final int SECURITY_WIPE_FAILED = 210023;

  /** 210024 security_key_generated (success|1),(key_id|3),(uid|1) */
  public static final int SECURITY_KEY_GENERATED = 210024;

  /** 210025 security_key_imported (success|1),(key_id|3),(uid|1) */
  public static final int SECURITY_KEY_IMPORTED = 210025;

  /** 210026 security_key_destroyed (success|1),(key_id|3),(uid|1) */
  public static final int SECURITY_KEY_DESTROYED = 210026;

  /** 210027 security_user_restriction_added (package|3),(admin_user|1),(restriction|3) */
  public static final int SECURITY_USER_RESTRICTION_ADDED = 210027;

  /** 210028 security_user_restriction_removed (package|3),(admin_user|1),(restriction|3) */
  public static final int SECURITY_USER_RESTRICTION_REMOVED = 210028;

  /** 210029 security_cert_authority_installed (success|1),(subject|3),(target_user|1) */
  public static final int SECURITY_CERT_AUTHORITY_INSTALLED = 210029;

  /** 210030 security_cert_authority_removed (success|1),(subject|3),(target_user|1) */
  public static final int SECURITY_CERT_AUTHORITY_REMOVED = 210030;

  /** 210031 security_crypto_self_test_completed (success|1) */
  public static final int SECURITY_CRYPTO_SELF_TEST_COMPLETED = 210031;

  /** 210032 security_key_integrity_violation (key_id|3),(uid|1) */
  public static final int SECURITY_KEY_INTEGRITY_VIOLATION = 210032;

  /** 210033 security_cert_validation_failure (reason|3) */
  public static final int SECURITY_CERT_VALIDATION_FAILURE = 210033;

  /** 210034 security_camera_policy_set (package|3),(admin_user|1),(target_user|1),(disabled|1) */
  public static final int SECURITY_CAMERA_POLICY_SET = 210034;

  /** 210035 security_password_complexity_required (package|3),(admin_user|1),(target_user|1),(complexity|1) */
  public static final int SECURITY_PASSWORD_COMPLEXITY_REQUIRED = 210035;

  /** 210036 security_password_changed (password_complexity|1),(target_user|1) */
  public static final int SECURITY_PASSWORD_CHANGED = 210036;

  /** 210037 security_wifi_connection (bssid|3),(event_type|3),(reason|3) */
  public static final int SECURITY_WIFI_CONNECTION = 210037;

  /** 210038 security_wifi_disconnection (bssid|3),(reason|3) */
  public static final int SECURITY_WIFI_DISCONNECTION = 210038;

  /** 210039 security_bluetooth_connection (addr|3),(success|1),(reason|3) */
  public static final int SECURITY_BLUETOOTH_CONNECTION = 210039;

  /** 210040 security_bluetooth_disconnection (addr|3),(reason|3) */
  public static final int SECURITY_BLUETOOTH_DISCONNECTION = 210040;

  /** 210041 security_package_installed (package_name|3),(version_code|1),(user_id|1) */
  public static final int SECURITY_PACKAGE_INSTALLED = 210041;

  /** 210042 security_package_updated (package_name|3),(version_code|1),(user_id|1) */
  public static final int SECURITY_PACKAGE_UPDATED = 210042;

  /** 210043 security_package_uninstalled (package_name|3),(version_code|1),(user_id|1) */
  public static final int SECURITY_PACKAGE_UNINSTALLED = 210043;

  public static void writeSecurityAdbShellInteractive() {
    android.util.EventLog.writeEvent(SECURITY_ADB_SHELL_INTERACTIVE);
  }

  public static void writeSecurityAdbShellCommand(String command) {
    android.util.EventLog.writeEvent(SECURITY_ADB_SHELL_COMMAND, command);
  }

  public static void writeSecurityAdbSyncRecv(String path) {
    android.util.EventLog.writeEvent(SECURITY_ADB_SYNC_RECV, path);
  }

  public static void writeSecurityAdbSyncSend(String path) {
    android.util.EventLog.writeEvent(SECURITY_ADB_SYNC_SEND, path);
  }

  public static void writeSecurityAppProcessStart(String process, long startTime, int uid, int pid, String seinfo, String sha256) {
    android.util.EventLog.writeEvent(SECURITY_APP_PROCESS_START, process, startTime, uid, pid, seinfo, sha256);
  }

  public static void writeSecurityKeyguardDismissed() {
    android.util.EventLog.writeEvent(SECURITY_KEYGUARD_DISMISSED);
  }

  public static void writeSecurityKeyguardDismissAuthAttempt(int success, int methodStrength) {
    android.util.EventLog.writeEvent(SECURITY_KEYGUARD_DISMISS_AUTH_ATTEMPT, success, methodStrength);
  }

  public static void writeSecurityKeyguardSecured() {
    android.util.EventLog.writeEvent(SECURITY_KEYGUARD_SECURED);
  }

  public static void writeSecurityOsStartup(String bootState, String verityMode) {
    android.util.EventLog.writeEvent(SECURITY_OS_STARTUP, bootState, verityMode);
  }

  public static void writeSecurityOsShutdown() {
    android.util.EventLog.writeEvent(SECURITY_OS_SHUTDOWN);
  }

  public static void writeSecurityLoggingStarted() {
    android.util.EventLog.writeEvent(SECURITY_LOGGING_STARTED);
  }

  public static void writeSecurityLoggingStopped() {
    android.util.EventLog.writeEvent(SECURITY_LOGGING_STOPPED);
  }

  public static void writeSecurityMediaMounted(String path, String label) {
    android.util.EventLog.writeEvent(SECURITY_MEDIA_MOUNTED, path, label);
  }

  public static void writeSecurityMediaUnmounted(String path, String label) {
    android.util.EventLog.writeEvent(SECURITY_MEDIA_UNMOUNTED, path, label);
  }

  public static void writeSecurityLogBufferSizeCritical() {
    android.util.EventLog.writeEvent(SECURITY_LOG_BUFFER_SIZE_CRITICAL);
  }

  public static void writeSecurityPasswordExpirationSet(String package_, int adminUser, int targetUser, long timeout) {
    android.util.EventLog.writeEvent(SECURITY_PASSWORD_EXPIRATION_SET, package_, adminUser, targetUser, timeout);
  }

  public static void writeSecurityPasswordComplexitySet(String package_, int adminUser, int targetUser, int length, int quality, int numLetters, int numNonLetters, int numNumeric, int numUppercase, int numLowercase, int numSymbols) {
    android.util.EventLog.writeEvent(SECURITY_PASSWORD_COMPLEXITY_SET, package_, adminUser, targetUser, length, quality, numLetters, numNonLetters, numNumeric, numUppercase, numLowercase, numSymbols);
  }

  public static void writeSecurityPasswordHistoryLengthSet(String package_, int adminUser, int targetUser, int length) {
    android.util.EventLog.writeEvent(SECURITY_PASSWORD_HISTORY_LENGTH_SET, package_, adminUser, targetUser, length);
  }

  public static void writeSecurityMaxScreenLockTimeoutSet(String package_, int adminUser, int targetUser, long timeout) {
    android.util.EventLog.writeEvent(SECURITY_MAX_SCREEN_LOCK_TIMEOUT_SET, package_, adminUser, targetUser, timeout);
  }

  public static void writeSecurityMaxPasswordAttemptsSet(String package_, int adminUser, int targetUser, int numFailures) {
    android.util.EventLog.writeEvent(SECURITY_MAX_PASSWORD_ATTEMPTS_SET, package_, adminUser, targetUser, numFailures);
  }

  public static void writeSecurityKeyguardDisabledFeaturesSet(String package_, int adminUser, int targetUser, int features) {
    android.util.EventLog.writeEvent(SECURITY_KEYGUARD_DISABLED_FEATURES_SET, package_, adminUser, targetUser, features);
  }

  public static void writeSecurityRemoteLock(String package_, int adminUser, int targetUser) {
    android.util.EventLog.writeEvent(SECURITY_REMOTE_LOCK, package_, adminUser, targetUser);
  }

  public static void writeSecurityWipeFailed(String package_, int adminUser) {
    android.util.EventLog.writeEvent(SECURITY_WIPE_FAILED, package_, adminUser);
  }

  public static void writeSecurityKeyGenerated(int success, String keyId, int uid) {
    android.util.EventLog.writeEvent(SECURITY_KEY_GENERATED, success, keyId, uid);
  }

  public static void writeSecurityKeyImported(int success, String keyId, int uid) {
    android.util.EventLog.writeEvent(SECURITY_KEY_IMPORTED, success, keyId, uid);
  }

  public static void writeSecurityKeyDestroyed(int success, String keyId, int uid) {
    android.util.EventLog.writeEvent(SECURITY_KEY_DESTROYED, success, keyId, uid);
  }

  public static void writeSecurityUserRestrictionAdded(String package_, int adminUser, String restriction) {
    android.util.EventLog.writeEvent(SECURITY_USER_RESTRICTION_ADDED, package_, adminUser, restriction);
  }

  public static void writeSecurityUserRestrictionRemoved(String package_, int adminUser, String restriction) {
    android.util.EventLog.writeEvent(SECURITY_USER_RESTRICTION_REMOVED, package_, adminUser, restriction);
  }

  public static void writeSecurityCertAuthorityInstalled(int success, String subject, int targetUser) {
    android.util.EventLog.writeEvent(SECURITY_CERT_AUTHORITY_INSTALLED, success, subject, targetUser);
  }

  public static void writeSecurityCertAuthorityRemoved(int success, String subject, int targetUser) {
    android.util.EventLog.writeEvent(SECURITY_CERT_AUTHORITY_REMOVED, success, subject, targetUser);
  }

  public static void writeSecurityCryptoSelfTestCompleted(int success) {
    android.util.EventLog.writeEvent(SECURITY_CRYPTO_SELF_TEST_COMPLETED, success);
  }

  public static void writeSecurityKeyIntegrityViolation(String keyId, int uid) {
    android.util.EventLog.writeEvent(SECURITY_KEY_INTEGRITY_VIOLATION, keyId, uid);
  }

  public static void writeSecurityCertValidationFailure(String reason) {
    android.util.EventLog.writeEvent(SECURITY_CERT_VALIDATION_FAILURE, reason);
  }

  public static void writeSecurityCameraPolicySet(String package_, int adminUser, int targetUser, int disabled) {
    android.util.EventLog.writeEvent(SECURITY_CAMERA_POLICY_SET, package_, adminUser, targetUser, disabled);
  }

  public static void writeSecurityPasswordComplexityRequired(String package_, int adminUser, int targetUser, int complexity) {
    android.util.EventLog.writeEvent(SECURITY_PASSWORD_COMPLEXITY_REQUIRED, package_, adminUser, targetUser, complexity);
  }

  public static void writeSecurityPasswordChanged(int passwordComplexity, int targetUser) {
    android.util.EventLog.writeEvent(SECURITY_PASSWORD_CHANGED, passwordComplexity, targetUser);
  }

  public static void writeSecurityWifiConnection(String bssid, String eventType, String reason) {
    android.util.EventLog.writeEvent(SECURITY_WIFI_CONNECTION, bssid, eventType, reason);
  }

  public static void writeSecurityWifiDisconnection(String bssid, String reason) {
    android.util.EventLog.writeEvent(SECURITY_WIFI_DISCONNECTION, bssid, reason);
  }

  public static void writeSecurityBluetoothConnection(String addr, int success, String reason) {
    android.util.EventLog.writeEvent(SECURITY_BLUETOOTH_CONNECTION, addr, success, reason);
  }

  public static void writeSecurityBluetoothDisconnection(String addr, String reason) {
    android.util.EventLog.writeEvent(SECURITY_BLUETOOTH_DISCONNECTION, addr, reason);
  }

  public static void writeSecurityPackageInstalled(String packageName, int versionCode, int userId) {
    android.util.EventLog.writeEvent(SECURITY_PACKAGE_INSTALLED, packageName, versionCode, userId);
  }

  public static void writeSecurityPackageUpdated(String packageName, int versionCode, int userId) {
    android.util.EventLog.writeEvent(SECURITY_PACKAGE_UPDATED, packageName, versionCode, userId);
  }

  public static void writeSecurityPackageUninstalled(String packageName, int versionCode, int userId) {
    android.util.EventLog.writeEvent(SECURITY_PACKAGE_UNINSTALLED, packageName, versionCode, userId);
  }
}
