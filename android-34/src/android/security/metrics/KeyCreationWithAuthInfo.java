/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.metrics;
/**
 * Atom that encapsulates authentication related information in key creation events.
 * @hide
 */
public class KeyCreationWithAuthInfo implements android.os.Parcelable
{
  public int user_auth_type;
  /**
   * Base 10 logarithm of time out in seconds.
   * Logarithm is taken in order to reduce the cardinaltiy.
   */
  public int log10_auth_key_timeout_seconds = 0;
  public int security_level;
  public static final android.os.Parcelable.Creator<KeyCreationWithAuthInfo> CREATOR = new android.os.Parcelable.Creator<KeyCreationWithAuthInfo>() {
    @Override
    public KeyCreationWithAuthInfo createFromParcel(android.os.Parcel _aidl_source) {
      KeyCreationWithAuthInfo _aidl_out = new KeyCreationWithAuthInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public KeyCreationWithAuthInfo[] newArray(int _aidl_size) {
      return new KeyCreationWithAuthInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(user_auth_type);
    _aidl_parcel.writeInt(log10_auth_key_timeout_seconds);
    _aidl_parcel.writeInt(security_level);
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
      user_auth_type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      log10_auth_key_timeout_seconds = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      security_level = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
