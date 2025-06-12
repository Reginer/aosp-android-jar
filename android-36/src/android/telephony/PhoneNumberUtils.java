/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.telephony;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.sysprop.TelephonyProperties;
import android.telecom.PhoneAccount;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.SparseIntArray;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.internal.telephony.flags.Flags;
import com.android.telephony.Rlog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various utilities for dealing with phone number strings.
 */
public class PhoneNumberUtils {
    /** {@hide} */
    @IntDef(prefix = "BCD_EXTENDED_TYPE_", value = {
            BCD_EXTENDED_TYPE_EF_ADN,
            BCD_EXTENDED_TYPE_CALLED_PARTY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BcdExtendType {}

    /*
     * The BCD extended type used to determine the extended char for the digit which is greater than
     * 9.
     *
     * see TS 51.011 section 10.5.1 EF_ADN(Abbreviated dialling numbers)
     */
    public static final int BCD_EXTENDED_TYPE_EF_ADN = 1;

    /*
     * The BCD extended type used to determine the extended char for the digit which is greater than
     * 9.
     *
     * see TS 24.008 section 10.5.4.7 Called party BCD number
     */
    public static final int BCD_EXTENDED_TYPE_CALLED_PARTY = 2;

    /*
     * Special characters
     *
     * (See "What is a phone number?" doc)
     * 'p' --- GSM pause character, same as comma
     * 'n' --- GSM wild character
     * 'w' --- GSM wait character
     */
    public static final char PAUSE = ',';
    public static final char WAIT = ';';
    public static final char WILD = 'N';

    /*
     * Calling Line Identification Restriction (CLIR)
     */
    private static final String CLIR_ON = "*31#";
    private static final String CLIR_OFF = "#31#";

    /*
     * TOA = TON + NPI
     * See TS 24.008 section 10.5.4.7 for details.
     * These are the only really useful TOA values
     */
    public static final int TOA_International = 0x91;
    public static final int TOA_Unknown = 0x81;

    static final String LOG_TAG = "PhoneNumberUtils";
    private static final boolean DBG = false;

    private static final String BCD_EF_ADN_EXTENDED = "*#,N;";
    private static final String BCD_CALLED_PARTY_EXTENDED = "*#abc";

    private static final String PREFIX_WPS = "*272";

    // WPS prefix when CLIR is being activated for the call.
    private static final String PREFIX_WPS_CLIR_ACTIVATE = "*31#*272";

    // WPS prefix when CLIR is being deactivated for the call.
    private static final String PREFIX_WPS_CLIR_DEACTIVATE = "#31#*272";

    /*
     * global-phone-number = ["+"] 1*( DIGIT / written-sep )
     * written-sep         = ("-"/".")
     */
    private static final Pattern GLOBAL_PHONE_NUMBER_PATTERN =
            Pattern.compile("[\\+]?[0-9.-]+");

    /** True if c is ISO-LATIN characters 0-9 */
    public static boolean
    isISODigit (char c) {
        return c >= '0' && c <= '9';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # */
    public final static boolean
    is12Key(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD  */
    public final static boolean
    isDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD;
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , + (no WILD)  */
    public final static boolean
    isReallyDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD, WAIT, PAUSE   */
    public final static boolean
    isNonSeparator(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+'
                || c == WILD || c == WAIT || c == PAUSE;
    }

    /** This any anything to the right of this char is part of the
     *  post-dial string (eg this is PAUSE or WAIT)
     */
    public final static boolean
    isStartsPostDial (char c) {
        return c == PAUSE || c == WAIT;
    }

    private static boolean
    isPause (char c){
        return c == 'p'||c == 'P';
    }

    private static boolean
    isToneWait (char c){
        return c == 'w'||c == 'W';
    }

    private static int sMinMatch = 0;

    private static int getMinMatch() {
        if (sMinMatch == 0) {
            sMinMatch = Resources.getSystem().getInteger(
                    com.android.internal.R.integer.config_phonenumber_compare_min_match);
        }
        return sMinMatch;
    }

    /**
     * A Test API to get current sMinMatch.
     * @hide
     */
    @TestApi
    public static int getMinMatchForTest() {
        return getMinMatch();
    }

    /**
     * A Test API to set sMinMatch.
     * @hide
     */
    @TestApi
    public static void setMinMatchForTest(int minMatch) {
        sMinMatch = minMatch;
    }

    /** Returns true if ch is not dialable or alpha char */
    private static boolean isSeparator(char ch) {
        return !isDialable(ch) && !(('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z'));
    }

    /** Extracts the phone number from an Intent.
     *
     * @param intent the intent to get the number of
     * @param context a context to use for database access
     *
     * @return the phone number that would be called by the intent, or
     *         <code>null</code> if the number cannot be found.
     */
    public static String getNumberFromIntent(Intent intent, Context context) {
        String number = null;

        Uri uri = intent.getData();

        if (uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            return null;
        }

        if (scheme.equals("tel") || scheme.equals("sip")) {
            return uri.getSchemeSpecificPart();
        }

        if (context == null) {
            return null;
        }

        String type = intent.resolveType(context);
        String phoneColumn = null;

        // Correctly read out the phone entry based on requested provider
        final String authority = uri.getAuthority();
        if (Contacts.AUTHORITY.equals(authority)) {
            phoneColumn = Contacts.People.Phones.NUMBER;
        } else if (ContactsContract.AUTHORITY.equals(authority)) {
            phoneColumn = ContactsContract.CommonDataKinds.Phone.NUMBER;
        }

        Cursor c = null;
        try {
            c = context.getContentResolver().query(uri, new String[] { phoneColumn },
                    null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    number = c.getString(c.getColumnIndex(phoneColumn));
                }
            }
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "Error getting phone number.", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return number;
    }

    /** Extracts the network address portion and canonicalizes
     *  (filters out separators.)
     *  Network address portion is everything up to DTMF control digit
     *  separators (pause or wait), but without non-dialable characters.
     *
     *  Please note that the GSM wild character is allowed in the result.
     *  This must be resolved before dialing.
     *
     *  Returns null if phoneNumber == null
     */
    public static String
    extractNetworkPortion(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (fullwidth, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                ret.append(digit);
            } else if (c == '+') {
                // Allow '+' as first character or after CLIR MMI prefix
                String prefix = ret.toString();
                if (prefix.length() == 0 || prefix.equals(CLIR_ON) || prefix.equals(CLIR_OFF)) {
                    ret.append(c);
                }
            } else if (isDialable(c)) {
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        return ret.toString();
    }

    /**
     * Extracts the network address portion and canonicalize.
     *
     * This function is equivalent to extractNetworkPortion(), except
     * for allowing the PLUS character to occur at arbitrary positions
     * in the address portion, not just the first position.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static String extractNetworkPortionAlt(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);
        boolean haveSeenPlus = false;

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (c == '+') {
                if (haveSeenPlus) {
                    continue;
                }
                haveSeenPlus = true;
            }
            if (isDialable(c)) {
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        return ret.toString();
    }

    /**
     * Strips separators from a phone number string.
     * @param phoneNumber phone number to strip.
     * @return phone string stripped of separators.
     */
    public static String stripSeparators(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (fullwidth, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                ret.append(digit);
            } else if (isNonSeparator(c)) {
                ret.append(c);
            }
        }

        return ret.toString();
    }

    /**
     * Translates keypad letters to actual digits (e.g. 1-800-GOOG-411 will
     * become 1-800-4664-411), and then strips all separators (e.g. 1-800-4664-411 will become
     * 18004664411).
     *
     * @see #convertKeypadLettersToDigits(String)
     * @see #stripSeparators(String)
     *
     * @hide
     */
    public static String convertAndStrip(String phoneNumber) {
        return stripSeparators(convertKeypadLettersToDigits(phoneNumber));
    }

    /**
     * Converts pause and tonewait pause characters
     * to Android representation.
     * RFC 3601 says pause is 'p' and tonewait is 'w'.
     * @hide
     */
    @UnsupportedAppUsage
    public static String convertPreDial(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);

            if (isPause(c)) {
                c = PAUSE;
            } else if (isToneWait(c)) {
                c = WAIT;
            }
            ret.append(c);
        }
        return ret.toString();
    }

    /** or -1 if both are negative */
    static private int
    minPositive (int a, int b) {
        if (a >= 0 && b >= 0) {
            return (a < b) ? a : b;
        } else if (a >= 0) { /* && b < 0 */
            return a;
        } else if (b >= 0) { /* && a < 0 */
            return b;
        } else { /* a < 0 && b < 0 */
            return -1;
        }
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }
    /** index of the last character of the network portion
     *  (eg anything after is a post-dial string)
     */
    static private int
    indexOfLastNetworkChar(String a) {
        int pIndex, wIndex;
        int origLength;
        int trimIndex;

        origLength = a.length();

        pIndex = a.indexOf(PAUSE);
        wIndex = a.indexOf(WAIT);

        trimIndex = minPositive(pIndex, wIndex);

        if (trimIndex < 0) {
            return origLength - 1;
        } else {
            return trimIndex - 1;
        }
    }

    /**
     * Extracts the post-dial sequence of DTMF control digits, pauses, and
     * waits. Strips separators. This string may be empty, but will not be null
     * unless phoneNumber == null.
     *
     * Returns null if phoneNumber == null
     */

    public static String
    extractPostDialPortion(String phoneNumber) {
        if (phoneNumber == null) return null;

        int trimIndex;
        StringBuilder ret = new StringBuilder();

        trimIndex = indexOfLastNetworkChar (phoneNumber);

        for (int i = trimIndex + 1, s = phoneNumber.length()
                ; i < s; i++
        ) {
            char c = phoneNumber.charAt(i);
            if (isNonSeparator(c)) {
                ret.append(c);
            }
        }

        return ret.toString();
    }

    /**
     * Compare phone numbers a and b, return true if they're identical enough for caller ID purposes.
     * @deprecated use {@link #areSamePhoneNumber(String, String, String)} instead
     */
    @Deprecated
    public static boolean compare(String a, String b) {
        // We've used loose comparation at least Eclair, which may change in the future.

        return compare(a, b, false);
    }

    /**
     * Compare phone numbers a and b, and return true if they're identical
     * enough for caller ID purposes. Checks a resource to determine whether
     * to use a strict or loose comparison algorithm.
     * @deprecated use {@link #areSamePhoneNumber(String, String, String)} instead
     */
    @Deprecated
    public static boolean compare(Context context, String a, String b) {
        boolean useStrict = context.getResources().getBoolean(
               com.android.internal.R.bool.config_use_strict_phone_number_comparation);
        return compare(a, b, useStrict);
    }

    /**
     * @hide only for testing.
     */
    @UnsupportedAppUsage
    public static boolean compare(String a, String b, boolean useStrictComparation) {
        return (useStrictComparation ? compareStrictly(a, b) : compareLoosely(a, b));
    }

    /**
     * Compare phone numbers a and b, return true if they're identical
     * enough for caller ID purposes.
     *
     * - Compares from right to left
     * - requires minimum characters to match
     * - handles common trunk prefixes and international prefixes
     *   (basically, everything except the Russian trunk prefix)
     *
     * Note that this method does not return false even when the two phone numbers
     * are not exactly same; rather; we can call this method "similar()", not "equals()".
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean
    compareLoosely(String a, String b) {
        int ia, ib;
        int matched;
        int numNonDialableCharsInA = 0;
        int numNonDialableCharsInB = 0;
        int minMatch = getMinMatch();

        if (a == null || b == null) return a == b;

        if (a.length() == 0 || b.length() == 0) {
            return false;
        }

        ia = indexOfLastNetworkChar (a);
        ib = indexOfLastNetworkChar (b);
        matched = 0;

        while (ia >= 0 && ib >=0) {
            char ca, cb;
            boolean skipCmp = false;

            ca = a.charAt(ia);

            if (!isDialable(ca)) {
                ia--;
                skipCmp = true;
                numNonDialableCharsInA++;
            }

            cb = b.charAt(ib);

            if (!isDialable(cb)) {
                ib--;
                skipCmp = true;
                numNonDialableCharsInB++;
            }

            if (!skipCmp) {
                if (cb != ca && ca != WILD && cb != WILD) {
                    break;
                }
                ia--; ib--; matched++;
            }
        }

        if (matched < minMatch) {
            int effectiveALen = a.length() - numNonDialableCharsInA;
            int effectiveBLen = b.length() - numNonDialableCharsInB;


            // if the number of dialable chars in a and b match, but the matched chars < minMatch,
            // treat them as equal (i.e. 404-04 and 40404)
            if (effectiveALen == effectiveBLen && effectiveALen == matched) {
                return true;
            }

            return false;
        }

        // At least one string has matched completely;
        if (matched >= minMatch && (ia < 0 || ib < 0)) {
            return true;
        }

        /*
         * Now, what remains must be one of the following for a
         * match:
         *
         *  - a '+' on one and a '00' or a '011' on the other
         *  - a '0' on one and a (+,00)<country code> on the other
         *     (for this, a '0' and a '00' prefix would have succeeded above)
         */

        if (matchIntlPrefix(a, ia + 1)
            && matchIntlPrefix (b, ib +1)
        ) {
            return true;
        }

        if (matchTrunkPrefix(a, ia + 1)
            && matchIntlPrefixAndCC(b, ib +1)
        ) {
            return true;
        }

        if (matchTrunkPrefix(b, ib + 1)
            && matchIntlPrefixAndCC(a, ia +1)
        ) {
            return true;
        }

        return false;
    }

    /**
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean
    compareStrictly(String a, String b) {
        return compareStrictly(a, b, true);
    }

    /**
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean
    compareStrictly(String a, String b, boolean acceptInvalidCCCPrefix) {
        if (a == null || b == null) {
            return a == b;
        } else if (a.length() == 0 && b.length() == 0) {
            return false;
        }

        int forwardIndexA = 0;
        int forwardIndexB = 0;

        CountryCallingCodeAndNewIndex cccA =
            tryGetCountryCallingCodeAndNewIndex(a, acceptInvalidCCCPrefix);
        CountryCallingCodeAndNewIndex cccB =
            tryGetCountryCallingCodeAndNewIndex(b, acceptInvalidCCCPrefix);
        boolean bothHasCountryCallingCode = false;
        boolean okToIgnorePrefix = true;
        boolean trunkPrefixIsOmittedA = false;
        boolean trunkPrefixIsOmittedB = false;
        if (cccA != null && cccB != null) {
            if (cccA.countryCallingCode != cccB.countryCallingCode) {
                // Different Country Calling Code. Must be different phone number.
                return false;
            }
            // When both have ccc, do not ignore trunk prefix. Without this,
            // "+81123123" becomes same as "+810123123" (+81 == Japan)
            okToIgnorePrefix = false;
            bothHasCountryCallingCode = true;
            forwardIndexA = cccA.newIndex;
            forwardIndexB = cccB.newIndex;
        } else if (cccA == null && cccB == null) {
            // When both do not have ccc, do not ignore trunk prefix. Without this,
            // "123123" becomes same as "0123123"
            okToIgnorePrefix = false;
        } else {
            if (cccA != null) {
                forwardIndexA = cccA.newIndex;
            } else {
                int tmp = tryGetTrunkPrefixOmittedIndex(b, 0);
                if (tmp >= 0) {
                    forwardIndexA = tmp;
                    trunkPrefixIsOmittedA = true;
                }
            }
            if (cccB != null) {
                forwardIndexB = cccB.newIndex;
            } else {
                int tmp = tryGetTrunkPrefixOmittedIndex(b, 0);
                if (tmp >= 0) {
                    forwardIndexB = tmp;
                    trunkPrefixIsOmittedB = true;
                }
            }
        }

        int backwardIndexA = a.length() - 1;
        int backwardIndexB = b.length() - 1;
        while (backwardIndexA >= forwardIndexA && backwardIndexB >= forwardIndexB) {
            boolean skip_compare = false;
            final char chA = a.charAt(backwardIndexA);
            final char chB = b.charAt(backwardIndexB);
            if (isSeparator(chA)) {
                backwardIndexA--;
                skip_compare = true;
            }
            if (isSeparator(chB)) {
                backwardIndexB--;
                skip_compare = true;
            }

            if (!skip_compare) {
                if (chA != chB) {
                    return false;
                }
                backwardIndexA--;
                backwardIndexB--;
            }
        }

        if (okToIgnorePrefix) {
            if ((trunkPrefixIsOmittedA && forwardIndexA <= backwardIndexA) ||
                !checkPrefixIsIgnorable(a, forwardIndexA, backwardIndexA)) {
                if (acceptInvalidCCCPrefix) {
                    // Maybe the code handling the special case for Thailand makes the
                    // result garbled, so disable the code and try again.
                    // e.g. "16610001234" must equal to "6610001234", but with
                    //      Thailand-case handling code, they become equal to each other.
                    //
                    // Note: we select simplicity rather than adding some complicated
                    //       logic here for performance(like "checking whether remaining
                    //       numbers are just 66 or not"), assuming inputs are small
                    //       enough.
                    return compare(a, b, false);
                } else {
                    return false;
                }
            }
            if ((trunkPrefixIsOmittedB && forwardIndexB <= backwardIndexB) ||
                !checkPrefixIsIgnorable(b, forwardIndexA, backwardIndexB)) {
                if (acceptInvalidCCCPrefix) {
                    return compare(a, b, false);
                } else {
                    return false;
                }
            }
        } else {
            // In the US, 1-650-555-1234 must be equal to 650-555-1234,
            // while 090-1234-1234 must not be equal to 90-1234-1234 in Japan.
            // This request exists just in US (with 1 trunk (NDD) prefix).
            // In addition, "011 11 7005554141" must not equal to "+17005554141",
            // while "011 1 7005554141" must equal to "+17005554141"
            //
            // In this comparison, we ignore the prefix '1' just once, when
            // - at least either does not have CCC, or
            // - the remaining non-separator number is 1
            boolean maybeNamp = !bothHasCountryCallingCode;
            while (backwardIndexA >= forwardIndexA) {
                final char chA = a.charAt(backwardIndexA);
                if (isDialable(chA)) {
                    if (maybeNamp && tryGetISODigit(chA) == 1) {
                        maybeNamp = false;
                    } else {
                        return false;
                    }
                }
                backwardIndexA--;
            }
            while (backwardIndexB >= forwardIndexB) {
                final char chB = b.charAt(backwardIndexB);
                if (isDialable(chB)) {
                    if (maybeNamp && tryGetISODigit(chB) == 1) {
                        maybeNamp = false;
                    } else {
                        return false;
                    }
                }
                backwardIndexB--;
            }
        }

        return true;
    }

    /**
     * Returns the rightmost minimum matched characters in the network portion
     * in *reversed* order
     *
     * This can be used to do a database lookup against the column
     * that stores getStrippedReversed()
     *
     * Returns null if phoneNumber == null
     */
    public static String
    toCallerIDMinMatch(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);
        return internalGetStrippedReversed(np, getMinMatch());
    }

    /**
     * Returns the network portion reversed.
     * This string is intended to go into an index column for a
     * database lookup.
     *
     * Returns null if phoneNumber == null
     */
    public static String
    getStrippedReversed(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);

        if (np == null) return null;

        return internalGetStrippedReversed(np, np.length());
    }

    /**
     * Returns the last numDigits of the reversed phone number
     * Returns null if np == null
     */
    private static String
    internalGetStrippedReversed(String np, int numDigits) {
        if (np == null) return null;

        StringBuilder ret = new StringBuilder(numDigits);
        int length = np.length();

        for (int i = length - 1, s = length
            ; i >= 0 && (s - i) <= numDigits ; i--
        ) {
            char c = np.charAt(i);

            ret.append(c);
        }

        return ret.toString();
    }

    /**
     * Basically: makes sure there's a + in front of a
     * TOA_International number
     *
     * Returns null if s == null
     */
    public static String
    stringFromStringAndTOA(String s, int TOA) {
        if (s == null) return null;

        if (TOA == TOA_International && s.length() > 0 && s.charAt(0) != '+') {
            return "+" + s;
        }

        return s;
    }

    /**
     * Returns the TOA for the given dial string
     * Basically, returns TOA_International if there's a + prefix
     */

    public static int
    toaFromString(String s) {
        if (s != null && s.length() > 0 && s.charAt(0) == '+') {
            return TOA_International;
        }

        return TOA_Unknown;
    }

    /**
     *  3GPP TS 24.008 10.5.4.7
     *  Called Party BCD Number
     *
     *  See Also TS 51.011 10.5.1 "dialing number/ssc string"
     *  and TS 11.11 "10.3.1 EF adn (Abbreviated dialing numbers)"
     *
     * @param bytes the data buffer
     * @param offset should point to the TOA (aka. TON/NPI) octet after the length byte
     * @param length is the number of bytes including TOA byte
     *                and must be at least 2
     *
     * @return partial string on invalid decode
     *
     * @deprecated use {@link #calledPartyBCDToString(byte[], int, int, int)} instead. Calling this
     * method is equivalent to calling {@link #calledPartyBCDToString(byte[], int, int)} with
     * {@link #BCD_EXTENDED_TYPE_EF_ADN} as the extended type.
     */
    @Deprecated
    public static String calledPartyBCDToString(byte[] bytes, int offset, int length) {
        return calledPartyBCDToString(bytes, offset, length, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     *  3GPP TS 24.008 10.5.4.7
     *  Called Party BCD Number
     *
     *  See Also TS 51.011 10.5.1 "dialing number/ssc string"
     *  and TS 11.11 "10.3.1 EF adn (Abbreviated dialing numbers)"
     *
     * @param bytes the data buffer
     * @param offset should point to the TOA (aka. TON/NPI) octet after the length byte
     * @param length is the number of bytes including TOA byte
     *                and must be at least 2
     * @param bcdExtType used to determine the extended bcd coding
     * @see #BCD_EXTENDED_TYPE_EF_ADN
     * @see #BCD_EXTENDED_TYPE_CALLED_PARTY
     *
     */
    public static String calledPartyBCDToString(
            byte[] bytes, int offset, int length, @BcdExtendType int bcdExtType) {
        boolean prependPlus = false;
        StringBuilder ret = new StringBuilder(1 + length * 2);

        if (length < 2) {
            return "";
        }

        //Only TON field should be taken in consideration
        if ((bytes[offset] & 0xf0) == (TOA_International & 0xf0)) {
            prependPlus = true;
        }

        internalCalledPartyBCDFragmentToString(
                ret, bytes, offset + 1, length - 1, bcdExtType);

        if (prependPlus && ret.length() == 0) {
            // If the only thing there is a prepended plus, return ""
            return "";
        }

        if (prependPlus) {
            // This is an "international number" and should have
            // a plus prepended to the dialing number. But there
            // can also be GSM MMI codes as defined in TS 22.030 6.5.2
            // so we need to handle those also.
            //
            // http://web.telia.com/~u47904776/gsmkode.htm
            // has a nice list of some of these GSM codes.
            //
            // Examples are:
            //   **21*+886988171479#
            //   **21*8311234567#
            //   *21#
            //   #21#
            //   *#21#
            //   *31#+11234567890
            //   #31#+18311234567
            //   #31#8311234567
            //   18311234567
            //   +18311234567#
            //   +18311234567
            // Odd ball cases that some phones handled
            // where there is no dialing number so they
            // append the "+"
            //   *21#+
            //   **21#+
            String retString = ret.toString();
            Pattern p = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$");
            Matcher m = p.matcher(retString);
            if (m.matches()) {
                if ("".equals(m.group(2))) {
                    // Started with two [#*] ends with #
                    // So no dialing number and we'll just
                    // append a +, this handles **21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(3));
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                    ret.append("+");
                } else {
                    // Starts with [#*] and ends with #
                    // Assume group 4 is a dialing number
                    // such as *21*+1234554#
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                }
            } else {
                p = Pattern.compile("(^[#*])(.*)([#*])(.*)");
                m = p.matcher(retString);
                if (m.matches()) {
                    // Starts with [#*] and only one other [#*]
                    // Assume the data after last [#*] is dialing
                    // number (i.e. group 4) such as *31#+11234567890.
                    // This also includes the odd ball *21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                } else {
                    // Does NOT start with [#*] just prepend '+'
                    ret = new StringBuilder();
                    ret.append('+');
                    ret.append(retString);
                }
            }
        }

        return ret.toString();
    }

    private static void internalCalledPartyBCDFragmentToString(
            StringBuilder sb, byte [] bytes, int offset, int length,
            @BcdExtendType int bcdExtType) {
        for (int i = offset ; i < length + offset ; i++) {
            byte b;
            char c;

            c = bcdToChar((byte)(bytes[i] & 0xf), bcdExtType);

            if (c == 0) {
                return;
            }
            sb.append(c);

            // FIXME(mkf) TS 23.040 9.1.2.3 says
            // "if a mobile receives 1111 in a position prior to
            // the last semi-octet then processing shall commence with
            // the next semi-octet and the intervening
            // semi-octet shall be ignored"
            // How does this jive with 24.008 10.5.4.7

            b = (byte)((bytes[i] >> 4) & 0xf);

            if (b == 0xf && i + 1 == length + offset) {
                //ignore final 0xf
                break;
            }

            c = bcdToChar(b, bcdExtType);
            if (c == 0) {
                return;
            }

            sb.append(c);
        }

    }

    /**
     * Like calledPartyBCDToString, but field does not start with a
     * TOA byte. For example: SIM ADN extension fields
     *
     * @deprecated use {@link #calledPartyBCDFragmentToString(byte[], int, int, int)} instead.
     * Calling this method is equivalent to calling
     * {@link #calledPartyBCDFragmentToString(byte[], int, int, int)} with
     * {@link #BCD_EXTENDED_TYPE_EF_ADN} as the extended type.
     */
    @Deprecated
    public static String calledPartyBCDFragmentToString(byte[] bytes, int offset, int length) {
        return calledPartyBCDFragmentToString(bytes, offset, length, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Like calledPartyBCDToString, but field does not start with a
     * TOA byte. For example: SIM ADN extension fields
     */
    public static String calledPartyBCDFragmentToString(
            byte[] bytes, int offset, int length, @BcdExtendType int bcdExtType) {
        StringBuilder ret = new StringBuilder(length * 2);
        internalCalledPartyBCDFragmentToString(ret, bytes, offset, length, bcdExtType);
        return ret.toString();
    }

    /**
     * Returns the correspond character for given {@code b} based on {@code bcdExtType}, or 0 on
     * invalid code.
     */
    private static char bcdToChar(byte b, @BcdExtendType int bcdExtType) {
        if (b < 0xa) {
            return (char) ('0' + b);
        }

        String extended = null;
        if (BCD_EXTENDED_TYPE_EF_ADN == bcdExtType) {
            extended = BCD_EF_ADN_EXTENDED;
        } else if (BCD_EXTENDED_TYPE_CALLED_PARTY == bcdExtType) {
            extended = BCD_CALLED_PARTY_EXTENDED;
        }
        if (extended == null || b - 0xa >= extended.length()) {
            return 0;
        }

        return extended.charAt(b - 0xa);
    }

    private static int charToBCD(char c, @BcdExtendType int bcdExtType) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }

        String extended = null;
        if (BCD_EXTENDED_TYPE_EF_ADN == bcdExtType) {
            extended = BCD_EF_ADN_EXTENDED;
        } else if (BCD_EXTENDED_TYPE_CALLED_PARTY == bcdExtType) {
            extended = BCD_CALLED_PARTY_EXTENDED;
        }
        if (extended == null || extended.indexOf(c) == -1) {
            throw new RuntimeException("invalid char for BCD " + c);
        }
        return 0xa + extended.indexOf(c);
    }

    /**
     * Return true iff the network portion of <code>address</code> is,
     * as far as we can tell on the device, suitable for use as an SMS
     * destination address.
     */
    public static boolean isWellFormedSmsAddress(String address) {
        String networkPortion =
                PhoneNumberUtils.extractNetworkPortion(address);

        return (!(networkPortion.equals("+")
                  || TextUtils.isEmpty(networkPortion)))
               && isDialable(networkPortion);
    }

    public static boolean isGlobalPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }

        Matcher match = GLOBAL_PHONE_NUMBER_PATTERN.matcher(phoneNumber);
        return match.matches();
    }

    private static boolean isDialable(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isDialable(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNonSeparator(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isNonSeparator(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    /**
     * Note: calls extractNetworkPortion(), so do not use for
     * SIM EF[ADN] style records
     *
     * Returns null if network portion is empty.
     */
    public static byte[] networkPortionToCalledPartyBCD(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(
                networkPortion, false, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Same as {@link #networkPortionToCalledPartyBCD}, but includes a
     * one-byte length prefix.
     */
    public static byte[] networkPortionToCalledPartyBCDWithLength(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(
                networkPortion, true, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Convert a dialing number to BCD byte array
     *
     * @param number dialing number string. If the dialing number starts with '+', set to
     * international TOA
     *
     * @return BCD byte array
     *
     * @deprecated use {@link #numberToCalledPartyBCD(String, int)} instead. Calling this method
     * is equivalent to calling {@link #numberToCalledPartyBCD(String, int)} with
     * {@link #BCD_EXTENDED_TYPE_EF_ADN} as the extended type.
     */
    @Deprecated
    public static byte[] numberToCalledPartyBCD(String number) {
        return numberToCalledPartyBCD(number, BCD_EXTENDED_TYPE_EF_ADN);
    }

    /**
     * Convert a dialing number to BCD byte array
     *
     * @param number dialing number string. If the dialing number starts with '+', set to
     * international TOA
     * @param bcdExtType used to determine the extended bcd coding
     * @see #BCD_EXTENDED_TYPE_EF_ADN
     * @see #BCD_EXTENDED_TYPE_CALLED_PARTY
     *
     * @return BCD byte array
     */
    public static byte[] numberToCalledPartyBCD(String number, @BcdExtendType int bcdExtType) {
        return numberToCalledPartyBCDHelper(number, false, bcdExtType);
    }

    /**
     * If includeLength is true, prepend a one-byte length value to
     * the return array.
     */
    private static byte[] numberToCalledPartyBCDHelper(
            String number, boolean includeLength, @BcdExtendType int bcdExtType) {
        int numberLenReal = number.length();
        int numberLenEffective = numberLenReal;
        boolean hasPlus = number.indexOf('+') != -1;
        if (hasPlus) numberLenEffective--;

        if (numberLenEffective == 0) return null;

        int resultLen = (numberLenEffective + 1) / 2;  // Encoded numbers require only 4 bits each.
        int extraBytes = 1;                            // Prepended TOA byte.
        if (includeLength) extraBytes++;               // Optional prepended length byte.
        resultLen += extraBytes;

        byte[] result = new byte[resultLen];

        int digitCount = 0;
        for (int i = 0; i < numberLenReal; i++) {
            char c = number.charAt(i);
            if (c == '+') continue;
            int shift = ((digitCount & 0x01) == 1) ? 4 : 0;
            result[extraBytes + (digitCount >> 1)] |=
                    (byte)((charToBCD(c, bcdExtType) & 0x0F) << shift);
            digitCount++;
        }

        // 1-fill any trailing odd nibble/quartet.
        if ((digitCount & 0x01) == 1) result[extraBytes + (digitCount >> 1)] |= 0xF0;

        int offset = 0;
        if (includeLength) result[offset++] = (byte)(resultLen - 1);
        result[offset] = (byte)(hasPlus ? TOA_International : TOA_Unknown);

        return result;
    }

    //================ Number formatting =========================

    /** The current locale is unknown, look for a country code or don't format */
    public static final int FORMAT_UNKNOWN = 0;
    /** NANP formatting */
    public static final int FORMAT_NANP = 1;
    /** Japanese formatting */
    public static final int FORMAT_JAPAN = 2;

    /** List of country codes for countries that use the NANP */
    private static final String[] NANP_COUNTRIES = new String[] {
        "US", // United States
        "CA", // Canada
        "AS", // American Samoa
        "AI", // Anguilla
        "AG", // Antigua and Barbuda
        "BS", // Bahamas
        "BB", // Barbados
        "BM", // Bermuda
        "VG", // British Virgin Islands
        "KY", // Cayman Islands
        "DM", // Dominica
        "DO", // Dominican Republic
        "GD", // Grenada
        "GU", // Guam
        "JM", // Jamaica
        "PR", // Puerto Rico
        "MS", // Montserrat
        "MP", // Northern Mariana Islands
        "KN", // Saint Kitts and Nevis
        "LC", // Saint Lucia
        "VC", // Saint Vincent and the Grenadines
        "TT", // Trinidad and Tobago
        "TC", // Turks and Caicos Islands
        "VI", // U.S. Virgin Islands
    };

    private static final String KOREA_ISO_COUNTRY_CODE = "KR";

    private static final String JAPAN_ISO_COUNTRY_CODE = "JP";

    private static final String SINGAPORE_ISO_COUNTRY_CODE = "SG";

    private static final String[] COUNTRY_CODES_TO_FORMAT_NATIONALLY = new String[] {
            "KR", // Korea
            "JP", // Japan
            "SG", // Singapore
            "TW", // Taiwan
    };

    /**
     * Breaks the given number down and formats it according to the rules
     * for the country the number is from.
     *
     * @param source The phone number to format
     * @return A locally acceptable formatting of the input, or the raw input if
     *  formatting rules aren't known for the number
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static String formatNumber(String source) {
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        formatNumber(text, getFormatTypeForLocale(Locale.getDefault()));
        return text.toString();
    }

    /**
     * Formats the given number with the given formatting type. Currently
     * {@link #FORMAT_NANP} and {@link #FORMAT_JAPAN} are supported as a formating type.
     *
     * @param source the phone number to format
     * @param defaultFormattingType The default formatting rules to apply if the number does
     * not begin with +[country_code]
     * @return The phone number formatted with the given formatting type.
     *
     * @hide
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    @UnsupportedAppUsage
    public static String formatNumber(String source, int defaultFormattingType) {
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        formatNumber(text, defaultFormattingType);
        return text.toString();
    }

    /**
     * Returns the phone number formatting type for the given locale.
     *
     * @param locale The locale of interest, usually {@link Locale#getDefault()}
     * @return The formatting type for the given locale, or FORMAT_UNKNOWN if the formatting
     * rules are not known for the given locale
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static int getFormatTypeForLocale(Locale locale) {
        String country = locale.getCountry();

        return getFormatTypeFromCountryCode(country);
    }

    /**
     * Formats a phone number in-place. Currently {@link #FORMAT_JAPAN} and {@link #FORMAT_NANP}
     * is supported as a second argument.
     *
     * @param text The number to be formatted, will be modified with the formatting
     * @param defaultFormattingType The default formatting rules to apply if the number does
     * not begin with +[country_code]
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static void formatNumber(Editable text, int defaultFormattingType) {
        int formatType = defaultFormattingType;

        if (text.length() > 2 && text.charAt(0) == '+') {
            if (text.charAt(1) == '1') {
                formatType = FORMAT_NANP;
            } else if (text.length() >= 3 && text.charAt(1) == '8'
                && text.charAt(2) == '1') {
                formatType = FORMAT_JAPAN;
            } else {
                formatType = FORMAT_UNKNOWN;
            }
        }

        switch (formatType) {
            case FORMAT_NANP:
                formatNanpNumber(text);
                return;
            case FORMAT_JAPAN:
                formatJapaneseNumber(text);
                return;
            case FORMAT_UNKNOWN:
                removeDashes(text);
                return;
        }
    }

    private static final int NANP_STATE_DIGIT = 1;
    private static final int NANP_STATE_PLUS = 2;
    private static final int NANP_STATE_ONE = 3;
    private static final int NANP_STATE_DASH = 4;

    /**
     * Formats a phone number in-place using the NANP formatting rules. Numbers will be formatted
     * as:
     *
     * <p><code>
     * xxxxx
     * xxx-xxxx
     * xxx-xxx-xxxx
     * 1-xxx-xxx-xxxx
     * +1-xxx-xxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with the formatting
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static void formatNanpNumber(Editable text) {
        int length = text.length();
        if (length > "+1-nnn-nnn-nnnn".length()) {
            // The string is too long to be formatted
            return;
        } else if (length <= 5) {
            // The string is either a shortcode or too short to be formatted
            return;
        }

        CharSequence saved = text.subSequence(0, length);

        // Strip the dashes first, as we're going to add them back
        removeDashes(text);
        length = text.length();

        // When scanning the number we record where dashes need to be added,
        // if they're non-0 at the end of the scan the dashes will be added in
        // the proper places.
        int dashPositions[] = new int[3];
        int numDashes = 0;

        int state = NANP_STATE_DIGIT;
        int numDigits = 0;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '1':
                    if (numDigits == 0 || state == NANP_STATE_PLUS) {
                        state = NANP_STATE_ONE;
                        break;
                    }
                    // fall through
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                    if (state == NANP_STATE_PLUS) {
                        // Only NANP number supported for now
                        text.replace(0, length, saved);
                        return;
                    } else if (state == NANP_STATE_ONE) {
                        // Found either +1 or 1, follow it up with a dash
                        dashPositions[numDashes++] = i;
                    } else if (state != NANP_STATE_DASH && (numDigits == 3 || numDigits == 6)) {
                        // Found a digit that should be after a dash that isn't
                        dashPositions[numDashes++] = i;
                    }
                    state = NANP_STATE_DIGIT;
                    numDigits++;
                    break;

                case '-':
                    state = NANP_STATE_DASH;
                    break;

                case '+':
                    if (i == 0) {
                        // Plus is only allowed as the first character
                        state = NANP_STATE_PLUS;
                        break;
                    }
                    // Fall through
                default:
                    // Unknown character, bail on formatting
                    text.replace(0, length, saved);
                    return;
            }
        }

        if (numDigits == 7) {
            // With 7 digits we want xxx-xxxx, not xxx-xxx-x
            numDashes--;
        }

        // Actually put the dashes in place
        for (int i = 0; i < numDashes; i++) {
            int pos = dashPositions[i];
            text.replace(pos + i, pos + i, "-");
        }

        // Remove trailing dashes
        int len = text.length();
        while (len > 0) {
            if (text.charAt(len - 1) == '-') {
                text.delete(len - 1, len);
                len--;
            } else {
                break;
            }
        }
    }

    /**
     * Formats a phone number in-place using the Japanese formatting rules.
     * Numbers will be formatted as:
     *
     * <p><code>
     * 03-xxxx-xxxx
     * 090-xxxx-xxxx
     * 0120-xxx-xxx
     * +81-3-xxxx-xxxx
     * +81-90-xxxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with
     * the formatting
     *
     * @deprecated Use link #formatNumber(String phoneNumber, String defaultCountryIso) instead
     */
    @Deprecated
    public static void formatJapaneseNumber(Editable text) {
        JapanesePhoneNumberFormatter.format(text);
    }

    /**
     * Removes all dashes from the number.
     *
     * @param text the number to clear from dashes
     */
    private static void removeDashes(Editable text) {
        int p = 0;
        while (p < text.length()) {
            if (text.charAt(p) == '-') {
                text.delete(p, p + 1);
           } else {
                p++;
           }
        }
    }

    /**
     * Formats the specified {@code phoneNumber} to the E.164 representation.
     *
     * @param phoneNumber the phone number to format.
     * @param defaultCountryIso the ISO 3166-1 two letters country code in UPPER CASE.
     * @return the E.164 representation, or null if the given phone number is not valid.
     */
    public static String formatNumberToE164(String phoneNumber, String defaultCountryIso) {
        if (defaultCountryIso != null) {
            defaultCountryIso = defaultCountryIso.toUpperCase(Locale.ROOT);
        }

        return formatNumberInternal(phoneNumber, defaultCountryIso, PhoneNumberFormat.E164);
    }

    /**
     * Formats the specified {@code phoneNumber} to the RFC3966 representation.
     *
     * @param phoneNumber the phone number to format.
     * @param defaultCountryIso the ISO 3166-1 two letters country code in UPPER CASE.
     * @return the RFC3966 representation, or null if the given phone number is not valid.
     */
    public static String formatNumberToRFC3966(String phoneNumber, String defaultCountryIso) {
        if (defaultCountryIso != null) {
            defaultCountryIso = defaultCountryIso.toUpperCase(Locale.ROOT);
        }

        return formatNumberInternal(phoneNumber, defaultCountryIso, PhoneNumberFormat.RFC3966);
    }

    /**
     * Formats the raw phone number (string) using the specified {@code formatIdentifier}.
     * <p>
     * The given phone number must have an area code and could have a country code.
     * <p>
     * The defaultCountryIso is used to validate the given number and generate the formatted number
     * if the specified number doesn't have a country code.
     *
     * @param rawPhoneNumber The phone number to format.
     * @param defaultCountryIso The ISO 3166-1 two letters country code.
     * @param formatIdentifier The (enum) identifier of the desired format.
     * @return the formatted representation, or null if the specified number is not valid.
     */
    private static String formatNumberInternal(
            String rawPhoneNumber, String defaultCountryIso, PhoneNumberFormat formatIdentifier) {

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        try {
            PhoneNumber phoneNumber = util.parse(rawPhoneNumber, defaultCountryIso);
            if (util.isValidNumber(phoneNumber)) {
                return util.format(phoneNumber, formatIdentifier);
            }
        } catch (NumberParseException ignored) { }

        return null;
    }

    /**
     * Determines if a {@param phoneNumber} is international if dialed from
     * {@param defaultCountryIso}.
     *
     * @param phoneNumber The phone number.
     * @param defaultCountryIso The current country ISO.
     * @return {@code true} if the number is international, {@code false} otherwise.
     * @hide
     */
    public static boolean isInternationalNumber(String phoneNumber, String defaultCountryIso) {
        // If no phone number is provided, it can't be international.
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }

        // If it starts with # or * its not international.
        if (phoneNumber.startsWith("#") || phoneNumber.startsWith("*")) {
            return false;
        }

        if (defaultCountryIso != null) {
            defaultCountryIso = defaultCountryIso.toUpperCase(Locale.ROOT);
        }

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        try {
            PhoneNumber pn = util.parseAndKeepRawInput(phoneNumber, defaultCountryIso);
            return pn.getCountryCode() != util.getCountryCodeForRegion(defaultCountryIso);
        } catch (NumberParseException e) {
            return false;
        }
    }

    /**
     * Format a phone number.
     * <p>
     * If the given number doesn't have the country code, the phone will be
     * formatted to the default country's convention.
     *
     * @param phoneNumber
     *            the number to be formatted.
     * @param defaultCountryIso
     *            the ISO 3166-1 two letters country code whose convention will
     *            be used if the given number doesn't have the country code.
     * @return the formatted number, or null if the given number is not valid.
     */
    public static String formatNumber(String phoneNumber, String defaultCountryIso) {
        // Do not attempt to format numbers that start with a hash or star symbol.
        if (phoneNumber.startsWith("#") || phoneNumber.startsWith("*")) {
            return phoneNumber;
        }

        if (defaultCountryIso != null) {
            defaultCountryIso = defaultCountryIso.toUpperCase(Locale.ROOT);
        }

        Rlog.v(LOG_TAG, "formatNumber: defaultCountryIso: " + defaultCountryIso);

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        String result = null;
        try {
            PhoneNumber pn = util.parseAndKeepRawInput(phoneNumber, defaultCountryIso);

            if (Flags.nationalCountryCodeFormattingForLocalCalls()) {
                if (Arrays.asList(COUNTRY_CODES_TO_FORMAT_NATIONALLY).contains(defaultCountryIso)
                        && pn.getCountryCode() == util.getCountryCodeForRegion(defaultCountryIso)
                        && pn.getCountryCodeSource()
                        == PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN) {
                    return util.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                } else {
                    return util.formatInOriginalFormat(pn, defaultCountryIso);
                }
            } else {
                if (KOREA_ISO_COUNTRY_CODE.equalsIgnoreCase(defaultCountryIso) && (
                        pn.getCountryCode() == util.getCountryCodeForRegion(KOREA_ISO_COUNTRY_CODE))
                        && (pn.getCountryCodeSource()
                        == PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)) {
                    /**
                     * Need to reformat any local Korean phone numbers (when the user is in
                     * Korea) with country code to corresponding national format which would
                     * replace the leading +82 with 0.
                     */
                    result = util.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                } else if (JAPAN_ISO_COUNTRY_CODE.equalsIgnoreCase(defaultCountryIso)
                        && pn.getCountryCode() == util.getCountryCodeForRegion(
                        JAPAN_ISO_COUNTRY_CODE) && (pn.getCountryCodeSource()
                        == PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)) {
                    /**
                     * Need to reformat Japanese phone numbers (when user is in Japan) with the
                     * national dialing format.
                     */
                    result = util.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                } else if (Flags.removeCountryCodeFromLocalSingaporeCalls() && (
                        SINGAPORE_ISO_COUNTRY_CODE.equalsIgnoreCase(defaultCountryIso)
                                && pn.getCountryCode() == util.getCountryCodeForRegion(
                                SINGAPORE_ISO_COUNTRY_CODE) && (pn.getCountryCodeSource()
                                == PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN))) {
                    /*
                     * Need to reformat Singaporean phone numbers (when the user is in Singapore)
                     * with the country code (+65) removed to comply with Singaporean regulations.
                     */
                    result = util.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                } else {
                    result = util.formatInOriginalFormat(pn, defaultCountryIso);
                }
            }
        } catch (NumberParseException e) {
            if (DBG) log("formatNumber: NumberParseException caught " + e);
        }
        return result;
    }

    /**
     * Format the phone number only if the given number hasn't been formatted.
     * <p>
     * The number which has only dailable character is treated as not being
     * formatted.
     *
     * @param phoneNumber
     *            the number to be formatted.
     * @param phoneNumberE164
     *            the E164 format number whose country code is used if the given
     *            phoneNumber doesn't have the country code.
     * @param defaultCountryIso
     *            the ISO 3166-1 two letters country code whose convention will
     *            be used if the phoneNumberE164 is null or invalid, or if phoneNumber
     *            contains IDD.
     * @return the formatted number if the given number has been formatted,
     *            otherwise, return the given number.
     */
    public static String formatNumber(
            String phoneNumber, String phoneNumberE164, String defaultCountryIso) {
        if (defaultCountryIso != null) {
            defaultCountryIso = defaultCountryIso.toUpperCase(Locale.ROOT);
        }

        int len = phoneNumber.length();
        for (int i = 0; i < len; i++) {
            if (!isDialable(phoneNumber.charAt(i))) {
                return phoneNumber;
            }
        }
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        // Get the country code from phoneNumberE164
        if (phoneNumberE164 != null && phoneNumberE164.length() >= 2
                && phoneNumberE164.charAt(0) == '+') {
            try {
                // The number to be parsed is in E164 format, so the default region used doesn't
                // matter.
                PhoneNumber pn = util.parse(phoneNumberE164, "ZZ");
                String regionCode = util.getRegionCodeForNumber(pn);
                if (!TextUtils.isEmpty(regionCode) &&
                    // This makes sure phoneNumber doesn't contain an IDD
                    normalizeNumber(phoneNumber).indexOf(phoneNumberE164.substring(1)) <= 0) {
                    defaultCountryIso = regionCode;
                }
            } catch (NumberParseException e) {
            }
        }
        String result = formatNumber(phoneNumber, defaultCountryIso);
        return result != null ? result : phoneNumber;
    }

    /**
     * Normalize a phone number by removing the characters other than digits. If
     * the given number has keypad letters, the letters will be converted to
     * digits first.
     *
     * @param phoneNumber the number to be normalized.
     * @return the normalized number.
     */
    public static String normalizeNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int len = phoneNumber.length();
        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (fullwidth, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                sb.append(digit);
            } else if (sb.length() == 0 && c == '+') {
                sb.append(c);
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return normalizeNumber(PhoneNumberUtils.convertKeypadLettersToDigits(phoneNumber));
            }
        }
        return sb.toString();
    }

    /**
     * Replaces all unicode(e.g. Arabic, Persian) digits with their decimal digit equivalents.
     *
     * @param number the number to perform the replacement on.
     * @return the replaced number.
     */
    public static String replaceUnicodeDigits(String number) {
        StringBuilder normalizedDigits = new StringBuilder(number.length());
        for (char c : number.toCharArray()) {
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                normalizedDigits.append(digit);
            } else {
                normalizedDigits.append(c);
            }
        }
        return normalizedDigits.toString();
    }

    /**
     * Checks a given number against the list of
     * emergency numbers provided by the RIL and SIM card.
     *
     * @param number the number to look up.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, otherwise return false.
     *
     * @deprecated Please use {@link TelephonyManager#isEmergencyNumber(String)} instead.
     */
    @Deprecated
    public static boolean isEmergencyNumber(String number) {
        return isEmergencyNumber(getDefaultVoiceSubId(), number);
    }

    /**
     * Checks a given number against the list of
     * emergency numbers provided by the RIL and SIM card.
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is in the list of emergency numbers
     *         listed in the RIL / SIM, otherwise return false.
     *
     * @deprecated Please use {@link TelephonyManager#isEmergencyNumber(String)}
     *             instead.
     *
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage
    public static boolean isEmergencyNumber(int subId, String number) {
        // Return true only if the specified number *exactly* matches
        // one of the emergency numbers listed by the RIL / SIM.
        return isEmergencyNumberInternal(subId, number);
    }

    /**
     * Helper function for isEmergencyNumber(String, String) and.
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is an emergency number for the specified country.
     * @hide
     */
    private static boolean isEmergencyNumberInternal(int subId, String number) {
        //TODO: remove subid later. Keep it for now in case we need it later.
        try {
                return TelephonyManager.getDefault().isEmergencyNumber(number);
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "isEmergencyNumberInternal: RuntimeException: " + ex);
        }
        return false;
    }

    /**
     * Checks if a given number is an emergency number for the country that the user is in.
     *
     * @param number the number to look up.
     * @param context the specific context which the number should be checked against
     * @return true if the specified number is an emergency number for the country the user
     * is currently in.
     *
     * @deprecated Please use {@link TelephonyManager#isEmergencyNumber(String)}
     *             instead.
     */
    @Deprecated
    public static boolean isLocalEmergencyNumber(Context context, String number) {
        return isEmergencyNumberInternal(getDefaultVoiceSubId(), number);
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     */
    public static boolean isVoiceMailNumber(String number) {
        return isVoiceMailNumber(SubscriptionManager.getDefaultSubscriptionId(), number);
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     * @hide
     */
    public static boolean isVoiceMailNumber(int subId, String number) {
        return isVoiceMailNumber(null, subId, number);
    }

    /**
     * isVoiceMailNumber: checks a given number against the voicemail
     *   number provided by the RIL and SIM card. The caller must have
     *   the READ_PHONE_STATE credential.
     *
     * @param context {@link Context}.
     * @param subId the subscription id of the SIM.
     * @param number the number to look up.
     * @return true if the number is in the list of voicemail. False
     * otherwise, including if the caller does not have the permission
     * to read the VM number.
     * @hide
     */
    @SystemApi
    public static boolean isVoiceMailNumber(@NonNull Context context, int subId,
            @Nullable String number) {
        String vmNumber, mdn;
        try {
            final TelephonyManager tm;
            if (context == null) {
                tm = TelephonyManager.getDefault();
                if (DBG) log("isVoiceMailNumber: default tm");
            } else {
                tm = TelephonyManager.from(context);
                if (DBG) log("isVoiceMailNumber: tm from context");
            }
            vmNumber = tm.getVoiceMailNumber(subId);
            mdn = tm.getLine1Number(subId);
            if (DBG) log("isVoiceMailNumber: mdn=" + mdn + ", vmNumber=" + vmNumber
                    + ", number=" + number);
        } catch (SecurityException ex) {
            if (DBG) log("isVoiceMailNumber: SecurityExcpetion caught");
            return false;
        }
        // Strip the separators from the number before comparing it
        // to the list.
        number = extractNetworkPortionAlt(number);
        if (TextUtils.isEmpty(number)) {
            if (DBG) log("isVoiceMailNumber: number is empty after stripping");
            return false;
        }

        // check if the carrier considers MDN to be an additional voicemail number
        boolean compareWithMdn = false;
        if (context != null) {
            CarrierConfigManager configManager = (CarrierConfigManager)
                    context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager != null) {
                PersistableBundle b = configManager.getConfigForSubId(subId);
                if (b != null) {
                    compareWithMdn = b.getBoolean(CarrierConfigManager.
                            KEY_MDN_IS_ADDITIONAL_VOICEMAIL_NUMBER_BOOL);
                    if (DBG) log("isVoiceMailNumber: compareWithMdn=" + compareWithMdn);
                }
            }
        }

        if (compareWithMdn) {
            if (DBG) log("isVoiceMailNumber: treating mdn as additional vm number");
            return compare(number, vmNumber) || compare(number, mdn);
        } else {
            if (DBG) log("isVoiceMailNumber: returning regular compare");
            return compare(number, vmNumber);
        }
    }

    /**
     * Translates any alphabetic letters (i.e. [A-Za-z]) in the
     * specified phone number into the equivalent numeric digits,
     * according to the phone keypad letter mapping described in
     * ITU E.161 and ISO/IEC 9995-8.
     *
     * @return the input string, with alpha letters converted to numeric
     *         digits using the phone keypad letter mapping.  For example,
     *         an input of "1-800-GOOG-411" will return "1-800-4664-411".
     */
    public static String convertKeypadLettersToDigits(String input) {
        if (input == null) {
            return input;
        }
        int len = input.length();
        if (len == 0) {
            return input;
        }

        char[] out = input.toCharArray();

        for (int i = 0; i < len; i++) {
            char c = out[i];
            // If this char isn't in KEYPAD_MAP at all, just leave it alone.
            out[i] = (char) KEYPAD_MAP.get(c, c);
        }

        return new String(out);
    }

    /**
     * The phone keypad letter mapping (see ITU E.161 or ISO/IEC 9995-8.)
     * TODO: This should come from a resource.
     */
    private static final SparseIntArray KEYPAD_MAP = new SparseIntArray();
    static {
        KEYPAD_MAP.put('a', '2'); KEYPAD_MAP.put('b', '2'); KEYPAD_MAP.put('c', '2');
        KEYPAD_MAP.put('A', '2'); KEYPAD_MAP.put('B', '2'); KEYPAD_MAP.put('C', '2');

        KEYPAD_MAP.put('d', '3'); KEYPAD_MAP.put('e', '3'); KEYPAD_MAP.put('f', '3');
        KEYPAD_MAP.put('D', '3'); KEYPAD_MAP.put('E', '3'); KEYPAD_MAP.put('F', '3');

        KEYPAD_MAP.put('g', '4'); KEYPAD_MAP.put('h', '4'); KEYPAD_MAP.put('i', '4');
        KEYPAD_MAP.put('G', '4'); KEYPAD_MAP.put('H', '4'); KEYPAD_MAP.put('I', '4');

        KEYPAD_MAP.put('j', '5'); KEYPAD_MAP.put('k', '5'); KEYPAD_MAP.put('l', '5');
        KEYPAD_MAP.put('J', '5'); KEYPAD_MAP.put('K', '5'); KEYPAD_MAP.put('L', '5');

        KEYPAD_MAP.put('m', '6'); KEYPAD_MAP.put('n', '6'); KEYPAD_MAP.put('o', '6');
        KEYPAD_MAP.put('M', '6'); KEYPAD_MAP.put('N', '6'); KEYPAD_MAP.put('O', '6');

        KEYPAD_MAP.put('p', '7'); KEYPAD_MAP.put('q', '7'); KEYPAD_MAP.put('r', '7'); KEYPAD_MAP.put('s', '7');
        KEYPAD_MAP.put('P', '7'); KEYPAD_MAP.put('Q', '7'); KEYPAD_MAP.put('R', '7'); KEYPAD_MAP.put('S', '7');

        KEYPAD_MAP.put('t', '8'); KEYPAD_MAP.put('u', '8'); KEYPAD_MAP.put('v', '8');
        KEYPAD_MAP.put('T', '8'); KEYPAD_MAP.put('U', '8'); KEYPAD_MAP.put('V', '8');

        KEYPAD_MAP.put('w', '9'); KEYPAD_MAP.put('x', '9'); KEYPAD_MAP.put('y', '9'); KEYPAD_MAP.put('z', '9');
        KEYPAD_MAP.put('W', '9'); KEYPAD_MAP.put('X', '9'); KEYPAD_MAP.put('Y', '9'); KEYPAD_MAP.put('Z', '9');
    }

    //================ Plus Code formatting =========================
    private static final char PLUS_SIGN_CHAR = '+';
    private static final String PLUS_SIGN_STRING = "+";
    private static final String NANP_IDP_STRING = "011";
    private static final int NANP_LENGTH = 10;

    /**
     * This function checks if there is a plus sign (+) in the passed-in dialing number.
     * If there is, it processes the plus sign based on the default telephone
     * numbering plan of the system when the phone is activated and the current
     * telephone numbering plan of the system that the phone is camped on.
     * Currently, we only support the case that the default and current telephone
     * numbering plans are North American Numbering Plan(NANP).
     *
     * The passed-in dialStr should only contain the valid format as described below,
     * 1) the 1st character in the dialStr should be one of the really dialable
     *    characters listed below
     *    ISO-LATIN characters 0-9, *, # , +
     * 2) the dialStr should already strip out the separator characters,
     *    every character in the dialStr should be one of the non separator characters
     *    listed below
     *    ISO-LATIN characters 0-9, *, # , +, WILD, WAIT, PAUSE
     *
     * Otherwise, this function returns the dial string passed in
     *
     * @param dialStr the original dial string
     * @return the converted dial string if the current/default countries belong to NANP,
     * and if there is the "+" in the original dial string. Otherwise, the original dial
     * string returns.
     *
     * This API is for CDMA only
     *
     * @hide TODO: pending API Council approval
     */
    @UnsupportedAppUsage
    public static String cdmaCheckAndProcessPlusCode(String dialStr) {
        if (!TextUtils.isEmpty(dialStr)) {
            if (isReallyDialable(dialStr.charAt(0)) &&
                isNonSeparator(dialStr)) {
                String currIso = TelephonyManager.getDefault().getNetworkCountryIso();
                String defaultIso = TelephonyManager.getDefault().getSimCountryIso();
                if (!TextUtils.isEmpty(currIso) && !TextUtils.isEmpty(defaultIso)) {
                    return cdmaCheckAndProcessPlusCodeByNumberFormat(dialStr,
                            getFormatTypeFromCountryCode(currIso),
                            getFormatTypeFromCountryCode(defaultIso));
                }
            }
        }
        return dialStr;
    }

    /**
     * Process phone number for CDMA, converting plus code using the home network number format.
     * This is used for outgoing SMS messages.
     *
     * @param dialStr the original dial string
     * @return the converted dial string
     * @hide for internal use
     */
    public static String cdmaCheckAndProcessPlusCodeForSms(String dialStr) {
        if (!TextUtils.isEmpty(dialStr)) {
            if (isReallyDialable(dialStr.charAt(0)) && isNonSeparator(dialStr)) {
                String defaultIso = TelephonyManager.getDefault().getSimCountryIso();
                if (!TextUtils.isEmpty(defaultIso)) {
                    int format = getFormatTypeFromCountryCode(defaultIso);
                    return cdmaCheckAndProcessPlusCodeByNumberFormat(dialStr, format, format);
                }
            }
        }
        return dialStr;
    }

    /**
     * This function should be called from checkAndProcessPlusCode only
     * And it is used for test purpose also.
     *
     * It checks the dial string by looping through the network portion,
     * post dial portion 1, post dial porting 2, etc. If there is any
     * plus sign, then process the plus sign.
     * Currently, this function supports the plus sign conversion within NANP only.
     * Specifically, it handles the plus sign in the following ways:
     * 1)+1NANP,remove +, e.g.
     *   +18475797000 is converted to 18475797000,
     * 2)+NANP or +non-NANP Numbers,replace + with the current NANP IDP, e.g,
     *   +8475797000 is converted to 0118475797000,
     *   +11875767800 is converted to 01111875767800
     * 3)+1NANP in post dial string(s), e.g.
     *   8475797000;+18475231753 is converted to 8475797000;18475231753
     *
     *
     * @param dialStr the original dial string
     * @param currFormat the numbering system of the current country that the phone is camped on
     * @param defaultFormat the numbering system of the country that the phone is activated on
     * @return the converted dial string if the current/default countries belong to NANP,
     * and if there is the "+" in the original dial string. Otherwise, the original dial
     * string returns.
     *
     * @hide
     */
    public static String
    cdmaCheckAndProcessPlusCodeByNumberFormat(String dialStr,int currFormat,int defaultFormat) {
        String retStr = dialStr;

        boolean useNanp = (currFormat == defaultFormat) && (currFormat == FORMAT_NANP);

        // Checks if the plus sign character is in the passed-in dial string
        if (dialStr != null &&
            dialStr.lastIndexOf(PLUS_SIGN_STRING) != -1) {

            // Handle case where default and current telephone numbering plans are NANP.
            String postDialStr = null;
            String tempDialStr = dialStr;

            // Sets the retStr to null since the conversion will be performed below.
            retStr = null;
            if (DBG) log("checkAndProcessPlusCode,dialStr=" + dialStr);
            // This routine is to process the plus sign in the dial string by loop through
            // the network portion, post dial portion 1, post dial portion 2... etc. if
            // applied
            do {
                String networkDialStr;
                // Format the string based on the rules for the country the number is from,
                // and the current country the phone is camped
                if (useNanp) {
                    networkDialStr = extractNetworkPortion(tempDialStr);
                } else  {
                    networkDialStr = extractNetworkPortionAlt(tempDialStr);

                }

                networkDialStr = processPlusCode(networkDialStr, useNanp);

                // Concatenates the string that is converted from network portion
                if (!TextUtils.isEmpty(networkDialStr)) {
                    if (retStr == null) {
                        retStr = networkDialStr;
                    } else {
                        retStr = retStr.concat(networkDialStr);
                    }
                } else {
                    // This should never happen since we checked the if dialStr is null
                    // and if it contains the plus sign in the beginning of this function.
                    // The plus sign is part of the network portion.
                    Rlog.e("checkAndProcessPlusCode: null newDialStr", networkDialStr);
                    return dialStr;
                }
                postDialStr = extractPostDialPortion(tempDialStr);
                if (!TextUtils.isEmpty(postDialStr)) {
                    int dialableIndex = findDialableIndexFromPostDialStr(postDialStr);

                    // dialableIndex should always be greater than 0
                    if (dialableIndex >= 1) {
                        retStr = appendPwCharBackToOrigDialStr(dialableIndex,
                                 retStr,postDialStr);
                        // Skips the P/W character, extracts the dialable portion
                        tempDialStr = postDialStr.substring(dialableIndex);
                    } else {
                        // Non-dialable character such as P/W should not be at the end of
                        // the dial string after P/W processing in GsmCdmaConnection.java
                        // Set the postDialStr to "" to break out of the loop
                        if (dialableIndex < 0) {
                            postDialStr = "";
                        }
                        Rlog.e("wrong postDialStr=", postDialStr);
                    }
                }
                if (DBG) log("checkAndProcessPlusCode,postDialStr=" + postDialStr);
            } while (!TextUtils.isEmpty(postDialStr) && !TextUtils.isEmpty(tempDialStr));
        }
        return retStr;
    }

    /**
     * Wrap the supplied {@code CharSequence} with a {@code TtsSpan}, annotating it as
     * containing a phone number in its entirety.
     *
     * @param phoneNumber A {@code CharSequence} the entirety of which represents a phone number.
     * @return A {@code CharSequence} with appropriate annotations.
     */
    public static CharSequence createTtsSpannable(CharSequence phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(phoneNumber);
        PhoneNumberUtils.addTtsSpan(spannable, 0, spannable.length());
        return spannable;
    }

    /**
     * Attach a {@link TtsSpan} to the supplied {@code Spannable} at the indicated location,
     * annotating that location as containing a phone number.
     *
     * @param s A {@code Spannable} to annotate.
     * @param start The starting character position of the phone number in {@code s}.
     * @param endExclusive The position after the ending character in the phone number {@code s}.
     */
    public static void addTtsSpan(Spannable s, int start, int endExclusive) {
        s.setSpan(createTtsSpan(s.subSequence(start, endExclusive).toString()),
                start,
                endExclusive,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Wrap the supplied {@code CharSequence} with a {@code TtsSpan}, annotating it as
     * containing a phone number in its entirety.
     *
     * @param phoneNumber A {@code CharSequence} the entirety of which represents a phone number.
     * @return A {@code CharSequence} with appropriate annotations.
     * @deprecated Renamed {@link #createTtsSpannable}.
     *
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage
    public static CharSequence ttsSpanAsPhoneNumber(CharSequence phoneNumber) {
        return createTtsSpannable(phoneNumber);
    }

    /**
     * Attach a {@link TtsSpan} to the supplied {@code Spannable} at the indicated location,
     * annotating that location as containing a phone number.
     *
     * @param s A {@code Spannable} to annotate.
     * @param start The starting character position of the phone number in {@code s}.
     * @param end The ending character position of the phone number in {@code s}.
     *
     * @deprecated Renamed {@link #addTtsSpan}.
     *
     * @hide
     */
    @Deprecated
    public static void ttsSpanAsPhoneNumber(Spannable s, int start, int end) {
        addTtsSpan(s, start, end);
    }

    /**
     * Create a {@code TtsSpan} for the supplied {@code String}.
     *
     * @param phoneNumberString A {@code String} the entirety of which represents a phone number.
     * @return A {@code TtsSpan} for {@param phoneNumberString}.
     */
    public static TtsSpan createTtsSpan(String phoneNumberString) {
        if (phoneNumberString == null) {
            return null;
        }

        // Parse the phone number
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        PhoneNumber phoneNumber = null;
        try {
            // Don't supply a defaultRegion so this fails for non-international numbers because
            // we don't want to TalkBalk to read a country code (e.g. +1) if it is not already
            // present
            phoneNumber = phoneNumberUtil.parse(phoneNumberString, /* defaultRegion */ null);
        } catch (NumberParseException ignored) {
        }

        // Build a telephone tts span
        final TtsSpan.TelephoneBuilder builder = new TtsSpan.TelephoneBuilder();
        if (phoneNumber == null) {
            // Strip separators otherwise TalkBack will be silent
            // (this behavior was observed with TalkBalk 4.0.2 from their alpha channel)
            builder.setNumberParts(splitAtNonNumerics(phoneNumberString));
        } else {
            if (phoneNumber.hasCountryCode()) {
                builder.setCountryCode(Integer.toString(phoneNumber.getCountryCode()));
            }
            builder.setNumberParts(Long.toString(phoneNumber.getNationalNumber()));
        }
        return builder.build();
    }

    // Split a phone number like "+20(123)-456#" using spaces, ignoring anything that is not
    // a digit or the characters * and #, to produce a result like "20 123 456#".
    private static String splitAtNonNumerics(CharSequence number) {
        StringBuilder sb = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            sb.append(PhoneNumberUtils.is12Key(number.charAt(i))
                    ? number.charAt(i)
                    : " ");
        }
        // It is very important to remove extra spaces. At time of writing, any leading or trailing
        // spaces, or any sequence of more than one space, will confuse TalkBack and cause the TTS
        // span to be non-functional!
        return sb.toString().replaceAll(" +", " ").trim();
    }

    private static String getCurrentIdp(boolean useNanp) {
        String ps = null;
        if (useNanp) {
            ps = NANP_IDP_STRING;
        } else {
            // in case, there is no IDD is found, we shouldn't convert it.
            ps = TelephonyProperties.operator_idp_string().orElse(PLUS_SIGN_STRING);
        }
        return ps;
    }

    private static boolean isTwoToNine (char c) {
        if (c >= '2' && c <= '9') {
            return true;
        } else {
            return false;
        }
    }

    private static int getFormatTypeFromCountryCode (String country) {
        // Check for the NANP countries
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(country) == 0) {
                return FORMAT_NANP;
            }
        }
        if ("jp".compareToIgnoreCase(country) == 0) {
            return FORMAT_JAPAN;
        }
        return FORMAT_UNKNOWN;
    }

    /**
     * This function checks if the passed in string conforms to the NANP format
     * i.e. NXX-NXX-XXXX, N is any digit 2-9 and X is any digit 0-9
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean isNanp (String dialStr) {
        boolean retVal = false;
        if (dialStr != null) {
            if (dialStr.length() == NANP_LENGTH) {
                if (isTwoToNine(dialStr.charAt(0)) &&
                    isTwoToNine(dialStr.charAt(3))) {
                    retVal = true;
                    for (int i=1; i<NANP_LENGTH; i++ ) {
                        char c=dialStr.charAt(i);
                        if (!PhoneNumberUtils.isISODigit(c)) {
                            retVal = false;
                            break;
                        }
                    }
                }
            }
        } else {
            Rlog.e("isNanp: null dialStr passed in", dialStr);
        }
        return retVal;
    }

   /**
    * This function checks if the passed in string conforms to 1-NANP format
    */
    private static boolean isOneNanp(String dialStr) {
        boolean retVal = false;
        if (dialStr != null) {
            String newDialStr = dialStr.substring(1);
            if ((dialStr.charAt(0) == '1') && isNanp(newDialStr)) {
                retVal = true;
            }
        } else {
            Rlog.e("isOneNanp: null dialStr passed in", dialStr);
        }
        return retVal;
    }

    /**
     * Determines if the specified number is actually a URI
     * (i.e. a SIP address) rather than a regular PSTN phone number,
     * based on whether or not the number contains an "@" character.
     *
     * @hide
     * @param number
     * @return true if number contains @
     */
    @SystemApi
    public static boolean isUriNumber(@Nullable String number) {
        // Note we allow either "@" or "%40" to indicate a URI, in case
        // the passed-in string is URI-escaped.  (Neither "@" nor "%40"
        // will ever be found in a legal PSTN number.)
        return number != null && (number.contains("@") || number.contains("%40"));
    }

    /**
     * @return the "username" part of the specified SIP address,
     *         i.e. the part before the "@" character (or "%40").
     *
     * @param number SIP address of the form "username@domainname"
     *               (or the URI-escaped equivalent "username%40domainname")
     * @see #isUriNumber
     *
     * @hide
     */
    @SystemApi
    public static @NonNull String getUsernameFromUriNumber(@NonNull String number) {
        // The delimiter between username and domain name can be
        // either "@" or "%40" (the URI-escaped equivalent.)
        int delimiterIndex = number.indexOf('@');
        if (delimiterIndex < 0) {
            delimiterIndex = number.indexOf("%40");
        }
        if (delimiterIndex < 0) {
            Rlog.w(LOG_TAG,
                  "getUsernameFromUriNumber: no delimiter found in SIP addr '" + number + "'");
            delimiterIndex = number.length();
        }
        return number.substring(0, delimiterIndex);
    }

    /**
     * Given a {@link Uri} with a {@code sip} scheme, attempts to build an equivalent {@code tel}
     * scheme {@link Uri}.  If the source {@link Uri} does not contain a valid number, or is not
     * using the {@code sip} scheme, the original {@link Uri} is returned.
     *
     * @param source The {@link Uri} to convert.
     * @return The equivalent {@code tel} scheme {@link Uri}.
     *
     * @hide
     */
    public static Uri convertSipUriToTelUri(Uri source) {
        // A valid SIP uri has the format: sip:user:password@host:port;uri-parameters?headers
        // Per RFC3261, the "user" can be a telephone number.
        // For example: sip:1650555121;phone-context=blah.com@host.com
        // In this case, the phone number is in the user field of the URI, and the parameters can be
        // ignored.
        //
        // A SIP URI can also specify a phone number in a format similar to:
        // sip:+1-212-555-1212@something.com;user=phone
        // In this case, the phone number is again in user field and the parameters can be ignored.
        // We can get the user field in these instances by splitting the string on the @, ;, or :
        // and looking at the first found item.

        String scheme = source.getScheme();

        if (!PhoneAccount.SCHEME_SIP.equals(scheme)) {
            // Not a sip URI, bail.
            return source;
        }

        String number = source.getSchemeSpecificPart();
        String numberParts[] = number.split("[@;:]");

        if (numberParts.length == 0) {
            // Number not found, bail.
            return source;
        }
        number = numberParts[0];

        return Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
    }

    /**
     * This function handles the plus code conversion
     * If the number format is
     * 1)+1NANP,remove +,
     * 2)other than +1NANP, any + numbers,replace + with the current IDP
     */
    private static String processPlusCode(String networkDialStr, boolean useNanp) {
        String retStr = networkDialStr;

        if (DBG) log("processPlusCode, networkDialStr = " + networkDialStr
                + "for NANP = " + useNanp);
        // If there is a plus sign at the beginning of the dial string,
        // Convert the plus sign to the default IDP since it's an international number
        if (networkDialStr != null &&
            networkDialStr.charAt(0) == PLUS_SIGN_CHAR &&
            networkDialStr.length() > 1) {
            String newStr = networkDialStr.substring(1);
            // TODO: for nonNanp, should the '+' be removed if following number is country code
            if (useNanp && isOneNanp(newStr)) {
                // Remove the leading plus sign
                retStr = newStr;
            } else {
                // Replaces the plus sign with the default IDP
                retStr = networkDialStr.replaceFirst("[+]", getCurrentIdp(useNanp));
            }
        }
        if (DBG) log("processPlusCode, retStr=" + retStr);
        return retStr;
    }

    // This function finds the index of the dialable character(s)
    // in the post dial string
    private static int findDialableIndexFromPostDialStr(String postDialStr) {
        for (int index = 0;index < postDialStr.length();index++) {
             char c = postDialStr.charAt(index);
             if (isReallyDialable(c)) {
                return index;
             }
        }
        return -1;
    }

    // This function appends the non-dialable P/W character to the original
    // dial string based on the dialable index passed in
    private static String
    appendPwCharBackToOrigDialStr(int dialableIndex,String origStr, String dialStr) {
        String retStr;

        // There is only 1 P/W character before the dialable characters
        if (dialableIndex == 1) {
            StringBuilder ret = new StringBuilder(origStr);
            ret = ret.append(dialStr.charAt(0));
            retStr = ret.toString();
        } else {
            // It means more than 1 P/W characters in the post dial string,
            // appends to retStr
            String nonDigitStr = dialStr.substring(0,dialableIndex);
            retStr = origStr.concat(nonDigitStr);
        }
        return retStr;
    }

    //===== Beginning of utility methods used in compareLoosely() =====

    /**
     * Phone numbers are stored in "lookup" form in the database
     * as reversed strings to allow for caller ID lookup
     *
     * This method takes a phone number and makes a valid SQL "LIKE"
     * string that will match the lookup form
     *
     */
    /** all of a up to len must be an international prefix or
     *  separators/non-dialing digits
     */
    private static boolean
    matchIntlPrefix(String a, int len) {
        /* '([^0-9*#+pwn]\+[^0-9*#+pwn] | [^0-9*#+pwn]0(0|11)[^0-9*#+pwn] )$' */
        /*        0       1                           2 3 45               */

        int state = 0;
        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                break;

                default:
                    if (isNonSeparator(c)) return false;
                break;

            }
        }

        return state == 1 || state == 3 || state == 5;
    }

    /** all of 'a' up to len must be a (+|00|011)country code)
     *  We're fast and loose with the country code. Any \d{1,3} matches */
    private static boolean
    matchIntlPrefixAndCC(String a, int len) {
        /*  [^0-9*#+pwn]*(\+|0(0|11)\d\d?\d? [^0-9*#+pwn] $ */
        /*      0          1 2 3 45  6 7  8                 */

        int state = 0;
        for (int i = 0 ; i < len ; i++ ) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                break;

                case 1:
                case 3:
                case 5:
                    if      (isISODigit(c)) state = 6;
                    else if (isNonSeparator(c)) return false;
                break;

                case 6:
                case 7:
                    if      (isISODigit(c)) state++;
                    else if (isNonSeparator(c)) return false;
                break;

                default:
                    if (isNonSeparator(c)) return false;
            }
        }

        return state == 6 || state == 7 || state == 8;
    }

    /** all of 'a' up to len must match non-US trunk prefix ('0') */
    private static boolean
    matchTrunkPrefix(String a, int len) {
        boolean found;

        found = false;

        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            if (c == '0' && !found) {
                found = true;
            } else if (isNonSeparator(c)) {
                return false;
            }
        }

        return found;
    }

    //===== End of utility methods used only in compareLoosely() =====

    //===== Beginning of utility methods used only in compareStrictly() ====

    /*
     * If true, the number is country calling code.
     */
    private static final boolean COUNTRY_CALLING_CALL[] = {
        true, true, false, false, false, false, false, true, false, false,
        false, false, false, false, false, false, false, false, false, false,
        true, false, false, false, false, false, false, true, true, false,
        true, true, true, true, true, false, true, false, false, true,
        true, false, false, true, true, true, true, true, true, true,
        false, true, true, true, true, true, true, true, true, false,
        true, true, true, true, true, true, true, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, true, true, true, true, false, true, false, false, true,
        true, true, true, true, true, true, false, false, true, false,
    };
    private static final int CCC_LENGTH = COUNTRY_CALLING_CALL.length;

    /**
     * @return true when input is valid Country Calling Code.
     */
    private static boolean isCountryCallingCode(int countryCallingCodeCandidate) {
        return countryCallingCodeCandidate > 0 && countryCallingCodeCandidate < CCC_LENGTH &&
                COUNTRY_CALLING_CALL[countryCallingCodeCandidate];
    }

    /**
     * Returns integer corresponding to the input if input "ch" is
     * ISO-LATIN characters 0-9.
     * Returns -1 otherwise
     */
    private static int tryGetISODigit(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        } else {
            return -1;
        }
    }

    private static class CountryCallingCodeAndNewIndex {
        public final int countryCallingCode;
        public final int newIndex;
        public CountryCallingCodeAndNewIndex(int countryCode, int newIndex) {
            this.countryCallingCode = countryCode;
            this.newIndex = newIndex;
        }
    }

    /*
     * Note that this function does not strictly care the country calling code with
     * 3 length (like Morocco: +212), assuming it is enough to use the first two
     * digit to compare two phone numbers.
     */
    private static CountryCallingCodeAndNewIndex tryGetCountryCallingCodeAndNewIndex(
        String str, boolean acceptThailandCase) {
        // Rough regexp:
        //  ^[^0-9*#+]*((\+|0(0|11)\d\d?|166) [^0-9*#+] $
        //         0        1 2 3 45  6 7  89
        //
        // In all the states, this function ignores separator characters.
        // "166" is the special case for the call from Thailand to the US. Uguu!
        int state = 0;
        int ccc = 0;
        final int length = str.length();
        for (int i = 0 ; i < length ; i++ ) {
            char ch = str.charAt(i);
            switch (state) {
                case 0:
                    if      (ch == '+') state = 1;
                    else if (ch == '0') state = 2;
                    else if (ch == '1') {
                        if (acceptThailandCase) {
                            state = 8;
                        } else {
                            return null;
                        }
                    } else if (isDialable(ch)) {
                        return null;
                    }
                break;

                case 2:
                    if      (ch == '0') state = 3;
                    else if (ch == '1') state = 4;
                    else if (isDialable(ch)) {
                        return null;
                    }
                break;

                case 4:
                    if      (ch == '1') state = 5;
                    else if (isDialable(ch)) {
                        return null;
                    }
                break;

                case 1:
                case 3:
                case 5:
                case 6:
                case 7:
                    {
                        int ret = tryGetISODigit(ch);
                        if (ret > 0) {
                            ccc = ccc * 10 + ret;
                            if (ccc >= 100 || isCountryCallingCode(ccc)) {
                                return new CountryCallingCodeAndNewIndex(ccc, i + 1);
                            }
                            if (state == 1 || state == 3 || state == 5) {
                                state = 6;
                            } else {
                                state++;
                            }
                        } else if (isDialable(ch)) {
                            return null;
                        }
                    }
                    break;
                case 8:
                    if (ch == '6') state = 9;
                    else if (isDialable(ch)) {
                        return null;
                    }
                    break;
                case 9:
                    if (ch == '6') {
                        return new CountryCallingCodeAndNewIndex(66, i + 1);
                    } else {
                        return null;
                    }
                default:
                    return null;
            }
        }

        return null;
    }

    /**
     * Currently this function simply ignore the first digit assuming it is
     * trunk prefix. Actually trunk prefix is different in each country.
     *
     * e.g.
     * "+79161234567" equals "89161234567" (Russian trunk digit is 8)
     * "+33123456789" equals "0123456789" (French trunk digit is 0)
     *
     */
    private static int tryGetTrunkPrefixOmittedIndex(String str, int currentIndex) {
        int length = str.length();
        for (int i = currentIndex ; i < length ; i++) {
            final char ch = str.charAt(i);
            if (tryGetISODigit(ch) >= 0) {
                return i + 1;
            } else if (isDialable(ch)) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Return true if the prefix of "str" is "ignorable". Here, "ignorable" means
     * that "str" has only one digit and separator characters. The one digit is
     * assumed to be trunk prefix.
     */
    private static boolean checkPrefixIsIgnorable(final String str,
            int forwardIndex, int backwardIndex) {
        boolean trunk_prefix_was_read = false;
        while (backwardIndex >= forwardIndex) {
            if (tryGetISODigit(str.charAt(backwardIndex)) >= 0) {
                if (trunk_prefix_was_read) {
                    // More than one digit appeared, meaning that "a" and "b"
                    // is different.
                    return false;
                } else {
                    // Ignore just one digit, assuming it is trunk prefix.
                    trunk_prefix_was_read = true;
                }
            } else if (isDialable(str.charAt(backwardIndex))) {
                // Trunk prefix is a digit, not "*", "#"...
                return false;
            }
            backwardIndex--;
        }

        return true;
    }

    /**
     * Returns Default voice subscription Id.
     */
    private static int getDefaultVoiceSubId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }
    //==== End of utility methods used only in compareStrictly() =====


    /*
     * The config held calling number conversion map, expected to convert to emergency number.
     */
    private static String[] sConvertToEmergencyMap = null;

    /**
     * Converts to emergency number based on the conversion map.
     * The conversion map is declared as config_convert_to_emergency_number_map.
     *
     * @param context a context to use for accessing resources
     * @return The converted emergency number if the number matches conversion map,
     * otherwise original number.
     *
     * @hide
     */
    public static String convertToEmergencyNumber(Context context, String number) {
        if (context == null || TextUtils.isEmpty(number)) {
            return number;
        }

        String normalizedNumber = normalizeNumber(number);

        // The number is already emergency number. Skip conversion.
        if (isEmergencyNumber(normalizedNumber)) {
            return number;
        }

        if (sConvertToEmergencyMap == null) {
            sConvertToEmergencyMap = context.getResources().getStringArray(
                    com.android.internal.R.array.config_convert_to_emergency_number_map);
        }

        // The conversion map is not defined (this is default). Skip conversion.
        if (sConvertToEmergencyMap == null || sConvertToEmergencyMap.length == 0) {
            return number;
        }

        for (String convertMap : sConvertToEmergencyMap) {
            if (DBG) log("convertToEmergencyNumber: " + convertMap);
            String[] entry = null;
            String[] filterNumbers = null;
            String convertedNumber = null;
            if (!TextUtils.isEmpty(convertMap)) {
                entry = convertMap.split(":");
            }
            if (entry != null && entry.length == 2) {
                convertedNumber = entry[1];
                if (!TextUtils.isEmpty(entry[0])) {
                    filterNumbers = entry[0].split(",");
                }
            }
            // Skip if the format of entry is invalid
            if (TextUtils.isEmpty(convertedNumber) || filterNumbers == null
                    || filterNumbers.length == 0) {
                continue;
            }

            for (String filterNumber : filterNumbers) {
                if (DBG) log("convertToEmergencyNumber: filterNumber = " + filterNumber
                        + ", convertedNumber = " + convertedNumber);
                if (!TextUtils.isEmpty(filterNumber) && filterNumber.equals(normalizedNumber)) {
                    if (DBG) log("convertToEmergencyNumber: Matched. Successfully converted to: "
                            + convertedNumber);
                    return convertedNumber;
                }
            }
        }
        return number;
    }

    /**
     * Determines if two phone numbers are the same.
     * <p>
     * Matching is based on <a href="https://github.com/google/libphonenumber>libphonenumber</a>.
     * Unlike {@link #compare(String, String)}, matching takes into account national
     * dialing plans rather than simply matching the last 7 digits of the two phone numbers. As a
     * result, it is expected that some numbers which would match using the previous method will no
     * longer match using this new approach.
     *
     * @param number1
     * @param number2
     * @param defaultCountryIso The lowercase two letter ISO 3166-1 country code. Used when parsing
     *                          the phone numbers where it is not possible to determine the country
     *                          associated with a phone number based on the number alone. It
     *                          is recommended to pass in
     *                          {@link TelephonyManager#getNetworkCountryIso()}.
     * @return True if the two given phone number are same.
     */
    public static boolean areSamePhoneNumber(@NonNull String number1,
            @NonNull String number2, @NonNull String defaultCountryIso) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        PhoneNumber n1;
        PhoneNumber n2;

        if (defaultCountryIso != null) {
            defaultCountryIso = defaultCountryIso.toUpperCase(Locale.ROOT);
        }

        try {
            n1 = util.parseAndKeepRawInput(number1, defaultCountryIso);
            n2 = util.parseAndKeepRawInput(number2, defaultCountryIso);
        } catch (NumberParseException e) {
            return false;
        }

        PhoneNumberUtil.MatchType matchType = util.isNumberMatch(n1, n2);
        if (matchType == PhoneNumberUtil.MatchType.EXACT_MATCH
                || matchType == PhoneNumberUtil.MatchType.NSN_MATCH) {
            return true;
        } else if (matchType == PhoneNumberUtil.MatchType.SHORT_NSN_MATCH) {
            return (n1.getNationalNumber() == n2.getNationalNumber()
                    && n1.getCountryCode() == n2.getCountryCode());
        } else {
            return false;
        }
    }

    /**
     * Check if the number is for Wireless Priority Service call.
     * @param number  The phone number used for WPS call.
     * @return {@code true} if number matches WPS pattern and {@code false} otherwise.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_WPS_CHECK_API_FLAG)
    public static boolean isWpsCallNumber(@NonNull String number) {
        return (number != null) && (number.startsWith(PREFIX_WPS)
                || number.startsWith(PREFIX_WPS_CLIR_ACTIVATE)
                || number.startsWith(PREFIX_WPS_CLIR_DEACTIVATE));
    }
}
