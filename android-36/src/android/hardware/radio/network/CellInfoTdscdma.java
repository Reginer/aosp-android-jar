/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 5867b4f5be491ec815fafea8a3f268b0295427df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen/android/hardware/radio/network/CellInfoTdscdma.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4/android/hardware/radio/network/CellInfoTdscdma.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.network;
/** @hide */
public class CellInfoTdscdma implements android.os.Parcelable
{
  public android.hardware.radio.network.CellIdentityTdscdma cellIdentityTdscdma;
  public android.hardware.radio.network.TdscdmaSignalStrength signalStrengthTdscdma;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CellInfoTdscdma> CREATOR = new android.os.Parcelable.Creator<CellInfoTdscdma>() {
    @Override
    public CellInfoTdscdma createFromParcel(android.os.Parcel _aidl_source) {
      CellInfoTdscdma _aidl_out = new CellInfoTdscdma();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CellInfoTdscdma[] newArray(int _aidl_size) {
      return new CellInfoTdscdma[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(cellIdentityTdscdma, _aidl_flag);
    _aidl_parcel.writeTypedObject(signalStrengthTdscdma, _aidl_flag);
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
      cellIdentityTdscdma = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentityTdscdma.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      signalStrengthTdscdma = _aidl_parcel.readTypedObject(android.hardware.radio.network.TdscdmaSignalStrength.CREATOR);
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
    _aidl_sj.add("cellIdentityTdscdma: " + (java.util.Objects.toString(cellIdentityTdscdma)));
    _aidl_sj.add("signalStrengthTdscdma: " + (java.util.Objects.toString(signalStrengthTdscdma)));
    return "CellInfoTdscdma" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(cellIdentityTdscdma);
    _mask |= describeContents(signalStrengthTdscdma);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
