/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media.soundtrigger_middleware;
/**
 * Wrapper to android.media.soundtrigger.RecognitionEvent providing additional fields used by the
 * framework.
 */
public class PhraseRecognitionEventSys implements android.os.Parcelable
{
  public android.media.soundtrigger.PhraseRecognitionEvent phraseRecognitionEvent;
  /**
   * Timestamp of when the trigger event from SoundTriggerHal was received by the
   * framework.
   * 
   * <p>same units and timebase as {@link SystemClock#elapsedRealtime()}.
   * The value will be -1 if the event was not generated from the HAL.
   */
  // @ElapsedRealtimeLong
  public long halEventReceivedMillis = -1L;
  /**
   * Token relating this event to a particular recognition session, returned by
   * {@link ISoundTriggerModule.startRecognition(int, RecognitionConfig}
   */
  public android.os.IBinder token;
  public static final android.os.Parcelable.Creator<PhraseRecognitionEventSys> CREATOR = new android.os.Parcelable.Creator<PhraseRecognitionEventSys>() {
    @Override
    public PhraseRecognitionEventSys createFromParcel(android.os.Parcel _aidl_source) {
      PhraseRecognitionEventSys _aidl_out = new PhraseRecognitionEventSys();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public PhraseRecognitionEventSys[] newArray(int _aidl_size) {
      return new PhraseRecognitionEventSys[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(phraseRecognitionEvent, _aidl_flag);
    _aidl_parcel.writeLong(halEventReceivedMillis);
    _aidl_parcel.writeStrongBinder(token);
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
      phraseRecognitionEvent = _aidl_parcel.readTypedObject(android.media.soundtrigger.PhraseRecognitionEvent.CREATOR);
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      halEventReceivedMillis = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      token = _aidl_parcel.readStrongBinder();
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
    _aidl_sj.add("phraseRecognitionEvent: " + (java.util.Objects.toString(phraseRecognitionEvent)));
    _aidl_sj.add("halEventReceivedMillis: " + (halEventReceivedMillis));
    _aidl_sj.add("token: " + (java.util.Objects.toString(token)));
    return "android.media.soundtrigger_middleware.PhraseRecognitionEventSys" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof PhraseRecognitionEventSys)) return false;
    PhraseRecognitionEventSys that = (PhraseRecognitionEventSys)other;
    if (!java.util.Objects.deepEquals(phraseRecognitionEvent, that.phraseRecognitionEvent)) return false;
    if (!java.util.Objects.deepEquals(halEventReceivedMillis, that.halEventReceivedMillis)) return false;
    if (!java.util.Objects.deepEquals(token, that.token)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(phraseRecognitionEvent, halEventReceivedMillis, token).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(phraseRecognitionEvent);
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
