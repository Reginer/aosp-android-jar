/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public final class AudioPortExt implements android.os.Parcelable {
  // tags for union fields
  public final static int unspecified = 0;  // boolean unspecified;
  public final static int device = 1;  // android.media.audio.common.AudioPortDeviceExt device;
  public final static int mix = 2;  // android.media.audio.common.AudioPortMixExt mix;
  public final static int session = 3;  // int session;

  private int _tag;
  private Object _value;

  public AudioPortExt() {
    boolean _value = false;
    this._tag = unspecified;
    this._value = _value;
  }

  private AudioPortExt(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioPortExt(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean unspecified;

  public static AudioPortExt unspecified(boolean _value) {
    return new AudioPortExt(unspecified, _value);
  }

  public boolean getUnspecified() {
    _assertTag(unspecified);
    return (boolean) _value;
  }

  public void setUnspecified(boolean _value) {
    _set(unspecified, _value);
  }

  // android.media.audio.common.AudioPortDeviceExt device;

  public static AudioPortExt device(android.media.audio.common.AudioPortDeviceExt _value) {
    return new AudioPortExt(device, _value);
  }

  public android.media.audio.common.AudioPortDeviceExt getDevice() {
    _assertTag(device);
    return (android.media.audio.common.AudioPortDeviceExt) _value;
  }

  public void setDevice(android.media.audio.common.AudioPortDeviceExt _value) {
    _set(device, _value);
  }

  // android.media.audio.common.AudioPortMixExt mix;

  public static AudioPortExt mix(android.media.audio.common.AudioPortMixExt _value) {
    return new AudioPortExt(mix, _value);
  }

  public android.media.audio.common.AudioPortMixExt getMix() {
    _assertTag(mix);
    return (android.media.audio.common.AudioPortMixExt) _value;
  }

  public void setMix(android.media.audio.common.AudioPortMixExt _value) {
    _set(mix, _value);
  }

  // int session;

  public static AudioPortExt session(int _value) {
    return new AudioPortExt(session, _value);
  }

  public int getSession() {
    _assertTag(session);
    return (int) _value;
  }

  public void setSession(int _value) {
    _set(session, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AudioPortExt> CREATOR = new android.os.Parcelable.Creator<AudioPortExt>() {
    @Override
    public AudioPortExt createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioPortExt(_aidl_source);
    }
    @Override
    public AudioPortExt[] newArray(int _aidl_size) {
      return new AudioPortExt[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case unspecified:
      _aidl_parcel.writeBoolean(getUnspecified());
      break;
    case device:
      _aidl_parcel.writeTypedObject(getDevice(), _aidl_flag);
      break;
    case mix:
      _aidl_parcel.writeTypedObject(getMix(), _aidl_flag);
      break;
    case session:
      _aidl_parcel.writeInt(getSession());
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
    case device: {
      android.media.audio.common.AudioPortDeviceExt _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioPortDeviceExt.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case mix: {
      android.media.audio.common.AudioPortMixExt _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioPortMixExt.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case session: {
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
    case device:
      _mask |= describeContents(getDevice());
      break;
    case mix:
      _mask |= describeContents(getMix());
      break;
    }
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }

  @Override
  public String toString() {
    switch (_tag) {
    case unspecified: return "android.media.audio.common.AudioPortExt.unspecified(" + (getUnspecified()) + ")";
    case device: return "android.media.audio.common.AudioPortExt.device(" + (java.util.Objects.toString(getDevice())) + ")";
    case mix: return "android.media.audio.common.AudioPortExt.mix(" + (java.util.Objects.toString(getMix())) + ")";
    case session: return "android.media.audio.common.AudioPortExt.session(" + (getSession()) + ")";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioPortExt)) return false;
    AudioPortExt that = (AudioPortExt)other;
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
    case device: return "device";
    case mix: return "mix";
    case session: return "session";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int unspecified = 0;
    public static final int device = 1;
    public static final int mix = 2;
    public static final int session = 3;
  }
}
