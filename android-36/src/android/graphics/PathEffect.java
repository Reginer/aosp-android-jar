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

package android.graphics;

/**
 * PathEffect is the base class for objects in the Paint that affect
 * the geometry of a drawing primitive before it is transformed by the
 * canvas' matrix and drawn.
 */
public class PathEffect {

    protected void finalize() throws Throwable {
        nativeDestructor(native_instance);
        native_instance = 0;  // Other finalizers can still call us.
    }

    private static native void nativeDestructor(long native_patheffect);
    long native_instance;
}
