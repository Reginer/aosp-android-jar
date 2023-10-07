package com.android.clockwork.power;

import static com.android.clockwork.power.WearCoreControlMediator.MODE_IDLE;
import static com.android.clockwork.power.WearCoreControlMediator.MODE_PERF;
import static com.android.clockwork.power.WearCoreControlMediator.SYSPROP_CORE_CONTROL_ENABLE;
import static com.android.clockwork.power.WearCoreControlMediator.SYSPROP_CORE_CONTROL_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowSystemProperties;

/** Test for {@link WearCoreControlMediator} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSystemProperties.class})
public class WearCoreControlMediatorTest {
    private final ShadowApplication shadowApplication = ShadowApplication.getInstance();

    @Mock PowerTracker mockPowerTracker;

    private Context mContext;
    private WearCoreControlMediator mMediator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);

        mContext = RuntimeEnvironment.application;
        mMediator = new WearCoreControlMediator(mContext, null, mockPowerTracker);
    }

    private void doBootCompleted() {
        ShadowSystemProperties.reset();
        mMediator.onBootCompleted();
    }

    @Test
    public void testOnBootCompleted() {
        mMediator.onBootCompleted();

        verify(mockPowerTracker).addListener(mMediator);

        assertThat(shadowApplication.hasReceiverForIntent(new Intent(Intent.ACTION_SCREEN_ON)))
                .isTrue();
        assertThat(shadowApplication.hasReceiverForIntent(new Intent(Intent.ACTION_SCREEN_OFF)))
                .isTrue();

        assertThat(SystemProperties.get(SYSPROP_CORE_CONTROL_MODE)).isEqualTo(MODE_PERF);
    }

    @Test
    public void testCharging_PerfMode() {
        doBootCompleted();

        when(mockPowerTracker.isCharging()).thenReturn(true);
        mMediator.onChargingStateChanged();

        assertThat(SystemProperties.get(SYSPROP_CORE_CONTROL_MODE)).isEqualTo(MODE_PERF);
    }

    @Test
    public void testUnplugScreenOff_IdleMode() {
        doBootCompleted();

        when(mockPowerTracker.isCharging()).thenReturn(false);
        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        assertThat(SystemProperties.get(SYSPROP_CORE_CONTROL_MODE)).isEqualTo(MODE_IDLE);

        mMediator.onChargingStateChanged();
        assertThat(SystemProperties.get(SYSPROP_CORE_CONTROL_MODE)).isEqualTo(MODE_IDLE);
    }

    @Test
    public void testUnplugScreenOn_PerfMode() {
        doBootCompleted();

        when(mockPowerTracker.isCharging()).thenReturn(false);
        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        assertThat(SystemProperties.get(SYSPROP_CORE_CONTROL_MODE)).isEqualTo(MODE_IDLE);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));
        assertThat(SystemProperties.get(SYSPROP_CORE_CONTROL_MODE)).isEqualTo(MODE_PERF);
    }

    @Test
    public void testDeviceIdle_IdleMode() {
        doBootCompleted();

        when(mockPowerTracker.isCharging()).thenReturn(false);
        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        assertThat(SystemProperties.get(SYSPROP_CORE_CONTROL_MODE)).isEqualTo(MODE_IDLE);
    }

    @Test
    public void testShouldEnable() {
        ShadowSystemProperties.reset();
        SystemProperties.set(SYSPROP_CORE_CONTROL_ENABLE, "true");
        assertThat(WearCoreControlMediator.shouldEnable()).isTrue();

        ShadowSystemProperties.reset();
        SystemProperties.set(SYSPROP_CORE_CONTROL_ENABLE, "false");
        assertThat(WearCoreControlMediator.shouldEnable()).isFalse();

        ShadowSystemProperties.reset();
        assertThat(WearCoreControlMediator.shouldEnable()).isFalse();
    }
}
