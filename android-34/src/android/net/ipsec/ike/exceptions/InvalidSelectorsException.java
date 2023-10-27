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
package android.net.ipsec.ike.exceptions;

import android.annotation.NonNull;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

import java.util.Objects;

/**
 * This exception is thrown if the remote server received an IPsec packet with mismatched selectors.
 *
 * <p>This exception indicates that the remote server received an IPsec packet whose selectors do
 * not match those of the IPsec SA on which it was delivered. The error data contains the start of
 * the offending packet (as in ICMP messages), which is the IP header plus the first 64 bits of the
 * original datagram's data.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.10.1">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class InvalidSelectorsException extends IkeProtocolException {
    // Minimum IP header length plus 64 bits
    private static final int EXPECTED_ERROR_DATA_LEN_MIN = 28;

    private final int mIpSecSpi;
    private final byte[] mIpSecPacketInfo;

    /**
     * Construct an instance of InvalidSelectorsException.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}
     *
     * @param spi the SPI of the IPsec SA that delivered the packet with mismtached selectors.
     * @param packetInfo the IP header plus the first 64 bits of the packet that has mismtached
     *     selectors.
     */
    public InvalidSelectorsException(int spi, @NonNull byte[] packetInfo) {
        super(ERROR_TYPE_INVALID_SELECTORS, packetInfo);
        Objects.requireNonNull(packetInfo, "packetInfo is null");
        mIpSecSpi = spi;
        mIpSecPacketInfo = packetInfo.clone();
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN_MIN <= dataLen;
    }

    /** Returns the SPI of the IPsec SA that delivered the packet with mismtached selectors. */
    public int getIpSecSpi() {
        return mIpSecSpi;
    }

    /** Returns the IP header plus the first 64 bits of the packet that has mismtached selectors. */
    @NonNull
    public byte[] getIpSecPacketInfo() {
        return mIpSecPacketInfo;
    }
}
