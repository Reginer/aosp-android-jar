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
import android.app.appsearch.SearchSpec;
import android.app.appsearch.ast.FunctionNode;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * {@link FunctionNode} that represents the semanticSearch function.
 *
 * <p>The semanticSearch function matches all documents that have at least one embedding vector with
 * a matching model signature (see {@link
 * android.app.appsearch.EmbeddingVector#getModelSignature()}) and a similarity score within the
 * range specified. The similarity score is calculated by determining the distance between the
 * document embedding vector with a matching model signature and the embedding vector indexed at the
 * list of vectors returned by {@link SearchSpec#getEmbeddingParameters()}. How this distance is
 * defined is based on what distance metric set.
 *
 * <p>This node can be used to build a query that contains the semanticSearch function. For example,
 * the node {@code SemanticSearchNode(0, -0.5, 0.5, DOT_PRODUCT)} is equivalent to the query
 * `semanticSearch(getEmbeddingParameter(0), -0.5, 0.5, "DOT_PRODUCT")`.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class SemanticSearchNode implements FunctionNode {
    private int mVectorIndex;
    private float mLowerBound;
    private float mUpperBound;
    private @SearchSpec.EmbeddingSearchMetricType int mDistanceMetric;

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * @param vectorIndex The index of the embedding vector in the list of vectors returned by
     *     {@link SearchSpec#getEmbeddingParameters()} to use in the search.
     * @param lowerBound The lower bound on similarity score for a embedding vector such that the
     *     associated document will be returned.
     * @param upperBound The upper bound on similarity score for a embedding vector such that the
     *     associated document will be returned.
     * @param distanceMetric How distance between embedding vectors will be calculated.
     */
    public SemanticSearchNode(
            int vectorIndex,
            float lowerBound,
            float upperBound,
            @SearchSpec.EmbeddingSearchMetricType int distanceMetric) {
        Preconditions.checkArgument(vectorIndex >= 0, "Vector index must be non-negative.");
        Preconditions.checkArgument(
                lowerBound <= upperBound,
                "Provided lower bound must be less than or equal to"
                        + " the provided upper bound.");
        Preconditions.checkArgumentInRange(
                distanceMetric,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN,
                "Embedding search metric type");
        mVectorIndex = vectorIndex;
        mLowerBound = lowerBound;
        mUpperBound = upperBound;
        mDistanceMetric = distanceMetric;
    }

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * <p>By default:
     *
     * <ul>
     *   <li>The default set by the user and returned by {@link
     *       SearchSpec#getDefaultEmbeddingSearchMetricType()} will be used to determine similarity
     *       between embedding vectors. If no default is set, cosine similarity will be used.
     * </ul>
     *
     * <p>See {@link #SemanticSearchNode(int, float, float, int)} for an explanation of the
     * parameters.
     */
    public SemanticSearchNode(int vectorIndex, float lowerBound, float upperBound) {
        this(vectorIndex, lowerBound, upperBound, SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT);
    }

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * <p>By default:
     *
     * <ul>
     *   <li>The default set by the user and returned by {@link
     *       SearchSpec#getDefaultEmbeddingSearchMetricType()} will be used to determine similarity
     *       between embedding vectors. If no default is set, cosine similarity will be used.
     *   <li>The upper bound on similarity scores for an embedding vector such that the associated
     *       document will be returned is positive infinity.
     * </ul>
     *
     * <p>See {@link #SemanticSearchNode(int, float, float, int)} for an explanation of the
     * parameters.
     */
    public SemanticSearchNode(int vectorIndex, float lowerBound) {
        this(vectorIndex, lowerBound, Float.POSITIVE_INFINITY);
    }

    /**
     * Constructor for {@link SemanticSearchNode} representing the semanticSearch function in a
     * query.
     *
     * <p>By default:
     *
     * <ul>
     *   <li>The default set by the user and returned by {@link
     *       SearchSpec#getDefaultEmbeddingSearchMetricType()} will be used to determine similarity
     *       between embedding vectors. If no default is set, cosine similarity will be used.
     *   <li>The upper bound on similarity scores for an embedding vector such that the associated
     *       document will be returned is positive infinity.
     *   <li>The lower bound on similarity scores for an embedding vector such that the associated
     *       document will be returned is negative infinity.
     * </ul>
     *
     * <p>See {@link #SemanticSearchNode(int, float, float, int)} for an explanation of the
     * parameters.
     */
    public SemanticSearchNode(int vectorIndex) {
        this(vectorIndex, Float.NEGATIVE_INFINITY);
    }

    /** Returns the name of the function represented by {@link SemanticSearchNode}. */
    @Override
    @FunctionName
    public @NonNull String getFunctionName() {
        return FUNCTION_NAME_SEMANTIC_SEARCH;
    }

    /**
     * Returns the index of the embedding vector to be retrieved from the list of embedding vectors
     * returned by {@link SearchSpec#getEmbeddingParameters()}.
     */
    public int getVectorIndex() {
        return mVectorIndex;
    }

    /** Returns the lower bound of the range of values similarity scores must fall in. */
    public float getLowerBound() {
        return mLowerBound;
    }

    /** Returns the upper bound of the range of values similarity scores must fall in. */
    public float getUpperBound() {
        return mUpperBound;
    }

    /** Returns the distance metric used to calculated similarity between embedding vectors. */
    @SearchSpec.EmbeddingSearchMetricType
    public int getDistanceMetric() {
        return mDistanceMetric;
    }

    /** Sets the index of the embedding vector that semanticSearch will use. */
    public void setVectorIndex(int vectorIndex) {
        Preconditions.checkArgument(vectorIndex >= 0, "Vector Index must be non-negative.");
        mVectorIndex = vectorIndex;
    }

    /**
     * Sets the bounds of the range of values that semanticSearch will search against.
     *
     * @param lowerBound The lower bound of the range of values.
     * @param upperBound The upper bound of the range of values.
     */
    public void setBounds(float lowerBound, float upperBound) {
        Preconditions.checkArgument(
                lowerBound <= upperBound,
                "Provided lower bound must be less than or equal to" + " the provided upper bound");
        mLowerBound = lowerBound;
        mUpperBound = upperBound;
    }

    /**
     * Sets how similarity is calculated between embedding vectors.
     *
     * @param distanceMetric How similarity is calculated between embedding vectors.
     */
    public void setDistanceMetric(@SearchSpec.EmbeddingSearchMetricType int distanceMetric) {
        Preconditions.checkArgumentInRange(
                distanceMetric,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT,
                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN,
                "Embedding search metric type");
        mDistanceMetric = distanceMetric;
    }

    /**
     * Get the query string representation of {@link SemanticSearchNode}.
     *
     * <p>The query string representation will be the function name, followed by the fields of
     * {@link SemanticSearchNode} as arguments, surrounded by parentheses, but formatted in the
     * following way:
     *
     * <ul>
     *   <li>The vector index will appear as an argument to the function `getEmbeddingParameter`.
     *   <li>The lower bound and upper bound will appear unchanged.
     *   <li>The distance metric will be mapped to its corresponding string literal representation.
     *       For example, if the distance metric is 1, then the corresponding string literal would
     *       be {@code "COSINE"}.
     * </ul>
     *
     * For example, the node {@code SemanticSearchNode(0, -1.5f, 2,
     * SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE)} will look like
     * `semanticSearch(getEmbeddingParameter(0), -1.5, 2, "COSINE")`
     *
     * <p>If possible, default parameters will be left out of the query string. For example the node
     * {@code SemanticSearchNode(0)} will look like `semanticSearch(getEmbeddingParameter(0))`.
     * However if some defaults are set and unset, the defaults will be included in the query
     * string. For example, if the user does something like this:
     *
     * <pre>{@code
     * SemanticSearchNode semanticSearchNode = new SemanticSearchNode(0, -1, 1);
     * semanticSearchNode.setBounds(Float.NEGATIVE_INFINITY, 1);
     * }</pre>
     *
     * Then the query string will look like `semanticSearch(getEmbeddingParameter(0),
     * -Float.MAX_VALUE, 1)` where {@code Float.MAX_VALUE} is the max value of float.
     */
    @Override
    public @NonNull String toString() {
        StringBuilder builder = new StringBuilder(FunctionNode.FUNCTION_NAME_SEMANTIC_SEARCH);
        builder.append("(getEmbeddingParameter(");
        builder.append(mVectorIndex);
        builder.append(")");
        if (mDistanceMetric != SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT) {
            // String will look like
            // semanticSearch(vectorIndex, lowerBound, upperBound, distanceMetric)
            formatBound(builder, mLowerBound);
            formatBound(builder, mUpperBound);
            builder.append(", ");
            builder.append(getEmbeddingMetricString());
        } else if (mUpperBound != Float.POSITIVE_INFINITY) {
            // String will look like semanticSearch(vectorIndex, lowerBound, upperBound)
            formatBound(builder, mLowerBound);
            formatBound(builder, mUpperBound);
        } else if (mLowerBound != Float.NEGATIVE_INFINITY) {
            // String will look like semanticSearch(vectorIndex, lowerBound)
            formatBound(builder, mLowerBound);
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * Formats the bounds for semantic search query strings. If the bound is finite, no formatting
     * is done. If the bound is infinite, the max value of float will be returned.
     */
    private void formatBound(StringBuilder builder, float bound) {
        builder.append(", ");
        if (Float.isFinite(bound)) {
            builder.append(bound);
        } else if (bound == Float.NEGATIVE_INFINITY) {
            builder.append(-Float.MAX_VALUE);
        } else {
            builder.append(Float.MAX_VALUE);
        }
    }

    /**
     * Returns the name of the embedding metric that the {@link SemanticSearchNode} is using to
     * calculate similarity.
     */
    private String getEmbeddingMetricString() {
        switch (mDistanceMetric) {
            case SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE:
                return "\"COSINE\"";
            case SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT:
                return "\"DOT_PRODUCT\"";
            case SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN:
                return "\"EUCLIDEAN\"";
        }
        throw new IllegalStateException("Invalid Metric Type");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticSearchNode that = (SemanticSearchNode) o;
        return mVectorIndex == that.mVectorIndex
                && Float.compare(mLowerBound, that.mLowerBound) == 0
                && Float.compare(mUpperBound, that.mUpperBound) == 0
                && mDistanceMetric == that.mDistanceMetric;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mVectorIndex, mLowerBound, mUpperBound, mDistanceMetric);
    }
}
