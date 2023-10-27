/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.audio.common;
public class MicrophoneInfo implements android.os.Parcelable
{
  public java.lang.String id;
  public android.media.audio.common.AudioDevice device;
  public int location = android.media.audio.common.MicrophoneInfo.Location.UNKNOWN;
  public int group = -1;
  public int indexInTheGroup = -1;
  public android.media.audio.common.MicrophoneInfo.Sensitivity sensitivity;
  public int directionality = android.media.audio.common.MicrophoneInfo.Directionality.UNKNOWN;
  public android.media.audio.common.MicrophoneInfo.FrequencyResponsePoint[] frequencyResponse;
  public android.media.audio.common.MicrophoneInfo.Coordinate position;
  public android.media.audio.common.MicrophoneInfo.Coordinate orientation;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<MicrophoneInfo> CREATOR = new android.os.Parcelable.Creator<MicrophoneInfo>() {
    @Override
    public MicrophoneInfo createFromParcel(android.os.Parcel _aidl_source) {
      MicrophoneInfo _aidl_out = new MicrophoneInfo();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public MicrophoneInfo[] newArray(int _aidl_size) {
      return new MicrophoneInfo[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(id);
    _aidl_parcel.writeTypedObject(device, _aidl_flag);
    _aidl_parcel.writeInt(location);
    _aidl_parcel.writeInt(group);
    _aidl_parcel.writeInt(indexInTheGroup);
    _aidl_parcel.writeTypedObject(sensitivity, _aidl_flag);
    _aidl_parcel.writeInt(directionality);
    _aidl_parcel.writeTypedArray(frequencyResponse, _aidl_flag);
    _aidl_parcel.writeTypedObject(position, _aidl_flag);
    _aidl_parcel.writeTypedObject(orientation, _aidl_flag);
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
      id = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      device = _aidl_parcel.readTypedObject(android.media.audio.common.AudioDevice.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      location = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      group = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      indexInTheGroup = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sensitivity = _aidl_parcel.readTypedObject(android.media.audio.common.MicrophoneInfo.Sensitivity.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      directionality = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      frequencyResponse = _aidl_parcel.createTypedArray(android.media.audio.common.MicrophoneInfo.FrequencyResponsePoint.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      position = _aidl_parcel.readTypedObject(android.media.audio.common.MicrophoneInfo.Coordinate.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      orientation = _aidl_parcel.readTypedObject(android.media.audio.common.MicrophoneInfo.Coordinate.CREATOR);
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int GROUP_UNKNOWN = -1;
  public static final int INDEX_IN_THE_GROUP_UNKNOWN = -1;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("id: " + (java.util.Objects.toString(id)));
    _aidl_sj.add("device: " + (java.util.Objects.toString(device)));
    _aidl_sj.add("location: " + (location));
    _aidl_sj.add("group: " + (group));
    _aidl_sj.add("indexInTheGroup: " + (indexInTheGroup));
    _aidl_sj.add("sensitivity: " + (java.util.Objects.toString(sensitivity)));
    _aidl_sj.add("directionality: " + (directionality));
    _aidl_sj.add("frequencyResponse: " + (java.util.Arrays.toString(frequencyResponse)));
    _aidl_sj.add("position: " + (java.util.Objects.toString(position)));
    _aidl_sj.add("orientation: " + (java.util.Objects.toString(orientation)));
    return "android.media.audio.common.MicrophoneInfo" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof MicrophoneInfo)) return false;
    MicrophoneInfo that = (MicrophoneInfo)other;
    if (!java.util.Objects.deepEquals(id, that.id)) return false;
    if (!java.util.Objects.deepEquals(device, that.device)) return false;
    if (!java.util.Objects.deepEquals(location, that.location)) return false;
    if (!java.util.Objects.deepEquals(group, that.group)) return false;
    if (!java.util.Objects.deepEquals(indexInTheGroup, that.indexInTheGroup)) return false;
    if (!java.util.Objects.deepEquals(sensitivity, that.sensitivity)) return false;
    if (!java.util.Objects.deepEquals(directionality, that.directionality)) return false;
    if (!java.util.Objects.deepEquals(frequencyResponse, that.frequencyResponse)) return false;
    if (!java.util.Objects.deepEquals(position, that.position)) return false;
    if (!java.util.Objects.deepEquals(orientation, that.orientation)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(id, device, location, group, indexInTheGroup, sensitivity, directionality, frequencyResponse, position, orientation).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(device);
    _mask |= describeContents(sensitivity);
    _mask |= describeContents(frequencyResponse);
    _mask |= describeContents(position);
    _mask |= describeContents(orientation);
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
  public static @interface Location {
    public static final int UNKNOWN = 0;
    public static final int MAINBODY = 1;
    public static final int MAINBODY_MOVABLE = 2;
    public static final int PERIPHERAL = 3;
  }
  public static class Sensitivity implements android.os.Parcelable
  {
    public float leveldBFS = 0.000000f;
    public float maxSpldB = 0.000000f;
    public float minSpldB = 0.000000f;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<Sensitivity> CREATOR = new android.os.Parcelable.Creator<Sensitivity>() {
      @Override
      public Sensitivity createFromParcel(android.os.Parcel _aidl_source) {
        Sensitivity _aidl_out = new Sensitivity();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public Sensitivity[] newArray(int _aidl_size) {
        return new Sensitivity[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeFloat(leveldBFS);
      _aidl_parcel.writeFloat(maxSpldB);
      _aidl_parcel.writeFloat(minSpldB);
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
        leveldBFS = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        maxSpldB = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        minSpldB = _aidl_parcel.readFloat();
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    @Override
    public int describeContents() {
      int _mask = 0;
      return _mask;
    }
  }
  public static @interface Directionality {
    public static final int UNKNOWN = 0;
    public static final int OMNI = 1;
    public static final int BI_DIRECTIONAL = 2;
    public static final int CARDIOID = 3;
    public static final int HYPER_CARDIOID = 4;
    public static final int SUPER_CARDIOID = 5;
  }
  public static class FrequencyResponsePoint implements android.os.Parcelable
  {
    public float frequencyHz = 0.000000f;
    public float leveldB = 0.000000f;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<FrequencyResponsePoint> CREATOR = new android.os.Parcelable.Creator<FrequencyResponsePoint>() {
      @Override
      public FrequencyResponsePoint createFromParcel(android.os.Parcel _aidl_source) {
        FrequencyResponsePoint _aidl_out = new FrequencyResponsePoint();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public FrequencyResponsePoint[] newArray(int _aidl_size) {
        return new FrequencyResponsePoint[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeFloat(frequencyHz);
      _aidl_parcel.writeFloat(leveldB);
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
        frequencyHz = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        leveldB = _aidl_parcel.readFloat();
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    @Override
    public int describeContents() {
      int _mask = 0;
      return _mask;
    }
  }
  public static class Coordinate implements android.os.Parcelable
  {
    public float x = 0.000000f;
    public float y = 0.000000f;
    public float z = 0.000000f;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<Coordinate> CREATOR = new android.os.Parcelable.Creator<Coordinate>() {
      @Override
      public Coordinate createFromParcel(android.os.Parcel _aidl_source) {
        Coordinate _aidl_out = new Coordinate();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public Coordinate[] newArray(int _aidl_size) {
        return new Coordinate[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeFloat(x);
      _aidl_parcel.writeFloat(y);
      _aidl_parcel.writeFloat(z);
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
        x = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        y = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        z = _aidl_parcel.readFloat();
      } finally {
        if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
          throw new android.os.BadParcelableException("Overflow in the size of parcelable");
        }
        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
      }
    }
    @Override
    public int describeContents() {
      int _mask = 0;
      return _mask;
    }
  }
}
