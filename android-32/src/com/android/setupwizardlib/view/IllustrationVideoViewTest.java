/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.setupwizardlib.view;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import androidx.annotation.RawRes;
import android.view.View;
import com.android.setupwizardlib.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.ShadowMediaPlayer.InvalidStateBehavior;
import org.robolectric.shadows.ShadowMediaPlayer.MediaInfo;
import org.robolectric.shadows.util.DataSource;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.NEWEST_SDK)
public class IllustrationVideoViewTest {

  @Mock private SurfaceTexture surfaceTexture;

  private IllustrationVideoView view;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    ShadowMediaPlayer.addMediaInfo(
        DataSource.toDataSource(
            "android.resource://" + application.getPackageName() + "/" + android.R.color.white),
        new ShadowMediaPlayer.MediaInfo(100, 10));
    ShadowMediaPlayer.addMediaInfo(
        DataSource.toDataSource(
            "android.resource://" + application.getPackageName() + "/" + android.R.color.black),
        new ShadowMediaPlayer.MediaInfo(100, 10));
  }

  @Test
  public void testPausedWhenWindowFocusLost() {
    createDefaultView();
    Robolectric.flushForegroundThreadScheduler();
    view.start();

    assertThat(view.mMediaPlayer).isNotNull();
    assertThat(view.surface).isNotNull();

    view.onWindowFocusChanged(false);
    assertThat(getShadowMediaPlayer().getState()).isEqualTo(ShadowMediaPlayer.State.PAUSED);
  }

  @Test
  public void testStartedWhenWindowFocusRegained() {
    testPausedWhenWindowFocusLost();
    Robolectric.flushForegroundThreadScheduler();

    view.onWindowFocusChanged(true);
    assertThat(getShadowMediaPlayer().getState()).isEqualTo(ShadowMediaPlayer.State.STARTED);
  }

  @Test
  public void testSurfaceReleasedWhenTextureDestroyed() {
    createDefaultView();
    view.start();

    assertThat(view.mMediaPlayer).isNotNull();
    assertThat(view.surface).isNotNull();

    // MediaPlayer is set to null after destroy. Retrieve it first before we call destroy.
    ShadowMediaPlayer shadowMediaPlayer = getShadowMediaPlayer();
    view.onSurfaceTextureDestroyed(surfaceTexture);
    assertThat(shadowMediaPlayer.getState()).isEqualTo(ShadowMediaPlayer.State.END);
  }

  @Test
  public void testXmlSetVideoResId() {
    createDefaultView();
    assertThat(getShadowMediaPlayer().getSourceUri().toString())
        .isEqualTo("android.resource://com.android.setupwizardlib/" + android.R.color.white);
  }

  @Test
  public void testSetVideoResId() {
    createDefaultView();

    @RawRes int black = android.R.color.black;
    view.setVideoResource(black);

    assertThat(getShadowMediaPlayer().getSourceUri().toString())
        .isEqualTo("android.resource://com.android.setupwizardlib/" + android.R.color.black);
  }

  @Test
  public void prepareVideo_shouldSetAspectRatio() {
    createDefaultView();

    ReflectionHelpers.setField(getShadowMediaPlayer(), "videoWidth", 720);
    ReflectionHelpers.setField(getShadowMediaPlayer(), "videoHeight", 1280);

    Robolectric.flushForegroundThreadScheduler();
    view.start();

    view.measure(
        View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY));

    final float aspectRatio = (float) view.getMeasuredHeight() / view.getMeasuredWidth();
    assertThat(aspectRatio).isWithin(0.001f).of(1280f / 720f);
  }

  @Test
  public void prepareVideo_zeroHeight_shouldSetAspectRatioToZero() {
    createDefaultView();

    ReflectionHelpers.setField(getShadowMediaPlayer(), "videoWidth", 720);
    ReflectionHelpers.setField(getShadowMediaPlayer(), "videoHeight", 0);

    Robolectric.flushForegroundThreadScheduler();
    view.start();

    final float aspectRatio = (float) view.getHeight() / view.getWidth();
    assertThat(aspectRatio).isEqualTo(0.0f);
  }

  @Test
  public void setVideoResId_resetDiffVideoResFromDiffPackage_videoResShouldBeSet() {
    // VideoRes default set as android.R.color.white with
    // default package(com.android.setupwizardlib)
    createDefaultView();

    // reset different videoRes from different package
    String newPackageName = "com.android.fakepackage";
    @RawRes int black = android.R.color.black;
    addMediaInfo(black, newPackageName);
    view.setVideoResource(black, newPackageName);

    // should be reset to black with the new package
    assertThat(getShadowMediaPlayer().getSourceUri().toString())
        .isEqualTo("android.resource://" + newPackageName + "/" + android.R.color.black);
  }

  @Test
  public void setVideoResId_resetDiffVideoResFromSamePackage_videoResShouldBeSet() {
    // VideoRes default set as android.R.color.white with
    // default package(com.android.setupwizardlib)
    createDefaultView();

    // reset different videoRes from the same package(default package)
    String defaultPackageName = "com.android.setupwizardlib";
    @RawRes int black = android.R.color.black;
    addMediaInfo(black, defaultPackageName);
    view.setVideoResource(black, defaultPackageName);

    // should be reset to black with the same package(default package)
    assertThat(getShadowMediaPlayer().getSourceUri().toString())
        .isEqualTo("android.resource://" + defaultPackageName + "/" + android.R.color.black);
  }

  @Test
  public void setVideoResId_resetSameVideoResFromDifferentPackage_videoResShouldBeSet() {
    // VideoRes default set as android.R.color.white with
    // default package(com.android.setupwizardlib)
    createDefaultView();

    // reset same videoRes from different package
    @RawRes int white = android.R.color.white;
    String newPackageName = "com.android.fakepackage";
    addMediaInfo(white, newPackageName);
    view.setVideoResource(white, newPackageName);

    // should be white with the new package
    assertThat(getShadowMediaPlayer().getSourceUri().toString())
        .isEqualTo("android.resource://" + newPackageName + "/" + android.R.color.white);
  }

  private ShadowMediaPlayer getShadowMediaPlayer() {
    return Shadows.shadowOf(view.mMediaPlayer);
  }

  private void createDefaultView() {
    view =
        new IllustrationVideoView(
            application,
            Robolectric.buildAttributeSet()
                // Any resource attribute should work, since the data source is fake
                .addAttribute(R.attr.suwVideo, "@android:color/white")
                .build());

    Activity activity = Robolectric.setupActivity(Activity.class);
    activity.setContentView(view);
    setWindowVisible();

    view.setSurfaceTexture(mock(SurfaceTexture.class));
    view.onSurfaceTextureAvailable(surfaceTexture, 500, 500);
    getShadowMediaPlayer().setInvalidStateBehavior(InvalidStateBehavior.EMULATE);
  }

  private void setWindowVisible() {
    Object viewRootImpl = ReflectionHelpers.callInstanceMethod(view, "getViewRootImpl");
    ReflectionHelpers.callInstanceMethod(
        viewRootImpl, "handleAppVisibility", ClassParameter.from(boolean.class, true));
    assertThat(view.isAttachedToWindow()).isTrue();
    assertThat(view.getWindowVisibility()).isEqualTo(View.VISIBLE);
  }

  private void addMediaInfo(@RawRes int res, String packageName) {
    ShadowMediaPlayer.addMediaInfo(
        DataSource.toDataSource(
            application, Uri.parse("android.resource://" + packageName + "/" + res), null),
        new MediaInfo(5000, 1));
  }
}
