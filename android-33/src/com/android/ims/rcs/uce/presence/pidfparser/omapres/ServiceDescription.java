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

package com.android.ims.rcs.uce.presence.pidfparser.omapres;

import com.android.ims.rcs.uce.presence.pidfparser.ElementBase;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * The "service-description" element of the pidf.
 */
public class ServiceDescription extends ElementBase {
    /** The name of this element */
    public static final String ELEMENT_NAME = "service-description";

    private ServiceId mServiceId;
    private Version mVersion;
    private Description mDescription;

    public ServiceDescription() {
    }

    @Override
    protected String initNamespace() {
        return OmaPresConstant.NAMESPACE;
    }

    @Override
    protected String initElementName() {
        return ELEMENT_NAME;
    }

    public void setServiceId(ServiceId serviceId) {
        mServiceId = serviceId;
    }

    public ServiceId getServiceId() {
        return mServiceId;
    }

    public void setVersion(Version version) {
        mVersion = version;
    }

    public Version getVersion() {
        return mVersion;
    }

    public void setDescription(Description description) {
        mDescription = description;
    }

    public Description getDescription() {
        return mDescription;
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        if(mServiceId == null && mVersion == null && mDescription == null) {
            return;
        }
        final String namespace = getNamespace();
        final String element = getElementName();
        serializer.startTag(namespace, element);
        if (mServiceId != null) {
            mServiceId.serialize(serializer);
        }
        if (mVersion != null) {
            mVersion.serialize(serializer);
        }
        if (mDescription != null) {
            mDescription.serialize(serializer);
        }
        serializer.endTag(namespace, element);
    }

    @Override
    public void parse(XmlPullParser parser) throws IOException, XmlPullParserException {
        String namespace = parser.getNamespace();
        String name = parser.getName();

        if (!verifyParsingElement(namespace, name)) {
            throw new XmlPullParserException("Incorrect element: " + namespace + ", " + name);
        }

        // Move to the next event.
        int eventType = parser.next();

        while(!(eventType == XmlPullParser.END_TAG
                && getNamespace().equals(parser.getNamespace())
                && getElementName().equals(parser.getName()))) {

            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                if (ServiceId.ELEMENT_NAME.equals(tagName)) {
                    ServiceId serviceId = new ServiceId();
                    serviceId.parse(parser);
                    mServiceId = serviceId;
                } else if (Version.ELEMENT_NAME.equals(tagName)) {
                    Version version = new Version();
                    version.parse(parser);
                    mVersion = version;
                } else if (Description.ELEMENT_NAME.equals(tagName)) {
                    Description description = new Description();
                    description.parse(parser);
                    mDescription = description;
                }
            }

            eventType = parser.next();

            // Leave directly if the event type is the end of the document.
            if (eventType == XmlPullParser.END_DOCUMENT) {
                return;
            }
        }
    }
}
