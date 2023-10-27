/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.data;
public class RouteSelectionDescriptor implements android.os.Parcelable
{
  public byte precedence = 0;
  public int sessionType;
  public byte sscMode = 0;
  public android.hardware.radio.data.SliceInfo[] sliceInfo;
  public java.lang.String[] dnn;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<RouteSelectionDescriptor> CREATOR = new android.os.Parcelable.Creator<RouteSelectionDescriptor>() {
    @Override
    public RouteSelectionDescriptor createFromParcel(android.os.Parcel _aidl_source) {
      RouteSelectionDescriptor _aidl_out = new RouteSelectionDescriptor();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public RouteSelectionDescriptor[] newArray(int _aidl_size) {
      return new RouteSelectionDescriptor[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeByte(precedence);
    _aidl_parcel.writeInt(sessionType);
    _aidl_parcel.writeByte(sscMode);
    _aidl_parcel.writeTypedArray(sliceInfo, _aidl_flag);
    _aidl_parcel.writeStringArray(dnn);
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
      precedence = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sessionType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sscMode = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sliceInfo = _aidl_parcel.createTypedArray(android.hardware.radio.data.SliceInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      dnn = _aidl_parcel.createStringArray();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final byte SSC_MODE_UNKNOWN = -1;
  public static final byte SSC_MODE_1 = 1;
  public static final byte SSC_MODE_2 = 2;
  public static final byte SSC_MODE_3 = 3;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("precedence: " + (precedence));
    _aidl_sj.add("sessionType: " + (android.hardware.radio.data.PdpProtocolType.$.toString(sessionType)));
    _aidl_sj.add("sscMode: " + (sscMode));
    _aidl_sj.add("sliceInfo: " + (java.util.Arrays.toString(sliceInfo)));
    _aidl_sj.add("dnn: " + (java.util.Arrays.toString(dnn)));
    return "android.hardware.radio.data.RouteSelectionDescriptor" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(sliceInfo);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof Object[]) {
      int _mask = 0;
      for (Object o : (Object[]) _v) {
        _mask |= describeContents(o);
      }
      return _mask;
    }
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
}
