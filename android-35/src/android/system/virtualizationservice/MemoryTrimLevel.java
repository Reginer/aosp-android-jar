/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationcommon_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen/android/system/virtualizationservice/MemoryTrimLevel.java.d -o out/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen -Npackages/modules/Virtualization/virtualizationservice/aidl packages/modules/Virtualization/virtualizationservice/aidl/android/system/virtualizationservice/MemoryTrimLevel.aidl
 */
package android.system.virtualizationservice;
/** Memory trim levels propagated from the app to the VM. */
public @interface MemoryTrimLevel {
  /** Same meaning as in ComponentCallbacks2 */
  public static final int TRIM_MEMORY_RUNNING_CRITICAL = 0;
  public static final int TRIM_MEMORY_RUNNING_LOW = 1;
  public static final int TRIM_MEMORY_RUNNING_MODERATE = 2;
}
