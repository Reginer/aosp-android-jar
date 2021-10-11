/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.util.imagepool;

import com.android.tools.layoutlib.annotations.NotNull;

import android.util.imagepool.ImagePool.ImagePoolPolicy;

public class ImagePoolProvider {

    private static ImagePool sInstance;

    @NotNull
    public static synchronized ImagePool get() {
        if (sInstance == null) {
            // Idea is to create more
            ImagePoolPolicy policy = new ImagePoolPolicy(
                    new int[]{100, 200, 400, 600, 800, 1000, 1600, 3200},
                    new int[]{  3,   3,   2,   2,   2,    1,    1,    1},
                    10_000_000L); // 10 MB

            sInstance = new ImagePoolImpl(policy);
        }
        return sInstance;
    }
}