/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public @interface ApnTypes {
  public static final int NONE = 0;
  public static final int DEFAULT = 1;
  public static final int MMS = 2;
  public static final int SUPL = 4;
  public static final int DUN = 8;
  public static final int HIPRI = 16;
  public static final int FOTA = 32;
  public static final int IMS = 64;
  public static final int CBS = 128;
  public static final int IA = 256;
  public static final int EMERGENCY = 512;
  public static final int MCX = 1024;
  public static final int XCAP = 2048;
  public static final int VSIM = 4096;
  public static final int BIP = 8192;
  public static final int ENTERPRISE = 16384;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == NONE) return "NONE";
      if (_aidl_v == DEFAULT) return "DEFAULT";
      if (_aidl_v == MMS) return "MMS";
      if (_aidl_v == SUPL) return "SUPL";
      if (_aidl_v == DUN) return "DUN";
      if (_aidl_v == HIPRI) return "HIPRI";
      if (_aidl_v == FOTA) return "FOTA";
      if (_aidl_v == IMS) return "IMS";
      if (_aidl_v == CBS) return "CBS";
      if (_aidl_v == IA) return "IA";
      if (_aidl_v == EMERGENCY) return "EMERGENCY";
      if (_aidl_v == MCX) return "MCX";
      if (_aidl_v == XCAP) return "XCAP";
      if (_aidl_v == VSIM) return "VSIM";
      if (_aidl_v == BIP) return "BIP";
      if (_aidl_v == ENTERPRISE) return "ENTERPRISE";
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
