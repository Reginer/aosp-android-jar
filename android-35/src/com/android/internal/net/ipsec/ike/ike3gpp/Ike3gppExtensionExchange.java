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
package com.android.internal.net.ipsec.ike.ike3gpp;

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import static com.android.internal.net.ipsec.ike.message.IkeMessage.IKE_EXCHANGE_SUBTYPE_GENERIC_INFO;
import static com.android.internal.net.ipsec.ike.message.IkeMessage.IKE_EXCHANGE_SUBTYPE_IKE_AUTH;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.net.ipsec.ike.ike3gpp.Ike3gppExtension;
import android.net.ipsec.ike.ike3gpp.Ike3gppExtension.Ike3gppDataListener;
import android.util.ArraySet;

import com.android.internal.net.ipsec.ike.message.IkeMessage;
import com.android.internal.net.ipsec.ike.message.IkePayload;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Ike3gppExtensionExchange contains the implementation for 3GPP-specific functionality in IKEv2.
 */
public class Ike3gppExtensionExchange implements AutoCloseable {
    private static final String TAG = Ike3gppExtensionExchange.class.getSimpleName();

    private static final Set<Ike3gppDataListener> REGISTERED_LISTENERS =
            Collections.synchronizedSet(new ArraySet<>());

    /**
     * Indicates that the caller must wait the specified time before attempting to open an IKE
     * Session with the peer.
     *
     * <p>Note that this is not an IANA-specified value.
     *
     * <p>Must be accompanied by an Error-Notify(ERROR_TYPE_NO_APN_SUBSCRIPTION) or
     * Error-Notify(ERROR_TYPE_NETWORK_FAILURE); otherwise, the payload will be logged and ignored.
     */
    public static final int NOTIFY_TYPE_BACKOFF_TIMER = 41041;

    /**
     * Indicates that the UE supports N1 Mode during 5G SA ePDG tunnel setup.
     *
     * <p>Note that this is not an IANA-specified value.
     *
     * <p>A PDU session ID will be included to indicate the PDU session associated with the IKEv2
     * SA.
     *
     * <p>See TS 124 302 - Universal Mobile Telecommunications System (UMTS); LTE; 5G; Access to the
     * 3GPP Evolved Packet Core (EPC) via non-3GPP access networks (Section 8.2.9.15) for more
     * details.
     */
    public static final int NOTIFY_TYPE_N1_MODE_CAPABILITY = 51015;

    /**
     * Used for reporting the S-NSSAI from the server to the UE for the reported PDU Session ID.
     *
     * <p>Note that this is not an IANA-specified value.
     *
     * <p>This Payload will only be sent from the server to the user device after {@link
     * NOTIFY_TYPE_N1_MODE_CAPABILITY} is sent during the IKE_AUTH exchange.
     *
     * <p>See TS 124 302 - Universal Mobile Telecommunications System (UMTS); LTE; 5G; Access to the
     * 3GPP Evolved Packet Core (EPC) via non-3GPP access networks (Section 8.2.9.16) for more
     * details.
     */
    public static final int NOTIFY_TYPE_N1_MODE_INFORMATION = 51115;

    /**
     * Used to share the device idenity (IMEI/IMEISV) with the carrier network.
     *
     * <p>Note that this is not an IANA-specified value.
     *
     * <p>See TS 124 302 - Universal Mobile Telecommunications System (UMTS); LTE; 5G; Access to the
     * 3GPP Evolved Packet Core (EPC) via non-3GPP access networks (Section 8.2.9.2) for more
     * details.
     */
    public static final int NOTIFY_TYPE_DEVICE_IDENTITY = 41101;

    @Nullable private final Ike3gppExtension mIke3gppExtension;
    @NonNull private final Executor mUserCbExecutor;
    @Nullable private final Ike3gppIkeAuth mIke3gppIkeAuth;
    @Nullable private final Ike3gppIkeInfo mIke3gppIkeInfo;

    /**
     * Initializes an Ike3gppExtensionExchange.
     *
     * <p>If ike3gppExtension is null, no 3GPP functionality will be enabled.
     */
    public Ike3gppExtensionExchange(
            @Nullable Ike3gppExtension ike3gppExtension, @NonNull Executor userCbExecutor) {
        mIke3gppExtension = ike3gppExtension;
        mUserCbExecutor = Objects.requireNonNull(userCbExecutor, "userCbExecutor must not be null");

        if (mIke3gppExtension != null) {
            mIke3gppIkeAuth = new Ike3gppIkeAuth(mIke3gppExtension, mUserCbExecutor);
            mIke3gppIkeInfo = new Ike3gppIkeInfo(mIke3gppExtension, mUserCbExecutor);

            if (!REGISTERED_LISTENERS.add(ike3gppExtension.getIke3gppDataListener())) {
                throw new IllegalArgumentException(
                        "Ike3gppDataListener must be unique for each IkeSession");
            }

            logd("IKE 3GPP Extension enabled: " + mIke3gppExtension.getIke3gppParams());
        } else {
            mIke3gppIkeAuth = null;
            mIke3gppIkeInfo = null;
        }
    }

    @Override
    public void close() {
        if (mIke3gppExtension == null) return;

        REGISTERED_LISTENERS.remove(mIke3gppExtension.getIke3gppDataListener());
    }

    /**
     * Gets the 3GPP-specific Information Response IkePayloads for the specified exchangeSubtype.
     */
    public List<IkePayload> getResponsePayloads(
            int exchangeSubtype, List<IkePayload> ike3gppRequestPayloads) {
        if (mIke3gppExtension == null) return Collections.EMPTY_LIST;

        switch (exchangeSubtype) {
            case IKE_EXCHANGE_SUBTYPE_GENERIC_INFO:
                return mIke3gppIkeInfo.getResponsePayloads(ike3gppRequestPayloads);
            default:
                // No 3GPP-specific behavior for this exchange subtype
                return Collections.EMPTY_LIST;
        }
    }

    /** Gets the 3GPP-specific Request IkePayloads for the specified exchangeSubtype. */
    public List<IkePayload> getRequestPayloads(int exchangeSubtype) {
        if (mIke3gppExtension == null) return Collections.EMPTY_LIST;

        switch (exchangeSubtype) {
            case IKE_EXCHANGE_SUBTYPE_IKE_AUTH:
                return mIke3gppIkeAuth.getRequestPayloads();
            default:
                // No 3GPP-specific behavior for this exchange subtype
                String exchangeSubtypeString =
                        IkeMessage.getIkeExchangeSubTypeString(exchangeSubtype);
                logw("No 3GPP request payloads added for: " + exchangeSubtypeString);
                return Collections.EMPTY_LIST;
        }
    }

    /**
     * Provides a list of 3GPP payloads to add in an outbound AUTH req based on peer authentication
     * status.
     */
    public List<IkePayload> getRequestPayloadsInEap(boolean serverAuthenticated) {
        if (mIke3gppExtension == null) return Collections.EMPTY_LIST;
        return mIke3gppIkeAuth.getRequestPayloadsInEap(serverAuthenticated);
    }

    /**
     * Returns a list of 3GPP-specific Response Payloads from the given list that are valid for the
     * specified exchangeSubtype.
     */
    public List<IkePayload> extract3gppResponsePayloads(
            int exchangeSubtype, List<IkePayload> payloads) {
        if (mIke3gppExtension == null) return Collections.EMPTY_LIST;

        switch (exchangeSubtype) {
            case IKE_EXCHANGE_SUBTYPE_IKE_AUTH:
                return mIke3gppIkeAuth.extract3gppResponsePayloads(payloads);
            default:
                // No 3GPP-specific behavior for this exchange subtype
                String exchangeSubtypeString =
                        IkeMessage.getIkeExchangeSubTypeString(exchangeSubtype);
                logw("No 3GPP response payloads expected for: " + exchangeSubtypeString);
                return Collections.EMPTY_LIST;
        }
    }

    /**
     * Handles the provided Response IkePayloads for the specified exchangeSubtype.
     *
     * <p>If the caller needs to be notified of received Ike3gppData, the configured
     * Ike3gppDataListener will be invoked.
     */
    public void handle3gppResponsePayloads(int exchangeSubtype, List<IkePayload> ike3gppPayloads)
            throws InvalidSyntaxException {
        if (mIke3gppExtension == null || ike3gppPayloads.isEmpty()) return;

        switch (exchangeSubtype) {
            case IKE_EXCHANGE_SUBTYPE_IKE_AUTH:
                mIke3gppIkeAuth.handleAuthResp(ike3gppPayloads);
                break;
            default:
                // No 3GPP-specific behavior for this exchange subtype
                String exchangeSubtypeString =
                        IkeMessage.getIkeExchangeSubTypeString(exchangeSubtype);
                logw("Received unexpected 3GPP payloads in: " + exchangeSubtypeString);
        }
    }

    private void logw(String msg) {
        getIkeLog().w(TAG, msg);
    }

    private void logd(String msg) {
        getIkeLog().d(TAG, msg);
    }
}
