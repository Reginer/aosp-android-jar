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

package android.adservices.common;

import static android.adservices.common.AdServicesModuleUserChoice.ModuleUserChoiceCode;
import static android.adservices.common.AdServicesModuleUserChoice.USER_CHOICE_UNKNOWN;
import static android.adservices.common.Module.ModuleCode;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseIntArray;

import com.android.adservices.flags.Flags;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The request sent from system applications to control the module user choices with AdServices.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
public final class UpdateAdServicesUserChoicesParams implements Parcelable {
    private final SparseIntArray mAdServicesUserChoiceList;

    private UpdateAdServicesUserChoicesParams(SparseIntArray adServicesUserChoiceList) {
        mAdServicesUserChoiceList = adServicesUserChoiceList;
    }

    private UpdateAdServicesUserChoicesParams(Parcel in) {
        int length = in.readInt();
        int[] intKeys = new int[length];
        int[] intValues = new int[length];
        in.readIntArray(intKeys);
        in.readIntArray(intValues);
        mAdServicesUserChoiceList = new SparseIntArray(intKeys.length);
        for (int i = 0; i < intKeys.length; i++) {
            mAdServicesUserChoiceList.append(intKeys[i], intValues[i]);
        }
    }

    @NonNull
    public static final Creator<UpdateAdServicesUserChoicesParams> CREATOR =
            new Creator<>() {
                @Override
                public UpdateAdServicesUserChoicesParams createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new UpdateAdServicesUserChoicesParams(in);
                }

                @Override
                public UpdateAdServicesUserChoicesParams[] newArray(int size) {
                    return new UpdateAdServicesUserChoicesParams[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        int length = mAdServicesUserChoiceList.size();
        int[] keys = new int[length];
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            keys[i] = mAdServicesUserChoiceList.keyAt(i);
            values[i] = mAdServicesUserChoiceList.valueAt(i);
        }

        out.writeInt(keys.length);
        out.writeIntArray(keys);
        out.writeIntArray(values);
    }

    /** Gets the last set AdServices user choice value for a module. */
    @ModuleUserChoiceCode
    public int getUserChoice(@ModuleCode int module) {
        return mAdServicesUserChoiceList.get(module, USER_CHOICE_UNKNOWN);
    }

    /**
     * Returns the Adservices user choice map associated with this result.
     *
     * <p>Keys represent modules and values represent module states.
     *
     * @hide
     */
    public Map<Integer, Integer> getUserChoiceMap() {
        int length = mAdServicesUserChoiceList.size();
        Map<Integer, Integer> userChoicemap = new HashMap<>(length);
        for (int i = 0; i < length; i++) {
            userChoicemap.put(
                    mAdServicesUserChoiceList.keyAt(i), mAdServicesUserChoiceList.valueAt(i));
        }
        return userChoicemap;
    }

    @Override
    public String toString() {
        return "UpdateAdIdRequest{"
                + "mAdServicesUserChoiceList="
                + mAdServicesUserChoiceList
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UpdateAdServicesUserChoicesParams that)) {
            return false;
        }

        return Objects.equals(mAdServicesUserChoiceList, that.mAdServicesUserChoiceList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdServicesUserChoiceList);
    }

    /** Builder for {@link UpdateAdServicesUserChoicesParams} objects. */
    public static final class Builder {
        private final SparseIntArray mAdServicesUserChoiceList = new SparseIntArray();

        public Builder() {}

        /**
         * Sets the AdServices user choice for one module. Will override previous set value if the
         * given module was set before.
         */
        @NonNull
        public Builder setUserChoice(@ModuleCode int module, @ModuleUserChoiceCode int userChoice) {
            this.mAdServicesUserChoiceList.put(module, userChoice);
            return this;
        }

        /** Builds a {@link UpdateAdServicesUserChoicesParams} instance. */
        @NonNull
        public UpdateAdServicesUserChoicesParams build() {
            return new UpdateAdServicesUserChoicesParams(mAdServicesUserChoiceList);
        }
    }
}
