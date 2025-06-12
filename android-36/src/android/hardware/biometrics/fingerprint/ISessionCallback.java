/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/ISessionCallback.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/ISessionCallback.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
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
  public static final int VERSION = true ? 4 : 5;
  // Interface is being downgraded to the last frozen version due to
  // RELEASE_AIDL_USE_UNFROZEN. See
  // https://source.android.com/docs/core/architecture/aidl/stable-aidl#flag-based-development
  public static final String HASH = "41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb";
  /** Default implementation for ISessionCallback. */
  public static class Default implements android.hardware.biometrics.fingerprint.ISessionCallback
  {
    /** Notifies the framework when a challenge is successfully generated. */
    @Override public void onChallengeGenerated(long challenge) throws android.os.RemoteException
    {
    }
    /** Notifies the framework when a challenge has been revoked. */
    @Override public void onChallengeRevoked(long challenge) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during the following operations:
     *   - ISession#enroll
     *   - ISession#authenticate
     *   - ISession#detectInteraction
     * 
     * These messages may be used to provide user guidance multiple times per operation if
     * necessary.
     * 
     * @param info See the AcquiredInfo enum.
     * @param vendorCode Only valid if info == AcquiredInfo::VENDOR. The vendorCode must be used to
     *                   index into the configuration
     *                   com.android.internal.R.array.fingerprint_acquired_vendor that's installed
     *                   on the vendor partition.
     */
    @Override public void onAcquired(byte info, int vendorCode) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during the following operations:
     *   - ISession#enroll
     *   - ISession#authenticate
     *   - ISession#detectInteraction
     *   - ISession#invalidateAuthenticatorId
     *   - ISession#resetLockout
     * 
     * These messages may be used to notify the framework or user that a non-recoverable error
     * has occurred. The operation is finished, and the HAL can proceed with the next operation
     * or return to the idling state.
     * 
     * Note that cancellation (see common::ICancellationSignal) must be followed with an
     * Error::CANCELED message.
     * 
     * @param error See the Error enum.
     * @param vendorCode Only valid if error == Error::VENDOR. The vendorCode must be used to index
     *                   into the configuration
     *                   com.android.internal.R.fingerprint_error_vendor that's installed on the
     *                   vendor partition.
     */
    @Override public void onError(byte error, int vendorCode) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during the ISession#enroll operation.
     * 
     * @param enrollmentId Unique stable identifier for the enrollment that's being added by this
     *                     ISession#enroll invocation.
     * @param remaining Remaining number of steps before enrollment is complete.
     */
    @Override public void onEnrollmentProgress(int enrollmentId, int remaining) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during ISession#authenticate.
     * 
     * Used to notify the framework upon successful authentication. Note that the authentication
     * lifecycle ends when either 1) a fingerprint is accepted, or 2) an error occurred. The
     * authentication lifecycle does NOT end when a fingerprint is rejected.
     * 
     * @param enrollmentId Fingerprint that was accepted.
     * @param hat If the sensor is configured as SensorStrength::STRONG, a non-null attestation that
     *            a fingerprint was accepted. The HardwareAuthToken's "challenge" field must be set
     *            with the operationId passed in during ISession#authenticate. If the sensor is NOT
     *            SensorStrength::STRONG, the HardwareAuthToken MUST be null.
     */
    @Override public void onAuthenticationSucceeded(int enrollmentId, android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during ISession#authenticate.
     * 
     * Used to notify the framework upon rejected attempts. Note that the authentication
     * lifecycle ends when either 1) a fingerprint is accepted, or 2) an error occurred.
     * The authentication lifecycle does NOT end when a fingerprint is rejected.
     */
    @Override public void onAuthenticationFailed() throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during ISession#authenticate.
     * 
     * Authentication is locked out due to too many unsuccessful attempts. This is a rate-limiting
     * lockout, and authentication can be restarted after a period of time. See
     * ISession#resetLockout.
     * 
     * @param sensorId Sensor for which the user is locked out.
     * @param userId User for which the sensor is locked out.
     * @param durationMillis Remaining duration of the lockout.
     */
    @Override public void onLockoutTimed(long durationMillis) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during ISession#authenticate.
     * 
     * Authentication is disabled until the user unlocks with their device credential
     * (PIN/Pattern/Password). See ISession#resetLockout.
     * 
     * @param sensorId Sensor for which the user is locked out.
     * @param userId User for which the sensor is locked out.
     */
    @Override public void onLockoutPermanent() throws android.os.RemoteException
    {
    }
    /**
     * Notifies the framework that lockout has been cleared for this (sensorId, userId) pair.
     * 
     * Note that this method can be used to notify the framework during any state.
     * 
     * Lockout can be cleared in the following scenarios:
     * 1) A timed lockout has ended (e.g. durationMillis specified in previous #onLockoutTimed
     *    has expired.
     * 2) See ISession#resetLockout.
     * 
     * @param sensorId Sensor for which the user's lockout is cleared.
     * @param userId User for the sensor's lockout is cleared.
     */
    @Override public void onLockoutCleared() throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during
     * ISession#detectInteraction
     * 
     * Notifies the framework that user interaction occurred. See ISession#detectInteraction.
     */
    @Override public void onInteractionDetected() throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during
     * ISession#enumerateEnrollments.
     * 
     * Notifies the framework of the current enrollments. See ISession#enumerateEnrollments.
     * 
     * @param enrollmentIds A list of enrollments for the session's (userId, sensorId) pair.
     */
    @Override public void onEnrollmentsEnumerated(int[] enrollmentIds) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during
     * ISession#removeEnrollments.
     * 
     * Notifies the framework that the specified enrollments are removed.
     * 
     * @param enrollmentIds The enrollments that were removed.
     */
    @Override public void onEnrollmentsRemoved(int[] enrollmentIds) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during
     * ISession#getAuthenticatorId.
     * 
     * Notifies the framework with the authenticatorId corresponding to this session's
     * (userId, sensorId) pair.
     * 
     * @param authenticatorId See the above documentation.
     */
    @Override public void onAuthenticatorIdRetrieved(long authenticatorId) throws android.os.RemoteException
    {
    }
    /**
     * This method must only be used to notify the framework during
     * ISession#invalidateAuthenticatorId.
     * 
     * See ISession#invalidateAuthenticatorId for more information.
     * 
     * @param newAuthenticatorId The new entropy-encoded random identifier associated with the
     *                           current set of enrollments.
     */
    @Override public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws android.os.RemoteException
    {
    }
    /**
     * This method notifes the client that this session has closed.
     * The client must not make any more calls to this session.
     */
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
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
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
      /** Notifies the framework when a challenge is successfully generated. */
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
      /** Notifies the framework when a challenge has been revoked. */
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
      /**
       * This method must only be used to notify the framework during the following operations:
       *   - ISession#enroll
       *   - ISession#authenticate
       *   - ISession#detectInteraction
       * 
       * These messages may be used to provide user guidance multiple times per operation if
       * necessary.
       * 
       * @param info See the AcquiredInfo enum.
       * @param vendorCode Only valid if info == AcquiredInfo::VENDOR. The vendorCode must be used to
       *                   index into the configuration
       *                   com.android.internal.R.array.fingerprint_acquired_vendor that's installed
       *                   on the vendor partition.
       */
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
      /**
       * This method must only be used to notify the framework during the following operations:
       *   - ISession#enroll
       *   - ISession#authenticate
       *   - ISession#detectInteraction
       *   - ISession#invalidateAuthenticatorId
       *   - ISession#resetLockout
       * 
       * These messages may be used to notify the framework or user that a non-recoverable error
       * has occurred. The operation is finished, and the HAL can proceed with the next operation
       * or return to the idling state.
       * 
       * Note that cancellation (see common::ICancellationSignal) must be followed with an
       * Error::CANCELED message.
       * 
       * @param error See the Error enum.
       * @param vendorCode Only valid if error == Error::VENDOR. The vendorCode must be used to index
       *                   into the configuration
       *                   com.android.internal.R.fingerprint_error_vendor that's installed on the
       *                   vendor partition.
       */
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
      /**
       * This method must only be used to notify the framework during the ISession#enroll operation.
       * 
       * @param enrollmentId Unique stable identifier for the enrollment that's being added by this
       *                     ISession#enroll invocation.
       * @param remaining Remaining number of steps before enrollment is complete.
       */
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
      /**
       * This method must only be used to notify the framework during ISession#authenticate.
       * 
       * Used to notify the framework upon successful authentication. Note that the authentication
       * lifecycle ends when either 1) a fingerprint is accepted, or 2) an error occurred. The
       * authentication lifecycle does NOT end when a fingerprint is rejected.
       * 
       * @param enrollmentId Fingerprint that was accepted.
       * @param hat If the sensor is configured as SensorStrength::STRONG, a non-null attestation that
       *            a fingerprint was accepted. The HardwareAuthToken's "challenge" field must be set
       *            with the operationId passed in during ISession#authenticate. If the sensor is NOT
       *            SensorStrength::STRONG, the HardwareAuthToken MUST be null.
       */
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
      /**
       * This method must only be used to notify the framework during ISession#authenticate.
       * 
       * Used to notify the framework upon rejected attempts. Note that the authentication
       * lifecycle ends when either 1) a fingerprint is accepted, or 2) an error occurred.
       * The authentication lifecycle does NOT end when a fingerprint is rejected.
       */
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
      /**
       * This method must only be used to notify the framework during ISession#authenticate.
       * 
       * Authentication is locked out due to too many unsuccessful attempts. This is a rate-limiting
       * lockout, and authentication can be restarted after a period of time. See
       * ISession#resetLockout.
       * 
       * @param sensorId Sensor for which the user is locked out.
       * @param userId User for which the sensor is locked out.
       * @param durationMillis Remaining duration of the lockout.
       */
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
      /**
       * This method must only be used to notify the framework during ISession#authenticate.
       * 
       * Authentication is disabled until the user unlocks with their device credential
       * (PIN/Pattern/Password). See ISession#resetLockout.
       * 
       * @param sensorId Sensor for which the user is locked out.
       * @param userId User for which the sensor is locked out.
       */
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
      /**
       * Notifies the framework that lockout has been cleared for this (sensorId, userId) pair.
       * 
       * Note that this method can be used to notify the framework during any state.
       * 
       * Lockout can be cleared in the following scenarios:
       * 1) A timed lockout has ended (e.g. durationMillis specified in previous #onLockoutTimed
       *    has expired.
       * 2) See ISession#resetLockout.
       * 
       * @param sensorId Sensor for which the user's lockout is cleared.
       * @param userId User for the sensor's lockout is cleared.
       */
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
      /**
       * This method must only be used to notify the framework during
       * ISession#detectInteraction
       * 
       * Notifies the framework that user interaction occurred. See ISession#detectInteraction.
       */
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
      /**
       * This method must only be used to notify the framework during
       * ISession#enumerateEnrollments.
       * 
       * Notifies the framework of the current enrollments. See ISession#enumerateEnrollments.
       * 
       * @param enrollmentIds A list of enrollments for the session's (userId, sensorId) pair.
       */
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
      /**
       * This method must only be used to notify the framework during
       * ISession#removeEnrollments.
       * 
       * Notifies the framework that the specified enrollments are removed.
       * 
       * @param enrollmentIds The enrollments that were removed.
       */
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
      /**
       * This method must only be used to notify the framework during
       * ISession#getAuthenticatorId.
       * 
       * Notifies the framework with the authenticatorId corresponding to this session's
       * (userId, sensorId) pair.
       * 
       * @param authenticatorId See the above documentation.
       */
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
      /**
       * This method must only be used to notify the framework during
       * ISession#invalidateAuthenticatorId.
       * 
       * See ISession#invalidateAuthenticatorId for more information.
       * 
       * @param newAuthenticatorId The new entropy-encoded random identifier associated with the
       *                           current set of enrollments.
       */
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
      /**
       * This method notifes the client that this session has closed.
       * The client must not make any more calls to this session.
       */
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
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$biometrics$fingerprint$ISessionCallback".replace('$', '.');
  /** Notifies the framework when a challenge is successfully generated. */
  public void onChallengeGenerated(long challenge) throws android.os.RemoteException;
  /** Notifies the framework when a challenge has been revoked. */
  public void onChallengeRevoked(long challenge) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during the following operations:
   *   - ISession#enroll
   *   - ISession#authenticate
   *   - ISession#detectInteraction
   * 
   * These messages may be used to provide user guidance multiple times per operation if
   * necessary.
   * 
   * @param info See the AcquiredInfo enum.
   * @param vendorCode Only valid if info == AcquiredInfo::VENDOR. The vendorCode must be used to
   *                   index into the configuration
   *                   com.android.internal.R.array.fingerprint_acquired_vendor that's installed
   *                   on the vendor partition.
   */
  public void onAcquired(byte info, int vendorCode) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during the following operations:
   *   - ISession#enroll
   *   - ISession#authenticate
   *   - ISession#detectInteraction
   *   - ISession#invalidateAuthenticatorId
   *   - ISession#resetLockout
   * 
   * These messages may be used to notify the framework or user that a non-recoverable error
   * has occurred. The operation is finished, and the HAL can proceed with the next operation
   * or return to the idling state.
   * 
   * Note that cancellation (see common::ICancellationSignal) must be followed with an
   * Error::CANCELED message.
   * 
   * @param error See the Error enum.
   * @param vendorCode Only valid if error == Error::VENDOR. The vendorCode must be used to index
   *                   into the configuration
   *                   com.android.internal.R.fingerprint_error_vendor that's installed on the
   *                   vendor partition.
   */
  public void onError(byte error, int vendorCode) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during the ISession#enroll operation.
   * 
   * @param enrollmentId Unique stable identifier for the enrollment that's being added by this
   *                     ISession#enroll invocation.
   * @param remaining Remaining number of steps before enrollment is complete.
   */
  public void onEnrollmentProgress(int enrollmentId, int remaining) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during ISession#authenticate.
   * 
   * Used to notify the framework upon successful authentication. Note that the authentication
   * lifecycle ends when either 1) a fingerprint is accepted, or 2) an error occurred. The
   * authentication lifecycle does NOT end when a fingerprint is rejected.
   * 
   * @param enrollmentId Fingerprint that was accepted.
   * @param hat If the sensor is configured as SensorStrength::STRONG, a non-null attestation that
   *            a fingerprint was accepted. The HardwareAuthToken's "challenge" field must be set
   *            with the operationId passed in during ISession#authenticate. If the sensor is NOT
   *            SensorStrength::STRONG, the HardwareAuthToken MUST be null.
   */
  public void onAuthenticationSucceeded(int enrollmentId, android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during ISession#authenticate.
   * 
   * Used to notify the framework upon rejected attempts. Note that the authentication
   * lifecycle ends when either 1) a fingerprint is accepted, or 2) an error occurred.
   * The authentication lifecycle does NOT end when a fingerprint is rejected.
   */
  public void onAuthenticationFailed() throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during ISession#authenticate.
   * 
   * Authentication is locked out due to too many unsuccessful attempts. This is a rate-limiting
   * lockout, and authentication can be restarted after a period of time. See
   * ISession#resetLockout.
   * 
   * @param sensorId Sensor for which the user is locked out.
   * @param userId User for which the sensor is locked out.
   * @param durationMillis Remaining duration of the lockout.
   */
  public void onLockoutTimed(long durationMillis) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during ISession#authenticate.
   * 
   * Authentication is disabled until the user unlocks with their device credential
   * (PIN/Pattern/Password). See ISession#resetLockout.
   * 
   * @param sensorId Sensor for which the user is locked out.
   * @param userId User for which the sensor is locked out.
   */
  public void onLockoutPermanent() throws android.os.RemoteException;
  /**
   * Notifies the framework that lockout has been cleared for this (sensorId, userId) pair.
   * 
   * Note that this method can be used to notify the framework during any state.
   * 
   * Lockout can be cleared in the following scenarios:
   * 1) A timed lockout has ended (e.g. durationMillis specified in previous #onLockoutTimed
   *    has expired.
   * 2) See ISession#resetLockout.
   * 
   * @param sensorId Sensor for which the user's lockout is cleared.
   * @param userId User for the sensor's lockout is cleared.
   */
  public void onLockoutCleared() throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during
   * ISession#detectInteraction
   * 
   * Notifies the framework that user interaction occurred. See ISession#detectInteraction.
   */
  public void onInteractionDetected() throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during
   * ISession#enumerateEnrollments.
   * 
   * Notifies the framework of the current enrollments. See ISession#enumerateEnrollments.
   * 
   * @param enrollmentIds A list of enrollments for the session's (userId, sensorId) pair.
   */
  public void onEnrollmentsEnumerated(int[] enrollmentIds) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during
   * ISession#removeEnrollments.
   * 
   * Notifies the framework that the specified enrollments are removed.
   * 
   * @param enrollmentIds The enrollments that were removed.
   */
  public void onEnrollmentsRemoved(int[] enrollmentIds) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during
   * ISession#getAuthenticatorId.
   * 
   * Notifies the framework with the authenticatorId corresponding to this session's
   * (userId, sensorId) pair.
   * 
   * @param authenticatorId See the above documentation.
   */
  public void onAuthenticatorIdRetrieved(long authenticatorId) throws android.os.RemoteException;
  /**
   * This method must only be used to notify the framework during
   * ISession#invalidateAuthenticatorId.
   * 
   * See ISession#invalidateAuthenticatorId for more information.
   * 
   * @param newAuthenticatorId The new entropy-encoded random identifier associated with the
   *                           current set of enrollments.
   */
  public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws android.os.RemoteException;
  /**
   * This method notifes the client that this session has closed.
   * The client must not make any more calls to this session.
   */
  public void onSessionClosed() throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
