/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 74a538630d5d90f732f361a2313cbb69b09eb047 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/secureclock/aidl/android.hardware.security.secureclock_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V3-java-source/gen/android/hardware/security/keymint/Digest.java.d -o out/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint-V3-java-source/gen -Nhardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/3 hardware/interfaces/security/keymint/aidl/aidl_api/android.hardware.security.keymint/3/android/hardware/security/keymint/Digest.aidl
 */
package android.hardware.security.keymint;
/** @hide */
public @interface Digest {
  public static final int NONE = 0;
  public static final int MD5 = 1;
  public static final int SHA1 = 2;
  public static final int SHA_2_224 = 3;
  public static final int SHA_2_256 = 4;
  public static final int SHA_2_384 = 5;
  public static final int SHA_2_512 = 6;
}