/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.net.mdns.aidl;
/** @hide */
public class DiscoveryInfo implements android.os.Parcelable
{
  public final int id;
  public final int result;
  public final java.lang.String serviceName;
  public final java.lang.String registrationType;
  public final java.lang.String domainName;
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
    private java.lang.String serviceName;
    public Builder setServiceName(java.lang.String serviceName) {
      this.serviceName = serviceName;
      return this;
    }
    private java.lang.String registrationType;
    public Builder setRegistrationType(java.lang.String registrationType) {
      this.registrationType = registrationType;
      return this;
    }
    private java.lang.String domainName;
    public Builder setDomainName(java.lang.String domainName) {
      this.domainName = domainName;
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
    public android.net.mdns.aidl.DiscoveryInfo build() {
      return new android.net.mdns.aidl.DiscoveryInfo(id, result, serviceName, registrationType, domainName, interfaceIdx, netId);
    }
  }
  public static final android.os.Parcelable.Creator<DiscoveryInfo> CREATOR = new android.os.Parcelable.Creator<DiscoveryInfo>() {
    @Override
    public DiscoveryInfo createFromParcel(android.os.Parcel _aidl_source) {
      return internalCreateFromParcel(_aidl_source);
    }
    @Override
    public DiscoveryInfo[] newArray(int _aidl_size) {
      return new DiscoveryInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(id);
    _aidl_parcel.writeInt(result);
    _aidl_parcel.writeString(serviceName);
    _aidl_parcel.writeString(registrationType);
    _aidl_parcel.writeString(domainName);
    _aidl_parcel.writeInt(interfaceIdx);
    _aidl_parcel.writeInt(netId);
    int _aidl_end_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.setDataPosition(_aidl_start_pos);
    _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
    _aidl_parcel.setDataPosition(_aidl_end_pos);
  }
  public DiscoveryInfo(int id, int result, java.lang.String serviceName, java.lang.String registrationType, java.lang.String domainName, int interfaceIdx, int netId)
  {
    this.id = id;
    this.result = result;
    this.serviceName = serviceName;
    this.registrationType = registrationType;
    this.domainName = domainName;
    this.interfaceIdx = interfaceIdx;
    this.netId = netId;
  }
  private static DiscoveryInfo internalCreateFromParcel(android.os.Parcel _aidl_parcel)
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
      java.lang.String _aidl_temp_serviceName;
      _aidl_temp_serviceName = _aidl_parcel.readString();
      _aidl_parcelable_builder.setServiceName(_aidl_temp_serviceName);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      java.lang.String _aidl_temp_registrationType;
      _aidl_temp_registrationType = _aidl_parcel.readString();
      _aidl_parcelable_builder.setRegistrationType(_aidl_temp_registrationType);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      java.lang.String _aidl_temp_domainName;
      _aidl_temp_domainName = _aidl_parcel.readString();
      _aidl_parcelable_builder.setDomainName(_aidl_temp_domainName);
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
    _aidl_sj.add("serviceName: " + (java.util.Objects.toString(serviceName)));
    _aidl_sj.add("registrationType: " + (java.util.Objects.toString(registrationType)));
    _aidl_sj.add("domainName: " + (java.util.Objects.toString(domainName)));
    _aidl_sj.add("interfaceIdx: " + (interfaceIdx));
    _aidl_sj.add("netId: " + (netId));
    return "android.net.mdns.aidl.DiscoveryInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof DiscoveryInfo)) return false;
    DiscoveryInfo that = (DiscoveryInfo)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(result, that.result)) return false;
    if (!java.util.Objects.deepEquals(serviceName, that.serviceName)) return false;
    if (!java.util.Objects.deepEquals(registrationType, that.registrationType)) return false;
    if (!java.util.Objects.deepEquals(domainName, that.domainName)) return false;
    if (!java.util.Objects.deepEquals(interfaceIdx, that.interfaceIdx)) return false;
    if (!java.util.Objects.deepEquals(netId, that.netId)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, result, serviceName, registrationType, domainName, interfaceIdx, netId).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
