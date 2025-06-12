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

package android.media;

import android.annotation.NonNull;
import android.os.Parcel;

/**
 * Class encapsulating subtitle data, as received through the
 * {@link MediaPlayer.OnSubtitleDataListener} interface.
 * The subtitle data includes:
 * <ul>
 * <li> the track index</li>
 * <li> the start time (in microseconds) of the data</li>
 * <li> the duration (in microseconds) of the data</li>
 * <li> the actual data.</li>
 * </ul>
 * The data is stored in a byte-array, and is encoded in one of the supported in-band
 * subtitle formats. The subtitle encoding is determined by the MIME type of the
 * {@link MediaPlayer.TrackInfo} of the subtitle track, one of
 * {@link MediaFormat#MIMETYPE_TEXT_CEA_608}, {@link MediaFormat#MIMETYPE_TEXT_CEA_708},
 * {@link MediaFormat#MIMETYPE_TEXT_VTT}.
 * <p>
 * Here is an example of iterating over the tracks of a {@link MediaPlayer}, and checking which
 * encoding is used for the subtitle tracks:
 * <p>
 * <pre class="prettyprint">
 * MediaPlayer mp = new MediaPlayer();
 * mp.setDataSource(myContentLocation);
 * mp.prepare(); // synchronous prepare, ready to use when method returns
 * final TrackInfo[] trackInfos = mp.getTrackInfo();
 * for (TrackInfo info : trackInfo) {
 *     if (info.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
 *         final String mime = info.getFormat().getString(MediaFormat.KEY_MIME);
 *         if (MediaFormat.MIMETYPE_TEXT_CEA_608.equals(mime) {
 *             // subtitle encoding is CEA 608
 *         } else if (MediaFormat.MIMETYPE_TEXT_CEA_708.equals(mime) {
 *             // subtitle encoding is CEA 708
 *         } else if (MediaFormat.MIMETYPE_TEXT_VTT.equals(mime) {
 *             // subtitle encoding is WebVTT
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * See
 * {@link MediaPlayer#setOnSubtitleDataListener(android.media.MediaPlayer.OnSubtitleDataListener, android.os.Handler)}
 * to receive subtitle data from a MediaPlayer object.
 *
 * @see android.media.MediaPlayer
 */
public final class SubtitleData
{
    private static final String TAG = "SubtitleData";

    private int mTrackIndex;
    private long mStartTimeUs;
    private long mDurationUs;
    private byte[] mData;

    /** @hide */
    public SubtitleData(Parcel parcel) {
        if (!parseParcel(parcel)) {
            throw new IllegalArgumentException("parseParcel() fails");
        }
    }

    /**
     * Constructor.
     *
     * @param trackIndex the index of the media player track which contains this subtitle data.
     * @param startTimeUs the start time in microsecond for the subtitle data
     * @param durationUs the duration in microsecond for the subtitle data
     * @param data the data array for the subtitle data. It should not be null.
     *            No data copying is made.
     */
    public SubtitleData(int trackIndex, long startTimeUs, long durationUs, @NonNull byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("null data is not allowed");
        }
        mTrackIndex = trackIndex;
        mStartTimeUs = startTimeUs;
        mDurationUs = durationUs;
        mData = data;
    }

    /**
     * Returns the index of the media player track which contains this subtitle data.
     * @return an index in the array returned by {@link MediaPlayer#getTrackInfo()}.
     */
    public int getTrackIndex() {
        return mTrackIndex;
    }

    /**
     * Returns the media time at which the subtitle should be displayed, expressed in microseconds.
     * @return the display start time for the subtitle
     */
    public long getStartTimeUs() {
        return mStartTimeUs;
    }

    /**
     * Returns the duration in microsecond during which the subtitle should be displayed.
     * @return the display duration for the subtitle
     */
    public long getDurationUs() {
        return mDurationUs;
    }

    /**
     * Returns the encoded data for the subtitle content.
     * Encoding format depends on the subtitle type, refer to
     * <a href="https://en.wikipedia.org/wiki/CEA-708">CEA 708</a>,
     * <a href="https://en.wikipedia.org/wiki/EIA-608">CEA/EIA 608</a> and
     * <a href="https://www.w3.org/TR/webvtt1/">WebVTT</a>, defined by the MIME type
     * of the subtitle track.
     * @return the encoded subtitle data
     */
    public @NonNull byte[] getData() {
        return mData;
    }

    private boolean parseParcel(Parcel parcel) {
        parcel.setDataPosition(0);
        if (parcel.dataAvail() == 0) {
            return false;
        }

        mTrackIndex = parcel.readInt();
        mStartTimeUs = parcel.readLong();
        mDurationUs = parcel.readLong();
        mData = new byte[parcel.readInt()];
        parcel.readByteArray(mData);

        return true;
    }
}
