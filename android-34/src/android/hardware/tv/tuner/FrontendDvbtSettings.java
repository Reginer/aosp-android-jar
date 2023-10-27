/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class FrontendDvbtSettings implements android.os.Parcelable
{
  public long frequency = 0L;
  public long endFrequency = 0L;
  public int inversion = android.hardware.tv.tuner.FrontendSpectralInversion.UNDEFINED;
  public int transmissionMode = android.hardware.tv.tuner.FrontendDvbtTransmissionMode.UNDEFINED;
  public int bandwidth = android.hardware.tv.tuner.FrontendDvbtBandwidth.UNDEFINED;
  public int constellation = android.hardware.tv.tuner.FrontendDvbtConstellation.UNDEFINED;
  public int hierarchy = android.hardware.tv.tuner.FrontendDvbtHierarchy.UNDEFINED;
  public int hpCoderate = android.hardware.tv.tuner.FrontendDvbtCoderate.UNDEFINED;
  public int lpCoderate = android.hardware.tv.tuner.FrontendDvbtCoderate.UNDEFINED;
  public int guardInterval = android.hardware.tv.tuner.FrontendDvbtGuardInterval.UNDEFINED;
  public boolean isHighPriority = false;
  public byte standard = android.hardware.tv.tuner.FrontendDvbtStandard.UNDEFINED;
  public boolean isMiso = false;
  public int plpMode = android.hardware.tv.tuner.FrontendDvbtPlpMode.UNDEFINED;
  public int plpId = 0;
  public int plpGroupId = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<FrontendDvbtSettings> CREATOR = new android.os.Parcelable.Creator<FrontendDvbtSettings>() {
    @Override
    public FrontendDvbtSettings createFromParcel(android.os.Parcel _aidl_source) {
      FrontendDvbtSettings _aidl_out = new FrontendDvbtSettings();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public FrontendDvbtSettings[] newArray(int _aidl_size) {
      return new FrontendDvbtSettings[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(frequency);
    _aidl_parcel.writeLong(endFrequency);
    _aidl_parcel.writeInt(inversion);
    _aidl_parcel.writeInt(transmissionMode);
    _aidl_parcel.writeInt(bandwidth);
    _aidl_parcel.writeInt(constellation);
    _aidl_parcel.writeInt(hierarchy);
    _aidl_parcel.writeInt(hpCoderate);
    _aidl_parcel.writeInt(lpCoderate);
    _aidl_parcel.writeInt(guardInterval);
    _aidl_parcel.writeBoolean(isHighPriority);
    _aidl_parcel.writeByte(standard);
    _aidl_parcel.writeBoolean(isMiso);
    _aidl_parcel.writeInt(plpMode);
    _aidl_parcel.writeInt(plpId);
    _aidl_parcel.writeInt(plpGroupId);
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
      frequency = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      endFrequency = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      inversion = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      transmissionMode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      bandwidth = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      constellation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hierarchy = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hpCoderate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lpCoderate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      guardInterval = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isHighPriority = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      standard = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isMiso = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      plpMode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      plpId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      plpGroupId = _aidl_parcel.readInt();
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
    return _mask;
  }
}
