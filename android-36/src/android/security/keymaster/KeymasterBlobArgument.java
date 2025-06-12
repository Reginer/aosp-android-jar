/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.security.keymaster;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Parcel;

/**
 * @hide
 */
class KeymasterBlobArgument extends KeymasterArgument {
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final byte[] blob;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public KeymasterBlobArgument(int tag, byte[] blob) {
        super(tag);
        switch (KeymasterDefs.getTagType(tag)) {
            case KeymasterDefs.KM_BIGNUM:
            case KeymasterDefs.KM_BYTES:
                break; // OK.
            default:
                throw new IllegalArgumentException("Bad blob tag " + tag);
        }
        this.blob = blob;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public KeymasterBlobArgument(int tag, Parcel in) {
        super(tag);
        blob = in.createByteArray();
    }

    @Override
    public void writeValue(Parcel out) {
        out.writeByteArray(blob);
    }
}
