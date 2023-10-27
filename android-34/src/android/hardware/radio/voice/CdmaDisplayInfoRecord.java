/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class CdmaDisplayInfoRecord implements android.os.Parcelable
{
  public java.lang.String alphaBuf;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaDisplayInfoRecord> CREATOR = new android.os.Parcelable.Creator<CdmaDisplayInfoRecord>() {
    @Override
    public CdmaDisplayInfoRecord createFromParcel(android.os.Parcel _aidl_source) {
      CdmaDisplayInfoRecord _aidl_out = new CdmaDisplayInfoRecord();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaDisplayInfoRecord[] newArray(int _aidl_size) {
      return new CdmaDisplayInfoRecord[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(alphaBuf);
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
      alphaBuf = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int CDMA_ALPHA_INFO_BUFFER_LENGTH = 64;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("alphaBuf: " + (java.util.Objects.toString(alphaBuf)));
    return "android.hardware.radio.voice.CdmaDisplayInfoRecord" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
