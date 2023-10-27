/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.cas;
/** @hide */
public final class DestinationBuffer implements android.os.Parcelable {
  // tags for union fields
  public final static int nonsecureMemory = 0;  // android.hardware.cas.SharedBuffer nonsecureMemory;
  public final static int secureMemory = 1;  // android.hardware.common.NativeHandle secureMemory;

  private int _tag;
  private Object _value;

  public DestinationBuffer() {
    android.hardware.cas.SharedBuffer _value = null;
    this._tag = nonsecureMemory;
    this._value = _value;
  }

  private DestinationBuffer(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DestinationBuffer(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.cas.SharedBuffer nonsecureMemory;

  public static DestinationBuffer nonsecureMemory(android.hardware.cas.SharedBuffer _value) {
    return new DestinationBuffer(nonsecureMemory, _value);
  }

  public android.hardware.cas.SharedBuffer getNonsecureMemory() {
    _assertTag(nonsecureMemory);
    return (android.hardware.cas.SharedBuffer) _value;
  }

  public void setNonsecureMemory(android.hardware.cas.SharedBuffer _value) {
    _set(nonsecureMemory, _value);
  }

  // android.hardware.common.NativeHandle secureMemory;

  public static DestinationBuffer secureMemory(android.hardware.common.NativeHandle _value) {
    return new DestinationBuffer(secureMemory, _value);
  }

  public android.hardware.common.NativeHandle getSecureMemory() {
    _assertTag(secureMemory);
    return (android.hardware.common.NativeHandle) _value;
  }

  public void setSecureMemory(android.hardware.common.NativeHandle _value) {
    _set(secureMemory, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DestinationBuffer> CREATOR = new android.os.Parcelable.Creator<DestinationBuffer>() {
    @Override
    public DestinationBuffer createFromParcel(android.os.Parcel _aidl_source) {
      return new DestinationBuffer(_aidl_source);
    }
    @Override
    public DestinationBuffer[] newArray(int _aidl_size) {
      return new DestinationBuffer[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case nonsecureMemory:
      _aidl_parcel.writeTypedObject(getNonsecureMemory(), _aidl_flag);
      break;
    case secureMemory:
      _aidl_parcel.writeTypedObject(getSecureMemory(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case nonsecureMemory: {
      android.hardware.cas.SharedBuffer _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.cas.SharedBuffer.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case secureMemory: {
      android.hardware.common.NativeHandle _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.common.NativeHandle.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case nonsecureMemory:
      _mask |= describeContents(getNonsecureMemory());
      break;
    case secureMemory:
      _mask |= describeContents(getSecureMemory());
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
    case nonsecureMemory: return "nonsecureMemory";
    case secureMemory: return "secureMemory";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int nonsecureMemory = 0;
    public static final int secureMemory = 1;
  }
}
