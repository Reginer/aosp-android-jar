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

package android.devicelock;

import android.annotation.IntDef;
import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The response returned from {@link DeviceLockManager#getDeviceId} on success.
 * A DeviceId represents a stable identifier (i.e. an identifier that is preserved after a factory
 * reset). At this moment, the only supported identifiers are IMEI and MEID.
 */
public final class DeviceId {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "DEVICE_ID_TYPE_", value = {
        DEVICE_ID_TYPE_IMEI,
        DEVICE_ID_TYPE_MEID,
    })
    public @interface DeviceIdType {}
    /** The device id is an IMEI */
    public static final int DEVICE_ID_TYPE_IMEI = 0;
    /** The device id is a MEID */
    public static final int DEVICE_ID_TYPE_MEID = 1;

    private final @DeviceIdType int mType;
    private final String mId;

    DeviceId(int type, @NonNull String id) {
        mType = type;
        mId = id;
    }

    public @DeviceIdType int getType() {
        return mType;
    }

    public @NonNull String getId() {
        return mId;
    }
}
