/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash f8d74c149f04e76b6d622db2bd8e465dae24b08c --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen/android/hardware/tv/tuner/FrontendDvbsModulation.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2/android/hardware/tv/tuner/FrontendDvbsModulation.aidl
 */
package android.hardware.tv.tuner;
/** @hide */
public @interface FrontendDvbsModulation {
  public static final int UNDEFINED = 0;
  public static final int AUTO = 1;
  public static final int MOD_QPSK = 2;
  public static final int MOD_8PSK = 4;
  public static final int MOD_16QAM = 8;
  public static final int MOD_16PSK = 16;
  public static final int MOD_32PSK = 32;
  public static final int MOD_ACM = 64;
  public static final int MOD_8APSK = 128;
  public static final int MOD_16APSK = 256;
  public static final int MOD_32APSK = 512;
  public static final int MOD_64APSK = 1024;
  public static final int MOD_128APSK = 2048;
  public static final int MOD_256APSK = 4096;
  public static final int MOD_RESERVED = 8192;
}