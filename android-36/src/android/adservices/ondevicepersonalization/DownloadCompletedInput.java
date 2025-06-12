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

package android.adservices.ondevicepersonalization;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.adservices.ondevicepersonalization.flags.Flags;

import java.util.Objects;

/**
 * The input data for {@link
 * IsolatedWorker#onDownloadCompleted(DownloadCompletedInput, android.os.OutcomeReceiver)}.
 *
 */
public final class DownloadCompletedInput {
    /**
     * A {@link KeyValueStore} that contains the downloaded content.
     */
    @NonNull KeyValueStore mDownloadedContents;


    /** Creates a {@link DownloadCompletedInput}
     *
     * @param downloadedContents a {@link KeyValueStore} that contains the downloaded contents.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public DownloadCompletedInput(
            @NonNull KeyValueStore downloadedContents) {
        this.mDownloadedContents = Objects.requireNonNull(downloadedContents);
    }

    /**
     * Map containing downloaded keys and values
     */
    public @NonNull KeyValueStore getDownloadedContents() {
        return mDownloadedContents;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        DownloadCompletedInput that = (DownloadCompletedInput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mDownloadedContents, that.mDownloadedContents);
    }

    @Override
    public int hashCode() {
        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mDownloadedContents);
        return _hash;
    }

    // TODO(b/353356413): Remove builder after it is not used in CTS.
    /**
     * A builder for {@link DownloadCompletedInput}
     * @hide
     */
    public static final class Builder {
        private @NonNull KeyValueStore mDownloadedContents;

        public Builder(
                @NonNull KeyValueStore downloadedContents) {
            mDownloadedContents = downloadedContents;
        }

        public @NonNull DownloadCompletedInput build() {
            DownloadCompletedInput o = new DownloadCompletedInput(
                    mDownloadedContents);
            return o;
        }
    }
}
