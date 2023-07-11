package com.android.clockwork.cellular;

import android.content.ContentResolver;
import android.provider.Settings;

import com.android.internal.telephony.PhoneConstants;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Test for {@link WearCellularMediatorSettings}
 */
@RunWith(RobolectricTestRunner.class)
public class WearCellularMediatorSettingsTest {
    ContentResolver cr;

    WearCellularMediatorSettings mCellularSettings;

    @Before
    public void setup() {
        cr = RuntimeEnvironment.application.getContentResolver();
        mCellularSettings =
                new WearCellularMediatorSettings(RuntimeEnvironment.application, true, "");
    }

    @Test
    public void testGetCellAutoSetting() {
        Settings.Global.putInt(
                cr,
                Settings.Global.CELL_ON,
                PhoneConstants.CELL_ON_FLAG);

        Assert.assertTrue(mCellularSettings.getCellState() == PhoneConstants.CELL_ON_FLAG);
    }

    @Test
    public void testIsLocalEditionDevice() {
        WearCellularMediatorSettings settingsLocalEdition =
                new WearCellularMediatorSettings(RuntimeEnvironment.application, true, "");
        WearCellularMediatorSettings settingsNonLocalEdition =
                new WearCellularMediatorSettings(RuntimeEnvironment.application, false, "");

        Assert.assertTrue(settingsLocalEdition.isLocalEditionDevice());
        Assert.assertFalse(settingsNonLocalEdition.isLocalEditionDevice());
    }

    @Test
    public void testGetRadioOnState() {
        List validStates = Arrays.asList(
                WearCellularMediator.RADIO_ON_STATE_UNKNOWN,
                WearCellularMediator.RADIO_ON_STATE_ON,
                WearCellularMediator.RADIO_ON_STATE_OFF);

        /* getRadioOnState claims that only these states can be returned */
        Assert.assertTrue(validStates.contains(mCellularSettings.getRadioOnState()));
    }
}
