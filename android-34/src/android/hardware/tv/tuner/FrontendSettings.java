/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendSettings implements android.os.Parcelable {
  // tags for union fields
  public final static int analog = 0;  // android.hardware.tv.tuner.FrontendAnalogSettings analog;
  public final static int atsc = 1;  // android.hardware.tv.tuner.FrontendAtscSettings atsc;
  public final static int atsc3 = 2;  // android.hardware.tv.tuner.FrontendAtsc3Settings atsc3;
  public final static int dvbs = 3;  // android.hardware.tv.tuner.FrontendDvbsSettings dvbs;
  public final static int dvbc = 4;  // android.hardware.tv.tuner.FrontendDvbcSettings dvbc;
  public final static int dvbt = 5;  // android.hardware.tv.tuner.FrontendDvbtSettings dvbt;
  public final static int isdbs = 6;  // android.hardware.tv.tuner.FrontendIsdbsSettings isdbs;
  public final static int isdbs3 = 7;  // android.hardware.tv.tuner.FrontendIsdbs3Settings isdbs3;
  public final static int isdbt = 8;  // android.hardware.tv.tuner.FrontendIsdbtSettings isdbt;
  public final static int dtmb = 9;  // android.hardware.tv.tuner.FrontendDtmbSettings dtmb;
  public final static int iptv = 10;  // android.hardware.tv.tuner.FrontendIptvSettings iptv;

  private int _tag;
  private Object _value;

  public FrontendSettings() {
    android.hardware.tv.tuner.FrontendAnalogSettings _value = null;
    this._tag = analog;
    this._value = _value;
  }

  private FrontendSettings(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendSettings(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendAnalogSettings analog;

  public static FrontendSettings analog(android.hardware.tv.tuner.FrontendAnalogSettings _value) {
    return new FrontendSettings(analog, _value);
  }

  public android.hardware.tv.tuner.FrontendAnalogSettings getAnalog() {
    _assertTag(analog);
    return (android.hardware.tv.tuner.FrontendAnalogSettings) _value;
  }

  public void setAnalog(android.hardware.tv.tuner.FrontendAnalogSettings _value) {
    _set(analog, _value);
  }

  // android.hardware.tv.tuner.FrontendAtscSettings atsc;

  public static FrontendSettings atsc(android.hardware.tv.tuner.FrontendAtscSettings _value) {
    return new FrontendSettings(atsc, _value);
  }

  public android.hardware.tv.tuner.FrontendAtscSettings getAtsc() {
    _assertTag(atsc);
    return (android.hardware.tv.tuner.FrontendAtscSettings) _value;
  }

  public void setAtsc(android.hardware.tv.tuner.FrontendAtscSettings _value) {
    _set(atsc, _value);
  }

  // android.hardware.tv.tuner.FrontendAtsc3Settings atsc3;

  public static FrontendSettings atsc3(android.hardware.tv.tuner.FrontendAtsc3Settings _value) {
    return new FrontendSettings(atsc3, _value);
  }

  public android.hardware.tv.tuner.FrontendAtsc3Settings getAtsc3() {
    _assertTag(atsc3);
    return (android.hardware.tv.tuner.FrontendAtsc3Settings) _value;
  }

  public void setAtsc3(android.hardware.tv.tuner.FrontendAtsc3Settings _value) {
    _set(atsc3, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbsSettings dvbs;

  public static FrontendSettings dvbs(android.hardware.tv.tuner.FrontendDvbsSettings _value) {
    return new FrontendSettings(dvbs, _value);
  }

  public android.hardware.tv.tuner.FrontendDvbsSettings getDvbs() {
    _assertTag(dvbs);
    return (android.hardware.tv.tuner.FrontendDvbsSettings) _value;
  }

  public void setDvbs(android.hardware.tv.tuner.FrontendDvbsSettings _value) {
    _set(dvbs, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbcSettings dvbc;

  public static FrontendSettings dvbc(android.hardware.tv.tuner.FrontendDvbcSettings _value) {
    return new FrontendSettings(dvbc, _value);
  }

  public android.hardware.tv.tuner.FrontendDvbcSettings getDvbc() {
    _assertTag(dvbc);
    return (android.hardware.tv.tuner.FrontendDvbcSettings) _value;
  }

  public void setDvbc(android.hardware.tv.tuner.FrontendDvbcSettings _value) {
    _set(dvbc, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbtSettings dvbt;

  public static FrontendSettings dvbt(android.hardware.tv.tuner.FrontendDvbtSettings _value) {
    return new FrontendSettings(dvbt, _value);
  }

  public android.hardware.tv.tuner.FrontendDvbtSettings getDvbt() {
    _assertTag(dvbt);
    return (android.hardware.tv.tuner.FrontendDvbtSettings) _value;
  }

  public void setDvbt(android.hardware.tv.tuner.FrontendDvbtSettings _value) {
    _set(dvbt, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbsSettings isdbs;

  public static FrontendSettings isdbs(android.hardware.tv.tuner.FrontendIsdbsSettings _value) {
    return new FrontendSettings(isdbs, _value);
  }

  public android.hardware.tv.tuner.FrontendIsdbsSettings getIsdbs() {
    _assertTag(isdbs);
    return (android.hardware.tv.tuner.FrontendIsdbsSettings) _value;
  }

  public void setIsdbs(android.hardware.tv.tuner.FrontendIsdbsSettings _value) {
    _set(isdbs, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbs3Settings isdbs3;

  public static FrontendSettings isdbs3(android.hardware.tv.tuner.FrontendIsdbs3Settings _value) {
    return new FrontendSettings(isdbs3, _value);
  }

  public android.hardware.tv.tuner.FrontendIsdbs3Settings getIsdbs3() {
    _assertTag(isdbs3);
    return (android.hardware.tv.tuner.FrontendIsdbs3Settings) _value;
  }

  public void setIsdbs3(android.hardware.tv.tuner.FrontendIsdbs3Settings _value) {
    _set(isdbs3, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtSettings isdbt;

  public static FrontendSettings isdbt(android.hardware.tv.tuner.FrontendIsdbtSettings _value) {
    return new FrontendSettings(isdbt, _value);
  }

  public android.hardware.tv.tuner.FrontendIsdbtSettings getIsdbt() {
    _assertTag(isdbt);
    return (android.hardware.tv.tuner.FrontendIsdbtSettings) _value;
  }

  public void setIsdbt(android.hardware.tv.tuner.FrontendIsdbtSettings _value) {
    _set(isdbt, _value);
  }

  // android.hardware.tv.tuner.FrontendDtmbSettings dtmb;

  public static FrontendSettings dtmb(android.hardware.tv.tuner.FrontendDtmbSettings _value) {
    return new FrontendSettings(dtmb, _value);
  }

  public android.hardware.tv.tuner.FrontendDtmbSettings getDtmb() {
    _assertTag(dtmb);
    return (android.hardware.tv.tuner.FrontendDtmbSettings) _value;
  }

  public void setDtmb(android.hardware.tv.tuner.FrontendDtmbSettings _value) {
    _set(dtmb, _value);
  }

  // android.hardware.tv.tuner.FrontendIptvSettings iptv;

  public static FrontendSettings iptv(android.hardware.tv.tuner.FrontendIptvSettings _value) {
    return new FrontendSettings(iptv, _value);
  }

  public android.hardware.tv.tuner.FrontendIptvSettings getIptv() {
    _assertTag(iptv);
    return (android.hardware.tv.tuner.FrontendIptvSettings) _value;
  }

  public void setIptv(android.hardware.tv.tuner.FrontendIptvSettings _value) {
    _set(iptv, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendSettings> CREATOR = new android.os.Parcelable.Creator<FrontendSettings>() {
    @Override
    public FrontendSettings createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendSettings(_aidl_source);
    }
    @Override
    public FrontendSettings[] newArray(int _aidl_size) {
      return new FrontendSettings[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case analog:
      _aidl_parcel.writeTypedObject(getAnalog(), _aidl_flag);
      break;
    case atsc:
      _aidl_parcel.writeTypedObject(getAtsc(), _aidl_flag);
      break;
    case atsc3:
      _aidl_parcel.writeTypedObject(getAtsc3(), _aidl_flag);
      break;
    case dvbs:
      _aidl_parcel.writeTypedObject(getDvbs(), _aidl_flag);
      break;
    case dvbc:
      _aidl_parcel.writeTypedObject(getDvbc(), _aidl_flag);
      break;
    case dvbt:
      _aidl_parcel.writeTypedObject(getDvbt(), _aidl_flag);
      break;
    case isdbs:
      _aidl_parcel.writeTypedObject(getIsdbs(), _aidl_flag);
      break;
    case isdbs3:
      _aidl_parcel.writeTypedObject(getIsdbs3(), _aidl_flag);
      break;
    case isdbt:
      _aidl_parcel.writeTypedObject(getIsdbt(), _aidl_flag);
      break;
    case dtmb:
      _aidl_parcel.writeTypedObject(getDtmb(), _aidl_flag);
      break;
    case iptv:
      _aidl_parcel.writeTypedObject(getIptv(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case analog: {
      android.hardware.tv.tuner.FrontendAnalogSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendAnalogSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case atsc: {
      android.hardware.tv.tuner.FrontendAtscSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendAtscSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case atsc3: {
      android.hardware.tv.tuner.FrontendAtsc3Settings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendAtsc3Settings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbs: {
      android.hardware.tv.tuner.FrontendDvbsSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDvbsSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbc: {
      android.hardware.tv.tuner.FrontendDvbcSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDvbcSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbt: {
      android.hardware.tv.tuner.FrontendDvbtSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDvbtSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbs: {
      android.hardware.tv.tuner.FrontendIsdbsSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIsdbsSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbs3: {
      android.hardware.tv.tuner.FrontendIsdbs3Settings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIsdbs3Settings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbt: {
      android.hardware.tv.tuner.FrontendIsdbtSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIsdbtSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dtmb: {
      android.hardware.tv.tuner.FrontendDtmbSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDtmbSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case iptv: {
      android.hardware.tv.tuner.FrontendIptvSettings _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIptvSettings.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case analog:
      _mask |= describeContents(getAnalog());
      break;
    case atsc:
      _mask |= describeContents(getAtsc());
      break;
    case atsc3:
      _mask |= describeContents(getAtsc3());
      break;
    case dvbs:
      _mask |= describeContents(getDvbs());
      break;
    case dvbc:
      _mask |= describeContents(getDvbc());
      break;
    case dvbt:
      _mask |= describeContents(getDvbt());
      break;
    case isdbs:
      _mask |= describeContents(getIsdbs());
      break;
    case isdbs3:
      _mask |= describeContents(getIsdbs3());
      break;
    case isdbt:
      _mask |= describeContents(getIsdbt());
      break;
    case dtmb:
      _mask |= describeContents(getDtmb());
      break;
    case iptv:
      _mask |= describeContents(getIptv());
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
    case analog: return "analog";
    case atsc: return "atsc";
    case atsc3: return "atsc3";
    case dvbs: return "dvbs";
    case dvbc: return "dvbc";
    case dvbt: return "dvbt";
    case isdbs: return "isdbs";
    case isdbs3: return "isdbs3";
    case isdbt: return "isdbt";
    case dtmb: return "dtmb";
    case iptv: return "iptv";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int analog = 0;
    public static final int atsc = 1;
    public static final int atsc3 = 2;
    public static final int dvbs = 3;
    public static final int dvbc = 4;
    public static final int dvbt = 5;
    public static final int isdbs = 6;
    public static final int isdbs3 = 7;
    public static final int isdbt = 8;
    public static final int dtmb = 9;
    public static final int iptv = 10;
  }
}
