/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.net;

import static android.net.NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_DUN;
import static android.net.NetworkCapabilities.NET_CAPABILITY_FOREGROUND;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN;
import static android.net.NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY;
import static android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_TRUSTED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;
import static android.net.NetworkCapabilities.RES_ID_UNSET;
import static android.net.NetworkCapabilities.TRANSPORT_TEST;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.NetworkCapabilities.NetCapability;
import android.net.NetworkCapabilities.Transport;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.text.TextUtils;
import android.util.Range;

import com.android.net.flags.Flags;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An object describing a network that the application is interested in.
 *
 * <p>@see <a href="https://developer.android.com/training/basics/network-ops/reading-network-state>
 * this general guide</a> on how to use NetworkCapabilities and related classes.
 *
 * NetworkRequest defines a request for a network, made through
 * {@link NetworkRequest.Builder} and used to request a network via
 * {@link ConnectivityManager#requestNetwork} or to listen for changes
 * via the {@link ConnectivityManager#registerNetworkCallback} family of
 * functions.
 *
 * <p>{@link ConnectivityManager#requestNetwork} will try to find a connected
 * network matching the NetworkRequest, and return it if there is one.
 * As long as the request is outstanding, the system will try to find the best
 * possible network that matches the request. The request will keep up the
 * currently best connected network, and if a better one is found (e.g. cheaper
 * or faster) the system will bring up that better network to better serve the
 * request. A request filed with {@link ConnectivityManager#requestNetwork} will
 * only match one network at a time (the one the system thinks is best), even if
 * other networks can match the request that are being kept up by other requests.
 *
 * For example, an application needing a network with
 * {@link NetworkCapabilities#NET_CAPABILITY_INTERNET} should use
 * {@link ConnectivityManager#requestNetwork} to request the system keeps one up.
 * A general cellular network can satisfy this request, but if the system finds
 * a free Wi-Fi network which is expected to be faster, it will try and connect
 * to that Wi-Fi network and switch the request over to it once it is connected.
 * The cell network may stay connected if there are outstanding requests (from
 * the same app or from other apps on the system) that match the cell network
 * but not the Wi-Fi network, such as a request with {@link NetworkCapabilities#NET_CAPABILITY_MMS}.
 *
 * When a network is no longer needed to serve any request, the system can
 * tear it down at any time and usually does so immediately, so make sure to
 * keep up requests for the networks your app needs.
 *
 * <p>By contrast, requests filed with {@link ConnectivityManager#registerNetworkCallback}
 * will receive callbacks for all matching networks, and will not cause the system to
 * keep up the networks they match. Use this to listen to networks that the device is
 * connected to, but that you don't want the system to keep up for your use case.
 *
 * <p>Applications build a NetworkRequest and pass it to one of the
 * {@link ConnectivityManager} methods above together with a
 * {@link ConnectivityManager.NetworkCallback} object. The callback
 * will then start receiving method calls about networks that match
 * the request.
 *
 * <p>Networks are brought up and/or matched according to the capabilities
 * set in the builder. For example, a request with
 * {@link NetworkCapabilities#NET_CAPABILITY_MMS} lets the system match
 * and/or bring up a network that is capable to send MMS. A request with
 * {@link NetworkCapabilities#NET_CAPABILITY_NOT_METERED} matches a network
 * that doesn't charge the user for usage. See
 * {@link NetworkCapabilities} for a list of capabilities and their
 * description.
 *
 * <p>While all capabilities can be matched with the
 * {@link ConnectivityManager#registerNetworkCallback} family of methods,
 * not all capabilities can be used to request that the system brings
 * up a network with {@link ConnectivityManager#requestNetwork}. For example,
 * an application cannot use {@link ConnectivityManager#requestNetwork} to
 * ask the system to bring up a network with
 * {@link NetworkCapabilities#NET_CAPABILITY_CAPTIVE_PORTAL}, because the
 * system won't know if a network has a captive portal before it connects
 * to that network. Similarly, some capabilities may require a specific
 * permission or privilege to be requested.
 *
 * Look up the specific capability and the {@link ConnectivityManager#requestNetwork}
 * method for limitations applicable to each capability.
 *
 * <p>Also, starting with {@link Build.VERSION_CODES#UPSIDE_DOWN_CAKE}, some capabilities
 * require the application to self-certify by explicitly adding the
 * {@link android.content.pm.PackageManager#PROPERTY_SELF_CERTIFIED_NETWORK_CAPABILITIES}
 * property in the AndroidManifest.xml, which points to an XML resource file. In the
 * XML resource file, the application declares what kind of network capabilities the application
 * wants to have.
 *
 * Here is an example self-certification XML resource file :
 * <pre>
 *  {@code
 *  <network-capabilities-declaration xmlns:android="http://schemas.android.com/apk/res/android">
 *     <uses-network-capability android:name="NET_CAPABILITY_PRIORITIZE_LATENCY"/>
 *     <uses-network-capability android:name="NET_CAPABILITY_PRIORITIZE_BANDWIDTH"/>
 * </network-capabilities-declaration>
 *  }
 *  </pre>
 * Look up the specific capability to learn whether its usage requires this self-certification.
 */
public class NetworkRequest implements Parcelable {
    /**
     * The first requestId value that will be allocated.
     * @hide only used by ConnectivityService.
     */
    public static final int FIRST_REQUEST_ID = 1;

    /**
     * The requestId value that represents the absence of a request.
     * @hide only used by ConnectivityService.
     */
    public static final int REQUEST_ID_NONE = -1;

    /**
     * The {@link NetworkCapabilities} that define this request.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final @NonNull NetworkCapabilities networkCapabilities;

    /**
     * Identifies the request.  NetworkRequests should only be constructed by
     * the Framework and given out to applications as tokens to be used to identify
     * the request.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final int requestId;

    /**
     * Set for legacy requests and the default.  Set to TYPE_NONE for none.
     * Causes CONNECTIVITY_ACTION broadcasts to be sent.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public final int legacyType;

    /**
     * A NetworkRequest as used by the system can be one of the following types:
     *
     *     - LISTEN, for which the framework will issue callbacks about any
     *       and all networks that match the specified NetworkCapabilities,
     *
     *     - REQUEST, capable of causing a specific network to be created
     *       first (e.g. a telephony DUN request), the framework will issue
     *       callbacks about the single, highest scoring current network
     *       (if any) that matches the specified NetworkCapabilities, or
     *
     *     - RESERVATION requests behave identically to those of type REQUEST.
     *       For example, unlike LISTEN, they cause networks to remain
     *       connected, and they match exactly one network (the best one).
     *       A RESERVATION generates a unique reservationId in its
     *       NetworkCapabilities by copying the requestId which affects
     *       matching. A NetworkProvider can register a "blanket" NetworkOffer
     *       with reservationId = MATCH_ALL_RESERVATIONS to indicate that it
     *       is capable of generating NetworkOffers in response to RESERVATION
     *       requests.
     *
     *     - TRACK_DEFAULT, which causes the framework to issue callbacks for
     *       the single, highest scoring current network (if any) that will
     *       be chosen for an app, but which cannot cause the framework to
     *       either create or retain the existence of any specific network.
     *
     *     - TRACK_SYSTEM_DEFAULT, which causes the framework to send callbacks
     *       for the network (if any) that satisfies the default Internet
     *       request.
     *
     *     - TRACK_BEST, which causes the framework to send callbacks about
     *       the single, highest scoring current network (if any) that matches
     *       the specified NetworkCapabilities.
     *
     *     - BACKGROUND_REQUEST, like REQUEST but does not cause any networks
     *       to retain the NET_CAPABILITY_FOREGROUND capability. A network with
     *       no foreground requests is in the background. A network that has
     *       one or more background requests and loses its last foreground
     *       request to a higher-scoring network will not go into the
     *       background immediately, but will linger and go into the background
     *       after the linger timeout.
     *
     *     - The value NONE is used only by applications. When an application
     *       creates a NetworkRequest, it does not have a type; the type is set
     *       by the system depending on the method used to file the request
     *       (requestNetwork, registerNetworkCallback, etc.).
     *
     * @hide
     */
    public static enum Type {
        NONE,
        LISTEN,
        TRACK_DEFAULT,
        REQUEST,
        BACKGROUND_REQUEST,
        TRACK_SYSTEM_DEFAULT,
        LISTEN_FOR_BEST,
        RESERVATION,
    };

    /**
     * The type of the request. This is only used by the system and is always NONE elsewhere.
     *
     * @hide
     */
    public final Type type;

    /**
     * @hide
     */
    public NetworkRequest(NetworkCapabilities nc, int legacyType, int rId, Type type) {
        if (nc == null) {
            throw new NullPointerException();
        }
        if (nc.getReservationId() != RES_ID_UNSET) {
            throw new IllegalArgumentException("ReservationId must only be set by the system");
        }
        requestId = rId;
        networkCapabilities = nc;
        if (type == Type.RESERVATION) {
            // Conceptually, the reservationId is not related to the requestId; however, the
            // requestId fulfills the same uniqueness requirements that are needed for the
            // reservationId, so it can be reused for this purpose.
            networkCapabilities.setReservationId(rId);
        }
        this.legacyType = legacyType;
        this.type = type;
    }

    /**
     * @hide
     */
    public NetworkRequest(NetworkRequest that) {
        networkCapabilities = new NetworkCapabilities(that.networkCapabilities);
        requestId = that.requestId;
        this.legacyType = that.legacyType;
        this.type = that.type;
    }

    private NetworkRequest(Parcel in) {
        networkCapabilities = NetworkCapabilities.CREATOR.createFromParcel(in);
        legacyType = in.readInt();
        requestId = in.readInt();
        type = Type.valueOf(in.readString());  // IllegalArgumentException if invalid.
    }

    /**
     * Builder used to create {@link NetworkRequest} objects.  Specify the Network features
     * needed in terms of {@link NetworkCapabilities} features
     */
    public static class Builder {
        /**
         * Capabilities that are currently compatible with VCN networks.
         */
        private static final List<Integer> VCN_SUPPORTED_CAPABILITIES = Arrays.asList(
                NET_CAPABILITY_CAPTIVE_PORTAL,
                NET_CAPABILITY_DUN,
                NET_CAPABILITY_FOREGROUND,
                NET_CAPABILITY_INTERNET,
                NET_CAPABILITY_NOT_CONGESTED,
                NET_CAPABILITY_NOT_METERED,
                NET_CAPABILITY_NOT_RESTRICTED,
                NET_CAPABILITY_NOT_ROAMING,
                NET_CAPABILITY_NOT_SUSPENDED,
                NET_CAPABILITY_NOT_VPN,
                NET_CAPABILITY_PARTIAL_CONNECTIVITY,
                NET_CAPABILITY_TEMPORARILY_NOT_METERED,
                NET_CAPABILITY_TRUSTED,
                NET_CAPABILITY_VALIDATED,
                NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED);

        private final NetworkCapabilities mNetworkCapabilities;

        // A boolean that represents whether the NOT_VCN_MANAGED capability should be deduced when
        // the NetworkRequest object is built.
        private boolean mShouldDeduceNotVcnManaged = true;

        /**
         * Default constructor for Builder.
         */
        public Builder() {
            // By default, restrict this request to networks available to this app.
            // Apps can rescind this restriction, but ConnectivityService will enforce
            // it for apps that do not have the NETWORK_SETTINGS permission.
            mNetworkCapabilities = new NetworkCapabilities();
            mNetworkCapabilities.setSingleUid(Process.myUid());
        }

        /**
         * Creates a new Builder of NetworkRequest from an existing instance.
         */
        public Builder(@NonNull final NetworkRequest request) {
            Objects.requireNonNull(request);
            mNetworkCapabilities = request.networkCapabilities;
            // If the caller constructed the builder from a request, it means the user
            // might explicitly want the capabilities from the request. Thus, the NOT_VCN_MANAGED
            // capabilities should not be touched later.
            mShouldDeduceNotVcnManaged = false;
        }

        /**
         * Build {@link NetworkRequest} give the current set of capabilities.
         */
        public NetworkRequest build() {
            // Make a copy of mNetworkCapabilities so we don't inadvertently remove NOT_RESTRICTED
            // when later an unrestricted capability could be added to mNetworkCapabilities, in
            // which case NOT_RESTRICTED should be returned to mNetworkCapabilities, which
            // maybeMarkCapabilitiesRestricted() doesn't add back.
            final NetworkCapabilities nc = new NetworkCapabilities(mNetworkCapabilities);
            nc.maybeMarkCapabilitiesRestricted();
            deduceNotVcnManagedCapability(nc);
            return new NetworkRequest(nc, ConnectivityManager.TYPE_NONE,
                    ConnectivityManager.REQUEST_ID_UNSET, Type.NONE);
        }

        /**
         * Add the given capability requirement to this builder.  These represent
         * the requested network's required capabilities.  Note that when searching
         * for a network to satisfy a request, all capabilities requested must be
         * satisfied.
         *
         * @param capability The capability to add.
         * @return The builder to facilitate chaining
         *         {@code builder.addCapability(...).addCapability();}.
         */
        public Builder addCapability(@NetworkCapabilities.NetCapability int capability) {
            mNetworkCapabilities.addCapability(capability);
            if (capability == NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED) {
                mShouldDeduceNotVcnManaged = false;
            }
            return this;
        }

        /**
         * Removes (if found) the given capability from this builder instance.
         *
         * @param capability The capability to remove.
         * @return The builder to facilitate chaining.
         */
        public Builder removeCapability(@NetworkCapabilities.NetCapability int capability) {
            mNetworkCapabilities.removeCapability(capability);
            if (capability == NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED) {
                mShouldDeduceNotVcnManaged = false;
            }
            return this;
        }

        /**
         * Set the {@code NetworkCapabilities} for this builder instance,
         * overriding any capabilities that had been previously set.
         *
         * @param nc The superseding {@code NetworkCapabilities} instance.
         * @return The builder to facilitate chaining.
         * @hide
         */
        public Builder setCapabilities(NetworkCapabilities nc) {
            mNetworkCapabilities.set(nc);
            return this;
        }

        /**
         * Sets this request to match only networks that apply to the specified UIDs.
         *
         * By default, the set of UIDs is the UID of the calling app, and this request will match
         * any network that applies to the app. Setting it to {@code null} will observe any
         * network on the system, even if it does not apply to this app. In this case, any
         * {@link NetworkSpecifier} set on this request will be redacted or removed to prevent the
         * application deducing restricted information such as location.
         *
         * @param uids The UIDs as a set of {@code Range<Integer>}, or null for everything.
         * @return The builder to facilitate chaining.
         * @hide
         */
        @NonNull
        @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setUids(@Nullable Set<Range<Integer>> uids) {
            mNetworkCapabilities.setUids(uids);
            return this;
        }

        /**
         * Add a capability that must not exist in the requested network.
         * <p>
         * If the capability was previously added to the list of required capabilities (for
         * example, it was there by default or added using {@link #addCapability(int)} method), then
         * it will be removed from the list of required capabilities as well.
         *
         * @see #addCapability(int)
         *
         * @param capability The capability to add to forbidden capability list.
         * @return The builder to facilitate chaining.
         *
         * @hide
         */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
        // TODO : @FlaggedApi(Flags.FLAG_FORBIDDEN_CAPABILITY) and public
        public Builder addForbiddenCapability(@NetworkCapabilities.NetCapability int capability) {
            mNetworkCapabilities.addForbiddenCapability(capability);
            return this;
        }

        /**
         * Removes (if found) the given forbidden capability from this builder instance.
         *
         * @param capability The forbidden capability to remove.
         * @return The builder to facilitate chaining.
         *
         * @hide
         */
        @NonNull
        @SuppressLint("BuilderSetStyle")
        @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
        // TODO : @FlaggedApi(Flags.FLAG_FORBIDDEN_CAPABILITY) and public
        public Builder removeForbiddenCapability(
                @NetworkCapabilities.NetCapability int capability) {
            mNetworkCapabilities.removeForbiddenCapability(capability);
            return this;
        }

        /**
         * Completely clears all the {@code NetworkCapabilities} from this builder instance,
         * removing even the capabilities that are set by default when the object is constructed.
         * Also removes any set forbidden capabilities.
         *
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder clearCapabilities() {
            mNetworkCapabilities.clearAll();
            // If the caller explicitly clear all capabilities, the NOT_VCN_MANAGED capabilities
            // should not be add back later.
            mShouldDeduceNotVcnManaged = false;
            return this;
        }

        /**
         * Adds the given transport requirement to this builder.  These represent
         * the set of allowed transports for the request.  Only networks using one
         * of these transports will satisfy the request.  If no particular transports
         * are required, none should be specified here.
         *
         * @param transportType The transport type to add.
         * @return The builder to facilitate chaining.
         */
        public Builder addTransportType(@NetworkCapabilities.Transport int transportType) {
            mNetworkCapabilities.addTransportType(transportType);
            return this;
        }

        /**
         * Removes (if found) the given transport from this builder instance.
         *
         * @param transportType The transport type to remove.
         * @return The builder to facilitate chaining.
         */
        public Builder removeTransportType(@NetworkCapabilities.Transport int transportType) {
            mNetworkCapabilities.removeTransportType(transportType);
            return this;
        }

        /**
         * @hide
         */
        public Builder setLinkUpstreamBandwidthKbps(int upKbps) {
            mNetworkCapabilities.setLinkUpstreamBandwidthKbps(upKbps);
            return this;
        }
        /**
         * @hide
         */
        public Builder setLinkDownstreamBandwidthKbps(int downKbps) {
            mNetworkCapabilities.setLinkDownstreamBandwidthKbps(downKbps);
            return this;
        }

        /**
         * Sets the optional bearer specific network specifier.
         * This has no meaning if a single transport is also not specified, so calling
         * this without a single transport set will generate an exception, as will
         * subsequently adding or removing transports after this is set.
         * </p>
         * If the {@code networkSpecifier} is provided, it shall be interpreted as follows:
         * <ul>
         * <li>If the specifier can be parsed as an integer, it will be treated as a
         * {@link android.net TelephonyNetworkSpecifier}, and the provided integer will be
         * interpreted as a SubscriptionId.
         * <li>If the value is an ethernet interface name, it will be treated as such.
         * <li>For all other cases, the behavior is undefined.
         * </ul>
         *
         * @param networkSpecifier A {@code String} of either a SubscriptionId in cellular
         *                         network request or an ethernet interface name in ethernet
         *                         network request.
         *
         * @deprecated Use {@link #setNetworkSpecifier(NetworkSpecifier)} instead.
         */
        @Deprecated
        public Builder setNetworkSpecifier(String networkSpecifier) {
            try {
                int subId = Integer.parseInt(networkSpecifier);
                return setNetworkSpecifier(new TelephonyNetworkSpecifier.Builder()
                        .setSubscriptionId(subId).build());
            } catch (NumberFormatException nfe) {
                // An EthernetNetworkSpecifier or TestNetworkSpecifier does not accept null or empty
                // ("") strings. When network specifiers were strings a null string and an empty
                // string were considered equivalent. Hence no meaning is attached to a null or
                // empty ("") string.
                if (TextUtils.isEmpty(networkSpecifier)) {
                    return setNetworkSpecifier((NetworkSpecifier) null);
                } else if (mNetworkCapabilities.hasTransport(TRANSPORT_TEST)) {
                    return setNetworkSpecifier(new TestNetworkSpecifier(networkSpecifier));
                } else {
                    return setNetworkSpecifier(new EthernetNetworkSpecifier(networkSpecifier));
                }
            }
        }

        /**
         * Sets the optional bearer specific network specifier.
         * This has no meaning if a single transport is also not specified, so calling
         * this without a single transport set will generate an exception, as will
         * subsequently adding or removing transports after this is set.
         * </p>
         *
         * @param networkSpecifier A concrete, parcelable framework class that extends
         *                         NetworkSpecifier.
         */
        public Builder setNetworkSpecifier(NetworkSpecifier networkSpecifier) {
            if (networkSpecifier instanceof MatchAllNetworkSpecifier) {
                throw new IllegalArgumentException("A MatchAllNetworkSpecifier is not permitted");
            }
            mNetworkCapabilities.setNetworkSpecifier(networkSpecifier);
            // Do not touch NOT_VCN_MANAGED if the caller needs to access to a very specific
            // Network.
            mShouldDeduceNotVcnManaged = false;
            return this;
        }

        /**
         * Sets the signal strength. This is a signed integer, with higher values indicating a
         * stronger signal. The exact units are bearer-dependent. For example, Wi-Fi uses the same
         * RSSI units reported by WifiManager.
         * <p>
         * Note that when used to register a network callback, this specifies the minimum acceptable
         * signal strength. When received as the state of an existing network it specifies the
         * current value. A value of {@code SIGNAL_STRENGTH_UNSPECIFIED} means no value when
         * received and has no effect when requesting a callback.
         *
         * <p>This method requires the caller to hold the
         * {@link android.Manifest.permission#NETWORK_SIGNAL_STRENGTH_WAKEUP} permission
         *
         * @param signalStrength the bearer-specific signal strength.
         * @hide
         */
        @SystemApi
        @RequiresPermission(android.Manifest.permission.NETWORK_SIGNAL_STRENGTH_WAKEUP)
        public @NonNull Builder setSignalStrength(int signalStrength) {
            mNetworkCapabilities.setSignalStrength(signalStrength);
            return this;
        }

        /**
         * Deduce the NET_CAPABILITY_NOT_VCN_MANAGED capability from other capabilities
         * and user intention, which includes:
         *   1. For the requests that don't have anything besides
         *      {@link #VCN_SUPPORTED_CAPABILITIES}, add the NET_CAPABILITY_NOT_VCN_MANAGED to
         *      allow the callers automatically utilize VCN networks if available.
         *   2. For the requests that explicitly add or remove NET_CAPABILITY_NOT_VCN_MANAGED,
         *      or has clear intention of tracking specific network,
         *      do not alter them to allow user fire request that suits their need.
         *
         * @hide
         */
        private void deduceNotVcnManagedCapability(final NetworkCapabilities nc) {
            if (!mShouldDeduceNotVcnManaged) return;
            for (final int cap : nc.getCapabilities()) {
                if (!VCN_SUPPORTED_CAPABILITIES.contains(cap)) return;
            }
            nc.addCapability(NET_CAPABILITY_NOT_VCN_MANAGED);
        }

        /**
         * Sets the optional subscription ID set.
         * <p>
         * This specify the subscription IDs requirement.
         * A network will satisfy this request only if it matches one of the subIds in this set.
         * An empty set matches all networks, including those without a subId.
         *
         * <p>Registering a NetworkRequest with a non-empty set of subIds requires the
         * NETWORK_FACTORY permission.
         *
         * @param subIds A {@code Set} that represents subscription IDs.
         */
        @NonNull
        @FlaggedApi(Flags.FLAG_REQUEST_RESTRICTED_WIFI)
        public Builder setSubscriptionIds(@NonNull Set<Integer> subIds) {
            mNetworkCapabilities.setSubscriptionIds(subIds);
            return this;
        }

        /**
         * Specifies whether the built request should also match networks that do not apply to the
         * calling UID.
         *
         * By default, the built request will only match networks that apply to the calling UID.
         * If this method is called with {@code true}, the built request will match any network on
         * the system that matches the other parameters of the request. In this case, any
         * information in the built request that is subject to redaction for security or privacy
         * purposes, such as a {@link NetworkSpecifier}, will be redacted or removed to prevent the
         * application deducing sensitive information.
         *
         * @param include Whether to match networks that do not apply to the calling UID.
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder setIncludeOtherUidNetworks(boolean include) {
            if (include) {
                mNetworkCapabilities.setUids(null);
            } else {
                mNetworkCapabilities.setSingleUid(Process.myUid());
            }
            return this;
        }
    }

    // implement the Parcelable interface
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel dest, int flags) {
        networkCapabilities.writeToParcel(dest, flags);
        dest.writeInt(legacyType);
        dest.writeInt(requestId);
        dest.writeString(type.name());
    }

    public static final @android.annotation.NonNull Creator<NetworkRequest> CREATOR =
        new Creator<NetworkRequest>() {
            public NetworkRequest createFromParcel(Parcel in) {
                return new NetworkRequest(in);
            }
            public NetworkRequest[] newArray(int size) {
                return new NetworkRequest[size];
            }
        };

    /**
     * Returns true iff. this NetworkRequest is of type LISTEN.
     *
     * @hide
     */
    public boolean isListen() {
        return type == Type.LISTEN;
    }

    /**
     * Returns true iff. this NetworkRequest is of type LISTEN_FOR_BEST.
     *
     * @hide
     */
    public boolean isListenForBest() {
        return type == Type.LISTEN_FOR_BEST;
    }

    /**
     * Returns true iff. the contained NetworkRequest is one that:
     *
     *     - should be associated with at most one satisfying network
     *       at a time;
     *
     *     - should cause a network to be kept up, but not necessarily in
     *       the foreground, if it is the best network which can satisfy the
     *       NetworkRequest.
     *
     * For full detail of how isRequest() is used for pairing Networks with
     * NetworkRequests read rematchNetworkAndRequests().
     *
     * @hide
     */
    public boolean isRequest() {
        return type == Type.REQUEST || type == Type.BACKGROUND_REQUEST || type == Type.RESERVATION;
    }

    /**
     * Returns true iff. this NetworkRequest is of type BACKGROUND_REQUEST.
     *
     * @hide
     */
    public boolean isBackgroundRequest() {
        return type == Type.BACKGROUND_REQUEST;
    }

    /**
     * @see Builder#addCapability(int)
     */
    public boolean hasCapability(@NetCapability int capability) {
        return networkCapabilities.hasCapability(capability);
    }

    /**
     * @see Builder#addForbiddenCapability(int)
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    // TODO : @FlaggedApi(Flags.FLAG_FORBIDDEN_CAPABILITY) and public instead of @SystemApi
    public boolean hasForbiddenCapability(@NetCapability int capability) {
        return networkCapabilities.hasForbiddenCapability(capability);
    }

    /**
     * Returns true if and only if the capabilities requested in this NetworkRequest are satisfied
     * by the provided {@link NetworkCapabilities}.
     *
     * @param nc Capabilities that should satisfy this NetworkRequest. null capabilities do not
     *           satisfy any request.
     */
    public boolean canBeSatisfiedBy(@Nullable NetworkCapabilities nc) {
        return networkCapabilities.satisfiedByNetworkCapabilities(nc);
    }

    /**
     * @see Builder#addTransportType(int)
     */
    public boolean hasTransport(@Transport int transportType) {
        return networkCapabilities.hasTransport(transportType);
    }

    /**
     * @see Builder#setNetworkSpecifier(NetworkSpecifier)
     */
    @Nullable
    public NetworkSpecifier getNetworkSpecifier() {
        return networkCapabilities.getNetworkSpecifier();
    }

    /**
     * @return the uid of the app making the request.
     *
     * Note: This could return {@link Process#INVALID_UID} if the {@link NetworkRequest} object was
     * not obtained from {@link ConnectivityManager}.
     * @hide
     */
    @SystemApi
    public int getRequestorUid() {
        return networkCapabilities.getRequestorUid();
    }

    /**
     * @return the package name of the app making the request.
     *
     * Note: This could return {@code null} if the {@link NetworkRequest} object was not obtained
     * from {@link ConnectivityManager}.
     * @hide
     */
    @SystemApi
    @Nullable
    public String getRequestorPackageName() {
        return networkCapabilities.getRequestorPackageName();
    }

    public String toString() {
        return "NetworkRequest [ " + type + " id=" + requestId +
                (legacyType != ConnectivityManager.TYPE_NONE ? ", legacyType=" + legacyType : "") +
                ", " + networkCapabilities.toString() + " ]";
    }

    public boolean equals(@Nullable Object obj) {
        if (obj instanceof NetworkRequest == false) return false;
        NetworkRequest that = (NetworkRequest)obj;
        return (that.legacyType == this.legacyType &&
                that.requestId == this.requestId &&
                that.type == this.type &&
                Objects.equals(that.networkCapabilities, this.networkCapabilities));
    }

    public int hashCode() {
        return Objects.hash(requestId, legacyType, networkCapabilities, type);
    }

    /**
     * Gets all the capabilities set on this {@code NetworkRequest} instance.
     *
     * @return an array of capability values for this instance.
     */
    @NonNull
    public @NetCapability int[] getCapabilities() {
        // No need to make a defensive copy here as NC#getCapabilities() already returns
        // a new array.
        return networkCapabilities.getCapabilities();
    }

    /**
     * Get the enteprise identifiers.
     *
     * Get all the enterprise identifiers set on this {@code NetworkCapability}
     * @return array of all the enterprise identifiers.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public @NonNull @NetworkCapabilities.EnterpriseId int[] getEnterpriseIds() {
        // No need to make a defensive copy here as NC#getCapabilities() already returns
        // a new array.
        return networkCapabilities.getEnterpriseIds();
    }

    /**
     * Tests for the presence of an enterprise identifier on this instance.
     *
     * @param enterpriseId the enterprise capability identifier to be tested for.
     * @return {@code true} if set on this instance.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public boolean hasEnterpriseId(
            @NetworkCapabilities.EnterpriseId int enterpriseId) {
        return networkCapabilities.hasEnterpriseId(enterpriseId);
    }

    /**
     * Gets all the forbidden capabilities set on this {@code NetworkRequest} instance.
     *
     * @return an array of forbidden capability values for this instance.
     *
     * @hide
     */
    @NonNull
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    // TODO : @FlaggedApi(Flags.FLAG_FORBIDDEN_CAPABILITY) and public instead of @SystemApi
    public @NetCapability int[] getForbiddenCapabilities() {
        // No need to make a defensive copy here as NC#getForbiddenCapabilities() already returns
        // a new array.
        return networkCapabilities.getForbiddenCapabilities();
    }

    /**
     * Gets all the transports set on this {@code NetworkRequest} instance.
     *
     * @return an array of transport type values for this instance.
     */
    @NonNull
    public @Transport int[] getTransportTypes() {
        // No need to make a defensive copy here as NC#getTransportTypes() already returns
        // a new array.
        return networkCapabilities.getTransportTypes();
    }

    /**
     * Gets all the subscription ids set on this {@code NetworkRequest} instance.
     *
     * @return Set of Integer values for this instance.
     */
    @NonNull
    @FlaggedApi(Flags.FLAG_REQUEST_RESTRICTED_WIFI)
    public Set<Integer> getSubscriptionIds() {
        // No need to make a defensive copy here as NC#getSubscriptionIds() already returns
        // a new set.
        return networkCapabilities.getSubscriptionIds();
    }
}
