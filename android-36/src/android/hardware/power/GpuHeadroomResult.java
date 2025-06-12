/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 6 --hash 13171cf98a48de298baf85167633376ea3db4ea0 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen/android/hardware/power/GpuHeadroomResult.java.d -o out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen -Nhardware/interfaces/power/aidl/aidl_api/android.hardware.power/6 hardware/interfaces/power/aidl/aidl_api/android.hardware.power/6/android/hardware/power/GpuHeadroomResult.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.power;
public final class GpuHeadroomResult implements android.os.Parcelable {
  // tags for union fields
  public final static int globalHeadroom = 0;  // float globalHeadroom;

  private int _tag;
  private Object _value;

  public GpuHeadroomResult() {
    float _value = 0.000000f;
    this._tag = globalHeadroom;
    this._value = _value;
  }

  private GpuHeadroomResult(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private GpuHeadroomResult(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // float globalHeadroom;

  public static GpuHeadroomResult globalHeadroom(float _value) {
    return new GpuHeadroomResult(globalHeadroom, _value);
  }

  public float getGlobalHeadroom() {
    _assertTag(globalHeadroom);
    return (float) _value;
  }

  public void setGlobalHeadroom(float _value) {
    _set(globalHeadroom, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<GpuHeadroomResult> CREATOR = new android.os.Parcelable.Creator<GpuHeadroomResult>() {
    @Override
    public GpuHeadroomResult createFromParcel(android.os.Parcel _aidl_source) {
      return new GpuHeadroomResult(_aidl_source);
    }
    @Override
    public GpuHeadroomResult[] newArray(int _aidl_size) {
      return new GpuHeadroomResult[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case globalHeadroom:
      _aidl_parcel.writeFloat(getGlobalHeadroom());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case globalHeadroom: {
      float _aidl_value;
      _aidl_value = _aidl_parcel.readFloat();
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
    case globalHeadroom: return "GpuHeadroomResult.globalHeadroom(" + (getGlobalHeadroom()) + ")";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof GpuHeadroomResult)) return false;
    GpuHeadroomResult that = (GpuHeadroomResult)other;
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
    case globalHeadroom: return "globalHeadroom";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface Tag {
    public static final int globalHeadroom = 0;
  }
}
