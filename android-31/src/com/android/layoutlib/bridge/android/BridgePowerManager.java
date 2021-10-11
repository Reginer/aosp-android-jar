/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.layoutlib.bridge.android;

import android.os.BatterySaverPolicyConfig;
import android.os.ParcelDuration;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeReason;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.WorkSource;

/**
 * Fake implementation of IPowerManager.
 */
public class BridgePowerManager implements IPowerManager {

    @Override
    public boolean isInteractive() throws RemoteException {
        return true;
    }

    @Override
    public boolean isPowerSaveMode() throws RemoteException {
        return false;
    }

    @Override
    public boolean setPowerSaveModeEnabled(boolean mode) throws RemoteException {
        return false;
    }

    @Override
    public BatterySaverPolicyConfig getFullPowerSavePolicy() {
        return new BatterySaverPolicyConfig.Builder().build();
    }

    @Override
    public boolean setFullPowerSavePolicy(BatterySaverPolicyConfig config) {
        return false;
    }

    @Override
    public boolean setDynamicPowerSaveHint(boolean powerSaveHint, int disableThreshold)
            throws RemoteException {
        return false;
    }

    @Override
    public boolean setAdaptivePowerSavePolicy(BatterySaverPolicyConfig config)
            throws RemoteException {
        return false;
    }

    @Override
    public boolean setAdaptivePowerSaveEnabled(boolean enabled) throws RemoteException {
        return false;
    }

    @Override
    public int getPowerSaveModeTrigger() {
        return 0;
    }

    public PowerSaveState getPowerSaveState(int serviceType) {
        return null;
    }

    @Override
    public void setBatteryDischargePrediction(ParcelDuration timeRemaining,
            boolean isPersonalized) {
        // pass for now
    }

    @Override
    public ParcelDuration getBatteryDischargePrediction() {
        return null;
    }

    @Override
    public boolean isBatteryDischargePredictionPersonalized() {
        return false;
    }

    @Override
    public IBinder asBinder() {
        // pass for now.
        return null;
    }

    @Override
    public void acquireWakeLock(IBinder arg0, int arg1, String arg2, String arg2_5, WorkSource arg3,
            String arg4, int arg5)
            throws RemoteException {
        // pass for now.
    }

    @Override
    public void acquireWakeLockAsync(IBinder arg0, int arg1, String arg2, String arg2_5,
            WorkSource arg3, String arg4) throws RemoteException {
        // pass for now.
    }

    @Override
    public void acquireWakeLockWithUid(IBinder arg0, int arg1, String arg2, String arg2_5,
            int arg3, int arg4)
            throws RemoteException {
        // pass for now.
    }

    @Override
    public void setPowerBoost(int boost, int durationMs) {
        // pass for now.
    }

    @Override
    public void setPowerMode(int mode, boolean enabled) {
        // pass for now.
    }

    @Override
    public boolean setPowerModeChecked(int mode, boolean enabled) {
        return false;
    }

    @Override
    public void crash(String arg0) throws RemoteException {
        // pass for now.
    }

    @Override
    public void goToSleep(long arg0, int arg1, int arg2) throws RemoteException {
        // pass for now.
    }

    @Override
    public void nap(long arg0) throws RemoteException {
        // pass for now.
    }

    @Override
    public float getBrightnessConstraint(int constraint) {
        return PowerManager.BRIGHTNESS_MAX;
    }

    @Override
    public void reboot(boolean confirm, String reason, boolean wait) {
        // pass for now.
    }

    @Override
    public void rebootSafeMode(boolean confirm, boolean wait) {
        // pass for now.
    }

    @Override
    public void shutdown(boolean confirm, String reason, boolean wait) {
        // pass for now.
    }

    @Override
    public void releaseWakeLock(IBinder arg0, int arg1) throws RemoteException {
        // pass for now.
    }

    @Override
    public void releaseWakeLockAsync(IBinder arg0, int arg1) throws RemoteException {
        // pass for now.
    }

    @Override
    public void updateWakeLockUids(IBinder arg0, int[] arg1) throws RemoteException {
        // pass for now.
    }

    @Override
    public void updateWakeLockUidsAsync(IBinder arg0, int[] arg1) throws RemoteException {
        // pass for now.
    }

    @Override
    public void setAttentionLight(boolean arg0, int arg1) throws RemoteException {
        // pass for now.
    }

    @Override
    public void setStayOnSetting(int arg0) throws RemoteException {
        // pass for now.
    }

    @Override
    public void updateWakeLockWorkSource(IBinder arg0, WorkSource arg1, String arg2) throws RemoteException {
        // pass for now.
    }

    @Override
    public boolean isWakeLockLevelSupported(int level) throws RemoteException {
        // pass for now.
        return true;
    }

    @Override
    public void userActivity(int displayId, long time, int event, int flags)
            throws RemoteException {
        // pass for now.
    }

    @Override
    public void wakeUp(long time, @WakeReason int reason, String details , String opPackageName)
            throws RemoteException {
        // pass for now.
    }

    @Override
    public void boostScreenBrightness(long time) throws RemoteException {
        // pass for now.
    }

    @Override
    public boolean isDeviceIdleMode() throws RemoteException {
        return false;
    }

    @Override
    public boolean isLightDeviceIdleMode() throws RemoteException {
        return false;
    }

    @Override
    public boolean isScreenBrightnessBoosted() throws RemoteException {
        return false;
    }

    @Override
    public int getLastShutdownReason() {
        return PowerManager.SHUTDOWN_REASON_UNKNOWN;
    }

    @Override
    public int getLastSleepReason() {
        return PowerManager.GO_TO_SLEEP_REASON_TIMEOUT;
    }

    @Override
    public void setDozeAfterScreenOff(boolean mode) throws RemoteException {
        // pass for now.
    }

    @Override
    public boolean isAmbientDisplayAvailable() {
        return false;
    }

    @Override
    public void suppressAmbientDisplay(String token, boolean suppress) {
        // pass for now
    }

    @Override
    public boolean isAmbientDisplaySuppressedForToken(String token) {
        return false;
    }

    @Override
    public boolean isAmbientDisplaySuppressedForTokenByApp(String token, int appUid) {
        return false;
    }

    @Override
    public boolean isAmbientDisplaySuppressed() {
        return false;
    }

    @Override
    public boolean forceSuspend() {
        return false;
    }
}
