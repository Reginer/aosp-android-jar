/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version 29 --ninja -d out/soong/.intermediates/frameworks/av/media/module/libmediatranscoding/mediatranscoding_aidl_interface-java-source/gen/android/media/TranscodingSessionPriority.java.d -o out/soong/.intermediates/frameworks/av/media/module/libmediatranscoding/mediatranscoding_aidl_interface-java-source/gen -Nframeworks/av/media/module/libmediatranscoding/aidl frameworks/av/media/module/libmediatranscoding/aidl/android/media/TranscodingSessionPriority.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/**
 * Priority of a transcoding session.
 * 
 * {@hide}
 */
public @interface TranscodingSessionPriority {
  // TODO(hkuang): define what each priority level actually mean.
  public static final int kUnspecified = 0;
  public static final int kLow = 1;
  /** 2 ~ 20 is reserved for future use. */
  public static final int kNormal = 21;
  /** 22 ~ 30 is reserved for future use. */
  public static final int kHigh = 31;
}
