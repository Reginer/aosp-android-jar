/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 0c86a38729dd5d560fe3a0eca6aa9d8cf83efb00 --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen/android/media/audio/common/AudioMode.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3/android/media/audio/common/AudioMode.aidl
 */
package android.media.audio.common;
/** @hide */
public @interface AudioMode {
  public static final int SYS_RESERVED_INVALID = -2;
  public static final int SYS_RESERVED_CURRENT = -1;
  public static final int NORMAL = 0;
  public static final int RINGTONE = 1;
  public static final int IN_CALL = 2;
  public static final int IN_COMMUNICATION = 3;
  public static final int CALL_SCREEN = 4;
  public static final int SYS_RESERVED_CALL_REDIRECT = 5;
  public static final int SYS_RESERVED_COMMUNICATION_REDIRECT = 6;
}
