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
package android.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import java.util.List;
import java.util.Map;

/**
 * Listener provided in
 * {@link Activity#setEnterSharedElementCallback(SharedElementCallback)} and
 * {@link Activity#setExitSharedElementCallback(SharedElementCallback)} as well as
 * {@link Fragment#setEnterSharedElementCallback(SharedElementCallback)} and
 * {@link Fragment#setExitSharedElementCallback(SharedElementCallback)}
 * to monitor the Shared element transitions. The events can be used to customize Activity
 * and Fragment Transition behavior.
 */
public abstract class SharedElementCallback {
    private Matrix mTempMatrix;
    private static final String BUNDLE_SNAPSHOT_BITMAP = "sharedElement:snapshot:bitmap";
    private static final String BUNDLE_SNAPSHOT_HARDWARE_BUFFER =
            "sharedElement:snapshot:hardwareBuffer";
    private static final String BUNDLE_SNAPSHOT_COLOR_SPACE = "sharedElement:snapshot:colorSpace";
    private static final String BUNDLE_SNAPSHOT_IMAGE_SCALETYPE = "sharedElement:snapshot:imageScaleType";
    private static final String BUNDLE_SNAPSHOT_IMAGE_MATRIX = "sharedElement:snapshot:imageMatrix";

    static final SharedElementCallback NULL_CALLBACK = new SharedElementCallback() {
    };

    /**
     * In Activity Transitions, onSharedElementStart is called immediately before
     * capturing the start of the shared element state on enter and reenter transitions and
     * immediately before capturing the end of the shared element state for exit and return
     * transitions.
     * <p>
     * In Fragment Transitions, onSharedElementStart is called immediately before capturing the
     * start state of all shared element transitions.
     * <p>
     * This call can be used to adjust the transition start state by modifying the shared
     * element Views. Note that no layout step will be executed between onSharedElementStart
     * and the transition state capture.
     * <p>
     * For Activity Transitions, any changes made in {@link #onSharedElementEnd(List, List, List)}
     * that are not updated during by layout should be corrected in onSharedElementStart for exit and
     * return transitions. For example, rotation or scale will not be affected by layout and
     * if changed in {@link #onSharedElementEnd(List, List, List)}, it will also have to be reset
     * in onSharedElementStart again to correct the end state.
     *
     * @param sharedElementNames The names of the shared elements that were accepted into
     *                           the View hierarchy.
     * @param sharedElements The shared elements that are part of the View hierarchy.
     * @param sharedElementSnapshots The Views containing snap shots of the shared element
     *                               from the launching Window. These elements will not
     *                               be part of the scene, but will be positioned relative
     *                               to the Window decor View. This list is null for Fragment
     *                               Transitions.
     */
    public void onSharedElementStart(List<String> sharedElementNames,
            List<View> sharedElements, List<View> sharedElementSnapshots) {}

    /**
     * In Activity Transitions, onSharedElementEnd is called immediately before
     * capturing the end of the shared element state on enter and reenter transitions and
     * immediately before capturing the start of the shared element state for exit and return
     * transitions.
     * <p>
     * In Fragment Transitions, onSharedElementEnd is called immediately before capturing the
     * end state of all shared element transitions.
     * <p>
     * This call can be used to adjust the transition end state by modifying the shared
     * element Views. Note that no layout step will be executed between onSharedElementEnd
     * and the transition state capture.
     * <p>
     * Any changes made in {@link #onSharedElementStart(List, List, List)} that are not updated
     * during layout should be corrected in onSharedElementEnd. For example, rotation or scale
     * will not be affected by layout and if changed in
     * {@link #onSharedElementStart(List, List, List)}, it will also have to be reset in
     * onSharedElementEnd again to correct the end state.
     *
     * @param sharedElementNames The names of the shared elements that were accepted into
     *                           the View hierarchy.
     * @param sharedElements The shared elements that are part of the View hierarchy.
     * @param sharedElementSnapshots The Views containing snap shots of the shared element
     *                               from the launching Window. These elements will not
     *                               be part of the scene, but will be positioned relative
     *                               to the Window decor View. This list will be null for
     *                               Fragment Transitions.
     */
    public void onSharedElementEnd(List<String> sharedElementNames,
            List<View> sharedElements, List<View> sharedElementSnapshots) {}

    /**
     * Called after {@link #onMapSharedElements(java.util.List, java.util.Map)} when
     * transferring shared elements in. Any shared elements that have no mapping will be in
     * <var>rejectedSharedElements</var>. The elements remaining in
     * <var>rejectedSharedElements</var> will be transitioned out of the Scene. If a
     * View is removed from <var>rejectedSharedElements</var>, it must be handled by the
     * <code>SharedElementCallback</code>.
     * <p>
     * Views in rejectedSharedElements will have their position and size set to the
     * position of the calling shared element, relative to the Window decor View and contain
     * snapshots of the View from the calling Activity or Fragment. This
     * view may be safely added to the decor View's overlay to remain in position.
     * </p>
     * <p>This method is not called for Fragment Transitions. All rejected shared elements
     * will be handled by the exit transition.</p>
     *
     * @param rejectedSharedElements Views containing visual information of shared elements
     *                               that are not part of the entering scene. These Views
     *                               are positioned relative to the Window decor View. A
     *                               View removed from this list will not be transitioned
     *                               automatically.
     */
    public void onRejectSharedElements(List<View> rejectedSharedElements) {}

    /**
     * Lets the SharedElementCallback adjust the mapping of shared element names to
     * Views.
     *
     * @param names The names of all shared elements transferred from the calling Activity
     *              or Fragment in the order they were provided.
     * @param sharedElements The mapping of shared element names to Views. The best guess
     *                       will be filled into sharedElements based on the transitionNames.
     */
    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {}

    /**
     * Creates a snapshot of a shared element to be used by the remote Activity and reconstituted
     * with {@link #onCreateSnapshotView(android.content.Context, android.os.Parcelable)}. A
     * null return value will mean that the remote Activity will have a null snapshot View in
     * {@link #onSharedElementStart(java.util.List, java.util.List, java.util.List)} and
     * {@link #onSharedElementEnd(java.util.List, java.util.List, java.util.List)}.
     *
     * <p>This is not called for Fragment Transitions.</p>
     *
     * @param sharedElement The shared element View to create a snapshot for.
     * @param viewToGlobalMatrix A matrix containing a transform from the view to the screen
     *                           coordinates.
     * @param screenBounds The bounds of shared element in screen coordinate space. This is
     *                     the bounds of the view with the viewToGlobalMatrix applied.
     * @return A snapshot to send to the remote Activity to be reconstituted with
     * {@link #onCreateSnapshotView(android.content.Context, android.os.Parcelable)} and passed
     * into {@link #onSharedElementStart(java.util.List, java.util.List, java.util.List)} and
     * {@link #onSharedElementEnd(java.util.List, java.util.List, java.util.List)}.
     */
    public Parcelable onCaptureSharedElementSnapshot(View sharedElement, Matrix viewToGlobalMatrix,
            RectF screenBounds) {
        if (sharedElement instanceof ImageView) {
            ImageView imageView = ((ImageView) sharedElement);
            Drawable d = imageView.getDrawable();
            Drawable bg = imageView.getBackground();
            if (d != null && (bg == null || bg.getAlpha() == 0)) {
                Bitmap bitmap = TransitionUtils.createDrawableBitmap(d, imageView);
                if (bitmap != null) {
                    Bundle bundle = new Bundle();
                    if (bitmap.getConfig() != Bitmap.Config.HARDWARE) {
                        bundle.putParcelable(BUNDLE_SNAPSHOT_BITMAP, bitmap);
                    } else {
                        HardwareBuffer hardwareBuffer = bitmap.getHardwareBuffer();
                        bundle.putParcelable(BUNDLE_SNAPSHOT_HARDWARE_BUFFER, hardwareBuffer);
                        ColorSpace cs = bitmap.getColorSpace();
                        if (cs != null) {
                            bundle.putInt(BUNDLE_SNAPSHOT_COLOR_SPACE, cs.getId());
                        }
                    }
                    bundle.putString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE,
                            imageView.getScaleType().toString());
                    if (imageView.getScaleType() == ScaleType.MATRIX) {
                        Matrix matrix = imageView.getImageMatrix();
                        float[] values = new float[9];
                        matrix.getValues(values);
                        bundle.putFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX, values);
                    }
                    return bundle;
                }
            }
        }
        if (mTempMatrix == null) {
            mTempMatrix = new Matrix(viewToGlobalMatrix);
        } else {
            mTempMatrix.set(viewToGlobalMatrix);
        }
        ViewGroup parent = (ViewGroup) sharedElement.getParent();
        return TransitionUtils.createViewBitmap(sharedElement, mTempMatrix, screenBounds, parent);
    }

    /**
     * Reconstitutes a snapshot View from a Parcelable returned in
     * {@link #onCaptureSharedElementSnapshot(android.view.View, android.graphics.Matrix,
     * android.graphics.RectF)} to be used in {@link #onSharedElementStart(java.util.List,
     * java.util.List, java.util.List)} and {@link #onSharedElementEnd(java.util.List,
     * java.util.List, java.util.List)}. The returned View will be sized and positioned after
     * this call so that it is ready to be added to the decor View's overlay.
     *
     * <p>This is not called for Fragment Transitions.</p>
     *
     * @param context The Context used to create the snapshot View.
     * @param snapshot The Parcelable returned by {@link #onCaptureSharedElementSnapshot(
     * android.view.View, android.graphics.Matrix, android.graphics.RectF)}.
     * @return A View to be sent in {@link #onSharedElementStart(java.util.List, java.util.List,
     * java.util.List)} and {@link #onSharedElementEnd(java.util.List, java.util.List,
     * java.util.List)}. A null value will produce a null snapshot value for those two methods.
     */
    public View onCreateSnapshotView(Context context, Parcelable snapshot) {
        View view = null;
        if (snapshot instanceof Bundle) {
            Bundle bundle = (Bundle) snapshot;
            HardwareBuffer buffer = bundle.getParcelable(BUNDLE_SNAPSHOT_HARDWARE_BUFFER, android.hardware.HardwareBuffer.class);
            Bitmap bitmap = bundle.getParcelable(BUNDLE_SNAPSHOT_BITMAP, android.graphics.Bitmap.class);
            if (buffer == null && bitmap == null) {
                return null;
            }
            if (bitmap == null) {
                ColorSpace colorSpace = null;
                int colorSpaceId = bundle.getInt(BUNDLE_SNAPSHOT_COLOR_SPACE, 0);
                if (colorSpaceId >= 0 && colorSpaceId < ColorSpace.Named.values().length) {
                    colorSpace = ColorSpace.get(ColorSpace.Named.values()[colorSpaceId]);
                }
                bitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace);
            }
            ImageView imageView = new ImageView(context);
            view = imageView;
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(
                    ScaleType.valueOf(bundle.getString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE)));
            if (imageView.getScaleType() == ScaleType.MATRIX) {
                float[] values = bundle.getFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX);
                Matrix matrix = new Matrix();
                matrix.setValues(values);
                imageView.setImageMatrix(matrix);
            }
        } else if (snapshot instanceof Bitmap) {
            Bitmap bitmap = (Bitmap) snapshot;
            view = new View(context);
            Resources resources = context.getResources();
            view.setBackground(new BitmapDrawable(resources, bitmap));
        }
        return view;
    }

    /**
     * Called during an Activity Transition when the shared elements have arrived at the
     * final location and are ready to be transferred. This method is called for both the
     * source and destination Activities.
     * <p>
     * When the shared elements are ready to be transferred,
     * {@link OnSharedElementsReadyListener#onSharedElementsReady()}
     * must be called to trigger the transfer.
     * <p>
     * The default behavior is to trigger the transfer immediately.
     *
     * @param sharedElementNames The names of the shared elements that are being transferred..
     * @param sharedElements The shared elements that are part of the View hierarchy.
     * @param listener The listener to call when the shared elements are ready to be hidden
     *                 in the source Activity or shown in the destination Activity.
     */
    public void onSharedElementsArrived(List<String> sharedElementNames,
            List<View> sharedElements, OnSharedElementsReadyListener listener) {
        listener.onSharedElementsReady();
    }

    /**
     * Listener to be called after {@link
     * SharedElementCallback#onSharedElementsArrived(List, List, OnSharedElementsReadyListener)}
     * when the shared elements are ready to be hidden in the source Activity and shown in the
     * destination Activity.
     */
    public interface OnSharedElementsReadyListener {

        /**
         * Call this method during or after the OnSharedElementsReadyListener has been received
         * in {@link SharedElementCallback#onSharedElementsArrived(List, List,
         * OnSharedElementsReadyListener)} to indicate that the shared elements are ready to be
         * hidden in the source and shown in the destination Activity.
         */
        void onSharedElementsReady();
    }
}
