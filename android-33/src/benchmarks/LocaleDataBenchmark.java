/*
 * Copyright (C) 2020 The Android Open Source Project
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

package benchmarks;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import java.util.Locale;
import libcore.icu.LocaleData;

public final class LocaleDataBenchmark {
    private static final Locale[] TEST_LOCALES = new Locale[]  {
        Locale.forLanguageTag("en-US"),
        Locale.forLanguageTag("jp-JP"),
        Locale.forLanguageTag("es-419"),
        Locale.forLanguageTag("ar-EG"),
        Locale.forLanguageTag("zh-CN"),
    };

    public void timeInitLocaleData(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            for (Locale locale : TEST_LOCALES) {
                LocaleData.initLocaleData(locale);
            }
        }
    }
}
