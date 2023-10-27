/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class SignalStrength implements android.os.Parcelable
{
  public android.hardware.radio.network.GsmSignalStrength gsm;
  public android.hardware.radio.network.CdmaSignalStrength cdma;
  public android.hardware.radio.network.EvdoSignalStrength evdo;
  public android.hardware.radio.network.LteSignalStrength lte;
  public android.hardware.radio.network.TdscdmaSignalStrength tdscdma;
  public android.hardware.radio.network.WcdmaSignalStrength wcdma;
  public android.hardware.radio.network.NrSignalStrength nr;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SignalStrength> CREATOR = new android.os.Parcelable.Creator<SignalStrength>() {
    @Override
    public SignalStrength createFromParcel(android.os.Parcel _aidl_source) {
      SignalStrength _aidl_out = new SignalStrength();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SignalStrength[] newArray(int _aidl_size) {
      return new SignalStrength[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(gsm, _aidl_flag);
    _aidl_parcel.writeTypedObject(cdma, _aidl_flag);
    _aidl_parcel.writeTypedObject(evdo, _aidl_flag);
    _aidl_parcel.writeTypedObject(lte, _aidl_flag);
    _aidl_parcel.writeTypedObject(tdscdma, _aidl_flag);
    _aidl_parcel.writeTypedObject(wcdma, _aidl_flag);
    _aidl_parcel.writeTypedObject(nr, _aidl_flag);
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
      gsm = _aidl_parcel.readTypedObject(android.hardware.radio.network.GsmSignalStrength.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cdma = _aidl_parcel.readTypedObject(android.hardware.radio.network.CdmaSignalStrength.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      evdo = _aidl_parcel.readTypedObject(android.hardware.radio.network.EvdoSignalStrength.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      lte = _aidl_parcel.readTypedObject(android.hardware.radio.network.LteSignalStrength.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tdscdma = _aidl_parcel.readTypedObject(android.hardware.radio.network.TdscdmaSignalStrength.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      wcdma = _aidl_parcel.readTypedObject(android.hardware.radio.network.WcdmaSignalStrength.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      nr = _aidl_parcel.readTypedObject(android.hardware.radio.network.NrSignalStrength.CREATOR);
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
    _aidl_sj.add("gsm: " + (java.util.Objects.toString(gsm)));
    _aidl_sj.add("cdma: " + (java.util.Objects.toString(cdma)));
    _aidl_sj.add("evdo: " + (java.util.Objects.toString(evdo)));
    _aidl_sj.add("lte: " + (java.util.Objects.toString(lte)));
    _aidl_sj.add("tdscdma: " + (java.util.Objects.toString(tdscdma)));
    _aidl_sj.add("wcdma: " + (java.util.Objects.toString(wcdma)));
    _aidl_sj.add("nr: " + (java.util.Objects.toString(nr)));
    return "android.hardware.radio.network.SignalStrength" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(gsm);
    _mask |= describeContents(cdma);
    _mask |= describeContents(evdo);
    _mask |= describeContents(lte);
    _mask |= describeContents(tdscdma);
    _mask |= describeContents(wcdma);
    _mask |= describeContents(nr);
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
