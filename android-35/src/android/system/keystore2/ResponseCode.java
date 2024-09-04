/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 4f1c704008e5687ed0d6f1590464aed39fc7f64e -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2-V3-java-source/gen/android/system/keystore2/ResponseCode.java.d -o out/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2-V3-java-source/gen -Nsystem/hardware/interfaces/keystore2/aidl/aidl_api/android.system.keystore2/3 system/hardware/interfaces/keystore2/aidl/aidl_api/android.system.keystore2/3/android/system/keystore2/ResponseCode.aidl
 */
package android.system.keystore2;
/** @hide */
public @interface ResponseCode {
  public static final int LOCKED = 2;
  public static final int UNINITIALIZED = 3;
  public static final int SYSTEM_ERROR = 4;
  public static final int PERMISSION_DENIED = 6;
  public static final int KEY_NOT_FOUND = 7;
  public static final int VALUE_CORRUPTED = 8;
  public static final int KEY_PERMANENTLY_INVALIDATED = 17;
  public static final int BACKEND_BUSY = 18;
  public static final int OPERATION_BUSY = 19;
  public static final int INVALID_ARGUMENT = 20;
  public static final int TOO_MUCH_DATA = 21;
  /** @deprecated replaced by other OUT_OF_KEYS_* errors below */
  @Deprecated
  public static final int OUT_OF_KEYS = 22;
  public static final int OUT_OF_KEYS_REQUIRES_SYSTEM_UPGRADE = 23;
  public static final int OUT_OF_KEYS_PENDING_INTERNET_CONNECTIVITY = 24;
  public static final int OUT_OF_KEYS_TRANSIENT_ERROR = 25;
  public static final int OUT_OF_KEYS_PERMANENT_ERROR = 26;
}
