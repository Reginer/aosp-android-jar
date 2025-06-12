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

/**
 * An exception that indicates that the SQLite database file is corrupt.
 */
public class SQLiteDatabaseCorruptException extends SQLiteException {
    public SQLiteDatabaseCorruptException() {}

    public SQLiteDatabaseCorruptException(String error) {
        super(error);
    }

    /**
     * @return true if a given {@link Throwable} or any of its inner causes is of
     * {@link SQLiteDatabaseCorruptException}.
     *
     * @hide
     */
    public static boolean isCorruptException(Throwable th) {
        while (th != null) {
            if (th instanceof SQLiteDatabaseCorruptException) {
                return true;
            }
            th = th.getCause();
        }
        return false;
    }
}
