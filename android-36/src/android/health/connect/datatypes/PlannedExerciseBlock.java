/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.health.connect.datatypes;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.internal.datatypes.PlannedExerciseBlockInternal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a series of {@link PlannedExerciseStep}. Part of a {@link
 * PlannedExerciseSessionRecord}.
 */
@FlaggedApi("com.android.healthconnect.flags.training_plans")
public final class PlannedExerciseBlock {
    private final int mRepetitions;
    @Nullable private final CharSequence mDescription;
    private final List<PlannedExerciseStep> mSteps;

    private PlannedExerciseBlock(
            int repetitions,
            @Nullable CharSequence description,
            @NonNull List<PlannedExerciseStep> steps) {
        this.mRepetitions = repetitions;
        this.mDescription = description;
        this.mSteps = steps;
    }

    /** A description for this block, e.g. "Main set". */
    @Nullable
    public CharSequence getDescription() {
        return mDescription;
    }

    /**
     * The steps associated with this block.
     *
     * @return An unmodifiable list of {@link PlannedExerciseStep}.
     */
    @NonNull
    public List<PlannedExerciseStep> getSteps() {
        return mSteps;
    }

    /** The number of times this block should be repeated by the user. */
    public int getRepetitions() {
        return mRepetitions;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof PlannedExerciseBlock)) return false;
        PlannedExerciseBlock that = (PlannedExerciseBlock) o;
        return RecordUtils.isEqualNullableCharSequences(getDescription(), that.getDescription())
                && this.getRepetitions() == that.getRepetitions()
                && Objects.equals(this.getSteps(), that.getSteps());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getRepetitions(), getSteps());
    }

    /** Builder of {@link PlannedExerciseBlock}. */
    public static final class Builder {
        private int mRepetitions;
        private List<PlannedExerciseStep> mSteps = new ArrayList<>();
        @Nullable private CharSequence mDescription;

        /**
         * @param repetitions The number of times this block should be repeated by the user.
         */
        public Builder(int repetitions) {
            this.mRepetitions = repetitions;
        }

        /** Sets the number of repetitions. */
        @NonNull
        public Builder setRepetitions(int repetitions) {
            this.mRepetitions = repetitions;
            return this;
        }

        /** Sets the description. */
        @NonNull
        public Builder setDescription(@Nullable CharSequence description) {
            this.mDescription = description;
            return this;
        }

        /** Adds a {@link PlannedExerciseStep}. */
        @NonNull
        public Builder addStep(@NonNull PlannedExerciseStep step) {
            Objects.requireNonNull(step);
            mSteps.add(step);
            return this;
        }

        /** Sets the {@link PlannedExerciseStep} entries. */
        @NonNull
        public Builder setSteps(@NonNull List<PlannedExerciseStep> steps) {
            Objects.requireNonNull(steps);
            mSteps.clear();
            mSteps.addAll(steps);
            return this;
        }

        /** Clears the {@link PlannedExerciseStep} entries. */
        @NonNull
        public Builder clearSteps() {
            mSteps.clear();
            return this;
        }

        /**
         * @return Object of {@link PlannedExerciseBlock}.
         */
        @NonNull
        public PlannedExerciseBlock build() {
            return new PlannedExerciseBlock(mRepetitions, mDescription, List.copyOf(mSteps));
        }
    }

    /** @hide */
    public PlannedExerciseBlockInternal toInternalObject() {
        PlannedExerciseBlockInternal result = new PlannedExerciseBlockInternal(getRepetitions());
        if (mDescription != null) {
            result.setDescription(mDescription.toString());
        }
        result.setExerciseSteps(
                getSteps().stream().map(it -> it.toInternalObject()).collect(Collectors.toList()));
        return result;
    }
}
