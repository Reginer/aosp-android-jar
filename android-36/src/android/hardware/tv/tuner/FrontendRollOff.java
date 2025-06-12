/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/FrontendRollOff.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/FrontendRollOff.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendRollOff implements android.os.Parcelable {
  // tags for union fields
  public final static int dvbs = 0;  // android.hardware.tv.tuner.FrontendDvbsRolloff dvbs;
  public final static int isdbs = 1;  // android.hardware.tv.tuner.FrontendIsdbsRolloff isdbs;
  public final static int isdbs3 = 2;  // android.hardware.tv.tuner.FrontendIsdbs3Rolloff isdbs3;

  private int _tag;
  private Object _value;

  public FrontendRollOff() {
    int _value = android.hardware.tv.tuner.FrontendDvbsRolloff.UNDEFINED;
    this._tag = dvbs;
    this._value = _value;
  }

  private FrontendRollOff(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendRollOff(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendDvbsRolloff dvbs;

  public static FrontendRollOff dvbs(int _value) {
    return new FrontendRollOff(dvbs, _value);
  }

  public int getDvbs() {
    _assertTag(dvbs);
    return (int) _value;
  }

  public void setDvbs(int _value) {
    _set(dvbs, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbsRolloff isdbs;

  public static FrontendRollOff isdbs(int _value) {
    return new FrontendRollOff(isdbs, _value);
  }

  public int getIsdbs() {
    _assertTag(isdbs);
    return (int) _value;
  }

  public void setIsdbs(int _value) {
    _set(isdbs, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbs3Rolloff isdbs3;

  public static FrontendRollOff isdbs3(int _value) {
    return new FrontendRollOff(isdbs3, _value);
  }

  public int getIsdbs3() {
    _assertTag(isdbs3);
    return (int) _value;
  }

  public void setIsdbs3(int _value) {
    _set(isdbs3, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendRollOff> CREATOR = new android.os.Parcelable.Creator<FrontendRollOff>() {
    @Override
    public FrontendRollOff createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendRollOff(_aidl_source);
    }
    @Override
    public FrontendRollOff[] newArray(int _aidl_size) {
      return new FrontendRollOff[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case dvbs:
      _aidl_parcel.writeInt(getDvbs());
      break;
    case isdbs:
      _aidl_parcel.writeInt(getIsdbs());
      break;
    case isdbs3:
      _aidl_parcel.writeInt(getIsdbs3());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case dvbs: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbs: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbs3: {
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
    case dvbs: return "dvbs";
    case isdbs: return "isdbs";
    case isdbs3: return "isdbs3";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int dvbs = 0;
    public static final int isdbs = 1;
    public static final int isdbs3 = 2;
  }
}
