/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/audio-permission-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/capture_state_listener-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/spatializer-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen/android/media/GetOutputForAttrResponse.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/GetOutputForAttrResponse.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public class GetOutputForAttrResponse implements android.os.Parcelable
{
  /** Interpreted as audio_io_handle_t. */
  public int output = 0;
  public int stream;
  /** Interpreted as audio_port_handle_t[]. */
  public int[] selectedDeviceIds;
  /** Interpreted as audio_port_handle_t. */
  public int portId = 0;
  /** Interpreted as audio_io_handle_t[]. */
  public int[] secondaryOutputs;
  /** True if the track is connected to a spatializer mixer and actually spatialized */
  public boolean isSpatialized = false;
  /** The suggested audio config if fails to get an output. * */
  public android.media.audio.common.AudioConfigBase configBase;
  public boolean isBitPerfect = false;
  /** The corrected audio attributes. * */
  public android.media.audio.common.AudioAttributes attr;
  /** initial port volume for the new audio track */
  public float volume = 0.000000f;
  /** initial port muted state for the new audio track */
  public boolean muted = false;
  public static final android.os.Parcelable.Creator<GetOutputForAttrResponse> CREATOR = new android.os.Parcelable.Creator<GetOutputForAttrResponse>() {
    @Override
    public GetOutputForAttrResponse createFromParcel(android.os.Parcel _aidl_source) {
      GetOutputForAttrResponse _aidl_out = new GetOutputForAttrResponse();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GetOutputForAttrResponse[] newArray(int _aidl_size) {
      return new GetOutputForAttrResponse[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(output);
    _aidl_parcel.writeInt(stream);
    _aidl_parcel.writeIntArray(selectedDeviceIds);
    _aidl_parcel.writeInt(portId);
    _aidl_parcel.writeIntArray(secondaryOutputs);
    _aidl_parcel.writeBoolean(isSpatialized);
    _aidl_parcel.writeTypedObject(configBase, _aidl_flag);
    _aidl_parcel.writeBoolean(isBitPerfect);
    _aidl_parcel.writeTypedObject(attr, _aidl_flag);
    _aidl_parcel.writeFloat(volume);
    _aidl_parcel.writeBoolean(muted);
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
      output = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      stream = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      selectedDeviceIds = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      portId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      secondaryOutputs = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isSpatialized = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      configBase = _aidl_parcel.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isBitPerfect = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      attr = _aidl_parcel.readTypedObject(android.media.audio.common.AudioAttributes.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      volume = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      muted = _aidl_parcel.readBoolean();
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
    _mask |= describeContents(configBase);
    _mask |= describeContents(attr);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
