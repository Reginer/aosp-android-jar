/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/AudioHalCapParameter.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/AudioHalCapParameter.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public final class AudioHalCapParameter implements android.os.Parcelable {
  // tags for union fields
  public final static int selectedStrategyDevice = 0;  // android.media.audio.common.AudioHalCapParameter.StrategyDevice selectedStrategyDevice;
  public final static int selectedInputSourceDevice = 1;  // android.media.audio.common.AudioHalCapParameter.InputSourceDevice selectedInputSourceDevice;
  public final static int strategyDeviceAddress = 2;  // android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress strategyDeviceAddress;
  public final static int streamVolumeProfile = 3;  // android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile streamVolumeProfile;

  private int _tag;
  private Object _value;

  public AudioHalCapParameter() {
    android.media.audio.common.AudioHalCapParameter.StrategyDevice _value = null;
    this._tag = selectedStrategyDevice;
    this._value = _value;
  }

  private AudioHalCapParameter(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioHalCapParameter(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.media.audio.common.AudioHalCapParameter.StrategyDevice selectedStrategyDevice;

  public static AudioHalCapParameter selectedStrategyDevice(android.media.audio.common.AudioHalCapParameter.StrategyDevice _value) {
    return new AudioHalCapParameter(selectedStrategyDevice, _value);
  }

  public android.media.audio.common.AudioHalCapParameter.StrategyDevice getSelectedStrategyDevice() {
    _assertTag(selectedStrategyDevice);
    return (android.media.audio.common.AudioHalCapParameter.StrategyDevice) _value;
  }

  public void setSelectedStrategyDevice(android.media.audio.common.AudioHalCapParameter.StrategyDevice _value) {
    _set(selectedStrategyDevice, _value);
  }

  // android.media.audio.common.AudioHalCapParameter.InputSourceDevice selectedInputSourceDevice;

  public static AudioHalCapParameter selectedInputSourceDevice(android.media.audio.common.AudioHalCapParameter.InputSourceDevice _value) {
    return new AudioHalCapParameter(selectedInputSourceDevice, _value);
  }

  public android.media.audio.common.AudioHalCapParameter.InputSourceDevice getSelectedInputSourceDevice() {
    _assertTag(selectedInputSourceDevice);
    return (android.media.audio.common.AudioHalCapParameter.InputSourceDevice) _value;
  }

  public void setSelectedInputSourceDevice(android.media.audio.common.AudioHalCapParameter.InputSourceDevice _value) {
    _set(selectedInputSourceDevice, _value);
  }

  // android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress strategyDeviceAddress;

  public static AudioHalCapParameter strategyDeviceAddress(android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress _value) {
    return new AudioHalCapParameter(strategyDeviceAddress, _value);
  }

  public android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress getStrategyDeviceAddress() {
    _assertTag(strategyDeviceAddress);
    return (android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress) _value;
  }

  public void setStrategyDeviceAddress(android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress _value) {
    _set(strategyDeviceAddress, _value);
  }

  // android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile streamVolumeProfile;

  public static AudioHalCapParameter streamVolumeProfile(android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile _value) {
    return new AudioHalCapParameter(streamVolumeProfile, _value);
  }

  public android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile getStreamVolumeProfile() {
    _assertTag(streamVolumeProfile);
    return (android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile) _value;
  }

  public void setStreamVolumeProfile(android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile _value) {
    _set(streamVolumeProfile, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AudioHalCapParameter> CREATOR = new android.os.Parcelable.Creator<AudioHalCapParameter>() {
    @Override
    public AudioHalCapParameter createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioHalCapParameter(_aidl_source);
    }
    @Override
    public AudioHalCapParameter[] newArray(int _aidl_size) {
      return new AudioHalCapParameter[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case selectedStrategyDevice:
      _aidl_parcel.writeTypedObject(getSelectedStrategyDevice(), _aidl_flag);
      break;
    case selectedInputSourceDevice:
      _aidl_parcel.writeTypedObject(getSelectedInputSourceDevice(), _aidl_flag);
      break;
    case strategyDeviceAddress:
      _aidl_parcel.writeTypedObject(getStrategyDeviceAddress(), _aidl_flag);
      break;
    case streamVolumeProfile:
      _aidl_parcel.writeTypedObject(getStreamVolumeProfile(), _aidl_flag);
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case selectedStrategyDevice: {
      android.media.audio.common.AudioHalCapParameter.StrategyDevice _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapParameter.StrategyDevice.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case selectedInputSourceDevice: {
      android.media.audio.common.AudioHalCapParameter.InputSourceDevice _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapParameter.InputSourceDevice.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case strategyDeviceAddress: {
      android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapParameter.StrategyDeviceAddress.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    case streamVolumeProfile: {
      android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile _aidl_value;
      _aidl_value = _aidl_parcel.readTypedObject(android.media.audio.common.AudioHalCapParameter.StreamVolumeProfile.CREATOR);
      _set(_aidl_tag, _aidl_value);
      return; }
    }
    throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
  }

  @Override
  public int describeContents() {
    int _mask = 0;
    switch (getTag()) {
    case selectedStrategyDevice:
      _mask |= describeContents(getSelectedStrategyDevice());
      break;
    case selectedInputSourceDevice:
      _mask |= describeContents(getSelectedInputSourceDevice());
      break;
    case strategyDeviceAddress:
      _mask |= describeContents(getStrategyDeviceAddress());
      break;
    case streamVolumeProfile:
      _mask |= describeContents(getStreamVolumeProfile());
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
    case selectedStrategyDevice: return "selectedStrategyDevice";
    case selectedInputSourceDevice: return "selectedInputSourceDevice";
    case strategyDeviceAddress: return "strategyDeviceAddress";
    case streamVolumeProfile: return "streamVolumeProfile";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static class StrategyDevice implements android.os.Parcelable
  {
    public android.media.audio.common.AudioDeviceDescription device;
    public int id = -1;
    public boolean isSelected = false;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<StrategyDevice> CREATOR = new android.os.Parcelable.Creator<StrategyDevice>() {
      @Override
      public StrategyDevice createFromParcel(android.os.Parcel _aidl_source) {
        StrategyDevice _aidl_out = new StrategyDevice();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public StrategyDevice[] newArray(int _aidl_size) {
        return new StrategyDevice[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedObject(device, _aidl_flag);
      _aidl_parcel.writeInt(id);
      _aidl_parcel.writeBoolean(isSelected);
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
        device = _aidl_parcel.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        id = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        isSelected = _aidl_parcel.readBoolean();
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
      _mask |= describeContents(device);
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }
  }
  public static class InputSourceDevice implements android.os.Parcelable
  {
    public android.media.audio.common.AudioDeviceDescription device;
    public int inputSource = android.media.audio.common.AudioSource.DEFAULT;
    public boolean isSelected = false;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<InputSourceDevice> CREATOR = new android.os.Parcelable.Creator<InputSourceDevice>() {
      @Override
      public InputSourceDevice createFromParcel(android.os.Parcel _aidl_source) {
        InputSourceDevice _aidl_out = new InputSourceDevice();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public InputSourceDevice[] newArray(int _aidl_size) {
        return new InputSourceDevice[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedObject(device, _aidl_flag);
      _aidl_parcel.writeInt(inputSource);
      _aidl_parcel.writeBoolean(isSelected);
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
        device = _aidl_parcel.readTypedObject(android.media.audio.common.AudioDeviceDescription.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        inputSource = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        isSelected = _aidl_parcel.readBoolean();
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
      _mask |= describeContents(device);
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }
  }
  public static class StrategyDeviceAddress implements android.os.Parcelable
  {
    public android.media.audio.common.AudioDeviceAddress deviceAddress;
    public int id = -1;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<StrategyDeviceAddress> CREATOR = new android.os.Parcelable.Creator<StrategyDeviceAddress>() {
      @Override
      public StrategyDeviceAddress createFromParcel(android.os.Parcel _aidl_source) {
        StrategyDeviceAddress _aidl_out = new StrategyDeviceAddress();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public StrategyDeviceAddress[] newArray(int _aidl_size) {
        return new StrategyDeviceAddress[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedObject(deviceAddress, _aidl_flag);
      _aidl_parcel.writeInt(id);
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
        deviceAddress = _aidl_parcel.readTypedObject(android.media.audio.common.AudioDeviceAddress.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        id = _aidl_parcel.readInt();
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
      _mask |= describeContents(deviceAddress);
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }
  }
  public static class StreamVolumeProfile implements android.os.Parcelable
  {
    public int stream = android.media.audio.common.AudioStreamType.INVALID;
    public int profile = android.media.audio.common.AudioStreamType.INVALID;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<StreamVolumeProfile> CREATOR = new android.os.Parcelable.Creator<StreamVolumeProfile>() {
      @Override
      public StreamVolumeProfile createFromParcel(android.os.Parcel _aidl_source) {
        StreamVolumeProfile _aidl_out = new StreamVolumeProfile();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public StreamVolumeProfile[] newArray(int _aidl_size) {
        return new StreamVolumeProfile[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(stream);
      _aidl_parcel.writeInt(profile);
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
        stream = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        profile = _aidl_parcel.readInt();
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
  public static @interface Tag {
    public static final int selectedStrategyDevice = 0;
    public static final int selectedInputSourceDevice = 1;
    public static final int strategyDeviceAddress = 2;
    public static final int streamVolumeProfile = 3;
  }
}
