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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;

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
@SafeParcelable.Class(creator = "ObserverSpecCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class ObserverSpec extends AbstractSafeParcelable {

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<ObserverSpec> CREATOR =
            new ObserverSpecCreator();

    @Field(id = 1)
    final List<String> mFilterSchemas;

    /** Populated on first use */
    private volatile @Nullable Set<String> mFilterSchemasCached;

    /** @hide */
    @Constructor
    public ObserverSpec(@Param(id = 1) @NonNull List<String> filterSchemas) {
        mFilterSchemas = Objects.requireNonNull(filterSchemas);
    }

    /**
     * Returns the list of schema types which observers using this spec will trigger on.
     *
     * <p>If empty, the observers will trigger on all schema types.
     */
    public @NonNull Set<String> getFilterSchemas() {
        if (mFilterSchemasCached == null) {
            if (mFilterSchemas == null) {
                mFilterSchemasCached = Collections.emptySet();
            } else {
                mFilterSchemasCached = Collections.unmodifiableSet(new ArraySet<>(mFilterSchemas));
            }
        }
        return mFilterSchemasCached;
    }

    /** Builder for {@link ObserverSpec} instances. */
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
        public @NonNull Builder addFilterSchemas(@NonNull String... schemas) {
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
        public @NonNull Builder addFilterSchemas(@NonNull Collection<String> schemas) {
            Objects.requireNonNull(schemas);
            resetIfBuilt();
            mFilterSchemas.addAll(schemas);
            return this;
        }

        /** Constructs a new {@link ObserverSpec} from the contents of this builder. */
        public @NonNull ObserverSpec build() {
            mBuilt = true;
            return new ObserverSpec(mFilterSchemas);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mFilterSchemas = new ArrayList<>(mFilterSchemas);
                mBuilt = false;
            }
        }
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ObserverSpecCreator.writeToParcel(this, dest, flags);
    }
}
