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

package com.android.ims.rcs.uce.eab;

import android.annotation.NonNull;
import android.net.Uri;
import android.telephony.ims.RcsContactUceCapability;

import com.android.ims.rcs.uce.ControllerBase;
import com.android.ims.rcs.uce.UceController.UceControllerCallback;

import java.util.List;

/**
 * The interface related to the Enhanced Address Book.
 */
public interface EabController extends ControllerBase {
    /**
     * Retrieve the contacts' capabilities from the EAB database.
     */
    @NonNull List<EabCapabilityResult> getCapabilities(@NonNull List<Uri> uris);

    /**
     * Get contact capabilities from cache including expired capabilities.
     * @param uris the uri list to get contact capabilities from cache.
     * @return The contact capabilities of the given uri list.
     */
    @NonNull List<EabCapabilityResult> getCapabilitiesIncludingExpired(@NonNull List<Uri> uris);

    /**
     * Retrieve the contact's capabilities from the availability cache.
     */
    @NonNull EabCapabilityResult getAvailability(@NonNull Uri contactUri);

    /**
     * Retrieve the contact's capabilities from the availability cache.
     */
    @NonNull EabCapabilityResult getAvailabilityIncludingExpired(@NonNull Uri contactUri);

    /**
     * Save the capabilities to the EAB database.
     */
    void saveCapabilities(@NonNull List<RcsContactUceCapability> contactCapabilities);

    /**
     * Set the UceRequestCallback for sending the request to UceController.
     */
    void setUceRequestCallback(@NonNull UceControllerCallback c);
}
