/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
/**
 * Type of the writable partition that virtualizationservice creates via
 * initializeWritablePartition.
 */
public @interface PartitionType {
  /** The partition is simply initialized as all zeros */
  public static final int RAW = 0;
  /** The partition is initialized as an instance image which is formatted to hold per-VM secrets */
  public static final int ANDROID_VM_INSTANCE = 1;
  /** The partition is initialized to back encryptedstore disk image formatted to indicate intent */
  public static final int ENCRYPTEDSTORE = 2;
}
