package com.android.clockwork.time;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class TimeStateTest {

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private TimeState mTimeState;

    @Before
    public void setUp() throws IOException {
        File dataDirectory = mTemporaryFolder.newFolder("time_dir");
        mTimeState = new TimeState(dataDirectory);
    }

    @Test
    public void testInit() {
        assertTrue(mTimeState.init());
    }

    @Test
    public void testSet12HourMode() {
        mTimeState.init();

        // first set 24 hour mode
        mTimeState.set24HourMode();
        assertFalse(mTimeState.is12HourModeRecorded());

        // then set 12 hour mode
        mTimeState.set12HourMode();
        assertTrue(mTimeState.is12HourModeRecorded());
    }

    @Test
    public void testSet24HourMode() {
        mTimeState.init();

        // first set 12 hour mode
        mTimeState.set12HourMode();
        assertTrue(mTimeState.is12HourModeRecorded());

        // then set 24 hour mode
        mTimeState.set24HourMode();
        assertFalse(mTimeState.is12HourModeRecorded());
    }
}
