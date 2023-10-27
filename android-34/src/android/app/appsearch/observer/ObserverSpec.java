/*
 * Copyright 2021 The Android Open Source Project
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

package android.app.appsearch.observer;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.os.Bundle;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configures the types, namespaces and other properties that {@link ObserverCallback} instances
 * match against.
 */
public final class ObserverSpec {
    private static final String FILTER_SCHEMA_FIELD = "filterSchema";

    private final Bundle mBundle;

    /** Populated on first use */
    @Nullable private volatile Set<String> mFilterSchemas;

    /** @hide */
    public ObserverSpec(@NonNull Bundle bundle) {
        Objects.requireNonNull(bundle);
        mBundle = bundle;
    }

    /**
     * Returns the {@link Bundle} backing this spec.
     *
     * @hide
     */
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

    /**
     * Returns the list of schema types which observers using this spec will trigger on.
     *
     * <p>If empty, the observers will trigger on all schema types.
     */
    @NonNull
    public Set<String> getFilterSchemas() {
        if (mFilterSchemas == null) {
            List<String> schemas = mBundle.getStringArrayList(FILTER_SCHEMA_FIELD);
            if (schemas == null) {
                mFilterSchemas = Collections.emptySet();
            } else {
                mFilterSchemas = Collections.unmodifiableSet(new ArraySet<>(schemas));
            }
        }
        return mFilterSchemas;
    }

    /** Builder for ObserverSpec instances. */
    public static final class Builder {
        private ArrayList<String> mFilterSchemas = new ArrayList<>();
        private boolean mBuilt = false;

        /**
         * Restricts an observer using this spec to triggering only for documents of one of the
         * provided schema types.
         *
         * <p>If unset, the observer will match documents of all types.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull String... schemas) {
            Objects.requireNonNull(schemas);
            resetIfBuilt();
            return addFilterSchemas(Arrays.asList(schemas));
        }

        /**
         * Restricts an observer using this spec to triggering only for documents of one of the
         * provided schema types.
         *
         * <p>If unset, the observer will match documents of all types.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull Collection<String> schemas) {
            Objects.requireNonNull(schemas);
            resetIfBuilt();
            mFilterSchemas.addAll(schemas);
            return this;
        }

        /** Constructs a new {@link ObserverSpec} from the contents of this builder. */
        @NonNull
        public ObserverSpec build() {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(FILTER_SCHEMA_FIELD, mFilterSchemas);
            mBuilt = true;
            return new ObserverSpec(bundle);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mFilterSchemas = new ArrayList<>(mFilterSchemas);
                mBuilt = false;
            }
        }
    }
}
