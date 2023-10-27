/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public interface IGnssDebug extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IGnssDebug. */
  public static class Default implements android.hardware.gnss.IGnssDebug
  {
    @Override public android.hardware.gnss.IGnssDebug.DebugData getDebugData() throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public int getInterfaceVersion() {
      return 0;
    }
    @Override
    public String getInterfaceHash() {
      return "";
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.IGnssDebug
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.IGnssDebug interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.IGnssDebug asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.IGnssDebug))) {
        return ((android.hardware.gnss.IGnssDebug)iin);
      }
      return new android.hardware.gnss.IGnssDebug.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    /** @hide */
    public static java.lang.String getDefaultTransactionName(int transactionCode)
    {
      switch (transactionCode)
      {
        case TRANSACTION_getDebugData:
        {
          return "getDebugData";
        }
        case TRANSACTION_getInterfaceVersion:
        {
          return "getInterfaceVersion";
        }
        case TRANSACTION_getInterfaceHash:
        {
          return "getInterfaceHash";
        }
        default:
        {
          return null;
        }
      }
    }
    /** @hide */
    public java.lang.String getTransactionName(int transactionCode)
    {
      return this.getDefaultTransactionName(transactionCode);
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_getInterfaceVersion:
        {
          reply.writeNoException();
          reply.writeInt(getInterfaceVersion());
          return true;
        }
        case TRANSACTION_getInterfaceHash:
        {
          reply.writeNoException();
          reply.writeString(getInterfaceHash());
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_getDebugData:
        {
          android.hardware.gnss.IGnssDebug.DebugData _result = this.getDebugData();
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.gnss.IGnssDebug
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      private int mCachedVersion = -1;
      private String mCachedHash = "-1";
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public android.hardware.gnss.IGnssDebug.DebugData getDebugData() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.gnss.IGnssDebug.DebugData _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDebugData, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getDebugData is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.gnss.IGnssDebug.DebugData.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override
      public int getInterfaceVersion() throws android.os.RemoteException {
        if (mCachedVersion == -1) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
            reply.readException();
            mCachedVersion = reply.readInt();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedVersion;
      }
      @Override
      public synchronized String getInterfaceHash() throws android.os.RemoteException {
        if ("-1".equals(mCachedHash)) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
            reply.readException();
            mCachedHash = reply.readString();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedHash;
      }
    }
    static final int TRANSACTION_getDebugData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$IGnssDebug".replace('$', '.');
  public android.hardware.gnss.IGnssDebug.DebugData getDebugData() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
  public static @interface SatelliteEphemerisType {
    public static final int EPHEMERIS = 0;
    public static final int ALMANAC_ONLY = 1;
    public static final int NOT_AVAILABLE = 2;
  }
  public static @interface SatelliteEphemerisHealth {
    public static final int GOOD = 0;
    public static final int BAD = 1;
    public static final int UNKNOWN = 2;
  }
  public static class TimeDebug implements android.os.Parcelable
  {
    public long timeEstimateMs = 0L;
    public float timeUncertaintyNs = 0.000000f;
    public float frequencyUncertaintyNsPerSec = 0.000000f;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<TimeDebug> CREATOR = new android.os.Parcelable.Creator<TimeDebug>() {
      @Override
      public TimeDebug createFromParcel(android.os.Parcel _aidl_source) {
        TimeDebug _aidl_out = new TimeDebug();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public TimeDebug[] newArray(int _aidl_size) {
        return new TimeDebug[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeLong(timeEstimateMs);
      _aidl_parcel.writeFloat(timeUncertaintyNs);
      _aidl_parcel.writeFloat(frequencyUncertaintyNsPerSec);
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
        timeEstimateMs = _aidl_parcel.readLong();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        timeUncertaintyNs = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        frequencyUncertaintyNsPerSec = _aidl_parcel.readFloat();
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
  public static class PositionDebug implements android.os.Parcelable
  {
    public boolean valid = false;
    public double latitudeDegrees = 0.000000;
    public double longitudeDegrees = 0.000000;
    public float altitudeMeters = 0.000000f;
    public float speedMetersPerSec = 0.000000f;
    public float bearingDegrees = 0.000000f;
    public double horizontalAccuracyMeters = 0.000000;
    public double verticalAccuracyMeters = 0.000000;
    public double speedAccuracyMetersPerSecond = 0.000000;
    public double bearingAccuracyDegrees = 0.000000;
    public float ageSeconds = 0.000000f;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<PositionDebug> CREATOR = new android.os.Parcelable.Creator<PositionDebug>() {
      @Override
      public PositionDebug createFromParcel(android.os.Parcel _aidl_source) {
        PositionDebug _aidl_out = new PositionDebug();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public PositionDebug[] newArray(int _aidl_size) {
        return new PositionDebug[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeBoolean(valid);
      _aidl_parcel.writeDouble(latitudeDegrees);
      _aidl_parcel.writeDouble(longitudeDegrees);
      _aidl_parcel.writeFloat(altitudeMeters);
      _aidl_parcel.writeFloat(speedMetersPerSec);
      _aidl_parcel.writeFloat(bearingDegrees);
      _aidl_parcel.writeDouble(horizontalAccuracyMeters);
      _aidl_parcel.writeDouble(verticalAccuracyMeters);
      _aidl_parcel.writeDouble(speedAccuracyMetersPerSecond);
      _aidl_parcel.writeDouble(bearingAccuracyDegrees);
      _aidl_parcel.writeFloat(ageSeconds);
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
        valid = _aidl_parcel.readBoolean();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        latitudeDegrees = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        longitudeDegrees = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        altitudeMeters = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        speedMetersPerSec = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        bearingDegrees = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        horizontalAccuracyMeters = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        verticalAccuracyMeters = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        speedAccuracyMetersPerSecond = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        bearingAccuracyDegrees = _aidl_parcel.readDouble();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        ageSeconds = _aidl_parcel.readFloat();
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
  public static class SatelliteData implements android.os.Parcelable
  {
    public int svid = 0;
    public int constellation;
    public int ephemerisType;
    public int ephemerisSource;
    public int ephemerisHealth;
    public float ephemerisAgeSeconds = 0.000000f;
    public boolean serverPredictionIsAvailable = false;
    public float serverPredictionAgeSeconds = 0.000000f;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<SatelliteData> CREATOR = new android.os.Parcelable.Creator<SatelliteData>() {
      @Override
      public SatelliteData createFromParcel(android.os.Parcel _aidl_source) {
        SatelliteData _aidl_out = new SatelliteData();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public SatelliteData[] newArray(int _aidl_size) {
        return new SatelliteData[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(svid);
      _aidl_parcel.writeInt(constellation);
      _aidl_parcel.writeInt(ephemerisType);
      _aidl_parcel.writeInt(ephemerisSource);
      _aidl_parcel.writeInt(ephemerisHealth);
      _aidl_parcel.writeFloat(ephemerisAgeSeconds);
      _aidl_parcel.writeBoolean(serverPredictionIsAvailable);
      _aidl_parcel.writeFloat(serverPredictionAgeSeconds);
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
        svid = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        constellation = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        ephemerisType = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        ephemerisSource = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        ephemerisHealth = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        ephemerisAgeSeconds = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        serverPredictionIsAvailable = _aidl_parcel.readBoolean();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        serverPredictionAgeSeconds = _aidl_parcel.readFloat();
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
  public static class DebugData implements android.os.Parcelable
  {
    public android.hardware.gnss.IGnssDebug.PositionDebug position;
    public android.hardware.gnss.IGnssDebug.TimeDebug time;
    public java.util.List<android.hardware.gnss.IGnssDebug.SatelliteData> satelliteDataArray;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<DebugData> CREATOR = new android.os.Parcelable.Creator<DebugData>() {
      @Override
      public DebugData createFromParcel(android.os.Parcel _aidl_source) {
        DebugData _aidl_out = new DebugData();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public DebugData[] newArray(int _aidl_size) {
        return new DebugData[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeTypedObject(position, _aidl_flag);
      _aidl_parcel.writeTypedObject(time, _aidl_flag);
      _aidl_parcel.writeTypedList(satelliteDataArray, _aidl_flag);
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
        position = _aidl_parcel.readTypedObject(android.hardware.gnss.IGnssDebug.PositionDebug.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        time = _aidl_parcel.readTypedObject(android.hardware.gnss.IGnssDebug.TimeDebug.CREATOR);
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        satelliteDataArray = _aidl_parcel.createTypedArrayList(android.hardware.gnss.IGnssDebug.SatelliteData.CREATOR);
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
      _mask |= describeContents(position);
      _mask |= describeContents(time);
      _mask |= describeContents(satelliteDataArray);
      return _mask;
    }
    private int describeContents(Object _v) {
      if (_v == null) return 0;
      if (_v instanceof java.util.Collection) {
        int _mask = 0;
        for (Object o : (java.util.Collection) _v) {
          _mask |= describeContents(o);
        }
        return _mask;
      }
      if (_v instanceof android.os.Parcelable) {
        return ((android.os.Parcelable) _v).describeContents();
      }
      return 0;
    }
  }
}
