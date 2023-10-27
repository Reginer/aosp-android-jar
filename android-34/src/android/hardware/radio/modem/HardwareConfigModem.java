/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.modem;
public class HardwareConfigModem implements android.os.Parcelable
{
  public int rilModel = 0;
  public int rat;
  public int maxVoiceCalls = 0;
  public int maxDataCalls = 0;
  public int maxStandby = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<HardwareConfigModem> CREATOR = new android.os.Parcelable.Creator<HardwareConfigModem>() {
    @Override
    public HardwareConfigModem createFromParcel(android.os.Parcel _aidl_source) {
      HardwareConfigModem _aidl_out = new HardwareConfigModem();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public HardwareConfigModem[] newArray(int _aidl_size) {
      return new HardwareConfigModem[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(rilModel);
    _aidl_parcel.writeInt(rat);
    _aidl_parcel.writeInt(maxVoiceCalls);
    _aidl_parcel.writeInt(maxDataCalls);
    _aidl_parcel.writeInt(maxStandby);
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
      rilModel = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      rat = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxVoiceCalls = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxDataCalls = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      maxStandby = _aidl_parcel.readInt();
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
    _aidl_sj.add("rilModel: " + (rilModel));
    _aidl_sj.add("rat: " + (android.hardware.radio.RadioTechnology.$.toString(rat)));
    _aidl_sj.add("maxVoiceCalls: " + (maxVoiceCalls));
    _aidl_sj.add("maxDataCalls: " + (maxDataCalls));
    _aidl_sj.add("maxStandby: " + (maxStandby));
    return "android.hardware.radio.modem.HardwareConfigModem" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
