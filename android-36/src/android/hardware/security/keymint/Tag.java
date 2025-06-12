/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash a05c8079586139db45b0762a528cdd9745ad15ce -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/secureclock/aidl/android.hardware.security.secureclock_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V4-java-source/gen/android/hardware/security/keymint/Tag.java.d -o out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V4-java-source/gen -Nhardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/4 hardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/4/android/hardware/security/keymint/Tag.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.security.keymint;
/** @hide */
public @interface Tag {
  public static final int INVALID = 0;
  public static final int PURPOSE = 536870913;
  public static final int ALGORITHM = 268435458;
  public static final int KEY_SIZE = 805306371;
  public static final int BLOCK_MODE = 536870916;
  public static final int DIGEST = 536870917;
  public static final int PADDING = 536870918;
  public static final int CALLER_NONCE = 1879048199;
  public static final int MIN_MAC_LENGTH = 805306376;
  public static final int EC_CURVE = 268435466;
  public static final int RSA_PUBLIC_EXPONENT = 1342177480;
  public static final int INCLUDE_UNIQUE_ID = 1879048394;
  public static final int RSA_OAEP_MGF_DIGEST = 536871115;
  public static final int BOOTLOADER_ONLY = 1879048494;
  public static final int ROLLBACK_RESISTANCE = 1879048495;
  public static final int HARDWARE_TYPE = 268435760;
  public static final int EARLY_BOOT_ONLY = 1879048497;
  public static final int ACTIVE_DATETIME = 1610613136;
  public static final int ORIGINATION_EXPIRE_DATETIME = 1610613137;
  public static final int USAGE_EXPIRE_DATETIME = 1610613138;
  public static final int MIN_SECONDS_BETWEEN_OPS = 805306771;
  public static final int MAX_USES_PER_BOOT = 805306772;
  public static final int USAGE_COUNT_LIMIT = 805306773;
  public static final int USER_ID = 805306869;
  public static final int USER_SECURE_ID = -1610612234;
  public static final int NO_AUTH_REQUIRED = 1879048695;
  public static final int USER_AUTH_TYPE = 268435960;
  public static final int AUTH_TIMEOUT = 805306873;
  public static final int ALLOW_WHILE_ON_BODY = 1879048698;
  public static final int TRUSTED_USER_PRESENCE_REQUIRED = 1879048699;
  public static final int TRUSTED_CONFIRMATION_REQUIRED = 1879048700;
  public static final int UNLOCKED_DEVICE_REQUIRED = 1879048701;
  public static final int APPLICATION_ID = -1879047591;
  public static final int APPLICATION_DATA = -1879047492;
  public static final int CREATION_DATETIME = 1610613437;
  public static final int ORIGIN = 268436158;
  public static final int ROOT_OF_TRUST = -1879047488;
  public static final int OS_VERSION = 805307073;
  public static final int OS_PATCHLEVEL = 805307074;
  public static final int UNIQUE_ID = -1879047485;
  public static final int ATTESTATION_CHALLENGE = -1879047484;
  public static final int ATTESTATION_APPLICATION_ID = -1879047483;
  public static final int ATTESTATION_ID_BRAND = -1879047482;
  public static final int ATTESTATION_ID_DEVICE = -1879047481;
  public static final int ATTESTATION_ID_PRODUCT = -1879047480;
  public static final int ATTESTATION_ID_SERIAL = -1879047479;
  public static final int ATTESTATION_ID_IMEI = -1879047478;
  public static final int ATTESTATION_ID_MEID = -1879047477;
  public static final int ATTESTATION_ID_MANUFACTURER = -1879047476;
  public static final int ATTESTATION_ID_MODEL = -1879047475;
  public static final int VENDOR_PATCHLEVEL = 805307086;
  public static final int BOOT_PATCHLEVEL = 805307087;
  public static final int DEVICE_UNIQUE_ATTESTATION = 1879048912;
  public static final int IDENTITY_CREDENTIAL_KEY = 1879048913;
  public static final int STORAGE_KEY = 1879048914;
  public static final int ATTESTATION_ID_SECOND_IMEI = -1879047469;
  public static final int MODULE_HASH = -1879047468;
  public static final int ASSOCIATED_DATA = -1879047192;
  public static final int NONCE = -1879047191;
  public static final int MAC_LENGTH = 805307371;
  public static final int RESET_SINCE_ID_ROTATION = 1879049196;
  public static final int CONFIRMATION_TOKEN = -1879047187;
  public static final int CERTIFICATE_SERIAL = -2147482642;
  public static final int CERTIFICATE_SUBJECT = -1879047185;
  public static final int CERTIFICATE_NOT_BEFORE = 1610613744;
  public static final int CERTIFICATE_NOT_AFTER = 1610613745;
  public static final int MAX_BOOT_LEVEL = 805307378;
}
