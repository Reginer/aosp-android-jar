/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash ec0dfedf764f3916783848c540ad312a74fa755d --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V2-java-source/gen/android/hardware/radio/ims/ImsDeregistrationReason.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.ims-V2-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/2 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.ims/2/android/hardware/radio/ims/ImsDeregistrationReason.aidl
 */
package android.hardware.radio.ims;
/** @hide */
public @interface ImsDeregistrationReason {
  public static final int REASON_SIM_REMOVED = 1;
  public static final int REASON_SIM_REFRESH = 2;
  public static final int REASON_ALLOWED_NETWORK_TYPES_CHANGED = 3;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == REASON_SIM_REMOVED) return "REASON_SIM_REMOVED";
      if (_aidl_v == REASON_SIM_REFRESH) return "REASON_SIM_REFRESH";
      if (_aidl_v == REASON_ALLOWED_NETWORK_TYPES_CHANGED) return "REASON_ALLOWED_NETWORK_TYPES_CHANGED";
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
