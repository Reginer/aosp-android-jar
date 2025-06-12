/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/HeadTracking.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/HeadTracking.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public class HeadTracking implements android.os.Parcelable
{
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<HeadTracking> CREATOR = new android.os.Parcelable.Creator<HeadTracking>() {
    @Override
    public HeadTracking createFromParcel(android.os.Parcel _aidl_source) {
      HeadTracking _aidl_out = new HeadTracking();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public HeadTracking[] newArray(int _aidl_size) {
      return new HeadTracking[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
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
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    return "HeadTracking" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof HeadTracking)) return false;
    HeadTracking that = (HeadTracking)other;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList().toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
  /** @hide */
  public static @interface Mode {
    public static final byte OTHER = 0;
    public static final byte DISABLED = 1;
    public static final byte RELATIVE_WORLD = 2;
    public static final byte RELATIVE_SCREEN = 3;
  }
  /** @hide */
  public static @interface ConnectionMode {
    public static final byte FRAMEWORK_PROCESSED = 0;
    public static final byte DIRECT_TO_SENSOR_SW = 1;
    public static final byte DIRECT_TO_SENSOR_TUNNEL = 2;
  }
  /** @hide */
  public static final class SensorData implements android.os.Parcelable {
    // tags for union fields
    public final static int headToStage = 0;  // float[6] headToStage;

    private int _tag;
    private Object _value;

    public SensorData() {
      float[] _value = {0.000000f, 0.000000f, 0.000000f, 0.000000f, 0.000000f, 0.000000f};
      this._tag = headToStage;
      this._value = _value;
    }

    private SensorData(android.os.Parcel _aidl_parcel) {
      readFromParcel(_aidl_parcel);
    }

    private SensorData(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }

    public int getTag() {
      return _tag;
    }

    // float[6] headToStage;

    public static SensorData headToStage(float[] _value) {
      return new SensorData(headToStage, _value);
    }

    public float[] getHeadToStage() {
      _assertTag(headToStage);
      return (float[]) _value;
    }

    public void setHeadToStage(float[] _value) {
      _set(headToStage, _value);
    }

    @Override
    public final int getStability() {
      return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
    }

    public static final android.os.Parcelable.Creator<SensorData> CREATOR = new android.os.Parcelable.Creator<SensorData>() {
      @Override
      public SensorData createFromParcel(android.os.Parcel _aidl_source) {
        return new SensorData(_aidl_source);
      }
      @Override
      public SensorData[] newArray(int _aidl_size) {
        return new SensorData[_aidl_size];
      }
    };

    @Override
    public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
      _aidl_parcel.writeInt(_tag);
      switch (_tag) {
      case headToStage:
        _aidl_parcel.writeFixedArray(getHeadToStage(), _aidl_flag, 6);
        break;
      }
    }

    public void readFromParcel(android.os.Parcel _aidl_parcel) {
      int _aidl_tag;
      _aidl_tag = _aidl_parcel.readInt();
      switch (_aidl_tag) {
      case headToStage: {
        float[] _aidl_value;
        _aidl_value = _aidl_parcel.createFixedArray(float[].class, 6);
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

    private void _assertTag(int tag) {
      if (getTag() != tag) {
        throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
      }
    }

    private String _tagString(int _tag) {
      switch (_tag) {
      case headToStage: return "headToStage";
      }
      throw new IllegalStateException("unknown field: " + _tag);
    }

    private void _set(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }
    public static @interface Tag {
      public static final int headToStage = 0;
    }
  }
}
