/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.telephony;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Immutable cell information from a point in time.
 */
public abstract class CellInfo implements Parcelable {

    /**
     * This value indicates that the integer field is unreported.
     */
    public static final int UNAVAILABLE = Integer.MAX_VALUE;

    /**
     * This value indicates that the long field is unreported.
     */
    public static final long UNAVAILABLE_LONG = Long.MAX_VALUE;

    /**
     * Cell identity type
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "TYPE_",
            value = {TYPE_GSM, TYPE_CDMA, TYPE_LTE, TYPE_WCDMA, TYPE_TDSCDMA, TYPE_NR})
    public @interface Type {}

    /**
     * Unknown cell identity type
     * @hide
     */
    public static final int TYPE_UNKNOWN = 0;

    /**
     * GSM cell identity type
     * @hide
     */
    public static final int TYPE_GSM = 1;

    /**
     * CDMA cell identity type
     * @hide
     */
    public static final int TYPE_CDMA = 2;

    /**
     * LTE cell identity type
     * @hide
     */
    public static final int TYPE_LTE = 3;

    /**
     * WCDMA cell identity type
     * @hide
     */
    public static final int TYPE_WCDMA = 4;

    /**
     * TD-SCDMA cell identity type
     * @hide
     */
    public static final int TYPE_TDSCDMA = 5;

    /**
     * 5G cell identity type
     * @hide
     */
    public static final int TYPE_NR = 6;

    // Type to distinguish where time stamp gets recorded.

    /** @hide */
    @UnsupportedAppUsage
    public static final int TIMESTAMP_TYPE_UNKNOWN = 0;
    /** @hide */
    @UnsupportedAppUsage
    public static final int TIMESTAMP_TYPE_ANTENNA = 1;
    /** @hide */
    @UnsupportedAppUsage
    public static final int TIMESTAMP_TYPE_MODEM = 2;
    /** @hide */
    @UnsupportedAppUsage
    public static final int TIMESTAMP_TYPE_OEM_RIL = 3;
    /** @hide */
    @UnsupportedAppUsage
    public static final int TIMESTAMP_TYPE_JAVA_RIL = 4;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        CONNECTION_NONE,
        CONNECTION_PRIMARY_SERVING,
        CONNECTION_SECONDARY_SERVING,
        CONNECTION_UNKNOWN
    })
    public @interface CellConnectionStatus {}

    /**
     * Cell is not a serving cell.
     *
     * <p>The cell has been measured but is neither a camped nor serving cell (3GPP 36.304).
     */
    public static final int CONNECTION_NONE = 0;

    /** UE is connected to cell for signalling and possibly data (3GPP 36.331, 25.331). */
    public static final int CONNECTION_PRIMARY_SERVING = 1;

    /** UE is connected to cell for data (3GPP 36.331, 25.331). */
    public static final int CONNECTION_SECONDARY_SERVING = 2;

    /** Connection status is unknown. */
    public static final int CONNECTION_UNKNOWN = Integer.MAX_VALUE;

    /** A cell connection status */
    private int mCellConnectionStatus;

    // True if device is mRegistered to the mobile network
    private boolean mRegistered;

    // Observation time stamped as type in nanoseconds since boot
    private long mTimeStamp;

    /** @hide */
    protected CellInfo(int cellConnectionStatus, boolean registered, long timestamp) {
        mCellConnectionStatus = cellConnectionStatus;
        mRegistered = registered;
        mTimeStamp = timestamp;
    }

    /** @hide */
    protected CellInfo() {
        this.mRegistered = false;
        this.mTimeStamp = Long.MAX_VALUE;
        this.mCellConnectionStatus = CONNECTION_NONE;
    }

    /** @hide */
    protected CellInfo(CellInfo ci) {
        this.mRegistered = ci.mRegistered;
        this.mTimeStamp = ci.mTimeStamp;
        this.mCellConnectionStatus = ci.mCellConnectionStatus;
    }

    /**
     * True if the phone is registered to a mobile network that provides service on this cell
     * and this cell is being used or would be used for network signaling.
     */
    public boolean isRegistered() {
        return mRegistered;
    }

    /** @hide */
    public void setRegistered(boolean registered) {
        mRegistered = registered;
    }

    /**
     * Approximate time this cell information was received from the modem.
     *
     * @return a time stamp in millis since boot.
     */
    @ElapsedRealtimeLong
    public long getTimestampMillis() {
        return mTimeStamp / 1000000;
    }

    /**
     * Approximate time this cell information was received from the modem.
     *
     * @return a time stamp in nanos since boot.
     * @deprecated Use {@link #getTimestampMillis} instead.
     */
    @Deprecated
    public long getTimeStamp() {
        return mTimeStamp;
    }

    /** @hide */
    @VisibleForTesting
    public void setTimeStamp(long ts) {
        mTimeStamp = ts;
    }

    /**
     * @return a {@link CellIdentity} instance.
     */
    @NonNull
    public abstract CellIdentity getCellIdentity();

    /**
     * @return a {@link CellSignalStrength} instance.
     */
    @NonNull
    public abstract CellSignalStrength getCellSignalStrength();

    /** @hide */
    public CellInfo sanitizeLocationInfo() {
        return null;
    }

    /**
     * Gets the connection status of this cell.
     *
     * @see #CONNECTION_NONE
     * @see #CONNECTION_PRIMARY_SERVING
     * @see #CONNECTION_SECONDARY_SERVING
     * @see #CONNECTION_UNKNOWN
     *
     * @return The connection status of the cell.
     */
    @CellConnectionStatus
    public int getCellConnectionStatus() {
        return mCellConnectionStatus;
    }
    /** @hide */
    public void setCellConnectionStatus(@CellConnectionStatus int cellConnectionStatus) {
        mCellConnectionStatus = cellConnectionStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCellConnectionStatus, mRegistered, mTimeStamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellInfo)) return false;
        CellInfo cellInfo = (CellInfo) o;
        return mCellConnectionStatus == cellInfo.mCellConnectionStatus
                && mRegistered == cellInfo.mRegistered
                && mTimeStamp == cellInfo.mTimeStamp;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("mRegistered=").append(mRegistered ? "YES" : "NO");
        sb.append(" mTimeStamp=").append(mTimeStamp).append("ns");
        sb.append(" mCellConnectionStatus=").append(mCellConnectionStatus);

        return sb.toString();
    }

    /**
     * Implement the Parcelable interface
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    @Override
    public abstract void writeToParcel(Parcel dest, int flags);

    /**
     * Used by child classes for parceling.
     *
     * @hide
     */
    protected void writeToParcel(Parcel dest, int flags, int type) {
        dest.writeInt(type);
        dest.writeInt(mRegistered ? 1 : 0);
        dest.writeLong(mTimeStamp);
        dest.writeInt(mCellConnectionStatus);
    }

    /**
     * Used by child classes for parceling
     *
     * @hide
     */
    protected CellInfo(Parcel in) {
        mRegistered = (in.readInt() == 1) ? true : false;
        mTimeStamp = in.readLong();
        mCellConnectionStatus = in.readInt();
    }

    /** Implement the Parcelable interface */
    public static final @android.annotation.NonNull Creator<CellInfo> CREATOR = new Creator<CellInfo>() {
        @Override
        public CellInfo createFromParcel(Parcel in) {
                int type = in.readInt();
                switch (type) {
                    case TYPE_GSM: return CellInfoGsm.createFromParcelBody(in);
                    case TYPE_CDMA: return CellInfoCdma.createFromParcelBody(in);
                    case TYPE_LTE: return CellInfoLte.createFromParcelBody(in);
                    case TYPE_WCDMA: return CellInfoWcdma.createFromParcelBody(in);
                    case TYPE_TDSCDMA: return CellInfoTdscdma.createFromParcelBody(in);
                    case TYPE_NR: return CellInfoNr.createFromParcelBody(in);
                    default: throw new RuntimeException("Bad CellInfo Parcel");
                }
        }

        @Override
        public CellInfo[] newArray(int size) {
            return new CellInfo[size];
        }
    };
}
