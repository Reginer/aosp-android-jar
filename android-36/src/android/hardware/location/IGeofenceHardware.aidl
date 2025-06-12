/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENS   E-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.location;

import android.location.IFusedGeofenceHardware;
import android.location.IGpsGeofenceHardware;
import android.hardware.location.GeofenceHardwareRequestParcelable;
import android.hardware.location.IGeofenceHardwareCallback;
import android.hardware.location.IGeofenceHardwareMonitorCallback;

/** @hide */
interface IGeofenceHardware {
    void setGpsGeofenceHardware(in IGpsGeofenceHardware service);
    void setFusedGeofenceHardware(in IFusedGeofenceHardware service);
    @EnforcePermission("LOCATION_HARDWARE")
    int[] getMonitoringTypes();
    @EnforcePermission("LOCATION_HARDWARE")
    int getStatusOfMonitoringType(int monitoringType);
    @EnforcePermission("LOCATION_HARDWARE")
    boolean addCircularFence(
            int monitoringType,
            in GeofenceHardwareRequestParcelable request,
            in IGeofenceHardwareCallback callback);
    @EnforcePermission("LOCATION_HARDWARE")
    boolean removeGeofence(int id, int monitoringType);
    @EnforcePermission("LOCATION_HARDWARE")
    boolean pauseGeofence(int id, int monitoringType);
    @EnforcePermission("LOCATION_HARDWARE")
    boolean resumeGeofence(int id, int monitoringType, int monitorTransitions);
    @EnforcePermission("LOCATION_HARDWARE")
    boolean registerForMonitorStateChangeCallback(int monitoringType,
            IGeofenceHardwareMonitorCallback callback);
    @EnforcePermission("LOCATION_HARDWARE")
    boolean unregisterForMonitorStateChangeCallback(int monitoringType,
            IGeofenceHardwareMonitorCallback callback);
}
