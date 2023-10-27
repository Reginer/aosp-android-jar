/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.tv.tuner;
/** @hide */
public class FrontendAtsc3Settings implements android.os.Parcelable
{
  public long frequency = 0L;
  public long endFrequency = 0L;
  public int bandwidth = android.hardware.tv.tuner.FrontendAtsc3Bandwidth.UNDEFINED;
  public int inversion = android.hardware.tv.tuner.FrontendSpectralInversion.UNDEFINED;
  public byte demodOutputFormat = android.hardware.tv.tuner.FrontendAtsc3DemodOutputFormat.UNDEFINED;
  public android.hardware.tv.tuner.FrontendAtsc3PlpSettings[] plpSettings;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<FrontendAtsc3Settings> CREATOR = new android.os.Parcelable.Creator<FrontendAtsc3Settings>() {
    @Override
    public FrontendAtsc3Settings createFromParcel(android.os.Parcel _aidl_source) {
      FrontendAtsc3Settings _aidl_out = new FrontendAtsc3Settings();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public FrontendAtsc3Settings[] newArray(int _aidl_size) {
      return new FrontendAtsc3Settings[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(frequency);
    _aidl_parcel.writeLong(endFrequency);
    _aidl_parcel.writeInt(bandwidth);
    _aidl_parcel.writeInt(inversion);
    _aidl_parcel.writeByte(demodOutputFormat);
    _aidl_parcel.writeTypedArray(plpSettings, _aidl_flag);
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
      bandwidth = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      inversion = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      demodOutputFormat = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      plpSettings = _aidl_parcel.createTypedArray(android.hardware.tv.tuner.FrontendAtsc3PlpSettings.CREATOR);
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
    _mask |= describeContents(plpSettings);
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
}
