/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.messaging;
public class GsmBroadcastSmsConfigInfo implements android.os.Parcelable
{
  public int fromServiceId = 0;
  public int toServiceId = 0;
  public int fromCodeScheme = 0;
  public int toCodeScheme = 0;
  public boolean selected = false;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<GsmBroadcastSmsConfigInfo> CREATOR = new android.os.Parcelable.Creator<GsmBroadcastSmsConfigInfo>() {
    @Override
    public GsmBroadcastSmsConfigInfo createFromParcel(android.os.Parcel _aidl_source) {
      GsmBroadcastSmsConfigInfo _aidl_out = new GsmBroadcastSmsConfigInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public GsmBroadcastSmsConfigInfo[] newArray(int _aidl_size) {
      return new GsmBroadcastSmsConfigInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(fromServiceId);
    _aidl_parcel.writeInt(toServiceId);
    _aidl_parcel.writeInt(fromCodeScheme);
    _aidl_parcel.writeInt(toCodeScheme);
    _aidl_parcel.writeBoolean(selected);
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
      fromServiceId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      toServiceId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fromCodeScheme = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      toCodeScheme = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      selected = _aidl_parcel.readBoolean();
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
    _aidl_sj.add("fromServiceId: " + (fromServiceId));
    _aidl_sj.add("toServiceId: " + (toServiceId));
    _aidl_sj.add("fromCodeScheme: " + (fromCodeScheme));
    _aidl_sj.add("toCodeScheme: " + (toCodeScheme));
    _aidl_sj.add("selected: " + (selected));
    return "android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
