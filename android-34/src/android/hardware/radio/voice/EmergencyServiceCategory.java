/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public @interface EmergencyServiceCategory {
  public static final int UNSPECIFIED = 0;
  public static final int POLICE = 1;
  public static final int AMBULANCE = 2;
  public static final int FIRE_BRIGADE = 4;
  public static final int MARINE_GUARD = 8;
  public static final int MOUNTAIN_RESCUE = 16;
  public static final int MIEC = 32;
  public static final int AIEC = 64;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNSPECIFIED) return "UNSPECIFIED";
      if (_aidl_v == POLICE) return "POLICE";
      if (_aidl_v == AMBULANCE) return "AMBULANCE";
      if (_aidl_v == FIRE_BRIGADE) return "FIRE_BRIGADE";
      if (_aidl_v == MARINE_GUARD) return "MARINE_GUARD";
      if (_aidl_v == MOUNTAIN_RESCUE) return "MOUNTAIN_RESCUE";
      if (_aidl_v == MIEC) return "MIEC";
      if (_aidl_v == AIEC) return "AIEC";
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
