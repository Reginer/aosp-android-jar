/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.tv.tunerresourcemanager;
/**
 * TunerDemuxInfo interface that carries tuner demux information.
 * 
 * This is used to update the TunerResourceManager demux resources.
 * @hide
 */
public class TunerDemuxInfo implements android.os.Parcelable
{
  /** Demux handle */
  public int handle = 0;
  /** Supported filter types (defined in {@link android.media.tv.tuner.filter.Filter}) */
  public int filterTypes = 0;
  public static final android.os.Parcelable.Creator<TunerDemuxInfo> CREATOR = new android.os.Parcelable.Creator<TunerDemuxInfo>() {
    @Override
    public TunerDemuxInfo createFromParcel(android.os.Parcel _aidl_source) {
      TunerDemuxInfo _aidl_out = new TunerDemuxInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TunerDemuxInfo[] newArray(int _aidl_size) {
      return new TunerDemuxInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(handle);
    _aidl_parcel.writeInt(filterTypes);
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
      filterTypes = _aidl_parcel.readInt();
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
