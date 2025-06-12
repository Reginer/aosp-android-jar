/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash a05c8079586139db45b0762a528cdd9745ad15ce -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/secureclock/aidl/android.hardware.security.secureclock_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V4-java-source/gen/android/hardware/security/keymint/ErrorCode.java.d -o out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V4-java-source/gen -Nhardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/4 hardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/4/android/hardware/security/keymint/ErrorCode.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.security.keymint;
/** @hide */
public @interface ErrorCode {
  public static final int OK = 0;
  public static final int ROOT_OF_TRUST_ALREADY_SET = -1;
  public static final int UNSUPPORTED_PURPOSE = -2;
  public static final int INCOMPATIBLE_PURPOSE = -3;
  public static final int UNSUPPORTED_ALGORITHM = -4;
  public static final int INCOMPATIBLE_ALGORITHM = -5;
  public static final int UNSUPPORTED_KEY_SIZE = -6;
  public static final int UNSUPPORTED_BLOCK_MODE = -7;
  public static final int INCOMPATIBLE_BLOCK_MODE = -8;
  public static final int UNSUPPORTED_MAC_LENGTH = -9;
  public static final int UNSUPPORTED_PADDING_MODE = -10;
  public static final int INCOMPATIBLE_PADDING_MODE = -11;
  public static final int UNSUPPORTED_DIGEST = -12;
  public static final int INCOMPATIBLE_DIGEST = -13;
  public static final int INVALID_EXPIRATION_TIME = -14;
  public static final int INVALID_USER_ID = -15;
  public static final int INVALID_AUTHORIZATION_TIMEOUT = -16;
  public static final int UNSUPPORTED_KEY_FORMAT = -17;
  public static final int INCOMPATIBLE_KEY_FORMAT = -18;
  public static final int UNSUPPORTED_KEY_ENCRYPTION_ALGORITHM = -19;
  public static final int UNSUPPORTED_KEY_VERIFICATION_ALGORITHM = -20;
  public static final int INVALID_INPUT_LENGTH = -21;
  public static final int KEY_EXPORT_OPTIONS_INVALID = -22;
  public static final int DELEGATION_NOT_ALLOWED = -23;
  public static final int KEY_NOT_YET_VALID = -24;
  public static final int KEY_EXPIRED = -25;
  public static final int KEY_USER_NOT_AUTHENTICATED = -26;
  public static final int OUTPUT_PARAMETER_NULL = -27;
  public static final int INVALID_OPERATION_HANDLE = -28;
  public static final int INSUFFICIENT_BUFFER_SPACE = -29;
  public static final int VERIFICATION_FAILED = -30;
  public static final int TOO_MANY_OPERATIONS = -31;
  public static final int UNEXPECTED_NULL_POINTER = -32;
  public static final int INVALID_KEY_BLOB = -33;
  public static final int IMPORTED_KEY_NOT_ENCRYPTED = -34;
  public static final int IMPORTED_KEY_DECRYPTION_FAILED = -35;
  public static final int IMPORTED_KEY_NOT_SIGNED = -36;
  public static final int IMPORTED_KEY_VERIFICATION_FAILED = -37;
  public static final int INVALID_ARGUMENT = -38;
  public static final int UNSUPPORTED_TAG = -39;
  public static final int INVALID_TAG = -40;
  public static final int MEMORY_ALLOCATION_FAILED = -41;
  public static final int IMPORT_PARAMETER_MISMATCH = -44;
  public static final int SECURE_HW_ACCESS_DENIED = -45;
  public static final int OPERATION_CANCELLED = -46;
  public static final int CONCURRENT_ACCESS_CONFLICT = -47;
  public static final int SECURE_HW_BUSY = -48;
  public static final int SECURE_HW_COMMUNICATION_FAILED = -49;
  public static final int UNSUPPORTED_EC_FIELD = -50;
  public static final int MISSING_NONCE = -51;
  public static final int INVALID_NONCE = -52;
  public static final int MISSING_MAC_LENGTH = -53;
  public static final int KEY_RATE_LIMIT_EXCEEDED = -54;
  public static final int CALLER_NONCE_PROHIBITED = -55;
  public static final int KEY_MAX_OPS_EXCEEDED = -56;
  public static final int INVALID_MAC_LENGTH = -57;
  public static final int MISSING_MIN_MAC_LENGTH = -58;
  public static final int UNSUPPORTED_MIN_MAC_LENGTH = -59;
  public static final int UNSUPPORTED_KDF = -60;
  public static final int UNSUPPORTED_EC_CURVE = -61;
  public static final int KEY_REQUIRES_UPGRADE = -62;
  public static final int ATTESTATION_CHALLENGE_MISSING = -63;
  public static final int KEYMINT_NOT_CONFIGURED = -64;
  public static final int ATTESTATION_APPLICATION_ID_MISSING = -65;
  public static final int CANNOT_ATTEST_IDS = -66;
  public static final int ROLLBACK_RESISTANCE_UNAVAILABLE = -67;
  public static final int HARDWARE_TYPE_UNAVAILABLE = -68;
  public static final int PROOF_OF_PRESENCE_REQUIRED = -69;
  public static final int CONCURRENT_PROOF_OF_PRESENCE_REQUESTED = -70;
  public static final int NO_USER_CONFIRMATION = -71;
  public static final int DEVICE_LOCKED = -72;
  public static final int EARLY_BOOT_ENDED = -73;
  public static final int ATTESTATION_KEYS_NOT_PROVISIONED = -74;
  public static final int ATTESTATION_IDS_NOT_PROVISIONED = -75;
  public static final int INVALID_OPERATION = -76;
  public static final int STORAGE_KEY_UNSUPPORTED = -77;
  public static final int INCOMPATIBLE_MGF_DIGEST = -78;
  public static final int UNSUPPORTED_MGF_DIGEST = -79;
  public static final int MISSING_NOT_BEFORE = -80;
  public static final int MISSING_NOT_AFTER = -81;
  public static final int MISSING_ISSUER_SUBJECT = -82;
  public static final int INVALID_ISSUER_SUBJECT = -83;
  public static final int BOOT_LEVEL_EXCEEDED = -84;
  public static final int HARDWARE_NOT_YET_AVAILABLE = -85;
  public static final int MODULE_HASH_ALREADY_SET = -86;
  public static final int UNIMPLEMENTED = -100;
  public static final int VERSION_MISMATCH = -101;
  public static final int UNKNOWN_ERROR = -1000;
}
