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

import android.telephony.CarrierConfigManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;

import com.android.ims.rcs.uce.util.FeatureTags;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses the Android Carrier Configuration for service-description -> feature tag mappings and
 * tracks the IMS registration to pass in the
 * to determine capabilities for features that the framework does not manage.
 *
 * @see CarrierConfigManager.Ims#KEY_PUBLISH_SERVICE_DESC_FEATURE_TAG_MAP_OVERRIDE_STRING_ARRAY for
 * more information on the format of this key.
 */
public class PublishServiceDescTracker {
    private static final String TAG = "PublishServiceDescTracker";

    /**
     * Map from (service-id, version) to the feature tags required in registration required in order
     * for the RCS feature to be considered "capable".
     * <p>
     * See {@link
     * CarrierConfigManager.Ims#KEY_PUBLISH_SERVICE_DESC_FEATURE_TAG_MAP_OVERRIDE_STRING_ARRAY}
     * for more information on how this can be overridden/extended.
     */
    private static final Map<ServiceDescription, Set<String>> DEFAULT_SERVICE_DESCRIPTION_MAP;
    static {
        ArrayMap<ServiceDescription, Set<String>> map = new ArrayMap<>(21);
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHAT_IM,
                Collections.singleton(FeatureTags.FEATURE_TAG_CHAT_IM));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHAT_SESSION,
                Collections.singleton(FeatureTags.FEATURE_TAG_CHAT_SESSION));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_FT,
                Collections.singleton(FeatureTags.FEATURE_TAG_FILE_TRANSFER));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_FT_SMS,
                Collections.singleton(FeatureTags.FEATURE_TAG_FILE_TRANSFER_VIA_SMS));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_PRESENCE,
                Collections.singleton(FeatureTags.FEATURE_TAG_PRESENCE));
        // Same service-ID & version for MMTEL, but different description.
        map.put(ServiceDescription.SERVICE_DESCRIPTION_MMTEL_VOICE,
                Collections.singleton(FeatureTags.FEATURE_TAG_MMTEL));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_MMTEL_VOICE_VIDEO, new ArraySet<>(
                Arrays.asList(FeatureTags.FEATURE_TAG_MMTEL, FeatureTags.FEATURE_TAG_VIDEO)));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_GEOPUSH,
                Collections.singleton(FeatureTags.FEATURE_TAG_GEO_PUSH));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_GEOPUSH_SMS,
                Collections.singleton(FeatureTags.FEATURE_TAG_GEO_PUSH_VIA_SMS));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CALL_COMPOSER,
                Collections.singleton(FeatureTags.FEATURE_TAG_CALL_COMPOSER_ENRICHED_CALLING));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CALL_COMPOSER_MMTEL,
                Collections.singleton(FeatureTags.FEATURE_TAG_CALL_COMPOSER_VIA_TELEPHONY));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_POST_CALL,
                Collections.singleton(FeatureTags.FEATURE_TAG_POST_CALL));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_SHARED_MAP,
                Collections.singleton(FeatureTags.FEATURE_TAG_SHARED_MAP));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_SHARED_SKETCH,
                Collections.singleton(FeatureTags.FEATURE_TAG_SHARED_SKETCH));
        // A map has one key and one value. And if the same key is used, the value is replaced
        // with a new one.
        // The service description between SERVICE_DESCRIPTION_CHATBOT_SESSION and
        // SERVICE_DESCRIPTION_CHATBOT_SESSION_V1 is the same, but this is for botVersion=#1 .
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHATBOT_SESSION, new ArraySet<>(
                Arrays.asList(FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_SESSION,
                        FeatureTags.FEATURE_TAG_CHATBOT_VERSION_SUPPORTED)));
        // This is the service description for botVersion=#1,#2 .
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHATBOT_SESSION_V1, new ArraySet<>(
                Arrays.asList(FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_SESSION,
                        FeatureTags.FEATURE_TAG_CHATBOT_VERSION_V2_SUPPORTED)));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHATBOT_SESSION_V2, new ArraySet<>(
                Arrays.asList(FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_SESSION,
                        FeatureTags.FEATURE_TAG_CHATBOT_VERSION_V2_SUPPORTED)));
        // The service description between SERVICE_DESCRIPTION_CHATBOT_SA_SESSION and
        // SERVICE_DESCRIPTION_CHATBOT_SA_SESSION_V1 is the same, but this is for botVersion=#1 .
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHATBOT_SA_SESSION, new ArraySet<>(
                Arrays.asList(FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_STANDALONE_MSG,
                        FeatureTags.FEATURE_TAG_CHATBOT_VERSION_SUPPORTED)));
        // This is the service description for botVersion=#1,#2 .
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHATBOT_SA_SESSION_V1, new ArraySet<>(
                Arrays.asList(FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_STANDALONE_MSG,
                        FeatureTags.FEATURE_TAG_CHATBOT_VERSION_V2_SUPPORTED)));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHATBOT_SA_SESSION_V2, new ArraySet<>(
                Arrays.asList(FeatureTags.FEATURE_TAG_CHATBOT_COMMUNICATION_USING_STANDALONE_MSG,
                        FeatureTags.FEATURE_TAG_CHATBOT_VERSION_V2_SUPPORTED)));
        map.put(ServiceDescription.SERVICE_DESCRIPTION_CHATBOT_ROLE,
                Collections.singleton(FeatureTags.FEATURE_TAG_CHATBOT_ROLE));
        DEFAULT_SERVICE_DESCRIPTION_MAP = Collections.unmodifiableMap(map);
    }

    // Maps from ServiceDescription to the set of feature tags required to consider the feature
    // capable for PUBLISH.
    private final Map<ServiceDescription, Set<String>> mServiceDescriptionFeatureTagMap;
    // Handles cases where multiple ServiceDescriptions match a subset of the same feature tags.
    // This will be used to only include the feature tags where the
    private final Set<ServiceDescription> mServiceDescriptionPartialMatches = new ArraySet<>();
    // The capabilities calculated based off of the last IMS registration.
    private final Set<ServiceDescription> mRegistrationCapabilities = new ArraySet<>();
    // Contains the feature tags used in the last update to IMS registration.
    private Set<String> mRegistrationFeatureTags = new ArraySet<>();

    /**
     * Create a new instance, which incorporates any carrier config overrides of the default
     * mapping.
     */
    public static PublishServiceDescTracker fromCarrierConfig(String[] carrierConfig) {
        Map<ServiceDescription, Set<String>> elements = new ArrayMap<>();
        for (Map.Entry<ServiceDescription, Set<String>> entry :
                DEFAULT_SERVICE_DESCRIPTION_MAP.entrySet()) {

            elements.put(entry.getKey(), entry.getValue().stream()
                    .map(PublishServiceDescTracker::removeInconsistencies)
                    .collect(Collectors.toSet()));
        }
        if (carrierConfig != null) {
            for (String entry : carrierConfig) {
                String[] serviceDesc = entry.split("\\|");
                if (serviceDesc.length < 4) {
                    Log.w(TAG, "fromCarrierConfig: error parsing " + entry);
                    continue;
                }
                elements.put(new ServiceDescription(serviceDesc[0].trim(), serviceDesc[1].trim(),
                        serviceDesc[2].trim()), parseFeatureTags(serviceDesc[3]));
            }
        }
        return new PublishServiceDescTracker(elements);
    }

    /**
     * Parse the feature tags in the string, which will be separated by ";".
     */
    private static Set<String> parseFeatureTags(String featureTags) {
        // First, split feature tags into individual params
        String[] featureTagSplit = featureTags.split(";");
        if (featureTagSplit.length == 0) {
            return Collections.emptySet();
        }
        ArraySet<String> tags = new ArraySet<>(featureTagSplit.length);
        // Add each tag, first trying to remove inconsistencies in string matching that may cause
        // it to fail.
        for (String tag : featureTagSplit) {
            tags.add(removeInconsistencies(tag));
        }
        return tags;
    }

    private PublishServiceDescTracker(Map<ServiceDescription, Set<String>> serviceFeatureTagMap) {
        mServiceDescriptionFeatureTagMap = serviceFeatureTagMap;
        Set<ServiceDescription> keySet = mServiceDescriptionFeatureTagMap.keySet();
        // Go through and collect any ServiceDescriptions that have the same service-id & version
        // (but not the same description) and add them to a "partial match" list.
        for (ServiceDescription c : keySet) {
            mServiceDescriptionPartialMatches.addAll(keySet.stream()
                    .filter(s -> !Objects.equals(s, c) && isSimilar(c , s))
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Update the IMS registration associated with this tracker.
     * @param imsRegistration A List of feature tags that were associated with the last IMS
     *                        registration.
     */
    public void updateImsRegistration(Set<String> imsRegistration) {
        Set<String> sanitizedTags = imsRegistration.stream()
                // Ensure formatting passed in is the same as format stored here.
                .map(PublishServiceDescTracker::parseFeatureTags)
                // Each entry should only contain one feature tag.
                .map(s -> s.iterator().next()).collect(Collectors.toSet());
        // For aliased service descriptions (service-id && version is the same, but desc is
        // different), Keep a "score" of the number of feature tags that the service description
        // has associated with it. If another is found with a higher score, replace this one.
        Map<ServiceDescription, Integer> aliasedServiceDescScore = new ArrayMap<>();
        synchronized (mRegistrationCapabilities) {
            mRegistrationFeatureTags = imsRegistration;
            mRegistrationCapabilities.clear();
            for (Map.Entry<ServiceDescription, Set<String>> desc :
                    mServiceDescriptionFeatureTagMap.entrySet()) {
                boolean found = true;
                for (String tag : desc.getValue()) {
                    if (!sanitizedTags.contains(tag)) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    // There may be ambiguity with multiple entries having the same service-id &&
                    // version, but not the same description. In this case, we need to find any
                    // other entries with the same id & version and replace it with the new entry
                    // if it matches more "completely", i.e. match "mmtel;video" over "mmtel" if the
                    // registration set includes "mmtel;video". Skip putting that in for now and
                    // instead track the match with the most feature tags associated with it that
                    // are all found in the IMS registration.
                    if (mServiceDescriptionPartialMatches.contains(desc.getKey())) {
                        ServiceDescription aliasedDesc = aliasedServiceDescScore.keySet().stream()
                                .filter(s -> isSimilar(s, desc.getKey()))
                                .findFirst().orElse(null);
                        if (aliasedDesc != null) {
                            Integer prevEntrySize = aliasedServiceDescScore.get(aliasedDesc);
                            if (prevEntrySize != null
                                    // Overrides are added below the original map, so prefer those.
                                    && (prevEntrySize <= desc.getValue().size())) {
                                aliasedServiceDescScore.remove(aliasedDesc);
                                aliasedServiceDescScore.put(desc.getKey(), desc.getValue().size());
                            }
                        } else {
                            aliasedServiceDescScore.put(desc.getKey(), desc.getValue().size());
                        }
                    } else {
                        mRegistrationCapabilities.add(desc.getKey());
                    }
                }
            }
            // Collect the highest "scored" ServiceDescriptions and add themto registration caps.
            mRegistrationCapabilities.addAll(aliasedServiceDescScore.keySet());
        }
    }

    /**
     * @return A copy of the service-description pairs (service-id, version) that are associated
     * with the last IMS registration update in {@link #updateImsRegistration(Set)}
     */
    public Set<ServiceDescription> copyRegistrationCapabilities() {
        synchronized (mRegistrationCapabilities) {
            return new ArraySet<>(mRegistrationCapabilities);
        }
    }

    /**
     * @return A copy of the last update to the IMS feature tags via {@link #updateImsRegistration}.
     */
    public Set<String> copyRegistrationFeatureTags() {
        synchronized (mRegistrationCapabilities) {
            return new ArraySet<>(mRegistrationFeatureTags);
        }
    }

    /**
     * Dumps the current state of this tracker.
     */
    public void dump(PrintWriter printWriter) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("PublishServiceDescTracker");
        pw.increaseIndent();

        pw.println("ServiceDescription -> Feature Tag Map:");
        pw.increaseIndent();
        for (Map.Entry<ServiceDescription, Set<String>> entry :
                mServiceDescriptionFeatureTagMap.entrySet()) {
            pw.print(entry.getKey());
            pw.print("->");
            pw.println(entry.getValue());
        }
        pw.println();
        pw.decreaseIndent();

        if (!mServiceDescriptionPartialMatches.isEmpty()) {
            pw.println("Similar ServiceDescriptions:");
            pw.increaseIndent();
            for (ServiceDescription entry : mServiceDescriptionPartialMatches) {
                pw.println(entry);
            }
            pw.decreaseIndent();
        } else {
            pw.println("No Similar ServiceDescriptions:");
        }
        pw.println();

        pw.println("Last IMS registration update:");
        pw.increaseIndent();
        for (String entry : mRegistrationFeatureTags) {
            pw.println(entry);
        }
        pw.println();
        pw.decreaseIndent();

        pw.println("Capabilities:");
        pw.increaseIndent();
        for (ServiceDescription entry : mRegistrationCapabilities) {
            pw.println(entry);
        }
        pw.println();
        pw.decreaseIndent();

        pw.decreaseIndent();
    }

    /**
     * Test if two ServiceDescriptions are similar, meaning service-id && version are equal.
     */
    private static boolean isSimilar(ServiceDescription a, ServiceDescription b) {
        return (a.serviceId.equals(b.serviceId) && a.version.equals(b.version));
    }

    /**
     * Remove any formatting inconsistencies that could make string matching difficult.
     */
    private static String removeInconsistencies(String tag) {
        tag = tag.toLowerCase();
        tag = tag.replaceAll("\\s+", "");
        return tag;
    }
}
