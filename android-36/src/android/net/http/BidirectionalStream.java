// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import android.annotation.IntDef;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Class for bidirectional sending and receiving of data over HTTP/2 or QUIC connections. Created by
 * {@link Builder}.
 *
 * Note: There are ordering restrictions on methods of {@link BidirectionalStream};
 * please see individual methods for description of restrictions.
 */
public abstract class BidirectionalStream {
    /**
     * Lowest stream priority. Passed to {@link Builder#setPriority}.
     */
    public static final int STREAM_PRIORITY_IDLE = 0;
    /**
     * Very low stream priority. Passed to {@link Builder#setPriority}.
     */
    public static final int STREAM_PRIORITY_LOWEST = 1;
    /**
     * Low stream priority. Passed to {@link Builder#setPriority}.
     */
    public static final int STREAM_PRIORITY_LOW = 2;
    /**
     * Medium stream priority. Passed to {@link Builder#setPriority}. This is the
     * default priority given to the stream.
     */
    public static final int STREAM_PRIORITY_MEDIUM = 3;
    /**
     * Highest stream priority. Passed to {@link Builder#setPriority}.
     */
    public static final int STREAM_PRIORITY_HIGHEST = 4;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BidirectionalStream.STREAM_PRIORITY_IDLE,
            BidirectionalStream.STREAM_PRIORITY_LOWEST,
            BidirectionalStream.STREAM_PRIORITY_LOW,
            BidirectionalStream.STREAM_PRIORITY_MEDIUM,
            BidirectionalStream.STREAM_PRIORITY_HIGHEST})
    public @interface BidirectionalStreamPriority {}


    /**
     * Builder for {@link BidirectionalStream}s. Allows configuring stream before constructing
     * it via {@link Builder#build}.
     */
    // SuppressLint: Builder can not be final since this is abstract and inherited
    @SuppressLint("StaticFinalBuilder")
    public abstract static class Builder {
        /**
         * Sets the HTTP method for the request. Returns builder to facilitate chaining.
         *
         * @param method the method to use for request. Default is 'POST'
         * @return the builder to facilitate chaining
         */
        @NonNull
        public abstract Builder setHttpMethod(@NonNull String method);

        /**
         * Adds a request header. Returns builder to facilitate chaining.
         *
         * @param header the header name
         * @param value the header value
         * @return the builder to facilitate chaining
         */
        @NonNull
        public abstract Builder addHeader(@NonNull String header, @NonNull String value);

        /**
         * Sets priority of the stream which should be one of the {@link #STREAM_PRIORITY_IDLE
         * STREAM_PRIORITY_*} values. The stream is given {@link #STREAM_PRIORITY_MEDIUM} priority
         * if this method is not called.
         *
         * @param priority priority of the stream which should be one of the {@link
         * #STREAM_PRIORITY_IDLE STREAM_PRIORITY_*} values.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setPriority(@BidirectionalStreamPriority int priority);

        /**
         * Sets whether to delay sending request headers until {@link BidirectionalStream#flush()}
         * is called. This flag is currently only respected when QUIC is negotiated.
         * When true, QUIC will send request header frame along with data frame(s)
         * as a single packet when possible.
         *
         * @param delayRequestHeadersUntilFirstFlush if true, sending request headers will be
         *         delayed
         * until flush() is called.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setDelayRequestHeadersUntilFirstFlushEnabled(
                boolean delayRequestHeadersUntilFirstFlush);

        /**
         * Sets {@link android.net.TrafficStats} tag to use when accounting socket traffic caused by
         * this request. See {@link android.net.TrafficStats} for more information. If no tag is
         * set (e.g. this method isn't called), then Android accounts for the socket traffic caused
         * by this request as if the tag value were set to 0.
         * <p>
         * <b>NOTE:</b>Setting a tag disallows sharing of sockets with requests
         * with other tags, which may adversely effect performance by prohibiting
         * connection sharing. In other words use of multiplexed sockets (e.g. HTTP/2
         * and QUIC) will only be allowed if all requests have the same socket tag.
         *
         * @param tag the tag value used to when accounting for socket traffic caused by this
         *            request. Tags between 0xFFFFFF00 and 0xFFFFFFFF are reserved and used
         *            internally by system services like {@link android.app.DownloadManager} when
         *            performing traffic on behalf of an application.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setTrafficStatsTag(int tag);

        /**
         * Sets specific UID to use when accounting socket traffic caused by this request. See
         * {@link android.net.TrafficStats} for more information. Designed for use when performing
         * an operation on behalf of another application. Caller must hold
         * {@code android.Manifest.permission#MODIFY_NETWORK_ACCOUNTING} permission. By default
         * traffic is attributed to UID of caller.
         * <p>
         * <b>NOTE:</b>Setting a UID disallows sharing of sockets with requests
         * with other UIDs, which may adversely effect performance by prohibiting
         * connection sharing. In other words use of multiplexed sockets (e.g. HTTP/2
         * and QUIC) will only be allowed if all requests have the same UID set.
         *
         * @param uid the UID to attribute socket traffic caused by this request.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setTrafficStatsUid(int uid);


        /**
         * Creates a {@link BidirectionalStream} using configuration from this {@link Builder}. The
         * returned {@code BidirectionalStream} can then be started by calling {@link
         * BidirectionalStream#start}.
         *
         * @return constructed {@link BidirectionalStream} using configuration from this {@link
         *         Builder}
         */
        @NonNull
        public abstract BidirectionalStream build();
    }

    /**
     * Callback interface used to receive callbacks from a {@link BidirectionalStream}.
     */
    public interface Callback {
        /**
         * Invoked when the stream is ready for reading and writing. Consumer may call {@link
         * BidirectionalStream#read read()} to start reading data. Consumer may call {@link
         * BidirectionalStream#write write()} to start writing data.
         *
         * @param stream the stream that is ready. <strong>This is not guaranteed to be the same
         *        object as the one received by other callbacks, nor is it guaranteed to be the one
         *        returned by {@link BidirectionalStream.Builder#build}.</strong> However, method
         *        calls on this object will have the same effects as calls on the original
         *        {@link BidirectionalStream}.
         */
        void onStreamReady(@NonNull BidirectionalStream stream);

        /**
         * Invoked when initial response headers are received. Headers are available from {@code
         * info.}{@link UrlResponseInfo#getHeaders getHeaders()}. Consumer may call {@link
         * BidirectionalStream#read read()} to start reading. Consumer may call {@link
         * BidirectionalStream#write write()} to start writing or close the stream.
         *
         * @param stream the stream on which response headers were received. <strong>This is not
         *        guaranteed to be the same object as the one received by other callbacks, nor is
         *        it guaranteed to be the one returned by {@link BidirectionalStream.Builder#build}.
         *        </strong> However, method calls on this object will have the same effects as
         *        calls on the original {@link BidirectionalStream}.
         * @param info the response information.
         */
        void onResponseHeadersReceived(@NonNull BidirectionalStream stream,
                @NonNull UrlResponseInfo info);

        /**
         * Invoked when data is read into the buffer passed to {@link BidirectionalStream#read
         * read()}. Only part of the buffer may be populated. To continue reading, call {@link
         * BidirectionalStream#read read()}. It may be invoked after {@code
         * onResponseTrailersReceived()}, if there was pending read data before trailers were
         * received.
         *
         * @param stream the stream on which the read completed. <strong>This is not guaranteed to
         *        be the same object as the one received by other callbacks, nor is it guaranteed
         *        to be the one returned by {@link BidirectionalStream.Builder#build}.</strong>
         *        However, method calls on this object will have the same effects as calls on the
         *        original {@link BidirectionalStream}.
         * @param info the response information
         * @param buffer the buffer that was passed to {@link BidirectionalStream#read read()}, now
         * set to the end of the received data. If position is not updated, it means the remote side
         * has signaled that it will send no more data.
         * @param endOfStream if true, this is the last read data, remote will not send more data,
         *         and
         * the read side is closed.
         */
        void onReadCompleted(@NonNull BidirectionalStream stream, @NonNull UrlResponseInfo info,
                @NonNull ByteBuffer buffer, boolean endOfStream);

        /**
         * Invoked when the entire ByteBuffer passed to {@link BidirectionalStream#write write()} is
         * sent. The buffer's position is updated to be the same as the buffer's limit. The buffer's
         * limit is not changed. To continue writing, call {@link BidirectionalStream#write
         * write()}.
         *
         * @param stream the stream on which the write completed. <strong>This is not guaranteed to
         *        be the same object as the one received by other callbacks, nor is it guaranteed
         *        to be the one returned by {@link BidirectionalStream.Builder#build}.</strong>
         *        However, method calls on this object will have the same effects as calls on the
         *        original {@link BidirectionalStream}.
         * @param info the response information
         * @param buffer the buffer that was passed to {@link BidirectionalStream#write write()}.
         *         The
         * buffer's position is set to the buffer's limit. The buffer's limit is not changed.
         * @param endOfStream the endOfStream flag that was passed to the corresponding {@link
         * BidirectionalStream#write write()}. If true, the write side is closed.
         */
        void onWriteCompleted(@NonNull BidirectionalStream stream, @NonNull UrlResponseInfo info,
                @NonNull ByteBuffer buffer, boolean endOfStream);

        /**
         * Invoked when trailers are received before closing the stream. Only invoked when server
         * sends trailers, which it may not. May be invoked while there is read data remaining in
         * local buffer.
         *
         * Default implementation takes no action.
         *
         * @param stream the stream on which response trailers were received. <strong>This is not
         *        guaranteed to be the same object as the one received by other callbacks, nor is
         *        it guaranteed to be the one returned by {@link BidirectionalStream.Builder#build}.
         *        </strong> However, method calls on this object will have the same effects as calls
         *        on the original {@link BidirectionalStream}.
         * @param info the response information
         * @param trailers the trailers received
         */
        void onResponseTrailersReceived(@NonNull BidirectionalStream stream,
                @NonNull UrlResponseInfo info, @NonNull HeaderBlock trailers);

        /**
         * Invoked when there is no data to be read or written and the stream is closed successfully
         * remotely and locally. Once invoked, no further {@link BidirectionalStream.Callback}
         * methods will be invoked.
         *
         * @param stream the stream which is closed successfully. <strong>This is not guaranteed to
         *        be the same object as the one received by other callbacks, nor is it guaranteed
         *        to be the one returned by {@link BidirectionalStream.Builder#build}.</strong>
         *        However, method calls on this object will have the same effects as calls on the
         *        original {@link BidirectionalStream}.
         * @param info the response information
         */
        void onSucceeded(@NonNull BidirectionalStream stream, @NonNull UrlResponseInfo info);

        /**
         * Invoked if the stream failed for any reason after {@link BidirectionalStream#start}.
         * <a href="https://tools.ietf.org/html/rfc7540#section-7">HTTP/2 error codes</a> are
         * mapped to {@link NetworkException#getErrorCode} codes. Once invoked,
         * no further {@link BidirectionalStream.Callback} methods will be invoked.
         *
         * @param stream the stream which has failed. <strong>This is not guaranteed to
         *        be the same object as the one received by other callbacks, nor is it guaranteed
         *        to be the one returned by {@link BidirectionalStream.Builder#build}.</strong>
         *        However, method calls on this object will have the same effects as calls on the
         *        original {@link BidirectionalStream}.
         * @param info the response information. May be {@code null} if no response was received.
         * @param error information about the failure
         */
        void onFailed(@NonNull BidirectionalStream stream, @Nullable UrlResponseInfo info,
                @NonNull HttpException error);

        /**
         * Invoked if the stream was canceled via {@link BidirectionalStream#cancel}. Once invoked,
         * no further {@link BidirectionalStream.Callback} methods will be invoked. Default
         * implementation takes no action.
         *
         * @param stream the stream that was canceled. <strong>This is not guaranteed to
         *        be the same object as the one received by other callbacks, nor is it guaranteed
         *        to be the one returned by {@link BidirectionalStream.Builder#build}.</strong>
         *        However, method calls on this object will have the same effects as calls on the
         *        original {@link BidirectionalStream}.
         * @param info the response information. May be {@code null} if no response was received.
         */
        void onCanceled(@NonNull BidirectionalStream stream, @Nullable UrlResponseInfo info);
    }

    /**
     * See {@link BidirectionalStream.Builder#setHttpMethod(String)}.
     */
    @NonNull
    public abstract String getHttpMethod();

    /**
     * See {@link BidirectionalStream.Builder#setTrafficStatsTag(int)}
     */
    public abstract boolean hasTrafficStatsTag();

    /**
     * See {@link BidirectionalStream.Builder#setTrafficStatsTag(int)}
     */
    public abstract int getTrafficStatsTag();

    /**
     * See {@link BidirectionalStream.Builder#setTrafficStatsUid(int)}
     */
    public abstract boolean hasTrafficStatsUid();

    /**
     * See {@link BidirectionalStream.Builder#setTrafficStatsUid(int)}
     */
    public abstract int getTrafficStatsUid();

    /**
     * See {@link Builder#addHeader(String, String)}
     */
    @NonNull
    public abstract HeaderBlock getHeaders();

    /**
     * See {@link Builder#setPriority(int)}
     */
    public abstract @BidirectionalStreamPriority int getPriority();

    /**
     * See {@link Builder#setDelayRequestHeadersUntilFirstFlushEnabled(boolean)}
     */
    public abstract boolean isDelayRequestHeadersUntilFirstFlushEnabled();

    /**
     * Starts the stream, all callbacks go to the {@code callback} argument passed to {@link
     * BidirectionalStream.Builder}'s constructor. Should only be called once.
     */
    public abstract void start();

    /**
     * Reads data from the stream into the provided buffer. Can only be called at most once in
     * response to each invocation of the {@link Callback#onStreamReady onStreamReady()}/ {@link
     * Callback#onResponseHeadersReceived onResponseHeadersReceived()} and {@link
     * Callback#onReadCompleted onReadCompleted()} methods of the {@link Callback}. Each call will
     * result in an invocation of one of the {@link Callback Callback}'s {@link
     * Callback#onReadCompleted onReadCompleted()} method if data is read, or its {@link
     * Callback#onFailed onFailed()} method if there's an error.
     *
     * An attempt to read data into {@code buffer} starting at {@code buffer.position()} is begun.
     * At most {@code buffer.remaining()} bytes are read. {@code buffer.position()} is updated upon
     * invocation of {@link Callback#onReadCompleted onReadCompleted()} to indicate how much data
     * was read.
     *
     * @param buffer the {@link ByteBuffer} to read data into. Must be a direct ByteBuffer. The
     * embedder must not read or modify buffer's position, limit, or data between its position and
     * limit until {@link Callback#onReadCompleted onReadCompleted()}, {@link Callback#onCanceled
     * onCanceled()}, or {@link Callback#onFailed onFailed()} are invoked.
     */
    public abstract void read(@NonNull ByteBuffer buffer);

    /**
     * Attempts to write data from the provided buffer into the stream. If auto flush is disabled,
     * data will be sent only after {@link #flush flush()} is called. Each call will result in an
     * invocation of one of the {@link Callback Callback}'s {@link Callback#onWriteCompleted
     * onWriteCompleted()} method if data is sent, or its {@link Callback#onFailed onFailed()}
     * method if there's an error.
     *
     * An attempt to write data from {@code buffer} starting at {@code buffer.position()} is begun.
     * {@code buffer.remaining()} bytes will be written. {@link Callback#onWriteCompleted
     * onWriteCompleted()} will be invoked only when the full ByteBuffer is written.
     *
     * @param buffer the {@link ByteBuffer} to write data from. Must be a direct ByteBuffer. The
     * embedder must not read or modify buffer's position, limit, or data between its position and
     * limit until {@link Callback#onWriteCompleted onWriteCompleted()}, {@link Callback#onCanceled
     * onCanceled()}, or {@link Callback#onFailed onFailed()} are invoked. Can be empty when {@code
     * endOfStream} is {@code true}.
     * @param endOfStream if {@code true}, then {@code buffer} is the last buffer to be written, and
     * once written, stream is closed from the client side, resulting in half-closed stream or a
     * fully closed stream if the remote side has already closed.
     */
    public abstract void write(@NonNull ByteBuffer buffer, boolean endOfStream);

    /**
     * Flushes pending writes. This method should not be invoked before {@link
     * Callback#onStreamReady onStreamReady()}. For previously delayed {@link #write write()}s, a
     * corresponding {@link Callback#onWriteCompleted onWriteCompleted()} will be invoked when the
     * buffer is sent.
     */
    public abstract void flush();

    /**
     * Cancels the stream. Can be called at any time after {@link #start}. {@link
     * Callback#onCanceled onCanceled()} will be invoked when cancelation is complete and no further
     * callback methods will be invoked. If the stream has completed or has not started, calling
     * {@code cancel()} has no effect and {@code onCanceled()} will not be invoked. If the {@link
     * Executor} passed in during
     * {@code BidirectionalStream} construction runs tasks on a single thread, and {@code cancel()}
     * is called on that thread, no listener methods (besides {@code onCanceled()}) will be invoked
     * after
     * {@code cancel()} is called. Otherwise, at most one callback method may be invoked after
     * {@code cancel()} has completed.
     */
    public abstract void cancel();

    /**
     * Returns {@code true} if the stream was successfully started and is now done (succeeded,
     * canceled, or failed).
     *
     * @return {@code true} if the stream was successfully started and is now done (completed,
     * canceled, or failed), otherwise returns {@code false} to indicate stream is not yet started
     * or is in progress.
     */
    public abstract boolean isDone();
}
