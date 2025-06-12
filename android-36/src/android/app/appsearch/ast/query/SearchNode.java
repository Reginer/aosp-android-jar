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
import android.app.appsearch.ast.Node;

import com.android.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * {@link FunctionNode} that represents the search function.
 *
 * <p>The search function is a convenience function that takes a query string and parses it
 * according to the supported query language, and can optionally take a list of property paths to
 * serve as property restricts. This means that the query `search("foo bar", createList("subject",
 * body")` is equivalent to the query `(subject:foo OR body:foo) (subject:bar OR body:bar)`.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class SearchNode implements FunctionNode {
    private final List<Node> mChildren = new ArrayList<>(1);
    private final List<PropertyPath> mPropertyPaths;

    /**
     * Create a {@link SearchNode} representing the query function `search(queryString,
     * createList(listOfProperties)`.
     *
     * @param childNode The query to search for represented as a {@link Node}.
     * @param propertyPaths A list of property paths to restrict results from the query. If the list
     *     is empty, all results from the query will be returned.
     */
    public SearchNode(@NonNull Node childNode, @NonNull List<PropertyPath> propertyPaths) {
        Objects.requireNonNull(childNode);
        Objects.requireNonNull(propertyPaths);
        for (int i = 0; i < propertyPaths.size(); i++) {
            Objects.requireNonNull(propertyPaths.get(i));
        }
        mChildren.add(childNode);
        mPropertyPaths = new ArrayList<>(propertyPaths);
    }

    /**
     * Create a {@link SearchNode} representing the query function `search(queryString)`.
     *
     * <p>By default, the query function search will have an empty list of restricts. The search
     * function will return all results from the query.
     *
     * @param childNode The query to search for represented as a {@link Node}.
     */
    public SearchNode(@NonNull Node childNode) {
        this(childNode, Collections.emptyList());
    }

    /** Returns the name of the function represented by {@link SearchNode}. */
    @Override
    @FunctionName
    public @NonNull String getFunctionName() {
        return FUNCTION_NAME_SEARCH;
    }

    /**
     * Returns the child {@link Node} of {@link SearchNode} as a list containing the only child
     * {@link Node}.
     */
    @Override
    public @NonNull List<Node> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /** Returns the child query searched for in the function. */
    public @NonNull Node getChild() {
        return mChildren.get(0);
    }

    /**
     * Returns the list of property restricts applied to the query. If the list is empty, there are
     * no property restricts, which means that `search` will return all results from the query.
     */
    public @NonNull List<PropertyPath> getPropertyPaths() {
        return Collections.unmodifiableList(mPropertyPaths);
    }

    /** Sets the query searched for in the function. */
    public void setChild(@NonNull Node childNode) {
        mChildren.set(0, Objects.requireNonNull(childNode));
    }

    /** Sets what property restricts will be applied to the query. */
    public void setPropertyPaths(@NonNull List<PropertyPath> properties) {
        Objects.requireNonNull(properties);
        for (int i = 0; i < properties.size(); i++) {
            Objects.requireNonNull(properties.get(i));
        }
        mPropertyPaths.clear();
        mPropertyPaths.addAll(properties);
    }

    /** Add a restrict to the end of the current list of restricts {@link #mPropertyPaths}. */
    public void addPropertyPath(@NonNull PropertyPath propertyPath) {
        mPropertyPaths.add(Objects.requireNonNull(propertyPath));
    }

    /**
     * Get the query string representation of {@link SearchNode}.
     *
     * <p>If there are no property restricts, then the string representation is the function name
     * followed by the string representation of the child subquery as a string literal, surrounded
     * by parentheses. For example the node represented by
     *
     * <pre>{@code
     * TextNode node = new TextNode("foo");
     * SearchNode searchNode = new SearchNode(node);
     * }</pre>
     *
     * will be represented by the query string `search("(foo)")`.
     *
     * <p>If there are property restricts, i.e. {@link #getPropertyPaths()} is not empty, then in
     * addition to the string representation of the child subquery, the property restricts will be
     * represented as inputs to the {@code createList} function, which itself will be an input. So
     * for the node represented by
     *
     * <pre>{@code
     * List<PropertyPath> propertyPaths = List.of(new PropertyPath("example.path"),
     *                                            new PropertyPath("anotherPath"));
     * TextNode node = new TextNode("foo");
     * SearchNode searchNode = new SearchNode(node, propertyPaths);
     * }</pre>
     *
     * the query string will be `search("(foo)", createList("example.path", "anotherPath"))`.
     *
     * <p>Operators in the query string are supported. As such additional escaping are applied to
     * ensure that operators stay scoped to the search node. This applies recursively, so if we had
     * three layers of search i.e. a search function that takes a query containing a nested search,
     * we would apply three levels of escaping. So for the node represented by
     *
     * <pre>{@code
     * TextNode node = new TextNode("foo");
     * node.setVerbatim(true);
     * SearchNode nestedSearchNode = new SearchNode(node);
     * SearchNode searchNode = new SearchNode(nestedSearchNode);
     * }</pre>
     *
     * the query string of {@code searchNode} will be `search("search(\"(\\\"foo\\\")\")")`
     */
    @Override
    public @NonNull String toString() {
        StringBuilder builder = new StringBuilder(FunctionNode.FUNCTION_NAME_SEARCH);
        builder.append("(\"");
        builder.append(escapeQuery(getChild().toString()));
        builder.append("\"");
        if (!mPropertyPaths.isEmpty()) {
            builder.append(", createList(");
            for (int i = 0; i < mPropertyPaths.size() - 1; i++) {
                builder.append("\"");
                builder.append(mPropertyPaths.get(i));
                builder.append("\", ");
            }
            builder.append("\"");
            builder.append(mPropertyPaths.get(mPropertyPaths.size() - 1));
            builder.append("\")");
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * Escapes queries passed into {@link SearchNode}. Queries are assumed to be already escaped,
     * but need additional escaping if they are an input of {@link SearchNode}.
     */
    private String escapeQuery(String strLiteral) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < strLiteral.length(); i++) {
            // We want to add an escape character if:
            // 1. There is a quote character ('"')
            // 2. There is an escape character ('\')
            // It is ok to add two escape characters for escaped quote characters ('\"') because if
            // we to unescape we need to unescape both the original escape character and the quote
            // character.
            if (strLiteral.charAt(i) == '"' || strLiteral.charAt(i) == '\\') {
                stringBuilder.append('\\');
            }
            stringBuilder.append(strLiteral.charAt(i));
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchNode that = (SearchNode) o;
        return Objects.equals(mChildren, that.mChildren)
                && Objects.equals(mPropertyPaths, that.mPropertyPaths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mChildren, mPropertyPaths);
    }
}
