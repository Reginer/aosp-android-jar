/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * Type enums of video transcoding errors.
 * 
 * {@hide}
 */
public @interface TranscodingErrorCode {
  // Errors exposed to client side.
  public static final int kNoError = 0;
  public static final int kDroppedByService = 1;
  public static final int kServiceUnavailable = 2;
  // Other private errors.
  public static final int kPrivateErrorFirst = 1000;
  public static final int kUnknown = 1000;
  public static final int kMalformed = 1001;
  public static final int kUnsupported = 1002;
  public static final int kInvalidParameter = 1003;
  public static final int kInvalidOperation = 1004;
  public static final int kErrorIO = 1005;
  public static final int kInsufficientResources = 1006;
  public static final int kWatchdogTimeout = 1007;
  public static final int kUidGoneCancelled = 1008;
}
