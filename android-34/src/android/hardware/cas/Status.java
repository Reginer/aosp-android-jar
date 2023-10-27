/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.cas;
/** @hide */
public class Status implements android.os.Parcelable
{
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Status> CREATOR = new android.os.Parcelable.Creator<Status>() {
    @Override
    public Status createFromParcel(android.os.Parcel _aidl_source) {
      Status _aidl_out = new Status();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Status[] newArray(int _aidl_size) {
      return new Status[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
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
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int OK = 0;
  public static final int ERROR_CAS_NO_LICENSE = 1;
  public static final int ERROR_CAS_LICENSE_EXPIRED = 2;
  public static final int ERROR_CAS_SESSION_NOT_OPENED = 3;
  public static final int ERROR_CAS_CANNOT_HANDLE = 4;
  public static final int ERROR_CAS_INVALID_STATE = 5;
  public static final int BAD_VALUE = 6;
  public static final int ERROR_CAS_NOT_PROVISIONED = 7;
  public static final int ERROR_CAS_RESOURCE_BUSY = 8;
  public static final int ERROR_CAS_INSUFFICIENT_OUTPUT_PROTECTION = 9;
  public static final int ERROR_CAS_TAMPER_DETECTED = 10;
  public static final int ERROR_CAS_DEVICE_REVOKED = 11;
  public static final int ERROR_CAS_DECRYPT_UNIT_NOT_INITIALIZED = 12;
  public static final int ERROR_CAS_DECRYPT = 13;
  public static final int ERROR_CAS_UNKNOWN = 14;
  public static final int ERROR_CAS_NEED_ACTIVATION = 15;
  public static final int ERROR_CAS_NEED_PAIRING = 16;
  public static final int ERROR_CAS_NO_CARD = 17;
  public static final int ERROR_CAS_CARD_MUTE = 18;
  public static final int ERROR_CAS_CARD_INVALID = 19;
  public static final int ERROR_CAS_BLACKOUT = 20;
  public static final int ERROR_CAS_REBOOTING = 21;
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
