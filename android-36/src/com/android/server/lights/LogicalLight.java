/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.server.lights;

import android.hardware.light.V2_0.Brightness;
import android.hardware.light.V2_0.Flash;

/**
 * Allow control over a logical light of a given type. The mapping of logical lights to physical
 * lights is HAL implementation-dependent.
 */
public abstract class LogicalLight {
    /**
     * Keep the light steady on or off.
     */
    public static final int LIGHT_FLASH_NONE = Flash.NONE;

    /**
     * Flash the light at specified rate.
     */
    public static final int LIGHT_FLASH_TIMED = Flash.TIMED;

    /**
     * Flash the light using hardware assist.
     */
    public static final int LIGHT_FLASH_HARDWARE = Flash.HARDWARE;

    /**
     * Light brightness is managed by a user setting.
     */
    public static final int BRIGHTNESS_MODE_USER = Brightness.USER;

    /**
     * Light brightness is managed by a light sensor.
     */
    public static final int BRIGHTNESS_MODE_SENSOR = Brightness.SENSOR;

    /**
     * Low-persistence light mode.
     */
    public static final int BRIGHTNESS_MODE_LOW_PERSISTENCE = Brightness.LOW_PERSISTENCE;

    /**
     * Set the brightness of a display.
     */
    public abstract void setBrightness(float brightness);

    /**
     * Set the brightness and mode of a display.
     */
    public abstract void setBrightness(float brightness, int brightnessMode);

    /**
     * Set the color of a light.
     */
    public abstract void setColor(int color);

    /**
     * Set the color of a light and control flashing.
     */
    public abstract void setFlashing(int color, int mode, int onMS, int offMS);

    /**
     * Pulses the light.
     */
    public abstract void pulse();

    /**
     * Pulses the light with a specified color for a specified duration.
     */
    public abstract void pulse(int color, int onMS);

    /**
     * Turns off the light.
     */
    public abstract void turnOff();

    /**
     * Set the VR mode of a display.
     */
    public abstract void setVrMode(boolean enabled);
}
