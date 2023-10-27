/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.metrics;
/**
 * Algorithm enum as defined in stats/enums/system/security/keystore2/enums.proto.
 * @hide
 */
public @interface Algorithm {
  /** ALGORITHM is prepended because UNSPECIFIED exists in other enums as well. */
  public static final int ALGORITHM_UNSPECIFIED = 0;
  /** Asymmetric algorithms. */
  public static final int RSA = 1;
  /** 2 removed, do not reuse. */
  public static final int EC = 3;
  /** Block cipher algorithms. */
  public static final int AES = 32;
  public static final int TRIPLE_DES = 33;
  /** MAC algorithms. */
  public static final int HMAC = 128;
}
