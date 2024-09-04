/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.ims.rcs.uce.request;

import android.net.Uri;
import android.util.Log;

import com.android.ims.rcs.uce.util.UceUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The class is used to store when the contact's capabilities request result is inconclusive.
 */
public class ContactThrottlingList {
    private static final String LOG_TAG = UceUtils.getLogPrefix() + "ThrottlingList";

    private static class ContactInfo {
        Uri mContactUri;
        int mSipCode;
        Instant mThrottleEndTimestamp;

        public ContactInfo(Uri contactUri, int sipCode, Instant timestamp) {
            mContactUri = contactUri;
            mSipCode = sipCode;
            mThrottleEndTimestamp = timestamp;
        }
    }

    private final int mSubId;
    private final List<ContactInfo> mThrottlingList = new ArrayList<>();

    public ContactThrottlingList(int subId) {
        mSubId = subId;
    }

    public synchronized void reset() {
        mThrottlingList.clear();
    }

    public synchronized void addToThrottlingList(List<Uri> uriList, int sipCode) {
        // Clean up the expired contacts before starting.
        cleanUpExpiredContacts();

        List<Uri> addToThrottlingList = getNotInThrottlingListUris(uriList);
        long expiration = UceUtils.getAvailabilityCacheExpiration(mSubId);
        Instant timestamp = Instant.now().plusSeconds(expiration);

        List<ContactInfo> list = addToThrottlingList.stream().map(uri ->
                new ContactInfo(uri, sipCode, timestamp)).collect(Collectors.toList());

        int previousSize = mThrottlingList.size();
        mThrottlingList.addAll(list);

        logd("addToThrottlingList: previous size=" + previousSize +
                ", current size=" + mThrottlingList.size() + ", expired time=" + timestamp);
    }

    private synchronized List<Uri> getNotInThrottlingListUris(List<Uri> uriList) {
        List<Uri> throttlingUris = mThrottlingList.stream().map(contactInfo ->
                contactInfo.mContactUri).collect(Collectors.toList());
        List<Uri> addToThrottlingUris = new ArrayList<>(uriList);
        addToThrottlingUris.removeAll(throttlingUris);
        return addToThrottlingUris;
    }

    public synchronized List<Uri> getInThrottlingListUris(List<Uri> uriList) {
        // Clean up the expired contacts before starting.
        cleanUpExpiredContacts();

        return uriList.stream()
                .filter(uri -> mThrottlingList.stream()
                    .anyMatch(contactInfo -> contactInfo.mContactUri.equals(uri)))
                    .collect(Collectors.toList());
    }

    /**
     * Clean up the expired contacts from the throttling list.
     */
    private synchronized void cleanUpExpiredContacts() {
        final int previousSize = mThrottlingList.size();
        List<ContactInfo> expiredContacts = mThrottlingList.stream()
                .filter(contactInfo -> Instant.now()
                        .isAfter(contactInfo.mThrottleEndTimestamp))
                        .collect(Collectors.toList());
        mThrottlingList.removeAll(expiredContacts);

        logd("cleanUpExpiredContacts: previous size=" + previousSize +
                ", current size=" + mThrottlingList.size());
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }
}
