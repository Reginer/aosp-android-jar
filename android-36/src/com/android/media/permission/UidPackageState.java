/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/av/audio-permission-aidl-java-source/gen/com/android/media/permission/UidPackageState.java.d -o out/soong/.intermediates/frameworks/av/audio-permission-aidl-java-source/gen -Nframeworks/av/aidl frameworks/av/aidl/com/android/media/permission/UidPackageState.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.android.media.permission;
/**
 * Entity representing the package names associated with a particular uid/app-id
 * {@hide}
 */
public class UidPackageState implements android.os.Parcelable
{
  public int uid = 0;
  public java.util.List<java.lang.String> packageNames;
  public static final android.os.Parcelable.Creator<UidPackageState> CREATOR = new android.os.Parcelable.Creator<UidPackageState>() {
    @Override
    public UidPackageState createFromParcel(android.os.Parcel _aidl_source) {
      UidPackageState _aidl_out = new UidPackageState();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public UidPackageState[] newArray(int _aidl_size) {
      return new UidPackageState[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeInt(uid);
    _aidl_parcel.writeStringList(packageNames);
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
      packageNames = _aidl_parcel.createStringArrayList();
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
    _aidl_sj.add("packageNames: " + (java.util.Objects.toString(packageNames)));
    return "UidPackageState" + _aidl_sj.toString()  ;
  }
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof UidPackageState)) return false;
    UidPackageState that = (UidPackageState)other;
    if (!java.util.Objects.deepEquals(uid, that.uid)) return false;
    if (!java.util.Objects.deepEquals(packageNames, that.packageNames)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.deepHashCode(java.util.Arrays.asList(uid, packageNames).toArray());
  }
  @Override
  public int describeContents() {
    int _mask = 0;
    return _mask;
  }
}
