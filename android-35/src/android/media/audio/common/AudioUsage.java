/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 0c86a38729dd5d560fe3a0eca6aa9d8cf83efb00 --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen/android/media/audio/common/AudioUsage.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V3-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/3/android/media/audio/common/AudioUsage.aidl
 */
package android.media.audio.common;
/** @hide */
public @interface AudioUsage {
  public static final int INVALID = -1;
  public static final int UNKNOWN = 0;
  public static final int MEDIA = 1;
  public static final int VOICE_COMMUNICATION = 2;
  public static final int VOICE_COMMUNICATION_SIGNALLING = 3;
  public static final int ALARM = 4;
  public static final int NOTIFICATION = 5;
  public static final int NOTIFICATION_TELEPHONY_RINGTONE = 6;
  public static final int SYS_RESERVED_NOTIFICATION_COMMUNICATION_REQUEST = 7;
  public static final int SYS_RESERVED_NOTIFICATION_COMMUNICATION_INSTANT = 8;
  public static final int SYS_RESERVED_NOTIFICATION_COMMUNICATION_DELAYED = 9;
  public static final int NOTIFICATION_EVENT = 10;
  public static final int ASSISTANCE_ACCESSIBILITY = 11;
  public static final int ASSISTANCE_NAVIGATION_GUIDANCE = 12;
  public static final int ASSISTANCE_SONIFICATION = 13;
  public static final int GAME = 14;
  public static final int VIRTUAL_SOURCE = 15;
  public static final int ASSISTANT = 16;
  public static final int CALL_ASSISTANT = 17;
  public static final int EMERGENCY = 1000;
  public static final int SAFETY = 1001;
  public static final int VEHICLE_STATUS = 1002;
  public static final int ANNOUNCEMENT = 1003;
}
