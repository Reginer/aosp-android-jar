package com.android.clockwork.mediarouting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.Context;
import android.media.AudioManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.server.SystemService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
public final class WearMediaRoutingServiceTest {

    @Mock private WearAudioPlaybackCallback mMockWearAudioPlaybackCallback;
    @Mock private WearAudioDeviceCallback mMockWearAudioDeviceCallback;
    @Mock private WearAudioServerStateCallback mMockWearAudioServerStateCallback;
    @Mock private AudioManager mMockAudioManager;
    @Mock private AudioApiProvider mMockAudioApiProvider;
    private WearMediaRoutingService mService;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mService =
                new WearMediaRoutingService(
                        mContext,
                        mMockWearAudioPlaybackCallback,
                        mMockWearAudioDeviceCallback,
                        mMockWearAudioServerStateCallback,
                        mMockAudioManager,
                        mMockAudioApiProvider);
        WearMediaRoutingService.sIsAutoLaunchOutputSwitcherEnabled = true;
    }

    @Test
    public void bootComplete_shouldRegisterAudioPlaybackCallbackListener() {
        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        verify(mMockAudioManager)
                .registerAudioPlaybackCallback(mMockWearAudioPlaybackCallback, null);
    }

    @Test
    public void bootComplete_shouldRegisterAudioDeviceCallback() {
        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        verify(mMockAudioManager).registerAudioDeviceCallback(mMockWearAudioDeviceCallback, null);
    }

    @Test
    public void bootComplete_shouldRegisterAudioStateServerCallback() {
        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        verify(mMockAudioManager)
                .setAudioServerStateCallback(
                        any(Executor.class), eq(mMockWearAudioServerStateCallback));
    }

    @Test
    public void bootComplete_autoLaunchDisabled_shouldNotRegisterCallback() {
        WearMediaRoutingService.sIsAutoLaunchOutputSwitcherEnabled = false;

        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        verifyNoMoreInteractions(mMockAudioManager);
    }

    @Test
    public void bootComplete_shouldInvokeDisableSpeaker() {
        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        verify(mMockAudioApiProvider).disableSpeaker();
    }

    @Test
    public void bootComplete_autoLaunchDisabled_shouldNotInvokeDisableSpeaker() {
        WearMediaRoutingService.sIsAutoLaunchOutputSwitcherEnabled = false;

        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        verifyNoMoreInteractions(mMockAudioApiProvider);
    }
}
