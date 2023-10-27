/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class CellIdentityNr implements android.os.Parcelable
{
  public java.lang.String mcc;
  public java.lang.String mnc;
  public long nci = 0L;
  public int pci = 0;
  public int tac = 0;
  public int nrarfcn = 0;
  public android.hardware.radio.network.OperatorInfo operatorNames;
  public java.lang.String[] additionalPlmns;
  public int[] bands;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CellIdentityNr> CREATOR = new android.os.Parcelable.Creator<CellIdentityNr>() {
    @Override
    public CellIdentityNr createFromParcel(android.os.Parcel _aidl_source) {
      CellIdentityNr _aidl_out = new CellIdentityNr();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CellIdentityNr[] newArray(int _aidl_size) {
      return new CellIdentityNr[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(mcc);
    _aidl_parcel.writeString(mnc);
    _aidl_parcel.writeLong(nci);
    _aidl_parcel.writeInt(pci);
    _aidl_parcel.writeInt(tac);
    _aidl_parcel.writeInt(nrarfcn);
    _aidl_parcel.writeTypedObject(operatorNames, _aidl_flag);
    _aidl_parcel.writeStringArray(additionalPlmns);
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
      nci = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pci = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tac = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nrarfcn = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      operatorNames = _aidl_parcel.readTypedObject(android.hardware.radio.network.OperatorInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      additionalPlmns = _aidl_parcel.createStringArray();
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
    _aidl_sj.add("nci: " + (nci));
    _aidl_sj.add("pci: " + (pci));
    _aidl_sj.add("tac: " + (tac));
    _aidl_sj.add("nrarfcn: " + (nrarfcn));
    _aidl_sj.add("operatorNames: " + (java.util.Objects.toString(operatorNames)));
    _aidl_sj.add("additionalPlmns: " + (java.util.Arrays.toString(additionalPlmns)));
    _aidl_sj.add("bands: " + (android.hardware.radio.network.NgranBands.$.arrayToString(bands)));
    return "android.hardware.radio.network.CellIdentityNr" + _aidl_sj.toString()  ;
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
