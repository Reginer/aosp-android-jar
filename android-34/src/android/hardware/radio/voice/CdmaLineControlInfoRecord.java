/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class CdmaLineControlInfoRecord implements android.os.Parcelable
{
  public byte lineCtrlPolarityIncluded = 0;
  public byte lineCtrlToggle = 0;
  public byte lineCtrlReverse = 0;
  public byte lineCtrlPowerDenial = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaLineControlInfoRecord> CREATOR = new android.os.Parcelable.Creator<CdmaLineControlInfoRecord>() {
    @Override
    public CdmaLineControlInfoRecord createFromParcel(android.os.Parcel _aidl_source) {
      CdmaLineControlInfoRecord _aidl_out = new CdmaLineControlInfoRecord();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaLineControlInfoRecord[] newArray(int _aidl_size) {
      return new CdmaLineControlInfoRecord[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(lineCtrlPolarityIncluded);
    _aidl_parcel.writeByte(lineCtrlToggle);
    _aidl_parcel.writeByte(lineCtrlReverse);
    _aidl_parcel.writeByte(lineCtrlPowerDenial);
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
      lineCtrlPolarityIncluded = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lineCtrlToggle = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lineCtrlReverse = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lineCtrlPowerDenial = _aidl_parcel.readByte();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("lineCtrlPolarityIncluded: " + (lineCtrlPolarityIncluded));
    _aidl_sj.add("lineCtrlToggle: " + (lineCtrlToggle));
    _aidl_sj.add("lineCtrlReverse: " + (lineCtrlReverse));
    _aidl_sj.add("lineCtrlPowerDenial: " + (lineCtrlPowerDenial));
    return "android.hardware.radio.voice.CdmaLineControlInfoRecord" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
