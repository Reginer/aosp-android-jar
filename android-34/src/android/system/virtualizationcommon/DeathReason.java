/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationcommon;
/** The reason why a VM died. */
public @interface DeathReason {
  /** There was an error waiting for the VM. */
  public static final int INFRASTRUCTURE_ERROR = 0;
  /** The VM was killed. */
  public static final int KILLED = 1;
  /** The VM died for an unknown reason. */
  public static final int UNKNOWN = 2;
  /** The VM requested to shut down. */
  public static final int SHUTDOWN = 3;
  /** crosvm had an error starting the VM. */
  public static final int START_FAILED = 4;
  /** The VM requested to reboot, possibly as the result of a kernel panic. */
  public static final int REBOOT = 5;
  /** The VM or crosvm crashed. */
  public static final int CRASH = 6;
  /** The pVM firmware failed to verify the VM because the public key doesn't match. */
  public static final int PVM_FIRMWARE_PUBLIC_KEY_MISMATCH = 7;
  /** The pVM firmware failed to verify the VM because the instance image changed. */
  public static final int PVM_FIRMWARE_INSTANCE_IMAGE_CHANGED = 8;
  // 9 & 10 intentionally removed.
  /** The microdroid failed to connect to VirtualizationService's RPC server. */
  public static final int MICRODROID_FAILED_TO_CONNECT_TO_VIRTUALIZATION_SERVICE = 11;
  /** The payload for microdroid is changed. */
  public static final int MICRODROID_PAYLOAD_HAS_CHANGED = 12;
  /** The microdroid failed to verify given payload APK. */
  public static final int MICRODROID_PAYLOAD_VERIFICATION_FAILED = 13;
  /** The VM config for microdroid is invalid (e.g. missing tasks). */
  public static final int MICRODROID_INVALID_PAYLOAD_CONFIG = 14;
  /** There was a runtime error while running microdroid manager. */
  public static final int MICRODROID_UNKNOWN_RUNTIME_ERROR = 15;
  /** The VM killed due to hangup */
  public static final int HANGUP = 16;
  /** The VCPU stalled */
  public static final int WATCHDOG_REBOOT = 17;
}
