/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/AudioProductStrategyType.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/AudioProductStrategyType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public @interface AudioProductStrategyType {
  public static final byte SYS_RESERVED_NONE = -1;
  public static final byte MEDIA = 0;
  public static final byte PHONE = 1;
  public static final byte SONIFICATION = 2;
  public static final byte SONIFICATION_RESPECTFUL = 3;
  public static final byte DTMF = 4;
  public static final byte ENFORCED_AUDIBLE = 5;
  public static final byte TRANSMITTED_THROUGH_SPEAKER = 6;
  public static final byte ACCESSIBILITY = 7;
  public static final byte SYS_RESERVED_REROUTING = 8;
  public static final byte SYS_RESERVED_CALL_ASSISTANT = 9;
}
