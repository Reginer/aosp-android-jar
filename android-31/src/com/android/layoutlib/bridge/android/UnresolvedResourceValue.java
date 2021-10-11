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
package com.android.layoutlib.bridge.android;

import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceValueImpl;
import com.android.resources.ResourceType;

import android.annotation.NonNull;

/**
 * Special subclass that layoutlib uses to start the resolution process and recognize if the
 * resolution failed.
 */
public class UnresolvedResourceValue extends ResourceValueImpl {
    public UnresolvedResourceValue(
            @NonNull String value,
            @NonNull ResourceNamespace namespace,
            @NonNull ResourceNamespace.Resolver namespaceResolver) {
        super(namespace, ResourceType.STRING, "layoutlib", value);
        setNamespaceResolver(namespaceResolver);
    }
}
