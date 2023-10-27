/*
 * Copyright (C) 2009 The Android Open Source Project
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

package libcore.icu;

import android.icu.text.DecimalFormat;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat;
import android.icu.text.NumberingSystem;
import android.icu.util.ULocale;
import com.android.icu.text.ExtendedDecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data cache for classes, e.g. {@link java.text.DecimalFormat} and
 * {@link java.text.DecimalFormatSymbols}.
 *
 * @hide
 */
public class DecimalFormatData {

    // TODO(http://b/217881004): Replace this with a LRU cache.
    private static final ConcurrentHashMap<String, DecimalFormatData> CACHE =
            new ConcurrentHashMap<>(/* initialCapacity */ 3);

    private final char zeroDigit;
    private final char decimalSeparator;
    private final char groupingSeparator;
    private final char patternSeparator;
    private final String percent;
    private final String perMill;
    private final String monetarySeparator;
    private final String monetaryGroupSeparator;
    private final String minusSign;
    private final String exponentSeparator;
    private final String infinity;
    private final String NaN;

    private final String numberPattern;
    private final String currencyPattern;
    private final String percentPattern;

    private DecimalFormatData(Locale locale) {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(locale);

        decimalSeparator = dfs.getDecimalSeparator();
        groupingSeparator = dfs.getGroupingSeparator();
        percent = dfs.getPercentString();
        perMill = dfs.getPerMillString();
        monetarySeparator = dfs.getDecimalSeparatorString();
        monetaryGroupSeparator = dfs.getMonetaryGroupingSeparatorString();
        minusSign = dfs.getMinusSignString();
        exponentSeparator = dfs.getExponentSeparator();
        infinity = dfs.getInfinity();
        NaN = dfs.getNaN();
        zeroDigit = dfs.getZeroDigit();

        // Libcore localizes pattern separator while ICU doesn't. http://b/112080617
        patternSeparator = loadPatternSeparator(locale);

        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(
                locale, NumberFormat.NUMBERSTYLE);
        numberPattern = df.toPattern();

        df = (DecimalFormat) NumberFormat.getInstance(locale, NumberFormat.CURRENCYSTYLE);
        currencyPattern = df.toPattern();

        df = (DecimalFormat) NumberFormat.getInstance(locale, NumberFormat.PERCENTSTYLE);
        percentPattern = df.toPattern();
    }

    /**
     * Returns an instance.
     *
     * @param locale can't be null
     * @throws NullPointerException if {@code locale} is null
     * @return a {@link DecimalFormatData} instance
     */
    public static DecimalFormatData getInstance(Locale locale) {
        Objects.requireNonNull(locale, "locale can't be null");

        locale = LocaleData.getCompatibleLocaleForBug159514442(locale);

        final String languageTag = locale.toLanguageTag();

        DecimalFormatData data = CACHE.get(languageTag);
        if (data != null) {
            return data;
        }

        data = new DecimalFormatData(locale);
        DecimalFormatData prev = CACHE.putIfAbsent(languageTag, data);
        if (prev != null) {
            return prev;
        }
        return data;
    }

    /**
     * Ensure that we pull in the locale data for the root locale, en_US, and the user's default
     * locale. All devices must support the root locale and en_US, and they're used for various
     * system things. Pre-populating the cache is especially useful on Android because
     * we'll share this via the Zygote.
     */
    public static void initializeCacheInZygote() {
        getInstance(Locale.ROOT);
        getInstance(Locale.US);
        getInstance(Locale.getDefault());
    }

    public char getZeroDigit() {
        return zeroDigit;
    }

    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    public char getGroupingSeparator() {
        return groupingSeparator;
    }

    public char getPatternSeparator() {
        return patternSeparator;
    }

    public String getPercent() {
        return percent;
    }

    public String getPerMill() {
        return perMill;
    }

    public String getMonetarySeparator() {
        return monetarySeparator;
    }

    public String getMonetaryGroupSeparator() {
        return monetaryGroupSeparator;
    }

    public String getMinusSign() {
        return minusSign;
    }

    public String getExponentSeparator() {
        return exponentSeparator;
    }

    public String getInfinity() {
        return infinity;
    }

    public String getNaN() {
        return NaN;
    }

    public String getNumberPattern() {
        return numberPattern;
    }

    public String getCurrencyPattern() {
        return currencyPattern;
    }

    public String getPercentPattern() {
        return percentPattern;
    }

    // Libcore localizes pattern separator while ICU doesn't. http://b/112080617
    private static char loadPatternSeparator(Locale locale) {
        ULocale uLocale = ULocale.forLocale(locale);
        NumberingSystem ns = NumberingSystem.getInstance(uLocale);
        // A numbering system could be numeric or algorithmic. DecimalFormat can only use
        // a numeric and decimal-based (radix == 10) system. Fallback to a Latin, a known numeric
        // and decimal-based if the default numbering system isn't. All locales should have data
        // for Latin numbering system after locale data fallback. See Numbering system section
        // in Unicode Technical Standard #35 for more details.
        if (ns == null || ns.getRadix() != 10 || ns.isAlgorithmic()) {
            ns = NumberingSystem.LATIN;
        }
        String patternSeparator = ExtendedDecimalFormatSymbols.getInstance(uLocale, ns)
                .getLocalizedPatternSeparator();

        if (patternSeparator == null || patternSeparator.isEmpty()) {
            patternSeparator = ";";
        }

        // Pattern separator in libcore supports single java character only.
        return patternSeparator.charAt(0);
    }
}
