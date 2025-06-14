/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package android.os;

import android.annotation.FlaggedApi;
import android.annotation.Nullable;
import android.util.ArraySet;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Blocks a looper from executing any messages, and allows the holder of this object
 * to control when and which messages get executed until it is released.
 * <p>
 * A TestLooperManager should be acquired using
 * {@link android.app.Instrumentation#acquireLooperManager}. Until {@link #release()} is called,
 * the Looper thread will not execute any messages except when {@link #execute(Message)} is called.
 * The test code may use {@link #next()} to acquire messages that have been queued to this
 * {@link Looper}/{@link MessageQueue} and then {@link #execute} to run any that desires.
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public class TestLooperManager {

    private static final ArraySet<Looper> sHeldLoopers = new ArraySet<>();

    private final MessageQueue mQueue;
    private final Looper mLooper;
    private final LinkedBlockingQueue<MessageExecution> mExecuteQueue = new LinkedBlockingQueue<>();
    private final boolean mLooperIsMyLooper;

    // When this latch is zero, it's guaranteed that the LooperHolder Message
    // is not in the underlying queue.
    private final CountDownLatch mLooperHolderLatch = new CountDownLatch(1);

    private boolean mReleased;

    /**
     * @hide
     */
    public TestLooperManager(Looper looper) {
        synchronized (sHeldLoopers) {
            if (sHeldLoopers.contains(looper)) {
                throw new RuntimeException("TestLooperManager already held for this looper");
            }
            sHeldLoopers.add(looper);
        }
        mLooper = looper;
        mQueue = mLooper.getQueue();
        mLooperIsMyLooper = Looper.myLooper() == looper;
        if (!mLooperIsMyLooper) {
            // Post a message that will keep the looper blocked as long as we are dispatching.
            new Handler(looper).post(new LooperHolder());
        } else {
            mLooperHolderLatch.countDown();
        }
    }

    /**
     * Returns the {@link MessageQueue} this object is wrapping.
     */
    public MessageQueue getMessageQueue() {
        checkReleased();
        return mQueue;
    }

    /** @removed */
    @Deprecated
    public MessageQueue getQueue() {
        return getMessageQueue();
    }

    /**
     * Returns the next message that should be executed by this queue, may block
     * if no messages are ready.
     * <p>
     * Callers should always call {@link #recycle(Message)} on the message when all
     * interactions with it have completed.
     */
    public Message next() {
        checkReleased();
        waitForLooperHolder();
        return mQueue.next();
    }

    /**
     * Retrieves and removes the next message in this queue.
     * If the queue is empty, returns null.
     * This method never blocks.
     *
     * <p>Callers should always call {@link #recycle(Message)} on the message when all interactions
     * with it have completed.
     */
    @FlaggedApi(Flags.FLAG_MESSAGE_QUEUE_TESTABILITY)
    @Nullable
    public Message poll() {
        checkReleased();
        waitForLooperHolder();
        return mQueue.pollForTest();
    }

    /**
     * Retrieves, but does not remove, the values of {@link Message#when} of next message in the
     * queue.
     * If the queue is empty, returns null.
     * This method never blocks.
     */
    @FlaggedApi(Flags.FLAG_MESSAGE_QUEUE_TESTABILITY)
    @SuppressWarnings("AutoBoxing")  // box the primitive long, or return null to indicate no value
    @Nullable
    public Long peekWhen() {
        checkReleased();
        waitForLooperHolder();
        return mQueue.peekWhenForTest();
    }

    /**
     * Checks whether the Looper is currently blocked on a sync barrier.
     */
    @FlaggedApi(Flags.FLAG_MESSAGE_QUEUE_TESTABILITY)
    public boolean isBlockedOnSyncBarrier() {
        checkReleased();
        waitForLooperHolder();
        return mQueue.isBlockedOnSyncBarrier();
    }

    /**
     * Releases the looper to continue standard looping and processing of messages, no further
     * interactions with TestLooperManager will be allowed after release() has been called.
     */
    public void release() {
        synchronized (sHeldLoopers) {
            sHeldLoopers.remove(mLooper);
        }
        checkReleased();
        mReleased = true;
        mExecuteQueue.add(new MessageExecution());
    }

    /**
     * Executes the given message on the Looper thread this wrapper is
     * attached to.
     * <p>
     * Execution will happen on the Looper's thread (whether it is the current thread
     * or not), but all RuntimeExceptions encountered while executing the message will
     * be thrown on the calling thread.
     */
    public void execute(Message message) {
        checkReleased();
        if (mLooper.isCurrentThread()) {
            // This is being called from the thread it should be executed on, we can just dispatch.
            message.target.dispatchMessage(message);
        } else {
            if (mLooperIsMyLooper) {
                throw new RuntimeException("Cannot call execute from non Looper thread");
            }
            MessageExecution execution = new MessageExecution();
            execution.m = message;
            synchronized (execution) {
                mExecuteQueue.add(execution);
                // Wait for the message to be executed.
                try {
                    execution.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (execution.response != null) {
                    throw new RuntimeException(execution.response);
                }
            }
        }
    }

    /**
     * Called to indicate that a Message returned by {@link #next()} has been parsed
     * and should be recycled.
     */
    public void recycle(Message msg) {
        checkReleased();
        msg.recycleUnchecked();
    }

    /**
     * Returns true if there are any queued messages that match the parameters.
     *
     * @param h      the value of {@link Message#getTarget()}
     * @param what   the value of {@link Message#what}
     * @param object the value of {@link Message#obj}, null for any
     */
    public boolean hasMessages(Handler h, Object object, int what) {
        checkReleased();
        return mQueue.hasMessages(h, what, object);
    }

    /**
     * Returns true if there are any queued messages that match the parameters.
     *
     * @param h      the value of {@link Message#getTarget()}
     * @param r      the value of {@link Message#getCallback()}
     * @param object the value of {@link Message#obj}, null for any
     */
    public boolean hasMessages(Handler h, Object object, Runnable r) {
        checkReleased();
        return mQueue.hasMessages(h, r, object);
    }

    private void checkReleased() {
        if (mReleased) {
            throw new RuntimeException("release() has already be called");
        }
    }

    /**
     * Waits until the Looper is blocked by the LooperHolder, if one was posted.
     *
     * After this method returns, it's guaranteed that the LooperHolder Message
     * is not in the underlying queue.
     */
    private void waitForLooperHolder() {
        try {
            mLooperHolderLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class LooperHolder implements Runnable {
        @Override
        public void run() {
            mLooperHolderLatch.countDown();
            while (!mReleased) {
                try {
                    final MessageExecution take = mExecuteQueue.take();
                    if (take.m != null) {
                        processMessage(take);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void processMessage(MessageExecution mex) {
            synchronized (mex) {
                try {
                    mex.m.target.dispatchMessage(mex.m);
                    mex.response = null;
                } catch (Throwable t) {
                    mex.response = t;
                }
                mex.notifyAll();
            }
        }
    }

    private static class MessageExecution {
        private Message m;
        private Throwable response;
    }
}
