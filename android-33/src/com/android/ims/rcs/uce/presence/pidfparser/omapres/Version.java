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
 * The "version" element of the pidf.
 */
public class Version extends ElementBase {
    /** The name of this element */
    public static final String ELEMENT_NAME = "version";

    private int mMajorVersion;
    private int mMinorVersion;

    public Version() {
    }

    public Version(int majorVersion, int minorVersion) {
        mMajorVersion = majorVersion;
        mMinorVersion = minorVersion;
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
        StringBuilder builder = new StringBuilder();
        builder.append(mMajorVersion).append(".").append(mMinorVersion);
        return builder.toString();
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        String namespace = getNamespace();
        String elementName = getElementName();
        serializer.startTag(namespace, elementName);
        serializer.text(getValue());
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
            String version = parser.getText();
            handleParsedVersion(version);
        }

        // Move to the end tag.
        moveToElementEndTag(parser, eventType);
    }

    private void handleParsedVersion(String version) {
        if (TextUtils.isEmpty(version)) {
            return;
        }

        String[] versionAry = version.split("\\.");
        if (versionAry != null && versionAry.length == 2) {
            mMajorVersion = Integer.parseInt(versionAry[0]);
            mMinorVersion = Integer.parseInt(versionAry[1]);
        }
    }
}
