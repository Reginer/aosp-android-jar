/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;

/**
 * Filter for scanning a nearby device.
 *
 * @hide
 */
@SystemApi
public abstract class ScanFilter {
    /**
     * Creates a {@link ScanFilter} from the parcel.
     *
     * @hide
     */
    public static ScanFilter createFromParcel(Parcel in) {
        int type = in.readInt();
        switch (type) {
            // Currently, only Nearby Presence filtering is supported, in the future
            // filtering other nearby specifications will be added.
            case ScanRequest.SCAN_TYPE_NEARBY_PRESENCE:
                return PresenceScanFilter.createFromParcelBody(in);
            default:
                throw new IllegalStateException(
                        "Unexpected scan type (value " + type + ") in parcel.");
        }
    }

    private final @ScanRequest.ScanType int mType;
    private final int mMaxPathLoss;

    /**
     * Constructs a Scan Filter.
     *
     * @hide
     */
    ScanFilter(@ScanRequest.ScanType int type, @IntRange(from = 0, to = 127) int maxPathLoss) {
        mType = type;
        mMaxPathLoss = maxPathLoss;
    }

    /**
     * Constructs a Scan Filter.
     *
     * @hide
     */
    ScanFilter(@ScanRequest.ScanType int type, Parcel in) {
        mType = type;
        mMaxPathLoss = in.readInt();
    }

    /**
     * Returns the type of this scan filter.
     */
    public @ScanRequest.ScanType int getType() {
        return mType;
    }

    /**
     * Returns the maximum path loss (in dBm) of the received scan result. The path loss is the
     * attenuation of radio energy between sender and receiver. Path loss here is defined as
     * (TxPower - Rssi).
     */
    @IntRange(from = 0, to = 127)
    public int getMaxPathLoss() {
        return mMaxPathLoss;
    }

    /**
     *
     * Writes the scan filter to the parcel.
     *
     * @hide
     */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeInt(mMaxPathLoss);
    }
}
