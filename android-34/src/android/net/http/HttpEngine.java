// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.net.ssl.HttpsURLConnection;

/**
 * An engine to process {@link UrlRequest}s, which uses the best HTTP stack available on the current
 * platform. An instance of this class can be created using {@link Builder}.
 */
// SuppressLint: Making the HttpEngine AutoCloseable indicates to the developers that it's
// expected to be used in a try-with-resource clause. This in turn promotes local, narrowly
// scoped instances of HttpEngine. That's the exact opposite of how HttpEngine is supposed
// to be used - it should live in an application-wide scope and be reused multiple times across
// the lifespan of the app.
@SuppressLint("NotCloseable")
public abstract class HttpEngine {

    /**
     * {@hide}
     */
    protected HttpEngine() {}

    /**
     * Returns a new {@link Builder} object that facilitates creating a {@link HttpEngine}.
     *
     * {@hide}
     */
    @NonNull
    public static Builder builder(@NonNull Context context) {
        return new Builder(context);
    }

    /**
     * The value of the active request count is unknown
     *
     * {@hide}
     */
    public static final int ACTIVE_REQUEST_COUNT_UNKNOWN = -1;

    /**
     * The value of a connection metric is unknown.
     *
     * {@hide}
     */
    public static final int CONNECTION_METRIC_UNKNOWN = -1;

    /**
     * The estimate of the effective connection type is unknown.
     *
     * {@hide}
     *
     * @see #getEffectiveConnectionType
     */
    public static final int EFFECTIVE_CONNECTION_TYPE_UNKNOWN = 0;

    /**
     * The device is offline.
     *
     * @see #getEffectiveConnectionType
     *
     * {@hide}
     */
    public static final int EFFECTIVE_CONNECTION_TYPE_OFFLINE = 1;

    /**
     * The estimate of the effective connection type is slow 2G.
     *
     * @see #getEffectiveConnectionType
     *
     * {@hide}
     */
    public static final int EFFECTIVE_CONNECTION_TYPE_SLOW_2G = 2;

    /**
     * The estimate of the effective connection type is 2G.
     *
     * @see #getEffectiveConnectionType
     *
     * {@hide}
     */
    public static final int EFFECTIVE_CONNECTION_TYPE_2G = 3;

    /**
     * The estimate of the effective connection type is 3G.
     *
     * @see #getEffectiveConnectionType
     *
     * {@hide}
     */
    public static final int EFFECTIVE_CONNECTION_TYPE_3G = 4;

    /**
     * The estimate of the effective connection type is 4G.
     *
     * @see #getEffectiveConnectionType
     *
     * {@hide}
     */
    public static final int EFFECTIVE_CONNECTION_TYPE_4G = 5;

    /**
     * A builder for {@link HttpEngine}s, which allows runtime configuration of
     * {@link HttpEngine}. Configuration options are set on the builder and
     * then {@link #build} is called to create the {@link HttpEngine}.
     */
    // NOTE(kapishnikov): In order to avoid breaking the existing API clients, all future methods
    // added to this class and other API classes must have default implementation.
    // SuppressLint: Builder can not be final since ExperimentalHttpEngine.Builder inherit this
    // Builder.
    @SuppressLint("StaticFinalBuilder")
    public static class Builder {

        /**
         * Reference to the actual builder implementation. {@hide exclude from JavaDoc}.
         */
        protected final IHttpEngineBuilder mBuilderDelegate;

        /**
         * Constructs a {@link Builder} object that facilitates creating a
         * {@link HttpEngine}. The default configuration enables HTTP/2 and
         * QUIC, but disables the HTTP cache.
         *
         * @param context Android {@link Context}, which is used by {@link Builder} to retrieve the
         * application context. A reference to only the application context will be kept, so as to
         * avoid extending the lifetime of {@code context} unnecessarily.
         */
        public Builder(@NonNull Context context) {
            this(createBuilderDelegate(context));
        }

        /**
         * Constructs {@link Builder} with a given delegate that provides the actual implementation
         * of the {@code Builder} methods. This constructor is used only by the internal
         * implementation.
         *
         * @param builderDelegate delegate that provides the actual implementation.
         *
         * {@hide}
         */
        Builder(@NonNull IHttpEngineBuilder builderDelegate) {
            mBuilderDelegate = builderDelegate;
        }

        /**
         * Constructs a default User-Agent string including the system build version, model and id,
         * and the HTTP stack version.
         *
         * @return User-Agent string.
         */
        // SuppressLint: API to get default user agent that could include system build version,
        // model, Id, and Cronet version.
        @NonNull @SuppressLint("GetterOnBuilder")
        public String getDefaultUserAgent() {
            return mBuilderDelegate.getDefaultUserAgent();
        }

        /**
         * Overrides the User-Agent header for all requests. An explicitly set User-Agent header
         * (set using {@link UrlRequest.Builder#addHeader}) will override a value set using this
         * function.
         *
         * @param userAgent the User-Agent string to use for all requests.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setUserAgent(@NonNull String userAgent) {
            mBuilderDelegate.setUserAgent(userAgent);
            return this;
        }

        /**
         * Sets directory for HTTP Cache and Cookie Storage. The directory must
         * exist.
         * <p>
         * <b>NOTE:</b> Do not use the same storage directory with more than one
         * {@link HttpEngine} at a time. Access to the storage directory does
         * not support concurrent access by multiple {@link HttpEngine} instances.
         *
         * @param value path to existing directory.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setStoragePath(@NonNull String value) {
            mBuilderDelegate.setStoragePath(value);
            return this;
        }

        /**
         * Sets whether <a href="https://www.chromium.org/quic">QUIC</a> protocol
         * is enabled. Defaults to enabled.
         *
         * @param value {@code true} to enable QUIC, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setEnableQuic(boolean value) {
            mBuilderDelegate.enableQuic(value);
            return this;
        }

        /**
         * Sets whether <a href="https://tools.ietf.org/html/rfc7540">HTTP/2</a> protocol is
         * enabled. Defaults to enabled.
         *
         * @param value {@code true} to enable HTTP/2, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setEnableHttp2(boolean value) {
            mBuilderDelegate.enableHttp2(value);
            return this;
        }

        /**
         * Sets whether <a href="https://tools.ietf.org/html/rfc7932">Brotli</a> compression is
         * enabled. If enabled, Brotli will be advertised in Accept-Encoding request headers.
         * Defaults to disabled.
         *
         * @param value {@code true} to enable Brotli, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setEnableBrotli(boolean value) {
            mBuilderDelegate.enableBrotli(value);
            return this;
        }

        /**
         * Setting to disable HTTP cache. Some data may still be temporarily stored in memory.
         * Passed to {@link #setEnableHttpCache}.
         */
        public static final int HTTP_CACHE_DISABLED = 0;

        /**
         * Setting to enable in-memory HTTP cache, including HTTP data.
         * Passed to {@link #setEnableHttpCache}.
         */
        public static final int HTTP_CACHE_IN_MEMORY = 1;

        /**
         * Setting to enable on-disk cache, excluding HTTP data.
         * {@link #setStoragePath} must be called prior to passing this constant to
         * {@link #setEnableHttpCache}.
         */
        public static final int HTTP_CACHE_DISK_NO_HTTP = 2;

        /**
         * Setting to enable on-disk cache, including HTTP data.
         * {@link #setStoragePath} must be called prior to passing this constant to
         * {@link #setEnableHttpCache}.
         */
        public static final int HTTP_CACHE_DISK = 3;

        /**
         * Enables or disables caching of HTTP data and other information like QUIC server
         * information.
         *
         * @param cacheMode control location and type of cached data. Must be one of {@link
         * #HTTP_CACHE_DISABLED HTTP_CACHE_*}.
         * @param maxSize maximum size in bytes used to cache data (advisory and maybe exceeded at
         * times).
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setEnableHttpCache(int cacheMode, long maxSize) {
            mBuilderDelegate.enableHttpCache(cacheMode, maxSize);
            return this;
        }

        /**
         * Adds hint that {@code host} supports QUIC.
         * Note that {@link #setEnableHttpCache enableHttpCache}
         * ({@link #HTTP_CACHE_DISK}) is needed to take advantage of 0-RTT
         * connection establishment between sessions.
         *
         * @param host hostname of the server that supports QUIC.
         * @param port host of the server that supports QUIC.
         * @param alternatePort alternate port to use for QUIC.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder addQuicHint(@NonNull String host, int port, int alternatePort) {
            mBuilderDelegate.addQuicHint(host, port, alternatePort);
            return this;
        }

        /**
         * Pins a set of public keys for a given host. By pinning a set of public keys, {@code
         * pinsSha256}, communication with {@code hostName} is required to authenticate with a
         * certificate with a public key from the set of pinned ones. An app can pin the public key
         * of the root certificate, any of the intermediate certificates or the end-entry
         * certificate. Authentication will fail and secure communication will not be established if
         * none of the public keys is present in the host's certificate chain, even if the host
         * attempts to authenticate with a certificate allowed by the device's trusted store of
         * certificates.
         *
         * <p>Calling this method multiple times with the same host name overrides the previously
         * set pins for the host.
         *
         * <p>More information about the public key pinning can be found in <a
         * href="https://tools.ietf.org/html/rfc7469">RFC 7469</a>.
         *
         * @param hostName name of the host to which the public keys should be pinned. A host that
         * consists only of digits and the dot character is treated as invalid.
         * @param pinsSha256 a set of pins. Each pin is the SHA-256 cryptographic hash of the
         * DER-encoded ASN.1 representation of the Subject Public Key Info (SPKI) of the host's
         * X.509 certificate. Use {@link java.security.cert.Certificate#getPublicKey()
         * Certificate.getPublicKey()} and {@link java.security.Key#getEncoded() Key.getEncoded()}
         * to obtain DER-encoded ASN.1 representation of the SPKI. Although, the method does not
         * mandate the presence of the backup pin that can be used if the control of the primary
         * private key has been lost, it is highly recommended to supply one.
         * @param includeSubdomains indicates whether the pinning policy should be applied to
         *                          subdomains of {@code hostName}.
         * @param expirationInstant specifies the expiration instant for the pins.
         * @return the builder to facilitate chaining.
         * @throws NullPointerException if any of the input parameters are {@code null}.
         * @throws IllegalArgumentException if the given host name is invalid or {@code pinsSha256}
         * contains a byte array that does not represent a valid SHA-256 hash.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder addPublicKeyPins(@NonNull String hostName, @NonNull Set<byte[]> pinsSha256,
                boolean includeSubdomains, @NonNull Instant expirationInstant) {
            mBuilderDelegate.addPublicKeyPins(
                    hostName, pinsSha256, includeSubdomains, expirationInstant);
            return this;
        }

        /**
         * Enables or disables public key pinning bypass for local trust anchors. Disabling the
         * bypass for local trust anchors is highly discouraged since it may prohibit the app from
         * communicating with the pinned hosts. E.g., a user may want to send all traffic through an
         * SSL enabled proxy by changing the device proxy settings and adding the proxy certificate
         * to the list of local trust anchor. Disabling the bypass will most likely prevent the app
         * from sending any traffic to the pinned hosts. For more information see 'How does key
         * pinning interact with local proxies and filters?' at
         * https://www.chromium.org/Home/chromium-security/security-faq
         *
         * @param value {@code true} to enable the bypass, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setEnablePublicKeyPinningBypassForLocalTrustAnchors(boolean value) {
            mBuilderDelegate.enablePublicKeyPinningBypassForLocalTrustAnchors(value);
            return this;
        }

        /**
         * Configures the behavior of the HTTP stack when using QUIC. For more details, see
         * documentation of {@link QuicOptions} and the individual methods
         * of {@link QuicOptions.Builder}.
         *
         * <p>Only relevant if {@link #setEnableQuic(boolean)} is enabled.
         *
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        @QuicOptions.Experimental
        public Builder setQuicOptions(@NonNull QuicOptions quicOptions) {
            mBuilderDelegate.setQuicOptions(quicOptions);
            return this;
        }

        /**
         * @see #setQuicOptions(QuicOptions)
         *
         * {@hide}
         */
        @NonNull
        @QuicOptions.Experimental
        public Builder setQuicOptions(@NonNull QuicOptions.Builder quicOptionsBuilder) {
            return setQuicOptions(quicOptionsBuilder.build());
        }

        /**
         * Configures the behavior of hostname lookup. For more details, see documentation
         * of {@link DnsOptions} and the individual methods of {@link DnsOptions.Builder}.
         *
         * <p>Only relevant if {@link #setEnableQuic(boolean)} is enabled.
         *
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        @DnsOptions.Experimental
        public Builder setDnsOptions(@NonNull DnsOptions dnsOptions) {
            mBuilderDelegate.setDnsOptions(dnsOptions);
            return this;
        }

        /**
         * @see #setDnsOptions(DnsOptions)
         *
         * {@hide}
         */
        @NonNull
        @DnsOptions.Experimental
        public Builder setDnsOptions(@NonNull DnsOptions.Builder dnsOptions) {
            return setDnsOptions(dnsOptions.build());
        }

        /**
         * Configures the behavior of connection migration. For more details, see documentation
         * of {@link ConnectionMigrationOptions} and the individual methods of {@link
         * ConnectionMigrationOptions.Builder}.
         *
         * <p>Only relevant if {@link #setEnableQuic(boolean)} is enabled.
         *
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: Value is passed to JNI code and maintained by JNI code after build
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        @ConnectionMigrationOptions.Experimental
        public Builder setConnectionMigrationOptions(
                @NonNull ConnectionMigrationOptions connectionMigrationOptions) {
            mBuilderDelegate.setConnectionMigrationOptions(connectionMigrationOptions);
            return this;
        }

        /**
         * @see #setConnectionMigrationOptions(ConnectionMigrationOptions)
         *
         * {@hide}
         */
        @NonNull
        @ConnectionMigrationOptions.Experimental
        public Builder setConnectionMigrationOptions(
                @NonNull ConnectionMigrationOptions.Builder connectionMigrationOptionsBuilder) {
            return setConnectionMigrationOptions(connectionMigrationOptionsBuilder.build());
        }

        /**
         * Sets the thread priority of Cronet's internal thread.
         *
         * @param priority the thread priority of Cronet's internal thread. A Linux priority level,
         *         from
         * -20 for highest scheduling priority to 19 for lowest scheduling priority. For more
         * information on values, see {@link android.os.Process#setThreadPriority(int, int)} and
         * {@link android.os.Process#THREAD_PRIORITY_DEFAULT THREAD_PRIORITY_*} values.
         * @return the builder to facilitate chaining.
         *
         * {@hide}
         */
        public Builder setThreadPriority(int priority) {
            mBuilderDelegate.setThreadPriority(priority);
            return this;
        }

        /**
         * Enables the network quality estimator, which collects and reports measurements of round
         * trip time (RTT) and downstream throughput at various layers of the network stack. After
         * enabling the estimator, listeners of RTT and throughput can be added with {@link
         * #addRttListener} and
         * {@link #addThroughputListener} and removed with {@link #removeRttListener} and {@link
         * #removeThroughputListener}. The estimator uses memory and CPU only when enabled.
         *
         * @param value {@code true} to enable network quality estimator, {@code false} to disable.
         * @return the builder to facilitate chaining.
         *
         * {@hide}
         */
        public Builder enableNetworkQualityEstimator(boolean value) {
            mBuilderDelegate.enableNetworkQualityEstimator(value);
            return this;
        }

        /**
         * Build a {@link HttpEngine} using this builder's configuration.
         * @return constructed {@link HttpEngine}.
         */
        @NonNull
        public HttpEngine build() {
            return mBuilderDelegate.build();
        }

        /**
         * Creates an implementation of {@link IHttpEngineBuilder} that can be used
         * to delegate the builder calls to.
         *
         * @param context Android Context to use.
         * @return the created {@link IHttpEngineBuilder}.
         */
        private static IHttpEngineBuilder createBuilderDelegate(Context context) {
            try {
                Class<?> clazz = context.getClassLoader().loadClass(
                        "android.net.connectivity.org.chromium.net.impl.NativeCronetEngineBuilderImpl");

                return (IHttpEngineBuilder) clazz.getConstructor(Context.class).newInstance(
                        context);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * @return a human-readable version string of the engine.
     */
    @NonNull
    public static String getVersionString() {
        return ApiVersion.getCronetVersion();
    }

    /**
     * Shuts down the {@link HttpEngine} if there are no active requests,
     * otherwise throws an exception.
     *
     * Cannot be called on network thread - the thread the HTTP stack calls into
     * Executor on (which is different from the thread the Executor invokes
     * callbacks on). May block until all the {@link HttpEngine} resources have been cleaned up.
     */
    public abstract void shutdown();

    /**
     * Starts NetLog logging to a file. The NetLog will contain events emitted
     * by all live {@link HttpEngine} instances. The NetLog is useful for debugging.
     * The file can be viewed using a Chrome browser navigated to
     * chrome://net-internals/#import
     * @param fileName the complete file path. It must not be empty. If the file
     *            exists, it is truncated before starting. If actively logging,
     *            this method is ignored.
     * @param logAll {@code true} to include basic events, user cookies,
     *            credentials and all transferred bytes in the log. This option presents
     *            a privacy risk, since it exposes the user's credentials, and should
     *            only be used with the user's consent and in situations where the log
     *            won't be public.
     *            {@code false} to just include basic events.
     *
     * {@hide}
     */
    public void startNetLogToFile(@NonNull String fileName, boolean logAll) {}

    /**
     * Stops NetLog logging and flushes file to disk. If a logging session is
     * not in progress, this call is ignored.
     *
     * {@hide}
     */
    public void stopNetLog() {}

    /**
     * Returns differences in metrics collected by Cronet since the last call to
     * this method.
     * <p>
     * Cronet collects these metrics globally. This means deltas returned by
     * {@code getGlobalMetricsDeltas()} will include measurements of requests
     * processed by other {@link HttpEngine} instances. Since this function
     * returns differences in metrics collected since the last call, and these
     * metrics are collected globally, a call to any {@code CronetEngine}
     * instance's {@code getGlobalMetricsDeltas()} method will affect the deltas
     * returned by any other {@code CronetEngine} instance's
     * {@code getGlobalMetricsDeltas()}.
     * <p>
     * Cronet starts collecting these metrics after the first call to
     * {@code getGlobalMetricsDeltras()}, so the first call returns no
     * useful data as no metrics have yet been collected.
     *
     * @return differences in metrics collected by Cronet, since the last call
     *         to {@code getGlobalMetricsDeltas()}, serialized as a
     *         <a href=https://developers.google.com/protocol-buffers>protobuf
     *         </a>, or an empty array if collecting metrics is not supported.
     *
     * {@hide}
     */
    @NonNull
    public byte[] getGlobalMetricsDeltas() {
        return new byte[0];
    }

    /**
     * Binds the engine to the specified network. All requests created through this engine
     * will use the network associated to this handle. If this network disconnects all requests will
     * fail, the exact error will depend on the stage of request processing when the network
     * disconnects.
     *
     * @param network the network to bind the engine to. Specify {@code null} to unbind.
     */
    public void bindToNetwork(@Nullable Network network) {}

    /**
     * Establishes a new connection to the resource specified by the {@link URL} {@code url}.
     * <p>
     * <b>Note:</b> This {@link java.net.HttpURLConnection} implementation is subject to certain
     * limitations, see {@link #createUrlStreamHandlerFactory} for details.
     *
     * @param url URL of resource to connect to.
     * @return an {@link java.net.HttpURLConnection} instance implemented
     *     by this {@link HttpEngine}.
     * @throws IOException if an error occurs while opening the connection.
     */
    // SuppressLint since this is for interface parity with j.n.URLConnection
    @SuppressLint("AndroidUri") @NonNull
    public abstract URLConnection openConnection(
            @SuppressLint("AndroidUri") @NonNull URL url) throws IOException;

    /**
     * Creates a {@link URLStreamHandlerFactory} to handle HTTP and HTTPS
     * traffic. An instance of this class can be installed via
     * {@link URL#setURLStreamHandlerFactory} thus using this {@link HttpEngine} by default for
     * all requests created via {@link URL#openConnection}.
     * <p>
     * This {@link java.net.HttpURLConnection} implementation does not implement all features
     * offered by the API:
     * <ul>
     * <li>the HTTP cache installed via
     *     {@link HttpResponseCache#install(java.io.File, long) HttpResponseCache.install()}</li>
     * <li>the HTTP authentication method installed via
     *     {@link java.net.Authenticator#setDefault}</li>
     * <li>the HTTP cookie storage installed via {@link java.net.CookieHandler#setDefault}</li>
     * </ul>
     * <p>
     * While we support and encourages requests using the HTTPS protocol, we don't provide support
     * for the {@link HttpsURLConnection} API. This lack of support also includes not using certain
     * HTTPS features provided via {@link HttpsURLConnection}:
     * <ul>
     * <li>the HTTPS hostname verifier installed via {@link
     *   HttpsURLConnection#setDefaultHostnameVerifier(javax.net.ssl.HostnameVerifier)
     *     HttpsURLConnection.setDefaultHostnameVerifier()}</li>
     * <li>the HTTPS socket factory installed via {@link
     *   HttpsURLConnection#setDefaultSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
     *     HttpsURLConnection.setDefaultSSLSocketFactory()}</li>
     * </ul>
     *
     * @return an {@link URLStreamHandlerFactory} instance implemented by this
     *         {@link HttpEngine}.
     */
    // SuppressLint since this is for interface parity with j.n.URLStreamHandlerFactory
    @SuppressLint("AndroidUri") @NonNull
    public abstract URLStreamHandlerFactory createUrlStreamHandlerFactory();

    /**
     * Creates a builder for {@link UrlRequest}. All callbacks for
     * generated {@link UrlRequest} objects will be invoked on
     * {@code executor}'s threads. {@code executor} must not run tasks on the
     * thread calling {@link Executor#execute} to prevent blocking networking
     * operations and causing exceptions during shutdown.
     *
     * @param url URL for the generated requests.
     * @param executor {@link Executor} on which all callbacks will be invoked.
     * @param callback callback object that gets invoked on different events.
     */
    @NonNull
    public abstract UrlRequest.Builder newUrlRequestBuilder(
            @NonNull String url, @NonNull Executor executor, @NonNull UrlRequest.Callback callback);

    /**
     * Creates a builder for {@link UrlRequest}. All callbacks for generated {@link UrlRequest}
     * objects will be invoked on {@code executor}'s threads. {@code executor} must not run tasks on
     * the thread calling {@link Executor#execute} to prevent blocking networking operations and
     * causing exceptions during shutdown.
     *
     * @param url URL for the generated requests.
     * @param callback callback object that gets invoked on different events.
     * @param executor {@link Executor} on which all callbacks will be invoked.
     *
     * @hide
     */
    // This API is kept for the backward compatibility in upstream
    @NonNull
    public UrlRequest.Builder newUrlRequestBuilder(@NonNull String url,
            @NonNull UrlRequest.Callback callback, @NonNull Executor executor) {
        return newUrlRequestBuilder(url, executor, callback);
    }

    /**
     * Creates a builder for {@link BidirectionalStream} objects. All callbacks for
     * generated {@code BidirectionalStream} objects will be invoked on
     * {@code executor}. {@code executor} must not run tasks on the
     * current thread, otherwise the networking operations may block and exceptions
     * may be thrown at shutdown time.
     *
     * @param url URL for the generated streams.
     * @param executor the {@link Executor} on which {@code callback} methods will be invoked.
     * @param callback the {@link BidirectionalStream.Callback} object that gets invoked upon
     * different events occurring.
     *
     * @return the created builder.
     */
    @NonNull
    public abstract BidirectionalStream.Builder newBidirectionalStreamBuilder(
            @NonNull String url, @NonNull Executor executor,
            @NonNull BidirectionalStream.Callback callback);

    /**
     * Creates a builder for {@link BidirectionalStream} objects. All callbacks for
     * generated {@code BidirectionalStream} objects will be invoked on
     * {@code executor}. {@code executor} must not run tasks on the
     * current thread, otherwise the networking operations may block and exceptions
     * may be thrown at shutdown time.
     *
     * @param url URL for the generated streams.
     * @param callback the {@link BidirectionalStream.Callback} object that gets invoked upon
     * different events occurring.
     * @param executor the {@link Executor} on which {@code callback} methods will be invoked.
     *
     * @return the created builder.
     *
     * @hide
     */
    // This API is kept for the backward compatibility in upstream
    @NonNull
    public BidirectionalStream.Builder newBidirectionalStreamBuilder(
            @NonNull String url, @NonNull BidirectionalStream.Callback callback,
            @NonNull Executor executor) {
        return newBidirectionalStreamBuilder(url, executor, callback);
    }

    /**
     * Returns the number of in-flight requests.
     * <p>
     * A request is in-flight if its start() method has been called but it hasn't reached a final
     * state yet. A request reaches the final state when one of the following callbacks has been
     * called:
     * <ul>
     *    <li>onSucceeded</li>
     *    <li>onCanceled</li>
     *    <li>onFailed</li>
     * </ul>
     *
     * <a href="https://developer.android.com/guide/topics/connectivity/cronet/lifecycle">Cronet
     *         requests's lifecycle</a> for more information.
     *
     * {@hide}
     */
    public int getActiveRequestCount() {
        return ACTIVE_REQUEST_COUNT_UNKNOWN;
    }

    /**
     * Registers a listener that gets called after the end of each request with the request info.
     *
     * <p>The listener is called on an {@link java.util.concurrent.Executor} provided by the
     * listener.
     *
     * @param listener the listener for finished requests.
     *
     * {@hide}
     */
    public void addRequestFinishedListener(RequestFinishedInfo.Listener listener) {}

    /**
     * Removes a finished request listener.
     *
     * @param listener the listener to remove.
     *
     * {@hide}
     */
    public void removeRequestFinishedListener(RequestFinishedInfo.Listener listener) {}

    /**
     * Returns the HTTP RTT estimate (in milliseconds) computed by the network quality estimator.
     * Set to {@link #CONNECTION_METRIC_UNKNOWN} if the value is unavailable. This must be called
     * after
     * {@link Builder#enableNetworkQualityEstimator}, and will throw an exception otherwise.
     *
     * @return Estimate of the HTTP RTT in milliseconds.
     *
     * {@hide}
     */
    public int getHttpRttMs() {
        return CONNECTION_METRIC_UNKNOWN;
    }

    /**
     * Returns the transport RTT estimate (in milliseconds) computed by the network quality
     * estimator. Set to {@link #CONNECTION_METRIC_UNKNOWN} if the value is unavailable. This must
     * be called after {@link Builder#enableNetworkQualityEstimator}, and will throw an exception
     * otherwise.
     *
     * @return Estimate of the transport RTT in milliseconds.
     *
     * {@hide}
     */
    public int getTransportRttMs() {
        return CONNECTION_METRIC_UNKNOWN;
    }

    /**
     * Returns the downstream throughput estimate (in kilobits per second) computed by the network
     * quality estimator. Set to {@link #CONNECTION_METRIC_UNKNOWN} if the value is unavailable.
     * This must be called after {@link Builder#enableNetworkQualityEstimator}, and will throw an
     * exception otherwise.
     *
     * @return Estimate of the downstream throughput in kilobits per second.
     *
     * {@hide}
     */
    public int getDownstreamThroughputKbps() {
        return CONNECTION_METRIC_UNKNOWN;
    }

    /**
     * Starts NetLog logging to a specified directory with a bounded size. The NetLog will contain
     * events emitted by all live CronetEngines. The NetLog is useful for debugging. Once logging
     * has stopped {@link #stopNetLog}, the data will be written to netlog.json in {@code dirPath}.
     * If logging is interrupted, you can stitch the files found in .inprogress subdirectory
     * manually using:
     * https://chromium.googlesource.com/chromium/src/+/main/net/tools/stitch_net_log_files.py. The
     * log can be viewed using a Chrome browser navigated to chrome://net-internals/#import.
     *
     * @param dirPath the directory where the netlog.json file will be created. dirPath must already
     * exist. NetLog files must not exist in the directory. If actively logging, this method is
     * ignored.
     * @param logAll {@code true} to include basic events, user cookies, credentials and all
     * transferred bytes in the log. This option presents a privacy risk, since it exposes the
     * user's credentials, and should only be used with the user's consent and in situations where
     * the log won't be public. {@code false} to just include basic events.
     * @param maxSize the maximum total disk space in bytes that should be used by NetLog. Actual
     *         disk
     * space usage may exceed this limit slightly.
     *
     * {@hide}
     */
    public void startNetLogToDisk(String dirPath, boolean logAll, int maxSize) {}

    /**
     * Returns an estimate of the effective connection type computed by the network quality
     * estimator. Call {@link Builder#enableNetworkQualityEstimator} to begin computing this value.
     *
     * @return the estimated connection type. The returned value is one of {@link
     * #EFFECTIVE_CONNECTION_TYPE_UNKNOWN EFFECTIVE_CONNECTION_TYPE_* }.
     *
     * {@hide}
     */
    public int getEffectiveConnectionType() {
        return EFFECTIVE_CONNECTION_TYPE_UNKNOWN;
    }

    /**
     * Configures the network quality estimator for testing. This must be called before round trip
     * time and throughput listeners are added, and after the network quality estimator has been
     * enabled.
     *
     * @param useLocalHostRequests include requests to localhost in estimates.
     * @param useSmallerResponses include small responses in throughput estimates.
     * @param disableOfflineCheck when set to true, disables the device offline checks when
     *         computing
     * the effective connection type or when writing the prefs.
     *
     * {@hide}
     */
    @VisibleForTesting
    public void configureNetworkQualityEstimatorForTesting(boolean useLocalHostRequests,
            boolean useSmallerResponses, boolean disableOfflineCheck) {}

    /**
     * Registers a listener that gets called whenever the network quality estimator witnesses a
     * sample round trip time. This must be called after {@link
     * Builder#enableNetworkQualityEstimator}, and with throw an exception otherwise. Round trip
     * times may be recorded at various layers of the network stack, including TCP, QUIC, and at the
     * URL request layer. The listener is called on the
     * {@link java.util.concurrent.Executor} that is passed to {@link
     * Builder#enableNetworkQualityEstimator}.
     *
     * @param listener the listener of round trip times.
     *
     * {@hide}
     */
    public void addRttListener(NetworkQualityRttListener listener) {}

    /**
     * Removes a listener of round trip times if previously registered with {@link #addRttListener}.
     * This should be called after a {@link NetworkQualityRttListener} is added in order to stop
     * receiving observations.
     *
     * @param listener the listener of round trip times.
     *
     * {@hide}
     */
    public void removeRttListener(NetworkQualityRttListener listener) {}

    /**
     * Registers a listener that gets called whenever the network quality estimator witnesses a
     * sample throughput measurement. This must be called after {@link
     * Builder#enableNetworkQualityEstimator}. Throughput observations are computed by measuring
     * bytes read over the active network interface at times when at least one URL response is being
     * received. The listener is called on the {@link java.util.concurrent.Executor} that is passed
     * to {@link Builder#enableNetworkQualityEstimator}.
     *
     * @param listener the listener of throughput.
     *
     * {@hide}
     */
    public void addThroughputListener(NetworkQualityThroughputListener listener) {}

    /**
     * Removes a listener of throughput. This should be called after a {@link
     * NetworkQualityThroughputListener} is added with {@link #addThroughputListener} in order to
     * stop receiving observations.
     *
     * @param listener the listener of throughput.
     *
     * {@hide}
     */
    public void removeThroughputListener(NetworkQualityThroughputListener listener) {}
}
