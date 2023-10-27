/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public final class CellIdentity implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int gsm = 1;  // android.hardware.radio.network.CellIdentityGsm gsm;
  public final static int wcdma = 2;  // android.hardware.radio.network.CellIdentityWcdma wcdma;
  public final static int tdscdma = 3;  // android.hardware.radio.network.CellIdentityTdscdma tdscdma;
  public final static int cdma = 4;  // android.hardware.radio.network.CellIdentityCdma cdma;
  public final static int lte = 5;  // android.hardware.radio.network.CellIdentityLte lte;
  public final static int nr = 6;  // android.hardware.radio.network.CellIdentityNr nr;

  private int _tag;
  private Object _value;

  public CellIdentity() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private CellIdentity(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private CellIdentity(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static CellIdentity noinit(boolean _value) {
    return new CellIdentity(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.radio.network.CellIdentityGsm gsm;

  public static CellIdentity gsm(android.hardware.radio.network.CellIdentityGsm _value) {
    return new CellIdentity(gsm, _value);
  }

  public android.hardware.radio.network.CellIdentityGsm getGsm() {
    _assertTag(gsm);
    return (android.hardware.radio.network.CellIdentityGsm) _value;
  }

  public void setGsm(android.hardware.radio.network.CellIdentityGsm _value) {
    _set(gsm, _value);
  }

  // android.hardware.radio.network.CellIdentityWcdma wcdma;

  public static CellIdentity wcdma(android.hardware.radio.network.CellIdentityWcdma _value) {
    return new CellIdentity(wcdma, _value);
  }

  public android.hardware.radio.network.CellIdentityWcdma getWcdma() {
    _assertTag(wcdma);
    return (android.hardware.radio.network.CellIdentityWcdma) _value;
  }

  public void setWcdma(android.hardware.radio.network.CellIdentityWcdma _value) {
    _set(wcdma, _value);
  }

  // android.hardware.radio.network.CellIdentityTdscdma tdscdma;

  public static CellIdentity tdscdma(android.hardware.radio.network.CellIdentityTdscdma _value) {
    return new CellIdentity(tdscdma, _value);
  }

  public android.hardware.radio.network.CellIdentityTdscdma getTdscdma() {
    _assertTag(tdscdma);
    return (android.hardware.radio.network.CellIdentityTdscdma) _value;
  }

  public void setTdscdma(android.hardware.radio.network.CellIdentityTdscdma _value) {
    _set(tdscdma, _value);
  }

  // android.hardware.radio.network.CellIdentityCdma cdma;

  public static CellIdentity cdma(android.hardware.radio.network.CellIdentityCdma _value) {
    return new CellIdentity(cdma, _value);
  }

  public android.hardware.radio.network.CellIdentityCdma getCdma() {
    _assertTag(cdma);
    return (android.hardware.radio.network.CellIdentityCdma) _value;
  }

  public void setCdma(android.hardware.radio.network.CellIdentityCdma _value) {
    _set(cdma, _value);
  }

  // android.hardware.radio.network.CellIdentityLte lte;

  public static CellIdentity lte(android.hardware.radio.network.CellIdentityLte _value) {
    return new CellIdentity(lte, _value);
  }

  public android.hardware.radio.network.CellIdentityLte getLte() {
    _assertTag(lte);
    return (android.hardware.radio.network.CellIdentityLte) _value;
  }

  public void setLte(android.hardware.radio.network.CellIdentityLte _value) {
    _set(lte, _value);
  }

  // android.hardware.radio.network.CellIdentityNr nr;

  public static CellIdentity nr(android.hardware.radio.network.CellIdentityNr _value) {
    return new CellIdentity(nr, _value);
  }

  public android.hardware.radio.network.CellIdentityNr getNr() {
    _assertTag(nr);
    return (android.hardware.radio.network.CellIdentityNr) _value;
  }

  public void setNr(android.hardware.radio.network.CellIdentityNr _value) {
    _set(nr, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<CellIdentity> CREATOR = new android.os.Parcelable.Creator<CellIdentity>() {
    @Override
    public CellIdentity createFromParcel(android.os.Parcel _aidl_source) {
      return new CellIdentity(_aidl_source);
    }
    @Override
    public CellIdentity[] newArray(int _aidl_size) {
      return new CellIdentity[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case noinit:
      _aidl_parcel.writeBoolean(getNoinit());
      break;
    case gsm:
      _aidl_parcel.writeTypedObject(getGsm(), _aidl_flag);
      break;
    case wcdma:
      _aidl_parcel.writeTypedObject(getWcdma(), _aidl_flag);
      break;
    case tdscdma:
      _aidl_parcel.writeTypedObject(getTdscdma(), _aidl_flag);
      break;
    case cdma:
      _aidl_parcel.writeTypedObject(getCdma(), _aidl_flag);
      break;
    case lte:
      _aidl_parcel.writeTypedObject(getLte(), _aidl_flag);
      break;
    case nr:
      _aidl_parcel.writeTypedObject(getNr(), _aidl_flag);
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
    case gsm: {
      android.hardware.radio.network.CellIdentityGsm _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentityGsm.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case wcdma: {
      android.hardware.radio.network.CellIdentityWcdma _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentityWcdma.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case tdscdma: {
      android.hardware.radio.network.CellIdentityTdscdma _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentityTdscdma.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case cdma: {
      android.hardware.radio.network.CellIdentityCdma _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentityCdma.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case lte: {
      android.hardware.radio.network.CellIdentityLte _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentityLte.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case nr: {
      android.hardware.radio.network.CellIdentityNr _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.network.CellIdentityNr.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case gsm:
      _mask |= describeContents(getGsm());
      break;
    case wcdma:
      _mask |= describeContents(getWcdma());
      break;
    case tdscdma:
      _mask |= describeContents(getTdscdma());
      break;
    case cdma:
      _mask |= describeContents(getCdma());
      break;
    case lte:
      _mask |= describeContents(getLte());
      break;
    case nr:
      _mask |= describeContents(getNr());
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
    case noinit: return "android.hardware.radio.network.CellIdentity.noinit(" + (getNoinit()) + ")";
    case gsm: return "android.hardware.radio.network.CellIdentity.gsm(" + (java.util.Objects.toString(getGsm())) + ")";
    case wcdma: return "android.hardware.radio.network.CellIdentity.wcdma(" + (java.util.Objects.toString(getWcdma())) + ")";
    case tdscdma: return "android.hardware.radio.network.CellIdentity.tdscdma(" + (java.util.Objects.toString(getTdscdma())) + ")";
    case cdma: return "android.hardware.radio.network.CellIdentity.cdma(" + (java.util.Objects.toString(getCdma())) + ")";
    case lte: return "android.hardware.radio.network.CellIdentity.lte(" + (java.util.Objects.toString(getLte())) + ")";
    case nr: return "android.hardware.radio.network.CellIdentity.nr(" + (java.util.Objects.toString(getNr())) + ")";
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
    case gsm: return "gsm";
    case wcdma: return "wcdma";
    case tdscdma: return "tdscdma";
    case cdma: return "cdma";
    case lte: return "lte";
    case nr: return "nr";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int noinit = 0;
    public static final int gsm = 1;
    public static final int wcdma = 2;
    public static final int tdscdma = 3;
    public static final int cdma = 4;
    public static final int lte = 5;
    public static final int nr = 6;
  }
}
