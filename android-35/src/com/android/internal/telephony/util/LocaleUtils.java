/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.util;

import android.content.Context;
import android.icu.util.ULocale;
import android.text.TextUtils;

import com.android.internal.telephony.MccTable;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This class provides various util functions about Locale.
 */
public class LocaleUtils {

    private static final String LOG_TAG = "LocaleUtils";

    /**
     * Get Locale based on the MCC of the SIM.
     *
     * @param context Context to act on.
     * @param mcc Mobile Country Code of the SIM or SIM-like entity (build prop on CDMA)
     * @param simLanguage (nullable) the language from the SIM records (if present).
     *
     * @return locale for the mcc or null if none
     */
    public static Locale getLocaleFromMcc(Context context, int mcc, String simLanguage) {
        boolean hasSimLanguage = !TextUtils.isEmpty(simLanguage);
        String language = hasSimLanguage ? simLanguage : defaultLanguageForMcc(mcc);
        String country = MccTable.countryCodeForMcc(mcc);

        Rlog.d(LOG_TAG, "getLocaleFromMcc(" + language + ", " + country + ", " + mcc);
        final Locale locale = getLocaleForLanguageCountry(context, language, country);

        // If we couldn't find a locale that matches the SIM language, give it a go again
        // with the "likely" language for the given country.
        if (locale == null && hasSimLanguage) {
            language = defaultLanguageForMcc(mcc);
            Rlog.d(LOG_TAG, "[retry ] getLocaleFromMcc(" + language + ", " + country + ", " + mcc);
            return getLocaleForLanguageCountry(context, language, country);
        }

        return locale;
    }

    /**
     * Return Locale for the language and country or null if no good match.
     *
     * @param context Context to act on.
     * @param language Two character language code desired
     * @param country Two character country code desired
     *
     * @return Locale or null if no appropriate value
     */
    private static Locale getLocaleForLanguageCountry(Context context, String language,
                                                      String country) {
        if (language == null) {
            Rlog.d(LOG_TAG, "getLocaleForLanguageCountry: skipping no language");
            return null; // no match possible
        }
        if (country == null) {
            country = ""; // The Locale constructor throws if passed null.
        }

        final Locale target = new Locale(language, country);
        try {
            String[] localeArray = context.getAssets().getLocales();
            List<String> locales = new ArrayList<>(Arrays.asList(localeArray));

            // Even in developer mode, you don't want the pseudolocales.
            locales.remove("ar-XB");
            locales.remove("en-XA");

            List<Locale> languageMatches = new ArrayList<>();
            for (String locale : locales) {
                final Locale l = Locale.forLanguageTag(locale.replace('_', '-'));

                // Only consider locales with both language and country.
                if (l == null || "und".equals(l.getLanguage())
                        || l.getLanguage().isEmpty() || l.getCountry().isEmpty()) {
                    continue;
                }
                if (l.getLanguage().equals(target.getLanguage())) {
                    // If we got a perfect match, we're done.
                    if (l.getCountry().equals(target.getCountry())) {
                        Rlog.d(LOG_TAG, "getLocaleForLanguageCountry: got perfect match: "
                                + l.toLanguageTag());
                        return l;
                    }

                    // We've only matched the language, not the country.
                    languageMatches.add(l);
                }
            }

            if (languageMatches.isEmpty()) {
                Rlog.d(LOG_TAG, "getLocaleForLanguageCountry: no locales for language " + language);
                return null;
            }

            Locale bestMatch = lookupFallback(target, languageMatches);
            if (bestMatch != null) {
                Rlog.d(LOG_TAG, "getLocaleForLanguageCountry: got a fallback match: "
                        + bestMatch.toLanguageTag());
                return bestMatch;
            } else {
                // If a locale is "translated", it is selectable in setup wizard, and can therefore
                // be considered a valid result for this method.
                if (!TextUtils.isEmpty(target.getCountry())) {
                    if (isTranslated(context, target)) {
                        Rlog.d(LOG_TAG, "getLocaleForLanguageCountry: "
                                + "target locale is translated: " + target);
                        return target;
                    }
                }

                // Somewhat arbitrarily take the first locale for the language,
                // unless we get a perfect match later. Note that these come back in no
                // particular order, so there's no reason to think the first match is
                // a particularly good match.
                Rlog.d(LOG_TAG, "getLocaleForLanguageCountry: got language-only match: "
                        + language);
                return languageMatches.get(0);
            }
        } catch (Exception e) {
            Rlog.d(LOG_TAG, "getLocaleForLanguageCountry: exception", e);
        }

        return null;
    }

    /**
     * Given a GSM Mobile Country Code, returns
     * an ISO 2-3 character language code if available.
     * Returns null if unavailable.
     */
    public static String defaultLanguageForMcc(int mcc) {
        MccTable.MccEntry entry = MccTable.entryForMcc(mcc);
        if (entry == null) {
            Rlog.d(LOG_TAG, "defaultLanguageForMcc(" + mcc + "): no country for mcc");
            return null;
        }

        final String country = entry.mIso;

        // Choose English as the default language for India.
        if ("in".equals(country)) {
            return "en";
        }

        // Ask CLDR for the language this country uses...
        ULocale likelyLocale = ULocale.addLikelySubtags(new ULocale("und", country));
        String likelyLanguage = likelyLocale.getLanguage();
        Rlog.d(LOG_TAG, "defaultLanguageForMcc(" + mcc + "): country " + country + " uses "
                + likelyLanguage);
        return likelyLanguage;
    }

    private static boolean isTranslated(Context context, Locale targetLocale) {
        ULocale fullTargetLocale = ULocale.addLikelySubtags(ULocale.forLocale(targetLocale));
        String language = fullTargetLocale.getLanguage();
        String script = fullTargetLocale.getScript();

        for (String localeId : context.getAssets().getLocales()) {
            ULocale fullLocale = ULocale.addLikelySubtags(new ULocale(localeId));
            if (language.equals(fullLocale.getLanguage())
                    && script.equals(fullLocale.getScript())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a suitable locale among {@code candidates} to use as the fallback locale for
     * {@code target}. This looks through the list of {@link MccTable#FALLBACKS},
     * and follows the chain until a locale in {@code candidates} is found.
     * This function assumes that {@code target} is not in {@code candidates}.
     *
     * TODO: This should really follow the CLDR chain of parent locales! That might be a bit
     * of a problem because we don't really have an en-001 locale on android.
     *
     * @return The fallback locale or {@code null} if there is no suitable fallback defined in the
     *         lookup.
     */
    private static Locale lookupFallback(Locale target, List<Locale> candidates) {
        Locale fallback = target;
        while ((fallback = MccTable.FALLBACKS.get(fallback)) != null) {
            if (candidates.contains(fallback)) {
                return fallback;
            }
        }

        return null;
    }
}
