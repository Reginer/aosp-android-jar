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
 * limitations under the License.
 */
package benchmarks.regression;

import android.icu.lang.UCharacter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Decode the same size of ASCII, BMP, Supplementary character using fast-path UTF-8 decoder.
 * The fast-path code is in {@link StringFactory#newStringFromBytes(byte[], int, int, Charset)}
 */
public class CharsetUtf8Benchmark {

    private static final int NO_OF_BYTES = 0x400000; // 4MB
    private static final byte[] ASCII = makeUnicodeRange(0, 0x7f, NO_OF_BYTES / 0x80);
    private static final byte[] BMP2 = makeUnicodeRange(0x0080, 0x07ff, NO_OF_BYTES / 2 / 0x780);
    private static final byte[] BMP3 = makeUnicodeRange(0x0800, 0xffff,
            NO_OF_BYTES / 3 / 0xf000 /* 0x10000 - 0x0800 - no of surrogate code points */);
    private static final byte[] SUPPLEMENTARY = makeUnicodeRange(0x10000, 0x10ffff,
            NO_OF_BYTES / 4 / 0x100000);

    private static byte[] makeUnicodeRange(int startingCodePoint, int endingCodePoint,
            int repeated) {
        StringBuilder builder = new StringBuilder();
        for (int codePoint = startingCodePoint; codePoint <= endingCodePoint; codePoint++) {
            if (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE) {
                builder.append(UCharacter.toString(codePoint));
            }
        }

        String str = builder.toString();
        builder = new StringBuilder();
        for (int i = 0; i < repeated; i++) {
            builder.append(str);
        }
        return builder.toString().getBytes();
    }

    public void time_ascii() {
        new String(ASCII, StandardCharsets.UTF_8);
    }

    public void time_bmp2() {
        new String(BMP2, StandardCharsets.UTF_8);
    }

    public void time_bmp3() {
        new String(BMP3, StandardCharsets.UTF_8);
    }

    public void time_supplementary() {
        new String(SUPPLEMENTARY, StandardCharsets.UTF_8);
    }
}
