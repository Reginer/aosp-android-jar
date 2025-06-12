/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/IFingerprint.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/IFingerprint.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint;
/** @hide */
public interface IFingerprint extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = true ? 4 : 5;
  // Interface is being downgraded to the last frozen version due to
  // RELEASE_AIDL_USE_UNFROZEN. See
  // https://source.android.com/docs/core/architecture/aidl/stable-aidl#flag-based-development
  public static final String HASH = "41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb";
  /** Default implementation for IFingerprint. */
  public static class Default implements android.hardware.biometrics.fingerprint.IFingerprint
  {
    /**
     * getSensorProps:
     * 
     * @return A list of properties for all of the fingerprint sensors supported by the HAL.
     */
    @Override public android.hardware.biometrics.fingerprint.SensorProps[] getSensorProps() throws android.os.RemoteException
    {
      return null;
    }
    /**
     * createSession:
     * 
     * Creates an instance of ISession that can be used by the framework to perform operations such
     * as ISession#enroll, ISession#authenticate, etc. for the given sensorId and userId.
     * 
     * Calling this method while there is an active session is considered an error. If the framework
     * wants to create a new session when it already has an active session, it must first cancel the
     * current operation if it's cancellable or wait until it completes. Then, the framework must
     * explicitly close the session with ISession#close. Once the framework receives
     * ISessionCallback#onSessionClosed, a new session can be created.
     * 
     * Implementations must store user-specific state or metadata in /data/vendor_de/<user>/fpdata
     * as specified by the SELinux policy. The directory /data/vendor_de is managed by vold (see
     * vold_prepare_subdirs.cpp). Implementations may store additional user-specific data, such as
     * embeddings or templates, in StrongBox.
     * 
     * @param sensorId The sensorId for which this session is being created.
     * @param userId The userId for which this session is being created.
     * @param cb A callback to notify the framework about the session's events.
     * @return A new session.
     */
    @Override public android.hardware.biometrics.fingerprint.ISession createSession(int sensorId, int userId, android.hardware.biometrics.fingerprint.ISessionCallback cb) throws android.os.RemoteException
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
  public static abstract class Stub extends android.os.Binder implements android.hardware.biometrics.fingerprint.IFingerprint
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.hardware.biometrics.fingerprint.IFingerprint interface,
     * generating a proxy if needed.
     */
    public static android.hardware.biometrics.fingerprint.IFingerprint asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.hardware.biometrics.fingerprint.IFingerprint))) {
        return ((android.hardware.biometrics.fingerprint.IFingerprint)iin);
      }
      return new android.hardware.biometrics.fingerprint.IFingerprint.Stub.Proxy(obj);
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
        case TRANSACTION_getSensorProps:
        {
          return "getSensorProps";
        }
        case TRANSACTION_createSession:
        {
          return "createSession";
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
        case TRANSACTION_getSensorProps:
        {
          android.hardware.biometrics.fingerprint.SensorProps[] _result = this.getSensorProps();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_createSession:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          android.hardware.biometrics.fingerprint.ISessionCallback _arg2;
          _arg2 = android.hardware.biometrics.fingerprint.ISessionCallback.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          android.hardware.biometrics.fingerprint.ISession _result = this.createSession(_arg0, _arg1, _arg2);
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
    private static class Proxy implements android.hardware.biometrics.fingerprint.IFingerprint
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
      /**
       * getSensorProps:
       * 
       * @return A list of properties for all of the fingerprint sensors supported by the HAL.
       */
      @Override public android.hardware.biometrics.fingerprint.SensorProps[] getSensorProps() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.fingerprint.SensorProps[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSensorProps, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getSensorProps is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createTypedArray(android.hardware.biometrics.fingerprint.SensorProps.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * createSession:
       * 
       * Creates an instance of ISession that can be used by the framework to perform operations such
       * as ISession#enroll, ISession#authenticate, etc. for the given sensorId and userId.
       * 
       * Calling this method while there is an active session is considered an error. If the framework
       * wants to create a new session when it already has an active session, it must first cancel the
       * current operation if it's cancellable or wait until it completes. Then, the framework must
       * explicitly close the session with ISession#close. Once the framework receives
       * ISessionCallback#onSessionClosed, a new session can be created.
       * 
       * Implementations must store user-specific state or metadata in /data/vendor_de/<user>/fpdata
       * as specified by the SELinux policy. The directory /data/vendor_de is managed by vold (see
       * vold_prepare_subdirs.cpp). Implementations may store additional user-specific data, such as
       * embeddings or templates, in StrongBox.
       * 
       * @param sensorId The sensorId for which this session is being created.
       * @param userId The userId for which this session is being created.
       * @param cb A callback to notify the framework about the session's events.
       * @return A new session.
       */
      @Override public android.hardware.biometrics.fingerprint.ISession createSession(int sensorId, int userId, android.hardware.biometrics.fingerprint.ISessionCallback cb) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.hardware.biometrics.fingerprint.ISession _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(sensorId);
          _data.writeInt(userId);
          _data.writeStrongInterface(cb);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createSession, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method createSession is unimplemented.");
          }
          _reply.readException();
          _result = android.hardware.biometrics.fingerprint.ISession.Stub.asInterface(_reply.readStrongBinder());
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
    static final int TRANSACTION_getSensorProps = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_createSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$biometrics$fingerprint$IFingerprint".replace('$', '.');
  /**
   * getSensorProps:
   * 
   * @return A list of properties for all of the fingerprint sensors supported by the HAL.
   */
  public android.hardware.biometrics.fingerprint.SensorProps[] getSensorProps() throws android.os.RemoteException;
  /**
   * createSession:
   * 
   * Creates an instance of ISession that can be used by the framework to perform operations such
   * as ISession#enroll, ISession#authenticate, etc. for the given sensorId and userId.
   * 
   * Calling this method while there is an active session is considered an error. If the framework
   * wants to create a new session when it already has an active session, it must first cancel the
   * current operation if it's cancellable or wait until it completes. Then, the framework must
   * explicitly close the session with ISession#close. Once the framework receives
   * ISessionCallback#onSessionClosed, a new session can be created.
   * 
   * Implementations must store user-specific state or metadata in /data/vendor_de/<user>/fpdata
   * as specified by the SELinux policy. The directory /data/vendor_de is managed by vold (see
   * vold_prepare_subdirs.cpp). Implementations may store additional user-specific data, such as
   * embeddings or templates, in StrongBox.
   * 
   * @param sensorId The sensorId for which this session is being created.
   * @param userId The userId for which this session is being created.
   * @param cb A callback to notify the framework about the session's events.
   * @return A new session.
   */
  public android.hardware.biometrics.fingerprint.ISession createSession(int sensorId, int userId, android.hardware.biometrics.fingerprint.ISessionCallback cb) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
