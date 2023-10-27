/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterMediaEventExtraMetaData implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int audio = 1;  // android.hardware.tv.tuner.AudioExtraMetaData audio;
  public final static int audioPresentations = 2;  // android.hardware.tv.tuner.AudioPresentation[] audioPresentations;

  private int _tag;
  private Object _value;

  public DemuxFilterMediaEventExtraMetaData() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private DemuxFilterMediaEventExtraMetaData(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterMediaEventExtraMetaData(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static DemuxFilterMediaEventExtraMetaData noinit(boolean _value) {
    return new DemuxFilterMediaEventExtraMetaData(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.tv.tuner.AudioExtraMetaData audio;

  public static DemuxFilterMediaEventExtraMetaData audio(android.hardware.tv.tuner.AudioExtraMetaData _value) {
    return new DemuxFilterMediaEventExtraMetaData(audio, _value);
  }

  public android.hardware.tv.tuner.AudioExtraMetaData getAudio() {
    _assertTag(audio);
    return (android.hardware.tv.tuner.AudioExtraMetaData) _value;
  }

  public void setAudio(android.hardware.tv.tuner.AudioExtraMetaData _value) {
    _set(audio, _value);
  }

  // android.hardware.tv.tuner.AudioPresentation[] audioPresentations;

  public static DemuxFilterMediaEventExtraMetaData audioPresentations(android.hardware.tv.tuner.AudioPresentation[] _value) {
    return new DemuxFilterMediaEventExtraMetaData(audioPresentations, _value);
  }

  public android.hardware.tv.tuner.AudioPresentation[] getAudioPresentations() {
    _assertTag(audioPresentations);
    return (android.hardware.tv.tuner.AudioPresentation[]) _value;
  }

  public void setAudioPresentations(android.hardware.tv.tuner.AudioPresentation[] _value) {
    _set(audioPresentations, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterMediaEventExtraMetaData> CREATOR = new android.os.Parcelable.Creator<DemuxFilterMediaEventExtraMetaData>() {
    @Override
    public DemuxFilterMediaEventExtraMetaData createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterMediaEventExtraMetaData(_aidl_source);
    }
    @Override
    public DemuxFilterMediaEventExtraMetaData[] newArray(int _aidl_size) {
      return new DemuxFilterMediaEventExtraMetaData[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case noinit:
      _aidl_parcel.writeBoolean(getNoinit());
      break;
    case audio:
      _aidl_parcel.writeTypedObject(getAudio(), _aidl_flag);
      break;
    case audioPresentations:
      _aidl_parcel.writeTypedArray(getAudioPresentations(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case noinit: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case audio: {
      android.hardware.tv.tuner.AudioExtraMetaData _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.AudioExtraMetaData.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case audioPresentations: {
      android.hardware.tv.tuner.AudioPresentation[] _aidl_value;
      _aidl_value = _aidl_parcel.createTypedArray(android.hardware.tv.tuner.AudioPresentation.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case audio:
      _mask |= describeContents(getAudio());
      break;
    case audioPresentations:
      _mask |= describeContents(getAudioPresentations());
      break;
    }
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof Object[]) {
      int _mask = 0;
      for (Object o : (Object[]) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case noinit: return "noinit";
    case audio: return "audio";
    case audioPresentations: return "audioPresentations";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int noinit = 0;
    public static final int audio = 1;
    public static final int audioPresentations = 2;
  }
}
