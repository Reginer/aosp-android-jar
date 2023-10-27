/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger;
/** @hide */
public class Properties implements android.os.Parcelable
{
  public java.lang.String implementor;
  public java.lang.String description;
  public int version = 0;
  public java.lang.String uuid;
  public java.lang.String supportedModelArch;
  public int maxSoundModels = 0;
  public int maxKeyPhrases = 0;
  public int maxUsers = 0;
  public int recognitionModes = 0;
  public boolean captureTransition = false;
  public int maxBufferMs = 0;
  public boolean concurrentCapture = false;
  public boolean triggerInEvent = false;
  public int powerConsumptionMw = 0;
  public int audioCapabilities = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Properties> CREATOR = new android.os.Parcelable.Creator<Properties>() {
    @Override
    public Properties createFromParcel(android.os.Parcel _aidl_source) {
      Properties _aidl_out = new Properties();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Properties[] newArray(int _aidl_size) {
      return new Properties[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(implementor);
    _aidl_parcel.writeString(description);
    _aidl_parcel.writeInt(version);
    _aidl_parcel.writeString(uuid);
    _aidl_parcel.writeString(supportedModelArch);
    _aidl_parcel.writeInt(maxSoundModels);
    _aidl_parcel.writeInt(maxKeyPhrases);
    _aidl_parcel.writeInt(maxUsers);
    _aidl_parcel.writeInt(recognitionModes);
    _aidl_parcel.writeBoolean(captureTransition);
    _aidl_parcel.writeInt(maxBufferMs);
    _aidl_parcel.writeBoolean(concurrentCapture);
    _aidl_parcel.writeBoolean(triggerInEvent);
    _aidl_parcel.writeInt(powerConsumptionMw);
    _aidl_parcel.writeInt(audioCapabilities);
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
      implementor = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      description = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      version = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uuid = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      supportedModelArch = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxSoundModels = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxKeyPhrases = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxUsers = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      recognitionModes = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      captureTransition = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxBufferMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      concurrentCapture = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      triggerInEvent = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      powerConsumptionMw = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      audioCapabilities = _aidl_parcel.readInt();
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
    _aidl_sj.add("implementor: " + (java.util.Objects.toString(implementor)));
    _aidl_sj.add("description: " + (java.util.Objects.toString(description)));
    _aidl_sj.add("version: " + (version));
    _aidl_sj.add("uuid: " + (java.util.Objects.toString(uuid)));
    _aidl_sj.add("supportedModelArch: " + (java.util.Objects.toString(supportedModelArch)));
    _aidl_sj.add("maxSoundModels: " + (maxSoundModels));
    _aidl_sj.add("maxKeyPhrases: " + (maxKeyPhrases));
    _aidl_sj.add("maxUsers: " + (maxUsers));
    _aidl_sj.add("recognitionModes: " + (recognitionModes));
    _aidl_sj.add("captureTransition: " + (captureTransition));
    _aidl_sj.add("maxBufferMs: " + (maxBufferMs));
    _aidl_sj.add("concurrentCapture: " + (concurrentCapture));
    _aidl_sj.add("triggerInEvent: " + (triggerInEvent));
    _aidl_sj.add("powerConsumptionMw: " + (powerConsumptionMw));
    _aidl_sj.add("audioCapabilities: " + (audioCapabilities));
    return "android.media.soundtrigger.Properties" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof Properties)) return false;
    Properties that = (Properties)other;
    if (!java.util.Objects.deepEquals(implementor, that.implementor)) return false;
    if (!java.util.Objects.deepEquals(description, that.description)) return false;
    if (!java.util.Objects.deepEquals(version, that.version)) return false;
    if (!java.util.Objects.deepEquals(uuid, that.uuid)) return false;
    if (!java.util.Objects.deepEquals(supportedModelArch, that.supportedModelArch)) return false;
    if (!java.util.Objects.deepEquals(maxSoundModels, that.maxSoundModels)) return false;
    if (!java.util.Objects.deepEquals(maxKeyPhrases, that.maxKeyPhrases)) return false;
    if (!java.util.Objects.deepEquals(maxUsers, that.maxUsers)) return false;
    if (!java.util.Objects.deepEquals(recognitionModes, that.recognitionModes)) return false;
    if (!java.util.Objects.deepEquals(captureTransition, that.captureTransition)) return false;
    if (!java.util.Objects.deepEquals(maxBufferMs, that.maxBufferMs)) return false;
    if (!java.util.Objects.deepEquals(concurrentCapture, that.concurrentCapture)) return false;
    if (!java.util.Objects.deepEquals(triggerInEvent, that.triggerInEvent)) return false;
    if (!java.util.Objects.deepEquals(powerConsumptionMw, that.powerConsumptionMw)) return false;
    if (!java.util.Objects.deepEquals(audioCapabilities, that.audioCapabilities)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(implementor, description, version, uuid, supportedModelArch, maxSoundModels, maxKeyPhrases, maxUsers, recognitionModes, captureTransition, maxBufferMs, concurrentCapture, triggerInEvent, powerConsumptionMw, audioCapabilities).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
