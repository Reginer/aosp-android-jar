/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/DemuxPid.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/DemuxPid.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxPid implements android.os.Parcelable {
  // tags for union fields
  public final static int tPid = 0;  // int tPid;
  public final static int mmtpPid = 1;  // int mmtpPid;

  private int _tag;
  private Object _value;

  public DemuxPid() {
    int _value = 0;
    this._tag = tPid;
    this._value = _value;
  }

  private DemuxPid(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxPid(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // int tPid;

  public static DemuxPid tPid(int _value) {
    return new DemuxPid(tPid, _value);
  }

  public int getTPid() {
    _assertTag(tPid);
    return (int) _value;
  }

  public void setTPid(int _value) {
    _set(tPid, _value);
  }

  // int mmtpPid;

  public static DemuxPid mmtpPid(int _value) {
    return new DemuxPid(mmtpPid, _value);
  }

  public int getMmtpPid() {
    _assertTag(mmtpPid);
    return (int) _value;
  }

  public void setMmtpPid(int _value) {
    _set(mmtpPid, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxPid> CREATOR = new android.os.Parcelable.Creator<DemuxPid>() {
    @Override
    public DemuxPid createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxPid(_aidl_source);
    }
    @Override
    public DemuxPid[] newArray(int _aidl_size) {
      return new DemuxPid[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case tPid:
      _aidl_parcel.writeInt(getTPid());
      break;
    case mmtpPid:
      _aidl_parcel.writeInt(getMmtpPid());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case tPid: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case mmtpPid: {
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
    case tPid: return "tPid";
    case mmtpPid: return "mmtpPid";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int tPid = 0;
    public static final int mmtpPid = 1;
  }
}
