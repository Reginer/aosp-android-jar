/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationcommon_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen/android/system/virtualizationservice/VirtualMachineState.java.d -o out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen -Npackages/modules/Virtualization/android/virtualizationservice/aidl packages/modules/Virtualization/android/virtualizationservice/aidl/android/system/virtualizationservice/VirtualMachineState.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.system.virtualizationservice;
/** The lifecycle state of a VM. */
public @interface VirtualMachineState {
  /** The VM has been created but not yet started. */
  public static final int NOT_STARTED = 0;
  /** The VM is running, but the payload has not yet started. */
  public static final int STARTING = 1;
  /**
   * The VM is running and the payload has been started, but it has not yet indicated that it is
   * ready.
   */
  public static final int STARTED = 2;
  /** The VM payload has indicated that it is ready to serve requests. */
  public static final int READY = 3;
  /** The VM payload has finished but the VM itself is still running. */
  public static final int FINISHED = 4;
  /** The VM has died. */
  public static final int DEAD = 6;
}
