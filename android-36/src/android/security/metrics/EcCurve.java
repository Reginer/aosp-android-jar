/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/EcCurve.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/EcCurve.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * EcCurve enum as defined in Keystore2KeyCreationWithGeneralInfo of
 * frameworks/proto_logging/stats/atoms.proto.
 * @hide
 */
public @interface EcCurve {
  /** Unspecified takes 0. Other values are incremented by 1 compared to the keymint spec. */
  public static final int EC_CURVE_UNSPECIFIED = 0;
  public static final int P_224 = 1;
  public static final int P_256 = 2;
  public static final int P_384 = 3;
  public static final int P_521 = 4;
  public static final int CURVE_25519 = 5;
}
