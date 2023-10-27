/*
 * Copyright (C) 2011 The Android Open Source Project
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
import static android.net.ConnectivityManager.TYPE_BLUETOOTH;
import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_PROXY;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.net.ConnectivityManager.TYPE_WIFI_P2P;
import static android.net.ConnectivityManager.TYPE_WIMAX;
import static android.net.NetworkIdentity.OEM_NONE;
import static android.net.NetworkIdentity.OEM_PAID;
import static android.net.NetworkIdentity.OEM_PRIVATE;
import static android.net.NetworkStats.DEFAULT_NETWORK_ALL;
import static android.net.NetworkStats.DEFAULT_NETWORK_NO;
import static android.net.NetworkStats.DEFAULT_NETWORK_YES;
import static android.net.NetworkStats.METERED_ALL;
import static android.net.NetworkStats.METERED_NO;
import static android.net.NetworkStats.METERED_YES;
import static android.net.NetworkStats.ROAMING_ALL;
import static android.net.NetworkStats.ROAMING_NO;
import static android.net.NetworkStats.ROAMING_YES;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.usage.NetworkStatsManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.CollectionUtils;
import com.android.net.module.util.NetworkIdentityUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Predicate used to match {@link NetworkIdentity}, usually when collecting
 * statistics. (It should probably have been named {@code NetworkPredicate}.)
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public final class NetworkTemplate implements Parcelable {
    private static final String TAG = NetworkTemplate.class.getSimpleName();

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "MATCH_" }, value = {
            MATCH_MOBILE,
            MATCH_WIFI,
            MATCH_ETHERNET,
            MATCH_BLUETOOTH,
            MATCH_PROXY,
            MATCH_CARRIER,
    })
    public @interface TemplateMatchRule{}

    /** Match rule to match cellular networks with given Subscriber Ids. */
    public static final int MATCH_MOBILE = 1;
    /** Match rule to match wifi networks. */
    public static final int MATCH_WIFI = 4;
    /** Match rule to match ethernet networks. */
    public static final int MATCH_ETHERNET = 5;
    /** Match rule to match bluetooth networks. */
    public static final int MATCH_BLUETOOTH = 8;
    /**
     * Match rule to match networks with {@link ConnectivityManager#TYPE_PROXY} as the legacy
     * network type.
     */
    public static final int MATCH_PROXY = 9;
    /**
     * Match rule to match all networks with subscriberId inside the template. Some carriers
     * may offer non-cellular networks like WiFi, which will be matched by this rule.
     */
    public static final int MATCH_CARRIER = 10;
    /**
     * Match rule to match networks with {@link ConnectivityManager#TYPE_TEST} as the legacy
     * network type.
     *
     * @hide
     */
    @VisibleForTesting
    public static final int MATCH_TEST = 11;

    // TODO: Remove this and replace all callers with WIFI_NETWORK_KEY_ALL.
    /** @hide */
    public static final String WIFI_NETWORKID_ALL = null;

    /**
     * Wi-Fi Network Key is never supposed to be null (if it is, it is a bug that
     * should be fixed), so it's not possible to want to match null vs
     * non-null. Therefore it's fine to use null as a sentinel for Wifi Network Key.
     *
     * @hide
     */
    public static final String WIFI_NETWORK_KEY_ALL = WIFI_NETWORKID_ALL;

    /**
     * Include all network types when filtering. This is meant to merge in with the
     * {@code TelephonyManager.NETWORK_TYPE_*} constants, and thus needs to stay in sync.
     */
    public static final int NETWORK_TYPE_ALL = -1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "OEM_MANAGED_" }, value = {
            OEM_MANAGED_ALL,
            OEM_MANAGED_NO,
            OEM_MANAGED_YES,
            OEM_MANAGED_PAID,
            OEM_MANAGED_PRIVATE
    })
    public @interface OemManaged{}

    /**
     * Value to match both OEM managed and unmanaged networks (all networks).
     */
    public static final int OEM_MANAGED_ALL = -1;
    /**
     * Value to match networks which are not OEM managed.
     */
    public static final int OEM_MANAGED_NO = OEM_NONE;
    /**
     * Value to match any OEM managed network.
     */
    public static final int OEM_MANAGED_YES = -2;
    /**
     * Network has {@link NetworkCapabilities#NET_CAPABILITY_OEM_PAID}.
     */
    public static final int OEM_MANAGED_PAID = OEM_PAID;
    /**
     * Network has {@link NetworkCapabilities#NET_CAPABILITY_OEM_PRIVATE}.
     */
    public static final int OEM_MANAGED_PRIVATE = OEM_PRIVATE;

    private static boolean isKnownMatchRule(final int rule) {
        switch (rule) {
            case MATCH_MOBILE:
            case MATCH_WIFI:
            case MATCH_ETHERNET:
            case MATCH_BLUETOOTH:
            case MATCH_PROXY:
            case MATCH_CARRIER:
            case MATCH_TEST:
                return true;

            default:
                return false;
        }
    }

    private static Set<String> setOf(@Nullable final String item) {
        if (item == null) {
            // Set.of will throw if item is null
            final Set<String> set = new HashSet<>();
            set.add(null);
            return Collections.unmodifiableSet(set);
        } else {
            return Set.of(item);
        }
    }

    private static void throwAtLeastU() {
        if (SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException("Method not supported on Android U or above");
        }
    }

    /**
     * Template to match {@link ConnectivityManager#TYPE_MOBILE} networks with
     * the given IMSI.
     *
     * @deprecated Use {@link Builder} to build a template.
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.TIRAMISU,
            publicAlternatives = "Use {@code Builder} instead.")
    public static NetworkTemplate buildTemplateMobileAll(@NonNull String subscriberId) {
        return new NetworkTemplate.Builder(MATCH_MOBILE).setMeteredness(METERED_YES)
                .setSubscriberIds(setOf(subscriberId)).build();
    }

    /**
     * Template to match metered {@link ConnectivityManager#TYPE_MOBILE} networks,
     * regardless of IMSI.
     *
     * @deprecated Use {@link Builder} to build a template.
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static NetworkTemplate buildTemplateMobileWildcard() {
        return new NetworkTemplate.Builder(MATCH_MOBILE).setMeteredness(METERED_YES).build();
    }

    /**
     * Template to match all metered {@link ConnectivityManager#TYPE_WIFI} networks,
     * regardless of key of the wifi network.
     *
     * @deprecated Use {@link Builder} to build a template.
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.TIRAMISU,
            publicAlternatives = "Use {@code Builder} instead.")
    public static NetworkTemplate buildTemplateWifiWildcard() {
        return new NetworkTemplate.Builder(MATCH_WIFI).build();
    }

    /**
     * @deprecated Use {@link Builder} to build a template.
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.TIRAMISU,
            publicAlternatives = "Use {@code Builder} instead.")
    public static NetworkTemplate buildTemplateWifi() {
        return buildTemplateWifiWildcard();
    }

    /**
     * Template to combine all {@link ConnectivityManager#TYPE_ETHERNET} style
     * networks together.
     *
     * @deprecated Use {@link Builder} to build a template.
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.TIRAMISU,
            publicAlternatives = "Use {@code Builder} instead.")
    public static NetworkTemplate buildTemplateEthernet() {
        return new NetworkTemplate.Builder(MATCH_ETHERNET).build();
    }

    /**
     * Template to combine all {@link ConnectivityManager#TYPE_BLUETOOTH} style
     * networks together.
     *
     * @hide
     */
    // TODO(b/270089918): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    public static NetworkTemplate buildTemplateBluetooth() {
        // TODO : this is part of hidden-o txt, does that mean it should be annotated with
        // @UnsupportedAppUsage(maxTargetSdk = O) ? If yes, can't throwAtLeastU() lest apps
        // targeting O- crash on those devices.
        return new NetworkTemplate.Builder(MATCH_BLUETOOTH).build();
    }

    /**
     * Template to combine all {@link ConnectivityManager#TYPE_PROXY} style
     * networks together.
     *
     * @hide
     */
    // TODO(b/270089918): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    public static NetworkTemplate buildTemplateProxy() {
        // TODO : this is part of hidden-o txt, does that mean it should be annotated with
        // @UnsupportedAppUsage(maxTargetSdk = O) ? If yes, can't throwAtLeastU() lest apps
        // targeting O- crash on those devices.
        return new NetworkTemplate(MATCH_PROXY, null, null);
    }

    /**
     * Template to match all metered carrier networks with the given IMSI.
     *
     * @hide
     */
    // TODO(b/273963543): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    public static NetworkTemplate buildTemplateCarrierMetered(@NonNull String subscriberId) {
        throwAtLeastU();
        return new NetworkTemplate.Builder(MATCH_CARRIER)
                // Set.of will throw if subscriberId is null, which is the historical
                // behavior and should be preserved.
                .setSubscriberIds(Set.of(subscriberId))
                .setMeteredness(METERED_YES)
                .build();
    }

    /**
     * Template to match cellular networks with the given IMSI, {@code ratType} and
     * {@code metered}. Use {@link #NETWORK_TYPE_ALL} to include all network types when
     * filtering. See {@code TelephonyManager.NETWORK_TYPE_*}.
     *
     * @hide
     */
    // TODO(b/273963543): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    public static NetworkTemplate buildTemplateMobileWithRatType(@Nullable String subscriberId,
            int ratType, int metered) {
        throwAtLeastU();
        return new NetworkTemplate.Builder(MATCH_MOBILE)
                .setSubscriberIds(TextUtils.isEmpty(subscriberId)
                        ? Collections.emptySet()
                        : Set.of(subscriberId))
                .setMeteredness(metered)
                .setRatType(ratType)
                .build();
    }

    /**
     * Template to match {@link ConnectivityManager#TYPE_WIFI} networks with the
     * given key of the wifi network.
     *
     * @param wifiNetworkKey key of the wifi network. see {@link WifiInfo#getNetworkKey()}
     *                  to know details about the key.
     * @hide
     */
    // TODO(b/273963543): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    public static NetworkTemplate buildTemplateWifi(@NonNull String wifiNetworkKey) {
        // TODO : this is part of hidden-o txt, does that mean it should be annotated with
        // @UnsupportedAppUsage(maxTargetSdk = O) ? If yes, can't throwAtLeastU() lest apps
        // targeting O- crash on those devices.
        return new NetworkTemplate.Builder(MATCH_WIFI)
                // Set.of will throw if wifiNetworkKey is null, which is the historical
                // behavior and should be preserved.
                .setWifiNetworkKeys(Set.of(wifiNetworkKey))
                .build();
    }

    /**
     * Template to match all {@link ConnectivityManager#TYPE_WIFI} networks with the given
     * key of the wifi network and IMSI.
     *
     * Call with {@link #WIFI_NETWORK_KEY_ALL} for {@code wifiNetworkKey} to get result regardless
     * of key of the wifi network.
     *
     * @param wifiNetworkKey key of the wifi network. see {@link WifiInfo#getNetworkKey()}
     *                  to know details about the key.
     * @param subscriberId the IMSI associated to this wifi network.
     *
     * @hide
     */
    // TODO(b/273963543): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    public static NetworkTemplate buildTemplateWifi(@Nullable String wifiNetworkKey,
            @Nullable String subscriberId) {
        throwAtLeastU();
        return new NetworkTemplate.Builder(MATCH_WIFI)
                .setSubscriberIds(setOf(subscriberId))
                .setWifiNetworkKeys(wifiNetworkKey == null
                        ? Collections.emptySet()
                        : Set.of(wifiNetworkKey))
                .build();
    }

    private final int mMatchRule;

    /**
     * Ugh, templates are designed to target a single subscriber, but we might
     * need to match several "merged" subscribers. These are the subscribers
     * that should be considered to match this template.
     * <p>
     * Since the merge set is dynamic, it should <em>not</em> be persisted or
     * used for determining equality.
     */
    @NonNull
    private final String[] mMatchSubscriberIds;

    @NonNull
    private final String[] mMatchWifiNetworkKeys;

    // Matches for the NetworkStats constants METERED_*, ROAMING_* and DEFAULT_NETWORK_*.
    private final int mMetered;
    private final int mRoaming;
    private final int mDefaultNetwork;
    private final int mRatType;

    // Bitfield containing OEM network properties{@code NetworkIdentity#OEM_*}.
    private final int mOemManaged;

    private static void checkValidMatchSubscriberIds(int matchRule, String[] matchSubscriberIds) {
        switch (matchRule) {
            // CARRIER templates must always specify a valid subscriber ID.
            // MOBILE templates can have empty matchSubscriberIds but it must not contain a null
            // subscriber ID.
            case MATCH_CARRIER:
                if (matchSubscriberIds.length == 0) {
                    throw new IllegalArgumentException("matchSubscriberIds may not contain"
                            + " null for rule " + getMatchRuleName(matchRule));
                }
                if (CollectionUtils.contains(matchSubscriberIds, null)) {
                    throw new IllegalArgumentException("matchSubscriberIds may not contain"
                            + " null for rule " + getMatchRuleName(matchRule));
                }
                break;
            case MATCH_MOBILE:
                // Prevent from crash for b/273963543, where the OEMs still call into unsupported
                // buildTemplateMobileAll with null subscriberId and get crashed.
                final int firstSdk = Build.VERSION.DEVICE_INITIAL_SDK_INT;
                if (firstSdk > Build.VERSION_CODES.TIRAMISU
                        && CollectionUtils.contains(matchSubscriberIds, null)) {
                    throw new IllegalArgumentException("checkValidMatchSubscriberIds list of ids"
                            + " may not contain null for rule " + getMatchRuleName(matchRule));
                }
                return;
            default:
                return;
        }
    }

    /**
     * @deprecated Use {@link Builder} to build a template.
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.TIRAMISU,
            publicAlternatives = "Use {@code Builder} instead.")
    public NetworkTemplate(int matchRule, String subscriberId, String wifiNetworkKey) {
        // Older versions used to only match MATCH_MOBILE and MATCH_MOBILE_WILDCARD templates
        // to metered networks. It is now possible to match mobile with any meteredness, but
        // in order to preserve backward compatibility of @UnsupportedAppUsage methods, this
        // constructor passes METERED_YES for these types.
        // For backwards compatibility, still accept old wildcard match rules (6 and 7 for
        // MATCH_{MOBILE,WIFI}_WILDCARD) but convert into functionally equivalent non-wildcard
        // ones.
        this(getBackwardsCompatibleMatchRule(matchRule),
                subscriberId != null ? new String[] { subscriberId } : new String[0],
                wifiNetworkKey != null ? new String[] { wifiNetworkKey } : new String[0],
                getMeterednessForBackwardsCompatibility(matchRule), ROAMING_ALL,
                DEFAULT_NETWORK_ALL, NETWORK_TYPE_ALL, OEM_MANAGED_ALL);
        if (matchRule == 6 || matchRule == 7) {
            Log.e(TAG, "Use MATCH_MOBILE with empty subscriberIds or MATCH_WIFI with empty "
                    + "wifiNetworkKeys instead of template with matchRule=" + matchRule);
        }
    }

    private static int getBackwardsCompatibleMatchRule(int matchRule) {
        // Backwards compatibility old constants
        // Old MATCH_MOBILE_WILDCARD
        if (6 == matchRule) return MATCH_MOBILE;
        // Old MATCH_WIFI_WILDCARD
        if (7 == matchRule) return MATCH_WIFI;
        return matchRule;
    }

    private static int getMeterednessForBackwardsCompatibility(int matchRule) {
        if (getBackwardsCompatibleMatchRule(matchRule) == MATCH_MOBILE
                || matchRule == MATCH_CARRIER) {
            return METERED_YES;
        }
        return METERED_ALL;
    }

    /** @hide */
    // TODO(b/270089918): Remove this method after no callers.
    public NetworkTemplate(int matchRule, String subscriberId, String[] matchSubscriberIds,
            String wifiNetworkKey) {
        // Older versions used to only match MATCH_MOBILE and MATCH_MOBILE_WILDCARD templates
        // to metered networks. It is now possible to match mobile with any meteredness, but
        // in order to preserve backward compatibility of @UnsupportedAppUsage methods, this
        // constructor passes METERED_YES for these types.
        this(getBackwardsCompatibleMatchRule(matchRule), matchSubscriberIds,
                wifiNetworkKey != null ? new String[] { wifiNetworkKey } : new String[0],
                getMeterednessForBackwardsCompatibility(matchRule),
                ROAMING_ALL, DEFAULT_NETWORK_ALL, NETWORK_TYPE_ALL,
                OEM_MANAGED_ALL);
        // TODO : this is part of hidden-o txt, does that mean it should be annotated with
        // @UnsupportedAppUsage(maxTargetSdk = O) ? If yes, can't throwAtLeastU() lest apps
        // targeting O- crash on those devices.
    }

    /** @hide */
    // TODO(b/269974916): Remove this method after Android U is released.
    //  This is only used by CTS of Android T.
    public NetworkTemplate(int matchRule, String subscriberId, String[] matchSubscriberIds,
            String[] matchWifiNetworkKeys, int metered, int roaming,
            int defaultNetwork, int ratType, int oemManaged, int subscriberIdMatchRule) {
        // subscriberId and subscriberIdMatchRule aren't used since they are replaced by
        // matchSubscriberIds, which could be null to indicate the intention of matching any
        // subscriberIds.
        this(getBackwardsCompatibleMatchRule(matchRule),
                matchSubscriberIds == null ? new String[]{} : matchSubscriberIds,
                matchWifiNetworkKeys, metered, roaming, defaultNetwork, ratType, oemManaged);
        throwAtLeastU();
    }

    /** @hide */
    public NetworkTemplate(int matchRule, String[] matchSubscriberIds,
            String[] matchWifiNetworkKeys, int metered, int roaming, int defaultNetwork,
            int ratType, int oemManaged) {
        Objects.requireNonNull(matchWifiNetworkKeys);
        Objects.requireNonNull(matchSubscriberIds);
        mMatchRule = matchRule;
        mMatchSubscriberIds = matchSubscriberIds;
        mMatchWifiNetworkKeys = matchWifiNetworkKeys;
        mMetered = metered;
        mRoaming = roaming;
        mDefaultNetwork = defaultNetwork;
        mRatType = ratType;
        mOemManaged = oemManaged;
        checkValidMatchSubscriberIds(matchRule, matchSubscriberIds);
        if (!isKnownMatchRule(matchRule)) {
            throw new IllegalArgumentException("Unknown network template rule " + matchRule
                    + " will not match any identity.");
        }
    }

    private NetworkTemplate(Parcel in) {
        mMatchRule = in.readInt();
        mMatchSubscriberIds = in.createStringArray();
        mMatchWifiNetworkKeys = in.createStringArray();
        mMetered = in.readInt();
        mRoaming = in.readInt();
        mDefaultNetwork = in.readInt();
        mRatType = in.readInt();
        mOemManaged = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mMatchRule);
        dest.writeStringArray(mMatchSubscriberIds);
        dest.writeStringArray(mMatchWifiNetworkKeys);
        dest.writeInt(mMetered);
        dest.writeInt(mRoaming);
        dest.writeInt(mDefaultNetwork);
        dest.writeInt(mRatType);
        dest.writeInt(mOemManaged);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("NetworkTemplate: ");
        builder.append("matchRule=").append(getMatchRuleName(mMatchRule));
        if (mMatchSubscriberIds != null) {
            builder.append(", matchSubscriberIds=").append(
                    Arrays.toString(NetworkIdentityUtils.scrubSubscriberIds(mMatchSubscriberIds)));
        }
        builder.append(", matchWifiNetworkKeys=").append(Arrays.toString(mMatchWifiNetworkKeys));
        if (mMetered != METERED_ALL) {
            builder.append(", metered=").append(NetworkStats.meteredToString(mMetered));
        }
        if (mRoaming != ROAMING_ALL) {
            builder.append(", roaming=").append(NetworkStats.roamingToString(mRoaming));
        }
        if (mDefaultNetwork != DEFAULT_NETWORK_ALL) {
            builder.append(", defaultNetwork=").append(NetworkStats.defaultNetworkToString(
                    mDefaultNetwork));
        }
        if (mRatType != NETWORK_TYPE_ALL) {
            builder.append(", ratType=").append(mRatType);
        }
        if (mOemManaged != OEM_MANAGED_ALL) {
            builder.append(", oemManaged=").append(getOemManagedNames(mOemManaged));
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMatchRule, Arrays.hashCode(mMatchSubscriberIds),
                Arrays.hashCode(mMatchWifiNetworkKeys), mMetered, mRoaming, mDefaultNetwork,
                mRatType, mOemManaged);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof NetworkTemplate) {
            final NetworkTemplate other = (NetworkTemplate) obj;
            return mMatchRule == other.mMatchRule
                    && mMetered == other.mMetered
                    && mRoaming == other.mRoaming
                    && mDefaultNetwork == other.mDefaultNetwork
                    && mRatType == other.mRatType
                    && mOemManaged == other.mOemManaged
                    && Arrays.equals(mMatchSubscriberIds, other.mMatchSubscriberIds)
                    && Arrays.equals(mMatchWifiNetworkKeys, other.mMatchWifiNetworkKeys);
        }
        return false;
    }

    // TODO(b/270089918): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    /** @hide */
    public boolean isMatchRuleMobile() {
        // TODO : this is part of hidden-o txt, does that mean it should be annotated with
        // @UnsupportedAppUsage(maxTargetSdk = O) ? If yes, can't throwAtLeastU() lest apps
        // targeting O- crash on those devices.
        switch (mMatchRule) {
            case MATCH_MOBILE:
            // Old MATCH_MOBILE_WILDCARD
            case 6:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get match rule of the template. See {@code MATCH_*}.
     */
    public int getMatchRule() {
        return mMatchRule;
    }

    /**
     * Get subscriber Id of the template.
     *
     * @deprecated User should use {@link #getSubscriberIds} instead.
     * @hide
     */
    @Deprecated
    @Nullable
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.TIRAMISU,
            publicAlternatives = "Caller should use {@code getSubscriberIds} instead.")
    public String getSubscriberId() {
        return CollectionUtils.isEmpty(mMatchSubscriberIds) ? null : mMatchSubscriberIds[0];
    }

    /**
     * Get set of subscriber Ids of the template.
     */
    @NonNull
    public Set<String> getSubscriberIds() {
        return new ArraySet<>(Arrays.asList(mMatchSubscriberIds));
    }

    /**
     * Get the set of Wifi Network Keys of the template.
     * See {@link WifiInfo#getNetworkKey()}.
     */
    @NonNull
    public Set<String> getWifiNetworkKeys() {
        return new ArraySet<>(Arrays.asList(mMatchWifiNetworkKeys));
    }

    /** @hide */
    // TODO: Remove this and replace all callers with {@link #getWifiNetworkKeys()}.
    @Nullable
    public String getNetworkId() {
        return getWifiNetworkKeys().isEmpty() ? null : getWifiNetworkKeys().iterator().next();
    }

    /**
     * Get meteredness filter of the template.
     */
    @NetworkStats.Meteredness
    public int getMeteredness() {
        return mMetered;
    }

    /**
     * Get roaming filter of the template.
     */
    @NetworkStats.Roaming
    public int getRoaming() {
        return mRoaming;
    }

    /**
     * Get the default network status filter of the template.
     */
    @NetworkStats.DefaultNetwork
    public int getDefaultNetworkStatus() {
        return mDefaultNetwork;
    }

    /**
     * Get the Radio Access Technology(RAT) type filter of the template.
     */
    public int getRatType() {
        return mRatType;
    }

    /**
     * Get the OEM managed filter of the template. See {@code OEM_MANAGED_*} or
     * {@code android.net.NetworkIdentity#OEM_*}.
     */
    @OemManaged
    public int getOemManaged() {
        return mOemManaged;
    }

    /**
     * Test if given {@link NetworkIdentity} matches this template.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public boolean matches(@NonNull NetworkIdentity ident) {
        Objects.requireNonNull(ident);
        if (!matchesMetered(ident)) return false;
        if (!matchesRoaming(ident)) return false;
        if (!matchesDefaultNetwork(ident)) return false;
        if (!matchesOemNetwork(ident)) return false;

        switch (mMatchRule) {
            case MATCH_MOBILE:
                return matchesMobile(ident);
            case MATCH_WIFI:
                return matchesWifi(ident);
            case MATCH_ETHERNET:
                return matchesEthernet(ident);
            case MATCH_BLUETOOTH:
                return matchesBluetooth(ident);
            case MATCH_PROXY:
                return matchesProxy(ident);
            case MATCH_CARRIER:
                return matchesCarrier(ident);
            case MATCH_TEST:
                return matchesTest(ident);
            default:
                // We have no idea what kind of network template we are, so we
                // just claim not to match anything.
                return false;
        }
    }

    private boolean matchesMetered(NetworkIdentity ident) {
        return (mMetered == METERED_ALL)
            || (mMetered == METERED_YES && ident.mMetered)
            || (mMetered == METERED_NO && !ident.mMetered);
    }

    private boolean matchesRoaming(NetworkIdentity ident) {
        return (mRoaming == ROAMING_ALL)
            || (mRoaming == ROAMING_YES && ident.mRoaming)
            || (mRoaming == ROAMING_NO && !ident.mRoaming);
    }

    private boolean matchesDefaultNetwork(NetworkIdentity ident) {
        return (mDefaultNetwork == DEFAULT_NETWORK_ALL)
            || (mDefaultNetwork == DEFAULT_NETWORK_YES && ident.mDefaultNetwork)
            || (mDefaultNetwork == DEFAULT_NETWORK_NO && !ident.mDefaultNetwork);
    }

    private boolean matchesOemNetwork(NetworkIdentity ident) {
        return (mOemManaged == OEM_MANAGED_ALL)
            || (mOemManaged == OEM_MANAGED_YES
                    && ident.mOemManaged != OEM_NONE)
            || (mOemManaged == ident.mOemManaged);
    }

    private boolean matchesCollapsedRatType(NetworkIdentity ident) {
        return mRatType == NETWORK_TYPE_ALL
                || NetworkStatsManager.getCollapsedRatType(mRatType)
                == NetworkStatsManager.getCollapsedRatType(ident.mRatType);
    }

    /**
     * Check if this template matches {@code subscriberId}. Returns true if this
     * template was created with a {@code mMatchSubscriberIds} array that contains
     * {@code subscriberId} or if {@code mMatchSubscriberIds} is empty.
     *
     * @hide
     */
    public boolean matchesSubscriberId(@Nullable String subscriberId) {
        return mMatchSubscriberIds.length == 0
                || CollectionUtils.contains(mMatchSubscriberIds, subscriberId);
    }

    /**
     * Check if network matches key of the wifi network.
     * Returns true when the key matches, or when {@code mMatchWifiNetworkKeys} is
     * empty.
     *
     * @param wifiNetworkKey key of the wifi network. see {@link WifiInfo#getNetworkKey()}
     *                  to know details about the key.
     */
    private boolean matchesWifiNetworkKey(@NonNull String wifiNetworkKey) {
        // Note that this code accepts null wifi network keys because of a past bug where wifi
        // code was sending a null network key for some connected networks, which isn't expected
        // and ended up stored in the data on many devices.
        // A null network key in the data matches a wildcard template (one where
        // {@code mMatchWifiNetworkKeys} is empty), but not one where {@code MatchWifiNetworkKeys}
        // contains null. See b/266598304.
        if (wifiNetworkKey == null) {
            return CollectionUtils.isEmpty(mMatchWifiNetworkKeys);
        }
        return CollectionUtils.isEmpty(mMatchWifiNetworkKeys)
                || CollectionUtils.contains(mMatchWifiNetworkKeys, wifiNetworkKey);
    }

    /**
     * Check if mobile network matches IMSI.
     */
    private boolean matchesMobile(NetworkIdentity ident) {
        if (ident.mType == TYPE_WIMAX) {
            // TODO: consider matching against WiMAX subscriber identity
            return true;
        } else {
            return (CollectionUtils.isEmpty(mMatchSubscriberIds)
                || CollectionUtils.contains(mMatchSubscriberIds, ident.mSubscriberId))
                && (ident.mType == TYPE_MOBILE && matchesCollapsedRatType(ident));
        }
    }

    /**
     * Check if matches Wi-Fi network template.
     */
    private boolean matchesWifi(NetworkIdentity ident) {
        switch (ident.mType) {
            case TYPE_WIFI:
                return matchesSubscriberId(ident.mSubscriberId)
                        && matchesWifiNetworkKey(ident.mWifiNetworkKey);
            case TYPE_WIFI_P2P:
                return CollectionUtils.isEmpty(mMatchWifiNetworkKeys);
            default:
                return false;
        }
    }

    /**
     * Check if matches Ethernet network template.
     */
    private boolean matchesEthernet(NetworkIdentity ident) {
        if (ident.mType == TYPE_ETHERNET) {
            return true;
        }
        return false;
    }

    /**
     * Check if matches carrier network. The carrier networks means it includes the subscriberId.
     */
    private boolean matchesCarrier(NetworkIdentity ident) {
        return ident.mSubscriberId != null
                && !CollectionUtils.isEmpty(mMatchSubscriberIds)
                && CollectionUtils.contains(mMatchSubscriberIds, ident.mSubscriberId);
    }

    /**
     * Check if matches test network. If the wifiNetworkKeys in the template is specified, Then it
     * will only match a network containing any of the specified the wifi network key. Otherwise,
     * all test networks would be matched.
     */
    private boolean matchesTest(NetworkIdentity ident) {
        return ident.mType == NetworkIdentity.TYPE_TEST
                && ((CollectionUtils.isEmpty(mMatchWifiNetworkKeys)
                || CollectionUtils.contains(mMatchWifiNetworkKeys, ident.mWifiNetworkKey)));
    }

    /**
     * Check if matches Bluetooth network template.
     */
    private boolean matchesBluetooth(NetworkIdentity ident) {
        if (ident.mType == TYPE_BLUETOOTH) {
            return true;
        }
        return false;
    }

    /**
     * Check if matches Proxy network template.
     */
    private boolean matchesProxy(NetworkIdentity ident) {
        return ident.mType == TYPE_PROXY;
    }

    private static String getMatchRuleName(int matchRule) {
        switch (matchRule) {
            case MATCH_MOBILE:
                return "MOBILE";
            case MATCH_WIFI:
                return "WIFI";
            case MATCH_ETHERNET:
                return "ETHERNET";
            case MATCH_BLUETOOTH:
                return "BLUETOOTH";
            case MATCH_PROXY:
                return "PROXY";
            case MATCH_CARRIER:
                return "CARRIER";
            case MATCH_TEST:
                return "TEST";
            default:
                return "UNKNOWN(" + matchRule + ")";
        }
    }

    private static String getOemManagedNames(int oemManaged) {
        switch (oemManaged) {
            case OEM_MANAGED_ALL:
                return "OEM_MANAGED_ALL";
            case OEM_MANAGED_NO:
                return "OEM_MANAGED_NO";
            case OEM_MANAGED_YES:
                return "OEM_MANAGED_YES";
            default:
                return NetworkIdentity.getOemManagedNames(oemManaged);
        }
    }

    /**
     * Examine the given template and normalize it.
     * We pick the "lowest" merged subscriber as the primary
     * for key purposes, and expand the template to match all other merged
     * subscribers.
     * <p>
     * For example, given an incoming template matching B, and the currently
     * active merge set [A,B], we'd return a new template that matches both A and B.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.TIRAMISU,
            publicAlternatives = "There is no alternative for {@code NetworkTemplate.normalize}."
                    + "Callers should have their own logic to merge template for"
                    + " different IMSIs and stop calling this function.")
    public static NetworkTemplate normalize(NetworkTemplate template, String[] merged) {
        return normalizeImpl(template, Collections.singletonList(merged));
    }

    /**
     * Examine the given template and normalize it.
     * We pick the "lowest" merged subscriber as the primary
     * for key purposes, and expand the template to match all other merged
     * subscribers.
     *
     * There can be multiple merged subscriberIds for multi-SIM devices.
     *
     * <p>
     * For example, given an incoming template matching B, and the currently
     * active merge set [A,B], we'd return a new template that matches both A and B.
     *
     * @hide
     */
    // TODO(b/273963543): Remove this method. This can only be done after there are no more callers,
    //  including in OEM code which can access this by linking against the framework.
    public static NetworkTemplate normalize(NetworkTemplate template, List<String[]> mergedList) {
        throwAtLeastU();
        return normalizeImpl(template, mergedList);
    }

    /**
     * Examine the given template and normalize it.
     * We pick the "lowest" merged subscriber as the primary
     * for key purposes, and expand the template to match all other merged
     * subscribers.
     *
     * There can be multiple merged subscriberIds for multi-SIM devices.
     *
     * <p>
     * For example, given an incoming template matching B, and the currently
     * active merge set [A,B], we'd return a new template that matches both A and B.
     *
     * @hide
     */
    private static NetworkTemplate normalizeImpl(NetworkTemplate template,
            List<String[]> mergedList) {
        // Now there are several types of network which uses SubscriberId to store network
        // information. For instances:
        // The TYPE_WIFI with subscriberId means that it is a merged carrier wifi network.
        // The TYPE_CARRIER means that the network associate to specific carrier network.

        if (CollectionUtils.isEmpty(template.mMatchSubscriberIds)) return template;

        for (String[] merged : mergedList) {
            if (CollectionUtils.contains(merged, template.mMatchSubscriberIds[0])) {
                // Requested template subscriber is part of the merge group; return
                // a template that matches all merged subscribers.
                final String[] matchWifiNetworkKeys = template.mMatchWifiNetworkKeys;
                // TODO: Use NetworkTemplate.Builder to build a template after NetworkTemplate
                // could handle incompatible subscriberIds. See b/217805241.
                return new NetworkTemplate(template.mMatchRule, merged,
                        CollectionUtils.isEmpty(matchWifiNetworkKeys)
                                ? new String[0] : new String[] { matchWifiNetworkKeys[0] },
                        (template.mMatchRule == MATCH_MOBILE
                                || template.mMatchRule == MATCH_CARRIER)
                                ? METERED_YES : METERED_ALL,
                        ROAMING_ALL, DEFAULT_NETWORK_ALL, NETWORK_TYPE_ALL, OEM_MANAGED_ALL);
            }
        }

        return template;
    }

    @UnsupportedAppUsage
    public static final @android.annotation.NonNull Creator<NetworkTemplate> CREATOR = new Creator<NetworkTemplate>() {
        @Override
        public NetworkTemplate createFromParcel(Parcel in) {
            return new NetworkTemplate(in);
        }

        @Override
        public NetworkTemplate[] newArray(int size) {
            return new NetworkTemplate[size];
        }
    };

    /**
     * Builder class for NetworkTemplate.
     */
    public static final class Builder {
        private final int mMatchRule;
        // Use a SortedSet to provide a deterministic order when fetching the first one.
        @NonNull
        private final SortedSet<String> mMatchSubscriberIds =
                new TreeSet<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        @NonNull
        private final SortedSet<String> mMatchWifiNetworkKeys = new TreeSet<>();

        // Matches for the NetworkStats constants METERED_*, ROAMING_* and DEFAULT_NETWORK_*.
        private int mMetered;
        private int mRoaming;
        private int mDefaultNetwork;
        private int mRatType;

        // Bitfield containing OEM network properties {@code NetworkIdentity#OEM_*}.
        private int mOemManaged;

        /**
         * Creates a new Builder with given match rule to construct NetworkTemplate objects.
         *
         * @param matchRule the match rule of the template, see {@code MATCH_*}.
         */
        public Builder(@TemplateMatchRule final int matchRule) {
            assertRequestableMatchRule(matchRule);
            // Initialize members with default values.
            mMatchRule = matchRule;
            mMetered = METERED_ALL;
            mRoaming = ROAMING_ALL;
            mDefaultNetwork = DEFAULT_NETWORK_ALL;
            mRatType = NETWORK_TYPE_ALL;
            mOemManaged = OEM_MANAGED_ALL;
        }

        /**
         * Set the Subscriber Ids. Calling this function with an empty set represents
         * the intention of matching any Subscriber Ids.
         *
         * @param subscriberIds the list of Subscriber Ids.
         * @return this builder.
         */
        @NonNull
        public Builder setSubscriberIds(@NonNull Set<String> subscriberIds) {
            Objects.requireNonNull(subscriberIds);
            mMatchSubscriberIds.clear();
            mMatchSubscriberIds.addAll(subscriberIds);
            return this;
        }

        /**
         * Set the Wifi Network Keys. Calling this function with an empty set represents
         * the intention of matching any Wifi Network Key.
         *
         * @param wifiNetworkKeys the list of Wifi Network Key,
         *                        see {@link WifiInfo#getNetworkKey()}.
         *                        Or an empty list to match all networks.
         *                        Note that {@code getNetworkKey()} might get null key
         *                        when wifi disconnects. However, the caller should never invoke
         *                        this function with a null Wifi Network Key since such statistics
         *                        never exists.
         * @return this builder.
         */
        @NonNull
        public Builder setWifiNetworkKeys(@NonNull Set<String> wifiNetworkKeys) {
            Objects.requireNonNull(wifiNetworkKeys);
            for (String key : wifiNetworkKeys) {
                if (key == null) {
                    throw new IllegalArgumentException("Null is not a valid key");
                }
            }
            mMatchWifiNetworkKeys.clear();
            mMatchWifiNetworkKeys.addAll(wifiNetworkKeys);
            return this;
        }

        /**
         * Set the meteredness filter.
         *
         * @param metered the meteredness filter.
         * @return this builder.
         */
        @NonNull
        public Builder setMeteredness(@NetworkStats.Meteredness int metered) {
            mMetered = metered;
            return this;
        }

        /**
         * Set the roaming filter.
         *
         * @param roaming the roaming filter.
         * @return this builder.
         */
        @NonNull
        public Builder setRoaming(@NetworkStats.Roaming int roaming) {
            mRoaming = roaming;
            return this;
        }

        /**
         * Set the default network status filter.
         *
         * @param defaultNetwork the default network status filter.
         * @return this builder.
         */
        @NonNull
        public Builder setDefaultNetworkStatus(@NetworkStats.DefaultNetwork int defaultNetwork) {
            mDefaultNetwork = defaultNetwork;
            return this;
        }

        /**
         * Set the Radio Access Technology(RAT) type filter.
         *
         * @param ratType the Radio Access Technology(RAT) type filter. Use
         *                {@link #NETWORK_TYPE_ALL} to include all network types when filtering.
         *                See {@code TelephonyManager.NETWORK_TYPE_*}.
         * @return this builder.
         */
        @NonNull
        public Builder setRatType(int ratType) {
            // Input will be validated with the match rule when building the template.
            mRatType = ratType;
            return this;
        }

        /**
         * Set the OEM managed filter.
         *
         * @param oemManaged the match rule to match different type of OEM managed network or
         *                   unmanaged networks. See {@code OEM_MANAGED_*}.
         * @return this builder.
         */
        @NonNull
        public Builder setOemManaged(@OemManaged int oemManaged) {
            mOemManaged = oemManaged;
            return this;
        }

        /**
         * Check whether the match rule is requestable.
         *
         * @param matchRule the target match rule to be checked.
         */
        private static void assertRequestableMatchRule(final int matchRule) {
            if (!isKnownMatchRule(matchRule) || matchRule == MATCH_PROXY) {
                throw new IllegalArgumentException("Invalid match rule: "
                        + getMatchRuleName(matchRule));
            }
        }

        private void assertRequestableParameters() {
            validateWifiNetworkKeys();
            // TODO: Check all the input are legitimate.
        }

        private void validateWifiNetworkKeys() {
            // Also allow querying test networks which use wifi network key as identifier.
            if (mMatchRule != MATCH_WIFI && mMatchRule != MATCH_TEST
                    && !mMatchWifiNetworkKeys.isEmpty()) {
                throw new IllegalArgumentException("Trying to build non wifi match rule: "
                        + mMatchRule + " with wifi network keys");
            }
        }

        /**
         * Builds the instance of the NetworkTemplate.
         *
         * @return the built instance of NetworkTemplate.
         */
        @NonNull
        public NetworkTemplate build() {
            assertRequestableParameters();
            return new NetworkTemplate(mMatchRule,
                    mMatchSubscriberIds.toArray(new String[0]),
                    mMatchWifiNetworkKeys.toArray(new String[0]), mMetered, mRoaming,
                    mDefaultNetwork, mRatType, mOemManaged);
        }
    }
}
