/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash f6e4f3bf2ea241a74ffac5643f8941921f0a2b98 --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V4-java-source/gen/android/hardware/radio/RadioResponseInfoModem.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio/4/android/hardware/radio/RadioResponseInfoModem.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio;
/** @hide */
public class RadioResponseInfoModem implements android.os.Parcelable
{
  public int type = android.hardware.radio.RadioResponseType.SOLICITED;
  public int serial = 0;
  public int error = android.hardware.radio.RadioError.NONE;
  public boolean isEnabled = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RadioResponseInfoModem> CREATOR = new android.os.Parcelable.Creator<RadioResponseInfoModem>() {
    @Override
    public RadioResponseInfoModem createFromParcel(android.os.Parcel _aidl_source) {
      RadioResponseInfoModem _aidl_out = new RadioResponseInfoModem();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RadioResponseInfoModem[] newArray(int _aidl_size) {
      return new RadioResponseInfoModem[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeInt(serial);
    _aidl_parcel.writeInt(error);
    _aidl_parcel.writeBoolean(isEnabled);
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
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      serial = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      error = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isEnabled = _aidl_parcel.readBoolean();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("type: " + (android.hardware.radio.RadioResponseType.$.toString(type)));
    _aidl_sj.add("serial: " + (serial));
    _aidl_sj.add("error: " + (android.hardware.radio.RadioError.$.toString(error)));
    _aidl_sj.add("isEnabled: " + (isEnabled));
    return "RadioResponseInfoModem" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
