/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.health.connect;

import static android.health.connect.Constants.DEFAULT_LONG;

import static java.lang.Integer.min;

import java.util.Objects;

/**
 * A wrapper object contains information encoded in the {@code long} page token.
 *
 * @hide
 */
public final class PageTokenWrapper {
    /**
     * This constant represents an empty token returned by the last read request, meaning no more
     * pages are available.
     *
     * <p>We do not use this for read requests where page token is passed. The API design is
     * asymmetry, when page token is passed in, it always contains {@code isAscending} information;
     * the information is not available when it's returned.
     */
    public static final PageTokenWrapper EMPTY_PAGE_TOKEN = new PageTokenWrapper();

    private static final long MAX_ALLOWED_TIME_MILLIS = (1L << 44) - 1;
    private static final long MAX_ALLOWED_OFFSET = (1 << 18) - 1;
    private static final int OFFSET_START_BIT = 45;
    private static final int TIMESTAMP_START_BIT = 1;

    private final boolean mIsAscending;
    private final long mTimeMillis;
    private final int mOffset;
    private final boolean mIsTimestampSet;
    private final boolean mIsEmpty;

    /** isAscending stored in the page token. */
    public boolean isAscending() {
        return mIsAscending;
    }

    /** Timestamp stored in the page token. */
    public long timeMillis() {
        return mTimeMillis;
    }

    /** Offset stored in the page token. */
    public int offset() {
        return mOffset;
    }

    /** Whether or not the timestamp is set. */
    public boolean isTimestampSet() {
        return mIsTimestampSet;
    }

    /** Whether or not the page token contains meaningful values. */
    public boolean isEmpty() {
        return mIsEmpty;
    }

    /**
     * Both {@code timeMillis} and {@code offset} have to be non-negative; {@code timeMillis} cannot
     * exceed 2^44-1.
     *
     * <p>Note that due to space constraints, {@code offset} cannot exceed 2^18-1 (262143). If the
     * {@code offset} parameter exceeds the maximum allowed value, it'll fallback to the max value.
     *
     * <p>More details see go/hc-page-token
     */
    public static PageTokenWrapper of(boolean isAscending, long timeMillis, int offset) {
        checkArgument(timeMillis >= 0, "timestamp can not be negative");
        checkArgument(timeMillis <= MAX_ALLOWED_TIME_MILLIS, "timestamp too large");
        checkArgument(offset >= 0, "offset can not be negative");
        int boundedOffset = min((int) MAX_ALLOWED_OFFSET, offset);
        return new PageTokenWrapper(isAscending, timeMillis, boundedOffset);
    }

    /**
     * Generate a page token that contains only {@code isAscending} information. Timestamp and
     * offset are not set.
     */
    public static PageTokenWrapper ofAscending(boolean isAscending) {
        return new PageTokenWrapper(isAscending);
    }

    /**
     * Construct a {@link PageTokenWrapper} from {@code pageToken} and {@code defaultIsAscending}.
     *
     * <p>When {@code pageToken} is not set, in which case we can not get {@code isAscending} from
     * the token, it falls back to {@code defaultIsAscending}.
     *
     * <p>{@code pageToken} must be a non-negative long number (except for using the sentinel value
     * {@code DEFAULT_LONG}, whose current value is {@code -1}, which represents page token not set)
     */
    public static PageTokenWrapper from(long pageToken, boolean defaultIsAscending) {
        if (pageToken == DEFAULT_LONG) {
            return PageTokenWrapper.ofAscending(defaultIsAscending);
        }
        checkArgument(pageToken >= 0, "pageToken cannot be negative");
        return PageTokenWrapper.of(
                getIsAscending(pageToken), getTimestamp(pageToken), getOffset(pageToken));
    }

    /**
     * Take the least significant bit in the given {@code pageToken} to retrieve isAscending
     * information.
     *
     * <p>If the last bit of the token is 1, isAscending is false; otherwise isAscending is true.
     */
    private static boolean getIsAscending(long pageToken) {
        return (pageToken & 1) == 0;
    }

    /** Shifts bits in the given {@code pageToken} to retrieve timestamp information. */
    private static long getTimestamp(long pageToken) {
        long mask = MAX_ALLOWED_TIME_MILLIS << TIMESTAMP_START_BIT;
        return (pageToken & mask) >> TIMESTAMP_START_BIT;
    }

    /** Shifts bits in the given {@code pageToken} to retrieve offset information. */
    private static int getOffset(long pageToken) {
        return (int) (pageToken >> OFFSET_START_BIT);
    }

    private static void checkArgument(boolean expression, String errorMsg) {
        if (!expression) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Encodes a {@link PageTokenWrapper} to a long value.
     *
     * <p>Page token is structured as following from right (least significant bit) to left (most
     * significant bit):
     * <li>Least significant bit: 0 = isAscending true, 1 = isAscending false
     * <li>Next 44 bits: timestamp, represents epoch time millis
     * <li>Next 18 bits: offset, represents number of records processed in the previous page
     * <li>Sign bit: not used for encoding, page token is a signed long
     */
    public long encode() {
        return mIsTimestampSet
                ? ((long) mOffset << OFFSET_START_BIT)
                        | (mTimeMillis << TIMESTAMP_START_BIT)
                        | (mIsAscending ? 0 : 1)
                : DEFAULT_LONG;
    }

    @Override
    public String toString() {
        if (mIsEmpty) {
            return "PageTokenWrapper{}";
        }
        StringBuilder builder = new StringBuilder("PageTokenWrapper{");
        builder.append("isAscending = ").append(mIsAscending);
        if (mIsTimestampSet) {
            builder.append(", timeMillis = ").append(mTimeMillis);
            builder.append(", offset = ").append(mOffset);
        }
        return builder.append("}").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageTokenWrapper that)) return false;
        return mIsAscending == that.mIsAscending
                && mTimeMillis == that.mTimeMillis
                && mOffset == that.mOffset
                && mIsTimestampSet == that.mIsTimestampSet
                && mIsEmpty == that.mIsEmpty;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsAscending, mOffset, mTimeMillis, mIsTimestampSet, mIsEmpty);
    }

    private PageTokenWrapper(boolean isAscending, long timeMillis, int offset) {
        this.mIsAscending = isAscending;
        this.mTimeMillis = timeMillis;
        this.mOffset = offset;
        this.mIsTimestampSet = true;
        this.mIsEmpty = false;
    }

    private PageTokenWrapper(boolean isAscending) {
        this.mIsAscending = isAscending;
        this.mTimeMillis = 0;
        this.mOffset = 0;
        this.mIsTimestampSet = false;
        this.mIsEmpty = false;
    }

    private PageTokenWrapper() {
        this.mIsAscending = true;
        this.mTimeMillis = 0;
        this.mOffset = 0;
        this.mIsTimestampSet = false;
        this.mIsEmpty = true;
    }
}
