/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.ims;
public class ConnectionFailureInfo implements android.os.Parcelable
{
  public int failureReason;
  public int causeCode = 0;
  public int waitTimeMillis = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ConnectionFailureInfo> CREATOR = new android.os.Parcelable.Creator<ConnectionFailureInfo>() {
    @Override
    public ConnectionFailureInfo createFromParcel(android.os.Parcel _aidl_source) {
      ConnectionFailureInfo _aidl_out = new ConnectionFailureInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ConnectionFailureInfo[] newArray(int _aidl_size) {
      return new ConnectionFailureInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(failureReason);
    _aidl_parcel.writeInt(causeCode);
    _aidl_parcel.writeInt(waitTimeMillis);
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
      failureReason = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      causeCode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      waitTimeMillis = _aidl_parcel.readInt();
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
    _aidl_sj.add("failureReason: " + (failureReason));
    _aidl_sj.add("causeCode: " + (causeCode));
    _aidl_sj.add("waitTimeMillis: " + (waitTimeMillis));
    return "android.hardware.radio.ims.ConnectionFailureInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
  public static @interface ConnectionFailureReason {
    public static final int REASON_ACCESS_DENIED = 1;
    public static final int REASON_NAS_FAILURE = 2;
    public static final int REASON_RACH_FAILURE = 3;
    public static final int REASON_RLC_FAILURE = 4;
    public static final int REASON_RRC_REJECT = 5;
    public static final int REASON_RRC_TIMEOUT = 6;
    public static final int REASON_NO_SERVICE = 7;
    public static final int REASON_PDN_NOT_AVAILABLE = 8;
    public static final int REASON_RF_BUSY = 9;
    public static final int REASON_UNSPECIFIED = 65535;
  }
}
