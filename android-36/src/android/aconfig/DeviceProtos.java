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
package android.aconfig;

import android.aconfig.nano.Aconfig.parsed_flag;
import android.aconfig.nano.Aconfig.parsed_flags;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @hide
 */
public class DeviceProtos {
	public static final String[] PATHS = {
"/system/etc/aconfig_flags.pb",
"/product/etc/aconfig_flags.pb",
"/vendor/etc/aconfig_flags.pb",
    };

    private static final String APEX_DIR = "/apex";
    private static final String APEX_ACONFIG_PATH_SUFFIX = "/etc/aconfig_flags.pb";

    /**
     * Returns a list of all on-device aconfig protos.
     *
     * May throw an exception if the protos can't be read at the call site. For
     * example, some of the protos are in the apex/ partition, which is mounted
     * somewhat late in the boot process.
     *
     * @throws IOException if we can't read one of the protos yet
     * @return a list of all on-device aconfig protos
     */
    public static List<parsed_flag> loadAndParseFlagProtos() throws IOException {
        ArrayList<parsed_flag> result = new ArrayList();

        for (String path : parsedFlagsProtoPaths()) {
            try (FileInputStream inputStream = new FileInputStream(path)) {
                parsed_flags parsedFlags = parsed_flags.parseFrom(inputStream.readAllBytes());
                for (parsed_flag flag : parsedFlags.parsedFlag) {
                    result.add(flag);
                }
            }
        }

        return result;
    }

    /**
     * Returns the list of all on-device aconfig protos paths.
     * @hide
     */
    public static List<String> parsedFlagsProtoPaths() {
        ArrayList<String> paths = new ArrayList(Arrays.asList(PATHS));

        File apexDirectory = new File(APEX_DIR);
        if (!apexDirectory.isDirectory()) {
            return paths;
        }

        File[] subdirs = apexDirectory.listFiles();
        if (subdirs == null) {
            return paths;
        }

        for (File prefix : subdirs) {
            // For each mainline modules, there are two directories, one <modulepackage>/,
            // and one <modulepackage>@<versioncode>/. Just read the former.
            if (prefix.getAbsolutePath().contains("@")) {
                continue;
            }

            File protoPath = new File(prefix + APEX_ACONFIG_PATH_SUFFIX);
            if (!protoPath.exists()) {
                continue;
            }

            paths.add(protoPath.getAbsolutePath());
        }
        return paths;
    }
}
