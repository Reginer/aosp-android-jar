/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib.summary;

import androidx.annotation.NonNull;

/** An instance that represents a single summary action item and all of its state. */
public class SummaryAction implements Comparable<SummaryAction> {

    public final String actionTitle;
    public final String actionDescription;
    public final boolean requiresNetwork;
    public final String scriptUri;
    public final int priority;
    public final boolean hasUnfinishedDependency;
    public final String dependencyDescription;
    public final SummaryActionState completeState;
    public final String iconResourceName;
    public final String completedDescription;

    public SummaryAction(
            String actionTitle,
            String actionDescription,
            boolean requiresNetwork,
            SummaryActionState completeState,
            int priority,
            String scriptUri,
            boolean hasUnfinishedDependency,
            String dependencyDescription,
            String iconResourceName,
            String completedDescription) {
        this.actionTitle = actionTitle;
        this.actionDescription = actionDescription;
        this.requiresNetwork = requiresNetwork;
        this.completeState = completeState;
        this.priority = priority;
        this.scriptUri = scriptUri;
        this.hasUnfinishedDependency = hasUnfinishedDependency;
        this.dependencyDescription = dependencyDescription;
        this.iconResourceName = iconResourceName;
        this.completedDescription = completedDescription;
    }

    /** Returns {@code true} if the action is completed. */
    public boolean isCompleted() {
        return completeState == SummaryActionState.COMPLETED;
    }

    @Override
    public int compareTo(@NonNull SummaryAction o) {
        if (o == null) {
            return 1;
        }
        return priority - o.priority;
    }
}
