/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl-java-source/gen/android/media/AudioIoConfigEvent.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/AudioIoConfigEvent.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public @interface AudioIoConfigEvent {
  public static final int OUTPUT_REGISTERED = 0;
  public static final int OUTPUT_OPENED = 1;
  public static final int OUTPUT_CLOSED = 2;
  public static final int OUTPUT_CONFIG_CHANGED = 3;
  public static final int INPUT_REGISTERED = 4;
  public static final int INPUT_OPENED = 5;
  public static final int INPUT_CLOSED = 6;
  public static final int INPUT_CONFIG_CHANGED = 7;
  public static final int CLIENT_STARTED = 8;
}
