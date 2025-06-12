/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Environment;
import android.util.Xml;

import com.android.internal.telephony.util.XmlUtils;
import com.android.telephony.Rlog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * {@hide}
 */
class VoiceMailConstants {
    private HashMap<String, String[]> CarrierVmMap;


    static final String LOG_TAG = "VoiceMailConstants";
    static final String PARTNER_VOICEMAIL_PATH ="etc/voicemail-conf.xml";

    static final int NAME = 0;
    static final int NUMBER = 1;
    static final int TAG = 2;
    static final int SIZE = 3;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    VoiceMailConstants () {
        CarrierVmMap = new HashMap<String, String[]>();
        loadVoiceMail();
    }

    boolean containsCarrier(String carrier) {
        return CarrierVmMap.containsKey(carrier);
    }

    String getCarrierName(String carrier) {
        String[] data = CarrierVmMap.get(carrier);
        return data[NAME];
    }

    String getVoiceMailNumber(String carrier) {
        String[] data = CarrierVmMap.get(carrier);
        return data[NUMBER];
    }

    String getVoiceMailTag(String carrier) {
        String[] data = CarrierVmMap.get(carrier);
        return data[TAG];
    }

    private void loadVoiceMail() {
        FileReader vmReader;

        final File vmFile = new File(Environment.getRootDirectory(),
                PARTNER_VOICEMAIL_PATH);

        try {
            vmReader = new FileReader(vmFile);
        } catch (FileNotFoundException e) {
            Rlog.w(LOG_TAG, "Can't open " +
                    Environment.getRootDirectory() + "/" + PARTNER_VOICEMAIL_PATH);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(vmReader);

            XmlUtils.beginDocument(parser, "voicemail");

            while (true) {
                XmlUtils.nextElement(parser);

                String name = parser.getName();
                if (!"voicemail".equals(name)) {
                    break;
                }

                String[] data = new String[SIZE];
                String numeric = parser.getAttributeValue(null, "numeric");
                data[NAME]     = parser.getAttributeValue(null, "carrier");
                data[NUMBER]   = parser.getAttributeValue(null, "vmnumber");
                data[TAG]      = parser.getAttributeValue(null, "vmtag");

                CarrierVmMap.put(numeric, data);
            }
        } catch (XmlPullParserException e) {
            Rlog.w(LOG_TAG, "Exception in Voicemail parser " + e);
        } catch (IOException e) {
            Rlog.w(LOG_TAG, "Exception in Voicemail parser " + e);
        } finally {
            try {
                if (vmReader != null) {
                    vmReader.close();
                }
            } catch (IOException e) {}
        }
    }
}
