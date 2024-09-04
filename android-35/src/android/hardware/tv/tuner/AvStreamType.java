/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 2 --hash f8d74c149f04e76b6d622db2bd8e465dae24b08c --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen/android/hardware/tv/tuner/AvStreamType.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V2-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/2/android/hardware/tv/tuner/AvStreamType.aidl
 */
package android.hardware.tv.tuner;
/** @hide */
public final class AvStreamType implements android.os.Parcelable {
  // tags for union fields
  public final static int video = 0;  // android.hardware.tv.tuner.VideoStreamType video;
  public final static int audio = 1;  // android.hardware.tv.tuner.AudioStreamType audio;

  private int _tag;
  private Object _value;

  public AvStreamType() {
    int _value = android.hardware.tv.tuner.VideoStreamType.UNDEFINED;
    this._tag = video;
    this._value = _value;
  }

  private AvStreamType(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AvStreamType(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.VideoStreamType video;

  public static AvStreamType video(int _value) {
    return new AvStreamType(video, _value);
  }

  public int getVideo() {
    _assertTag(video);
    return (int) _value;
  }

  public void setVideo(int _value) {
    _set(video, _value);
  }

  // android.hardware.tv.tuner.AudioStreamType audio;

  public static AvStreamType audio(int _value) {
    return new AvStreamType(audio, _value);
  }

  public int getAudio() {
    _assertTag(audio);
    return (int) _value;
  }

  public void setAudio(int _value) {
    _set(audio, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AvStreamType> CREATOR = new android.os.Parcelable.Creator<AvStreamType>() {
    @Override
    public AvStreamType createFromParcel(android.os.Parcel _aidl_source) {
      return new AvStreamType(_aidl_source);
    }
    @Override
    public AvStreamType[] newArray(int _aidl_size) {
      return new AvStreamType[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case video:
      _aidl_parcel.writeInt(getVideo());
      break;
    case audio:
      _aidl_parcel.writeInt(getAudio());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case video: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case audio: {
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
    case video: return "video";
    case audio: return "audio";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int video = 0;
    public static final int audio = 1;
  }
}
