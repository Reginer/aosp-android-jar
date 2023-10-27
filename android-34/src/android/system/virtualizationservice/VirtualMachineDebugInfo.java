/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
/** Information about a running VM, for debug purposes only. */
public class VirtualMachineDebugInfo implements android.os.Parcelable
{
  /** The CID assigned to the VM. */
  public int cid = 0;
  /** Directory of temporary files used by the VM while it is running. */
  public java.lang.String temporaryDirectory;
  /** The UID of the process which requested the VM. */
  public int requesterUid = 0;
  /**
   * The PID of the process which requested the VM. Note that this process may no longer exist and
   * the PID may have been reused for a different process, so this should not be trusted.
   */
  public int requesterPid = 0;
  public static final android.os.Parcelable.Creator<VirtualMachineDebugInfo> CREATOR = new android.os.Parcelable.Creator<VirtualMachineDebugInfo>() {
    @Override
    public VirtualMachineDebugInfo createFromParcel(android.os.Parcel _aidl_source) {
      VirtualMachineDebugInfo _aidl_out = new VirtualMachineDebugInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public VirtualMachineDebugInfo[] newArray(int _aidl_size) {
      return new VirtualMachineDebugInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(cid);
    _aidl_parcel.writeString(temporaryDirectory);
    _aidl_parcel.writeInt(requesterUid);
    _aidl_parcel.writeInt(requesterPid);
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
      cid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      temporaryDirectory = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      requesterUid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      requesterPid = _aidl_parcel.readInt();
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
