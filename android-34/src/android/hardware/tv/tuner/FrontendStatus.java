/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public final class FrontendStatus implements android.os.Parcelable {
  // tags for union fields
  public final static int isDemodLocked = 0;  // boolean isDemodLocked;
  public final static int snr = 1;  // int snr;
  public final static int ber = 2;  // int ber;
  public final static int per = 3;  // int per;
  public final static int preBer = 4;  // int preBer;
  public final static int signalQuality = 5;  // int signalQuality;
  public final static int signalStrength = 6;  // int signalStrength;
  public final static int symbolRate = 7;  // int symbolRate;
  public final static int innerFec = 8;  // android.hardware.tv.tuner.FrontendInnerFec innerFec;
  public final static int modulationStatus = 9;  // android.hardware.tv.tuner.FrontendModulationStatus modulationStatus;
  public final static int inversion = 10;  // android.hardware.tv.tuner.FrontendSpectralInversion inversion;
  public final static int lnbVoltage = 11;  // android.hardware.tv.tuner.LnbVoltage lnbVoltage;
  public final static int plpId = 12;  // int plpId;
  public final static int isEWBS = 13;  // boolean isEWBS;
  public final static int agc = 14;  // int agc;
  public final static int isLnaOn = 15;  // boolean isLnaOn;
  public final static int isLayerError = 16;  // boolean[] isLayerError;
  public final static int mer = 17;  // int mer;
  public final static int freqOffset = 18;  // long freqOffset;
  public final static int hierarchy = 19;  // android.hardware.tv.tuner.FrontendDvbtHierarchy hierarchy;
  public final static int isRfLocked = 20;  // boolean isRfLocked;
  public final static int plpInfo = 21;  // android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo[] plpInfo;
  public final static int modulations = 22;  // android.hardware.tv.tuner.FrontendModulation[] modulations;
  public final static int bers = 23;  // int[] bers;
  public final static int codeRates = 24;  // android.hardware.tv.tuner.FrontendInnerFec[] codeRates;
  public final static int bandwidth = 25;  // android.hardware.tv.tuner.FrontendBandwidth bandwidth;
  public final static int interval = 26;  // android.hardware.tv.tuner.FrontendGuardInterval interval;
  public final static int transmissionMode = 27;  // android.hardware.tv.tuner.FrontendTransmissionMode transmissionMode;
  public final static int uec = 28;  // int uec;
  public final static int systemId = 29;  // int systemId;
  public final static int interleaving = 30;  // android.hardware.tv.tuner.FrontendInterleaveMode[] interleaving;
  public final static int isdbtSegment = 31;  // int[] isdbtSegment;
  public final static int tsDataRate = 32;  // int[] tsDataRate;
  public final static int rollOff = 33;  // android.hardware.tv.tuner.FrontendRollOff rollOff;
  public final static int isMiso = 34;  // boolean isMiso;
  public final static int isLinear = 35;  // boolean isLinear;
  public final static int isShortFrames = 36;  // boolean isShortFrames;
  public final static int isdbtMode = 37;  // android.hardware.tv.tuner.FrontendIsdbtMode isdbtMode;
  public final static int partialReceptionFlag = 38;  // android.hardware.tv.tuner.FrontendIsdbtPartialReceptionFlag partialReceptionFlag;
  public final static int streamIdList = 39;  // int[] streamIdList;
  public final static int dvbtCellIds = 40;  // int[] dvbtCellIds;
  public final static int allPlpInfo = 41;  // android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] allPlpInfo;
  public final static int iptvContentUrl = 42;  // String iptvContentUrl;
  public final static int iptvPacketsReceived = 43;  // long iptvPacketsReceived;
  public final static int iptvPacketsLost = 44;  // long iptvPacketsLost;
  public final static int iptvWorstJitterMs = 45;  // int iptvWorstJitterMs;
  public final static int iptvAverageJitterMs = 46;  // int iptvAverageJitterMs;

  private int _tag;
  private Object _value;

  public FrontendStatus() {
    boolean _value = false;
    this._tag = isDemodLocked;
    this._value = _value;
  }

  private FrontendStatus(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private FrontendStatus(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // boolean isDemodLocked;

  public static FrontendStatus isDemodLocked(boolean _value) {
    return new FrontendStatus(isDemodLocked, _value);
  }

  public boolean getIsDemodLocked() {
    _assertTag(isDemodLocked);
    return (boolean) _value;
  }

  public void setIsDemodLocked(boolean _value) {
    _set(isDemodLocked, _value);
  }

  // int snr;

  public static FrontendStatus snr(int _value) {
    return new FrontendStatus(snr, _value);
  }

  public int getSnr() {
    _assertTag(snr);
    return (int) _value;
  }

  public void setSnr(int _value) {
    _set(snr, _value);
  }

  // int ber;

  public static FrontendStatus ber(int _value) {
    return new FrontendStatus(ber, _value);
  }

  public int getBer() {
    _assertTag(ber);
    return (int) _value;
  }

  public void setBer(int _value) {
    _set(ber, _value);
  }

  // int per;

  public static FrontendStatus per(int _value) {
    return new FrontendStatus(per, _value);
  }

  public int getPer() {
    _assertTag(per);
    return (int) _value;
  }

  public void setPer(int _value) {
    _set(per, _value);
  }

  // int preBer;

  public static FrontendStatus preBer(int _value) {
    return new FrontendStatus(preBer, _value);
  }

  public int getPreBer() {
    _assertTag(preBer);
    return (int) _value;
  }

  public void setPreBer(int _value) {
    _set(preBer, _value);
  }

  // int signalQuality;

  public static FrontendStatus signalQuality(int _value) {
    return new FrontendStatus(signalQuality, _value);
  }

  public int getSignalQuality() {
    _assertTag(signalQuality);
    return (int) _value;
  }

  public void setSignalQuality(int _value) {
    _set(signalQuality, _value);
  }

  // int signalStrength;

  public static FrontendStatus signalStrength(int _value) {
    return new FrontendStatus(signalStrength, _value);
  }

  public int getSignalStrength() {
    _assertTag(signalStrength);
    return (int) _value;
  }

  public void setSignalStrength(int _value) {
    _set(signalStrength, _value);
  }

  // int symbolRate;

  public static FrontendStatus symbolRate(int _value) {
    return new FrontendStatus(symbolRate, _value);
  }

  public int getSymbolRate() {
    _assertTag(symbolRate);
    return (int) _value;
  }

  public void setSymbolRate(int _value) {
    _set(symbolRate, _value);
  }

  // android.hardware.tv.tuner.FrontendInnerFec innerFec;

  public static FrontendStatus innerFec(long _value) {
    return new FrontendStatus(innerFec, _value);
  }

  public long getInnerFec() {
    _assertTag(innerFec);
    return (long) _value;
  }

  public void setInnerFec(long _value) {
    _set(innerFec, _value);
  }

  // android.hardware.tv.tuner.FrontendModulationStatus modulationStatus;

  public static FrontendStatus modulationStatus(android.hardware.tv.tuner.FrontendModulationStatus _value) {
    return new FrontendStatus(modulationStatus, _value);
  }

  public android.hardware.tv.tuner.FrontendModulationStatus getModulationStatus() {
    _assertTag(modulationStatus);
    return (android.hardware.tv.tuner.FrontendModulationStatus) _value;
  }

  public void setModulationStatus(android.hardware.tv.tuner.FrontendModulationStatus _value) {
    _set(modulationStatus, _value);
  }

  // android.hardware.tv.tuner.FrontendSpectralInversion inversion;

  public static FrontendStatus inversion(int _value) {
    return new FrontendStatus(inversion, _value);
  }

  public int getInversion() {
    _assertTag(inversion);
    return (int) _value;
  }

  public void setInversion(int _value) {
    _set(inversion, _value);
  }

  // android.hardware.tv.tuner.LnbVoltage lnbVoltage;

  public static FrontendStatus lnbVoltage(int _value) {
    return new FrontendStatus(lnbVoltage, _value);
  }

  public int getLnbVoltage() {
    _assertTag(lnbVoltage);
    return (int) _value;
  }

  public void setLnbVoltage(int _value) {
    _set(lnbVoltage, _value);
  }

  // int plpId;

  public static FrontendStatus plpId(int _value) {
    return new FrontendStatus(plpId, _value);
  }

  public int getPlpId() {
    _assertTag(plpId);
    return (int) _value;
  }

  public void setPlpId(int _value) {
    _set(plpId, _value);
  }

  // boolean isEWBS;

  public static FrontendStatus isEWBS(boolean _value) {
    return new FrontendStatus(isEWBS, _value);
  }

  public boolean getIsEWBS() {
    _assertTag(isEWBS);
    return (boolean) _value;
  }

  public void setIsEWBS(boolean _value) {
    _set(isEWBS, _value);
  }

  // int agc;

  public static FrontendStatus agc(int _value) {
    return new FrontendStatus(agc, _value);
  }

  public int getAgc() {
    _assertTag(agc);
    return (int) _value;
  }

  public void setAgc(int _value) {
    _set(agc, _value);
  }

  // boolean isLnaOn;

  public static FrontendStatus isLnaOn(boolean _value) {
    return new FrontendStatus(isLnaOn, _value);
  }

  public boolean getIsLnaOn() {
    _assertTag(isLnaOn);
    return (boolean) _value;
  }

  public void setIsLnaOn(boolean _value) {
    _set(isLnaOn, _value);
  }

  // boolean[] isLayerError;

  public static FrontendStatus isLayerError(boolean[] _value) {
    return new FrontendStatus(isLayerError, _value);
  }

  public boolean[] getIsLayerError() {
    _assertTag(isLayerError);
    return (boolean[]) _value;
  }

  public void setIsLayerError(boolean[] _value) {
    _set(isLayerError, _value);
  }

  // int mer;

  public static FrontendStatus mer(int _value) {
    return new FrontendStatus(mer, _value);
  }

  public int getMer() {
    _assertTag(mer);
    return (int) _value;
  }

  public void setMer(int _value) {
    _set(mer, _value);
  }

  // long freqOffset;

  public static FrontendStatus freqOffset(long _value) {
    return new FrontendStatus(freqOffset, _value);
  }

  public long getFreqOffset() {
    _assertTag(freqOffset);
    return (long) _value;
  }

  public void setFreqOffset(long _value) {
    _set(freqOffset, _value);
  }

  // android.hardware.tv.tuner.FrontendDvbtHierarchy hierarchy;

  public static FrontendStatus hierarchy(int _value) {
    return new FrontendStatus(hierarchy, _value);
  }

  public int getHierarchy() {
    _assertTag(hierarchy);
    return (int) _value;
  }

  public void setHierarchy(int _value) {
    _set(hierarchy, _value);
  }

  // boolean isRfLocked;

  public static FrontendStatus isRfLocked(boolean _value) {
    return new FrontendStatus(isRfLocked, _value);
  }

  public boolean getIsRfLocked() {
    _assertTag(isRfLocked);
    return (boolean) _value;
  }

  public void setIsRfLocked(boolean _value) {
    _set(isRfLocked, _value);
  }

  // android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo[] plpInfo;

  public static FrontendStatus plpInfo(android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo[] _value) {
    return new FrontendStatus(plpInfo, _value);
  }

  public android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo[] getPlpInfo() {
    _assertTag(plpInfo);
    return (android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo[]) _value;
  }

  public void setPlpInfo(android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo[] _value) {
    _set(plpInfo, _value);
  }

  // android.hardware.tv.tuner.FrontendModulation[] modulations;

  public static FrontendStatus modulations(android.hardware.tv.tuner.FrontendModulation[] _value) {
    return new FrontendStatus(modulations, _value);
  }

  public android.hardware.tv.tuner.FrontendModulation[] getModulations() {
    _assertTag(modulations);
    return (android.hardware.tv.tuner.FrontendModulation[]) _value;
  }

  public void setModulations(android.hardware.tv.tuner.FrontendModulation[] _value) {
    _set(modulations, _value);
  }

  // int[] bers;

  public static FrontendStatus bers(int[] _value) {
    return new FrontendStatus(bers, _value);
  }

  public int[] getBers() {
    _assertTag(bers);
    return (int[]) _value;
  }

  public void setBers(int[] _value) {
    _set(bers, _value);
  }

  // android.hardware.tv.tuner.FrontendInnerFec[] codeRates;

  public static FrontendStatus codeRates(long[] _value) {
    return new FrontendStatus(codeRates, _value);
  }

  public long[] getCodeRates() {
    _assertTag(codeRates);
    return (long[]) _value;
  }

  public void setCodeRates(long[] _value) {
    _set(codeRates, _value);
  }

  // android.hardware.tv.tuner.FrontendBandwidth bandwidth;

  public static FrontendStatus bandwidth(android.hardware.tv.tuner.FrontendBandwidth _value) {
    return new FrontendStatus(bandwidth, _value);
  }

  public android.hardware.tv.tuner.FrontendBandwidth getBandwidth() {
    _assertTag(bandwidth);
    return (android.hardware.tv.tuner.FrontendBandwidth) _value;
  }

  public void setBandwidth(android.hardware.tv.tuner.FrontendBandwidth _value) {
    _set(bandwidth, _value);
  }

  // android.hardware.tv.tuner.FrontendGuardInterval interval;

  public static FrontendStatus interval(android.hardware.tv.tuner.FrontendGuardInterval _value) {
    return new FrontendStatus(interval, _value);
  }

  public android.hardware.tv.tuner.FrontendGuardInterval getInterval() {
    _assertTag(interval);
    return (android.hardware.tv.tuner.FrontendGuardInterval) _value;
  }

  public void setInterval(android.hardware.tv.tuner.FrontendGuardInterval _value) {
    _set(interval, _value);
  }

  // android.hardware.tv.tuner.FrontendTransmissionMode transmissionMode;

  public static FrontendStatus transmissionMode(android.hardware.tv.tuner.FrontendTransmissionMode _value) {
    return new FrontendStatus(transmissionMode, _value);
  }

  public android.hardware.tv.tuner.FrontendTransmissionMode getTransmissionMode() {
    _assertTag(transmissionMode);
    return (android.hardware.tv.tuner.FrontendTransmissionMode) _value;
  }

  public void setTransmissionMode(android.hardware.tv.tuner.FrontendTransmissionMode _value) {
    _set(transmissionMode, _value);
  }

  // int uec;

  public static FrontendStatus uec(int _value) {
    return new FrontendStatus(uec, _value);
  }

  public int getUec() {
    _assertTag(uec);
    return (int) _value;
  }

  public void setUec(int _value) {
    _set(uec, _value);
  }

  // int systemId;

  public static FrontendStatus systemId(int _value) {
    return new FrontendStatus(systemId, _value);
  }

  public int getSystemId() {
    _assertTag(systemId);
    return (int) _value;
  }

  public void setSystemId(int _value) {
    _set(systemId, _value);
  }

  // android.hardware.tv.tuner.FrontendInterleaveMode[] interleaving;

  public static FrontendStatus interleaving(android.hardware.tv.tuner.FrontendInterleaveMode[] _value) {
    return new FrontendStatus(interleaving, _value);
  }

  public android.hardware.tv.tuner.FrontendInterleaveMode[] getInterleaving() {
    _assertTag(interleaving);
    return (android.hardware.tv.tuner.FrontendInterleaveMode[]) _value;
  }

  public void setInterleaving(android.hardware.tv.tuner.FrontendInterleaveMode[] _value) {
    _set(interleaving, _value);
  }

  // int[] isdbtSegment;

  public static FrontendStatus isdbtSegment(int[] _value) {
    return new FrontendStatus(isdbtSegment, _value);
  }

  public int[] getIsdbtSegment() {
    _assertTag(isdbtSegment);
    return (int[]) _value;
  }

  public void setIsdbtSegment(int[] _value) {
    _set(isdbtSegment, _value);
  }

  // int[] tsDataRate;

  public static FrontendStatus tsDataRate(int[] _value) {
    return new FrontendStatus(tsDataRate, _value);
  }

  public int[] getTsDataRate() {
    _assertTag(tsDataRate);
    return (int[]) _value;
  }

  public void setTsDataRate(int[] _value) {
    _set(tsDataRate, _value);
  }

  // android.hardware.tv.tuner.FrontendRollOff rollOff;

  public static FrontendStatus rollOff(android.hardware.tv.tuner.FrontendRollOff _value) {
    return new FrontendStatus(rollOff, _value);
  }

  public android.hardware.tv.tuner.FrontendRollOff getRollOff() {
    _assertTag(rollOff);
    return (android.hardware.tv.tuner.FrontendRollOff) _value;
  }

  public void setRollOff(android.hardware.tv.tuner.FrontendRollOff _value) {
    _set(rollOff, _value);
  }

  // boolean isMiso;

  public static FrontendStatus isMiso(boolean _value) {
    return new FrontendStatus(isMiso, _value);
  }

  public boolean getIsMiso() {
    _assertTag(isMiso);
    return (boolean) _value;
  }

  public void setIsMiso(boolean _value) {
    _set(isMiso, _value);
  }

  // boolean isLinear;

  public static FrontendStatus isLinear(boolean _value) {
    return new FrontendStatus(isLinear, _value);
  }

  public boolean getIsLinear() {
    _assertTag(isLinear);
    return (boolean) _value;
  }

  public void setIsLinear(boolean _value) {
    _set(isLinear, _value);
  }

  // boolean isShortFrames;

  public static FrontendStatus isShortFrames(boolean _value) {
    return new FrontendStatus(isShortFrames, _value);
  }

  public boolean getIsShortFrames() {
    _assertTag(isShortFrames);
    return (boolean) _value;
  }

  public void setIsShortFrames(boolean _value) {
    _set(isShortFrames, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtMode isdbtMode;

  public static FrontendStatus isdbtMode(int _value) {
    return new FrontendStatus(isdbtMode, _value);
  }

  public int getIsdbtMode() {
    _assertTag(isdbtMode);
    return (int) _value;
  }

  public void setIsdbtMode(int _value) {
    _set(isdbtMode, _value);
  }

  // android.hardware.tv.tuner.FrontendIsdbtPartialReceptionFlag partialReceptionFlag;

  public static FrontendStatus partialReceptionFlag(int _value) {
    return new FrontendStatus(partialReceptionFlag, _value);
  }

  public int getPartialReceptionFlag() {
    _assertTag(partialReceptionFlag);
    return (int) _value;
  }

  public void setPartialReceptionFlag(int _value) {
    _set(partialReceptionFlag, _value);
  }

  // int[] streamIdList;

  public static FrontendStatus streamIdList(int[] _value) {
    return new FrontendStatus(streamIdList, _value);
  }

  public int[] getStreamIdList() {
    _assertTag(streamIdList);
    return (int[]) _value;
  }

  public void setStreamIdList(int[] _value) {
    _set(streamIdList, _value);
  }

  // int[] dvbtCellIds;

  public static FrontendStatus dvbtCellIds(int[] _value) {
    return new FrontendStatus(dvbtCellIds, _value);
  }

  public int[] getDvbtCellIds() {
    _assertTag(dvbtCellIds);
    return (int[]) _value;
  }

  public void setDvbtCellIds(int[] _value) {
    _set(dvbtCellIds, _value);
  }

  // android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] allPlpInfo;

  public static FrontendStatus allPlpInfo(android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] _value) {
    return new FrontendStatus(allPlpInfo, _value);
  }

  public android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] getAllPlpInfo() {
    _assertTag(allPlpInfo);
    return (android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[]) _value;
  }

  public void setAllPlpInfo(android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] _value) {
    _set(allPlpInfo, _value);
  }

  // String iptvContentUrl;

  public static FrontendStatus iptvContentUrl(java.lang.String _value) {
    return new FrontendStatus(iptvContentUrl, _value);
  }

  public java.lang.String getIptvContentUrl() {
    _assertTag(iptvContentUrl);
    return (java.lang.String) _value;
  }

  public void setIptvContentUrl(java.lang.String _value) {
    _set(iptvContentUrl, _value);
  }

  // long iptvPacketsReceived;

  public static FrontendStatus iptvPacketsReceived(long _value) {
    return new FrontendStatus(iptvPacketsReceived, _value);
  }

  public long getIptvPacketsReceived() {
    _assertTag(iptvPacketsReceived);
    return (long) _value;
  }

  public void setIptvPacketsReceived(long _value) {
    _set(iptvPacketsReceived, _value);
  }

  // long iptvPacketsLost;

  public static FrontendStatus iptvPacketsLost(long _value) {
    return new FrontendStatus(iptvPacketsLost, _value);
  }

  public long getIptvPacketsLost() {
    _assertTag(iptvPacketsLost);
    return (long) _value;
  }

  public void setIptvPacketsLost(long _value) {
    _set(iptvPacketsLost, _value);
  }

  // int iptvWorstJitterMs;

  public static FrontendStatus iptvWorstJitterMs(int _value) {
    return new FrontendStatus(iptvWorstJitterMs, _value);
  }

  public int getIptvWorstJitterMs() {
    _assertTag(iptvWorstJitterMs);
    return (int) _value;
  }

  public void setIptvWorstJitterMs(int _value) {
    _set(iptvWorstJitterMs, _value);
  }

  // int iptvAverageJitterMs;

  public static FrontendStatus iptvAverageJitterMs(int _value) {
    return new FrontendStatus(iptvAverageJitterMs, _value);
  }

  public int getIptvAverageJitterMs() {
    _assertTag(iptvAverageJitterMs);
    return (int) _value;
  }

  public void setIptvAverageJitterMs(int _value) {
    _set(iptvAverageJitterMs, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<FrontendStatus> CREATOR = new android.os.Parcelable.Creator<FrontendStatus>() {
    @Override
    public FrontendStatus createFromParcel(android.os.Parcel _aidl_source) {
      return new FrontendStatus(_aidl_source);
    }
    @Override
    public FrontendStatus[] newArray(int _aidl_size) {
      return new FrontendStatus[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case isDemodLocked:
      _aidl_parcel.writeBoolean(getIsDemodLocked());
      break;
    case snr:
      _aidl_parcel.writeInt(getSnr());
      break;
    case ber:
      _aidl_parcel.writeInt(getBer());
      break;
    case per:
      _aidl_parcel.writeInt(getPer());
      break;
    case preBer:
      _aidl_parcel.writeInt(getPreBer());
      break;
    case signalQuality:
      _aidl_parcel.writeInt(getSignalQuality());
      break;
    case signalStrength:
      _aidl_parcel.writeInt(getSignalStrength());
      break;
    case symbolRate:
      _aidl_parcel.writeInt(getSymbolRate());
      break;
    case innerFec:
      _aidl_parcel.writeLong(getInnerFec());
      break;
    case modulationStatus:
      _aidl_parcel.writeTypedObject(getModulationStatus(), _aidl_flag);
      break;
    case inversion:
      _aidl_parcel.writeInt(getInversion());
      break;
    case lnbVoltage:
      _aidl_parcel.writeInt(getLnbVoltage());
      break;
    case plpId:
      _aidl_parcel.writeInt(getPlpId());
      break;
    case isEWBS:
      _aidl_parcel.writeBoolean(getIsEWBS());
      break;
    case agc:
      _aidl_parcel.writeInt(getAgc());
      break;
    case isLnaOn:
      _aidl_parcel.writeBoolean(getIsLnaOn());
      break;
    case isLayerError:
      _aidl_parcel.writeBooleanArray(getIsLayerError());
      break;
    case mer:
      _aidl_parcel.writeInt(getMer());
      break;
    case freqOffset:
      _aidl_parcel.writeLong(getFreqOffset());
      break;
    case hierarchy:
      _aidl_parcel.writeInt(getHierarchy());
      break;
    case isRfLocked:
      _aidl_parcel.writeBoolean(getIsRfLocked());
      break;
    case plpInfo:
      _aidl_parcel.writeTypedArray(getPlpInfo(), _aidl_flag);
      break;
    case modulations:
      _aidl_parcel.writeTypedArray(getModulations(), _aidl_flag);
      break;
    case bers:
      _aidl_parcel.writeIntArray(getBers());
      break;
    case codeRates:
      _aidl_parcel.writeLongArray(getCodeRates());
      break;
    case bandwidth:
      _aidl_parcel.writeTypedObject(getBandwidth(), _aidl_flag);
      break;
    case interval:
      _aidl_parcel.writeTypedObject(getInterval(), _aidl_flag);
      break;
    case transmissionMode:
      _aidl_parcel.writeTypedObject(getTransmissionMode(), _aidl_flag);
      break;
    case uec:
      _aidl_parcel.writeInt(getUec());
      break;
    case systemId:
      _aidl_parcel.writeInt(getSystemId());
      break;
    case interleaving:
      _aidl_parcel.writeTypedArray(getInterleaving(), _aidl_flag);
      break;
    case isdbtSegment:
      _aidl_parcel.writeIntArray(getIsdbtSegment());
      break;
    case tsDataRate:
      _aidl_parcel.writeIntArray(getTsDataRate());
      break;
    case rollOff:
      _aidl_parcel.writeTypedObject(getRollOff(), _aidl_flag);
      break;
    case isMiso:
      _aidl_parcel.writeBoolean(getIsMiso());
      break;
    case isLinear:
      _aidl_parcel.writeBoolean(getIsLinear());
      break;
    case isShortFrames:
      _aidl_parcel.writeBoolean(getIsShortFrames());
      break;
    case isdbtMode:
      _aidl_parcel.writeInt(getIsdbtMode());
      break;
    case partialReceptionFlag:
      _aidl_parcel.writeInt(getPartialReceptionFlag());
      break;
    case streamIdList:
      _aidl_parcel.writeIntArray(getStreamIdList());
      break;
    case dvbtCellIds:
      _aidl_parcel.writeIntArray(getDvbtCellIds());
      break;
    case allPlpInfo:
      _aidl_parcel.writeTypedArray(getAllPlpInfo(), _aidl_flag);
      break;
    case iptvContentUrl:
      _aidl_parcel.writeString(getIptvContentUrl());
      break;
    case iptvPacketsReceived:
      _aidl_parcel.writeLong(getIptvPacketsReceived());
      break;
    case iptvPacketsLost:
      _aidl_parcel.writeLong(getIptvPacketsLost());
      break;
    case iptvWorstJitterMs:
      _aidl_parcel.writeInt(getIptvWorstJitterMs());
      break;
    case iptvAverageJitterMs:
      _aidl_parcel.writeInt(getIptvAverageJitterMs());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case isDemodLocked: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case snr: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case ber: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case per: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case preBer: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case signalQuality: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case signalStrength: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case symbolRate: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case innerFec: {
      long _aidl_value;
      _aidl_value = _aidl_parcel.readLong();
      _set(_aidl_tag, _aidl_value);
      return; }
    case modulationStatus: {
      android.hardware.tv.tuner.FrontendModulationStatus _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendModulationStatus.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case inversion: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case lnbVoltage: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case plpId: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isEWBS: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case agc: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isLnaOn: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isLayerError: {
      boolean[] _aidl_value;
      _aidl_value = _aidl_parcel.createBooleanArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case mer: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case freqOffset: {
      long _aidl_value;
      _aidl_value = _aidl_parcel.readLong();
      _set(_aidl_tag, _aidl_value);
      return; }
    case hierarchy: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isRfLocked: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case plpInfo: {
      android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo[] _aidl_value;
      _aidl_value = _aidl_parcel.createTypedArray(android.hardware.tv.tuner.FrontendStatusAtsc3PlpInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case modulations: {
      android.hardware.tv.tuner.FrontendModulation[] _aidl_value;
      _aidl_value = _aidl_parcel.createTypedArray(android.hardware.tv.tuner.FrontendModulation.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case bers: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case codeRates: {
      long[] _aidl_value;
      _aidl_value = _aidl_parcel.createLongArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case bandwidth: {
      android.hardware.tv.tuner.FrontendBandwidth _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendBandwidth.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case interval: {
      android.hardware.tv.tuner.FrontendGuardInterval _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendGuardInterval.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case transmissionMode: {
      android.hardware.tv.tuner.FrontendTransmissionMode _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendTransmissionMode.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case uec: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case systemId: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case interleaving: {
      android.hardware.tv.tuner.FrontendInterleaveMode[] _aidl_value;
      _aidl_value = _aidl_parcel.createTypedArray(android.hardware.tv.tuner.FrontendInterleaveMode.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbtSegment: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case tsDataRate: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case rollOff: {
      android.hardware.tv.tuner.FrontendRollOff _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendRollOff.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case isMiso: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isLinear: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isShortFrames: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case isdbtMode: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case partialReceptionFlag: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case streamIdList: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dvbtCellIds: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case allPlpInfo: {
      android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo[] _aidl_value;
      _aidl_value = _aidl_parcel.createTypedArray(android.hardware.tv.tuner.FrontendScanAtsc3PlpInfo.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case iptvContentUrl: {
      java.lang.String _aidl_value;
      _aidl_value = _aidl_parcel.readString();
      _set(_aidl_tag, _aidl_value);
      return; }
    case iptvPacketsReceived: {
      long _aidl_value;
      _aidl_value = _aidl_parcel.readLong();
      _set(_aidl_tag, _aidl_value);
      return; }
    case iptvPacketsLost: {
      long _aidl_value;
      _aidl_value = _aidl_parcel.readLong();
      _set(_aidl_tag, _aidl_value);
      return; }
    case iptvWorstJitterMs: {
      int _aidl_value;
      _aidl_value = _aidl_parcel.readInt();
      _set(_aidl_tag, _aidl_value);
      return; }
    case iptvAverageJitterMs: {
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
    case modulationStatus:
      _mask |= describeContents(getModulationStatus());
      break;
    case plpInfo:
      _mask |= describeContents(getPlpInfo());
      break;
    case modulations:
      _mask |= describeContents(getModulations());
      break;
    case bandwidth:
      _mask |= describeContents(getBandwidth());
      break;
    case interval:
      _mask |= describeContents(getInterval());
      break;
    case transmissionMode:
      _mask |= describeContents(getTransmissionMode());
      break;
    case interleaving:
      _mask |= describeContents(getInterleaving());
      break;
    case rollOff:
      _mask |= describeContents(getRollOff());
      break;
    case allPlpInfo:
      _mask |= describeContents(getAllPlpInfo());
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
    case isDemodLocked: return "isDemodLocked";
    case snr: return "snr";
    case ber: return "ber";
    case per: return "per";
    case preBer: return "preBer";
    case signalQuality: return "signalQuality";
    case signalStrength: return "signalStrength";
    case symbolRate: return "symbolRate";
    case innerFec: return "innerFec";
    case modulationStatus: return "modulationStatus";
    case inversion: return "inversion";
    case lnbVoltage: return "lnbVoltage";
    case plpId: return "plpId";
    case isEWBS: return "isEWBS";
    case agc: return "agc";
    case isLnaOn: return "isLnaOn";
    case isLayerError: return "isLayerError";
    case mer: return "mer";
    case freqOffset: return "freqOffset";
    case hierarchy: return "hierarchy";
    case isRfLocked: return "isRfLocked";
    case plpInfo: return "plpInfo";
    case modulations: return "modulations";
    case bers: return "bers";
    case codeRates: return "codeRates";
    case bandwidth: return "bandwidth";
    case interval: return "interval";
    case transmissionMode: return "transmissionMode";
    case uec: return "uec";
    case systemId: return "systemId";
    case interleaving: return "interleaving";
    case isdbtSegment: return "isdbtSegment";
    case tsDataRate: return "tsDataRate";
    case rollOff: return "rollOff";
    case isMiso: return "isMiso";
    case isLinear: return "isLinear";
    case isShortFrames: return "isShortFrames";
    case isdbtMode: return "isdbtMode";
    case partialReceptionFlag: return "partialReceptionFlag";
    case streamIdList: return "streamIdList";
    case dvbtCellIds: return "dvbtCellIds";
    case allPlpInfo: return "allPlpInfo";
    case iptvContentUrl: return "iptvContentUrl";
    case iptvPacketsReceived: return "iptvPacketsReceived";
    case iptvPacketsLost: return "iptvPacketsLost";
    case iptvWorstJitterMs: return "iptvWorstJitterMs";
    case iptvAverageJitterMs: return "iptvAverageJitterMs";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int isDemodLocked = 0;
    public static final int snr = 1;
    public static final int ber = 2;
    public static final int per = 3;
    public static final int preBer = 4;
    public static final int signalQuality = 5;
    public static final int signalStrength = 6;
    public static final int symbolRate = 7;
    public static final int innerFec = 8;
    public static final int modulationStatus = 9;
    public static final int inversion = 10;
    public static final int lnbVoltage = 11;
    public static final int plpId = 12;
    public static final int isEWBS = 13;
    public static final int agc = 14;
    public static final int isLnaOn = 15;
    public static final int isLayerError = 16;
    public static final int mer = 17;
    public static final int freqOffset = 18;
    public static final int hierarchy = 19;
    public static final int isRfLocked = 20;
    public static final int plpInfo = 21;
    public static final int modulations = 22;
    public static final int bers = 23;
    public static final int codeRates = 24;
    public static final int bandwidth = 25;
    public static final int interval = 26;
    public static final int transmissionMode = 27;
    public static final int uec = 28;
    public static final int systemId = 29;
    public static final int interleaving = 30;
    public static final int isdbtSegment = 31;
    public static final int tsDataRate = 32;
    public static final int rollOff = 33;
    public static final int isMiso = 34;
    public static final int isLinear = 35;
    public static final int isShortFrames = 36;
    public static final int isdbtMode = 37;
    public static final int partialReceptionFlag = 38;
    public static final int streamIdList = 39;
    public static final int dvbtCellIds = 40;
    public static final int allPlpInfo = 41;
    public static final int iptvContentUrl = 42;
    public static final int iptvPacketsReceived = 43;
    public static final int iptvPacketsLost = 44;
    public static final int iptvWorstJitterMs = 45;
    public static final int iptvAverageJitterMs = 46;
  }
}
