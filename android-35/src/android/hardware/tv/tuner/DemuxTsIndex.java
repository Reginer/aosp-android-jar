/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash f8d74c149f04e76b6d622db2bd8e465dae24b08c --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen/android/hardware/tv/tuner/DemuxTsIndex.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2/android/hardware/tv/tuner/DemuxTsIndex.aidl
 */
package android.hardware.tv.tuner;
/** @hide */
public @interface DemuxTsIndex {
  public static final int FIRST_PACKET = 1;
  public static final int PAYLOAD_UNIT_START_INDICATOR = 2;
  public static final int CHANGE_TO_NOT_SCRAMBLED = 4;
  public static final int CHANGE_TO_EVEN_SCRAMBLED = 8;
  public static final int CHANGE_TO_ODD_SCRAMBLED = 16;
  public static final int DISCONTINUITY_INDICATOR = 32;
  public static final int RANDOM_ACCESS_INDICATOR = 64;
  public static final int PRIORITY_INDICATOR = 128;
  public static final int PCR_FLAG = 256;
  public static final int OPCR_FLAG = 512;
  public static final int SPLICING_POINT_FLAG = 1024;
  public static final int PRIVATE_DATA = 2048;
  public static final int ADAPTATION_EXTENSION_FLAG = 4096;
  public static final int MPT_INDEX_MPT = 65536;
  public static final int MPT_INDEX_VIDEO = 131072;
  public static final int MPT_INDEX_AUDIO = 262144;
  public static final int MPT_INDEX_TIMESTAMP_TARGET_VIDEO = 524288;
  public static final int MPT_INDEX_TIMESTAMP_TARGET_AUDIO = 1048576;
}
