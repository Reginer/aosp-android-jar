/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.graphics.text;

import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.graphics.Rect;
import android.graphics.text.LineBreaker_Delegate.Builder;
import android.graphics.text.LineBreaker_Delegate.Run;

import libcore.util.NativeAllocationRegistry_Delegate;

/**
 * Delegate that provides implementation for native methods in
 * {@link android.graphics.text.MeasuredText}
 * <p/>
 * Through the layoutlib_create tool, selected methods of StaticLayout have been replaced
 * by calls to methods of the same name in this delegate class.
 *
 */
public class MeasuredText_Delegate {

    // ---- Builder delegate manager ----
    protected static final DelegateManager<MeasuredText_Delegate> sManager =
            new DelegateManager<>(MeasuredText_Delegate.class);
    private static long sFinalizer = -1;

    protected long mNativeBuilderPtr;

    @LayoutlibDelegate
    /*package*/ static float nGetWidth(long nativePtr, int start, int end) {
        // Ignore as it is not used for the layoutlib implementation
        return 0.0f;
    }

    @LayoutlibDelegate
    /*package*/ static long nGetReleaseFunc() {
        synchronized (MeasuredText_Delegate.class) {
            if (sFinalizer == -1) {
                sFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(
                        sManager::removeJavaReferenceFor);
            }
        }
        return sFinalizer;
    }

    @LayoutlibDelegate
    /*package*/ static int nGetMemoryUsage(long nativePtr) {
        // Ignore as it is not used for the layoutlib implementation
        return 0;
    }

    @LayoutlibDelegate
    /*package*/ static void nGetBounds(long nativePtr, char[] buf, int start, int end, Rect rect) {
        // Ignore as it is not used for the layoutlib implementation
    }

    @LayoutlibDelegate
    /*package*/ static float nGetCharWidthAt(long nativePtr, int offset) {
        // Ignore as it is not used for the layoutlib implementation
        return 0.0f;
    }

    public static void computeRuns(long measuredTextPtr, Builder staticLayoutBuilder) {
        MeasuredText_Delegate delegate = sManager.getDelegate(measuredTextPtr);
        if (delegate == null) {
            return;
        }
        MeasuredText_Builder_Delegate builder =
                MeasuredText_Builder_Delegate.sBuilderManager.getDelegate(delegate.mNativeBuilderPtr);
        if (builder == null) {
            return;
        }
        for (Run run: builder.mRuns) {
            run.addTo(staticLayoutBuilder);
        }
    }
}