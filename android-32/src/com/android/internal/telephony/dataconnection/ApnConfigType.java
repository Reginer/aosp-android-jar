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
 * limitations under the License.
 */

package com.android.internal.telephony.dataconnection;

import android.telephony.Annotation;

/**
 * Container of network configuration settings relevant for telephony module.
 *
 */
public class ApnConfigType {

    private final int mType;
    private final int mPriority;

    public ApnConfigType(@Annotation.ApnType int type, int priority) {
        mType = type;
        mPriority = priority;
    }

    /**
     * Returns the apn type of this config type
     * @return Type of apn.
     */
    public int getType() {
        return mType;
    }

    /**
     * Returns the priority of this apn config type.
     * @return The priority of this apn.
     */
    public int getPriority() {
        return mPriority;
    }
}
