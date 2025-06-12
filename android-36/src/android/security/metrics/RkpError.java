/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/RkpError.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/RkpError.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * KeyOrigin enum as defined in RkpErrorStats of frameworks/proto_logging/stats/atoms.proto.
 * @hide
 */
public @interface RkpError {
  public static final int RKP_ERROR_UNSPECIFIED = 0;
  /** The key pool is out of keys. */
  public static final int OUT_OF_KEYS = 1;
  /** Falling back to factory provisioned keys during hybrid mode. */
  public static final int FALL_BACK_DURING_HYBRID = 2;
}
