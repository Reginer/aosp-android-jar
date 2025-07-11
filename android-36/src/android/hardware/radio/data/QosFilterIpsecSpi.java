/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 70713939dbe39fdbd3a294b3a3e3d2842b3bf4eb --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V4-java-source/gen/android/hardware/radio/data/QosFilterIpsecSpi.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V4-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/4 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/4/android/hardware/radio/data/QosFilterIpsecSpi.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.radio.data;
/** @hide */
public final class QosFilterIpsecSpi implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int value = 1;  // int value;

  private int _tag;
  private Object _value;

  public QosFilterIpsecSpi() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private QosFilterIpsecSpi(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private QosFilterIpsecSpi(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static QosFilterIpsecSpi noinit(boolean _value) {
    return new QosFilterIpsecSpi(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // int value;

  public static QosFilterIpsecSpi value(int _value) {
    return new QosFilterIpsecSpi(value, _value);
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

  public static final android.os.Parcelable.Creator<QosFilterIpsecSpi> CREATOR = new android.os.Parcelable.Creator<QosFilterIpsecSpi>() {
    @Override
    public QosFilterIpsecSpi createFromParcel(android.os.Parcel _aidl_source) {
      return new QosFilterIpsecSpi(_aidl_source);
    }
    @Override
    public QosFilterIpsecSpi[] newArray(int _aidl_size) {
      return new QosFilterIpsecSpi[_aidl_size];
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
    case noinit: return "QosFilterIpsecSpi.noinit(" + (getNoinit()) + ")";
    case value: return "QosFilterIpsecSpi.value(" + (getValue()) + ")";
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
