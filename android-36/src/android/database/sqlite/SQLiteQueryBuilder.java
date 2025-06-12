/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.database.sqlite;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.util.ArrayUtils;

import libcore.util.EmptyArray;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a convenience class that helps build SQL queries to be sent to
 * {@link SQLiteDatabase} objects.
 * <p>
 * This class is often used to compose a SQL query from client-supplied fragments.  Best practice
 * to protect against invalid or illegal SQL is to set the following:
 * <ul>
 * <li>{@link #setStrict} true.
 * <li>{@link #setProjectionMap} with the list of queryable columns.
 * <li>{@link #setStrictColumns} true.
 * <li>{@link #setStrictGrammar} true.
 * </ul>
 */
public class SQLiteQueryBuilder {
    private static final String TAG = "SQLiteQueryBuilder";

    private static final Pattern sAggregationPattern = Pattern.compile(
            "(?i)(AVG|COUNT|MAX|MIN|SUM|TOTAL|GROUP_CONCAT)\\((.+)\\)");

    private Map<String, String> mProjectionMap = null;
    private Collection<Pattern> mProjectionGreylist = null;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private String mTables = "";
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private StringBuilder mWhereClause = null;  // lazily created
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private boolean mDistinct;
    private SQLiteDatabase.CursorFactory mFactory;

    private static final int STRICT_PARENTHESES = 1 << 0;
    private static final int STRICT_COLUMNS = 1 << 1;
    private static final int STRICT_GRAMMAR = 1 << 2;

    private int mStrictFlags;

    public SQLiteQueryBuilder() {
        mDistinct = false;
        mFactory = null;
    }

    /**
     * Mark the query as {@code DISTINCT}.
     *
     * @param distinct if true the query is {@code DISTINCT}, otherwise it isn't
     */
    public void setDistinct(boolean distinct) {
        mDistinct = distinct;
    }

    /**
     * Get if the query is marked as {@code DISTINCT}, as last configured by
     * {@link #setDistinct(boolean)}.
     */
    public boolean isDistinct() {
        return mDistinct;
    }

    /**
     * Returns the list of tables being queried
     *
     * @return the list of tables being queried
     */
    public @Nullable String getTables() {
        return mTables;
    }

    /**
     * Sets the list of tables to query. Multiple tables can be specified to perform a join.
     * For example:
     *   setTables("foo, bar")
     *   setTables("foo LEFT OUTER JOIN bar ON (foo.id = bar.foo_id)")
     *
     * @param inTables the list of tables to query on
     */
    public void setTables(@Nullable String inTables) {
        mTables = inTables;
    }

    /**
     * Append a chunk to the {@code WHERE} clause of the query. All chunks appended are surrounded
     * by parenthesis and {@code AND}ed with the selection passed to {@link #query}. The final
     * {@code WHERE} clause looks like:
     * <p>
     * WHERE (&lt;append chunk 1>&lt;append chunk2>) AND (&lt;query() selection parameter>)
     *
     * @param inWhere the chunk of text to append to the {@code WHERE} clause.
     */
    public void appendWhere(@NonNull CharSequence inWhere) {
        if (mWhereClause == null) {
            mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        mWhereClause.append(inWhere);
    }

    /**
     * Append a chunk to the {@code WHERE} clause of the query. All chunks appended are surrounded
     * by parenthesis and ANDed with the selection passed to {@link #query}. The final
     * {@code WHERE} clause looks like:
     * <p>
     * WHERE (&lt;append chunk 1>&lt;append chunk2>) AND (&lt;query() selection parameter>)
     *
     * @param inWhere the chunk of text to append to the {@code WHERE} clause. it will be escaped
     * to avoid SQL injection attacks
     */
    public void appendWhereEscapeString(@NonNull String inWhere) {
        if (mWhereClause == null) {
            mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        DatabaseUtils.appendEscapedSQLString(mWhereClause, inWhere);
    }

    /**
     * Add a standalone chunk to the {@code WHERE} clause of this query.
     * <p>
     * This method differs from {@link #appendWhere(CharSequence)} in that it
     * automatically appends {@code AND} to any existing {@code WHERE} clause
     * already under construction before appending the given standalone
     * expression wrapped in parentheses.
     *
     * @param inWhere the standalone expression to append to the {@code WHERE}
     *            clause. It will be wrapped in parentheses when it's appended.
     */
    public void appendWhereStandalone(@NonNull CharSequence inWhere) {
        if (mWhereClause == null) {
            mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        if (mWhereClause.length() > 0) {
            mWhereClause.append(" AND ");
        }
        mWhereClause.append('(').append(inWhere).append(')');
    }

    /**
     * Sets the projection map for the query.  The projection map maps
     * from column names that the caller passes into query to database
     * column names. This is useful for renaming columns as well as
     * disambiguating column names when doing joins. For example you
     * could map "name" to "people.name".  If a projection map is set
     * it must contain all column names the user may request, even if
     * the key and value are the same.
     *
     * @param columnMap maps from the user column names to the database column names
     */
    public void setProjectionMap(@Nullable Map<String, String> columnMap) {
        mProjectionMap = columnMap;
    }

    /**
     * Gets the projection map for the query, as last configured by
     * {@link #setProjectionMap(Map)}.
     */
    public @Nullable Map<String, String> getProjectionMap() {
        return mProjectionMap;
    }

    /**
     * Sets a projection greylist of columns that will be allowed through, even
     * when {@link #setStrict(boolean)} is enabled. This provides a way for
     * abusive custom columns like {@code COUNT(*)} to continue working.
     */
    public void setProjectionGreylist(@Nullable Collection<Pattern> projectionGreylist) {
        mProjectionGreylist = projectionGreylist;
    }

    /**
     * Gets the projection greylist for the query, as last configured by
     * {@link #setProjectionGreylist}.
     */
    public @Nullable Collection<Pattern> getProjectionGreylist() {
        return mProjectionGreylist;
    }

    /** {@hide} */
    @Deprecated
    public void setProjectionAggregationAllowed(boolean projectionAggregationAllowed) {
    }

    /** {@hide} */
    @Deprecated
    public boolean isProjectionAggregationAllowed() {
        return true;
    }

    /**
     * Sets the cursor factory to be used for the query.  You can use
     * one factory for all queries on a database but it is normally
     * easier to specify the factory when doing this query.
     *
     * @param factory the factory to use.
     */
    public void setCursorFactory(@Nullable SQLiteDatabase.CursorFactory factory) {
        mFactory = factory;
    }

    /**
     * Gets the cursor factory to be used for the query, as last configured by
     * {@link #setCursorFactory(android.database.sqlite.SQLiteDatabase.CursorFactory)}.
     */
    public @Nullable SQLiteDatabase.CursorFactory getCursorFactory() {
        return mFactory;
    }

    /**
     * When set, the selection is verified against malicious arguments. When
     * using this class to create a statement using
     * {@link #buildQueryString(boolean, String, String[], String, String, String, String, String)},
     * non-numeric limits will raise an exception. If a projection map is
     * specified, fields not in that map will be ignored. If this class is used
     * to execute the statement directly using
     * {@link #query(SQLiteDatabase, String[], String, String[], String, String, String)}
     * or
     * {@link #query(SQLiteDatabase, String[], String, String[], String, String, String, String)},
     * additionally also parenthesis escaping selection are caught. To
     * summarize: To get maximum protection against malicious third party apps
     * (for example content provider consumers), make sure to do the following:
     * <ul>
     * <li>Set this value to true</li>
     * <li>Use a projection map</li>
     * <li>Use one of the query overloads instead of getting the statement as a
     * sql string</li>
     * </ul>
     * <p>
     * This feature is disabled by default on each newly constructed
     * {@link SQLiteQueryBuilder} and needs to be manually enabled.
     */
    public void setStrict(boolean strict) {
        if (strict) {
            mStrictFlags |= STRICT_PARENTHESES;
        } else {
            mStrictFlags &= ~STRICT_PARENTHESES;
        }
    }

    /**
     * Get if the query is marked as strict, as last configured by
     * {@link #setStrict(boolean)}.
     */
    public boolean isStrict() {
        return (mStrictFlags & STRICT_PARENTHESES) != 0;
    }

    /**
     * When enabled, verify that all projections and {@link ContentValues} only
     * contain valid columns as defined by {@link #setProjectionMap(Map)}.
     * <p>
     * This enforcement applies to {@link #insert}, {@link #query}, and
     * {@link #update} operations. Any enforcement failures will throw an
     * {@link IllegalArgumentException}.
     * <p>
     * This feature is disabled by default on each newly constructed
     * {@link SQLiteQueryBuilder} and needs to be manually enabled.
     */
    public void setStrictColumns(boolean strictColumns) {
        if (strictColumns) {
            mStrictFlags |= STRICT_COLUMNS;
        } else {
            mStrictFlags &= ~STRICT_COLUMNS;
        }
    }

    /**
     * Get if the query is marked as strict, as last configured by
     * {@link #setStrictColumns(boolean)}.
     */
    public boolean isStrictColumns() {
        return (mStrictFlags & STRICT_COLUMNS) != 0;
    }

    /**
     * When enabled, verify that all untrusted SQL conforms to a restricted SQL
     * grammar. Here are the restrictions applied:
     * <ul>
     * <li>In {@code WHERE} and {@code HAVING} clauses: subqueries, raising, and
     * windowing terms are rejected.
     * <li>In {@code GROUP BY} clauses: only valid columns are allowed.
     * <li>In {@code ORDER BY} clauses: only valid columns, collation, and
     * ordering terms are allowed.
     * <li>In {@code LIMIT} clauses: only numerical values and offset terms are
     * allowed.
     * </ul>
     * All column references must be valid as defined by
     * {@link #setProjectionMap(Map)}.
     * <p>
     * This enforcement applies to {@link #query}, {@link #update} and
     * {@link #delete} operations. This enforcement does not apply to trusted
     * inputs, such as those provided by {@link #appendWhere}. Any enforcement
     * failures will throw an {@link IllegalArgumentException}.
     * <p>
     * This feature is disabled by default on each newly constructed
     * {@link SQLiteQueryBuilder} and needs to be manually enabled.
     */
    public void setStrictGrammar(boolean strictGrammar) {
        if (strictGrammar) {
            mStrictFlags |= STRICT_GRAMMAR;
        } else {
            mStrictFlags &= ~STRICT_GRAMMAR;
        }
    }

    /**
     * Get if the query is marked as strict, as last configured by
     * {@link #setStrictGrammar(boolean)}.
     */
    public boolean isStrictGrammar() {
        return (mStrictFlags & STRICT_GRAMMAR) != 0;
    }

    /**
     * Build an SQL query string from the given clauses.
     *
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param tables The table names to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param where A filter declaring which rows to return, formatted as an SQL
     *            {@code WHERE} clause (excluding the {@code WHERE} itself). Passing {@code null} will
     *            return all rows for the given URL.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            {@code GROUP BY} clause (excluding the {@code GROUP BY} itself). Passing {@code null}
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL {@code HAVING}
     *            clause (excluding the {@code HAVING} itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL {@code ORDER BY} clause
     *            (excluding the {@code ORDER BY} itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *            formatted as {@code LIMIT} clause. Passing null denotes no {@code LIMIT} clause.
     * @return the SQL query string
     */
    public static String buildQueryString(
            boolean distinct, String tables, String[] columns, String where,
            String groupBy, String having, String orderBy, String limit) {
        if (TextUtils.isEmpty(groupBy) && !TextUtils.isEmpty(having)) {
            throw new IllegalArgumentException(
                    "HAVING clauses are only permitted when using a groupBy clause");
        }

        StringBuilder query = new StringBuilder(120);

        query.append("SELECT ");
        if (distinct) {
            query.append("DISTINCT ");
        }
        if (columns != null && columns.length != 0) {
            appendColumns(query, columns);
        } else {
            query.append("* ");
        }
        query.append("FROM ");
        query.append(tables);
        appendClause(query, " WHERE ", where);
        appendClause(query, " GROUP BY ", groupBy);
        appendClause(query, " HAVING ", having);
        appendClause(query, " ORDER BY ", orderBy);
        appendClause(query, " LIMIT ", limit);

        return query.toString();
    }

    private static void appendClause(StringBuilder s, String name, String clause) {
        if (!TextUtils.isEmpty(clause)) {
            s.append(name);
            s.append(clause);
        }
    }

    /**
     * Add the names that are non-null in columns to s, separating
     * them with commas.
     */
    public static void appendColumns(StringBuilder s, String[] columns) {
        int n = columns.length;

        for (int i = 0; i < n; i++) {
            String column = columns[i];

            if (column != null) {
                if (i > 0) {
                    s.append(", ");
                }
                s.append(column);
            }
        }
        s.append(' ');
    }

    /**
     * Perform a query by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to query on
     * @param projectionIn A list of which columns to return. Passing
     *   null will return all columns, which is discouraged to prevent
     *   reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL {@code WHERE} clause (excluding the {@code WHERE}
     *   itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     *   will be replaced by the values from selectionArgs, in order
     *   that they appear in the selection. The values will be bound
     *   as Strings.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL {@code GROUP BY} clause (excluding the {@code GROUP BY}
     *   itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL {@code HAVING} clause (excluding the {@code HAVING} itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     *   {@code ORDER BY} clause (excluding the {@code ORDER BY} itself). Passing null
     *   will use the default sort order, which may be unordered.
     * @return a cursor over the result set
     * @see android.content.ContentResolver#query(android.net.Uri, String[],
     *      String, String[], String)
     */
    public Cursor query(SQLiteDatabase db, String[] projectionIn,
            String selection, String[] selectionArgs, String groupBy,
            String having, String sortOrder) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder,
                null /* limit */, null /* cancellationSignal */);
    }

    /**
     * Perform a query by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to query on
     * @param projectionIn A list of which columns to return. Passing
     *   null will return all columns, which is discouraged to prevent
     *   reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL {@code WHERE} clause (excluding the {@code WHERE}
     *   itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     *   will be replaced by the values from selectionArgs, in order
     *   that they appear in the selection. The values will be bound
     *   as Strings.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL {@code GROUP BY} clause (excluding the {@code GROUP BY}
     *   itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL {@code HAVING} clause (excluding the {@code HAVING} itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     *   {@code ORDER BY} clause (excluding the {@code ORDER BY} itself). Passing null
     *   will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *   formatted as {@code LIMIT} clause. Passing null denotes no {@code LIMIT} clause.
     * @return a cursor over the result set
     * @see android.content.ContentResolver#query(android.net.Uri, String[],
     *      String, String[], String)
     */
    public Cursor query(SQLiteDatabase db, String[] projectionIn,
            String selection, String[] selectionArgs, String groupBy,
            String having, String sortOrder, String limit) {
        return query(db, projectionIn, selection, selectionArgs,
                groupBy, having, sortOrder, limit, null);
    }

    /**
     * Perform a query by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to query on
     * @param projectionIn A list of which columns to return. Passing
     *   null will return all columns, which is discouraged to prevent
     *   reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL {@code WHERE} clause (excluding the {@code WHERE}
     *   itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     *   will be replaced by the values from selectionArgs, in order
     *   that they appear in the selection. The values will be bound
     *   as Strings.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL {@code GROUP BY} clause (excluding the {@code GROUP BY}
     *   itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL {@code HAVING} clause (excluding the {@code HAVING} itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     *   {@code ORDER BY} clause (excluding the {@code ORDER BY} itself). Passing null
     *   will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *   formatted as {@code LIMIT} clause. Passing null denotes no {@code LIMIT} clause.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then {@link OperationCanceledException} will be thrown
     * when the query is executed.
     * @return a cursor over the result set
     * @see android.content.ContentResolver#query(android.net.Uri, String[],
     *      String, String[], String)
     */
    public Cursor query(SQLiteDatabase db, String[] projectionIn,
            String selection, String[] selectionArgs, String groupBy,
            String having, String sortOrder, String limit, CancellationSignal cancellationSignal) {
        if (mTables == null) {
            return null;
        }

        final String sql;
        final String unwrappedSql = buildQuery(
                projectionIn, selection, groupBy, having,
                sortOrder, limit);

        if (isStrictColumns()) {
            enforceStrictColumns(projectionIn);
        }
        if (isStrictGrammar()) {
            enforceStrictGrammar(selection, groupBy, having, sortOrder, limit);
        }
        if (isStrict()) {
            // Validate the user-supplied selection to detect syntactic anomalies
            // in the selection string that could indicate a SQL injection attempt.
            // The idea is to ensure that the selection clause is a valid SQL expression
            // by compiling it twice: once wrapped in parentheses and once as
            // originally specified. An attacker cannot create an expression that
            // would escape the SQL expression while maintaining balanced parentheses
            // in both the wrapped and original forms.

            // NOTE: The ordering of the below operations is important; we must
            // execute the wrapped query to ensure the untrusted clause has been
            // fully isolated.

            // Validate the unwrapped query
            db.validateSql(unwrappedSql, cancellationSignal); // will throw if query is invalid

            // Execute wrapped query for extra protection
            final String wrappedSql = buildQuery(projectionIn, wrap(selection), groupBy,
                    wrap(having), sortOrder, limit);
            sql = wrappedSql;
        } else {
            // Execute unwrapped query
            sql = unwrappedSql;
        }

        final String[] sqlArgs = selectionArgs;
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            if (Build.IS_DEBUGGABLE) {
                Log.d(TAG, sql + " with args " + Arrays.toString(sqlArgs));
            } else {
                Log.d(TAG, sql);
            }
        }
        return db.rawQueryWithFactory(
                mFactory, sql, sqlArgs,
                SQLiteDatabase.findEditTable(mTables),
                cancellationSignal); // will throw if query is invalid
    }

    /**
     * Perform an insert by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to insert on
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    public long insert(@NonNull SQLiteDatabase db, @NonNull ContentValues values) {
        Objects.requireNonNull(mTables, "No tables defined");
        Objects.requireNonNull(db, "No database defined");
        Objects.requireNonNull(values, "No values defined");

        if (isStrictColumns()) {
            enforceStrictColumns(values);
        }

        final String sql = buildInsert(values);

        final ArrayMap<String, Object> rawValues = values.getValues();
        final int valuesLength = rawValues.size();
        final Object[] sqlArgs = new Object[valuesLength];
        for (int i = 0; i < sqlArgs.length; i++) {
            sqlArgs[i] = rawValues.valueAt(i);
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            if (Build.IS_DEBUGGABLE) {
                Log.d(TAG, sql + " with args " + Arrays.toString(sqlArgs));
            } else {
                Log.d(TAG, sql);
            }
        }
        return DatabaseUtils.executeInsert(db, sql, sqlArgs);
    }

    /**
     * Perform an update by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to update on
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL {@code WHERE} clause (excluding the {@code WHERE}
     *   itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     *   will be replaced by the values from selectionArgs, in order
     *   that they appear in the selection. The values will be bound
     *   as Strings.
     * @return the number of rows updated
     */
    public int update(@NonNull SQLiteDatabase db, @NonNull ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        Objects.requireNonNull(mTables, "No tables defined");
        Objects.requireNonNull(db, "No database defined");
        Objects.requireNonNull(values, "No values defined");

        final String sql;
        final String unwrappedSql = buildUpdate(values, selection);

        if (isStrictColumns()) {
            enforceStrictColumns(values);
        }
        if (isStrictGrammar()) {
            enforceStrictGrammar(selection, null, null, null, null);
        }
        if (isStrict()) {
            // Validate the user-supplied selection to detect syntactic anomalies
            // in the selection string that could indicate a SQL injection attempt.
            // The idea is to ensure that the selection clause is a valid SQL expression
            // by compiling it twice: once wrapped in parentheses and once as
            // originally specified. An attacker cannot create an expression that
            // would escape the SQL expression while maintaining balanced parentheses
            // in both the wrapped and original forms.

            // NOTE: The ordering of the below operations is important; we must
            // execute the wrapped query to ensure the untrusted clause has been
            // fully isolated.

            // Validate the unwrapped query
            db.validateSql(unwrappedSql, null); // will throw if query is invalid

            // Execute wrapped query for extra protection
            final String wrappedSql = buildUpdate(values, wrap(selection));
            sql = wrappedSql;
        } else {
            // Execute unwrapped query
            sql = unwrappedSql;
        }

        if (selectionArgs == null) {
            selectionArgs = EmptyArray.STRING;
        }
        final ArrayMap<String, Object> rawValues = values.getValues();
        final int valuesLength = rawValues.size();
        final Object[] sqlArgs = new Object[valuesLength + selectionArgs.length];
        for (int i = 0; i < sqlArgs.length; i++) {
            if (i < valuesLength) {
                sqlArgs[i] = rawValues.valueAt(i);
            } else {
                sqlArgs[i] = selectionArgs[i - valuesLength];
            }
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            if (Build.IS_DEBUGGABLE) {
                Log.d(TAG, sql + " with args " + Arrays.toString(sqlArgs));
            } else {
                Log.d(TAG, sql);
            }
        }
        return DatabaseUtils.executeUpdateDelete(db, sql, sqlArgs);
    }

    /**
     * Perform a delete by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to delete on
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL {@code WHERE} clause (excluding the {@code WHERE}
     *   itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     *   will be replaced by the values from selectionArgs, in order
     *   that they appear in the selection. The values will be bound
     *   as Strings.
     * @return the number of rows deleted
     */
    public int delete(@NonNull SQLiteDatabase db, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        Objects.requireNonNull(mTables, "No tables defined");
        Objects.requireNonNull(db, "No database defined");

        final String sql;
        final String unwrappedSql = buildDelete(selection);

        if (isStrictGrammar()) {
            enforceStrictGrammar(selection, null, null, null, null);
        }
        if (isStrict()) {
            // Validate the user-supplied selection to detect syntactic anomalies
            // in the selection string that could indicate a SQL injection attempt.
            // The idea is to ensure that the selection clause is a valid SQL expression
            // by compiling it twice: once wrapped in parentheses and once as
            // originally specified. An attacker cannot create an expression that
            // would escape the SQL expression while maintaining balanced parentheses
            // in both the wrapped and original forms.

            // NOTE: The ordering of the below operations is important; we must
            // execute the wrapped query to ensure the untrusted clause has been
            // fully isolated.

            // Validate the unwrapped query
            db.validateSql(unwrappedSql, null); // will throw if query is invalid

            // Execute wrapped query for extra protection
            final String wrappedSql = buildDelete(wrap(selection));
            sql = wrappedSql;
        } else {
            // Execute unwrapped query
            sql = unwrappedSql;
        }

        final String[] sqlArgs = selectionArgs;
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            if (Build.IS_DEBUGGABLE) {
                Log.d(TAG, sql + " with args " + Arrays.toString(sqlArgs));
            } else {
                Log.d(TAG, sql);
            }
        }
        return DatabaseUtils.executeUpdateDelete(db, sql, sqlArgs);
    }

    private void enforceStrictColumns(@Nullable String[] projection) {
        Objects.requireNonNull(mProjectionMap, "No projection map defined");

        computeProjection(projection);
    }

    private void enforceStrictColumns(@NonNull ContentValues values) {
        Objects.requireNonNull(mProjectionMap, "No projection map defined");

        final ArrayMap<String, Object> rawValues = values.getValues();
        for (int i = 0; i < rawValues.size(); i++) {
            final String column = rawValues.keyAt(i);
            if (!mProjectionMap.containsKey(column)) {
                throw new IllegalArgumentException("Invalid column " + column);
            }
        }
    }

    private void enforceStrictGrammar(@Nullable String selection, @Nullable String groupBy,
            @Nullable String having, @Nullable String sortOrder, @Nullable String limit) {
        SQLiteTokenizer.tokenize(selection, SQLiteTokenizer.OPTION_NONE,
                this::enforceStrictToken);
        SQLiteTokenizer.tokenize(groupBy, SQLiteTokenizer.OPTION_NONE,
                this::enforceStrictToken);
        SQLiteTokenizer.tokenize(having, SQLiteTokenizer.OPTION_NONE,
                this::enforceStrictToken);
        SQLiteTokenizer.tokenize(sortOrder, SQLiteTokenizer.OPTION_NONE,
                this::enforceStrictToken);
        SQLiteTokenizer.tokenize(limit, SQLiteTokenizer.OPTION_NONE,
                this::enforceStrictToken);
    }

    private void enforceStrictToken(@NonNull String token) {
        if (TextUtils.isEmpty(token)) return;
        if (isTableOrColumn(token)) return;
        if (SQLiteTokenizer.isFunction(token)) return;
        if (SQLiteTokenizer.isType(token)) return;

        // Carefully block any tokens that are attempting to jump across query
        // clauses or create subqueries, since they could leak data that should
        // have been filtered by the trusted where clause
        boolean isAllowedKeyword = SQLiteTokenizer.isKeyword(token);
        switch (token.toUpperCase(Locale.US)) {
            case "SELECT":
            case "FROM":
            case "WHERE":
            case "GROUP":
            case "HAVING":
            case "WINDOW":
            case "VALUES":
            case "ORDER":
            case "LIMIT":
                isAllowedKeyword = false;
                break;
        }
        if (!isAllowedKeyword) {
            throw new IllegalArgumentException("Invalid token " + token);
        }
    }

    /**
     * Construct a {@code SELECT} statement suitable for use in a group of
     * {@code SELECT} statements that will be joined through {@code UNION} operators
     * in buildUnionQuery.
     *
     * @param projectionIn A list of which columns to return. Passing
     *    null will return all columns, which is discouraged to
     *    prevent reading data from storage that isn't going to be
     *    used.
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL {@code WHERE} clause (excluding the {@code WHERE}
     *   itself).  Passing null will return all rows for the given
     *   URL.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL {@code GROUP BY} clause (excluding the {@code GROUP BY} itself).
     *   Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL {@code HAVING} clause (excluding the {@code HAVING} itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     *   {@code ORDER BY} clause (excluding the {@code ORDER BY} itself). Passing null
     *   will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *   formatted as {@code LIMIT} clause. Passing null denotes no {@code LIMIT} clause.
     * @return the resulting SQL {@code SELECT} statement
     */
    public String buildQuery(
            String[] projectionIn, String selection, String groupBy,
            String having, String sortOrder, String limit) {
        String[] projection = computeProjection(projectionIn);
        String where = computeWhere(selection);

        return buildQueryString(
                mDistinct, mTables, projection, where,
                groupBy, having, sortOrder, limit);
    }

    /**
     * @deprecated This method's signature is misleading since no SQL parameter
     * substitution is carried out.  The selection arguments parameter does not get
     * used at all.  To avoid confusion, call
     * {@link #buildQuery(String[], String, String, String, String, String)} instead.
     */
    @Deprecated
    public String buildQuery(
            String[] projectionIn, String selection, String[] selectionArgs,
            String groupBy, String having, String sortOrder, String limit) {
        return buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
    }

    /** {@hide} */
    public String buildInsert(ContentValues values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Empty values");
        }

        StringBuilder sql = new StringBuilder(120);
        sql.append("INSERT INTO ");
        sql.append(SQLiteDatabase.findEditTable(mTables));
        sql.append(" (");

        final ArrayMap<String, Object> rawValues = values.getValues();
        for (int i = 0; i < rawValues.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(rawValues.keyAt(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < rawValues.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
        }
        sql.append(")");
        return sql.toString();
    }

    /** {@hide} */
    public String buildUpdate(ContentValues values, String selection) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Empty values");
        }

        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(SQLiteDatabase.findEditTable(mTables));
        sql.append(" SET ");

        final ArrayMap<String, Object> rawValues = values.getValues();
        for (int i = 0; i < rawValues.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(rawValues.keyAt(i));
            sql.append("=?");
        }

        final String where = computeWhere(selection);
        appendClause(sql, " WHERE ", where);
        return sql.toString();
    }

    /** {@hide} */
    public String buildDelete(String selection) {
        StringBuilder sql = new StringBuilder(120);
        sql.append("DELETE FROM ");
        sql.append(SQLiteDatabase.findEditTable(mTables));

        final String where = computeWhere(selection);
        appendClause(sql, " WHERE ", where);
        return sql.toString();
    }

    /**
     * Construct a {@code SELECT} statement suitable for use in a group of
     * {@code SELECT} statements that will be joined through {@code UNION} operators
     * in buildUnionQuery.
     *
     * @param typeDiscriminatorColumn the name of the result column
     *   whose cells will contain the name of the table from which
     *   each row was drawn.
     * @param unionColumns the names of the columns to appear in the
     *   result.  This may include columns that do not appear in the
     *   table this {@code SELECT} is querying (i.e. mTables), but that do
     *   appear in one of the other tables in the {@code UNION} query that we
     *   are constructing.
     * @param columnsPresentInTable a Set of the names of the columns
     *   that appear in this table (i.e. in the table whose name is
     *   mTables).  Since columns in unionColumns include columns that
     *   appear only in other tables, we use this array to distinguish
     *   which ones actually are present.  Other columns will have
     *   NULL values for results from this subquery.
     * @param computedColumnsOffset all columns in unionColumns before
     *   this index are included under the assumption that they're
     *   computed and therefore won't appear in columnsPresentInTable,
     *   e.g. "date * 1000 as normalized_date"
     * @param typeDiscriminatorValue the value used for the
     *   type-discriminator column in this subquery
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL {@code WHERE} clause (excluding the {@code WHERE}
     *   itself).  Passing null will return all rows for the given
     *   URL.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL {@code GROUP BY} clause (excluding the {@code GROUP BY} itself).
     *   Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL {@code HAVING} clause (excluding the {@code HAVING} itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @return the resulting SQL {@code SELECT} statement
     */
    public String buildUnionSubQuery(
            String typeDiscriminatorColumn,
            String[] unionColumns,
            Set<String> columnsPresentInTable,
            int computedColumnsOffset,
            String typeDiscriminatorValue,
            String selection,
            String groupBy,
            String having) {
        int unionColumnsCount = unionColumns.length;
        String[] projectionIn = new String[unionColumnsCount];

        for (int i = 0; i < unionColumnsCount; i++) {
            String unionColumn = unionColumns[i];

            if (unionColumn.equals(typeDiscriminatorColumn)) {
                projectionIn[i] = "'" + typeDiscriminatorValue + "' AS "
                        + typeDiscriminatorColumn;
            } else if (i <= computedColumnsOffset
                       || columnsPresentInTable.contains(unionColumn)) {
                projectionIn[i] = unionColumn;
            } else {
                projectionIn[i] = "NULL AS " + unionColumn;
            }
        }
        return buildQuery(
                projectionIn, selection, groupBy, having,
                null /* sortOrder */,
                null /* limit */);
    }

    /**
     * @deprecated This method's signature is misleading since no SQL parameter
     * substitution is carried out.  The selection arguments parameter does not get
     * used at all.  To avoid confusion, call
     * {@link #buildUnionSubQuery}
     * instead.
     */
    @Deprecated
    public String buildUnionSubQuery(
            String typeDiscriminatorColumn,
            String[] unionColumns,
            Set<String> columnsPresentInTable,
            int computedColumnsOffset,
            String typeDiscriminatorValue,
            String selection,
            String[] selectionArgs,
            String groupBy,
            String having) {
        return buildUnionSubQuery(
                typeDiscriminatorColumn, unionColumns, columnsPresentInTable,
                computedColumnsOffset, typeDiscriminatorValue, selection,
                groupBy, having);
    }

    /**
     * Given a set of subqueries, all of which are {@code SELECT} statements,
     * construct a query that returns the union of what those
     * subqueries return.
     * @param subQueries an array of SQL {@code SELECT} statements, all of
     *   which must have the same columns as the same positions in
     *   their results
     * @param sortOrder How to order the rows, formatted as an SQL
     *   {@code ORDER BY} clause (excluding the {@code ORDER BY} itself).  Passing
     *   null will use the default sort order, which may be unordered.
     * @param limit The limit clause, which applies to the entire union result set
     *
     * @return the resulting SQL {@code SELECT} statement
     */
    public String buildUnionQuery(String[] subQueries, String sortOrder, String limit) {
        StringBuilder query = new StringBuilder(128);
        int subQueryCount = subQueries.length;
        String unionOperator = mDistinct ? " UNION " : " UNION ALL ";

        for (int i = 0; i < subQueryCount; i++) {
            if (i > 0) {
                query.append(unionOperator);
            }
            query.append(subQueries[i]);
        }
        appendClause(query, " ORDER BY ", sortOrder);
        appendClause(query, " LIMIT ", limit);
        return query.toString();
    }

    private static @NonNull String maybeWithOperator(@Nullable String operator,
            @NonNull String column) {
        if (operator != null) {
            return operator + "(" + column + ")";
        } else {
            return column;
        }
    }

    /** {@hide} */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public @Nullable String[] computeProjection(@Nullable String[] projectionIn) {
        if (!ArrayUtils.isEmpty(projectionIn)) {
            String[] projectionOut = new String[projectionIn.length];
            for (int i = 0; i < projectionIn.length; i++) {
                projectionOut[i] = computeSingleProjectionOrThrow(projectionIn[i]);
            }
            return projectionOut;
        } else if (mProjectionMap != null) {
            // Return all columns in projection map.
            Set<Entry<String, String>> entrySet = mProjectionMap.entrySet();
            String[] projection = new String[entrySet.size()];
            Iterator<Entry<String, String>> entryIter = entrySet.iterator();
            int i = 0;

            while (entryIter.hasNext()) {
                Entry<String, String> entry = entryIter.next();

                // Don't include the _count column when people ask for no projection.
                if (entry.getKey().equals(BaseColumns._COUNT)) {
                    continue;
                }
                projection[i++] = entry.getValue();
            }
            return projection;
        }
        return null;
    }

    private @NonNull String computeSingleProjectionOrThrow(@NonNull String userColumn) {
        final String column = computeSingleProjection(userColumn);
        if (column != null) {
            return column;
        } else {
            throw new IllegalArgumentException("Invalid column " + userColumn);
        }
    }

    private @Nullable String computeSingleProjection(@NonNull String userColumn) {
        // When no mapping provided, anything goes
        if (mProjectionMap == null) {
            return userColumn;
        }

        String operator = null;
        String column = mProjectionMap.get(userColumn);

        // When no direct match found, look for aggregation
        if (column == null) {
            final Matcher matcher = sAggregationPattern.matcher(userColumn);
            if (matcher.matches()) {
                operator = matcher.group(1);
                userColumn = matcher.group(2);
                column = mProjectionMap.get(userColumn);
            }
        }

        if (column != null) {
            return maybeWithOperator(operator, column);
        }

        if (mStrictFlags == 0 &&
                (userColumn.contains(" AS ") || userColumn.contains(" as "))) {
            /* A column alias already exist */
            return maybeWithOperator(operator, userColumn);
        }

        // If greylist is configured, we might be willing to let
        // this custom column bypass our strict checks.
        if (mProjectionGreylist != null) {
            boolean match = false;
            for (Pattern p : mProjectionGreylist) {
                if (p.matcher(userColumn).matches()) {
                    match = true;
                    break;
                }
            }

            if (match) {
                Log.w(TAG, "Allowing abusive custom column: " + userColumn);
                return maybeWithOperator(operator, userColumn);
            }
        }

        return null;
    }

    private boolean isTableOrColumn(String token) {
        if (mTables.equals(token)) return true;
        return computeSingleProjection(token) != null;
    }

    /** {@hide} */
    public @Nullable String computeWhere(@Nullable String selection) {
        final boolean hasInternal = !TextUtils.isEmpty(mWhereClause);
        final boolean hasExternal = !TextUtils.isEmpty(selection);

        if (hasInternal || hasExternal) {
            final StringBuilder where = new StringBuilder();
            if (hasInternal) {
                where.append('(').append(mWhereClause).append(')');
            }
            if (hasInternal && hasExternal) {
                where.append(" AND ");
            }
            if (hasExternal) {
                where.append('(').append(selection).append(')');
            }
            return where.toString();
        } else {
            return null;
        }
    }

    /**
     * Wrap given argument in parenthesis, unless it's {@code null} or
     * {@code ()}, in which case return it verbatim.
     */
    private @Nullable String wrap(@Nullable String arg) {
        if (TextUtils.isEmpty(arg)) {
            return arg;
        } else {
            return "(" + arg + ")";
        }
    }
}
