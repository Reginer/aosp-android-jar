/**
 * Copyright (c) 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi.hotspot2;

import static android.net.wifi.WifiConfiguration.METERED_OVERRIDE_NONE;
import static android.net.wifi.WifiConfiguration.MeteredOverride;

import android.annotation.CurrentTimeMillisLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.net.wifi.hotspot2.pps.Policy;
import android.net.wifi.hotspot2.pps.UpdateParameter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Class representing Passpoint configuration.  This contains configurations specified in
 * PerProviderSubscription (PPS) Management Object (MO) tree.
 *
 * For more info, refer to Hotspot 2.0 PPS MO defined in section 9.1 of the Hotspot 2.0
 * Release 2 Technical Specification.
 */
public final class PasspointConfiguration implements Parcelable {
    private static final String TAG = "PasspointConfiguration";

    /**
     * Number of bytes for certificate SHA-256 fingerprint byte array.
     */
    private static final int CERTIFICATE_SHA256_BYTES = 32;

    /**
     * Maximum bytes for URL string.
     * @hide
     */
    public static final int MAX_URL_BYTES = 2048;

    /**
     * Maximum size for match entry, just to limit the size of the Passpoint config.
     * @hide
     */
    public static final int MAX_NUMBER_OF_ENTRIES = 16;

    /**
     * Maximum size for OI entry.
     * The spec allows a string of up to 255 characters, with comma delimited numbers like
     * 001122,334455. So with minimum OI size of 7, the maximum amount of OIs is 36.
     * @hide
     */
    public static final int MAX_NUMBER_OF_OI = 36;


    /**
     * Maximum bytes for a string entry like FQDN and friendly name.
     * @hide
     */
    public static final int MAX_STRING_LENGTH = 255;

    /**
     * HESSID is 48 bit.
     * @hide
     */
    public static final long MAX_HESSID_VALUE = ((long) 1 << 48)  - 1;

    /**
     * Organization Identifiers is 3 or 5 Octets. 24 or 36 bit.
     * @hide
     */
    public static final long MAX_OI_VALUE = ((long) 1 << 40)  - 1;

    /**
     * Integer value used for indicating null value in the Parcel.
     */
    private static final int NULL_VALUE = -1;

    /**
     * Configurations under HomeSp subtree.
     */
    private HomeSp mHomeSp = null;

    /**
     * Set the Home SP (Service Provider) information.
     *
     * @param homeSp The Home SP information to set to
     */
    public void setHomeSp(HomeSp homeSp) { mHomeSp = homeSp; }
    /**
     * Get the Home SP (Service Provider) information.
     *
     * @return Home SP information
     */
    public HomeSp getHomeSp() { return mHomeSp; }

    /**
     * Configurations under AAAServerTrustedNames subtree.
     */
    private String[] mAaaServerTrustedNames = null;
    /**
     * Set the AAA server trusted names information.
     *
     * @param aaaServerTrustedNames The AAA server trusted names information to set to
     * @hide
     */
    public void setAaaServerTrustedNames(@Nullable String[] aaaServerTrustedNames) {
        mAaaServerTrustedNames = aaaServerTrustedNames;
    }
    /**
     * Get the AAA server trusted names information.
     *
     * @return AAA server trusted names information
     * @hide
     */
    public @Nullable String[] getAaaServerTrustedNames() {
        return mAaaServerTrustedNames;
    }

    /**
     * Configurations under Credential subtree.
     */
    private Credential mCredential = null;
    /**
     * Set the credential information.
     *
     * @param credential The credential information to set to
     */
    public void setCredential(Credential credential) {
        mCredential = credential;
    }
    /**
     * Get the credential information.
     *
     * @return credential information
     */
    public Credential getCredential() {
        return mCredential;
    }

    /**
     * Configurations under Policy subtree.
     */
    private Policy mPolicy = null;
    /**
     * @hide
     */
    public void setPolicy(Policy policy) {
        mPolicy = policy;
    }
    /**
     * @hide
     */
    public Policy getPolicy() {
        return mPolicy;
    }

    /**
     * Meta data for performing subscription update.
     */
    private UpdateParameter mSubscriptionUpdate = null;
    /**
     * @hide
     */
    public void setSubscriptionUpdate(UpdateParameter subscriptionUpdate) {
        mSubscriptionUpdate = subscriptionUpdate;
    }
    /**
     * @hide
     */
    public UpdateParameter getSubscriptionUpdate() {
        return mSubscriptionUpdate;
    }

    /**
     * List of HTTPS URL for retrieving trust root certificate and the corresponding SHA-256
     * fingerprint of the certificate.  The certificates are used for verifying AAA server's
     * identity during EAP authentication.
     */
    private Map<String, byte[]> mTrustRootCertList = null;
    /**
     * @hide
     */
    public void setTrustRootCertList(Map<String, byte[]> trustRootCertList) {
        mTrustRootCertList = trustRootCertList;
    }
    /**
     * @hide
     */
    public Map<String, byte[]> getTrustRootCertList() {
        return mTrustRootCertList;
    }

    /**
     * Set by the subscription server, updated every time the configuration is updated by
     * the subscription server.
     *
     * Use Integer.MIN_VALUE to indicate unset value.
     */
    private int mUpdateIdentifier = Integer.MIN_VALUE;
    /**
     * @hide
     */
    public void setUpdateIdentifier(int updateIdentifier) {
        mUpdateIdentifier = updateIdentifier;
    }
    /**
     * @hide
     */
    public int getUpdateIdentifier() {
        return mUpdateIdentifier;
    }

    /**
     * The priority of the credential.
     *
     * Use Integer.MIN_VALUE to indicate unset value.
     */
    private int mCredentialPriority = Integer.MIN_VALUE;
    /**
     * @hide
     */
    public void setCredentialPriority(int credentialPriority) {
        mCredentialPriority = credentialPriority;
    }
    /**
     * @hide
     */
    public int getCredentialPriority() {
        return mCredentialPriority;
    }

    /**
     * The time this subscription is created. It is in the format of number
     * of milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * Use Long.MIN_VALUE to indicate unset value.
     */
    private long mSubscriptionCreationTimeInMillis = Long.MIN_VALUE;
    /**
     * @hide
     */
    public void setSubscriptionCreationTimeInMillis(long subscriptionCreationTimeInMillis) {
        mSubscriptionCreationTimeInMillis = subscriptionCreationTimeInMillis;
    }
    /**
     * @hide
     */
    public long getSubscriptionCreationTimeInMillis() {
        return mSubscriptionCreationTimeInMillis;
    }

    /**
     * The time this subscription will expire. It is in the format of number
     * of milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * Use Long.MIN_VALUE to indicate unset value.
     */
    private long mSubscriptionExpirationTimeMillis = Long.MIN_VALUE;

    /**
     * Utility method to set the time this subscription will expire. The framework will not attempt
     * to auto-connect to networks using expired subscriptions.
     * @param subscriptionExpirationTimeInMillis The expiration time in the format of number of
     *                                           milliseconds since January 1, 1970, 00:00:00 GMT,
     *                                           or {@link Long#MIN_VALUE} to unset.
     */
    public void setSubscriptionExpirationTimeInMillis(@CurrentTimeMillisLong
            long subscriptionExpirationTimeInMillis) {
        mSubscriptionExpirationTimeMillis = subscriptionExpirationTimeInMillis;
    }

    /**
     *  Utility method to get the time this subscription will expire. It is in the format of number
     *  of milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     *  @return The time this subscription will expire, or Long.MIN_VALUE to indicate unset value
     */
    @CurrentTimeMillisLong
    public long getSubscriptionExpirationTimeMillis() {
        return mSubscriptionExpirationTimeMillis;
    }

    /**
     * The type of the subscription.  This is defined by the provider and the value is provider
     * specific.
     */
    private String mSubscriptionType = null;
    /**
     * @hide
     */
    public void setSubscriptionType(String subscriptionType) {
        mSubscriptionType = subscriptionType;
    }
    /**
     * @hide
     */
    public String getSubscriptionType() {
        return mSubscriptionType;
    }

    /**
     * The time period for usage statistics accumulation. A value of zero means that usage
     * statistics are not accumulated on a periodic basis (e.g., a one-time limit for
     * “pay as you go” - PAYG service). A non-zero value specifies the usage interval in minutes.
     */
    private long mUsageLimitUsageTimePeriodInMinutes = Long.MIN_VALUE;
    /**
     * @hide
     */
    public void setUsageLimitUsageTimePeriodInMinutes(long usageLimitUsageTimePeriodInMinutes) {
        mUsageLimitUsageTimePeriodInMinutes = usageLimitUsageTimePeriodInMinutes;
    }
    /**
     * @hide
     */
    public long getUsageLimitUsageTimePeriodInMinutes() {
        return mUsageLimitUsageTimePeriodInMinutes;
    }

    /**
     * The time at which usage statistic accumulation  begins.  It is in the format of number
     * of milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * Use Long.MIN_VALUE to indicate unset value.
     */
    private long mUsageLimitStartTimeInMillis = Long.MIN_VALUE;
    /**
     * @hide
     */
    public void setUsageLimitStartTimeInMillis(long usageLimitStartTimeInMillis) {
        mUsageLimitStartTimeInMillis = usageLimitStartTimeInMillis;
    }
    /**
     * @hide
     */
    public long getUsageLimitStartTimeInMillis() {
        return mUsageLimitStartTimeInMillis;
    }

    /**
     * The cumulative data limit in megabytes for the {@link #usageLimitUsageTimePeriodInMinutes}.
     * A value of zero indicate unlimited data usage.
     *
     * Use Long.MIN_VALUE to indicate unset value.
     */
    private long mUsageLimitDataLimit = Long.MIN_VALUE;
    /**
     * @hide
     */
    public void setUsageLimitDataLimit(long usageLimitDataLimit) {
        mUsageLimitDataLimit = usageLimitDataLimit;
    }
    /**
     * @hide
     */
    public long getUsageLimitDataLimit() {
        return mUsageLimitDataLimit;
    }

    /**
     * The cumulative time limit in minutes for the {@link #usageLimitUsageTimePeriodInMinutes}.
     * A value of zero indicate unlimited time usage.
     */
    private long mUsageLimitTimeLimitInMinutes = Long.MIN_VALUE;
    /**
     * @hide
     */
    public void setUsageLimitTimeLimitInMinutes(long usageLimitTimeLimitInMinutes) {
        mUsageLimitTimeLimitInMinutes = usageLimitTimeLimitInMinutes;
    }
    /**
     * @hide
     */
    public long getUsageLimitTimeLimitInMinutes() {
        return mUsageLimitTimeLimitInMinutes;
    }

    /**
     * The map of OSU service provider names whose each element is presented in different
     * languages for the service provider, which is used for finding a matching
     * PasspointConfiguration with a given service provider name.
     */
    private Map<String, String> mServiceFriendlyNames = null;

    /**
     * @hide
     */
    public void setServiceFriendlyNames(Map<String, String> serviceFriendlyNames) {
        mServiceFriendlyNames = serviceFriendlyNames;
    }

    /**
     * @hide
     */
    public Map<String, String> getServiceFriendlyNames() {
        return mServiceFriendlyNames;
    }

    /**
     * Return the friendly Name for current language from the list of friendly names of OSU
     * provider.
     * The string matching the default locale will be returned if it is found, otherwise the
     * first string in the list will be returned.  A null will be returned if the list is empty.
     *
     * @return String matching the default locale, null otherwise
     * @hide
     */
    public String getServiceFriendlyName() {
        if (mServiceFriendlyNames == null || mServiceFriendlyNames.isEmpty()) return null;
        String lang = Locale.getDefault().getLanguage();
        String friendlyName = mServiceFriendlyNames.get(lang);
        if (friendlyName != null) {
            return friendlyName;
        }
        friendlyName = mServiceFriendlyNames.get("en");
        if (friendlyName != null) {
            return friendlyName;
        }
        return mServiceFriendlyNames.get(mServiceFriendlyNames.keySet().stream().findFirst().get());
    }

    /**
     * The carrier ID identifies the operator who provides this network configuration.
     *    see {@link TelephonyManager#getSimCarrierId()}
     */
    private int mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;

    /**
     * The subscription ID identifies the SIM card who provides this network configuration.
     * See {@link SubscriptionInfo#getSubscriptionId()}
     */
    private int mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private ParcelUuid mSubscriptionGroup = null;

    /**
     * Set the carrier ID associated with current configuration.
     * @param carrierId {@code mCarrierId}
     * @hide
     */
    public void setCarrierId(int carrierId) {
        this.mCarrierId = carrierId;
    }

    /**
     * Get the carrier ID associated with current configuration.
     * @return {@code mCarrierId}
     * @hide
     */
    public int getCarrierId() {
        return mCarrierId;
    }

    /**
     * Set the subscription ID associated with current configuration.
     * @param subscriptionId {@code mSubscriptionId}
     * @hide
     */
    public void setSubscriptionId(int subscriptionId) {
        this.mSubscriptionId = subscriptionId;
    }

    /**
     * Get the carrier ID associated with current configuration.
     * @return {@code mSubscriptionId}
     * @hide
     */
    public int getSubscriptionId() {
        return mSubscriptionId;
    }

    /**
     * Set the subscription group uuid associated with current configuration.
     * @hide
     */
    public void setSubscriptionGroup(ParcelUuid subscriptionGroup) {
        this.mSubscriptionGroup = subscriptionGroup;
    }

    /**
     * Get the subscription group uuid associated with current configuration.
     * @hide
     */
    public ParcelUuid getSubscriptionGroup() {
        return this.mSubscriptionGroup;
    }

    /**
     * The auto-join configuration specifies whether or not the Passpoint Configuration is
     * considered for auto-connection. If true then yes, if false then it isn't considered as part
     * of auto-connection - but can still be manually connected to.
     */
    private boolean mIsAutojoinEnabled = true;

    /**
     * The mac randomization setting specifies whether a randomized or device MAC address will
     * be used to connect to the passpoint network. If true, a randomized MAC will be used.
     * Otherwise, the device MAC address will be used.
     */
    private boolean mIsMacRandomizationEnabled = true;

    /**
     * Whether this passpoint configuration should use non-persistent MAC randomization.
     */
    private boolean mIsNonPersistentMacRandomizationEnabled = false;


    /**
     * Indicate whether the network is oem paid or not. Networks are considered oem paid
     * if the corresponding connection is only available to system apps.
     * @hide
     */
    private boolean mIsOemPaid;

    /**
     * Indicate whether the network is oem private or not. Networks are considered oem private
     * if the corresponding connection is only available to system apps.
     * @hide
     */
    private boolean mIsOemPrivate;

    /**
     * Indicate whether or not the network is a carrier merged network.
     * @hide
     */
    private boolean mIsCarrierMerged;

    /**
     * Indicates if the end user has expressed an explicit opinion about the
     * meteredness of this network, such as through the Settings app.
     * This value is one of {@link #METERED_OVERRIDE_NONE}, {@link #METERED_OVERRIDE_METERED},
     * or {@link #METERED_OVERRIDE_NOT_METERED}.
     * <p>
     * This should always override any values from {@link WifiInfo#getMeteredHint()}.
     *
     * By default this field is set to {@link #METERED_OVERRIDE_NONE}.
     */
    private int mMeteredOverride = METERED_OVERRIDE_NONE;

    private String mDecoratedIdentityPrefix;

    /**
     * Configures the auto-association status of this Passpoint configuration. A value of true
     * indicates that the configuration will be considered for auto-connection, a value of false
     * indicates that only manual connection will work - the framework will not auto-associate to
     * this Passpoint network.
     *
     * @param autojoinEnabled true to be considered for framework auto-connection, false otherwise.
     * @hide
     */
    public void setAutojoinEnabled(boolean autojoinEnabled) {
        mIsAutojoinEnabled = autojoinEnabled;
    }

    /**
     * Configures the MAC randomization setting for this Passpoint configuration.
     * If set to true, the framework will use a randomized MAC address to connect to this Passpoint
     * network. Otherwise, the framework will use the device MAC address.
     *
     * @param enabled true to use randomized MAC address, false to use device MAC address.
     * @hide
     */
    public void setMacRandomizationEnabled(boolean enabled) {
        mIsMacRandomizationEnabled = enabled;
    }

    /**
     * This setting is only applicable if MAC randomization is enabled.
     * If set to true, the framework will periodically generate new MAC addresses for new
     * connections.
     * If set to false (the default), the framework will use the same locally generated MAC address
     * for connections to this passpoint configuration.
     * @param enabled true to use non-persistent MAC randomization, false to use persistent MAC
     *                randomization.
     * @hide
     */
    public void setNonPersistentMacRandomizationEnabled(boolean enabled) {
        mIsNonPersistentMacRandomizationEnabled = enabled;
    }

    /**
     * Sets the metered override setting for this Passpoint configuration.
     *
     * @param meteredOverride One of the values in {@link MeteredOverride}
     * @hide
     */
    public void setMeteredOverride(@MeteredOverride int meteredOverride) {
        mMeteredOverride = meteredOverride;
    }

    /**
     * Indicates whether the Passpoint configuration may be auto-connected to by the framework. A
     * value of true indicates that auto-connection can happen, a value of false indicates that it
     * cannot. However, even when auto-connection is not possible manual connection by the user is
     * possible.
     *
     * @return the auto-join configuration: true for auto-connection (or join) enabled, false
     * otherwise.
     * @hide
     */
    @SystemApi
    public boolean isAutojoinEnabled() {
        return mIsAutojoinEnabled;
    }

    /**
     * Indicates whether the user chose this configuration to be treated as metered or not.
     *
     * @return One of the values in {@link MeteredOverride}
     * @hide
     */
    @SystemApi
    @MeteredOverride
    public int getMeteredOverride() {
        return mMeteredOverride;
    }

    /**
     * Indicates whether a randomized MAC address or device MAC address will be used for
     * connections to this Passpoint network. If true, a randomized MAC address will be used.
     * Otherwise, the device MAC address will be used.
     *
     * @return true for MAC randomization enabled. False for disabled.
     * @hide
     */
    @SystemApi
    public boolean isMacRandomizationEnabled() {
        return mIsMacRandomizationEnabled;
    }

    /**
     * When MAC randomization is enabled, this indicates whether non-persistent MAC randomization or
     * persistent MAC randomization will be used for connections to this Passpoint network.
     * If true, the MAC address used for connections will periodically change. Otherwise, the same
     * locally generated MAC will be used for all connections to this passpoint configuration.
     *
     * @return true for enhanced MAC randomization enabled. False for disabled.
     * @hide
     */
    public boolean isNonPersistentMacRandomizationEnabled() {
        return mIsNonPersistentMacRandomizationEnabled;
    }

    /**
     * Set whether the network is oem paid or not.
     * @hide
     */
    public void setOemPaid(boolean isOemPaid) {
        mIsOemPaid = isOemPaid;
    }

    /**
     * Get whether the network is oem paid or not.
     * @hide
     */
    public boolean isOemPaid() {
        return mIsOemPaid;
    }

    /**
     * Set whether the network is oem private or not.
     * @hide
     */
    public void setOemPrivate(boolean isOemPrivate) {
        mIsOemPrivate = isOemPrivate;
    }

    /**
     * Get whether the network is oem private or not.
     * @hide
     */
    public boolean isOemPrivate() {
        return mIsOemPrivate;
    }

    /**
     * Set whether the network is carrier merged or not.
     * @hide
     */
    public void setCarrierMerged(boolean isCarrierMerged) {
        mIsCarrierMerged = isCarrierMerged;
    }

    /**
     * Get whether the network is carrier merged or not.
     * @hide
     */
    public boolean isCarrierMerged() {
        return mIsCarrierMerged;
    }

    /**
     * Constructor for creating PasspointConfiguration with default values.
     */
    public PasspointConfiguration() {}

    /**
     * Copy constructor.
     *
     * @param source The source to copy from
     */
    public PasspointConfiguration(PasspointConfiguration source) {
        if (source == null) {
            return;
        }

        if (source.mHomeSp != null) {
            mHomeSp = new HomeSp(source.mHomeSp);
        }
        if (source.mCredential != null) {
            mCredential = new Credential(source.mCredential);
        }
        if (source.mPolicy != null) {
            mPolicy = new Policy(source.mPolicy);
        }
        if (source.mTrustRootCertList != null) {
            mTrustRootCertList = Collections.unmodifiableMap(source.mTrustRootCertList);
        }
        if (source.mSubscriptionUpdate != null) {
            mSubscriptionUpdate = new UpdateParameter(source.mSubscriptionUpdate);
        }
        mUpdateIdentifier = source.mUpdateIdentifier;
        mCredentialPriority = source.mCredentialPriority;
        mSubscriptionCreationTimeInMillis = source.mSubscriptionCreationTimeInMillis;
        mSubscriptionExpirationTimeMillis = source.mSubscriptionExpirationTimeMillis;
        mSubscriptionType = source.mSubscriptionType;
        mUsageLimitDataLimit = source.mUsageLimitDataLimit;
        mUsageLimitStartTimeInMillis = source.mUsageLimitStartTimeInMillis;
        mUsageLimitTimeLimitInMinutes = source.mUsageLimitTimeLimitInMinutes;
        mUsageLimitUsageTimePeriodInMinutes = source.mUsageLimitUsageTimePeriodInMinutes;
        mServiceFriendlyNames = source.mServiceFriendlyNames;
        mAaaServerTrustedNames = source.mAaaServerTrustedNames;
        mCarrierId = source.mCarrierId;
        mSubscriptionId = source.mSubscriptionId;
        mIsAutojoinEnabled = source.mIsAutojoinEnabled;
        mIsMacRandomizationEnabled = source.mIsMacRandomizationEnabled;
        mIsNonPersistentMacRandomizationEnabled = source.mIsNonPersistentMacRandomizationEnabled;
        mMeteredOverride = source.mMeteredOverride;
        mIsCarrierMerged = source.mIsCarrierMerged;
        mIsOemPaid = source.mIsOemPaid;
        mIsOemPrivate = source.mIsOemPrivate;
        mDecoratedIdentityPrefix = source.mDecoratedIdentityPrefix;
        mSubscriptionGroup = source.mSubscriptionGroup;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mHomeSp, flags);
        dest.writeParcelable(mCredential, flags);
        dest.writeParcelable(mPolicy, flags);
        dest.writeParcelable(mSubscriptionUpdate, flags);
        writeTrustRootCerts(dest, mTrustRootCertList);
        dest.writeInt(mUpdateIdentifier);
        dest.writeInt(mCredentialPriority);
        dest.writeLong(mSubscriptionCreationTimeInMillis);
        dest.writeLong(mSubscriptionExpirationTimeMillis);
        dest.writeString(mSubscriptionType);
        dest.writeLong(mUsageLimitUsageTimePeriodInMinutes);
        dest.writeLong(mUsageLimitStartTimeInMillis);
        dest.writeLong(mUsageLimitDataLimit);
        dest.writeLong(mUsageLimitTimeLimitInMinutes);
        dest.writeStringArray(mAaaServerTrustedNames);
        Bundle bundle = new Bundle();
        bundle.putSerializable("serviceFriendlyNames",
                (HashMap<String, String>) mServiceFriendlyNames);
        dest.writeBundle(bundle);
        dest.writeInt(mCarrierId);
        dest.writeBoolean(mIsAutojoinEnabled);
        dest.writeBoolean(mIsMacRandomizationEnabled);
        dest.writeBoolean(mIsNonPersistentMacRandomizationEnabled);
        dest.writeInt(mMeteredOverride);
        dest.writeInt(mSubscriptionId);
        dest.writeBoolean(mIsCarrierMerged);
        dest.writeBoolean(mIsOemPaid);
        dest.writeBoolean(mIsOemPrivate);
        dest.writeString(mDecoratedIdentityPrefix);
        dest.writeParcelable(mSubscriptionGroup, flags);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof PasspointConfiguration)) {
            return false;
        }
        PasspointConfiguration that = (PasspointConfiguration) thatObject;
        return (mHomeSp == null ? that.mHomeSp == null : mHomeSp.equals(that.mHomeSp))
                && (mAaaServerTrustedNames == null ? that.mAaaServerTrustedNames == null
                : Arrays.equals(mAaaServerTrustedNames, that.mAaaServerTrustedNames))
                && (mCredential == null ? that.mCredential == null
                : mCredential.equals(that.mCredential))
                && (mPolicy == null ? that.mPolicy == null : mPolicy.equals(that.mPolicy))
                && (mSubscriptionUpdate == null ? that.mSubscriptionUpdate == null
                : mSubscriptionUpdate.equals(that.mSubscriptionUpdate))
                && isTrustRootCertListEquals(mTrustRootCertList, that.mTrustRootCertList)
                && mUpdateIdentifier == that.mUpdateIdentifier
                && mCredentialPriority == that.mCredentialPriority
                && mSubscriptionCreationTimeInMillis == that.mSubscriptionCreationTimeInMillis
                && mSubscriptionExpirationTimeMillis == that.mSubscriptionExpirationTimeMillis
                && TextUtils.equals(mSubscriptionType, that.mSubscriptionType)
                && mUsageLimitUsageTimePeriodInMinutes == that.mUsageLimitUsageTimePeriodInMinutes
                && mUsageLimitStartTimeInMillis == that.mUsageLimitStartTimeInMillis
                && mUsageLimitDataLimit == that.mUsageLimitDataLimit
                && mUsageLimitTimeLimitInMinutes == that.mUsageLimitTimeLimitInMinutes
                && mCarrierId == that.mCarrierId
                && mSubscriptionId == that.mSubscriptionId
                && mIsOemPrivate == that.mIsOemPrivate
                && mIsOemPaid == that.mIsOemPaid
                && mIsCarrierMerged == that.mIsCarrierMerged
                && mIsAutojoinEnabled == that.mIsAutojoinEnabled
                && mIsMacRandomizationEnabled == that.mIsMacRandomizationEnabled
                && mIsNonPersistentMacRandomizationEnabled
                == that.mIsNonPersistentMacRandomizationEnabled
                && mMeteredOverride == that.mMeteredOverride
                && (mServiceFriendlyNames == null ? that.mServiceFriendlyNames == null
                : mServiceFriendlyNames.equals(that.mServiceFriendlyNames))
                && Objects.equals(mDecoratedIdentityPrefix, that.mDecoratedIdentityPrefix)
                && Objects.equals(mSubscriptionGroup, that.mSubscriptionGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHomeSp, mCredential, mPolicy, mSubscriptionUpdate, mTrustRootCertList,
                mUpdateIdentifier, mCredentialPriority, mSubscriptionCreationTimeInMillis,
                mSubscriptionExpirationTimeMillis, mUsageLimitUsageTimePeriodInMinutes,
                mUsageLimitStartTimeInMillis, mUsageLimitDataLimit, mUsageLimitTimeLimitInMinutes,
                mServiceFriendlyNames, mCarrierId, mIsAutojoinEnabled, mIsMacRandomizationEnabled,
                mIsNonPersistentMacRandomizationEnabled, mMeteredOverride, mSubscriptionId,
                mIsCarrierMerged, mIsOemPaid, mIsOemPrivate, mDecoratedIdentityPrefix,
                mSubscriptionGroup);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UpdateIdentifier: ").append(mUpdateIdentifier).append("\n");
        builder.append("CredentialPriority: ").append(mCredentialPriority).append("\n");
        builder.append("SubscriptionCreationTime: ").append(
                mSubscriptionCreationTimeInMillis != Long.MIN_VALUE
                ? new Date(mSubscriptionCreationTimeInMillis) : "Not specified").append("\n");
        builder.append("SubscriptionExpirationTime: ").append(
                mSubscriptionExpirationTimeMillis != Long.MIN_VALUE
                ? new Date(mSubscriptionExpirationTimeMillis) : "Not specified").append("\n");
        builder.append("UsageLimitStartTime: ").append(mUsageLimitStartTimeInMillis != Long.MIN_VALUE
                ? new Date(mUsageLimitStartTimeInMillis) : "Not specified").append("\n");
        builder.append("UsageTimePeriod: ").append(mUsageLimitUsageTimePeriodInMinutes)
                .append("\n");
        builder.append("UsageLimitDataLimit: ").append(mUsageLimitDataLimit).append("\n");
        builder.append("UsageLimitTimeLimit: ").append(mUsageLimitTimeLimitInMinutes).append("\n");
        builder.append("Provisioned by a subscription server: ")
                .append(isOsuProvisioned() ? "Yes" : "No").append("\n");
        if (mHomeSp != null) {
            builder.append("HomeSP Begin ---\n");
            builder.append(mHomeSp);
            builder.append("HomeSP End ---\n");
        }
        if (mCredential != null) {
            builder.append("Credential Begin ---\n");
            builder.append(mCredential);
            builder.append("Credential End ---\n");
        }
        if (mPolicy != null) {
            builder.append("Policy Begin ---\n");
            builder.append(mPolicy);
            builder.append("Policy End ---\n");
        }
        if (mSubscriptionUpdate != null) {
            builder.append("SubscriptionUpdate Begin ---\n");
            builder.append(mSubscriptionUpdate);
            builder.append("SubscriptionUpdate End ---\n");
        }
        if (mTrustRootCertList != null) {
            builder.append("TrustRootCertServers: ").append(mTrustRootCertList.keySet())
                    .append("\n");
        }
        if (mAaaServerTrustedNames != null) {
            builder.append("AAAServerTrustedNames: ")
                    .append(String.join(";", mAaaServerTrustedNames)).append("\n");
        }
        if (mServiceFriendlyNames != null) {
            builder.append("ServiceFriendlyNames: ").append(mServiceFriendlyNames);
        }
        builder.append("CarrierId:" + mCarrierId);
        builder.append("SubscriptionId:" + mSubscriptionId);
        builder.append("IsAutojoinEnabled:" + mIsAutojoinEnabled);
        builder.append("mIsMacRandomizationEnabled:" + mIsMacRandomizationEnabled);
        builder.append("mIsNonPersistentMacRandomizationEnabled:"
                + mIsNonPersistentMacRandomizationEnabled);
        builder.append("mMeteredOverride:" + mMeteredOverride);
        builder.append("mIsCarrierMerged:" + mIsCarrierMerged);
        builder.append("mIsOemPaid:" + mIsOemPaid);
        builder.append("mIsOemPrivate:" + mIsOemPrivate);
        builder.append("mDecoratedUsernamePrefix:" + mDecoratedIdentityPrefix);
        builder.append("mSubscriptionGroup:" + mSubscriptionGroup);
        return builder.toString();
    }

    /**
     * Validate the R1 configuration data.
     *
     * @return true on success or false on failure
     * @hide
     */
    public boolean validate() {
        // Optional: PerProviderSubscription/<X+>/SubscriptionUpdate
        if (mSubscriptionUpdate != null && !mSubscriptionUpdate.validate()) {
            return false;
        }
        return validateForCommonR1andR2();
    }

    /**
     * Validate the R2 configuration data.
     *
     * @return true on success or false on failure
     * @hide
     */
    public boolean validateForR2() {
        // Required: PerProviderSubscription/UpdateIdentifier
        if (mUpdateIdentifier == Integer.MIN_VALUE) {
            return false;
        }

        // Required: PerProviderSubscription/<X+>/SubscriptionUpdate
        if (mSubscriptionUpdate == null || !mSubscriptionUpdate.validate()) {
            return false;
        }
        return validateForCommonR1andR2();
    }

    private boolean validateForCommonR1andR2() {
        // Required: PerProviderSubscription/<X+>/HomeSP
        if (mHomeSp == null || !mHomeSp.validate()) {
            return false;
        }

        // Required: PerProviderSubscription/<X+>/Credential
        if (mCredential == null || !mCredential.validate()) {
            return false;
        }

        // Optional: PerProviderSubscription/<X+>/Policy
        if (mPolicy != null && !mPolicy.validate()) {
            return false;
        }
        // Optional: DecoratedIdentityPrefix
        if (!TextUtils.isEmpty(mDecoratedIdentityPrefix)) {
            if (!mDecoratedIdentityPrefix.endsWith("!")) {
                EventLog.writeEvent(0x534e4554, "246539931", -1,
                    "Invalid decorated identity prefix");
                return false;
            }
            String[] decoratedIdentityPrefixArray = mDecoratedIdentityPrefix.split("!");
            if (decoratedIdentityPrefixArray.length > MAX_NUMBER_OF_ENTRIES) {
                Log.e(TAG, "too many decoratedIdentityPrefix");
                return false;
            }
            for (String prefix : decoratedIdentityPrefixArray) {
                if (prefix.length() > MAX_STRING_LENGTH) {
                    Log.e(TAG, "The decoratedIdentityPrefix is too long: " + prefix);
                    return false;
                }
            }
        }

        if (mAaaServerTrustedNames != null) {
            if (mAaaServerTrustedNames.length > MAX_NUMBER_OF_ENTRIES) {
                Log.e(TAG, "Too many AaaServerTrustedNames");
                return false;
            }
            for (String fqdn : mAaaServerTrustedNames) {
                if (fqdn.getBytes(StandardCharsets.UTF_8).length > MAX_STRING_LENGTH) {
                    Log.e(TAG, "AaaServerTrustedNames is too long");
                    return false;
                }
            }
        }
        if (mSubscriptionType != null) {
            if (mSubscriptionType.getBytes(StandardCharsets.UTF_8).length > MAX_STRING_LENGTH) {
                Log.e(TAG, "SubscriptionType is too long");
                return false;
            }
        }

        if (mTrustRootCertList != null) {
            if (mTrustRootCertList.size() > MAX_NUMBER_OF_ENTRIES) {
                Log.e(TAG, "Too many TrustRootCert");
                return false;
            }
            for (Map.Entry<String, byte[]> entry : mTrustRootCertList.entrySet()) {
                String url = entry.getKey();
                byte[] certFingerprint = entry.getValue();
                if (TextUtils.isEmpty(url)) {
                    Log.e(TAG, "Empty URL");
                    return false;
                }
                if (url.getBytes(StandardCharsets.UTF_8).length > MAX_URL_BYTES) {
                    Log.e(TAG, "URL bytes exceeded the max: "
                            + url.getBytes(StandardCharsets.UTF_8).length);
                    return false;
                }

                if (certFingerprint == null) {
                    Log.e(TAG, "Fingerprint not specified");
                    return false;
                }
                if (certFingerprint.length != CERTIFICATE_SHA256_BYTES) {
                    Log.e(TAG, "Incorrect size of trust root certificate SHA-256 fingerprint: "
                            + certFingerprint.length);
                    return false;
                }
            }
        }

        if (mServiceFriendlyNames != null) {
            if (mServiceFriendlyNames.size() > MAX_NUMBER_OF_ENTRIES) {
                Log.e(TAG, "ServiceFriendlyNames exceed the max!");
                return false;
            }
            for (Map.Entry<String, String> names : mServiceFriendlyNames.entrySet()) {
                if (names.getKey() == null || names.getValue() == null) {
                    Log.e(TAG, "Service friendly name entry should not be null");
                    return false;
                }
                if (names.getKey().length() > MAX_STRING_LENGTH
                        || names.getValue().length() > MAX_STRING_LENGTH) {
                    Log.e(TAG, "Service friendly name is to long");
                    return false;
                }
            }
        }
        return true;
    }

    public static final @android.annotation.NonNull Creator<PasspointConfiguration> CREATOR =
        new Creator<PasspointConfiguration>() {
            @Override
            public PasspointConfiguration createFromParcel(Parcel in) {
                PasspointConfiguration config = new PasspointConfiguration();
                config.setHomeSp(in.readParcelable(null));
                config.setCredential(in.readParcelable(null));
                config.setPolicy(in.readParcelable(null));
                config.setSubscriptionUpdate(in.readParcelable(null));
                config.setTrustRootCertList(readTrustRootCerts(in));
                config.setUpdateIdentifier(in.readInt());
                config.setCredentialPriority(in.readInt());
                config.setSubscriptionCreationTimeInMillis(in.readLong());
                config.setSubscriptionExpirationTimeInMillis(in.readLong());
                config.setSubscriptionType(in.readString());
                config.setUsageLimitUsageTimePeriodInMinutes(in.readLong());
                config.setUsageLimitStartTimeInMillis(in.readLong());
                config.setUsageLimitDataLimit(in.readLong());
                config.setUsageLimitTimeLimitInMinutes(in.readLong());
                config.setAaaServerTrustedNames(in.createStringArray());
                Bundle bundle = in.readBundle();
                Map<String, String> friendlyNamesMap = (HashMap) bundle.getSerializable(
                        "serviceFriendlyNames");
                config.setServiceFriendlyNames(friendlyNamesMap);
                config.mCarrierId = in.readInt();
                config.mIsAutojoinEnabled = in.readBoolean();
                config.mIsMacRandomizationEnabled = in.readBoolean();
                config.mIsNonPersistentMacRandomizationEnabled = in.readBoolean();
                config.mMeteredOverride = in.readInt();
                config.mSubscriptionId = in.readInt();
                config.mIsCarrierMerged = in.readBoolean();
                config.mIsOemPaid = in.readBoolean();
                config.mIsOemPrivate = in.readBoolean();
                config.mDecoratedIdentityPrefix = in.readString();
                config.mSubscriptionGroup = in.readParcelable(null);

                return config;
            }

            @Override
            public PasspointConfiguration[] newArray(int size) {
                return new PasspointConfiguration[size];
            }

            /**
             * Helper function for reading trust root certificate info list from a Parcel.
             *
             * @param in The Parcel to read from
             * @return The list of trust root certificate URL with the corresponding certificate
             *         fingerprint
             */
            private Map<String, byte[]> readTrustRootCerts(Parcel in) {
                int size = in.readInt();
                if (size == NULL_VALUE) {
                    return null;
                }
                Map<String, byte[]> trustRootCerts = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    String key = in.readString();
                    byte[] value = in.createByteArray();
                    trustRootCerts.put(key, value);
                }
                return trustRootCerts;
            }
        };

    /**
     * Helper function for writing trust root certificate information list.
     *
     * @param dest The Parcel to write to
     * @param trustRootCerts The list of trust root certificate URL with the corresponding
     *                       certificate fingerprint
     */
    private static void writeTrustRootCerts(Parcel dest, Map<String, byte[]> trustRootCerts) {
        if (trustRootCerts == null) {
            dest.writeInt(NULL_VALUE);
            return;
        }
        dest.writeInt(trustRootCerts.size());
        for (Map.Entry<String, byte[]> entry : trustRootCerts.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeByteArray(entry.getValue());
        }
    }

    /**
     * Helper function for comparing two trust root certificate list.  Cannot use Map#equals
     * method since the value type (byte[]) doesn't override equals method.
     *
     * @param list1 The first trust root certificate list
     * @param list2 The second trust root certificate list
     * @return true if the two list are equal
     */
    private static boolean isTrustRootCertListEquals(Map<String, byte[]> list1,
            Map<String, byte[]> list2) {
        if (list1 == null || list2 == null) {
            return list1 == list2;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        for (Map.Entry<String, byte[]> entry : list1.entrySet()) {
            if (!Arrays.equals(entry.getValue(), list2.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates if the Passpoint Configuration was provisioned by a subscription (OSU) server,
     * which means that it's an R2 (or R3) profile.
     *
     * @return true if the Passpoint Configuration was provisioned by a subscription server.
     */
    public boolean isOsuProvisioned() {
        return getUpdateIdentifier() != Integer.MIN_VALUE;
    }

    /**
     * Get a unique identifier for a PasspointConfiguration object. The identifier depends on the
     * configuration that identify the service provider under the HomeSp subtree, and on the
     * credential configuration under the Credential subtree.
     * The method throws an {@link IllegalStateException} if the configuration under HomeSp subtree
     * or the configuration under Credential subtree are not initialized.
     *
     * @return A unique identifier
     */
    public @NonNull String getUniqueId() {
        if (mCredential == null || mHomeSp == null || TextUtils.isEmpty(mHomeSp.getFqdn())) {
            throw new IllegalStateException("Credential or HomeSP are not initialized");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s_%x%x", mHomeSp.getFqdn(), mHomeSp.getUniqueId(),
                mCredential.getUniqueId()));
        return sb.toString();
    }

    /**
     * Set a prefix for a decorated identity as per RFC 7542.
     * This prefix must contain a list of realms (could be a list of 1) delimited by a '!'
     * character. e.g. homerealm.example.org! or proxyrealm.example.net!homerealm.example.org!
     * A prefix of "homerealm.example.org!" will generate a decorated identity that
     * looks like: homerealm.example.org!user@otherrealm.example.net
     * Calling with a null parameter will clear the decorated prefix.
     * Note: Caller must verify that the device supports this feature by calling
     * {@link WifiManager#isDecoratedIdentitySupported()}
     *
     * @param decoratedIdentityPrefix The prefix to add to the outer/anonymous identity
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public void setDecoratedIdentityPrefix(@Nullable String decoratedIdentityPrefix) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        if (!TextUtils.isEmpty(decoratedIdentityPrefix) && !decoratedIdentityPrefix.endsWith("!")) {
            throw new IllegalArgumentException(
                    "Decorated identity prefix must be delimited by '!'");
        }
        mDecoratedIdentityPrefix = decoratedIdentityPrefix;
    }

    /**
     * Get the decorated identity prefix.
     *
     * @return The decorated identity prefix
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public @Nullable String getDecoratedIdentityPrefix() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mDecoratedIdentityPrefix;
    }
}
