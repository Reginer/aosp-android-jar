/*
 * Copyright 2020 The Android Open Source Project
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

package android.app.appsearch;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.app.appsearch.util.BundleUtil;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents the specification logic for AppSearch. It can be used to set the type of
 * search, like prefix or exact only or apply filters to search for a specific schema type only etc.
 */
@SafeParcelable.Class(creator = "SearchSpecCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class SearchSpec extends AbstractSafeParcelable {

    /** Creator class for {@link SearchSpec}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<SearchSpec> CREATOR = new SearchSpecCreator();

    /**
     * Schema type to be used in {@link SearchSpec.Builder#addProjection} to apply property paths to
     * all results, excepting any types that have had their own, specific property paths set.
     *
     * @deprecated use {@link #SCHEMA_TYPE_WILDCARD} instead.
     */
    @Deprecated public static final String PROJECTION_SCHEMA_TYPE_WILDCARD = "*";

    /**
     * Schema type to be used in {@link SearchSpec.Builder#addFilterProperties(String, Collection)}
     * and {@link SearchSpec.Builder#addProjection} to apply property paths to all results,
     * excepting any types that have had their own, specific property paths set.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
    public static final String SCHEMA_TYPE_WILDCARD = "*";

    @Field(id = 1, getter = "getTermMatch")
    private final int mTermMatchType;

    @Field(id = 2, getter = "getFilterSchemas")
    private final List<String> mSchemas;

    @Field(id = 3, getter = "getFilterNamespaces")
    private final List<String> mNamespaces;

    @Field(id = 4)
    final Bundle mTypePropertyFilters;

    @Field(id = 5, getter = "getFilterPackageNames")
    private final List<String> mPackageNames;

    @Field(id = 6, getter = "getResultCountPerPage")
    private final int mResultCountPerPage;

    @Field(id = 7, getter = "getRankingStrategy")
    @RankingStrategy
    private final int mRankingStrategy;

    @Field(id = 8, getter = "getOrder")
    @Order
    private final int mOrder;

    @Field(id = 9, getter = "getSnippetCount")
    private final int mSnippetCount;

    @Field(id = 10, getter = "getSnippetCountPerProperty")
    private final int mSnippetCountPerProperty;

    @Field(id = 11, getter = "getMaxSnippetSize")
    private final int mMaxSnippetSize;

    @Field(id = 12)
    final Bundle mProjectionTypePropertyMasks;

    @Field(id = 13, getter = "getResultGroupingTypeFlags")
    @GroupingType
    private final int mResultGroupingTypeFlags;

    @Field(id = 14, getter = "getResultGroupingLimit")
    private final int mGroupingLimit;

    @Field(id = 15)
    final Bundle mTypePropertyWeightsField;

    @Field(id = 16, getter = "getJoinSpec")
    private final @Nullable JoinSpec mJoinSpec;

    @Field(id = 17, getter = "getAdvancedRankingExpression")
    private final String mAdvancedRankingExpression;

    @Field(id = 18, getter = "getEnabledFeatures")
    private final List<String> mEnabledFeatures;

    @Field(id = 19, getter = "getSearchSourceLogTag")
    private final @Nullable String mSearchSourceLogTag;

    @Field(id = 20, getter = "getEmbeddingParameters")
    private final @NonNull List<EmbeddingVector> mEmbeddingParameters;

    @Field(id = 21, getter = "getDefaultEmbeddingSearchMetricType")
    private final int mDefaultEmbeddingSearchMetricType;

    @Field(id = 22, getter = "getInformationalRankingExpressions")
    private final @NonNull List<String> mInformationalRankingExpressions;

    @Field(id = 23, getter = "getSearchStringParameters")
    private final @NonNull List<String> mSearchStringParameters;

    /**
     * Holds the list of document ids to search over.
     *
     * <p>If empty, the query will search over all documents.
     */
    @Field(id = 24, getter = "getFilterDocumentIds")
    private final @NonNull List<String> mFilterDocumentIds;

    /**
     * Default number of documents per page.
     *
     * @hide
     */
    public static final int DEFAULT_NUM_PER_PAGE = 10;

    // TODO(b/170371356): In framework, we may want these limits to be flag controlled.
    //  If that happens, the @IntRange() directives in this class may have to change.
    private static final int MAX_NUM_PER_PAGE = 10_000;
    private static final int MAX_SNIPPET_COUNT = 10_000;
    private static final int MAX_SNIPPET_PER_PROPERTY_COUNT = 10_000;
    private static final int MAX_SNIPPET_SIZE_LIMIT = 10_000;

    /**
     * Term Match Type for the query.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link com.google.android.icing.proto.SearchSpecProto.termMatchType}

    @IntDef(value = {TERM_MATCH_EXACT_ONLY, TERM_MATCH_PREFIX})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TermMatch {}

    /**
     * Query terms will only match exact tokens in the index.
     *
     * <p>For example, a query term "foo" will only match indexed token "foo", and not "foot" or
     * "football".
     */
    public static final int TERM_MATCH_EXACT_ONLY = 1;

    /**
     * Query terms will match indexed tokens when the query term is a prefix of the token.
     *
     * <p>For example, a query term "foo" will match indexed tokens like "foo", "foot", and
     * "football".
     */
    public static final int TERM_MATCH_PREFIX = 2;

    /**
     * Ranking Strategy for query result.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link ScoringSpecProto.RankingStrategy.Code}

    @IntDef(
            value = {
                RANKING_STRATEGY_NONE,
                RANKING_STRATEGY_DOCUMENT_SCORE,
                RANKING_STRATEGY_CREATION_TIMESTAMP,
                RANKING_STRATEGY_RELEVANCE_SCORE,
                RANKING_STRATEGY_USAGE_COUNT,
                RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP,
                RANKING_STRATEGY_SYSTEM_USAGE_COUNT,
                RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP,
                RANKING_STRATEGY_JOIN_AGGREGATE_SCORE,
                RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RankingStrategy {}

    /** No Ranking, results are returned in arbitrary order. */
    public static final int RANKING_STRATEGY_NONE = 0;

    /** Ranked by app-provided document scores. */
    public static final int RANKING_STRATEGY_DOCUMENT_SCORE = 1;

    /** Ranked by document creation timestamps. */
    public static final int RANKING_STRATEGY_CREATION_TIMESTAMP = 2;

    /** Ranked by document relevance score. */
    public static final int RANKING_STRATEGY_RELEVANCE_SCORE = 3;

    /** Ranked by number of usages, as reported by the app. */
    public static final int RANKING_STRATEGY_USAGE_COUNT = 4;

    /** Ranked by timestamp of last usage, as reported by the app. */
    public static final int RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP = 5;

    /** Ranked by number of usages from a system UI surface. */
    public static final int RANKING_STRATEGY_SYSTEM_USAGE_COUNT = 6;

    /** Ranked by timestamp of last usage from a system UI surface. */
    public static final int RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP = 7;

    /**
     * Ranked by the aggregated ranking signal of the joined documents.
     *
     * <p>Which aggregation strategy is used to determine a ranking signal is specified in the
     * {@link JoinSpec} set by {@link Builder#setJoinSpec}. This ranking strategy may not be used if
     * no {@link JoinSpec} is provided.
     *
     * @see Builder#build
     */
    public static final int RANKING_STRATEGY_JOIN_AGGREGATE_SCORE = 8;

    /** Ranked by the advanced ranking expression provided. */
    public static final int RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION = 9;

    /**
     * Order for query result.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link ScoringSpecProto.Order.Code}

    @IntDef(value = {ORDER_DESCENDING, ORDER_ASCENDING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Order {}

    /** Search results will be returned in a descending order. */
    public static final int ORDER_DESCENDING = 0;

    /** Search results will be returned in an ascending order. */
    public static final int ORDER_ASCENDING = 1;

    /**
     * Grouping type for result limits.
     *
     * @hide
     */
    @IntDef(
            flag = true,
            value = {
                GROUPING_TYPE_PER_PACKAGE,
                GROUPING_TYPE_PER_NAMESPACE,
                GROUPING_TYPE_PER_SCHEMA
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface GroupingType {}

    /**
     * Results should be grouped together by package for the purpose of enforcing a limit on the
     * number of results returned per package.
     */
    public static final int GROUPING_TYPE_PER_PACKAGE = 1 << 0;

    /**
     * Results should be grouped together by namespace for the purpose of enforcing a limit on the
     * number of results returned per namespace.
     */
    public static final int GROUPING_TYPE_PER_NAMESPACE = 1 << 1;

    /**
     * Results should be grouped together by schema type for the purpose of enforcing a limit on the
     * number of results returned per schema type.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_GROUPING_TYPE_PER_SCHEMA)
    public static final int GROUPING_TYPE_PER_SCHEMA = 1 << 2;

    /**
     * Type of scoring used to calculate similarity for embedding vectors. For details of each, see
     * comments above each value.
     *
     * @hide
     */
    // NOTE: The integer values of these constants must match the proto enum constants in
    // {@link SearchSpecProto.EmbeddingQueryMetricType.Code}

    @IntDef(
            value = {
                EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT,
                EMBEDDING_SEARCH_METRIC_TYPE_COSINE,
                EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT,
                EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EmbeddingSearchMetricType {}

    /**
     * Use the default metric set in {@link SearchSpec#getDefaultEmbeddingSearchMetricType()} for
     * embedding search and ranking.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public static final int EMBEDDING_SEARCH_METRIC_TYPE_DEFAULT = 0;

    /** Cosine similarity as metric for embedding search and ranking. */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public static final int EMBEDDING_SEARCH_METRIC_TYPE_COSINE = 1;

    /** Dot product similarity as metric for embedding search and ranking. */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public static final int EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT = 2;

    /** Euclidean distance as metric for embedding search and ranking. */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public static final int EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN = 3;

    @Constructor
    SearchSpec(
            @Param(id = 1) int termMatchType,
            @Param(id = 2) @NonNull List<String> schemas,
            @Param(id = 3) @NonNull List<String> namespaces,
            @Param(id = 4) @NonNull Bundle properties,
            @Param(id = 5) @NonNull List<String> packageNames,
            @Param(id = 6) int resultCountPerPage,
            @Param(id = 7) @RankingStrategy int rankingStrategy,
            @Param(id = 8) @Order int order,
            @Param(id = 9) int snippetCount,
            @Param(id = 10) int snippetCountPerProperty,
            @Param(id = 11) int maxSnippetSize,
            @Param(id = 12) @NonNull Bundle projectionTypePropertyMasks,
            @Param(id = 13) int resultGroupingTypeFlags,
            @Param(id = 14) int groupingLimit,
            @Param(id = 15) @NonNull Bundle typePropertyWeightsField,
            @Param(id = 16) @Nullable JoinSpec joinSpec,
            @Param(id = 17) @NonNull String advancedRankingExpression,
            @Param(id = 18) @NonNull List<String> enabledFeatures,
            @Param(id = 19) @Nullable String searchSourceLogTag,
            @Param(id = 20) @Nullable List<EmbeddingVector> embeddingParameters,
            @Param(id = 21) int defaultEmbeddingSearchMetricType,
            @Param(id = 22) @Nullable List<String> informationalRankingExpressions,
            @Param(id = 23) @Nullable List<String> searchStringParameters,
            @Param(id = 24) @Nullable List<String> filterDocumentIds) {
        mTermMatchType = termMatchType;
        mSchemas = Collections.unmodifiableList(Objects.requireNonNull(schemas));
        mNamespaces = Collections.unmodifiableList(Objects.requireNonNull(namespaces));
        mTypePropertyFilters = Objects.requireNonNull(properties);
        mPackageNames = Collections.unmodifiableList(Objects.requireNonNull(packageNames));
        mResultCountPerPage = resultCountPerPage;
        mRankingStrategy = rankingStrategy;
        mOrder = order;
        mSnippetCount = snippetCount;
        mSnippetCountPerProperty = snippetCountPerProperty;
        mMaxSnippetSize = maxSnippetSize;
        mProjectionTypePropertyMasks = Objects.requireNonNull(projectionTypePropertyMasks);
        mResultGroupingTypeFlags = resultGroupingTypeFlags;
        mGroupingLimit = groupingLimit;
        mTypePropertyWeightsField = Objects.requireNonNull(typePropertyWeightsField);
        mJoinSpec = joinSpec;
        mAdvancedRankingExpression = Objects.requireNonNull(advancedRankingExpression);
        mEnabledFeatures = Collections.unmodifiableList(Objects.requireNonNull(enabledFeatures));
        mSearchSourceLogTag = searchSourceLogTag;
        if (embeddingParameters != null) {
            mEmbeddingParameters = Collections.unmodifiableList(embeddingParameters);
        } else {
            mEmbeddingParameters = Collections.emptyList();
        }
        mDefaultEmbeddingSearchMetricType = defaultEmbeddingSearchMetricType;
        if (informationalRankingExpressions != null) {
            mInformationalRankingExpressions =
                    Collections.unmodifiableList(informationalRankingExpressions);
        } else {
            mInformationalRankingExpressions = Collections.emptyList();
        }
        mSearchStringParameters =
                (searchStringParameters != null)
                        ? Collections.unmodifiableList(searchStringParameters)
                        : Collections.emptyList();
        mFilterDocumentIds =
                (filterDocumentIds != null)
                        ? Collections.unmodifiableList(filterDocumentIds)
                        : Collections.emptyList();
    }

    /** Returns how the query terms should match terms in the index. */
    @TermMatch
    public int getTermMatch() {
        return mTermMatchType;
    }

    /**
     * Returns the list of schema types to search for.
     *
     * <p>If empty, the query will search over all schema types.
     */
    public @NonNull List<String> getFilterSchemas() {
        if (mSchemas == null) {
            return Collections.emptyList();
        }
        return mSchemas;
    }

    /**
     * Returns the map of schema and target properties to search over.
     *
     * <p>If empty, will search over all schema and properties.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned by this
     * function, rather than calling it multiple times.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
    public @NonNull Map<String, List<String>> getFilterProperties() {
        Set<String> schemas = mTypePropertyFilters.keySet();
        Map<String, List<String>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            typePropertyPathsMap.put(
                    schema,
                    Objects.requireNonNull(mTypePropertyFilters.getStringArrayList(schema)));
        }
        return typePropertyPathsMap;
    }

    /**
     * Returns the list of namespaces to search over.
     *
     * <p>If empty, the query will search over all namespaces.
     */
    public @NonNull List<String> getFilterNamespaces() {
        if (mNamespaces == null) {
            return Collections.emptyList();
        }
        return mNamespaces;
    }

    /**
     * Returns the list of package name filters to search over.
     *
     * <p>If empty, the query will search over all packages that the caller has access to. If
     * package names are specified which caller doesn't have access to, then those package names
     * will be ignored.
     */
    public @NonNull List<String> getFilterPackageNames() {
        if (mPackageNames == null) {
            return Collections.emptyList();
        }
        return mPackageNames;
    }

    /**
     * Returns the list of document ids to search over.
     *
     * <p>If empty, the query will search over all documents.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS)
    public @NonNull List<String> getFilterDocumentIds() {
        return mFilterDocumentIds;
    }

    /** Returns the number of results per page in the result set. */
    public int getResultCountPerPage() {
        return mResultCountPerPage;
    }

    /** Returns the ranking strategy. */
    @RankingStrategy
    public int getRankingStrategy() {
        return mRankingStrategy;
    }

    /** Returns the order of returned search results (descending or ascending). */
    @Order
    public int getOrder() {
        return mOrder;
    }

    /** Returns how many documents to generate snippets for. */
    public int getSnippetCount() {
        return mSnippetCount;
    }

    /**
     * Returns how many matches for each property of a matching document to generate snippets for.
     */
    public int getSnippetCountPerProperty() {
        return mSnippetCountPerProperty;
    }

    /** Returns the maximum size of a snippet in characters. */
    public int getMaxSnippetSize() {
        return mMaxSnippetSize;
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned by this
     * function, rather than calling it multiple times.
     *
     * @return A mapping of schema types to lists of projection strings.
     */
    public @NonNull Map<String, List<String>> getProjections() {
        Set<String> schemas = mProjectionTypePropertyMasks.keySet();
        Map<String, List<String>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            typePropertyPathsMap.put(
                    schema,
                    Objects.requireNonNull(
                            mProjectionTypePropertyMasks.getStringArrayList(schema)));
        }
        return typePropertyPathsMap;
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned by this
     * function, rather than calling it multiple times.
     *
     * @return A mapping of schema types to lists of projection {@link PropertyPath} objects.
     */
    public @NonNull Map<String, List<PropertyPath>> getProjectionPaths() {
        Set<String> schemas = mProjectionTypePropertyMasks.keySet();
        Map<String, List<PropertyPath>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            ArrayList<String> propertyPathList =
                    mProjectionTypePropertyMasks.getStringArrayList(schema);
            if (propertyPathList != null) {
                List<PropertyPath> copy = new ArrayList<>(propertyPathList.size());
                for (int i = 0; i < propertyPathList.size(); i++) {
                    String p = propertyPathList.get(i);
                    copy.add(new PropertyPath(p));
                }
                typePropertyPathsMap.put(schema, copy);
            }
        }
        return typePropertyPathsMap;
    }

    /**
     * Returns properties weights to be used for scoring.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the {@link Map} returned
     * by this function, rather than calling it multiple times.
     *
     * @return a {@link Map} of schema type to an inner-map of property paths of the schema type to
     *     the weight to set for that property.
     */
    public @NonNull Map<String, Map<String, Double>> getPropertyWeights() {
        Set<String> schemaTypes = mTypePropertyWeightsField.keySet();
        Map<String, Map<String, Double>> typePropertyWeightsMap =
                new ArrayMap<>(schemaTypes.size());
        for (String schemaType : schemaTypes) {
            Bundle propertyPathBundle = mTypePropertyWeightsField.getBundle(schemaType);
            if (propertyPathBundle != null) {
                Set<String> propertyPaths = propertyPathBundle.keySet();
                Map<String, Double> propertyPathWeights = new ArrayMap<>(propertyPaths.size());
                for (String propertyPath : propertyPaths) {
                    propertyPathWeights.put(
                            propertyPath, propertyPathBundle.getDouble(propertyPath));
                }
                typePropertyWeightsMap.put(schemaType, propertyPathWeights);
            }
        }
        return typePropertyWeightsMap;
    }

    /**
     * Returns properties weights to be used for scoring.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the {@link Map} returned
     * by this function, rather than calling it multiple times.
     *
     * @return a {@link Map} of schema type to an inner-map of property paths of the schema type to
     *     the weight to set for that property.
     */
    public @NonNull Map<String, Map<PropertyPath, Double>> getPropertyWeightPaths() {
        Set<String> schemaTypes = mTypePropertyWeightsField.keySet();
        Map<String, Map<PropertyPath, Double>> typePropertyWeightsMap =
                new ArrayMap<>(schemaTypes.size());
        for (String schemaType : schemaTypes) {
            Bundle propertyPathBundle = mTypePropertyWeightsField.getBundle(schemaType);
            if (propertyPathBundle != null) {
                Set<String> propertyPaths = propertyPathBundle.keySet();
                Map<PropertyPath, Double> propertyPathWeights =
                        new ArrayMap<>(propertyPaths.size());
                for (String propertyPath : propertyPaths) {
                    propertyPathWeights.put(
                            new PropertyPath(propertyPath),
                            propertyPathBundle.getDouble(propertyPath));
                }
                typePropertyWeightsMap.put(schemaType, propertyPathWeights);
            }
        }
        return typePropertyWeightsMap;
    }

    /**
     * Get the type of grouping limit to apply, or 0 if {@link Builder#setResultGrouping} was not
     * called.
     */
    @GroupingType
    public int getResultGroupingTypeFlags() {
        return mResultGroupingTypeFlags;
    }

    /**
     * Get the maximum number of results to return for each group.
     *
     * @return the maximum number of results to return for each group or 0 if {@link
     *     Builder#setResultGrouping(int, int)} was not called.
     */
    public int getResultGroupingLimit() {
        return mGroupingLimit;
    }

    /** Returns specification on which documents need to be joined. */
    public @Nullable JoinSpec getJoinSpec() {
        return mJoinSpec;
    }

    /**
     * Get the advanced ranking expression, or "" if {@link Builder#setRankingStrategy(String)} was
     * not called.
     */
    public @NonNull String getAdvancedRankingExpression() {
        return mAdvancedRankingExpression;
    }

    /**
     * Gets a tag to indicate the source of this search, or {@code null} if {@link
     * Builder#setSearchSourceLogTag(String)} was not called.
     *
     * <p>Some AppSearch implementations may log a hash of this tag using statsd. This tag may be
     * used for tracing performance issues and crashes to a component of an app.
     *
     * <p>Call {@link Builder#setSearchSourceLogTag} and give a unique value if you want to
     * distinguish this search scenario with other search scenarios during performance analysis.
     *
     * <p>Under no circumstances will AppSearch log the raw String value using statsd, but it will
     * be provided as-is to custom {@code AppSearchLogger} implementations you have registered in
     * your app.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG)
    public @Nullable String getSearchSourceLogTag() {
        return mSearchSourceLogTag;
    }

    /**
     * Returns the list of {@link EmbeddingVector} that can be referenced in the query through the
     * "getEmbeddingParameter({index})" function.
     *
     * @see AppSearchSession#search
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public @NonNull List<EmbeddingVector> getEmbeddingParameters() {
        return mEmbeddingParameters;
    }

    /**
     * Returns the default embedding metric type used for embedding search (see {@link
     * AppSearchSession#search}) and ranking (see {@link
     * SearchSpec.Builder#setRankingStrategy(String)}).
     */
    @EmbeddingSearchMetricType
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public int getDefaultEmbeddingSearchMetricType() {
        return mDefaultEmbeddingSearchMetricType;
    }

    /**
     * Returns the informational ranking expressions.
     *
     * @see Builder#addInformationalRankingExpressions
     */
    @FlaggedApi(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public @NonNull List<String> getInformationalRankingExpressions() {
        return mInformationalRankingExpressions;
    }

    /**
     * Returns the list of String parameters that can be referenced in the query through the
     * "getSearchStringParameter({index})" function.
     *
     * @see AppSearchSession#search
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
    public @NonNull List<String> getSearchStringParameters() {
        return mSearchStringParameters;
    }

    /** Returns whether the NUMERIC_SEARCH feature is enabled. */
    public boolean isNumericSearchEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.NUMERIC_SEARCH);
    }

    /** Returns whether the VERBATIM_SEARCH feature is enabled. */
    public boolean isVerbatimSearchEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.VERBATIM_SEARCH);
    }

    /** Returns whether the LIST_FILTER_QUERY_LANGUAGE feature is enabled. */
    public boolean isListFilterQueryLanguageEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.LIST_FILTER_QUERY_LANGUAGE);
    }

    /** Returns whether the ScorablePropertyRanking feature is enabled. */
    @FlaggedApi(Flags.FLAG_ENABLE_SCORABLE_PROPERTY)
    public boolean isScorablePropertyRankingEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.SCHEMA_SCORABLE_PROPERTY_CONFIG);
    }

    /** Returns whether the LIST_FILTER_HAS_PROPERTY_FUNCTION feature is enabled. */
    @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION)
    public boolean isListFilterHasPropertyFunctionEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.LIST_FILTER_HAS_PROPERTY_FUNCTION);
    }

    /** Returns whether the LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION feature is enabled. */
    @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION)
    public boolean isListFilterMatchScoreExpressionFunctionEnabled() {
        return mEnabledFeatures.contains(
                FeatureConstants.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION);
    }

    /**
     * Get the list of enabled features that the caller is intending to use in this search call.
     *
     * @return the set of {@link Features} enabled in this {@link SearchSpec} Entry.
     * @hide
     */
    public @NonNull List<String> getEnabledFeatures() {
        return mEnabledFeatures;
    }

    @Override
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchSpecCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link SearchSpec objects}. */
    public static final class Builder {
        private List<String> mSchemas = new ArrayList<>();
        private List<String> mNamespaces = new ArrayList<>();
        private Bundle mTypePropertyFilters = new Bundle();
        private List<String> mPackageNames = new ArrayList<>();
        private ArraySet<String> mEnabledFeatures = new ArraySet<>();
        private Bundle mProjectionTypePropertyMasks = new Bundle();
        private Bundle mTypePropertyWeights = new Bundle();
        private List<EmbeddingVector> mEmbeddingParameters = new ArrayList<>();
        private List<String> mSearchStringParameters = new ArrayList<>();
        private List<String> mFilterDocumentIds = new ArrayList<>();

        private int mResultCountPerPage = DEFAULT_NUM_PER_PAGE;
        @TermMatch private int mTermMatchType = TERM_MATCH_PREFIX;

        @EmbeddingSearchMetricType
        private int mDefaultEmbeddingSearchMetricType = EMBEDDING_SEARCH_METRIC_TYPE_COSINE;

        private int mSnippetCount = 0;
        private int mSnippetCountPerProperty = MAX_SNIPPET_PER_PROPERTY_COUNT;
        private int mMaxSnippetSize = 0;
        @RankingStrategy private int mRankingStrategy = RANKING_STRATEGY_NONE;
        @Order private int mOrder = ORDER_DESCENDING;
        @GroupingType private int mGroupingTypeFlags = 0;
        private int mGroupingLimit = 0;
        private @Nullable JoinSpec mJoinSpec;
        private String mAdvancedRankingExpression = "";
        private List<String> mInformationalRankingExpressions = new ArrayList<>();
        private @Nullable String mSearchSourceLogTag;
        private boolean mBuilt = false;

        /** Constructs a new {@link Builder} for {@link SearchSpec} objects. */
        public Builder() {}

        /** Constructs a new {@link Builder} from the given {@link SearchSpec}. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        public Builder(@NonNull SearchSpec searchSpec) {
            Objects.requireNonNull(searchSpec);
            mSchemas = new ArrayList<>(searchSpec.getFilterSchemas());
            mNamespaces = new ArrayList<>(searchSpec.getFilterNamespaces());
            for (Map.Entry<String, List<String>> entry :
                    searchSpec.getFilterProperties().entrySet()) {
                addFilterProperties(entry.getKey(), entry.getValue());
            }
            mPackageNames = new ArrayList<>(searchSpec.getFilterPackageNames());
            mEnabledFeatures = new ArraySet<>(searchSpec.getEnabledFeatures());
            for (Map.Entry<String, List<String>> entry : searchSpec.getProjections().entrySet()) {
                addProjection(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Map<String, Double>> entry :
                    searchSpec.getPropertyWeights().entrySet()) {
                setPropertyWeights(entry.getKey(), entry.getValue());
            }
            mEmbeddingParameters = new ArrayList<>(searchSpec.getEmbeddingParameters());
            mSearchStringParameters = new ArrayList<>(searchSpec.getSearchStringParameters());
            mResultCountPerPage = searchSpec.getResultCountPerPage();
            mTermMatchType = searchSpec.getTermMatch();
            mDefaultEmbeddingSearchMetricType = searchSpec.getDefaultEmbeddingSearchMetricType();
            mSnippetCount = searchSpec.getSnippetCount();
            mSnippetCountPerProperty = searchSpec.getSnippetCountPerProperty();
            mMaxSnippetSize = searchSpec.getMaxSnippetSize();
            mRankingStrategy = searchSpec.getRankingStrategy();
            mOrder = searchSpec.getOrder();
            mGroupingTypeFlags = searchSpec.getResultGroupingTypeFlags();
            mGroupingLimit = searchSpec.getResultGroupingLimit();
            mJoinSpec = searchSpec.getJoinSpec();
            mAdvancedRankingExpression = searchSpec.getAdvancedRankingExpression();
            mInformationalRankingExpressions =
                    new ArrayList<>(searchSpec.getInformationalRankingExpressions());
            mSearchSourceLogTag = searchSpec.getSearchSourceLogTag();
            mFilterDocumentIds = new ArrayList<>(searchSpec.getFilterDocumentIds());
        }

        /**
         * Sets how the query terms should match {@code TermMatchCode} in the index.
         *
         * <p>If this method is not called, the default term match type is {@link
         * SearchSpec#TERM_MATCH_PREFIX}.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setTermMatch(@TermMatch int termMatchType) {
            Preconditions.checkArgumentInRange(
                    termMatchType, TERM_MATCH_EXACT_ONLY, TERM_MATCH_PREFIX, "Term match type");
            resetIfBuilt();
            mTermMatchType = termMatchType;
            return this;
        }

        /**
         * Adds a Schema type filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified schema types.
         *
         * <p>If unset, the query will search over all schema types.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addFilterSchemas(@NonNull String... schemas) {
            Objects.requireNonNull(schemas);
            resetIfBuilt();
            return addFilterSchemas(Arrays.asList(schemas));
        }

        /**
         * Adds a Schema type filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified schema types.
         *
         * <p>If unset, the query will search over all schema types.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addFilterSchemas(@NonNull Collection<String> schemas) {
            Objects.requireNonNull(schemas);
            resetIfBuilt();
            mSchemas.addAll(schemas);
            return this;
        }

        /** Clears all schema type filters. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearFilterSchemas() {
            resetIfBuilt();
            mSchemas.clear();
            return this;
        }

        /**
         * Adds property paths for the specified type to the property filter of {@link SearchSpec}
         * Entry. Only returns documents that have matches under the specified properties. If
         * property paths are added for a type, then only the properties referred to will be
         * searched for results of that type.
         *
         * <p>If a property path that is specified isn't present in a result, it will be ignored for
         * that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of results
         * of that type will be searched.
         *
         * <p>Example properties: 'body', 'sender.name', 'sender.emailaddress', etc.
         *
         * <p>If property paths are added for the {@link SearchSpec#SCHEMA_TYPE_WILDCARD}, then
         * those property paths will apply to all results, excepting any types that have their own,
         * specific property paths set.
         *
         * @param schema the {@link AppSearchSchema} that contains the target properties
         * @param propertyPaths The String version of {@link PropertyPath}. A dot-delimited sequence
         *     of property names.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
        public @NonNull Builder addFilterProperties(
                @NonNull String schema, @NonNull Collection<String> propertyPaths) {
            Objects.requireNonNull(schema);
            Objects.requireNonNull(propertyPaths);
            resetIfBuilt();
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                Objects.requireNonNull(propertyPath);
                propertyPathsArrayList.add(propertyPath);
            }
            mTypePropertyFilters.putStringArrayList(schema, propertyPathsArrayList);
            return this;
        }

        /**
         * Adds property paths for the specified type to the property filter of {@link SearchSpec}
         * Entry. Only returns documents that have matches under the specified properties. If
         * property paths are added for a type, then only the properties referred to will be
         * searched for results of that type.
         *
         * @see #addFilterProperties(String, Collection)
         * @param schema the {@link AppSearchSchema} that contains the target properties
         * @param propertyPaths The {@link PropertyPath} to search search over
         */
        // Getter method is getFilterProperties
        @SuppressLint("MissingGetterMatchingBuilder")
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
        public @NonNull Builder addFilterPropertyPaths(
                @NonNull String schema, @NonNull Collection<PropertyPath> propertyPaths) {
            Objects.requireNonNull(schema);
            Objects.requireNonNull(propertyPaths);
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (PropertyPath propertyPath : propertyPaths) {
                propertyPathsArrayList.add(propertyPath.toString());
            }
            return addFilterProperties(schema, propertyPathsArrayList);
        }

        /** Clears the property filters for all schema types. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearFilterProperties() {
            resetIfBuilt();
            mTypePropertyFilters.clear();
            return this;
        }

        /**
         * Adds a namespace filter to {@link SearchSpec} Entry. Only search for documents that have
         * the specified namespaces.
         *
         * <p>If unset, the query will search over all namespaces.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addFilterNamespaces(@NonNull String... namespaces) {
            Objects.requireNonNull(namespaces);
            resetIfBuilt();
            return addFilterNamespaces(Arrays.asList(namespaces));
        }

        /**
         * Adds a namespace filter to {@link SearchSpec} Entry. Only search for documents that have
         * the specified namespaces.
         *
         * <p>If unset, the query will search over all namespaces.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addFilterNamespaces(@NonNull Collection<String> namespaces) {
            Objects.requireNonNull(namespaces);
            resetIfBuilt();
            mNamespaces.addAll(namespaces);
            return this;
        }

        /** Clears all namespace filters. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearFilterNamespaces() {
            resetIfBuilt();
            mNamespaces.clear();
            return this;
        }

        /**
         * Adds a package name filter to {@link SearchSpec} Entry. Only search for documents that
         * were indexed from the specified packages.
         *
         * <p>If unset, the query will search over all packages that the caller has access to. If
         * package names are specified which caller doesn't have access to, then those package names
         * will be ignored.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addFilterPackageNames(@NonNull String... packageNames) {
            Objects.requireNonNull(packageNames);
            resetIfBuilt();
            return addFilterPackageNames(Arrays.asList(packageNames));
        }

        /**
         * Adds a package name filter to {@link SearchSpec} Entry. Only search for documents that
         * were indexed from the specified packages.
         *
         * <p>If unset, the query will search over all packages that the caller has access to. If
         * package names are specified which caller doesn't have access to, then those package names
         * will be ignored.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addFilterPackageNames(@NonNull Collection<String> packageNames) {
            Objects.requireNonNull(packageNames);
            resetIfBuilt();
            mPackageNames.addAll(packageNames);
            return this;
        }

        /** Clears all package name filters. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearFilterPackageNames() {
            resetIfBuilt();
            mPackageNames.clear();
            return this;
        }

        /**
         * Adds a document id filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified document ids.
         *
         * <p>If unset, the query will search over all documents.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS)
        public @NonNull Builder addFilterDocumentIds(@NonNull String... documentIds) {
            Objects.requireNonNull(documentIds);
            resetIfBuilt();
            return addFilterDocumentIds(Arrays.asList(documentIds));
        }

        /**
         * Adds a document id filter to {@link SearchSpec} Entry. Only search for documents that
         * have the specified document ids.
         *
         * <p>If unset, the query will search over all documents.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS)
        public @NonNull Builder addFilterDocumentIds(@NonNull Collection<String> documentIds) {
            Objects.requireNonNull(documentIds);
            resetIfBuilt();
            mFilterDocumentIds.addAll(documentIds);
            return this;
        }

        /** Clears the document id filters. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearFilterDocumentIds() {
            resetIfBuilt();
            mFilterDocumentIds.clear();
            return this;
        }

        /**
         * Sets the number of results per page in the returned object.
         *
         * <p>The default number of results per page is 10.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder setResultCountPerPage(
                @IntRange(from = 0, to = MAX_NUM_PER_PAGE) int resultCountPerPage) {
            Preconditions.checkArgumentInRange(
                    resultCountPerPage, 0, MAX_NUM_PER_PAGE, "resultCountPerPage");
            resetIfBuilt();
            mResultCountPerPage = resultCountPerPage;
            return this;
        }

        /** Sets ranking strategy for AppSearch results. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRankingStrategy(@RankingStrategy int rankingStrategy) {
            Preconditions.checkArgumentInRange(
                    rankingStrategy,
                    RANKING_STRATEGY_NONE,
                    RANKING_STRATEGY_JOIN_AGGREGATE_SCORE,
                    "Result ranking strategy");
            resetIfBuilt();
            mRankingStrategy = rankingStrategy;
            mAdvancedRankingExpression = "";
            return this;
        }

        /**
         * Enables advanced ranking to score based on {@code advancedRankingExpression}.
         *
         * <p>This method will set RankingStrategy to {@link
         * #RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION}.
         *
         * <p>The ranking expression is a mathematical expression that will be evaluated to a
         * floating-point number of double type representing the score of each document.
         *
         * <p>Numeric literals, arithmetic operators, mathematical functions, document-based, and
         * property-value-based functions are supported to build expressions.
         *
         * <p>The following are supported arithmetic operators:
         *
         * <ul>
         *   <li>Addition(+)
         *   <li>Subtraction(-)
         *   <li>Multiplication(*)
         *   <li>Floating Point Division(/)
         * </ul>
         *
         * <p>Operator precedences are compliant with the Java Language, and parentheses are
         * supported. For example, "2.2 + (3 - 4) / 2" evaluates to 1.7.
         *
         * <p>The following are supported basic mathematical functions:
         *
         * <ul>
         *   <li>log(x) - the natural log of x
         *   <li>log(x, y) - the log of y with base x
         *   <li>pow(x, y) - x to the power of y
         *   <li>sqrt(x)
         *   <li>abs(x)
         *   <li>sin(x), cos(x), tan(x)
         *   <li>Example: "max(abs(-100), 10) + pow(2, 10)" will be evaluated to 1124
         * </ul>
         *
         * <p>The following variadic mathematical functions are supported, with n > 0. They also
         * accept list value parameters. For example, if V is a value of list type, we can call
         * sum(V) to get the sum of all the values in V. List literals are not supported, so a value
         * of list type can only be constructed as a return value of some particular document-based
         * functions.
         *
         * <ul>
         *   <li>max(v1, v2, ..., vn) or max(V)
         *   <li>min(v1, v2, ..., vn) or min(V)
         *   <li>len(v1, v2, ..., vn) or len(V)
         *   <li>sum(v1, v2, ..., vn) or sum(V)
         *   <li>avg(v1, v2, ..., vn) or avg(V)
         * </ul>
         *
         * <p>Document-based functions must be called via "this", which represents the current
         * document being scored. The following are supported document-based functions:
         *
         * <ul>
         *   <li>this.documentScore()
         *       <p>Get the app-provided document score of the current document. This is the same
         *       score that is returned for {@link #RANKING_STRATEGY_DOCUMENT_SCORE}.
         *   <li>this.creationTimestamp()
         *       <p>Get the creation timestamp of the current document. This is the same score that
         *       is returned for {@link #RANKING_STRATEGY_CREATION_TIMESTAMP}.
         *   <li>this.relevanceScore()
         *       <p>Get the BM25F relevance score of the current document in relation to the query
         *       string. This is the same score that is returned for {@link
         *       #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *   <li>this.usageCount(type) and this.usageLastUsedTimestamp(type)
         *       <p>Get the number of usages or the timestamp of last usage by type for the current
         *       document, where type must be evaluated to an integer from 1 to 2. Type 1 refers to
         *       usages reported by {@link AppSearchSession#reportUsage}, and type 2 refers to
         *       usages reported by {@link GlobalSearchSession#reportSystemUsage}.
         *   <li>this.childrenRankingSignals()
         *       <p>Returns a list of children ranking signals calculated by scoring the joined
         *       documents using the ranking strategy specified in the nested {@link SearchSpec}.
         *       Currently, a document can only be a child of another document in the context of
         *       joins. If this function is called without the Join API enabled, a type error will
         *       be raised.
         *   <li>this.propertyWeights()
         *       <p>Returns a list of the normalized weights of the matched properties for the
         *       current document being scored. Property weights come from what's specified in
         *       {@link SearchSpec}. After normalizing, each provided weight will be divided by the
         *       maximum weight, so that each of them will be <= 1.
         *   <li>this.matchedSemanticScores(getEmbeddingParameter({embedding_index}), {metric})
         *       <p>Returns a list of the matched similarity scores from "semanticSearch" in the
         *       query expression (see also {@link AppSearchSession#search}) based on
         *       embedding_index and metric. If metric is omitted, it defaults to the metric
         *       specified in {@link SearchSpec.Builder#setDefaultEmbeddingSearchMetricType(int)}.
         *       If no "semanticSearch" is called for embedding_index and metric in the query, this
         *       function will return an empty list. If multiple "semanticSearch"s are called for
         *       the same embedding_index and metric, this function will return a list of their
         *       merged scores.
         *       <p>Example: `this.matchedSemanticScores(getEmbeddingParameter(0), "COSINE")` will
         *       return a list of matched scores within the range of [0.5, 1], if
         *       `semanticSearch(getEmbeddingParameter(0), 0.5, 1, "COSINE")` is called in the query
         *       expression.
         * </ul>
         *
         * <p>Property-value-based functions can be called via the function of
         * getScorableProperty(schemaType, propertyPath)
         *
         * <ul>
         *   <li>In order to use this function, ScorablePropertyRanking feature must be enabled via
         *       {@link SearchSpec.Builder#setScorablePropertyRankingEnabled(boolean)}.
         *   <li>Param 'schemaType' must be a valid AppSearch SchemaType otherwise an error is
         *       returned.
         *   <li>Param 'propertyPath' must be valid and scorable otherwise an error is returned. It
         *       is considered scorable when:
         *       <ul>
         *         <li>It is to a property that is set to be enabled for scoring, or that
         *         <li>It points to a scorable property of nested schema types.
         *       </ul>
         *   <li>This function returns a list double values for the matched documents.
         *       <ul>
         *         <li>If the matched document's schema is different from 'schemaType', or the
         *             property under the 'propertyPath' holds no element, an empty list is
         *             returned.
         *       </ul>
         *   <li>Some examples below:
         *       <p>Suppose that there are two schemas: 'Gmail' and 'Person'. 'Gmail' schema has a
         *       property 'recipient' with schema type 'Person'. In the advanced ranking expression,
         *       you can have:
         *       <ul>
         *         <li>"sum(getScorableProperty('Gmail', 'viewTimes'))"
         *         <li>"maxOrDefault(getScorableProperty('Person', 'income'), 0)"
         *         <li>"sum(getScorableProperty('Gmail', 'recipient.income'))"
         *         <li>"this.documentScore() + sum(getScorableProperty('Gmail', 'viewTimes'))"
         *       </ul>
         * </ul>
         *
         * <p>The following functions are provided for enhanced list manipulation.
         *
         * <ul>
         *   <li>minOrDefault(V, default_score)
         *       <p>Returns the minimum value in the input list V or the default_score if the list
         *       is empty.
         *       <p>Example: "minOrDefault(this.matchedSemanticScores(getEmbeddingParameter(0)),
         *       10)" will return the minimum matched semantic scores or 10 if there is no matched
         *       score for the current document.
         *       <p>This function requires the feature {@link
         *       Features#SEARCH_SPEC_RANKING_FUNCTION_MAX_MIN_OR_DEFAULT}.
         *   <li>maxOrDefault(V, default_score)
         *       <p>Returns the maximum value in the input list V or the default_score if the list
         *       is empty.
         *       <p>Example: "maxOrDefault(this.matchedSemanticScores(getEmbeddingParameter(0)),
         *       -10)" will return the maximum matched semantic scores or -10 if there is no matched
         *       score for the current document.
         *       <p>This function requires the feature {@link
         *       Features#SEARCH_SPEC_RANKING_FUNCTION_MAX_MIN_OR_DEFAULT}.
         *   <li>filterByRange(V, low, high)
         *       <p>Returns a sublist of V that only contains the elements that fall within the
         *       specified range [low, high].
         *       <p>Example: "filterByRange(this.matchedSemanticScores(getEmbeddingParameter(0)), 0,
         *       1)" will return a list of matched semantic scores that are between 0 and 1,
         *       inclusive.
         *       <p>This function requires the feature {@link
         *       Features#SEARCH_SPEC_RANKING_FUNCTION_FILTER_BY_RANGE}.
         * </ul>
         *
         * <p>Some errors may occur when using advanced ranking.
         *
         * <p>Syntax Error: the expression violates the syntax of the advanced ranking language.
         * Below are some examples.
         *
         * <ul>
         *   <li>"1 + " - missing operand
         *   <li>"2 * (1 + 2))" - unbalanced parenthesis
         *   <li>"2 ^ 3" - unknown operator
         * </ul>
         *
         * <p>Type Error: the expression fails a static type check. Below are some examples.
         *
         * <ul>
         *   <li>"sin(2, 3)" - wrong number of arguments for the sin function
         *   <li>"this.childrenRankingSignals() + 1" - cannot add a list with a number
         *   <li>"this.propertyWeights()" - the final type of the overall expression cannot be a
         *       list, which can be fixed by "max(this.propertyWeights())"
         *   <li>"abs(this.propertyWeights())" - the abs function does not support list type
         *       arguments
         *   <li>"print(2)" - unknown function
         * </ul>
         *
         * <p>Evaluation Error: an error occurred while evaluating the value of the expression.
         * Below are some examples.
         *
         * <ul>
         *   <li>"1 / 0", "log(0)", "1 + sqrt(-1)" - getting a non-finite value in the middle of
         *       evaluation
         *   <li>"this.usageCount(1 + 0.5)" - expect the argument to be an integer. Note that this
         *       is not a type error and "this.usageCount(1.5 + 1/2)" can succeed without any issues
         *   <li>"this.documentScore()" - in case of an IO error, this will be an evaluation error
         * </ul>
         *
         * <p>Syntax errors and type errors will fail the entire search and will cause {@link
         * SearchResults#getNextPage} to throw an {@link AppSearchException} with the result code of
         * {@link AppSearchResult#RESULT_INVALID_ARGUMENT}.
         *
         * <p>Evaluation errors will result in the offending documents receiving the default score.
         * For {@link #ORDER_DESCENDING}, the default score will be 0, for {@link #ORDER_ASCENDING}
         * the default score will be infinity.
         *
         * @param advancedRankingExpression a non-empty string representing the ranking expression.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setRankingStrategy(@NonNull String advancedRankingExpression) {
            Preconditions.checkStringNotEmpty(advancedRankingExpression);
            resetIfBuilt();
            mRankingStrategy = RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION;
            mAdvancedRankingExpression = advancedRankingExpression;
            return this;
        }

        /**
         * Adds informational ranking expressions to be evaluated for each document in the search
         * result. The values of these expressions will be returned to the caller via {@link
         * SearchResult#getInformationalRankingSignals()}. These expressions are purely for the
         * caller to retrieve additional information about the result and have no effect on ranking.
         *
         * <p>The syntax is exactly the same as specified in {@link
         * SearchSpec.Builder#setRankingStrategy(String)}.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
        public @NonNull Builder addInformationalRankingExpressions(
                @NonNull String... informationalRankingExpressions) {
            Objects.requireNonNull(informationalRankingExpressions);
            resetIfBuilt();
            return addInformationalRankingExpressions(
                    Arrays.asList(informationalRankingExpressions));
        }

        /**
         * Adds informational ranking expressions to be evaluated for each document in the search
         * result. The values of these expressions will be returned to the caller via {@link
         * SearchResult#getInformationalRankingSignals()}. These expressions are purely for the
         * caller to retrieve additional information about the result and have no effect on ranking.
         *
         * <p>The syntax is exactly the same as specified in {@link
         * SearchSpec.Builder#setRankingStrategy(String)}.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
        public @NonNull Builder addInformationalRankingExpressions(
                @NonNull Collection<String> informationalRankingExpressions) {
            Objects.requireNonNull(informationalRankingExpressions);
            resetIfBuilt();
            mInformationalRankingExpressions.addAll(informationalRankingExpressions);
            return this;
        }

        /** Clears all informational ranking expressions. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearInformationalRankingExpressions() {
            resetIfBuilt();
            mInformationalRankingExpressions.clear();
            return this;
        }

        /**
         * Sets an optional log tag to indicate the source of this search.
         *
         * <p>Some AppSearch implementations may log a hash of this tag using statsd. This tag may
         * be used for tracing performance issues and crashes to a component of an app.
         *
         * <p>Call this method and give a unique value if you want to distinguish this search
         * scenario with other search scenarios during performance analysis.
         *
         * <p>Under no circumstances will AppSearch log the raw String value using statsd, but it
         * will be provided as-is to custom {@code AppSearchLogger} implementations you have
         * registered in your app.
         *
         * @param searchSourceLogTag A String to indicate the source caller of this search. It is
         *     used to label the search statsd for performance analysis. It is not the tag we are
         *     using in {@link android.util.Log}. The length of the teg should between 1 and 100.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG)
        public @NonNull Builder setSearchSourceLogTag(@NonNull String searchSourceLogTag) {
            Preconditions.checkStringNotEmpty(searchSourceLogTag);
            Preconditions.checkArgument(
                    searchSourceLogTag.length() <= 100,
                    "The maximum supported tag length is 100. This tag is too long: "
                            + searchSourceLogTag.length());
            resetIfBuilt();
            mSearchSourceLogTag = searchSourceLogTag;
            return this;
        }

        /** Clears the log tag that indicates the source of this search. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearSearchSourceLogTag() {
            resetIfBuilt();
            mSearchSourceLogTag = null;
            return this;
        }

        /**
         * Sets the order of returned search results, the default is {@link #ORDER_DESCENDING},
         * meaning that results with higher scores come first.
         *
         * <p>This order field will be ignored if RankingStrategy = {@code RANKING_STRATEGY_NONE}.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setOrder(@Order int order) {
            Preconditions.checkArgumentInRange(
                    order, ORDER_DESCENDING, ORDER_ASCENDING, "Result ranking order");
            resetIfBuilt();
            mOrder = order;
            return this;
        }

        /**
         * Sets the {@code snippetCount} such that the first {@code snippetCount} documents based on
         * the ranking strategy will have snippet information provided.
         *
         * <p>The list returned from {@link SearchResult#getMatchInfos} will contain at most this
         * many entries.
         *
         * <p>If set to 0 (default), snippeting is disabled and the list returned from {@link
         * SearchResult#getMatchInfos} will be empty.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder setSnippetCount(
                @IntRange(from = 0, to = MAX_SNIPPET_COUNT) int snippetCount) {
            Preconditions.checkArgumentInRange(snippetCount, 0, MAX_SNIPPET_COUNT, "snippetCount");
            resetIfBuilt();
            mSnippetCount = snippetCount;
            return this;
        }

        /**
         * Sets {@code snippetCountPerProperty}. Only the first {@code snippetCountPerProperty}
         * snippets for each property of each {@link GenericDocument} will contain snippet
         * information.
         *
         * <p>If set to 0, snippeting is disabled and the list returned from {@link
         * SearchResult#getMatchInfos} will be empty.
         *
         * <p>The default behavior is to snippet all matches a property contains, up to the maximum
         * value of 10,000.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder setSnippetCountPerProperty(
                @IntRange(from = 0, to = MAX_SNIPPET_PER_PROPERTY_COUNT)
                        int snippetCountPerProperty) {
            Preconditions.checkArgumentInRange(
                    snippetCountPerProperty,
                    0,
                    MAX_SNIPPET_PER_PROPERTY_COUNT,
                    "snippetCountPerProperty");
            resetIfBuilt();
            mSnippetCountPerProperty = snippetCountPerProperty;
            return this;
        }

        /**
         * Sets {@code maxSnippetSize}, the maximum snippet size. Snippet windows start at {@code
         * maxSnippetSize/2} bytes before the middle of the matching token and end at {@code
         * maxSnippetSize/2} bytes after the middle of the matching token. It respects token
         * boundaries, therefore the returned window may be smaller than requested.
         *
         * <p>Setting {@code maxSnippetSize} to 0 will disable windowing and an empty String will be
         * returned. If matches enabled is also set to false, then snippeting is disabled.
         *
         * <p>For example, {@code maxSnippetSize} = 16. "foo bar baz bat rat" with a query of "baz"
         * will return a window of "bar baz bat" which is only 11 bytes long.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder setMaxSnippetSize(
                @IntRange(from = 0, to = MAX_SNIPPET_SIZE_LIMIT) int maxSnippetSize) {
            Preconditions.checkArgumentInRange(
                    maxSnippetSize, 0, MAX_SNIPPET_SIZE_LIMIT, "maxSnippetSize");
            resetIfBuilt();
            mMaxSnippetSize = maxSnippetSize;
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property paths
         * are added for a type, then only the properties referred to will be retrieved for results
         * of that type. If a property path that is specified isn't present in a result, it will be
         * ignored for that result. Property paths cannot be null.
         *
         * @see #addProjectionPaths
         * @param schema a string corresponding to the schema to add projections to.
         * @param propertyPaths the projections to add.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder addProjection(
                @NonNull String schema, @NonNull Collection<String> propertyPaths) {
            Objects.requireNonNull(schema);
            Objects.requireNonNull(propertyPaths);
            resetIfBuilt();
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                Objects.requireNonNull(propertyPath);
                propertyPathsArrayList.add(propertyPath);
            }
            mProjectionTypePropertyMasks.putStringArrayList(schema, propertyPathsArrayList);
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property paths
         * are added for a type, then only the properties referred to will be retrieved for results
         * of that type. If a property path that is specified isn't present in a result, it will be
         * ignored for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of results
         * of that type will be retrieved.
         *
         * <p>If property path is added for the {@link SearchSpec#SCHEMA_TYPE_WILDCARD}, then those
         * property paths will apply to all results, excepting any types that have their own,
         * specific property paths set.
         *
         * <p>Suppose the following document is in the index.
         *
         * <pre>{@code
         * Email: Document {
         *   sender: Document {
         *     name: "Mr. Person"
         *     email: "mrperson123@google.com"
         *   }
         *   recipients: [
         *     Document {
         *       name: "John Doe"
         *       email: "johndoe123@google.com"
         *     }
         *     Document {
         *       name: "Jane Doe"
         *       email: "janedoe123@google.com"
         *     }
         *   ]
         *   subject: "IMPORTANT"
         *   body: "Limited time offer!"
         * }
         * }</pre>
         *
         * <p>Then, suppose that a query for "important" is issued with the following projection
         * type property paths:
         *
         * <pre>{@code
         * {schema: "Email", ["subject", "sender.name", "recipients.name"]}
         * }</pre>
         *
         * <p>The above document will be returned as:
         *
         * <pre>{@code
         * Email: Document {
         *   sender: Document {
         *     name: "Mr. Body"
         *   }
         *   recipients: [
         *     Document {
         *       name: "John Doe"
         *     }
         *     Document {
         *       name: "Jane Doe"
         *     }
         *   ]
         *   subject: "IMPORTANT"
         * }
         * }</pre>
         *
         * @param schema a string corresponding to the schema to add projections to.
         * @param propertyPaths the projections to add.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder addProjectionPaths(
                @NonNull String schema, @NonNull Collection<PropertyPath> propertyPaths) {
            Objects.requireNonNull(schema);
            Objects.requireNonNull(propertyPaths);
            ArrayList<String> propertyPathsArrayList = new ArrayList<>(propertyPaths.size());
            for (PropertyPath propertyPath : propertyPaths) {
                propertyPathsArrayList.add(propertyPath.toString());
            }
            return addProjection(schema, propertyPathsArrayList);
        }

        /** Clears the projections for all schema types. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearProjections() {
            resetIfBuilt();
            mProjectionTypePropertyMasks.clear();
            return this;
        }

        /**
         * Sets the maximum number of results to return for each group, where groups are defined by
         * grouping type.
         *
         * <p>Calling this method will override any previous calls. So calling {@code
         * setResultGrouping(GROUPING_TYPE_PER_PACKAGE, 7)} and then calling {@code
         * setResultGrouping(GROUPING_TYPE_PER_PACKAGE, 2)} will result in only the latter, a limit
         * of two results per package, being applied. Or calling {@code setResultGrouping
         * (GROUPING_TYPE_PER_PACKAGE, 1)} and then calling {@code setResultGrouping
         * (GROUPING_TYPE_PER_PACKAGE | GROUPING_PER_NAMESPACE, 5)} will result in five results per
         * package per namespace.
         *
         * @param groupingTypeFlags One or more combination of grouping types.
         * @param limit Number of results to return per {@code groupingTypeFlags}.
         * @throws IllegalArgumentException if groupingTypeFlags is zero.
         */
        // Individual parameters available from getResultGroupingTypeFlags and
        // getResultGroupingLimit
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setResultGrouping(@GroupingType int groupingTypeFlags, int limit) {
            Preconditions.checkState(
                    groupingTypeFlags != 0, "Result grouping type cannot be zero.");
            resetIfBuilt();
            mGroupingTypeFlags = groupingTypeFlags;
            mGroupingLimit = limit;
            return this;
        }

        /** Clears the result grouping and limit. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearResultGrouping() {
            resetIfBuilt();
            mGroupingTypeFlags = 0;
            mGroupingLimit = 0;
            return this;
        }

        /**
         * Sets property weights by schema type and property path.
         *
         * <p>Property weights are used to promote and demote query term matches within a {@link
         * GenericDocument} property when applying scoring.
         *
         * <p>Property weights must be positive values (greater than 0). A property's weight is
         * multiplied with that property's scoring contribution. This means weights set between 0.0
         * and 1.0 demote scoring contributions by a term match within the property. Weights set
         * above 1.0 promote scoring contributions by a term match within the property.
         *
         * <p>Properties that exist in the {@link AppSearchSchema}, but do not have a weight
         * explicitly set will be given a default weight of 1.0.
         *
         * <p>Weights set for property paths that do not exist in the {@link AppSearchSchema} will
         * be discarded and not affect scoring.
         *
         * <p><b>NOTE:</b> Property weights only affect scoring for query-dependent scoring
         * strategies, such as {@link #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *
         * @param schemaType the schema type to set property weights for.
         * @param propertyPathWeights a {@link Map} of property paths of the schema type to the
         *     weight to set for that property.
         * @throws IllegalArgumentException if a weight is equal to or less than 0.0.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder setPropertyWeights(
                @NonNull String schemaType, @NonNull Map<String, Double> propertyPathWeights) {
            Objects.requireNonNull(schemaType);
            Objects.requireNonNull(propertyPathWeights);

            Bundle propertyPathBundle = new Bundle();
            for (Map.Entry<String, Double> propertyPathWeightEntry :
                    propertyPathWeights.entrySet()) {
                String propertyPath = Objects.requireNonNull(propertyPathWeightEntry.getKey());
                Double weight = Objects.requireNonNull(propertyPathWeightEntry.getValue());
                if (weight <= 0.0) {
                    throw new IllegalArgumentException(
                            "Cannot set non-positive property weight "
                                    + "value "
                                    + weight
                                    + " for property path: "
                                    + propertyPath);
                }
                propertyPathBundle.putDouble(propertyPath, weight);
            }
            mTypePropertyWeights.putBundle(schemaType, propertyPathBundle);
            return this;
        }

        /** Clears the property weights for all schema types. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearPropertyWeights() {
            resetIfBuilt();
            mTypePropertyWeights.clear();
            return this;
        }

        /**
         * Specifies which documents to join with, and how to join.
         *
         * <p>If the ranking strategy is {@link #RANKING_STRATEGY_JOIN_AGGREGATE_SCORE}, and the
         * JoinSpec is null, {@link #build} will throw an {@link AppSearchException}.
         *
         * @param joinSpec a specification on how to perform the Join operation.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setJoinSpec(@NonNull JoinSpec joinSpec) {
            resetIfBuilt();
            mJoinSpec = Objects.requireNonNull(joinSpec);
            return this;
        }

        /** Clears the {@link JoinSpec}. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearJoinSpec() {
            resetIfBuilt();
            mJoinSpec = null;
            return this;
        }

        /**
         * Sets property weights by schema type and property path.
         *
         * <p>Property weights are used to promote and demote query term matches within a {@link
         * GenericDocument} property when applying scoring.
         *
         * <p>Property weights must be positive values (greater than 0). A property's weight is
         * multiplied with that property's scoring contribution. This means weights set between 0.0
         * and 1.0 demote scoring contributions by a term match within the property. Weights set
         * above 1.0 promote scoring contributions by a term match within the property.
         *
         * <p>Properties that exist in the {@link AppSearchSchema}, but do not have a weight
         * explicitly set will be given a default weight of 1.0.
         *
         * <p>Weights set for property paths that do not exist in the {@link AppSearchSchema} will
         * be discarded and not affect scoring.
         *
         * <p><b>NOTE:</b> Property weights only affect scoring for query-dependent scoring
         * strategies, such as {@link #RANKING_STRATEGY_RELEVANCE_SCORE}.
         *
         * @param schemaType the schema type to set property weights for.
         * @param propertyPathWeights a {@link Map} of property paths of the schema type to the
         *     weight to set for that property.
         * @throws IllegalArgumentException if a weight is equal to or less than 0.0.
         */
        @CanIgnoreReturnValue
        public @NonNull SearchSpec.Builder setPropertyWeightPaths(
                @NonNull String schemaType,
                @NonNull Map<PropertyPath, Double> propertyPathWeights) {
            Objects.requireNonNull(propertyPathWeights);

            Map<String, Double> propertyWeights = new ArrayMap<>(propertyPathWeights.size());
            for (Map.Entry<PropertyPath, Double> propertyPathWeightEntry :
                    propertyPathWeights.entrySet()) {
                PropertyPath propertyPath =
                        Objects.requireNonNull(propertyPathWeightEntry.getKey());
                propertyWeights.put(propertyPath.toString(), propertyPathWeightEntry.getValue());
            }
            return setPropertyWeights(schemaType, propertyWeights);
        }

        /**
         * Adds an embedding search to {@link SearchSpec} Entry, which will be referred in the query
         * expression and the ranking expression for embedding search.
         *
         * @see AppSearchSession#search
         * @see SearchSpec.Builder#setRankingStrategy(String)
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
        public @NonNull Builder addEmbeddingParameters(
                @NonNull EmbeddingVector... searchEmbeddings) {
            Objects.requireNonNull(searchEmbeddings);
            resetIfBuilt();
            return addEmbeddingParameters(Arrays.asList(searchEmbeddings));
        }

        /**
         * Adds an embedding search to {@link SearchSpec} Entry, which will be referred in the query
         * expression and the ranking expression for embedding search.
         *
         * @see AppSearchSession#search
         * @see SearchSpec.Builder#setRankingStrategy(String)
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
        public @NonNull Builder addEmbeddingParameters(
                @NonNull Collection<EmbeddingVector> searchEmbeddings) {
            Objects.requireNonNull(searchEmbeddings);
            resetIfBuilt();
            mEmbeddingParameters.addAll(searchEmbeddings);
            return this;
        }

        /** Clears the embedding parameters. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearEmbeddingParameters() {
            resetIfBuilt();
            mEmbeddingParameters.clear();
            return this;
        }

        /**
         * Sets the default embedding metric type used for embedding search (see {@link
         * AppSearchSession#search}) and ranking (see {@link
         * SearchSpec.Builder#setRankingStrategy(String)}).
         *
         * <p>If this method is not called, the default embedding search metric type is {@link
         * SearchSpec#EMBEDDING_SEARCH_METRIC_TYPE_COSINE}. Metrics specified within
         * "semanticSearch" or "matchedSemanticScores" functions in search/ranking expressions will
         * override this default.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
        public @NonNull Builder setDefaultEmbeddingSearchMetricType(
                @EmbeddingSearchMetricType int defaultEmbeddingSearchMetricType) {
            Preconditions.checkArgumentInRange(
                    defaultEmbeddingSearchMetricType,
                    EMBEDDING_SEARCH_METRIC_TYPE_COSINE,
                    EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN,
                    "Embedding search metric type");
            resetIfBuilt();
            mDefaultEmbeddingSearchMetricType = defaultEmbeddingSearchMetricType;
            return this;
        }

        /**
         * Adds Strings to the list of String parameters that can be referenced in the query through
         * the "getSearchStringParameter({index})" function.
         *
         * @see AppSearchSession#search
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
        public @NonNull Builder addSearchStringParameters(
                @NonNull String... searchStringParameters) {
            Objects.requireNonNull(searchStringParameters);
            resetIfBuilt();
            return addSearchStringParameters(Arrays.asList(searchStringParameters));
        }

        /**
         * Adds Strings to the list of String parameters that can be referenced in the query through
         * the "getSearchStringParameter({index})" function.
         *
         * @see AppSearchSession#search
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
        public @NonNull Builder addSearchStringParameters(
                @NonNull List<String> searchStringParameters) {
            Objects.requireNonNull(searchStringParameters);
            resetIfBuilt();
            mSearchStringParameters.addAll(searchStringParameters);
            return this;
        }

        /**
         * Clears the list of String parameters that can be referenced in the query through the
         * "getSearchStringParameter({index})" function.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        public @NonNull Builder clearSearchStringParameters() {
            resetIfBuilt();
            mSearchStringParameters.clear();
            return this;
        }

        /**
         * Sets the NUMERIC_SEARCH feature as enabled/disabled according to the enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         *     <p>If disabled, disallows use of {@link
         *     AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE} and all other numeric
         *     querying features.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumericSearchEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.NUMERIC_SEARCH, enabled);
            return this;
        }

        /**
         * Sets the VERBATIM_SEARCH feature as enabled/disabled according to the enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it
         *     <p>If disabled, disallows use of {@link
         *     AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_VERBATIM} and all other verbatim
         *     search features within the query language that allows clients to search using the
         *     verbatim string operator.
         *     <p>For example, The verbatim string operator '"foo/bar" OR baz' will ensure that
         *     'foo/bar' is treated as a single 'verbatim' token.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setVerbatimSearchEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.VERBATIM_SEARCH, enabled);
            return this;
        }

        /**
         * Sets the LIST_FILTER_QUERY_LANGUAGE feature as enabled/disabled according to the enabled
         * parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         *     <p>This feature covers the expansion of the query language to conform to the
         *     definition of the list filters language (https://aip.dev/160). This includes:
         *     <ul>
         *       <li>addition of explicit 'AND' and 'NOT' operators
         *       <li>property restricts are allowed with grouping (ex. "prop:(a OR b)")
         *       <li>addition of custom functions to control matching
         *     </ul>
         *     <p>The newly added custom functions covered by this feature are:
         *     <ul>
         *       <li>createList(String...)
         *       <li>termSearch(String, {@code List<String>})
         *     </ul>
         *     <p>createList takes a variable number of strings and returns a list of strings. It is
         *     for use with termSearch.
         *     <p>termSearch takes a query string that will be parsed according to the supported
         *     query language and an optional list of strings that specify the properties to be
         *     restricted to. This exists as a convenience for multiple property restricts. So, for
         *     example, the query "(subject:foo OR body:foo) (subject:bar OR body:bar)" could be
         *     rewritten as "termSearch(\"foo bar\", createList(\"subject\", \"bar\"))"
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setListFilterQueryLanguageEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.LIST_FILTER_QUERY_LANGUAGE, enabled);
            return this;
        }

        /**
         * Sets the LIST_FILTER_HAS_PROPERTY_FUNCTION feature as enabled/disabled according to the
         * enabled parameter.
         *
         * @param enabled Enables the feature if true, otherwise disables it
         *     <p>If disabled, disallows the use of the "hasProperty" function. See {@link
         *     AppSearchSession#search} for more details about the function.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION)
        public @NonNull Builder setListFilterHasPropertyFunctionEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.LIST_FILTER_HAS_PROPERTY_FUNCTION, enabled);
            return this;
        }

        /**
         * Sets the LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION feature as enabled/disabled
         * according to the enabled parameter.
         *
         * <p>If not enabled, the use of the "matchScoreExpression" function is disallowed. See
         * {@link AppSearchSession#search} for more details about the function.
         *
         * @param enabled Enables the feature if true, otherwise disables it
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION)
        public @NonNull Builder setListFilterMatchScoreExpressionFunctionEnabled(boolean enabled) {
            modifyEnabledFeature(
                    FeatureConstants.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION, enabled);
            return this;
        }

        /**
         * Sets the ScorablePropertyRanking feature as enabled or disabled.
         *
         * <p>If enabled, 'getScorableProperty' function can be used in the advanced ranking
         * expression. For details, see {@link SearchSpec.Builder#setRankingStrategy(String)}.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SCORABLE_PROPERTY)
        public @NonNull Builder setScorablePropertyRankingEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.SCHEMA_SCORABLE_PROPERTY_CONFIG, enabled);
            return this;
        }

        /**
         * Constructs a new {@link SearchSpec} from the contents of this builder.
         *
         * @throws IllegalArgumentException if property weights are provided with a ranking strategy
         *     that isn't RANKING_STRATEGY_RELEVANCE_SCORE.
         * @throws IllegalStateException if the ranking strategy is {@link
         *     #RANKING_STRATEGY_JOIN_AGGREGATE_SCORE} and {@link #setJoinSpec} has never been
         *     called.
         * @throws IllegalStateException if the aggregation scoring strategy has been set in {@link
         *     JoinSpec#getAggregationScoringStrategy()} but the ranking strategy is not {@link
         *     #RANKING_STRATEGY_JOIN_AGGREGATE_SCORE}.
         */
        public @NonNull SearchSpec build() {
            if (mJoinSpec != null) {
                if (mRankingStrategy != RANKING_STRATEGY_JOIN_AGGREGATE_SCORE
                        && mJoinSpec.getAggregationScoringStrategy()
                                != JoinSpec.AGGREGATION_SCORING_OUTER_RESULT_RANKING_SIGNAL) {
                    throw new IllegalStateException(
                            "Aggregate scoring strategy has been set in "
                                    + "the nested JoinSpec, but ranking strategy is not "
                                    + "RANKING_STRATEGY_JOIN_AGGREGATE_SCORE");
                }
            } else if (mRankingStrategy == RANKING_STRATEGY_JOIN_AGGREGATE_SCORE) {
                throw new IllegalStateException(
                        "Attempting to rank based on joined documents, but "
                                + "no JoinSpec provided");
            }
            if (!mTypePropertyWeights.isEmpty()
                    && mRankingStrategy != RANKING_STRATEGY_RELEVANCE_SCORE
                    && mRankingStrategy != RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION) {
                throw new IllegalArgumentException(
                        "Property weights are only compatible with the"
                            + " RANKING_STRATEGY_RELEVANCE_SCORE and"
                            + " RANKING_STRATEGY_ADVANCED_RANKING_EXPRESSION ranking strategies.");
            }

            mBuilt = true;
            return new SearchSpec(
                    mTermMatchType,
                    mSchemas,
                    mNamespaces,
                    mTypePropertyFilters,
                    mPackageNames,
                    mResultCountPerPage,
                    mRankingStrategy,
                    mOrder,
                    mSnippetCount,
                    mSnippetCountPerProperty,
                    mMaxSnippetSize,
                    mProjectionTypePropertyMasks,
                    mGroupingTypeFlags,
                    mGroupingLimit,
                    mTypePropertyWeights,
                    mJoinSpec,
                    mAdvancedRankingExpression,
                    new ArrayList<>(mEnabledFeatures),
                    mSearchSourceLogTag,
                    mEmbeddingParameters,
                    mDefaultEmbeddingSearchMetricType,
                    mInformationalRankingExpressions,
                    mSearchStringParameters,
                    mFilterDocumentIds);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mSchemas = new ArrayList<>(mSchemas);
                mTypePropertyFilters = BundleUtil.deepCopy(mTypePropertyFilters);
                mNamespaces = new ArrayList<>(mNamespaces);
                mPackageNames = new ArrayList<>(mPackageNames);
                mProjectionTypePropertyMasks = BundleUtil.deepCopy(mProjectionTypePropertyMasks);
                mTypePropertyWeights = BundleUtil.deepCopy(mTypePropertyWeights);
                mEmbeddingParameters = new ArrayList<>(mEmbeddingParameters);
                mInformationalRankingExpressions =
                        new ArrayList<>(mInformationalRankingExpressions);
                mSearchStringParameters = new ArrayList<>(mSearchStringParameters);
                mFilterDocumentIds = new ArrayList<>(mFilterDocumentIds);
                mBuilt = false;
            }
        }

        private void modifyEnabledFeature(@NonNull String feature, boolean enabled) {
            resetIfBuilt();
            if (enabled) {
                mEnabledFeatures.add(feature);
            } else {
                mEnabledFeatures.remove(feature);
            }
        }
    }
}
