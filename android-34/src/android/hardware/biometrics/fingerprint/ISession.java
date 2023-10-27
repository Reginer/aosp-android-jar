/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public interface ISession extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 3;
  public static final String HASH = "637371b53fb7faf9bd43aa51b72c23852d6e6d96";
  /** Default implementation for ISession. */
  public static class Default implements android.hardware.biometrics.fingerprint.ISession
  {
    @Override public void generateChallenge() throws android.os.RemoteException
    {
    }
    @Override public void revokeChallenge(long challenge) throws android.os.RemoteException
    {
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal authenticate(long operationId) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal detectInteraction() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void enumerateEnrollments() throws android.os.RemoteException
    {
    }
    @Override public void removeEnrollments(int[] enrollmentIds) throws android.os.RemoteException
    {
    }
    @Override public void getAuthenticatorId() throws android.os.RemoteException
    {
    }
    @Override public void invalidateAuthenticatorId() throws android.os.RemoteException
    {
    }
    @Override public void resetLockout(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
    {
    }
    @Override public void close() throws android.os.RemoteException
    {
    }
    /** @deprecated use onPointerDownWithContext instead. */
    @Override public void onPointerDown(int pointerId, int x, int y, float minor, float major) throws android.os.RemoteException
    {
    }
    /** @deprecated use onPointerUpWithContext instead. */
    @Override public void onPointerUp(int pointerId) throws android.os.RemoteException
    {
    }
    @Override public void onUiReady() throws android.os.RemoteException
    {
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal authenticateWithContext(long operationId, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal detectInteractionWithContext(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void onPointerDownWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
    {
    }
    @Override public void onPointerUpWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
    {
    }
    @Override public void onContextChanged(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
    }
    @Override public void onPointerCancelWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
    {
    }
    @Override public void setIgnoreDisplayTouches(boolean shouldIgnore) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.biometrics.fingerprint.ISession
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.biometrics.fingerprint.ISession interface,
     * generating a proxy if needed.
     */
    public static android.hardware.biometrics.fingerprint.ISession asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.biometrics.fingerprint.ISession))) {
        return ((android.hardware.biometrics.fingerprint.ISession)iin);
      }
      return new android.hardware.biometrics.fingerprint.ISession.Stub.Proxy(obj);
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
        case TRANSACTION_generateChallenge:
        {
          return "generateChallenge";
        }
        case TRANSACTION_revokeChallenge:
        {
          return "revokeChallenge";
        }
        case TRANSACTION_enroll:
        {
          return "enroll";
        }
        case TRANSACTION_authenticate:
        {
          return "authenticate";
        }
        case TRANSACTION_detectInteraction:
        {
          return "detectInteraction";
        }
        case TRANSACTION_enumerateEnrollments:
        {
          return "enumerateEnrollments";
        }
        case TRANSACTION_removeEnrollments:
        {
          return "removeEnrollments";
        }
        case TRANSACTION_getAuthenticatorId:
        {
          return "getAuthenticatorId";
        }
        case TRANSACTION_invalidateAuthenticatorId:
        {
          return "invalidateAuthenticatorId";
        }
        case TRANSACTION_resetLockout:
        {
          return "resetLockout";
        }
        case TRANSACTION_close:
        {
          return "close";
        }
        case TRANSACTION_onPointerDown:
        {
          return "onPointerDown";
        }
        case TRANSACTION_onPointerUp:
        {
          return "onPointerUp";
        }
        case TRANSACTION_onUiReady:
        {
          return "onUiReady";
        }
        case TRANSACTION_authenticateWithContext:
        {
          return "authenticateWithContext";
        }
        case TRANSACTION_enrollWithContext:
        {
          return "enrollWithContext";
        }
        case TRANSACTION_detectInteractionWithContext:
        {
          return "detectInteractionWithContext";
        }
        case TRANSACTION_onPointerDownWithContext:
        {
          return "onPointerDownWithContext";
        }
        case TRANSACTION_onPointerUpWithContext:
        {
          return "onPointerUpWithContext";
        }
        case TRANSACTION_onContextChanged:
        {
          return "onContextChanged";
        }
        case TRANSACTION_onPointerCancelWithContext:
        {
          return "onPointerCancelWithContext";
        }
        case TRANSACTION_setIgnoreDisplayTouches:
        {
          return "setIgnoreDisplayTouches";
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
        case TRANSACTION_generateChallenge:
        {
          this.generateChallenge();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_revokeChallenge:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          this.revokeChallenge(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_enroll:
        {
          android.hardware.keymaster.HardwareAuthToken _arg0;
          _arg0 = data.readTypedObject(android.hardware.keymaster.HardwareAuthToken.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.enroll(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_authenticate:
        {
          long _arg0;
          _arg0 = data.readLong();
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.authenticate(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_detectInteraction:
        {
          android.hardware.biometrics.common.ICancellationSignal _result = this.detectInteraction();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_enumerateEnrollments:
        {
          this.enumerateEnrollments();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_removeEnrollments:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          this.removeEnrollments(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getAuthenticatorId:
        {
          this.getAuthenticatorId();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_invalidateAuthenticatorId:
        {
          this.invalidateAuthenticatorId();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_resetLockout:
        {
          android.hardware.keymaster.HardwareAuthToken _arg0;
          _arg0 = data.readTypedObject(android.hardware.keymaster.HardwareAuthToken.CREATOR);
          data.enforceNoDataAvail();
          this.resetLockout(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_close:
        {
          this.close();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onPointerDown:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          float _arg3;
          _arg3 = data.readFloat();
          float _arg4;
          _arg4 = data.readFloat();
          data.enforceNoDataAvail();
          this.onPointerDown(_arg0, _arg1, _arg2, _arg3, _arg4);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onPointerUp:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          this.onPointerUp(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onUiReady:
        {
          this.onUiReady();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_authenticateWithContext:
        {
          long _arg0;
          _arg0 = data.readLong();
          android.hardware.biometrics.common.OperationContext _arg1;
          _arg1 = data.readTypedObject(android.hardware.biometrics.common.OperationContext.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.authenticateWithContext(_arg0, _arg1);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_enrollWithContext:
        {
          android.hardware.keymaster.HardwareAuthToken _arg0;
          _arg0 = data.readTypedObject(android.hardware.keymaster.HardwareAuthToken.CREATOR);
          android.hardware.biometrics.common.OperationContext _arg1;
          _arg1 = data.readTypedObject(android.hardware.biometrics.common.OperationContext.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.enrollWithContext(_arg0, _arg1);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_detectInteractionWithContext:
        {
          android.hardware.biometrics.common.OperationContext _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.common.OperationContext.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.detectInteractionWithContext(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_onPointerDownWithContext:
        {
          android.hardware.biometrics.fingerprint.PointerContext _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.fingerprint.PointerContext.CREATOR);
          data.enforceNoDataAvail();
          this.onPointerDownWithContext(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onPointerUpWithContext:
        {
          android.hardware.biometrics.fingerprint.PointerContext _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.fingerprint.PointerContext.CREATOR);
          data.enforceNoDataAvail();
          this.onPointerUpWithContext(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onContextChanged:
        {
          android.hardware.biometrics.common.OperationContext _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.common.OperationContext.CREATOR);
          data.enforceNoDataAvail();
          this.onContextChanged(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onPointerCancelWithContext:
        {
          android.hardware.biometrics.fingerprint.PointerContext _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.fingerprint.PointerContext.CREATOR);
          data.enforceNoDataAvail();
          this.onPointerCancelWithContext(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setIgnoreDisplayTouches:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setIgnoreDisplayTouches(_arg0);
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
    private static class Proxy implements android.hardware.biometrics.fingerprint.ISession
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
      @Override public void generateChallenge() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_generateChallenge, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method generateChallenge is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void revokeChallenge(long challenge) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(challenge);
          boolean _status = mRemote.transact(Stub.TRANSACTION_revokeChallenge, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method revokeChallenge is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(hat, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enroll, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method enroll is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.biometrics.common.ICancellationSignal.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.biometrics.common.ICancellationSignal authenticate(long operationId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(operationId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_authenticate, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method authenticate is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.biometrics.common.ICancellationSignal.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.biometrics.common.ICancellationSignal detectInteraction() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_detectInteraction, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method detectInteraction is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.biometrics.common.ICancellationSignal.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void enumerateEnrollments() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enumerateEnrollments, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method enumerateEnrollments is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeEnrollments(int[] enrollmentIds) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(enrollmentIds);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeEnrollments, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method removeEnrollments is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void getAuthenticatorId() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAuthenticatorId, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getAuthenticatorId is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void invalidateAuthenticatorId() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_invalidateAuthenticatorId, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method invalidateAuthenticatorId is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void resetLockout(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(hat, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_resetLockout, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method resetLockout is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void close() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_close, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method close is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** @deprecated use onPointerDownWithContext instead. */
      @Override public void onPointerDown(int pointerId, int x, int y, float minor, float major) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(pointerId);
          _data.writeInt(x);
          _data.writeInt(y);
          _data.writeFloat(minor);
          _data.writeFloat(major);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPointerDown, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onPointerDown is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** @deprecated use onPointerUpWithContext instead. */
      @Override public void onPointerUp(int pointerId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(pointerId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPointerUp, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onPointerUp is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onUiReady() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onUiReady, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onUiReady is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.hardware.biometrics.common.ICancellationSignal authenticateWithContext(long operationId, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(operationId);
          _data.writeTypedObject(context, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_authenticateWithContext, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method authenticateWithContext is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.biometrics.common.ICancellationSignal.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(hat, 0);
          _data.writeTypedObject(context, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enrollWithContext, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method enrollWithContext is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.biometrics.common.ICancellationSignal.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.hardware.biometrics.common.ICancellationSignal detectInteractionWithContext(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(context, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_detectInteractionWithContext, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method detectInteractionWithContext is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.biometrics.common.ICancellationSignal.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void onPointerDownWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(context, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPointerDownWithContext, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onPointerDownWithContext is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onPointerUpWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(context, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPointerUpWithContext, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onPointerUpWithContext is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onContextChanged(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(context, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onContextChanged, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onContextChanged is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onPointerCancelWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(context, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPointerCancelWithContext, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onPointerCancelWithContext is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setIgnoreDisplayTouches(boolean shouldIgnore) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(shouldIgnore);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setIgnoreDisplayTouches, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setIgnoreDisplayTouches is unimplemented.");
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
    static final int TRANSACTION_generateChallenge = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_revokeChallenge = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_enroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_authenticate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_detectInteraction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_enumerateEnrollments = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_removeEnrollments = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getAuthenticatorId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_invalidateAuthenticatorId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_resetLockout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_onPointerDown = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_onPointerUp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onUiReady = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_authenticateWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_enrollWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_detectInteractionWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_onPointerDownWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_onPointerUpWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_onContextChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_onPointerCancelWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_setIgnoreDisplayTouches = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "android$hardware$biometrics$fingerprint$ISession".replace('$', '.');
  public void generateChallenge() throws android.os.RemoteException;
  public void revokeChallenge(long challenge) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal authenticate(long operationId) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal detectInteraction() throws android.os.RemoteException;
  public void enumerateEnrollments() throws android.os.RemoteException;
  public void removeEnrollments(int[] enrollmentIds) throws android.os.RemoteException;
  public void getAuthenticatorId() throws android.os.RemoteException;
  public void invalidateAuthenticatorId() throws android.os.RemoteException;
  public void resetLockout(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  /** @deprecated use onPointerDownWithContext instead. */
  @Deprecated
  public void onPointerDown(int pointerId, int x, int y, float minor, float major) throws android.os.RemoteException;
  /** @deprecated use onPointerUpWithContext instead. */
  @Deprecated
  public void onPointerUp(int pointerId) throws android.os.RemoteException;
  public void onUiReady() throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal authenticateWithContext(long operationId, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal detectInteractionWithContext(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  public void onPointerDownWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException;
  public void onPointerUpWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException;
  public void onContextChanged(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  public void onPointerCancelWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException;
  public void setIgnoreDisplayTouches(boolean shouldIgnore) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
