/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxIpAddressIpAddress implements android.os.Parcelable {
  // tags for union fields
  public final static int v4 = 0;  // byte[] v4;
  public final static int v6 = 1;  // byte[] v6;

  private int _tag;
  private Object _value;

  public DemuxIpAddressIpAddress() {
    byte[] _value = {};
    this._tag = v4;
    this._value = _value;
  }

  private DemuxIpAddressIpAddress(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxIpAddressIpAddress(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // byte[] v4;

  public static DemuxIpAddressIpAddress v4(byte[] _value) {
    return new DemuxIpAddressIpAddress(v4, _value);
  }

  public byte[] getV4() {
    _assertTag(v4);
    return (byte[]) _value;
  }

  public void setV4(byte[] _value) {
    _set(v4, _value);
  }

  // byte[] v6;

  public static DemuxIpAddressIpAddress v6(byte[] _value) {
    return new DemuxIpAddressIpAddress(v6, _value);
  }

  public byte[] getV6() {
    _assertTag(v6);
    return (byte[]) _value;
  }

  public void setV6(byte[] _value) {
    _set(v6, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxIpAddressIpAddress> CREATOR = new android.os.Parcelable.Creator<DemuxIpAddressIpAddress>() {
    @Override
    public DemuxIpAddressIpAddress createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxIpAddressIpAddress(_aidl_source);
    }
    @Override
    public DemuxIpAddressIpAddress[] newArray(int _aidl_size) {
      return new DemuxIpAddressIpAddress[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case v4:
      _aidl_parcel.writeByteArray(getV4());
      break;
    case v6:
      _aidl_parcel.writeByteArray(getV6());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case v4: {
      byte[] _aidl_value;
      _aidl_value = _aidl_parcel.createByteArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case v6: {
      byte[] _aidl_value;
      _aidl_value = _aidl_parcel.createByteArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    }
    return _mask;
  }

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case v4: return "v4";
    case v6: return "v6";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int v4 = 0;
    public static final int v6 = 1;
  }
}
