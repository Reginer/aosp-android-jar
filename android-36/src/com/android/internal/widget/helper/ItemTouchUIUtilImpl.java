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

package com.android.internal.widget.helper;

import android.graphics.Canvas;
import android.view.View;

import com.android.internal.R;
import com.android.internal.widget.RecyclerView;

/**
 * Package private class to keep implementations. Putting them inside ItemTouchUIUtil makes them
 * public API, which is not desired in this case.
 */
class ItemTouchUIUtilImpl implements ItemTouchUIUtil {
    @Override
    public void onDraw(Canvas c, RecyclerView recyclerView, View view,
            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (isCurrentlyActive) {
            Object originalElevation = view.getTag(
                    R.id.item_touch_helper_previous_elevation);
            if (originalElevation == null) {
                originalElevation = view.getElevation();
                float newElevation = 1f + findMaxElevation(recyclerView, view);
                view.setElevation(newElevation);
                view.setTag(R.id.item_touch_helper_previous_elevation,
                        originalElevation);
            }
        }
        view.setTranslationX(dX);
        view.setTranslationY(dY);
    }

    private float findMaxElevation(RecyclerView recyclerView, View itemView) {
        final int childCount = recyclerView.getChildCount();
        float max = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = recyclerView.getChildAt(i);
            if (child == itemView) {
                continue;
            }
            final float elevation = child.getElevation();
            if (elevation > max) {
                max = elevation;
            }
        }
        return max;
    }

    @Override
    public void clearView(View view) {
        final Object tag = view.getTag(
                R.id.item_touch_helper_previous_elevation);
        if (tag != null && tag instanceof Float) {
            view.setElevation((Float) tag);
        }
        view.setTag(R.id.item_touch_helper_previous_elevation, null);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
    }

    @Override
    public void onSelected(View view) {
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView recyclerView,
            View view, float dX, float dY, int actionState, boolean isCurrentlyActive) {
    }
}
