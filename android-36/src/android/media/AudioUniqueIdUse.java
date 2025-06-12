/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl-java-source/gen/android/media/AudioUniqueIdUse.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/AudioUniqueIdUse.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public @interface AudioUniqueIdUse {
  public static final int UNSPECIFIED = 0;
  public static final int SESSION = 1;
  // audio_session_t
  // for allocated sessions, not special AUDIO_SESSION_*
  public static final int MODULE = 2;
  // audio_module_handle_t
  public static final int EFFECT = 3;
  // audio_effect_handle_t
  public static final int PATCH = 4;
  // audio_patch_handle_t
  public static final int OUTPUT = 5;
  // audio_io_handle_t
  public static final int INPUT = 6;
  // audio_io_handle_t
  public static final int CLIENT = 7;
}
