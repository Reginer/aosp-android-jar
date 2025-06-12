/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen/android/os/instrumentation/MethodDescriptor.java.d -o out/soong/.intermediates/frameworks/base/core/java/dynamic_instrumentation_manager_aidl-java-source/gen -Nframeworks/base/core/java frameworks/base/core/java/android/os/instrumentation/MethodDescriptor.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.os.instrumentation;
/**
 * Represents a JVM method, where class fields that make up its signature.
 * {@hide}
 */
public class MethodDescriptor implements android.os.Parcelable
{
  /** Fully qualified class in reverse.domain.Naming */
  public java.lang.String fullyQualifiedClassName;
  /** Name of the method. */
  public java.lang.String methodName;
  /** Fully qualified types of method parameters, or string representations if primitive e.g. "int". */
  public java.lang.String[] fullyQualifiedParameters;
  public static final android.os.Parcelable.Creator<MethodDescriptor> CREATOR = new android.os.Parcelable.Creator<MethodDescriptor>() {
    @Override
    public MethodDescriptor createFromParcel(android.os.Parcel _aidl_source) {
      MethodDescriptor _aidl_out = new MethodDescriptor();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public MethodDescriptor[] newArray(int _aidl_size) {
      return new MethodDescriptor[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(fullyQualifiedClassName);
    _aidl_parcel.writeString(methodName);
    _aidl_parcel.writeStringArray(fullyQualifiedParameters);
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
      fullyQualifiedClassName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      methodName = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      fullyQualifiedParameters = _aidl_parcel.createStringArray();
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
    _aidl_sj.add("fullyQualifiedClassName: " + (java.util.Objects.toString(fullyQualifiedClassName)));
    _aidl_sj.add("methodName: " + (java.util.Objects.toString(methodName)));
    _aidl_sj.add("fullyQualifiedParameters: " + (java.util.Arrays.toString(fullyQualifiedParameters)));
    return "MethodDescriptor" + _aidl_sj.toString()  ;
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
