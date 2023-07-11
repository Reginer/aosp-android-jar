/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
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

package java.time.format;

import libcore.api.Hide;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class DateTimeFormatter {

DateTimeFormatter() { throw new RuntimeException("Stub!"); }

public static java.time.format.DateTimeFormatter ofPattern(java.lang.String pattern) { throw new RuntimeException("Stub!"); }

public static java.time.format.DateTimeFormatter ofPattern(java.lang.String pattern, java.util.Locale locale) { throw new RuntimeException("Stub!"); }

public static java.time.format.DateTimeFormatter ofLocalizedDate(java.time.format.FormatStyle dateStyle) { throw new RuntimeException("Stub!"); }

public static java.time.format.DateTimeFormatter ofLocalizedTime(java.time.format.FormatStyle timeStyle) { throw new RuntimeException("Stub!"); }

public static java.time.format.DateTimeFormatter ofLocalizedDateTime(java.time.format.FormatStyle dateTimeStyle) { throw new RuntimeException("Stub!"); }

public static java.time.format.DateTimeFormatter ofLocalizedDateTime(java.time.format.FormatStyle dateStyle, java.time.format.FormatStyle timeStyle) { throw new RuntimeException("Stub!"); }

public static java.time.temporal.TemporalQuery<java.time.Period> parsedExcessDays() { throw new RuntimeException("Stub!"); }

public static java.time.temporal.TemporalQuery<java.lang.Boolean> parsedLeapSecond() { throw new RuntimeException("Stub!"); }

public java.util.Locale getLocale() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter withLocale(java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@Hide
public java.time.format.DateTimeFormatter localizedBy(java.util.Locale locale) { throw new RuntimeException("Stub!"); }

public java.time.format.DecimalStyle getDecimalStyle() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter withDecimalStyle(java.time.format.DecimalStyle decimalStyle) { throw new RuntimeException("Stub!"); }

public java.time.chrono.Chronology getChronology() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter withChronology(java.time.chrono.Chronology chrono) { throw new RuntimeException("Stub!"); }

public java.time.ZoneId getZone() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter withZone(java.time.ZoneId zone) { throw new RuntimeException("Stub!"); }

public java.time.format.ResolverStyle getResolverStyle() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter withResolverStyle(java.time.format.ResolverStyle resolverStyle) { throw new RuntimeException("Stub!"); }

public java.util.Set<java.time.temporal.TemporalField> getResolverFields() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter withResolverFields(java.time.temporal.TemporalField... resolverFields) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter withResolverFields(java.util.Set<java.time.temporal.TemporalField> resolverFields) { throw new RuntimeException("Stub!"); }

public java.lang.String format(java.time.temporal.TemporalAccessor temporal) { throw new RuntimeException("Stub!"); }

public void formatTo(java.time.temporal.TemporalAccessor temporal, java.lang.Appendable appendable) { throw new RuntimeException("Stub!"); }

public java.time.temporal.TemporalAccessor parse(java.lang.CharSequence text) { throw new RuntimeException("Stub!"); }

public java.time.temporal.TemporalAccessor parse(java.lang.CharSequence text, java.text.ParsePosition position) { throw new RuntimeException("Stub!"); }

public <T> T parse(java.lang.CharSequence text, java.time.temporal.TemporalQuery<T> query) { throw new RuntimeException("Stub!"); }

public java.time.temporal.TemporalAccessor parseBest(java.lang.CharSequence text, java.time.temporal.TemporalQuery<?>... queries) { throw new RuntimeException("Stub!"); }

public java.time.temporal.TemporalAccessor parseUnresolved(java.lang.CharSequence text, java.text.ParsePosition position) { throw new RuntimeException("Stub!"); }

public java.text.Format toFormat() { throw new RuntimeException("Stub!"); }

public java.text.Format toFormat(java.time.temporal.TemporalQuery<?> parseQuery) { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public static final java.time.format.DateTimeFormatter BASIC_ISO_DATE;
static { BASIC_ISO_DATE = null; }

public static final java.time.format.DateTimeFormatter ISO_DATE;
static { ISO_DATE = null; }

public static final java.time.format.DateTimeFormatter ISO_DATE_TIME;
static { ISO_DATE_TIME = null; }

public static final java.time.format.DateTimeFormatter ISO_INSTANT;
static { ISO_INSTANT = null; }

public static final java.time.format.DateTimeFormatter ISO_LOCAL_DATE;
static { ISO_LOCAL_DATE = null; }

public static final java.time.format.DateTimeFormatter ISO_LOCAL_DATE_TIME;
static { ISO_LOCAL_DATE_TIME = null; }

public static final java.time.format.DateTimeFormatter ISO_LOCAL_TIME;
static { ISO_LOCAL_TIME = null; }

public static final java.time.format.DateTimeFormatter ISO_OFFSET_DATE;
static { ISO_OFFSET_DATE = null; }

public static final java.time.format.DateTimeFormatter ISO_OFFSET_DATE_TIME;
static { ISO_OFFSET_DATE_TIME = null; }

public static final java.time.format.DateTimeFormatter ISO_OFFSET_TIME;
static { ISO_OFFSET_TIME = null; }

public static final java.time.format.DateTimeFormatter ISO_ORDINAL_DATE;
static { ISO_ORDINAL_DATE = null; }

public static final java.time.format.DateTimeFormatter ISO_TIME;
static { ISO_TIME = null; }

public static final java.time.format.DateTimeFormatter ISO_WEEK_DATE;
static { ISO_WEEK_DATE = null; }

public static final java.time.format.DateTimeFormatter ISO_ZONED_DATE_TIME;
static { ISO_ZONED_DATE_TIME = null; }

public static final java.time.format.DateTimeFormatter RFC_1123_DATE_TIME;
static { RFC_1123_DATE_TIME = null; }
}

