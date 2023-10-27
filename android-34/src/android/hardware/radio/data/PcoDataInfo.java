/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public class PcoDataInfo implements android.os.Parcelable
{
  public int cid = 0;
  public java.lang.String bearerProto;
  public int pcoId = 0;
  public byte[] contents;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<PcoDataInfo> CREATOR = new android.os.Parcelable.Creator<PcoDataInfo>() {
    @Override
    public PcoDataInfo createFromParcel(android.os.Parcel _aidl_source) {
      PcoDataInfo _aidl_out = new PcoDataInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PcoDataInfo[] newArray(int _aidl_size) {
      return new PcoDataInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(cid);
    _aidl_parcel.writeString(bearerProto);
    _aidl_parcel.writeInt(pcoId);
    _aidl_parcel.writeByteArray(contents);
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
      cid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bearerProto = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pcoId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      contents = _aidl_parcel.createByteArray();
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
    _aidl_sj.add("cid: " + (cid));
    _aidl_sj.add("bearerProto: " + (java.util.Objects.toString(bearerProto)));
    _aidl_sj.add("pcoId: " + (pcoId));
    _aidl_sj.add("contents: " + (java.util.Arrays.toString(contents)));
    return "android.hardware.radio.data.PcoDataInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
