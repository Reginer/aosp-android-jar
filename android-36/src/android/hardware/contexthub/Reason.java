/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash df80fdbb6f95a8a2988bc72b7f08f891847b80eb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen/android/hardware/contexthub/Reason.java.d -o out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen -Nhardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4 hardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4/android/hardware/contexthub/Reason.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.contexthub;
public @interface Reason {
  public static final byte UNSPECIFIED = 0;
  public static final byte OUT_OF_MEMORY = 1;
  public static final byte TIMEOUT = 2;
  public static final byte OPEN_ENDPOINT_SESSION_REQUEST_REJECTED = 3;
  public static final byte CLOSE_ENDPOINT_SESSION_REQUESTED = 4;
  public static final byte ENDPOINT_INVALID = 5;
  public static final byte ENDPOINT_GONE = 6;
  public static final byte ENDPOINT_CRASHED = 7;
  public static final byte HUB_RESET = 8;
  public static final byte PERMISSION_DENIED = 9;
}
