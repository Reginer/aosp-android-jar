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

import android.net.Uri;
import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactPresenceTuple.BasicStatus;
import android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities;
import android.telephony.ims.RcsContactUceCapability;
import android.text.TextUtils;

import com.android.ims.rcs.uce.presence.pidfparser.capabilities.Audio;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.Duplex;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.ServiceCaps;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.Video;
import com.android.ims.rcs.uce.presence.pidfparser.omapres.Description;
import com.android.ims.rcs.uce.presence.pidfparser.omapres.ServiceDescription;
import com.android.ims.rcs.uce.presence.pidfparser.omapres.ServiceId;
import com.android.ims.rcs.uce.presence.pidfparser.omapres.Version;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Basic;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Contact;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Presence;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Status;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Timestamp;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Tuple;

import java.util.Arrays;
import java.util.List;

/**
 * The utils to help the PIDF parsing process.
 */
public class PidfParserUtils {

    /*
     * The resource terminated reason with NOT FOUND
     */
    private static String[] REQUEST_RESULT_REASON_NOT_FOUND = { "noresource", "rejected" };

    /**
     * Convert the given class RcsContactUceCapability to the class Presence.
     */
    static Presence getPresence(RcsContactUceCapability capabilities) {
        // Create "presence" element which is the root element of the pidf
        Presence presence = new Presence(capabilities.getContactUri());

        List<RcsContactPresenceTuple> tupleList = capabilities.getCapabilityTuples();
        if (tupleList == null || tupleList.isEmpty()) {
            return presence;
        }

        for (RcsContactPresenceTuple presenceTuple : tupleList) {
            Tuple tupleElement = getTupleElement(presenceTuple);
            if (tupleElement != null) {
                presence.addTuple(tupleElement);
            }
        }

        return presence;
    }

    /**
     * Convert the class from RcsContactPresenceTuple to the class Tuple
     */
    private static Tuple getTupleElement(RcsContactPresenceTuple presenceTuple) {
        if (presenceTuple == null) {
            return null;
        }
        Tuple tupleElement = new Tuple();

        // status element
        handleTupleStatusElement(tupleElement, presenceTuple.getStatus());

        // service description element
        handleTupleServiceDescriptionElement(tupleElement, presenceTuple.getServiceId(),
                presenceTuple.getServiceVersion(), presenceTuple.getServiceDescription());

        // service capabilities element
        handleServiceCapsElement(tupleElement, presenceTuple.getServiceCapabilities());

        // contact element
        handleTupleContactElement(tupleElement, presenceTuple.getContactUri());

        return tupleElement;
    }

    private static void handleTupleContactElement(Tuple tupleElement, Uri uri) {
        if (uri == null) {
            return;
        }
        Contact contactElement = new Contact();
        contactElement.setContact(uri.toString());
        tupleElement.setContact(contactElement);
    }

    private static void handleTupleStatusElement(Tuple tupleElement, @BasicStatus String status) {
        if (TextUtils.isEmpty(status)) {
            return;
        }
        Basic basicElement = new Basic(status);
        Status statusElement = new Status();
        statusElement.setBasic(basicElement);
        tupleElement.setStatus(statusElement);
    }

    private static void handleTupleServiceDescriptionElement(Tuple tupleElement, String serviceId,
            String version, String description) {
        ServiceId serviceIdElement = null;
        Version versionElement = null;
        Description descriptionElement = null;

        // init serviceId element
        if (!TextUtils.isEmpty(serviceId)) {
            serviceIdElement = new ServiceId(serviceId);
        }

        // init version element
        if (!TextUtils.isEmpty(version)) {
            String[] versionAry = version.split("\\.");
            if (versionAry != null && versionAry.length == 2) {
                int majorVersion = Integer.parseInt(versionAry[0]);
                int minorVersion = Integer.parseInt(versionAry[1]);
                versionElement = new Version(majorVersion, minorVersion);
            }
        }

        // init description element
        if (!TextUtils.isEmpty(description)) {
            descriptionElement = new Description(description);
        }

        // Add the Service Description element into the tuple
        if (serviceIdElement != null && versionElement != null) {
            ServiceDescription serviceDescription = new ServiceDescription();
            serviceDescription.setServiceId(serviceIdElement);
            serviceDescription.setVersion(versionElement);
            if (descriptionElement != null) {
                serviceDescription.setDescription(descriptionElement);
            }
            tupleElement.setServiceDescription(serviceDescription);
        }
    }

    private static void handleServiceCapsElement(Tuple tupleElement,
            ServiceCapabilities serviceCaps) {
        if (serviceCaps == null) {
            return;
        }

        ServiceCaps servCapsElement = new ServiceCaps();

        // Audio and Video element
        Audio audioElement = new Audio(serviceCaps.isAudioCapable());
        Video videoElement = new Video(serviceCaps.isVideoCapable());
        servCapsElement.addElement(audioElement);
        servCapsElement.addElement(videoElement);

        // Duplex element
        List<String> supportedDuplexModes = serviceCaps.getSupportedDuplexModes();
        List<String> UnsupportedDuplexModes = serviceCaps.getUnsupportedDuplexModes();
        if ((supportedDuplexModes != null && !supportedDuplexModes.isEmpty()) ||
                (UnsupportedDuplexModes != null && !UnsupportedDuplexModes.isEmpty())) {
            Duplex duplex = new Duplex();
            if (!supportedDuplexModes.isEmpty()) {
                duplex.addSupportedType(supportedDuplexModes.get(0));
            }
            if (!UnsupportedDuplexModes.isEmpty()) {
                duplex.addNotSupportedType(UnsupportedDuplexModes.get(0));
            }
            servCapsElement.addElement(duplex);
        }

        tupleElement.setServiceCaps(servCapsElement);
    }

    /**
     * Get the status from the given tuple.
     */
    public static String getTupleStatus(Tuple tuple) {
        if (tuple == null) {
            return null;
        }
        Status status = tuple.getStatus();
        if (status != null) {
            Basic basic = status.getBasic();
            if (basic != null) {
                return basic.getValue();
            }
        }
        return null;
    }

    /**
     * Get the service Id from the given tuple.
     */
    public static String getTupleServiceId(Tuple tuple) {
        if (tuple == null) {
            return null;
        }
        ServiceDescription servDescription = tuple.getServiceDescription();
        if (servDescription != null) {
            ServiceId serviceId = servDescription.getServiceId();
            if (serviceId != null) {
                return serviceId.getValue();
            }
        }
        return null;
    }

    /**
     * Get the service version from the given tuple.
     */
    public static String getTupleServiceVersion(Tuple tuple) {
        if (tuple == null) {
            return null;
        }
        ServiceDescription servDescription = tuple.getServiceDescription();
        if (servDescription != null) {
            Version version = servDescription.getVersion();
            if (version != null) {
                return version.getValue();
            }
        }
        return null;
    }

    /**
     * Get the service description from the given tuple.
     */
    public static String getTupleServiceDescription(Tuple tuple) {
        if (tuple == null) {
            return null;
        }
        ServiceDescription servDescription = tuple.getServiceDescription();
        if (servDescription != null) {
            Description description = servDescription.getDescription();
            if (description != null) {
                return description.getValue();
            }
        }
        return null;
    }

    /**
     * Get the contact from the given tuple.
     */
    public static String getTupleContact(Tuple tuple) {
        if (tuple == null) {
            return null;
        }
        Contact contact = tuple.getContact();
        if (contact != null) {
            return contact.getContact();
        }
        return null;
    }

    /**
     * Get the timestamp from the given tuple.
     */
    public static String getTupleTimestamp(Tuple tuple) {
        if (tuple == null) {
            return null;
        }
        Timestamp timestamp = tuple.getTimestamp();
        if (timestamp != null) {
            return timestamp.getValue();
        }
        return null;
    }

    /**
     * Get the malformed status from the given tuple.
     */
    public static boolean getTupleMalformedStatus(Tuple tuple) {
        if (tuple == null) {
            return false;
        }
        return tuple.getMalformed();
    }

    /**
     * Get the terminated capability which disable all the capabilities.
     */
    public static RcsContactUceCapability getTerminatedCapability(Uri contact, String reason) {
        if (reason == null) reason = "";
        int requestResult = (Arrays.stream(REQUEST_RESULT_REASON_NOT_FOUND)
                    .anyMatch(reason::equalsIgnoreCase) == true) ?
                            RcsContactUceCapability.REQUEST_RESULT_NOT_FOUND :
                                    RcsContactUceCapability.REQUEST_RESULT_UNKNOWN;

        RcsContactUceCapability.PresenceBuilder builder =
                new RcsContactUceCapability.PresenceBuilder(
                        contact, RcsContactUceCapability.SOURCE_TYPE_NETWORK, requestResult);
        return builder.build();
    }

    /**
     * Get the RcsContactUceCapability instance which the request result is NOT FOUND.
     */
    public static RcsContactUceCapability getNotFoundContactCapabilities(Uri contact) {
        RcsContactUceCapability.PresenceBuilder builder =
                new RcsContactUceCapability.PresenceBuilder(contact,
                        RcsContactUceCapability.SOURCE_TYPE_NETWORK,
                        RcsContactUceCapability.REQUEST_RESULT_NOT_FOUND);
        return builder.build();
    }
}
