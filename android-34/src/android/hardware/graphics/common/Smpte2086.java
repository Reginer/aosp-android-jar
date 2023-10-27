/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.graphics.common;
/** @hide */
public class Smpte2086 implements android.os.Parcelable
{
  public android.hardware.graphics.common.XyColor primaryRed;
  public android.hardware.graphics.common.XyColor primaryGreen;
  public android.hardware.graphics.common.XyColor primaryBlue;
  public android.hardware.graphics.common.XyColor whitePoint;
  public float maxLuminance = 0.000000f;
  public float minLuminance = 0.000000f;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Smpte2086> CREATOR = new android.os.Parcelable.Creator<Smpte2086>() {
    @Override
    public Smpte2086 createFromParcel(android.os.Parcel _aidl_source) {
      Smpte2086 _aidl_out = new Smpte2086();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Smpte2086[] newArray(int _aidl_size) {
      return new Smpte2086[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(primaryRed, _aidl_flag);
    _aidl_parcel.writeTypedObject(primaryGreen, _aidl_flag);
    _aidl_parcel.writeTypedObject(primaryBlue, _aidl_flag);
    _aidl_parcel.writeTypedObject(whitePoint, _aidl_flag);
    _aidl_parcel.writeFloat(maxLuminance);
    _aidl_parcel.writeFloat(minLuminance);
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
      primaryRed = _aidl_parcel.readTypedObject(android.hardware.graphics.common.XyColor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      primaryGreen = _aidl_parcel.readTypedObject(android.hardware.graphics.common.XyColor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      primaryBlue = _aidl_parcel.readTypedObject(android.hardware.graphics.common.XyColor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      whitePoint = _aidl_parcel.readTypedObject(android.hardware.graphics.common.XyColor.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxLuminance = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      minLuminance = _aidl_parcel.readFloat();
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
    _mask |= describeContents(primaryRed);
    _mask |= describeContents(primaryGreen);
    _mask |= describeContents(primaryBlue);
    _mask |= describeContents(whitePoint);
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
