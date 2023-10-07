/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.clockwork.wristorientation;

import static com.android.clockwork.common.WristOrientationConstants.LEFT_WRIST_ROTATION_0;
import static com.android.clockwork.common.WristOrientationConstants.LEFT_WRIST_ROTATION_180;
import static com.android.clockwork.common.WristOrientationConstants.RIGHT_WRIST_ROTATION_0;
import static com.android.clockwork.common.WristOrientationConstants.RIGHT_WRIST_ROTATION_180;
import static com.android.clockwork.wristorientation.WristOrientationService.KEY_PROP_WRIST_ORIENTATION;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.view.IWindowManager;
import android.view.Surface;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import vendor.google_clockwork.wristorientation.V1_0.IWristOrientation;

@RunWith(RobolectricTestRunner.class)
public class WristOrientationServiceTest {

    @Mock
    Context mockContext;
    @Mock
    IWristOrientation mockWristOrientationHal;
    @Mock
    IWindowManager mockWindowManager;
    @Mock
    IServiceNotification mockServiceManagerCb;

    WristOrientationService wos;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        wos = new WristOrientationService(mockContext);
        wos.mWristOrientationHal = mockWristOrientationHal;
        wos.mWindowManager = mockWindowManager;

        // default orientation
        SystemProperties.set(KEY_PROP_WRIST_ORIENTATION,
                Integer.toString(LEFT_WRIST_ROTATION_0));
    }


    @Test
    public void testGetOrientation_checkDefaultOrientation_returnsLeftWristRotation0() {
        Assert.assertEquals(LEFT_WRIST_ROTATION_0,
                wos.getOrientation());
    }

    @Test
    public void testGetOrientation_afterSettingChange_returnsUpdatedOrientation() {
        wos.updateOrientation(LEFT_WRIST_ROTATION_180);
        Assert.assertEquals(LEFT_WRIST_ROTATION_180,
                wos.getOrientation());
    }

    @Test
    public void testSetOrientation_leftWristRotation0() throws RemoteException {
        // WHEN orientation is set to LEFT_WRIST_ROTATION_0
        wos.updateOrientation(LEFT_WRIST_ROTATION_0);


        // THEN the wrist orientation HAL is called
        verify(mockWristOrientationHal)
                .setOrientation((byte) LEFT_WRIST_ROTATION_0);
        // THEN the display rotation is set to 0
        verify(mockWindowManager).freezeRotation(Surface.ROTATION_0);

        // THEN the system property is updated
        Assert.assertEquals(LEFT_WRIST_ROTATION_0,
                SystemProperties.getInt(KEY_PROP_WRIST_ORIENTATION,
                        LEFT_WRIST_ROTATION_0));
    }

    @Test
    public void testSetOrientation_leftWristRotation180() throws RemoteException {
        // WHEN the orientation is set to LEFT_WRIST_ROTATION_180
        wos.updateOrientation(LEFT_WRIST_ROTATION_180);

        // THEN the wrist orientation HAL is called
        verify(mockWristOrientationHal)
                .setOrientation((byte) LEFT_WRIST_ROTATION_180);
        // THEN the display rotation is set to 180
        verify(mockWindowManager).freezeRotation(Surface.ROTATION_180);
        // THEN the system property is updated
        Assert.assertEquals(LEFT_WRIST_ROTATION_180,
                SystemProperties.getInt(KEY_PROP_WRIST_ORIENTATION,
                        LEFT_WRIST_ROTATION_0));
    }

    @Test
    public void testSetOrientation_rightWristRotation0() throws RemoteException {
        // WHEN orientation is set to RIGHT_WRIST_ROTATION_0
        wos.updateOrientation(RIGHT_WRIST_ROTATION_0);

        // THEN the wrist orientation HAL is called
        verify(mockWristOrientationHal)
                .setOrientation((byte) RIGHT_WRIST_ROTATION_0);
        // THEN the display rotation is set to 0
        verify(mockWindowManager).freezeRotation(Surface.ROTATION_0);
        // THEN the system property is updated
        Assert.assertEquals(RIGHT_WRIST_ROTATION_0,
                SystemProperties.getInt(KEY_PROP_WRIST_ORIENTATION,
                        LEFT_WRIST_ROTATION_0));

    }

    @Test
    public void testSetOrientation_rightWristRotation180() throws RemoteException {
        // WHEN orientation is set to RIGHT_WRIST_ROTATION_180
        wos.updateOrientation(RIGHT_WRIST_ROTATION_180);

        // THEN the wrist orientation HAL is called
        verify(mockWristOrientationHal)
                .setOrientation((byte) RIGHT_WRIST_ROTATION_180);
        // THEN the display rotation is set to 180
        verify(mockWindowManager).freezeRotation(Surface.ROTATION_180);
        // THEN the system property is updated
        Assert.assertEquals(RIGHT_WRIST_ROTATION_180,
                SystemProperties.getInt(KEY_PROP_WRIST_ORIENTATION,
                        LEFT_WRIST_ROTATION_0));
    }
}
