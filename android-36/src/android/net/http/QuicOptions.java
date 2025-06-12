// Copyright 2022 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Configuration options for QUIC.
 *
 * <p>The settings in this class are only relevant if QUIC is enabled. Use
 * {@link HttpEngine.Builder#setEnableQuic(boolean)} to enable / disable QUIC for
 * the HTTP engine.
 */
// SuppressLint to be consistent with other cronet code
@SuppressLint("UserHandleName")
public class QuicOptions {
    private final Set<String> mQuicHostAllowlist;
    private final Set<String> mEnabledQuicVersions;

    private final Set<String> mConnectionOptions;
    private final Set<String> mClientConnectionOptions;
    @Nullable
    private final Integer mInMemoryServerConfigsCacheSize;
    @Nullable
    private final String mHandshakeUserAgent;
    @Nullable
    private final Boolean mRetryWithoutAltSvcOnQuicErrors;
    @Nullable
    private final Boolean mEnableTlsZeroRtt;

    @Nullable
    private final Duration mPreCryptoHandshakeIdleTimeout;
    @Nullable
    private final Duration mCryptoHandshakeTimeout;

    @Nullable
    private final Duration mIdleConnectionTimeout;
    @Nullable
    private final Duration mRetransmittableOnWireTimeout;

    @Nullable
    private final Boolean mCloseSessionsOnIpChange;
    @Nullable
    private final Boolean mGoawaySessionsOnIpChange;

    @Nullable
    private final Duration mInitialBrokenServicePeriod;
    @Nullable
    private final Boolean mIncreaseBrokenServicePeriodExponentially;
    @Nullable
    private final Boolean mDelayJobsWithAvailableSpdySession;

    private final Set<String> mExtraQuicheFlags;

    QuicOptions(Builder builder) {
        this.mQuicHostAllowlist =
                Collections.unmodifiableSet(new LinkedHashSet<>(builder.mQuicHostAllowlist));
        this.mEnabledQuicVersions =
                Collections.unmodifiableSet(new LinkedHashSet<>(builder.mEnabledQuicVersions));
        this.mConnectionOptions =
                Collections.unmodifiableSet(new LinkedHashSet<>(builder.mConnectionOptions));
        this.mClientConnectionOptions =
                Collections.unmodifiableSet(new LinkedHashSet<>(builder.mClientConnectionOptions));
        this.mInMemoryServerConfigsCacheSize = builder.mInMemoryServerConfigsCacheSize;
        this.mHandshakeUserAgent = builder.mHandshakeUserAgent;
        this.mRetryWithoutAltSvcOnQuicErrors = builder.mRetryWithoutAltSvcOnQuicErrors;
        this.mEnableTlsZeroRtt = builder.mEnableTlsZeroRtt;
        this.mPreCryptoHandshakeIdleTimeout = builder.mPreCryptoHandshakeIdleTimeout;
        this.mCryptoHandshakeTimeout = builder.mCryptoHandshakeTimeout;
        this.mIdleConnectionTimeout = builder.mIdleConnectionTimeout;
        this.mRetransmittableOnWireTimeout = builder.mRetransmittableOnWireTimeout;
        this.mCloseSessionsOnIpChange = builder.mCloseSessionsOnIpChange;
        this.mGoawaySessionsOnIpChange = builder.mGoawaySessionsOnIpChange;
        this.mInitialBrokenServicePeriod = builder.mInitialBrokenServicePeriod;
        this.mIncreaseBrokenServicePeriodExponentially =
                builder.mIncreaseBrokenServicePeriodExponentially;
        this.mDelayJobsWithAvailableSpdySession = builder.mDelayJobsWithAvailableSpdySession;
        this.mExtraQuicheFlags =
                Collections.unmodifiableSet(new LinkedHashSet<>(builder.mExtraQuicheFlags));
    }

    /**
     * See {@link Builder#addAllowedQuicHost}
     */
    @NonNull
    public Set<String> getAllowedQuicHosts() {
        return mQuicHostAllowlist;
    }


    /**
     * See {@link Builder#setInMemoryServerConfigsCacheSize}
     */
     public boolean hasInMemoryServerConfigsCacheSize() {
        return mInMemoryServerConfigsCacheSize != null;
     }

    /**
     * See {@link Builder#setInMemoryServerConfigsCacheSize}
     */
    public int getInMemoryServerConfigsCacheSize() {
        if (!hasInMemoryServerConfigsCacheSize()) {
            throw new IllegalStateException("InMemoryServerConfigsCacheSize is not set");
        }
        return mInMemoryServerConfigsCacheSize;
    }

    /**
     * See {@link Builder#setHandshakeUserAgent}
     */
    @Nullable
    public String getHandshakeUserAgent() {
        return mHandshakeUserAgent;
    }

    /**
     * See {@link Builder#setIdleConnectionTimeout}
     */
    @Nullable
    public Duration getIdleConnectionTimeout() {
        return mIdleConnectionTimeout;
    }

    /**
     * Create a new {@code QuicOptions} builder.
     *
     * {@hide}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link QuicOptions}.
     */
    public static final class Builder {
        private final Set<String> mQuicHostAllowlist = new LinkedHashSet<>();
        private final Set<String> mEnabledQuicVersions = new LinkedHashSet<>();
        private final Set<String> mConnectionOptions = new LinkedHashSet<>();
        private final Set<String> mClientConnectionOptions = new LinkedHashSet<>();
        @Nullable
        private Integer mInMemoryServerConfigsCacheSize;
        @Nullable
        private String mHandshakeUserAgent;
        @Nullable
        private Boolean mRetryWithoutAltSvcOnQuicErrors;
        @Nullable
        private Boolean mEnableTlsZeroRtt;
        @Nullable
        private Duration mPreCryptoHandshakeIdleTimeout;
        @Nullable
        private Duration mCryptoHandshakeTimeout;
        @Nullable
        private Duration mIdleConnectionTimeout;
        @Nullable
        private Duration mRetransmittableOnWireTimeout;
        @Nullable
        private Boolean mCloseSessionsOnIpChange;
        @Nullable
        private Boolean mGoawaySessionsOnIpChange;
        @Nullable
        private Duration mInitialBrokenServicePeriod;
        @Nullable
        private Boolean mIncreaseBrokenServicePeriodExponentially;
        @Nullable
        private Boolean mDelayJobsWithAvailableSpdySession;
        private final Set<String> mExtraQuicheFlags = new LinkedHashSet<>();

        public Builder() {}

        /**
         * Adds a host to the QUIC allowlist.
         *
         * <p>If no hosts are specified, the per-host allowlist functionality is disabled.
         * Otherwise, the HTTP stack will only use QUIC when talking to hosts on the allowlist.
         *
         * @return the builder for chaining
         */
        @NonNull
        public Builder addAllowedQuicHost(@NonNull String quicHost) {
            mQuicHostAllowlist.add(quicHost);
            return this;
        }

        /**
         * Sets how many server configurations (metadata like list of alt svc, whether QUIC is
         * supported, etc.) should be held in memory.
         *
         * <p>If the storage path is set ({@link HttpEngine.Builder#setStoragePath(String)},
         * the HTTP stack will also persist the server configurations on disk.
         *
         * @return the builder for chaining
         */
        @NonNull
        public Builder setInMemoryServerConfigsCacheSize(int inMemoryServerConfigsCacheSize) {
            this.mInMemoryServerConfigsCacheSize = inMemoryServerConfigsCacheSize;
            return this;
        }

        /**
         * Sets the user agent to be used outside of HTTP requests (for example for QUIC
         * handshakes).
         *
         * <p>To set the default user agent for HTTP requests, use
         * {@link HttpEngine.Builder#setUserAgent(String)} instead.
         *
         * @return the builder for chaining
         */
        @NonNull
        public Builder setHandshakeUserAgent(@NonNull String handshakeUserAgent) {
            this.mHandshakeUserAgent = handshakeUserAgent;
            return this;
        }

        /**
         * Sets the maximum idle time for a connection. The actual value for the idle timeout is
         * the minimum of this value and the server's and is negotiated during the handshake. Thus,
         * it only applies after the handshake has completed. If no activity is detected
         * on the connection for the set duration, the connection is closed.
         *
         * <p>See <a href="https://www.rfc-editor.org/rfc/rfc9114.html#name-idle-connections">RFC
         * 9114, section 5.1 </a> for more details.
         * 
         * @return the builder for chaining
         */
        @NonNull
        public Builder setIdleConnectionTimeout(@NonNull Duration idleConnectionTimeout) {
            this.mIdleConnectionTimeout = idleConnectionTimeout;
            return this;
        }

        /**
         * Creates and returns the final {@link QuicOptions} instance, based on the values
         * in this builder.
         */
        @NonNull
        public QuicOptions build() {
            return new QuicOptions(this);
        }
    }

    /**
     * An annotation for APIs which are not considered stable yet.
     *
     * <p>Applications using experimental APIs must acknowledge that they're aware of using APIs
     * that are not considered stable. The APIs might change functionality, break or cease to exist
     * without notice.
     *
     * <p>It's highly recommended to reach out to Cronet maintainers ({@code net-dev@chromium.org})
     * before using one of the APIs annotated as experimental outside of debugging
     * and proof-of-concept code. Be ready to help to help polishing the API, or for a "sorry,
     * really not production ready yet".
     *
     * <p>If you still want to use an experimental API in production, you're doing so at your
     * own risk. You have been warned.
     *
     * {@hide}
     */
    public @interface Experimental {}
}
