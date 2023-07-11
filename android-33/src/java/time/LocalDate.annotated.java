/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package java.time;

import libcore.api.Hide;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class LocalDate implements java.time.temporal.Temporal, java.time.temporal.TemporalAdjuster, java.time.chrono.ChronoLocalDate, java.io.Serializable {

private LocalDate() { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate now() { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate now(java.time.ZoneId zone) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate now(java.time.Clock clock) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate of(int year, java.time.Month month, int dayOfMonth) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate of(int year, int month, int dayOfMonth) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate ofYearDay(int year, int dayOfYear) { throw new RuntimeException("Stub!"); }

@Hide
public static java.time.LocalDate ofInstant(java.time.Instant instant, java.time.ZoneId zone) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate ofEpochDay(long epochDay) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate from(java.time.temporal.TemporalAccessor temporal) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate parse(java.lang.CharSequence text) { throw new RuntimeException("Stub!"); }

public static java.time.LocalDate parse(java.lang.CharSequence text, java.time.format.DateTimeFormatter formatter) { throw new RuntimeException("Stub!"); }

public boolean isSupported(java.time.temporal.TemporalField field) { throw new RuntimeException("Stub!"); }

public boolean isSupported(java.time.temporal.TemporalUnit unit) { throw new RuntimeException("Stub!"); }

public java.time.temporal.ValueRange range(java.time.temporal.TemporalField field) { throw new RuntimeException("Stub!"); }

public int get(java.time.temporal.TemporalField field) { throw new RuntimeException("Stub!"); }

public long getLong(java.time.temporal.TemporalField field) { throw new RuntimeException("Stub!"); }

public java.time.chrono.IsoChronology getChronology() { throw new RuntimeException("Stub!"); }

public java.time.chrono.Era getEra() { throw new RuntimeException("Stub!"); }

public int getYear() { throw new RuntimeException("Stub!"); }

public int getMonthValue() { throw new RuntimeException("Stub!"); }

public java.time.Month getMonth() { throw new RuntimeException("Stub!"); }

public int getDayOfMonth() { throw new RuntimeException("Stub!"); }

public int getDayOfYear() { throw new RuntimeException("Stub!"); }

public java.time.DayOfWeek getDayOfWeek() { throw new RuntimeException("Stub!"); }

public boolean isLeapYear() { throw new RuntimeException("Stub!"); }

public int lengthOfMonth() { throw new RuntimeException("Stub!"); }

public int lengthOfYear() { throw new RuntimeException("Stub!"); }

public java.time.LocalDate with(java.time.temporal.TemporalAdjuster adjuster) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate with(java.time.temporal.TemporalField field, long newValue) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate withYear(int year) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate withMonth(int month) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate withDayOfMonth(int dayOfMonth) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate withDayOfYear(int dayOfYear) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate plus(java.time.temporal.TemporalAmount amountToAdd) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate plus(long amountToAdd, java.time.temporal.TemporalUnit unit) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate plusYears(long yearsToAdd) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate plusMonths(long monthsToAdd) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate plusWeeks(long weeksToAdd) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate plusDays(long daysToAdd) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate minus(java.time.temporal.TemporalAmount amountToSubtract) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate minus(long amountToSubtract, java.time.temporal.TemporalUnit unit) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate minusYears(long yearsToSubtract) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate minusMonths(long monthsToSubtract) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate minusWeeks(long weeksToSubtract) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate minusDays(long daysToSubtract) { throw new RuntimeException("Stub!"); }

public <R> R query(java.time.temporal.TemporalQuery<R> query) { throw new RuntimeException("Stub!"); }

public java.time.temporal.Temporal adjustInto(java.time.temporal.Temporal temporal) { throw new RuntimeException("Stub!"); }

public long until(java.time.temporal.Temporal endExclusive, java.time.temporal.TemporalUnit unit) { throw new RuntimeException("Stub!"); }

public java.time.Period until(java.time.chrono.ChronoLocalDate endDateExclusive) { throw new RuntimeException("Stub!"); }

@Hide
public java.util.stream.Stream<java.time.LocalDate> datesUntil(java.time.LocalDate endExclusive) { throw new RuntimeException("Stub!"); }

@Hide
public java.util.stream.Stream<java.time.LocalDate> datesUntil(java.time.LocalDate endExclusive, java.time.Period step) { throw new RuntimeException("Stub!"); }

public java.lang.String format(java.time.format.DateTimeFormatter formatter) { throw new RuntimeException("Stub!"); }

public java.time.LocalDateTime atTime(java.time.LocalTime time) { throw new RuntimeException("Stub!"); }

public java.time.LocalDateTime atTime(int hour, int minute) { throw new RuntimeException("Stub!"); }

public java.time.LocalDateTime atTime(int hour, int minute, int second) { throw new RuntimeException("Stub!"); }

public java.time.LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) { throw new RuntimeException("Stub!"); }

public java.time.OffsetDateTime atTime(java.time.OffsetTime time) { throw new RuntimeException("Stub!"); }

public java.time.LocalDateTime atStartOfDay() { throw new RuntimeException("Stub!"); }

public java.time.ZonedDateTime atStartOfDay(java.time.ZoneId zone) { throw new RuntimeException("Stub!"); }

public long toEpochDay() { throw new RuntimeException("Stub!"); }

@Hide
public long toEpochSecond(java.time.LocalTime time, java.time.ZoneOffset offset) { throw new RuntimeException("Stub!"); }

public int compareTo(java.time.chrono.ChronoLocalDate other) { throw new RuntimeException("Stub!"); }

public boolean isAfter(java.time.chrono.ChronoLocalDate other) { throw new RuntimeException("Stub!"); }

public boolean isBefore(java.time.chrono.ChronoLocalDate other) { throw new RuntimeException("Stub!"); }

public boolean isEqual(java.time.chrono.ChronoLocalDate other) { throw new RuntimeException("Stub!"); }

public boolean equals(java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@Hide
public static final java.time.LocalDate EPOCH;
static { EPOCH = null; }

public static final java.time.LocalDate MAX;
static { MAX = null; }

public static final java.time.LocalDate MIN;
static { MIN = null; }
}

