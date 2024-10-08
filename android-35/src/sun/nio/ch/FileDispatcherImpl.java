/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import dalvik.system.BlockGuard;

import java.io.FileDescriptor;
import java.io.IOException;

class FileDispatcherImpl extends FileDispatcher {

    // Android-removed: Code to load native libraries, doesn't make sense on Android.
    /*
    static {
        IOUtil.load();
        init();
    }
    */

    FileDispatcherImpl() {
    }

    int read(FileDescriptor fd, long address, int len) throws IOException {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return read0(fd, address, len);
    }

    int pread(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return pread0(fd, address, len, position);
    }

    long readv(FileDescriptor fd, long address, int len) throws IOException {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return readv0(fd, address, len);
    }

    int write(FileDescriptor fd, long address, int len) throws IOException {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return write0(fd, address, len);
    }

    int pwrite(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return pwrite0(fd, address, len, position);
    }

    long writev(FileDescriptor fd, long address, int len)
        throws IOException
    {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return writev0(fd, address, len);
    }

    int force(FileDescriptor fd, boolean metaData) throws IOException {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return force0(fd, metaData);
    }

    int truncate(FileDescriptor fd, long size) throws IOException {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return truncate0(fd, size);
    }

    long size(FileDescriptor fd) throws IOException {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return size0(fd);
    }

    int lock(FileDescriptor fd, boolean blocking, long pos, long size,
             boolean shared) throws IOException
    {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return lock0(fd, blocking, pos, size, shared);
    }

    void release(FileDescriptor fd, long pos, long size) throws IOException {
        // Android-added: BlockGuard support.
        BlockGuard.getThreadPolicy().onWriteToDisk();
        release0(fd, pos, size);
    }

    void close(FileDescriptor fd) throws IOException {
        close0(fd);
    }

    void preClose(FileDescriptor fd) throws IOException {
        preClose0(fd);
    }

    FileDescriptor duplicateForMapping(FileDescriptor fd) {
        // file descriptor not required for mapping operations; okay
        // to return invalid file descriptor.
        return new FileDescriptor();
    }

    boolean canTransferToDirectly(java.nio.channels.SelectableChannel sc) {
        return true;
    }

    boolean transferToDirectlyNeedsPositionLock() {
        return false;
    }

    // -- Native methods --

    static native int read0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int pread0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native long readv0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int write0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int pwrite0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native long writev0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int force0(FileDescriptor fd, boolean metaData)
        throws IOException;

    static native int truncate0(FileDescriptor fd, long size)
        throws IOException;

    static native long size0(FileDescriptor fd) throws IOException;

    static native int lock0(FileDescriptor fd, boolean blocking, long pos,
                            long size, boolean shared) throws IOException;

    static native void release0(FileDescriptor fd, long pos, long size)
        throws IOException;

    static native void close0(FileDescriptor fd) throws IOException;

    static native void preClose0(FileDescriptor fd) throws IOException;

    static native void closeIntFD(int fd) throws IOException;

    // Android-removed: Code to load native libraries, doesn't make sense on Android.
    // static native void init();

}
