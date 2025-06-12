/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash 98d815116c190250e9e5a1d9182cea8126fd0e97 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2-V5-java-source/gen/android/system/keystore2/KeyPermission.java.d -o out/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2-V5-java-source/gen -Nsystem/hardware/interfaces/keystore2/aidl/aidl_api/android.system.keystore2/5 system/hardware/interfaces/keystore2/aidl/aidl_api/android.system.keystore2/5/android/system/keystore2/KeyPermission.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
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
