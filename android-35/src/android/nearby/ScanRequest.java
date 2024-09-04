/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.nearby;

import android.Manifest;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.WorkSource;

import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * An encapsulation of various parameters for requesting nearby scans.
 *
 * @hide
 */
@SystemApi
public final class ScanRequest implements Parcelable {

    /** Scan type for scanning devices using fast pair protocol. */
    public static final int SCAN_TYPE_FAST_PAIR = 1;
    /** Scan type for scanning devices using nearby presence protocol. */
    public static final int SCAN_TYPE_NEARBY_PRESENCE = 2;

    /** Scan mode uses highest duty cycle. */
    public static final int SCAN_MODE_LOW_LATENCY = 2;
    /** Scan in balanced power mode.
     *  Scan results are returned at a rate that provides a good trade-off between scan
     *  frequency and power consumption.
     */
    public static final int SCAN_MODE_BALANCED = 1;
    /** Perform scan in low power mode. This is the default scan mode. */
    public static final int SCAN_MODE_LOW_POWER = 0;
    /**
     * A special scan mode. Applications using this scan mode will passively listen for other scan
     * results without starting BLE scans themselves.
     */
    public static final int SCAN_MODE_NO_POWER = -1;
    /**
     * A special scan mode to indicate that client only wants to use CHRE to scan.
     *
     * @hide
     */
    public static final int SCAN_MODE_CHRE_ONLY = 3;
    /**
     * Used to read a ScanRequest from a Parcel.
     */
    @NonNull
    public static final Creator<ScanRequest> CREATOR = new Creator<ScanRequest>() {
        @Override
        public ScanRequest createFromParcel(Parcel in) {
            ScanRequest.Builder builder = new ScanRequest.Builder()
                    .setScanType(in.readInt())
                    .setScanMode(in.readInt())
                    .setBleEnabled(in.readBoolean())
                    .setOffloadOnly(in.readBoolean())
                    .setWorkSource(in.readTypedObject(WorkSource.CREATOR));
            final int size = in.readInt();
            for (int i = 0; i < size; i++) {
                builder.addScanFilter(ScanFilter.createFromParcel(in));
            }
            return builder.build();
        }

        @Override
        public ScanRequest[] newArray(int size) {
            return new ScanRequest[size];
        }
    };

    private final @ScanType int mScanType;
    private final @ScanMode int mScanMode;
    private final boolean mBleEnabled;
    private final boolean mOffloadOnly;
    private final @NonNull WorkSource mWorkSource;
    private final List<ScanFilter> mScanFilters;

    private ScanRequest(@ScanType int scanType, @ScanMode int scanMode, boolean bleEnabled,
            boolean offloadOnly, @NonNull WorkSource workSource, List<ScanFilter> scanFilters) {
        mScanType = scanType;
        mScanMode = scanMode;
        mBleEnabled = bleEnabled;
        mOffloadOnly = offloadOnly;
        mWorkSource = workSource;
        mScanFilters = scanFilters;
    }

    /**
     * Convert scan mode to readable string.
     *
     * @param scanMode Integer that may represent a{@link ScanMode}.
     */
    @NonNull
    public static String scanModeToString(@ScanMode int scanMode) {
        switch (scanMode) {
            case SCAN_MODE_LOW_LATENCY:
                return "SCAN_MODE_LOW_LATENCY";
            case SCAN_MODE_BALANCED:
                return "SCAN_MODE_BALANCED";
            case SCAN_MODE_LOW_POWER:
                return "SCAN_MODE_LOW_POWER";
            case SCAN_MODE_NO_POWER:
                return "SCAN_MODE_NO_POWER";
            default:
                return "SCAN_MODE_INVALID";
        }
    }

    /**
     * Returns true if an integer is a defined scan type.
     */
    public static boolean isValidScanType(@ScanType int scanType) {
        return scanType == SCAN_TYPE_FAST_PAIR
                || scanType == SCAN_TYPE_NEARBY_PRESENCE;
    }

    /**
     * Returns true if an integer is a defined scan mode.
     */
    public static boolean isValidScanMode(@ScanMode int scanMode) {
        return scanMode == SCAN_MODE_LOW_LATENCY
                || scanMode == SCAN_MODE_BALANCED
                || scanMode == SCAN_MODE_LOW_POWER
                || scanMode == SCAN_MODE_NO_POWER;
    }

    /**
     * Returns the scan type for this request.
     */
    public @ScanType int getScanType() {
        return mScanType;
    }

    /**
     * Returns the scan mode for this request.
     */
    public @ScanMode int getScanMode() {
        return mScanMode;
    }

    /**
     * Returns if Bluetooth Low Energy enabled for scanning.
     */
    public boolean isBleEnabled() {
        return mBleEnabled;
    }

    /**
     * Returns if CHRE enabled for scanning.
     */
    public boolean isOffloadOnly() {
        return mOffloadOnly;
    }

    /**
     * Returns Scan Filters for this request.
     */
    @NonNull
    public List<ScanFilter> getScanFilters() {
        return mScanFilters;
    }

    /**
     * Returns the work source used for power attribution of this request.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    public WorkSource getWorkSource() {
        return mWorkSource;
    }

    /**
     * No special parcel contents.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns a string representation of this ScanRequest.
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Request[")
                .append("scanType=").append(mScanType);
        stringBuilder.append(", scanMode=").append(scanModeToString(mScanMode));
        // TODO(b/286137024): Remove this when CTS R5 is rolled out.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            stringBuilder.append(", bleEnabled=").append(mBleEnabled);
            stringBuilder.append(", offloadOnly=").append(mOffloadOnly);
        } else {
            stringBuilder.append(", enableBle=").append(mBleEnabled);
        }
        stringBuilder.append(", workSource=").append(mWorkSource);
        stringBuilder.append(", scanFilters=").append(mScanFilters);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mScanType);
        dest.writeInt(mScanMode);
        dest.writeBoolean(mBleEnabled);
        dest.writeBoolean(mOffloadOnly);
        dest.writeTypedObject(mWorkSource, /* parcelableFlags= */0);
        final int size = mScanFilters.size();
        dest.writeInt(size);
        for (int i = 0; i < size; i++) {
            mScanFilters.get(i).writeToParcel(dest, flags);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ScanRequest) {
            ScanRequest otherRequest = (ScanRequest) other;
            return mScanType == otherRequest.mScanType
                    && (mScanMode == otherRequest.mScanMode)
                    && (mBleEnabled == otherRequest.mBleEnabled)
                    && (mOffloadOnly == otherRequest.mOffloadOnly)
                    && (Objects.equals(mWorkSource, otherRequest.mWorkSource));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mScanType, mScanMode, mBleEnabled, mOffloadOnly, mWorkSource);
    }

    /** @hide **/
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SCAN_TYPE_FAST_PAIR, SCAN_TYPE_NEARBY_PRESENCE})
    public @interface ScanType {
    }

    /** @hide **/
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SCAN_MODE_LOW_LATENCY, SCAN_MODE_BALANCED,
            SCAN_MODE_LOW_POWER,
            SCAN_MODE_NO_POWER})
    public @interface ScanMode {}

    /** A builder class for {@link ScanRequest}. */
    public static final class Builder {
        private static final int INVALID_SCAN_TYPE = -1;
        private @ScanType int mScanType;
        private @ScanMode int mScanMode;

        private boolean mBleEnabled;
        private boolean mOffloadOnly;
        private WorkSource mWorkSource;
        private List<ScanFilter> mScanFilters;

        /** Creates a new Builder with the given scan type. */
        public Builder() {
            mScanType = INVALID_SCAN_TYPE;
            mBleEnabled = true;
            mOffloadOnly = false;
            mWorkSource = new WorkSource();
            mScanFilters = new ArrayList<>();
        }

        /**
         * Sets the scan type for the request. The scan type must be one of the SCAN_TYPE_ constants
         * in {@link ScanRequest}.
         *
         * @param scanType The scan type for the request
         */
        @NonNull
        public Builder setScanType(@ScanType int scanType) {
            mScanType = scanType;
            return this;
        }

        /**
         * Sets the scan mode for the request. The scan type must be one of the SCAN_MODE_ constants
         * in {@link ScanRequest}.
         *
         * @param scanMode The scan mode for the request
         */
        @NonNull
        public Builder setScanMode(@ScanMode int scanMode) {
            mScanMode = scanMode;
            return this;
        }

        /**
         * Sets if the ble is enabled for scanning.
         *
         * @param bleEnabled If the BluetoothLe is enabled in the device.
         */
        @NonNull
        public Builder setBleEnabled(boolean bleEnabled) {
            mBleEnabled = bleEnabled;
            return this;
        }

        /**
         * By default, a scan request can be served by either offload or
         * non-offload implementation, depending on the resource available in the device.
         *
         * A client can explicitly request a scan to be served by offload only.
         * Before the request, the client should query the offload capability by
         * using {@link NearbyManager#queryOffloadCapability(Executor, Consumer)}}. Otherwise,
         * {@link ScanCallback#ERROR_UNSUPPORTED} will be returned on devices without
         * offload capability.
         */
        @NonNull
        public Builder setOffloadOnly(boolean offloadOnly) {
            mOffloadOnly = offloadOnly;
            return this;
        }

        /**
         * Sets the work source to use for power attribution for this scan request. Defaults to
         * empty work source, which implies the caller that sends the scan request will be used
         * for power attribution.
         *
         * <p>Permission enforcement occurs when the resulting scan request is used, not when
         * this method is invoked.
         *
         * @param workSource identifying the application(s) for which to blame for the scan.
         * @hide
         */
        @RequiresPermission(Manifest.permission.UPDATE_DEVICE_STATS)
        @NonNull
        @SystemApi
        public Builder setWorkSource(@Nullable WorkSource workSource) {
            if (workSource == null) {
                mWorkSource = new WorkSource();
            } else {
                mWorkSource = workSource;
            }
            return this;
        }

        /**
         * Adds a scan filter to the request. Client can call this method multiple times to add
         * more than one scan filter. Scan results that match any of these scan filters will
         * be returned.
         *
         * <p>On devices with hardware support, scan filters can significantly improve the battery
         * usage of Nearby scans.
         *
         * @param scanFilter Filter for scanning the request.
         */
        @NonNull
        public Builder addScanFilter(@NonNull ScanFilter scanFilter) {
            Objects.requireNonNull(scanFilter);
            mScanFilters.add(scanFilter);
            return this;
        }

        /**
         * Builds a scan request from this builder.
         *
         * @return a new nearby scan request.
         * @throws IllegalStateException if the scanType is not one of the SCAN_TYPE_ constants in
         *                               {@link ScanRequest}.
         */
        @NonNull
        public ScanRequest build() {
            Preconditions.checkState(isValidScanType(mScanType),
                    "invalid scan type : " + mScanType
                            + ", scan type must be one of ScanRequest#SCAN_TYPE_");
            Preconditions.checkState(isValidScanMode(mScanMode),
                    "invalid scan mode : " + mScanMode
                            + ", scan mode must be one of ScanMode#SCAN_MODE_");
            return new ScanRequest(
                    mScanType, mScanMode, mBleEnabled, mOffloadOnly, mWorkSource, mScanFilters);
        }
    }
}
