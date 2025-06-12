/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.net.module.util;

import static android.net.DnsResolver.TYPE_A;
import static android.net.DnsResolver.TYPE_AAAA;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class for a DNS SVCB response packet.
 *
 * @hide
 */
public class DnsSvcbPacket extends DnsPacket {
    public static final int TYPE_SVCB = 64;

    private static final String TAG = DnsSvcbPacket.class.getSimpleName();

    /**
     * Creates a DnsSvcbPacket object from the given wire-format DNS packet.
     */
    private DnsSvcbPacket(@NonNull byte[] data) throws DnsPacket.ParseException {
        // If data is null, ParseException will be thrown.
        super(data);

        final int questions = mHeader.getRecordCount(QDSECTION);
        if (questions != 1) {
            throw new DnsPacket.ParseException("Unexpected question count " + questions);
        }
        final int nsType = mRecords[QDSECTION].get(0).nsType;
        if (nsType != TYPE_SVCB) {
            throw new DnsPacket.ParseException("Unexpected query type " + nsType);
        }
    }

    /**
     * Returns true if the DnsSvcbPacket is a DNS response.
     */
    public boolean isResponse() {
        return mHeader.isResponse();
    }

    /**
     * Returns whether the given protocol alpn is supported.
     */
    public boolean isSupported(@NonNull String alpn) {
        return findSvcbRecord(alpn) != null;
    }

    /**
     * Returns the TargetName associated with the given protocol alpn.
     * If the alpn is not supported, a null is returned.
     */
    @Nullable
    public String getTargetName(@NonNull String alpn) {
        final DnsSvcbRecord record = findSvcbRecord(alpn);
        return (record != null) ? record.getTargetName() : null;
    }

    /**
     * Returns the TargetName that associated with the given protocol alpn.
     * If the alpn is not supported, -1 is returned.
     */
    public int getPort(@NonNull String alpn) {
        final DnsSvcbRecord record = findSvcbRecord(alpn);
        return (record != null) ? record.getPort() : -1;
    }

    /**
     * Returns the IP addresses that support the given protocol alpn.
     * If the alpn is not supported, an empty list is returned.
     */
    @NonNull
    public List<InetAddress> getAddresses(@NonNull String alpn) {
        final DnsSvcbRecord record = findSvcbRecord(alpn);
        if (record == null) return Collections.EMPTY_LIST;

        // As per draft-ietf-dnsop-svcb-https-10#section-7.4 and draft-ietf-add-ddr-10#section-4,
        // if A/AAAA records are available in the Additional section, use the IP addresses in
        // those records instead of the IP addresses in ipv4hint/ipv6hint.
        final List<InetAddress> out = getAddressesFromAdditionalSection();
        if (out.size() > 0) return out;

        return record.getAddresses();
    }

    /**
     * Returns the value of SVCB key dohpath that associated with the given protocol alpn.
     * If the alpn is not supported, a null is returned.
     */
    @Nullable
    public String getDohPath(@NonNull String alpn) {
        final DnsSvcbRecord record = findSvcbRecord(alpn);
        return (record != null) ? record.getDohPath() : null;
    }

    /**
     * Returns the DnsSvcbRecord associated with the given protocol alpn.
     * If the alpn is not supported, a null is returned.
     */
    @Nullable
    private DnsSvcbRecord findSvcbRecord(@NonNull String alpn) {
        for (final DnsRecord record : mRecords[ANSECTION]) {
            if (record instanceof DnsSvcbRecord) {
                final DnsSvcbRecord svcbRecord = (DnsSvcbRecord) record;
                if (svcbRecord.getAlpns().contains(alpn)) {
                    return svcbRecord;
                }
            }
        }
        return null;
    }

    /**
     * Returns the IP addresses in additional section.
     */
    @NonNull
    private List<InetAddress> getAddressesFromAdditionalSection() {
        final List<InetAddress> out = new ArrayList<InetAddress>();
        if (mHeader.getRecordCount(ARSECTION) == 0) {
            return out;
        }
        for (final DnsRecord record : mRecords[ARSECTION]) {
            if (record.nsType != TYPE_A && record.nsType != TYPE_AAAA) {
                Log.d(TAG, "Found type other than A/AAAA in Additional section: " + record.nsType);
                continue;
            }
            try {
                out.add(InetAddress.getByAddress(record.getRR()));
            } catch (UnknownHostException e) {
                Log.w(TAG, "Failed to parse address");
            }
        }
        return out;
    }

    /**
     * Creates a DnsSvcbPacket object from the given wire-format DNS answer.
     */
    public static DnsSvcbPacket fromResponse(@NonNull byte[] data) throws DnsPacket.ParseException {
        DnsSvcbPacket out = new DnsSvcbPacket(data);
        if (!out.isResponse()) {
            throw new DnsPacket.ParseException("Not an answer packet");
        }
        return out;
    }
}
