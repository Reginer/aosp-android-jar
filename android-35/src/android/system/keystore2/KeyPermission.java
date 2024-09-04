/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 4f1c704008e5687ed0d6f1590464aed39fc7f64e -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2-V3-java-source/gen/android/system/keystore2/KeyPermission.java.d -o out/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2-V3-java-source/gen -Nsystem/hardware/interfaces/keystore2/aidl/aidl_api/android.system.keystore2/3 system/hardware/interfaces/keystore2/aidl/aidl_api/android.system.keystore2/3/android/system/keystore2/KeyPermission.aidl
 */
package android.system.keystore2;
/** @hide */
public @interface KeyPermission {
  public static final int NONE = 0;
  public static final int DELETE = 1;
  public static final int GEN_UNIQUE_ID = 2;
  public static final int GET_INFO = 4;
  public static final int GRANT = 8;
  public static final int MANAGE_BLOB = 16;
  public static final int REBIND = 32;
  public static final int REQ_FORCED_OP = 64;
  public static final int UPDATE = 128;
  public static final int USE = 256;
  public static final int USE_DEV_ID = 512;
  public static final int USE_NO_LSKF_BINDING = 1024;
  public static final int CONVERT_STORAGE_KEY_TO_EPHEMERAL = 2048;
}
