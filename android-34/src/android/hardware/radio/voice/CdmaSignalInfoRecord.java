/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class CdmaSignalInfoRecord implements android.os.Parcelable
{
  public boolean isPresent = false;
  public byte signalType = 0;
  public byte alertPitch = 0;
  public byte signal = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaSignalInfoRecord> CREATOR = new android.os.Parcelable.Creator<CdmaSignalInfoRecord>() {
    @Override
    public CdmaSignalInfoRecord createFromParcel(android.os.Parcel _aidl_source) {
      CdmaSignalInfoRecord _aidl_out = new CdmaSignalInfoRecord();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaSignalInfoRecord[] newArray(int _aidl_size) {
      return new CdmaSignalInfoRecord[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(isPresent);
    _aidl_parcel.writeByte(signalType);
    _aidl_parcel.writeByte(alertPitch);
    _aidl_parcel.writeByte(signal);
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
      isPresent = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      signalType = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      alertPitch = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      signal = _aidl_parcel.readByte();
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
    _aidl_sj.add("isPresent: " + (isPresent));
    _aidl_sj.add("signalType: " + (signalType));
    _aidl_sj.add("alertPitch: " + (alertPitch));
    _aidl_sj.add("signal: " + (signal));
    return "android.hardware.radio.voice.CdmaSignalInfoRecord" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
