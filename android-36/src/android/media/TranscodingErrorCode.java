/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version 29 --ninja -d out/soong/.intermediates/frameworks/av/media/module/libmediatranscoding/mediatranscoding_aidl_interface-java-source/gen/android/media/TranscodingErrorCode.java.d -o out/soong/.intermediates/frameworks/av/media/module/libmediatranscoding/mediatranscoding_aidl_interface-java-source/gen -Nframeworks/av/media/module/libmediatranscoding/aidl frameworks/av/media/module/libmediatranscoding/aidl/android/media/TranscodingErrorCode.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/**
 * Type enums of video transcoding errors.
 * 
 * {@hide}
 */
public @interface TranscodingErrorCode {
  // Errors exposed to client side.
  public static final int kNoError = 0;
  public static final int kDroppedByService = 1;
  public static final int kServiceUnavailable = 2;
  // Other private errors.
  public static final int kPrivateErrorFirst = 1000;
  public static final int kUnknown = 1000;
  public static final int kMalformed = 1001;
  public static final int kUnsupported = 1002;
  public static final int kInvalidParameter = 1003;
  public static final int kInvalidOperation = 1004;
  public static final int kErrorIO = 1005;
  public static final int kInsufficientResources = 1006;
  public static final int kWatchdogTimeout = 1007;
  public static final int kUidGoneCancelled = 1008;
}
