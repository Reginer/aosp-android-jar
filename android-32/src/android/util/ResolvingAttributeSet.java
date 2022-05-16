/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.util;

import com.android.ide.common.rendering.api.ResourceValue;

import android.annotation.NonNull;
import android.annotation.Nullable;

public interface ResolvingAttributeSet extends AttributeSet {
    /**
     * Returns the resolved value of the attribute with the given name and namespace
     *
     * @param namespace the namespace of the attribute
     * @param name the name of the attribute
     * @return the resolved value of the attribute, or null if the attribute does not exist
     */
    @Nullable
    ResourceValue getResolvedAttributeValue(@Nullable String namespace, @NonNull String name);
}
