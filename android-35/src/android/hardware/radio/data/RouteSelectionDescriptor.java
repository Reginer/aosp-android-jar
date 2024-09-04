/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 3 --hash cd8913a3f9d39f1cc0a5fcf9e90257be94ec38df --stability vintf --min_sdk_version current -pout/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio_interface/3/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V3-java-source/gen/android/hardware/radio/data/RouteSelectionDescriptor.java.d -o out/soong/.intermediates/hardware/interfaces/radio/aidl/android.hardware.radio.data-V3-java-source/gen -Nhardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/3 hardware/interfaces/radio/aidl/aidl_api/android.hardware.radio.data/3/android/hardware/radio/data/RouteSelectionDescriptor.aidl
 */
package android.hardware.radio.data;
/** @hide */
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
    return "RouteSelectionDescriptor" + _aidl_sj.toString()  ;
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
