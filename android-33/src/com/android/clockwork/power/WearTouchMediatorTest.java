package com.android.clockwork.power;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.remote.Home;
import com.android.clockwork.remote.WetMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.BitSet;

/** Test for {@link WearTouchMediator} */
@RunWith(RobolectricTestRunner.class)
public class WearTouchMediatorTest {
    private final ShadowApplication shadowApplication = ShadowApplication.getInstance();

    @Mock
    EnablableInputDevice mockInputDevice;
    @Mock
    WearTouchMediator.TouchInputProvider mockTouchInputProvider;

    @Mock
    AmbientConfig mockAmbientConfig;
    @Mock
    PowerTracker mockPowerTracker;
    @Mock
    TimeOnlyMode mockTimeOnlyMode;

    @Mock
    BooleanFlag mockUserAbsentTouchOffFlag;

    private Context mContext;
    private WearTouchMediator mMediator;

    private BitSet mBitSet;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        when(mockAmbientConfig.isTouchToWake()).thenReturn(true);
        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(false);
        when(mockTimeOnlyMode.isTouchToWakeDisabled()).thenReturn(true);

        when(mockUserAbsentTouchOffFlag.isEnabled()).thenReturn(true);

        when(mockTouchInputProvider.getTouchInput()).thenReturn(mockInputDevice);

        mMediator =
                new WearTouchMediator(
                        mContext,
                        mockAmbientConfig,
                        mockPowerTracker,
                        mockTimeOnlyMode,
                        mockTouchInputProvider,
                        mockUserAbsentTouchOffFlag);

        mBitSet = new BitSet(PowerTracker.MAX_DOZE_MODE_INDEX);
        when(mockPowerTracker.getDozeModeAllowListedFeatures())
                .thenReturn(mBitSet);
    }

    @Test
    public void testOnBootCompleted() {
        mMediator.onBootCompleted();

        verify(mockAmbientConfig).addListener(mMediator);
        verify(mockTimeOnlyMode).addListener(mMediator);
        verify(mockPowerTracker).addListener(mMediator);

        assertThat(shadowApplication.hasReceiverForIntent(
                new Intent(Intent.ACTION_SCREEN_ON)))
                .isTrue();
        assertThat(shadowApplication.hasReceiverForIntent(
                new Intent(Intent.ACTION_SCREEN_OFF)))
                .isTrue();
        assertThat(shadowApplication.hasReceiverForIntent(
                new Intent(Home.ACTION_ENABLE_TOUCH)))
                .isTrue();
        assertThat(shadowApplication.hasReceiverForIntent(
                new Intent(Home.ACTION_DISABLE_TOUCH)))
                .isTrue();
        assertThat(shadowApplication.hasReceiverForIntent(
                new Intent(WetMode.ACTION_WET_MODE_STARTED)))
                .isTrue();
        assertThat(shadowApplication.hasReceiverForIntent(
                new Intent(WetMode.ACTION_WET_MODE_ENDED)))
                .isTrue();

        verify(mockInputDevice).enable();
    }

    @Test
    public void testAmbient_touchToWakeEnabled() {
        doBootCompleted();

        when(mockAmbientConfig.isTouchToWake()).thenReturn(true);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));

        verify(mockInputDevice, never()).disable();
    }

    @Test
    public void testAmbient_touchToWakeDisabled() {
        doBootCompleted();

        when(mockAmbientConfig.isTouchToWake()).thenReturn(false);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        verify(mockInputDevice).disable();

        verifyNoOtherEnable(WearTouchMediator.Reason.OFF_AMBIENT);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));
        verify(mockInputDevice).enable();
    }

    @Test
    public void testDoze_flagDisabled() {
        doBootCompleted();

        mMediator.onUserAbsentTouchOffChanged(false);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();

        verify(mockInputDevice, never()).disable();
    }

    @Test
    public void testDoze_flagEnabled() {
        doBootCompleted();

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verify(mockInputDevice).disable();

        verifyNoOtherEnable(WearTouchMediator.Reason.OFF_DOZE);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();
        verify(mockInputDevice).enable();
    }

    @Test
    public void testDoze_flagToggle() {
        doBootCompleted();

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        reset(mockInputDevice);

        mMediator.onUserAbsentTouchOffChanged(false);
        verify(mockInputDevice).enable();

        mMediator.onUserAbsentTouchOffChanged(true);
        verify(mockInputDevice).disable();
    }

    @Test
    public void testDeviceIdleAllowListedFeature() {
        doBootCompleted();
        mBitSet.set(PowerTracker.DOZE_MODE_TOUCH_INDEX);
        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);

        mMediator.onDeviceIdleModeChanged();

        verify(mockInputDevice, never()).disable();
        verify(mockInputDevice, never()).enable();
    }

    @Test
    public void testDeviceIdleBlockListedFeature() {
        doBootCompleted();

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verify(mockInputDevice).disable();
    }

    @Test
    public void testTimeOnlyMode_touchToWakeEnabled() {
        when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(true);
        when(mockTimeOnlyMode.isTouchToWakeDisabled()).thenReturn(false);

        doBootCompleted();

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));

        verify(mockInputDevice, never()).disable();
    }

    @Test
    public void testTimeOnlyMode_touchToWakeDisabled() {
        when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(true);
        when(mockTimeOnlyMode.isTouchToWakeDisabled()).thenReturn(true);

        doBootCompleted();

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        verify(mockInputDevice).disable();
        verifyNoOtherEnable(WearTouchMediator.Reason.OFF_BATTERY_SAVER);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));
        verify(mockInputDevice).enable();
    }


    @Test
    public void testTimeOnlyMode_disabledToEnabled() {
        when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(false);
        when(mockTimeOnlyMode.isTouchToWakeDisabled()).thenReturn(false);

        doBootCompleted();

        when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(true);
        when(mockTimeOnlyMode.isTouchToWakeDisabled()).thenReturn(true);

        mMediator.onTimeOnlyModeChanged(true);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        verify(mockInputDevice).disable();
        verifyNoOtherEnable(WearTouchMediator.Reason.OFF_BATTERY_SAVER);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));
        verify(mockInputDevice).enable();
    }

    @Test
    public void testTimeOnlyMode_enabledToDisabled() {
        when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(true);
        when(mockTimeOnlyMode.isTouchToWakeDisabled()).thenReturn(true);

        doBootCompleted();

        when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(false);
        when(mockTimeOnlyMode.isTouchToWakeDisabled()).thenReturn(false);

        mMediator.onTimeOnlyModeChanged(false);

        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
        mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));

        verify(mockInputDevice, never()).disable();
    }

    @Test
    public void testHome() {
        doBootCompleted();

        mMediator.mTouchReceiver.onReceive(mContext,
                new Intent(Home.ACTION_DISABLE_TOUCH));
        verify(mockInputDevice).disable();

        verifyNoOtherEnable(WearTouchMediator.Reason.OFF_HOME_REQUEST);

        mMediator.mTouchReceiver.onReceive(mContext,
                new Intent(Home.ACTION_ENABLE_TOUCH));
        verify(mockInputDevice).enable();
    }

    @Test
    public void testWetMode() {
        doBootCompleted();

        mMediator.mTouchReceiver.onReceive(mContext,
                new Intent(WetMode.ACTION_WET_MODE_STARTED));
        verify(mockInputDevice).disable();

        verifyNoOtherEnable(WearTouchMediator.Reason.OFF_TOUCH_LOCK);

        mMediator.mTouchReceiver.onReceive(mContext,
                new Intent(WetMode.ACTION_WET_MODE_ENDED));
        verify(mockInputDevice).enable();
    }

    /**
     * Make sure that toggling other settings does not trigger an enable, given that something else
     * disabled the sensor before calling this method.
     *
     * @param reason The reason the touch sensor is already disabled; don't test toggling this
     *               feature
     */
    private void verifyNoOtherEnable(WearTouchMediator.Reason reason) {
        reset(mockInputDevice);
        if (reason != WearTouchMediator.Reason.OFF_AMBIENT &&
                reason != WearTouchMediator.Reason.OFF_BATTERY_SAVER) {
            mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_OFF));
            mMediator.receiver.onReceive(mContext, new Intent(Intent.ACTION_SCREEN_ON));
            verify(mockInputDevice, times(2)).disable();
            reset(mockInputDevice);
        }
        if (reason != WearTouchMediator.Reason.OFF_BATTERY_SAVER) {
            when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(true);
            mMediator.onTimeOnlyModeChanged(true);
            when(mockTimeOnlyMode.isInTimeOnlyMode()).thenReturn(false);
            mMediator.onTimeOnlyModeChanged(false);
            verify(mockInputDevice, times(2)).disable();
            reset(mockInputDevice);
        }
        if (reason != WearTouchMediator.Reason.OFF_DOZE) {
            when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
            mMediator.onDeviceIdleModeChanged();
            when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
            mMediator.onDeviceIdleModeChanged();
            verify(mockInputDevice, times(2)).disable();
            reset(mockInputDevice);
        }
        if (reason != WearTouchMediator.Reason.OFF_HOME_REQUEST) {
            mMediator.mTouchReceiver.onReceive(mContext,
                    new Intent(Home.ACTION_DISABLE_TOUCH));
            mMediator.mTouchReceiver.onReceive(mContext,
                    new Intent(Home.ACTION_ENABLE_TOUCH));
            verify(mockInputDevice, times(2)).disable();
            reset(mockInputDevice);
        }
        if (reason != WearTouchMediator.Reason.OFF_TOUCH_LOCK) {
            mMediator.mTouchReceiver.onReceive(mContext,
                    new Intent(WetMode.ACTION_WET_MODE_STARTED));
            mMediator.mTouchReceiver.onReceive(mContext,
                    new Intent(WetMode.ACTION_WET_MODE_ENDED));
            verify(mockInputDevice, times(2)).disable();
            reset(mockInputDevice);
        }
    }

    private void doBootCompleted() {
        mMediator.onBootCompleted();
        reset(mockInputDevice);
    }
}
