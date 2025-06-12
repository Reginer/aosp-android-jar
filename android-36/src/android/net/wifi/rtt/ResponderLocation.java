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

package android.net.wifi.rtt;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.location.Address;
import android.location.Location;
import android.net.MacAddress;
import android.net.Uri;
import android.net.wifi.rtt.CivicLocationKeys.CivicLocationKeysType;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import java.lang.annotation.Retention;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ResponderLocation is both a Location Configuration Information (LCI) decoder and a Location Civic
 * Report (LCR) decoder for information received from a Wi-Fi Access Point (AP) during Wi-Fi RTT
 * ranging process.
 *
 * <p>This is based on the IEEE P802.11-REVmc/D8.0 spec section 9.4.2.22, under Measurement Report
 * Element. Subelement location data-fields parsed from separate input LCI and LCR Information
 * Elements are unified in this class.</p>
 *
 * <p>Note: The information provided by this class is broadcast by a responder (usually an Access
 * Point), and passed on as-is. There is no guarantee this information is accurate or correct, and
 * as a result developers should carefully consider how this information should be used and provide
 * corresponding advice to users.</p>
 */
public final class ResponderLocation implements Parcelable {
    private static final int BYTE_MASK = 0xFF;
    private static final int LSB_IN_BYTE = 0x01;
    private static final int MSB_IN_BYTE = 0x80;
    private static final int MIN_BUFFER_SIZE = 3; // length of LEAD_LCI_ELEMENT_BYTES
    private static final int MAX_BUFFER_SIZE = 256;

    // Information Element (IE) fields
    private static final byte MEASUREMENT_TOKEN_AUTONOMOUS = 0x01;
    private static final byte MEASUREMENT_REPORT_MODE = 0x00;
    private static final byte MEASUREMENT_TYPE_LCI = 0x08;
    private static final byte MEASUREMENT_TYPE_LCR = 0x0b;

    // LCI Subelement IDs
    private static final byte SUBELEMENT_LCI = 0x00;
    private static final byte SUBELEMENT_Z = 0x04;
    private static final byte SUBELEMENT_USAGE = 0x06;
    private static final byte SUBELEMENT_BSSID_LIST = 0x07;

    // LCI Subelement Lengths
    private static final int SUBELEMENT_LCI_LENGTH = 16;
    private static final int SUBELEMENT_Z_LENGTH = 6;
    private static final int SUBELEMENT_USAGE_LENGTH1 = 1;
    private static final int SUBELEMENT_USAGE_LENGTH3 = 3;
    private static final int SUBELEMENT_BSSID_LIST_MIN_BUFFER_LENGTH = 1;

    private static final byte[] LEAD_LCI_ELEMENT_BYTES = {
            MEASUREMENT_TOKEN_AUTONOMOUS, MEASUREMENT_REPORT_MODE, MEASUREMENT_TYPE_LCI
    };

    // Subelement LCI constants

    /* The LCI subelement bit-field lengths are described in Figure 9-214 of the REVmc spec and
    represented here as a an array of integers */
    private static final int[] SUBELEMENT_LCI_BIT_FIELD_LENGTHS = {
            6, 34, 6, 34, 4, 6, 30, 3, 1, 1, 1, 2
    };
    private static final int LATLNG_FRACTION_BITS = 25;
    private static final int LATLNG_UNCERTAINTY_BASE = 8;
    private static final int ALTITUDE_FRACTION_BITS = 8;
    private static final int ALTITUDE_UNCERTAINTY_BASE = 21;
    private static final double LAT_ABS_LIMIT = 90.0;
    private static final double LNG_ABS_LIMIT = 180.0;
    private static final int UNCERTAINTY_UNDEFINED = 0;

    // Subelement LCI fields indices
    private static final int SUBELEMENT_LCI_LAT_UNCERTAINTY_INDEX = 0;
    private static final int SUBELEMENT_LCI_LAT_INDEX = 1;
    private static final int SUBELEMENT_LCI_LNG_UNCERTAINTY_INDEX = 2;
    private static final int SUBELEMENT_LCI_LNG_INDEX = 3;
    private static final int SUBELEMENT_LCI_ALT_TYPE_INDEX = 4;
    private static final int SUBELEMENT_LCI_ALT_UNCERTAINTY_INDEX = 5;
    private static final int SUBELEMENT_LCI_ALT_INDEX = 6;
    private static final int SUBELEMENT_LCI_DATUM_INDEX = 7;
    private static final int SUBELEMENT_LCI_REGLOC_AGREEMENT_INDEX = 8;
    private static final int SUBELEMENT_LCI_REGLOC_DSE_INDEX = 9;
    private static final int SUBELEMENT_LCI_DEPENDENT_STA_INDEX = 10;
    private static final int SUBELEMENT_LCI_VERSION_INDEX = 11;

    /**
     * The Altitude value is interpreted based on the Altitude Type, and the selected mDatum.
     *
     * @hide
     */
    @Retention(SOURCE)
    @IntDef({ALTITUDE_UNDEFINED, ALTITUDE_METERS, ALTITUDE_FLOORS})
    public @interface AltitudeType {
    }

    /**
     * Altitude is not defined for the Responder.
     * The altitude and altitude uncertainty should not be used: see section 2.4 of IETF RFC 6225.
     */
    public static final int ALTITUDE_UNDEFINED = 0;
    /** Responder Altitude is measured in meters.  */
    public static final int ALTITUDE_METERS = 1;
    /** Responder Altitude is measured in floors. */
    public static final int ALTITUDE_FLOORS = 2;

    /**
     * The Datum value determines how coordinates are organized in relation to the real world.
     *
     * @hide
     */
    @Retention(SOURCE)
    @IntDef({DATUM_UNDEFINED, DATUM_WGS84, DATUM_NAD83_NAV88, DATUM_NAD83_MLLW})
    public @interface DatumType {
    }

    /** Datum is not defined. */
    public static final int DATUM_UNDEFINED = 0;
    /** Datum used is WGS84. */
    public static final int DATUM_WGS84 = 1;
    /** Datum used is NAD83 NAV88. */
    public static final int DATUM_NAD83_NAV88 = 2;
    /** Datum used is NAD83 MLLW. */
    public static final int DATUM_NAD83_MLLW = 3;


    /** Version of the LCI protocol is 1.0, the only defined protocol at this time. */
    public static final int LCI_VERSION_1 = 1;

    /** Provider/Source of the location */
    private static final String LOCATION_PROVIDER = "WiFi Access Point";

    // LCI Subelement Z constants
    private static final int[] SUBELEMENT_Z_BIT_FIELD_LENGTHS = {2, 14, 24, 8};
    private static final int Z_FLOOR_NUMBER_FRACTION_BITS = 4;
    private static final int Z_FLOOR_HEIGHT_FRACTION_BITS = 12;
    private static final int Z_MAX_HEIGHT_UNCERTAINTY_FACTOR = 25;

    // LCI Subelement Z fields indices
    private static final int SUBELEMENT_Z_LAT_EXPECTED_TO_MOVE_INDEX = 0;
    private static final int SUBELEMENT_Z_FLOOR_NUMBER_INDEX = 1;
    private static final int SUBELEMENT_Z_HEIGHT_ABOVE_FLOOR_INDEX = 2;
    private static final int SUBELEMENT_Z_HEIGHT_ABOVE_FLOOR_UNCERTAINTY_INDEX = 3;

    // LCI Subelement Usage Rules constants
    private static final int SUBELEMENT_USAGE_MASK_RETRANSMIT = 0x01;
    private static final int SUBELEMENT_USAGE_MASK_RETENTION_EXPIRES = 0x02;
    private static final int SUBELEMENT_USAGE_MASK_STA_LOCATION_POLICY = 0x04;

    // LCI Subelement Usage Rules field indices
    private static final int SUBELEMENT_USAGE_PARAMS_INDEX = 0; // 8 bits

    // LCI Subelement BSSID List
    private static final int SUBELEMENT_BSSID_MAX_INDICATOR_INDEX = 0;
    private static final int SUBELEMENT_BSSID_LIST_INDEX = 1;
    private static final int BYTES_IN_A_BSSID = 6;

    /**
     * The Expected-To-Move value determines how mobile we expect the STA to be.
     *
     * @hide
     */
    @Retention(SOURCE)
    @IntDef({LOCATION_FIXED, LOCATION_VARIABLE, LOCATION_MOVEMENT_UNKNOWN, LOCATION_RESERVED})
    public @interface ExpectedToMoveType {
    }

    /** Location of responder is fixed (does not move) */
    public static final int LOCATION_FIXED = 0;
    /** Location of the responder is variable, and may move */
    public static final int LOCATION_VARIABLE = 1;
    /** Location of the responder is not known to be either fixed or variable. */
    public static final int LOCATION_MOVEMENT_UNKNOWN = 2;
    /** Location of the responder status is a reserved value */
    public static final int LOCATION_RESERVED = 3;

    // LCR Subelement IDs
    private static final byte SUBELEMENT_LOCATION_CIVIC = 0x00;
    private static final byte SUBELEMENT_MAP_IMAGE = 0x05;

    // LCR Subelement Lengths
    private static final int SUBELEMENT_LOCATION_CIVIC_MIN_LENGTH = 2;
    private static final int SUBELEMENT_LOCATION_CIVIC_MAX_LENGTH = 256;
    private static final int SUBELEMENT_MAP_IMAGE_URL_MAX_LENGTH = 256;

    private static final byte[] LEAD_LCR_ELEMENT_BYTES = {
            MEASUREMENT_TOKEN_AUTONOMOUS, MEASUREMENT_REPORT_MODE, MEASUREMENT_TYPE_LCR
    };

    // LCR Location Civic Subelement
    private static final int CIVIC_COUNTRY_CODE_INDEX = 0;
    private static final int CIVIC_TLV_LIST_INDEX = 2;

    // LCR Map Image Subelement field indexes.
    private static final int SUBELEMENT_IMAGE_MAP_TYPE_INDEX = 0;
    private static final int MAP_TYPE_URL_DEFINED = 0;
    private static final String[] SUPPORTED_IMAGE_FILE_EXTENSIONS = {
            "",
            "png",
            "gif",
            "jpg",
            "svg",
            "dxf",
            "dwg",
            "dwf",
            "cad",
            "tif",
            "gml",
            "kml",
            "bmp",
            "pgm",
            "ppm",
            "xbm",
            "xpm",
            "ico"
    };

    // General LCI and LCR state
    private final boolean mIsValid;

    /*
      These members are not final because we are not sure if the corresponding subelement will be
      present until after the parsing process. However, the default value should be set as listed.
    */
    private boolean mIsLciValid = false;
    private boolean mIsZValid = false;
    private boolean mIsUsageValid = true; // By default this is assumed true
    private boolean mIsBssidListValid = false;
    private boolean mIsLocationCivicValid = false;
    private boolean mIsMapImageValid = false;

    // LCI Subelement LCI state
    private double mLatitudeUncertainty;
    private double mLatitude;
    private double mLongitudeUncertainty;
    private double mLongitude;
    private int mAltitudeType;
    private double mAltitudeUncertainty;
    private double mAltitude;
    private int mDatum;
    private boolean mLciRegisteredLocationAgreement;
    private boolean mLciRegisteredLocationDse;
    private boolean mLciDependentStation;
    private int mLciVersion;

    // LCI Subelement Z state
    private int mExpectedToMove;
    private double mFloorNumber;
    private double mHeightAboveFloorMeters;
    private double mHeightAboveFloorUncertaintyMeters;

    // LCI Subelement Usage Rights state
    private boolean mUsageRetransmit;
    private boolean mUsageRetentionExpires;
    private boolean mUsageExtraInfoOnAssociation;

    // LCI Subelement BSSID List state
    private ArrayList<MacAddress> mBssidList;

    // LCR Subelement Location Civic state
    private String mCivicLocationCountryCode;
    private String mCivicLocationString;
    private CivicLocation mCivicLocation;

    // LCR Subelement Map Image state
    private int mMapImageType;
    private Uri mMapImageUri;

    /**
     * Constructor
     *
     * @param lciBuffer the bytes received in the LCI Measurement Report Information Element
     * @param lcrBuffer the bytes received in the LCR Measurement Report Information Element
     *
     * @hide
     */
    public ResponderLocation(byte[] lciBuffer, byte[] lcrBuffer) {
        boolean isLciIeValid = false;
        boolean isLcrIeValid = false;
        setLciSubelementDefaults();
        setZaxisSubelementDefaults();
        setUsageSubelementDefaults();
        setBssidListSubelementDefaults();
        setCivicLocationSubelementDefaults();
        setMapImageSubelementDefaults();
        if (lciBuffer != null && lciBuffer.length > LEAD_LCI_ELEMENT_BYTES.length) {
            isLciIeValid = parseInformationElementBuffer(
                MEASUREMENT_TYPE_LCI, lciBuffer, LEAD_LCI_ELEMENT_BYTES);
        }
        if (lcrBuffer != null && lcrBuffer.length > LEAD_LCR_ELEMENT_BYTES.length) {
            isLcrIeValid = parseInformationElementBuffer(
                MEASUREMENT_TYPE_LCR, lcrBuffer, LEAD_LCR_ELEMENT_BYTES);
        }

        boolean isLciValid = isLciIeValid && mIsUsageValid
                && (mIsLciValid || mIsZValid || mIsBssidListValid);

        boolean isLcrValid = isLcrIeValid && mIsUsageValid
                && (mIsLocationCivicValid || mIsMapImageValid);

        mIsValid = isLciValid || isLcrValid;

        if (!mIsValid) {
            setLciSubelementDefaults();
            setZaxisSubelementDefaults();
            setCivicLocationSubelementDefaults();
            setMapImageSubelementDefaults();
        }
    }

    private ResponderLocation(Parcel in) {
        // Object Validation
        mIsValid = in.readByte() != 0;
        mIsLciValid = in.readByte() != 0;
        mIsZValid = in.readByte() != 0;
        mIsUsageValid = in.readByte() != 0;
        mIsBssidListValid = in.readByte() != 0;
        mIsLocationCivicValid = in.readByte() != 0;
        mIsMapImageValid = in.readByte() != 0;

        // LCI Subelement LCI state
        mLatitudeUncertainty = in.readDouble();
        mLatitude = in.readDouble();
        mLongitudeUncertainty = in.readDouble();
        mLongitude = in.readDouble();
        mAltitudeType = in.readInt();
        mAltitudeUncertainty = in.readDouble();
        mAltitude = in.readDouble();
        mDatum = in.readInt();
        mLciRegisteredLocationAgreement = in.readByte() != 0;
        mLciRegisteredLocationDse = in.readByte() != 0;
        mLciDependentStation = in.readByte() != 0;
        mLciVersion = in.readInt();

        // LCI Subelement Z state
        mExpectedToMove = in.readInt();
        mFloorNumber = in.readDouble();
        mHeightAboveFloorMeters = in.readDouble();
        mHeightAboveFloorUncertaintyMeters = in.readDouble();

        // LCI Usage Rights
        mUsageRetransmit = in.readByte() != 0;
        mUsageRetentionExpires = in.readByte() != 0;
        mUsageExtraInfoOnAssociation = in.readByte() != 0;

        // LCI Subelement BSSID List
        mBssidList = in.readArrayList(MacAddress.class.getClassLoader());

        // LCR Subelement Location Civic
        mCivicLocationCountryCode = in.readString();
        mCivicLocationString = in.readString();
        mCivicLocation = in.readParcelable(this.getClass().getClassLoader());

        // LCR Subelement Map Image
        mMapImageType = in.readInt();
        String urlString = in.readString();
        if (TextUtils.isEmpty(urlString)) {
            mMapImageUri = null;
        } else {
            mMapImageUri = Uri.parse(urlString);
        }
    }

    public static final @android.annotation.NonNull Creator<ResponderLocation> CREATOR = new Creator<ResponderLocation>() {
        @Override
        public ResponderLocation createFromParcel(Parcel in) {
            return new ResponderLocation(in);
        }

        @Override
        public ResponderLocation[] newArray(int size) {
            return new ResponderLocation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        // Object
        parcel.writeByte((byte) (mIsValid ? 1 : 0));
        parcel.writeByte((byte) (mIsLciValid ? 1 : 0));
        parcel.writeByte((byte) (mIsZValid ? 1 : 0));
        parcel.writeByte((byte) (mIsUsageValid ? 1 : 0));
        parcel.writeByte((byte) (mIsBssidListValid ? 1 : 0));
        parcel.writeByte((byte) (mIsLocationCivicValid ? 1 : 0));
        parcel.writeByte((byte) (mIsMapImageValid ? 1 : 0));

        // LCI Subelement LCI state
        parcel.writeDouble(mLatitudeUncertainty);
        parcel.writeDouble(mLatitude);
        parcel.writeDouble(mLongitudeUncertainty);
        parcel.writeDouble(mLongitude);
        parcel.writeInt(mAltitudeType);
        parcel.writeDouble(mAltitudeUncertainty);
        parcel.writeDouble(mAltitude);
        parcel.writeInt(mDatum);
        parcel.writeByte((byte) (mLciRegisteredLocationAgreement ? 1 : 0));
        parcel.writeByte((byte) (mLciRegisteredLocationDse ? 1 : 0));
        parcel.writeByte((byte) (mLciDependentStation ? 1 : 0));
        parcel.writeInt(mLciVersion);

        // LCI Subelement Z state
        parcel.writeInt(mExpectedToMove);
        parcel.writeDouble(mFloorNumber);
        parcel.writeDouble(mHeightAboveFloorMeters);
        parcel.writeDouble(mHeightAboveFloorUncertaintyMeters);

        // LCI Usage Rights
        parcel.writeByte((byte) (mUsageRetransmit ? 1 : 0));
        parcel.writeByte((byte) (mUsageRetentionExpires ? 1 : 0));
        parcel.writeByte((byte) (mUsageExtraInfoOnAssociation ? 1 : 0));

        // LCI Subelement BSSID List
        parcel.writeList(mBssidList);

        // LCR Subelement Location Civic
        parcel.writeString(mCivicLocationCountryCode);
        parcel.writeString(mCivicLocationString);
        parcel.writeParcelable(mCivicLocation, flags);

        // LCR Subelement Map Image
        parcel.writeInt(mMapImageType);
        if (mMapImageUri != null) {
            parcel.writeString(mMapImageUri.toString());
        } else {
            parcel.writeString("");
        }
    }

    /**
     * Test if the Information Element (IE) is in the correct format, and then parse its Subelements
     * based on their type, and setting state in this object when present.
     *
     * @return a boolean indicating the success of the parsing function
     */
    private boolean parseInformationElementBuffer(
            int ieType, byte[] buffer, byte[] expectedLeadBytes) {
        int bufferPtr = 0;
        int bufferLength = buffer.length;

        // Ensure the buffer size is within expected limits
        if (bufferLength < MIN_BUFFER_SIZE || bufferLength > MAX_BUFFER_SIZE) {
            return false;
        }

        // Ensure the IE contains the correct leading bytes
        byte[] leadBufferBytes = Arrays.copyOfRange(buffer, bufferPtr, expectedLeadBytes.length);
        // Ignore variable field measurement token number check, which is the first byte
        for (int i = 1; i < leadBufferBytes.length; i++) {
            if (leadBufferBytes[i] != expectedLeadBytes[i]) {
                return false;
            }
        }

        // Iterate through the sub-elements contained in the Information Element (IE)
        bufferPtr += expectedLeadBytes.length;
        // Loop over the buffer ensuring there are 2-bytes available for each new subelement tested.
        while (bufferPtr + 1 < bufferLength) {
            byte subelement = buffer[bufferPtr++];
            int subelementLength = buffer[bufferPtr++];
            // Check there is enough data for the next subelement
            if ((bufferPtr + subelementLength) > bufferLength || subelementLength <= 0) {
                return false;
            }

            byte[] subelementData =
                    Arrays.copyOfRange(buffer, bufferPtr, bufferPtr + subelementLength);

            if (ieType == MEASUREMENT_TYPE_LCI) {
                switch (subelement) {
                    case SUBELEMENT_LCI:
                        mIsLciValid = parseSubelementLci(subelementData);
                        if (!mIsLciValid || mLciVersion != LCI_VERSION_1) {
                            setLciSubelementDefaults();
                        }
                        break;
                    case SUBELEMENT_Z:
                        mIsZValid = parseSubelementZ(subelementData);
                        if (!mIsZValid) {
                            setZaxisSubelementDefaults();
                        }
                        break;
                    case SUBELEMENT_USAGE:
                        mIsUsageValid = parseSubelementUsage(subelementData);
                        // Note: if the Usage Subelement is not valid, don't reset the state, as
                        // it is now indicating the whole ResponderLocation is invalid.
                        break;
                    case SUBELEMENT_BSSID_LIST:
                        mIsBssidListValid = parseSubelementBssidList(subelementData);
                        if (!mIsBssidListValid) {
                            setBssidListSubelementDefaults();
                        }
                        break;
                    default:
                        break; // skip over unused or vendor specific subelements
                }
            } else if (ieType == MEASUREMENT_TYPE_LCR) {
                switch (subelement) {
                    case SUBELEMENT_LOCATION_CIVIC:
                        mIsLocationCivicValid = parseSubelementLocationCivic(subelementData);
                        if (!mIsLocationCivicValid) {
                            setCivicLocationSubelementDefaults();
                        }
                        break;
                    case SUBELEMENT_MAP_IMAGE:
                        mIsMapImageValid = parseSubelementMapImage(subelementData);
                        if (!mIsMapImageValid) {
                            setMapImageSubelementDefaults();
                        }
                        break;
                    default:
                        break; // skip over unused or other vendor specific subelements
                }
            }

            bufferPtr += subelementLength;
        }
        return true;
    }

    /**
     * Parse the LCI Sub-Element in the LCI Information Element (IE).
     *
     * @param buffer a buffer containing the subelement
     * @return boolean true indicates success
     */
    private boolean parseSubelementLci(byte[] buffer) {
        if (buffer.length > SUBELEMENT_LCI_LENGTH) {
            return false;
        }
        swapEndianByteByByte(buffer);
        long[] subelementLciFields = getFieldData(buffer, SUBELEMENT_LCI_BIT_FIELD_LENGTHS);
        if (subelementLciFields == null) {
            return false;
        }
        // Set member state based on parsed buffer data
        mLatitudeUncertainty = decodeLciLatLngUncertainty(
                subelementLciFields[SUBELEMENT_LCI_LAT_UNCERTAINTY_INDEX]);
        mLatitude = decodeLciLatLng(subelementLciFields, SUBELEMENT_LCI_BIT_FIELD_LENGTHS,
                SUBELEMENT_LCI_LAT_INDEX, LAT_ABS_LIMIT);
        mLongitudeUncertainty = decodeLciLatLngUncertainty(
                subelementLciFields[SUBELEMENT_LCI_LNG_UNCERTAINTY_INDEX]);
        mLongitude =
                decodeLciLatLng(subelementLciFields, SUBELEMENT_LCI_BIT_FIELD_LENGTHS,
                        SUBELEMENT_LCI_LNG_INDEX, LNG_ABS_LIMIT);
        mAltitudeType = (int) subelementLciFields[SUBELEMENT_LCI_ALT_TYPE_INDEX] & BYTE_MASK;
        mAltitudeUncertainty =
                decodeLciAltUncertainty(subelementLciFields[SUBELEMENT_LCI_ALT_UNCERTAINTY_INDEX]);
        mAltitude =
                Math.scalb(subelementLciFields[SUBELEMENT_LCI_ALT_INDEX], -ALTITUDE_FRACTION_BITS);
        mDatum = (int) subelementLciFields[SUBELEMENT_LCI_DATUM_INDEX] & BYTE_MASK;
        mLciRegisteredLocationAgreement =
                (subelementLciFields[SUBELEMENT_LCI_REGLOC_AGREEMENT_INDEX] == 1);
        mLciRegisteredLocationDse =
                (subelementLciFields[SUBELEMENT_LCI_REGLOC_DSE_INDEX] == 1);
        mLciDependentStation =
                (subelementLciFields[SUBELEMENT_LCI_DEPENDENT_STA_INDEX] == 1);
        mLciVersion = (int) subelementLciFields[SUBELEMENT_LCI_VERSION_INDEX];
        return true;
    }

    /**
     * Decode the floating point value of an encoded lat or lng in the LCI subelement field.
     *
     * @param fields        the array of field data represented as longs
     * @param bitFieldSizes the lengths of each field
     * @param offset        the offset of the field being decoded
     * @param limit the maximum absolute value (note: different for lat vs lng)
     * @return the floating point value of the lat or lng
     */
    private double decodeLciLatLng(long[] fields, int[] bitFieldSizes, int offset, double limit) {
        double angle;
        if ((fields[offset] & (long) Math.pow(2, bitFieldSizes[offset] - 1)) != 0) {
            // Negative 2's complement value
            // Note: The Math.pow(...) method cannot return a NaN value because the bitFieldSize
            // for Lat or Lng is limited to exactly 34 bits.
            angle = Math.scalb((double) fields[offset] - Math.pow(2, bitFieldSizes[offset]),
                    -LATLNG_FRACTION_BITS);
        } else {
            // Positive 2's complement value
            angle = Math.scalb((double) fields[offset], -LATLNG_FRACTION_BITS);
        }
        if (angle > limit) {
            angle = limit;
        } else if (angle < -limit) {
            angle = -limit;
        }
        return angle;
    }

    /**
     * Coverts an encoded Lat/Lng uncertainty into a number of degrees.
     *
     * @param encodedValue the encoded uncertainty
     * @return the value in degrees
     */
    private double decodeLciLatLngUncertainty(long encodedValue) {
        return Math.pow(2, LATLNG_UNCERTAINTY_BASE - encodedValue);
    }

    /**
     * Converts an encoded Alt uncertainty into a number of degrees.
     *
     * @param encodedValue the encoded uncertainty
     * @return the value in degrees
     */
    private double decodeLciAltUncertainty(long encodedValue) {
        return Math.pow(2, ALTITUDE_UNCERTAINTY_BASE - encodedValue);
    }

    /**
     * Parse the Z subelement of the LCI IE.
     *
     * @param buffer a buffer containing the subelement
     * @return boolean true indicates success
     */
    private boolean parseSubelementZ(byte[] buffer) {
        if (buffer.length != SUBELEMENT_Z_LENGTH) {
            return false;
        }
        swapEndianByteByByte(buffer);
        long[] subelementZFields = getFieldData(buffer, SUBELEMENT_Z_BIT_FIELD_LENGTHS);
        if (subelementZFields == null) {
            return false;
        }

        mExpectedToMove =
                (int) subelementZFields[SUBELEMENT_Z_LAT_EXPECTED_TO_MOVE_INDEX] & BYTE_MASK;
        mFloorNumber = decodeZUnsignedToSignedValue(subelementZFields,
                SUBELEMENT_Z_BIT_FIELD_LENGTHS, SUBELEMENT_Z_FLOOR_NUMBER_INDEX,
                Z_FLOOR_NUMBER_FRACTION_BITS);

        mHeightAboveFloorMeters = decodeZUnsignedToSignedValue(subelementZFields,
                SUBELEMENT_Z_BIT_FIELD_LENGTHS, SUBELEMENT_Z_HEIGHT_ABOVE_FLOOR_INDEX,
                Z_FLOOR_HEIGHT_FRACTION_BITS);

        long zHeightUncertainty =
                subelementZFields[SUBELEMENT_Z_HEIGHT_ABOVE_FLOOR_UNCERTAINTY_INDEX];
        if (zHeightUncertainty >= 0 && zHeightUncertainty < Z_MAX_HEIGHT_UNCERTAINTY_FACTOR) {
            mHeightAboveFloorUncertaintyMeters = zHeightUncertainty == 0
                    ? 0 : Math.pow(2, Z_FLOOR_HEIGHT_FRACTION_BITS - zHeightUncertainty - 1);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Decode a two's complement encoded value, to a signed double based on the field length.
     *
     * @param fieldValues the array of field values reprented as longs
     * @param fieldLengths the array of field lengths
     * @param index the index of the field being decoded
     * @param fraction the number of fractional bits in the value
     * @return the signed value represented as a double
     */
    private double decodeZUnsignedToSignedValue(long[] fieldValues, int[] fieldLengths, int index,
            int fraction) {
        int value = (int) fieldValues[index];
        int maxPositiveValue = (int) Math.pow(2, fieldLengths[index] - 1) - 1;
        if (value > maxPositiveValue) {
            value -= Math.pow(2, fieldLengths[index]);
        }
        return Math.scalb(value, -fraction);
    }

    /**
     * Parse Subelement Usage Rights
     */
    private boolean parseSubelementUsage(byte[] buffer) {
        if (buffer.length != SUBELEMENT_USAGE_LENGTH1
                && buffer.length != SUBELEMENT_USAGE_LENGTH3) {
            return false;
        }
        mUsageRetransmit =
                (buffer[SUBELEMENT_USAGE_PARAMS_INDEX] & SUBELEMENT_USAGE_MASK_RETRANSMIT) != 0;
        mUsageRetentionExpires =
                (buffer[SUBELEMENT_USAGE_PARAMS_INDEX] & SUBELEMENT_USAGE_MASK_RETENTION_EXPIRES)
                        != 0;
        mUsageExtraInfoOnAssociation =
                (buffer[SUBELEMENT_USAGE_PARAMS_INDEX] & SUBELEMENT_USAGE_MASK_STA_LOCATION_POLICY)
                        != 0;
        // Note: the Retransmit flag must be true, and RetentionExpires, false for the
        // ResponderLocation object to be usable by public applications.
        return (mUsageRetransmit && !mUsageRetentionExpires);
    }

    /**
     * Parse the BSSID List Subelement of the LCI IE.
     *
     * @param buffer a buffer containing the subelement
     * @return boolean true indicates success
     */
    private boolean parseSubelementBssidList(byte[] buffer) {
        if (buffer.length < SUBELEMENT_BSSID_LIST_MIN_BUFFER_LENGTH) {
            return false;
        }
        if ((buffer.length - 1) % BYTES_IN_A_BSSID != 0) {
            return false;
        }

        int maxBssidIndicator = (int) buffer[SUBELEMENT_BSSID_MAX_INDICATOR_INDEX] & BYTE_MASK;
        int bssidListLength = (buffer.length - 1) / BYTES_IN_A_BSSID;
        // The maxBSSIDIndicator is ignored. Its use is still being clarified in 802.11REVmd,
        // which is not published at this time. This field will be used in a future
        // release of Android after 802.11REVmd is public. Here, we interpret the following
        // params as an explicit list of BSSIDs.


        int bssidOffset = SUBELEMENT_BSSID_LIST_INDEX;
        for (int i = 0; i < bssidListLength; i++) {
            byte[] bssid = Arrays.copyOfRange(buffer, bssidOffset, bssidOffset + BYTES_IN_A_BSSID);
            MacAddress macAddress = MacAddress.fromBytes(bssid);
            mBssidList.add(macAddress);
            bssidOffset += BYTES_IN_A_BSSID;
        }
        return true;
    }

    /**
     * Parse the Location Civic subelement in the LCR IE.
     *
     * @param buffer a buffer containing the subelement
     * @return boolean true indicates success
     */
    private boolean parseSubelementLocationCivic(byte[] buffer) {
        if (buffer.length <  SUBELEMENT_LOCATION_CIVIC_MIN_LENGTH
                || buffer.length > SUBELEMENT_LOCATION_CIVIC_MAX_LENGTH) {
            return false;
        }
        mCivicLocationCountryCode =
                new String(Arrays.copyOfRange(buffer, CIVIC_COUNTRY_CODE_INDEX,
                        CIVIC_TLV_LIST_INDEX)).toUpperCase();
        CivicLocation civicLocation =
                new CivicLocation(
                        Arrays.copyOfRange(buffer, CIVIC_TLV_LIST_INDEX, buffer.length),
                        mCivicLocationCountryCode);
        if (!civicLocation.isValid()) {
            return false;
        }
        this.mCivicLocation = civicLocation;
        mCivicLocationString = civicLocation.toString();
        return true;
    }

    /**
     * Parse the Map Image subelement in the LCR IE.
     *
     * @param buffer a buffer containing the subelement
     * @return boolean true indicates success
     */
    private boolean parseSubelementMapImage(byte[] buffer) {
        if (buffer.length > SUBELEMENT_MAP_IMAGE_URL_MAX_LENGTH) {
            return false;
        }
        int mapImageType = buffer[SUBELEMENT_IMAGE_MAP_TYPE_INDEX];
        int supportedTypesMax = SUPPORTED_IMAGE_FILE_EXTENSIONS.length - 1;
        if (mapImageType < MAP_TYPE_URL_DEFINED || mapImageType > supportedTypesMax) {
            return false;
        }
        this.mMapImageType = mapImageType;
        byte[] urlBytes = Arrays.copyOfRange(buffer, 1, buffer.length);
        mMapImageUri = Uri.parse(new String(urlBytes, StandardCharsets.UTF_8));
        return true;
    }

    /**
     * Convert an image type code to a Mime type
     *
     * @param imageTypeCode encoded as an integer
     * @return the mime type of the image file
     */
    private String imageTypeToMime(int imageTypeCode, String imageUrl) {
        int supportedExtensionsMax = SUPPORTED_IMAGE_FILE_EXTENSIONS.length - 1;
        if ((imageTypeCode == 0 && imageUrl == null) || imageTypeCode > supportedExtensionsMax) {
            return null;
        }
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        if (imageTypeCode == 0) {
            return mimeTypeMap.getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(imageUrl));
        } else {
            return mimeTypeMap.getMimeTypeFromExtension(
                    SUPPORTED_IMAGE_FILE_EXTENSIONS[imageTypeCode]);
        }
    }

    /**
     * Converts a byte array containing fields of variable size, into an array of longs where the
     * field boundaries are defined in a constant int array passed as an argument.
     *
     * @param buffer        the byte array containing all the fields
     * @param bitFieldSizes the int array defining the size of each field
     */
    private long[] getFieldData(byte[] buffer, int[] bitFieldSizes) {
        int bufferLengthBits = buffer.length * Byte.SIZE;
        int sumBitFieldSizes = 0;
        for (int i : bitFieldSizes) {
            if (i > Long.SIZE) {
                return null;
            }
            sumBitFieldSizes += i;
        }
        if (bufferLengthBits != sumBitFieldSizes) {
            return null;
        }
        long[] fieldData = new long[bitFieldSizes.length];
        int bufferBitPos = 0;
        for (int fieldIndex = 0; fieldIndex < bitFieldSizes.length; fieldIndex++) {
            int bitFieldSize = bitFieldSizes[fieldIndex];
            long field = 0;
            for (int n = 0; n < bitFieldSize; n++) {
                field |= ((long) getBitAtBitOffsetInByteArray(buffer, bufferBitPos + n) << n);
            }
            fieldData[fieldIndex] = field;
            bufferBitPos += bitFieldSize;
        }
        return fieldData;
    }

    /**
     * Retrieves state of a bit at the bit-offset in a byte array, where the offset represents the
     * bytes in contiguous data with each value in big endian order.
     *
     * @param buffer          the data buffer of bytes containing all the fields
     * @param bufferBitOffset the bit offset into the entire buffer
     * @return a zero or one value, representing the state of that bit.
     */
    private int getBitAtBitOffsetInByteArray(byte[] buffer, int bufferBitOffset) {
        int bufferIndex = bufferBitOffset / Byte.SIZE; // The byte index that contains the bit
        int bitOffsetInByte = bufferBitOffset % Byte.SIZE; // The bit offset within that byte
        int result = (buffer[bufferIndex] & (MSB_IN_BYTE >> bitOffsetInByte)) == 0 ? 0 : 1;
        return result;
    }

    /**
     * Reverses the order of the bits in each byte of a byte array.
     *
     * @param buffer the array containing each byte that will be reversed
     */
    private void swapEndianByteByByte(byte[] buffer) {
        for (int n = 0; n < buffer.length; n++) {
            byte currentByte = buffer[n];
            byte reversedByte = 0; // Cleared value
            byte bitSelectorMask = LSB_IN_BYTE;
            for (int i = 0; i < Byte.SIZE; i++) {
                reversedByte = (byte) (reversedByte << 1);
                if ((currentByte & bitSelectorMask) != 0) {
                    reversedByte = (byte) (reversedByte | LSB_IN_BYTE);
                }
                bitSelectorMask = (byte) (bitSelectorMask << 1);
            }
            buffer[n] = reversedByte;
        }
    }

    /**
     * Sets the LCI subelement fields to the default undefined values.
     */
    private void setLciSubelementDefaults() {
        mIsLciValid = false;
        mLatitudeUncertainty = UNCERTAINTY_UNDEFINED;
        mLatitude = 0;
        mLongitudeUncertainty = UNCERTAINTY_UNDEFINED;
        mLongitude = 0;
        mAltitudeType = ALTITUDE_UNDEFINED;
        mAltitudeUncertainty = UNCERTAINTY_UNDEFINED;
        mAltitude = 0;
        mDatum = DATUM_UNDEFINED;
        mLciRegisteredLocationAgreement = false;
        mLciRegisteredLocationDse = false;
        mLciDependentStation = false;
        mLciVersion = 0;
    }

    /**
     * Sets the Z subelement fields to the default values when undefined.
     */
    private void setZaxisSubelementDefaults() {
        mIsZValid = false;
        mExpectedToMove = 0;
        mFloorNumber = 0;
        mHeightAboveFloorMeters = 0;
        mHeightAboveFloorUncertaintyMeters = 0;
    }

    /**
     * Sets the Usage Policy subelement fields to the default undefined values.
     */
    private void setUsageSubelementDefaults() {
        mUsageRetransmit = true;
        mUsageRetentionExpires = false;
        mUsageExtraInfoOnAssociation = false;
    }

    /**
     * Sets the BSSID List subelement fields to the default values when undefined.
     */
    private void setBssidListSubelementDefaults() {
        mIsBssidListValid = false;
        mBssidList = new ArrayList<>();
    }

    /**
     * Sets the LCR Civic Location subelement field to the default undefined value.
     *
     * @hide
     */
    public void setCivicLocationSubelementDefaults() {
        mIsLocationCivicValid = false;
        mCivicLocationCountryCode = "";
        mCivicLocationString = "";
        mCivicLocation = null;
    }

    /**
     * Sets the LCR Map Image subelement field to the default values when undefined.
     */
    private void setMapImageSubelementDefaults() {
        mIsMapImageValid = false;
        mMapImageType = MAP_TYPE_URL_DEFINED;
        mMapImageUri = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ResponderLocation other = (ResponderLocation) obj;
        return mIsValid == other.mIsValid
                && mIsLciValid == other.mIsLciValid
                && mIsZValid == other.mIsZValid
                && mIsUsageValid == other.mIsUsageValid
                && mIsBssidListValid == other.mIsBssidListValid
                && mIsLocationCivicValid == other.mIsLocationCivicValid
                && mIsMapImageValid == other.mIsMapImageValid
                && mLatitudeUncertainty == other.mLatitudeUncertainty
                && mLatitude == other.mLatitude
                && mLongitudeUncertainty == other.mLongitudeUncertainty
                && mLongitude == other.mLongitude
                && mAltitudeType == other.mAltitudeType
                && mAltitudeUncertainty == other.mAltitudeUncertainty
                && mAltitude == other.mAltitude
                && mDatum == other.mDatum
                && mLciRegisteredLocationAgreement == other.mLciRegisteredLocationAgreement
                && mLciRegisteredLocationDse == other.mLciRegisteredLocationDse
                && mLciDependentStation == other.mLciDependentStation
                && mLciVersion == other.mLciVersion
                && mExpectedToMove == other.mExpectedToMove
                && mFloorNumber == other.mFloorNumber
                && mHeightAboveFloorMeters == other.mHeightAboveFloorMeters
                && mHeightAboveFloorUncertaintyMeters
                        == other.mHeightAboveFloorUncertaintyMeters
                && mUsageRetransmit == other.mUsageRetransmit
                && mUsageRetentionExpires == other.mUsageRetentionExpires
                && mUsageExtraInfoOnAssociation == other.mUsageExtraInfoOnAssociation
                && mBssidList.equals(other.mBssidList)
                && mCivicLocationCountryCode.equals(other.mCivicLocationCountryCode)
                && mCivicLocationString.equals(other.mCivicLocationString)
                && Objects.equals(mCivicLocation, other.mCivicLocation)
                && mMapImageType == other.mMapImageType
                && Objects.equals(mMapImageUri, other.mMapImageUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsValid, mIsLciValid, mIsZValid, mIsUsageValid, mIsBssidListValid,
                mIsLocationCivicValid, mIsMapImageValid, mLatitudeUncertainty, mLatitude,
                mLongitudeUncertainty, mLongitude, mAltitudeType, mAltitudeUncertainty, mAltitude,
                mDatum, mLciRegisteredLocationAgreement,
                mLciRegisteredLocationDse, mLciDependentStation, mLciVersion,
                mExpectedToMove, mFloorNumber, mHeightAboveFloorMeters,
                mHeightAboveFloorUncertaintyMeters, mUsageRetransmit, mUsageRetentionExpires,
                mUsageExtraInfoOnAssociation, mBssidList, mCivicLocationCountryCode,
                mCivicLocationString, mCivicLocation, mMapImageType, mMapImageUri);
    }

    /**
     * @return true if the ResponderLocation object is valid and contains useful information
     * relevant to the location of the Responder. If this is ever false, this object will not be
     * available to developers, and have a null value.
     *
     * @hide
     */
    public boolean isValid() {
        return mIsValid;
    }

    /**
     * @return true if the LCI subelement (containing Latitude, Longitude and Altitude) is valid.
     *
     * <p> This method tells us if the responder has provided its Location Configuration
     * Information (LCI) directly, and is useful when an external database of responder locations
     * is not available</p>
     *
     * <p>If isLciSubelementValid() returns true, all the LCI values provided by the corresponding
     * getter methods will have been set as described by the responder, or else if false, they
     * should not be used and will throw an IllegalStateException.</p>
     */
    public boolean isLciSubelementValid() {
        return mIsLciValid;
    }

    /**
     * @return the latitude uncertainty in degrees.
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     * </p>
     * <p> An unknown uncertainty is indicated by 0.</p>
     */
    public double getLatitudeUncertainty() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getLatitudeUncertainty(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mLatitudeUncertainty;
    }

    /**
     * @return the latitude in degrees
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    public double getLatitude() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getLatitude(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mLatitude;
    }

    /**
     * @return the Longitude uncertainty in degrees.
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     * </p>
     * <p> An unknown uncertainty is indicated by 0.</p>
     */
    public double getLongitudeUncertainty() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getLongitudeUncertainty(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mLongitudeUncertainty;
    }

    /**
     * @return the Longitude in degrees..
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    public double getLongitude() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getLatitudeUncertainty(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mLongitude;
    }

    /**
     * @return the Altitude type.
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    @AltitudeType
    public int getAltitudeType() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getLatitudeUncertainty(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mAltitudeType;
    }

    /**
     * @return the Altitude uncertainty in meters.
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     * </p>
     * <p>An unknown uncertainty is indicated by 0.</p>
     */
    public double getAltitudeUncertainty() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getLatitudeUncertainty(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mAltitudeUncertainty;
    }

    /**
     * @return the Altitude in units defined by the altitude type.
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    public double getAltitude() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getAltitude(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mAltitude;
    }

    /**
     * @return the Datum used for the LCI positioning information.
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    @DatumType
    public int getDatum() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getDatum(): invoked on an invalid result: mIsLciValid = false.");
        }
        return mDatum;
    }

    /**
     * @return true if the station is operating within a national policy area or an international
     * agreement area near a national border, otherwise false
     * (see 802.11REVmc Section 11.12.3 - Registered STA Operation).
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    public boolean getRegisteredLocationAgreementIndication() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getRegisteredLocationAgreementIndication(): "
                        + "invoked on an invalid result: mIsLciValid = false.");
        }
        return mLciRegisteredLocationAgreement;
    }

    /**
     * @return true indicating this is an enabling station, enabling the operation of nearby STAs
     * with Dynamic Station Enablement (DSE), otherwise false.
     * (see 802.11REVmc Section 11.12.3 - Registered STA Operation).
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     *
     * @hide
     */
    public boolean getRegisteredLocationDseIndication() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getRegisteredLocationDseIndication(): "
                    + "invoked on an invalid result: mIsLciValid = false.");
        }
        return mLciRegisteredLocationDse;
    }

    /**
     * @return true indicating this is a dependent station that is operating with the enablement of
     * an enabling station whose LCI is being reported, otherwise false.
     * (see 802.11REVmc Section 11.12.3 - Registered STA Operation).
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     *
     * @hide
     */
    public boolean getDependentStationIndication() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getDependentStationIndication(): "
                    + "invoked on an invalid result: mIsLciValid = false.");
        }
        return mLciDependentStation;
    }

    /**
     * @return a value greater or equal to 1, indicating the current version number
     * of the LCI protocol.
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    public int getLciVersion() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "getLciVersion(): "
                    + "invoked on an invalid result: mIsLciValid = false.");
        }
        return mLciVersion;
    }

    /**
     * @return the LCI location represented as a {@link Location} object (best effort).
     * <p>
     * Only valid if {@link #isLciSubelementValid()} returns true, or will throw an exception.
     */
    @NonNull
    public Location toLocation() {
        if (!mIsLciValid) {
            throw new IllegalStateException(
                "toLocation(): "
                    + "invoked on an invalid result: mIsLciValid = false.");
        }
        Location location = new Location(LOCATION_PROVIDER);
        location.setLatitude(mLatitude);
        location.setLongitude(mLongitude);
        location.setAccuracy((float) (mLatitudeUncertainty + mLongitudeUncertainty) / 2);
        location.setAltitude(mAltitude);
        location.setVerticalAccuracyMeters((float) mAltitudeUncertainty);
        location.setTime(System.currentTimeMillis());
        return location;
    }

    /**
     * @return if the Z subelement (containing mobility, Floor, Height above floor) is valid.
     */
    public boolean isZaxisSubelementValid() {
        return mIsZValid;
    }

    /**
     * @return an integer representing the mobility of the responder.
     * <p>
     * Only valid if {@link #isZaxisSubelementValid()} returns true, or will throw an exception.
     */
    @ExpectedToMoveType
    public int getExpectedToMove() {
        if (!mIsZValid) {
            throw new IllegalStateException(
                "getExpectedToMove(): invoked on an invalid result: mIsZValid = false.");
        }
        return mExpectedToMove;
    }

    /**
     * @return the Z sub element Floor Number.
     * <p>
     * Only valid if {@link #isZaxisSubelementValid()} returns true, or will throw an exception.
     * </p>
     * <p>Note: this number can be positive or negative, with value increments of +/- 1/16 of a
     * floor.</p>.
     */
    public double getFloorNumber() {
        if (!mIsZValid) {
            throw new IllegalStateException(
                "getFloorNumber(): invoked on an invalid result: mIsZValid = false)");
        }
        return mFloorNumber;
    }

    /**
     * @return the Z subelement Height above the floor in meters.
     * <p>
     * Only valid if {@link #isZaxisSubelementValid()} returns true, or will throw an exception.
     * </p>
     * <p>This value can be positive or negative. </p>
     */
    public double getHeightAboveFloorMeters() {
        if (!mIsZValid) {
            throw new IllegalStateException(
                "getHeightAboveFloorMeters(): invoked on an invalid result: mIsZValid = false)");
        }
        return mHeightAboveFloorMeters;
    }

    /**
     * @return the Z subelement Height above the floor uncertainty in meters.
     * <p>
     * Only valid if {@link #isZaxisSubelementValid()} returns true, or will throw an exception.
     * </p>
     * <p>An unknown uncertainty is indicated by 0.</p>
     */
    public double getHeightAboveFloorUncertaintyMeters() {
        if (!mIsZValid) {
            throw new IllegalStateException(
                "getHeightAboveFloorUncertaintyMeters():"
                    + "invoked on an invalid result: mIsZValid = false)");
        }
        return mHeightAboveFloorUncertaintyMeters;
    }

    /**
     * @return true if the location information received from the responder can be
     * retransmitted to another device, physically separate from the one that received it.
     *
     * @hide
     */
    public boolean getRetransmitPolicyIndication() {
        return mUsageRetransmit;
    }

    /**
     * @return true if location-data received should expire (and be deleted)
     * by the time provided in the getRelativeExpirationTimeHours() method.
     *
     * @hide
     */
    public boolean getRetentionExpiresIndication() {
        return mUsageRetentionExpires;
    }

    /**
     * @return true if there is extra location info available on association.
     *
     * @hide
     */
    @SystemApi
    public boolean getExtraInfoOnAssociationIndication() {
        return mUsageExtraInfoOnAssociation;
    }

    /**
     * @return the Immutable list of colocated BSSIDs at the responder.
     *
     * <p> Will return an empty list when there are no bssids listed.
     */
    public List<MacAddress> getColocatedBssids() {
        return Collections.unmodifiableList(mBssidList);
    }

    /**
     * @return the civic location represented as an {@link Address} object (best effort).
     *
     * <p> Will return a {@code null} when there is no Civic Location defined.
     */
    @Nullable
    public Address toCivicLocationAddress() {
        if (mCivicLocation != null && mCivicLocation.isValid()) {
            return mCivicLocation.toAddress();
        } else {
            return null;
        }
    }

    /**
     * @return the civic location represented as a {@link SparseArray}
     * <p>
     * Valid keys to access the SparseArray can be found in {@code CivicLocationKeys}.
     * </p>
     * <p> Will return a {@code null} when there is no Civic Location defined.
     *
     */
    @Nullable
    @SuppressLint("ChangedType")
    public SparseArray<String> toCivicLocationSparseArray() {
        if (mCivicLocation != null && mCivicLocation.isValid()) {
            return mCivicLocation.toSparseArray();
        } else {
            return null;
        }
    }

    /**
     * @return the civic location two upper-case ASCII character country code defined in ISO 3166.
     *
     * <p> Will return a {@code null} when there is no country code defined.
     *
     * @hide
     */
    @Nullable
    public String getCivicLocationCountryCode() {
        return mCivicLocationCountryCode;
    }

    /**
     * @return the value of the Civic Location String associated with a key.
     *
     * <p> Will return a {@code null} when there is no value associated with the key provided.
     *
     * @param key used to find a corresponding value in the Civic Location Tuple list
     *
     * @hide
     */
    @Nullable
    public String getCivicLocationElementValue(@CivicLocationKeysType int key) {
        return mCivicLocation.getCivicElementValue(key);
    }

    /**
     * @return the Map Image file Mime type, referred to by getMapImageUrl().
     */
    @Nullable
    public String getMapImageMimeType() {
        if (mMapImageUri == null) {
            return null;
        } else {
            return imageTypeToMime(mMapImageType, mMapImageUri.toString());
        }
    }

    /**
     * @return a URI referencing a map-file showing the local floor plan.
     *
     * <p> Will return a {@code null} when there is no URI defined.
     */
    @Nullable
    public Uri getMapImageUri() {
        return mMapImageUri;
    }
}
