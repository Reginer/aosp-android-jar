/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java -Weverything -Wno-missing-permission-annotation -t --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/security/keymint/aidl/android.hardware.security.keymint_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/security/secureclock/aidl/android.hardware.security.secureclock_interface/1/preprocessed.aidl --ninja -d out/soong/.intermediates/system/security/keystore2/aidl/android.security.authorization-java-source/gen/android/security/authorization/IKeystoreAuthorization.java.d -o out/soong/.intermediates/system/security/keystore2/aidl/android.security.authorization-java-source/gen -Nsystem/security/keystore2/aidl system/security/keystore2/aidl/android/security/authorization/IKeystoreAuthorization.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.security.authorization;
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
     * Tells Keystore that the device is now unlocked for a user.  Requires the 'Unlock' permission.
     * 
     * This method makes Keystore start allowing the use of the given user's keys that require an
     * unlocked device, following the device boot or an earlier call to onDeviceLocked() which
     * disabled the use of such keys.  In addition, once per boot, this method must be called with a
     * password before keys that require user authentication can be used.
     * 
     * This method does two things to restore access to UnlockedDeviceRequired keys.  First, it sets
     * a flag that indicates the user is unlocked.  This is always done, and it makes Keystore's
     * logical enforcement of UnlockedDeviceRequired start passing.  Second, it recovers and caches
     * the user's UnlockedDeviceRequired super keys.  This succeeds only in the following cases:
     * 
     *  - The (correct) password is provided, proving that the user has authenticated using LSKF or
     *    equivalent.  This is the most powerful type of unlock.  Keystore uses the password to
     *    decrypt the user's UnlockedDeviceRequired super keys from disk.  It also uses the password
     *    to decrypt the user's AfterFirstUnlock super key from disk, if not already done.
     * 
     *  - The user's UnlockedDeviceRequired super keys are cached in biometric-encrypted form, and a
     *    matching valid HardwareAuthToken has been added to Keystore.  I.e., class 3 biometric
     *    unlock is enabled and the user recently authenticated using a class 3 biometric.  The keys
     *    are cached in biometric-encrypted form if onDeviceLocked() was called with a nonempty list
     *    of unlockingSids, and onNonLskfUnlockMethodsExpired() was not called later.
     * 
     *  - The user's UnlockedDeviceRequired super keys are already cached in plaintext.  This is the
     *    case if onDeviceLocked() was called with weakUnlockEnabled=true, and
     *    onWeakUnlockMethodsExpired() was not called later.  This case provides only
     *    Keystore-enforced logical security for UnlockedDeviceRequired.
     * 
     * ## Error conditions:
     * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Unlock' permission.
     * `ResponseCode::VALUE_CORRUPTED` - if a super key can not be decrypted.
     * `ResponseCode::KEY_NOT_FOUND` - if a super key is not found.
     * `ResponseCode::SYSTEM_ERROR` - if another error occurred.
     * 
     * @param userId The Android user ID of the user for which the device is now unlocked
     * @param password If available, a secret derived from the user's synthetic password
     */
    @Override public void onDeviceUnlocked(int userId, byte[] password) throws android.os.RemoteException
    {
    }
    /**
     * Tells Keystore that the device is now locked for a user.  Requires the 'Lock' permission.
     * 
     * This method makes Keystore stop allowing the use of the given user's keys that require an
     * unlocked device.  This is enforced logically, and when possible it's also enforced
     * cryptographically by wiping the UnlockedDeviceRequired super keys from memory.
     * 
     * unlockingSids and weakUnlockEnabled specify the methods by which the device can become
     * unlocked for the user, in addition to LSKF-equivalent authentication.
     * 
     * unlockingSids is the list of SIDs of class 3 (strong) biometrics that can unlock.  If
     * unlockingSids is non-empty, then this method saves a copy of the UnlockedDeviceRequired super
     * keys in memory encrypted by a new AES key that is imported into KeyMint and configured to be
     * usable only when user authentication has occurred using any of the SIDs.  This allows the
     * keys to be recovered if the device is unlocked using a class 3 biometric.
     * 
     * weakUnlockEnabled is true if the unlock can happen using a method that does not have an
     * associated SID, such as a class 1 (convenience) biometric, class 2 (weak) biometric, or trust
     * agent.  These methods don't count as "authentication" from Keystore's perspective.  In this
     * case, Keystore keeps a copy of the UnlockedDeviceRequired super keys in memory in plaintext,
     * providing only logical security for UnlockedDeviceRequired.
     * 
     * ## Error conditions:
     * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
     * 
     * @param userId The Android user ID of the user for which the device is now locked
     * @param unlockingSids SIDs of class 3 biometrics that can unlock the device for the user
     * @param weakUnlockEnabled Whether a weak unlock method can unlock the device for the user
     */
    @Override public void onDeviceLocked(int userId, long[] unlockingSids, boolean weakUnlockEnabled) throws android.os.RemoteException
    {
    }
    /**
     * Tells Keystore that weak unlock methods can no longer unlock the device for the given user.
     * This is intended to be called after an earlier call to onDeviceLocked() with
     * weakUnlockEnabled=true.  It upgrades the security level of UnlockedDeviceRequired keys to
     * that which would have resulted from calling onDeviceLocked() with weakUnlockEnabled=false.
     * 
     * ## Error conditions:
     * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
     * 
     * @param userId The Android user ID of the user for which weak unlock methods have expired
     */
    @Override public void onWeakUnlockMethodsExpired(int userId) throws android.os.RemoteException
    {
    }
    /**
     * Tells Keystore that non-LSKF-equivalent unlock methods can no longer unlock the device for
     * the given user.  This is intended to be called after an earlier call to onDeviceLocked() with
     * nonempty unlockingSids.  It upgrades the security level of UnlockedDeviceRequired keys to
     * that which would have resulted from calling onDeviceLocked() with unlockingSids=[] and
     * weakUnlockEnabled=false.
     * 
     * ## Error conditions:
     * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
     * 
     * @param userId The Android user ID of the user for which non-LSKF unlock methods have expired
     */
    @Override public void onNonLskfUnlockMethodsExpired(int userId) throws android.os.RemoteException
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
    /**
     * Returns the last successful authentication time since boot for the given user with any of the
     * given authenticator types. This is determined by inspecting the cached auth tokens.
     * 
     * ## Error conditions:
     * `ResponseCode::NO_AUTH_TOKEN_FOUND` - if there is no matching authentication token found
     */
    @Override public long getLastAuthTime(long secureUserId, int[] authTypes) throws android.os.RemoteException
    {
      return 0L;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.security.authorization.IKeystoreAuthorization
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
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
        case TRANSACTION_onDeviceUnlocked:
        {
          return "onDeviceUnlocked";
        }
        case TRANSACTION_onDeviceLocked:
        {
          return "onDeviceLocked";
        }
        case TRANSACTION_onWeakUnlockMethodsExpired:
        {
          return "onWeakUnlockMethodsExpired";
        }
        case TRANSACTION_onNonLskfUnlockMethodsExpired:
        {
          return "onNonLskfUnlockMethodsExpired";
        }
        case TRANSACTION_getAuthTokensForCredStore:
        {
          return "getAuthTokensForCredStore";
        }
        case TRANSACTION_getLastAuthTime:
        {
          return "getLastAuthTime";
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
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
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
        case TRANSACTION_onDeviceUnlocked:
        {
          int _arg0;
          _arg0 = data.readInt();
          byte[] _arg1;
          _arg1 = data.createByteArray();
          data.enforceNoDataAvail();
          this.onDeviceUnlocked(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onDeviceLocked:
        {
          int _arg0;
          _arg0 = data.readInt();
          long[] _arg1;
          _arg1 = data.createLongArray();
          boolean _arg2;
          _arg2 = data.readBoolean();
          data.enforceNoDataAvail();
          this.onDeviceLocked(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onWeakUnlockMethodsExpired:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onWeakUnlockMethodsExpired(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onNonLskfUnlockMethodsExpired:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onNonLskfUnlockMethodsExpired(_arg0);
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
        case TRANSACTION_getLastAuthTime:
        {
          long _arg0;
          _arg0 = data.readLong();
          int[] _arg1;
          _arg1 = data.createIntArray();
          data.enforceNoDataAvail();
          long _result = this.getLastAuthTime(_arg0, _arg1);
          reply.writeNoException();
          reply.writeLong(_result);
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
       * Tells Keystore that the device is now unlocked for a user.  Requires the 'Unlock' permission.
       * 
       * This method makes Keystore start allowing the use of the given user's keys that require an
       * unlocked device, following the device boot or an earlier call to onDeviceLocked() which
       * disabled the use of such keys.  In addition, once per boot, this method must be called with a
       * password before keys that require user authentication can be used.
       * 
       * This method does two things to restore access to UnlockedDeviceRequired keys.  First, it sets
       * a flag that indicates the user is unlocked.  This is always done, and it makes Keystore's
       * logical enforcement of UnlockedDeviceRequired start passing.  Second, it recovers and caches
       * the user's UnlockedDeviceRequired super keys.  This succeeds only in the following cases:
       * 
       *  - The (correct) password is provided, proving that the user has authenticated using LSKF or
       *    equivalent.  This is the most powerful type of unlock.  Keystore uses the password to
       *    decrypt the user's UnlockedDeviceRequired super keys from disk.  It also uses the password
       *    to decrypt the user's AfterFirstUnlock super key from disk, if not already done.
       * 
       *  - The user's UnlockedDeviceRequired super keys are cached in biometric-encrypted form, and a
       *    matching valid HardwareAuthToken has been added to Keystore.  I.e., class 3 biometric
       *    unlock is enabled and the user recently authenticated using a class 3 biometric.  The keys
       *    are cached in biometric-encrypted form if onDeviceLocked() was called with a nonempty list
       *    of unlockingSids, and onNonLskfUnlockMethodsExpired() was not called later.
       * 
       *  - The user's UnlockedDeviceRequired super keys are already cached in plaintext.  This is the
       *    case if onDeviceLocked() was called with weakUnlockEnabled=true, and
       *    onWeakUnlockMethodsExpired() was not called later.  This case provides only
       *    Keystore-enforced logical security for UnlockedDeviceRequired.
       * 
       * ## Error conditions:
       * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Unlock' permission.
       * `ResponseCode::VALUE_CORRUPTED` - if a super key can not be decrypted.
       * `ResponseCode::KEY_NOT_FOUND` - if a super key is not found.
       * `ResponseCode::SYSTEM_ERROR` - if another error occurred.
       * 
       * @param userId The Android user ID of the user for which the device is now unlocked
       * @param password If available, a secret derived from the user's synthetic password
       */
      @Override public void onDeviceUnlocked(int userId, byte[] password) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(userId);
          _data.writeByteArray(password);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDeviceUnlocked, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Tells Keystore that the device is now locked for a user.  Requires the 'Lock' permission.
       * 
       * This method makes Keystore stop allowing the use of the given user's keys that require an
       * unlocked device.  This is enforced logically, and when possible it's also enforced
       * cryptographically by wiping the UnlockedDeviceRequired super keys from memory.
       * 
       * unlockingSids and weakUnlockEnabled specify the methods by which the device can become
       * unlocked for the user, in addition to LSKF-equivalent authentication.
       * 
       * unlockingSids is the list of SIDs of class 3 (strong) biometrics that can unlock.  If
       * unlockingSids is non-empty, then this method saves a copy of the UnlockedDeviceRequired super
       * keys in memory encrypted by a new AES key that is imported into KeyMint and configured to be
       * usable only when user authentication has occurred using any of the SIDs.  This allows the
       * keys to be recovered if the device is unlocked using a class 3 biometric.
       * 
       * weakUnlockEnabled is true if the unlock can happen using a method that does not have an
       * associated SID, such as a class 1 (convenience) biometric, class 2 (weak) biometric, or trust
       * agent.  These methods don't count as "authentication" from Keystore's perspective.  In this
       * case, Keystore keeps a copy of the UnlockedDeviceRequired super keys in memory in plaintext,
       * providing only logical security for UnlockedDeviceRequired.
       * 
       * ## Error conditions:
       * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
       * 
       * @param userId The Android user ID of the user for which the device is now locked
       * @param unlockingSids SIDs of class 3 biometrics that can unlock the device for the user
       * @param weakUnlockEnabled Whether a weak unlock method can unlock the device for the user
       */
      @Override public void onDeviceLocked(int userId, long[] unlockingSids, boolean weakUnlockEnabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(userId);
          _data.writeLongArray(unlockingSids);
          _data.writeBoolean(weakUnlockEnabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDeviceLocked, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Tells Keystore that weak unlock methods can no longer unlock the device for the given user.
       * This is intended to be called after an earlier call to onDeviceLocked() with
       * weakUnlockEnabled=true.  It upgrades the security level of UnlockedDeviceRequired keys to
       * that which would have resulted from calling onDeviceLocked() with weakUnlockEnabled=false.
       * 
       * ## Error conditions:
       * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
       * 
       * @param userId The Android user ID of the user for which weak unlock methods have expired
       */
      @Override public void onWeakUnlockMethodsExpired(int userId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(userId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onWeakUnlockMethodsExpired, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Tells Keystore that non-LSKF-equivalent unlock methods can no longer unlock the device for
       * the given user.  This is intended to be called after an earlier call to onDeviceLocked() with
       * nonempty unlockingSids.  It upgrades the security level of UnlockedDeviceRequired keys to
       * that which would have resulted from calling onDeviceLocked() with unlockingSids=[] and
       * weakUnlockEnabled=false.
       * 
       * ## Error conditions:
       * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
       * 
       * @param userId The Android user ID of the user for which non-LSKF unlock methods have expired
       */
      @Override public void onNonLskfUnlockMethodsExpired(int userId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(userId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onNonLskfUnlockMethodsExpired, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
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
      /**
       * Returns the last successful authentication time since boot for the given user with any of the
       * given authenticator types. This is determined by inspecting the cached auth tokens.
       * 
       * ## Error conditions:
       * `ResponseCode::NO_AUTH_TOKEN_FOUND` - if there is no matching authentication token found
       */
      @Override public long getLastAuthTime(long secureUserId, int[] authTypes) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        _data.markSensitive();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(secureUserId);
          _data.writeIntArray(authTypes);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLastAuthTime, _data, _reply, android.os.IBinder.FLAG_CLEAR_BUF);
          _reply.readException();
          _result = _reply.readLong();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_addAuthToken = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onDeviceUnlocked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onDeviceLocked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onWeakUnlockMethodsExpired = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onNonLskfUnlockMethodsExpired = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getAuthTokensForCredStore = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_getLastAuthTime = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 6;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android.security.authorization.IKeystoreAuthorization";
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
   * Tells Keystore that the device is now unlocked for a user.  Requires the 'Unlock' permission.
   * 
   * This method makes Keystore start allowing the use of the given user's keys that require an
   * unlocked device, following the device boot or an earlier call to onDeviceLocked() which
   * disabled the use of such keys.  In addition, once per boot, this method must be called with a
   * password before keys that require user authentication can be used.
   * 
   * This method does two things to restore access to UnlockedDeviceRequired keys.  First, it sets
   * a flag that indicates the user is unlocked.  This is always done, and it makes Keystore's
   * logical enforcement of UnlockedDeviceRequired start passing.  Second, it recovers and caches
   * the user's UnlockedDeviceRequired super keys.  This succeeds only in the following cases:
   * 
   *  - The (correct) password is provided, proving that the user has authenticated using LSKF or
   *    equivalent.  This is the most powerful type of unlock.  Keystore uses the password to
   *    decrypt the user's UnlockedDeviceRequired super keys from disk.  It also uses the password
   *    to decrypt the user's AfterFirstUnlock super key from disk, if not already done.
   * 
   *  - The user's UnlockedDeviceRequired super keys are cached in biometric-encrypted form, and a
   *    matching valid HardwareAuthToken has been added to Keystore.  I.e., class 3 biometric
   *    unlock is enabled and the user recently authenticated using a class 3 biometric.  The keys
   *    are cached in biometric-encrypted form if onDeviceLocked() was called with a nonempty list
   *    of unlockingSids, and onNonLskfUnlockMethodsExpired() was not called later.
   * 
   *  - The user's UnlockedDeviceRequired super keys are already cached in plaintext.  This is the
   *    case if onDeviceLocked() was called with weakUnlockEnabled=true, and
   *    onWeakUnlockMethodsExpired() was not called later.  This case provides only
   *    Keystore-enforced logical security for UnlockedDeviceRequired.
   * 
   * ## Error conditions:
   * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Unlock' permission.
   * `ResponseCode::VALUE_CORRUPTED` - if a super key can not be decrypted.
   * `ResponseCode::KEY_NOT_FOUND` - if a super key is not found.
   * `ResponseCode::SYSTEM_ERROR` - if another error occurred.
   * 
   * @param userId The Android user ID of the user for which the device is now unlocked
   * @param password If available, a secret derived from the user's synthetic password
   */
  public void onDeviceUnlocked(int userId, byte[] password) throws android.os.RemoteException;
  /**
   * Tells Keystore that the device is now locked for a user.  Requires the 'Lock' permission.
   * 
   * This method makes Keystore stop allowing the use of the given user's keys that require an
   * unlocked device.  This is enforced logically, and when possible it's also enforced
   * cryptographically by wiping the UnlockedDeviceRequired super keys from memory.
   * 
   * unlockingSids and weakUnlockEnabled specify the methods by which the device can become
   * unlocked for the user, in addition to LSKF-equivalent authentication.
   * 
   * unlockingSids is the list of SIDs of class 3 (strong) biometrics that can unlock.  If
   * unlockingSids is non-empty, then this method saves a copy of the UnlockedDeviceRequired super
   * keys in memory encrypted by a new AES key that is imported into KeyMint and configured to be
   * usable only when user authentication has occurred using any of the SIDs.  This allows the
   * keys to be recovered if the device is unlocked using a class 3 biometric.
   * 
   * weakUnlockEnabled is true if the unlock can happen using a method that does not have an
   * associated SID, such as a class 1 (convenience) biometric, class 2 (weak) biometric, or trust
   * agent.  These methods don't count as "authentication" from Keystore's perspective.  In this
   * case, Keystore keeps a copy of the UnlockedDeviceRequired super keys in memory in plaintext,
   * providing only logical security for UnlockedDeviceRequired.
   * 
   * ## Error conditions:
   * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
   * 
   * @param userId The Android user ID of the user for which the device is now locked
   * @param unlockingSids SIDs of class 3 biometrics that can unlock the device for the user
   * @param weakUnlockEnabled Whether a weak unlock method can unlock the device for the user
   */
  public void onDeviceLocked(int userId, long[] unlockingSids, boolean weakUnlockEnabled) throws android.os.RemoteException;
  /**
   * Tells Keystore that weak unlock methods can no longer unlock the device for the given user.
   * This is intended to be called after an earlier call to onDeviceLocked() with
   * weakUnlockEnabled=true.  It upgrades the security level of UnlockedDeviceRequired keys to
   * that which would have resulted from calling onDeviceLocked() with weakUnlockEnabled=false.
   * 
   * ## Error conditions:
   * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
   * 
   * @param userId The Android user ID of the user for which weak unlock methods have expired
   */
  public void onWeakUnlockMethodsExpired(int userId) throws android.os.RemoteException;
  /**
   * Tells Keystore that non-LSKF-equivalent unlock methods can no longer unlock the device for
   * the given user.  This is intended to be called after an earlier call to onDeviceLocked() with
   * nonempty unlockingSids.  It upgrades the security level of UnlockedDeviceRequired keys to
   * that which would have resulted from calling onDeviceLocked() with unlockingSids=[] and
   * weakUnlockEnabled=false.
   * 
   * ## Error conditions:
   * `ResponseCode::PERMISSION_DENIED` - if the caller does not have the 'Lock' permission.
   * 
   * @param userId The Android user ID of the user for which non-LSKF unlock methods have expired
   */
  public void onNonLskfUnlockMethodsExpired(int userId) throws android.os.RemoteException;
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
  /**
   * Returns the last successful authentication time since boot for the given user with any of the
   * given authenticator types. This is determined by inspecting the cached auth tokens.
   * 
   * ## Error conditions:
   * `ResponseCode::NO_AUTH_TOKEN_FOUND` - if there is no matching authentication token found
   */
  public long getLastAuthTime(long secureUserId, int[] authTypes) throws android.os.RemoteException;
}
