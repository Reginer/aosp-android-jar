/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.sim;
public class SelectUiccSub implements android.os.Parcelable
{
  public int slot = 0;
  public int appIndex = 0;
  public int subType = 0;
  public int actStatus = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SelectUiccSub> CREATOR = new android.os.Parcelable.Creator<SelectUiccSub>() {
    @Override
    public SelectUiccSub createFromParcel(android.os.Parcel _aidl_source) {
      SelectUiccSub _aidl_out = new SelectUiccSub();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SelectUiccSub[] newArray(int _aidl_size) {
      return new SelectUiccSub[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(slot);
    _aidl_parcel.writeInt(appIndex);
    _aidl_parcel.writeInt(subType);
    _aidl_parcel.writeInt(actStatus);
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
      slot = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      appIndex = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      subType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      actStatus = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int SUBSCRIPTION_TYPE_1 = 0;
  public static final int SUBSCRIPTION_TYPE_2 = 1;
  public static final int SUBSCRIPTION_TYPE_3 = 2;
  public static final int ACT_STATUS_DEACTIVATE = 0;
  public static final int ACT_STATUS_ACTIVATE = 1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("slot: " + (slot));
    _aidl_sj.add("appIndex: " + (appIndex));
    _aidl_sj.add("subType: " + (subType));
    _aidl_sj.add("actStatus: " + (actStatus));
    return "android.hardware.radio.sim.SelectUiccSub" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
