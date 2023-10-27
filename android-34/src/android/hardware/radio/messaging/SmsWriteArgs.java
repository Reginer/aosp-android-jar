/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.messaging;
public class SmsWriteArgs implements android.os.Parcelable
{
  public int status = 0;
  public java.lang.String pdu;
  public java.lang.String smsc;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SmsWriteArgs> CREATOR = new android.os.Parcelable.Creator<SmsWriteArgs>() {
    @Override
    public SmsWriteArgs createFromParcel(android.os.Parcel _aidl_source) {
      SmsWriteArgs _aidl_out = new SmsWriteArgs();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SmsWriteArgs[] newArray(int _aidl_size) {
      return new SmsWriteArgs[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(status);
    _aidl_parcel.writeString(pdu);
    _aidl_parcel.writeString(smsc);
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
      pdu = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      smsc = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int STATUS_REC_UNREAD = 0;
  public static final int STATUS_REC_READ = 1;
  public static final int STATUS_STO_UNSENT = 2;
  public static final int STATUS_STO_SENT = 3;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("status: " + (status));
    _aidl_sj.add("pdu: " + (java.util.Objects.toString(pdu)));
    _aidl_sj.add("smsc: " + (java.util.Objects.toString(smsc)));
    return "android.hardware.radio.messaging.SmsWriteArgs" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
