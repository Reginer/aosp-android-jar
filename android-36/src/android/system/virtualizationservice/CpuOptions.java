/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation --min_sdk_version current -pout/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationcommon_interface/preprocessed.aidl --ninja -d out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen/android/system/virtualizationservice/CpuOptions.java.d -o out/soong/.intermediates/packages/modules/Virtualization/android/virtualizationservice/aidl/android.system.virtualizationservice-java-source/gen -Npackages/modules/Virtualization/android/virtualizationservice/aidl packages/modules/Virtualization/android/virtualizationservice/aidl/android/system/virtualizationservice/CpuOptions.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.system.virtualizationservice;
/** CPU options that will be used for the VM's Vcpus. */
public class CpuOptions implements android.os.Parcelable
{
  public android.system.virtualizationservice.CpuOptions.CpuTopology cpuTopology;
  public static final android.os.Parcelable.Creator<CpuOptions> CREATOR = new android.os.Parcelable.Creator<CpuOptions>() {
    @Override
    public CpuOptions createFromParcel(android.os.Parcel _aidl_source) {
      CpuOptions _aidl_out = new CpuOptions();
      _aidl_out.readFromParcel(_aidl_source);
      return _aidl_out;
    }
    @Override
    public CpuOptions[] newArray(int _aidl_size) {
      return new CpuOptions[_aidl_size];
    }
  };
  @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
  {
    int _aidl_start_pos = _aidl_parcel.dataPosition();
    _aidl_parcel.writeInt(0);
    _aidl_parcel.writeTypedObject(cpuTopology, _aidl_flag);
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
      cpuTopology = _aidl_parcel.readTypedObject(android.system.virtualizationservice.CpuOptions.CpuTopology.CREATOR);
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
    _mask |= describeContents(cpuTopology);
    return _mask;
  }
  private int describeContents(Object _v) {
    if (_v == null) return 0;
    if (_v instanceof android.os.Parcelable) {
      return ((android.os.Parcelable) _v).describeContents();
    }
    return 0;
  }
  public static final class CpuTopology implements android.os.Parcelable {
    // tags for union fields
    public final static int cpuCount = 0;  // int cpuCount;
    public final static int matchHost = 1;  // boolean matchHost;

    private int _tag;
    private Object _value;

    public CpuTopology() {
      int _value = 1;
      this._tag = cpuCount;
      this._value = _value;
    }

    private CpuTopology(android.os.Parcel _aidl_parcel) {
      readFromParcel(_aidl_parcel);
    }

    private CpuTopology(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }

    public int getTag() {
      return _tag;
    }

    // int cpuCount;

    /** Number of Vcpus to boot the VM with. */
    public static CpuTopology cpuCount(int _value) {
      return new CpuTopology(cpuCount, _value);
    }

    public int getCpuCount() {
      _assertTag(cpuCount);
      return (int) _value;
    }

    public void setCpuCount(int _value) {
      _set(cpuCount, _value);
    }

    // boolean matchHost;

    /** Match host number of Vcpus to boot the VM with. */
    public static CpuTopology matchHost(boolean _value) {
      return new CpuTopology(matchHost, _value);
    }

    public boolean getMatchHost() {
      _assertTag(matchHost);
      return (boolean) _value;
    }

    public void setMatchHost(boolean _value) {
      _set(matchHost, _value);
    }

    public static final android.os.Parcelable.Creator<CpuTopology> CREATOR = new android.os.Parcelable.Creator<CpuTopology>() {
      @Override
      public CpuTopology createFromParcel(android.os.Parcel _aidl_source) {
        return new CpuTopology(_aidl_source);
      }
      @Override
      public CpuTopology[] newArray(int _aidl_size) {
        return new CpuTopology[_aidl_size];
      }
    };

    @Override
    public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag) {
      _aidl_parcel.writeInt(_tag);
      switch (_tag) {
      case cpuCount:
        _aidl_parcel.writeInt(getCpuCount());
        break;
      case matchHost:
        _aidl_parcel.writeBoolean(getMatchHost());
        break;
      }
    }

    public void readFromParcel(android.os.Parcel _aidl_parcel) {
      int _aidl_tag;
      _aidl_tag = _aidl_parcel.readInt();
      switch (_aidl_tag) {
      case cpuCount: {
        int _aidl_value;
        _aidl_value = _aidl_parcel.readInt();
        _set(_aidl_tag, _aidl_value);
        return; }
      case matchHost: {
        boolean _aidl_value;
        _aidl_value = _aidl_parcel.readBoolean();
        _set(_aidl_tag, _aidl_value);
        return; }
      }
      throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
    }

    @Override
    public int describeContents() {
      int _mask = 0;
      switch (getTag()) {
      }
      return _mask;
    }

    private void _assertTag(int tag) {
      if (getTag() != tag) {
        throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
      }
    }

    private String _tagString(int _tag) {
      switch (_tag) {
      case cpuCount: return "cpuCount";
      case matchHost: return "matchHost";
      }
      throw new IllegalStateException("unknown field: " + _tag);
    }

    private void _set(int _tag, Object _value) {
      this._tag = _tag;
      this._value = _value;
    }
    public static @interface Tag {
      /** Number of Vcpus to boot the VM with. */
      public static final int cpuCount = 0;
      /** Match host number of Vcpus to boot the VM with. */
      public static final int matchHost = 1;
    }
  }
}
