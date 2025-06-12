/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/KeyOrigin.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/KeyOrigin.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * KeyOrigin enum as defined in Keystore2KeyCreationWithGeneralInfo of
 * frameworks/proto_logging/stats/atoms.proto.
 * @hide
 */
public @interface KeyOrigin {
  /** Unspecified takes 0. Other values are incremented by 1 compared to keymint spec. */
  public static final int ORIGIN_UNSPECIFIED = 0;
  /** Generated in KeyMint.  Should not exist outside the TEE. */
  public static final int GENERATED = 1;
  /** Derived inside KeyMint.  Likely exists off-device. */
  public static final int DERIVED = 2;
  /** Imported into KeyMint.  Existed as cleartext in Android. */
  public static final int IMPORTED = 3;
  /** Previously used for another purpose that is now obsolete. */
  public static final int RESERVED = 4;
  /** Securely imported into KeyMint. */
  public static final int SECURELY_IMPORTED = 5;
}
