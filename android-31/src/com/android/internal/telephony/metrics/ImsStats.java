/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.metrics;

import static android.telephony.ims.RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED;
import static android.telephony.ims.RegistrationManager.REGISTRATION_STATE_REGISTERED;
import static android.telephony.ims.RegistrationManager.REGISTRATION_STATE_REGISTERING;
import static android.telephony.ims.feature.MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_SMS;
import static android.telephony.ims.feature.MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT;
import static android.telephony.ims.feature.MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO;
import static android.telephony.ims.feature.MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_LTE;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_NONE;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_NR;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static android.util.Patterns.EMAIL_ADDRESS;

import android.annotation.Nullable;
import android.os.SystemClock;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.NetworkType;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.RegistrationManager.ImsRegistrationState;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities.MmTelCapability;
import android.telephony.ims.stub.ImsRegistrationImplBase.ImsRegistrationTech;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationTermination;
import com.android.telephony.Rlog;

import java.util.regex.Pattern;

/** Tracks IMS registration metrics for each phone. */
public class ImsStats {
    private static final String TAG = ImsStats.class.getSimpleName();

    /**
     * Minimal duration of the registration state.
     *
     * <p>Registration state (including changes in capable/available features) with duration shorter
     * than this will be ignored as they are considered transient states.
     */
    private static final long MIN_REGISTRATION_DURATION_MILLIS = 1L * SECOND_IN_MILLIS;

    /**
     * Maximum length of the extra message in the termination reason.
     *
     * <p>If the extra message is longer than this length, it will be truncated.
     */
    private static final int MAX_EXTRA_MESSAGE_LENGTH = 128;

    /** Pattern used to match UUIDs in IMS extra messages for filtering. */
    private static final Pattern PATTERN_UUID =
            Pattern.compile(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** Replacement for UUIDs. */
    private static final String REPLACEMENT_UUID = "<UUID_REDACTED>";

    /**
     * Pattern used to match URI (e.g. sip, tel) in IMS extra messages for filtering.
     *
     * <p>NOTE: this simple pattern aims to catch the most common URI schemes. It is not meant to be
     * RFC-complaint.
     */
    private static final Pattern PATTERN_URI =
            Pattern.compile("([a-zA-Z]{2,}:)" + EMAIL_ADDRESS.pattern());

    /** Replacement for URI. */
    private static final String REPLACEMENT_URI = "$1<REDACTED>";

    /**
     * Pattern used to match IPv4 addresses in IMS extra messages for filtering.
     *
     * <p>This is a copy of {@code android.util.Patterns.IP_ADDRESS}, which is deprecated and might
     * be removed in the future.
     */
    private static final Pattern PATTERN_IPV4 =
            Pattern.compile(
                    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                            + "|[1-9][0-9]|[0-9]))");

    /** Replacement for IPv4 addresses. */
    private static final String REPLACEMENT_IPV4 = "<IPV4_REDACTED>";

    /**
     * Pattern used to match IPv6 addresses in IMS extra messages for filtering.
     *
     * <p>NOTE: this pattern aims to catch the most common IPv6 addresses. It is not meant to be
     * RFC-complaint or free of false positives.
     */
    private static final Pattern PATTERN_IPV6 =
            Pattern.compile(
                    // Full address
                    "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
                            // Abbreviated address, e.g. 2001:4860:4860::8888
                            + "|([0-9a-fA-F]{1,4}:){1,6}(:[0-9a-fA-F]{1,4}){1,6}"
                            // Abbreviated network address, e.g. 2607:F8B0::
                            + "|([0-9a-fA-F]{1,4}:){1,7}:"
                            // Abbreviated address, e.g. ::1
                            + "|:(:[0-9a-fA-F]{1,4}){1,7}");

    /** Replacement for IPv6 addresses. */
    private static final String REPLACEMENT_IPV6 = "<IPV6_REDACTED>";

    /**
     * Pattern used to match potential IMEI values in IMS extra messages for filtering.
     *
     * <p>This includes segmented IMEI or IMEI/SV, as well as unsegmented IMEI/SV.
     */
    private static final Pattern PATTERN_IMEI =
            Pattern.compile(
                    "(^|[^0-9])(?:"
                            // IMEI, AABBBBBB-CCCCCC-D format; IMEI/SV, AABBBBBB-CCCCCC-EE format
                            + "[0-9]{8}-[0-9]{6}-[0-9][0-9]?"
                            // IMEI, AA-BBBBBB-CCCCCC-D format; IMEI/SV, AA-BBBBBB-CCCCCC-EE format
                            + "|[0-9]{2}-[0-9]{6}-[0-9]{6}-[0-9][0-9]?"
                            // IMEI/SV, unsegmented
                            + "|[0-9]{16}"
                            + ")($|[^0-9])");

    /** Replacement for IMEI. */
    private static final String REPLACEMENT_IMEI = "$1<IMEI_REDACTED>$2";

    /**
     * Pattern used to match potential unsegmented IMEI/IMSI values in IMS extra messages for
     * filtering.
     */
    private static final Pattern PATTERN_UNSEGMENTED_IMEI_IMSI =
            Pattern.compile("(^|[^0-9])[0-9]{15}($|[^0-9])");

    /** Replacement for unsegmented IMEI/IMSI. */
    private static final String REPLACEMENT_UNSEGMENTED_IMEI_IMSI = "$1<IMEI_IMSI_REDACTED>$2";

    /**
     * Pattern used to match hostnames in IMS extra messages for filtering.
     *
     * <p>This pattern differs from {@link android.util.Patterns.DOMAIN_NAME} in a few ways: it
     * requires the name to have at least 3 segments (shorter names are nearly always public or
     * typos, i.e. missing space after period), does not check the validity of TLDs, and does not
     * support punycodes in TLDs.
     */
    private static final Pattern PATTERN_HOSTNAME =
            Pattern.compile("([0-9a-zA-Z][0-9a-zA-Z_\\-]{0,61}[0-9a-zA-Z]\\.){2,}[a-zA-Z]{2,}");

    /** Replacement for hostnames. */
    private static final String REPLACEMENT_HOSTNAME = "<HOSTNAME_REDACTED>";

    /**
     * Pattern used to match potential IDs in IMS extra messages for filtering.
     *
     * <p>This pattern target numbers that are potential IDs in unknown formats. It should be
     * replaced after all other replacements are done to ensure complete and correct filtering.
     *
     * <p>Specifically, this pattern looks for any number (including hex) that is separated by dots
     * or dashes has at least 6 significant digits, and any unsegmented numbers that has at least 5
     * significant digits.
     */
    private static final Pattern PATTERN_UNKNOWN_ID =
            Pattern.compile(
                    "(^|[^0-9a-fA-F])(([-\\.]?0)*[1-9a-fA-F]([-\\.]?[0-9a-fA-F]){5,}"
                            + "|0*[1-9a-fA-F]([0-9a-fA-F]){4,})");

    /** Replacement for potential IDs. */
    private static final String REPLACEMENT_UNKNOWN_ID = "$1<ID_REDACTED>";

    private final ImsPhone mPhone;
    private final PersistAtomsStorage mStorage;

    @ImsRegistrationState private int mLastRegistrationState = REGISTRATION_STATE_NOT_REGISTERED;

    private long mLastTimestamp;
    @Nullable private ImsRegistrationStats mLastRegistrationStats;

    // Available features are those reported by ImsService to be available for use.
    private MmTelCapabilities mLastAvailableFeatures = new MmTelCapabilities();

    // Capable features (enabled by device/carrier). Theses are available before IMS is registered
    // and not necessarily updated when RAT changes.
    private final MmTelCapabilities mLastWwanCapableFeatures = new MmTelCapabilities();
    private final MmTelCapabilities mLastWlanCapableFeatures = new MmTelCapabilities();

    public ImsStats(ImsPhone phone) {
        mPhone = phone;
        mStorage = PhoneFactory.getMetricsCollector().getAtomsStorage();
    }

    /**
     * Finalizes the durations of the current IMS registration stats segment.
     *
     * <p>This method is also invoked whenever the registration state, feature capability, or
     * feature availability changes.
     */
    public synchronized void conclude() {
        long now = getTimeMillis();

        // Currently not tracking time spent on registering.
        if (mLastRegistrationState == REGISTRATION_STATE_REGISTERED) {
            ImsRegistrationStats stats = copyOf(mLastRegistrationStats);
            long duration = now - mLastTimestamp;

            if (duration < MIN_REGISTRATION_DURATION_MILLIS) {
                logw("conclude: discarding transient stats, duration=%d", duration);
            } else {
                stats.registeredMillis = duration;

                stats.voiceAvailableMillis =
                        mLastAvailableFeatures.isCapable(CAPABILITY_TYPE_VOICE) ? duration : 0;
                stats.videoAvailableMillis =
                        mLastAvailableFeatures.isCapable(CAPABILITY_TYPE_VIDEO) ? duration : 0;
                stats.utAvailableMillis =
                        mLastAvailableFeatures.isCapable(CAPABILITY_TYPE_UT) ? duration : 0;
                stats.smsAvailableMillis =
                        mLastAvailableFeatures.isCapable(CAPABILITY_TYPE_SMS) ? duration : 0;

                MmTelCapabilities lastCapableFeatures =
                        stats.rat == TelephonyManager.NETWORK_TYPE_IWLAN
                                ? mLastWlanCapableFeatures
                                : mLastWwanCapableFeatures;
                stats.voiceCapableMillis =
                        lastCapableFeatures.isCapable(CAPABILITY_TYPE_VOICE) ? duration : 0;
                stats.videoCapableMillis =
                        lastCapableFeatures.isCapable(CAPABILITY_TYPE_VIDEO) ? duration : 0;
                stats.utCapableMillis =
                        lastCapableFeatures.isCapable(CAPABILITY_TYPE_UT) ? duration : 0;
                stats.smsCapableMillis =
                        lastCapableFeatures.isCapable(CAPABILITY_TYPE_SMS) ? duration : 0;

                mStorage.addImsRegistrationStats(stats);
            }
        }

        mLastTimestamp = now;
    }

    /** Updates the stats when registered features changed. */
    public synchronized void onImsCapabilitiesChanged(
            @ImsRegistrationTech int radioTech, MmTelCapabilities capabilities) {
        conclude();

        if (mLastRegistrationStats != null) {
            mLastRegistrationStats.rat = convertRegistrationTechToNetworkType(radioTech);
        }
        mLastAvailableFeatures = capabilities;
    }

    /** Updates the stats when capable features changed. */
    public synchronized void onSetFeatureResponse(
            @MmTelCapability int feature, @ImsRegistrationTech int network, int value) {
        MmTelCapabilities lastCapableFeatures = getLastCapableFeaturesForTech(network);
        if (lastCapableFeatures != null) {
            conclude();
            if (value == ProvisioningManager.PROVISIONING_VALUE_ENABLED) {
                lastCapableFeatures.addCapabilities(feature);
            } else {
                lastCapableFeatures.removeCapabilities(feature);
            }
        }
    }

    /** Updates the stats when IMS registration is progressing. */
    public synchronized void onImsRegistering(@TransportType int imsRadioTech) {
        conclude();

        mLastRegistrationStats = getDefaultImsRegistrationStats();
        mLastRegistrationStats.rat = convertTransportTypeToNetworkType(imsRadioTech);
        mLastRegistrationState = REGISTRATION_STATE_REGISTERING;
    }

    /** Updates the stats when IMS registration succeeds. */
    public synchronized void onImsRegistered(@TransportType int imsRadioTech) {
        conclude();

        // NOTE: mLastRegistrationStats can be null (no registering phase).
        if (mLastRegistrationStats == null) {
            mLastRegistrationStats = getDefaultImsRegistrationStats();
        }
        mLastRegistrationStats.rat = convertTransportTypeToNetworkType(imsRadioTech);
        mLastRegistrationState = REGISTRATION_STATE_REGISTERED;
    }

    /** Updates the stats and generates a termination atom when IMS registration fails/ends. */
    public synchronized void onImsUnregistered(ImsReasonInfo reasonInfo) {
        conclude();

        // Generate end reason atom.
        // NOTE: mLastRegistrationStats can be null (no registering phase).
        ImsRegistrationTermination termination = new ImsRegistrationTermination();
        if (mLastRegistrationStats != null) {
            termination.carrierId = mLastRegistrationStats.carrierId;
            termination.ratAtEnd = getRatAtEnd(mLastRegistrationStats.rat);
        } else {
            termination.carrierId = mPhone.getDefaultPhone().getCarrierId();
            // We cannot tell whether the registration was intended for WWAN or WLAN
            termination.ratAtEnd = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        termination.isMultiSim = SimSlotState.isMultiSim();
        termination.setupFailed = (mLastRegistrationState != REGISTRATION_STATE_REGISTERED);
        termination.reasonCode = reasonInfo.getCode();
        termination.extraCode = reasonInfo.getExtraCode();
        termination.extraMessage = filterExtraMessage(reasonInfo.getExtraMessage());
        termination.count = 1;
        mStorage.addImsRegistrationTermination(termination);

        // Reset state to unregistered.
        mLastRegistrationState = REGISTRATION_STATE_NOT_REGISTERED;
        mLastRegistrationStats = null;
        mLastAvailableFeatures = new MmTelCapabilities();
    }

    @NetworkType
    private int getRatAtEnd(@NetworkType int lastStateRat) {
        return lastStateRat == TelephonyManager.NETWORK_TYPE_IWLAN ? lastStateRat : getWwanPsRat();
    }

    @NetworkType
    private int convertTransportTypeToNetworkType(@TransportType int transportType) {
        switch (transportType) {
            case AccessNetworkConstants.TRANSPORT_TYPE_WWAN:
                return getWwanPsRat();
            case AccessNetworkConstants.TRANSPORT_TYPE_WLAN:
                return TelephonyManager.NETWORK_TYPE_IWLAN;
            default:
                return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    @NetworkType
    private int getWwanPsRat() {
        ServiceState state = mPhone.getServiceStateTracker().getServiceState();
        final NetworkRegistrationInfo wwanRegInfo =
                state.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        return wwanRegInfo != null
                ? wwanRegInfo.getAccessNetworkTechnology()
                : TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    private ImsRegistrationStats getDefaultImsRegistrationStats() {
        Phone phone = mPhone.getDefaultPhone();
        ImsRegistrationStats stats = new ImsRegistrationStats();
        stats.carrierId = phone.getCarrierId();
        stats.simSlotIndex = phone.getPhoneId();
        return stats;
    }

    @Nullable
    private MmTelCapabilities getLastCapableFeaturesForTech(@ImsRegistrationTech int radioTech) {
        switch (radioTech) {
            case REGISTRATION_TECH_NONE:
                return null;
            case REGISTRATION_TECH_IWLAN:
                return mLastWlanCapableFeatures;
            default:
                return mLastWwanCapableFeatures;
        }
    }

    @NetworkType
    private int convertRegistrationTechToNetworkType(@ImsRegistrationTech int radioTech) {
        switch (radioTech) {
            case REGISTRATION_TECH_NONE:
                return TelephonyManager.NETWORK_TYPE_UNKNOWN;
            case REGISTRATION_TECH_LTE:
                return TelephonyManager.NETWORK_TYPE_LTE;
            case REGISTRATION_TECH_IWLAN:
                return TelephonyManager.NETWORK_TYPE_IWLAN;
            case REGISTRATION_TECH_NR:
                return TelephonyManager.NETWORK_TYPE_NR;
            default:
                loge("convertRegistrationTechToNetworkType: unknown radio tech %d", radioTech);
                return getWwanPsRat();
        }
    }

    private static ImsRegistrationStats copyOf(ImsRegistrationStats source) {
        ImsRegistrationStats dest = new ImsRegistrationStats();

        dest.carrierId = source.carrierId;
        dest.simSlotIndex = source.simSlotIndex;
        dest.rat = source.rat;
        dest.registeredMillis = source.registeredMillis;
        dest.voiceCapableMillis = source.voiceCapableMillis;
        dest.voiceAvailableMillis = source.voiceAvailableMillis;
        dest.smsCapableMillis = source.smsCapableMillis;
        dest.smsAvailableMillis = source.smsAvailableMillis;
        dest.videoCapableMillis = source.videoCapableMillis;
        dest.videoAvailableMillis = source.videoAvailableMillis;
        dest.utCapableMillis = source.utCapableMillis;
        dest.utAvailableMillis = source.utAvailableMillis;

        return dest;
    }

    @VisibleForTesting
    protected long getTimeMillis() {
        return SystemClock.elapsedRealtime();
    }

    /** Filters IMS extra messages to ensure length limit and remove IDs. */
    public static String filterExtraMessage(@Nullable String str) {
        if (str == null) {
            return "";
        }

        str = PATTERN_UUID.matcher(str).replaceAll(REPLACEMENT_UUID);
        str = PATTERN_URI.matcher(str).replaceAll(REPLACEMENT_URI);
        str = PATTERN_HOSTNAME.matcher(str).replaceAll(REPLACEMENT_HOSTNAME);
        str = PATTERN_IPV4.matcher(str).replaceAll(REPLACEMENT_IPV4);
        str = PATTERN_IPV6.matcher(str).replaceAll(REPLACEMENT_IPV6);
        str = PATTERN_IMEI.matcher(str).replaceAll(REPLACEMENT_IMEI);
        str = PATTERN_UNSEGMENTED_IMEI_IMSI.matcher(str)
                .replaceAll(REPLACEMENT_UNSEGMENTED_IMEI_IMSI);
        str = PATTERN_UNKNOWN_ID.matcher(str).replaceAll(REPLACEMENT_UNKNOWN_ID);

        return str.length() > MAX_EXTRA_MESSAGE_LENGTH
                ? str.substring(0, MAX_EXTRA_MESSAGE_LENGTH)
                : str;
    }

    private void logw(String format, Object... args) {
        Rlog.w(TAG, "[" + mPhone.getPhoneId() + "] " + String.format(format, args));
    }

    private void loge(String format, Object... args) {
        Rlog.e(TAG, "[" + mPhone.getPhoneId() + "] " + String.format(format, args));
    }
}
