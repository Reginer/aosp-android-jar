/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.view;

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.content.Context;
import android.graphics.Canvas;
import android.os.IBinder;

/**
 * Delegate used to provide new implementation of a select few methods of {@link View}
 *
 * Through the layoutlib_create tool, the original  methods of View have been replaced
 * by calls to methods of the same name in this delegate class.
 *
 */
public class View_Delegate {

    @LayoutlibDelegate
    /*package*/ static boolean isInEditMode(View thisView) {
        return true;
    }

    @LayoutlibDelegate
    /*package*/ static IBinder getWindowToken(View thisView) {
        Context baseContext = BridgeContext.getBaseContext(thisView.getContext());
        if (baseContext instanceof BridgeContext) {
            return ((BridgeContext) baseContext).getBinder();
        }
        return null;
    }

    @LayoutlibDelegate
    /*package*/ static void draw(View thisView, Canvas canvas) {
        try {
            // This code is run within a catch to prevent misbehaving components from breaking
            // all the layout.
            thisView.draw_Original(canvas);
        } catch (Throwable t) {
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN, "View draw failed", t, null, null);
        }
    }

    @LayoutlibDelegate
    /*package*/ static boolean draw(
            View thisView, Canvas canvas, ViewGroup parent, long drawingTime) {
        try {
            // This code is run within a catch to prevent misbehaving components from breaking
            // all the layout.
            return thisView.draw_Original(canvas, parent, drawingTime);
        } catch (Throwable t) {
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN, "View draw failed", t, null, null);
        }
        return false;
    }

    @LayoutlibDelegate
    /*package*/ static void measure(View thisView, int widthMeasureSpec, int heightMeasureSpec) {
        try {
            // This code is run within a catch to prevent misbehaving components from breaking
            // all the layout.
            thisView.measure_Original(widthMeasureSpec, heightMeasureSpec);
        } catch (Throwable t) {
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN, "View measure failed", t, null, null);
        }
    }

    @LayoutlibDelegate
    /*package*/ static void layout(View thisView, int l, int t, int r, int b) {
        try {
            // This code is run within a catch to prevent misbehaving components from breaking
            // all the layout.
            thisView.layout_Original(l, t, r, b);
        } catch (Throwable th) {
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN, "View layout failed", th, null, null);
        }
    }

    @LayoutlibDelegate
    /*package*/ static void dispatchDetachedFromWindow(View thisView) {
        try {
            // This code is run within a try/catch to prevent components from throwing user-visible
            // exceptions when being disposed.
            thisView.dispatchDetachedFromWindow_Original();
        } catch (Throwable t) {
            Context context = BridgeContext.getBaseContext(thisView.getContext());
            if (context instanceof BridgeContext) {
                ((BridgeContext) context).warn("Exception while detaching " + thisView.getClass(), t);
            }
        }
    }
}
