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
import android.annotation.IntDef;
import android.app.appsearch.PropertyPath;
import android.app.appsearch.ast.Node;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * {@link Node} that represents a numeric search expression between a property and a numeric value.
 *
 * <p>All numeric search expressions are represented by this {@link Node} by passing in a {@link
 * Comparator} that represent one of the comparator operators available in the query language, a
 * {@link PropertyPath} representing the property, and a numeric value to compare the property
 * against.
 *
 * <p>This node represents comparators as defined in the <a
 * href="https://google.aip.dev/assets/misc/ebnf-filtering.txt">Google AIP EBNF Filtering
 * Definition</a>.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class ComparatorNode implements Node {
    /**
     * Enums representing different comparators for numeric search expressions in the query
     * language.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                EQUALS,
                LESS_THAN,
                LESS_EQUALS,
                GREATER_THAN,
                GREATER_EQUALS,
            })
    public @interface Comparator {}

    public static final int EQUALS = 0;
    public static final int LESS_THAN = 1;
    public static final int LESS_EQUALS = 2;
    public static final int GREATER_THAN = 3;
    public static final int GREATER_EQUALS = 4;

    private static final int MAX_COMPARATOR_VALUE = GREATER_EQUALS;

    private @Comparator int mComparator;
    private PropertyPath mPropertyPath;
    private long mValue;

    /**
     * Construct a {@link Node} representing a numeric search expression between a property and a
     * numeric value.
     *
     * @param comparator An {@code IntDef} representing what comparison is being made.
     * @param propertyPath A {@link PropertyPath} that is property being compared i.e. the left hand
     *     side of the comparison.
     * @param value The numeric value being compared i.e. the right hand side of the comparison.
     */
    public ComparatorNode(
            @Comparator int comparator, @NonNull PropertyPath propertyPath, long value) {
        Preconditions.checkArgumentInRange(
                comparator, EQUALS, MAX_COMPARATOR_VALUE, "Comparator intDef");
        mComparator = comparator;
        mPropertyPath = Objects.requireNonNull(propertyPath);
        mValue = value;
    }

    /** Get the {@code @Comparator} used in the comparison. */
    @Comparator
    public int getComparator() {
        return mComparator;
    }

    /**
     * Get the {@code PropertyPath} being compared.
     *
     * <p>I.e. left hand side of the comparison represented by this node.
     */
    public @NonNull PropertyPath getPropertyPath() {
        return mPropertyPath;
    }

    /**
     * Get the numeric value being compared.
     *
     * <p>I.e. the right hand side of the comparison represented by this node.
     */
    public long getValue() {
        return mValue;
    }

    /** Set the {@code @Comparator} being used to compare the {@code PropertyPath} and value. */
    public void setComparator(@Comparator int comparator) {
        Preconditions.checkArgumentInRange(
                comparator, EQUALS, MAX_COMPARATOR_VALUE, "Comparator intDef");
        mComparator = comparator;
    }

    /** Set the {@code PropertyPath} being compared, i.e. the left side of the comparison. */
    public void setPropertyPath(@NonNull PropertyPath propertyPath) {
        mPropertyPath = Objects.requireNonNull(propertyPath);
    }

    /** Set the numeric value being compared, i.e. the right side of the comparison. */
    public void setValue(long value) {
        mValue = value;
    }

    /**
     * Get the query string representation of {@link ComparatorNode}.
     *
     * <p>The string representation is the string representation of the property path joined from
     * the left to the value being compared with the string representation of the {@link
     * Comparator}.
     */
    @Override
    public @NonNull String toString() {
        String comparatorString = "";
        switch (mComparator) {
            case ComparatorNode.EQUALS:
                comparatorString = "==";
                break;
            case ComparatorNode.LESS_THAN:
                comparatorString = "<";
                break;
            case ComparatorNode.LESS_EQUALS:
                comparatorString = "<=";
                break;
            case ComparatorNode.GREATER_THAN:
                comparatorString = ">";
                break;
            case ComparatorNode.GREATER_EQUALS:
                comparatorString = ">=";
        }
        // Equivalent in behavior but more efficient than
        // String.format("(%s %s %s)", mPropertyPath, comparatorString, mValue);
        return "(" + mPropertyPath + " " + comparatorString + " " + mValue + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComparatorNode)) return false;
        ComparatorNode that = (ComparatorNode) o;
        return mComparator == that.mComparator
                && mValue == that.mValue
                && Objects.equals(mPropertyPath, that.mPropertyPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mComparator, mPropertyPath, mValue);
    }
}
