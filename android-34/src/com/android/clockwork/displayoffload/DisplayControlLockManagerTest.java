/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.os.IBinder;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DisplayControlLockManagerTest {
    @Mock Runnable mAcquireRunnableMock;
    @Mock Runnable mReleaseRunnableMock;
    @Mock
    IBinder mBinderMock;
    @Mock IBinder mBinderMock2;
    @Captor
    ArgumentCaptor<IBinder.DeathRecipient> mDeathRecipientCaptor;

    DisplayControlLockManager mDisplayControlLockManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mDisplayControlLockManager =
                new DisplayControlLockManager(mAcquireRunnableMock, mReleaseRunnableMock);
    }

    @Test
    public void testAcquireLockThenBinderDies() throws RemoteException {
        assertThat(mDisplayControlLockManager.acquire(mBinderMock, "lock1")).isTrue();
        // Since this is the first lock, verify the acquire runnable ran
        verify(mAcquireRunnableMock, times(1)).run();

        verify(mBinderMock).linkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        IBinder.DeathRecipient onLinkDeathRecipient = mDeathRecipientCaptor.getValue();

        // Simulate binder dying
        onLinkDeathRecipient.binderDied();

        verify(mBinderMock).unlinkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        IBinder.DeathRecipient onUnlinkDeathRecipient = mDeathRecipientCaptor.getValue();

        assertThat(onLinkDeathRecipient).isEqualTo(onUnlinkDeathRecipient);
        // Since there are no more locks, verify the release runnable ran
        verify(mReleaseRunnableMock, times(1)).run();
        // Since there are no more locks, shouldBlock() should return false
        assertThat(mDisplayControlLockManager.shouldBlock()).isFalse();
    }

    @Test
    public void testAcquireThenReleaseLock() throws RemoteException {
        // Acquire lock
        assertThat(mDisplayControlLockManager.acquire(mBinderMock, "lock1")).isTrue();
        // Since this is the first lock, verify the acquire runnable ran
        verify(mAcquireRunnableMock, times(1)).run();

        verify(mBinderMock).linkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        IBinder.DeathRecipient onLinkDeathRecipient = mDeathRecipientCaptor.getValue();

        // Release lock
        mDisplayControlLockManager.release(mBinderMock);

        verify(mBinderMock).unlinkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        IBinder.DeathRecipient onUnlinkDeathRecipient = mDeathRecipientCaptor.getValue();

        assertThat(onLinkDeathRecipient).isEqualTo(onUnlinkDeathRecipient);
        // Since there are no more locks, verify the release runnable ran
        verify(mReleaseRunnableMock, times(1)).run();
        // Since there are no more locks, shouldBlock() should return false
        assertThat(mDisplayControlLockManager.shouldBlock()).isFalse();
    }

    @Test
    public void testAcquireThenReleaseMultipleLocks() throws RemoteException {
        // Acquire lock
        assertThat(mDisplayControlLockManager.acquire(mBinderMock, "lock1")).isTrue();
        assertThat(mDisplayControlLockManager.acquire(mBinderMock2, "lock2")).isTrue();
        // Verify Locks' DeathRecipient are linked
        verify(mBinderMock).linkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        verify(mBinderMock2).linkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        // Verify acquire runnable is run once only
        verify(mAcquireRunnableMock).run();
        verifyNoMoreInteractions(mAcquireRunnableMock);
        verifyNoMoreInteractions(mReleaseRunnableMock);

        // Release lock
        mDisplayControlLockManager.release(mBinderMock);
        verifyNoMoreInteractions(mReleaseRunnableMock);
        mDisplayControlLockManager.release(mBinderMock2);
        verify(mReleaseRunnableMock).run();

        // Double unlock when no lock is held should not run any callbacks
        mDisplayControlLockManager.release(mBinderMock);
        verifyNoMoreInteractions(mAcquireRunnableMock);
        verifyNoMoreInteractions(mReleaseRunnableMock);
    }

    @Test
    public void testAcquireThenReleaseMultipleLocksDeathRecipient() throws RemoteException {
        // Acquire lock
        assertThat(mDisplayControlLockManager.acquire(mBinderMock, "lock1")).isTrue();
        assertThat(mDisplayControlLockManager.acquire(mBinderMock2, "lock2")).isTrue();
        // Verify Locks' DeathRecipient are linked
        verify(mBinderMock).linkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        IBinder.DeathRecipient onLinkDeathRecipient = mDeathRecipientCaptor.getValue();
        verify(mBinderMock2).linkToDeath(mDeathRecipientCaptor.capture(), anyInt());
        // Verify acquire runnable is ran once only
        verify(mAcquireRunnableMock).run();
        verifyNoMoreInteractions(mAcquireRunnableMock);
        verifyNoMoreInteractions(mReleaseRunnableMock);

        // Release lock
        mDisplayControlLockManager.release(mBinderMock2);
        verifyNoMoreInteractions(mReleaseRunnableMock);
        // Simulate binder dying
        onLinkDeathRecipient.binderDied();
        verify(mReleaseRunnableMock).run();

        verifyNoMoreInteractions(mAcquireRunnableMock);
        verifyNoMoreInteractions(mReleaseRunnableMock);
    }

    @Test
    public void testAcquireLockAfterBinderDied() throws RemoteException {
        // Throw RemoteException on linkToDeath() to simulate dead binder
        doThrow(new RemoteException())
                .when(mBinderMock)
                .linkToDeath(any(IBinder.DeathRecipient.class), anyInt());

        // Fail to acquire a lock
        assertThat(mDisplayControlLockManager.acquire(mBinderMock, "lock1")).isFalse();
    }

    @Test
    public void testReleaseWithoutAcquire() {
        mDisplayControlLockManager.release(mBinderMock);

        // no interactions with the binder or the runnable
        verifyNoMoreInteractions(mBinderMock);
        verifyNoMoreInteractions(mAcquireRunnableMock);
    }
}
