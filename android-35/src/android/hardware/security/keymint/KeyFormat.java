/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 74a538630d5d90f732f361a2313cbb69b09eb047 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/secureclock/aidl/android.hardware.security.secureclock_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V3-java-source/gen/android/hardware/security/keymint/KeyFormat.java.d -o out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V3-java-source/gen -Nhardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/3 hardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/3/android/hardware/security/keymint/KeyFormat.aidl
 */
package android.hardware.security.keymint;
/** @hide */
public @interface KeyFormat {
  public static final int X509 = 0;
  public static final int PKCS8 = 1;
  public static final int RAW = 3;
}