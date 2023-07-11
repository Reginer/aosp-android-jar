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

package com.android.server.wm;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_INPUT_METHOD;
import static android.view.WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG;
import static android.window.DisplayAreaOrganizer.FEATURE_DEFAULT_TASK_CONTAINER;
import static android.window.DisplayAreaOrganizer.FEATURE_IME_PLACEHOLDER;
import static android.window.DisplayAreaOrganizer.FEATURE_VENDOR_FIRST;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider for platform-default car display area policy for reference design.
 *
 * @hide
 */
public class CarDisplayAreaPolicyProvider implements DisplayAreaPolicy.Provider {

    /**
     * This display area is mandatory to be defined. This is where the applications will be
     * launched.
     */
    private static final int DEFAULT_APP_TASK_CONTAINER = FEATURE_DEFAULT_TASK_CONTAINER;

    /**
     * The display partition to launch applications by default. This contains {@link
     * #DEFAULT_APP_TASK_CONTAINER}.
     */
    private static final int FOREGROUND_DISPLAY_AREA_ROOT = FEATURE_VENDOR_FIRST + 1;

    /**
     * Background applications task container.
     */
    private static final int BACKGROUND_TASK_CONTAINER = FEATURE_VENDOR_FIRST + 2;
    private static final int FEATURE_TASKDISPLAYAREA_PARENT = FEATURE_VENDOR_FIRST + 3;

    /**
     * Control bar task container.
     *
     * Currently we are launching CarLauncher activity in this TDA. This is because the audio card
     * implementation today is using fragments. If that changes in future then we can use the window
     * instead to display that view instead of fragments that need an activity.
     */
    private static final int CONTROL_BAR_DISPLAY_AREA = FEATURE_VENDOR_FIRST + 4;

    /**
     * Feature to display the title bar.
     */
    private static final int FEATURE_TITLE_BAR = FEATURE_VENDOR_FIRST + 5;

    /**
     * Feature to display voice plate.
     */
    private static final int FEATURE_VOICE_PLATE = FEATURE_VENDOR_FIRST + 6;

    @Override
    public DisplayAreaPolicy instantiate(WindowManagerService wmService, DisplayContent content,
            RootDisplayArea root, DisplayArea.Tokens imeContainer) {

        if (!content.isDefaultDisplay) {
            return new DisplayAreaPolicy.DefaultProvider().instantiate(wmService, content, root,
                    imeContainer);
        }

        TaskDisplayArea backgroundTaskDisplayArea = new TaskDisplayArea(content, wmService,
                "BackgroundTaskDisplayArea", BACKGROUND_TASK_CONTAINER,
                /* createdByOrganizer= */ false, /* canHostHomeTask= */ false);

        TaskDisplayArea controlBarDisplayArea = new TaskDisplayArea(content, wmService,
                "ControlBarTaskDisplayArea", CONTROL_BAR_DISPLAY_AREA,
                /* createdByOrganizer= */ false, /* canHostHomeTask= */ false);

        TaskDisplayArea voicePlateTaskDisplayArea = new TaskDisplayArea(content, wmService,
                "VoicePlateTaskDisplayArea", FEATURE_VOICE_PLATE,
                /* createdByOrganizer= */ false, /* canHostHomeTask= */ false);

        List<TaskDisplayArea> backgroundTdaList = new ArrayList<>();
        backgroundTdaList.add(voicePlateTaskDisplayArea);
        backgroundTdaList.add(backgroundTaskDisplayArea);
        backgroundTdaList.add(controlBarDisplayArea);

        // Root
        DisplayAreaPolicyBuilder.HierarchyBuilder rootHierarchy =
                new DisplayAreaPolicyBuilder.HierarchyBuilder(root)
                        .setTaskDisplayAreas(backgroundTdaList)
                        .addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy,
                                "ImePlaceholder", FEATURE_IME_PLACEHOLDER)
                                .and(TYPE_INPUT_METHOD, TYPE_INPUT_METHOD_DIALOG)
                                .build())
                        // to make sure there are 2 children under root.
                        // TODO: replace when b/188102153 is resolved to set this to top.
                        .addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy,
                                "TaskDisplayAreaParent", FEATURE_TASKDISPLAYAREA_PARENT)
                                .and(TYPE_APPLICATION)
                                .build());

        // Default application launches here
        RootDisplayArea defaultAppsRoot = new DisplayAreaGroup(wmService,
                "FeatureForegroundApplication", FOREGROUND_DISPLAY_AREA_ROOT);
        TaskDisplayArea defaultAppTaskDisplayArea = new TaskDisplayArea(content, wmService,
                "DefaultApplicationTaskDisplayArea", DEFAULT_APP_TASK_CONTAINER);
        List<TaskDisplayArea> firstTdaList = new ArrayList<>();
        firstTdaList.add(defaultAppTaskDisplayArea);
        DisplayAreaPolicyBuilder.HierarchyBuilder applicationHierarchy =
                new DisplayAreaPolicyBuilder.HierarchyBuilder(defaultAppsRoot)
                        .setTaskDisplayAreas(firstTdaList)
                        .setImeContainer(imeContainer)
                        .addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy,
                                "ImePlaceholder", FEATURE_IME_PLACEHOLDER)
                                .and(TYPE_INPUT_METHOD, TYPE_INPUT_METHOD_DIALOG)
                                .build())
                        .addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy,
                                "TitleBar", FEATURE_TITLE_BAR)
                                .and(TYPE_APPLICATION_OVERLAY)
                                .build());

        return new DisplayAreaPolicyBuilder()
                .setRootHierarchy(rootHierarchy)
                .addDisplayAreaGroupHierarchy(applicationHierarchy)
                .build(wmService);
    }
}
