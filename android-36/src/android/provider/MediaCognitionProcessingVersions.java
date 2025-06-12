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
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaCognitionService.ProcessingTypes;

import androidx.annotation.NonNull;

import com.android.providers.media.flags.Flags;

import java.util.Arrays;


/**
 * A class containing versions of different cognition processing methods that can be
 * requested by MediaProvider. See {@link MediaCognitionService.ProcessingTypes}
 *
 * This will be sent by the implementation of MediaCognitionService in response of
 * {@link MediaCognitionService#onGetProcessingVersions}.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_MEDIA_COGNITION_SERVICE)
public final class MediaCognitionProcessingVersions implements Parcelable {
    private int[] mProcessingVersions;

    /**
     * @hide
     */
    MediaCognitionProcessingVersions(int[] processingVersions) {
        mProcessingVersions = processingVersions;
    }

    /**
     * This will create an empty instance. Use {@link #setProcessingVersion} to set versions.
     */
    public MediaCognitionProcessingVersions() {
        mProcessingVersions = new int[ProcessingTypes.class.getDeclaredFields().length];
        Arrays.fill(mProcessingVersions, -1);
    }

    /**
     * This function is used to set versions of different processing types
     * implemented in MediaCognitionService. (like {@code IMAGE_LABEL})
     *
     * @param processingType Any one of the processing type
     *                       from {@link MediaCognitionService.ProcessingTypes}
     * @param version The current version of the processing.
     */
    public void setProcessingVersion(@MediaCognitionService.ProcessingType int processingType,
            int version) {
        final int index = Integer.numberOfTrailingZeros(processingType);
        if (!checkProcessingTypeCorrectness(processingType, index)) {
            throw new IllegalArgumentException("Wrong Processing Type");
        }
        mProcessingVersions[index] = version;
    }

    /**
     * Get the versions of processing types set by {@link #setProcessingVersion(int, int)}
     * If not set, default value is -1
     *
     * @param processingType Any one of the processing type
     *                       from {@link MediaCognitionService.ProcessingTypes}
     */
    public int getProcessingVersion(@MediaCognitionService.ProcessingType  int processingType) {
        final int index = Integer.numberOfTrailingZeros(processingType);
        if (!checkProcessingTypeCorrectness(processingType, index)) {
            throw new IllegalArgumentException("Wrong Processing Type");
        }
        return mProcessingVersions[index];
    }

    private boolean checkProcessingTypeCorrectness(
            @MediaCognitionService.ProcessingType  int processingType, int index) {
        if (Integer.bitCount(processingType) != 1 || index >= mProcessingVersions.length) {
            return false;
        }
        return true;
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
        dest.writeIntArray(mProcessingVersions);
    }

    @NonNull
    public static final Creator<MediaCognitionProcessingVersions> CREATOR =
            new Creator<MediaCognitionProcessingVersions>() {
                @Override
                public MediaCognitionProcessingVersions createFromParcel(Parcel source) {
                    int[] processingVersions =
                            new int[ProcessingTypes.class.getDeclaredFields().length];
                    source.readIntArray(processingVersions);
                    return new MediaCognitionProcessingVersions(processingVersions);
                }

                @Override
                public MediaCognitionProcessingVersions[] newArray(int size) {
                    return new MediaCognitionProcessingVersions[size];
                }
            };
}
