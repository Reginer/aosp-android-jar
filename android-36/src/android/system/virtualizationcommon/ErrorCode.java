/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationcommon-java-source/gen/android/system/virtualizationcommon/ErrorCode.java.d -o out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationcommon-java-source/gen -Npackages/modules/Virtualization/android/virtualizationservice/aidl packages/modules/Virtualization/android/virtualizationservice/aidl/android/system/virtualizationcommon/ErrorCode.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.system.virtualizationcommon;
/** Errors reported from within a VM. */
public @interface ErrorCode {
  /** Error code for all other errors not listed below. */
  public static final int UNKNOWN = 0;
  /**
   * Error code indicating that the payload can't be verified due to various reasons (e.g invalid
   * merkle tree, invalid formats, etc).
   */
  public static final int PAYLOAD_VERIFICATION_FAILED = 1;
  /** Error code indicating that the payload is verified, but has changed since the last boot. */
  public static final int PAYLOAD_CHANGED = 2;
  /** Error code indicating that the payload config is invalid. */
  public static final int PAYLOAD_INVALID_CONFIG = 3;
}
