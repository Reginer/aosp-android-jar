/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public interface IGnssCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IGnssCallback. */
  public static class Default implements android.hardware.gnss.IGnssCallback
  {
    @Override public void gnssSetCapabilitiesCb(int capabilities) throws android.os.RemoteException
    {
    }
    @Override public void gnssStatusCb(int status) throws android.os.RemoteException
    {
    }
    @Override public void gnssSvStatusCb(android.hardware.gnss.IGnssCallback.GnssSvInfo[] svInfoList) throws android.os.RemoteException
    {
    }
    @Override public void gnssLocationCb(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException
    {
    }
    @Override public void gnssNmeaCb(long timestamp, java.lang.String nmea) throws android.os.RemoteException
    {
    }
    @Override public void gnssAcquireWakelockCb() throws android.os.RemoteException
    {
    }
    @Override public void gnssReleaseWakelockCb() throws android.os.RemoteException
    {
    }
    @Override public void gnssSetSystemInfoCb(android.hardware.gnss.IGnssCallback.GnssSystemInfo info) throws android.os.RemoteException
    {
    }
    @Override public void gnssRequestTimeCb() throws android.os.RemoteException
    {
    }
    @Override public void gnssRequestLocationCb(boolean independentFromGnss, boolean isUserEmergency) throws android.os.RemoteException
    {
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.IGnssCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.IGnssCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.IGnssCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.IGnssCallback))) {
        return ((android.hardware.gnss.IGnssCallback)iin);
      }
      return new android.hardware.gnss.IGnssCallback.Stub.Proxy(obj);
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
        case TRANSACTION_gnssSetCapabilitiesCb:
        {
          return "gnssSetCapabilitiesCb";
        }
        case TRANSACTION_gnssStatusCb:
        {
          return "gnssStatusCb";
        }
        case TRANSACTION_gnssSvStatusCb:
        {
          return "gnssSvStatusCb";
        }
        case TRANSACTION_gnssLocationCb:
        {
          return "gnssLocationCb";
        }
        case TRANSACTION_gnssNmeaCb:
        {
          return "gnssNmeaCb";
        }
        case TRANSACTION_gnssAcquireWakelockCb:
        {
          return "gnssAcquireWakelockCb";
        }
        case TRANSACTION_gnssReleaseWakelockCb:
        {
          return "gnssReleaseWakelockCb";
        }
        case TRANSACTION_gnssSetSystemInfoCb:
        {
          return "gnssSetSystemInfoCb";
        }
        case TRANSACTION_gnssRequestTimeCb:
        {
          return "gnssRequestTimeCb";
        }
        case TRANSACTION_gnssRequestLocationCb:
        {
          return "gnssRequestLocationCb";
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
        case TRANSACTION_gnssSetCapabilitiesCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.gnssSetCapabilitiesCb(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssStatusCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.gnssStatusCb(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssSvStatusCb:
        {
          android.hardware.gnss.IGnssCallback.GnssSvInfo[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.gnss.IGnssCallback.GnssSvInfo.CREATOR);
          data.enforceNoDataAvail();
          this.gnssSvStatusCb(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssLocationCb:
        {
          android.hardware.gnss.GnssLocation _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.GnssLocation.CREATOR);
          data.enforceNoDataAvail();
          this.gnssLocationCb(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssNmeaCb:
        {
          long _arg0;
          _arg0 = data.readLong();
          java.lang.String _arg1;
          _arg1 = data.readString();
          data.enforceNoDataAvail();
          this.gnssNmeaCb(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssAcquireWakelockCb:
        {
          this.gnssAcquireWakelockCb();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssReleaseWakelockCb:
        {
          this.gnssReleaseWakelockCb();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssSetSystemInfoCb:
        {
          android.hardware.gnss.IGnssCallback.GnssSystemInfo _arg0;
          _arg0 = data.readTypedObject(android.hardware.gnss.IGnssCallback.GnssSystemInfo.CREATOR);
          data.enforceNoDataAvail();
          this.gnssSetSystemInfoCb(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssRequestTimeCb:
        {
          this.gnssRequestTimeCb();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssRequestLocationCb:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          boolean _arg1;
          _arg1 = data.readBoolean();
          data.enforceNoDataAvail();
          this.gnssRequestLocationCb(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.gnss.IGnssCallback
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
      @Override public void gnssSetCapabilitiesCb(int capabilities) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(capabilities);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssSetCapabilitiesCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssSetCapabilitiesCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssStatusCb(int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssStatusCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssStatusCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssSvStatusCb(android.hardware.gnss.IGnssCallback.GnssSvInfo[] svInfoList) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(svInfoList, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssSvStatusCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssSvStatusCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssLocationCb(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(location, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssLocationCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssLocationCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssNmeaCb(long timestamp, java.lang.String nmea) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(timestamp);
          _data.writeString(nmea);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssNmeaCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssNmeaCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssAcquireWakelockCb() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssAcquireWakelockCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssAcquireWakelockCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssReleaseWakelockCb() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssReleaseWakelockCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssReleaseWakelockCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssSetSystemInfoCb(android.hardware.gnss.IGnssCallback.GnssSystemInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssSetSystemInfoCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssSetSystemInfoCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssRequestTimeCb() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssRequestTimeCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssRequestTimeCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssRequestLocationCb(boolean independentFromGnss, boolean isUserEmergency) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(independentFromGnss);
          _data.writeBoolean(isUserEmergency);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssRequestLocationCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssRequestLocationCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
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
    static final int TRANSACTION_gnssSetCapabilitiesCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_gnssStatusCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_gnssSvStatusCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_gnssLocationCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_gnssNmeaCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_gnssAcquireWakelockCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_gnssReleaseWakelockCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_gnssSetSystemInfoCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_gnssRequestTimeCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_gnssRequestLocationCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$IGnssCallback".replace('$', '.');
  public static final int CAPABILITY_SCHEDULING = 1;
  public static final int CAPABILITY_MSB = 2;
  public static final int CAPABILITY_MSA = 4;
  public static final int CAPABILITY_SINGLE_SHOT = 8;
  public static final int CAPABILITY_ON_DEMAND_TIME = 16;
  public static final int CAPABILITY_GEOFENCING = 32;
  public static final int CAPABILITY_MEASUREMENTS = 64;
  public static final int CAPABILITY_NAV_MESSAGES = 128;
  public static final int CAPABILITY_LOW_POWER_MODE = 256;
  public static final int CAPABILITY_SATELLITE_BLOCKLIST = 512;
  public static final int CAPABILITY_MEASUREMENT_CORRECTIONS = 1024;
  public static final int CAPABILITY_ANTENNA_INFO = 2048;
  public static final int CAPABILITY_CORRELATION_VECTOR = 4096;
  public static final int CAPABILITY_SATELLITE_PVT = 8192;
  public static final int CAPABILITY_MEASUREMENT_CORRECTIONS_FOR_DRIVING = 16384;
  public void gnssSetCapabilitiesCb(int capabilities) throws android.os.RemoteException;
  public void gnssStatusCb(int status) throws android.os.RemoteException;
  public void gnssSvStatusCb(android.hardware.gnss.IGnssCallback.GnssSvInfo[] svInfoList) throws android.os.RemoteException;
  public void gnssLocationCb(android.hardware.gnss.GnssLocation location) throws android.os.RemoteException;
  public void gnssNmeaCb(long timestamp, java.lang.String nmea) throws android.os.RemoteException;
  public void gnssAcquireWakelockCb() throws android.os.RemoteException;
  public void gnssReleaseWakelockCb() throws android.os.RemoteException;
  public void gnssSetSystemInfoCb(android.hardware.gnss.IGnssCallback.GnssSystemInfo info) throws android.os.RemoteException;
  public void gnssRequestTimeCb() throws android.os.RemoteException;
  public void gnssRequestLocationCb(boolean independentFromGnss, boolean isUserEmergency) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
  public static @interface GnssStatusValue {
    public static final int NONE = 0;
    public static final int SESSION_BEGIN = 1;
    public static final int SESSION_END = 2;
    public static final int ENGINE_ON = 3;
    public static final int ENGINE_OFF = 4;
  }
  public static @interface GnssSvFlags {
    public static final int NONE = 0;
    public static final int HAS_EPHEMERIS_DATA = 1;
    public static final int HAS_ALMANAC_DATA = 2;
    public static final int USED_IN_FIX = 4;
    public static final int HAS_CARRIER_FREQUENCY = 8;
  }
  public static class GnssSvInfo implements android.os.Parcelable
  {
    public int svid = 0;
    public int constellation;
    public float cN0Dbhz = 0.000000f;
    public float basebandCN0DbHz = 0.000000f;
    public float elevationDegrees = 0.000000f;
    public float azimuthDegrees = 0.000000f;
    public long carrierFrequencyHz = 0L;
    public int svFlag = 0;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<GnssSvInfo> CREATOR = new android.os.Parcelable.Creator<GnssSvInfo>() {
      @Override
      public GnssSvInfo createFromParcel(android.os.Parcel _aidl_source) {
        GnssSvInfo _aidl_out = new GnssSvInfo();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public GnssSvInfo[] newArray(int _aidl_size) {
        return new GnssSvInfo[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(svid);
      _aidl_parcel.writeInt(constellation);
      _aidl_parcel.writeFloat(cN0Dbhz);
      _aidl_parcel.writeFloat(basebandCN0DbHz);
      _aidl_parcel.writeFloat(elevationDegrees);
      _aidl_parcel.writeFloat(azimuthDegrees);
      _aidl_parcel.writeLong(carrierFrequencyHz);
      _aidl_parcel.writeInt(svFlag);
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
        cN0Dbhz = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        basebandCN0DbHz = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        elevationDegrees = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        azimuthDegrees = _aidl_parcel.readFloat();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        carrierFrequencyHz = _aidl_parcel.readLong();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        svFlag = _aidl_parcel.readInt();
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
  public static class GnssSystemInfo implements android.os.Parcelable
  {
    public int yearOfHw = 0;
    public java.lang.String name;
    @Override
     public final int getStability() { return android.os.Parcelable.PARCELABLE_STABILITY_VINTF; }
    public static final android.os.Parcelable.Creator<GnssSystemInfo> CREATOR = new android.os.Parcelable.Creator<GnssSystemInfo>() {
      @Override
      public GnssSystemInfo createFromParcel(android.os.Parcel _aidl_source) {
        GnssSystemInfo _aidl_out = new GnssSystemInfo();
        _aidl_out.readFromParcel(_aidl_source);
        return _aidl_out;
      }
      @Override
      public GnssSystemInfo[] newArray(int _aidl_size) {
        return new GnssSystemInfo[_aidl_size];
      }
    };
    @Override public final void writeToParcel(android.os.Parcel _aidl_parcel, int _aidl_flag)
    {
      int _aidl_start_pos = _aidl_parcel.dataPosition();
      _aidl_parcel.writeInt(0);
      _aidl_parcel.writeInt(yearOfHw);
      _aidl_parcel.writeString(name);
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
        yearOfHw = _aidl_parcel.readInt();
        if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
        name = _aidl_parcel.readString();
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
}
