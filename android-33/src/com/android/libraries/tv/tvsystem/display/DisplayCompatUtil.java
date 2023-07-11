/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.libraries.tv.tvsystem.display;

import android.view.Display;
import android.view.DisplayInfo;

public final class DisplayCompatUtil {

    /**
     * <p> Returns true if the connected display can be switched into a mode with minimal
     * post processing. </p>
     *
     * <p> If the Display sink is connected via HDMI, this method will return true if the
     * display supports either Auto Low Latency Mode or Game Content Type.
     *
     * <p> If the Display sink has an internal connection or uses some other protocol than
     * HDMI, this method will return true if the sink can be switched into an
     * implementation-defined low latency image processing mode. </p>
     *
     * <p> The ability to switch to a mode with minimal post processing may be disabled
     * by a user setting in the system settings menu. In that case, this method returns
     * false. </p>
     *
     * @see Display#isMinimalPostProcessingSupported
     * @see WindowCompatUtil#setPreferMinimalPostProcessing
     */
    public static boolean isMinimalPostProcessingSupported(Display display) {
        return display.isMinimalPostProcessingSupported();
    }

    /**
     * <p> Returns product-specific information about the display or the directly connected device
     *  on the display chain. For example, if the display is transitively connected, this field may
     *  contain product information about the intermediate device. </p>
     */
    public static DeviceProductInfo getDeviceProductInfo(Display display) {
        DisplayInfo displayInfo = new DisplayInfo();
        display.getDisplayInfo(displayInfo);
        android.hardware.display.DeviceProductInfo info = displayInfo.deviceProductInfo;
        if (info == null) {
            return null;
        }
        DeviceProductInfo.ManufactureDate manufactureDate;
        if (info.getManufactureDate() == null) {
            manufactureDate = null;
        } else {
            manufactureDate = new DeviceProductInfo.ManufactureDate(
                    info.getManufactureDate().getWeek(), info.getManufactureDate().getYear());
        }
        return new DeviceProductInfo(info.getName(), info.getManufacturerPnpId(),
                info.getProductId(), info.getModelYear(), manufactureDate,
                info.getConnectionToSinkType());
    }

    private DisplayCompatUtil() {}
}
