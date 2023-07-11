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

package com.android.internal.telephony.metrics;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CALL_COMPOSER;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHATBOT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHATBOT_ROLE;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHATBOT_STANDALONE;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHAT_V1;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHAT_V2;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CUSTOM;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_FT;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_FT_OVER_SMS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_GEO_PUSH;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_GEO_PUSH_VIA_SMS;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_MMTEL;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_POST_CALL;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_SHARED_MAP;
import static com.android.internal.telephony.TelephonyStatsLog.IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_SHARED_SKETCH;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT__REASON__REASON_CUSTOM;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT__REASON__REASON_DEACTIVATED;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT__REASON__REASON_GIVEUP;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT__REASON__REASON_NORESOURCE;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT__REASON__REASON_PROBATION;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT__REASON__REASON_REJECTED;
import static com.android.internal.telephony.TelephonyStatsLog.PRESENCE_NOTIFY_EVENT__REASON__REASON_TIMEOUT;
import static com.android.internal.telephony.TelephonyStatsLog.RCS_ACS_PROVISIONING_STATS__RESPONSE_TYPE__ERROR;
import static com.android.internal.telephony.TelephonyStatsLog.RCS_ACS_PROVISIONING_STATS__RESPONSE_TYPE__PRE_PROVISIONING_XML;
import static com.android.internal.telephony.TelephonyStatsLog.UCE_EVENT_STATS__TYPE__INCOMING_OPTION;
import static com.android.internal.telephony.TelephonyStatsLog.UCE_EVENT_STATS__TYPE__OUTGOING_OPTION;
import static com.android.internal.telephony.TelephonyStatsLog.UCE_EVENT_STATS__TYPE__PUBLISH;
import static com.android.internal.telephony.TelephonyStatsLog.UCE_EVENT_STATS__TYPE__SUBSCRIBE;

import android.annotation.NonNull;
import android.os.Binder;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyProtoEnums;
import android.telephony.ims.FeatureTagState;
import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.aidl.IRcsConfigCallback;
import android.util.Base64;
import android.util.IndentingPrintWriter;

import com.android.ims.rcs.uce.UceStatsWriter;
import com.android.ims.rcs.uce.UceStatsWriter.UceStatsCallback;
import com.android.ims.rcs.uce.util.FeatureTags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.nano.PersistAtomsProto;
import com.android.internal.telephony.nano.PersistAtomsProto.GbaEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsDedicatedBearerEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsDedicatedBearerListenerEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationFeatureTagStats;
import com.android.internal.telephony.nano.PersistAtomsProto.ImsRegistrationServiceDescStats;
import com.android.internal.telephony.nano.PersistAtomsProto.PresenceNotifyEvent;
import com.android.internal.telephony.nano.PersistAtomsProto.RcsAcsProvisioningStats;
import com.android.internal.telephony.nano.PersistAtomsProto.RcsClientProvisioningStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipDelegateStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipMessageResponse;
import com.android.internal.telephony.nano.PersistAtomsProto.SipTransportFeatureTagStats;
import com.android.internal.telephony.nano.PersistAtomsProto.SipTransportSession;
import com.android.internal.telephony.nano.PersistAtomsProto.UceEventStats;
import com.android.telephony.Rlog;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Tracks RCS provisioning, sip transport, UCE metrics for phone. */
public class RcsStats {
    private static final String TAG = RcsStats.class.getSimpleName();
    private static final long MIN_DURATION_MILLIS = 1L * SECOND_IN_MILLIS;
    private final PersistAtomsStorage mAtomsStorage =
            PhoneFactory.getMetricsCollector().getAtomsStorage();
    private static final Random RANDOM = new Random();

    private UceStatsWriterCallback mCallback;
    private static RcsStats sInstance;

    public static final int NONE = -1;
    public static final int STATE_REGISTERED = 0;
    public static final int STATE_DEREGISTERED = 1;
    public static final int STATE_DENIED = 2;

    private static final String SIP_REQUEST_MESSAGE_TYPE_INVITE = "INVITE";
    private static final String SIP_REQUEST_MESSAGE_TYPE_ACK = "ACK";
    private static final String SIP_REQUEST_MESSAGE_TYPE_OPTIONS = "OPTIONS";
    private static final String SIP_REQUEST_MESSAGE_TYPE_BYE = "BYE";
    private static final String SIP_REQUEST_MESSAGE_TYPE_CANCEL = "CANCEL";
    private static final String SIP_REQUEST_MESSAGE_TYPE_REGISTER = "REGISTER";
    private static final String SIP_REQUEST_MESSAGE_TYPE_PRACK = "PRACK";
    private static final String SIP_REQUEST_MESSAGE_TYPE_SUBSCRIBE = "SUBSCRIBE";
    private static final String SIP_REQUEST_MESSAGE_TYPE_NOTIFY = "NOTIFY";
    private static final String SIP_REQUEST_MESSAGE_TYPE_PUBLISH = "PUBLISH";
    private static final String SIP_REQUEST_MESSAGE_TYPE_INFO = "INFO";
    private static final String SIP_REQUEST_MESSAGE_TYPE_REFER = "REFER";
    private static final String SIP_REQUEST_MESSAGE_TYPE_MESSAGE = "MESSAGE";
    private static final String SIP_REQUEST_MESSAGE_TYPE_UPDATE = "UPDATE";

    /**
     * Describe Feature Tags
     * See frameworks/opt/net/ims/src/java/com/android/ims/rcs/uce/util/FeatureTags.java
     * and int value matching the Feature Tags
     * See stats/enums/telephony/enums.proto
     */
    private static final Map<String, Integer> FEATURE_TAGS = new HashMap<>();

    static {
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_STANDALONE_MSG.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_STANDALONE_MSG);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_CHAT_IM.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CHAT_IM);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_CHAT_SESSION.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CHAT_SESSION);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_FILE_TRANSFER.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_FILE_TRANSFER);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_FILE_TRANSFER_VIA_SMS.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_FILE_TRANSFER_VIA_SMS);
        FEATURE_TAGS.put(
                FeatureTags.FEATURE_TAG_CALL_COMPOSER_ENRICHED_CALLING.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CALL_COMPOSER_ENRICHED_CALLING);
        FEATURE_TAGS.put(
                FeatureTags.FEATURE_TAG_CALL_COMPOSER_VIA_TELEPHONY.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CALL_COMPOSER_VIA_TELEPHONY);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_POST_CALL.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_POST_CALL);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_SHARED_MAP.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_SHARED_MAP);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_SHARED_SKETCH.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_SHARED_SKETCH);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_GEO_PUSH.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_GEO_PUSH);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_GEO_PUSH_VIA_SMS.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_GEO_PUSH_VIA_SMS);
        FEATURE_TAGS.put(
                FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_SESSION.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CHATBOT_COMMUNICATION_USING_SESSION);
        String FeatureTag = FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_STANDALONE_MSG;
        FEATURE_TAGS.put(FeatureTag.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CHATBOT_COMMUNICATION_USING_STANDALONE_MSG);
        FEATURE_TAGS.put(
                FeatureTags.FEATURE_TAG_CHATBOT_VERSION_SUPPORTED.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CHATBOT_VERSION_SUPPORTED);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_CHATBOT_ROLE.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CHATBOT_ROLE);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_MMTEL.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_MMTEL);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_VIDEO.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_VIDEO);
        FEATURE_TAGS.put(FeatureTags.FEATURE_TAG_PRESENCE.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_PRESENCE);
    }

    /**
     * Describe Service IDs
     * See frameworks/base/telephony/java/android/telephony/ims/RcsContactPresenceTuple.java
     * and int value matching the service IDs
     * See frameworks/proto_logging/stats/atoms.proto
     */
    private static final Map<String, Integer> SERVICE_IDS = new HashMap<>();

    static {
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_MMTEL.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_MMTEL);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_CHAT_V1.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHAT_V1);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_CHAT_V2.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHAT_V2);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_FT.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_FT);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_FT_OVER_SMS.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_FT_OVER_SMS);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_GEO_PUSH.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_GEO_PUSH);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_GEO_PUSH_VIA_SMS.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_GEO_PUSH_VIA_SMS);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_CALL_COMPOSER.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CALL_COMPOSER);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_POST_CALL.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_POST_CALL);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_SHARED_MAP.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_SHARED_MAP);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_SHARED_SKETCH.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_SHARED_SKETCH);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_CHATBOT.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHATBOT);
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_CHATBOT_STANDALONE.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHATBOT_STANDALONE
        );
        SERVICE_IDS.put(RcsContactPresenceTuple.SERVICE_ID_CHATBOT_ROLE.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CHATBOT_ROLE);
    }

    /**
     * Describe Message Method Type
     * See stats/enums/telephony/enums.proto
     */
    private static final Map<String, Integer> MESSAGE_TYPE = new HashMap<>();

    static {
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_INVITE.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_INVITE);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_ACK.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_ACK);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_OPTIONS.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_OPTIONS);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_BYE.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_BYE);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_CANCEL.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_CANCEL);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_REGISTER.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_REGISTER);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_PRACK.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_PRACK);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_SUBSCRIBE.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_SUBSCRIBE);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_NOTIFY.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_NOTIFY);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_PUBLISH.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_PUBLISH);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_INFO.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_INFO);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_REFER.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_REFER);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_MESSAGE.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_MESSAGE);
        MESSAGE_TYPE.put(SIP_REQUEST_MESSAGE_TYPE_UPDATE.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_UPDATE);
    }

    /**
     * Describe Reasons
     * See frameworks/opt/net/ims/src/java/com/android/ims/rcs/uce/request/
     * SubscriptionTerminatedHelper.java
     * and int value matching the Reasons
     * See frameworks/proto_logging/stats/atoms.proto
     */
    private static final Map<String, Integer> NOTIFY_REASONS = new HashMap<>();

    static {
        NOTIFY_REASONS.put("deactivated", PRESENCE_NOTIFY_EVENT__REASON__REASON_DEACTIVATED);
        NOTIFY_REASONS.put("probation", PRESENCE_NOTIFY_EVENT__REASON__REASON_PROBATION);
        NOTIFY_REASONS.put("rejected", PRESENCE_NOTIFY_EVENT__REASON__REASON_REJECTED);
        NOTIFY_REASONS.put("timeout", PRESENCE_NOTIFY_EVENT__REASON__REASON_TIMEOUT);
        NOTIFY_REASONS.put("giveup", PRESENCE_NOTIFY_EVENT__REASON__REASON_GIVEUP);
        NOTIFY_REASONS.put("noresource", PRESENCE_NOTIFY_EVENT__REASON__REASON_NORESOURCE);
    }

    /**
     * Describe Rcs Capability set
     * See frameworks/base/telephony/java/android/telephony/ims/RcsContactPresenceTuple.java
     */
    private static final HashSet<String> RCS_SERVICE_ID_SET = new HashSet<>();
    static {
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_CHAT_V1);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_CHAT_V2);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_FT);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_FT_OVER_SMS);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_GEO_PUSH);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_GEO_PUSH_VIA_SMS);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_SHARED_MAP);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_SHARED_SKETCH);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_CHATBOT);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_CHATBOT_STANDALONE);
        RCS_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_CHATBOT_ROLE);
    }

    /**
     * Describe Mmtel Capability set
     * See frameworks/base/telephony/java/android/telephony/ims/RcsContactPresenceTuple.java
     */
    private static final HashSet<String> MMTEL_SERVICE_ID_SET = new HashSet<>();
    static {
        MMTEL_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_MMTEL);
        MMTEL_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_CALL_COMPOSER);
        MMTEL_SERVICE_ID_SET.add(RcsContactPresenceTuple.SERVICE_ID_POST_CALL);
    }

    private static final Map<Long, Integer> sSubscribeTaskIds = new HashMap<>();
    private static final int SUBSCRIBE_SUCCESS = 1;
    private static final int SUBSCRIBE_NOTIFY = 2;

    @VisibleForTesting
    protected final Map<Integer, ImsDedicatedBearerListenerEvent> mDedicatedBearerListenerEventMap =
            new HashMap<>();
    @VisibleForTesting
    protected final List<RcsAcsProvisioningStats> mRcsAcsProvisioningStatsList =
            new ArrayList<RcsAcsProvisioningStats>();
    @VisibleForTesting
    protected final HashMap<Integer, RcsProvisioningCallback> mRcsProvisioningCallbackMap =
            new HashMap<>();

    // Maps feature tag name -> ImsRegistrationFeatureTagStats.
    private final List<ImsRegistrationFeatureTagStats> mImsRegistrationFeatureTagStatsList =
            new ArrayList<>();

    // Maps service id -> ImsRegistrationServiceDescStats.
    @VisibleForTesting
    protected final List<ImsRegistrationServiceDescStats> mImsRegistrationServiceDescStatsList =
            new ArrayList<>();

    private List<LastSipDelegateStat> mLastSipDelegateStatList = new ArrayList<>();
    private HashMap<Integer, SipTransportFeatureTags> mLastFeatureTagStatMap = new HashMap<>();
    private ArrayList<SipMessageArray> mSipMessageArray = new ArrayList<>();
    private ArrayList<SipTransportSessionArray> mSipTransportSessionArray = new ArrayList<>();
    private SipTransportSessionArray mSipTransportSession;
    private SipMessageArray mSipMessage;

    private class LastSipDelegateStat {
        public int mSubId;
        public SipDelegateStats mLastStat;
        private Set<String> mSupportedTags;

        LastSipDelegateStat(int subId, Set<String> supportedTags) {
            mSubId = subId;
            mSupportedTags = supportedTags;
        }

        public void createSipDelegateStat(int subId) {
            mLastStat = getDefaultSipDelegateStat(subId);
            mLastStat.uptimeMillis = getWallTimeMillis();
            mLastStat.destroyReason = NONE;
        }

        public void setSipDelegateDestroyReason(int destroyReason) {
            mLastStat.destroyReason = destroyReason;
        }

        public boolean isDestroyed() {
            return mLastStat.destroyReason > NONE;
        }

        public void conclude(long now) {
            long duration = now - mLastStat.uptimeMillis;
            if (duration < MIN_DURATION_MILLIS) {
                logd("concludeSipDelegateStat: discarding transient stats,"
                        + " duration= " + duration);
            } else {
                mLastStat.uptimeMillis = duration;
                mAtomsStorage.addSipDelegateStats(copyOf(mLastStat));
            }
            mLastStat.uptimeMillis = now;
        }

        public boolean compare(int subId, Set<String> supportedTags) {
            if (subId != mSubId || supportedTags == null || supportedTags.isEmpty()) {
                return false;
            }
            for (String tag : supportedTags) {
                if (!mSupportedTags.contains(tag)) {
                    return false;
                }
            }
            return true;
        }

        private SipDelegateStats getDefaultSipDelegateStat(int subId) {
            SipDelegateStats stat = new SipDelegateStats();
            stat.dimension = RANDOM.nextInt();
            stat.carrierId = getCarrierId(subId);
            stat.slotId = getSlotId(subId);
            return stat;
        }
    }

    private static SipDelegateStats copyOf(@NonNull SipDelegateStats source) {
        SipDelegateStats newStat = new SipDelegateStats();

        newStat.dimension = source.dimension;
        newStat.slotId = source.slotId;
        newStat.carrierId = source.carrierId;
        newStat.destroyReason = source.destroyReason;
        newStat.uptimeMillis = source.uptimeMillis;

        return newStat;
    }

    private class SipTransportFeatureTags {
        private HashMap<String, LastFeatureTagState> mFeatureTagMap;
        private int mSubId;

        private class LastFeatureTagState {
            public long timeStamp;
            public int carrierId;
            public int slotId;
            public int state;
            public int reason;

            LastFeatureTagState(int carrierId, int slotId, int state, int reason, long timeStamp) {
                this.carrierId = carrierId;
                this.slotId = slotId;
                this.state = state;
                this.reason = reason;
                this.timeStamp = timeStamp;
            }

            public void update(int state, int reason, long timeStamp) {
                this.state = state;
                this.reason = reason;
                this.timeStamp = timeStamp;
            }

            public void update(long timeStamp) {
                this.timeStamp = timeStamp;
            }
        }

        SipTransportFeatureTags(int subId) {
            mFeatureTagMap = new HashMap<>();
            mSubId = subId;
        }

        public HashMap<String, LastFeatureTagState> getLastTagStates() {
            return mFeatureTagMap;
        }

        /*** Create or update featureTags whenever feature Tag states are changed */
        public synchronized void updateLastFeatureTagState(String tagName, int state, int reason,
                long timeStamp) {
            int carrierId = getCarrierId(mSubId);
            int slotId = getSlotId(mSubId);
            if (mFeatureTagMap.containsKey(tagName)) {
                LastFeatureTagState lastFeatureTagState = mFeatureTagMap.get(tagName);
                if (lastFeatureTagState != null) {
                    addFeatureTagStat(tagName, lastFeatureTagState, timeStamp);
                    lastFeatureTagState.update(state, reason, timeStamp);
                } else {
                    create(tagName, carrierId, slotId, state, reason, timeStamp);
                }

            } else {
                create(tagName, carrierId, slotId, state, reason, timeStamp);
            }
        }

        /** Update current featureTags associated to active SipDelegates when metrics is pulled */
        public synchronized void conclude(long timeStamp) {
            HashMap<String, LastFeatureTagState> featureTagsCopy = new HashMap<>();
            featureTagsCopy.putAll(mFeatureTagMap);
            for (Map.Entry<String, LastFeatureTagState> last : featureTagsCopy.entrySet()) {
                String tagName = last.getKey();
                LastFeatureTagState lastFeatureTagState = last.getValue();
                addFeatureTagStat(tagName, lastFeatureTagState, timeStamp);
                updateTimeStamp(mSubId, tagName, timeStamp);
            }
        }

        /** Finalizes the durations of the current featureTags associated to active SipDelegates */
        private synchronized boolean addFeatureTagStat(@NonNull String tagName,
                @NonNull LastFeatureTagState lastFeatureTagState, long now) {
            long duration = now - lastFeatureTagState.timeStamp;
            if (duration < MIN_DURATION_MILLIS
                    || !isValidCarrierId(lastFeatureTagState.carrierId)) {
                logd("conclude: discarding transient stats, duration= " + duration
                        + ", carrierId = " + lastFeatureTagState.carrierId);
            } else {
                SipTransportFeatureTagStats sipFeatureTagStat = new SipTransportFeatureTagStats();
                switch (lastFeatureTagState.state) {
                    case STATE_DENIED:
                        sipFeatureTagStat.sipTransportDeniedReason = lastFeatureTagState.reason;
                        sipFeatureTagStat.sipTransportDeregisteredReason = NONE;
                        break;
                    case STATE_DEREGISTERED:
                        sipFeatureTagStat.sipTransportDeniedReason = NONE;
                        sipFeatureTagStat.sipTransportDeregisteredReason =
                                lastFeatureTagState.reason;
                        break;
                    default:
                        sipFeatureTagStat.sipTransportDeniedReason = NONE;
                        sipFeatureTagStat.sipTransportDeregisteredReason = NONE;
                        break;
                }

                sipFeatureTagStat.carrierId = lastFeatureTagState.carrierId;
                sipFeatureTagStat.slotId = lastFeatureTagState.slotId;
                sipFeatureTagStat.associatedMillis = duration;
                sipFeatureTagStat.featureTagName = convertTagNameToValue(tagName);
                mAtomsStorage.addSipTransportFeatureTagStats(sipFeatureTagStat);
                return true;
            }
            return false;
        }

        private void updateTimeStamp(int subId, String tagName, long timeStamp) {
            SipTransportFeatureTags sipTransportFeatureTags = mLastFeatureTagStatMap.get(subId);
            if (sipTransportFeatureTags != null) {
                HashMap<String, LastFeatureTagState> lastTagStates =
                        sipTransportFeatureTags.getLastTagStates();
                if (lastTagStates != null && lastTagStates.containsKey(tagName)) {
                    LastFeatureTagState lastFeatureTagState = lastTagStates.get(tagName);
                    if (lastFeatureTagState != null) {
                        lastFeatureTagState.update(timeStamp);
                    }
                }
            }
        }

        private LastFeatureTagState create(String tagName, int carrierId, int slotId, int state,
                int reason, long timeStamp) {
            LastFeatureTagState lastFeatureTagState = new LastFeatureTagState(carrierId, slotId,
                    state, reason, timeStamp);
            mFeatureTagMap.put(tagName, lastFeatureTagState);
            return lastFeatureTagState;
        }
    }

    class UceStatsWriterCallback implements UceStatsCallback {
        private RcsStats mRcsStats;

        UceStatsWriterCallback(RcsStats rcsStats) {
            logd("created Callback");
            mRcsStats = rcsStats;
        }

        public void onImsRegistrationFeatureTagStats(int subId, List<String> featureTagList,
                int registrationTech) {
            mRcsStats.onImsRegistrationFeatureTagStats(subId, featureTagList, registrationTech);
        }

        public void onStoreCompleteImsRegistrationFeatureTagStats(int subId) {
            mRcsStats.onStoreCompleteImsRegistrationFeatureTagStats(subId);
        }

        public void onImsRegistrationServiceDescStats(int subId, List<String> serviceIdList,
                List<String> serviceIdVersionList, int registrationTech) {
            mRcsStats.onImsRegistrationServiceDescStats(subId, serviceIdList, serviceIdVersionList,
                    registrationTech);
        }

        public void onSubscribeResponse(int subId, long taskId, int networkResponse) {
            if (networkResponse >= 200 && networkResponse <= 299) {
                if (!sSubscribeTaskIds.containsKey(taskId)) {
                    sSubscribeTaskIds.put(taskId, SUBSCRIBE_SUCCESS);
                }
            }
            mRcsStats.onUceEventStats(subId, UCE_EVENT_STATS__TYPE__SUBSCRIBE,
                    true, 0, networkResponse);
        }

        public void onUceEvent(int subId, int type, boolean successful, int commandCode,
                int networkResponse) {
            int eventType = 0;
            switch (type) {
                case UceStatsWriter.PUBLISH_EVENT:
                    eventType = UCE_EVENT_STATS__TYPE__PUBLISH;
                    break;
                case UceStatsWriter.SUBSCRIBE_EVENT:
                    eventType = UCE_EVENT_STATS__TYPE__SUBSCRIBE;
                    break;
                case UceStatsWriter.INCOMING_OPTION_EVENT:
                    eventType = UCE_EVENT_STATS__TYPE__INCOMING_OPTION;
                    break;
                case UceStatsWriter.OUTGOING_OPTION_EVENT:
                    eventType = UCE_EVENT_STATS__TYPE__OUTGOING_OPTION;
                    break;
                default:
                    return;
            }
            mRcsStats.onUceEventStats(subId, eventType, successful, commandCode, networkResponse);
        }

        public void onSubscribeTerminated(int subId, long taskId, String reason) {
            if (sSubscribeTaskIds.containsKey(taskId)) {
                int previousSubscribeStatus = sSubscribeTaskIds.get(taskId);
                sSubscribeTaskIds.remove(taskId);
                // The device received a success response related to the subscription request.
                // However, PIDF was not received due to reason value.
                if (previousSubscribeStatus == SUBSCRIBE_SUCCESS) {
                    mRcsStats.onPresenceNotifyEvent(subId, reason, false,
                            false, false, false);
                }
            }
        }

        public void onPresenceNotifyEvent(int subId, long taskId,
                List<RcsContactUceCapability> updatedCapList) {
            if (updatedCapList == null || updatedCapList.isEmpty()) {
                return;
            }
            if (sSubscribeTaskIds.containsKey(taskId)) {
                sSubscribeTaskIds.replace(taskId, SUBSCRIBE_NOTIFY);
            }
            for (RcsContactUceCapability capability : updatedCapList) {
                boolean rcsCap = false;
                boolean mmtelCap = false;
                boolean noCap = true;
                List<RcsContactPresenceTuple> tupleList = capability.getCapabilityTuples();
                if (tupleList.isEmpty()) {
                    noCap = true;
                    mRcsStats.onPresenceNotifyEvent(subId, "", true,
                            rcsCap, mmtelCap, noCap);
                    continue;
                }
                for (RcsContactPresenceTuple tuple : tupleList) {
                    String serviceId = tuple.getServiceId();
                    if (RCS_SERVICE_ID_SET.contains(serviceId)) {
                        rcsCap = true;
                        noCap = false;
                    } else if (MMTEL_SERVICE_ID_SET.contains(serviceId)) {
                        if (serviceId.equals(RcsContactPresenceTuple.SERVICE_ID_CALL_COMPOSER)) {
                            if ("1.0".equals(tuple.getServiceVersion())) {
                                rcsCap = true;
                                noCap = false;
                                continue;
                            }
                        }
                        mmtelCap = true;
                        noCap = false;
                    }
                }
                mRcsStats.onPresenceNotifyEvent(subId, "", true, rcsCap,
                        mmtelCap, noCap);
            }
        }

        public void onStoreCompleteImsRegistrationServiceDescStats(int subId) {
            mRcsStats.onStoreCompleteImsRegistrationServiceDescStats(subId);
        }
    }

    /** Callback class to receive RCS ACS result and to store metrics. */
    public class RcsProvisioningCallback extends IRcsConfigCallback.Stub {
        private RcsStats mRcsStats;
        private int mSubId;
        private boolean mEnableSingleRegistration;
        private boolean mRegistered;

        RcsProvisioningCallback(RcsStats rcsStats, int subId, boolean enableSingleRegistration) {
            logd("created RcsProvisioningCallback");
            mRcsStats = rcsStats;
            mSubId = subId;
            mEnableSingleRegistration = enableSingleRegistration;
            mRegistered = false;
        }

        public synchronized void setEnableSingleRegistration(boolean enableSingleRegistration) {
            mEnableSingleRegistration = enableSingleRegistration;
        }

        public boolean getRegistered() {
            return mRegistered;
        }

        public void setRegistered(boolean registered) {
            mRegistered = registered;
        }

        @Override
        public void onConfigurationChanged(byte[] config) {
            // this callback will not be handled.
        }

        @Override
        public void onAutoConfigurationErrorReceived(int errorCode, String errorString) {
            final long callingIdentity = Binder.clearCallingIdentity();
            try {
                mRcsStats.onRcsAcsProvisioningStats(mSubId, errorCode,
                        RCS_ACS_PROVISIONING_STATS__RESPONSE_TYPE__ERROR,
                        mEnableSingleRegistration);
            } finally {
                restoreCallingIdentity(callingIdentity);
            }
        }

        @Override
        public void onConfigurationReset() {
            // this callback will not be handled.
        }

        @Override
        public void onRemoved() {
            final long callingIdentity = Binder.clearCallingIdentity();
            try {
                // store cached metrics
                mRcsStats.onStoreCompleteRcsAcsProvisioningStats(mSubId);
               // remove this obj from Map
                mRcsStats.removeRcsProvisioningCallback(mSubId);
            } finally {
                restoreCallingIdentity(callingIdentity);
            }
        }

        @Override
        public void onPreProvisioningReceived(byte[] config) {
            final long callingIdentity = Binder.clearCallingIdentity();
            try {
                // Receiving pre provisioning means http 200 OK with body.
                mRcsStats.onRcsAcsProvisioningStats(mSubId, 200,
                        RCS_ACS_PROVISIONING_STATS__RESPONSE_TYPE__PRE_PROVISIONING_XML,
                        mEnableSingleRegistration);
            } finally {
                restoreCallingIdentity(callingIdentity);
            }
        }
    };

    private class SipMessageArray {
        private String mMethod;
        private String mCallId;
        private int mDirection;

        SipMessageArray(String method, int direction, String callId) {
            this.mMethod = method;
            this.mCallId = callId;
            this.mDirection = direction;
        }

        private synchronized void addSipMessageStat(
                @NonNull int subId, @NonNull String sipMessageMethod,
                int sipMessageResponse, int sipMessageDirection, int messageError) {
            int carrierId = getCarrierId(subId);
            if (!isValidCarrierId(carrierId)) {
                return;
            }
            SipMessageResponse proto = new SipMessageResponse();
            proto.carrierId = carrierId;
            proto.slotId = getSlotId(subId);
            proto.sipMessageMethod = convertMessageTypeToValue(sipMessageMethod);
            proto.sipMessageResponse = sipMessageResponse;
            proto.sipMessageDirection = sipMessageDirection;
            proto.messageError = messageError;
            proto.count = 1;
            mAtomsStorage.addSipMessageResponse(proto);
        }
    }

    private class SipTransportSessionArray {
        private String mMethod;
        private String mCallId;
        private int mDirection;
        private int mSipResponse;

        SipTransportSessionArray(String method, int direction, String callId) {
            this.mMethod = method;
            this.mCallId = callId;
            this.mDirection = direction;
            this.mSipResponse = 0;
        }

        private synchronized void addSipTransportSessionStat(
                @NonNull int subId, @NonNull String sessionMethod, int sipMessageDirection,
                int sipResponse, boolean isEndedGracefully) {
            int carrierId = getCarrierId(subId);
            if (!isValidCarrierId(carrierId)) {
                return;
            }
            SipTransportSession proto = new SipTransportSession();
            proto.carrierId = carrierId;
            proto.slotId = getSlotId(subId);
            proto.sessionMethod = convertMessageTypeToValue(sessionMethod);
            proto.sipMessageDirection = sipMessageDirection;
            proto.sipResponse = sipResponse;
            proto.sessionCount = 1;
            proto.endedGracefullyCount = 1;
            proto.isEndedGracefully = isEndedGracefully;
            mAtomsStorage.addCompleteSipTransportSession(proto);
        }
    }

    @VisibleForTesting
    protected RcsStats() {
        mCallback = null;
    }

    /** Gets a RcsStats instance. */
    public static RcsStats getInstance() {
        synchronized (RcsStats.class) {
            if (sInstance == null) {
                Rlog.d(TAG, "RcsStats created.");
                sInstance = new RcsStats();
            }
            return sInstance;
        }
    }

    /** register callback to UceStatsWriter. */
    public void registerUceCallback() {
        if (mCallback == null) {
            mCallback = new UceStatsWriterCallback(sInstance);
            Rlog.d(TAG, "UceStatsWriterCallback created.");
            UceStatsWriter.init(mCallback);
        }
    }

    /** Update or create new atom when RCS service registered. */
    public void onImsRegistrationFeatureTagStats(int subId, List<String> featureTagList,
            int registrationTech) {
        synchronized (mImsRegistrationFeatureTagStatsList) {
            int carrierId = getCarrierId(subId);
            if (!isValidCarrierId(carrierId)) {
                flushImsRegistrationFeatureTagStatsInvalid();
                return;
            }

            // update cached atom if exists
            onStoreCompleteImsRegistrationFeatureTagStats(subId);

            if (featureTagList == null) {
                Rlog.d(TAG, "featureTagNames is null or empty");
                return;
            }

            for (String featureTag : featureTagList) {
                ImsRegistrationFeatureTagStats proto = new ImsRegistrationFeatureTagStats();
                proto.carrierId = carrierId;
                proto.slotId = getSlotId(subId);
                proto.featureTagName = convertTagNameToValue(featureTag);
                proto.registrationTech = registrationTech;
                proto.registeredMillis = getWallTimeMillis();
                mImsRegistrationFeatureTagStatsList.add(proto);
            }
        }
    }

    /** Update duration, store and delete cached ImsRegistrationFeatureTagStats list to storage. */
    public void onStoreCompleteImsRegistrationFeatureTagStats(int subId) {
        synchronized (mImsRegistrationFeatureTagStatsList) {
            int carrierId = getCarrierId(subId);
            List<ImsRegistrationFeatureTagStats> deleteList = new ArrayList<>();
            long now = getWallTimeMillis();
            for (ImsRegistrationFeatureTagStats proto : mImsRegistrationFeatureTagStatsList) {
                if (proto.carrierId == carrierId) {
                    proto.registeredMillis = now - proto.registeredMillis;
                    mAtomsStorage.addImsRegistrationFeatureTagStats(proto);
                    deleteList.add(proto);
                }
            }
            for (ImsRegistrationFeatureTagStats proto : deleteList) {
                mImsRegistrationFeatureTagStatsList.remove(proto);
            }
        }
    }

    /** Update duration and store cached ImsRegistrationFeatureTagStats when metrics are pulled */
    public void onFlushIncompleteImsRegistrationFeatureTagStats() {
        synchronized (mImsRegistrationFeatureTagStatsList) {
            long now = getWallTimeMillis();
            for (ImsRegistrationFeatureTagStats proto : mImsRegistrationFeatureTagStatsList) {
                ImsRegistrationFeatureTagStats newProto = copyImsRegistrationFeatureTagStats(proto);
                // the current time is a placeholder and total registered time will be
                // calculated when generating final atoms
                newProto.registeredMillis = now - proto.registeredMillis;
                mAtomsStorage.addImsRegistrationFeatureTagStats(newProto);
                proto.registeredMillis = now;
            }
        }
    }

    /** Create a new atom when RCS client stat changed. */
    public synchronized void onRcsClientProvisioningStats(int subId, int event) {
        int carrierId = getCarrierId(subId);

        if (!isValidCarrierId(carrierId)) {
            return;
        }

        RcsClientProvisioningStats proto = new RcsClientProvisioningStats();
        proto.carrierId = carrierId;
        proto.slotId = getSlotId(subId);
        proto.event = event;
        proto.count = 1;
        mAtomsStorage.addRcsClientProvisioningStats(proto);
    }

    /** Update or create new atom when RCS ACS stat changed. */
    public void onRcsAcsProvisioningStats(int subId, int responseCode, int responseType,
            boolean enableSingleRegistration) {

        synchronized (mRcsAcsProvisioningStatsList) {
            int carrierId = getCarrierId(subId);
            if (!isValidCarrierId(carrierId)) {
                flushRcsAcsProvisioningStatsInvalid();
                return;
            }

            // update cached atom if exists
            onStoreCompleteRcsAcsProvisioningStats(subId);

            // create new stats to cache
            RcsAcsProvisioningStats newStats = new RcsAcsProvisioningStats();
            newStats.carrierId = carrierId;
            newStats.slotId = getSlotId(subId);
            newStats.responseCode = responseCode;
            newStats.responseType = responseType;
            newStats.isSingleRegistrationEnabled = enableSingleRegistration;
            newStats.count = 1;
            newStats.stateTimerMillis = getWallTimeMillis();

            // add new stats in list
            mRcsAcsProvisioningStatsList.add(newStats);
        }
    }

    /** Update duration, store and delete cached RcsAcsProvisioningStats */
    public void onStoreCompleteRcsAcsProvisioningStats(int subId) {
        synchronized (mRcsAcsProvisioningStatsList) {
            // find cached RcsAcsProvisioningStats based sub ID
            RcsAcsProvisioningStats existingStats = getRcsAcsProvisioningStats(subId);
            if (existingStats != null) {
                existingStats.stateTimerMillis =
                        getWallTimeMillis() - existingStats.stateTimerMillis;
                mAtomsStorage.addRcsAcsProvisioningStats(existingStats);
                // remove cached atom from list
                mRcsAcsProvisioningStatsList.remove(existingStats);
            }
        }
    }

    /** Update duration and store cached RcsAcsProvisioningStats when metrics are pulled */
    public void onFlushIncompleteRcsAcsProvisioningStats() {
        synchronized (mRcsAcsProvisioningStatsList) {
            long now = getWallTimeMillis();
            for (RcsAcsProvisioningStats stats : mRcsAcsProvisioningStatsList) {
                // we store a copy into atoms storage
                // so that we can continue using the original object.
                RcsAcsProvisioningStats proto = copyRcsAcsProvisioningStats(stats);
                // the current time is a placeholder and total registered time will be
                // calculated when generating final atoms
                proto.stateTimerMillis = now - proto.stateTimerMillis;
                mAtomsStorage.addRcsAcsProvisioningStats(proto);
                // update cached atom's time
                stats.stateTimerMillis = now;
            }
        }
    }

    /** Create SipDelegateStat when SipDelegate is created */
    public synchronized void createSipDelegateStats(int subId, Set<String> supportedTags) {
        if (supportedTags != null && !supportedTags.isEmpty()) {
            LastSipDelegateStat lastState = getLastSipDelegateStat(subId, supportedTags);
            lastState.createSipDelegateStat(subId);
        }
    }

    /** Update destroyReason and duration of SipDelegateStat when SipDelegate is destroyed */
    public synchronized void onSipDelegateStats(int subId, Set<String> supportedTags,
            int destroyReason) {
        if (supportedTags != null && !supportedTags.isEmpty()) {
            LastSipDelegateStat lastState = getLastSipDelegateStat(subId, supportedTags);
            lastState.setSipDelegateDestroyReason(destroyReason);
            concludeSipDelegateStat();
        }
    }

    /** Create/Update atoms when states of sipTransportFeatureTags are changed */
    public synchronized void onSipTransportFeatureTagStats(
            int subId,
            Set<FeatureTagState> deniedTags,
            Set<FeatureTagState> deRegiTags,
            Set<String> regiTags) {
        long now = getWallTimeMillis();
        SipTransportFeatureTags sipTransportFeatureTags = getLastFeatureTags(subId);
        if (regiTags != null && !regiTags.isEmpty()) {
            for (String tag : regiTags) {
                sipTransportFeatureTags.updateLastFeatureTagState(tag, STATE_REGISTERED,
                        NONE, now);
            }
        }
        if (deniedTags != null && !deniedTags.isEmpty()) {
            for (FeatureTagState tag : deniedTags) {
                sipTransportFeatureTags.updateLastFeatureTagState(tag.getFeatureTag(), STATE_DENIED,
                        tag.getState(), now);
            }
        }
        if (deRegiTags != null && !deRegiTags.isEmpty()) {
            for (FeatureTagState tag : deRegiTags) {
                sipTransportFeatureTags.updateLastFeatureTagState(
                        tag.getFeatureTag(), STATE_DEREGISTERED, tag.getState(), now);
            }
        }
    }

    /** Update duration of  sipTransportFeatureTags when metrics are pulled */
    public synchronized void concludeSipTransportFeatureTagsStat() {
        if (mLastFeatureTagStatMap.isEmpty()) {
            return;
        }

        long now = getWallTimeMillis();
        HashMap<Integer, SipTransportFeatureTags> lastFeatureTagStatsCopy = new HashMap<>();
        lastFeatureTagStatsCopy.putAll(mLastFeatureTagStatMap);
        for (SipTransportFeatureTags sipTransportFeatureTags : lastFeatureTagStatsCopy.values()) {
            if (sipTransportFeatureTags != null) {
                sipTransportFeatureTags.conclude(now);
            }
        }
    }

    /** Request Message */
    public synchronized void onSipMessageRequest(String callId, String sipMessageMethod,
            int sipMessageDirection) {
        mSipMessage = new SipMessageArray(sipMessageMethod, sipMessageDirection, callId);
        mSipMessageArray.add(mSipMessage);
    }

    /** invalidated result when Request message is sent */
    public synchronized void invalidatedMessageResult(int subId, String sipMessageMethod,
            int sipMessageDirection, int messageError) {
        mSipMessage.addSipMessageStat(subId, sipMessageMethod, 0,
                sipMessageDirection, messageError);
    }

    /** Create a new atom when RCS SIP Message Response changed. */
    public synchronized void onSipMessageResponse(int subId, String callId,
            int sipMessageResponse, int messageError) {
        SipMessageArray match = mSipMessageArray.stream()
                .filter(d -> d.mCallId.equals(callId)).findFirst().orElse(null);
        if (match != null) {
            mSipMessage.addSipMessageStat(subId, match.mMethod, sipMessageResponse,
                    match.mDirection, messageError);
            mSipMessageArray.removeIf(d -> d.mCallId.equals(callId));
        }
    }

    /** Request SIP Method Message */
    public synchronized void earlySipTransportSession(String sessionMethod, String callId,
            int sipMessageDirection) {
        mSipTransportSession = new SipTransportSessionArray(sessionMethod,
                sipMessageDirection, callId);
        mSipTransportSessionArray.add(mSipTransportSession);
    }

    /** Response Message */
    public synchronized void confirmedSipTransportSession(String callId,
            int sipResponse) {
        SipTransportSessionArray match = mSipTransportSessionArray.stream()
                .filter(d -> d.mCallId.equals(callId)).findFirst().orElse(null);
        if (match != null) {
            match.mSipResponse = sipResponse;
        }
    }

    /** Create a new atom when RCS SIP Transport Session changed. */
    public synchronized void onSipTransportSessionClosed(int subId, String callId,
            int sipResponse, boolean isEndedGracefully) {
        SipTransportSessionArray match = mSipTransportSessionArray.stream()
                .filter(d -> d.mCallId.equals(callId)).findFirst().orElse(null);
        if (match != null) {
            if (sipResponse != 0) {
                match.mSipResponse = sipResponse;
            }
            mSipTransportSession.addSipTransportSessionStat(subId, match.mMethod, match.mDirection,
                    sipResponse, isEndedGracefully);
            mSipTransportSessionArray.removeIf(d -> d.mCallId.equals(callId));
        }
    }

    /** Add a listener to the hashmap for waiting upcoming DedicatedBearer established event */
    public synchronized void onImsDedicatedBearerListenerAdded(@NonNull final int listenerId,
            @NonNull final int slotId, @NonNull final int ratAtEnd, @NonNull final int qci) {
        int subId = getSubId(slotId);
        int carrierId = getCarrierId(subId);

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                || !isValidCarrierId(carrierId)) {
            return;
        }

        if (mDedicatedBearerListenerEventMap.containsKey(listenerId)) {
            return;
        }

        ImsDedicatedBearerListenerEvent preProto = new ImsDedicatedBearerListenerEvent();
        preProto.carrierId = carrierId;
        preProto.slotId = slotId;
        preProto.ratAtEnd = ratAtEnd;
        preProto.qci = qci;
        preProto.dedicatedBearerEstablished = false;
        preProto.eventCount = 1;

        mDedicatedBearerListenerEventMap.put(listenerId, preProto);
    }

    /** update previously added atom with dedicatedBearerEstablished = true when
     *  DedicatedBearerListener Event changed. */
    public synchronized void onImsDedicatedBearerListenerUpdateSession(final int listenerId,
            final int slotId, final int rat, final int qci,
            @NonNull final boolean dedicatedBearerEstablished) {
        int subId = getSubId(slotId);
        int carrierId = getCarrierId(subId);

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                || !isValidCarrierId(carrierId)) {
            return;
        }

        if (mDedicatedBearerListenerEventMap.containsKey(listenerId)) {
            ImsDedicatedBearerListenerEvent preProto =
                    mDedicatedBearerListenerEventMap.get(listenerId);

            preProto.ratAtEnd = rat;
            preProto.qci = qci;
            preProto.dedicatedBearerEstablished = dedicatedBearerEstablished;

            mDedicatedBearerListenerEventMap.replace(listenerId, preProto);
        } else {
            ImsDedicatedBearerListenerEvent preProto = new ImsDedicatedBearerListenerEvent();
            preProto.carrierId = carrierId;
            preProto.slotId = slotId;
            preProto.ratAtEnd = rat;
            preProto.qci = qci;
            preProto.dedicatedBearerEstablished = dedicatedBearerEstablished;
            preProto.eventCount = 1;

            mDedicatedBearerListenerEventMap.put(listenerId, preProto);
        }
    }

    /** add proto to atom when listener is removed, so that I can save the status of dedicatedbearer
     *  establishment per listener id */
    public synchronized void onImsDedicatedBearerListenerRemoved(@NonNull final int listenerId) {
        if (mDedicatedBearerListenerEventMap.containsKey(listenerId)) {

            ImsDedicatedBearerListenerEvent newProto =
                    mDedicatedBearerListenerEventMap.get(listenerId);

            mAtomsStorage.addImsDedicatedBearerListenerEvent(newProto);
            mDedicatedBearerListenerEventMap.remove(listenerId);
        }
    }

    /** Create a new atom when DedicatedBearer Event changed. */
    public synchronized void onImsDedicatedBearerEvent(int slotId, int ratAtEnd, int qci,
            int bearerState, boolean localConnectionInfoReceived,
            boolean remoteConnectionInfoReceived, boolean hasListeners) {
        int subId = getSubId(slotId);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }

        ImsDedicatedBearerEvent proto = new ImsDedicatedBearerEvent();
        proto.carrierId = getCarrierId(subId);
        proto.slotId = getSlotId(subId);
        proto.ratAtEnd = ratAtEnd;
        proto.qci = qci;
        proto.bearerState = bearerState;
        proto.localConnectionInfoReceived = localConnectionInfoReceived;
        proto.remoteConnectionInfoReceived = remoteConnectionInfoReceived;
        proto.hasListeners = hasListeners;
        proto.count = 1;
        mAtomsStorage.addImsDedicatedBearerEvent(proto);
    }

    /**
     * Update or Create a new atom when Ims Registration Service Desc state changed.
     * Use-related parts are already converted from UseStatsWriter based on RcsContactPresenceTuple.
     */
    public void onImsRegistrationServiceDescStats(int subId, List<String> serviceIdList,
            List<String> serviceIdVersionList, int registrationTech) {
        synchronized (mImsRegistrationServiceDescStatsList) {
            int carrierId = getCarrierId(subId);
            if (!isValidCarrierId(carrierId)) {
                handleImsRegistrationServiceDescStats();
                return;
            }
            // update cached atom if exists
            onStoreCompleteImsRegistrationServiceDescStats(subId);

            if (serviceIdList == null) {
                Rlog.d(TAG, "serviceIds is null or empty");
                return;
            }

            int index = 0;
            for (String serviceId : serviceIdList) {
                ImsRegistrationServiceDescStats mImsRegistrationServiceDescStats =
                        new ImsRegistrationServiceDescStats();

                mImsRegistrationServiceDescStats.carrierId = carrierId;
                mImsRegistrationServiceDescStats.slotId = getSlotId(subId);
                mImsRegistrationServiceDescStats.serviceIdName = convertServiceIdToValue(serviceId);
                mImsRegistrationServiceDescStats.serviceIdVersion =
                        Float.parseFloat(serviceIdVersionList.get(index++));
                mImsRegistrationServiceDescStats.registrationTech = registrationTech;
                mImsRegistrationServiceDescStatsList.add(mImsRegistrationServiceDescStats);
            }
        }
    }

    /** Update duration and cached of ImsRegistrationServiceDescStats when metrics are pulled */
    public void onFlushIncompleteImsRegistrationServiceDescStats() {
        synchronized (mImsRegistrationServiceDescStatsList) {
            for (ImsRegistrationServiceDescStats proto : mImsRegistrationServiceDescStatsList) {
                ImsRegistrationServiceDescStats newProto =
                        copyImsRegistrationServiceDescStats(proto);
                long now = getWallTimeMillis();
                // the current time is a placeholder and total registered time will be
                // calculated when generating final atoms
                newProto.publishedMillis = now - proto.publishedMillis;
                mAtomsStorage.addImsRegistrationServiceDescStats(newProto);
                proto.publishedMillis = now;
            }
        }
    }

    /** Create a new atom when Uce Event Stats changed. */
    public synchronized void onUceEventStats(int subId, int type, boolean successful,
            int commandCode, int networkResponse) {
        UceEventStats proto = new UceEventStats();

        int carrierId = getCarrierId(subId);
        if (!isValidCarrierId(carrierId)) {
            handleImsRegistrationServiceDescStats();
            return;
        }
        proto.carrierId = carrierId;
        proto.slotId = getSlotId(subId);
        proto.type = type;
        proto.successful = successful;
        proto.commandCode = commandCode;
        proto.networkResponse = networkResponse;
        proto.count = 1;
        mAtomsStorage.addUceEventStats(proto);

        /**
         * The publishedMillis of ImsRegistrationServiceDescStat is the time gap between
         * Publish success and Un publish.
         * So, when the publish operation is successful, the corresponding time gap is set,
         * and in case of failure, the cached stat is deleted.
         */
        if (type == UCE_EVENT_STATS__TYPE__PUBLISH) {
            if (successful) {
                setImsRegistrationServiceDescStatsTime(proto.carrierId);
            } else {
                deleteImsRegistrationServiceDescStats(proto.carrierId);
            }
        }
    }

    /** Create a new atom when Presence Notify Event changed. */
    public synchronized void onPresenceNotifyEvent(int subId, String reason,
            boolean contentBodyReceived, boolean rcsCaps, boolean mmtelCaps, boolean noCaps) {
        PresenceNotifyEvent proto = new PresenceNotifyEvent();

        int carrierId = getCarrierId(subId);
        if (!isValidCarrierId(carrierId)) {
            handleImsRegistrationServiceDescStats();
            return;
        }

        proto.carrierId = carrierId;
        proto.slotId = getSlotId(subId);
        proto.reason = convertPresenceNotifyReason(reason);
        proto.contentBodyReceived = contentBodyReceived;
        proto.rcsCapsCount = rcsCaps ? 1 : 0;
        proto.mmtelCapsCount = mmtelCaps ? 1 : 0;
        proto.noCapsCount = noCaps ? 1 : 0;
        proto.count = 1;
        mAtomsStorage.addPresenceNotifyEvent(proto);
    }

    /** Update duration a created Ims Registration Desc Stat atom when Un publish event happened. */
    public void onStoreCompleteImsRegistrationServiceDescStats(int subId) {
        synchronized (mImsRegistrationServiceDescStatsList) {
            int carrierId = getCarrierId(subId);
            List<ImsRegistrationServiceDescStats> deleteList = new ArrayList<>();
            for (ImsRegistrationServiceDescStats proto : mImsRegistrationServiceDescStatsList) {
                if (proto.carrierId == carrierId) {
                    proto.publishedMillis = getWallTimeMillis() - proto.publishedMillis;
                    mAtomsStorage.addImsRegistrationServiceDescStats(proto);
                    deleteList.add(proto);
                }
            }
            for (ImsRegistrationServiceDescStats proto : deleteList) {
                mImsRegistrationServiceDescStatsList.remove(proto);
            }
        }
    }

    /** Create a new atom when GBA Success Event changed. */
    public synchronized void onGbaSuccessEvent(int subId) {
        int carrierId = getCarrierId(subId);
        if (!isValidCarrierId(carrierId)) {
            return;
        }

        GbaEvent proto = new GbaEvent();
        proto.carrierId = carrierId;
        proto.slotId = getSlotId(subId);
        proto.successful = true;
        proto.failedReason = -1;
        proto.count = 1;
        mAtomsStorage.addGbaEvent(proto);
    }

    /** Create a new atom when GBA Failure Event changed. */
    public synchronized void onGbaFailureEvent(int subId, int reason) {
        int carrierId = getCarrierId(subId);
        if (!isValidCarrierId(carrierId)) {
            return;
        }

        GbaEvent proto = new GbaEvent();
        proto.carrierId = carrierId;
        proto.slotId = getSlotId(subId);
        proto.successful = false;
        proto.failedReason = reason;
        proto.count = 1;
        mAtomsStorage.addGbaEvent(proto);
    }

    /** Create or return exist RcsProvisioningCallback based on subId. */
    public synchronized RcsProvisioningCallback getRcsProvisioningCallback(int subId,
            boolean enableSingleRegistration) {
        // find exist obj in Map
        RcsProvisioningCallback rcsProvisioningCallback = mRcsProvisioningCallbackMap.get(subId);
        if (rcsProvisioningCallback != null) {
            return rcsProvisioningCallback;
        }

        // create new, add Map and return
        rcsProvisioningCallback = new RcsProvisioningCallback(this, subId,
                enableSingleRegistration);
        mRcsProvisioningCallbackMap.put(subId, rcsProvisioningCallback);
        return rcsProvisioningCallback;
    }

    /** Set whether single registration is supported. */
    public synchronized void setEnableSingleRegistration(int subId,
            boolean enableSingleRegistration) {
        // find exist obj and set
        RcsProvisioningCallback callbackBinder = mRcsProvisioningCallbackMap.get(subId);
        if (callbackBinder != null) {
            callbackBinder.setEnableSingleRegistration(enableSingleRegistration);
        }
    }

    private synchronized void removeRcsProvisioningCallback(int subId) {
        // remove obj from Map based on subId
        mRcsProvisioningCallbackMap.remove(subId);
    }

    private ImsRegistrationFeatureTagStats copyImsRegistrationFeatureTagStats(
            ImsRegistrationFeatureTagStats proto) {
        ImsRegistrationFeatureTagStats newProto = new ImsRegistrationFeatureTagStats();
        newProto.carrierId = proto.carrierId;
        newProto.slotId = proto.slotId;
        newProto.featureTagName = proto.featureTagName;
        newProto.registrationTech = proto.registrationTech;
        newProto.registeredMillis = proto.registeredMillis;

        return newProto;
    }

    private RcsAcsProvisioningStats copyRcsAcsProvisioningStats(RcsAcsProvisioningStats proto) {
        RcsAcsProvisioningStats newProto = new RcsAcsProvisioningStats();
        newProto.carrierId = proto.carrierId;
        newProto.slotId = proto.slotId;
        newProto.responseCode = proto.responseCode;
        newProto.responseType = proto.responseType;
        newProto.isSingleRegistrationEnabled = proto.isSingleRegistrationEnabled;
        newProto.count = proto.count;
        newProto.stateTimerMillis = proto.stateTimerMillis;

        return newProto;
    }

    private ImsRegistrationServiceDescStats copyImsRegistrationServiceDescStats(
            ImsRegistrationServiceDescStats proto) {
        ImsRegistrationServiceDescStats newProto = new ImsRegistrationServiceDescStats();
        newProto.carrierId = proto.carrierId;
        newProto.slotId = proto.slotId;
        newProto.serviceIdName = proto.serviceIdName;
        newProto.serviceIdVersion = proto.serviceIdVersion;
        newProto.registrationTech = proto.registrationTech;
        return newProto;
    }

    private void setImsRegistrationServiceDescStatsTime(int carrierId) {
        synchronized (mImsRegistrationServiceDescStatsList) {
            for (ImsRegistrationServiceDescStats descStats : mImsRegistrationServiceDescStatsList) {
                if (descStats.carrierId == carrierId) {
                    descStats.publishedMillis = getWallTimeMillis();
                }
            }
        }
    }

    private void deleteImsRegistrationServiceDescStats(int carrierId) {
        synchronized (mImsRegistrationServiceDescStatsList) {
            List<ImsRegistrationServiceDescStats> deleteList = new ArrayList<>();
            for (ImsRegistrationServiceDescStats proto : mImsRegistrationServiceDescStatsList) {
                if (proto.carrierId == carrierId) {
                    deleteList.add(proto);
                }
            }
            for (ImsRegistrationServiceDescStats stats : deleteList) {
                mImsRegistrationServiceDescStatsList.remove(stats);
            }
        }
    }

    private void handleImsRegistrationServiceDescStats() {
        synchronized (mImsRegistrationServiceDescStatsList) {
            List<ImsRegistrationServiceDescStats> deleteList = new ArrayList<>();
            for (ImsRegistrationServiceDescStats proto : mImsRegistrationServiceDescStatsList) {
                int subId = getSubId(proto.slotId);
                int newCarrierId = getCarrierId(subId);
                if (proto.carrierId != newCarrierId) {
                    deleteList.add(proto);
                    if (proto.publishedMillis != 0) {
                        proto.publishedMillis = getWallTimeMillis() - proto.publishedMillis;
                        mAtomsStorage.addImsRegistrationServiceDescStats(proto);
                    }
                }
            }
            for (ImsRegistrationServiceDescStats stats : deleteList) {
                mImsRegistrationServiceDescStatsList.remove(stats);
            }
        }
    }

    private RcsAcsProvisioningStats getRcsAcsProvisioningStats(int subId) {
        int carrierId = getCarrierId(subId);
        int slotId = getSlotId(subId);

        for (RcsAcsProvisioningStats stats : mRcsAcsProvisioningStatsList) {
            if (stats == null) {
                continue;
            }
            if (stats.carrierId == carrierId && stats.slotId == slotId) {
                return stats;
            }
        }
        return null;
    }

    private void flushRcsAcsProvisioningStatsInvalid() {
        List<RcsAcsProvisioningStats> inValidList = new ArrayList<RcsAcsProvisioningStats>();

        int subId;
        int newCarrierId;

        for (RcsAcsProvisioningStats stats : mRcsAcsProvisioningStatsList) {
            subId = getSubId(stats.slotId);
            newCarrierId = getCarrierId(subId);
            if (stats.carrierId != newCarrierId) {
                inValidList.add(stats);
            }
        }

        for (RcsAcsProvisioningStats inValid : inValidList) {
            inValid.stateTimerMillis = getWallTimeMillis() - inValid.stateTimerMillis;
            mAtomsStorage.addRcsAcsProvisioningStats(inValid);
            mRcsAcsProvisioningStatsList.remove(inValid);
        }
        inValidList.clear();
    }

    private void flushImsRegistrationFeatureTagStatsInvalid() {
        List<ImsRegistrationFeatureTagStats> inValidList =
                new ArrayList<ImsRegistrationFeatureTagStats>();

        int subId;
        int newCarrierId;

        for (ImsRegistrationFeatureTagStats stats : mImsRegistrationFeatureTagStatsList) {
            subId = getSubId(stats.slotId);
            newCarrierId = getCarrierId(subId);
            if (stats.carrierId != newCarrierId) {
                inValidList.add(stats);
            }
        }

        for (ImsRegistrationFeatureTagStats inValid : inValidList) {
            inValid.registeredMillis = getWallTimeMillis() - inValid.registeredMillis;
            mAtomsStorage.addImsRegistrationFeatureTagStats(inValid);
            mImsRegistrationFeatureTagStatsList.remove(inValid);
        }
        inValidList.clear();
    }

    private LastSipDelegateStat getLastSipDelegateStat(int subId, Set<String> supportedTags) {
        LastSipDelegateStat stat = null;
        for (LastSipDelegateStat lastStat : mLastSipDelegateStatList) {
            if (lastStat.compare(subId, supportedTags)) {
                stat = lastStat;
                break;
            }
        }

        if (stat == null) {
            stat = new LastSipDelegateStat(subId, supportedTags);
            mLastSipDelegateStatList.add(stat);
        }

        return stat;
    }

    private void concludeSipDelegateStat() {
        if (mLastSipDelegateStatList.isEmpty()) {
            return;
        }
        long now = getWallTimeMillis();
        List<LastSipDelegateStat> sipDelegateStatsCopy = new ArrayList<>(mLastSipDelegateStatList);
        for (LastSipDelegateStat stat : sipDelegateStatsCopy) {
            if (stat.isDestroyed()) {
                stat.conclude(now);
                mLastSipDelegateStatList.remove(stat);
            }
        }
    }

    private SipTransportFeatureTags getLastFeatureTags(int subId) {
        SipTransportFeatureTags sipTransportFeatureTags;
        if (mLastFeatureTagStatMap.containsKey(subId)) {
            sipTransportFeatureTags = mLastFeatureTagStatMap.get(subId);
        } else {
            sipTransportFeatureTags = new SipTransportFeatureTags(subId);
            mLastFeatureTagStatMap.put(subId, sipTransportFeatureTags);
        }
        return sipTransportFeatureTags;
    }
    @VisibleForTesting
    protected boolean isValidCarrierId(int carrierId) {
        return carrierId > TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    @VisibleForTesting
    protected int getSlotId(int subId) {
        return SubscriptionManager.getPhoneId(subId);
    }

    @VisibleForTesting
    protected int getCarrierId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        return phone != null ? phone.getCarrierId() : TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    @VisibleForTesting
    protected long getWallTimeMillis() {
        //time in UTC, preserved across reboots, but can be adjusted e.g. by the user or NTP
        return System.currentTimeMillis();
    }

    @VisibleForTesting
    protected void logd(String msg) {
        Rlog.d(TAG, msg);
    }

    @VisibleForTesting
    protected int getSubId(int slotId) {
        final int[] subIds = SubscriptionManager.getSubId(slotId);
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (subIds != null && subIds.length >= 1) {
            subId = subIds[0];
        }
        return subId;
    }

    /** Get a enum value from pre-defined feature tag name list */
    @VisibleForTesting
    public int convertTagNameToValue(@NonNull String tagName) {
        return FEATURE_TAGS.getOrDefault(tagName.trim().toLowerCase(),
                TelephonyProtoEnums.IMS_FEATURE_TAG_CUSTOM);
    }

    /** Get a enum value from pre-defined service id list */
    @VisibleForTesting
    public int convertServiceIdToValue(@NonNull String serviceId) {
        return SERVICE_IDS.getOrDefault(serviceId.trim().toLowerCase(),
                IMS_REGISTRATION_SERVICE_DESC_STATS__SERVICE_ID_NAME__SERVICE_ID_CUSTOM);
    }

    /** Get a enum value from pre-defined message type list */
    @VisibleForTesting
    public int convertMessageTypeToValue(@NonNull String messageType) {
        return MESSAGE_TYPE.getOrDefault(messageType.trim().toLowerCase(),
                TelephonyProtoEnums.SIP_REQUEST_CUSTOM);
    }

    /** Get a enum value from pre-defined reason list */
    @VisibleForTesting
    public int convertPresenceNotifyReason(@NonNull String reason) {
        return NOTIFY_REASONS.getOrDefault(reason.trim().toLowerCase(),
                PRESENCE_NOTIFY_EVENT__REASON__REASON_CUSTOM);
    }

    /**
     * Print all metrics data for debugging purposes
     *
     * @param rawWriter Print writer
     */
    public synchronized void printAllMetrics(PrintWriter rawWriter) {
        if (mAtomsStorage == null || mAtomsStorage.mAtoms == null) {
            return;
        }

        final IndentingPrintWriter pw = new IndentingPrintWriter(rawWriter, "  ");
        PersistAtomsProto.PersistAtoms metricAtoms = mAtomsStorage.mAtoms;

        pw.println("RcsStats Metrics Proto: ");
        pw.println("------------------------------------------");
        pw.println("ImsRegistrationFeatureTagStats:");
        pw.increaseIndent();
        for (ImsRegistrationFeatureTagStats stat : metricAtoms.imsRegistrationFeatureTagStats) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Feature Tag Name = " + stat.featureTagName
                    + ", Registration Tech = " + stat.registrationTech
                    + ", Registered Duration (ms) = " + stat.registeredMillis);
        }
        pw.decreaseIndent();

        pw.println("RcsClientProvisioningStats:");
        pw.increaseIndent();
        for (RcsClientProvisioningStats stat : metricAtoms.rcsClientProvisioningStats) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Event = " + stat.event
                    + ", Count = " + stat.count);
        }
        pw.decreaseIndent();

        pw.println("RcsAcsProvisioningStats:");
        pw.increaseIndent();
        for (RcsAcsProvisioningStats stat : metricAtoms.rcsAcsProvisioningStats) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Response Code = " + stat.responseCode
                    + ", Response Type = " + stat.responseType
                    + ", Single Registration Enabled = " + stat.isSingleRegistrationEnabled
                    + ", Count = " + stat.count
                    + ", State Timer (ms) = " + stat.stateTimerMillis);
        }
        pw.decreaseIndent();

        pw.println("SipDelegateStats:");
        pw.increaseIndent();
        for (SipDelegateStats stat : metricAtoms.sipDelegateStats) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " [" + stat.dimension + "]"
                    + " Destroy Reason = " + stat.destroyReason
                    + ", Uptime (ms) = " + stat.uptimeMillis);
        }
        pw.decreaseIndent();

        pw.println("SipTransportFeatureTagStats:");
        pw.increaseIndent();
        for (SipTransportFeatureTagStats stat : metricAtoms.sipTransportFeatureTagStats) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Feature Tag Name = " + stat.featureTagName
                    + ", Denied Reason = " + stat.sipTransportDeniedReason
                    + ", Deregistered Reason = " + stat.sipTransportDeregisteredReason
                    + ", Associated Time (ms) = " + stat.associatedMillis);
        }
        pw.decreaseIndent();

        pw.println("SipMessageResponse:");
        pw.increaseIndent();
        for (SipMessageResponse stat : metricAtoms.sipMessageResponse) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Message Method = " + stat.sipMessageMethod
                    + ", Response = " + stat.sipMessageResponse
                    + ", Direction = " + stat.sipMessageDirection
                    + ", Error = " + stat.messageError
                    + ", Count = " + stat.count);
        }
        pw.decreaseIndent();

        pw.println("SipTransportSession:");
        pw.increaseIndent();
        for (SipTransportSession stat : metricAtoms.sipTransportSession) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Session Method = " + stat.sessionMethod
                    + ", Direction = " + stat.sipMessageDirection
                    + ", Response = " + stat.sipResponse
                    + ", Count = " + stat.sessionCount
                    + ", GraceFully Count = " + stat.endedGracefullyCount);
        }
        pw.decreaseIndent();

        pw.println("ImsDedicatedBearerListenerEvent:");
        pw.increaseIndent();
        for (ImsDedicatedBearerListenerEvent stat : metricAtoms.imsDedicatedBearerListenerEvent) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " RAT = " + stat.ratAtEnd
                    + ", QCI = " + stat.qci
                    + ", Dedicated Bearer Established = " + stat.dedicatedBearerEstablished
                    + ", Count = " + stat.eventCount);
        }
        pw.decreaseIndent();

        pw.println("ImsDedicatedBearerEvent:");
        pw.increaseIndent();
        for (ImsDedicatedBearerEvent stat : metricAtoms.imsDedicatedBearerEvent) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " RAT = " + stat.ratAtEnd
                    + ", QCI = " + stat.qci
                    + ", Bearer State = " + stat.bearerState
                    + ", Local Connection Info = " + stat.localConnectionInfoReceived
                    + ", Remote Connection Info = " + stat.remoteConnectionInfoReceived
                    + ", Listener Existence = " + stat.hasListeners
                    + ", Count = " + stat.count);
        }
        pw.decreaseIndent();

        pw.println("ImsRegistrationServiceDescStats:");
        pw.increaseIndent();
        for (ImsRegistrationServiceDescStats stat : metricAtoms.imsRegistrationServiceDescStats) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Name = " + stat.serviceIdName
                    + ", Version = " + stat.serviceIdVersion
                    + ", Registration Tech = " + stat.registrationTech
                    + ", Published Time (ms) = " + stat.publishedMillis);
        }
        pw.decreaseIndent();

        pw.println("UceEventStats:");
        pw.increaseIndent();
        for (UceEventStats stat : metricAtoms.uceEventStats) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Type = " + stat.type
                    + ", Successful = " + stat.successful
                    + ", Code = " + stat.commandCode
                    + ", Response = " + stat.networkResponse
                    + ", Count = " + stat.count);
        }
        pw.decreaseIndent();

        pw.println("PresenceNotifyEvent:");
        pw.increaseIndent();
        for (PresenceNotifyEvent stat : metricAtoms.presenceNotifyEvent) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Reason = " + stat.reason
                    + ", Body = " + stat.contentBodyReceived
                    + ", RCS Count = " + stat.rcsCapsCount
                    + ", MMTEL Count = " + stat.mmtelCapsCount
                    + ", NoCaps Count = " + stat.noCapsCount
                    + ", Count = " + stat.count);
        }
        pw.decreaseIndent();

        pw.println("GbaEvent:");
        pw.increaseIndent();
        for (GbaEvent stat : metricAtoms.gbaEvent) {
            pw.println("[" + stat.carrierId + "]"
                    + " [" + stat.slotId + "]"
                    + " Successful = "  + stat.successful
                    + ", Fail Reason = " + stat.failedReason
                    + ", Count = " + stat.count);
        }
        pw.decreaseIndent();
    }

    /**
     * Reset all events
     */
    public synchronized void reset() {
        if (mAtomsStorage == null || mAtomsStorage.mAtoms == null) {
            return;
        }

        PersistAtomsProto.PersistAtoms metricAtoms = mAtomsStorage.mAtoms;

        metricAtoms.imsRegistrationFeatureTagStats =
                PersistAtomsProto.ImsRegistrationFeatureTagStats.emptyArray();
        metricAtoms.rcsClientProvisioningStats =
                PersistAtomsProto.RcsClientProvisioningStats.emptyArray();
        metricAtoms.rcsAcsProvisioningStats =
                PersistAtomsProto.RcsAcsProvisioningStats.emptyArray();
        metricAtoms.sipDelegateStats = PersistAtomsProto.SipDelegateStats.emptyArray();
        metricAtoms.sipTransportFeatureTagStats =
                PersistAtomsProto.SipTransportFeatureTagStats.emptyArray();
        metricAtoms.sipMessageResponse = PersistAtomsProto.SipMessageResponse.emptyArray();
        metricAtoms.sipTransportSession = PersistAtomsProto.SipTransportSession.emptyArray();
        metricAtoms.imsDedicatedBearerListenerEvent =
                PersistAtomsProto.ImsDedicatedBearerListenerEvent.emptyArray();
        metricAtoms.imsDedicatedBearerEvent =
                PersistAtomsProto.ImsDedicatedBearerEvent.emptyArray();
        metricAtoms.imsRegistrationServiceDescStats =
                PersistAtomsProto.ImsRegistrationServiceDescStats.emptyArray();
        metricAtoms.uceEventStats = PersistAtomsProto.UceEventStats.emptyArray();
        metricAtoms.presenceNotifyEvent = PersistAtomsProto.PresenceNotifyEvent.emptyArray();
        metricAtoms.gbaEvent = PersistAtomsProto.GbaEvent.emptyArray();
    }

    /**
     * Convert the PersistAtomsProto into Base-64 encoded string
     *
     * @return Encoded string
     */
    public String buildLog() {
        PersistAtomsProto.PersistAtoms log = buildProto();
        return Base64.encodeToString(
                PersistAtomsProto.PersistAtoms.toByteArray(log), Base64.DEFAULT);
    }

    /**
     * Build the PersistAtomsProto
     *
     * @return PersistAtomsProto.PersistAtoms
     */
    public PersistAtomsProto.PersistAtoms buildProto() {
        PersistAtomsProto.PersistAtoms log = new PersistAtomsProto.PersistAtoms();

        PersistAtomsProto.PersistAtoms atoms = mAtomsStorage.mAtoms;
        log.imsRegistrationFeatureTagStats = Arrays.copyOf(atoms.imsRegistrationFeatureTagStats,
                atoms.imsRegistrationFeatureTagStats.length);
        log.rcsClientProvisioningStats = Arrays.copyOf(atoms.rcsClientProvisioningStats,
                atoms.rcsClientProvisioningStats.length);
        log.rcsAcsProvisioningStats = Arrays.copyOf(atoms.rcsAcsProvisioningStats,
                atoms.rcsAcsProvisioningStats.length);
        log.sipDelegateStats = Arrays.copyOf(atoms.sipDelegateStats, atoms.sipDelegateStats.length);
        log.sipTransportFeatureTagStats = Arrays.copyOf(atoms.sipTransportFeatureTagStats,
                atoms.sipTransportFeatureTagStats.length);
        log.sipMessageResponse = Arrays.copyOf(atoms.sipMessageResponse,
                atoms.sipMessageResponse.length);
        log.sipTransportSession = Arrays.copyOf(atoms.sipTransportSession,
                atoms.sipTransportSession.length);
        log.imsDedicatedBearerListenerEvent = Arrays.copyOf(atoms.imsDedicatedBearerListenerEvent,
                atoms.imsDedicatedBearerListenerEvent.length);
        log.imsDedicatedBearerEvent = Arrays.copyOf(atoms.imsDedicatedBearerEvent,
                atoms.imsDedicatedBearerEvent.length);
        log.imsRegistrationServiceDescStats = Arrays.copyOf(atoms.imsRegistrationServiceDescStats,
                atoms.imsRegistrationServiceDescStats.length);
        log.uceEventStats = Arrays.copyOf(atoms.uceEventStats, atoms.uceEventStats.length);
        log.presenceNotifyEvent = Arrays.copyOf(atoms.presenceNotifyEvent,
                atoms.presenceNotifyEvent.length);
        log.gbaEvent = Arrays.copyOf(atoms.gbaEvent, atoms.gbaEvent.length);

        return log;
    }

}
