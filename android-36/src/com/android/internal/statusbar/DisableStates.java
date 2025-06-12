/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.internal.statusbar;

import android.app.StatusBarManager.Disable2Flags;
import android.app.StatusBarManager.DisableFlags;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds display ids with their disable flags.
 */
public class DisableStates implements Parcelable {

    /**
     * A map of display IDs (integers) with corresponding disable flags.
     */
    public Map<Integer, Pair<@DisableFlags Integer, @Disable2Flags Integer>> displaysWithStates;

    /**
     * Whether the disable state change should be animated.
     */
    public boolean animate;

    public DisableStates(
            Map<Integer, Pair<@DisableFlags Integer, @Disable2Flags Integer>> displaysWithStates,
            boolean animate) {
        this.displaysWithStates = displaysWithStates;
        this.animate = animate;
    }

    public DisableStates(
            Map<Integer, Pair<@DisableFlags Integer, @Disable2Flags Integer>> displaysWithStates) {
        this(displaysWithStates, true);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(displaysWithStates.size()); // Write the size of the map
        for (Map.Entry<Integer, Pair<Integer, Integer>> entry : displaysWithStates.entrySet()) {
            dest.writeInt(entry.getKey());
            dest.writeInt(entry.getValue().first);
            dest.writeInt(entry.getValue().second);
        }
        dest.writeBoolean(animate);
    }

    /**
     * Used to make this class parcelable.
     */
    public static final Parcelable.Creator<DisableStates> CREATOR = new Parcelable.Creator<>() {
        @Override
        public DisableStates createFromParcel(Parcel source) {
            int size = source.readInt(); // Read the size of the map
            Map<Integer, Pair<Integer, Integer>> displaysWithStates = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                int key = source.readInt();
                int first = source.readInt();
                int second = source.readInt();
                displaysWithStates.put(key, new Pair<>(first, second));
            }
            final boolean animate = source.readBoolean();
            return new DisableStates(displaysWithStates, animate);
        }

        @Override
        public DisableStates[] newArray(int size) {
            return new DisableStates[size];
        }
    };
}

