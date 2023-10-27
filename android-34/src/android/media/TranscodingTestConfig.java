/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * TranscodingTestConfig contains the test configureation used in testing.
 * 
 * {@hide}
 */
public class TranscodingTestConfig implements android.os.Parcelable
{
  /**
   * Passthrough mode used for testing. The transcoding service will assume the destination
   * path already contains the transcoding of the source file and return it to client directly.
   */
  public boolean passThroughMode = false;
  /**
   * Time of processing the session in milliseconds. Service will return the session result at
   * least after processingTotalTimeMs from the time it starts to process the session. Note that
   * if service uses real MediaTranscoder to do transcoding, the time spent on transcoding may be
   * more than that.
   */
  public int processingTotalTimeMs = 0;
  public static final android.os.Parcelable.Creator<TranscodingTestConfig> CREATOR = new android.os.Parcelable.Creator<TranscodingTestConfig>() {
    @Override
    public TranscodingTestConfig createFromParcel(android.os.Parcel _aidl_source) {
      TranscodingTestConfig _aidl_out = new TranscodingTestConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TranscodingTestConfig[] newArray(int _aidl_size) {
      return new TranscodingTestConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(passThroughMode);
    _aidl_parcel.writeInt(processingTotalTimeMs);
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
      passThroughMode = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      processingTotalTimeMs = _aidl_parcel.readInt();
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
