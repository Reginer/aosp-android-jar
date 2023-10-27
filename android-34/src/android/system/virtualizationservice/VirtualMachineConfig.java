/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.virtualizationservice;
/** Configuration for running a VM */
public final class VirtualMachineConfig implements android.os.Parcelable {
  // tags for union fields
  public final static int appConfig = 0;  // android.system.virtualizationservice.VirtualMachineAppConfig appConfig;
  public final static int rawConfig = 1;  // android.system.virtualizationservice.VirtualMachineRawConfig rawConfig;

  private int _tag;
  private Object _value;

  public VirtualMachineConfig() {
    android.system.virtualizationservice.VirtualMachineAppConfig _value = null;
    this._tag = appConfig;
    this._value = _value;
  }

  private VirtualMachineConfig(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private VirtualMachineConfig(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.system.virtualizationservice.VirtualMachineAppConfig appConfig;

  /** Configuration for a VM to run an app */
  public static VirtualMachineConfig appConfig(android.system.virtualizationservice.VirtualMachineAppConfig _value) {
    return new VirtualMachineConfig(appConfig, _value);
  }

  public android.system.virtualizationservice.VirtualMachineAppConfig getAppConfig() {
    _assertTag(appConfig);
    return (android.system.virtualizationservice.VirtualMachineAppConfig) _value;
  }

  public void setAppConfig(android.system.virtualizationservice.VirtualMachineAppConfig _value) {
    _set(appConfig, _value);
  }

  // android.system.virtualizationservice.VirtualMachineRawConfig rawConfig;

  /** Configuration for a VM with low-level configuration */
  public static VirtualMachineConfig rawConfig(android.system.virtualizationservice.VirtualMachineRawConfig _value) {
    return new VirtualMachineConfig(rawConfig, _value);
  }

  public android.system.virtualizationservice.VirtualMachineRawConfig getRawConfig() {
    _assertTag(rawConfig);
    return (android.system.virtualizationservice.VirtualMachineRawConfig) _value;
  }

  public void setRawConfig(android.system.virtualizationservice.VirtualMachineRawConfig _value) {
    _set(rawConfig, _value);
  }

  public static final android.os.Parcelable.Creator<VirtualMachineConfig> CREATOR = new android.os.Parcelable.Creator<VirtualMachineConfig>() {
    @Override
    public VirtualMachineConfig createFromParcel(android.os.Parcel _aidl_source) {
      return new VirtualMachineConfig(_aidl_source);
    }
    @Override
    public VirtualMachineConfig[] newArray(int _aidl_size) {
      return new VirtualMachineConfig[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case appConfig:
      _aidl_parcel.writeTypedObject(getAppConfig(), _aidl_flag);
      break;
    case rawConfig:
      _aidl_parcel.writeTypedObject(getRawConfig(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case appConfig: {
      android.system.virtualizationservice.VirtualMachineAppConfig _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.system.virtualizationservice.VirtualMachineAppConfig.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case rawConfig: {
      android.system.virtualizationservice.VirtualMachineRawConfig _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.system.virtualizationservice.VirtualMachineRawConfig.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case appConfig:
      _mask |= describeContents(getAppConfig());
      break;
    case rawConfig:
      _mask |= describeContents(getRawConfig());
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
    case appConfig: return "appConfig";
    case rawConfig: return "rawConfig";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    /** Configuration for a VM to run an app */
    public static final int appConfig = 0;
    /** Configuration for a VM with low-level configuration */
    public static final int rawConfig = 1;
  }
}
