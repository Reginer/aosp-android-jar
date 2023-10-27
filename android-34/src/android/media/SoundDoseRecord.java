/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** Record containing information about the computed sound dose. */
public class SoundDoseRecord implements android.os.Parcelable
{
  /**
   * Corresponds to the time in seconds when the CSD value is calculated from.
   * Values should be consistent and referenced from the same clock (e.g.: monotonic)
   */
  public long timestamp = 0L;
  /** Corresponds to the duration that leads to the CSD value. */
  public int duration = 0;
  /** The actual contribution to the CSD computation normalized: 1.f is 100%CSD. */
  public float value = 0.000000f;
  /** The average MEL value in this time frame that lead to this CSD value. */
  public float averageMel = 0.000000f;
  public static final android.os.Parcelable.Creator<SoundDoseRecord> CREATOR = new android.os.Parcelable.Creator<SoundDoseRecord>() {
    @Override
    public SoundDoseRecord createFromParcel(android.os.Parcel _aidl_source) {
      SoundDoseRecord _aidl_out = new SoundDoseRecord();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SoundDoseRecord[] newArray(int _aidl_size) {
      return new SoundDoseRecord[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(timestamp);
    _aidl_parcel.writeInt(duration);
    _aidl_parcel.writeFloat(value);
    _aidl_parcel.writeFloat(averageMel);
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
      timestamp = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      duration = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      value = _aidl_parcel.readFloat();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      averageMel = _aidl_parcel.readFloat();
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
    _aidl_sj.add("timestamp: " + (timestamp));
    _aidl_sj.add("duration: " + (duration));
    _aidl_sj.add("value: " + (value));
    _aidl_sj.add("averageMel: " + (averageMel));
    return "android.media.SoundDoseRecord" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
