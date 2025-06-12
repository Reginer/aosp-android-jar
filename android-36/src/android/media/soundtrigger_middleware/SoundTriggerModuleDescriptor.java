/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.soundtrigger.types_interface/3/preprocessed.aidl -pout/soong/.intermediates/system/hardware/interfaces/media/android.media.audio.common.types_interface/4/preprocessed.aidl -pout/soong/.intermediates/frameworks/base/media/media_permission-aidl_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/frameworks/base/media/soundtrigger_middleware-aidl-java-source/gen/android/media/soundtrigger_middleware/SoundTriggerModuleDescriptor.java.d -o out/soong/.intermediates/frameworks/base/media/soundtrigger_middleware-aidl-java-source/gen -Nframeworks/base/media/aidl frameworks/base/media/aidl/android/media/soundtrigger_middleware/SoundTriggerModuleDescriptor.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.soundtrigger_middleware;
/**
 * A descriptor of an available sound trigger module, containing the handle used to reference the
 * module, as well its capabilities.
 * {@hide}
 */
public class SoundTriggerModuleDescriptor implements android.os.Parcelable
{
  /** Module handle to be used for attaching to it. */
  public int handle = 0;
  /** Module capabilities. */
  public android.media.soundtrigger.Properties properties;
  public static final android.os.Parcelable.Creator<SoundTriggerModuleDescriptor> CREATOR = new android.os.Parcelable.Creator<SoundTriggerModuleDescriptor>() {
    @Override
    public SoundTriggerModuleDescriptor createFromParcel(android.os.Parcel _aidl_source) {
      SoundTriggerModuleDescriptor _aidl_out = new SoundTriggerModuleDescriptor();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public SoundTriggerModuleDescriptor[] newArray(int _aidl_size) {
      return new SoundTriggerModuleDescriptor[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(handle);
    _aidl_parcel.writeTypedObject(properties, _aidl_flag);
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
      handle = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      properties = _aidl_parcel.readTypedObject(android.media.soundtrigger.Properties.CREATOR);
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
    _aidl_sj.add("handle: " + (handle));
    _aidl_sj.add("properties: " + (java.util.Objects.toString(properties)));
    return "SoundTriggerModuleDescriptor" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof SoundTriggerModuleDescriptor)) return false;
    SoundTriggerModuleDescriptor that = (SoundTriggerModuleDescriptor)other;
    if (!java.util.Objects.deepEquals(handle, that.handle)) return false;
    if (!java.util.Objects.deepEquals(properties, that.properties)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(handle, properties).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    _mask |= describeContents(properties);
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
