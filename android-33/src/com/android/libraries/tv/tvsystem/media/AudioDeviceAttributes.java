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

package com.android.libraries.tv.tvsystem.media;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.media.AudioDeviceInfo;
import android.media.AudioPort;
import android.media.AudioSystem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * @hide
 * Class to represent the attributes of an audio device: its type (speaker, headset...), address
 * (if known) and role (input, output).
 * <p>Unlike {@link AudioDeviceInfo}, the device
 * doesn't need to be connected to be uniquely identified, it can
 * for instance represent a specific A2DP headset even after a
 * disconnection, whereas the corresponding <code>AudioDeviceInfo</code>
 * would then be invalid.
 * <p>While creating / obtaining an instance is not protected by a
 * permission, APIs using one rely on MODIFY_AUDIO_ROUTING.
 */
@SystemApi
public final class AudioDeviceAttributes {

    /**
     * A role identifying input devices, such as microphones.
     */
    public static final int ROLE_INPUT = AudioPort.ROLE_SOURCE;
    /**
     * A role identifying output devices, such as speakers or headphones.
     */
    public static final int ROLE_OUTPUT = AudioPort.ROLE_SINK;

    /** @hide */
    @IntDef(flag = false, prefix = "ROLE_", value = {
            ROLE_INPUT, ROLE_OUTPUT }
    )
    @Retention(RetentionPolicy.SOURCE)
    public @interface Role {}

    /**
     * The audio device type, as defined in {@link AudioDeviceInfo}
     */
    private final @AudioDeviceInfo.AudioDeviceType int mType;
    /**
     * The unique address of the device. Some devices don't have addresses, only an empty string.
     */
    private final @NonNull String mAddress;

    /**
     * Is input or output device
     */
    private final @AudioDeviceAttributes.Role
    int mRole;

    /**
     * @hide
     * Constructor from a valid {@link AudioDeviceInfo}
     * @param deviceInfo the connected audio device from which to obtain the device-identifying
     *                   type and address.
     */
    @SystemApi
    public AudioDeviceAttributes(@NonNull AudioDeviceInfo deviceInfo) {
        Objects.requireNonNull(deviceInfo);
        mRole = deviceInfo.isSink() ? ROLE_OUTPUT : ROLE_INPUT;
        mType = deviceInfo.getType();
        mAddress = deviceInfo.getAddress();
    }

    /**
     * @hide
     * Constructor from role, device type and address
     * @param role indicates input or output role
     * @param type the device type, as defined in {@link AudioDeviceInfo}
     * @param address the address of the device, or an empty string for devices without one
     */
    @SystemApi
    public AudioDeviceAttributes(@AudioDeviceAttributes.Role int role,
            @AudioDeviceInfo.AudioDeviceType int type,
            @NonNull String address) {
        Objects.requireNonNull(address);
        if (role != ROLE_OUTPUT && role != ROLE_INPUT) {
            throw new IllegalArgumentException("Invalid role " + role);
        }

        mRole = role;
        mType = type;
        mAddress = address;
    }

    /*package*/ AudioDeviceAttributes(int nativeType, @NonNull String address) {
        mRole = (nativeType & AudioSystem.DEVICE_BIT_IN) != 0 ? ROLE_INPUT : ROLE_OUTPUT;
        mType = AudioDeviceInfo.convertInternalDeviceToDeviceType(nativeType);
        mAddress = address;
    }

    /**
     * @hide
     * Returns the role of a device
     * @return the role
     */
    @SystemApi
    public @AudioDeviceAttributes.Role
    int getRole() {
        return mRole;
    }

    /**
     * @hide
     * Returns the audio device type of a device
     * @return the type, as defined in {@link AudioDeviceInfo}
     */
    @SystemApi
    public @AudioDeviceInfo.AudioDeviceType int getType() {
        return mType;
    }

    /**
     * @hide
     * Returns the address of the audio device, or an empty string for devices without one
     * @return the device address
     */
    @SystemApi
    public @NonNull String getAddress() {
        return mAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRole, mType, mAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AudioDeviceAttributes that = (AudioDeviceAttributes) o;
        return ((mRole == that.mRole)
                && (mType == that.mType)
                && mAddress.equals(that.mAddress));
    }

    /** @hide */
    public static String roleToString(@AudioDeviceAttributes.Role int role) {
        return (role == ROLE_OUTPUT ? "output" : "input");
    }

    @Override
    public String toString() {
        return new String("AudioDeviceAttributes:"
                + " role:" + roleToString(mRole)
                + " type:" + (mRole == ROLE_OUTPUT ? AudioSystem.getOutputDeviceName(
                AudioDeviceInfo.convertDeviceTypeToInternalDevice(mType))
                : AudioSystem.getInputDeviceName(
                        AudioDeviceInfo.convertDeviceTypeToInternalDevice(mType)))
                + " addr:" + mAddress);
    }
}
