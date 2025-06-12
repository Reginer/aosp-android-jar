/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/HardwareAuthenticatorType.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/HardwareAuthenticatorType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * AIDL enum representing the
 * android.os.statsd.Keystore2KeyCreationWithAuthInfo.HardwareAuthenticatorType protocol buffer enum
 * defined in frameworks/proto_logging/stats/atoms.proto.
 * 
 * This enum is a mirror of
 * hardware/interfaces/security/keymint/aidl/android/hardware/security/keymint/HardwareAuthenticatorType.aidl
 * except that:
 *   - The enum tag number for the ANY value is set to 5,
 *   - The enum tag numbers of all other values are incremented by 1, and
 *   - Two new values are added: AUTH_TYPE_UNSPECIFIED and NO_AUTH_TYPE.
 * The KeyMint AIDL enum is a bitmask, but since the enum tag numbers in this metrics-specific
 * mirror were shifted, this enum can't behave as a bitmask. As a result, we have to explicitly add
 * values to represent the bitwise OR of pairs of values that we expect to see in the wild.
 * @hide
 */
public @interface HardwareAuthenticatorType {
  // Sentinel value to represent undefined enum tag numbers (which would represent combinations of
  // values from the KeyMint enum that aren't explicitly represented here). We don't expect to see
  // this value in the metrics, but if we do it means that an unexpected (bitwise OR) combination
  // of KeyMint HardwareAuthenticatorType values is being used as the HardwareAuthenticatorType
  // key parameter.
  public static final int AUTH_TYPE_UNSPECIFIED = 0;
  // Corresponds to KeyMint's HardwareAuthenticatorType::NONE value (enum tag number 0).
  public static final int NONE = 1;
  // Corresponds to KeyMint's HardwareAuthenticatorType::PASSWORD value (enum tag number 1 << 0).
  public static final int PASSWORD = 2;
  // Corresponds to KeyMint's HardwareAuthenticatorType::FINGERPRINT value (enum tag number
  // 1 << 1).
  public static final int FINGERPRINT = 3;
  // Corresponds to the (bitwise OR) combination of KeyMint's HardwareAuthenticatorType::PASSWORD
  // and HardwareAuthenticatorType::FINGERPRINT values.
  public static final int PASSWORD_OR_FINGERPRINT = 4;
  // Corresponds to KeyMint's HardwareAuthenticatorType::ANY value (enum tag number 0xFFFFFFFF).
  public static final int ANY = 5;
  // No HardwareAuthenticatorType was specified in the key parameters.
  public static final int NO_AUTH_TYPE = 6;
}
