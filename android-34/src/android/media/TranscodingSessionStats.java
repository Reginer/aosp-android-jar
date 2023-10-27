/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * TranscodingSessionStats encapsulated the stats of the a TranscodingSession.
 * 
 * {@hide}
 */
public class TranscodingSessionStats implements android.os.Parcelable
{
  /** System time of when the session is created. */
  public long sessionCreatedTimeUs = 0L;
  /** System time of when the session is finished. */
  public long sessionFinishedTimeUs = 0L;
  /** Total time spend on transcoding, exclude the time in pause. */
  public long totalProcessingTimeUs = 0L;
  /**
   * Total time spend on handling the session, include the time in pause.
   * The totaltimeUs is actually the same as sessionFinishedTimeUs - sessionCreatedTimeUs.
   */
  public long totalTimeUs = 0L;
  public static final android.os.Parcelable.Creator<TranscodingSessionStats> CREATOR = new android.os.Parcelable.Creator<TranscodingSessionStats>() {
    @Override
    public TranscodingSessionStats createFromParcel(android.os.Parcel _aidl_source) {
      TranscodingSessionStats _aidl_out = new TranscodingSessionStats();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TranscodingSessionStats[] newArray(int _aidl_size) {
      return new TranscodingSessionStats[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(sessionCreatedTimeUs);
    _aidl_parcel.writeLong(sessionFinishedTimeUs);
    _aidl_parcel.writeLong(totalProcessingTimeUs);
    _aidl_parcel.writeLong(totalTimeUs);
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
      sessionCreatedTimeUs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sessionFinishedTimeUs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      totalProcessingTimeUs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      totalTimeUs = _aidl_parcel.readLong();
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
