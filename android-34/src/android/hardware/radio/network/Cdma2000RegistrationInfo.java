/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class Cdma2000RegistrationInfo implements android.os.Parcelable
{
  public boolean cssSupported = false;
  public int roamingIndicator = 0;
  public int systemIsInPrl = 0;
  public int defaultRoamingIndicator = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Cdma2000RegistrationInfo> CREATOR = new android.os.Parcelable.Creator<Cdma2000RegistrationInfo>() {
    @Override
    public Cdma2000RegistrationInfo createFromParcel(android.os.Parcel _aidl_source) {
      Cdma2000RegistrationInfo _aidl_out = new Cdma2000RegistrationInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Cdma2000RegistrationInfo[] newArray(int _aidl_size) {
      return new Cdma2000RegistrationInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(cssSupported);
    _aidl_parcel.writeInt(roamingIndicator);
    _aidl_parcel.writeInt(systemIsInPrl);
    _aidl_parcel.writeInt(defaultRoamingIndicator);
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
      cssSupported = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      roamingIndicator = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      systemIsInPrl = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      defaultRoamingIndicator = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int PRL_INDICATOR_NOT_REGISTERED = -1;
  public static final int PRL_INDICATOR_NOT_IN_PRL = 0;
  public static final int PRL_INDICATOR_IN_PRL = 1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("cssSupported: " + (cssSupported));
    _aidl_sj.add("roamingIndicator: " + (roamingIndicator));
    _aidl_sj.add("systemIsInPrl: " + (systemIsInPrl));
    _aidl_sj.add("defaultRoamingIndicator: " + (defaultRoamingIndicator));
    return "android.hardware.radio.network.Cdma2000RegistrationInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
