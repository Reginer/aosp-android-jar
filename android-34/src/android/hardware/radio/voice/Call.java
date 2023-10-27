/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.voice;
public class Call implements android.os.Parcelable
{
  public int state = 0;
  public int index = 0;
  public int toa = 0;
  public boolean isMpty = false;
  public boolean isMT = false;
  public byte als = 0;
  public boolean isVoice = false;
  public boolean isVoicePrivacy = false;
  public java.lang.String number;
  public int numberPresentation = 0;
  public java.lang.String name;
  public int namePresentation = 0;
  public android.hardware.radio.voice.UusInfo[] uusInfo;
  public int audioQuality;
  public java.lang.String forwardedNumber;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<Call> CREATOR = new android.os.Parcelable.Creator<Call>() {
    @Override
    public Call createFromParcel(android.os.Parcel _aidl_source) {
      Call _aidl_out = new Call();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Call[] newArray(int _aidl_size) {
      return new Call[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(state);
    _aidl_parcel.writeInt(index);
    _aidl_parcel.writeInt(toa);
    _aidl_parcel.writeBoolean(isMpty);
    _aidl_parcel.writeBoolean(isMT);
    _aidl_parcel.writeByte(als);
    _aidl_parcel.writeBoolean(isVoice);
    _aidl_parcel.writeBoolean(isVoicePrivacy);
    _aidl_parcel.writeString(number);
    _aidl_parcel.writeInt(numberPresentation);
    _aidl_parcel.writeString(name);
    _aidl_parcel.writeInt(namePresentation);
    _aidl_parcel.writeTypedArray(uusInfo, _aidl_flag);
    _aidl_parcel.writeInt(audioQuality);
    _aidl_parcel.writeString(forwardedNumber);
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
      state = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      index = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      toa = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isMpty = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isMT = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      als = _aidl_parcel.readByte();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isVoice = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isVoicePrivacy = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      number = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      numberPresentation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      name = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      namePresentation = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      uusInfo = _aidl_parcel.createTypedArray(android.hardware.radio.voice.UusInfo.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      audioQuality = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      forwardedNumber = _aidl_parcel.readString();
    } finally {
      if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
        throw new android.os.BadParcelableException("Overflow in the size of parcelable");
      }
      _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
    }
  }
  public static final int PRESENTATION_ALLOWED = 0;
  public static final int PRESENTATION_RESTRICTED = 1;
  public static final int PRESENTATION_UNKNOWN = 2;
  public static final int PRESENTATION_PAYPHONE = 3;
  public static final int STATE_ACTIVE = 0;
  public static final int STATE_HOLDING = 1;
  public static final int STATE_DIALING = 2;
  public static final int STATE_ALERTING = 3;
  public static final int STATE_INCOMING = 4;
  public static final int STATE_WAITING = 5;
  @Override
  public String toString() {
    java.util.StringJoiner _aidl_sj = new java.util.StringJoiner(", ", "{", "}");
    _aidl_sj.add("state: " + (state));
    _aidl_sj.add("index: " + (index));
    _aidl_sj.add("toa: " + (toa));
    _aidl_sj.add("isMpty: " + (isMpty));
    _aidl_sj.add("isMT: " + (isMT));
    _aidl_sj.add("als: " + (als));
    _aidl_sj.add("isVoice: " + (isVoice));
    _aidl_sj.add("isVoicePrivacy: " + (isVoicePrivacy));
    _aidl_sj.add("number: " + (java.util.Objects.toString(number)));
    _aidl_sj.add("numberPresentation: " + (numberPresentation));
    _aidl_sj.add("name: " + (java.util.Objects.toString(name)));
    _aidl_sj.add("namePresentation: " + (namePresentation));
    _aidl_sj.add("uusInfo: " + (java.util.Arrays.toString(uusInfo)));
    _aidl_sj.add("audioQuality: " + (android.hardware.radio.voice.AudioQuality.$.toString(audioQuality)));
    _aidl_sj.add("forwardedNumber: " + (java.util.Objects.toString(forwardedNumber)));
    return "android.hardware.radio.voice.Call" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(uusInfo);
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
