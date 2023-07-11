package com.android.clockwork.power;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class PowerTrackerTest {
    final ShadowApplication shadowApplication = ShadowApplication.getInstance();

    @Mock
    PowerManager mockPowerManager;
    @Mock
    PowerTracker.Listener mockListener;
    PowerTracker powerTracker;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        powerTracker = new PowerTracker(mContext, mockPowerManager);
        powerTracker.addListener(mockListener);
        powerTracker.onBootCompleted();
    }

    @Test
    public void testConstructorRegistersAppropriateReceivers() {
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(Intent.ACTION_POWER_CONNECTED)));
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(Intent.ACTION_POWER_DISCONNECTED)));
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)));
    }

    @Test
    public void testChargingState() {
        Assert.assertFalse(powerTracker.isCharging());
        mContext.sendBroadcast(new Intent(Intent.ACTION_POWER_CONNECTED));
        Assert.assertTrue(powerTracker.isCharging());
        verify(mockListener).onChargingStateChanged();

        reset(mockListener);
        mContext.sendBroadcast(new Intent(Intent.ACTION_POWER_DISCONNECTED));
        Assert.assertFalse(powerTracker.isCharging());
        verify(mockListener).onChargingStateChanged();
    }

    @Test
    public void testPowerSaveMode() {
        when(mockPowerManager.isPowerSaveMode()).thenReturn(true);
        mContext.sendBroadcast(new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        Assert.assertTrue(powerTracker.isInPowerSave());
        verify(mockListener).onPowerSaveModeChanged();

        reset(mockListener);

        when(mockPowerManager.isPowerSaveMode()).thenReturn(false);
        mContext.sendBroadcast(new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        Assert.assertFalse(powerTracker.isInPowerSave());
        verify(mockListener).onPowerSaveModeChanged();
    }

    @Test
    public void testBluetoothAllowListedFeature() {
        String[] features = new String[]{"bluetooth"};
        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_BT_INDEX), false);

        powerTracker.populateAllowListedFeatures(features);

        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_BT_INDEX), true);
    }

    @Test
    public void testWiFiAllowListedFeature() {
        String[] features = new String[]{"wifi"};
        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_WIFI_INDEX),
                false);

        powerTracker.populateAllowListedFeatures(features);

        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_WIFI_INDEX), true);
    }

    @Test
    public void testCellularAllowListedFeature() {
        String[] features = new String[]{"cellular"};
        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_CELLULAR_INDEX),
                false);

        powerTracker.populateAllowListedFeatures(features);

        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_CELLULAR_INDEX),
                true);
    }

    @Test
    public void testTouchAllowListedFeature() {
        String[] features = new String[]{"touch"};
        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_TOUCH_INDEX),
                false);

        powerTracker.populateAllowListedFeatures(features);

        Assert.assertEquals(powerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_TOUCH_INDEX),
                true);
    }

}
