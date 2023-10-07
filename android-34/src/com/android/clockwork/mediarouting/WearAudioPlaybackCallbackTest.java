package com.android.clockwork.mediarouting;

import static com.android.clockwork.mediarouting.WearAudioPlaybackCallback.ACTION_OUTPUT_SWITCHER;
import static com.android.clockwork.mediarouting.WearAudioPlaybackCallback.EXTRA_MEDIA_APP_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioPlaybackConfiguration;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class WearAudioPlaybackCallbackTest {

    private static final String PACKAGE_NAME_MEDIA_ROUTING =
            "com.google.android.wearable.media.routing";
    private static final String FAKE_MEDIA_ROUTING_ACTIVITY = "MediaRoutingFakeActivity";
    private static final String FAKE_MEDIA_APP_PACKAGE_NAME = "com.fake.music";
    private static final int FAKE_MEDIA_APP_UID = 10;
    private static final int MEDIA_USAGE_ATTRIBUTE = AudioAttributes.USAGE_MEDIA;
    private static final int TYPE_STUB_SPEAKER = AudioDeviceInfo.TYPE_BUS;
    private WearAudioPlaybackCallback mPlaybackCallbackListener;
    private ShadowPackageManager mShadowPackageManager;
    private ShadowApplication mShadowApplication;
    @Mock AudioPlaybackConfiguration mMockPlaybackConfiguration;
    @Mock AudioDeviceInfo mMockAudioDeviceInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Application application = ApplicationProvider.getApplicationContext();
        mShadowApplication = Shadows.shadowOf(application);
        mShadowPackageManager = Shadows.shadowOf(application.getPackageManager());
        mPlaybackCallbackListener = new WearAudioPlaybackCallback(application);
    }

    @Test
    public void onPlaybackConfigChanged_onMediaStartOnStubSpeaker_shouldStartOutputSwitcher() {
        List<AudioPlaybackConfiguration> audioPlaybackConfigurationList =
                List.of(
                        createPlaybackConfiguration(
                                MEDIA_USAGE_ATTRIBUTE,
                                TYPE_STUB_SPEAKER,
                                AudioPlaybackConfiguration.PLAYER_STATE_STARTED,
                                AudioAttributes.CONTENT_TYPE_MUSIC));
        mShadowPackageManager.setNameForUid(FAKE_MEDIA_APP_UID, FAKE_MEDIA_APP_PACKAGE_NAME);
        try {
            registerFakeActivityIntentResolver(mShadowPackageManager);
        } catch (PackageManager.NameNotFoundException e) {
            assertWithMessage("Cannot register Media Routing Activity").fail();
        }

        mPlaybackCallbackListener.onPlaybackConfigChanged(audioPlaybackConfigurationList);

        Intent intentTriggered = mShadowApplication.getNextStartedActivity();
        assertThat(intentTriggered).isNotNull();
        assertThat(intentTriggered.getAction()).isEqualTo(ACTION_OUTPUT_SWITCHER);
        assertThat(intentTriggered.getStringExtra(EXTRA_MEDIA_APP_PACKAGE_NAME))
                .isEqualTo(FAKE_MEDIA_APP_PACKAGE_NAME);
        assertThat(intentTriggered.getComponent().getClassName())
                .isEqualTo(FAKE_MEDIA_ROUTING_ACTIVITY);
    }

    @Test
    public void onPlaybackConfigChanged_whenMediaNotPlaying_shouldNotStartOutputSwitcher() {
        List<AudioPlaybackConfiguration> audioPlaybackConfigurationList =
                List.of(
                        createPlaybackConfiguration(
                                MEDIA_USAGE_ATTRIBUTE,
                                TYPE_STUB_SPEAKER,
                                AudioPlaybackConfiguration.PLAYER_STATE_PAUSED,
                                AudioAttributes.CONTENT_TYPE_SPEECH));

        mPlaybackCallbackListener.onPlaybackConfigChanged(audioPlaybackConfigurationList);

        assertThat(mShadowApplication.getNextStartedActivity()).isNull();
    }

    @Test
    public void onPlaybackConfigChanged_onMediaStartOnValidOutput_shouldNotStartOutputSwitcher() {
        List<AudioPlaybackConfiguration> audioPlaybackConfigurationList =
                List.of(
                        createPlaybackConfiguration(
                                MEDIA_USAGE_ATTRIBUTE,
                                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                                AudioPlaybackConfiguration.PLAYER_STATE_STARTED,
                                AudioAttributes.CONTENT_TYPE_SPEECH));

        mPlaybackCallbackListener.onPlaybackConfigChanged(audioPlaybackConfigurationList);

        assertThat(mShadowApplication.getNextStartedActivity()).isNull();
    }

    @Test
    public void onPlaybackConfigChanged_whenNotMediaUsage_shouldNotStartOutputSwitcher() {
        List<AudioPlaybackConfiguration> audioPlaybackConfigurationList =
                List.of(
                        createPlaybackConfiguration(
                                AudioAttributes.USAGE_ASSISTANCE_SONIFICATION,
                                TYPE_STUB_SPEAKER,
                                AudioPlaybackConfiguration.PLAYER_STATE_STARTED,
                                AudioAttributes.CONTENT_TYPE_UNKNOWN));

        mPlaybackCallbackListener.onPlaybackConfigChanged(audioPlaybackConfigurationList);

        assertThat(mShadowApplication.getNextStartedActivity()).isNull();
    }

    @Test
    public void
            onPlaybackConfigChanged_onMediaWithInvalidContentType_shouldNotStartOutputSwitcher() {
        AudioPlaybackConfiguration testConfig =
                createPlaybackConfiguration(
                        AudioAttributes.USAGE_MEDIA,
                        TYPE_STUB_SPEAKER,
                        AudioPlaybackConfiguration.PLAYER_STATE_STARTED,
                        WearAudioPlaybackCallback.EXCLUDED_CONTENT_TYPES.get(0));
        List<AudioPlaybackConfiguration> audioPlaybackConfigurationList = new ArrayList<>();
        audioPlaybackConfigurationList.add(testConfig);

        mPlaybackCallbackListener.onPlaybackConfigChanged(audioPlaybackConfigurationList);

        assertThat(mShadowApplication.getNextStartedActivity()).isNull();
    }

    private AudioPlaybackConfiguration createPlaybackConfiguration(
            int audioUsage, int audioDeviceType, int playerState, int contentType) {
        AudioAttributes fakeAttributes =
                new AudioAttributes.Builder()
                        .setUsage(audioUsage)
                        .setContentType(contentType)
                        .build();
        when(mMockPlaybackConfiguration.getAudioAttributes()).thenReturn(fakeAttributes);
        when(mMockAudioDeviceInfo.getType()).thenReturn(audioDeviceType);
        when(mMockPlaybackConfiguration.getAudioDeviceInfo()).thenReturn(mMockAudioDeviceInfo);
        when(mMockPlaybackConfiguration.getPlayerState()).thenReturn(playerState);
        when(mMockPlaybackConfiguration.getClientUid()).thenReturn(FAKE_MEDIA_APP_UID);
        return mMockPlaybackConfiguration;
    }

    private static void registerFakeActivityIntentResolver(
            ShadowPackageManager shadowPackageManager) throws PackageManager.NameNotFoundException {
        ComponentName mediaRoutingComponentName =
                new ComponentName(PACKAGE_NAME_MEDIA_ROUTING, FAKE_MEDIA_ROUTING_ACTIVITY);

        ApplicationInfo systemAppInfo = new ApplicationInfo();
        systemAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;

        ActivityInfo mediaRoutingActivityInfo = new ActivityInfo();
        mediaRoutingActivityInfo.applicationInfo = systemAppInfo;
        mediaRoutingActivityInfo.name = mediaRoutingComponentName.getClassName();
        mediaRoutingActivityInfo.packageName = mediaRoutingComponentName.getPackageName();

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = mediaRoutingActivityInfo;

        Intent intent =
                new Intent(ACTION_OUTPUT_SWITCHER)
                        .putExtra(EXTRA_MEDIA_APP_PACKAGE_NAME, FAKE_MEDIA_APP_PACKAGE_NAME);
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }
}
