/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public class AudioDeviceDescription implements android.os.Parcelable
{
  public int type = android.media.audio.common.AudioDeviceType.NONE;
  public java.lang.String connection;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<AudioDeviceDescription> CREATOR = new android.os.Parcelable.Creator<AudioDeviceDescription>() {
    @Override
    public AudioDeviceDescription createFromParcel(android.os.Parcel _aidl_source) {
      AudioDeviceDescription _aidl_out = new AudioDeviceDescription();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioDeviceDescription[] newArray(int _aidl_size) {
      return new AudioDeviceDescription[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(type);
    _aidl_parcel.writeString(connection);
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
      type = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      connection = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final String CONNECTION_ANALOG = "analog";
  public static final String CONNECTION_BT_A2DP = "bt-a2dp";
  public static final String CONNECTION_BT_LE = "bt-le";
  public static final String CONNECTION_BT_SCO = "bt-sco";
  public static final String CONNECTION_BUS = "bus";
  public static final String CONNECTION_HDMI = "hdmi";
  public static final String CONNECTION_HDMI_ARC = "hdmi-arc";
  public static final String CONNECTION_HDMI_EARC = "hdmi-earc";
  public static final String CONNECTION_IP_V4 = "ip-v4";
  public static final String CONNECTION_SPDIF = "spdif";
  public static final String CONNECTION_WIRELESS = "wireless";
  public static final String CONNECTION_USB = "usb";
  public static final String CONNECTION_VIRTUAL = "virtual";
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("type: " + (type));
    _aidl_sj.add("connection: " + (java.util.Objects.toString(connection)));
    return "android.media.audio.common.AudioDeviceDescription" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioDeviceDescription)) return false;
    AudioDeviceDescription that = (AudioDeviceDescription)other;
    if (!java.util.Objects.deepEquals(type, that.type)) return false;
    if (!java.util.Objects.deepEquals(connection, that.connection)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(type, connection).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
