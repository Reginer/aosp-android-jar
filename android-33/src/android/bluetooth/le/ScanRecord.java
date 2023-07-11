/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.bluetooth.le;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothUuid;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.ParcelUuid;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Represents a scan record from Bluetooth LE scan.
 */
@SuppressLint("AndroidFrameworkBluetoothPermission")
public final class ScanRecord {

    private static final String TAG = "ScanRecord";

    /** @hide */
    @IntDef(prefix = "DATA_TYPE_", value = {
        DATA_TYPE_FLAGS,
        DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL,
        DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE,
        DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL,
        DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE,
        DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL,
        DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE,
        DATA_TYPE_LOCAL_NAME_SHORT,
        DATA_TYPE_LOCAL_NAME_COMPLETE,
        DATA_TYPE_TX_POWER_LEVEL,
        DATA_TYPE_CLASS_OF_DEVICE,
        DATA_TYPE_SIMPLE_PAIRING_HASH_C,
        DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R,
        DATA_TYPE_DEVICE_ID,
        DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS,
        DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE,
        DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT,
        DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT,
        DATA_TYPE_SERVICE_DATA_16_BIT,
        DATA_TYPE_PUBLIC_TARGET_ADDRESS,
        DATA_TYPE_RANDOM_TARGET_ADDRESS,
        DATA_TYPE_APPEARANCE,
        DATA_TYPE_ADVERTISING_INTERVAL,
        DATA_TYPE_LE_BLUETOOTH_DEVICE_ADDRESS,
        DATA_TYPE_LE_ROLE,
        DATA_TYPE_SIMPLE_PAIRING_HASH_C_256,
        DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R_256,
        DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT,
        DATA_TYPE_SERVICE_DATA_32_BIT,
        DATA_TYPE_SERVICE_DATA_128_BIT,
        DATA_TYPE_LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE,
        DATA_TYPE_LE_SECURE_CONNECTIONS_RANDOM_VALUE,
        DATA_TYPE_URI,
        DATA_TYPE_INDOOR_POSITIONING,
        DATA_TYPE_TRANSPORT_DISCOVERY_DATA,
        DATA_TYPE_LE_SUPPORTED_FEATURES,
        DATA_TYPE_CHANNEL_MAP_UPDATE_INDICATION,
        DATA_TYPE_PB_ADV,
        DATA_TYPE_MESH_MESSAGE,
        DATA_TYPE_MESH_BEACON,
        DATA_TYPE_BIG_INFO,
        DATA_TYPE_BROADCAST_CODE,
        DATA_TYPE_RESOLVABLE_SET_IDENTIFIER,
        DATA_TYPE_ADVERTISING_INTERVAL_LONG,
        DATA_TYPE_3D_INFORMATION_DATA,
        DATA_TYPE_MANUFACTURER_SPECIFIC_DATA,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdvertisingDataType {}

    /**
     * Data type is not set for the filter. Will not filter advertising data type.
     */
    public static final int DATA_TYPE_NONE = -1;
    /**
     * Data type is Flags, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_FLAGS = 0x01;
    /**
     * Data type is Incomplete List of 16-bit Service Class UUIDs, see the Bluetooth Generic Access
     * Profile for the details.
     */
    public static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    /**
     * Data type is Complete List of 16-bit Service Class UUIDs, see the Bluetooth Generic Access
     * Profile for more details.
     */
    public static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    /**
     * Data type is Incomplete List of 32-bit Service Class UUIDs, see the Bluetooth Generic Access
     * Profile for the details.
     */
    public static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    /**
     * Data type is Complete List of 32-bit Service Class UUIDs, see the Bluetooth Generic Access
     * Profile for more details.
     */
    public static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    /**
     * Data type is Incomplete List of 128-bit Service Class UUIDs, see the Bluetooth Generic Access
     * Profile for the details.
     */
    public static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    /**
     * Data type is Complete List of 128-bit Service Class UUIDs, see the Bluetooth Generic Access
     * Profile for more details.
     */
    public static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    /**
     * Data type is Shortened Local Name, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    /**
     * Data type is Complete Local Name, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    /**
     * Data type is Tx Power Level, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    /**
     * Data type is Class of Device, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_CLASS_OF_DEVICE = 0x0D;
    /**
     * Data type is Simple Pairing Hash C, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_SIMPLE_PAIRING_HASH_C = 0x0E;
    /**
     * Data type is Simple Pairing Randomizer R, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R = 0x0F;
    /**
     * Data type is Device ID, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_DEVICE_ID = 0x10;
    /**
     * Data type is Security Manager Out of Band Flags, see the Bluetooth Generic Access Profile for
     * more details.
     */
    public static final int DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS = 0x11;
    /**
     * Data type is Slave Connection Interval Range, see the Bluetooth Generic Access Profile for
     * more details.
     */
    public static final int DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE = 0x12;
    /**
     * Data type is List of 16-bit Service Solicitation UUIDs, see the Bluetooth Generic Access
     * Profile for more details.
     */
    public static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT = 0x14;
    /**
     * Data type is List of 128-bit Service Solicitation UUIDs, see the Bluetooth Generic Access
     * Profile for more details.
     */
    public static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT = 0x15;
    /**
     * Data type is Service Data - 16-bit UUID, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_SERVICE_DATA_16_BIT = 0x16;
    /**
     * Data type is Public Target Address, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_PUBLIC_TARGET_ADDRESS = 0x17;
    /**
     * Data type is Random Target Address, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_RANDOM_TARGET_ADDRESS = 0x18;
    /**
     * Data type is Appearance, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_APPEARANCE = 0x19;
    /**
     * Data type is Advertising Interval, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_ADVERTISING_INTERVAL = 0x1A;
    /**
     * Data type is LE Bluetooth Device Address, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_LE_BLUETOOTH_DEVICE_ADDRESS = 0x1B;
    /**
     * Data type is LE Role, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_LE_ROLE = 0x1C;
    /**
     * Data type is Simple Pairing Hash C-256, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_SIMPLE_PAIRING_HASH_C_256 = 0x1D;
    /**
     * Data type is Simple Pairing Randomizer R-256, see the Bluetooth Generic Access Profile for
     * more details.
     */
    public static final int DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R_256 = 0x1E;
    /**
     * Data type is List of 32-bit Service Solicitation UUIDs, see the Bluetooth Generic Access
     * Profile for more details.
     */
    public static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT = 0x1F;
    /**
     * Data type is Service Data - 32-bit UUID, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_SERVICE_DATA_32_BIT = 0x20;
    /**
     * Data type is Service Data - 128-bit UUID, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_SERVICE_DATA_128_BIT = 0x21;
    /**
     * Data type is LE Secure Connections Confirmation Value, see the Bluetooth Generic Access
     * Profile for more details.
     */
    public static final int DATA_TYPE_LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE = 0x22;
    /**
     * Data type is LE Secure Connections Random Value, see the Bluetooth Generic Access Profile for
     * more details.
     */
    public static final int DATA_TYPE_LE_SECURE_CONNECTIONS_RANDOM_VALUE = 0x23;
    /**
     * Data type is URI, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_URI = 0x24;
    /**
     * Data type is Indoor Positioning, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_INDOOR_POSITIONING = 0x25;
    /**
     * Data type is Transport Discovery Data, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_TRANSPORT_DISCOVERY_DATA = 0x26;
    /**
     * Data type is LE Supported Features, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_LE_SUPPORTED_FEATURES = 0x27;
    /**
     * Data type is Channel Map Update Indication, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_CHANNEL_MAP_UPDATE_INDICATION = 0x28;
    /**
     * Data type is PB-ADV, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_PB_ADV = 0x29;
    /**
     * Data type is Mesh Message, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_MESH_MESSAGE = 0x2A;
    /**
     * Data type is Mesh Beacon, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_MESH_BEACON = 0x2B;
    /**
     * Data type is BIGInfo, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_BIG_INFO = 0x2C;
    /**
     * Data type is Broadcast_Code, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_BROADCAST_CODE = 0x2D;
    /**
     * Data type is Resolvable Set Identifier, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_RESOLVABLE_SET_IDENTIFIER = 0x2E;
    /**
     * Data type is Advertising Interval - long, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_ADVERTISING_INTERVAL_LONG = 0x2F;
    /**
     * Data type is 3D Information Data, see the Bluetooth Generic Access Profile for more details.
     */
    public static final int DATA_TYPE_3D_INFORMATION_DATA = 0x3D;
    /**
     * Data type is Manufacturer Specific Data, see the Bluetooth Generic Access Profile for more
     * details.
     */
    public static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    // Flags of the advertising data.
    private final int mAdvertiseFlags;

    @Nullable
    private final List<ParcelUuid> mServiceUuids;
    @Nullable
    private final List<ParcelUuid> mServiceSolicitationUuids;

    private final SparseArray<byte[]> mManufacturerSpecificData;

    private final Map<ParcelUuid, byte[]> mServiceData;

    // Transmission power level(in dB).
    private final int mTxPowerLevel;

    // Local name of the Bluetooth LE device.
    private final String mDeviceName;

    // Raw bytes of scan record.
    private final byte[] mBytes;

    private final HashMap<Integer, byte[]> mAdvertisingDataMap;

    /**
     * Returns the advertising flags indicating the discoverable mode and capability of the device.
     * Returns -1 if the flag field is not set.
     */
    public int getAdvertiseFlags() {
        return mAdvertiseFlags;
    }

    /**
     * Returns a list of service UUIDs within the advertisement that are used to identify the
     * bluetooth GATT services.
     */
    public List<ParcelUuid> getServiceUuids() {
        return mServiceUuids;
    }

    /**
     * Returns a list of service solicitation UUIDs within the advertisement that are used to
     * identify the Bluetooth GATT services.
     */
    @NonNull
    public List<ParcelUuid> getServiceSolicitationUuids() {
        return mServiceSolicitationUuids;
    }

    /**
     * Returns a sparse array of manufacturer identifier and its corresponding manufacturer specific
     * data.
     */
    public SparseArray<byte[]> getManufacturerSpecificData() {
        return mManufacturerSpecificData;
    }

    /**
     * Returns the manufacturer specific data associated with the manufacturer id. Returns
     * {@code null} if the {@code manufacturerId} is not found.
     */
    @Nullable
    public byte[] getManufacturerSpecificData(int manufacturerId) {
        if (mManufacturerSpecificData == null) {
            return null;
        }
        return mManufacturerSpecificData.get(manufacturerId);
    }

    /**
     * Returns a map of service UUID and its corresponding service data.
     */
    public Map<ParcelUuid, byte[]> getServiceData() {
        return mServiceData;
    }

    /**
     * Returns the service data byte array associated with the {@code serviceUuid}. Returns
     * {@code null} if the {@code serviceDataUuid} is not found.
     */
    @Nullable
    public byte[] getServiceData(ParcelUuid serviceDataUuid) {
        if (serviceDataUuid == null || mServiceData == null) {
            return null;
        }
        return mServiceData.get(serviceDataUuid);
    }

    /**
     * Returns the transmission power level of the packet in dBm. Returns {@link Integer#MIN_VALUE}
     * if the field is not set. This value can be used to calculate the path loss of a received
     * packet using the following equation:
     * <p>
     * <code>pathloss = txPowerLevel - rssi</code>
     */
    public int getTxPowerLevel() {
        return mTxPowerLevel;
    }

    /**
     * Returns the local name of the BLE device. This is a UTF-8 encoded string.
     */
    @Nullable
    public String getDeviceName() {
        return mDeviceName;
    }


    /**
     * Returns a map of advertising data type and its corresponding advertising data.
     * The values of advertising data type are defined in the Bluetooth Generic Access Profile
     * (https://www.bluetooth.com/specifications/assigned-numbers/)
     */
    public @NonNull Map<Integer, byte[]> getAdvertisingDataMap() {
        return mAdvertisingDataMap;
    }

    /**
     * Returns raw bytes of scan record.
     */
    public byte[] getBytes() {
        return mBytes;
    }

    /**
     * Test if any fields contained inside this scan record are matched by the
     * given matcher.
     *
     * @hide
     */
    public boolean matchesAnyField(@NonNull Predicate<byte[]> matcher) {
        int pos = 0;
        while (pos < mBytes.length) {
            final int length = mBytes[pos] & 0xFF;
            if (length == 0) {
                break;
            }
            if (matcher.test(Arrays.copyOfRange(mBytes, pos, pos + length + 1))) {
                return true;
            }
            pos += length + 1;
        }
        return false;
    }

    private ScanRecord(List<ParcelUuid> serviceUuids,
            List<ParcelUuid> serviceSolicitationUuids,
            SparseArray<byte[]> manufacturerData,
            Map<ParcelUuid, byte[]> serviceData,
            int advertiseFlags, int txPowerLevel,
            String localName, HashMap<Integer, byte[]> advertisingDataMap, byte[] bytes) {
        mServiceSolicitationUuids = serviceSolicitationUuids;
        mServiceUuids = serviceUuids;
        mManufacturerSpecificData = manufacturerData;
        mServiceData = serviceData;
        mDeviceName = localName;
        mAdvertiseFlags = advertiseFlags;
        mTxPowerLevel = txPowerLevel;
        mAdvertisingDataMap = advertisingDataMap;
        mBytes = bytes;
    }

    /**
     * Parse scan record bytes to {@link ScanRecord}.
     * <p>
     * The format is defined in Bluetooth 4.1 specification, Volume 3, Part C, Section 11 and 18.
     * <p>
     * All numerical multi-byte entities and values shall use little-endian <strong>byte</strong>
     * order.
     *
     * @param scanRecord The scan record of Bluetooth LE advertisement and/or scan response.
     * @hide
     */
    @UnsupportedAppUsage
    public static ScanRecord parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }

        int currentPos = 0;
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<ParcelUuid>();
        List<ParcelUuid> serviceSolicitationUuids = new ArrayList<ParcelUuid>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;

        SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
        Map<ParcelUuid, byte[]> serviceData = new ArrayMap<ParcelUuid, byte[]>();
        HashMap<Integer, byte[]> advertisingDataMap = new HashMap<Integer, byte[]>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;
                byte[] advertisingData = extractBytes(scanRecord, currentPos, dataLength);
                advertisingDataMap.put(fieldType, advertisingData);
                switch (fieldType) {
                    case DATA_TYPE_FLAGS:
                        advertiseFlag = scanRecord[currentPos] & 0xFF;
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos,
                                dataLength, BluetoothUuid.UUID_BYTES_16_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_32_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_128_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT:
                        parseServiceSolicitationUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_16_BIT, serviceSolicitationUuids);
                        break;
                    case DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT:
                        parseServiceSolicitationUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_32_BIT, serviceSolicitationUuids);
                        break;
                    case DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT:
                        parseServiceSolicitationUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_128_BIT, serviceSolicitationUuids);
                        break;
                    case DATA_TYPE_LOCAL_NAME_SHORT:
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(
                                extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    case DATA_TYPE_TX_POWER_LEVEL:
                        txPowerLevel = scanRecord[currentPos];
                        break;
                    case DATA_TYPE_SERVICE_DATA_16_BIT:
                    case DATA_TYPE_SERVICE_DATA_32_BIT:
                    case DATA_TYPE_SERVICE_DATA_128_BIT:
                        int serviceUuidLength = BluetoothUuid.UUID_BYTES_16_BIT;
                        if (fieldType == DATA_TYPE_SERVICE_DATA_32_BIT) {
                            serviceUuidLength = BluetoothUuid.UUID_BYTES_32_BIT;
                        } else if (fieldType == DATA_TYPE_SERVICE_DATA_128_BIT) {
                            serviceUuidLength = BluetoothUuid.UUID_BYTES_128_BIT;
                        }

                        byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos,
                                serviceUuidLength);
                        ParcelUuid serviceDataUuid = BluetoothUuid.parseUuidFrom(
                                serviceDataUuidBytes);
                        byte[] serviceDataArray = extractBytes(scanRecord,
                                currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                        serviceData.put(serviceDataUuid, serviceDataArray);
                        break;
                    case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8)
                                + (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2,
                                dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

            if (serviceUuids.isEmpty()) {
                serviceUuids = null;
            }
            return new ScanRecord(serviceUuids, serviceSolicitationUuids, manufacturerData,
                    serviceData, advertiseFlag, txPowerLevel, localName, advertisingDataMap,
                    scanRecord);
        } catch (Exception e) {
            Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
            return new ScanRecord(null, null, null, null, -1, Integer.MIN_VALUE, null,
                    advertisingDataMap, scanRecord);
        }
    }

    @Override
    public String toString() {
        return "ScanRecord [mAdvertiseFlags=" + mAdvertiseFlags + ", mServiceUuids=" + mServiceUuids
                + ", mServiceSolicitationUuids=" + mServiceSolicitationUuids
                + ", mManufacturerSpecificData=" + BluetoothLeUtils.toString(
                mManufacturerSpecificData)
                + ", mServiceData=" + BluetoothLeUtils.toString(mServiceData)
                + ", mTxPowerLevel=" + mTxPowerLevel + ", mDeviceName=" + mDeviceName + "]";
    }

    // Parse service UUIDs.
    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength,
            int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos,
                    uuidLength);
            serviceUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    /**
     * Parse service Solicitation UUIDs.
     */
    private static int parseServiceSolicitationUuid(byte[] scanRecord, int currentPos,
            int dataLength, int uuidLength, List<ParcelUuid> serviceSolicitationUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos, uuidLength);
            serviceSolicitationUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }
}
