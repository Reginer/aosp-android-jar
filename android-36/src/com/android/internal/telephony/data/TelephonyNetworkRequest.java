/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.internal.telephony.data;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.os.SystemClock;
import android.telephony.Annotation.ConnectivityTransport;
import android.telephony.Annotation.NetCapability;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataProfile;
import android.telephony.data.TrafficDescriptor;
import android.telephony.data.TrafficDescriptor.OsAppId;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.flags.FeatureFlags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TelephonyNetworkRequest is a wrapper class on top of {@link NetworkRequest}, which is originated
 * from the apps to request network. This class is intended to track supplemental information
 * related to this request, for example priority, evaluation result, whether this request is
 * actively being satisfied, timestamp, etc...
 *
 */
public class TelephonyNetworkRequest {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"REQUEST_STATE_"},
            value = {
                    REQUEST_STATE_UNSATISFIED,
                    REQUEST_STATE_SATISFIED})
    public @interface RequestState {}

    /**
     * Indicating currently no data networks can satisfy this network request.
     */
    public static final int REQUEST_STATE_UNSATISFIED = 0;

    /**
     * Indicating this request is already satisfied. It must have an attached network (which could
     * be in any state, including disconnecting). Also note this does not mean the network request
     * is satisfied in telephony layer. Whether the network request is finally satisfied or not is
     * determined at the connectivity service layer.
     */
    public static final int REQUEST_STATE_SATISFIED = 1;

    /** @hide */
    @IntDef(flag = true, prefix = { "CAPABILITY_ATTRIBUTE_" }, value = {
            CAPABILITY_ATTRIBUTE_NONE,
            CAPABILITY_ATTRIBUTE_APN_SETTING,
            CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN,
            CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetCapabilityAttribute {}

    /** Network capability attribute none. */
    public static final int CAPABILITY_ATTRIBUTE_NONE = 0;

    /**
     * The network capability should result in filling {@link ApnSetting} in {@link DataProfile}.
     */
    public static final int CAPABILITY_ATTRIBUTE_APN_SETTING = 1;

    /** The network capability should result in filling DNN in {@link TrafficDescriptor}. */
    public static final int CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN = 1 << 1;

    /** The network capability should result in filling OS/APP id in {@link TrafficDescriptor}. */
    public static final int CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID = 1 << 2;

    /**
     * Describes the attributes of network capabilities. Different capabilities can be translated
     * to different fields in {@link DataProfile}, or might be expanded to support special actions
     * in telephony in the future.
     */
    private static final Map<Integer, Integer> CAPABILITY_ATTRIBUTE_MAP = Map.ofEntries(
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_MMS,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_SUPL,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_DUN,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_FOTA,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_IMS,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_CBS,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN
                            | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_XCAP,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_EIMS,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_INTERNET,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_MCX,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN
                            | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_VSIM,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_BIP,
                    CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY,
                    CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH,
                    CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID),
            new SimpleImmutableEntry<>(NetworkCapabilities.NET_CAPABILITY_RCS,
                CAPABILITY_ATTRIBUTE_APN_SETTING | CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN)
    );

    /**
     * Native network request from the clients. See {@link NetworkRequest};
     */
    @NonNull
    private final NetworkRequest mNativeNetworkRequest;

    /**
     * The attributes of the network capabilities in this network request. This describes how this
     * network request can be translated to different fields in {@link DataProfile} or perform
     * special actions in telephony.
     */
    @NetCapabilityAttribute
    private final int mCapabilitiesAttributes;

    /**
     * Priority of the network request. The network request has higher priority will be satisfied
     * first than lower priority ones.
     */
    private int mPriority;

    /**
     * Data config manager for retrieving data config.
     */
    @Nullable
    private DataConfigManager mDataConfigManager;

    /**
     * The attached data network. Note that the data network could be in any state. {@code null}
     * indicates this network request is not satisfied.
     */
    @Nullable
    private DataNetwork mAttachedDataNetwork;

    /**
     * The state of the network request.
     *
     * @see #REQUEST_STATE_UNSATISFIED
     * @see #REQUEST_STATE_SATISFIED
     */
    // This is not a boolean because there might be more states in the future.
    @RequestState
    private int mState;

    /** The timestamp when this network request enters telephony. */
    @ElapsedRealtimeLong
    private final long mCreatedTimeMillis;

    /** The data evaluation result. */
    @Nullable
    private DataEvaluation mEvaluation;

    /** Feature flag. */
    @NonNull
    private final FeatureFlags mFeatureFlags;

    /**
     * Constructor
     *
     * @param request The native network request from the clients.
     * @param phone The phone instance
     * @param featureFlags The feature flag
     */
    public TelephonyNetworkRequest(@NonNull NetworkRequest request, @NonNull Phone phone,
                                   @NonNull FeatureFlags featureFlags) {
        this(request, featureFlags);
        mDataConfigManager = phone.getDataNetworkController().getDataConfigManager();
        updatePriority();
    }

    /**
     * Constructor
     *
     * @param request The native network request from the clients.
     * @param featureFlags The feature flag
     */
    public TelephonyNetworkRequest(@NonNull NetworkRequest request,
                                   @NonNull FeatureFlags featureFlags) {
        mNativeNetworkRequest = request;
        mFeatureFlags = featureFlags;

        int capabilitiesAttributes = CAPABILITY_ATTRIBUTE_NONE;
        for (int networkCapability : mNativeNetworkRequest.getCapabilities()) {
            capabilitiesAttributes |= CAPABILITY_ATTRIBUTE_MAP.getOrDefault(
                    networkCapability, CAPABILITY_ATTRIBUTE_NONE);
        }
        mCapabilitiesAttributes = capabilitiesAttributes;

        mPriority = 0;
        mAttachedDataNetwork = null;
        // When the request was first created, it is in active state so we can actively attempt
        // to satisfy it.
        mState = REQUEST_STATE_UNSATISFIED;
        mCreatedTimeMillis = SystemClock.elapsedRealtime();
    }

    /**
     * Update the associated data config manager.
     *
     * @param dataConfigManager Data config manager
     */
    public void updateDataConfig(@NonNull DataConfigManager dataConfigManager) {
        mDataConfigManager = dataConfigManager;
        updatePriority();
    }

    /**
     * @see NetworkRequest#getNetworkSpecifier()
     */
    @Nullable
    public NetworkSpecifier getNetworkSpecifier() {
        return mNativeNetworkRequest.getNetworkSpecifier();
    }

    /**
     * @see NetworkRequest#getCapabilities()
     */
    @NonNull
    @NetCapability
    public int[] getCapabilities() {
        return mNativeNetworkRequest.getCapabilities();
    }

    /**
     * @see NetworkRequest#hasCapability(int)
     */
    public boolean hasCapability(@NetCapability int capability) {
        return mNativeNetworkRequest.hasCapability(capability);
    }

    /**
     * @see NetworkRequest#getTransportTypes()
     */
    @NonNull
    @ConnectivityTransport
    public int[] getTransportTypes() {
        return mNativeNetworkRequest.getTransportTypes();
    }

    /**
     * @return {@code true} if the request can be served on the specified transport.
     * @see NetworkRequest#hasTransport
     */
    public boolean hasTransport(@ConnectivityTransport int transport) {
        return mNativeNetworkRequest.hasTransport(transport);
    }

    /**
     * @see NetworkRequest#canBeSatisfiedBy(NetworkCapabilities)
     */
    public boolean canBeSatisfiedBy(@Nullable NetworkCapabilities nc) {
        return mNativeNetworkRequest.canBeSatisfiedBy(nc);
    }


    /**
     * Check if the request's capabilities have certain attributes.
     *
     * @param capabilitiesAttributes The attributes to check.
     * @return {@code true} if the capabilities have provided attributes.
     *
     * @see NetCapabilityAttribute
     */
    public boolean hasAttribute(@NetCapabilityAttribute int capabilitiesAttributes) {
        return (mCapabilitiesAttributes & capabilitiesAttributes) == capabilitiesAttributes;
    }

    /**
     * Check if this network request can be satisfied by a data profile.
     *
     * @param dataProfile The data profile to check.
     * @return {@code true} if this network request can be satisfied by the data profile.
     */
    public boolean canBeSatisfiedBy(@NonNull DataProfile dataProfile) {
        // If the network request can be translated to OS/App id, then check if the data profile's
        // OS/App id can satisfy it.
        if (hasAttribute(CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID)
                && getOsAppId() != null) {
            // The network request has traffic descriptor type capabilities. Match the traffic
            // descriptor.
            if (dataProfile.getTrafficDescriptor() != null && Arrays.equals(getOsAppId().getBytes(),
                    dataProfile.getTrafficDescriptor().getOsAppId())) {
                return true;
            }
        }

        // If the network request can be translated to APN setting or DNN in traffic descriptor,
        // then check if the data profile's APN setting can satisfy it.
        if ((hasAttribute(CAPABILITY_ATTRIBUTE_APN_SETTING)
                || hasAttribute(CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN))
                && dataProfile.getApnSetting() != null) {
            if (mNativeNetworkRequest.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    && !mNativeNetworkRequest.hasTransport(
                            NetworkCapabilities.TRANSPORT_SATELLITE)) {
                if (mDataConfigManager != null) {
                    if (Arrays.stream(getCapabilities()).noneMatch(mDataConfigManager
                            .getForcedCellularTransportCapabilities()::contains)) {
                        // If the request is explicitly for the cellular, then the data profile
                        // needs to support cellular.
                        if (!dataProfile.getApnSetting().isForInfrastructure(
                                ApnSetting.INFRASTRUCTURE_CELLULAR)) {
                            return false;
                        }
                    }
                }
            } else if (mNativeNetworkRequest.hasTransport(
                    NetworkCapabilities.TRANSPORT_SATELLITE)
                    && !mNativeNetworkRequest.hasTransport(
                            NetworkCapabilities.TRANSPORT_CELLULAR)) {
                // If the request is explicitly for the satellite, then the data profile needs
                // to support satellite.
                if (!dataProfile.getApnSetting().isForInfrastructure(
                        ApnSetting.INFRASTRUCTURE_SATELLITE)) {
                    return false;
                }
            }
            // Fallback to the legacy APN type matching.
            List<Integer> apnTypes = Arrays.stream(getCapabilities()).boxed()
                    .map(DataUtils::networkCapabilityToApnType)
                    .filter(apnType -> apnType != ApnSetting.TYPE_NONE)
                    .collect(Collectors.toList());
            // In case of enterprise network request, the network request will have internet,
            // but APN type will not have default type as the enterprise apn should not be used
            // as default network. Ignore default type of the network request if it
            // has enterprise type as well. This will make sure the network request with
            // internet and enterprise will be satisfied with data profile with enterprise at the
            // same time default network request will not get satisfied with enterprise data
            // profile.
            // TODO b/232264746
            if (apnTypes.contains(ApnSetting.TYPE_ENTERPRISE)) {
                apnTypes.remove((Integer) ApnSetting.TYPE_DEFAULT);
            }

            return apnTypes.stream().allMatch(dataProfile.getApnSetting()::canHandleType);
        }
        return false;
    }

    /**
     * Get the priority of the network request.
     *
     * @return The priority from 0 to 100. 100 indicates the highest priority.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * Update the priority from data config manager.
     */
    public void updatePriority() {
        if (mDataConfigManager != null) {
            mPriority = Arrays.stream(mNativeNetworkRequest.getCapabilities())
                    .map(mDataConfigManager::getNetworkCapabilityPriority)
                    .max()
                    .orElse(0);
        }
    }

    /**
     * Get the network capability which is APN-type based from the network request. If there are
     * multiple APN types capability, the highest priority one will be returned.
     *
     * @return The highest priority APN type based network capability from this network request. -1
     * if there is no APN type capabilities in this network request.
     */
    @NetCapability
    public int getHighestPriorityApnTypeNetworkCapability() {
        if (!hasAttribute(CAPABILITY_ATTRIBUTE_APN_SETTING)) return -1;
        if (mDataConfigManager == null) return -1;
        return Arrays.stream(getCapabilities()).boxed()
                .filter(cap -> DataUtils.networkCapabilityToApnType(cap) != ApnSetting.TYPE_NONE)
                .max(Comparator.comparingInt(mDataConfigManager::getNetworkCapabilityPriority))
                .orElse(-1);
    }

    /**
     * A parent set of {@link #getHighestPriorityApnTypeNetworkCapability()}.
     * Get the network capability from the network request that can lead to data setup. If there are
     * multiple capabilities, the highest priority one will be returned.
     *
     * @return The highest priority traffic descriptor type network capability from this network
     * request. -1 if there is no traffic descriptor type capabilities in this network request.
     */
    @NetCapability
    public int getHighestPrioritySupportedNetworkCapability() {
        if (mDataConfigManager == null) return -1;
        return Arrays.stream(getCapabilities()).boxed()
                .filter(CAPABILITY_ATTRIBUTE_MAP::containsKey)
                .max(Comparator.comparingInt(mDataConfigManager::getNetworkCapabilityPriority))
                .orElse(-1);
    }

    /**
     * @return Get all the network capabilities that can lead to data setup.
     */
    @NonNull
    @NetCapability
    public static List<Integer> getAllSupportedNetworkCapabilities() {
        Set<Integer> unsupportedCaps = PhoneFactory.getDefaultPhone()
                .getDataNetworkController().getDataConfigManager()
                .getUnsupportedNetworkCapabilities();
        return CAPABILITY_ATTRIBUTE_MAP.keySet().stream()
                .filter(cap -> !unsupportedCaps.contains(cap)).toList();
    }

    /**
     * @return The native network request.
     */
    @NonNull
    public NetworkRequest getNativeNetworkRequest() {
        return mNativeNetworkRequest;
    }

    /**
     * Set the attached data network.
     *
     * @param dataNetwork The data network.
     */
    public void setAttachedNetwork(@Nullable DataNetwork dataNetwork) {
        mAttachedDataNetwork = dataNetwork;
    }

    /**
     * @return The attached network. {@code null} indicates the request is not attached to any
     * network (i.e. the request is unsatisfied).
     */
    @Nullable
    public DataNetwork getAttachedNetwork() {
        return mAttachedDataNetwork;
    }

    /**
     * Set the state of the network request.
     *
     * @param state The state.
     */
    public void setState(@RequestState int state) {
        mState = state;
    }

    /**
     * @return The state of the network request.
     */
    @RequestState
    public int getState() {
        return mState;
    }

    /**
     * Set the data evaluation result.
     *
     * @param evaluation The data evaluation result.
     */
    public void setEvaluation(@Nullable DataEvaluation evaluation) {
        mEvaluation = evaluation;
    }

    /**
     * Get the capability differentiator from the network request. Some capabilities
     * (e.g. {@link NetworkCapabilities#NET_CAPABILITY_ENTERPRISE} could support more than one
     * traffic (e.g. "ENTERPRISE2", "ENTERPRISE3"). This method returns that differentiator.
     *
     * @return The differentiator. 0 if not found.
     */
    public int getCapabilityDifferentiator() {
        if (hasCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)) {
            int[] ids = mNativeNetworkRequest.getEnterpriseIds();
            // No need to verify the range of the id. It has been done in NetworkCapabilities.
            if (ids.length > 0) return ids[0];
        }
        return 0;
    }

    /**
     * Get Os/App id from the network request.
     *
     * @return Os/App id. {@code null} if the request does not have traffic descriptor based network
     * capabilities.
     */
    @Nullable
    public OsAppId getOsAppId() {
        if (!hasAttribute(CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID)) return null;

        // We do not support multiple network capabilities translated to Os/App id at this time.
        // If someday this needs to be done, we need to expand TrafficDescriptor to support
        // connection capabilities instead of using Os/App id to do the work.
        int networkCapability = Arrays.stream(getCapabilities()).boxed()
                .filter(cap -> (CAPABILITY_ATTRIBUTE_MAP.getOrDefault(
                        cap, CAPABILITY_ATTRIBUTE_NONE)
                        & CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID) != 0)
                .findFirst()
                .orElse(-1);

        if (networkCapability == -1) return null;

        int differentiator = getCapabilityDifferentiator();
        if (differentiator > 0) {
            return new OsAppId(OsAppId.ANDROID_OS_ID,
                    DataUtils.networkCapabilityToString(networkCapability), differentiator);
        } else {
            return new OsAppId(OsAppId.ANDROID_OS_ID,
                    DataUtils.networkCapabilityToString(networkCapability));
        }
    }

    /**
     * Convert the telephony request state to string.
     *
     * @param state The request state.
     * @return The request state in string format.
     */
    @NonNull
    private static String requestStateToString(
            @TelephonyNetworkRequest.RequestState int state) {
        return switch (state) {
            case TelephonyNetworkRequest.REQUEST_STATE_UNSATISFIED -> "UNSATISFIED";
            case TelephonyNetworkRequest.REQUEST_STATE_SATISFIED -> "SATISFIED";
            default -> "UNKNOWN(" + state + ")";
        };
    }

    @Override
    public String toString() {
        return "[" + mNativeNetworkRequest + ", mPriority=" + mPriority
                + ", state=" + requestStateToString(mState)
                + ", mAttachedDataNetwork=" + (mAttachedDataNetwork != null
                ? mAttachedDataNetwork.name() : null)
                + ", created time=" + DataUtils.elapsedTimeToString(mCreatedTimeMillis)
                + ", evaluation result=" + mEvaluation + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelephonyNetworkRequest that = (TelephonyNetworkRequest) o;
        // Only compare the native network request.
        return mNativeNetworkRequest.equals(that.mNativeNetworkRequest);
    }

    @Override
    public int hashCode() {
        // Only use the native network request's hash code.
        return mNativeNetworkRequest.hashCode();
    }
}
