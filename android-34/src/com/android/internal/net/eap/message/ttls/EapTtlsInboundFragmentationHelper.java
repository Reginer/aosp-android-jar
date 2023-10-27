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

package com.android.internal.net.eap.message.ttls;

import static com.android.internal.net.eap.EapAuthenticator.LOG;

import android.annotation.IntDef;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/** The inbound fragmentation helper is responsible for assembling fragmented EAP-TTLS data. */
public class EapTtlsInboundFragmentationHelper {
    private static final String TAG = EapTtlsInboundFragmentationHelper.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        FRAGMENTATION_STATUS_ACK,
        FRAGMENTATION_STATUS_ASSEMBLED,
        FRAGMENTATION_STATUS_INVALID
    })
    public @interface FragmentationStatus {}

    // ACK indicates that an inbound fragment has been processed and an ACK should be sent
    public static final int FRAGMENTATION_STATUS_ACK = 0;
    // ASSEMBLED indicates that fragments have been reasembeled and data can now be processed
    public static final int FRAGMENTATION_STATUS_ASSEMBLED = 1;
    // INVALID indicates some kind of failure likely due to an unexpected request or invalid data
    public static final int FRAGMENTATION_STATUS_INVALID = 2;

    @VisibleForTesting public boolean mIsAwaitingFragments = false;
    @VisibleForTesting public ByteBuffer mFragmentedData;

    /**
     * This method is responsible for processing incoming fragmented data (RFC5281#9.2.2)
     *
     * @param typeData the type data to process
     * @return a fragmentation status indicating the result of the process
     */
    public @FragmentationStatus int assembleInboundMessage(EapTtlsTypeData typeData) {
        if (!mIsAwaitingFragments) {
            if (typeData.isDataFragmented) {
                mIsAwaitingFragments = true;
                mFragmentedData = ByteBuffer.allocate(typeData.messageLength);
            } else {
                // If there is no fragmentation, simply return the full data in a byte array
                mFragmentedData = ByteBuffer.wrap(typeData.data);
                return FRAGMENTATION_STATUS_ASSEMBLED;
            }
        } else if (typeData.isLengthIncluded) {
            // the length bit MUST only be set on the first packet for a fragmented packet
            // (RFC5281#9.2.2)
            LOG.e(
                    TAG,
                    "Fragmentation failure: Received a second or greater fragmented request"
                            + " with the length bit set.");
            return FRAGMENTATION_STATUS_INVALID;
        }

        if (typeData.data.length > mFragmentedData.remaining()) {
            LOG.e(
                    TAG,
                    "Fragmentation failure: Received more data then declared and failed to"
                            + " reassemble fragment.");
            return FRAGMENTATION_STATUS_INVALID;
        }

        mFragmentedData.put(typeData.data);

        if (typeData.isDataFragmented) {
            return FRAGMENTATION_STATUS_ACK;
        }

        LOG.d(TAG, "Successfully assembled a fragment.");
        mIsAwaitingFragments = false;
        return FRAGMENTATION_STATUS_ASSEMBLED;
    }

    /**
     * Retrieves assembled inbound fragmented data
     *
     * @return a byte array containing an assembled inbound fragment
     */
    public byte[] getAssembledInboundFragment() {
        return mFragmentedData.array();
    }

    /**
     * Indicates whether a fragmentation session is currently in progress
     *
     * @return true if fragmentation is in progress
     */
    public boolean isAwaitingFragments() {
        return mIsAwaitingFragments;
    }
}
