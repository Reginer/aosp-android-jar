/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen/android/os/instrumentation/ExecutableMethodFileOffsets.java.d -o out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen -Nframeworks/base/core/java frameworks/base/core/java/android/os/instrumentation/ExecutableMethodFileOffsets.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.os.instrumentation;
/**
 * Represents the location of the code for a compiled method within a process'
 * memory.
 * {@hide}
 */
public class ExecutableMethodFileOffsets implements android.os.Parcelable
{
  /** The OS path of the containing file (could be virtual). */
  public java.lang.String containerPath;
  /** The offset of the containing file within the process' memory. */
  public long containerOffset = 0L;
  /** The offset of the method within the containing file. */
  public long methodOffset = 0L;
  public static final android.os.Parcelable.Creator<ExecutableMethodFileOffsets> CREATOR = new android.os.Parcelable.Creator<ExecutableMethodFileOffsets>() {
    @Override
    public ExecutableMethodFileOffsets createFromParcel(android.os.Parcel _aidl_source) {
      ExecutableMethodFileOffsets _aidl_out = new ExecutableMethodFileOffsets();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ExecutableMethodFileOffsets[] newArray(int _aidl_size) {
      return new ExecutableMethodFileOffsets[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(containerPath);
    _aidl_parcel.writeLong(containerOffset);
    _aidl_parcel.writeLong(methodOffset);
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
      containerPath = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      containerOffset = _aidl_parcel.readLong();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      methodOffset = _aidl_parcel.readLong();
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
    _aidl_sj.add("containerPath: " + (java.util.Objects.toString(containerPath)));
    _aidl_sj.add("containerOffset: " + (containerOffset));
    _aidl_sj.add("methodOffset: " + (methodOffset));
    return "ExecutableMethodFileOffsets" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
