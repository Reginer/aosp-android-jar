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

package com.android.ims.rcs.uce.presence.pidfparser.pidf;

import android.util.Log;

import com.android.ims.rcs.uce.presence.pidfparser.ElementBase;
import com.android.ims.rcs.uce.util.UceUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * The "status" element of the pidf.
 */
public class Status extends ElementBase {
    private static final String LOG_TAG = UceUtils.getLogPrefix() + "Status";

    /** The name of this element */
    public static final String ELEMENT_NAME = "status";

    // The "status" element contain one optional "basic" element.
    private Basic mBasic;

    public Status() {
    }

    @Override
    protected String initNamespace() {
        return PidfConstant.NAMESPACE;
    }

    @Override
    protected String initElementName() {
        return ELEMENT_NAME;
    }

    public void setBasic(Basic basic) {
        mBasic = basic;
    }

    public Basic getBasic() {
        return mBasic;
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        if (mBasic == null) {
            return;
        }
        final String namespace = getNamespace();
        final String element = getElementName();
        serializer.startTag(namespace, element);
        mBasic.serialize(serializer);
        serializer.endTag(namespace, element);
    }

    @Override
    public void parse(XmlPullParser parser) throws IOException, XmlPullParserException {
        String namespace = parser.getNamespace();
        String name = parser.getName();

        if (!verifyParsingElement(namespace, name)) {
            throw new XmlPullParserException("Incorrect element: " + namespace + ", " + name);
        }

        // Move to the next tag to get the Basic element.
        int eventType = parser.nextTag();

        // Get the value if the event type is text.
        if (eventType == XmlPullParser.START_TAG) {
            Basic basic = new Basic();
            basic.parse(parser);
            mBasic = basic;
        } else {
            Log.d(LOG_TAG, "The eventType is not START_TAG=" + eventType);
        }

        // Move to the end tag.
        moveToElementEndTag(parser, eventType);
    }
}
