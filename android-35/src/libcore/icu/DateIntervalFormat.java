/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.compat.annotation.UnsupportedAppUsage;

import dalvik.annotation.compat.VersionCodes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Formatter;

/**
 * This class is only kept for @UnsupportedAppUsage, and should not be used by libcore / frameworks.
 *
 * @hide
 */
public final class DateIntervalFormat {

    private static final int FORMAT_SHOW_TIME = 0x00001;
    private static final int FORMAT_12HOUR = 0x00040;
    private static final int FORMAT_24HOUR = 0x00080;

    private DateIntervalFormat() {
    }

    /**
     * Do not use this method because it's only kept for app backward compatibility.
     * To format a date range, please use {@link android.icu.text.DateIntervalFormat} instead.
     *
     * @implNote best-effort implementation to invoke the frameworks' implementation. If it fails
     * when framework.jar is absent in the boot class path, it returns null instead of
     * throwing exception to avoid crash.
     *
     * @return null if the corresponding frameworks' method is not found.
     */
    @UnsupportedAppUsage(maxTargetSdk = VersionCodes.TIRAMISU,
            publicAlternatives = "Use {@code android.text.format.DateUtils#formatDateRange("
                    + "Context, Formatter, long, long, int, String)} instead.")
    public static String formatDateRange(long startMs, long endMs, int flags, String olsonId) {
        // First, try the internal method in android.text.format.DateIntervalFormat, which
        // can be modified by the OEMs.
        Method m = getFrameworksDateIntervaFormatMethod();
        if (m != null) {
            try {
                return (String) m.invoke(null, startMs, endMs, flags, olsonId);
            } catch (IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }

        // Second, try the public SDK method in android.text.format.DateUtils.
        m = getDateUtilsMethod();
        if (m != null) {
            try {
                // To pass null frameworks' Context pointer, pre-process the flags here.
                if ((flags & (FORMAT_SHOW_TIME | FORMAT_12HOUR | FORMAT_24HOUR))
                        == FORMAT_SHOW_TIME) {
                    // Make an arbitrary choice of using 24-hour format if is24Hour is null.
                    if (DateFormat.is24Hour == null || DateFormat.is24Hour) {
                        flags |= FORMAT_24HOUR;
                    } else {
                        flags |= FORMAT_12HOUR;
                    }
                }

                Formatter formatter = new Formatter();
                m.invoke(null, /*context="*/ null, formatter, startMs, endMs, flags, olsonId);
                return formatter.toString();
            } catch (IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }

        // If the above 2 attempts fail, it's likely that frameworks.jar is not present in the
        // boot class path, and no 3rd party app code is running in this process. It should be safe
        // to return null here.
        return null;
    }

    private static Method getFrameworksDateIntervaFormatMethod() {
        try {
            Class<?> cls = Class.forName("android.text.format.DateIntervalFormat");
            Method m = cls.getDeclaredMethod("formatDateRange", long.class, long.class, int.class,
                    String.class);
            m.setAccessible(true);
            return m;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        }
    }

    private static Method getDateUtilsMethod() {
        try {
            Class<?> cls = Class.forName("android.text.format.DateUtils");
            Method m = cls.getMethod("formatDateRange", Class.forName("android.content.Context"),
                    Formatter.class, long.class, long.class, int.class, String.class);
            return m;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        }
    }

}
