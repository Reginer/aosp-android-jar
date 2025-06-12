/*
 * Copyright 2024 The Android Open Source Project
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

package android.app.appsearch.ast.query;

import android.annotation.FlaggedApi;
import android.app.appsearch.PropertyPath;
import android.app.appsearch.ast.FunctionNode;

import com.android.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * {@link FunctionNode} representing the `propertyDefined` query function.
 *
 * <p>The `propertyDefined` query function will return all documents of types that define the given
 * property. This will include documents that do not have the property itself, so long as that
 * property is a part of the document's schema.
 *
 * <p>If you need to restrict to documents that have >=1 value(s) populated for that property, see
 * {@link HasPropertyNode}.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class PropertyDefinedNode implements FunctionNode {
    private PropertyPath mPropertyPath;

    /**
     * Constructor for a {@link PropertyDefinedNode} representing the query function
     * `propertyDefined` that takes in a {@link PropertyPath}.
     */
    public PropertyDefinedNode(@NonNull PropertyPath propertyPath) {
        mPropertyPath = Objects.requireNonNull(propertyPath);
    }

    /** Returns the name of the function represented by {@link PropertyDefinedNode}. */
    @Override
    @FunctionName
    public @NonNull String getFunctionName() {
        return FUNCTION_NAME_PROPERTY_DEFINED;
    }

    /**
     * Returns the {@link PropertyPath} representing the property being checked for in the document.
     */
    public @NonNull PropertyPath getPropertyPath() {
        return mPropertyPath;
    }

    /**
     * Sets the property being checked for in the document, as represented by {@link
     * PropertyDefinedNode}.
     */
    public void setPropertyPath(@NonNull PropertyPath property) {
        mPropertyPath = Objects.requireNonNull(property);
    }

    /**
     * Get the string representation of {@link PropertyDefinedNode}.
     *
     * <p>The string representation of {@link PropertyDefinedNode} is the function name followed by
     * the property path in quotes surrounded by parentheses.
     */
    @Override
    public @NonNull String toString() {
        return FUNCTION_NAME_PROPERTY_DEFINED + "(\"" + mPropertyPath + "\")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyDefinedNode that = (PropertyDefinedNode) o;
        return Objects.equals(mPropertyPath, that.mPropertyPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPropertyPath);
    }
}
