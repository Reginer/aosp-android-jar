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

package com.android.clockwork.mediarouting;

import android.media.AudioDeviceAttributes;
import android.media.AudioSystem;

import java.util.List;

/** A wrapper for {@link AudioSystem} class to ease unit testing of the static methods used. */
class AudioSystemWrapper {

    /** A wrapper method for {@link AudioSystem#setDevicesRoleForStrategy(int, int, List)} */
    // TODO(b/265637734): Should be changed to AudioManager#setDeviceAsNonDefaultForStrategy in
    // Android U
    int setDevicesRoleForStrategy(int id, int role, List<AudioDeviceAttributes> devices) {
        return AudioSystem.setDevicesRoleForStrategy(id, role, devices);
    }
}
