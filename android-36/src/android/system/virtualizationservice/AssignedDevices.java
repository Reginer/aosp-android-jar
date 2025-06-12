/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationcommon_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen/android/system/virtualizationservice/AssignedDevices.java.d -o out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen -Npackages/modules/Virtualization/android/virtualizationservice/aidl packages/modules/Virtualization/android/virtualizationservice/aidl/android/system/virtualizationservice/AssignedDevices.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.system.virtualizationservice;
/** Assigned devices */
public final class AssignedDevices implements android.os.Parcelable {
  // tags for union fields
  public final static int devices = 0;  // String[] devices;
  public final static int dtbo = 1;  // ParcelFileDescriptor dtbo;

  private int _tag;
  private Object _value;

  public AssignedDevices() {
    java.lang.String[] _value = {};
    this._tag = devices;
    this._value = _value;
  }

  private AssignedDevices(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AssignedDevices(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // String[] devices;

  /** List of SysFS nodes of devices to be assigned for VFIO */
  public static AssignedDevices devices(java.lang.String[] _value) {
    return new AssignedDevices(devices, _value);
  }

  public java.lang.String[] getDevices() {
    _assertTag(devices);
    return (java.lang.String[]) _value;
  }

  public void setDevices(java.lang.String[] _value) {
    _set(devices, _value);
  }

  // ParcelFileDescriptor dtbo;

  /** Device tree overlay for non-VFIO case */
  public static AssignedDevices dtbo(android.os.ParcelFileDescriptor _value) {
    return new AssignedDevices(dtbo, _value);
  }

  public android.os.ParcelFileDescriptor getDtbo() {
    _assertTag(dtbo);
    return (android.os.ParcelFileDescriptor) _value;
  }

  public void setDtbo(android.os.ParcelFileDescriptor _value) {
    _set(dtbo, _value);
  }

  public static final android.os.Parcelable.Creator<AssignedDevices> CREATOR = new android.os.Parcelable.Creator<AssignedDevices>() {
    @Override
    public AssignedDevices createFromParcel(android.os.Parcel _aidl_source) {
      return new AssignedDevices(_aidl_source);
    }
    @Override
    public AssignedDevices[] newArray(int _aidl_size) {
      return new AssignedDevices[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case devices:
      _aidl_parcel.writeStringArray(getDevices());
      break;
    case dtbo:
      _aidl_parcel.writeTypedObject(getDtbo(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case devices: {
      java.lang.String[] _aidl_value;
      _aidl_value = _aidl_parcel.createStringArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dtbo: {
      android.os.ParcelFileDescriptor _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.os.ParcelFileDescriptor.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case dtbo:
      _mask |= describeContents(getDtbo());
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
    case devices: return "devices";
    case dtbo: return "dtbo";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    /** List of SysFS nodes of devices to be assigned for VFIO */
    public static final int devices = 0;
    /** Device tree overlay for non-VFIO case */
    public static final int dtbo = 1;
  }
}
