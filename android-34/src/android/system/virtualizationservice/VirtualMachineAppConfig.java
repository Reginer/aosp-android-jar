/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
/** Configuration for running an App in a VM */
public class VirtualMachineAppConfig implements android.os.Parcelable
{
  /** Name of VM */
  public java.lang.String name;
  /** Main APK */
  public android.os.ParcelFileDescriptor apk;
  /** idsig for an APK */
  public android.os.ParcelFileDescriptor idsig;
  /** Idsigs for the extra APKs. Must match with the extra_apks in the payload config. */
  public java.util.List<android.os.ParcelFileDescriptor> extraIdsigs;
  /** instance.img that has per-instance data */
  public android.os.ParcelFileDescriptor instanceImage;
  /**
   * This backs the persistent, encrypted storage in vm.
   * It also comes with some integrity guarantees.
   * Note: Storage is an optional feature
   */
  public android.os.ParcelFileDescriptor encryptedStorageImage;
  /** Detailed configuration for the VM, specifying how the payload will be run. */
  public android.system.virtualizationservice.VirtualMachineAppConfig.Payload payload;
  /** Debug level of the VM */
  public byte debugLevel = android.system.virtualizationservice.VirtualMachineAppConfig.DebugLevel.NONE;
  /**
   * Port at which crosvm will start a gdb server to debug guest kernel.
   * If set to zero, then gdb server won't be started.
   */
  public int gdbPort = 0;
  /** Whether the VM should be a protected VM. */
  public boolean protectedVm = false;
  /**
   * The amount of RAM to give the VM, in MiB. If this is 0 or negative then it will default to
   * the value in microdroid.json, if any, or the crosvm default.
   */
  public int memoryMib = 0;
  /** The vCPU topology that will be generated for the VM. Default to 1 vCPU. */
  public byte cpuTopology = android.system.virtualizationservice.CpuTopology.ONE_CPU;
  /**
   * List of task profile names to apply for the VM
   * 
   * Note: Specifying a value here requires android.permission.USE_CUSTOM_VIRTUAL_MACHINE.
   */
  public java.lang.String[] taskProfiles;
  public static final android.os.Parcelable.Creator<VirtualMachineAppConfig> CREATOR = new android.os.Parcelable.Creator<VirtualMachineAppConfig>() {
    @Override
    public VirtualMachineAppConfig createFromParcel(android.os.Parcel _aidl_source) {
      VirtualMachineAppConfig _aidl_out = new VirtualMachineAppConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public VirtualMachineAppConfig[] newArray(int _aidl_size) {
      return new VirtualMachineAppConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeTypedObject(apk, _aidl_flag);
    _aidl_parcel.writeTypedObject(idsig, _aidl_flag);
    _aidl_parcel.writeTypedList(extraIdsigs, _aidl_flag);
    _aidl_parcel.writeTypedObject(instanceImage, _aidl_flag);
    _aidl_parcel.writeTypedObject(encryptedStorageImage, _aidl_flag);
    _aidl_parcel.writeTypedObject(payload, _aidl_flag);
    _aidl_parcel.writeByte(debugLevel);
    _aidl_parcel.writeInt(gdbPort);
    _aidl_parcel.writeBoolean(protectedVm);
    _aidl_parcel.writeInt(memoryMib);
    _aidl_parcel.writeByte(cpuTopology);
    _aidl_parcel.writeStringArray(taskProfiles);
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
      apk = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      idsig = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      extraIdsigs = _aidl_parcel.createTypedArrayList(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      instanceImage = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      encryptedStorageImage = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      payload = _aidl_parcel.readTypedObject(android.system.virtualizationservice.VirtualMachineAppConfig.Payload.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      debugLevel = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gdbPort = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      protectedVm = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      memoryMib = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cpuTopology = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      taskProfiles = _aidl_parcel.createStringArray();
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
    _mask |= describeContents(apk);
    _mask |= describeContents(idsig);
    _mask |= describeContents(extraIdsigs);
    _mask |= describeContents(instanceImage);
    _mask |= describeContents(encryptedStorageImage);
    _mask |= describeContents(payload);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof java.util.Collection) {
      int _mask = 0;
      for (Object o : (java.util.Collection) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
  public static final class Payload implements android.os.Parcelable {
    // tags for union fields
    public final static int configPath = 0;  // String configPath;
    public final static int payloadConfig = 1;  // android.system.virtualizationservice.VirtualMachinePayloadConfig payloadConfig;

    private int _tag;
    private Object _value;

    public Payload() {
      java.lang.String _value = null;
      this._tag = configPath;
      this._value = _value;
    }

    private Payload(android.os.Parcel _aidl_parcel) {
      readFromParcel(_aidl_parcel);
    }

    private Payload(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }

    public int getTag() {
      return _tag;
    }

    // String configPath;

    /** Path to a JSON file in an APK containing the configuration. */
    public static Payload configPath(java.lang.String _value) {
      return new Payload(configPath, _value);
    }

    public java.lang.String getConfigPath() {
      _assertTag(configPath);
      return (java.lang.String) _value;
    }

    public void setConfigPath(java.lang.String _value) {
      _set(configPath, _value);
    }

    // android.system.virtualizationservice.VirtualMachinePayloadConfig payloadConfig;

    /** Configuration provided explicitly. */
    public static Payload payloadConfig(android.system.virtualizationservice.VirtualMachinePayloadConfig _value) {
      return new Payload(payloadConfig, _value);
    }

    public android.system.virtualizationservice.VirtualMachinePayloadConfig getPayloadConfig() {
      _assertTag(payloadConfig);
      return (android.system.virtualizationservice.VirtualMachinePayloadConfig) _value;
    }

    public void setPayloadConfig(android.system.virtualizationservice.VirtualMachinePayloadConfig _value) {
      _set(payloadConfig, _value);
    }

    public static final android.os.Parcelable.Creator<Payload> CREATOR = new android.os.Parcelable.Creator<Payload>() {
      @Override
      public Payload createFromParcel(android.os.Parcel _aidl_source) {
        return new Payload(_aidl_source);
      }
      @Override
      public Payload[] newArray(int _aidl_size) {
        return new Payload[_aidl_size];
      }
    };

    @Override
    public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
      _aidl_parcel.writeInt(_tag);
      switch (_tag) {
      case configPath:
        _aidl_parcel.writeString(getConfigPath());
        break;
      case payloadConfig:
        _aidl_parcel.writeTypedObject(getPayloadConfig(), _aidl_flag);
        break;
      }
    }

    public void readFromParcel(android.os.Parcel _aidl_parcel) {
      int _aidl_tag;
      _aidl_tag = _aidl_parcel.readInt();
      switch (_aidl_tag) {
      case configPath: {
        java.lang.String _aidl_value;
        _aidl_value = _aidl_parcel.readString();
        _set(_aidl_tag, _aidl_value);
        return; }
      case payloadConfig: {
        android.system.virtualizationservice.VirtualMachinePayloadConfig _aidl_value;
        _aidl_value = _aidl_parcel.readTypedObject(android.system.virtualizationservice.VirtualMachinePayloadConfig.CREATOR);
        _set(_aidl_tag, _aidl_value);
        return; }
      }
      throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
    }

    @Override
    public int describeContents() {
      int _mask = 0;
      switch (getTag()) {
      case payloadConfig:
        _mask |= describeContents(getPayloadConfig());
        break;
      }
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }

    private void _assertTag(int tag) {
      if (getTag() != tag) {
        throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
      }
    }

    private String _tagString(int _tag) {
      switch (_tag) {
      case configPath: return "configPath";
      case payloadConfig: return "payloadConfig";
      }
      throw new IllegalStateException("unknown field: " + _tag);
    }

    private void _set(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }
    public static @interface Tag {
      /** Path to a JSON file in an APK containing the configuration. */
      public static final int configPath = 0;
      /** Configuration provided explicitly. */
      public static final int payloadConfig = 1;
    }
  }
  public static @interface DebugLevel {
    /** Not debuggable at all */
    public static final byte NONE = 0;
    /**
     * Fully debuggable. All logs are shown, kernel messages are shown, and adb shell is
     * supported
     */
    public static final byte FULL = 1;
  }
}
