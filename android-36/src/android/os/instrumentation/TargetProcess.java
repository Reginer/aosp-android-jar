/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen/android/os/instrumentation/TargetProcess.java.d -o out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen -Nframeworks/base/core/java frameworks/base/core/java/android/os/instrumentation/TargetProcess.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.os.instrumentation;
/**
 * Addresses a process that would run on the device.
 * Helps disambiguate targeted processes in cases of pid re-use.
 * {@hide}
 */
public class TargetProcess implements android.os.Parcelable
{
  public int uid = 0;
  public int pid = 0;
  public java.lang.String processName;
  public static final android.os.Parcelable.Creator<TargetProcess> CREATOR = new android.os.Parcelable.Creator<TargetProcess>() {
    @Override
    public TargetProcess createFromParcel(android.os.Parcel _aidl_source) {
      TargetProcess _aidl_out = new TargetProcess();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public TargetProcess[] newArray(int _aidl_size) {
      return new TargetProcess[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(uid);
    _aidl_parcel.writeInt(pid);
    _aidl_parcel.writeString(processName);
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
      uid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      pid = _aidl_parcel.readInt();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      processName = _aidl_parcel.readString();
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
    _aidl_sj.add("uid: " + (uid));
    _aidl_sj.add("pid: " + (pid));
    _aidl_sj.add("processName: " + (java.util.Objects.toString(processName)));
    return "TargetProcess" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
