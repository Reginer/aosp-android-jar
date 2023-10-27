/*
 * This file is auto-generated.  DO NOT MODIFY.
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
