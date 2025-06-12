/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server;

import android.annotation.BytesLong;
import android.annotation.NonNull;
import android.os.DropBoxManager;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;

public abstract class DropBoxManagerInternal {
    public abstract void addEntry(@NonNull String tag, @NonNull EntrySource source,
            @DropBoxManager.Flags int flags);

    /**
     * Interface which describes a pending entry which knows how to write itself
     * to the given FD. This abstraction supports implementations which may want
     * to dynamically generate the entry contents.
     */
    public interface EntrySource extends Closeable {
        public void writeTo(@NonNull FileDescriptor fd) throws IOException;

        public default @BytesLong long length() {
            // By default, length is unknown
            return 0;
        }

        public default void close() throws IOException {
            // By default, no resources to close
        }
    }
}
