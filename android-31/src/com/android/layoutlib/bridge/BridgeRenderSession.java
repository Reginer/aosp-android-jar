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

package com.android.layoutlib.bridge;

import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.layoutlib.bridge.impl.RenderSessionImpl;
import com.android.tools.layoutlib.java.System_Delegate;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link RenderSession}.
 *
 * This is a pretty basic class that does almost nothing. All of the work is done in
 * {@link RenderSessionImpl}.
 *
 */
public class BridgeRenderSession extends RenderSession {

    @Nullable
    private final RenderSessionImpl mSession;
    @NonNull
    private Result mLastResult;

    @Override
    public Result getResult() {
        return mLastResult;
    }

    @Override
    public BufferedImage getImage() {
        return mSession != null ? mSession.getImage() :
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public List<ViewInfo> getRootViews() {
        return mSession != null ? mSession.getViewInfos() : Collections.emptyList();
    }

    @Override
    public List<ViewInfo> getSystemRootViews() {
        return mSession != null ? mSession.getSystemViewInfos() : Collections.emptyList();
    }

    @Override
    public Map<Object, Map<ResourceReference, ResourceValue>> getDefaultNamespacedProperties() {
        return mSession != null ? mSession.getDefaultNamespacedProperties() :
                Collections.emptyMap();
    }

    @Override
    public Map<Object, ResourceReference> getDefaultNamespacedStyles() {
        return mSession != null ? mSession.getDefaultNamespacedStyles() : Collections.emptyMap();
    }

    @Override
    public Result measure(long timeout) {
        if (mSession != null) {
            try {
                Bridge.prepareThread();
                mLastResult = mSession.acquire(timeout);
                if (mLastResult.isSuccess()) {
                    mSession.invalidateRenderingSize();
                    mLastResult = mSession.measure();
                }
            } finally {
                mSession.release();
                Bridge.cleanupThread();
            }
        }

        return mLastResult;
    }

    @Override
    public Result render(long timeout, boolean forceMeasure) {
        if (mSession != null) {
            try {
                Bridge.prepareThread();
                mLastResult = mSession.acquire(timeout);
                if (mLastResult.isSuccess()) {
                    if (forceMeasure) {
                        mSession.invalidateRenderingSize();
                    }
                    mLastResult = mSession.render(false /*freshRender*/);
                }
            } finally {
                mSession.release();
                Bridge.cleanupThread();
            }
        }

        return mLastResult;
    }

    @Override
    public void setSystemTimeNanos(long nanos) {
        System_Delegate.setNanosTime(nanos);
    }

    @Override
    public void setSystemBootTimeNanos(long nanos) {
        System_Delegate.setBootTimeNanos(nanos);
    }

    @Override
    public void setElapsedFrameTimeNanos(long nanos) {
        if (mSession != null) {
            mSession.setElapsedFrameTimeNanos(nanos);
        }
    }

    @Override
    public void dispose() {
        if (mSession != null) {
            mSession.dispose();
        }
    }

    /*package*/ BridgeRenderSession(@Nullable RenderSessionImpl scene, @NonNull Result lastResult) {
        mSession = scene;
        if (scene != null) {
            mSession.setScene(this);
        }
        mLastResult = lastResult;
    }

    @Override
    public Object getValidationData() {
        if (mSession != null) {
            return mSession.getValidatorResult();
        }
        return null;
    }
}
