/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.icu.text.TimeZoneNames;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Benchmark for java.text.SimpleDateFormat. This tests common formatting, parsing and creation
 * operations with a specific focus on TimeZone handling.
 */
public class SimpleDateFormatBenchmark {
    public void time_createFormatWithTimeZone(int reps) {
        for (int i = 0; i < reps; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd z");
        }
    }

    public void time_parseWithTimeZoneShort(int reps) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd z");
        for (int i = 0; i < reps; i++) {
            sdf.parse("2000.01.01 PST");
        }
    }

    public void time_parseWithTimeZoneLong(int reps) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd zzzz");
        for (int i = 0; i < reps; i++) {
            sdf.parse("2000.01.01 Pacific Standard Time");
        }
    }

    public void time_parseWithoutTimeZone(int reps) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        for (int i = 0; i < reps; i++) {
            sdf.parse("2000.01.01");
        }
    }

    public void time_createAndParseWithTimeZoneShort(int reps) throws ParseException {
        for (int i = 0; i < reps; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd z");
            sdf.parse("2000.01.01 PST");
        }
    }

    public void time_createAndParseWithTimeZoneLong(int reps) throws ParseException {
        for (int i = 0; i < reps; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd zzzz");
            sdf.parse("2000.01.01 Pacific Standard Time");
        }
    }

    public void time_formatWithTimeZoneShort(int reps) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd z");
        for (int i = 0; i < reps; i++) {
            sdf.format(new Date());
        }
    }

    public void time_formatWithTimeZoneLong(int reps) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd zzzz");
        for (int i = 0; i < reps; i++) {
            sdf.format(new Date());
        }
    }

    /**
     * Times first-time execution to measure effects of initial loading of data that's lost in
     * full caliper benchmarks.
     */
    public static void main(String[] args) throws ParseException {
        long start, end;

        Locale locale = Locale.GERMAN;
        start = System.nanoTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd zzzz", locale);
        end = System.nanoTime();
        System.out.printf("Creating first SDF: %,d ns\n", end-start);

        // N code had special cases for currently-set and for default timezone. We want to measure
        // the generic case.
        sdf.setTimeZone(TimeZone.getTimeZone("Hongkong"));

        start = System.nanoTime();
        sdf.parse("2000.1.1 Kubanische Normalzeit");
        end = System.nanoTime();
        System.out.printf("First parse: %,d ns\n", end-start);

        start = System.nanoTime();
        sdf.format(new Date());
        end = System.nanoTime();
        System.out.printf("First format: %,d ns\n", end-start);
    }
}
