/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.imsphone;

import static com.android.internal.telephony.Call.State.DISCONNECTED;
import static com.android.internal.telephony.Call.State.IDLE;

import android.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Contains the state of all IMS calls.
 */
public class ImsCallInfoTracker {
    private static final String LOG_TAG = "ImsCallInfoTracker";
    private static final boolean DBG = false;

    private final Phone mPhone;
    private final List<ImsCallInfo> mQueue = new ArrayList<>();
    private int mNextIndex = 1;

    private final Map<Connection, ImsCallInfo> mImsCallInfo = new HashMap<>();

    public ImsCallInfoTracker(Phone phone) {
        mPhone = phone;
    }

    /**
     * Adds a new instance of the IMS call.
     *
     * @param c The instance of {@link ImsPhoneConnection}.
     */
    public void addImsCallStatus(@NonNull ImsPhoneConnection c) {
        if (DBG) Rlog.d(LOG_TAG, "addImsCallStatus");

        synchronized (mImsCallInfo) {
            if (mQueue.isEmpty()) {
                mQueue.add(new ImsCallInfo(mNextIndex++));
            }

            Iterator<ImsCallInfo> it = mQueue.iterator();
            ImsCallInfo imsCallInfo = it.next();
            mQueue.remove(imsCallInfo);

            imsCallInfo.update(c);
            mImsCallInfo.put(c, imsCallInfo);

            notifyImsCallStatus();

            if (DBG) dump();
        }
    }

    /**
     * Updates the list of IMS calls.
     *
     * @param c The instance of {@link ImsPhoneConnection}.
     */
    public void updateImsCallStatus(@NonNull ImsPhoneConnection c) {
        updateImsCallStatus(c, false, false);
    }

    /**
     * Updates the list of IMS calls.
     *
     * @param c The instance of {@link ImsPhoneConnection}.
     * @param holdReceived {@code true} if the remote party held the call.
     * @param resumeReceived {@code true} if the remote party resumed the call.
     */
    public void updateImsCallStatus(@NonNull ImsPhoneConnection c,
            boolean holdReceived, boolean resumeReceived) {
        if (DBG) {
            Rlog.d(LOG_TAG, "updateImsCallStatus holdReceived=" + holdReceived
                    + ", resumeReceived=" + resumeReceived);
        }

        synchronized (mImsCallInfo) {
            ImsCallInfo info = mImsCallInfo.get(c);

            if (info == null) {
                // This happens when the user tries to hangup the call after handover has completed.
                return;
            }

            boolean changed = info.update(c, holdReceived, resumeReceived);

            if (changed) notifyImsCallStatus();

            Call.State state = c.getState();

            if (DBG) Rlog.d(LOG_TAG, "updateImsCallStatus state=" + state);
            // Call is disconnected. There are 2 cases in disconnected state:
            // if silent redial, state == IDLE, otherwise, state == DISCONNECTED.
            if (state == DISCONNECTED || state == IDLE) {
                // clear the disconnected call
                mImsCallInfo.remove(c);
                info.reset();
                if (info.getIndex() < (mNextIndex - 1)) {
                    mQueue.add(info);
                    sort(mQueue);
                } else {
                    mNextIndex--;
                }
            }

            if (DBG) dump();
        }
    }

    /** Clears all orphaned IMS call information. */
    public void clearAllOrphanedConnections() {
        if (DBG) Rlog.d(LOG_TAG, "clearAllOrphanedConnections");

        Collection<ImsCallInfo> infos = mImsCallInfo.values();
        infos.stream().forEach(info -> { info.onDisconnect(); });
        notifyImsCallStatus();
        clearAllCallInfo();

        if (DBG) dump();
    }

    /** Notifies that SRVCC has completed. */
    public void notifySrvccCompleted() {
        if (DBG) Rlog.d(LOG_TAG, "notifySrvccCompleted");

        clearAllCallInfo();
        notifyImsCallStatus();

        if (DBG) dump();
    }

    private void clearAllCallInfo() {
        try {
            Collection<ImsCallInfo> infos = mImsCallInfo.values();
            infos.stream().forEach(info -> { info.reset(); });
            mImsCallInfo.clear();
            mQueue.clear();
            mNextIndex = 1;
        } catch (UnsupportedOperationException e) {
            Rlog.e(LOG_TAG, "e=" + e);
        }
    }

    private void notifyImsCallStatus() {
        Collection<ImsCallInfo> infos = mImsCallInfo.values();
        ArrayList<ImsCallInfo> imsCallInfo = new ArrayList<ImsCallInfo>(infos);
        sort(imsCallInfo);
        mPhone.updateImsCallStatus(imsCallInfo, null);
    }

    /**
     * Sorts the list of IMS calls by the call index.
     *
     * @param infos The list of IMS calls.
     */
    @VisibleForTesting
    public static void sort(List<ImsCallInfo> infos) {
        Collections.sort(infos, new Comparator<ImsCallInfo>() {
            @Override
            public int compare(ImsCallInfo l, ImsCallInfo r) {
                if (l.getIndex() > r.getIndex()) {
                    return 1;
                } else if (l.getIndex() < r.getIndex()) {
                    return -1;
                }
                return 0;
            }
        });
    }

    private void dump() {
        Collection<ImsCallInfo> infos = mImsCallInfo.values();
        ArrayList<ImsCallInfo> imsCallInfo = new ArrayList<ImsCallInfo>(infos);
        sort(imsCallInfo);
        Rlog.d(LOG_TAG, "imsCallInfos=" + imsCallInfo);
    }
}
