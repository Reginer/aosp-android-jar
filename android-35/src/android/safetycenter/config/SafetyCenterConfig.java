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

package android.safetycenter.config;

import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Data class used to represent the initial configuration of the Safety Center.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyCenterConfig implements Parcelable {

    @NonNull
    public static final Creator<SafetyCenterConfig> CREATOR =
            new Creator<SafetyCenterConfig>() {
                @Override
                public SafetyCenterConfig createFromParcel(Parcel in) {
                    List<SafetySourcesGroup> safetySourcesGroups =
                            requireNonNull(in.createTypedArrayList(SafetySourcesGroup.CREATOR));
                    Builder builder = new Builder();
                    for (int i = 0; i < safetySourcesGroups.size(); i++) {
                        builder.addSafetySourcesGroup(safetySourcesGroups.get(i));
                    }
                    return builder.build();
                }

                @Override
                public SafetyCenterConfig[] newArray(int size) {
                    return new SafetyCenterConfig[size];
                }
            };

    @NonNull private final List<SafetySourcesGroup> mSafetySourcesGroups;

    private SafetyCenterConfig(@NonNull List<SafetySourcesGroup> safetySourcesGroups) {
        mSafetySourcesGroups = safetySourcesGroups;
    }

    /**
     * Returns the list of {@link SafetySourcesGroup}s in the Safety Center configuration.
     *
     * <p>A Safety Center configuration contains at least one {@link SafetySourcesGroup}.
     */
    @NonNull
    public List<SafetySourcesGroup> getSafetySourcesGroups() {
        return mSafetySourcesGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyCenterConfig)) return false;
        SafetyCenterConfig that = (SafetyCenterConfig) o;
        return Objects.equals(mSafetySourcesGroups, that.mSafetySourcesGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSafetySourcesGroups);
    }

    @Override
    public String toString() {
        return "SafetyCenterConfig{" + "mSafetySourcesGroups=" + mSafetySourcesGroups + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(mSafetySourcesGroups);
    }

    /** Builder class for {@link SafetyCenterConfig}. */
    public static final class Builder {

        private final List<SafetySourcesGroup> mSafetySourcesGroups = new ArrayList<>();

        /** Creates a {@link Builder} for a {@link SafetyCenterConfig}. */
        public Builder() {}

        /** Creates a {@link Builder} with the values from the given {@link SafetyCenterConfig}. */
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder(@NonNull SafetyCenterConfig safetyCenterConfig) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException(
                        "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
            }
            requireNonNull(safetyCenterConfig);
            mSafetySourcesGroups.addAll(safetyCenterConfig.mSafetySourcesGroups);
        }

        /**
         * Adds a {@link SafetySourcesGroup} to the Safety Center configuration.
         *
         * <p>A Safety Center configuration must contain at least one {@link SafetySourcesGroup}.
         */
        @NonNull
        public Builder addSafetySourcesGroup(@NonNull SafetySourcesGroup safetySourcesGroup) {
            mSafetySourcesGroups.add(requireNonNull(safetySourcesGroup));
            return this;
        }

        /**
         * Creates the {@link SafetyCenterConfig} defined by this {@link Builder}.
         *
         * @throws IllegalStateException if any constraint on the Safety Center configuration is
         *     violated
         */
        @NonNull
        public SafetyCenterConfig build() {
            List<SafetySourcesGroup> safetySourcesGroups =
                    unmodifiableList(new ArrayList<>(mSafetySourcesGroups));
            if (safetySourcesGroups.isEmpty()) {
                throw new IllegalStateException("No safety sources groups present");
            }
            Set<String> safetySourceIds = new HashSet<>();
            Set<String> safetySourcesGroupsIds = new HashSet<>();
            int safetySourcesGroupsSize = safetySourcesGroups.size();
            for (int i = 0; i < safetySourcesGroupsSize; i++) {
                SafetySourcesGroup safetySourcesGroup = safetySourcesGroups.get(i);
                String groupId = safetySourcesGroup.getId();
                if (safetySourcesGroupsIds.contains(groupId)) {
                    throw new IllegalStateException(
                            "Duplicate id " + groupId + " among safety sources groups");
                }
                safetySourcesGroupsIds.add(groupId);
                List<SafetySource> safetySources = safetySourcesGroup.getSafetySources();
                int safetySourcesSize = safetySources.size();
                for (int j = 0; j < safetySourcesSize; j++) {
                    SafetySource staticSafetySource = safetySources.get(j);
                    String sourceId = staticSafetySource.getId();
                    if (safetySourceIds.contains(sourceId)) {
                        throw new IllegalStateException(
                                "Duplicate id " + sourceId + " among safety sources");
                    }
                    safetySourceIds.add(sourceId);
                }
            }
            return new SafetyCenterConfig(safetySourcesGroups);
        }
    }
}
