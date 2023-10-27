/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.keystore2;
/** @hide */
public interface IKeystoreService extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "4f1c704008e5687ed0d6f1590464aed39fc7f64e";
  /** Default implementation for IKeystoreService. */
  public static class Default implements android.system.keystore2.IKeystoreService
  {
    @Override public android.system.keystore2.IKeystoreSecurityLevel getSecurityLevel(int securityLevel) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.system.keystore2.KeyEntryResponse getKeyEntry(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void updateSubcomponent(android.system.keystore2.KeyDescriptor key, byte[] publicCert, byte[] certificateChain) throws android.os.RemoteException
    {
    }
    /** @deprecated use listEntriesBatched instead. */
    @Override public android.system.keystore2.KeyDescriptor[] listEntries(int domain, long nspace) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void deleteKey(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException
    {
    }
    @Override public android.system.keystore2.KeyDescriptor grant(android.system.keystore2.KeyDescriptor key, int granteeUid, int accessVector) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void ungrant(android.system.keystore2.KeyDescriptor key, int granteeUid) throws android.os.RemoteException
    {
    }
    @Override public int getNumberOfEntries(int domain, long nspace) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public android.system.keystore2.KeyDescriptor[] listEntriesBatched(int domain, long nspace, java.lang.String startingPastAlias) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.system.keystore2.IKeystoreService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.system.keystore2.IKeystoreService interface,
     * generating a proxy if needed.
     */
    public static android.system.keystore2.IKeystoreService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.system.keystore2.IKeystoreService))) {
        return ((android.system.keystore2.IKeystoreService)iin);
      }
      return new android.system.keystore2.IKeystoreService.Stub.Proxy(obj);
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
        case TRANSACTION_getSecurityLevel:
        {
          return "getSecurityLevel";
        }
        case TRANSACTION_getKeyEntry:
        {
          return "getKeyEntry";
        }
        case TRANSACTION_updateSubcomponent:
        {
          return "updateSubcomponent";
        }
        case TRANSACTION_listEntries:
        {
          return "listEntries";
        }
        case TRANSACTION_deleteKey:
        {
          return "deleteKey";
        }
        case TRANSACTION_grant:
        {
          return "grant";
        }
        case TRANSACTION_ungrant:
        {
          return "ungrant";
        }
        case TRANSACTION_getNumberOfEntries:
        {
          return "getNumberOfEntries";
        }
        case TRANSACTION_listEntriesBatched:
        {
          return "listEntriesBatched";
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
        case TRANSACTION_getSecurityLevel:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          android.system.keystore2.IKeystoreSecurityLevel _result = this.getSecurityLevel(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_getKeyEntry:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          data.enforceNoDataAvail();
          android.system.keystore2.KeyEntryResponse _result = this.getKeyEntry(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_updateSubcomponent:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          byte[] _arg1;
          _arg1 = data.createByteArray();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          data.enforceNoDataAvail();
          this.updateSubcomponent(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_listEntries:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          android.system.keystore2.KeyDescriptor[] _result = this.listEntries(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_deleteKey:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          data.enforceNoDataAvail();
          this.deleteKey(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_grant:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          data.enforceNoDataAvail();
          android.system.keystore2.KeyDescriptor _result = this.grant(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_ungrant:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.ungrant(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getNumberOfEntries:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          data.enforceNoDataAvail();
          int _result = this.getNumberOfEntries(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_listEntriesBatched:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          java.lang.String _arg2;
          _arg2 = data.readString();
          data.enforceNoDataAvail();
          android.system.keystore2.KeyDescriptor[] _result = this.listEntriesBatched(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.system.keystore2.IKeystoreService
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
      @Override public android.system.keystore2.IKeystoreSecurityLevel getSecurityLevel(int securityLevel) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.IKeystoreSecurityLevel _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(securityLevel);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSecurityLevel, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSecurityLevel is unimplemented.");
          }
          _reply.readException();
          _result = android.system.keystore2.IKeystoreSecurityLevel.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.system.keystore2.KeyEntryResponse getKeyEntry(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.KeyEntryResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getKeyEntry, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getKeyEntry is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.system.keystore2.KeyEntryResponse.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void updateSubcomponent(android.system.keystore2.KeyDescriptor key, byte[] publicCert, byte[] certificateChain) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          _data.writeByteArray(publicCert);
          _data.writeByteArray(certificateChain);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateSubcomponent, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method updateSubcomponent is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** @deprecated use listEntriesBatched instead. */
      @Override public android.system.keystore2.KeyDescriptor[] listEntries(int domain, long nspace) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.KeyDescriptor[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(domain);
          _data.writeLong(nspace);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listEntries, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method listEntries is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.system.keystore2.KeyDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void deleteKey(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deleteKey, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method deleteKey is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.system.keystore2.KeyDescriptor grant(android.system.keystore2.KeyDescriptor key, int granteeUid, int accessVector) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.KeyDescriptor _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          _data.writeInt(granteeUid);
          _data.writeInt(accessVector);
          boolean _status = mRemote.transact(Stub.TRANSACTION_grant, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method grant is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void ungrant(android.system.keystore2.KeyDescriptor key, int granteeUid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          _data.writeInt(granteeUid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_ungrant, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method ungrant is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getNumberOfEntries(int domain, long nspace) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(domain);
          _data.writeLong(nspace);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getNumberOfEntries, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getNumberOfEntries is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.system.keystore2.KeyDescriptor[] listEntriesBatched(int domain, long nspace, java.lang.String startingPastAlias) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.KeyDescriptor[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(domain);
          _data.writeLong(nspace);
          _data.writeString(startingPastAlias);
          boolean _status = mRemote.transact(Stub.TRANSACTION_listEntriesBatched, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method listEntriesBatched is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.system.keystore2.KeyDescriptor.CREATOR);
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
    static final int TRANSACTION_getSecurityLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getKeyEntry = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_updateSubcomponent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_listEntries = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_deleteKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_grant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_ungrant = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getNumberOfEntries = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_listEntriesBatched = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$system$keystore2$IKeystoreService".replace('$', '.');
  public android.system.keystore2.IKeystoreSecurityLevel getSecurityLevel(int securityLevel) throws android.os.RemoteException;
  public android.system.keystore2.KeyEntryResponse getKeyEntry(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException;
  public void updateSubcomponent(android.system.keystore2.KeyDescriptor key, byte[] publicCert, byte[] certificateChain) throws android.os.RemoteException;
  /** @deprecated use listEntriesBatched instead. */
  @Deprecated
  public android.system.keystore2.KeyDescriptor[] listEntries(int domain, long nspace) throws android.os.RemoteException;
  public void deleteKey(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException;
  public android.system.keystore2.KeyDescriptor grant(android.system.keystore2.KeyDescriptor key, int granteeUid, int accessVector) throws android.os.RemoteException;
  public void ungrant(android.system.keystore2.KeyDescriptor key, int granteeUid) throws android.os.RemoteException;
  public int getNumberOfEntries(int domain, long nspace) throws android.os.RemoteException;
  public android.system.keystore2.KeyDescriptor[] listEntriesBatched(int domain, long nspace, java.lang.String startingPastAlias) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
