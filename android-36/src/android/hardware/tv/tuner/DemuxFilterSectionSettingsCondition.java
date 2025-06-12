/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash b0d0067a930514438d7772c2e02069c7370f3620 --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen/android/hardware/tv/tuner/DemuxFilterSectionSettingsCondition.java.d -o out/soong/.intermediates/hardware/interfaces/tv/tuner/aidl/android.hardware.tv.tuner-V3-java-source/gen -Nhardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3 hardware/interfaces/tv/tuner/aidl/aidl_api/android.hardware.tv.tuner/3/android/hardware/tv/tuner/DemuxFilterSectionSettingsCondition.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class DemuxFilterSectionSettingsCondition implements android.os.Parcelable {
  // tags for union fields
  public final static int sectionBits = 0;  // android.hardware.tv.tuner.DemuxFilterSectionBits sectionBits;
  public final static int tableInfo = 1;  // android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo tableInfo;

  private int _tag;
  private Object _value;

  public DemuxFilterSectionSettingsCondition() {
    android.hardware.tv.tuner.DemuxFilterSectionBits _value = null;
    this._tag = sectionBits;
    this._value = _value;
  }

  private DemuxFilterSectionSettingsCondition(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private DemuxFilterSectionSettingsCondition(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.hardware.tv.tuner.DemuxFilterSectionBits sectionBits;

  public static DemuxFilterSectionSettingsCondition sectionBits(android.hardware.tv.tuner.DemuxFilterSectionBits _value) {
    return new DemuxFilterSectionSettingsCondition(sectionBits, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterSectionBits getSectionBits() {
    _assertTag(sectionBits);
    return (android.hardware.tv.tuner.DemuxFilterSectionBits) _value;
  }

  public void setSectionBits(android.hardware.tv.tuner.DemuxFilterSectionBits _value) {
    _set(sectionBits, _value);
  }

  // android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo tableInfo;

  public static DemuxFilterSectionSettingsCondition tableInfo(android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo _value) {
    return new DemuxFilterSectionSettingsCondition(tableInfo, _value);
  }

  public android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo getTableInfo() {
    _assertTag(tableInfo);
    return (android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo) _value;
  }

  public void setTableInfo(android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo _value) {
    _set(tableInfo, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<DemuxFilterSectionSettingsCondition> CREATOR = new android.os.Parcelable.Creator<DemuxFilterSectionSettingsCondition>() {
    @Override
    public DemuxFilterSectionSettingsCondition createFromParcel(android.os.Parcel _aidl_source) {
      return new DemuxFilterSectionSettingsCondition(_aidl_source);
    }
    @Override
    public DemuxFilterSectionSettingsCondition[] newArray(int _aidl_size) {
      return new DemuxFilterSectionSettingsCondition[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case sectionBits:
      _aidl_parcel.writeTypedObject(getSectionBits(), _aidl_flag);
      break;
    case tableInfo:
      _aidl_parcel.writeTypedObject(getTableInfo(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case sectionBits: {
      android.hardware.tv.tuner.DemuxFilterSectionBits _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterSectionBits.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case tableInfo: {
      android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.DemuxFilterSectionSettingsConditionTableInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case sectionBits:
      _mask |= describeContents(getSectionBits());
      break;
    case tableInfo:
      _mask |= describeContents(getTableInfo());
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
    case sectionBits: return "sectionBits";
    case tableInfo: return "tableInfo";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int sectionBits = 0;
    public static final int tableInfo = 1;
  }
}
