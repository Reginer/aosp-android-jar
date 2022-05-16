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
package com.android.layoutlib.bridge.android;

import com.android.ide.common.rendering.api.ResourceValue;

import android.util.ResolvingAttributeSet;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * Empty {@link AttributeSet}
 */
class NopAttributeSet implements ResolvingAttributeSet {
    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public String getAttributeNamespace(int index) {
        return null;
    }

    @Override
    public String getAttributeName(int index) {
        return null;
    }

    @Override
    public String getAttributeValue(int index) {
        return null;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return null;
    }

    @Override
    public String getPositionDescription() {
        return null;
    }

    @Override
    public int getAttributeNameResource(int index) {
        return 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int
            defaultValue) {
        return 0;
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute,
            boolean defaultValue) {
        return false;
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute,
            int defaultValue) {
        return 0;
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute,
            float defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        return 0;
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return false;
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        return 0;
    }

    @Override
    public String getIdAttribute() {
        return null;
    }

    @Override
    public String getClassAttribute() {
        return null;
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        return 0;
    }

    @Override
    public int getStyleAttribute() {
        return 0;
    }

    @Override
    @Nullable
    public ResourceValue getResolvedAttributeValue(@Nullable String namespace,
            @NonNull String name) {
        return null;
    }
}
