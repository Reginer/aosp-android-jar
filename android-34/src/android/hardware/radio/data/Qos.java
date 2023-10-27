/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public final class Qos implements android.os.Parcelable {
  // tags for union fields
  public final static int noinit = 0;  // boolean noinit;
  public final static int eps = 1;  // android.hardware.radio.data.EpsQos eps;
  public final static int nr = 2;  // android.hardware.radio.data.NrQos nr;

  private int _tag;
  private Object _value;

  public Qos() {
    boolean _value = false;
    this._tag = noinit;
    this._value = _value;
  }

  private Qos(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private Qos(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean noinit;

  public static Qos noinit(boolean _value) {
    return new Qos(noinit, _value);
  }

  public boolean getNoinit() {
    _assertTag(noinit);
    return (boolean) _value;
  }

  public void setNoinit(boolean _value) {
    _set(noinit, _value);
  }

  // android.hardware.radio.data.EpsQos eps;

  public static Qos eps(android.hardware.radio.data.EpsQos _value) {
    return new Qos(eps, _value);
  }

  public android.hardware.radio.data.EpsQos getEps() {
    _assertTag(eps);
    return (android.hardware.radio.data.EpsQos) _value;
  }

  public void setEps(android.hardware.radio.data.EpsQos _value) {
    _set(eps, _value);
  }

  // android.hardware.radio.data.NrQos nr;

  public static Qos nr(android.hardware.radio.data.NrQos _value) {
    return new Qos(nr, _value);
  }

  public android.hardware.radio.data.NrQos getNr() {
    _assertTag(nr);
    return (android.hardware.radio.data.NrQos) _value;
  }

  public void setNr(android.hardware.radio.data.NrQos _value) {
    _set(nr, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<Qos> CREATOR = new android.os.Parcelable.Creator<Qos>() {
    @Override
    public Qos createFromParcel(android.os.Parcel _aidl_source) {
      return new Qos(_aidl_source);
    }
    @Override
    public Qos[] newArray(int _aidl_size) {
      return new Qos[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case noinit:
      _aidl_parcel.writeBoolean(getNoinit());
      break;
    case eps:
      _aidl_parcel.writeTypedObject(getEps(), _aidl_flag);
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
    case eps: {
      android.hardware.radio.data.EpsQos _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.data.EpsQos.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case nr: {
      android.hardware.radio.data.NrQos _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.radio.data.NrQos.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case eps:
      _mask |= describeContents(getEps());
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
    case noinit: return "android.hardware.radio.data.Qos.noinit(" + (getNoinit()) + ")";
    case eps: return "android.hardware.radio.data.Qos.eps(" + (java.util.Objects.toString(getEps())) + ")";
    case nr: return "android.hardware.radio.data.Qos.nr(" + (java.util.Objects.toString(getNr())) + ")";
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
    case eps: return "eps";
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
    public static final int eps = 1;
    public static final int nr = 2;
  }
}
