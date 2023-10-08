/**
 * Copyright (c) 2008, The Android Open Source Project
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

package android.net.wifi;

import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.IProvisioningCallback;

import android.net.DhcpInfo;
import android.net.DhcpOption;
import android.net.Network;
import android.net.wifi.CoexUnsafeChannel;
import android.net.wifi.IActionListener;
import android.net.wifi.IBooleanListener;
import android.net.wifi.ICoexCallback;
import android.net.wifi.IDppCallback;
import android.net.wifi.IIntegerListener;
import android.net.wifi.IInterfaceCreationInfoCallback;
import android.net.wifi.ILastCallerListener;
import android.net.wifi.IListListener;
import android.net.wifi.ILocalOnlyHotspotCallback;
import android.net.wifi.ILocalOnlyConnectionStatusListener;
import android.net.wifi.INetworkRequestMatchCallback;
import android.net.wifi.IOnWifiActivityEnergyInfoListener;
import android.net.wifi.IOnWifiDriverCountryCodeChangedListener;
import android.net.wifi.IWifiNetworkStateChangedListener;
import android.net.wifi.IOnWifiUsabilityStatsListener;
import android.net.wifi.IPnoScanResultsCallback;
import android.net.wifi.IScanResultsCallback;
import android.net.wifi.ISoftApCallback;
import android.net.wifi.IStringListener;
import android.net.wifi.ISubsystemRestartCallback;
import android.net.wifi.ISuggestionConnectionStatusListener;
import android.net.wifi.ISuggestionUserApprovalStatusListener;
import android.net.wifi.ITrafficStateCallback;
import android.net.wifi.IWifiBandsListener;
import android.net.wifi.IWifiConnectedNetworkScorer;
import android.net.wifi.IWifiLowLatencyLockListener;
import android.net.wifi.IWifiNetworkSelectionConfigListener;
import android.net.wifi.IWifiVerboseLoggingStatusChangedListener;
import android.net.wifi.QosPolicyParams;
import android.net.wifi.ScanResult;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiAvailableChannel;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSelectionConfig;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.WifiSsid;

import android.os.Bundle;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.os.WorkSource;

import com.android.modules.utils.ParceledListSlice;

/**
 * Interface that allows controlling and querying Wi-Fi connectivity.
 *
 * {@hide}
 */
interface IWifiManager
{
    long getSupportedFeatures();

    oneway void getWifiActivityEnergyInfoAsync(in IOnWifiActivityEnergyInfoListener listener);

    void setNetworkSelectionConfig(in WifiNetworkSelectionConfig nsConfig);

    void getNetworkSelectionConfig(in IWifiNetworkSelectionConfigListener listener);

    void setThirdPartyAppEnablingWifiConfirmationDialogEnabled(boolean enable);

    boolean isThirdPartyAppEnablingWifiConfirmationDialogEnabled();

    void setScreenOnScanSchedule(in int[] scanScheduleSeconds, in int[] scanType);

    void setOneShotScreenOnConnectivityScanDelayMillis(int delayMs);

    ParceledListSlice getConfiguredNetworks(String packageName, String featureId, boolean callerNetworksOnly);

    ParceledListSlice getPrivilegedConfiguredNetworks(String packageName, String featureId, in Bundle extras);

    WifiConfiguration getPrivilegedConnectedNetwork(String packageName, String featureId, in Bundle extras);

    Map getAllMatchingFqdnsForScanResults(in List<ScanResult> scanResult);

    void setSsidsAllowlist(String packageName, in List<WifiSsid> ssids);

    List getSsidsAllowlist(String packageName);

    Map getMatchingOsuProviders(in List<ScanResult> scanResult);

    Map getMatchingPasspointConfigsForOsuProviders(in List<OsuProvider> osuProviders);

    int addOrUpdateNetwork(in WifiConfiguration config, String packageName, in Bundle extras);

    WifiManager.AddNetworkResult addOrUpdateNetworkPrivileged(in WifiConfiguration config, String packageName);

    boolean addOrUpdatePasspointConfiguration(in PasspointConfiguration config, String packageName);

    boolean removePasspointConfiguration(in String fqdn, String packageName);

    List<PasspointConfiguration> getPasspointConfigurations(in String packageName);

    List<WifiConfiguration> getWifiConfigsForPasspointProfiles(in List<String> fqdnList);

    void queryPasspointIcon(long bssid, String fileName);

    int matchProviderWithCurrentNetwork(String fqdn);

    boolean removeNetwork(int netId, String packageName);

    boolean removeNonCallerConfiguredNetworks(String packageName);

    boolean enableNetwork(int netId, boolean disableOthers, String packageName);

    boolean disableNetwork(int netId, String packageName);

    void allowAutojoinGlobal(boolean choice, String packageName, in Bundle extras);

    void queryAutojoinGlobal(in IBooleanListener listener);

    void allowAutojoin(int netId, boolean choice);

    void allowAutojoinPasspoint(String fqdn, boolean enableAutoJoin);

    void setMacRandomizationSettingPasspointEnabled(String fqdn, boolean enable);

    void setPasspointMeteredOverride(String fqdn, int meteredOverride);

    boolean startScan(String packageName, String featureId);

    List<ScanResult> getScanResults(String callingPackage, String callingFeatureId);

    void getChannelData(in IListListener listener, String packageName, in Bundle extras);

    boolean disconnect(String packageName);

    boolean reconnect(String packageName);

    boolean reassociate(String packageName);

    WifiInfo getConnectionInfo(String callingPackage, String callingFeatureId);

    boolean setWifiEnabled(String packageName, boolean enable);

    int getWifiEnabledState();

    void registerDriverCountryCodeChangedListener(
            in IOnWifiDriverCountryCodeChangedListener listener, String packageName,
            String featureId);

    void unregisterDriverCountryCodeChangedListener(
            in IOnWifiDriverCountryCodeChangedListener listener);

    void addWifiNetworkStateChangedListener(in IWifiNetworkStateChangedListener listener);

    void removeWifiNetworkStateChangedListener(in IWifiNetworkStateChangedListener listener);

    String getCountryCode(in String packageName, in String featureId);

    void setOverrideCountryCode(String country);

    void clearOverrideCountryCode();

    void setDefaultCountryCode(String country);

    boolean is24GHzBandSupported();

    boolean is5GHzBandSupported();

    boolean is6GHzBandSupported();

    boolean is60GHzBandSupported();

    boolean isWifiStandardSupported(int standard);

    DhcpInfo getDhcpInfo(String packageName);

    void setScanAlwaysAvailable(boolean isAvailable, String packageName);

    boolean isScanAlwaysAvailable();

    boolean acquireWifiLock(IBinder lock, int lockType, String tag, in WorkSource ws);

    void updateWifiLockWorkSource(IBinder lock, in WorkSource ws);

    boolean releaseWifiLock(IBinder lock);

    void initializeMulticastFiltering();

    boolean isMulticastEnabled();

    void acquireMulticastLock(IBinder binder, String tag);

    void releaseMulticastLock(String tag);

    void updateInterfaceIpState(String ifaceName, int mode);

    boolean isDefaultCoexAlgorithmEnabled();

    void setCoexUnsafeChannels(in List<CoexUnsafeChannel> unsafeChannels, int mandatoryRestrictions);

    void registerCoexCallback(in ICoexCallback callback);

    void unregisterCoexCallback(in ICoexCallback callback);

    boolean startSoftAp(in WifiConfiguration wifiConfig, String packageName);

    boolean startTetheredHotspot(in SoftApConfiguration softApConfig, String packageName);

    boolean stopSoftAp();

    boolean validateSoftApConfiguration(in SoftApConfiguration config);

    int startLocalOnlyHotspot(in ILocalOnlyHotspotCallback callback, String packageName,
                              String featureId, in SoftApConfiguration customConfig, in Bundle extras);

    void stopLocalOnlyHotspot();

    void registerLocalOnlyHotspotSoftApCallback(in ISoftApCallback callback, in Bundle extras);

    void unregisterLocalOnlyHotspotSoftApCallback(in ISoftApCallback callback, in Bundle extras);

    void startWatchLocalOnlyHotspot(in ILocalOnlyHotspotCallback callback);

    void stopWatchLocalOnlyHotspot();

    @UnsupportedAppUsage
    int getWifiApEnabledState();

    @UnsupportedAppUsage
    WifiConfiguration getWifiApConfiguration();

    SoftApConfiguration getSoftApConfiguration();

    void queryLastConfiguredTetheredApPassphraseSinceBoot(IStringListener listener);

    boolean setWifiApConfiguration(in WifiConfiguration wifiConfig, String packageName);

    boolean setSoftApConfiguration(in SoftApConfiguration softApConfig, String packageName);

    void notifyUserOfApBandConversion(String packageName);

    void enableTdls(String remoteIPAddress, boolean enable);

    void enableTdlsWithRemoteIpAddress(String remoteIPAddress, boolean enable, in IBooleanListener listener);

    void enableTdlsWithMacAddress(String remoteMacAddress, boolean enable);

    void enableTdlsWithRemoteMacAddress(String remoteMacAddress, boolean enable, in IBooleanListener listener);

    void isTdlsOperationCurrentlyAvailable(in IBooleanListener listener);

    void getMaxSupportedConcurrentTdlsSessions(in IIntegerListener callback);

    void getNumberOfEnabledTdlsSessions(in IIntegerListener callback);

    String getCurrentNetworkWpsNfcConfigurationToken();

    void enableVerboseLogging(int verbose);

    int getVerboseLoggingLevel();

    void disableEphemeralNetwork(String SSID, String packageName);

    void factoryReset(String packageName);

    @UnsupportedAppUsage(maxTargetSdk = 30, trackingBug = 170729553)
    Network getCurrentNetwork();

    byte[] retrieveBackupData();

    void restoreBackupData(in byte[] data);

    byte[] retrieveSoftApBackupData();

    SoftApConfiguration restoreSoftApBackupData(in byte[] data);

    void restoreSupplicantBackupData(in byte[] supplicantData, in byte[] ipConfigData);

    void startSubscriptionProvisioning(in OsuProvider provider, in IProvisioningCallback callback);

    void registerSoftApCallback(in ISoftApCallback callback);

    void unregisterSoftApCallback(in ISoftApCallback callback);

    void addWifiVerboseLoggingStatusChangedListener(in IWifiVerboseLoggingStatusChangedListener listener);

    void removeWifiVerboseLoggingStatusChangedListener(in IWifiVerboseLoggingStatusChangedListener listener);

    void addOnWifiUsabilityStatsListener(in IOnWifiUsabilityStatsListener listener);

    void removeOnWifiUsabilityStatsListener(in IOnWifiUsabilityStatsListener listener);

    void registerTrafficStateCallback(in ITrafficStateCallback callback);

    void unregisterTrafficStateCallback(in ITrafficStateCallback callback);

    void registerNetworkRequestMatchCallback(in INetworkRequestMatchCallback callback);

    void unregisterNetworkRequestMatchCallback(in INetworkRequestMatchCallback callback);

    int addNetworkSuggestions(in List<WifiNetworkSuggestion> networkSuggestions, in String packageName,
        in String featureId);

    int removeNetworkSuggestions(in List<WifiNetworkSuggestion> networkSuggestions, in String packageName, int action);

    List<WifiNetworkSuggestion> getNetworkSuggestions(in String packageName);

    String[] getFactoryMacAddresses();

    void setDeviceMobilityState(int state);

    void startDppAsConfiguratorInitiator(in IBinder binder, in String packageName,
        in String enrolleeUri, int selectedNetworkId, int netRole, in IDppCallback callback);

    void startDppAsEnrolleeInitiator(in IBinder binder, in String configuratorUri,
        in IDppCallback callback);

    void startDppAsEnrolleeResponder(in IBinder binder, in String deviceInfo, int curve,
        in IDppCallback callback);

    void stopDppSession();

    void updateWifiUsabilityScore(int seqNum, int score, int predictionHorizonSec);

    oneway void connect(in WifiConfiguration config, int netId, in IActionListener listener, in String packageName);

    oneway void save(in WifiConfiguration config, in IActionListener listener, in String packageName);

    oneway void forget(int netId, in IActionListener listener);

    void registerScanResultsCallback(in IScanResultsCallback callback);

    void unregisterScanResultsCallback(in IScanResultsCallback callback);

    void registerSuggestionConnectionStatusListener(in ISuggestionConnectionStatusListener listener, String packageName, String featureId);

    void unregisterSuggestionConnectionStatusListener(in ISuggestionConnectionStatusListener listener, String packageName);

    void addLocalOnlyConnectionStatusListener(in ILocalOnlyConnectionStatusListener listener, String packageName, String featureId);

    void removeLocalOnlyConnectionStatusListener(in ILocalOnlyConnectionStatusListener listener, String packageName);

    int calculateSignalLevel(int rssi);

    List<WifiConfiguration> getWifiConfigForMatchedNetworkSuggestionsSharedWithUser(in List<ScanResult> scanResults);

    boolean setWifiConnectedNetworkScorer(in IBinder binder, in IWifiConnectedNetworkScorer scorer);

    void clearWifiConnectedNetworkScorer();

    void setExternalPnoScanRequest(in IBinder binder, in IPnoScanResultsCallback callback, in List<WifiSsid> ssids, in int[] frequencies, String packageName, String featureId);

    void clearExternalPnoScanRequest();

    void getLastCallerInfoForApi(int api, in ILastCallerListener listener);

    /**
     * Return the Map of {@link WifiNetworkSuggestion} and the list of <ScanResult>
     */
    Map getMatchingScanResults(in List<WifiNetworkSuggestion> networkSuggestions, in List<ScanResult> scanResults, String callingPackage, String callingFeatureId);

    void setScanThrottleEnabled(boolean enable);

    boolean isScanThrottleEnabled();

    Map getAllMatchingPasspointProfilesForScanResults(in List<ScanResult> scanResult);

    void setAutoWakeupEnabled(boolean enable);

    boolean isAutoWakeupEnabled();

    void startRestrictingAutoJoinToSubscriptionId(int subId);

    void stopRestrictingAutoJoinToSubscriptionId();

    void setCarrierNetworkOffloadEnabled(int subscriptionId, boolean merged, boolean enabled);

    boolean isCarrierNetworkOffloadEnabled(int subscriptionId, boolean merged);

    void registerSubsystemRestartCallback(in ISubsystemRestartCallback callback);

    void unregisterSubsystemRestartCallback(in ISubsystemRestartCallback callback);

    void restartWifiSubsystem();

    void addSuggestionUserApprovalStatusListener(in ISuggestionUserApprovalStatusListener listener, String packageName);

    void removeSuggestionUserApprovalStatusListener(in ISuggestionUserApprovalStatusListener listener, String packageName);

    void setEmergencyScanRequestInProgress(boolean inProgress);

    void removeAppState(int targetAppUid, String targetApppackageName);

    boolean setWifiScoringEnabled(boolean enabled);

    void flushPasspointAnqpCache(String packageName);

    List<WifiAvailableChannel> getUsableChannels(int band, int mode, int filter, String packageName, in Bundle extras);

    boolean isWifiPasspointEnabled();

    void setWifiPasspointEnabled(boolean enabled);

    int getStaConcurrencyForMultiInternetMode();

    boolean setStaConcurrencyForMultiInternetMode(int mode);

    void notifyMinimumRequiredWifiSecurityLevelChanged(int level);

    void notifyWifiSsidPolicyChanged(int policyType, in List<WifiSsid> ssids);

    String[] getOemPrivilegedWifiAdminPackages();

    void replyToP2pInvitationReceivedDialog(int dialogId, boolean accepted, String optionalPin);

    void replyToSimpleDialog(int dialogId, int reply);

    void addCustomDhcpOptions(in WifiSsid ssid, in byte[] oui, in List<DhcpOption> options);

    void removeCustomDhcpOptions(in WifiSsid ssid, in byte[] oui);

    void reportCreateInterfaceImpact(String packageName, int interfaceType, boolean requireNewInterface, in IInterfaceCreationInfoCallback callback);

    int getMaxNumberOfChannelsPerRequest();

    void addQosPolicies(in List<QosPolicyParams> policyParamsList, in IBinder binder, String packageName, in IListListener callback);

    void removeQosPolicies(in int[] policyIdList, String packageName);

    void removeAllQosPolicies(String packageName);

    void setLinkLayerStatsPollingInterval(int intervalMs);

    void getLinkLayerStatsPollingInterval(in IIntegerListener listener);

    void setMloMode(int mode, in IBooleanListener listener);

    void getMloMode(in IIntegerListener listener);

    void addWifiLowLatencyLockListener(in IWifiLowLatencyLockListener listener);

    void removeWifiLowLatencyLockListener(in IWifiLowLatencyLockListener listener);

    void getMaxMloAssociationLinkCount(in IIntegerListener listener, in Bundle extras);

    void getMaxMloStrLinkCount(in IIntegerListener listener, in Bundle extras);

    void getSupportedSimultaneousBandCombinations(in IWifiBandsListener listener, in Bundle extras);
}
