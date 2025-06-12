/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl-java-source/gen/android/media/AudioHalVersion.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/AudioHalVersion.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/**
 * The audio HAL version definition.
 * 
 * {@hide}
 */
public class AudioHalVersion implements android.os.Parcelable
{
  public int type = android.media.AudioHalVersion.Type.HIDL;
  /** Major version number. */
  public int major = 0;
  /** Minor version number. */
  public int minor = 0;
  public static final android.os.Parcelable.Creator<AudioHalVersion> CREATOR = new android.os.Parcelable.Creator<AudioHalVersion>() {
    @Override
    public AudioHalVersion createFromParcel(android.os.Parcel _aidl_source) {
      AudioHalVersion _aidl_out = new AudioHalVersion();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioHalVersion[] newArray(int _aidl_size) {
      return new AudioHalVersion[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeInt(major);
    _aidl_parcel.writeInt(minor);
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
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      major = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      minor = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
  public static @interface Type {
    /**
     * Indicate the audio HAL is implemented with HIDL (HAL interface definition language).
     * @see <a href="https://source.android.com/docs/core/architecture/hidl/">HIDL</a>
     */
    public static final int HIDL = 0;
    /**
     * Indicate the audio HAL is implemented with AIDL (Android Interface Definition Language).
     * @see <a href="https://source.android.com/docs/core/architecture/aidl/">AIDL</a>
     */
    public static final int AIDL = 1;
  }
}
