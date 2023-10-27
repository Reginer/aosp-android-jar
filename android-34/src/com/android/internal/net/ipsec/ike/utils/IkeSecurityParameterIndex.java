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
package com.android.internal.net.ipsec.ike.utils;

import android.util.CloseGuard;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a reserved IKE SPI.
 *
 * <p>This class is created to avoid assigning same SPI to the same address.
 *
 * <p>Objects of this type are used to track reserved IKE SPI to avoid SPI collision. They can be
 * obtained by calling {@link #allocateSecurityParameterIndex()} and must be released by calling
 * {@link #close()} when they are no longer needed.
 *
 * <p>This class MUST only be called from IKE worker thread. Methods that allocate and close IKE
 * SPI resource are not thread safe.
 *
 * <p>This class follows the pattern of {@link IpSecManager.SecurityParameterIndex}.
 */
public final class IkeSecurityParameterIndex implements AutoCloseable {
    // Package private Set to remember assigned IKE SPIs to avoid SPI collision. MUST be
    // accessed only by IkeSecurityParameterIndex and IkeSpiGenerator
    static final Set<Pair<InetAddress, Long>> sAssignedIkeSpis = new HashSet<>();

    private InetAddress mSourceAddress;
    private final long mSpi;
    private final CloseGuard mCloseGuard = new CloseGuard();

    // Package private constructor that MUST only be called from IkeSpiGenerator
    IkeSecurityParameterIndex(InetAddress sourceAddress, long spi) {
        mSourceAddress = sourceAddress;
        mSpi = spi;
        mCloseGuard.open("close");
    }

    /**
     * Get the underlying SPI held by this object.
     *
     * @return the underlying IKE SPI.
     */
    public long getSpi() {
        return mSpi;
    }

    /** Gets the current source address for this IkeSecurityParameterIndex. */
    @VisibleForTesting
    public InetAddress getSourceAddress() {
        return mSourceAddress;
    }

    /** Release an SPI that was previously reserved. */
    @Override
    public void close() {
        sAssignedIkeSpis.remove(new Pair<InetAddress, Long>(mSourceAddress, mSpi));
        mCloseGuard.close();
    }

    /** Check that the IkeSecurityParameterIndex was closed properly. */
    @Override
    protected void finalize() throws Throwable {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /** Migrate this IkeSecurityParameterIndex to the specified InetAddress. */
    public void migrate(InetAddress newSourceAddress) throws IOException {
        if (mSourceAddress.equals(newSourceAddress)) {
            // not actually migrating - this is a no op
            return;
        }

        if (!sAssignedIkeSpis.add(new Pair<>(newSourceAddress, mSpi))) {
            throw new IOException(
                    String.format(
                            "SPI colllision migrating IKE SPI <%s, %d> to <%s, %d>",
                            mSourceAddress.getHostAddress(),
                            mSpi,
                            newSourceAddress.getHostAddress(),
                            mSpi));
        }

        sAssignedIkeSpis.remove(new Pair<InetAddress, Long>(mSourceAddress, mSpi));
        mSourceAddress = newSourceAddress;
    }
}
