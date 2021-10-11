/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Decoder;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.asn1.InvalidAsn1DataException;
import com.android.internal.telephony.uicc.asn1.TagNotFoundException;
import com.android.telephony.Rlog;

import java.util.Arrays;

/**
 * This represents the version of GSMA SGP.22 spec in the form of 3 numbers: major, minor, and
 * revision.
 */
public final class EuiccSpecVersion implements Comparable<EuiccSpecVersion> {
    private static final String LOG_TAG = "EuiccSpecVer";

    // ASN.1 Tags
    private static final int TAG_ISD_R_APP_TEMPLATE = 0xE0;
    private static final int TAG_VERSION = 0x82;

    private final int[] mVersionValues = new int[3];

    /**
     * Parses the response of opening a logical channel to get spec version of the eUICC card.
     *
     * @return Parsed spec version. If any error is encountered, null will be returned.
     */
    public static EuiccSpecVersion fromOpenChannelResponse(byte[] response) {
        Asn1Node node;
        try {
            Asn1Decoder decoder = new Asn1Decoder(response);
            if (!decoder.hasNextNode()) {
                return null;
            }
            node = decoder.nextNode();
        } catch (InvalidAsn1DataException e) {
            Rlog.e(LOG_TAG, "Cannot parse the select response of ISD-R.", e);
            return null;
        }
        try {
            byte[] versionType;
            if (node.getTag() == TAG_ISD_R_APP_TEMPLATE) {
                versionType = node.getChild(TAG_VERSION).asBytes();
            } else {
                versionType =
                        node.getChild(TAG_ISD_R_APP_TEMPLATE, TAG_VERSION).asBytes();
            }
            if (versionType.length == 3) {
                return new EuiccSpecVersion(versionType);
            } else {
                Rlog.e(LOG_TAG, "Cannot parse select response of ISD-R: " + node.toHex());
            }
        } catch (InvalidAsn1DataException | TagNotFoundException e) {
            Rlog.e(LOG_TAG, "Cannot parse select response of ISD-R: " + node.toHex());
        }
        return null;
    }

    public EuiccSpecVersion(int major, int minor, int revision) {
        mVersionValues[0] = major;
        mVersionValues[1] = minor;
        mVersionValues[2] = revision;
    }

    /**
     * @param version The version bytes from ASN1 data. The length must be 3.
     */
    public EuiccSpecVersion(byte[] version) {
        mVersionValues[0] = version[0] & 0xFF;
        mVersionValues[1] = version[1] & 0xFF;
        mVersionValues[2] = version[2] & 0xFF;
    }

    public int getMajor() {
        return mVersionValues[0];
    }

    public int getMinor() {
        return mVersionValues[1];
    }

    public int getRevision() {
        return mVersionValues[2];
    }

    @Override
    public int compareTo(EuiccSpecVersion that) {
        if (getMajor() > that.getMajor()) {
            return 1;
        } else if (getMajor() < that.getMajor()) {
            return -1;
        }
        if (getMinor() > that.getMinor()) {
            return 1;
        } else if (getMinor() < that.getMinor()) {
            return -1;
        }
        if (getRevision() > that.getRevision()) {
            return 1;
        } else if (getRevision() < that.getRevision()) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(mVersionValues, ((EuiccSpecVersion) obj).mVersionValues);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mVersionValues);
    }

    @Override
    public String toString() {
        return mVersionValues[0] + "." + mVersionValues[1] + "." + mVersionValues[2];
    }
}
