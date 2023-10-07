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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.os.IHwBinder;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vendor.google_clockwork.displayoffload.V1_0.Status;

/**
 * Tests for {@link com.google.android.clockwork.displayoffload.HalAdapter}.
 */
@RunWith(AndroidJUnit4.class)
public class HalAdapterTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final HalAdapter mHalAdapter = spy(new HalAdapter(mContext));
    private final FakeHalImplV1_1 mFakeHalImplV1_1 = spy(new FakeHalImplV1_1());
    private final FakeHalImplV2_0 mFakeHalImplV2_0 = spy(new FakeHalImplV2_0());
    private final IHwBinder fakeBinder1 = spy(new FakeHwBinder());
    private final IHwBinder fakeBinder2 = spy(new FakeHwBinder());
    private final HalAdapter.HalListener mFakeHalListener =
            spy(
                    new HalAdapter.HalListener() {
                        @Override
                        public void onHalRegistered() {}

                        @Override
                        public void onHalDied() {}
                    });

    @Before
    public void setup() {
        mFakeHalImplV1_1.mHwBinder = fakeBinder1;
        mFakeHalImplV2_0.mHwBinder = fakeBinder2;
    }

    @Test
    public void testHalConnectionReporting() {
        // No HAL connected
        assertThat(mHalAdapter.isHalConnected()).isFalse();
        assertThat(mHalAdapter.getConnectedHalVersions()).hasSize(0);
        // V1
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = null;
        mHalAdapter.mDisplayOffloadV2_0 = null;
        assertThat(mHalAdapter.isHalConnected()).isTrue();
        assertThat(mHalAdapter.getConnectedHalVersions()).hasSize(1);
        // V1_1
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV2_0 = null;
        assertThat(mHalAdapter.isHalConnected()).isTrue();
        assertThat(mHalAdapter.getConnectedHalVersions()).hasSize(2);
        // V2_0
        mHalAdapter.mDisplayOffloadV1_0 = null;
        mHalAdapter.mDisplayOffloadV1_1 = null;
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        assertThat(mHalAdapter.isHalConnected()).isTrue();
        assertThat(mHalAdapter.getConnectedHalVersions()).hasSize(1);
        // V1_1 & V2_0
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        assertThat(mHalAdapter.isHalConnected()).isTrue();
        assertThat(mHalAdapter.getConnectedHalVersions()).hasSize(3);
    }

    @Test
    public void testSetBrightnessConfiguration() throws RemoteException {
        boolean brightenOnWristTilt = true;
        int status;
        ArrayList<Short> alsThresholds = new ArrayList<>(
                Arrays.asList(new Short[]{0x1, 0x2, 0x3}));
        ArrayList<Short> brightnessValuesDim = new ArrayList<>(
                Arrays.asList(new Short[]{0x4, 0x5, 0x6}));
        ArrayList<Short> brightnessValuesBright = new ArrayList<>(
                Arrays.asList(new Short[]{0x7, 0x8, 0x9}));

        // No HAL connected, don't crash
        status = mHalAdapter.setBrightnessConfiguration(brightenOnWristTilt, alsThresholds,
                brightnessValuesDim, brightnessValuesBright);
        assertThat(status).isNotEqualTo(Status.OK);
        verifyNoMoreInteractions(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V1
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        when(mFakeHalImplV1_1.setBrightnessConfiguration(anyBoolean(),any(),any(),any()))
                .thenReturn(Status.OK);
        status = mHalAdapter.setBrightnessConfiguration(brightenOnWristTilt, alsThresholds,
                brightnessValuesDim, brightnessValuesBright);
        assertThat(status).isEqualTo(Status.OK);
        verify(mFakeHalImplV1_1).setBrightnessConfiguration(eq(brightenOnWristTilt),
                eq(alsThresholds), eq(brightnessValuesDim), eq(brightnessValuesBright));
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V2
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        when(mFakeHalImplV2_0.setBrightnessConfiguration(anyBoolean(),any(),any(),any()))
                .thenReturn(Status.OK);
        status = mHalAdapter.setBrightnessConfiguration(brightenOnWristTilt, alsThresholds,
                brightnessValuesDim, brightnessValuesBright);
        assertThat(status).isEqualTo(Status.OK);
        verify(mFakeHalImplV2_0).setBrightnessConfiguration(eq(brightenOnWristTilt),
                eq(alsThresholds), eq(brightnessValuesDim), eq(brightnessValuesBright));
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testIsRenderControlSupported() {
        // No HAL connected, don't crash
        assertThat(mHalAdapter.isRenderControlSupported()).isFalse();
        // V1 connected
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        assertThat(mHalAdapter.isRenderControlSupported()).isTrue();
        // V2 connected
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        assertThat(mHalAdapter.isRenderControlSupported()).isTrue();
    }

    @Test
    public void testBeginEndDisplayControl() throws RemoteException {
        // No HAL connected, don't crash
        mHalAdapter.beginDisplay();
        mHalAdapter.beginRendering();
        mHalAdapter.endRendering();
        mHalAdapter.endDisplay();
        verifyNoMoreInteractions(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V1 connected
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.beginDisplay();
        mHalAdapter.beginRendering();
        mHalAdapter.endRendering();
        mHalAdapter.endDisplay();
        // Only V1 should be called
        verify(mFakeHalImplV1_1).beginDisplay();
        verify(mFakeHalImplV1_1).beginRendering();
        verify(mFakeHalImplV1_1).endRendering();
        verify(mFakeHalImplV1_1).endDisplay();
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V2 connected
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        mHalAdapter.beginDisplay();
        mHalAdapter.beginRendering();
        mHalAdapter.endRendering();
        mHalAdapter.endDisplay();
        // Should prefer V2 when V1 is also connected
        verify(mFakeHalImplV2_0).beginDisplayControl();
        verify(mFakeHalImplV2_0).beginRendering();
        verify(mFakeHalImplV2_0).endRendering();
        verify(mFakeHalImplV2_0).endDisplayControl();
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testResetResource() throws RemoteException {
        // No HAL connected, don't crash
        mHalAdapter.resetResource();
        verifyNoMoreInteractions(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V1
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.resetResource();
        verify(mFakeHalImplV1_1).resetResource();
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V2
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        mHalAdapter.resetResource();
        verify(mFakeHalImplV2_0).reset();
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testSetRootResourceId() throws RemoteException {
        final int rootId = 42;
        // No HAL connected, don't crash
        mHalAdapter.setRoot(rootId);
        verifyNoMoreInteractions(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.setRoot(rootId);
        verify(mFakeHalImplV1_1).setRootResourceId(eq(rootId));
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        mHalAdapter.setRoot(rootId);
        verify(mFakeHalImplV2_0).setRootResourceId(eq(rootId));
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testFetchDataSnapshot() throws RemoteException {
        // No HAL connected, don't crash
        Bundle bundle = mHalAdapter.fetchDataSnapshot();
        assertThat(bundle).isNull();
        verifyNoMoreInteractions(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V1
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        bundle = mHalAdapter.fetchDataSnapshot();
        assertThat(bundle).isNotNull();
        verify(mFakeHalImplV1_1).fetchDataSnapshot();
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V2
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        bundle = mHalAdapter.fetchDataSnapshot();
        assertThat(bundle).isNotNull();
        verify(mFakeHalImplV2_0).fetchDataSnapshot();
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testSendNoHalAvailable() throws RemoteException, DisplayOffloadException {
        // No HAL connected, don't crash
        assertThat(mHalAdapter.send(new Object())).isEqualTo(Status.OK);
        verifyNoMoreInteractions(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);
    }

    @Test
    public void testSendV1HALTypesConnected() throws RemoteException, DisplayOffloadException {
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV2_0 = null;
        verifySendHalObject(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);
    }

    @Test
    public void testSendV2HALTypesConnected() throws RemoteException, DisplayOffloadException {
        mHalAdapter.mDisplayOffloadV1_0 = null;
        mHalAdapter.mDisplayOffloadV1_1 = null;
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        verifySendHalObject(mFakeHalImplV2_0);
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testSetLocaleConfig() throws RemoteException {
        // No HAL connected, don't crash
        mHalAdapter.setLocaleConfig();
        verifyNoMoreInteractions(mFakeHalImplV1_1);
        verifyNoMoreInteractions(mFakeHalImplV2_0);
        // V1
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.setLocaleConfig();
        verify(mFakeHalImplV1_1).setLocaleConfig(eq(
                LocaleHelper.getCurrentLocaleConfig(mContext).getHalObject()));
        verifyNoMoreInteractions(mFakeHalImplV2_0);
        // V2
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        mHalAdapter.setLocaleConfig();
        verify(mFakeHalImplV2_0).setLocaleConfig(eq(
                LocaleHelper.getCurrentLocaleConfig(mContext).getHalObject()));
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testOnRegistration() throws RemoteException {
        // V1
        mHalAdapter.onRegistration(
                vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload.kInterfaceName,
                "",
                true);
        verify(mHalAdapter, times(1)).registerHalV1(eq(null));
        verifyNoMoreInteractions(mFakeHalImplV2_0);

        // V2
        mHalAdapter.onRegistration(
                vendor.google_clockwork.displayoffload.V2_0.IDisplayOffload.kInterfaceName,
                "",
                true);
        verify(mHalAdapter, times(1)).registerHalV2(eq(null));
        verifyNoMoreInteractions(mFakeHalImplV1_1);
    }

    @Test
    public void testRegisterHal() {
        mHalAdapter.registerHalV1(mFakeHalImplV1_1);
        assertThat(mHalAdapter.mDisplayOffloadV1_0).isEqualTo(mFakeHalImplV1_1);
        assertThat(mHalAdapter.mDisplayOffloadV1_1).isEqualTo(mFakeHalImplV1_1);

        mHalAdapter.registerHalV2(mFakeHalImplV2_0);
        assertThat(mHalAdapter.mDisplayOffloadV2_0).isEqualTo(mFakeHalImplV2_0);
    }

    @Test
    public void testHalListener() throws RemoteException {
        mHalAdapter.registerHalV1(mFakeHalImplV1_1);
        mHalAdapter.attachHalListener(mFakeHalListener);

        // onHalRegistered should be called when service becomes available
        mHalAdapter.onRegistration(
                vendor.google_clockwork.displayoffload.V1_0.IDisplayOffload.kInterfaceName,
                "",
                true);
        verify(mHalAdapter.mHalListener).onHalRegistered();

        // onHalDied should be called when service dies
        mHalAdapter.serviceDied(1);
        verify(mHalAdapter.mHalListener).onHalDied();
    }

    @Test
    public void testDeathRecipient() {
        // Already Dead
        mHalAdapter.serviceDied(1);
        verify(fakeBinder1, never()).unlinkToDeath(eq(mHalAdapter));
        mHalAdapter.serviceDied(0);
        verify(fakeBinder1, never()).unlinkToDeath(eq(mHalAdapter));

        // V1
        mHalAdapter.mDisplayOffloadV1_0 = mFakeHalImplV1_1;
        mHalAdapter.mDisplayOffloadV1_1 = mFakeHalImplV1_1;
        mHalAdapter.serviceDied(1);
        verify(fakeBinder1, times(2)).unlinkToDeath(eq(mHalAdapter));
        assertThat(mHalAdapter.mDisplayOffloadV1_0).isNull();
        assertThat(mHalAdapter.mDisplayOffloadV1_1).isNull();

        // V2
        mHalAdapter.mDisplayOffloadV2_0 = mFakeHalImplV2_0;
        mHalAdapter.serviceDied(2);
        verify(fakeBinder2, times(1)).unlinkToDeath(eq(mHalAdapter));
        assertThat(mHalAdapter.mDisplayOffloadV2_0).isNull();
    }

    public void verifySendHalObject(IFakeDisplayOffloadHal mHal)
            throws RemoteException, DisplayOffloadException {
        for (Object o : mHal.listOfHALObjects()) {
            mHalAdapter.send(o);
            mHal.verifyHalInteractions(o);
        }
    }

    interface IFakeDisplayOffloadHal {
        List<Object> listOfHALObjects();

        void verifyHalInteractions(Object o) throws RemoteException, DisplayOffloadException;
    }
}
