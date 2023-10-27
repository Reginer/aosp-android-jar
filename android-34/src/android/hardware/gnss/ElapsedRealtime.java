/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public class ElapsedRealtime implements android.os.Parcelable
{
  public int flags = 0;
  public long timestampNs = 0L;
  public double timeUncertaintyNs = 0.000000;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ElapsedRealtime> CREATOR = new android.os.Parcelable.Creator<ElapsedRealtime>() {
    @Override
    public ElapsedRealtime createFromParcel(android.os.Parcel _aidl_source) {
      ElapsedRealtime _aidl_out = new ElapsedRealtime();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ElapsedRealtime[] newArray(int _aidl_size) {
      return new ElapsedRealtime[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeLong(timestampNs);
    _aidl_parcel.writeDouble(timeUncertaintyNs);
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
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timestampNs = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeUncertaintyNs = _aidl_parcel.readDouble();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int HAS_TIMESTAMP_NS = 1;
  public static final int HAS_TIME_UNCERTAINTY_NS = 2;
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
