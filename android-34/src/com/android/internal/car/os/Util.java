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

package com.android.internal.car.os;

import android.annotation.NonNull;

/**
 * Generic OS level utility which can also have JNI support.
 * @hide
 */
public class Util {

    /**
     * Assigns the given process to the specified process profile.
     *
     * <p>It will throw {@link IllegalArgumentException} for any failure.
     *
     * @param pid PID of the target process.
     * @param uid UID of the target process.
     * @param profile Process profile to set.
     *
     * @hide
     */
    public static native void setProcessProfile(int pid, int uid, @NonNull String profile);

}
