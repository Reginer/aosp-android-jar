/*
 * Copyright 2019 The Android Open Source Project
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

package android.media.tv.tuner.dvr;

import android.annotation.BytesLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.media.tv.tuner.Tuner;
import android.media.tv.tuner.Tuner.Result;
import android.media.tv.tuner.TunerUtils;
import android.media.tv.tuner.TunerVersionChecker;
import android.media.tv.tuner.filter.Filter;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Log;

import com.android.internal.util.FrameworkStatsLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Digital Video Record (DVR) class which provides playback control on Demux's input buffer.
 *
 * <p>It's used to play recorded programs.
 *
 * @hide
 */
@SystemApi
public class DvrPlayback implements AutoCloseable {


    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "PLAYBACK_STATUS_",
            value = {PLAYBACK_STATUS_EMPTY, PLAYBACK_STATUS_ALMOST_EMPTY,
                    PLAYBACK_STATUS_ALMOST_FULL, PLAYBACK_STATUS_FULL})
    @interface PlaybackStatus {}

    /**
     * The space of the playback is empty.
     */
    public static final int PLAYBACK_STATUS_EMPTY =
            android.hardware.tv.tuner.PlaybackStatus.SPACE_EMPTY;
    /**
     * The space of the playback is almost empty.
     *
     * <p> the threshold is set in {@link DvrSettings}.
     */
    public static final int PLAYBACK_STATUS_ALMOST_EMPTY =
            android.hardware.tv.tuner.PlaybackStatus.SPACE_ALMOST_EMPTY;
    /**
     * The space of the playback is almost full.
     *
     * <p> the threshold is set in {@link DvrSettings}.
     */
    public static final int PLAYBACK_STATUS_ALMOST_FULL =
            android.hardware.tv.tuner.PlaybackStatus.SPACE_ALMOST_FULL;
    /**
     * The space of the playback is full.
     */
    public static final int PLAYBACK_STATUS_FULL =
            android.hardware.tv.tuner.PlaybackStatus.SPACE_FULL;

    private static final String TAG = "TvTunerPlayback";

    private long mNativeContext;
    private OnPlaybackStatusChangedListener mListener;
    private Executor mExecutor;
    private int mUserId;
    private static int sInstantId = 0;
    private int mSegmentId = 0;
    private int mUnderflow;
    private final Object mListenerLock = new Object();

    private native int nativeAttachFilter(Filter filter);
    private native int nativeDetachFilter(Filter filter);
    private native int nativeConfigureDvr(DvrSettings settings);
    private native int nativeSetStatusCheckIntervalHint(long durationInMs);
    private native int nativeStartDvr();
    private native int nativeStopDvr();
    private native int nativeFlushDvr();
    private native int nativeClose();
    private native void nativeSetFileDescriptor(int fd);
    private native long nativeRead(long size);
    private native long nativeRead(byte[] bytes, long offset, long size);
    private native long nativeSeek(long pos);

    private DvrPlayback() {
        mUserId = Process.myUid();
        mSegmentId = (sInstantId & 0x0000ffff) << 16;
        sInstantId++;
    }

    /** @hide */
    public void setListener(
            @NonNull Executor executor, @NonNull OnPlaybackStatusChangedListener listener) {
        synchronized (mListenerLock) {
            mExecutor = executor;
            mListener = listener;
        }
    }

    private void onPlaybackStatusChanged(int status) {
        if (status == PLAYBACK_STATUS_EMPTY) {
            mUnderflow++;
        }
        synchronized (mListenerLock) {
            if (mExecutor != null && mListener != null) {
                mExecutor.execute(() -> {
                    synchronized (mListenerLock) {
                        if (mListener != null) {
                            mListener.onPlaybackStatusChanged(status);
                        }
                    }
                });
            }
        }
    }


    /**
     * Attaches a filter to DVR interface for playback.
     *
     * @deprecated attaching filters is not valid in Dvr Playback use case. This API is a no-op.
     *             Filters opened by {@link Tuner#openFilter} are used for DVR playback.
     *
     * @param filter the filter to be attached.
     * @return result status of the operation.
     */
    @Result
    @Deprecated
    public int attachFilter(@NonNull Filter filter) {
        // no-op
        return Tuner.RESULT_UNAVAILABLE;
    }

    /**
     * Detaches a filter from DVR interface.
     *
     * @deprecated detaching filters is not valid in Dvr Playback use case. This API is a no-op.
     *             Filters opened by {@link Tuner#openFilter} are used for DVR playback.
     *
     * @param filter the filter to be detached.
     * @return result status of the operation.
     */
    @Result
    @Deprecated
    public int detachFilter(@NonNull Filter filter) {
        // no-op
        return Tuner.RESULT_UNAVAILABLE;
    }

    /**
     * Configures the DVR.
     *
     * @param settings the settings of the DVR interface.
     * @return result status of the operation.
     */
    @Result
    public int configure(@NonNull DvrSettings settings) {
        return nativeConfigureDvr(settings);
    }

    /**
     * Set playback buffer status check time interval.
     *
     * This status check time interval will be used by the Dvr to decide how often to evaluate
     * data. The default value will be decided by HAL if it’s not set.
     *
     * <p>This functionality is only available in Tuner version 3.0 and higher and will otherwise
     * return a {@link Tuner#RESULT_UNAVAILABLE}. Use {@link TunerVersionChecker#getTunerVersion()}
     * to get the version information.
     *
     * @param durationInMs specifies the duration of the delay in milliseconds.
     *
     * @return one of the following results:
     * {@link Tuner#RESULT_SUCCESS} if succeed,
     * {@link Tuner#RESULT_UNAVAILABLE} if Dvr is unavailable or unsupported HAL versions,
     * {@link Tuner#RESULT_NOT_INITIALIZED} if Dvr is not initialized,
     * {@link Tuner#RESULT_INVALID_STATE} if Dvr is in a wrong state,
     * {@link Tuner#RESULT_INVALID_ARGUMENT}  if the input parameter is invalid.
     */
    @Result
    public int setPlaybackBufferStatusCheckIntervalHint(long durationInMs) {
        if (!TunerVersionChecker.checkHigherOrEqualVersionTo(
                TunerVersionChecker.TUNER_VERSION_3_0, "Set status check interval hint")) {
            // no-op
            return Tuner.RESULT_UNAVAILABLE;
        }
        return nativeSetStatusCheckIntervalHint(durationInMs);
    }

    /**
     * Starts DVR.
     *
     * <p>Starts consuming playback data or producing data for recording.
     *
     * @return result status of the operation.
     */
    @Result
    public int start() {
        mSegmentId =  (mSegmentId & 0xffff0000) | (((mSegmentId & 0x0000ffff) + 1) & 0x0000ffff);
        mUnderflow = 0;
        Log.d(TAG, "Write Stats Log for Playback.");
        FrameworkStatsLog
                .write(FrameworkStatsLog.TV_TUNER_DVR_STATUS, mUserId,
                    FrameworkStatsLog.TV_TUNER_DVR_STATUS__TYPE__PLAYBACK,
                    FrameworkStatsLog.TV_TUNER_DVR_STATUS__STATE__STARTED, mSegmentId, 0);
        return nativeStartDvr();
    }

    /**
     * Stops DVR.
     *
     * <p>Stops consuming playback data or producing data for recording.
     * <p>Does nothing if the filter is stopped or not started.</p>
     *
     * @return result status of the operation.
     */
    @Result
    public int stop() {
        Log.d(TAG, "Write Stats Log for Playback.");
        FrameworkStatsLog
                .write(FrameworkStatsLog.TV_TUNER_DVR_STATUS, mUserId,
                    FrameworkStatsLog.TV_TUNER_DVR_STATUS__TYPE__PLAYBACK,
                    FrameworkStatsLog.TV_TUNER_DVR_STATUS__STATE__STOPPED, mSegmentId, mUnderflow);
        return nativeStopDvr();
    }

    /**
     * Flushed DVR data.
     *
     * <p>The data in DVR buffer is cleared.
     *
     * @return result status of the operation.
     */
    @Result
    public int flush() {
        return nativeFlushDvr();
    }

    /**
     * Closes the DVR instance to release resources.
     */
    @Override
    public void close() {
        int res = nativeClose();
        if (res != Tuner.RESULT_SUCCESS) {
            TunerUtils.throwExceptionForResult(res, "failed to close DVR playback");
        }
    }

    /**
     * Sets file descriptor to read data.
     *
     * <p>When a read operation of the filter object is happening, this method should not be
     * called.
     *
     * @param fd the file descriptor to read data.
     * @see #read(long)
     * @see #seek(long)
     */
    public void setFileDescriptor(@NonNull ParcelFileDescriptor fd) {
        nativeSetFileDescriptor(fd.getFd());
    }

    /**
     * Reads data from the file for DVR playback.
     *
     * @param size the maximum number of bytes to read.
     * @return the number of bytes read.
     */
    @BytesLong
    public long read(@BytesLong long size) {
        return nativeRead(size);
    }

    /**
     * Reads data from the buffer for DVR playback.
     *
     * @param buffer the byte array where DVR reads data from.
     * @param offset the index of the first byte in {@code buffer} to read.
     * @param size the maximum number of bytes to read.
     * @return the number of bytes read.
     */
    @BytesLong
    public long read(@NonNull byte[] buffer, @BytesLong long offset, @BytesLong long size) {
        if (size + offset > buffer.length) {
            throw new ArrayIndexOutOfBoundsException(
                    "Array length=" + buffer.length + ", offset=" + offset + ", size=" + size);
        }
        return nativeRead(buffer, offset, size);
    }

    /**
     * Sets the file pointer offset of the file descriptor.
     *
     * @param position the offset position, measured in bytes from the beginning of the file.
     * @return the new offset position. On error, {@code -1} is returned.
     */
    @BytesLong
    public long seek(@BytesLong long position) {
        return nativeSeek(position);
    }
}
