/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony.satellite;

import static android.telephony.ServiceState.STATE_EMERGENCY_ONLY;
import static android.telephony.ServiceState.STATE_IN_SERVICE;

import static java.util.stream.Collectors.joining;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.PersistableBundle;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.DropBoxManagerLoggerBackend;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PersistentLogger;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.AntennaPosition;
import android.telephony.satellite.EarfcnRange;
import android.telephony.satellite.NtnSignalStrength;
import android.telephony.satellite.PointingInfo;
import android.telephony.satellite.SatelliteCapabilities;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteInfo;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteModemEnableRequestAttributes;
import android.telephony.satellite.SatelliteSubscriptionInfo;
import android.telephony.satellite.SystemSelectionSpecifier;
import android.telephony.satellite.stub.NTRadioTechnology;
import android.telephony.satellite.stub.SatelliteModemState;
import android.telephony.satellite.stub.SatelliteResult;
import android.text.TextUtils;

import com.android.internal.R;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.util.TelephonyUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utils class for satellite service <-> framework conversions
 */
public class SatelliteServiceUtils {
    private static final String TAG = "SatelliteServiceUtils";

    /**
     * Converts a carrier roaming NTN (Non-Terrestrial Network) connect type constant
     * from {@link CarrierConfigManager} to string.
     * @param type The carrier roaming NTN connect type constant.
     * @return A string representation of the connect type, or "Unknown(type)" if not recognized.
     */
    public static String carrierRoamingNtnConnectTypeToString(
            @CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_TYPE int type) {
        return switch (type) {
            case CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC -> "AUTOMATIC";
            case CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL -> "MANUAL";
            default -> "Unknown(" + type + ")";
        };
    }

    /**
     * Convert radio technology from service definition to framework definition.
     * @param radioTechnology The NTRadioTechnology from the satellite service.
     * @return The converted NTRadioTechnology for the framework.
     */
    @SatelliteManager.NTRadioTechnology
    public static int fromSatelliteRadioTechnology(int radioTechnology) {
        switch (radioTechnology) {
            case NTRadioTechnology.NB_IOT_NTN:
                return SatelliteManager.NT_RADIO_TECHNOLOGY_NB_IOT_NTN;
            case NTRadioTechnology.NR_NTN:
                return SatelliteManager.NT_RADIO_TECHNOLOGY_NR_NTN;
            case NTRadioTechnology.EMTC_NTN:
                return SatelliteManager.NT_RADIO_TECHNOLOGY_EMTC_NTN;
            case NTRadioTechnology.PROPRIETARY:
                return SatelliteManager.NT_RADIO_TECHNOLOGY_PROPRIETARY;
            default:
                loge("Received invalid radio technology: " + radioTechnology);
                return SatelliteManager.NT_RADIO_TECHNOLOGY_UNKNOWN;
        }
    }

    /**
     * Convert satellite error from service definition to framework definition.
     * @param error The SatelliteError from the satellite service.
     * @return The converted SatelliteResult for the framework.
     */
    @SatelliteManager.SatelliteResult public static int fromSatelliteError(int error) {
        switch (error) {
            case SatelliteResult.SATELLITE_RESULT_SUCCESS:
                return SatelliteManager.SATELLITE_RESULT_SUCCESS;
            case SatelliteResult.SATELLITE_RESULT_ERROR:
                return SatelliteManager.SATELLITE_RESULT_ERROR;
            case SatelliteResult.SATELLITE_RESULT_SERVER_ERROR:
                return SatelliteManager.SATELLITE_RESULT_SERVER_ERROR;
            case SatelliteResult.SATELLITE_RESULT_SERVICE_ERROR:
                return SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR;
            case SatelliteResult.SATELLITE_RESULT_MODEM_ERROR:
                return SatelliteManager.SATELLITE_RESULT_MODEM_ERROR;
            case SatelliteResult.SATELLITE_RESULT_NETWORK_ERROR:
                return SatelliteManager.SATELLITE_RESULT_NETWORK_ERROR;
            case SatelliteResult.SATELLITE_RESULT_INVALID_MODEM_STATE:
                return SatelliteManager.SATELLITE_RESULT_INVALID_MODEM_STATE;
            case SatelliteResult.SATELLITE_RESULT_INVALID_ARGUMENTS:
                return SatelliteManager.SATELLITE_RESULT_INVALID_ARGUMENTS;
            case SatelliteResult.SATELLITE_RESULT_REQUEST_FAILED:
                return SatelliteManager.SATELLITE_RESULT_REQUEST_FAILED;
            case SatelliteResult.SATELLITE_RESULT_RADIO_NOT_AVAILABLE:
                return SatelliteManager.SATELLITE_RESULT_RADIO_NOT_AVAILABLE;
            case SatelliteResult.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED:
                return SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
            case SatelliteResult.SATELLITE_RESULT_NO_RESOURCES:
                return SatelliteManager.SATELLITE_RESULT_NO_RESOURCES;
            case SatelliteResult.SATELLITE_RESULT_SERVICE_NOT_PROVISIONED:
                return SatelliteManager.SATELLITE_RESULT_SERVICE_NOT_PROVISIONED;
            case SatelliteResult.SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS:
                return SatelliteManager.SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS;
            case SatelliteResult.SATELLITE_RESULT_REQUEST_ABORTED:
                return SatelliteManager.SATELLITE_RESULT_REQUEST_ABORTED;
            case SatelliteResult.SATELLITE_RESULT_ACCESS_BARRED:
                return SatelliteManager.SATELLITE_RESULT_ACCESS_BARRED;
            case SatelliteResult.SATELLITE_RESULT_NETWORK_TIMEOUT:
                return SatelliteManager.SATELLITE_RESULT_NETWORK_TIMEOUT;
            case SatelliteResult.SATELLITE_RESULT_NOT_REACHABLE:
                return SatelliteManager.SATELLITE_RESULT_NOT_REACHABLE;
            case SatelliteResult.SATELLITE_RESULT_NOT_AUTHORIZED:
                return SatelliteManager.SATELLITE_RESULT_NOT_AUTHORIZED;
        }
        loge("Received invalid satellite service error: " + error);
        return SatelliteManager.SATELLITE_RESULT_SERVICE_ERROR;
    }

    /**
     * Convert satellite modem state from service definition to framework definition.
     * @param modemState The SatelliteModemState from the satellite service.
     * @return The converted SatelliteModemState for the framework.
     */
    @SatelliteManager.SatelliteModemState
    public static int fromSatelliteModemState(int modemState) {
        switch (modemState) {
            case SatelliteModemState.SATELLITE_MODEM_STATE_IDLE:
                return SatelliteManager.SATELLITE_MODEM_STATE_IDLE;
            case SatelliteModemState.SATELLITE_MODEM_STATE_LISTENING:
                return SatelliteManager.SATELLITE_MODEM_STATE_LISTENING;
            case SatelliteModemState.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING:
                return SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING;
            case SatelliteModemState.SATELLITE_MODEM_STATE_DATAGRAM_RETRYING:
                return SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_RETRYING;
            case SatelliteModemState.SATELLITE_MODEM_STATE_OFF:
                return SatelliteManager.SATELLITE_MODEM_STATE_OFF;
            case SatelliteModemState.SATELLITE_MODEM_STATE_UNAVAILABLE:
                return SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE;
            case SatelliteModemState.SATELLITE_MODEM_STATE_OUT_OF_SERVICE:
                return SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED;
            case SatelliteModemState.SATELLITE_MODEM_STATE_IN_SERVICE:
                return SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED;
            default:
                loge("Received invalid modem state: " + modemState);
                return SatelliteManager.SATELLITE_MODEM_STATE_UNKNOWN;
        }
    }

    /**
     * Convert SatelliteCapabilities from service definition to framework definition.
     * @param capabilities The SatelliteCapabilities from the satellite service.
     * @return The converted SatelliteCapabilities for the framework.
     */
    @Nullable public static SatelliteCapabilities fromSatelliteCapabilities(
            @Nullable android.telephony.satellite.stub.SatelliteCapabilities capabilities) {
        if (capabilities == null) return null;
        int[] radioTechnologies = capabilities.supportedRadioTechnologies == null
                ? new int[0] : capabilities.supportedRadioTechnologies;

        Map<Integer, AntennaPosition> antennaPositionMap = new HashMap<>();
        int[] antennaPositionKeys = capabilities.antennaPositionKeys;
        AntennaPosition[] antennaPositionValues = capabilities.antennaPositionValues;
        if (antennaPositionKeys != null && antennaPositionValues != null &&
                antennaPositionKeys.length == antennaPositionValues.length) {
            for(int i = 0; i < antennaPositionKeys.length; i++) {
                antennaPositionMap.put(antennaPositionKeys[i], antennaPositionValues[i]);
            }
        }

        return new SatelliteCapabilities(
                Arrays.stream(radioTechnologies)
                        .map(SatelliteServiceUtils::fromSatelliteRadioTechnology)
                        .boxed().collect(Collectors.toSet()),
                capabilities.isPointingRequired, capabilities.maxBytesPerOutgoingDatagram,
                antennaPositionMap);
    }

    /**
     * Convert PointingInfo from service definition to framework definition.
     * @param pointingInfo The PointingInfo from the satellite service.
     * @return The converted PointingInfo for the framework.
     */
    @Nullable public static PointingInfo fromPointingInfo(
            android.telephony.satellite.stub.PointingInfo pointingInfo) {
        if (pointingInfo == null) return null;
        return new PointingInfo(pointingInfo.satelliteAzimuth, pointingInfo.satelliteElevation);
    }

    /**
     * Convert SatelliteDatagram from service definition to framework definition.
     * @param datagram The SatelliteDatagram from the satellite service.
     * @return The converted SatelliteDatagram for the framework.
     */
    @Nullable public static SatelliteDatagram fromSatelliteDatagram(
            android.telephony.satellite.stub.SatelliteDatagram datagram) {
        if (datagram == null) return null;
        byte[] data = datagram.data == null ? new byte[0] : datagram.data;
        return new SatelliteDatagram(data);
    }

    /**
     * Convert non-terrestrial signal strength from service definition to framework definition.
     * @param ntnSignalStrength The non-terrestrial signal strength from the satellite service.
     * @return The converted non-terrestrial signal strength for the framework.
     */
    @Nullable public static NtnSignalStrength fromNtnSignalStrength(
            android.telephony.satellite.stub.NtnSignalStrength ntnSignalStrength) {
        return new NtnSignalStrength(ntnSignalStrength.signalStrengthLevel);
    }

    /**
     * Convert SatelliteDatagram from framework definition to service definition.
     * @param datagram The SatelliteDatagram from the framework.
     * @return The converted SatelliteDatagram for the satellite service.
     */
    @Nullable public static android.telephony.satellite.stub.SatelliteDatagram toSatelliteDatagram(
            @Nullable SatelliteDatagram datagram) {
        android.telephony.satellite.stub.SatelliteDatagram converted =
                new android.telephony.satellite.stub.SatelliteDatagram();
        converted.data = datagram.getSatelliteDatagram();
        return converted;
    }

    /**
     * Convert SatelliteSubscriptionInfo from framework definition to service definition.
     * @param info The SatelliteSubscriptionInfo from the framework.
     * @return The converted SatelliteSubscriptionInfo for the satellite service.
     */
    @NonNull public static android.telephony.satellite.stub
            .SatelliteSubscriptionInfo toSatelliteSubscriptionInfo(
            @NonNull SatelliteSubscriptionInfo info
    ) {
        android.telephony.satellite.stub.SatelliteSubscriptionInfo converted =
                new android.telephony.satellite.stub.SatelliteSubscriptionInfo();
        converted.iccId = info.getIccId();
        converted.niddApn = info.getNiddApn();
        return converted;
    }

    /**
     * Convert SatelliteModemEnableRequestAttributes from framework definition to service definition
     * @param attributes The SatelliteModemEnableRequestAttributes from the framework.
     * @return The converted SatelliteModemEnableRequestAttributes for the satellite service.
     */
    @NonNull public static android.telephony.satellite.stub
            .SatelliteModemEnableRequestAttributes toSatelliteModemEnableRequestAttributes(
            @NonNull SatelliteModemEnableRequestAttributes attributes
    ) {
        android.telephony.satellite.stub.SatelliteModemEnableRequestAttributes converted =
                new android.telephony.satellite.stub.SatelliteModemEnableRequestAttributes();
        converted.isEnabled = attributes.isEnabled();
        converted.isDemoMode = attributes.isForDemoMode();
        converted.isEmergencyMode = attributes.isForEmergencyMode();
        converted.satelliteSubscriptionInfo = toSatelliteSubscriptionInfo(
                attributes.getSatelliteSubscriptionInfo());
        return converted;
    }

    /**
     * Get the {@link SatelliteManager.SatelliteResult} from the provided result.
     *
     * @param ar AsyncResult used to determine the error code.
     * @param caller The satellite request.
     *
     * @return The {@link SatelliteManager.SatelliteResult} error code from the request.
     */
    @SatelliteManager.SatelliteResult public static int getSatelliteError(@NonNull AsyncResult ar,
            @NonNull String caller) {
        int errorCode;
        if (ar.exception == null) {
            errorCode = SatelliteManager.SATELLITE_RESULT_SUCCESS;
        } else {
            errorCode = SatelliteManager.SATELLITE_RESULT_ERROR;
            if (ar.exception instanceof SatelliteManager.SatelliteException) {
                errorCode = ((SatelliteManager.SatelliteException) ar.exception).getErrorCode();
                loge(caller + " SatelliteException: " + ar.exception);
            } else {
                loge(caller + " unknown exception: " + ar.exception);
            }
        }
        logd(caller + " error: " + errorCode);
        return errorCode;
    }

    /**
     * Get valid subscription id for satellite communication.
     *
     * @param subId The subscription id.
     * @return input subId if the subscription is active else return default subscription id.
     */
    public static int getValidSatelliteSubId(int subId, @NonNull Context context) {
        final long identity = Binder.clearCallingIdentity();
        try {
            boolean isActive = SubscriptionManagerService.getInstance().isActiveSubId(subId,
                    context.getOpPackageName(), context.getAttributionTag());

            if (isActive) {
                return subId;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        logd("getValidSatelliteSubId: use DEFAULT_SUBSCRIPTION_ID for subId=" + subId);
        return SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
    }

    /**
     * Get the subscription ID which supports OEM based NTN satellite service.
     *
     * @return ID of the subscription that supports OEM-based satellite if any,
     * return {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID} otherwise.
     */
    public static int getNtnOnlySubscriptionId(@NonNull Context context) {
        List<SubscriptionInfo> infoList =
                SubscriptionManagerService.getInstance().getAllSubInfoList(
                        context.getOpPackageName(), null);

        int subId = infoList.stream()
                .filter(info -> info.isOnlyNonTerrestrialNetwork())
                .mapToInt(SubscriptionInfo::getSubscriptionId)
                .findFirst()
                .orElse(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        logd("getNtnOnlySubscriptionId: subId=" + subId);
        return subId;
    }

    /**
     * Check if the subscription ID is a NTN only subscription ID.
     *
     * @return {@code true} if the subscription ID is a NTN only subscription ID,
     * {@code false} otherwise.
    */
    public static boolean isNtnOnlySubscriptionId(int subId) {
        SubscriptionManagerService subscriptionManagerService =
            SubscriptionManagerService.getInstance();
        if (subscriptionManagerService == null) {
            logd("isNtnOnlySubscriptionId: subscriptionManagerService is null");
            return false;
        }

        SubscriptionInfo subInfo = subscriptionManagerService.getSubscriptionInfo(subId);
        if (subInfo == null) {
            logd("isNtnOnlySubscriptionId: subInfo is null for subId=" + subId);
            return false;
        }

        return subInfo.isOnlyNonTerrestrialNetwork();
    }

    /**
     * Expected format of the input dictionary bundle is:
     * <ul>
     *     <li>Key: PLMN string.</li>
     *     <li>Value: A string with format "service_1,service_2,..."</li>
     * </ul>
     * @return The map of supported services with key: PLMN, value: set of services supported by
     * the PLMN.
     */
    @NonNull
    @NetworkRegistrationInfo.ServiceType
    public static Map<String, Set<Integer>> parseSupportedSatelliteServices(
            PersistableBundle supportedServicesBundle) {
        Map<String, Set<Integer>> supportedServicesMap = new HashMap<>();
        if (supportedServicesBundle == null || supportedServicesBundle.isEmpty()) {
            return supportedServicesMap;
        }

        for (String plmn : supportedServicesBundle.keySet()) {
            if (TelephonyUtils.isValidPlmn(plmn)) {
                Set<Integer> supportedServicesSet = new HashSet<>();
                for (int serviceType : supportedServicesBundle.getIntArray(plmn)) {
                    if (TelephonyUtils.isValidService(serviceType)) {
                        supportedServicesSet.add(serviceType);
                    } else {
                        loge("parseSupportedSatelliteServices: invalid service type=" + serviceType
                                + " for plmn=" + plmn);
                    }
                }
                logd("parseSupportedSatelliteServices: plmn=" + plmn + ", supportedServicesSet="
                        + supportedServicesSet.stream().map(String::valueOf).collect(
                        joining(",")));
                supportedServicesMap.put(plmn, supportedServicesSet);
            } else {
                loge("parseSupportedSatelliteServices: invalid plmn=" + plmn);
            }
        }
        return supportedServicesMap;
    }

    /**
     * Merge two string lists into one such that the result list does not have any duplicate items.
     */
    @NonNull
    public static List<String> mergeStrLists(List<String> strList1, List<String> strList2) {
        Set<String> mergedStrSet = new HashSet<>();
        if (strList1 != null) {
            mergedStrSet.addAll(strList1);
        }

        if (strList2 != null) {
            mergedStrSet.addAll(strList2);
        }

        return mergedStrSet.stream().toList();
    }

    /**
     * Merge three string lists into one such that the result list does not have any duplicate
     * items.
     */
    @NonNull
    public static List<String> mergeStrLists(List<String> strList1, List<String> strList2,
            List<String> strList3) {
        Set<String> mergedStrSet = new HashSet<>();
        mergedStrSet.addAll(strList1);
        mergedStrSet.addAll(strList2);
        mergedStrSet.addAll(strList3);
        return mergedStrSet.stream().toList();
    }

    /**
     * Check if the datagramType is the sos message (DATAGRAM_TYPE_SOS_MESSAGE,
     * DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP,
     * DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED) or not
     */
    public static boolean isSosMessage(int datagramType) {
        return datagramType == SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE
                || datagramType == SatelliteManager.DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP
                || datagramType == SatelliteManager.DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED;
    }

    /**
     * Check if the datagramType is the last sos message
     * (DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP or
     * DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED) or not
     */
    public static boolean isLastSosMessage(int datagramType) {
        return datagramType == SatelliteManager.DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP
                || datagramType == SatelliteManager.DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED;
    }

    /**
     * Return phone associated with phoneId 0.
     *
     * @return phone associated with phoneId 0 or {@code null} if it doesn't exist.
     */
    public static @Nullable Phone getPhone() {
        return PhoneFactory.getPhone(0);
    }

    /**
     * Return phone associated with subscription ID.
     *
     * @return phone associated with {@code subId} or {@code null} if it doesn't exist.
     */
    public static @Nullable Phone getPhone(int subId) {
        return PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
    }

    /** Return {@code true} if device has cellular coverage, else return {@code false}. */
    public static boolean isCellularAvailable() {
        for (Phone phone : PhoneFactory.getPhones()) {
            ServiceState serviceState = phone.getServiceState();
            if (serviceState != null) {
                int state = serviceState.getState();
                if ((state == STATE_IN_SERVICE || state == STATE_EMERGENCY_ONLY
                        || serviceState.isEmergencyOnly())
                        && !isSatellitePlmn(phone.getSubId(), serviceState)) {
                    logd("isCellularAvailable true");
                    return true;
                }
            }
        }
        logd("isCellularAvailable false");
        return false;
    }

    /** Check whether device is connected to satellite PLMN */
    public static boolean isSatellitePlmn(int subId, @NonNull ServiceState serviceState) {
        List<String> satellitePlmnList =
                SatelliteController.getInstance().getSatellitePlmnsForCarrier(subId);
        if (satellitePlmnList.isEmpty()) {
            logd("isSatellitePlmn: satellitePlmnList is empty");
            return false;
        }

        for (NetworkRegistrationInfo nri :
                serviceState.getNetworkRegistrationInfoListForTransportType(
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN)) {
            String registeredPlmn = nri.getRegisteredPlmn();
            if (TextUtils.isEmpty(registeredPlmn)) {
                logd("isSatellitePlmn: registeredPlmn is empty");
                continue;
            }

            String mccmnc = getMccMnc(nri);
            for (String satellitePlmn : satellitePlmnList) {
                if (TextUtils.equals(satellitePlmn, registeredPlmn)
                        || TextUtils.equals(satellitePlmn, mccmnc)) {
                    logd("isSatellitePlmn: return true, satellitePlmn:" + satellitePlmn
                            + " registeredPlmn:" + registeredPlmn + " mccmnc:" + mccmnc);
                    return true;
                }
            }
        }

        logd("isSatellitePlmn: return false");
        return false;
    }

    /** Get mccmnc string from NetworkRegistrationInfo. */
    @Nullable
    public static String getMccMnc(@NonNull NetworkRegistrationInfo nri) {
        CellIdentity cellIdentity = nri.getCellIdentity();
        if (cellIdentity == null) {
            logd("getMccMnc: cellIdentity is null");
            return null;
        }

        String mcc = cellIdentity.getMccString();
        String mnc = cellIdentity.getMncString();
        if (mcc == null || mnc == null) {
            logd("getMccMnc: mcc or mnc is null. mcc=" + mcc + " mnc=" + mnc);
            return null;
        }

        return mcc + mnc;
    }

    @NonNull
    private static android.telephony.satellite.stub
            .SystemSelectionSpecifier convertSystemSelectionSpecifierToHALFormat(
            @NonNull SystemSelectionSpecifier systemSelectionSpecifier) {
        android.telephony.satellite.stub.SystemSelectionSpecifier convertedSpecifier =
                new android.telephony.satellite.stub.SystemSelectionSpecifier();

        convertedSpecifier.mMccMnc = systemSelectionSpecifier.getMccMnc();
        convertedSpecifier.mBands = systemSelectionSpecifier.getBands();
        convertedSpecifier.mEarfcs = systemSelectionSpecifier.getEarfcns();
        SatelliteInfo[] satelliteInfos = systemSelectionSpecifier.getSatelliteInfos()
                .toArray(new SatelliteInfo[0]);
        android.telephony.satellite.stub.SatelliteInfo[] halSatelliteInfos =
                new android.telephony.satellite.stub.SatelliteInfo[satelliteInfos.length];
        for (int i = 0; i < satelliteInfos.length; i++) {
            halSatelliteInfos[i] = new android.telephony.satellite.stub.SatelliteInfo();

            halSatelliteInfos[i].id = new android.telephony.satellite.stub.UUID();
            halSatelliteInfos[i].id.mostSigBits =
                    satelliteInfos[i].getSatelliteId().getMostSignificantBits();
            halSatelliteInfos[i].id.leastSigBits =
                    satelliteInfos[i].getSatelliteId().getLeastSignificantBits();

            halSatelliteInfos[i].position =
                    new android.telephony.satellite.stub.SatellitePosition();
            halSatelliteInfos[i].position.longitudeDegree =
                    satelliteInfos[i].getSatellitePosition().getLongitudeDegrees();
            halSatelliteInfos[i].position.altitudeKm =
                    satelliteInfos[i].getSatellitePosition().getAltitudeKm();

            halSatelliteInfos[i].bands = satelliteInfos[i].getBands().stream().mapToInt(
                    Integer::intValue).toArray();

            List<EarfcnRange> earfcnRangeList = satelliteInfos[i].getEarfcnRanges();
            halSatelliteInfos[i].earfcnRanges =
                    new android.telephony.satellite.stub.EarfcnRange[earfcnRangeList.size()];
            for (int j = 0; j < earfcnRangeList.size(); j++) {
                halSatelliteInfos[i].earfcnRanges[j] =
                        new android.telephony.satellite.stub.EarfcnRange();
                halSatelliteInfos[i].earfcnRanges[j].startEarfcn = earfcnRangeList.get(
                        j).getStartEarfcn();
                halSatelliteInfos[i].earfcnRanges[j].endEarfcn = earfcnRangeList.get(
                        j).getEndEarfcn();
            }
        }
        convertedSpecifier.satelliteInfos = halSatelliteInfos;
        convertedSpecifier.tagIds = systemSelectionSpecifier.getTagIds();
        return convertedSpecifier;
    }

    /**
     * Convert SystemSelectionSpecifier from framework definition to service definition
     * @param systemSelectionSpecifier The SystemSelectionSpecifier from the framework.
     * @return The converted SystemSelectionSpecifier for the satellite service.
     */
    @NonNull
    public static List<android.telephony.satellite.stub
            .SystemSelectionSpecifier> toSystemSelectionSpecifier(
            @NonNull List<SystemSelectionSpecifier> systemSelectionSpecifier) {
        return systemSelectionSpecifier.stream().map(
                SatelliteServiceUtils::convertSystemSelectionSpecifierToHALFormat).collect(
                Collectors.toList());
    }

    /**
     * Expected format of the input dictionary bundle is:
     * <ul>
     *     <li>Key: Regional satellite config Id string.</li>
     *     <li>Value: Integer arrays of earfcns in the corresponding regions."</li>
     * </ul>
     * @return The map of earfcns with key: regional satellite config Id,
     * value: set of earfcns in the corresponding regions.
     */
    @NonNull
    public static Map<String, Set<Integer>> parseRegionalSatelliteEarfcns(
            @Nullable PersistableBundle earfcnsBundle) {
        Map<String, Set<Integer>> earfcnsMap = new HashMap<>();
        if (earfcnsBundle == null || earfcnsBundle.isEmpty()) {
            logd("parseRegionalSatelliteEarfcns: earfcnsBundle is null or empty");
            return earfcnsMap;
        }

        for (String configId : earfcnsBundle.keySet()) {
            Set<Integer> earfcnsSet = new HashSet<>();
            for (int earfcn : earfcnsBundle.getIntArray(configId)) {
                earfcnsSet.add(earfcn);
            }
            logd("parseRegionalSatelliteEarfcns: configId = " + configId + ", earfcns ="
                    + earfcnsSet.stream().map(String::valueOf).collect(joining(",")));
            earfcnsMap.put(configId, earfcnsSet);
        }
        return earfcnsMap;
    }

    /**
     * Returns a persistent logger to persist important log because logcat logs may not be
     * retained long enough.
     *
     * @return a PersistentLogger, return {@code null} if it is not supported or encounters
     * exception.
     */
    @Nullable
    public static PersistentLogger getPersistentLogger(@NonNull Context context) {
        try {
            if (context.getResources().getBoolean(
                    R.bool.config_dropboxmanager_persistent_logging_enabled)) {
                return new PersistentLogger(DropBoxManagerLoggerBackend.getInstance(context));
            }
        } catch (RuntimeException ex) {
            loge("getPersistentLogger: RuntimeException ex=" + ex);
        }
        return null;
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }

    private static void logv(@NonNull String log) {
        Rlog.v(TAG, log);
    }
}
