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

package com.android.internal.telephony.uicc.euicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class EuiccCard extends UiccCard {
    private static final String LOG_TAG = "EuiccCard";
    private static final boolean DBG = true;

    private volatile String mEid;
    private RegistrantList mEidReadyRegistrants;

    public EuiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId, Object lock,
            boolean isSupportsMultipleEnabledProfiles) {
        super(c, ci, ics, phoneId, lock, isSupportsMultipleEnabledProfiles);
        if (TextUtils.isEmpty(ics.eid)) {
            loge("no eid given in constructor for phone " + phoneId);
            loadEidAndNotifyRegistrants();
        } else {
            mEid = ics.eid;
            mCardId = ics.eid;
        }
    }

    /**
     * Updates MEP(Multiple Enabled Profile) support flag.
     *
     * <p>If IccSlotStatus comes later, the number of ports reported is only known after the
     * UiccCard creation which will impact UICC MEP capability.
     */
    @Override
    public void updateSupportMultipleEnabledProfile(boolean supported) {
        mIsSupportsMultipleEnabledProfiles = supported;
        for (UiccPort port : mUiccPorts.values()) {
            if (port instanceof EuiccPort) {
                ((EuiccPort) port).updateSupportMultipleEnabledProfile(supported);
            } else {
                loge("eUICC card has non-euicc port object:" + port.toString());
            }
        }
    }

    @Override
    public void update(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId) {
        synchronized (mLock) {
            if (!TextUtils.isEmpty(ics.eid)) {
                mEid = ics.eid;
            }
            super.update(c, ci, ics, phoneId);
        }
    }

    @Override
    protected void updateCardId(String iccId) {
        if (TextUtils.isEmpty(mEid)) {
            super.updateCardId(iccId);
        } else {
            mCardId = mEid;
        }
    }

    /**
     * Registers to be notified when EID is ready. If the EID is ready when this method is called,
     * the registrant will be notified immediately.
     */
    public void registerForEidReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        if (mEid != null) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        } else {
            if (mEidReadyRegistrants == null) {
                mEidReadyRegistrants = new RegistrantList();
            }
            mEidReadyRegistrants.add(r);
        }
    }

    /**
     * Unregisters to be notified when EID is ready.
     */
    public void unregisterForEidReady(Handler h) {
        if (mEidReadyRegistrants != null) {
            mEidReadyRegistrants.remove(h);
        }
    }

    // For RadioConfig<1.2 we don't know the EID when constructing the EuiccCard, so callers may
    // need to register to be notified when we have the EID
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected void loadEidAndNotifyRegistrants() {
        Handler euiccMainThreadHandler = new Handler();
        AsyncResultCallback<String> cardCb = new AsyncResultCallback<String>() {
            @Override
            public void onResult(String result) {
                mEid = result;
                mCardId = result;
                if (TextUtils.isEmpty(result)) {
                    logd("eid is loaded but empty ");
                }
                if (mEidReadyRegistrants != null) {
                    mEidReadyRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                }
            }

            @Override
            public void onException(Throwable e) {
                // Still notifying registrants even getting eid fails.
                if (mEidReadyRegistrants != null) {
                    mEidReadyRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                }
                mEid = "";
                mCardId = "";
                Rlog.e(LOG_TAG, "Failed loading eid", e);
            }
        };
        ((EuiccPort) mUiccPorts.get(TelephonyManager.DEFAULT_PORT_INDEX)).getEid(cardCb,
                euiccMainThreadHandler);
    }

    /**
     * Gets the EID synchronously.
     * @return The EID string. Returns null if it is not ready yet.
     */
    public String getEid() {
        return mEid;
    }

    private static void loge(String message) {
        Rlog.e(LOG_TAG, message);
    }

    private static void loge(String message, Throwable tr) {
        Rlog.e(LOG_TAG, message, tr);
    }

    private static void logi(String message) {
        Rlog.i(LOG_TAG, message);
    }

    private static void logd(String message) {
        if (DBG) {
            Rlog.d(LOG_TAG, message);
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println("EuiccCard:");
        pw.println(" mEid=" + mEid);
    }
}
