/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/SecurityLevel.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/SecurityLevel.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * SecurityLevel enum as defined in stats/enums/system/security/keystore2/enums.proto.
 * @hide
 */
public @interface SecurityLevel {
  /** Unspecified takes 0. Other values are incremented by 1 compared to keymint spec. */
  public static final int SECURITY_LEVEL_UNSPECIFIED = 0;
  public static final int SECURITY_LEVEL_SOFTWARE = 1;
  public static final int SECURITY_LEVEL_TRUSTED_ENVIRONMENT = 2;
  public static final int SECURITY_LEVEL_STRONGBOX = 3;
  public static final int SECURITY_LEVEL_KEYSTORE = 4;
}
