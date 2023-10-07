/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vendor.google_clockwork.displayoffload.V1_0.RotationGroup;
import vendor.google_clockwork.displayoffload.V1_0.StringResource;
import vendor.google_clockwork.displayoffload.V1_0.TranslationGroup;
import vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource;
import vendor.google_clockwork.displayoffload.V1_1.StaticTextResource;
import vendor.google_clockwork.displayoffload.V1_1.TextParam;

@RunWith(AndroidJUnit4.class)
public class HalTypeConverterV1Test {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private final UniqueIdGenerator mUniqueIdGenerator = new UniqueIdGenerator((x) -> false);
    private final HalTypeConverter mHalTypeConverterV1 = new HalTypeConverterV1(mContext);

    @Before
    public void setup() {
        mUniqueIdGenerator.reset();
        mHalTypeConverterV1.begin();
    }

    @Test
    public void testGetIdReferenced() throws DisplayOffloadException {
        Set<Integer> referencesAns = new HashSet<>();
        // TranslationGroup
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV1.getIdReferenced(createTranslationGroup(referencesAns,
                        createIntegerArrayListOfSize(5))));

        // RotationGroup
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV1.getIdReferenced(createRotationGroup(referencesAns,
                        createIntegerArrayListOfSize(5))));

        // StaticTextAdapter
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV1.getIdReferenced(createStaticTextAdapter(referencesAns)));

        // StringResource
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV1.getIdReferenced(createStringResource(referencesAns)));

        // DynamicTextAdapter
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV1.getIdReferenced(createDynamicTextAdapter(referencesAns)));
    }

    private ArrayList<Integer> createIntegerArrayListOfSize(int size) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(mUniqueIdGenerator.nextId());
        }
        return arrayList;
    }

    private void checkSizeAndContentThenClear(Set<Integer> ans, List<Integer> input) {
        assertThat(ans.size()).isEqualTo(input.size());
        assertThat(ans.containsAll(input)).isTrue();
        ans.clear();
    }

    private TranslationGroup createTranslationGroup(Set<Integer> references,
            ArrayList<Integer> contents) {
        TranslationGroup translationGroup = new TranslationGroup();
        translationGroup.id = mUniqueIdGenerator.nextId();
        translationGroup.contents = contents == null ? new ArrayList<>() : contents;

        references.addAll(translationGroup.contents);
        return translationGroup;
    }

    private RotationGroup createRotationGroup(Set<Integer> references,
            ArrayList<Integer> contents) {
        RotationGroup rotationGroup = new RotationGroup();
        rotationGroup.id = mUniqueIdGenerator.nextId();
        rotationGroup.contents = contents == null ? new ArrayList<>() : contents;

        references.addAll(rotationGroup.contents);
        return rotationGroup;
    }

    private StaticTextAdapter createStaticTextAdapter(Set<Integer> references) {
        StaticTextResource staticText = new StaticTextResource();
        staticText.id = mUniqueIdGenerator.nextId();
        staticText.textParam = new TextParam();
        staticText.textParam.ttfFont = mUniqueIdGenerator.nextId();

        references.add(staticText.textParam.ttfFont);
        return new StaticTextAdapter(staticText);
    }

    private DynamicTextAdapter createDynamicTextAdapter(Set<Integer> references) {
        DynamicTextResource dynamicText = new DynamicTextResource();
        dynamicText.id = mUniqueIdGenerator.nextId();
        dynamicText.textParam = new TextParam();
        dynamicText.textParam.ttfFont = mUniqueIdGenerator.nextId();
        dynamicText.bindings = new ArrayList<>();

        references.add(dynamicText.textParam.ttfFont);
        return new DynamicTextAdapter(dynamicText);
    }

    private StringResource createStringResource(Set<Integer> references) {
        StringResource stringResource = new StringResource();
        stringResource.id = mUniqueIdGenerator.nextId();
        stringResource.font = mUniqueIdGenerator.nextId();

        references.add(stringResource.font);
        return stringResource;
    }
}
