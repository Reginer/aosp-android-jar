/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
public final class DateTimeFormatterBuilder {

public DateTimeFormatterBuilder() { throw new RuntimeException("Stub!"); }

public static java.lang.String getLocalizedDateTimePattern(java.time.format.FormatStyle dateStyle, java.time.format.FormatStyle timeStyle, java.time.chrono.Chronology chrono, java.util.Locale locale) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder parseCaseSensitive() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder parseCaseInsensitive() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder parseStrict() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder parseLenient() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder parseDefaulting(java.time.temporal.TemporalField field, long value) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendValue(java.time.temporal.TemporalField field) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendValue(java.time.temporal.TemporalField field, int width) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendValue(java.time.temporal.TemporalField field, int minWidth, int maxWidth, java.time.format.SignStyle signStyle) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendValueReduced(java.time.temporal.TemporalField field, int width, int maxWidth, int baseValue) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendValueReduced(java.time.temporal.TemporalField field, int width, int maxWidth, java.time.chrono.ChronoLocalDate baseDate) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendFraction(java.time.temporal.TemporalField field, int minWidth, int maxWidth, boolean decimalPoint) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendText(java.time.temporal.TemporalField field) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendText(java.time.temporal.TemporalField field, java.time.format.TextStyle textStyle) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendText(java.time.temporal.TemporalField field, java.util.Map<java.lang.Long,java.lang.String> textLookup) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendInstant() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendInstant(int fractionalDigits) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendOffsetId() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendOffset(java.lang.String pattern, java.lang.String noOffsetText) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendLocalizedOffset(java.time.format.TextStyle style) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendZoneId() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendZoneRegionId() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendZoneOrOffsetId() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendZoneText(java.time.format.TextStyle textStyle) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendZoneText(java.time.format.TextStyle textStyle, java.util.Set<java.time.ZoneId> preferredZones) { throw new RuntimeException("Stub!"); }

@Hide
public java.time.format.DateTimeFormatterBuilder appendGenericZoneText(java.time.format.TextStyle textStyle) { throw new RuntimeException("Stub!"); }

@Hide
public java.time.format.DateTimeFormatterBuilder appendGenericZoneText(java.time.format.TextStyle textStyle, java.util.Set<java.time.ZoneId> preferredZones) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendChronologyId() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendChronologyText(java.time.format.TextStyle textStyle) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendLocalized(java.time.format.FormatStyle dateStyle, java.time.format.FormatStyle timeStyle) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendLiteral(char literal) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendLiteral(java.lang.String literal) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder append(java.time.format.DateTimeFormatter formatter) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendOptional(java.time.format.DateTimeFormatter formatter) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder appendPattern(java.lang.String pattern) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder padNext(int padWidth) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder padNext(int padWidth, char padChar) { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder optionalStart() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatterBuilder optionalEnd() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter toFormatter() { throw new RuntimeException("Stub!"); }

public java.time.format.DateTimeFormatter toFormatter(java.util.Locale locale) { throw new RuntimeException("Stub!"); }
}

