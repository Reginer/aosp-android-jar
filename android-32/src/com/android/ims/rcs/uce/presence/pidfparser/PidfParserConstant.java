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

package com.android.ims.rcs.uce.presence.pidfparser;

/**
 * The constant of the pidf
 */
public class PidfParserConstant {
    /**
     * The UTF-8 encoding format
     */
    public static final String ENCODING_UTF_8 = "utf-8";

    /**
     * The service id of the capabilities discovery via presence.
     */
    public static final String SERVICE_ID_CAPS_DISCOVERY =
            "org.3gpp.urn:urn-7:3gpp-application.ims.iari.rcse.dp";

    /**
     * The service id of the VoLTE voice and video call.
     */
    public static final String SERVICE_ID_IpCall =
            "org.3gpp.urn:urn-7:3gpp-service.ims.icsi.mmtel";
}
