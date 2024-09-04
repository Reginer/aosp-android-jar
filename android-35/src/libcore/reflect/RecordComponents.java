/*
 * Copyright (C) 2023 The Android Open Source Project
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

package libcore.reflect;

import dalvik.annotation.Signature;

import libcore.util.EmptyArray;

import java.lang.annotation.Annotation;

/**
 * Mirror of the data structure specified in {@link dalvik.annotation.Record}. But annotation type
 * isn't allowed to have a 2D array element, but it's stored in the .dex format. This is regular
 * class created to store the annotation elements consumed internally by libcore.
 *
 * Please see {@link dalvik.annotation.Record} for detailed javadoc for each element.
 */
public final class RecordComponents {

    /**
     * VISIBILITY_RUNTIME value.
     * See https://source.android.com/docs/core/runtime/dex-format#visibility
     */
    private static final byte DEX_ANNOTATION_VISIBILITY_RUNTIME = 1;

    private final Class<?> declaringClass;

    private final String[] componentNames;
    private final Class<?>[] componentTypes;
    private final Signature[] componentSignatures;
    private byte[][] componentAnnotationVisibilities;
    private Annotation[][] componentAnnotations;

    /**
     * Created an empty instance and the fields will be filled by the ART runtime.
     */
    public RecordComponents(Class<?> declaringClass) {
        this.declaringClass = declaringClass;
        componentNames = readElement("componentNames", String[].class);
        componentTypes = readElement("componentTypes", Class[].class);
        componentSignatures = readElement("componentSignatures", Signature[].class);
    }

    private <T> T[] readElement(String name, Class<T[]> array_class) {
        return declaringClass.getRecordAnnotationElement(name, array_class);
    }

    public String[] getNames() {
        return componentNames;
    }

    public Class<?>[] getTypes() {
        return componentTypes;
    }

    /**
     * @return null if no signature is found.
     */
    public String getGenericSignature(int index) {
        Signature signature = null;
        if (componentSignatures != null && index >= 0 && index < componentSignatures.length) {
            signature = componentSignatures[index];
        }
        if (signature == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String s : signature.value()) {
            result.append(s);
        }
        return result.toString();
    }

    /**
     * Return all annotations visible at runtime.
     *
     * @param index Component index
     */
    public Annotation[] getVisibleAnnotations(int index) {
        synchronized (this) {
            if (componentAnnotations == null) {
                Annotation[][] allAnnotations = readElement("componentAnnotations",
                        Annotation[][].class);
                byte[][] allVisibilities = readElement("componentAnnotationVisibilities",
                        byte[][].class);
                if (allAnnotations == null) {
                    allAnnotations = new Annotation[0][];
                }
                if (allVisibilities == null) {
                    allVisibilities = new byte[0][];
                }
                componentAnnotationVisibilities = allVisibilities;
                componentAnnotations = allAnnotations;
            }
        }
        Annotation[][] allAnnotations = componentAnnotations;
        byte[][] allVisibilities = componentAnnotationVisibilities;

        if (index < 0 || index >= allAnnotations.length || allAnnotations[index] == null) {
            return EmptyArray.ANNOTATION;
        }

        Annotation[] annotations = allAnnotations[index];
        int size = annotations.length;
        if (size == 0) {
            return annotations;
        }
        byte[] visibilities = index < allVisibilities.length ? allVisibilities[index] : null;
        if (visibilities == null) {
            return EmptyArray.ANNOTATION;
        }

        int minSize = Math.min(visibilities.length, size);
        int visibleAnnotationsSize = 0;
        for (int i = 0; i < minSize; i++) {
            if (visibilities[i] == DEX_ANNOTATION_VISIBILITY_RUNTIME) {
                visibleAnnotationsSize++;
            }
        }

        // In most cases, component has no invisible annotations and return all annotations here.
        if (visibleAnnotationsSize == size) {
            return annotations;
        }

        // slow path
        Annotation[] visibleAnnotations = new Annotation[visibleAnnotationsSize];
        int j = 0;
        for (int i = 0; i < minSize; i++) {
            if (visibilities[i] == DEX_ANNOTATION_VISIBILITY_RUNTIME) {
                visibleAnnotations[j] = annotations[i];
                j++;
            }
        }
        return visibleAnnotations;
    }
}
