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

package com.android.ims.rcs.uce.presence.pidfparser.capabilities;

import android.annotation.StringDef;
import android.text.TextUtils;

import com.android.ims.rcs.uce.presence.pidfparser.ElementBase;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * The "duplex" element indicates how the communication service send and receive media. It can
 * contain two elements: "supported" and "notsupported." The supported and
 * nonsupported elements can contains four elements: "full", "half", "receive-only" and
 * "send-only".
 */
public class Duplex extends ElementBase {
    /** The name of the duplex element */
    public static final String ELEMENT_NAME = "duplex";

    /** The name of the supported element */
    public static final String ELEMENT_SUPPORTED = "supported";

    /** The name of the notsupported element */
    public static final String ELEMENT_NOT_SUPPORTED = "notsupported";

    /** The device can simultaneously send and receive media */
    public static final String DUPLEX_FULL = "full";

    /** The service can alternate between sending and receiving media.*/
    public static final String DUPLEX_HALF = "half";

    /** The service can only receive media */
    public static final String DUPLEX_RECEIVE_ONLY = "receive-only";

    /** The service can only send media */
    public static final String DUPLEX_SEND_ONLY = "send-only";

    @StringDef(value = {
            DUPLEX_FULL,
            DUPLEX_HALF,
            DUPLEX_RECEIVE_ONLY,
            DUPLEX_SEND_ONLY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DuplexType {}

    private final List<String> mSupportedTypeList = new ArrayList<>();
    private final List<String> mNotSupportedTypeList = new ArrayList<>();

    public Duplex() {
    }

    @Override
    protected String initNamespace() {
        return CapsConstant.NAMESPACE;
    }

    @Override
    protected String initElementName() {
        return ELEMENT_NAME;
    }

    public void addSupportedType(@DuplexType String type) {
        mSupportedTypeList.add(type);
    }

    public List<String> getSupportedTypes() {
        return Collections.unmodifiableList(mSupportedTypeList);
    }

    public void addNotSupportedType(@DuplexType String type) {
        mNotSupportedTypeList.add(type);
    }

    public List<String> getNotSupportedTypes() {
        return Collections.unmodifiableList(mNotSupportedTypeList);
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        if (mSupportedTypeList.isEmpty() && mNotSupportedTypeList.isEmpty()) {
            return;
        }
        String namespace = getNamespace();
        String elementName = getElementName();
        serializer.startTag(namespace, elementName);
        for (String supportedType : mSupportedTypeList) {
            serializer.startTag(namespace, ELEMENT_SUPPORTED);
            serializer.startTag(namespace, supportedType);
            serializer.endTag(namespace, supportedType);
            serializer.endTag(namespace, ELEMENT_SUPPORTED);
        }
        for (String notSupportedType : mNotSupportedTypeList) {
            serializer.startTag(namespace, ELEMENT_NOT_SUPPORTED);
            serializer.startTag(namespace, notSupportedType);
            serializer.endTag(namespace, notSupportedType);
            serializer.endTag(namespace, ELEMENT_NOT_SUPPORTED);
        }
        serializer.endTag(namespace, elementName);
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

                if (ELEMENT_SUPPORTED.equals(tagName)) {
                    String duplexType = getDuplexType(parser);
                    if (!TextUtils.isEmpty(duplexType)) {
                        addSupportedType(duplexType);
                    }
                } else if (ELEMENT_NOT_SUPPORTED.equals(tagName)) {
                    String duplexType = getDuplexType(parser);
                    if (!TextUtils.isEmpty(duplexType)) {
                        addNotSupportedType(duplexType);
                    }
                }
            }

            eventType = parser.next();

            // Leave directly if the event type is the end of the document.
            if (eventType == XmlPullParser.END_DOCUMENT) {
                return;
            }
        }
    }

    private String getDuplexType(XmlPullParser parser) throws IOException, XmlPullParserException {
        // Move to the next event
        int eventType = parser.next();

        String name = parser.getName();
        if (eventType == XmlPullParser.START_TAG) {
            if (DUPLEX_FULL.equals(name) ||
                    DUPLEX_HALF.equals(name) ||
                    DUPLEX_RECEIVE_ONLY.equals(name) ||
                    DUPLEX_SEND_ONLY.equals(name)) {
                return name;
            }
        }
        return null;
    }
}
