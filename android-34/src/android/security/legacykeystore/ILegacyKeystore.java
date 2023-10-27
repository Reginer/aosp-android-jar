/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.legacykeystore;
/**
 * Internal interface for accessing and storing legacy keystore blobs.
 * Before Android S, Keystore offered a key-value store that was intended for storing
 * data associated with certain types of keys. E.g., public certificates for asymmetric keys.
 * This key value store no longer exists as part of the Keystore 2.0 protocol.
 * However, there are some clients that used Keystore in an unintended way.
 * This interface exists to give these clients a grace period to migrate their keys
 * out of legacy keystore. In Android S, this legacy keystore may be used as keystore was
 * used in earlier versions, and provides access to entries that were put into keystore
 * before Android S.
 * 
 * DEPRECATION NOTICE: In Android T, the `put` function is slated to be removed.
 * This will allow clients to use the `get`, `list`, and `remove` API to migrate blobs out
 * of legacy keystore.
 * @hide
 */
public interface ILegacyKeystore extends android.os.IInterface
{
  /** Default implementation for ILegacyKeystore. */
  public static class Default implements android.security.legacykeystore.ILegacyKeystore
  {
    /**
     * Returns the blob stored under the given name.
     * 
     * @param alias name of the blob entry.
     * @param uid designates the legacy namespace. Specify UID_SELF for the caller's namespace.
     * @return The unstructured blob that was passed as blob parameter into put()
     */
    @Override public byte[] get(java.lang.String alias, int uid) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * Stores one entry as unstructured blob under the given alias.
     * Overwrites existing entries with the same alias.
     * 
     * @param alias name of the new entry.
     * @param uid designates the legacy namespace. Specify UID_SELF for the caller's namespace.
     * @param blob the payload of the new entry.
     * 
     * IMPORTANT DEPRECATION NOTICE: This function is slated to be removed in Android T.
     *     Do not add new callers. The remaining functionality will remain for the purpose
     *     of migrating legacy configuration out.
     */
    @Override public void put(java.lang.String alias, int uid, byte[] blob) throws android.os.RemoteException
    {
    }
    /**
     * Deletes the entry under the given alias.
     * 
     * @param alias name of the entry to be removed.
     * @param uid designates the legacy namespace of the entry. Specify UID_SELF for the caller's
     *            namespace.
     */
    @Override public void remove(java.lang.String alias, int uid) throws android.os.RemoteException
    {
    }
    /**
     * Returns a list of aliases of entries stored. The list is filtered by prefix.
     * The resulting strings are the full aliases including the prefix.
     * 
     * @param prefix used to filter results.
     * @param uid legacy namespace to list. Specify UID_SELF for caller's namespace.
     */
    @Override public java.lang.String[] list(java.lang.String prefix, int uid) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.security.legacykeystore.ILegacyKeystore
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.security.legacykeystore.ILegacyKeystore interface,
     * generating a proxy if needed.
     */
    public static android.security.legacykeystore.ILegacyKeystore asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.security.legacykeystore.ILegacyKeystore))) {
        return ((android.security.legacykeystore.ILegacyKeystore)iin);
      }
      return new android.security.legacykeystore.ILegacyKeystore.Stub.Proxy(obj);
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
        case TRANSACTION_get:
        {
          return "get";
        }
        case TRANSACTION_put:
        {
          return "put";
        }
        case TRANSACTION_remove:
        {
          return "remove";
        }
        case TRANSACTION_list:
        {
          return "list";
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
      }
      switch (code)
      {
        case TRANSACTION_get:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          byte[] _result = this.get(_arg0, _arg1);
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_put:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          data.enforceNoDataAvail();
          this.put(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_remove:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.remove(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_list:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          java.lang.String[] _result = this.list(_arg0, _arg1);
          reply.writeNoException();
          reply.writeStringArray(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.security.legacykeystore.ILegacyKeystore
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /**
       * Returns the blob stored under the given name.
       * 
       * @param alias name of the blob entry.
       * @param uid designates the legacy namespace. Specify UID_SELF for the caller's namespace.
       * @return The unstructured blob that was passed as blob parameter into put()
       */
      @Override public byte[] get(java.lang.String alias, int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(alias);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_get, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createByteArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Stores one entry as unstructured blob under the given alias.
       * Overwrites existing entries with the same alias.
       * 
       * @param alias name of the new entry.
       * @param uid designates the legacy namespace. Specify UID_SELF for the caller's namespace.
       * @param blob the payload of the new entry.
       * 
       * IMPORTANT DEPRECATION NOTICE: This function is slated to be removed in Android T.
       *     Do not add new callers. The remaining functionality will remain for the purpose
       *     of migrating legacy configuration out.
       */
      @Override public void put(java.lang.String alias, int uid, byte[] blob) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(alias);
          _data.writeInt(uid);
          _data.writeByteArray(blob);
          boolean _status = mRemote.transact(Stub.TRANSACTION_put, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Deletes the entry under the given alias.
       * 
       * @param alias name of the entry to be removed.
       * @param uid designates the legacy namespace of the entry. Specify UID_SELF for the caller's
       *            namespace.
       */
      @Override public void remove(java.lang.String alias, int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(alias);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_remove, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Returns a list of aliases of entries stored. The list is filtered by prefix.
       * The resulting strings are the full aliases including the prefix.
       * 
       * @param prefix used to filter results.
       * @param uid legacy namespace to list. Specify UID_SELF for caller's namespace.
       */
      @Override public java.lang.String[] list(java.lang.String prefix, int uid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(prefix);
          _data.writeInt(uid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_list, _data, _reply, 0);
          _reply.readException();
          _result = _reply.createStringArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_get = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_put = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_remove = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_list = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 3;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$security$legacykeystore$ILegacyKeystore".replace('$', '.');
  /** Special value indicating the callers uid. */
  public static final int UID_SELF = -1;
  /** Service specific error code indicating that an unexpected system error occurred. */
  public static final int ERROR_SYSTEM_ERROR = 4;
  /**
   * Service specific error code indicating that the caller does not have the
   * right to access the requested uid.
   */
  public static final int ERROR_PERMISSION_DENIED = 6;
  /** Service specific error code indicating that the entry was not found. */
  public static final int ERROR_ENTRY_NOT_FOUND = 7;
  /**
   * Returns the blob stored under the given name.
   * 
   * @param alias name of the blob entry.
   * @param uid designates the legacy namespace. Specify UID_SELF for the caller's namespace.
   * @return The unstructured blob that was passed as blob parameter into put()
   */
  public byte[] get(java.lang.String alias, int uid) throws android.os.RemoteException;
  /**
   * Stores one entry as unstructured blob under the given alias.
   * Overwrites existing entries with the same alias.
   * 
   * @param alias name of the new entry.
   * @param uid designates the legacy namespace. Specify UID_SELF for the caller's namespace.
   * @param blob the payload of the new entry.
   * 
   * IMPORTANT DEPRECATION NOTICE: This function is slated to be removed in Android T.
   *     Do not add new callers. The remaining functionality will remain for the purpose
   *     of migrating legacy configuration out.
   */
  public void put(java.lang.String alias, int uid, byte[] blob) throws android.os.RemoteException;
  /**
   * Deletes the entry under the given alias.
   * 
   * @param alias name of the entry to be removed.
   * @param uid designates the legacy namespace of the entry. Specify UID_SELF for the caller's
   *            namespace.
   */
  public void remove(java.lang.String alias, int uid) throws android.os.RemoteException;
  /**
   * Returns a list of aliases of entries stored. The list is filtered by prefix.
   * The resulting strings are the full aliases including the prefix.
   * 
   * @param prefix used to filter results.
   * @param uid legacy namespace to list. Specify UID_SELF for caller's namespace.
   */
  public java.lang.String[] list(java.lang.String prefix, int uid) throws android.os.RemoteException;
}
