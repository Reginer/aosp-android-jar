/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.network;
public class ClosedSubscriberGroupInfo implements android.os.Parcelable
{
  public boolean csgIndication = false;
  public java.lang.String homeNodebName;
  public int csgIdentity = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ClosedSubscriberGroupInfo> CREATOR = new android.os.Parcelable.Creator<ClosedSubscriberGroupInfo>() {
    @Override
    public ClosedSubscriberGroupInfo createFromParcel(android.os.Parcel _aidl_source) {
      ClosedSubscriberGroupInfo _aidl_out = new ClosedSubscriberGroupInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ClosedSubscriberGroupInfo[] newArray(int _aidl_size) {
      return new ClosedSubscriberGroupInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeBoolean(csgIndication);
    _aidl_parcel.writeString(homeNodebName);
    _aidl_parcel.writeInt(csgIdentity);
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
      csgIndication = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      homeNodebName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      csgIdentity = _aidl_parcel.readInt();
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
    _aidl_sj.add("csgIndication: " + (csgIndication));
    _aidl_sj.add("homeNodebName: " + (java.util.Objects.toString(homeNodebName)));
    _aidl_sj.add("csgIdentity: " + (csgIdentity));
    return "android.hardware.radio.network.ClosedSubscriberGroupInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
