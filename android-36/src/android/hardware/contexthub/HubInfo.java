/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash df80fdbb6f95a8a2988bc72b7f08f891847b80eb --stability vintf --min_sdk_version current --ninja -d out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen/android/hardware/contexthub/HubInfo.java.d -o out/soong/.intermediates/hardware/interfaces/contexthub/aidl/android.hardware.contexthub-V4-java-source/gen -Nhardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4 hardware/interfaces/contexthub/aidl/aidl_api/android.hardware.contexthub/4/android/hardware/contexthub/HubInfo.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.contexthub;
public class HubInfo implements android.os.Parcelable
{
  public long hubId = 0L;
  public android.hardware.contexthub.HubInfo.HubDetails hubDetails;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<HubInfo> CREATOR = new android.os.Parcelable.Creator<HubInfo>() {
    @Override
    public HubInfo createFromParcel(android.os.Parcel _aidl_source) {
      HubInfo _aidl_out = new HubInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public HubInfo[] newArray(int _aidl_size) {
      return new HubInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeLong(hubId);
    _aidl_parcel.writeTypedObject(hubDetails, _aidl_flag);
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
      hubId = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      hubDetails = _aidl_parcel.readTypedObject(android.hardware.contexthub.HubInfo.HubDetails.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final long HUB_ID_INVALID = 0L;
  public static final long HUB_ID_RESERVED = -1L;
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(hubDetails);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
  public static final class HubDetails implements android.os.Parcelable {
    // tags for union fields
    public final static int contextHubInfo = 0;  // android.hardware.contexthub.ContextHubInfo contextHubInfo;
    public final static int vendorHubInfo = 1;  // android.hardware.contexthub.VendorHubInfo vendorHubInfo;

    private int _tag;
    private Object _value;

    public HubDetails() {
      android.hardware.contexthub.ContextHubInfo _value = null;
      this._tag = contextHubInfo;
      this._value = _value;
    }

    private HubDetails(android.os.Parcel _aidl_parcel) {
      readFromParcel(_aidl_parcel);
    }

    private HubDetails(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }

    public int getTag() {
      return _tag;
    }

    // android.hardware.contexthub.ContextHubInfo contextHubInfo;

    public static HubDetails contextHubInfo(android.hardware.contexthub.ContextHubInfo _value) {
      return new HubDetails(contextHubInfo, _value);
    }

    public android.hardware.contexthub.ContextHubInfo getContextHubInfo() {
      _assertTag(contextHubInfo);
      return (android.hardware.contexthub.ContextHubInfo) _value;
    }

    public void setContextHubInfo(android.hardware.contexthub.ContextHubInfo _value) {
      _set(contextHubInfo, _value);
    }

    // android.hardware.contexthub.VendorHubInfo vendorHubInfo;

    public static HubDetails vendorHubInfo(android.hardware.contexthub.VendorHubInfo _value) {
      return new HubDetails(vendorHubInfo, _value);
    }

    public android.hardware.contexthub.VendorHubInfo getVendorHubInfo() {
      _assertTag(vendorHubInfo);
      return (android.hardware.contexthub.VendorHubInfo) _value;
    }

    public void setVendorHubInfo(android.hardware.contexthub.VendorHubInfo _value) {
      _set(vendorHubInfo, _value);
    }

    @Override
    public final int getStability() {
      return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
    }

    public static final android.os.Parcelable.Creator<HubDetails> CREATOR = new android.os.Parcelable.Creator<HubDetails>() {
      @Override
      public HubDetails createFromParcel(android.os.Parcel _aidl_source) {
        return new HubDetails(_aidl_source);
      }
      @Override
      public HubDetails[] newArray(int _aidl_size) {
        return new HubDetails[_aidl_size];
      }
    };

    @Override
    public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
      _aidl_parcel.writeInt(_tag);
      switch (_tag) {
      case contextHubInfo:
        _aidl_parcel.writeTypedObject(getContextHubInfo(), _aidl_flag);
        break;
      case vendorHubInfo:
        _aidl_parcel.writeTypedObject(getVendorHubInfo(), _aidl_flag);
        break;
      }
    }

    public void readFromParcel(android.os.Parcel _aidl_parcel) {
      int _aidl_tag;
      _aidl_tag = _aidl_parcel.readInt();
      switch (_aidl_tag) {
      case contextHubInfo: {
        android.hardware.contexthub.ContextHubInfo _aidl_value;
        _aidl_value = _aidl_parcel.readTypedObject(android.hardware.contexthub.ContextHubInfo.CREATOR);
        _set(_aidl_tag, _aidl_value);
        return; }
      case vendorHubInfo: {
        android.hardware.contexthub.VendorHubInfo _aidl_value;
        _aidl_value = _aidl_parcel.readTypedObject(android.hardware.contexthub.VendorHubInfo.CREATOR);
        _set(_aidl_tag, _aidl_value);
        return; }
      }
      throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
    }

    @Override
    public int describeContents() {
      int _mask = 0;
      switch (getTag()) {
      case contextHubInfo:
        _mask |= describeContents(getContextHubInfo());
        break;
      case vendorHubInfo:
        _mask |= describeContents(getVendorHubInfo());
        break;
      }
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }

    private void _assertTag(int tag) {
      if (getTag() != tag) {
        throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
      }
    }

    private String _tagString(int _tag) {
      switch (_tag) {
      case contextHubInfo: return "contextHubInfo";
      case vendorHubInfo: return "vendorHubInfo";
      }
      throw new IllegalStateException("unknown field: " + _tag);
    }

    private void _set(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }
    public static @interface Tag {
      public static final int contextHubInfo = 0;
      public static final int vendorHubInfo = 1;
    }
  }
}
