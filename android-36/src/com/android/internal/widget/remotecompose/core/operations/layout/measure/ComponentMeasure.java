/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.internal.widget.remotecompose.core.operations.layout.measure;

import android.annotation.NonNull;

import com.android.internal.widget.remotecompose.core.operations.layout.Component;

/** Encapsulate the result of a measure pass for a component */
public class ComponentMeasure {
    int mId = -1;
    float mX;
    float mY;
    float mW;
    float mH;
    int mVisibility = Component.Visibility.VISIBLE;

    public void setX(float value) {
        mX = value;
    }

    public void setY(float value) {
        mY = value;
    }

    public void setW(float value) {
        mW = value;
    }

    public void setH(float value) {
        mH = value;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public float getW() {
        return mW;
    }

    public float getH() {
        return mH;
    }

    public int getVisibility() {
        return mVisibility;
    }

    public void setVisibility(int visibility) {
        mVisibility = visibility;
    }

    public ComponentMeasure(int id, float x, float y, float w, float h, int visibility) {
        this.mId = id;
        this.mX = x;
        this.mY = y;
        this.mW = w;
        this.mH = h;
        this.mVisibility = visibility;
    }

    public ComponentMeasure(int id, float x, float y, float w, float h) {
        this(id, x, y, w, h, Component.Visibility.VISIBLE);
    }

    public ComponentMeasure(@NonNull Component component) {
        this(
                component.getComponentId(),
                component.getX(),
                component.getY(),
                component.getWidth(),
                component.getHeight(),
                component.mVisibility);
    }

    /**
     * Initialize this ComponentMeasure from another ComponentMeasure instance.
     *
     * @param m the ComponentMeasure to copy from
     */
    public void copyFrom(@NonNull ComponentMeasure m) {
        mX = m.mX;
        mY = m.mY;
        mW = m.mW;
        mH = m.mH;
        mVisibility = m.mVisibility;
    }

    /**
     * Returns true if the ComponentMeasure passed is identical to us
     *
     * @param m the ComponentMeasure to check
     * @return true if the passed ComponentMeasure is identical to ourself
     */
    public boolean same(@NonNull ComponentMeasure m) {
        return mX == m.mX && mY == m.mY && mW == m.mW && mH == m.mH && mVisibility == m.mVisibility;
    }

    /**
     * Returns true if the component will be gone
     *
     * @return true if gone
     */
    public boolean isGone() {
        return Component.Visibility.isGone(mVisibility);
    }

    /**
     * Returns true if the component will be visible
     *
     * @return true if visible
     */
    public boolean isVisible() {
        return Component.Visibility.isVisible(mVisibility);
    }

    /**
     * Returns true if the component will be invisible
     *
     * @return true if invisible
     */
    public boolean isInvisible() {
        return Component.Visibility.isInvisible(mVisibility);
    }

    /** Clear any override on the visibility */
    public void clearVisibilityOverride() {
        mVisibility = Component.Visibility.clearOverride(mVisibility);
    }

    /** Add a visibility override */
    public void addVisibilityOverride(int value) {
        mVisibility = Component.Visibility.clearOverride(mVisibility);
        mVisibility = Component.Visibility.add(mVisibility, value);
    }
}
