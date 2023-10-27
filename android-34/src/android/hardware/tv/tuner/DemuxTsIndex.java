/*
 * This file is auto-generated.  DO NOT MODIFY.
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
