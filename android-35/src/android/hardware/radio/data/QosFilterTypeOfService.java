/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash cd8913a3f9d39f1cc0a5fcf9e90257be94ec38df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V3-java-source/gen/android/hardware/radio/data/QosFilterTypeOfService.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/3/android/hardware/radio/data/QosFilterTypeOfService.aidl
 */
package android.hardware.radio.data;
/** @hide */
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
    case noinit: return "QosFilterTypeOfService.noinit(" + (getNoinit()) + ")";
    case value: return "QosFilterTypeOfService.value(" + (getValue()) + ")";
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
