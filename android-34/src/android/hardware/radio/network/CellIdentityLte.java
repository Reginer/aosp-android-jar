/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class CellIdentityLte implements android.os.Parcelable
{
  public java.lang.String mcc;
  public java.lang.String mnc;
  public int ci = 0;
  public int pci = 0;
  public int tac = 0;
  public int earfcn = 0;
  public android.hardware.radio.network.OperatorInfo operatorNames;
  public int bandwidth = 0;
  public java.lang.String[] additionalPlmns;
  public android.hardware.radio.network.ClosedSubscriberGroupInfo csgInfo;
  public int[] bands;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CellIdentityLte> CREATOR = new android.os.Parcelable.Creator<CellIdentityLte>() {
    @Override
    public CellIdentityLte createFromParcel(android.os.Parcel _aidl_source) {
      CellIdentityLte _aidl_out = new CellIdentityLte();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CellIdentityLte[] newArray(int _aidl_size) {
      return new CellIdentityLte[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(mcc);
    _aidl_parcel.writeString(mnc);
    _aidl_parcel.writeInt(ci);
    _aidl_parcel.writeInt(pci);
    _aidl_parcel.writeInt(tac);
    _aidl_parcel.writeInt(earfcn);
    _aidl_parcel.writeTypedObject(operatorNames, _aidl_flag);
    _aidl_parcel.writeInt(bandwidth);
    _aidl_parcel.writeStringArray(additionalPlmns);
    _aidl_parcel.writeTypedObject(csgInfo, _aidl_flag);
    _aidl_parcel.writeIntArray(bands);
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
      ci = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pci = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tac = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      earfcn = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      operatorNames = _aidl_parcel.readTypedObject(android.hardware.radio.network.OperatorInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bandwidth = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      additionalPlmns = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csgInfo = _aidl_parcel.readTypedObject(android.hardware.radio.network.ClosedSubscriberGroupInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bands = _aidl_parcel.createIntArray();
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
    _aidl_sj.add("ci: " + (ci));
    _aidl_sj.add("pci: " + (pci));
    _aidl_sj.add("tac: " + (tac));
    _aidl_sj.add("earfcn: " + (earfcn));
    _aidl_sj.add("operatorNames: " + (java.util.Objects.toString(operatorNames)));
    _aidl_sj.add("bandwidth: " + (bandwidth));
    _aidl_sj.add("additionalPlmns: " + (java.util.Arrays.toString(additionalPlmns)));
    _aidl_sj.add("csgInfo: " + (java.util.Objects.toString(csgInfo)));
    _aidl_sj.add("bands: " + (android.hardware.radio.network.EutranBands.$.arrayToString(bands)));
    return "android.hardware.radio.network.CellIdentityLte" + _aidl_sj.toString()  ;
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
