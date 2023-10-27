/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class MicrophoneDynamicInfo implements android.os.Parcelable
{
  public java.lang.String id;
  public int[] channelMapping;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<MicrophoneDynamicInfo> CREATOR = new android.os.Parcelable.Creator<MicrophoneDynamicInfo>() {
    @Override
    public MicrophoneDynamicInfo createFromParcel(android.os.Parcel _aidl_source) {
      MicrophoneDynamicInfo _aidl_out = new MicrophoneDynamicInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public MicrophoneDynamicInfo[] newArray(int _aidl_size) {
      return new MicrophoneDynamicInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(id);
    _aidl_parcel.writeIntArray(channelMapping);
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
      id = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      channelMapping = _aidl_parcel.createIntArray();
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
    _aidl_sj.add("id: " + (java.util.Objects.toString(id)));
    _aidl_sj.add("channelMapping: " + (java.util.Arrays.toString(channelMapping)));
    return "android.media.audio.common.MicrophoneDynamicInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof MicrophoneDynamicInfo)) return false;
    MicrophoneDynamicInfo that = (MicrophoneDynamicInfo)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(channelMapping, that.channelMapping)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, channelMapping).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
  public static @interface ChannelMapping {
    public static final int UNUSED = 0;
    public static final int DIRECT = 1;
    public static final int PROCESSED = 2;
  }
}
