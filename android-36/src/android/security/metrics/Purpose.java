/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/Purpose.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/Purpose.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * Purpose enum as defined in Keystore2KeyOperationWithPurposeAndModesInfo of
 * frameworks/proto_logging/stats/atoms.proto.
 * @hide
 */
public @interface Purpose {
  /** Unspecified takes 0. Other values are incremented by 1 compared to keymint spec. */
  public static final int KEY_PURPOSE_UNSPECIFIED = 0;
  /** Usable with RSA, 3DES and AES keys. */
  public static final int ENCRYPT = 1;
  /** Usable with RSA, 3DES and AES keys. */
  public static final int DECRYPT = 2;
  /** Usable with RSA, EC and HMAC keys. */
  public static final int SIGN = 3;
  /** Usable with RSA, EC and HMAC keys. */
  public static final int VERIFY = 4;
  /** 4 is reserved */
  /** Usable with RSA keys. */
  public static final int WRAP_KEY = 6;
  /** Key Agreement, usable with EC keys. */
  public static final int AGREE_KEY = 7;
  /**
   * Usable as an attestation signing key.  Keys with this purpose must not have any other
   * purpose.
   */
  public static final int ATTEST_KEY = 8;
}
