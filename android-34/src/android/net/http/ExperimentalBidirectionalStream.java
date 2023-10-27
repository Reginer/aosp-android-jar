// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

/**
 * {@link BidirectionalStream} that exposes experimental features. To obtain an
 * instance of this class, cast a {@code BidirectionalStream} to this type. Every
 * instance of {@code BidirectionalStream} can be cast to an instance of this class,
 * as they are backed by the same implementation and hence perform identically.
 * Instances of this class are not meant for general use, but instead only
 * to access experimental features. Experimental features may be deprecated in the
 * future. Use at your own risk.
 *
 * {@hide prototype}
 */
public abstract class ExperimentalBidirectionalStream extends BidirectionalStream {
    /**
     * {@link BidirectionalStream.Builder} that exposes experimental features. To obtain an
     * instance of this class, cast a {@code BidirectionalStream.Builder} to this type. Every
     * instance of {@code BidirectionalStream.Builder} can be cast to an instance of this class,
     * as they are backed by the same implementation and hence perform identically.
     * Instances of this class are not meant for general use, but instead only
     * to access experimental features. Experimental features may be deprecated in the
     * future. Use at your own risk.
     */
    public abstract static class Builder extends BidirectionalStream.Builder {
        /**
         * Associates the annotation object with this request. May add more than one.
         * Passed through to a {@link RequestFinishedInfo.Listener},
         * see {@link RequestFinishedInfo#getAnnotations}.
         *
         * @param annotation an object to pass on to the {@link RequestFinishedInfo.Listener} with a
         * {@link RequestFinishedInfo}.
         * @return the builder to facilitate chaining.
         */
        public Builder addRequestAnnotation(Object annotation) {
            return this;
        }

        /**
         * Binds the request to the specified network handle. The HTTP stack will send this request
         * only using the network associated to this handle. If this network disconnects the request
         * will  fail, the exact error will depend on the stage of request processing when
         * the network disconnects. Network handles can be obtained
         * through {@code Network#getNetworkHandle}.
         *
         * @param networkHandle the network handle to bind the request to. Specify
         *        {@link ExperimentalHttpEngine#UNBIND_NETWORK_HANDLE} to unbind.
         * @return the builder to facilitate chaining.
         */
        public Builder bindToNetwork(long networkHandle) {
            return this;
        }

        // To support method chaining, override superclass methods to return an
        // instance of this class instead of the parent.

        @Override
        public abstract Builder setHttpMethod(String method);

        @Override
        public abstract Builder addHeader(String header, String value);

        @Override
        public abstract Builder setPriority(int priority);

        @Override
        public abstract Builder setDelayRequestHeadersUntilFirstFlushEnabled(
                boolean delayRequestHeadersUntilFirstFlush);

        @Override
        public abstract ExperimentalBidirectionalStream build();
    }
}
