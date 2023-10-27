/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class OperatorInfo implements android.os.Parcelable
{
  public java.lang.String alphaLong;
  public java.lang.String alphaShort;
  public java.lang.String operatorNumeric;
  public int status = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<OperatorInfo> CREATOR = new android.os.Parcelable.Creator<OperatorInfo>() {
    @Override
    public OperatorInfo createFromParcel(android.os.Parcel _aidl_source) {
      OperatorInfo _aidl_out = new OperatorInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public OperatorInfo[] newArray(int _aidl_size) {
      return new OperatorInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(alphaLong);
    _aidl_parcel.writeString(alphaShort);
    _aidl_parcel.writeString(operatorNumeric);
    _aidl_parcel.writeInt(status);
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
      alphaLong = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      alphaShort = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      operatorNumeric = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      status = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int STATUS_UNKNOWN = 0;
  public static final int STATUS_AVAILABLE = 1;
  public static final int STATUS_CURRENT = 2;
  public static final int STATUS_FORBIDDEN = 3;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("alphaLong: " + (java.util.Objects.toString(alphaLong)));
    _aidl_sj.add("alphaShort: " + (java.util.Objects.toString(alphaShort)));
    _aidl_sj.add("operatorNumeric: " + (java.util.Objects.toString(operatorNumeric)));
    _aidl_sj.add("status: " + (status));
    return "android.hardware.radio.network.OperatorInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
