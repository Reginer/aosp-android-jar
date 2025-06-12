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

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresFeature;
import android.annotation.RequiresPermission;
import android.annotation.StringDef;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.annotation.WorkerThread;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.sysprop.HypervisorProperties;
import android.system.virtualizationservice.IVirtualizationService;
import android.util.ArrayMap;

import com.android.internal.annotations.GuardedBy;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Manages {@linkplain VirtualMachine virtual machine} instances created by an app. Each instance is
 * created from a {@linkplain VirtualMachineConfig configuration} that defines the shape of the VM
 * (RAM, CPUs), the code to execute within it, etc.
 *
 * <p>Each virtual machine instance is named; the configuration and related state of each is
 * persisted in the app's private data directory and an instance can be retrieved given the name.
 * The name must be a valid directory name and must not contain '/'.
 *
 * <p>The app can then start, stop and otherwise interact with the VM.
 *
 * <p>An instance of {@link VirtualMachineManager} can be obtained by calling {@link
 * Context#getSystemService(Class)}.
 *
 * @hide
 */
@SystemApi
@RequiresFeature(PackageManager.FEATURE_VIRTUALIZATION_FRAMEWORK)
public class VirtualMachineManager {
    /**
     * A lock used to synchronize the creation of virtual machines. It protects {@link #mVmsByName},
     * but is also held throughout VM creation / retrieval / deletion, to prevent these actions
     * racing with each other.
     */
    private static final Object sCreateLock = new Object();

    @NonNull private final Context mContext;

    /** @hide */
    public VirtualMachineManager(@NonNull Context context) {
        mContext = requireNonNull(context);
    }

    @GuardedBy("sCreateLock")
    private final Map<String, WeakReference<VirtualMachine>> mVmsByName = new ArrayMap<>();

    /**
     * Capabilities of the virtual machine implementation.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "CAPABILITY_",
            flag = true,
            value = {CAPABILITY_PROTECTED_VM, CAPABILITY_NON_PROTECTED_VM})
    public @interface Capability {}

    /**
     * The implementation supports creating protected VMs, whose memory is inaccessible to the host
     * OS.
     *
     * @see VirtualMachineConfig.Builder#setProtectedVm
     */
    public static final int CAPABILITY_PROTECTED_VM = 1;

    /**
     * The implementation supports creating non-protected VMs, whose memory is accessible to the
     * host OS.
     *
     * @see VirtualMachineConfig.Builder#setProtectedVm
     */
    public static final int CAPABILITY_NON_PROTECTED_VM = 2;

    /**
     * Features provided by {@link VirtualMachineManager}.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef(
            prefix = "FEATURE_",
            value = {
                FEATURE_DICE_CHANGES,
                FEATURE_LLPVM_CHANGES,
                FEATURE_MULTI_TENANT,
                FEATURE_NETWORK,
                FEATURE_REMOTE_ATTESTATION,
                FEATURE_VENDOR_MODULES,
            })
    public @interface Features {}

    /**
     * Feature to include new data in the VM DICE chain.
     *
     * @hide
     */
    @TestApi
    public static final String FEATURE_DICE_CHANGES = IVirtualizationService.FEATURE_DICE_CHANGES;

    /**
     * Feature to run payload as non-root user.
     *
     * @hide
     */
    @TestApi
    public static final String FEATURE_MULTI_TENANT = IVirtualizationService.FEATURE_MULTI_TENANT;

    /**
     * Feature to allow network features in VM.
     *
     * @hide
     */
    @TestApi public static final String FEATURE_NETWORK = IVirtualizationService.FEATURE_NETWORK;

    /**
     * Feature to allow remote attestation in Microdroid.
     *
     * @hide
     */
    @TestApi
    public static final String FEATURE_REMOTE_ATTESTATION =
            IVirtualizationService.FEATURE_REMOTE_ATTESTATION;

    /**
     * Feature to allow vendor modules in Microdroid.
     *
     * @hide
     */
    @TestApi
    public static final String FEATURE_VENDOR_MODULES =
            IVirtualizationService.FEATURE_VENDOR_MODULES;

    /**
     * Feature to enable Secretkeeper protected secrets in Microdroid based pVMs.
     *
     * @hide
     */
    @TestApi
    public static final String FEATURE_LLPVM_CHANGES = IVirtualizationService.FEATURE_LLPVM_CHANGES;

    /**
     * Returns a set of flags indicating what this implementation of virtualization is capable of.
     *
     * @see #CAPABILITY_PROTECTED_VM
     * @see #CAPABILITY_NON_PROTECTED_VM
     * @hide
     */
    @SystemApi
    @Capability
    public int getCapabilities() {
        @Capability int result = 0;
        if (HypervisorProperties.hypervisor_protected_vm_supported().orElse(false)) {
            result |= CAPABILITY_PROTECTED_VM;
        }
        if (HypervisorProperties.hypervisor_vm_supported().orElse(false)) {
            result |= CAPABILITY_NON_PROTECTED_VM;
        }
        return result;
    }

    /**
     * Creates a new {@link VirtualMachine} with the given name and config. Creating a virtual
     * machine with the same name as an existing virtual machine is an error. The existing virtual
     * machine has to be deleted before its name can be reused.
     *
     * <p>Each successful call to this method creates a new (and different) virtual machine even if
     * the name and the config are the same as a deleted one. The new virtual machine will initially
     * be stopped.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the VM cannot be created, or there is an existing VM with
     *     the given name.
     * @hide
     */
    @SystemApi
    @NonNull
    @WorkerThread
    @RequiresPermission(VirtualMachine.MANAGE_VIRTUAL_MACHINE_PERMISSION)
    public VirtualMachine create(@NonNull String name, @NonNull VirtualMachineConfig config)
            throws VirtualMachineException {
        synchronized (sCreateLock) {
            return createLocked(name, config);
        }
    }

    @NonNull
    @GuardedBy("sCreateLock")
    private VirtualMachine createLocked(@NonNull String name, @NonNull VirtualMachineConfig config)
            throws VirtualMachineException {
        VirtualMachine vm = VirtualMachine.create(mContext, name, config);
        mVmsByName.put(name, new WeakReference<>(vm));
        return vm;
    }

    /**
     * Returns an existing {@link VirtualMachine} with the given name. Returns null if there is no
     * such virtual machine.
     *
     * <p>There is at most one {@code VirtualMachine} object corresponding to a given virtual
     * machine instance. Multiple calls to get() passing the same name will get the same object
     * returned, until the virtual machine is deleted (via {@link #delete}) and then recreated.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @see #getOrCreate
     * @throws VirtualMachineException if the virtual machine exists but could not be successfully
     *     retrieved. This can be resolved by calling {@link #delete} on the VM.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @Nullable
    public VirtualMachine get(@NonNull String name) throws VirtualMachineException {
        synchronized (sCreateLock) {
            return getLocked(name);
        }
    }

    @Nullable
    @GuardedBy("sCreateLock")
    private VirtualMachine getLocked(@NonNull String name) throws VirtualMachineException {
        VirtualMachine vm = getVmByName(name);
        if (vm != null) return vm;

        vm = VirtualMachine.load(mContext, name);
        if (vm != null) {
            mVmsByName.put(name, new WeakReference<>(vm));
        }
        return vm;
    }

    /**
     * Imports a virtual machine from an {@link VirtualMachineDescriptor} object and associates it
     * with the given name.
     *
     * <p>The new virtual machine will be in the same state as the descriptor indicates. The
     * descriptor is automatically closed and cannot be used again.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the VM cannot be imported or the {@code
     *     VirtualMachineDescriptor} has already been closed.
     * @hide
     */
    @NonNull
    @SystemApi
    @WorkerThread
    public VirtualMachine importFromDescriptor(
            @NonNull String name, @NonNull VirtualMachineDescriptor vmDescriptor)
            throws VirtualMachineException {
        VirtualMachine vm;
        synchronized (sCreateLock) {
            vm = VirtualMachine.fromDescriptor(mContext, name, vmDescriptor);
            mVmsByName.put(name, new WeakReference<>(vm));
        }
        return vm;
    }

    /**
     * Returns an existing {@link VirtualMachine} if it exists, or create a new one. The config
     * parameter is used only when a new virtual machine is created.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the virtual machine could not be created or retrieved.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public VirtualMachine getOrCreate(@NonNull String name, @NonNull VirtualMachineConfig config)
            throws VirtualMachineException {
        synchronized (sCreateLock) {
            VirtualMachine vm = getLocked(name);
            if (vm != null) {
                return vm;
            } else {
                return createLocked(name, config);
            }
        }
    }

    /**
     * Deletes an existing {@link VirtualMachine}. Deleting a virtual machine means deleting any
     * persisted data associated with it including the per-VM secret. This is an irreversible
     * action. A virtual machine once deleted can never be restored. A new virtual machine created
     * with the same name is different from an already deleted virtual machine even if it has the
     * same config.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the virtual machine does not exist, is not stopped, or
     *     cannot be deleted.
     * @hide
     */
    @SystemApi
    @WorkerThread
    public void delete(@NonNull String name) throws VirtualMachineException {
        synchronized (sCreateLock) {
            VirtualMachine vm = getVmByName(name);
            if (vm == null) {
                VirtualMachine.vmInstanceCleanup(mContext, name);
            } else {
                vm.delete(mContext, name);
            }
            mVmsByName.remove(name);
        }
    }

    @Nullable
    @GuardedBy("sCreateLock")
    private VirtualMachine getVmByName(@NonNull String name) {
        requireNonNull(name);
        WeakReference<VirtualMachine> weakReference = mVmsByName.get(name);
        if (weakReference != null) {
            VirtualMachine vm = weakReference.get();
            if (vm != null && vm.getStatus() != VirtualMachine.STATUS_DELETED) {
                return vm;
            }
        }
        return null;
    }

    private static final String JSON_SUFFIX = ".json";
    private static final List<String> SUPPORTED_OS_LIST_FROM_CFG =
            extractSupportedOSListFromConfig();

    private static List<String> extractSupportedOSListFromConfig() {
        List<String> supportedOsList = new ArrayList<>();
        File directory = new File("/apex/com.android.virt/etc");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.endsWith(JSON_SUFFIX)) {
                    supportedOsList.add(
                            fileName.substring(0, fileName.length() - JSON_SUFFIX.length()));
                }
            }
        }
        return supportedOsList;
    }

    /**
     * Returns a list of supported OS names.
     *
     * @hide
     */
    @TestApi
    @NonNull
    public List<String> getSupportedOSList() throws VirtualMachineException {
        if (BuildFlags.VENDOR_MODULES_ENABLED) {
            return SUPPORTED_OS_LIST_FROM_CFG;
        } else {
            return Arrays.asList("microdroid");
        }
    }

    /**
     * Returns {@code true} if given {@code featureName} is enabled.
     *
     * @hide
     */
    @TestApi
    @RequiresPermission(VirtualMachine.MANAGE_VIRTUAL_MACHINE_PERMISSION)
    public boolean isFeatureEnabled(@Features String featureName) throws VirtualMachineException {
        synchronized (sCreateLock) {
            VirtualizationService service = VirtualizationService.getInstance();
            try {
                return service.getBinder().isFeatureEnabled(featureName);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    /**
     * Returns {@code true} if the pVM remote attestation feature is supported. Remote attestation
     * allows a protected VM to attest its authenticity to a remote server.
     *
     * @hide
     */
    @TestApi
    @RequiresPermission(VirtualMachine.MANAGE_VIRTUAL_MACHINE_PERMISSION)
    public boolean isRemoteAttestationSupported() throws VirtualMachineException {
        synchronized (sCreateLock) {
            VirtualizationService service = VirtualizationService.getInstance();
            try {
                return service.getBinder().isRemoteAttestationSupported();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    /**
     * Returns {@code true} if Updatable VM feature is supported by AVF. Updatable VM allow secrets
     * and data to be accessible even after updates of boot images and apks. For more info see
     * packages/modules/Virtualization/docs/updatable_vm.md
     *
     * @hide
     */
    @TestApi
    @RequiresPermission(VirtualMachine.MANAGE_VIRTUAL_MACHINE_PERMISSION)
    public boolean isUpdatableVmSupported() throws VirtualMachineException {
        synchronized (sCreateLock) {
            VirtualizationService service = VirtualizationService.getInstance();
            try {
                return service.getBinder().isUpdatableVmSupported();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }
}
