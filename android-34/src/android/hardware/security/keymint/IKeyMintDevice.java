/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.security.keymint;
/** @hide */
public interface IKeyMintDevice extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "74a538630d5d90f732f361a2313cbb69b09eb047";
  /** Default implementation for IKeyMintDevice. */
  public static class Default implements android.hardware.security.keymint.IKeyMintDevice
  {
    @Override public android.hardware.security.keymint.KeyMintHardwareInfo getHardwareInfo() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void addRngEntropy(byte[] data) throws android.os.RemoteException
    {
    }
    @Override public android.hardware.security.keymint.KeyCreationResult generateKey(android.hardware.security.keymint.KeyParameter[] keyParams, android.hardware.security.keymint.AttestationKey attestationKey) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.security.keymint.KeyCreationResult importKey(android.hardware.security.keymint.KeyParameter[] keyParams, int keyFormat, byte[] keyData, android.hardware.security.keymint.AttestationKey attestationKey) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.security.keymint.KeyCreationResult importWrappedKey(byte[] wrappedKeyData, byte[] wrappingKeyBlob, byte[] maskingKey, android.hardware.security.keymint.KeyParameter[] unwrappingParams, long passwordSid, long biometricSid) throws android.os.RemoteException
    {
      return null;
    }
    @Override public byte[] upgradeKey(byte[] keyBlobToUpgrade, android.hardware.security.keymint.KeyParameter[] upgradeParams) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void deleteKey(byte[] keyBlob) throws android.os.RemoteException
    {
    }
    @Override public void deleteAllKeys() throws android.os.RemoteException
    {
    }
    @Override public void destroyAttestationIds() throws android.os.RemoteException
    {
    }
    @Override public android.hardware.security.keymint.BeginResult begin(int purpose, byte[] keyBlob, android.hardware.security.keymint.KeyParameter[] params, android.hardware.security.keymint.HardwareAuthToken authToken) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void deviceLocked(boolean passwordOnly, android.hardware.security.secureclock.TimeStampToken timestampToken) throws android.os.RemoteException
    {
    }
    @Override public void earlyBootEnded() throws android.os.RemoteException
    {
    }
    @Override public byte[] convertStorageKeyToEphemeral(byte[] storageKeyBlob) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.security.keymint.KeyCharacteristics[] getKeyCharacteristics(byte[] keyBlob, byte[] appId, byte[] appData) throws android.os.RemoteException
    {
      return null;
    }
    @Override public byte[] getRootOfTrustChallenge() throws android.os.RemoteException
    {
      return null;
    }
    @Override public byte[] getRootOfTrust(byte[] challenge) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void sendRootOfTrust(byte[] rootOfTrust) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.security.keymint.IKeyMintDevice
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.security.keymint.IKeyMintDevice interface,
     * generating a proxy if needed.
     */
    public static android.hardware.security.keymint.IKeyMintDevice asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.security.keymint.IKeyMintDevice))) {
        return ((android.hardware.security.keymint.IKeyMintDevice)iin);
      }
      return new android.hardware.security.keymint.IKeyMintDevice.Stub.Proxy(obj);
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
        case TRANSACTION_getHardwareInfo:
        {
          return "getHardwareInfo";
        }
        case TRANSACTION_addRngEntropy:
        {
          return "addRngEntropy";
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
        case TRANSACTION_upgradeKey:
        {
          return "upgradeKey";
        }
        case TRANSACTION_deleteKey:
        {
          return "deleteKey";
        }
        case TRANSACTION_deleteAllKeys:
        {
          return "deleteAllKeys";
        }
        case TRANSACTION_destroyAttestationIds:
        {
          return "destroyAttestationIds";
        }
        case TRANSACTION_begin:
        {
          return "begin";
        }
        case TRANSACTION_deviceLocked:
        {
          return "deviceLocked";
        }
        case TRANSACTION_earlyBootEnded:
        {
          return "earlyBootEnded";
        }
        case TRANSACTION_convertStorageKeyToEphemeral:
        {
          return "convertStorageKeyToEphemeral";
        }
        case TRANSACTION_getKeyCharacteristics:
        {
          return "getKeyCharacteristics";
        }
        case TRANSACTION_getRootOfTrustChallenge:
        {
          return "getRootOfTrustChallenge";
        }
        case TRANSACTION_getRootOfTrust:
        {
          return "getRootOfTrust";
        }
        case TRANSACTION_sendRootOfTrust:
        {
          return "sendRootOfTrust";
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
        case TRANSACTION_getHardwareInfo:
        {
          android.hardware.security.keymint.KeyMintHardwareInfo _result = this.getHardwareInfo();
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_addRngEntropy:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.addRngEntropy(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_generateKey:
        {
          android.hardware.security.keymint.KeyParameter[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          android.hardware.security.keymint.AttestationKey _arg1;
          _arg1 = data.readTypedObject(android.hardware.security.keymint.AttestationKey.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.security.keymint.KeyCreationResult _result = this.generateKey(_arg0, _arg1);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_importKey:
        {
          android.hardware.security.keymint.KeyParameter[] _arg0;
          _arg0 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          android.hardware.security.keymint.AttestationKey _arg3;
          _arg3 = data.readTypedObject(android.hardware.security.keymint.AttestationKey.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.security.keymint.KeyCreationResult _result = this.importKey(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_importWrappedKey:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          android.hardware.security.keymint.KeyParameter[] _arg3;
          _arg3 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          long _arg4;
          _arg4 = data.readLong();
          long _arg5;
          _arg5 = data.readLong();
          data.enforceNoDataAvail();
          android.hardware.security.keymint.KeyCreationResult _result = this.importWrappedKey(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_upgradeKey:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          android.hardware.security.keymint.KeyParameter[] _arg1;
          _arg1 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          data.enforceNoDataAvail();
          byte[] _result = this.upgradeKey(_arg0, _arg1);
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_deleteKey:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.deleteKey(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_deleteAllKeys:
        {
          this.deleteAllKeys();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_destroyAttestationIds:
        {
          this.destroyAttestationIds();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_begin:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          android.hardware.security.keymint.KeyParameter[] _arg2;
          _arg2 = data.createTypedArray(android.hardware.security.keymint.KeyParameter.CREATOR);
          android.hardware.security.keymint.HardwareAuthToken _arg3;
          _arg3 = data.readTypedObject(android.hardware.security.keymint.HardwareAuthToken.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.security.keymint.BeginResult _result = this.begin(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          reply.writeTypedObject(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_deviceLocked:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          android.hardware.security.secureclock.TimeStampToken _arg1;
          _arg1 = data.readTypedObject(android.hardware.security.secureclock.TimeStampToken.CREATOR);
          data.enforceNoDataAvail();
          this.deviceLocked(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_earlyBootEnded:
        {
          this.earlyBootEnded();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_convertStorageKeyToEphemeral:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          byte[] _result = this.convertStorageKeyToEphemeral(_arg0);
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_getKeyCharacteristics:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          data.enforceNoDataAvail();
          android.hardware.security.keymint.KeyCharacteristics[] _result = this.getKeyCharacteristics(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_getRootOfTrustChallenge:
        {
          byte[] _result = this.getRootOfTrustChallenge();
          reply.writeNoException();
          reply.writeFixedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE, 16);
          break;
        }
        case TRANSACTION_getRootOfTrust:
        {
          byte[] _arg0;
          _arg0 = data.createFixedArray(byte[].class, 16);
          data.enforceNoDataAvail();
          byte[] _result = this.getRootOfTrust(_arg0);
          reply.writeNoException();
          reply.writeByteArray(_result);
          break;
        }
        case TRANSACTION_sendRootOfTrust:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.sendRootOfTrust(_arg0);
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
    private static class Proxy implements android.hardware.security.keymint.IKeyMintDevice
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
      @Override public android.hardware.security.keymint.KeyMintHardwareInfo getHardwareInfo() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.security.keymint.KeyMintHardwareInfo _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHardwareInfo, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method getHardwareInfo is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.security.keymint.KeyMintHardwareInfo.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void addRngEntropy(byte[] data) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(data);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addRngEntropy, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method addRngEntropy is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.hardware.security.keymint.KeyCreationResult generateKey(android.hardware.security.keymint.KeyParameter[] keyParams, android.hardware.security.keymint.AttestationKey attestationKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.security.keymint.KeyCreationResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(keyParams, 0);
          _data.writeTypedObject(attestationKey, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_generateKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method generateKey is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.security.keymint.KeyCreationResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.security.keymint.KeyCreationResult importKey(android.hardware.security.keymint.KeyParameter[] keyParams, int keyFormat, byte[] keyData, android.hardware.security.keymint.AttestationKey attestationKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.security.keymint.KeyCreationResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedArray(keyParams, 0);
          _data.writeInt(keyFormat);
          _data.writeByteArray(keyData);
          _data.writeTypedObject(attestationKey, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_importKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method importKey is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.security.keymint.KeyCreationResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.security.keymint.KeyCreationResult importWrappedKey(byte[] wrappedKeyData, byte[] wrappingKeyBlob, byte[] maskingKey, android.hardware.security.keymint.KeyParameter[] unwrappingParams, long passwordSid, long biometricSid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.security.keymint.KeyCreationResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(wrappedKeyData);
          _data.writeByteArray(wrappingKeyBlob);
          _data.writeByteArray(maskingKey);
          _data.writeTypedArray(unwrappingParams, 0);
          _data.writeLong(passwordSid);
          _data.writeLong(biometricSid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_importWrappedKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method importWrappedKey is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.security.keymint.KeyCreationResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public byte[] upgradeKey(byte[] keyBlobToUpgrade, android.hardware.security.keymint.KeyParameter[] upgradeParams) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(keyBlobToUpgrade);
          _data.writeTypedArray(upgradeParams, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_upgradeKey, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method upgradeKey is unimplemented.");
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
      @Override public void deleteKey(byte[] keyBlob) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(keyBlob);
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
      @Override public void deleteAllKeys() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deleteAllKeys, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method deleteAllKeys is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void destroyAttestationIds() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroyAttestationIds, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method destroyAttestationIds is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.hardware.security.keymint.BeginResult begin(int purpose, byte[] keyBlob, android.hardware.security.keymint.KeyParameter[] params, android.hardware.security.keymint.HardwareAuthToken authToken) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.security.keymint.BeginResult _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(purpose);
          _data.writeByteArray(keyBlob);
          _data.writeTypedArray(params, 0);
          _data.writeTypedObject(authToken, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_begin, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method begin is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readTypedObject(android.hardware.security.keymint.BeginResult.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void deviceLocked(boolean passwordOnly, android.hardware.security.secureclock.TimeStampToken timestampToken) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(passwordOnly);
          _data.writeTypedObject(timestampToken, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_deviceLocked, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method deviceLocked is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void earlyBootEnded() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_earlyBootEnded, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method earlyBootEnded is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public byte[] convertStorageKeyToEphemeral(byte[] storageKeyBlob) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(storageKeyBlob);
          boolean _status = mRemote.transact(Stub.TRANSACTION_convertStorageKeyToEphemeral, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method convertStorageKeyToEphemeral is unimplemented.");
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
      @Override public android.hardware.security.keymint.KeyCharacteristics[] getKeyCharacteristics(byte[] keyBlob, byte[] appId, byte[] appData) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.security.keymint.KeyCharacteristics[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(keyBlob);
          _data.writeByteArray(appId);
          _data.writeByteArray(appData);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getKeyCharacteristics, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method getKeyCharacteristics is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.security.keymint.KeyCharacteristics.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public byte[] getRootOfTrustChallenge() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRootOfTrustChallenge, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method getRootOfTrustChallenge is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createFixedArray(byte[].class, 16);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public byte[] getRootOfTrust(byte[] challenge) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        byte[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFixedArray(challenge, 0, 16);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRootOfTrust, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method getRootOfTrust is unimplemented.");
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
      @Override public void sendRootOfTrust(byte[] rootOfTrust) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(rootOfTrust);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendRootOfTrust, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          if (!_status) {
            throw new android.os.RemoteException("Method sendRootOfTrust is unimplemented.");
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
    static final int TRANSACTION_getHardwareInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_addRngEntropy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_generateKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_importKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_importWrappedKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_upgradeKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_deleteKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_deleteAllKeys = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_destroyAttestationIds = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_begin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_deviceLocked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_earlyBootEnded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_convertStorageKeyToEphemeral = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_getKeyCharacteristics = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_getRootOfTrustChallenge = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_getRootOfTrust = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_sendRootOfTrust = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$security$keymint$IKeyMintDevice".replace('$', '.');
  public static final int AUTH_TOKEN_MAC_LENGTH = 32;
  public android.hardware.security.keymint.KeyMintHardwareInfo getHardwareInfo() throws android.os.RemoteException;
  public void addRngEntropy(byte[] data) throws android.os.RemoteException;
  public android.hardware.security.keymint.KeyCreationResult generateKey(android.hardware.security.keymint.KeyParameter[] keyParams, android.hardware.security.keymint.AttestationKey attestationKey) throws android.os.RemoteException;
  public android.hardware.security.keymint.KeyCreationResult importKey(android.hardware.security.keymint.KeyParameter[] keyParams, int keyFormat, byte[] keyData, android.hardware.security.keymint.AttestationKey attestationKey) throws android.os.RemoteException;
  public android.hardware.security.keymint.KeyCreationResult importWrappedKey(byte[] wrappedKeyData, byte[] wrappingKeyBlob, byte[] maskingKey, android.hardware.security.keymint.KeyParameter[] unwrappingParams, long passwordSid, long biometricSid) throws android.os.RemoteException;
  public byte[] upgradeKey(byte[] keyBlobToUpgrade, android.hardware.security.keymint.KeyParameter[] upgradeParams) throws android.os.RemoteException;
  public void deleteKey(byte[] keyBlob) throws android.os.RemoteException;
  public void deleteAllKeys() throws android.os.RemoteException;
  public void destroyAttestationIds() throws android.os.RemoteException;
  public android.hardware.security.keymint.BeginResult begin(int purpose, byte[] keyBlob, android.hardware.security.keymint.KeyParameter[] params, android.hardware.security.keymint.HardwareAuthToken authToken) throws android.os.RemoteException;
  public void deviceLocked(boolean passwordOnly, android.hardware.security.secureclock.TimeStampToken timestampToken) throws android.os.RemoteException;
  public void earlyBootEnded() throws android.os.RemoteException;
  public byte[] convertStorageKeyToEphemeral(byte[] storageKeyBlob) throws android.os.RemoteException;
  public android.hardware.security.keymint.KeyCharacteristics[] getKeyCharacteristics(byte[] keyBlob, byte[] appId, byte[] appData) throws android.os.RemoteException;
  public byte[] getRootOfTrustChallenge() throws android.os.RemoteException;
  public byte[] getRootOfTrust(byte[] challenge) throws android.os.RemoteException;
  public void sendRootOfTrust(byte[] rootOfTrust) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
