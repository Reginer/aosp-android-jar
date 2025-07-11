/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.internal.widget.remotecompose.core.documentation;

import android.annotation.NonNull;

public interface DocumentationBuilder {

    /**
     * Add arbitrary text to the documentation
     *
     * @param value
     */
    void add(@NonNull String value);

    /**
     * Add the operation to the documentation
     *
     * @param category category of the operation
     * @param id the OPCODE of the operation
     * @param name the name of the operation
     * @return a DocumentedOperation
     */
    @NonNull
    DocumentedOperation operation(@NonNull String category, int id, @NonNull String name);

    /**
     * Add the operation to the documentation as a Work in Progress (WIP) operation
     *
     * @param category category of the operation
     * @param id the OPCODE of the operation
     * @param name the name of the operation
     * @return a DocumentedOperation
     */
    @NonNull
    DocumentedOperation wipOperation(@NonNull String category, int id, @NonNull String name);
}
