// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package android.net.http;


import androidx.annotation.NonNull;
import java.time.Instant;
import java.util.Set;

/**
 * Defines methods that the actual implementation of {@link HttpEngine.Builder} has to implement.
 * {@code HttpEngine.Builder} uses this interface to delegate the calls. For the documentation of
 * individual methods, please see the identically named methods in {@link
 * org.chromium.net.HttpEngine.Builder}.
 *
 * {@hide internal class}
 */
public abstract class IHttpEngineBuilder {
    // Public API methods.
    public abstract IHttpEngineBuilder addPublicKeyPins(String hostName, Set<byte[]> pinsSha256,
            boolean includeSubdomains, Instant expirationInstant);

    public abstract IHttpEngineBuilder addQuicHint(String host, int port, int alternatePort);

    public abstract IHttpEngineBuilder setEnableHttp2(boolean value);

    public abstract IHttpEngineBuilder setEnableHttpCache(int cacheMode, long maxSize);

    public abstract IHttpEngineBuilder setEnablePublicKeyPinningBypassForLocalTrustAnchors(
            boolean value);

    public abstract IHttpEngineBuilder setEnableQuic(boolean value);

    public abstract IHttpEngineBuilder setEnableBrotli(boolean value);

    public abstract IHttpEngineBuilder setStoragePath(String value);

    public abstract IHttpEngineBuilder setUserAgent(String userAgent);

    public abstract String getDefaultUserAgent();

    public abstract IHttpEngineBuilder setQuicOptions(@NonNull QuicOptions quicOptions);

    public abstract IHttpEngineBuilder setDnsOptions(@NonNull DnsOptions dnsOptions);

    public abstract IHttpEngineBuilder setConnectionMigrationOptions(@NonNull ConnectionMigrationOptions connectionMigrationOptions);

    public abstract HttpEngine build();
}
