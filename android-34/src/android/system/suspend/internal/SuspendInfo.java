/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.suspend.internal;
public class SuspendInfo implements android.os.Parcelable
{
  /** Total number of times that suspend was attempted */
  public long suspendAttemptCount = 0L;
  /** Total number of times that suspend attempt failed */
  public long failedSuspendCount = 0L;
  /**
   * Total number of times that a short suspend occurred. A successful suspend is considered a
   * short suspend if the suspend duration is less than suspend.short_suspend_threshold_millis
   */
  public long shortSuspendCount = 0L;
  /** Total time, in milliseconds, spent in suspend */
  public long suspendTimeMillis = 0L;
  /** Total time, in milliseconds, spent in short suspends */
  public long shortSuspendTimeMillis = 0L;
  /** Total time, in milliseconds, spent doing suspend/resume work for successful suspends */
  public long suspendOverheadTimeMillis = 0L;
  /** Total time, in milliseconds, spent doing suspend/resume work for failed suspends */
  public long failedSuspendOverheadTimeMillis = 0L;
  /**
   * Total number of times the number of consecutive bad (short, failed) suspends
   * crossed suspend.backoff_threshold_count
   */
  public long newBackoffCount = 0L;
  /**
   * Total number of times the number of consecutive bad (short, failed) suspends
   * exceeded suspend.backoff_threshold_count
   */
  public long backoffContinueCount = 0L;
  /** Total time, in milliseconds, that system has waited between suspend attempts */
  public long sleepTimeMillis = 0L;
  public static final android.os.Parcelable.Creator<SuspendInfo> CREATOR = new android.os.Parcelable.Creator<SuspendInfo>() {
    @Override
    public SuspendInfo createFromParcel(android.os.Parcel _aidl_source) {
      SuspendInfo _aidl_out = new SuspendInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SuspendInfo[] newArray(int _aidl_size) {
      return new SuspendInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(suspendAttemptCount);
    _aidl_parcel.writeLong(failedSuspendCount);
    _aidl_parcel.writeLong(shortSuspendCount);
    _aidl_parcel.writeLong(suspendTimeMillis);
    _aidl_parcel.writeLong(shortSuspendTimeMillis);
    _aidl_parcel.writeLong(suspendOverheadTimeMillis);
    _aidl_parcel.writeLong(failedSuspendOverheadTimeMillis);
    _aidl_parcel.writeLong(newBackoffCount);
    _aidl_parcel.writeLong(backoffContinueCount);
    _aidl_parcel.writeLong(sleepTimeMillis);
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
      suspendAttemptCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      failedSuspendCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      shortSuspendCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      suspendTimeMillis = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      shortSuspendTimeMillis = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      suspendOverheadTimeMillis = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      failedSuspendOverheadTimeMillis = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      newBackoffCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      backoffContinueCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sleepTimeMillis = _aidl_parcel.readLong();
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
