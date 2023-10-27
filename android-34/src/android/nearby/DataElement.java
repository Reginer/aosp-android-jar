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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a data element in Nearby Presence.
 *
 * @hide
 */
@SystemApi
public final class DataElement implements Parcelable {

    private final int mKey;
    private final byte[] mValue;

    /** @hide */
    @IntDef({
            DataType.BLE_SERVICE_DATA,
            DataType.BLE_ADDRESS,
            DataType.SALT,
            DataType.PRIVATE_IDENTITY,
            DataType.TRUSTED_IDENTITY,
            DataType.PUBLIC_IDENTITY,
            DataType.PROVISIONED_IDENTITY,
            DataType.TX_POWER,
            DataType.ACTION,
            DataType.MODEL_ID,
            DataType.EDDYSTONE_EPHEMERAL_IDENTIFIER,
            DataType.ACCOUNT_KEY_DATA,
            DataType.CONNECTION_STATUS,
            DataType.BATTERY,
            DataType.SCAN_MODE,
            DataType.TEST_DE_BEGIN,
            DataType.TEST_DE_END
    })
    public @interface DataType {
        int BLE_SERVICE_DATA = 100;
        int BLE_ADDRESS = 101;
        // This is to indicate if the scan is offload only
        int SCAN_MODE = 102;
        int SALT = 0;
        int PRIVATE_IDENTITY = 1;
        int TRUSTED_IDENTITY = 2;
        int PUBLIC_IDENTITY = 3;
        int PROVISIONED_IDENTITY = 4;
        int TX_POWER = 5;
        int ACTION = 6;
        int MODEL_ID = 7;
        int EDDYSTONE_EPHEMERAL_IDENTIFIER = 8;
        int ACCOUNT_KEY_DATA = 9;
        int CONNECTION_STATUS = 10;
        int BATTERY = 11;
        // Reserves test DE ranges from {@link DataElement.DataType#TEST_DE_BEGIN}
        // to {@link DataElement.DataType#TEST_DE_END}, inclusive.
        // Reserves 128 Test DEs.
        int TEST_DE_BEGIN = Integer.MAX_VALUE - 127; // 2147483520
        int TEST_DE_END = Integer.MAX_VALUE; // 2147483647
    }

    /**
     * @hide
     */
    public static boolean isValidType(int type) {
        return type == DataType.BLE_SERVICE_DATA
                || type == DataType.ACCOUNT_KEY_DATA
                || type == DataType.BLE_ADDRESS
                || type == DataType.SCAN_MODE
                || type == DataType.SALT
                || type == DataType.PRIVATE_IDENTITY
                || type == DataType.TRUSTED_IDENTITY
                || type == DataType.PUBLIC_IDENTITY
                || type == DataType.PROVISIONED_IDENTITY
                || type == DataType.TX_POWER
                || type == DataType.ACTION
                || type == DataType.MODEL_ID
                || type == DataType.EDDYSTONE_EPHEMERAL_IDENTIFIER
                || type == DataType.CONNECTION_STATUS
                || type == DataType.BATTERY;
    }

    /**
     * @return {@code true} if this is identity type.
     * @hide
     */
    public boolean isIdentityDataType() {
        return mKey == DataType.PRIVATE_IDENTITY
                || mKey == DataType.TRUSTED_IDENTITY
                || mKey == DataType.PUBLIC_IDENTITY
                || mKey == DataType.PROVISIONED_IDENTITY;
    }

    /**
     * @return {@code true} if this is test data element type.
     * @hide
     */
    public static boolean isTestDeType(int type) {
        return type >= DataType.TEST_DE_BEGIN && type <= DataType.TEST_DE_END;
    }

    /**
     * Constructs a {@link DataElement}.
     */
    public DataElement(int key, @NonNull byte[] value) {
        Preconditions.checkArgument(value != null, "value cannot be null");
        mKey = key;
        mValue = value;
    }

    @NonNull
    public static final Creator<DataElement> CREATOR = new Creator<DataElement>() {
        @Override
        public DataElement createFromParcel(Parcel in) {
            int key = in.readInt();
            byte[] value = new byte[in.readInt()];
            in.readByteArray(value);
            return new DataElement(key, value);
        }

        @Override
        public DataElement[] newArray(int size) {
            return new DataElement[size];
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof DataElement) {
            return mKey == ((DataElement) obj).mKey
                    && Arrays.equals(mValue, ((DataElement) obj).mValue);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mKey, Arrays.hashCode(mValue));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mKey);
        dest.writeInt(mValue.length);
        dest.writeByteArray(mValue);
    }

    /**
     * Returns the key of the data element, as defined in the nearby presence specification.
     */
    public int getKey() {
        return mKey;
    }

    /**
     * Returns the value of the data element.
     */
    @NonNull
    public byte[] getValue() {
        return mValue;
    }
}
