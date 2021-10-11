/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */


package java.util;

import java.text.MessageFormat;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class Locale implements java.lang.Cloneable, java.io.Serializable {

public Locale(@libcore.util.NonNull java.lang.String language, @libcore.util.NonNull java.lang.String country, @libcore.util.NonNull java.lang.String variant) { throw new RuntimeException("Stub!"); }

public Locale(@libcore.util.NonNull java.lang.String language, @libcore.util.NonNull java.lang.String country) { throw new RuntimeException("Stub!"); }

public Locale(@libcore.util.NonNull java.lang.String language) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Locale getDefault() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Locale getDefault(@libcore.util.NonNull java.util.Locale.Category category) { throw new RuntimeException("Stub!"); }

public static synchronized void setDefault(@libcore.util.NonNull java.util.Locale newLocale) { throw new RuntimeException("Stub!"); }

public static synchronized void setDefault(@libcore.util.NonNull java.util.Locale.Category category, @libcore.util.NonNull java.util.Locale newLocale) { throw new RuntimeException("Stub!"); }

public static java.util.@libcore.util.NonNull Locale @libcore.util.NonNull [] getAvailableLocales() { throw new RuntimeException("Stub!"); }

public static java.lang.@libcore.util.NonNull String @libcore.util.NonNull [] getISOCountries() { throw new RuntimeException("Stub!"); }

public static java.lang.@libcore.util.NonNull String @libcore.util.NonNull [] getISOLanguages() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getLanguage() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getScript() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getCountry() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getVariant() { throw new RuntimeException("Stub!"); }

public boolean hasExtensions() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale stripExtensions() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getExtension(char key) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<java.lang.@libcore.util.NonNull Character> getExtensionKeys() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<java.lang.@libcore.util.NonNull String> getUnicodeLocaleAttributes() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getUnicodeLocaleType(@libcore.util.NonNull java.lang.String key) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<java.lang.@libcore.util.NonNull String> getUnicodeLocaleKeys() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toLanguageTag() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Locale forLanguageTag(@libcore.util.NonNull java.lang.String languageTag) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getISO3Language() throws java.util.MissingResourceException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getISO3Country() throws java.util.MissingResourceException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayLanguage() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayLanguage(@libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayScript() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayScript(@libcore.util.NonNull java.util.Locale inLocale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayCountry() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayCountry(@libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayVariant() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayVariant(@libcore.util.NonNull java.util.Locale inLocale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayName() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getDisplayName(@libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Object clone() { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.List<java.util.@libcore.util.NonNull Locale> filter(@libcore.util.NonNull java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> priorityList, @libcore.util.NonNull java.util.Collection<java.util.@libcore.util.NonNull Locale> locales, @libcore.util.NonNull java.util.Locale.FilteringMode mode) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.List<java.util.@libcore.util.NonNull Locale> filter(@libcore.util.NonNull java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> priorityList, @libcore.util.NonNull java.util.Collection<java.util.@libcore.util.NonNull Locale> locales) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.List<java.lang.@libcore.util.NonNull String> filterTags(@libcore.util.NonNull java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> priorityList, @libcore.util.NonNull java.util.Collection<java.lang.@libcore.util.NonNull String> tags, @libcore.util.NonNull java.util.Locale.FilteringMode mode) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.List<java.lang.@libcore.util.NonNull String> filterTags(@libcore.util.NonNull java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> priorityList, @libcore.util.NonNull java.util.Collection<java.lang.@libcore.util.NonNull String> tags) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public static java.util.Locale lookup(@libcore.util.NonNull java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> priorityList, @libcore.util.NonNull java.util.Collection<java.util.@libcore.util.NonNull Locale> locales) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public static java.lang.String lookupTag(@libcore.util.NonNull java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> priorityList, @libcore.util.NonNull java.util.Collection<java.lang.@libcore.util.NonNull String> tags) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final java.util.Locale CANADA;
static { CANADA = null; }

@libcore.util.NonNull public static final java.util.Locale CANADA_FRENCH;
static { CANADA_FRENCH = null; }

@libcore.util.NonNull public static final java.util.Locale CHINA;
static { CHINA = null; }

@libcore.util.NonNull public static final java.util.Locale CHINESE;
static { CHINESE = null; }

@libcore.util.NonNull public static final java.util.Locale ENGLISH;
static { ENGLISH = null; }

@libcore.util.NonNull public static final java.util.Locale FRANCE;
static { FRANCE = null; }

@libcore.util.NonNull public static final java.util.Locale FRENCH;
static { FRENCH = null; }

@libcore.util.NonNull public static final java.util.Locale GERMAN;
static { GERMAN = null; }

@libcore.util.NonNull public static final java.util.Locale GERMANY;
static { GERMANY = null; }

@libcore.util.NonNull public static final java.util.Locale ITALIAN;
static { ITALIAN = null; }

@libcore.util.NonNull public static final java.util.Locale ITALY;
static { ITALY = null; }

@libcore.util.NonNull public static final java.util.Locale JAPAN;
static { JAPAN = null; }

@libcore.util.NonNull public static final java.util.Locale JAPANESE;
static { JAPANESE = null; }

@libcore.util.NonNull public static final java.util.Locale KOREA;
static { KOREA = null; }

@libcore.util.NonNull public static final java.util.Locale KOREAN;
static { KOREAN = null; }

@libcore.util.NonNull public static final java.util.Locale PRC;
static { PRC = null; }

public static final char PRIVATE_USE_EXTENSION = 120; // 0x0078 'x'

@libcore.util.NonNull public static final java.util.Locale ROOT;
static { ROOT = null; }

@libcore.util.NonNull public static final java.util.Locale SIMPLIFIED_CHINESE;
static { SIMPLIFIED_CHINESE = null; }

@libcore.util.NonNull public static final java.util.Locale TAIWAN;
static { TAIWAN = null; }

@libcore.util.NonNull public static final java.util.Locale TRADITIONAL_CHINESE;
static { TRADITIONAL_CHINESE = null; }

@libcore.util.NonNull public static final java.util.Locale UK;
static { UK = null; }

public static final char UNICODE_LOCALE_EXTENSION = 117; // 0x0075 'u'

@libcore.util.NonNull public static final java.util.Locale US;
static { US = null; }

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static final class Builder {

public Builder() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setLocale(@libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setLanguageTag(@libcore.util.NonNull java.lang.String languageTag) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setLanguage(@libcore.util.Nullable java.lang.String language) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setScript(@libcore.util.Nullable java.lang.String script) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setRegion(@libcore.util.Nullable java.lang.String region) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setVariant(@libcore.util.Nullable java.lang.String variant) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setExtension(char key, @libcore.util.Nullable java.lang.String value) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder setUnicodeLocaleKeyword(@libcore.util.NonNull java.lang.String key, @libcore.util.Nullable java.lang.String type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder addUnicodeLocaleAttribute(@libcore.util.NonNull java.lang.String attribute) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder removeUnicodeLocaleAttribute(@libcore.util.NonNull java.lang.String attribute) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder clear() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale.Builder clearExtensions() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Locale build() { throw new RuntimeException("Stub!"); }
}

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static enum Category {
DISPLAY,
FORMAT;
}

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static enum FilteringMode {
AUTOSELECT_FILTERING,
EXTENDED_FILTERING,
IGNORE_EXTENDED_RANGES,
MAP_EXTENDED_RANGES,
REJECT_EXTENDED_RANGES;
}

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static final class LanguageRange {

public LanguageRange(@libcore.util.NonNull java.lang.String range) { throw new RuntimeException("Stub!"); }

public LanguageRange(@libcore.util.NonNull java.lang.String range, double weight) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getRange() { throw new RuntimeException("Stub!"); }

public double getWeight() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> parse(@libcore.util.NonNull java.lang.String ranges) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.List<java.util.Locale.LanguageRange> parse(@libcore.util.NonNull java.lang.String ranges, @libcore.util.NonNull java.util.Map<java.lang.@libcore.util.NonNull String,java.util.@libcore.util.NonNull List<java.lang.@libcore.util.NonNull String>> map) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.List<java.util.Locale.LanguageRange> mapEquivalents(@libcore.util.NonNull java.util.List<java.util.Locale.@libcore.util.NonNull LanguageRange> priorityList, @libcore.util.NonNull java.util.Map<java.lang.@libcore.util.NonNull String,java.util.@libcore.util.NonNull List<java.lang.@libcore.util.NonNull String>> map) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public static final double MAX_WEIGHT = 1.0;

public static final double MIN_WEIGHT = 0.0;
}

}
