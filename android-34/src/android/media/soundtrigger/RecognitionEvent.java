/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger;
/** @hide */
public class RecognitionEvent implements android.os.Parcelable
{
  public int status = android.media.soundtrigger.RecognitionStatus.INVALID;
  public int type = android.media.soundtrigger.SoundModelType.INVALID;
  public boolean captureAvailable = false;
  public int captureDelayMs = 0;
  public int capturePreambleMs = 0;
  public boolean triggerInData = false;
  public android.media.audio.common.AudioConfig audioConfig;
  public byte[] data;
  public boolean recognitionStillActive = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RecognitionEvent> CREATOR = new android.os.Parcelable.Creator<RecognitionEvent>() {
    @Override
    public RecognitionEvent createFromParcel(android.os.Parcel _aidl_source) {
      RecognitionEvent _aidl_out = new RecognitionEvent();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RecognitionEvent[] newArray(int _aidl_size) {
      return new RecognitionEvent[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(status);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeBoolean(captureAvailable);
    _aidl_parcel.writeInt(captureDelayMs);
    _aidl_parcel.writeInt(capturePreambleMs);
    _aidl_parcel.writeBoolean(triggerInData);
    _aidl_parcel.writeTypedObject(audioConfig, _aidl_flag);
    _aidl_parcel.writeByteArray(data);
    _aidl_parcel.writeBoolean(recognitionStillActive);
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
      status = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      captureAvailable = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      captureDelayMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      capturePreambleMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      triggerInData = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      audioConfig = _aidl_parcel.readTypedObject(android.media.audio.common.AudioConfig.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      data = _aidl_parcel.createByteArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      recognitionStillActive = _aidl_parcel.readBoolean();
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
    _aidl_sj.add("status: " + (status));
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("captureAvailable: " + (captureAvailable));
    _aidl_sj.add("captureDelayMs: " + (captureDelayMs));
    _aidl_sj.add("capturePreambleMs: " + (capturePreambleMs));
    _aidl_sj.add("triggerInData: " + (triggerInData));
    _aidl_sj.add("audioConfig: " + (java.util.Objects.toString(audioConfig)));
    _aidl_sj.add("data: " + (java.util.Arrays.toString(data)));
    _aidl_sj.add("recognitionStillActive: " + (recognitionStillActive));
    return "android.media.soundtrigger.RecognitionEvent" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof RecognitionEvent)) return false;
    RecognitionEvent that = (RecognitionEvent)other;
    if (!java.util.Objects.deepEquals(status, that.status)) return false;
    if (!java.util.Objects.deepEquals(type, that.type)) return false;
    if (!java.util.Objects.deepEquals(captureAvailable, that.captureAvailable)) return false;
    if (!java.util.Objects.deepEquals(captureDelayMs, that.captureDelayMs)) return false;
    if (!java.util.Objects.deepEquals(capturePreambleMs, that.capturePreambleMs)) return false;
    if (!java.util.Objects.deepEquals(triggerInData, that.triggerInData)) return false;
    if (!java.util.Objects.deepEquals(audioConfig, that.audioConfig)) return false;
    if (!java.util.Objects.deepEquals(data, that.data)) return false;
    if (!java.util.Objects.deepEquals(recognitionStillActive, that.recognitionStillActive)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(status, type, captureAvailable, captureDelayMs, capturePreambleMs, triggerInData, audioConfig, data, recognitionStillActive).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(audioConfig);
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
