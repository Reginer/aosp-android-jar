/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/FrontendStandardExt.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/FrontendStandardExt.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendStandardExt implements android.os.Parcelable {
  // tags for union fields
  public final static int dvbsStandardExt = 0;  // android.hardware.tv.tuner.FrontendDvbsStandard dvbsStandardExt;
  public final static int dvbtStandardExt = 1;  // android.hardware.tv.tuner.FrontendDvbtStandard dvbtStandardExt;

  private int _tag;
  private Object _value;

  public FrontendStandardExt() {
    byte _value = android.hardware.tv.tuner.FrontendDvbsStandard.UNDEFINED;
    this._tag = dvbsStandardExt;
    this._value = _value;
  }

  private FrontendStandardExt(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendStandardExt(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.FrontendDvbsStandard dvbsStandardExt;

  public static FrontendStandardExt dvbsStandardExt(byte _value) {
    return new FrontendStandardExt(dvbsStandardExt, _value);
  }

  public byte getDvbsStandardExt() {
    _assertTag(dvbsStandardExt);
    return (byte) _value;
  }

  public void setDvbsStandardExt(byte _value) {
    _set(dvbsStandardExt, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbtStandard dvbtStandardExt;

  public static FrontendStandardExt dvbtStandardExt(byte _value) {
    return new FrontendStandardExt(dvbtStandardExt, _value);
  }

  public byte getDvbtStandardExt() {
    _assertTag(dvbtStandardExt);
    return (byte) _value;
  }

  public void setDvbtStandardExt(byte _value) {
    _set(dvbtStandardExt, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendStandardExt> CREATOR = new android.os.Parcelable.Creator<FrontendStandardExt>() {
    @Override
    public FrontendStandardExt createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendStandardExt(_aidl_source);
    }
    @Override
    public FrontendStandardExt[] newArray(int _aidl_size) {
      return new FrontendStandardExt[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case dvbsStandardExt:
      _aidl_parcel.writeByte(getDvbsStandardExt());
      break;
    case dvbtStandardExt:
      _aidl_parcel.writeByte(getDvbtStandardExt());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case dvbsStandardExt: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbtStandardExt: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
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
    case dvbsStandardExt: return "dvbsStandardExt";
    case dvbtStandardExt: return "dvbtStandardExt";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int dvbsStandardExt = 0;
    public static final int dvbtStandardExt = 1;
  }
}
