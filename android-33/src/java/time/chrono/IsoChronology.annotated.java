/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
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

package java.time.chrono;

import libcore.api.Hide;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class IsoChronology extends java.time.chrono.AbstractChronology implements java.io.Serializable {

private IsoChronology() { throw new RuntimeException("Stub!"); }

public java.lang.String getId() { throw new RuntimeException("Stub!"); }

public java.lang.String getCalendarType() { throw new RuntimeException("Stub!"); }

public java.time.LocalDate date(java.time.chrono.Era era, int yearOfEra, int month, int dayOfMonth) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate date(int prolepticYear, int month, int dayOfMonth) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate dateYearDay(java.time.chrono.Era era, int yearOfEra, int dayOfYear) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate dateYearDay(int prolepticYear, int dayOfYear) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate dateEpochDay(long epochDay) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate date(java.time.temporal.TemporalAccessor temporal) { throw new RuntimeException("Stub!"); }

@Hide
public long epochSecond(int prolepticYear, int month, int dayOfMonth, int hour, int minute, int second, java.time.ZoneOffset zoneOffset) { throw new RuntimeException("Stub!"); }

public java.time.LocalDateTime localDateTime(java.time.temporal.TemporalAccessor temporal) { throw new RuntimeException("Stub!"); }

public java.time.ZonedDateTime zonedDateTime(java.time.temporal.TemporalAccessor temporal) { throw new RuntimeException("Stub!"); }

public java.time.ZonedDateTime zonedDateTime(java.time.Instant instant, java.time.ZoneId zone) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate dateNow() { throw new RuntimeException("Stub!"); }

public java.time.LocalDate dateNow(java.time.ZoneId zone) { throw new RuntimeException("Stub!"); }

public java.time.LocalDate dateNow(java.time.Clock clock) { throw new RuntimeException("Stub!"); }

public boolean isLeapYear(long prolepticYear) { throw new RuntimeException("Stub!"); }

public int prolepticYear(java.time.chrono.Era era, int yearOfEra) { throw new RuntimeException("Stub!"); }

public java.time.chrono.IsoEra eraOf(int eraValue) { throw new RuntimeException("Stub!"); }

public java.util.List<java.time.chrono.Era> eras() { throw new RuntimeException("Stub!"); }

public java.time.LocalDate resolveDate(java.util.Map<java.time.temporal.TemporalField,java.lang.Long> fieldValues, java.time.format.ResolverStyle resolverStyle) { throw new RuntimeException("Stub!"); }

public java.time.temporal.ValueRange range(java.time.temporal.ChronoField field) { throw new RuntimeException("Stub!"); }

public java.time.Period period(int years, int months, int days) { throw new RuntimeException("Stub!"); }

public static final java.time.chrono.IsoChronology INSTANCE;
static { INSTANCE = null; }
}

