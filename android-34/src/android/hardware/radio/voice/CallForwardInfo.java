/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class CallForwardInfo implements android.os.Parcelable
{
  public int status = 0;
  public int reason = 0;
  public int serviceClass = 0;
  public int toa = 0;
  public java.lang.String number;
  public int timeSeconds = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CallForwardInfo> CREATOR = new android.os.Parcelable.Creator<CallForwardInfo>() {
    @Override
    public CallForwardInfo createFromParcel(android.os.Parcel _aidl_source) {
      CallForwardInfo _aidl_out = new CallForwardInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CallForwardInfo[] newArray(int _aidl_size) {
      return new CallForwardInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(status);
    _aidl_parcel.writeInt(reason);
    _aidl_parcel.writeInt(serviceClass);
    _aidl_parcel.writeInt(toa);
    _aidl_parcel.writeString(number);
    _aidl_parcel.writeInt(timeSeconds);
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
      status = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      reason = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      serviceClass = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      toa = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      number = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeSeconds = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int STATUS_DISABLE = 0;
  public static final int STATUS_ENABLE = 1;
  public static final int STATUS_INTERROGATE = 2;
  public static final int STATUS_REGISTRATION = 3;
  public static final int STATUS_ERASURE = 4;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("status: " + (status));
    _aidl_sj.add("reason: " + (reason));
    _aidl_sj.add("serviceClass: " + (serviceClass));
    _aidl_sj.add("toa: " + (toa));
    _aidl_sj.add("number: " + (java.util.Objects.toString(number)));
    _aidl_sj.add("timeSeconds: " + (timeSeconds));
    return "android.hardware.radio.voice.CallForwardInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
