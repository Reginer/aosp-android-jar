/*
 * Copyright 2018 The Android Open Source Project
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

import android.annotation.Nullable;
import android.util.ArrayMap;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class parses an Answer To Reset (ATR) message.
 * The ATR message structure is defined in standard ISO/IEC 7816-3. The eUICC related ATR message
 * is defined in standard ETSI TS 102 221 V14.0.0.
 */
public class AnswerToReset {
    private static final String TAG = "AnswerToReset";
    private static final boolean VDBG = false; // STOPSHIP if true
    private static final int TAG_CARD_CAPABILITIES = 0x07;
    private static final int EXTENDED_APDU_INDEX = 2;
    private static final int B8_MASK = 0x80;
    private static final int B7_MASK = 0x40;
    private static final int B2_MASK = 0x02;

    public static final byte DIRECT_CONVENTION = (byte) 0x3B;
    public static final byte INVERSE_CONVENTION = (byte) 0x3F;
    public static final int INTERFACE_BYTES_MASK = 0xF0;
    public static final int T_MASK = 0x0F;
    public static final int T_VALUE_FOR_GLOBAL_INTERFACE = 15;
    public static final int TA_MASK = 0x10;
    public static final int TB_MASK = 0x20;
    public static final int TC_MASK = 0x40;
    public static final int TD_MASK = 0x80;

    private boolean mIsDirectConvention;
    private boolean mOnlyTEqualsZero = true;
    private boolean mIsEuiccSupported;
    private byte mFormatByte;
    private ArrayList<InterfaceByte> mInterfaceBytes = new ArrayList<>();
    private HistoricalBytes mHistoricalBytes;
    private Byte mCheckByte;

    /** Class for the historical bytes. */
    public static class HistoricalBytes {
        private static final int TAG_MASK = 0xF0;
        private static final int LENGTH_MASK = 0x0F;

        private final byte[] mRawData;
        private final ArrayMap<Integer, byte[]> mNodes;
        private final byte mCategory;

        /** Get the category of the historical bytes. */
        public byte getCategory() {
            return mCategory;
        }

        /** Get the raw data of historical bytes. */
        public byte[] getRawData() {
            return mRawData;
        }

        /** Get the value of the tag in historical bytes. */
        @Nullable
        public byte[] getValue(int tag) {
            return mNodes.get(tag);
        }

        @Nullable
        private static HistoricalBytes parseHistoricalBytes(
                byte[] originalData, int startIndex, int length) {
            if (length <= 0 || startIndex + length > originalData.length) {
                return null;
            }
            ArrayMap<Integer, byte[]> nodes = new ArrayMap<>();

            // Start parsing from second byte since the first one is category.
            int index = startIndex + 1;
            while (index < startIndex + length && index > 0) {
                index = parseLtvNode(index, nodes, originalData, startIndex + length - 1);
            }
            if (index < 0) {
                return null;
            }
            byte[] rawData = new byte[length];
            System.arraycopy(originalData, startIndex, rawData, 0, length);
            return new HistoricalBytes(rawData, nodes, rawData[0]);
        }

        private HistoricalBytes(byte[] rawData, ArrayMap<Integer, byte[]> nodes, byte category) {
            mRawData = rawData;
            mNodes = nodes;
            mCategory = category;
        }

        private static int parseLtvNode(
                int index, ArrayMap<Integer, byte[]> nodes, byte[] data, int lastByteIndex) {
            if (index > lastByteIndex) {
                return -1;
            }
            int tag = (data[index] & TAG_MASK) >> 4;
            int length = data[index++] & LENGTH_MASK;
            if (index + length > lastByteIndex + 1 || length == 0) {
                return -1;
            }
            byte[] value = new byte[length];
            System.arraycopy(data, index, value, 0, length);
            nodes.put(tag, value);
            return index + length;
        }
    }


    /**
     * Returns an AnswerToReset by parsing the input atr string, return null if the parsing fails.
     */
    public static AnswerToReset parseAtr(String atr) {
        AnswerToReset answerToReset = new AnswerToReset();
        if (answerToReset.parseAtrString(atr)) {
            return answerToReset;
        }
        return null;
    }

    private AnswerToReset() {}

    private static String byteToStringHex(Byte b) {
        return b == null ? null : IccUtils.byteToHex(b);
    }

    private void checkIsEuiccSupported() {
        // eUICC is supported only if the b8 and b2 of the first tB after T=15 are set to 1.
        for (int i = 0; i < mInterfaceBytes.size() - 1; i++) {
            if (mInterfaceBytes.get(i).getTD() != null
                    && (mInterfaceBytes.get(i).getTD() & T_MASK) == T_VALUE_FOR_GLOBAL_INTERFACE
                    && mInterfaceBytes.get(i + 1).getTB() != null
                    && (mInterfaceBytes.get(i + 1).getTB() & B8_MASK) != 0
                    && (mInterfaceBytes.get(i + 1).getTB() & B2_MASK) != 0) {
                mIsEuiccSupported = true;
                return;
            }
        }
    }

    private int parseConventionByte(byte[] atrBytes, int index) {
        if (index >= atrBytes.length) {
            loge("Failed to read the convention byte.");
            return -1;
        }
        byte value = atrBytes[index];
        if (value == DIRECT_CONVENTION) {
            mIsDirectConvention = true;
        } else if (value == INVERSE_CONVENTION) {
            mIsDirectConvention = false;
        } else {
            loge("Unrecognized convention byte " + IccUtils.byteToHex(value));
            return -1;
        }
        return index + 1;
    }

    private int parseFormatByte(byte[] atrBytes, int index) {
        if (index >= atrBytes.length) {
            loge("Failed to read the format byte.");
            return -1;
        }
        mFormatByte = atrBytes[index];
        if (VDBG) log("mHistoricalBytesLength: " + (mFormatByte & T_MASK));
        return index + 1;
    }

    private int parseInterfaceBytes(byte[] atrBytes, int index) {
        // The first lastTD is actually not any TD but instead the format byte.
        byte lastTD = mFormatByte;
        while (true) {
            if (VDBG) log("lastTD: " + IccUtils.byteToHex(lastTD));
            // Parse the interface bytes.
            if ((lastTD & INTERFACE_BYTES_MASK) == 0) {
                break;
            }

            InterfaceByte interfaceByte = new InterfaceByte();
            if (VDBG) log("lastTD & TA_MASK: " + IccUtils.byteToHex((byte) (lastTD & TA_MASK)));
            if ((lastTD & TA_MASK) != 0) {
                if (index >= atrBytes.length) {
                    loge("Failed to read the byte for TA.");
                    return -1;
                }
                interfaceByte.setTA(atrBytes[index]);
                index++;
            }
            if (VDBG) log("lastTD & TB_MASK: " + IccUtils.byteToHex((byte) (lastTD & TB_MASK)));
            if ((lastTD & TB_MASK) != 0) {
                if (index >= atrBytes.length) {
                    loge("Failed to read the byte for TB.");
                    return -1;
                }
                interfaceByte.setTB(atrBytes[index]);
                index++;
            }
            if (VDBG) log("lastTD & TC_MASK: " + IccUtils.byteToHex((byte) (lastTD & TC_MASK)));
            if ((lastTD & TC_MASK) != 0) {
                if (index >= atrBytes.length) {
                    loge("Failed to read the byte for TC.");
                    return -1;
                }
                interfaceByte.setTC(atrBytes[index]);
                index++;
            }
            if (VDBG) log("lastTD & TD_MASK: " + IccUtils.byteToHex((byte) (lastTD & TD_MASK)));
            if ((lastTD & TD_MASK) != 0) {
                if (index >= atrBytes.length) {
                    loge("Failed to read the byte for TD.");
                    return -1;
                }
                interfaceByte.setTD(atrBytes[index]);
                index++;
            }
            mInterfaceBytes.add(interfaceByte);
            Byte newTD = interfaceByte.getTD();
            if (VDBG) log("index=" + index + ", " + toString());
            if (newTD == null) {
                break;
            }
            lastTD = newTD;
            // Parse the T values from all the TD, here we only check whether T is equal to any
            // other values other than 0, since the check byte can be absent only when T is
            // equal to 0.
            if ((lastTD & T_MASK) != 0) {
                mOnlyTEqualsZero = false;
            }
        }
        return index;
    }

    private int parseHistoricalBytes(byte[] atrBytes, int index) {
        int length = mFormatByte & T_MASK;
        if (length + index > atrBytes.length) {
            loge("Failed to read the historical bytes.");
            return -1;
        }
        if (length > 0) {
            mHistoricalBytes = HistoricalBytes.parseHistoricalBytes(atrBytes, index, length);
        }
        return index + length;
    }

    private int parseCheckBytes(byte[] atrBytes, int index) {
        if (index < atrBytes.length) {
            mCheckByte = atrBytes[index];
            index++;
        } else {
            if (!mOnlyTEqualsZero) {
                loge("Check byte must be present because T equals to values other than 0.");
                return -1;
            } else {
                log("Check byte can be absent because T=0.");
            }
        }
        return index;
    }

    private boolean parseAtrString(String atr) {
        if (atr == null) {
            loge("The input ATR string can not be null");
            return false;
        }

        if (atr.length() % 2 != 0) {
            loge("The length of input ATR string " + atr.length() + " is not even.");
            return false;
        }

        if (atr.length() < 4) {
            loge("Valid ATR string must at least contains TS and T0.");
            return false;
        }

        byte[] atrBytes = IccUtils.hexStringToBytes(atr);
        if (atrBytes == null) {
            return false;
        }

        int index = parseConventionByte(atrBytes, 0);
        if (index == -1) {
            return false;
        }

        index = parseFormatByte(atrBytes, index);
        if (index == -1) {
            return false;
        }

        index = parseInterfaceBytes(atrBytes, index);
        if (index == -1) {
            return false;
        }

        index = parseHistoricalBytes(atrBytes, index);
        if (index == -1) {
            return false;
        }

        index = parseCheckBytes(atrBytes, index);
        if (index == -1) {
            return false;
        }

        if (index != atrBytes.length) {
            loge("Unexpected bytes after the check byte.");
            return false;
        }
        log("Successfully parsed the ATR string " + atr + " into " + toString());
        checkIsEuiccSupported();
        return true;
    }

    /**
     * This class holds the interface bytes.
     */
    public static class InterfaceByte {
        private Byte mTA;
        private Byte mTB;
        private Byte mTC;
        private Byte mTD;

        @Nullable
        public Byte getTA() {
            return mTA;
        }

        @Nullable
        public Byte getTB() {
            return mTB;
        }

        @Nullable
        public Byte getTC() {
            return mTC;
        }

        @Nullable
        public Byte getTD() {
            return mTD;
        }

        public void setTA(Byte tA) {
            mTA = tA;
        }

        public void setTB(Byte tB) {
            mTB = tB;
        }

        public void setTC(Byte tC) {
            mTC = tC;
        }

        public void setTD(Byte tD) {
            mTD = tD;
        }

        private InterfaceByte() {
            mTA = null;
            mTB = null;
            mTC = null;
            mTD = null;
        }

        @VisibleForTesting
        public InterfaceByte(Byte tA, Byte tB, Byte tC, Byte tD) {
            this.mTA = tA;
            this.mTB = tB;
            this.mTC = tC;
            this.mTD = tD;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InterfaceByte ib = (InterfaceByte) o;
            return (Objects.equals(mTA, ib.getTA())
                    && Objects.equals(mTB, ib.getTB())
                    && Objects.equals(mTC, ib.getTC())
                    && Objects.equals(mTD, ib.getTD()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(mTA, mTB, mTC, mTD);
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("{");
            sb.append("TA=").append(byteToStringHex(mTA)).append(",");
            sb.append("TB=").append(byteToStringHex(mTB)).append(",");
            sb.append("TC=").append(byteToStringHex(mTC)).append(",");
            sb.append("TD=").append(byteToStringHex(mTD));
            sb.append("}");
            return sb.toString();
        }
    };

    private static void log(String msg) {
        Rlog.d(TAG, msg);
    }

    private static void loge(String msg) {
        Rlog.e(TAG, msg);
    }

    public byte getConventionByte() {
        return mIsDirectConvention ? DIRECT_CONVENTION : INVERSE_CONVENTION;
    }

    public byte getFormatByte() {
        return mFormatByte;
    }

    public List<InterfaceByte> getInterfaceBytes() {
        return mInterfaceBytes;
    }

    @Nullable
    public HistoricalBytes getHistoricalBytes() {
        return mHistoricalBytes;
    }

    @Nullable
    public Byte getCheckByte() {
        return mCheckByte;
    }

    public boolean isEuiccSupported() {
        return mIsEuiccSupported;
    }

    /** Return whether the extended LC & LE is supported. */
    public boolean isExtendedApduSupported() {
        if (mHistoricalBytes == null) {
            return false;
        }
        byte[] cardCapabilities = mHistoricalBytes.getValue(TAG_CARD_CAPABILITIES);
        if (cardCapabilities == null || cardCapabilities.length < 3) {
            return false;
        }
        if (mIsDirectConvention) {
            return (cardCapabilities[EXTENDED_APDU_INDEX] & B7_MASK) > 0;
        } else {
            return (cardCapabilities[EXTENDED_APDU_INDEX] & B2_MASK) > 0;
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("AnswerToReset:{");
        sb.append("mConventionByte=")
                .append(IccUtils.byteToHex(getConventionByte())).append(",");
        sb.append("mFormatByte=").append(byteToStringHex(mFormatByte)).append(",");
        sb.append("mInterfaceBytes={");
        for (InterfaceByte ib : mInterfaceBytes) {
            sb.append(ib.toString());
        }
        sb.append("},");
        sb.append("mHistoricalBytes={");
        if (mHistoricalBytes != null) {
            for (byte b : mHistoricalBytes.getRawData()) {
                sb.append(IccUtils.byteToHex(b)).append(",");
            }
        }
        sb.append("},");
        sb.append("mCheckByte=").append(byteToStringHex(mCheckByte));
        sb.append("}");
        return sb.toString();
    }

    /**
     * Dump
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AnswerToReset:");
        pw.println(toString());
        pw.flush();
    }
}
