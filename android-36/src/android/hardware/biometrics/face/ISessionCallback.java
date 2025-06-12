/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 4 --hash c43fbb9be4a662cc9ace640dba21cccdb84c6c21 -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/common/aidl/android.hardware.common_interface/2/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen/android/hardware/biometrics/face/ISessionCallback.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/face/aidl/android.hardware.biometrics.face-V4-java-source/gen -Iframeworks/native/aidl/gui -Nhardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4 hardware/interfaces/biometrics/face/aidl/aidl_api/android.hardware.biometrics.face/4/android/hardware/biometrics/face/ISessionCallback.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.face;
/** @hide */
public interface ISessionCallback extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 4;
  public static final String HASH = "c43fbb9be4a662cc9ace640dba21cccdb84c6c21";
  /** Default implementation for ISessionCallback. */
  public static class Default implements android.hardware.biometrics.face.ISessionCallback
  {
    @Override public void onChallengeGenerated(long challenge) throws android.os.RemoteException
    {
    }
    @Override public void onChallengeRevoked(long challenge) throws android.os.RemoteException
    {
    }
    @Override public void onAuthenticationFrame(android.hardware.biometrics.face.AuthenticationFrame frame) throws android.os.RemoteException
    {
    }
    @Override public void onEnrollmentFrame(android.hardware.biometrics.face.EnrollmentFrame frame) throws android.os.RemoteException
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
    @Override public void onFeaturesRetrieved(byte[] features) throws android.os.RemoteException
    {
    }
    @Override public void onFeatureSet(byte feature) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.biometrics.face.ISessionCallback
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.biometrics.face.ISessionCallback interface,
     * generating a proxy if needed.
     */
    public static android.hardware.biometrics.face.ISessionCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.biometrics.face.ISessionCallback))) {
        return ((android.hardware.biometrics.face.ISessionCallback)iin);
      }
      return new android.hardware.biometrics.face.ISessionCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onAuthenticationFrame:
        {
          return "onAuthenticationFrame";
        }
        case TRANSACTION_onEnrollmentFrame:
        {
          return "onEnrollmentFrame";
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
        case TRANSACTION_onFeaturesRetrieved:
        {
          return "onFeaturesRetrieved";
        }
        case TRANSACTION_onFeatureSet:
        {
          return "onFeatureSet";
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
        case TRANSACTION_onAuthenticationFrame:
        {
          android.hardware.biometrics.face.AuthenticationFrame _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.face.AuthenticationFrame.CREATOR);
          data.enforceNoDataAvail();
          this.onAuthenticationFrame(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onEnrollmentFrame:
        {
          android.hardware.biometrics.face.EnrollmentFrame _arg0;
          _arg0 = data.readTypedObject(android.hardware.biometrics.face.EnrollmentFrame.CREATOR);
          data.enforceNoDataAvail();
          this.onEnrollmentFrame(_arg0);
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
        case TRANSACTION_onFeaturesRetrieved:
        {
          byte[] _arg0;
          _arg0 = data.createByteArray();
          data.enforceNoDataAvail();
          this.onFeaturesRetrieved(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onFeatureSet:
        {
          byte _arg0;
          _arg0 = data.readByte();
          data.enforceNoDataAvail();
          this.onFeatureSet(_arg0);
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
    private static class Proxy implements android.hardware.biometrics.face.ISessionCallback
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
      @Override public void onAuthenticationFrame(android.hardware.biometrics.face.AuthenticationFrame frame) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(frame, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAuthenticationFrame, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onAuthenticationFrame is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onEnrollmentFrame(android.hardware.biometrics.face.EnrollmentFrame frame) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedObject(frame, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onEnrollmentFrame, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onEnrollmentFrame is unimplemented.");
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
      @Override public void onFeaturesRetrieved(byte[] features) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByteArray(features);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onFeaturesRetrieved, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onFeaturesRetrieved is unimplemented.");
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onFeatureSet(byte feature) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(feature);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onFeatureSet, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method onFeatureSet is unimplemented.");
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
    static final int TRANSACTION_onAuthenticationFrame = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onEnrollmentFrame = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onEnrollmentProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onAuthenticationSucceeded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onAuthenticationFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_onLockoutTimed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onLockoutPermanent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_onLockoutCleared = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_onInteractionDetected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_onEnrollmentsEnumerated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onFeaturesRetrieved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_onFeatureSet = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_onEnrollmentsRemoved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_onAuthenticatorIdRetrieved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_onAuthenticatorIdInvalidated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_onSessionClosed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$biometrics$face$ISessionCallback".replace('$', '.');
  public void onChallengeGenerated(long challenge) throws android.os.RemoteException;
  public void onChallengeRevoked(long challenge) throws android.os.RemoteException;
  public void onAuthenticationFrame(android.hardware.biometrics.face.AuthenticationFrame frame) throws android.os.RemoteException;
  public void onEnrollmentFrame(android.hardware.biometrics.face.EnrollmentFrame frame) throws android.os.RemoteException;
  public void onError(byte error, int vendorCode) throws android.os.RemoteException;
  public void onEnrollmentProgress(int enrollmentId, int remaining) throws android.os.RemoteException;
  public void onAuthenticationSucceeded(int enrollmentId, android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  public void onAuthenticationFailed() throws android.os.RemoteException;
  public void onLockoutTimed(long durationMillis) throws android.os.RemoteException;
  public void onLockoutPermanent() throws android.os.RemoteException;
  public void onLockoutCleared() throws android.os.RemoteException;
  public void onInteractionDetected() throws android.os.RemoteException;
  public void onEnrollmentsEnumerated(int[] enrollmentIds) throws android.os.RemoteException;
  public void onFeaturesRetrieved(byte[] features) throws android.os.RemoteException;
  public void onFeatureSet(byte feature) throws android.os.RemoteException;
  public void onEnrollmentsRemoved(int[] enrollmentIds) throws android.os.RemoteException;
  public void onAuthenticatorIdRetrieved(long authenticatorId) throws android.os.RemoteException;
  public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws android.os.RemoteException;
  public void onSessionClosed() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
