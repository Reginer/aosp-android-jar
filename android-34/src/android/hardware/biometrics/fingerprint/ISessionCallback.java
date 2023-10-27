/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public interface ISessionCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "637371b53fb7faf9bd43aa51b72c23852d6e6d96";
  /** Default implementation for ISessionCallback. */
  public static class Default implements android.hardware.biometrics.fingerprint.ISessionCallback
  {
    @Override public void onChallengeGenerated(long challenge) throws android.os.RemoteException
    {
    }
    @Override public void onChallengeRevoked(long challenge) throws android.os.RemoteException
    {
    }
    @Override public void onAcquired(byte info, int vendorCode) throws android.os.RemoteException
    {
    }
    @Override public void onError(byte error, int vendorCode) throws android.os.RemoteException
    {
    }
    @Override public void onEnrollmentProgress(int enrollmentId, int remaining) throws android.os.RemoteException
    {
    }
    @Override public void onAuthenticationSucceeded(int enrollmentId, android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
    {
    }
    @Override public void onAuthenticationFailed() throws android.os.RemoteException
    {
    }
    @Override public void onLockoutTimed(long durationMillis) throws android.os.RemoteException
    {
    }
    @Override public void onLockoutPermanent() throws android.os.RemoteException
    {
    }
    @Override public void onLockoutCleared() throws android.os.RemoteException
    {
    }
    @Override public void onInteractionDetected() throws android.os.RemoteException
    {
    }
    @Override public void onEnrollmentsEnumerated(int[] enrollmentIds) throws android.os.RemoteException
    {
    }
    @Override public void onEnrollmentsRemoved(int[] enrollmentIds) throws android.os.RemoteException
    {
    }
    @Override public void onAuthenticatorIdRetrieved(long authenticatorId) throws android.os.RemoteException
    {
    }
    @Override public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws android.os.RemoteException
    {
    }
    @Override public void onSessionClosed() throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.biometrics.fingerprint.ISessionCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.biometrics.fingerprint.ISessionCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.biometrics.fingerprint.ISessionCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.biometrics.fingerprint.ISessionCallback))) {
        return ((android.hardware.biometrics.fingerprint.ISessionCallback)iin);
      }
      return new android.hardware.biometrics.fingerprint.ISessionCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onChallengeGenerated:
        {
          return "onChallengeGenerated";
        }
        case TRANSACTION_onChallengeRevoked:
        {
          return "onChallengeRevoked";
        }
        case TRANSACTION_onAcquired:
        {
          return "onAcquired";
        }
        case TRANSACTION_onError:
        {
          return "onError";
        }
        case TRANSACTION_onEnrollmentProgress:
        {
          return "onEnrollmentProgress";
        }
        case TRANSACTION_onAuthenticationSucceeded:
        {
          return "onAuthenticationSucceeded";
        }
        case TRANSACTION_onAuthenticationFailed:
        {
          return "onAuthenticationFailed";
        }
        case TRANSACTION_onLockoutTimed:
        {
          return "onLockoutTimed";
        }
        case TRANSACTION_onLockoutPermanent:
        {
          return "onLockoutPermanent";
        }
        case TRANSACTION_onLockoutCleared:
        {
          return "onLockoutCleared";
        }
        case TRANSACTION_onInteractionDetected:
        {
          return "onInteractionDetected";
        }
        case TRANSACTION_onEnrollmentsEnumerated:
        {
          return "onEnrollmentsEnumerated";
        }
        case TRANSACTION_onEnrollmentsRemoved:
        {
          return "onEnrollmentsRemoved";
        }
        case TRANSACTION_onAuthenticatorIdRetrieved:
        {
          return "onAuthenticatorIdRetrieved";
        }
        case TRANSACTION_onAuthenticatorIdInvalidated:
        {
          return "onAuthenticatorIdInvalidated";
        }
        case TRANSACTION_onSessionClosed:
        {
          return "onSessionClosed";
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
        case TRANSACTION_onChallengeGenerated:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.onChallengeGenerated(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onChallengeRevoked:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.onChallengeRevoked(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAcquired:
        {
          byte _arg0;
          _arg0 = data.readByte();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onAcquired(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onError:
        {
          byte _arg0;
          _arg0 = data.readByte();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onError(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onEnrollmentProgress:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          data.enforceNoDataAvail();
          this.onEnrollmentProgress(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAuthenticationSucceeded:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.hardware.keymaster.HardwareAuthToken _arg1;
          _arg1 = data.readTypedObject(android.hardware.keymaster.HardwareAuthToken.CREATOR);
          data.enforceNoDataAvail();
          this.onAuthenticationSucceeded(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAuthenticationFailed:
        {
          this.onAuthenticationFailed();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onLockoutTimed:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.onLockoutTimed(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onLockoutPermanent:
        {
          this.onLockoutPermanent();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onLockoutCleared:
        {
          this.onLockoutCleared();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onInteractionDetected:
        {
          this.onInteractionDetected();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onEnrollmentsEnumerated:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.onEnrollmentsEnumerated(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onEnrollmentsRemoved:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.onEnrollmentsRemoved(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAuthenticatorIdRetrieved:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.onAuthenticatorIdRetrieved(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAuthenticatorIdInvalidated:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.onAuthenticatorIdInvalidated(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onSessionClosed:
        {
          this.onSessionClosed();
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
    private static class Proxy implements android.hardware.biometrics.fingerprint.ISessionCallback
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
      @Override public void onChallengeGenerated(long challenge) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(challenge);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onChallengeGenerated, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onChallengeGenerated is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onChallengeRevoked(long challenge) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(challenge);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onChallengeRevoked, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onChallengeRevoked is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onAcquired(byte info, int vendorCode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(info);
          _data.writeInt(vendorCode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAcquired, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onAcquired is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onError(byte error, int vendorCode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(error);
          _data.writeInt(vendorCode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onError, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onError is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onEnrollmentProgress(int enrollmentId, int remaining) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(enrollmentId);
          _data.writeInt(remaining);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEnrollmentProgress, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onEnrollmentProgress is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onAuthenticationSucceeded(int enrollmentId, android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(enrollmentId);
          _data.writeTypedObject(hat, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAuthenticationSucceeded, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onAuthenticationSucceeded is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onAuthenticationFailed() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAuthenticationFailed, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onAuthenticationFailed is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onLockoutTimed(long durationMillis) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(durationMillis);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onLockoutTimed, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onLockoutTimed is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onLockoutPermanent() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onLockoutPermanent, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onLockoutPermanent is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onLockoutCleared() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onLockoutCleared, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onLockoutCleared is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onInteractionDetected() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onInteractionDetected, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onInteractionDetected is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onEnrollmentsEnumerated(int[] enrollmentIds) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(enrollmentIds);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEnrollmentsEnumerated, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onEnrollmentsEnumerated is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onEnrollmentsRemoved(int[] enrollmentIds) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(enrollmentIds);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEnrollmentsRemoved, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onEnrollmentsRemoved is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onAuthenticatorIdRetrieved(long authenticatorId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(authenticatorId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAuthenticatorIdRetrieved, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onAuthenticatorIdRetrieved is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(newAuthenticatorId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAuthenticatorIdInvalidated, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onAuthenticatorIdInvalidated is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onSessionClosed() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSessionClosed, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onSessionClosed is unimplemented.");
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
    static final int TRANSACTION_onChallengeGenerated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onChallengeRevoked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onAcquired = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onEnrollmentProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onAuthenticationSucceeded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onAuthenticationFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onLockoutTimed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_onLockoutPermanent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onLockoutCleared = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_onInteractionDetected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_onEnrollmentsEnumerated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_onEnrollmentsRemoved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onAuthenticatorIdRetrieved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_onAuthenticatorIdInvalidated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_onSessionClosed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$biometrics$fingerprint$ISessionCallback".replace('$', '.');
  public void onChallengeGenerated(long challenge) throws android.os.RemoteException;
  public void onChallengeRevoked(long challenge) throws android.os.RemoteException;
  public void onAcquired(byte info, int vendorCode) throws android.os.RemoteException;
  public void onError(byte error, int vendorCode) throws android.os.RemoteException;
  public void onEnrollmentProgress(int enrollmentId, int remaining) throws android.os.RemoteException;
  public void onAuthenticationSucceeded(int enrollmentId, android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  public void onAuthenticationFailed() throws android.os.RemoteException;
  public void onLockoutTimed(long durationMillis) throws android.os.RemoteException;
  public void onLockoutPermanent() throws android.os.RemoteException;
  public void onLockoutCleared() throws android.os.RemoteException;
  public void onInteractionDetected() throws android.os.RemoteException;
  public void onEnrollmentsEnumerated(int[] enrollmentIds) throws android.os.RemoteException;
  public void onEnrollmentsRemoved(int[] enrollmentIds) throws android.os.RemoteException;
  public void onAuthenticatorIdRetrieved(long authenticatorId) throws android.os.RemoteException;
  public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws android.os.RemoteException;
  public void onSessionClosed() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
