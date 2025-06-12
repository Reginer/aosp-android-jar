/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 6 --hash 13171cf98a48de298baf85167633376ea3db4ea0 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen/android/hardware/power/WorkDuration.java.d -o out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen -Nhardware/interfaces/power/aidl/aidl_api/android.hardware.power/6 hardware/interfaces/power/aidl/aidl_api/android.hardware.power/6/android/hardware/power/WorkDuration.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.power;
public class WorkDuration implements android.os.Parcelable
{
  public long timeStampNanos = 0L;
  public long durationNanos = 0L;
  public long workPeriodStartTimestampNanos = 0L;
  public long cpuDurationNanos = 0L;
  public long gpuDurationNanos = 0L;
  public long intendedPresentTimestampNanos = 0L;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<WorkDuration> CREATOR = new android.os.Parcelable.Creator<WorkDuration>() {
    @Override
    public WorkDuration createFromParcel(android.os.Parcel _aidl_source) {
      WorkDuration _aidl_out = new WorkDuration();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public WorkDuration[] newArray(int _aidl_size) {
      return new WorkDuration[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(timeStampNanos);
    _aidl_parcel.writeLong(durationNanos);
    _aidl_parcel.writeLong(workPeriodStartTimestampNanos);
    _aidl_parcel.writeLong(cpuDurationNanos);
    _aidl_parcel.writeLong(gpuDurationNanos);
    _aidl_parcel.writeLong(intendedPresentTimestampNanos);
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
      timeStampNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      durationNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      workPeriodStartTimestampNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cpuDurationNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gpuDurationNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      intendedPresentTimestampNanos = _aidl_parcel.readLong();
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
