/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash d111735ed2b89b6c32443aac9b162b1afbbea3f2 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V5-java-source/gen/android/hardware/power/SessionTag.java.d -o out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V5-java-source/gen -Nhardware/interfaces/power/aidl/aidl_api/android.hardware.power/5 hardware/interfaces/power/aidl/aidl_api/android.hardware.power/5/android/hardware/power/SessionTag.aidl
 */
package android.hardware.power;
public @interface SessionTag {
  public static final int OTHER = 0;
  public static final int SURFACEFLINGER = 1;
  public static final int HWUI = 2;
  public static final int GAME = 3;
  public static final int APP = 4;
}
