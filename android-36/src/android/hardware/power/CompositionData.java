/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 6 --hash 13171cf98a48de298baf85167633376ea3db4ea0 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen/android/hardware/power/CompositionData.java.d -o out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen -Nhardware/interfaces/power/aidl/aidl_api/android.hardware.power/6 hardware/interfaces/power/aidl/aidl_api/android.hardware.power/6/android/hardware/power/CompositionData.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.power;
public class CompositionData implements android.os.Parcelable
{
  public long timestampNanos = 0L;
  public long[] scheduledPresentTimestampsNanos;
  public long latchTimestampNanos = 0L;
  public android.hardware.power.FrameProducer[] producers;
  public android.hardware.power.CompositionUpdate updateData;
  public long[] outputIds;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CompositionData> CREATOR = new android.os.Parcelable.Creator<CompositionData>() {
    @Override
    public CompositionData createFromParcel(android.os.Parcel _aidl_source) {
      CompositionData _aidl_out = new CompositionData();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CompositionData[] newArray(int _aidl_size) {
      return new CompositionData[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(timestampNanos);
    _aidl_parcel.writeLongArray(scheduledPresentTimestampsNanos);
    _aidl_parcel.writeLong(latchTimestampNanos);
    _aidl_parcel.writeTypedArray(producers, _aidl_flag);
    _aidl_parcel.writeTypedObject(updateData, _aidl_flag);
    _aidl_parcel.writeLongArray(outputIds);
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
      timestampNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      scheduledPresentTimestampsNanos = _aidl_parcel.createLongArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      latchTimestampNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      producers = _aidl_parcel.createTypedArray(android.hardware.power.FrameProducer.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      updateData = _aidl_parcel.readTypedObject(android.hardware.power.CompositionUpdate.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      outputIds = _aidl_parcel.createLongArray();
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
    _mask |= describeContents(producers);
    _mask |= describeContents(updateData);
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
