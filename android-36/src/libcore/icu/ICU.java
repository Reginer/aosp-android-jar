/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.icu.lang.UCharacter;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.TimeZoneFormat;
import android.icu.util.Currency;
import android.icu.util.IllformedLocaleException;
import android.icu.util.ULocale;

import com.android.icu.util.ExtendedCalendar;
import com.android.icu.util.LocaleNative;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import libcore.util.BasicLruCache;

/**
 * Makes ICU data accessible to Java.
 * @hide
 */
public final class ICU {

  @UnsupportedAppUsage
  private static final BasicLruCache<String, String> CACHED_PATTERNS =
      new BasicLruCache<String, String>(8);

  private static volatile Locale[] availableLocalesCache;

  private static volatile String[] isoCountries;
  private static volatile Set<String> isoCountriesSet;

  private static volatile String[] isoLanguages;

  /**
   * Avoid initialization with many dependencies here, because when this is called,
   * lower-level classes, e.g. java.lang.System, are not initialized and java.lang.System
   * relies on getIcuVersion().
   */
  static {

  }

  private ICU() {
  }

  public static void initializeCacheInZygote() {
    // Fill CACHED_PATTERNS with the patterns from default locale and en-US initially.
    // This should be called in Zygote pre-fork process and the initial values in the cache
    // can be shared among app. The cache was filled by LocaleData in the older Android platform,
    // but moved here, due to an performance issue http://b/161846393.
    // It initializes 2 x 4 = 8 values in the CACHED_PATTERNS whose max size should be >= 8.
    for (Locale locale : new Locale[] {Locale.US, Locale.getDefault()}) {
      getTimePattern(locale, false, false);
      getTimePattern(locale, false, true);
      getTimePattern(locale, true, false);
      getTimePattern(locale, true, true);
    }
  }

  /**
   * Returns an array of two-letter ISO 639-1 language codes, either from ICU or our cache.
   */
  public static String[] getISOLanguages() {
    if (isoLanguages == null) {
      synchronized (ICU.class) {
        if (isoLanguages == null) {
          isoLanguages = getISOLanguagesNative();
        }
      }
    }
    return isoLanguages.clone();
  }

  /**
   * Returns an array of two-letter ISO 3166 country codes, either from ICU or our cache.
   */
  public static String[] getISOCountries() {
    return getISOCountriesInternal().clone();
  }

  /**
   * Returns true if the string is a 2-letter ISO 3166 country code.
   */
  public static boolean isIsoCountry(String country) {
    if (isoCountriesSet == null) {
      synchronized (ICU.class) {
        if (isoCountriesSet == null) {
          String[] isoCountries = getISOCountriesInternal();
          Set<String> newSet = new HashSet<>(isoCountries.length);
          for (String isoCountry : isoCountries) {
            newSet.add(isoCountry);
          }
          isoCountriesSet = newSet;
        }
      }
    }
    return country != null && isoCountriesSet.contains(country);
  }

  private static String[] getISOCountriesInternal() {
    if (isoCountries == null) {
      synchronized (ICU.class) {
        if (isoCountries == null) {
          isoCountries = getISOCountriesNative();
        }
      }
    }
    return isoCountries;
  }



  private static final int IDX_LANGUAGE = 0;
  private static final int IDX_SCRIPT = 1;
  private static final int IDX_REGION = 2;
  private static final int IDX_VARIANT = 3;

  /*
   * Parse the {Language, Script, Region, Variant*} section of the ICU locale
   * ID. This is the bit that appears before the keyword separate "@". The general
   * structure is a series of ASCII alphanumeric strings (subtags)
   * separated by underscores.
   *
   * Each subtag is interpreted according to its position in the list of subtags
   * AND its length (groan...). The various cases are explained in comments
   * below.
   */
  private static void parseLangScriptRegionAndVariants(String string,
          String[] outputArray) {
    final int first = string.indexOf('_');
    final int second = string.indexOf('_', first + 1);
    final int third = string.indexOf('_', second + 1);

    if (first == -1) {
      outputArray[IDX_LANGUAGE] = string;
    } else if (second == -1) {
      // Language and country ("ja_JP") OR
      // Language and script ("en_Latn") OR
      // Language and variant ("en_POSIX").

      outputArray[IDX_LANGUAGE] = string.substring(0, first);
      final String secondString = string.substring(first + 1);

      if (secondString.length() == 4) {
          // 4 Letter ISO script code.
          outputArray[IDX_SCRIPT] = secondString;
      } else if (secondString.length() == 2 || secondString.length() == 3) {
          // 2 or 3 Letter region code.
          outputArray[IDX_REGION] = secondString;
      } else {
          // If we're here, the length of the second half is either 1 or greater
          // than 5. Assume that ICU won't hand us malformed tags, and therefore
          // assume the rest of the string is a series of variant tags.
          outputArray[IDX_VARIANT] = secondString;
      }
    } else if (third == -1) {
      // Language and country and variant ("ja_JP_TRADITIONAL") OR
      // Language and script and variant ("en_Latn_POSIX") OR
      // Language and script and region ("en_Latn_US"). OR
      // Language and variant with multiple subtags ("en_POSIX_XISOP")

      outputArray[IDX_LANGUAGE] = string.substring(0, first);
      final String secondString = string.substring(first + 1, second);
      final String thirdString = string.substring(second + 1);

      if (secondString.length() == 4) {
          // The second subtag is a script.
          outputArray[IDX_SCRIPT] = secondString;

          // The third subtag can be either a region or a variant, depending
          // on its length.
          if (thirdString.length() == 2 || thirdString.length() == 3 ||
                  thirdString.isEmpty()) {
              outputArray[IDX_REGION] = thirdString;
          } else {
              outputArray[IDX_VARIANT] = thirdString;
          }
      } else if (secondString.isEmpty() ||
              secondString.length() == 2 || secondString.length() == 3) {
          // The second string is a region, and the third a variant.
          outputArray[IDX_REGION] = secondString;
          outputArray[IDX_VARIANT] = thirdString;
      } else {
          // Variant with multiple subtags.
          outputArray[IDX_VARIANT] = string.substring(first + 1);
      }
    } else {
      // Language, script, region and variant with 1 or more subtags
      // ("en_Latn_US_POSIX") OR
      // Language, region and variant with 2 or more subtags
      // (en_US_POSIX_VARIANT).
      outputArray[IDX_LANGUAGE] = string.substring(0, first);
      final String secondString = string.substring(first + 1, second);
      if (secondString.length() == 4) {
          outputArray[IDX_SCRIPT] = secondString;
          outputArray[IDX_REGION] = string.substring(second + 1, third);
          outputArray[IDX_VARIANT] = string.substring(third + 1);
      } else {
          outputArray[IDX_REGION] = secondString;
          outputArray[IDX_VARIANT] = string.substring(second + 1);
      }
    }
  }

  /**
   * Returns the appropriate {@code Locale} given a {@code String} of the form returned
   * by {@code toString}. This is very lenient, and doesn't care what's between the underscores:
   * this method can parse strings that {@code Locale.toString} won't produce.
   * Used to remove duplication.
   */
  public static Locale localeFromIcuLocaleId(String localeId) {
    // @ == ULOC_KEYWORD_SEPARATOR_UNICODE (uloc.h).
    final int extensionsIndex = localeId.indexOf('@');

    Map<Character, String> extensionsMap = Collections.EMPTY_MAP;
    Map<String, String> unicodeKeywordsMap = Collections.EMPTY_MAP;
    Set<String> unicodeAttributeSet = Collections.EMPTY_SET;

    if (extensionsIndex != -1) {
      extensionsMap = new HashMap<Character, String>();
      unicodeKeywordsMap = new HashMap<String, String>();
      unicodeAttributeSet = new HashSet<String>();

      // ICU sends us a semi-colon (ULOC_KEYWORD_ITEM_SEPARATOR) delimited string
      // containing all "keywords" it could parse. An ICU keyword is a key-value pair
      // separated by an "=" (ULOC_KEYWORD_ASSIGN).
      //
      // Each keyword item can be one of three things :
      // - A unicode extension attribute list: In this case the item key is "attribute"
      //   and the value is a hyphen separated list of unicode attributes.
      // - A unicode extension keyword: In this case, the item key will be larger than
      //   1 char in length, and the value will be the unicode extension value.
      // - A BCP-47 extension subtag: In this case, the item key will be exactly one
      //   char in length, and the value will be a sequence of unparsed subtags that
      //   represent the extension.
      //
      // Note that this implies that unicode extension keywords are "promoted" to
      // to the same namespace as the top level extension subtags and their values.
      // There can't be any collisions in practice because the BCP-47 spec imposes
      // restrictions on their lengths.
      final String extensionsString = localeId.substring(extensionsIndex + 1);
      final String[] extensions = extensionsString.split(";");
      for (String extension : extensions) {
        // This is the special key for the unicode attributes
        if (extension.startsWith("attribute=")) {
          String unicodeAttributeValues = extension.substring("attribute=".length());
          for (String unicodeAttribute : unicodeAttributeValues.split("-")) {
            unicodeAttributeSet.add(unicodeAttribute);
          }
        } else {
          final int separatorIndex = extension.indexOf('=');

          if (separatorIndex == 1) {
            // This is a BCP-47 extension subtag.
            final String value = extension.substring(2);
            final char extensionId = extension.charAt(0);

            extensionsMap.put(extensionId, value);
          } else {
            // This is a unicode extension keyword.
            unicodeKeywordsMap.put(extension.substring(0, separatorIndex),
            extension.substring(separatorIndex + 1));
          }
        }
      }
    }

    final String[] outputArray = new String[] { "", "", "", "" };
    if (extensionsIndex == -1) {
      parseLangScriptRegionAndVariants(localeId, outputArray);
    } else {
      parseLangScriptRegionAndVariants(localeId.substring(0, extensionsIndex),
          outputArray);
    }
    Locale.Builder builder = new Locale.Builder();
    builder.setLanguage(outputArray[IDX_LANGUAGE]);
    builder.setRegion(outputArray[IDX_REGION]);
    builder.setVariant(outputArray[IDX_VARIANT]);
    builder.setScript(outputArray[IDX_SCRIPT]);
    for (String attribute : unicodeAttributeSet) {
      builder.addUnicodeLocaleAttribute(attribute);
    }
    for (Entry<String, String> keyword : unicodeKeywordsMap.entrySet()) {
      builder.setUnicodeLocaleKeyword(keyword.getKey(), keyword.getValue());
    }

    for (Entry<Character, String> extension : extensionsMap.entrySet()) {
      builder.setExtension(extension.getKey(), extension.getValue());
    }

    return builder.build();
  }

  public static Locale[] localesFromStrings(String[] localeNames) {
    // We need to remove duplicates caused by the conversion of "he" to "iw", et cetera.
    // Java needs the obsolete code, ICU needs the modern code, but we let ICU know about
    // both so that we never need to convert back when talking to it.
    LinkedHashSet<Locale> set = new LinkedHashSet<Locale>();
    for (String localeName : localeNames) {
      set.add(localeFromIcuLocaleId(localeName));
    }
    return set.toArray(new Locale[set.size()]);
  }

  // This method returns availableLocalesCache array as-it-is. Do not leak it.
  private static Locale[] getAvailableLocalesInternal() {
    if (availableLocalesCache == null) {
      synchronized (ICU.class) {
        if (availableLocalesCache == null) {
          availableLocalesCache = localesFromStrings(getAvailableLocalesNative());
        }
      }
    }
    return availableLocalesCache;
  }

  public static Locale[] getAvailableLocales() {
    return getAvailableLocalesInternal().clone();
  }

  public static Stream<Locale> streamAvailableLocales() {
    return Arrays.stream(getAvailableLocalesInternal());
  }

  /**
   * Content of {@link #availableLocalesCache} depends on the USE_NEW_ISO_LOCALE_CODES flag value.
   * Resetting it so a following {@link #getAvailableLocales()} call will fill it with the right
   * values.
   */
  // VisibleForTesting
  public static void clearAvailableLocales() {
    availableLocalesCache = null;
  }

  /**
   * DO NOT USE this method directly.
   * Please use {@link SimpleDateFormatData.DateTimeFormatStringGenerator#getTimePattern}
   */
  /* package */ static String getTimePattern(Locale locale, boolean is24Hour, boolean withSecond) {
    final String skeleton;
    if (withSecond) {
      skeleton = is24Hour ? "Hms" : "hms";
    } else {
      skeleton = is24Hour ? "Hm" : "hm";
    }
    return getBestDateTimePattern(skeleton, locale);
  }
  /**
   * DO NOT USE this method directly.
   * Please use {@link SimpleDateFormatData.DateTimeFormatStringGenerator#getTimePattern}
   */
  @UnsupportedAppUsage
  public static String getBestDateTimePattern(String skeleton, Locale locale) {
    String languageTag = locale.toLanguageTag();
    String key = skeleton + "\t" + languageTag;
    synchronized (CACHED_PATTERNS) {
      String pattern = CACHED_PATTERNS.get(key);
      if (pattern == null) {
        pattern = getBestDateTimePattern0(skeleton, locale);
        CACHED_PATTERNS.put(key, pattern);
      }
      return pattern;
    }
  }

  private static String getBestDateTimePattern0(String skeleton, Locale locale) {
      DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance(locale);
      return dtpg.getBestPattern(skeleton);
  }

  @UnsupportedAppUsage
  private static String getBestDateTimePatternNative(String skeleton, String languageTag) {
    return getBestDateTimePattern0(skeleton, Locale.forLanguageTag(languageTag));
  }

  @UnsupportedAppUsage
  public static char[] getDateFormatOrder(String pattern) {
    char[] result = new char[3];
    int resultIndex = 0;
    boolean sawDay = false;
    boolean sawMonth = false;
    boolean sawYear = false;

    for (int i = 0; i < pattern.length(); ++i) {
      char ch = pattern.charAt(i);
      if (ch == 'd' || ch == 'L' || ch == 'M' || ch == 'y') {
        if (ch == 'd' && !sawDay) {
          result[resultIndex++] = 'd';
          sawDay = true;
        } else if ((ch == 'L' || ch == 'M') && !sawMonth) {
          result[resultIndex++] = 'M';
          sawMonth = true;
        } else if ((ch == 'y') && !sawYear) {
          result[resultIndex++] = 'y';
          sawYear = true;
        }
      } else if (ch == 'G') {
        // Ignore the era specifier, if present.
      } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
        throw new IllegalArgumentException("Bad pattern character '" + ch + "' in " + pattern);
      } else if (ch == '\'') {
        if (i < pattern.length() - 1 && pattern.charAt(i + 1) == '\'') {
          ++i;
        } else {
          i = pattern.indexOf('\'', i + 1);
          if (i == -1) {
            throw new IllegalArgumentException("Bad quoting in " + pattern);
          }
          ++i;
        }
      } else {
        // Ignore spaces and punctuation.
      }
    }
    return result;
  }

  /**
   * {@link java.time.format.DateTimeFormatter} does not handle some date symbols, e.g. 'B' / 'b',
   * and thus we use a heuristic algorithm to remove the symbol. See http://b/174804526.
   * See {@link #transformIcuDateTimePattern(String)} for documentation about the implementation.
   */
  public static String transformIcuDateTimePattern_forJavaTime(String pattern) {
    return transformIcuDateTimePattern(pattern, /* isJavaTime= */ true);
  }

  /**
   * {@link java.text.SimpleDateFormat} does not handle some date symbols, e.g. 'B' / 'b',
   * and simply ignore the symbol in formatting. Instead, we should avoid exposing the symbol
   * entirely in all public APIs, e.g. {@link java.text.SimpleDateFormat#toPattern()},
   * and thus we use a heuristic algorithm to remove the symbol. See http://b/174804526.
   * See {@link #transformIcuDateTimePattern(String)} for documentation about the implementation.
   */
  public static String transformIcuDateTimePattern_forJavaText(String pattern) {
    return transformIcuDateTimePattern(pattern,  /* isJavaTime= */ false);
  }

  /**
   * Rewrite the date/time pattern coming ICU to be consumed by libcore classes.
   * It's an ideal place to rewrite the pattern entirely when multiple symbols not digested
   * by libcore need to be removed/processed. Rewriting in single place could be more efficient
   * in a small or constant number of scans instead of scanning for every symbol.
   *
   * {@link LocaleData#initLocaleData(Locale)} also rewrites time format, but only a subset of
   * patterns. In the future, that should migrate to this function in order to handle the symbols
   * in one place, but now separate because java.text and java.time handles different sets of
   * symbols.
   */
  private static String transformIcuDateTimePattern(String pattern, boolean isJavaTime) {
    if (pattern == null) {
      return null;
    }

    pattern = transformSymbolB(pattern);

    if (isJavaTime) {
      // '#' is reserved for the future use in java.time, but it's treated as literal in CLDR.
      // It needs to be quoted for the usage in java.time.
      pattern = transformHashSign(pattern);
    }

    return pattern;
  }

  private static String transformHashSign(String pattern) {
    if (pattern.indexOf('#') == -1) {
      return pattern;
    }

    StringBuilder sb = new StringBuilder(pattern.length());
    boolean isInQuote = false;
    for (int i = 0; i < pattern.length(); i++) {
      char curr = pattern.charAt(i);
      if (isInQuote) {
        if (curr == '\'') {
          // e.g. '' represents a single quote literal or 'xyz' represents literal text.
          // This applies to both java.time and java.text date / time patterns.
          isInQuote = false;
        }
        sb.append(curr);
      } else if (curr == '#') {
        sb.append("'#'");
      } else {
         if (curr == '\'') {
           isInQuote = true;
        }
        sb.append(curr);
      }
    }
    return sb.toString();

  }

  private static String transformSymbolB(String pattern) {
    // For details about the different symbols, see
    // http://cldr.unicode.org/translation/date-time-1/date-time-patterns#TOC-Day-period-patterns
    // The symbols B means "Day periods with locale-specific ranges".
    // English example: 2:00 at night, 10:00 in the morning, 12:00 in the afternoon.
    boolean contains_B = pattern.indexOf('B') != -1;
    // AM, PM, noon and midnight. English example: 10:00 AM, 12:00 noon, 7:00 PM
    boolean contains_b = pattern.indexOf('b') != -1;

    if (!contains_B && !contains_b) {
      return pattern;
    }

    // Simply remove the symbol 'B' and 'b' if 24-hour 'H' exists because the 24-hour format
    // provides enough information and the day periods are optional. See http://b/174804526.
    // Don't handle symbol 'B'/'b' with 12-hour 'h' because it's much more complicated because
    // we likely need to replace 'B'/'b' with 'a' inserted into a new right position or use other
    // ways.
    if (pattern.indexOf('H') != -1) {
      return removeBFromDateTimePattern(pattern);
    }

    // Non-ideal workaround until http://b/68139386 is implemented.
    // This workaround may create a pattern that isn't usual / common for the language users.
    if (pattern.indexOf('h') != -1) {
      if (contains_b) {
        pattern = replaceSymbolInDatePattern(pattern, 'b', 'a');
      }
      if (contains_B) {
        pattern = replaceSymbolInDatePattern(pattern, 'B', 'a');
      }
    } // else {  } // not sure what to do as we assume that B is only useful when the hour is given.

    return pattern;
  }

  /**
   * Remove 'b' and 'B' from simple patterns, e.g. "B H:mm" and "dd-MM-yy B HH:mm:ss" only.
   */
  private static String removeBFromDateTimePattern(String pattern) {
    // The below implementation can likely be replaced by a regular expression via
    // String.replaceAll(). However, it's known that libcore's regex implementation is more
    // memory-intensive, and the below implementation is likely cheaper, but it's not yet measured.
    StringBuilder sb = new StringBuilder(pattern.length());
    char prev = ' '; // the initial value is not used.
    boolean isInQuote = false;
    for (int i = 0; i < pattern.length(); i++) {
      char curr = pattern.charAt(i);
      if (isInQuote) {
        if (curr == '\'') {
          // e.g. '' represents a single quote literal or 'xyz' represents literal text.
          // This applies to both java.time and java.text date / time patterns.
          isInQuote = false;
        }
        sb.append(curr);
        continue;
      }
      switch(curr) {
        case 'B':
        case 'b':
          // Ignore 'B' and 'b'
          break;
        case ' ': // Ascii whitespace
          // caveat: Ideally it's a case for all Unicode whitespaces by UCharacter.isUWhiteSpace(c)
          // but checking ascii whitespace only is enough for the CLDR data when this is written.
          if (i != 0 && (prev == 'B' || prev == 'b')) {
            // Ignore the whitespace behind the symbol 'B'/'b' because it's likely a whitespace to
            // separate the day period with the next text.
          } else {
            sb.append(curr);
          }
          break;
        case '\'':
          isInQuote = true;
          sb.append(curr);
          break;
        default:
          sb.append(curr);
          break;
      }
      prev = curr;
    }

    // Remove the trailing whitespace which is likely following the symbol 'B'/'b' in the original
    // pattern, e.g. "hh:mm B" (12:00 in the afternoon).
    int lastIndex = sb.length() - 1;
    if (lastIndex >= 0 && sb.charAt(lastIndex) == ' ') {
      sb.deleteCharAt(lastIndex);
    }
    return sb.toString();
  }


  private static String replaceSymbolInDatePattern(String pattern, char existingSymbol,
      char newSymbol) {
    if (pattern.indexOf('\'') == -1) {
      // Fast path if the pattern contains no quoted literals.
      return pattern.replace(existingSymbol, newSymbol);
    }

    StringBuilder sb = new StringBuilder(pattern.length());
    boolean isInQuote = false;
    for (int i = 0; i < pattern.length(); i++) {
      char curr = pattern.charAt(i);
      char modified;
      if (isInQuote) {
        if (curr == '\'') {
          // e.g. '' represents a single quote literal or 'xyz' represents literal text.
          // This applies to both java.time and java.text date / time patterns.
          isInQuote = false;
        }
        modified = curr;
      } else if (curr == '\'') {
        isInQuote = true;
        modified = curr;
      } else if (curr == existingSymbol) {
        modified = newSymbol;
      } else {
        modified = curr;
      }
      sb.append(modified);
    }
    return sb.toString();
  }

  /**
   * Returns the version of the CLDR data in use, such as "22.1.1".
   *
   */
  public static native String getCldrVersion();

  /**
   * Returns the icu4c version in use, such as "50.1.1".
   */
  public static native String getIcuVersion();

  /**
   * Returns the Unicode version our ICU supports, such as "6.2".
   */
  public static native String getUnicodeVersion();

  // --- Errors.

  // --- Native methods accessing ICU's database.

  private static native String[] getAvailableLocalesNative();

    /**
     * Query ICU for the currency being used in the country right now.
     * @param countryCode ISO 3166 two-letter country code
     * @return ISO 4217 3-letter currency code if found, otherwise null.
     */
  public static String getCurrencyCode(String countryCode) {
      // Fail fast when country code is not valid.
      if (countryCode == null || countryCode.length() == 0) {
          return null;
      }
      final ULocale countryLocale;
      try {
          countryLocale = new ULocale.Builder().setRegion(countryCode).build();
      } catch (IllformedLocaleException e) {
          return null; // Return null on invalid country code.
      }
      String[] isoCodes = Currency.getAvailableCurrencyCodes(countryLocale, new Date());
      if (isoCodes == null || isoCodes.length == 0) {
        return null;
      }
      return isoCodes[0];
  }


  public static native String getISO3Country(String languageTag);

  public static native String getISO3Language(String languageTag);

  /**
   * @deprecated Use {@link android.icu.util.ULocale#addLikelySubtags(ULocale)} instead.
   * The method is only kept for @UnsupportedAppUsage.
   */
  @UnsupportedAppUsage
  @Deprecated
  public static Locale addLikelySubtags(Locale locale) {
      return ULocale.addLikelySubtags(ULocale.forLocale(locale)).toLocale();
  }

  /**
   * @return ICU localeID
   * @deprecated Use {@link android.icu.util.ULocale#addLikelySubtags(ULocale)} instead.
   * The method is only kept for @UnsupportedAppUsage.
   */
  @UnsupportedAppUsage
  @Deprecated
  public static String addLikelySubtags(String locale) {
      return ULocale.addLikelySubtags(new ULocale(locale)).getName();
  }

  /**
   * @deprecated use {@link java.util.Locale#getScript()} instead. This has been kept
   *     around only for the support library.
   */
  @UnsupportedAppUsage
  @Deprecated
  public static native String getScript(String locale);

  private static native String[] getISOLanguagesNative();
  private static native String[] getISOCountriesNative();

  /**
   * Takes a BCP-47 language tag (Locale.toLanguageTag()). e.g. en-US, not en_US
   */
  public static void setDefaultLocale(String languageTag) {
    LocaleNative.setDefault(languageTag);
  }

  /**
   * Returns a locale name, not a BCP-47 language tag. e.g. en_US not en-US.
   */
  public static native String getDefaultLocale();


  /**
   * @param calendarType LDML-defined legacy calendar type. See keyTypeData.txt in ICU.
   */
  public static ExtendedCalendar getExtendedCalendar(Locale locale, String calendarType) {
      ULocale uLocale = ULocale.forLocale(locale)
              .setKeywordValue("calendar", calendarType);
      return ExtendedCalendar.getInstance(uLocale);
  }

  /**
   * Converts CLDR LDML short time zone id to an ID that can be recognized by
   * {@link java.util.TimeZone#getTimeZone(String)}.
   * @param cldrShortTzId
   * @return null if no tz id can be matched to the short id.
   */
  public static String convertToTzId(String cldrShortTzId) {
    if (cldrShortTzId == null) {
      return null;
    }
    String tzid = ULocale.toLegacyType("tz", cldrShortTzId);
    // ULocale.toLegacyType() returns the lower case of the input ID if it matches the spec, but
    // it's not a valid tz id.
    if (tzid == null || tzid.equals(cldrShortTzId.toLowerCase(Locale.ROOT))) {
      return null;
    }
    return tzid;
  }

  public static String getGMTZeroFormatString(Locale locale) {
    return TimeZoneFormat.getInstance(locale).getGMTZeroFormat();
  }

    /**
     * If {@link java.lang.Character} calls {@link UCharacter#hasBinaryProperty(int, int)} directly,
     * Dex2oatImageTest.TestExtension gtest fails. dex2oat fails to initialize the class because
     * class verification fails and returns kAccessChecksFailure error when creating
     * a boot image extension.
     * This method is created to avoid the class initialization and verification failure.
     * If this method creates any actual runtime circular dependency between {@link Character}
     * and {@link UCharacter#hasBinaryProperty(int, int)}, consider use the ICU4C API instead.
     * https://developer.android.com/ndk/reference/group/icu4c#u_hasbinaryproperty
     */
  public static boolean hasBinaryProperty(int ch, int property) {
      return UCharacter.hasBinaryProperty(ch, property);
  }

}
