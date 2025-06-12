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

package android.health.connect.aidl;

import android.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.AppInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A parcel to carry response to {@link HealthConnectManager#getContributorApplicationsInfo}
 *
 * @hide
 */
public class ApplicationInfoResponseParcel implements Parcelable {

    private final List<AppInfo> mAppInfoList;
    private static final int COMPRESS_FACTOR = 100;

    public ApplicationInfoResponseParcel(@NonNull List<AppInfo> appInfoList) {
        Objects.requireNonNull(appInfoList);
        mAppInfoList = appInfoList;
    }

    public static final Creator<ApplicationInfoResponseParcel> CREATOR =
            new Creator<ApplicationInfoResponseParcel>() {
                @Override
                public ApplicationInfoResponseParcel createFromParcel(Parcel in) {
                    return new ApplicationInfoResponseParcel(in);
                }

                @Override
                public ApplicationInfoResponseParcel[] newArray(int size) {
                    return new ApplicationInfoResponseParcel[size];
                }
            };

    protected ApplicationInfoResponseParcel(Parcel in) {
        int size = in.readInt();

        mAppInfoList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String packageName = in.readString();
            String name = in.readString();
            byte[] icon = in.createByteArray();
            Bitmap bitmap =
                    icon != null ? BitmapFactory.decodeByteArray(icon, 0, icon.length) : null;
            mAppInfoList.add(new AppInfo.Builder(packageName, name, bitmap).build());
        }
    }

    @NonNull
    public List<AppInfo> getAppInfoList() {
        return mAppInfoList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written. May be 0 or {@link
     *     #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mAppInfoList.size());
        mAppInfoList.forEach(
                (appInfo -> {
                    dest.writeString(appInfo.getPackageName());
                    dest.writeString(appInfo.getName());
                    Bitmap bitmap = appInfo.getIcon();
                    byte[] bitmapData = null;
                    if (bitmap != null) {
                        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESS_FACTOR, stream);
                            bitmapData = stream.toByteArray();
                        } catch (IOException exception) {
                            throw new IllegalArgumentException(exception);
                        }
                    }
                    dest.writeByteArray(bitmapData);
                }));
    }
}
