/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.app.contentsuggestions;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Request object used when asking {@link ContentSuggestionsManager} to classify content selections.
 *
 * <p>The request contains a list of {@link ContentSelection} objects to be classified along with
 * implementation specific extras.
 *
 * @hide
 */
@SystemApi
public final class ClassificationsRequest implements Parcelable {
    @NonNull
    private final List<ContentSelection> mSelections;
    @Nullable
    private final Bundle mExtras;

    private ClassificationsRequest(@NonNull List<ContentSelection> selections,
            @Nullable Bundle extras) {
        mSelections = selections;
        mExtras = extras;
    }

    /**
     * Return request selections.
     */
    public @NonNull List<ContentSelection> getSelections() {
        return mSelections;
    }

    /**
     * Return the request extras, can be an empty bundle.
     */
    public @NonNull Bundle getExtras() {
        return mExtras == null ? new Bundle() : mExtras;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mSelections);
        dest.writeBundle(mExtras);
    }

    public static final @android.annotation.NonNull Creator<ClassificationsRequest> CREATOR =
            new Creator<ClassificationsRequest>() {
        @Override
        public ClassificationsRequest createFromParcel(Parcel source) {
            return new ClassificationsRequest(
                    source.createTypedArrayList(ContentSelection.CREATOR),
                    source.readBundle());
        }

        @Override
        public ClassificationsRequest[] newArray(int size) {
            return new ClassificationsRequest[size];
        }
    };

    /**
     * A builder for classifications request events.
     * @hide
     */
    @SystemApi
    public static final class Builder {

        private final List<ContentSelection> mSelections;
        private Bundle mExtras;

        public Builder(@NonNull List<ContentSelection> selections) {
            mSelections = selections;
        }

        /**
         * Sets the request extras.
         */
        public @NonNull Builder setExtras(@NonNull Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Builds a new request instance.
         */
        public @NonNull ClassificationsRequest build() {
            return new ClassificationsRequest(mSelections, mExtras);
        }
    }
}
