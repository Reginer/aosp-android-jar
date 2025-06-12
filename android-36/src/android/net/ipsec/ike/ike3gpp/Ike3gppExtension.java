/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.net.ipsec.ike.ike3gpp;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;

import java.util.List;
import java.util.Objects;

/**
 * Ike3gppExtension is used to provide 3GPP-specific extensions for an IKE Session.
 *
 * <p>Ike3gppExtension must be set in IkeSessionParams.Builder in order for it to be enabled during
 * an IKE Session.
 *
 * @see 3GPP ETSI TS 24.302: Access to the 3GPP Evolved Packet Core (EPC) via non-3GPP access
 *     networks
 * @hide
 */
@SystemApi
public final class Ike3gppExtension {
    @NonNull private final Ike3gppParams mIke3gppParams;
    @NonNull private final Ike3gppDataListener mIke3gppDataListener;

    /**
     * Constructs an Ike3gppExtension instance with the given Ike3gppDataListener and Ike3gppParams
     * instances.
     *
     * @param ike3gppParams Ike3gppParams used to configure the 3GPP-support for an IKE Session.
     * @param ike3gppDataListener Ike3gppDataListener used to notify the caller of 3GPP-specific
     *     data received during an IKE Session.
     */
    // ExecutorRegistration: Not necessary to take an Executor for invoking the listener here, as
    // this is not actually where the listener is registered. The caller's Executor provided in the
    // IkeSession constructor will be used to invoke the Ike3gppDataListener.
    @SuppressLint("ExecutorRegistration")
    public Ike3gppExtension(
            @NonNull Ike3gppParams ike3gppParams,
            @NonNull Ike3gppDataListener ike3gppDataListener) {
        Objects.requireNonNull(ike3gppParams, "ike3gppParams must not be null");
        Objects.requireNonNull(ike3gppDataListener, "ike3gppDataListener must not be null");

        mIke3gppParams = ike3gppParams;
        mIke3gppDataListener = ike3gppDataListener;
    }

    /** Retrieves the configured Ike3gppDataListener. */
    @NonNull
    public Ike3gppDataListener getIke3gppDataListener() {
        return mIke3gppDataListener;
    }

    /** Retrieves the configured Ike3gppParams. */
    @NonNull
    public Ike3gppParams getIke3gppParams() {
        return mIke3gppParams;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIke3gppParams, mIke3gppDataListener);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ike3gppExtension)) {
            return false;
        }

        Ike3gppExtension other = (Ike3gppExtension) o;

        return mIke3gppParams.equals(other.mIke3gppParams)
                && mIke3gppDataListener.equals(other.mIke3gppDataListener);
    }

    /**
     * Listener for receiving 3GPP-specific data.
     *
     * <p>MUST be unique to each IKE Session.
     *
     * <p>All Ike3gppDataListener calls will be invoked on the Executor provided in the IkeSession
     * constructor.
     */
    public interface Ike3gppDataListener {
        /**
         * Invoked when the IKE Session receives 3GPP-specific data.
         *
         * <p>This function will be invoked at most once for each IKE Message received by the IKEv2
         * library.
         *
         * @param ike3gppDataList List<Ike3gppData> the 3GPP-data received
         */
        void onIke3gppDataReceived(@NonNull List<Ike3gppData> ike3gppDataList);
    }
}
