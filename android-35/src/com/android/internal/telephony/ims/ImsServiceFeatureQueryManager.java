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

package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages the querying of multiple ImsServices asynchronously in order to retrieve the ImsFeatures
 * they support.
 */

public class ImsServiceFeatureQueryManager {

    private final class ImsServiceFeatureQuery implements ServiceConnection {

        private static final String LOG_TAG = "ImsServiceFeatureQuery";

        private final ComponentName mName;
        private final String mIntentFilter;
        // Track the status of whether or not the Service has died in case we need to permanently
        // unbind (see onNullBinding below).
        private boolean mIsServiceConnectionDead = false;


        ImsServiceFeatureQuery(ComponentName name, String intentFilter) {
            mName = name;
            mIntentFilter = intentFilter;
        }

        /**
         * Starts the bind to the ImsService specified ComponentName.
         * @return true if binding started, false if it failed and will not recover.
         */
        public boolean start() {
            Log.d(LOG_TAG, "start: intent filter=" + mIntentFilter + ", name=" + mName);
            Intent imsServiceIntent = new Intent(mIntentFilter).setComponent(mName);
            int serviceFlags = Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE
                    | Context.BIND_IMPORTANT;
            boolean bindStarted = mContext.bindService(imsServiceIntent, this, serviceFlags);
            if (!bindStarted) {
                // Docs say to unbind if this fails.
                cleanup();
            }
            return bindStarted;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(LOG_TAG, "onServiceConnected for component: " + name);
            if (service != null) {
                queryImsFeatures(IImsServiceController.Stub.asInterface(service));
            } else {
                Log.w(LOG_TAG, "onServiceConnected: " + name + " binder null.");
                cleanup();
                mListener.onPermanentError(name);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(LOG_TAG, "onServiceDisconnected for component: " + name);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            mIsServiceConnectionDead = true;
            Log.w(LOG_TAG, "onBindingDied: " + name);
            cleanup();
            // retry again!
            mListener.onError(name);
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.w(LOG_TAG, "onNullBinding: " + name);
            // onNullBinding will happen after onBindingDied. In this case, we should not
            // permanently unbind and instead let the automatic rebind occur.
            if (mIsServiceConnectionDead) return;
            cleanup();
            mListener.onPermanentError(name);
        }

        private void queryImsFeatures(IImsServiceController controller) {
            ImsFeatureConfiguration config;
            try {
                config = controller.querySupportedImsFeatures();
            } catch (Exception e) {
                Log.w(LOG_TAG, "queryImsFeatures - error: " + e);
                cleanup();
                // Retry again!
                mListener.onError(mName);
                return;
            }
            Set<ImsFeatureConfiguration.FeatureSlotPair> servicePairs;
            if (config == null) {
                // ensure that if the ImsService sent a null config, we return an empty feature
                // set to the ImsResolver.
                servicePairs = Collections.emptySet();
            } else {
                servicePairs = config.getServiceFeatures();
            }
            // Complete, remove from active queries and notify.
            cleanup();
            mListener.onComplete(mName, servicePairs);
        }

        private void cleanup() {
            mContext.unbindService(this);
            synchronized (mLock) {
                mActiveQueries.remove(mName);
            }
        }
    }

    public interface Listener {
        /**
         * Called when a query has completed.
         * @param name The Package Name of the query
         * @param features A Set of slotid->feature pairs that the ImsService supports.
         */
        void onComplete(ComponentName name, Set<ImsFeatureConfiguration.FeatureSlotPair> features);

        /**
         * Called when a query has failed and should be retried.
         */
        void onError(ComponentName name);

        /**
         * Called when a query has failed due to a permanent error and should not be retried.
         */
        void onPermanentError(ComponentName name);
    }

    // Maps an active ImsService query (by Package Name String) its query.
    private final Map<ComponentName, ImsServiceFeatureQuery> mActiveQueries = new HashMap<>();
    private final Context mContext;
    private final Listener mListener;
    private final Object mLock = new Object();

    public ImsServiceFeatureQueryManager(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
    }

    /**
     * Starts an ImsService feature query for the ComponentName and Intent specified.
     * @param name The ComponentName of the ImsService being queried.
     * @param intentFilter The Intent filter that the ImsService specified.
     * @return true if the query started, false if it was unable to start.
     */
    public boolean startQuery(ComponentName name, String intentFilter) {
        synchronized (mLock) {
            if (mActiveQueries.containsKey(name)) {
                // We already have an active query, wait for it to return.
                return true;
            }
            ImsServiceFeatureQuery query = new ImsServiceFeatureQuery(name, intentFilter);
            mActiveQueries.put(name, query);
            return query.start();
        }
    }

    /**
     * @return true if there are any active queries, false if the manager is idle.
     */
    public boolean isQueryInProgress() {
        synchronized (mLock) {
            return !mActiveQueries.isEmpty();
        }
    }
}
