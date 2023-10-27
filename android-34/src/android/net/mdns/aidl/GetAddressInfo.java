/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.net.mdns.aidl;
/** @hide */
public class GetAddressInfo implements android.os.Parcelable
{
  public final int id;
  public final int result;
  public final java.lang.String hostname;
  public final java.lang.String address;
  public final int interfaceIdx;
  public final int netId;
  public static final class Builder
  {
    private int id = 0;
    public Builder setId(int id) {
      this.id = id;
      return this;
    }
    private int result = 0;
    public Builder setResult(int result) {
      this.result = result;
      return this;
    }
    private java.lang.String hostname;
    public Builder setHostname(java.lang.String hostname) {
      this.hostname = hostname;
      return this;
    }
    private java.lang.String address;
    public Builder setAddress(java.lang.String address) {
      this.address = address;
      return this;
    }
    private int interfaceIdx = 0;
    public Builder setInterfaceIdx(int interfaceIdx) {
      this.interfaceIdx = interfaceIdx;
      return this;
    }
    private int netId = 0;
    public Builder setNetId(int netId) {
      this.netId = netId;
      return this;
    }
    public android.net.mdns.aidl.GetAddressInfo build() {
      return new android.net.mdns.aidl.GetAddressInfo(id, result, hostname, address, interfaceIdx, netId);
    }
  }
  public static final android.os.Parcelable.Creator<GetAddressInfo> CREATOR = new android.os.Parcelable.Creator<GetAddressInfo>() {
    @Override
    public GetAddressInfo createFromParcel(android.os.Parcel _aidl_source) {
      return internalCreateFromParcel(_aidl_source);
    }
    @Override
    public GetAddressInfo[] newArray(int _aidl_size) {
      return new GetAddressInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeInt(result);
    _aidl_parcel.writeString(hostname);
    _aidl_parcel.writeString(address);
    _aidl_parcel.writeInt(interfaceIdx);
    _aidl_parcel.writeInt(netId);
    int _aidl_end_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.setDataPosition(_aidl_start_pos);
    _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
    _aidl_parcel.setDataPosition(_aidl_end_pos);
  }
  public GetAddressInfo(int id, int result, java.lang.String hostname, java.lang.String address, int interfaceIdx, int netId)
  {
    this.id = id;
    this.result = result;
    this.hostname = hostname;
    this.address = address;
    this.interfaceIdx = interfaceIdx;
    this.netId = netId;
  }
  private static GetAddressInfo internalCreateFromParcel(android.os.Parcel _aidl_parcel)
  {
    Builder _aidl_parcelable_builder = new Builder();
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    int _aidl_parcelable_size = _aidl_parcel.readInt();
    try {
      if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small"); _aidl_parcelable_builder.build();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      int _aidl_temp_id;
      _aidl_temp_id = _aidl_parcel.readInt();
      _aidl_parcelable_builder.setId(_aidl_temp_id);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      int _aidl_temp_result;
      _aidl_temp_result = _aidl_parcel.readInt();
      _aidl_parcelable_builder.setResult(_aidl_temp_result);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      java.lang.String _aidl_temp_hostname;
      _aidl_temp_hostname = _aidl_parcel.readString();
      _aidl_parcelable_builder.setHostname(_aidl_temp_hostname);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      java.lang.String _aidl_temp_address;
      _aidl_temp_address = _aidl_parcel.readString();
      _aidl_parcelable_builder.setAddress(_aidl_temp_address);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      int _aidl_temp_interfaceIdx;
      _aidl_temp_interfaceIdx = _aidl_parcel.readInt();
      _aidl_parcelable_builder.setInterfaceIdx(_aidl_temp_interfaceIdx);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      int _aidl_temp_netId;
      _aidl_temp_netId = _aidl_parcel.readInt();
      _aidl_parcelable_builder.setNetId(_aidl_temp_netId);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      return _aidl_parcelable_builder.build();
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("id: " + (id));
    _aidl_sj.add("result: " + (result));
    _aidl_sj.add("hostname: " + (java.util.Objects.toString(hostname)));
    _aidl_sj.add("address: " + (java.util.Objects.toString(address)));
    _aidl_sj.add("interfaceIdx: " + (interfaceIdx));
    _aidl_sj.add("netId: " + (netId));
    return "android.net.mdns.aidl.GetAddressInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof GetAddressInfo)) return false;
    GetAddressInfo that = (GetAddressInfo)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(result, that.result)) return false;
    if (!java.util.Objects.deepEquals(hostname, that.hostname)) return false;
    if (!java.util.Objects.deepEquals(address, that.address)) return false;
    if (!java.util.Objects.deepEquals(interfaceIdx, that.interfaceIdx)) return false;
    if (!java.util.Objects.deepEquals(netId, that.netId)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, result, hostname, address, interfaceIdx, netId).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
