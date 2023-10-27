/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendTransmissionMode implements android.os.Parcelable {
  // tags for union fields
  public final static int dvbt = 0;  // android.hardware.tv.tuner.FrontendDvbtTransmissionMode dvbt;
  public final static int isdbt = 1;  // android.hardware.tv.tuner.FrontendIsdbtMode isdbt;
  public final static int dtmb = 2;  // android.hardware.tv.tuner.FrontendDtmbTransmissionMode dtmb;

  private int _tag;
  private Object _value;

  public FrontendTransmissionMode() {
    int _value = android.hardware.tv.tuner.FrontendDvbtTransmissionMode.UNDEFINED;
    this._tag = dvbt;
    this._value = _value;
  }

  private FrontendTransmissionMode(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendTransmissionMode(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendDvbtTransmissionMode dvbt;

  public static FrontendTransmissionMode dvbt(int _value) {
    return new FrontendTransmissionMode(dvbt, _value);
  }

  public int getDvbt() {
    _assertTag(dvbt);
    return (int) _value;
  }

  public void setDvbt(int _value) {
    _set(dvbt, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtMode isdbt;

  public static FrontendTransmissionMode isdbt(int _value) {
    return new FrontendTransmissionMode(isdbt, _value);
  }

  public int getIsdbt() {
    _assertTag(isdbt);
    return (int) _value;
  }

  public void setIsdbt(int _value) {
    _set(isdbt, _value);
  }

  // android.hardware.tv.tuner.FrontendDtmbTransmissionMode dtmb;

  public static FrontendTransmissionMode dtmb(int _value) {
    return new FrontendTransmissionMode(dtmb, _value);
  }

  public int getDtmb() {
    _assertTag(dtmb);
    return (int) _value;
  }

  public void setDtmb(int _value) {
    _set(dtmb, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendTransmissionMode> CREATOR = new android.os.Parcelable.Creator<FrontendTransmissionMode>() {
    @Override
    public FrontendTransmissionMode createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendTransmissionMode(_aidl_source);
    }
    @Override
    public FrontendTransmissionMode[] newArray(int _aidl_size) {
      return new FrontendTransmissionMode[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case dvbt:
      _aidl_parcel.writeInt(getDvbt());
      break;
    case isdbt:
      _aidl_parcel.writeInt(getIsdbt());
      break;
    case dtmb:
      _aidl_parcel.writeInt(getDtmb());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case dvbt: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbt: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dtmb: {
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

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case dvbt: return "dvbt";
    case isdbt: return "isdbt";
    case dtmb: return "dtmb";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int dvbt = 0;
    public static final int isdbt = 1;
    public static final int dtmb = 2;
  }
}
