/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.text.style;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

/**
 * A SuggestionRangeSpan is used to show which part of an EditText is affected by a suggestion
 * popup window.
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public final class SuggestionRangeSpan extends CharacterStyle implements ParcelableSpan {
    private int mBackgroundColor;

    public SuggestionRangeSpan() {
        // 0 is a fully transparent black. Has to be set using #setBackgroundColor
        mBackgroundColor = 0;
    }

    /** @hide */
    public SuggestionRangeSpan(@NonNull Parcel src) {
        mBackgroundColor = src.readInt();
    }

    public static final @NonNull Parcelable.Creator<SuggestionRangeSpan>
            CREATOR = new Parcelable.Creator<SuggestionRangeSpan>() {
                @Override
                public SuggestionRangeSpan createFromParcel(Parcel source) {
                    return new SuggestionRangeSpan(source);
                }

                @Override
                public SuggestionRangeSpan[] newArray(int size) {
                    return new SuggestionRangeSpan[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        writeToParcelInternal(dest, flags);
    }

    /** @hide */
    public void writeToParcelInternal(Parcel dest, int flags) {
        dest.writeInt(mBackgroundColor);
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    /** @hide */
    public int getSpanTypeIdInternal() {
        return TextUtils.SUGGESTION_RANGE_SPAN;
    }

    public void setBackgroundColor(int backgroundColor) {
        mBackgroundColor = backgroundColor;
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint tp) {
        tp.bgColor = mBackgroundColor;
    }
}
