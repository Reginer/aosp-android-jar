/*
 * Copyright (C) 2021 The Android Open Source Project
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.Scanner;

public class ScannerBenchmark {

    private static final Random RANDOM = new Random();

    enum LineLength {
        TINY(1, 5),
        SHORT(10, 50),
        MEDIUM(100, 1_000),
        LONG(10_000, 50_000);

        private final int from;
        private final int to;

        LineLength(int from, int to) {
            this.from = from;
            this.to = to;
        }

        int get() {
            return RANDOM.nextInt(to - from) + from;
        }

        int getMax() {
            return to;
        }
    }

    @Param({"TINY", "SHORT", "MEDIUM"})
    private LineLength lineLength;

    @Param({"1", "5", "10", "100", "1000", "10000", "25000"})
    private int linesCount;

    private byte[] data;
    private int size;

    @BeforeExperiment
    void setUp() {
        data = new byte[lineLength.getMax() * linesCount];
        size = 0;

        for (int i = 0; i < linesCount; ++i) {
            append(makeString(lineLength.get()));
            append("\n");
        }

        byte[] temp = new byte[size];
        System.arraycopy(data, 0, temp, 0, size);
        data = temp;
    }

    private void doubleSize() {
        byte[] temp = new byte[data.length * 2];
        System.arraycopy(data, 0, temp, 0, size);
        data = temp;
    }

    private void append(String line) {
        byte[] lineBytes = line.getBytes();

        while (lineBytes.length + size > data.length) {
            doubleSize();
        }

        System.arraycopy(lineBytes, 0, data, size, lineBytes.length);
        size += lineBytes.length;
    }

    private String makeString(int length) {
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; ++i) {
            builder.append(i % 10).append(", ");
        }

        return builder.toString();
    }

    @Benchmark
    void readAll() {
        Scanner scanner = new Scanner(new ByteArrayInputStream(data));
        while (scanner.hasNext()) {
            scanner.nextLine();
        }
    }
}
