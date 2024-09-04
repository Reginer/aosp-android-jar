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

package com.android.ims.rcs.uce.eab;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.telephony.ims.RcsContactUceCapability;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The result class of retrieving capabilities from cache.
 */
public class EabCapabilityResult {

    /**
     * Query successful.
     */
    public static final int EAB_QUERY_SUCCESSFUL = 0;

    /**
     * The {@link EabControllerImpl} has been destroyed.
     */
    public static final int EAB_CONTROLLER_DESTROYED_FAILURE = 1;

    /**
     * The contact's capabilities expired.
     */
    public static final int EAB_CONTACT_EXPIRED_FAILURE = 2;

    /**
     * The contact cannot be found in the contact provider.
     */
    public static final int EAB_CONTACT_NOT_FOUND_FAILURE = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "EAB_", value = {
            EAB_QUERY_SUCCESSFUL,
            EAB_CONTROLLER_DESTROYED_FAILURE,
            EAB_CONTACT_EXPIRED_FAILURE,
            EAB_CONTACT_NOT_FOUND_FAILURE
    })
    public @interface QueryResult {}

    private final @QueryResult int mStatus;
    private final Uri mContactUri;
    private final RcsContactUceCapability mContactCapabilities;

    public EabCapabilityResult(@QueryResult Uri contactUri, int status,
            RcsContactUceCapability capabilities) {
        mStatus = status;
        mContactUri = contactUri;
        mContactCapabilities = capabilities;
    }

    /**
     * Return the status of query. The possible values are
     * {@link EabCapabilityResult#EAB_QUERY_SUCCESSFUL},
     * {@link EabCapabilityResult#EAB_CONTROLLER_DESTROYED_FAILURE},
     * {@link EabCapabilityResult#EAB_CONTACT_EXPIRED_FAILURE},
     * {@link EabCapabilityResult#EAB_CONTACT_NOT_FOUND_FAILURE}.
     *
     */
    public @NonNull int getStatus() {
        return mStatus;
    }

    /**
     * Return the contact uri.
     */
    public @NonNull Uri getContact() {
        return mContactUri;
    }

    /**
     * Return the contacts capabilities which are cached in the EAB database and
     * are not expired.
     */
    public @Nullable RcsContactUceCapability getContactCapabilities() {
        return mContactCapabilities;
    }
}
