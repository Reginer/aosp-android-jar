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

package android.app.appsearch.ast.operators;

import android.annotation.FlaggedApi;
import android.app.appsearch.PropertyPath;
import android.app.appsearch.ast.Node;

import com.android.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * {@link Node} that represents a property restrict.
 *
 * <p>A property restrict is an expression in the query language that allows a querier to restrict
 * the results of a query expression to those contained in a given property path. Written as a query
 * string, this node should be equivalent to the query `property:child`, where `property` is the
 * property path to restrict results to and `child` is the query subexpression.
 *
 * <p>This node is a comparator that should correspond with HAS in the <a
 * href="https://google.aip.dev/assets/misc/ebnf-filtering.txt">Google AIP EBNF Filtering
 * Definition</a>.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class PropertyRestrictNode implements Node {
    private PropertyPath mPropertyPath;
    private final List<Node> mChildren = new ArrayList<>(1);

    /**
     * Constructor for building a {@link PropertyRestrictNode} that represents a restriction on a
     * query subexpression by some property i.e. the query `property:subexpression`.
     *
     * @param propertyPath The property that will restrict results returned by the subexpression in
     *     the property restrict
     * @param childNode The subexpression to be restricted in the property restrict
     */
    public PropertyRestrictNode(@NonNull PropertyPath propertyPath, @NonNull Node childNode) {
        mPropertyPath = Objects.requireNonNull(propertyPath);
        mChildren.add(Objects.requireNonNull(childNode));
    }

    /**
     * Get the {@link PropertyPath} in the property restriction (i.e. the left hand side of the
     * property restrict sign (":")).
     */
    public @NonNull PropertyPath getPropertyPath() {
        return mPropertyPath;
    }

    /**
     * Get the child {@link Node} of {@link PropertyRestrictNode} as a list containing the only
     * child {@link Node}.
     */
    @Override
    public @NonNull List<Node> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /**
     * Get the subexpression in the property restriction as a {@link Node} (i.e. the right hand side
     * of the property restrict sign (":")).
     */
    public @NonNull Node getChild() {
        return mChildren.get(0);
    }

    /**
     * Set the {@link PropertyPath} in the property restriction (i.e. the left hand side of the
     * property restrict sign (":")).
     */
    public void setPropertyPath(@NonNull PropertyPath propertyPath) {
        mPropertyPath = Objects.requireNonNull(propertyPath);
    }

    /**
     * Set the query subexpression in the property restriction (i.e. the right hand side of the
     * property restrict sign (":")).
     */
    public void setChild(@NonNull Node childNode) {
        mChildren.set(0, Objects.requireNonNull(childNode));
    }

    /**
     * Get the query string representation of {@link PropertyRestrictNode}.
     *
     * <p>The string representation is the string representation of the property path joined from
     * the left to the query sub expression surrounded in parentheses with the property restrict
     * symbol (":").
     */
    @Override
    public @NonNull String toString() {
        return "(" + mPropertyPath + ":" + getChild() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyRestrictNode)) return false;
        PropertyRestrictNode that = (PropertyRestrictNode) o;
        return Objects.equals(mPropertyPath, that.mPropertyPath)
                && Objects.equals(mChildren, that.mChildren);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPropertyPath, mChildren);
    }
}
