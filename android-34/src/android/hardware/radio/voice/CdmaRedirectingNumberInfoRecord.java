/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class CdmaRedirectingNumberInfoRecord implements android.os.Parcelable
{
  public android.hardware.radio.voice.CdmaNumberInfoRecord redirectingNumber;
  public int redirectingReason = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaRedirectingNumberInfoRecord> CREATOR = new android.os.Parcelable.Creator<CdmaRedirectingNumberInfoRecord>() {
    @Override
    public CdmaRedirectingNumberInfoRecord createFromParcel(android.os.Parcel _aidl_source) {
      CdmaRedirectingNumberInfoRecord _aidl_out = new CdmaRedirectingNumberInfoRecord();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaRedirectingNumberInfoRecord[] newArray(int _aidl_size) {
      return new CdmaRedirectingNumberInfoRecord[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(redirectingNumber, _aidl_flag);
    _aidl_parcel.writeInt(redirectingReason);
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
      redirectingNumber = _aidl_parcel.readTypedObject(android.hardware.radio.voice.CdmaNumberInfoRecord.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      redirectingReason = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int REDIRECTING_REASON_UNKNOWN = 0;
  public static final int REDIRECTING_REASON_CALL_FORWARDING_BUSY = 1;
  public static final int REDIRECTING_REASON_CALL_FORWARDING_NO_REPLY = 2;
  public static final int REDIRECTING_REASON_CALLED_DTE_OUT_OF_ORDER = 9;
  public static final int REDIRECTING_REASON_CALL_FORWARDING_BY_THE_CALLED_DTE = 10;
  public static final int REDIRECTING_REASON_CALL_FORWARDING_UNCONDITIONAL = 15;
  public static final int REDIRECTING_REASON_RESERVED = 16;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("redirectingNumber: " + (java.util.Objects.toString(redirectingNumber)));
    _aidl_sj.add("redirectingReason: " + (redirectingReason));
    return "android.hardware.radio.voice.CdmaRedirectingNumberInfoRecord" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(redirectingNumber);
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
