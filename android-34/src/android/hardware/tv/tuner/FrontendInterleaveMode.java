/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendInterleaveMode implements android.os.Parcelable {
  // tags for union fields
  public final static int atsc3 = 0;  // android.hardware.tv.tuner.FrontendAtsc3TimeInterleaveMode atsc3;
  public final static int dvbc = 1;  // android.hardware.tv.tuner.FrontendCableTimeInterleaveMode dvbc;
  public final static int dtmb = 2;  // android.hardware.tv.tuner.FrontendDtmbTimeInterleaveMode dtmb;
  public final static int isdbt = 3;  // android.hardware.tv.tuner.FrontendIsdbtTimeInterleaveMode isdbt;

  private int _tag;
  private Object _value;

  public FrontendInterleaveMode() {
    int _value = android.hardware.tv.tuner.FrontendAtsc3TimeInterleaveMode.UNDEFINED;
    this._tag = atsc3;
    this._value = _value;
  }

  private FrontendInterleaveMode(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendInterleaveMode(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendAtsc3TimeInterleaveMode atsc3;

  public static FrontendInterleaveMode atsc3(int _value) {
    return new FrontendInterleaveMode(atsc3, _value);
  }

  public int getAtsc3() {
    _assertTag(atsc3);
    return (int) _value;
  }

  public void setAtsc3(int _value) {
    _set(atsc3, _value);
  }

  // android.hardware.tv.tuner.FrontendCableTimeInterleaveMode dvbc;

  public static FrontendInterleaveMode dvbc(int _value) {
    return new FrontendInterleaveMode(dvbc, _value);
  }

  public int getDvbc() {
    _assertTag(dvbc);
    return (int) _value;
  }

  public void setDvbc(int _value) {
    _set(dvbc, _value);
  }

  // android.hardware.tv.tuner.FrontendDtmbTimeInterleaveMode dtmb;

  public static FrontendInterleaveMode dtmb(int _value) {
    return new FrontendInterleaveMode(dtmb, _value);
  }

  public int getDtmb() {
    _assertTag(dtmb);
    return (int) _value;
  }

  public void setDtmb(int _value) {
    _set(dtmb, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtTimeInterleaveMode isdbt;

  public static FrontendInterleaveMode isdbt(int _value) {
    return new FrontendInterleaveMode(isdbt, _value);
  }

  public int getIsdbt() {
    _assertTag(isdbt);
    return (int) _value;
  }

  public void setIsdbt(int _value) {
    _set(isdbt, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendInterleaveMode> CREATOR = new android.os.Parcelable.Creator<FrontendInterleaveMode>() {
    @Override
    public FrontendInterleaveMode createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendInterleaveMode(_aidl_source);
    }
    @Override
    public FrontendInterleaveMode[] newArray(int _aidl_size) {
      return new FrontendInterleaveMode[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case atsc3:
      _aidl_parcel.writeInt(getAtsc3());
      break;
    case dvbc:
      _aidl_parcel.writeInt(getDvbc());
      break;
    case dtmb:
      _aidl_parcel.writeInt(getDtmb());
      break;
    case isdbt:
      _aidl_parcel.writeInt(getIsdbt());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case atsc3: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbc: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dtmb: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbt: {
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
    case atsc3: return "atsc3";
    case dvbc: return "dvbc";
    case dtmb: return "dtmb";
    case isdbt: return "isdbt";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int atsc3 = 0;
    public static final int dvbc = 1;
    public static final int dtmb = 2;
    public static final int isdbt = 3;
  }
}
