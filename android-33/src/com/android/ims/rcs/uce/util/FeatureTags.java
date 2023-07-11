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

package com.android.ims.rcs.uce.util;

import android.net.Uri;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.OptionsBuilder;
import android.telephony.ims.RcsContactUceCapability.SourceType;

import java.util.List;
import java.util.Set;

/**
 * The util class of the feature tags.
 */
public class FeatureTags {

    public static final String FEATURE_TAG_STANDALONE_MSG =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-"
                    + "service.ims.icsi.oma.cpm.msg,urn%3Aurn-7%3A3gpp-"
                    + "service.ims.icsi.oma.cpm.largemsg,urn%3Aurn-7%3A3gpp-"
                    + "service.ims.icsi.oma.cpm.deferred\";+g.gsma.rcs.cpm.pager-large";

    public static final String FEATURE_TAG_CHAT_IM =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.im\"";

    public static final String FEATURE_TAG_CHAT_SESSION =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.session\"";

    public static final String FEATURE_TAG_FILE_TRANSFER =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.fthttp\"";

    public static final String FEATURE_TAG_FILE_TRANSFER_VIA_SMS =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.ftsms\"";

    public static final String FEATURE_TAG_CALL_COMPOSER_ENRICHED_CALLING =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.gsma.callcomposer\"";

    public static final String FEATURE_TAG_CALL_COMPOSER_VIA_TELEPHONY = "+g.gsma.callcomposer";

    public static final String FEATURE_TAG_POST_CALL =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.gsma.callunanswered\"";

    public static final String FEATURE_TAG_SHARED_MAP =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.gsma.sharedmap\"";

    public static final String FEATURE_TAG_SHARED_SKETCH =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.gsma.sharedsketch\"";

    public static final String FEATURE_TAG_GEO_PUSH =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.geopush\"";

    public static final String FEATURE_TAG_GEO_PUSH_VIA_SMS =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.geosms\"";

    public static final String FEATURE_TAG_CHATBOT_COMMUNICATION_USING_SESSION =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.chatbot\"";

    public static final String FEATURE_TAG_CHATBOT_COMMUNICATION_USING_STANDALONE_MSG =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.chatbot.sa\"";

    public static final String FEATURE_TAG_CHATBOT_VERSION_SUPPORTED =
            "+g.gsma.rcs.botversion=\"#=1\"";

    public static final String FEATURE_TAG_CHATBOT_VERSION_V2_SUPPORTED =
            "+g.gsma.rcs.botversion=\"#=1,#=2\"";

    public static final String FEATURE_TAG_CHATBOT_ROLE = "+g.gsma.rcs.isbot";

    public static final String FEATURE_TAG_MMTEL =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel\"";

    public static final String FEATURE_TAG_VIDEO = "video";

    public static final String FEATURE_TAG_PRESENCE =
            "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.dp\"";

    /**
     * Add the feature tags to the given RcsContactUceCapability OPTIONS builder.
     * @param optionsBuilder The OptionsBuilder to add the feature tags
     * @param mmtelAudioSupport If the audio capability is supported
     * @param mmtelVideoSupport If the video capability is supported
     * @param presenceSupport If presence is also supported
     * @param callComposerSupport If call composer via telephony is supported
     * @param registrationTags The other feature tags included in the IMS registration.
     */
    public static void addFeatureTags(final OptionsBuilder optionsBuilder,
            boolean mmtelAudioSupport, boolean mmtelVideoSupport,
            boolean presenceSupport, boolean callComposerSupport, Set<String> registrationTags) {
        if (presenceSupport) {
            registrationTags.add(FEATURE_TAG_PRESENCE);
        } else {
            registrationTags.remove(FEATURE_TAG_PRESENCE);
        }
        if (mmtelAudioSupport && mmtelVideoSupport) {
            registrationTags.add(FEATURE_TAG_MMTEL);
            registrationTags.add(FEATURE_TAG_VIDEO);
        } else if (mmtelAudioSupport) {
            registrationTags.add(FEATURE_TAG_MMTEL);
            registrationTags.remove(FEATURE_TAG_VIDEO);
        } else {
            registrationTags.remove(FEATURE_TAG_MMTEL);
            registrationTags.remove(FEATURE_TAG_VIDEO);
        }
        if (callComposerSupport) {
            registrationTags.add(FEATURE_TAG_CALL_COMPOSER_VIA_TELEPHONY);
        } else {
            registrationTags.remove(FEATURE_TAG_CALL_COMPOSER_VIA_TELEPHONY);
        }
        if (!registrationTags.isEmpty()) {
            optionsBuilder.addFeatureTags(registrationTags);
        }
    }

    /**
     * Get RcsContactUceCapabilities from the given feature tags.
     */
    public static RcsContactUceCapability getContactCapability(Uri contact,
            @SourceType int sourceType, List<String> featureTags) {
        OptionsBuilder builder = new OptionsBuilder(contact, sourceType);
        builder.setRequestResult(RcsContactUceCapability.REQUEST_RESULT_FOUND);
        featureTags.forEach(feature -> builder.addFeatureTag(feature));
        return builder.build();
    }
}
