/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.internal.widget.remotecompose.core.operations.layout.managers;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.internal.widget.remotecompose.core.Operation;
import com.android.internal.widget.remotecompose.core.Operations;
import com.android.internal.widget.remotecompose.core.PaintContext;
import com.android.internal.widget.remotecompose.core.RemoteContext;
import com.android.internal.widget.remotecompose.core.WireBuffer;
import com.android.internal.widget.remotecompose.core.operations.layout.Component;
import com.android.internal.widget.remotecompose.core.operations.layout.LayoutComponent;
import com.android.internal.widget.remotecompose.core.operations.layout.measure.ComponentMeasure;
import com.android.internal.widget.remotecompose.core.operations.layout.measure.MeasurePass;
import com.android.internal.widget.remotecompose.core.operations.layout.measure.Size;
import com.android.internal.widget.remotecompose.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;

import java.util.ArrayList;
import java.util.List;

public class CollapsibleRowLayout extends RowLayout {

    public CollapsibleRowLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        super(
                parent,
                componentId,
                animationId,
                x,
                y,
                width,
                height,
                horizontalPositioning,
                verticalPositioning,
                spacedBy);
    }

    public CollapsibleRowLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        super(
                parent,
                componentId,
                animationId,
                horizontalPositioning,
                verticalPositioning,
                spacedBy);
    }

    @NonNull
    @Override
    protected String getSerializedName() {
        return "COLLAPSIBLE_ROW";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.LAYOUT_COLLAPSIBLE_ROW;
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer wire buffer
     * @param componentId component id
     * @param animationId animation id (-1 if not set)
     * @param horizontalPositioning horizontal positioning rules
     * @param verticalPositioning vertical positioning rules
     * @param spacedBy spaced by value
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        buffer.start(id());
        buffer.writeInt(componentId);
        buffer.writeInt(animationId);
        buffer.writeInt(horizontalPositioning);
        buffer.writeInt(verticalPositioning);
        buffer.writeFloat(spacedBy);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int componentId = buffer.readInt();
        int animationId = buffer.readInt();
        int horizontalPositioning = buffer.readInt();
        int verticalPositioning = buffer.readInt();
        float spacedBy = buffer.readFloat();
        operations.add(
                new CollapsibleRowLayout(
                        null,
                        componentId,
                        animationId,
                        horizontalPositioning,
                        verticalPositioning,
                        spacedBy));
    }

    @Override
    public float minIntrinsicHeight(@NonNull RemoteContext context) {
        float height = computeModifierDefinedHeight(context);
        if (!mChildrenComponents.isEmpty()) {
            height += mChildrenComponents.get(0).minIntrinsicHeight(context);
        }
        return height;
    }

    @Override
    public float minIntrinsicWidth(@NonNull RemoteContext context) {
        float width = computeModifierDefinedWidth(context);
        if (!mChildrenComponents.isEmpty()) {
            width += mChildrenComponents.get(0).minIntrinsicWidth(context);
        }
        return width;
    }

    @Override
    public boolean hasHorizontalIntrinsicDimension() {
        return true;
    }

    @Override
    public void computeWrapSize(
            @NonNull PaintContext context,
            float maxWidth,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {
        computeVisibleChildren(
                context, maxWidth, maxHeight, horizontalWrap, verticalWrap, measure, size);
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        computeVisibleChildren(context, maxWidth, maxHeight, false, false, measure, null);
    }

    @Override
    public void internalLayoutMeasure(@NonNull PaintContext context, @NonNull MeasurePass measure) {
        // if needed, take care of weight calculations
        super.internalLayoutMeasure(context, measure);
        // Check again for visibility
        ComponentMeasure m = measure.get(this);
        computeVisibleChildren(context, m.getW(), m.getH(), false, false, measure, null);
    }

    private void computeVisibleChildren(
            @NonNull PaintContext context,
            float maxWidth,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @Nullable Size size) {
        int visibleChildren = 0;
        ComponentMeasure self = measure.get(this);
        self.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
        float currentMaxWidth = maxWidth;
        boolean hasPriorities = false;
        for (Component c : mChildrenComponents) {
            if (!measure.contains(c.getComponentId())) {
                // No need to remeasure here if already done
                if (c instanceof CollapsibleRowLayout) {
                    c.measure(context, 0f, currentMaxWidth, 0f, maxHeight, measure);
                } else {
                    c.measure(context, 0f, Float.MAX_VALUE, 0f, maxHeight, measure);
                }
            }
            ComponentMeasure m = measure.get(c);
            if (!m.isGone()) {
                if (size != null) {
                    size.setHeight(Math.max(size.getHeight(), m.getH()));
                    size.setWidth(size.getWidth() + m.getW());
                }
                visibleChildren++;
                currentMaxWidth -= m.getW();
            }
            if (c instanceof LayoutComponent) {
                LayoutComponent lc = (LayoutComponent) c;
                CollapsiblePriorityModifierOperation priority =
                        lc.selfOrModifier(CollapsiblePriorityModifierOperation.class);
                if (priority != null) {
                    hasPriorities = true;
                }
            }
        }
        if (!mChildrenComponents.isEmpty() && size != null) {
            size.setWidth(size.getWidth() + (mSpacedBy * (visibleChildren - 1)));
        }

        float childrenWidth = 0f;
        float childrenHeight = 0f;

        boolean overflow = false;
        ArrayList<Component> children = mChildrenComponents;
        if (hasPriorities) {
            // TODO: We need to cache this.
            children =
                    CollapsiblePriority.sortWithPriorities(
                            mChildrenComponents, CollapsiblePriority.HORIZONTAL);
        }
        for (Component child : children) {
            ComponentMeasure childMeasure = measure.get(child);
            if (overflow || childMeasure.isGone()) {
                childMeasure.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                continue;
            }
            float childWidth = childMeasure.getW();
            boolean childDoesNotFits = childrenWidth + childWidth > maxWidth;
            if (childDoesNotFits) {
                childMeasure.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                overflow = true;
            } else {
                childrenWidth += childWidth;
                childrenHeight = Math.max(childrenHeight, childMeasure.getH());
                visibleChildren++;
            }
        }
        if (horizontalWrap && size != null) {
            size.setWidth(Math.min(maxWidth, childrenWidth));
        }
        if (visibleChildren == 0 || (size != null && size.getWidth() <= 0f)) {
            self.addVisibilityOverride(Visibility.OVERRIDE_GONE);
        }
    }
}
