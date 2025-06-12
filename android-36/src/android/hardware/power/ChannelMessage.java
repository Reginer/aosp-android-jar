/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 6 --hash 13171cf98a48de298baf85167633376ea3db4ea0 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/common/fmq/aidl/android.hardware.common.fmq_interface/1/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen/android/hardware/power/ChannelMessage.java.d -o out/soong/.intermediates/hardware/interfaces/power/aidl/android.hardware.power-V6-java-source/gen -Nhardware/interfaces/power/aidl/aidl_api/android.hardware.power/6 hardware/interfaces/power/aidl/aidl_api/android.hardware.power/6/android/hardware/power/ChannelMessage.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.power;
public class ChannelMessage implements android.os.Parcelable
{
  public int sessionID = 0;
  public long timeStampNanos = 0L;
  public android.hardware.power.ChannelMessage.ChannelMessageContents data;
  @Override
   public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
  public static final android.os.Parcelable.Creator<ChannelMessage> CREATOR = new android.os.Parcelable.Creator<ChannelMessage>() {
    @Override
    public ChannelMessage createFromParcel(android.os.Parcel _aidl_source) {
      ChannelMessage _aidl_out = new ChannelMessage();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ChannelMessage[] newArray(int _aidl_size) {
      return new ChannelMessage[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(sessionID);
    _aidl_parcel.writeLong(timeStampNanos);
    _aidl_parcel.writeTypedObject(data, _aidl_flag);
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
      sessionID = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      timeStampNanos = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      data = _aidl_parcel.readTypedObject(android.hardware.power.ChannelMessage.ChannelMessageContents.CREATOR);
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
    _mask |= describeContents(data);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
  public static final class ChannelMessageContents implements android.os.Parcelable {
    // tags for union fields
    public final static int reserved = 0;  // long[16] reserved;
    public final static int targetDuration = 1;  // long targetDuration;
    public final static int hint = 2;  // android.hardware.power.SessionHint hint;
    public final static int mode = 3;  // android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter mode;
    public final static int workDuration = 4;  // android.hardware.power.WorkDurationFixedV1 workDuration;

    private int _tag;
    private Object _value;

    public ChannelMessageContents() {
      long[] _value = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
      this._tag = reserved;
      this._value = _value;
    }

    private ChannelMessageContents(android.os.Parcel _aidl_parcel) {
      readFromParcel(_aidl_parcel);
    }

    private ChannelMessageContents(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }

    public int getTag() {
      return _tag;
    }

    // long[16] reserved;

    public static ChannelMessageContents reserved(long[] _value) {
      return new ChannelMessageContents(reserved, _value);
    }

    public long[] getReserved() {
      _assertTag(reserved);
      return (long[]) _value;
    }

    public void setReserved(long[] _value) {
      _set(reserved, _value);
    }

    // long targetDuration;

    public static ChannelMessageContents targetDuration(long _value) {
      return new ChannelMessageContents(targetDuration, _value);
    }

    public long getTargetDuration() {
      _assertTag(targetDuration);
      return (long) _value;
    }

    public void setTargetDuration(long _value) {
      _set(targetDuration, _value);
    }

    // android.hardware.power.SessionHint hint;

    public static ChannelMessageContents hint(int _value) {
      return new ChannelMessageContents(hint, _value);
    }

    public int getHint() {
      _assertTag(hint);
      return (int) _value;
    }

    public void setHint(int _value) {
      _set(hint, _value);
    }

    // android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter mode;

    public static ChannelMessageContents mode(android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter _value) {
      return new ChannelMessageContents(mode, _value);
    }

    public android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter getMode() {
      _assertTag(mode);
      return (android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter) _value;
    }

    public void setMode(android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter _value) {
      _set(mode, _value);
    }

    // android.hardware.power.WorkDurationFixedV1 workDuration;

    public static ChannelMessageContents workDuration(android.hardware.power.WorkDurationFixedV1 _value) {
      return new ChannelMessageContents(workDuration, _value);
    }

    public android.hardware.power.WorkDurationFixedV1 getWorkDuration() {
      _assertTag(workDuration);
      return (android.hardware.power.WorkDurationFixedV1) _value;
    }

    public void setWorkDuration(android.hardware.power.WorkDurationFixedV1 _value) {
      _set(workDuration, _value);
    }

    @Override
    public final int getStability() {
      return android.os.Parcelable.PARCELABLE_STABILITY_VINTF;
    }

    public static final android.os.Parcelable.Creator<ChannelMessageContents> CREATOR = new android.os.Parcelable.Creator<ChannelMessageContents>() {
      @Override
      public ChannelMessageContents createFromParcel(android.os.Parcel _aidl_source) {
        return new ChannelMessageContents(_aidl_source);
      }
      @Override
      public ChannelMessageContents[] newArray(int _aidl_size) {
        return new ChannelMessageContents[_aidl_size];
      }
    };

    @Override
    public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
      _aidl_parcel.writeInt(_tag);
      switch (_tag) {
      case reserved:
        _aidl_parcel.writeFixedArray(getReserved(), _aidl_flag, 16);
        break;
      case targetDuration:
        _aidl_parcel.writeLong(getTargetDuration());
        break;
      case hint:
        _aidl_parcel.writeInt(getHint());
        break;
      case mode:
        _aidl_parcel.writeTypedObject(getMode(), _aidl_flag);
        break;
      case workDuration:
        _aidl_parcel.writeTypedObject(getWorkDuration(), _aidl_flag);
        break;
      }
    }

    public void readFromParcel(android.os.Parcel _aidl_parcel) {
      int _aidl_tag;
      _aidl_tag = _aidl_parcel.readInt();
      switch (_aidl_tag) {
      case reserved: {
        long[] _aidl_value;
        _aidl_value = _aidl_parcel.createFixedArray(long[].class, 16);
        _set(_aidl_tag, _aidl_value);
        return; }
      case targetDuration: {
        long _aidl_value;
        _aidl_value = _aidl_parcel.readLong();
        _set(_aidl_tag, _aidl_value);
        return; }
      case hint: {
        int _aidl_value;
        _aidl_value = _aidl_parcel.readInt();
        _set(_aidl_tag, _aidl_value);
        return; }
      case mode: {
        android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter _aidl_value;
        _aidl_value = _aidl_parcel.readTypedObject(android.hardware.power.ChannelMessage.ChannelMessageContents.SessionModeSetter.CREATOR);
        _set(_aidl_tag, _aidl_value);
        return; }
      case workDuration: {
        android.hardware.power.WorkDurationFixedV1 _aidl_value;
        _aidl_value = _aidl_parcel.readTypedObject(android.hardware.power.WorkDurationFixedV1.CREATOR);
        _set(_aidl_tag, _aidl_value);
        return; }
      }
      throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
    }

    @Override
    public int describeContents() {
      int _mask = 0;
      switch (getTag()) {
      case mode:
        _mask |= describeContents(getMode());
        break;
      case workDuration:
        _mask |= describeContents(getWorkDuration());
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
      case reserved: return "reserved";
      case targetDuration: return "targetDuration";
      case hint: return "hint";
      case mode: return "mode";
      case workDuration: return "workDuration";
      }
      throw new IllegalStateException("unknown field: " + _tag);
    }

    private void _set(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }
    public static class SessionModeSetter implements android.os.Parcelable
    {
      public int modeInt;
      public boolean enabled = false;
      @Override
       public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
      public static final android.os.Parcelable.Creator<SessionModeSetter> CREATOR = new android.os.Parcelable.Creator<SessionModeSetter>() {
        @Override
        public SessionModeSetter createFromParcel(android.os.Parcel _aidl_source) {
          SessionModeSetter _aidl_out = new SessionModeSetter();
          _aidl_out.readFromParcel(_aidl_source);
          return _aidl_out;
        }
        @Override
        public SessionModeSetter[] newArray(int _aidl_size) {
          return new SessionModeSetter[_aidl_size];
        }
      };
      @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
      {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(modeInt);
        _aidl_parcel.writeBoolean(enabled);
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
          modeInt = _aidl_parcel.readInt();
          if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
          enabled = _aidl_parcel.readBoolean();
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
      public static final byte reserved = 0;
      public static final byte targetDuration = 1;
      public static final byte hint = 2;
      public static final byte mode = 3;
      public static final byte workDuration = 4;
    }
  }
}
