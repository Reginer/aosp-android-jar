/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl --lang=java --structured --version 5 --hash notfrozen -t --stability vintf --min_sdk_version platform_apis -pout/soong/.intermediates/hardware/interfaces/biometrics/common/aidl/android.hardware.biometrics.common_interface/4/preprocessed.aidl -pout/soong/.intermediates/hardware/interfaces/keymaster/aidl/android.hardware.keymaster_interface/4/preprocessed.aidl --previous_api_dir=hardware/interfaces/biometrics/fingerprint/aidl/aidl_api/android.hardware.biometrics.fingerprint/4 --previous_hash 41a730a7a6b5aa9cebebce70ee5b5e509b0af6fb --ninja -d out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen/android/hardware/biometrics/fingerprint/ISession.java.d -o out/soong/.intermediates/hardware/interfaces/biometrics/fingerprint/aidl/android.hardware.biometrics.fingerprint-V5-java-source/gen -Nhardware/interfaces/biometrics/fingerprint/aidl hardware/interfaces/biometrics/fingerprint/aidl/android/hardware/biometrics/fingerprint/ISession.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package android.hardware.biometrics.fingerprint;
/**
 * Operations defined within this interface can be split into the following categories:
 *   1) Non-interrupting operations. These operations are handled by the HAL in a FIFO order.
 *     1a) Cancellable operations. These operations can usually run for several minutes. To allow
 *         for cancellation, they return an instance of ICancellationSignal that allows the
 *         framework to cancel them by calling ICancellationSignal#cancel. If such an operation is
 *         cancelled, it must notify the framework by calling ISessionCallback#onError with
 *         Error::CANCELED.
 *     1b) Non-cancellable operations. Such operations cannot be cancelled once started.
 *   2) Interrupting operations. These operations may be invoked by the framework immediately,
 *      regardless of whether another operation is executing. For example, on devices with sensors
 *      of FingerprintSensorType::UNDER_DISPLAY_*, ISession#onPointerDown may be invoked while the
 *      HAL is executing ISession#enroll, ISession#authenticate or ISession#detectInteraction.
 * 
 * The lifecycle of a non-interrupting operation ends when one of its final callbacks is called.
 * For example, ISession#authenticate is considered completed when either ISessionCallback#onError
 * or ISessionCallback#onAuthenticationSucceeded is called.
 * 
 * The lifecycle of an interrupting operation ends when it returns. Interrupting operations do not
 * have callbacks.
 * 
 * ISession only supports execution of one non-interrupting operation at a time, regardless of
 * whether it's cancellable. The framework must wait for a callback indicating the end of the
 * current non-interrupting operation before a new non-interrupting operation can be started.
 * @hide
 */
public interface ISession extends android.os.IInterface
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
  /** Default implementation for ISession. */
  public static class Default implements android.hardware.biometrics.fingerprint.ISession
  {
    /** Methods applicable to any fingerprint type. */
    /**
     * generateChallenge:
     * 
     * Begins a secure transaction request. Note that the challenge by itself is not useful. It only
     * becomes useful when wrapped in a verifiable message such as a HardwareAuthToken.
     * 
     * Canonical example:
     *   1) User requests an operation, such as fingerprint enrollment.
     *   2) Fingerprint enrollment cannot happen until the user confirms their lockscreen credential
     *      (PIN/Pattern/Password).
     *   3) However, the biometric subsystem does not want just "any" proof of credential
     *      confirmation. It needs proof that the user explicitly authenticated credential in order
     *      to allow addition of biometric enrollments.
     * To secure this path, the following path is taken:
     *   1) Upon user requesting fingerprint enroll, the framework requests
     *      ISession#generateChallenge
     *   2) Framework sends the challenge to the credential subsystem, and upon credential
     *      confirmation, a HAT is created, containing the challenge in the "challenge" field.
     *   3) Framework sends the HAT to the HAL, e.g. ISession#enroll.
     *   4) Implementation verifies the authenticity and integrity of the HAT.
     *   5) Implementation now has confidence that the user entered their credential to allow
     *      biometric enrollment.
     * 
     * Note that this interface allows multiple in-flight challenges. Invoking generateChallenge
     * twice does not invalidate the first challenge. The challenge is invalidated only when:
     *   1) Its lifespan exceeds the HAL's internal challenge timeout
     *   2) IFingerprint#revokeChallenge is invoked
     * 
     * For example, the following is a possible table of valid challenges:
     * ----------------------------------------------
     * | SensorId | UserId | ValidUntil | Challenge |
     * |----------|--------|------------|-----------|
     * | 0        | 0      | <Time1>    | <Random1> |
     * | 0        | 0      | <Time2>    | <Random2> |
     * | 1        | 0      | <Time3>    | <Random3> |
     * | 0        | 10     | <Time4>    | <Random4> |
     * ----------------------------------------------
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onChallengeGenerated
     */
    @Override public void generateChallenge() throws android.os.RemoteException
    {
    }
    /**
     * revokeChallenge:
     * 
     * Revokes a challenge that was previously generated. Note that if a non-existent challenge is
     * provided, the HAL must still notify the framework using ISessionCallback#onChallengeRevoked.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onChallengeRevoked
     * 
     * @param challenge Challenge that should be revoked.
     */
    @Override public void revokeChallenge(long challenge) throws android.os.RemoteException
    {
    }
    /**
     * enroll:
     * 
     * A request to add a fingerprint enrollment.
     * 
     * At any point during enrollment, if a non-recoverable error occurs, the HAL must notify the
     * framework via ISessionCallback#onError with the applicable enrollment-specific error.
     * 
     * Before capturing fingerprint data, the HAL must first verify the authenticity and integrity
     * of the provided HardwareAuthToken. In addition, it must check that the challenge within the
     * provided HardwareAuthToken is valid. See ISession#generateChallenge. If any of the above
     * checks fail, the framework must be notified using ISessionCallback#onError with
     * Error::UNABLE_TO_PROCESS.
     * 
     * During enrollment, the HAL may notify the framework via ISessionCallback#onAcquired with
     * messages that may be used to guide the user. This callback can be invoked multiple times if
     * necessary. Similarly, the framework may be notified of enrollment progress changes via
     * ISessionCallback#onEnrollmentProgress. Once the framework is notified that there are 0
     * "remaining" steps, the framework may cache the "enrollmentId". See
     * ISessionCallback#onEnrollmentProgress for more info.
     * 
     * When a finger is successfully added and before the framework is notified of remaining=0, the
     * HAL must update and associate this (sensorId, userId) pair with a new entropy-encoded random
     * identifier. See ISession#getAuthenticatorId for more information.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onError
     *   - ISessionCallback#onEnrollmentProgress(enrollmentId, remaining=0)
     * 
     * Other applicable callbacks:
     *   - ISessionCallback#onAcquired
     * 
     * @param hat See above documentation.
     * @return ICancellationSignal An object that can be used by the framework to cancel this
     *                             operation.
     */
    @Override public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * authenticate:
     * 
     * A request to start looking for fingerprints to authenticate.
     * 
     * At any point during authentication, if a non-recoverable error occurs, the HAL must notify
     * the framework via ISessionCallback#onError with the applicable authentication-specific error.
     * 
     * During authentication, the HAL may notify the framework via ISessionCallback#onAcquired with
     * messages that may be used to guide the user. This callback can be invoked multiple times if
     * necessary.
     * 
     * The HAL must notify the framework of accepts and rejects via
     * ISessionCallback#onAuthenticationSucceeded and ISessionCallback#onAuthenticationFailed,
     * correspondingly.
     * 
     * The authentication lifecycle ends when either:
     *   1) A fingerprint is accepted, and ISessionCallback#onAuthenticationSucceeded is invoked.
     *   2) Any non-recoverable error occurs (such as lockout). See the full list of
     *      authentication-specific errors in the Error enum.
     * 
     * Note that upon successful authentication, the lockout counter for this (sensorId, userId)
     * pair must be cleared.
     * 
     * Note that upon successful authentication, ONLY sensors configured as SensorStrength::STRONG
     * are allowed to create and send a HardwareAuthToken to the framework. See the Android CDD for
     * more details. For SensorStrength::STRONG sensors, the HardwareAuthToken's "challenge" field
     * must be set with the operationId passed in during #authenticate. If the sensor is NOT
     * SensorStrength::STRONG, the HardwareAuthToken MUST be null.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onError
     *   - ISessionCallback#onAuthenticationSucceeded
     * 
     * Other applicable callbacks:
     *   - ISessionCallback#onAcquired
     *   - ISessionCallback#onAuthenticationFailed
     *   - ISessionCallback#onLockoutTimed
     *   - ISessionCallback#onLockoutPermanent
     * 
     * @param operationId For sensors configured as SensorStrength::STRONG, this must be used ONLY
     *                    upon successful authentication and wrapped in the HardwareAuthToken's
     *                    "challenge" field and sent to the framework via
     *                    ISessionCallback#onAuthenticationSucceeded. The operationId is an opaque
     *                    identifier created from a separate secure subsystem such as, but not
     *                    limited to KeyStore/KeyMaster. The HardwareAuthToken can then be used as
     *                    an attestation for the provided operation. For example, this is used to
     *                    unlock biometric-bound auth-per-use keys (see
     *                    setUserAuthenticationParameters in KeyGenParameterSpec.Builder and
     *                    KeyProtection.Builder).
     * @return ICancellationSignal An object that can be used by the framework to cancel this
     * operation.
     */
    @Override public android.hardware.biometrics.common.ICancellationSignal authenticate(long operationId) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * detectInteraction:
     * 
     * A request to start looking for fingerprints without performing matching. Must only be called
     * if SensorProps#supportsDetectInteraction is true. If invoked on HALs that do not support this
     * functionality, the HAL must respond with ISession#onError(UNABLE_TO_PROCESS, 0).
     * 
     * The framework will use this operation in cases where determining user presence is required,
     * but identifying/authenticating is not. For example, when the device is encrypted (first boot)
     * or in lockdown mode.
     * 
     * At any point during detectInteraction, if a non-recoverable error occurs, the HAL must notify
     * the framework via ISessionCallback#onError with the applicable error.
     * 
     * The HAL must only check whether a fingerprint-like image was detected (e.g. to minimize
     * interactions due to non-fingerprint objects), and the lockout counter must not be modified.
     * 
     * Upon detecting any fingerprint, the HAL must invoke ISessionCallback#onInteractionDetected.
     * 
     * The lifecycle of this operation ends when either:
     * 1) Any fingerprint is detected and the framework is notified via
     *    ISessionCallback#onInteractionDetected.
     * 2) An error occurs, for example Error::TIMEOUT.
     * 
     * Note that if the operation is canceled, the HAL must notify the framework via
     * ISessionCallback#onError with Error::CANCELED.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onError
     *   - ISessionCallback#onInteractionDetected
     * 
     * Other applicable callbacks:
     *   - ISessionCallback#onAcquired
     * 
     * @return ICancellationSignal An object that can be used by the framework to cancel this
     *                             operation.
     */
    @Override public android.hardware.biometrics.common.ICancellationSignal detectInteraction() throws android.os.RemoteException
    {
      return null;
    }
    /**
     * enumerateEnrollments:
     * 
     * A request to enumerate (list) the enrollments for this (sensorId, userId) pair. The framework
     * typically uses this to ensure that its cache is in sync with the HAL.
     * 
     * The HAL must then notify the framework with a list of enrollments applicable for the current
     * session via ISessionCallback#onEnrollmentsEnumerated.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onEnrollmentsEnumerated
     */
    @Override public void enumerateEnrollments() throws android.os.RemoteException
    {
    }
    /**
     * removeEnrollments:
     * 
     * A request to remove the enrollments for this (sensorId, userId) pair.
     * 
     * After removing the enrollmentIds from everywhere necessary (filesystem, secure subsystems,
     * etc), the HAL must notify the framework via ISessionCallback#onEnrollmentsRemoved.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onEnrollmentsRemoved
     * 
     * @param enrollmentIds a list of enrollments that should be removed.
     */
    @Override public void removeEnrollments(int[] enrollmentIds) throws android.os.RemoteException
    {
    }
    /**
     * getAuthenticatorId:
     * 
     * MUST return 0 via ISessionCallback#onAuthenticatorIdRetrieved for sensors that are configured
     * as SensorStrength::WEAK or SensorStrength::CONVENIENCE.
     * 
     * The following only applies to sensors that are configured as SensorStrength::STRONG.
     * 
     * The authenticatorId is a (sensorId, user)-specific identifier which can be used during key
     * generation and import to associate the key (in KeyStore / KeyMaster) with the current set of
     * enrolled fingerprints. For example, the following public Android APIs allow for keys to be
     * invalidated when the user adds a new enrollment after the key was created:
     * KeyGenParameterSpec.Builder.setInvalidatedByBiometricEnrollment and
     * KeyProtection.Builder.setInvalidatedByBiometricEnrollment.
     * 
     * In addition, upon successful fingerprint authentication, the signed HAT that is returned to
     * the framework via ISessionCallback#onAuthenticationSucceeded must contain this identifier in
     * the authenticatorId field.
     * 
     * Returns an entropy-encoded random identifier associated with the current set of enrollments
     * via ISessionCallback#onAuthenticatorIdRetrieved. The authenticatorId
     *   1) MUST change whenever a new fingerprint is enrolled
     *   2) MUST return 0 if no fingerprints are enrolled
     *   3) MUST not change if a fingerprint is deleted.
     *   4) MUST be an entropy-encoded random number
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onAuthenticatorIdRetrieved
     */
    @Override public void getAuthenticatorId() throws android.os.RemoteException
    {
    }
    /**
     * invalidateAuthenticatorId:
     * 
     * This operation only applies to sensors that are configured as SensorStrength::STRONG. If
     * invoked by the framework for sensors of other strengths, the HAL should immediately invoke
     * ISessionCallback#onAuthenticatorIdInvalidated.
     * 
     * The following only applies to sensors that are configured as SensorStrength::STRONG.
     * 
     * When invoked by the framework, the HAL must perform the following sequence of events:
     *   1) Update the authenticatorId with a new entropy-encoded random number
     *   2) Persist the new authenticatorId to non-ephemeral storage
     *   3) Notify the framework that the above is completed, via
     *      ISessionCallback#onAuthenticatorInvalidated
     * 
     * A practical use case of invalidation would be when the user adds a new enrollment to a sensor
     * managed by a different HAL instance. The public android.security.keystore APIs bind keys to
     * "all biometrics" rather than "fingerprint-only" or "face-only" (see #getAuthenticatorId for
     * more details). As such, the framework would coordinate invalidation across multiple biometric
     * HALs as necessary.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onAuthenticatorIdInvalidated
     */
    @Override public void invalidateAuthenticatorId() throws android.os.RemoteException
    {
    }
    /**
     * resetLockout:
     * 
     * Requests the HAL to clear the lockout counter. Upon receiving this request, the HAL must
     * perform the following:
     *   1) Verify the authenticity and integrity of the provided HAT
     *   2) Verify that the timestamp provided within the HAT is relatively recent (e.g. on the
     *      order of minutes, not hours).
     * If either of the checks fail, the HAL must invoke ISessionCallback#onError with
     * Error::UNABLE_TO_PROCESS.
     * 
     * Upon successful verification, the HAL must clear the lockout counter and notify the framework
     * via ISessionCallback#onLockoutCleared.
     * 
     * Note that lockout is user AND sensor specific. In other words, there is a separate lockout
     * state for each (user, sensor) pair. For example, the following is a valid state on a
     * multi-sensor device:
     * ------------------------------------------------------------------
     * | SensorId | UserId | FailedAttempts | LockedOut | LockedUntil   |
     * |----------|--------|----------------|-----------|---------------|
     * | 0        | 0      | 1              | false     | x             |
     * | 1        | 0      | 5              | true      | <future_time> |
     * | 0        | 10     | 0              | false     | x             |
     * | 1        | 10     | 0              | false     | x             |
     * ------------------------------------------------------------------
     * 
     * Lockout may be cleared in the following ways:
     *   1) ISession#resetLockout
     *   2) After a period of time, according to a rate-limiter.
     * 
     * Note that the "FailedAttempts" counter must be cleared upon successful fingerprint
     * authentication. For example, if SensorId=0 UserId=0 FailedAttempts=1, and a successful
     * fingerprint authentication occurs, the counter for that (SensorId, UserId) pair must be reset
     * to 0.
     * 
     * In addition, lockout states MUST persist after device reboots, HAL crashes, etc.
     * 
     * See the Android CDD section 7.3.10 for the full set of lockout and rate-limiting
     * requirements.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onLockoutCleared
     * 
     * @param hat HardwareAuthToken See above documentation.
     */
    @Override public void resetLockout(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException
    {
    }
    /**
     * Close this session and allow the HAL to release the resources associated with this session.
     * 
     * A session can only be closed when the HAL is idling, i.e. not performing any of the
     * non-interruptable operations. If the HAL is busy performing a cancellable operation, the
     * operation must be explicitly cancelled with a call to ICancellationSignal#cancel before
     * the session can be closed.
     * 
     * After a session is closed, the HAL must notify the framework by calling
     * ISessionCallback#onSessionClosed.
     * 
     * All sessions must be explicitly closed. Calling IFingerprint#createSession while there is an
     * active session is considered an error.
     * 
     * Callbacks that signify the end of this operation's lifecycle:
     *   - ISessionCallback#onSessionClosed
     */
    @Override public void close() throws android.os.RemoteException
    {
    }
    /** Methods for notifying the under-display fingerprint sensor about external events. */
    /**
     * onPointerDown:
     * 
     * This operation only applies to sensors that are configured as
     * FingerprintSensorType::UNDER_DISPLAY_*. If invoked erroneously by the framework for sensors
     * of other types, the HAL must treat this as a no-op and return immediately.
     * 
     * This operation is used to notify the HAL of display touches. This operation can be invoked
     * when the HAL is performing any one of: ISession#authenticate, ISession#enroll,
     * ISession#detectInteraction.
     * 
     * Note that the framework will only invoke this operation if the event occurred on the display
     * on which this sensor is located.
     * 
     * Note that for sensors which require illumination such as
     * FingerprintSensorType::UNDER_DISPLAY_OPTICAL, and where illumination is handled below the
     * framework, this is a good time to start illuminating.
     * 
     * @param pointerId See android.view.MotionEvent#getPointerId
     * @param x The distance in pixels from the left edge of the display.
     * @param y The distance in pixels from the top edge of the display.
     * @param minor See android.view.MotionEvent#getTouchMinor
     * @param major See android.view.MotionEvent#getTouchMajor
     * 
     * @deprecated use onPointerDownWithContext instead.
     */
    @Override public void onPointerDown(int pointerId, int x, int y, float minor, float major) throws android.os.RemoteException
    {
    }
    /**
     * onPointerUp:
     * 
     * This operation only applies to sensors that are configured as
     * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
     * treat this as a no-op and return immediately.
     * 
     * This operation can be invoked when the HAL is performing any one of: ISession#authenticate,
     * ISession#enroll, ISession#detectInteraction.
     * 
     * @param pointerId See android.view.MotionEvent#getPointerId
     * 
     * @deprecated use onPointerUpWithContext instead.
     */
    @Override public void onPointerUp(int pointerId) throws android.os.RemoteException
    {
    }
    /**
     * onUiReady:
     * 
     * This operation only applies to sensors that are configured as
     * FingerprintSensorType::UNDER_DISPLAY_OPTICAL. If invoked for sensors of other types, the HAL
     * must treat this as a no-op and return immediately.
     * 
     * This operation can be invoked when the HAL is performing any one of: ISession#authenticate,
     * ISession#enroll, ISession#detectInteraction.
     * 
     * For FingerprintSensorType::UNDER_DISPLAY_OPTICAL where illumination is handled above the
     * HAL, the framework will invoke this operation to notify when the illumination is showing.
     */
    @Override public void onUiReady() throws android.os.RemoteException
    {
    }
    /**
     * These are alternative methods for some operations to allow the HAL to make optional
     * optimizations during execution.
     * 
     * HALs may ignore the additional context and treat all *WithContext methods the same as
     * the original methods.
     */
    /** See ISession#authenticate(long) */
    @Override public android.hardware.biometrics.common.ICancellationSignal authenticateWithContext(long operationId, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    /** See ISession#enroll(HardwareAuthToken) */
    @Override public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    /** See ISession#detectInteraction() */
    @Override public android.hardware.biometrics.common.ICancellationSignal detectInteractionWithContext(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
      return null;
    }
    /**
     * onPointerDownWithContext:
     * 
     * This operation only applies to sensors that are configured as
     * FingerprintSensorType::UNDER_DISPLAY_*. If invoked erroneously by the framework for sensors
     * of other types, the HAL must treat this as a no-op and return immediately.
     * 
     * Notifies the HAL that a finger entered the sensor area. This operation can be invoked
     * regardless of the current state of the HAL.
     * 
     * Note that for sensors which require illumination, for example
     * FingerprintSensorType::UNDER_DISPLAY_OPTICAL, this is a good time to start illuminating.
     * 
     * @param context See PointerContext
     */
    @Override public void onPointerDownWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
    {
    }
    /**
     * onPointerUpWithContext:
     * 
     * This operation only applies to sensors that are configured as
     * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
     * treat this as a no-op and return immediately.
     * 
     * Notifies the HAL that a finger left the sensor area. This operation can be invoked regardless
     * of the current state of the HAL.
     * 
     * @param context See PointerContext
     */
    @Override public void onPointerUpWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
    {
    }
    /**
     * onContextChanged:
     * 
     * This may be called while an authenticate, detect interaction, or enrollment operation is
     * running when the context changes.
     */
    @Override public void onContextChanged(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException
    {
    }
    /**
     * onPointerCancelWithContext:
     * 
     * This operation only applies to sensors that are configured as
     * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
     * treat this as a no-op and return immediately.
     * 
     * Notifies the HAL that if there were fingers within the sensor area, they are no longer being
     * tracked. The fingers may or may not still be on the sensor. This operation can be invoked
     * regardless of the current state of the HAL.
     * 
     * @param context See PointerContext
     */
    @Override public void onPointerCancelWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException
    {
    }
    /**
     * setIgnoreDisplayTouches:
     * 
     * This operation only applies to sensors that have SensorProps#halHandlesDisplayTouches
     * set to true. For all other sensors this is a no-op.
     * 
     * Instructs the HAL whether to ignore display touches. This can be useful to avoid unintended
     * fingerprint captures during certain UI interactions. For example, when entering a lockscreen
     * PIN, some of the touches might overlap with the fingerprint sensor. Those touches should be
     * ignored to avoid unintended authentication attempts.
     * 
     * This flag must default to false when the HAL starts.
     * 
     * The framework is responsible for both setting the flag to true and resetting it to false
     * whenever it's appropriate.
     * 
     * @param shouldIgnore whether the display touches should be ignored.
     * 
     * @deprecated use isHardwareIgnoringTouches in OperationContext from onContextChanged instead
     */
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
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
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
      /** Methods applicable to any fingerprint type. */
      /**
       * generateChallenge:
       * 
       * Begins a secure transaction request. Note that the challenge by itself is not useful. It only
       * becomes useful when wrapped in a verifiable message such as a HardwareAuthToken.
       * 
       * Canonical example:
       *   1) User requests an operation, such as fingerprint enrollment.
       *   2) Fingerprint enrollment cannot happen until the user confirms their lockscreen credential
       *      (PIN/Pattern/Password).
       *   3) However, the biometric subsystem does not want just "any" proof of credential
       *      confirmation. It needs proof that the user explicitly authenticated credential in order
       *      to allow addition of biometric enrollments.
       * To secure this path, the following path is taken:
       *   1) Upon user requesting fingerprint enroll, the framework requests
       *      ISession#generateChallenge
       *   2) Framework sends the challenge to the credential subsystem, and upon credential
       *      confirmation, a HAT is created, containing the challenge in the "challenge" field.
       *   3) Framework sends the HAT to the HAL, e.g. ISession#enroll.
       *   4) Implementation verifies the authenticity and integrity of the HAT.
       *   5) Implementation now has confidence that the user entered their credential to allow
       *      biometric enrollment.
       * 
       * Note that this interface allows multiple in-flight challenges. Invoking generateChallenge
       * twice does not invalidate the first challenge. The challenge is invalidated only when:
       *   1) Its lifespan exceeds the HAL's internal challenge timeout
       *   2) IFingerprint#revokeChallenge is invoked
       * 
       * For example, the following is a possible table of valid challenges:
       * ----------------------------------------------
       * | SensorId | UserId | ValidUntil | Challenge |
       * |----------|--------|------------|-----------|
       * | 0        | 0      | <Time1>    | <Random1> |
       * | 0        | 0      | <Time2>    | <Random2> |
       * | 1        | 0      | <Time3>    | <Random3> |
       * | 0        | 10     | <Time4>    | <Random4> |
       * ----------------------------------------------
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onChallengeGenerated
       */
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
      /**
       * revokeChallenge:
       * 
       * Revokes a challenge that was previously generated. Note that if a non-existent challenge is
       * provided, the HAL must still notify the framework using ISessionCallback#onChallengeRevoked.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onChallengeRevoked
       * 
       * @param challenge Challenge that should be revoked.
       */
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
      /**
       * enroll:
       * 
       * A request to add a fingerprint enrollment.
       * 
       * At any point during enrollment, if a non-recoverable error occurs, the HAL must notify the
       * framework via ISessionCallback#onError with the applicable enrollment-specific error.
       * 
       * Before capturing fingerprint data, the HAL must first verify the authenticity and integrity
       * of the provided HardwareAuthToken. In addition, it must check that the challenge within the
       * provided HardwareAuthToken is valid. See ISession#generateChallenge. If any of the above
       * checks fail, the framework must be notified using ISessionCallback#onError with
       * Error::UNABLE_TO_PROCESS.
       * 
       * During enrollment, the HAL may notify the framework via ISessionCallback#onAcquired with
       * messages that may be used to guide the user. This callback can be invoked multiple times if
       * necessary. Similarly, the framework may be notified of enrollment progress changes via
       * ISessionCallback#onEnrollmentProgress. Once the framework is notified that there are 0
       * "remaining" steps, the framework may cache the "enrollmentId". See
       * ISessionCallback#onEnrollmentProgress for more info.
       * 
       * When a finger is successfully added and before the framework is notified of remaining=0, the
       * HAL must update and associate this (sensorId, userId) pair with a new entropy-encoded random
       * identifier. See ISession#getAuthenticatorId for more information.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onError
       *   - ISessionCallback#onEnrollmentProgress(enrollmentId, remaining=0)
       * 
       * Other applicable callbacks:
       *   - ISessionCallback#onAcquired
       * 
       * @param hat See above documentation.
       * @return ICancellationSignal An object that can be used by the framework to cancel this
       *                             operation.
       */
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
      /**
       * authenticate:
       * 
       * A request to start looking for fingerprints to authenticate.
       * 
       * At any point during authentication, if a non-recoverable error occurs, the HAL must notify
       * the framework via ISessionCallback#onError with the applicable authentication-specific error.
       * 
       * During authentication, the HAL may notify the framework via ISessionCallback#onAcquired with
       * messages that may be used to guide the user. This callback can be invoked multiple times if
       * necessary.
       * 
       * The HAL must notify the framework of accepts and rejects via
       * ISessionCallback#onAuthenticationSucceeded and ISessionCallback#onAuthenticationFailed,
       * correspondingly.
       * 
       * The authentication lifecycle ends when either:
       *   1) A fingerprint is accepted, and ISessionCallback#onAuthenticationSucceeded is invoked.
       *   2) Any non-recoverable error occurs (such as lockout). See the full list of
       *      authentication-specific errors in the Error enum.
       * 
       * Note that upon successful authentication, the lockout counter for this (sensorId, userId)
       * pair must be cleared.
       * 
       * Note that upon successful authentication, ONLY sensors configured as SensorStrength::STRONG
       * are allowed to create and send a HardwareAuthToken to the framework. See the Android CDD for
       * more details. For SensorStrength::STRONG sensors, the HardwareAuthToken's "challenge" field
       * must be set with the operationId passed in during #authenticate. If the sensor is NOT
       * SensorStrength::STRONG, the HardwareAuthToken MUST be null.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onError
       *   - ISessionCallback#onAuthenticationSucceeded
       * 
       * Other applicable callbacks:
       *   - ISessionCallback#onAcquired
       *   - ISessionCallback#onAuthenticationFailed
       *   - ISessionCallback#onLockoutTimed
       *   - ISessionCallback#onLockoutPermanent
       * 
       * @param operationId For sensors configured as SensorStrength::STRONG, this must be used ONLY
       *                    upon successful authentication and wrapped in the HardwareAuthToken's
       *                    "challenge" field and sent to the framework via
       *                    ISessionCallback#onAuthenticationSucceeded. The operationId is an opaque
       *                    identifier created from a separate secure subsystem such as, but not
       *                    limited to KeyStore/KeyMaster. The HardwareAuthToken can then be used as
       *                    an attestation for the provided operation. For example, this is used to
       *                    unlock biometric-bound auth-per-use keys (see
       *                    setUserAuthenticationParameters in KeyGenParameterSpec.Builder and
       *                    KeyProtection.Builder).
       * @return ICancellationSignal An object that can be used by the framework to cancel this
       * operation.
       */
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
      /**
       * detectInteraction:
       * 
       * A request to start looking for fingerprints without performing matching. Must only be called
       * if SensorProps#supportsDetectInteraction is true. If invoked on HALs that do not support this
       * functionality, the HAL must respond with ISession#onError(UNABLE_TO_PROCESS, 0).
       * 
       * The framework will use this operation in cases where determining user presence is required,
       * but identifying/authenticating is not. For example, when the device is encrypted (first boot)
       * or in lockdown mode.
       * 
       * At any point during detectInteraction, if a non-recoverable error occurs, the HAL must notify
       * the framework via ISessionCallback#onError with the applicable error.
       * 
       * The HAL must only check whether a fingerprint-like image was detected (e.g. to minimize
       * interactions due to non-fingerprint objects), and the lockout counter must not be modified.
       * 
       * Upon detecting any fingerprint, the HAL must invoke ISessionCallback#onInteractionDetected.
       * 
       * The lifecycle of this operation ends when either:
       * 1) Any fingerprint is detected and the framework is notified via
       *    ISessionCallback#onInteractionDetected.
       * 2) An error occurs, for example Error::TIMEOUT.
       * 
       * Note that if the operation is canceled, the HAL must notify the framework via
       * ISessionCallback#onError with Error::CANCELED.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onError
       *   - ISessionCallback#onInteractionDetected
       * 
       * Other applicable callbacks:
       *   - ISessionCallback#onAcquired
       * 
       * @return ICancellationSignal An object that can be used by the framework to cancel this
       *                             operation.
       */
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
      /**
       * enumerateEnrollments:
       * 
       * A request to enumerate (list) the enrollments for this (sensorId, userId) pair. The framework
       * typically uses this to ensure that its cache is in sync with the HAL.
       * 
       * The HAL must then notify the framework with a list of enrollments applicable for the current
       * session via ISessionCallback#onEnrollmentsEnumerated.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onEnrollmentsEnumerated
       */
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
      /**
       * removeEnrollments:
       * 
       * A request to remove the enrollments for this (sensorId, userId) pair.
       * 
       * After removing the enrollmentIds from everywhere necessary (filesystem, secure subsystems,
       * etc), the HAL must notify the framework via ISessionCallback#onEnrollmentsRemoved.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onEnrollmentsRemoved
       * 
       * @param enrollmentIds a list of enrollments that should be removed.
       */
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
      /**
       * getAuthenticatorId:
       * 
       * MUST return 0 via ISessionCallback#onAuthenticatorIdRetrieved for sensors that are configured
       * as SensorStrength::WEAK or SensorStrength::CONVENIENCE.
       * 
       * The following only applies to sensors that are configured as SensorStrength::STRONG.
       * 
       * The authenticatorId is a (sensorId, user)-specific identifier which can be used during key
       * generation and import to associate the key (in KeyStore / KeyMaster) with the current set of
       * enrolled fingerprints. For example, the following public Android APIs allow for keys to be
       * invalidated when the user adds a new enrollment after the key was created:
       * KeyGenParameterSpec.Builder.setInvalidatedByBiometricEnrollment and
       * KeyProtection.Builder.setInvalidatedByBiometricEnrollment.
       * 
       * In addition, upon successful fingerprint authentication, the signed HAT that is returned to
       * the framework via ISessionCallback#onAuthenticationSucceeded must contain this identifier in
       * the authenticatorId field.
       * 
       * Returns an entropy-encoded random identifier associated with the current set of enrollments
       * via ISessionCallback#onAuthenticatorIdRetrieved. The authenticatorId
       *   1) MUST change whenever a new fingerprint is enrolled
       *   2) MUST return 0 if no fingerprints are enrolled
       *   3) MUST not change if a fingerprint is deleted.
       *   4) MUST be an entropy-encoded random number
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onAuthenticatorIdRetrieved
       */
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
      /**
       * invalidateAuthenticatorId:
       * 
       * This operation only applies to sensors that are configured as SensorStrength::STRONG. If
       * invoked by the framework for sensors of other strengths, the HAL should immediately invoke
       * ISessionCallback#onAuthenticatorIdInvalidated.
       * 
       * The following only applies to sensors that are configured as SensorStrength::STRONG.
       * 
       * When invoked by the framework, the HAL must perform the following sequence of events:
       *   1) Update the authenticatorId with a new entropy-encoded random number
       *   2) Persist the new authenticatorId to non-ephemeral storage
       *   3) Notify the framework that the above is completed, via
       *      ISessionCallback#onAuthenticatorInvalidated
       * 
       * A practical use case of invalidation would be when the user adds a new enrollment to a sensor
       * managed by a different HAL instance. The public android.security.keystore APIs bind keys to
       * "all biometrics" rather than "fingerprint-only" or "face-only" (see #getAuthenticatorId for
       * more details). As such, the framework would coordinate invalidation across multiple biometric
       * HALs as necessary.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onAuthenticatorIdInvalidated
       */
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
      /**
       * resetLockout:
       * 
       * Requests the HAL to clear the lockout counter. Upon receiving this request, the HAL must
       * perform the following:
       *   1) Verify the authenticity and integrity of the provided HAT
       *   2) Verify that the timestamp provided within the HAT is relatively recent (e.g. on the
       *      order of minutes, not hours).
       * If either of the checks fail, the HAL must invoke ISessionCallback#onError with
       * Error::UNABLE_TO_PROCESS.
       * 
       * Upon successful verification, the HAL must clear the lockout counter and notify the framework
       * via ISessionCallback#onLockoutCleared.
       * 
       * Note that lockout is user AND sensor specific. In other words, there is a separate lockout
       * state for each (user, sensor) pair. For example, the following is a valid state on a
       * multi-sensor device:
       * ------------------------------------------------------------------
       * | SensorId | UserId | FailedAttempts | LockedOut | LockedUntil   |
       * |----------|--------|----------------|-----------|---------------|
       * | 0        | 0      | 1              | false     | x             |
       * | 1        | 0      | 5              | true      | <future_time> |
       * | 0        | 10     | 0              | false     | x             |
       * | 1        | 10     | 0              | false     | x             |
       * ------------------------------------------------------------------
       * 
       * Lockout may be cleared in the following ways:
       *   1) ISession#resetLockout
       *   2) After a period of time, according to a rate-limiter.
       * 
       * Note that the "FailedAttempts" counter must be cleared upon successful fingerprint
       * authentication. For example, if SensorId=0 UserId=0 FailedAttempts=1, and a successful
       * fingerprint authentication occurs, the counter for that (SensorId, UserId) pair must be reset
       * to 0.
       * 
       * In addition, lockout states MUST persist after device reboots, HAL crashes, etc.
       * 
       * See the Android CDD section 7.3.10 for the full set of lockout and rate-limiting
       * requirements.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onLockoutCleared
       * 
       * @param hat HardwareAuthToken See above documentation.
       */
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
      /**
       * Close this session and allow the HAL to release the resources associated with this session.
       * 
       * A session can only be closed when the HAL is idling, i.e. not performing any of the
       * non-interruptable operations. If the HAL is busy performing a cancellable operation, the
       * operation must be explicitly cancelled with a call to ICancellationSignal#cancel before
       * the session can be closed.
       * 
       * After a session is closed, the HAL must notify the framework by calling
       * ISessionCallback#onSessionClosed.
       * 
       * All sessions must be explicitly closed. Calling IFingerprint#createSession while there is an
       * active session is considered an error.
       * 
       * Callbacks that signify the end of this operation's lifecycle:
       *   - ISessionCallback#onSessionClosed
       */
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
      /** Methods for notifying the under-display fingerprint sensor about external events. */
      /**
       * onPointerDown:
       * 
       * This operation only applies to sensors that are configured as
       * FingerprintSensorType::UNDER_DISPLAY_*. If invoked erroneously by the framework for sensors
       * of other types, the HAL must treat this as a no-op and return immediately.
       * 
       * This operation is used to notify the HAL of display touches. This operation can be invoked
       * when the HAL is performing any one of: ISession#authenticate, ISession#enroll,
       * ISession#detectInteraction.
       * 
       * Note that the framework will only invoke this operation if the event occurred on the display
       * on which this sensor is located.
       * 
       * Note that for sensors which require illumination such as
       * FingerprintSensorType::UNDER_DISPLAY_OPTICAL, and where illumination is handled below the
       * framework, this is a good time to start illuminating.
       * 
       * @param pointerId See android.view.MotionEvent#getPointerId
       * @param x The distance in pixels from the left edge of the display.
       * @param y The distance in pixels from the top edge of the display.
       * @param minor See android.view.MotionEvent#getTouchMinor
       * @param major See android.view.MotionEvent#getTouchMajor
       * 
       * @deprecated use onPointerDownWithContext instead.
       */
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
      /**
       * onPointerUp:
       * 
       * This operation only applies to sensors that are configured as
       * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
       * treat this as a no-op and return immediately.
       * 
       * This operation can be invoked when the HAL is performing any one of: ISession#authenticate,
       * ISession#enroll, ISession#detectInteraction.
       * 
       * @param pointerId See android.view.MotionEvent#getPointerId
       * 
       * @deprecated use onPointerUpWithContext instead.
       */
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
      /**
       * onUiReady:
       * 
       * This operation only applies to sensors that are configured as
       * FingerprintSensorType::UNDER_DISPLAY_OPTICAL. If invoked for sensors of other types, the HAL
       * must treat this as a no-op and return immediately.
       * 
       * This operation can be invoked when the HAL is performing any one of: ISession#authenticate,
       * ISession#enroll, ISession#detectInteraction.
       * 
       * For FingerprintSensorType::UNDER_DISPLAY_OPTICAL where illumination is handled above the
       * HAL, the framework will invoke this operation to notify when the illumination is showing.
       */
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
      /**
       * These are alternative methods for some operations to allow the HAL to make optional
       * optimizations during execution.
       * 
       * HALs may ignore the additional context and treat all *WithContext methods the same as
       * the original methods.
       */
      /** See ISession#authenticate(long) */
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
      /** See ISession#enroll(HardwareAuthToken) */
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
      /** See ISession#detectInteraction() */
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
      /**
       * onPointerDownWithContext:
       * 
       * This operation only applies to sensors that are configured as
       * FingerprintSensorType::UNDER_DISPLAY_*. If invoked erroneously by the framework for sensors
       * of other types, the HAL must treat this as a no-op and return immediately.
       * 
       * Notifies the HAL that a finger entered the sensor area. This operation can be invoked
       * regardless of the current state of the HAL.
       * 
       * Note that for sensors which require illumination, for example
       * FingerprintSensorType::UNDER_DISPLAY_OPTICAL, this is a good time to start illuminating.
       * 
       * @param context See PointerContext
       */
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
      /**
       * onPointerUpWithContext:
       * 
       * This operation only applies to sensors that are configured as
       * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
       * treat this as a no-op and return immediately.
       * 
       * Notifies the HAL that a finger left the sensor area. This operation can be invoked regardless
       * of the current state of the HAL.
       * 
       * @param context See PointerContext
       */
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
      /**
       * onContextChanged:
       * 
       * This may be called while an authenticate, detect interaction, or enrollment operation is
       * running when the context changes.
       */
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
      /**
       * onPointerCancelWithContext:
       * 
       * This operation only applies to sensors that are configured as
       * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
       * treat this as a no-op and return immediately.
       * 
       * Notifies the HAL that if there were fingers within the sensor area, they are no longer being
       * tracked. The fingers may or may not still be on the sensor. This operation can be invoked
       * regardless of the current state of the HAL.
       * 
       * @param context See PointerContext
       */
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
      /**
       * setIgnoreDisplayTouches:
       * 
       * This operation only applies to sensors that have SensorProps#halHandlesDisplayTouches
       * set to true. For all other sensors this is a no-op.
       * 
       * Instructs the HAL whether to ignore display touches. This can be useful to avoid unintended
       * fingerprint captures during certain UI interactions. For example, when entering a lockscreen
       * PIN, some of the touches might overlap with the fingerprint sensor. Those touches should be
       * ignored to avoid unintended authentication attempts.
       * 
       * This flag must default to false when the HAL starts.
       * 
       * The framework is responsible for both setting the flag to true and resetting it to false
       * whenever it's appropriate.
       * 
       * @param shouldIgnore whether the display touches should be ignored.
       * 
       * @deprecated use isHardwareIgnoringTouches in OperationContext from onContextChanged instead
       */
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
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "android$hardware$biometrics$fingerprint$ISession".replace('$', '.');
  /** Methods applicable to any fingerprint type. */
  /**
   * generateChallenge:
   * 
   * Begins a secure transaction request. Note that the challenge by itself is not useful. It only
   * becomes useful when wrapped in a verifiable message such as a HardwareAuthToken.
   * 
   * Canonical example:
   *   1) User requests an operation, such as fingerprint enrollment.
   *   2) Fingerprint enrollment cannot happen until the user confirms their lockscreen credential
   *      (PIN/Pattern/Password).
   *   3) However, the biometric subsystem does not want just "any" proof of credential
   *      confirmation. It needs proof that the user explicitly authenticated credential in order
   *      to allow addition of biometric enrollments.
   * To secure this path, the following path is taken:
   *   1) Upon user requesting fingerprint enroll, the framework requests
   *      ISession#generateChallenge
   *   2) Framework sends the challenge to the credential subsystem, and upon credential
   *      confirmation, a HAT is created, containing the challenge in the "challenge" field.
   *   3) Framework sends the HAT to the HAL, e.g. ISession#enroll.
   *   4) Implementation verifies the authenticity and integrity of the HAT.
   *   5) Implementation now has confidence that the user entered their credential to allow
   *      biometric enrollment.
   * 
   * Note that this interface allows multiple in-flight challenges. Invoking generateChallenge
   * twice does not invalidate the first challenge. The challenge is invalidated only when:
   *   1) Its lifespan exceeds the HAL's internal challenge timeout
   *   2) IFingerprint#revokeChallenge is invoked
   * 
   * For example, the following is a possible table of valid challenges:
   * ----------------------------------------------
   * | SensorId | UserId | ValidUntil | Challenge |
   * |----------|--------|------------|-----------|
   * | 0        | 0      | <Time1>    | <Random1> |
   * | 0        | 0      | <Time2>    | <Random2> |
   * | 1        | 0      | <Time3>    | <Random3> |
   * | 0        | 10     | <Time4>    | <Random4> |
   * ----------------------------------------------
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onChallengeGenerated
   */
  public void generateChallenge() throws android.os.RemoteException;
  /**
   * revokeChallenge:
   * 
   * Revokes a challenge that was previously generated. Note that if a non-existent challenge is
   * provided, the HAL must still notify the framework using ISessionCallback#onChallengeRevoked.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onChallengeRevoked
   * 
   * @param challenge Challenge that should be revoked.
   */
  public void revokeChallenge(long challenge) throws android.os.RemoteException;
  /**
   * enroll:
   * 
   * A request to add a fingerprint enrollment.
   * 
   * At any point during enrollment, if a non-recoverable error occurs, the HAL must notify the
   * framework via ISessionCallback#onError with the applicable enrollment-specific error.
   * 
   * Before capturing fingerprint data, the HAL must first verify the authenticity and integrity
   * of the provided HardwareAuthToken. In addition, it must check that the challenge within the
   * provided HardwareAuthToken is valid. See ISession#generateChallenge. If any of the above
   * checks fail, the framework must be notified using ISessionCallback#onError with
   * Error::UNABLE_TO_PROCESS.
   * 
   * During enrollment, the HAL may notify the framework via ISessionCallback#onAcquired with
   * messages that may be used to guide the user. This callback can be invoked multiple times if
   * necessary. Similarly, the framework may be notified of enrollment progress changes via
   * ISessionCallback#onEnrollmentProgress. Once the framework is notified that there are 0
   * "remaining" steps, the framework may cache the "enrollmentId". See
   * ISessionCallback#onEnrollmentProgress for more info.
   * 
   * When a finger is successfully added and before the framework is notified of remaining=0, the
   * HAL must update and associate this (sensorId, userId) pair with a new entropy-encoded random
   * identifier. See ISession#getAuthenticatorId for more information.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onError
   *   - ISessionCallback#onEnrollmentProgress(enrollmentId, remaining=0)
   * 
   * Other applicable callbacks:
   *   - ISessionCallback#onAcquired
   * 
   * @param hat See above documentation.
   * @return ICancellationSignal An object that can be used by the framework to cancel this
   *                             operation.
   */
  public android.hardware.biometrics.common.ICancellationSignal enroll(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  /**
   * authenticate:
   * 
   * A request to start looking for fingerprints to authenticate.
   * 
   * At any point during authentication, if a non-recoverable error occurs, the HAL must notify
   * the framework via ISessionCallback#onError with the applicable authentication-specific error.
   * 
   * During authentication, the HAL may notify the framework via ISessionCallback#onAcquired with
   * messages that may be used to guide the user. This callback can be invoked multiple times if
   * necessary.
   * 
   * The HAL must notify the framework of accepts and rejects via
   * ISessionCallback#onAuthenticationSucceeded and ISessionCallback#onAuthenticationFailed,
   * correspondingly.
   * 
   * The authentication lifecycle ends when either:
   *   1) A fingerprint is accepted, and ISessionCallback#onAuthenticationSucceeded is invoked.
   *   2) Any non-recoverable error occurs (such as lockout). See the full list of
   *      authentication-specific errors in the Error enum.
   * 
   * Note that upon successful authentication, the lockout counter for this (sensorId, userId)
   * pair must be cleared.
   * 
   * Note that upon successful authentication, ONLY sensors configured as SensorStrength::STRONG
   * are allowed to create and send a HardwareAuthToken to the framework. See the Android CDD for
   * more details. For SensorStrength::STRONG sensors, the HardwareAuthToken's "challenge" field
   * must be set with the operationId passed in during #authenticate. If the sensor is NOT
   * SensorStrength::STRONG, the HardwareAuthToken MUST be null.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onError
   *   - ISessionCallback#onAuthenticationSucceeded
   * 
   * Other applicable callbacks:
   *   - ISessionCallback#onAcquired
   *   - ISessionCallback#onAuthenticationFailed
   *   - ISessionCallback#onLockoutTimed
   *   - ISessionCallback#onLockoutPermanent
   * 
   * @param operationId For sensors configured as SensorStrength::STRONG, this must be used ONLY
   *                    upon successful authentication and wrapped in the HardwareAuthToken's
   *                    "challenge" field and sent to the framework via
   *                    ISessionCallback#onAuthenticationSucceeded. The operationId is an opaque
   *                    identifier created from a separate secure subsystem such as, but not
   *                    limited to KeyStore/KeyMaster. The HardwareAuthToken can then be used as
   *                    an attestation for the provided operation. For example, this is used to
   *                    unlock biometric-bound auth-per-use keys (see
   *                    setUserAuthenticationParameters in KeyGenParameterSpec.Builder and
   *                    KeyProtection.Builder).
   * @return ICancellationSignal An object that can be used by the framework to cancel this
   * operation.
   */
  public android.hardware.biometrics.common.ICancellationSignal authenticate(long operationId) throws android.os.RemoteException;
  /**
   * detectInteraction:
   * 
   * A request to start looking for fingerprints without performing matching. Must only be called
   * if SensorProps#supportsDetectInteraction is true. If invoked on HALs that do not support this
   * functionality, the HAL must respond with ISession#onError(UNABLE_TO_PROCESS, 0).
   * 
   * The framework will use this operation in cases where determining user presence is required,
   * but identifying/authenticating is not. For example, when the device is encrypted (first boot)
   * or in lockdown mode.
   * 
   * At any point during detectInteraction, if a non-recoverable error occurs, the HAL must notify
   * the framework via ISessionCallback#onError with the applicable error.
   * 
   * The HAL must only check whether a fingerprint-like image was detected (e.g. to minimize
   * interactions due to non-fingerprint objects), and the lockout counter must not be modified.
   * 
   * Upon detecting any fingerprint, the HAL must invoke ISessionCallback#onInteractionDetected.
   * 
   * The lifecycle of this operation ends when either:
   * 1) Any fingerprint is detected and the framework is notified via
   *    ISessionCallback#onInteractionDetected.
   * 2) An error occurs, for example Error::TIMEOUT.
   * 
   * Note that if the operation is canceled, the HAL must notify the framework via
   * ISessionCallback#onError with Error::CANCELED.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onError
   *   - ISessionCallback#onInteractionDetected
   * 
   * Other applicable callbacks:
   *   - ISessionCallback#onAcquired
   * 
   * @return ICancellationSignal An object that can be used by the framework to cancel this
   *                             operation.
   */
  public android.hardware.biometrics.common.ICancellationSignal detectInteraction() throws android.os.RemoteException;
  /**
   * enumerateEnrollments:
   * 
   * A request to enumerate (list) the enrollments for this (sensorId, userId) pair. The framework
   * typically uses this to ensure that its cache is in sync with the HAL.
   * 
   * The HAL must then notify the framework with a list of enrollments applicable for the current
   * session via ISessionCallback#onEnrollmentsEnumerated.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onEnrollmentsEnumerated
   */
  public void enumerateEnrollments() throws android.os.RemoteException;
  /**
   * removeEnrollments:
   * 
   * A request to remove the enrollments for this (sensorId, userId) pair.
   * 
   * After removing the enrollmentIds from everywhere necessary (filesystem, secure subsystems,
   * etc), the HAL must notify the framework via ISessionCallback#onEnrollmentsRemoved.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onEnrollmentsRemoved
   * 
   * @param enrollmentIds a list of enrollments that should be removed.
   */
  public void removeEnrollments(int[] enrollmentIds) throws android.os.RemoteException;
  /**
   * getAuthenticatorId:
   * 
   * MUST return 0 via ISessionCallback#onAuthenticatorIdRetrieved for sensors that are configured
   * as SensorStrength::WEAK or SensorStrength::CONVENIENCE.
   * 
   * The following only applies to sensors that are configured as SensorStrength::STRONG.
   * 
   * The authenticatorId is a (sensorId, user)-specific identifier which can be used during key
   * generation and import to associate the key (in KeyStore / KeyMaster) with the current set of
   * enrolled fingerprints. For example, the following public Android APIs allow for keys to be
   * invalidated when the user adds a new enrollment after the key was created:
   * KeyGenParameterSpec.Builder.setInvalidatedByBiometricEnrollment and
   * KeyProtection.Builder.setInvalidatedByBiometricEnrollment.
   * 
   * In addition, upon successful fingerprint authentication, the signed HAT that is returned to
   * the framework via ISessionCallback#onAuthenticationSucceeded must contain this identifier in
   * the authenticatorId field.
   * 
   * Returns an entropy-encoded random identifier associated with the current set of enrollments
   * via ISessionCallback#onAuthenticatorIdRetrieved. The authenticatorId
   *   1) MUST change whenever a new fingerprint is enrolled
   *   2) MUST return 0 if no fingerprints are enrolled
   *   3) MUST not change if a fingerprint is deleted.
   *   4) MUST be an entropy-encoded random number
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onAuthenticatorIdRetrieved
   */
  public void getAuthenticatorId() throws android.os.RemoteException;
  /**
   * invalidateAuthenticatorId:
   * 
   * This operation only applies to sensors that are configured as SensorStrength::STRONG. If
   * invoked by the framework for sensors of other strengths, the HAL should immediately invoke
   * ISessionCallback#onAuthenticatorIdInvalidated.
   * 
   * The following only applies to sensors that are configured as SensorStrength::STRONG.
   * 
   * When invoked by the framework, the HAL must perform the following sequence of events:
   *   1) Update the authenticatorId with a new entropy-encoded random number
   *   2) Persist the new authenticatorId to non-ephemeral storage
   *   3) Notify the framework that the above is completed, via
   *      ISessionCallback#onAuthenticatorInvalidated
   * 
   * A practical use case of invalidation would be when the user adds a new enrollment to a sensor
   * managed by a different HAL instance. The public android.security.keystore APIs bind keys to
   * "all biometrics" rather than "fingerprint-only" or "face-only" (see #getAuthenticatorId for
   * more details). As such, the framework would coordinate invalidation across multiple biometric
   * HALs as necessary.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onAuthenticatorIdInvalidated
   */
  public void invalidateAuthenticatorId() throws android.os.RemoteException;
  /**
   * resetLockout:
   * 
   * Requests the HAL to clear the lockout counter. Upon receiving this request, the HAL must
   * perform the following:
   *   1) Verify the authenticity and integrity of the provided HAT
   *   2) Verify that the timestamp provided within the HAT is relatively recent (e.g. on the
   *      order of minutes, not hours).
   * If either of the checks fail, the HAL must invoke ISessionCallback#onError with
   * Error::UNABLE_TO_PROCESS.
   * 
   * Upon successful verification, the HAL must clear the lockout counter and notify the framework
   * via ISessionCallback#onLockoutCleared.
   * 
   * Note that lockout is user AND sensor specific. In other words, there is a separate lockout
   * state for each (user, sensor) pair. For example, the following is a valid state on a
   * multi-sensor device:
   * ------------------------------------------------------------------
   * | SensorId | UserId | FailedAttempts | LockedOut | LockedUntil   |
   * |----------|--------|----------------|-----------|---------------|
   * | 0        | 0      | 1              | false     | x             |
   * | 1        | 0      | 5              | true      | <future_time> |
   * | 0        | 10     | 0              | false     | x             |
   * | 1        | 10     | 0              | false     | x             |
   * ------------------------------------------------------------------
   * 
   * Lockout may be cleared in the following ways:
   *   1) ISession#resetLockout
   *   2) After a period of time, according to a rate-limiter.
   * 
   * Note that the "FailedAttempts" counter must be cleared upon successful fingerprint
   * authentication. For example, if SensorId=0 UserId=0 FailedAttempts=1, and a successful
   * fingerprint authentication occurs, the counter for that (SensorId, UserId) pair must be reset
   * to 0.
   * 
   * In addition, lockout states MUST persist after device reboots, HAL crashes, etc.
   * 
   * See the Android CDD section 7.3.10 for the full set of lockout and rate-limiting
   * requirements.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onLockoutCleared
   * 
   * @param hat HardwareAuthToken See above documentation.
   */
  public void resetLockout(android.hardware.keymaster.HardwareAuthToken hat) throws android.os.RemoteException;
  /**
   * Close this session and allow the HAL to release the resources associated with this session.
   * 
   * A session can only be closed when the HAL is idling, i.e. not performing any of the
   * non-interruptable operations. If the HAL is busy performing a cancellable operation, the
   * operation must be explicitly cancelled with a call to ICancellationSignal#cancel before
   * the session can be closed.
   * 
   * After a session is closed, the HAL must notify the framework by calling
   * ISessionCallback#onSessionClosed.
   * 
   * All sessions must be explicitly closed. Calling IFingerprint#createSession while there is an
   * active session is considered an error.
   * 
   * Callbacks that signify the end of this operation's lifecycle:
   *   - ISessionCallback#onSessionClosed
   */
  public void close() throws android.os.RemoteException;
  /** Methods for notifying the under-display fingerprint sensor about external events. */
  /**
   * onPointerDown:
   * 
   * This operation only applies to sensors that are configured as
   * FingerprintSensorType::UNDER_DISPLAY_*. If invoked erroneously by the framework for sensors
   * of other types, the HAL must treat this as a no-op and return immediately.
   * 
   * This operation is used to notify the HAL of display touches. This operation can be invoked
   * when the HAL is performing any one of: ISession#authenticate, ISession#enroll,
   * ISession#detectInteraction.
   * 
   * Note that the framework will only invoke this operation if the event occurred on the display
   * on which this sensor is located.
   * 
   * Note that for sensors which require illumination such as
   * FingerprintSensorType::UNDER_DISPLAY_OPTICAL, and where illumination is handled below the
   * framework, this is a good time to start illuminating.
   * 
   * @param pointerId See android.view.MotionEvent#getPointerId
   * @param x The distance in pixels from the left edge of the display.
   * @param y The distance in pixels from the top edge of the display.
   * @param minor See android.view.MotionEvent#getTouchMinor
   * @param major See android.view.MotionEvent#getTouchMajor
   * 
   * @deprecated use onPointerDownWithContext instead.
   */
  @Deprecated
  public void onPointerDown(int pointerId, int x, int y, float minor, float major) throws android.os.RemoteException;
  /**
   * onPointerUp:
   * 
   * This operation only applies to sensors that are configured as
   * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
   * treat this as a no-op and return immediately.
   * 
   * This operation can be invoked when the HAL is performing any one of: ISession#authenticate,
   * ISession#enroll, ISession#detectInteraction.
   * 
   * @param pointerId See android.view.MotionEvent#getPointerId
   * 
   * @deprecated use onPointerUpWithContext instead.
   */
  @Deprecated
  public void onPointerUp(int pointerId) throws android.os.RemoteException;
  /**
   * onUiReady:
   * 
   * This operation only applies to sensors that are configured as
   * FingerprintSensorType::UNDER_DISPLAY_OPTICAL. If invoked for sensors of other types, the HAL
   * must treat this as a no-op and return immediately.
   * 
   * This operation can be invoked when the HAL is performing any one of: ISession#authenticate,
   * ISession#enroll, ISession#detectInteraction.
   * 
   * For FingerprintSensorType::UNDER_DISPLAY_OPTICAL where illumination is handled above the
   * HAL, the framework will invoke this operation to notify when the illumination is showing.
   */
  public void onUiReady() throws android.os.RemoteException;
  /**
   * These are alternative methods for some operations to allow the HAL to make optional
   * optimizations during execution.
   * 
   * HALs may ignore the additional context and treat all *WithContext methods the same as
   * the original methods.
   */
  /** See ISession#authenticate(long) */
  public android.hardware.biometrics.common.ICancellationSignal authenticateWithContext(long operationId, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  /** See ISession#enroll(HardwareAuthToken) */
  public android.hardware.biometrics.common.ICancellationSignal enrollWithContext(android.hardware.keymaster.HardwareAuthToken hat, android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  /** See ISession#detectInteraction() */
  public android.hardware.biometrics.common.ICancellationSignal detectInteractionWithContext(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  /**
   * onPointerDownWithContext:
   * 
   * This operation only applies to sensors that are configured as
   * FingerprintSensorType::UNDER_DISPLAY_*. If invoked erroneously by the framework for sensors
   * of other types, the HAL must treat this as a no-op and return immediately.
   * 
   * Notifies the HAL that a finger entered the sensor area. This operation can be invoked
   * regardless of the current state of the HAL.
   * 
   * Note that for sensors which require illumination, for example
   * FingerprintSensorType::UNDER_DISPLAY_OPTICAL, this is a good time to start illuminating.
   * 
   * @param context See PointerContext
   */
  public void onPointerDownWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException;
  /**
   * onPointerUpWithContext:
   * 
   * This operation only applies to sensors that are configured as
   * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
   * treat this as a no-op and return immediately.
   * 
   * Notifies the HAL that a finger left the sensor area. This operation can be invoked regardless
   * of the current state of the HAL.
   * 
   * @param context See PointerContext
   */
  public void onPointerUpWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException;
  /**
   * onContextChanged:
   * 
   * This may be called while an authenticate, detect interaction, or enrollment operation is
   * running when the context changes.
   */
  public void onContextChanged(android.hardware.biometrics.common.OperationContext context) throws android.os.RemoteException;
  /**
   * onPointerCancelWithContext:
   * 
   * This operation only applies to sensors that are configured as
   * FingerprintSensorType::UNDER_DISPLAY_*. If invoked for sensors of other types, the HAL must
   * treat this as a no-op and return immediately.
   * 
   * Notifies the HAL that if there were fingers within the sensor area, they are no longer being
   * tracked. The fingers may or may not still be on the sensor. This operation can be invoked
   * regardless of the current state of the HAL.
   * 
   * @param context See PointerContext
   */
  public void onPointerCancelWithContext(android.hardware.biometrics.fingerprint.PointerContext context) throws android.os.RemoteException;
  /**
   * setIgnoreDisplayTouches:
   * 
   * This operation only applies to sensors that have SensorProps#halHandlesDisplayTouches
   * set to true. For all other sensors this is a no-op.
   * 
   * Instructs the HAL whether to ignore display touches. This can be useful to avoid unintended
   * fingerprint captures during certain UI interactions. For example, when entering a lockscreen
   * PIN, some of the touches might overlap with the fingerprint sensor. Those touches should be
   * ignored to avoid unintended authentication attempts.
   * 
   * This flag must default to false when the HAL starts.
   * 
   * The framework is responsible for both setting the flag to true and resetting it to false
   * whenever it's appropriate.
   * 
   * @param shouldIgnore whether the display touches should be ignored.
   * 
   * @deprecated use isHardwareIgnoringTouches in OperationContext from onContextChanged instead
   */
  @Deprecated
  public void setIgnoreDisplayTouches(boolean shouldIgnore) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
