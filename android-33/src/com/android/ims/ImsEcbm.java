/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.ims;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;

import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsEcbmListener;
import com.android.telephony.Rlog;

/**
 * Provides APIs for the modem to communicate the CDMA Emergency Callback Mode status for IMS.
 *
 * @hide
 */
public class ImsEcbm {
    private static final String TAG = "ImsEcbm";
    private static final boolean DBG = true;

    private final IImsEcbm miEcbm;

    public ImsEcbm(IImsEcbm iEcbm) {
        if (DBG) Rlog.d(TAG, "ImsEcbm created");
        miEcbm = iEcbm;
    }

    public void setEcbmStateListener(ImsEcbmStateListener ecbmListener) throws RemoteException {
            miEcbm.setListener(ecbmListener != null ?
                    new ImsEcbmListenerProxy(ecbmListener) : null);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void exitEmergencyCallbackMode() throws ImsException {
        try {
            miEcbm.exitEmergencyCallbackMode();
        } catch (RemoteException e) {
            throw new ImsException("exitEmergencyCallbackMode()", e,
                    ImsReasonInfo.CODE_LOCAL_IMS_SERVICE_DOWN);
        }
    }

    public boolean isBinderAlive() {
        return miEcbm.asBinder().isBinderAlive();
    }

    /**
     * Adapter class for {@link IImsEcbmListener}.
     */
    private static class ImsEcbmListenerProxy extends IImsEcbmListener.Stub {
        private final ImsEcbmStateListener mListener;

        public ImsEcbmListenerProxy(ImsEcbmStateListener listener) {
            mListener = listener;
        }

        @Override
        public void enteredECBM() {
            if (DBG) Rlog.d(TAG, "enteredECBM ::");

            if (mListener != null) {
                mListener.onECBMEntered();
            }
        }

        @Override
        public void exitedECBM() {
            if (DBG) Rlog.d(TAG, "exitedECBM ::");

            if (mListener != null) {
                mListener.onECBMExited();
            }
        }
    }
}
