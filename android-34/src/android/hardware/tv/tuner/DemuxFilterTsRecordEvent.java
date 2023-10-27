/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class DemuxFilterTsRecordEvent implements android.os.Parcelable
{
  public android.hardware.tv.tuner.DemuxPid pid;
  public int tsIndexMask = 0;
  public android.hardware.tv.tuner.DemuxFilterScIndexMask scIndexMask;
  public long byteNumber = 0L;
  public long pts = 0L;
  public int firstMbInSlice = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<DemuxFilterTsRecordEvent> CREATOR = new android.os.Parcelable.Creator<DemuxFilterTsRecordEvent>() {
    @Override
    public DemuxFilterTsRecordEvent createFromParcel(android.os.Parcel _aidl_source) {
      DemuxFilterTsRecordEvent _aidl_out = new DemuxFilterTsRecordEvent();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public DemuxFilterTsRecordEvent[] newArray(int _aidl_size) {
      return new DemuxFilterTsRecordEvent[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(pid, _aidl_flag);
    _aidl_parcel.writeInt(tsIndexMask);
    _aidl_parcel.writeTypedObject(scIndexMask, _aidl_flag);
    _aidl_parcel.writeLong(byteNumber);
    _aidl_parcel.writeLong(pts);
    _aidl_parcel.writeInt(firstMbInSlice);
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
      pid = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxPid.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tsIndexMask = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      scIndexMask = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterScIndexMask.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      byteNumber = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pts = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      firstMbInSlice = _aidl_parcel.readInt();
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
    _mask |= describeContents(pid);
    _mask |= describeContents(scIndexMask);
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
