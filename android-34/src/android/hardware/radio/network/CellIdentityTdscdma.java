/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class CellIdentityTdscdma implements android.os.Parcelable
{
  public java.lang.String mcc;
  public java.lang.String mnc;
  public int lac = 0;
  public int cid = 0;
  public int cpid = 0;
  public int uarfcn = 0;
  public android.hardware.radio.network.OperatorInfo operatorNames;
  public java.lang.String[] additionalPlmns;
  public android.hardware.radio.network.ClosedSubscriberGroupInfo csgInfo;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CellIdentityTdscdma> CREATOR = new android.os.Parcelable.Creator<CellIdentityTdscdma>() {
    @Override
    public CellIdentityTdscdma createFromParcel(android.os.Parcel _aidl_source) {
      CellIdentityTdscdma _aidl_out = new CellIdentityTdscdma();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CellIdentityTdscdma[] newArray(int _aidl_size) {
      return new CellIdentityTdscdma[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(mcc);
    _aidl_parcel.writeString(mnc);
    _aidl_parcel.writeInt(lac);
    _aidl_parcel.writeInt(cid);
    _aidl_parcel.writeInt(cpid);
    _aidl_parcel.writeInt(uarfcn);
    _aidl_parcel.writeTypedObject(operatorNames, _aidl_flag);
    _aidl_parcel.writeStringArray(additionalPlmns);
    _aidl_parcel.writeTypedObject(csgInfo, _aidl_flag);
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
      mcc = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mnc = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lac = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cpid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uarfcn = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      operatorNames = _aidl_parcel.readTypedObject(android.hardware.radio.network.OperatorInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      additionalPlmns = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csgInfo = _aidl_parcel.readTypedObject(android.hardware.radio.network.ClosedSubscriberGroupInfo.CREATOR);
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
    _aidl_sj.add("mcc: " + (java.util.Objects.toString(mcc)));
    _aidl_sj.add("mnc: " + (java.util.Objects.toString(mnc)));
    _aidl_sj.add("lac: " + (lac));
    _aidl_sj.add("cid: " + (cid));
    _aidl_sj.add("cpid: " + (cpid));
    _aidl_sj.add("uarfcn: " + (uarfcn));
    _aidl_sj.add("operatorNames: " + (java.util.Objects.toString(operatorNames)));
    _aidl_sj.add("additionalPlmns: " + (java.util.Arrays.toString(additionalPlmns)));
    _aidl_sj.add("csgInfo: " + (java.util.Objects.toString(csgInfo)));
    return "android.hardware.radio.network.CellIdentityTdscdma" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(operatorNames);
    _mask |= describeContents(csgInfo);
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
