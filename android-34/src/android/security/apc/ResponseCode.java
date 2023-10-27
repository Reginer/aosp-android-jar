/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.apc;
/**
 * Used as service specific exception code by IProtectedConfirmation and as result
 * code by IConfirmationCallback
 * @hide
 */
public @interface ResponseCode {
  /** The prompt completed successfully with the user confirming the message (callback result). */
  public static final int OK = 0;
  /** The user cancelled the TUI (callback result). */
  public static final int CANCELLED = 1;
  /**
   * The prompt was aborted (callback result). This may happen when the app cancels the prompt,
   * or when the prompt was cancelled due to an unexpected asynchronous event, such as an
   * incoming phone call.
   */
  public static final int ABORTED = 2;
  /** Another prompt cannot be started because another prompt is pending. */
  public static final int OPERATION_PENDING = 3;
  /** The request was ignored. */
  public static final int IGNORED = 4;
  /** An unexpected system error occurred. */
  public static final int SYSTEM_ERROR = 5;
  /** Backend is not implemented. */
  public static final int UNIMPLEMENTED = 6;
  /** Permission Denied. */
  public static final int PERMISSION_DENIED = 30;
}
