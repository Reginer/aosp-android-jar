/*
 * Copyright (c) 2013 The Android Open Source Project
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

package com.android.ims;

import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RemoteException;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import android.telephony.ims.ImsUtListener;

import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener;
import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides APIs for the supplementary service settings using IMS (Ut interface).
 * It is created from 3GPP TS 24.623 (XCAP(XML Configuration Access Protocol)
 * over the Ut interface for manipulating supplementary services).
 *
 * @hide
 */
public class ImsUt implements ImsUtInterface {
    /**
     * Key string for an additional supplementary service configurations.
     */
    /**
     * Actions : string format of ImsUtInterface#ACTION_xxx
     *      "0" (deactivation), "1" (activation), "2" (not_used),
     *      "3" (registration), "4" (erasure), "5" (Interrogation)
     */
    public static final String KEY_ACTION = "action";
    /**
     * Categories :
     *      "OIP", "OIR", "TIP", "TIR", "CDIV", "CB", "CW", "CONF",
     *      "ACR", "MCID", "ECT", "CCBS", "AOC", "MWI", "FA", "CAT"
     *
     * Detailed parameter name will be determined according to the properties
     * of the supplementary service configuration.
     */
    public static final String KEY_CATEGORY = "category";
    public static final String CATEGORY_OIP = "OIP";
    public static final String CATEGORY_OIR = "OIR";
    public static final String CATEGORY_TIP = "TIP";
    public static final String CATEGORY_TIR = "TIR";
    public static final String CATEGORY_CDIV = "CDIV";
    public static final String CATEGORY_CB = "CB";
    public static final String CATEGORY_CW = "CW";
    public static final String CATEGORY_CONF = "CONF";

    private static final String TAG = "ImsUt";
    private static final boolean DBG = true;

    //These service class values are same as the one in CommandsInterface.java
    private static final int SERVICE_CLASS_NONE = 0;
    private static final int SERVICE_CLASS_VOICE = (1 << 0);

    // For synchronization of private variables
    private Object mLockObj = new Object();
    private final IImsUt miUt;
    private HashMap<Integer, Message> mPendingCmds =
            new HashMap<Integer, Message>();
    private Registrant mSsIndicationRegistrant;

    public ImsUt(IImsUt iUt) {
        miUt = iUt;

        if (miUt != null) {
            try {
                miUt.setListener(new IImsUtListenerProxy());
            } catch (RemoteException e) {
            }
        }
    }

    public void close() {
        synchronized(mLockObj) {
            if (miUt != null) {
                try {
                    miUt.close();
                } catch (RemoteException e) {
                }
            }

            if (!mPendingCmds.isEmpty()) {
                Map.Entry<Integer, Message>[] entries =
                    mPendingCmds.entrySet().toArray(new Map.Entry[mPendingCmds.size()]);

                for (Map.Entry<Integer, Message> entry : entries) {
                    sendFailureReport(entry.getValue(),
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                }

                mPendingCmds.clear();
            }
        }
    }

    /**
     * Registers a handler for Supplementary Service Indications. The
     * result is returned in the {@link AsyncResult#result) field
     * of the {@link AsyncResult} object returned by {@link Message.obj}.
     * Value of ((AsyncResult)result.obj) is of {@link ImsSsData}.
     */
    public void registerForSuppServiceIndication(Handler h, int what, Object obj) {
        mSsIndicationRegistrant = new Registrant (h, what, obj);
    }

    /**
     * UnRegisters a handler for Supplementary Service Indications.
     */
    public void unregisterForSuppServiceIndication(Handler h) {
        mSsIndicationRegistrant.clear();
    }

    /**
     * Operations for the supplementary service configuration
     */

    /**
     * Retrieves the configuration of the call barring.
     *
     * @param cbType type of call barring to be queried; ImsUtInterface#CB_XXX
     * @param result message to pass the result of this operation
     *      The return value of ((AsyncResult)result.obj) is an array of {@link ImsSsInfo}.
     * @deprecated Use {@link #queryCallBarring(int, Message, int)} instead.
     */
    @Override
    public void queryCallBarring(int cbType, Message result) {
        queryCallBarring(cbType, result, SERVICE_CLASS_NONE);
    }

    /**
     * Retrieves the configuration of the call barring for specified service class.
     *
     * @param cbType type of call barring to be queried; ImsUtInterface#CB_XXX
     * @param result message to pass the result of this operation
     *      The return value of ((AsyncResult)result.obj) is an array of {@link ImsSsInfo}.
     * @param serviceClass service class for e.g. voice/video
     */
    @Override
    public void queryCallBarring(int cbType, Message result, int serviceClass) {
        if (DBG) {
            log("queryCallBarring :: Ut=" + miUt + ", cbType=" + cbType + ", serviceClass="
                    + serviceClass);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.queryCallBarringForServiceClass(cbType, serviceClass);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Retrieves the configuration of the call forward.
     * The return value of ((AsyncResult)result.obj) is an array of {@link ImsCallForwardInfo}.
     */
    @Override
    public void queryCallForward(int condition, String number, Message result) {
        if (DBG) {
            log("queryCallForward :: Ut=" + miUt + ", condition=" + condition
                    + ", number=" + Rlog.pii(TAG, number));
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.queryCallForward(condition, number);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Retrieves the configuration of the call waiting.
     * The return value of ((AsyncResult)result.obj) is an array of {@link ImsSsInfo}.
     */
    @Override
    public void queryCallWaiting(Message result) {
        if (DBG) {
            log("queryCallWaiting :: Ut=" + miUt);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.queryCallWaiting();

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Retrieves the default CLIR setting.
     */
    @Override
    public void queryCLIR(Message result) {
        if (DBG) {
            log("queryCLIR :: Ut=" + miUt);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.queryCLIR();

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Retrieves the CLIP call setting.
     */
    public void queryCLIP(Message result) {
        if (DBG) {
            log("queryCLIP :: Ut=" + miUt);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.queryCLIP();

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Retrieves the COLR call setting.
     */
    public void queryCOLR(Message result) {
        if (DBG) {
            log("queryCOLR :: Ut=" + miUt);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.queryCOLR();

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Retrieves the COLP call setting.
     */
    public void queryCOLP(Message result) {
        if (DBG) {
            log("queryCOLP :: Ut=" + miUt);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.queryCOLP();

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Modifies the configuration of the call barring.
     * @deprecated Use {@link #updateCallBarring(int, int, Message, String[], int)} instead.
     */
    @Override
    public void updateCallBarring(int cbType, int action, Message result, String[] barrList) {
        updateCallBarring(cbType, action, result, barrList, SERVICE_CLASS_NONE);
    }

    /**
     * Modifies the configuration of the call barring for specified service class.
     * @deprecated Use {@link #updateCallBarring(int, int, Message, String[], int, String)} instead.
     */
    @Override
    public void updateCallBarring(int cbType, int action, Message result, String[] barrList,
            int serviceClass) {
        updateCallBarring(cbType, action, result, barrList, serviceClass, "");
    }

    /**
     * Modifies the configuration of the call barring for specified service class with password.
     */
    @Override
    public void updateCallBarring(int cbType, int action, Message result,
            String[] barrList, int serviceClass, String password) {
        if (DBG) {
            if (barrList != null) {
                String bList = new String();
                for (int i = 0; i < barrList.length; i++) {
                    bList.concat(barrList[i] + " ");
                }
                log("updateCallBarring :: Ut=" + miUt + ", cbType=" + cbType
                        + ", action=" + action + ", serviceClass=" + serviceClass
                        + ", barrList=" + bList);
            }
            else {
                log("updateCallBarring :: Ut=" + miUt + ", cbType=" + cbType
                        + ", action=" + action + ", serviceClass=" + serviceClass);
            }
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.updateCallBarringWithPassword(cbType, action,
                        barrList, serviceClass, password);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Modifies the configuration of the call forward.
     */
    @Override
    public void updateCallForward(int action, int condition, String number,
            int serviceClass, int timeSeconds, Message result) {
        if (DBG) {
            log("updateCallForward :: Ut=" + miUt + ", action=" + action
                    + ", condition=" + condition + ", number=" + Rlog.pii(TAG, number)
                    + ", serviceClass=" + serviceClass + ", timeSeconds=" + timeSeconds);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.updateCallForward(action, condition, number, serviceClass, timeSeconds);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Modifies the configuration of the call waiting.
     */
    @Override
    public void updateCallWaiting(boolean enable, int serviceClass, Message result) {
        if (DBG) {
            log("updateCallWaiting :: Ut=" + miUt + ", enable=" + enable
            + ",serviceClass="  + serviceClass);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.updateCallWaiting(enable, serviceClass);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Updates the configuration of the CLIR supplementary service.
     */
    @Override
    public void updateCLIR(int clirMode, Message result) {
        if (DBG) {
            log("updateCLIR :: Ut=" + miUt + ", clirMode=" + clirMode);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.updateCLIR(clirMode);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Updates the configuration of the CLIP supplementary service.
     */
    @Override
    public void updateCLIP(boolean enable, Message result) {
        if (DBG) {
            log("updateCLIP :: Ut=" + miUt + ", enable=" + enable);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.updateCLIP(enable);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Updates the configuration of the COLR supplementary service.
     */
    @Override
    public void updateCOLR(int presentation, Message result) {
        if (DBG) {
            log("updateCOLR :: Ut=" + miUt + ", presentation=" + presentation);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.updateCOLR(presentation);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * Updates the configuration of the COLP supplementary service.
     */
    @Override
    public void updateCOLP(boolean enable, Message result) {
        if (DBG) {
            log("updateCallWaiting :: Ut=" + miUt + ", enable=" + enable);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.updateCOLP(enable);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    /**
     * @return returns true if the binder is alive, false otherwise.
     */
    public boolean isBinderAlive() {
        return miUt.asBinder().isBinderAlive();
    }

    public void transact(Bundle ssInfo, Message result) {
        if (DBG) {
            log("transact :: Ut=" + miUt + ", ssInfo=" + ssInfo);
        }

        synchronized(mLockObj) {
            try {
                int id = miUt.transact(ssInfo);

                if (id < 0) {
                    sendFailureReport(result,
                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
                    return;
                }

                mPendingCmds.put(Integer.valueOf(id), result);
            } catch (RemoteException e) {
                sendFailureReport(result,
                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE, 0));
            }
        }
    }

    private void sendFailureReport(Message result, ImsReasonInfo error) {
        if (result == null || error == null) {
            return;
        }

        String errorString;
        // If ImsReasonInfo object does not have a String error code, use a
        // default error string.
        if (error.mExtraMessage == null) {
            errorString = Resources.getSystem().getString(
                    com.android.internal.R.string.mmiError);
        }
        else {
            errorString = new String(error.mExtraMessage);
        }
        AsyncResult.forMessage(result, null, new ImsException(errorString, error.mCode));
        result.sendToTarget();
    }

    private void sendSuccessReport(Message result) {
        if (result == null) {
            return;
        }

        AsyncResult.forMessage(result, null, null);
        result.sendToTarget();
    }

    private void sendSuccessReport(Message result, Object ssInfo) {
        if (result == null) {
            return;
        }

        AsyncResult.forMessage(result, ssInfo, null);
        result.sendToTarget();
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }

    /**
     * A listener type for the result of the supplementary service configuration.
     */
    @VisibleForTesting
    public class IImsUtListenerProxy extends IImsUtListener.Stub {
        /**
         * Notifies the result of the supplementary service configuration udpate.
         */
        @Override
        public void utConfigurationUpdated(IImsUt ut, int id) {
            Integer key = Integer.valueOf(id);

            synchronized(mLockObj) {
                sendSuccessReport(mPendingCmds.get(key));
                mPendingCmds.remove(key);
            }
        }

        @Override
        public void utConfigurationUpdateFailed(IImsUt ut, int id, ImsReasonInfo error) {
            Integer key = Integer.valueOf(id);

            synchronized(mLockObj) {
                sendFailureReport(mPendingCmds.get(key), error);
                mPendingCmds.remove(key);
            }
        }

        /**
         * Notifies the result of the supplementary service configuration query.
         */
        // API Deprecated, internally use new API to process query result.
        @Override
        public void utConfigurationQueried(IImsUt ut, int id, Bundle ssInfo) {
            int[] clirResponse = ssInfo.getIntArray(ImsUtListener.BUNDLE_KEY_CLIR);
            if (clirResponse != null && clirResponse.length == 2) {
                // Deprecated functionality does not use status, set as NOT_REGISTERED.
                ImsSsInfo info = new ImsSsInfo.Builder(ImsSsInfo.NOT_REGISTERED)
                        .setClirOutgoingState(clirResponse[0])
                        .setClirInterrogationStatus(clirResponse[1]).build();
                lineIdentificationSupplementaryServiceResponse(id, info);
                return;
            }
            ImsSsInfo info = ssInfo.getParcelable(ImsUtListener.BUNDLE_KEY_SSINFO);
            if (info != null) {
                lineIdentificationSupplementaryServiceResponse(id, info);
                return;
            }
            Rlog.w(TAG, "Invalid utConfigurationQueried response received for Bundle " + ssInfo);
        }

        /**
         * Notifies the result of a line identification supplementary service query.
         */
        @Override
        public void lineIdentificationSupplementaryServiceResponse(int id, ImsSsInfo config) {
            synchronized(mLockObj) {
                sendSuccessReport(mPendingCmds.get(id), config);
                mPendingCmds.remove(id);
            }
        }

        @Override
        public void utConfigurationQueryFailed(IImsUt ut, int id, ImsReasonInfo error) {
            Integer key = Integer.valueOf(id);

            synchronized(mLockObj) {
                sendFailureReport(mPendingCmds.get(key), error);
                mPendingCmds.remove(key);
            }
        }

        /**
         * Notifies the status of the call barring supplementary service.
         */
        @Override
        public void utConfigurationCallBarringQueried(IImsUt ut,
                int id, ImsSsInfo[] cbInfo) {
            Integer key = Integer.valueOf(id);

            synchronized(mLockObj) {
                sendSuccessReport(mPendingCmds.get(key), cbInfo);
                mPendingCmds.remove(key);
            }
        }

        /**
         * Notifies the status of the call forwarding supplementary service.
         */
        @Override
        public void utConfigurationCallForwardQueried(IImsUt ut,
                int id, ImsCallForwardInfo[] cfInfo) {
            Integer key = Integer.valueOf(id);

            synchronized(mLockObj) {
                sendSuccessReport(mPendingCmds.get(key), cfInfo);
                mPendingCmds.remove(key);
            }
        }

        /**
         * Notifies the status of the call waiting supplementary service.
         */
        @Override
        public void utConfigurationCallWaitingQueried(IImsUt ut,
                int id, ImsSsInfo[] cwInfo) {
            Integer key = Integer.valueOf(id);

            synchronized(mLockObj) {
                sendSuccessReport(mPendingCmds.get(key), cwInfo);
                mPendingCmds.remove(key);
            }
        }

        /**
         * Notifies client when Supplementary Service indication is received
         */
        @Override
        public void onSupplementaryServiceIndication(ImsSsData ssData) {
            if (mSsIndicationRegistrant != null) {
                mSsIndicationRegistrant.notifyResult(ssData);
            }
        }
    }
}
