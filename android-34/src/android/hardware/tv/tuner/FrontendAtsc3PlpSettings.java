/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class FrontendAtsc3PlpSettings implements android.os.Parcelable
{
  public int plpId = 0;
  public int modulation = android.hardware.tv.tuner.FrontendAtsc3Modulation.UNDEFINED;
  public int interleaveMode = android.hardware.tv.tuner.FrontendAtsc3TimeInterleaveMode.UNDEFINED;
  public int codeRate = android.hardware.tv.tuner.FrontendAtsc3CodeRate.UNDEFINED;
  public int fec = android.hardware.tv.tuner.FrontendAtsc3Fec.UNDEFINED;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<FrontendAtsc3PlpSettings> CREATOR = new android.os.Parcelable.Creator<FrontendAtsc3PlpSettings>() {
    @Override
    public FrontendAtsc3PlpSettings createFromParcel(android.os.Parcel _aidl_source) {
      FrontendAtsc3PlpSettings _aidl_out = new FrontendAtsc3PlpSettings();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public FrontendAtsc3PlpSettings[] newArray(int _aidl_size) {
      return new FrontendAtsc3PlpSettings[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(plpId);
    _aidl_parcel.writeInt(modulation);
    _aidl_parcel.writeInt(interleaveMode);
    _aidl_parcel.writeInt(codeRate);
    _aidl_parcel.writeInt(fec);
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
      plpId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      modulation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      interleaveMode = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      codeRate = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fec = _aidl_parcel.readInt();
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
