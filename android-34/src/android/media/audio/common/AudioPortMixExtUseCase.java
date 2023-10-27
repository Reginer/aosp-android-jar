/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public final class AudioPortMixExtUseCase implements android.os.Parcelable {
  // tags for union fields
  public final static int unspecified = 0;  // boolean unspecified;
  public final static int stream = 1;  // android.media.audio.common.AudioStreamType stream;
  public final static int source = 2;  // android.media.audio.common.AudioSource source;

  private int _tag;
  private Object _value;

  public AudioPortMixExtUseCase() {
    boolean _value = false;
    this._tag = unspecified;
    this._value = _value;
  }

  private AudioPortMixExtUseCase(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioPortMixExtUseCase(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean unspecified;

  public static AudioPortMixExtUseCase unspecified(boolean _value) {
    return new AudioPortMixExtUseCase(unspecified, _value);
  }

  public boolean getUnspecified() {
    _assertTag(unspecified);
    return (boolean) _value;
  }

  public void setUnspecified(boolean _value) {
    _set(unspecified, _value);
  }

  // android.media.audio.common.AudioStreamType stream;

  public static AudioPortMixExtUseCase stream(int _value) {
    return new AudioPortMixExtUseCase(stream, _value);
  }

  public int getStream() {
    _assertTag(stream);
    return (int) _value;
  }

  public void setStream(int _value) {
    _set(stream, _value);
  }

  // android.media.audio.common.AudioSource source;

  public static AudioPortMixExtUseCase source(int _value) {
    return new AudioPortMixExtUseCase(source, _value);
  }

  public int getSource() {
    _assertTag(source);
    return (int) _value;
  }

  public void setSource(int _value) {
    _set(source, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AudioPortMixExtUseCase> CREATOR = new android.os.Parcelable.Creator<AudioPortMixExtUseCase>() {
    @Override
    public AudioPortMixExtUseCase createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioPortMixExtUseCase(_aidl_source);
    }
    @Override
    public AudioPortMixExtUseCase[] newArray(int _aidl_size) {
      return new AudioPortMixExtUseCase[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case unspecified:
      _aidl_parcel.writeBoolean(getUnspecified());
      break;
    case stream:
      _aidl_parcel.writeInt(getStream());
      break;
    case source:
      _aidl_parcel.writeInt(getSource());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case unspecified: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case stream: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case source: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    }
    return _mask;
  }

  @Override
  public String toString() {
    switch (_tag) {
    case unspecified: return "android.media.audio.common.AudioPortMixExtUseCase.unspecified(" + (getUnspecified()) + ")";
    case stream: return "android.media.audio.common.AudioPortMixExtUseCase.stream(" + (getStream()) + ")";
    case source: return "android.media.audio.common.AudioPortMixExtUseCase.source(" + (getSource()) + ")";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioPortMixExtUseCase)) return false;
    AudioPortMixExtUseCase that = (AudioPortMixExtUseCase)other;
    if (_tag != that._tag) return false;
    if (!java.util.Objects.deepEquals(_value, that._value)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(_tag, _value).toArray());
  }

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case unspecified: return "unspecified";
    case stream: return "stream";
    case source: return "source";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int unspecified = 0;
    public static final int stream = 1;
    public static final int source = 2;
  }
}
