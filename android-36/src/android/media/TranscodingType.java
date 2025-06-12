/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version 29 --ninja -d out/soong/.intermediates/frameworks/av/media/module/libmediatranscoding/mediatranscoding_aidl_interface-java-source/gen/android/media/TranscodingType.java.d -o out/soong/.intermediates/frameworks/av/media/module/libmediatranscoding/mediatranscoding_aidl_interface-java-source/gen -Nframeworks/av/media/module/libmediatranscoding/aidl frameworks/av/media/module/libmediatranscoding/aidl/android/media/TranscodingType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/**
 * Type of transcoding.
 * 
 * {@hide}
 */
public @interface TranscodingType {
  public static final int kUnknown = 0;
  public static final int kVideoTranscoding = 1;
  public static final int kImageTranscoding = 2;
}
