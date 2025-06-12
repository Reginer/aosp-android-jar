/*
 * Copyright (C) 2008-2012 The Android Open Source Project
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

package android.renderscript;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @hide
 * @deprecated in API 16
 * The Surface View for a graphics renderscript (RenderScriptGL) to draw on.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about creating an application that uses RenderScript, read the
 * <a href="{@docRoot}guide/topics/renderscript/index.html">RenderScript</a> developer guide.</p>
 * </div>
 */
@Deprecated
public class RSSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mSurfaceHolder;
    private RenderScriptGL mRS;

    /**
     * @deprecated in API 16
     * Standard View constructor. In order to render something, you
     * must call {@link android.opengl.GLSurfaceView#setRenderer} to
     * register a renderer.
     */
    @UnsupportedAppUsage
    public RSSurfaceView(Context context) {
        super(context);
        init();
        //Log.v(RenderScript.LOG_TAG, "RSSurfaceView");
    }

    /**
     * @deprecated in API 16
     * Standard View constructor. In order to render something, you
     * must call {@link android.opengl.GLSurfaceView#setRenderer} to
     * register a renderer.
     */
    @UnsupportedAppUsage
    public RSSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        //Log.v(RenderScript.LOG_TAG, "RSSurfaceView");
    }

    private void init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    /**
     * @deprecated in API 16
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of RSSurfaceView.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    /**
     * @deprecated in API 16
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of RSSurfaceView.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (this) {
            // Surface will be destroyed when we return
            if (mRS != null) {
                mRS.setSurface(null, 0, 0);
            }
        }
    }

    /**
     * @deprecated in API 16
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of RSSurfaceView.
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        synchronized (this) {
            if (mRS != null) {
                mRS.setSurface(holder, w, h);
            }
        }
    }

   /**
     * @deprecated in API 16
     * Inform the view that the activity is paused. The owner of this view must
     * call this method when the activity is paused. Calling this method will
     * pause the rendering thread.
     * Must not be called before a renderer has been set.
     */
    public void pause() {
        if(mRS != null) {
            mRS.pause();
        }
    }

    /**
     * @deprecated in API 16
     * Inform the view that the activity is resumed. The owner of this view must
     * call this method when the activity is resumed. Calling this method will
     * recreate the OpenGL display and resume the rendering
     * thread.
     * Must not be called before a renderer has been set.
     */
    public void resume() {
        if(mRS != null) {
            mRS.resume();
        }
    }

    /**
     * @deprecated in API 16
     **/
    public RenderScriptGL createRenderScriptGL(RenderScriptGL.SurfaceConfig sc) {
      RenderScriptGL rs = new RenderScriptGL(this.getContext(), sc);
        setRenderScriptGL(rs);
        return rs;
    }

    /**
     * @deprecated in API 16
     **/
    public void destroyRenderScriptGL() {
        synchronized (this) {
            mRS.destroy();
            mRS = null;
        }
    }

    /**
     * @deprecated in API 16
     **/
    public void setRenderScriptGL(RenderScriptGL rs) {
        mRS = rs;
    }

    /**
     * @deprecated in API 16
     **/
    public RenderScriptGL getRenderScriptGL() {
        return mRS;
    }
}
