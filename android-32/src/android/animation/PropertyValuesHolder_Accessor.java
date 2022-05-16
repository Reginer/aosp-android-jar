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

package android.animation;

import android.animation.PropertyValuesHolder.FloatPropertyValuesHolder;
import android.animation.PropertyValuesHolder.IntPropertyValuesHolder;
import android.animation.PropertyValuesHolder.MultiFloatValuesHolder;
import android.animation.PropertyValuesHolder.MultiIntValuesHolder;

public class PropertyValuesHolder_Accessor {
    /**
     * Method used by layoutlib to ensure that {@link Class} instances are not cached by this
     * class. Caching the {@link Class} instances will lead to leaking the {@link ClassLoader}
     * used by that class.
     * In layoutlib, these class loaders are instantiated dynamically to allow for new classes to
     * be loaded when the user code changes.
     */
    public static void clearClassCaches() {
        PropertyValuesHolder.sGetterPropertyMap.clear();
        PropertyValuesHolder.sSetterPropertyMap.clear();
        IntPropertyValuesHolder.sJNISetterPropertyMap.clear();
        MultiIntValuesHolder.sJNISetterPropertyMap.clear();
        FloatPropertyValuesHolder.sJNISetterPropertyMap.clear();
        MultiFloatValuesHolder.sJNISetterPropertyMap.clear();
    }
}
