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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.app.appsearch.util.BundleUtil;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents one of the results obtained from an AppSearch query.
 *
 * <p>This allows clients to obtain:
 *
 * <ul>
 *   <li>The document which matched, using {@link #getGenericDocument}
 *   <li>Information about which properties in the document matched, and "snippet" information
 *       containing textual summaries of the document's matches, using {@link #getMatchInfos}
 * </ul>
 *
 * <p>"Snippet" refers to a substring of text from the content of document that is returned as a
 * part of search result.
 *
 * @see SearchResults
 */
@SafeParcelable.Class(creator = "SearchResultCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class SearchResult extends AbstractSafeParcelable {

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<SearchResult> CREATOR =
            new SearchResultCreator();

    @Field(id = 1)
    final GenericDocumentParcel mDocument;

    @Field(id = 2)
    final List<MatchInfo> mMatchInfos;

    @Field(id = 3, getter = "getPackageName")
    private final String mPackageName;

    @Field(id = 4, getter = "getDatabaseName")
    private final String mDatabaseName;

    @Field(id = 5, getter = "getRankingSignal")
    private final double mRankingSignal;

    @Field(id = 6, getter = "getJoinedResults")
    private final List<SearchResult> mJoinedResults;

    @Field(id = 7, getter = "getInformationalRankingSignals")
    private final @NonNull List<Double> mInformationalRankingSignals;

    /**
     * Holds the map from schema type names to the list of their parent types.
     *
     * <p>The map includes entries for the {@link GenericDocument}'s own type and all of the nested
     * documents' types. Child types are guaranteed to appear before parent types in each list.
     *
     * <p>Parent types include transitive parents.
     *
     * <p>All schema names in this map are un-prefixed, for both keys and values.
     */
    @Field(id = 8)
    final @NonNull Bundle mParentTypeMap;

    /** Cache of the {@link GenericDocument}. Comes from mDocument at first use. */
    private @Nullable GenericDocument mDocumentCached;

    /** Cache of the inflated {@link MatchInfo}. Comes from inflating mMatchInfos at first use. */
    private @Nullable List<MatchInfo> mMatchInfosCached;

    /** @hide */
    @Constructor
    SearchResult(
            @Param(id = 1) @NonNull GenericDocumentParcel document,
            @Param(id = 2) @NonNull List<MatchInfo> matchInfos,
            @Param(id = 3) @NonNull String packageName,
            @Param(id = 4) @NonNull String databaseName,
            @Param(id = 5) double rankingSignal,
            @Param(id = 6) @NonNull List<SearchResult> joinedResults,
            @Param(id = 7) @Nullable List<Double> informationalRankingSignals,
            @Param(id = 8) @Nullable Bundle parentTypeMap) {
        mDocument = Objects.requireNonNull(document);
        mMatchInfos = Objects.requireNonNull(matchInfos);
        mPackageName = Objects.requireNonNull(packageName);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mRankingSignal = rankingSignal;
        mJoinedResults = Collections.unmodifiableList(Objects.requireNonNull(joinedResults));
        if (informationalRankingSignals != null) {
            mInformationalRankingSignals =
                    Collections.unmodifiableList(informationalRankingSignals);
        } else {
            mInformationalRankingSignals = Collections.emptyList();
        }
        if (parentTypeMap != null) {
            mParentTypeMap = parentTypeMap;
        } else {
            mParentTypeMap = Bundle.EMPTY;
        }
    }

    /**
     * Contains the matching {@link GenericDocument}.
     *
     * @return Document object which matched the query.
     */
    public @NonNull GenericDocument getGenericDocument() {
        if (mDocumentCached == null) {
            mDocumentCached = new GenericDocument(mDocument);
        }
        return mDocumentCached;
    }

    /**
     * Returns a list of {@link MatchInfo}s providing information about how the document in {@link
     * #getGenericDocument} matched the query.
     *
     * @return List of matches based on {@link SearchSpec}. If snippeting is disabled using {@link
     *     SearchSpec.Builder#setSnippetCount} or {@link
     *     SearchSpec.Builder#setSnippetCountPerProperty}, for all results after that value, this
     *     method returns an empty list.
     */
    public @NonNull List<MatchInfo> getMatchInfos() {
        if (mMatchInfosCached == null) {
            mMatchInfosCached = new ArrayList<>(mMatchInfos.size());
            for (int i = 0; i < mMatchInfos.size(); i++) {
                MatchInfo matchInfo = mMatchInfos.get(i);
                matchInfo.setDocument(getGenericDocument());
                if (mMatchInfosCached != null) {
                    // This additional check is added for NullnessChecker.
                    mMatchInfosCached.add(matchInfo);
                }
            }
            mMatchInfosCached = Collections.unmodifiableList(mMatchInfosCached);
        }
        // This check is added for NullnessChecker, mMatchInfos will always be NonNull.
        return Objects.requireNonNull(mMatchInfosCached);
    }

    /**
     * Contains the package name of the app that stored the {@link GenericDocument}.
     *
     * @return Package name that stored the document
     */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /**
     * Contains the database name that stored the {@link GenericDocument}.
     *
     * @return Name of the database within which the document is stored
     */
    public @NonNull String getDatabaseName() {
        return mDatabaseName;
    }

    /**
     * Returns the ranking signal of the {@link GenericDocument}, according to the ranking strategy
     * set in {@link SearchSpec.Builder#setRankingStrategy(int)}.
     *
     * <p>The meaning of the ranking signal and its value is determined by the selected ranking
     * strategy:
     *
     * <ul>
     *   <li>{@link SearchSpec#RANKING_STRATEGY_NONE} - this value will be 0
     *   <li>{@link SearchSpec#RANKING_STRATEGY_DOCUMENT_SCORE} - the value returned by calling
     *       {@link GenericDocument#getScore()} on the document returned by {@link
     *       #getGenericDocument()}
     *   <li>{@link SearchSpec#RANKING_STRATEGY_CREATION_TIMESTAMP} - the value returned by calling
     *       {@link GenericDocument#getCreationTimestampMillis()} on the document returned by {@link
     *       #getGenericDocument()}
     *   <li>{@link SearchSpec#RANKING_STRATEGY_RELEVANCE_SCORE} - an arbitrary double value where a
     *       higher value means more relevant
     *   <li>{@link SearchSpec#RANKING_STRATEGY_USAGE_COUNT} - the number of times usage has been
     *       reported for the document returned by {@link #getGenericDocument()}
     *   <li>{@link SearchSpec#RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP} - the timestamp of the
     *       most recent usage that has been reported for the document returned by {@link
     *       #getGenericDocument()}
     * </ul>
     *
     * @return Ranking signal of the document
     */
    public double getRankingSignal() {
        return mRankingSignal;
    }

    /**
     * Returns the informational ranking signals of the {@link GenericDocument}, according to the
     * expressions added in {@link SearchSpec.Builder#addInformationalRankingExpressions}.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public @NonNull List<Double> getInformationalRankingSignals() {
        return mInformationalRankingSignals;
    }

    /**
     * Returns the map from schema type names to the list of their parent types.
     *
     * <p>The map includes entries for the {@link GenericDocument}'s own type and all of the nested
     * documents' types. Child types are guaranteed to appear before parent types in each list.
     *
     * <p>Parent types include transitive parents.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned by this
     * function, rather than calling it multiple times.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES)
    public @NonNull Map<String, List<String>> getParentTypeMap() {
        Set<String> schemaTypes = mParentTypeMap.keySet();
        Map<String, List<String>> parentTypeMap = new ArrayMap<>(schemaTypes.size());
        for (String schemaType : schemaTypes) {
            ArrayList<String> parentTypes = mParentTypeMap.getStringArrayList(schemaType);
            if (parentTypes != null) {
                parentTypeMap.put(schemaType, parentTypes);
            }
        }
        return parentTypeMap;
    }

    /**
     * Gets a list of {@link SearchResult} joined from the join operation.
     *
     * <p>These joined documents match the outer document as specified in the {@link JoinSpec} with
     * parentPropertyExpression and childPropertyExpression. They are ordered according to the
     * {@link JoinSpec#getNestedSearchSpec}, and as many SearchResults as specified by {@link
     * JoinSpec#getMaxJoinedResultCount} will be returned. If no {@link JoinSpec} was specified,
     * this returns an empty list.
     *
     * <p>This method is inefficient to call repeatedly, as new {@link SearchResult} objects are
     * created each time.
     *
     * @return a List of SearchResults containing joined documents.
     */
    public @NonNull List<SearchResult> getJoinedResults() {
        return mJoinedResults;
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchResultCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link SearchResult} objects. */
    public static final class Builder {
        private final String mPackageName;
        private final String mDatabaseName;
        private List<MatchInfo> mMatchInfos = new ArrayList<>();
        private GenericDocument mGenericDocument;
        private double mRankingSignal;
        private List<Double> mInformationalRankingSignals = new ArrayList<>();
        private Bundle mParentTypeMap = new Bundle();
        private List<SearchResult> mJoinedResults = new ArrayList<>();
        private boolean mBuilt = false;

        /**
         * Constructs a new builder for {@link SearchResult} objects.
         *
         * @param packageName the package name the matched document belongs to
         * @param databaseName the database name the matched document belongs to.
         */
        public Builder(@NonNull String packageName, @NonNull String databaseName) {
            mPackageName = Objects.requireNonNull(packageName);
            mDatabaseName = Objects.requireNonNull(databaseName);
        }

        /** @hide */
        public Builder(@NonNull SearchResult searchResult) {
            Objects.requireNonNull(searchResult);
            mPackageName = searchResult.getPackageName();
            mDatabaseName = searchResult.getDatabaseName();
            mGenericDocument = searchResult.getGenericDocument();
            mRankingSignal = searchResult.getRankingSignal();
            mInformationalRankingSignals =
                    new ArrayList<>(searchResult.getInformationalRankingSignals());
            setParentTypeMap(searchResult.getParentTypeMap());
            List<MatchInfo> matchInfos = searchResult.getMatchInfos();
            for (int i = 0; i < matchInfos.size(); i++) {
                addMatchInfo(new MatchInfo.Builder(matchInfos.get(i)).build());
            }
            List<SearchResult> joinedResults = searchResult.getJoinedResults();
            for (int i = 0; i < joinedResults.size(); i++) {
                addJoinedResult(joinedResults.get(i));
            }
        }

        /** Sets the document which matched. */
        @CanIgnoreReturnValue
        public @NonNull Builder setGenericDocument(@NonNull GenericDocument document) {
            Objects.requireNonNull(document);
            resetIfBuilt();
            mGenericDocument = document;
            return this;
        }

        /** Adds another match to this SearchResult. */
        @CanIgnoreReturnValue
        public @NonNull Builder addMatchInfo(@NonNull MatchInfo matchInfo) {
            Preconditions.checkState(
                    matchInfo.mDocument == null,
                    "This MatchInfo is already associated with a SearchResult and can't be "
                            + "reassigned");
            resetIfBuilt();
            mMatchInfos.add(matchInfo);
            return this;
        }

        /** Sets the ranking signal of the matched document in this SearchResult. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRankingSignal(double rankingSignal) {
            resetIfBuilt();
            mRankingSignal = rankingSignal;
            return this;
        }

        /** Adds the informational ranking signal of the matched document in this SearchResult. */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
        public @NonNull Builder addInformationalRankingSignal(double rankingSignal) {
            resetIfBuilt();
            mInformationalRankingSignals.add(rankingSignal);
            return this;
        }

        /**
         * Sets the map from schema type names to the list of their parent types.
         *
         * <p>The map should include entries for the {@link GenericDocument}'s own type and all of
         * the nested documents' types.
         *
         * <p>Child types must appear before parent types in each list. Otherwise, the
         * GenericDocument's toDocumentClass method (an AndroidX-only API) may not correctly
         * identify the most concrete type. This could lead to unintended deserialization into a
         * more general type instead of a more specific type.
         *
         * <p>Parent types should include transitive parents.
         */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES)
        public @NonNull Builder setParentTypeMap(@NonNull Map<String, List<String>> parentTypeMap) {
            Objects.requireNonNull(parentTypeMap);
            resetIfBuilt();
            mParentTypeMap.clear();

            for (Map.Entry<String, List<String>> entry : parentTypeMap.entrySet()) {
                Objects.requireNonNull(entry.getKey());
                Objects.requireNonNull(entry.getValue());

                ArrayList<String> parentTypes = new ArrayList<>(entry.getValue().size());
                for (int i = 0; i < entry.getValue().size(); i++) {
                    String parentType = entry.getValue().get(i);
                    parentTypes.add(Objects.requireNonNull(parentType));
                }
                mParentTypeMap.putStringArrayList(entry.getKey(), parentTypes);
            }
            return this;
        }

        /**
         * Adds a {@link SearchResult} that was joined by the {@link JoinSpec}.
         *
         * @param joinedResult The joined SearchResult to add.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addJoinedResult(@NonNull SearchResult joinedResult) {
            resetIfBuilt();
            mJoinedResults.add(joinedResult);
            return this;
        }

        /**
         * Clears the {@link MatchInfo}s.
         *
         * @hide
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearMatchInfos() {
            resetIfBuilt();
            mMatchInfos.clear();
            return this;
        }

        /**
         * Clears the {@link SearchResult}s that were joined.
         *
         * @hide
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearJoinedResults() {
            resetIfBuilt();
            mJoinedResults.clear();
            return this;
        }

        /** Constructs a new {@link SearchResult}. */
        public @NonNull SearchResult build() {
            mBuilt = true;
            return new SearchResult(
                    mGenericDocument.getDocumentParcel(),
                    mMatchInfos,
                    mPackageName,
                    mDatabaseName,
                    mRankingSignal,
                    mJoinedResults,
                    mInformationalRankingSignals,
                    mParentTypeMap);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mMatchInfos = new ArrayList<>(mMatchInfos);
                mJoinedResults = new ArrayList<>(mJoinedResults);
                mInformationalRankingSignals = new ArrayList<>(mInformationalRankingSignals);
                mParentTypeMap = BundleUtil.deepCopy(mParentTypeMap);
                mBuilt = false;
            }
        }
    }

    /**
     * This class represents match objects for any snippets that might be present in {@link
     * SearchResults} from a query. Using this class, you can get:
     *
     * <ul>
     *   <li>the full text - all of the text in that String property
     *   <li>the exact term match - the 'term' (full word) that matched the query
     *   <li>the subterm match - the portion of the matched term that appears in the query
     *   <li>a suggested text snippet - a portion of the full text surrounding the exact term match,
     *       set to term boundaries. The size of the snippet is specified in {@link
     *       SearchSpec.Builder#setMaxSnippetSize}
     * </ul>
     *
     * for each match in the document.
     *
     * <p>Class Example 1:
     *
     * <p>A document contains the following text in property "subject":
     *
     * <p>"A commonly used fake word is foo. Another nonsense word that’s used a lot is bar."
     *
     * <p>If the queryExpression is "foo" and {@link SearchSpec#getMaxSnippetSize} is 10,
     *
     * <ul>
     *   <li>{@link MatchInfo#getPropertyPath()} returns "subject"
     *   <li>{@link MatchInfo#getFullText()} returns "A commonly used fake word is foo. Another
     *       nonsense word that’s used a lot is bar."
     *   <li>{@link MatchInfo#getExactMatchRange()} returns [29, 32]
     *   <li>{@link MatchInfo#getExactMatch()} returns "foo"
     *   <li>{@link MatchInfo#getSubmatchRange()} returns [29, 32]
     *   <li>{@link MatchInfo#getSubmatch()} returns "foo"
     *   <li>{@link MatchInfo#getSnippetRange()} returns [26, 33]
     *   <li>{@link MatchInfo#getSnippet()} returns "is foo."
     * </ul>
     *
     * <p>
     *
     * <p>Class Example 2:
     *
     * <p>A document contains one property named "subject" and one property named "sender" which
     * contains a "name" property.
     *
     * <p>In this case, we will have 2 property paths: {@code sender.name} and {@code subject}.
     *
     * <p>Let {@code sender.name = "Test Name Jr."} and {@code subject = "Testing 1 2 3"}
     *
     * <p>If the queryExpression is "Test" with {@link SearchSpec#TERM_MATCH_PREFIX} and {@link
     * SearchSpec#getMaxSnippetSize} is 10. We will have 2 matches:
     *
     * <p>Match-1
     *
     * <ul>
     *   <li>{@link MatchInfo#getPropertyPath()} returns "sender.name"
     *   <li>{@link MatchInfo#getFullText()} returns "Test Name Jr."
     *   <li>{@link MatchInfo#getExactMatchRange()} returns [0, 4]
     *   <li>{@link MatchInfo#getExactMatch()} returns "Test"
     *   <li>{@link MatchInfo#getSubmatchRange()} returns [0, 4]
     *   <li>{@link MatchInfo#getSubmatch()} returns "Test"
     *   <li>{@link MatchInfo#getSnippetRange()} returns [0, 9]
     *   <li>{@link MatchInfo#getSnippet()} returns "Test Name"
     * </ul>
     *
     * <p>Match-2
     *
     * <ul>
     *   <li>{@link MatchInfo#getPropertyPath()} returns "subject"
     *   <li>{@link MatchInfo#getFullText()} returns "Testing 1 2 3"
     *   <li>{@link MatchInfo#getExactMatchRange()} returns [0, 7]
     *   <li>{@link MatchInfo#getExactMatch()} returns "Testing"
     *   <li>{@link MatchInfo#getSubmatchRange()} returns [0, 4]
     *   <li>{@link MatchInfo#getSubmatch()} returns "Test"
     *   <li>{@link MatchInfo#getSnippetRange()} returns [0, 9]
     *   <li>{@link MatchInfo#getSnippet()} returns "Testing 1"
     * </ul>
     */
    @SafeParcelable.Class(creator = "MatchInfoCreator")
    @SuppressWarnings("HiddenSuperclass")
    public static final class MatchInfo extends AbstractSafeParcelable {

        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        public static final @NonNull Parcelable.Creator<MatchInfo> CREATOR = new MatchInfoCreator();

        /** The path of the matching snippet property. */
        @Field(id = 1, getter = "getPropertyPath")
        private final String mPropertyPath;

        @Field(id = 2)
        final int mExactMatchRangeStart;

        @Field(id = 3)
        final int mExactMatchRangeEnd;

        @Field(id = 4)
        final int mSubmatchRangeStart;

        @Field(id = 5)
        final int mSubmatchRangeEnd;

        @Field(id = 6)
        final int mSnippetRangeStart;

        @Field(id = 7)
        final int mSnippetRangeEnd;

        private @Nullable PropertyPath mPropertyPathObject = null;

        /**
         * Document which the match comes from.
         *
         * <p>If this is {@code null}, methods which require access to the document, like {@link
         * #getExactMatch}, will throw {@link NullPointerException}.
         */
        private @Nullable GenericDocument mDocument = null;

        /** Full text of the matched property. Populated on first use. */
        private @Nullable String mFullText;

        /** Range of property that exactly matched the query. Populated on first use. */
        private @Nullable MatchRange mExactMatchRangeCached;

        /**
         * Range of property that corresponds to the subsequence of the exact match that directly
         * matches a query term. Populated on first use.
         */
        private @Nullable MatchRange mSubmatchRangeCached;

        /** Range of some reasonable amount of context around the query. Populated on first use. */
        private @Nullable MatchRange mWindowRangeCached;

        @Constructor
        MatchInfo(
                @Param(id = 1) @NonNull String propertyPath,
                @Param(id = 2) int exactMatchRangeStart,
                @Param(id = 3) int exactMatchRangeEnd,
                @Param(id = 4) int submatchRangeStart,
                @Param(id = 5) int submatchRangeEnd,
                @Param(id = 6) int snippetRangeStart,
                @Param(id = 7) int snippetRangeEnd) {
            mPropertyPath = Objects.requireNonNull(propertyPath);
            mExactMatchRangeStart = exactMatchRangeStart;
            mExactMatchRangeEnd = exactMatchRangeEnd;
            mSubmatchRangeStart = submatchRangeStart;
            mSubmatchRangeEnd = submatchRangeEnd;
            mSnippetRangeStart = snippetRangeStart;
            mSnippetRangeEnd = snippetRangeEnd;
        }

        /**
         * Gets the property path corresponding to the given entry.
         *
         * <p>A property path is a '.' - delimited sequence of property names indicating which
         * property in the document these snippets correspond to.
         *
         * <p>Example properties: 'body', 'sender.name', 'sender.emailaddress', etc. For class
         * example 1 this returns "subject"
         */
        public @NonNull String getPropertyPath() {
            return mPropertyPath;
        }

        /**
         * Gets a {@link PropertyPath} object representing the property path corresponding to the
         * given entry.
         *
         * <p>Methods such as {@link GenericDocument#getPropertyDocument} accept a path as a string
         * rather than a {@link PropertyPath} object. However, you may want to manipulate the path
         * before getting a property document. This method returns a {@link PropertyPath} rather
         * than a String for easier path manipulation, which can then be converted to a String.
         *
         * @see #getPropertyPath
         * @see PropertyPath
         */
        public @NonNull PropertyPath getPropertyPathObject() {
            if (mPropertyPathObject == null) {
                mPropertyPathObject = new PropertyPath(mPropertyPath);
            }
            return mPropertyPathObject;
        }

        /**
         * Gets the full text corresponding to the given entry.
         *
         * <p>Class example 1: this returns "A commonly used fake word is foo. Another nonsense word
         * that's used a lot is bar."
         *
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test Name Jr." and,
         * for the second {@link MatchInfo}, this returns "Testing 1 2 3".
         */
        public @NonNull String getFullText() {
            if (mFullText == null) {
                if (mDocument == null) {
                    throw new IllegalStateException(
                            "Document has not been populated; this MatchInfo cannot be used yet");
                }
                mFullText = getPropertyValues(mDocument, mPropertyPath);
            }
            return mFullText;
        }

        /**
         * Gets the {@link MatchRange} of the exact term of the given entry that matched the query.
         *
         * <p>Class example 1: this returns [29, 32].
         *
         * <p>Class example 2: for the first {@link MatchInfo}, this returns [0, 4] and, for the
         * second {@link MatchInfo}, this returns [0, 7].
         */
        public @NonNull MatchRange getExactMatchRange() {
            if (mExactMatchRangeCached == null) {
                mExactMatchRangeCached = new MatchRange(mExactMatchRangeStart, mExactMatchRangeEnd);
            }
            return mExactMatchRangeCached;
        }

        /**
         * Gets the exact term of the given entry that matched the query.
         *
         * <p>Class example 1: this returns "foo".
         *
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test" and, for the
         * second {@link MatchInfo}, this returns "Testing".
         */
        public @NonNull CharSequence getExactMatch() {
            return getSubstring(getExactMatchRange());
        }

        /**
         * Gets the {@link MatchRange} of the exact term subsequence of the given entry that matched
         * the query.
         *
         * <p>Class example 1: this returns [29, 32].
         *
         * <p>Class example 2: for the first {@link MatchInfo}, this returns [0, 4] and, for the
         * second {@link MatchInfo}, this returns [0, 4].
         */
        public @NonNull MatchRange getSubmatchRange() {
            checkSubmatchSupported();
            if (mSubmatchRangeCached == null) {
                mSubmatchRangeCached = new MatchRange(mSubmatchRangeStart, mSubmatchRangeEnd);
            }
            return mSubmatchRangeCached;
        }

        /**
         * Gets the exact term subsequence of the given entry that matched the query.
         *
         * <p>Class example 1: this returns "foo".
         *
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test" and, for the
         * second {@link MatchInfo}, this returns "Test".
         */
        public @NonNull CharSequence getSubmatch() {
            checkSubmatchSupported();
            return getSubstring(getSubmatchRange());
        }

        /**
         * Gets the snippet {@link MatchRange} corresponding to the given entry.
         *
         * <p>Only populated when set maxSnippetSize > 0 in {@link
         * SearchSpec.Builder#setMaxSnippetSize}.
         *
         * <p>Class example 1: this returns [29, 41].
         *
         * <p>Class example 2: for the first {@link MatchInfo}, this returns [0, 9] and, for the
         * second {@link MatchInfo}, this returns [0, 13].
         */
        public @NonNull MatchRange getSnippetRange() {
            if (mWindowRangeCached == null) {
                mWindowRangeCached = new MatchRange(mSnippetRangeStart, mSnippetRangeEnd);
            }
            return mWindowRangeCached;
        }

        /**
         * Gets the snippet corresponding to the given entry.
         *
         * <p>Snippet - Provides a subset of the content to display. Only populated when requested
         * maxSnippetSize > 0. The size of this content can be changed by {@link
         * SearchSpec.Builder#setMaxSnippetSize}. Windowing is centered around the middle of the
         * matched token with content on either side clipped to token boundaries.
         *
         * <p>Class example 1: this returns "foo. Another".
         *
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test Name" and, for
         * the second {@link MatchInfo}, this returns "Testing 1 2 3".
         */
        public @NonNull CharSequence getSnippet() {
            return getSubstring(getSnippetRange());
        }

        private CharSequence getSubstring(MatchRange range) {
            return getFullText().substring(range.getStart(), range.getEnd());
        }

        private void checkSubmatchSupported() {
            if (mSubmatchRangeStart == -1) {
                throw new UnsupportedOperationException(
                        "Submatch is not supported with this backend/Android API level "
                                + "combination");
            }
        }

        /** Extracts the matching string from the document. */
        private static String getPropertyValues(GenericDocument document, String propertyName) {
            String result = document.getPropertyString(propertyName);
            if (result == null) {
                throw new IllegalStateException(
                        "No content found for requested property path: " + propertyName);
            }
            return result;
        }

        /**
         * Sets the {@link GenericDocument} for {@link MatchInfo}.
         *
         * <p>{@link MatchInfo} lacks a constructor that populates {@link MatchInfo#mDocument} This
         * provides the ability to set {@link MatchInfo#mDocument}
         */
        void setDocument(@NonNull GenericDocument document) {
            mDocument = document;
        }

        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            MatchInfoCreator.writeToParcel(this, dest, flags);
        }

        /** Builder for {@link MatchInfo} objects. */
        public static final class Builder {
            private final String mPropertyPath;
            private MatchRange mExactMatchRange = new MatchRange(0, 0);
            int mSubmatchRangeStart = -1;
            int mSubmatchRangeEnd = -1;
            private MatchRange mSnippetRange = new MatchRange(0, 0);

            /**
             * Creates a new {@link MatchInfo.Builder} reporting a match with the given property
             * path.
             *
             * <p>A property path is a dot-delimited sequence of property names indicating which
             * property in the document these snippets correspond to.
             *
             * <p>Example properties: 'body', 'sender.name', 'sender.emailaddress', etc. For class
             * example 1, this returns "subject".
             *
             * @param propertyPath A dot-delimited sequence of property names indicating which
             *     property in the document these snippets correspond to.
             */
            public Builder(@NonNull String propertyPath) {
                mPropertyPath = Objects.requireNonNull(propertyPath);
            }

            /** @hide */
            public Builder(@NonNull MatchInfo matchInfo) {
                Objects.requireNonNull(matchInfo);
                mPropertyPath = matchInfo.mPropertyPath;
                mExactMatchRange = matchInfo.getExactMatchRange();
                mSubmatchRangeStart = matchInfo.mSubmatchRangeStart;
                mSubmatchRangeEnd = matchInfo.mSubmatchRangeEnd;
                mSnippetRange = matchInfo.getSnippetRange();
            }

            /** Sets the exact {@link MatchRange} corresponding to the given entry. */
            @CanIgnoreReturnValue
            public @NonNull Builder setExactMatchRange(@NonNull MatchRange matchRange) {
                mExactMatchRange = Objects.requireNonNull(matchRange);
                return this;
            }

            /**
             * Sets the start and end of a submatch {@link MatchRange} corresponding to the given
             * entry.
             */
            @CanIgnoreReturnValue
            public @NonNull Builder setSubmatchRange(@NonNull MatchRange matchRange) {
                mSubmatchRangeStart = matchRange.getStart();
                mSubmatchRangeEnd = matchRange.getEnd();
                return this;
            }

            /** Sets the snippet {@link MatchRange} corresponding to the given entry. */
            @CanIgnoreReturnValue
            public @NonNull Builder setSnippetRange(@NonNull MatchRange matchRange) {
                mSnippetRange = Objects.requireNonNull(matchRange);
                return this;
            }

            /** Constructs a new {@link MatchInfo}. */
            public @NonNull MatchInfo build() {
                return new MatchInfo(
                        mPropertyPath,
                        mExactMatchRange.getStart(),
                        mExactMatchRange.getEnd(),
                        mSubmatchRangeStart,
                        mSubmatchRangeEnd,
                        mSnippetRange.getStart(),
                        mSnippetRange.getEnd());
            }
        }
    }

    /**
     * Class providing the position range of matching information.
     *
     * <p>All ranges are finite, and the left side of the range is always {@code <=} the right side
     * of the range.
     *
     * <p>Example: MatchRange(0, 100) represents hundred ints from 0 to 99."
     */
    public static final class MatchRange {
        private final int mEnd;
        private final int mStart;

        /**
         * Creates a new immutable range.
         *
         * <p>The endpoints are {@code [start, end)}; that is the range is bounded. {@code start}
         * must be lesser or equal to {@code end}.
         *
         * @param start The start point (inclusive)
         * @param end The end point (exclusive)
         */
        public MatchRange(int start, int end) {
            if (start > end) {
                throw new IllegalArgumentException(
                        "Start point must be less than or equal to " + "end point");
            }
            mStart = start;
            mEnd = end;
        }

        /** Gets the start point (inclusive). */
        public int getStart() {
            return mStart;
        }

        /** Gets the end point (exclusive). */
        public int getEnd() {
            return mEnd;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MatchRange)) {
                return false;
            }
            MatchRange otherMatchRange = (MatchRange) other;
            return this.getStart() == otherMatchRange.getStart()
                    && this.getEnd() == otherMatchRange.getEnd();
        }

        @Override
        public @NonNull String toString() {
            return "MatchRange { start: " + mStart + " , end: " + mEnd + "}";
        }

        @Override
        public int hashCode() {
            return Objects.hash(mStart, mEnd);
        }
    }
}
