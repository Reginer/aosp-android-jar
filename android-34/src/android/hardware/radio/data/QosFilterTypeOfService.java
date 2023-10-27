/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public final class QosFilterTypeOfService implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int value = 1;  // byte value;

  private int _tag;
  private Object _value;

  public QosFilterTypeOfService() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private QosFilterTypeOfService(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private QosFilterTypeOfService(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static QosFilterTypeOfService noinit(boolean _value) {
    return new QosFilterTypeOfService(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // byte value;

  public static QosFilterTypeOfService value(byte _value) {
    return new QosFilterTypeOfService(value, _value);
  }

  public byte getValue() {
    _assertTag(value);
    return (byte) _value;
  }

  public void setValue(byte _value) {
    _set(value, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<QosFilterTypeOfService> CREATOR = new android.os.Parcelable.Creator<QosFilterTypeOfService>() {
    @Override
    public QosFilterTypeOfService createFromParcel(android.os.Parcel _aidl_source) {
      return new QosFilterTypeOfService(_aidl_source);
    }
    @Override
    public QosFilterTypeOfService[] newArray(int _aidl_size) {
      return new QosFilterTypeOfService[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case noinit:
      _aidl_parcel.writeBoolean(getNoinit());
      break;
    case value:
      _aidl_parcel.writeByte(getValue());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case noinit: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case value: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
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

  @Override
  public String toString() {
    switch (_tag) {
    case noinit: return "android.hardware.radio.data.QosFilterTypeOfService.noinit(" + (getNoinit()) + ")";
    case value: return "android.hardware.radio.data.QosFilterTypeOfService.value(" + (getValue()) + ")";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }
  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case noinit: return "noinit";
    case value: return "value";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int noinit = 0;
    public static final int value = 1;
  }
}
