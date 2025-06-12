/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1997, 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.ref;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reference queues, to which registered reference objects are appended by the
 * garbage collector after the appropriate reachability changes are detected.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */
// BEGIN Android-changed: Reimplemented to accomodate a different GC and compiler.

public class ReferenceQueue<T> {

    // Reference.queueNext will be set to sQueueNextUnenqueued to indicate
    // when a reference has been enqueued and removed from its queue.
    private static final Reference sQueueNextUnenqueued = new PhantomReference(null, null);

    // NOTE: This implementation of ReferenceQueue is FIFO (queue-like) whereas
    // the OpenJdk implementation is LIFO (stack-like).
    private Reference<? extends T> head = null;
    private Reference<? extends T> tail = null;

    private final Object lock = new Object();

    // Current target of enqueuePending. Either a Cleaner or a ReferenceQueue.
    private static Object currentTarget = null;

    /**
     * Constructs a new reference-object queue.
     */
    public ReferenceQueue() { }

    /**
     * Enqueue the given reference onto this queue.
     * The caller is responsible for ensuring the lock is held on this queue,
     * and for calling notifyAll on this queue after the reference has been
     * enqueued. Returns true if the reference was enqueued successfully,
     * false if the reference had already been enqueued.
     * @GuardedBy("lock")
     */
    private boolean enqueueLocked(Reference<? extends T> r) {
        // Verify the reference has not already been enqueued.
        if (r.queueNext != null) {
            return false;
        }

        // Callers already handled sun.misc.Cleaner instances.
        if (tail == null) {
            head = r;
        } else {
            tail.queueNext = r;
        }
        tail = r;
        tail.queueNext = r;
        return true;
    }

    /**
     * The queue or Runnable from a sun.misc.Cleaner currently being targeted by enqueuePending.
     * Used only to get slightly informative output for timeouts. May be read via a data race,
     * but only for crash debugging output.
     * @hide
     */
    public static Object getCurrentTarget() {
        if (currentTarget instanceof sun.misc.Cleaner cleaner) {
            // The printed version of the Runnable is likely to be more informative than the
            // Cleaner itself.
            return cleaner.getThunk();
        } else {
            return currentTarget;
        }
    }

    /**
     * Test if the given reference object has been enqueued but not yet
     * removed from the queue, assuming this is the reference object's queue.
     */
    boolean isEnqueued(Reference<? extends T> reference) {
        synchronized (lock) {
            return reference.queueNext != null && reference.queueNext != sQueueNextUnenqueued;
        }
    }

    /**
     * Enqueue the reference object on the receiver.
     *
     * @param reference
     *            reference object to be enqueued.
     * @return true if the reference was enqueued.
     */
    boolean enqueue(Reference<? extends T> reference) {
        synchronized (lock) {
            if (reference instanceof sun.misc.Cleaner cl) {
                // If this reference is a Cleaner, then simply invoke the clean method instead of
                // enqueueing it in the queue. Cleaners are associated with placeholder queues
                // that are never polled (except for error checking) and objects are never
                // enqueued on them.
                cl.clean();

                // Update queueNext to indicate that the reference has been
                // enqueued, but is now removed from the queue.
                reference.queueNext = sQueueNextUnenqueued;
                return true;
            }

            if (enqueueLocked(reference)) {
                lock.notifyAll();
                return true;
            }
            return false;
        }
    }

    // @GuardedBy("lock")
    private Reference<? extends T> reallyPollLocked() {
        if (head != null) {
            Reference<? extends T> r = head;
            if (head == tail) {
                tail = null;
                head = null;
            } else {
                head = head.queueNext;
            }

            // Update queueNext to indicate that the reference has been
            // enqueued, but is now removed from the queue.
            r.queueNext = sQueueNextUnenqueued;
            return r;
        }

        return null;
    }

    /**
     * Polls this queue to see if a reference object is available.  If one is
     * available without further delay then it is removed from the queue and
     * returned.  Otherwise this method immediately returns <tt>null</tt>.
     *
     * @return  A reference object, if one was immediately available,
     *          otherwise <code>null</code>
     */
    public Reference<? extends T> poll() {
        synchronized (lock) {
            return reallyPollLocked();
        }
    }

    /**
     * Removes the next reference object in this queue, blocking until either
     * one becomes available or the given timeout period expires.
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method.
     *
     * @param  timeout  If positive, block for up to <code>timeout</code>
     *                  milliseconds while waiting for a reference to be
     *                  added to this queue.  If zero, block indefinitely.
     *
     * @return  A reference object, if one was available within the specified
     *          timeout period, otherwise <code>null</code>
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     *
     * @throws  InterruptedException
     *          If the timeout wait is interrupted
     */
    public Reference<? extends T> remove(long timeout)
        throws IllegalArgumentException, InterruptedException
    {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value");
        }
        synchronized (lock) {
            Reference<? extends T> r = reallyPollLocked();
            if (r != null) return r;
            long start = (timeout == 0) ? 0 : System.nanoTime();
            for (;;) {
                lock.wait(timeout);
                r = reallyPollLocked();
                if (r != null) return r;
                if (timeout != 0) {
                    long end = System.nanoTime();
                    timeout -= (end - start) / 1000_000;
                    if (timeout <= 0) return null;
                    start = end;
                }
            }
        }
    }

    /**
     * Removes the next reference object in this queue, blocking until one
     * becomes available.
     *
     * @return A reference object, blocking until one becomes available
     * @throws  InterruptedException  If the wait is interrupted
     */
    public Reference<? extends T> remove() throws InterruptedException {
        return remove(0);
    }

    /**
     * Enqueue the given list of currently pending (unenqueued) references.
     *
     * @hide
     */
    public static void enqueuePending(Reference<?> list, AtomicInteger progressCounter) {
        Reference<?> start = list;
        do {
            ReferenceQueue queue = list.queue;
            if (queue == null || sun.misc.Cleaner.isCleanerQueue(queue)) {
                Reference<?> next = list.pendingNext;
                // Always make pendingNext a self-loop to preserve the invariant that
                // once enqueued, pendingNext is non-null -- without leaking
                // the object pendingNext was previously pointing to.
                list.pendingNext = list;
                if (queue != null) {
                    // This is a Cleaner. Run directly without additional synchronization.
                    sun.misc.Cleaner cl = (sun.misc.Cleaner) list;
                    currentTarget = cl;
                    cl.clean();  // Idempotent. No need to check queueNext first. Handles all
                                 // exceptions.
                    list.queueNext = sQueueNextUnenqueued;
                }
                list = next;
            } else {
                currentTarget = queue;
                // To improve performance, we try to avoid repeated
                // synchronization on the same queue by batching enqueueing of
                // consecutive references in the list that have the same
                // queue. We limit this so that progressCounter gets incremented
                // occasionally,
                final int MAX_ITERS = 100;
                int i = 0;
                synchronized (queue.lock) {
                    // Nothing in here should throw, even OOME,
                    do {
                        Reference<?> next = list.pendingNext;
                        list.pendingNext = list;
                        queue.enqueueLocked(list);
                        list = next;
                    } while (list != start && list.queue == queue && ++i < MAX_ITERS);
                    queue.lock.notifyAll();
                }
            }
            progressCounter.incrementAndGet();
        } while (list != start);
        currentTarget = null;
        sun.misc.Cleaner.checkCleanerQueueEmpty();
    }

    /**
     * List of references that the GC says need to be enqueued.
     * Protected by ReferenceQueue.class lock.
     * @hide
     */
    public static Reference<?> unenqueued = null;

    static void add(Reference<?> list) {
        synchronized (ReferenceQueue.class) {
            if (unenqueued == null) {
                unenqueued = list;
            } else {
                // Find the last element in unenqueued.
                Reference<?> last = unenqueued;
                while (last.pendingNext != unenqueued) {
                  last = last.pendingNext;
                }
                // Add our list to the end. Update the pendingNext to point back to enqueued.
                last.pendingNext = list;
                last = list;
                while (last.pendingNext != list) {
                    last = last.pendingNext;
                }
                last.pendingNext = unenqueued;
            }
            ReferenceQueue.class.notifyAll();
        }
    }
}
// END Android-changed: Reimplemented to accomodate a different GC and compiler.
