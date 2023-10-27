/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public @interface GeranBands {
  public static final int BAND_T380 = 1;
  public static final int BAND_T410 = 2;
  public static final int BAND_450 = 3;
  public static final int BAND_480 = 4;
  public static final int BAND_710 = 5;
  public static final int BAND_750 = 6;
  public static final int BAND_T810 = 7;
  public static final int BAND_850 = 8;
  public static final int BAND_P900 = 9;
  public static final int BAND_E900 = 10;
  public static final int BAND_R900 = 11;
  public static final int BAND_DCS1800 = 12;
  public static final int BAND_PCS1900 = 13;
  public static final int BAND_ER900 = 14;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == BAND_T380) return "BAND_T380";
      if (_aidl_v == BAND_T410) return "BAND_T410";
      if (_aidl_v == BAND_450) return "BAND_450";
      if (_aidl_v == BAND_480) return "BAND_480";
      if (_aidl_v == BAND_710) return "BAND_710";
      if (_aidl_v == BAND_750) return "BAND_750";
      if (_aidl_v == BAND_T810) return "BAND_T810";
      if (_aidl_v == BAND_850) return "BAND_850";
      if (_aidl_v == BAND_P900) return "BAND_P900";
      if (_aidl_v == BAND_E900) return "BAND_E900";
      if (_aidl_v == BAND_R900) return "BAND_R900";
      if (_aidl_v == BAND_DCS1800) return "BAND_DCS1800";
      if (_aidl_v == BAND_PCS1900) return "BAND_PCS1900";
      if (_aidl_v == BAND_ER900) return "BAND_ER900";
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
