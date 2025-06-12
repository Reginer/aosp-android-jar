/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash c43fbb9be4a662cc9ace640dba21cccdb84c6c21 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen/android/hardware/biometrics/face/AcquiredInfo.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen -Iframeworks/native/aidl/gui -Nhardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4 hardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4/android/hardware/biometrics/face/AcquiredInfo.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.face;
/** @hide */
public @interface AcquiredInfo {
  public static final byte UNKNOWN = 0;
  public static final byte GOOD = 1;
  public static final byte INSUFFICIENT = 2;
  public static final byte TOO_BRIGHT = 3;
  public static final byte TOO_DARK = 4;
  public static final byte TOO_CLOSE = 5;
  public static final byte TOO_FAR = 6;
  public static final byte FACE_TOO_HIGH = 7;
  public static final byte FACE_TOO_LOW = 8;
  public static final byte FACE_TOO_RIGHT = 9;
  public static final byte FACE_TOO_LEFT = 10;
  public static final byte POOR_GAZE = 11;
  public static final byte NOT_DETECTED = 12;
  public static final byte TOO_MUCH_MOTION = 13;
  public static final byte RECALIBRATE = 14;
  public static final byte TOO_DIFFERENT = 15;
  public static final byte TOO_SIMILAR = 16;
  public static final byte PAN_TOO_EXTREME = 17;
  public static final byte TILT_TOO_EXTREME = 18;
  public static final byte ROLL_TOO_EXTREME = 19;
  public static final byte FACE_OBSCURED = 20;
  public static final byte START = 21;
  public static final byte SENSOR_DIRTY = 22;
  public static final byte VENDOR = 23;
  public static final byte FIRST_FRAME_RECEIVED = 24;
  public static final byte DARK_GLASSES_DETECTED = 25;
  public static final byte MOUTH_COVERING_DETECTED = 26;
}
