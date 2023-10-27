/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public @interface UtranBands {
  public static final int BAND_1 = 1;
  public static final int BAND_2 = 2;
  public static final int BAND_3 = 3;
  public static final int BAND_4 = 4;
  public static final int BAND_5 = 5;
  public static final int BAND_6 = 6;
  public static final int BAND_7 = 7;
  public static final int BAND_8 = 8;
  public static final int BAND_9 = 9;
  public static final int BAND_10 = 10;
  public static final int BAND_11 = 11;
  public static final int BAND_12 = 12;
  public static final int BAND_13 = 13;
  public static final int BAND_14 = 14;
  public static final int BAND_19 = 19;
  public static final int BAND_20 = 20;
  public static final int BAND_21 = 21;
  public static final int BAND_22 = 22;
  public static final int BAND_25 = 25;
  public static final int BAND_26 = 26;
  public static final int BAND_A = 101;
  public static final int BAND_B = 102;
  public static final int BAND_C = 103;
  public static final int BAND_D = 104;
  public static final int BAND_E = 105;
  public static final int BAND_F = 106;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == BAND_1) return "BAND_1";
      if (_aidl_v == BAND_2) return "BAND_2";
      if (_aidl_v == BAND_3) return "BAND_3";
      if (_aidl_v == BAND_4) return "BAND_4";
      if (_aidl_v == BAND_5) return "BAND_5";
      if (_aidl_v == BAND_6) return "BAND_6";
      if (_aidl_v == BAND_7) return "BAND_7";
      if (_aidl_v == BAND_8) return "BAND_8";
      if (_aidl_v == BAND_9) return "BAND_9";
      if (_aidl_v == BAND_10) return "BAND_10";
      if (_aidl_v == BAND_11) return "BAND_11";
      if (_aidl_v == BAND_12) return "BAND_12";
      if (_aidl_v == BAND_13) return "BAND_13";
      if (_aidl_v == BAND_14) return "BAND_14";
      if (_aidl_v == BAND_19) return "BAND_19";
      if (_aidl_v == BAND_20) return "BAND_20";
      if (_aidl_v == BAND_21) return "BAND_21";
      if (_aidl_v == BAND_22) return "BAND_22";
      if (_aidl_v == BAND_25) return "BAND_25";
      if (_aidl_v == BAND_26) return "BAND_26";
      if (_aidl_v == BAND_A) return "BAND_A";
      if (_aidl_v == BAND_B) return "BAND_B";
      if (_aidl_v == BAND_C) return "BAND_C";
      if (_aidl_v == BAND_D) return "BAND_D";
      if (_aidl_v == BAND_E) return "BAND_E";
      if (_aidl_v == BAND_F) return "BAND_F";
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
