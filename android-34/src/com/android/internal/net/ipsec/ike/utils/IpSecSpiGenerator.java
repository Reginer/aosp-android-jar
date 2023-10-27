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

import android.annotation.NonNull;
import android.net.IpSecManager;
import android.net.IpSecManager.ResourceUnavailableException;
import android.net.IpSecManager.SecurityParameterIndex;
import android.net.IpSecManager.SpiUnavailableException;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Objects;

/** This class provides method to allocate IPsec SPI */
public class IpSecSpiGenerator {
    private final IpSecManager mIpSecManager;
    private final SecureRandom mRandom;

    /**
     * Constructor of IpSecSpiGenerator
     *
     * @param secureRandom the seed to generate SPI
     */
    public IpSecSpiGenerator(
            @NonNull IpSecManager ipSecManager, RandomnessFactory randomnessFactory) {
        Objects.requireNonNull(ipSecManager);
        mIpSecManager = ipSecManager;
        mRandom = randomnessFactory.getRandom();
    }

    /**
     * Get a new IPsec SPI and maintain the reservation.
     *
     * <p>If this instance is constructed by a spiGenerator, this method will try to allocate SPI
     * once for the return value of spiGenerator#nextInt. Otherwise the SPI will be allocated by the
     * Kernel.
     *
     * @return an instance of SecurityParameterIndex.
     */
    public SecurityParameterIndex allocateSpi(InetAddress sourceAddress)
            throws SpiUnavailableException, ResourceUnavailableException {
        if (mRandom == null) {
            return mIpSecManager.allocateSecurityParameterIndex(sourceAddress);
        } else {
            return mIpSecManager.allocateSecurityParameterIndex(sourceAddress, mRandom.nextInt());
        }
    }

    /**
     * Get a new IPsec SPI and maintain the reservation.
     *
     * @return an instance of SecurityParameterIndex.
     */
    public SecurityParameterIndex allocateSpi(InetAddress sourceAddress, int requestedSpi)
            throws SpiUnavailableException, ResourceUnavailableException {
        return mIpSecManager.allocateSecurityParameterIndex(sourceAddress, requestedSpi);
    }
}
