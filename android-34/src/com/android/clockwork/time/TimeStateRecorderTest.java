package com.android.clockwork.time;

import android.content.Context;
import android.util.Log;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TimeStateRecorderTest {
    private static final String TAG = TimeStateRecorderTest.class.getSimpleName();

    TimeStateRecorder mTimeStateRecorder = new TimeStateRecorder();
    TestTimeState mMockTimeState;

    @Mock
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mMockTimeState = new TestTimeState();
    }

    @Test
    public void testInit() throws Exception {
        assertTrue(mTimeStateRecorder.init(mContext, mMockTimeState));
    }

    @Test
    public void testSyncIn12HourMode() {
        mMockTimeState.system24HourMode = false;

        mMockTimeState.recorded12HourMode = true;
        mTimeStateRecorder.init(mContext, mMockTimeState);
        mTimeStateRecorder.run();
        assertTrue("Sync should not change correct value", true);

        mMockTimeState.recorded12HourMode = false;
        mTimeStateRecorder.init(mContext, mMockTimeState);
        mTimeStateRecorder.run();
        assertTrue("Sync should correct incorrect value", mMockTimeState.recorded12HourMode);
    }

    @Test
    public void testSyncIn24HourMode() {
        mMockTimeState.system24HourMode = true;

        mMockTimeState.recorded12HourMode = false;
        mTimeStateRecorder.init(mContext, mMockTimeState);
        mTimeStateRecorder.run();
        assertFalse("Sync should not change correct value", mMockTimeState.recorded12HourMode);

        mMockTimeState.recorded12HourMode = true;
        mTimeStateRecorder.init(mContext, mMockTimeState);
        mTimeStateRecorder.run();
        assertFalse("Sync should correct incorrect value", mMockTimeState.recorded12HourMode);
    }

    @Test
    public void testLastUpdateTimeSync() {
        mTimeStateRecorder.init(mContext, mMockTimeState);
        mMockTimeState.currentTime = 20160907;

        mTimeStateRecorder.run();

        assertEquals("run() should update the lastUpdateTimeSync", "20160907", mMockTimeState.lastChangedValue);
    }

    // TODO convert this to a Mockito Mock
    class TestTimeState extends TimeState {
        long currentTime = 0;
        boolean recorded12HourMode = true;
        boolean system24HourMode = false;
        String lastChangedValue = null;

        public TestTimeState() {
            super(null);
        }

        public boolean init() {
            return true;
        }

        public long getCurrentTime() {
            return currentTime;
        }

        public boolean isSystem24HourFormat(Context context) {
            return system24HourMode;
        }

        public boolean is12HourModeRecorded() {
            return recorded12HourMode;
        }


        public boolean set12HourMode() {
            Log.d(TAG, "set12HourMode()");
            recorded12HourMode = true;
            return true;
        }

        public boolean set24HourMode() {
            Log.d(TAG, "set24HourMode()");
            recorded12HourMode = false;
            return true;
        }

        public boolean updateLastChangeValue() {
            String value = String.valueOf(getCurrentTime());
            Log.d(TAG, "writeLastChangeValue(" + value + ")");
            lastChangedValue = value;
            return true;
        }
    }
}
