/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/DvrSettings.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/DvrSettings.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DvrSettings implements android.os.Parcelable {
  // tags for union fields
  public final static int record = 0;  // android.hardware.tv.tuner.RecordSettings record;
  public final static int playback = 1;  // android.hardware.tv.tuner.PlaybackSettings playback;

  private int _tag;
  private Object _value;

  public DvrSettings() {
    android.hardware.tv.tuner.RecordSettings _value = null;
    this._tag = record;
    this._value = _value;
  }

  private DvrSettings(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DvrSettings(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.RecordSettings record;

  public static DvrSettings record(android.hardware.tv.tuner.RecordSettings _value) {
    return new DvrSettings(record, _value);
  }

  public android.hardware.tv.tuner.RecordSettings getRecord() {
    _assertTag(record);
    return (android.hardware.tv.tuner.RecordSettings) _value;
  }

  public void setRecord(android.hardware.tv.tuner.RecordSettings _value) {
    _set(record, _value);
  }

  // android.hardware.tv.tuner.PlaybackSettings playback;

  public static DvrSettings playback(android.hardware.tv.tuner.PlaybackSettings _value) {
    return new DvrSettings(playback, _value);
  }

  public android.hardware.tv.tuner.PlaybackSettings getPlayback() {
    _assertTag(playback);
    return (android.hardware.tv.tuner.PlaybackSettings) _value;
  }

  public void setPlayback(android.hardware.tv.tuner.PlaybackSettings _value) {
    _set(playback, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DvrSettings> CREATOR = new android.os.Parcelable.Creator<DvrSettings>() {
    @Override
    public DvrSettings createFromParcel(android.os.Parcel _aidl_source) {
      return new DvrSettings(_aidl_source);
    }
    @Override
    public DvrSettings[] newArray(int _aidl_size) {
      return new DvrSettings[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case record:
      _aidl_parcel.writeTypedObject(getRecord(), _aidl_flag);
      break;
    case playback:
      _aidl_parcel.writeTypedObject(getPlayback(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case record: {
      android.hardware.tv.tuner.RecordSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.RecordSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case playback: {
      android.hardware.tv.tuner.PlaybackSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.PlaybackSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case record:
      _mask |= describeContents(getRecord());
      break;
    case playback:
      _mask |= describeContents(getPlayback());
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
    case record: return "record";
    case playback: return "playback";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int record = 0;
    public static final int playback = 1;
  }
}
