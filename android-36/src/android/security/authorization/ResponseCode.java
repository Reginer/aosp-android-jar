/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/security/secureclock/aidl/android.hardware.security.secureclock_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.authorization-java-source/gen/android/security/authorization/ResponseCode.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.authorization-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/authorization/ResponseCode.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.authorization;
/**
 * Used as exception codes by IKeystoreAuthorization.
 * @hide
 */
public @interface ResponseCode {
  /** A matching auth token is not found. */
  public static final int NO_AUTH_TOKEN_FOUND = 1;
  /** The matching auth token is expired. */
  public static final int AUTH_TOKEN_EXPIRED = 2;
  /**
   * Same as in keystore2/ResponseCode.aidl.
   * Any unexpected Error such as IO or communication errors.
   */
  public static final int SYSTEM_ERROR = 4;
  /**
   * Same as in keystore2/ResponseCode.aidl.
   * Indicates that the caller does not have the permissions for the attempted request.
   */
  public static final int PERMISSION_DENIED = 6;
  /**
   * Same as in keystore2/ResponseCode.aidl.
   * Indicates that the requested key does not exist.
   */
  public static final int KEY_NOT_FOUND = 7;
  /**
   * Same as in keystore2/ResponseCode.aidl.
   * Indicates that a value being processed is corrupted.
   */
  public static final int VALUE_CORRUPTED = 8;
  /**
   * Same as in keystore2/ResponseCode.aidl.
   * Indicates that an invalid argument was passed to an API call.
   */
  public static final int INVALID_ARGUMENT = 20;
}
