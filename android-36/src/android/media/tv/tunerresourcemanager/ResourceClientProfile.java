/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current --ninja -d out/soong/.intermediates/frameworks/base/media/java/android/media/tv/tunerresourcemanager/tv_tuner_resource_manager_aidl_interface-java-source/gen/android/media/tv/tunerresourcemanager/ResourceClientProfile.java.d -o out/soong/.intermediates/frameworks/base/media/java/android/media/tv/tunerresourcemanager/tv_tuner_resource_manager_aidl_interface-java-source/gen -Nframeworks/base/media/java/android/media/tv/tunerresourcemanager/aidl frameworks/base/media/java/android/media/tv/tunerresourcemanager/aidl/android/media/tv/tunerresourcemanager/ResourceClientProfile.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.media.tv.tunerresourcemanager;
/**
 * A profile of a resource client. This profile is used to register the client info
 * with the Tuner Resource Manager.
 * 
 * @hide
 */
public class ResourceClientProfile implements android.os.Parcelable
{
  public java.lang.String tvInputSessionId;
  public int useCase = 0;
  public static final android.os.Parcelable.Creator<ResourceClientProfile> CREATOR = new android.os.Parcelable.Creator<ResourceClientProfile>() {
    @Override
    public ResourceClientProfile createFromParcel(android.os.Parcel _aidl_source) {
      ResourceClientProfile _aidl_out = new ResourceClientProfile();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public ResourceClientProfile[] newArray(int _aidl_size) {
      return new ResourceClientProfile[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeString(tvInputSessionId);
    _aidl_parcel.writeInt(useCase);
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
      tvInputSessionId = _aidl_parcel.readString();
      if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
      useCase = _aidl_parcel.readInt();
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
