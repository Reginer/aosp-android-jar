/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/system/hardware/interfaces/keystore2/aidl/android.system.keystore2_interface/5/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen/android/security/metrics/Storage.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.metrics-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/metrics/Storage.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.metrics;
/**
 * Storage enum as defined in Keystore2StorageStats of frameworks/proto_logging/stats/atoms.proto.
 * @hide
 */
public @interface Storage {
  public static final int STORAGE_UNSPECIFIED = 0;
  public static final int KEY_ENTRY = 1;
  public static final int KEY_ENTRY_ID_INDEX = 2;
  public static final int KEY_ENTRY_DOMAIN_NAMESPACE_INDEX = 3;
  public static final int BLOB_ENTRY = 4;
  public static final int BLOB_ENTRY_KEY_ENTRY_ID_INDEX = 5;
  public static final int KEY_PARAMETER = 6;
  public static final int KEY_PARAMETER_KEY_ENTRY_ID_INDEX = 7;
  public static final int KEY_METADATA = 8;
  public static final int KEY_METADATA_KEY_ENTRY_ID_INDEX = 9;
  public static final int GRANT = 10;
  public static final int AUTH_TOKEN = 11;
  public static final int BLOB_METADATA = 12;
  public static final int BLOB_METADATA_BLOB_ENTRY_ID_INDEX = 13;
  public static final int METADATA = 14;
  public static final int DATABASE = 15;
  public static final int LEGACY_STORAGE = 16;
}
