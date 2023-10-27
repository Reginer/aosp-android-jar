/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public @interface AudioQuality {
  public static final int UNSPECIFIED = 0;
  public static final int AMR = 1;
  public static final int AMR_WB = 2;
  public static final int GSM_EFR = 3;
  public static final int GSM_FR = 4;
  public static final int GSM_HR = 5;
  public static final int EVRC = 6;
  public static final int EVRC_B = 7;
  public static final int EVRC_WB = 8;
  public static final int EVRC_NW = 9;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNSPECIFIED) return "UNSPECIFIED";
      if (_aidl_v == AMR) return "AMR";
      if (_aidl_v == AMR_WB) return "AMR_WB";
      if (_aidl_v == GSM_EFR) return "GSM_EFR";
      if (_aidl_v == GSM_FR) return "GSM_FR";
      if (_aidl_v == GSM_HR) return "GSM_HR";
      if (_aidl_v == EVRC) return "EVRC";
      if (_aidl_v == EVRC_B) return "EVRC_B";
      if (_aidl_v == EVRC_WB) return "EVRC_WB";
      if (_aidl_v == EVRC_NW) return "EVRC_NW";
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
