/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The "Internal" suffix of this type name is to disambiguate it from the
 * android.media.AudioAttributes SDK type.
 * {@hide}
 */
public class AudioAttributesInternal implements android.os.Parcelable
{
  public int contentType;
  public int usage;
  public int source;
  // Bitmask, indexed by AudioFlag.
  public int flags = 0;
  public java.lang.String tags;
  public static final android.os.Parcelable.Creator<AudioAttributesInternal> CREATOR = new android.os.Parcelable.Creator<AudioAttributesInternal>() {
    @Override
    public AudioAttributesInternal createFromParcel(android.os.Parcel _aidl_source) {
      AudioAttributesInternal _aidl_out = new AudioAttributesInternal();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioAttributesInternal[] newArray(int _aidl_size) {
      return new AudioAttributesInternal[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(contentType);
    _aidl_parcel.writeInt(usage);
    _aidl_parcel.writeInt(source);
    _aidl_parcel.writeInt(flags);
    _aidl_parcel.writeString(tags);
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
      contentType = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      usage = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      source = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      flags = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      tags = _aidl_parcel.readString();
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
