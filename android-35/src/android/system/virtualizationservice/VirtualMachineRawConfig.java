/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationcommon_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen/android/system/virtualizationservice/VirtualMachineRawConfig.java.d -o out/soong/.intermediates/packages/modules/Virtualization/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen -Npackages/modules/Virtualization/virtualizationservice/aidl packages/modules/Virtualization/virtualizationservice/aidl/android/system/virtualizationservice/VirtualMachineRawConfig.aidl
 */
package android.system.virtualizationservice;
/** Raw configuration for running a VM. */
public class VirtualMachineRawConfig implements android.os.Parcelable
{
  /** Name of VM */
  public java.lang.String name;
  /** Id of the VM instance */
  public byte[] instanceId;
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
  /**
   * Port at which crosvm will start a gdb server to debug guest kernel.
   * If set to zero, then gdb server won't be started.
   */
  public int gdbPort = 0;
  /**
   *  Ask the kernel for transparent huge-pages (THP). This is only a hint and
   *  the kernel will allocate THP-backed memory only if globally enabled by
   *  the system and if any can be found. See
   *  https://docs.kernel.org/admin-guide/mm/transhuge.html
   */
  public boolean hugePages = false;
  /** List of SysFS nodes of devices to be assigned */
  public java.lang.String[] devices;
  public android.system.virtualizationservice.DisplayConfig displayConfig;
  /** List of input devices to the VM */
  public android.system.virtualizationservice.InputDevice[] inputDevices;
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
    _aidl_parcel.writeFixedArray(instanceId, _aidl_flag, 64);
    _aidl_parcel.writeTypedObject(kernel, _aidl_flag);
    _aidl_parcel.writeTypedObject(initrd, _aidl_flag);
    _aidl_parcel.writeString(params);
    _aidl_parcel.writeTypedObject(bootloader, _aidl_flag);
    _aidl_parcel.writeTypedArray(disks, _aidl_flag);
    _aidl_parcel.writeBoolean(protectedVm);
    _aidl_parcel.writeInt(memoryMib);
    _aidl_parcel.writeByte(cpuTopology);
    _aidl_parcel.writeString(platformVersion);
    _aidl_parcel.writeInt(gdbPort);
    _aidl_parcel.writeBoolean(hugePages);
    _aidl_parcel.writeStringArray(devices);
    _aidl_parcel.writeTypedObject(displayConfig, _aidl_flag);
    _aidl_parcel.writeTypedArray(inputDevices, _aidl_flag);
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
      instanceId = _aidl_parcel.createFixedArray(byte[].class, 64);
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
      gdbPort = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hugePages = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      devices = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      displayConfig = _aidl_parcel.readTypedObject(android.system.virtualizationservice.DisplayConfig.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      inputDevices = _aidl_parcel.createTypedArray(android.system.virtualizationservice.InputDevice.CREATOR);
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
    _mask |= describeContents(displayConfig);
    _mask |= describeContents(inputDevices);
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
