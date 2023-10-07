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

package com.android.clockwork.displayoffload;

import static com.android.clockwork.displayoffload.Utils.TAG;

import android.content.Context;
import android.icu.text.DecimalFormat;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Helper class that handles all locale data manipulation.
 */
public class LocaleHelper {
    private static LocaleConfigAdapter mLocaleConfig;
    private static int[] mNumericCodepoints;

    public static synchronized void onLocaleChanged(Context context) {
        Locale current = context.getResources().getConfiguration().getLocales().get(0);
        DecimalFormat nf = (DecimalFormat) android.icu.text.NumberFormat.getInstance(current);
        LocaleConfigAdapter config = new LocaleConfigAdapter();

        // Prefixes
        config.plusSignPrefix = nf.getDecimalFormatSymbols().getPlusSignString();
        config.minusSignPrefix = nf.getDecimalFormatSymbols().getMinusSignString();

        // Separators
        config.decimalSeparator = nf.getDecimalFormatSymbols().getDecimalSeparatorString();
        config.groupingSeparator = nf.getDecimalFormatSymbols().getGroupingSeparatorString();

        // Group Size
        config.groupSizePrimary = nf.getGroupingSize();
        if (config.groupSizePrimary == 0) {
            config.groupSizePrimary = 3;
        }
        config.groupSizeSecondary = nf.getSecondaryGroupingSize();
        if (config.groupSizeSecondary == 0) {
            config.groupSizeSecondary = config.groupSizePrimary;
        }
        // ICU API is not not available on R. For API levels above, use:
        // config.minGrouping = (byte) nf.getMinimumGroupingDigits();
        config.groupSizeMin = 1;

        // Digits (Need to send as UTF-32)
        String[] digits = nf.getDecimalFormatSymbols().getDigitStrings();
        config.digits = new ArrayList<>(digits.length);
        for (String digit : digits) {
            byte[] utf32Bytes = new byte[0];
            try {
                utf32Bytes = digit.getBytes("UTF-32BE");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Digit [" + digit + "] can't be converted to UTF-32.");
            }
            int utf32Int = 0;
            for (byte utf32Byte : utf32Bytes) {
                // Fill from LSB
                utf32Int = (utf32Int << 8) | utf32Byte;
            }
            config.digits.add(utf32Int);
        }

        // Generate integer code points for font subsetting
        ArrayList<Integer> codepoints = new ArrayList<>();
        codepoints.add(Character.codePointAt(config.minusSignPrefix, 0));
        codepoints.add(Character.codePointAt(config.plusSignPrefix, 0));
        codepoints.add(Character.codePointAt(config.decimalSeparator, 0));
        codepoints.add(Character.codePointAt(config.groupingSeparator, 0));
        for (String digit : digits) {
            codepoints.add(Character.codePointAt(digit, 0));
        }
        mNumericCodepoints = Utils.convertToIntArray(codepoints);

        mLocaleConfig = config;
    }

    public static synchronized LocaleConfigAdapter getCurrentLocaleConfig(Context context) {
        if (mLocaleConfig == null) {
            onLocaleChanged(context);
        }
        return mLocaleConfig;
    }

    public static int[] getCurrentLocaleNumericCodepoints(Context context) {
        if (mNumericCodepoints == null) {
            onLocaleChanged(context);
        }
        return mNumericCodepoints;
    }

    public static synchronized void dump(Context context, IndentingPrintWriter ipw) {
        // Force locale refresh on dump
        onLocaleChanged(context);

        LocaleConfigAdapter config = getCurrentLocaleConfig(context);

        Locale current = context.getResources().getConfiguration().getLocales().get(0);
        DecimalFormat nf = (DecimalFormat) android.icu.text.NumberFormat.getInstance(current);
        String[] digits = nf.getDecimalFormatSymbols().getDigitStrings();

        ipw.println("LocaleConfig:");
        ipw.println("  -- Prefixes");
        ipw.println("  .minusSignPrefix=" + config.minusSignPrefix);
        ipw.println("  .plusSignPrefix=" + config.plusSignPrefix);
        ipw.println("  -- Separators");
        ipw.println("  .decimalSeparator=" + config.decimalSeparator);
        ipw.println("  .groupingSeparator=" + config.groupingSeparator);
        ipw.println("  -- Grouping");
        ipw.println("  .groupSizePrimary=" + config.groupSizePrimary);
        ipw.println("  .groupSizeSecondary=" + config.groupSizeSecondary);
        ipw.println("  .groupSizeMin=" + config.groupSizeMin);
        ipw.println("  -- Digits");
        ipw.println("  .digits(Readable)=" + String.join(",", digits));
        ipw.println("  .digits(UTF-32 Hex)=" +
                config.digits.stream()
                        .map((v) -> String.format("0x%08X", v))
                        .collect(Collectors.joining(","))
        );
        ipw.println();
        ipw.println("Numeric Codepoints");
        ipw.println("  (UTF-32 Hex)=" +
                Arrays.stream(mNumericCodepoints)
                        .mapToObj((v) -> String.format("0x%08X", v))
                        .collect(Collectors.joining(","))
        );
        ipw.println();
    }

    static class LocaleConfigAdapter {
        public String plusSignPrefix;
        public String minusSignPrefix;
        public String decimalSeparator;
        public String groupingSeparator;
        public int groupSizePrimary;
        public int groupSizeSecondary;
        public int groupSizeMin;
        public ArrayList<Integer> digits;

        vendor.google_clockwork.displayoffload.V1_1.LocaleConfig getHalObject() {
            vendor.google_clockwork.displayoffload.V1_1.LocaleConfig localeConfig =
                    new vendor.google_clockwork.displayoffload.V1_1.LocaleConfig();
            localeConfig.plusSignPrefix = plusSignPrefix;
            localeConfig.minusSignPrefix = minusSignPrefix;
            localeConfig.decimalSeparator = decimalSeparator;
            localeConfig.groupingSeparator = groupingSeparator;
            localeConfig.groupSizePrimary = (byte) groupSizePrimary;
            localeConfig.groupSizeSecondary = (byte) groupSizeSecondary;
            localeConfig.groupSizeMin = (byte) groupSizeMin;
            localeConfig.digits = digits;

            return localeConfig;
        }
    }
}
