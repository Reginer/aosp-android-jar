/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.metrics;
/**
 * Atom that encapsulates a set of general information in key creation events.
 * @hide
 */
public class KeyCreationWithGeneralInfo implements android.os.Parcelable
{
  public int algorithm;
  public int key_size = 0;
  public int ec_curve;
  public int key_origin;
  public int error_code = 0;
  public boolean attestation_requested = false;
  public static final android.os.Parcelable.Creator<KeyCreationWithGeneralInfo> CREATOR = new android.os.Parcelable.Creator<KeyCreationWithGeneralInfo>() {
    @Override
    public KeyCreationWithGeneralInfo createFromParcel(android.os.Parcel _aidl_source) {
      KeyCreationWithGeneralInfo _aidl_out = new KeyCreationWithGeneralInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public KeyCreationWithGeneralInfo[] newArray(int _aidl_size) {
      return new KeyCreationWithGeneralInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(algorithm);
    _aidl_parcel.writeInt(key_size);
    _aidl_parcel.writeInt(ec_curve);
    _aidl_parcel.writeInt(key_origin);
    _aidl_parcel.writeInt(error_code);
    _aidl_parcel.writeBoolean(attestation_requested);
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
      algorithm = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      key_size = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      ec_curve = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      key_origin = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      error_code = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      attestation_requested = _aidl_parcel.readBoolean();
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
