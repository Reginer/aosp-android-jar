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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringRes;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data class used to represent the initial configuration of a group of safety sources.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetySourcesGroup implements Parcelable {

    /**
     * Indicates that the safety sources group should be displayed as a collapsible group with an
     * icon (stateless or stateful) and an optional default summary.
     *
     * @deprecated use {@link #SAFETY_SOURCES_GROUP_TYPE_STATEFUL} instead.
     */
    public static final int SAFETY_SOURCES_GROUP_TYPE_COLLAPSIBLE = 0;

    /**
     * Indicates that the safety sources group should be displayed as a group that may contribute to
     * the overall Safety Center status. This is indicated by a group stateful icon. If all sources
     * in the group have an unspecified status then a stateless group icon might be applied.
     */
    public static final int SAFETY_SOURCES_GROUP_TYPE_STATEFUL =
            SAFETY_SOURCES_GROUP_TYPE_COLLAPSIBLE;

    /**
     * Indicates that the safety sources group should be displayed as a rigid group with no icon and
     * no summary.
     *
     * @deprecated use {@link #SAFETY_SOURCES_GROUP_TYPE_STATELESS} instead.
     */
    public static final int SAFETY_SOURCES_GROUP_TYPE_RIGID = 1;

    /**
     * Indicates that the safety sources group should be displayed as a group that does not
     * contribute to the overall Safety Center status. All sources of type dynamic in the group can
     * only report an unspecified status. The stateless icon and summary may be ignored and not be
     * displayed.
     */
    public static final int SAFETY_SOURCES_GROUP_TYPE_STATELESS = SAFETY_SOURCES_GROUP_TYPE_RIGID;

    /**
     * Indicates that the safety sources group should not be displayed. All sources in the group
     * must be of type issue-only.
     */
    public static final int SAFETY_SOURCES_GROUP_TYPE_HIDDEN = 2;

    /**
     * All possible types for a safety sources group.
     *
     * @hide
     */
    @SuppressLint("UniqueConstants") // Intentionally renaming the COLLAPSIBLE and RIGID constants.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "SAFETY_SOURCES_GROUP_TYPE_",
            value = {
                SAFETY_SOURCES_GROUP_TYPE_COLLAPSIBLE,
                SAFETY_SOURCES_GROUP_TYPE_STATEFUL,
                SAFETY_SOURCES_GROUP_TYPE_RIGID,
                SAFETY_SOURCES_GROUP_TYPE_STATELESS,
                SAFETY_SOURCES_GROUP_TYPE_HIDDEN
            })
    public @interface SafetySourceGroupType {}

    /**
     * Indicates that no special icon will be displayed by a safety sources group when all the
     * sources contained in it are stateless.
     */
    public static final int STATELESS_ICON_TYPE_NONE = 0;

    /**
     * Indicates that the privacy icon will be displayed by a safety sources group when all the
     * sources contained in it are stateless.
     */
    public static final int STATELESS_ICON_TYPE_PRIVACY = 1;

    /**
     * All possible stateless icon types for a safety sources group.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "STATELESS_ICON_TYPE_",
            value = {STATELESS_ICON_TYPE_NONE, STATELESS_ICON_TYPE_PRIVACY})
    public @interface StatelessIconType {}

    @NonNull
    public static final Creator<SafetySourcesGroup> CREATOR =
            new Creator<SafetySourcesGroup>() {
                @Override
                public SafetySourcesGroup createFromParcel(Parcel in) {
                    Builder builder =
                            new Builder()
                                    .setId(in.readString())
                                    .setTitleResId(in.readInt())
                                    .setSummaryResId(in.readInt())
                                    .setStatelessIconType(in.readInt());
                    List<SafetySource> safetySources =
                            requireNonNull(in.createTypedArrayList(SafetySource.CREATOR));
                    for (int i = 0; i < safetySources.size(); i++) {
                        builder.addSafetySource(safetySources.get(i));
                    }
                    if (SdkLevel.isAtLeastU()) {
                        builder.setType(in.readInt());
                    }
                    return builder.build();
                }

                @Override
                public SafetySourcesGroup[] newArray(int size) {
                    return new SafetySourcesGroup[size];
                }
            };

    @SafetySourceGroupType private final int mType;
    @NonNull private final String mId;
    @StringRes private final int mTitleResId;
    @StringRes private final int mSummaryResId;
    @StatelessIconType private final int mStatelessIconType;
    @NonNull private final List<SafetySource> mSafetySources;

    private SafetySourcesGroup(
            @SafetySourceGroupType int type,
            @NonNull String id,
            @StringRes int titleResId,
            @StringRes int summaryResId,
            @StatelessIconType int statelessIconType,
            @NonNull List<SafetySource> safetySources) {
        mType = type;
        mId = id;
        mTitleResId = titleResId;
        mSummaryResId = summaryResId;
        mStatelessIconType = statelessIconType;
        mSafetySources = safetySources;
    }

    /** Returns the type of this safety sources group. */
    @SafetySourceGroupType
    public int getType() {
        return mType;
    }

    /**
     * Returns the id of this safety sources group.
     *
     * <p>The id is unique among safety sources groups in a Safety Center configuration.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the resource id of the title of this safety sources group.
     *
     * <p>The id refers to a string resource that is either accessible from any resource context or
     * that is accessible from the same resource context that was used to load the Safety Center
     * configuration. The id is {@link Resources#ID_NULL} when a title is not provided.
     */
    @StringRes
    public int getTitleResId() {
        return mTitleResId;
    }

    /**
     * Returns the resource id of the summary of this safety sources group.
     *
     * <p>The id refers to a string resource that is either accessible from any resource context or
     * that is accessible from the same resource context that was used to load the Safety Center
     * configuration. The id is {@link Resources#ID_NULL} when a summary is not provided.
     */
    @StringRes
    public int getSummaryResId() {
        return mSummaryResId;
    }

    /**
     * Returns the stateless icon type of this safety sources group.
     *
     * <p>If set to a value other than {@link SafetySourcesGroup#STATELESS_ICON_TYPE_NONE}, the icon
     * specified will be displayed for collapsible groups when all the sources contained in the
     * group are stateless.
     */
    @StatelessIconType
    public int getStatelessIconType() {
        return mStatelessIconType;
    }

    /**
     * Returns the list of {@link SafetySource}s in this safety sources group.
     *
     * <p>A safety sources group contains at least one {@link SafetySource}.
     */
    @NonNull
    public List<SafetySource> getSafetySources() {
        return mSafetySources;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetySourcesGroup)) return false;
        SafetySourcesGroup that = (SafetySourcesGroup) o;
        return mType == that.mType
                && Objects.equals(mId, that.mId)
                && mTitleResId == that.mTitleResId
                && mSummaryResId == that.mSummaryResId
                && mStatelessIconType == that.mStatelessIconType
                && Objects.equals(mSafetySources, that.mSafetySources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mType, mId, mTitleResId, mSummaryResId, mStatelessIconType, mSafetySources);
    }

    @Override
    public String toString() {
        return "SafetySourcesGroup{"
                + "mType="
                + mType
                + ", mId="
                + mId
                + ", mTitleResId="
                + mTitleResId
                + ", mSummaryResId="
                + mSummaryResId
                + ", mStatelessIconType="
                + mStatelessIconType
                + ", mSafetySources="
                + mSafetySources
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeInt(mTitleResId);
        dest.writeInt(mSummaryResId);
        dest.writeInt(mStatelessIconType);
        dest.writeTypedList(mSafetySources);
        if (SdkLevel.isAtLeastU()) {
            dest.writeInt(mType);
        }
    }

    /** Builder class for {@link SafetySourcesGroup}. */
    public static final class Builder {

        private final List<SafetySource> mSafetySources = new ArrayList<>();

        @Nullable @SafetySourceGroupType private Integer mType;
        @Nullable private String mId;
        @Nullable @StringRes private Integer mTitleResId;
        @Nullable @StringRes private Integer mSummaryResId;
        @Nullable @StatelessIconType private Integer mStatelessIconType;

        /** Creates a {@link Builder} for a {@link SafetySourcesGroup}. */
        public Builder() {}

        /** Creates a {@link Builder} with the values from the given {@link SafetySourcesGroup}. */
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder(@NonNull SafetySourcesGroup original) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException(
                        "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
            }
            requireNonNull(original);
            mSafetySources.addAll(original.mSafetySources);
            mType = original.mType;
            mId = original.mId;
            mTitleResId = original.mTitleResId;
            mSummaryResId = original.mSummaryResId;
            mStatelessIconType = original.mStatelessIconType;
        }

        /**
         * Sets the type of this safety sources group.
         *
         * <p>If the type is not explicitly set, the type is inferred according to the state of
         * certain fields. If no title is provided when building the group, the group is of type
         * hidden. If a title is provided but no summary or stateless icon are provided when
         * building the group, the group is of type stateless. Otherwise, the group is of type
         * stateful.
         */
        @NonNull
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder setType(@SafetySourceGroupType int type) {
            mType = type;
            return this;
        }

        /**
         * Sets the id of this safety sources group.
         *
         * <p>The id must be unique among safety sources groups in a Safety Center configuration.
         */
        @NonNull
        public Builder setId(@Nullable String id) {
            mId = id;
            return this;
        }

        /**
         * Sets the resource id of the title of this safety sources group.
         *
         * <p>The id must refer to a string resource that is either accessible from any resource
         * context or that is accessible from the same resource context that was used to load the
         * Safety Center configuration. The id defaults to {@link Resources#ID_NULL} when a title is
         * not provided. A title is required unless the group only contains safety sources of type
         * issue only.
         */
        @NonNull
        public Builder setTitleResId(@StringRes int titleResId) {
            mTitleResId = titleResId;
            return this;
        }

        /**
         * Sets the resource id of the summary of this safety sources group.
         *
         * <p>The id must refer to a string resource that is either accessible from any resource
         * context or that is accessible from the same resource context that was used to load the
         * Safety Center configuration. The id defaults to {@link Resources#ID_NULL} when a summary
         * is not provided.
         */
        @NonNull
        public Builder setSummaryResId(@StringRes int summaryResId) {
            mSummaryResId = summaryResId;
            return this;
        }

        /**
         * Sets the stateless icon type of this safety sources group.
         *
         * <p>If set to a value other than {@link SafetySourcesGroup#STATELESS_ICON_TYPE_NONE}, the
         * icon specified will be displayed for collapsible groups when all the sources contained in
         * the group are stateless.
         */
        @NonNull
        public Builder setStatelessIconType(@StatelessIconType int statelessIconType) {
            mStatelessIconType = statelessIconType;
            return this;
        }

        /**
         * Adds a {@link SafetySource} to this safety sources group.
         *
         * <p>A safety sources group must contain at least one {@link SafetySource}.
         */
        @NonNull
        public Builder addSafetySource(@NonNull SafetySource safetySource) {
            mSafetySources.add(requireNonNull(safetySource));
            return this;
        }

        /**
         * Creates the {@link SafetySourcesGroup} defined by this {@link Builder}.
         *
         * @throws IllegalStateException if any constraint on the safety sources group is violated
         */
        @NonNull
        public SafetySourcesGroup build() {
            String id = mId;
            BuilderUtils.validateId(id, "id", true, false);

            List<SafetySource> safetySources = unmodifiableList(new ArrayList<>(mSafetySources));
            if (safetySources.isEmpty()) {
                throw new IllegalStateException("Safety sources group empty");
            }

            int summaryResId = BuilderUtils.validateResId(mSummaryResId, "summary", false, false);

            int statelessIconType =
                    BuilderUtils.validateIntDef(
                            mStatelessIconType,
                            "statelessIconType",
                            false,
                            false,
                            STATELESS_ICON_TYPE_NONE,
                            STATELESS_ICON_TYPE_NONE,
                            STATELESS_ICON_TYPE_PRIVACY);

            boolean hasOnlyIssueOnlySources = true;
            int safetySourcesSize = safetySources.size();
            for (int i = 0; i < safetySourcesSize; i++) {
                int type = safetySources.get(i).getType();
                if (type != SafetySource.SAFETY_SOURCE_TYPE_ISSUE_ONLY) {
                    hasOnlyIssueOnlySources = false;
                    break;
                }
            }

            int inferredGroupType = SAFETY_SOURCES_GROUP_TYPE_STATELESS;
            if (hasOnlyIssueOnlySources) {
                inferredGroupType = SAFETY_SOURCES_GROUP_TYPE_HIDDEN;
            } else if (summaryResId != Resources.ID_NULL
                    || statelessIconType != STATELESS_ICON_TYPE_NONE) {
                inferredGroupType = SAFETY_SOURCES_GROUP_TYPE_STATEFUL;
            }
            int type =
                    BuilderUtils.validateIntDef(
                            mType,
                            "type",
                            false,
                            false,
                            inferredGroupType,
                            SAFETY_SOURCES_GROUP_TYPE_STATEFUL,
                            SAFETY_SOURCES_GROUP_TYPE_STATELESS,
                            SAFETY_SOURCES_GROUP_TYPE_HIDDEN);
            if (type == SAFETY_SOURCES_GROUP_TYPE_HIDDEN && !hasOnlyIssueOnlySources) {
                throw new IllegalStateException(
                        "Safety sources groups of type hidden can only contain sources of type "
                                + "issue-only");
            }
            if (type != SAFETY_SOURCES_GROUP_TYPE_HIDDEN && hasOnlyIssueOnlySources) {
                throw new IllegalStateException(
                        "Safety sources groups containing only sources of type issue-only must be "
                                + "of type hidden");
            }

            boolean isStateful = type == SAFETY_SOURCES_GROUP_TYPE_STATEFUL;
            boolean isStateless = type == SAFETY_SOURCES_GROUP_TYPE_STATELESS;
            int titleResId =
                    BuilderUtils.validateResId(
                            mTitleResId, "title", isStateful || isStateless, false);

            return new SafetySourcesGroup(
                    type, id, titleResId, summaryResId, statelessIconType, safetySources);
        }
    }
}
