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

import com.android.ims.rcs.uce.presence.pidfparser.ElementBase;
import com.android.ims.rcs.uce.presence.pidfparser.capabilities.ServiceCaps;
import com.android.ims.rcs.uce.presence.pidfparser.omapres.ServiceDescription;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The "tuple" element of the pidf.
 */
public class Tuple extends ElementBase {
    /**
     * The tuple element consists the following elements:
     * 1: one "status" element
     * 2: any number of optional extension elements
     * 3: an optional "contact" element
     * 4: any number of optional "note" elements
     * 5: an optional "timestamp" element
     */

    /** The name of this element */
    public static final String ELEMENT_NAME = "tuple";

    private static final String ATTRIBUTE_NAME_TUPLE_ID = "id";

    private static long sTupleId = 0;

    private static final Object LOCK = new Object();

    private String mId;
    private Status mStatus;
    private ServiceDescription mServiceDescription;
    private ServiceCaps mServiceCaps;
    private Contact mContact;
    private List<Note> mNoteList = new ArrayList<>();
    private Timestamp mTimestamp;

    private boolean mMalformed;

    public Tuple() {
        mId = getTupleId();
        mMalformed = false;
    }

    @Override
    protected String initNamespace() {
        return PidfConstant.NAMESPACE;
    }

    @Override
    protected String initElementName() {
        return ELEMENT_NAME;
    }

    public void setStatus(Status status) {
        mStatus = status;
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setServiceDescription(ServiceDescription servDescription) {
        mServiceDescription = servDescription;
    }

    public ServiceDescription getServiceDescription() {
        return mServiceDescription;
    }

    public void setServiceCaps(ServiceCaps serviceCaps) {
        mServiceCaps = serviceCaps;
    }

    public ServiceCaps getServiceCaps() {
        return mServiceCaps;
    }

    public void setContact(Contact contact) {
        mContact = contact;
    }

    public Contact getContact() {
        return mContact;
    }

    public void addNote(Note note) {
        mNoteList.add(note);
    }

    public List<Note> getNoteList() {
        return Collections.unmodifiableList(mNoteList);
    }

    public void setTimestamp(Timestamp timestamp) {
        mTimestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return mTimestamp;
    }

    public void setMalformed(boolean malformed) {
        mMalformed = malformed;
    }

    public boolean getMalformed() {
        return mMalformed;
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        String namespace = getNamespace();
        String elementName = getElementName();

        serializer.startTag(namespace, elementName);
        // id attribute
        serializer.attribute(XmlPullParser.NO_NAMESPACE, ATTRIBUTE_NAME_TUPLE_ID, mId);

        // status element
        mStatus.serialize(serializer);

        // Service description
        if (mServiceDescription != null) {
            mServiceDescription.serialize(serializer);
        }

        // Service capabilities
        if (mServiceCaps != null) {
            mServiceCaps.serialize(serializer);
        }

        // contact element
        if (mContact != null) {
            mContact.serialize(serializer);
        }

        // note element
        for (Note note: mNoteList) {
            note.serialize(serializer);
        }

        // Timestamp
        if (mTimestamp != null) {
            mTimestamp.serialize(serializer);
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

        // id attribute
        mId = parser.getAttributeValue(XmlPullParser.NO_NAMESPACE, ATTRIBUTE_NAME_TUPLE_ID);

        // Move to the next event.
        int eventType = parser.next();

        while(!(eventType == XmlPullParser.END_TAG
                && getNamespace().equals(parser.getNamespace())
                && getElementName().equals(parser.getName()))) {

            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                if (Status.ELEMENT_NAME.equals(tagName)) {
                    Status status = new Status();
                    status.parse(parser);
                    mStatus = status;
                } else if (ServiceDescription.ELEMENT_NAME.equals(tagName)) {
                    ServiceDescription serviceDescription = new ServiceDescription();
                    serviceDescription.parse(parser);
                    mServiceDescription = serviceDescription;
                } else if (ServiceCaps.ELEMENT_NAME.equals(tagName)) {
                    ServiceCaps serviceCaps = new ServiceCaps();
                    serviceCaps.parse(parser);
                    mServiceCaps = serviceCaps;
                } else if (Contact.ELEMENT_NAME.equals(tagName)) {
                    Contact contact = new Contact();
                    contact.parse(parser);
                    mContact = contact;
                } else if (Note.ELEMENT_NAME.equals(tagName)) {
                    Note note = new Note();
                    note.parse(parser);
                    mNoteList.add(note);
                } else if (Timestamp.ELEMENT_NAME.equals(tagName)) {
                    Timestamp timestamp = new Timestamp();
                    timestamp.parse(parser);
                    mTimestamp = timestamp;
                }
            }

            eventType = parser.next();

            // Leave directly if the event type is the end of the document.
            if (eventType == XmlPullParser.END_DOCUMENT) {
                return;
            }
        }
    }

    private String getTupleId() {
        synchronized (LOCK) {
            return "tid" + (sTupleId++);
        }
    }
}
