/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.resources.Density;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap_Delegate;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path_Delegate;
import android.graphics.Rect;
import android.view.animation.Transformation;
import android.view.shadow.HighQualityShadowPainter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Delegate used to provide new implementation of a select few methods of {@link ViewGroup}
 * <p/>
 * Through the layoutlib_create tool, the original  methods of ViewGroup have been replaced by calls
 * to methods of the same name in this delegate class.
 */
public class ViewGroup_Delegate {

    /**
     * Overrides the original drawChild call in ViewGroup to draw the shadow.
     */
    @LayoutlibDelegate
    /*package*/ static boolean drawChild(ViewGroup thisVG, Canvas canvas, View child,
            long drawingTime) {
        if (child.getZ() > thisVG.getZ()) {
            // The background's bounds are set lazily. Make sure they are set correctly so that
            // the outline obtained is correct.
            child.setBackgroundBounds();
            ViewOutlineProvider outlineProvider = child.getOutlineProvider();
            if (outlineProvider != null) {
                Outline outline = child.mAttachInfo.mTmpOutline;
                outlineProvider.getOutline(child, outline);
                if (outline.mPath != null || (outline.mRect != null && !outline.mRect.isEmpty())) {
                    int restoreTo = transformCanvas(thisVG, canvas, child);
                    drawShadow(thisVG, canvas, child, outline);
                    canvas.restoreToCount(restoreTo);
                }
            }
        }
        return thisVG.drawChild_Original(canvas, child, drawingTime);
    }

    private static void drawShadow(ViewGroup parent, Canvas canvas, View child,
            Outline outline) {
        boolean highQualityShadow = false;
        boolean enableShadow = true;
        float elevation = getElevation(child, parent);
        Context bridgeContext = parent.getContext();
        if (bridgeContext instanceof BridgeContext) {
            highQualityShadow = ((BridgeContext) bridgeContext).isHighQualityShadows();
            enableShadow = ((BridgeContext) bridgeContext).isShadowsEnabled();
        }

        if (!enableShadow) {
            return;
        }

        if(outline.mMode == Outline.MODE_ROUND_RECT && outline.mRect != null) {
            if (highQualityShadow) {
                float densityDpi = bridgeContext.getResources().getDisplayMetrics().densityDpi;
                HighQualityShadowPainter.paintRectShadow(
                        parent, outline, elevation, canvas, child.getAlpha(), densityDpi);
            } else {
                RectShadowPainter.paintShadow(outline, elevation, canvas, child.getAlpha());
            }
            return;
        }

        BufferedImage shadow = null;
        if (outline.mPath != null) {
            shadow = getPathShadow(outline, canvas, elevation, child.getAlpha());
        }
        if (shadow == null) {
            return;
        }
        Bitmap bitmap = Bitmap_Delegate.createBitmap(shadow, false,
                Density.getEnum(canvas.getDensity()));
        canvas.save();
        Rect clipBounds = canvas.getClipBounds();
        Rect newBounds = new Rect(clipBounds);
        newBounds.inset((int)-elevation, (int)-elevation);
        canvas.clipRectUnion(newBounds);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.restore();
    }

    private static float getElevation(View child, ViewGroup parent) {
        return child.getZ() - parent.getZ();
    }

    private static BufferedImage getPathShadow(Outline outline, Canvas canvas, float elevation,
            float alpha) {
        Rect clipBounds = canvas.getClipBounds();
        if (clipBounds.isEmpty()) {
          return null;
        }
        BufferedImage image = new BufferedImage(clipBounds.width(), clipBounds.height(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.draw(Path_Delegate.getDelegate(outline.mPath.mNativePath).getJavaShape());
        graphics.dispose();
        return ShadowPainter.createDropShadow(image, (int) elevation, alpha);
    }

    // Copied from android.view.View#draw(Canvas, ViewGroup, long) and removed code paths
    // which were never taken. Ideally, we should hook up the shadow code in the same method so
    // that we don't have to transform the canvas twice.
    private static int transformCanvas(ViewGroup thisVG, Canvas canvas, View child) {
        final int restoreTo = canvas.save();
        final boolean childHasIdentityMatrix = child.hasIdentityMatrix();
        int flags = thisVG.mGroupFlags;
        Transformation transformToApply = null;
        boolean concatMatrix = false;
        if ((flags & ViewGroup.FLAG_SUPPORT_STATIC_TRANSFORMATIONS) != 0) {
            final Transformation t = thisVG.getChildTransformation();
            final boolean hasTransform = thisVG.getChildStaticTransformation(child, t);
            if (hasTransform) {
                final int transformType = t.getTransformationType();
                transformToApply = transformType != Transformation.TYPE_IDENTITY ? t : null;
                concatMatrix = (transformType & Transformation.TYPE_MATRIX) != 0;
            }
        }
        concatMatrix |= childHasIdentityMatrix;

        canvas.translate(child.mLeft, child.mTop);
        float alpha = child.getAlpha() * child.getTransitionAlpha();

        if (transformToApply != null || alpha < 1 || !childHasIdentityMatrix) {
            if (transformToApply != null || !childHasIdentityMatrix) {

                if (transformToApply != null) {
                    if (concatMatrix) {
                        canvas.concat(transformToApply.getMatrix());
                    }
                }
                if (!childHasIdentityMatrix) {
                    canvas.concat(child.getMatrix());
                }

            }
        }
        return restoreTo;
    }
}
