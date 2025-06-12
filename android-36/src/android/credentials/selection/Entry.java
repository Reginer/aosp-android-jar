/*
 * Copyright 2022 The Android Open Source Project
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

package android.credentials.selection;

import static android.credentials.flags.Flags.FLAG_CONFIGURABLE_SELECTOR_UI_ENABLED;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.app.slice.Slice;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.AnnotationValidations;

/**
 * A credential, create, or action entry to be rendered.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(FLAG_CONFIGURABLE_SELECTOR_UI_ENABLED)
public final class Entry implements Parcelable {
    @NonNull
    private final String mKey;
    @NonNull
    private final String mSubkey;
    @Nullable
    private PendingIntent mPendingIntent;
    @Nullable
    private Intent mFrameworkExtrasIntent;

    @NonNull
    private final Slice mSlice;

    private Entry(@NonNull Parcel in) {
        String key = in.readString8();
        String subkey = in.readString8();
        Slice slice = in.readTypedObject(Slice.CREATOR);

        mKey = key;
        AnnotationValidations.validate(NonNull.class, null, mKey);
        mSubkey = subkey;
        AnnotationValidations.validate(NonNull.class, null, mSubkey);
        mSlice = slice;
        AnnotationValidations.validate(NonNull.class, null, mSlice);
        mPendingIntent = in.readTypedObject(PendingIntent.CREATOR);
        mFrameworkExtrasIntent = in.readTypedObject(Intent.CREATOR);
    }

    /**
     * Constructor to be used for an entry that requires a pending intent to be invoked
     * when clicked.
     *
     * @param key    the identifier of this entry that's unique within the context of the given
     *               CredentialManager request. This is used when constructing the
     *               {@link android.credentials.selection.UserSelectionResult#UserSelectionResult(
     *                String providerId, String entryKey, String entrySubkey,
     *                ProviderPendingIntentResponse providerPendingIntentResponse)}
     * @param subkey the sub-identifier of this entry that's unique within the context of the
     *               {@code key}. This is used when constructing the
     *               {@link android.credentials.selection.UserSelectionResult#UserSelectionResult(
     *                String providerId, String entryKey, String entrySubkey,
     *                ProviderPendingIntentResponse providerPendingIntentResponse)}
     * @param intent the intent containing extra data that has to be filled in when launching this
     *               entry's provider PendingIntent
     * @param slice  the Slice to be displayed
     */
    public Entry(@NonNull String key, @NonNull String subkey, @NonNull Slice slice,
            @NonNull Intent intent) {
        mKey = key;
        mSubkey = subkey;
        mSlice = slice;
        mFrameworkExtrasIntent = intent;
    }

    /**
     * Returns the identifier of this entry that's unique within the context of the
     * CredentialManager request.
     *
     * Generally used when sending the user selection result back to the system service.
     */
    @NonNull
    public String getKey() {
        return mKey;
    }

    /**
     * Returns the sub-identifier of this entry that's unique within the context of the {@code key}.
     *
     * Generally used when sending the user selection result back to the system service.
     */
    @NonNull
    public String getSubkey() {
        return mSubkey;
    }

    /** Returns the Slice to be rendered. */
    @NonNull
    public Slice getSlice() {
        return mSlice;
    }

    /**
     * Returns the intent containing extra data that has to be filled in when launching this
     * entry's provider PendingIntent.
     *
     * If null, the provider PendingIntent can be launched without any fill in intent.
     */
    @Nullable
    @SuppressLint("IntentBuilderName") // Not building a new intent.
    public Intent getFrameworkExtrasIntent() {
        return mFrameworkExtrasIntent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString8(mKey);
        dest.writeString8(mSubkey);
        dest.writeTypedObject(mSlice, flags);
        dest.writeTypedObject(mPendingIntent, flags);
        dest.writeTypedObject(mFrameworkExtrasIntent, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final @NonNull Creator<Entry> CREATOR = new Creator<>() {
        @Override
        public Entry createFromParcel(@NonNull Parcel in) {
            return new Entry(in);
        }

        @Override
        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };
}
