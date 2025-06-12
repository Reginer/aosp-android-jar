/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash 8a6cd86630181a4df6f20056259ec200ffe39209 -t --stability vintf --min_sdk_version platform_apis --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common-V4-java-source/gen/android/hardware/biometrics/common/OperationState.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common-V4-java-source/gen -Nhardware/interfaces/biometrics/common/aidl/aidl_api/android.hardware.biometrics.common/4 hardware/interfaces/biometrics/common/aidl/aidl_api/android.hardware.biometrics.common/4/android/hardware/biometrics/common/OperationState.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.common;
/** @hide */
public final class OperationState implements android.os.Parcelable {
  // tags for union fields
  public final static int fingerprintOperationState = 0;  // android.hardware.biometrics.common.OperationState.FingerprintOperationState fingerprintOperationState;
  public final static int faceOperationState = 1;  // android.hardware.biometrics.common.OperationState.FaceOperationState faceOperationState;

  private int _tag;
  private Object _value;

  public OperationState() {
    android.hardware.biometrics.common.OperationState.FingerprintOperationState _value = null;
    this._tag = fingerprintOperationState;
    this._value = _value;
  }

  private OperationState(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private OperationState(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.biometrics.common.OperationState.FingerprintOperationState fingerprintOperationState;

  public static OperationState fingerprintOperationState(android.hardware.biometrics.common.OperationState.FingerprintOperationState _value) {
    return new OperationState(fingerprintOperationState, _value);
  }

  public android.hardware.biometrics.common.OperationState.FingerprintOperationState getFingerprintOperationState() {
    _assertTag(fingerprintOperationState);
    return (android.hardware.biometrics.common.OperationState.FingerprintOperationState) _value;
  }

  public void setFingerprintOperationState(android.hardware.biometrics.common.OperationState.FingerprintOperationState _value) {
    _set(fingerprintOperationState, _value);
  }

  // android.hardware.biometrics.common.OperationState.FaceOperationState faceOperationState;

  public static OperationState faceOperationState(android.hardware.biometrics.common.OperationState.FaceOperationState _value) {
    return new OperationState(faceOperationState, _value);
  }

  public android.hardware.biometrics.common.OperationState.FaceOperationState getFaceOperationState() {
    _assertTag(faceOperationState);
    return (android.hardware.biometrics.common.OperationState.FaceOperationState) _value;
  }

  public void setFaceOperationState(android.hardware.biometrics.common.OperationState.FaceOperationState _value) {
    _set(faceOperationState, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<OperationState> CREATOR = new android.os.Parcelable.Creator<OperationState>() {
    @Override
    public OperationState createFromParcel(android.os.Parcel _aidl_source) {
      return new OperationState(_aidl_source);
    }
    @Override
    public OperationState[] newArray(int _aidl_size) {
      return new OperationState[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case fingerprintOperationState:
      _aidl_parcel.writeTypedObject(getFingerprintOperationState(), _aidl_flag);
      break;
    case faceOperationState:
      _aidl_parcel.writeTypedObject(getFaceOperationState(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case fingerprintOperationState: {
      android.hardware.biometrics.common.OperationState.FingerprintOperationState _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.biometrics.common.OperationState.FingerprintOperationState.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case faceOperationState: {
      android.hardware.biometrics.common.OperationState.FaceOperationState _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.biometrics.common.OperationState.FaceOperationState.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case fingerprintOperationState:
      _mask |= describeContents(getFingerprintOperationState());
      break;
    case faceOperationState:
      _mask |= describeContents(getFaceOperationState());
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
    case fingerprintOperationState: return "fingerprintOperationState";
    case faceOperationState: return "faceOperationState";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static class FingerprintOperationState implements android.os.Parcelable
  {
    public final android.os.ParcelableHolder extension = new android.os.ParcelableHolder(android.os.Parcelable.PARCELABLE_STABILITY_VINTF);
    public boolean isHardwareIgnoringTouches = false;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<FingerprintOperationState> CREATOR = new android.os.Parcelable.Creator<FingerprintOperationState>() {
      @Override
      public FingerprintOperationState createFromParcel(android.os.Parcel _aidl_source) {
        FingerprintOperationState _aidl_out = new FingerprintOperationState();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public FingerprintOperationState[] newArray(int _aidl_size) {
        return new FingerprintOperationState[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedObject(extension, 0);
      _aidl_parcel.writeBoolean(isHardwareIgnoringTouches);
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
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        isHardwareIgnoringTouches = _aidl_parcel.readBoolean();
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
  public static class FaceOperationState implements android.os.Parcelable
  {
    public final android.os.ParcelableHolder extension = new android.os.ParcelableHolder(android.os.Parcelable.PARCELABLE_STABILITY_VINTF);
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<FaceOperationState> CREATOR = new android.os.Parcelable.Creator<FaceOperationState>() {
      @Override
      public FaceOperationState createFromParcel(android.os.Parcel _aidl_source) {
        FaceOperationState _aidl_out = new FaceOperationState();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public FaceOperationState[] newArray(int _aidl_size) {
        return new FaceOperationState[_aidl_size];
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
  public static @interface Tag {
    public static final int fingerprintOperationState = 0;
    public static final int faceOperationState = 1;
  }
}
