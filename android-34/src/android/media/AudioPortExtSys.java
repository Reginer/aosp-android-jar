/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public final class AudioPortExtSys implements android.os.Parcelable {
  // tags for union fields
  public final static int unspecified = 0;  // boolean unspecified;
  public final static int device = 1;  // android.media.AudioPortDeviceExtSys device;
  public final static int mix = 2;  // android.media.AudioPortMixExtSys mix;
  public final static int session = 3;  // int session;

  private int _tag;
  private Object _value;

  public AudioPortExtSys() {
    boolean _value = false;
    this._tag = unspecified;
    this._value = _value;
  }

  private AudioPortExtSys(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioPortExtSys(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean unspecified;

  /** This represents an empty union. Value is ignored. */
  public static AudioPortExtSys unspecified(boolean _value) {
    return new AudioPortExtSys(unspecified, _value);
  }

  public boolean getUnspecified() {
    _assertTag(unspecified);
    return (boolean) _value;
  }

  public void setUnspecified(boolean _value) {
    _set(unspecified, _value);
  }

  // android.media.AudioPortDeviceExtSys device;

  /** System-only parameters when the port is an audio device. */
  public static AudioPortExtSys device(android.media.AudioPortDeviceExtSys _value) {
    return new AudioPortExtSys(device, _value);
  }

  public android.media.AudioPortDeviceExtSys getDevice() {
    _assertTag(device);
    return (android.media.AudioPortDeviceExtSys) _value;
  }

  public void setDevice(android.media.AudioPortDeviceExtSys _value) {
    _set(device, _value);
  }

  // android.media.AudioPortMixExtSys mix;

  /** System-only parameters when the port is an audio mix. */
  public static AudioPortExtSys mix(android.media.AudioPortMixExtSys _value) {
    return new AudioPortExtSys(mix, _value);
  }

  public android.media.AudioPortMixExtSys getMix() {
    _assertTag(mix);
    return (android.media.AudioPortMixExtSys) _value;
  }

  public void setMix(android.media.AudioPortMixExtSys _value) {
    _set(mix, _value);
  }

  // int session;

  /** Framework audio session identifier. */
  public static AudioPortExtSys session(int _value) {
    return new AudioPortExtSys(session, _value);
  }

  public int getSession() {
    _assertTag(session);
    return (int) _value;
  }

  public void setSession(int _value) {
    _set(session, _value);
  }

  public static final android.os.Parcelable.Creator<AudioPortExtSys> CREATOR = new android.os.Parcelable.Creator<AudioPortExtSys>() {
    @Override
    public AudioPortExtSys createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioPortExtSys(_aidl_source);
    }
    @Override
    public AudioPortExtSys[] newArray(int _aidl_size) {
      return new AudioPortExtSys[_aidl_size];
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
      android.media.AudioPortDeviceExtSys _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.AudioPortDeviceExtSys.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case mix: {
      android.media.AudioPortMixExtSys _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.AudioPortMixExtSys.CREATOR);
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
    /** This represents an empty union. Value is ignored. */
    public static final int unspecified = 0;
    /** System-only parameters when the port is an audio device. */
    public static final int device = 1;
    /** System-only parameters when the port is an audio mix. */
    public static final int mix = 2;
    /** Framework audio session identifier. */
    public static final int session = 3;
  }
}
