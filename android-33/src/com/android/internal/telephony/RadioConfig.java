/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.telephony.PhoneCapability.DEVICE_NR_CAPABILITY_NSA;
import static android.telephony.PhoneCapability.DEVICE_NR_CAPABILITY_SA;

import static com.android.internal.telephony.RILConstants.RADIO_NOT_AVAILABLE;
import static com.android.internal.telephony.RILConstants.REQUEST_NOT_SUPPORTED;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_HAL_DEVICE_CAPABILITIES;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_PHONE_CAPABILITY;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_GET_SLOT_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SET_PREFERRED_DATA_MODEM;
import static com.android.internal.telephony.RILConstants.RIL_REQUEST_SWITCH_DUAL_SIM_CONFIG;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Registrant;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.WorkSource;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotMapping;
import android.util.SparseArray;

import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides wrapper APIs for IRadioConfig interface.
 */
public class RadioConfig extends Handler {
    private static final String TAG = "RadioConfig";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; //STOPSHIP if true
    private static final Object sLock = new Object();

    static final int EVENT_HIDL_SERVICE_DEAD = 1;
    static final int EVENT_AIDL_SERVICE_DEAD = 2;
    static final HalVersion RADIO_CONFIG_HAL_VERSION_UNKNOWN = new HalVersion(-1, -1);
    static final HalVersion RADIO_CONFIG_HAL_VERSION_1_0 = new HalVersion(1, 0);
    static final HalVersion RADIO_CONFIG_HAL_VERSION_1_1 = new HalVersion(1, 1);
    static final HalVersion RADIO_CONFIG_HAL_VERSION_1_3 = new HalVersion(1, 3);
    static final HalVersion RADIO_CONFIG_HAL_VERSION_2_0 = new HalVersion(2, 0);

    private final boolean mIsMobileNetworkSupported;
    private final SparseArray<RILRequest> mRequestList = new SparseArray<>();
    /* default work source which will blame phone process */
    private final WorkSource mDefaultWorkSource;
    private final int[] mDeviceNrCapabilities;
    private final AtomicLong mRadioConfigProxyCookie = new AtomicLong(0);
    private final RadioConfigProxy mRadioConfigProxy;
    private MockModem mMockModem;
    private static Context sContext;

    private static RadioConfig sRadioConfig;

    protected Registrant mSimSlotStatusRegistrant;

    private boolean isMobileDataCapable(Context context) {
        final TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        return tm != null && tm.isDataCapable();
    }

    private RadioConfig(Context context, HalVersion radioHalVersion) {
        mIsMobileNetworkSupported = isMobileDataCapable(context);
        mRadioConfigProxy = new RadioConfigProxy(this, radioHalVersion);
        mDefaultWorkSource = new WorkSource(context.getApplicationInfo().uid,
                context.getPackageName());

        boolean is5gStandalone = context.getResources().getBoolean(
                com.android.internal.R.bool.config_telephony5gStandalone);
        boolean is5gNonStandalone = context.getResources().getBoolean(
                com.android.internal.R.bool.config_telephony5gNonStandalone);

        if (!is5gStandalone && !is5gNonStandalone) {
            mDeviceNrCapabilities = new int[0];
        } else {
            List<Integer> list = new ArrayList<>();
            if (is5gNonStandalone) {
                list.add(DEVICE_NR_CAPABILITY_NSA);
            }
            if (is5gStandalone) {
                list.add(DEVICE_NR_CAPABILITY_SA);
            }
            mDeviceNrCapabilities = list.stream().mapToInt(Integer::valueOf).toArray();
        }
    }

    /**
     * Returns the singleton static instance of RadioConfig
     */
    public static RadioConfig getInstance() {
        synchronized (sLock) {
            if (sRadioConfig == null) {
                throw new RuntimeException(
                        "RadioConfig.getInstance can't be called before make()");
            }
            return sRadioConfig;
        }
    }

    /**
     * Makes the radio config based on the context and the radio hal version passed in
     */
    public static RadioConfig make(Context c, HalVersion radioHalVersion) {
        synchronized (sLock) {
            if (sRadioConfig != null) {
                throw new RuntimeException("RadioConfig.make() should only be called once");
            }
            sContext = c;
            sRadioConfig = new RadioConfig(c, radioHalVersion);
            return sRadioConfig;
        }
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == EVENT_HIDL_SERVICE_DEAD) {
            logd("handleMessage: EVENT_HIDL_SERVICE_DEAD cookie = " + message.obj
                    + " mRadioConfigProxyCookie = " + mRadioConfigProxyCookie.get());
            if ((long) message.obj == mRadioConfigProxyCookie.get()) {
                resetProxyAndRequestList("EVENT_HIDL_SERVICE_DEAD", null);
            }
        } else if (message.what == EVENT_AIDL_SERVICE_DEAD) {
            logd("handleMessage: EVENT_AIDL_SERVICE_DEAD mRadioConfigProxyCookie = "
                    + mRadioConfigProxyCookie.get());
            resetProxyAndRequestList("EVENT_AIDL_SERVICE_DEAD", null);
        }
    }

    /**
     * Release each request in mRequestList then clear the list
     * @param error is the RIL_Errno sent back
     * @param loggable true means to print all requests in mRequestList
     */
    private void clearRequestList(int error, boolean loggable) {
        RILRequest rr;
        synchronized (mRequestList) {
            int count = mRequestList.size();
            if (DBG && loggable) {
                logd("clearRequestList: mRequestList=" + count);
            }

            for (int i = 0; i < count; i++) {
                rr = mRequestList.valueAt(i);
                if (DBG && loggable) {
                    logd(i + ": [" + rr.mSerial + "] " + RILUtils.requestToString(rr.mRequest));
                }
                rr.onError(error, null);
                rr.release();
            }
            mRequestList.clear();
        }
    }

    private void resetProxyAndRequestList(String caller, Exception e) {
        loge(caller + ": " + e);
        mRadioConfigProxy.clear();

        // increment the cookie so that death notification can be ignored
        mRadioConfigProxyCookie.incrementAndGet();

        RILRequest.resetSerial();
        // Clear request list on close
        clearRequestList(RADIO_NOT_AVAILABLE, false);

        getRadioConfigProxy(null);
    }

    /**
     * Returns a holder that has either:
     * - getV1() -> {@link android.hardware.radio.config.V1_0.IRadioConfig}
     * - getV2() -> {@link android.hardware.radio.config.IRadioConfig}
     * that returns corresponding hal implementation
     */
    public RadioConfigProxy getRadioConfigProxy(Message result) {
        if (!mIsMobileNetworkSupported) {
            if (VDBG) logd("getRadioConfigProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
            mRadioConfigProxy.clear();
            return mRadioConfigProxy;
        }

        if (!mRadioConfigProxy.isEmpty()) {
            return mRadioConfigProxy;
        }

        updateRadioConfigProxy();

        if (mRadioConfigProxy.isEmpty() && result != null) {
            AsyncResult.forMessage(
                    result, null, CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
            result.sendToTarget();
        }

        return mRadioConfigProxy;
    }

    /**
     * Request to enable/disable the mock modem service.
     * This is invoked from shell commands during CTS testing only.
     *
     * @param serviceName the service name we want to bind to
     */
    public boolean setModemService(String serviceName) {
        boolean serviceBound = true;

        if (serviceName != null) {
            logd("Overriding connected service to MockModemService");
            mMockModem = null;

            mMockModem = new MockModem(sContext, serviceName);
            if (mMockModem == null) {
                loge("MockModem creation failed.");
                return false;
            }

            mMockModem.bindToMockModemService(MockModem.RADIOCONFIG_SERVICE);

            int retryCount = 0;
            IBinder binder;
            do {
                binder = mMockModem.getServiceBinder(MockModem.RADIOCONFIG_SERVICE);

                retryCount++;
                if (binder == null) {
                    logd("Retry(" + retryCount + ") Mock RadioConfig");
                    try {
                        Thread.sleep(MockModem.BINDER_RETRY_MILLIS);
                    } catch (InterruptedException e) {
                    }
                }
            } while ((binder == null) && (retryCount < MockModem.BINDER_MAX_RETRY));

            if (binder == null) {
                loge("Mock RadioConfig bind fail");
                serviceBound = false;
            }

            if (serviceBound) resetProxyAndRequestList("EVENT_HIDL_SERVICE_DEAD", null);
        }

        if ((serviceName == null) || (!serviceBound)) {
            if (serviceBound) logd("Unbinding to mock RadioConfig service");

            if (mMockModem != null) {
                mMockModem = null;
                resetProxyAndRequestList("EVENT_AIDL_SERVICE_DEAD", null);
            }
        }

        return serviceBound;
    }

    private void updateRadioConfigProxy() {
        IBinder service;
        if (mMockModem == null) {
            service = ServiceManager.waitForDeclaredService(
                android.hardware.radio.config.IRadioConfig.DESCRIPTOR + "/default");
        } else {
            // Binds to Mock RadioConfig Service
            service = mMockModem.getServiceBinder(MockModem.RADIOCONFIG_SERVICE);
        }

        if (service != null) {
            mRadioConfigProxy.setAidl(
                    RADIO_CONFIG_HAL_VERSION_2_0,
                    android.hardware.radio.config.IRadioConfig.Stub.asInterface(service));
        }

        if (mRadioConfigProxy.isEmpty()) {
            try {
                mRadioConfigProxy.setHidl(RADIO_CONFIG_HAL_VERSION_1_3,
                        android.hardware.radio.config.V1_3.IRadioConfig.getService(true));
            } catch (RemoteException | NoSuchElementException e) {
                mRadioConfigProxy.clear();
                loge("getHidlRadioConfigProxy1_3: RadioConfigProxy getService: " + e);
            }
        }

        if (mRadioConfigProxy.isEmpty()) {
            try {
                mRadioConfigProxy.setHidl(RADIO_CONFIG_HAL_VERSION_1_1,
                        android.hardware.radio.config.V1_1.IRadioConfig.getService(true));
            } catch (RemoteException | NoSuchElementException e) {
                mRadioConfigProxy.clear();
                loge("getHidlRadioConfigProxy1_1: RadioConfigProxy getService | linkToDeath: " + e);
            }
        }

        if (mRadioConfigProxy.isEmpty()) {
            try {
                mRadioConfigProxy.setHidl(RADIO_CONFIG_HAL_VERSION_1_0,
                        android.hardware.radio.config.V1_0.IRadioConfig.getService(true));
            } catch (RemoteException | NoSuchElementException e) {
                mRadioConfigProxy.clear();
                loge("getHidlRadioConfigProxy1_0: RadioConfigProxy getService | linkToDeath: " + e);
            }
        }

        if (!mRadioConfigProxy.isEmpty()) {
            try {
                mRadioConfigProxy.linkToDeath(mRadioConfigProxyCookie.incrementAndGet());
                mRadioConfigProxy.setResponseFunctions(this);
                return;
            } catch (RemoteException e) {
                mRadioConfigProxy.clear();
                loge("RadioConfigProxy: failed to linkToDeath() or setResponseFunction()");
            }
        }

        loge("getRadioConfigProxy: mRadioConfigProxy == null");
    }

    private RILRequest obtainRequest(int request, Message result, WorkSource workSource) {
        RILRequest rr = RILRequest.obtain(request, result, workSource);
        synchronized (mRequestList) {
            mRequestList.append(rr.mSerial, rr);
        }
        return rr;
    }

    private RILRequest findAndRemoveRequestFromList(int serial) {
        RILRequest rr;
        synchronized (mRequestList) {
            rr = mRequestList.get(serial);
            if (rr != null) {
                mRequestList.remove(serial);
            }
        }

        return rr;
    }

    /**
     * This is a helper function to be called when a RadioConfigResponse callback is called.
     * It finds and returns RILRequest corresponding to the response if one is found.
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    public RILRequest processResponse(android.hardware.radio.RadioResponseInfo responseInfo) {
        int serial = responseInfo.serial;
        int error = responseInfo.error;
        int type = responseInfo.type;

        if (type != android.hardware.radio.RadioResponseType.SOLICITED) {
            loge("processResponse: Unexpected response type " + type);
        }

        RILRequest rr = findAndRemoveRequestFromList(serial);
        if (rr == null) {
            loge("processResponse: Unexpected response! serial: " + serial + " error: " + error);
            return null;
        }

        return rr;
    }

    /**
     * This is a helper function to be called when a RadioConfigResponse callback is called.
     * It finds and returns RILRequest corresponding to the response if one is found.
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    public RILRequest processResponse(android.hardware.radio.V1_0.RadioResponseInfo responseInfo) {
        int serial = responseInfo.serial;
        int error = responseInfo.error;
        int type = responseInfo.type;

        if (type != android.hardware.radio.RadioResponseType.SOLICITED) {
            loge("processResponse: Unexpected response type " + type);
        }

        RILRequest rr = findAndRemoveRequestFromList(serial);
        if (rr == null) {
            loge("processResponse: Unexpected response! serial: " + serial + " error: " + error);
            return null;
        }

        return rr;
    }

    /**
     * This is a helper function to be called when a RadioConfigResponse callback is called.
     * It finds and returns RILRequest corresponding to the response if one is found.
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    public RILRequest processResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo) {
        int serial = responseInfo.serial;
        int error = responseInfo.error;
        int type = responseInfo.type;
        if (type != android.hardware.radio.RadioResponseType.SOLICITED) {
            loge("processResponse: Unexpected response type " + type);
        }

        RILRequest rr = findAndRemoveRequestFromList(serial);
        if (rr == null) {
            loge("processResponse: Unexpected response! serial: " + serial + " error: " + error);
            return null;
        }

        return rr;
    }

    /**
     * Wrapper function for IRadioConfig.getSimSlotsStatus().
     */
    public void getSimSlotsStatus(Message result) {
        RadioConfigProxy proxy = getRadioConfigProxy(result);
        if (proxy.isEmpty()) return;

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SLOT_STATUS, result, mDefaultWorkSource);
        if (DBG) {
            logd(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }
        try {
            proxy.getSimSlotStatus(rr.mSerial);
        } catch (RemoteException | RuntimeException e) {
            resetProxyAndRequestList("getSimSlotsStatus", e);
        }
    }

    /**
     * Wrapper function for IRadioConfig.setPreferredDataModem(int modemId).
     */
    public void setPreferredDataModem(int modemId, Message result) {
        RadioConfigProxy proxy = getRadioConfigProxy(null);
        if (proxy.isEmpty()) return;

        if (!isSetPreferredDataCommandSupported()) {
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_PREFERRED_DATA_MODEM,
                result, mDefaultWorkSource);
        if (DBG) {
            logd(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }
        try {
            proxy.setPreferredDataModem(rr.mSerial, modemId);
        } catch (RemoteException | RuntimeException e) {
            resetProxyAndRequestList("setPreferredDataModem", e);
        }
    }

    /**
     * Wrapper function for IRadioConfig.getPhoneCapability().
     */
    public void getPhoneCapability(Message result) {
        RadioConfigProxy proxy = getRadioConfigProxy(null);
        if (proxy.isEmpty()) return;

        if (proxy.getVersion().less(RADIO_CONFIG_HAL_VERSION_1_1)) {
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_PHONE_CAPABILITY, result, mDefaultWorkSource);
        if (DBG) {
            logd(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }
        try {
            proxy.getPhoneCapability(rr.mSerial);
        } catch (RemoteException | RuntimeException e) {
            resetProxyAndRequestList("getPhoneCapability", e);
        }
    }

    /**
     * @return whether current radio config version supports SET_PREFERRED_DATA_MODEM command.
     * If yes, we'll use RIL_REQUEST_SET_PREFERRED_DATA_MODEM to indicate which modem is preferred.
     * If not, we shall use RIL_REQUEST_ALLOW_DATA for on-demand PS attach / detach.
     * See PhoneSwitcher for more details.
     */
    public boolean isSetPreferredDataCommandSupported() {
        RadioConfigProxy proxy = getRadioConfigProxy(null);
        return !proxy.isEmpty() && proxy.getVersion().greaterOrEqual(RADIO_CONFIG_HAL_VERSION_1_1);
    }

    /**
     * Wrapper function for IRadioConfig.setSimSlotsMapping(int32_t serial, vec<uint32_t> slotMap).
     */
    public void setSimSlotsMapping(List<UiccSlotMapping> slotMapping, Message result) {
        RadioConfigProxy proxy = getRadioConfigProxy(result);
        if (proxy.isEmpty()) return;

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING, result,
                mDefaultWorkSource);
        if (DBG) {
            logd(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest) + " "
                    + slotMapping);
        }
        try {
            proxy.setSimSlotsMapping(rr.mSerial, slotMapping);
        } catch (RemoteException | RuntimeException e) {
            resetProxyAndRequestList("setSimSlotsMapping", e);
        }
    }

    /**
     * Wrapper function for using IRadioConfig.setNumOfLiveModems(int32_t serial,
     * byte numOfLiveModems) to switch between single-sim and multi-sim.
     */
    public void setNumOfLiveModems(int numOfLiveModems, Message result) {
        RadioConfigProxy proxy = getRadioConfigProxy(result);
        if (proxy.isEmpty()) return;

        if (proxy.getVersion().less(RADIO_CONFIG_HAL_VERSION_1_1)) {
            if (result != null) {
                AsyncResult.forMessage(
                        result, null, CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SWITCH_DUAL_SIM_CONFIG,
                result, mDefaultWorkSource);
        if (DBG) {
            logd(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + ", numOfLiveModems = " + numOfLiveModems);
        }
        try {
            proxy.setNumOfLiveModems(rr.mSerial, numOfLiveModems);
        } catch (RemoteException | RuntimeException e) {
            resetProxyAndRequestList("setNumOfLiveModems", e);
        }
    }

    /**
     * Register a handler to get SIM slot status changed notifications.
     */
    public void registerForSimSlotStatusChanged(Handler h, int what, Object obj) {
        mSimSlotStatusRegistrant = new Registrant(h, what, obj);
    }

    /**
     * Unregister corresponding to registerForSimSlotStatusChanged().
     */
    public void unregisterForSimSlotStatusChanged(Handler h) {
        if (mSimSlotStatusRegistrant != null && mSimSlotStatusRegistrant.getHandler() == h) {
            mSimSlotStatusRegistrant.clear();
            mSimSlotStatusRegistrant = null;
        }
    }

    /**
     * Gets the hal capabilities from the device.
     */
    public void getHalDeviceCapabilities(Message result) {
        RadioConfigProxy proxy = getRadioConfigProxy(Message.obtain(result));
        if (proxy.isEmpty()) return;

        if (proxy.getVersion().less(RADIO_CONFIG_HAL_VERSION_1_3)) {
            if (result != null) {
                if (DBG) {
                    logd("RIL_REQUEST_GET_HAL_DEVICE_CAPABILITIES > REQUEST_NOT_SUPPORTED");
                }
                AsyncResult.forMessage(result,
                        /* Send response such that all capabilities are supported (depending on
                           the hal version of course.) */
                        proxy.getFullCapabilitySet(),
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            } else {
                if (DBG) {
                    logd("RIL_REQUEST_GET_HAL_DEVICE_CAPABILITIES > REQUEST_NOT_SUPPORTED "
                            + "on complete message not set.");
                }
            }
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_HAL_DEVICE_CAPABILITIES,
                result, mDefaultWorkSource);
        if (DBG) {
            logd(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }
        try {
            proxy.getHalDeviceCapabilities(rr.mSerial);
        } catch (RemoteException | RuntimeException e) {
            resetProxyAndRequestList("getHalDeviceCapabilities", e);
        }
    }

    /**
     * Returns the device's nr capability.
     */
    public int[] getDeviceNrCapabilities() {
        return mDeviceNrCapabilities;
    }

    private static void logd(String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(String log) {
        Rlog.e(TAG, log);
    }
}
