/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public final class AccessTechnologySpecificInfo implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int cdmaInfo = 1;  // android.hardware.radio.network.Cdma2000RegistrationInfo cdmaInfo;
  public final static int eutranInfo = 2;  // android.hardware.radio.network.EutranRegistrationInfo eutranInfo;
  public final static int ngranNrVopsInfo = 3;  // android.hardware.radio.network.NrVopsInfo ngranNrVopsInfo;
  public final static int geranDtmSupported = 4;  // boolean geranDtmSupported;

  private int _tag;
  private Object _value;

  public AccessTechnologySpecificInfo() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private AccessTechnologySpecificInfo(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AccessTechnologySpecificInfo(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static AccessTechnologySpecificInfo noinit(boolean _value) {
    return new AccessTechnologySpecificInfo(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.radio.network.Cdma2000RegistrationInfo cdmaInfo;

  public static AccessTechnologySpecificInfo cdmaInfo(android.hardware.radio.network.Cdma2000RegistrationInfo _value) {
    return new AccessTechnologySpecificInfo(cdmaInfo, _value);
  }

  public android.hardware.radio.network.Cdma2000RegistrationInfo getCdmaInfo() {
    _assertTag(cdmaInfo);
    return (android.hardware.radio.network.Cdma2000RegistrationInfo) _value;
  }

  public void setCdmaInfo(android.hardware.radio.network.Cdma2000RegistrationInfo _value) {
    _set(cdmaInfo, _value);
  }

  // android.hardware.radio.network.EutranRegistrationInfo eutranInfo;

  public static AccessTechnologySpecificInfo eutranInfo(android.hardware.radio.network.EutranRegistrationInfo _value) {
    return new AccessTechnologySpecificInfo(eutranInfo, _value);
  }

  public android.hardware.radio.network.EutranRegistrationInfo getEutranInfo() {
    _assertTag(eutranInfo);
    return (android.hardware.radio.network.EutranRegistrationInfo) _value;
  }

  public void setEutranInfo(android.hardware.radio.network.EutranRegistrationInfo _value) {
    _set(eutranInfo, _value);
  }

  // android.hardware.radio.network.NrVopsInfo ngranNrVopsInfo;

  public static AccessTechnologySpecificInfo ngranNrVopsInfo(android.hardware.radio.network.NrVopsInfo _value) {
    return new AccessTechnologySpecificInfo(ngranNrVopsInfo, _value);
  }

  public android.hardware.radio.network.NrVopsInfo getNgranNrVopsInfo() {
    _assertTag(ngranNrVopsInfo);
    return (android.hardware.radio.network.NrVopsInfo) _value;
  }

  public void setNgranNrVopsInfo(android.hardware.radio.network.NrVopsInfo _value) {
    _set(ngranNrVopsInfo, _value);
  }

  // boolean geranDtmSupported;

  public static AccessTechnologySpecificInfo geranDtmSupported(boolean _value) {
    return new AccessTechnologySpecificInfo(geranDtmSupported, _value);
  }

  public boolean getGeranDtmSupported() {
    _assertTag(geranDtmSupported);
    return (boolean) _value;
  }

  public void setGeranDtmSupported(boolean _value) {
    _set(geranDtmSupported, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AccessTechnologySpecificInfo> CREATOR = new android.os.Parcelable.Creator<AccessTechnologySpecificInfo>() {
    @Override
    public AccessTechnologySpecificInfo createFromParcel(android.os.Parcel _aidl_source) {
      return new AccessTechnologySpecificInfo(_aidl_source);
    }
    @Override
    public AccessTechnologySpecificInfo[] newArray(int _aidl_size) {
      return new AccessTechnologySpecificInfo[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case noinit:
      _aidl_parcel.writeBoolean(getNoinit());
      break;
    case cdmaInfo:
      _aidl_parcel.writeTypedObject(getCdmaInfo(), _aidl_flag);
      break;
    case eutranInfo:
      _aidl_parcel.writeTypedObject(getEutranInfo(), _aidl_flag);
      break;
    case ngranNrVopsInfo:
      _aidl_parcel.writeTypedObject(getNgranNrVopsInfo(), _aidl_flag);
      break;
    case geranDtmSupported:
      _aidl_parcel.writeBoolean(getGeranDtmSupported());
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
    case cdmaInfo: {
      android.hardware.radio.network.Cdma2000RegistrationInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.Cdma2000RegistrationInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case eutranInfo: {
      android.hardware.radio.network.EutranRegistrationInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.EutranRegistrationInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case ngranNrVopsInfo: {
      android.hardware.radio.network.NrVopsInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.NrVopsInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case geranDtmSupported: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case cdmaInfo:
      _mask |= describeContents(getCdmaInfo());
      break;
    case eutranInfo:
      _mask |= describeContents(getEutranInfo());
      break;
    case ngranNrVopsInfo:
      _mask |= describeContents(getNgranNrVopsInfo());
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

  @Override
  public String toString() {
    switch (_tag) {
    case noinit: return "android.hardware.radio.network.AccessTechnologySpecificInfo.noinit(" + (getNoinit()) + ")";
    case cdmaInfo: return "android.hardware.radio.network.AccessTechnologySpecificInfo.cdmaInfo(" + (java.util.Objects.toString(getCdmaInfo())) + ")";
    case eutranInfo: return "android.hardware.radio.network.AccessTechnologySpecificInfo.eutranInfo(" + (java.util.Objects.toString(getEutranInfo())) + ")";
    case ngranNrVopsInfo: return "android.hardware.radio.network.AccessTechnologySpecificInfo.ngranNrVopsInfo(" + (java.util.Objects.toString(getNgranNrVopsInfo())) + ")";
    case geranDtmSupported: return "android.hardware.radio.network.AccessTechnologySpecificInfo.geranDtmSupported(" + (getGeranDtmSupported()) + ")";
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
    case cdmaInfo: return "cdmaInfo";
    case eutranInfo: return "eutranInfo";
    case ngranNrVopsInfo: return "ngranNrVopsInfo";
    case geranDtmSupported: return "geranDtmSupported";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int noinit = 0;
    public static final int cdmaInfo = 1;
    public static final int eutranInfo = 2;
    public static final int ngranNrVopsInfo = 3;
    public static final int geranDtmSupported = 4;
  }
}
