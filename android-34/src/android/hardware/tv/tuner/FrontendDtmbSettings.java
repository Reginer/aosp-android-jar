/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class FrontendDtmbSettings implements android.os.Parcelable
{
  public long frequency = 0L;
  public long endFrequency = 0L;
  public int inversion = android.hardware.tv.tuner.FrontendSpectralInversion.UNDEFINED;
  public int transmissionMode = android.hardware.tv.tuner.FrontendDtmbTransmissionMode.UNDEFINED;
  public int bandwidth = android.hardware.tv.tuner.FrontendDtmbBandwidth.UNDEFINED;
  public int modulation = android.hardware.tv.tuner.FrontendDtmbModulation.UNDEFINED;
  public int codeRate = android.hardware.tv.tuner.FrontendDtmbCodeRate.UNDEFINED;
  public int guardInterval = android.hardware.tv.tuner.FrontendDtmbGuardInterval.UNDEFINED;
  public int interleaveMode = android.hardware.tv.tuner.FrontendDtmbTimeInterleaveMode.UNDEFINED;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<FrontendDtmbSettings> CREATOR = new android.os.Parcelable.Creator<FrontendDtmbSettings>() {
    @Override
    public FrontendDtmbSettings createFromParcel(android.os.Parcel _aidl_source) {
      FrontendDtmbSettings _aidl_out = new FrontendDtmbSettings();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public FrontendDtmbSettings[] newArray(int _aidl_size) {
      return new FrontendDtmbSettings[_aidl_size];
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
    _aidl_parcel.writeInt(modulation);
    _aidl_parcel.writeInt(codeRate);
    _aidl_parcel.writeInt(guardInterval);
    _aidl_parcel.writeInt(interleaveMode);
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
      modulation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      codeRate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      guardInterval = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      interleaveMode = _aidl_parcel.readInt();
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
