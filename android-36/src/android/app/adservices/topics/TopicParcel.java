/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.app.adservices.topics;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents a Topic.
 *
 * @hide
 */
public final class TopicParcel implements Parcelable {
    private final long mTaxonomyVersion;
    private final long mModelVersion;
    private final int mTopicId;

    private TopicParcel(@NonNull Builder builder) {
        mTaxonomyVersion = builder.mTaxonomyVersion;
        mModelVersion = builder.mModelVersion;
        mTopicId = builder.mTopicId;
    }

    private TopicParcel(@NonNull Parcel in) {
        mTaxonomyVersion = in.readLong();
        mModelVersion = in.readLong();
        mTopicId = in.readInt();
    }

    @NonNull
    public static final Creator<TopicParcel> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public TopicParcel createFromParcel(Parcel in) {
                    return new TopicParcel(in);
                }

                @Override
                public TopicParcel[] newArray(int size) {
                    return new TopicParcel[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeLong(mTaxonomyVersion);
        out.writeLong(mModelVersion);
        out.writeInt(mTopicId);
    }

    /** Get the taxonomy version. */
    public long getTaxonomyVersion() {
        return mTaxonomyVersion;
    }

    /** Get the model Version. */
    public long getModelVersion() {
        return mModelVersion;
    }

    /** Get the Topic ID. */
    public int getTopicId() {
        return mTopicId;
    }

    /** Builder for {@link TopicParcel} objects. */
    public static final class Builder {
        private long mTaxonomyVersion;
        private long mModelVersion;
        private int mTopicId;

        public Builder() {}

        /** Set the taxonomy version */
        @NonNull
        public TopicParcel.Builder setTaxonomyVersion(long taxonomyVersion) {
            mTaxonomyVersion = taxonomyVersion;
            return this;
        }

        /** Set the model version */
        @NonNull
        public TopicParcel.Builder setModelVersion(long modelVersion) {
            mModelVersion = modelVersion;
            return this;
        }

        /** Set the topic id */
        @NonNull
        public TopicParcel.Builder setTopicId(int topicId) {
            mTopicId = topicId;
            return this;
        }

        /** Builds a {@link TopicParcel} instance. */
        @NonNull
        public TopicParcel build() {
            return new TopicParcel(this);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaxonomyVersion(), getModelVersion(), getTopicId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TopicParcel)) {
            return false;
        }

        TopicParcel topicParcel = (TopicParcel) obj;
        return this.getTaxonomyVersion() == topicParcel.getTaxonomyVersion()
                && this.getModelVersion() == topicParcel.getModelVersion()
                && this.getTopicId() == topicParcel.getTopicId();
    }
}
