/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public @interface RadioBandMode {
  public static final int BAND_MODE_UNSPECIFIED = 0;
  public static final int BAND_MODE_EURO = 1;
  public static final int BAND_MODE_USA = 2;
  public static final int BAND_MODE_JPN = 3;
  public static final int BAND_MODE_AUS = 4;
  public static final int BAND_MODE_AUS_2 = 5;
  public static final int BAND_MODE_CELL_800 = 6;
  public static final int BAND_MODE_PCS = 7;
  public static final int BAND_MODE_JTACS = 8;
  public static final int BAND_MODE_KOREA_PCS = 9;
  public static final int BAND_MODE_5_450M = 10;
  public static final int BAND_MODE_IMT2000 = 11;
  public static final int BAND_MODE_7_700M_2 = 12;
  public static final int BAND_MODE_8_1800M = 13;
  public static final int BAND_MODE_9_900M = 14;
  public static final int BAND_MODE_10_800M_2 = 15;
  public static final int BAND_MODE_EURO_PAMR_400M = 16;
  public static final int BAND_MODE_AWS = 17;
  public static final int BAND_MODE_USA_2500M = 18;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == BAND_MODE_UNSPECIFIED) return "BAND_MODE_UNSPECIFIED";
      if (_aidl_v == BAND_MODE_EURO) return "BAND_MODE_EURO";
      if (_aidl_v == BAND_MODE_USA) return "BAND_MODE_USA";
      if (_aidl_v == BAND_MODE_JPN) return "BAND_MODE_JPN";
      if (_aidl_v == BAND_MODE_AUS) return "BAND_MODE_AUS";
      if (_aidl_v == BAND_MODE_AUS_2) return "BAND_MODE_AUS_2";
      if (_aidl_v == BAND_MODE_CELL_800) return "BAND_MODE_CELL_800";
      if (_aidl_v == BAND_MODE_PCS) return "BAND_MODE_PCS";
      if (_aidl_v == BAND_MODE_JTACS) return "BAND_MODE_JTACS";
      if (_aidl_v == BAND_MODE_KOREA_PCS) return "BAND_MODE_KOREA_PCS";
      if (_aidl_v == BAND_MODE_5_450M) return "BAND_MODE_5_450M";
      if (_aidl_v == BAND_MODE_IMT2000) return "BAND_MODE_IMT2000";
      if (_aidl_v == BAND_MODE_7_700M_2) return "BAND_MODE_7_700M_2";
      if (_aidl_v == BAND_MODE_8_1800M) return "BAND_MODE_8_1800M";
      if (_aidl_v == BAND_MODE_9_900M) return "BAND_MODE_9_900M";
      if (_aidl_v == BAND_MODE_10_800M_2) return "BAND_MODE_10_800M_2";
      if (_aidl_v == BAND_MODE_EURO_PAMR_400M) return "BAND_MODE_EURO_PAMR_400M";
      if (_aidl_v == BAND_MODE_AWS) return "BAND_MODE_AWS";
      if (_aidl_v == BAND_MODE_USA_2500M) return "BAND_MODE_USA_2500M";
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
