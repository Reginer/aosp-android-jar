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

package android.app.sdksandbox;

import android.annotation.NonNull;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

// TODO(b/239403323): Add unit tests for this class.
/**
 * Class to encapsulate change in {@link SharedPreferences}.
 *
 * <p>To be used for passing updates to Sandbox for syncing data via {@link
 * SharedPreferencesSyncManager#syncData()}.
 *
 * <p>Each update instance contains a list of {@link SharedPreferencesKey}, which are the keys whose
 * updates are being sent over with this class. User can get the list using {@link
 * #getKeysInUpdate}.
 *
 * <p>The data associated with the keys are sent as a {@link Bundle} which can be retrieved using
 * {@link #getData()}. If a key is present in list returned in {@link #getKeysInUpdate} but missing
 * in the {@link Bundle}, then that key has been removed in the update.
 *
 * @hide
 */
public final class SharedPreferencesUpdate implements Parcelable {

    private final ArrayList<SharedPreferencesKey> mKeysToSync;
    private final Bundle mData;

    public static final @NonNull Parcelable.Creator<SharedPreferencesUpdate> CREATOR =
            new Parcelable.Creator<SharedPreferencesUpdate>() {
                public SharedPreferencesUpdate createFromParcel(Parcel in) {
                    return new SharedPreferencesUpdate(in);
                }

                public SharedPreferencesUpdate[] newArray(int size) {
                    return new SharedPreferencesUpdate[size];
                }
            };

    public SharedPreferencesUpdate(
            @NonNull Collection<SharedPreferencesKey> keysToSync, @NonNull Bundle data) {
        Objects.requireNonNull(keysToSync, "keysToSync should not be null");
        Objects.requireNonNull(data, "data should not be null");

        mKeysToSync = new ArrayList<>(keysToSync);
        mData = new Bundle(data);
    }

    private SharedPreferencesUpdate(Parcel in) {
        mKeysToSync =
                in.readArrayList(
                        SharedPreferencesKey.class.getClassLoader(), SharedPreferencesKey.class);
        Objects.requireNonNull(mKeysToSync, "mKeysToSync should not be null");

        mData = Bundle.CREATOR.createFromParcel(in);
        Objects.requireNonNull(mData, "mData should not be null");
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeList(mKeysToSync);
        mData.writeToParcel(out, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public List<SharedPreferencesKey> getKeysInUpdate() {
        return mKeysToSync;
    }

    @NonNull
    public Bundle getData() {
        return mData;
    }
}
