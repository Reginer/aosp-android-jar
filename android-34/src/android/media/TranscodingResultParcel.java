/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * Result of the transcoding.
 * 
 * {@hide}
 */
//TODO(hkuang): Implement the parcelable.
public class TranscodingResultParcel implements android.os.Parcelable
{
  /** The sessionId associated with the TranscodingResult. */
  public int sessionId = 0;
  /**
   * Actual bitrate of the transcoded video in bits per second. This will only present for video
   * transcoding. -1 means not available.
   */
  public int actualBitrateBps = 0;
  /**
   * Stats of the transcoding session. This will only be available when client requests to get the
   * stats in TranscodingRequestParcel.
   */
  public android.media.TranscodingSessionStats sessionStats;
  public static final android.os.Parcelable.Creator<TranscodingResultParcel> CREATOR = new android.os.Parcelable.Creator<TranscodingResultParcel>() {
    @Override
    public TranscodingResultParcel createFromParcel(android.os.Parcel _aidl_source) {
      TranscodingResultParcel _aidl_out = new TranscodingResultParcel();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TranscodingResultParcel[] newArray(int _aidl_size) {
      return new TranscodingResultParcel[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(sessionId);
    _aidl_parcel.writeInt(actualBitrateBps);
    _aidl_parcel.writeTypedObject(sessionStats, _aidl_flag);
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
      sessionId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      actualBitrateBps = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sessionStats = _aidl_parcel.readTypedObject(android.media.TranscodingSessionStats.CREATOR);
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
    _mask |= describeContents(sessionStats);
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
