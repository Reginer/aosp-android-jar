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

package com.android.ims.rcs.uce.presence.publish;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.telephony.ims.RcsContactPresenceTuple;
import android.text.TextUtils;

import java.util.Objects;

/**
 * Represents the "service-description" element in the PIDF XML for SIP PUBLISH of RCS capabilities.
 */
public class ServiceDescription {

    public static final ServiceDescription SERVICE_DESCRIPTION_CHAT_IM = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_CHAT_V1,
            "1.0" /*version*/,
            null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_CHAT_SESSION =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHAT_V2,
                    "2.0" /*version*/,
                    null /*description*/
            );

    public static final ServiceDescription SERVICE_DESCRIPTION_FT = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_FT,
            "1.0" /*version*/,
            null /*description*/
            );

    public static final ServiceDescription SERVICE_DESCRIPTION_FT_SMS = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_FT_OVER_SMS,
            "1.0" /*version*/,
            null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_PRESENCE = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_PRESENCE,
            "1.0" /*version*/,
            "Capabilities Discovery Service" /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_MMTEL_VOICE = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_MMTEL,
            "1.0" /*version*/,
            "Voice Service" /*description*/
    );

    // No change except for description (service capabilities generated elsewhere).
    public static final ServiceDescription SERVICE_DESCRIPTION_MMTEL_VOICE_VIDEO =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_MMTEL,
                    "1.0" /*version*/,
                    "Voice and Video Service" /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_GEOPUSH = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_GEO_PUSH,
            "1.0" /*version*/,
            null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_GEOPUSH_SMS = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_GEO_PUSH_VIA_SMS,
            "1.0" /*version*/,
            null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_CALL_COMPOSER =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CALL_COMPOSER,
                    "1.0" /*version*/,
                    null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_CALL_COMPOSER_MMTEL =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CALL_COMPOSER,
                    "2.0" /*version*/,
                    null /*description*/
            );

    public static final ServiceDescription SERVICE_DESCRIPTION_POST_CALL = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_POST_CALL,
            "1.0" /*version*/,
            null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_SHARED_MAP = new ServiceDescription(
            RcsContactPresenceTuple.SERVICE_ID_SHARED_MAP,
            "1.0" /*version*/,
            null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_SHARED_SKETCH =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_SHARED_SKETCH,
                    "1.0" /*version*/,
                    null /*description*/
    );
    // this is the same as below but redefined so that it can be a separate key entry
    // in DEFAULT_SERVICE_DESCRIPTION_MAP
    public static final ServiceDescription SERVICE_DESCRIPTION_CHATBOT_SESSION =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHATBOT,
                    "1.0" /*version*/,
                    null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_CHATBOT_SESSION_V1 =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHATBOT,
                    "1.0" /*version*/,
                    "Chatbot Session" /*description*/
            );

    public static final ServiceDescription SERVICE_DESCRIPTION_CHATBOT_SESSION_V2 =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHATBOT,
                    "2.0" /*version*/,
                    "Chatbot Session" /*description*/
    );
    // this is the same as below but redefined so that it can be a separate key entry
    // in DEFAULT_SERVICE_DESCRIPTION_MAP
    public static final ServiceDescription SERVICE_DESCRIPTION_CHATBOT_SA_SESSION =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHATBOT_STANDALONE.trim(),
                    "1.0" /*version*/,
                    null /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_CHATBOT_SA_SESSION_V1 =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHATBOT_STANDALONE.trim(),
                    "1.0" /*version*/,
                    "Chatbot Standalone" /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_CHATBOT_SA_SESSION_V2 =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHATBOT_STANDALONE.trim(),
                    "2.0" /*version*/,
                    "Chatbot Standalone" /*description*/
    );

    public static final ServiceDescription SERVICE_DESCRIPTION_CHATBOT_ROLE =
            new ServiceDescription(
                    RcsContactPresenceTuple.SERVICE_ID_CHATBOT_ROLE,
                    "1.0" /*version*/,
                    null /*description*/
            );

    /** Mandatory "service-id" element */
    public final @NonNull String serviceId;
    /** Mandatory "version" element */
    public final @NonNull String version;
    /** Optional "description" element */
    public final @Nullable String description;

    public ServiceDescription(String serviceId, String version, String description) {
        this.serviceId = serviceId;
        this.version = version;
        this.description = description;
    }

    public RcsContactPresenceTuple.Builder getTupleBuilder() {
        RcsContactPresenceTuple.Builder b = new RcsContactPresenceTuple.Builder(
                RcsContactPresenceTuple.TUPLE_BASIC_STATUS_OPEN, serviceId, version);
        if (!TextUtils.isEmpty(description)) {
            b.setServiceDescription(description);
        }
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDescription that = (ServiceDescription) o;
        return serviceId.equals(that.serviceId)
                && version.equals(that.version)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, version, description);
    }

    @Override
    public String toString() {
        return "(id=" + serviceId + ", v=" + version + ", d=" + description + ')';
    }
}
