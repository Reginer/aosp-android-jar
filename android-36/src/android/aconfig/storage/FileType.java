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

package android.aconfig.storage;

public enum FileType {
    PACKAGE_MAP(0),
    FLAG_MAP(1),
    FLAG_VAL(2),
    FLAG_INFO(3);

    public final int type;

    FileType(int type) {
        this.type = type;
    }

    public static FileType fromInt(int index) {
        switch (index) {
            case 0:
                return PACKAGE_MAP;
            case 1:
                return FLAG_MAP;
            case 2:
                return FLAG_VAL;
            case 3:
                return FLAG_INFO;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case 0:
                return "PACKAGE_MAP";
            case 1:
                return "FLAG_MAP";
            case 2:
                return "FLAG_VAL";
            case 3:
                return "FLAG_INFO";
            default:
                return "unrecognized type";
        }
    }
}
