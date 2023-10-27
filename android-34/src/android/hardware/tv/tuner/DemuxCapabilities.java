/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class DemuxCapabilities implements android.os.Parcelable
{
  public int numDemux = 0;
  public int numRecord = 0;
  public int numPlayback = 0;
  public int numTsFilter = 0;
  public int numSectionFilter = 0;
  public int numAudioFilter = 0;
  public int numVideoFilter = 0;
  public int numPesFilter = 0;
  public int numPcrFilter = 0;
  public long numBytesInSectionFilter = 0L;
  public int filterCaps = 0;
  public int[] linkCaps;
  public boolean bTimeFilter = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<DemuxCapabilities> CREATOR = new android.os.Parcelable.Creator<DemuxCapabilities>() {
    @Override
    public DemuxCapabilities createFromParcel(android.os.Parcel _aidl_source) {
      DemuxCapabilities _aidl_out = new DemuxCapabilities();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public DemuxCapabilities[] newArray(int _aidl_size) {
      return new DemuxCapabilities[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(numDemux);
    _aidl_parcel.writeInt(numRecord);
    _aidl_parcel.writeInt(numPlayback);
    _aidl_parcel.writeInt(numTsFilter);
    _aidl_parcel.writeInt(numSectionFilter);
    _aidl_parcel.writeInt(numAudioFilter);
    _aidl_parcel.writeInt(numVideoFilter);
    _aidl_parcel.writeInt(numPesFilter);
    _aidl_parcel.writeInt(numPcrFilter);
    _aidl_parcel.writeLong(numBytesInSectionFilter);
    _aidl_parcel.writeInt(filterCaps);
    _aidl_parcel.writeIntArray(linkCaps);
    _aidl_parcel.writeBoolean(bTimeFilter);
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
      numDemux = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numRecord = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numPlayback = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numTsFilter = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numSectionFilter = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numAudioFilter = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numVideoFilter = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numPesFilter = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numPcrFilter = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numBytesInSectionFilter = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      filterCaps = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      linkCaps = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bTimeFilter = _aidl_parcel.readBoolean();
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
