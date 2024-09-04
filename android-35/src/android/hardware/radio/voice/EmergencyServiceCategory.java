/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 78fb79bcb32590a868b3eb7affb39ab90e4ca782 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen/android/hardware/radio/voice/EmergencyServiceCategory.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.voice-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.voice/3/android/hardware/radio/voice/EmergencyServiceCategory.aidl
 */
package android.hardware.radio.voice;
/** @hide */
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
