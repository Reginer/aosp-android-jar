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

import android.util.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;

/** This class provides method to allocate IKE SPI */
public class IkeSpiGenerator {
    private final SecureRandom mRandom;

    /**
     * Constructor of IkeSpiGenerator
     *
     * @param secureRandom the seed to generate SPI
     */
    public IkeSpiGenerator(RandomnessFactory randomnessFactory) {
        SecureRandom random = randomnessFactory.getRandom();
        mRandom = random == null ? new SecureRandom() : random;
    }

    /**
     * Get a new IKE SPI and maintain the reservation.
     *
     * <p>If this instance is constructed by a spiGenerator, this method will try to allocate SPI
     * once for the return value of spiGenerator#nextLong. Otherwise, this methods will try multiple
     * times until it finds an avalaible IKE SPI.
     *
     * @return an instance of IkeSecurityParameterIndex.
     */
    public IkeSecurityParameterIndex allocateSpi(InetAddress sourceAddress) throws IOException {
        long spi;
        do {
            spi = mRandom.nextLong();
        } while (spi == 0L
                || !IkeSecurityParameterIndex.sAssignedIkeSpis.add(
                        new Pair<InetAddress, Long>(sourceAddress, spi)));
        return new IkeSecurityParameterIndex(sourceAddress, spi);
    }

    /**
     * Get a new IKE SPI and maintain the reservation.
     *
     * @return an instance of IkeSecurityParameterIndex.
     */
    public IkeSecurityParameterIndex allocateSpi(InetAddress sourceAddress, long requestedSpi)
            throws IOException {
        if (IkeSecurityParameterIndex.sAssignedIkeSpis.add(
                new Pair<InetAddress, Long>(sourceAddress, requestedSpi))) {
            return new IkeSecurityParameterIndex(sourceAddress, requestedSpi);
        }

        throw new IOException(
                "Failed to generate IKE SPI for "
                        + requestedSpi
                        + " with source address "
                        + sourceAddress.getHostAddress());
    }
}
