/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/audio-permission-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audioclient-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-types-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/capture_state_listener-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/native/libs/permission/framework-permission-aidl_interface/preprocessed.aidl -pout/soong/.intermediates/frameworks/av/media/libaudioclient/spatializer-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen/android/media/RecordClientInfo.java.d -o out/soong/.intermediates/frameworks/av/media/libaudioclient/audiopolicy-aidl-java-source/gen -Nframeworks/av/media/libaudioclient/aidl frameworks/av/media/libaudioclient/aidl/android/media/RecordClientInfo.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media;
/** {@hide} */
public class RecordClientInfo implements android.os.Parcelable
{
  /** Interpreted as audio_unique_id_t. */
  public int riid = 0;
  /** Interpreted as uid_t. */
  public int uid = 0;
  /** Interpreted as audio_session_t. */
  public int session = 0;
  public int source;
  /** Interpreted as audio_port_handle_t. */
  public int portId = 0;
  public boolean silenced = false;
  public static final android.os.Parcelable.Creator<RecordClientInfo> CREATOR = new android.os.Parcelable.Creator<RecordClientInfo>() {
    @Override
    public RecordClientInfo createFromParcel(android.os.Parcel _aidl_source) {
      RecordClientInfo _aidl_out = new RecordClientInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RecordClientInfo[] newArray(int _aidl_size) {
      return new RecordClientInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(riid);
    _aidl_parcel.writeInt(uid);
    _aidl_parcel.writeInt(session);
    _aidl_parcel.writeInt(source);
    _aidl_parcel.writeInt(portId);
    _aidl_parcel.writeBoolean(silenced);
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
      riid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      session = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      source = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      portId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      silenced = _aidl_parcel.readBoolean();
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
