/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class CdmaNumberInfoRecord implements android.os.Parcelable
{
  public java.lang.String number;
  public byte numberType = 0;
  public byte numberPlan = 0;
  public byte pi = 0;
  public byte si = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<CdmaNumberInfoRecord> CREATOR = new android.os.Parcelable.Creator<CdmaNumberInfoRecord>() {
    @Override
    public CdmaNumberInfoRecord createFromParcel(android.os.Parcel _aidl_source) {
      CdmaNumberInfoRecord _aidl_out = new CdmaNumberInfoRecord();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CdmaNumberInfoRecord[] newArray(int _aidl_size) {
      return new CdmaNumberInfoRecord[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(number);
    _aidl_parcel.writeByte(numberType);
    _aidl_parcel.writeByte(numberPlan);
    _aidl_parcel.writeByte(pi);
    _aidl_parcel.writeByte(si);
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
      number = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberType = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberPlan = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pi = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      si = _aidl_parcel.readByte();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int CDMA_NUMBER_INFO_BUFFER_LENGTH = 81;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("number: " + (java.util.Objects.toString(number)));
    _aidl_sj.add("numberType: " + (numberType));
    _aidl_sj.add("numberPlan: " + (numberPlan));
    _aidl_sj.add("pi: " + (pi));
    _aidl_sj.add("si: " + (si));
    return "android.hardware.radio.voice.CdmaNumberInfoRecord" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
