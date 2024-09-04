/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package java.lang;

import android.compat.annotation.UnsupportedAppUsage;
import dalvik.annotation.optimization.FastNative;

/**
 * A dex cache holds resolved copies of strings, fields, methods, and classes from the dexfile.
 */
final class DexCache {
    /** The classloader this dex cache is for. */
    private ClassLoader classLoader;

    /** The location of the associated dex file. */
    private String location;

    /** Holds C pointer to dexFile. */
    @UnsupportedAppUsage
    private long dexFile;

    /**
     * References to CallSite (C array pointer) as they become resolved following
     * interpreter semantics.
     */
    private long resolvedCallSites;

    /**
     * References to fields (C array pointers) as they become resolved following
     * interpreter semantics. May refer to fields defined in other dex files.
     */
    private long resolvedFields;
    private long resolvedFieldsArray;

    /**
     * References to MethodType (C array pointers) as they become resolved following
     * interpreter semantics.
     */
    private long resolvedMethodTypes;
    private long resolvedMethodTypesArray;

    /**
     * References to methods (C array pointers) as they become resolved following
     * interpreter semantics. May refer to methods defined in other dex files.
     */
    private long resolvedMethods;
    private long resolvedMethodsArray;

    /**
     * References to types (C array pointers) as they become resolved following
     * interpreter semantics. May refer to types defined in other dex files.
     */
    private long resolvedTypes;
    private long resolvedTypesArray;

    /**
     * References to strings (C array pointers) as they become resolved following
     * interpreter semantics. All strings are interned.
     */
    private long strings;
    private long stringsArray;

    // Only created by the VM.
    private DexCache() {}
}
