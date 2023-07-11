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
package com.android.internal.car.updatable;

import static com.android.car.internal.SystemConstants.ICAR_SYSTEM_SERVER_CLIENT;
import static com.android.car.internal.common.CommonConstants.CAR_SERVICE_INTERFACE;

import android.annotation.Nullable;
import android.car.ICar;
import android.car.ICarResultReceiver;
import android.car.builtin.os.UserManagerHelper;
import android.car.builtin.util.EventLogHelper;
import android.car.builtin.util.Slogf;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;

import com.android.car.internal.ICarServiceHelper;
import com.android.car.internal.ICarSystemServerClient;
import com.android.car.internal.util.IndentingPrintWriter;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.car.CarServiceHelperInterface;
import com.android.internal.car.CarServiceHelperServiceUpdatable;
import java.io.File;
import com.android.server.wm.CarLaunchParamsModifierInterface;
import com.android.server.wm.CarLaunchParamsModifierUpdatable;
import com.android.server.wm.CarLaunchParamsModifierUpdatableImpl;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * Implementation of the abstract class CarServiceHelperUpdatable
 */
public final class CarServiceHelperServiceUpdatableImpl
        implements CarServiceHelperServiceUpdatable, Executor {

    @VisibleForTesting
    static final String TAG = "CarServiceHelper";

    private static final boolean DBG = false;

    private static final String PROP_RESTART_RUNTIME = "ro.car.recovery.restart_runtime.enabled";

    private static final long CAR_SERVICE_BINDER_CALL_TIMEOUT_MS = 15_000;

    private final Runnable mCallbackForCarServiceUnresponsiveness;

    // exit code for
    private static final int STATUS_CODE_To_EXIT = 10;

    private static final String CAR_SERVICE_PACKAGE = "com.android.car";

    private final Context mContext;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private ICar mCarServiceBinder;

    private final Handler mHandler;
    private final HandlerThread mHandlerThread = new HandlerThread(
            CarServiceHelperServiceUpdatableImpl.class.getSimpleName());

    private final ICarServiceHelperImpl mHelper = new ICarServiceHelperImpl();

    private final CarServiceConnectedCallback mCarServiceConnectedCallback =
            new CarServiceConnectedCallback();

    private final CarServiceProxy mCarServiceProxy;

    private final CarServiceHelperInterface mCarServiceHelperInterface;

    private final CarLaunchParamsModifierUpdatableImpl mCarLaunchParamsModifierUpdatable;

    public CarServiceHelperServiceUpdatableImpl(Context context,
            CarServiceHelperInterface carServiceHelperInterface,
            CarLaunchParamsModifierInterface carLaunchParamsModifierInterface) {
        this(context, carServiceHelperInterface, carLaunchParamsModifierInterface,
                /* carServiceProxy= */ null);
    }

    @VisibleForTesting
    CarServiceHelperServiceUpdatableImpl(Context context,
            CarServiceHelperInterface carServiceHelperInterface,
            CarLaunchParamsModifierInterface carLaunchParamsModifierInterface,
            @Nullable CarServiceProxy carServiceProxy) {
        mContext = context;
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCarServiceHelperInterface = carServiceHelperInterface;
        mCarLaunchParamsModifierUpdatable = new CarLaunchParamsModifierUpdatableImpl(
                carLaunchParamsModifierInterface);
        // carServiceProxy is Nullable because it is not possible to construct carServiceProxy with
        // "this" object in the previous constructor as CarServiceHelperServiceUpdatableImpl has
        // not been fully constructed.
        mCarServiceProxy = carServiceProxy == null ? new CarServiceProxy(this) : carServiceProxy;
        mCallbackForCarServiceUnresponsiveness = () -> handleCarServiceUnresponsive();
    }

    private final ServiceConnection mCarServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (DBG) Slogf.d(TAG, "onServiceConnected: %s", iBinder);
            handleCarServiceConnection(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            handleCarServiceCrash();
        }
    };

    @Override
    public void onStart() {
        Intent intent = new Intent(CAR_SERVICE_INTERFACE).setPackage(CAR_SERVICE_PACKAGE);
        Context userContext = mContext.createContextAsUser(UserHandle.SYSTEM, /* flags= */ 0);
        if (!userContext.bindService(intent, Context.BIND_AUTO_CREATE, this,
                mCarServiceConnection)) {
            Slogf.wtf(TAG, "cannot start car service");
        }
    }

    @Override // From Executor
    public void execute(Runnable command) {
        mHandler.post(command);
    }

    @Override
    public void onUserRemoved(UserHandle user) {
        mCarServiceProxy.onUserRemoved(user);
    }

    @Override
    public void onFactoryReset(BiConsumer<Integer, Bundle> callback) {
        ICarResultReceiver resultReceiver = new ICarResultReceiver.Stub() {
            @Override
            public void send(int resultCode, Bundle resultData) throws RemoteException {
                callback.accept(resultCode, resultData);

            }
        };
        mCarServiceProxy.onFactoryReset(resultReceiver);
    }

    @Override
    public void initBootUser() {
        mCarServiceProxy.initBootUser();
    }

    @Override
    public CarLaunchParamsModifierUpdatable getCarLaunchParamsModifierUpdatable() {
        return mCarLaunchParamsModifierUpdatable;
    }

    @VisibleForTesting
    void handleCarServiceConnection(IBinder iBinder) {
        synchronized (mLock) {
            if (mCarServiceBinder == ICar.Stub.asInterface(iBinder)) {
                return; // already connected.
            }
            if (DBG) {
                Slogf.d(TAG, "car service binder changed, was %s new: %s", mCarServiceBinder,
                        iBinder);
            }
            mCarServiceBinder = ICar.Stub.asInterface(iBinder);
            Slogf.i(TAG, "**CarService connected**");
        }

        EventLogHelper.writeCarHelperServiceConnected();

        // Post mCallbackForCarServiceUnresponsiveness before setting system server connection
        // because CarService may respond before the sendSetSystemServerConnectionsCall call
        // returns and try to remove mCallbackForCarServiceUnresponsiveness from the handler.
        // Thus, posting this callback after setting system server connection may result in a race
        // condition where the callback is never removed from the handler.
        mHandler.removeCallbacks(mCallbackForCarServiceUnresponsiveness);
        mHandler.postDelayed(mCallbackForCarServiceUnresponsiveness,
                CAR_SERVICE_BINDER_CALL_TIMEOUT_MS);

        sendSetSystemServerConnectionsCall();
    }

    @VisibleForTesting
    void handleCarServiceCrash() {
        // Recovery behavior.  Kill the system server and reset
        // everything if enabled by the property.
        boolean restartOnServiceCrash = SystemProperties.getBoolean(PROP_RESTART_RUNTIME, false);
        mHandler.removeCallbacks(mCallbackForCarServiceUnresponsiveness);

        mCarServiceHelperInterface.dumpServiceStacks();
        if (restartOnServiceCrash) {
            Slogf.w(TAG, "*** CARHELPER KILLING SYSTEM PROCESS: CarService crash");
            Slogf.w(TAG, "*** GOODBYE!");
            Process.killProcess(Process.myPid());
            System.exit(STATUS_CODE_To_EXIT);
        } else {
            Slogf.w(TAG, "*** CARHELPER ignoring: CarService crash");
        }
    }

    private void sendSetSystemServerConnectionsCall() {
        ICar binder;
        synchronized (mLock) {
            binder = mCarServiceBinder;
        }
        try {
            binder.setSystemServerConnections(mHelper, mCarServiceConnectedCallback);
        } catch (RemoteException e) {
            Slogf.w(TAG, e, "RemoteException from car service");
            handleCarServiceCrash();
        } catch (RuntimeException e) {
            Slogf.wtf(TAG, e, "Exception calling setSystemServerConnections");
            throw e;
        }
    }

    private void handleCarServiceUnresponsive() {
        // This should not happen. Calling this method means ICarSystemServerClient binder is not
        // returned after service connection. and CarService has not connected in the given time.
        Slogf.w(TAG, "*** CARHELPER KILLING SYSTEM PROCESS: CarService unresponsive.");
        Slogf.w(TAG, "*** GOODBYE!");
        Process.killProcess(Process.myPid());
        System.exit(STATUS_CODE_To_EXIT);
    }

    @Override
    public void sendUserLifecycleEvent(int eventType, UserHandle userFrom, UserHandle userTo) {
        mCarServiceProxy.sendUserLifecycleEvent(eventType,
                userFrom == null ? UserManagerHelper.USER_NULL : userFrom.getIdentifier(),
                userTo.getIdentifier());
    }

    @Override
    public void dump(PrintWriter writer,  String[] args) {
        if (args != null && args.length > 0 && "--user-metrics-only".equals(args[0])) {
            mCarServiceProxy.dumpUserMetrics(new IndentingPrintWriter(writer));
            return;
        }

        if (args != null && args.length > 0 && "--dump-service-stacks".equals(args[0])) {
            File file = mCarServiceHelperInterface.dumpServiceStacks();
            if (file != null) {
                writer.printf("dumpServiceStacks ANR file path=%s\n", file.getAbsolutePath());
            } else {
                writer.printf("dumpServiceStacks no ANR file.\n");
            }
            return;
        }

        mCarServiceProxy.dump(new IndentingPrintWriter(writer));
    }

    private final class ICarServiceHelperImpl extends ICarServiceHelper.Stub {

        @Override
        public void setDisplayAllowlistForUser(int userId, int[] displayIds) {
            mCarLaunchParamsModifierUpdatable.setDisplayAllowListForUser(userId, displayIds);
        }

        @Override
        public void setPassengerDisplays(int[] displayIdsForPassenger) {
            mCarLaunchParamsModifierUpdatable.setPassengerDisplays(displayIdsForPassenger);
        }

        @Override
        public void setSourcePreferredComponents(boolean enableSourcePreferred,
                @Nullable List<ComponentName> sourcePreferredComponents) {
            mCarLaunchParamsModifierUpdatable.setSourcePreferredComponents(
                    enableSourcePreferred, sourcePreferredComponents);
        }

        @Override
        public int setPersistentActivity(ComponentName activity, int displayId, int featureId) {
            return mCarLaunchParamsModifierUpdatable.setPersistentActivity(
                    activity, displayId, featureId);
        }

        @Override
        public void setSafetyMode(boolean safe) {
            mCarServiceHelperInterface.setSafetyMode(safe);
        }

        @Override
        public UserHandle createUserEvenWhenDisallowed(String name, String userType, int flags) {
            return mCarServiceHelperInterface.createUserEvenWhenDisallowed(name, userType, flags);
        }

        @Override
        public void sendInitialUser(UserHandle user) {
            mCarServiceProxy.saveInitialUser(user);
        }
    }

    private final class CarServiceConnectedCallback extends ICarResultReceiver.Stub {
        @Override
        public void send(int resultCode, Bundle resultData) {
            mHandler.removeCallbacks(mCallbackForCarServiceUnresponsiveness);

            IBinder binder;
            if (resultData == null
                    || (binder = resultData.getBinder(ICAR_SYSTEM_SERVER_CLIENT)) == null) {
                Slogf.wtf(TAG, "setSystemServerConnections return NULL data or Binder.");
                handleCarServiceUnresponsive();
                return;
            }

            ICarSystemServerClient carService = ICarSystemServerClient.Stub.asInterface(binder);
            mCarServiceProxy.handleCarServiceConnection(carService);
        }
    }
}
