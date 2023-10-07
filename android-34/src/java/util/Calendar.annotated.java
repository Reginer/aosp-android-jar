/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996-1998 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */


package java.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.time.Instant;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class Calendar implements java.io.Serializable, java.lang.Cloneable, java.lang.Comparable<java.util.Calendar> {

protected Calendar() { throw new RuntimeException("Stub!"); }

protected Calendar(@libcore.util.NonNull java.util.TimeZone zone, @libcore.util.NonNull java.util.Locale aLocale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Calendar getInstance() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Calendar getInstance(@libcore.util.NonNull java.util.TimeZone zone) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Calendar getInstance(@libcore.util.NonNull java.util.Locale aLocale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Calendar getInstance(@libcore.util.NonNull java.util.TimeZone zone, @libcore.util.NonNull java.util.Locale aLocale) { throw new RuntimeException("Stub!"); }

public static synchronized java.util.@libcore.util.NonNull Locale @libcore.util.NonNull [] getAvailableLocales() { throw new RuntimeException("Stub!"); }

protected abstract void computeTime();

protected abstract void computeFields();

@libcore.util.NonNull public final java.util.Date getTime() { throw new RuntimeException("Stub!"); }

public final void setTime(@libcore.util.NonNull java.util.Date date) { throw new RuntimeException("Stub!"); }

public long getTimeInMillis() { throw new RuntimeException("Stub!"); }

public void setTimeInMillis(long millis) { throw new RuntimeException("Stub!"); }

public int get(int field) { throw new RuntimeException("Stub!"); }

protected final int internalGet(int field) { throw new RuntimeException("Stub!"); }

public void set(int field, int value) { throw new RuntimeException("Stub!"); }

public final void set(int year, int month, int date) { throw new RuntimeException("Stub!"); }

public final void set(int year, int month, int date, int hourOfDay, int minute) { throw new RuntimeException("Stub!"); }

public final void set(int year, int month, int date, int hourOfDay, int minute, int second) { throw new RuntimeException("Stub!"); }

public final void clear() { throw new RuntimeException("Stub!"); }

public final void clear(int field) { throw new RuntimeException("Stub!"); }

public final boolean isSet(int field) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getDisplayName(int field, int style, @libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map<java.lang.@libcore.util.NonNull String, java.lang.@libcore.util.NonNull Integer> getDisplayNames(int field, int style, @libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

protected void complete() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.Set<java.lang.@libcore.util.NonNull String> getAvailableCalendarTypes() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getCalendarType() { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public boolean before(@libcore.util.Nullable java.lang.Object when) { throw new RuntimeException("Stub!"); }

public boolean after(@libcore.util.Nullable java.lang.Object when) { throw new RuntimeException("Stub!"); }

public int compareTo(@libcore.util.NonNull java.util.Calendar anotherCalendar) { throw new RuntimeException("Stub!"); }

public abstract void add(int field, int amount);

public abstract void roll(int field, boolean up);

public void roll(int field, int amount) { throw new RuntimeException("Stub!"); }

public void setTimeZone(@libcore.util.NonNull java.util.TimeZone value) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.TimeZone getTimeZone() { throw new RuntimeException("Stub!"); }

public void setLenient(boolean lenient) { throw new RuntimeException("Stub!"); }

public boolean isLenient() { throw new RuntimeException("Stub!"); }

public void setFirstDayOfWeek(int value) { throw new RuntimeException("Stub!"); }

public int getFirstDayOfWeek() { throw new RuntimeException("Stub!"); }

public void setMinimalDaysInFirstWeek(int value) { throw new RuntimeException("Stub!"); }

public int getMinimalDaysInFirstWeek() { throw new RuntimeException("Stub!"); }

public boolean isWeekDateSupported() { throw new RuntimeException("Stub!"); }

public int getWeekYear() { throw new RuntimeException("Stub!"); }

public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) { throw new RuntimeException("Stub!"); }

public int getWeeksInWeekYear() { throw new RuntimeException("Stub!"); }

public abstract int getMinimum(int field);

public abstract int getMaximum(int field);

public abstract int getGreatestMinimum(int field);

public abstract int getLeastMaximum(int field);

public int getActualMinimum(int field) { throw new RuntimeException("Stub!"); }

public int getActualMaximum(int field) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Object clone() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public final java.time.Instant toInstant() { throw new RuntimeException("Stub!"); }

public static final int ALL_STYLES = 0; // 0x0

public static final int AM = 0; // 0x0

public static final int AM_PM = 9; // 0x9

public static final int APRIL = 3; // 0x3

public static final int AUGUST = 7; // 0x7

public static final int DATE = 5; // 0x5

public static final int DAY_OF_MONTH = 5; // 0x5

public static final int DAY_OF_WEEK = 7; // 0x7

public static final int DAY_OF_WEEK_IN_MONTH = 8; // 0x8

public static final int DAY_OF_YEAR = 6; // 0x6

public static final int DECEMBER = 11; // 0xb

public static final int DST_OFFSET = 16; // 0x10

public static final int ERA = 0; // 0x0

public static final int FEBRUARY = 1; // 0x1

public static final int FIELD_COUNT = 17; // 0x11

public static final int FRIDAY = 6; // 0x6

public static final int HOUR = 10; // 0xa

public static final int HOUR_OF_DAY = 11; // 0xb

public static final int JANUARY = 0; // 0x0

public static final int JULY = 6; // 0x6

public static final int JUNE = 5; // 0x5

public static final int LONG = 2; // 0x2

public static final int LONG_FORMAT = 2; // 0x2

public static final int LONG_STANDALONE = 32770; // 0x8002

public static final int MARCH = 2; // 0x2

public static final int MAY = 4; // 0x4

public static final int MILLISECOND = 14; // 0xe

public static final int MINUTE = 12; // 0xc

public static final int MONDAY = 2; // 0x2

public static final int MONTH = 2; // 0x2

public static final int NARROW_FORMAT = 4; // 0x4

public static final int NARROW_STANDALONE = 32772; // 0x8004

public static final int NOVEMBER = 10; // 0xa

public static final int OCTOBER = 9; // 0x9

public static final int PM = 1; // 0x1

public static final int SATURDAY = 7; // 0x7

public static final int SECOND = 13; // 0xd

public static final int SEPTEMBER = 8; // 0x8

public static final int SHORT = 1; // 0x1

public static final int SHORT_FORMAT = 1; // 0x1

public static final int SHORT_STANDALONE = 32769; // 0x8001

public static final int SUNDAY = 1; // 0x1

public static final int THURSDAY = 5; // 0x5

public static final int TUESDAY = 3; // 0x3

public static final int UNDECIMBER = 12; // 0xc

public static final int WEDNESDAY = 4; // 0x4

public static final int WEEK_OF_MONTH = 4; // 0x4

public static final int WEEK_OF_YEAR = 3; // 0x3

public static final int YEAR = 1; // 0x1

public static final int ZONE_OFFSET = 15; // 0xf

protected boolean areFieldsSet;

protected int @libcore.util.NonNull [] fields;

protected boolean @libcore.util.NonNull [] isSet;

protected boolean isTimeSet;

protected long time;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static class Builder {

public Builder() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setInstant(long instant) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setInstant(@libcore.util.NonNull java.util.Date instant) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder set(int field, int value) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setFields(int @libcore.util.NonNull ... fieldValuePairs) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setDate(int year, int month, int dayOfMonth) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setTimeOfDay(int hourOfDay, int minute, int second) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setTimeOfDay(int hourOfDay, int minute, int second, int millis) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setTimeZone(@libcore.util.NonNull java.util.TimeZone zone) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setLenient(boolean lenient) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setCalendarType(@libcore.util.NonNull java.lang.String type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setLocale(@libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar.Builder setWeekDefinition(int firstDayOfWeek, int minimalDaysInFirstWeek) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Calendar build() { throw new RuntimeException("Stub!"); }
}

}
