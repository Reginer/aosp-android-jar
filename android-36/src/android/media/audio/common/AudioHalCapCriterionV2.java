/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/AudioHalCapCriterionV2.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/AudioHalCapCriterionV2.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public final class AudioHalCapCriterionV2 implements android.os.Parcelable {
  // tags for union fields
  public final static int availableInputDevices = 0;  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices availableInputDevices;
  public final static int availableOutputDevices = 1;  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices availableOutputDevices;
  public final static int availableInputDevicesAddresses = 2;  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses availableInputDevicesAddresses;
  public final static int availableOutputDevicesAddresses = 3;  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses availableOutputDevicesAddresses;
  public final static int telephonyMode = 4;  // android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode telephonyMode;
  public final static int forceConfigForUse = 5;  // android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse forceConfigForUse;

  private int _tag;
  private Object _value;

  public AudioHalCapCriterionV2() {
    android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices _value = null;
    this._tag = availableInputDevices;
    this._value = _value;
  }

  private AudioHalCapCriterionV2(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioHalCapCriterionV2(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices availableInputDevices;

  public static AudioHalCapCriterionV2 availableInputDevices(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices _value) {
    return new AudioHalCapCriterionV2(availableInputDevices, _value);
  }

  public android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices getAvailableInputDevices() {
    _assertTag(availableInputDevices);
    return (android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices) _value;
  }

  public void setAvailableInputDevices(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices _value) {
    _set(availableInputDevices, _value);
  }

  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices availableOutputDevices;

  public static AudioHalCapCriterionV2 availableOutputDevices(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices _value) {
    return new AudioHalCapCriterionV2(availableOutputDevices, _value);
  }

  public android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices getAvailableOutputDevices() {
    _assertTag(availableOutputDevices);
    return (android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices) _value;
  }

  public void setAvailableOutputDevices(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices _value) {
    _set(availableOutputDevices, _value);
  }

  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses availableInputDevicesAddresses;

  public static AudioHalCapCriterionV2 availableInputDevicesAddresses(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses _value) {
    return new AudioHalCapCriterionV2(availableInputDevicesAddresses, _value);
  }

  public android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses getAvailableInputDevicesAddresses() {
    _assertTag(availableInputDevicesAddresses);
    return (android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses) _value;
  }

  public void setAvailableInputDevicesAddresses(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses _value) {
    _set(availableInputDevicesAddresses, _value);
  }

  // android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses availableOutputDevicesAddresses;

  public static AudioHalCapCriterionV2 availableOutputDevicesAddresses(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses _value) {
    return new AudioHalCapCriterionV2(availableOutputDevicesAddresses, _value);
  }

  public android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses getAvailableOutputDevicesAddresses() {
    _assertTag(availableOutputDevicesAddresses);
    return (android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses) _value;
  }

  public void setAvailableOutputDevicesAddresses(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses _value) {
    _set(availableOutputDevicesAddresses, _value);
  }

  // android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode telephonyMode;

  public static AudioHalCapCriterionV2 telephonyMode(android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode _value) {
    return new AudioHalCapCriterionV2(telephonyMode, _value);
  }

  public android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode getTelephonyMode() {
    _assertTag(telephonyMode);
    return (android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode) _value;
  }

  public void setTelephonyMode(android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode _value) {
    _set(telephonyMode, _value);
  }

  // android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse forceConfigForUse;

  public static AudioHalCapCriterionV2 forceConfigForUse(android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse _value) {
    return new AudioHalCapCriterionV2(forceConfigForUse, _value);
  }

  public android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse getForceConfigForUse() {
    _assertTag(forceConfigForUse);
    return (android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse) _value;
  }

  public void setForceConfigForUse(android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse _value) {
    _set(forceConfigForUse, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AudioHalCapCriterionV2> CREATOR = new android.os.Parcelable.Creator<AudioHalCapCriterionV2>() {
    @Override
    public AudioHalCapCriterionV2 createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioHalCapCriterionV2(_aidl_source);
    }
    @Override
    public AudioHalCapCriterionV2[] newArray(int _aidl_size) {
      return new AudioHalCapCriterionV2[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case availableInputDevices:
      _aidl_parcel.writeTypedObject(getAvailableInputDevices(), _aidl_flag);
      break;
    case availableOutputDevices:
      _aidl_parcel.writeTypedObject(getAvailableOutputDevices(), _aidl_flag);
      break;
    case availableInputDevicesAddresses:
      _aidl_parcel.writeTypedObject(getAvailableInputDevicesAddresses(), _aidl_flag);
      break;
    case availableOutputDevicesAddresses:
      _aidl_parcel.writeTypedObject(getAvailableOutputDevicesAddresses(), _aidl_flag);
      break;
    case telephonyMode:
      _aidl_parcel.writeTypedObject(getTelephonyMode(), _aidl_flag);
      break;
    case forceConfigForUse:
      _aidl_parcel.writeTypedObject(getForceConfigForUse(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case availableInputDevices: {
      android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case availableOutputDevices: {
      android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevices.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case availableInputDevicesAddresses: {
      android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case availableOutputDevicesAddresses: {
      android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapCriterionV2.AvailableDevicesAddresses.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case telephonyMode: {
      android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapCriterionV2.TelephonyMode.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case forceConfigForUse: {
      android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapCriterionV2.ForceConfigForUse.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case availableInputDevices:
      _mask |= describeContents(getAvailableInputDevices());
      break;
    case availableOutputDevices:
      _mask |= describeContents(getAvailableOutputDevices());
      break;
    case availableInputDevicesAddresses:
      _mask |= describeContents(getAvailableInputDevicesAddresses());
      break;
    case availableOutputDevicesAddresses:
      _mask |= describeContents(getAvailableOutputDevicesAddresses());
      break;
    case telephonyMode:
      _mask |= describeContents(getTelephonyMode());
      break;
    case forceConfigForUse:
      _mask |= describeContents(getForceConfigForUse());
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
    case availableInputDevices: return "availableInputDevices";
    case availableOutputDevices: return "availableOutputDevices";
    case availableInputDevicesAddresses: return "availableInputDevicesAddresses";
    case availableOutputDevicesAddresses: return "availableOutputDevicesAddresses";
    case telephonyMode: return "telephonyMode";
    case forceConfigForUse: return "forceConfigForUse";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface LogicalDisjunction {
    public static final byte EXCLUSIVE = 0;
    public static final byte INCLUSIVE = 1;
  }
  public static class ForceConfigForUse implements android.os.Parcelable
  {
    public android.media.audio.common.AudioPolicyForceUse[] values;
    public android.media.audio.common.AudioPolicyForceUse defaultValue;
    public byte logic = android.media.audio.common.AudioHalCapCriterionV2.LogicalDisjunction.EXCLUSIVE;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<ForceConfigForUse> CREATOR = new android.os.Parcelable.Creator<ForceConfigForUse>() {
      @Override
      public ForceConfigForUse createFromParcel(android.os.Parcel _aidl_source) {
        ForceConfigForUse _aidl_out = new ForceConfigForUse();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public ForceConfigForUse[] newArray(int _aidl_size) {
        return new ForceConfigForUse[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedArray(values, _aidl_flag);
      _aidl_parcel.writeTypedObject(defaultValue, _aidl_flag);
      _aidl_parcel.writeByte(logic);
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
        values = _aidl_parcel.createTypedArray(android.media.audio.common.AudioPolicyForceUse.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        defaultValue = _aidl_parcel.readTypedObject(android.media.audio.common.AudioPolicyForceUse.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        logic = _aidl_parcel.readByte();
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
      _mask |= describeContents(values);
      _mask |= describeContents(defaultValue);
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
  public static class TelephonyMode implements android.os.Parcelable
  {
    public int[] values;
    public int defaultValue = android.media.audio.common.AudioMode.NORMAL;
    public byte logic = android.media.audio.common.AudioHalCapCriterionV2.LogicalDisjunction.EXCLUSIVE;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<TelephonyMode> CREATOR = new android.os.Parcelable.Creator<TelephonyMode>() {
      @Override
      public TelephonyMode createFromParcel(android.os.Parcel _aidl_source) {
        TelephonyMode _aidl_out = new TelephonyMode();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public TelephonyMode[] newArray(int _aidl_size) {
        return new TelephonyMode[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeIntArray(values);
      _aidl_parcel.writeInt(defaultValue);
      _aidl_parcel.writeByte(logic);
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
        values = _aidl_parcel.createIntArray();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        defaultValue = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        logic = _aidl_parcel.readByte();
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
  public static class AvailableDevices implements android.os.Parcelable
  {
    public android.media.audio.common.AudioDeviceDescription[] values;
    public byte logic = android.media.audio.common.AudioHalCapCriterionV2.LogicalDisjunction.INCLUSIVE;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<AvailableDevices> CREATOR = new android.os.Parcelable.Creator<AvailableDevices>() {
      @Override
      public AvailableDevices createFromParcel(android.os.Parcel _aidl_source) {
        AvailableDevices _aidl_out = new AvailableDevices();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public AvailableDevices[] newArray(int _aidl_size) {
        return new AvailableDevices[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedArray(values, _aidl_flag);
      _aidl_parcel.writeByte(logic);
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
        values = _aidl_parcel.createTypedArray(android.media.audio.common.AudioDeviceDescription.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        logic = _aidl_parcel.readByte();
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
      _mask |= describeContents(values);
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
  public static class AvailableDevicesAddresses implements android.os.Parcelable
  {
    public android.media.audio.common.AudioDeviceAddress[] values;
    public byte logic = android.media.audio.common.AudioHalCapCriterionV2.LogicalDisjunction.INCLUSIVE;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<AvailableDevicesAddresses> CREATOR = new android.os.Parcelable.Creator<AvailableDevicesAddresses>() {
      @Override
      public AvailableDevicesAddresses createFromParcel(android.os.Parcel _aidl_source) {
        AvailableDevicesAddresses _aidl_out = new AvailableDevicesAddresses();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public AvailableDevicesAddresses[] newArray(int _aidl_size) {
        return new AvailableDevicesAddresses[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedArray(values, _aidl_flag);
      _aidl_parcel.writeByte(logic);
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
        values = _aidl_parcel.createTypedArray(android.media.audio.common.AudioDeviceAddress.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        logic = _aidl_parcel.readByte();
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
      _mask |= describeContents(values);
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
  public static @interface Tag {
    public static final int availableInputDevices = 0;
    public static final int availableOutputDevices = 1;
    public static final int availableInputDevicesAddresses = 2;
    public static final int availableOutputDevicesAddresses = 3;
    public static final int telephonyMode = 4;
    public static final int forceConfigForUse = 5;
  }
}
