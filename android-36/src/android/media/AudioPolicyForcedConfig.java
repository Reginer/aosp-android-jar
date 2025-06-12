/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl-java-source/gen/android/media/AudioPolicyForcedConfig.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/AudioPolicyForcedConfig.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public @interface AudioPolicyForcedConfig {
  public static final int NONE = 0;
  public static final int SPEAKER = 1;
  public static final int HEADPHONES = 2;
  public static final int BT_SCO = 3;
  public static final int BT_A2DP = 4;
  public static final int WIRED_ACCESSORY = 5;
  public static final int BT_CAR_DOCK = 6;
  public static final int BT_DESK_DOCK = 7;
  public static final int ANALOG_DOCK = 8;
  public static final int DIGITAL_DOCK = 9;
  public static final int NO_BT_A2DP = 10;
  /** A2DP sink is not preferred to speaker or wired HS */
  public static final int SYSTEM_ENFORCED = 11;
  public static final int HDMI_SYSTEM_AUDIO_ENFORCED = 12;
  public static final int ENCODED_SURROUND_NEVER = 13;
  public static final int ENCODED_SURROUND_ALWAYS = 14;
  public static final int ENCODED_SURROUND_MANUAL = 15;
  public static final int BT_BLE = 16;
}
