/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash 58d15e9e2c355be7b3dda6d4d34effd672bfd1cb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V3-java-source/gen/android/hardware/radio/RadioConst.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/3/android/hardware/radio/RadioConst.aidl
 */
package android.hardware.radio;
/** @hide */
public class RadioConst implements android.os.Parcelable
{
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RadioConst> CREATOR = new android.os.Parcelable.Creator<RadioConst>() {
    @Override
    public RadioConst createFromParcel(android.os.Parcel _aidl_source) {
      RadioConst _aidl_out = new RadioConst();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RadioConst[] newArray(int _aidl_size) {
      return new RadioConst[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
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
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int MAX_RILDS = 3;
  public static final int MAX_UUID_LENGTH = 64;
  public static final int CARD_MAX_APPS = 8;
  public static final int P2_CONSTANT_NO_P2 = -1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    return "RadioConst" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
