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

import static android.adservices.common.AdServicesCommonManager.MODULE_STATE_UNKNOWN;
import static android.adservices.common.AdServicesCommonManager.Module;
import static android.adservices.common.AdServicesCommonManager.ModuleState;
import static android.adservices.common.AdServicesCommonManager.NotificationType;
import static android.adservices.common.AdServicesCommonManager.validateModule;
import static android.adservices.common.AdServicesCommonManager.validateModuleState;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseIntArray;

import com.android.adservices.flags.Flags;

import java.util.Objects;

/**
 * The request sent from from system applications to control the module states with Adservices.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
public final class UpdateAdServicesModuleStatesParams implements Parcelable {
    private final SparseIntArray mAdServicesModuleStates;
    @NotificationType private final int mNotificationType;

    private UpdateAdServicesModuleStatesParams(
            SparseIntArray adServicesModuleStates, @NotificationType int notificationType) {
        mAdServicesModuleStates = Objects.requireNonNull(adServicesModuleStates);
        mNotificationType = notificationType;
    }

    private UpdateAdServicesModuleStatesParams(Parcel in) {
        int moduleStatesSize = in.readInt();
        mAdServicesModuleStates = new SparseIntArray(moduleStatesSize);
        for (int i = 0; i < moduleStatesSize; i++) {
            mAdServicesModuleStates.put(in.readInt(), in.readInt());
        }
        mNotificationType = in.readInt();
    }

    @NonNull
    public static final Creator<UpdateAdServicesModuleStatesParams> CREATOR =
            new Creator<>() {
                @Override
                public UpdateAdServicesModuleStatesParams createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new UpdateAdServicesModuleStatesParams(in);
                }

                @Override
                public UpdateAdServicesModuleStatesParams[] newArray(int size) {
                    return new UpdateAdServicesModuleStatesParams[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Objects.requireNonNull(out);

        int size = mAdServicesModuleStates.size();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeInt(mAdServicesModuleStates.keyAt(i));
            out.writeInt(mAdServicesModuleStates.valueAt(i));
        }
        out.writeInt(mNotificationType);
    }

    /** Gets the last set AdServices module state value for a module. */
    @ModuleState
    public int getModuleState(@Module int module) {
        return mAdServicesModuleStates.get(module, MODULE_STATE_UNKNOWN);
    }

    /**
     * Returns the Adservices module state map associated with this result. Keys represent modules
     * and values represent module states.
     *
     * @hide
     */
    public SparseIntArray getModuleStates() {
        return mAdServicesModuleStates;
    }

    /** Returns the Notification type associated with this result. */
    @NotificationType
    public int getNotificationType() {
        return mNotificationType;
    }

    @Override
    public String toString() {
        return "UpdateAdIdRequest{"
                + "mAdServicesModuleStates="
                + mAdServicesModuleStates
                + ", mNotificationType="
                + mNotificationType
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UpdateAdServicesModuleStatesParams that)) {
            return false;
        }

        return (mAdServicesModuleStates == that.mAdServicesModuleStates)
                && (mNotificationType == that.mNotificationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdServicesModuleStates, mNotificationType);
    }

    /** Builder for {@link UpdateAdServicesModuleStatesParams} objects. */
    public static final class Builder {
        private final SparseIntArray mAdServicesModuleStateList = new SparseIntArray();
        @NotificationType private int mNotificationType;

        public Builder() {}

        /**
         * Sets the AdServices module state for one module. Will override previous set value if the
         * given module was set before.
         */
        @NonNull
        public Builder setModuleState(@Module int module, @ModuleState int moduleState) {
            this.mAdServicesModuleStateList.put(
                    validateModule(module), validateModuleState(moduleState));
            return this;
        }

        /** Sets the notification type. */
        @NonNull
        public Builder setNotificationType(@NotificationType int notificationType) {
            this.mNotificationType = notificationType;
            return this;
        }

        /** Builds a {@link UpdateAdServicesModuleStatesParams} instance. */
        @NonNull
        public UpdateAdServicesModuleStatesParams build() {
            return new UpdateAdServicesModuleStatesParams(
                    mAdServicesModuleStateList, this.mNotificationType);
        }
    }
}
