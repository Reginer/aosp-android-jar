/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.ranging;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.SuppressLint;
import android.os.Parcelable;
import android.ranging.oob.OobInitiatorRangingConfig;
import android.ranging.oob.OobResponderRangingConfig;
import android.ranging.raw.RawInitiatorRangingConfig;
import android.ranging.raw.RawResponderRangingConfig;

import com.android.ranging.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Abstract class to represent type of ranging configuration.
 *
 * <p>Subclasses include:</p>
 * <ul>
 *     <li>{@link RawResponderRangingConfig}</li>
 *     <li>{@link RawInitiatorRangingConfig}</li>
 *     <li>{@link OobResponderRangingConfig}</li>
 *     <li>{@link OobInitiatorRangingConfig}</li>
 * </ul>
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
@SuppressLint({"ParcelCreator", "ParcelNotFinal"})
public abstract class RangingConfig implements Parcelable {
    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            RANGING_SESSION_RAW,
            RANGING_SESSION_OOB,
    })
    public @interface RangingSessionType {
    }

    protected RangingConfig() { }

    /** Ranging session with the out-of-band negotiations performed by the app. */
    public static final int RANGING_SESSION_RAW = 0;
    /** Ranging session with the out-of-band negotiations performed by the ranging API. */
    public static final int RANGING_SESSION_OOB = 1;

    @RangingSessionType
    private int mRangingSessionType;

    /**
     * @hide
     */
    protected void setRangingSessionType(@RangingSessionType int rangingSessionType) {
        mRangingSessionType = rangingSessionType;
    }

    /**
     * Gets the ranging session type {@link RangingSessionType}
     *
     * @return the type of ranging session.
     */
    public int getRangingSessionType() {
        return mRangingSessionType;
    }

    @Override
    public String toString() {
        return "RangingParams{ "
                + "mRangingSessionType="
                + mRangingSessionType
                + " }";
    }
}
