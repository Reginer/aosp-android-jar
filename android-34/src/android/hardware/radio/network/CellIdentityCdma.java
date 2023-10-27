/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class CellIdentityCdma implements android.os.Parcelable
{
  public int networkId = 0;
  public int systemId = 0;
  public int baseStationId = 0;
  public int longitude = 0;
  public int latitude = 0;
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
    return "android.hardware.radio.network.CellIdentityCdma" + _aidl_sj.toString()  ;
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
