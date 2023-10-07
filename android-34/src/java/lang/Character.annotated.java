/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package java.lang;

import java.util.Locale;
import java.util.Map;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class Character implements java.io.Serializable, java.lang.Comparable<java.lang.Character> {

public Character(char value) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.Character valueOf(char c) { throw new RuntimeException("Stub!"); }

public char charValue() { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public static int hashCode(char value) { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(char c) { throw new RuntimeException("Stub!"); }

public static boolean isValidCodePoint(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isBmpCodePoint(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isSupplementaryCodePoint(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isHighSurrogate(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isLowSurrogate(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isSurrogate(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isSurrogatePair(char high, char low) { throw new RuntimeException("Stub!"); }

public static int charCount(int codePoint) { throw new RuntimeException("Stub!"); }

public static int toCodePoint(char high, char low) { throw new RuntimeException("Stub!"); }

public static int codePointAt(@libcore.util.NonNull java.lang.CharSequence seq, int index) { throw new RuntimeException("Stub!"); }

public static int codePointAt(char[] a, int index) { throw new RuntimeException("Stub!"); }

public static int codePointAt(char[] a, int index, int limit) { throw new RuntimeException("Stub!"); }

public static int codePointBefore(@libcore.util.NonNull java.lang.CharSequence seq, int index) { throw new RuntimeException("Stub!"); }

public static int codePointBefore(char[] a, int index) { throw new RuntimeException("Stub!"); }

public static int codePointBefore(char[] a, int index, int start) { throw new RuntimeException("Stub!"); }

public static char highSurrogate(int codePoint) { throw new RuntimeException("Stub!"); }

public static char lowSurrogate(int codePoint) { throw new RuntimeException("Stub!"); }

public static int toChars(int codePoint, char[] dst, int dstIndex) { throw new RuntimeException("Stub!"); }

public static char[] toChars(int codePoint) { throw new RuntimeException("Stub!"); }

public static int codePointCount(@libcore.util.NonNull java.lang.CharSequence seq, int beginIndex, int endIndex) { throw new RuntimeException("Stub!"); }

public static int codePointCount(char[] a, int offset, int count) { throw new RuntimeException("Stub!"); }

public static int offsetByCodePoints(@libcore.util.NonNull java.lang.CharSequence seq, int index, int codePointOffset) { throw new RuntimeException("Stub!"); }

public static int offsetByCodePoints(char[] a, int start, int count, int index, int codePointOffset) { throw new RuntimeException("Stub!"); }

public static boolean isLowerCase(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isLowerCase(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isUpperCase(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isUpperCase(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isTitleCase(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isTitleCase(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isDigit(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isDigit(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isDefined(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isDefined(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isLetter(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isLetter(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isLetterOrDigit(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isLetterOrDigit(int codePoint) { throw new RuntimeException("Stub!"); }

@Deprecated public static boolean isJavaLetter(char ch) { throw new RuntimeException("Stub!"); }

@Deprecated public static boolean isJavaLetterOrDigit(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isAlphabetic(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isIdeographic(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isJavaIdentifierStart(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isJavaIdentifierStart(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isJavaIdentifierPart(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isJavaIdentifierPart(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isUnicodeIdentifierStart(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isUnicodeIdentifierStart(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isUnicodeIdentifierPart(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isUnicodeIdentifierPart(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isIdentifierIgnorable(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isIdentifierIgnorable(int codePoint) { throw new RuntimeException("Stub!"); }

public static char toLowerCase(char ch) { throw new RuntimeException("Stub!"); }

public static int toLowerCase(int codePoint) { throw new RuntimeException("Stub!"); }

public static char toUpperCase(char ch) { throw new RuntimeException("Stub!"); }

public static int toUpperCase(int codePoint) { throw new RuntimeException("Stub!"); }

public static char toTitleCase(char ch) { throw new RuntimeException("Stub!"); }

public static int toTitleCase(int codePoint) { throw new RuntimeException("Stub!"); }

public static int digit(char ch, int radix) { throw new RuntimeException("Stub!"); }

public static int digit(int codePoint, int radix) { throw new RuntimeException("Stub!"); }

public static int getNumericValue(char ch) { throw new RuntimeException("Stub!"); }

public static int getNumericValue(int codePoint) { throw new RuntimeException("Stub!"); }

@Deprecated public static boolean isSpace(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isSpaceChar(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isSpaceChar(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isWhitespace(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isWhitespace(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isISOControl(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isISOControl(int codePoint) { throw new RuntimeException("Stub!"); }

public static int getType(char ch) { throw new RuntimeException("Stub!"); }

public static int getType(int codePoint) { throw new RuntimeException("Stub!"); }

public static char forDigit(int digit, int radix) { throw new RuntimeException("Stub!"); }

public static byte getDirectionality(char ch) { throw new RuntimeException("Stub!"); }

public static byte getDirectionality(int codePoint) { throw new RuntimeException("Stub!"); }

public static boolean isMirrored(char ch) { throw new RuntimeException("Stub!"); }

public static boolean isMirrored(int codePoint) { throw new RuntimeException("Stub!"); }

public int compareTo(@libcore.util.NonNull java.lang.Character anotherCharacter) { throw new RuntimeException("Stub!"); }

public static int compare(char x, char y) { throw new RuntimeException("Stub!"); }

public static char reverseBytes(char ch) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public static java.lang.String getName(int codePoint) { throw new RuntimeException("Stub!"); }

public static final int BYTES = 2; // 0x2

public static final byte COMBINING_SPACING_MARK = 8; // 0x8

public static final byte CONNECTOR_PUNCTUATION = 23; // 0x17

public static final byte CONTROL = 15; // 0xf

public static final byte CURRENCY_SYMBOL = 26; // 0x1a

public static final byte DASH_PUNCTUATION = 20; // 0x14

public static final byte DECIMAL_DIGIT_NUMBER = 9; // 0x9

public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6; // 0x6

public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9; // 0x9

public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7; // 0x7

public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3; // 0x3

public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4; // 0x4

public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5; // 0x5

public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0; // 0x0

public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14; // 0xe

public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15; // 0xf

public static final byte DIRECTIONALITY_NONSPACING_MARK = 8; // 0x8

public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13; // 0xd

public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10; // 0xa

public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18; // 0x12

public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1; // 0x1

public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2; // 0x2

public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16; // 0x10

public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17; // 0x11

public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11; // 0xb

public static final byte DIRECTIONALITY_UNDEFINED = -1; // 0xffffffff

public static final byte DIRECTIONALITY_WHITESPACE = 12; // 0xc

public static final byte ENCLOSING_MARK = 7; // 0x7

public static final byte END_PUNCTUATION = 22; // 0x16

public static final byte FINAL_QUOTE_PUNCTUATION = 30; // 0x1e

public static final byte FORMAT = 16; // 0x10

public static final byte INITIAL_QUOTE_PUNCTUATION = 29; // 0x1d

public static final byte LETTER_NUMBER = 10; // 0xa

public static final byte LINE_SEPARATOR = 13; // 0xd

public static final byte LOWERCASE_LETTER = 2; // 0x2

public static final byte MATH_SYMBOL = 25; // 0x19

public static final int MAX_CODE_POINT = 1114111; // 0x10ffff

public static final char MAX_HIGH_SURROGATE = 56319; // 0xdbff '\udbff'

public static final char MAX_LOW_SURROGATE = 57343; // 0xdfff '\udfff'

public static final int MAX_RADIX = 36; // 0x24

public static final char MAX_SURROGATE = 57343; // 0xdfff '\udfff'

public static final char MAX_VALUE = 65535; // 0xffff '\uffff'

public static final int MIN_CODE_POINT = 0; // 0x0

public static final char MIN_HIGH_SURROGATE = 55296; // 0xd800 '\ud800'

public static final char MIN_LOW_SURROGATE = 56320; // 0xdc00 '\udc00'

public static final int MIN_RADIX = 2; // 0x2

public static final int MIN_SUPPLEMENTARY_CODE_POINT = 65536; // 0x10000

public static final char MIN_SURROGATE = 55296; // 0xd800 '\ud800'

public static final char MIN_VALUE = 0; // 0x0000 '\u0000'

public static final byte MODIFIER_LETTER = 4; // 0x4

public static final byte MODIFIER_SYMBOL = 27; // 0x1b

public static final byte NON_SPACING_MARK = 6; // 0x6

public static final byte OTHER_LETTER = 5; // 0x5

public static final byte OTHER_NUMBER = 11; // 0xb

public static final byte OTHER_PUNCTUATION = 24; // 0x18

public static final byte OTHER_SYMBOL = 28; // 0x1c

public static final byte PARAGRAPH_SEPARATOR = 14; // 0xe

public static final byte PRIVATE_USE = 18; // 0x12

public static final int SIZE = 16; // 0x10

public static final byte SPACE_SEPARATOR = 12; // 0xc

public static final byte START_PUNCTUATION = 21; // 0x15

public static final byte SURROGATE = 19; // 0x13

public static final byte TITLECASE_LETTER = 3; // 0x3

public static final java.lang.Class<java.lang.Character> TYPE;
static { TYPE = null; }

public static final byte UNASSIGNED = 0; // 0x0

public static final byte UPPERCASE_LETTER = 1; // 0x1
@SuppressWarnings({"unchecked", "deprecation", "all"})
public static class Subset {

protected Subset(@libcore.util.NonNull java.lang.String name) { throw new RuntimeException("Stub!"); }

public final boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public final int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public final java.lang.String toString() { throw new RuntimeException("Stub!"); }
}

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static final class UnicodeBlock extends java.lang.Character.Subset {

UnicodeBlock(java.lang.String idName) { super(null); throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public static java.lang.Character.UnicodeBlock of(char c) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public static java.lang.Character.UnicodeBlock of(int codePoint) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.Character.UnicodeBlock forName(@libcore.util.NonNull java.lang.String blockName) { throw new RuntimeException("Stub!"); }

public static final java.lang.Character.UnicodeBlock AEGEAN_NUMBERS;
static { AEGEAN_NUMBERS = null; }

public static final java.lang.Character.UnicodeBlock ALCHEMICAL_SYMBOLS;
static { ALCHEMICAL_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock ALPHABETIC_PRESENTATION_FORMS;
static { ALPHABETIC_PRESENTATION_FORMS = null; }

public static final java.lang.Character.UnicodeBlock ANCIENT_GREEK_MUSICAL_NOTATION;
static { ANCIENT_GREEK_MUSICAL_NOTATION = null; }

public static final java.lang.Character.UnicodeBlock ANCIENT_GREEK_NUMBERS;
static { ANCIENT_GREEK_NUMBERS = null; }

public static final java.lang.Character.UnicodeBlock ANCIENT_SYMBOLS;
static { ANCIENT_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock ARABIC;
static { ARABIC = null; }

public static final java.lang.Character.UnicodeBlock ARABIC_EXTENDED_A;
static { ARABIC_EXTENDED_A = null; }

public static final java.lang.Character.UnicodeBlock ARABIC_MATHEMATICAL_ALPHABETIC_SYMBOLS;
static { ARABIC_MATHEMATICAL_ALPHABETIC_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_A;
static { ARABIC_PRESENTATION_FORMS_A = null; }

public static final java.lang.Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_B;
static { ARABIC_PRESENTATION_FORMS_B = null; }

public static final java.lang.Character.UnicodeBlock ARABIC_SUPPLEMENT;
static { ARABIC_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock ARMENIAN;
static { ARMENIAN = null; }

public static final java.lang.Character.UnicodeBlock ARROWS;
static { ARROWS = null; }

public static final java.lang.Character.UnicodeBlock AVESTAN;
static { AVESTAN = null; }

public static final java.lang.Character.UnicodeBlock BALINESE;
static { BALINESE = null; }

public static final java.lang.Character.UnicodeBlock BAMUM;
static { BAMUM = null; }

public static final java.lang.Character.UnicodeBlock BAMUM_SUPPLEMENT;
static { BAMUM_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock BASIC_LATIN;
static { BASIC_LATIN = null; }

public static final java.lang.Character.UnicodeBlock BATAK;
static { BATAK = null; }

public static final java.lang.Character.UnicodeBlock BENGALI;
static { BENGALI = null; }

public static final java.lang.Character.UnicodeBlock BLOCK_ELEMENTS;
static { BLOCK_ELEMENTS = null; }

public static final java.lang.Character.UnicodeBlock BOPOMOFO;
static { BOPOMOFO = null; }

public static final java.lang.Character.UnicodeBlock BOPOMOFO_EXTENDED;
static { BOPOMOFO_EXTENDED = null; }

public static final java.lang.Character.UnicodeBlock BOX_DRAWING;
static { BOX_DRAWING = null; }

public static final java.lang.Character.UnicodeBlock BRAHMI;
static { BRAHMI = null; }

public static final java.lang.Character.UnicodeBlock BRAILLE_PATTERNS;
static { BRAILLE_PATTERNS = null; }

public static final java.lang.Character.UnicodeBlock BUGINESE;
static { BUGINESE = null; }

public static final java.lang.Character.UnicodeBlock BUHID;
static { BUHID = null; }

public static final java.lang.Character.UnicodeBlock BYZANTINE_MUSICAL_SYMBOLS;
static { BYZANTINE_MUSICAL_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock CARIAN;
static { CARIAN = null; }

public static final java.lang.Character.UnicodeBlock CHAKMA;
static { CHAKMA = null; }

public static final java.lang.Character.UnicodeBlock CHAM;
static { CHAM = null; }

public static final java.lang.Character.UnicodeBlock CHEROKEE;
static { CHEROKEE = null; }

public static final java.lang.Character.UnicodeBlock CJK_COMPATIBILITY;
static { CJK_COMPATIBILITY = null; }

public static final java.lang.Character.UnicodeBlock CJK_COMPATIBILITY_FORMS;
static { CJK_COMPATIBILITY_FORMS = null; }

public static final java.lang.Character.UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS;
static { CJK_COMPATIBILITY_IDEOGRAPHS = null; }

public static final java.lang.Character.UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
static { CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock CJK_RADICALS_SUPPLEMENT;
static { CJK_RADICALS_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock CJK_STROKES;
static { CJK_STROKES = null; }

public static final java.lang.Character.UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION;
static { CJK_SYMBOLS_AND_PUNCTUATION = null; }

public static final java.lang.Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS;
static { CJK_UNIFIED_IDEOGRAPHS = null; }

public static final java.lang.Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
static { CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A = null; }

public static final java.lang.Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B;
static { CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B = null; }

public static final java.lang.Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C;
static { CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C = null; }

public static final java.lang.Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D;
static { CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D = null; }

public static final java.lang.Character.UnicodeBlock COMBINING_DIACRITICAL_MARKS;
static { COMBINING_DIACRITICAL_MARKS = null; }

public static final java.lang.Character.UnicodeBlock COMBINING_DIACRITICAL_MARKS_SUPPLEMENT;
static { COMBINING_DIACRITICAL_MARKS_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock COMBINING_HALF_MARKS;
static { COMBINING_HALF_MARKS = null; }

public static final java.lang.Character.UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS;
static { COMBINING_MARKS_FOR_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock COMMON_INDIC_NUMBER_FORMS;
static { COMMON_INDIC_NUMBER_FORMS = null; }

public static final java.lang.Character.UnicodeBlock CONTROL_PICTURES;
static { CONTROL_PICTURES = null; }

public static final java.lang.Character.UnicodeBlock COPTIC;
static { COPTIC = null; }

public static final java.lang.Character.UnicodeBlock COUNTING_ROD_NUMERALS;
static { COUNTING_ROD_NUMERALS = null; }

public static final java.lang.Character.UnicodeBlock CUNEIFORM;
static { CUNEIFORM = null; }

public static final java.lang.Character.UnicodeBlock CUNEIFORM_NUMBERS_AND_PUNCTUATION;
static { CUNEIFORM_NUMBERS_AND_PUNCTUATION = null; }

public static final java.lang.Character.UnicodeBlock CURRENCY_SYMBOLS;
static { CURRENCY_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock CYPRIOT_SYLLABARY;
static { CYPRIOT_SYLLABARY = null; }

public static final java.lang.Character.UnicodeBlock CYRILLIC;
static { CYRILLIC = null; }

public static final java.lang.Character.UnicodeBlock CYRILLIC_EXTENDED_A;
static { CYRILLIC_EXTENDED_A = null; }

public static final java.lang.Character.UnicodeBlock CYRILLIC_EXTENDED_B;
static { CYRILLIC_EXTENDED_B = null; }

public static final java.lang.Character.UnicodeBlock CYRILLIC_SUPPLEMENTARY;
static { CYRILLIC_SUPPLEMENTARY = null; }

public static final java.lang.Character.UnicodeBlock DESERET;
static { DESERET = null; }

public static final java.lang.Character.UnicodeBlock DEVANAGARI;
static { DEVANAGARI = null; }

public static final java.lang.Character.UnicodeBlock DEVANAGARI_EXTENDED;
static { DEVANAGARI_EXTENDED = null; }

public static final java.lang.Character.UnicodeBlock DINGBATS;
static { DINGBATS = null; }

public static final java.lang.Character.UnicodeBlock DOMINO_TILES;
static { DOMINO_TILES = null; }

public static final java.lang.Character.UnicodeBlock EGYPTIAN_HIEROGLYPHS;
static { EGYPTIAN_HIEROGLYPHS = null; }

public static final java.lang.Character.UnicodeBlock EMOTICONS;
static { EMOTICONS = null; }

public static final java.lang.Character.UnicodeBlock ENCLOSED_ALPHANUMERICS;
static { ENCLOSED_ALPHANUMERICS = null; }

public static final java.lang.Character.UnicodeBlock ENCLOSED_ALPHANUMERIC_SUPPLEMENT;
static { ENCLOSED_ALPHANUMERIC_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS;
static { ENCLOSED_CJK_LETTERS_AND_MONTHS = null; }

public static final java.lang.Character.UnicodeBlock ENCLOSED_IDEOGRAPHIC_SUPPLEMENT;
static { ENCLOSED_IDEOGRAPHIC_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock ETHIOPIC;
static { ETHIOPIC = null; }

public static final java.lang.Character.UnicodeBlock ETHIOPIC_EXTENDED;
static { ETHIOPIC_EXTENDED = null; }

public static final java.lang.Character.UnicodeBlock ETHIOPIC_EXTENDED_A;
static { ETHIOPIC_EXTENDED_A = null; }

public static final java.lang.Character.UnicodeBlock ETHIOPIC_SUPPLEMENT;
static { ETHIOPIC_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock GENERAL_PUNCTUATION;
static { GENERAL_PUNCTUATION = null; }

public static final java.lang.Character.UnicodeBlock GEOMETRIC_SHAPES;
static { GEOMETRIC_SHAPES = null; }

public static final java.lang.Character.UnicodeBlock GEORGIAN;
static { GEORGIAN = null; }

public static final java.lang.Character.UnicodeBlock GEORGIAN_SUPPLEMENT;
static { GEORGIAN_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock GLAGOLITIC;
static { GLAGOLITIC = null; }

public static final java.lang.Character.UnicodeBlock GOTHIC;
static { GOTHIC = null; }

public static final java.lang.Character.UnicodeBlock GREEK;
static { GREEK = null; }

public static final java.lang.Character.UnicodeBlock GREEK_EXTENDED;
static { GREEK_EXTENDED = null; }

public static final java.lang.Character.UnicodeBlock GUJARATI;
static { GUJARATI = null; }

public static final java.lang.Character.UnicodeBlock GURMUKHI;
static { GURMUKHI = null; }

public static final java.lang.Character.UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS;
static { HALFWIDTH_AND_FULLWIDTH_FORMS = null; }

public static final java.lang.Character.UnicodeBlock HANGUL_COMPATIBILITY_JAMO;
static { HANGUL_COMPATIBILITY_JAMO = null; }

public static final java.lang.Character.UnicodeBlock HANGUL_JAMO;
static { HANGUL_JAMO = null; }

public static final java.lang.Character.UnicodeBlock HANGUL_JAMO_EXTENDED_A;
static { HANGUL_JAMO_EXTENDED_A = null; }

public static final java.lang.Character.UnicodeBlock HANGUL_JAMO_EXTENDED_B;
static { HANGUL_JAMO_EXTENDED_B = null; }

public static final java.lang.Character.UnicodeBlock HANGUL_SYLLABLES;
static { HANGUL_SYLLABLES = null; }

public static final java.lang.Character.UnicodeBlock HANUNOO;
static { HANUNOO = null; }

public static final java.lang.Character.UnicodeBlock HEBREW;
static { HEBREW = null; }

public static final java.lang.Character.UnicodeBlock HIGH_PRIVATE_USE_SURROGATES;
static { HIGH_PRIVATE_USE_SURROGATES = null; }

public static final java.lang.Character.UnicodeBlock HIGH_SURROGATES;
static { HIGH_SURROGATES = null; }

public static final java.lang.Character.UnicodeBlock HIRAGANA;
static { HIRAGANA = null; }

public static final java.lang.Character.UnicodeBlock IDEOGRAPHIC_DESCRIPTION_CHARACTERS;
static { IDEOGRAPHIC_DESCRIPTION_CHARACTERS = null; }

public static final java.lang.Character.UnicodeBlock IMPERIAL_ARAMAIC;
static { IMPERIAL_ARAMAIC = null; }

public static final java.lang.Character.UnicodeBlock INSCRIPTIONAL_PAHLAVI;
static { INSCRIPTIONAL_PAHLAVI = null; }

public static final java.lang.Character.UnicodeBlock INSCRIPTIONAL_PARTHIAN;
static { INSCRIPTIONAL_PARTHIAN = null; }

public static final java.lang.Character.UnicodeBlock IPA_EXTENSIONS;
static { IPA_EXTENSIONS = null; }

public static final java.lang.Character.UnicodeBlock JAVANESE;
static { JAVANESE = null; }

public static final java.lang.Character.UnicodeBlock KAITHI;
static { KAITHI = null; }

public static final java.lang.Character.UnicodeBlock KANA_SUPPLEMENT;
static { KANA_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock KANBUN;
static { KANBUN = null; }

public static final java.lang.Character.UnicodeBlock KANGXI_RADICALS;
static { KANGXI_RADICALS = null; }

public static final java.lang.Character.UnicodeBlock KANNADA;
static { KANNADA = null; }

public static final java.lang.Character.UnicodeBlock KATAKANA;
static { KATAKANA = null; }

public static final java.lang.Character.UnicodeBlock KATAKANA_PHONETIC_EXTENSIONS;
static { KATAKANA_PHONETIC_EXTENSIONS = null; }

public static final java.lang.Character.UnicodeBlock KAYAH_LI;
static { KAYAH_LI = null; }

public static final java.lang.Character.UnicodeBlock KHAROSHTHI;
static { KHAROSHTHI = null; }

public static final java.lang.Character.UnicodeBlock KHMER;
static { KHMER = null; }

public static final java.lang.Character.UnicodeBlock KHMER_SYMBOLS;
static { KHMER_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock LAO;
static { LAO = null; }

public static final java.lang.Character.UnicodeBlock LATIN_1_SUPPLEMENT;
static { LATIN_1_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock LATIN_EXTENDED_A;
static { LATIN_EXTENDED_A = null; }

public static final java.lang.Character.UnicodeBlock LATIN_EXTENDED_ADDITIONAL;
static { LATIN_EXTENDED_ADDITIONAL = null; }

public static final java.lang.Character.UnicodeBlock LATIN_EXTENDED_B;
static { LATIN_EXTENDED_B = null; }

public static final java.lang.Character.UnicodeBlock LATIN_EXTENDED_C;
static { LATIN_EXTENDED_C = null; }

public static final java.lang.Character.UnicodeBlock LATIN_EXTENDED_D;
static { LATIN_EXTENDED_D = null; }

public static final java.lang.Character.UnicodeBlock LEPCHA;
static { LEPCHA = null; }

public static final java.lang.Character.UnicodeBlock LETTERLIKE_SYMBOLS;
static { LETTERLIKE_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock LIMBU;
static { LIMBU = null; }

public static final java.lang.Character.UnicodeBlock LINEAR_B_IDEOGRAMS;
static { LINEAR_B_IDEOGRAMS = null; }

public static final java.lang.Character.UnicodeBlock LINEAR_B_SYLLABARY;
static { LINEAR_B_SYLLABARY = null; }

public static final java.lang.Character.UnicodeBlock LISU;
static { LISU = null; }

public static final java.lang.Character.UnicodeBlock LOW_SURROGATES;
static { LOW_SURROGATES = null; }

public static final java.lang.Character.UnicodeBlock LYCIAN;
static { LYCIAN = null; }

public static final java.lang.Character.UnicodeBlock LYDIAN;
static { LYDIAN = null; }

public static final java.lang.Character.UnicodeBlock MAHJONG_TILES;
static { MAHJONG_TILES = null; }

public static final java.lang.Character.UnicodeBlock MALAYALAM;
static { MALAYALAM = null; }

public static final java.lang.Character.UnicodeBlock MANDAIC;
static { MANDAIC = null; }

public static final java.lang.Character.UnicodeBlock MATHEMATICAL_ALPHANUMERIC_SYMBOLS;
static { MATHEMATICAL_ALPHANUMERIC_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock MATHEMATICAL_OPERATORS;
static { MATHEMATICAL_OPERATORS = null; }

public static final java.lang.Character.UnicodeBlock MEETEI_MAYEK;
static { MEETEI_MAYEK = null; }

public static final java.lang.Character.UnicodeBlock MEETEI_MAYEK_EXTENSIONS;
static { MEETEI_MAYEK_EXTENSIONS = null; }

public static final java.lang.Character.UnicodeBlock MEROITIC_CURSIVE;
static { MEROITIC_CURSIVE = null; }

public static final java.lang.Character.UnicodeBlock MEROITIC_HIEROGLYPHS;
static { MEROITIC_HIEROGLYPHS = null; }

public static final java.lang.Character.UnicodeBlock MIAO;
static { MIAO = null; }

public static final java.lang.Character.UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A;
static { MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A = null; }

public static final java.lang.Character.UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B;
static { MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B = null; }

public static final java.lang.Character.UnicodeBlock MISCELLANEOUS_SYMBOLS;
static { MISCELLANEOUS_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_ARROWS;
static { MISCELLANEOUS_SYMBOLS_AND_ARROWS = null; }

public static final java.lang.Character.UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS;
static { MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS = null; }

public static final java.lang.Character.UnicodeBlock MISCELLANEOUS_TECHNICAL;
static { MISCELLANEOUS_TECHNICAL = null; }

public static final java.lang.Character.UnicodeBlock MODIFIER_TONE_LETTERS;
static { MODIFIER_TONE_LETTERS = null; }

public static final java.lang.Character.UnicodeBlock MONGOLIAN;
static { MONGOLIAN = null; }

public static final java.lang.Character.UnicodeBlock MUSICAL_SYMBOLS;
static { MUSICAL_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock MYANMAR;
static { MYANMAR = null; }

public static final java.lang.Character.UnicodeBlock MYANMAR_EXTENDED_A;
static { MYANMAR_EXTENDED_A = null; }

public static final java.lang.Character.UnicodeBlock NEW_TAI_LUE;
static { NEW_TAI_LUE = null; }

public static final java.lang.Character.UnicodeBlock NKO;
static { NKO = null; }

public static final java.lang.Character.UnicodeBlock NUMBER_FORMS;
static { NUMBER_FORMS = null; }

public static final java.lang.Character.UnicodeBlock OGHAM;
static { OGHAM = null; }

public static final java.lang.Character.UnicodeBlock OLD_ITALIC;
static { OLD_ITALIC = null; }

public static final java.lang.Character.UnicodeBlock OLD_PERSIAN;
static { OLD_PERSIAN = null; }

public static final java.lang.Character.UnicodeBlock OLD_SOUTH_ARABIAN;
static { OLD_SOUTH_ARABIAN = null; }

public static final java.lang.Character.UnicodeBlock OLD_TURKIC;
static { OLD_TURKIC = null; }

public static final java.lang.Character.UnicodeBlock OL_CHIKI;
static { OL_CHIKI = null; }

public static final java.lang.Character.UnicodeBlock OPTICAL_CHARACTER_RECOGNITION;
static { OPTICAL_CHARACTER_RECOGNITION = null; }

public static final java.lang.Character.UnicodeBlock ORIYA;
static { ORIYA = null; }

public static final java.lang.Character.UnicodeBlock OSMANYA;
static { OSMANYA = null; }

public static final java.lang.Character.UnicodeBlock PHAGS_PA;
static { PHAGS_PA = null; }

public static final java.lang.Character.UnicodeBlock PHAISTOS_DISC;
static { PHAISTOS_DISC = null; }

public static final java.lang.Character.UnicodeBlock PHOENICIAN;
static { PHOENICIAN = null; }

public static final java.lang.Character.UnicodeBlock PHONETIC_EXTENSIONS;
static { PHONETIC_EXTENSIONS = null; }

public static final java.lang.Character.UnicodeBlock PHONETIC_EXTENSIONS_SUPPLEMENT;
static { PHONETIC_EXTENSIONS_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock PLAYING_CARDS;
static { PLAYING_CARDS = null; }

public static final java.lang.Character.UnicodeBlock PRIVATE_USE_AREA;
static { PRIVATE_USE_AREA = null; }

public static final java.lang.Character.UnicodeBlock REJANG;
static { REJANG = null; }

public static final java.lang.Character.UnicodeBlock RUMI_NUMERAL_SYMBOLS;
static { RUMI_NUMERAL_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock RUNIC;
static { RUNIC = null; }

public static final java.lang.Character.UnicodeBlock SAMARITAN;
static { SAMARITAN = null; }

public static final java.lang.Character.UnicodeBlock SAURASHTRA;
static { SAURASHTRA = null; }

public static final java.lang.Character.UnicodeBlock SHARADA;
static { SHARADA = null; }

public static final java.lang.Character.UnicodeBlock SHAVIAN;
static { SHAVIAN = null; }

public static final java.lang.Character.UnicodeBlock SINHALA;
static { SINHALA = null; }

public static final java.lang.Character.UnicodeBlock SMALL_FORM_VARIANTS;
static { SMALL_FORM_VARIANTS = null; }

public static final java.lang.Character.UnicodeBlock SORA_SOMPENG;
static { SORA_SOMPENG = null; }

public static final java.lang.Character.UnicodeBlock SPACING_MODIFIER_LETTERS;
static { SPACING_MODIFIER_LETTERS = null; }

public static final java.lang.Character.UnicodeBlock SPECIALS;
static { SPECIALS = null; }

public static final java.lang.Character.UnicodeBlock SUNDANESE;
static { SUNDANESE = null; }

public static final java.lang.Character.UnicodeBlock SUNDANESE_SUPPLEMENT;
static { SUNDANESE_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS;
static { SUPERSCRIPTS_AND_SUBSCRIPTS = null; }

public static final java.lang.Character.UnicodeBlock SUPPLEMENTAL_ARROWS_A;
static { SUPPLEMENTAL_ARROWS_A = null; }

public static final java.lang.Character.UnicodeBlock SUPPLEMENTAL_ARROWS_B;
static { SUPPLEMENTAL_ARROWS_B = null; }

public static final java.lang.Character.UnicodeBlock SUPPLEMENTAL_MATHEMATICAL_OPERATORS;
static { SUPPLEMENTAL_MATHEMATICAL_OPERATORS = null; }

public static final java.lang.Character.UnicodeBlock SUPPLEMENTAL_PUNCTUATION;
static { SUPPLEMENTAL_PUNCTUATION = null; }

public static final java.lang.Character.UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_A;
static { SUPPLEMENTARY_PRIVATE_USE_AREA_A = null; }

public static final java.lang.Character.UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_B;
static { SUPPLEMENTARY_PRIVATE_USE_AREA_B = null; }

@Deprecated public static final java.lang.Character.UnicodeBlock SURROGATES_AREA;
static { SURROGATES_AREA = null; }

public static final java.lang.Character.UnicodeBlock SYLOTI_NAGRI;
static { SYLOTI_NAGRI = null; }

public static final java.lang.Character.UnicodeBlock SYRIAC;
static { SYRIAC = null; }

public static final java.lang.Character.UnicodeBlock TAGALOG;
static { TAGALOG = null; }

public static final java.lang.Character.UnicodeBlock TAGBANWA;
static { TAGBANWA = null; }

public static final java.lang.Character.UnicodeBlock TAGS;
static { TAGS = null; }

public static final java.lang.Character.UnicodeBlock TAI_LE;
static { TAI_LE = null; }

public static final java.lang.Character.UnicodeBlock TAI_THAM;
static { TAI_THAM = null; }

public static final java.lang.Character.UnicodeBlock TAI_VIET;
static { TAI_VIET = null; }

public static final java.lang.Character.UnicodeBlock TAI_XUAN_JING_SYMBOLS;
static { TAI_XUAN_JING_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock TAKRI;
static { TAKRI = null; }

public static final java.lang.Character.UnicodeBlock TAMIL;
static { TAMIL = null; }

public static final java.lang.Character.UnicodeBlock TELUGU;
static { TELUGU = null; }

public static final java.lang.Character.UnicodeBlock THAANA;
static { THAANA = null; }

public static final java.lang.Character.UnicodeBlock THAI;
static { THAI = null; }

public static final java.lang.Character.UnicodeBlock TIBETAN;
static { TIBETAN = null; }

public static final java.lang.Character.UnicodeBlock TIFINAGH;
static { TIFINAGH = null; }

public static final java.lang.Character.UnicodeBlock TRANSPORT_AND_MAP_SYMBOLS;
static { TRANSPORT_AND_MAP_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock UGARITIC;
static { UGARITIC = null; }

public static final java.lang.Character.UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS;
static { UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS = null; }

public static final java.lang.Character.UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED;
static { UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED = null; }

public static final java.lang.Character.UnicodeBlock VAI;
static { VAI = null; }

public static final java.lang.Character.UnicodeBlock VARIATION_SELECTORS;
static { VARIATION_SELECTORS = null; }

public static final java.lang.Character.UnicodeBlock VARIATION_SELECTORS_SUPPLEMENT;
static { VARIATION_SELECTORS_SUPPLEMENT = null; }

public static final java.lang.Character.UnicodeBlock VEDIC_EXTENSIONS;
static { VEDIC_EXTENSIONS = null; }

public static final java.lang.Character.UnicodeBlock VERTICAL_FORMS;
static { VERTICAL_FORMS = null; }

public static final java.lang.Character.UnicodeBlock YIJING_HEXAGRAM_SYMBOLS;
static { YIJING_HEXAGRAM_SYMBOLS = null; }

public static final java.lang.Character.UnicodeBlock YI_RADICALS;
static { YI_RADICALS = null; }

public static final java.lang.Character.UnicodeBlock YI_SYLLABLES;
static { YI_SYLLABLES = null; }
}

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static enum UnicodeScript {
COMMON,
LATIN,
GREEK,
CYRILLIC,
ARMENIAN,
HEBREW,
ARABIC,
SYRIAC,
THAANA,
DEVANAGARI,
BENGALI,
GURMUKHI,
GUJARATI,
ORIYA,
TAMIL,
TELUGU,
KANNADA,
MALAYALAM,
SINHALA,
THAI,
LAO,
TIBETAN,
MYANMAR,
GEORGIAN,
HANGUL,
ETHIOPIC,
CHEROKEE,
CANADIAN_ABORIGINAL,
OGHAM,
RUNIC,
KHMER,
MONGOLIAN,
HIRAGANA,
KATAKANA,
BOPOMOFO,
HAN,
YI,
OLD_ITALIC,
GOTHIC,
DESERET,
INHERITED,
TAGALOG,
HANUNOO,
BUHID,
TAGBANWA,
LIMBU,
TAI_LE,
LINEAR_B,
UGARITIC,
SHAVIAN,
OSMANYA,
CYPRIOT,
BRAILLE,
BUGINESE,
COPTIC,
NEW_TAI_LUE,
GLAGOLITIC,
TIFINAGH,
SYLOTI_NAGRI,
OLD_PERSIAN,
KHAROSHTHI,
BALINESE,
CUNEIFORM,
PHOENICIAN,
PHAGS_PA,
NKO,
SUNDANESE,
BATAK,
LEPCHA,
OL_CHIKI,
VAI,
SAURASHTRA,
KAYAH_LI,
REJANG,
LYCIAN,
CARIAN,
LYDIAN,
CHAM,
TAI_THAM,
TAI_VIET,
AVESTAN,
EGYPTIAN_HIEROGLYPHS,
SAMARITAN,
MANDAIC,
LISU,
BAMUM,
JAVANESE,
MEETEI_MAYEK,
IMPERIAL_ARAMAIC,
OLD_SOUTH_ARABIAN,
INSCRIPTIONAL_PARTHIAN,
INSCRIPTIONAL_PAHLAVI,
OLD_TURKIC,
BRAHMI,
KAITHI,
MEROITIC_HIEROGLYPHS,
MEROITIC_CURSIVE,
SORA_SOMPENG,
CHAKMA,
SHARADA,
TAKRI,
MIAO,
UNKNOWN;

@libcore.util.NonNull public static java.lang.Character.UnicodeScript of(int codePoint) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.Character.UnicodeScript forName(@libcore.util.NonNull java.lang.String scriptName) { throw new RuntimeException("Stub!"); }
}

}

