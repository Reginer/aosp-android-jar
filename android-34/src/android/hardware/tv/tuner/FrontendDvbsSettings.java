/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class FrontendDvbsSettings implements android.os.Parcelable
{
  public long frequency = 0L;
  public long endFrequency = 0L;
  public int inversion = android.hardware.tv.tuner.FrontendSpectralInversion.UNDEFINED;
  public int modulation = android.hardware.tv.tuner.FrontendDvbsModulation.UNDEFINED;
  public android.hardware.tv.tuner.FrontendDvbsCodeRate coderate;
  public int symbolRate = 0;
  public int rolloff = android.hardware.tv.tuner.FrontendDvbsRolloff.UNDEFINED;
  public int pilot = android.hardware.tv.tuner.FrontendDvbsPilot.UNDEFINED;
  public int inputStreamId = 0;
  public byte standard = android.hardware.tv.tuner.FrontendDvbsStandard.UNDEFINED;
  public int vcmMode = android.hardware.tv.tuner.FrontendDvbsVcmMode.UNDEFINED;
  public int scanType = android.hardware.tv.tuner.FrontendDvbsScanType.UNDEFINED;
  public boolean isDiseqcRxMessage = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<FrontendDvbsSettings> CREATOR = new android.os.Parcelable.Creator<FrontendDvbsSettings>() {
    @Override
    public FrontendDvbsSettings createFromParcel(android.os.Parcel _aidl_source) {
      FrontendDvbsSettings _aidl_out = new FrontendDvbsSettings();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public FrontendDvbsSettings[] newArray(int _aidl_size) {
      return new FrontendDvbsSettings[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(frequency);
    _aidl_parcel.writeLong(endFrequency);
    _aidl_parcel.writeInt(inversion);
    _aidl_parcel.writeInt(modulation);
    _aidl_parcel.writeTypedObject(coderate, _aidl_flag);
    _aidl_parcel.writeInt(symbolRate);
    _aidl_parcel.writeInt(rolloff);
    _aidl_parcel.writeInt(pilot);
    _aidl_parcel.writeInt(inputStreamId);
    _aidl_parcel.writeByte(standard);
    _aidl_parcel.writeInt(vcmMode);
    _aidl_parcel.writeInt(scanType);
    _aidl_parcel.writeBoolean(isDiseqcRxMessage);
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
      modulation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      coderate = _aidl_parcel.readTypedObject(android.hardware.tv.tuner.FrontendDvbsCodeRate.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      symbolRate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rolloff = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pilot = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      inputStreamId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      standard = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      vcmMode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      scanType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isDiseqcRxMessage = _aidl_parcel.readBoolean();
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
    _mask |= describeContents(coderate);
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
