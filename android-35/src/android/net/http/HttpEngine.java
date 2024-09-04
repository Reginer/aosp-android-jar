// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Network;

import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.ICronetEngineBuilder;
import org.chromium.net.ApiVersion;

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
        @VisibleForTesting
        public Builder(@NonNull IHttpEngineBuilder builderDelegate) {
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
            mBuilderDelegate.setEnableQuic(value);
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
            mBuilderDelegate.setEnableHttp2(value);
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
            mBuilderDelegate.setEnableBrotli(value);
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
            mBuilderDelegate.setEnableHttpCache(cacheMode, maxSize);
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
            mBuilderDelegate.setEnablePublicKeyPinningBypassForLocalTrustAnchors(value);
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
                Class<?> cronetClazz = context.getClassLoader().loadClass(
                        "android.net.connectivity.org.chromium.net.impl.NativeCronetEngineBuilderImpl");
                Class<?> aospClazz = context.getClassLoader().loadClass(
                        "android.net.http.CronetEngineBuilderWrapper");

                ICronetEngineBuilder cronetBuilderImpl = (ICronetEngineBuilder)
                        cronetClazz.getConstructor(Context.class).newInstance(context);
                IHttpEngineBuilder aospBuilderImpl = (IHttpEngineBuilder)
                        aospClazz.getConstructor(ExperimentalCronetEngine.Builder.class).newInstance(new ExperimentalCronetEngine.Builder(cronetBuilderImpl));
                return aospBuilderImpl;
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

}
