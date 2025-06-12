/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.media;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.app.ActivityThread;
import android.app.Application;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.util.Log;

import dalvik.system.CloseGuard;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MediaDrm can be used to obtain keys for decrypting protected media streams, in
 * conjunction with {@link android.media.MediaCrypto}.  The MediaDrm APIs
 * are designed to support the ISO/IEC 23001-7: Common Encryption standard, but
 * may also be used to implement other encryption schemes.
 * <p>
 * Encrypted content is prepared using an encryption server and stored in a content
 * library. The encrypted content is streamed or downloaded from the content library to
 * client devices via content servers.  Licenses to view the content are obtained from
 * a License Server.
 * <p>
 * <p><img src="../../../images/mediadrm_overview.png"
 *      alt="MediaDrm Overview diagram"
 *      border="0" /></p>
 * <p>
 * Keys are requested from the license server using a key request. The key
 * response is delivered to the client app, which provides the response to the
 * MediaDrm API.
 * <p>
 * A Provisioning server may be required to distribute device-unique credentials to
 * the devices.
 * <p>
 * Enforcing requirements related to the number of devices that may play content
 * simultaneously can be performed either through key renewal or using the secure
 * stop methods.
 * <p>
 * The following sequence diagram shows the interactions between the objects
 * involved while playing back encrypted content:
 * <p>
 * <p><img src="../../../images/mediadrm_decryption_sequence.png"
 *         alt="MediaDrm Overview diagram"
 *         border="0" /></p>
 * <p>
 * The app first constructs {@link android.media.MediaExtractor} and
 * {@link android.media.MediaCodec} objects. It accesses the DRM-scheme-identifying UUID,
 * typically from metadata in the content, and uses this UUID to construct an instance
 * of a MediaDrm object that is able to support the DRM scheme required by the content.
 * Crypto schemes are assigned 16 byte UUIDs.  The method {@link #isCryptoSchemeSupported}
 * can be used to query if a given scheme is supported on the device.
 * <p>
 * The app calls {@link #openSession} to generate a sessionId that will uniquely identify
 * the session in subsequent interactions. The app next uses the MediaDrm object to
 * obtain a key request message and send it to the license server, then provide
 * the server's response to the MediaDrm object.
 * <p>
 * Once the app has a sessionId, it can construct a MediaCrypto object from the UUID and
 * sessionId.  The MediaCrypto object is registered with the MediaCodec in the
 * {@link MediaCodec#configure} method to enable the codec to decrypt content.
 * <p>
 * When the app has constructed {@link android.media.MediaExtractor},
 * {@link android.media.MediaCodec} and {@link android.media.MediaCrypto} objects,
 * it proceeds to pull samples from the extractor and queue them into the decoder.  For
 * encrypted content, the samples returned from the extractor remain encrypted, they
 * are only decrypted when the samples are delivered to the decoder.
 * <p>
 * MediaDrm methods throw {@link android.media.MediaDrm.MediaDrmStateException}
 * when a method is called on a MediaDrm object that has had an unrecoverable failure
 * in the DRM plugin or security hardware.
 * {@link android.media.MediaDrm.MediaDrmStateException} extends
 * {@link java.lang.IllegalStateException} with the addition of a developer-readable
 * diagnostic information string associated with the exception.
 * <p>
 * In the event of a mediaserver process crash or restart while a MediaDrm object
 * is active, MediaDrm methods may throw {@link android.media.MediaDrmResetException}.
 * To recover, the app must release the MediaDrm object, then create and initialize
 * a new one.
 * <p>
 * As {@link android.media.MediaDrmResetException} and
 * {@link android.media.MediaDrm.MediaDrmStateException} both extend
 * {@link java.lang.IllegalStateException}, they should be in an earlier catch()
 * block than {@link java.lang.IllegalStateException} if handled separately.
 * <p>
 * <a name="Callbacks"></a>
 * <h3>Callbacks</h3>
 * <p>Applications should register for informational events in order
 * to be informed of key state updates during playback or streaming.
 * Registration for these events is done via a call to
 * {@link #setOnEventListener}. In order to receive the respective
 * callback associated with this listener, applications are required to create
 * MediaDrm objects on a thread with its own Looper running (main UI
 * thread by default has a Looper running).
 */
public final class MediaDrm implements AutoCloseable {

    private static final String TAG = "MediaDrm";

    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();

    private static final String PERMISSION = android.Manifest.permission.ACCESS_DRM_CERTIFICATES;

    private long mNativeContext;
    private final String mAppPackageName;

    /**
     * Specify no certificate type
     *
     * @hide - not part of the public API at this time
     */
    public static final int CERTIFICATE_TYPE_NONE = 0;

    /**
     * Specify X.509 certificate type
     *
     * @hide - not part of the public API at this time
     */
    public static final int CERTIFICATE_TYPE_X509 = 1;

    /** @hide */
    @IntDef({
        CERTIFICATE_TYPE_NONE,
        CERTIFICATE_TYPE_X509,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CertificateType {}

    /**
     * Query if the given scheme identified by its UUID is supported on
     * this device.
     * @param uuid The UUID of the crypto scheme.
     */
    public static final boolean isCryptoSchemeSupported(@NonNull UUID uuid) {
        return isCryptoSchemeSupportedNative(getByteArrayFromUUID(uuid), null,
                SECURITY_LEVEL_UNKNOWN);
    }

    /**
     * Query if the given scheme identified by its UUID is supported on
     * this device, and whether the DRM plugin is able to handle the
     * media container format specified by mimeType.
     * @param uuid The UUID of the crypto scheme.
     * @param mimeType The MIME type of the media container, e.g. "video/mp4"
     *   or "video/webm"
     */
    public static final boolean isCryptoSchemeSupported(
            @NonNull UUID uuid, @NonNull String mimeType) {
        return isCryptoSchemeSupportedNative(getByteArrayFromUUID(uuid),
                mimeType, SECURITY_LEVEL_UNKNOWN);
    }

    /**
     * Query if the given scheme identified by its UUID is supported on
     * this device, and whether the DRM plugin is able to handle the
     * media container format specified by mimeType at the requested
     * security level.
     *
     * Calling this method while the application is running on the physical Android device or a
     * {@link android.companion.virtual.VirtualDevice} may lead to different results, based on
     * the different DRM capabilities of the devices.
     *
     * @param uuid The UUID of the crypto scheme.
     * @param mimeType The MIME type of the media container, e.g. "video/mp4"
     *   or "video/webm"
     * @param securityLevel the security level requested
     */
    public static final boolean isCryptoSchemeSupported(
            @NonNull UUID uuid, @NonNull String mimeType, @SecurityLevel int securityLevel) {
        return isCryptoSchemeSupportedNative(getByteArrayFromUUID(uuid), mimeType,
                securityLevel);
    }

    /**
     * @return list of crypto schemes (as {@link UUID}s) for which
     * {@link #isCryptoSchemeSupported(UUID)} returns true; each {@link UUID}
     * can be used as input to create {@link MediaDrm} objects via {@link #MediaDrm(UUID)}.
     */
    public static final @NonNull List<UUID> getSupportedCryptoSchemes(){
        byte[] uuidBytes = getSupportedCryptoSchemesNative();
        return getUUIDsFromByteArray(uuidBytes);
    }

    private static final byte[] getByteArrayFromUUID(@NonNull UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        byte[] uuidBytes = new byte[16];
        for (int i = 0; i < 8; ++i) {
            uuidBytes[i] = (byte)(msb >>> (8 * (7 - i)));
            uuidBytes[8 + i] = (byte)(lsb >>> (8 * (7 - i)));
        }

        return uuidBytes;
    }

    private static final UUID getUUIDFromByteArray(@NonNull byte[] uuidBytes, int off) {
        long msb = 0;
        long lsb = 0;

        for (int i = 0; i < 8; ++i) {
            msb = (msb << 8) | (0xffl & uuidBytes[off + i]);
            lsb = (lsb << 8) | (0xffl & uuidBytes[off + i + 8]);
        }

        return new UUID(msb, lsb);
    }

    private static final List<UUID> getUUIDsFromByteArray(@NonNull byte[] uuidBytes) {
        Set<UUID> uuids = new LinkedHashSet<>();
        for (int off = 0; off < uuidBytes.length; off+=16) {
            uuids.add(getUUIDFromByteArray(uuidBytes, off));
        }
        return new ArrayList<>(uuids);
    }

    private static final native byte[] getSupportedCryptoSchemesNative();

    private static final native boolean isCryptoSchemeSupportedNative(
            @NonNull byte[] uuid, @Nullable String mimeType, @SecurityLevel int securityLevel);

    private Handler createHandler() {
        Looper looper;
        Handler handler;
        if ((looper = Looper.myLooper()) != null) {
            handler = new Handler(looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            handler = new Handler(looper);
        } else {
            handler = null;
        }
        return handler;
    }

    /**
     * Instantiate a MediaDrm object
     *
     * @param uuid The UUID of the crypto scheme.
     *
     * @throws UnsupportedSchemeException if the device does not support the
     * specified scheme UUID
     */
    public MediaDrm(@NonNull UUID uuid) throws UnsupportedSchemeException {
        /* Native setup requires a weak reference to our object.
         * It's easier to create it here than in C++.
         */
        mAppPackageName = ActivityThread.currentOpPackageName();
        native_setup(new WeakReference<MediaDrm>(this),
                getByteArrayFromUUID(uuid), mAppPackageName);

        mCloseGuard.open("release");
    }

    /**
     * Error codes that may be returned from {@link
     * MediaDrmStateException#getErrorCode()} and {@link
     * MediaCodec.CryptoException#getErrorCode()}
     * <p>
     * The description of each error code includes steps that may be taken to
     * resolve the error condition. For some errors however, a recovery action
     * cannot be predetermined. The description of those codes refers to a
     * general strategy for handling the error condition programmatically, which
     * is to try the following in listed order until successful:
     * <ol>
     * <li> retry the operation </li>
     * <li> if the operation is related to a session, {@link
     * #closeSession(byte[]) close} the session, {@link #openSession() open} a
     * new session, and retry the operation </li>
     * <li> {@link #close() close} the {@link MediaDrm} instance and any other
     * related components such as the {@link MediaCodec codec} and retry
     * playback, or </li>
     * <li> try using a different configuration of the {@link MediaDrm} plugin,
     * such as a different {@link #openSession(int) security level}. </li>
     * </ol>
     * <p>
     * If the problem still persists after all the aforementioned steps, please
     * report the failure to the {@link MediaDrm} plugin vendor along with the
     * {@link LogMessage log messages} returned by {@link
     * MediaDrm#getLogMessages()}, and a bugreport if possible.
     */
    public final static class ErrorCodes {
        private ErrorCodes() {}

        /**
         * ERROR_UNKNOWN is used where no other defined error code is applicable
         * to the current failure.
         * <p>
         * Please see the general error handling strategy for unexpected errors
         * described in {@link ErrorCodes}.
         */
        public static final int ERROR_UNKNOWN = 0;

        /**
         * The requested key was not found when trying to perform a decrypt
         * operation.
         * <p>
         * The operation can be retried after adding the correct decryption key.
         */
        public static final int ERROR_NO_KEY = 1;

        /**
         * The key used for decryption is no longer valid due to license term
         * expiration.
         * <p>
         * The operation can be retried after updating the expired keys.
         */
        public static final int ERROR_KEY_EXPIRED = 2;

        /**
         * A required crypto resource was not able to be allocated while
         * attempting the requested operation.
         * <p>
         * The operation can be retried if the app is able to release resources.
         */
        public static final int ERROR_RESOURCE_BUSY = 3;

        /**
         * The output protection levels supported by the device are not
         * sufficient to meet the requirements set by the content owner in the
         * license policy.
         */
        public static final int ERROR_INSUFFICIENT_OUTPUT_PROTECTION = 4;

        /**
         * Decryption was attempted on a session that is not opened, which could
         * be due to a failure to open the session, closing the session
         * prematurely, the session being reclaimed by the resource manager, or
         * a non-existent session id.
         */
        public static final int ERROR_SESSION_NOT_OPENED = 5;

        /**
         * An operation was attempted that could not be supported by the crypto
         * system of the device in its current configuration.
         * <p>
         * This may occur when the license policy requires device security
         * features that aren't supported by the device, or due to an internal
         * error in the crypto system that prevents the specified security
         * policy from being met.
         */
        public static final int ERROR_UNSUPPORTED_OPERATION = 6;

        /**
         * The security level of the device is not sufficient to meet the
         * requirements set by the content owner in the license policy.
         */
        public static final int ERROR_INSUFFICIENT_SECURITY = 7;

        /**
         * The video frame being decrypted exceeds the size of the device's
         * protected output buffers.
         * <p>
         * When encountering this error the app should try playing content
         * of a lower resolution or skipping the problematic frame.
         */
        public static final int ERROR_FRAME_TOO_LARGE = 8;

        /**
         * The session state has been invalidated. This can occur on devices
         * that are not capable of retaining crypto session state across device
         * suspend/resume.
         * <p>
         * The session must be closed and a new session opened to resume
         * operation.
         */
        public static final int ERROR_LOST_STATE = 9;

        /**
         * Certificate is malformed or is of the wrong type.
         * <p>
         * Ensure the certificate provided by the app or returned from the
         * license server is valid. Check with the {@link MediaDrm} plugin
         * vendor for the expected certificate format.
         */
        public static final int ERROR_CERTIFICATE_MALFORMED = 10;

        /**
         * Certificate has not been set.
         * <p>
         * Ensure the certificate has been provided by the app. Check with the
         * {@link MediaDrm} plugin vendor for the expected method to provide
         * {@link MediaDrm} a certificate.
         */
        public static final int ERROR_CERTIFICATE_MISSING = 11;

        /**
         * An error happened within the crypto library used by the drm plugin.
         */
        public static final int ERROR_CRYPTO_LIBRARY = 12;

        /**
         * Unexpected error reported by the device OEM subsystem.
         * <p>
         * Please see the general error handling strategy for unexpected errors
         * described in {@link ErrorCodes}.
         */
        public static final int ERROR_GENERIC_OEM = 13;

        /**
         * Unexpected internal failure in {@link MediaDrm}/{@link MediaCrypto}.
         * <p>
         * Please see the general error handling strategy for unexpected errors
         * described in {@link ErrorCodes}.
         */
        public static final int ERROR_GENERIC_PLUGIN = 14;

        /**
         * The init data parameter passed to {@link MediaDrm#getKeyRequest} is
         * empty or invalid.
         * <p>
         * Init data is typically obtained from {@link
         * MediaExtractor#getPsshInfo()} or {@link
         * MediaExtractor#getDrmInitData()}. Check with the {@link MediaDrm}
         * plugin vendor for the expected init data format.
         */
        public static final int ERROR_INIT_DATA = 15;

        /**
         * Either the key was not loaded from the license before attempting the
         * operation, or the key ID parameter provided by the app is incorrect.
         * <p>
         * Ensure the proper keys are in the license, and check the key ID
         * parameter provided by the app is correct. Check with the {@link
         * MediaDrm} plugin vendor for the expected license format.
         */
        public static final int ERROR_KEY_NOT_LOADED = 16;

        /**
         * The license response was empty, fields are missing or otherwise
         * unable to be parsed or decrypted.
         * <p>
         * Check for mistakes such as empty or overwritten buffers. Otherwise,
         * check with the {@link MediaDrm} plugin vendor for the expected
         * license format.
         */
        public static final int ERROR_LICENSE_PARSE = 17;

        /**
         * The operation (e.g. to renew or persist a license) is prohibited by
         * the license policy.
         * <p>
         * Check the license policy configuration on the license server.
         */
        public static final int ERROR_LICENSE_POLICY = 18;

        /**
         * Failed to generate a release request because a field in the offline
         * license is empty or malformed.
         * <p>
         * The license can't be released on the server, but the app may remove
         * the offline license explicitly using {@link
         * MediaDrm#removeOfflineLicense}.
         */
        public static final int ERROR_LICENSE_RELEASE = 19;

        /**
         * The license server detected an error in the license request.
         * <p>
         * Check for errors on the license server.
         */
        public static final int ERROR_LICENSE_REQUEST_REJECTED = 20;

        /**
         * Failed to restore an offline license because a field in the offline
         * license is empty or malformed.
         * <p>
         * Try requesting the license again if the device is online.
         */
        public static final int ERROR_LICENSE_RESTORE = 21;

        /**
         * Offline license is in an invalid state for the attempted operation.
         * <p>
         * Check the sequence of API calls made that can affect offline license
         * state. For example, this could happen when the app attempts to
         * restore a license after it has been released.
         */
        public static final int ERROR_LICENSE_STATE = 22;

        /**
         * Failure in the media framework.
         * <p>
         * Try releasing media resources (e.g. {@link MediaCodec}, {@link
         * MediaDrm}), and restarting playback.
         */
        public static final int ERROR_MEDIA_FRAMEWORK = 23;

        /**
         * Error loading the provisioned certificate.
         * <p>
         * Re-provisioning may resolve the problem; check with the {@link
         * MediaDrm} plugin vendor for re-provisioning instructions. Otherwise,
         * using a different security level may resolve the issue.
         */
        public static final int ERROR_PROVISIONING_CERTIFICATE = 24;

        /**
         * Required steps were not performed before provisioning was attempted.
         * <p>
         * Ask the {@link MediaDrm} plugin vendor for situations where this
         * error may occur.
         */
        public static final int ERROR_PROVISIONING_CONFIG = 25;

        /**
         * The provisioning response was empty, fields are missing or otherwise
         * unable to be parsed.
         * <p>
         * Check for mistakes such as empty or overwritten buffers. Otherwise,
         * check with the {@link MediaDrm} plugin vendor for the expected
         * provisioning response format.
         */
        public static final int ERROR_PROVISIONING_PARSE = 26;

        /**
         * The provisioning server detected an error in the provisioning
         * request.
         * <p>
         * Check for errors on the provisioning server.
         */
        public static final int ERROR_PROVISIONING_REQUEST_REJECTED = 27;

        /**
         * Provisioning failed in a way that is likely to succeed on a
         * subsequent attempt.
         * <p>
         * The app should retry the operation.
         */
        public static final int ERROR_PROVISIONING_RETRY = 28;

        /**
         * This indicates that apps using MediaDrm sessions are
         * temporarily exceeding the capacity of available crypto
         * resources.
         * <p>
         * The app should retry the operation later.
         */
        public static final int ERROR_RESOURCE_CONTENTION = 29;

        /**
         * Failed to generate a secure stop request because a field in the
         * stored license is empty or malformed.
         * <p>
         * The secure stop can't be released on the server, but the app may
         * remove it explicitly using {@link MediaDrm#removeSecureStop}.
         */
        public static final int ERROR_SECURE_STOP_RELEASE = 30;

        /**
         * The plugin was unable to read data from the filesystem.
         * <p>
         * Please see the general error handling strategy for unexpected errors
         * described in {@link ErrorCodes}.
         */
        public static final int ERROR_STORAGE_READ = 31;

        /**
         * The plugin was unable to write data to the filesystem.
         * <p>
         * Please see the general error handling strategy for unexpected errors
         * described in {@link ErrorCodes}.
         */
        public static final int ERROR_STORAGE_WRITE = 32;

        /**
         * {@link MediaCodec#queueSecureInputBuffer} called with 0 subsamples.
         * <p>
         * Check the {@link MediaCodec.CryptoInfo} object passed to {@link
         * MediaCodec#queueSecureInputBuffer}.
         */
        public static final int ERROR_ZERO_SUBSAMPLES = 33;

    }

    /** @hide */
    @IntDef({
        ErrorCodes.ERROR_NO_KEY,
        ErrorCodes.ERROR_KEY_EXPIRED,
        ErrorCodes.ERROR_RESOURCE_BUSY,
        ErrorCodes.ERROR_INSUFFICIENT_OUTPUT_PROTECTION,
        ErrorCodes.ERROR_SESSION_NOT_OPENED,
        ErrorCodes.ERROR_UNSUPPORTED_OPERATION,
        ErrorCodes.ERROR_INSUFFICIENT_SECURITY,
        ErrorCodes.ERROR_FRAME_TOO_LARGE,
        ErrorCodes.ERROR_LOST_STATE,
        ErrorCodes.ERROR_CERTIFICATE_MALFORMED,
        ErrorCodes.ERROR_CERTIFICATE_MISSING,
        ErrorCodes.ERROR_CRYPTO_LIBRARY,
        ErrorCodes.ERROR_GENERIC_OEM,
        ErrorCodes.ERROR_GENERIC_PLUGIN,
        ErrorCodes.ERROR_INIT_DATA,
        ErrorCodes.ERROR_KEY_NOT_LOADED,
        ErrorCodes.ERROR_LICENSE_PARSE,
        ErrorCodes.ERROR_LICENSE_POLICY,
        ErrorCodes.ERROR_LICENSE_RELEASE,
        ErrorCodes.ERROR_LICENSE_REQUEST_REJECTED,
        ErrorCodes.ERROR_LICENSE_RESTORE,
        ErrorCodes.ERROR_LICENSE_STATE,
        ErrorCodes.ERROR_MEDIA_FRAMEWORK,
        ErrorCodes.ERROR_PROVISIONING_CERTIFICATE,
        ErrorCodes.ERROR_PROVISIONING_CONFIG,
        ErrorCodes.ERROR_PROVISIONING_PARSE,
        ErrorCodes.ERROR_PROVISIONING_REQUEST_REJECTED,
        ErrorCodes.ERROR_PROVISIONING_RETRY,
        ErrorCodes.ERROR_SECURE_STOP_RELEASE,
        ErrorCodes.ERROR_STORAGE_READ,
        ErrorCodes.ERROR_STORAGE_WRITE,
        ErrorCodes.ERROR_ZERO_SUBSAMPLES
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaDrmErrorCode {}

    /**
     * Thrown when a general failure occurs during a MediaDrm operation.
     * Extends {@link IllegalStateException} with the addition of an error
     * code that may be useful in diagnosing the failure.
     * <p>
     * Please refer to {@link ErrorCodes} for the general error handling
     * strategy and details about each possible return value from {@link
     * MediaDrmStateException#getErrorCode()}.
     */
    public static final class MediaDrmStateException extends java.lang.IllegalStateException
            implements MediaDrmThrowable {
        private final int mErrorCode, mVendorError, mOemError, mErrorContext;
        private final String mDiagnosticInfo;

        /**
         * @hide
         */
        public MediaDrmStateException(int errorCode, @Nullable String detailMessage) {
            this(detailMessage, errorCode, 0, 0, 0);
        }

        /**
         * @hide
         */
        public MediaDrmStateException(String detailMessage, int errorCode,
                int vendorError, int oemError, int errorContext) {
            super(detailMessage);
            mErrorCode = errorCode;
            mVendorError = vendorError;
            mOemError = oemError;
            mErrorContext = errorContext;

            // TODO get this from DRM session
            final String sign = errorCode < 0 ? "neg_" : "";
            mDiagnosticInfo =
                    "android.media.MediaDrm.error_" + sign + Math.abs(errorCode);

        }

        /**
         * Returns error code associated with this {@link
         * MediaDrmStateException}.
         * <p>
         * Please refer to {@link ErrorCodes} for the general error handling
         * strategy and details about each possible return value.
         *
         * @return an error code defined in {@link MediaDrm.ErrorCodes}.
         */
        @MediaDrmErrorCode
        public int getErrorCode() {
            return mErrorCode;
        }

        @Override
        public int getVendorError() {
            return mVendorError;
        }

        @Override
        public int getOemError() {
            return mOemError;
        }

        @Override
        public int getErrorContext() {
            return mErrorContext;
        }

        /**
         * Returns true if the {@link MediaDrmStateException} is a transient
         * issue, perhaps due to resource constraints, and that the operation
         * (e.g. provisioning) may succeed on a subsequent attempt.
         */
        public boolean isTransient() {
            return mErrorCode == ErrorCodes.ERROR_PROVISIONING_RETRY
                    || mErrorCode == ErrorCodes.ERROR_RESOURCE_CONTENTION;
        }

        /**
         * Retrieve a developer-readable diagnostic information string
         * associated with the exception. Do not show this to end-users,
         * since this string will not be localized or generally comprehensible
         * to end-users.
         */
        @NonNull
        public String getDiagnosticInfo() {
            return mDiagnosticInfo;
        }
    }

    /**
     * {@link SessionException} is a misnomer because it may occur in methods
     * <b>without</b> a session context.
     * <p>
     * A {@link SessionException} is most likely to be thrown when an operation
     * failed in a way that is likely to succeed on a subsequent attempt; call
     * {@link #isTransient()} to determine whether the app should retry the
     * failing operation.
     */
    public static final class SessionException extends RuntimeException
            implements MediaDrmThrowable {
        public SessionException(int errorCode, @Nullable String detailMessage) {
            this(detailMessage, errorCode, 0, 0, 0);
        }

        /**
         * @hide
         */
        public SessionException(String detailMessage, int errorCode, int vendorError, int oemError,
                int errorContext) {
            super(detailMessage);
            mErrorCode = errorCode;
            mVendorError = vendorError;
            mOemError = oemError;
            mErrorContext = errorContext;
        }

        /**
         * The SessionException has an unknown error code.
         * @deprecated Unused.
         */
        public static final int ERROR_UNKNOWN = 0;

        /**
         * This indicates that apps using MediaDrm sessions are
         * temporarily exceeding the capacity of available crypto
         * resources. The app should retry the operation later.
         *
         * @deprecated Please use {@link #isTransient()} instead of comparing
         * the return value of {@link #getErrorCode()} against
         * {@link SessionException#ERROR_RESOURCE_CONTENTION}.
         */
        public static final int ERROR_RESOURCE_CONTENTION = 1;

        /** @hide */
        @IntDef({
            ERROR_RESOURCE_CONTENTION,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface SessionErrorCode {}

        /**
         * Retrieve the error code associated with the SessionException
         *
         * @deprecated Please use {@link #isTransient()} instead of comparing
         * the return value of {@link #getErrorCode()} against
         * {@link SessionException#ERROR_RESOURCE_CONTENTION}.
         */
        @SessionErrorCode
        public int getErrorCode() {
            return mErrorCode;
        }

        @Override
        public int getVendorError() {
            return mVendorError;
        }

        @Override
        public int getOemError() {
            return mOemError;
        }

        @Override
        public int getErrorContext() {
            return mErrorContext;
        }

        /**
         * Returns true if the {@link SessionException} is a transient
         * issue, perhaps due to resource constraints, and that the operation
         * (e.g. provisioning, generating requests) may succeed on a subsequent
         * attempt.
         */
        public boolean isTransient() {
            return mErrorCode == ERROR_RESOURCE_CONTENTION;
        }

        private final int mErrorCode, mVendorError, mOemError, mErrorContext;
    }

    /**
     * Register a callback to be invoked when a session expiration update
     * occurs.  The app's OnExpirationUpdateListener will be notified
     * when the expiration time of the keys in the session have changed.
     * @param listener the callback that will be run, or {@code null} to unregister the
     *     previously registered callback.
     * @param handler the handler on which the listener should be invoked, or
     *     {@code null} if the listener should be invoked on the calling thread's looper.
     */
    public void setOnExpirationUpdateListener(
            @Nullable OnExpirationUpdateListener listener, @Nullable Handler handler) {
        setListenerWithHandler(EXPIRATION_UPDATE, handler, listener,
                this::createOnExpirationUpdateListener);
    }
    /**
     * Register a callback to be invoked when a session expiration update
     * occurs.
     *
     * @see #setOnExpirationUpdateListener(OnExpirationUpdateListener, Handler)
     *
     * @param executor the executor through which the listener should be invoked
     * @param listener the callback that will be run.
     */
    public void setOnExpirationUpdateListener(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OnExpirationUpdateListener listener) {
        setListenerWithExecutor(EXPIRATION_UPDATE, executor, listener,
                this::createOnExpirationUpdateListener);
    }

    /**
     * Clear the {@link OnExpirationUpdateListener}.
     */
    public void clearOnExpirationUpdateListener() {
        clearGenericListener(EXPIRATION_UPDATE);
    }

    /**
     * Interface definition for a callback to be invoked when a drm session
     * expiration update occurs
     */
    public interface OnExpirationUpdateListener
    {
        /**
         * Called when a session expiration update occurs, to inform the app
         * about the change in expiration time
         *
         * @param md the MediaDrm object on which the event occurred
         * @param sessionId the DRM session ID on which the event occurred
         * @param expirationTime the new expiration time for the keys in the session.
         *     The time is in milliseconds, relative to the Unix epoch.  A time of
         *     0 indicates that the keys never expire.
         */
        void onExpirationUpdate(
                @NonNull MediaDrm md, @NonNull byte[] sessionId, long expirationTime);
    }

    /**
     * Register a callback to be invoked when the state of keys in a session
     * change, e.g. when a license update occurs or when a license expires.
     *
     * @param listener the callback that will be run when key status changes, or
     *     {@code null} to unregister the previously registered callback.
     * @param handler the handler on which the listener should be invoked, or
     *     null if the listener should be invoked on the calling thread's looper.
     */
    public void setOnKeyStatusChangeListener(
            @Nullable OnKeyStatusChangeListener listener, @Nullable Handler handler) {
        setListenerWithHandler(KEY_STATUS_CHANGE, handler, listener,
                this::createOnKeyStatusChangeListener);
    }

    /**
     * Register a callback to be invoked when the state of keys in a session
     * change.
     *
     * @see #setOnKeyStatusChangeListener(OnKeyStatusChangeListener, Handler)
     *
     * @param listener the callback that will be run when key status changes.
     * @param executor the executor on which the listener should be invoked.
     */
    public void setOnKeyStatusChangeListener(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OnKeyStatusChangeListener listener) {
        setListenerWithExecutor(KEY_STATUS_CHANGE, executor, listener,
                this::createOnKeyStatusChangeListener);
    }

    /**
     * Clear the {@link OnKeyStatusChangeListener}.
     */
    public void clearOnKeyStatusChangeListener() {
        clearGenericListener(KEY_STATUS_CHANGE);
    }

    /**
     * Interface definition for a callback to be invoked when the keys in a drm
     * session change states.
     */
    public interface OnKeyStatusChangeListener
    {
        /**
         * Called when the keys in a session change status, such as when the license
         * is renewed or expires.
         *
         * @param md the MediaDrm object on which the event occurred
         * @param sessionId the DRM session ID on which the event occurred
         * @param keyInformation a list of {@link MediaDrm.KeyStatus}
         *     instances indicating the status for each key in the session
         * @param hasNewUsableKey indicates if a key has been added that is usable,
         *     which may trigger an attempt to resume playback on the media stream
         *     if it is currently blocked waiting for a key.
         */
        void onKeyStatusChange(
                @NonNull MediaDrm md, @NonNull byte[] sessionId,
                @NonNull List<KeyStatus> keyInformation,
                boolean hasNewUsableKey);
    }

    /**
     * Register a callback to be invoked when session state has been
     * lost. This event can occur on devices that are not capable of
     * retaining crypto session state across device suspend/resume
     * cycles.  When this event occurs, the session must be closed and
     * a new session opened to resume operation.
     *
     * @param listener the callback that will be run, or {@code null} to unregister the
     *     previously registered callback.
     * @param handler the handler on which the listener should be invoked, or
     *     {@code null} if the listener should be invoked on the calling thread's looper.
     */
    public void setOnSessionLostStateListener(
            @Nullable OnSessionLostStateListener listener, @Nullable Handler handler) {
        setListenerWithHandler(SESSION_LOST_STATE, handler, listener,
                this::createOnSessionLostStateListener);
    }

    /**
     * Register a callback to be invoked when session state has been
     * lost.
     *
     * @see #setOnSessionLostStateListener(OnSessionLostStateListener, Handler)
     *
     * @param listener the callback that will be run.
     * @param executor the executor on which the listener should be invoked.
     */
    public void setOnSessionLostStateListener(
            @NonNull @CallbackExecutor Executor executor,
            @Nullable OnSessionLostStateListener listener) {
        setListenerWithExecutor(SESSION_LOST_STATE, executor, listener,
                this::createOnSessionLostStateListener);
    }

    /**
     * Clear the {@link OnSessionLostStateListener}.
     */
    public void clearOnSessionLostStateListener() {
        clearGenericListener(SESSION_LOST_STATE);
    }

    /**
     * Interface definition for a callback to be invoked when the
     * session state has been lost and is now invalid
     */
    public interface OnSessionLostStateListener
    {
        /**
         * Called when session state has lost state, to inform the app
         * about the condition so it can close the session and open a new
         * one to resume operation.
         *
         * @param md the MediaDrm object on which the event occurred
         * @param sessionId the DRM session ID on which the event occurred
         */
        void onSessionLostState(
                @NonNull MediaDrm md, @NonNull byte[] sessionId);
    }

    /**
     * Defines the status of a key.
     * A KeyStatus for each key in a session is provided to the
     * {@link OnKeyStatusChangeListener#onKeyStatusChange}
     * listener.
     */
    public static final class KeyStatus {
        private final byte[] mKeyId;
        private final int mStatusCode;

        /**
         * The key is currently usable to decrypt media data
         */
        public static final int STATUS_USABLE = 0;

        /**
         * The key is no longer usable to decrypt media data because its
         * expiration time has passed.
         */
        public static final int STATUS_EXPIRED = 1;

        /**
         * The key is not currently usable to decrypt media data because its
         * output requirements cannot currently be met.
         */
        public static final int STATUS_OUTPUT_NOT_ALLOWED = 2;

        /**
         * The status of the key is not yet known and is being determined.
         * The status will be updated with the actual status when it has
         * been determined.
         */
        public static final int STATUS_PENDING = 3;

        /**
         * The key is not currently usable to decrypt media data because of an
         * internal error in processing unrelated to input parameters.  This error
         * is not actionable by an app.
         */
        public static final int STATUS_INTERNAL_ERROR = 4;

        /**
         * The key is not yet usable to decrypt media because the start
         * time is in the future. The key will become usable when
         * its start time is reached.
         */
        public static final int STATUS_USABLE_IN_FUTURE = 5;

        /** @hide */
        @IntDef({
            STATUS_USABLE,
            STATUS_EXPIRED,
            STATUS_OUTPUT_NOT_ALLOWED,
            STATUS_PENDING,
            STATUS_INTERNAL_ERROR,
            STATUS_USABLE_IN_FUTURE,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface KeyStatusCode {}

        KeyStatus(@NonNull byte[] keyId, @KeyStatusCode int statusCode) {
            mKeyId = keyId;
            mStatusCode = statusCode;
        }

        /**
         * Returns the status code for the key
         */
        @KeyStatusCode
        public int getStatusCode() { return mStatusCode; }

        /**
         * Returns the id for the key
         */
        @NonNull
        public byte[] getKeyId() { return mKeyId; }
    }

    /**
     * Register a callback to be invoked when an event occurs
     *
     * @see #setOnEventListener(OnEventListener, Handler)
     *
     * @param listener the callback that will be run.  Use {@code null} to
     *        stop receiving event callbacks.
     */
    public void setOnEventListener(@Nullable OnEventListener listener)
    {
        setOnEventListener(listener, null);
    }

    /**
     * Register a callback to be invoked when an event occurs
     *
     * @param listener the callback that will be run.  Use {@code null} to
     *        stop receiving event callbacks.
     * @param handler the handler on which the listener should be invoked, or
     *        null if the listener should be invoked on the calling thread's looper.
     */

    public void setOnEventListener(@Nullable OnEventListener listener, @Nullable Handler handler)
    {
        setListenerWithHandler(DRM_EVENT, handler, listener, this::createOnEventListener);
    }

    /**
     * Register a callback to be invoked when an event occurs
     *
     * @see #setOnEventListener(OnEventListener)
     *
     * @param executor the executor through which the listener should be invoked
     * @param listener the callback that will be run.
     */
    public void setOnEventListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull OnEventListener listener) {
        setListenerWithExecutor(DRM_EVENT, executor, listener, this::createOnEventListener);
    }

    /**
     * Clear the {@link OnEventListener}.
     */
    public void clearOnEventListener() {
        clearGenericListener(DRM_EVENT);
    }

    /**
     * Interface definition for a callback to be invoked when a drm event
     * occurs
     */
    public interface OnEventListener
    {
        /**
         * Called when an event occurs that requires the app to be notified
         *
         * @param md the MediaDrm object on which the event occurred
         * @param sessionId the DRM session ID on which the event occurred,
         *        or {@code null} if there is no session ID associated with the event.
         * @param event indicates the event type
         * @param extra an secondary error code
         * @param data optional byte array of data that may be associated with the event
         */
        void onEvent(
                @NonNull MediaDrm md, @Nullable byte[] sessionId,
                @DrmEvent int event, int extra,
                @Nullable byte[] data);
    }

    /**
     * This event type indicates that the app needs to request a certificate from
     * the provisioning server.  The request message data is obtained using
     * {@link #getProvisionRequest}
     *
     * @deprecated Handle provisioning via {@link android.media.NotProvisionedException}
     * instead.
     */
    public static final int EVENT_PROVISION_REQUIRED = 1;

    /**
     * This event type indicates that the app needs to request keys from a license
     * server.  The request message data is obtained using {@link #getKeyRequest}.
     */
    public static final int EVENT_KEY_REQUIRED = 2;

    /**
     * This event type indicates that the licensed usage duration for keys in a session
     * has expired.  The keys are no longer valid.
     * @deprecated Use {@link OnKeyStatusChangeListener#onKeyStatusChange}
     * and check for {@link MediaDrm.KeyStatus#STATUS_EXPIRED} in the {@link MediaDrm.KeyStatus}
     * instead.
     */
    public static final int EVENT_KEY_EXPIRED = 3;

    /**
     * This event may indicate some specific vendor-defined condition, see your
     * DRM provider documentation for details
     */
    public static final int EVENT_VENDOR_DEFINED = 4;

    /**
     * This event indicates that a session opened by the app has been reclaimed by the resource
     * manager.
     */
    public static final int EVENT_SESSION_RECLAIMED = 5;

    /** @hide */
    @IntDef({
        EVENT_PROVISION_REQUIRED,
        EVENT_KEY_REQUIRED,
        EVENT_KEY_EXPIRED,
        EVENT_VENDOR_DEFINED,
        EVENT_SESSION_RECLAIMED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DrmEvent {}

    private static final int DRM_EVENT = 200;
    private static final int EXPIRATION_UPDATE = 201;
    private static final int KEY_STATUS_CHANGE = 202;
    private static final int SESSION_LOST_STATE = 203;

    // Use ConcurrentMap to support concurrent read/write to listener settings.
    // ListenerWithExecutor is immutable so we shouldn't need further locks.
    private final Map<Integer, ListenerWithExecutor> mListenerMap = new ConcurrentHashMap<>();

    // called by old-style set*Listener APIs using Handlers; listener & handler are Nullable
    private <T> void setListenerWithHandler(int what, Handler handler, T listener,
            Function<T, Consumer<ListenerArgs>> converter) {
        if (listener == null) {
            clearGenericListener(what);
        } else {
            handler = handler == null ? createHandler() : handler;
            final HandlerExecutor executor = new HandlerExecutor(handler);
            setGenericListener(what, executor, listener, converter);
        }
    }

    // called by new-style set*Listener APIs using Executors; listener & executor must be NonNull
    private <T> void setListenerWithExecutor(int what, Executor executor, T listener,
            Function<T, Consumer<ListenerArgs>> converter) {
        if (executor == null || listener == null) {
            final String errMsg = String.format("executor %s listener %s", executor, listener);
            throw new IllegalArgumentException(errMsg);
        }
        setGenericListener(what, executor, listener, converter);
    }

    private <T> void setGenericListener(int what, Executor executor, T listener,
            Function<T, Consumer<ListenerArgs>> converter) {
        mListenerMap.put(what, new ListenerWithExecutor(executor, converter.apply(listener)));
    }

    private void clearGenericListener(int what) {
        mListenerMap.remove(what);
    }

    private Consumer<ListenerArgs> createOnEventListener(OnEventListener listener) {
        return args -> {
            byte[] sessionId = args.sessionId;
            if (sessionId.length == 0) {
                sessionId = null;
            }
            byte[] data = args.data;
            if (data != null && data.length == 0) {
                data = null;
            }

            Log.i(TAG, "Drm event (" + args.arg1 + "," + args.arg2 + ")");
            listener.onEvent(this, sessionId, args.arg1, args.arg2, data);
        };
    }

    private Consumer<ListenerArgs> createOnKeyStatusChangeListener(
            OnKeyStatusChangeListener listener) {
        return args -> {
            byte[] sessionId = args.sessionId;
            if (sessionId.length > 0) {
                List<KeyStatus> keyStatusList = args.keyStatusList;
                boolean hasNewUsableKey = args.hasNewUsableKey;

                Log.i(TAG, "Drm key status changed");
                listener.onKeyStatusChange(this, sessionId, keyStatusList, hasNewUsableKey);
            }
        };
    }

    private Consumer<ListenerArgs> createOnExpirationUpdateListener(
            OnExpirationUpdateListener listener) {
        return args -> {
            byte[] sessionId = args.sessionId;
            if (sessionId.length > 0) {
                long expirationTime = args.expirationTime;

                Log.i(TAG, "Drm key expiration update: " + expirationTime);
                listener.onExpirationUpdate(this, sessionId, expirationTime);
            }
        };
    }

    private Consumer<ListenerArgs> createOnSessionLostStateListener(
            OnSessionLostStateListener listener) {
        return args -> {
            byte[] sessionId = args.sessionId;
            Log.i(TAG, "Drm session lost state event: ");
            listener.onSessionLostState(this, sessionId);
        };
    }

    private static class ListenerArgs {
        private final int arg1;
        private final int arg2;
        private final byte[] sessionId;
        private final byte[] data;
        private final long expirationTime;
        private final List<KeyStatus> keyStatusList;
        private final boolean hasNewUsableKey;

        public ListenerArgs(
                int arg1,
                int arg2,
                byte[] sessionId,
                byte[] data,
                long expirationTime,
                List<KeyStatus> keyStatusList,
                boolean hasNewUsableKey) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.sessionId = sessionId;
            this.data = data;
            this.expirationTime = expirationTime;
            this.keyStatusList = keyStatusList;
            this.hasNewUsableKey = hasNewUsableKey;
        }

    }

    private static class ListenerWithExecutor {
        private final Consumer<ListenerArgs> mConsumer;
        private final Executor mExecutor;

        public ListenerWithExecutor(Executor executor, Consumer<ListenerArgs> consumer) {
            this.mExecutor = executor;
            this.mConsumer = consumer;
        }
    }

    /**
     * Parse a list of KeyStatus objects from an event parcel
     */
    @NonNull
    private List<KeyStatus> keyStatusListFromParcel(@NonNull Parcel parcel) {
        int nelems = parcel.readInt();
        List<KeyStatus> keyStatusList = new ArrayList(nelems);
        while (nelems-- > 0) {
            byte[] keyId = parcel.createByteArray();
            int keyStatusCode = parcel.readInt();
            keyStatusList.add(new KeyStatus(keyId, keyStatusCode));
        }
        return keyStatusList;
    }

    /**
     * This method is called from native code when an event occurs.  This method
     * just uses the EventHandler system to post the event back to the main app thread.
     * We use a weak reference to the original MediaPlayer object so that the native
     * code is safe from the object disappearing from underneath it.  (This is
     * the cookie passed to native_setup().)
     */
    private static void postEventFromNative(@NonNull Object mediadrm_ref,
            int what, int eventType, int extra,
            byte[] sessionId, byte[] data, long expirationTime,
            List<KeyStatus> keyStatusList, boolean hasNewUsableKey)
    {
        MediaDrm md = (MediaDrm)((WeakReference<MediaDrm>)mediadrm_ref).get();
        if (md == null) {
            return;
        }
        switch (what) {
            case DRM_EVENT:
            case EXPIRATION_UPDATE:
            case KEY_STATUS_CHANGE:
            case SESSION_LOST_STATE:
                ListenerWithExecutor listener  = md.mListenerMap.get(what);
                if (listener != null) {
                    final Runnable command = () -> {
                        if (md.mNativeContext == 0) {
                            Log.w(TAG, "MediaDrm went away with unhandled events");
                            return;
                        }
                        ListenerArgs args = new ListenerArgs(eventType, extra,
                                sessionId, data, expirationTime,
                                keyStatusList, hasNewUsableKey);
                        listener.mConsumer.accept(args);
                    };
                    listener.mExecutor.execute(command);
                }
                break;
            default:
                Log.e(TAG, "Unknown message type " + what);
                break;
        }
    }

    /**
     * Open a new session with the MediaDrm object. A session ID is returned.
     * By default, sessions are opened at the native security level of the device.
     *
     * If the application is currently running on a {@link android.companion.virtual.VirtualDevice}
     * the security level will be adjusted accordingly to the maximum supported level for the
     * display.
     *
     * @throws NotProvisionedException if provisioning is needed
     * @throws ResourceBusyException if required resources are in use
     */
    @NonNull
    public byte[] openSession() throws NotProvisionedException,
            ResourceBusyException {
        return openSession(getMaxSecurityLevel());
    }

    /**
     * Open a new session at a requested security level. The security level
     * represents the robustness of the device's DRM implementation. By default,
     * sessions are opened at the native security level of the device.
     * Overriding the security level is necessary when the decrypted frames need
     * to be manipulated, such as for image compositing. The security level
     * parameter must be lower than the native level. Reducing the security
     * level will typically limit the content to lower resolutions, as
     * determined by the license policy. If the requested level is not
     * supported, the next lower supported security level will be set. The level
     * can be queried using {@link #getSecurityLevel}. A session
     * ID is returned.
     *
     * If the application is currently running on a {@link android.companion.virtual.VirtualDevice}
     * the security level will be adjusted accordingly to the maximum supported level for the
     * display.
     *
     * @param level the new security level
     * @throws NotProvisionedException if provisioning is needed
     * @throws ResourceBusyException if required resources are in use
     * @throws IllegalArgumentException if the requested security level is
     * higher than the native level or lower than the lowest supported level or
     * if the device does not support specifying the security level when opening
     * a session
     */
    @NonNull
    public byte[] openSession(@SecurityLevel int level) throws
            NotProvisionedException, ResourceBusyException {
        byte[] sessionId = openSessionNative(level);
        mPlaybackComponentMap.put(ByteBuffer.wrap(sessionId), new PlaybackComponent(sessionId));
        return sessionId;
    }

    @NonNull
    private native byte[] openSessionNative(int level) throws
            NotProvisionedException, ResourceBusyException;

    /**
     * Close a session on the MediaDrm object that was previously opened
     * with {@link #openSession}.
     */
    public void closeSession(@NonNull byte[] sessionId) {
        closeSessionNative(sessionId);
        mPlaybackComponentMap.remove(ByteBuffer.wrap(sessionId));
    }

    private native void closeSessionNative(@NonNull byte[] sessionId);

    private final Map<ByteBuffer, PlaybackComponent> mPlaybackComponentMap
            = new ConcurrentHashMap<>();

    /**
     * This key request type species that the keys will be for online use, they will
     * not be saved to the device for subsequent use when the device is not connected
     * to a network.
     */
    public static final int KEY_TYPE_STREAMING = 1;

    /**
     * This key request type specifies that the keys will be for offline use, they
     * will be saved to the device for use when the device is not connected to a network.
     */
    public static final int KEY_TYPE_OFFLINE = 2;

    /**
     * This key request type specifies that previously saved offline keys should be released.
     */
    public static final int KEY_TYPE_RELEASE = 3;

    /** @hide */
    @IntDef({
        KEY_TYPE_STREAMING,
        KEY_TYPE_OFFLINE,
        KEY_TYPE_RELEASE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface KeyType {}

    /**
     * Contains the opaque data an app uses to request keys from a license server.
     * These request types may or may not be generated by a given plugin. Refer
     * to plugin vendor documentation for more information.
     */
    public static final class KeyRequest {
        private byte[] mData;
        private String mDefaultUrl;
        private int mRequestType;

        /**
         * Key request type is initial license request. A license request
         * is necessary to load keys.
         */
        public static final int REQUEST_TYPE_INITIAL = 0;

        /**
         * Key request type is license renewal. A license request is
         * necessary to prevent the keys from expiring.
         */
        public static final int REQUEST_TYPE_RENEWAL = 1;

        /**
         * Key request type is license release
         */
        public static final int REQUEST_TYPE_RELEASE = 2;

        /**
         * Keys are already loaded and are available for use. No license request is necessary, and
         * no key request data is returned.
         */
        public static final int REQUEST_TYPE_NONE = 3;

        /**
         * Keys have been loaded but an additional license request is needed
         * to update their values.
         */
        public static final int REQUEST_TYPE_UPDATE = 4;

        /** @hide */
        @IntDef({
            REQUEST_TYPE_INITIAL,
            REQUEST_TYPE_RENEWAL,
            REQUEST_TYPE_RELEASE,
            REQUEST_TYPE_NONE,
            REQUEST_TYPE_UPDATE,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface RequestType {}

        KeyRequest() {}

        /**
         * Get the opaque message data
         */
        @NonNull
        public byte[] getData() {
            if (mData == null) {
                // this should never happen as mData is initialized in
                // JNI after construction of the KeyRequest object. The check
                // is needed here to guarantee @NonNull annotation.
                throw new RuntimeException("KeyRequest is not initialized");
            }
            return mData;
        }

        /**
         * Get the default URL to use when sending the key request message to a
         * server, if known.  The app may prefer to use a different license
         * server URL from other sources.
         * This method returns an empty string if the default URL is not known.
         */
        @NonNull
        public String getDefaultUrl() {
            if (mDefaultUrl == null) {
                // this should never happen as mDefaultUrl is initialized in
                // JNI after construction of the KeyRequest object. The check
                // is needed here to guarantee @NonNull annotation.
                throw new RuntimeException("KeyRequest is not initialized");
            }
            return mDefaultUrl;
        }

        /**
         * Get the type of the request
         */
        @RequestType
        public int getRequestType() { return mRequestType; }
    };

    /**
     * A key request/response exchange occurs between the app and a license server
     * to obtain or release keys used to decrypt encrypted content.
     * <p>
     * getKeyRequest() is used to obtain an opaque key request byte array that is
     * delivered to the license server.  The opaque key request byte array is returned
     * in KeyRequest.data.  The recommended URL to deliver the key request to is
     * returned in KeyRequest.defaultUrl.
     * <p>
     * After the app has received the key request response from the server,
     * it should deliver to the response to the MediaDrm instance using the method
     * {@link #provideKeyResponse}.
     *
     * @param scope may be a sessionId or a keySetId, depending on the specified keyType.
     * When the keyType is KEY_TYPE_STREAMING or KEY_TYPE_OFFLINE,
     * scope should be set to the sessionId the keys will be provided to.  When the keyType
     * is KEY_TYPE_RELEASE, scope should be set to the keySetId of the keys
     * being released. Releasing keys from a device invalidates them for all sessions.
     * @param init container-specific data, its meaning is interpreted based on the
     * mime type provided in the mimeType parameter.  It could contain, for example,
     * the content ID, key ID or other data obtained from the content metadata that is
     * required in generating the key request. May be null when keyType is
     * KEY_TYPE_RELEASE or if the request is a renewal, i.e. not the first key
     * request for the session.
     * @param mimeType identifies the mime type of the content. May be null if the
     * keyType is KEY_TYPE_RELEASE or if the request is a renewal, i.e. not the
     * first key request for the session.
     * @param keyType specifes the type of the request. The request may be to acquire
     * keys for streaming or offline content, or to release previously acquired
     * keys, which are identified by a keySetId.
     * @param optionalParameters are included in the key request message to
     * allow a client application to provide additional message parameters to the server.
     * This may be {@code null} if no additional parameters are to be sent.
     * @throws NotProvisionedException if reprovisioning is needed, due to a
     * problem with the certifcate
     */
    @NonNull
    public KeyRequest getKeyRequest(
            @NonNull byte[] scope, @Nullable byte[] init,
            @Nullable String mimeType, @KeyType int keyType,
            @Nullable HashMap<String, String> optionalParameters)
            throws NotProvisionedException {
        HashMap<String, String> internalParams;
        if (optionalParameters == null) {
            internalParams = new HashMap<>();
        } else {
            internalParams = new HashMap<>(optionalParameters);
        }
        byte[] rawBytes = getNewestAvailablePackageCertificateRawBytes();
        byte[] hashBytes = null;
        if (rawBytes != null) {
            hashBytes = getDigestBytes(rawBytes, "SHA-256");
        }
        if (hashBytes != null) {
            Base64.Encoder encoderB64 = Base64.getEncoder();
            String hashBytesB64 = encoderB64.encodeToString(hashBytes);
            internalParams.put("package_certificate_hash_bytes", hashBytesB64);
        }
        return getKeyRequestNative(scope, init, mimeType, keyType, internalParams);
    }

    @Nullable
    private byte[] getNewestAvailablePackageCertificateRawBytes() {
        Application application = ActivityThread.currentApplication();
        if (application == null) {
            Log.w(TAG, "pkg cert: Application is null");
            return null;
        }
        PackageManager pm = application.getPackageManager();
        if (pm == null) {
            Log.w(TAG, "pkg cert: PackageManager is null");
            return null;
        }
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(mAppPackageName,
                    PackageManager.GET_SIGNING_CERTIFICATES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, mAppPackageName, e);
        }
        if (packageInfo == null || packageInfo.signingInfo == null) {
            Log.w(TAG, "pkg cert: PackageInfo or SigningInfo is null");
            return null;
        }
        Signature[] signers = packageInfo.signingInfo.getApkContentsSigners();
        if (signers != null && signers.length == 1) {
            return signers[0].toByteArray();
        }
        Log.w(TAG, "pkg cert: " + signers.length + " signers");
        return null;
    }

    @Nullable
    private static byte[] getDigestBytes(@NonNull byte[] rawBytes, @NonNull String algorithm) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            return messageDigest.digest(rawBytes);
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, algorithm, e);
        }
        return null;
    }

    @NonNull
    private native KeyRequest getKeyRequestNative(
            @NonNull byte[] scope, @Nullable byte[] init,
            @Nullable String mimeType, @KeyType int keyType,
            @Nullable HashMap<String, String> optionalParameters)
            throws NotProvisionedException;

    /**
     * A key response is received from the license server by the app, then it is
     * provided to the MediaDrm instance using provideKeyResponse.  When the
     * response is for an offline key request, a keySetId is returned that can be
     * used to later restore the keys to a new session with the method
     * {@link #restoreKeys}.
     * When the response is for a streaming or release request, an empty byte array
     * is returned.
     *
     * @param scope may be a sessionId or keySetId depending on the type of the
     * response.  Scope should be set to the sessionId when the response is for either
     * streaming or offline key requests.  Scope should be set to the keySetId when
     * the response is for a release request.
     * @param response the byte array response from the server
     * @return If the response is for an offline request, the keySetId for the offline
     * keys will be returned. If the response is for a streaming or release request
     * an empty byte array will be returned.
     *
     * @throws NotProvisionedException if the response indicates that
     * reprovisioning is required
     * @throws DeniedByServerException if the response indicates that the
     * server rejected the request
     */
    @Nullable
    public native byte[] provideKeyResponse(
            @NonNull byte[] scope, @NonNull byte[] response)
            throws NotProvisionedException, DeniedByServerException;


    /**
     * Restore persisted offline keys into a new session.  keySetId identifies the
     * keys to load, obtained from a prior call to {@link #provideKeyResponse}.
     *
     * @param sessionId the session ID for the DRM session
     * @param keySetId identifies the saved key set to restore
     */
    public native void restoreKeys(@NonNull byte[] sessionId, @NonNull byte[] keySetId);

    /**
     * Remove the current keys from a session.
     *
     * @param sessionId the session ID for the DRM session
     */
    public native void removeKeys(@NonNull byte[] sessionId);

    /**
     * Request an informative description of the key status for the session.  The status is
     * in the form of {name, value} pairs.  Since DRM license policies vary by vendor,
     * the specific status field names are determined by each DRM vendor.  Refer to your
     * DRM provider documentation for definitions of the field names for a particular
     * DRM plugin.
     *
     * @param sessionId the session ID for the DRM session
     */
    @NonNull
    public native HashMap<String, String> queryKeyStatus(@NonNull byte[] sessionId);

    /**
     * Contains the opaque data an app uses to request a certificate from a provisioning
     * server
     */
    public static final class ProvisionRequest {
        ProvisionRequest() {}

        /**
         * Get the opaque message data
         */
        @NonNull
        public byte[] getData() {
            if (mData == null) {
                // this should never happen as mData is initialized in
                // JNI after construction of the KeyRequest object. The check
                // is needed here to guarantee @NonNull annotation.
                throw new RuntimeException("ProvisionRequest is not initialized");
            }
            return mData;
        }

        /**
         * Get the default URL to use when sending the provision request
         * message to a server, if known. The app may prefer to use a different
         * provisioning server URL obtained from other sources.
         * This method returns an empty string if the default URL is not known.
         */
        @NonNull
        public String getDefaultUrl() {
            if (mDefaultUrl == null) {
                // this should never happen as mDefaultUrl is initialized in
                // JNI after construction of the ProvisionRequest object. The check
                // is needed here to guarantee @NonNull annotation.
                throw new RuntimeException("ProvisionRequest is not initialized");
            }
            return mDefaultUrl;
        }

        private byte[] mData;
        private String mDefaultUrl;
    }

    /**
     * A provision request/response exchange occurs between the app and a provisioning
     * server to retrieve a device certificate.  If provisionining is required, the
     * EVENT_PROVISION_REQUIRED event will be sent to the event handler.
     * getProvisionRequest is used to obtain the opaque provision request byte array that
     * should be delivered to the provisioning server. The provision request byte array
     * is returned in ProvisionRequest.data. The recommended URL to deliver the provision
     * request to is returned in ProvisionRequest.defaultUrl.
     */
    @NonNull
    public ProvisionRequest getProvisionRequest() {
        return getProvisionRequestNative(CERTIFICATE_TYPE_NONE, "");
    }

    @NonNull
    private native ProvisionRequest getProvisionRequestNative(int certType,
           @NonNull String certAuthority);

    /**
     * After a provision response is received by the app, it is provided to the
     * MediaDrm instance using this method.
     *
     * @param response the opaque provisioning response byte array to provide to the
     * MediaDrm instance.
     *
     * @throws DeniedByServerException if the response indicates that the
     * server rejected the request
     */
    public void provideProvisionResponse(@NonNull byte[] response)
            throws DeniedByServerException {
        provideProvisionResponseNative(response);
    }

    @NonNull
    private native Certificate provideProvisionResponseNative(@NonNull byte[] response)
            throws DeniedByServerException;

    /**
     * The keys in an offline license allow protected content to be played even
     * if the device is not connected to a network. Offline licenses are stored
     * on the device after a key request/response exchange when the key request
     * KeyType is OFFLINE. Normally each app is responsible for keeping track of
     * the keySetIds it has created. If an app loses the keySetId for any stored
     * licenses that it created, however, it must be able to recover the stored
     * keySetIds so those licenses can be removed when they expire or when the
     * app is uninstalled.
     * <p>
     * This method returns a list of the keySetIds for all offline licenses.
     * The offline license keySetId may be used to query the status of an
     * offline license with {@link #getOfflineLicenseState} or remove it with
     * {@link #removeOfflineLicense}.
     *
     * @return a list of offline license keySetIds
     */
    @NonNull
    public native List<byte[]> getOfflineLicenseKeySetIds();

    /**
     * Normally offline licenses are released using a key request/response
     * exchange using {@link #getKeyRequest} where the key type is
     * KEY_TYPE_RELEASE, followed by {@link #provideKeyResponse}. This allows
     * the server to cryptographically confirm that the license has been removed
     * and then adjust the count of offline licenses allocated to the device.
     * <p>
     * In some exceptional situations it may be necessary to directly remove
     * offline licenses without notifying the server, which may be performed
     * using this method.
     *
     * @param keySetId the id of the offline license to remove
     * @throws IllegalArgumentException if the keySetId does not refer to an
     * offline license.
     */
    public native void removeOfflineLicense(@NonNull byte[] keySetId);

    /**
     * Offline license state is unknown, an error occurred while trying
     * to access it.
     */
    public static final int OFFLINE_LICENSE_STATE_UNKNOWN = 0;

    /**
     * Offline license is usable, the keys may be used for decryption.
     */
    public static final int OFFLINE_LICENSE_STATE_USABLE = 1;

    /**
     * Offline license is released, the keys have been marked for
     * release using {@link #getKeyRequest} with KEY_TYPE_RELEASE but
     * the key response has not been received.
     */
    public static final int OFFLINE_LICENSE_STATE_RELEASED = 2;

    /** @hide */
    @IntDef({
        OFFLINE_LICENSE_STATE_UNKNOWN,
        OFFLINE_LICENSE_STATE_USABLE,
        OFFLINE_LICENSE_STATE_RELEASED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface OfflineLicenseState {}

    /**
     * Request the state of an offline license. An offline license may be usable
     * or inactive. The keys in a usable offline license are available for
     * decryption. When the offline license state is inactive, the keys have
     * been marked for release using {@link #getKeyRequest} with
     * KEY_TYPE_RELEASE but the key response has not been received. The keys in
     * an inactive offline license are not usable for decryption.
     *
     * @param keySetId selects the offline license
     * @return the offline license state
     * @throws IllegalArgumentException if the keySetId does not refer to an
     * offline license.
     */
    @OfflineLicenseState
    public native int getOfflineLicenseState(@NonNull byte[] keySetId);

    /**
     * Secure stops are a way to enforce limits on the number of concurrent
     * streams per subscriber across devices. They provide secure monitoring of
     * the lifetime of content decryption keys in MediaDrm sessions.
     * <p>
     * A secure stop is written to secure persistent memory when keys are loaded
     * into a MediaDrm session. The secure stop state indicates that the keys
     * are available for use. When playback completes and the keys are removed
     * or the session is destroyed, the secure stop state is updated to indicate
     * that keys are no longer usable.
     * <p>
     * After playback, the app can query the secure stop and send it in a
     * message to the license server confirming that the keys are no longer
     * active. The license server returns a secure stop release response
     * message to the app which then deletes the secure stop from persistent
     * memory using {@link #releaseSecureStops}.
     * <p>
     * Each secure stop has a unique ID that can be used to identify it during
     * enumeration, access and removal.
     *
     * @return a list of all secure stops from secure persistent memory
     * @deprecated This method is deprecated and may be removed in a future
     * release. Secure stops are a way to enforce limits on the number of
     * concurrent streams per subscriber across devices. They provide secure
     * monitoring of the lifetime of content decryption keys in MediaDrm
     * sessions. Limits on concurrent streams may also be enforced by
     * periodically renewing licenses. This can be achieved by calling
     * {@link #getKeyRequest} to initiate a renewal. MediaDrm users should
     * transition away from secure stops to periodic renewals.
     */
    @NonNull
    public native List<byte[]> getSecureStops();

    /**
     * Return a list of all secure stop IDs currently in persistent memory.
     * The secure stop ID can be used to access or remove the corresponding
     * secure stop.
     *
     * @return a list of secure stop IDs
     * @deprecated This method is deprecated and may be removed in a future
     * release. Use renewals by calling {@link #getKeyRequest} to track
     * concurrent playback. See additional information in
     * {@link #getSecureStops}
     */
    @NonNull
    public native List<byte[]> getSecureStopIds();

    /**
     * Access a specific secure stop given its secure stop ID.
     * Each secure stop has a unique ID.
     *
     * @param ssid the ID of the secure stop to return
     * @return the secure stop identified by ssid
     * @deprecated This method is deprecated and may be removed in a future
     * release. Use renewals by calling {@link #getKeyRequest} to track
     * concurrent playback. See additional information in
     * {@link #getSecureStops}
     */
    @NonNull
    public native byte[] getSecureStop(@NonNull byte[] ssid);

    /**
     * Process the secure stop server response message ssRelease.  After
     * authenticating the message, remove the secure stops identified in the
     * response.
     *
     * @param ssRelease the server response indicating which secure stops to release
     * @deprecated This method is deprecated and may be removed in a future
     * release. Use renewals by calling {@link #getKeyRequest} to track
     * concurrent playback. See additional information in
     * {@link #getSecureStops}
     */
    public native void releaseSecureStops(@NonNull byte[] ssRelease);

    /**
     * Remove a specific secure stop without requiring a secure stop release message
     * from the license server.
     * @param ssid the ID of the secure stop to remove
     * @deprecated This method is deprecated and may be removed in a future
     * release. Use renewals by calling {@link #getKeyRequest} to track
     * concurrent playback. See additional information in
     * {@link #getSecureStops}
     */
    public native void removeSecureStop(@NonNull byte[] ssid);

    /**
     * Remove all secure stops without requiring a secure stop release message from
     * the license server.
     *
     * This method was added in API 28. In API versions 18 through 27,
     * {@link #releaseAllSecureStops} should be called instead. There is no need to
     * do anything for API versions prior to 18.
     * @deprecated This method is deprecated and may be removed in a future
     * release. Use renewals by calling {@link #getKeyRequest} to track
     * concurrent playback. See additional information in
     * {@link #getSecureStops}
     */
    public native void removeAllSecureStops();

    /**
     * Remove all secure stops without requiring a secure stop release message from
     * the license server.
     *
     * @deprecated Remove all secure stops using {@link #removeAllSecureStops} instead.
     */
    public void releaseAllSecureStops() {
        removeAllSecureStops();;
    }

    /**
     * @deprecated Not of any use for application development;
     * please note that the related integer constants remain supported:
     * {@link #HDCP_LEVEL_UNKNOWN},
     * {@link #HDCP_NONE},
     * {@link #HDCP_V1},
     * {@link #HDCP_V2},
     * {@link #HDCP_V2_1},
     * {@link #HDCP_V2_2},
     * {@link #HDCP_V2_3}
     *
     * @removed mistakenly exposed previously
     */
    @Deprecated
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HDCP_LEVEL_UNKNOWN, HDCP_NONE, HDCP_V1, HDCP_V2,
                        HDCP_V2_1, HDCP_V2_2, HDCP_V2_3, HDCP_NO_DIGITAL_OUTPUT})
    public @interface HdcpLevel {}


    /**
     * The DRM plugin did not report an HDCP level, or an error
     * occurred accessing it
     */
    public static final int HDCP_LEVEL_UNKNOWN = 0;

    /**
     * HDCP is not supported on this device, content is unprotected
     */
    public static final int HDCP_NONE = 1;

    /**
     * HDCP version 1.0
     */
    public static final int HDCP_V1 = 2;

    /**
     * HDCP version 2.0 Type 1.
     */
    public static final int HDCP_V2 = 3;

    /**
     * HDCP version 2.1 Type 1.
     */
    public static final int HDCP_V2_1 = 4;

    /**
     *  HDCP version 2.2 Type 1.
     */
    public static final int HDCP_V2_2 = 5;

    /**
     *  HDCP version 2.3 Type 1.
     */
    public static final int HDCP_V2_3 = 6;

    /**
     * No digital output, implicitly secure
     */
    public static final int HDCP_NO_DIGITAL_OUTPUT = Integer.MAX_VALUE;

    /**
     * Return the HDCP level negotiated with downstream receivers the
     * device is connected to. If multiple HDCP-capable displays are
     * simultaneously connected to separate interfaces, this method
     * returns the lowest negotiated level of all interfaces.
     * <p>
     * This method should only be used for informational purposes, not for
     * enforcing compliance with HDCP requirements. Trusted enforcement of
     * HDCP policies must be handled by the DRM system.
     * <p>
     * @return the connected HDCP level
     */
    @HdcpLevel
    public native int getConnectedHdcpLevel();

    /**
     * Return the maximum supported HDCP level. The maximum HDCP level is a
     * constant for a given device, it does not depend on downstream receivers
     * that may be connected. If multiple HDCP-capable interfaces are present,
     * it indicates the highest of the maximum HDCP levels of all interfaces.
     * <p>
     * @return the maximum supported HDCP level
     */
    @HdcpLevel
    public native int getMaxHdcpLevel();

    /**
     * Return the number of MediaDrm sessions that are currently opened
     * simultaneously among all MediaDrm instances for the active DRM scheme.
     * @return the number of open sessions.
     */
    public native int getOpenSessionCount();

    /**
     * Return the maximum number of MediaDrm sessions that may be opened
     * simultaneosly among all MediaDrm instances for the active DRM
     * scheme. The maximum number of sessions is not affected by any
     * sessions that may have already been opened.
     * @return maximum sessions.
     */
    public native int getMaxSessionCount();

    /**
     * Security level indicates the robustness of the device's DRM
     * implementation.
     *
     * @deprecated Not of any use for application development;
     * please note that the related integer constants remain supported:
     * {@link #SECURITY_LEVEL_UNKNOWN},
     * {@link #SECURITY_LEVEL_SW_SECURE_CRYPTO},
     * {@link #SECURITY_LEVEL_SW_SECURE_DECODE},
     * {@link #SECURITY_LEVEL_HW_SECURE_CRYPTO},
     * {@link #SECURITY_LEVEL_HW_SECURE_DECODE},
     * {@link #SECURITY_LEVEL_HW_SECURE_ALL}
     *
     * @removed mistakenly exposed previously
     */
    @Deprecated
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SECURITY_LEVEL_UNKNOWN, SECURITY_LEVEL_SW_SECURE_CRYPTO,
            SECURITY_LEVEL_SW_SECURE_DECODE, SECURITY_LEVEL_HW_SECURE_CRYPTO,
            SECURITY_LEVEL_HW_SECURE_DECODE, SECURITY_LEVEL_HW_SECURE_ALL})
    public @interface SecurityLevel {}

    /**
     * The DRM plugin did not report a security level, or an error occurred
     * accessing it
     */
    public static final int SECURITY_LEVEL_UNKNOWN = 0;

    /**
     * DRM key management uses software-based whitebox crypto.
     */
    public static final int SECURITY_LEVEL_SW_SECURE_CRYPTO = 1;

    /**
     * DRM key management and decoding use software-based whitebox crypto.
     */
    public static final int SECURITY_LEVEL_SW_SECURE_DECODE = 2;

    /**
     * DRM key management and crypto operations are performed within a hardware
     * backed trusted execution environment.
     */
    public static final int SECURITY_LEVEL_HW_SECURE_CRYPTO = 3;

    /**
     * DRM key management, crypto operations and decoding of content are
     * performed within a hardware backed trusted execution environment.
     */
    public static final int SECURITY_LEVEL_HW_SECURE_DECODE = 4;

    /**
     * DRM key management, crypto operations, decoding of content and all
     * handling of the media (compressed and uncompressed) is handled within a
     * hardware backed trusted execution environment.
     */
    public static final int SECURITY_LEVEL_HW_SECURE_ALL = 5;

    /**
     * Indicates that the maximum security level supported by the device should
     * be used when opening a session. This is the default security level
     * selected when a session is opened.
     * @hide
     */
    public static final int SECURITY_LEVEL_MAX = 6;

    /**
     * Returns a value that may be passed as a parameter to {@link #openSession(int)}
     * requesting that the session be opened at the maximum security level of
     * the device.
     *
     * This security level is only valid for the application running on the physical Android
     * device (e.g. {@link android.content.Context#DEVICE_ID_DEFAULT}). While running on a
     * {@link android.companion.virtual.VirtualDevice} the maximum supported security level
     * might be different.
     */
    public static final int getMaxSecurityLevel() {
        return SECURITY_LEVEL_MAX;
    }

    /**
     * Return the current security level of a session. A session has an initial
     * security level determined by the robustness of the DRM system's
     * implementation on the device. The security level may be changed at the
     * time a session is opened using {@link #openSession}.
     * @param sessionId the session to query.
     * <p>
     * @return the security level of the session
     */
    @SecurityLevel
    public native int getSecurityLevel(@NonNull byte[] sessionId);

    /**
     * String property name: identifies the maker of the DRM plugin
     */
    public static final String PROPERTY_VENDOR = "vendor";

    /**
     * String property name: identifies the version of the DRM plugin
     */
    public static final String PROPERTY_VERSION = "version";

    /**
     * String property name: describes the DRM plugin
     */
    public static final String PROPERTY_DESCRIPTION = "description";

    /**
     * String property name: a comma-separated list of cipher and mac algorithms
     * supported by CryptoSession.  The list may be empty if the DRM
     * plugin does not support CryptoSession operations.
     */
    public static final String PROPERTY_ALGORITHMS = "algorithms";

    /** @hide */
    @StringDef(prefix = { "PROPERTY_" }, value = {
        PROPERTY_VENDOR,
        PROPERTY_VERSION,
        PROPERTY_DESCRIPTION,
        PROPERTY_ALGORITHMS,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StringProperty {}

    /**
     * Read a MediaDrm String property value, given the property name string.
     * <p>
     * Standard fields names are:
     * {@link #PROPERTY_VENDOR}, {@link #PROPERTY_VERSION},
     * {@link #PROPERTY_DESCRIPTION}, {@link #PROPERTY_ALGORITHMS}
     */
    @NonNull
    public native String getPropertyString(@NonNull String propertyName);

    /**
     * Set a MediaDrm String property value, given the property name string
     * and new value for the property.
     */
    public native void setPropertyString(@NonNull String propertyName,
            @NonNull String value);

    /**
     * Byte array property name: the device unique identifier is established during
     * device provisioning and provides a means of uniquely identifying each device.
     */
    public static final String PROPERTY_DEVICE_UNIQUE_ID = "deviceUniqueId";

    /** @hide */
    @StringDef(prefix = { "PROPERTY_" }, value = {
        PROPERTY_DEVICE_UNIQUE_ID,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ArrayProperty {}

    /**
     * Read a MediaDrm byte array property value, given the property name string.
     * <p>
     * Standard fields names are {@link #PROPERTY_DEVICE_UNIQUE_ID}
     */
    @NonNull
    public native byte[] getPropertyByteArray(String propertyName);

    /**
    * Set a MediaDrm byte array property value, given the property name string
    * and new value for the property.
    */
    public native void setPropertyByteArray(
            @NonNull String propertyName, @NonNull byte[] value);

    private static final native void setCipherAlgorithmNative(
            @NonNull MediaDrm drm, @NonNull byte[] sessionId, @NonNull String algorithm);

    private static final native void setMacAlgorithmNative(
            @NonNull MediaDrm drm, @NonNull byte[] sessionId, @NonNull String algorithm);

    @NonNull
    private static final native byte[] encryptNative(
            @NonNull MediaDrm drm, @NonNull byte[] sessionId,
            @NonNull byte[] keyId, @NonNull byte[] input, @NonNull byte[] iv);

    @NonNull
    private static final native byte[] decryptNative(
            @NonNull MediaDrm drm, @NonNull byte[] sessionId,
            @NonNull byte[] keyId, @NonNull byte[] input, @NonNull byte[] iv);

    @NonNull
    private static final native byte[] signNative(
            @NonNull MediaDrm drm, @NonNull byte[] sessionId,
            @NonNull byte[] keyId, @NonNull byte[] message);

    private static final native boolean verifyNative(
            @NonNull MediaDrm drm, @NonNull byte[] sessionId,
            @NonNull byte[] keyId, @NonNull byte[] message, @NonNull byte[] signature);

    /**
     * Return Metrics data about the current MediaDrm instance.
     *
     * @return a {@link PersistableBundle} containing the set of attributes and values
     * available for this instance of MediaDrm.
     * The attributes are described in {@link MetricsConstants}.
     *
     * Additional vendor-specific fields may also be present in
     * the return value.
     */
    public PersistableBundle getMetrics() {
        PersistableBundle bundle = getMetricsNative();
        return bundle;
    }

    private native PersistableBundle getMetricsNative();

    /**
     * In addition to supporting decryption of DASH Common Encrypted Media, the
     * MediaDrm APIs provide the ability to securely deliver session keys from
     * an operator's session key server to a client device, based on the factory-installed
     * root of trust, and then perform encrypt, decrypt, sign and verify operations
     * with the session key on arbitrary user data.
     * <p>
     * The CryptoSession class implements generic encrypt/decrypt/sign/verify methods
     * based on the established session keys.  These keys are exchanged using the
     * getKeyRequest/provideKeyResponse methods.
     * <p>
     * Applications of this capability could include securing various types of
     * purchased or private content, such as applications, books and other media,
     * photos or media delivery protocols.
     * <p>
     * Operators can create session key servers that are functionally similar to a
     * license key server, except that instead of receiving license key requests and
     * providing encrypted content keys which are used specifically to decrypt A/V media
     * content, the session key server receives session key requests and provides
     * encrypted session keys which can be used for general purpose crypto operations.
     * <p>
     * A CryptoSession is obtained using {@link #getCryptoSession}
     */
    public final class CryptoSession {
        private byte[] mSessionId;

        CryptoSession(@NonNull byte[] sessionId,
                      @NonNull String cipherAlgorithm,
                      @NonNull String macAlgorithm)
        {
            mSessionId = sessionId;
            setCipherAlgorithmNative(MediaDrm.this, sessionId, cipherAlgorithm);
            setMacAlgorithmNative(MediaDrm.this, sessionId, macAlgorithm);
        }

        /**
         * Encrypt data using the CryptoSession's cipher algorithm
         *
         * @param keyid specifies which key to use
         * @param input the data to encrypt
         * @param iv the initialization vector to use for the cipher
         */
        @NonNull
        public byte[] encrypt(
                @NonNull byte[] keyid, @NonNull byte[] input, @NonNull byte[] iv) {
            return encryptNative(MediaDrm.this, mSessionId, keyid, input, iv);
        }

        /**
         * Decrypt data using the CryptoSessions's cipher algorithm
         *
         * @param keyid specifies which key to use
         * @param input the data to encrypt
         * @param iv the initialization vector to use for the cipher
         */
        @NonNull
        public byte[] decrypt(
                @NonNull byte[] keyid, @NonNull byte[] input, @NonNull byte[] iv) {
            return decryptNative(MediaDrm.this, mSessionId, keyid, input, iv);
        }

        /**
         * Sign data using the CryptoSessions's mac algorithm.
         *
         * @param keyid specifies which key to use
         * @param message the data for which a signature is to be computed
         */
        @NonNull
        public byte[] sign(@NonNull byte[] keyid, @NonNull byte[] message) {
            return signNative(MediaDrm.this, mSessionId, keyid, message);
        }

        /**
         * Verify a signature using the CryptoSessions's mac algorithm. Return true
         * if the signatures match, false if they do no.
         *
         * @param keyid specifies which key to use
         * @param message the data to verify
         * @param signature the reference signature which will be compared with the
         *        computed signature
         */
        public boolean verify(
                @NonNull byte[] keyid, @NonNull byte[] message, @NonNull byte[] signature) {
            return verifyNative(MediaDrm.this, mSessionId, keyid, message, signature);
        }
    };

    /**
     * Obtain a CryptoSession object which can be used to encrypt, decrypt,
     * sign and verify messages or data using the session keys established
     * for the session using methods {@link #getKeyRequest} and
     * {@link #provideKeyResponse} using a session key server.
     *
     * @param sessionId the session ID for the session containing keys
     * to be used for encrypt, decrypt, sign and/or verify
     * @param cipherAlgorithm the algorithm to use for encryption and
     * decryption ciphers. The algorithm string conforms to JCA Standard
     * Names for Cipher Transforms and is case insensitive.  For example
     * "AES/CBC/NoPadding".
     * @param macAlgorithm the algorithm to use for sign and verify
     * The algorithm string conforms to JCA Standard Names for Mac
     * Algorithms and is case insensitive.  For example "HmacSHA256".
     * <p>
     * The list of supported algorithms for a DRM plugin can be obtained
     * using the method {@link #getPropertyString} with the property name
     * "algorithms".
     */
    public CryptoSession getCryptoSession(
            @NonNull byte[] sessionId,
            @NonNull String cipherAlgorithm, @NonNull String macAlgorithm)
    {
        return new CryptoSession(sessionId, cipherAlgorithm, macAlgorithm);
    }

    /**
     * Contains the opaque data an app uses to request a certificate from a provisioning
     * server
     *
     * @hide - not part of the public API at this time
     */
    public static final class CertificateRequest {
        private byte[] mData;
        private String mDefaultUrl;

        CertificateRequest(@NonNull byte[] data, @NonNull String defaultUrl) {
            mData = data;
            mDefaultUrl = defaultUrl;
        }

        /**
         * Get the opaque message data
         */
        @NonNull
        @UnsupportedAppUsage
        public byte[] getData() { return mData; }

        /**
         * Get the default URL to use when sending the certificate request
         * message to a server, if known. The app may prefer to use a different
         * certificate server URL obtained from other sources.
         */
        @NonNull
        @UnsupportedAppUsage
        public String getDefaultUrl() { return mDefaultUrl; }
    }

    /**
     * Generate a certificate request, specifying the certificate type
     * and authority. The response received should be passed to
     * provideCertificateResponse.
     *
     * @param certType Specifies the certificate type.
     *
     * @param certAuthority is passed to the certificate server to specify
     * the chain of authority.
     *
     * @hide - not part of the public API at this time
     */
    @NonNull
    @UnsupportedAppUsage
    public CertificateRequest getCertificateRequest(
            @CertificateType int certType, @NonNull String certAuthority)
    {
        ProvisionRequest provisionRequest = getProvisionRequestNative(certType, certAuthority);
        return new CertificateRequest(provisionRequest.getData(),
                provisionRequest.getDefaultUrl());
    }

    /**
     * Contains the wrapped private key and public certificate data associated
     * with a certificate.
     *
     * @hide - not part of the public API at this time
     */
    public static final class Certificate {
        Certificate() {}

        /**
         * Get the wrapped private key data
         */
        @NonNull
        @UnsupportedAppUsage
        public byte[] getWrappedPrivateKey() {
            if (mWrappedKey == null) {
                // this should never happen as mWrappedKey is initialized in
                // JNI after construction of the KeyRequest object. The check
                // is needed here to guarantee @NonNull annotation.
                throw new RuntimeException("Certificate is not initialized");
            }
            return mWrappedKey;
        }

        /**
         * Get the PEM-encoded certificate chain
         */
        @NonNull
        @UnsupportedAppUsage
        public byte[] getContent() {
            if (mCertificateData == null) {
                // this should never happen as mCertificateData is initialized in
                // JNI after construction of the KeyRequest object. The check
                // is needed here to guarantee @NonNull annotation.
                throw new RuntimeException("Certificate is not initialized");
            }
            return mCertificateData;
        }

        private byte[] mWrappedKey;
        private byte[] mCertificateData;
    }


    /**
     * Process a response from the certificate server.  The response
     * is obtained from an HTTP Post to the url provided by getCertificateRequest.
     * <p>
     * The public X509 certificate chain and wrapped private key are returned
     * in the returned Certificate objec.  The certificate chain is in PEM format.
     * The wrapped private key should be stored in application private
     * storage, and used when invoking the signRSA method.
     *
     * @param response the opaque certificate response byte array to provide to the
     * MediaDrm instance.
     *
     * @throws DeniedByServerException if the response indicates that the
     * server rejected the request
     *
     * @hide - not part of the public API at this time
     */
    @NonNull
    @UnsupportedAppUsage
    public Certificate provideCertificateResponse(@NonNull byte[] response)
            throws DeniedByServerException {
        return provideProvisionResponseNative(response);
    }

    @NonNull
    private static final native byte[] signRSANative(
            @NonNull MediaDrm drm, @NonNull byte[] sessionId,
            @NonNull String algorithm, @NonNull byte[] wrappedKey, @NonNull byte[] message);

    /**
     * Sign data using an RSA key
     *
     * @param sessionId a sessionId obtained from openSession on the MediaDrm object
     * @param algorithm the signing algorithm to use, e.g. "PKCS1-BlockType1"
     * @param wrappedKey - the wrapped (encrypted) RSA private key obtained
     * from provideCertificateResponse
     * @param message the data for which a signature is to be computed
     *
     * @hide - not part of the public API at this time
     */
    @NonNull
    @UnsupportedAppUsage
    public byte[] signRSA(
            @NonNull byte[] sessionId, @NonNull String algorithm,
            @NonNull byte[] wrappedKey, @NonNull byte[] message) {
        return signRSANative(this, sessionId, algorithm, wrappedKey, message);
    }

    /**
     * Query if the crypto scheme requires the use of a secure decoder
     * to decode data of the given mime type at the default security level.
     * The default security level is defined as the highest security level
     * supported on the device.
     *
     * @param mime The mime type of the media data. Please use {@link
     *             #isCryptoSchemeSupported(UUID, String)} to query mime type support separately;
     *             for unsupported mime types the return value of {@link
     *             #requiresSecureDecoder(String)} is crypto scheme dependent.
     */
    public boolean requiresSecureDecoder(@NonNull String mime) {
        return requiresSecureDecoder(mime, getMaxSecurityLevel());
    }

    /**
     * Query if the crypto scheme requires the use of a secure decoder
     * to decode data of the given mime type at the given security level.
     *
     * @param mime The mime type of the media data. Please use {@link
     *             #isCryptoSchemeSupported(UUID, String, int)} to query mime type support
     *             separately; for unsupported mime types the return value of {@link
     *             #requiresSecureDecoder(String, int)} is crypto scheme dependent.
     * @param level a security level between {@link #SECURITY_LEVEL_SW_SECURE_CRYPTO}
     *              and {@link #SECURITY_LEVEL_HW_SECURE_ALL}. Otherwise the special value
     *              {@link #getMaxSecurityLevel()} is also permitted;
     *              use {@link #getMaxSecurityLevel()} to indicate the maximum security level
     *              supported by the device.
     * @throws IllegalArgumentException if the requested security level is none of the documented
     * values for the parameter {@code level}.
     */
    public native boolean requiresSecureDecoder(@NonNull String mime, @SecurityLevel int level);

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mCloseGuard != null) {
                mCloseGuard.warnIfOpen();
            }
            release();
        } finally {
            super.finalize();
        }
    }

    /**
     * Releases resources associated with the current session of
     * MediaDrm. It is considered good practice to call this method when
     * the {@link MediaDrm} object is no longer needed in your
     * application. After this method is called, {@link MediaDrm} is no
     * longer usable since it has lost all of its required resource.
     *
     * This method was added in API 28. In API versions 18 through 27, release()
     * should be called instead. There is no need to do anything for API
     * versions prior to 18.
     */
    @Override
    public void close() {
        release();
    }

    /**
     * @deprecated replaced by {@link #close()}.
     */
    @Deprecated
    public void release() {
        mCloseGuard.close();
        if (mClosed.compareAndSet(false, true)) {
            native_release();
            mPlaybackComponentMap.clear();
        }
    }

    /** @hide */
    public native final void native_release();

    private static native final void native_init();

    private native final void native_setup(Object mediadrm_this, byte[] uuid,
            String appPackageName);

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    /**
     * Definitions for the metrics that are reported via the
     * {@link #getMetrics} call.
     */
    public final static class MetricsConstants
    {
        private MetricsConstants() {}

        /**
         * Key to extract the number of successful {@link #openSession} calls
         * from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String OPEN_SESSION_OK_COUNT
            = "drm.mediadrm.open_session.ok.count";

        /**
         * Key to extract the number of failed {@link #openSession} calls
         * from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String OPEN_SESSION_ERROR_COUNT
            = "drm.mediadrm.open_session.error.count";

        /**
         * Key to extract the list of error codes that were returned from
         * {@link #openSession} calls. The key is used to lookup the list
         * in the {@link PersistableBundle} returned by a {@link #getMetrics}
         * call.
         * The list is an array of Long values
         * ({@link android.os.BaseBundle#getLongArray}).
         */
        public static final String OPEN_SESSION_ERROR_LIST
            = "drm.mediadrm.open_session.error.list";

        /**
         * Key to extract the number of successful {@link #closeSession} calls
         * from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String CLOSE_SESSION_OK_COUNT
            = "drm.mediadrm.close_session.ok.count";

        /**
         * Key to extract the number of failed {@link #closeSession} calls
         * from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String CLOSE_SESSION_ERROR_COUNT
            = "drm.mediadrm.close_session.error.count";

        /**
         * Key to extract the list of error codes that were returned from
         * {@link #closeSession} calls. The key is used to lookup the list
         * in the {@link PersistableBundle} returned by a {@link #getMetrics}
         * call.
         * The list is an array of Long values
         * ({@link android.os.BaseBundle#getLongArray}).
         */
        public static final String CLOSE_SESSION_ERROR_LIST
            = "drm.mediadrm.close_session.error.list";

        /**
         * Key to extract the start times of sessions. Times are
         * represented as milliseconds since epoch (1970-01-01T00:00:00Z).
         * The start times are returned from the {@link PersistableBundle}
         * from a {@link #getMetrics} call.
         * The start times are returned as another {@link PersistableBundle}
         * containing the session ids as keys and the start times as long
         * values. Use {@link android.os.BaseBundle#keySet} to get the list of
         * session ids, and then {@link android.os.BaseBundle#getLong} to get
         * the start time for each session.
         */
        public static final String SESSION_START_TIMES_MS
            = "drm.mediadrm.session_start_times_ms";

        /**
         * Key to extract the end times of sessions. Times are
         * represented as milliseconds since epoch (1970-01-01T00:00:00Z).
         * The end times are returned from the {@link PersistableBundle}
         * from a {@link #getMetrics} call.
         * The end times are returned as another {@link PersistableBundle}
         * containing the session ids as keys and the end times as long
         * values. Use {@link android.os.BaseBundle#keySet} to get the list of
         * session ids, and then {@link android.os.BaseBundle#getLong} to get
         * the end time for each session.
         */
        public static final String SESSION_END_TIMES_MS
            = "drm.mediadrm.session_end_times_ms";

        /**
         * Key to extract the number of successful {@link #getKeyRequest} calls
         * from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String GET_KEY_REQUEST_OK_COUNT
            = "drm.mediadrm.get_key_request.ok.count";

        /**
         * Key to extract the number of failed {@link #getKeyRequest}
         * calls from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String GET_KEY_REQUEST_ERROR_COUNT
            = "drm.mediadrm.get_key_request.error.count";

        /**
         * Key to extract the list of error codes that were returned from
         * {@link #getKeyRequest} calls. The key is used to lookup the list
         * in the {@link PersistableBundle} returned by a {@link #getMetrics}
         * call.
         * The list is an array of Long values
         * ({@link android.os.BaseBundle#getLongArray}).
         */
        public static final String GET_KEY_REQUEST_ERROR_LIST
            = "drm.mediadrm.get_key_request.error.list";

        /**
         * Key to extract the average time in microseconds of calls to
         * {@link #getKeyRequest}. The value is retrieved from the
         * {@link PersistableBundle} returned from {@link #getMetrics}.
         * The time is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String GET_KEY_REQUEST_OK_TIME_MICROS
            = "drm.mediadrm.get_key_request.ok.average_time_micros";

        /**
         * Key to extract the number of successful {@link #provideKeyResponse}
         * calls from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String PROVIDE_KEY_RESPONSE_OK_COUNT
            = "drm.mediadrm.provide_key_response.ok.count";

        /**
         * Key to extract the number of failed {@link #provideKeyResponse}
         * calls from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String PROVIDE_KEY_RESPONSE_ERROR_COUNT
            = "drm.mediadrm.provide_key_response.error.count";

        /**
         * Key to extract the list of error codes that were returned from
         * {@link #provideKeyResponse} calls. The key is used to lookup the
         * list in the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The list is an array of Long values
         * ({@link android.os.BaseBundle#getLongArray}).
         */
        public static final String PROVIDE_KEY_RESPONSE_ERROR_LIST
            = "drm.mediadrm.provide_key_response.error.list";

        /**
         * Key to extract the average time in microseconds of calls to
         * {@link #provideKeyResponse}. The valus is retrieved from the
         * {@link PersistableBundle} returned from {@link #getMetrics}.
         * The time is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String PROVIDE_KEY_RESPONSE_OK_TIME_MICROS
            = "drm.mediadrm.provide_key_response.ok.average_time_micros";

        /**
         * Key to extract the number of successful {@link #getProvisionRequest}
         * calls from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String GET_PROVISION_REQUEST_OK_COUNT
            = "drm.mediadrm.get_provision_request.ok.count";

        /**
         * Key to extract the number of failed {@link #getProvisionRequest}
         * calls from the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String GET_PROVISION_REQUEST_ERROR_COUNT
            = "drm.mediadrm.get_provision_request.error.count";

        /**
         * Key to extract the list of error codes that were returned from
         * {@link #getProvisionRequest} calls. The key is used to lookup the
         * list in the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The list is an array of Long values
         * ({@link android.os.BaseBundle#getLongArray}).
         */
        public static final String GET_PROVISION_REQUEST_ERROR_LIST
            = "drm.mediadrm.get_provision_request.error.list";

        /**
         * Key to extract the number of successful
         * {@link #provideProvisionResponse} calls from the
         * {@link PersistableBundle} returned by a {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String PROVIDE_PROVISION_RESPONSE_OK_COUNT
            = "drm.mediadrm.provide_provision_response.ok.count";

        /**
         * Key to extract the number of failed
         * {@link #provideProvisionResponse} calls from the
         * {@link PersistableBundle} returned by a {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String PROVIDE_PROVISION_RESPONSE_ERROR_COUNT
            = "drm.mediadrm.provide_provision_response.error.count";

        /**
         * Key to extract the list of error codes that were returned from
         * {@link #provideProvisionResponse} calls. The key is used to lookup
         * the list in the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The list is an array of Long values
         * ({@link android.os.BaseBundle#getLongArray}).
         */
        public static final String PROVIDE_PROVISION_RESPONSE_ERROR_LIST
            = "drm.mediadrm.provide_provision_response.error.list";

        /**
         * Key to extract the number of successful
         * {@link #getPropertyByteArray} calls were made with the
         * {@link #PROPERTY_DEVICE_UNIQUE_ID} value. The key is used to lookup
         * the value in the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String GET_DEVICE_UNIQUE_ID_OK_COUNT
            = "drm.mediadrm.get_device_unique_id.ok.count";

        /**
         * Key to extract the number of failed
         * {@link #getPropertyByteArray} calls were made with the
         * {@link #PROPERTY_DEVICE_UNIQUE_ID} value. The key is used to lookup
         * the value in the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String GET_DEVICE_UNIQUE_ID_ERROR_COUNT
            = "drm.mediadrm.get_device_unique_id.error.count";

        /**
         * Key to extract the list of error codes that were returned from
         * {@link #getPropertyByteArray} calls with the
         * {@link #PROPERTY_DEVICE_UNIQUE_ID} value. The key is used to lookup
         * the list in the {@link PersistableBundle} returned by a
         * {@link #getMetrics} call.
         * The list is an array of Long values
         * ({@link android.os.BaseBundle#getLongArray}).
         */
        public static final String GET_DEVICE_UNIQUE_ID_ERROR_LIST
            = "drm.mediadrm.get_device_unique_id.error.list";

        /**
         * Key to extract the count of {@link KeyStatus#STATUS_EXPIRED} events
         * that occured. The count is extracted from the
         * {@link PersistableBundle} returned from a {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String KEY_STATUS_EXPIRED_COUNT
            = "drm.mediadrm.key_status.EXPIRED.count";

        /**
         * Key to extract the count of {@link KeyStatus#STATUS_INTERNAL_ERROR}
         * events that occured. The count is extracted from the
         * {@link PersistableBundle} returned from a {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String KEY_STATUS_INTERNAL_ERROR_COUNT
            = "drm.mediadrm.key_status.INTERNAL_ERROR.count";

        /**
         * Key to extract the count of
         * {@link KeyStatus#STATUS_OUTPUT_NOT_ALLOWED} events that occured.
         * The count is extracted from the
         * {@link PersistableBundle} returned from a {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String KEY_STATUS_OUTPUT_NOT_ALLOWED_COUNT
            = "drm.mediadrm.key_status_change.OUTPUT_NOT_ALLOWED.count";

        /**
         * Key to extract the count of {@link KeyStatus#STATUS_PENDING}
         * events that occured. The count is extracted from the
         * {@link PersistableBundle} returned from a {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String KEY_STATUS_PENDING_COUNT
            = "drm.mediadrm.key_status_change.PENDING.count";

        /**
         * Key to extract the count of {@link KeyStatus#STATUS_USABLE}
         * events that occured. The count is extracted from the
         * {@link PersistableBundle} returned from a {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String KEY_STATUS_USABLE_COUNT
            = "drm.mediadrm.key_status_change.USABLE.count";

        /**
         * Key to extract the count of {@link OnEventListener#onEvent}
         * calls of type PROVISION_REQUIRED occured. The count is
         * extracted from the {@link PersistableBundle} returned from a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String EVENT_PROVISION_REQUIRED_COUNT
            = "drm.mediadrm.event.PROVISION_REQUIRED.count";

        /**
         * Key to extract the count of {@link OnEventListener#onEvent}
         * calls of type KEY_NEEDED occured. The count is
         * extracted from the {@link PersistableBundle} returned from a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String EVENT_KEY_NEEDED_COUNT
            = "drm.mediadrm.event.KEY_NEEDED.count";

        /**
         * Key to extract the count of {@link OnEventListener#onEvent}
         * calls of type KEY_EXPIRED occured. The count is
         * extracted from the {@link PersistableBundle} returned from a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String EVENT_KEY_EXPIRED_COUNT
            = "drm.mediadrm.event.KEY_EXPIRED.count";

        /**
         * Key to extract the count of {@link OnEventListener#onEvent}
         * calls of type VENDOR_DEFINED. The count is
         * extracted from the {@link PersistableBundle} returned from a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String EVENT_VENDOR_DEFINED_COUNT
            = "drm.mediadrm.event.VENDOR_DEFINED.count";

        /**
         * Key to extract the count of {@link OnEventListener#onEvent}
         * calls of type SESSION_RECLAIMED. The count is
         * extracted from the {@link PersistableBundle} returned from a
         * {@link #getMetrics} call.
         * The count is a Long value ({@link android.os.BaseBundle#getLong}).
         */
        public static final String EVENT_SESSION_RECLAIMED_COUNT
            = "drm.mediadrm.event.SESSION_RECLAIMED.count";
    }

    /**
     * Obtain a {@link PlaybackComponent} associated with a DRM session.
     * Call {@link PlaybackComponent#setLogSessionId(LogSessionId)} on
     * the returned object to associate a playback session with the DRM session.
     *
     * @param sessionId a DRM session ID obtained from {@link #openSession()}
     * @return a {@link PlaybackComponent} associated with the session,
     * or {@code null} if the session is closed or does not exist.
     * @see PlaybackComponent
     */
    @Nullable
    public PlaybackComponent getPlaybackComponent(@NonNull byte[] sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is null");
        }
        return mPlaybackComponentMap.get(ByteBuffer.wrap(sessionId));
    }

    private native void setPlaybackId(byte[] sessionId, String logSessionId);

    /** This class contains the Drm session ID and log session ID */
    public final class PlaybackComponent {
        private final byte[] mSessionId;
        @NonNull private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;

        /** @hide */
        public PlaybackComponent(byte[] sessionId) {
            mSessionId = sessionId;
        }


        /**
         * Sets the {@link LogSessionId}.
         *
         * <p>The implementation of this method varies by DRM provider; Please refer
         * to your DRM provider documentation for more details on this method.
         *
         * @throws UnsupportedOperationException when the vendor plugin does not
         * implement this method
         */
        public void setLogSessionId(@NonNull LogSessionId logSessionId) {
            Objects.requireNonNull(logSessionId);
            if (logSessionId.getStringId() == null) {
                throw new IllegalArgumentException("playbackId is null");
            }
            MediaDrm.this.setPlaybackId(mSessionId, logSessionId.getStringId());
            mLogSessionId = logSessionId;
        }


        /**
         * Returns the {@link LogSessionId}.
         */
        @NonNull public LogSessionId getLogSessionId() {
            return mLogSessionId;
        }
    }

    /**
     * Returns recent {@link LogMessage LogMessages} associated with this {@link MediaDrm}
     * instance.
     */
    @NonNull
    public native List<LogMessage> getLogMessages();

    /**
     * A {@link LogMessage} records an event in the {@link MediaDrm} framework
     * or vendor plugin.
     */
    public static final class LogMessage {
        private final long timestampMillis;
        private final int priority;
        private final String message;

        /**
         * Timing of the recorded event measured in milliseconds since the Epoch,
         * 1970-01-01 00:00:00 +0000 (UTC).
         */
        public final long getTimestampMillis() { return timestampMillis; }

        /**
         * Priority of the recorded event.
         * <p>
         * Possible priority constants are defined in {@link Log}, e.g.:
         * <ul>
         *     <li>{@link Log#ASSERT}</li>
         *     <li>{@link Log#ERROR}</li>
         *     <li>{@link Log#WARN}</li>
         *     <li>{@link Log#INFO}</li>
         *     <li>{@link Log#DEBUG}</li>
         *     <li>{@link Log#VERBOSE}</li>
         * </ul>
         */
        @Log.Level
        public final int getPriority() { return priority; }

        /**
         * Description of the recorded event.
         */
        @NonNull
        public final String getMessage() { return message; }

        private LogMessage(long timestampMillis, int priority, String message) {
            this.timestampMillis = timestampMillis;
            if (priority < Log.VERBOSE || priority > Log.ASSERT) {
                throw new IllegalArgumentException("invalid log priority " + priority);
            }
            this.priority = priority;
            this.message = message;
        }

        private char logPriorityChar() {
            switch (priority) {
                case Log.VERBOSE:
                    return 'V';
                case Log.DEBUG:
                    return 'D';
                case Log.INFO:
                    return 'I';
                case Log.WARN:
                    return 'W';
                case Log.ERROR:
                    return 'E';
                case Log.ASSERT:
                    return 'F';
                default:
            }
            return 'U';
        }

        @Override
        public String toString() {
            return String.format("LogMessage{%s %c %s}",
                    Instant.ofEpochMilli(timestampMillis), logPriorityChar(), message);
        }
    }
}
