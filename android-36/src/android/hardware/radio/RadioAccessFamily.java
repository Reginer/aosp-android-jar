/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash f6e4f3bf2ea241a74ffac5643f8941921f0a2b98 --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V4-java-source/gen/android/hardware/radio/RadioAccessFamily.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/4/android/hardware/radio/RadioAccessFamily.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio;
/** @hide */
public @interface RadioAccessFamily {
  public static final int UNKNOWN = 1;
  public static final int GPRS = 2;
  public static final int EDGE = 4;
  public static final int UMTS = 8;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int IS95A = 16;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int IS95B = 32;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int ONE_X_RTT = 64;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int EVDO_0 = 128;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int EVDO_A = 256;
  public static final int HSDPA = 512;
  public static final int HSUPA = 1024;
  public static final int HSPA = 2048;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int EVDO_B = 4096;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public static final int EHRPD = 8192;
  public static final int LTE = 16384;
  public static final int HSPAP = 32768;
  public static final int GSM = 65536;
  public static final int TD_SCDMA = 131072;
  public static final int IWLAN = 262144;
  /** @deprecated use LTE instead. */
  @Deprecated
  public static final int LTE_CA = 524288;
  public static final int NR = 1048576;
  interface $ {
    static String toString(int _aidl_v) {
      if (_aidl_v == UNKNOWN) return "UNKNOWN";
      if (_aidl_v == GPRS) return "GPRS";
      if (_aidl_v == EDGE) return "EDGE";
      if (_aidl_v == UMTS) return "UMTS";
      if (_aidl_v == IS95A) return "IS95A";
      if (_aidl_v == IS95B) return "IS95B";
      if (_aidl_v == ONE_X_RTT) return "ONE_X_RTT";
      if (_aidl_v == EVDO_0) return "EVDO_0";
      if (_aidl_v == EVDO_A) return "EVDO_A";
      if (_aidl_v == HSDPA) return "HSDPA";
      if (_aidl_v == HSUPA) return "HSUPA";
      if (_aidl_v == HSPA) return "HSPA";
      if (_aidl_v == EVDO_B) return "EVDO_B";
      if (_aidl_v == EHRPD) return "EHRPD";
      if (_aidl_v == LTE) return "LTE";
      if (_aidl_v == HSPAP) return "HSPAP";
      if (_aidl_v == GSM) return "GSM";
      if (_aidl_v == TD_SCDMA) return "TD_SCDMA";
      if (_aidl_v == IWLAN) return "IWLAN";
      if (_aidl_v == LTE_CA) return "LTE_CA";
      if (_aidl_v == NR) return "NR";
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
