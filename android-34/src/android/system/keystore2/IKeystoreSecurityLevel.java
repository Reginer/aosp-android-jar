/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.system.keystore2;
/** @hide */
public interface IKeystoreSecurityLevel extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "4f1c704008e5687ed0d6f1590464aed39fc7f64e";
  /** Default implementation for IKeystoreSecurityLevel. */
  public static class Default implements android.system.keystore2.IKeystoreSecurityLevel
  {
    @Override public android.system.keystore2.CreateOperationResponse createOperation(android.system.keystore2.KeyDescriptor key, android.hardware.security.keymint.KeyParameter[] operationParameters, boolean forced) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.system.keystore2.KeyMetadata generateKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor attestationKey, android.hardware.security.keymint.KeyParameter[] params, int flags, byte[] entropy) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.system.keystore2.KeyMetadata importKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor attestationKey, android.hardware.security.keymint.KeyParameter[] params, int flags, byte[] keyData) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.system.keystore2.KeyMetadata importWrappedKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor wrappingKey, byte[] maskingKey, android.hardware.security.keymint.KeyParameter[] params, android.system.keystore2.AuthenticatorSpec[] authenticators) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.system.keystore2.EphemeralStorageKeyResponse convertStorageKeyToEphemeral(android.system.keystore2.KeyDescriptor storageKey) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void deleteKey(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.system.keystore2.IKeystoreSecurityLevel
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.system.keystore2.IKeystoreSecurityLevel interface,
     * generating a proxy if needed.
     */
    public static android.system.keystore2.IKeystoreSecurityLevel asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.system.keystore2.IKeystoreSecurityLevel))) {
        return ((android.system.keystore2.IKeystoreSecurityLevel)iin);
      }
      return new android.system.keystore2.IKeystoreSecurityLevel.Stub.Proxy(obj);
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
        case TRANSACTION_createOperation:
        {
          return "createOperation";
        }
        case TRANSACTION_generateKey:
        {
          return "generateKey";
        }
        case TRANSACTION_importKey:
        {
          return "importKey";
        }
        case TRANSACTION_importWrappedKey:
        {
          return "importWrappedKey";
        }
        case TRANSACTION_convertStorageKeyToEphemeral:
        {
          return "convertStorageKeyToEphemeral";
        }
        case TRANSACTION_deleteKey:
        {
          return "deleteKey";
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
        case TRANSACTION_createOperation:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          android.hardware.security.keymint.KeyParameter[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          boolean _arg2;
          _arg2 = data.readBoolean();
          data.enforceNoDataAvail();
          android.system.keystore2.CreateOperationResponse _result = this.createOperation(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_generateKey:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          android.system.keystore2.KeyDescriptor _arg1;
          _arg1 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          android.hardware.security.keymint.KeyParameter[] _arg2;
          _arg2 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          int _arg3;
          _arg3 = data.readInt();
          byte[] _arg4;
          _arg4 = data.createByteArray();
          data.enforceNoDataAvail();
          android.system.keystore2.KeyMetadata _result = this.generateKey(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_importKey:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          android.system.keystore2.KeyDescriptor _arg1;
          _arg1 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          android.hardware.security.keymint.KeyParameter[] _arg2;
          _arg2 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          int _arg3;
          _arg3 = data.readInt();
          byte[] _arg4;
          _arg4 = data.createByteArray();
          data.enforceNoDataAvail();
          android.system.keystore2.KeyMetadata _result = this.importKey(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_importWrappedKey:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          android.system.keystore2.KeyDescriptor _arg1;
          _arg1 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          byte[] _arg2;
          _arg2 = data.createByteArray();
          android.hardware.security.keymint.KeyParameter[] _arg3;
          _arg3 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          android.system.keystore2.AuthenticatorSpec[] _arg4;
          _arg4 = data.createTypedArray(android.system.keystore2.AuthenticatorSpec.CREATOR);
          data.enforceNoDataAvail();
          android.system.keystore2.KeyMetadata _result = this.importWrappedKey(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_convertStorageKeyToEphemeral:
        {
          android.system.keystore2.KeyDescriptor _arg0;
          _arg0 = data.readTypedObject(android.system.keystore2.KeyDescriptor.CREATOR);
          data.enforceNoDataAvail();
          android.system.keystore2.EphemeralStorageKeyResponse _result = this.convertStorageKeyToEphemeral(_arg0);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
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
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.system.keystore2.IKeystoreSecurityLevel
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
      @Override public android.system.keystore2.CreateOperationResponse createOperation(android.system.keystore2.KeyDescriptor key, android.hardware.security.keymint.KeyParameter[] operationParameters, boolean forced) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.CreateOperationResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          _data.writeTypedArray(operationParameters, 0);
          _data.writeBoolean(forced);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createOperation, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method createOperation is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.system.keystore2.CreateOperationResponse.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.system.keystore2.KeyMetadata generateKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor attestationKey, android.hardware.security.keymint.KeyParameter[] params, int flags, byte[] entropy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.KeyMetadata _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          _data.writeTypedObject(attestationKey, 0);
          _data.writeTypedArray(params, 0);
          _data.writeInt(flags);
          _data.writeByteArray(entropy);
          boolean _status = mRemote.transact(Stub.TRANSACTION_generateKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method generateKey is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.system.keystore2.KeyMetadata.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.system.keystore2.KeyMetadata importKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor attestationKey, android.hardware.security.keymint.KeyParameter[] params, int flags, byte[] keyData) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.KeyMetadata _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          _data.writeTypedObject(attestationKey, 0);
          _data.writeTypedArray(params, 0);
          _data.writeInt(flags);
          _data.writeByteArray(keyData);
          boolean _status = mRemote.transact(Stub.TRANSACTION_importKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method importKey is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.system.keystore2.KeyMetadata.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.system.keystore2.KeyMetadata importWrappedKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor wrappingKey, byte[] maskingKey, android.hardware.security.keymint.KeyParameter[] params, android.system.keystore2.AuthenticatorSpec[] authenticators) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.KeyMetadata _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          _data.writeTypedObject(wrappingKey, 0);
          _data.writeByteArray(maskingKey);
          _data.writeTypedArray(params, 0);
          _data.writeTypedArray(authenticators, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_importWrappedKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method importWrappedKey is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.system.keystore2.KeyMetadata.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.system.keystore2.EphemeralStorageKeyResponse convertStorageKeyToEphemeral(android.system.keystore2.KeyDescriptor storageKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.system.keystore2.EphemeralStorageKeyResponse _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(storageKey, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_convertStorageKeyToEphemeral, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method convertStorageKeyToEphemeral is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.system.keystore2.EphemeralStorageKeyResponse.CREATOR);
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
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(key, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deleteKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
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
    static final int TRANSACTION_createOperation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_generateKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_importKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_importWrappedKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_convertStorageKeyToEphemeral = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_deleteKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$system$keystore2$IKeystoreSecurityLevel".replace('$', '.');
  public static final int KEY_FLAG_AUTH_BOUND_WITHOUT_CRYPTOGRAPHIC_LSKF_BINDING = 1;
  public android.system.keystore2.CreateOperationResponse createOperation(android.system.keystore2.KeyDescriptor key, android.hardware.security.keymint.KeyParameter[] operationParameters, boolean forced) throws android.os.RemoteException;
  public android.system.keystore2.KeyMetadata generateKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor attestationKey, android.hardware.security.keymint.KeyParameter[] params, int flags, byte[] entropy) throws android.os.RemoteException;
  public android.system.keystore2.KeyMetadata importKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor attestationKey, android.hardware.security.keymint.KeyParameter[] params, int flags, byte[] keyData) throws android.os.RemoteException;
  public android.system.keystore2.KeyMetadata importWrappedKey(android.system.keystore2.KeyDescriptor key, android.system.keystore2.KeyDescriptor wrappingKey, byte[] maskingKey, android.hardware.security.keymint.KeyParameter[] params, android.system.keystore2.AuthenticatorSpec[] authenticators) throws android.os.RemoteException;
  public android.system.keystore2.EphemeralStorageKeyResponse convertStorageKeyToEphemeral(android.system.keystore2.KeyDescriptor storageKey) throws android.os.RemoteException;
  public void deleteKey(android.system.keystore2.KeyDescriptor key) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
