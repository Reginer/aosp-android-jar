/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash af71e6ae2c6861fc2b09bb477e7285e6777cd41c --stability vintf --min_sdk_version 29 --ninja -d out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen/android/media/audio/common/AudioPolicyForceUse.java.d -o out/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types-V4-java-source/gen -Nsystem/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4 system/hardware/interfaces/media/aidl_api/android.media.audio.common.types/4/android/media/audio/common/AudioPolicyForceUse.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.audio.common;
/** @hide */
public final class AudioPolicyForceUse implements android.os.Parcelable {
  // tags for union fields
  public final static int forMedia = 0;  // android.media.audio.common.AudioPolicyForceUse.MediaDeviceCategory forMedia;
  public final static int forCommunication = 1;  // android.media.audio.common.AudioPolicyForceUse.CommunicationDeviceCategory forCommunication;
  public final static int forRecord = 2;  // android.media.audio.common.AudioPolicyForceUse.CommunicationDeviceCategory forRecord;
  public final static int forVibrateRinging = 3;  // android.media.audio.common.AudioPolicyForceUse.CommunicationDeviceCategory forVibrateRinging;
  public final static int dock = 4;  // android.media.audio.common.AudioPolicyForceUse.DockType dock;
  public final static int systemSounds = 5;  // boolean systemSounds;
  public final static int hdmiSystemAudio = 6;  // boolean hdmiSystemAudio;
  public final static int encodedSurround = 7;  // android.media.audio.common.AudioPolicyForceUse.EncodedSurroundConfig encodedSurround;

  private int _tag;
  private Object _value;

  public AudioPolicyForceUse() {
    byte _value = android.media.audio.common.AudioPolicyForceUse.MediaDeviceCategory.NONE;
    this._tag = forMedia;
    this._value = _value;
  }

  private AudioPolicyForceUse(android.os.Parcel _aidl_parcel) {
    readFromParcel(_aidl_parcel);
  }

  private AudioPolicyForceUse(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }

  public int getTag() {
    return _tag;
  }

  // android.media.audio.common.AudioPolicyForceUse.MediaDeviceCategory forMedia;

  public static AudioPolicyForceUse forMedia(byte _value) {
    return new AudioPolicyForceUse(forMedia, _value);
  }

  public byte getForMedia() {
    _assertTag(forMedia);
    return (byte) _value;
  }

  public void setForMedia(byte _value) {
    _set(forMedia, _value);
  }

  // android.media.audio.common.AudioPolicyForceUse.CommunicationDeviceCategory forCommunication;

  public static AudioPolicyForceUse forCommunication(byte _value) {
    return new AudioPolicyForceUse(forCommunication, _value);
  }

  public byte getForCommunication() {
    _assertTag(forCommunication);
    return (byte) _value;
  }

  public void setForCommunication(byte _value) {
    _set(forCommunication, _value);
  }

  // android.media.audio.common.AudioPolicyForceUse.CommunicationDeviceCategory forRecord;

  public static AudioPolicyForceUse forRecord(byte _value) {
    return new AudioPolicyForceUse(forRecord, _value);
  }

  public byte getForRecord() {
    _assertTag(forRecord);
    return (byte) _value;
  }

  public void setForRecord(byte _value) {
    _set(forRecord, _value);
  }

  // android.media.audio.common.AudioPolicyForceUse.CommunicationDeviceCategory forVibrateRinging;

  public static AudioPolicyForceUse forVibrateRinging(byte _value) {
    return new AudioPolicyForceUse(forVibrateRinging, _value);
  }

  public byte getForVibrateRinging() {
    _assertTag(forVibrateRinging);
    return (byte) _value;
  }

  public void setForVibrateRinging(byte _value) {
    _set(forVibrateRinging, _value);
  }

  // android.media.audio.common.AudioPolicyForceUse.DockType dock;

  public static AudioPolicyForceUse dock(byte _value) {
    return new AudioPolicyForceUse(dock, _value);
  }

  public byte getDock() {
    _assertTag(dock);
    return (byte) _value;
  }

  public void setDock(byte _value) {
    _set(dock, _value);
  }

  // boolean systemSounds;

  public static AudioPolicyForceUse systemSounds(boolean _value) {
    return new AudioPolicyForceUse(systemSounds, _value);
  }

  public boolean getSystemSounds() {
    _assertTag(systemSounds);
    return (boolean) _value;
  }

  public void setSystemSounds(boolean _value) {
    _set(systemSounds, _value);
  }

  // boolean hdmiSystemAudio;

  public static AudioPolicyForceUse hdmiSystemAudio(boolean _value) {
    return new AudioPolicyForceUse(hdmiSystemAudio, _value);
  }

  public boolean getHdmiSystemAudio() {
    _assertTag(hdmiSystemAudio);
    return (boolean) _value;
  }

  public void setHdmiSystemAudio(boolean _value) {
    _set(hdmiSystemAudio, _value);
  }

  // android.media.audio.common.AudioPolicyForceUse.EncodedSurroundConfig encodedSurround;

  public static AudioPolicyForceUse encodedSurround(byte _value) {
    return new AudioPolicyForceUse(encodedSurround, _value);
  }

  public byte getEncodedSurround() {
    _assertTag(encodedSurround);
    return (byte) _value;
  }

  public void setEncodedSurround(byte _value) {
    _set(encodedSurround, _value);
  }

  @Override
  public final int getStability() {
    return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
  }

  public static final android.os.Parcelable.Creator<AudioPolicyForceUse> CREATOR = new android.os.Parcelable.Creator<AudioPolicyForceUse>() {
    @Override
    public AudioPolicyForceUse createFromParcel(android.os.Parcel _aidl_source) {
      return new AudioPolicyForceUse(_aidl_source);
    }
    @Override
    public AudioPolicyForceUse[] newArray(int _aidl_size) {
      return new AudioPolicyForceUse[_aidl_size];
    }
  };

  @Override
  public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
    _aidl_parcel.writeInt(_tag);
    switch (_tag) {
    case forMedia:
      _aidl_parcel.writeByte(getForMedia());
      break;
    case forCommunication:
      _aidl_parcel.writeByte(getForCommunication());
      break;
    case forRecord:
      _aidl_parcel.writeByte(getForRecord());
      break;
    case forVibrateRinging:
      _aidl_parcel.writeByte(getForVibrateRinging());
      break;
    case dock:
      _aidl_parcel.writeByte(getDock());
      break;
    case systemSounds:
      _aidl_parcel.writeBoolean(getSystemSounds());
      break;
    case hdmiSystemAudio:
      _aidl_parcel.writeBoolean(getHdmiSystemAudio());
      break;
    case encodedSurround:
      _aidl_parcel.writeByte(getEncodedSurround());
      break;
    }
  }

  public void readFromParcel(android.os.Parcel _aidl_parcel) {
    int _aidl_tag;
    _aidl_tag = _aidl_parcel.readInt();
    switch (_aidl_tag) {
    case forMedia: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case forCommunication: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case forRecord: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case forVibrateRinging: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case dock: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
      _set(_aidl_tag, _aidl_value);
      return; }
    case systemSounds: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case hdmiSystemAudio: {
      boolean _aidl_value;
      _aidl_value = _aidl_parcel.readBoolean();
      _set(_aidl_tag, _aidl_value);
      return; }
    case encodedSurround: {
      byte _aidl_value;
      _aidl_value = _aidl_parcel.readByte();
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
    case forMedia: return "forMedia";
    case forCommunication: return "forCommunication";
    case forRecord: return "forRecord";
    case forVibrateRinging: return "forVibrateRinging";
    case dock: return "dock";
    case systemSounds: return "systemSounds";
    case hdmiSystemAudio: return "hdmiSystemAudio";
    case encodedSurround: return "encodedSurround";
    }
    throw new IllegalStateException("unknown field: " + _tag);
  }

  private void _set(int _tag, Object _value) {
    this._tag = _tag;
    this._value = _value;
  }
  public static @interface CommunicationDeviceCategory {
    public static final byte NONE = 0;
    public static final byte SPEAKER = 1;
    public static final byte BT_SCO = 2;
    public static final byte BT_BLE = 3;
    public static final byte WIRED_ACCESSORY = 4;
  }
  public static @interface MediaDeviceCategory {
    public static final byte NONE = 0;
    public static final byte SPEAKER = 1;
    public static final byte HEADPHONES = 2;
    public static final byte BT_A2DP = 3;
    public static final byte ANALOG_DOCK = 4;
    public static final byte DIGITAL_DOCK = 5;
    public static final byte WIRED_ACCESSORY = 6;
    public static final byte NO_BT_A2DP = 7;
  }
  public static @interface DockType {
    public static final byte NONE = 0;
    public static final byte BT_CAR_DOCK = 1;
    public static final byte BT_DESK_DOCK = 2;
    public static final byte ANALOG_DOCK = 3;
    public static final byte DIGITAL_DOCK = 4;
    public static final byte WIRED_ACCESSORY = 5;
  }
  public static @interface EncodedSurroundConfig {
    public static final byte UNSPECIFIED = 0;
    public static final byte NEVER = 1;
    public static final byte ALWAYS = 2;
    public static final byte MANUAL = 3;
  }
  public static @interface Tag {
    public static final int forMedia = 0;
    public static final int forCommunication = 1;
    public static final int forRecord = 2;
    public static final int forVibrateRinging = 3;
    public static final int dock = 4;
    public static final int systemSounds = 5;
    public static final int hdmiSystemAudio = 6;
    public static final int encodedSurround = 7;
  }
}
