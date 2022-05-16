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

package com.android.layout.remote.api;

import com.android.ide.common.rendering.api.HardwareConfig;
import com.android.resources.Density;
import com.android.resources.ScreenOrientation;
import com.android.resources.ScreenRound;
import com.android.resources.ScreenSize;
import com.android.tools.layoutlib.annotations.NotNull;

import java.io.Serializable;

/**
 * Remote version of the {@link HardwareConfig} class
 */
// TODO: Just make HardwareConfig serializable
public class RemoteHardwareConfig implements Serializable {
    private int mScreenWidth;
    private int mScreenHeight;
    private Density mDensity;
    private float mXdpi;
    private float mYdpi;
    private ScreenSize mScreenSize;
    private ScreenOrientation mOrientation;
    private ScreenRound mScreenRoundness;
    private boolean mHasSoftwareButtons;

    public RemoteHardwareConfig() {
    }

    public RemoteHardwareConfig(@NotNull HardwareConfig config) {
        this(config.getScreenWidth(), config.getScreenHeight(), config.getDensity(),
                config.getXdpi(), config.getYdpi(), config.getScreenSize(), config.getOrientation(),
                config.getScreenRoundness(), config.hasSoftwareButtons());
    }

    private RemoteHardwareConfig(int screenWidth, int screenHeight, Density density, float xdpi,
            float ydpi, ScreenSize screenSize, ScreenOrientation orientation,
            ScreenRound screenRoundness, boolean hasSoftwareButtons) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mDensity = density;
        mXdpi = xdpi;
        mYdpi = ydpi;
        mScreenSize = screenSize;
        mOrientation = orientation;
        mScreenRoundness = screenRoundness;
        mHasSoftwareButtons = hasSoftwareButtons;
    }

    @NotNull
    public HardwareConfig getHardwareConfig() {
        return new HardwareConfig(mScreenWidth, mScreenHeight, mDensity, mXdpi, mYdpi, mScreenSize,
                mOrientation, mScreenRoundness, mHasSoftwareButtons);
    }
}
