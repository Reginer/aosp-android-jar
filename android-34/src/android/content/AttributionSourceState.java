/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.content;
/**
 * Payload for the {@link AttributionSource} class needed to interoperate
 * with different languages.
 * 
 * {@hide}
 */
public class AttributionSourceState implements android.os.Parcelable
{
  /** The PID that is accessing the permission protected data. */
  public int pid = -1;
  /** The UID that is accessing the permission protected data. */
  public int uid = -1;
  /** The package that is accessing the permission protected data. */
  public java.lang.String packageName;
  /** The attribution tag of the app accessing the permission protected data. */
  public java.lang.String attributionTag;
  /** Unique token for that source. */
  public android.os.IBinder token;
  /** Permissions that should be considered revoked regardless if granted. */
  public java.lang.String[] renouncedPermissions;
  /** The next app to receive the permission protected data. */
  // TODO: We use an array as a workaround - the C++ backend doesn't
  // support referring to the parcelable as it expects ctor/dtor
  public android.content.AttributionSourceState[] next;
  public static final android.os.Parcelable.Creator<AttributionSourceState> CREATOR = new android.os.Parcelable.Creator<AttributionSourceState>() {
    @Override
    public AttributionSourceState createFromParcel(android.os.Parcel _aidl_source) {
      AttributionSourceState _aidl_out = new AttributionSourceState();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AttributionSourceState[] newArray(int _aidl_size) {
      return new AttributionSourceState[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(pid);
    _aidl_parcel.writeInt(uid);
    _aidl_parcel.writeString(packageName);
    _aidl_parcel.writeString(attributionTag);
    _aidl_parcel.writeStrongBinder(token);
    _aidl_parcel.writeStringArray(renouncedPermissions);
    _aidl_parcel.writeTypedArray(next, _aidl_flag);
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
      pid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      packageName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      attributionTag = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      token = _aidl_parcel.readStrongBinder();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      renouncedPermissions = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      next = _aidl_parcel.createTypedArray(android.content.AttributionSourceState.CREATOR);
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
    _mask |= describeContents(next);
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
