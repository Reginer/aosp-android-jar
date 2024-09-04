/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash c45c122528c07c449ea08f6eacaace17bb7abc38 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen/android/hardware/radio/network/SecurityAlgorithm.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3/android/hardware/radio/network/SecurityAlgorithm.aidl
 */
package android.hardware.radio.network;
/** @hide */
public @interface SecurityAlgorithm {
  public static final int A50 = 0;
  public static final int A51 = 1;
  public static final int A52 = 2;
  public static final int A53 = 3;
  public static final int A54 = 4;
  public static final int GEA0 = 14;
  public static final int GEA1 = 15;
  public static final int GEA2 = 16;
  public static final int GEA3 = 17;
  public static final int GEA4 = 18;
  public static final int GEA5 = 19;
  public static final int UEA0 = 29;
  public static final int UEA1 = 30;
  public static final int UEA2 = 31;
  public static final int EEA0 = 41;
  public static final int EEA1 = 42;
  public static final int EEA2 = 43;
  public static final int EEA3 = 44;
  public static final int NEA0 = 55;
  public static final int NEA1 = 56;
  public static final int NEA2 = 57;
  public static final int NEA3 = 58;
  public static final int SIP_NO_IPSEC_CONFIG = 66;
  public static final int IMS_NULL = 67;
  public static final int SIP_NULL = 68;
  public static final int AES_GCM = 69;
  public static final int AES_GMAC = 70;
  public static final int AES_CBC = 71;
  public static final int DES_EDE3_CBC = 72;
  public static final int AES_EDE3_CBC = 73;
  public static final int HMAC_SHA1_96 = 74;
  public static final int HMAC_MD5_96 = 75;
  public static final int RTP = 85;
  public static final int SRTP_NULL = 86;
  public static final int SRTP_AES_COUNTER = 87;
  public static final int SRTP_AES_F8 = 88;
  public static final int SRTP_HMAC_SHA1 = 89;
  public static final int ENCR_AES_GCM_16 = 99;
  public static final int ENCR_AES_CBC = 100;
  public static final int AUTH_HMAC_SHA2_256_128 = 101;
  public static final int UNKNOWN = 113;
  public static final int OTHER = 114;
  public static final int ORYX = 124;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == A50) return "A50";
      if (_aidl_v == A51) return "A51";
      if (_aidl_v == A52) return "A52";
      if (_aidl_v == A53) return "A53";
      if (_aidl_v == A54) return "A54";
      if (_aidl_v == GEA0) return "GEA0";
      if (_aidl_v == GEA1) return "GEA1";
      if (_aidl_v == GEA2) return "GEA2";
      if (_aidl_v == GEA3) return "GEA3";
      if (_aidl_v == GEA4) return "GEA4";
      if (_aidl_v == GEA5) return "GEA5";
      if (_aidl_v == UEA0) return "UEA0";
      if (_aidl_v == UEA1) return "UEA1";
      if (_aidl_v == UEA2) return "UEA2";
      if (_aidl_v == EEA0) return "EEA0";
      if (_aidl_v == EEA1) return "EEA1";
      if (_aidl_v == EEA2) return "EEA2";
      if (_aidl_v == EEA3) return "EEA3";
      if (_aidl_v == NEA0) return "NEA0";
      if (_aidl_v == NEA1) return "NEA1";
      if (_aidl_v == NEA2) return "NEA2";
      if (_aidl_v == NEA3) return "NEA3";
      if (_aidl_v == SIP_NO_IPSEC_CONFIG) return "SIP_NO_IPSEC_CONFIG";
      if (_aidl_v == IMS_NULL) return "IMS_NULL";
      if (_aidl_v == SIP_NULL) return "SIP_NULL";
      if (_aidl_v == AES_GCM) return "AES_GCM";
      if (_aidl_v == AES_GMAC) return "AES_GMAC";
      if (_aidl_v == AES_CBC) return "AES_CBC";
      if (_aidl_v == DES_EDE3_CBC) return "DES_EDE3_CBC";
      if (_aidl_v == AES_EDE3_CBC) return "AES_EDE3_CBC";
      if (_aidl_v == HMAC_SHA1_96) return "HMAC_SHA1_96";
      if (_aidl_v == HMAC_MD5_96) return "HMAC_MD5_96";
      if (_aidl_v == RTP) return "RTP";
      if (_aidl_v == SRTP_NULL) return "SRTP_NULL";
      if (_aidl_v == SRTP_AES_COUNTER) return "SRTP_AES_COUNTER";
      if (_aidl_v == SRTP_AES_F8) return "SRTP_AES_F8";
      if (_aidl_v == SRTP_HMAC_SHA1) return "SRTP_HMAC_SHA1";
      if (_aidl_v == ENCR_AES_GCM_16) return "ENCR_AES_GCM_16";
      if (_aidl_v == ENCR_AES_CBC) return "ENCR_AES_CBC";
      if (_aidl_v == AUTH_HMAC_SHA2_256_128) return "AUTH_HMAC_SHA2_256_128";
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == OTHER) return "OTHER";
      if (_aidl_v == ORYX) return "ORYX";
      return Integer.toString(_aidl_v);
    }
    static String arrayToString(Object _aidl_v) {
      if (_aidl_v == null) return "null";
      Class<?> _aidl_cls = _aidl_v.getClass();
      if (!_aidl_cls.isArray()) throw new IllegalArgumentException("not an array: " + _aidl_v);
      Class<?> comp = _aidl_cls.getComponentType();
      java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "[", "]");
      if (comp.isArray()) {
        for (int _aidl_i = 0; _aidl_i < java.lang.reflect.Array.getLength(_aidl_v); _aidl_i++) {
          _aidl_sj.add(arrayToString(java.lang.reflect.Array.get(_aidl_v, _aidl_i)));
        }
      } else {
        if (_aidl_cls != int[].class) throw new IllegalArgumentException("wrong type: " + _aidl_cls);
        for (int e : (int[]) _aidl_v) {
          _aidl_sj.add(toString(e));
        }
      }
      return _aidl_sj.toString();
    }
  }
}
