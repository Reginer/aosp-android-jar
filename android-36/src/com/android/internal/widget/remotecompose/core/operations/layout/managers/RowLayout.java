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

import static com.android.internal.widget.remotecompose.core.documentation.DocumentedOperation.FLOAT;
import static com.android.internal.widget.remotecompose.core.documentation.DocumentedOperation.INT;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.internal.widget.remotecompose.core.Operation;
import com.android.internal.widget.remotecompose.core.Operations;
import com.android.internal.widget.remotecompose.core.PaintContext;
import com.android.internal.widget.remotecompose.core.RemoteContext;
import com.android.internal.widget.remotecompose.core.WireBuffer;
import com.android.internal.widget.remotecompose.core.documentation.DocumentationBuilder;
import com.android.internal.widget.remotecompose.core.operations.layout.Component;
import com.android.internal.widget.remotecompose.core.operations.layout.LayoutComponent;
import com.android.internal.widget.remotecompose.core.operations.layout.measure.ComponentMeasure;
import com.android.internal.widget.remotecompose.core.operations.layout.measure.MeasurePass;
import com.android.internal.widget.remotecompose.core.operations.layout.measure.Size;
import com.android.internal.widget.remotecompose.core.operations.layout.modifiers.ScrollModifierOperation;
import com.android.internal.widget.remotecompose.core.operations.layout.modifiers.WidthInModifierOperation;
import com.android.internal.widget.remotecompose.core.operations.layout.utils.DebugLog;
import com.android.internal.widget.remotecompose.core.serialize.MapSerializer;

import java.util.List;

/** Simple Row layout implementation - also supports weight and horizontal/vertical positioning */
public class RowLayout extends LayoutManager {
    public static final int START = 1;
    public static final int CENTER = 2;
    public static final int END = 3;
    public static final int TOP = 4;
    public static final int BOTTOM = 5;
    public static final int SPACE_BETWEEN = 6;
    public static final int SPACE_EVENLY = 7;
    public static final int SPACE_AROUND = 8;

    int mHorizontalPositioning;
    int mVerticalPositioning;
    float mSpacedBy = 0f;

    public RowLayout(
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
        super(parent, componentId, animationId, x, y, width, height);
        mHorizontalPositioning = horizontalPositioning;
        mVerticalPositioning = verticalPositioning;
        mSpacedBy = spacedBy;
    }

    public RowLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        this(
                parent,
                componentId,
                animationId,
                0,
                0,
                0,
                0,
                horizontalPositioning,
                verticalPositioning,
                spacedBy);
    }

    @NonNull
    @Override
    public String toString() {
        return getSerializedName()
                + " ["
                + mComponentId
                + ":"
                + mAnimationId
                + "] ("
                + mX
                + ", "
                + mY
                + " - "
                + mWidth
                + " x "
                + mHeight
                + ") "
                + mVisibility;
    }

    @NonNull
    @Override
    protected String getSerializedName() {
        return "ROW";
    }

    @Override
    public boolean isInHorizontalFill() {
        return super.isInHorizontalFill() || childrenHaveHorizontalWeights();
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
        DebugLog.s(() -> "COMPUTE WRAP SIZE in " + this + " (" + mComponentId + ")");
        int visibleChildrens = 0;
        float currentMaxWidth = maxWidth;
        for (Component c : mChildrenComponents) {
            c.measure(context, 0f, currentMaxWidth, 0f, maxHeight, measure);
            ComponentMeasure m = measure.get(c);
            if (!m.isGone()) {
                size.setWidth(size.getWidth() + m.getW());
                size.setHeight(Math.max(size.getHeight(), m.getH()));
                visibleChildrens++;
                currentMaxWidth -= m.getW();
            }
        }
        if (!mChildrenComponents.isEmpty()) {
            size.setWidth(size.getWidth() + (mSpacedBy * (visibleChildrens - 1)));
        }
        DebugLog.e();
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        DebugLog.s(() -> "COMPUTE SIZE in " + this + " (" + mComponentId + ")");
        float mw = maxWidth;
        for (Component child : mChildrenComponents) {
            child.measure(context, minWidth, mw, minHeight, maxHeight, measure);
            ComponentMeasure m = measure.get(child);
            if (!m.isGone()) {
                mw -= m.getW();
            }
        }
        DebugLog.e();
    }

    @Override
    public float minIntrinsicWidth(@Nullable RemoteContext context) {
        float width = computeModifierDefinedWidth(context);
        float componentWidths = 0f;
        for (Component c : mChildrenComponents) {
            componentWidths += c.minIntrinsicWidth(context);
        }
        return Math.max(width, componentWidths);
    }

    @Override
    public float minIntrinsicHeight(@Nullable RemoteContext context) {
        float height = computeModifierDefinedHeight(context);
        float componentHeights = 0f;
        for (Component c : mChildrenComponents) {
            componentHeights = Math.max(componentHeights, c.minIntrinsicHeight(context));
        }
        return Math.max(height, componentHeights);
    }

    @Override
    public void internalLayoutMeasure(@NonNull PaintContext context, @NonNull MeasurePass measure) {
        ComponentMeasure selfMeasure = measure.get(this);
        DebugLog.s(
                () ->
                        "INTERNAL LAYOUT "
                                + this
                                + " ("
                                + mComponentId
                                + ") children: "
                                + mChildrenComponents.size()
                                + " size ("
                                + selfMeasure.getW()
                                + " x "
                                + selfMeasure.getH()
                                + ")");
        if (mChildrenComponents.isEmpty()) {
            DebugLog.e();
            return;
        }
        float selfWidth = selfMeasure.getW() - mPaddingLeft - mPaddingRight;
        float selfHeight = selfMeasure.getH() - mPaddingTop - mPaddingBottom;
        float childrenWidth = 0f;
        float childrenHeight = 0f;

        if (mComponentModifiers.hasHorizontalScroll()) {
            selfWidth =
                    mComponentModifiers.getHorizontalScrollDimension()
                            - mPaddingLeft
                            - mPaddingRight;
        }
        if (mComponentModifiers.hasVerticalScroll()) {
            selfHeight =
                    mComponentModifiers.getVerticalScrollDimension() - mPaddingTop - mPaddingBottom;
        }

        boolean checkWeights = true;

        while (checkWeights) {
            checkWeights = false;
            childrenWidth = 0f;
            childrenHeight = 0f;
            boolean hasWeights = false;
            float totalWeights = 0f;
            for (Component child : mChildrenComponents) {
                ComponentMeasure childMeasure = measure.get(child);
                if (childMeasure.isGone()) {
                    continue;
                }
                if (child instanceof LayoutComponent
                        && ((LayoutComponent) child).getWidthModifier().hasWeight()) {
                    hasWeights = true;
                    totalWeights += ((LayoutComponent) child).getWidthModifier().getValue();
                } else {
                    childrenWidth += childMeasure.getW();
                }
            }

            // TODO: need to move the weight measuring in the measure function,
            // currently we'll measure unnecessarily
            if (hasWeights) {
                float availableSpace = selfWidth - childrenWidth;
                for (Component child : mChildrenComponents) {
                    if (child instanceof LayoutComponent
                            && ((LayoutComponent) child).getWidthModifier().hasWeight()) {
                        ComponentMeasure childMeasure = measure.get(child);
                        if (childMeasure.isGone()) {
                            continue;
                        }
                        float weight = ((LayoutComponent) child).getWidthModifier().getValue();
                        float childWidth = (weight * availableSpace) / totalWeights;
                        WidthInModifierOperation widthInConstraints =
                                ((LayoutComponent) child).getWidthModifier().getWidthIn();
                        if (widthInConstraints != null) {
                            float min = widthInConstraints.getMin();
                            float max = widthInConstraints.getMax();
                            if (min != -1) {
                                childWidth = Math.max(min, childWidth);
                            }
                            if (max != -1) {
                                childWidth = Math.min(max, childWidth);
                            }
                        }
                        childMeasure.setW(childWidth);
                        child.measure(
                                context,
                                childMeasure.getW(),
                                childMeasure.getW(),
                                childMeasure.getH(),
                                childMeasure.getH(),
                                measure);
                    }
                }
            }

            if (applyVisibility(selfWidth, selfHeight, measure) && hasWeights) {
                checkWeights = true;
            }
        }

        childrenWidth = 0f;
        int visibleChildrens = 0;
        for (Component child : mChildrenComponents) {
            ComponentMeasure childMeasure = measure.get(child);
            if (childMeasure.isGone()) {
                continue;
            }
            childrenWidth += childMeasure.getW();
            childrenHeight = Math.max(childrenHeight, childMeasure.getH());
            visibleChildrens++;
        }
        childrenWidth += mSpacedBy * (visibleChildrens - 1);

        float tx = 0f;
        float ty = 0f;

        float horizontalGap = 0f;
        float total = 0f;

        switch (mHorizontalPositioning) {
            case START:
                tx = 0f;
                break;
            case END:
                tx = selfWidth - childrenWidth;
                break;
            case CENTER:
                tx = (selfWidth - childrenWidth) / 2f;
                break;
            case SPACE_BETWEEN:
                for (Component child : mChildrenComponents) {
                    ComponentMeasure childMeasure = measure.get(child);
                    if (childMeasure.isGone()) {
                        continue;
                    }
                    total += childMeasure.getW();
                }
                if (visibleChildrens > 1) {
                    horizontalGap = (selfWidth - total) / (visibleChildrens - 1);
                } else {
                    // we center the element
                    tx = (selfWidth - childrenWidth) / 2f;
                }
                break;
            case SPACE_EVENLY:
                for (Component child : mChildrenComponents) {
                    ComponentMeasure childMeasure = measure.get(child);
                    if (childMeasure.isGone()) {
                        continue;
                    }
                    total += childMeasure.getW();
                }
                horizontalGap = (selfWidth - total) / (visibleChildrens + 1);
                tx = horizontalGap;
                break;
            case SPACE_AROUND:
                for (Component child : mChildrenComponents) {
                    ComponentMeasure childMeasure = measure.get(child);
                    if (childMeasure.isGone()) {
                        continue;
                    }
                    total += childMeasure.getW();
                }
                horizontalGap = (selfWidth - total) / visibleChildrens;
                tx = horizontalGap / 2f;
                break;
        }

        for (Component child : mChildrenComponents) {
            ComponentMeasure childMeasure = measure.get(child);
            switch (mVerticalPositioning) {
                case TOP:
                    ty = 0f;
                    break;
                case CENTER:
                    ty = (selfHeight - childMeasure.getH()) / 2f;
                    break;
                case BOTTOM:
                    ty = selfHeight - childMeasure.getH();
                    break;
            }
            childMeasure.setX(tx);
            childMeasure.setY(ty);
            if (childMeasure.isGone()) {
                continue;
            }
            tx += childMeasure.getW();
            if (mHorizontalPositioning == SPACE_BETWEEN
                    || mHorizontalPositioning == SPACE_AROUND
                    || mHorizontalPositioning == SPACE_EVENLY) {
                tx += horizontalGap;
            }
            tx += mSpacedBy;
        }
        DebugLog.e();
    }

    @Override
    public void getLocationInWindow(@NonNull float[] value, boolean forSelf) {
        super.getLocationInWindow(value, forSelf);

        if (!forSelf && mHorizontalScrollDelegate instanceof ScrollModifierOperation) {
            ScrollModifierOperation smo = (ScrollModifierOperation) mHorizontalScrollDelegate;

            value[0] += smo.getScrollX();
        }
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "RowLayout";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.LAYOUT_ROW;
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
                new RowLayout(
                        null,
                        componentId,
                        animationId,
                        horizontalPositioning,
                        verticalPositioning,
                        spacedBy));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", id(), name())
                .description(
                        "Row layout implementation, positioning components one"
                                + " after the other horizontally.\n\n"
                                + "It supports weight and horizontal/vertical positioning.")
                .examplesDimension(400, 100)
                .exampleImage("Start", "layout-RowLayout-start-top.png")
                .exampleImage("Center", "layout-RowLayout-center-top.png")
                .exampleImage("End", "layout-RowLayout-end-top.png")
                .exampleImage("SpaceEvenly", "layout-RowLayout-space-evenly-top.png")
                .exampleImage("SpaceAround", "layout-RowLayout-space-around-top.png")
                .exampleImage("SpaceBetween", "layout-RowLayout-space-between-top.png")
                .field(INT, "COMPONENT_ID", "unique id for this component")
                .field(
                        INT,
                        "ANIMATION_ID",
                        "id used to match components," + " for animation purposes")
                .field(INT, "HORIZONTAL_POSITIONING", "horizontal positioning value")
                .possibleValues("START", RowLayout.START)
                .possibleValues("CENTER", RowLayout.CENTER)
                .possibleValues("END", RowLayout.END)
                .possibleValues("SPACE_BETWEEN", RowLayout.SPACE_BETWEEN)
                .possibleValues("SPACE_EVENLY", RowLayout.SPACE_EVENLY)
                .possibleValues("SPACE_AROUND", RowLayout.SPACE_AROUND)
                .field(INT, "VERTICAL_POSITIONING", "vertical positioning value")
                .possibleValues("TOP", RowLayout.TOP)
                .possibleValues("CENTER", RowLayout.CENTER)
                .possibleValues("BOTTOM", RowLayout.BOTTOM)
                .field(FLOAT, "SPACED_BY", "Horizontal spacing between components");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mComponentId,
                mAnimationId,
                mHorizontalPositioning,
                mVerticalPositioning,
                mSpacedBy);
    }

    @Override
    public void serialize(MapSerializer serializer) {
        super.serialize(serializer);
        serializer.add("verticalPositioning", getPositioningString(mVerticalPositioning));
        serializer.add("horizontalPositioning", getPositioningString(mHorizontalPositioning));
        serializer.add("spacedBy", mSpacedBy);
    }

    private String getPositioningString(int pos) {
        switch (pos) {
            case START:
                return "START";
            case CENTER:
                return "CENTER";
            case END:
                return "END";
            case TOP:
                return "TOP";
            case BOTTOM:
                return "BOTTOM";
            case SPACE_BETWEEN:
                return "SPACE_BETWEEN";
            case SPACE_EVENLY:
                return "SPACE_EVENLY";
            case SPACE_AROUND:
                return "SPACE_AROUND";
            default:
                return "NONE";
        }
    }
}
