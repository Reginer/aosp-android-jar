/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public class QosFilter implements android.os.Parcelable
{
  public java.lang.String[] localAddresses;
  public java.lang.String[] remoteAddresses;
  public android.hardware.radio.data.PortRange localPort;
  public android.hardware.radio.data.PortRange remotePort;
  public byte protocol = 0;
  public android.hardware.radio.data.QosFilterTypeOfService tos;
  public android.hardware.radio.data.QosFilterIpv6FlowLabel flowLabel;
  public android.hardware.radio.data.QosFilterIpsecSpi spi;
  public byte direction = 0;
  public int precedence = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<QosFilter> CREATOR = new android.os.Parcelable.Creator<QosFilter>() {
    @Override
    public QosFilter createFromParcel(android.os.Parcel _aidl_source) {
      QosFilter _aidl_out = new QosFilter();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public QosFilter[] newArray(int _aidl_size) {
      return new QosFilter[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeStringArray(localAddresses);
    _aidl_parcel.writeStringArray(remoteAddresses);
    _aidl_parcel.writeTypedObject(localPort, _aidl_flag);
    _aidl_parcel.writeTypedObject(remotePort, _aidl_flag);
    _aidl_parcel.writeByte(protocol);
    _aidl_parcel.writeTypedObject(tos, _aidl_flag);
    _aidl_parcel.writeTypedObject(flowLabel, _aidl_flag);
    _aidl_parcel.writeTypedObject(spi, _aidl_flag);
    _aidl_parcel.writeByte(direction);
    _aidl_parcel.writeInt(precedence);
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
      localAddresses = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      remoteAddresses = _aidl_parcel.createStringArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      localPort = _aidl_parcel.readTypedObject(android.hardware.radio.data.PortRange.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      remotePort = _aidl_parcel.readTypedObject(android.hardware.radio.data.PortRange.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      protocol = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tos = _aidl_parcel.readTypedObject(android.hardware.radio.data.QosFilterTypeOfService.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flowLabel = _aidl_parcel.readTypedObject(android.hardware.radio.data.QosFilterIpv6FlowLabel.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      spi = _aidl_parcel.readTypedObject(android.hardware.radio.data.QosFilterIpsecSpi.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      direction = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      precedence = _aidl_parcel.readInt();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final byte DIRECTION_DOWNLINK = 0;
  public static final byte DIRECTION_UPLINK = 1;
  public static final byte DIRECTION_BIDIRECTIONAL = 2;
  public static final byte PROTOCOL_UNSPECIFIED = -1;
  public static final byte PROTOCOL_TCP = 6;
  public static final byte PROTOCOL_UDP = 17;
  public static final byte PROTOCOL_ESP = 50;
  public static final byte PROTOCOL_AH = 51;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("localAddresses: " + (java.util.Arrays.toString(localAddresses)));
    _aidl_sj.add("remoteAddresses: " + (java.util.Arrays.toString(remoteAddresses)));
    _aidl_sj.add("localPort: " + (java.util.Objects.toString(localPort)));
    _aidl_sj.add("remotePort: " + (java.util.Objects.toString(remotePort)));
    _aidl_sj.add("protocol: " + (protocol));
    _aidl_sj.add("tos: " + (java.util.Objects.toString(tos)));
    _aidl_sj.add("flowLabel: " + (java.util.Objects.toString(flowLabel)));
    _aidl_sj.add("spi: " + (java.util.Objects.toString(spi)));
    _aidl_sj.add("direction: " + (direction));
    _aidl_sj.add("precedence: " + (precedence));
    return "android.hardware.radio.data.QosFilter" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(localPort);
    _mask |= describeContents(remotePort);
    _mask |= describeContents(tos);
    _mask |= describeContents(flowLabel);
    _mask |= describeContents(spi);
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
