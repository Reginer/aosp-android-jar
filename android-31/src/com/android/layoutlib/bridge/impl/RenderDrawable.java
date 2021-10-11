/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.layoutlib.bridge.impl;

import com.android.ide.common.rendering.api.DrawableParams;
import com.android.ide.common.rendering.api.HardwareConfig;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.Result.Status;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.RenderParamsFlags;
import com.android.resources.ResourceType;

import android.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.Bitmap_Delegate;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.AttachInfo_Accessor;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Action to render a given {@link Drawable} provided through {@link DrawableParams#getDrawable()}.
 *
 * The class only provides a simple {@link #render()} method, but the full life-cycle of the
 * action must be respected.
 *
 * @see RenderAction
 */
public class RenderDrawable extends RenderAction<DrawableParams> {

    public RenderDrawable(@NonNull DrawableParams params) {
        super(new DrawableParams(params));
    }

    @NonNull
    public Result render() {
        checkLock();
        // Get the drawable resource value.
        DrawableParams params = getParams();
        HardwareConfig hardwareConfig = params.getHardwareConfig();
        ResourceValue drawableResource = params.getDrawable();

        // Resolve it.
        BridgeContext context = getContext();
        drawableResource = context.getRenderResources().resolveResValue(drawableResource);

        if (drawableResource == null) {
            return Status.ERROR_NOT_A_DRAWABLE.createResult();
        }

        ResourceType resourceType = drawableResource.getResourceType();
        if (resourceType != ResourceType.DRAWABLE && resourceType != ResourceType.MIPMAP) {
            return Status.ERROR_NOT_A_DRAWABLE.createResult();
        }

        Drawable d = ResourceHelper.getDrawable(drawableResource, context);
        if (d == null) {
            return Status.ERROR_NOT_A_DRAWABLE.createResult();
        }

        Boolean allStates = params.getFlag(RenderParamsFlags.FLAG_KEY_RENDER_ALL_DRAWABLE_STATES);
        if (allStates == Boolean.TRUE) {
            List<BufferedImage> result;

            if (d instanceof StateListDrawable) {
                result = new ArrayList<BufferedImage>();
                StateListDrawable stateList = (StateListDrawable) d;
                for (int i = 0; i < stateList.getStateCount(); i++) {
                    Drawable stateDrawable = stateList.getStateDrawable(i);
                    result.add(renderImage(hardwareConfig, stateDrawable, context));
                }
            } else {
                result = Collections.singletonList(renderImage(hardwareConfig, d, context));
            }

            return Status.SUCCESS.createResult(result);
        } else {
            BufferedImage image = renderImage(hardwareConfig, d, context);
            return Status.SUCCESS.createResult(image);
        }
    }

    @NonNull
    private BufferedImage renderImage(@NonNull HardwareConfig hardwareConfig, @NonNull Drawable d,
            @NonNull BridgeContext context) {
        // Create a simple FrameLayout.
        FrameLayout content = new FrameLayout(context);

        // Get the actual Drawable object to draw.
        content.setBackground(d);

        // Set the AttachInfo on the root view.
        AttachInfo_Accessor.setAttachInfo(content);

        // Measure.
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();

        int screenWidth = hardwareConfig.getScreenWidth();
        int screenHeight = hardwareConfig.getScreenHeight();

        if (w == -1 || h == -1) {
            // Use screen size when either intrinsic width or height isn't available.
            w = screenWidth;
            h = screenHeight;
        } else if (w > screenWidth || h > screenHeight) {
            // If image wouldn't fit to the screen, resize it to avoid cropping.

            // We need to find scale such that scale * w <= screenWidth, scale * h <= screenHeight.
            double scale = Math.min((double) screenWidth / w, (double) screenHeight / h);

            // scale * w / scale * h = w / h, so, proportions are preserved.
            w = (int) Math.floor(scale * w);
            h = (int) Math.floor(scale * h);
        }

        int w_spec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        int h_spec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        content.measure(w_spec, h_spec);

        // Now do the layout.
        content.layout(0, 0, w, h);

        // Pre-draw setup.
        AttachInfo_Accessor.dispatchOnPreDraw(content);

        // Draw into a new image.
        BufferedImage image = getImage(w, h);

        // Create an Android bitmap around the BufferedImage.
        Bitmap bitmap = Bitmap_Delegate.createBitmap(image,
                true /*isMutable*/, hardwareConfig.getDensity());

        // Create a Canvas around the Android bitmap.
        Canvas canvas = new Canvas(bitmap);
        canvas.setDensity(hardwareConfig.getDensity().getDpiValue());

        // Draw.
        content.draw(canvas);

        // Detach root from window after draw.
        AttachInfo_Accessor.detachFromWindow(content);

        return image;
    }

    @NonNull
    protected BufferedImage getImage(int w, int h) {
        BufferedImage image = new BufferedImage(w > 0 ? w : 1,
                h > 0 ? h : 1,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = image.createGraphics();
        gc.setComposite(AlphaComposite.Src);

        gc.setColor(new Color(0x00000000, true));
        gc.fillRect(0, 0, w, h);

        // done
        gc.dispose();

        return image;
    }
}
