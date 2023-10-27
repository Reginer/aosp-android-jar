/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioUuid implements android.os.Parcelable
{
  public int timeLow = 0;
  public int timeMid = 0;
  public int timeHiAndVersion = 0;
  public int clockSeq = 0;
  public byte[] node;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioUuid> CREATOR = new android.os.Parcelable.Creator<AudioUuid>() {
    @Override
    public AudioUuid createFromParcel(android.os.Parcel _aidl_source) {
      AudioUuid _aidl_out = new AudioUuid();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioUuid[] newArray(int _aidl_size) {
      return new AudioUuid[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(timeLow);
    _aidl_parcel.writeInt(timeMid);
    _aidl_parcel.writeInt(timeHiAndVersion);
    _aidl_parcel.writeInt(clockSeq);
    _aidl_parcel.writeByteArray(node);
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
      timeLow = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeMid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeHiAndVersion = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      clockSeq = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      node = _aidl_parcel.createByteArray();
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
    _aidl_sj.add("timeLow: " + (timeLow));
    _aidl_sj.add("timeMid: " + (timeMid));
    _aidl_sj.add("timeHiAndVersion: " + (timeHiAndVersion));
    _aidl_sj.add("clockSeq: " + (clockSeq));
    _aidl_sj.add("node: " + (java.util.Arrays.toString(node)));
    return "android.media.audio.common.AudioUuid" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioUuid)) return false;
    AudioUuid that = (AudioUuid)other;
    if (!java.util.Objects.deepEquals(timeLow, that.timeLow)) return false;
    if (!java.util.Objects.deepEquals(timeMid, that.timeMid)) return false;
    if (!java.util.Objects.deepEquals(timeHiAndVersion, that.timeHiAndVersion)) return false;
    if (!java.util.Objects.deepEquals(clockSeq, that.clockSeq)) return false;
    if (!java.util.Objects.deepEquals(node, that.node)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(timeLow, timeMid, timeHiAndVersion, clockSeq, node).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
