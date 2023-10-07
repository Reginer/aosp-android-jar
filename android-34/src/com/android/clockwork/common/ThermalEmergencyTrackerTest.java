package com.android.clockwork.common;

import static android.os.Temperature.THROTTLING_EMERGENCY;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.OnThermalStatusChangedListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ThermalEmergencyTrackerTest {
    ContentResolver mContentResolver;

    @Mock
    PowerManager mMockPowerManager;
    ThermalEmergencyTracker mThermalEmergencyTracker;
    @Captor
    ArgumentCaptor<ThermalEmergencyTracker.ThermalEmergencyMode> mModeCaptor;
    @Captor
    ArgumentCaptor<OnThermalStatusChangedListener> mListenerCaptor;
    @Mock
    private Context mMockContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mMockContext.getSystemService(PowerManager.class)).thenReturn(mMockPowerManager);

        mThermalEmergencyTracker = new ThermalEmergencyTracker(mMockContext);

    }

    @Test
    public void testNotifyListeners() {
        verify(mMockPowerManager).addThermalStatusListener(mListenerCaptor.capture());

        ThermalEmergencyTracker.Listener fakeListener = new ThermalEmergencyTracker.Listener() {
            @Override
            public void onThermalEmergencyChanged(
                    ThermalEmergencyTracker.ThermalEmergencyMode mode) {
                assertEquals(mode.isWifiEffected(), true);
                assertEquals(mode.isBtEffected(), true);
                assertEquals(mode.isCellEffected(), true);
            }
        };

        mThermalEmergencyTracker.addListener(fakeListener);

        mListenerCaptor.getValue().onThermalStatusChanged(THROTTLING_EMERGENCY);
    }
}
