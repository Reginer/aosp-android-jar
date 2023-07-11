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

package com.android.ims.rcs.uce.presence.pidfparser;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;

import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.PresenceBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper class that uses the parsed information to construct {@link RcsContactUceCapability}
 * instances.
 */

public class RcsContactUceCapabilityWrapper {
    private final Uri mContactUri;
    private final int mSourceType;
    private final int mRequestResult;
    private boolean mIsMalformed;
    private final List<RcsContactPresenceTuple> mPresenceTuples = new ArrayList<>();
    private Uri mEntityUri;

    /**
     * Create the wrapper, which can be used to set UCE capabilities as well as custom
     * capability extensions.
     * @param contact The contact URI that the capabilities are attached to.
     * @param sourceType The type where the capabilities of this contact were retrieved from.
     * @param requestResult the request result
     */
    public RcsContactUceCapabilityWrapper(@NonNull Uri contact, int sourceType, int requestResult) {
        mContactUri = contact;
        mSourceType = sourceType;
        mRequestResult = requestResult;
        mIsMalformed = false;
    }

    /**
     * Add the {@link RcsContactPresenceTuple} into the presence tuple list.
     * @param tuple The {@link RcsContactPresenceTuple} to be added into.
     */
    public void addCapabilityTuple(@NonNull RcsContactPresenceTuple tuple) {
        mPresenceTuples.add(tuple);
    }

    /**
     * This flag is set if at least one tuple could not be parsed due to malformed contents.
     */
    public void setMalformedContents() {
        mIsMalformed = true;
    }

    /**
     * Set the entity URI related to the contact whose capabilities were requested.
     * @param entityUri the 'pres' URL of the PRESENTITY publishing presence document.
     */
    public void setEntityUri(@NonNull Uri entityUri) {
        mEntityUri = entityUri;
    }

    /**
     * Whether the XML is malformed.
     * @return {@code true} if all of the presence tuple information associated with
     * the entity URI ({@link #getEntityUri}) is malformed and there is no tuple info
     * available. If one or more of the tuples are still well-formed after parsing the
     * XML, this method will return {@code false}.
     */
    public boolean isMalformed() {
        if (mIsMalformed == false) {
            return false;
        }
        if (mPresenceTuples.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Retrieve the entity URI of the contact whose presence information is being requested for.
     * @return the URI representing the 'pres' URL of the PRESENTITY publishing presence document
     * or {@code null} if the entity uri does not exist in the presence document.
     */
    public @Nullable Uri getEntityUri() {
        return mEntityUri;
    }

    /**
     * @return a new RcsContactUceCapability instance from the contents of this wrapper.
     */
    public @NonNull RcsContactUceCapability toRcsContactUceCapability() {

        PresenceBuilder presenceBuilder = new PresenceBuilder(mContactUri,
                mSourceType, mRequestResult);

        // Add all the capability tuples of this contact
        presenceBuilder.addCapabilityTuples(mPresenceTuples);
        presenceBuilder.setEntityUri(mEntityUri);
        return presenceBuilder.build();
    }
}
