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

import android.annotation.Nullable;
import android.net.Uri;
import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities;
import android.telephony.ims.RcsContactUceCapability;
import android.text.TextUtils;
import android.util.Log;


import com.android.ims.rcs.uce.presence.pidfparser.capabilities.Audio;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.CapsConstant;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.Duplex;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.ServiceCaps;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.Video;
import com.android.ims.rcs.uce.presence.pidfparser.omapres.OmaPresConstant;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Basic;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.PidfConstant;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Presence;
import com.android.ims.rcs.uce.presence.pidfparser.pidf.Tuple;
import com.android.ims.rcs.uce.presence.pidfparser.RcsContactUceCapabilityWrapper;
import com.android.ims.rcs.uce.util.UceUtils;
import com.android.internal.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Convert between the class RcsContactUceCapability and the pidf format.
 */
public class PidfParser {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "PidfParser";

    private static final Pattern PIDF_PATTERN = Pattern.compile("\t|\r|\n");

    /**
     * Testing interface used to get the timestamp.
     */
    @VisibleForTesting
    public interface TimestampProxy {
        Instant getTimestamp();
    }

    // The timestamp proxy to create the local timestamp.
    private static final TimestampProxy sLocalTimestampProxy = () -> Instant.now();

    // Override timestamp proxy for testing only.
    private static TimestampProxy sOverrideTimestampProxy;

    @VisibleForTesting
    public static void setTimestampProxy(TimestampProxy proxy) {
        sOverrideTimestampProxy = proxy;
    }

    private static TimestampProxy getTimestampProxy() {
        return (sOverrideTimestampProxy != null) ? sOverrideTimestampProxy : sLocalTimestampProxy;
    }

    /**
     * Convert the RcsContactUceCapability to the string of pidf.
     */
    public static String convertToPidf(RcsContactUceCapability capabilities) {
        StringWriter pidfWriter = new StringWriter();
        try {
            // Init the instance of the XmlSerializer.
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlSerializer serializer = factory.newSerializer();

            // setup output and namespace
            serializer.setOutput(pidfWriter);
            serializer.setPrefix("", PidfConstant.NAMESPACE);
            serializer.setPrefix("op", OmaPresConstant.NAMESPACE);
            serializer.setPrefix("caps", CapsConstant.NAMESPACE);

            // Get the Presence element
            Presence presence = PidfParserUtils.getPresence(capabilities);

            // Start serializing.
            serializer.startDocument(PidfParserConstant.ENCODING_UTF_8, true);
            presence.serialize(serializer);
            serializer.endDocument();
            serializer.flush();

        } catch (XmlPullParserException parserEx) {
            parserEx.printStackTrace();
            return null;
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return null;
        }
        return pidfWriter.toString();
    }

    /**
     * Get the RcsContactUceCapabilityWrapper from the given PIDF xml format.
     */
    public static @Nullable RcsContactUceCapabilityWrapper getRcsContactUceCapabilityWrapper(
            String pidf) {
        if (TextUtils.isEmpty(pidf)) {
            Log.w(LOG_TAG, "getRcsContactUceCapabilityWrapper: The given pidf is empty");
            return null;
        }

        // Filter the newline characters
        Matcher matcher = PIDF_PATTERN.matcher(pidf);
        String formattedPidf = matcher.replaceAll("");
        if (TextUtils.isEmpty(formattedPidf)) {
            Log.w(LOG_TAG, "getRcsContactUceCapabilityWrapper: The formatted pidf is empty");
            return null;
        }

        Reader reader = null;
        try {
            // Init the instance of the parser
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            reader = new StringReader(formattedPidf);
            parser.setInput(reader);

            // Start parsing
            Presence presence = parsePidf(parser);

            // Convert from the Presence to the RcsContactUceCapabilityWrapper
            return convertToRcsContactUceCapability(presence);

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static Presence parsePidf(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        Presence presence = null;
        int nextType = parser.next();
        boolean findPresenceTag = false;
        do {
            // Find the Presence start tag
            if (nextType == XmlPullParser.START_TAG
                    && Presence.ELEMENT_NAME.equals(parser.getName())) {
                findPresenceTag = true;
                presence = new Presence();
                presence.parse(parser);
                break;
            }
            nextType = parser.next();
        } while(nextType != XmlPullParser.END_DOCUMENT);

        if (!findPresenceTag) {
            Log.w(LOG_TAG, "parsePidf: The presence start tag not found.");
        }

        return presence;
    }

    /*
     * Convert the given Presence to the RcsContactUceCapabilityWrapper
     */
    private static RcsContactUceCapabilityWrapper convertToRcsContactUceCapability(
            Presence presence) {
        if (presence == null) {
            Log.w(LOG_TAG, "convertToRcsContactUceCapability: The presence is null");
            return null;
        }
        if (TextUtils.isEmpty(presence.getEntity())) {
            Log.w(LOG_TAG, "convertToRcsContactUceCapability: The entity is empty");
            return null;
        }

        RcsContactUceCapabilityWrapper uceCapabilityWrapper = new RcsContactUceCapabilityWrapper(
                Uri.parse(presence.getEntity()), RcsContactUceCapability.SOURCE_TYPE_NETWORK,
                RcsContactUceCapability.REQUEST_RESULT_FOUND);

        // Add all the capability tuples of this contact
        presence.getTupleList().forEach(tuple -> {
            // The tuple that fails parsing is invalid data, so discard it.
            if (!tuple.getMalformed()) {
                RcsContactPresenceTuple capabilityTuple = getRcsContactPresenceTuple(tuple);
                if (capabilityTuple != null) {
                    uceCapabilityWrapper.addCapabilityTuple(capabilityTuple);
                }
            } else {
                uceCapabilityWrapper.setMalformedContents();
            }
        });
        uceCapabilityWrapper.setEntityUri(Uri.parse(presence.getEntity()));
        return uceCapabilityWrapper;
    }

    /*
     * Get the RcsContactPresenceTuple from the giving tuple element.
     */
    private static RcsContactPresenceTuple getRcsContactPresenceTuple(Tuple tuple) {
        if (tuple == null) {
            return null;
        }

        String status = RcsContactPresenceTuple.TUPLE_BASIC_STATUS_CLOSED;
        if (Basic.OPEN.equals(PidfParserUtils.getTupleStatus(tuple))) {
            status = RcsContactPresenceTuple.TUPLE_BASIC_STATUS_OPEN;
        }

        String serviceId = PidfParserUtils.getTupleServiceId(tuple);
        String serviceVersion = PidfParserUtils.getTupleServiceVersion(tuple);
        String serviceDescription = PidfParserUtils.getTupleServiceDescription(tuple);

        RcsContactPresenceTuple.Builder builder = new RcsContactPresenceTuple.Builder(status,
                serviceId, serviceVersion);

        // Set contact uri
        String contact = PidfParserUtils.getTupleContact(tuple);
        if (!TextUtils.isEmpty(contact)) {
            builder.setContactUri(Uri.parse(contact));
        }

        // Use local time instead to prevent we receive the incorrect timestamp from the network.
        builder.setTime(getTimestampProxy().getTimestamp());

        // Set service description
        if (!TextUtils.isEmpty(serviceDescription)) {
            builder.setServiceDescription(serviceDescription);
        }

        // Set service capabilities
        ServiceCaps serviceCaps = tuple.getServiceCaps();
        if (serviceCaps != null) {
            List<ElementBase> serviceCapsList = serviceCaps.getElements();
            if (serviceCapsList != null && !serviceCapsList.isEmpty()) {
                boolean isAudioSupported = false;
                boolean isVideoSupported = false;
                List<String> supportedTypes = null;
                List<String> notSupportedTypes = null;

                for (ElementBase element : serviceCapsList) {
                    if (element instanceof Audio) {
                        isAudioSupported = ((Audio) element).isAudioSupported();
                    } else if (element instanceof Video) {
                        isVideoSupported = ((Video) element).isVideoSupported();
                    } else if (element instanceof Duplex) {
                        supportedTypes = ((Duplex) element).getSupportedTypes();
                        notSupportedTypes = ((Duplex) element).getNotSupportedTypes();
                    }
                }

                ServiceCapabilities.Builder capabilitiesBuilder
                        = new ServiceCapabilities.Builder(isAudioSupported, isVideoSupported);

                if (supportedTypes != null && !supportedTypes.isEmpty()) {
                    for (String supportedType : supportedTypes) {
                        capabilitiesBuilder.addSupportedDuplexMode(supportedType);
                    }
                }

                if (notSupportedTypes != null && !notSupportedTypes.isEmpty()) {
                    for (String notSupportedType : notSupportedTypes) {
                        capabilitiesBuilder.addUnsupportedDuplexMode(notSupportedType);
                    }
                }
                builder.setServiceCapabilities(capabilitiesBuilder.build());
            }
        }
        return builder.build();
    }
}
