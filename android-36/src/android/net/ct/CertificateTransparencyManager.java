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
package android.net.ct;

import android.annotation.FlaggedApi;
import android.annotation.SystemService;

import com.android.net.ct.flags.Flags;

/**
 * Provides the primary API for the Certificate Transparency Manager.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_CERTIFICATE_TRANSPARENCY_SERVICE)
@SystemService(CertificateTransparencyManager.SERVICE_NAME)
public final class CertificateTransparencyManager {

    public static final String SERVICE_NAME = "certificate_transparency";

    /**
     * Creates a new CertificateTransparencyManager instance.
     *
     * @hide
     */
    public CertificateTransparencyManager() {}
}
