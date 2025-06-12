/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 8a6cd86630181a4df6f20056259ec200ffe39209 -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common-V4-java-source/gen/android/hardware/biometrics/common/WakeReason.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common-V4-java-source/gen -Nhardware/interfaces/biometrics/common/aidl/aidl_api/android.hardware.biometrics.common/4 hardware/interfaces/biometrics/common/aidl/aidl_api/android.hardware.biometrics.common/4/android/hardware/biometrics/common/WakeReason.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.common;
/** @hide */
public @interface WakeReason {
  public static final int UNKNOWN = 0;
  public static final int POWER_BUTTON = 1;
  public static final int GESTURE = 2;
  public static final int WAKE_KEY = 3;
  public static final int WAKE_MOTION = 4;
  public static final int LID = 5;
  public static final int DISPLAY_GROUP_ADDED = 6;
  public static final int TAP = 7;
  public static final int LIFT = 8;
  public static final int BIOMETRIC = 9;
}
