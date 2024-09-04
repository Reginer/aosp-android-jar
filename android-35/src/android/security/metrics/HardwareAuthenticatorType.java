/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/HardwareAuthenticatorType.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/HardwareAuthenticatorType.aidl
 */
package android.security.metrics;
/**
 * HardwareAuthenticatorType enum as defined in Keystore2KeyCreationWithAuthInfo of
 * frameworks/proto_logging/stats/atoms.proto.
 * @hide
 */
public @interface HardwareAuthenticatorType {
  /** Unspecified takes 0. Other values are incremented by 1 compared to keymint spec. */
  public static final int AUTH_TYPE_UNSPECIFIED = 0;
  public static final int NONE = 1;
  public static final int PASSWORD = 2;
  public static final int FINGERPRINT = 3;
  public static final int ANY = 5;
}
