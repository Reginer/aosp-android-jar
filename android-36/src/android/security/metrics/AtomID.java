/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/AtomID.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/AtomID.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * Atom IDs as defined in frameworks/proto_logging/stats/atoms.proto.
 * @hide
 */
public @interface AtomID {
  public static final int STORAGE_STATS = 10103;
  // reserved 10104
  public static final int KEY_CREATION_WITH_GENERAL_INFO = 10118;
  public static final int KEY_CREATION_WITH_AUTH_INFO = 10119;
  public static final int KEY_CREATION_WITH_PURPOSE_AND_MODES_INFO = 10120;
  public static final int KEYSTORE2_ATOM_WITH_OVERFLOW = 10121;
  public static final int KEY_OPERATION_WITH_PURPOSE_AND_MODES_INFO = 10122;
  public static final int KEY_OPERATION_WITH_GENERAL_INFO = 10123;
  public static final int RKP_ERROR_STATS = 10124;
  public static final int CRASH_STATS = 10125;
}
