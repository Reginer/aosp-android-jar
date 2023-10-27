/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendScanMessage implements android.os.Parcelable {
  // tags for union fields
  public final static int isLocked = 0;  // boolean isLocked;
  public final static int isEnd = 1;  // boolean isEnd;
  public final static int progressPercent = 2;  // int progressPercent;
  public final static int frequencies = 3;  // long[] frequencies;
  public final static int symbolRates = 4;  // int[] symbolRates;
  public final static int hierarchy = 5;  // android.hardware.tv.tuner.FrontendDvbtHierarchy hierarchy;
  public final static int analogType = 6;  // android.hardware.tv.tuner.FrontendAnalogType analogType;
  public final static int plpIds = 7;  // int[] plpIds;
  public final static int groupIds = 8;  // int[] groupIds;
  public final static int inputStreamIds = 9;  // int[] inputStreamIds;
  public final static int std = 10;  // android.hardware.tv.tuner.FrontendScanMessageStandard std;
  public final static int atsc3PlpInfos = 11;  // android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] atsc3PlpInfos;
  public final static int modulation = 12;  // android.hardware.tv.tuner.FrontendModulation modulation;
  public final static int annex = 13;  // android.hardware.tv.tuner.FrontendDvbcAnnex annex;
  public final static int isHighPriority = 14;  // boolean isHighPriority;
  public final static int dvbtCellIds = 15;  // int[] dvbtCellIds;

  private int _tag;
  private Object _value;

  public FrontendScanMessage() {
    boolean _value = false;
    this._tag = isLocked;
    this._value = _value;
  }

  private FrontendScanMessage(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendScanMessage(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean isLocked;

  public static FrontendScanMessage isLocked(boolean _value) {
    return new FrontendScanMessage(isLocked, _value);
  }

  public boolean getIsLocked() {
    _assertTag(isLocked);
    return (boolean) _value;
  }

  public void setIsLocked(boolean _value) {
    _set(isLocked, _value);
  }

  // boolean isEnd;

  public static FrontendScanMessage isEnd(boolean _value) {
    return new FrontendScanMessage(isEnd, _value);
  }

  public boolean getIsEnd() {
    _assertTag(isEnd);
    return (boolean) _value;
  }

  public void setIsEnd(boolean _value) {
    _set(isEnd, _value);
  }

  // int progressPercent;

  public static FrontendScanMessage progressPercent(int _value) {
    return new FrontendScanMessage(progressPercent, _value);
  }

  public int getProgressPercent() {
    _assertTag(progressPercent);
    return (int) _value;
  }

  public void setProgressPercent(int _value) {
    _set(progressPercent, _value);
  }

  // long[] frequencies;

  public static FrontendScanMessage frequencies(long[] _value) {
    return new FrontendScanMessage(frequencies, _value);
  }

  public long[] getFrequencies() {
    _assertTag(frequencies);
    return (long[]) _value;
  }

  public void setFrequencies(long[] _value) {
    _set(frequencies, _value);
  }

  // int[] symbolRates;

  public static FrontendScanMessage symbolRates(int[] _value) {
    return new FrontendScanMessage(symbolRates, _value);
  }

  public int[] getSymbolRates() {
    _assertTag(symbolRates);
    return (int[]) _value;
  }

  public void setSymbolRates(int[] _value) {
    _set(symbolRates, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbtHierarchy hierarchy;

  public static FrontendScanMessage hierarchy(int _value) {
    return new FrontendScanMessage(hierarchy, _value);
  }

  public int getHierarchy() {
    _assertTag(hierarchy);
    return (int) _value;
  }

  public void setHierarchy(int _value) {
    _set(hierarchy, _value);
  }

  // android.hardware.tv.tuner.FrontendAnalogType analogType;

  public static FrontendScanMessage analogType(int _value) {
    return new FrontendScanMessage(analogType, _value);
  }

  public int getAnalogType() {
    _assertTag(analogType);
    return (int) _value;
  }

  public void setAnalogType(int _value) {
    _set(analogType, _value);
  }

  // int[] plpIds;

  public static FrontendScanMessage plpIds(int[] _value) {
    return new FrontendScanMessage(plpIds, _value);
  }

  public int[] getPlpIds() {
    _assertTag(plpIds);
    return (int[]) _value;
  }

  public void setPlpIds(int[] _value) {
    _set(plpIds, _value);
  }

  // int[] groupIds;

  public static FrontendScanMessage groupIds(int[] _value) {
    return new FrontendScanMessage(groupIds, _value);
  }

  public int[] getGroupIds() {
    _assertTag(groupIds);
    return (int[]) _value;
  }

  public void setGroupIds(int[] _value) {
    _set(groupIds, _value);
  }

  // int[] inputStreamIds;

  public static FrontendScanMessage inputStreamIds(int[] _value) {
    return new FrontendScanMessage(inputStreamIds, _value);
  }

  public int[] getInputStreamIds() {
    _assertTag(inputStreamIds);
    return (int[]) _value;
  }

  public void setInputStreamIds(int[] _value) {
    _set(inputStreamIds, _value);
  }

  // android.hardware.tv.tuner.FrontendScanMessageStandard std;

  public static FrontendScanMessage std(android.hardware.tv.tuner.FrontendScanMessageStandard _value) {
    return new FrontendScanMessage(std, _value);
  }

  public android.hardware.tv.tuner.FrontendScanMessageStandard getStd() {
    _assertTag(std);
    return (android.hardware.tv.tuner.FrontendScanMessageStandard) _value;
  }

  public void setStd(android.hardware.tv.tuner.FrontendScanMessageStandard _value) {
    _set(std, _value);
  }

  // android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] atsc3PlpInfos;

  public static FrontendScanMessage atsc3PlpInfos(android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] _value) {
    return new FrontendScanMessage(atsc3PlpInfos, _value);
  }

  public android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] getAtsc3PlpInfos() {
    _assertTag(atsc3PlpInfos);
    return (android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[]) _value;
  }

  public void setAtsc3PlpInfos(android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] _value) {
    _set(atsc3PlpInfos, _value);
  }

  // android.hardware.tv.tuner.FrontendModulation modulation;

  public static FrontendScanMessage modulation(android.hardware.tv.tuner.FrontendModulation _value) {
    return new FrontendScanMessage(modulation, _value);
  }

  public android.hardware.tv.tuner.FrontendModulation getModulation() {
    _assertTag(modulation);
    return (android.hardware.tv.tuner.FrontendModulation) _value;
  }

  public void setModulation(android.hardware.tv.tuner.FrontendModulation _value) {
    _set(modulation, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbcAnnex annex;

  public static FrontendScanMessage annex(byte _value) {
    return new FrontendScanMessage(annex, _value);
  }

  public byte getAnnex() {
    _assertTag(annex);
    return (byte) _value;
  }

  public void setAnnex(byte _value) {
    _set(annex, _value);
  }

  // boolean isHighPriority;

  public static FrontendScanMessage isHighPriority(boolean _value) {
    return new FrontendScanMessage(isHighPriority, _value);
  }

  public boolean getIsHighPriority() {
    _assertTag(isHighPriority);
    return (boolean) _value;
  }

  public void setIsHighPriority(boolean _value) {
    _set(isHighPriority, _value);
  }

  // int[] dvbtCellIds;

  public static FrontendScanMessage dvbtCellIds(int[] _value) {
    return new FrontendScanMessage(dvbtCellIds, _value);
  }

  public int[] getDvbtCellIds() {
    _assertTag(dvbtCellIds);
    return (int[]) _value;
  }

  public void setDvbtCellIds(int[] _value) {
    _set(dvbtCellIds, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendScanMessage> CREATOR = new android.os.Parcelable.Creator<FrontendScanMessage>() {
    @Override
    public FrontendScanMessage createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendScanMessage(_aidl_source);
    }
    @Override
    public FrontendScanMessage[] newArray(int _aidl_size) {
      return new FrontendScanMessage[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case isLocked:
      _aidl_parcel.writeBoolean(getIsLocked());
      break;
    case isEnd:
      _aidl_parcel.writeBoolean(getIsEnd());
      break;
    case progressPercent:
      _aidl_parcel.writeInt(getProgressPercent());
      break;
    case frequencies:
      _aidl_parcel.writeLongArray(getFrequencies());
      break;
    case symbolRates:
      _aidl_parcel.writeIntArray(getSymbolRates());
      break;
    case hierarchy:
      _aidl_parcel.writeInt(getHierarchy());
      break;
    case analogType:
      _aidl_parcel.writeInt(getAnalogType());
      break;
    case plpIds:
      _aidl_parcel.writeIntArray(getPlpIds());
      break;
    case groupIds:
      _aidl_parcel.writeIntArray(getGroupIds());
      break;
    case inputStreamIds:
      _aidl_parcel.writeIntArray(getInputStreamIds());
      break;
    case std:
      _aidl_parcel.writeTypedObject(getStd(), _aidl_flag);
      break;
    case atsc3PlpInfos:
      _aidl_parcel.writeTypedArray(getAtsc3PlpInfos(), _aidl_flag);
      break;
    case modulation:
      _aidl_parcel.writeTypedObject(getModulation(), _aidl_flag);
      break;
    case annex:
      _aidl_parcel.writeByte(getAnnex());
      break;
    case isHighPriority:
      _aidl_parcel.writeBoolean(getIsHighPriority());
      break;
    case dvbtCellIds:
      _aidl_parcel.writeIntArray(getDvbtCellIds());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case isLocked: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isEnd: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case progressPercent: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case frequencies: {
      long[] _aidl_value;
      _aidl_value = _aidl_parcel.createLongArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case symbolRates: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case hierarchy: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case analogType: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case plpIds: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case groupIds: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case inputStreamIds: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case std: {
      android.hardware.tv.tuner.FrontendScanMessageStandard _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendScanMessageStandard.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case atsc3PlpInfos: {
      android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] _aidl_value;
      _aidl_value = _aidl_parcel.createTypedArray(android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case modulation: {
      android.hardware.tv.tuner.FrontendModulation _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendModulation.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case annex: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isHighPriority: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbtCellIds: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case std:
      _mask |= describeContents(getStd());
      break;
    case atsc3PlpInfos:
      _mask |= describeContents(getAtsc3PlpInfos());
      break;
    case modulation:
      _mask |= describeContents(getModulation());
      break;
    }
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof Object[]) {
      int _mask = 0;
      for (Object o : (Object[]) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
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
    case isLocked: return "isLocked";
    case isEnd: return "isEnd";
    case progressPercent: return "progressPercent";
    case frequencies: return "frequencies";
    case symbolRates: return "symbolRates";
    case hierarchy: return "hierarchy";
    case analogType: return "analogType";
    case plpIds: return "plpIds";
    case groupIds: return "groupIds";
    case inputStreamIds: return "inputStreamIds";
    case std: return "std";
    case atsc3PlpInfos: return "atsc3PlpInfos";
    case modulation: return "modulation";
    case annex: return "annex";
    case isHighPriority: return "isHighPriority";
    case dvbtCellIds: return "dvbtCellIds";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int isLocked = 0;
    public static final int isEnd = 1;
    public static final int progressPercent = 2;
    public static final int frequencies = 3;
    public static final int symbolRates = 4;
    public static final int hierarchy = 5;
    public static final int analogType = 6;
    public static final int plpIds = 7;
    public static final int groupIds = 8;
    public static final int inputStreamIds = 9;
    public static final int std = 10;
    public static final int atsc3PlpInfos = 11;
    public static final int modulation = 12;
    public static final int annex = 13;
    public static final int isHighPriority = 14;
    public static final int dvbtCellIds = 15;
  }
}
