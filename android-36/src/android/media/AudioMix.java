/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl-java-source/gen/android/media/AudioMix.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/AudioMix.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public class AudioMix implements android.os.Parcelable
{
  public android.media.AudioMixMatchCriterion[] criteria;
  public int mixType;
  public android.media.audio.common.AudioConfig format;
  /** Bitmask, indexed by AudioMixRouteFlag. */
  public int routeFlags = 0;
  public android.media.audio.common.AudioDevice device;
  /** Flags indicating which callbacks to use. Bitmask, indexed by AudioMixCallbackFlag. */
  public int cbFlags = 0;
  /** Ignore the AUDIO_FLAG_NO_MEDIA_PROJECTION */
  public boolean allowPrivilegedMediaPlaybackCapture = false;
  /** Indicates if the caller can capture voice communication output */
  public boolean voiceCommunicationCaptureAllowed = false;
  /** Identifies the owner of the AudioPolicy that this AudioMix belongs to */
  public android.os.IBinder mToken;
  /** Indicates the Id of the VirtualDevice this AudioMix was registered for */
  public int mVirtualDeviceId = 0;
  public static final android.os.Parcelable.Creator<AudioMix> CREATOR = new android.os.Parcelable.Creator<AudioMix>() {
    @Override
    public AudioMix createFromParcel(android.os.Parcel _aidl_source) {
      AudioMix _aidl_out = new AudioMix();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioMix[] newArray(int _aidl_size) {
      return new AudioMix[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedArray(criteria, _aidl_flag);
    _aidl_parcel.writeInt(mixType);
    _aidl_parcel.writeTypedObject(format, _aidl_flag);
    _aidl_parcel.writeInt(routeFlags);
    _aidl_parcel.writeTypedObject(device, _aidl_flag);
    _aidl_parcel.writeInt(cbFlags);
    _aidl_parcel.writeBoolean(allowPrivilegedMediaPlaybackCapture);
    _aidl_parcel.writeBoolean(voiceCommunicationCaptureAllowed);
    _aidl_parcel.writeStrongBinder(mToken);
    _aidl_parcel.writeInt(mVirtualDeviceId);
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
      criteria = _aidl_parcel.createTypedArray(android.media.AudioMixMatchCriterion.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mixType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      format = _aidl_parcel.readTypedObject(android.media.audio.common.AudioConfig.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      routeFlags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      device = _aidl_parcel.readTypedObject(android.media.audio.common.AudioDevice.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cbFlags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      allowPrivilegedMediaPlaybackCapture = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      voiceCommunicationCaptureAllowed = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mToken = _aidl_parcel.readStrongBinder();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mVirtualDeviceId = _aidl_parcel.readInt();
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
    _mask |= describeContents(criteria);
    _mask |= describeContents(format);
    _mask |= describeContents(device);
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
}
