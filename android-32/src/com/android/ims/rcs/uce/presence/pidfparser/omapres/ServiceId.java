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

import android.text.TextUtils;

import com.android.ims.rcs.uce.presence.pidfparser.ElementBase;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * The "service-id" element of the pidf.
 */
public class ServiceId extends ElementBase {
    /** The name of this element */
    public static final String ELEMENT_NAME = "service-id";

    private String mServiceId;

    public ServiceId() {
    }

    public ServiceId(String serviceId) {
        mServiceId = serviceId;
    }

    @Override
    protected String initNamespace() {
        return OmaPresConstant.NAMESPACE;
    }

    @Override
    protected String initElementName() {
        return ELEMENT_NAME;
    }

    public String getValue() {
        return mServiceId;
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        if (mServiceId == null) {
            return;
        }
        String namespace = getNamespace();
        String elementName = getElementName();
        serializer.startTag(namespace, elementName);
        serializer.text(mServiceId);
        serializer.endTag(namespace, elementName);
    }

    @Override
    public void parse(XmlPullParser parser) throws IOException, XmlPullParserException {
        String namespace = parser.getNamespace();
        String name = parser.getName();

        if (!verifyParsingElement(namespace, name)) {
            throw new XmlPullParserException("Incorrect element: " + namespace + ", " + name);
        }

        // Move to the next event to get the value.
        int eventType = parser.next();

        // Get the value if the event type is text.
        if (eventType == XmlPullParser.TEXT) {
            String serviceId = parser.getText();
            if (!TextUtils.isEmpty(serviceId)) {
                mServiceId = serviceId;
            }
        }

        // Move to the end tag.
        moveToElementEndTag(parser, eventType);
    }
}
