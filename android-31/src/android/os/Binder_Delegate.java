/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.os;

import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import libcore.util.NativeAllocationRegistry_Delegate;

/**
 * Delegate overriding selected methods of android.os.Binder
 *
 * Through the layoutlib_create tool, selected methods of Binder have been replaced
 * by calls to methods of the same name in this delegate class.
 *
 *
 */
public class Binder_Delegate {

    // ---- delegate manager ----
    private static final DelegateManager<Binder_Delegate> sManager =
            new DelegateManager<>(Binder_Delegate.class);
    private static long sFinalizer = -1;

    @LayoutlibDelegate
    /*package*/ static long getNativeBBinderHolder() {
        return sManager.addNewDelegate(new Binder_Delegate());
    }

    @LayoutlibDelegate
    /*package*/ static long getNativeFinalizer() {
        synchronized (Binder_Delegate.class) {
            if (sFinalizer == -1) {
                sFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(
                        sManager::removeJavaReferenceFor);
            }
        }
        return sFinalizer;
    }
}
