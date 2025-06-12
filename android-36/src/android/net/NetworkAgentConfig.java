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

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.modules.utils.build.SdkLevel;

import java.util.Objects;

/**
 * Allows a network transport to provide the system with policy and configuration information about
 * a particular network when registering a {@link NetworkAgent}. This information cannot change once the agent is registered.
 *
 * @hide
 */
@SystemApi
public final class NetworkAgentConfig implements Parcelable {
    // TODO : make this object immutable. The fields that should stay mutable should likely
    // migrate to NetworkAgentInfo.

    /**
     * If the {@link Network} is a VPN, whether apps are allowed to bypass the
     * VPN. This is set by a {@link VpnService} and used by
     * {@link ConnectivityManager} when creating a VPN.
     *
     * @hide
     */
    public boolean allowBypass;

    /**
     * Set if the network was manually/explicitly connected to by the user either from settings
     * or a 3rd party app.  For example, turning on cell data is not explicit but tapping on a wifi
     * ap in the wifi settings to trigger a connection is explicit.  A 3rd party app asking to
     * connect to a particular access point is also explicit, though this may change in the future
     * as we want apps to use the multinetwork apis.
     * TODO : this is a bad name, because it sounds like the user just tapped on the network.
     * It's not necessarily the case ; auto-reconnection to WiFi has this true for example.
     * @hide
     */
    public boolean explicitlySelected;

    /**
     * @return whether this network was explicitly selected by the user.
     */
    public boolean isExplicitlySelected() {
        return explicitlySelected;
    }

    /**
     * @return whether this VPN connection can be bypassed by the apps.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public boolean isBypassableVpn() {
        return allowBypass;
    }

    /**
     * Set if the user desires to use this network even if it is unvalidated. This field has meaning
     * only if {@link explicitlySelected} is true. If it is, this field must also be set to the
     * appropriate value based on previous user choice.
     *
     * TODO : rename this field to match its accessor
     * @hide
     */
    public boolean acceptUnvalidated;

    /**
     * @return whether the system should accept this network even if it doesn't validate.
     */
    public boolean isUnvalidatedConnectivityAcceptable() {
        return acceptUnvalidated;
    }

    /**
     * Whether the user explicitly set that this network should be validated even if presence of
     * only partial internet connectivity.
     *
     * TODO : rename this field to match its accessor
     * @hide
     */
    public boolean acceptPartialConnectivity;

    /**
     * @return whether the system should validate this network even if it only offers partial
     *     Internet connectivity.
     */
    public boolean isPartialConnectivityAcceptable() {
        return acceptPartialConnectivity;
    }

    /**
     * Set to avoid surfacing the "Sign in to network" notification.
     * if carrier receivers/apps are registered to handle the carrier-specific provisioning
     * procedure, a carrier specific provisioning notification will be placed.
     * only one notification should be displayed. This field is set based on
     * which notification should be used for provisioning.
     *
     * @hide
     */
    public boolean provisioningNotificationDisabled;

    /**
     *
     * @return whether the sign in to network notification is enabled by this configuration.
     * @hide
     */
    public boolean isProvisioningNotificationEnabled() {
        return !provisioningNotificationDisabled;
    }

    /**
     * For mobile networks, this is the subscriber ID (such as IMSI).
     *
     * @hide
     */
    public String subscriberId;

    /**
     * @return the subscriber ID, or null if none.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @Nullable
    public String getSubscriberId() {
        return subscriberId;
    }

    /**
     * Set to skip 464xlat. This means the device will treat the network as IPv6-only and
     * will not attempt to detect a NAT64 via RFC 7050 DNS lookups.
     *
     * @hide
     */
    public boolean skip464xlat;

    /**
     * @return whether NAT64 prefix detection is enabled.
     * @hide
     */
    public boolean isNat64DetectionEnabled() {
        return !skip464xlat;
    }

    /**
     * The legacy type of this network agent, or TYPE_NONE if unset.
     * @hide
     */
    public int legacyType = ConnectivityManager.TYPE_NONE;

    /**
     * @return the legacy type
     */
    @ConnectivityManager.LegacyNetworkType
    public int getLegacyType() {
        return legacyType;
    }

    /**
     * The legacy Sub type of this network agent, or TYPE_NONE if unset.
     * @hide
     */
    public int legacySubType = ConnectivityManager.TYPE_NONE;

    /**
     * Set to true if the PRIVATE_DNS_BROKEN notification has shown for this network.
     * Reset this bit when private DNS mode is changed from strict mode to opportunistic/off mode.
     *
     * This is not parceled, because it would not make sense. It's also ignored by the
     * equals() and hashcode() methods.
     *
     * @hide
     */
    public transient boolean hasShownBroken;

    /**
     * The name of the legacy network type. It's a free-form string used in logging.
     * @hide
     */
    @NonNull
    public String legacyTypeName = "";

    /**
     * @return the name of the legacy network type. It's a free-form string used in logging.
     */
    @NonNull
    public String getLegacyTypeName() {
        return legacyTypeName;
    }

    /**
     * The name of the legacy Sub network type. It's a free-form string.
     * @hide
     */
    @NonNull
    public String legacySubTypeName = "";

    /**
     * The legacy extra info of the agent. The extra info should only be :
     * <ul>
     *   <li>For cellular agents, the APN name.</li>
     *   <li>For ethernet agents, the interface name.</li>
     * </ul>
     * @hide
     */
    @NonNull
    private String mLegacyExtraInfo = "";

    /**
     * The legacy extra info of the agent.
     * @hide
     */
    @NonNull
    public String getLegacyExtraInfo() {
        return mLegacyExtraInfo;
    }

    /**
     * If the {@link Network} is a VPN, whether the local traffic is exempted from the VPN.
     * @hide
     */
    public boolean excludeLocalRouteVpn = false;

    /**
     * @return whether local traffic is excluded from the VPN network.
     * @hide
     */
    public boolean areLocalRoutesExcludedForVpn() {
        return excludeLocalRouteVpn;
    }

    /**
     * Whether network validation should be performed for this VPN network.
     * @see #isVpnValidationRequired
     * @hide
     */
    private boolean mVpnRequiresValidation = false;

    /**
     * Whether network validation should be performed for this VPN network.
     *
     * If this network isn't a VPN this should always be {@code false}, and will be ignored
     * if set.
     * If this network is a VPN, false means this network should always be considered validated;
     * true means it follows the same validation semantics as general internet networks.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public boolean isVpnValidationRequired() {
        return mVpnRequiresValidation;
    }

    /**
     * Whether the native network creation should be skipped.
     *
     * If set, the native network and routes should be maintained by the caller.
     *
     * @hide
     */
    private boolean mSkipNativeNetworkCreation = false;


    /**
     * @return Whether the native network creation should be skipped.
     * @hide
     */
    // TODO: Expose API when ready.
    // @FlaggedApi(Flags.FLAG_TETHERING_NETWORK_AGENT)
    // @SystemApi(client = MODULE_LIBRARIES) when ready.
    public boolean shouldSkipNativeNetworkCreation() {
        return mSkipNativeNetworkCreation;
    }

    /** @hide */
    public NetworkAgentConfig() {
    }

    /** @hide */
    public NetworkAgentConfig(@Nullable NetworkAgentConfig nac) {
        if (nac != null) {
            allowBypass = nac.allowBypass;
            explicitlySelected = nac.explicitlySelected;
            acceptUnvalidated = nac.acceptUnvalidated;
            acceptPartialConnectivity = nac.acceptPartialConnectivity;
            subscriberId = nac.subscriberId;
            provisioningNotificationDisabled = nac.provisioningNotificationDisabled;
            skip464xlat = nac.skip464xlat;
            legacyType = nac.legacyType;
            legacyTypeName = nac.legacyTypeName;
            legacySubType = nac.legacySubType;
            legacySubTypeName = nac.legacySubTypeName;
            mLegacyExtraInfo = nac.mLegacyExtraInfo;
            excludeLocalRouteVpn = nac.excludeLocalRouteVpn;
            mVpnRequiresValidation = nac.mVpnRequiresValidation;
            mSkipNativeNetworkCreation = nac.mSkipNativeNetworkCreation;
        }
    }

    /**
     * Builder class to facilitate constructing {@link NetworkAgentConfig} objects.
     */
    public static final class Builder {
        private final NetworkAgentConfig mConfig = new NetworkAgentConfig();

        /**
         * Sets whether the network was explicitly selected by the user.
         *
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setExplicitlySelected(final boolean explicitlySelected) {
            mConfig.explicitlySelected = explicitlySelected;
            return this;
        }

        /**
         * Sets whether the system should validate this network even if it is found not to offer
         * Internet connectivity.
         *
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setUnvalidatedConnectivityAcceptable(
                final boolean unvalidatedConnectivityAcceptable) {
            mConfig.acceptUnvalidated = unvalidatedConnectivityAcceptable;
            return this;
        }

        /**
         * Sets whether the system should validate this network even if it is found to only offer
         * partial Internet connectivity.
         *
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setPartialConnectivityAcceptable(
                final boolean partialConnectivityAcceptable) {
            mConfig.acceptPartialConnectivity = partialConnectivityAcceptable;
            return this;
        }

        /**
         * Sets the subscriber ID for this network.
         *
         * @return this builder, to facilitate chaining.
         * @hide
         */
        @NonNull
        @SystemApi(client = MODULE_LIBRARIES)
        public Builder setSubscriberId(@Nullable String subscriberId) {
            mConfig.subscriberId = subscriberId;
            return this;
        }

        /**
         * Enables or disables active detection of NAT64 (e.g., via RFC 7050 DNS lookups). Used to
         * save power and reduce idle traffic on networks that are known to be IPv6-only without a
         * NAT64. By default, NAT64 detection is enabled.
         *
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setNat64DetectionEnabled(boolean enabled) {
            mConfig.skip464xlat = !enabled;
            return this;
        }

        /**
         * Enables or disables the "Sign in to network" notification. Used if the network transport
         * will perform its own carrier-specific provisioning procedure. By default, the
         * notification is enabled.
         *
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setProvisioningNotificationEnabled(boolean enabled) {
            mConfig.provisioningNotificationDisabled = !enabled;
            return this;
        }

        /**
         * Sets the legacy type for this network.
         *
         * @param legacyType the type
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setLegacyType(int legacyType) {
            mConfig.legacyType = legacyType;
            return this;
        }

        /**
         * Sets the legacy sub-type for this network.
         *
         * @param legacySubType the type
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setLegacySubType(final int legacySubType) {
            mConfig.legacySubType = legacySubType;
            return this;
        }

        /**
         * Sets the name of the legacy type of the agent. It's a free-form string used in logging.
         * @param legacyTypeName the name
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setLegacyTypeName(@NonNull String legacyTypeName) {
            mConfig.legacyTypeName = legacyTypeName;
            return this;
        }

        /**
         * Sets the name of the legacy Sub-type of the agent. It's a free-form string.
         * @param legacySubTypeName the name
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setLegacySubTypeName(@NonNull String legacySubTypeName) {
            mConfig.legacySubTypeName = legacySubTypeName;
            return this;
        }

        /**
         * Sets the legacy extra info of the agent.
         * @param legacyExtraInfo the legacy extra info.
         * @return this builder, to facilitate chaining.
         */
        @NonNull
        public Builder setLegacyExtraInfo(@NonNull String legacyExtraInfo) {
            mConfig.mLegacyExtraInfo = legacyExtraInfo;
            return this;
        }

        /**
         * Sets whether network validation should be performed for this VPN network.
         *
         * Only agents registering a VPN network should use this setter. On other network
         * types it will be ignored.
         * False means this network should always be considered validated;
         * true means it follows the same validation semantics as general internet.
         *
         * @param vpnRequiresValidation whether this VPN requires validation.
         *                              Default is {@code false}.
         * @hide
         */
        @NonNull
        @SystemApi(client = MODULE_LIBRARIES)
        public Builder setVpnRequiresValidation(boolean vpnRequiresValidation) {
            mConfig.mVpnRequiresValidation = vpnRequiresValidation;
            return this;
        }

        /**
         * Sets whether the apps can bypass the VPN connection.
         *
         * @return this builder, to facilitate chaining.
         * @hide
         */
        @NonNull
        @SystemApi(client = MODULE_LIBRARIES)
        public Builder setBypassableVpn(boolean allowBypass) {
            mConfig.allowBypass = allowBypass;
            return this;
        }

        /**
         * Sets whether the local traffic is exempted from VPN.
         *
         * @return this builder, to facilitate chaining.
         * @hide
         */
        @NonNull
        @SystemApi(client = MODULE_LIBRARIES)
        public Builder setLocalRoutesExcludedForVpn(boolean excludeLocalRoutes) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException("Method is not supported");
            }
            mConfig.excludeLocalRouteVpn = excludeLocalRoutes;
            return this;
        }

        /**
         * Sets the native network creation should be skipped.
         *
         * @return this builder, to facilitate chaining.
         * @hide
         */
        @NonNull
        // TODO: Expose API when ready.
        // @FlaggedApi(Flags.FLAG_TETHERING_NETWORK_AGENT)
        // @SystemApi(client = MODULE_LIBRARIES) when ready.
        public Builder setSkipNativeNetworkCreation(boolean skipNativeNetworkCreation) {
            if (!SdkLevel.isAtLeastV()) {
                // Local agents are supported starting on U on TVs and on V on everything else.
                // Thus, only support this flag on V+.
                throw new UnsupportedOperationException("Method is not supported");
            }
            mConfig.mSkipNativeNetworkCreation = skipNativeNetworkCreation;
            return this;
        }

        /**
         * Returns the constructed {@link NetworkAgentConfig} object.
         */
        @NonNull
        public NetworkAgentConfig build() {
            return mConfig;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NetworkAgentConfig that = (NetworkAgentConfig) o;
        return allowBypass == that.allowBypass
                && explicitlySelected == that.explicitlySelected
                && acceptUnvalidated == that.acceptUnvalidated
                && acceptPartialConnectivity == that.acceptPartialConnectivity
                && provisioningNotificationDisabled == that.provisioningNotificationDisabled
                && skip464xlat == that.skip464xlat
                && legacyType == that.legacyType
                && legacySubType == that.legacySubType
                && Objects.equals(subscriberId, that.subscriberId)
                && Objects.equals(legacyTypeName, that.legacyTypeName)
                && Objects.equals(legacySubTypeName, that.legacySubTypeName)
                && Objects.equals(mLegacyExtraInfo, that.mLegacyExtraInfo)
                && excludeLocalRouteVpn == that.excludeLocalRouteVpn
                && mVpnRequiresValidation == that.mVpnRequiresValidation
                && mSkipNativeNetworkCreation == that.mSkipNativeNetworkCreation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowBypass, explicitlySelected, acceptUnvalidated,
                acceptPartialConnectivity, provisioningNotificationDisabled, subscriberId,
                skip464xlat, legacyType, legacySubType, legacyTypeName, legacySubTypeName,
                mLegacyExtraInfo, excludeLocalRouteVpn, mVpnRequiresValidation,
                mSkipNativeNetworkCreation);
    }

    @Override
    public String toString() {
        return "NetworkAgentConfig {"
                + " allowBypass = " + allowBypass
                + ", explicitlySelected = " + explicitlySelected
                + ", acceptUnvalidated = " + acceptUnvalidated
                + ", acceptPartialConnectivity = " + acceptPartialConnectivity
                + ", provisioningNotificationDisabled = " + provisioningNotificationDisabled
                + ", subscriberId = '" + subscriberId + '\''
                + ", skip464xlat = " + skip464xlat
                + ", legacyType = " + legacyType
                + ", legacySubType = " + legacySubType
                + ", hasShownBroken = " + hasShownBroken
                + ", legacyTypeName = '" + legacyTypeName + '\''
                + ", legacySubTypeName = '" + legacySubTypeName + '\''
                + ", legacyExtraInfo = '" + mLegacyExtraInfo + '\''
                + ", excludeLocalRouteVpn = '" + excludeLocalRouteVpn + '\''
                + ", vpnRequiresValidation = '" + mVpnRequiresValidation + '\''
                + ", skipNativeNetworkCreation = '" + mSkipNativeNetworkCreation + '\''
                + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(allowBypass ? 1 : 0);
        out.writeInt(explicitlySelected ? 1 : 0);
        out.writeInt(acceptUnvalidated ? 1 : 0);
        out.writeInt(acceptPartialConnectivity ? 1 : 0);
        out.writeString(subscriberId);
        out.writeInt(provisioningNotificationDisabled ? 1 : 0);
        out.writeInt(skip464xlat ? 1 : 0);
        out.writeInt(legacyType);
        out.writeString(legacyTypeName);
        out.writeInt(legacySubType);
        out.writeString(legacySubTypeName);
        out.writeString(mLegacyExtraInfo);
        out.writeInt(excludeLocalRouteVpn ? 1 : 0);
        out.writeInt(mVpnRequiresValidation ? 1 : 0);
        out.writeInt(mSkipNativeNetworkCreation ? 1 : 0);
    }

    public static final @NonNull Creator<NetworkAgentConfig> CREATOR =
            new Creator<NetworkAgentConfig>() {
                @Override
                public NetworkAgentConfig createFromParcel(Parcel in) {
                    NetworkAgentConfig networkAgentConfig = new NetworkAgentConfig();
                    networkAgentConfig.allowBypass = in.readInt() != 0;
                    networkAgentConfig.explicitlySelected = in.readInt() != 0;
                    networkAgentConfig.acceptUnvalidated = in.readInt() != 0;
                    networkAgentConfig.acceptPartialConnectivity = in.readInt() != 0;
                    networkAgentConfig.subscriberId = in.readString();
                    networkAgentConfig.provisioningNotificationDisabled = in.readInt() != 0;
                    networkAgentConfig.skip464xlat = in.readInt() != 0;
                    networkAgentConfig.legacyType = in.readInt();
                    networkAgentConfig.legacyTypeName = in.readString();
                    networkAgentConfig.legacySubType = in.readInt();
                    networkAgentConfig.legacySubTypeName = in.readString();
                    networkAgentConfig.mLegacyExtraInfo = in.readString();
                    networkAgentConfig.excludeLocalRouteVpn = in.readInt() != 0;
                    networkAgentConfig.mVpnRequiresValidation = in.readInt() != 0;
                    networkAgentConfig.mSkipNativeNetworkCreation = in.readInt() != 0;
                    return networkAgentConfig;
                }

                @Override
                public NetworkAgentConfig[] newArray(int size) {
                    return new NetworkAgentConfig[size];
                }
            };
}
