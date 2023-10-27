/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * TODO(b/280077672): This is a temporary copy of the stable
 * android.hardware.audio.core.AudioRoute. Interfaces from the Core API do not
 * support the CPP backend. This copy will be removed either by moving the
 * AudioRoute from core to a.m.a.common or by switching the framework internal
 * interfaces to the NDK backend.
 * {@hide}
 */
public class AudioRoute implements android.os.Parcelable
{
  /**
   * The list of IDs of source audio ports ('AudioPort.id').
   * There must be at least one source in a valid route and all IDs must be
   * unique.
   */
  public int[] sourcePortIds;
  /** The ID of the sink audio port ('AudioPort.id'). */
  public int sinkPortId = 0;
  /** If set, only one source can be active, mixing is not supported. */
  public boolean isExclusive = false;
  public static final android.os.Parcelable.Creator<AudioRoute> CREATOR = new android.os.Parcelable.Creator<AudioRoute>() {
    @Override
    public AudioRoute createFromParcel(android.os.Parcel _aidl_source) {
      AudioRoute _aidl_out = new AudioRoute();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public AudioRoute[] newArray(int _aidl_size) {
      return new AudioRoute[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeIntArray(sourcePortIds);
    _aidl_parcel.writeInt(sinkPortId);
    _aidl_parcel.writeBoolean(isExclusive);
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
      sourcePortIds = _aidl_parcel.createIntArray();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      sinkPortId = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      isExclusive = _aidl_parcel.readBoolean();
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
