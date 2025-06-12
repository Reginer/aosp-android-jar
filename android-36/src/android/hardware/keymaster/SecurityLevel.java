/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash d60ca1bb57f94508910cac7b8910c85e2a49a11f -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster-V4-java-source/gen/android/hardware/keymaster/SecurityLevel.java.d -o out/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster-V4-java-source/gen -Nhardware/interfaces/keymaster/aidl/aidl_api/android.hardware.keymaster/4 hardware/interfaces/keymaster/aidl/aidl_api/android.hardware.keymaster/4/android/hardware/keymaster/SecurityLevel.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.keymaster;
/** @hide */
public @interface SecurityLevel {
  public static final int SOFTWARE = 0;
  public static final int TRUSTED_ENVIRONMENT = 1;
  public static final int STRONGBOX = 2;
}
