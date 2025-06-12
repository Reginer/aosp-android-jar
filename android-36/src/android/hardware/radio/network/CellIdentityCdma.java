/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 5867b4f5be491ec815fafea8a3f268b0295427df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen/android/hardware/radio/network/CellIdentityCdma.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.network-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.network/4/android/hardware/radio/network/CellIdentityCdma.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.network;
/** @hide */
public class CellIdentityCdma implements android.os.Parcelable
{
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int networkId = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int systemId = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int baseStationId = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int longitude = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public int latitude = 0;
  /** @deprecated Legacy CDMA is unsupported. */
  @Deprecated
  public android.hardware.radio.network.OperatorInfo operatorNames;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CellIdentityCdma> CREATOR = new android.os.Parcelable.Creator<CellIdentityCdma>() {
    @Override
    public CellIdentityCdma createFromParcel(android.os.Parcel _aidl_source) {
      CellIdentityCdma _aidl_out = new CellIdentityCdma();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CellIdentityCdma[] newArray(int _aidl_size) {
      return new CellIdentityCdma[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(networkId);
    _aidl_parcel.writeInt(systemId);
    _aidl_parcel.writeInt(baseStationId);
    _aidl_parcel.writeInt(longitude);
    _aidl_parcel.writeInt(latitude);
    _aidl_parcel.writeTypedObject(operatorNames, _aidl_flag);
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
      networkId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      systemId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      baseStationId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      longitude = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      latitude = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      operatorNames = _aidl_parcel.readTypedObject(android.hardware.radio.network.OperatorInfo.CREATOR);
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
    _aidl_sj.add("networkId: " + (networkId));
    _aidl_sj.add("systemId: " + (systemId));
    _aidl_sj.add("baseStationId: " + (baseStationId));
    _aidl_sj.add("longitude: " + (longitude));
    _aidl_sj.add("latitude: " + (latitude));
    _aidl_sj.add("operatorNames: " + (java.util.Objects.toString(operatorNames)));
    return "CellIdentityCdma" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(operatorNames);
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
