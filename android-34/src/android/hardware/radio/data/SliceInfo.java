/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public class SliceInfo implements android.os.Parcelable
{
  public byte sliceServiceType = 0;
  public int sliceDifferentiator = 0;
  public byte mappedHplmnSst = 0;
  public int mappedHplmnSd = 0;
  public byte status = 0;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<SliceInfo> CREATOR = new android.os.Parcelable.Creator<SliceInfo>() {
    @Override
    public SliceInfo createFromParcel(android.os.Parcel _aidl_source) {
      SliceInfo _aidl_out = new SliceInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SliceInfo[] newArray(int _aidl_size) {
      return new SliceInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(sliceServiceType);
    _aidl_parcel.writeInt(sliceDifferentiator);
    _aidl_parcel.writeByte(mappedHplmnSst);
    _aidl_parcel.writeInt(mappedHplmnSd);
    _aidl_parcel.writeByte(status);
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
      sliceServiceType = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sliceDifferentiator = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mappedHplmnSst = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      mappedHplmnSd = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      status = _aidl_parcel.readByte();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final byte SERVICE_TYPE_NONE = 0;
  public static final byte SERVICE_TYPE_EMBB = 1;
  public static final byte SERVICE_TYPE_URLLC = 2;
  public static final byte SERVICE_TYPE_MIOT = 3;
  public static final byte STATUS_UNKNOWN = 0;
  public static final byte STATUS_CONFIGURED = 1;
  public static final byte STATUS_ALLOWED = 2;
  public static final byte STATUS_REJECTED_NOT_AVAILABLE_IN_PLMN = 3;
  public static final byte STATUS_REJECTED_NOT_AVAILABLE_IN_REG_AREA = 4;
  public static final byte STATUS_DEFAULT_CONFIGURED = 5;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("sliceServiceType: " + (sliceServiceType));
    _aidl_sj.add("sliceDifferentiator: " + (sliceDifferentiator));
    _aidl_sj.add("mappedHplmnSst: " + (mappedHplmnSst));
    _aidl_sj.add("mappedHplmnSd: " + (mappedHplmnSd));
    _aidl_sj.add("status: " + (status));
    return "android.hardware.radio.data.SliceInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
