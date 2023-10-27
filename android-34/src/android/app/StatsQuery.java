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

package android.app;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;

/**
 * Represents a query that contains information required for StatsManager to return relevant metric
 * data.
 *
 * @hide
 */
@SystemApi
public final class StatsQuery {
    /**
     * Default value for SQL dialect.
     */
    public static final int DIALECT_UNKNOWN = 0;

    /**
     * Query passed is of SQLite dialect.
     */
    public static final int DIALECT_SQLITE = 1;

    /**
     * @hide
     */
    @IntDef(prefix = {"DIALECT_"}, value = {DIALECT_UNKNOWN, DIALECT_SQLITE})
    @interface SqlDialect {
    }

    private final int sqlDialect;
    private final String rawSql;
    private final int minClientSqlVersion;
    private final byte[] policyConfig;
    private StatsQuery(int sqlDialect, @NonNull String rawSql, int minClientSqlVersion,
            @Nullable byte[] policyConfig) {
        this.sqlDialect = sqlDialect;
        this.rawSql = rawSql;
        this.minClientSqlVersion = minClientSqlVersion;
        this.policyConfig = policyConfig;
    }

    /**
     * Returns the SQL dialect of the query.
     */
    public @SqlDialect int getSqlDialect() {
        return sqlDialect;
    }

    /**
     * Returns the raw SQL of the query.
     */
    @NonNull
    public String getRawSql() {
        return rawSql;
    }

    /**
     * Returns the minimum SQL client library version required to execute the query.
     */
    @IntRange(from = 0)
    public int getMinSqlClientVersion() {
        return minClientSqlVersion;
    }

    /**
     * Returns the wire-encoded StatsPolicyConfig proto that contains information to verify the
     * query against a policy defined on the underlying data. Returns null if no policy was set.
     */
    @Nullable
    public byte[] getPolicyConfig() {
        return policyConfig;
    }

    /**
     * Builder for constructing a StatsQuery object.
     * <p>Usage:</p>
     * <code>
     * StatsQuery statsQuery = new StatsQuery.Builder("SELECT * from table")
     * .setSqlDialect(StatsQuery.DIALECT_SQLITE)
     * .setMinClientSqlVersion(1)
     * .build();
     * </code>
     */
    public static final class Builder {
        private int sqlDialect;
        private String rawSql;
        private int minSqlClientVersion;
        private byte[] policyConfig;

        /**
         * Returns a new StatsQuery.Builder object for constructing StatsQuery for
         * StatsManager#query
         */
        public Builder(@NonNull final String rawSql) {
            if (rawSql == null) {
                throw new IllegalArgumentException("rawSql must not be null");
            }
            this.rawSql = rawSql;
            this.sqlDialect = DIALECT_SQLITE;
            this.minSqlClientVersion = 1;
            this.policyConfig = null;
        }

        /**
         * Sets the SQL dialect of the query.
         *
         * @param sqlDialect The SQL dialect of the query.
         */
        @NonNull
        public Builder setSqlDialect(@SqlDialect final int sqlDialect) {
            this.sqlDialect = sqlDialect;
            return this;
        }

        /**
         * Sets the minimum SQL client library version required to execute the query.
         *
         * @param minSqlClientVersion The minimum SQL client version required to execute the query.
         */
        @NonNull
        public Builder setMinSqlClientVersion(@IntRange(from = 0) final int minSqlClientVersion) {
            if (minSqlClientVersion < 0) {
                throw new IllegalArgumentException("minSqlClientVersion must be a "
                        + "positive integer");
            }
            this.minSqlClientVersion = minSqlClientVersion;
            return this;
        }

        /**
         * Sets the wire-encoded StatsPolicyConfig proto that contains information to verify the
         * query against a policy defined on the underlying data.
         *
         * @param policyConfig The wire-encoded StatsPolicyConfig proto.
         */
        @NonNull
        public Builder setPolicyConfig(@NonNull final byte[] policyConfig) {
            this.policyConfig = policyConfig;
            return this;
        }

        /**
         * Builds a new instance of {@link StatsQuery}.
         *
         * @return A new instance of {@link StatsQuery}.
         */
        @NonNull
        public StatsQuery build() {
            return new StatsQuery(sqlDialect, rawSql, minSqlClientVersion, policyConfig);
        }
    }
}
