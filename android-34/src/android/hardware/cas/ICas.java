/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.cas;
/** @hide */
public interface ICas extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "bc51d8d70a55ec4723d3f73d0acf7003306bf69f";
  /** Default implementation for ICas. */
  public static class Default implements android.hardware.cas.ICas
  {
    @Override public void closeSession(byte[] sessionId) throws android.os.RemoteException
    {
    }
    @Override public byte[] openSessionDefault() throws android.os.RemoteException
    {
      return null;
    }
    @Override public byte[] openSession(int intent, int mode) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void processEcm(byte[] sessionId, byte[] ecm) throws android.os.RemoteException
    {
    }
    @Override public void processEmm(byte[] emm) throws android.os.RemoteException
    {
    }
    @Override public void provision(java.lang.String provisionString) throws android.os.RemoteException
    {
    }
    @Override public void refreshEntitlements(int refreshType, byte[] refreshData) throws android.os.RemoteException
    {
    }
    @Override public void release() throws android.os.RemoteException
    {
    }
    @Override public void sendEvent(int event, int arg, byte[] eventData) throws android.os.RemoteException
    {
    }
    @Override public void sendSessionEvent(byte[] sessionId, int event, int arg, byte[] eventData) throws android.os.RemoteException
    {
    }
    @Override public void setPrivateData(byte[] pvtData) throws android.os.RemoteException
    {
    }
    @Override public void setSessionPrivateData(byte[] sessionId, byte[] pvtData) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.cas.ICas
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.cas.ICas interface,
     * generating a proxy if needed.
     */
    public static android.hardware.cas.ICas asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.cas.ICas))) {
        return ((android.hardware.cas.ICas)iin);
      }
      return new android.hardware.cas.ICas.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
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
        case TRANSACTION_closeSession:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.closeSession(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_openSessionDefault:
        {
          byte[] _result = this.openSessionDefault();
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_openSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          byte[] _result = this.openSession(_arg0, _arg1);
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_processEcm:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.processEcm(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_processEmm:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.processEmm(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_provision:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          data.enforceNoDataAvail();
          this.provision(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_refreshEntitlements:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.refreshEntitlements(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_release:
        {
          this.release();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_sendEvent:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          data.enforceNoDataAvail();
          this.sendEvent(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_sendSessionEvent:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          byte[] _arg3;
          _arg3 = data.createByteArray();
          data.enforceNoDataAvail();
          this.sendSessionEvent(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setPrivateData:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.setPrivateData(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setSessionPrivateData:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.setSessionPrivateData(_arg0, _arg1);
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
    private static class Proxy implements android.hardware.cas.ICas
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
      @Override public void closeSession(byte[] sessionId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(sessionId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_closeSession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method closeSession is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public byte[] openSessionDefault() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openSessionDefault, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openSessionDefault is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createByteArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public byte[] openSession(int intent, int mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(intent);
          _data.writeInt(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openSession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method openSession is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createByteArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void processEcm(byte[] sessionId, byte[] ecm) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(sessionId);
          _data.writeByteArray(ecm);
          boolean _status = mRemote.transact(Stub.TRANSACTION_processEcm, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method processEcm is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void processEmm(byte[] emm) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(emm);
          boolean _status = mRemote.transact(Stub.TRANSACTION_processEmm, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method processEmm is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void provision(java.lang.String provisionString) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(provisionString);
          boolean _status = mRemote.transact(Stub.TRANSACTION_provision, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method provision is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void refreshEntitlements(int refreshType, byte[] refreshData) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(refreshType);
          _data.writeByteArray(refreshData);
          boolean _status = mRemote.transact(Stub.TRANSACTION_refreshEntitlements, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method refreshEntitlements is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void release() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_release, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method release is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void sendEvent(int event, int arg, byte[] eventData) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(event);
          _data.writeInt(arg);
          _data.writeByteArray(eventData);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendEvent, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method sendEvent is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void sendSessionEvent(byte[] sessionId, int event, int arg, byte[] eventData) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(sessionId);
          _data.writeInt(event);
          _data.writeInt(arg);
          _data.writeByteArray(eventData);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendSessionEvent, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method sendSessionEvent is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setPrivateData(byte[] pvtData) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(pvtData);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPrivateData, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setPrivateData is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setSessionPrivateData(byte[] sessionId, byte[] pvtData) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(sessionId);
          _data.writeByteArray(pvtData);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSessionPrivateData, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setSessionPrivateData is unimplemented.");
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
    static final int TRANSACTION_closeSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_openSessionDefault = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_openSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_processEcm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_processEmm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_provision = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_refreshEntitlements = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_release = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_sendEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_sendSessionEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_setPrivateData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_setSessionPrivateData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$cas$ICas".replace('$', '.');
  public void closeSession(byte[] sessionId) throws android.os.RemoteException;
  public byte[] openSessionDefault() throws android.os.RemoteException;
  public byte[] openSession(int intent, int mode) throws android.os.RemoteException;
  public void processEcm(byte[] sessionId, byte[] ecm) throws android.os.RemoteException;
  public void processEmm(byte[] emm) throws android.os.RemoteException;
  public void provision(java.lang.String provisionString) throws android.os.RemoteException;
  public void refreshEntitlements(int refreshType, byte[] refreshData) throws android.os.RemoteException;
  public void release() throws android.os.RemoteException;
  public void sendEvent(int event, int arg, byte[] eventData) throws android.os.RemoteException;
  public void sendSessionEvent(byte[] sessionId, int event, int arg, byte[] eventData) throws android.os.RemoteException;
  public void setPrivateData(byte[] pvtData) throws android.os.RemoteException;
  public void setSessionPrivateData(byte[] sessionId, byte[] pvtData) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
