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
package com.android.internal.widget.remotecompose.core.operations.layout.utils;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.ArrayList;

/** Internal utility debug class */
public class DebugLog {

    public static final boolean DEBUG_LAYOUT_ON = false;

    public static class Node {
        @Nullable public Node parent;
        @NonNull public String name;
        @NonNull public String endString;
        @NonNull public ArrayList<Node> list = new ArrayList<>();

        public Node(@Nullable Node parent, @NonNull String name) {
            this.parent = parent;
            this.name = name;
            this.endString = name + " DONE";
            if (parent != null) {
                parent.add(this);
            }
        }

        /**
         * Add a node to the current node
         *
         * @param node
         */
        public void add(@NonNull Node node) {
            list.add(node);
        }
    }

    public static class LogNode extends Node {
        public LogNode(@Nullable Node parent, @NonNull String name) {
            super(parent, name);
        }
    }

    @NonNull public static Node node = new Node(null, "Root");
    @NonNull public static Node currentNode = node;

    /** clear the current logging */
    public static void clear() {
        node = new Node(null, "Root");
        currentNode = node;
    }

    /**
     * start a node
     *
     * @param valueSupplier
     */
    public static void s(@NonNull StringValueSupplier valueSupplier) {
        if (DEBUG_LAYOUT_ON) {
            currentNode = new Node(currentNode, valueSupplier.getString());
        }
    }

    /**
     * arbitrary log statement
     *
     * @param valueSupplier
     */
    public static void log(@NonNull StringValueSupplier valueSupplier) {
        if (DEBUG_LAYOUT_ON) {
            new LogNode(currentNode, valueSupplier.getString());
        }
    }

    /** end a node */
    public static void e() {
        if (DEBUG_LAYOUT_ON) {
            if (currentNode.parent != null) {
                currentNode = currentNode.parent;
            } else {
                currentNode = node;
            }
        }
    }

    /**
     * end a node
     *
     * @param valueSupplier
     */
    public static void e(@NonNull StringValueSupplier valueSupplier) {
        if (DEBUG_LAYOUT_ON) {
            currentNode.endString = valueSupplier.getString();
            if (currentNode.parent != null) {
                currentNode = currentNode.parent;
            } else {
                currentNode = node;
            }
        }
    }

    /**
     * print a given node
     *
     * @param indent
     * @param node
     * @param builder
     */
    public static void printNode(int indent, @NonNull Node node, @NonNull StringBuilder builder) {
        if (DEBUG_LAYOUT_ON) {
            StringBuilder indentationBuilder = new StringBuilder();
            for (int i = 0; i < indent; i++) {
                indentationBuilder.append("| ");
            }
            String indentation = indentationBuilder.toString();

            if (node.list.size() > 0) {
                builder.append(indentation).append(node.name).append("\n");
                for (Node c : node.list) {
                    printNode(indent + 1, c, builder);
                }
                builder.append(indentation).append(node.endString).append("\n");
            } else {
                if (node instanceof LogNode) {
                    builder.append(indentation).append("     ").append(node.name).append("\n");
                } else {
                    builder.append(indentation)
                            .append("-- ")
                            .append(node.name)
                            .append(" : ")
                            .append(node.endString)
                            .append("\n");
                }
            }
        }
    }

    /** Output the captured log to System.out */
    public static void display() {
        if (DEBUG_LAYOUT_ON) {
            StringBuilder builder = new StringBuilder();
            printNode(0, node, builder);
            System.out.println("\n" + builder.toString());
        }
    }
}
