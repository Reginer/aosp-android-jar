/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.modules.utils;

import android.util.Log;
import android.os.Binder;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Helper for implementing {@link Binder#onShellCommand Binder.onShellCommand}. This is meant to
 * be copied into mainline modules, so this class must not use any hidden APIs.
 */
public abstract class BasicShellCommandHandler {
    protected static final String TAG = "ShellCommand";
    protected static final boolean DEBUG = false;

    private Binder mTarget;
    private FileDescriptor mIn;
    private FileDescriptor mOut;
    private FileDescriptor mErr;
    private String[] mArgs;

    private String mCmd;
    private int mArgPos;
    private String mCurArgData;

    private FileInputStream mFileIn;
    private FileOutputStream mFileOut;
    private FileOutputStream mFileErr;

    private PrintWriter mOutPrintWriter;
    private PrintWriter mErrPrintWriter;
    private InputStream mInputStream;

    public void init(Binder target, FileDescriptor in, FileDescriptor out, FileDescriptor err,
            String[] args, int firstArgPos) {
        mTarget = target;
        mIn = in;
        mOut = out;
        mErr = err;
        mArgs = args;
        mCmd = null;
        mArgPos = firstArgPos;
        mCurArgData = null;
        mFileIn = null;
        mFileOut = null;
        mFileErr = null;
        mOutPrintWriter = null;
        mErrPrintWriter = null;
        mInputStream = null;
    }

    public int exec(Binder target, FileDescriptor in, FileDescriptor out, FileDescriptor err,
            String[] args) {
        String cmd;
        int start;
        if (args != null && args.length > 0) {
            cmd = args[0];
            start = 1;
        } else {
            cmd = null;
            start = 0;
        }
        init(target, in, out, err, args, start);
        mCmd = cmd;

        if (DEBUG) {
            RuntimeException here = new RuntimeException("here");
            here.fillInStackTrace();
            Log.d(TAG, "Starting command " + mCmd + " on " + mTarget, here);
            Log.d(TAG, "Calling uid=" + Binder.getCallingUid()
                    + " pid=" + Binder.getCallingPid());
        }
        int res = -1;
        try {
            res = onCommand(mCmd);
            if (DEBUG) Log.d(TAG, "Executed command " + mCmd + " on " + mTarget);
        } catch (Throwable e) {
            // Unlike usual calls, in this case if an exception gets thrown
            // back to us we want to print it back in to the dump data, since
            // that is where the caller expects all interesting information to
            // go.
            PrintWriter eout = getErrPrintWriter();
            eout.println();
            eout.println("Exception occurred while executing '" + mCmd + "':");
            e.printStackTrace(eout);
        } finally {
            if (DEBUG) Log.d(TAG, "Flushing output streams on " + mTarget);
            if (mOutPrintWriter != null) {
                mOutPrintWriter.flush();
            }
            if (mErrPrintWriter != null) {
                mErrPrintWriter.flush();
            }
            if (DEBUG) Log.d(TAG, "Sending command result on " + mTarget);
        }
        if (DEBUG) Log.d(TAG, "Finished command " + mCmd + " on " + mTarget);
        return res;
    }

    /**
     * Return the raw FileDescriptor for the output stream.
     */
    public FileDescriptor getOutFileDescriptor() {
        return mOut;
    }

    /**
     * Return direct raw access (not buffered) to the command's output data stream.
     */
    public OutputStream getRawOutputStream() {
        if (mFileOut == null) {
            mFileOut = new FileOutputStream(mOut);
        }
        return mFileOut;
    }

    /**
     * Return a PrintWriter for formatting output to {@link #getRawOutputStream()}.
     */
    public PrintWriter getOutPrintWriter() {
        if (mOutPrintWriter == null) {
            mOutPrintWriter = new PrintWriter(getRawOutputStream());
        }
        return mOutPrintWriter;
    }

    /**
     * Return the raw FileDescriptor for the error stream.
     */
    public FileDescriptor getErrFileDescriptor() {
        return mErr;
    }

    /**
     * Return direct raw access (not buffered) to the command's error output data stream.
     */
    public OutputStream getRawErrorStream() {
        if (mFileErr == null) {
            mFileErr = new FileOutputStream(mErr);
        }
        return mFileErr;
    }

    /**
     * Return a PrintWriter for formatting output to {@link #getRawErrorStream()}.
     */
    public PrintWriter getErrPrintWriter() {
        if (mErr == null) {
            return getOutPrintWriter();
        }
        if (mErrPrintWriter == null) {
            mErrPrintWriter = new PrintWriter(getRawErrorStream());
        }
        return mErrPrintWriter;
    }

    /**
     * Return the raw FileDescriptor for the input stream.
     */
    public FileDescriptor getInFileDescriptor() {
        return mIn;
    }

    /**
     * Return direct raw access (not buffered) to the command's input data stream.
     */
    public InputStream getRawInputStream() {
        if (mFileIn == null) {
            mFileIn = new FileInputStream(mIn);
        }
        return mFileIn;
    }

    /**
     * Return buffered access to the command's {@link #getRawInputStream()}.
     */
    public InputStream getBufferedInputStream() {
        if (mInputStream == null) {
            mInputStream = new BufferedInputStream(getRawInputStream());
        }
        return mInputStream;
    }

    /**
     * Return the next option on the command line -- that is an argument that
     * starts with '-'.  If the next argument is not an option, null is returned.
     */
    public String getNextOption() {
        if (mCurArgData != null) {
            String prev = mArgs[mArgPos - 1];
            throw new IllegalArgumentException("No argument expected after \"" + prev + "\"");
        }
        if (mArgPos >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mArgPos];
        if (!arg.startsWith("-")) {
            return null;
        }
        mArgPos++;
        if (arg.equals("--")) {
            return null;
        }
        if (arg.length() > 1 && arg.charAt(1) != '-') {
            if (arg.length() > 2) {
                mCurArgData = arg.substring(2);
                return arg.substring(0, 2);
            } else {
                mCurArgData = null;
                return arg;
            }
        }
        mCurArgData = null;
        return arg;
    }

    /**
     * Return the next argument on the command line, whatever it is; if there are
     * no arguments left, return null.
     */
    public String getNextArg() {
        if (mCurArgData != null) {
            String arg = mCurArgData;
            mCurArgData = null;
            return arg;
        } else if (mArgPos < mArgs.length) {
            return mArgs[mArgPos++];
        } else {
            return null;
        }
    }

    public String peekNextArg() {
        if (mCurArgData != null) {
            return mCurArgData;
        } else if (mArgPos < mArgs.length) {
            return mArgs[mArgPos];
        } else {
            return null;
        }
    }

    /**
     * @return all the remaining arguments in the command without moving the current position.
     */
    public String[] peekRemainingArgs() {
        int remaining = getRemainingArgsCount();
        String[] args = new String[remaining];
        for (int pos = mArgPos; pos < mArgs.length; pos++) {
            args[pos - mArgPos] = mArgs[pos];
        }
        return args;
    }

    /**
     * Returns number of arguments that haven't been processed yet.
     */
    public int getRemainingArgsCount() {
        if (mArgPos >= mArgs.length) {
            return 0;
        }
        return mArgs.length - mArgPos;
    }

    /**
     * Return the next argument on the command line, whatever it is; if there are
     * no arguments left, throws an IllegalArgumentException to report this to the user.
     */
    public String getNextArgRequired() {
        String arg = getNextArg();
        if (arg == null) {
            String prev = mArgs[mArgPos - 1];
            throw new IllegalArgumentException("Argument expected after \"" + prev + "\"");
        }
        return arg;
    }

    public int handleDefaultCommands(String cmd) {
        if (cmd == null || "help".equals(cmd) || "-h".equals(cmd)) {
            onHelp();
        } else {
            getOutPrintWriter().println("Unknown command: " + cmd);
        }
        return -1;
    }

    public Binder getTarget() {
        return mTarget;
    }

    public String[] getAllArgs() {
        return mArgs;
    }

    /**
     * Implement parsing and execution of a command.  If it isn't a command you understand,
     * call {@link #handleDefaultCommands(String)} and return its result as a last resort.
     * Use {@link #getNextOption()}, {@link #getNextArg()}, and {@link #getNextArgRequired()}
     * to process additional command line arguments.  Command output can be written to
     * {@link #getOutPrintWriter()} and errors to {@link #getErrPrintWriter()}.
     *
     * <p class="caution">Note that no permission checking has been done before entering this
     * function, so you need to be sure to do your own security verification for any commands you
     * are executing.  The easiest way to do this is to have the ShellCommand contain
     * only a reference to your service's aidl interface, and do all of your command
     * implementations on top of that -- that way you can rely entirely on your executing security
     * code behind that interface.</p>
     *
     * @param cmd The first command line argument representing the name of the command to execute.
     * @return Return the command result; generally 0 or positive indicates success and
     * negative values indicate error.
     */
    public abstract int onCommand(String cmd);

    /**
     * Implement this to print help text about your command to {@link #getOutPrintWriter()}.
     */
    public abstract void onHelp();
}
