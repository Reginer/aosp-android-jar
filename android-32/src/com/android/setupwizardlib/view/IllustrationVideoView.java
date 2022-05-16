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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Animatable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import com.android.setupwizardlib.R;
import java.io.IOException;

/**
 * A view for displaying videos in a continuous loop (without audio). This is typically used for
 * animated illustrations.
 *
 * <p>The video can be specified using {@code app:suwVideo}, specifying the raw resource to the mp4
 * video. Optionally, {@code app:suwLoopStartMs} can be used to specify which part of the video it
 * should loop back to
 *
 * <p>For optimal file size, use avconv or other video compression tool to remove the unused audio
 * track and reduce the size of your video asset: avconv -i [input file] -vcodec h264 -crf 20 -an
 * [output_file]
 */
@TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH)
public class IllustrationVideoView extends TextureView
    implements Animatable,
        SurfaceTextureListener,
        OnPreparedListener,
        OnSeekCompleteListener,
        OnInfoListener,
        OnErrorListener {

  private static final String TAG = "IllustrationVideoView";

  protected float mAspectRatio = 1.0f; // initial guess until we know

  @Nullable // Can be null when media player fails to initialize
  protected MediaPlayer mMediaPlayer;

  private @RawRes int videoResId = 0;

  private String videoResPackageName;

  @VisibleForTesting Surface surface;

  private boolean prepared;

  public IllustrationVideoView(Context context, AttributeSet attrs) {
    super(context, attrs);
    final TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.SuwIllustrationVideoView);
    final int videoResId = a.getResourceId(R.styleable.SuwIllustrationVideoView_suwVideo, 0);
    a.recycle();
    setVideoResource(videoResId);

    // By default the video scales without interpolation, resulting in jagged edges in the
    // video. This works around it by making the view go through scaling, which will apply
    // anti-aliasing effects.
    setScaleX(0.9999999f);
    setScaleX(0.9999999f);

    setSurfaceTextureListener(this);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (height < width * mAspectRatio) {
      // Height constraint is tighter. Need to scale down the width to fit aspect ratio.
      width = (int) (height / mAspectRatio);
    } else {
      // Width constraint is tighter. Need to scale down the height to fit aspect ratio.
      height = (int) (width * mAspectRatio);
    }

    super.onMeasure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
  }

  /**
   * Set the video and video package name to be played by this view.
   *
   * @param videoResId Resource ID of the video, typically an MP4 under res/raw.
   * @param videoResPackageName The package name of videoResId.
   */
  public void setVideoResource(@RawRes int videoResId, String videoResPackageName) {
    if (videoResId != this.videoResId
        || (videoResPackageName != null && !videoResPackageName.equals(this.videoResPackageName))) {
      this.videoResId = videoResId;
      this.videoResPackageName = videoResPackageName;
      createMediaPlayer();
    }
  }

  /**
   * Set the video to be played by this view.
   *
   * @param resId Resource ID of the video, typically an MP4 under res/raw.
   */
  public void setVideoResource(@RawRes int resId) {
    setVideoResource(resId, getContext().getPackageName());
  }

  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    super.onWindowFocusChanged(hasWindowFocus);
    if (hasWindowFocus) {
      start();
    } else {
      stop();
    }
  }

  /**
   * Creates a media player for the current URI. The media player will be started immediately if the
   * view's window is visible. If there is an existing media player, it will be released.
   */
  protected void createMediaPlayer() {
    if (mMediaPlayer != null) {
      mMediaPlayer.release();
    }
    if (surface == null || videoResId == 0) {
      return;
    }

    mMediaPlayer = new MediaPlayer();

    mMediaPlayer.setSurface(surface);
    mMediaPlayer.setOnPreparedListener(this);
    mMediaPlayer.setOnSeekCompleteListener(this);
    mMediaPlayer.setOnInfoListener(this);
    mMediaPlayer.setOnErrorListener(this);

    setVideoResourceInternal(videoResId, videoResPackageName);
  }

  private void setVideoResourceInternal(@RawRes int videoRes, String videoResPackageName) {
    Uri uri = Uri.parse("android.resource://" + videoResPackageName + "/" + videoRes);
    try {
      mMediaPlayer.setDataSource(getContext(), uri, null);
      mMediaPlayer.prepareAsync();
    } catch (IOException e) {
      Log.wtf(TAG, "Unable to set data source", e);
    }
  }

  protected void createSurface() {
    if (surface != null) {
      surface.release();
      surface = null;
    }
    // Reattach only if it has been previously released
    SurfaceTexture surfaceTexture = getSurfaceTexture();
    if (surfaceTexture != null) {
      setVisibility(View.INVISIBLE);
      surface = new Surface(surfaceTexture);
    }
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == View.VISIBLE) {
      reattach();
    } else {
      release();
    }
  }

  /**
   * Whether the media player should play the video in a continuous loop. The default value is true.
   */
  protected boolean shouldLoop() {
    return true;
  }

  /**
   * Release any resources used by this view. This is automatically called in
   * onSurfaceTextureDestroyed so in most cases you don't have to call this.
   */
  public void release() {
    if (mMediaPlayer != null) {
      mMediaPlayer.release();
      mMediaPlayer = null;
      prepared = false;
    }
    if (surface != null) {
      surface.release();
      surface = null;
    }
  }

  private void reattach() {
    if (surface == null) {
      initVideo();
    }
  }

  private void initVideo() {
    if (getWindowVisibility() != View.VISIBLE) {
      return;
    }
    createSurface();
    if (surface != null) {
      createMediaPlayer();
    } else {
      Log.w(TAG, "Surface creation failed");
    }
  }

  protected void onRenderingStart() {}

  /* SurfaceTextureListener methods */

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
    // Keep the view hidden until video starts
    setVisibility(View.INVISIBLE);
    initVideo();
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {}

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
    release();
    return true;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

  /* Animatable methods */

  @Override
  public void start() {
    if (prepared && mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
      mMediaPlayer.start();
    }
  }

  @Override
  public void stop() {
    if (prepared && mMediaPlayer != null) {
      mMediaPlayer.pause();
    }
  }

  @Override
  public boolean isRunning() {
    return mMediaPlayer != null && mMediaPlayer.isPlaying();
  }

  /* MediaPlayer callbacks */

  @Override
  public boolean onInfo(MediaPlayer mp, int what, int extra) {
    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
      // Video available, show view now
      setVisibility(View.VISIBLE);
      onRenderingStart();
    }
    return false;
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    prepared = true;
    mp.setLooping(shouldLoop());

    float aspectRatio = 0.0f;
    if (mp.getVideoWidth() > 0 && mp.getVideoHeight() > 0) {
      aspectRatio = (float) mp.getVideoHeight() / mp.getVideoWidth();
    } else {
      Log.w(TAG, "Unexpected video size=" + mp.getVideoWidth() + "x" + mp.getVideoHeight());
    }
    if (Float.compare(mAspectRatio, aspectRatio) != 0) {
      mAspectRatio = aspectRatio;
      requestLayout();
    }
    if (getWindowVisibility() == View.VISIBLE) {
      start();
    }
  }

  @Override
  public void onSeekComplete(MediaPlayer mp) {
    if (isPrepared()) {
      mp.start();
    } else {
      Log.wtf(TAG, "Seek complete but media player not prepared");
    }
  }

  public int getCurrentPosition() {
    return mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
  }

  protected boolean isPrepared() {
    return prepared;
  }

  @Override
  public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
    Log.w(TAG, "MediaPlayer error. what=" + what + " extra=" + extra);
    return false;
  }
}
