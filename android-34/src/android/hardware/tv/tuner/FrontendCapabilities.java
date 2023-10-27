/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendCapabilities implements android.os.Parcelable {
  // tags for union fields
  public final static int analogCaps = 0;  // android.hardware.tv.tuner.FrontendAnalogCapabilities analogCaps;
  public final static int atscCaps = 1;  // android.hardware.tv.tuner.FrontendAtscCapabilities atscCaps;
  public final static int atsc3Caps = 2;  // android.hardware.tv.tuner.FrontendAtsc3Capabilities atsc3Caps;
  public final static int dtmbCaps = 3;  // android.hardware.tv.tuner.FrontendDtmbCapabilities dtmbCaps;
  public final static int dvbsCaps = 4;  // android.hardware.tv.tuner.FrontendDvbsCapabilities dvbsCaps;
  public final static int dvbcCaps = 5;  // android.hardware.tv.tuner.FrontendDvbcCapabilities dvbcCaps;
  public final static int dvbtCaps = 6;  // android.hardware.tv.tuner.FrontendDvbtCapabilities dvbtCaps;
  public final static int isdbsCaps = 7;  // android.hardware.tv.tuner.FrontendIsdbsCapabilities isdbsCaps;
  public final static int isdbs3Caps = 8;  // android.hardware.tv.tuner.FrontendIsdbs3Capabilities isdbs3Caps;
  public final static int isdbtCaps = 9;  // android.hardware.tv.tuner.FrontendIsdbtCapabilities isdbtCaps;
  public final static int iptvCaps = 10;  // android.hardware.tv.tuner.FrontendIptvCapabilities iptvCaps;

  private int _tag;
  private Object _value;

  public FrontendCapabilities() {
    android.hardware.tv.tuner.FrontendAnalogCapabilities _value = null;
    this._tag = analogCaps;
    this._value = _value;
  }

  private FrontendCapabilities(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendCapabilities(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendAnalogCapabilities analogCaps;

  public static FrontendCapabilities analogCaps(android.hardware.tv.tuner.FrontendAnalogCapabilities _value) {
    return new FrontendCapabilities(analogCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendAnalogCapabilities getAnalogCaps() {
    _assertTag(analogCaps);
    return (android.hardware.tv.tuner.FrontendAnalogCapabilities) _value;
  }

  public void setAnalogCaps(android.hardware.tv.tuner.FrontendAnalogCapabilities _value) {
    _set(analogCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendAtscCapabilities atscCaps;

  public static FrontendCapabilities atscCaps(android.hardware.tv.tuner.FrontendAtscCapabilities _value) {
    return new FrontendCapabilities(atscCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendAtscCapabilities getAtscCaps() {
    _assertTag(atscCaps);
    return (android.hardware.tv.tuner.FrontendAtscCapabilities) _value;
  }

  public void setAtscCaps(android.hardware.tv.tuner.FrontendAtscCapabilities _value) {
    _set(atscCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendAtsc3Capabilities atsc3Caps;

  public static FrontendCapabilities atsc3Caps(android.hardware.tv.tuner.FrontendAtsc3Capabilities _value) {
    return new FrontendCapabilities(atsc3Caps, _value);
  }

  public android.hardware.tv.tuner.FrontendAtsc3Capabilities getAtsc3Caps() {
    _assertTag(atsc3Caps);
    return (android.hardware.tv.tuner.FrontendAtsc3Capabilities) _value;
  }

  public void setAtsc3Caps(android.hardware.tv.tuner.FrontendAtsc3Capabilities _value) {
    _set(atsc3Caps, _value);
  }

  // android.hardware.tv.tuner.FrontendDtmbCapabilities dtmbCaps;

  public static FrontendCapabilities dtmbCaps(android.hardware.tv.tuner.FrontendDtmbCapabilities _value) {
    return new FrontendCapabilities(dtmbCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendDtmbCapabilities getDtmbCaps() {
    _assertTag(dtmbCaps);
    return (android.hardware.tv.tuner.FrontendDtmbCapabilities) _value;
  }

  public void setDtmbCaps(android.hardware.tv.tuner.FrontendDtmbCapabilities _value) {
    _set(dtmbCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbsCapabilities dvbsCaps;

  public static FrontendCapabilities dvbsCaps(android.hardware.tv.tuner.FrontendDvbsCapabilities _value) {
    return new FrontendCapabilities(dvbsCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendDvbsCapabilities getDvbsCaps() {
    _assertTag(dvbsCaps);
    return (android.hardware.tv.tuner.FrontendDvbsCapabilities) _value;
  }

  public void setDvbsCaps(android.hardware.tv.tuner.FrontendDvbsCapabilities _value) {
    _set(dvbsCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbcCapabilities dvbcCaps;

  public static FrontendCapabilities dvbcCaps(android.hardware.tv.tuner.FrontendDvbcCapabilities _value) {
    return new FrontendCapabilities(dvbcCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendDvbcCapabilities getDvbcCaps() {
    _assertTag(dvbcCaps);
    return (android.hardware.tv.tuner.FrontendDvbcCapabilities) _value;
  }

  public void setDvbcCaps(android.hardware.tv.tuner.FrontendDvbcCapabilities _value) {
    _set(dvbcCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbtCapabilities dvbtCaps;

  public static FrontendCapabilities dvbtCaps(android.hardware.tv.tuner.FrontendDvbtCapabilities _value) {
    return new FrontendCapabilities(dvbtCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendDvbtCapabilities getDvbtCaps() {
    _assertTag(dvbtCaps);
    return (android.hardware.tv.tuner.FrontendDvbtCapabilities) _value;
  }

  public void setDvbtCaps(android.hardware.tv.tuner.FrontendDvbtCapabilities _value) {
    _set(dvbtCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbsCapabilities isdbsCaps;

  public static FrontendCapabilities isdbsCaps(android.hardware.tv.tuner.FrontendIsdbsCapabilities _value) {
    return new FrontendCapabilities(isdbsCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendIsdbsCapabilities getIsdbsCaps() {
    _assertTag(isdbsCaps);
    return (android.hardware.tv.tuner.FrontendIsdbsCapabilities) _value;
  }

  public void setIsdbsCaps(android.hardware.tv.tuner.FrontendIsdbsCapabilities _value) {
    _set(isdbsCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbs3Capabilities isdbs3Caps;

  public static FrontendCapabilities isdbs3Caps(android.hardware.tv.tuner.FrontendIsdbs3Capabilities _value) {
    return new FrontendCapabilities(isdbs3Caps, _value);
  }

  public android.hardware.tv.tuner.FrontendIsdbs3Capabilities getIsdbs3Caps() {
    _assertTag(isdbs3Caps);
    return (android.hardware.tv.tuner.FrontendIsdbs3Capabilities) _value;
  }

  public void setIsdbs3Caps(android.hardware.tv.tuner.FrontendIsdbs3Capabilities _value) {
    _set(isdbs3Caps, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtCapabilities isdbtCaps;

  public static FrontendCapabilities isdbtCaps(android.hardware.tv.tuner.FrontendIsdbtCapabilities _value) {
    return new FrontendCapabilities(isdbtCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendIsdbtCapabilities getIsdbtCaps() {
    _assertTag(isdbtCaps);
    return (android.hardware.tv.tuner.FrontendIsdbtCapabilities) _value;
  }

  public void setIsdbtCaps(android.hardware.tv.tuner.FrontendIsdbtCapabilities _value) {
    _set(isdbtCaps, _value);
  }

  // android.hardware.tv.tuner.FrontendIptvCapabilities iptvCaps;

  public static FrontendCapabilities iptvCaps(android.hardware.tv.tuner.FrontendIptvCapabilities _value) {
    return new FrontendCapabilities(iptvCaps, _value);
  }

  public android.hardware.tv.tuner.FrontendIptvCapabilities getIptvCaps() {
    _assertTag(iptvCaps);
    return (android.hardware.tv.tuner.FrontendIptvCapabilities) _value;
  }

  public void setIptvCaps(android.hardware.tv.tuner.FrontendIptvCapabilities _value) {
    _set(iptvCaps, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendCapabilities> CREATOR = new android.os.Parcelable.Creator<FrontendCapabilities>() {
    @Override
    public FrontendCapabilities createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendCapabilities(_aidl_source);
    }
    @Override
    public FrontendCapabilities[] newArray(int _aidl_size) {
      return new FrontendCapabilities[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case analogCaps:
      _aidl_parcel.writeTypedObject(getAnalogCaps(), _aidl_flag);
      break;
    case atscCaps:
      _aidl_parcel.writeTypedObject(getAtscCaps(), _aidl_flag);
      break;
    case atsc3Caps:
      _aidl_parcel.writeTypedObject(getAtsc3Caps(), _aidl_flag);
      break;
    case dtmbCaps:
      _aidl_parcel.writeTypedObject(getDtmbCaps(), _aidl_flag);
      break;
    case dvbsCaps:
      _aidl_parcel.writeTypedObject(getDvbsCaps(), _aidl_flag);
      break;
    case dvbcCaps:
      _aidl_parcel.writeTypedObject(getDvbcCaps(), _aidl_flag);
      break;
    case dvbtCaps:
      _aidl_parcel.writeTypedObject(getDvbtCaps(), _aidl_flag);
      break;
    case isdbsCaps:
      _aidl_parcel.writeTypedObject(getIsdbsCaps(), _aidl_flag);
      break;
    case isdbs3Caps:
      _aidl_parcel.writeTypedObject(getIsdbs3Caps(), _aidl_flag);
      break;
    case isdbtCaps:
      _aidl_parcel.writeTypedObject(getIsdbtCaps(), _aidl_flag);
      break;
    case iptvCaps:
      _aidl_parcel.writeTypedObject(getIptvCaps(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case analogCaps: {
      android.hardware.tv.tuner.FrontendAnalogCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendAnalogCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case atscCaps: {
      android.hardware.tv.tuner.FrontendAtscCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendAtscCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case atsc3Caps: {
      android.hardware.tv.tuner.FrontendAtsc3Capabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendAtsc3Capabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dtmbCaps: {
      android.hardware.tv.tuner.FrontendDtmbCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDtmbCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbsCaps: {
      android.hardware.tv.tuner.FrontendDvbsCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDvbsCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbcCaps: {
      android.hardware.tv.tuner.FrontendDvbcCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDvbcCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbtCaps: {
      android.hardware.tv.tuner.FrontendDvbtCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDvbtCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbsCaps: {
      android.hardware.tv.tuner.FrontendIsdbsCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIsdbsCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbs3Caps: {
      android.hardware.tv.tuner.FrontendIsdbs3Capabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIsdbs3Capabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbtCaps: {
      android.hardware.tv.tuner.FrontendIsdbtCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIsdbtCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case iptvCaps: {
      android.hardware.tv.tuner.FrontendIptvCapabilities _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendIptvCapabilities.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case analogCaps:
      _mask |= describeContents(getAnalogCaps());
      break;
    case atscCaps:
      _mask |= describeContents(getAtscCaps());
      break;
    case atsc3Caps:
      _mask |= describeContents(getAtsc3Caps());
      break;
    case dtmbCaps:
      _mask |= describeContents(getDtmbCaps());
      break;
    case dvbsCaps:
      _mask |= describeContents(getDvbsCaps());
      break;
    case dvbcCaps:
      _mask |= describeContents(getDvbcCaps());
      break;
    case dvbtCaps:
      _mask |= describeContents(getDvbtCaps());
      break;
    case isdbsCaps:
      _mask |= describeContents(getIsdbsCaps());
      break;
    case isdbs3Caps:
      _mask |= describeContents(getIsdbs3Caps());
      break;
    case isdbtCaps:
      _mask |= describeContents(getIsdbtCaps());
      break;
    case iptvCaps:
      _mask |= describeContents(getIptvCaps());
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
    case analogCaps: return "analogCaps";
    case atscCaps: return "atscCaps";
    case atsc3Caps: return "atsc3Caps";
    case dtmbCaps: return "dtmbCaps";
    case dvbsCaps: return "dvbsCaps";
    case dvbcCaps: return "dvbcCaps";
    case dvbtCaps: return "dvbtCaps";
    case isdbsCaps: return "isdbsCaps";
    case isdbs3Caps: return "isdbs3Caps";
    case isdbtCaps: return "isdbtCaps";
    case iptvCaps: return "iptvCaps";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int analogCaps = 0;
    public static final int atscCaps = 1;
    public static final int atsc3Caps = 2;
    public static final int dtmbCaps = 3;
    public static final int dvbsCaps = 4;
    public static final int dvbcCaps = 5;
    public static final int dvbtCaps = 6;
    public static final int isdbsCaps = 7;
    public static final int isdbs3Caps = 8;
    public static final int isdbtCaps = 9;
    public static final int iptvCaps = 10;
  }
}
