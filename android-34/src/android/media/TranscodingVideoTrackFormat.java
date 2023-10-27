/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * TranscodingVideoTrackFormat contains the video track format of a video.
 * 
 * TODO(hkuang): Switch to PersistableBundle when b/156428735 is fixed or after we remove
 * aidl_interface
 * 
 * Note that TranscodingVideoTrackFormat is used in TranscodingRequestParcel for the  client to
 * specify the desired transcoded video format, and is also used in TranscodingSessionParcel for the
 * service to notify client of the final video format for transcoding.
 * When used as input in TranscodingRequestParcel, the client only needs to specify the config that
 * they want to change, e.g. codec or resolution, and all the missing configs will be extracted
 * from the source video and applied to the destination video.
 * When used as output in TranscodingSessionParcel, all the configs will be populated to indicate
 * the final encoder configs used for transcoding.
 * 
 * {@hide}
 */
public class TranscodingVideoTrackFormat implements android.os.Parcelable
{
  /** Video Codec type. */
  public int codecType;
  // TranscodingVideoCodecType::kUnspecified;
  /** Width of the video in pixels. -1 means unavailable. */
  public int width = -1;
  /** Height of the video in pixels. -1 means unavailable. */
  public int height = -1;
  /** Bitrate in bits per second. -1 means unavailable. */
  public int bitrateBps = -1;
  /**
   * Codec profile. This must be the same constant as used in MediaCodecInfo.CodecProfileLevel.
   * -1 means unavailable.
   */
  public int profile = -1;
  /**
   * Codec level. This must be the same constant as used in MediaCodecInfo.CodecProfileLevel.
   * -1 means unavailable.
   */
  public int level = -1;
  /**
   * Decoder operating rate. This is used to work around the fact that vendor does not boost the
   * hardware to maximum speed in transcoding usage case. This operating rate will be applied
   * to decoder inside MediaTranscoder. -1 means unavailable.
   */
  public int decoderOperatingRate = -1;
  /**
   * Encoder operating rate. This is used to work around the fact that vendor does not boost the
   * hardware to maximum speed in transcoding usage case. This operating rate will be applied
   * to encoder inside MediaTranscoder. -1 means unavailable.
   */
  public int encoderOperatingRate = -1;
  public static final android.os.Parcelable.Creator<TranscodingVideoTrackFormat> CREATOR = new android.os.Parcelable.Creator<TranscodingVideoTrackFormat>() {
    @Override
    public TranscodingVideoTrackFormat createFromParcel(android.os.Parcel _aidl_source) {
      TranscodingVideoTrackFormat _aidl_out = new TranscodingVideoTrackFormat();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TranscodingVideoTrackFormat[] newArray(int _aidl_size) {
      return new TranscodingVideoTrackFormat[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(codecType);
    _aidl_parcel.writeInt(width);
    _aidl_parcel.writeInt(height);
    _aidl_parcel.writeInt(bitrateBps);
    _aidl_parcel.writeInt(profile);
    _aidl_parcel.writeInt(level);
    _aidl_parcel.writeInt(decoderOperatingRate);
    _aidl_parcel.writeInt(encoderOperatingRate);
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
      codecType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      width = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      height = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bitrateBps = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      profile = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      level = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      decoderOperatingRate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encoderOperatingRate = _aidl_parcel.readInt();
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
