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

package android.net;

import com.android.net.module.util.Struct;

/**
 * Value type for per uid traffic control configuration map.
 *
 * @hide
 */
public class UidOwnerValue extends Struct {
    // Allowed interface index. Only applicable if IIF_MATCH is set in the rule bitmask below.
    @Field(order = 0, type = Type.S32)
    public final int iif;

    // A bitmask of match type.
    @Field(order = 1, type = Type.U32)
    public final long rule;

    public UidOwnerValue(final int iif, final long rule) {
        this.iif = iif;
        this.rule = rule;
    }
}
