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

package android.devicelock;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Wrapper class for transporting exceptions, similar to android.os.ParcelableException that is
 * hidden and cannot be used by device lock.
 *
 * @hide
 */
public final class ParcelableException extends Exception implements Parcelable {
    public ParcelableException(Exception t) {
        super(t);
    }

    private static Exception readFromParcel(Parcel in) {
        final String name = in.readString();
        final String msg = in.readString();
        try {
            final Class<?> clazz = Class.forName(name, true, Parcelable.class.getClassLoader());
            if (Exception.class.isAssignableFrom(clazz)) {
                return (Exception) clazz.getConstructor(String.class).newInstance(msg);
            }
        } catch (ReflectiveOperationException e) {
            // return the below exception in this case.
        }
        return new Exception(name + ": " + msg);
    }

    private static void writeToParcel(Parcel out, Throwable t) {
        out.writeString(t.getClass().getName());
        out.writeString(t.getMessage());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        writeToParcel(dest, getCause());
    }

    /**
     * Required per Parcelable documentation.
     */
    public static final Creator<ParcelableException> CREATOR = new Creator<>() {
        @Override
        public ParcelableException createFromParcel(Parcel source) {
            return new ParcelableException(readFromParcel(source));
        }

        @Override
        public ParcelableException[] newArray(int size) {
            return new ParcelableException[size];
        }
    };

    /**
     * Get the encapsulated exception.
     */
    public Exception getException() {
        return (Exception) getCause();
    }
}
