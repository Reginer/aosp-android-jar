// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package org.chromium.net.impl;

import static android.net.http.ExperimentalHttpEngine.UNBIND_NETWORK_HANDLE;
import static android.net.http.UrlRequest.REQUEST_PRIORITY_MEDIUM;

import android.annotation.SuppressLint;
import android.net.Network;
import android.util.Log;
import android.util.Pair;

import android.net.http.HttpEngine;
import android.net.http.ExperimentalUrlRequest;
import android.net.http.RequestFinishedInfo;
import android.net.http.UploadDataProvider;
import android.net.http.UrlRequest;

import androidx.annotation.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Implements {@link ExperimentalUrlRequest.Builder}.
 */
public class UrlRequestBuilderImpl extends ExperimentalUrlRequest.Builder {
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String TAG = UrlRequestBuilderImpl.class.getSimpleName();

    // All fields are temporary storage of ExperimentalUrlRequest configuration to be
    // copied to built ExperimentalUrlRequest.

    // CronetEngineBase to execute request.
    private final CronetEngineBase mCronetEngine;
    // URL to request.
    private final String mUrl;
    // Callback to receive progress callbacks.
    private final UrlRequest.Callback mCallback;
    // Executor to invoke callback on.
    private final Executor mExecutor;
    // HTTP method (e.g. GET, POST etc).
    private String mMethod;

    // List of request headers, stored as header field name and value pairs.
    private final ArrayList<Map.Entry<String, String>> mRequestHeaders = new ArrayList<>();
    // Disable the cache for just this request.
    private boolean mDisableCache;
    // Disable connection migration for just this request.
    private boolean mDisableConnectionMigration;
    // Priority of request. Default is medium.
    @CronetEngineBase.RequestPriority
    private int mPriority = REQUEST_PRIORITY_MEDIUM;
    // Request reporting annotations. Avoid extra object creation if no annotations added.
    private Collection<Object> mRequestAnnotations;
    // If request is an upload, this provides the request body data.
    private UploadDataProvider mUploadDataProvider;
    // Executor to call upload data provider back on.
    private Executor mUploadDataProviderExecutor;
    private boolean mAllowDirectExecutor;
    private boolean mTrafficStatsTagSet;
    private int mTrafficStatsTag;
    private boolean mTrafficStatsUidSet;
    private int mTrafficStatsUid;
    private RequestFinishedInfo.Listener mRequestFinishedListener;
    private long mNetworkHandle = CronetEngineBase.DEFAULT_NETWORK_HANDLE;
    // Idempotency of the request.
    @CronetEngineBase.Idempotency
    private int mIdempotency = DEFAULT_IDEMPOTENCY;

    /**
     * Creates a builder for {@link UrlRequest} objects. All callbacks for
     * generated {@link UrlRequest} objects will be invoked on
     * {@code executor}'s thread. {@code executor} must not run tasks on the
     * current thread to prevent blocking networking operations and causing
     * exceptions during shutdown.
     *
     * @param url URL for the generated requests.
     * @param callback callback object that gets invoked on different events.
     * @param executor {@link Executor} on which all callbacks will be invoked.
     * @param cronetEngine {@link HttpEngine} used to execute this request.
     */
    UrlRequestBuilderImpl(String url, UrlRequest.Callback callback, Executor executor,
            CronetEngineBase cronetEngine) {
        super();
        if (url == null) {
            throw new NullPointerException("URL is required.");
        }
        if (callback == null) {
            throw new NullPointerException("Callback is required.");
        }
        if (executor == null) {
            throw new NullPointerException("Executor is required.");
        }
        if (cronetEngine == null) {
            throw new NullPointerException("CronetEngine is required.");
        }
        mUrl = url;
        mCallback = callback;
        mExecutor = executor;
        mCronetEngine = cronetEngine;
    }

    @Override
    public ExperimentalUrlRequest.Builder setHttpMethod(String method) {
        if (method == null) {
            throw new NullPointerException("Method is required.");
        }
        mMethod = method;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl addHeader(String header, String value) {
        if (header == null) {
            throw new NullPointerException("Invalid header name.");
        }
        if (value == null) {
            throw new NullPointerException("Invalid header value.");
        }
        if (ACCEPT_ENCODING.equalsIgnoreCase(header)) {
            Log.w(TAG, "It's not necessary to set Accept-Encoding on requests - cronet will do"
                            + " this automatically for you, and setting it yourself has no "
                            + "effect. See https://crbug.com/581399 for details.",
                    new Exception());
            return this;
        }
        mRequestHeaders.add(new AbstractMap.SimpleImmutableEntry<String, String>(header, value));
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setCacheDisabled(boolean disableCache) {
        mDisableCache = disableCache;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl disableConnectionMigration() {
        mDisableConnectionMigration = true;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setPriority(@CronetEngineBase.RequestPriority int priority) {
        mPriority = priority;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setIdempotency(@CronetEngineBase.Idempotency int idempotency) {
        mIdempotency = idempotency;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setUploadDataProvider(
            UploadDataProvider uploadDataProvider, Executor executor) {
        if (uploadDataProvider == null) {
            throw new NullPointerException("Invalid UploadDataProvider.");
        }
        if (executor == null) {
            throw new NullPointerException("Invalid UploadDataProvider Executor.");
        }
        if (mMethod == null) {
            mMethod = "POST";
        }
        mUploadDataProvider = uploadDataProvider;
        mUploadDataProviderExecutor = executor;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setDirectExecutorAllowed(boolean allowDirectExecutor) {
        mAllowDirectExecutor = allowDirectExecutor;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl addRequestAnnotation(Object annotation) {
        if (annotation == null) {
            throw new NullPointerException("Invalid metrics annotation.");
        }
        if (mRequestAnnotations == null) {
            mRequestAnnotations = new ArrayList<>();
        }
        mRequestAnnotations.add(annotation);
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setTrafficStatsTag(int tag) {
        mTrafficStatsTagSet = true;
        mTrafficStatsTag = tag;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setTrafficStatsUid(int uid) {
        mTrafficStatsUidSet = true;
        mTrafficStatsUid = uid;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl setRequestFinishedListener(RequestFinishedInfo.Listener listener) {
        mRequestFinishedListener = listener;
        return this;
    }

    @Override
    public UrlRequestBuilderImpl bindToNetwork(@Nullable Network network) {
        if (network == null) {
            mNetworkHandle = UNBIND_NETWORK_HANDLE;
        } else {
            mNetworkHandle = network.getNetworkHandle();
        }
        return this;
    }

    @Override
    public UrlRequestBase build() {
        @SuppressLint("WrongConstant") // TODO(jbudorick): Remove this after rolling to the N SDK.
        final UrlRequestBase request = mCronetEngine.createRequest(mUrl, mCallback, mExecutor,
                mPriority, mRequestAnnotations, mDisableCache, mDisableConnectionMigration,
                mAllowDirectExecutor, mTrafficStatsTagSet, mTrafficStatsTag, mTrafficStatsUidSet,
                mTrafficStatsUid, mRequestFinishedListener, mIdempotency, mNetworkHandle,
                new HeaderBlockImpl(mRequestHeaders));
        if (mMethod != null) {
            request.setHttpMethod(mMethod);
        }
        if (mUploadDataProvider != null) {
            request.setUploadDataProvider(mUploadDataProvider, mUploadDataProviderExecutor);
        }
        return request;
    }
}
