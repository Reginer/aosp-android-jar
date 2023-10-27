/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
/** @hide */
public final class AudioDeviceAddress implements android.os.Parcelable {
  // tags for union fields
  public final static int id = 0;  // String id;
  public final static int mac = 1;  // byte[] mac;
  public final static int ipv4 = 2;  // byte[] ipv4;
  public final static int ipv6 = 3;  // int[] ipv6;
  public final static int alsa = 4;  // int[] alsa;

  private int _tag;
  private Object _value;

  public AudioDeviceAddress() {
    java.lang.String _value = null;
    this._tag = id;
    this._value = _value;
  }

  private AudioDeviceAddress(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioDeviceAddress(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // String id;

  public static AudioDeviceAddress id(java.lang.String _value) {
    return new AudioDeviceAddress(id, _value);
  }

  public java.lang.String getId() {
    _assertTag(id);
    return (java.lang.String) _value;
  }

  public void setId(java.lang.String _value) {
    _set(id, _value);
  }

  // byte[] mac;

  public static AudioDeviceAddress mac(byte[] _value) {
    return new AudioDeviceAddress(mac, _value);
  }

  public byte[] getMac() {
    _assertTag(mac);
    return (byte[]) _value;
  }

  public void setMac(byte[] _value) {
    _set(mac, _value);
  }

  // byte[] ipv4;

  public static AudioDeviceAddress ipv4(byte[] _value) {
    return new AudioDeviceAddress(ipv4, _value);
  }

  public byte[] getIpv4() {
    _assertTag(ipv4);
    return (byte[]) _value;
  }

  public void setIpv4(byte[] _value) {
    _set(ipv4, _value);
  }

  // int[] ipv6;

  public static AudioDeviceAddress ipv6(int[] _value) {
    return new AudioDeviceAddress(ipv6, _value);
  }

  public int[] getIpv6() {
    _assertTag(ipv6);
    return (int[]) _value;
  }

  public void setIpv6(int[] _value) {
    _set(ipv6, _value);
  }

  // int[] alsa;

  public static AudioDeviceAddress alsa(int[] _value) {
    return new AudioDeviceAddress(alsa, _value);
  }

  public int[] getAlsa() {
    _assertTag(alsa);
    return (int[]) _value;
  }

  public void setAlsa(int[] _value) {
    _set(alsa, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AudioDeviceAddress> CREATOR = new android.os.Parcelable.Creator<AudioDeviceAddress>() {
    @Override
    public AudioDeviceAddress createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioDeviceAddress(_aidl_source);
    }
    @Override
    public AudioDeviceAddress[] newArray(int _aidl_size) {
      return new AudioDeviceAddress[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case id:
      _aidl_parcel.writeString(getId());
      break;
    case mac:
      _aidl_parcel.writeByteArray(getMac());
      break;
    case ipv4:
      _aidl_parcel.writeByteArray(getIpv4());
      break;
    case ipv6:
      _aidl_parcel.writeIntArray(getIpv6());
      break;
    case alsa:
      _aidl_parcel.writeIntArray(getAlsa());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case id: {
      java.lang.String _aidl_value;
      _aidl_value = _aidl_parcel.readString();
      _set(_aidl_tag, _aidl_value);
      return; }
    case mac: {
      byte[] _aidl_value;
      _aidl_value = _aidl_parcel.createByteArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case ipv4: {
      byte[] _aidl_value;
      _aidl_value = _aidl_parcel.createByteArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case ipv6: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    case alsa: {
      int[] _aidl_value;
      _aidl_value = _aidl_parcel.createIntArray();
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    }
    return _mask;
  }

  @Override
  public String toString() {
    switch (_tag) {
    case id: return "android.media.audio.common.AudioDeviceAddress.id(" + (java.util.Objects.toString(getId())) + ")";
    case mac: return "android.media.audio.common.AudioDeviceAddress.mac(" + (java.util.Arrays.toString(getMac())) + ")";
    case ipv4: return "android.media.audio.common.AudioDeviceAddress.ipv4(" + (java.util.Arrays.toString(getIpv4())) + ")";
    case ipv6: return "android.media.audio.common.AudioDeviceAddress.ipv6(" + (java.util.Arrays.toString(getIpv6())) + ")";
    case alsa: return "android.media.audio.common.AudioDeviceAddress.alsa(" + (java.util.Arrays.toString(getAlsa())) + ")";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof AudioDeviceAddress)) return false;
    AudioDeviceAddress that = (AudioDeviceAddress)other;
    if (_tag != that._tag) return false;
    if (!java.util.Objects.deepEquals(_value, that._value)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(_tag, _value).toArray());
  }

  private void _assertTag(int tag) {
    if (getTag() != tag) {
      throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
    }
  }

  private String _tagString(int _tag) {
    switch (_tag) {
    case id: return "id";
    case mac: return "mac";
    case ipv4: return "ipv4";
    case ipv6: return "ipv6";
    case alsa: return "alsa";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int id = 0;
    public static final int mac = 1;
    public static final int ipv4 = 2;
    public static final int ipv6 = 3;
    public static final int alsa = 4;
  }
}
