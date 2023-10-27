/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class UusInfo implements android.os.Parcelable
{
  public int uusType = 0;
  public int uusDcs = 0;
  public java.lang.String uusData;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<UusInfo> CREATOR = new android.os.Parcelable.Creator<UusInfo>() {
    @Override
    public UusInfo createFromParcel(android.os.Parcel _aidl_source) {
      UusInfo _aidl_out = new UusInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public UusInfo[] newArray(int _aidl_size) {
      return new UusInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(uusType);
    _aidl_parcel.writeInt(uusDcs);
    _aidl_parcel.writeString(uusData);
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
      uusType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uusDcs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uusData = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int UUS_DCS_USP = 0;
  public static final int UUS_DCS_OSIHLP = 1;
  public static final int UUS_DCS_X244 = 2;
  public static final int UUS_DCS_RMCF = 3;
  public static final int UUS_DCS_IA5C = 4;
  public static final int UUS_TYPE_TYPE1_IMPLICIT = 0;
  public static final int UUS_TYPE_TYPE1_REQUIRED = 1;
  public static final int UUS_TYPE_TYPE1_NOT_REQUIRED = 2;
  public static final int UUS_TYPE_TYPE2_REQUIRED = 3;
  public static final int UUS_TYPE_TYPE2_NOT_REQUIRED = 4;
  public static final int UUS_TYPE_TYPE3_REQUIRED = 5;
  public static final int UUS_TYPE_TYPE3_NOT_REQUIRED = 6;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("uusType: " + (uusType));
    _aidl_sj.add("uusDcs: " + (uusDcs));
    _aidl_sj.add("uusData: " + (java.util.Objects.toString(uusData)));
    return "android.hardware.radio.voice.UusInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
