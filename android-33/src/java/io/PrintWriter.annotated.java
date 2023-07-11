/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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


package java.io;

import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.Locale;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class PrintWriter extends java.io.Writer {

public PrintWriter(@libcore.util.NonNull java.io.Writer out) { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.io.Writer out, boolean autoFlush) { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.io.OutputStream out) { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.io.OutputStream out, boolean autoFlush) { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.io.OutputStream out, boolean autoFlush, @libcore.util.NonNull java.nio.charset.Charset charset) { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.lang.String fileName) throws java.io.FileNotFoundException { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.lang.String fileName, @libcore.util.NonNull java.lang.String csn) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.lang.String fileName, @libcore.util.NonNull java.nio.charset.Charset charset) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.io.File file) throws java.io.FileNotFoundException { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.io.File file, @libcore.util.NonNull java.nio.charset.Charset charset) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public PrintWriter(@libcore.util.NonNull java.io.File file, @libcore.util.NonNull java.lang.String csn) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("Stub!"); }

public void flush() { throw new RuntimeException("Stub!"); }

public void close() { throw new RuntimeException("Stub!"); }

public boolean checkError() { throw new RuntimeException("Stub!"); }

protected void setError() { throw new RuntimeException("Stub!"); }

protected void clearError() { throw new RuntimeException("Stub!"); }

public void write(int c) { throw new RuntimeException("Stub!"); }

public void write(char[] buf, int off, int len) { throw new RuntimeException("Stub!"); }

public void write(char[] buf) { throw new RuntimeException("Stub!"); }

public void write(@libcore.util.NonNull java.lang.String s, int off, int len) { throw new RuntimeException("Stub!"); }

public void write(@libcore.util.NonNull java.lang.String s) { throw new RuntimeException("Stub!"); }

public void print(boolean b) { throw new RuntimeException("Stub!"); }

public void print(char c) { throw new RuntimeException("Stub!"); }

public void print(int i) { throw new RuntimeException("Stub!"); }

public void print(long l) { throw new RuntimeException("Stub!"); }

public void print(float f) { throw new RuntimeException("Stub!"); }

public void print(double d) { throw new RuntimeException("Stub!"); }

public void print(char[] s) { throw new RuntimeException("Stub!"); }

public void print(@libcore.util.Nullable java.lang.String s) { throw new RuntimeException("Stub!"); }

public void print(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public void println() { throw new RuntimeException("Stub!"); }

public void println(boolean x) { throw new RuntimeException("Stub!"); }

public void println(char x) { throw new RuntimeException("Stub!"); }

public void println(int x) { throw new RuntimeException("Stub!"); }

public void println(long x) { throw new RuntimeException("Stub!"); }

public void println(float x) { throw new RuntimeException("Stub!"); }

public void println(double x) { throw new RuntimeException("Stub!"); }

public void println(char[] x) { throw new RuntimeException("Stub!"); }

public void println(@libcore.util.Nullable java.lang.String x) { throw new RuntimeException("Stub!"); }

public void println(@libcore.util.Nullable java.lang.Object x) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.PrintWriter printf(@libcore.util.NonNull java.lang.String format, java.lang.@libcore.util.Nullable Object @libcore.util.NonNull ... args) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.PrintWriter printf(@libcore.util.Nullable java.util.Locale l, @libcore.util.NonNull java.lang.String format, java.lang.@libcore.util.Nullable Object @libcore.util.NonNull ... args) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.PrintWriter format(@libcore.util.NonNull java.lang.String format, java.lang.@libcore.util.Nullable Object @libcore.util.NonNull ... args) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.PrintWriter format(@libcore.util.Nullable java.util.Locale l, @libcore.util.NonNull java.lang.String format, java.lang.@libcore.util.Nullable Object @libcore.util.NonNull ... args) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.PrintWriter append(@libcore.util.Nullable java.lang.CharSequence csq) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.PrintWriter append(@libcore.util.Nullable java.lang.CharSequence csq, int start, int end) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.PrintWriter append(char c) { throw new RuntimeException("Stub!"); }

protected java.io.Writer out;
}
