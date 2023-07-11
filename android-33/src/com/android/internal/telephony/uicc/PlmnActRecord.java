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

import android.annotation.IntDef;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.telephony.Rlog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

/**
 * {@hide}
 */
public class PlmnActRecord implements Parcelable {
    private static final String LOG_TAG = "PlmnActRecord";

    private static final boolean VDBG = false;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {ACCESS_TECH_UTRAN, ACCESS_TECH_EUTRAN, ACCESS_TECH_GSM,
            ACCESS_TECH_GSM_COMPACT, ACCESS_TECH_CDMA2000_HRPD, ACCESS_TECH_CDMA2000_1XRTT,
            ACCESS_TECH_RESERVED})
    public @interface AccessTech {}
    // Values specified in 3GPP 31.102 sec. 4.2.5
    public static final int ACCESS_TECH_UTRAN = 0x8000;
    public static final int ACCESS_TECH_EUTRAN = 0x4000;
    public static final int ACCESS_TECH_GSM = 0x0080;
    public static final int ACCESS_TECH_GSM_COMPACT = 0x0040;
    public static final int ACCESS_TECH_CDMA2000_HRPD = 0x0020;
    public static final int ACCESS_TECH_CDMA2000_1XRTT = 0x0010;
    public static final int ACCESS_TECH_RESERVED = 0x3F0F;

    public static final int ENCODED_LENGTH = 5;

    public final String plmn;
    public final int accessTechs;

    /**
     * Instantiate a PLMN w/ ACT Record
     */
    public PlmnActRecord(String plmn, int accessTechs) {
        this.plmn = plmn;
        this.accessTechs = accessTechs;
    }

    /* From 3gpp 31.102 section 4.2.5
     * Bytes 0-2 bcd-encoded PLMN-ID
     * Bytes 3-4 bitfield of access technologies
     */
    public PlmnActRecord(byte[] bytes, int offset) {
        if (VDBG) Rlog.v(LOG_TAG, "Creating PlmnActRecord " + offset);
        this.plmn = IccUtils.bcdPlmnToString(bytes, offset);
        this.accessTechs = (Byte.toUnsignedInt(bytes[offset + 3]) << 8)
                        | Byte.toUnsignedInt(bytes[offset + 4]);
    }

    /**
     * Get a record encoded as per 31.102 section 4.2.5
     */
    public byte[] getBytes() {
        byte[] ret = new byte[ENCODED_LENGTH];
        IccUtils.stringToBcdPlmn(this.plmn, ret, 0);
        ret[3] = (byte) (this.accessTechs >> 8);
        ret[4] = (byte) this.accessTechs;
        return ret;
    }

    private String accessTechString() {
        if (accessTechs == 0) {
            return "NONE";
        }

        StringBuilder sb = new StringBuilder();
        if ((accessTechs & ACCESS_TECH_UTRAN) != 0) {
            sb.append("UTRAN|");
        }
        if ((accessTechs & ACCESS_TECH_EUTRAN) != 0) {
            sb.append("EUTRAN|");
        }
        if ((accessTechs & ACCESS_TECH_GSM) != 0) {
            sb.append("GSM|");
        }
        if ((accessTechs & ACCESS_TECH_GSM_COMPACT) != 0) {
            sb.append("GSM_COMPACT|");
        }
        if ((accessTechs & ACCESS_TECH_CDMA2000_HRPD) != 0) {
            sb.append("CDMA2000_HRPD|");
        }
        if ((accessTechs & ACCESS_TECH_CDMA2000_1XRTT) != 0) {
            sb.append("CDMA2000_1XRTT|");
        }
        if ((accessTechs & ACCESS_TECH_RESERVED) != 0) {
            sb.append(String.format("UNKNOWN:%x|", accessTechs & ACCESS_TECH_RESERVED));
        }
        // Trim the tailing pipe character
        return sb.substring(0, sb.length() - 1);
    }

    @Override
    public String toString() {
        return String.format("{PLMN=%s,AccessTechs=%s}", plmn, accessTechString());
    }

    /**
     * Convenience method for extracting all records from encoded bytes
     */
    public static PlmnActRecord[] getRecords(byte[] recordBytes) {
        if (recordBytes == null || recordBytes.length == 0
                || recordBytes.length % ENCODED_LENGTH != 0) {
            Rlog.e(LOG_TAG, "Malformed PlmnActRecord, bytes: "
                    + ((recordBytes != null) ? Arrays.toString(recordBytes) : null));
            return null;
        }
        int numRecords = recordBytes.length / ENCODED_LENGTH;
        if (VDBG) Rlog.v(LOG_TAG, "Extracting Logs, count=" + numRecords);

        PlmnActRecord[] records = new PlmnActRecord[numRecords];

        for(int i = 0; i < numRecords; i++) {
            records[i] = new PlmnActRecord(recordBytes, i * ENCODED_LENGTH);
        }
        return records;
    }

    // Parcelable Implementation
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(plmn);
        dest.writeInt(accessTechs);
    }

    public static final Parcelable.Creator<PlmnActRecord> CREATOR =
            new Parcelable.Creator<PlmnActRecord>() {
        @Override
        public PlmnActRecord createFromParcel(Parcel source) {
            return new PlmnActRecord(source.readString(), source.readInt());
        }

        @Override
        public PlmnActRecord[] newArray(int size) {
            return new PlmnActRecord[size];
        }
    };

    @Override
    public int hashCode() {
        return Objects.hash(plmn, accessTechs);
    }

    @Override
    public boolean equals(Object rhs) {
        if (!(rhs instanceof PlmnActRecord)) return false;

        PlmnActRecord r = (PlmnActRecord) rhs;
        return plmn.equals(r.plmn) && accessTechs == r.accessTechs;
    }
}
