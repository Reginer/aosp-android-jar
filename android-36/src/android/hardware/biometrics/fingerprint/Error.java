/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/Error.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/Error.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public @interface Error {
  /**
   * Placeholder value used for default initialization of Error. This value
   * means Error wasn't explicitly initialized and must be discarded by the
   * recipient.
   */
  public static final byte UNKNOWN = 0;
  /**
   * A hardware error has occurred that cannot be resolved. For example, I2C failure or a broken
   * sensor.
   */
  public static final byte HW_UNAVAILABLE = 1;
  /**
   * The implementation is unable to process the request. For example, invalid arguments were
   * supplied.
   */
  public static final byte UNABLE_TO_PROCESS = 2;
  /** The current operation took too long to complete. */
  public static final byte TIMEOUT = 3;
  /** No space available to store additional enrollments. */
  public static final byte NO_SPACE = 4;
  /** The operation was canceled. See common::ICancellationSignal. */
  public static final byte CANCELED = 5;
  /**
   * The implementation was unable to remove an enrollment.
   * See ISession#removeEnrollments.
   */
  public static final byte UNABLE_TO_REMOVE = 6;
  /** Used to enable vendor-specific error messages. */
  public static final byte VENDOR = 7;
  /** There's a problem with the sensor's calibration. */
  public static final byte BAD_CALIBRATION = 8;
  /**
   * Indicates a power press event has occurred. This is typically sent by fingerprint
   * sensors that have the sensor co-located with the power button.
   */
  public static final byte POWER_PRESS = 9;
}
