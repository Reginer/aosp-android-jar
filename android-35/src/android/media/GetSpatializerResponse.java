/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/3/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/capture_state_listener-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/spatializer-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen/android/media/GetSpatializerResponse.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/GetSpatializerResponse.aidl
 */
package android.media;
/**
 * Used as a return value for IAudioPolicyService.getSpatializer() method
 * {@hide}
 */
public class GetSpatializerResponse implements android.os.Parcelable
{
  /** The ISpatializer interface if successful, null if not */
  public android.media.ISpatializer spatializer;
  public static final android.os.Parcelable.Creator<GetSpatializerResponse> CREATOR = new android.os.Parcelable.Creator<GetSpatializerResponse>() {
    @Override
    public GetSpatializerResponse createFromParcel(android.os.Parcel _aidl_source) {
      GetSpatializerResponse _aidl_out = new GetSpatializerResponse();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GetSpatializerResponse[] newArray(int _aidl_size) {
      return new GetSpatializerResponse[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeStrongInterface(spatializer);
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
      spatializer = android.media.ISpatializer.Stub.asInterface(_aidl_parcel.readStrongBinder());
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
}
