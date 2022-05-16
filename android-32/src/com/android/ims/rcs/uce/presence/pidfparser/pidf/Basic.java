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

import android.annotation.StringDef;
import android.util.Log;

import com.android.ims.rcs.uce.presence.pidfparser.ElementBase;
import com.android.ims.rcs.uce.util.UceUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The "basic" element of the pidf.
 */
public class Basic extends ElementBase {
    private static final String LOG_TAG = UceUtils.getLogPrefix() + "Basic";

    /** The name of this element */
    public static final String ELEMENT_NAME = "basic";

    /** The value "open" of the Basic element */
    public static final String OPEN = "open";

    /** The value "closed" of the Basic element */
    public static final String CLOSED = "closed";

    @StringDef(value = {
            OPEN,
            CLOSED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BasicValue {}

    private @BasicValue String mBasic;

    public Basic() {
    }

    public Basic(@BasicValue String value) {
        mBasic = value;
    }

    @Override
    protected String initNamespace() {
        return PidfConstant.NAMESPACE;
    }

    @Override
    protected String initElementName() {
        return ELEMENT_NAME;
    }

    public String getValue() {
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
        serializer.text(mBasic);
        serializer.endTag(namespace, element);
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
            String basicValue = parser.getText();
            if (OPEN.equals(basicValue)) {
                mBasic = OPEN;
            } else if (CLOSED.equals(basicValue)) {
                mBasic = CLOSED;
            } else {
                mBasic = null;
            }
        } else {
            Log.d(LOG_TAG, "The eventType is not TEXT=" + eventType);
        }

        // Move to the end tag.
        moveToElementEndTag(parser, eventType);
    }
}
