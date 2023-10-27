/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioMMapPolicyInfo implements android.os.Parcelable
{
  public android.media.audio.common.AudioDevice device;
  public int mmapPolicy = android.media.audio.common.AudioMMapPolicy.UNSPECIFIED;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioMMapPolicyInfo> CREATOR = new android.os.Parcelable.Creator<AudioMMapPolicyInfo>() {
    @Override
    public AudioMMapPolicyInfo createFromParcel(android.os.Parcel _aidl_source) {
      AudioMMapPolicyInfo _aidl_out = new AudioMMapPolicyInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioMMapPolicyInfo[] newArray(int _aidl_size) {
      return new AudioMMapPolicyInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(device, _aidl_flag);
    _aidl_parcel.writeInt(mmapPolicy);
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
      device = _aidl_parcel.readTypedObject(android.media.audio.common.AudioDevice.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mmapPolicy = _aidl_parcel.readInt();
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
    _aidl_sj.add("device: " + (java.util.Objects.toString(device)));
    _aidl_sj.add("mmapPolicy: " + (mmapPolicy));
    return "android.media.audio.common.AudioMMapPolicyInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioMMapPolicyInfo)) return false;
    AudioMMapPolicyInfo that = (AudioMMapPolicyInfo)other;
    if (!java.util.Objects.deepEquals(device, that.device)) return false;
    if (!java.util.Objects.deepEquals(mmapPolicy, that.mmapPolicy)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(device, mmapPolicy).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(device);
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
