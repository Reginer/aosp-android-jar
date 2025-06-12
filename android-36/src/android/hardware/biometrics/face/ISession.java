/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash c43fbb9be4a662cc9ace640dba21cccdb84c6c21 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen/android/hardware/biometrics/face/ISession.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen -Iframeworks/native/aidl/gui -Nhardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4 hardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4/android/hardware/biometrics/face/ISession.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.face;
/** @hide */
public interface ISession extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 4;
  public static final String HASH = "c43fbb9be4a662cc9ace640dba21cccdb84c6c21";
  /** Default implementation for ISession. */
  public static class Default implements android.hardware.biometrics.face.ISession
  {
    @Override public void generateChallenge() throws android.os.RemoteException
    {
    }
    @Override public void revokeChallenge(long challenge) throws android.os.RemoteException
    {
    }
    @Override public android.hardware.biometrics.face.EnrollmentStageConfig[] getEnrollmentConfig(byte enrollmentType) throws android.os.RemoteException
    {
      return null;
    }
    /** @deprecated use {@link enrollWithOptions} instead. */
    @Override public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat, byte type, byte[] features, android.hardware.common.NativeHandle previewSurface) throws android.os.RemoteException
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
    @Override public void getFeatures() throws android.os.RemoteException
    {
    }
    @Override public void setFeature(android.hardware.keymaster.HardwareAuthToken hat, byte feature, boolean enabled) throws android.os.RemoteException
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
    @Override public android.hardware.biometrics.common.ICancellationSignal authenticateWithContext(long operationId, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    /** @deprecated use {@link enrollWithOptions} instead. */
    @Override public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, byte type, byte[] features, android.hardware.common.NativeHandle previewSurface, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal detectInteractionWithContext(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void onContextChanged(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
    }
    @Override public android.hardware.biometrics.common.ICancellationSignal enrollWithOptions(android.hardware.biometrics.face.FaceEnrollOptions options) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.biometrics.face.ISession
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.biometrics.face.ISession interface,
     * generating a proxy if needed.
     */
    public static android.hardware.biometrics.face.ISession asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.biometrics.face.ISession))) {
        return ((android.hardware.biometrics.face.ISession)iin);
      }
      return new android.hardware.biometrics.face.ISession.Stub.Proxy(obj);
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
        case TRANSACTION_getEnrollmentConfig:
        {
          return "getEnrollmentConfig";
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
        case TRANSACTION_getFeatures:
        {
          return "getFeatures";
        }
        case TRANSACTION_setFeature:
        {
          return "setFeature";
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
        case TRANSACTION_onContextChanged:
        {
          return "onContextChanged";
        }
        case TRANSACTION_enrollWithOptions:
        {
          return "enrollWithOptions";
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
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      else if (code == TRANSACTION_getInterfaceVersion) {
        reply.writeNoException();
        reply.writeInt(getInterfaceVersion());
        return true;
      }
      else if (code == TRANSACTION_getInterfaceHash) {
        reply.writeNoException();
        reply.writeString(getInterfaceHash());
        return true;
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
        case TRANSACTION_getEnrollmentConfig:
        {
          byte _arg0;
          _arg0 = data.readByte();
          data.enforceNoDataAvail();
          android.hardware.biometrics.face.EnrollmentStageConfig[] _result = this.getEnrollmentConfig(_arg0);
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_enroll:
        {
          android.hardware.keymaster.HardwareAuthToken _arg0;
          _arg0 = data.readTypedObject(android.hardware.keymaster.HardwareAuthToken.CREATOR);
          byte _arg1;
          _arg1 = data.readByte();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          android.hardware.common.NativeHandle _arg3;
          _arg3 = data.readTypedObject(android.hardware.common.NativeHandle.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.enroll(_arg0, _arg1, _arg2, _arg3);
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
        case TRANSACTION_getFeatures:
        {
          this.getFeatures();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setFeature:
        {
          android.hardware.keymaster.HardwareAuthToken _arg0;
          _arg0 = data.readTypedObject(android.hardware.keymaster.HardwareAuthToken.CREATOR);
          byte _arg1;
          _arg1 = data.readByte();
          boolean _arg2;
          _arg2 = data.readBoolean();
          data.enforceNoDataAvail();
          this.setFeature(_arg0, _arg1, _arg2);
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
          byte _arg1;
          _arg1 = data.readByte();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          android.hardware.common.NativeHandle _arg3;
          _arg3 = data.readTypedObject(android.hardware.common.NativeHandle.CREATOR);
          android.hardware.biometrics.common.OperationContext _arg4;
          _arg4 = data.readTypedObject(android.hardware.biometrics.common.OperationContext.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.enrollWithContext(_arg0, _arg1, _arg2, _arg3, _arg4);
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
        case TRANSACTION_onContextChanged:
        {
          android.hardware.biometrics.common.OperationContext _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.common.OperationContext.CREATOR);
          data.enforceNoDataAvail();
          this.onContextChanged(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_enrollWithOptions:
        {
          android.hardware.biometrics.face.FaceEnrollOptions _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.face.FaceEnrollOptions.CREATOR);
          data.enforceNoDataAvail();
          android.hardware.biometrics.common.ICancellationSignal _result = this.enrollWithOptions(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.hardware.biometrics.face.ISession
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
      @Override public android.hardware.biometrics.face.EnrollmentStageConfig[] getEnrollmentConfig(byte enrollmentType) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.face.EnrollmentStageConfig[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(enrollmentType);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getEnrollmentConfig, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getEnrollmentConfig is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.biometrics.face.EnrollmentStageConfig.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** @deprecated use {@link enrollWithOptions} instead. */
      @Override public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat, byte type, byte[] features, android.hardware.common.NativeHandle previewSurface) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(hat, 0);
          _data.writeByte(type);
          _data.writeByteArray(features);
          _data.writeTypedObject(previewSurface, 0);
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
      @Override public void getFeatures() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getFeatures, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getFeatures is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setFeature(android.hardware.keymaster.HardwareAuthToken hat, byte feature, boolean enabled) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(hat, 0);
          _data.writeByte(feature);
          _data.writeBoolean(enabled);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setFeature, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setFeature is unimplemented.");
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
      /** @deprecated use {@link enrollWithOptions} instead. */
      @Override public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, byte type, byte[] features, android.hardware.common.NativeHandle previewSurface, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(hat, 0);
          _data.writeByte(type);
          _data.writeByteArray(features);
          _data.writeTypedObject(previewSurface, 0);
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
      @Override public android.hardware.biometrics.common.ICancellationSignal enrollWithOptions(android.hardware.biometrics.face.FaceEnrollOptions options) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.common.ICancellationSignal _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(options, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enrollWithOptions, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method enrollWithOptions is unimplemented.");
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
    static final int TRANSACTION_getEnrollmentConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_enroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_authenticate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_detectInteraction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_enumerateEnrollments = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_removeEnrollments = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getFeatures = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setFeature = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getAuthenticatorId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_invalidateAuthenticatorId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_resetLockout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_authenticateWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_enrollWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_detectInteractionWithContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_onContextChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_enrollWithOptions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$biometrics$face$ISession".replace('$', '.');
  public void generateChallenge() throws android.os.RemoteException;
  public void revokeChallenge(long challenge) throws android.os.RemoteException;
  public android.hardware.biometrics.face.EnrollmentStageConfig[] getEnrollmentConfig(byte enrollmentType) throws android.os.RemoteException;
  /** @deprecated use {@link enrollWithOptions} instead. */
  @Deprecated
  public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat, byte type, byte[] features, android.hardware.common.NativeHandle previewSurface) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal authenticate(long operationId) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal detectInteraction() throws android.os.RemoteException;
  public void enumerateEnrollments() throws android.os.RemoteException;
  public void removeEnrollments(int[] enrollmentIds) throws android.os.RemoteException;
  public void getFeatures() throws android.os.RemoteException;
  public void setFeature(android.hardware.keymaster.HardwareAuthToken hat, byte feature, boolean enabled) throws android.os.RemoteException;
  public void getAuthenticatorId() throws android.os.RemoteException;
  public void invalidateAuthenticatorId() throws android.os.RemoteException;
  public void resetLockout(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal authenticateWithContext(long operationId, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  /** @deprecated use {@link enrollWithOptions} instead. */
  @Deprecated
  public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, byte type, byte[] features, android.hardware.common.NativeHandle previewSurface, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal detectInteractionWithContext(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  public void onContextChanged(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  public android.hardware.biometrics.common.ICancellationSignal enrollWithOptions(android.hardware.biometrics.face.FaceEnrollOptions options) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
