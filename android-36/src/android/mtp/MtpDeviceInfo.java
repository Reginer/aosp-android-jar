/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.mtp;

import android.annotation.NonNull;

/**
 * This class encapsulates information about an MTP device.
 * This corresponds to the DeviceInfo Dataset described in
 * section 5.1.1 of the MTP specification.
 */
public class MtpDeviceInfo {

    private String mManufacturer;
    private String mModel;
    private String mVersion;
    private String mSerialNumber;
    private int[] mOperationsSupported;
    private int[] mEventsSupported;
    private int[] mDevicePropertySupported;

    // only instantiated via JNI
    private MtpDeviceInfo() {
    }

    /**
     * Returns the manufacturer's name for the MTP device
     *
     * @return the manufacturer name
     */
    public final @NonNull String getManufacturer() {
        return mManufacturer;
    }

    /**
     * Returns the model name for the MTP device
     *
     * @return the model name
     */
    public final @NonNull String getModel() {
        return mModel;
    }

    /**
     * Returns the version string the MTP device
     *
     * @return the device version
     */
    public final @NonNull String getVersion() {
        return mVersion;
    }

    /**
     * Returns the unique serial number for the MTP device
     *
     * @return the serial number
     */
    public final @NonNull String getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * Returns operation code supported by the device.
     *
     * @return supported operation code. Can be null if device does not provide the property.
     * @see MtpConstants#OPERATION_GET_DEVICE_INFO
     * @see MtpConstants#OPERATION_OPEN_SESSION
     * @see MtpConstants#OPERATION_CLOSE_SESSION
     * @see MtpConstants#OPERATION_GET_STORAGE_I_DS
     * @see MtpConstants#OPERATION_GET_STORAGE_INFO
     * @see MtpConstants#OPERATION_GET_NUM_OBJECTS
     * @see MtpConstants#OPERATION_GET_OBJECT_HANDLES
     * @see MtpConstants#OPERATION_GET_OBJECT_INFO
     * @see MtpConstants#OPERATION_GET_OBJECT
     * @see MtpConstants#OPERATION_GET_THUMB
     * @see MtpConstants#OPERATION_DELETE_OBJECT
     * @see MtpConstants#OPERATION_SEND_OBJECT_INFO
     * @see MtpConstants#OPERATION_SEND_OBJECT
     * @see MtpConstants#OPERATION_INITIATE_CAPTURE
     * @see MtpConstants#OPERATION_FORMAT_STORE
     * @see MtpConstants#OPERATION_RESET_DEVICE
     * @see MtpConstants#OPERATION_SELF_TEST
     * @see MtpConstants#OPERATION_SET_OBJECT_PROTECTION
     * @see MtpConstants#OPERATION_POWER_DOWN
     * @see MtpConstants#OPERATION_GET_DEVICE_PROP_DESC
     * @see MtpConstants#OPERATION_GET_DEVICE_PROP_VALUE
     * @see MtpConstants#OPERATION_SET_DEVICE_PROP_VALUE
     * @see MtpConstants#OPERATION_RESET_DEVICE_PROP_VALUE
     * @see MtpConstants#OPERATION_TERMINATE_OPEN_CAPTURE
     * @see MtpConstants#OPERATION_MOVE_OBJECT
     * @see MtpConstants#OPERATION_COPY_OBJECT
     * @see MtpConstants#OPERATION_GET_PARTIAL_OBJECT
     * @see MtpConstants#OPERATION_INITIATE_OPEN_CAPTURE
     * @see MtpConstants#OPERATION_GET_OBJECT_PROPS_SUPPORTED
     * @see MtpConstants#OPERATION_GET_OBJECT_PROP_DESC
     * @see MtpConstants#OPERATION_GET_OBJECT_PROP_VALUE
     * @see MtpConstants#OPERATION_SET_OBJECT_PROP_VALUE
     * @see MtpConstants#OPERATION_GET_OBJECT_REFERENCES
     * @see MtpConstants#OPERATION_SET_OBJECT_REFERENCES
     * @see MtpConstants#OPERATION_SKIP
     */
    public final @NonNull int[] getOperationsSupported() {
        return mOperationsSupported;
    }

    /**
     * Returns event code supported by the device.
     *
     * @return supported event code. Can be null if device does not provide the property.
     * @see MtpEvent#EVENT_UNDEFINED
     * @see MtpEvent#EVENT_CANCEL_TRANSACTION
     * @see MtpEvent#EVENT_OBJECT_ADDED
     * @see MtpEvent#EVENT_OBJECT_REMOVED
     * @see MtpEvent#EVENT_STORE_ADDED
     * @see MtpEvent#EVENT_STORE_REMOVED
     * @see MtpEvent#EVENT_DEVICE_PROP_CHANGED
     * @see MtpEvent#EVENT_OBJECT_INFO_CHANGED
     * @see MtpEvent#EVENT_DEVICE_INFO_CHANGED
     * @see MtpEvent#EVENT_REQUEST_OBJECT_TRANSFER
     * @see MtpEvent#EVENT_STORE_FULL
     * @see MtpEvent#EVENT_DEVICE_RESET
     * @see MtpEvent#EVENT_STORAGE_INFO_CHANGED
     * @see MtpEvent#EVENT_CAPTURE_COMPLETE
     * @see MtpEvent#EVENT_UNREPORTED_STATUS
     * @see MtpEvent#EVENT_OBJECT_PROP_CHANGED
     * @see MtpEvent#EVENT_OBJECT_PROP_DESC_CHANGED
     * @see MtpEvent#EVENT_OBJECT_REFERENCES_CHANGED
     */
    public final @NonNull int[] getEventsSupported() {
        return mEventsSupported;
    }

    /**
     * Returns Device property code supported by the device.
     *
     * @return supported Device property code. Can be null if device does not provide the property.
     *
     * @see MtpConstants#DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER
     * @see MtpConstants#DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME
     * @see MtpConstants#DEVICE_PROPERTY_SESSION_INITIATOR_VERSION_INFO
     *
     * {@hide}
     */
    public final @NonNull int[] getDevicePropertySupported() {
        return mDevicePropertySupported;
    }

    /**
     * Returns if the given operation is supported by the device or not.
     * @param code Operation code.
     * @return If the given operation is supported by the device or not.
     */
    public boolean isOperationSupported(int code) {
        return isSupported(mOperationsSupported, code);
    }

    /**
     * Returns if the given event is supported by the device or not.
     * @param code Event code.
     * @return If the given event is supported by the device or not.
     */
    public boolean isEventSupported(int code) {
        return isSupported(mEventsSupported, code);
    }

    /**
     * Returns if the given Device property is supported by the device or not.
     * @param code Device property code.
     * @return If the given Device property is supported by the device or not.
     *
     * {@hide}
     */
    public boolean isDevicePropertySupported(int code) {
        return isSupported(mDevicePropertySupported, code);
    }

    /**
     * Returns if the code set contains code.
     * @hide
     */
    private static boolean isSupported(@NonNull int[] set, int code) {
        for (final int element : set) {
            if (element == code) {
                return true;
            }
        }
        return false;
    }
}
