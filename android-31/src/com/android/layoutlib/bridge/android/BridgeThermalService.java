/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.layoutlib.bridge.android;

import android.os.CoolingDevice;
import android.os.IBinder;
import android.os.IThermalEventListener;
import android.os.IThermalStatusListener;
import android.os.IThermalService;
import android.os.Temperature;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake implementation of IThermalService
 */
public class BridgeThermalService implements IThermalService {

    @Override
    public IBinder asBinder() {
        return null;
    }

    @Override
    public boolean registerThermalEventListener(IThermalEventListener listener) {
        return false;
    }

    @Override
    public boolean registerThermalEventListenerWithType(IThermalEventListener listener, int type) {
        return false;
    }

    @Override
    public boolean unregisterThermalEventListener(IThermalEventListener listener) {
        return false;
    }

    @Override
    public Temperature[] getCurrentTemperatures() {
        return new Temperature[0];
    }

    @Override
    public Temperature[] getCurrentTemperaturesWithType(int type) {
        return new Temperature[0];
    }

    @Override
    public boolean registerThermalStatusListener(IThermalStatusListener listener) {
        return false;
    }

    @Override
    public boolean unregisterThermalStatusListener(IThermalStatusListener listener) {
        return false;
    }

    @Override
    public int getCurrentThermalStatus() {
        return 0;
    }

    @Override
    public CoolingDevice[] getCurrentCoolingDevices() {
        return new CoolingDevice[0];
    }

    @Override
    public CoolingDevice[] getCurrentCoolingDevicesWithType(int type) {
        return new CoolingDevice[0];
    }

    @Override
    public float getThermalHeadroom(int forecastSeconds) {
        return Float.NaN;
    }
}
