/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.modules.utils;

import androidx.test.filters.SmallTest;
import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@RunWith(JUnit4.class)
@SmallTest
public class SynchronousResultReceiverTest extends TestCase {
    private static final Duration OK_TIME = Duration.ofMillis(100);
    private static final Duration NEG_TIME = Duration.ofSeconds(-1);

    @Test
    public void testSimpleData() throws Exception {
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        recv.send(true);
        final boolean result = recv.awaitResultNoInterrupt(OK_TIME).getValue(false);
        assertTrue(result);
    }

    @Test
    public void testDoubleComplete() throws Exception {
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        recv.send(true);
        Assert.assertThrows(IllegalStateException.class,
                () -> recv.send(true));
    }

    @Test
    public void testDefaultValue() throws Exception {
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        recv.send(null);
        assertTrue(recv.awaitResultNoInterrupt(OK_TIME).getValue(true));
    }

    @Test
    public void testPropagateException() throws Exception {
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        recv.propagateException(new RuntimeException("Placeholder exception"));
        Assert.assertThrows(RuntimeException.class,
                () -> recv.awaitResultNoInterrupt(OK_TIME).getValue(false));
    }

    @Test
    public void testTimeout() throws Exception {
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        Assert.assertThrows(TimeoutException.class,
                () -> recv.awaitResultNoInterrupt(OK_TIME));
    }

    @Test
    public void testNegativeTime() throws Exception {
        final SynchronousResultReceiver<Boolean> recv = SynchronousResultReceiver.get();
        recv.send(false);
        Assert.assertThrows(TimeoutException.class,
                () -> recv.awaitResultNoInterrupt(NEG_TIME));
    }
}
