/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash c45c122528c07c449ea08f6eacaace17bb7abc38 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen/android/hardware/radio/network/ConnectionEvent.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3/android/hardware/radio/network/ConnectionEvent.aidl
 */
package android.hardware.radio.network;
/** @hide */
public @interface ConnectionEvent {
  public static final int CS_SIGNALLING_GSM = 0;
  public static final int PS_SIGNALLING_GPRS = 1;
  public static final int CS_SIGNALLING_3G = 2;
  public static final int PS_SIGNALLING_3G = 3;
  public static final int NAS_SIGNALLING_LTE = 4;
  public static final int AS_SIGNALLING_LTE = 5;
  public static final int VOLTE_SIP = 6;
  public static final int VOLTE_SIP_SOS = 7;
  public static final int VOLTE_RTP = 8;
  public static final int VOLTE_RTP_SOS = 9;
  public static final int NAS_SIGNALLING_5G = 10;
  public static final int AS_SIGNALLING_5G = 11;
  public static final int VONR_SIP = 12;
  public static final int VONR_SIP_SOS = 13;
  public static final int VONR_RTP = 14;
  public static final int VONR_RTP_SOS = 15;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == CS_SIGNALLING_GSM) return "CS_SIGNALLING_GSM";
      if (_aidl_v == PS_SIGNALLING_GPRS) return "PS_SIGNALLING_GPRS";
      if (_aidl_v == CS_SIGNALLING_3G) return "CS_SIGNALLING_3G";
      if (_aidl_v == PS_SIGNALLING_3G) return "PS_SIGNALLING_3G";
      if (_aidl_v == NAS_SIGNALLING_LTE) return "NAS_SIGNALLING_LTE";
      if (_aidl_v == AS_SIGNALLING_LTE) return "AS_SIGNALLING_LTE";
      if (_aidl_v == VOLTE_SIP) return "VOLTE_SIP";
      if (_aidl_v == VOLTE_SIP_SOS) return "VOLTE_SIP_SOS";
      if (_aidl_v == VOLTE_RTP) return "VOLTE_RTP";
      if (_aidl_v == VOLTE_RTP_SOS) return "VOLTE_RTP_SOS";
      if (_aidl_v == NAS_SIGNALLING_5G) return "NAS_SIGNALLING_5G";
      if (_aidl_v == AS_SIGNALLING_5G) return "AS_SIGNALLING_5G";
      if (_aidl_v == VONR_SIP) return "VONR_SIP";
      if (_aidl_v == VONR_SIP_SOS) return "VONR_SIP_SOS";
      if (_aidl_v == VONR_RTP) return "VONR_RTP";
      if (_aidl_v == VONR_RTP_SOS) return "VONR_RTP_SOS";
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
