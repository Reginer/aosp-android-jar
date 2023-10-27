/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
/** Raw configuration for running a VM. */
public class VirtualMachineRawConfig implements android.os.Parcelable
{
  /** Name of VM */
  public java.lang.String name;
  /** The kernel image, if any. */
  public android.os.ParcelFileDescriptor kernel;
  /** The initial ramdisk for the kernel, if any. */
  public android.os.ParcelFileDescriptor initrd;
  /**
   * Parameters to pass to the kernel. As far as the VMM and boot protocol are concerned this is
   * just a string, but typically it will contain multiple parameters separated by spaces.
   */
  public java.lang.String params;
  /**
   * The bootloader to use. If this is supplied then the kernel and initrd must not be supplied;
   * the bootloader is instead responsibly for loading the kernel from one of the disks.
   */
  public android.os.ParcelFileDescriptor bootloader;
  /** Disk images to be made available to the VM. */
  public android.system.virtualizationservice.DiskImage[] disks;
  /** Whether the VM should be a protected VM. */
  public boolean protectedVm = false;
  /** The amount of RAM to give the VM, in MiB. 0 or negative to use the default. */
  public int memoryMib = 0;
  /** The vCPU topology that will be generated for the VM. Default to 1 vCPU. */
  public byte cpuTopology = android.system.virtualizationservice.CpuTopology.ONE_CPU;
  /**
   * A version or range of versions of the virtual platform that this config is compatible with.
   * The format follows SemVer.
   */
  public java.lang.String platformVersion;
  /** List of task profile names to apply for the VM */
  public java.lang.String[] taskProfiles;
  /**
   * Port at which crosvm will start a gdb server to debug guest kernel.
   * If set to zero, then gdb server won't be started.
   */
  public int gdbPort = 0;
  public static final android.os.Parcelable.Creator<VirtualMachineRawConfig> CREATOR = new android.os.Parcelable.Creator<VirtualMachineRawConfig>() {
    @Override
    public VirtualMachineRawConfig createFromParcel(android.os.Parcel _aidl_source) {
      VirtualMachineRawConfig _aidl_out = new VirtualMachineRawConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public VirtualMachineRawConfig[] newArray(int _aidl_size) {
      return new VirtualMachineRawConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeTypedObject(kernel, _aidl_flag);
    _aidl_parcel.writeTypedObject(initrd, _aidl_flag);
    _aidl_parcel.writeString(params);
    _aidl_parcel.writeTypedObject(bootloader, _aidl_flag);
    _aidl_parcel.writeTypedArray(disks, _aidl_flag);
    _aidl_parcel.writeBoolean(protectedVm);
    _aidl_parcel.writeInt(memoryMib);
    _aidl_parcel.writeByte(cpuTopology);
    _aidl_parcel.writeString(platformVersion);
    _aidl_parcel.writeStringArray(taskProfiles);
    _aidl_parcel.writeInt(gdbPort);
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
      kernel = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      initrd = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      params = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bootloader = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      disks = _aidl_parcel.createTypedArray(android.system.virtualizationservice.DiskImage.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      protectedVm = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      memoryMib = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cpuTopology = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      platformVersion = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      taskProfiles = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gdbPort = _aidl_parcel.readInt();
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
    _mask |= describeContents(kernel);
    _mask |= describeContents(initrd);
    _mask |= describeContents(bootloader);
    _mask |= describeContents(disks);
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
