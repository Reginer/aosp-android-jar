// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package android.net.http;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

/**
 * Defines methods that the actual implementation of {@link HttpEngine.Builder} has to implement.
 * {@link HttpEngine.Builder} uses this interface to delegate the calls.
 * For the documentation of individual methods, please see the identically named methods in
 * {@link HttpEngine.Builder} and
 * {@link ExperimentalHttpEngine.Builder}.
 *
 * <p>{@hide internal class}
 */
public abstract class IHttpEngineBuilder {
    // The fields below list values which are known to getSupportedConfigOptions().
    //
    // Given the fields are final the constant value associated with them is compiled into
    // class using them. This makes it safe for all implementation to use the field in their code
    // and not worry about version skew (new implementation aware of values the old API is not),
    // as long as the values don't change meaning. This isn't true of enums and other dynamic
    // structures, hence we resort to plain old good ints.
    public static final int CONNECTION_MIGRATION_OPTIONS = 1;
    public static final int DNS_OPTIONS = 2;
    public static final int QUIC_OPTIONS = 3;

    // Public API methods.
    public abstract IHttpEngineBuilder addPublicKeyPins(String hostName, Set<byte[]> pinsSha256,
            boolean includeSubdomains, Instant expirationInstant);

    public abstract IHttpEngineBuilder addQuicHint(String host, int port, int alternatePort);

    public abstract IHttpEngineBuilder enableHttp2(boolean value);

    public abstract IHttpEngineBuilder enableHttpCache(int cacheMode, long maxSize);

    public abstract IHttpEngineBuilder enablePublicKeyPinningBypassForLocalTrustAnchors(
            boolean value);

    public abstract IHttpEngineBuilder enableQuic(boolean value);

    public abstract IHttpEngineBuilder enableSdch(boolean value);

    public IHttpEngineBuilder enableBrotli(boolean value) {
        // Do nothing for older implementations.
        return this;
    }

    public IHttpEngineBuilder setQuicOptions(QuicOptions quicOptions) {
        return this;
    }

    public IHttpEngineBuilder setDnsOptions(DnsOptions dnsOptions) {
        return this;
    }

    public IHttpEngineBuilder setConnectionMigrationOptions(
            ConnectionMigrationOptions connectionMigrationOptions) {
        return this;
    }

    public abstract IHttpEngineBuilder setExperimentalOptions(String options);
    public abstract IHttpEngineBuilder setStoragePath(String value);

    public abstract IHttpEngineBuilder setUserAgent(String userAgent);

    public abstract String getDefaultUserAgent();

    public abstract ExperimentalHttpEngine build();

    /**
     * Returns the set of configuration options the builder is able to support natively. This is
     * used internally to emulate newly added functionality using older APIs where possible.
     *
     * <p>The default implementation returns an empty set. Subclasses should override this method to
     * reflect the supported options that are applicable to them.
     */
    protected Set<Integer> getSupportedConfigOptions() {
        return Collections.emptySet();
    }

    // Experimental API methods.
    //
    // Note: all experimental API methods should have default implementation. This will allow
    // removing the experimental methods from the implementation layer without breaking
    // the client.

    public IHttpEngineBuilder enableNetworkQualityEstimator(boolean value) {
        return this;
    }

    public IHttpEngineBuilder setThreadPriority(int priority) {
        return this;
    }
}