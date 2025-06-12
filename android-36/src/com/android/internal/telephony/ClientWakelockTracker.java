/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.internal.telephony;

import android.os.SystemClock;
import android.telephony.ClientRequestStats;

import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClientWakelockTracker {
    public static final String LOG_TAG = "ClientWakelockTracker";
    @VisibleForTesting
    public HashMap<String, ClientWakelockAccountant> mClients =
        new HashMap<String, ClientWakelockAccountant>();
    @VisibleForTesting
    public ArrayList<ClientWakelockAccountant> mActiveClients = new ArrayList<>();

    @VisibleForTesting
    public void startTracking(String clientId, int requestId, int token, int numRequestsInQueue) {
        ClientWakelockAccountant client = getClientWakelockAccountant(clientId);
        long uptime = SystemClock.uptimeMillis();
        client.startAttributingWakelock(requestId, token, numRequestsInQueue, uptime);
        updateConcurrentRequests(numRequestsInQueue, uptime);
        synchronized (mActiveClients) {
            if (!mActiveClients.contains(client)) {
                mActiveClients.add(client);
            }
        }
    }

    @VisibleForTesting
    public void stopTracking(String clientId, int requestId, int token, int numRequestsInQueue) {
        ClientWakelockAccountant client = getClientWakelockAccountant(clientId);
        long uptime = SystemClock.uptimeMillis();
        client.stopAttributingWakelock(requestId, token, uptime);
        if(client.getPendingRequestCount() == 0) {
            synchronized (mActiveClients) {
                mActiveClients.remove(client);
            }
        }
        updateConcurrentRequests(numRequestsInQueue, uptime);
    }

    @VisibleForTesting
    public void stopTrackingAll() {
        long uptime = SystemClock.uptimeMillis();
        synchronized (mActiveClients) {
            for (ClientWakelockAccountant client : mActiveClients) {
                client.stopAllPendingRequests(uptime);
            }
            mActiveClients.clear();
        }
    }

    List<ClientRequestStats> getClientRequestStats() {
        List<ClientRequestStats> list;
        long uptime = SystemClock.uptimeMillis();
        synchronized (mClients) {
            list = new ArrayList<>(mClients.size());
            for (String key :  mClients.keySet()) {
                ClientWakelockAccountant client = mClients.get(key);
                client.updatePendingRequestWakelockTime(uptime);
                list.add(new ClientRequestStats(client.mRequestStats));
            }
        }
        return list;
    }

    private ClientWakelockAccountant getClientWakelockAccountant(String clientId) {
        ClientWakelockAccountant client;
        synchronized (mClients) {
            if (mClients.containsKey(clientId)) {
                client = mClients.get(clientId);
            } else {
                client = new ClientWakelockAccountant(clientId);
                mClients.put(clientId, client);
            }
        }
        return client;
    }

    private void updateConcurrentRequests(int numRequestsInQueue, long time) {
        if(numRequestsInQueue != 0) {
            synchronized (mActiveClients) {
                for (ClientWakelockAccountant cI : mActiveClients) {
                    cI.changeConcurrentRequests(numRequestsInQueue, time);
                }
            }
        }
    }

    public boolean isClientActive(String clientId) {
        ClientWakelockAccountant client = getClientWakelockAccountant(clientId);
        synchronized (mActiveClients) {
            if (mActiveClients.contains(client)) {
                return true;
            }
        }

        return false;
    }

    void dumpClientRequestTracker(PrintWriter pw) {
        pw.println("-------mClients---------------");
        synchronized (mClients) {
            for (String key : mClients.keySet()) {
                pw.println("Client : " + key);
                pw.println(mClients.get(key).toString());
            }
        }
    }
}
