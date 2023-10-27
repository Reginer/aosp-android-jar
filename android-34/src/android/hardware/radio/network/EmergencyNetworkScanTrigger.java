/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class EmergencyNetworkScanTrigger implements android.os.Parcelable
{
  public int[] accessNetwork;
  public int scanType;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<EmergencyNetworkScanTrigger> CREATOR = new android.os.Parcelable.Creator<EmergencyNetworkScanTrigger>() {
    @Override
    public EmergencyNetworkScanTrigger createFromParcel(android.os.Parcel _aidl_source) {
      EmergencyNetworkScanTrigger _aidl_out = new EmergencyNetworkScanTrigger();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public EmergencyNetworkScanTrigger[] newArray(int _aidl_size) {
      return new EmergencyNetworkScanTrigger[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeIntArray(accessNetwork);
    _aidl_parcel.writeInt(scanType);
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
      accessNetwork = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      scanType = _aidl_parcel.readInt();
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
    _aidl_sj.add("accessNetwork: " + (android.hardware.radio.AccessNetwork.$.arrayToString(accessNetwork)));
    _aidl_sj.add("scanType: " + (android.hardware.radio.network.EmergencyScanType.$.toString(scanType)));
    return "android.hardware.radio.network.EmergencyNetworkScanTrigger" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
