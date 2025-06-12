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

import com.android.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * This is the basic Abstract Syntax Tree (AST) class. All other classes extend from this class
 * depending on the specific node.
 *
 * <p>This API may change in response to feedback and additional changes.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public interface Node {
    /**
     * Get a list of the node's child {@link Node}s.
     *
     * <p>By default this method will return an empty list representing that the node has no child
     * nodes.
     *
     * <p>If a node type extends this interface and has child nodes, then that class should override
     * this implementation and return a list of nodes of size equal to the number of child nodes
     * that node has.
     *
     * @return An empty list of {@link Node} representing the child nodes.
     */
    default @NonNull List<Node> getChildren() {
        return Collections.emptyList();
    }
}
