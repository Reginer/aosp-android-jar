/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class File implements java.io.Serializable, java.lang.Comparable<java.io.File> {

public File(@libcore.util.NonNull java.lang.String pathname) { throw new RuntimeException("Stub!"); }

public File(@libcore.util.Nullable java.lang.String parent, @libcore.util.NonNull java.lang.String child) { throw new RuntimeException("Stub!"); }

public File(@libcore.util.Nullable java.io.File parent, @libcore.util.NonNull java.lang.String child) { throw new RuntimeException("Stub!"); }

public File(@libcore.util.NonNull java.net.URI uri) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getName() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getParent() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.io.File getParentFile() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getPath() { throw new RuntimeException("Stub!"); }

public boolean isAbsolute() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getAbsolutePath() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.File getAbsoluteFile() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getCanonicalPath() throws java.io.IOException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.io.File getCanonicalFile() throws java.io.IOException { throw new RuntimeException("Stub!"); }

@Deprecated
@libcore.util.NonNull public java.net.URL toURL() throws java.net.MalformedURLException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.net.URI toURI() { throw new RuntimeException("Stub!"); }

public boolean canRead() { throw new RuntimeException("Stub!"); }

public boolean canWrite() { throw new RuntimeException("Stub!"); }

public boolean exists() { throw new RuntimeException("Stub!"); }

public boolean isDirectory() { throw new RuntimeException("Stub!"); }

public boolean isFile() { throw new RuntimeException("Stub!"); }

public boolean isHidden() { throw new RuntimeException("Stub!"); }

public long lastModified() { throw new RuntimeException("Stub!"); }

public long length() { throw new RuntimeException("Stub!"); }

public boolean createNewFile() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public boolean delete() { throw new RuntimeException("Stub!"); }

public void deleteOnExit() { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.NonNull String @libcore.util.Nullable [] list() { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.NonNull String @libcore.util.Nullable [] list(@libcore.util.Nullable java.io.FilenameFilter filter) { throw new RuntimeException("Stub!"); }

public java.io.@libcore.util.NonNull File @libcore.util.Nullable [] listFiles() { throw new RuntimeException("Stub!"); }

public java.io.@libcore.util.NonNull File @libcore.util.Nullable [] listFiles(@libcore.util.Nullable java.io.FilenameFilter filter) { throw new RuntimeException("Stub!"); }

public java.io.@libcore.util.NonNull File @libcore.util.Nullable [] listFiles(@libcore.util.Nullable java.io.FileFilter filter) { throw new RuntimeException("Stub!"); }

public boolean mkdir() { throw new RuntimeException("Stub!"); }

public boolean mkdirs() { throw new RuntimeException("Stub!"); }

public boolean renameTo(@libcore.util.NonNull java.io.File dest) { throw new RuntimeException("Stub!"); }

public boolean setLastModified(long time) { throw new RuntimeException("Stub!"); }

public boolean setReadOnly() { throw new RuntimeException("Stub!"); }

public boolean setWritable(boolean writable, boolean ownerOnly) { throw new RuntimeException("Stub!"); }

public boolean setWritable(boolean writable) { throw new RuntimeException("Stub!"); }

public boolean setReadable(boolean readable, boolean ownerOnly) { throw new RuntimeException("Stub!"); }

public boolean setReadable(boolean readable) { throw new RuntimeException("Stub!"); }

public boolean setExecutable(boolean executable, boolean ownerOnly) { throw new RuntimeException("Stub!"); }

public boolean setExecutable(boolean executable) { throw new RuntimeException("Stub!"); }

public boolean canExecute() { throw new RuntimeException("Stub!"); }

public static java.io.@libcore.util.NonNull File @libcore.util.NonNull [] listRoots() { throw new RuntimeException("Stub!"); }

public long getTotalSpace() { throw new RuntimeException("Stub!"); }

public long getFreeSpace() { throw new RuntimeException("Stub!"); }

public long getUsableSpace() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.io.File createTempFile(@libcore.util.NonNull java.lang.String prefix, @libcore.util.Nullable java.lang.String suffix, @libcore.util.Nullable java.io.File directory) throws java.io.IOException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.io.File createTempFile(@libcore.util.NonNull java.lang.String prefix, @libcore.util.Nullable java.lang.String suffix) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public int compareTo(@libcore.util.NonNull java.io.File pathname) { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.nio.file.Path toPath() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final java.lang.String pathSeparator;
static { pathSeparator = null; }

public static final char pathSeparatorChar;
static { pathSeparatorChar = 0; }

@libcore.util.NonNull public static final java.lang.String separator;
static { separator = null; }

public static final char separatorChar;
static { separatorChar = 0; }
}
