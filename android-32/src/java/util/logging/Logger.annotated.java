/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
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



package java.util.logging;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.MissingResourceException;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Logger {

protected Logger(@libcore.util.Nullable java.lang.String name, @libcore.util.Nullable java.lang.String resourceBundleName) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final java.util.logging.Logger getGlobal() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.logging.Logger getLogger(@libcore.util.NonNull java.lang.String name) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.logging.Logger getLogger(@libcore.util.NonNull java.lang.String name, @libcore.util.Nullable java.lang.String resourceBundleName) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.logging.Logger getAnonymousLogger() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.logging.Logger getAnonymousLogger(@libcore.util.Nullable java.lang.String resourceBundleName) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.ResourceBundle getResourceBundle() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getResourceBundleName() { throw new RuntimeException("Stub!"); }

public void setFilter(@libcore.util.Nullable java.util.logging.Filter newFilter) throws java.lang.SecurityException { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.logging.Filter getFilter() { throw new RuntimeException("Stub!"); }

public void log(@libcore.util.NonNull java.util.logging.LogRecord record) { throw new RuntimeException("Stub!"); }

public void log(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void log(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void log(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String msg, @libcore.util.Nullable java.lang.Object param1) { throw new RuntimeException("Stub!"); }

public void log(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String msg, java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] params) { throw new RuntimeException("Stub!"); }

public void log(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String msg, @libcore.util.Nullable java.lang.Throwable thrown) { throw new RuntimeException("Stub!"); }

public void log(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.Throwable thrown, @libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void logp(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void logp(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void logp(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String msg, @libcore.util.Nullable java.lang.Object param1) { throw new RuntimeException("Stub!"); }

public void logp(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String msg, java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] params) { throw new RuntimeException("Stub!"); }

public void logp(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String msg, @libcore.util.Nullable java.lang.Throwable thrown) { throw new RuntimeException("Stub!"); }

public void logp(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.Throwable thrown, @libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

@Deprecated
public void logrb(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String bundleName, @libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

@Deprecated
public void logrb(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String bundleName, @libcore.util.Nullable java.lang.String msg, @libcore.util.Nullable java.lang.Object param1) { throw new RuntimeException("Stub!"); }

@Deprecated
public void logrb(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String bundleName, @libcore.util.Nullable java.lang.String msg, java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] params) { throw new RuntimeException("Stub!"); }

public void logrb(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.util.ResourceBundle bundle, @libcore.util.Nullable java.lang.String msg, java.lang.@libcore.util.Nullable Object @libcore.util.Nullable ... params) { throw new RuntimeException("Stub!"); }

@Deprecated
public void logrb(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.String bundleName, @libcore.util.Nullable java.lang.String msg, @libcore.util.Nullable java.lang.Throwable thrown) { throw new RuntimeException("Stub!"); }

public void logrb(@libcore.util.NonNull java.util.logging.Level level, @libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.util.ResourceBundle bundle, @libcore.util.Nullable java.lang.String msg, @libcore.util.Nullable java.lang.Throwable thrown) { throw new RuntimeException("Stub!"); }

public void entering(@libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod) { throw new RuntimeException("Stub!"); }

public void entering(@libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.Object param1) { throw new RuntimeException("Stub!"); }

public void entering(@libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, java.lang.@libcore.util.Nullable Object @libcore.util.Nullable [] params) { throw new RuntimeException("Stub!"); }

public void exiting(@libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod) { throw new RuntimeException("Stub!"); }

public void exiting(@libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.Object result) { throw new RuntimeException("Stub!"); }

public void throwing(@libcore.util.Nullable java.lang.String sourceClass, @libcore.util.Nullable java.lang.String sourceMethod, @libcore.util.Nullable java.lang.Throwable thrown) { throw new RuntimeException("Stub!"); }

public void severe(@libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void warning(@libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void info(@libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void config(@libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void fine(@libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void finer(@libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void finest(@libcore.util.Nullable java.lang.String msg) { throw new RuntimeException("Stub!"); }

public void severe(@libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void warning(@libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void info(@libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void config(@libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void fine(@libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void finer(@libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void finest(@libcore.util.NonNull java.util.function.Supplier<java.lang.@libcore.util.Nullable String> msgSupplier) { throw new RuntimeException("Stub!"); }

public void setLevel(@libcore.util.Nullable java.util.logging.Level newLevel) throws java.lang.SecurityException { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.logging.Level getLevel() { throw new RuntimeException("Stub!"); }

public boolean isLoggable(@libcore.util.NonNull java.util.logging.Level level) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getName() { throw new RuntimeException("Stub!"); }

public void addHandler(@libcore.util.NonNull java.util.logging.Handler handler) throws java.lang.SecurityException { throw new RuntimeException("Stub!"); }

public void removeHandler(@libcore.util.Nullable java.util.logging.Handler handler) throws java.lang.SecurityException { throw new RuntimeException("Stub!"); }

public java.util.logging.@libcore.util.NonNull Handler @libcore.util.NonNull [] getHandlers() { throw new RuntimeException("Stub!"); }

public void setUseParentHandlers(boolean useParentHandlers) { throw new RuntimeException("Stub!"); }

public boolean getUseParentHandlers() { throw new RuntimeException("Stub!"); }

public void setResourceBundle(@libcore.util.NonNull java.util.ResourceBundle bundle) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.logging.Logger getParent() { throw new RuntimeException("Stub!"); }

public void setParent(@libcore.util.NonNull java.util.logging.Logger parent) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final java.lang.String GLOBAL_LOGGER_NAME = "global";

@Deprecated @libcore.util.NonNull public static final java.util.logging.Logger global;
static { global = null; }
}
