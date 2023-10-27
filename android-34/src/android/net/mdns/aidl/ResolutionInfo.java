/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.net.mdns.aidl;
/** @hide */
public class ResolutionInfo implements android.os.Parcelable
{
  public final int id;
  public final int result;
  public final java.lang.String serviceName;
  public final java.lang.String registrationType;
  public final java.lang.String domain;
  public final java.lang.String serviceFullName;
  public final java.lang.String hostname;
  public final int port;
  public final byte[] txtRecord;
  public final int interfaceIdx;
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
    private java.lang.String domain;
    public Builder setDomain(java.lang.String domain) {
      this.domain = domain;
      return this;
    }
    private java.lang.String serviceFullName;
    public Builder setServiceFullName(java.lang.String serviceFullName) {
      this.serviceFullName = serviceFullName;
      return this;
    }
    private java.lang.String hostname;
    public Builder setHostname(java.lang.String hostname) {
      this.hostname = hostname;
      return this;
    }
    private int port = 0;
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }
    private byte[] txtRecord;
    public Builder setTxtRecord(byte[] txtRecord) {
      this.txtRecord = txtRecord;
      return this;
    }
    private int interfaceIdx = 0;
    public Builder setInterfaceIdx(int interfaceIdx) {
      this.interfaceIdx = interfaceIdx;
      return this;
    }
    public android.net.mdns.aidl.ResolutionInfo build() {
      return new android.net.mdns.aidl.ResolutionInfo(id, result, serviceName, registrationType, domain, serviceFullName, hostname, port, txtRecord, interfaceIdx);
    }
  }
  public static final android.os.Parcelable.Creator<ResolutionInfo> CREATOR = new android.os.Parcelable.Creator<ResolutionInfo>() {
    @Override
    public ResolutionInfo createFromParcel(android.os.Parcel _aidl_source) {
      return internalCreateFromParcel(_aidl_source);
    }
    @Override
    public ResolutionInfo[] newArray(int _aidl_size) {
      return new ResolutionInfo[_aidl_size];
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
    _aidl_parcel.writeString(domain);
    _aidl_parcel.writeString(serviceFullName);
    _aidl_parcel.writeString(hostname);
    _aidl_parcel.writeInt(port);
    _aidl_parcel.writeByteArray(txtRecord);
    _aidl_parcel.writeInt(interfaceIdx);
    int _aidl_end_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.setDataPosition(_aidl_start_pos);
    _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
    _aidl_parcel.setDataPosition(_aidl_end_pos);
  }
  public ResolutionInfo(int id, int result, java.lang.String serviceName, java.lang.String registrationType, java.lang.String domain, java.lang.String serviceFullName, java.lang.String hostname, int port, byte[] txtRecord, int interfaceIdx)
  {
    this.id = id;
    this.result = result;
    this.serviceName = serviceName;
    this.registrationType = registrationType;
    this.domain = domain;
    this.serviceFullName = serviceFullName;
    this.hostname = hostname;
    this.port = port;
    this.txtRecord = txtRecord;
    this.interfaceIdx = interfaceIdx;
  }
  private static ResolutionInfo internalCreateFromParcel(android.os.Parcel _aidl_parcel)
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
      java.lang.String _aidl_temp_domain;
      _aidl_temp_domain = _aidl_parcel.readString();
      _aidl_parcelable_builder.setDomain(_aidl_temp_domain);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      java.lang.String _aidl_temp_serviceFullName;
      _aidl_temp_serviceFullName = _aidl_parcel.readString();
      _aidl_parcelable_builder.setServiceFullName(_aidl_temp_serviceFullName);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      java.lang.String _aidl_temp_hostname;
      _aidl_temp_hostname = _aidl_parcel.readString();
      _aidl_parcelable_builder.setHostname(_aidl_temp_hostname);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      int _aidl_temp_port;
      _aidl_temp_port = _aidl_parcel.readInt();
      _aidl_parcelable_builder.setPort(_aidl_temp_port);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      byte[] _aidl_temp_txtRecord;
      _aidl_temp_txtRecord = _aidl_parcel.createByteArray();
      _aidl_parcelable_builder.setTxtRecord(_aidl_temp_txtRecord);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return _aidl_parcelable_builder.build();
      int _aidl_temp_interfaceIdx;
      _aidl_temp_interfaceIdx = _aidl_parcel.readInt();
      _aidl_parcelable_builder.setInterfaceIdx(_aidl_temp_interfaceIdx);
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
    _aidl_sj.add("domain: " + (java.util.Objects.toString(domain)));
    _aidl_sj.add("serviceFullName: " + (java.util.Objects.toString(serviceFullName)));
    _aidl_sj.add("hostname: " + (java.util.Objects.toString(hostname)));
    _aidl_sj.add("port: " + (port));
    _aidl_sj.add("txtRecord: " + (java.util.Arrays.toString(txtRecord)));
    _aidl_sj.add("interfaceIdx: " + (interfaceIdx));
    return "android.net.mdns.aidl.ResolutionInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof ResolutionInfo)) return false;
    ResolutionInfo that = (ResolutionInfo)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(result, that.result)) return false;
    if (!java.util.Objects.deepEquals(serviceName, that.serviceName)) return false;
    if (!java.util.Objects.deepEquals(registrationType, that.registrationType)) return false;
    if (!java.util.Objects.deepEquals(domain, that.domain)) return false;
    if (!java.util.Objects.deepEquals(serviceFullName, that.serviceFullName)) return false;
    if (!java.util.Objects.deepEquals(hostname, that.hostname)) return false;
    if (!java.util.Objects.deepEquals(port, that.port)) return false;
    if (!java.util.Objects.deepEquals(txtRecord, that.txtRecord)) return false;
    if (!java.util.Objects.deepEquals(interfaceIdx, that.interfaceIdx)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, result, serviceName, registrationType, domain, serviceFullName, hostname, port, txtRecord, interfaceIdx).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
