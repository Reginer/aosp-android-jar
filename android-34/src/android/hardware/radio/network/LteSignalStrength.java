/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class LteSignalStrength implements android.os.Parcelable
{
  public int signalStrength = 0;
  public int rsrp = 0;
  public int rsrq = 0;
  public int rssnr = 0;
  public int cqi = 0;
  public int timingAdvance = 0;
  public int cqiTableIndex = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<LteSignalStrength> CREATOR = new android.os.Parcelable.Creator<LteSignalStrength>() {
    @Override
    public LteSignalStrength createFromParcel(android.os.Parcel _aidl_source) {
      LteSignalStrength _aidl_out = new LteSignalStrength();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public LteSignalStrength[] newArray(int _aidl_size) {
      return new LteSignalStrength[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(signalStrength);
    _aidl_parcel.writeInt(rsrp);
    _aidl_parcel.writeInt(rsrq);
    _aidl_parcel.writeInt(rssnr);
    _aidl_parcel.writeInt(cqi);
    _aidl_parcel.writeInt(timingAdvance);
    _aidl_parcel.writeInt(cqiTableIndex);
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
      signalStrength = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rsrp = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rsrq = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rssnr = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cqi = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timingAdvance = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cqiTableIndex = _aidl_parcel.readInt();
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
    _aidl_sj.add("signalStrength: " + (signalStrength));
    _aidl_sj.add("rsrp: " + (rsrp));
    _aidl_sj.add("rsrq: " + (rsrq));
    _aidl_sj.add("rssnr: " + (rssnr));
    _aidl_sj.add("cqi: " + (cqi));
    _aidl_sj.add("timingAdvance: " + (timingAdvance));
    _aidl_sj.add("cqiTableIndex: " + (cqiTableIndex));
    return "android.hardware.radio.network.LteSignalStrength" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
