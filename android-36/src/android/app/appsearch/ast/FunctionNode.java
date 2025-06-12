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

package android.app.appsearch.ast;

import android.annotation.FlaggedApi;
import android.annotation.StringDef;

import com.android.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link Node} that represents a function.
 *
 * <p>Every function node will have a function name and some arguments represented as fields on the
 * class extending {@link FunctionNode}.
 *
 * <p>FunctionNode should be implemented by a node that implements a specific function.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public interface FunctionNode extends Node {
    /**
     * Enums representing functions available to use in the query language.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
        FUNCTION_NAME_GET_SEARCH_STRING_PARAMETER,
        FUNCTION_NAME_HAS_PROPERTY,
        FUNCTION_NAME_PROPERTY_DEFINED,
        FUNCTION_NAME_SEARCH,
        FUNCTION_NAME_SEMANTIC_SEARCH
    })
    @interface FunctionName {}

    /**
     * Name of the query function represented by {@link
     * android.app.appsearch.ast.query.GetSearchStringParameterNode}.
     */
    String FUNCTION_NAME_GET_SEARCH_STRING_PARAMETER = "getSearchStringParameter";

    /**
     * Name of the query function represented by {@link
     * android.app.appsearch.ast.query.HasPropertyNode}.
     */
    String FUNCTION_NAME_HAS_PROPERTY = "hasProperty";

    /**
     * Name of the query function represented by {@link
     * android.app.appsearch.ast.query.PropertyDefinedNode}.
     */
    String FUNCTION_NAME_PROPERTY_DEFINED = "propertyDefined";

    /**
     * Name of the query function represented by {@link android.app.appsearch.ast.query.SearchNode}.
     */
    String FUNCTION_NAME_SEARCH = "search";

    /**
     * Name of the query function represented by {@link
     * android.app.appsearch.ast.query.SemanticSearchNode}.
     */
    String FUNCTION_NAME_SEMANTIC_SEARCH = "semanticSearch";

    /** Gets the name of the node that extends the {@link FunctionNode}. */
    @FunctionName
    @NonNull String getFunctionName();
}
