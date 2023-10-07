package com.android.clockwork.bluetooth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class MultistageExponentialBackoffTest {

    @Test
    public void testBackoffDistribution() {
        MultistageExponentialBackoff backoff = new MultistageExponentialBackoff(2, 5, 300);

        List<Integer> expected = Arrays.asList(
                2, 2, 2, 2, 2, 4, 8, 16, 32, 64, 128, 256, 300, 300, 300, 300);
        List<Integer> actual = new ArrayList<>();
        for (int i = 0; i < expected.size(); i++) {
            actual.add(backoff.getNextBackoff());
        }
        assertEquals(expected, actual);

        backoff.reset();
        assertEquals(2, backoff.getNextBackoff());
    }
}
