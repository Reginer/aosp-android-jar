/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl-java-source/gen/android/media/AudioPolicyForceUse.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/AudioPolicyForceUse.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public @interface AudioPolicyForceUse {
  public static final int COMMUNICATION = 0;
  public static final int MEDIA = 1;
  public static final int RECORD = 2;
  public static final int DOCK = 3;
  public static final int SYSTEM = 4;
  public static final int HDMI_SYSTEM_AUDIO = 5;
  public static final int ENCODED_SURROUND = 6;
  public static final int VIBRATE_RINGING = 7;
}
