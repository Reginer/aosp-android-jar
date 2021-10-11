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

package android.view.shadow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.math.Math3DHelper;

import static android.view.shadow.ShadowConstants.MIN_ALPHA;
import static android.view.shadow.ShadowConstants.SCALE_DOWN;

public class HighQualityShadowPainter {
    private static final float sRoundedGap = (float) (1.0 - Math.sqrt(2.0) / 2.0);

    private HighQualityShadowPainter() { }

    /**
     * Draws simple Rect shadow
     */
    public static void paintRectShadow(ViewGroup parent, Outline outline, float elevation,
            Canvas canvas, float alpha, float densityDpi) {

        if (!validate(elevation, densityDpi)) {
            return;
        }

        int width = parent.getWidth() / SCALE_DOWN;
        int height = parent.getHeight() / SCALE_DOWN;

        Rect rectOriginal = new Rect();
        Rect rectScaled = new Rect();
        if (!outline.getRect(rectScaled) || alpha < MIN_ALPHA) {
            // If alpha below MIN_ALPHA it's invisible (based on manual test). Save some perf.
            return;
        }

        outline.getRect(rectOriginal);

        rectScaled.left /= SCALE_DOWN;
        rectScaled.right /= SCALE_DOWN;
        rectScaled.top /= SCALE_DOWN;
        rectScaled.bottom /= SCALE_DOWN;
        float radius = outline.getRadius() / SCALE_DOWN;

        if (radius > rectScaled.width() || radius > rectScaled.height()) {
            // Rounded edge generation fails if radius is bigger than drawing box.
            return;
        }

        // ensure alpha doesn't go over 1
        alpha = (alpha > 1.0f) ? 1.0f : alpha;
        boolean isOpaque = outline.getAlpha() * alpha == 1.0f;
        float[] poly = getPoly(rectScaled, elevation / SCALE_DOWN, radius);

        AmbientShadowConfig ambientConfig = new AmbientShadowConfig.Builder()
                .setPolygon(poly)
                .setLightSourcePosition(
                        (rectScaled.left + rectScaled.right) / 2.0f,
                        (rectScaled.top + rectScaled.bottom) / 2.0f)
                .setEdgeScale(ShadowConstants.AMBIENT_SHADOW_EDGE_SCALE)
                .setShadowBoundRatio(ShadowConstants.AMBIENT_SHADOW_SHADOW_BOUND)
                .setShadowStrength(ShadowConstants.AMBIENT_SHADOW_STRENGTH * alpha)
                .build();

        AmbientShadowTriangulator ambientTriangulator = new AmbientShadowTriangulator(ambientConfig);
        ambientTriangulator.triangulate();

        SpotShadowTriangulator spotTriangulator = null;
        float lightZHeightPx = ShadowConstants.SPOT_SHADOW_LIGHT_Z_HEIGHT_DP * (densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        if (lightZHeightPx - elevation / SCALE_DOWN >= ShadowConstants.SPOT_SHADOW_LIGHT_Z_EPSILON) {

            float lightX = (rectScaled.left + rectScaled.right) / 2;
            float lightY = rectScaled.top;
            // Light shouldn't be bigger than the object by too much.
            int dynamicLightRadius = Math.min(rectScaled.width(), rectScaled.height());

            SpotShadowConfig spotConfig = new SpotShadowConfig.Builder()
                    .setLightCoord(lightX, lightY, lightZHeightPx)
                    .setLightRadius(dynamicLightRadius)
                    .setShadowStrength(ShadowConstants.SPOT_SHADOW_STRENGTH * alpha)
                    .setPolygon(poly, poly.length / ShadowConstants.COORDINATE_SIZE)
                    .build();

            spotTriangulator = new SpotShadowTriangulator(spotConfig);
            spotTriangulator.triangulate();
        }

        int translateX = 0;
        int translateY = 0;
        int imgW = 0;
        int imgH = 0;

        if (ambientTriangulator.isValid()) {
            float[] shadowBounds = Math3DHelper.flatBound(ambientTriangulator.getVertices(), 2);
            // Move the shadow to the left top corner to occupy the least possible bitmap

            translateX = -(int) Math.floor(shadowBounds[0]);
            translateY = -(int) Math.floor(shadowBounds[1]);

            // create bitmap of the least possible size that covers the entire shadow
            imgW = (int) Math.ceil(shadowBounds[2] + translateX);
            imgH = (int) Math.ceil(shadowBounds[3] + translateY);
        }

        if (spotTriangulator != null && spotTriangulator.validate()) {

            // Bit of a hack to re-adjust spot shadow to fit correctly within parent canvas.
            // Problem is that outline passed is not a final position, which throws off our
            // whereas our shadow rendering algorithm, which requires pre-set range for
            // optimization purposes.
            float[] shadowBounds = Math3DHelper.flatBound(spotTriangulator.getStrips()[0], 3);

            if ((shadowBounds[2] - shadowBounds[0]) > width ||
                    (shadowBounds[3] - shadowBounds[1]) > height) {
                // Spot shadow to be casted is larger than the parent canvas,
                // We'll let ambient shadow do the trick and skip spot shadow here.
                spotTriangulator = null;
            }

            translateX = Math.max(-(int) Math.floor(shadowBounds[0]), translateX);
            translateY = Math.max(-(int) Math.floor(shadowBounds[1]), translateY);

            // create bitmap of the least possible size that covers the entire shadow
            imgW = Math.max((int) Math.ceil(shadowBounds[2] + translateX), imgW);
            imgH = Math.max((int) Math.ceil(shadowBounds[3] + translateY), imgH);
        }

        TriangleBuffer renderer = new TriangleBuffer();
        renderer.setSize(imgW, imgH, 0);

        if (ambientTriangulator.isValid()) {

            Math3DHelper.translate(ambientTriangulator.getVertices(), translateX, translateY, 2);
            renderer.drawTriangles(ambientTriangulator.getIndices(), ambientTriangulator.getVertices(),
                    ambientTriangulator.getColors(), ambientConfig.getShadowStrength());
        }

        if (spotTriangulator != null && spotTriangulator.validate()) {
            float[][] strips = spotTriangulator.getStrips();
            for (int i = 0; i < strips.length; ++i) {
                Math3DHelper.translate(strips[i], translateX, translateY, 3);
                renderer.drawTriangles(strips[i], ShadowConstants.SPOT_SHADOW_STRENGTH * alpha);
            }
        }

        Bitmap img = renderer.createImage();

        drawScaled(canvas, img, translateX, translateY, rectOriginal, radius, isOpaque);
    }

    /**
     * High quality shadow does not work well with object that is too high in elevation. Check if
     * the object elevation is reasonable and returns true if shadow will work well. False other
     * wise.
     */
    private static boolean validate(float elevation, float densityDpi) {
        float scaledElevationPx = elevation / SCALE_DOWN;
        float scaledSpotLightHeightPx = ShadowConstants.SPOT_SHADOW_LIGHT_Z_HEIGHT_DP *
                (densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        if (scaledElevationPx > scaledSpotLightHeightPx) {
            return false;
        }

        return true;
    }

    /**
     * Draw the bitmap scaled up.
     * @param translateX - offset in x axis by which the bitmap is shifted.
     * @param translateY - offset in y axis by which the bitmap is shifted.
     * @param shadowCaster - unscaled outline of shadow caster
     * @param radius
     */
    private static void drawScaled(Canvas canvas, Bitmap bitmap, int translateX, int translateY,
            Rect shadowCaster, float radius, boolean isOpaque) {
        int unscaledTranslateX = translateX * SCALE_DOWN;
        int unscaledTranslateY = translateY * SCALE_DOWN;

        // To the canvas
        Rect dest = new Rect(
                -unscaledTranslateX,
                -unscaledTranslateY,
                (bitmap.getWidth() * SCALE_DOWN) - unscaledTranslateX,
                (bitmap.getHeight() * SCALE_DOWN) - unscaledTranslateY);
        Rect destSrc = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        // We can skip drawing the shadows behind the caster if either
        // 1) radius is 0, the shadow caster is rectangle and we can have a perfect cut
        // 2) shadow caster is opaque and even if remove shadow only partially it won't affect
        // the visual quality, otherwise we will observe shadow part through the translucent caster
        // This can be improved by:
        // TODO: do not draw the shadow behind the caster at all during the tesselation phase
        if (radius > 0 && !isOpaque) {
            // Rounded edge.
            int save = canvas.save();
            canvas.drawBitmap(bitmap, destSrc, dest, null);
            canvas.restoreToCount(save);
            return;
        }

        /**
         * ----------------------------------
         * |                                |
         * |              top               |
         * |                                |
         * ----------------------------------
         * |      |                 |       |
         * | left |  shadow caster  | right |
         * |      |                 |       |
         * ----------------------------------
         * |                                |
         * |            bottom              |
         * |                                |
         * ----------------------------------
         *
         * dest == top + left + shadow caster + right + bottom
         * Visually, canvas.drawBitmap(bitmap, destSrc, dest, paint) would achieve the same result.
         */
        int gap = (int) Math.ceil(radius * SCALE_DOWN * sRoundedGap);
        shadowCaster.bottom -= gap;
        shadowCaster.top += gap;
        shadowCaster.left += gap;
        shadowCaster.right -= gap;
        Rect left = new Rect(dest.left, shadowCaster.top, shadowCaster.left,
                shadowCaster.bottom);
        int leftScaled = left.width() / SCALE_DOWN + destSrc.left;

        Rect top = new Rect(dest.left, dest.top, dest.right, shadowCaster.top);
        int topScaled = top.height() / SCALE_DOWN + destSrc.top;

        Rect right = new Rect(shadowCaster.right, shadowCaster.top, dest.right,
                shadowCaster.bottom);
        int rightScaled = (shadowCaster.right - dest.left) / SCALE_DOWN + destSrc.left;

        Rect bottom = new Rect(dest.left, shadowCaster.bottom, dest.right, dest.bottom);
        int bottomScaled = (shadowCaster.bottom - dest.top) / SCALE_DOWN + destSrc.top;

        // calculate parts of the middle ground that can be ignored.
        Rect leftSrc = new Rect(destSrc.left, topScaled, leftScaled, bottomScaled);
        Rect topSrc = new Rect(destSrc.left, destSrc.top, destSrc.right, topScaled);
        Rect rightSrc = new Rect(rightScaled, topScaled, destSrc.right, bottomScaled);
        Rect bottomSrc = new Rect(destSrc.left, bottomScaled, destSrc.right, destSrc.bottom);

        int save = canvas.save();
        Paint paint = new Paint();
        canvas.drawBitmap(bitmap, leftSrc, left, paint);
        canvas.drawBitmap(bitmap, topSrc, top, paint);
        canvas.drawBitmap(bitmap, rightSrc, right, paint);
        canvas.drawBitmap(bitmap, bottomSrc, bottom, paint);
        canvas.restoreToCount(save);
    }

    private static float[] getPoly(Rect rect, float elevation, float radius) {
        if (radius <= 0) {
            float[] poly = new float[ShadowConstants.RECT_VERTICES_SIZE * ShadowConstants.COORDINATE_SIZE];

            poly[0] = poly[9] = rect.left;
            poly[1] = poly[4] = rect.top;
            poly[3] = poly[6] = rect.right;
            poly[7] = poly[10] = rect.bottom;
            poly[2] = poly[5] = poly[8] = poly[11] = elevation;

            return poly;
        }

        return buildRoundedEdges(rect, elevation, radius);
    }

    private static float[] buildRoundedEdges(
            Rect rect, float elevation, float radius) {

        float[] roundedEdgeVertices = new float[(ShadowConstants.SPLICE_ROUNDED_EDGE + 1) * 4 * 3];
        int index = 0;
        // 1.0 LT. From theta 0 to pi/2 in K division.
        for (int i = 0; i <= ShadowConstants.SPLICE_ROUNDED_EDGE; i++) {
            double theta = (Math.PI / 2.0d) * ((double) i / ShadowConstants.SPLICE_ROUNDED_EDGE);
            float x = (float) (rect.left + (radius - radius * Math.cos(theta)));
            float y = (float) (rect.top + (radius - radius * Math.sin(theta)));
            roundedEdgeVertices[index++] = x;
            roundedEdgeVertices[index++] = y;
            roundedEdgeVertices[index++] = elevation;
        }

        // 2.0 RT
        for (int i = ShadowConstants.SPLICE_ROUNDED_EDGE; i >= 0; i--) {
            double theta = (Math.PI / 2.0d) * ((double) i / ShadowConstants.SPLICE_ROUNDED_EDGE);
            float x = (float) (rect.right - (radius - radius * Math.cos(theta)));
            float y = (float) (rect.top + (radius - radius * Math.sin(theta)));
            roundedEdgeVertices[index++] = x;
            roundedEdgeVertices[index++] = y;
            roundedEdgeVertices[index++] = elevation;
        }

        // 3.0 RB
        for (int i = 0; i <= ShadowConstants.SPLICE_ROUNDED_EDGE; i++) {
            double theta = (Math.PI / 2.0d) * ((double) i / ShadowConstants.SPLICE_ROUNDED_EDGE);
            float x = (float) (rect.right - (radius - radius * Math.cos(theta)));
            float y = (float) (rect.bottom - (radius - radius * Math.sin(theta)));
            roundedEdgeVertices[index++] = x;
            roundedEdgeVertices[index++] = y;
            roundedEdgeVertices[index++] = elevation;
        }

        // 4.0 LB
        for (int i = ShadowConstants.SPLICE_ROUNDED_EDGE; i >= 0; i--) {
            double theta = (Math.PI / 2.0d) * ((double) i / ShadowConstants.SPLICE_ROUNDED_EDGE);
            float x = (float) (rect.left + (radius - radius * Math.cos(theta)));
            float y = (float) (rect.bottom - (radius - radius * Math.sin(theta)));
            roundedEdgeVertices[index++] = x;
            roundedEdgeVertices[index++] = y;
            roundedEdgeVertices[index++] = elevation;
        }

        return roundedEdgeVertices;
    }
}
