/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/av/audio-permission-aidl-java-source/gen/com/android/media/permission/PermissionEnum.java.d -o out/soong/.intermediates/frameworks/av/audio-permission-aidl-java-source/gen -Nframeworks/av/aidl frameworks/av/aidl/com/android/media/permission/PermissionEnum.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.android.media.permission;
/**
 * Enumerates permissions which are tracked/pushed by NativePermissionController
 * {@hide}
 */
public @interface PermissionEnum {
  // This is a runtime + WIU permission, which means data delivery should be protected by AppOps
  // We query the controller only for early fails/hard errors
  public static final byte RECORD_AUDIO = 0;
  public static final byte MODIFY_AUDIO_ROUTING = 1;
  public static final byte MODIFY_AUDIO_SETTINGS = 2;
  public static final byte MODIFY_PHONE_STATE = 3;
  public static final byte MODIFY_DEFAULT_AUDIO_EFFECTS = 4;
  public static final byte WRITE_SECURE_SETTINGS = 5;
  public static final byte CALL_AUDIO_INTERCEPTION = 6;
  public static final byte ACCESS_ULTRASOUND = 7;
  public static final byte CAPTURE_AUDIO_OUTPUT = 8;
  public static final byte CAPTURE_MEDIA_OUTPUT = 9;
  public static final byte CAPTURE_AUDIO_HOTWORD = 10;
  public static final byte CAPTURE_TUNER_AUDIO_INPUT = 11;
  public static final byte CAPTURE_VOICE_COMMUNICATION_OUTPUT = 12;
  public static final byte BLUETOOTH_CONNECT = 13;
  public static final byte BYPASS_CONCURRENT_RECORD_AUDIO_RESTRICTION = 14;
  public static final byte MODIFY_AUDIO_SETTINGS_PRIVILEGED = 15;
  public static final byte ENUM_SIZE = 16;
}
