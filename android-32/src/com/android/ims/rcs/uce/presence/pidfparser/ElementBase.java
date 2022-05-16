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

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * The base class of the pidf element.
 */
public abstract class ElementBase {
    private String mNamespace;
    private String mElementName;

    public ElementBase() {
        mNamespace = initNamespace();
        mElementName = initElementName();
    }

    protected abstract String initNamespace();
    protected abstract String initElementName();

    /**
     * @return The namespace of this element
     */
    public String getNamespace() {
        return mNamespace;
    }

    /**
     * @return The name of this element.
     */
    public String getElementName() {
        return mElementName;
    }

    public abstract void serialize(XmlSerializer serializer) throws IOException;

    public abstract void parse(XmlPullParser parser) throws IOException, XmlPullParserException;

    protected boolean verifyParsingElement(String namespace, String elementName) {
        if (!getNamespace().equals(namespace) || !getElementName().equals(elementName)) {
            return false;
        } else {
            return true;
        }
    }

    // Move to the end tag of this element
    protected void moveToElementEndTag(XmlPullParser parser, int type)
            throws IOException, XmlPullParserException {
        int eventType = type;

        // Move to the end tag of this element.
        while(!(eventType == XmlPullParser.END_TAG
                && getNamespace().equals(parser.getNamespace())
                && getElementName().equals(parser.getName()))) {
            eventType = parser.next();

            // Leave directly if the event type is the end of the document.
            if (eventType == XmlPullParser.END_DOCUMENT) {
                return;
            }
        }
    }
}
