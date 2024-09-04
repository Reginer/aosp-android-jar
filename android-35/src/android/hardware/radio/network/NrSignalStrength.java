/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash c45c122528c07c449ea08f6eacaace17bb7abc38 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen/android/hardware/radio/network/NrSignalStrength.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/3/android/hardware/radio/network/NrSignalStrength.aidl
 */
package android.hardware.radio.network;
/** @hide */
public class NrSignalStrength implements android.os.Parcelable
{
  public int ssRsrp = 0;
  public int ssRsrq = 0;
  public int ssSinr = 0;
  public int csiRsrp = 0;
  public int csiRsrq = 0;
  public int csiSinr = 0;
  public int csiCqiTableIndex = 0;
  public byte[] csiCqiReport;
  public int timingAdvance = 2147483647;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<NrSignalStrength> CREATOR = new android.os.Parcelable.Creator<NrSignalStrength>() {
    @Override
    public NrSignalStrength createFromParcel(android.os.Parcel _aidl_source) {
      NrSignalStrength _aidl_out = new NrSignalStrength();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public NrSignalStrength[] newArray(int _aidl_size) {
      return new NrSignalStrength[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(ssRsrp);
    _aidl_parcel.writeInt(ssRsrq);
    _aidl_parcel.writeInt(ssSinr);
    _aidl_parcel.writeInt(csiRsrp);
    _aidl_parcel.writeInt(csiRsrq);
    _aidl_parcel.writeInt(csiSinr);
    _aidl_parcel.writeInt(csiCqiTableIndex);
    _aidl_parcel.writeByteArray(csiCqiReport);
    _aidl_parcel.writeInt(timingAdvance);
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
      ssRsrp = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ssRsrq = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ssSinr = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csiRsrp = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csiRsrq = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csiSinr = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csiCqiTableIndex = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csiCqiReport = _aidl_parcel.createByteArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timingAdvance = _aidl_parcel.readInt();
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
    _aidl_sj.add("ssRsrp: " + (ssRsrp));
    _aidl_sj.add("ssRsrq: " + (ssRsrq));
    _aidl_sj.add("ssSinr: " + (ssSinr));
    _aidl_sj.add("csiRsrp: " + (csiRsrp));
    _aidl_sj.add("csiRsrq: " + (csiRsrq));
    _aidl_sj.add("csiSinr: " + (csiSinr));
    _aidl_sj.add("csiCqiTableIndex: " + (csiCqiTableIndex));
    _aidl_sj.add("csiCqiReport: " + (java.util.Arrays.toString(csiCqiReport)));
    _aidl_sj.add("timingAdvance: " + (timingAdvance));
    return "NrSignalStrength" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
