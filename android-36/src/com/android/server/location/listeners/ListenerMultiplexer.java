/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.location.listeners;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.listeners.ListenerExecutor.ListenerOperation;
import com.android.internal.util.Preconditions;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A base class to multiplex some event source to multiple listener registrations. Every listener is
 * represented by a registration object which stores all required state for a listener. Keys are
 * used to uniquely identify every registration. Listener operations may be executed on
 * registrations in order to invoke the represented listener.
 *
 * <p>Registrations are divided into two categories, active registrations and inactive
 * registrations, as defined by {@link #isActive(ListenerRegistration)}. The set of active
 * registrations is combined into a single merged registration, which is submitted to the backing
 * event source when necessary in order to register with the event source. The merged registration
 * is updated whenever the set of active registration changes. Listeners will only be invoked for
 * active registrations.
 *
 * <p>In order to inform the multiplexer of state changes, if a registration's active state changes,
 * or if the merged registration changes, {@link #updateRegistrations(Predicate)} or {@link
 * #updateRegistration(Object, Predicate)} must be invoked and return true for any registration
 * whose state may have changed in such a way that the active state or merged registration state has
 * changed. It is acceptable to return true from a predicate even if nothing has changed, though
 * this may result in extra pointless work.
 *
 * <p>Callbacks invoked for various changes will always be ordered according to this lifecycle list:
 *
 * <ul>
 *   <li>{@link #onRegister()}
 *   <li>{@link ListenerRegistration#onRegister(Object)}
 *   <li>{@link #onRegistrationAdded(Object, ListenerRegistration)}
 *   <li>{@link #onActive()}
 *   <li>{@link ListenerRegistration#onActive()}
 *   <li>{@link ListenerRegistration#onInactive()}
 *   <li>{@link #onInactive()}
 *   <li>{@link #onRegistrationRemoved(Object, ListenerRegistration)}
 *   <li>{@link ListenerRegistration#onUnregister()}
 *   <li>{@link #onUnregister()}
 * </ul>
 *
 * <p>If one registration replaces another, then {@link #onRegistrationReplaced(Object,
 * ListenerRegistration, Object, ListenerRegistration)} is invoked instead of {@link
 * #onRegistrationRemoved(Object, ListenerRegistration)} and {@link #onRegistrationAdded(Object,
 * ListenerRegistration)}.
 *
 * <p>Adding registrations is not allowed to be called re-entrantly (ie, while in the middle of some
 * other operation or callback). Removal is allowed re-entrantly, however only via {@link
 * #removeRegistration(Object, ListenerRegistration)}, not via any other removal method. This
 * ensures re-entrant removal does not accidentally remove the incorrect registration.
 *
 * @param <TKey>                key type
 * @param <TListener>           listener type
 * @param <TRegistration>       registration type
 * @param <TMergedRegistration> merged registration type
 */
public abstract class ListenerMultiplexer<TKey, TListener,
        TRegistration extends ListenerRegistration<TListener>, TMergedRegistration> {

    /**
     * The lock object used by the multiplexer. Acquiring this lock allows for multiple operations
     * on the multiplexer to be completed atomically. Otherwise, it is not required to hold this
     * lock. This lock is held while invoking all lifecycle callbacks on both the multiplexer and
     * any registrations.
     */
    protected final Object mMultiplexerLock = new Object();

    @GuardedBy("mMultiplexerLock")
    private final ArrayMap<TKey, TRegistration> mRegistrations = new ArrayMap<>();

    private final UpdateServiceBuffer mUpdateServiceBuffer = new UpdateServiceBuffer();

    private final ReentrancyGuard mReentrancyGuard = new ReentrancyGuard();

    @GuardedBy("mMultiplexerLock")
    private int mActiveRegistrationsCount = 0;

    @GuardedBy("mMultiplexerLock")
    private boolean mServiceRegistered = false;

    @GuardedBy("mMultiplexerLock")
    @Nullable private TMergedRegistration mMerged;

    /**
     * Should be implemented to register with the backing service with the given merged
     * registration, and should return true if a matching call to {@link #unregisterWithService()}
     * is required to unregister (ie, if registration succeeds). The set of registrations passed in
     * is the same set passed into {@link #mergeRegistrations(Collection)} to generate the merged
     * registration.
     *
     * <p class="note">It may seem redundant to pass in the set of active registrations when they
     * have already been used to generate the merged request, and indeed, for many implementations
     * this parameter can likely simply be ignored. However, some implementations may require access
     * to the set of registrations used to generate the merged requestion for further logic even
     * after the merged registration has been generated.
     *
     * @see #mergeRegistrations(Collection)
     * @see #reregisterWithService(Object, Object, Collection)
     */
    @GuardedBy("mMultiplexerLock")
    protected abstract boolean registerWithService(TMergedRegistration merged,
            @NonNull Collection<TRegistration> registrations);

    /**
     * Invoked when the service has already been registered with some merged registration, and is
     * now being registered with a different merged registration. The default implementation simply
     * invokes {@link #registerWithService(Object, Collection)}.
     *
     * @see #registerWithService(Object, Collection)
     */
    @GuardedBy("mMultiplexerLock")
    protected boolean reregisterWithService(TMergedRegistration oldMerged,
            TMergedRegistration newMerged, @NonNull Collection<TRegistration> registrations) {
        return registerWithService(newMerged, registrations);
    }

    /**
     * Should be implemented to unregister from the backing service.
     */
    @GuardedBy("mMultiplexerLock")
    protected abstract void unregisterWithService();

    /**
     * Defines whether a registration is currently active or not. Only active registrations will be
     * forwarded to {@link #registerWithService(Object, Collection)}, and listener invocations will
     * only be delivered to active requests. If a registration's active state changes,
     * {@link #updateRegistrations(Predicate)} must be invoked with a function that returns true for
     * any registrations that may have changed their active state.
     */
    @GuardedBy("mMultiplexerLock")
    protected abstract boolean isActive(@NonNull TRegistration registration);

    /**
     * Called in order to generate a merged registration from the given set of active registrations.
     * The list of registrations will never be empty. If the resulting merged registration is equal
     * to the currently registered merged registration, nothing further will happen. If the merged
     * registration differs, {@link #registerWithService(Object, Collection)} or
     * {@link #reregisterWithService(Object, Object, Collection)} will be invoked with the new
     * merged registration so that the backing service can be updated.
     */
    @GuardedBy("mMultiplexerLock")
    protected abstract TMergedRegistration mergeRegistrations(
            @NonNull Collection<TRegistration> registrations);

    /**
     * Invoked when the multiplexer goes from having no registrations to having some registrations.
     * This is a convenient entry point for registering listeners, etc, which only need to be
     * present while there are any registrations. Invoked while holding the multiplexer's internal
     * lock.
     */
    @GuardedBy("mMultiplexerLock")
    protected void onRegister() {}

    /**
     * Invoked when the multiplexer goes from having some registrations to having no registrations.
     * This is a convenient entry point for unregistering listeners, etc, which only need to be
     * present while there are any registrations. Invoked while holding the multiplexer's internal
     * lock.
     */
    @GuardedBy("mMultiplexerLock")
    protected void onUnregister() {}

    /**
     * Invoked when a registration is added. Invoked while holding the multiplexer's internal lock.
     */
    @GuardedBy("mMultiplexerLock")
    protected void onRegistrationAdded(@NonNull TKey key, @NonNull TRegistration registration) {}

    /**
     * Invoked when one registration replaces another (through {@link #replaceRegistration(Object,
     * Object, ListenerRegistration)}). The old registration has already been unregistered at this
     * point. Invoked while holding the multiplexer's internal lock.
     *
     * <p>The default behavior is simply to call first {@link #onRegistrationRemoved(Object,
     * ListenerRegistration)} and then {@link #onRegistrationAdded(Object, ListenerRegistration)}.
     */
    @GuardedBy("mMultiplexerLock")
    protected void onRegistrationReplaced(
            @NonNull TKey oldKey,
            @NonNull TRegistration oldRegistration,
            @NonNull TKey newKey,
            @NonNull TRegistration newRegistration) {
        onRegistrationRemoved(oldKey, oldRegistration);
        onRegistrationAdded(newKey, newRegistration);
    }

    /**
     * Invoked when a registration is removed. Invoked while holding the multiplexer's internal
     * lock.
     */
    @GuardedBy("mMultiplexerLock")
    protected void onRegistrationRemoved(@NonNull TKey key, @NonNull TRegistration registration) {}

    /**
     * Invoked when the multiplexer goes from having no active registrations to having some active
     * registrations. This is a convenient entry point for registering listeners, etc, which only
     * need to be present while there are active registrations. Invoked while holding the
     * multiplexer's internal lock.
     */
    @GuardedBy("mMultiplexerLock")
    protected void onActive() {}

    /**
     * Invoked when the multiplexer goes from having some active registrations to having no active
     * registrations. This is a convenient entry point for unregistering listeners, etc, which only
     * need to be present while there are active registrations. Invoked while holding the
     * multiplexer's internal lock.
     */
    @GuardedBy("mMultiplexerLock")
    protected void onInactive() {}

    /**
     * Puts a new registration with the given key, replacing any previous registration under the
     * same key. This method cannot be called to put a registration re-entrantly.
     */
    protected final void putRegistration(@NonNull TKey key, @NonNull TRegistration registration) {
        replaceRegistration(key, key, registration);
    }

    /**
     * Atomically removes the registration with the old key and adds a new registration with the
     * given key. If there was a registration for the old key, {@link
     * #onRegistrationReplaced(Object, ListenerRegistration, Object, ListenerRegistration)} will be
     * invoked instead of {@link #onRegistrationAdded(Object, ListenerRegistration)}, even if they
     * share the same key. The old key may be the same value as the new key, in which case this
     * function is equivalent to {@link #putRegistration(Object, ListenerRegistration)}. This method
     * cannot be called to add a registration re-entrantly.
     */
    protected final void replaceRegistration(@NonNull TKey oldKey, @NonNull TKey key,
            @NonNull TRegistration registration) {
        Objects.requireNonNull(oldKey);
        Objects.requireNonNull(key);
        Objects.requireNonNull(registration);

        synchronized (mMultiplexerLock) {
            // adding listeners reentrantly is not supported
            Preconditions.checkState(!mReentrancyGuard.isReentrant());

            // new key may only have a prior registration if the oldKey is the same as the key
            Preconditions.checkArgument(oldKey == key || !mRegistrations.containsKey(key));

            // since adding a registration can invoke a variety of callbacks, we need to ensure
            // those callbacks themselves do not re-enter, as this could lead to out-of-order
            // callbacks. further, we buffer service updates since adding a registration may
            // involve removing a prior registration. note that try-with-resources ordering is
            // meaningful here as well. we want to close the reentrancy guard first, as this may
            // generate additional service updates, then close the update service buffer.
            try (UpdateServiceBuffer ignored1 = mUpdateServiceBuffer.acquire();
                 ReentrancyGuard ignored2 = mReentrancyGuard.acquire()) {

                boolean wasEmpty = mRegistrations.isEmpty();

                TRegistration oldRegistration = null;
                int oldIndex = mRegistrations.indexOfKey(oldKey);
                if (oldIndex >= 0) {
                    // remove ourselves instead of using remove(), to balance registration callbacks
                    oldRegistration = mRegistrations.valueAt(oldIndex);
                    unregister(oldRegistration);
                    oldRegistration.onUnregister();
                    if (oldKey != key) {
                        mRegistrations.removeAt(oldIndex);
                    }
                }
                if (oldKey == key && oldIndex >= 0) {
                    mRegistrations.setValueAt(oldIndex, registration);
                } else {
                    mRegistrations.put(key, registration);
                }

                if (wasEmpty) {
                    onRegister();
                }
                registration.onRegister(key);
                if (oldRegistration == null) {
                    onRegistrationAdded(key, registration);
                } else {
                    onRegistrationReplaced(oldKey, oldRegistration, key, registration);
                }
                onRegistrationActiveChanged(registration);
            }
        }
    }

    /**
     * Removes all registrations with keys that satisfy the given predicate. This method cannot be
     * called to remove a registration re-entrantly.
     */
    protected final void removeRegistrationIf(@NonNull Predicate<TKey> predicate) {
        synchronized (mMultiplexerLock) {
            // this method does not support removing listeners reentrantly
            Preconditions.checkState(!mReentrancyGuard.isReentrant());

            // since removing a registration can invoke a variety of callbacks, we need to ensure
            // those callbacks themselves do not re-enter, as this could lead to out-of-order
            // callbacks. further, we buffer service updates since chains of removeLater()
            // invocations could result in multiple service updates. note that try-with-resources
            // ordering is meaningful here as well. we want to close the reentrancy guard first, as
            // this may generate additional service updates, then close the update service buffer.
            try (UpdateServiceBuffer ignored1 = mUpdateServiceBuffer.acquire();
                 ReentrancyGuard ignored2 = mReentrancyGuard.acquire()) {

                final int size = mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TKey key = mRegistrations.keyAt(i);
                    if (predicate.test(key)) {
                        removeRegistration(key, mRegistrations.valueAt(i));
                    }
                }
            }
        }
    }

    /**
     * Removes the registration with the given key. This method cannot be called to remove a
     * registration re-entrantly.
     */
    protected final void removeRegistration(TKey key) {
        synchronized (mMultiplexerLock) {
            // this method does not support removing listeners reentrantly
            Preconditions.checkState(!mReentrancyGuard.isReentrant());

            int index = mRegistrations.indexOfKey(key);
            if (index < 0) {
                return;
            }

            removeRegistration(index);
        }
    }

    /**
     * Removes the given registration with the given key. If the given key has a different
     * registration at the time this method is called, nothing happens. This method allows for
     * re-entrancy, and may be called to remove a registration re-entrantly.
     */
    protected final void removeRegistration(@NonNull TKey key,
            @NonNull ListenerRegistration<?> registration) {
        synchronized (mMultiplexerLock) {
            int index = mRegistrations.indexOfKey(key);
            if (index < 0) {
                return;
            }

            TRegistration typedRegistration = mRegistrations.valueAt(index);
            if (typedRegistration != registration) {
                return;
            }

            if (mReentrancyGuard.isReentrant()) {
                unregister(typedRegistration);
                mReentrancyGuard.markForRemoval(key, typedRegistration);
            } else {
                removeRegistration(index);
            }
        }
    }

    @GuardedBy("mMultiplexerLock")
    private void removeRegistration(int index) {
        TKey key = mRegistrations.keyAt(index);
        TRegistration registration = mRegistrations.valueAt(index);

        // since removing a registration can invoke a variety of callbacks, we need to ensure those
        // callbacks themselves do not re-enter, as this could lead to out-of-order callbacks.
        // further, we buffer service updates since chains of removeLater() invocations could result
        // in multiple service updates. note that try-with-resources ordering is meaningful here as
        // well. we want to close the reentrancy guard first, as this may generate additional
        // service updates, then close the update service buffer.
        try (UpdateServiceBuffer ignored1 = mUpdateServiceBuffer.acquire();
             ReentrancyGuard ignored2 = mReentrancyGuard.acquire()) {

            unregister(registration);
            onRegistrationRemoved(key, registration);
            registration.onUnregister();
            mRegistrations.removeAt(index);
            if (mRegistrations.isEmpty()) {
                onUnregister();
            }
        }
    }

    /**
     * Forces a re-evalution of the merged request for all active registrations and updates service
     * registration accordingly.
     */
    protected final void updateService() {
        synchronized (mMultiplexerLock) {
            if (mUpdateServiceBuffer.isBuffered()) {
                mUpdateServiceBuffer.markUpdateServiceRequired();
                return;
            }

            final int size = mRegistrations.size();
            ArrayList<TRegistration> actives = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                TRegistration registration = mRegistrations.valueAt(i);
                if (registration.isActive()) {
                    actives.add(registration);
                }
            }

            if (actives.isEmpty()) {
                if (mServiceRegistered) {
                    mMerged = null;
                    mServiceRegistered = false;
                    unregisterWithService();
                }
            } else {
                TMergedRegistration merged = mergeRegistrations(actives);
                if (mServiceRegistered) {
                    if (!Objects.equals(merged, mMerged)) {
                        mServiceRegistered = reregisterWithService(mMerged, merged, actives);
                        mMerged = mServiceRegistered ? merged : null;
                    }
                } else {
                    mServiceRegistered = registerWithService(merged, actives);
                    mMerged = mServiceRegistered ? merged : null;
                }
            }
        }
    }

    /**
     * If the service is currently registered, unregisters it and then calls
     * {@link #updateService()} so that {@link #registerWithService(Object, Collection)} will be
     * re-invoked. This is useful, for instance, if the backing service has crashed or otherwise
     * lost state, and needs to be re-initialized. Because this unregisters first, this is safe to
     * use even if there is a possibility the backing server has not crashed, or has already been
     * reinitialized.
     */
    protected final void resetService() {
        synchronized (mMultiplexerLock) {
            if (mServiceRegistered) {
                mMerged = null;
                mServiceRegistered = false;
                unregisterWithService();
                updateService();
            }
        }
    }

    /**
     * Begins buffering calls to {@link #updateService()} until {@link UpdateServiceLock#close()}
     * is called. This is useful to prevent extra work when combining multiple calls (for example,
     * buffering {@code updateService()} until after multiple adds/removes/updates occur.
     */
    public UpdateServiceLock newUpdateServiceLock() {
        return new UpdateServiceLock(mUpdateServiceBuffer);
    }

    /**
     * Evaluates the predicate on all registrations until the predicate returns true, at which point
     * evaluation will cease. Returns true if the predicate ever returned true, and returns false
     * otherwise.
     */
    protected final boolean findRegistration(Predicate<TRegistration> predicate) {
        synchronized (mMultiplexerLock) {
            // we only acquire a reentrancy guard in case of removal while iterating. this method
            // does not directly affect active state or merged state, so there is no advantage to
            // acquiring an update source buffer.
            try (ReentrancyGuard ignored = mReentrancyGuard.acquire()) {
                final int size = mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TRegistration registration = mRegistrations.valueAt(i);
                    if (predicate.test(registration)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    /**
     * Evaluates the predicate on all registrations. The predicate should return true if the active
     * state of the registration may have changed as a result. If the active state of any
     * registration has changed, {@link #updateService()} will automatically be invoked to handle
     * the resulting changes.
     */
    protected final void updateRegistrations(@NonNull Predicate<TRegistration> predicate) {
        synchronized (mMultiplexerLock) {
            // since updating a registration can invoke a variety of callbacks, we need to ensure
            // those callbacks themselves do not re-enter, as this could lead to out-of-order
            // callbacks. note that try-with-resources ordering is meaningful here as well. we want
            // to close the reentrancy guard first, as this may generate additional service updates,
            // then close the update service buffer.
            try (UpdateServiceBuffer ignored1 = mUpdateServiceBuffer.acquire();
                 ReentrancyGuard ignored2 = mReentrancyGuard.acquire()) {

                final int size = mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TRegistration registration = mRegistrations.valueAt(i);
                    if (predicate.test(registration)) {
                        onRegistrationActiveChanged(registration);
                    }
                }
            }
        }
    }

    /**
     * Evaluates the predicate on a registration with the given key. The predicate should return
     * true if the active state of the registration may have changed as a result. If the active
     * state of the registration has changed, {@link #updateService()} will automatically be invoked
     * to handle the resulting changes. Returns true if there is a registration with the given key
     * (and thus the predicate was invoked), and false otherwise.
     */
    protected final boolean updateRegistration(@NonNull Object key,
            @NonNull Predicate<TRegistration> predicate) {
        synchronized (mMultiplexerLock) {
            // since updating a registration can invoke a variety of callbacks, we need to ensure
            // those callbacks themselves do not re-enter, as this could lead to out-of-order
            // callbacks. note that try-with-resources ordering is meaningful here as well. we want
            // to close the reentrancy guard first, as this may generate additional service updates,
            // then close the update service buffer.
            try (UpdateServiceBuffer ignored1 = mUpdateServiceBuffer.acquire();
                 ReentrancyGuard ignored2 = mReentrancyGuard.acquire()) {

                int index = mRegistrations.indexOfKey(key);
                if (index < 0) {
                    return false;
                }

                TRegistration registration = mRegistrations.valueAt(index);
                if (predicate.test(registration)) {
                    onRegistrationActiveChanged(registration);
                }
                return true;
            }
        }
    }

    @GuardedBy("mMultiplexerLock")
    private void onRegistrationActiveChanged(TRegistration registration) {
        boolean active = registration.isRegistered() && isActive(registration);
        boolean changed = registration.setActive(active);
        if (changed) {
            if (active) {
                if (++mActiveRegistrationsCount == 1) {
                    onActive();
                }
                registration.onActive();
            } else {
                registration.onInactive();
                if (--mActiveRegistrationsCount == 0) {
                    onInactive();
                }
            }

            updateService();
        }
    }

    /**
     * Executes the given function for all active registrations. If the function returns a non-null
     * operation, that operation will be invoked with the associated listener. The function may not
     * change the active state of the registration.
     */
    protected final void deliverToListeners(
            @NonNull Function<TRegistration, ListenerOperation<TListener>> function) {
        synchronized (mMultiplexerLock) {
            try (ReentrancyGuard ignored = mReentrancyGuard.acquire()) {
                final int size = mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TRegistration registration = mRegistrations.valueAt(i);
                    if (registration.isActive()) {
                        ListenerOperation<TListener> operation = function.apply(registration);
                        if (operation != null) {
                            registration.executeOperation(operation);
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes the given operation for all active listeners. This is a convenience function
     * equivalent to:
     * <pre>
     * deliverToListeners(registration -> operation);
     * </pre>
     */
    protected final void deliverToListeners(@NonNull ListenerOperation<TListener> operation) {
        synchronized (mMultiplexerLock) {
            try (ReentrancyGuard ignored = mReentrancyGuard.acquire()) {
                final int size = mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TRegistration registration = mRegistrations.valueAt(i);
                    if (registration.isActive()) {
                        registration.executeOperation(operation);
                    }
                }
            }
        }
    }

    @GuardedBy("mMultiplexerLock")
    private void unregister(TRegistration registration) {
        registration.unregisterInternal();
        onRegistrationActiveChanged(registration);
    }

    /**
     * Dumps debug information.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (mMultiplexerLock) {
            pw.print("service: ");
            pw.print(getServiceState());
            pw.println();

            if (!mRegistrations.isEmpty()) {
                pw.println("listeners:");

                final int size = mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TRegistration registration = mRegistrations.valueAt(i);
                    pw.print("  ");
                    pw.print(registration);
                    if (!registration.isActive()) {
                        pw.println(" (inactive)");
                    } else {
                        pw.println();
                    }
                }
            }
        }
    }

    /**
     * May be overridden to provide additional details on service state when dumping the manager
     * state. Invoked while holding the multiplexer's internal lock.
     */
    @GuardedBy("mMultiplexerLock")
    protected String getServiceState() {
        if (mServiceRegistered) {
            if (mMerged != null) {
                return mMerged.toString();
            } else {
                return "registered";
            }
        } else {
            return "unregistered";
        }
    }

    /**
     * A reference counted helper class that guards against re-entrancy, and also helps implement
     * registration removal during reentrancy. When this class is {@link #acquire()}d, it increments
     * the reference count. To check whether re-entrancy is occurring, clients may use
     * {@link #isReentrant()}, and modify their behavior (such as by failing the call, or calling
     * {@link #markForRemoval(Object, ListenerRegistration)}). When this class is {@link #close()}d,
     * any key/registration pairs that were marked for removal prior to the close operation will
     * then be removed - which is safe since the operation will no longer be re-entrant.
     */
    private final class ReentrancyGuard implements AutoCloseable {

        @GuardedBy("mMultiplexerLock")
        private int mGuardCount;

        @GuardedBy("mMultiplexerLock")
        @Nullable private ArraySet<Entry<TKey, ListenerRegistration<?>>> mScheduledRemovals;

        ReentrancyGuard() {
            mGuardCount = 0;
            mScheduledRemovals = null;
        }

        boolean isReentrant() {
            synchronized (mMultiplexerLock) {
                return mGuardCount != 0;
            }
        }

        void markForRemoval(TKey key, ListenerRegistration<?> registration) {
            synchronized (mMultiplexerLock) {
                Preconditions.checkState(isReentrant());

                if (mScheduledRemovals == null) {
                    mScheduledRemovals = new ArraySet<>(mRegistrations.size());
                }
                mScheduledRemovals.add(new AbstractMap.SimpleImmutableEntry<>(key, registration));
            }
        }

        ReentrancyGuard acquire() {
            synchronized (mMultiplexerLock) {
                ++mGuardCount;
                return this;
            }
        }

        @Override
        public void close() {
            synchronized (mMultiplexerLock) {
                Preconditions.checkState(mGuardCount > 0);

                ArraySet<Entry<TKey, ListenerRegistration<?>>> scheduledRemovals = null;

                if (--mGuardCount == 0) {
                    scheduledRemovals = mScheduledRemovals;
                    mScheduledRemovals = null;
                }

                if (scheduledRemovals == null) {
                    return;
                }

                try (UpdateServiceBuffer ignored = mUpdateServiceBuffer.acquire()) {
                    final int size = scheduledRemovals.size();
                    for (int i = 0; i < size; i++) {
                        Entry<TKey, ListenerRegistration<?>> entry = scheduledRemovals.valueAt(i);
                        removeRegistration(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    /**
     * A reference counted helper class that buffers class to {@link #updateService()}. Since
     * {@link #updateService()} iterates through every registration and performs request merging
     * work, it can often be the most expensive part of any update to the multiplexer. This means
     * that if multiple calls to updateService() can be buffered, work will be saved. This class
     * allows clients to begin buffering calls after {@link #acquire()}ing this class, and when
     * {@link #close()} is called, any buffered calls to {@link #updateService()} will be combined
     * into a single final call. Clients should acquire this class when they are doing work that
     * could potentially result in multiple calls to updateService(), and close when they are done
     * with that work.
     */
    private final class UpdateServiceBuffer implements AutoCloseable {

        // requires internal locking because close() may be exposed externally and could be called
        // from any thread

        @GuardedBy("this")
        private int mBufferCount;

        @GuardedBy("this")
        private boolean mUpdateServiceRequired;

        UpdateServiceBuffer() {
            mBufferCount = 0;
            mUpdateServiceRequired = false;
        }

        synchronized boolean isBuffered() {
            return mBufferCount != 0;
        }

        synchronized void markUpdateServiceRequired() {
            Preconditions.checkState(isBuffered());
            mUpdateServiceRequired = true;
        }

        synchronized UpdateServiceBuffer acquire() {
            ++mBufferCount;
            return this;
        }

        @Override
        public void close() {
            boolean updateServiceRequired = false;
            synchronized (this) {
                Preconditions.checkState(mBufferCount > 0);
                if (--mBufferCount == 0) {
                    updateServiceRequired = mUpdateServiceRequired;
                    mUpdateServiceRequired = false;
                }
            }

            if (updateServiceRequired) {
                updateService();
            }
        }
    }

    /**
     * Acquiring this lock will buffer all calls to {@link #updateService()} until the lock is
     * {@link #close()}ed. This can be used to save work by acquiring the lock before multiple calls
     * to updateService() are expected, and closing the lock after.
     */
    public static final class UpdateServiceLock implements AutoCloseable {

        @Nullable private ListenerMultiplexer<?, ?, ?, ?>.UpdateServiceBuffer mUpdateServiceBuffer;

        UpdateServiceLock(ListenerMultiplexer<?, ?, ?, ?>.UpdateServiceBuffer updateServiceBuffer) {
            mUpdateServiceBuffer = updateServiceBuffer.acquire();
        }

        @Override
        public void close() {
            if (mUpdateServiceBuffer != null) {
                ListenerMultiplexer<?, ?, ?, ?>.UpdateServiceBuffer buffer = mUpdateServiceBuffer;
                mUpdateServiceBuffer = null;
                buffer.close();
            }
        }
    }
}
