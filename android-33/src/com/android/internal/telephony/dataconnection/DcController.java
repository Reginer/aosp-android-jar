/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.internal.telephony.dataconnection;

import android.hardware.radio.V1_4.DataConnActiveStatus;
import android.net.LinkAddress;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RegistrantList;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.DataFailCause;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.TrafficDescriptor;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.DataConnection.UpdateLinkPropertyResult;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.net.module.util.LinkPropertiesUtils;
import com.android.net.module.util.LinkPropertiesUtils.CompareOrUpdateResult;
import com.android.net.module.util.NetUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Data Connection Controller which is a package visible class and controls
 * multiple data connections. For instance listening for unsolicited messages
 * and then demultiplexing them to the appropriate DC.
 */
public class DcController extends Handler {
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    private final Phone mPhone;
    private final DcTracker mDct;
    private final String mTag;
    private final DataServiceManager mDataServiceManager;
    private final DcTesterDeactivateAll mDcTesterDeactivateAll;

    // package as its used by Testing code
    // @GuardedBy("mDcListAll")
    final ArrayList<DataConnection> mDcListAll = new ArrayList<>();
    // @GuardedBy("mDcListAll")
    private final HashMap<Integer, DataConnection> mDcListActiveByCid = new HashMap<>();
    // @GuardedBy("mTrafficDescriptorsByCid")
    private final HashMap<Integer, List<TrafficDescriptor>> mTrafficDescriptorsByCid =
            new HashMap<>();

    /**
     * Aggregated physical link status from all data connections. This reflects the device's RRC
     * connection state.
     * If {@link CarrierConfigManager#KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL} is true,
     * then This reflects "internet data connection" instead of RRC state.
     */
    private @DataCallResponse.LinkStatus int mPhysicalLinkStatus =
            DataCallResponse.LINK_STATUS_UNKNOWN;

    private RegistrantList mPhysicalLinkStatusChangedRegistrants = new RegistrantList();

    /**
     * Constructor.
     *
     * @param name to be used for the Controller
     * @param phone the phone associated with Dcc and Dct
     * @param dct the DataConnectionTracker associated with Dcc
     * @param dataServiceManager the data service manager that manages data services
     * @param looper looper for this handler
     */
    private DcController(String name, Phone phone, DcTracker dct,
                         DataServiceManager dataServiceManager, Looper looper) {
        super(looper);
        mPhone = phone;
        mDct = dct;
        mTag = name;
        mDataServiceManager = dataServiceManager;

        mDcTesterDeactivateAll = (TelephonyUtils.IS_DEBUGGABLE)
                ? new DcTesterDeactivateAll(mPhone, DcController.this, this)
                : null;
        mDataServiceManager.registerForDataCallListChanged(this,
                DataConnection.EVENT_DATA_STATE_CHANGED);
    }

    public static DcController makeDcc(Phone phone, DcTracker dct,
                                       DataServiceManager dataServiceManager, Looper looper,
                                       String tagSuffix) {
        return new DcController("Dcc" + tagSuffix, phone, dct, dataServiceManager, looper);
    }

    void addDc(DataConnection dc) {
        synchronized (mDcListAll) {
            mDcListAll.add(dc);
        }
    }

    void removeDc(DataConnection dc) {
        synchronized (mDcListAll) {
            mDcListActiveByCid.remove(dc.mCid);
            mDcListAll.remove(dc);
        }
        synchronized (mTrafficDescriptorsByCid) {
            mTrafficDescriptorsByCid.remove(dc.mCid);
        }
    }

    public void addActiveDcByCid(DataConnection dc) {
        if (DBG && dc.mCid < 0) {
            log("addActiveDcByCid dc.mCid < 0 dc=" + dc);
        }
        synchronized (mDcListAll) {
            mDcListActiveByCid.put(dc.mCid, dc);
        }
        updateTrafficDescriptorsForCid(dc.mCid, dc.getTrafficDescriptors());
    }

    DataConnection getActiveDcByCid(int cid) {
        synchronized (mDcListAll) {
            return mDcListActiveByCid.get(cid);
        }
    }

    void removeActiveDcByCid(DataConnection dc) {
        synchronized (mDcListAll) {
            DataConnection removedDc = mDcListActiveByCid.remove(dc.mCid);
            if (DBG && removedDc == null) {
                log("removeActiveDcByCid removedDc=null dc=" + dc);
            }
        }
        synchronized (mTrafficDescriptorsByCid) {
            mTrafficDescriptorsByCid.remove(dc.mCid);
        }
    }

    boolean isDefaultDataActive() {
        synchronized (mDcListAll) {
            return mDcListActiveByCid.values().stream()
                    .anyMatch(dc -> dc.getApnContexts().stream()
                            .anyMatch(apn -> apn.getApnTypeBitmask() == ApnSetting.TYPE_DEFAULT));
        }
    }

    List<TrafficDescriptor> getTrafficDescriptorsForCid(int cid) {
        synchronized (mTrafficDescriptorsByCid) {
            return mTrafficDescriptorsByCid.get(cid);
        }
    }

    void updateTrafficDescriptorsForCid(int cid, List<TrafficDescriptor> tds) {
        synchronized (mTrafficDescriptorsByCid) {
            mTrafficDescriptorsByCid.put(cid, tds);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case DataConnection.EVENT_DATA_STATE_CHANGED:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    onDataStateChanged((ArrayList<DataCallResponse>) ar.result);
                } else {
                    log("EVENT_DATA_STATE_CHANGED: exception; likely radio not available, ignore");
                }
                break;
            default:
                loge("Unexpected event " + msg);
                break;
        }
    }

    /**
     * Process the new list of "known" Data Calls
     * @param dcsList as sent by RIL_UNSOL_DATA_CALL_LIST_CHANGED
     */
    private void onDataStateChanged(ArrayList<DataCallResponse> dcsList) {
        final HashMap<Integer, DataConnection> dcListActiveByCid;
        synchronized (mDcListAll) {
            dcListActiveByCid = new HashMap<>(mDcListActiveByCid);
        }

        if (DBG) {
            log("onDataStateChanged: dcsList=" + dcsList
                    + " dcListActiveByCid=" + dcListActiveByCid);
        }

        // Create hashmap of cid to DataCallResponse
        HashMap<Integer, DataCallResponse> dataCallResponseListByCid = new HashMap<>();
        for (DataCallResponse dcs : dcsList) {
            dataCallResponseListByCid.put(dcs.getId(), dcs);
        }

        // Add a DC that is active but not in the dcsList to the list of DC's to retry
        ArrayList<DataConnection> dcsToRetry = new ArrayList<>();
        for (DataConnection dc : dcListActiveByCid.values()) {
            DataCallResponse response = dataCallResponseListByCid.get(dc.mCid);
            if (response == null) {
                if (DBG) log("onDataStateChanged: add to retry dc=" + dc);
                dcsToRetry.add(dc);
            } else {
                List<TrafficDescriptor> oldTds = getTrafficDescriptorsForCid(dc.mCid);
                List<TrafficDescriptor> newTds = response.getTrafficDescriptors();
                if (!oldTds.equals(newTds)) {
                    if (DBG) {
                        log("onDataStateChanged: add to retry due to TD changed dc=" + dc
                                + ", oldTds=" + oldTds + ", newTds=" + newTds);
                    }
                    updateTrafficDescriptorsForCid(dc.mCid, newTds);
                    dcsToRetry.add(dc);
                }
            }
        }
        if (DBG) log("onDataStateChanged: dcsToRetry=" + dcsToRetry);

        // Find which connections have changed state and send a notification or cleanup
        // and any that are in active need to be retried.
        ArrayList<ApnContext> apnsToCleanup = new ArrayList<ApnContext>();

        boolean isAnyDataCallDormant = false;
        boolean isAnyDataCallActive = false;
        boolean isInternetDataCallActive = false;

        for (DataCallResponse newState : dcsList) {

            DataConnection dc = dcListActiveByCid.get(newState.getId());
            if (dc == null) {
                // UNSOL_DATA_CALL_LIST_CHANGED arrived before SETUP_DATA_CALL completed.
                loge("onDataStateChanged: no associated DC yet, ignore");
                continue;
            }

            List<ApnContext> apnContexts = dc.getApnContexts();
            if (apnContexts.size() == 0) {
                if (DBG) loge("onDataStateChanged: no connected apns, ignore");
            } else {
                // Determine if the connection/apnContext should be cleaned up
                // or just a notification should be sent out.
                if (DBG) {
                    log("onDataStateChanged: Found ConnId=" + newState.getId()
                            + " newState=" + newState.toString());
                }
                if (apnContexts.stream().anyMatch(
                        i -> ApnSetting.TYPE_DEFAULT_STRING.equals(i.getApnType()))
                        && newState.getLinkStatus() == DataConnActiveStatus.ACTIVE) {
                    isInternetDataCallActive = true;
                }
                if (newState.getLinkStatus() == DataConnActiveStatus.INACTIVE) {
                    if (mDct.isCleanupRequired.get()) {
                        apnsToCleanup.addAll(apnContexts);
                        mDct.isCleanupRequired.set(false);
                    } else {
                        int failCause = DataFailCause.getFailCause(newState.getCause());
                        if (DataFailCause.isRadioRestartFailure(mPhone.getContext(), failCause,
                                    mPhone.getSubId())) {
                            if (DBG) {
                                log("onDataStateChanged: X restart radio, failCause="
                                        + failCause);
                            }
                            mDct.sendRestartRadio();
                        } else if (mDct.isPermanentFailure(failCause)) {
                            if (DBG) {
                                log("onDataStateChanged: inactive, add to cleanup list. "
                                        + "failCause=" + failCause);
                            }
                            apnsToCleanup.addAll(apnContexts);
                        } else {
                            if (DBG) {
                                log("onDataStateChanged: inactive, add to retry list. "
                                        + "failCause=" + failCause);
                            }
                            dcsToRetry.add(dc);
                        }
                    }
                } else {
                    // Update the pdu session id
                    dc.setPduSessionId(newState.getPduSessionId());

                    dc.updatePcscfAddr(newState);

                    // Its active so update the DataConnections link properties
                    UpdateLinkPropertyResult result = dc.updateLinkProperty(newState);
                    dc.updateResponseFields(newState);
                    if (result.oldLp.equals(result.newLp)) {
                        if (DBG) log("onDataStateChanged: no change");
                    } else {
                        if (LinkPropertiesUtils.isIdenticalInterfaceName(
                                result.oldLp, result.newLp)) {
                            if (!LinkPropertiesUtils.isIdenticalDnses(
                                    result.oldLp, result.newLp)
                                    || !LinkPropertiesUtils.isIdenticalRoutes(
                                            result.oldLp, result.newLp)
                                    || !LinkPropertiesUtils.isIdenticalHttpProxy(
                                            result.oldLp, result.newLp)
                                    || !LinkPropertiesUtils.isIdenticalAddresses(
                                            result.oldLp, result.newLp)) {
                                // If the same address type was removed and
                                // added we need to cleanup
                                CompareOrUpdateResult<Integer, LinkAddress> car
                                    = new CompareOrUpdateResult(
                                  result.oldLp != null ?
                                    result.oldLp.getLinkAddresses() : null,
                                  result.newLp != null ?
                                    result.newLp.getLinkAddresses() : null,
                                  (la) -> Objects.hash(((LinkAddress)la).getAddress(),
                                                       ((LinkAddress)la).getPrefixLength(),
                                                       ((LinkAddress)la).getScope()));
                                if (DBG) {
                                    log("onDataStateChanged: oldLp=" + result.oldLp
                                            + " newLp=" + result.newLp + " car=" + car);
                                }
                                boolean needToClean = false;
                                for (LinkAddress added : car.added) {
                                    for (LinkAddress removed : car.removed) {
                                        if (NetUtils.addressTypeMatches(
                                                removed.getAddress(),
                                                added.getAddress())) {
                                            needToClean = true;
                                            break;
                                        }
                                    }
                                }
                                if (needToClean) {
                                    if (DBG) {
                                        log("onDataStateChanged: addr change,"
                                                + " cleanup apns=" + apnContexts
                                                + " oldLp=" + result.oldLp
                                                + " newLp=" + result.newLp);
                                    }
                                    apnsToCleanup.addAll(apnContexts);
                                }
                            } else {
                                if (DBG) {
                                    log("onDataStateChanged: no changes");
                                }
                            }
                        } else {
                            apnsToCleanup.addAll(apnContexts);
                            if (DBG) {
                                log("onDataStateChanged: interface change, cleanup apns="
                                        + apnContexts);
                            }
                        }
                    }
                }
            }

            if (newState.getLinkStatus() == DataConnActiveStatus.ACTIVE) {
                isAnyDataCallActive = true;
            }
            if (newState.getLinkStatus() == DataConnActiveStatus.DORMANT) {
                isAnyDataCallDormant = true;
            }
        }

        if (mDataServiceManager.getTransportType()
                == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            boolean isPhysicalLinkStatusFocusingOnInternetData =
                    mDct.getLteEndcUsingUserDataForIdleDetection();
            int physicalLinkStatus =
                    (isPhysicalLinkStatusFocusingOnInternetData
                            ? isInternetDataCallActive : isAnyDataCallActive)
                            ? DataCallResponse.LINK_STATUS_ACTIVE
                            : DataCallResponse.LINK_STATUS_DORMANT;
            if (mPhysicalLinkStatus != physicalLinkStatus) {
                mPhysicalLinkStatus = physicalLinkStatus;
                mPhysicalLinkStatusChangedRegistrants.notifyResult(mPhysicalLinkStatus);
            }
            if (isAnyDataCallDormant && !isAnyDataCallActive) {
                // There is no way to indicate link activity per APN right now. So
                // Link Activity will be considered dormant only when all data calls
                // are dormant.
                // If a single data call is in dormant state and none of the data
                // calls are active broadcast overall link status as dormant.
                if (DBG) {
                    log("onDataStateChanged: Data activity DORMANT. stopNetStatePoll");
                }
                mDct.sendStopNetStatPoll(DctConstants.Activity.DORMANT);
            } else {
                if (DBG) {
                    log("onDataStateChanged: Data Activity updated to NONE. "
                            + "isAnyDataCallActive = " + isAnyDataCallActive
                            + " isAnyDataCallDormant = " + isAnyDataCallDormant);
                }
                if (isAnyDataCallActive) {
                    mDct.sendStartNetStatPoll(DctConstants.Activity.NONE);
                }
            }
        }

        if (DBG) {
            log("onDataStateChanged: dcsToRetry=" + dcsToRetry
                    + " apnsToCleanup=" + apnsToCleanup);
        }

        // Cleanup connections that have changed
        for (ApnContext apnContext : apnsToCleanup) {
            mDct.cleanUpConnection(apnContext);
        }

        // Retry connections that have disappeared
        for (DataConnection dc : dcsToRetry) {
            if (DBG) log("onDataStateChanged: send EVENT_LOST_CONNECTION dc.mTag=" + dc.mTag);
            dc.sendMessage(DataConnection.EVENT_LOST_CONNECTION, dc.mTag);
        }

        if (VDBG) log("onDataStateChanged: X");
    }

    /**
     * Register for physical link status (i.e. RRC state) changed event.
     * if {@link CarrierConfigManager#KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL} is true,
     * then physical link status is focusing on "internet data connection" instead of RRC state.
     * @param h The handler
     * @param what The event
     */
    @VisibleForTesting
    public void registerForPhysicalLinkStatusChanged(Handler h, int what) {
        mPhysicalLinkStatusChangedRegistrants.addUnique(h, what, null);
    }

    /**
     * Unregister from physical link status (i.e. RRC state) changed event.
     *
     * @param h The previously registered handler
     */
    void unregisterForPhysicalLinkStatusChanged(Handler h) {
        mPhysicalLinkStatusChangedRegistrants.remove(h);
    }

    private void log(String s) {
        Rlog.d(mTag, s);
    }

    private void loge(String s) {
        Rlog.e(mTag, s);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        synchronized (mDcListAll) {
            sb.append("mDcListAll=").append(mDcListAll)
                    .append(" mDcListActiveByCid=").append(mDcListActiveByCid);
        }
        synchronized (mTrafficDescriptorsByCid) {
            sb.append("mTrafficDescriptorsByCid=").append(mTrafficDescriptorsByCid);
        }
        return sb.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(" mPhone=" + mPhone);
        synchronized (mDcListAll) {
            pw.println(" mDcListAll=" + mDcListAll);
            pw.println(" mDcListActiveByCid=" + mDcListActiveByCid);
        }
        synchronized (mTrafficDescriptorsByCid) {
            pw.println(" mTrafficDescriptorsByCid=" + mTrafficDescriptorsByCid);
        }
    }
}
