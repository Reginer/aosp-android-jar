/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public final class QosFilterIpv6FlowLabel implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int value = 1;  // int value;

  private int _tag;
  private Object _value;

  public QosFilterIpv6FlowLabel() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private QosFilterIpv6FlowLabel(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private QosFilterIpv6FlowLabel(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static QosFilterIpv6FlowLabel noinit(boolean _value) {
    return new QosFilterIpv6FlowLabel(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // int value;

  public static QosFilterIpv6FlowLabel value(int _value) {
    return new QosFilterIpv6FlowLabel(value, _value);
  }

  public int getValue() {
    _assertTag(value);
    return (int) _value;
  }

  public void setValue(int _value) {
    _set(value, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<QosFilterIpv6FlowLabel> CREATOR = new android.os.Parcelable.Creator<QosFilterIpv6FlowLabel>() {
    @Override
    public QosFilterIpv6FlowLabel createFromParcel(android.os.Parcel _aidl_source) {
      return new QosFilterIpv6FlowLabel(_aidl_source);
    }
    @Override
    public QosFilterIpv6FlowLabel[] newArray(int _aidl_size) {
      return new QosFilterIpv6FlowLabel[_aidl_size];
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
      _aidl_parcel.writeInt(getValue());
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
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
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
    case noinit: return "android.hardware.radio.data.QosFilterIpv6FlowLabel.noinit(" + (getNoinit()) + ")";
    case value: return "android.hardware.radio.data.QosFilterIpv6FlowLabel.value(" + (getValue()) + ")";
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
