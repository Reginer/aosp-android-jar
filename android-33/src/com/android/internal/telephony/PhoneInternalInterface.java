/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.telephony;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.WorkSource;
import android.telecom.VideoProfile;
import android.telephony.Annotation.DataActivityType;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.emergency.EmergencyNumber;

import com.android.internal.telephony.PhoneConstants.DataState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.function.Consumer;

/**
 * Internal interface used to control the phone; SDK developers cannot
 * obtain this interface.
 *
 * {@hide}
 *
 */
public interface PhoneInternalInterface {

    /** used to enable additional debug messages */
    static final boolean DEBUG_PHONE = true;

    public enum DataActivityState {
        /**
         * The state of a data activity.
         * <ul>
         * <li>NONE = No traffic</li>
         * <li>DATAIN = Receiving IP ppp traffic</li>
         * <li>DATAOUT = Sending IP ppp traffic</li>
         * <li>DATAINANDOUT = Both receiving and sending IP ppp traffic</li>
         * <li>DORMANT = The data connection is still active,
                                     but physical link is down</li>
         * </ul>
         */
        @UnsupportedAppUsage
        NONE, DATAIN, DATAOUT, DATAINANDOUT, DORMANT;
    }

    enum SuppService {
      UNKNOWN, SWITCH, SEPARATE, TRANSFER, CONFERENCE, REJECT, HANGUP, RESUME, HOLD;
    }

    /**
     * Arguments that control behavior of dialing a call.
     */
    public static class DialArgs {
        public static class Builder<T extends Builder<T>> {
            protected UUSInfo mUusInfo;
            protected int mClirMode = CommandsInterface.CLIR_DEFAULT;
            protected boolean mIsEmergency;
            protected int mVideoState = VideoProfile.STATE_AUDIO_ONLY;
            protected Bundle mIntentExtras;
            protected int mEccCategory = EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED;

            public static DialArgs.Builder from(DialArgs dialArgs) {
                return new DialArgs.Builder()
                    .setUusInfo(dialArgs.uusInfo)
                    .setClirMode(dialArgs.clirMode)
                    .setIsEmergency(dialArgs.isEmergency)
                    .setVideoState(dialArgs.videoState)
                    .setIntentExtras(dialArgs.intentExtras)
                    .setEccCategory(dialArgs.eccCategory);
            }

            public T setUusInfo(UUSInfo uusInfo) {
                mUusInfo = uusInfo;
                return (T) this;
            }

            public T setClirMode(int clirMode) {
                mClirMode = clirMode;
                return (T) this;
            }

            public T setIsEmergency(boolean isEmergency) {
                mIsEmergency = isEmergency;
                return (T) this;
            }

            public T setVideoState(int videoState) {
                mVideoState = videoState;
                return (T) this;
            }

            public T setIntentExtras(Bundle intentExtras) {
                this.mIntentExtras = intentExtras;
                return (T) this;
            }

            public T setEccCategory(int eccCategory) {
                mEccCategory = eccCategory;
                return (T) this;
            }

            public PhoneInternalInterface.DialArgs build() {
                return new DialArgs(this);
            }
        }

        /** The UUSInfo */
        public final UUSInfo uusInfo;

        /** The CLIR mode to use */
        public final int clirMode;

        /** Indicates emergency call */
        public final boolean isEmergency;

        /** The desired video state for the connection. */
        public final int videoState;

        /** The extras from the original CALL intent. */
        public final Bundle intentExtras;

        /** Indicates emergency service category */
        public final int eccCategory;

        protected DialArgs(Builder b) {
            this.uusInfo = b.mUusInfo;
            this.clirMode = b.mClirMode;
            this.isEmergency = b.mIsEmergency;
            this.videoState = b.mVideoState;
            this.intentExtras = b.mIntentExtras;
            this.eccCategory = b.mEccCategory;
        }
    }

    // "Features" accessible through the connectivity manager
    static final String FEATURE_ENABLE_MMS = "enableMMS";
    static final String FEATURE_ENABLE_SUPL = "enableSUPL";
    static final String FEATURE_ENABLE_DUN = "enableDUN";
    static final String FEATURE_ENABLE_HIPRI = "enableHIPRI";
    static final String FEATURE_ENABLE_DUN_ALWAYS = "enableDUNAlways";
    static final String FEATURE_ENABLE_FOTA = "enableFOTA";
    static final String FEATURE_ENABLE_IMS = "enableIMS";
    static final String FEATURE_ENABLE_CBS = "enableCBS";
    static final String FEATURE_ENABLE_EMERGENCY = "enableEmergency";

    /**
     * Optional reasons for disconnect and connect
     */
    static final String REASON_ROAMING_ON = "roamingOn";
    static final String REASON_ROAMING_OFF = "roamingOff";
    static final String REASON_DATA_DISABLED_INTERNAL = "dataDisabledInternal";
    static final String REASON_DATA_ENABLED = "dataEnabled";
    static final String REASON_DATA_ATTACHED = "dataAttached";
    static final String REASON_DATA_DETACHED = "dataDetached";
    static final String REASON_CDMA_DATA_ATTACHED = "cdmaDataAttached";
    static final String REASON_CDMA_DATA_DETACHED = "cdmaDataDetached";
    static final String REASON_APN_CHANGED = "apnChanged";
    static final String REASON_APN_SWITCHED = "apnSwitched";
    static final String REASON_APN_FAILED = "apnFailed";
    static final String REASON_RESTORE_DEFAULT_APN = "restoreDefaultApn";
    static final String REASON_RADIO_TURNED_OFF = "radioTurnedOff";
    static final String REASON_PDP_RESET = "pdpReset";
    static final String REASON_VOICE_CALL_ENDED = "2GVoiceCallEnded";
    static final String REASON_VOICE_CALL_STARTED = "2GVoiceCallStarted";
    static final String REASON_PS_RESTRICT_ENABLED = "psRestrictEnabled";
    static final String REASON_PS_RESTRICT_DISABLED = "psRestrictDisabled";
    static final String REASON_SIM_LOADED = "simLoaded";
    static final String REASON_NW_TYPE_CHANGED = "nwTypeChanged";
    static final String REASON_DATA_DEPENDENCY_MET = "dependencyMet";
    static final String REASON_DATA_DEPENDENCY_UNMET = "dependencyUnmet";
    static final String REASON_LOST_DATA_CONNECTION = "lostDataConnection";
    static final String REASON_CONNECTED = "connected";
    static final String REASON_SINGLE_PDN_ARBITRATION = "SinglePdnArbitration";
    static final String REASON_DATA_SPECIFIC_DISABLED = "specificDisabled";
    static final String REASON_SIM_NOT_READY = "simNotReady";
    static final String REASON_IWLAN_AVAILABLE = "iwlanAvailable";
    static final String REASON_CARRIER_CHANGE = "carrierChange";
    static final String REASON_CARRIER_ACTION_DISABLE_METERED_APN =
            "carrierActionDisableMeteredApn";
    static final String REASON_CSS_INDICATOR_CHANGED = "cssIndicatorChanged";
    static final String REASON_RELEASED_BY_CONNECTIVITY_SERVICE = "releasedByConnectivityService";
    static final String REASON_DATA_ENABLED_OVERRIDE = "dataEnabledOverride";
    static final String REASON_IWLAN_DATA_SERVICE_DIED = "iwlanDataServiceDied";
    static final String REASON_VCN_REQUESTED_TEARDOWN = "vcnRequestedTeardown";
    static final String REASON_DATA_UNTHROTTLED = "dataUnthrottled";
    static final String REASON_TRAFFIC_DESCRIPTORS_UPDATED = "trafficDescriptorsUpdated";

    // Reasons for Radio being powered off
    int RADIO_POWER_REASON_USER = 0;
    int RADIO_POWER_REASON_THERMAL = 1;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"RADIO_POWER_REASON_"},
        value = {
            RADIO_POWER_REASON_USER,
            RADIO_POWER_REASON_THERMAL})
    public @interface RadioPowerReason {}

    // Used for band mode selection methods
    static final int BM_UNSPECIFIED = RILConstants.BAND_MODE_UNSPECIFIED; // automatic
    static final int BM_EURO_BAND   = RILConstants.BAND_MODE_EURO;
    static final int BM_US_BAND     = RILConstants.BAND_MODE_USA;
    static final int BM_JPN_BAND    = RILConstants.BAND_MODE_JPN;
    static final int BM_AUS_BAND    = RILConstants.BAND_MODE_AUS;
    static final int BM_AUS2_BAND   = RILConstants.BAND_MODE_AUS_2;
    static final int BM_CELL_800    = RILConstants.BAND_MODE_CELL_800;
    static final int BM_PCS         = RILConstants.BAND_MODE_PCS;
    static final int BM_JTACS       = RILConstants.BAND_MODE_JTACS;
    static final int BM_KOREA_PCS   = RILConstants.BAND_MODE_KOREA_PCS;
    static final int BM_4_450M      = RILConstants.BAND_MODE_5_450M;
    static final int BM_IMT2000     = RILConstants.BAND_MODE_IMT2000;
    static final int BM_7_700M2     = RILConstants.BAND_MODE_7_700M_2;
    static final int BM_8_1800M     = RILConstants.BAND_MODE_8_1800M;
    static final int BM_9_900M      = RILConstants.BAND_MODE_9_900M;
    static final int BM_10_800M_2   = RILConstants.BAND_MODE_10_800M_2;
    static final int BM_EURO_PAMR   = RILConstants.BAND_MODE_EURO_PAMR_400M;
    static final int BM_AWS         = RILConstants.BAND_MODE_AWS;
    static final int BM_US_2500M    = RILConstants.BAND_MODE_USA_2500M;
    static final int BM_NUM_BAND_MODES = 19; //Total number of band modes

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int PREFERRED_NT_MODE                = RILConstants.PREFERRED_NETWORK_MODE;

    // Used for CDMA roaming mode
    // Home Networks only, as defined in PRL
    int CDMA_RM_HOME        = TelephonyManager.CDMA_ROAMING_MODE_HOME;
    // Roaming an Affiliated networks, as defined in PRL
    int CDMA_RM_AFFILIATED  = TelephonyManager.CDMA_ROAMING_MODE_AFFILIATED;
    // Roaming on Any Network, as defined in PRL
    int CDMA_RM_ANY         = TelephonyManager.CDMA_ROAMING_MODE_ANY;

    // Used for CDMA subscription mode
    // Unknown
    static final int CDMA_SUBSCRIPTION_UNKNOWN  = TelephonyManager.CDMA_SUBSCRIPTION_UNKNOWN;
    // RUIM/SIM (default)
    static final int CDMA_SUBSCRIPTION_RUIM_SIM = TelephonyManager.CDMA_SUBSCRIPTION_RUIM_SIM;
    // NV -> non-volatile memory
    static final int CDMA_SUBSCRIPTION_NV       = TelephonyManager.CDMA_SUBSCRIPTION_NV;

    static final int PREFERRED_CDMA_SUBSCRIPTION = CDMA_SUBSCRIPTION_RUIM_SIM;

    static final int TTY_MODE_OFF = 0;
    static final int TTY_MODE_FULL = 1;
    static final int TTY_MODE_HCO = 2;
    static final int TTY_MODE_VCO = 3;

     /**
     * CDMA OTA PROVISION STATUS, the same as RIL_CDMA_OTA_Status in ril.h
     */

    public static final int CDMA_OTA_PROVISION_STATUS_SPL_UNLOCKED = 0;
    public static final int CDMA_OTA_PROVISION_STATUS_SPC_RETRIES_EXCEEDED = 1;
    public static final int CDMA_OTA_PROVISION_STATUS_A_KEY_EXCHANGED = 2;
    public static final int CDMA_OTA_PROVISION_STATUS_SSD_UPDATED = 3;
    public static final int CDMA_OTA_PROVISION_STATUS_NAM_DOWNLOADED = 4;
    public static final int CDMA_OTA_PROVISION_STATUS_MDN_DOWNLOADED = 5;
    public static final int CDMA_OTA_PROVISION_STATUS_IMSI_DOWNLOADED = 6;
    public static final int CDMA_OTA_PROVISION_STATUS_PRL_DOWNLOADED = 7;
    public static final int CDMA_OTA_PROVISION_STATUS_COMMITTED = 8;
    public static final int CDMA_OTA_PROVISION_STATUS_OTAPA_STARTED = 9;
    public static final int CDMA_OTA_PROVISION_STATUS_OTAPA_STOPPED = 10;
    public static final int CDMA_OTA_PROVISION_STATUS_OTAPA_ABORTED = 11;


    /**
     * Get the current ServiceState. Use
     * <code>registerForServiceStateChanged</code> to be informed of
     * updates.
     */
    ServiceState getServiceState();

    /**
     * Get the current DataState. No change notification exists at this
     * interface -- use
     * {@link android.telephony.PhoneStateListener} instead.
     * @param apnType specify for which apn to get connection state info.
     */
    DataState getDataConnectionState(String apnType);

    /**
     * Get the current Precise DataState. No change notification exists at this
     * interface -- use
     * {@link android.telephony.PhoneStateListener} instead.
     *
     * @param apnType specify for which apn to get connection state info.
     * @return the PreciseDataConnectionState for the data connection supporting apnType
     */
    PreciseDataConnectionState getPreciseDataConnectionState(String apnType);

    /**
     * Get the current data activity. No change notification exists at this
     * interface.
     */
    @DataActivityType int getDataActivityState();

    /**
     * Returns a list of MMI codes that are pending. (They have initiated
     * but have not yet completed).
     * Presently there is only ever one.
     * Use <code>registerForMmiInitiate</code>
     * and <code>registerForMmiComplete</code> for change notification.
     */
    public List<? extends MmiCode> getPendingMmiCodes();

    /**
     * Sends user response to a USSD REQUEST message.  An MmiCode instance
     * representing this response is sent to handlers registered with
     * registerForMmiInitiate.
     *
     * @param ussdMessge    Message to send in the response.
     */
    public void sendUssdResponse(String ussdMessge);

    /**
     * Register for Supplementary Service notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSuppServiceNotification(Handler h, int what, Object obj);

    /**
     * Unregisters for Supplementary Service notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForSuppServiceNotification(Handler h);

    /**
     * Answers a ringing or waiting call. Active calls, if any, go on hold.
     * Answering occurs asynchronously, and final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @param videoState The video state in which to answer the call.
     * @exception CallStateException when no call is ringing or waiting
     */
    void acceptCall(int videoState) throws CallStateException;

    /**
     * Reject (ignore) a ringing call. In GSM, this means UDUB
     * (User Determined User Busy). Reject occurs asynchronously,
     * and final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException when no call is ringing or waiting
     */
    void rejectCall() throws CallStateException;

    /**
     * Places any active calls on hold, and makes any held calls
     *  active. Switch occurs asynchronously and may fail.
     * Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if a call is ringing, waiting, or
     * dialing/alerting. In these cases, this operation may not be performed.
     */
    void switchHoldingAndActive() throws CallStateException;

    /**
     * Whether or not the phone can conference in the current phone
     * state--that is, one call holding and one call active.
     * @return true if the phone can conference; false otherwise.
     */
    boolean canConference();

    /**
     * Conferences holding and active. Conference occurs asynchronously
     * and may fail. Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if canConference() would return false.
     * In these cases, this operation may not be performed.
     */
    void conference() throws CallStateException;

    /**
     * Whether or not the phone can do explicit call transfer in the current
     * phone state--that is, one call holding and one call active.
     * @return true if the phone can do explicit call transfer; false otherwise.
     */
    boolean canTransfer();

    /**
     * Connects the two calls and disconnects the subscriber from both calls
     * Explicit Call Transfer occurs asynchronously
     * and may fail. Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if canTransfer() would return false.
     * In these cases, this operation may not be performed.
     */
    void explicitCallTransfer() throws CallStateException;

    /**
     * Clears all DISCONNECTED connections from Call connection lists.
     * Calls that were in the DISCONNECTED state become idle. This occurs
     * synchronously.
     */
    void clearDisconnected();

    /**
     * Gets the foreground call object, which represents all connections that
     * are dialing or active (all connections
     * that have their audio path connected).<p>
     *
     * The foreground call is a singleton object. It is constant for the life
     * of this phone. It is never null.<p>
     *
     * The foreground call will only ever be in one of these states:
     * IDLE, ACTIVE, DIALING, ALERTING, or DISCONNECTED.
     *
     * State change notification is available via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     */
    Call getForegroundCall();

    /**
     * Gets the background call object, which represents all connections that
     * are holding (all connections that have been accepted or connected, but
     * do not have their audio path connected). <p>
     *
     * The background call is a singleton object. It is constant for the life
     * of this phone object . It is never null.<p>
     *
     * The background call will only ever be in one of these states:
     * IDLE, HOLDING or DISCONNECTED.
     *
     * State change notification is available via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     */
    Call getBackgroundCall();

    /**
     * Gets the ringing call object, which represents an incoming
     * connection (if present) that is pending answer/accept. (This connection
     * may be RINGING or WAITING, and there may be only one.)<p>

     * The ringing call is a singleton object. It is constant for the life
     * of this phone. It is never null.<p>
     *
     * The ringing call will only ever be in one of these states:
     * IDLE, INCOMING, WAITING or DISCONNECTED.
     *
     * State change notification is available via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     */
    Call getRingingCall();

    /**
     * Initiate a new voice connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @param dialString The dial string.
     * @param dialArgs Parameters to perform the dial with.
     * @param chosenPhone The Phone (either GsmCdmaPhone or ImsPhone) that has been chosen to dial
     *                    this number. This is used for any setup that should occur before dial
     *                    actually occurs.
     * @exception CallStateException if a new outgoing call is not currently
     *                possible because no more call slots exist or a call exists
     *                that is dialing, alerting, ringing, or waiting. Other
     *                errors are handled asynchronously.
     */
    Connection dial(String dialString, @NonNull DialArgs dialArgs,
            Consumer<Phone> chosenPhone) throws CallStateException;

    /**
     * Initiate a new voice connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @param dialString The dial string.
     * @param dialArgs Parameters to perform the dial with.
     * @exception CallStateException if a new outgoing call is not currently
     *                possible because no more call slots exist or a call exists
     *                that is dialing, alerting, ringing, or waiting. Other
     *                errors are handled asynchronously.
     */
    default Connection dial(String dialString, @NonNull DialArgs dialArgs)
            throws CallStateException {
        return dial(dialString, dialArgs, (phone) -> {});
    }

    /**
     * Initiate a new conference connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @param participantsToDial The participants to dial.
     * @param dialArgs Parameters to perform the start conference with.
     * @exception CallStateException if a new outgoing call is not currently
     *                possible because no more call slots exist or a call exists
     *                that is dialing, alerting, ringing, or waiting. Other
     *                errors are handled asynchronously.
     */
    Connection startConference(String[] participantsToDial, @NonNull DialArgs dialArgs)
            throws CallStateException;

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated
     * without SEND (so <code>dial</code> is not appropriate).
     *
     * @param dialString the MMI command to be executed.
     * @return true if MMI command is executed.
     */
    boolean handlePinMmi(String dialString);

    /**
     * Handles USSD commands
     *
     * @param ussdRequest the USSD command to be executed.
     * @param wrappedCallback receives the callback result.
     */
    boolean handleUssdRequest(String ussdRequest, ResultReceiver wrappedCallback)
            throws CallStateException;

    /**
     * Handles in-call MMI commands. While in a call, or while receiving a
     * call, use this to execute MMI commands.
     * see 3GPP 20.030, section 6.5.5.1 for specs on the allowed MMI commands.
     *
     * @param command the MMI command to be executed.
     * @return true if the MMI command is executed.
     * @throws CallStateException
     */
    boolean handleInCallMmiCommands(String command) throws CallStateException;

    /**
     * Play a DTMF tone on the active call. Ignored if there is no active call.
     * @param c should be one of 0-9, '*' or '#'. Other values will be
     * silently ignored.
     */
    void sendDtmf(char c);

    /**
     * Start to paly a DTMF tone on the active call. Ignored if there is no active call
     * or there is a playing DTMF tone.
     * @param c should be one of 0-9, '*' or '#'. Other values will be
     * silently ignored.
     */
    void startDtmf(char c);

    /**
     * Stop the playing DTMF tone. Ignored if there is no playing DTMF
     * tone or no active call.
     */
    void stopDtmf();

    /**
     * Sets the radio power on/off state (off is sometimes
     * called "airplane mode"). Current state can be gotten via
     * {@link #getServiceState()}.{@link
     * android.telephony.ServiceState#getState() getState()}.
     * <strong>Note: </strong>This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the
     * request is complete. This will set the reason for radio power state as {@link
     * #RADIO_POWER_REASON_USER}. This will not guarantee that the requested radio power state will
     * actually be set. See {@link #setRadioPowerForReason(boolean, boolean, boolean, boolean, int)}
     * for details.
     *
     * @param power true means "on", false means "off".
     */
    default void setRadioPower(boolean power) {
        setRadioPower(power, false, false, false);
    }

    /**
     * Sets the radio power on for a test emergency number.
     *
     * @param isSelectedPhoneForEmergencyCall true means this phone / modem is selected to place
     *                                  emergency call after turning power on.
     */
    default void setRadioPowerOnForTestEmergencyCall(boolean isSelectedPhoneForEmergencyCall) {}

    /**
     * Sets the radio power on/off state with option to specify whether it's for emergency call
     * (off is sometimes called "airplane mode"). Current state can be gotten via
     * {@link #getServiceState()}.{@link
     * android.telephony.ServiceState#getState() getState()}.
     * <strong>Note: </strong>This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the
     * request is complete. This will set the reason for radio power state as {@link
     * #RADIO_POWER_REASON_USER}. This will not guarantee that the requested radio power state will
     * actually be set. See {@link #setRadioPowerForReason(boolean, boolean, boolean, boolean, int)}
     * for details.
     *
     * @param power true means "on", false means "off".
     * @param forEmergencyCall true means the purpose of turning radio power on is for emergency
     *                         call. No effect if power is set false.
     * @param isSelectedPhoneForEmergencyCall true means this phone / modem is selected to place
     *                                  emergency call after turning power on. No effect if power
     *                                  or forEmergency is set false.
     * @param forceApply true means always call setRadioPower HAL API without checking against
     *                   current radio power state. It's needed when: radio was powered on into
     *                   emergency call mode, to exit from that mode, we set radio
     *                   power on again with forEmergencyCall being false.
     */
    default void setRadioPower(boolean power, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply) {
        setRadioPowerForReason(power, forEmergencyCall, isSelectedPhoneForEmergencyCall, forceApply,
                RADIO_POWER_REASON_USER);
    }

    /**
     * Sets the radio power on/off state (off is sometimes
     * called "airplane mode") for the specified reason, if possible. Current state can be gotten
     * via {@link #getServiceState()}.{@link
     * android.telephony.ServiceState#getState() getState()}.
     * <strong>Note: </strong>This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the
     * request is complete. Radio power will not be set if it is currently off for a reason other
     * than the reason for which it is being turned on. However, if forEmergency call is {@code
     * true}, it will forcefully turn radio power on.
     *
     * @param power true means "on", false means "off".
     * @param reason RadioPowerReason constant defining the reason why the radio power was set.
     */
    default void setRadioPowerForReason(boolean power, @RadioPowerReason int reason) {
        setRadioPowerForReason(power, false, false, false, reason);
    }

    /**
     * Sets the radio power on/off state with option to specify whether it's for emergency call
     * (off is sometimes called "airplane mode") and option to set the reason for setting the power
     * state. Current state can be gotten via {@link #getServiceState()}.
     * {@link android.telephony.ServiceState#getState() getState()}.
     * <strong>Note: </strong>This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the
     * request is complete. Radio power will not be set if it is currently off for a reason other
     * than the reason for which it is being turned on. However, if forEmergency call is {@code
     * true}, it will forcefully turn radio power on.
     *
     * @param power true means "on", false means "off".
     * @param forEmergencyCall true means the purpose of turning radio power on is for emergency
     *                         call. No effect if power is set false.
     * @param isSelectedPhoneForEmergencyCall true means this phone / modem is selected to place
     *                                  emergency call after turning power on. No effect if power
     *                                  or forEmergency is set false.
     * @param forceApply true means always call setRadioPower HAL API without checking against
     *                   current radio power state. It's needed when: radio was powered on into
     *                   emergency call mode, to exit from that mode, we set radio
     *                   power on again with forEmergencyCall being false.
     * @param reason RadioPowerReason constant defining the reason why the radio power was set.
     */
    default void setRadioPowerForReason(boolean power, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply,
            @RadioPowerReason int reason) {}

    /**
     * Get the line 1 phone number (MSISDN). For CDMA phones, the MDN is returned
     * and {@link #getMsisdn()} will return the MSISDN on CDMA/LTE phones.<p>
     *
     * @return phone number. May return null if not
     * available or the SIM is not ready
     */
    String getLine1Number();

    /**
     * Returns the alpha tag associated with the msisdn number.
     * If there is no alpha tag associated or the record is not yet available,
     * returns a default localized string. <p>
     */
    String getLine1AlphaTag();

    /**
     * Sets the MSISDN phone number in the SIM card.
     *
     * @param alphaTag the alpha tag associated with the MSISDN phone number
     *        (see getMsisdnAlphaTag)
     * @param number the new MSISDN phone number to be set on the SIM.
     * @param onComplete a callback message when the action is completed.
     *
     * @return true if req is sent, false otherwise. If req is not sent there will be no response,
     * that is, onComplete will never be sent.
     */
    boolean setLine1Number(String alphaTag, String number, Message onComplete);

    /**
     * Get the voice mail access phone number. Typically dialed when the
     * user holds the "1" key in the phone app. May return null if not
     * available or the SIM is not ready.<p>
     */
    String getVoiceMailNumber();

    /**
     * Returns the alpha tag associated with the voice mail number.
     * If there is no alpha tag associated or the record is not yet available,
     * returns a default localized string. <p>
     *
     * Please use this value instead of some other localized string when
     * showing a name for this number in the UI. For example, call log
     * entries should show this alpha tag. <p>
     *
     * Usage of this alpha tag in the UI is a common carrier requirement.
     */
    String getVoiceMailAlphaTag();

    /**
     * setVoiceMailNumber
     * sets the voicemail number in the SIM card.
     *
     * @param alphaTag the alpha tag associated with the voice mail number
     *        (see getVoiceMailAlphaTag)
     * @param voiceMailNumber the new voicemail number to be set on the SIM.
     * @param onComplete a callback message when the action is completed.
     */
    void setVoiceMailNumber(String alphaTag,
                            String voiceMailNumber,
                            Message onComplete);

    /**
     * getCallForwardingOptions
     * gets a call forwarding option for SERVICE_CLASS_VOICE. The return value of
     * ((AsyncResult)onComplete.obj) is an array of CallForwardInfo.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CallForwardInfo for details.
     */
    void getCallForwardingOption(int commandInterfaceCFReason,
                                  Message onComplete);

    /**
     * getCallForwardingOptions
     * gets a call forwarding option. The return value of
     * ((AsyncResult)onComplete.obj) is an array of CallForwardInfo.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param serviceClass is a sum of SERVICE_CLASS_* as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CallForwardInfo for details.
     */
    void getCallForwardingOption(int commandInterfaceCFReason, int serviceClass,
                                  Message onComplete);

    /**
     * setCallForwardingOptions
     * sets a call forwarding option for SERVICE_CLASS_VOICE.
     *
     * @param commandInterfaceCFAction is one of the valid call forwarding
     *        CF_ACTIONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param dialingNumber is the target phone number to forward calls to
     * @param timerSeconds is used by CFNRy to indicate the timeout before
     *        forwarding is attempted.
     * @param onComplete a callback message when the action is completed.
     */
    void setCallForwardingOption(int commandInterfaceCFAction,
                                 int commandInterfaceCFReason,
                                 String dialingNumber,
                                 int timerSeconds,
                                 Message onComplete);

    /**
     * setCallForwardingOptions
     * sets a call forwarding option.
     *
     * @param commandInterfaceCFAction is one of the valid call forwarding
     *        CF_ACTIONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param dialingNumber is the target phone number to forward calls to
     * @param serviceClass is a sum of SERVICE_CLASS_* as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param timerSeconds is used by CFNRy to indicate the timeout before
     *        forwarding is attempted.
     * @param onComplete a callback message when the action is completed.
     */
    void setCallForwardingOption(int commandInterfaceCFAction,
                                 int commandInterfaceCFReason,
                                 String dialingNumber,
                                 int serviceClass,
                                 int timerSeconds,
                                 Message onComplete);

    /**
     * Gets a call barring option. The return value of ((AsyncResult) onComplete.obj) will be an
     * Integer representing the sum of enabled serivice classes (sum of SERVICE_CLASS_*)
     *
     * @param facility is one of CB_FACILTY_*
     * @param password is password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param onComplete is callback message when the action is completed.
     */
    public void getCallBarring(String facility,
            String password,
            Message onComplete,
            int serviceClass);

    /**
     * Sets a call barring option.
     *
     * @param facility is one of CB_FACILTY_*
     * @param lockState is true means lock, false means unlock
     * @param password is password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param onComplete is callback message when the action is completed.
     */
    public void setCallBarring(String facility,
            boolean lockState,
            String password,
            Message onComplete,
            int serviceClass);

    /**
     * getOutgoingCallerIdDisplay
     * gets outgoing caller id display. The return value of
     * ((AsyncResult)onComplete.obj) is an array of int, with a length of 2.
     *
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CommandsInterface#getCLIR for details.
     */
    void getOutgoingCallerIdDisplay(Message onComplete);

    /**
     * setOutgoingCallerIdDisplay
     * sets a call forwarding option.
     *
     * @param commandInterfaceCLIRMode is one of the valid call CLIR
     *        modes, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface./code>
     * @param onComplete a callback message when the action is completed.
     */
    void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode,
                                    Message onComplete);

    /**
     * getCallWaiting
     * gets call waiting activation state. The return value of
     * ((AsyncResult)onComplete.obj) is an array of int, with a length of 1.
     *
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CommandsInterface#queryCallWaiting for details.
     */
    void getCallWaiting(Message onComplete);

    /**
     * setCallWaiting
     * sets a call forwarding option.
     *
     * @param enable is a boolean representing the state that you are
     *        requesting, true for enabled, false for disabled.
     * @param onComplete a callback message when the action is completed.
     */
    void setCallWaiting(boolean enable, Message onComplete);

    /**
     * Scan available networks. This method is asynchronous; .
     * On completion, <code>response.obj</code> is set to an AsyncResult with
     * one of the following members:.<p>
     *<ul>
     * <li><code>response.obj.result</code> will be a <code>List</code> of
     * <code>OperatorInfo</code> objects, or</li>
     * <li><code>response.obj.exception</code> will be set with an exception
     * on failure.</li>
     * </ul>
     */
    void getAvailableNetworks(Message response);

    /**
     * Start a network scan. This method is asynchronous; .
     * On completion, <code>response.obj</code> is set to an AsyncResult with
     * one of the following members:.<p>
     * <ul>
     * <li><code>response.obj.result</code> will be a <code>NetworkScanResult</code> object, or</li>
     * <li><code>response.obj.exception</code> will be set with an exception
     * on failure.</li>
     * </ul>
     */
    void startNetworkScan(NetworkScanRequest nsr, Message response);

    /**
     * Stop ongoing network scan. This method is asynchronous; .
     * On completion, <code>response.obj</code> is set to an AsyncResult with
     * one of the following members:.<p>
     * <ul>
     * <li><code>response.obj.result</code> will be a <code>NetworkScanResult</code> object, or</li>
     * <li><code>response.obj.exception</code> will be set with an exception
     * on failure.</li>
     * </ul>
     */
    void stopNetworkScan(Message response);

    /**
     * Mutes or unmutes the microphone for the active call. The microphone
     * is automatically unmuted if a call is answered, dialed, or resumed
     * from a holding state.
     *
     * @param muted true to mute the microphone,
     * false to activate the microphone.
     */

    void setMute(boolean muted);

    /**
     * Gets current mute status. Use
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}
     * as a change notifcation, although presently phone state changed is not
     * fired when setMute() is called.
     *
     * @return true is muting, false is unmuting
     */
    boolean getMute();

    /**
     * Update the ServiceState CellLocation for current network registration.
     *
     * @param workSource the caller to be billed for work.
     */
    default void updateServiceLocation(WorkSource workSource) {}

    /**
     * To be deleted.
     */
    default void updateServiceLocation() {}

    /**
     * Enable location update notifications.
     */
    void enableLocationUpdates();

    /**
     * Disable location update notifications.
     */
    void disableLocationUpdates();

    /**
     * @return true if enable data connection on roaming
     */
    boolean getDataRoamingEnabled();

    /**
     * @param enable set true if enable data connection on roaming
     */
    void setDataRoamingEnabled(boolean enable);

    /**
     * @return true if user has enabled data
     */
    boolean isUserDataEnabled();

    /**
     * Retrieves the unique device ID, e.g., IMEI for GSM phones and MEID for CDMA phones.
     */
    String getDeviceId();

    /**
     * Retrieves the software version number for the device, e.g., IMEI/SV
     * for GSM phones.
     */
    String getDeviceSvn();

    /**
     * Retrieves the unique subscriber ID, e.g., IMSI for GSM phones.
     */
    String getSubscriberId();

    /**
     * Retrieves the Group Identifier Level1 for GSM phones.
     */
    String getGroupIdLevel1();

    /**
     * Retrieves the Group Identifier Level2 for phones.
     */
    String getGroupIdLevel2();

    /* CDMA support methods */

    /**
     * Retrieves the ESN for CDMA phones.
     */
    String getEsn();

    /**
     * Retrieves MEID for CDMA phones.
     */
    String getMeid();

    /**
     * Retrieves IMEI for phones. Returns null if IMEI is not set.
     */
    String getImei();

    /**
     * Retrieves the IccPhoneBookInterfaceManager of the Phone
     */
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager();

    /**
     * Activate or deactivate cell broadcast SMS.
     *
     * @param activate
     *            0 = activate, 1 = deactivate
     * @param response
     *            Callback message is empty on completion
     */
    void activateCellBroadcastSms(int activate, Message response);

    /**
     * Query the current configuration of cdma cell broadcast SMS.
     *
     * @param response
     *            Callback message is empty on completion
     */
    void getCellBroadcastSmsConfig(Message response);

    /**
     * Configure cell broadcast SMS.
     *
     * TODO: Change the configValuesArray to a RIL_BroadcastSMSConfig
     *
     * @param response
     *            Callback message is empty on completion
     */
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response);

    /*
    * Sets the carrier information needed to encrypt the IMSI and IMPI.
    * @param imsiEncryptionInfo Carrier specific information that will be used to encrypt the
    *        IMSI and IMPI. This includes the Key type, the Public key
    *        {@link java.security.PublicKey} and the Key identifier.
    */
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo);

    /**
     * Returns Carrier specific information that will be used to encrypt the IMSI and IMPI.
     * @param keyType whether the key is being used for WLAN or ePDG.
     * @param fallback whether to fall back to the encryption key stored in carrier config
     * @return ImsiEncryptionInfo which includes the Key Type, the Public Key
     *        {@link java.security.PublicKey} and the Key Identifier.
     *        The keyIdentifier This is used by the server to help it locate the private key to
     *        decrypt the permanent identity.
     */
    ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int keyType, boolean fallback);

    /**
     * Resets the Carrier Keys, by deleting them from the database and sending a download intent.
     */
    public void resetCarrierKeysForImsiEncryption();

    /**
     *  Return the mobile provisioning url that is used to launch a browser to allow users to manage
     *  their mobile plan.
     */
    String getMobileProvisioningUrl();

    /**
     * Update the cellular usage setting if applicable.
     */
    boolean updateUsageSetting();

}
