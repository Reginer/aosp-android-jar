/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.security.authorization;
// TODO: mark the interface with @SensitiveData when the annotation is ready (b/176110256).
/**
 * IKeystoreAuthorization interface exposes the methods for other system components to
 * provide keystore with the information required to enforce authorizations on key usage.
 * @hide
 */
public interface IKeystoreAuthorization extends android.os.IInterface
{
  /** Default implementation for IKeystoreAuthorization. */
  public static class Default implements android.security.authorization.IKeystoreAuthorization
  {
    /**
     * Allows the Android authenticators to hand over an auth token to Keystore.
     * Callers require 'AddAuth' permission.
     * ## Error conditions:
     * `ResponseCode::PERMISSION_DENIED` - if the callers do not have the 'AddAuth' permission.
     * `ResponseCode::SYSTEM_ERROR` - if failed to store the auth token in the database or if failed
     * to add the auth token to the operation, if it is a per-op auth token.
     * 
     * @param authToken The auth token created by an authenticator, upon user authentication.
     */
    @Override public void addAuthToken(android.hardware.security.keymint.HardwareAuthToken authToken) throws android.os.RemoteException
    {
    }
    /**
     * Unlocks the keystore for the given user id.
     * 
     * Callers require 'Unlock' permission.
     * 
     * Super-Encryption Key:
     * When the device is unlocked (and password is non-null), Keystore stores in memory
     * a super-encryption key derived from the password that protects UNLOCKED_DEVICE_REQUIRED
     * keys; this key is wiped from memory when the device is locked.
     * 
     * If unlockingSids is non-empty on lock, then before the super-encryption key is wiped from
     * memory, a copy of it is stored in memory encrypted with a fresh AES key. This key is then
     * imported into KM, tagged such that it can be used given a valid, recent auth token for any
     * of the unlockingSids.
     * 
     * Options for unlock:
     *  - If the password is non-null, the super-encryption key is re-derived as above.
     *  - If the password is null, then if a suitable auth token to access the encrypted
     *    Super-encryption key stored in KM has been sent to keystore (via addAuthToken), the
     *    encrypted super-encryption key is recovered so that UNLOCKED_DEVICE_REQUIRED keys can
     *    be used once again.
     *  - If neither of these are met, then the operation fails.
     * 
     * ## Error conditions:
     * `ResponseCode::PERMISSION_DENIED` - if the callers do not have the 'Unlock' permission.
     * `ResponseCode::SYSTEM_ERROR` - if failed to perform lock/unlock operations due to various
     * `ResponseCode::VALUE_CORRUPTED` - if the super key can not be decrypted.
     * `ResponseCode::KEY_NOT_FOUND` - if the super key is not found.
     * 
     * @param lockScreenEvent whether the lock screen locked or unlocked
     * @param userId android user id
     * @param password synthetic password derived from the user's LSKF, must be null on lock
     * @param unlockingSids list of biometric SIDs for this user, ignored on unlock
     */
    @Override public void onLockScreenEvent(int lockScreenEvent, int userId, byte[] password, long[] unlockingSids) throws android.os.RemoteException
    {
    }
    /**
     * Allows Credstore to retrieve a HardwareAuthToken and a TimestampToken.
     * Identity Credential Trusted App can run either in the TEE or in other secure Hardware.
     * So, credstore always need to retrieve a TimestampToken along with a HardwareAuthToken.
     * 
     * The passed in |challenge| parameter must always be non-zero.
     * 
     * The returned TimestampToken will always have its |challenge| field set to
     * the |challenge| parameter.
     * 
     * This method looks through auth-tokens cached by keystore which match
     * the passed-in |secureUserId|.
     * The most recent matching auth token which has a |challenge| field which matches
     * the passed-in |challenge| parameter is returned.
     * In this case the |authTokenMaxAgeMillis| parameter is not used.
     * 
     * Otherwise, the most recent matching auth token which is younger
     * than |authTokenMaxAgeMillis| is returned.
     * 
     * This method is called by credstore (and only credstore).
     * 
     * The caller requires 'get_auth_token' permission.
     * 
     * ## Error conditions:
     * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'get_auth_token'
     *                                     permission.
     * `ResponseCode::SYSTEM_ERROR` - if failed to obtain an authtoken from the database.
     * `ResponseCode::NO_AUTH_TOKEN_FOUND` - a matching auth token is not found.
     * `ResponseCode::INVALID_ARGUMENT` - if the passed-in |challenge| parameter is zero.
     */
    @Override public android.security.authorization.AuthorizationTokens getAuthTokensForCredStore(long challenge, long secureUserId, long authTokenMaxAgeMillis) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.security.authorization.IKeystoreAuthorization
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.security.authorization.IKeystoreAuthorization interface,
     * generating a proxy if needed.
     */
    public static android.security.authorization.IKeystoreAuthorization asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.security.authorization.IKeystoreAuthorization))) {
        return ((android.security.authorization.IKeystoreAuthorization)iin);
      }
      return new android.security.authorization.IKeystoreAuthorization.Stub.Proxy(obj);
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
        case TRANSACTION_addAuthToken:
        {
          return "addAuthToken";
        }
        case TRANSACTION_onLockScreenEvent:
        {
          return "onLockScreenEvent";
        }
        case TRANSACTION_getAuthTokensForCredStore:
        {
          return "getAuthTokensForCredStore";
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
        case TRANSACTION_addAuthToken:
        {
          android.hardware.security.keymint.HardwareAuthToken _arg0;
          _arg0 = data.readTypedObject(android.hardware.security.keymint.HardwareAuthToken.CREATOR);
          data.enforceNoDataAvail();
          this.addAuthToken(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onLockScreenEvent:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          long[] _arg3;
          _arg3 = data.createLongArray();
          data.enforceNoDataAvail();
          this.onLockScreenEvent(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getAuthTokensForCredStore:
        {
          long _arg0;
          _arg0 = data.readLong();
          long _arg1;
          _arg1 = data.readLong();
          long _arg2;
          _arg2 = data.readLong();
          data.enforceNoDataAvail();
          android.security.authorization.AuthorizationTokens _result = this.getAuthTokensForCredStore(_arg0, _arg1, _arg2);
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
    private static class Proxy implements android.security.authorization.IKeystoreAuthorization
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
       * Allows the Android authenticators to hand over an auth token to Keystore.
       * Callers require 'AddAuth' permission.
       * ## Error conditions:
       * `ResponseCode::PERMISSION_DENIED` - if the callers do not have the 'AddAuth' permission.
       * `ResponseCode::SYSTEM_ERROR` - if failed to store the auth token in the database or if failed
       * to add the auth token to the operation, if it is a per-op auth token.
       * 
       * @param authToken The auth token created by an authenticator, upon user authentication.
       */
      @Override public void addAuthToken(android.hardware.security.keymint.HardwareAuthToken authToken) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(authToken, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addAuthToken, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Unlocks the keystore for the given user id.
       * 
       * Callers require 'Unlock' permission.
       * 
       * Super-Encryption Key:
       * When the device is unlocked (and password is non-null), Keystore stores in memory
       * a super-encryption key derived from the password that protects UNLOCKED_DEVICE_REQUIRED
       * keys; this key is wiped from memory when the device is locked.
       * 
       * If unlockingSids is non-empty on lock, then before the super-encryption key is wiped from
       * memory, a copy of it is stored in memory encrypted with a fresh AES key. This key is then
       * imported into KM, tagged such that it can be used given a valid, recent auth token for any
       * of the unlockingSids.
       * 
       * Options for unlock:
       *  - If the password is non-null, the super-encryption key is re-derived as above.
       *  - If the password is null, then if a suitable auth token to access the encrypted
       *    Super-encryption key stored in KM has been sent to keystore (via addAuthToken), the
       *    encrypted super-encryption key is recovered so that UNLOCKED_DEVICE_REQUIRED keys can
       *    be used once again.
       *  - If neither of these are met, then the operation fails.
       * 
       * ## Error conditions:
       * `ResponseCode::PERMISSION_DENIED` - if the callers do not have the 'Unlock' permission.
       * `ResponseCode::SYSTEM_ERROR` - if failed to perform lock/unlock operations due to various
       * `ResponseCode::VALUE_CORRUPTED` - if the super key can not be decrypted.
       * `ResponseCode::KEY_NOT_FOUND` - if the super key is not found.
       * 
       * @param lockScreenEvent whether the lock screen locked or unlocked
       * @param userId android user id
       * @param password synthetic password derived from the user's LSKF, must be null on lock
       * @param unlockingSids list of biometric SIDs for this user, ignored on unlock
       */
      @Override public void onLockScreenEvent(int lockScreenEvent, int userId, byte[] password, long[] unlockingSids) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(lockScreenEvent);
          _data.writeInt(userId);
          _data.writeByteArray(password);
          _data.writeLongArray(unlockingSids);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onLockScreenEvent, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Allows Credstore to retrieve a HardwareAuthToken and a TimestampToken.
       * Identity Credential Trusted App can run either in the TEE or in other secure Hardware.
       * So, credstore always need to retrieve a TimestampToken along with a HardwareAuthToken.
       * 
       * The passed in |challenge| parameter must always be non-zero.
       * 
       * The returned TimestampToken will always have its |challenge| field set to
       * the |challenge| parameter.
       * 
       * This method looks through auth-tokens cached by keystore which match
       * the passed-in |secureUserId|.
       * The most recent matching auth token which has a |challenge| field which matches
       * the passed-in |challenge| parameter is returned.
       * In this case the |authTokenMaxAgeMillis| parameter is not used.
       * 
       * Otherwise, the most recent matching auth token which is younger
       * than |authTokenMaxAgeMillis| is returned.
       * 
       * This method is called by credstore (and only credstore).
       * 
       * The caller requires 'get_auth_token' permission.
       * 
       * ## Error conditions:
       * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'get_auth_token'
       *                                     permission.
       * `ResponseCode::SYSTEM_ERROR` - if failed to obtain an authtoken from the database.
       * `ResponseCode::NO_AUTH_TOKEN_FOUND` - a matching auth token is not found.
       * `ResponseCode::INVALID_ARGUMENT` - if the passed-in |challenge| parameter is zero.
       */
      @Override public android.security.authorization.AuthorizationTokens getAuthTokensForCredStore(long challenge, long secureUserId, long authTokenMaxAgeMillis) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.security.authorization.AuthorizationTokens _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(challenge);
          _data.writeLong(secureUserId);
          _data.writeLong(authTokenMaxAgeMillis);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAuthTokensForCredStore, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          _reply.readException();
          _result = _reply.readTypedObject(android.security.authorization.AuthorizationTokens.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_addAuthToken = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onLockScreenEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getAuthTokensForCredStore = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 2;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$security$authorization$IKeystoreAuthorization".replace('$', '.');
  /**
   * Allows the Android authenticators to hand over an auth token to Keystore.
   * Callers require 'AddAuth' permission.
   * ## Error conditions:
   * `ResponseCode::PERMISSION_DENIED` - if the callers do not have the 'AddAuth' permission.
   * `ResponseCode::SYSTEM_ERROR` - if failed to store the auth token in the database or if failed
   * to add the auth token to the operation, if it is a per-op auth token.
   * 
   * @param authToken The auth token created by an authenticator, upon user authentication.
   */
  public void addAuthToken(android.hardware.security.keymint.HardwareAuthToken authToken) throws android.os.RemoteException;
  /**
   * Unlocks the keystore for the given user id.
   * 
   * Callers require 'Unlock' permission.
   * 
   * Super-Encryption Key:
   * When the device is unlocked (and password is non-null), Keystore stores in memory
   * a super-encryption key derived from the password that protects UNLOCKED_DEVICE_REQUIRED
   * keys; this key is wiped from memory when the device is locked.
   * 
   * If unlockingSids is non-empty on lock, then before the super-encryption key is wiped from
   * memory, a copy of it is stored in memory encrypted with a fresh AES key. This key is then
   * imported into KM, tagged such that it can be used given a valid, recent auth token for any
   * of the unlockingSids.
   * 
   * Options for unlock:
   *  - If the password is non-null, the super-encryption key is re-derived as above.
   *  - If the password is null, then if a suitable auth token to access the encrypted
   *    Super-encryption key stored in KM has been sent to keystore (via addAuthToken), the
   *    encrypted super-encryption key is recovered so that UNLOCKED_DEVICE_REQUIRED keys can
   *    be used once again.
   *  - If neither of these are met, then the operation fails.
   * 
   * ## Error conditions:
   * `ResponseCode::PERMISSION_DENIED` - if the callers do not have the 'Unlock' permission.
   * `ResponseCode::SYSTEM_ERROR` - if failed to perform lock/unlock operations due to various
   * `ResponseCode::VALUE_CORRUPTED` - if the super key can not be decrypted.
   * `ResponseCode::KEY_NOT_FOUND` - if the super key is not found.
   * 
   * @param lockScreenEvent whether the lock screen locked or unlocked
   * @param userId android user id
   * @param password synthetic password derived from the user's LSKF, must be null on lock
   * @param unlockingSids list of biometric SIDs for this user, ignored on unlock
   */
  public void onLockScreenEvent(int lockScreenEvent, int userId, byte[] password, long[] unlockingSids) throws android.os.RemoteException;
  /**
   * Allows Credstore to retrieve a HardwareAuthToken and a TimestampToken.
   * Identity Credential Trusted App can run either in the TEE or in other secure Hardware.
   * So, credstore always need to retrieve a TimestampToken along with a HardwareAuthToken.
   * 
   * The passed in |challenge| parameter must always be non-zero.
   * 
   * The returned TimestampToken will always have its |challenge| field set to
   * the |challenge| parameter.
   * 
   * This method looks through auth-tokens cached by keystore which match
   * the passed-in |secureUserId|.
   * The most recent matching auth token which has a |challenge| field which matches
   * the passed-in |challenge| parameter is returned.
   * In this case the |authTokenMaxAgeMillis| parameter is not used.
   * 
   * Otherwise, the most recent matching auth token which is younger
   * than |authTokenMaxAgeMillis| is returned.
   * 
   * This method is called by credstore (and only credstore).
   * 
   * The caller requires 'get_auth_token' permission.
   * 
   * ## Error conditions:
   * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'get_auth_token'
   *                                     permission.
   * `ResponseCode::SYSTEM_ERROR` - if failed to obtain an authtoken from the database.
   * `ResponseCode::NO_AUTH_TOKEN_FOUND` - a matching auth token is not found.
   * `ResponseCode::INVALID_ARGUMENT` - if the passed-in |challenge| parameter is zero.
   */
  public android.security.authorization.AuthorizationTokens getAuthTokensForCredStore(long challenge, long secureUserId, long authTokenMaxAgeMillis) throws android.os.RemoteException;
}
