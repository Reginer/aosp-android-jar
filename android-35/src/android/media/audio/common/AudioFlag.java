/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 0c86a38729dd5d560fe3a0eca6aa9d8cf83efb00 --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen/android/media/audio/common/AudioFlag.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3/android/media/audio/common/AudioFlag.aidl
 */
package android.media.audio.common;
/** @hide */
public @interface AudioFlag {
  public static final int NONE = 0;
  public static final int AUDIBILITY_ENFORCED = 1;
  public static final int SCO = 4;
  public static final int BEACON = 8;
  public static final int HW_AV_SYNC = 16;
  public static final int HW_HOTWORD = 32;
  public static final int BYPASS_INTERRUPTION_POLICY = 64;
  public static final int BYPASS_MUTE = 128;
  public static final int LOW_LATENCY = 256;
  public static final int DEEP_BUFFER = 512;
  public static final int NO_MEDIA_PROJECTION = 1024;
  public static final int MUTE_HAPTIC = 2048;
  public static final int NO_SYSTEM_CAPTURE = 4096;
  public static final int CAPTURE_PRIVATE = 8192;
  public static final int CONTENT_SPATIALIZED = 16384;
  public static final int NEVER_SPATIALIZE = 32768;
  public static final int CALL_REDIRECTION = 65536;
}
