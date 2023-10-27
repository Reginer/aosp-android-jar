/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.suspend.internal;
/**
 * Parcelable WakelockInfo - Representation of wake lock stats.
 * 
 * Wakelocks obtained via SystemSuspend are hereafter referred to as
 * native wake locks.
 * 
 * @name:               Name of wake lock (Not guaranteed to be unique).
 * @activeCount:        Number of times the wake lock was activated.
 * @lastChange:         Monotonic time (in ms) when the wake lock was last touched.
 * @maxTime:            Maximum time (in ms) this wake lock has been continuously active.
 * @totalTime:          Total time (in ms) this wake lock has been active.
 * @isActive:           Status of wake lock.
 * @activeTime:         Time since wake lock was activated, 0 if wake lock is not active.
 * @isKernelWakelock:   True if kernel wake lock, false if native wake lock.
 * 
 * The stats below are specific to NATIVE wake locks and hold no valid
 * data in the context of kernel wake locks.
 * 
 * @pid:                Pid of process that acquired native wake lock.
 * 
 * The stats below are specific to KERNEL wake locks and hold no valid
 * data in the context of native wake locks.
 * 
 * @eventCount:         Number of signaled wakeup events.
 * @expireCount:        Number times the wakeup source's timeout expired.
 * @preventSuspendTime: Total time this wake lock has been preventing autosuspend.
 * @wakeupCount:        Number of times the wakeup source might abort suspend.
 */
public class WakeLockInfo implements android.os.Parcelable
{
  public java.lang.String name;
  public long activeCount = 0L;
  public long lastChange = 0L;
  public long maxTime = 0L;
  public long totalTime = 0L;
  public boolean isActive = false;
  public long activeTime = 0L;
  public boolean isKernelWakelock = false;
  // ---- Specific to Native Wake locks ---- //
  public int pid = 0;
  // ---- Specific to Kernel Wake locks ---- //
  public long eventCount = 0L;
  public long expireCount = 0L;
  public long preventSuspendTime = 0L;
  public long wakeupCount = 0L;
  public static final android.os.Parcelable.Creator<WakeLockInfo> CREATOR = new android.os.Parcelable.Creator<WakeLockInfo>() {
    @Override
    public WakeLockInfo createFromParcel(android.os.Parcel _aidl_source) {
      WakeLockInfo _aidl_out = new WakeLockInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public WakeLockInfo[] newArray(int _aidl_size) {
      return new WakeLockInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeLong(activeCount);
    _aidl_parcel.writeLong(lastChange);
    _aidl_parcel.writeLong(maxTime);
    _aidl_parcel.writeLong(totalTime);
    _aidl_parcel.writeInt(((isActive)?(1):(0)));
    _aidl_parcel.writeLong(activeTime);
    _aidl_parcel.writeInt(((isKernelWakelock)?(1):(0)));
    _aidl_parcel.writeInt(pid);
    _aidl_parcel.writeLong(eventCount);
    _aidl_parcel.writeLong(expireCount);
    _aidl_parcel.writeLong(preventSuspendTime);
    _aidl_parcel.writeLong(wakeupCount);
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
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      activeCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lastChange = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxTime = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      totalTime = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isActive = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      activeTime = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isKernelWakelock = (0!=_aidl_parcel.readInt());
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      eventCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      expireCount = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      preventSuspendTime = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      wakeupCount = _aidl_parcel.readLong();
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
