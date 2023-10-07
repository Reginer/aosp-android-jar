/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.Manifest;
import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.telephony.CellBroadcastService;
import android.telephony.ICellBroadcastService;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;

import com.android.cellbroadcastservice.CellBroadcastStatsLog;
import com.android.internal.telephony.cdma.SmsMessage;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

/**
 * Manages a single binding to the CellBroadcastService from the platform. In mSIM cases callers
 * should have one CellBroadcastServiceManager per phone, and the CellBroadcastServiceManager
 * will handle the single binding.
 */
public class CellBroadcastServiceManager {

    private static final String TAG = "CellBroadcastServiceManager";

    private String mCellBroadcastServicePackage;
    private static CellBroadcastServiceConnection sServiceConnection;
    private Handler mModuleCellBroadcastHandler = null;

    private Phone mPhone;
    private Context mContext;

    private final LocalLog mLocalLog = new LocalLog(64);

    /** New SMS cell broadcast received as an AsyncResult. */
    private static final int EVENT_NEW_GSM_SMS_CB = 0;
    private static final int EVENT_NEW_CDMA_SMS_CB = 1;
    private static final int EVENT_NEW_CDMA_SCP_MESSAGE = 2;
    private boolean mEnabled = false;

    public CellBroadcastServiceManager(Context context, Phone phone) {
        Log.d(TAG, "CellBroadcastServiceManager created for phone " + phone.getPhoneId());
        mContext = context;
        mPhone = phone;
    }

    private boolean cbMessagesDisabledByOem() {
        if (mContext != null && mContext.getResources() != null) {
            return mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_disable_all_cb_messages);
        } else {
            return false;
        }
    }

    /**
     * Send a GSM CB message to the CellBroadcastServiceManager's handler.
     * @param m the message
     */
    public void sendGsmMessageToHandler(Message m) {
        if (cbMessagesDisabledByOem()) {
            Log.d(TAG, "GSM CB message ignored - CB messages disabled by OEM.");
            CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_FILTERED,
                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_FILTERED__TYPE__GSM,
                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_FILTERED__FILTER__DISABLED_BY_OEM);
            return;
        }
        m.what = EVENT_NEW_GSM_SMS_CB;
        mModuleCellBroadcastHandler.sendMessage(m);
    }

    /**
     * Send a CDMA CB message to the CellBroadcastServiceManager's handler.
     * @param sms the SmsMessage to forward
     */
    public void sendCdmaMessageToHandler(SmsMessage sms) {
        if (cbMessagesDisabledByOem()) {
            Log.d(TAG, "CDMA CB message ignored - CB messages disabled by OEM.");
            CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_FILTERED,
                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_FILTERED__TYPE__CDMA,
                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_FILTERED__FILTER__DISABLED_BY_OEM);
            return;
        }
        Message m = Message.obtain();
        m.what = EVENT_NEW_CDMA_SMS_CB;
        m.obj = sms;
        mModuleCellBroadcastHandler.sendMessage(m);
    }

    /**
     * Send a CDMA Service Category Program message to the CellBroadcastServiceManager's handler.
     * @param sms the SCP message
     */
    public void sendCdmaScpMessageToHandler(SmsMessage sms, RemoteCallback callback) {
        if (cbMessagesDisabledByOem()) {
            Log.d(TAG, "CDMA SCP CB message ignored - CB messages disabled by OEM.");
            CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_FILTERED,
                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_FILTERED__TYPE__CDMA_SPC,
                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_FILTERED__FILTER__DISABLED_BY_OEM);
            return;
        }
        Message m = Message.obtain();
        m.what = EVENT_NEW_CDMA_SCP_MESSAGE;
        m.obj = Pair.create(sms, callback);
        mModuleCellBroadcastHandler.sendMessage(m);
    }

    /**
     * Enable the CB module. The CellBroadcastService will be bound to and CB messages from the
     * RIL will be forwarded to the module.
     */
    public void enable() {
        initCellBroadcastServiceModule();
    }

    /**
     * Disable the CB module. The manager's handler will no longer receive CB messages from the RIL.
     */
    public void disable() {
        if (mEnabled == false) {
            return;
        }
        mEnabled = false;
        mPhone.mCi.unSetOnNewGsmBroadcastSms(mModuleCellBroadcastHandler);
        if (sServiceConnection.mService != null) {
            mContext.unbindService(sServiceConnection);
        }
    }

    /**
     * The CellBroadcastServiceManager binds to an implementation of the CellBroadcastService
     * specified in com.android.internal.R.string.cellbroadcast_default_package (typically the
     * DefaultCellBroadcastService) and forwards cell broadcast messages to the service.
     */
    private void initCellBroadcastServiceModule() {
        mEnabled = true;
        if (sServiceConnection == null) {
            sServiceConnection = new CellBroadcastServiceConnection();
        }
        mCellBroadcastServicePackage = getCellBroadcastServicePackage();
        if (mCellBroadcastServicePackage != null) {
            mModuleCellBroadcastHandler = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (!mEnabled) {
                        Log.d(TAG, "CB module is disabled.");
                        return;
                    }
                    if (sServiceConnection.mService == null) {
                        final String errorMessage = "sServiceConnection.mService is null, ignoring message.";
                        Log.d(TAG, errorMessage);
                        CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_ERROR,
                                CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_ERROR__TYPE__NO_CONNECTION_TO_CB_SERVICE,
                                errorMessage);
                        return;
                    }
                    try {
                        ICellBroadcastService cellBroadcastService =
                                ICellBroadcastService.Stub.asInterface(
                                        sServiceConnection.mService);
                        if (msg.what == EVENT_NEW_GSM_SMS_CB) {
                            mLocalLog.log("GSM SMS CB for phone " + mPhone.getPhoneId());
                            CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_REPORTED,
                                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_REPORTED__TYPE__GSM,
                                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_REPORTED__SOURCE__FRAMEWORK);
                            cellBroadcastService.handleGsmCellBroadcastSms(mPhone.getPhoneId(),
                                    (byte[]) ((AsyncResult) msg.obj).result);
                        } else if (msg.what == EVENT_NEW_CDMA_SMS_CB) {
                            mLocalLog.log("CDMA SMS CB for phone " + mPhone.getPhoneId());
                            SmsMessage sms = (SmsMessage) msg.obj;
                            CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_REPORTED,
                                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_REPORTED__TYPE__CDMA,
                                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_REPORTED__SOURCE__FRAMEWORK);
                            cellBroadcastService.handleCdmaCellBroadcastSms(mPhone.getPhoneId(),
                                    sms.getEnvelopeBearerData(), sms.getEnvelopeServiceCategory());
                        } else if (msg.what == EVENT_NEW_CDMA_SCP_MESSAGE) {
                            mLocalLog.log("CDMA SCP message for phone " + mPhone.getPhoneId());
                            CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_REPORTED,
                                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_REPORTED__TYPE__CDMA_SPC,
                                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_REPORTED__SOURCE__FRAMEWORK);
                            Pair<SmsMessage, RemoteCallback> smsAndCallback =
                                    (Pair<SmsMessage, RemoteCallback>) msg.obj;
                            SmsMessage sms = smsAndCallback.first;
                            RemoteCallback callback = smsAndCallback.second;
                            cellBroadcastService.handleCdmaScpMessage(mPhone.getPhoneId(),
                                    sms.getSmsCbProgramData(),
                                    sms.getOriginatingAddress(),
                                    callback);
                        }
                    } catch (RemoteException e) {
                        final String errorMessage = "Failed to connect to default app: "
                                + mCellBroadcastServicePackage + " err: " + e.toString();
                        Log.e(TAG, errorMessage);
                        mLocalLog.log(errorMessage);
                        CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_ERROR,
                                CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_ERROR__TYPE__NO_CONNECTION_TO_CB_SERVICE,
                                errorMessage);
                        mContext.unbindService(sServiceConnection);
                        sServiceConnection = null;
                    }
                }
            };

            Intent intent = new Intent(CellBroadcastService.CELL_BROADCAST_SERVICE_INTERFACE);
            intent.setPackage(mCellBroadcastServicePackage);
            if (sServiceConnection.mService == null) {
                boolean serviceWasBound = mContext.bindService(intent, sServiceConnection,
                        Context.BIND_AUTO_CREATE);
                Log.d(TAG, "serviceWasBound=" + serviceWasBound);
                if (!serviceWasBound) {
                    final String errorMessage = "Unable to bind to service";
                    Log.e(TAG, errorMessage);
                    mLocalLog.log(errorMessage);
                    CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_ERROR,
                            CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_ERROR__TYPE__NO_CONNECTION_TO_CB_SERVICE,
                            errorMessage);
                    return;
                }
            } else {
                Log.d(TAG, "skipping bindService because connection already exists");
            }
            mPhone.mCi.setOnNewGsmBroadcastSms(mModuleCellBroadcastHandler, EVENT_NEW_GSM_SMS_CB,
                    null);
        } else {
            final String errorMessage = "Unable to bind service; no cell broadcast service found";
            Log.e(TAG, errorMessage);
            mLocalLog.log(errorMessage);
            CellBroadcastStatsLog.write(CellBroadcastStatsLog.CB_MESSAGE_ERROR,
                    CellBroadcastStatsLog.CELL_BROADCAST_MESSAGE_ERROR__TYPE__NO_CONNECTION_TO_CB_SERVICE,
                    errorMessage);
        }
    }

    /** Returns the package name of the cell broadcast service, or null if there is none. */
    private String getCellBroadcastServicePackage() {
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> cbsPackages = packageManager.queryIntentServices(
                new Intent(CellBroadcastService.CELL_BROADCAST_SERVICE_INTERFACE),
                PackageManager.MATCH_SYSTEM_ONLY);
        if (cbsPackages.size() != 1) {
            Log.e(TAG, "getCellBroadcastServicePackageName: found " + cbsPackages.size()
                    + " CBS packages");
        }
        for (ResolveInfo info : cbsPackages) {
            if (info.serviceInfo == null) continue;
            String packageName = info.serviceInfo.packageName;
            if (!TextUtils.isEmpty(packageName)) {
                if (packageManager.checkPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
                        packageName) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "getCellBroadcastServicePackageName: " + packageName);
                    return packageName;
                } else {
                    Log.e(TAG, "getCellBroadcastServicePackageName: " + packageName
                            + " does not have READ_PRIVILEGED_PHONE_STATE permission");
                }
            } else {
                Log.e(TAG, "getCellBroadcastServicePackageName: found a CBS package but "
                        + "packageName is null/empty");
            }
        }
        Log.e(TAG, "getCellBroadcastServicePackageName: package name not found");
        return null;
    }

    private class CellBroadcastServiceConnection implements ServiceConnection {
        IBinder mService;

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "connected to CellBroadcastService");
            this.mService = service;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "mICellBroadcastService has disconnected unexpectedly");
            this.mService = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG, "Binding died");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.d(TAG, "Null binding");
        }
    }

    /**
     * Triggered with `adb shell dumpsys isms`
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CellBroadcastServiceManager:");
        pw.println(" mEnabled=" + mEnabled);
        pw.println(" mCellBroadcastServicePackage=" + mCellBroadcastServicePackage);
        if (mEnabled) {
            try {
                if (sServiceConnection != null && sServiceConnection.mService != null) {
                    sServiceConnection.mService.dump(fd, args);
                } else {
                    pw.println(" sServiceConnection is null");
                }
            } catch (RemoteException e) {
                pw.println(" mService.dump() threw RemoteException e: " + e.toString());
            }
        }
        mLocalLog.dump(fd, pw, args);
        pw.flush();
    }
}
