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

package android.provider;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaCognitionService.ProcessingCombination;
import android.provider.MediaCognitionService.ProcessingType;

import com.android.providers.media.flags.Flags;


/**
 * This is a request made by MediaProvider to the implementation of {@link MediaCognitionService}.
 * This contains request for a single media.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_MEDIA_COGNITION_SERVICE)
public final class MediaCognitionProcessingRequest implements Parcelable {
    private Uri mUri;

    @ProcessingCombination
    private int mProcessingCombination;


    private MediaCognitionProcessingRequest(Builder builder) {
        mUri = builder.mUri;
        mProcessingCombination = builder.mProcessingCombination;
    }

    /**
     * Builder class to build {@link MediaCognitionProcessingRequest} instances.
     */
    public static final class Builder {
        private Uri mUri; // Required
        @ProcessingCombination
        private int mProcessingCombination;


        public Builder(@NonNull Uri uri) {
            this.mUri = uri;
        }

        /**
         * set processing mode of the request.
         * @param processingCombination bitmask of requests that can contain
         *                       any combination of {@link MediaCognitionService.ProcessingTypes}
         */
        @NonNull
        public Builder setProcessingCombination(@ProcessingCombination int processingCombination) {
            this.mProcessingCombination = processingCombination;
            return this;
        }

        /**
         * add processing types in the request.
         * @param processingRequest a single processing request that can contain any processing type
         *                         from {@link MediaCognitionService.ProcessingTypes}
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addProcessingRequest(@ProcessingType int processingRequest) {
            this.mProcessingCombination |= processingRequest;
            return this;
        }

        /**
         * Instantiate a {@link MediaCognitionProcessingRequest} from this {@link Builder}
         * @return {@link MediaCognitionProcessingRequest} representation of this builder
         */
        @NonNull
        public MediaCognitionProcessingRequest build() {
            return new MediaCognitionProcessingRequest(this);
        }
    }

    /**
     * Get {@link Uri}  of the media for which the request of processing is made for
     * @return {@link Uri}  of the media
     */
    @NonNull
    public Uri getUri() {
        return mUri;
    }

    /**
     * Use this function to check if processing is requested for a specific processing type
     * @param processingType Any processing type from {@link MediaCognitionService.ProcessingTypes}
     * @return true if processing requested, false otherwise
     */
    public boolean checkProcessingRequired(@ProcessingCombination int processingType) {
        return (mProcessingCombination & processingType) == processingType;
    }

    /**
     * This function returns a bitmask for all the processing that has been requested for.
     *
     * @return bitmask of all processing requests
     * based on {@link MediaCognitionService.ProcessingTypes}
     */
    @ProcessingCombination
    public int getProcessingCombination() {
        return mProcessingCombination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mUri.toString());
        dest.writeInt(mProcessingCombination);
    }

    @NonNull
    public static final Creator<MediaCognitionProcessingRequest> CREATOR =
            new Creator<MediaCognitionProcessingRequest>() {
                @Override
                public MediaCognitionProcessingRequest createFromParcel(Parcel source) {
                    Uri uri = Uri.parse(source.readString());
                    int processingCombination = source.readInt();

                    return new MediaCognitionProcessingRequest.Builder(uri)
                            .setProcessingCombination(processingCombination)
                            .build();
                }

                @Override
                public MediaCognitionProcessingRequest[] newArray(int size) {
                    return new MediaCognitionProcessingRequest[size];
                }
            };

}
