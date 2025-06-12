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
import android.annotation.SuppressLint;
import android.annotation.SystemApi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.providers.media.flags.Flags;

import java.util.List;



/**
 * <p>
 * The response class for {@link MediaCognitionProcessingRequest}.
 * Use Builder class and setter functions like {@link Builder#setImageOcrLatin(String)}
 * to set results of cognition processing.
 * </p>
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_MEDIA_COGNITION_SERVICE)
public class MediaCognitionProcessingResponse {

    private MediaCognitionProcessingRequest mRequest;
    private List<String> mImageLabels;
    private String mImageOcrLatin;


    private MediaCognitionProcessingResponse(Builder builder) {
        mRequest = builder.mRequest;
        mImageLabels = builder.mImageLabels;
        mImageOcrLatin = builder.mImageOcrLatin;
    }

    /**
     * Builder class to build {@link MediaCognitionProcessingResponse} instances.
     */
    public static final class Builder {
        private MediaCognitionProcessingRequest mRequest; // Required
        private List<String> mImageLabels = null;
        private String mImageOcrLatin = null;

        public Builder(@NonNull MediaCognitionProcessingRequest request) {
            this.mRequest = request;
        }

        /**
         * Call this setter function in response of the
         * processing: {@link MediaCognitionService.ProcessingTypes#IMAGE_LABEL}
         * @param imageLabels list of image labels
         */
        @NonNull
        public Builder setImageLabels(@Nullable List<String> imageLabels) {
            this.mImageLabels = imageLabels;
            return this;
        }

        /**
         * Call this setter function in response of the
         * processing: {@link MediaCognitionService.ProcessingTypes#IMAGE_OCR_LATIN}
         * @param imageOcrLatin latin text content in the image
         */
        @NonNull
        public Builder setImageOcrLatin(@Nullable String imageOcrLatin) {
            this.mImageOcrLatin = imageOcrLatin;
            return this;
        }

        /**
         * Instantiate a {@link MediaCognitionProcessingResponse} from this {@link Builder}
         * @return {@link MediaCognitionProcessingResponse} representation of this builder
         */
        @NonNull
        public MediaCognitionProcessingResponse build() {
            return new MediaCognitionProcessingResponse(this);
        }
    }

    /**
     * get {@link MediaCognitionProcessingRequest} set by {@link Builder}
     */
    @NonNull
    public MediaCognitionProcessingRequest getRequest() {
        return mRequest;
    }

    /**
     * get Image Labels set by {@link Builder#setImageLabels(List)}
     */
    @SuppressLint("NullableCollection") // return value can be null
    @Nullable
    public List<String> getImageLabels() {
        return mImageLabels;
    }

    /**
     * get Image Ocr Latin set by {@link Builder#setImageOcrLatin(String)}
     */
    @Nullable
    public String getImageOcrLatin() {
        return mImageOcrLatin;
    }

}
