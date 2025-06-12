/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/Algorithm.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/Algorithm.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * Algorithm enum as defined in stats/enums/system/security/keystore2/enums.proto.
 * @hide
 */
public @interface Algorithm {
  /** ALGORITHM is prepended because UNSPECIFIED exists in other enums as well. */
  public static final int ALGORITHM_UNSPECIFIED = 0;
  /** Asymmetric algorithms. */
  public static final int RSA = 1;
  /** 2 removed, do not reuse. */
  public static final int EC = 3;
  /** Block cipher algorithms. */
  public static final int AES = 32;
  public static final int TRIPLE_DES = 33;
  /** MAC algorithms. */
  public static final int HMAC = 128;
}
