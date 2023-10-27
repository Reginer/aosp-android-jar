/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.net.mdns.aidl;
/** @hide */
public interface IMDns extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "ae4cfe565d66acc7d816aabd0dfab991e64031ab";
  /** Default implementation for IMDns. */
  public static class Default implements android.net.mdns.aidl.IMDns
  {
    @Override public void startDaemon() throws android.os.RemoteException
    {
    }
    @Override public void stopDaemon() throws android.os.RemoteException
    {
    }
    @Override public void registerService(android.net.mdns.aidl.RegistrationInfo info) throws android.os.RemoteException
    {
    }
    @Override public void discover(android.net.mdns.aidl.DiscoveryInfo info) throws android.os.RemoteException
    {
    }
    @Override public void resolve(android.net.mdns.aidl.ResolutionInfo info) throws android.os.RemoteException
    {
    }
    @Override public void getServiceAddress(android.net.mdns.aidl.GetAddressInfo info) throws android.os.RemoteException
    {
    }
    @Override public void stopOperation(int id) throws android.os.RemoteException
    {
    }
    @Override public void registerEventListener(android.net.mdns.aidl.IMDnsEventListener listener) throws android.os.RemoteException
    {
    }
    @Override public void unregisterEventListener(android.net.mdns.aidl.IMDnsEventListener listener) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.net.mdns.aidl.IMDns
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.net.mdns.aidl.IMDns interface,
     * generating a proxy if needed.
     */
    public static android.net.mdns.aidl.IMDns asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.net.mdns.aidl.IMDns))) {
        return ((android.net.mdns.aidl.IMDns)iin);
      }
      return new android.net.mdns.aidl.IMDns.Stub.Proxy(obj);
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
        case TRANSACTION_startDaemon:
        {
          this.startDaemon();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopDaemon:
        {
          this.stopDaemon();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerService:
        {
          android.net.mdns.aidl.RegistrationInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.RegistrationInfo.CREATOR);
          this.registerService(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_discover:
        {
          android.net.mdns.aidl.DiscoveryInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.DiscoveryInfo.CREATOR);
          this.discover(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_resolve:
        {
          android.net.mdns.aidl.ResolutionInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.ResolutionInfo.CREATOR);
          this.resolve(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getServiceAddress:
        {
          android.net.mdns.aidl.GetAddressInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.GetAddressInfo.CREATOR);
          this.getServiceAddress(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_stopOperation:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.stopOperation(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerEventListener:
        {
          android.net.mdns.aidl.IMDnsEventListener _arg0;
          _arg0 = android.net.mdns.aidl.IMDnsEventListener.Stub.asInterface(data.readStrongBinder());
          this.registerEventListener(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterEventListener:
        {
          android.net.mdns.aidl.IMDnsEventListener _arg0;
          _arg0 = android.net.mdns.aidl.IMDnsEventListener.Stub.asInterface(data.readStrongBinder());
          this.unregisterEventListener(_arg0);
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
    private static class Proxy implements android.net.mdns.aidl.IMDns
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
      @Override public void startDaemon() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startDaemon, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method startDaemon is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopDaemon() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopDaemon, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stopDaemon is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void registerService(android.net.mdns.aidl.RegistrationInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerService, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerService is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void discover(android.net.mdns.aidl.DiscoveryInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_discover, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method discover is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void resolve(android.net.mdns.aidl.ResolutionInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_resolve, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method resolve is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void getServiceAddress(android.net.mdns.aidl.GetAddressInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getServiceAddress, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getServiceAddress is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void stopOperation(int id) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(id);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopOperation, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method stopOperation is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void registerEventListener(android.net.mdns.aidl.IMDnsEventListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerEventListener, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerEventListener is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterEventListener(android.net.mdns.aidl.IMDnsEventListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterEventListener, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method unregisterEventListener is unimplemented.");
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
          android.os.Parcel data = android.os.Parcel.obtain();
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
          android.os.Parcel data = android.os.Parcel.obtain();
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
    static final int TRANSACTION_startDaemon = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_stopDaemon = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_registerService = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_discover = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_resolve = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getServiceAddress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_stopOperation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_registerEventListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_unregisterEventListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$net$mdns$aidl$IMDns".replace('$', '.');
  public void startDaemon() throws android.os.RemoteException;
  public void stopDaemon() throws android.os.RemoteException;
  public void registerService(android.net.mdns.aidl.RegistrationInfo info) throws android.os.RemoteException;
  public void discover(android.net.mdns.aidl.DiscoveryInfo info) throws android.os.RemoteException;
  public void resolve(android.net.mdns.aidl.ResolutionInfo info) throws android.os.RemoteException;
  public void getServiceAddress(android.net.mdns.aidl.GetAddressInfo info) throws android.os.RemoteException;
  public void stopOperation(int id) throws android.os.RemoteException;
  public void registerEventListener(android.net.mdns.aidl.IMDnsEventListener listener) throws android.os.RemoteException;
  public void unregisterEventListener(android.net.mdns.aidl.IMDnsEventListener listener) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
