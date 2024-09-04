/*
 * Copyright (C) 2016 The Android Open Source Project
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

package dalvik.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;

/**
 * Marks the current class as a record class and stores information about its record components.
 *
 * <p>The annotation is allowed only on classes that are final and can be used at most once for
 * each of these classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@interface Record {

    /*
     * This annotation is never used in source code; it is expected to be generated in .dex
     * files by tools like compilers. Commented definitions for the annotation members expected
     * by the runtime / reflection code can be found below for reference.
     *
     * The arrays documented below must be the same size as for the field_id_item dex structure
     * associated with the record class otherwise a java.lang.reflect.MalformedParametersException
     * will be thrown at runtime.
     */

    /*
     * The array of component names for the record class. The array cannot be null, but can be
     * empty. Also all values in the array must be non-null, non-empty and not contain '.', ';', '['
     * or '/', otherwise a java.lang.reflect.MalformedParametersException will be thrown at runtime.
     */
    // String[] componentNames();

    /*
     * The array of component types for the record class. The array cannot be null, but can be
     * empty. All values in the array must be non-null, otherwise a
     * java.lang.reflect.MalformedParametersException will be thrown at runtime.
     */
    // Class<?>[] componentTypes();

    /*
     * The array of {@link dalvik.annotation.Signature} attribute for each record component.
     */
    // Signature[] componentSignatures();

    /*
     * The 2D array of annotation visibilities for annotations in record component.
     */

    // byte[][] componentAnnotationVisibilities();

    /*
     * The 2D array of annotations. Each component can have multiple annotations.
     */
    // Annotation[][] componentAnnotations();
}

