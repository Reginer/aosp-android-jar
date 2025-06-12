/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.telephony.TelephonyManager.CAPABILITY_NR_DUAL_CONNECTIVITY_CONFIGURATION_AVAILABLE;
import static android.telephony.TelephonyManager.CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED;
import static android.telephony.TelephonyManager.CAPABILITY_SECONDARY_LINK_BANDWIDTH_VISIBLE;
import static android.telephony.TelephonyManager.CAPABILITY_SIM_PHONEBOOK_IN_MODEM;
import static android.telephony.TelephonyManager.CAPABILITY_SLICING_CONFIG_SUPPORTED;
import static android.telephony.TelephonyManager.CAPABILITY_THERMAL_MITIGATION_DATA_THROTTLING;
import static android.telephony.TelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK;

import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ALLOCATE_PDU_SESSION_ID;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ALLOW_DATA;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ANSWER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_BASEBAND_VERSION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CANCEL_EMERGENCY_NETWORK_SCAN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CANCEL_HANDOVER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CANCEL_USSD;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_BROADCAST_ACTIVATION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_BURST_DTMF;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_FLASH;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SEND_SMS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SEND_SMS_EXPECT_MORE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_SUBSCRIPTION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CHANGE_BARRING_PASSWORD;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CHANGE_SIM_PIN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CHANGE_SIM_PIN2;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_CONFERENCE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DATA_CALL_LIST;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DATA_REGISTRATION_STATE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DEACTIVATE_DATA_CALL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DELETE_SMS_ON_SIM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DEVICE_IDENTITY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DEVICE_IMEI;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DIAL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DTMF;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DTMF_START;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_DTMF_STOP;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_EMERGENCY_DIAL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENABLE_MODEM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENABLE_NR_DUAL_CONNECTIVITY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENABLE_UICC_APPLICATIONS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENABLE_VONR;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENTER_SIM_DEPERSONALIZATION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENTER_SIM_PIN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENTER_SIM_PIN2;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENTER_SIM_PUK;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ENTER_SIM_PUK2;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_EXIT_EMERGENCY_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_EXPLICIT_CALL_TRANSFER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_ACTIVITY_INFO;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_ALLOWED_CARRIERS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_ALLOWED_NETWORK_TYPES_BITMAP;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_BARRING_INFO;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_CELL_INFO_LIST;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_CLIR;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_CURRENT_CALLS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_DC_RT_INFO;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_HAL_DEVICE_CAPABILITIES;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_HARDWARE_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_IMEI;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_IMEISV;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_IMSI;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_LOCATION_PRIVACY_SETTING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_MODEM_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_MUTE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_NEIGHBORING_CELL_IDS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_PHONE_CAPABILITY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_RADIO_CAPABILITY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SIMULTANEOUS_CALLING_SUPPORT;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SIM_PHONEBOOK_CAPACITY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SIM_PHONEBOOK_RECORDS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SIM_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SLICING_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SLOT_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SMSC_ADDRESS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SYSTEM_SELECTION_CHANNELS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_UICC_APPLICATIONS_ENABLEMENT;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_USAGE_SETTING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GSM_BROADCAST_ACTIVATION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GSM_GET_BROADCAST_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GSM_SET_BROADCAST_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_HANGUP;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IMS_REGISTRATION_STATE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IMS_SEND_SMS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_ISIM_AUTHENTICATION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IS_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IS_N1_MODE_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IS_NR_DUAL_CONNECTIVITY_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IS_NULL_CIPHER_AND_INTEGRITY_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IS_SATELLITE_ENABLED_FOR_CARRIER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IS_SECURITY_ALGORITHMS_UPDATED_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_IS_VONR_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_LAST_CALL_FAIL_CAUSE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_NV_READ_ITEM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_NV_RESET_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_NV_WRITE_CDMA_PRL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_NV_WRITE_ITEM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_OEM_HOOK_RAW;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_OEM_HOOK_STRINGS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_OPERATOR;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_PULL_LCEDATA;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_AVAILABLE_NETWORKS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_CALL_FORWARD_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_CALL_WAITING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_CLIP;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_FACILITY_LOCK;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_QUERY_TTY_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_RADIO_POWER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_RELEASE_PDU_SESSION_ID;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_REPORT_SMS_MEMORY_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_RESET_RADIO;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SCREEN_STATE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEND_ANBR_QUERY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEND_DEVICE_STATE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEND_SMS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEND_SMS_EXPECT_MORE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEND_USSD;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SEPARATE_CONNECTION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SETUP_DATA_CALL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_ALLOWED_CARRIERS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_ALLOWED_NETWORK_TYPES_BITMAP;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_BAND_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_CALL_FORWARD;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_CALL_WAITING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_CLIR;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_DATA_PROFILE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_DATA_THROTTLING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_DC_RT_INFO_RATE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_EMERGENCY_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_FACILITY_LOCK;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_INITIAL_ATTACH_APN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_LINK_CAPACITY_REPORTING_CRITERIA;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_LOCATION_PRIVACY_SETTING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_LOCATION_UPDATES;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_MUTE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_N1_MODE_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_NULL_CIPHER_AND_INTEGRITY_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_PREFERRED_DATA_MODEM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_RADIO_CAPABILITY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SATELLITE_ENABLED_FOR_CARRIER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SATELLITE_PLMN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SECURITY_ALGORITHMS_UPDATED_ENABLED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SIM_CARD_POWER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SMSC_ADDRESS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SRVCC_CALL_INFO;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_SYSTEM_SELECTION_CHANNELS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_TTY_MODE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_UICC_SUBSCRIPTION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_UNSOLICITED_RESPONSE_FILTER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_USAGE_SETTING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SHUTDOWN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SIGNAL_STRENGTH;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SIM_AUTHENTICATION;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SIM_CLOSE_CHANNEL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SIM_IO;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SIM_OPEN_CHANNEL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SMS_ACKNOWLEDGE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_START_HANDOVER;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_START_IMS_TRAFFIC;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_START_KEEPALIVE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_START_LCE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_START_NETWORK_SCAN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STK_GET_PROFILE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STK_SET_PROFILE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STOP_IMS_TRAFFIC;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STOP_KEEPALIVE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STOP_LCE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_STOP_NETWORK_SCAN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SWITCH_DUAL_SIM_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_TRIGGER_EMERGENCY_NETWORK_SCAN;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_TRIGGER_EPS_FALLBACK;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_UDUB;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_UPDATE_IMS_CALL_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_UPDATE_IMS_REGISTRATION_INFO;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_UPDATE_SIM_PHONEBOOK_RECORD;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_VOICE_RADIO_TECH;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_VOICE_REGISTRATION_STATE;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_WRITE_SMS_TO_SIM;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_BARRING_INFO_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CALL_RING;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_CALL_WAITING;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_INFO_REC;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_OTA_PROVISION_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_PRL_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CELLULAR_IDENTIFIER_DISCLOSED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CELL_INFO_LIST;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CONNECTION_SETUP_FAILURE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_DATA_CALL_LIST_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_DC_RT_INFO_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_EMERGENCY_NETWORK_SCAN_RESULT;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_EMERGENCY_NUMBER_LIST;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_HARDWARE_CONFIG_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ICC_SLOT_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_IMEI_MAPPING_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_KEEPALIVE_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_LCEDATA_RECV;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_MODEM_RESTART;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_NETWORK_SCAN_RESULT;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_NITZ_TIME_RECEIVED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_NOTIFY_ANBR;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_OEM_HOOK_RAW;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ON_SS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ON_USSD;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_ON_USSD_REQUEST;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_PCO_DATA;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RADIO_CAPABILITY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_REGISTRATION_FAILED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESEND_INCALL_MUTE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_CDMA_NEW_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NETWORK_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESTRICTED_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RIL_CONNECTED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RINGBACK_TONE;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SECURITY_ALGORITHMS_UPDATED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIGNAL_STRENGTH;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIMULTANEOUS_CALLING_SUPPORT_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIM_REFRESH;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIM_SMS_STORAGE_FULL;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SLICING_CONFIG_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SRVCC_STATE_NOTIFY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_CALL_SETUP;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_CC_ALPHA_NOTIFY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_EVENT_NOTIFY;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_PROACTIVE_COMMAND;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_STK_SESSION_END;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SUPP_SVC_NOTIFICATION;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_TRIGGER_IMS_DEREGISTRATION;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UNTHROTTLE_APN;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_VOICE_RADIO_TECH_CHANGED;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.radio.data.SliceInfo;
import android.net.InetAddresses;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.os.SystemClock;
import android.service.carrier.CarrierIdentifier;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation;
import android.telephony.BarringInfo;
import android.telephony.CarrierInfo;
import android.telephony.CarrierRestrictionRules;
import android.telephony.CellConfigLte;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.CellularIdentifierDisclosure;
import android.telephony.ClosedSubscriberGroupInfo;
import android.telephony.DomainSelectionService;
import android.telephony.EmergencyRegistrationResult;
import android.telephony.LinkCapacityEstimate;
import android.telephony.ModemInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneCapability;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhysicalChannelConfig;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SecurityAlgorithmUpdate;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SignalThresholdInfo;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotMapping;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.DataService.DeactivateDataReason;
import android.telephony.data.DataService.SetupDataReason;
import android.telephony.data.EpsQos;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.NetworkSlicingConfig;
import android.telephony.data.NrQos;
import android.telephony.data.Qos;
import android.telephony.data.QosBearerFilter;
import android.telephony.data.QosBearerSession;
import android.telephony.data.RouteSelectionDescriptor;
import android.telephony.data.TrafficDescriptor;
import android.telephony.data.UrspRule;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.feature.ConnectionFailureInfo;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.ImsRegistrationImplBase.ImsDeregistrationReason;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cat.ComprehensionTlv;
import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.CdmaSmsSubaddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.data.KeepaliveStatus;
import com.android.internal.telephony.data.KeepaliveStatus.KeepaliveStatusCode;
import com.android.internal.telephony.flags.Flags;
import com.android.internal.telephony.imsphone.ImsCallInfo;
import com.android.internal.telephony.uicc.AdnCapacity;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccSimPortInfo;
import com.android.internal.telephony.uicc.IccSlotPortMapping;
import com.android.internal.telephony.uicc.IccSlotStatus;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.PortUtils;
import com.android.internal.telephony.uicc.SimPhonebookRecord;
import com.android.internal.telephony.uicc.SimTypeInfo;
import com.android.telephony.Rlog;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utils class for HAL <-> RIL conversions
 */
public class RILUtils {
    private static final String TAG = "RILUtils";

    // The number of required config values for broadcast SMS stored in RIL_CdmaBroadcastServiceInfo
    public static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;
    // The number of service categories for broadcast SMS
    public static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;

    // Radio power failure UUIDs
    public static final String RADIO_POWER_FAILURE_BUGREPORT_UUID =
            "316f3801-fa21-4954-a42f-0041eada3b31";
    public static final String RADIO_POWER_FAILURE_RF_HARDWARE_ISSUE_UUID =
            "316f3801-fa21-4954-a42f-0041eada3b32";
    public static final String RADIO_POWER_FAILURE_NO_RF_CALIBRATION_UUID =
            "316f3801-fa21-4954-a42f-0041eada3b33";

    private static final Set<Class> WRAPPER_CLASSES = new HashSet(Arrays.asList(
            Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class));

    /**
     * Convert to PersoSubstate defined in radio/1.5/types.hal
     * @param persoType PersoSubState type
     * @return The converted PersoSubstate
     */
    public static int convertToHalPersoType(
            IccCardApplicationStatus.PersoSubState persoType) {
        switch (persoType) {
            case PERSOSUBSTATE_IN_PROGRESS:
                return android.hardware.radio.V1_5.PersoSubstate.IN_PROGRESS;
            case  PERSOSUBSTATE_READY:
                return android.hardware.radio.V1_5.PersoSubstate.READY;
            case PERSOSUBSTATE_SIM_NETWORK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_NETWORK;
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_NETWORK_SUBSET;
            case PERSOSUBSTATE_SIM_CORPORATE:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_CORPORATE;
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SERVICE_PROVIDER;
            case PERSOSUBSTATE_SIM_SIM:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SIM;
            case PERSOSUBSTATE_SIM_NETWORK_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_NETWORK_PUK;
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_NETWORK_SUBSET_PUK;
            case PERSOSUBSTATE_SIM_CORPORATE_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_CORPORATE_PUK;
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SERVICE_PROVIDER_PUK;
            case PERSOSUBSTATE_SIM_SIM_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SIM_PUK;
            case PERSOSUBSTATE_RUIM_NETWORK1:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_NETWORK1;
            case PERSOSUBSTATE_RUIM_NETWORK2:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_NETWORK2;
            case PERSOSUBSTATE_RUIM_HRPD:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_HRPD;
            case PERSOSUBSTATE_RUIM_CORPORATE:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_CORPORATE;
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_SERVICE_PROVIDER;
            case PERSOSUBSTATE_RUIM_RUIM:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_RUIM;
            case PERSOSUBSTATE_RUIM_NETWORK1_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_NETWORK1_PUK;
            case PERSOSUBSTATE_RUIM_NETWORK2_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_NETWORK2_PUK;
            case PERSOSUBSTATE_RUIM_HRPD_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_HRPD_PUK;
            case PERSOSUBSTATE_RUIM_CORPORATE_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_CORPORATE_PUK;
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_SERVICE_PROVIDER_PUK;
            case PERSOSUBSTATE_RUIM_RUIM_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.RUIM_RUIM_PUK;
            case PERSOSUBSTATE_SIM_SPN:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SPN;
            case PERSOSUBSTATE_SIM_SPN_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SPN_PUK;
            case PERSOSUBSTATE_SIM_SP_EHPLMN:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SP_EHPLMN;
            case PERSOSUBSTATE_SIM_SP_EHPLMN_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_SP_EHPLMN_PUK;
            case PERSOSUBSTATE_SIM_ICCID:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_ICCID;
            case PERSOSUBSTATE_SIM_ICCID_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_ICCID_PUK;
            case PERSOSUBSTATE_SIM_IMPI:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_IMPI;
            case PERSOSUBSTATE_SIM_IMPI_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_IMPI_PUK;
            case PERSOSUBSTATE_SIM_NS_SP:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_NS_SP;
            case PERSOSUBSTATE_SIM_NS_SP_PUK:
                return android.hardware.radio.V1_5.PersoSubstate.SIM_NS_SP_PUK;
            default:
                return android.hardware.radio.V1_5.PersoSubstate.UNKNOWN;
        }
    }

    /**
     * Convert to PersoSubstate.aidl
     * @param persoType PersoSubState type
     * @return The converted PersoSubstate
     */
    public static int convertToHalPersoTypeAidl(
            IccCardApplicationStatus.PersoSubState persoType) {
        switch (persoType) {
            case PERSOSUBSTATE_IN_PROGRESS:
                return android.hardware.radio.sim.PersoSubstate.IN_PROGRESS;
            case  PERSOSUBSTATE_READY:
                return android.hardware.radio.sim.PersoSubstate.READY;
            case PERSOSUBSTATE_SIM_NETWORK:
                return android.hardware.radio.sim.PersoSubstate.SIM_NETWORK;
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET:
                return android.hardware.radio.sim.PersoSubstate.SIM_NETWORK_SUBSET;
            case PERSOSUBSTATE_SIM_CORPORATE:
                return android.hardware.radio.sim.PersoSubstate.SIM_CORPORATE;
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER:
                return android.hardware.radio.sim.PersoSubstate.SIM_SERVICE_PROVIDER;
            case PERSOSUBSTATE_SIM_SIM:
                return android.hardware.radio.sim.PersoSubstate.SIM_SIM;
            case PERSOSUBSTATE_SIM_NETWORK_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_NETWORK_PUK;
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_NETWORK_SUBSET_PUK;
            case PERSOSUBSTATE_SIM_CORPORATE_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_CORPORATE_PUK;
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_SERVICE_PROVIDER_PUK;
            case PERSOSUBSTATE_SIM_SIM_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_SIM_PUK;
            case PERSOSUBSTATE_RUIM_NETWORK1:
                return android.hardware.radio.sim.PersoSubstate.RUIM_NETWORK1;
            case PERSOSUBSTATE_RUIM_NETWORK2:
                return android.hardware.radio.sim.PersoSubstate.RUIM_NETWORK2;
            case PERSOSUBSTATE_RUIM_HRPD:
                return android.hardware.radio.sim.PersoSubstate.RUIM_HRPD;
            case PERSOSUBSTATE_RUIM_CORPORATE:
                return android.hardware.radio.sim.PersoSubstate.RUIM_CORPORATE;
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER:
                return android.hardware.radio.sim.PersoSubstate.RUIM_SERVICE_PROVIDER;
            case PERSOSUBSTATE_RUIM_RUIM:
                return android.hardware.radio.sim.PersoSubstate.RUIM_RUIM;
            case PERSOSUBSTATE_RUIM_NETWORK1_PUK:
                return android.hardware.radio.sim.PersoSubstate.RUIM_NETWORK1_PUK;
            case PERSOSUBSTATE_RUIM_NETWORK2_PUK:
                return android.hardware.radio.sim.PersoSubstate.RUIM_NETWORK2_PUK;
            case PERSOSUBSTATE_RUIM_HRPD_PUK:
                return android.hardware.radio.sim.PersoSubstate.RUIM_HRPD_PUK;
            case PERSOSUBSTATE_RUIM_CORPORATE_PUK:
                return android.hardware.radio.sim.PersoSubstate.RUIM_CORPORATE_PUK;
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK:
                return android.hardware.radio.sim.PersoSubstate.RUIM_SERVICE_PROVIDER_PUK;
            case PERSOSUBSTATE_RUIM_RUIM_PUK:
                return android.hardware.radio.sim.PersoSubstate.RUIM_RUIM_PUK;
            case PERSOSUBSTATE_SIM_SPN:
                return android.hardware.radio.sim.PersoSubstate.SIM_SPN;
            case PERSOSUBSTATE_SIM_SPN_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_SPN_PUK;
            case PERSOSUBSTATE_SIM_SP_EHPLMN:
                return android.hardware.radio.sim.PersoSubstate.SIM_SP_EHPLMN;
            case PERSOSUBSTATE_SIM_SP_EHPLMN_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_SP_EHPLMN_PUK;
            case PERSOSUBSTATE_SIM_ICCID:
                return android.hardware.radio.sim.PersoSubstate.SIM_ICCID;
            case PERSOSUBSTATE_SIM_ICCID_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_ICCID_PUK;
            case PERSOSUBSTATE_SIM_IMPI:
                return android.hardware.radio.sim.PersoSubstate.SIM_IMPI;
            case PERSOSUBSTATE_SIM_IMPI_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_IMPI_PUK;
            case PERSOSUBSTATE_SIM_NS_SP:
                return android.hardware.radio.sim.PersoSubstate.SIM_NS_SP;
            case PERSOSUBSTATE_SIM_NS_SP_PUK:
                return android.hardware.radio.sim.PersoSubstate.SIM_NS_SP_PUK;
            default:
                return android.hardware.radio.sim.PersoSubstate.UNKNOWN;
        }
    }

    /**
     * Convert to GsmSmsMessage defined in radio/1.0/types.hal
     * @param smscPdu SMSC address
     * @param pdu SMS in PDU format
     * @return A converted GsmSmsMessage
     */
    public static android.hardware.radio.V1_0.GsmSmsMessage convertToHalGsmSmsMessage(
            String smscPdu, String pdu) {
        android.hardware.radio.V1_0.GsmSmsMessage msg =
                new android.hardware.radio.V1_0.GsmSmsMessage();
        msg.smscPdu = smscPdu == null ? "" : smscPdu;
        msg.pdu = pdu == null ? "" : pdu;
        return msg;
    }

    /**
     * Convert to GsmSmsMessage.aidl
     * @param smscPdu SMSC address
     * @param pdu SMS in PDU format
     * @return A converted GsmSmsMessage
     */
    public static android.hardware.radio.messaging.GsmSmsMessage convertToHalGsmSmsMessageAidl(
            String smscPdu, String pdu) {
        android.hardware.radio.messaging.GsmSmsMessage msg =
                new android.hardware.radio.messaging.GsmSmsMessage();
        msg.smscPdu = convertNullToEmptyString(smscPdu);
        msg.pdu = convertNullToEmptyString(pdu);
        return msg;
    }

    /**
     * Convert to CdmaSmsMessage defined in radio/1.0/types.hal
     * @param pdu SMS in PDU format
     * @return A converted CdmaSmsMessage
     */
    public static android.hardware.radio.V1_0.CdmaSmsMessage convertToHalCdmaSmsMessage(
            byte[] pdu) {
        android.hardware.radio.V1_0.CdmaSmsMessage msg =
                new android.hardware.radio.V1_0.CdmaSmsMessage();
        int addrNbrOfDigits;
        int subaddrNbrOfDigits;
        int bearerDataLength;
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);

        try {
            msg.teleserviceId = dis.readInt(); // teleServiceId
            msg.isServicePresent = (byte) dis.readInt() == 1; // servicePresent
            msg.serviceCategory = dis.readInt(); // serviceCategory
            msg.address.digitMode = dis.read();  // address digit mode
            msg.address.numberMode = dis.read(); // address number mode
            msg.address.numberType = dis.read(); // address number type
            msg.address.numberPlan = dis.read(); // address number plan
            addrNbrOfDigits = (byte) dis.read();
            for (int i = 0; i < addrNbrOfDigits; i++) {
                msg.address.digits.add(dis.readByte()); // address_orig_bytes[i]
            }
            msg.subAddress.subaddressType = dis.read(); //subaddressType
            msg.subAddress.odd = (byte) dis.read() == 1; //subaddr odd
            subaddrNbrOfDigits = (byte) dis.read();
            for (int i = 0; i < subaddrNbrOfDigits; i++) {
                msg.subAddress.digits.add(dis.readByte()); //subaddr_orig_bytes[i]
            }

            bearerDataLength = dis.read();
            for (int i = 0; i < bearerDataLength; i++) {
                msg.bearerData.add(dis.readByte()); //bearerData[i]
            }
        } catch (IOException ex) {
        }
        return msg;
    }

    /**
     * Convert to CdmaSmsMessage.aidl
     * @param pdu SMS in PDU format
     * @return The converted CdmaSmsMessage
     */
    public static android.hardware.radio.messaging.CdmaSmsMessage convertToHalCdmaSmsMessageAidl(
            byte[] pdu) {
        android.hardware.radio.messaging.CdmaSmsMessage msg =
                new android.hardware.radio.messaging.CdmaSmsMessage();
        msg.address = new android.hardware.radio.messaging.CdmaSmsAddress();
        msg.subAddress = new android.hardware.radio.messaging.CdmaSmsSubaddress();
        int addrNbrOfDigits;
        int subaddrNbrOfDigits;
        int bearerDataLength;
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);

        try {
            msg.teleserviceId = dis.readInt(); // teleServiceId
            msg.isServicePresent = (byte) dis.readInt() == 1; // servicePresent
            msg.serviceCategory = dis.readInt(); // serviceCategory
            msg.address.digitMode = dis.read();  // address digit mode
            msg.address.isNumberModeDataNetwork =
                    dis.read() == CdmaSmsAddress.NUMBER_MODE_DATA_NETWORK; // address number mode
            msg.address.numberType = dis.read(); // address number type
            msg.address.numberPlan = dis.read(); // address number plan
            addrNbrOfDigits = (byte) dis.read();
            byte[] digits = new byte[addrNbrOfDigits];
            for (int i = 0; i < addrNbrOfDigits; i++) {
                digits[i] = dis.readByte(); // address_orig_bytes[i]
            }
            msg.address.digits = digits;
            msg.subAddress.subaddressType = dis.read(); //subaddressType
            msg.subAddress.odd = (byte) dis.read() == 1; //subaddr odd
            subaddrNbrOfDigits = (byte) dis.read();
            digits = new byte[subaddrNbrOfDigits];
            for (int i = 0; i < subaddrNbrOfDigits; i++) {
                digits[i] = dis.readByte(); //subaddr_orig_bytes[i]
            }
            msg.subAddress.digits = digits;

            bearerDataLength = dis.read();
            byte[] bearerData = new byte[bearerDataLength];
            for (int i = 0; i < bearerDataLength; i++) {
                bearerData[i] = dis.readByte(); //bearerData[i]
            }
            msg.bearerData = bearerData;
        } catch (IOException ex) {
        }
        return msg;
    }

    /**
     * Convert CdmaSmsMessage defined in radio/1.0/types.hal to SmsMessage
     * Note only primitive fields are set
     * @param cdmaSmsMessage CdmaSmsMessage defined in radio/1.0/types.hal
     * @return A converted SmsMessage
     */
    public static SmsMessage convertHalCdmaSmsMessage(
            android.hardware.radio.V1_0.CdmaSmsMessage cdmaSmsMessage) {
        // Note: Parcel.readByte actually reads one Int and masks to byte
        SmsEnvelope env = new SmsEnvelope();
        CdmaSmsAddress addr = new CdmaSmsAddress();
        CdmaSmsSubaddress subaddr = new CdmaSmsSubaddress();
        byte[] data;
        byte count;
        int countInt;
        int addressDigitMode;

        //currently not supported by the modem-lib: env.mMessageType
        env.teleService = cdmaSmsMessage.teleserviceId;

        if (cdmaSmsMessage.isServicePresent) {
            env.messageType = SmsEnvelope.MESSAGE_TYPE_BROADCAST;
        } else {
            if (SmsEnvelope.TELESERVICE_NOT_SET == env.teleService) {
                // assume type ACK
                env.messageType = SmsEnvelope.MESSAGE_TYPE_ACKNOWLEDGE;
            } else {
                env.messageType = SmsEnvelope.MESSAGE_TYPE_POINT_TO_POINT;
            }
        }
        env.serviceCategory = cdmaSmsMessage.serviceCategory;

        // address
        addressDigitMode = cdmaSmsMessage.address.digitMode;
        addr.digitMode = (byte) (0xFF & addressDigitMode);
        addr.numberMode = (byte) (0xFF & cdmaSmsMessage.address.numberMode);
        addr.ton = cdmaSmsMessage.address.numberType;
        addr.numberPlan = (byte) (0xFF & cdmaSmsMessage.address.numberPlan);
        count = (byte) cdmaSmsMessage.address.digits.size();
        addr.numberOfDigits = count;
        data = new byte[count];
        for (int index = 0; index < count; index++) {
            data[index] = cdmaSmsMessage.address.digits.get(index);

            // convert the value if it is 4-bit DTMF to 8 bit
            if (addressDigitMode == CdmaSmsAddress.DIGIT_MODE_4BIT_DTMF) {
                data[index] = SmsMessage.convertDtmfToAscii(data[index]);
            }
        }

        addr.origBytes = data;

        subaddr.type = cdmaSmsMessage.subAddress.subaddressType;
        subaddr.odd = (byte) (cdmaSmsMessage.subAddress.odd ? 1 : 0);
        count = (byte) cdmaSmsMessage.subAddress.digits.size();

        if (count < 0) {
            count = 0;
        }

        // p_cur->sSubAddress.digits[digitCount] :

        data = new byte[count];

        for (int index = 0; index < count; ++index) {
            data[index] = cdmaSmsMessage.subAddress.digits.get(index);
        }

        subaddr.origBytes = data;

        /* currently not supported by the modem-lib:
            env.bearerReply
            env.replySeqNo
            env.errorClass
            env.causeCode
        */

        // bearer data
        countInt = cdmaSmsMessage.bearerData.size();
        if (countInt < 0) {
            countInt = 0;
        }

        data = new byte[countInt];
        for (int index = 0; index < countInt; index++) {
            data[index] = cdmaSmsMessage.bearerData.get(index);
        }
        // BD gets further decoded when accessed in SMSDispatcher
        env.bearerData = data;

        // link the filled objects to the SMS
        env.origAddress = addr;
        env.origSubaddress = subaddr;

        SmsMessage msg = new SmsMessage(addr, env);

        return msg;
    }

    /**
     * Convert CdmaSmsMessage defined in CdmaSmsMessage.aidl to SmsMessage
     * Note only primitive fields are set
     * @param msg CdmaSmsMessage defined in CdmaSmsMessage.aidl
     * @return A converted SmsMessage
     */
    public static SmsMessage convertHalCdmaSmsMessage(
            android.hardware.radio.messaging.CdmaSmsMessage msg) {
        // Note: Parcel.readByte actually reads one Int and masks to byte
        SmsEnvelope env = new SmsEnvelope();
        CdmaSmsAddress addr = new CdmaSmsAddress();
        CdmaSmsSubaddress subaddr = new CdmaSmsSubaddress();

        // address
        int addressDigitMode = msg.address.digitMode;
        addr.digitMode = (byte) (0xFF & addressDigitMode);
        addr.numberMode = (byte) (0xFF & (msg.address.isNumberModeDataNetwork ? 1 : 0));
        addr.ton = msg.address.numberType;
        addr.numberPlan = (byte) (0xFF & msg.address.numberPlan);
        addr.numberOfDigits = msg.address.digits.length;
        byte[] data = new byte[msg.address.digits.length];
        for (int index = 0; index < data.length; index++) {
            data[index] = msg.address.digits[index];
            // convert the value if it is 4-bit DTMF to 8 bit
            if (addressDigitMode == CdmaSmsAddress.DIGIT_MODE_4BIT_DTMF) {
                data[index] = SmsMessage.convertDtmfToAscii(data[index]);
            }
        }
        addr.origBytes = data;

        // subaddress
        subaddr.type = msg.subAddress.subaddressType;
        subaddr.odd = (byte) (msg.subAddress.odd ? 1 : 0);
        subaddr.origBytes = msg.subAddress.digits;

        // envelope
        // currently not supported by the modem-lib: env.bearerReply, env.replySeqNo,
        // env.errorClass, env.causeCode, env.mMessageType
        env.teleService = msg.teleserviceId;
        if (msg.isServicePresent) {
            env.messageType = SmsEnvelope.MESSAGE_TYPE_BROADCAST;
        } else {
            if (SmsEnvelope.TELESERVICE_NOT_SET == env.teleService) {
                // assume type ACK
                env.messageType = SmsEnvelope.MESSAGE_TYPE_ACKNOWLEDGE;
            } else {
                env.messageType = SmsEnvelope.MESSAGE_TYPE_POINT_TO_POINT;
            }
        }
        env.serviceCategory = msg.serviceCategory;

        // bearer data is further decoded when accessed in SmsDispatcher
        env.bearerData = msg.bearerData;

        // link the filled objects to the SMS
        env.origAddress = addr;
        env.origSubaddress = subaddr;

        return new SmsMessage(addr, env);
    }

    /**
     * Convert to DataProfileInfo defined in radio/1.4/types.hal
     * @param dp Data profile
     * @return The converted DataProfileInfo
     */
    public static android.hardware.radio.V1_4.DataProfileInfo convertToHalDataProfile14(
            DataProfile dp) {
        android.hardware.radio.V1_4.DataProfileInfo dpi =
                new android.hardware.radio.V1_4.DataProfileInfo();

        dpi.apn = dp.getApn();
        dpi.protocol = dp.getProtocolType();
        dpi.roamingProtocol = dp.getRoamingProtocolType();
        dpi.authType = dp.getAuthType();
        dpi.user = TextUtils.emptyIfNull(dp.getUserName());
        dpi.password = TextUtils.emptyIfNull(dp.getPassword());
        dpi.type = dp.getType();
        dpi.maxConnsTime = dp.getMaxConnectionsTime();
        dpi.maxConns = dp.getMaxConnections();
        dpi.waitTime = dp.getWaitTime();
        dpi.enabled = dp.isEnabled();
        dpi.supportedApnTypesBitmap = dp.getSupportedApnTypesBitmask();
        // Shift by 1 bit due to the discrepancy between
        // android.hardware.radio.V1_0.RadioAccessFamily and the bitmask version of
        // ServiceState.RIL_RADIO_TECHNOLOGY_XXXX.
        dpi.bearerBitmap = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(
                dp.getBearerBitmask()) << 1;
        dpi.mtu = dp.getMtuV4();
        dpi.persistent = dp.isPersistent();
        dpi.preferred = dp.isPreferred();

        // profile id is only meaningful when it's persistent on the modem.
        dpi.profileId = (dpi.persistent) ? dp.getProfileId()
                : android.hardware.radio.V1_0.DataProfileId.INVALID;

        return dpi;
    }

    /**
     * Convert to DataProfileInfo defined in radio/1.5/types.hal
     * @param dp Data profile
     * @return The converted DataProfileInfo
     */
    public static android.hardware.radio.V1_5.DataProfileInfo convertToHalDataProfile15(
            DataProfile dp) {
        android.hardware.radio.V1_5.DataProfileInfo dpi =
                new android.hardware.radio.V1_5.DataProfileInfo();

        dpi.apn = dp.getApn();
        dpi.protocol = dp.getProtocolType();
        dpi.roamingProtocol = dp.getRoamingProtocolType();
        dpi.authType = dp.getAuthType();
        dpi.user = TextUtils.emptyIfNull(dp.getUserName());
        dpi.password = TextUtils.emptyIfNull(dp.getPassword());
        dpi.type = dp.getType();
        dpi.maxConnsTime = dp.getMaxConnectionsTime();
        dpi.maxConns = dp.getMaxConnections();
        dpi.waitTime = dp.getWaitTime();
        dpi.enabled = dp.isEnabled();
        dpi.supportedApnTypesBitmap = dp.getSupportedApnTypesBitmask();
        // Shift by 1 bit due to the discrepancy between
        // android.hardware.radio.V1_0.RadioAccessFamily and the bitmask version of
        // ServiceState.RIL_RADIO_TECHNOLOGY_XXXX.
        dpi.bearerBitmap = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(
                dp.getBearerBitmask()) << 1;
        dpi.mtuV4 = dp.getMtuV4();
        dpi.mtuV6 = dp.getMtuV6();
        dpi.persistent = dp.isPersistent();
        dpi.preferred = dp.isPreferred();

        // profile id is only meaningful when it's persistent on the modem.
        dpi.profileId = (dpi.persistent) ? dp.getProfileId()
                : android.hardware.radio.V1_0.DataProfileId.INVALID;

        return dpi;
    }

    /**
     * Convert to DataProfileInfo.aidl
     * @param dp Data profile
     * @return The converted DataProfileInfo
     */
    public static android.hardware.radio.data.DataProfileInfo convertToHalDataProfile(
            @Nullable DataProfile dp) {
        if (dp == null) return null;
        android.hardware.radio.data.DataProfileInfo dpi =
                new android.hardware.radio.data.DataProfileInfo();

        dpi.apn = dp.getApn();
        dpi.protocol = dp.getProtocolType();
        dpi.roamingProtocol = dp.getRoamingProtocolType();
        dpi.authType = dp.getAuthType();
        dpi.user = convertNullToEmptyString(dp.getUserName());
        dpi.password = convertNullToEmptyString(dp.getPassword());
        dpi.type = dp.getType();
        dpi.maxConnsTime = dp.getMaxConnectionsTime();
        dpi.maxConns = dp.getMaxConnections();
        dpi.waitTime = dp.getWaitTime();
        dpi.enabled = dp.isEnabled();
        dpi.supportedApnTypesBitmap = dp.getSupportedApnTypesBitmask();
        // Shift by 1 bit due to the discrepancy between RadioAccessFamily.aidl and the bitmask
        // version of ServiceState.RIL_RADIO_TECHNOLOGY_XXXX.
        dpi.bearerBitmap = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(
                dp.getBearerBitmask()) << 1;
        dpi.mtuV4 = dp.getMtuV4();
        dpi.mtuV6 = dp.getMtuV6();
        dpi.persistent = dp.isPersistent();
        dpi.preferred = dp.isPreferred();
        dpi.alwaysOn = false;
        dpi.infrastructureBitmap = android.hardware.radio.data.DataProfileInfo
                .INFRASTRUCTURE_CELLULAR;
        if (dp.getApnSetting() != null) {
            dpi.alwaysOn = dp.getApnSetting().isAlwaysOn();
            dpi.infrastructureBitmap = dp.getApnSetting().getInfrastructureBitmask();
        }
        dpi.trafficDescriptor = convertToHalTrafficDescriptorAidl(dp.getTrafficDescriptor());

        // profile id is only meaningful when it's persistent on the modem.
        dpi.profileId = (dpi.persistent) ? dp.getProfileId()
                : android.hardware.radio.data.DataProfileInfo.ID_INVALID;

        return dpi;
    }

    /**
     * Convert from DataProfileInfo.aidl to DataProfile
     * @param dpi DataProfileInfo
     * @return The converted DataProfile
     */
    public static DataProfile convertToDataProfile(
            android.hardware.radio.data.DataProfileInfo dpi) {
        ApnSetting apnSetting = new ApnSetting.Builder()
                .setEntryName(dpi.apn)
                .setApnName(dpi.apn)
                .setApnTypeBitmask(dpi.supportedApnTypesBitmap)
                .setAuthType(dpi.authType)
                .setMaxConnsTime(dpi.maxConnsTime)
                .setMaxConns(dpi.maxConns)
                .setWaitTime(dpi.waitTime)
                .setCarrierEnabled(dpi.enabled)
                .setModemCognitive(dpi.persistent)
                .setMtuV4(dpi.mtuV4)
                .setMtuV6(dpi.mtuV6)
                .setNetworkTypeBitmask(ServiceState.convertBearerBitmaskToNetworkTypeBitmask(
                        dpi.bearerBitmap) >> 1)
                .setProfileId(dpi.profileId)
                .setPassword(dpi.password)
                .setProtocol(dpi.protocol)
                .setRoamingProtocol(dpi.roamingProtocol)
                .setUser(dpi.user)
                .setAlwaysOn(dpi.alwaysOn)
                .setInfrastructureBitmask(dpi.infrastructureBitmap)
                .build();

        TrafficDescriptor td;
        try {
            td = convertHalTrafficDescriptor(dpi.trafficDescriptor);
        } catch (IllegalArgumentException e) {
            loge("convertToDataProfile: Failed to convert traffic descriptor. e=" + e);
            td = null;
        }

        return new DataProfile.Builder()
                .setType(dpi.type)
                .setPreferred(dpi.preferred)
                .setTrafficDescriptor(td)
                .setApnSetting(apnSetting)
                .build();
    }

    /**
     * Convert to OptionalSliceInfo defined in radio/1.6/types.hal
     * @param sliceInfo Slice info
     * @return The converted OptionalSliceInfo
     */
    public static android.hardware.radio.V1_6.OptionalSliceInfo convertToHalSliceInfo(
            @Nullable NetworkSliceInfo sliceInfo) {
        android.hardware.radio.V1_6.OptionalSliceInfo optionalSliceInfo =
                new android.hardware.radio.V1_6.OptionalSliceInfo();
        if (sliceInfo == null) {
            return optionalSliceInfo;
        }

        android.hardware.radio.V1_6.SliceInfo si = new android.hardware.radio.V1_6.SliceInfo();
        si.sst = (byte) sliceInfo.getSliceServiceType();
        si.mappedHplmnSst = (byte) sliceInfo.getMappedHplmnSliceServiceType();
        si.sliceDifferentiator = sliceInfo.getSliceDifferentiator();
        si.mappedHplmnSD = sliceInfo.getMappedHplmnSliceDifferentiator();
        optionalSliceInfo.value(si);
        return optionalSliceInfo;
    }

    /**
     * Convert to SliceInfo.aidl
     * @param sliceInfo Slice info
     * @return The converted SliceInfo
     */
    public static android.hardware.radio.data.SliceInfo convertToHalSliceInfoAidl(
            @Nullable NetworkSliceInfo sliceInfo) {
        if (sliceInfo == null) {
            return null;
        }

        android.hardware.radio.data.SliceInfo si = new android.hardware.radio.data.SliceInfo();
        si.sliceServiceType = (byte) sliceInfo.getSliceServiceType();
        si.mappedHplmnSst = (byte) sliceInfo.getMappedHplmnSliceServiceType();
        si.sliceDifferentiator = sliceInfo.getSliceDifferentiator();
        si.mappedHplmnSd = sliceInfo.getMappedHplmnSliceDifferentiator();
        return si;
    }

    /**
     * Convert to OptionalTrafficDescriptor defined in radio/1.6/types.hal
     * @param trafficDescriptor Traffic descriptor
     * @return The converted OptionalTrafficDescriptor
     */
    public static android.hardware.radio.V1_6.OptionalTrafficDescriptor
            convertToHalTrafficDescriptor(@Nullable TrafficDescriptor trafficDescriptor) {
        android.hardware.radio.V1_6.OptionalTrafficDescriptor optionalTrafficDescriptor =
                new android.hardware.radio.V1_6.OptionalTrafficDescriptor();
        if (trafficDescriptor == null) {
            return optionalTrafficDescriptor;
        }

        android.hardware.radio.V1_6.TrafficDescriptor td =
                new android.hardware.radio.V1_6.TrafficDescriptor();

        android.hardware.radio.V1_6.OptionalDnn optionalDnn =
                new android.hardware.radio.V1_6.OptionalDnn();
        if (trafficDescriptor.getDataNetworkName() != null) {
            optionalDnn.value(trafficDescriptor.getDataNetworkName());
        }
        td.dnn = optionalDnn;

        android.hardware.radio.V1_6.OptionalOsAppId optionalOsAppId =
                new android.hardware.radio.V1_6.OptionalOsAppId();
        if (trafficDescriptor.getOsAppId() != null) {
            android.hardware.radio.V1_6.OsAppId osAppId = new android.hardware.radio.V1_6.OsAppId();
            osAppId.osAppId = primitiveArrayToArrayList(trafficDescriptor.getOsAppId());
            optionalOsAppId.value(osAppId);
        }
        td.osAppId = optionalOsAppId;

        optionalTrafficDescriptor.value(td);
        return optionalTrafficDescriptor;
    }

    /**
     * Convert to TrafficDescriptor.aidl
     * @param trafficDescriptor Traffic descriptor
     * @return The converted TrafficDescriptor
     */
    public static android.hardware.radio.data.TrafficDescriptor
            convertToHalTrafficDescriptorAidl(@Nullable TrafficDescriptor trafficDescriptor) {
        if (trafficDescriptor == null) {
            return new android.hardware.radio.data.TrafficDescriptor();
        }

        android.hardware.radio.data.TrafficDescriptor td =
                new android.hardware.radio.data.TrafficDescriptor();
        td.dnn = trafficDescriptor.getDataNetworkName();
        if (trafficDescriptor.getOsAppId() == null) {
            td.osAppId = null;
        } else {
            android.hardware.radio.data.OsAppId osAppId = new android.hardware.radio.data.OsAppId();
            osAppId.osAppId = trafficDescriptor.getOsAppId();
            td.osAppId = osAppId;
        }
        return td;
    }

    /**
     * Convert to ResetNvType defined in radio/1.0/types.hal
     * @param resetType NV reset type
     * @return The converted reset type in integer or -1 if param is invalid
     */
    public static int convertToHalResetNvType(int resetType) {
        /**
         * resetType values
         * 1 - reload all NV items
         * 2 - erase NV reset (SCRTN)
         * 3 - factory reset (RTN)
         */
        if (Flags.cleanupCdma()) {
            if (resetType == 1) return android.hardware.radio.V1_0.ResetNvType.RELOAD;
            return -1;
        }
        switch (resetType) {
            case 1: return android.hardware.radio.V1_0.ResetNvType.RELOAD;
            case 2: return android.hardware.radio.V1_0.ResetNvType.ERASE;
            case 3: return android.hardware.radio.V1_0.ResetNvType.FACTORY_RESET;
        }
        return -1;
    }

    /**
     * Convert to ResetNvType.aidl
     * @param resetType NV reset type
     * @return The converted reset type in integer or -1 if param is invalid
     */
    public static int convertToHalResetNvTypeAidl(int resetType) {
        /**
         * resetType values
         * 1 - reload all NV items
         * 2 - erase NV reset (SCRTN)
         * 3 - factory reset (RTN)
         */
        if (Flags.cleanupCdma()) {
            if (resetType == 1) return android.hardware.radio.modem.ResetNvType.RELOAD;
            return -1;
        }
        switch (resetType) {
            case 1: return android.hardware.radio.modem.ResetNvType.RELOAD;
            case 2: return android.hardware.radio.modem.ResetNvType.ERASE;
            case 3: return android.hardware.radio.modem.ResetNvType.FACTORY_RESET;
        }
        return -1;
    }

    /**
     * Convert to a list of LinkAddress defined in radio/1.5/types.hal
     * @param linkProperties Link properties
     * @return The converted list of LinkAddresses
     */
    public static ArrayList<android.hardware.radio.V1_5.LinkAddress> convertToHalLinkProperties15(
            LinkProperties linkProperties) {
        ArrayList<android.hardware.radio.V1_5.LinkAddress> addresses15 = new ArrayList<>();
        if (linkProperties != null) {
            for (android.net.LinkAddress la : linkProperties.getAllLinkAddresses()) {
                android.hardware.radio.V1_5.LinkAddress linkAddress =
                        new android.hardware.radio.V1_5.LinkAddress();
                linkAddress.address = la.getAddress().getHostAddress();
                linkAddress.properties = la.getFlags();
                linkAddress.deprecationTime = la.getDeprecationTime();
                linkAddress.expirationTime = la.getExpirationTime();
                addresses15.add(linkAddress);
            }
        }
        return addresses15;
    }

    /**
     * Convert to a list of LinkAddress.aidl
     * @param linkProperties Link properties
     * @return The converted list of LinkAddresses
     */
    public static android.hardware.radio.data.LinkAddress[] convertToHalLinkProperties(
            LinkProperties linkProperties) {
        if (linkProperties == null) {
            return new android.hardware.radio.data.LinkAddress[0];
        }
        android.hardware.radio.data.LinkAddress[] addresses =
                new android.hardware.radio.data.LinkAddress[
                        linkProperties.getAllLinkAddresses().size()];
        for (int i = 0; i < linkProperties.getAllLinkAddresses().size(); i++) {
            LinkAddress la = linkProperties.getAllLinkAddresses().get(i);
            android.hardware.radio.data.LinkAddress linkAddress =
                    new android.hardware.radio.data.LinkAddress();
            linkAddress.address = la.getAddress().getHostAddress();
            linkAddress.addressProperties = la.getFlags();
            linkAddress.deprecationTime = la.getDeprecationTime();
            linkAddress.expirationTime = la.getExpirationTime();
            addresses[i] = linkAddress;
        }
        return addresses;
    }

    /**
     * Convert RadioAccessSpecifier defined in radio/1.5/types.hal to RadioAccessSpecifier
     * @param specifier RadioAccessSpecifier defined in radio/1.5/types.hal
     * @return The converted RadioAccessSpecifier
     */
    public static RadioAccessSpecifier convertHalRadioAccessSpecifier(
            android.hardware.radio.V1_5.RadioAccessSpecifier specifier) {
        if (specifier == null) return null;
        ArrayList<Integer> halBands = new ArrayList<>();
        switch (specifier.bands.getDiscriminator()) {
            case android.hardware.radio.V1_5.RadioAccessSpecifier.Bands.hidl_discriminator
                    .geranBands:
                halBands = specifier.bands.geranBands();
                break;
            case android.hardware.radio.V1_5.RadioAccessSpecifier.Bands.hidl_discriminator
                    .utranBands:
                halBands = specifier.bands.utranBands();
                break;
            case android.hardware.radio.V1_5.RadioAccessSpecifier.Bands.hidl_discriminator
                    .eutranBands:
                halBands = specifier.bands.eutranBands();
                break;
            case android.hardware.radio.V1_5.RadioAccessSpecifier.Bands.hidl_discriminator
                    .ngranBands:
                halBands = specifier.bands.ngranBands();
                break;
        }
        return new RadioAccessSpecifier(convertHalRadioAccessNetworks(specifier.radioAccessNetwork),
                halBands.stream().mapToInt(Integer::intValue).toArray(),
                specifier.channels.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Convert RadioAccessSpecifier defined in RadioAccessSpecifier.aidl to RadioAccessSpecifier
     * @param specifier RadioAccessSpecifier defined in RadioAccessSpecifier.aidl
     * @return The converted RadioAccessSpecifier
     */
    public static RadioAccessSpecifier convertHalRadioAccessSpecifier(
            android.hardware.radio.network.RadioAccessSpecifier specifier) {
        if (specifier == null) return null;
        int[] halBands = null;
        switch (specifier.bands.getTag()) {
            case android.hardware.radio.network.RadioAccessSpecifierBands.geranBands:
                halBands = specifier.bands.getGeranBands();
                break;
            case android.hardware.radio.network.RadioAccessSpecifierBands.utranBands:
                halBands = specifier.bands.getUtranBands();
                break;
            case android.hardware.radio.network.RadioAccessSpecifierBands.eutranBands:
                halBands = specifier.bands.getEutranBands();
                break;
            case android.hardware.radio.network.RadioAccessSpecifierBands.ngranBands:
                halBands = specifier.bands.getNgranBands();
                break;
        }
        return new RadioAccessSpecifier(specifier.accessNetwork, halBands, specifier.channels);
    }

    /**
     * Convert to RadioAccessSpecifier defined in radio/1.1/types.hal
     * @param ras Radio access specifier
     * @return The converted RadioAccessSpecifier
     */
    public static android.hardware.radio.V1_1.RadioAccessSpecifier
            convertToHalRadioAccessSpecifier11(RadioAccessSpecifier ras) {
        android.hardware.radio.V1_1.RadioAccessSpecifier rasInHalFormat =
                new android.hardware.radio.V1_1.RadioAccessSpecifier();
        rasInHalFormat.radioAccessNetwork = ras.getRadioAccessNetwork();
        ArrayList<Integer> bands = new ArrayList<>();
        if (ras.getBands() != null) {
            for (int band : ras.getBands()) {
                bands.add(band);
            }
        }
        switch (ras.getRadioAccessNetwork()) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                rasInHalFormat.geranBands = bands;
                break;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                rasInHalFormat.utranBands = bands;
                break;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                rasInHalFormat.eutranBands = bands;
                break;
            default:
                return null;
        }

        if (ras.getChannels() != null) {
            for (int channel : ras.getChannels()) {
                rasInHalFormat.channels.add(channel);
            }
        }

        return rasInHalFormat;
    }

    /**
     * Convert to RadioAccessSpecifier defined in radio/1.5/types.hal
     * @param ras Radio access specifier
     * @return The converted RadioAccessSpecifier
     */
    public static android.hardware.radio.V1_5.RadioAccessSpecifier
            convertToHalRadioAccessSpecifier15(RadioAccessSpecifier ras) {
        android.hardware.radio.V1_5.RadioAccessSpecifier rasInHalFormat =
                new android.hardware.radio.V1_5.RadioAccessSpecifier();
        android.hardware.radio.V1_5.RadioAccessSpecifier.Bands bandsInHalFormat =
                new android.hardware.radio.V1_5.RadioAccessSpecifier.Bands();
        rasInHalFormat.radioAccessNetwork = convertToHalRadioAccessNetworks(
                ras.getRadioAccessNetwork());
        ArrayList<Integer> bands = new ArrayList<>();
        if (ras.getBands() != null) {
            for (int band : ras.getBands()) {
                bands.add(band);
            }
        }
        switch (ras.getRadioAccessNetwork()) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                bandsInHalFormat.geranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                bandsInHalFormat.utranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                bandsInHalFormat.eutranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                bandsInHalFormat.ngranBands(bands);
                break;
            default:
                return null;
        }
        rasInHalFormat.bands = bandsInHalFormat;

        if (ras.getChannels() != null) {
            for (int channel : ras.getChannels()) {
                rasInHalFormat.channels.add(channel);
            }
        }

        return rasInHalFormat;
    }

    /**
     * Convert to RadioAccessSpecifier.aidl
     * @param ras Radio access specifier
     * @return The converted RadioAccessSpecifier
     */
    public static android.hardware.radio.network.RadioAccessSpecifier
            convertToHalRadioAccessSpecifierAidl(RadioAccessSpecifier ras) {
        android.hardware.radio.network.RadioAccessSpecifier rasInHalFormat =
                new android.hardware.radio.network.RadioAccessSpecifier();
        android.hardware.radio.network.RadioAccessSpecifierBands bandsInHalFormat =
                new android.hardware.radio.network.RadioAccessSpecifierBands();
        rasInHalFormat.accessNetwork = convertToHalAccessNetworkAidl(ras.getRadioAccessNetwork());
        int[] bands;
        if (ras.getBands() != null) {
            bands = new int[ras.getBands().length];
            for (int i = 0; i < ras.getBands().length; i++) {
                bands[i] = ras.getBands()[i];
            }
        } else {
            bands = new int[0];
        }
        switch (ras.getRadioAccessNetwork()) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                bandsInHalFormat.setGeranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                bandsInHalFormat.setUtranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                bandsInHalFormat.setEutranBands(bands);
                break;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                bandsInHalFormat.setNgranBands(bands);
                break;
            default:
                return null;
        }
        rasInHalFormat.bands = bandsInHalFormat;

        int[] channels;
        if (ras.getChannels() != null) {
            channels = new int[ras.getChannels().length];
            for (int i = 0; i < ras.getChannels().length; i++) {
                channels[i] = ras.getChannels()[i];
            }
        } else {
            channels = new int[0];
        }
        rasInHalFormat.channels = channels;

        return rasInHalFormat;
    }

    /**
     * Convert to censored terminal response
     * @param terminalResponse Terminal response
     * @return The converted censored terminal response
     */
    public static String convertToCensoredTerminalResponse(String terminalResponse) {
        try {
            byte[] bytes = IccUtils.hexStringToBytes(terminalResponse);
            if (bytes != null) {
                List<ComprehensionTlv> ctlvs = ComprehensionTlv.decodeMany(bytes, 0);
                int from = 0;
                for (ComprehensionTlv ctlv : ctlvs) {
                    // Find text strings which might be personal information input by user,
                    // then replace it with "********".
                    if (ComprehensionTlvTag.TEXT_STRING.value() == ctlv.getTag()) {
                        byte[] target = Arrays.copyOfRange(ctlv.getRawValue(), from,
                                ctlv.getValueIndex() + ctlv.getLength());
                        terminalResponse = terminalResponse.toLowerCase(Locale.ROOT).replace(
                                IccUtils.bytesToHexString(target).toLowerCase(Locale.ROOT),
                                "********");
                    }
                    // The text string tag and the length field should also be hidden.
                    from = ctlv.getValueIndex() + ctlv.getLength();
                }
            }
        } catch (Exception e) {
            terminalResponse = null;
        }

        return terminalResponse;
    }

    /**
     * Convert to {@link TelephonyManager.NetworkTypeBitMask}, the bitmask represented by
     * {@link android.telephony.Annotation.NetworkType}.
     *
     * @param raf {@link android.hardware.radio.V1_0.RadioAccessFamily}
     * @return {@link TelephonyManager.NetworkTypeBitMask}
     */
    @TelephonyManager.NetworkTypeBitMask
    public static int convertHalNetworkTypeBitMask(int raf) {
        int networkTypeRaf = 0;

        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.GSM) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_GSM;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.GPRS) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_GPRS;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EDGE) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EDGE;
        }
        // convert both IS95A/IS95B to CDMA as network mode doesn't support CDMA
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.IS95A) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_CDMA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.IS95B) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_CDMA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.ONE_X_RTT) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EVDO_0) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_0;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EVDO_A) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_A;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EVDO_B) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_B;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EHRPD) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EHRPD;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSUPA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSDPA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSPA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSPA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSPAP) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.UMTS) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_UMTS;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.TD_SCDMA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.LTE) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_LTE;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.LTE_CA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA;
        }
        if ((raf & android.hardware.radio.V1_4.RadioAccessFamily.NR) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_NR;
        }
        if ((raf & (1 << ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN)) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_IWLAN;
        }
        return (networkTypeRaf == 0) ? TelephonyManager.NETWORK_TYPE_UNKNOWN : networkTypeRaf;
    }

    /**
     * Convert to RadioAccessFamily defined in radio/1.4/types.hal
     * @param networkTypeBitmask {@link TelephonyManager.NetworkTypeBitMask}, the bitmask
     *        represented by {@link android.telephony.Annotation.NetworkType}
     * @return The converted RadioAccessFamily
     */
    public static int convertToHalRadioAccessFamily(
            @TelephonyManager.NetworkTypeBitMask int networkTypeBitmask) {
        int raf = 0;

        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_GSM) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.GSM;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_GPRS) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.GPRS;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EDGE) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EDGE;
        }
        // convert CDMA to IS95A, consistent with ServiceState.networkTypeToRilRadioTechnology
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_CDMA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.IS95A;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.ONE_X_RTT;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_0) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EVDO_0;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_A) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EVDO_A;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_B) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EVDO_B;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EHRPD) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EHRPD;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSUPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSDPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSPA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSPAP;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_UMTS) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.UMTS;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.TD_SCDMA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_IWLAN) != 0) {
            raf |= (1 << android.hardware.radio.V1_4.RadioTechnology.IWLAN);
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_LTE) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.LTE;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.LTE_CA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_NR) != 0) {
            raf |= android.hardware.radio.V1_4.RadioAccessFamily.NR;
        }
        return (raf == 0) ? android.hardware.radio.V1_4.RadioAccessFamily.UNKNOWN : raf;
    }

    /**
     * Convert to RadioAccessFamily.aidl
     * @param networkTypeBitmask {@link TelephonyManager.NetworkTypeBitMask}, the bitmask
     *        represented by {@link android.telephony.Annotation.NetworkType}
     * @return The converted RadioAccessFamily
     */
    public static int convertToHalRadioAccessFamilyAidl(
            @TelephonyManager.NetworkTypeBitMask int networkTypeBitmask) {
        int raf = 0;

        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_GSM) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.GSM;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_GPRS) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.GPRS;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EDGE) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.EDGE;
        }
        // convert CDMA to IS95A, consistent with ServiceState.networkTypeToRilRadioTechnology
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_CDMA) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.IS95A;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.ONE_X_RTT;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_0) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.EVDO_0;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_A) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.EVDO_A;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_B) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.EVDO_B;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EHRPD) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.EHRPD;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.HSUPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.HSDPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSPA) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.HSPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.HSPAP;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_UMTS) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.UMTS;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.TD_SCDMA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_IWLAN) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.IWLAN;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_LTE) != 0
                || (networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.LTE;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_NR) != 0) {
            raf |= android.hardware.radio.RadioAccessFamily.NR;
        }
        return (raf == 0) ? android.hardware.radio.RadioAccessFamily.UNKNOWN : raf;
    }

    /**
     * Convert AccessNetworkType to AccessNetwork defined in radio/1.5/types.hal
     * @param accessNetworkType Access network type
     * @return The converted AccessNetwork
     */
    public static int convertToHalAccessNetwork(int accessNetworkType) {
        switch (accessNetworkType) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                return android.hardware.radio.V1_5.AccessNetwork.GERAN;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                return android.hardware.radio.V1_5.AccessNetwork.UTRAN;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                return android.hardware.radio.V1_5.AccessNetwork.EUTRAN;
            case AccessNetworkConstants.AccessNetworkType.CDMA2000:
                return android.hardware.radio.V1_5.AccessNetwork.CDMA2000;
            case AccessNetworkConstants.AccessNetworkType.IWLAN:
                return android.hardware.radio.V1_5.AccessNetwork.IWLAN;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                return android.hardware.radio.V1_5.AccessNetwork.NGRAN;
            case AccessNetworkConstants.AccessNetworkType.UNKNOWN:
            default:
                return android.hardware.radio.V1_5.AccessNetwork.UNKNOWN;
        }
    }

    /**
     * Convert to AccessNetwork.aidl
     * @param accessNetworkType Access network type
     * @return The converted AccessNetwork
     */
    public static int convertToHalAccessNetworkAidl(int accessNetworkType) {
        switch (accessNetworkType) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                return android.hardware.radio.AccessNetwork.GERAN;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                return android.hardware.radio.AccessNetwork.UTRAN;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                return android.hardware.radio.AccessNetwork.EUTRAN;
            case AccessNetworkConstants.AccessNetworkType.CDMA2000:
                return android.hardware.radio.AccessNetwork.CDMA2000;
            case AccessNetworkConstants.AccessNetworkType.IWLAN:
                return android.hardware.radio.AccessNetwork.IWLAN;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                return android.hardware.radio.AccessNetwork.NGRAN;
            case AccessNetworkConstants.AccessNetworkType.UNKNOWN:
            default:
                return android.hardware.radio.AccessNetwork.UNKNOWN;
        }
    }

    /**
     * Convert to RadioAccessNetwork defined in radio/1.1/types.hal
     * @param accessNetworkType Access network type
     * @return The converted RadioAccessNetwork
     */
    public static int convertToHalRadioAccessNetworks(int accessNetworkType) {
        switch (accessNetworkType) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                return android.hardware.radio.V1_1.RadioAccessNetworks.GERAN;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                return android.hardware.radio.V1_1.RadioAccessNetworks.UTRAN;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                return android.hardware.radio.V1_1.RadioAccessNetworks.EUTRAN;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                return android.hardware.radio.V1_5.RadioAccessNetworks.NGRAN;
            case AccessNetworkConstants.AccessNetworkType.CDMA2000:
                return android.hardware.radio.V1_5.RadioAccessNetworks.CDMA2000;
            case AccessNetworkConstants.AccessNetworkType.UNKNOWN:
            default:
                return android.hardware.radio.V1_5.RadioAccessNetworks.UNKNOWN;
        }
    }

    /**
     * Convert RadioAccessNetworks defined in radio/1.5/types.hal to AccessNetworkType
     * @param ran RadioAccessNetwork defined in radio/1.5/types.hal
     * @return The converted AccessNetworkType
     */
    public static int convertHalRadioAccessNetworks(int ran) {
        switch (ran) {
            case android.hardware.radio.V1_5.RadioAccessNetworks.GERAN:
                return AccessNetworkConstants.AccessNetworkType.GERAN;
            case android.hardware.radio.V1_5.RadioAccessNetworks.UTRAN:
                return AccessNetworkConstants.AccessNetworkType.UTRAN;
            case android.hardware.radio.V1_5.RadioAccessNetworks.EUTRAN:
                return AccessNetworkConstants.AccessNetworkType.EUTRAN;
            case android.hardware.radio.V1_5.RadioAccessNetworks.NGRAN:
                return AccessNetworkConstants.AccessNetworkType.NGRAN;
            case android.hardware.radio.V1_5.RadioAccessNetworks.CDMA2000:
                return AccessNetworkConstants.AccessNetworkType.CDMA2000;
            case android.hardware.radio.V1_5.RadioAccessNetworks.UNKNOWN:
            default:
                return AccessNetworkConstants.AccessNetworkType.UNKNOWN;
        }
    }

    /**
     * Convert to SimApdu defined in radio/1.0/types.hal
     * @param channel channel
     * @param cla cla
     * @param instruction instruction
     * @param p1 p1
     * @param p2 p2
     * @param p3 p3
     * @param data data
     * @return The converted SimApdu
     */
    public static android.hardware.radio.V1_0.SimApdu convertToHalSimApdu(int channel, int cla,
            int instruction, int p1, int p2, int p3, String data) {
        android.hardware.radio.V1_0.SimApdu msg = new android.hardware.radio.V1_0.SimApdu();
        msg.sessionId = channel;
        msg.cla = cla;
        msg.instruction = instruction;
        msg.p1 = p1;
        msg.p2 = p2;
        msg.p3 = p3;
        msg.data = convertNullToEmptyString(data);
        return msg;
    }

    /**
     * Convert to SimApdu.aidl
     * @param channel channel
     * @param cla cla
     * @param instruction instruction
     * @param p1 p1
     * @param p2 p2
     * @param p3 p3
     * @param data data
     * @param radioHalVersion radio hal version
     * @return The converted SimApdu
     */
    public static android.hardware.radio.sim.SimApdu convertToHalSimApduAidl(int channel, int cla,
            int instruction, int p1, int p2, int p3, String data, boolean isEs10Command,
            HalVersion radioHalVersion) {
        android.hardware.radio.sim.SimApdu msg = new android.hardware.radio.sim.SimApdu();
        msg.sessionId = channel;
        msg.cla = cla;
        msg.instruction = instruction;
        msg.p1 = p1;
        msg.p2 = p2;
        msg.p3 = p3;
        msg.data = convertNullToEmptyString(data);
        if (radioHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_2_1)) {
            msg.isEs10 = isEs10Command;
        }
        return msg;
    }

    /**
     * Convert to SimLockMultiSimPolicy defined in radio/1.4/types.hal
     * @param policy Multi SIM policy
     * @return The converted SimLockMultiSimPolicy
     */
    public static int convertToHalSimLockMultiSimPolicy(int policy) {
        switch (policy) {
            case CarrierRestrictionRules.MULTISIM_POLICY_ONE_VALID_SIM_MUST_BE_PRESENT:
                return android.hardware.radio.V1_4.SimLockMultiSimPolicy
                        .ONE_VALID_SIM_MUST_BE_PRESENT;
            case CarrierRestrictionRules.MULTISIM_POLICY_NONE:
                // fallthrough
            default:
                return android.hardware.radio.V1_4.SimLockMultiSimPolicy.NO_MULTISIM_POLICY;

        }
    }

    /**
     * Convert to SimLockMultiSimPolicy.aidl
     * @param policy Multi SIM policy
     * @return The converted SimLockMultiSimPolicy
     */
    public static int convertToHalSimLockMultiSimPolicyAidl(int policy) {
        switch (policy) {
            case CarrierRestrictionRules.MULTISIM_POLICY_ONE_VALID_SIM_MUST_BE_PRESENT:
                return android.hardware.radio.sim.SimLockMultiSimPolicy
                        .ONE_VALID_SIM_MUST_BE_PRESENT;
            case CarrierRestrictionRules.MULTISIM_POLICY_NONE:
                // fallthrough
            default:
                return android.hardware.radio.sim.SimLockMultiSimPolicy.NO_MULTISIM_POLICY;

        }
    }

    /**
     * Convert a list of CarrierIdentifiers into a list of Carriers defined in radio/1.0/types.hal
     * @param carriers List of CarrierIdentifiers
     * @return The converted list of Carriers
     */
    public static ArrayList<android.hardware.radio.V1_0.Carrier> convertToHalCarrierRestrictionList(
            List<CarrierIdentifier> carriers) {
        ArrayList<android.hardware.radio.V1_0.Carrier> result = new ArrayList<>();
        for (CarrierIdentifier ci : carriers) {
            android.hardware.radio.V1_0.Carrier c = new android.hardware.radio.V1_0.Carrier();
            c.mcc = convertNullToEmptyString(ci.getMcc());
            c.mnc = convertNullToEmptyString(ci.getMnc());
            int matchType = CarrierIdentifier.MatchType.ALL;
            String matchData = null;
            if (!TextUtils.isEmpty(ci.getSpn())) {
                matchType = CarrierIdentifier.MatchType.SPN;
                matchData = ci.getSpn();
            } else if (!TextUtils.isEmpty(ci.getImsi())) {
                matchType = CarrierIdentifier.MatchType.IMSI_PREFIX;
                matchData = ci.getImsi();
            } else if (!TextUtils.isEmpty(ci.getGid1())) {
                matchType = CarrierIdentifier.MatchType.GID1;
                matchData = ci.getGid1();
            } else if (!TextUtils.isEmpty(ci.getGid2())) {
                matchType = CarrierIdentifier.MatchType.GID2;
                matchData = ci.getGid2();
            }
            c.matchType = matchType;
            c.matchData = convertNullToEmptyString(matchData);
            result.add(c);
        }
        return result;
    }

    /**
     * Convert a list of CarrierIdentifiers into an array of Carrier.aidl
     * @param carriers List of CarrierIdentifiers
     * @return The converted array of Carriers
     */
    public static android.hardware.radio.sim.Carrier[] convertToHalCarrierRestrictionListAidl(
            List<CarrierIdentifier> carriers) {
        android.hardware.radio.sim.Carrier[] result =
                new android.hardware.radio.sim.Carrier[carriers.size()];
        for (int i = 0; i < carriers.size(); i++) {
            CarrierIdentifier ci = carriers.get(i);
            android.hardware.radio.sim.Carrier carrier = new android.hardware.radio.sim.Carrier();
            carrier.mcc = convertNullToEmptyString(ci.getMcc());
            carrier.mnc = convertNullToEmptyString(ci.getMnc());
            int matchType = CarrierIdentifier.MatchType.ALL;
            String matchData = null;
            if (!TextUtils.isEmpty(ci.getSpn())) {
                matchType = CarrierIdentifier.MatchType.SPN;
                matchData = ci.getSpn();
            } else if (!TextUtils.isEmpty(ci.getImsi())) {
                matchType = CarrierIdentifier.MatchType.IMSI_PREFIX;
                matchData = ci.getImsi();
            } else if (!TextUtils.isEmpty(ci.getGid1())) {
                matchType = CarrierIdentifier.MatchType.GID1;
                matchData = ci.getGid1();
            } else if (!TextUtils.isEmpty(ci.getGid2())) {
                matchType = CarrierIdentifier.MatchType.GID2;
                matchData = ci.getGid2();
            }
            carrier.matchType = matchType;
            carrier.matchData = convertNullToEmptyString(matchData);
            result[i] = carrier;
        }
        return result;
    }

    /**
     * Convert a list of CarrierIdentifiers into an array of CarrierInfo.aidl
     *
     * @param carriers List of CarrierIdentifiers
     * @return The converted array of CarrierInfos.
     */
    public static android.hardware.radio.sim.CarrierInfo[] convertToHalCarrierInfoListAidl(
            List<CarrierIdentifier> carriers) {
        android.hardware.radio.sim.CarrierInfo[] result =
                new android.hardware.radio.sim.CarrierInfo[carriers.size()];
        for (int i = 0; i < carriers.size(); i++) {
            CarrierIdentifier ci = carriers.get(i);
            android.hardware.radio.sim.CarrierInfo carrierInfo =
                    new android.hardware.radio.sim.CarrierInfo();
            carrierInfo.mcc = convertNullToEmptyString(ci.getMcc());
            carrierInfo.mnc = convertNullToEmptyString(ci.getMnc());
            carrierInfo.spn = ci.getSpn();
            carrierInfo.imsiPrefix = ci.getImsi();
            carrierInfo.gid1 = ci.getGid1();
            carrierInfo.gid2 = ci.getGid2();
            result[i] = carrierInfo;
        }
        return result;
    }

    /**
     * Convert to Dial defined in radio/1.0/types.hal
     * @param address Address
     * @param clirMode CLIR mode
     * @param uusInfo UUS info
     * @return The converted Dial
     */
    public static android.hardware.radio.V1_0.Dial convertToHalDial(String address, int clirMode,
            UUSInfo uusInfo) {
        android.hardware.radio.V1_0.Dial dial = new android.hardware.radio.V1_0.Dial();
        dial.address = convertNullToEmptyString(address);
        dial.clir = clirMode;
        if (uusInfo != null) {
            android.hardware.radio.V1_0.UusInfo info = new android.hardware.radio.V1_0.UusInfo();
            info.uusType = uusInfo.getType();
            info.uusDcs = uusInfo.getDcs();
            info.uusData = new String(uusInfo.getUserData());
            dial.uusInfo.add(info);
        }
        return dial;
    }

    /**
     * Convert to Dial.aidl
     * @param address Address
     * @param clirMode CLIR mode
     * @param uusInfo UUS info
     * @return The converted Dial.aidl
     */
    public static android.hardware.radio.voice.Dial convertToHalDialAidl(String address,
            int clirMode, UUSInfo uusInfo) {
        android.hardware.radio.voice.Dial dial = new android.hardware.radio.voice.Dial();
        dial.address = convertNullToEmptyString(address);
        dial.clir = clirMode;
        if (uusInfo != null) {
            android.hardware.radio.voice.UusInfo info = new android.hardware.radio.voice.UusInfo();
            info.uusType = uusInfo.getType();
            info.uusDcs = uusInfo.getDcs();
            info.uusData = new String(uusInfo.getUserData());
            dial.uusInfo = new android.hardware.radio.voice.UusInfo[] {info};
        } else {
            dial.uusInfo = new android.hardware.radio.voice.UusInfo[0];
        }
        return dial;
    }

    /**
     * Convert to SignalThresholdInfo defined in radio/1.5/types.hal
     * @param signalThresholdInfo Signal threshold info
     * @return The converted SignalThresholdInfo
     */
    public static android.hardware.radio.V1_5.SignalThresholdInfo convertToHalSignalThresholdInfo(
            SignalThresholdInfo signalThresholdInfo) {
        android.hardware.radio.V1_5.SignalThresholdInfo signalThresholdInfoHal =
                new android.hardware.radio.V1_5.SignalThresholdInfo();
        signalThresholdInfoHal.signalMeasurement = signalThresholdInfo.getSignalMeasurementType();
        signalThresholdInfoHal.hysteresisMs = signalThresholdInfo.getHysteresisMs();
        signalThresholdInfoHal.hysteresisDb = signalThresholdInfo.getHysteresisDb();
        signalThresholdInfoHal.thresholds = primitiveArrayToArrayList(
                signalThresholdInfo.getThresholds());
        signalThresholdInfoHal.isEnabled = signalThresholdInfo.isEnabled();
        return signalThresholdInfoHal;
    }

    /**
     * Convert to SignalThresholdInfo.aidl
     * @param signalThresholdInfo Signal threshold info
     * @return The converted SignalThresholdInfo
     */
    public static android.hardware.radio.network.SignalThresholdInfo
            convertToHalSignalThresholdInfoAidl(SignalThresholdInfo signalThresholdInfo) {
        android.hardware.radio.network.SignalThresholdInfo signalThresholdInfoHal =
                new android.hardware.radio.network.SignalThresholdInfo();
        signalThresholdInfoHal.signalMeasurement = signalThresholdInfo.getSignalMeasurementType();
        signalThresholdInfoHal.hysteresisMs = signalThresholdInfo.getHysteresisMs();
        signalThresholdInfoHal.hysteresisDb = signalThresholdInfo.getHysteresisDb();
        signalThresholdInfoHal.thresholds = signalThresholdInfo.getThresholds();
        signalThresholdInfoHal.isEnabled = signalThresholdInfo.isEnabled();
        signalThresholdInfoHal.ran = signalThresholdInfo.getRadioAccessNetworkType();
        return signalThresholdInfoHal;
    }

    /**
     * Convert to SmsWriteArgsStatus defined in radio/1.0/types.hal
     * @param status StatusOnIcc
     * @return The converted SmsWriteArgsStatus defined in radio/1.0/types.hal
     */
    public static int convertToHalSmsWriteArgsStatus(int status) {
        switch (status & 0x7) {
            case SmsManager.STATUS_ON_ICC_READ:
                return android.hardware.radio.V1_0.SmsWriteArgsStatus.REC_READ;
            case SmsManager.STATUS_ON_ICC_UNREAD:
                return android.hardware.radio.V1_0.SmsWriteArgsStatus.REC_UNREAD;
            case SmsManager.STATUS_ON_ICC_SENT:
                return android.hardware.radio.V1_0.SmsWriteArgsStatus.STO_SENT;
            case SmsManager.STATUS_ON_ICC_UNSENT:
                return android.hardware.radio.V1_0.SmsWriteArgsStatus.STO_UNSENT;
            default:
                return android.hardware.radio.V1_0.SmsWriteArgsStatus.REC_READ;
        }
    }

    /**
     * Convert to statuses defined in SmsWriteArgs.aidl
     * @param status StatusOnIcc
     * @return The converted statuses defined in SmsWriteArgs.aidl
     */
    public static int convertToHalSmsWriteArgsStatusAidl(int status) {
        switch (status & 0x7) {
            case SmsManager.STATUS_ON_ICC_READ:
                return android.hardware.radio.messaging.SmsWriteArgs.STATUS_REC_READ;
            case SmsManager.STATUS_ON_ICC_UNREAD:
                return android.hardware.radio.messaging.SmsWriteArgs.STATUS_REC_UNREAD;
            case SmsManager.STATUS_ON_ICC_SENT:
                return android.hardware.radio.messaging.SmsWriteArgs.STATUS_STO_SENT;
            case SmsManager.STATUS_ON_ICC_UNSENT:
                return android.hardware.radio.messaging.SmsWriteArgs.STATUS_STO_UNSENT;
            default:
                return android.hardware.radio.messaging.SmsWriteArgs.STATUS_REC_READ;
        }
    }

    /**
     * Convert a list of HardwareConfig defined in radio/1.0/types.hal to a list of HardwareConfig
     * @param hwListRil List of HardwareConfig defined in radio/1.0/types.hal
     * @return The converted list of HardwareConfig
     */
    public static ArrayList<HardwareConfig> convertHalHardwareConfigList(
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> hwListRil) {
        int num;
        ArrayList<HardwareConfig> response;
        HardwareConfig hw;

        num = hwListRil.size();
        response = new ArrayList<>(num);

        for (android.hardware.radio.V1_0.HardwareConfig hwRil : hwListRil) {
            int type = hwRil.type;
            switch(type) {
                case HardwareConfig.DEV_HARDWARE_TYPE_MODEM: {
                    hw = new HardwareConfig(type);
                    android.hardware.radio.V1_0.HardwareConfigModem hwModem = hwRil.modem.get(0);
                    hw.assignModem(hwRil.uuid, hwRil.state, hwModem.rilModel, hwModem.rat,
                            hwModem.maxVoice, hwModem.maxData, hwModem.maxStandby);
                    break;
                }
                case HardwareConfig.DEV_HARDWARE_TYPE_SIM: {
                    hw = new HardwareConfig(type);
                    hw.assignSim(hwRil.uuid, hwRil.state, hwRil.sim.get(0).modemUuid);
                    break;
                }
                default: {
                    throw new RuntimeException(
                            "RIL_REQUEST_GET_HARDWARE_CONFIG invalid hardware type:" + type);
                }
            }
            response.add(hw);
        }
        return response;
    }

    /**
     * Convert a list of HardwareConfig defined in HardwareConfig.aidl to a list of HardwareConfig
     * @param hwListRil List of HardwareConfig defined in HardwareConfig.aidl
     * @return The converted list of HardwareConfig
     */
    public static ArrayList<HardwareConfig> convertHalHardwareConfigList(
            android.hardware.radio.modem.HardwareConfig[] hwListRil) {
        ArrayList<HardwareConfig> response = new ArrayList<>(hwListRil.length);
        HardwareConfig hw;

        for (android.hardware.radio.modem.HardwareConfig hwRil : hwListRil) {
            int type = hwRil.type;
            switch (type) {
                case HardwareConfig.DEV_HARDWARE_TYPE_MODEM: {
                    hw = new HardwareConfig(type);
                    android.hardware.radio.modem.HardwareConfigModem hwModem = hwRil.modem[0];
                    hw.assignModem(hwRil.uuid, hwRil.state, hwModem.rilModel, hwModem.rat,
                            hwModem.maxVoiceCalls, hwModem.maxDataCalls, hwModem.maxStandby);
                    break;
                }
                case HardwareConfig.DEV_HARDWARE_TYPE_SIM: {
                    hw = new HardwareConfig(type);
                    hw.assignSim(hwRil.uuid, hwRil.state, hwRil.sim[0].modemUuid);
                    break;
                }
                default: {
                    throw new RuntimeException(
                            "RIL_REQUEST_GET_HARDWARE_CONFIG invalid hardware type:" + type);
                }
            }
            response.add(hw);
        }
        return response;
    }

    /**
     * Convert RadioCapability defined in radio/1.0/types.hal to RadioCapability
     * @param rc RadioCapability defined in radio/1.0/types.hal
     * @param ril RIL
     * @return The converted RadioCapability
     */
    public static RadioCapability convertHalRadioCapability(
            android.hardware.radio.V1_0.RadioCapability rc, RIL ril) {
        int session = rc.session;
        int phase = rc.phase;
        int rat = convertHalNetworkTypeBitMask(rc.raf);
        String logicModemUuid = rc.logicalModemUuid;
        int status = rc.status;

        ril.riljLog("convertHalRadioCapability: session=" + session + ", phase=" + phase + ", rat="
                + rat + ", logicModemUuid=" + logicModemUuid + ", status=" + status + ", rcRil.raf="
                + rc.raf);
        return new RadioCapability(ril.mPhoneId, session, phase, rat, logicModemUuid, status);
    }

    /**
     * Convert RadioCapability defined in RadioCapability.aidl to RadioCapability
     * @param rc RadioCapability defined in RadioCapability.aidl
     * @param ril RIL
     * @return The converted RadioCapability
     */
    public static RadioCapability convertHalRadioCapability(
            android.hardware.radio.modem.RadioCapability rc, RIL ril) {
        int session = rc.session;
        int phase = rc.phase;
        int rat = convertHalNetworkTypeBitMask(rc.raf);
        String logicModemUuid = rc.logicalModemUuid;
        int status = rc.status;

        ril.riljLog("convertHalRadioCapability: session=" + session + ", phase=" + phase + ", rat="
                + rat + ", logicModemUuid=" + logicModemUuid + ", status=" + status + ", rcRil.raf="
                + rc.raf);
        return new RadioCapability(ril.mPhoneId, session, phase, rat, logicModemUuid, status);
    }

    /**
     * Convert LinkCapacityEstimate defined in radio/1.2, 1.6/types.hal to
     * a list of LinkCapacityEstimates
     * @param lceObj LinkCapacityEstimate defined in radio/1.2, 1.6/types.hal
     * @return The converted list of LinkCapacityEstimates
     */
    public static List<LinkCapacityEstimate> convertHalLinkCapacityEstimate(Object lceObj) {
        final List<LinkCapacityEstimate> lceList = new ArrayList<>();
        if (lceObj == null) return lceList;
        if (lceObj instanceof android.hardware.radio.V1_2.LinkCapacityEstimate) {
            android.hardware.radio.V1_2.LinkCapacityEstimate lce =
                    (android.hardware.radio.V1_2.LinkCapacityEstimate) lceObj;
            lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_COMBINED,
                    lce.downlinkCapacityKbps, lce.uplinkCapacityKbps));
        } else if (lceObj instanceof android.hardware.radio.V1_6.LinkCapacityEstimate) {
            android.hardware.radio.V1_6.LinkCapacityEstimate lce =
                    (android.hardware.radio.V1_6.LinkCapacityEstimate) lceObj;
            int primaryDownlinkCapacityKbps = lce.downlinkCapacityKbps;
            int primaryUplinkCapacityKbps = lce.uplinkCapacityKbps;
            if (primaryDownlinkCapacityKbps != LinkCapacityEstimate.INVALID
                    && lce.secondaryDownlinkCapacityKbps != LinkCapacityEstimate.INVALID) {
                primaryDownlinkCapacityKbps =
                        lce.downlinkCapacityKbps - lce.secondaryDownlinkCapacityKbps;
            }
            if (primaryUplinkCapacityKbps != LinkCapacityEstimate.INVALID
                    && lce.secondaryUplinkCapacityKbps != LinkCapacityEstimate.INVALID) {
                primaryUplinkCapacityKbps =
                        lce.uplinkCapacityKbps - lce.secondaryUplinkCapacityKbps;
            }
            lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_PRIMARY,
                    primaryDownlinkCapacityKbps, primaryUplinkCapacityKbps));
            lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_SECONDARY,
                    lce.secondaryDownlinkCapacityKbps, lce.secondaryUplinkCapacityKbps));
        }
        return lceList;
    }

    /**
     * Convert LinkCapacityEstimate defined in LinkCapacityEstimate.aidl to a list of
     * LinkCapacityEstimates
     * @param lce LinkCapacityEstimate defined in LinkCapacityEstimate.aidl
     * @return The converted list of LinkCapacityEstimates
     */
    public static List<LinkCapacityEstimate> convertHalLinkCapacityEstimate(
            android.hardware.radio.network.LinkCapacityEstimate lce) {
        final List<LinkCapacityEstimate> lceList = new ArrayList<>();
        int primaryDownlinkCapacityKbps = lce.downlinkCapacityKbps;
        int primaryUplinkCapacityKbps = lce.uplinkCapacityKbps;
        if (primaryDownlinkCapacityKbps != LinkCapacityEstimate.INVALID
                && lce.secondaryDownlinkCapacityKbps != LinkCapacityEstimate.INVALID) {
            primaryDownlinkCapacityKbps =
                    lce.downlinkCapacityKbps - lce.secondaryDownlinkCapacityKbps;
        }
        if (primaryUplinkCapacityKbps != LinkCapacityEstimate.INVALID
                && lce.secondaryUplinkCapacityKbps != LinkCapacityEstimate.INVALID) {
            primaryUplinkCapacityKbps =
                    lce.uplinkCapacityKbps - lce.secondaryUplinkCapacityKbps;
        }
        lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_PRIMARY,
                primaryDownlinkCapacityKbps, primaryUplinkCapacityKbps));
        lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_SECONDARY,
                lce.secondaryDownlinkCapacityKbps, lce.secondaryUplinkCapacityKbps));
        return lceList;
    }


    /**
     * Convert a list of CellInfo defined in radio/1.4, 1.5, 1.6/types.hal to a list of
     * CellInfos
     * @param records List of CellInfo defined in radio/1.4, 1.5, 1.6/types.hal
     * @return The converted list of CellInfos
     */
    public static ArrayList<CellInfo> convertHalCellInfoList(ArrayList<Object> records) {
        ArrayList<CellInfo> response = new ArrayList<>(records.size());
        if (records.isEmpty()) return response;
        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (Object obj : records) {
            response.add(convertHalCellInfo(obj, nanotime));
        }
        return response;
    }

    /**
     * Convert a list of CellInfo defined in CellInfo.aidl to a list of CellInfos
     * @param records List of CellInfo defined in CellInfo.aidl
     * @return The converted list of CellInfos
     */
    public static ArrayList<CellInfo> convertHalCellInfoList(
            android.hardware.radio.network.CellInfo[] records) {
        ArrayList<CellInfo> response = new ArrayList<>(records.length);
        if (records.length == 0) return response;
        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (android.hardware.radio.network.CellInfo ci : records) {
            response.add(convertHalCellInfo(ci, nanotime));
        }
        return response;
    }

    /**
     * Convert a CellInfo defined in radio/1.4, 1.5, 1.6/types.hal to CellInfo
     * @param cellInfo CellInfo defined in radio/1.4, 1.5, 1.6/types.hal
     * @param nanotime time the CellInfo was created
     * @return The converted CellInfo
     */
    private static CellInfo convertHalCellInfo(Object cellInfo, long nanotime) {
        if (cellInfo == null) return null;
        int type;
        int connectionStatus;
        boolean registered;
        CellIdentityGsm gsmCi = null;
        CellSignalStrengthGsm gsmSs = null;
        CellIdentityCdma cdmaCi = null;
        CellSignalStrengthCdma cdmaSs = null;
        CellIdentityLte lteCi = null;
        CellSignalStrengthLte lteSs = null;
        CellConfigLte lteCc = null;
        CellIdentityWcdma wcdmaCi = null;
        CellSignalStrengthWcdma wcdmaSs = null;
        CellIdentityTdscdma tdscdmaCi = null;
        CellSignalStrengthTdscdma tdscdmaSs = null;
        CellIdentityNr nrCi = null;
        CellSignalStrengthNr nrSs = null;
        if (cellInfo instanceof android.hardware.radio.V1_4.CellInfo) {
            final android.hardware.radio.V1_4.CellInfo record =
                    (android.hardware.radio.V1_4.CellInfo) cellInfo;
            connectionStatus = record.connectionStatus;
            registered = record.isRegistered;
            switch (record.info.getDiscriminator()) {
                case android.hardware.radio.V1_4.CellInfo.Info.hidl_discriminator.gsm:
                    type = CellInfo.TYPE_GSM;
                    android.hardware.radio.V1_2.CellInfoGsm gsm = record.info.gsm();
                    gsmCi = convertHalCellIdentityGsm(gsm.cellIdentityGsm);
                    gsmSs = convertHalGsmSignalStrength(gsm.signalStrengthGsm);
                    break;
                case android.hardware.radio.V1_4.CellInfo.Info.hidl_discriminator.cdma:
                    type = CellInfo.TYPE_CDMA;
                    android.hardware.radio.V1_2.CellInfoCdma cdma = record.info.cdma();
                    cdmaCi = convertHalCellIdentityCdma(cdma.cellIdentityCdma);
                    cdmaSs = convertHalCdmaSignalStrength(
                            cdma.signalStrengthCdma, cdma.signalStrengthEvdo);
                    break;
                case android.hardware.radio.V1_4.CellInfo.Info.hidl_discriminator.lte:
                    type = CellInfo.TYPE_LTE;
                    android.hardware.radio.V1_4.CellInfoLte lte = record.info.lte();
                    lteCi = convertHalCellIdentityLte(lte.base.cellIdentityLte);
                    lteSs = convertHalLteSignalStrength(lte.base.signalStrengthLte);
                    lteCc = new CellConfigLte(lte.cellConfig.isEndcAvailable);
                    break;
                case android.hardware.radio.V1_4.CellInfo.Info.hidl_discriminator.wcdma:
                    type = CellInfo.TYPE_WCDMA;
                    android.hardware.radio.V1_2.CellInfoWcdma wcdma = record.info.wcdma();
                    wcdmaCi = convertHalCellIdentityWcdma(wcdma.cellIdentityWcdma);
                    wcdmaSs = convertHalWcdmaSignalStrength(wcdma.signalStrengthWcdma);
                    break;
                case android.hardware.radio.V1_4.CellInfo.Info.hidl_discriminator.tdscdma:
                    type = CellInfo.TYPE_TDSCDMA;
                    android.hardware.radio.V1_2.CellInfoTdscdma tdscdma = record.info.tdscdma();
                    tdscdmaCi = convertHalCellIdentityTdscdma(tdscdma.cellIdentityTdscdma);
                    tdscdmaSs = convertHalTdscdmaSignalStrength(tdscdma.signalStrengthTdscdma);
                    break;
                case android.hardware.radio.V1_4.CellInfo.Info.hidl_discriminator.nr:
                    type = CellInfo.TYPE_NR;
                    android.hardware.radio.V1_4.CellInfoNr nr = record.info.nr();
                    nrCi = convertHalCellIdentityNr(nr.cellidentity);
                    nrSs = convertHalNrSignalStrength(nr.signalStrength);
                    break;
                default: return null;
            }
        } else if (cellInfo instanceof android.hardware.radio.V1_5.CellInfo) {
            final android.hardware.radio.V1_5.CellInfo record =
                    (android.hardware.radio.V1_5.CellInfo) cellInfo;
            connectionStatus = record.connectionStatus;
            registered = record.registered;
            switch (record.ratSpecificInfo.getDiscriminator()) {
                case android.hardware.radio.V1_5.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.gsm:
                    type = CellInfo.TYPE_GSM;
                    android.hardware.radio.V1_5.CellInfoGsm gsm = record.ratSpecificInfo.gsm();
                    gsmCi = convertHalCellIdentityGsm(gsm.cellIdentityGsm);
                    gsmSs = convertHalGsmSignalStrength(gsm.signalStrengthGsm);
                    break;
                case android.hardware.radio.V1_5.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.cdma:
                    type = CellInfo.TYPE_CDMA;
                    android.hardware.radio.V1_2.CellInfoCdma cdma = record.ratSpecificInfo.cdma();
                    cdmaCi = convertHalCellIdentityCdma(cdma.cellIdentityCdma);
                    cdmaSs = convertHalCdmaSignalStrength(
                            cdma.signalStrengthCdma, cdma.signalStrengthEvdo);
                    break;
                case android.hardware.radio.V1_5.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.lte:
                    type = CellInfo.TYPE_LTE;
                    android.hardware.radio.V1_5.CellInfoLte lte = record.ratSpecificInfo.lte();
                    lteCi = convertHalCellIdentityLte(lte.cellIdentityLte);
                    lteSs = convertHalLteSignalStrength(lte.signalStrengthLte);
                    lteCc = new CellConfigLte();
                    break;
                case android.hardware.radio.V1_5.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.wcdma:
                    type = CellInfo.TYPE_WCDMA;
                    android.hardware.radio.V1_5.CellInfoWcdma wcdma =
                            record.ratSpecificInfo.wcdma();
                    wcdmaCi = convertHalCellIdentityWcdma(wcdma.cellIdentityWcdma);
                    wcdmaSs = convertHalWcdmaSignalStrength(wcdma.signalStrengthWcdma);
                    break;
                case android.hardware.radio.V1_5.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.tdscdma:
                    type = CellInfo.TYPE_TDSCDMA;
                    android.hardware.radio.V1_5.CellInfoTdscdma tdscdma =
                            record.ratSpecificInfo.tdscdma();
                    tdscdmaCi = convertHalCellIdentityTdscdma(tdscdma.cellIdentityTdscdma);
                    tdscdmaSs = convertHalTdscdmaSignalStrength(tdscdma.signalStrengthTdscdma);
                    break;
                case android.hardware.radio.V1_5.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.nr:
                    type = CellInfo.TYPE_NR;
                    android.hardware.radio.V1_5.CellInfoNr nr = record.ratSpecificInfo.nr();
                    nrCi = convertHalCellIdentityNr(nr.cellIdentityNr);
                    nrSs = convertHalNrSignalStrength(nr.signalStrengthNr);
                    break;
                default: return null;
            }
        } else if (cellInfo instanceof android.hardware.radio.V1_6.CellInfo) {
            final android.hardware.radio.V1_6.CellInfo record =
                    (android.hardware.radio.V1_6.CellInfo) cellInfo;
            connectionStatus = record.connectionStatus;
            registered = record.registered;
            switch (record.ratSpecificInfo.getDiscriminator()) {
                case android.hardware.radio.V1_6.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.gsm:
                    type = CellInfo.TYPE_GSM;
                    android.hardware.radio.V1_5.CellInfoGsm gsm = record.ratSpecificInfo.gsm();
                    gsmCi = convertHalCellIdentityGsm(gsm.cellIdentityGsm);
                    gsmSs = convertHalGsmSignalStrength(gsm.signalStrengthGsm);
                    break;
                case android.hardware.radio.V1_6.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.cdma:
                    type = CellInfo.TYPE_CDMA;
                    android.hardware.radio.V1_2.CellInfoCdma cdma = record.ratSpecificInfo.cdma();
                    cdmaCi = convertHalCellIdentityCdma(cdma.cellIdentityCdma);
                    cdmaSs = convertHalCdmaSignalStrength(
                            cdma.signalStrengthCdma, cdma.signalStrengthEvdo);
                    break;
                case android.hardware.radio.V1_6.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.lte:
                    type = CellInfo.TYPE_LTE;
                    android.hardware.radio.V1_6.CellInfoLte lte = record.ratSpecificInfo.lte();
                    lteCi = convertHalCellIdentityLte(lte.cellIdentityLte);
                    lteSs = convertHalLteSignalStrength(lte.signalStrengthLte);
                    lteCc = new CellConfigLte();
                    break;
                case android.hardware.radio.V1_6.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.wcdma:
                    type = CellInfo.TYPE_WCDMA;
                    android.hardware.radio.V1_5.CellInfoWcdma wcdma =
                            record.ratSpecificInfo.wcdma();
                    wcdmaCi = convertHalCellIdentityWcdma(wcdma.cellIdentityWcdma);
                    wcdmaSs = convertHalWcdmaSignalStrength(wcdma.signalStrengthWcdma);
                    break;
                case android.hardware.radio.V1_6.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.tdscdma:
                    type = CellInfo.TYPE_TDSCDMA;
                    android.hardware.radio.V1_5.CellInfoTdscdma tdscdma =
                            record.ratSpecificInfo.tdscdma();
                    tdscdmaCi = convertHalCellIdentityTdscdma(tdscdma.cellIdentityTdscdma);
                    tdscdmaSs = convertHalTdscdmaSignalStrength(tdscdma.signalStrengthTdscdma);
                    break;
                case android.hardware.radio.V1_6.CellInfo
                        .CellInfoRatSpecificInfo.hidl_discriminator.nr:
                    type = CellInfo.TYPE_NR;
                    android.hardware.radio.V1_6.CellInfoNr nr = record.ratSpecificInfo.nr();
                    nrCi = convertHalCellIdentityNr(nr.cellIdentityNr);
                    nrSs = convertHalNrSignalStrength(nr.signalStrengthNr);
                    break;
                default: return null;
            }
        } else {
            return null;
        }

        switch (type) {
            case CellInfo.TYPE_GSM:
                return new CellInfoGsm(connectionStatus, registered, nanotime, gsmCi, gsmSs);
            case CellInfo.TYPE_CDMA:
                return new CellInfoCdma(connectionStatus, registered, nanotime, cdmaCi, cdmaSs);
            case CellInfo.TYPE_LTE:
                return new CellInfoLte(connectionStatus, registered, nanotime, lteCi, lteSs, lteCc);
            case CellInfo.TYPE_WCDMA:
                return new CellInfoWcdma(connectionStatus, registered, nanotime, wcdmaCi, wcdmaSs);
            case CellInfo.TYPE_TDSCDMA:
                return new CellInfoTdscdma(connectionStatus, registered, nanotime, tdscdmaCi,
                        tdscdmaSs);
            case CellInfo.TYPE_NR:
                return new CellInfoNr(connectionStatus, registered, nanotime, nrCi, nrSs);
            case CellInfo.TYPE_UNKNOWN:
            default:
                return null;
        }
    }

    /**
     * Convert a CellInfo defined in CellInfo.aidl to CellInfo
     * @param cellInfo CellInfo defined in CellInfo.aidl
     * @param nanotime time the CellInfo was created
     * @return The converted CellInfo
     */
    private static CellInfo convertHalCellInfo(android.hardware.radio.network.CellInfo cellInfo,
            long nanotime) {
        if (cellInfo == null) return null;
        int connectionStatus = cellInfo.connectionStatus;
        boolean registered = cellInfo.registered;
        switch (cellInfo.ratSpecificInfo.getTag()) {
            case android.hardware.radio.network.CellInfoRatSpecificInfo.gsm:
                android.hardware.radio.network.CellInfoGsm gsm = cellInfo.ratSpecificInfo.getGsm();
                return new CellInfoGsm(connectionStatus, registered, nanotime,
                        convertHalCellIdentityGsm(gsm.cellIdentityGsm),
                        convertHalGsmSignalStrength(gsm.signalStrengthGsm));
            case android.hardware.radio.network.CellInfoRatSpecificInfo.cdma:
                android.hardware.radio.network.CellInfoCdma cdma =
                        cellInfo.ratSpecificInfo.getCdma();
                return new CellInfoCdma(connectionStatus, registered, nanotime,
                        convertHalCellIdentityCdma(cdma.cellIdentityCdma),
                        convertHalCdmaSignalStrength(cdma.signalStrengthCdma,
                                cdma.signalStrengthEvdo));
            case android.hardware.radio.network.CellInfoRatSpecificInfo.lte:
                android.hardware.radio.network.CellInfoLte lte = cellInfo.ratSpecificInfo.getLte();
                return new CellInfoLte(connectionStatus, registered, nanotime,
                        convertHalCellIdentityLte(lte.cellIdentityLte),
                        convertHalLteSignalStrength(lte.signalStrengthLte), new CellConfigLte());
            case android.hardware.radio.network.CellInfoRatSpecificInfo.wcdma:
                android.hardware.radio.network.CellInfoWcdma wcdma =
                        cellInfo.ratSpecificInfo.getWcdma();
                return new CellInfoWcdma(connectionStatus, registered, nanotime,
                        convertHalCellIdentityWcdma(wcdma.cellIdentityWcdma),
                        convertHalWcdmaSignalStrength(wcdma.signalStrengthWcdma));
            case android.hardware.radio.network.CellInfoRatSpecificInfo.tdscdma:
                android.hardware.radio.network.CellInfoTdscdma tdscdma =
                        cellInfo.ratSpecificInfo.getTdscdma();
                return new CellInfoTdscdma(connectionStatus, registered, nanotime,
                        convertHalCellIdentityTdscdma(tdscdma.cellIdentityTdscdma),
                        convertHalTdscdmaSignalStrength(tdscdma.signalStrengthTdscdma));
            case android.hardware.radio.network.CellInfoRatSpecificInfo.nr:
                android.hardware.radio.network.CellInfoNr nr = cellInfo.ratSpecificInfo.getNr();
                return new CellInfoNr(connectionStatus, registered, nanotime,
                        convertHalCellIdentityNr(nr.cellIdentityNr),
                        convertHalNrSignalStrength(nr.signalStrengthNr));
            default:
                return null;
        }
    }

    /**
     * Convert a CellIdentity defined in radio/1.2, 1.5/types.hal to CellIdentity
     * @param halCi CellIdentity defined in radio/1.2, 1.5/types.hal
     * @return The converted CellIdentity
     */
    public static CellIdentity convertHalCellIdentity(Object halCi) {
        if (halCi == null) return null;
        if (halCi instanceof android.hardware.radio.V1_2.CellIdentity) {
            android.hardware.radio.V1_2.CellIdentity ci =
                    (android.hardware.radio.V1_2.CellIdentity) halCi;
            switch (ci.cellInfoType) {
                case CellInfo.TYPE_GSM:
                    if (ci.cellIdentityGsm.size() == 1) {
                        return convertHalCellIdentityGsm(ci.cellIdentityGsm.get(0));
                    }
                    break;
                case CellInfo.TYPE_CDMA:
                    if (ci.cellIdentityCdma.size() == 1) {
                        return convertHalCellIdentityCdma(ci.cellIdentityCdma.get(0));
                    }
                    break;
                case CellInfo.TYPE_LTE:
                    if (ci.cellIdentityLte.size() == 1) {
                        return convertHalCellIdentityLte(ci.cellIdentityLte.get(0));
                    }
                    break;
                case CellInfo.TYPE_WCDMA:
                    if (ci.cellIdentityWcdma.size() == 1) {
                        return convertHalCellIdentityWcdma(ci.cellIdentityWcdma.get(0));
                    }
                    break;
                case CellInfo.TYPE_TDSCDMA:
                    if (ci.cellIdentityTdscdma.size() == 1) {
                        return convertHalCellIdentityTdscdma(ci.cellIdentityTdscdma.get(0));
                    }
                    break;
            }
        } else if (halCi instanceof android.hardware.radio.V1_5.CellIdentity) {
            android.hardware.radio.V1_5.CellIdentity ci =
                    (android.hardware.radio.V1_5.CellIdentity) halCi;
            switch (ci.getDiscriminator()) {
                case android.hardware.radio.V1_5.CellIdentity.hidl_discriminator.gsm:
                    return convertHalCellIdentityGsm(ci.gsm());
                case android.hardware.radio.V1_5.CellIdentity.hidl_discriminator.cdma:
                    return convertHalCellIdentityCdma(ci.cdma());
                case android.hardware.radio.V1_5.CellIdentity.hidl_discriminator.lte:
                    return convertHalCellIdentityLte(ci.lte());
                case android.hardware.radio.V1_5.CellIdentity.hidl_discriminator.wcdma:
                    return convertHalCellIdentityWcdma(ci.wcdma());
                case android.hardware.radio.V1_5.CellIdentity.hidl_discriminator.tdscdma:
                    return convertHalCellIdentityTdscdma(ci.tdscdma());
                case android.hardware.radio.V1_5.CellIdentity.hidl_discriminator.nr:
                    return convertHalCellIdentityNr(ci.nr());
            }
        }
        return null;
    }

    /**
     * Convert a CellIdentity defined in CellIdentity.aidl to CellInfo
     * @param ci CellIdentity defined in CellIdentity.aidl
     * @return The converted CellIdentity
     */
    public static CellIdentity convertHalCellIdentity(
            android.hardware.radio.network.CellIdentity ci) {
        if (ci == null) return null;
        switch (ci.getTag()) {
            case android.hardware.radio.network.CellIdentity.gsm:
                return convertHalCellIdentityGsm(ci.getGsm());
            case android.hardware.radio.network.CellIdentity.cdma:
                return convertHalCellIdentityCdma(ci.getCdma());
            case android.hardware.radio.network.CellIdentity.lte:
                return convertHalCellIdentityLte(ci.getLte());
            case android.hardware.radio.network.CellIdentity.wcdma:
                return convertHalCellIdentityWcdma(ci.getWcdma());
            case android.hardware.radio.network.CellIdentity.tdscdma:
                return convertHalCellIdentityTdscdma(ci.getTdscdma());
            case android.hardware.radio.network.CellIdentity.nr:
                return convertHalCellIdentityNr(ci.getNr());
            default: return null;
        }
    }

    /**
     * Convert a CellIdentityGsm defined in radio/1.2, 1.5/types.hal to CellIdentityGsm
     * @param gsm CellIdentityGsm defined in radio/1.2, 1.5/types.hal
     * @return The converted CellIdentityGsm
     */
    public static CellIdentityGsm convertHalCellIdentityGsm(Object gsm) {
        if (gsm == null) return null;
        if (gsm instanceof android.hardware.radio.V1_2.CellIdentityGsm) {
            android.hardware.radio.V1_2.CellIdentityGsm ci =
                    (android.hardware.radio.V1_2.CellIdentityGsm) gsm;
            return new CellIdentityGsm(ci.base.lac, ci.base.cid, ci.base.arfcn,
                    ci.base.bsic == (byte) 0xFF ? CellInfo.UNAVAILABLE : ci.base.bsic, ci.base.mcc,
                    ci.base.mnc, ci.operatorNames.alphaLong, ci.operatorNames.alphaShort,
                    new ArraySet<>());
        } else if (gsm instanceof android.hardware.radio.V1_5.CellIdentityGsm) {
            android.hardware.radio.V1_5.CellIdentityGsm ci =
                    (android.hardware.radio.V1_5.CellIdentityGsm) gsm;
            return new CellIdentityGsm(ci.base.base.lac, ci.base.base.cid, ci.base.base.arfcn,
                    ci.base.base.bsic == (byte) 0xFF ? CellInfo.UNAVAILABLE
                            : ci.base.base.bsic, ci.base.base.mcc, ci.base.base.mnc,
                    ci.base.operatorNames.alphaLong, ci.base.operatorNames.alphaShort,
                    ci.additionalPlmns);
        } else {
            return null;
        }
    }

    /**
     * Convert a CellIdentityGsm defined in CellIdentityGsm.aidl to CellIdentityGsm
     * @param cid CellIdentityGsm defined in CellIdentityGsm.aidl
     * @return The converted CellIdentityGsm
     */
    public static CellIdentityGsm convertHalCellIdentityGsm(
            android.hardware.radio.network.CellIdentityGsm cid) {
        return new CellIdentityGsm(cid.lac, cid.cid, cid.arfcn,
                cid.bsic == (byte) 0xFF ? CellInfo.UNAVAILABLE : cid.bsic, cid.mcc, cid.mnc,
                cid.operatorNames.alphaLong, cid.operatorNames.alphaShort, new ArraySet<>());
    }

    /**
     * Convert a CellIdentityCdma defined in radio/1.2/types.hal to CellIdentityCdma
     * @param cdma CellIdentityCdma defined in radio/1.2/types.hal
     * @return The converted CellIdentityCdma
     */
    public static CellIdentityCdma convertHalCellIdentityCdma(Object cdma) {
        if (cdma == null) return null;
        if (cdma instanceof android.hardware.radio.V1_2.CellIdentityCdma) {
            android.hardware.radio.V1_2.CellIdentityCdma ci =
                    (android.hardware.radio.V1_2.CellIdentityCdma) cdma;
            return new CellIdentityCdma(ci.base.networkId, ci.base.systemId, ci.base.baseStationId,
                    ci.base.longitude, ci.base.latitude, ci.operatorNames.alphaLong,
                    ci.operatorNames.alphaShort);
        } else {
            return null;
        }
    }

    /**
     * Convert a CellIdentityCdma defined in CellIdentityCdma.aidl to CellIdentityCdma
     * @param cid CellIdentityCdma defined in CelIdentityCdma.aidl
     * @return The converted CellIdentityCdma
     */
    public static CellIdentityCdma convertHalCellIdentityCdma(
            android.hardware.radio.network.CellIdentityCdma cid) {
        return new CellIdentityCdma(cid.networkId, cid.systemId, cid.baseStationId, cid.longitude,
                cid.latitude, cid.operatorNames.alphaLong, cid.operatorNames.alphaShort);
    }

    /**
     * Convert a CellIdentityLte defined in radio/1.2, 1.5/types.hal to CellIdentityLte
     * @param lte CellIdentityLte defined in radio/1.2, 1.5/types.hal
     * @return The converted CellIdentityLte
     */
    public static CellIdentityLte convertHalCellIdentityLte(Object lte) {
        if (lte == null) return null;
        if (lte instanceof android.hardware.radio.V1_2.CellIdentityLte) {
            android.hardware.radio.V1_2.CellIdentityLte ci =
                    (android.hardware.radio.V1_2.CellIdentityLte) lte;
            return new CellIdentityLte(ci.base.ci, ci.base.pci, ci.base.tac, ci.base.earfcn,
                    new int[] {}, ci.bandwidth, ci.base.mcc, ci.base.mnc,
                    ci.operatorNames.alphaLong, ci.operatorNames.alphaShort, new ArraySet<>(),
                    null);
        } else if (lte instanceof android.hardware.radio.V1_5.CellIdentityLte) {
            android.hardware.radio.V1_5.CellIdentityLte ci =
                    (android.hardware.radio.V1_5.CellIdentityLte) lte;
            return new CellIdentityLte(ci.base.base.ci, ci.base.base.pci, ci.base.base.tac,
                    ci.base.base.earfcn, ci.bands.stream().mapToInt(Integer::intValue).toArray(),
                    ci.base.bandwidth, ci.base.base.mcc, ci.base.base.mnc,
                    ci.base.operatorNames.alphaLong, ci.base.operatorNames.alphaShort,
                    ci.additionalPlmns, convertHalClosedSubscriberGroupInfo(ci.optionalCsgInfo));
        } else {
            return null;
        }
    }

    /**
     * Convert a CellIdentityLte defined in CellIdentityLte.aidl to CellIdentityLte
     * @param cid CellIdentityLte defined in CellIdentityLte.aidl
     * @return The converted CellIdentityLte
     */
    public static CellIdentityLte convertHalCellIdentityLte(
            android.hardware.radio.network.CellIdentityLte cid) {
        return new CellIdentityLte(cid.ci, cid.pci, cid.tac, cid.earfcn, cid.bands, cid.bandwidth,
                cid.mcc, cid.mnc, cid.operatorNames.alphaLong, cid.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.additionalPlmns),
                convertHalClosedSubscriberGroupInfo(cid.csgInfo));
    }

    /**
     * Convert a CellIdentityWcdma defined in radio/1.2, 1.5/types.hal to CellIdentityWcdma
     * @param wcdma CellIdentityWcdma defined in radio/1.2, 1.5/types.hal
     * @return The converted CellIdentityWcdma
     */
    public static CellIdentityWcdma convertHalCellIdentityWcdma(Object wcdma) {
        if (wcdma == null) return null;
        if (wcdma instanceof android.hardware.radio.V1_2.CellIdentityWcdma) {
            android.hardware.radio.V1_2.CellIdentityWcdma ci =
                    (android.hardware.radio.V1_2.CellIdentityWcdma) wcdma;
            return new CellIdentityWcdma(ci.base.lac, ci.base.cid, ci.base.psc, ci.base.uarfcn,
                    ci.base.mcc, ci.base.mnc, ci.operatorNames.alphaLong,
                    ci.operatorNames.alphaShort, new ArraySet<>(), null);
        } else if (wcdma instanceof android.hardware.radio.V1_5.CellIdentityWcdma) {
            android.hardware.radio.V1_5.CellIdentityWcdma ci =
                    (android.hardware.radio.V1_5.CellIdentityWcdma) wcdma;
            return new CellIdentityWcdma(ci.base.base.lac, ci.base.base.cid, ci.base.base.psc,
                    ci.base.base.uarfcn, ci.base.base.mcc, ci.base.base.mnc,
                    ci.base.operatorNames.alphaLong, ci.base.operatorNames.alphaShort,
                    ci.additionalPlmns, convertHalClosedSubscriberGroupInfo(ci.optionalCsgInfo));
        } else {
            return null;
        }
    }

    /**
     * Convert a CellIdentityWcdma defined in CellIdentityWcdma.aidl to CellIdentityWcdma
     * @param cid CellIdentityWcdma defined in CellIdentityWcdma.aidl
     * @return The converted CellIdentityWcdma
     */
    public static CellIdentityWcdma convertHalCellIdentityWcdma(
            android.hardware.radio.network.CellIdentityWcdma cid) {
        return new CellIdentityWcdma(cid.lac, cid.cid, cid.psc, cid.uarfcn, cid.mcc, cid.mnc,
                cid.operatorNames.alphaLong, cid.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.additionalPlmns),
                convertHalClosedSubscriberGroupInfo(cid.csgInfo));
    }

    /**
     * Convert a CellIdentityTdscdma defined in radio/1.2, 1.5/types.hal to CellIdentityTdscdma
     * @param tdscdma CellIdentityTdscdma defined in radio/1.2, 1.5/types.hal
     * @return The converted CellIdentityTdscdma
     */
    public static CellIdentityTdscdma convertHalCellIdentityTdscdma(Object tdscdma) {
        if (tdscdma == null) return null;
        if (tdscdma instanceof android.hardware.radio.V1_2.CellIdentityTdscdma) {
            android.hardware.radio.V1_2.CellIdentityTdscdma ci =
                    (android.hardware.radio.V1_2.CellIdentityTdscdma) tdscdma;
            return new CellIdentityTdscdma(ci.base.mcc, ci.base.mnc, ci.base.lac, ci.base.cid,
                    ci.base.cpid, ci.uarfcn, ci.operatorNames.alphaLong,
                    ci.operatorNames.alphaShort, Collections.emptyList(), null);
        } else if (tdscdma instanceof android.hardware.radio.V1_5.CellIdentityTdscdma) {
            android.hardware.radio.V1_5.CellIdentityTdscdma ci =
                    (android.hardware.radio.V1_5.CellIdentityTdscdma) tdscdma;
            return new CellIdentityTdscdma(ci.base.base.mcc, ci.base.base.mnc, ci.base.base.lac,
                    ci.base.base.cid, ci.base.base.cpid, ci.base.uarfcn,
                    ci.base.operatorNames.alphaLong, ci.base.operatorNames.alphaShort,
                    ci.additionalPlmns, convertHalClosedSubscriberGroupInfo(ci.optionalCsgInfo));
        } else {
            return null;
        }
    }

    /**
     * Convert a CellIdentityTdscdma defined in CellIdentityTdscdma.aidl to CellIdentityTdscdma
     * @param cid CellIdentityTdscdma defined in radio/1.2, 1.5/types.hal
     * @return The converted CellIdentityTdscdma
     */
    public static CellIdentityTdscdma convertHalCellIdentityTdscdma(
            android.hardware.radio.network.CellIdentityTdscdma cid) {
        return new CellIdentityTdscdma(cid.mcc, cid.mnc, cid.lac, cid.cid, cid.cpid, cid.uarfcn,
                cid.operatorNames.alphaLong, cid.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.additionalPlmns),
                convertHalClosedSubscriberGroupInfo(cid.csgInfo));
    }

    /**
     * Convert a CellIdentityNr defined in radio/1.4, 1.5/types.hal to CellIdentityNr
     * @param nr CellIdentityNr defined in radio/1.4 1.5/types.hal
     * @return The converted CellIdentityNr
     */
    public static CellIdentityNr convertHalCellIdentityNr(Object nr) {
        if (nr == null) return null;
        if (nr instanceof android.hardware.radio.V1_4.CellIdentityNr) {
            android.hardware.radio.V1_4.CellIdentityNr ci =
                    (android.hardware.radio.V1_4.CellIdentityNr) nr;
            return new CellIdentityNr(ci.pci, ci.tac, ci.nrarfcn, new int[] {}, ci.mcc, ci.mnc,
                    ci.nci, ci.operatorNames.alphaLong, ci.operatorNames.alphaShort,
                    new ArraySet<>());
        } else if (nr instanceof android.hardware.radio.V1_5.CellIdentityNr) {
            android.hardware.radio.V1_5.CellIdentityNr ci =
                    (android.hardware.radio.V1_5.CellIdentityNr) nr;
            return new CellIdentityNr(ci.base.pci, ci.base.tac, ci.base.nrarfcn,
                    ci.bands.stream().mapToInt(Integer::intValue).toArray(), ci.base.mcc,
                    ci.base.mnc, ci.base.nci, ci.base.operatorNames.alphaLong,
                    ci.base.operatorNames.alphaShort, ci.additionalPlmns);
        } else {
            return null;
        }
    }

    /**
     * Convert a CellIdentityNr defined in CellIdentityNr.aidl to CellIdentityNr
     * @param cid CellIdentityNr defined in CellIdentityNr.aidl
     * @return The converted CellIdentityNr
     */
    public static CellIdentityNr convertHalCellIdentityNr(
            android.hardware.radio.network.CellIdentityNr cid) {
        return new CellIdentityNr(cid.pci, cid.tac, cid.nrarfcn, cid.bands, cid.mcc, cid.mnc,
                cid.nci, cid.operatorNames.alphaLong, cid.operatorNames.alphaShort,
                primitiveArrayToArrayList(cid.additionalPlmns));
    }

    /**
     * Convert a SignalStrength defined in radio/1.4, 1.6/types.hal to SignalStrength
     * @param ss SignalStrength defined in radio/1.4, 1.6/types.hal
     * @return The converted SignalStrength
     */
    public static SignalStrength convertHalSignalStrength(Object ss) {
        if (ss == null) return null;
        if (ss instanceof android.hardware.radio.V1_4.SignalStrength) {
            android.hardware.radio.V1_4.SignalStrength signalStrength =
                    (android.hardware.radio.V1_4.SignalStrength) ss;
            return new SignalStrength(
                    convertHalCdmaSignalStrength(signalStrength.cdma, signalStrength.evdo),
                    convertHalGsmSignalStrength(signalStrength.gsm),
                    convertHalWcdmaSignalStrength(signalStrength.wcdma),
                    convertHalTdscdmaSignalStrength(signalStrength.tdscdma),
                    convertHalLteSignalStrength(signalStrength.lte),
                    convertHalNrSignalStrength(signalStrength.nr));
        } else if (ss instanceof android.hardware.radio.V1_6.SignalStrength) {
            android.hardware.radio.V1_6.SignalStrength signalStrength =
                    (android.hardware.radio.V1_6.SignalStrength) ss;
            return new SignalStrength(
                    convertHalCdmaSignalStrength(signalStrength.cdma, signalStrength.evdo),
                    convertHalGsmSignalStrength(signalStrength.gsm),
                    convertHalWcdmaSignalStrength(signalStrength.wcdma),
                    convertHalTdscdmaSignalStrength(signalStrength.tdscdma),
                    convertHalLteSignalStrength(signalStrength.lte),
                    convertHalNrSignalStrength(signalStrength.nr));
        }
        return null;
    }

    /**
     * Convert a SignalStrength defined in SignalStrength.aidl to SignalStrength
     * @param signalStrength SignalStrength defined in SignalStrength.aidl
     * @return The converted SignalStrength
     */
    public static SignalStrength convertHalSignalStrength(
            android.hardware.radio.network.SignalStrength signalStrength) {
        return new SignalStrength(
                convertHalCdmaSignalStrength(signalStrength.cdma, signalStrength.evdo),
                convertHalGsmSignalStrength(signalStrength.gsm),
                convertHalWcdmaSignalStrength(signalStrength.wcdma),
                convertHalTdscdmaSignalStrength(signalStrength.tdscdma),
                convertHalLteSignalStrength(signalStrength.lte),
                convertHalNrSignalStrength(signalStrength.nr));
    }

    /**
     * Convert a GsmSignalStrength defined in radio/1.0/types.hal to CellSignalStrengthGsm
     * @param ss GsmSignalStrength defined in radio/1.0/types.hal
     * @return The converted CellSignalStrengthGsm
     */
    public static CellSignalStrengthGsm convertHalGsmSignalStrength(
            android.hardware.radio.V1_0.GsmSignalStrength ss) {
        if (ss == null) return new CellSignalStrengthGsm();
        CellSignalStrengthGsm ret = new CellSignalStrengthGsm(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength), ss.bitErrorRate,
                ss.timingAdvance);
        if (ret.getRssi() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a GsmSignalStrength defined in GsmSignalStrength.aidl to CellSignalStrengthGsm
     * @param ss GsmSignalStrength defined in GsmSignalStrength.aidl
     * @return The converted CellSignalStrengthGsm
     */
    public static CellSignalStrengthGsm convertHalGsmSignalStrength(
            android.hardware.radio.network.GsmSignalStrength ss) {
        if (ss == null) return new CellSignalStrengthGsm();
        CellSignalStrengthGsm ret = new CellSignalStrengthGsm(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength), ss.bitErrorRate,
                ss.timingAdvance);
        if (ret.getRssi() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a CdmaSignalStrength and EvdoSignalStrength defined in radio/1.0/types.hal to
     * CellSignalStrengthCdma
     * @param cdma CdmaSignalStrength defined in radio/1.0/types.hal
     * @param evdo EvdoSignalStrength defined in radio/1.0/types.hal
     * @return The converted CellSignalStrengthCdma
     */
    public static CellSignalStrengthCdma convertHalCdmaSignalStrength(
            android.hardware.radio.V1_0.CdmaSignalStrength cdma,
            android.hardware.radio.V1_0.EvdoSignalStrength evdo) {
        if (cdma == null || evdo == null) return new CellSignalStrengthCdma();
        return new CellSignalStrengthCdma(-cdma.dbm, -cdma.ecio, -evdo.dbm, -evdo.ecio,
                evdo.signalNoiseRatio);
    }

    /**
     * Convert a CdmaSignalStrength and EvdoSignalStrength defined in radio/network to
     * CellSignalStrengthCdma
     * @param cdma CdmaSignalStrength defined in CdmaSignalStrength.aidl
     * @param evdo EvdoSignalStrength defined in EvdoSignalStrength.aidl
     * @return The converted CellSignalStrengthCdma
     */
    public static CellSignalStrengthCdma convertHalCdmaSignalStrength(
            android.hardware.radio.network.CdmaSignalStrength cdma,
            android.hardware.radio.network.EvdoSignalStrength evdo) {
        if (cdma == null || evdo == null) return new CellSignalStrengthCdma();
        return new CellSignalStrengthCdma(-cdma.dbm, -cdma.ecio, -evdo.dbm, -evdo.ecio,
                evdo.signalNoiseRatio);
    }

    /**
     * Convert a LteSignalStrength defined in radio/1.0, 1.6/types.hal to CellSignalStrengthLte
     * @param lte LteSignalStrength defined in radio/1.0, 1.6/types.hal
     * @return The converted CellSignalStrengthLte
     */
    public static CellSignalStrengthLte convertHalLteSignalStrength(Object lte) {
        if (lte == null) return null;
        if (lte instanceof android.hardware.radio.V1_0.LteSignalStrength) {
            android.hardware.radio.V1_0.LteSignalStrength ss =
                    (android.hardware.radio.V1_0.LteSignalStrength) lte;
            return new CellSignalStrengthLte(
                    CellSignalStrengthLte.convertRssiAsuToDBm(ss.signalStrength),
                    ss.rsrp != CellInfo.UNAVAILABLE ? -ss.rsrp : ss.rsrp,
                    ss.rsrq != CellInfo.UNAVAILABLE ? -ss.rsrq : ss.rsrq,
                    CellSignalStrengthLte.convertRssnrUnitFromTenDbToDB(ss.rssnr), ss.cqi,
                    ss.timingAdvance);
        } else if (lte instanceof android.hardware.radio.V1_6.LteSignalStrength) {
            android.hardware.radio.V1_6.LteSignalStrength ss =
                    (android.hardware.radio.V1_6.LteSignalStrength) lte;
            return new CellSignalStrengthLte(
                    CellSignalStrengthLte.convertRssiAsuToDBm(ss.base.signalStrength),
                    ss.base.rsrp != CellInfo.UNAVAILABLE ? -ss.base.rsrp : ss.base.rsrp,
                    ss.base.rsrq != CellInfo.UNAVAILABLE ? -ss.base.rsrq : ss.base.rsrq,
                    CellSignalStrengthLte.convertRssnrUnitFromTenDbToDB(ss.base.rssnr),
                    ss.cqiTableIndex, ss.base.cqi, ss.base.timingAdvance);
        } else {
            return null;
        }
    }

    /**
     * Convert a LteSignalStrength defined in LteSignalStrength.aidl to CellSignalStrengthLte
     * @param ss LteSignalStrength defined in LteSignalStrength.aidl
     * @return The converted CellSignalStrengthLte
     */
    public static CellSignalStrengthLte convertHalLteSignalStrength(
            android.hardware.radio.network.LteSignalStrength ss) {
        return new CellSignalStrengthLte(
                CellSignalStrengthLte.convertRssiAsuToDBm(ss.signalStrength),
                ss.rsrp != CellInfo.UNAVAILABLE ? -ss.rsrp : ss.rsrp,
                ss.rsrq != CellInfo.UNAVAILABLE ? -ss.rsrq : ss.rsrq,
                CellSignalStrengthLte.convertRssnrUnitFromTenDbToDB(ss.rssnr), ss.cqiTableIndex,
                ss.cqi, ss.timingAdvance);
    }

    /**
     * Convert a WcdmaSignalStrength defined in radio/1.2/types.hal to CellSignalStrengthWcdma
     * @param wcdma WcdmaSignalStrength defined in radio/1.2/types.hal
     * @return The converted CellSignalStrengthWcdma
     */
    public static CellSignalStrengthWcdma convertHalWcdmaSignalStrength(Object wcdma) {
        if (wcdma == null) return null;
        android.hardware.radio.V1_2.WcdmaSignalStrength ss =
                (android.hardware.radio.V1_2.WcdmaSignalStrength) wcdma;
        CellSignalStrengthWcdma ret = new CellSignalStrengthWcdma(
                CellSignalStrength.getRssiDbmFromAsu(ss.base.signalStrength),
                ss.base.bitErrorRate, CellSignalStrength.getRscpDbmFromAsu(ss.rscp),
                CellSignalStrength.getEcNoDbFromAsu(ss.ecno));
        if (ret.getRssi() == CellInfo.UNAVAILABLE && ret.getRscp() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a WcdmaSignalStrength defined in WcdmaSignalStrength.aidl to CellSignalStrengthWcdma
     * @param ss WcdmaSignalStrength defined in WcdmaSignalStrength.aidl
     * @return The converted CellSignalStrengthWcdma
     */
    public static CellSignalStrengthWcdma convertHalWcdmaSignalStrength(
            android.hardware.radio.network.WcdmaSignalStrength ss) {
        CellSignalStrengthWcdma ret = new CellSignalStrengthWcdma(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength),
                ss.bitErrorRate, CellSignalStrength.getRscpDbmFromAsu(ss.rscp),
                CellSignalStrength.getEcNoDbFromAsu(ss.ecno));
        if (ret.getRssi() == CellInfo.UNAVAILABLE && ret.getRscp() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a TdscdmaSignalStrength defined in radio/1.2/types.hal to CellSignalStrengthTdscdma
     * @param tdscdma TdscdmaSignalStrength defined in radio/1.2/types.hal
     * @return The converted CellSignalStrengthTdscdma
     */
    public static CellSignalStrengthTdscdma convertHalTdscdmaSignalStrength(Object tdscdma) {
        if (tdscdma == null) return null;
        android.hardware.radio.V1_2.TdscdmaSignalStrength ss =
                (android.hardware.radio.V1_2.TdscdmaSignalStrength) tdscdma;
        CellSignalStrengthTdscdma ret = new CellSignalStrengthTdscdma(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength), ss.bitErrorRate,
                CellSignalStrength.getRscpDbmFromAsu(ss.rscp));
        if (ret.getRssi() == CellInfo.UNAVAILABLE && ret.getRscp() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a TdscdmaSignalStrength defined in TdscdmaSignalStrength.aidl to
     * CellSignalStrengthTdscdma
     * @param ss TdscdmaSignalStrength defined in TdscdmaSignalStrength.aidl
     * @return The converted CellSignalStrengthTdscdma
     */
    public static CellSignalStrengthTdscdma convertHalTdscdmaSignalStrength(
            android.hardware.radio.network.TdscdmaSignalStrength ss) {
        CellSignalStrengthTdscdma ret = new CellSignalStrengthTdscdma(
                CellSignalStrength.getRssiDbmFromAsu(ss.signalStrength),
                ss.bitErrorRate, CellSignalStrength.getRscpDbmFromAsu(ss.rscp));
        if (ret.getRssi() == CellInfo.UNAVAILABLE && ret.getRscp() == CellInfo.UNAVAILABLE) {
            ret.setDefaultValues();
            ret.updateLevel(null, null);
        }
        return ret;
    }

    /**
     * Convert a NrSignalStrength defined in radio/1.4, 1.6/types.hal to CellSignalStrengthNr
     * @param nr NrSignalStrength defined in radio/1.4, 1.6/types.hal
     * @return The converted CellSignalStrengthNr
     */
    public static CellSignalStrengthNr convertHalNrSignalStrength(Object nr) {
        if (nr == null) return null;
        if (nr instanceof android.hardware.radio.V1_4.NrSignalStrength) {
            android.hardware.radio.V1_4.NrSignalStrength ss =
                    (android.hardware.radio.V1_4.NrSignalStrength) nr;
            return new CellSignalStrengthNr(CellSignalStrengthNr.flip(ss.csiRsrp),
                    CellSignalStrengthNr.flip(ss.csiRsrq), ss.csiSinr,
                    CellSignalStrengthNr.flip(ss.ssRsrp), CellSignalStrengthNr.flip(ss.ssRsrq),
                    ss.ssSinr);
        } else if (nr instanceof android.hardware.radio.V1_6.NrSignalStrength) {
            android.hardware.radio.V1_6.NrSignalStrength ss =
                    (android.hardware.radio.V1_6.NrSignalStrength) nr;
            return new CellSignalStrengthNr(CellSignalStrengthNr.flip(ss.base.csiRsrp),
                    CellSignalStrengthNr.flip(ss.base.csiRsrq), ss.base.csiSinr,
                    ss.csiCqiTableIndex, ss.csiCqiReport, CellSignalStrengthNr.flip(ss.base.ssRsrp),
                    CellSignalStrengthNr.flip(ss.base.ssRsrq), ss.base.ssSinr,
                    CellInfo.UNAVAILABLE);
        }
        return null;
    }

    /**
     * Convert a NrSignalStrength defined in NrSignalStrength.aidl to CellSignalStrengthNr
     * @param ss NrSignalStrength defined in NrSignalStrength.aidl
     * @return The converted CellSignalStrengthNr
     */
    public static CellSignalStrengthNr convertHalNrSignalStrength(
            android.hardware.radio.network.NrSignalStrength ss) {
        return new CellSignalStrengthNr(CellSignalStrengthNr.flip(ss.csiRsrp),
                CellSignalStrengthNr.flip(ss.csiRsrq), ss.csiSinr, ss.csiCqiTableIndex,
                primitiveArrayToArrayList(ss.csiCqiReport), CellSignalStrengthNr.flip(ss.ssRsrp),
                CellSignalStrengthNr.flip(ss.ssRsrq), ss.ssSinr, ss.timingAdvance);
    }

    private static ClosedSubscriberGroupInfo convertHalClosedSubscriberGroupInfo(
            android.hardware.radio.V1_5.OptionalCsgInfo optionalCsgInfo) {
        android.hardware.radio.V1_5.ClosedSubscriberGroupInfo csgInfo =
                optionalCsgInfo.getDiscriminator()
                        == android.hardware.radio.V1_5.OptionalCsgInfo.hidl_discriminator.csgInfo
                        ? optionalCsgInfo.csgInfo() : null;
        if (csgInfo == null) return null;
        return new ClosedSubscriberGroupInfo(csgInfo.csgIndication, csgInfo.homeNodebName,
                csgInfo.csgIdentity);
    }

    private static ClosedSubscriberGroupInfo convertHalClosedSubscriberGroupInfo(
            android.hardware.radio.network.ClosedSubscriberGroupInfo csgInfo) {
        if (csgInfo == null) return null;
        return new ClosedSubscriberGroupInfo(csgInfo.csgIndication, csgInfo.homeNodebName,
                csgInfo.csgIdentity);
    }

    /**
     * Convert a list of BarringInfo defined in radio/1.5/types.hal to a sparse array of
     * BarringServiceInfos
     * @param halBarringInfos List of BarringInfos defined in radio/1.5/types.hal
     * @return The converted sparse array of BarringServiceInfos
     */
    public static SparseArray<BarringInfo.BarringServiceInfo> convertHalBarringInfoList(
            List<android.hardware.radio.V1_5.BarringInfo> halBarringInfos) {
        SparseArray<BarringInfo.BarringServiceInfo> serviceInfos = new SparseArray<>();
        for (android.hardware.radio.V1_5.BarringInfo halBarringInfo : halBarringInfos) {
            if (halBarringInfo.barringType
                    == android.hardware.radio.V1_5.BarringInfo.BarringType.CONDITIONAL) {
                if (halBarringInfo.barringTypeSpecificInfo.getDiscriminator()
                        != android.hardware.radio.V1_5.BarringInfo.BarringTypeSpecificInfo
                        .hidl_discriminator.conditional) {
                    // this is an error case where the barring info is conditional but the
                    // conditional barring fields weren't included
                    continue;
                }
                android.hardware.radio.V1_5.BarringInfo.BarringTypeSpecificInfo
                        .Conditional conditionalInfo =
                        halBarringInfo.barringTypeSpecificInfo.conditional();
                serviceInfos.put(
                        halBarringInfo.serviceType, new BarringInfo.BarringServiceInfo(
                                halBarringInfo.barringType, // will always be CONDITIONAL here
                                conditionalInfo.isBarred,
                                conditionalInfo.factor,
                                conditionalInfo.timeSeconds));
            } else {
                // Barring type is either NONE or UNCONDITIONAL
                serviceInfos.put(
                        halBarringInfo.serviceType, new BarringInfo.BarringServiceInfo(
                                halBarringInfo.barringType, false, 0, 0));
            }
        }
        return serviceInfos;
    }

    /**
     * Convert a list of BarringInfo defined in BarringInfo.aidl to a sparse array of
     * BarringServiceInfos
     * @param halBarringInfos List of BarringInfos defined in BarringInfo.aidl
     * @return The converted sparse array of BarringServiceInfos
     */
    public static SparseArray<BarringInfo.BarringServiceInfo> convertHalBarringInfoList(
            android.hardware.radio.network.BarringInfo[] halBarringInfos) {
        SparseArray<BarringInfo.BarringServiceInfo> serviceInfos = new SparseArray<>();
        for (android.hardware.radio.network.BarringInfo halBarringInfo : halBarringInfos) {
            if (halBarringInfo.barringType
                    == android.hardware.radio.network.BarringInfo.BARRING_TYPE_CONDITIONAL) {
                if (halBarringInfo.barringTypeSpecificInfo == null) {
                    // this is an error case where the barring info is conditional but the
                    // conditional barring fields weren't included
                    continue;
                }
                serviceInfos.put(
                        halBarringInfo.serviceType, new BarringInfo.BarringServiceInfo(
                                halBarringInfo.barringType, // will always be CONDITIONAL here
                                halBarringInfo.barringTypeSpecificInfo.isBarred,
                                halBarringInfo.barringTypeSpecificInfo.factor,
                                halBarringInfo.barringTypeSpecificInfo.timeSeconds));
            } else {
                // Barring type is either NONE or UNCONDITIONAL
                serviceInfos.put(halBarringInfo.serviceType, new BarringInfo.BarringServiceInfo(
                        halBarringInfo.barringType, false, 0, 0));
            }
        }
        return serviceInfos;
    }

    private static LinkAddress convertToLinkAddress(String addressString) {
        return convertToLinkAddress(addressString, 0, LinkAddress.LIFETIME_UNKNOWN,
                LinkAddress.LIFETIME_UNKNOWN);
    }

    private static LinkAddress convertToLinkAddress(String addressString, int properties,
            long deprecationTime, long expirationTime) {
        addressString = addressString.trim();
        InetAddress address = null;
        int prefixLength = -1;
        try {
            String[] pieces = addressString.split("/", 2);
            address = InetAddresses.parseNumericAddress(pieces[0]);
            if (pieces.length == 1) {
                prefixLength = (address instanceof Inet4Address) ? 32 : 128;
            } else if (pieces.length == 2) {
                prefixLength = Integer.parseInt(pieces[1]);
            }
        } catch (NullPointerException e) {            // Null string.
        } catch (ArrayIndexOutOfBoundsException e) {  // No prefix length.
        } catch (NumberFormatException e) {           // Non-numeric prefix.
        } catch (IllegalArgumentException e) {        // Invalid IP address.
        }

        if (address == null || prefixLength == -1) {
            throw new IllegalArgumentException("Invalid link address " + addressString);
        }

        return new LinkAddress(address, prefixLength, properties, 0, deprecationTime,
                expirationTime);
    }

    /**
     * Convert SetupDataCallResult defined in radio/1.4, 1.5, 1.6/types.hal into
     * DataCallResponse
     * @param dcResult SetupDataCallResult defined in radio/1.4, 1.5, 1.6/types.hal
     * @return The converted DataCallResponse
     */
    @VisibleForTesting
    public static DataCallResponse convertHalDataCallResult(Object dcResult) {
        if (dcResult == null) return null;

        int cause, cid, active, mtu, mtuV4, mtuV6;
        long suggestedRetryTime;
        String ifname;
        int protocolType;
        String[] addresses;
        String[] dnses;
        String[] gateways;
        String[] pcscfs;
        Qos defaultQos = null;
        @DataCallResponse.HandoverFailureMode
        int handoverFailureMode = DataCallResponse.HANDOVER_FAILURE_MODE_LEGACY;
        int pduSessionId = DataCallResponse.PDU_SESSION_ID_NOT_SET;
        List<LinkAddress> laList = new ArrayList<>();
        List<QosBearerSession> qosSessions = new ArrayList<>();
        NetworkSliceInfo sliceInfo = null;
        List<TrafficDescriptor> trafficDescriptors = new ArrayList<>();

        if (dcResult instanceof android.hardware.radio.V1_4.SetupDataCallResult) {
            final android.hardware.radio.V1_4.SetupDataCallResult result =
                    (android.hardware.radio.V1_4.SetupDataCallResult) dcResult;
            cause = result.cause;
            suggestedRetryTime = result.suggestedRetryTime;
            cid = result.cid;
            active = result.active;
            protocolType = result.type;
            ifname = result.ifname;
            addresses = result.addresses.toArray(new String[0]);
            dnses = result.dnses.toArray(new String[0]);
            gateways = result.gateways.toArray(new String[0]);
            pcscfs = result.pcscf.toArray(new String[0]);
            mtu = mtuV4 = mtuV6 = result.mtu;
            if (addresses != null) {
                for (String address : addresses) {
                    laList.add(convertToLinkAddress(address));
                }
            }
        } else if (dcResult instanceof android.hardware.radio.V1_5.SetupDataCallResult) {
            final android.hardware.radio.V1_5.SetupDataCallResult result =
                    (android.hardware.radio.V1_5.SetupDataCallResult) dcResult;
            cause = result.cause;
            suggestedRetryTime = result.suggestedRetryTime;
            cid = result.cid;
            active = result.active;
            protocolType = result.type;
            ifname = result.ifname;
            laList = result.addresses.stream().map(la -> convertToLinkAddress(
                            la.address, la.properties, la.deprecationTime, la.expirationTime))
                    .collect(Collectors.toList());
            dnses = result.dnses.toArray(new String[0]);
            gateways = result.gateways.toArray(new String[0]);
            pcscfs = result.pcscf.toArray(new String[0]);
            mtu = Math.max(result.mtuV4, result.mtuV6);
            mtuV4 = result.mtuV4;
            mtuV6 = result.mtuV6;
        } else if (dcResult instanceof android.hardware.radio.V1_6.SetupDataCallResult) {
            final android.hardware.radio.V1_6.SetupDataCallResult result =
                    (android.hardware.radio.V1_6.SetupDataCallResult) dcResult;
            cause = result.cause;
            suggestedRetryTime = result.suggestedRetryTime;
            cid = result.cid;
            active = result.active;
            protocolType = result.type;
            ifname = result.ifname;
            laList = result.addresses.stream().map(la -> convertToLinkAddress(
                            la.address, la.properties, la.deprecationTime, la.expirationTime))
                    .collect(Collectors.toList());
            dnses = result.dnses.toArray(new String[0]);
            gateways = result.gateways.toArray(new String[0]);
            pcscfs = result.pcscf.toArray(new String[0]);
            mtu = Math.max(result.mtuV4, result.mtuV6);
            mtuV4 = result.mtuV4;
            mtuV6 = result.mtuV6;
            handoverFailureMode = result.handoverFailureMode;
            pduSessionId = result.pduSessionId;
            defaultQos = convertHalQos(result.defaultQos);
            qosSessions = result.qosSessions.stream().map(RILUtils::convertHalQosBearerSession)
                    .collect(Collectors.toList());
            sliceInfo = result.sliceInfo.getDiscriminator()
                    == android.hardware.radio.V1_6.OptionalSliceInfo.hidl_discriminator.noinit
                    ? null : convertHalSliceInfo(result.sliceInfo.value());
            for (android.hardware.radio.V1_6.TrafficDescriptor td : result.trafficDescriptors) {
                try {
                    trafficDescriptors.add(RILUtils.convertHalTrafficDescriptor(td));
                } catch (IllegalArgumentException e) {
                    loge("convertHalDataCallResult: Failed to convert traffic descriptor. e=" + e);
                }
            }
        } else {
            loge("Unsupported SetupDataCallResult " + dcResult);
            return null;
        }

        // Process dns
        List<InetAddress> dnsList = new ArrayList<>();
        if (dnses != null) {
            for (String dns : dnses) {
                dns = dns.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(dns);
                    dnsList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(TAG, "Unknown dns: " + dns, e);
                }
            }
        }

        // Process gateway
        List<InetAddress> gatewayList = new ArrayList<>();
        if (gateways != null) {
            for (String gateway : gateways) {
                gateway = gateway.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(gateway);
                    gatewayList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(TAG, "Unknown gateway: " + gateway, e);
                }
            }
        }

        // Process gateway
        List<InetAddress> pcscfList = new ArrayList<>();
        if (pcscfs != null) {
            for (String pcscf : pcscfs) {
                pcscf = pcscf.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(pcscf);
                    pcscfList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(TAG, "Unknown pcscf: " + pcscf, e);
                }
            }
        }

        return new DataCallResponse.Builder()
                .setCause(cause)
                .setRetryDurationMillis(suggestedRetryTime)
                .setId(cid)
                .setLinkStatus(active)
                .setProtocolType(protocolType)
                .setInterfaceName(ifname)
                .setAddresses(laList)
                .setDnsAddresses(dnsList)
                .setGatewayAddresses(gatewayList)
                .setPcscfAddresses(pcscfList)
                .setMtu(mtu)
                .setMtuV4(mtuV4)
                .setMtuV6(mtuV6)
                .setHandoverFailureMode(handoverFailureMode)
                .setPduSessionId(pduSessionId)
                .setDefaultQos(defaultQos)
                .setQosBearerSessions(qosSessions)
                .setSliceInfo(sliceInfo)
                .setTrafficDescriptors(trafficDescriptors)
                .build();
    }

    /**
     * Convert SetupDataCallResult defined in SetupDataCallResult.aidl into DataCallResponse
     * @param result SetupDataCallResult defined in SetupDataCallResult.aidl
     * @return The converted DataCallResponse
     */
    @VisibleForTesting
    public static DataCallResponse convertHalDataCallResult(
            android.hardware.radio.data.SetupDataCallResult result) {
        if (result == null) return null;
        List<LinkAddress> laList = new ArrayList<>();
        if (result.addresses != null) {
            for (android.hardware.radio.data.LinkAddress la : result.addresses) {
                laList.add(convertToLinkAddress(la.address, la.addressProperties,
                        la.deprecationTime, la.expirationTime));
            }
        }
        List<InetAddress> dnsList = new ArrayList<>();
        if (result.dnses != null) {
            for (String dns : result.dnses) {
                dns = dns.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(dns);
                    dnsList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(TAG, "Unknown dns: " + dns, e);
                }
            }
        }
        List<InetAddress> gatewayList = new ArrayList<>();
        if (result.gateways != null) {
            for (String gateway : result.gateways) {
                gateway = gateway.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(gateway);
                    gatewayList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(TAG, "Unknown gateway: " + gateway, e);
                }
            }
        }
        List<InetAddress> pcscfList = new ArrayList<>();
        if (result.pcscf != null) {
            for (String pcscf : result.pcscf) {
                pcscf = pcscf.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(pcscf);
                    pcscfList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(TAG, "Unknown pcscf: " + pcscf, e);
                }
            }
        }
        List<QosBearerSession> qosSessions = new ArrayList<>();
        if (result.qosSessions != null) {
            for (android.hardware.radio.data.QosSession session : result.qosSessions) {
                qosSessions.add(convertHalQosBearerSession(session));
            }
        }
        List<TrafficDescriptor> trafficDescriptors = new ArrayList<>();
        if (result.trafficDescriptors != null) {
            for (android.hardware.radio.data.TrafficDescriptor td : result.trafficDescriptors) {
                try {
                    trafficDescriptors.add(convertHalTrafficDescriptor(td));
                } catch (IllegalArgumentException e) {
                    loge("convertHalDataCallResult: Failed to convert traffic descriptor. e=" + e);
                }
            }
        }

        return new DataCallResponse.Builder()
                .setCause(result.cause)
                .setRetryDurationMillis(result.suggestedRetryTime)
                .setId(result.cid)
                .setLinkStatus(result.active)
                .setProtocolType(result.type)
                .setInterfaceName(result.ifname)
                .setAddresses(laList)
                .setDnsAddresses(dnsList)
                .setGatewayAddresses(gatewayList)
                .setPcscfAddresses(pcscfList)
                .setMtu(Math.max(result.mtuV4, result.mtuV6))
                .setMtuV4(result.mtuV4)
                .setMtuV6(result.mtuV6)
                .setHandoverFailureMode(result.handoverFailureMode)
                .setPduSessionId(result.pduSessionId)
                .setDefaultQos(convertHalQos(result.defaultQos))
                .setQosBearerSessions(qosSessions)
                .setSliceInfo(result.sliceInfo == null ? null
                        : convertHalSliceInfo(result.sliceInfo))
                .setTrafficDescriptors(trafficDescriptors)
                .build();
    }

    private static NetworkSliceInfo convertHalSliceInfo(android.hardware.radio.V1_6.SliceInfo si) {
        NetworkSliceInfo.Builder builder = new NetworkSliceInfo.Builder()
                .setSliceServiceType(si.sst)
                .setMappedHplmnSliceServiceType(si.mappedHplmnSst)
                .setStatus(convertHalSliceStatus(si.status));
        if (si.sliceDifferentiator != NetworkSliceInfo.SLICE_DIFFERENTIATOR_NO_SLICE) {
            builder.setSliceDifferentiator(si.sliceDifferentiator)
                    .setMappedHplmnSliceDifferentiator(si.mappedHplmnSD);
        }
        return builder.build();
    }

    private static NetworkSliceInfo convertHalSliceInfo(android.hardware.radio.data.SliceInfo si) {
        NetworkSliceInfo.Builder builder = new NetworkSliceInfo.Builder()
                .setSliceServiceType(si.sliceServiceType)
                .setMappedHplmnSliceServiceType(si.mappedHplmnSst)
                .setStatus(convertHalSliceStatus(si.status));
        if (si.sliceDifferentiator != NetworkSliceInfo.SLICE_DIFFERENTIATOR_NO_SLICE) {
            builder.setSliceDifferentiator(si.sliceDifferentiator)
                    .setMappedHplmnSliceDifferentiator(si.mappedHplmnSd);
        }
        return builder.build();
    }

    @NetworkSliceInfo.SliceStatus private static int convertHalSliceStatus(byte status) {
        switch (status) {
            case SliceInfo.STATUS_CONFIGURED:
                return NetworkSliceInfo.SLICE_STATUS_CONFIGURED;
            case SliceInfo.STATUS_ALLOWED:
                return NetworkSliceInfo.SLICE_STATUS_ALLOWED;
            case SliceInfo.STATUS_REJECTED_NOT_AVAILABLE_IN_PLMN:
                return NetworkSliceInfo.SLICE_STATUS_REJECTED_NOT_AVAILABLE_IN_PLMN;
            case SliceInfo.STATUS_REJECTED_NOT_AVAILABLE_IN_REG_AREA:
                return NetworkSliceInfo.SLICE_STATUS_REJECTED_NOT_AVAILABLE_IN_REGISTERED_AREA;
            case SliceInfo.STATUS_DEFAULT_CONFIGURED:
                return NetworkSliceInfo.SLICE_STATUS_DEFAULT_CONFIGURED;
            default:
                return NetworkSliceInfo.SLICE_STATUS_UNKNOWN;
        }
    }

    private static TrafficDescriptor convertHalTrafficDescriptor(
            android.hardware.radio.V1_6.TrafficDescriptor td) throws IllegalArgumentException {
        String dnn = td.dnn.getDiscriminator()
                == android.hardware.radio.V1_6.OptionalDnn.hidl_discriminator.noinit
                ? null : td.dnn.value();
        byte[] osAppId = td.osAppId.getDiscriminator()
                == android.hardware.radio.V1_6.OptionalOsAppId.hidl_discriminator.noinit
                ? null : arrayListToPrimitiveArray(td.osAppId.value().osAppId);

        TrafficDescriptor.Builder builder = new TrafficDescriptor.Builder();
        if (dnn != null) {
            builder.setDataNetworkName(dnn);
        }
        if (osAppId != null) {
            builder.setOsAppId(osAppId);
        }
        return builder.build();
    }

    private static TrafficDescriptor convertHalTrafficDescriptor(
            android.hardware.radio.data.TrafficDescriptor td) throws IllegalArgumentException {
        String dnn = td.dnn;
        byte[] osAppId = td.osAppId == null ? null : td.osAppId.osAppId;
        TrafficDescriptor.Builder builder = new TrafficDescriptor.Builder();
        if (dnn != null) {
            builder.setDataNetworkName(dnn);
        }
        if (osAppId != null) {
            builder.setOsAppId(osAppId);
        }
        return builder.build();
    }

    /**
     * Convert SlicingConfig defined in radio/1.6/types.hal to NetworkSlicingConfig
     * @param sc SlicingConfig defined in radio/1.6/types.hal
     * @return The converted NetworkSlicingConfig
     */
    public static NetworkSlicingConfig convertHalSlicingConfig(
            android.hardware.radio.V1_6.SlicingConfig sc) {
        List<UrspRule> urspRules = sc.urspRules.stream().map(ur -> new UrspRule(ur.precedence,
                        ur.trafficDescriptors.stream()
                                .map(td -> {
                                    try {
                                        return convertHalTrafficDescriptor(td);
                                    } catch (IllegalArgumentException e) {
                                        loge("convertHalSlicingConfig: Failed to convert traffic "
                                                + "descriptor. e=" + e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()),
                        ur.routeSelectionDescriptor.stream().map(
                                rsd -> new RouteSelectionDescriptor(rsd.precedence,
                                        rsd.sessionType.value(), rsd.sscMode.value(),
                                        rsd.sliceInfo.stream().map(RILUtils::convertHalSliceInfo)
                                                .collect(Collectors.toList()),
                                        rsd.dnn)).collect(Collectors.toList())))
                .collect(Collectors.toList());
        return new NetworkSlicingConfig(urspRules, sc.sliceInfo.stream()
                .map(RILUtils::convertHalSliceInfo).collect(Collectors.toList()));
    }

    /**
     * Convert SlicingConfig defined in SlicingConfig.aidl to NetworkSlicingConfig
     * @param sc SlicingConfig defined in SlicingConfig.aidl
     * @return The converted NetworkSlicingConfig
     */
    public static NetworkSlicingConfig convertHalSlicingConfig(
            android.hardware.radio.data.SlicingConfig sc) {
        List<UrspRule> urspRules = new ArrayList<>();
        for (android.hardware.radio.data.UrspRule ur : sc.urspRules) {
            List<TrafficDescriptor> tds = new ArrayList<>();
            for (android.hardware.radio.data.TrafficDescriptor td : ur.trafficDescriptors) {
                try {
                    tds.add(convertHalTrafficDescriptor(td));
                } catch (IllegalArgumentException e) {
                    loge("convertHalTrafficDescriptor: " + e);
                }
            }
            List<RouteSelectionDescriptor> rsds = new ArrayList<>();
            for (android.hardware.radio.data.RouteSelectionDescriptor rsd
                    : ur.routeSelectionDescriptor) {
                List<NetworkSliceInfo> sliceInfo = new ArrayList<>();
                for (android.hardware.radio.data.SliceInfo si : rsd.sliceInfo) {
                    sliceInfo.add(convertHalSliceInfo(si));
                }
                rsds.add(new RouteSelectionDescriptor(rsd.precedence, rsd.sessionType, rsd.sscMode,
                        sliceInfo, primitiveArrayToArrayList(rsd.dnn)));
            }
            urspRules.add(new UrspRule(ur.precedence, tds, rsds));
        }
        List<NetworkSliceInfo> sliceInfo = new ArrayList<>();
        for (android.hardware.radio.data.SliceInfo si : sc.sliceInfo) {
            sliceInfo.add(convertHalSliceInfo(si));
        }
        return new NetworkSlicingConfig(urspRules, sliceInfo);
    }

    private static Qos.QosBandwidth convertHalQosBandwidth(
            android.hardware.radio.V1_6.QosBandwidth bandwidth) {
        return new Qos.QosBandwidth(bandwidth.maxBitrateKbps, bandwidth.guaranteedBitrateKbps);
    }

    private static Qos.QosBandwidth convertHalQosBandwidth(
            android.hardware.radio.data.QosBandwidth bandwidth) {
        return new Qos.QosBandwidth(bandwidth.maxBitrateKbps, bandwidth.guaranteedBitrateKbps);
    }

    private static Qos convertHalQos(android.hardware.radio.V1_6.Qos qos) {
        if (qos == null) return null;
        switch (qos.getDiscriminator()) {
            case android.hardware.radio.V1_6.Qos.hidl_discriminator.eps:
                android.hardware.radio.V1_6.EpsQos eps = qos.eps();
                return new EpsQos(convertHalQosBandwidth(eps.downlink),
                        convertHalQosBandwidth(eps.uplink), eps.qci);
            case android.hardware.radio.V1_6.Qos.hidl_discriminator.nr:
                android.hardware.radio.V1_6.NrQos nr = qos.nr();
                return new NrQos(convertHalQosBandwidth(nr.downlink),
                        convertHalQosBandwidth(nr.uplink), nr.qfi, nr.fiveQi, nr.averagingWindowMs);
            default:
                return null;
        }
    }

    private static Qos convertHalQos(android.hardware.radio.data.Qos qos) {
        if (qos == null) return null;
        switch (qos.getTag()) {
            case android.hardware.radio.data.Qos.eps:
                android.hardware.radio.data.EpsQos eps = qos.getEps();
                return new EpsQos(convertHalQosBandwidth(eps.downlink),
                        convertHalQosBandwidth(eps.uplink), eps.qci);
            case android.hardware.radio.data.Qos.nr:
                android.hardware.radio.data.NrQos nr = qos.getNr();
                int averagingWindowMs = nr.averagingWindowMillis;
                if (averagingWindowMs
                        == android.hardware.radio.data.NrQos.AVERAGING_WINDOW_UNKNOWN) {
                    averagingWindowMs = nr.averagingWindowMs;
                }
                return new NrQos(convertHalQosBandwidth(nr.downlink),
                        convertHalQosBandwidth(nr.uplink), nr.qfi, nr.fiveQi, averagingWindowMs);
            default:
                return null;
        }
    }

    private static QosBearerFilter convertHalQosBearerFilter(
            android.hardware.radio.V1_6.QosFilter qosFilter) {
        List<LinkAddress> localAddressList = new ArrayList<>();
        String[] localAddresses = qosFilter.localAddresses.toArray(new String[0]);
        if (localAddresses != null) {
            for (String address : localAddresses) {
                localAddressList.add(convertToLinkAddress(address));
            }
        }
        List<LinkAddress> remoteAddressList = new ArrayList<>();
        String[] remoteAddresses = qosFilter.remoteAddresses.toArray(new String[0]);
        if (remoteAddresses != null) {
            for (String address : remoteAddresses) {
                remoteAddressList.add(convertToLinkAddress(address));
            }
        }
        QosBearerFilter.PortRange localPort = null;
        if (qosFilter.localPort != null) {
            if (qosFilter.localPort.getDiscriminator()
                    == android.hardware.radio.V1_6.MaybePort.hidl_discriminator.range) {
                final android.hardware.radio.V1_6.PortRange portRange = qosFilter.localPort.range();
                localPort = new QosBearerFilter.PortRange(portRange.start, portRange.end);
            }
        }
        QosBearerFilter.PortRange remotePort = null;
        if (qosFilter.remotePort != null) {
            if (qosFilter.remotePort.getDiscriminator()
                    == android.hardware.radio.V1_6.MaybePort.hidl_discriminator.range) {
                final android.hardware.radio.V1_6.PortRange portRange =
                        qosFilter.remotePort.range();
                remotePort = new QosBearerFilter.PortRange(portRange.start, portRange.end);
            }
        }
        int tos = -1;
        if (qosFilter.tos != null) {
            if (qosFilter.tos.getDiscriminator() == android.hardware.radio.V1_6.QosFilter
                    .TypeOfService.hidl_discriminator.value) {
                tos = qosFilter.tos.value();
            }
        }
        long flowLabel = -1;
        if (qosFilter.flowLabel != null) {
            if (qosFilter.flowLabel.getDiscriminator() == android.hardware.radio.V1_6.QosFilter
                    .Ipv6FlowLabel.hidl_discriminator.value) {
                flowLabel = qosFilter.flowLabel.value();
            }
        }
        long spi = -1;
        if (qosFilter.spi != null) {
            if (qosFilter.spi.getDiscriminator()
                    == android.hardware.radio.V1_6.QosFilter.IpsecSpi.hidl_discriminator.value) {
                spi = qosFilter.spi.value();
            }
        }
        return new QosBearerFilter(localAddressList, remoteAddressList, localPort, remotePort,
                qosFilter.protocol, tos, flowLabel, spi, qosFilter.direction, qosFilter.precedence);
    }

    private static QosBearerFilter convertHalQosBearerFilter(
            android.hardware.radio.data.QosFilter qosFilter) {
        List<LinkAddress> localAddressList = new ArrayList<>();
        String[] localAddresses = qosFilter.localAddresses;
        if (localAddresses != null) {
            for (String address : localAddresses) {
                localAddressList.add(convertToLinkAddress(address));
            }
        }
        List<LinkAddress> remoteAddressList = new ArrayList<>();
        String[] remoteAddresses = qosFilter.remoteAddresses;
        if (remoteAddresses != null) {
            for (String address : remoteAddresses) {
                remoteAddressList.add(convertToLinkAddress(address));
            }
        }
        QosBearerFilter.PortRange localPort = null;
        if (qosFilter.localPort != null) {
            localPort = new QosBearerFilter.PortRange(
                    qosFilter.localPort.start, qosFilter.localPort.end);
        }
        QosBearerFilter.PortRange remotePort = null;
        if (qosFilter.remotePort != null) {
            remotePort = new QosBearerFilter.PortRange(
                    qosFilter.remotePort.start, qosFilter.remotePort.end);
        }
        int tos = -1;
        if (qosFilter.tos != null) {
            if (qosFilter.tos.getTag()
                    == android.hardware.radio.data.QosFilterTypeOfService.value) {
                tos = qosFilter.tos.value;
            }
        }
        long flowLabel = -1;
        if (qosFilter.flowLabel != null) {
            if (qosFilter.flowLabel.getTag()
                    == android.hardware.radio.data.QosFilterIpv6FlowLabel.value) {
                flowLabel = qosFilter.flowLabel.value;
            }
        }
        long spi = -1;
        if (qosFilter.spi != null) {
            if (qosFilter.spi.getTag()
                    == android.hardware.radio.data.QosFilterIpsecSpi.value) {
                spi = qosFilter.spi.value;
            }
        }
        return new QosBearerFilter(localAddressList, remoteAddressList, localPort, remotePort,
                qosFilter.protocol, tos, flowLabel, spi, qosFilter.direction, qosFilter.precedence);
    }

    private static QosBearerSession convertHalQosBearerSession(
            android.hardware.radio.V1_6.QosSession qosSession) {
        List<QosBearerFilter> qosBearerFilters = new ArrayList<>();
        if (qosSession.qosFilters != null) {
            for (android.hardware.radio.V1_6.QosFilter filter : qosSession.qosFilters) {
                qosBearerFilters.add(convertHalQosBearerFilter(filter));
            }
        }
        return new QosBearerSession(qosSession.qosSessionId, convertHalQos(qosSession.qos),
                qosBearerFilters);
    }

    private static QosBearerSession convertHalQosBearerSession(
            android.hardware.radio.data.QosSession qosSession) {
        List<QosBearerFilter> qosBearerFilters = new ArrayList<>();
        if (qosSession.qosFilters != null) {
            for (android.hardware.radio.data.QosFilter filter : qosSession.qosFilters) {
                qosBearerFilters.add(convertHalQosBearerFilter(filter));
            }
        }
        return new QosBearerSession(qosSession.qosSessionId, convertHalQos(qosSession.qos),
                qosBearerFilters);
    }

    /**
     * Convert a list of SetupDataCallResult defined in radio/1.4, 1.5, 1.6/types.hal into
     * a list of DataCallResponse
     * @param dataCallResultList List of SetupDataCallResult defined in
     *        radio/1.4, 1.5, 1.6/types.hal
     * @return The converted list of DataCallResponses
     */
    @VisibleForTesting
    public static ArrayList<DataCallResponse> convertHalDataCallResultList(
            List<? extends Object> dataCallResultList) {
        ArrayList<DataCallResponse> response = new ArrayList<>(dataCallResultList.size());

        for (Object obj : dataCallResultList) {
            response.add(convertHalDataCallResult(obj));
        }
        return response;
    }

    /**
     * Convert a list of SetupDataCallResult defined in SetupDataCallResult.aidl into a list of
     * DataCallResponse
     * @param dataCallResultList Array of SetupDataCallResult defined in SetupDataCallResult.aidl
     * @return The converted list of DataCallResponses
     */
    @VisibleForTesting
    public static ArrayList<DataCallResponse> convertHalDataCallResultList(
            android.hardware.radio.data.SetupDataCallResult[] dataCallResultList) {
        ArrayList<DataCallResponse> response = new ArrayList<>(dataCallResultList.length);

        for (android.hardware.radio.data.SetupDataCallResult result : dataCallResultList) {
            response.add(convertHalDataCallResult(result));
        }
        return response;
    }

    /**
     * Convert KeepaliveStatusCode defined in radio/1.1/types.hal and KeepaliveStatus.aidl
     * to KeepaliveStatus
     * @param halCode KeepaliveStatus code defined in radio/1.1/types.hal or KeepaliveStatus.aidl
     * @return The converted KeepaliveStatus
     */
    public static @KeepaliveStatusCode int convertHalKeepaliveStatusCode(int halCode) {
        switch (halCode) {
            case android.hardware.radio.V1_1.KeepaliveStatusCode.ACTIVE:
                return KeepaliveStatus.STATUS_ACTIVE;
            case android.hardware.radio.V1_1.KeepaliveStatusCode.INACTIVE:
                return KeepaliveStatus.STATUS_INACTIVE;
            case android.hardware.radio.V1_1.KeepaliveStatusCode.PENDING:
                return KeepaliveStatus.STATUS_PENDING;
            default:
                return -1;
        }
    }

    /**
     * Convert RadioState defined in radio/1.0/types.hal and RadioState.aidl to RadioPowerState
     * @param stateInt Radio state defined in radio/1.0/types.hal or RadioState.aidl
     * @return The converted {@link Annotation.RadioPowerState RadioPowerState}
     */
    public static @Annotation.RadioPowerState int convertHalRadioState(int stateInt) {
        int state;
        switch(stateInt) {
            case android.hardware.radio.V1_0.RadioState.OFF:
                state = TelephonyManager.RADIO_POWER_OFF;
                break;
            case android.hardware.radio.V1_0.RadioState.UNAVAILABLE:
                state = TelephonyManager.RADIO_POWER_UNAVAILABLE;
                break;
            case android.hardware.radio.V1_0.RadioState.ON:
                state = TelephonyManager.RADIO_POWER_ON;
                break;
            default:
                throw new RuntimeException("Unrecognized RadioState: " + stateInt);
        }
        return state;
    }

    /**
     * Convert CellConnectionStatus defined in radio/1.2/types.hal to ConnectionStatus
     * @param status Cell connection status defined in radio/1.2/types.hal
     * @return The converted ConnectionStatus
     */
    public static int convertHalCellConnectionStatus(int status) {
        switch (status) {
            case android.hardware.radio.V1_2.CellConnectionStatus.PRIMARY_SERVING:
                return PhysicalChannelConfig.CONNECTION_PRIMARY_SERVING;
            case android.hardware.radio.V1_2.CellConnectionStatus.SECONDARY_SERVING:
                return PhysicalChannelConfig.CONNECTION_SECONDARY_SERVING;
            default:
                return PhysicalChannelConfig.CONNECTION_UNKNOWN;
        }
    }

    /**
     * Convert Call defined in radio/1.2, 1.6/types.hal to DriverCall
     * @param halCall Call defined in radio/1.2, 1.6/types.hal
     * @return The converted DriverCall
     */
    public static DriverCall convertToDriverCall(Object halCall) {
        DriverCall dc = new DriverCall();
        final android.hardware.radio.V1_6.Call call16;
        final android.hardware.radio.V1_2.Call call12;
        final android.hardware.radio.V1_0.Call call10;
        if (halCall instanceof android.hardware.radio.V1_6.Call) {
            call16 = (android.hardware.radio.V1_6.Call) halCall;
            call12 = call16.base;
            call10 = call12.base;
        } else if (halCall instanceof android.hardware.radio.V1_2.Call) {
            call16 = null;
            call12 = (android.hardware.radio.V1_2.Call) halCall;
            call10 = call12.base;
        } else {
            call16 = null;
            call12 = null;
            call10 = null;
        }
        if (call10 != null) {
            dc.state = DriverCall.stateFromCLCC(call10.state);
            dc.index = call10.index;
            dc.TOA = call10.toa;
            dc.isMpty = call10.isMpty;
            dc.isMT = call10.isMT;
            dc.als = call10.als;
            dc.isVoice = call10.isVoice;
            dc.isVoicePrivacy = call10.isVoicePrivacy;
            dc.number = call10.number;
            dc.numberPresentation = DriverCall.presentationFromCLIP(call10.numberPresentation);
            dc.name = call10.name;
            dc.namePresentation = DriverCall.presentationFromCLIP(call10.namePresentation);
            if (call10.uusInfo.size() == 1) {
                dc.uusInfo = new UUSInfo();
                dc.uusInfo.setType(call10.uusInfo.get(0).uusType);
                dc.uusInfo.setDcs(call10.uusInfo.get(0).uusDcs);
                if (!TextUtils.isEmpty(call10.uusInfo.get(0).uusData)) {
                    byte[] userData = call10.uusInfo.get(0).uusData.getBytes();
                    dc.uusInfo.setUserData(userData);
                }
            }
            // Make sure there's a leading + on addresses with a TOA of 145
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
        }
        if (call12 != null) {
            dc.audioQuality = call12.audioQuality;
        }
        if (call16 != null) {
            dc.forwardedNumber = call16.forwardedNumber;
        }
        return dc;
    }

    /**
     * Convert Call defined in Call.aidl to DriverCall
     * @param halCall Call defined in Call.aidl
     * @return The converted DriverCall
     */
    public static DriverCall convertToDriverCall(android.hardware.radio.voice.Call halCall) {
        DriverCall dc = new DriverCall();
        dc.state = DriverCall.stateFromCLCC(halCall.state);
        dc.index = halCall.index;
        dc.TOA = halCall.toa;
        dc.isMpty = halCall.isMpty;
        dc.isMT = halCall.isMT;
        dc.als = halCall.als;
        dc.isVoice = halCall.isVoice;
        dc.isVoicePrivacy = halCall.isVoicePrivacy;
        dc.number = halCall.number;
        dc.numberPresentation = DriverCall.presentationFromCLIP(halCall.numberPresentation);
        dc.name = halCall.name;
        dc.namePresentation = DriverCall.presentationFromCLIP(halCall.namePresentation);
        if (halCall.uusInfo.length == 1) {
            dc.uusInfo = new UUSInfo();
            dc.uusInfo.setType(halCall.uusInfo[0].uusType);
            dc.uusInfo.setDcs(halCall.uusInfo[0].uusDcs);
            if (!TextUtils.isEmpty(halCall.uusInfo[0].uusData)) {
                dc.uusInfo.setUserData(halCall.uusInfo[0].uusData.getBytes());
            }
        }
        // Make sure there's a leading + on addresses with a TOA of 145
        dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
        dc.audioQuality = halCall.audioQuality;
        dc.forwardedNumber = halCall.forwardedNumber;
        return dc;
    }

    /**
     * Convert OperatorStatus defined in radio/1.0/types.hal to OperatorInfo.State
     * @param status Operator status defined in radio/1.0/types.hal
     * @return The converted OperatorStatus as a String
     */
    public static String convertHalOperatorStatus(int status) {
        if (status == android.hardware.radio.V1_0.OperatorStatus.UNKNOWN) {
            return "unknown";
        } else if (status == android.hardware.radio.V1_0.OperatorStatus.AVAILABLE) {
            return "available";
        } else if (status == android.hardware.radio.V1_0.OperatorStatus.CURRENT) {
            return "current";
        } else if (status == android.hardware.radio.V1_0.OperatorStatus.FORBIDDEN) {
            return "forbidden";
        } else {
            return "";
        }
    }

    /**
     * Convert a list of Carriers defined in radio/1.0/types.hal to a list of CarrierIdentifiers
     * @param carrierList List of Carriers defined in radio/1.0/types.hal
     * @return The converted list of CarrierIdentifiers
     */
    public static List<CarrierIdentifier> convertHalCarrierList(
            List<android.hardware.radio.V1_0.Carrier> carrierList) {
        List<CarrierIdentifier> ret = new ArrayList<>();
        for (int i = 0; i < carrierList.size(); i++) {
            String mcc = carrierList.get(i).mcc;
            String mnc = carrierList.get(i).mnc;
            String spn = null, imsi = null, gid1 = null, gid2 = null;
            int matchType = carrierList.get(i).matchType;
            String matchData = carrierList.get(i).matchData;
            if (matchType == CarrierIdentifier.MatchType.SPN) {
                spn = matchData;
            } else if (matchType == CarrierIdentifier.MatchType.IMSI_PREFIX) {
                imsi = matchData;
            } else if (matchType == CarrierIdentifier.MatchType.GID1) {
                gid1 = matchData;
            } else if (matchType == CarrierIdentifier.MatchType.GID2) {
                gid2 = matchData;
            }
            ret.add(new CarrierIdentifier(mcc, mnc, spn, imsi, gid1, gid2));
        }
        return ret;
    }

    /**
     * Convert a list of Carriers defined in radio/1.0/types.hal to a list of CarrierIdentifiers
     * @param carrierList List of Carriers defined in radio/1.0/types.hal
     * @return The converted list of CarrierIdentifiers
     */
    public static List<CarrierIdentifier> convertHalCarrierList(
            android.hardware.radio.sim.Carrier[] carrierList) {
        List<CarrierIdentifier> ret = new ArrayList<>();
        if (carrierList == null) {
            return ret;
        }
        for (int i = 0; i < carrierList.length; i++) {
            String mcc = carrierList[i].mcc;
            String mnc = carrierList[i].mnc;
            String spn = null, imsi = null, gid1 = null, gid2 = null;
            int matchType = carrierList[i].matchType;
            String matchData = carrierList[i].matchData;
            if (matchType == CarrierIdentifier.MatchType.SPN) {
                spn = matchData;
            } else if (matchType == CarrierIdentifier.MatchType.IMSI_PREFIX) {
                imsi = matchData;
            } else if (matchType == CarrierIdentifier.MatchType.GID1) {
                gid1 = matchData;
            } else if (matchType == CarrierIdentifier.MatchType.GID2) {
                gid2 = matchData;
            }
            ret.add(new CarrierIdentifier(mcc, mnc, spn, imsi, gid1, gid2));
        }
        return ret;
    }

    /**
     * Convert an array of CarrierInfo defined in
     * radio/aidl/android/hardware/radio/sim/CarrierInfo.aidl to a list of CarrierInfo
     * defined in android/service/carrier/CarrierInfo.java
     *
     * @param carrierInfos array of CarrierInfo defined in
     *                     radio/aidl/android/hardware/radio/sim/CarrierInfo.aidl
     * @return The converted list of CarrierInfo
     */
    public static List<CarrierInfo> convertAidlCarrierInfoList(
            android.hardware.radio.sim.CarrierInfo[] carrierInfos) {
        List<CarrierInfo> carrierInfoList = new ArrayList<>();
        if (carrierInfos == null) {
            loge("convertAidlCarrierInfoList received NULL carrierInfos");
            return carrierInfoList;
        }
        for (int index = 0; index < carrierInfos.length; index++) {
            String mcc = carrierInfos[index].mcc;
            String mnc = carrierInfos[index].mnc;
            String spn = carrierInfos[index].spn;
            String gid1 = carrierInfos[index].gid1;
            String gid2 = carrierInfos[index].gid2;
            String imsi = carrierInfos[index].imsiPrefix;
            String iccid = carrierInfos[index].iccid;
            String impi = carrierInfos[index].impi;
            List<android.hardware.radio.sim.Plmn> halEhplmn = carrierInfos[index].ehplmn;
            List<String> eHplmnList = new ArrayList<>();
            if (halEhplmn != null) {
                for (int plmnIndex = 0; plmnIndex < halEhplmn.size(); plmnIndex++) {
                    String ehplmnMcc = halEhplmn.get(plmnIndex).mcc;
                    String ehplmnMnc = halEhplmn.get(plmnIndex).mnc;
                    eHplmnList.add(ehplmnMcc + "," + ehplmnMnc);
                }
            } else {
                loge("convertAidlCarrierInfoList ehplmList is NULL");
            }
            CarrierInfo carrierInfo = new CarrierInfo(mcc, mnc, spn, gid1, gid2, imsi, iccid, impi,
                    eHplmnList);
            carrierInfoList.add(carrierInfo);
        }
        return carrierInfoList;
    }

    /**
     * This API is for fallback to support getAllowedCarriers too.
     *
     * Convert an array of CarrierInfo defined in
     * radio/aidl/android/hardware/radio/sim/CarrierInfo.aidl to a list of CarrierIdentifiers.
     *
     * @param carrierInfos array of CarrierInfo defined in
     *                     radio/aidl/android/hardware/radio/sim/CarrierInfo.aidl
     * @return The converted list of CarrierIdentifiers
     */
    public static List<CarrierIdentifier> convertAidlCarrierInfoListToHalCarrierList(
            android.hardware.radio.sim.CarrierInfo[] carrierInfos) {
        List<CarrierIdentifier> ret = new ArrayList<>();
        if (carrierInfos == null) {
            return ret;
        }
        for (android.hardware.radio.sim.CarrierInfo carrierInfo : carrierInfos) {
            String mcc = carrierInfo.mcc;
            String mnc = carrierInfo.mnc;
            String spn = carrierInfo.spn;
            String imsi = carrierInfo.imsiPrefix;
            String gid1 = carrierInfo.gid1;
            String gid2 = carrierInfo.gid2;
            ret.add(new CarrierIdentifier(mcc, mnc, spn, imsi, gid1, gid2));
        }
        return ret;
    }

    /**
     * Convert the sim policy defined in
     * radio/aidl/android/hardware/radio/sim/SimLockMultiSimPolicy.aidl to the equivalent sim
     * policy defined in android.telephony/CarrierRestrictionRules.MultiSimPolicy
     *
     * @param multiSimPolicy of type defined in SimLockMultiSimPolicy.aidl
     * @return int of type CarrierRestrictionRules.MultiSimPolicy
     */
    public static @CarrierRestrictionRules.MultiSimPolicy int convertAidlSimLockMultiSimPolicy(
            int multiSimPolicy) {
        switch (multiSimPolicy) {
            case android.hardware.radio.sim.SimLockMultiSimPolicy.ONE_VALID_SIM_MUST_BE_PRESENT:
                return CarrierRestrictionRules.MULTISIM_POLICY_ONE_VALID_SIM_MUST_BE_PRESENT;
            case android.hardware.radio.sim.SimLockMultiSimPolicy.APPLY_TO_ALL_SLOTS:
                return CarrierRestrictionRules.MULTISIM_POLICY_APPLY_TO_ALL_SLOTS;
            case android.hardware.radio.sim.SimLockMultiSimPolicy.APPLY_TO_ONLY_SLOT_1:
                return CarrierRestrictionRules.MULTISIM_POLICY_APPLY_TO_ONLY_SLOT_1;
            case android.hardware.radio.sim.SimLockMultiSimPolicy.VALID_SIM_MUST_PRESENT_ON_SLOT_1:
                return CarrierRestrictionRules.MULTISIM_POLICY_VALID_SIM_MUST_PRESENT_ON_SLOT_1;
            case android.hardware.radio.sim.SimLockMultiSimPolicy.
                    ACTIVE_SERVICE_ON_SLOT_1_TO_UNBLOCK_OTHER_SLOTS:
                return CarrierRestrictionRules.
                        MULTISIM_POLICY_ACTIVE_SERVICE_ON_SLOT_1_TO_UNBLOCK_OTHER_SLOTS;
            case android.hardware.radio.sim.SimLockMultiSimPolicy.
                    ACTIVE_SERVICE_ON_ANY_SLOT_TO_UNBLOCK_OTHER_SLOTS:
                return CarrierRestrictionRules.
                        MULTISIM_POLICY_ACTIVE_SERVICE_ON_ANY_SLOT_TO_UNBLOCK_OTHER_SLOTS;
            case android.hardware.radio.sim.SimLockMultiSimPolicy.ALL_SIMS_MUST_BE_VALID:
                return CarrierRestrictionRules.MULTISIM_POLICY_ALL_SIMS_MUST_BE_VALID;
            case android.hardware.radio.sim.SimLockMultiSimPolicy.SLOT_POLICY_OTHER:
                return CarrierRestrictionRules.MULTISIM_POLICY_SLOT_POLICY_OTHER;
            default:
                return CarrierRestrictionRules.MULTISIM_POLICY_NONE;
        }
    }

    /**
     * Convert CardStatus defined in radio/1.0, 1.5/types.hal to IccCardStatus
     * @param cardStatus CardStatus defined in radio/1.0, 1.5/types.hal
     * @return The converted IccCardStatus
     */
    public static IccCardStatus convertHalCardStatus(Object cardStatus) {
        final android.hardware.radio.V1_0.CardStatus cardStatus10;
        final android.hardware.radio.V1_5.CardStatus cardStatus15;
        if (cardStatus instanceof android.hardware.radio.V1_5.CardStatus) {
            cardStatus15 = (android.hardware.radio.V1_5.CardStatus) cardStatus;
            cardStatus10 = cardStatus15.base.base.base;
        } else if (cardStatus instanceof android.hardware.radio.V1_0.CardStatus) {
            cardStatus15 = null;
            cardStatus10 = (android.hardware.radio.V1_0.CardStatus) cardStatus;
        } else {
            cardStatus15 = null;
            cardStatus10 = null;
        }

        IccCardStatus iccCardStatus = new IccCardStatus();
        if (cardStatus10 != null) {
            iccCardStatus.setCardState(cardStatus10.cardState);
            iccCardStatus.setUniversalPinState(cardStatus10.universalPinState);
            iccCardStatus.mGsmUmtsSubscriptionAppIndex = cardStatus10.gsmUmtsSubscriptionAppIndex;
            iccCardStatus.mCdmaSubscriptionAppIndex =
                    Flags.cleanupCdma() ? -1 : cardStatus10.cdmaSubscriptionAppIndex;
            iccCardStatus.mImsSubscriptionAppIndex = cardStatus10.imsSubscriptionAppIndex;
            int numApplications = cardStatus10.applications.size();

            // limit to maximum allowed applications
            if (numApplications > com.android.internal.telephony.uicc.IccCardStatus.CARD_MAX_APPS) {
                numApplications = com.android.internal.telephony.uicc.IccCardStatus.CARD_MAX_APPS;
            }
            iccCardStatus.mApplications = new IccCardApplicationStatus[numApplications];
            for (int i = 0; i < numApplications; i++) {
                android.hardware.radio.V1_0.AppStatus rilAppStatus =
                        cardStatus10.applications.get(i);
                IccCardApplicationStatus appStatus = new IccCardApplicationStatus();
                appStatus.app_type = appStatus.AppTypeFromRILInt(rilAppStatus.appType);
                appStatus.app_state = appStatus.AppStateFromRILInt(rilAppStatus.appState);
                appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(
                        rilAppStatus.persoSubstate);
                appStatus.aid = rilAppStatus.aidPtr;
                appStatus.app_label = rilAppStatus.appLabelPtr;
                appStatus.pin1_replaced = rilAppStatus.pin1Replaced != 0;
                appStatus.pin1 = appStatus.PinStateFromRILInt(rilAppStatus.pin1);
                appStatus.pin2 = appStatus.PinStateFromRILInt(rilAppStatus.pin2);
                iccCardStatus.mApplications[i] = appStatus;
            }
        }
        if (cardStatus15 != null) {
            IccSlotPortMapping slotPortMapping = new IccSlotPortMapping();
            slotPortMapping.mPhysicalSlotIndex = cardStatus15.base.base.physicalSlotId;
            iccCardStatus.mSlotPortMapping = slotPortMapping;
            iccCardStatus.atr = cardStatus15.base.base.atr;
            iccCardStatus.iccid = cardStatus15.base.base.iccid;
            iccCardStatus.eid = cardStatus15.base.eid;
            int numApplications = cardStatus15.applications.size();

            // limit to maximum allowed applications
            if (numApplications > com.android.internal.telephony.uicc.IccCardStatus.CARD_MAX_APPS) {
                numApplications = com.android.internal.telephony.uicc.IccCardStatus.CARD_MAX_APPS;
            }
            iccCardStatus.mApplications = new IccCardApplicationStatus[numApplications];
            for (int i = 0; i < numApplications; i++) {
                android.hardware.radio.V1_5.AppStatus rilAppStatus =
                        cardStatus15.applications.get(i);
                IccCardApplicationStatus appStatus = new IccCardApplicationStatus();
                appStatus.app_type = appStatus.AppTypeFromRILInt(rilAppStatus.base.appType);
                appStatus.app_state = appStatus.AppStateFromRILInt(rilAppStatus.base.appState);
                appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(
                        rilAppStatus.persoSubstate);
                appStatus.aid = rilAppStatus.base.aidPtr;
                appStatus.app_label = rilAppStatus.base.appLabelPtr;
                appStatus.pin1_replaced = rilAppStatus.base.pin1Replaced != 0;
                appStatus.pin1 = appStatus.PinStateFromRILInt(rilAppStatus.base.pin1);
                appStatus.pin2 = appStatus.PinStateFromRILInt(rilAppStatus.base.pin2);
                iccCardStatus.mApplications[i] = appStatus;
            }
        }
        return iccCardStatus;
    }

    /**
     * Convert CardStatus defined in CardStatus.aidl to IccCardStatus
     * @param cardStatus CardStatus defined in CardStatus.aidl
     * @return The converted IccCardStatus
     */
    public static IccCardStatus convertHalCardStatus(
            android.hardware.radio.sim.CardStatus cardStatus) {
        IccCardStatus iccCardStatus = new IccCardStatus();
        iccCardStatus.setCardState(cardStatus.cardState);
        iccCardStatus.setMultipleEnabledProfilesMode(cardStatus.supportedMepMode);
        iccCardStatus.setUniversalPinState(cardStatus.universalPinState);
        iccCardStatus.mGsmUmtsSubscriptionAppIndex = cardStatus.gsmUmtsSubscriptionAppIndex;
        iccCardStatus.mCdmaSubscriptionAppIndex =
                Flags.cleanupCdma() ? -1 : cardStatus.cdmaSubscriptionAppIndex;
        iccCardStatus.mImsSubscriptionAppIndex = cardStatus.imsSubscriptionAppIndex;
        iccCardStatus.atr = cardStatus.atr;
        iccCardStatus.iccid = cardStatus.iccid;
        iccCardStatus.eid = cardStatus.eid;

        int numApplications = Math.min(cardStatus.applications.length,
                com.android.internal.telephony.uicc.IccCardStatus.CARD_MAX_APPS);
        iccCardStatus.mApplications = new IccCardApplicationStatus[numApplications];
        for (int i = 0; i < numApplications; i++) {
            android.hardware.radio.sim.AppStatus rilAppStatus = cardStatus.applications[i];
            IccCardApplicationStatus appStatus = new IccCardApplicationStatus();
            appStatus.app_type = appStatus.AppTypeFromRILInt(rilAppStatus.appType);
            appStatus.app_state = appStatus.AppStateFromRILInt(rilAppStatus.appState);
            appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(
                    rilAppStatus.persoSubstate);
            appStatus.aid = rilAppStatus.aidPtr;
            appStatus.app_label = rilAppStatus.appLabelPtr;
            appStatus.pin1_replaced = rilAppStatus.pin1Replaced;
            appStatus.pin1 = appStatus.PinStateFromRILInt(rilAppStatus.pin1);
            appStatus.pin2 = appStatus.PinStateFromRILInt(rilAppStatus.pin2);
            iccCardStatus.mApplications[i] = appStatus;
        }
        IccSlotPortMapping slotPortMapping = new IccSlotPortMapping();
        slotPortMapping.mPhysicalSlotIndex = cardStatus.slotMap.physicalSlotId;
        slotPortMapping.mPortIndex = PortUtils.convertFromHalPortIndex(
                cardStatus.slotMap.physicalSlotId, cardStatus.slotMap.portId,
                iccCardStatus.mCardState, iccCardStatus.mSupportedMepMode);
        iccCardStatus.mSlotPortMapping = slotPortMapping;
        return iccCardStatus;
    }

    /**
     * Convert PhonebookCapacity defined in radio/1.6/types.hal to AdnCapacity
     * @param pbCap PhonebookCapacity defined in radio/1.6/types.hal
     * @return The converted AdnCapacity
     */
    public static AdnCapacity convertHalPhonebookCapacity(
            android.hardware.radio.V1_6.PhonebookCapacity pbCap) {
        if (pbCap != null) {
            return new AdnCapacity(pbCap.maxAdnRecords, pbCap.usedAdnRecords, pbCap.maxEmailRecords,
                    pbCap.usedEmailRecords, pbCap.maxAdditionalNumberRecords,
                    pbCap.usedAdditionalNumberRecords, pbCap.maxNameLen, pbCap.maxNumberLen,
                    pbCap.maxEmailLen, pbCap.maxAdditionalNumberLen);
        }
        return null;
    }

    /**
     * Convert PhonebookCapacity defined in PhonebookCapacity.aidl to AdnCapacity
     * @param pbCap PhonebookCapacity defined in PhonebookCapacity.aidl
     * @return The converted AdnCapacity
     */
    public static AdnCapacity convertHalPhonebookCapacity(
            android.hardware.radio.sim.PhonebookCapacity pbCap) {
        if (pbCap != null) {
            return new AdnCapacity(pbCap.maxAdnRecords, pbCap.usedAdnRecords, pbCap.maxEmailRecords,
                    pbCap.usedEmailRecords, pbCap.maxAdditionalNumberRecords,
                    pbCap.usedAdditionalNumberRecords, pbCap.maxNameLen, pbCap.maxNumberLen,
                    pbCap.maxEmailLen, pbCap.maxAdditionalNumberLen);
        }
        return null;
    }

    /**
     * Convert PhonebookRecordInfo defined in radio/1.6/types.hal to SimPhonebookRecord
     * @param recInfo PhonebookRecordInfo defined in radio/1.6/types.hal
     * @return The converted SimPhonebookRecord
     */
    public static SimPhonebookRecord convertHalPhonebookRecordInfo(
            android.hardware.radio.V1_6.PhonebookRecordInfo recInfo) {
        String[] emails = recInfo.emails == null ? null
                : recInfo.emails.toArray(new String[recInfo.emails.size()]);
        String[] numbers = recInfo.additionalNumbers == null ? null
                : recInfo.additionalNumbers.toArray(new String[recInfo.additionalNumbers.size()]);
        return new SimPhonebookRecord(recInfo.recordId, recInfo.name, recInfo.number, emails,
                numbers);
    }

    /**
     * Convert PhonebookRecordInfo defined in PhonebookRecordInfo.aidl to SimPhonebookRecord
     * @param recInfo PhonebookRecordInfo defined in PhonebookRecordInfo.aidl
     * @return The converted SimPhonebookRecord
     */
    public static SimPhonebookRecord convertHalPhonebookRecordInfo(
            android.hardware.radio.sim.PhonebookRecordInfo recInfo) {
        return new SimPhonebookRecord(recInfo.recordId, recInfo.name, recInfo.number,
                recInfo.emails, recInfo.additionalNumbers);
    }

    /**
     * Convert to PhonebookRecordInfo defined in radio/1.6/types.hal
     * @param record SimPhonebookRecord to convert
     * @return The converted PhonebookRecordInfo defined in radio/1.6/types.hal
     */
    public static android.hardware.radio.V1_6.PhonebookRecordInfo convertToHalPhonebookRecordInfo(
            SimPhonebookRecord record) {
        if (record != null) {
            return record.toPhonebookRecordInfo();
        }
        return null;
    }

    /**
     * Convert to PhonebookRecordInfo.aidl
     * @param record SimPhonebookRecord to convert
     * @return The converted PhonebookRecordInfo
     */
    public static android.hardware.radio.sim.PhonebookRecordInfo
            convertToHalPhonebookRecordInfoAidl(SimPhonebookRecord record) {
        if (record != null) {
            return record.toPhonebookRecordInfoAidl();
        }
        return new android.hardware.radio.sim.PhonebookRecordInfo();
    }

    /**
     * Convert array of SimSlotStatus to IccSlotStatus
     * @param o object that represents array/list of SimSlotStatus
     * @return ArrayList of IccSlotStatus
     */
    public static ArrayList<IccSlotStatus> convertHalSlotStatus(Object o) {
        ArrayList<IccSlotStatus> response = new ArrayList<>();
        try {
            final android.hardware.radio.config.SimSlotStatus[] halSlotStatusArray =
                    (android.hardware.radio.config.SimSlotStatus[]) o;
            for (android.hardware.radio.config.SimSlotStatus slotStatus : halSlotStatusArray) {
                IccSlotStatus iccSlotStatus = new IccSlotStatus();
                iccSlotStatus.setCardState(slotStatus.cardState);
                int portCount = slotStatus.portInfo.length;
                iccSlotStatus.mSimPortInfos = new IccSimPortInfo[portCount];
                for (int i = 0; i < portCount; i++) {
                    IccSimPortInfo simPortInfo = new IccSimPortInfo();
                    simPortInfo.mIccId = slotStatus.portInfo[i].iccId;
                    // If port is not active, set invalid logical slot index(-1) irrespective of
                    // the modem response. For more info, check http://b/209035150
                    simPortInfo.mLogicalSlotIndex = slotStatus.portInfo[i].portActive
                            ? slotStatus.portInfo[i].logicalSlotId : -1;
                    simPortInfo.mPortActive = slotStatus.portInfo[i].portActive;
                    iccSlotStatus.mSimPortInfos[i] = simPortInfo;
                }
                iccSlotStatus.atr = slotStatus.atr;
                iccSlotStatus.eid = slotStatus.eid;
                iccSlotStatus.setMultipleEnabledProfilesMode(slotStatus.supportedMepMode);
                response.add(iccSlotStatus);
            }
            return response;
        } catch (ClassCastException ignore) { }
        try {
            final ArrayList<android.hardware.radio.config.V1_2.SimSlotStatus>
                    halSlotStatusArray =
                    (ArrayList<android.hardware.radio.config.V1_2.SimSlotStatus>) o;
            for (android.hardware.radio.config.V1_2.SimSlotStatus slotStatus :
                    halSlotStatusArray) {
                IccSlotStatus iccSlotStatus = new IccSlotStatus();
                iccSlotStatus.setCardState(slotStatus.base.cardState);
                // Old HAL versions does not support MEP, so only one port is available.
                iccSlotStatus.mSimPortInfos = new IccSimPortInfo[1];
                IccSimPortInfo simPortInfo = new IccSimPortInfo();
                simPortInfo.mIccId = slotStatus.base.iccid;
                simPortInfo.mPortActive = (slotStatus.base.slotState == IccSlotStatus.STATE_ACTIVE);
                // If port/slot is not active, set invalid logical slot index(-1) irrespective of
                // the modem response. For more info, check http://b/209035150
                simPortInfo.mLogicalSlotIndex = simPortInfo.mPortActive
                        ? slotStatus.base.logicalSlotId : -1;
                iccSlotStatus.mSimPortInfos[TelephonyManager.DEFAULT_PORT_INDEX] = simPortInfo;
                iccSlotStatus.atr = slotStatus.base.atr;
                iccSlotStatus.eid = slotStatus.eid;
                response.add(iccSlotStatus);
            }
            return response;
        } catch (ClassCastException ignore) { }
        try {
            final ArrayList<android.hardware.radio.config.V1_0.SimSlotStatus>
                    halSlotStatusArray =
                    (ArrayList<android.hardware.radio.config.V1_0.SimSlotStatus>) o;
            for (android.hardware.radio.config.V1_0.SimSlotStatus slotStatus :
                    halSlotStatusArray) {
                IccSlotStatus iccSlotStatus = new IccSlotStatus();
                iccSlotStatus.setCardState(slotStatus.cardState);
                // Old HAL versions does not support MEP, so only one port is available.
                iccSlotStatus.mSimPortInfos = new IccSimPortInfo[1];
                IccSimPortInfo simPortInfo = new IccSimPortInfo();
                simPortInfo.mIccId = slotStatus.iccid;
                simPortInfo.mPortActive = (slotStatus.slotState == IccSlotStatus.STATE_ACTIVE);
                // If port/slot is not active, set invalid logical slot index(-1) irrespective of
                // the modem response. For more info, check http://b/209035150
                simPortInfo.mLogicalSlotIndex = simPortInfo.mPortActive
                        ? slotStatus.logicalSlotId : -1;
                iccSlotStatus.mSimPortInfos[TelephonyManager.DEFAULT_PORT_INDEX] = simPortInfo;
                iccSlotStatus.atr = slotStatus.atr;
                response.add(iccSlotStatus);
            }
            return response;
        } catch (ClassCastException ignore) { }
        return response;
    }

    /**
     * Convert List<UiccSlotMapping> list to SlotPortMapping[]
     * @param slotMapping List<UiccSlotMapping> of slots mapping
     * @return SlotPortMapping[] of slots mapping
     */
    public static android.hardware.radio.config.SlotPortMapping[] convertSimSlotsMapping(
            List<UiccSlotMapping> slotMapping) {
        android.hardware.radio.config.SlotPortMapping[] res =
                new android.hardware.radio.config.SlotPortMapping[slotMapping.size()];
        for (UiccSlotMapping mapping : slotMapping) {
            int logicalSlotIdx = mapping.getLogicalSlotIndex();
            res[logicalSlotIdx] = new android.hardware.radio.config.SlotPortMapping();
            res[logicalSlotIdx].physicalSlotId = mapping.getPhysicalSlotIndex();
            res[logicalSlotIdx].portId = PortUtils.convertToHalPortIndex(
                    mapping.getPhysicalSlotIndex(), mapping.getPortIndex());
        }
        return res;
    }

    /** Convert a list of UiccSlotMapping to an ArrayList<Integer>.*/
    public static ArrayList<Integer> convertSlotMappingToList(
            List<UiccSlotMapping> slotMapping) {
        int[] physicalSlots = new int[slotMapping.size()];
        for (UiccSlotMapping mapping : slotMapping) {
            physicalSlots[mapping.getLogicalSlotIndex()] = mapping.getPhysicalSlotIndex();
        }
        return primitiveArrayToArrayList(physicalSlots);
    }


    /**
     * Convert PhoneCapability to telephony PhoneCapability.
     * @param deviceNrCapabilities device's nr capability array
     * @param o PhoneCapability to convert
     * @return converted PhoneCapability
     */
    public static PhoneCapability convertHalPhoneCapability(int[] deviceNrCapabilities, Object o) {
        int maxActiveVoiceCalls = 0;
        int maxActiveData = 0;
        boolean validationBeforeSwitchSupported = false;
        List<ModemInfo> logicalModemList = new ArrayList<>();
        if (o instanceof android.hardware.radio.config.PhoneCapability) {
            final android.hardware.radio.config.PhoneCapability phoneCapability =
                    (android.hardware.radio.config.PhoneCapability) o;
            maxActiveData = phoneCapability.maxActiveData;
            // If the maxActiveVoice field has been set, use that value. Otherwise, default to the
            // legacy behavior and rely on the maxActiveInternetData field:
            if (phoneCapability.maxActiveVoice ==
                    android.hardware.radio.config.PhoneCapability.UNKNOWN) {
                maxActiveVoiceCalls = phoneCapability.maxActiveInternetData;
            } else {
                maxActiveVoiceCalls = phoneCapability.maxActiveVoice;
            }
            validationBeforeSwitchSupported = phoneCapability.isInternetLingeringSupported;
            for (int modemId : phoneCapability.logicalModemIds) {
                logicalModemList.add(new ModemInfo(modemId));
            }
        } else if (o instanceof android.hardware.radio.config.V1_1.PhoneCapability) {
            final android.hardware.radio.config.V1_1.PhoneCapability phoneCapability =
                    (android.hardware.radio.config.V1_1.PhoneCapability) o;
            maxActiveData = phoneCapability.maxActiveData;
            // maxActiveInternetData defines how many logical modems can have internet PDN
            // connections simultaneously. For L+L DSDS modem it’s 1, and for DSDA modem it’s 2.
            maxActiveVoiceCalls = phoneCapability.maxActiveInternetData;
            validationBeforeSwitchSupported = phoneCapability.isInternetLingeringSupported;
            for (android.hardware.radio.config.V1_1.ModemInfo modemInfo :
                    phoneCapability.logicalModemList) {
                logicalModemList.add(new ModemInfo(modemInfo.modemId));
            }
        }
        return new PhoneCapability(maxActiveVoiceCalls, maxActiveData, logicalModemList,
                validationBeforeSwitchSupported, deviceNrCapabilities);
    }

    /**
     * Convert network scan type
     * @param scanType The network scan type
     * @return The converted EmergencyScanType
     */
    public static int convertEmergencyScanType(int scanType) {
        switch (scanType) {
            case DomainSelectionService.SCAN_TYPE_LIMITED_SERVICE:
                return android.hardware.radio.network.EmergencyScanType.LIMITED_SERVICE;
            case DomainSelectionService.SCAN_TYPE_FULL_SERVICE:
                return android.hardware.radio.network.EmergencyScanType.FULL_SERVICE;
            default:
                return android.hardware.radio.network.EmergencyScanType.NO_PREFERENCE;
        }
    }

    /**
     * Convert to EmergencyNetworkScanTrigger
     * @param accessNetwork The list of access network types
     * @param scanType The network scan type
     * @return The converted EmergencyNetworkScanTrigger
     */
    public static android.hardware.radio.network.EmergencyNetworkScanTrigger
            convertEmergencyNetworkScanTrigger(@NonNull int[] accessNetwork, int scanType) {
        int[] halAccessNetwork = new int[accessNetwork.length];
        for (int i = 0; i < accessNetwork.length; i++) {
            halAccessNetwork[i] = convertToHalAccessNetworkAidl(accessNetwork[i]);
        }

        android.hardware.radio.network.EmergencyNetworkScanTrigger scanRequest =
                new android.hardware.radio.network.EmergencyNetworkScanTrigger();

        scanRequest.accessNetwork = halAccessNetwork;
        scanRequest.scanType = convertEmergencyScanType(scanType);
        return scanRequest;
    }

    /**
     * Convert EmergencyRegResult.aidl to EmergencyRegistrationResult.
     * @param halResult EmergencyRegResult.aidl in HAL.
     * @return Converted EmergencyRegistrationResult.
     */
    public static EmergencyRegistrationResult convertHalEmergencyRegResult(
            android.hardware.radio.network.EmergencyRegResult halResult) {
        return new EmergencyRegistrationResult(
                halResult.accessNetwork,
                convertHalRegState(halResult.regState),
                halResult.emcDomain,
                halResult.isVopsSupported,
                halResult.isEmcBearerSupported,
                halResult.nwProvidedEmc,
                halResult.nwProvidedEmf,
                halResult.mcc,
                halResult.mnc,
                getCountryCodeForMccMnc(halResult.mcc, halResult.mnc));
    }

    private static @NonNull String getCountryCodeForMccMnc(
            @NonNull String mcc, @NonNull String mnc) {
        if (TextUtils.isEmpty(mcc)) return "";
        if (TextUtils.isEmpty(mnc)) mnc = "000";
        String operatorNumeric = TextUtils.concat(mcc, mnc).toString();

        MccTable.MccMnc mccMnc = MccTable.MccMnc.fromOperatorNumeric(operatorNumeric);
        return MccTable.geoCountryCodeForMccMnc(mccMnc);
    }

    /**
     * Convert RegResult.aidl to RegistrationState.
     * @param halRegState RegResult in HAL.
     * @return Converted RegistrationState.
     */
    public static @NetworkRegistrationInfo.RegistrationState int convertHalRegState(
            int halRegState) {
        switch (halRegState) {
            case android.hardware.radio.network.RegState.NOT_REG_MT_NOT_SEARCHING_OP:
            case android.hardware.radio.network.RegState.NOT_REG_MT_NOT_SEARCHING_OP_EM:
                return NetworkRegistrationInfo.REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;
            case android.hardware.radio.network.RegState.REG_HOME:
                return NetworkRegistrationInfo.REGISTRATION_STATE_HOME;
            case android.hardware.radio.network.RegState.NOT_REG_MT_SEARCHING_OP:
            case android.hardware.radio.network.RegState.NOT_REG_MT_SEARCHING_OP_EM:
                return NetworkRegistrationInfo.REGISTRATION_STATE_NOT_REGISTERED_SEARCHING;
            case android.hardware.radio.network.RegState.REG_DENIED:
            case android.hardware.radio.network.RegState.REG_DENIED_EM:
                return NetworkRegistrationInfo.REGISTRATION_STATE_DENIED;
            case android.hardware.radio.network.RegState.UNKNOWN:
            case android.hardware.radio.network.RegState.UNKNOWN_EM:
                return NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN;
            case android.hardware.radio.network.RegState.REG_ROAMING:
                return NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING;
            default:
                return NetworkRegistrationInfo.REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;
        }
    }

    /** Converts the array of network types to readable String array */
    public static @NonNull String accessNetworkTypesToString(
            @NonNull @AccessNetworkConstants.RadioAccessNetworkType int[] accessNetworkTypes) {
        int length = accessNetworkTypes.length;
        StringBuilder sb = new StringBuilder("{");
        if (length > 0) {
            sb.append(Arrays.stream(accessNetworkTypes)
                    .mapToObj(RILUtils::accessNetworkTypeToString)
                    .collect(Collectors.joining(",")));
        }
        sb.append("}");
        return sb.toString();
    }

    private static @NonNull String accessNetworkTypeToString(
            @AccessNetworkConstants.RadioAccessNetworkType int accessNetworkType) {
        switch (accessNetworkType) {
            case AccessNetworkConstants.AccessNetworkType.UNKNOWN: return "UNKNOWN";
            case AccessNetworkConstants.AccessNetworkType.GERAN: return "GERAN";
            case AccessNetworkConstants.AccessNetworkType.UTRAN: return "UTRAN";
            case AccessNetworkConstants.AccessNetworkType.EUTRAN: return "EUTRAN";
            case AccessNetworkConstants.AccessNetworkType.CDMA2000: return "CDMA2000";
            case AccessNetworkConstants.AccessNetworkType.IWLAN: return "IWLAN";
            case AccessNetworkConstants.AccessNetworkType.NGRAN: return "NGRAN";
            default: return Integer.toString(accessNetworkType);
        }
    }

    /** Converts scan type to readable String */
    public static @NonNull String scanTypeToString(
            @DomainSelectionService.EmergencyScanType int scanType) {
        switch (scanType) {
            case DomainSelectionService.SCAN_TYPE_LIMITED_SERVICE:
                return "LIMITED_SERVICE";
            case DomainSelectionService.SCAN_TYPE_FULL_SERVICE:
                return "FULL_SERVICE";
            default:
                return "NO_PREFERENCE";
        }
    }

    /** Convert IMS deregistration reason */
    public static @ImsDeregistrationReason int convertHalDeregistrationReason(int reason) {
        switch (reason) {
            case android.hardware.radio.ims.ImsDeregistrationReason.REASON_SIM_REMOVED:
                return ImsRegistrationImplBase.REASON_SIM_REMOVED;
            case android.hardware.radio.ims.ImsDeregistrationReason.REASON_SIM_REFRESH:
                return ImsRegistrationImplBase.REASON_SIM_REFRESH;
            case android.hardware.radio.ims.ImsDeregistrationReason
                    .REASON_ALLOWED_NETWORK_TYPES_CHANGED:
                return ImsRegistrationImplBase.REASON_ALLOWED_NETWORK_TYPES_CHANGED;
            default:
                return ImsRegistrationImplBase.REASON_UNKNOWN;
        }
    }

    /**
     * Convert the IMS traffic type.
     * @param trafficType IMS traffic type like registration, voice, video, SMS, emergency, and etc.
     * @return The converted IMS traffic type.
     */
    public static int convertImsTrafficType(@MmTelFeature.ImsTrafficType int trafficType) {
        switch (trafficType) {
            case MmTelFeature.IMS_TRAFFIC_TYPE_EMERGENCY:
                return android.hardware.radio.ims.ImsTrafficType.EMERGENCY;
            case MmTelFeature.IMS_TRAFFIC_TYPE_EMERGENCY_SMS:
                return android.hardware.radio.ims.ImsTrafficType.EMERGENCY_SMS;
            case MmTelFeature.IMS_TRAFFIC_TYPE_VOICE:
                return android.hardware.radio.ims.ImsTrafficType.VOICE;
            case MmTelFeature.IMS_TRAFFIC_TYPE_VIDEO:
                return android.hardware.radio.ims.ImsTrafficType.VIDEO;
            case MmTelFeature.IMS_TRAFFIC_TYPE_SMS:
                return android.hardware.radio.ims.ImsTrafficType.SMS;
            case MmTelFeature.IMS_TRAFFIC_TYPE_REGISTRATION:
                return android.hardware.radio.ims.ImsTrafficType.REGISTRATION;
        }
        return android.hardware.radio.ims.ImsTrafficType.UT_XCAP;
    }

    /**
     * Convert the IMS traffic direction.
     * @param trafficDirection Indicates the traffic direction.
     * @return The converted IMS traffic direction.
     */
    public static int convertImsTrafficDirection(
            @MmTelFeature.ImsTrafficDirection int trafficDirection) {
        switch (trafficDirection) {
            case MmTelFeature.IMS_TRAFFIC_DIRECTION_INCOMING:
                return android.hardware.radio.ims.ImsCall.Direction.INCOMING;
            default:
                return android.hardware.radio.ims.ImsCall.Direction.OUTGOING;
        }
    }

    /**
     * Convert the IMS connection failure reason.
     * @param halReason  Specifies the reason that IMS connection failed.
     * @return The converted IMS connection failure reason.
     */
    public static @ConnectionFailureInfo.FailureReason int convertHalConnectionFailureReason(
            int halReason) {
        switch (halReason) {
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_ACCESS_DENIED:
                return ConnectionFailureInfo.REASON_ACCESS_DENIED;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_NAS_FAILURE:
                return ConnectionFailureInfo.REASON_NAS_FAILURE;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_RACH_FAILURE:
                return ConnectionFailureInfo.REASON_RACH_FAILURE;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_RLC_FAILURE:
                return ConnectionFailureInfo.REASON_RLC_FAILURE;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_RRC_REJECT:
                return ConnectionFailureInfo.REASON_RRC_REJECT;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_RRC_TIMEOUT:
                return ConnectionFailureInfo.REASON_RRC_TIMEOUT;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_NO_SERVICE:
                return ConnectionFailureInfo.REASON_NO_SERVICE;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_PDN_NOT_AVAILABLE:
                return ConnectionFailureInfo.REASON_PDN_NOT_AVAILABLE;
            case android.hardware.radio.ims.ConnectionFailureInfo
                    .ConnectionFailureReason.REASON_RF_BUSY:
                return ConnectionFailureInfo.REASON_RF_BUSY;
        }
        return ConnectionFailureInfo.REASON_UNSPECIFIED;
    }

    /** Append the data to the end of an ArrayList */
    public static void appendPrimitiveArrayToArrayList(byte[] src, ArrayList<Byte> dst) {
        for (byte b : src) {
            dst.add(b);
        }
    }

    /** Convert a primitive byte array to an ArrayList<Integer>. */
    public static ArrayList<Byte> primitiveArrayToArrayList(byte[] arr) {
        ArrayList<Byte> arrayList = new ArrayList<>(arr.length);
        for (byte b : arr) {
            arrayList.add(b);
        }
        return arrayList;
    }

    /** Convert a primitive int array to an ArrayList<Integer>. */
    public static ArrayList<Integer> primitiveArrayToArrayList(int[] arr) {
        ArrayList<Integer> arrayList = new ArrayList<>(arr.length);
        for (int i : arr) {
            arrayList.add(i);
        }
        return arrayList;
    }

    /** Convert a primitive String array to an ArrayList<String>. */
    public static ArrayList<String> primitiveArrayToArrayList(String[] arr) {
        return new ArrayList<>(Arrays.asList(arr));
    }

    /** Convert an ArrayList of Bytes to an exactly-sized primitive array */
    public static byte[] arrayListToPrimitiveArray(ArrayList<Byte> bytes) {
        byte[] ret = new byte[bytes.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = bytes.get(i);
        }
        return ret;
    }

    /** Convert null to an empty String */
    public static String convertNullToEmptyString(String string) {
        return string != null ? string : "";
    }

    /**
     * Convert setup data reason to string.
     *
     * @param reason The reason for setup data call.
     * @return The reason in string format.
     */
    public static String setupDataReasonToString(@SetupDataReason int reason) {
        switch (reason) {
            case DataService.REQUEST_REASON_NORMAL:
                return "NORMAL";
            case DataService.REQUEST_REASON_HANDOVER:
                return "HANDOVER";
            case DataService.REQUEST_REASON_UNKNOWN:
                return "UNKNOWN";
            default:
                return "UNKNOWN(" + reason + ")";
        }
    }

    /**
     * Convert deactivate data reason to string.
     *
     * @param reason The reason for deactivate data call.
     * @return The reason in string format.
     */
    public static String deactivateDataReasonToString(@DeactivateDataReason int reason) {
        switch (reason) {
            case DataService.REQUEST_REASON_NORMAL:
                return "NORMAL";
            case DataService.REQUEST_REASON_HANDOVER:
                return "HANDOVER";
            case DataService.REQUEST_REASON_SHUTDOWN:
                return "SHUTDOWN";
            case DataService.REQUEST_REASON_UNKNOWN:
                return "UNKNOWN";
            default:
                return "UNKNOWN(" + reason + ")";
        }
    }

    /**
     * RIL request to String
     * @param request request
     * @return The converted String request
     */
    public static String requestToString(int request) {
        switch(request) {
            case RIL_REQUEST_GET_SIM_STATUS:
                return "GET_SIM_STATUS";
            case RIL_REQUEST_ENTER_SIM_PIN:
                return "ENTER_SIM_PIN";
            case RIL_REQUEST_ENTER_SIM_PUK:
                return "ENTER_SIM_PUK";
            case RIL_REQUEST_ENTER_SIM_PIN2:
                return "ENTER_SIM_PIN2";
            case RIL_REQUEST_ENTER_SIM_PUK2:
                return "ENTER_SIM_PUK2";
            case RIL_REQUEST_CHANGE_SIM_PIN:
                return "CHANGE_SIM_PIN";
            case RIL_REQUEST_CHANGE_SIM_PIN2:
                return "CHANGE_SIM_PIN2";
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION:
                return "ENTER_NETWORK_DEPERSONALIZATION";
            case RIL_REQUEST_GET_CURRENT_CALLS:
                return "GET_CURRENT_CALLS";
            case RIL_REQUEST_DIAL:
                return "DIAL";
            case RIL_REQUEST_GET_IMSI:
                return "GET_IMSI";
            case RIL_REQUEST_HANGUP:
                return "HANGUP";
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND:
                return "HANGUP_WAITING_OR_BACKGROUND";
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND:
                return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE:
                return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            case RIL_REQUEST_CONFERENCE:
                return "CONFERENCE";
            case RIL_REQUEST_UDUB:
                return "UDUB";
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE:
                return "LAST_CALL_FAIL_CAUSE";
            case RIL_REQUEST_SIGNAL_STRENGTH:
                return "SIGNAL_STRENGTH";
            case RIL_REQUEST_VOICE_REGISTRATION_STATE:
                return "VOICE_REGISTRATION_STATE";
            case RIL_REQUEST_DATA_REGISTRATION_STATE:
                return "DATA_REGISTRATION_STATE";
            case RIL_REQUEST_OPERATOR:
                return "OPERATOR";
            case RIL_REQUEST_RADIO_POWER:
                return "RADIO_POWER";
            case RIL_REQUEST_DTMF:
                return "DTMF";
            case RIL_REQUEST_SEND_SMS:
                return "SEND_SMS";
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE:
                return "SEND_SMS_EXPECT_MORE";
            case RIL_REQUEST_SETUP_DATA_CALL:
                return "SETUP_DATA_CALL";
            case RIL_REQUEST_SIM_IO:
                return "SIM_IO";
            case RIL_REQUEST_SEND_USSD:
                return "SEND_USSD";
            case RIL_REQUEST_CANCEL_USSD:
                return "CANCEL_USSD";
            case RIL_REQUEST_GET_CLIR:
                return "GET_CLIR";
            case RIL_REQUEST_SET_CLIR:
                return "SET_CLIR";
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS:
                return "QUERY_CALL_FORWARD_STATUS";
            case RIL_REQUEST_SET_CALL_FORWARD:
                return "SET_CALL_FORWARD";
            case RIL_REQUEST_QUERY_CALL_WAITING:
                return "QUERY_CALL_WAITING";
            case RIL_REQUEST_SET_CALL_WAITING:
                return "SET_CALL_WAITING";
            case RIL_REQUEST_SMS_ACKNOWLEDGE:
                return "SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GET_IMEI:
                return "GET_IMEI";
            case RIL_REQUEST_GET_IMEISV:
                return "GET_IMEISV";
            case RIL_REQUEST_ANSWER:
                return "ANSWER";
            case RIL_REQUEST_DEACTIVATE_DATA_CALL:
                return "DEACTIVATE_DATA_CALL";
            case RIL_REQUEST_QUERY_FACILITY_LOCK:
                return "QUERY_FACILITY_LOCK";
            case RIL_REQUEST_SET_FACILITY_LOCK:
                return "SET_FACILITY_LOCK";
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD:
                return "CHANGE_BARRING_PASSWORD";
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE:
                return "QUERY_NETWORK_SELECTION_MODE";
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC:
                return "SET_NETWORK_SELECTION_AUTOMATIC";
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL:
                return "SET_NETWORK_SELECTION_MANUAL";
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS :
                return "QUERY_AVAILABLE_NETWORKS ";
            case RIL_REQUEST_DTMF_START:
                return "DTMF_START";
            case RIL_REQUEST_DTMF_STOP:
                return "DTMF_STOP";
            case RIL_REQUEST_BASEBAND_VERSION:
                return "BASEBAND_VERSION";
            case RIL_REQUEST_SEPARATE_CONNECTION:
                return "SEPARATE_CONNECTION";
            case RIL_REQUEST_SET_MUTE:
                return "SET_MUTE";
            case RIL_REQUEST_GET_MUTE:
                return "GET_MUTE";
            case RIL_REQUEST_QUERY_CLIP:
                return "QUERY_CLIP";
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE:
                return "LAST_DATA_CALL_FAIL_CAUSE";
            case RIL_REQUEST_DATA_CALL_LIST:
                return "DATA_CALL_LIST";
            case RIL_REQUEST_RESET_RADIO:
                return "RESET_RADIO";
            case RIL_REQUEST_OEM_HOOK_RAW:
                return "OEM_HOOK_RAW";
            case RIL_REQUEST_OEM_HOOK_STRINGS:
                return "OEM_HOOK_STRINGS";
            case RIL_REQUEST_SCREEN_STATE:
                return "SCREEN_STATE";
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION:
                return "SET_SUPP_SVC_NOTIFICATION";
            case RIL_REQUEST_WRITE_SMS_TO_SIM:
                return "WRITE_SMS_TO_SIM";
            case RIL_REQUEST_DELETE_SMS_ON_SIM:
                return "DELETE_SMS_ON_SIM";
            case RIL_REQUEST_SET_BAND_MODE:
                return "SET_BAND_MODE";
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE:
                return "QUERY_AVAILABLE_BAND_MODE";
            case RIL_REQUEST_STK_GET_PROFILE:
                return "STK_GET_PROFILE";
            case RIL_REQUEST_STK_SET_PROFILE:
                return "STK_SET_PROFILE";
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND:
                return "STK_SEND_ENVELOPE_COMMAND";
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE:
                return "STK_SEND_TERMINAL_RESPONSE";
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM:
                return "STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER:
                return "EXPLICIT_CALL_TRANSFER";
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE:
                return "SET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE:
                return "GET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS:
                return "GET_NEIGHBORING_CELL_IDS";
            case RIL_REQUEST_SET_LOCATION_UPDATES:
                return "SET_LOCATION_UPDATES";
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE:
                return "CDMA_SET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE:
                return "CDMA_SET_ROAMING_PREFERENCE";
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE:
                return "CDMA_QUERY_ROAMING_PREFERENCE";
            case RIL_REQUEST_SET_TTY_MODE:
                return "SET_TTY_MODE";
            case RIL_REQUEST_QUERY_TTY_MODE:
                return "QUERY_TTY_MODE";
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE:
                return "CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE:
                return "CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_FLASH:
                return "CDMA_FLASH";
            case RIL_REQUEST_CDMA_BURST_DTMF:
                return "CDMA_BURST_DTMF";
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY:
                return "CDMA_VALIDATE_AND_WRITE_AKEY";
            case RIL_REQUEST_CDMA_SEND_SMS:
                return "CDMA_SEND_SMS";
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE:
                return "CDMA_SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG:
                return "GSM_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG:
                return "GSM_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION:
                return "GSM_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG:
                return "CDMA_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG:
                return "CDMA_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION:
                return "CDMA_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_SUBSCRIPTION:
                return "CDMA_SUBSCRIPTION";
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM:
                return "CDMA_WRITE_SMS_TO_RUIM";
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM:
                return "CDMA_DELETE_SMS_ON_RUIM";
            case RIL_REQUEST_DEVICE_IDENTITY:
                return "DEVICE_IDENTITY";
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE:
                return "EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_REQUEST_GET_SMSC_ADDRESS:
                return "GET_SMSC_ADDRESS";
            case RIL_REQUEST_SET_SMSC_ADDRESS:
                return "SET_SMSC_ADDRESS";
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS:
                return "REPORT_SMS_MEMORY_STATUS";
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING:
                return "REPORT_STK_SERVICE_IS_RUNNING";
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE:
                return "CDMA_GET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_ISIM_AUTHENTICATION:
                return "ISIM_AUTHENTICATION";
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU:
                return "ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS:
                return "STK_SEND_ENVELOPE_WITH_STATUS";
            case RIL_REQUEST_VOICE_RADIO_TECH:
                return "VOICE_RADIO_TECH";
            case RIL_REQUEST_GET_CELL_INFO_LIST:
                return "GET_CELL_INFO_LIST";
            case RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE:
                return "SET_CELL_INFO_LIST_RATE";
            case RIL_REQUEST_SET_INITIAL_ATTACH_APN:
                return "SET_INITIAL_ATTACH_APN";
            case RIL_REQUEST_IMS_REGISTRATION_STATE:
                return "IMS_REGISTRATION_STATE";
            case RIL_REQUEST_IMS_SEND_SMS:
                return "IMS_SEND_SMS";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC:
                return "SIM_TRANSMIT_APDU_BASIC";
            case RIL_REQUEST_SIM_OPEN_CHANNEL:
                return "SIM_OPEN_CHANNEL";
            case RIL_REQUEST_SIM_CLOSE_CHANNEL:
                return "SIM_CLOSE_CHANNEL";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL:
                return "SIM_TRANSMIT_APDU_CHANNEL";
            case RIL_REQUEST_NV_READ_ITEM:
                return "NV_READ_ITEM";
            case RIL_REQUEST_NV_WRITE_ITEM:
                return "NV_WRITE_ITEM";
            case RIL_REQUEST_NV_WRITE_CDMA_PRL:
                return "NV_WRITE_CDMA_PRL";
            case RIL_REQUEST_NV_RESET_CONFIG:
                return "NV_RESET_CONFIG";
            case RIL_REQUEST_SET_UICC_SUBSCRIPTION:
                return "SET_UICC_SUBSCRIPTION";
            case RIL_REQUEST_ALLOW_DATA:
                return "ALLOW_DATA";
            case RIL_REQUEST_GET_HARDWARE_CONFIG:
                return "GET_HARDWARE_CONFIG";
            case RIL_REQUEST_SIM_AUTHENTICATION:
                return "SIM_AUTHENTICATION";
            case RIL_REQUEST_GET_DC_RT_INFO:
                return "GET_DC_RT_INFO";
            case RIL_REQUEST_SET_DC_RT_INFO_RATE:
                return "SET_DC_RT_INFO_RATE";
            case RIL_REQUEST_SET_DATA_PROFILE:
                return "SET_DATA_PROFILE";
            case RIL_REQUEST_SHUTDOWN:
                return "SHUTDOWN";
            case RIL_REQUEST_GET_RADIO_CAPABILITY:
                return "GET_RADIO_CAPABILITY";
            case RIL_REQUEST_SET_RADIO_CAPABILITY:
                return "SET_RADIO_CAPABILITY";
            case RIL_REQUEST_START_LCE:
                return "START_LCE";
            case RIL_REQUEST_STOP_LCE:
                return "STOP_LCE";
            case RIL_REQUEST_PULL_LCEDATA:
                return "PULL_LCEDATA";
            case RIL_REQUEST_GET_ACTIVITY_INFO:
                return "GET_ACTIVITY_INFO";
            case RIL_REQUEST_SET_ALLOWED_CARRIERS:
                return "SET_ALLOWED_CARRIERS";
            case RIL_REQUEST_GET_ALLOWED_CARRIERS:
                return "GET_ALLOWED_CARRIERS";
            case RIL_REQUEST_SEND_DEVICE_STATE:
                return "SEND_DEVICE_STATE";
            case RIL_REQUEST_SET_UNSOLICITED_RESPONSE_FILTER:
                return "SET_UNSOLICITED_RESPONSE_FILTER";
            case RIL_REQUEST_SET_SIM_CARD_POWER:
                return "SET_SIM_CARD_POWER";
            case RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION:
                return "SET_CARRIER_INFO_IMSI_ENCRYPTION";
            case RIL_REQUEST_START_NETWORK_SCAN:
                return "START_NETWORK_SCAN";
            case RIL_REQUEST_STOP_NETWORK_SCAN:
                return "STOP_NETWORK_SCAN";
            case RIL_REQUEST_START_KEEPALIVE:
                return "START_KEEPALIVE";
            case RIL_REQUEST_STOP_KEEPALIVE:
                return "STOP_KEEPALIVE";
            case RIL_REQUEST_ENABLE_MODEM:
                return "ENABLE_MODEM";
            case RIL_REQUEST_GET_MODEM_STATUS:
                return "GET_MODEM_STATUS";
            case RIL_REQUEST_CDMA_SEND_SMS_EXPECT_MORE:
                return "CDMA_SEND_SMS_EXPECT_MORE";
            case RIL_REQUEST_GET_SIM_PHONEBOOK_CAPACITY:
                return "GET_SIM_PHONEBOOK_CAPACITY";
            case RIL_REQUEST_GET_SIM_PHONEBOOK_RECORDS:
                return "GET_SIM_PHONEBOOK_RECORDS";
            case RIL_REQUEST_UPDATE_SIM_PHONEBOOK_RECORD:
                return "UPDATE_SIM_PHONEBOOK_RECORD";
            case RIL_REQUEST_DEVICE_IMEI:
                return "DEVICE_IMEI";
            /* The following requests are not defined in RIL.h */
            case RIL_REQUEST_GET_SLOT_STATUS:
                return "GET_SLOT_STATUS";
            case RIL_REQUEST_SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING:
                return "SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING";
            case RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA:
                return "SET_SIGNAL_STRENGTH_REPORTING_CRITERIA";
            case RIL_REQUEST_SET_LINK_CAPACITY_REPORTING_CRITERIA:
                return "SET_LINK_CAPACITY_REPORTING_CRITERIA";
            case RIL_REQUEST_SET_PREFERRED_DATA_MODEM:
                return "SET_PREFERRED_DATA_MODEM";
            case RIL_REQUEST_EMERGENCY_DIAL:
                return "EMERGENCY_DIAL";
            case RIL_REQUEST_GET_PHONE_CAPABILITY:
                return "GET_PHONE_CAPABILITY";
            case RIL_REQUEST_SWITCH_DUAL_SIM_CONFIG:
                return "SWITCH_DUAL_SIM_CONFIG";
            case RIL_REQUEST_ENABLE_UICC_APPLICATIONS:
                return "ENABLE_UICC_APPLICATIONS";
            case RIL_REQUEST_GET_UICC_APPLICATIONS_ENABLEMENT:
                return "GET_UICC_APPLICATIONS_ENABLEMENT";
            case RIL_REQUEST_SET_SYSTEM_SELECTION_CHANNELS:
                return "SET_SYSTEM_SELECTION_CHANNELS";
            case RIL_REQUEST_GET_BARRING_INFO:
                return "GET_BARRING_INFO";
            case RIL_REQUEST_ENTER_SIM_DEPERSONALIZATION:
                return "ENTER_SIM_DEPERSONALIZATION";
            case RIL_REQUEST_ENABLE_NR_DUAL_CONNECTIVITY:
                return "ENABLE_NR_DUAL_CONNECTIVITY";
            case RIL_REQUEST_IS_NR_DUAL_CONNECTIVITY_ENABLED:
                return "IS_NR_DUAL_CONNECTIVITY_ENABLED";
            case RIL_REQUEST_ALLOCATE_PDU_SESSION_ID:
                return "ALLOCATE_PDU_SESSION_ID";
            case RIL_REQUEST_RELEASE_PDU_SESSION_ID:
                return "RELEASE_PDU_SESSION_ID";
            case RIL_REQUEST_START_HANDOVER:
                return "START_HANDOVER";
            case RIL_REQUEST_CANCEL_HANDOVER:
                return "CANCEL_HANDOVER";
            case RIL_REQUEST_GET_SYSTEM_SELECTION_CHANNELS:
                return "GET_SYSTEM_SELECTION_CHANNELS";
            case RIL_REQUEST_GET_HAL_DEVICE_CAPABILITIES:
                return "GET_HAL_DEVICE_CAPABILITIES";
            case RIL_REQUEST_SET_DATA_THROTTLING:
                return "SET_DATA_THROTTLING";
            case RIL_REQUEST_SET_ALLOWED_NETWORK_TYPES_BITMAP:
                return "SET_ALLOWED_NETWORK_TYPES_BITMAP";
            case RIL_REQUEST_GET_ALLOWED_NETWORK_TYPES_BITMAP:
                return "GET_ALLOWED_NETWORK_TYPES_BITMAP";
            case RIL_REQUEST_GET_SLICING_CONFIG:
                return "GET_SLICING_CONFIG";
            case RIL_REQUEST_ENABLE_VONR:
                return "ENABLE_VONR";
            case RIL_REQUEST_IS_VONR_ENABLED:
                return "IS_VONR_ENABLED";
            case RIL_REQUEST_SET_USAGE_SETTING:
                return "SET_USAGE_SETTING";
            case RIL_REQUEST_GET_USAGE_SETTING:
                return "GET_USAGE_SETTING";
            case RIL_REQUEST_SET_EMERGENCY_MODE:
                return "SET_EMERGENCY_MODE";
            case RIL_REQUEST_TRIGGER_EMERGENCY_NETWORK_SCAN:
                return "TRIGGER_EMERGENCY_NETWORK_SCAN";
            case RIL_REQUEST_CANCEL_EMERGENCY_NETWORK_SCAN:
                return "CANCEL_EMERGENCY_NETWORK_SCAN";
            case RIL_REQUEST_EXIT_EMERGENCY_MODE:
                return "EXIT_EMERGENCY_MODE";
            case RIL_REQUEST_SET_SRVCC_CALL_INFO:
                return "SET_SRVCC_CALL_INFO";
            case RIL_REQUEST_UPDATE_IMS_REGISTRATION_INFO:
                return "UPDATE_IMS_REGISTRATION_INFO";
            case RIL_REQUEST_START_IMS_TRAFFIC:
                return "START_IMS_TRAFFIC";
            case RIL_REQUEST_STOP_IMS_TRAFFIC:
                return "STOP_IMS_TRAFFIC";
            case RIL_REQUEST_SEND_ANBR_QUERY:
                return "SEND_ANBR_QUERY";
            case RIL_REQUEST_TRIGGER_EPS_FALLBACK:
                return "TRIGGER_EPS_FALLBACK";
            case RIL_REQUEST_SET_NULL_CIPHER_AND_INTEGRITY_ENABLED:
                return "SET_NULL_CIPHER_AND_INTEGRITY_ENABLED";
            case RIL_REQUEST_IS_NULL_CIPHER_AND_INTEGRITY_ENABLED:
                return "IS_NULL_CIPHER_AND_INTEGRITY_ENABLED";
            case RIL_REQUEST_UPDATE_IMS_CALL_STATUS:
                return "UPDATE_IMS_CALL_STATUS";
            case RIL_REQUEST_SET_N1_MODE_ENABLED:
                return "SET_N1_MODE_ENABLED";
            case RIL_REQUEST_IS_N1_MODE_ENABLED:
                return "IS_N1_MODE_ENABLED";
            case RIL_REQUEST_SET_LOCATION_PRIVACY_SETTING:
                return "SET_LOCATION_PRIVACY_SETTING";
            case RIL_REQUEST_GET_LOCATION_PRIVACY_SETTING:
                return "GET_LOCATION_PRIVACY_SETTING";
            case RIL_REQUEST_IS_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED:
                return "IS_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED";
            case RIL_REQUEST_SET_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED:
                return "SET_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED";
            case RIL_REQUEST_SET_SECURITY_ALGORITHMS_UPDATED_ENABLED:
                return "SET_SECURITY_ALGORITHMS_UPDATED_ENABLED";
            case RIL_REQUEST_IS_SECURITY_ALGORITHMS_UPDATED_ENABLED:
                return "IS_SECURITY_ALGORITHMS_UPDATED_ENABLED";
            case RIL_REQUEST_GET_SIMULTANEOUS_CALLING_SUPPORT:
                return "GET_SIMULTANEOUS_CALLING_SUPPORT";
            case RIL_REQUEST_SET_SATELLITE_PLMN:
                return "SET_SATELLITE_PLMN";
            case RIL_REQUEST_SET_SATELLITE_ENABLED_FOR_CARRIER:
                return "SET_SATELLITE_ENABLED_FOR_CARRIER";
            case RIL_REQUEST_IS_SATELLITE_ENABLED_FOR_CARRIER:
                return "IS_SATELLITE_ENABLED_FOR_CARRIER";
            default:
                return "<unknown request " + request + ">";
        }
    }

    /**
     * RIL response to String
     * @param response response
     * @return The converted String response
     */
    public static String responseToString(int response) {
        switch (response) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
                return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED:
                return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_NETWORK_STATE_CHANGED:
                return "UNSOL_RESPONSE_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_NEW_SMS:
                return "UNSOL_RESPONSE_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT:
                return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
            case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM:
                return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
            case RIL_UNSOL_ON_USSD:
                return "UNSOL_ON_USSD";
            case RIL_UNSOL_ON_USSD_REQUEST:
                return "UNSOL_ON_USSD_REQUEST";
            case RIL_UNSOL_NITZ_TIME_RECEIVED:
                return "UNSOL_NITZ_TIME_RECEIVED";
            case RIL_UNSOL_SIGNAL_STRENGTH:
                return "UNSOL_SIGNAL_STRENGTH";
            case RIL_UNSOL_DATA_CALL_LIST_CHANGED:
                return "UNSOL_DATA_CALL_LIST_CHANGED";
            case RIL_UNSOL_SUPP_SVC_NOTIFICATION:
                return "UNSOL_SUPP_SVC_NOTIFICATION";
            case RIL_UNSOL_STK_SESSION_END:
                return "UNSOL_STK_SESSION_END";
            case RIL_UNSOL_STK_PROACTIVE_COMMAND:
                return "UNSOL_STK_PROACTIVE_COMMAND";
            case RIL_UNSOL_STK_EVENT_NOTIFY:
                return "UNSOL_STK_EVENT_NOTIFY";
            case RIL_UNSOL_STK_CALL_SETUP:
                return "UNSOL_STK_CALL_SETUP";
            case RIL_UNSOL_SIM_SMS_STORAGE_FULL:
                return "UNSOL_SIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_SIM_REFRESH:
                return "UNSOL_SIM_REFRESH";
            case RIL_UNSOL_CALL_RING:
                return "UNSOL_CALL_RING";
            case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED:
                return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
            case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS:
                return "UNSOL_RESPONSE_CDMA_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS:
                return "UNSOL_RESPONSE_NEW_BROADCAST_SMS";
            case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL:
                return "UNSOL_CDMA_RUIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_RESTRICTED_STATE_CHANGED:
                return "UNSOL_RESTRICTED_STATE_CHANGED";
            case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE:
                return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_CDMA_CALL_WAITING:
                return "UNSOL_CDMA_CALL_WAITING";
            case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS:
                return "UNSOL_CDMA_OTA_PROVISION_STATUS";
            case RIL_UNSOL_CDMA_INFO_REC:
                return "UNSOL_CDMA_INFO_REC";
            case RIL_UNSOL_OEM_HOOK_RAW:
                return "UNSOL_OEM_HOOK_RAW";
            case RIL_UNSOL_RINGBACK_TONE:
                return "UNSOL_RINGBACK_TONE";
            case RIL_UNSOL_RESEND_INCALL_MUTE:
                return "UNSOL_RESEND_INCALL_MUTE";
            case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                return "UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED";
            case RIL_UNSOL_CDMA_PRL_CHANGED:
                return "UNSOL_CDMA_PRL_CHANGED";
            case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE:
                return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_RIL_CONNECTED:
                return "UNSOL_RIL_CONNECTED";
            case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED:
                return "UNSOL_VOICE_RADIO_TECH_CHANGED";
            case RIL_UNSOL_CELL_INFO_LIST:
                return "UNSOL_CELL_INFO_LIST";
            case RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED:
                return "UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED:
                return "UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED";
            case RIL_UNSOL_SRVCC_STATE_NOTIFY:
                return "UNSOL_SRVCC_STATE_NOTIFY";
            case RIL_UNSOL_HARDWARE_CONFIG_CHANGED:
                return "UNSOL_HARDWARE_CONFIG_CHANGED";
            case RIL_UNSOL_DC_RT_INFO_CHANGED:
                return "UNSOL_DC_RT_INFO_CHANGED";
            case RIL_UNSOL_RADIO_CAPABILITY:
                return "UNSOL_RADIO_CAPABILITY";
            case RIL_UNSOL_ON_SS:
                return "UNSOL_ON_SS";
            case RIL_UNSOL_STK_CC_ALPHA_NOTIFY:
                return "UNSOL_STK_CC_ALPHA_NOTIFY";
            case RIL_UNSOL_LCEDATA_RECV:
                return "UNSOL_LCE_INFO_RECV";
            case RIL_UNSOL_PCO_DATA:
                return "UNSOL_PCO_DATA";
            case RIL_UNSOL_MODEM_RESTART:
                return "UNSOL_MODEM_RESTART";
            case RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION:
                return "UNSOL_CARRIER_INFO_IMSI_ENCRYPTION";
            case RIL_UNSOL_NETWORK_SCAN_RESULT:
                return "UNSOL_NETWORK_SCAN_RESULT";
            case RIL_UNSOL_KEEPALIVE_STATUS:
                return "UNSOL_KEEPALIVE_STATUS";
            case RIL_UNSOL_UNTHROTTLE_APN:
                return "UNSOL_UNTHROTTLE_APN";
            case RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED:
                return "UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED";
            case RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED:
                return "UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED";
            case RIL_UNSOL_SLICING_CONFIG_CHANGED:
                return "UNSOL_SLICING_CONFIG_CHANGED";
            case RIL_UNSOL_CELLULAR_IDENTIFIER_DISCLOSED:
                return "UNSOL_CELLULAR_IDENTIFIER_DISCLOSED";
            case RIL_UNSOL_SECURITY_ALGORITHMS_UPDATED:
                return "UNSOL_SECURITY_ALGORITHMS_UPDATED";
            /* The follow unsols are not defined in RIL.h */
            case RIL_UNSOL_ICC_SLOT_STATUS:
                return "UNSOL_ICC_SLOT_STATUS";
            case RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG:
                return "UNSOL_PHYSICAL_CHANNEL_CONFIG";
            case RIL_UNSOL_EMERGENCY_NUMBER_LIST:
                return "UNSOL_EMERGENCY_NUMBER_LIST";
            case RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED:
                return "UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED";
            case RIL_UNSOL_REGISTRATION_FAILED:
                return "UNSOL_REGISTRATION_FAILED";
            case RIL_UNSOL_BARRING_INFO_CHANGED:
                return "UNSOL_BARRING_INFO_CHANGED";
            case RIL_UNSOL_EMERGENCY_NETWORK_SCAN_RESULT:
                return "UNSOL_EMERGENCY_NETWORK_SCAN_RESULT";
            case RIL_UNSOL_CONNECTION_SETUP_FAILURE:
                return "UNSOL_CONNECTION_SETUP_FAILURE";
            case RIL_UNSOL_NOTIFY_ANBR:
                return "UNSOL_NOTIFY_ANBR";
            case RIL_UNSOL_TRIGGER_IMS_DEREGISTRATION:
                return "UNSOL_TRIGGER_IMS_DEREGISTRATION";
            case RIL_UNSOL_IMEI_MAPPING_CHANGED:
                return "UNSOL_IMEI_MAPPING_CHANGED";
            case RIL_UNSOL_SIMULTANEOUS_CALLING_SUPPORT_CHANGED:
                return "UNSOL_SIMULTANEOUS_CALLING_SUPPORT_CHANGED";
            default:
                return "<unknown response " + response + ">";
        }
    }

    /**
     * Create capabilities based off of the radio hal version and feature set configurations.
     * @param radioHalVersion radio hal version
     * @param modemReducedFeatureSet1 reduced feature set
     * @return set of capabilities
     */
    @VisibleForTesting
    public static Set<String> getCaps(HalVersion radioHalVersion, boolean modemReducedFeatureSet1) {
        final Set<String> caps = new HashSet<>();

        if (radioHalVersion.equals(RIL.RADIO_HAL_VERSION_UNKNOWN)) {
            // If the Radio HAL is UNKNOWN, no capabilities will present themselves.
            loge("Radio Hal Version is UNKNOWN!");
        }

        logd("Radio Hal Version = " + radioHalVersion.toString());
        if (radioHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            caps.add(CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
            logd("CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK");

            if (!modemReducedFeatureSet1) {
                caps.add(CAPABILITY_SECONDARY_LINK_BANDWIDTH_VISIBLE);
                logd("CAPABILITY_SECONDARY_LINK_BANDWIDTH_VISIBLE");
                caps.add(CAPABILITY_NR_DUAL_CONNECTIVITY_CONFIGURATION_AVAILABLE);
                logd("CAPABILITY_NR_DUAL_CONNECTIVITY_CONFIGURATION_AVAILABLE");
                caps.add(CAPABILITY_THERMAL_MITIGATION_DATA_THROTTLING);
                logd("CAPABILITY_THERMAL_MITIGATION_DATA_THROTTLING");
                caps.add(CAPABILITY_SLICING_CONFIG_SUPPORTED);
                logd("CAPABILITY_SLICING_CONFIG_SUPPORTED");
                caps.add(CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED);
                logd("CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED");
            } else {
                caps.add(CAPABILITY_SIM_PHONEBOOK_IN_MODEM);
                logd("CAPABILITY_SIM_PHONEBOOK_IN_MODEM");
            }
        }
        return caps;
    }

    private static boolean isPrimitiveOrWrapper(Class c) {
        return c.isPrimitive() || WRAPPER_CLASSES.contains(c);
    }

    /**
     * Return a general String representation of a class
     * @param o The object to convert to String
     * @return A string containing all public non-static local variables of a class
     */
    public static String convertToString(Object o) {
        boolean toStringExists = false;
        try {
            toStringExists = o.getClass().getMethod("toString").getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            loge(e.toString());
        }
        if (toStringExists || isPrimitiveOrWrapper(o.getClass()) || o instanceof ArrayList) {
            return o.toString();
        }
        if (o.getClass().isArray()) {
            // Special handling for arrays
            StringBuilder sb = new StringBuilder("[");
            boolean added = false;
            if (isPrimitiveOrWrapper(o.getClass().getComponentType())) {
                for (int i = 0; i < Array.getLength(o); i++) {
                    sb.append(convertToString(Array.get(o, i))).append(", ");
                    added = true;
                }
            } else {
                for (Object element : (Object[]) o) {
                    sb.append(convertToString(element)).append(", ");
                    added = true;
                }
            }
            if (added) {
                // Remove extra ,
                sb.delete(sb.length() - 2, sb.length());
            }
            sb.append("]");
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder(o.getClass().getSimpleName());
        sb.append("{");
        Field[] fields = o.getClass().getDeclaredFields();
        int tag = -1;
        try {
            tag = (int) o.getClass().getDeclaredMethod("getTag").invoke(o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            loge(e.toString());
        } catch (NoSuchMethodException ignored) {
            // Ignored since only unions have the getTag method
        }
        if (tag != -1) {
            // Special handling for unions
            String tagName = null;
            try {
                Method method = o.getClass().getDeclaredMethod("_tagString", int.class);
                method.setAccessible(true);
                tagName = (String) method.invoke(o, tag);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                loge(e.toString());
            }
            if (tagName != null) {
                sb.append(tagName);
                sb.append("=");
                // From tag, create method name getTag
                String getTagMethod = "get" + tagName.substring(0, 1).toUpperCase(Locale.ROOT)
                        + tagName.substring(1);
                Object val = null;
                try {
                    val = o.getClass().getDeclaredMethod(getTagMethod).invoke(o);
                } catch (NoSuchMethodException | IllegalAccessException
                         | InvocationTargetException e) {
                    loge(e.toString());
                }
                if (val != null) {
                    sb.append(convertToString(val));
                }
            }
        } else {
            boolean added = false;
            for (Field field : fields) {
                // Ignore static variables
                if (Modifier.isStatic(field.getModifiers())) continue;
                sb.append(field.getName()).append("=");
                Object val = null;
                try {
                    val = field.get(o);
                } catch (IllegalAccessException e) {
                    loge(e.toString());
                }
                if (val == null) continue;
                sb.append(convertToString(val)).append(", ");
                added = true;
            }
            if (added) {
                // Remove extra ,
                sb.delete(sb.length() - 2, sb.length());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts the list of call information for Single Radio Voice Call Continuity(SRVCC).
     *
     * @param srvccConnections The list of call information for SRVCC.
     * @return The converted list of call information.
     */
    public static android.hardware.radio.ims.SrvccCall[] convertToHalSrvccCall(
            SrvccConnection[] srvccConnections) {
        if (srvccConnections == null) {
            return new android.hardware.radio.ims.SrvccCall[0];
        }

        int length = srvccConnections.length;
        android.hardware.radio.ims.SrvccCall[] srvccCalls =
                new android.hardware.radio.ims.SrvccCall[length];

        for (int i = 0; i < length; i++) {
            srvccCalls[i] = new android.hardware.radio.ims.SrvccCall();
            srvccCalls[i].index = i + 1;
            srvccCalls[i].callType = convertSrvccCallType(srvccConnections[i].getType());
            srvccCalls[i].callState = convertCallState(srvccConnections[i].getState());
            srvccCalls[i].callSubstate =
                    convertSrvccCallSubState(srvccConnections[i].getSubState());
            srvccCalls[i].ringbackToneType =
                    convertSrvccCallRingbackToneType(srvccConnections[i].getRingbackToneType());
            srvccCalls[i].isMpty = srvccConnections[i].isMultiParty();
            srvccCalls[i].isMT = srvccConnections[i].isIncoming();
            srvccCalls[i].number = TextUtils.emptyIfNull(srvccConnections[i].getNumber());
            srvccCalls[i].numPresentation =
                    convertPresentation(srvccConnections[i].getNumberPresentation());
            srvccCalls[i].name = TextUtils.emptyIfNull(srvccConnections[i].getName());
            srvccCalls[i].namePresentation =
                    convertPresentation(srvccConnections[i].getNamePresentation());
        }

        return srvccCalls;
    }

    /**
     * Converts the call type.
     *
     * @param type The call type.
     * @return The converted call type.
     */
    public static int convertSrvccCallType(int type) {
        switch (type) {
            case  SrvccConnection.CALL_TYPE_NORMAL:
                return android.hardware.radio.ims.SrvccCall.CallType.NORMAL;
            case  SrvccConnection.CALL_TYPE_EMERGENCY:
                return android.hardware.radio.ims.SrvccCall.CallType.EMERGENCY;
            default:
                throw new RuntimeException("illegal call type " + type);
        }
    }

    /**
     * Converts the call state.
     *
     * @param state The call state.
     * @return The converted call state.
     */
    public static int convertCallState(Call.State state) {
        switch (state) {
            case ACTIVE: return android.hardware.radio.voice.Call.STATE_ACTIVE;
            case HOLDING: return android.hardware.radio.voice.Call.STATE_HOLDING;
            case DIALING: return android.hardware.radio.voice.Call.STATE_DIALING;
            case ALERTING: return android.hardware.radio.voice.Call.STATE_ALERTING;
            case INCOMING: return android.hardware.radio.voice.Call.STATE_INCOMING;
            case WAITING: return android.hardware.radio.voice.Call.STATE_WAITING;
            default:
                throw new RuntimeException("illegal state " + state);
        }
    }

    /**
     * Converts the substate of a call.
     *
     * @param state The substate of a call.
     * @return The converted substate.
     */
    public static int convertSrvccCallSubState(int state) {
        switch (state) {
            case SrvccConnection.SUBSTATE_NONE:
                return android.hardware.radio.ims.SrvccCall.CallSubState.NONE;
            case SrvccConnection.SUBSTATE_PREALERTING:
                return android.hardware.radio.ims.SrvccCall.CallSubState.PREALERTING;
            default:
                throw new RuntimeException("illegal substate " + state);
        }
    }

    /**
     * Converts the ringback tone type.
     *
     * @param type The ringback tone type.
     * @return The converted ringback tone type.
     */
    public static int convertSrvccCallRingbackToneType(int type) {
        switch (type) {
            case SrvccConnection.TONE_NONE:
                return android.hardware.radio.ims.SrvccCall.ToneType.NONE;
            case SrvccConnection.TONE_LOCAL:
                return android.hardware.radio.ims.SrvccCall.ToneType.LOCAL;
            case SrvccConnection.TONE_NETWORK:
                return android.hardware.radio.ims.SrvccCall.ToneType.NETWORK;
            default:
                throw new RuntimeException("illegal ringback tone type " + type);
        }
    }

    /**
     * Converts the number presentation type for caller id display.
     *
     * @param presentation The number presentation type.
     * @return The converted presentation type.
     */
    public static int convertPresentation(int presentation) {
        switch (presentation) {
            case PhoneConstants.PRESENTATION_ALLOWED:
                return android.hardware.radio.voice.Call.PRESENTATION_ALLOWED;
            case PhoneConstants.PRESENTATION_RESTRICTED:
                return android.hardware.radio.voice.Call.PRESENTATION_RESTRICTED;
            case PhoneConstants.PRESENTATION_UNKNOWN:
                return android.hardware.radio.voice.Call.PRESENTATION_UNKNOWN;
            case PhoneConstants.PRESENTATION_PAYPHONE:
                return android.hardware.radio.voice.Call.PRESENTATION_PAYPHONE;
            default:
                throw new RuntimeException("illegal presentation " + presentation);
        }
    }

    /**
     * Converts IMS registration state.
     *
     * @param state The IMS registration state.
     * @return The converted HAL IMS registration state.
     */
    public static int convertImsRegistrationState(int state) {
        switch (state) {
            case RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED:
                return android.hardware.radio.ims.ImsRegistrationState.NOT_REGISTERED;
            case RegistrationManager.REGISTRATION_STATE_REGISTERED:
                return android.hardware.radio.ims.ImsRegistrationState.REGISTERED;
            default:
                throw new RuntimeException("illegal state " + state);
        }
    }

    /**
     * Converts IMS service radio technology.
     *
     * @param imsRadioTech The IMS service radio technology.
     * @return The converted HAL access network type.
     */

    public static int convertImsRegistrationTech(
            @ImsRegistrationImplBase.ImsRegistrationTech int imsRadioTech) {
        switch (imsRadioTech) {
            case ImsRegistrationImplBase.REGISTRATION_TECH_LTE:
                return android.hardware.radio.AccessNetwork.EUTRAN;
            case ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN:
                return android.hardware.radio.AccessNetwork.IWLAN;
            case ImsRegistrationImplBase.REGISTRATION_TECH_NR:
                return android.hardware.radio.AccessNetwork.NGRAN;
            case ImsRegistrationImplBase.REGISTRATION_TECH_3G:
                return android.hardware.radio.AccessNetwork.UTRAN;
            default:
                return android.hardware.radio.AccessNetwork.UNKNOWN;
        }
    }

    /**
     * Converts IMS capabilities.
     *
     * @param capabilities The IMS capabilities.
     * @return The converted HAL IMS capabilities.
     */
    public static int convertImsCapability(int capabilities) {
        int halCapabilities = android.hardware.radio.ims.ImsRegistration.IMS_MMTEL_CAPABILITY_NONE;
        if ((capabilities & CommandsInterface.IMS_MMTEL_CAPABILITY_VOICE) > 0) {
            halCapabilities |=
                    android.hardware.radio.ims.ImsRegistration.IMS_MMTEL_CAPABILITY_VOICE;
        }
        if ((capabilities & CommandsInterface.IMS_MMTEL_CAPABILITY_VIDEO) > 0) {
            halCapabilities |=
                    android.hardware.radio.ims.ImsRegistration.IMS_MMTEL_CAPABILITY_VIDEO;
        }
        if ((capabilities & CommandsInterface.IMS_MMTEL_CAPABILITY_SMS) > 0) {
            halCapabilities |= android.hardware.radio.ims.ImsRegistration.IMS_MMTEL_CAPABILITY_SMS;
        }
        if ((capabilities & CommandsInterface.IMS_RCS_CAPABILITIES) > 0) {
            halCapabilities |= android.hardware.radio.ims.ImsRegistration.IMS_RCS_CAPABILITIES;
        }
        return halCapabilities;
    }

    /** Converts the ImsCallInfo instances to HAL ImsCall instances. */
    public static android.hardware.radio.ims.ImsCall[] convertImsCallInfo(
            List<ImsCallInfo> imsCallInfos) {
        if (imsCallInfos == null) {
            return new android.hardware.radio.ims.ImsCall[0];
        }

        int length = 0;
        for (int i = 0; i < imsCallInfos.size(); i++) {
            if (imsCallInfos.get(i) != null) length++;
        }
        if (length == 0) {
            return new android.hardware.radio.ims.ImsCall[0];
        }

        android.hardware.radio.ims.ImsCall[] halInfos =
                new android.hardware.radio.ims.ImsCall[length];

        int index = 0;
        for (int i = 0; i < imsCallInfos.size(); i++) {
            ImsCallInfo info = imsCallInfos.get(i);
            if (info == null) continue;

            halInfos[index] = new android.hardware.radio.ims.ImsCall();
            halInfos[index].index = info.getIndex();
            halInfos[index].callState = convertToHalImsCallState(info.getCallState());
            halInfos[index].callType = info.isEmergencyCall()
                    ? android.hardware.radio.ims.ImsCall.CallType.EMERGENCY
                    : android.hardware.radio.ims.ImsCall.CallType.NORMAL;
            halInfos[index].accessNetwork = convertToHalAccessNetworkAidl(info.getCallRadioTech());
            halInfos[index].direction = info.isIncoming()
                    ? android.hardware.radio.ims.ImsCall.Direction.INCOMING
                    : android.hardware.radio.ims.ImsCall.Direction.OUTGOING;
            halInfos[index].isHeldByRemote = info.isHeldByRemote();
            index++;
        }

        return halInfos;
    }

    /**
     * Converts the call state to HAL IMS call state.
     *
     * @param state The {@link Call.State}.
     * @return The converted {@link android.hardware.radio.ims.ImsCall.CallState}.
     */
    private static int convertToHalImsCallState(Call.State state) {
        switch (state) {
            case ACTIVE: return android.hardware.radio.ims.ImsCall.CallState.ACTIVE;
            case HOLDING: return android.hardware.radio.ims.ImsCall.CallState.HOLDING;
            case DIALING: return android.hardware.radio.ims.ImsCall.CallState.DIALING;
            case ALERTING: return android.hardware.radio.ims.ImsCall.CallState.ALERTING;
            case INCOMING: return android.hardware.radio.ims.ImsCall.CallState.INCOMING;
            case WAITING: return android.hardware.radio.ims.ImsCall.CallState.WAITING;
            case DISCONNECTING: return android.hardware.radio.ims.ImsCall.CallState.DISCONNECTING;
            default: return android.hardware.radio.ims.ImsCall.CallState.DISCONNECTED;
        }
    }

    /** Convert an AIDL-based CellularIdentifierDisclosure to its Java wrapper. */
    public static CellularIdentifierDisclosure convertCellularIdentifierDisclosure(
            android.hardware.radio.network.CellularIdentifierDisclosure identifierDisclsoure) {
        if (identifierDisclsoure == null) {
            return null;
        }

        return new CellularIdentifierDisclosure(
                identifierDisclsoure.protocolMessage,
                identifierDisclsoure.identifier,
                identifierDisclsoure.plmn,
                identifierDisclsoure.isEmergency);
    }

    /** Convert an AIDL-based SecurityAlgorithmUpdate to its Java wrapper. */
    public static SecurityAlgorithmUpdate convertSecurityAlgorithmUpdate(
            android.hardware.radio.network.SecurityAlgorithmUpdate securityAlgorithmUpdate) {
        if (securityAlgorithmUpdate == null) {
            return null;
        }

        return new SecurityAlgorithmUpdate(
                securityAlgorithmUpdate.connectionEvent,
                securityAlgorithmUpdate.encryption,
                securityAlgorithmUpdate.integrity,
                securityAlgorithmUpdate.isUnprotectedEmergency);
    }

    /** Convert an AIDL-based SimTypeInfo to its Java wrapper. */
    public static ArrayList<SimTypeInfo> convertAidlSimTypeInfo(
            android.hardware.radio.config.SimTypeInfo[] simTypeInfos) {
        ArrayList<SimTypeInfo> response = new ArrayList<>();
        if (simTypeInfos == null) {
            loge("convertAidlSimTypeInfo received NULL simTypeInfos");
            return response;
        }
        for (android.hardware.radio.config.SimTypeInfo simTypeInfo : simTypeInfos) {
            SimTypeInfo info = new SimTypeInfo();
            info.mSupportedSimTypes = simTypeInfo.supportedSimTypes;
            info.setCurrentSimType(simTypeInfo.currentSimType);
            response.add(info);
        }
        return response;
    }

    private static void logd(String log) {
        Rlog.d("RILUtils", log);
    }

    private static void loge(String log) {
        Rlog.e("RILUtils", log);
    }
}
