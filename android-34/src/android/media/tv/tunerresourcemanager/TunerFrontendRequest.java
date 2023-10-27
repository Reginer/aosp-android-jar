/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.tv.tunerresourcemanager;
/**
 * Information required to request a Tuner Frontend.
 * 
 * @hide
 */
public class TunerFrontendRequest implements android.os.Parcelable
{
  public int clientId = 0;
  public int frontendType = 0;
  public int desiredId = -1;
  public static final android.os.Parcelable.Creator<TunerFrontendRequest> CREATOR = new android.os.Parcelable.Creator<TunerFrontendRequest>() {
    @Override
    public TunerFrontendRequest createFromParcel(android.os.Parcel _aidl_source) {
      TunerFrontendRequest _aidl_out = new TunerFrontendRequest();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TunerFrontendRequest[] newArray(int _aidl_size) {
      return new TunerFrontendRequest[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(clientId);
    _aidl_parcel.writeInt(frontendType);
    _aidl_parcel.writeInt(desiredId);
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
      clientId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      frontendType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      desiredId = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int DEFAULT_DESIRED_ID = -1;
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
