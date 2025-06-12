/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/FingerprintSensorType.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/FingerprintSensorType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public @interface FingerprintSensorType {
  public static final byte UNKNOWN = 0;
  public static final byte REAR = 1;
  public static final byte UNDER_DISPLAY_ULTRASONIC = 2;
  public static final byte UNDER_DISPLAY_OPTICAL = 3;
  public static final byte POWER_BUTTON = 4;
  public static final byte HOME_BUTTON = 5;
}
