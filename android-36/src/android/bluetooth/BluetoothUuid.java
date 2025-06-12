/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.bluetooth;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.ParcelUuid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

/**
 * Static helper methods and constants to decode the ParcelUuid of remote devices. Bluetooth service
 * UUIDs are defined in the SDP section of the Bluetooth Assigned Numbers document. The constant 128
 * bit values in this class are calculated as: uuid * 2^96 + {@link #BASE_UUID}.
 *
 * @hide
 */
@SystemApi
@SuppressLint("AndroidFrameworkBluetoothPermission")
public final class BluetoothUuid {

    /**
     * UUID corresponding to the Audio sink role (also referred to as the A2DP sink role).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid A2DP_SINK =
            ParcelUuid.fromString("0000110B-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Audio source role (also referred to as the A2DP source role).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid A2DP_SOURCE =
            ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Advanced Audio Distribution Profile (A2DP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid ADV_AUDIO_DIST =
            ParcelUuid.fromString("0000110D-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Headset Profile (HSP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HSP =
            ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Headset Profile (HSP) Audio Gateway role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HSP_AG =
            ParcelUuid.fromString("00001112-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Hands-Free Profile (HFP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HFP =
            ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Hands-Free Profile (HFP) Audio Gateway role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HFP_AG =
            ParcelUuid.fromString("0000111F-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Audio Video Remote Control Profile (AVRCP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid AVRCP =
            ParcelUuid.fromString("0000110E-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Audio Video Remote Control Profile (AVRCP) controller role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid AVRCP_CONTROLLER =
            ParcelUuid.fromString("0000110F-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Audio Video Remote Control Profile (AVRCP) target role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid AVRCP_TARGET =
            ParcelUuid.fromString("0000110C-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the OBject EXchange (OBEX) Object Push Profile (OPP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid OBEX_OBJECT_PUSH =
            ParcelUuid.fromString("00001105-0000-1000-8000-00805f9b34fb");

    /**
     * UUID corresponding to the Human Interface Device (HID) profile.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HID =
            ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb");

    /**
     * UUID corresponding to the Human Interface Device over GATT Profile (HOGP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HOGP =
            ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb");

    /**
     * UUID corresponding to the Personal Area Network User (PANU) role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid PANU =
            ParcelUuid.fromString("00001115-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Network Access Point (NAP) role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid NAP =
            ParcelUuid.fromString("00001116-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Bluetooth Network Encapsulation Protocol (BNEP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid BNEP =
            ParcelUuid.fromString("0000000f-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Phonebook Access Profile (PBAP) client role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid PBAP_PCE =
            ParcelUuid.fromString("0000112e-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Phonebook Access Profile (PBAP) server role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid PBAP_PSE =
            ParcelUuid.fromString("0000112f-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Message Access Profile (MAP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid MAP =
            ParcelUuid.fromString("00001134-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Message Notification Server (MNS) role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid MNS =
            ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Message Access Server (MAS) role.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid MAS =
            ParcelUuid.fromString("00001132-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Sim Access Profile (SAP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid SAP =
            ParcelUuid.fromString("0000112D-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Hearing Aid Profile.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HEARING_AID =
            ParcelUuid.fromString("0000FDF0-0000-1000-8000-00805f9b34fb");

    /**
     * UUID corresponding to the Hearing Access Service (HAS).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid HAS =
            ParcelUuid.fromString("00001854-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Made For iPhone/iPod/iPad Hearing Aid Service (MFi HAS).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid MFI_HAS =
            ParcelUuid.fromString("7D74F4BD-C74A-4431-862C-CCE884371592");

    /**
     * UUID corresponding to Audio Stream Control (also known as Bluetooth Low Energy Audio).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid LE_AUDIO =
            ParcelUuid.fromString("0000184E-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Device Identification Profile (DIP).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid DIP =
            ParcelUuid.fromString("00001200-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Volume Control Service.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid VOLUME_CONTROL =
            ParcelUuid.fromString("00001844-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Generic Media Control Service.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid GENERIC_MEDIA_CONTROL =
            ParcelUuid.fromString("00001849-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Gaming Audio Profile (GMAP).
     *
     * @hide
     */
    @NonNull
    public static final ParcelUuid GMAP =
            ParcelUuid.fromString("00001858-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Media Control Service.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid MEDIA_CONTROL =
            ParcelUuid.fromString("00001848-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Coordinated Set Identification Service.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid COORDINATED_SET =
            ParcelUuid.fromString("00001846-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Common Audio Service.
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid CAP =
            ParcelUuid.fromString("00001853-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Battery Service.
     *
     * @hide
     */
    @NonNull
    public static final ParcelUuid BATTERY =
            ParcelUuid.fromString("0000180F-0000-1000-8000-00805F9B34FB");

    /**
     * UUID corresponding to the Broadcast Audio Scan Service (also known as LE Audio Broadcast
     * Assistant).
     *
     * @hide
     */
    @NonNull @SystemApi
    public static final ParcelUuid BASS =
            ParcelUuid.fromString("0000184F-0000-1000-8000-00805F9B34FB");

    /**
     * Telephony and Media Audio Profile (TMAP) UUID
     *
     * @hide
     */
    @NonNull
    public static final ParcelUuid TMAP =
            ParcelUuid.fromString("00001855-0000-1000-8000-00805F9B34FB");

    /** @hide */
    @NonNull @SystemApi
    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

    /**
     * Length of bytes for 16 bit UUID
     *
     * @hide
     */
    @SystemApi public static final int UUID_BYTES_16_BIT = 2;

    /**
     * Length of bytes for 32 bit UUID
     *
     * @hide
     */
    @SystemApi public static final int UUID_BYTES_32_BIT = 4;

    /**
     * Length of bytes for 128 bit UUID
     *
     * @hide
     */
    @SystemApi public static final int UUID_BYTES_128_BIT = 16;

    /**
     * Returns true if there any common ParcelUuids in uuidA and uuidB.
     *
     * @param uuidA - List of ParcelUuids
     * @param uuidB - List of ParcelUuids
     * @hide
     */
    @SystemApi
    public static boolean containsAnyUuid(
            @Nullable ParcelUuid[] uuidA, @Nullable ParcelUuid[] uuidB) {
        if (uuidA == null && uuidB == null) return true;

        if (uuidA == null) {
            return uuidB.length == 0;
        }

        if (uuidB == null) {
            return uuidA.length == 0;
        }

        HashSet<ParcelUuid> uuidSet = new HashSet<ParcelUuid>(Arrays.asList(uuidA));
        for (ParcelUuid uuid : uuidB) {
            if (uuidSet.contains(uuid)) return true;
        }
        return false;
    }

    /**
     * Extract the Service Identifier or the actual uuid from the Parcel Uuid. For example, if
     * 0000110B-0000-1000-8000-00805F9B34FB is the parcel Uuid, this function will return 110B
     */
    private static int getServiceIdentifierFromParcelUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        long value = (uuid.getMostSignificantBits() & 0xFFFFFFFF00000000L) >>> 32;
        return (int) value;
    }

    /**
     * Parse UUID from bytes. The {@code uuidBytes} can represent a 16-bit, 32-bit or 128-bit UUID,
     * but the returned UUID is always in 128-bit format. Note UUID is little endian in Bluetooth.
     *
     * @param uuidBytes Byte representation of uuid.
     * @return {@link ParcelUuid} parsed from bytes.
     * @throws IllegalArgumentException If the {@code uuidBytes} cannot be parsed.
     * @hide
     */
    @NonNull
    @SystemApi
    public static ParcelUuid parseUuidFrom(@Nullable byte[] uuidBytes) {
        if (uuidBytes == null) {
            throw new IllegalArgumentException("uuidBytes cannot be null");
        }
        int length = uuidBytes.length;
        if (length != UUID_BYTES_16_BIT
                && length != UUID_BYTES_32_BIT
                && length != UUID_BYTES_128_BIT) {
            throw new IllegalArgumentException("uuidBytes length invalid - " + length);
        }

        // Construct a 128 bit UUID.
        if (length == UUID_BYTES_128_BIT) {
            ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
            long msb = buf.getLong(8);
            long lsb = buf.getLong(0);
            return new ParcelUuid(new UUID(msb, lsb));
        }

        // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
        // 128_bit_value = uuid * 2^96 + BASE_UUID
        long shortUuid;
        if (length == UUID_BYTES_16_BIT) {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
        } else {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
            shortUuid += (uuidBytes[2] & 0xFF) << 16;
            shortUuid += (uuidBytes[3] & 0xFF) << 24;
        }
        long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
        long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
        return new ParcelUuid(new UUID(msb, lsb));
    }

    /**
     * Parse UUID to bytes. The returned value is shortest representation, a 16-bit, 32-bit or
     * 128-bit UUID, Note returned value is little endian (Bluetooth).
     *
     * @param uuid uuid to parse.
     * @return shortest representation of {@code uuid} as bytes.
     * @throws IllegalArgumentException If the {@code uuid} is null.
     * @hide
     */
    public static byte[] uuidToBytes(ParcelUuid uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }

        if (is16BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_16_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            return uuidBytes;
        }

        if (is32BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_32_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            uuidBytes[2] = (byte) ((uuidVal & 0xFF0000) >> 16);
            uuidBytes[3] = (byte) ((uuidVal & 0xFF000000) >> 24);
            return uuidBytes;
        }

        // Construct a 128 bit UUID.
        long msb = uuid.getUuid().getMostSignificantBits();
        long lsb = uuid.getUuid().getLeastSignificantBits();

        byte[] uuidBytes = new byte[UUID_BYTES_128_BIT];
        ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(8, msb);
        buf.putLong(0, lsb);
        return uuidBytes;
    }

    /**
     * Check whether the given parcelUuid can be converted to 16 bit bluetooth uuid.
     *
     * @return true if the parcelUuid can be converted to 16 bit uuid, false otherwise.
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean is16BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getUuid().getLeastSignificantBits()) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFF0000FFFFFFFFL) == 0x1000L);
    }

    /**
     * Check whether the given parcelUuid can be converted to 32 bit bluetooth uuid.
     *
     * @return true if the parcelUuid can be converted to 32 bit uuid, false otherwise.
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean is32BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getUuid().getLeastSignificantBits()) {
            return false;
        }
        if (is16BitUuid(parcelUuid)) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFFFFFFL) == 0x1000L);
    }

    private BluetoothUuid() {}
}
