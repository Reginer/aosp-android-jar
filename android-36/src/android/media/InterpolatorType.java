/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/av-types-aidl-java-source/gen/android/media/InterpolatorType.java.d -o out/soong/.intermediates/frameworks/av/av-types-aidl-java-source/gen -Nframeworks/av/aidl frameworks/av/aidl/android/media/InterpolatorType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/**
 * Polynomial spline interpolators.
 * 
 * {@hide}
 */
public @interface InterpolatorType {
  /** Not continuous. */
  public static final int STEP = 0;
  /** C0. */
  public static final int LINEAR = 1;
  /** C1. */
  public static final int CUBIC = 2;
  /** C1 (to provide locally monotonic curves). */
  public static final int CUBIC_MONOTONIC = 3;
}
