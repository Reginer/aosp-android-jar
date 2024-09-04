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
import android.annotation.SystemApi;

import java.util.Objects;

/**
 * Ike3gppN1ModeInformation represents the data provided by the peer/remote endpoint for an
 * N1_MODE_INFORMATION Notify payload.
 *
 * @see 3GPP TS 24.302 Section 8.2.9.16 N1_MODE_INFORMATION Notify payload
 * @hide
 */
@SystemApi
public final class Ike3gppN1ModeInformation extends Ike3gppData {
    private final byte[] mSnssai;

    /**
     * Constructs an Ike3gppN1ModeInformation with the specified parameters.
     *
     * @param snssai the SNSSAI value indicated by the peer
     * @hide
     */
    @SystemApi
    public Ike3gppN1ModeInformation(@NonNull byte[] snssai) {
        Objects.requireNonNull(snssai, "snssai must not be null");
        mSnssai = snssai.clone();
    }

    @Override
    public @DataType int getDataType() {
        return DATA_TYPE_NOTIFY_N1_MODE_INFORMATION;
    }

    /**
     * Returns the S-NSSAI value reported by the peer.
     *
     * <p>The S-NSSAI is coded as defined in 3GPP TS 24.501 Section 9.11.2.8.
     */
    @NonNull
    public byte[] getSnssai() {
        return mSnssai.clone();
    }
}
