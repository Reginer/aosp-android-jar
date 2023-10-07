/*
 * Copyright (C) 2022 The Android Open Source Project
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
/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.subscription;

import android.annotation.ColorInt;
import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.os.UserHandle;
import android.provider.Telephony.SimInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.DeviceToDeviceStatusSharingPreference;
import android.telephony.SubscriptionManager.ProfileClass;
import android.telephony.SubscriptionManager.SimDisplayNameSource;
import android.telephony.SubscriptionManager.SubscriptionType;
import android.telephony.SubscriptionManager.UsageSetting;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.ims.ImsMmTelManager;
import android.text.TextUtils;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The class represents a single row of {@link SimInfo} table. All columns (excepts unused columns)
 * in the database have a corresponding field in this class.
 *
 * The difference between {@link SubscriptionInfo} and this class is that {@link SubscriptionInfo}
 * is a subset of this class. This is intended to solve the problem that some database fields
 * required higher permission like
 * {@link android.Manifest.permission#READ_PRIVILEGED_PHONE_STATE} to access while
 * {@link SubscriptionManager#getActiveSubscriptionIdList()} only requires
 * {@link android.Manifest.permission#READ_PHONE_STATE} to access. Sometimes blanking out fields in
 * {@link SubscriptionInfo} creates ambiguity for clients hard to distinguish between insufficient
 * permission versus true failure.
 *
 * Also the fields in this class match the format used in database. For example, boolean values
 * are stored as integer, or string arrays are stored as a single comma separated string.
 */
public class SubscriptionInfoInternal {
    /**
     * Subscription Identifier, this is a device unique number
     * and not an index into an array
     */
    private final int mId;

    /**
     * The ICCID of the SIM that is associated with this subscription, empty if unknown.
     */
    @NonNull
    private final String mIccId;

    /**
     * The index of the SIM slot that currently contains the subscription and not necessarily unique
     * and maybe {@link SubscriptionManager#INVALID_SIM_SLOT_INDEX} if unknown or the subscription
     * is inactive.
     */
    private final int mSimSlotIndex;

    /**
     * The name displayed to the user that identifies this subscription. This name is used
     * in Settings page and can be renamed by the user.
     */
    @NonNull
    private final String mDisplayName;

    /**
     * The name displayed to the user that identifies subscription provider name. This name is the
     * SPN displayed in status bar and many other places. Can't be renamed by the user.
     */
    @NonNull
    private final String mCarrierName;

    /**
     * The source of the {@link #mDisplayName}.
     */
    @SimDisplayNameSource
    private final int mDisplayNameSource;

    /**
     * The color to be used for tinting the icon when displaying to the user.
     */
    @ColorInt
    private final int mIconTint;

    /**
     * The number presented to the user identify this subscription.
     */
    @NonNull
    private final String mNumber;

    /**
     * Whether user enables data roaming for this subscription or not. Either
     * {@link SubscriptionManager#DATA_ROAMING_ENABLE} or
     * {@link SubscriptionManager#DATA_ROAMING_DISABLE}.
     */
    private final int mDataRoaming;

    /**
     * Mobile Country Code.
     */
    @NonNull
    private final String mMcc;

    /**
     * Mobile Network Code.
     */
    @NonNull
    private final String mMnc;

    /**
     * EHPLMNs associated with the subscription.
     */
    @NonNull
    private final String mEhplmns;

    /**
     * HPLMNs associated with the subscription.
     */
    @NonNull
    private final String mHplmns;

    /**
     * Whether the subscription is from eSIM. It is intended to use integer to fit the database
     * format.
     */
    private final int mIsEmbedded;

    /**
     * The string ID of the SIM card. It is the ICCID of the active profile for a UICC card and the
     * EID for an eUICC card.
     */
    @NonNull
    private final String mCardString;

    /**
     * The access rules for this subscription, if it is embedded and defines any. This does not
     * include access rules for non-embedded subscriptions.
     */
    @NonNull
    private final byte[] mNativeAccessRules;

    /**
     * The carrier certificates for this subscription that are saved in carrier configs.
     * This does not include access rules from the Uicc, whether embedded or non-embedded.
     */
    @NonNull
    private final byte[] mCarrierConfigAccessRules;

    /**
     * Whether an embedded subscription is on a removable card. Such subscriptions are marked
     * inaccessible as soon as the current card is removed. Otherwise, they will remain accessible
     * unless explicitly deleted. Only meaningful when {@link #getEmbedded()} is {@code 1}. It
     * is intended to use integer to fit the database format.
     */
    private final int mIsRemovableEmbedded;

    /**
     * Whether cell broadcast extreme threat alert is enabled by the user or not.
     */
    private int mIsExtremeThreatAlertEnabled;

    /**
     * Whether cell broadcast severe threat alert is enabled by the user or not.
     */
    private int mIsSevereThreatAlertEnabled;

    /**
     * Whether cell broadcast amber alert is enabled by the user or not.
     */
    private int mIsAmberAlertEnabled;

    /**
     * Whether cell broadcast emergency alert is enabled by the user or not.
     */
    private int mIsEmergencyAlertEnabled;

    /**
     * Cell broadcast alert sound duration in seconds.
     */
    private int mAlertSoundDuration;

    /**
     * Cell broadcast alert reminder interval in minutes.
     */
    private int mReminderInterval;

    /**
     * Whether cell broadcast alert vibration is enabled by the user or not.
     */
    private int mIsAlertVibrationEnabled;

    /**
     * Whether cell broadcast alert speech is enabled by the user or not.
     */
    private int mIsAlertSpeechEnabled;

    /**
     * Whether ETWS test alert is enabled by the user or not.
     */
    private int mIsEtwsTestAlertEnabled;

    /**
     * Whether area info message is enabled by the user or not.
     */
    private int mIsAreaInfoMessageEnabled;

    /**
     * Whether cell broadcast test alert is enabled by the user or not.
     */
    private int mIsTestAlertEnabled;

    /**
     * Whether cell broadcast opt-out dialog should be shown or not.
     */
    private int mIsOptOutDialogEnabled;

    /**
     * Whether enhanced 4G mode is enabled by the user or not. It is intended to use integer to fit
     * the database format.
     */
    private final int mIsEnhanced4GModeEnabled;

    /**
     * Whether video telephony is enabled by the user or not. It is intended to use integer to fit
     * the database format.
     */
    private final int mIsVideoTelephonyEnabled;

    /**
     * Whether Wi-Fi calling is enabled by the user or not when the device is not roaming. It is
     * intended to use integer to fit the database format.
     */
    private final int mIsWifiCallingEnabled;

    /**
     * Wi-Fi calling mode when the device is not roaming.
     */
    @ImsMmTelManager.WiFiCallingMode
    private final int mWifiCallingMode;

    /**
     * Wi-Fi calling mode when the device is roaming.
     */
    @ImsMmTelManager.WiFiCallingMode
    private final int mWifiCallingModeForRoaming;

    /**
     * Whether Wi-Fi calling is enabled by the user or not when the device is roaming. It is
     * intended to use integer to fit the database format.
     */
    private final int mIsWifiCallingEnabledForRoaming;

    /**
     * Whether the subscription is opportunistic. It is intended to use integer to fit the database
     * format.
     */
    private final int mIsOpportunistic;

    /**
     * A UUID assigned to the subscription group in string format.
     *
     * @see SubscriptionManager#createSubscriptionGroup(List)
     */
    @NonNull
    private final String mGroupUuid;

    /**
     * ISO Country code for the subscription's provider.
     */
    @NonNull
    private final String mCountryIso;

    /**
     * The subscription carrier id.
     *
     * @see TelephonyManager#getSimCarrierId()
     */
    private final int mCarrierId;

    /**
     * The profile class populated from the profile metadata if present. Otherwise,
     * the profile class defaults to {@link SubscriptionManager#PROFILE_CLASS_UNSET} if there is no
     * profile metadata or the subscription is not on an eUICC ({@link #getEmbedded} returns
     * {@code 0}).
     */
    @ProfileClass
    private final int mProfileClass;

    /**
     * Type of the subscription.
     */
    @SubscriptionType
    private final int mType;

    /**
     * A package name that specifies who created the group. Empty if not available.
     */
    @NonNull
    private final String mGroupOwner;

    /**
     * The enabled mobile data policies in string format.
     */
    @NonNull
    private final String mEnabledMobileDataPolicies;

    /**
     * The IMSI (International Mobile Subscriber Identity) of the subscription.
     */
    @NonNull
    private final String mImsi;

    /**
     * Whether uicc applications are configured to enable or disable.
     * By default it's true. It is intended to use integer to fit the database format.
     */
    private final int mAreUiccApplicationsEnabled;

    /**
     * Whether the user has enabled IMS RCS User Capability Exchange (UCE) for this subscription.
     * It is intended to use integer to fit the database format.
     */
    private final int mIsRcsUceEnabled;

    /**
     * Whether the user has enabled cross SIM calling for this subscription. It is intended to
     * use integer to fit the database format.
     */
    private final int mIsCrossSimCallingEnabled;

    /**
     * The RCS configuration.
     */
    @NonNull
    private final byte[] mRcsConfig;

    /**
     * The allowed network types for reasons in string format. The format is
     * "[reason]=[network types bitmask], [reason]=[network types bitmask], ..."
     *
     * For example, "user=1239287394, thermal=298791239, carrier=3456812312".
     */
    @NonNull
    private final String mAllowedNetworkTypesForReasons;

    /**
     * Device to device sharing status.
     */
    @DeviceToDeviceStatusSharingPreference
    private final int mDeviceToDeviceStatusSharingPreference;

    /**
     * Whether the user has opted-in voice over IMS. It is intended to use integer to fit the
     * database format.
     */
    private final int mIsVoImsOptInEnabled;

    /**
     * Contacts information that allow device to device sharing.
     */
    @NonNull
    private final String mDeviceToDeviceStatusSharingContacts;

    /**
     * Whether the user has enabled NR advanced calling. It is intended to use integer to fit the
     * database format.
     */
    private final int mIsNrAdvancedCallingEnabled;

    /**
     * The phone number retrieved from carrier.
     */
    @NonNull
    private final String mNumberFromCarrier;

    /**
     * The phone number retrieved from IMS.
     */
    @NonNull
    private final String mNumberFromIms;

    /**
     * The port index of the Uicc card.
     */
    private final int mPortIndex;

    /**
     * Subscription's preferred usage setting.
     */
    @UsageSetting
    private final int mUsageSetting;

    /**
     * Last used TP message reference.
     */
    private final int mLastUsedTPMessageReference;

    /**
     * The user id associated with this subscription.
     */
    private final int mUserId;

    /**
     * Whether satellite is enabled or disabled.
     * By default, its disabled. It is intended to use integer to fit the database format.
     */
    private final int mIsSatelliteEnabled;

    // Below are the fields that do not exist in the SimInfo table.
    /**
     * The card ID of the SIM card. This maps uniquely to {@link #mCardString}.
     */
    private final int mCardId;

    /**
     * Whether group of the subscription is disabled. This is only useful if it's a grouped
     * opportunistic subscription. In this case, if all primary (non-opportunistic) subscriptions
     * in the group are deactivated (unplugged pSIM or deactivated eSIM profile), we should disable
     * this opportunistic subscription. It is intended to use integer to fit the database format.
     */
    private final boolean mIsGroupDisabled;

    /**
     * Constructor from builder.
     *
     * @param builder Builder of {@link SubscriptionInfoInternal}.
     */
    private SubscriptionInfoInternal(@NonNull Builder builder) {
        this.mId = builder.mId;
        this.mIccId = builder.mIccId;
        this.mSimSlotIndex = builder.mSimSlotIndex;
        this.mDisplayName = builder.mDisplayName;
        this.mCarrierName = builder.mCarrierName;
        this.mDisplayNameSource = builder.mDisplayNameSource;
        this.mIconTint = builder.mIconTint;
        this.mNumber = builder.mNumber;
        this.mDataRoaming = builder.mDataRoaming;
        this.mMcc = builder.mMcc;
        this.mMnc = builder.mMnc;
        this.mEhplmns = builder.mEhplmns;
        this.mHplmns = builder.mHplmns;
        this.mIsEmbedded = builder.mIsEmbedded;
        this.mCardString = builder.mCardString;
        this.mNativeAccessRules = builder.mNativeAccessRules;
        this.mCarrierConfigAccessRules = builder.mCarrierConfigAccessRules;
        this.mIsRemovableEmbedded = builder.mIsRemovableEmbedded;
        this.mIsExtremeThreatAlertEnabled = builder.mIsExtremeThreatAlertEnabled;
        this.mIsSevereThreatAlertEnabled = builder.mIsSevereThreatAlertEnabled;
        this.mIsAmberAlertEnabled = builder.mIsAmberAlertEnabled;
        this.mIsEmergencyAlertEnabled = builder.mIsEmergencyAlertEnabled;
        this.mAlertSoundDuration = builder.mAlertSoundDuration;
        this.mReminderInterval = builder.mReminderInterval;
        this.mIsAlertVibrationEnabled = builder.mIsAlertVibrationEnabled;
        this.mIsAlertSpeechEnabled = builder.mIsAlertSpeechEnabled;
        this.mIsEtwsTestAlertEnabled = builder.mIsEtwsTestAlertEnabled;
        this.mIsAreaInfoMessageEnabled = builder.mIsAreaInfoMessageEnabled;
        this.mIsTestAlertEnabled = builder.mIsTestAlertEnabled;
        this.mIsOptOutDialogEnabled = builder.mIsOptOutDialogEnabled;
        this.mIsEnhanced4GModeEnabled = builder.mIsEnhanced4GModeEnabled;
        this.mIsVideoTelephonyEnabled = builder.mIsVideoTelephonyEnabled;
        this.mIsWifiCallingEnabled = builder.mIsWifiCallingEnabled;
        this.mWifiCallingMode = builder.mWifiCallingMode;
        this.mWifiCallingModeForRoaming = builder.mWifiCallingModeForRoaming;
        this.mIsWifiCallingEnabledForRoaming = builder.mIsWifiCallingEnabledForRoaming;
        this.mIsOpportunistic = builder.mIsOpportunistic;
        this.mGroupUuid = builder.mGroupUuid;
        this.mCountryIso = builder.mCountryIso;
        this.mCarrierId = builder.mCarrierId;
        this.mProfileClass = builder.mProfileClass;
        this.mType = builder.mType;
        this.mGroupOwner = builder.mGroupOwner;
        this.mEnabledMobileDataPolicies = builder.mEnabledMobileDataPolicies;
        this.mImsi = builder.mImsi;
        this.mAreUiccApplicationsEnabled = builder.mAreUiccApplicationsEnabled;
        this.mIsRcsUceEnabled = builder.mIsRcsUceEnabled;
        this.mIsCrossSimCallingEnabled = builder.mIsCrossSimCallingEnabled;
        this.mRcsConfig = builder.mRcsConfig;
        this.mAllowedNetworkTypesForReasons = builder.mAllowedNetworkTypesForReasons;
        this.mDeviceToDeviceStatusSharingPreference =
                builder.mDeviceToDeviceStatusSharingPreference;
        this.mIsVoImsOptInEnabled = builder.mIsVoImsOptInEnabled;
        this.mDeviceToDeviceStatusSharingContacts = builder.mDeviceToDeviceStatusSharingContacts;
        this.mIsNrAdvancedCallingEnabled = builder.mIsNrAdvancedCallingEnabled;
        this.mNumberFromCarrier = builder.mNumberFromCarrier;
        this.mNumberFromIms = builder.mNumberFromIms;
        this.mPortIndex = builder.mPortIndex;
        this.mUsageSetting = builder.mUsageSetting;
        this.mLastUsedTPMessageReference = builder.mLastUsedTPMessageReference;
        this.mUserId = builder.mUserId;
        this.mIsSatelliteEnabled = builder.mIsSatelliteEnabled;

        // Below are the fields that do not exist in the SimInfo table.
        this.mCardId = builder.mCardId;
        this.mIsGroupDisabled = builder.mIsGroupDisabled;
    }

    /**
     * @return The subscription ID.
     */
    public int getSubscriptionId() {
        return mId;
    }

    /**
     * Returns the ICC ID.
     *
     * @return the ICC ID, or an empty string if one of these requirements is not met
     */
    @NonNull
    public String getIccId() {
        return mIccId;
    }

    /**
     * @return The index of the SIM slot that currently contains the subscription and not
     * necessarily unique and maybe {@link SubscriptionManager#INVALID_SIM_SLOT_INDEX} if unknown or
     * the subscription is inactive.
     */
    public int getSimSlotIndex() {
        return mSimSlotIndex;
    }

    /**
     * @return The name displayed to the user that identifies this subscription. This name is
     * used in Settings page and can be renamed by the user.
     *
     * @see #getCarrierName()
     */
    @NonNull
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * @return The name displayed to the user that identifies subscription provider name. This name
     * is the SPN displayed in status bar and many other places. Can't be renamed by the user.
     *
     * @see #getDisplayName()
     */
    @NonNull
    public String getCarrierName() {
        return mCarrierName;
    }

    /**
     * @return The source of the {@link #getDisplayName()}.
     */
    @SimDisplayNameSource
    public int getDisplayNameSource() {
        return mDisplayNameSource;
    }

    /**
     * A highlight color to use in displaying information about this {@code PhoneAccount}.
     *
     * @return A hexadecimal color value.
     */
    @ColorInt
    public int getIconTint() {
        return mIconTint;
    }

    /**
     * @return the number of this subscription.
     */
    public String getNumber() {
        return mNumber;
    }

    /**
     * Whether user enables data roaming for this subscription or not. Either
     * {@link SubscriptionManager#DATA_ROAMING_ENABLE} or
     * {@link SubscriptionManager#DATA_ROAMING_DISABLE}.
     */
    public int getDataRoaming() {
        return mDataRoaming;
    }

    /**
     * @return The mobile country code.
     */
    @NonNull
    public String getMcc() {
        return mMcc;
    }

    /**
     * @return The mobile network code.
     */
    @NonNull
    public String getMnc() {
        return mMnc;
    }

    /**
     * @return Extended home PLMNs associated with this subscription.
     */
    @NonNull
    public String getEhplmns() {
        return mEhplmns;
    }

    /**
     * @return Home PLMNs associated with this subscription.
     */
    @NonNull
    public String getHplmns() {
        return mHplmns;
    }

    /**
     * @return {@code true} if the subscription is from eSIM.
     */
    public boolean isEmbedded() {
        return mIsEmbedded != 0;
    }

    /**
     * @return {@code 1} if the subscription is from eSIM.
     */
    public int getEmbedded() {
        return mIsEmbedded;
    }

    /**
     * Returns the card string of the SIM card which contains the subscription.
     *
     * @return The card string of the SIM card which contains the subscription. The card string is
     * the ICCID for UICCs or the EID for
     * eUICCs.
     */
    @NonNull
    public String getCardString() {
        return mCardString;
    }

    /**
     * @return The access rules for this subscription, if it is embedded and defines any. This
     * does not include access rules for non-embedded subscriptions. This is the raw string
     * stored in the database.
     */
    @NonNull
    public byte[] getNativeAccessRules() {
        return mNativeAccessRules;
    }

    /**
     * @return The carrier certificates for this subscription that are saved in carrier configs.
     * This does not include access rules from the Uicc, whether embedded or non-embedded. This
     * is the raw string stored in the database.
     */
    public byte[] getCarrierConfigAccessRules() {
        return mCarrierConfigAccessRules;
    }

    /**
     * @return {@code true} if an embedded subscription is on a removable card. Such subscriptions
     * are marked inaccessible as soon as the current card is removed. Otherwise, they will remain
     * accessible unless explicitly deleted. Only meaningful when {@link #getEmbedded()} is 1.
     */
    public boolean isRemovableEmbedded() {
        return mIsRemovableEmbedded != 0;
    }

    /**
     * @return {@code 1} if an embedded subscription is on a removable card. Such subscriptions are
     * marked inaccessible as soon as the current card is removed. Otherwise, they will remain
     * accessible unless explicitly deleted. Only meaningful when {@link #getEmbedded()} is 1.
     */
    public int getRemovableEmbedded() {
        return mIsRemovableEmbedded;
    }

    /**
     * @return {@code 1} if cell broadcast extreme threat alert is enabled by the user.
     */
    public int getCellBroadcastExtremeThreatAlertEnabled() {
        return mIsExtremeThreatAlertEnabled;
    }

    /**
     * @return {@code 1} if cell broadcast amber alert is enabled by the user.
     */
    public int getCellBroadcastSevereThreatAlertEnabled() {
        return mIsSevereThreatAlertEnabled;
    }

    /**
     * @return {@code 1} if cell broadcast emergency alert is enabled by the user.
     */
    public int getCellBroadcastAmberAlertEnabled() {
        return mIsAmberAlertEnabled;
    }

    /**
     * @return {@code 1} if cell broadcast emergency alert is enabled by the user.
     */
    public int getCellBroadcastEmergencyAlertEnabled() {
        return mIsEmergencyAlertEnabled;
    }

    /**
     * @return {@code 1} if cell broadcast alert sound duration in seconds.
     */
    public int getCellBroadcastAlertSoundDuration() {
        return mAlertSoundDuration;
    }

    /**
     * @return Cell broadcast alert reminder interval in minutes.
     */
    public int getCellBroadcastAlertReminderInterval() {
        return mReminderInterval;
    }

    /**
     * @return {@code 1} if cell broadcast alert vibration is enabled by the user.
     */
    public int getCellBroadcastAlertVibrationEnabled() {
        return mIsAlertVibrationEnabled;
    }

    /**
     * @return {@code 1} if cell broadcast alert speech is enabled by the user.
     */
    public int getCellBroadcastAlertSpeechEnabled() {
        return mIsAlertSpeechEnabled;
    }

    /**
     * @return {@code 1} if ETWS test alert is enabled by the user.
     */
    public int getCellBroadcastEtwsTestAlertEnabled() {
        return mIsEtwsTestAlertEnabled;
    }

    /**
     * @return {@code 1} if area info message is enabled by the user.
     */
    public int getCellBroadcastAreaInfoMessageEnabled() {
        return mIsAreaInfoMessageEnabled;
    }

    /**
     * @return {@code 1} if cell broadcast test alert is enabled by the user.
     */
    public int getCellBroadcastTestAlertEnabled() {
        return mIsTestAlertEnabled;
    }

    /**
     * @return {@code 1} if cell broadcast opt-out dialog should be shown.
     */
    public int getCellBroadcastOptOutDialogEnabled() {
        return mIsOptOutDialogEnabled;
    }

    /**
     * @return {@code true} if enhanced 4G mode is enabled by the user or not.
     */
    public boolean isEnhanced4GModeEnabled() {
        return mIsEnhanced4GModeEnabled == 1;
    }

    /**
     * @return {@code 1} if enhanced 4G mode is enabled by the user or not. {@code 0} if disabled.
     * {@code -1} if the user did not change any setting.
     */
    public int getEnhanced4GModeEnabled() {
        return mIsEnhanced4GModeEnabled;
    }

    /**
     * @return {@code true} if video telephony is enabled by the user or not.
     */
    public boolean isVideoTelephonyEnabled() {
        return mIsVideoTelephonyEnabled != 0;
    }

    /**
     * @return {@code 1} if video telephony is enabled by the user or not.
     */
    public int getVideoTelephonyEnabled() {
        return mIsVideoTelephonyEnabled;
    }

    /**
     * @return {@code true} if Wi-Fi calling is enabled by the user or not when the device is not
     * roaming.
     */
    public boolean isWifiCallingEnabled() {
        return mIsWifiCallingEnabled == 1;
    }

    /**
     * @return {@code 1} if Wi-Fi calling is enabled by the user or not when the device is not
     * roaming. {@code 0} if disabled. {@code -1} if the user did not change any setting.
     */
    public int getWifiCallingEnabled() {
        return mIsWifiCallingEnabled;
    }

    /**
     * @return Wi-Fi calling mode when the device is not roaming.
     */
    @ImsMmTelManager.WiFiCallingMode
    public int getWifiCallingMode() {
        return mWifiCallingMode;
    }

    /**
     * @return Wi-Fi calling mode when the device is roaming.
     */
    @ImsMmTelManager.WiFiCallingMode
    public int getWifiCallingModeForRoaming() {
        return mWifiCallingModeForRoaming;
    }

    /**
     * @return {@code true} if Wi-Fi calling is enabled by the user or not when the device is
     * roaming. {@code 0} if disabled. {@code -1} if the user did not change any setting.
     */
    public boolean isWifiCallingEnabledForRoaming() {
        return mIsWifiCallingEnabledForRoaming == 1;
    }

    /**
     * @return {@code 1} if Wi-Fi calling is enabled by the user or not when the device is roaming.
     */
    public int getWifiCallingEnabledForRoaming() {
        return mIsWifiCallingEnabledForRoaming;
    }

    /**
     * An opportunistic subscription connects to a network that is
     * limited in functionality and / or coverage.
     *
     * @return {@code true} if subscription is opportunistic.
     */
    public boolean isOpportunistic() {
        return mIsOpportunistic != 0;
    }

    /**
     * An opportunistic subscription connects to a network that is
     * limited in functionality and / or coverage.
     *
     * @return {@code 1} if subscription is opportunistic.
     */
    public int getOpportunistic() {
        return mIsOpportunistic;
    }

    /**
     * Used in scenarios where different subscriptions are bundled as a group.
     * It's typically a primary and an opportunistic subscription. (see {@link #getOpportunistic()})
     * Such that those subscriptions will have some affiliated behaviors such as opportunistic
     * subscription may be invisible to the user.
     *
     * @return Group UUID in string format.
     */
    @NonNull
    public String getGroupUuid() {
        return mGroupUuid;
    }

    /**
     * @return The ISO country code. Empty if not available.
     */
    public String getCountryIso() {
        return mCountryIso;
    }

    /**
     * @return The carrier id of this subscription carrier.
     *
     * @see TelephonyManager#getSimCarrierId()
     */
    public int getCarrierId() {
        return mCarrierId;
    }

    /**
     * @return The profile class populated from the profile metadata if present. Otherwise,
     * the profile class defaults to {@link SubscriptionManager#PROFILE_CLASS_UNSET} if there is no
     * profile metadata or the subscription is not on an eUICC ({@link #getEmbedded} return
     * {@code 0}).
     */
    @ProfileClass
    public int getProfileClass() {
        return mProfileClass;
    }

    /**
     * This method returns the type of a subscription. It can be
     * {@link SubscriptionManager#SUBSCRIPTION_TYPE_LOCAL_SIM} or
     * {@link SubscriptionManager#SUBSCRIPTION_TYPE_REMOTE_SIM}.
     *
     * @return The type of the subscription.
     */
    @SubscriptionType
    public int getSubscriptionType() {
        return mType;
    }

    /**
     * @return The owner package of group the subscription belongs to.
     */
    @NonNull
    public String getGroupOwner() {
        return mGroupOwner;
    }

    /**
     * @return The enabled mobile data policies in string format.
     *
     * @see com.android.internal.telephony.data.DataSettingsManager#getMobileDataPolicyEnabled
     */
    @NonNull
    public String getEnabledMobileDataPolicies() {
        return mEnabledMobileDataPolicies;
    }

    /**
     * @return The IMSI (International Mobile Subscriber Identity) of the subscription.
     */
    @NonNull
    public String getImsi() {
        return mImsi;
    }

    /**
     * @return {@code true} if Uicc applications are set to be enabled or disabled.
     */
    public boolean areUiccApplicationsEnabled() {
        return mAreUiccApplicationsEnabled != 0;
    }

    /**
     * @return {@code 1} if Uicc applications are set to be enabled or disabled.
     */
    public int getUiccApplicationsEnabled() {
        return mAreUiccApplicationsEnabled;
    }

    /**
     * @return {@code true} if the user has enabled IMS RCS User Capability Exchange (UCE) for this
     * subscription.
     */
    public boolean isRcsUceEnabled() {
        return mIsRcsUceEnabled != 0;
    }

    /**
     * @return {@code 1} if the user has enabled IMS RCS User Capability Exchange (UCE) for this
     * subscription.
     */
    public int getRcsUceEnabled() {
        return mIsRcsUceEnabled;
    }

    /**
     * @return {@code true} if the user has enabled cross SIM calling for this subscription.
     */
    public boolean isCrossSimCallingEnabled() {
        return mIsCrossSimCallingEnabled != 0;
    }

    /**
     * @return {@code 1} if the user has enabled cross SIM calling for this subscription.
     */
    public int getCrossSimCallingEnabled() {
        return mIsCrossSimCallingEnabled;
    }

    /**
     * @return The RCS configuration.
     */
    @NonNull
    public byte[] getRcsConfig() {
        return mRcsConfig;
    }

    /**
     * The allowed network types for reasons in string format. The format is
     * "[reason]=[network types bitmask], [reason]=[network types bitmask], ..."
     *
     * For example, "user=1239287394, thermal=298791239, carrier=3456812312".
     */
    @NonNull
    public String getAllowedNetworkTypesForReasons() {
        return mAllowedNetworkTypesForReasons;
    }

    /**
     * @return Device to device sharing status.
     */
    @DeviceToDeviceStatusSharingPreference
    public int getDeviceToDeviceStatusSharingPreference() {
        return mDeviceToDeviceStatusSharingPreference;
    }

    /**
     * @return {@code true} if the user has opted-in voice over IMS.
     */
    public boolean isVoImsOptInEnabled() {
        return mIsVoImsOptInEnabled != 0;
    }

    /**
     * @return {@code 1} if the user has opted-in voice over IMS.
     */
    public int getVoImsOptInEnabled() {
        return mIsVoImsOptInEnabled;
    }

    /**
     * @return Contacts information that allow device to device sharing.
     */
    @NonNull
    public String getDeviceToDeviceStatusSharingContacts() {
        return mDeviceToDeviceStatusSharingContacts;
    }

    /**
     * @return {@code true} if the user has enabled NR advanced calling.
     */
    public boolean isNrAdvancedCallingEnabled() {
        return mIsNrAdvancedCallingEnabled == 1;
    }

    /**
     * @return {@code 1} if the user has enabled NR advanced calling. {code 0} if disabled.
     * {code -1} if the user did not change any setting.
     */
    public int getNrAdvancedCallingEnabled() {
        return mIsNrAdvancedCallingEnabled;
    }

    /**
     * @return Get the phone number retrieved from carrier.
     */
    @NonNull
    public String getNumberFromCarrier() {
        return mNumberFromCarrier;
    }

    /**
     * @return Get the phone number retrieved from IMS.
     */
    @NonNull
    public String getNumberFromIms() {
        return mNumberFromIms;
    }

    /**
     * @return The port index of the SIM card which contains the subscription.
     */
    public int getPortIndex() {
        return mPortIndex;
    }

    /**
     * Get the usage setting for this subscription.
     *
     * @return The usage setting used for this subscription.
     */
    @UsageSetting
    public int getUsageSetting() {
        return mUsageSetting;
    }

    /**
     * @return Last used TP message reference.
     */
    public int getLastUsedTPMessageReference() {
        return mLastUsedTPMessageReference;
    }

    /**
     * @return The user id associated with this subscription.
     */
    @UserIdInt
    public int getUserId() {
        return mUserId;
    }

    /**
     * @return {@code 1} if satellite is enabled.
     */
    public int getSatelliteEnabled() {
        return mIsSatelliteEnabled;
    }

    // Below are the fields that do not exist in SimInfo table.
    /**
     * @return The card ID of the SIM card which contains the subscription.
     *
     * @see android.telephony.UiccCardInfo#getCardId().
     */
    public int getCardId() {
        return mCardId;
    }

    /**
     * @return {@code true} if the group of the subscription is disabled. This is only useful if
     * it's a grouped opportunistic subscription. In this case, if all primary (non-opportunistic)
     * subscriptions in the group are deactivated (unplugged pSIM or deactivated eSIM profile), we
     * should disable this opportunistic subscription.
     */
    public boolean isGroupDisabled() {
        return mIsGroupDisabled;
    }

    /**
     * @return {@code true} if the subscription is from the actively used SIM.
     */
    public boolean isActive() {
        return mSimSlotIndex >= 0 || mType == SubscriptionManager.SUBSCRIPTION_TYPE_REMOTE_SIM;
    }

    /**
     * @return {@code true} if the subscription is visible to the user.
     */
    public boolean isVisible() {
        return !isOpportunistic() || TextUtils.isEmpty(mGroupUuid);
    }

    /** @return converted {@link SubscriptionInfo}. */
    @NonNull
    public SubscriptionInfo toSubscriptionInfo() {
        return new SubscriptionInfo.Builder()
                .setId(mId)
                .setIccId(mIccId)
                .setSimSlotIndex(mSimSlotIndex)
                .setDisplayName(mDisplayName)
                .setCarrierName(mCarrierName)
                .setDisplayNameSource(mDisplayNameSource)
                .setIconTint(mIconTint)
                .setNumber(mNumber)
                .setDataRoaming(mDataRoaming)
                .setMcc(mMcc)
                .setMnc(mMnc)
                .setEhplmns(TextUtils.isEmpty(mEhplmns) ? null : mEhplmns.split(","))
                .setHplmns(TextUtils.isEmpty(mHplmns) ? null : mHplmns.split(","))
                .setCountryIso(mCountryIso)
                .setEmbedded(mIsEmbedded != 0)
                .setNativeAccessRules(mNativeAccessRules.length == 0
                        ? null : UiccAccessRule.decodeRules(mNativeAccessRules))
                .setCardString(mCardString)
                .setCardId(mCardId)
                .setOpportunistic(mIsOpportunistic != 0)
                .setGroupUuid(mGroupUuid)
                .setGroupDisabled(mIsGroupDisabled)
                .setCarrierId(mCarrierId)
                .setProfileClass(mProfileClass)
                .setType(mType)
                .setGroupOwner(mGroupOwner)
                .setCarrierConfigAccessRules(mCarrierConfigAccessRules.length == 0
                        ? null : UiccAccessRule.decodeRules(mCarrierConfigAccessRules))
                .setUiccApplicationsEnabled(mAreUiccApplicationsEnabled != 0)
                .setPortIndex(mPortIndex)
                .setUsageSetting(mUsageSetting)
                .build();
    }

    @Override
    public String toString() {
        return "[SubscriptionInfoInternal: id=" + mId
                + " iccId=" + SubscriptionInfo.getPrintableId(mIccId)
                + " simSlotIndex=" + mSimSlotIndex
                + " portIndex=" + mPortIndex
                + " isEmbedded=" + mIsEmbedded
                + " isRemovableEmbedded=" + mIsRemovableEmbedded
                + " carrierId=" + mCarrierId
                + " displayName=" + mDisplayName
                + " carrierName=" + mCarrierName
                + " isOpportunistic=" + mIsOpportunistic
                + " groupUuid=" + mGroupUuid
                + " groupOwner=" + mGroupOwner
                + " displayNameSource="
                + SubscriptionManager.displayNameSourceToString(mDisplayNameSource)
                + " iconTint=" + mIconTint
                + " number=" + Rlog.pii(TelephonyUtils.IS_DEBUGGABLE, mNumber)
                + " dataRoaming=" + mDataRoaming
                + " mcc=" + mMcc
                + " mnc=" + mMnc
                + " ehplmns=" + mEhplmns
                + " hplmns=" + mHplmns
                + " cardString=" + SubscriptionInfo.getPrintableId(mCardString)
                + " cardId=" + mCardId
                + " nativeAccessRules=" + IccUtils.bytesToHexString(mNativeAccessRules)
                + " carrierConfigAccessRules=" + IccUtils.bytesToHexString(
                        mCarrierConfigAccessRules)
                + " countryIso=" + mCountryIso
                + " profileClass=" + mProfileClass
                + " type=" + SubscriptionManager.subscriptionTypeToString(mType)
                + " areUiccApplicationsEnabled=" + mAreUiccApplicationsEnabled
                + " usageSetting=" + SubscriptionManager.usageSettingToString(mUsageSetting)
                + " isEnhanced4GModeEnabled=" + mIsEnhanced4GModeEnabled
                + " isVideoTelephonyEnabled=" + mIsVideoTelephonyEnabled
                + " isWifiCallingEnabled=" + mIsWifiCallingEnabled
                + " isWifiCallingEnabledForRoaming=" + mIsWifiCallingEnabledForRoaming
                + " wifiCallingMode=" + ImsMmTelManager.wifiCallingModeToString(mWifiCallingMode)
                + " wifiCallingModeForRoaming="
                + ImsMmTelManager.wifiCallingModeToString(mWifiCallingModeForRoaming)
                + " enabledMobileDataPolicies=" + mEnabledMobileDataPolicies
                + " imsi=" + SubscriptionInfo.getPrintableId(mImsi)
                + " rcsUceEnabled=" + mIsRcsUceEnabled
                + " crossSimCallingEnabled=" + mIsCrossSimCallingEnabled
                + " rcsConfig=" + IccUtils.bytesToHexString(mRcsConfig)
                + " allowedNetworkTypesForReasons=" + mAllowedNetworkTypesForReasons
                + " deviceToDeviceStatusSharingPreference=" + mDeviceToDeviceStatusSharingPreference
                + " isVoImsOptInEnabled=" + mIsVoImsOptInEnabled
                + " deviceToDeviceStatusSharingContacts=" + mDeviceToDeviceStatusSharingContacts
                + " numberFromCarrier=" + Rlog.pii(TelephonyUtils.IS_DEBUGGABLE, mNumberFromCarrier)
                + " numberFromIms=" + Rlog.pii(TelephonyUtils.IS_DEBUGGABLE, mNumberFromIms)
                + " userId=" + mUserId
                + " isSatelliteEnabled=" + mIsSatelliteEnabled
                + " isGroupDisabled=" + mIsGroupDisabled
                + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionInfoInternal that = (SubscriptionInfoInternal) o;
        return mId == that.mId && mSimSlotIndex == that.mSimSlotIndex
                && mDisplayNameSource == that.mDisplayNameSource && mIconTint == that.mIconTint
                && mDataRoaming == that.mDataRoaming && mIsEmbedded == that.mIsEmbedded
                && mIsRemovableEmbedded == that.mIsRemovableEmbedded
                && mIsExtremeThreatAlertEnabled == that.mIsExtremeThreatAlertEnabled
                && mIsSevereThreatAlertEnabled == that.mIsSevereThreatAlertEnabled
                && mIsAmberAlertEnabled == that.mIsAmberAlertEnabled
                && mIsEmergencyAlertEnabled == that.mIsEmergencyAlertEnabled
                && mAlertSoundDuration == that.mAlertSoundDuration
                && mReminderInterval == that.mReminderInterval
                && mIsAlertVibrationEnabled == that.mIsAlertVibrationEnabled
                && mIsAlertSpeechEnabled == that.mIsAlertSpeechEnabled
                && mIsEtwsTestAlertEnabled == that.mIsEtwsTestAlertEnabled
                && mIsAreaInfoMessageEnabled == that.mIsAreaInfoMessageEnabled
                && mIsEnhanced4GModeEnabled == that.mIsEnhanced4GModeEnabled
                && mIsVideoTelephonyEnabled == that.mIsVideoTelephonyEnabled
                && mIsWifiCallingEnabled == that.mIsWifiCallingEnabled
                && mWifiCallingMode == that.mWifiCallingMode
                && mWifiCallingModeForRoaming == that.mWifiCallingModeForRoaming
                && mIsWifiCallingEnabledForRoaming == that.mIsWifiCallingEnabledForRoaming
                && mIsOpportunistic == that.mIsOpportunistic && mCarrierId == that.mCarrierId
                && mProfileClass == that.mProfileClass && mType == that.mType
                && mAreUiccApplicationsEnabled == that.mAreUiccApplicationsEnabled
                && mIsRcsUceEnabled == that.mIsRcsUceEnabled
                && mIsCrossSimCallingEnabled == that.mIsCrossSimCallingEnabled
                && mDeviceToDeviceStatusSharingPreference
                == that.mDeviceToDeviceStatusSharingPreference
                && mIsVoImsOptInEnabled == that.mIsVoImsOptInEnabled
                && mIsNrAdvancedCallingEnabled == that.mIsNrAdvancedCallingEnabled
                && mPortIndex == that.mPortIndex && mUsageSetting == that.mUsageSetting
                && mLastUsedTPMessageReference == that.mLastUsedTPMessageReference
                && mUserId == that.mUserId && mIsSatelliteEnabled == that.mIsSatelliteEnabled
                && mCardId == that.mCardId && mIsGroupDisabled == that.mIsGroupDisabled
                && mIccId.equals(that.mIccId) && mDisplayName.equals(that.mDisplayName)
                && mCarrierName.equals(that.mCarrierName) && mNumber.equals(that.mNumber)
                && mMcc.equals(that.mMcc) && mMnc.equals(that.mMnc) && mEhplmns.equals(
                that.mEhplmns)
                && mHplmns.equals(that.mHplmns) && mCardString.equals(that.mCardString)
                && Arrays.equals(mNativeAccessRules, that.mNativeAccessRules)
                && Arrays.equals(mCarrierConfigAccessRules, that.mCarrierConfigAccessRules)
                && mGroupUuid.equals(that.mGroupUuid) && mCountryIso.equals(that.mCountryIso)
                && mGroupOwner.equals(that.mGroupOwner) && mEnabledMobileDataPolicies.equals(
                that.mEnabledMobileDataPolicies) && mImsi.equals(that.mImsi) && Arrays.equals(
                mRcsConfig, that.mRcsConfig) && mAllowedNetworkTypesForReasons.equals(
                that.mAllowedNetworkTypesForReasons) && mDeviceToDeviceStatusSharingContacts.equals(
                that.mDeviceToDeviceStatusSharingContacts) && mNumberFromCarrier.equals(
                that.mNumberFromCarrier) && mNumberFromIms.equals(that.mNumberFromIms);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mId, mIccId, mSimSlotIndex, mDisplayName, mCarrierName,
                mDisplayNameSource, mIconTint, mNumber, mDataRoaming, mMcc, mMnc, mEhplmns, mHplmns,
                mIsEmbedded, mCardString, mIsRemovableEmbedded, mIsExtremeThreatAlertEnabled,
                mIsSevereThreatAlertEnabled, mIsAmberAlertEnabled, mIsEmergencyAlertEnabled,
                mAlertSoundDuration, mReminderInterval, mIsAlertVibrationEnabled,
                mIsAlertSpeechEnabled,
                mIsEtwsTestAlertEnabled, mIsAreaInfoMessageEnabled, mIsEnhanced4GModeEnabled,
                mIsVideoTelephonyEnabled, mIsWifiCallingEnabled, mWifiCallingMode,
                mWifiCallingModeForRoaming, mIsWifiCallingEnabledForRoaming, mIsOpportunistic,
                mGroupUuid, mCountryIso, mCarrierId, mProfileClass, mType, mGroupOwner,
                mEnabledMobileDataPolicies, mImsi, mAreUiccApplicationsEnabled, mIsRcsUceEnabled,
                mIsCrossSimCallingEnabled, mAllowedNetworkTypesForReasons,
                mDeviceToDeviceStatusSharingPreference, mIsVoImsOptInEnabled,
                mDeviceToDeviceStatusSharingContacts, mIsNrAdvancedCallingEnabled,
                mNumberFromCarrier,
                mNumberFromIms, mPortIndex, mUsageSetting, mLastUsedTPMessageReference, mUserId,
                mIsSatelliteEnabled, mCardId, mIsGroupDisabled);
        result = 31 * result + Arrays.hashCode(mNativeAccessRules);
        result = 31 * result + Arrays.hashCode(mCarrierConfigAccessRules);
        result = 31 * result + Arrays.hashCode(mRcsConfig);
        return result;
    }

    /**
     * The builder class of {@link SubscriptionInfoInternal}.
     */
    public static class Builder {
        /**
         * The subscription id.
         */
        private int mId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        /**
         * The ICCID of the SIM that is associated with this subscription, empty if unknown.
         */
        @NonNull
        private String mIccId = "";

        /**
         * The index of the SIM slot that currently contains the subscription and not necessarily
         * unique and maybe {@link SubscriptionManager#INVALID_SIM_SLOT_INDEX} if unknown or the
         * subscription is inactive.
         */
        private int mSimSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;

        /**
         * The name displayed to the user that identifies this subscription. This name is used
         * in Settings page and can be renamed by the user.
         */
        @NonNull
        private String mDisplayName = "";

        /**
         * The name displayed to the user that identifies subscription provider name. This name
         * is the SPN displayed in status bar and many other places. Can't be renamed by the user.
         */
        @NonNull
        private String mCarrierName = "";

        /**
         * The source of the display name.
         */
        @SimDisplayNameSource
        private int mDisplayNameSource = SubscriptionManager.NAME_SOURCE_UNKNOWN;

        /**
         * The color to be used for tinting the icon when displaying to the user.
         */
        private int mIconTint = 0;

        /**
         * The number presented to the user identify this subscription.
         */
        @NonNull
        private String mNumber = "";

        /**
         * Whether user enables data roaming for this subscription or not. Either
         * {@link SubscriptionManager#DATA_ROAMING_ENABLE} or
         * {@link SubscriptionManager#DATA_ROAMING_DISABLE}.
         */
        private int mDataRoaming = SubscriptionManager.DATA_ROAMING_DISABLE;

        /**
         * The mobile country code.
         */
        @NonNull
        private String mMcc = "";

        /**
         * The mobile network code.
         */
        @NonNull
        private String mMnc = "";

        /**
         * EHPLMNs associated with the subscription.
         */
        @NonNull
        private String mEhplmns = "";

        /**
         * HPLMNs associated with the subscription.
         */
        @NonNull
        private String mHplmns = "";

        /**
         * Whether the subscription is from eSIM.
         */
        private int mIsEmbedded = 0;

        /**
         * The card string of the SIM card.
         */
        @NonNull
        private String mCardString = "";

        /**
         * The native access rules for this subscription, if it is embedded and defines any. This
         * does not include access rules for non-embedded subscriptions.
         */
        @NonNull
        private byte[] mNativeAccessRules = new byte[0];

        /**
         * The carrier certificates for this subscription that are saved in carrier configs.
         * This does not include access rules from the Uicc, whether embedded or non-embedded.
         */
        @NonNull
        private byte[] mCarrierConfigAccessRules = new byte[0];

        /**
         * Whether an embedded subscription is on a removable card. Such subscriptions are marked
         * inaccessible as soon as the current card is removed. Otherwise, they will remain
         * accessible unless explicitly deleted. Only meaningful when {@link #getEmbedded()} is
         * {@code 1}.
         */
        private int mIsRemovableEmbedded = 0;

        /**
         * Whether cell broadcast extreme threat alert is enabled by the user or not.
         */
        private int mIsExtremeThreatAlertEnabled = 1;

        /**
         * Whether cell broadcast severe threat alert is enabled by the user or not.
         */
        private int mIsSevereThreatAlertEnabled = 1;

        /**
         * Whether cell broadcast amber alert is enabled by the user or not.
         */
        private int mIsAmberAlertEnabled = 1;

        /**
         * Whether cell broadcast emergency alert is enabled by the user or not.
         */
        private int mIsEmergencyAlertEnabled = 1;

        /**
         * Cell broadcast alert sound duration in seconds.
         */
        private int mAlertSoundDuration = 4;

        /**
         * Cell broadcast alert reminder interval in minutes.
         */
        private int mReminderInterval = 0;

        /**
         * Whether cell broadcast alert vibration is enabled by the user or not.
         */
        private int mIsAlertVibrationEnabled = 1;

        /**
         * Whether cell broadcast alert speech is enabled by the user or not.
         */
        private int mIsAlertSpeechEnabled = 1;

        /**
         * Whether ETWS test alert is enabled by the user or not.
         */
        private int mIsEtwsTestAlertEnabled = 0;

        /**
         * Whether area info message is enabled by the user or not.
         */
        private int mIsAreaInfoMessageEnabled = 1;

        /**
         * Whether cell broadcast test alert is enabled by the user or not.
         */
        private int mIsTestAlertEnabled = 0;

        /**
         * Whether cell broadcast opt-out dialog should be shown or not.
         */
        private int mIsOptOutDialogEnabled = 1;

        /**
         * Whether enhanced 4G mode is enabled by the user or not.
         */
        private int mIsEnhanced4GModeEnabled = -1;

        /**
         * Whether video telephony is enabled by the user or not.
         */
        private int mIsVideoTelephonyEnabled = -1;

        /**
         * Whether Wi-Fi calling is enabled by the user or not when the device is not roaming.
         */
        private int mIsWifiCallingEnabled = -1;

        /**
         * Wi-Fi calling mode when the device is not roaming.
         */
        @ImsMmTelManager.WiFiCallingMode
        private int mWifiCallingMode = ImsMmTelManager.WIFI_MODE_UNKNOWN;

        /**
         * Wi-Fi calling mode when the device is roaming.
         */
        @ImsMmTelManager.WiFiCallingMode
        private int mWifiCallingModeForRoaming = ImsMmTelManager.WIFI_MODE_UNKNOWN;

        /**
         * Whether Wi-Fi calling is enabled by the user or not when the device is roaming.
         */
        private int mIsWifiCallingEnabledForRoaming = -1;

        /**
         * Whether the subscription is opportunistic or not.
         */
        private int mIsOpportunistic = 0;

        /**
         * The group UUID of the subscription group in string format.
         */
        @NonNull
        private String mGroupUuid = "";

        /**
         * The ISO Country code for the subscription's provider.
         */
        @NonNull
        private String mCountryIso = "";

        /**
         * The carrier id.
         *
         * @see TelephonyManager#getSimCarrierId()
         */
        private int mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;

        /**
         * The profile class populated from the profile metadata if present. Otherwise, the profile
         * class defaults to {@link SubscriptionManager#PROFILE_CLASS_UNSET} if there is no profile
         * metadata or the subscription is not on an eUICC ({@link #getEmbedded} returns
         * {@code 0}).
         */
        @ProfileClass
        private int mProfileClass = SubscriptionManager.PROFILE_CLASS_UNSET;

        /**
         * The subscription type.
         */
        @SubscriptionType
        private int mType = SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM;

        /**
         * The owner package of group the subscription belongs to.
         */
        @NonNull
        private String mGroupOwner = "";

        /**
         * The enabled mobile data policies in string format.
         */
        @NonNull
        private String mEnabledMobileDataPolicies = "";

        /**
         * The IMSI (International Mobile Subscriber Identity) of the subscription.
         */
        @NonNull
        private String mImsi = "";

        /**
         * Whether Uicc applications are configured to enable or not.
         */
        private int mAreUiccApplicationsEnabled = 1;

        /**
         * Whether the user has enabled IMS RCS User Capability Exchange (UCE) for this
         * subscription.
         */
        private int mIsRcsUceEnabled = 0;

        /**
         * Whether the user has enabled cross SIM calling for this subscription.
         */
        private int mIsCrossSimCallingEnabled = 0;

        /**
         * The RCS configuration.
         */
        private byte[] mRcsConfig = new byte[0];

        /**
         * The allowed network types for reasons in string format. The format is
         * "[reason]=[network types bitmask], [reason]=[network types bitmask], ..."
         *
         * For example, "user=1239287394, thermal=298791239, carrier=3456812312".
         */
        private String mAllowedNetworkTypesForReasons = "";

        /**
         * Device to device sharing status.
         */
        @DeviceToDeviceStatusSharingPreference
        private int mDeviceToDeviceStatusSharingPreference =
                SubscriptionManager.D2D_SHARING_DISABLED;

        /**
         * Whether the user has opted-in voice over IMS.
         */
        private int mIsVoImsOptInEnabled = 0;

        /**
         * Contacts information that allow device to device sharing.
         */
        @NonNull
        private String mDeviceToDeviceStatusSharingContacts = "";

        /**
         * Whether the user has enabled NR advanced calling.
         */
        private int mIsNrAdvancedCallingEnabled = -1;

        /**
         * The phone number retrieved from carrier.
         */
        @NonNull
        private String mNumberFromCarrier = "";

        /**
         * The phone number retrieved from IMS.
         */
        @NonNull
        private String mNumberFromIms = "";

        /**
         * the port index of the Uicc card.
         */
        private int mPortIndex = TelephonyManager.INVALID_PORT_INDEX;

        /**
         * Subscription's preferred usage setting.
         */
        @UsageSetting
        private int mUsageSetting = SubscriptionManager.USAGE_SETTING_UNKNOWN;

        /**
         * Last used TP message reference.
         */
        private int mLastUsedTPMessageReference = -1;

        /**
         * The user id associated with this subscription.
         */
        private int mUserId = UserHandle.USER_NULL;

        /**
         * Whether satellite is enabled or not.
         */
        private int mIsSatelliteEnabled = -1;

        // The following fields do not exist in the SimInfo table.
        /**
         * The card ID of the SIM card which contains the subscription.
         */
        private int mCardId = TelephonyManager.UNINITIALIZED_CARD_ID;

        /**
         * Whether group of the subscription is disabled. This is only useful if it's a grouped
         * opportunistic subscription. In this case, if all primary (non-opportunistic)
         * subscriptions in the group are deactivated (unplugged pSIM or deactivated eSIM profile),
         * we should disable this opportunistic subscription.
         */
        private boolean mIsGroupDisabled;

        /**
         * Default constructor.
         */
        public Builder() {
        }

        /**
         * Constructor from {@link SubscriptionInfoInternal}.
         *
         * @param info The subscription info.
         */
        public Builder(@NonNull SubscriptionInfoInternal info) {
            mId = info.mId;
            mIccId = info.mIccId;
            mSimSlotIndex = info.mSimSlotIndex;
            mDisplayName = info.mDisplayName;
            mCarrierName = info.mCarrierName;
            mDisplayNameSource = info.mDisplayNameSource;
            mIconTint = info.mIconTint;
            mNumber = info.mNumber;
            mDataRoaming = info.mDataRoaming;
            mMcc = info.mMcc;
            mMnc = info.mMnc;
            mEhplmns = info.mEhplmns;
            mHplmns = info.mHplmns;
            mIsEmbedded = info.mIsEmbedded;
            mCardString = info.mCardString;
            mNativeAccessRules = info.mNativeAccessRules;
            mCarrierConfigAccessRules = info.mCarrierConfigAccessRules;
            mIsRemovableEmbedded = info.mIsRemovableEmbedded;
            mIsExtremeThreatAlertEnabled = info.mIsExtremeThreatAlertEnabled;
            mIsSevereThreatAlertEnabled = info.mIsSevereThreatAlertEnabled;
            mIsAmberAlertEnabled = info.mIsAmberAlertEnabled;
            mIsEmergencyAlertEnabled = info.mIsEmergencyAlertEnabled;
            mAlertSoundDuration = info.mAlertSoundDuration;
            mReminderInterval = info.mReminderInterval;
            mIsAlertVibrationEnabled = info.mIsAlertVibrationEnabled;
            mIsAlertSpeechEnabled = info.mIsAlertSpeechEnabled;
            mIsEtwsTestAlertEnabled = info.mIsEtwsTestAlertEnabled;
            mIsAreaInfoMessageEnabled = info.mIsAreaInfoMessageEnabled;
            mIsTestAlertEnabled = info.mIsTestAlertEnabled;
            mIsOptOutDialogEnabled = info.mIsOptOutDialogEnabled;
            mIsEnhanced4GModeEnabled = info.mIsEnhanced4GModeEnabled;
            mIsVideoTelephonyEnabled = info.mIsVideoTelephonyEnabled;
            mIsWifiCallingEnabled = info.mIsWifiCallingEnabled;
            mWifiCallingMode = info.mWifiCallingMode;
            mWifiCallingModeForRoaming = info.mWifiCallingModeForRoaming;
            mIsWifiCallingEnabledForRoaming = info.mIsWifiCallingEnabledForRoaming;
            mIsOpportunistic = info.mIsOpportunistic;
            mGroupUuid = info.mGroupUuid;
            mCountryIso = info.mCountryIso;
            mCarrierId = info.mCarrierId;
            mProfileClass = info.mProfileClass;
            mType = info.mType;
            mGroupOwner = info.mGroupOwner;
            mEnabledMobileDataPolicies = info.mEnabledMobileDataPolicies;
            mImsi = info.mImsi;
            mAreUiccApplicationsEnabled = info.mAreUiccApplicationsEnabled;
            mIsRcsUceEnabled = info.mIsRcsUceEnabled;
            mIsCrossSimCallingEnabled = info.mIsCrossSimCallingEnabled;
            mRcsConfig = info.mRcsConfig;
            mAllowedNetworkTypesForReasons = info.mAllowedNetworkTypesForReasons;
            mDeviceToDeviceStatusSharingPreference = info.mDeviceToDeviceStatusSharingPreference;
            mIsVoImsOptInEnabled = info.mIsVoImsOptInEnabled;
            mDeviceToDeviceStatusSharingContacts = info.mDeviceToDeviceStatusSharingContacts;
            mIsNrAdvancedCallingEnabled = info.mIsNrAdvancedCallingEnabled;
            mNumberFromCarrier = info.mNumberFromCarrier;
            mNumberFromIms = info.mNumberFromIms;
            mPortIndex = info.mPortIndex;
            mUsageSetting = info.mUsageSetting;
            mLastUsedTPMessageReference = info.getLastUsedTPMessageReference();
            mUserId = info.mUserId;
            mIsSatelliteEnabled = info.mIsSatelliteEnabled;
            // Below are the fields that do not exist in the SimInfo table.
            mCardId = info.mCardId;
            mIsGroupDisabled = info.mIsGroupDisabled;
        }

        /**
         * Set the subscription id.
         *
         * @param id The subscription id.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setId(int id) {
            mId = id;
            return this;
        }

        /**
         * Set the ICCID of the SIM that is associated with this subscription.
         *
         * @param iccId The ICCID of the SIM that is associated with this subscription.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setIccId(@NonNull String iccId) {
            Objects.requireNonNull(iccId);
            mIccId = iccId;
            return this;
        }

        /**
         * Set the SIM index of the slot that currently contains the subscription. Set to
         * {@link SubscriptionManager#INVALID_SIM_SLOT_INDEX} if the subscription is inactive.
         *
         * @param simSlotIndex The SIM slot index.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setSimSlotIndex(int simSlotIndex) {
            mSimSlotIndex = simSlotIndex;
            return this;
        }

        /**
         * The name displayed to the user that identifies this subscription. This name is used
         * in Settings page and can be renamed by the user.
         *
         * @param displayName The display name.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setDisplayName(@NonNull String displayName) {
            Objects.requireNonNull(displayName);
            mDisplayName = displayName;
            return this;
        }

        /**
         * The name displayed to the user that identifies subscription provider name. This name
         * is the SPN displayed in status bar and many other places. Can't be renamed by the user.
         *
         * @param carrierName The carrier name.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCarrierName(@NonNull String carrierName) {
            Objects.requireNonNull(carrierName);
            mCarrierName = carrierName;
            return this;
        }

        /**
         * Set the source of the display name.
         *
         * @param displayNameSource The source of the display name.
         * @return The builder.
         *
         * @see SubscriptionInfoInternal#getDisplayName()
         */
        @NonNull
        public Builder setDisplayNameSource(@SimDisplayNameSource int displayNameSource) {
            mDisplayNameSource = displayNameSource;
            return this;
        }

        /**
         * Set the color to be used for tinting the icon when displaying to the user.
         *
         * @param iconTint The color to be used for tinting the icon when displaying to the user.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setIconTint(int iconTint) {
            mIconTint = iconTint;
            return this;
        }

        /**
         * Set the number presented to the user identify this subscription.
         *
         * @param number the number presented to the user identify this subscription.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setNumber(@NonNull String number) {
            Objects.requireNonNull(number);
            mNumber = number;
            return this;
        }

        /**
         * Set whether user enables data roaming for this subscription or not.
         *
         * @param dataRoaming Data roaming mode. Either
         * {@link SubscriptionManager#DATA_ROAMING_ENABLE} or
         * {@link SubscriptionManager#DATA_ROAMING_DISABLE}
         *
         * @return The builder.
         */
        @NonNull
        public Builder setDataRoaming(int dataRoaming) {
            mDataRoaming = dataRoaming;
            return this;
        }

        /**
         * Set the mobile country code.
         *
         * @param mcc The mobile country code.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setMcc(@NonNull String mcc) {
            Objects.requireNonNull(mcc);
            mMcc = mcc;
            return this;
        }

        /**
         * Set the mobile network code.
         *
         * @param mnc Mobile network code.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setMnc(@NonNull String mnc) {
            Objects.requireNonNull(mnc);
            mMnc = mnc;
            return this;
        }

        /**
         * Set EHPLMNs associated with the subscription.
         *
         * @param ehplmns EHPLMNs associated with the subscription.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setEhplmns(@NonNull String ehplmns) {
            Objects.requireNonNull(ehplmns);
            mEhplmns = ehplmns;
            return this;
        }

        /**
         * Set HPLMNs associated with the subscription.
         *
         * @param hplmns HPLMNs associated with the subscription.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setHplmns(@NonNull String hplmns) {
            Objects.requireNonNull(hplmns);
            mHplmns = hplmns;
            return this;
        }

        /**
         * Set whether the subscription is from eSIM or not.
         *
         * @param isEmbedded {@code 1} if the subscription is from eSIM.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setEmbedded(int isEmbedded) {
            mIsEmbedded = isEmbedded;
            return this;
        }

        /**
         * Set the card string of the SIM card.
         *
         * @param cardString The card string of the SIM card.
         *
         * @return The builder.
         *
         * @see #getCardString()
         */
        @NonNull
        public Builder setCardString(@NonNull String cardString) {
            Objects.requireNonNull(cardString);
            mCardString = cardString;
            return this;
        }

        /**
         * Set the native access rules for this subscription, if it is embedded and defines any.
         * This does not include access rules for non-embedded subscriptions.
         *
         * @param nativeAccessRules The native access rules for this subscription.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setNativeAccessRules(@NonNull byte[] nativeAccessRules) {
            Objects.requireNonNull(nativeAccessRules);
            mNativeAccessRules = nativeAccessRules;
            return this;
        }

        /**
         * Set the native access rules for this subscription, if it is embedded and defines any.
         * This does not include access rules for non-embedded subscriptions.
         *
         * @param nativeAccessRules The native access rules for this subscription.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setNativeAccessRules(@NonNull List<UiccAccessRule> nativeAccessRules) {
            Objects.requireNonNull(nativeAccessRules);
            if (!nativeAccessRules.isEmpty()) {
                mNativeAccessRules = UiccAccessRule.encodeRules(
                        nativeAccessRules.toArray(new UiccAccessRule[0]));
            }
            return this;
        }

        /**
         * Set the carrier certificates for this subscription that are saved in carrier configs.
         * This does not include access rules from the Uicc, whether embedded or non-embedded.
         *
         * @param carrierConfigAccessRules The carrier certificates for this subscription.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCarrierConfigAccessRules(@NonNull byte[] carrierConfigAccessRules) {
            Objects.requireNonNull(carrierConfigAccessRules);
            mCarrierConfigAccessRules = carrierConfigAccessRules;
            return this;
        }

        /**
         * Set whether an embedded subscription is on a removable card. Such subscriptions are
         * marked inaccessible as soon as the current card is removed. Otherwise, they will remain
         * accessible unless explicitly deleted. Only meaningful when {@link #getEmbedded()} is
         * {@code 1}.
         *
         * @param isRemovableEmbedded {@code true} if the subscription is from the removable
         * embedded SIM.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setRemovableEmbedded(boolean isRemovableEmbedded) {
            mIsRemovableEmbedded = isRemovableEmbedded ? 1 : 0;
            return this;
        }

        /**
         * Set whether an embedded subscription is on a removable card. Such subscriptions are
         * marked inaccessible as soon as the current card is removed. Otherwise, they will remain
         * accessible unless explicitly deleted. Only meaningful when {@link #getEmbedded()} is
         * {@code 1}.
         *
         * @param isRemovableEmbedded {@code 1} if the subscription is from the removable
         * embedded SIM.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setRemovableEmbedded(int isRemovableEmbedded) {
            mIsRemovableEmbedded = isRemovableEmbedded;
            return this;
        }

        /**
         * Set whether cell broadcast extreme threat alert is enabled by the user or not.
         *
         * @param isExtremeThreatAlertEnabled whether cell broadcast extreme threat alert is enabled
         * by the user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastExtremeThreatAlertEnabled(int isExtremeThreatAlertEnabled) {
            mIsExtremeThreatAlertEnabled = isExtremeThreatAlertEnabled;
            return this;
        }

        /**
         * Set whether cell broadcast severe threat alert is enabled by the user or not.
         *
         * @param isSevereThreatAlertEnabled whether cell broadcast severe threat alert is enabled
         * by the user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastSevereThreatAlertEnabled(int isSevereThreatAlertEnabled) {
            mIsSevereThreatAlertEnabled = isSevereThreatAlertEnabled;
            return this;
        }

        /**
         * Set whether cell broadcast amber alert is enabled by the user or not.
         *
         * @param isAmberAlertEnabled whether cell broadcast amber alert is enabled by the user or
         * not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastAmberAlertEnabled(int isAmberAlertEnabled) {
            mIsAmberAlertEnabled = isAmberAlertEnabled;
            return this;
        }

        /**
         * Set whether cell broadcast emergency alert is enabled by the user or not.
         *
         * @param isEmergencyAlertEnabled whether cell broadcast emergency alert is enabled by the
         * user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastEmergencyAlertEnabled(int isEmergencyAlertEnabled) {
            mIsEmergencyAlertEnabled = isEmergencyAlertEnabled;
            return this;
        }

        /**
         * Set cell broadcast alert sound duration.
         *
         * @param alertSoundDuration Alert sound duration in seconds.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastAlertSoundDuration(int alertSoundDuration) {
            mAlertSoundDuration = alertSoundDuration;
            return this;
        }

        /**
         * Set cell broadcast alert reminder interval in minutes.
         *
         * @param reminderInterval Alert reminder interval in minutes.
         *
         * @return The builder.
         */
        public Builder setCellBroadcastAlertReminderInterval(int reminderInterval) {
            mReminderInterval = reminderInterval;
            return this;
        }

        /**
         * Set whether cell broadcast alert vibration is enabled by the user or not.
         *
         * @param isAlertVibrationEnabled whether cell broadcast alert vibration is enabled by the
         * user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastAlertVibrationEnabled(int isAlertVibrationEnabled) {
            mIsAlertVibrationEnabled = isAlertVibrationEnabled;
            return this;
        }

        /**
         * Set whether cell broadcast alert speech is enabled by the user or not.
         *
         * @param isAlertSpeechEnabled whether cell broadcast alert speech is enabled by the user or
         * not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastAlertSpeechEnabled(int isAlertSpeechEnabled) {
            mIsAlertSpeechEnabled = isAlertSpeechEnabled;
            return this;
        }

        /**
         * Set whether ETWS test alert is enabled by the user or not.
         *
         * @param isEtwsTestAlertEnabled whether cell broadcast ETWS test alert is enabled by the
         * user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastEtwsTestAlertEnabled(int isEtwsTestAlertEnabled) {
            mIsEtwsTestAlertEnabled = isEtwsTestAlertEnabled;
            return this;
        }

        /**
         * Set whether area info message is enabled by the user or not.
         *
         * @param isAreaInfoMessageEnabled whether cell broadcast area info message is enabled by
         * the user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastAreaInfoMessageEnabled(int isAreaInfoMessageEnabled) {
            mIsAreaInfoMessageEnabled = isAreaInfoMessageEnabled;
            return this;
        }

        /**
         * Set whether cell broadcast test alert is enabled by the user or not.
         *
         * @param isTestAlertEnabled whether cell broadcast test alert is enabled by the user or
         * not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastTestAlertEnabled(int isTestAlertEnabled) {
            mIsTestAlertEnabled = isTestAlertEnabled;
            return this;
        }

        /**
         * Set whether cell broadcast opt-out dialog should be shown or not.
         *
         * @param isOptOutDialogEnabled whether cell broadcast opt-out dialog should be shown or
         * not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setCellBroadcastOptOutDialogEnabled(int isOptOutDialogEnabled) {
            mIsOptOutDialogEnabled = isOptOutDialogEnabled;
            return this;
        }

        /**
         * Set whether enhanced 4G mode is enabled by the user or not.
         *
         * @param isEnhanced4GModeEnabled whether enhanced 4G mode is enabled by the user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setEnhanced4GModeEnabled(int isEnhanced4GModeEnabled) {
            mIsEnhanced4GModeEnabled = isEnhanced4GModeEnabled;
            return this;
        }

        /**
         * Set whether video telephony is enabled by the user or not.
         *
         * @param isVideoTelephonyEnabled whether video telephony is enabled by the user or not.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setVideoTelephonyEnabled(int isVideoTelephonyEnabled) {
            mIsVideoTelephonyEnabled = isVideoTelephonyEnabled;
            return this;
        }

        /**
         * Set whether Wi-Fi calling is enabled by the user or not when the device is not roaming.
         *
         * @param isWifiCallingEnabled whether Wi-Fi calling is enabled by the user or not when
         * the device is not roaming.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setWifiCallingEnabled(int isWifiCallingEnabled) {
            mIsWifiCallingEnabled = isWifiCallingEnabled;
            return this;
        }

        /**
         * Set Wi-Fi calling mode when the device is not roaming.
         *
         * @param wifiCallingMode Wi-Fi calling mode when the device is not roaming.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setWifiCallingMode(@ImsMmTelManager.WiFiCallingMode int wifiCallingMode) {
            mWifiCallingMode = wifiCallingMode;
            return this;
        }

        /**
         * Set Wi-Fi calling mode when the device is roaming.
         *
         * @param wifiCallingModeForRoaming Wi-Fi calling mode when the device is roaming.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setWifiCallingModeForRoaming(
                @ImsMmTelManager.WiFiCallingMode int wifiCallingModeForRoaming) {
            mWifiCallingModeForRoaming = wifiCallingModeForRoaming;
            return this;
        }

        /**
         * Set whether Wi-Fi calling is enabled by the user or not when the device is roaming.
         *
         * @param wifiCallingEnabledForRoaming whether Wi-Fi calling is enabled by the user or not
         * when the device is roaming.
         *
         * @return The builder.
         */
        @NonNull
        public Builder setWifiCallingEnabledForRoaming(int wifiCallingEnabledForRoaming) {
            mIsWifiCallingEnabledForRoaming = wifiCallingEnabledForRoaming;
            return this;
        }

        /**
         * Set whether the subscription is opportunistic or not.
         *
         * @param isOpportunistic {@code 1} if the subscription is opportunistic.
         * @return The builder.
         */
        @NonNull
        public Builder setOpportunistic(int isOpportunistic) {
            mIsOpportunistic = isOpportunistic;
            return this;
        }

        /**
         * Set the group UUID of the subscription group.
         *
         * @param groupUuid The group UUID.
         * @return The builder.
         *
         */
        @NonNull
        public Builder setGroupUuid(@NonNull String groupUuid) {
            Objects.requireNonNull(groupUuid);
            mGroupUuid = groupUuid;
            return this;
        }

        /**
         * Set the ISO country code for the subscription's provider.
         *
         * @param countryIso The ISO country code for the subscription's provider.
         * @return The builder.
         */
        @NonNull
        public Builder setCountryIso(@NonNull String countryIso) {
            Objects.requireNonNull(countryIso);
            mCountryIso = countryIso;
            return this;
        }

        /**
         * Set the subscription carrier id.
         *
         * @param carrierId The carrier id.
         * @return The builder
         *
         * @see TelephonyManager#getSimCarrierId()
         */
        @NonNull
        public Builder setCarrierId(int carrierId) {
            mCarrierId = carrierId;
            return this;
        }

        /**
         * Set the profile class populated from the profile metadata if present.
         *
         * @param profileClass the profile class populated from the profile metadata if present.
         * @return The builder
         *
         * @see #getProfileClass()
         */
        @NonNull
        public Builder setProfileClass(@ProfileClass int profileClass) {
            mProfileClass = profileClass;
            return this;
        }

        /**
         * Set the subscription type.
         *
         * @param type Subscription type.
         * @return The builder.
         */
        @NonNull
        public Builder setType(@SubscriptionType int type) {
            mType = type;
            return this;
        }

        /**
         * Set the owner package of group the subscription belongs to.
         *
         * @param groupOwner Owner package of group the subscription belongs to.
         * @return The builder.
         */
        @NonNull
        public Builder setGroupOwner(@NonNull String groupOwner) {
            Objects.requireNonNull(groupOwner);
            mGroupOwner = groupOwner;
            return this;
        }

        /**
         * Set the enabled mobile data policies.
         *
         * @param enabledMobileDataPolicies The enabled mobile data policies.
         * @return The builder.
         */
        @NonNull
        public Builder setEnabledMobileDataPolicies(@NonNull String enabledMobileDataPolicies) {
            Objects.requireNonNull(enabledMobileDataPolicies);
            mEnabledMobileDataPolicies = enabledMobileDataPolicies;
            return this;
        }

        /**
         * Set the IMSI (International Mobile Subscriber Identity) of the subscription.
         *
         * @param imsi The IMSI.
         * @return The builder.
         */
        @NonNull
        public Builder setImsi(@NonNull String imsi) {
            Objects.requireNonNull(imsi);
            mImsi = imsi;
            return this;
        }

        /**
         * Set whether Uicc applications are configured to enable or not.
         *
         * @param areUiccApplicationsEnabled {@code 1} if Uicc applications are configured to
         * enable.
         * @return The builder.
         */
        @NonNull
        public Builder setUiccApplicationsEnabled(int areUiccApplicationsEnabled) {
            mAreUiccApplicationsEnabled = areUiccApplicationsEnabled;
            return this;
        }

        /**
         * Set whether the user has enabled IMS RCS User Capability Exchange (UCE) for this
         * subscription.
         *
         * @param isRcsUceEnabled If the user enabled RCS UCE for this subscription.
         * @return The builder.
         */
        @NonNull
        public Builder setRcsUceEnabled(int isRcsUceEnabled) {
            mIsRcsUceEnabled = isRcsUceEnabled;
            return this;
        }

        /**
         * Set whether the user has enabled cross SIM calling for this subscription.
         *
         * @param isCrossSimCallingEnabled If the user enabled cross SIM calling for this
         * subscription.
         * @return The builder.
         */
        @NonNull
        public Builder setCrossSimCallingEnabled(int isCrossSimCallingEnabled) {
            mIsCrossSimCallingEnabled = isCrossSimCallingEnabled;
            return this;
        }

        /**
         * Set the RCS config for this subscription.
         *
         * @param rcsConfig The RCS config for this subscription.
         * @return The builder.
         */
        @NonNull
        public Builder setRcsConfig(byte[] rcsConfig) {
            Objects.requireNonNull(rcsConfig);
            mRcsConfig = rcsConfig;
            return this;
        }

        /**
         * Set the allowed network types for reasons.
         *
         * @param allowedNetworkTypesForReasons The allowed network types for reasons in string
         * format. The format is
         * "[reason]=[network types bitmask], [reason]=[network types bitmask], ..."
         *
         * For example, "user=1239287394, thermal=298791239, carrier=3456812312".
         *
         * @return The builder.
         */
        public Builder setAllowedNetworkTypesForReasons(
                @NonNull String allowedNetworkTypesForReasons) {
            Objects.requireNonNull(allowedNetworkTypesForReasons);
            mAllowedNetworkTypesForReasons = allowedNetworkTypesForReasons;
            return this;
        }

        /**
         * Set device to device sharing status.
         *
         * @param deviceToDeviceStatusSharingPreference Device to device sharing status.
         * @return The builder.
         */
        public Builder setDeviceToDeviceStatusSharingPreference(
                @DeviceToDeviceStatusSharingPreference int deviceToDeviceStatusSharingPreference) {
            mDeviceToDeviceStatusSharingPreference = deviceToDeviceStatusSharingPreference;
            return this;
        }

        /**
         * Set whether the user has opted-in voice over IMS.
         *
         * @param isVoImsOptInEnabled Whether the user has opted-in voice over IMS.
         * @return The builder.
         */
        @NonNull
        public Builder setVoImsOptInEnabled(int isVoImsOptInEnabled) {
            mIsVoImsOptInEnabled = isVoImsOptInEnabled;
            return this;
        }

        /**
         * Set contacts information that allow device to device sharing.
         *
         * @param deviceToDeviceStatusSharingContacts contacts information that allow device to
         * device sharing.
         * @return The builder.
         */
        @NonNull
        public Builder setDeviceToDeviceStatusSharingContacts(
                @NonNull String deviceToDeviceStatusSharingContacts) {
            Objects.requireNonNull(deviceToDeviceStatusSharingContacts);
            mDeviceToDeviceStatusSharingContacts = deviceToDeviceStatusSharingContacts;
            return this;
        }

        /**
         * Set whether the user has enabled NR advanced calling.
         *
         * @param isNrAdvancedCallingEnabled Whether the user has enabled NR advanced calling.
         * @return The builder.
         */
        @NonNull
        public Builder setNrAdvancedCallingEnabled(int isNrAdvancedCallingEnabled) {
            mIsNrAdvancedCallingEnabled = isNrAdvancedCallingEnabled;
            return this;
        }

        /**
         * Set the phone number retrieved from carrier.
         *
         * @param numberFromCarrier The phone number retrieved from carrier.
         * @return The builder.
         */
        @NonNull
        public Builder setNumberFromCarrier(@NonNull String numberFromCarrier) {
            Objects.requireNonNull(numberFromCarrier);
            mNumberFromCarrier = numberFromCarrier;
            return this;
        }

        /**
         * Set the phone number retrieved from IMS.
         *
         * @param numberFromIms The phone number retrieved from IMS.
         * @return The builder.
         */
        @NonNull
        public Builder setNumberFromIms(@NonNull String numberFromIms) {
            Objects.requireNonNull(numberFromIms);
            mNumberFromIms = numberFromIms;
            return this;
        }

        /**
         * Set the port index of the Uicc card.
         *
         * @param portIndex The port index of the Uicc card.
         * @return The builder.
         */
        @NonNull
        public Builder setPortIndex(int portIndex) {
            mPortIndex = portIndex;
            return this;
        }

        /**
         * Set subscription's preferred usage setting.
         *
         * @param usageSetting Subscription's preferred usage setting.
         * @return The builder.
         */
        @NonNull
        public Builder setUsageSetting(@UsageSetting int usageSetting) {
            mUsageSetting = usageSetting;
            return this;
        }

        /**
         * Set last used TP message reference.
         *
         * @param lastUsedTPMessageReference Last used TP message reference.
         * @return The builder.
         */
        @NonNull
        public Builder setLastUsedTPMessageReference(
                int lastUsedTPMessageReference) {
            mLastUsedTPMessageReference = lastUsedTPMessageReference;
            return this;
        }

        /**
         * Set the user id associated with this subscription.
         *
         * @param userId The user id associated with this subscription.
         * @return The builder.
         */
        @NonNull
        public Builder setUserId(@UserIdInt int userId) {
            mUserId = userId;
            return this;
        }

        /**
         * Set whether satellite is enabled or not.
         * @param isSatelliteEnabled {@code 1} if satellite is enabled.
         * @return The builder.
         */
        @NonNull
        public Builder setSatelliteEnabled(int isSatelliteEnabled) {
            mIsSatelliteEnabled = isSatelliteEnabled;
            return this;
        }

        // Below are the fields that do not exist in the SimInfo table.
        /**
         * Set the card ID of the SIM card which contains the subscription.
         *
         * @param cardId The card ID of the SIM card which contains the subscription.
         * @return The builder.
         */
        @NonNull
        public Builder setCardId(int cardId) {
            mCardId = cardId;
            return this;
        }

        /**
         * Whether group of the subscription is disabled. This is only useful if it's a grouped
         * opportunistic subscription. In this case, if all primary (non-opportunistic)
         * subscriptions in the group are deactivated (unplugged pSIM or deactivated eSIM profile),
         * we should disable this opportunistic subscription.
         *
         * @param isGroupDisabled {@code 1} if group of the subscription is disabled.
         * @return The builder.
         */
        @NonNull
        public Builder setGroupDisabled(boolean isGroupDisabled) {
            mIsGroupDisabled = isGroupDisabled;
            return this;
        }

        /**
         * Build the {@link SubscriptionInfoInternal}.
         *
         * @return The {@link SubscriptionInfoInternal} instance.
         */
        public SubscriptionInfoInternal build() {
            return new SubscriptionInfoInternal(this);
        }
    }
}
