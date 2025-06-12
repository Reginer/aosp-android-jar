/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/audio-permission-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/capture_state_listener-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/spatializer-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen/android/media/GetInputForAttrResponse.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/GetInputForAttrResponse.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public class GetInputForAttrResponse implements android.os.Parcelable
{
  /** Interpreted as audio_io_handle_t. */
  public int input = 0;
  /** Interpreted as audio_port_handle_t. */
  public int selectedDeviceId = 0;
  /** Interpreted as audio_port_handle_t. */
  public int portId = 0;
  /** The virtual device id corresponding to the opened input. */
  public int virtualDeviceId = 0;
  /** The suggested config if fails to get an input. * */
  public android.media.audio.common.AudioConfigBase config;
  /** The audio source, possibly updated by audio policy manager */
  public int source;
  public static final android.os.Parcelable.Creator<GetInputForAttrResponse> CREATOR = new android.os.Parcelable.Creator<GetInputForAttrResponse>() {
    @Override
    public GetInputForAttrResponse createFromParcel(android.os.Parcel _aidl_source) {
      GetInputForAttrResponse _aidl_out = new GetInputForAttrResponse();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GetInputForAttrResponse[] newArray(int _aidl_size) {
      return new GetInputForAttrResponse[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(input);
    _aidl_parcel.writeInt(selectedDeviceId);
    _aidl_parcel.writeInt(portId);
    _aidl_parcel.writeInt(virtualDeviceId);
    _aidl_parcel.writeTypedObject(config, _aidl_flag);
    _aidl_parcel.writeInt(source);
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
      input = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      selectedDeviceId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      portId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      virtualDeviceId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      config = _aidl_parcel.readTypedObject(android.media.audio.common.AudioConfigBase.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      source = _aidl_parcel.readInt();
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
    _mask |= describeContents(config);
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
