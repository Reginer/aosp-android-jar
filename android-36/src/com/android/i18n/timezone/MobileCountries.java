/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.i18n.timezone;

import static java.util.stream.Collectors.toUnmodifiableSet;

import android.annotation.NonNull;

import com.android.icu.Flags;

import java.util.Objects;
import java.util.Set;

/**
 * Information about the countries where an MCC operates.
 *
 * @hide
 */
@libcore.api.CorePlatformApi
@android.annotation.FlaggedApi(Flags.FLAG_TELEPHONY_LOOKUP_MCC_EXTENSION)
public final class MobileCountries {

    private final String mcc;
    private final String defaultCountryIsoCode;
    private final Set<String> countryIsoCodes;

    public static MobileCountries create(String mcc, Set<String> countryIsoCodes,
            String defaultCountryIsoCode) {
        Objects.requireNonNull(mcc);
        Objects.requireNonNull(countryIsoCodes);
        Objects.requireNonNull(defaultCountryIsoCode);

        Set<String> normalizedCountryIsos = countryIsoCodes.stream()
                .map(XmlUtils::normalizeCountryIso)
                .collect(toUnmodifiableSet());
        return new MobileCountries(Objects.requireNonNull(mcc), normalizedCountryIsos,
                XmlUtils.normalizeCountryIso(defaultCountryIsoCode));
    }

    private MobileCountries(String mcc, Set<String> countryIsoCodes,
            String defaultCountryIsoCode) {
        if (!countryIsoCodes.contains(Objects.requireNonNull(defaultCountryIsoCode))) {
            throw new IllegalArgumentException(
                    "The default country ISO code was not found in the set of country ISO codes");
        }
        this.mcc = Objects.requireNonNull(mcc);
        this.countryIsoCodes = Objects.requireNonNull(countryIsoCodes);
        this.defaultCountryIsoCode = Objects.requireNonNull(defaultCountryIsoCode);
    }

    /**
     * Returns the MCC of the network.
     */
    @libcore.api.CorePlatformApi
    @NonNull
    public String getMcc() {
        return mcc;
    }

    /**
     * Returns the countries in which this MCC is used as an ISO 3166 alpha-2 (lower case).
     */
    @libcore.api.CorePlatformApi
    @NonNull
    public Set<String> getCountryIsoCodes() {
        return countryIsoCodes;
    }

    /**
     * Returns the default country for this MCC as an ISO 3166 alpha-2 (lower case).
     */
    @libcore.api.CorePlatformApi
    @NonNull
    public String getDefaultCountryIsoCode() {
        return defaultCountryIsoCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MobileCountries that) {
            return mcc.equals(that.mcc) && countryIsoCodes.equals(that.countryIsoCodes)
                    && defaultCountryIsoCode.equals(that.defaultCountryIsoCode);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mcc, countryIsoCodes, defaultCountryIsoCode);
    }

    @Override
    public String toString() {
        return "MobileCountries{"
                + "mcc=" + mcc
                + ", countryIsoCodes='" + countryIsoCodes
                + ", defaultCountryIsoCode='" + defaultCountryIsoCode + '\''
                + '}';
    }
}
