/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/DemuxFilterScIndexMask.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/DemuxFilterScIndexMask.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterScIndexMask implements android.os.Parcelable {
  // tags for union fields
  public final static int scIndex = 0;  // int scIndex;
  public final static int scAvc = 1;  // int scAvc;
  public final static int scHevc = 2;  // int scHevc;
  public final static int scVvc = 3;  // int scVvc;

  private int _tag;
  private Object _value;

  public DemuxFilterScIndexMask() {
    int _value = 0;
    this._tag = scIndex;
    this._value = _value;
  }

  private DemuxFilterScIndexMask(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterScIndexMask(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // int scIndex;

  public static DemuxFilterScIndexMask scIndex(int _value) {
    return new DemuxFilterScIndexMask(scIndex, _value);
  }

  public int getScIndex() {
    _assertTag(scIndex);
    return (int) _value;
  }

  public void setScIndex(int _value) {
    _set(scIndex, _value);
  }

  // int scAvc;

  public static DemuxFilterScIndexMask scAvc(int _value) {
    return new DemuxFilterScIndexMask(scAvc, _value);
  }

  public int getScAvc() {
    _assertTag(scAvc);
    return (int) _value;
  }

  public void setScAvc(int _value) {
    _set(scAvc, _value);
  }

  // int scHevc;

  public static DemuxFilterScIndexMask scHevc(int _value) {
    return new DemuxFilterScIndexMask(scHevc, _value);
  }

  public int getScHevc() {
    _assertTag(scHevc);
    return (int) _value;
  }

  public void setScHevc(int _value) {
    _set(scHevc, _value);
  }

  // int scVvc;

  public static DemuxFilterScIndexMask scVvc(int _value) {
    return new DemuxFilterScIndexMask(scVvc, _value);
  }

  public int getScVvc() {
    _assertTag(scVvc);
    return (int) _value;
  }

  public void setScVvc(int _value) {
    _set(scVvc, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterScIndexMask> CREATOR = new android.os.Parcelable.Creator<DemuxFilterScIndexMask>() {
    @Override
    public DemuxFilterScIndexMask createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterScIndexMask(_aidl_source);
    }
    @Override
    public DemuxFilterScIndexMask[] newArray(int _aidl_size) {
      return new DemuxFilterScIndexMask[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case scIndex:
      _aidl_parcel.writeInt(getScIndex());
      break;
    case scAvc:
      _aidl_parcel.writeInt(getScAvc());
      break;
    case scHevc:
      _aidl_parcel.writeInt(getScHevc());
      break;
    case scVvc:
      _aidl_parcel.writeInt(getScVvc());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case scIndex: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case scAvc: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case scHevc: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case scVvc: {
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
    case scIndex: return "scIndex";
    case scAvc: return "scAvc";
    case scHevc: return "scHevc";
    case scVvc: return "scVvc";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int scIndex = 0;
    public static final int scAvc = 1;
    public static final int scHevc = 2;
    public static final int scVvc = 3;
  }
}
