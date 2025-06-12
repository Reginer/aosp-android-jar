/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/AcquiredInfo.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/AcquiredInfo.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public @interface AcquiredInfo {
  /**
   * Placeholder value used for default initialization of AcquiredInfo. This
   * value means AcquiredInfo wasn't explicitly initialized and must be
   * discarded by the recipient.
   */
  public static final byte UNKNOWN = 0;
  /** A high quality fingerprint image was detected, no further user interaction is necessary. */
  public static final byte GOOD = 1;
  /** Not enough of a fingerprint was detected. Reposition the finger, or a longer swipe needed. */
  public static final byte PARTIAL = 2;
  /** Image doesn't contain enough detail for recognition. */
  public static final byte INSUFFICIENT = 3;
  /** The sensor needs to be cleaned. */
  public static final byte SENSOR_DIRTY = 4;
  /** For swipe-type sensors, the swipe was too slow and not enough data was collected. */
  public static final byte TOO_SLOW = 5;
  /** For swipe-type sensors, the swipe was too fast and not enough data was collected. */
  public static final byte TOO_FAST = 6;
  /**
   * Vendor-specific acquisition message. See ISessionCallback#onAcquired vendorCode
   * documentation.
   */
  public static final byte VENDOR = 7;
  /**
   * This message represents the earliest message sent at the beginning of the authentication
   * pipeline. It is expected to be used to measure latency. For example, in a camera-based
   * authentication system it's expected to be sent prior to camera initialization. Note this
   * should be sent whenever authentication is started or restarted. The framework may measure
   * latency based on the time between the last START message and the onAuthenticated callback.
   */
  public static final byte START = 8;
  /**
   * For sensors that require illumination, such as optical under-display fingerprint sensors,
   * the image was too dark to be used for matching.
   */
  public static final byte TOO_DARK = 9;
  /**
   * For sensors that require illumination, such as optical under-display fingerprint sensors,
   * the image was too bright to be used for matching.
   */
  public static final byte TOO_BRIGHT = 10;
  /**
   * This message may be sent during enrollment if the same area of the finger has already
   * been captured during this enrollment session. In general, enrolling multiple areas of the
   * same finger can help against false rejections.
   */
  public static final byte IMMOBILE = 11;
  /**
   * This message may be sent to notify the framework that an additional image capture is taking
   * place. Multiple RETRYING_CAPTURE may be sent before an ACQUIRED_GOOD message is sent.
   * However, RETRYING_CAPTURE must not be sent after ACQUIRED_GOOD is sent.
   */
  public static final byte RETRYING_CAPTURE = 12;
  /** Fingerprint was lifted before the capture completed. */
  public static final byte LIFT_TOO_SOON = 13;
  /**
   * Indicates a power press event has occurred. This is typically sent by fingerprint
   * sensors that have the sensor co-located with the power button.
   */
  public static final byte POWER_PRESS = 14;
}
