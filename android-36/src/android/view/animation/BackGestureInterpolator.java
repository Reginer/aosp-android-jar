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

package android.view.animation;
/**
 * Decelerating interpolator with a very slight acceleration phase at the beginning.
 * @hide
 */
public class BackGestureInterpolator extends PathInterpolator {
    public BackGestureInterpolator() {
        super(0.1f, 0.1f, 0f, 1f);
    }
}
