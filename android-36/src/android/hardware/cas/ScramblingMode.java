/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 1 --hash bc51d8d70a55ec4723d3f73d0acf7003306bf69f --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/cas/aidl/android.hardware.cas-V1-java-source/gen/android/hardware/cas/ScramblingMode.java.d -o out/soong/.intermediates/hardware/interfaces/cas/aidl/android.hardware.cas-V1-java-source/gen -Nhardware/interfaces/cas/aidl/aidl_api/android.hardware.cas/1 hardware/interfaces/cas/aidl/aidl_api/android.hardware.cas/1/android/hardware/cas/ScramblingMode.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.cas;
/** @hide */
public @interface ScramblingMode {
  public static final int RESERVED = 0;
  public static final int DVB_CSA1 = 1;
  public static final int DVB_CSA2 = 2;
  public static final int DVB_CSA3_STANDARD = 3;
  public static final int DVB_CSA3_MINIMAL = 4;
  public static final int DVB_CSA3_ENHANCE = 5;
  public static final int DVB_CISSA_V1 = 6;
  public static final int DVB_IDSA = 7;
  public static final int MULTI2 = 8;
  public static final int AES128 = 9;
  public static final int AES_ECB = 10;
  public static final int AES_SCTE52 = 11;
  public static final int TDES_ECB = 12;
  public static final int TDES_SCTE52 = 13;
  public static final int AES_CBC = 14;
}
