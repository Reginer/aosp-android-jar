/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.vibrator;
public final class PrimitivePwle implements android.os.Parcelable {
  // tags for union fields
  public final static int active = 0;  // android.hardware.vibrator.ActivePwle active;
  public final static int braking = 1;  // android.hardware.vibrator.BrakingPwle braking;

  private int _tag;
  private Object _value;

  public PrimitivePwle() {
    android.hardware.vibrator.ActivePwle _value = null;
    this._tag = active;
    this._value = _value;
  }

  private PrimitivePwle(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private PrimitivePwle(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.vibrator.ActivePwle active;

  public static PrimitivePwle active(android.hardware.vibrator.ActivePwle _value) {
    return new PrimitivePwle(active, _value);
  }

  public android.hardware.vibrator.ActivePwle getActive() {
    _assertTag(active);
    return (android.hardware.vibrator.ActivePwle) _value;
  }

  public void setActive(android.hardware.vibrator.ActivePwle _value) {
    _set(active, _value);
  }

  // android.hardware.vibrator.BrakingPwle braking;

  public static PrimitivePwle braking(android.hardware.vibrator.BrakingPwle _value) {
    return new PrimitivePwle(braking, _value);
  }

  public android.hardware.vibrator.BrakingPwle getBraking() {
    _assertTag(braking);
    return (android.hardware.vibrator.BrakingPwle) _value;
  }

  public void setBraking(android.hardware.vibrator.BrakingPwle _value) {
    _set(braking, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<PrimitivePwle> CREATOR = new android.os.Parcelable.Creator<PrimitivePwle>() {
    @Override
    public PrimitivePwle createFromParcel(android.os.Parcel _aidl_source) {
      return new PrimitivePwle(_aidl_source);
    }
    @Override
    public PrimitivePwle[] newArray(int _aidl_size) {
      return new PrimitivePwle[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case active:
      _aidl_parcel.writeTypedObject(getActive(), _aidl_flag);
      break;
    case braking:
      _aidl_parcel.writeTypedObject(getBraking(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case active: {
      android.hardware.vibrator.ActivePwle _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.vibrator.ActivePwle.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case braking: {
      android.hardware.vibrator.BrakingPwle _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.vibrator.BrakingPwle.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case active:
      _mask |= describeContents(getActive());
      break;
    case braking:
      _mask |= describeContents(getBraking());
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
    case active: return "active";
    case braking: return "braking";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int active = 0;
    public static final int braking = 1;
  }
}
