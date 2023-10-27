/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.net.mdns.aidl;
/** @hide */
public interface IMDnsEventListener extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "ae4cfe565d66acc7d816aabd0dfab991e64031ab";
  /** Default implementation for IMDnsEventListener. */
  public static class Default implements android.net.mdns.aidl.IMDnsEventListener
  {
    @Override public void onServiceRegistrationStatus(android.net.mdns.aidl.RegistrationInfo status) throws android.os.RemoteException
    {
    }
    @Override public void onServiceDiscoveryStatus(android.net.mdns.aidl.DiscoveryInfo status) throws android.os.RemoteException
    {
    }
    @Override public void onServiceResolutionStatus(android.net.mdns.aidl.ResolutionInfo status) throws android.os.RemoteException
    {
    }
    @Override public void onGettingServiceAddressStatus(android.net.mdns.aidl.GetAddressInfo status) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.net.mdns.aidl.IMDnsEventListener
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.net.mdns.aidl.IMDnsEventListener interface,
     * generating a proxy if needed.
     */
    public static android.net.mdns.aidl.IMDnsEventListener asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.net.mdns.aidl.IMDnsEventListener))) {
        return ((android.net.mdns.aidl.IMDnsEventListener)iin);
      }
      return new android.net.mdns.aidl.IMDnsEventListener.Stub.Proxy(obj);
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
        case TRANSACTION_onServiceRegistrationStatus:
        {
          android.net.mdns.aidl.RegistrationInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.RegistrationInfo.CREATOR);
          this.onServiceRegistrationStatus(_arg0);
          break;
        }
        case TRANSACTION_onServiceDiscoveryStatus:
        {
          android.net.mdns.aidl.DiscoveryInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.DiscoveryInfo.CREATOR);
          this.onServiceDiscoveryStatus(_arg0);
          break;
        }
        case TRANSACTION_onServiceResolutionStatus:
        {
          android.net.mdns.aidl.ResolutionInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.ResolutionInfo.CREATOR);
          this.onServiceResolutionStatus(_arg0);
          break;
        }
        case TRANSACTION_onGettingServiceAddressStatus:
        {
          android.net.mdns.aidl.GetAddressInfo _arg0;
          _arg0 = data.readTypedObject(android.net.mdns.aidl.GetAddressInfo.CREATOR);
          this.onGettingServiceAddressStatus(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.net.mdns.aidl.IMDnsEventListener
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
      @Override public void onServiceRegistrationStatus(android.net.mdns.aidl.RegistrationInfo status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(status, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onServiceRegistrationStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onServiceRegistrationStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onServiceDiscoveryStatus(android.net.mdns.aidl.DiscoveryInfo status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(status, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onServiceDiscoveryStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onServiceDiscoveryStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onServiceResolutionStatus(android.net.mdns.aidl.ResolutionInfo status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(status, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onServiceResolutionStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onServiceResolutionStatus is unimplemented.");
          }
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onGettingServiceAddressStatus(android.net.mdns.aidl.GetAddressInfo status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(status, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onGettingServiceAddressStatus, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            throw new android.os.RemoteException("Method onGettingServiceAddressStatus is unimplemented.");
          }
        }
        finally {
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
    static final int TRANSACTION_onServiceRegistrationStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onServiceDiscoveryStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onServiceResolutionStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onGettingServiceAddressStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
  }
  public static final java.lang.String DESCRIPTOR = "android$net$mdns$aidl$IMDnsEventListener".replace('$', '.');
  public static final int SERVICE_DISCOVERY_FAILED = 602;
  public static final int SERVICE_FOUND = 603;
  public static final int SERVICE_LOST = 604;
  public static final int SERVICE_REGISTRATION_FAILED = 605;
  public static final int SERVICE_REGISTERED = 606;
  public static final int SERVICE_RESOLUTION_FAILED = 607;
  public static final int SERVICE_RESOLVED = 608;
  public static final int SERVICE_GET_ADDR_FAILED = 611;
  public static final int SERVICE_GET_ADDR_SUCCESS = 612;
  public void onServiceRegistrationStatus(android.net.mdns.aidl.RegistrationInfo status) throws android.os.RemoteException;
  public void onServiceDiscoveryStatus(android.net.mdns.aidl.DiscoveryInfo status) throws android.os.RemoteException;
  public void onServiceResolutionStatus(android.net.mdns.aidl.ResolutionInfo status) throws android.os.RemoteException;
  public void onGettingServiceAddressStatus(android.net.mdns.aidl.GetAddressInfo status) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
