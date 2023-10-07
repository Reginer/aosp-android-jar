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

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_HAL_REMOTE_EXCEPTION;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_HAL_STATUS_NOT_OK;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONTAINS_CYCLE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_INVALID_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_VIRTUAL_ROOT;
import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.util.IndentingPrintWriter;

import com.google.android.clockwork.ambient.offload.IDisplayOffloadService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vendor.google_clockwork.displayoffload.V1_0.Status;
import vendor.google_clockwork.displayoffload.V1_0.TranslationGroup;


/**
 * Tests for {@link com.google.android.clockwork.displayoffload.HalAdapter}.
 */
@RunWith(AndroidJUnit4.class)
public class HalResourceStoreTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private final Integer fakeId1 = 1;
    private final Integer fakeId2 = 2;
    private final Object fakeResource1 = new Object();
    private final Object fakeResource2 = new Object();
    private final String fakeStringResource1 = "StringResource1";
    private final String fakeStringResource2 = "StringResource2";

    private final FakeHalImplV1_1 mFakeHalImplV1_1 = spy(new FakeHalImplV1_1());
    private final FakeHalImplV2_0 mFakeHalImplV2_0 = spy(new FakeHalImplV2_0());

    @Mock
    HalAdapter mHalAdapter;

    @Mock
    HalTypeConverter mHalTypeConverter;

    @Mock
    IndentingPrintWriter mIndentingPrintWriter;

    private HalResourceStore mHalResourceStore;

    @Before
    public void setup() throws RemoteException, DisplayOffloadException {
        MockitoAnnotations.initMocks(this);
        when(mHalAdapter.send(any())).thenReturn(Status.OK);
        when(mHalAdapter.getConverter()).thenReturn(mHalTypeConverter);

        mHalResourceStore = spy(new HalResourceStore(mHalAdapter));

        when(mIndentingPrintWriter.printPair(anyString(), any())).thenReturn(null);
        doNothing().when(mIndentingPrintWriter).println();
        doNothing().when(mIndentingPrintWriter).println(anyString());
        when(mIndentingPrintWriter.printf(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);
    }

    @Test
    public void testAddReplaceResource() throws RemoteException, DisplayOffloadException {
        List<Object> dummyObjects = makeGraph();
        int dummyObjectsCount = dummyObjects.size();
        for (int i = 0; i < dummyObjectsCount; i++) {
            assertThat(mHalResourceStore.get(i)).isEqualTo(dummyObjects.get(i));
        }
        assertThat(mHalResourceStore.mResources.size()).isEqualTo(dummyObjectsCount);

        // Replace
        mHalResourceStore.addReplaceResource(1, fakeResource1);
        assertThat(mHalResourceStore.get(1)).isEqualTo(fakeResource1);
        assertThat(mHalResourceStore.mResources.size()).isEqualTo(dummyObjectsCount);
    }

    @Test
    public void testAddReplaceResourceDuplicateNoAction() {
        mHalResourceStore.mResources.put(fakeId1, fakeResource1);

        // Add the duplicate pair
        mHalResourceStore.addReplaceResource(fakeId1, fakeResource1);
        assertThat(mHalResourceStore.mResources.size()).isEqualTo(1);
        assertThat(mHalResourceStore.get(fakeId1)).isEqualTo(fakeResource1);
    }

    @Test
    public void testAddReplaceStringIdentifiedObjects() {
        String fakeIdString1 = fakeId1.toString();
        String fakeIdString2 = fakeId2.toString();

        // Add
        mHalResourceStore.addReplaceResource(fakeIdString1, fakeStringResource1);
        mHalResourceStore.addReplaceResource(fakeIdString2, fakeStringResource2);
        assertThat(mHalResourceStore.get(fakeIdString1)).isEqualTo(fakeStringResource1);
        assertThat(mHalResourceStore.get(fakeIdString2)).isEqualTo(fakeStringResource2);
        assertThat(mHalResourceStore.mStringIdentifiedBindings.size()).isEqualTo(2);

        // Replace
        mHalResourceStore.addReplaceResource(fakeIdString1, fakeStringResource2);
        assertThat(mHalResourceStore.get(fakeIdString1)).isEqualTo(fakeStringResource2);
        assertThat(mHalResourceStore.mStringIdentifiedBindings.size()).isEqualTo(2);
    }

    @Test
    public void testAddReplaceStringIdentifiedObjectsDuplicateNoAction() {
        String fakeIdString1 = fakeId1.toString();
        mHalResourceStore.mStringIdentifiedBindings.put(fakeIdString1, fakeStringResource1);

        // Add the duplicate pair
        mHalResourceStore.addReplaceResource(fakeIdString1, fakeStringResource1);
        assertThat(mHalResourceStore.mStringIdentifiedBindings.size()).isEqualTo(1);
        assertThat(mHalResourceStore.get(fakeIdString1)).isEqualTo(fakeStringResource1);
    }

    @Test
    public void testGet() {
        mHalResourceStore.addReplaceResource(fakeId1, fakeResource1);
        mHalResourceStore.addReplaceResource(fakeId2, fakeResource2);
        assertThat(mHalResourceStore.get(fakeId1)).isEqualTo(fakeResource1);
        assertThat(mHalResourceStore.get(fakeId2)).isEqualTo(fakeResource2);

        mHalResourceStore.addReplaceResource(fakeId1.toString(), fakeStringResource1);
        mHalResourceStore.addReplaceResource(fakeId2.toString(), fakeStringResource2);
        assertThat(mHalResourceStore.get(fakeId1.toString())).isEqualTo(fakeStringResource1);
        assertThat(mHalResourceStore.get(fakeId2.toString())).isEqualTo(fakeStringResource2);
    }

    @Test
    public void testIsEmpty() {
        assertThat(mHalResourceStore.isEmpty()).isTrue();
        mHalResourceStore.addReplaceResource(fakeId1, fakeResource1);
        assertThat(mHalResourceStore.isEmpty()).isFalse();
    }

    @Test
    public void testClear() {
        mHalResourceStore.addReplaceResource(fakeId1, fakeResource1);
        mHalResourceStore.addReplaceResource(fakeId1.toString(), fakeStringResource1);
        assertThat(mHalResourceStore.mResources.isEmpty()).isFalse();
        assertThat(mHalResourceStore.mStringIdentifiedBindings.isEmpty()).isFalse();

        mHalResourceStore.clear();
        assertThat(mHalResourceStore.mResources.isEmpty()).isTrue();
        assertThat(mHalResourceStore.mStringIdentifiedBindings.isEmpty()).isTrue();
    }

    @Test
    public void testMarkAllDirty() throws DisplayOffloadException, RemoteException {
        // Add resources & send order should be generated by default
        mHalResourceStore.addReplaceResource(fakeId1, fakeResource1);
        mHalResourceStore.addReplaceResource(fakeId2.toString(), fakeStringResource2);
        mHalResourceStore.sendToHal(mHalAdapter);
        verify(mHalAdapter, times(2)).send(any());

        // String-identified objects always get resent
        clearInvocations(mHalAdapter);
        mHalResourceStore.sendToHal(mHalAdapter);
        verify(mHalAdapter, times(1)).send(any());

        // markAllDirty should cause resources to be re-sent
        clearInvocations(mHalAdapter);
        mHalResourceStore.markAllDirty();
        mHalResourceStore.sendToHal(mHalAdapter);
        verify(mHalAdapter, times(2)).send(any());
    }

    @Test
    public void testGetRootId() throws DisplayOffloadException {
        int rootId = mHalResourceStore.getRootId();
        assertThat(rootId).isEqualTo(-1);

        makeGraph();

        rootId = mHalResourceStore.getRootId();
        assertThat(rootId).isEqualTo(0);
    }

    @Test
    public void testGetRootIdMultiRoot() throws DisplayOffloadException {
        makeGraphMultiRoot();

        int rootId = mHalResourceStore.getRootId();
        assertThat(rootId).isEqualTo(RESOURCE_ID_VIRTUAL_ROOT);
    }

    @Test
    public void testGenerateSendOrder() throws DisplayOffloadException {
        makeGraph();
        mHalResourceStore.generateSendOrder();
        assertThat(mHalResourceStore.mSendOrder).isEqualTo(new int[]{4, 3, 2, 1, 0});
    }

    @Test
    public void testGenerateSendOrderEmpty() throws DisplayOffloadException {
        mHalResourceStore.generateSendOrder();
        assertThat(mHalResourceStore.mSendOrder).isEqualTo(new int[]{});
    }

    @Test
    public void testGenerateSendOrderMultiRoot() throws DisplayOffloadException {
        makeGraphMultiRoot();
        mHalResourceStore.generateSendOrder();
        // 0 and 5 are both root. Who goes first doesn't matter.
        int[][] answerList = new int[][]{
                new int[]{4, 3, 2, 1, 0, 5, RESOURCE_ID_VIRTUAL_ROOT},
                new int[]{4, 3, 2, 1, 5, 0, RESOURCE_ID_VIRTUAL_ROOT}
        };
        for (int[] answer : answerList) {
            if (Arrays.equals(mHalResourceStore.mSendOrder, answer)) {
                return;
            }
        }
        fail("No possible answer was matched. The order must be wrong.");
    }

    @Test
    public void testGenerateSendOrderException() throws DisplayOffloadException {
        makeGraphCycled();
        try {
            mHalResourceStore.generateSendOrder();
        } catch (DisplayOffloadException e) {
            assertThat(e.getErrorType()).isEqualTo(ERROR_LAYOUT_CONTAINS_CYCLE);
        }
        mHalResourceStore.clear();
        makeGraphWithNull();
        try {
            mHalResourceStore.generateSendOrder();
        } catch (DisplayOffloadException e) {
            assertThat(e.getErrorType()).isEqualTo(ERROR_LAYOUT_INVALID_RESOURCE);
        }
    }

    @Test
    public void testSendToHal() throws RemoteException, DisplayOffloadException {
        // Add variables
        int variableCount = 5;
        for (int i = 0; i < variableCount; i++) {
            mHalResourceStore.addReplaceResource("object" + i, "var" + i);
        }

        // Add resources; size = 5
        makeGraph();

        mHalResourceStore.sendToHal(mHalAdapter);
        verify(mHalAdapter, times(10)).send(any());
    }

    @Test
    public void testSendToHalException() throws DisplayOffloadException, RemoteException {
        when(mHalAdapter.send(any())).thenReturn(Status.BAD_VALUE);
        makeGraph();
        try {
            mHalResourceStore.sendToHal(mHalAdapter);
        } catch (DisplayOffloadException e) {
            assertThat(e.getErrorType()).isEqualTo(ERROR_HAL_STATUS_NOT_OK);
        }

        when(mHalAdapter.send(any())).thenReturn(Status.OK);
        doThrow(new RemoteException("Test Exception.")).when(mHalAdapter).send(any());
        try {
            mHalResourceStore.sendToHal(mHalAdapter);
        } catch (DisplayOffloadException e) {
            assertThat(e.getErrorType()).isEqualTo(ERROR_HAL_REMOTE_EXCEPTION);
        }
    }

    private List<Object> makeGraph() throws DisplayOffloadException {
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Object dummyObject = new Object();
            objects.add(dummyObject);
            mHalResourceStore.addReplaceResource(i, dummyObject);
        }
        when(mHalTypeConverter.getIdReferenced(objects.get(0))).thenReturn(
                new ArrayList<>(Arrays.asList(1, 2)));
        when(mHalTypeConverter.getIdReferenced(objects.get(1))).thenReturn(
                new ArrayList<>(Arrays.asList(2, 3)));
        when(mHalTypeConverter.getIdReferenced(objects.get(2))).thenReturn(
                new ArrayList<>(Arrays.asList(3)));
        when(mHalTypeConverter.getIdReferenced(objects.get(3))).thenReturn(
                new ArrayList<>(Arrays.asList(4)));

        return objects;
    }

    private void makeGraphCycled() throws DisplayOffloadException {
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Object dummyObject = new Object();
            objects.add(dummyObject);
            mHalResourceStore.addReplaceResource(i, dummyObject);
        }
        when(mHalTypeConverter.getIdReferenced(objects.get(0))).thenReturn(
                new ArrayList<>(Arrays.asList(1)));
        when(mHalTypeConverter.getIdReferenced(objects.get(1))).thenReturn(
                new ArrayList<>(Arrays.asList(0)));
    }

    private void makeGraphWithNull() {
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            objects.add(null);
            mHalResourceStore.addReplaceResource(i, null);
        }
    }

    private void makeGraphMultiRoot() throws DisplayOffloadException {
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Object dummyObject = new Object();
            objects.add(dummyObject);
            mHalResourceStore.addReplaceResource(i, dummyObject);
        }

        // V1 or V2 doesn't matter here.
        // Just need to call createDummyTranslationGroup representing a virtual root.
        when(mHalTypeConverter.createDummyTranslationGroup(anyInt(), any(ArrayList.class)))
                .thenReturn(Collections.singletonList(
                        ResourceObject.of(RESOURCE_ID_VIRTUAL_ROOT, new TranslationGroup())));

        // TranslationGroup here represents the virtual root.
        when(mHalTypeConverter.getIdReferenced(any(TranslationGroup.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(0, 5)));

        // Graph with 2 roots.
        when(mHalTypeConverter.getIdReferenced(objects.get(0))).thenReturn(
                new ArrayList<>(Arrays.asList(1, 2)));
        when(mHalTypeConverter.getIdReferenced(objects.get(5))).thenReturn(
                new ArrayList<>(Arrays.asList(1)));
        when(mHalTypeConverter.getIdReferenced(objects.get(1))).thenReturn(
                new ArrayList<>(Arrays.asList(2, 3)));
        when(mHalTypeConverter.getIdReferenced(objects.get(2))).thenReturn(
                new ArrayList<>(Arrays.asList(3)));
        when(mHalTypeConverter.getIdReferenced(objects.get(3))).thenReturn(
                new ArrayList<>(Arrays.asList(4)));

    }
}
