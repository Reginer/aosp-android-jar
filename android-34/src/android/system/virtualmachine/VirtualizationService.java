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

package android.system.virtualmachine;

import android.annotation.NonNull;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.system.virtualizationservice.IVirtualizationService;

import com.android.internal.annotations.GuardedBy;

import java.lang.ref.WeakReference;

/** A running instance of virtmgr that is hosting a VirtualizationService AIDL service. */
class VirtualizationService {
    static {
        System.loadLibrary("virtualizationservice_jni");
    }

    /* Soft reference caching the last created instance of this class. */
    @GuardedBy("VirtualMachineManager.sCreateLock")
    private static WeakReference<VirtualizationService> sInstance;

    /*
     * Client FD for UDS connection to virtmgr's RpcBinder server. Closing it
     * will make virtmgr shut down.
     */
    private final ParcelFileDescriptor mClientFd;

    /* Persistent connection to IVirtualizationService. */
    private final IVirtualizationService mBinder;

    private static native int nativeSpawn();

    private native IBinder nativeConnect(int clientFd);

    private native boolean nativeIsOk(int clientFd);

    /*
     * Spawns a new virtmgr subprocess that will host a VirtualizationService
     * AIDL service.
     */
    private VirtualizationService() throws VirtualMachineException {
        int clientFd = nativeSpawn();
        if (clientFd < 0) {
            throw new VirtualMachineException("Could not spawn VirtualizationService");
        }
        mClientFd = ParcelFileDescriptor.adoptFd(clientFd);

        IBinder binder = nativeConnect(mClientFd.getFd());
        if (binder == null) {
            throw new VirtualMachineException("Could not connect to VirtualizationService");
        }
        mBinder = IVirtualizationService.Stub.asInterface(binder);
    }

    /* Returns the IVirtualizationService binder. */
    @NonNull
    IVirtualizationService getBinder() {
        return mBinder;
    }

    /*
     * Checks the state of the client FD. Returns false if the FD is in erroneous state
     * or if the other endpoint had closed its FD.
     */
    private boolean isOk() {
        return nativeIsOk(mClientFd.getFd());
    }

    /*
     * Returns an instance of this class. Might spawn a new instance if one doesn't exist, or
     * if the previous instance had crashed.
     */
    @GuardedBy("VirtualMachineManager.sCreateLock")
    @NonNull
    static VirtualizationService getInstance() throws VirtualMachineException {
        VirtualizationService service = (sInstance == null) ? null : sInstance.get();
        if (service == null || !service.isOk()) {
            service = new VirtualizationService();
            sInstance = new WeakReference<>(service);
        }
        return service;
    }
}
