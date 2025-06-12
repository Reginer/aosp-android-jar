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

package android.view.accessibility;

/**
 * A callback for magnification animation result.
 * @hide
 */

 oneway interface IRemoteMagnificationAnimationCallback {

    /**
     * Called when the animation is finished or interrupted during animating.
     *
     * @param success {@code true} if animating successfully with given spec or the spec did not
     *                change. Otherwise {@code false}
     */
     @RequiresNoPermission
    void onResult(boolean success);
}
