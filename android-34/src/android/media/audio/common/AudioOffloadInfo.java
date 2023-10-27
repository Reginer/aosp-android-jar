/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioOffloadInfo implements android.os.Parcelable
{
  public android.media.audio.common.AudioConfigBase base;
  public int streamType = android.media.audio.common.AudioStreamType.INVALID;
  public int bitRatePerSecond = 0;
  public long durationUs = 0L;
  public boolean hasVideo = false;
  public boolean isStreaming = false;
  public int bitWidth = 16;
  public int offloadBufferSize = 0;
  public int usage = android.media.audio.common.AudioUsage.INVALID;
  public byte encapsulationMode = android.media.audio.common.AudioEncapsulationMode.INVALID;
  public int contentId = 0;
  public int syncId = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioOffloadInfo> CREATOR = new android.os.Parcelable.Creator<AudioOffloadInfo>() {
    @Override
    public AudioOffloadInfo createFromParcel(android.os.Parcel _aidl_source) {
      AudioOffloadInfo _aidl_out = new AudioOffloadInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioOffloadInfo[] newArray(int _aidl_size) {
      return new AudioOffloadInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(base, _aidl_flag);
    _aidl_parcel.writeInt(streamType);
    _aidl_parcel.writeInt(bitRatePerSecond);
    _aidl_parcel.writeLong(durationUs);
    _aidl_parcel.writeBoolean(hasVideo);
    _aidl_parcel.writeBoolean(isStreaming);
    _aidl_parcel.writeInt(bitWidth);
    _aidl_parcel.writeInt(offloadBufferSize);
    _aidl_parcel.writeInt(usage);
    _aidl_parcel.writeByte(encapsulationMode);
    _aidl_parcel.writeInt(contentId);
    _aidl_parcel.writeInt(syncId);
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
      base = _aidl_parcel.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      streamType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bitRatePerSecond = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      durationUs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hasVideo = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isStreaming = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bitWidth = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      offloadBufferSize = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      usage = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encapsulationMode = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      contentId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      syncId = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("base: " + (java.util.Objects.toString(base)));
    _aidl_sj.add("streamType: " + (streamType));
    _aidl_sj.add("bitRatePerSecond: " + (bitRatePerSecond));
    _aidl_sj.add("durationUs: " + (durationUs));
    _aidl_sj.add("hasVideo: " + (hasVideo));
    _aidl_sj.add("isStreaming: " + (isStreaming));
    _aidl_sj.add("bitWidth: " + (bitWidth));
    _aidl_sj.add("offloadBufferSize: " + (offloadBufferSize));
    _aidl_sj.add("usage: " + (usage));
    _aidl_sj.add("encapsulationMode: " + (encapsulationMode));
    _aidl_sj.add("contentId: " + (contentId));
    _aidl_sj.add("syncId: " + (syncId));
    return "android.media.audio.common.AudioOffloadInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioOffloadInfo)) return false;
    AudioOffloadInfo that = (AudioOffloadInfo)other;
    if (!java.util.Objects.deepEquals(base, that.base)) return false;
    if (!java.util.Objects.deepEquals(streamType, that.streamType)) return false;
    if (!java.util.Objects.deepEquals(bitRatePerSecond, that.bitRatePerSecond)) return false;
    if (!java.util.Objects.deepEquals(durationUs, that.durationUs)) return false;
    if (!java.util.Objects.deepEquals(hasVideo, that.hasVideo)) return false;
    if (!java.util.Objects.deepEquals(isStreaming, that.isStreaming)) return false;
    if (!java.util.Objects.deepEquals(bitWidth, that.bitWidth)) return false;
    if (!java.util.Objects.deepEquals(offloadBufferSize, that.offloadBufferSize)) return false;
    if (!java.util.Objects.deepEquals(usage, that.usage)) return false;
    if (!java.util.Objects.deepEquals(encapsulationMode, that.encapsulationMode)) return false;
    if (!java.util.Objects.deepEquals(contentId, that.contentId)) return false;
    if (!java.util.Objects.deepEquals(syncId, that.syncId)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(base, streamType, bitRatePerSecond, durationUs, hasVideo, isStreaming, bitWidth, offloadBufferSize, usage, encapsulationMode, contentId, syncId).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(base);
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
