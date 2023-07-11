package com.android.clockwork.power;

import android.content.ContentResolver;
import android.provider.Settings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TimeOnlyModeTest {
    ContentResolver cr;

    @Mock PowerTracker mockPowerTracker;
    @Mock TimeOnlyMode.Listener mockListener;
    TimeOnlyMode timeOnlyMode;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        cr = RuntimeEnvironment.application.getContentResolver();
        timeOnlyMode = new TimeOnlyMode(cr, mockPowerTracker);
        timeOnlyMode.addListener(mockListener);
        when(mockPowerTracker.isCharging()).thenReturn(false);
        when(mockPowerTracker.isInPowerSave()).thenReturn(false);
    }

    @Test
    public void testConstructorListensToPowerTracker() {
        verify(mockPowerTracker).addListener(timeOnlyMode);
    }

    @Test
    public void testAllModesWhenFeatureEnabled() {
        Settings.Global.putString(cr, Settings.Global.TIME_ONLY_MODE_CONSTANTS,
                timeOnlyModeSettingsString(true, true, true));

        // when power saver mode is off, everything should be off
        when(mockPowerTracker.isInPowerSave()).thenReturn(false);
        Assert.assertFalse(timeOnlyMode.isInTimeOnlyMode());
        Assert.assertFalse(timeOnlyMode.isTiltToWakeDisabled());
        Assert.assertFalse(timeOnlyMode.isTouchToWakeDisabled());

        // when power saver mode is on, everything should come on
        when(mockPowerTracker.isInPowerSave()).thenReturn(true);
        Assert.assertTrue(timeOnlyMode.isInTimeOnlyMode());
        Assert.assertTrue(timeOnlyMode.isTiltToWakeDisabled());
        Assert.assertTrue(timeOnlyMode.isTouchToWakeDisabled());
    }

    @Test
    public void testTimeOnlyModeListenersGetNotified() {
        Settings.Global.putString(cr, Settings.Global.TIME_ONLY_MODE_CONSTANTS,
                timeOnlyModeSettingsString(true, true, true));

        // if TimeOnlyMode feature is enabled, changes in power save should result in
        // proper notifications being fired
        when(mockPowerTracker.isInPowerSave()).thenReturn(false);
        timeOnlyMode.onPowerSaveModeChanged();
        verify(mockListener).onTimeOnlyModeChanged(false);

        reset(mockListener);
        when(mockPowerTracker.isInPowerSave()).thenReturn(true);
        timeOnlyMode.onPowerSaveModeChanged();
        verify(mockListener).onTimeOnlyModeChanged(true);

        // if TimeOnlyMode feature is disabled, no notifications should occur
        reset(mockListener);
        Settings.Global.putString(cr, Settings.Global.TIME_ONLY_MODE_CONSTANTS,
                timeOnlyModeSettingsString(false, false, false));

        when(mockPowerTracker.isInPowerSave()).thenReturn(false);
        timeOnlyMode.onPowerSaveModeChanged();
        when(mockPowerTracker.isInPowerSave()).thenReturn(true);
        timeOnlyMode.onPowerSaveModeChanged();
        verifyNoMoreInteractions(mockListener);
    }

    /**
     * Returns a Settings string which corresponds to the specified time only settings.
     *
     * The Settings parser expects the setting string to be of the following format:
     * <pre>key1=value,key2=value,key3=value</pre>
     *
     */
    private String timeOnlyModeSettingsString(
            boolean timeOnlyEnabled, boolean disableTiltToWake, boolean disableTouchToWake) {
        StringBuilder sb = new StringBuilder();
        sb.append(TimeOnlyMode.KEY_ENABLED).append("=").append(timeOnlyEnabled);
        sb.append(",");
        sb.append(TimeOnlyMode.KEY_DISABLE_TILT_TO_WAKE).append("=").append(disableTiltToWake);
        sb.append(",");
        sb.append(TimeOnlyMode.KEY_DISABLE_TOUCH_TO_WAKE).append("=").append(disableTouchToWake);
        return sb.toString();
    }

}
