/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.biometrics.common;
/** @hide */
public final class AuthenticateReason implements android.os.Parcelable {
  // tags for union fields
  public final static int vendorAuthenticateReason = 0;  // android.hardware.biometrics.common.AuthenticateReason.Vendor vendorAuthenticateReason;
  public final static int faceAuthenticateReason = 1;  // android.hardware.biometrics.common.AuthenticateReason.Face faceAuthenticateReason;
  public final static int fingerprintAuthenticateReason = 2;  // android.hardware.biometrics.common.AuthenticateReason.Fingerprint fingerprintAuthenticateReason;

  private int _tag;
  private Object _value;

  public AuthenticateReason() {
    android.hardware.biometrics.common.AuthenticateReason.Vendor _value = null;
    this._tag = vendorAuthenticateReason;
    this._value = _value;
  }

  private AuthenticateReason(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AuthenticateReason(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.biometrics.common.AuthenticateReason.Vendor vendorAuthenticateReason;

  public static AuthenticateReason vendorAuthenticateReason(android.hardware.biometrics.common.AuthenticateReason.Vendor _value) {
    return new AuthenticateReason(vendorAuthenticateReason, _value);
  }

  public android.hardware.biometrics.common.AuthenticateReason.Vendor getVendorAuthenticateReason() {
    _assertTag(vendorAuthenticateReason);
    return (android.hardware.biometrics.common.AuthenticateReason.Vendor) _value;
  }

  public void setVendorAuthenticateReason(android.hardware.biometrics.common.AuthenticateReason.Vendor _value) {
    _set(vendorAuthenticateReason, _value);
  }

  // android.hardware.biometrics.common.AuthenticateReason.Face faceAuthenticateReason;

  public static AuthenticateReason faceAuthenticateReason(int _value) {
    return new AuthenticateReason(faceAuthenticateReason, _value);
  }

  public int getFaceAuthenticateReason() {
    _assertTag(faceAuthenticateReason);
    return (int) _value;
  }

  public void setFaceAuthenticateReason(int _value) {
    _set(faceAuthenticateReason, _value);
  }

  // android.hardware.biometrics.common.AuthenticateReason.Fingerprint fingerprintAuthenticateReason;

  public static AuthenticateReason fingerprintAuthenticateReason(int _value) {
    return new AuthenticateReason(fingerprintAuthenticateReason, _value);
  }

  public int getFingerprintAuthenticateReason() {
    _assertTag(fingerprintAuthenticateReason);
    return (int) _value;
  }

  public void setFingerprintAuthenticateReason(int _value) {
    _set(fingerprintAuthenticateReason, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AuthenticateReason> CREATOR = new android.os.Parcelable.Creator<AuthenticateReason>() {
    @Override
    public AuthenticateReason createFromParcel(android.os.Parcel _aidl_source) {
      return new AuthenticateReason(_aidl_source);
    }
    @Override
    public AuthenticateReason[] newArray(int _aidl_size) {
      return new AuthenticateReason[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case vendorAuthenticateReason:
      _aidl_parcel.writeTypedObject(getVendorAuthenticateReason(), _aidl_flag);
      break;
    case faceAuthenticateReason:
      _aidl_parcel.writeInt(getFaceAuthenticateReason());
      break;
    case fingerprintAuthenticateReason:
      _aidl_parcel.writeInt(getFingerprintAuthenticateReason());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case vendorAuthenticateReason: {
      android.hardware.biometrics.common.AuthenticateReason.Vendor _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.biometrics.common.AuthenticateReason.Vendor.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case faceAuthenticateReason: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case fingerprintAuthenticateReason: {
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
    case vendorAuthenticateReason:
      _mask |= describeContents(getVendorAuthenticateReason());
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
    case vendorAuthenticateReason: return "vendorAuthenticateReason";
    case faceAuthenticateReason: return "faceAuthenticateReason";
    case fingerprintAuthenticateReason: return "fingerprintAuthenticateReason";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static class Vendor implements android.os.Parcelable
  {
    public final android.os.ParcelableHolder extension = new android.os.ParcelableHolder(android.os.Parcelable.PARCELABLE_STABILITY_VINTF);
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<Vendor> CREATOR = new android.os.Parcelable.Creator<Vendor>() {
      @Override
      public Vendor createFromParcel(android.os.Parcel _aidl_source) {
        Vendor _aidl_out = new Vendor();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public Vendor[] newArray(int _aidl_size) {
        return new Vendor[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedObject(extension, 0);
      int _aidl_end_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.setDataPosition(_aidl_start_pos);
      _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
      _aidl_parcel.setDataPosition(_aidl_end_pos);
    }
    public final void readFromParcel(android.os.Parcel _aidl_parcel)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      int _aidl_parcelable_size = _aidl_parcel.readInt();
      try {
        if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small");;
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        if ((0!=_aidl_parcel.readInt())) {
          extension.readFromParcel(_aidl_parcel);
        }
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    @Override
    public int describeContents() {
      int _mask = 0;
      _mask |= describeContents(extension);
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }
  }
  public static @interface Fingerprint {
    public static final int UNKNOWN = 0;
  }
  public static @interface Face {
    public static final int UNKNOWN = 0;
    public static final int STARTED_WAKING_UP = 1;
    public static final int PRIMARY_BOUNCER_SHOWN = 2;
    public static final int ASSISTANT_VISIBLE = 3;
    public static final int ALTERNATE_BIOMETRIC_BOUNCER_SHOWN = 4;
    public static final int NOTIFICATION_PANEL_CLICKED = 5;
    public static final int OCCLUDING_APP_REQUESTED = 6;
    public static final int PICK_UP_GESTURE_TRIGGERED = 7;
    public static final int QS_EXPANDED = 8;
    public static final int SWIPE_UP_ON_BOUNCER = 9;
    public static final int UDFPS_POINTER_DOWN = 10;
  }
  public static @interface Tag {
    public static final int vendorAuthenticateReason = 0;
    public static final int faceAuthenticateReason = 1;
    public static final int fingerprintAuthenticateReason = 2;
  }
}
