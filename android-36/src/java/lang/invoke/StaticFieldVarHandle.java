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

package java.lang.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A VarHandle that's associated with a static field.
 * @hide
 */
final class StaticFieldVarHandle extends FieldVarHandle {
    private final Class declaringClass;

    private StaticFieldVarHandle(Field staticField) {
        super(staticField);
        declaringClass = staticField.getDeclaringClass();
    }

    static StaticFieldVarHandle create(Field staticField) {
        assert Modifier.isStatic(staticField.getModifiers());
        // TODO(b/379259800): should this be handled at the invocation?
        MethodHandleStatics.UNSAFE.ensureClassInitialized(staticField.getDeclaringClass());
        return new StaticFieldVarHandle(staticField);
    }
}
