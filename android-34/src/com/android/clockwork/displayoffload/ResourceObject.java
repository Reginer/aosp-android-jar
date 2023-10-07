/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import android.util.Pair;

/**
 * This is a Pair that wraps a Hal resource with its identifier for the convenience of resources
 * to HalResourceStore.
 *
 * The Pair's key is the identifier and value is the resource.
 * The key should be either Integer or String.
 */
public class ResourceObject extends Pair<Object, Object> {
    private ResourceObject(Integer id, Object obj) {
        super(id, obj);
    }

    private ResourceObject(String name, Object obj) {
        super(name, obj);
    }

    public Integer getId() {
        return (Integer) this.first;
    }

    public String getName() {
        return (String) this.first;
    }

    public boolean useStringName() {
        return this.first instanceof String;
    }

    public Object getObject() {
        return this.second;
    }

    /**
     * Create a ResourceObject for integer identifier resource.
     *
     * @param id Integer identifier
     * @param resource Object for the resource
     * @return ResourceObject representing the id & resource pair.
     * @param <V> Type of the resource object
     */
    public static <V> ResourceObject of(Integer id, V resource) {
        return new ResourceObject(id, resource);
    }

    /**
     * Create a ResourceObject for String identifier resource.
     *
     * @param id String identifier
     * @param resource Object for the resource
     * @return ResourceObject representing the id & resource pair.
     * @param <V> Type of the resource object
     */
    public static <V> ResourceObject of(String id, V resource) {
        return new ResourceObject(id, resource);
    }
}
