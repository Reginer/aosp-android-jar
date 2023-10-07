package com.android.clockwork.time;

import android.content.Context;
import android.os.FileUtils;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TimeState {
    private static final String TAG = TimeState.class.getSimpleName();

    private static final int DIRECTORY_PERMISSION =
            FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IROTH | FileUtils.S_IXOTH;
    private static final int FILE_PERMISSION =
            FileUtils.S_IRUSR | FileUtils.S_IWUSR |
            FileUtils.S_IRGRP | FileUtils.S_IWGRP |
            FileUtils.S_IROTH | FileUtils.S_IWOTH;


    private File mDataDirectory;
    private File mLastTimeChangeFile = null;
    private File mTimeFormat12HourFlagFile = null;

    public TimeState(File dataDirectory) {
        mDataDirectory = dataDirectory;
    }

    public boolean init() {
        try {
            mDataDirectory.mkdirs();
            if (mDataDirectory.exists()) {
                FileUtils.setPermissions(mDataDirectory, DIRECTORY_PERMISSION, -1, -1);
            }
            if (!mDataDirectory.canWrite()) {
                Log.e(TAG, "Could not create writable system time directory: "
                      + mDataDirectory.getAbsolutePath());
                return false;
            }

            mLastTimeChangeFile = new File(mDataDirectory, "last_time_change");
            mLastTimeChangeFile.createNewFile();
            FileUtils.setPermissions(mLastTimeChangeFile, FILE_PERMISSION, -1, -1);
            if (!mLastTimeChangeFile.isFile() || !mLastTimeChangeFile.canWrite()) {
                Log.e(TAG, "Last time change file is not writable or is not a file: "
                      + mLastTimeChangeFile.getAbsolutePath());
                return false;
            }

            mTimeFormat12HourFlagFile = new File(mDataDirectory, "time_format_12_hour");
        } catch (SecurityException | IOException e) {
            Log.e(TAG, "Error when trying to create system time files", e);
            mLastTimeChangeFile = null;
            mTimeFormat12HourFlagFile = null;
            return false;
        }

        return true;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public boolean isSystem24HourFormat(Context context) {
        return DateFormat.is24HourFormat(context);
    }

    public boolean is12HourModeRecorded() {
        return mTimeFormat12HourFlagFile.isFile();
    }

    public boolean set12HourMode() {
        try {
            mTimeFormat12HourFlagFile.createNewFile();
            FileUtils.setPermissions(mTimeFormat12HourFlagFile, FILE_PERMISSION, -1, -1);
        } catch (SecurityException | IOException e) {
            Log.e(TAG, "Could not create 12 hour format file", e);
            return false;
        }
        return true;
    }

    public boolean set24HourMode() {
        try {
            mTimeFormat12HourFlagFile.delete();
        } catch (SecurityException e) {
            Log.e(TAG, "Could not delete 12 hour format file", e);
            return false;
        }
        return true;
    }

    public boolean updateLastChangeValue() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(mLastTimeChangeFile));
            writer.write(String.valueOf(getCurrentTime()));
            writer.newLine();
        } catch (IOException e) {
            Log.e(TAG, "Could not write last time change", e);
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                }  catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }
}
