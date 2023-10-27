/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendGuardInterval implements android.os.Parcelable {
  // tags for union fields
  public final static int dvbt = 0;  // android.hardware.tv.tuner.FrontendDvbtGuardInterval dvbt;
  public final static int isdbt = 1;  // android.hardware.tv.tuner.FrontendIsdbtGuardInterval isdbt;
  public final static int dtmb = 2;  // android.hardware.tv.tuner.FrontendDtmbGuardInterval dtmb;

  private int _tag;
  private Object _value;

  public FrontendGuardInterval() {
    int _value = android.hardware.tv.tuner.FrontendDvbtGuardInterval.UNDEFINED;
    this._tag = dvbt;
    this._value = _value;
  }

  private FrontendGuardInterval(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendGuardInterval(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendDvbtGuardInterval dvbt;

  public static FrontendGuardInterval dvbt(int _value) {
    return new FrontendGuardInterval(dvbt, _value);
  }

  public int getDvbt() {
    _assertTag(dvbt);
    return (int) _value;
  }

  public void setDvbt(int _value) {
    _set(dvbt, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtGuardInterval isdbt;

  public static FrontendGuardInterval isdbt(int _value) {
    return new FrontendGuardInterval(isdbt, _value);
  }

  public int getIsdbt() {
    _assertTag(isdbt);
    return (int) _value;
  }

  public void setIsdbt(int _value) {
    _set(isdbt, _value);
  }

  // android.hardware.tv.tuner.FrontendDtmbGuardInterval dtmb;

  public static FrontendGuardInterval dtmb(int _value) {
    return new FrontendGuardInterval(dtmb, _value);
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

  public static final android.os.Parcelable.Creator<FrontendGuardInterval> CREATOR = new android.os.Parcelable.Creator<FrontendGuardInterval>() {
    @Override
    public FrontendGuardInterval createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendGuardInterval(_aidl_source);
    }
    @Override
    public FrontendGuardInterval[] newArray(int _aidl_size) {
      return new FrontendGuardInterval[_aidl_size];
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
