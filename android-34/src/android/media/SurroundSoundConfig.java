/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * TODO(b/280077672): This is a temporary copy of the stable
 * android.hardware.audio.core.SurroundSoundConfig parcelable.
 * Interfaces from the Core API do not support the CPP backend. This copy will
 * be removed either by moving the AudioRoute from core to a.m.a.common or by
 * switching the framework internal interfaces to the NDK backend.
 * {@hide}
 */
public class SurroundSoundConfig implements android.os.Parcelable
{
  public android.media.SurroundSoundConfig.SurroundFormatFamily[] formatFamilies;
  public static final android.os.Parcelable.Creator<SurroundSoundConfig> CREATOR = new android.os.Parcelable.Creator<SurroundSoundConfig>() {
    @Override
    public SurroundSoundConfig createFromParcel(android.os.Parcel _aidl_source) {
      SurroundSoundConfig _aidl_out = new SurroundSoundConfig();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SurroundSoundConfig[] newArray(int _aidl_size) {
      return new SurroundSoundConfig[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedArray(formatFamilies, _aidl_flag);
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
      formatFamilies = _aidl_parcel.createTypedArray(android.media.SurroundSoundConfig.SurroundFormatFamily.CREATOR);
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
    _mask |= describeContents(formatFamilies);
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
  public static class SurroundFormatFamily implements android.os.Parcelable
  {
    /**
     * A primaryFormat shall get an entry in the Surround Settings dialog on TV
     * devices. There must be a corresponding Java ENCODING_... constant
     * defined in AudioFormat.java, and a display name defined in
     * AudioFormat.toDisplayName.
     */
    public android.media.audio.common.AudioFormatDescription primaryFormat;
    /**
     * List of formats that shall be equivalent to the primaryFormat from the
     * users' point of view and don't need a dedicated Surround Settings
     * dialog entry.
     */
    public android.media.audio.common.AudioFormatDescription[] subFormats;
    public static final android.os.Parcelable.Creator<SurroundFormatFamily> CREATOR = new android.os.Parcelable.Creator<SurroundFormatFamily>() {
      @Override
      public SurroundFormatFamily createFromParcel(android.os.Parcel _aidl_source) {
        SurroundFormatFamily _aidl_out = new SurroundFormatFamily();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public SurroundFormatFamily[] newArray(int _aidl_size) {
        return new SurroundFormatFamily[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedObject(primaryFormat, _aidl_flag);
      _aidl_parcel.writeTypedArray(subFormats, _aidl_flag);
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
        primaryFormat = _aidl_parcel.readTypedObject(android.media.audio.common.AudioFormatDescription.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        subFormats = _aidl_parcel.createTypedArray(android.media.audio.common.AudioFormatDescription.CREATOR);
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
      _mask |= describeContents(primaryFormat);
      _mask |= describeContents(subFormats);
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
}
