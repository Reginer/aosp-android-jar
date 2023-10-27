/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public final class AudioChannelLayout implements android.os.Parcelable {
  // tags for union fields
  public final static int none = 0;  // int none;
  public final static int invalid = 1;  // int invalid;
  public final static int indexMask = 2;  // int indexMask;
  public final static int layoutMask = 3;  // int layoutMask;
  public final static int voiceMask = 4;  // int voiceMask;

  private int _tag;
  private Object _value;

  public AudioChannelLayout() {
    int _value = 0;
    this._tag = none;
    this._value = _value;
  }

  private AudioChannelLayout(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioChannelLayout(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // int none;

  public static AudioChannelLayout none(int _value) {
    return new AudioChannelLayout(none, _value);
  }

  public int getNone() {
    _assertTag(none);
    return (int) _value;
  }

  public void setNone(int _value) {
    _set(none, _value);
  }

  // int invalid;

  public static AudioChannelLayout invalid(int _value) {
    return new AudioChannelLayout(invalid, _value);
  }

  public int getInvalid() {
    _assertTag(invalid);
    return (int) _value;
  }

  public void setInvalid(int _value) {
    _set(invalid, _value);
  }

  // int indexMask;

  public static AudioChannelLayout indexMask(int _value) {
    return new AudioChannelLayout(indexMask, _value);
  }

  public int getIndexMask() {
    _assertTag(indexMask);
    return (int) _value;
  }

  public void setIndexMask(int _value) {
    _set(indexMask, _value);
  }

  // int layoutMask;

  public static AudioChannelLayout layoutMask(int _value) {
    return new AudioChannelLayout(layoutMask, _value);
  }

  public int getLayoutMask() {
    _assertTag(layoutMask);
    return (int) _value;
  }

  public void setLayoutMask(int _value) {
    _set(layoutMask, _value);
  }

  // int voiceMask;

  public static AudioChannelLayout voiceMask(int _value) {
    return new AudioChannelLayout(voiceMask, _value);
  }

  public int getVoiceMask() {
    _assertTag(voiceMask);
    return (int) _value;
  }

  public void setVoiceMask(int _value) {
    _set(voiceMask, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AudioChannelLayout> CREATOR = new android.os.Parcelable.Creator<AudioChannelLayout>() {
    @Override
    public AudioChannelLayout createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioChannelLayout(_aidl_source);
    }
    @Override
    public AudioChannelLayout[] newArray(int _aidl_size) {
      return new AudioChannelLayout[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case none:
      _aidl_parcel.writeInt(getNone());
      break;
    case invalid:
      _aidl_parcel.writeInt(getInvalid());
      break;
    case indexMask:
      _aidl_parcel.writeInt(getIndexMask());
      break;
    case layoutMask:
      _aidl_parcel.writeInt(getLayoutMask());
      break;
    case voiceMask:
      _aidl_parcel.writeInt(getVoiceMask());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case none: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case invalid: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case indexMask: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case layoutMask: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case voiceMask: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  public static final int INDEX_MASK_1 = 1;
  public static final int INDEX_MASK_2 = 3;
  public static final int INDEX_MASK_3 = 7;
  public static final int INDEX_MASK_4 = 15;
  public static final int INDEX_MASK_5 = 31;
  public static final int INDEX_MASK_6 = 63;
  public static final int INDEX_MASK_7 = 127;
  public static final int INDEX_MASK_8 = 255;
  public static final int INDEX_MASK_9 = 511;
  public static final int INDEX_MASK_10 = 1023;
  public static final int INDEX_MASK_11 = 2047;
  public static final int INDEX_MASK_12 = 4095;
  public static final int INDEX_MASK_13 = 8191;
  public static final int INDEX_MASK_14 = 16383;
  public static final int INDEX_MASK_15 = 32767;
  public static final int INDEX_MASK_16 = 65535;
  public static final int INDEX_MASK_17 = 131071;
  public static final int INDEX_MASK_18 = 262143;
  public static final int INDEX_MASK_19 = 524287;
  public static final int INDEX_MASK_20 = 1048575;
  public static final int INDEX_MASK_21 = 2097151;
  public static final int INDEX_MASK_22 = 4194303;
  public static final int INDEX_MASK_23 = 8388607;
  public static final int INDEX_MASK_24 = 16777215;
  public static final int LAYOUT_MONO = 1;
  public static final int LAYOUT_STEREO = 3;
  public static final int LAYOUT_2POINT1 = 11;
  public static final int LAYOUT_TRI = 7;
  public static final int LAYOUT_TRI_BACK = 259;
  public static final int LAYOUT_3POINT1 = 15;
  public static final int LAYOUT_2POINT0POINT2 = 786435;
  public static final int LAYOUT_2POINT1POINT2 = 786443;
  public static final int LAYOUT_3POINT0POINT2 = 786439;
  public static final int LAYOUT_3POINT1POINT2 = 786447;
  public static final int LAYOUT_QUAD = 51;
  public static final int LAYOUT_QUAD_SIDE = 1539;
  public static final int LAYOUT_SURROUND = 263;
  public static final int LAYOUT_PENTA = 55;
  public static final int LAYOUT_5POINT1 = 63;
  public static final int LAYOUT_5POINT1_SIDE = 1551;
  public static final int LAYOUT_5POINT1POINT2 = 786495;
  public static final int LAYOUT_5POINT1POINT4 = 184383;
  public static final int LAYOUT_6POINT1 = 319;
  public static final int LAYOUT_7POINT1 = 1599;
  public static final int LAYOUT_7POINT1POINT2 = 788031;
  public static final int LAYOUT_7POINT1POINT4 = 185919;
  public static final int LAYOUT_9POINT1POINT4 = 50517567;
  public static final int LAYOUT_9POINT1POINT6 = 51303999;
  public static final int LAYOUT_13POINT_360RA = 7534087;
  public static final int LAYOUT_22POINT2 = 16777215;
  public static final int LAYOUT_MONO_HAPTIC_A = 1073741825;
  public static final int LAYOUT_STEREO_HAPTIC_A = 1073741827;
  public static final int LAYOUT_HAPTIC_AB = 1610612736;
  public static final int LAYOUT_MONO_HAPTIC_AB = 1610612737;
  public static final int LAYOUT_STEREO_HAPTIC_AB = 1610612739;
  public static final int LAYOUT_FRONT_BACK = 260;
  public static final int INTERLEAVE_LEFT = 0;
  public static final int INTERLEAVE_RIGHT = 1;
  public static final int CHANNEL_FRONT_LEFT = 1;
  public static final int CHANNEL_FRONT_RIGHT = 2;
  public static final int CHANNEL_FRONT_CENTER = 4;
  public static final int CHANNEL_LOW_FREQUENCY = 8;
  public static final int CHANNEL_BACK_LEFT = 16;
  public static final int CHANNEL_BACK_RIGHT = 32;
  public static final int CHANNEL_FRONT_LEFT_OF_CENTER = 64;
  public static final int CHANNEL_FRONT_RIGHT_OF_CENTER = 128;
  public static final int CHANNEL_BACK_CENTER = 256;
  public static final int CHANNEL_SIDE_LEFT = 512;
  public static final int CHANNEL_SIDE_RIGHT = 1024;
  public static final int CHANNEL_TOP_CENTER = 2048;
  public static final int CHANNEL_TOP_FRONT_LEFT = 4096;
  public static final int CHANNEL_TOP_FRONT_CENTER = 8192;
  public static final int CHANNEL_TOP_FRONT_RIGHT = 16384;
  public static final int CHANNEL_TOP_BACK_LEFT = 32768;
  public static final int CHANNEL_TOP_BACK_CENTER = 65536;
  public static final int CHANNEL_TOP_BACK_RIGHT = 131072;
  public static final int CHANNEL_TOP_SIDE_LEFT = 262144;
  public static final int CHANNEL_TOP_SIDE_RIGHT = 524288;
  public static final int CHANNEL_BOTTOM_FRONT_LEFT = 1048576;
  public static final int CHANNEL_BOTTOM_FRONT_CENTER = 2097152;
  public static final int CHANNEL_BOTTOM_FRONT_RIGHT = 4194304;
  public static final int CHANNEL_LOW_FREQUENCY_2 = 8388608;
  public static final int CHANNEL_FRONT_WIDE_LEFT = 16777216;
  public static final int CHANNEL_FRONT_WIDE_RIGHT = 33554432;
  public static final int CHANNEL_HAPTIC_B = 536870912;
  public static final int CHANNEL_HAPTIC_A = 1073741824;
  public static final int VOICE_UPLINK_MONO = 16384;
  public static final int VOICE_DNLINK_MONO = 32768;
  public static final int VOICE_CALL_MONO = 49152;
  public static final int CHANNEL_VOICE_UPLINK = 16384;
  public static final int CHANNEL_VOICE_DNLINK = 32768;
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
    case none: return "android.media.audio.common.AudioChannelLayout.none(" + (getNone()) + ")";
    case invalid: return "android.media.audio.common.AudioChannelLayout.invalid(" + (getInvalid()) + ")";
    case indexMask: return "android.media.audio.common.AudioChannelLayout.indexMask(" + (getIndexMask()) + ")";
    case layoutMask: return "android.media.audio.common.AudioChannelLayout.layoutMask(" + (getLayoutMask()) + ")";
    case voiceMask: return "android.media.audio.common.AudioChannelLayout.voiceMask(" + (getVoiceMask()) + ")";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioChannelLayout)) return false;
    AudioChannelLayout that = (AudioChannelLayout)other;
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
    case none: return "none";
    case invalid: return "invalid";
    case indexMask: return "indexMask";
    case layoutMask: return "layoutMask";
    case voiceMask: return "voiceMask";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int none = 0;
    public static final int invalid = 1;
    public static final int indexMask = 2;
    public static final int layoutMask = 3;
    public static final int voiceMask = 4;
  }
}
