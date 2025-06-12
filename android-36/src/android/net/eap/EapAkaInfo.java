/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net.eap;

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_AKA;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.ipsec.ike.IkeSessionConfiguration;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * EapAkaInfo represents data provided by the server during EAP AKA authentication
 */
public final class EapAkaInfo extends EapInfo {
    /**
     * Re-authentication ID for next use
     *
     * <p>This identity encoding MUST follow the UTF-8 transformation format[RFC3629].
     *
     * @hide
     */
    private final byte[] mReauthId;

    /** @hide */
    @VisibleForTesting
    public EapAkaInfo(@Nullable byte[] reauthId) {
        super(EAP_TYPE_AKA);
        mReauthId = reauthId;
    }

    private EapAkaInfo(Builder builder) {
        super(EAP_TYPE_AKA);
        mReauthId = builder.mReauthId;
    }

    /**
     * Retrieves re-authentication ID from server for next use.
     *
     * @return re-authentication ID
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc4187#section-5">RFC 4186,
     *     Extensible Authentication Protocol Method for 3rd Generation Authentication and
     *     Key Agreement (EAP-AKA)</a>
     */
    @Nullable
    public byte[] getReauthId() {
        return mReauthId;
    }

    /**
     * This class can be used to incrementally construct an {@link EapAkaInfo}.
     *
     * <p>Except for testing, IKE library users normally do not instantiate {@link EapAkaInfo}
     * themselves but instead get a reference via {@link IkeSessionConfiguration}
     */
    public static final class Builder {
        private byte[] mReauthId;

        /**
         * Sets the re-authentication ID for next use.
         *
         * @param reauthId byte[] representing the client's EAP Identity.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder setReauthId(@NonNull byte[] reauthId) {
            Objects.requireNonNull(reauthId, "reauthId must not be null");
            this.mReauthId = new byte[reauthId.length];
            System.arraycopy(reauthId, 0, this.mReauthId, 0, reauthId.length);
            return this;
        }

        /**
         * Constructs and returns an EapAkaInfo with the information applied to this
         * Builder.
         *
         * @return the EapAkaInfo constructed by this Builder.
         */
        @NonNull
        public EapAkaInfo build() {
            return new EapAkaInfo(this);
        }
    }
}
