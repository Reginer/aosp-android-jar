/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 0c86a38729dd5d560fe3a0eca6aa9d8cf83efb00 --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen/android/media/audio/common/AudioProductStrategyType.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3/android/media/audio/common/AudioProductStrategyType.aidl
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
