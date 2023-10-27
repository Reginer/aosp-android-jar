/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.gnss;
/** @hide */
public interface IGnssGeofenceCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 2;
  public static final String HASH = "fc957f1d3d261d065ff5e5415f2d21caa79c310f";
  /** Default implementation for IGnssGeofenceCallback. */
  public static class Default implements android.hardware.gnss.IGnssGeofenceCallback
  {
    @Override public void gnssGeofenceTransitionCb(int geofenceId, android.hardware.gnss.GnssLocation location, int transition, long timestampMillis) throws android.os.RemoteException
    {
    }
    @Override public void gnssGeofenceStatusCb(int availability, android.hardware.gnss.GnssLocation lastLocation) throws android.os.RemoteException
    {
    }
    @Override public void gnssGeofenceAddCb(int geofenceId, int status) throws android.os.RemoteException
    {
    }
    @Override public void gnssGeofenceRemoveCb(int geofenceId, int status) throws android.os.RemoteException
    {
    }
    @Override public void gnssGeofencePauseCb(int geofenceId, int status) throws android.os.RemoteException
    {
    }
    @Override public void gnssGeofenceResumeCb(int geofenceId, int status) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.gnss.IGnssGeofenceCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.gnss.IGnssGeofenceCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.gnss.IGnssGeofenceCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.gnss.IGnssGeofenceCallback))) {
        return ((android.hardware.gnss.IGnssGeofenceCallback)iin);
      }
      return new android.hardware.gnss.IGnssGeofenceCallback.Stub.Proxy(obj);
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
        case TRANSACTION_gnssGeofenceTransitionCb:
        {
          return "gnssGeofenceTransitionCb";
        }
        case TRANSACTION_gnssGeofenceStatusCb:
        {
          return "gnssGeofenceStatusCb";
        }
        case TRANSACTION_gnssGeofenceAddCb:
        {
          return "gnssGeofenceAddCb";
        }
        case TRANSACTION_gnssGeofenceRemoveCb:
        {
          return "gnssGeofenceRemoveCb";
        }
        case TRANSACTION_gnssGeofencePauseCb:
        {
          return "gnssGeofencePauseCb";
        }
        case TRANSACTION_gnssGeofenceResumeCb:
        {
          return "gnssGeofenceResumeCb";
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
        case TRANSACTION_gnssGeofenceTransitionCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.gnss.GnssLocation _arg1;
          _arg1 = data.readTypedObject(android.hardware.gnss.GnssLocation.CREATOR);
          int _arg2;
          _arg2 = data.readInt();
          long _arg3;
          _arg3 = data.readLong();
          data.enforceNoDataAvail();
          this.gnssGeofenceTransitionCb(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssGeofenceStatusCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.gnss.GnssLocation _arg1;
          _arg1 = data.readTypedObject(android.hardware.gnss.GnssLocation.CREATOR);
          data.enforceNoDataAvail();
          this.gnssGeofenceStatusCb(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssGeofenceAddCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.gnssGeofenceAddCb(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssGeofenceRemoveCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.gnssGeofenceRemoveCb(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssGeofencePauseCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.gnssGeofencePauseCb(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_gnssGeofenceResumeCb:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.gnssGeofenceResumeCb(_arg0, _arg1);
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
    private static class Proxy implements android.hardware.gnss.IGnssGeofenceCallback
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
      @Override public void gnssGeofenceTransitionCb(int geofenceId, android.hardware.gnss.GnssLocation location, int transition, long timestampMillis) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(geofenceId);
          _data.writeTypedObject(location, 0);
          _data.writeInt(transition);
          _data.writeLong(timestampMillis);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssGeofenceTransitionCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssGeofenceTransitionCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssGeofenceStatusCb(int availability, android.hardware.gnss.GnssLocation lastLocation) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(availability);
          _data.writeTypedObject(lastLocation, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssGeofenceStatusCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssGeofenceStatusCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssGeofenceAddCb(int geofenceId, int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(geofenceId);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssGeofenceAddCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssGeofenceAddCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssGeofenceRemoveCb(int geofenceId, int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(geofenceId);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssGeofenceRemoveCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssGeofenceRemoveCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssGeofencePauseCb(int geofenceId, int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(geofenceId);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssGeofencePauseCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssGeofencePauseCb is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void gnssGeofenceResumeCb(int geofenceId, int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(geofenceId);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_gnssGeofenceResumeCb, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method gnssGeofenceResumeCb is unimplemented.");
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
    static final int TRANSACTION_gnssGeofenceTransitionCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_gnssGeofenceStatusCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_gnssGeofenceAddCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_gnssGeofenceRemoveCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_gnssGeofencePauseCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_gnssGeofenceResumeCb = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$gnss$IGnssGeofenceCallback".replace('$', '.');
  public static final int ENTERED = 1;
  public static final int EXITED = 2;
  public static final int UNCERTAIN = 4;
  public static final int UNAVAILABLE = 1;
  public static final int AVAILABLE = 2;
  public static final int OPERATION_SUCCESS = 0;
  public static final int ERROR_TOO_MANY_GEOFENCES = -100;
  public static final int ERROR_ID_EXISTS = -101;
  public static final int ERROR_ID_UNKNOWN = -102;
  public static final int ERROR_INVALID_TRANSITION = -103;
  public static final int ERROR_GENERIC = -149;
  public void gnssGeofenceTransitionCb(int geofenceId, android.hardware.gnss.GnssLocation location, int transition, long timestampMillis) throws android.os.RemoteException;
  public void gnssGeofenceStatusCb(int availability, android.hardware.gnss.GnssLocation lastLocation) throws android.os.RemoteException;
  public void gnssGeofenceAddCb(int geofenceId, int status) throws android.os.RemoteException;
  public void gnssGeofenceRemoveCb(int geofenceId, int status) throws android.os.RemoteException;
  public void gnssGeofencePauseCb(int geofenceId, int status) throws android.os.RemoteException;
  public void gnssGeofenceResumeCb(int geofenceId, int status) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
