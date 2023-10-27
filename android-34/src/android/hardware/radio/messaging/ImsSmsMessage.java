/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.radio.messaging;
public class ImsSmsMessage implements android.os.Parcelable
{
  public int tech;
  public boolean retry = false;
  public int messageRef = 0;
  public android.hardware.radio.messaging.CdmaSmsMessage[] cdmaMessage;
  public android.hardware.radio.messaging.GsmSmsMessage[] gsmMessage;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ImsSmsMessage> CREATOR = new android.os.Parcelable.Creator<ImsSmsMessage>() {
    @Override
    public ImsSmsMessage createFromParcel(android.os.Parcel _aidl_source) {
      ImsSmsMessage _aidl_out = new ImsSmsMessage();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ImsSmsMessage[] newArray(int _aidl_size) {
      return new ImsSmsMessage[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(tech);
    _aidl_parcel.writeBoolean(retry);
    _aidl_parcel.writeInt(messageRef);
    _aidl_parcel.writeTypedArray(cdmaMessage, _aidl_flag);
    _aidl_parcel.writeTypedArray(gsmMessage, _aidl_flag);
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
      tech = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      retry = _aidl_parcel.readBoolean();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      messageRef = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      cdmaMessage = _aidl_parcel.createTypedArray(android.hardware.radio.messaging.CdmaSmsMessage.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      gsmMessage = _aidl_parcel.createTypedArray(android.hardware.radio.messaging.GsmSmsMessage.CREATOR);
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
    _aidl_sj.add("tech: " + (android.hardware.radio.RadioTechnologyFamily.$.toString(tech)));
    _aidl_sj.add("retry: " + (retry));
    _aidl_sj.add("messageRef: " + (messageRef));
    _aidl_sj.add("cdmaMessage: " + (java.util.Arrays.toString(cdmaMessage)));
    _aidl_sj.add("gsmMessage: " + (java.util.Arrays.toString(gsmMessage)));
    return "android.hardware.radio.messaging.ImsSmsMessage" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(cdmaMessage);
    _mask |= describeContents(gsmMessage);
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
