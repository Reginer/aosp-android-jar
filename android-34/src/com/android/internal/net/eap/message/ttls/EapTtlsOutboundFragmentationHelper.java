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

import com.android.internal.annotations.VisibleForTesting;

import java.nio.ByteBuffer;

/** The outbound fragmentation helper is responsible for fragmenting outbound EAP-TTLS data */
public class EapTtlsOutboundFragmentationHelper {
    private static final String TAG = EapTtlsInboundFragmentationHelper.class.getSimpleName();

    private static final int DEFAULT_FRAGMENTATION_SIZE = 1024;

    // Defines the outbound fragment size
    private final int mFragmentSize;
    private ByteBuffer mFragmentedData;

    // TODO(b/165668196): Modify outbound fragmentation helper to be per-message in EAP-TTLS
    public EapTtlsOutboundFragmentationHelper() {
        this(DEFAULT_FRAGMENTATION_SIZE);
    }

    /**
     * Sets a specific fragment size for the fragmentation helper instance. This should only be used
     * for testing.
     *
     * @param fragmentSize the fragment size to set
     */
    @VisibleForTesting
    public EapTtlsOutboundFragmentationHelper(int fragmentSize) {
        mFragmentSize = fragmentSize;
    }

    /**
     * Prepares an outbound message for fragmentation
     *
     * @param data the data to fragment
     */
    public void setupOutboundFragmentation(byte[] data) {
        mFragmentedData = ByteBuffer.wrap(data);
    }

    /**
     * Returns fragmented data ready to be sent to the server
     *
     * @return a fragmentation result which contains the fragmented data as well as a boolean
     *     indicating whether more fragments will follow
     * @throws IllegalStateException if this is called when a fragmentation session is not in
     *     progress
     */
    public FragmentationResult getNextOutboundFragment() throws IllegalStateException {
        if (mFragmentedData == null || !mFragmentedData.hasRemaining()) {
            throw new IllegalStateException(
                    "Error producing next outbound fragment. No fragmented packets are currently"
                            + " being processed.");
        }
        // If the data in the buffer is larger than the fragmentSize, produce a fragment of
        // fragmentSize. Otherwise, return all the remaining data
        int outboundDataSize = Math.min(mFragmentSize, mFragmentedData.remaining());
        byte[] outboundData = new byte[outboundDataSize];
        mFragmentedData.get(outboundData);

        return new FragmentationResult(outboundData, mFragmentedData.hasRemaining());
    }

    /**
     * Indicates whether there is additional outbound fragmented data to be sent
     *
     * <p>This should be called in case the server does not send an ack but sends a regular request
     * in response to a fragment. This will allow the state machine to detect an unexpected request
     * error.
     *
     * @return a boolean indicating whether there are outbound fragments that need to be sent
     */
    public boolean hasRemainingFragments() {
        return mFragmentedData != null && mFragmentedData.hasRemaining();
    }

    /** FragmentationResult encapsulates the results of outbound fragmentation processing */
    public class FragmentationResult {

        public final boolean hasRemainingFragments;
        public final byte[] fragmentedData;

        public FragmentationResult(byte[] fragmentedData, boolean hasRemainingFragments) {
            this.fragmentedData = fragmentedData;
            this.hasRemainingFragments = hasRemainingFragments;
        }
    }
}
