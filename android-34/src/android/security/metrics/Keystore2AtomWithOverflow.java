/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.metrics;
/**
 * Logs the atom id of the atoms associated with key creation/operation events, that have reached
 * the maximum storage limit allocated for different atom objects of that atom,
 * in keystore in-memory store.
 * 
 * Size of the storage bucket for each atom is limited considering their expected cardinaltity.
 * This limit may exceed if the dimensions of the atoms take a large number of unexpected
 * combinations. This atom is used to track such cases.
 * @hide
 */
public class Keystore2AtomWithOverflow implements android.os.Parcelable
{
  public int atom_id;
  public static final android.os.Parcelable.Creator<Keystore2AtomWithOverflow> CREATOR = new android.os.Parcelable.Creator<Keystore2AtomWithOverflow>() {
    @Override
    public Keystore2AtomWithOverflow createFromParcel(android.os.Parcel _aidl_source) {
      Keystore2AtomWithOverflow _aidl_out = new Keystore2AtomWithOverflow();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public Keystore2AtomWithOverflow[] newArray(int _aidl_size) {
      return new Keystore2AtomWithOverflow[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(atom_id);
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
      atom_id = _aidl_parcel.readInt();
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
