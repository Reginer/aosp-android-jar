/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package benchmarks.regression;

import com.google.caliper.Param;

public class StringReplaceAllBenchmark {
    // NOTE: These estimates of MOVEABLE / NON_MOVEABLE are based on a knowledge of
    // ART implementation details. They make a difference here because JNI calls related
    // to strings took different paths depending on whether the String in question was
    // moveable or not.
    enum StringLengths {
        EMPTY(""),
        MOVEABLE_16(makeString(16)),
        MOVEABLE_256(makeString(256)),
        MOVEABLE_1024(makeString(1024)),
        NON_MOVEABLE(makeString(64 * 1024)),
        BOOT_IMAGE(java.util.jar.JarFile.MANIFEST_NAME);

        private final String value;

        StringLengths(String s) {
            this.value = s;
        }
    }

    private static final String makeString(int length) {
        final String sequence8 = "abcdefghijklmnop";
        final int numAppends = (length / 16) - 1;
        StringBuilder stringBuilder = new StringBuilder(length);

        // (n-1) occurences of "abcdefghijklmnop"
        for (int i = 0; i < numAppends; ++i) {
            stringBuilder.append(sequence8);
        }

        // and one final occurence of qrstuvwx.
        stringBuilder.append("qrstuvwx");

        return stringBuilder.toString();
    }

    @Param private StringLengths s;

    public void timeReplaceAllTrivialPatternNonExistent(int reps) {
        for (int i = 0; i < reps; ++i) {
            s.value.replaceAll("fish", "0");
        }
    }

    public void timeReplaceTrivialPatternAllRepeated(int reps) {
        for (int i = 0; i < reps; ++i) {
            s.value.replaceAll("jklm", "0");
        }
    }

    public void timeReplaceAllTrivialPatternSingleOccurence(int reps) {
        for (int i = 0; i < reps; ++i) {
            s.value.replaceAll("qrst", "0");
        }
    }
}
