/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash d111735ed2b89b6c32443aac9b162b1afbbea3f2 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V5-java-source/gen/android/hardware/power/SessionHint.java.d -o out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V5-java-source/gen -Nhardware/interfaces/power/aidl/aidl_api/android.hardware.power/5 hardware/interfaces/power/aidl/aidl_api/android.hardware.power/5/android/hardware/power/SessionHint.aidl
 */
package android.hardware.power;
public @interface SessionHint {
  public static final int CPU_LOAD_UP = 0;
  public static final int CPU_LOAD_DOWN = 1;
  public static final int CPU_LOAD_RESET = 2;
  public static final int CPU_LOAD_RESUME = 3;
  public static final int POWER_EFFICIENCY = 4;
  public static final int GPU_LOAD_UP = 5;
  public static final int GPU_LOAD_DOWN = 6;
  public static final int GPU_LOAD_RESET = 7;
}
