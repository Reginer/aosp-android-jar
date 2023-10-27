/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.modem;
public class ActivityStatsInfo implements android.os.Parcelable
{
  public int sleepModeTimeMs = 0;
  public int idleModeTimeMs = 0;
  public android.hardware.radio.modem.ActivityStatsTechSpecificInfo[] techSpecificInfo;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ActivityStatsInfo> CREATOR = new android.os.Parcelable.Creator<ActivityStatsInfo>() {
    @Override
    public ActivityStatsInfo createFromParcel(android.os.Parcel _aidl_source) {
      ActivityStatsInfo _aidl_out = new ActivityStatsInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ActivityStatsInfo[] newArray(int _aidl_size) {
      return new ActivityStatsInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(sleepModeTimeMs);
    _aidl_parcel.writeInt(idleModeTimeMs);
    _aidl_parcel.writeTypedArray(techSpecificInfo, _aidl_flag);
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
      sleepModeTimeMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      idleModeTimeMs = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      techSpecificInfo = _aidl_parcel.createTypedArray(android.hardware.radio.modem.ActivityStatsTechSpecificInfo.CREATOR);
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
    _aidl_sj.add("sleepModeTimeMs: " + (sleepModeTimeMs));
    _aidl_sj.add("idleModeTimeMs: " + (idleModeTimeMs));
    _aidl_sj.add("techSpecificInfo: " + (java.util.Arrays.toString(techSpecificInfo)));
    return "android.hardware.radio.modem.ActivityStatsInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(techSpecificInfo);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof Object[]) {
      int _mask = 0;
      for (Object o : (Object[]) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
