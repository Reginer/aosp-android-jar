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

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for performing file related operations
 *
 * @hide
 */
public class FileUtil {

    public static final String TAG = "SdkSandboxManager";
    public static final int CONVERSION_FACTOR_FROM_BYTES_TO_KB = 1024;

    /** Calculate the storage of SDK iteratively */
    public static int getStorageInKbForPaths(List<String> paths) {
        float storageSize = 0;
        for (int i = 0; i < paths.size(); i++) {
            final File dir = new File(paths.get(i));

            if (Objects.nonNull(dir)) {
                storageSize += getStorageForFiles(dir.listFiles());
            }
        }
        return convertByteToKb(storageSize);
    }

    private static float getStorageForFiles(File[] files) {
        if (Objects.isNull(files)) {
            return 0;
        }

        float sizeInBytes = 0;

        for (File file : files) {
            if (file.isDirectory()) {
                sizeInBytes += getStorageForFiles(file.listFiles());
            } else {
                sizeInBytes += file.length();
            }
        }
        return sizeInBytes;
    }

    private static int convertByteToKb(float storageSize) {
        return (int) (storageSize / CONVERSION_FACTOR_FROM_BYTES_TO_KB);
    }
}
