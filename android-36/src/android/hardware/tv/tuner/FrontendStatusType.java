/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/FrontendStatusType.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/FrontendStatusType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public @interface FrontendStatusType {
  public static final int DEMOD_LOCK = 0;
  public static final int SNR = 1;
  public static final int BER = 2;
  public static final int PER = 3;
  public static final int PRE_BER = 4;
  public static final int SIGNAL_QUALITY = 5;
  public static final int SIGNAL_STRENGTH = 6;
  public static final int SYMBOL_RATE = 7;
  public static final int FEC = 8;
  public static final int MODULATION = 9;
  public static final int SPECTRAL = 10;
  public static final int LNB_VOLTAGE = 11;
  public static final int PLP_ID = 12;
  public static final int EWBS = 13;
  public static final int AGC = 14;
  public static final int LNA = 15;
  public static final int LAYER_ERROR = 16;
  public static final int MER = 17;
  public static final int FREQ_OFFSET = 18;
  public static final int HIERARCHY = 19;
  public static final int RF_LOCK = 20;
  public static final int ATSC3_PLP_INFO = 21;
  public static final int MODULATIONS = 22;
  public static final int BERS = 23;
  public static final int CODERATES = 24;
  public static final int BANDWIDTH = 25;
  public static final int GUARD_INTERVAL = 26;
  public static final int TRANSMISSION_MODE = 27;
  public static final int UEC = 28;
  public static final int T2_SYSTEM_ID = 29;
  public static final int INTERLEAVINGS = 30;
  public static final int ISDBT_SEGMENTS = 31;
  public static final int TS_DATA_RATES = 32;
  public static final int ROLL_OFF = 33;
  public static final int IS_MISO = 34;
  public static final int IS_LINEAR = 35;
  public static final int IS_SHORT_FRAMES = 36;
  public static final int ISDBT_MODE = 37;
  public static final int ISDBT_PARTIAL_RECEPTION_FLAG = 38;
  public static final int STREAM_ID_LIST = 39;
  public static final int DVBT_CELL_IDS = 40;
  public static final int ATSC3_ALL_PLP_INFO = 41;
  public static final int IPTV_CONTENT_URL = 42;
  public static final int IPTV_PACKETS_LOST = 43;
  public static final int IPTV_PACKETS_RECEIVED = 44;
  public static final int IPTV_WORST_JITTER_MS = 45;
  public static final int IPTV_AVERAGE_JITTER_MS = 46;
  public static final int STANDARD_EXT = 47;
}
