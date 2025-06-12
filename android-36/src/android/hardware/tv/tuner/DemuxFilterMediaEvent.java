/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/DemuxFilterMediaEvent.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/DemuxFilterMediaEvent.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public class DemuxFilterMediaEvent implements android.os.Parcelable
{
  public int streamId = 0;
  public boolean isPtsPresent = false;
  public long pts = 0L;
  public boolean isDtsPresent = false;
  public long dts = 0L;
  public long dataLength = 0L;
  public long offset = 0L;
  public android.hardware.common.NativeHandle avMemory;
  public boolean isSecureMemory = false;
  public long avDataId = 0L;
  public int mpuSequenceNumber = 0;
  public boolean isPesPrivateData = false;
  public android.hardware.tv.tuner.DemuxFilterMediaEventExtraMetaData extraMetaData;
  public android.hardware.tv.tuner.DemuxFilterScIndexMask scIndexMask;
  public int numDataPieces = 0;
  public int indexInDataGroup = 0;
  public int dataGroupId = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<DemuxFilterMediaEvent> CREATOR = new android.os.Parcelable.Creator<DemuxFilterMediaEvent>() {
    @Override
    public DemuxFilterMediaEvent createFromParcel(android.os.Parcel _aidl_source) {
      DemuxFilterMediaEvent _aidl_out = new DemuxFilterMediaEvent();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public DemuxFilterMediaEvent[] newArray(int _aidl_size) {
      return new DemuxFilterMediaEvent[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(streamId);
    _aidl_parcel.writeBoolean(isPtsPresent);
    _aidl_parcel.writeLong(pts);
    _aidl_parcel.writeBoolean(isDtsPresent);
    _aidl_parcel.writeLong(dts);
    _aidl_parcel.writeLong(dataLength);
    _aidl_parcel.writeLong(offset);
    _aidl_parcel.writeTypedObject(avMemory, _aidl_flag);
    _aidl_parcel.writeBoolean(isSecureMemory);
    _aidl_parcel.writeLong(avDataId);
    _aidl_parcel.writeInt(mpuSequenceNumber);
    _aidl_parcel.writeBoolean(isPesPrivateData);
    _aidl_parcel.writeTypedObject(extraMetaData, _aidl_flag);
    _aidl_parcel.writeTypedObject(scIndexMask, _aidl_flag);
    _aidl_parcel.writeInt(numDataPieces);
    _aidl_parcel.writeInt(indexInDataGroup);
    _aidl_parcel.writeInt(dataGroupId);
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
      streamId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isPtsPresent = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pts = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isDtsPresent = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      dts = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      dataLength = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      offset = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      avMemory = _aidl_parcel.readTypedObject(android.hardware.common.NativeHandle.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isSecureMemory = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      avDataId = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mpuSequenceNumber = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isPesPrivateData = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      extraMetaData = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterMediaEventExtraMetaData.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      scIndexMask = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterScIndexMask.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numDataPieces = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      indexInDataGroup = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      dataGroupId = _aidl_parcel.readInt();
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
    _mask |= describeContents(avMemory);
    _mask |= describeContents(extraMetaData);
    _mask |= describeContents(scIndexMask);
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
