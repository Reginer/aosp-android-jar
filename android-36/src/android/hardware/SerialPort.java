/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @hide
 */
public class SerialPort {

    private static final String TAG = "SerialPort";

    // used by the JNI code
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mNativeContext;
    private final String mName;
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * SerialPort should only be instantiated by SerialManager
     * @hide
     */
    public SerialPort(String name) {
        mName = name;
    }

    /**
     * SerialPort should only be instantiated by SerialManager
     * Speed must be one of 50, 75, 110, 134, 150, 200, 300, 600, 1200, 1800, 2400, 4800, 9600,
     * 19200, 38400, 57600, 115200, 230400, 460800, 500000, 576000, 921600, 1000000, 1152000,
     * 1500000, 2000000, 2500000, 3000000, 3500000, 4000000
     *
     * @hide
     */
    public void open(ParcelFileDescriptor pfd, int speed) throws IOException {
        native_open(pfd.getFileDescriptor(), speed);
        mFileDescriptor = pfd;
    }

    /**
     * Closes the serial port
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void close() throws IOException {
        if (mFileDescriptor != null) {
            mFileDescriptor.close();
            mFileDescriptor = null;
        }
        native_close();
    }

    /**
     * Returns the name of the serial port
     *
     * @return the serial port's name
     */
    public String getName() {
        return mName;
    }

    /**
     * Reads data into the provided buffer.
     * Note that the value returned by {@link java.nio.Buffer#position()} on this buffer is
     * unchanged after a call to this method.
     *
     * @param buffer to read into
     * @return number of bytes read
     */
    public int read(ByteBuffer buffer) throws IOException {
        if (buffer.isDirect()) {
            return native_read_direct(buffer, buffer.remaining());
        } else if (buffer.hasArray()) {
            return native_read_array(buffer.array(), buffer.remaining());
        } else {
            throw new IllegalArgumentException("buffer is not direct and has no array");
        }
    }

    /**
     * Writes data from provided buffer.
     * Note that the value returned by {@link java.nio.Buffer#position()} on this buffer is
     * unchanged after a call to this method.
     *
     * @param buffer to write
     * @param length number of bytes to write
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void write(ByteBuffer buffer, int length) throws IOException {
        if (buffer.isDirect()) {
            native_write_direct(buffer, length);
        } else if (buffer.hasArray()) {
            native_write_array(buffer.array(), length);
        } else {
            throw new IllegalArgumentException("buffer is not direct and has no array");
        }
    }

    /**
     * Sends a stream of zero valued bits for 0.25 to 0.5 seconds
     */
    public void sendBreak() {
        native_send_break();
    }

    private native void native_open(FileDescriptor pfd, int speed) throws IOException;
    private native void native_close();
    private native int native_read_array(byte[] buffer, int length) throws IOException;
    private native int native_read_direct(ByteBuffer buffer, int length) throws IOException;
    private native void native_write_array(byte[] buffer, int length) throws IOException;
    private native void native_write_direct(ByteBuffer buffer, int length) throws IOException;
    private native void native_send_break();
}
