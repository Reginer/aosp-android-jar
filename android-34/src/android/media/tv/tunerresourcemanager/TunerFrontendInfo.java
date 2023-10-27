/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.tv.tunerresourcemanager;
/**
 * FrontendInfo interface that carries tuner frontend information.
 * 
 * This is used to update the TunerResourceManager fronted resources.
 * @hide
 */
public class TunerFrontendInfo implements android.os.Parcelable
{
  /** Frontend Handle */
  public int handle = 0;
  /** Frontend Type */
  public int type = 0;
  /**
   * Frontends are assigned with the same exclusiveGroupId if they can't
   * function at same time. For instance, they share same hardware module.
   */
  public int exclusiveGroupId = 0;
  public static final android.os.Parcelable.Creator<TunerFrontendInfo> CREATOR = new android.os.Parcelable.Creator<TunerFrontendInfo>() {
    @Override
    public TunerFrontendInfo createFromParcel(android.os.Parcel _aidl_source) {
      TunerFrontendInfo _aidl_out = new TunerFrontendInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TunerFrontendInfo[] newArray(int _aidl_size) {
      return new TunerFrontendInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(handle);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeInt(exclusiveGroupId);
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
      handle = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      exclusiveGroupId = _aidl_parcel.readInt();
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
