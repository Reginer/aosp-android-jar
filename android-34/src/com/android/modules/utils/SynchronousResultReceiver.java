/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.modules.utils;


import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Generic interface for receiving a callback result from someone.
 * Allow the server end to synchronously wait on the response from the client.
 * This enables an RPC like system but with the ability to timeout and discard late results.
 *
 * <p>NOTE: Use the static {@link #get} method to retrieve an available instance of this class.
 * If no instances are available, a new one is created.
 */
public final class SynchronousResultReceiver<T> implements Parcelable {
    private static final String TAG = "SynchronousResultReceiver";
    private final boolean mLocal;
    private boolean mIsCompleted;
    private final static Object sLock = new Object();
    private final static int QUEUE_THRESHOLD = 4;

    @GuardedBy("sLock")
    private CompletableFuture<Result<T>> mFuture = new CompletableFuture<>();

    @GuardedBy("sLock")
    private static final ConcurrentLinkedQueue<SynchronousResultReceiver> sAvailableReceivers
            = new ConcurrentLinkedQueue<>();

    public static <T> SynchronousResultReceiver<T> get() {
        synchronized(sLock) {
            if (sAvailableReceivers.isEmpty()) {
                return new SynchronousResultReceiver();
            }
            SynchronousResultReceiver receiver = sAvailableReceivers.poll();
            receiver.resetLocked();
            return receiver;
        }
    }

    private SynchronousResultReceiver() {
        mLocal = true;
        mIsCompleted = false;
    }

    @GuardedBy("sLock")
    private void releaseLocked() {
        mFuture = null;
        if (sAvailableReceivers.size() < QUEUE_THRESHOLD) {
            sAvailableReceivers.add(this);
        }
    }

    @GuardedBy("sLock")
    private void resetLocked() {
        mFuture = new CompletableFuture<>();
        mIsCompleted = false;
    }

    private CompletableFuture<Result<T>> getFuture() {
       synchronized (sLock) {
           return mFuture;
       }
    }

    public static class Result<T> implements Parcelable {
        private final @Nullable T mObject;
        private final RuntimeException mException;

        public Result(RuntimeException exception) {
            mObject = null;
            mException = exception;
        }

        public Result(@Nullable T object) {
            mObject = object;
            mException = null;
        }

        /**
         * Return the stored value
         * May throw a {@link RuntimeException} thrown from the client
         */
        public T getValue(T defaultValue) {
            if (mException != null) {
                throw mException;
            }
            if (mObject == null) {
                return defaultValue;
            }
            return mObject;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeValue(mObject);
            out.writeValue(mException);
        }

        private Result(Parcel in) {
            mObject = (T)in.readValue(null);
            mException= (RuntimeException)in.readValue(null);
        }

        public static final @NonNull Parcelable.Creator<Result<?>> CREATOR =
            new Parcelable.Creator<Result<?>>() {
                public Result createFromParcel(Parcel in) {
                    return new Result(in);
                }
                public Result[] newArray(int size) {
                    return new Result[size];
                }
            };
    }

    private void complete(Result<T> result) {
        if (mIsCompleted) {
            throw new IllegalStateException("Receiver has already been completed");
        }
        mIsCompleted = true;
        if (mLocal) {
            getFuture().complete(result);
        } else {
            final ISynchronousResultReceiver rr;
            synchronized (this) {
                rr = mReceiver;
            }
            if (rr != null) {
                try {
                    rr.send(result);
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to complete future");
                }
            }
        }
    }

    /**
     * Deliver a result to this receiver.
     *
     * @param resultData Additional data provided by you.
     */
    public void send(@Nullable T resultData) {
        complete(new Result<>(resultData));
    }

    /**
     * Deliver an {@link Exception} to this receiver
     *
     * @param e exception to be sent
     */
    public void propagateException(@NonNull RuntimeException e) {
        Objects.requireNonNull(e, "RuntimeException cannot be null");
        complete(new Result<>(e));
    }

    /**
     * Blocks waiting for the result from the remote client.
     *
     * If it is interrupted before completion of the duration, wait again with remaining time until
     * the deadline.
     *
     * @param timeout The duration to wait before sending a {@link TimeoutException}
     * @return the Result
     * @throws TimeoutException if the timeout in milliseconds expired.
     */
    public @NonNull Result<T> awaitResultNoInterrupt(@NonNull Duration timeout)
            throws TimeoutException {
        Objects.requireNonNull(timeout, "Null timeout is not allowed");

        final long startWaitNanoTime = SystemClock.elapsedRealtimeNanos();
        Duration remainingTime = timeout;
        while (!remainingTime.isNegative()) {
            try {
                Result<T> result = getFuture().get(remainingTime.toMillis(), TimeUnit.MILLISECONDS);
                synchronized (sLock) {
                    releaseLocked();
                    return result;
                }
            } catch (ExecutionException e) {
                // This will NEVER happen.
                throw new AssertionError("Error receiving response", e);
            } catch (InterruptedException e) {
                // The thread was interrupted, try and get the value again, this time
                // with the remaining time until the deadline.
                remainingTime = timeout.minus(
                        Duration.ofNanos(SystemClock.elapsedRealtimeNanos() - startWaitNanoTime));
            }
        }
        synchronized (sLock) {
            releaseLocked();
        }
        throw new TimeoutException();
    }

    ISynchronousResultReceiver mReceiver = null;

    private final class MyResultReceiver extends ISynchronousResultReceiver.Stub {
        public void send(@SuppressWarnings("rawtypes") @NonNull Result result) {
            @SuppressWarnings("unchecked") Result<T> res = (Result<T>) result;
            CompletableFuture<Result<T>> future;
            future = getFuture();
            if (future != null) {
                future.complete(res);
            }
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(@NonNull Parcel out, int flags) {
        synchronized (this) {
            if (mReceiver == null) {
                mReceiver = new MyResultReceiver();
            }
            out.writeStrongBinder(mReceiver.asBinder());
        }
    }

    private SynchronousResultReceiver(Parcel in) {
        mLocal = false;
        mIsCompleted = false;
        mReceiver = ISynchronousResultReceiver.Stub.asInterface(in.readStrongBinder());
    }

    public static final @NonNull Parcelable.Creator<SynchronousResultReceiver<?>> CREATOR =
            new Parcelable.Creator<SynchronousResultReceiver<?>>() {
            public SynchronousResultReceiver<?> createFromParcel(Parcel in) {
                return new SynchronousResultReceiver(in);
            }
            public SynchronousResultReceiver<?>[] newArray(int size) {
                return new SynchronousResultReceiver[size];
            }
        };
}
