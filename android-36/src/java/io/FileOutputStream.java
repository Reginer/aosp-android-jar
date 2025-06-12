/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static android.system.OsConstants.O_APPEND;
import static android.system.OsConstants.O_CREAT;
import static android.system.OsConstants.O_TRUNC;
import static android.system.OsConstants.O_WRONLY;
import java.nio.channels.FileChannel;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaIOFileDescriptorAccess;
import sun.nio.ch.FileChannelImpl;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import libcore.io.IoBridge;
import libcore.io.IoTracker;
import libcore.io.IoUtils;

/**
 * A file output stream is an output stream for writing data to a
 * {@code File} or to a {@code FileDescriptor}. Whether or not
 * a file is available or may be created depends upon the underlying
 * platform.  Some platforms, in particular, allow a file to be opened
 * for writing by only one {@code FileOutputStream} (or other
 * file-writing object) at a time.  In such situations the constructors in
 * this class will fail if the file involved is already open.
 *
 * <p>{@code FileOutputStream} is meant for writing streams of raw bytes
 * such as image data. For writing streams of characters, consider using
 * {@code FileWriter}.
 *
 * @apiNote
 * To release resources used by this stream {@link #close} should be called
 * directly or by try-with-resources. Subclasses are responsible for the cleanup
 * of resources acquired by the subclass.
 * Subclasses that override {@link #finalize} in order to perform cleanup
 * should be modified to use alternative cleanup mechanisms such as
 * {@link java.lang.ref.Cleaner} and remove the overriding {@code finalize} method.
 *
 * @implSpec
 * If this FileOutputStream has been subclassed and the {@link #close}
 * method has been overridden, the {@link #close} method will be
 * called when the FileInputStream is unreachable.
 * Otherwise, it is implementation specific how the resource cleanup described in
 * {@link #close} is performed.
 *
 * @author  Arthur van Hoff
 * @see     java.io.File
 * @see     java.io.FileDescriptor
 * @see     java.io.FileInputStream
 * @see     java.nio.file.Files#newOutputStream
 * @since   1.0
 */
public class FileOutputStream extends OutputStream
{
    /**
     * Access to FileDescriptor internals.
     */
    // Android-removed: Remove unused fdAccess.
    // private static final JavaIOFileDescriptorAccess fdAccess =
    //     SharedSecrets.getJavaIOFileDescriptorAccess();

    /**
     * The system dependent file descriptor.
     */
    // Android-added: @ReachabilitySensitive
    @ReachabilitySensitive
    private final FileDescriptor fd;

    /**
     * The associated channel, initialized lazily.
     */
    private volatile FileChannel channel;

    /**
     * The path of the referenced file
     * (null if the stream is created with a file descriptor)
     */
    private final String path;

    private final Object closeLock = new Object();

    private volatile boolean closed;

    // Android-added: CloseGuard support: Log if the stream is not closed.
    @ReachabilitySensitive
    private final CloseGuard guard = CloseGuard.get();

    // Android-added: Field for tracking whether the stream owns the underlying FileDescriptor.
    private final boolean isFdOwner;

    // Android-added: Tracking of unbuffered I/O.
    private final IoTracker tracker = new IoTracker();

    /**
     * Creates a file output stream to write to the file with the
     * specified name. A new {@code FileDescriptor} object is
     * created to represent this file connection.
     * <p>
     * First, if there is a security manager, its {@code checkWrite}
     * method is called with {@code name} as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a {@code FileNotFoundException} is thrown.
     *
     * @implSpec Invoking this constructor with the parameter {@code name} is
     * equivalent to invoking {@link #FileOutputStream(String,boolean)
     * new FileOutputStream(name, false)}.
     *
     * @param      name   the system-dependent filename
     * @throws     FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason
     * @throws     SecurityException  if a security manager exists and its
     *               {@code checkWrite} method denies write access
     *               to the file.
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    public FileOutputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null, false);
    }

    /**
     * Creates a file output stream to write to the file with the specified
     * name.  If the second argument is {@code true}, then
     * bytes will be written to the end of the file rather than the beginning.
     * A new {@code FileDescriptor} object is created to represent this
     * file connection.
     * <p>
     * First, if there is a security manager, its {@code checkWrite}
     * method is called with {@code name} as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a {@code FileNotFoundException} is thrown.
     *
     * @param     name        the system-dependent file name
     * @param     append      if {@code true}, then bytes will be written
     *                   to the end of the file rather than the beginning
     * @throws     FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason.
     * @throws     SecurityException  if a security manager exists and its
     *               {@code checkWrite} method denies write access
     *               to the file.
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     * @since     1.1
     */
    public FileOutputStream(String name, boolean append)
        throws FileNotFoundException
    {
        this(name != null ? new File(name) : null, append);
    }

    /**
     * Creates a file output stream to write to the file represented by
     * the specified {@code File} object. A new
     * {@code FileDescriptor} object is created to represent this
     * file connection.
     * <p>
     * First, if there is a security manager, its {@code checkWrite}
     * method is called with the path represented by the {@code file}
     * argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a {@code FileNotFoundException} is thrown.
     *
     * @param      file               the file to be opened for writing.
     * @throws     FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason
     * @throws     SecurityException  if a security manager exists and its
     *               {@code checkWrite} method denies write access
     *               to the file.
     * @see        java.io.File#getPath()
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    /**
     * Creates a file output stream to write to the file represented by
     * the specified {@code File} object. If the second argument is
     * {@code true}, then bytes will be written to the end of the file
     * rather than the beginning. A new {@code FileDescriptor} object is
     * created to represent this file connection.
     * <p>
     * First, if there is a security manager, its {@code checkWrite}
     * method is called with the path represented by the {@code file}
     * argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a {@code FileNotFoundException} is thrown.
     *
     * @param      file               the file to be opened for writing.
     * @param     append      if {@code true}, then bytes will be written
     *                   to the end of the file rather than the beginning
     * @throws     FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason
     * @throws     SecurityException  if a security manager exists and its
     *               {@code checkWrite} method denies write access
     *               to the file.
     * @see        java.io.File#getPath()
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     * @since 1.4
     */
    public FileOutputStream(File file, boolean append)
        throws FileNotFoundException
    {
        String name = (file != null ? file.getPath() : null);
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkWrite(name);
        }
        if (name == null) {
            throw new NullPointerException();
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        // BEGIN Android-changed: Open files using IoBridge to share BlockGuard & StrictMode logic.
        // http://b/111268862
        // this.fd = new FileDescriptor();
        int flags = O_WRONLY | O_CREAT | (append ? O_APPEND : O_TRUNC);
        this.fd = IoBridge.open(name, flags);
        // END Android-changed: Open files using IoBridge to share BlockGuard & StrictMode logic.

        // Android-changed: Tracking mechanism for FileDescriptor sharing.
        // fd.attach(this);
        this.isFdOwner = true;

        this.path = name;

        // Android-removed: Open files using IoBridge to share BlockGuard & StrictMode logic.
        // open(name, append);

        // Android-added: File descriptor ownership tracking.
        IoUtils.setFdOwner(this.fd, this);

        // Android-added: CloseGuard support.
        guard.open("close");
        // Android-removed: TODO: Enable this when FileCleanable is imported to replace finalize().
        // FileCleanable.register(fd);   // open sets the fd, register the cleanup
    }

    // Android-removed: Documentation around SecurityException. Not thrown on Android.
    // Android-changed: Added doc for the Android-specific file descriptor ownership.
    /**
     * Creates a file output stream to write to the specified file
     * descriptor, which represents an existing connection to an actual
     * file in the file system.
     * <p>
     * First, if there is a security manager, its {@code checkWrite}
     * method is called with the file descriptor {@code fdObj}
     * argument as its argument.
     * <p>
     * If {@code fdObj} is null then a {@code NullPointerException}
     * is thrown.
     * <p>
     * This constructor does not throw an exception if {@code fdObj}
     * is {@link java.io.FileDescriptor#valid() invalid}.
     * However, if the methods are invoked on the resulting stream to attempt
     * I/O on the stream, an {@code IOException} is thrown.
     * <p>
     * Android-specific warning: {@link #close()} method doesn't close the {@code fdObj} provided,
     * because this object doesn't own the file descriptor, but the caller does. The caller can
     * call {@link android.system.Os#close(FileDescriptor)} to close the fd.
     *
     * @param      fdObj   the file descriptor to be opened for writing
     * @throws     SecurityException  if a security manager exists and its
     *               {@code checkWrite} method denies
     *               write access to the file descriptor
     * @see        java.lang.SecurityManager#checkWrite(java.io.FileDescriptor)
     */
    public FileOutputStream(FileDescriptor fdObj) {
        // Android-changed: Delegate to added hidden constructor.
        this(fdObj, false /* isOwner */);
    }

    // Android-added: Internal/hidden constructor for specifying FileDescriptor ownership.
    // Android-removed: SecurityManager calls.
    /**
     * Internal constructor for {@code FileOutputStream} objects where the file descriptor
     * is owned by this tream.
     *
     * @hide
     */
    public FileOutputStream(FileDescriptor fdObj, boolean isFdOwner) {
        if (fdObj == null) {
            // Android-changed: Improved NullPointerException message.
            throw new NullPointerException("fdObj == null");
        }

        this.fd = fdObj;
        this.path = null;

        // Android-changed: FileDescriptor ownership tracking mechanism.
        // fd.attach(this);
        this.isFdOwner = isFdOwner;
        if (isFdOwner) {
            IoUtils.setFdOwner(this.fd, this);
        }
    }

    // BEGIN Android-changed: Open files using IoBridge to share BlockGuard & StrictMode logic.
    // http://b/112107427
    /*
    /**
     * Opens a file, with the specified name, for overwriting or appending.
     * @param name name of file to be opened
     * @param append whether the file is to be opened in append mode
     *
    private native void open0(String name, boolean append)
        throws FileNotFoundException;

    // wrap native call to allow instrumentation
    /**
     * Opens a file, with the specified name, for overwriting or appending.
     * @param name name of file to be opened
     * @param append whether the file is to be opened in append mode
     *
    private void open(String name, boolean append)
        throws FileNotFoundException {
        open0(name, append);
    }
    */
    // END Android-changed: Open files using IoBridge to share BlockGuard & StrictMode logic.

    // Android-removed: write(int, boolean), use IoBridge instead.
    /*
    /**
     * Writes the specified byte to this file output stream.
     *
     * @param   b   the byte to be written.
     * @param   append   {@code true} if the write operation first
     *     advances the position to the end of file
     *
    private native void write(int b, boolean append) throws IOException;
    */

    /**
     * Writes the specified byte to this file output stream. Implements
     * the {@code write} method of {@code OutputStream}.
     *
     * @param      b   the byte to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    public void write(int b) throws IOException {
        // Android-changed: Write methods delegate to write(byte[],int,int) to share Android logic.
        // write(b, fdAccess.getAppend(fd));
        write(new byte[] { (byte) b }, 0, 1);
    }

    // Android-removed: Write methods delegate to write(byte[],int,int) to share Android logic.
    /*
    /**
     * Writes a sub array as a sequence of bytes.
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @param append {@code true} to first advance the position to the
     *     end of file
     * @throws    IOException If an I/O error has occurred.
     *
    private native void writeBytes(byte b[], int off, int len, boolean append)
        throws IOException;
    */

    /**
     * Writes {@code b.length} bytes from the specified byte array
     * to this file output stream.
     *
     * @param      b   the data.
     * @throws     IOException  if an I/O error occurs.
     */
    public void write(byte b[]) throws IOException {
        // Android-changed: Write methods delegate to write(byte[],int,int) to share Android logic.
        // writeBytes(b, 0, b.length, fdAccess.getAppend(fd));
        write(b, 0, b.length);
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to this file output stream.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @throws     IOException  if an I/O error occurs.
     */
    public void write(byte b[], int off, int len) throws IOException {
        // Android-added: close() check before I/O.
        if (closed && len > 0) {
            throw new IOException("Stream Closed");
        }

        // Android-added: Tracking of unbuffered I/O.
        tracker.trackIo(len, IoTracker.Mode.WRITE);

        // Android-changed: Use IoBridge instead of calling native method.
        // writeBytes(b, off, len, fdAccess.getAppend(fd));
        IoBridge.write(fd, b, off, len);
    }

    /**
     * Closes this file output stream and releases any system resources
     * associated with this stream. This file output stream may no longer
     * be used for writing bytes.
     *
     * <p> If this stream has an associated channel then the channel is closed
     * as well.
     *
     * @apiNote
     * Overriding {@link #close} to perform cleanup actions is reliable
     * only when called directly or when called by try-with-resources.
     * Do not depend on finalization to invoke {@code close};
     * finalization is not reliable and is deprecated.
     * If cleanup of native resources is needed, other mechanisms such as
     * {@linkplain java.lang.ref.Cleaner} should be used.
     *
     * @throws     IOException  if an I/O error occurs.
     *
     * @revised 1.4
     */
    public void close() throws IOException {
        if (closed) {
            return;
        }
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }

        // Android-added: CloseGuard support.
        guard.close();

        if (channel != null) {
            channel.close();
        }

        // BEGIN Android-changed: Close handling / notification of blocked threads.
        /*
        fd.closeAll(new Closeable() {
            public void close() throws IOException {
               fd.close();
           }
        });
         */
        if (isFdOwner) {
            IoBridge.closeAndSignalBlockedThreads(fd);
        }
        // END Android-changed: Close handling / notification of blocked threads.
    }

    /**
     * Returns the file descriptor associated with this stream.
     *
     * @return  the {@code FileDescriptor} object that represents
     *          the connection to the file in the file system being used
     *          by this {@code FileOutputStream} object.
     *
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.FileDescriptor
     */
     // Android-added: @ReachabilitySensitive
     @ReachabilitySensitive
     public final FileDescriptor getFD()  throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
     }

    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file output stream.
     *
     * <p> The initial {@link java.nio.channels.FileChannel#position()
     * position} of the returned channel will be equal to the
     * number of bytes written to the file so far unless this stream is in
     * append mode, in which case it will be equal to the size of the file.
     * Writing bytes to this stream will increment the channel's position
     * accordingly.  Changing the channel's position, either explicitly or by
     * writing, will change this stream's file position.
     *
     * @return  the file channel associated with this file output stream
     *
     * @since 1.4
     */
    public FileChannel getChannel() {
        FileChannel fc = this.channel;
        if (fc == null) {
            synchronized (this) {
                fc = this.channel;
                if (fc == null) {
                    this.channel = fc = FileChannelImpl.open(fd, path, false,
                        // Android-changed: TODO: remove patch when FileChannelImpl supports direct.
                        // This patch should cause no behavior change as direct is off by default.
                        // true, false, this);
                        true, this);
                    if (closed) {
                        try {
                            // possible race with close(), benign since
                            // FileChannel.close is final and idempotent
                            fc.close();
                        } catch (IOException ioe) {
                            throw new InternalError(ioe); // should not happen
                        }
                    }
                }
            }
        }
        return fc;
    }

    // TODO: Remove finalize() when FileCleanable is imported and used.
    /**
     * Cleans up the connection to the file, and ensures that the
     * <code>close</code> method of this file output stream is
     * called when there are no more references to this stream.
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FileInputStream#close()
     */
    protected void finalize() throws IOException {
        // Android-added: CloseGuard support.
        if (guard != null) {
            guard.warnIfOpen();
        }

        if (fd != null) {
            if (fd == FileDescriptor.out || fd == FileDescriptor.err) {
                flush();
            } else {
                // Android-removed: Obsoleted comment about shared FileDescriptor handling.
                close();
            }
        }
    }

    // BEGIN Android-removed: Unused code.
    /*
    private native void close0() throws IOException;

    private static native void initIDs();

    static {
        initIDs();
    }
    */
    // END Android-removed: Unused code.

}
