/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public final class AudioMixMatchCriterionValue implements android.os.Parcelable {
  // tags for union fields
  public final static int usage = 0;  // android.media.audio.common.AudioUsage usage;
  public final static int source = 1;  // android.media.audio.common.AudioSource source;
  public final static int uid = 2;  // int uid;
  public final static int userId = 3;  // int userId;
  public final static int audioSessionId = 4;  // int audioSessionId;

  private int _tag;
  private Object _value;

  public AudioMixMatchCriterionValue() {
    int _value = android.media.audio.common.AudioUsage.UNKNOWN;
    this._tag = usage;
    this._value = _value;
  }

  private AudioMixMatchCriterionValue(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioMixMatchCriterionValue(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.media.audio.common.AudioUsage usage;

  public static AudioMixMatchCriterionValue usage(int _value) {
    return new AudioMixMatchCriterionValue(usage, _value);
  }

  public int getUsage() {
    _assertTag(usage);
    return (int) _value;
  }

  public void setUsage(int _value) {
    _set(usage, _value);
  }

  // android.media.audio.common.AudioSource source;

  public static AudioMixMatchCriterionValue source(int _value) {
    return new AudioMixMatchCriterionValue(source, _value);
  }

  public int getSource() {
    _assertTag(source);
    return (int) _value;
  }

  public void setSource(int _value) {
    _set(source, _value);
  }

  // int uid;

  /** Interpreted as uid_t. */
  public static AudioMixMatchCriterionValue uid(int _value) {
    return new AudioMixMatchCriterionValue(uid, _value);
  }

  public int getUid() {
    _assertTag(uid);
    return (int) _value;
  }

  public void setUid(int _value) {
    _set(uid, _value);
  }

  // int userId;

  public static AudioMixMatchCriterionValue userId(int _value) {
    return new AudioMixMatchCriterionValue(userId, _value);
  }

  public int getUserId() {
    _assertTag(userId);
    return (int) _value;
  }

  public void setUserId(int _value) {
    _set(userId, _value);
  }

  // int audioSessionId;

  /** Interpreted as audio_session_t. */
  public static AudioMixMatchCriterionValue audioSessionId(int _value) {
    return new AudioMixMatchCriterionValue(audioSessionId, _value);
  }

  public int getAudioSessionId() {
    _assertTag(audioSessionId);
    return (int) _value;
  }

  public void setAudioSessionId(int _value) {
    _set(audioSessionId, _value);
  }

  public static final android.os.Parcelable.Creator<AudioMixMatchCriterionValue> CREATOR = new android.os.Parcelable.Creator<AudioMixMatchCriterionValue>() {
    @Override
    public AudioMixMatchCriterionValue createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioMixMatchCriterionValue(_aidl_source);
    }
    @Override
    public AudioMixMatchCriterionValue[] newArray(int _aidl_size) {
      return new AudioMixMatchCriterionValue[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case usage:
      _aidl_parcel.writeInt(getUsage());
      break;
    case source:
      _aidl_parcel.writeInt(getSource());
      break;
    case uid:
      _aidl_parcel.writeInt(getUid());
      break;
    case userId:
      _aidl_parcel.writeInt(getUserId());
      break;
    case audioSessionId:
      _aidl_parcel.writeInt(getAudioSessionId());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case usage: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case source: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case uid: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case userId: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case audioSessionId: {
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

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case usage: return "usage";
    case source: return "source";
    case uid: return "uid";
    case userId: return "userId";
    case audioSessionId: return "audioSessionId";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int usage = 0;
    public static final int source = 1;
    /** Interpreted as uid_t. */
    public static final int uid = 2;
    public static final int userId = 3;
    /** Interpreted as audio_session_t. */
    public static final int audioSessionId = 4;
  }
}
