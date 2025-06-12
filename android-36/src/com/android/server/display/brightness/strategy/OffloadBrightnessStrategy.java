/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.display.brightness.strategy;

import android.os.PowerManager;

import com.android.server.display.DisplayBrightnessState;
import com.android.server.display.brightness.BrightnessReason;
import com.android.server.display.brightness.StrategyExecutionRequest;
import com.android.server.display.brightness.StrategySelectionNotifyRequest;
import com.android.server.display.feature.DisplayManagerFlags;

import java.io.PrintWriter;

/**
 * Manages the brightness of the display when auto-brightness is on, the screen has just turned on
 * and there is no available lux reading yet. The brightness value is read from the offload chip.
 */
public class OffloadBrightnessStrategy implements DisplayBrightnessStrategy {

    private float mOffloadScreenBrightness;
    private final DisplayManagerFlags mDisplayManagerFlags;

    public OffloadBrightnessStrategy(DisplayManagerFlags displayManagerFlags) {
        mDisplayManagerFlags = displayManagerFlags;
        mOffloadScreenBrightness = PowerManager.BRIGHTNESS_INVALID_FLOAT;
    }

    @Override
    public DisplayBrightnessState updateBrightness(
            StrategyExecutionRequest strategyExecutionRequest) {
        float offloadBrightness = mOffloadScreenBrightness;
        if (mDisplayManagerFlags.isRefactorDisplayPowerControllerEnabled()) {
            // We reset the offload brightness to invalid so that there is no stale value lingering
            // around. After this request is processed, the current brightness will be set to
            // offload brightness. Hence even if the lux values don't become valid for the next
            // request, we will fallback to the current brightness anyways.
            mOffloadScreenBrightness = PowerManager.BRIGHTNESS_INVALID_FLOAT;
        }
        BrightnessReason brightnessReason = new BrightnessReason();
        brightnessReason.setReason(BrightnessReason.REASON_OFFLOAD);
        return new DisplayBrightnessState.Builder()
                .setBrightness(offloadBrightness)
                .setBrightnessReason(brightnessReason)
                .setDisplayBrightnessStrategyName(getName())
                .setIsSlowChange(false)
                .setShouldUpdateScreenBrightnessSetting(true)
                .build();
    }

    @Override
    public String getName() {
        return "OffloadBrightnessStrategy";
    }

    public float getOffloadScreenBrightness() {
        return mOffloadScreenBrightness;
    }

    public void setOffloadScreenBrightness(float offloadScreenBrightness) {
        mOffloadScreenBrightness = offloadScreenBrightness;
    }

    /**
     * Dumps the state of this class.
     */
    @Override
    public void dump(PrintWriter writer) {
        writer.println("OffloadBrightnessStrategy:");
        writer.println("  mOffloadScreenBrightness:" + mOffloadScreenBrightness);
    }

    @Override
    public void strategySelectionPostProcessor(
            StrategySelectionNotifyRequest strategySelectionNotifyRequest) {
        // We reset the offload brightness only if the selected strategy is not offload or invalid,
        // as we are yet to use the brightness to evaluate the brightness state.
        if (!strategySelectionNotifyRequest.getSelectedDisplayBrightnessStrategy().getName()
                .equals(getName())
                && !strategySelectionNotifyRequest.getSelectedDisplayBrightnessStrategy().getName()
                .equals(
                DisplayBrightnessStrategyConstants.INVALID_BRIGHTNESS_STRATEGY_NAME)) {
            mOffloadScreenBrightness = PowerManager.BRIGHTNESS_INVALID_FLOAT;
        }
    }

    @Override
    public int getReason() {
        return BrightnessReason.REASON_OFFLOAD;
    }
}
