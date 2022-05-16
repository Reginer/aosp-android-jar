/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.IBootstrapAuthenticationCallback;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gba.GbaAuthRequest;
import android.telephony.gba.GbaService;
import android.telephony.gba.IGbaService;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.metrics.RcsStats;
import com.android.telephony.Rlog;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class that serves as the layer between GbaService and ServiceStateTracker. It helps binding,
 * sending request, receiving callback, and registering for state change to GbaService.
 */
public class GbaManager {
    private static final boolean DBG = Build.IS_DEBUGGABLE;
    private static final int EVENT_BIND_SERVICE = 1;
    private static final int EVENT_UNBIND_SERVICE = 2;
    private static final int EVENT_BIND_FAIL = 3;
    private static final int EVENT_BIND_SUCCESS = 4;
    private static final int EVENT_CONFIG_CHANGED = 5;
    private static final int EVENT_REQUESTS_RECEIVED = 6;

    @VisibleForTesting
    public static final int RETRY_TIME_MS = 3000;
    @VisibleForTesting
    public static final int MAX_RETRY = 5;
    @VisibleForTesting
    public static final int REQUEST_TIMEOUT_MS = 5000;
    private final RcsStats mRcsStats;

    private final String mLogTag;
    private final Context mContext;
    private final int mSubId;

    private IGbaService mIGbaService;
    private GbaDeathRecipient mDeathRecipient;
    private String mTargetBindingPackageName;
    private GbaServiceConnection mServiceConnection;
    private Handler mHandler;

    private String mServicePackageName;
    private String mServicePackageNameOverride;
    private int mReleaseTime;
    private int mRetryTimes = 0;

    //the requests to be sent to the GBA service
    private final ConcurrentLinkedQueue<GbaAuthRequest> mRequestQueue =
            new ConcurrentLinkedQueue<>();
    //the callbacks of the pending requests which have been sent to the GBA service
    private final SparseArray<IBootstrapAuthenticationCallback> mCallbacks = new SparseArray<>();

    private static final SparseArray<GbaManager> sGbaManagers = new SparseArray<>();

    private final class GbaManagerHandler extends Handler {
        GbaManagerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            logv("handle msg:" + msg.what);
            switch (msg.what) {
                case EVENT_BIND_SERVICE:
                    if (mRetryTimes++ < MAX_RETRY) {
                        rebindService(false);
                    } else {
                        loge("Too many retries, stop now!");
                        sendEmptyMessage(EVENT_BIND_FAIL);
                    }
                    break;
                case EVENT_UNBIND_SERVICE:
                    //do nothing if new requests are coming
                    if (mRequestQueue.isEmpty()) {
                        clearCallbacksAndNotifyFailure();
                        unbindService();
                    }
                    break;
                case EVENT_BIND_FAIL:
                case EVENT_BIND_SUCCESS:
                    mRetryTimes = 0;
                    processRequests();
                    break;
                case EVENT_REQUESTS_RECEIVED:
                    if (isServiceConnected()) {
                        processRequests();
                    } else {
                        if (!mHandler.hasMessages(EVENT_BIND_SERVICE)) {
                            mHandler.sendEmptyMessage(EVENT_BIND_SERVICE);
                        }
                    }
                    break;
                case EVENT_CONFIG_CHANGED:
                    mRetryTimes = 0;
                    if (isServiceConnetable() || isServiceConnected()) {
                        //force to rebind when config is changed
                        rebindService(true);
                    }
                    break;
                default:
                    loge("Unhandled event " + msg.what);
            }
        }
    }

    private final class GbaDeathRecipient implements IBinder.DeathRecipient {

        private final ComponentName mComponentName;
        private IBinder mBinder;

        GbaDeathRecipient(ComponentName name) {
            mComponentName = name;
        }

        public void linkToDeath(IBinder service) throws RemoteException {
            if (service != null) {
                mBinder = service;
                mBinder.linkToDeath(this, 0);
            }
        }

        public synchronized void unlinkToDeath() {
            if (mBinder != null) {
                mBinder.unlinkToDeath(this, 0);
                mBinder = null;
            }
        }

        @Override
        public void binderDied() {
            logd("GbaService(" + mComponentName + ") has died.");
            unlinkToDeath();
            //retry if died
            retryBind();
        }
    }

    private final class GbaServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logd("service " + name + " for Gba is connected.");
            mIGbaService = IGbaService.Stub.asInterface(service);
            mDeathRecipient = new GbaDeathRecipient(name);
            try {
                mDeathRecipient.linkToDeath(service);
            } catch (RemoteException exception) {
                // Remote exception means that the binder already died.
                mDeathRecipient.binderDied();
                logd("RemoteException " + exception);
            }
            mHandler.sendEmptyMessage(EVENT_BIND_SUCCESS);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logd("service " + name + " is now disconnected.");
            mTargetBindingPackageName = null;
        }
    }

    @VisibleForTesting
    public GbaManager(Context context, int subId, String servicePackageName, int releaseTime,
            RcsStats rcsStats) {
        mContext = context;
        mSubId = subId;
        mLogTag = "GbaManager[" + subId + "]";

        mServicePackageName = servicePackageName;
        mReleaseTime = releaseTime;

        HandlerThread headlerThread = new HandlerThread(mLogTag);
        headlerThread.start();
        mHandler = new GbaManagerHandler(headlerThread.getLooper());

        if (mReleaseTime < 0) {
            mHandler.sendEmptyMessage(EVENT_BIND_SERVICE);
        }
        mRcsStats = rcsStats;
    }

    /**
     * create a GbaManager instance for a sub
     */
    public static GbaManager make(Context context, int subId,
            String servicePackageName, int releaseTime) {
        GbaManager gm = new GbaManager(context, subId, servicePackageName, releaseTime,
                RcsStats.getInstance());
        synchronized (sGbaManagers) {
            sGbaManagers.put(subId, gm);
        }
        return gm;
    }

    /**
     * get singleton instance of GbaManager
     * @return GbaManager
     */
    public static GbaManager getInstance(int subId) {
        synchronized (sGbaManagers) {
            return sGbaManagers.get(subId);
        }
    }

    /**
     * handle the bootstrap authentication request
     * @hide
     */
    public void bootstrapAuthenticationRequest(GbaAuthRequest req) {
        logv("bootstrapAuthenticationRequest: " + req);
        //No GBA service configured
        if (TextUtils.isEmpty(getServicePackage())) {
            logd("do not support!");
            try {
                req.getCallback().onAuthenticationFailure(req.getToken(),
                        TelephonyManager.GBA_FAILURE_REASON_FEATURE_NOT_SUPPORTED);
            } catch (RemoteException exception) {
                loge("exception to call service: " + exception);
            }
            return;
        }

        mRequestQueue.offer(req);
        if (!mHandler.hasMessages(EVENT_REQUESTS_RECEIVED)) {
            mHandler.sendEmptyMessage(EVENT_REQUESTS_RECEIVED);
        }
    }

    private final IBootstrapAuthenticationCallback mServiceCallback =
            new IBootstrapAuthenticationCallback.Stub() {
                @Override
                public void onKeysAvailable(int token, byte[] gbaKey, String btId) {
                    logv("onKeysAvailable: " + Integer.toHexString(token) + ", id: " + btId);

                    IBootstrapAuthenticationCallback cb = null;
                    synchronized (mCallbacks) {
                        cb = mCallbacks.get(token);
                    }
                    if (cb != null) {
                        try {
                            cb.onKeysAvailable(token, gbaKey, btId);
                            mRcsStats.onGbaSuccessEvent(mSubId);
                        } catch (RemoteException exception) {
                            logd("RemoteException " + exception);
                        }
                        synchronized (mCallbacks) {
                            mCallbacks.remove(token);
                            if (mCallbacks.size() == 0) {
                                releaseServiceAsNeeded(0);
                            }
                        }
                    }
                }

                @Override
                public void onAuthenticationFailure(int token, int reason) {
                    logd("onAuthenticationFailure: "
                            + Integer.toHexString(token) + " for: " + reason);

                    IBootstrapAuthenticationCallback cb = null;
                    synchronized (mCallbacks) {
                        cb = mCallbacks.get(token);
                    }
                    if (cb != null) {
                        try {
                            cb.onAuthenticationFailure(token, reason);
                            mRcsStats.onGbaFailureEvent(mSubId, reason);
                        } catch (RemoteException exception) {
                            logd("RemoteException " + exception);
                        }
                        synchronized (mCallbacks) {
                            mCallbacks.remove(token);
                            if (mCallbacks.size() == 0) {
                                releaseServiceAsNeeded(0);
                            }
                        }
                    }
                }
            };

    private void processRequests() {
        if (isServiceConnected()) {
            try {
                while (!mRequestQueue.isEmpty()) {
                    GbaAuthRequest request = new GbaAuthRequest(mRequestQueue.peek());
                    synchronized (mCallbacks) {
                        mCallbacks.put(request.getToken(), request.getCallback());
                    }
                    request.setCallback(mServiceCallback);
                    mIGbaService.authenticationRequest(request);
                    mRequestQueue.poll();
                }
            } catch (RemoteException exception) {
                // Remote exception means that the binder already died.
                mDeathRecipient.binderDied();
                logd("RemoteException " + exception);
            }
        } else {
            while (!mRequestQueue.isEmpty()) {
                GbaAuthRequest req = mRequestQueue.poll();
                try {
                    req.getCallback().onAuthenticationFailure(req.getToken(),
                            TelephonyManager.GBA_FAILURE_REASON_FEATURE_NOT_SUPPORTED);
                } catch (RemoteException exception) {
                    logd("RemoteException " + exception);
                }
            }
        }

        releaseServiceAsNeeded(REQUEST_TIMEOUT_MS);
    }

    private void releaseServiceAsNeeded(int timeout) {
        int configReleaseTime = getReleaseTime();
        //always on
        if (configReleaseTime < 0) {
            return;
        }
        //schedule to release service
        int delayTime = configReleaseTime > timeout ? configReleaseTime : timeout;
        if (mHandler.hasMessages(EVENT_UNBIND_SERVICE)) {
            mHandler.removeMessages(EVENT_UNBIND_SERVICE);
        }
        mHandler.sendEmptyMessageDelayed(EVENT_UNBIND_SERVICE, delayTime);
    }

    private void clearCallbacksAndNotifyFailure() {
        synchronized (mCallbacks) {
            for (int i = 0; i < mCallbacks.size(); i++) {
                IBootstrapAuthenticationCallback cb = mCallbacks.valueAt(i);
                try {
                    cb.onAuthenticationFailure(mCallbacks.keyAt(i),
                            TelephonyManager.GBA_FAILURE_REASON_UNKNOWN);
                } catch (RemoteException exception) {
                    logd("RemoteException " + exception);
                }
            }
            mCallbacks.clear();
        }
    }

    /** return if GBA service has been connected */
    @VisibleForTesting
    public boolean isServiceConnected() {
        //current bound service should be the updated service package.
        synchronized (this) {
            return (mIGbaService != null) && (mIGbaService.asBinder().isBinderAlive())
                    && TextUtils.equals(mServicePackageName, mTargetBindingPackageName);
        }
    }

    private boolean isServiceConnetable() {
        synchronized (this) {
            return mTargetBindingPackageName != null || (
                    mReleaseTime < 0 && !TextUtils.isEmpty(mServicePackageName));
        }
    }

    private void unbindService() {
        if (mDeathRecipient != null) {
            mDeathRecipient.unlinkToDeath();
        }
        if (mServiceConnection != null) {
            logv("unbind service.");
            mContext.unbindService(mServiceConnection);
        }
        mDeathRecipient = null;
        mIGbaService = null;
        mServiceConnection = null;
        mTargetBindingPackageName = null;
    }

    private void bindService() {
        if (mContext == null || !SubscriptionManager.isValidSubscriptionId(mSubId)) {
            loge("Can't bind service with invalid sub Id.");
            return;
        }

        String servicePackage = getServicePackage();
        if (TextUtils.isEmpty(servicePackage)) {
            loge("Can't find the binding package");
            return;
        }

        Intent intent = new Intent(GbaService.SERVICE_INTERFACE);
        intent.setPackage(servicePackage);

        try {
            logv("Trying to bind " + servicePackage);
            mServiceConnection = new GbaServiceConnection();
            if (!mContext.bindService(intent, mServiceConnection,
                    Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE)) {
                logd("Cannot bind to the service.");
                retryBind();
                return;
            }
            mTargetBindingPackageName = servicePackage;
        } catch (SecurityException exception) {
            loge("bindService failed " + exception);
        }
    }

    private void retryBind() {
        //do nothing if binding service has been scheduled
        if (mHandler.hasMessages(EVENT_BIND_SERVICE)) {
            logv("wait for pending retry.");
            return;
        }

        logv("starting retry:" + mRetryTimes);

        mHandler.sendEmptyMessageDelayed(EVENT_BIND_SERVICE, RETRY_TIME_MS);
    }

    private void rebindService(boolean isForce) {
        // Do nothing if no need to rebind.
        if (!isForce && isServiceConnected()) {
            logv("Service " + getServicePackage() + " already bound or being bound.");
            return;
        }

        unbindService();
        bindService();
    }

    /** override GBA service package name to be connected */
    public boolean overrideServicePackage(String packageName) {
        synchronized (this) {
            if (!TextUtils.equals(mServicePackageName, packageName)) {
                logv("Service package name is changed from " + mServicePackageName
                        + " to " + packageName);
                mServicePackageName = packageName;
                if (!mHandler.hasMessages(EVENT_CONFIG_CHANGED)) {
                    mHandler.sendEmptyMessage(EVENT_CONFIG_CHANGED);
                }
                return true;
            }
        }
        return false;
    }

    /** return GBA service package name */
    public String getServicePackage() {
        synchronized (this) {
            return mServicePackageName;
        }
    }

    /** override the release time to unbind GBA service after the request is handled */
    public boolean overrideReleaseTime(int interval) {
        synchronized (this) {
            if (mReleaseTime != interval) {
                logv("Service release time is changed from " + mReleaseTime
                        + " to " + interval);
                mReleaseTime = interval;
                if (!mHandler.hasMessages(EVENT_CONFIG_CHANGED)) {
                    mHandler.sendEmptyMessage(EVENT_CONFIG_CHANGED);
                }
                return true;
            }
        }
        return false;
    }

    /** return the release time to unbind GBA service after the request is handled */
    public int getReleaseTime() {
        synchronized (this) {
            return mReleaseTime;
        }
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    /** only for testing */
    @VisibleForTesting
    public void destroy() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.getLooper().quit();
        mRequestQueue.clear();
        mCallbacks.clear();
        unbindService();
        sGbaManagers.remove(mSubId);
    }

    private void logv(String msg) {
        if (DBG) {
            Rlog.d(mLogTag, msg);
        }
    }

    private void logd(String msg) {
        Rlog.d(mLogTag, msg);
    }

    private void loge(String msg) {
        Rlog.e(mLogTag, msg);
    }
}
