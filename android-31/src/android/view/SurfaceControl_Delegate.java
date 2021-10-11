/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.view;

import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import libcore.util.NativeAllocationRegistry_Delegate;

public class SurfaceControl_Delegate {

    // ---- delegate manager ----
    private static final DelegateManager<SurfaceControl_Delegate> sManager =
            new DelegateManager<>(SurfaceControl_Delegate.class);
    private static long sFinalizer = -1;

    @LayoutlibDelegate
    /*package*/ static long nativeCreateTransaction() {
        return sManager.addNewDelegate(new SurfaceControl_Delegate());
    }

    @LayoutlibDelegate
    /*package*/ static long nativeGetNativeTransactionFinalizer() {
        synchronized (SurfaceControl_Delegate.class) {
            if (sFinalizer == -1) {
                sFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(sManager::removeJavaReferenceFor);
            }
        }
        return sFinalizer;
    }
}
