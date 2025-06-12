/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash fc957f1d3d261d065ff5e5415f2d21caa79c310f -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/gnss/aidl/android.hardware.gnss-V2-java-source/gen/android/hardware/gnss/GnssSignalType.java.d -o out/soong/.intermediates/hardware/interfaces/gnss/aidl/android.hardware.gnss-V2-java-source/gen -Nhardware/interfaces/gnss/aidl/aidl_api/android.hardware.gnss/2 hardware/interfaces/gnss/aidl/aidl_api/android.hardware.gnss/2/android/hardware/gnss/GnssSignalType.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.gnss;
/** @hide */
public class GnssSignalType implements android.os.Parcelable
{
  public int constellation = android.hardware.gnss.GnssConstellationType.UNKNOWN;
  public double carrierFrequencyHz = 0.000000;
  public java.lang.String codeType;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GnssSignalType> CREATOR = new android.os.Parcelable.Creator<GnssSignalType>() {
    @Override
    public GnssSignalType createFromParcel(android.os.Parcel _aidl_source) {
      GnssSignalType _aidl_out = new GnssSignalType();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GnssSignalType[] newArray(int _aidl_size) {
      return new GnssSignalType[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(constellation);
    _aidl_parcel.writeDouble(carrierFrequencyHz);
    _aidl_parcel.writeString(codeType);
    int _aidl_end_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.setDataPosition(_aidl_start_pos);
    _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
    _aidl_parcel.setDataPosition(_aidl_end_pos);
  }
  public final void readFromParcel(android.os.Parcel _aidl_parcel)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    int _aidl_parcelable_size = _aidl_parcel.readInt();
    try {
      if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small");;
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      constellation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      carrierFrequencyHz = _aidl_parcel.readDouble();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      codeType = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final String CODE_TYPE_A = "A";
  public static final String CODE_TYPE_B = "B";
  public static final String CODE_TYPE_C = "C";
  public static final String CODE_TYPE_D = "D";
  public static final String CODE_TYPE_I = "I";
  public static final String CODE_TYPE_L = "L";
  public static final String CODE_TYPE_M = "M";
  public static final String CODE_TYPE_N = "N";
  public static final String CODE_TYPE_P = "P";
  public static final String CODE_TYPE_Q = "Q";
  public static final String CODE_TYPE_S = "S";
  public static final String CODE_TYPE_W = "W";
  public static final String CODE_TYPE_X = "X";
  public static final String CODE_TYPE_Y = "Y";
  public static final String CODE_TYPE_Z = "Z";
  public static final String CODE_TYPE_UNKNOWN = "UNKNOWN";
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
