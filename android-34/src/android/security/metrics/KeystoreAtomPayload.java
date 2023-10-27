/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.metrics;
/** @hide */
public final class KeystoreAtomPayload implements android.os.Parcelable {
  // tags for union fields
  public final static int storageStats = 0;  // android.security.metrics.StorageStats storageStats;
  public final static int keyCreationWithGeneralInfo = 1;  // android.security.metrics.KeyCreationWithGeneralInfo keyCreationWithGeneralInfo;
  public final static int keyCreationWithAuthInfo = 2;  // android.security.metrics.KeyCreationWithAuthInfo keyCreationWithAuthInfo;
  public final static int keyCreationWithPurposeAndModesInfo = 3;  // android.security.metrics.KeyCreationWithPurposeAndModesInfo keyCreationWithPurposeAndModesInfo;
  public final static int keystore2AtomWithOverflow = 4;  // android.security.metrics.Keystore2AtomWithOverflow keystore2AtomWithOverflow;
  public final static int keyOperationWithPurposeAndModesInfo = 5;  // android.security.metrics.KeyOperationWithPurposeAndModesInfo keyOperationWithPurposeAndModesInfo;
  public final static int keyOperationWithGeneralInfo = 6;  // android.security.metrics.KeyOperationWithGeneralInfo keyOperationWithGeneralInfo;
  public final static int rkpErrorStats = 7;  // android.security.metrics.RkpErrorStats rkpErrorStats;
  public final static int crashStats = 8;  // android.security.metrics.CrashStats crashStats;

  private int _tag;
  private Object _value;

  public KeystoreAtomPayload() {
    android.security.metrics.StorageStats _value = null;
    this._tag = storageStats;
    this._value = _value;
  }

  private KeystoreAtomPayload(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private KeystoreAtomPayload(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.security.metrics.StorageStats storageStats;

  public static KeystoreAtomPayload storageStats(android.security.metrics.StorageStats _value) {
    return new KeystoreAtomPayload(storageStats, _value);
  }

  public android.security.metrics.StorageStats getStorageStats() {
    _assertTag(storageStats);
    return (android.security.metrics.StorageStats) _value;
  }

  public void setStorageStats(android.security.metrics.StorageStats _value) {
    _set(storageStats, _value);
  }

  // android.security.metrics.KeyCreationWithGeneralInfo keyCreationWithGeneralInfo;

  public static KeystoreAtomPayload keyCreationWithGeneralInfo(android.security.metrics.KeyCreationWithGeneralInfo _value) {
    return new KeystoreAtomPayload(keyCreationWithGeneralInfo, _value);
  }

  public android.security.metrics.KeyCreationWithGeneralInfo getKeyCreationWithGeneralInfo() {
    _assertTag(keyCreationWithGeneralInfo);
    return (android.security.metrics.KeyCreationWithGeneralInfo) _value;
  }

  public void setKeyCreationWithGeneralInfo(android.security.metrics.KeyCreationWithGeneralInfo _value) {
    _set(keyCreationWithGeneralInfo, _value);
  }

  // android.security.metrics.KeyCreationWithAuthInfo keyCreationWithAuthInfo;

  public static KeystoreAtomPayload keyCreationWithAuthInfo(android.security.metrics.KeyCreationWithAuthInfo _value) {
    return new KeystoreAtomPayload(keyCreationWithAuthInfo, _value);
  }

  public android.security.metrics.KeyCreationWithAuthInfo getKeyCreationWithAuthInfo() {
    _assertTag(keyCreationWithAuthInfo);
    return (android.security.metrics.KeyCreationWithAuthInfo) _value;
  }

  public void setKeyCreationWithAuthInfo(android.security.metrics.KeyCreationWithAuthInfo _value) {
    _set(keyCreationWithAuthInfo, _value);
  }

  // android.security.metrics.KeyCreationWithPurposeAndModesInfo keyCreationWithPurposeAndModesInfo;

  public static KeystoreAtomPayload keyCreationWithPurposeAndModesInfo(android.security.metrics.KeyCreationWithPurposeAndModesInfo _value) {
    return new KeystoreAtomPayload(keyCreationWithPurposeAndModesInfo, _value);
  }

  public android.security.metrics.KeyCreationWithPurposeAndModesInfo getKeyCreationWithPurposeAndModesInfo() {
    _assertTag(keyCreationWithPurposeAndModesInfo);
    return (android.security.metrics.KeyCreationWithPurposeAndModesInfo) _value;
  }

  public void setKeyCreationWithPurposeAndModesInfo(android.security.metrics.KeyCreationWithPurposeAndModesInfo _value) {
    _set(keyCreationWithPurposeAndModesInfo, _value);
  }

  // android.security.metrics.Keystore2AtomWithOverflow keystore2AtomWithOverflow;

  public static KeystoreAtomPayload keystore2AtomWithOverflow(android.security.metrics.Keystore2AtomWithOverflow _value) {
    return new KeystoreAtomPayload(keystore2AtomWithOverflow, _value);
  }

  public android.security.metrics.Keystore2AtomWithOverflow getKeystore2AtomWithOverflow() {
    _assertTag(keystore2AtomWithOverflow);
    return (android.security.metrics.Keystore2AtomWithOverflow) _value;
  }

  public void setKeystore2AtomWithOverflow(android.security.metrics.Keystore2AtomWithOverflow _value) {
    _set(keystore2AtomWithOverflow, _value);
  }

  // android.security.metrics.KeyOperationWithPurposeAndModesInfo keyOperationWithPurposeAndModesInfo;

  public static KeystoreAtomPayload keyOperationWithPurposeAndModesInfo(android.security.metrics.KeyOperationWithPurposeAndModesInfo _value) {
    return new KeystoreAtomPayload(keyOperationWithPurposeAndModesInfo, _value);
  }

  public android.security.metrics.KeyOperationWithPurposeAndModesInfo getKeyOperationWithPurposeAndModesInfo() {
    _assertTag(keyOperationWithPurposeAndModesInfo);
    return (android.security.metrics.KeyOperationWithPurposeAndModesInfo) _value;
  }

  public void setKeyOperationWithPurposeAndModesInfo(android.security.metrics.KeyOperationWithPurposeAndModesInfo _value) {
    _set(keyOperationWithPurposeAndModesInfo, _value);
  }

  // android.security.metrics.KeyOperationWithGeneralInfo keyOperationWithGeneralInfo;

  public static KeystoreAtomPayload keyOperationWithGeneralInfo(android.security.metrics.KeyOperationWithGeneralInfo _value) {
    return new KeystoreAtomPayload(keyOperationWithGeneralInfo, _value);
  }

  public android.security.metrics.KeyOperationWithGeneralInfo getKeyOperationWithGeneralInfo() {
    _assertTag(keyOperationWithGeneralInfo);
    return (android.security.metrics.KeyOperationWithGeneralInfo) _value;
  }

  public void setKeyOperationWithGeneralInfo(android.security.metrics.KeyOperationWithGeneralInfo _value) {
    _set(keyOperationWithGeneralInfo, _value);
  }

  // android.security.metrics.RkpErrorStats rkpErrorStats;

  public static KeystoreAtomPayload rkpErrorStats(android.security.metrics.RkpErrorStats _value) {
    return new KeystoreAtomPayload(rkpErrorStats, _value);
  }

  public android.security.metrics.RkpErrorStats getRkpErrorStats() {
    _assertTag(rkpErrorStats);
    return (android.security.metrics.RkpErrorStats) _value;
  }

  public void setRkpErrorStats(android.security.metrics.RkpErrorStats _value) {
    _set(rkpErrorStats, _value);
  }

  // android.security.metrics.CrashStats crashStats;

  public static KeystoreAtomPayload crashStats(android.security.metrics.CrashStats _value) {
    return new KeystoreAtomPayload(crashStats, _value);
  }

  public android.security.metrics.CrashStats getCrashStats() {
    _assertTag(crashStats);
    return (android.security.metrics.CrashStats) _value;
  }

  public void setCrashStats(android.security.metrics.CrashStats _value) {
    _set(crashStats, _value);
  }

  public static final android.os.Parcelable.Creator<KeystoreAtomPayload> CREATOR = new android.os.Parcelable.Creator<KeystoreAtomPayload>() {
    @Override
    public KeystoreAtomPayload createFromParcel(android.os.Parcel _aidl_source) {
      return new KeystoreAtomPayload(_aidl_source);
    }
    @Override
    public KeystoreAtomPayload[] newArray(int _aidl_size) {
      return new KeystoreAtomPayload[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case storageStats:
      _aidl_parcel.writeTypedObject(getStorageStats(), _aidl_flag);
      break;
    case keyCreationWithGeneralInfo:
      _aidl_parcel.writeTypedObject(getKeyCreationWithGeneralInfo(), _aidl_flag);
      break;
    case keyCreationWithAuthInfo:
      _aidl_parcel.writeTypedObject(getKeyCreationWithAuthInfo(), _aidl_flag);
      break;
    case keyCreationWithPurposeAndModesInfo:
      _aidl_parcel.writeTypedObject(getKeyCreationWithPurposeAndModesInfo(), _aidl_flag);
      break;
    case keystore2AtomWithOverflow:
      _aidl_parcel.writeTypedObject(getKeystore2AtomWithOverflow(), _aidl_flag);
      break;
    case keyOperationWithPurposeAndModesInfo:
      _aidl_parcel.writeTypedObject(getKeyOperationWithPurposeAndModesInfo(), _aidl_flag);
      break;
    case keyOperationWithGeneralInfo:
      _aidl_parcel.writeTypedObject(getKeyOperationWithGeneralInfo(), _aidl_flag);
      break;
    case rkpErrorStats:
      _aidl_parcel.writeTypedObject(getRkpErrorStats(), _aidl_flag);
      break;
    case crashStats:
      _aidl_parcel.writeTypedObject(getCrashStats(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case storageStats: {
      android.security.metrics.StorageStats _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.StorageStats.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case keyCreationWithGeneralInfo: {
      android.security.metrics.KeyCreationWithGeneralInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.KeyCreationWithGeneralInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case keyCreationWithAuthInfo: {
      android.security.metrics.KeyCreationWithAuthInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.KeyCreationWithAuthInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case keyCreationWithPurposeAndModesInfo: {
      android.security.metrics.KeyCreationWithPurposeAndModesInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.KeyCreationWithPurposeAndModesInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case keystore2AtomWithOverflow: {
      android.security.metrics.Keystore2AtomWithOverflow _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.Keystore2AtomWithOverflow.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case keyOperationWithPurposeAndModesInfo: {
      android.security.metrics.KeyOperationWithPurposeAndModesInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.KeyOperationWithPurposeAndModesInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case keyOperationWithGeneralInfo: {
      android.security.metrics.KeyOperationWithGeneralInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.KeyOperationWithGeneralInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case rkpErrorStats: {
      android.security.metrics.RkpErrorStats _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.RkpErrorStats.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case crashStats: {
      android.security.metrics.CrashStats _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.security.metrics.CrashStats.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case storageStats:
      _mask |= describeContents(getStorageStats());
      break;
    case keyCreationWithGeneralInfo:
      _mask |= describeContents(getKeyCreationWithGeneralInfo());
      break;
    case keyCreationWithAuthInfo:
      _mask |= describeContents(getKeyCreationWithAuthInfo());
      break;
    case keyCreationWithPurposeAndModesInfo:
      _mask |= describeContents(getKeyCreationWithPurposeAndModesInfo());
      break;
    case keystore2AtomWithOverflow:
      _mask |= describeContents(getKeystore2AtomWithOverflow());
      break;
    case keyOperationWithPurposeAndModesInfo:
      _mask |= describeContents(getKeyOperationWithPurposeAndModesInfo());
      break;
    case keyOperationWithGeneralInfo:
      _mask |= describeContents(getKeyOperationWithGeneralInfo());
      break;
    case rkpErrorStats:
      _mask |= describeContents(getRkpErrorStats());
      break;
    case crashStats:
      _mask |= describeContents(getCrashStats());
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
    case storageStats: return "storageStats";
    case keyCreationWithGeneralInfo: return "keyCreationWithGeneralInfo";
    case keyCreationWithAuthInfo: return "keyCreationWithAuthInfo";
    case keyCreationWithPurposeAndModesInfo: return "keyCreationWithPurposeAndModesInfo";
    case keystore2AtomWithOverflow: return "keystore2AtomWithOverflow";
    case keyOperationWithPurposeAndModesInfo: return "keyOperationWithPurposeAndModesInfo";
    case keyOperationWithGeneralInfo: return "keyOperationWithGeneralInfo";
    case rkpErrorStats: return "rkpErrorStats";
    case crashStats: return "crashStats";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int storageStats = 0;
    public static final int keyCreationWithGeneralInfo = 1;
    public static final int keyCreationWithAuthInfo = 2;
    public static final int keyCreationWithPurposeAndModesInfo = 3;
    public static final int keystore2AtomWithOverflow = 4;
    public static final int keyOperationWithPurposeAndModesInfo = 5;
    public static final int keyOperationWithGeneralInfo = 6;
    public static final int rkpErrorStats = 7;
    public static final int crashStats = 8;
  }
}
