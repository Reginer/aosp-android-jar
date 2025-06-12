// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import android.annotation.IntDef;
import android.annotation.SuppressLint;
import android.net.Network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Controls an HTTP request (GET, PUT, POST etc).
 * Created by {@link UrlRequest.Builder}, which can be obtained by calling
 * {@link HttpEngine#newUrlRequestBuilder}.
 * Note: All methods must be called on the {@link Executor} passed to
 * {@link HttpEngine#newUrlRequestBuilder}.
 */
public abstract class UrlRequest {

    UrlRequest() {}

    /**
     * Lowest request priority. Passed to {@link Builder#setPriority}.
     */
    public static final int REQUEST_PRIORITY_IDLE = 0;
    /**
     * Very low request priority. Passed to {@link Builder#setPriority}.
     */
    public static final int REQUEST_PRIORITY_LOWEST = 1;
    /**
     * Low request priority. Passed to {@link Builder#setPriority}.
     */
    public static final int REQUEST_PRIORITY_LOW = 2;
    /**
     * Medium request priority. Passed to {@link Builder#setPriority}. This is the
     * default priority given to the request.
     */
    public static final int REQUEST_PRIORITY_MEDIUM = 3;
    /**
     * Highest request priority. Passed to {@link Builder#setPriority}.
     */
    public static final int REQUEST_PRIORITY_HIGHEST = 4;

    /**
     * Builder for {@link UrlRequest}s. Allows configuring requests before constructing them
     * with {@link Builder#build}. The builder can be created by calling
     * {@link HttpEngine#newUrlRequestBuilder}.
     */
    // SuppressLint: Builder can not be final since this is abstract and inherited
    // e.g. ExperimentalUrlRequest.Builder
    @SuppressLint("StaticFinalBuilder")
    public abstract static class Builder {

        Builder() {}

        /**
         * Sets the HTTP method verb to use for this request.
         *
         * <p>The default when this method is not called is "GET" if the request has
         * no body or "POST" if it does.
         *
         * @param method "GET", "HEAD", "DELETE", "POST" or "PUT".
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setHttpMethod(@NonNull String method);

        /**
         * Adds a request header.
         *
         * @param header header name.
         * @param value header value.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder addHeader(@NonNull String header, @NonNull String value);

        /**
         * WARNING: This method should not be called with `setCacheDisabled(false)` as this may
         * lead to incorrect behaviour on older versions of HttpEngine.
         *
         * Whether to disable cache for the request. If the engine is not set up to use cache,
         * this call has no effect.
         * @param disableCache {@code true} to disable cache, {@code false} otherwise.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setCacheDisabled(boolean disableCache);

        /**
         * Sets priority of the request which should be one of the {@link #REQUEST_PRIORITY_IDLE
         * REQUEST_PRIORITY_*} values. The request is given {@link #REQUEST_PRIORITY_MEDIUM}
         * priority if this method is not called.
         *
         * @param priority priority of the request which should be one of the {@link
         * #REQUEST_PRIORITY_IDLE REQUEST_PRIORITY_*} values.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setPriority(int priority);

        /**
         * Sets upload data provider. Switches method to "POST" if not explicitly set. Starting the
         * request will throw an exception if a Content-Type header is not set.
         *
         * @param uploadDataProvider responsible for providing the upload data.
         * @param executor All {@code uploadDataProvider} methods will be invoked using this {@code
         * Executor}. May optionally be the same {@code Executor} the request itself is using.
         * @return the builder to facilitate chaining.
         */
        // SuppressLint: UploadDataProvider is wrapped by other classes after set.
        // Also, UploadDataProvider is a class to provide an upload body and getter is not useful
        @NonNull @SuppressLint("MissingGetterMatchingBuilder")
        public abstract Builder setUploadDataProvider(
                @NonNull UploadDataProvider uploadDataProvider, @NonNull Executor executor);

        /**
         * WARNING: This method should not be called with `setDirectExecutorAllowed(false)` as
         * this may lead to incorrect behaviour on older versions of HttpEngine.
         *
         * Marks whether the executors this request will use to notify callbacks (for
         * {@code UploadDataProvider}s and {@code UrlRequest.Callback}s) is intentionally performing
         * inline execution, like Guava's directExecutor or
         * {@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy}.
         *
         * <p><b>Warning:</b> If set to true: This option makes it easy to accidentally block the
         * network thread. This should not be done if your callbacks perform disk I/O, acquire
         * locks, or call into other code you don't carefully control and audit.
         *
         * @param allowDirectExecutor {@code true} to allow executors performing inline execution,
         *                            {@code false} otherwise.
         * @return the builder to facilitate chaining.
         */
        @NonNull
        public abstract Builder setDirectExecutorAllowed(boolean allowDirectExecutor);

        /**
         * Binds the request to the specified network. The HTTP stack will send this request
         * only using the network associated to this handle. If this network disconnects the request
         * will fail, the exact error will depend on the stage of request processing when
         * the network disconnects.
         *
         * @param network the network to bind the request to. Specify {@code null} to unbind.
         * @return the builder to facilitate chaining.
         */
        @NonNull @SuppressLint("BuilderSetStyle")
        public abstract Builder bindToNetwork(@Nullable Network network);

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
         * Creates a {@link UrlRequest} using configuration within this {@link Builder}. The
         * returned
         * {@code UrlRequest} can then be started by calling {@link UrlRequest#start}.
         *
         * @return constructed {@link UrlRequest} using configuration within this {@link Builder}.
         */
        @NonNull
        public abstract UrlRequest build();
    }

    /**
     * Users of the HTTP stack extend this class to receive callbacks indicating the
     * progress of a {@link UrlRequest} being processed. An instance of this class
     * is passed in to {@link UrlRequest.Builder}'s constructor when
     * constructing the {@code UrlRequest}.
     * <p>
     * Note:  All methods will be invoked on the thread of the {@link java.util.concurrent.Executor}
     * used during construction of the {@code UrlRequest}.
     */
    public interface Callback {
        /**
         * Invoked whenever a redirect is encountered. This will only be invoked between the call to
         * {@link UrlRequest#start} and {@link Callback#onResponseStarted onResponseStarted()}. The
         * body of the redirect response, if it has one, will be ignored.
         *
         * The redirect will not be followed until the URLRequest's {@link
         * UrlRequest#followRedirect} method is called, either synchronously or asynchronously.
         *
         * @param request Request being redirected. <strong>This is not guaranteed to be the same
         *        object as the one received by other callbacks, nor is it guaranteed to be the one
         *        returned by {@link URLRequest.Builder#build}.</strong> However, method calls on
         *        this object will have the same effects as calls on the original
         *        {@link URLRequest}.
         * @param info Response information.
         * @param newLocationUrl Location where request is redirected.
         * @throws Exception if an error occurs while processing a redirect. {@link #onFailed} will
         *         be
         * called with the thrown exception set as the cause of the {@link CallbackException}.
         */
        // SuppressLint: Exception will be wrapped and passed to #onFailed, see above javadoc
        @SuppressLint("GenericException")
        void onRedirectReceived(@NonNull UrlRequest request,
                @NonNull UrlResponseInfo info, @NonNull String newLocationUrl) throws Exception;

        /**
         * Invoked when the final set of headers, after all redirects, is received. Will only be
         * invoked once for each request.
         *
         * With the exception of {@link Callback#onCanceled onCanceled()}, no other {@link Callback}
         * method will be invoked for the request, including {@link Callback#onSucceeded
         * onSucceeded()} and {@link Callback#onFailed onFailed()}, until {@link UrlRequest#read
         * UrlRequest.read()} is called to attempt to start reading the response body.
         *
         * @param request Request that started to get response. <strong>This is not guaranteed to be
         *        the same object as the one received by other callbacks, nor is it guaranteed to be
         *        the one returned by {@link URLRequest.Builder#build}.</strong> However, method
         *        calls on this object will have the same effects as calls on the original
         *        {@link URLRequest}.
         * @param info Response information.
         * @throws Exception if an error occurs while processing response start. {@link #onFailed}
         *         will
         * be called with the thrown exception set as the cause of the {@link CallbackException}.
         */
        // SuppressLint: Exception will be wrapped and passed to #onFailed, see above javadoc
        @SuppressLint("GenericException")
        void onResponseStarted(@NonNull UrlRequest request,
                @NonNull UrlResponseInfo info) throws Exception;

        /**
         * Invoked whenever part of the response body has been read. Only part of the buffer may be
         * populated, even if the entire response body has not yet been consumed.
         *
         * With the exception of {@link Callback#onCanceled onCanceled()}, no other {@link Callback}
         * method will be invoked for the request, including {@link Callback#onSucceeded
         * onSucceeded()} and {@link Callback#onFailed onFailed()}, until {@link UrlRequest#read
         * UrlRequest.read()} is called to attempt to continue reading the response body.
         *
         * @param request Request that received data. <strong>This is not guaranteed to be the same
         *        object as the one received by other callbacks, nor is it guaranteed to be the one
         *        returned by {@link URLRequest.Builder#build}.</strong> However, method calls on
         *        this object will have the same effects as calls on the original
         *        {@link URLRequest}.
         * @param info Response information.
         * @param byteBuffer The buffer that was passed in to {@link UrlRequest#read
         *         UrlRequest.read()},
         * now containing the received data. The buffer's position is updated to the end of the
         * received data. The buffer's limit is not changed.
         * @throws Exception if an error occurs while processing a read completion. {@link
         *         #onFailed}
         * will be called with the thrown exception set as the cause of the {@link
         * CallbackException}.
         */
        // SuppressLint: Exception will be wrapped and passed to #onFailed, see above javadoc
        @SuppressLint("GenericException")
        void onReadCompleted(@NonNull UrlRequest request,
                @NonNull UrlResponseInfo info, @NonNull ByteBuffer byteBuffer) throws Exception;

        /**
         * Invoked when request is completed successfully. Once invoked, no other {@link Callback}
         * methods will be invoked.
         *
         * @param request Request that succeeded. <strong>This is not guaranteed to be the same
         *        object as the one received by other callbacks, nor is it guaranteed to be the one
         *        returned by {@link URLRequest.Builder#build}.</strong> However, method calls on
         *        this object will have the same effects as calls on the original
         *        {@link URLRequest}.
         * @param info Response information.
         */
         void onSucceeded(
                @NonNull UrlRequest request, @NonNull UrlResponseInfo info);

        /**
         * Invoked if request failed for any reason after {@link UrlRequest#start}. Once invoked, no
         * other {@link Callback} methods will be invoked. {@code error} provides information about
         * the failure.
         *
         * @param request Request that failed. <strong>This is not guaranteed to be the same
         *        object as the one received by other callbacks, nor is it guaranteed to be the one
         *        returned by {@link URLRequest.Builder#build}.</strong> However, method calls on
         *        this object will have the same effects as calls on the original
         *        {@link URLRequest}.
         * @param info Response information. May be {@code null} if no response was received.
         * @param error information about error.
         */
        void onFailed(@NonNull UrlRequest request,
                @Nullable UrlResponseInfo info, @NonNull HttpException error);

        /**
         * Invoked if request was canceled via {@link UrlRequest#cancel}. Once invoked, no other
         * {@link Callback} methods will be invoked. Default implementation takes no action.
         *
         * @param request Request that was canceled. <strong>This is not guaranteed to be the same
         *        object as the one received by other callbacks, nor is it guaranteed to be the one
         *        returned by {@link URLRequest.Builder#build}.</strong> However, method calls on
         *        this object will have the same effects as calls on the original
         *        {@link URLRequest}.
         * @param info Response information. May be {@code null} if no response was received.
         */
        void onCanceled(@NonNull UrlRequest request, @Nullable UrlResponseInfo info);
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            Status.INVALID,
            Status.IDLE,
            Status.WAITING_FOR_STALLED_SOCKET_POOL,
            Status.WAITING_FOR_AVAILABLE_SOCKET,
            Status.WAITING_FOR_DELEGATE,
            Status.WAITING_FOR_CACHE,
            Status.DOWNLOADING_PAC_FILE,
            Status.RESOLVING_PROXY_FOR_URL,
            Status.RESOLVING_PROXY_FOR_URL,
            Status.RESOLVING_HOST_IN_PAC_FILE,
            Status.ESTABLISHING_PROXY_TUNNEL,
            Status.RESOLVING_HOST,
            Status.CONNECTING,
            Status.SSL_HANDSHAKE,
            Status.SENDING_REQUEST,
            Status.WAITING_FOR_RESPONSE,
            Status.READING_RESPONSE})
    public @interface UrlRequestStatus {}

    /**
     * Request status values returned by {@link #getStatus}.
     */
    public static class Status {
        /**
         * This state indicates that the request is completed, canceled, or is not started.
         */
        public static final int INVALID = -1;
        /**
         * This state corresponds to a resource load that has either not yet begun or is idle
         * waiting for the consumer to do something to move things along (e.g. when the consumer of
         * a {@link UrlRequest} has not called {@link UrlRequest#read read()} yet).
         */
        public static final int IDLE = 0;
        /**
         * When a socket pool group is below the maximum number of sockets allowed per group, but a
         * new socket cannot be created due to the per-pool socket limit, this state is returned by
         * all requests for the group waiting on an idle connection, except those that may be
         * serviced by a pending new connection.
         */
        public static final int WAITING_FOR_STALLED_SOCKET_POOL = 1;
        /**
         * When a socket pool group has reached the maximum number of sockets allowed per group,
         * this state is returned for all requests that don't have a socket, except those that
         * correspond to a pending new connection.
         */
        public static final int WAITING_FOR_AVAILABLE_SOCKET = 2;
        /**
         * This state indicates that the URLRequest delegate has chosen to block this request before
         * it was sent over the network.
         */
        public static final int WAITING_FOR_DELEGATE = 3;
        /**
         * This state corresponds to a resource load that is blocked waiting for access to a
         * resource in the cache. If multiple requests are made for the same resource, the first
         * request will be responsible for writing (or updating) the cache entry and the second
         * request will be deferred until the first completes. This may be done to optimize for
         * cache reuse.
         */
        public static final int WAITING_FOR_CACHE = 4;
        /**
         * This state corresponds to a resource being blocked waiting for the PAC script to be
         * downloaded.
         */
        public static final int DOWNLOADING_PAC_FILE = 5;
        /**
         * This state corresponds to a resource load that is blocked waiting for a proxy autoconfig
         * script to return a proxy server to use.
         */
        public static final int RESOLVING_PROXY_FOR_URL = 6;
        /**
         * This state corresponds to a resource load that is blocked waiting for a proxy autoconfig
         * script to return a proxy server to use, but that proxy script is busy resolving the IP
         * address of a host.
         */
        public static final int RESOLVING_HOST_IN_PAC_FILE = 7;
        /**
         * This state indicates that we're in the process of establishing a tunnel through the proxy
         * server.
         */
        public static final int ESTABLISHING_PROXY_TUNNEL = 8;
        /**
         * This state corresponds to a resource load that is blocked waiting for a host name to be
         * resolved. This could either indicate resolution of the origin server corresponding to the
         * resource or to the host name of a proxy server used to fetch the resource.
         */
        public static final int RESOLVING_HOST = 9;
        /**
         * This state corresponds to a resource load that is blocked waiting for a TCP connection
         * (or other network connection) to be established. HTTP requests that reuse a keep-alive
         * connection skip this state.
         */
        public static final int CONNECTING = 10;
        /**
         * This state corresponds to a resource load that is blocked waiting for the SSL handshake
         * to complete.
         */
        public static final int SSL_HANDSHAKE = 11;
        /**
         * This state corresponds to a resource load that is blocked waiting to completely upload a
         * request to a server. In the case of a HTTP POST request, this state includes the period
         * of time during which the message body is being uploaded.
         */
        public static final int SENDING_REQUEST = 12;
        /**
         * This state corresponds to a resource load that is blocked waiting for the response to a
         * network request. In the case of a HTTP transaction, this corresponds to the period after
         * the request is sent and before all of the response headers have been received.
         */
        public static final int WAITING_FOR_RESPONSE = 13;
        /**
         * This state corresponds to a resource load that is blocked waiting for a read to complete.
         * In the case of a HTTP transaction, this corresponds to the period after the response
         * headers have been received and before all of the response body has been downloaded.
         * (NOTE: This state only applies for an {@link UrlRequest} while there is an outstanding
         * {@link UrlRequest#read read()} operation.)
         */
        public static final int READING_RESPONSE = 14;

        private Status() {}
    }

    /**
     * Listener interface used with {@link #getStatus} to receive the status of a
     * {@link UrlRequest}.
     */
    public interface StatusListener {
        /**
         * Invoked on {@link UrlRequest}'s {@link Executor}'s thread when request status is
         * obtained.
         *
         * @param status integer representing the status of the request. It is one of the values
         *         defined
         * in {@link Status}.
         */
        void onStatus(@UrlRequestStatus int status);
    }

    /**
     * See {@link UrlRequest.Builder#setHttpMethod(String)}.
     */
    @Nullable
    public abstract String getHttpMethod();

    /**
     * See {@link UrlRequest.Builder#addHeader(String, String)}
     */
    @NonNull
    public abstract HeaderBlock getHeaders();

    /**
     * See {@link Builder#setCacheDisabled(boolean)}
     */
    public abstract boolean isCacheDisabled();

    /**
     * See {@link UrlRequest.Builder#setDirectExecutorAllowed(boolean)}
     */
    public abstract boolean isDirectExecutorAllowed();

    /**
     * See {@link Builder#setPriority(int)}
     */
    public abstract int getPriority();

    /**
     * See {@link Builder#setTrafficStatsTag(int)}
     */
    public abstract boolean hasTrafficStatsTag();

    /**
     * See {@link Builder#setTrafficStatsTag(int)}
     */
    public abstract int getTrafficStatsTag();

    /**
     * See {@link Builder#setTrafficStatsUid(int)}
     */
    public abstract boolean hasTrafficStatsUid();

    /**
     * See {@link Builder#setTrafficStatsUid(int)}
     */
    public abstract int getTrafficStatsUid();

    /**
     * Starts the request, all callbacks go to {@link Callback}. May only be called
     * once. May not be called if {@link #cancel} has been called.
     */
    public abstract void start();

    /**
     * Follows a pending redirect. Must only be called at most once for each invocation of {@link
     * Callback#onRedirectReceived onRedirectReceived()}.
     */
    public abstract void followRedirect();

    /**
     * Attempts to read part of the response body into the provided buffer. Must only be called at
     * most once in response to each invocation of the {@link Callback#onResponseStarted
     * onResponseStarted()} and {@link Callback#onReadCompleted onReadCompleted()} methods of the
     * {@link Callback}. Each call will result in an asynchronous call to either the {@link Callback
     * Callback's} {@link Callback#onReadCompleted onReadCompleted()} method if data is read, its
     * {@link Callback#onSucceeded onSucceeded()} method if there's no more data to read, or its
     * {@link Callback#onFailed onFailed()} method if there's an error.
     *
     * @param buffer {@link ByteBuffer} to write response body to. Must be a direct ByteBuffer. The
     * embedder must not read or modify buffer's position, limit, or data between its position and
     * limit until the request calls back into the {@link Callback}.
     */
    public abstract void read(@NonNull ByteBuffer buffer);

    /**
     * Cancels the request. Can be called at any time. {@link Callback#onCanceled onCanceled()} will
     * be invoked when cancellation is complete and no further callback methods will be invoked. If
     * the request has completed or has not started, calling {@code cancel()} has no effect and
     * {@code onCanceled()} will not be invoked. If the {@link Executor} passed in during {@code
     * UrlRequest} construction runs tasks on a single thread, and {@code cancel()} is called on
     * that thread, no callback methods (besides {@code onCanceled()}) will be invoked after {@code
     * cancel()} is called. Otherwise, at most one callback method may be invoked after {@code
     * cancel()} has completed.
     */
    public abstract void cancel();

    /**
     * Returns {@code true} if the request was successfully started and is now finished (completed,
     * canceled, or failed).
     *
     * @return {@code true} if the request was successfully started and is now finished (completed,
     * canceled, or failed).
     */
    public abstract boolean isDone();

    /**
     * Queries the status of the request.
     *
     * <p>This is most useful to query the status of the request before any of the
     * {@link UrlRequest.Callback} methods are called by Cronet.
     *
     * <p>The {@code listener} will be invoked back on the {@link Executor} passed in when
     * the request was created. While you can assume the callback will be invoked in a timely
     * fashion, the API doesn't make any guarantees about the latency, nor does it specify the
     * order in which the listener and other callbacks will be invoked.
     *
     * @param listener a {@link StatusListener} that will be invoked with
     *         the request's current status.
     */
    // SuppressLint: The listener will be invoked back on the Executor passed in when the request
    // was created.
    @SuppressLint("ExecutorRegistration")
    public abstract void getStatus(@NonNull final StatusListener listener);

    // Note:  There are deliberately no accessors for the results of the request
    // here. Having none removes any ambiguity over when they are populated,
    // particularly in the redirect case.
}
