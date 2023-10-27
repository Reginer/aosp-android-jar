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

import static android.os.ParcelFileDescriptor.AutoCloseInputStream;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static android.os.ParcelFileDescriptor.MODE_READ_WRITE;
import static android.system.virtualmachine.VirtualMachineCallback.ERROR_PAYLOAD_CHANGED;
import static android.system.virtualmachine.VirtualMachineCallback.ERROR_PAYLOAD_INVALID_CONFIG;
import static android.system.virtualmachine.VirtualMachineCallback.ERROR_PAYLOAD_VERIFICATION_FAILED;
import static android.system.virtualmachine.VirtualMachineCallback.ERROR_UNKNOWN;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_CRASH;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_HANGUP;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_INFRASTRUCTURE_ERROR;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_KILLED;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_MICRODROID_FAILED_TO_CONNECT_TO_VIRTUALIZATION_SERVICE;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_MICRODROID_INVALID_PAYLOAD_CONFIG;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_MICRODROID_PAYLOAD_HAS_CHANGED;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_MICRODROID_PAYLOAD_VERIFICATION_FAILED;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_MICRODROID_UNKNOWN_RUNTIME_ERROR;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_PVM_FIRMWARE_INSTANCE_IMAGE_CHANGED;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_PVM_FIRMWARE_PUBLIC_KEY_MISMATCH;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_REBOOT;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_SHUTDOWN;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_START_FAILED;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_UNKNOWN;
import static android.system.virtualmachine.VirtualMachineCallback.STOP_REASON_VIRTUALIZATION_SERVICE_DIED;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.annotation.WorkerThread;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.virtualizationcommon.DeathReason;
import android.system.virtualizationcommon.ErrorCode;
import android.system.virtualizationservice.IVirtualMachine;
import android.system.virtualizationservice.IVirtualMachineCallback;
import android.system.virtualizationservice.IVirtualizationService;
import android.system.virtualizationservice.MemoryTrimLevel;
import android.system.virtualizationservice.PartitionType;
import android.system.virtualizationservice.VirtualMachineAppConfig;
import android.system.virtualizationservice.VirtualMachineState;
import android.util.JsonReader;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

/**
 * Represents an VM instance, with its own configuration and state. Instances are persistent and are
 * created or retrieved via {@link VirtualMachineManager}.
 *
 * <p>The {@link #run} method actually starts up the VM and allows the payload code to execute. It
 * will continue until it exits or {@link #stop} is called. Updates on the state of the VM can be
 * received using {@link #setCallback}. The app can communicate with the VM using {@link
 * #connectToVsockServer} or {@link #connectVsock}.
 *
 * <p>The payload code running inside the VM has access to a set of native APIs; see the <a
 * href="https://cs.android.com/android/platform/superproject/+/master:packages/modules/Virtualization/vm_payload/README.md">README
 * file</a> for details.
 *
 * <p>Each VM has a unique secret, computed from the APK that contains the code running in it, the
 * VM configuration, and a random per-instance salt. The secret can be accessed by the payload code
 * running inside the VM (using {@code AVmPayload_getVmInstanceSecret}) but is not made available
 * outside it.
 *
 * @hide
 */
@SystemApi
public class VirtualMachine implements AutoCloseable {
    /** The permission needed to create or run a virtual machine. */
    public static final String MANAGE_VIRTUAL_MACHINE_PERMISSION =
            "android.permission.MANAGE_VIRTUAL_MACHINE";

    /**
     * The permission needed to create a virtual machine with more advanced configuration options.
     */
    public static final String USE_CUSTOM_VIRTUAL_MACHINE_PERMISSION =
            "android.permission.USE_CUSTOM_VIRTUAL_MACHINE";

    /**
     * The lowest port number that can be used to communicate with the virtual machine payload.
     *
     * @see #connectToVsockServer
     * @see #connectVsock
     */
    @SuppressLint("MinMaxConstant") // Won't change: see man 7 vsock.
    public static final long MIN_VSOCK_PORT = 1024;

    /**
     * The highest port number that can be used to communicate with the virtual machine payload.
     *
     * @see #connectToVsockServer
     * @see #connectVsock
     */
    @SuppressLint("MinMaxConstant") // Won't change: see man 7 vsock.
    public static final long MAX_VSOCK_PORT = (1L << 32) - 1;

    /**
     * Status of a virtual machine
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "STATUS_", value = {
            STATUS_STOPPED,
            STATUS_RUNNING,
            STATUS_DELETED
    })
    public @interface Status {}

    /** The virtual machine has just been created, or {@link #stop} was called on it. */
    public static final int STATUS_STOPPED = 0;

    /** The virtual machine is running. */
    public static final int STATUS_RUNNING = 1;

    /**
     * The virtual machine has been deleted. This is an irreversible state. Once a virtual machine
     * is deleted all its secrets are permanently lost, and it cannot be run. A new virtual machine
     * with the same name and config may be created, with new and different secrets.
     */
    public static final int STATUS_DELETED = 2;

    private static final String TAG = "VirtualMachine";

    /** Name of the directory under the files directory where all VMs created for the app exist. */
    private static final String VM_DIR = "vm";

    /** Name of the persisted config file for a VM. */
    private static final String CONFIG_FILE = "config.xml";

    /** Name of the instance image file for a VM. (Not implemented) */
    private static final String INSTANCE_IMAGE_FILE = "instance.img";

    /** Name of the idsig file for a VM */
    private static final String IDSIG_FILE = "idsig";

    /** Name of the idsig files for extra APKs. */
    private static final String EXTRA_IDSIG_FILE_PREFIX = "extra_idsig_";

    /** Size of the instance image. 10 MB. */
    private static final long INSTANCE_FILE_SIZE = 10 * 1024 * 1024;

    /** Name of the file backing the encrypted storage */
    private static final String ENCRYPTED_STORE_FILE = "storage.img";

    /** The package which owns this VM. */
    @NonNull private final String mPackageName;

    /** Name of this VM within the package. The name should be unique in the package. */
    @NonNull private final String mName;

    /**
     * Path to the directory containing all the files related to this VM.
     */
    @NonNull private final File mVmRootPath;

    /**
     * Path to the config file for this VM. The config file is where the configuration is persisted.
     */
    @NonNull private final File mConfigFilePath;

    /** Path to the instance image file for this VM. */
    @NonNull private final File mInstanceFilePath;

    /** Path to the idsig file for this VM. */
    @NonNull private final File mIdsigFilePath;

    /** File that backs the encrypted storage - Will be null if not enabled. */
    @Nullable private final File mEncryptedStoreFilePath;

    /**
     * Unmodifiable list of extra apks. Apks are specified by the vm config, and corresponding
     * idsigs are to be generated.
     */
    @NonNull private final List<ExtraApkSpec> mExtraApks;

    private class MemoryManagementCallbacks implements ComponentCallbacks2 {
        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {}

        @Override
        public void onLowMemory() {}

        @Override
        public void onTrimMemory(int level) {
            @MemoryTrimLevel int vmTrimLevel;

            switch (level) {
                case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                    vmTrimLevel = MemoryTrimLevel.TRIM_MEMORY_RUNNING_CRITICAL;
                    break;
                case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                    vmTrimLevel = MemoryTrimLevel.TRIM_MEMORY_RUNNING_LOW;
                    break;
                case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                    vmTrimLevel = MemoryTrimLevel.TRIM_MEMORY_RUNNING_MODERATE;
                    break;
                case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                    /* Release as much memory as we can. The app is on the LMKD LRU kill list. */
                    vmTrimLevel = MemoryTrimLevel.TRIM_MEMORY_RUNNING_CRITICAL;
                    break;
                default:
                    /* Treat unrecognised messages as generic low-memory warnings. */
                    vmTrimLevel = MemoryTrimLevel.TRIM_MEMORY_RUNNING_LOW;
                    break;
            }

            synchronized (mLock) {
                try {
                    if (mVirtualMachine != null) {
                        mVirtualMachine.onTrimMemory(vmTrimLevel);
                    }
                } catch (Exception e) {
                    /* Caller doesn't want our exceptions. Log them instead. */
                    Log.w(TAG, "TrimMemory failed: ", e);
                }
            }
        }
    }

    /** Running instance of virtmgr that hosts VirtualizationService for this VM. */
    @NonNull private final VirtualizationService mVirtualizationService;

    @NonNull private final MemoryManagementCallbacks mMemoryManagementCallbacks;

    @NonNull private final Context mContext;

    // A note on lock ordering:
    // You can take mLock while holding VirtualMachineManager.sCreateLock, but not vice versa.
    // We never take any other lock while holding mCallbackLock; therefore you can
    // take mCallbackLock while holding any other lock.

    /** Lock protecting our mutable state (other than callbacks). */
    private final Object mLock = new Object();

    /** Lock protecting callbacks. */
    private final Object mCallbackLock = new Object();

    private final boolean mVmOutputCaptured;

    /** The configuration that is currently associated with this VM. */
    @GuardedBy("mLock")
    @NonNull
    private VirtualMachineConfig mConfig;

    /** Handle to the "running" VM. */
    @GuardedBy("mLock")
    @Nullable
    private IVirtualMachine mVirtualMachine;

    @GuardedBy("mLock")
    @Nullable
    private ParcelFileDescriptor mConsoleReader;

    @GuardedBy("mLock")
    @Nullable
    private ParcelFileDescriptor mConsoleWriter;

    @GuardedBy("mLock")
    @Nullable
    private ParcelFileDescriptor mLogReader;

    @GuardedBy("mLock")
    @Nullable
    private ParcelFileDescriptor mLogWriter;

    @GuardedBy("mLock")
    private boolean mWasDeleted = false;

    /** The registered callback */
    @GuardedBy("mCallbackLock")
    @Nullable
    private VirtualMachineCallback mCallback;

    /** The executor on which the callback will be executed */
    @GuardedBy("mCallbackLock")
    @Nullable
    private Executor mCallbackExecutor;

    private static class ExtraApkSpec {
        public final File apk;
        public final File idsig;

        ExtraApkSpec(File apk, File idsig) {
            this.apk = apk;
            this.idsig = idsig;
        }
    }

    static {
        System.loadLibrary("virtualmachine_jni");
    }

    private VirtualMachine(
            @NonNull Context context,
            @NonNull String name,
            @NonNull VirtualMachineConfig config,
            @NonNull VirtualizationService service)
            throws VirtualMachineException {
        mPackageName = context.getPackageName();
        mName = requireNonNull(name, "Name must not be null");
        mConfig = requireNonNull(config, "Config must not be null");
        mVirtualizationService = service;

        File thisVmDir = getVmDir(context, mName);
        mVmRootPath = thisVmDir;
        mConfigFilePath = new File(thisVmDir, CONFIG_FILE);
        mInstanceFilePath = new File(thisVmDir, INSTANCE_IMAGE_FILE);
        mIdsigFilePath = new File(thisVmDir, IDSIG_FILE);
        mExtraApks = setupExtraApks(context, config, thisVmDir);
        mMemoryManagementCallbacks = new MemoryManagementCallbacks();
        mContext = context;
        mEncryptedStoreFilePath =
                (config.isEncryptedStorageEnabled())
                        ? new File(thisVmDir, ENCRYPTED_STORE_FILE)
                        : null;

        mVmOutputCaptured = config.isVmOutputCaptured();
    }

    /**
     * Creates a virtual machine from an {@link VirtualMachineDescriptor} object and associates it
     * with the given name.
     *
     * <p>The new virtual machine will be in the same state as the descriptor indicates.
     *
     * <p>Once a virtual machine is imported it is persisted until it is deleted by calling {@link
     * #delete}. The imported virtual machine is in {@link #STATUS_STOPPED} state. To run the VM,
     * call {@link #run}.
     */
    @GuardedBy("VirtualMachineManager.sCreateLock")
    @NonNull
    static VirtualMachine fromDescriptor(
            @NonNull Context context,
            @NonNull String name,
            @NonNull VirtualMachineDescriptor vmDescriptor)
            throws VirtualMachineException {
        File vmDir = createVmDir(context, name);
        try {
            VirtualMachine vm;
            try (vmDescriptor) {
                VirtualMachineConfig config = VirtualMachineConfig.from(vmDescriptor.getConfigFd());
                vm = new VirtualMachine(context, name, config, VirtualizationService.getInstance());
                config.serialize(vm.mConfigFilePath);
                try {
                    vm.mInstanceFilePath.createNewFile();
                } catch (IOException e) {
                    throw new VirtualMachineException("failed to create instance image", e);
                }
                vm.importInstanceFrom(vmDescriptor.getInstanceImgFd());

                if (vmDescriptor.getEncryptedStoreFd() != null) {
                    try {
                        vm.mEncryptedStoreFilePath.createNewFile();
                    } catch (IOException e) {
                        throw new VirtualMachineException(
                                "failed to create encrypted storage image", e);
                    }
                    vm.importEncryptedStoreFrom(vmDescriptor.getEncryptedStoreFd());
                }
            }
            return vm;
        } catch (VirtualMachineException | RuntimeException e) {
            // If anything goes wrong, delete any files created so far and the VM's directory
            try {
                deleteRecursively(vmDir);
            } catch (IOException innerException) {
                e.addSuppressed(innerException);
            }
            throw e;
        }
    }

    /**
     * Creates a virtual machine with the given name and config. Once a virtual machine is created
     * it is persisted until it is deleted by calling {@link #delete}. The created virtual machine
     * is in {@link #STATUS_STOPPED} state. To run the VM, call {@link #run}.
     */
    @GuardedBy("VirtualMachineManager.sCreateLock")
    @NonNull
    static VirtualMachine create(
            @NonNull Context context, @NonNull String name, @NonNull VirtualMachineConfig config)
            throws VirtualMachineException {
        File vmDir = createVmDir(context, name);

        try {
            VirtualMachine vm =
                    new VirtualMachine(context, name, config, VirtualizationService.getInstance());
            config.serialize(vm.mConfigFilePath);
            try {
                vm.mInstanceFilePath.createNewFile();
            } catch (IOException e) {
                throw new VirtualMachineException("failed to create instance image", e);
            }
            if (config.isEncryptedStorageEnabled()) {
                try {
                    vm.mEncryptedStoreFilePath.createNewFile();
                } catch (IOException e) {
                    throw new VirtualMachineException(
                            "failed to create encrypted storage image", e);
                }
            }

            IVirtualizationService service = vm.mVirtualizationService.getBinder();

            try {
                service.initializeWritablePartition(
                        ParcelFileDescriptor.open(vm.mInstanceFilePath, MODE_READ_WRITE),
                        INSTANCE_FILE_SIZE,
                        PartitionType.ANDROID_VM_INSTANCE);
            } catch (FileNotFoundException e) {
                throw new VirtualMachineException("instance image missing", e);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            } catch (ServiceSpecificException | IllegalArgumentException e) {
                throw new VirtualMachineException("failed to create instance partition", e);
            }

            if (config.isEncryptedStorageEnabled()) {
                try {
                    service.initializeWritablePartition(
                            ParcelFileDescriptor.open(vm.mEncryptedStoreFilePath, MODE_READ_WRITE),
                            config.getEncryptedStorageBytes(),
                            PartitionType.ENCRYPTEDSTORE);
                } catch (FileNotFoundException e) {
                    throw new VirtualMachineException("encrypted storage image missing", e);
                } catch (RemoteException e) {
                    throw e.rethrowAsRuntimeException();
                } catch (ServiceSpecificException | IllegalArgumentException e) {
                    throw new VirtualMachineException(
                            "failed to create encrypted storage partition", e);
                }
            }
            return vm;
        } catch (VirtualMachineException | RuntimeException e) {
            // If anything goes wrong, delete any files created so far and the VM's directory
            try {
                deleteRecursively(vmDir);
            } catch (IOException innerException) {
                e.addSuppressed(innerException);
            }
            throw e;
        }
    }

    /** Loads a virtual machine that is already created before. */
    @GuardedBy("VirtualMachineManager.sCreateLock")
    @Nullable
    static VirtualMachine load(@NonNull Context context, @NonNull String name)
            throws VirtualMachineException {
        File thisVmDir = getVmDir(context, name);
        if (!thisVmDir.exists()) {
            // The VM doesn't exist.
            return null;
        }
        File configFilePath = new File(thisVmDir, CONFIG_FILE);
        VirtualMachineConfig config = VirtualMachineConfig.from(configFilePath);
        VirtualMachine vm =
                new VirtualMachine(context, name, config, VirtualizationService.getInstance());

        if (!vm.mInstanceFilePath.exists()) {
            throw new VirtualMachineException("instance image missing");
        }
        if (config.isEncryptedStorageEnabled() && !vm.mEncryptedStoreFilePath.exists()) {
            throw new VirtualMachineException("Storage image missing");
        }
        return vm;
    }

    @GuardedBy("VirtualMachineManager.sCreateLock")
    void delete(Context context, String name) throws VirtualMachineException {
        synchronized (mLock) {
            checkStopped();
            // Once we explicitly delete a VM it must remain permanently in the deleted state;
            // if a new VM is created with the same name (and files) that's unrelated.
            mWasDeleted = true;
        }
        deleteVmDirectory(context, name);
    }

    static void deleteVmDirectory(Context context, String name) throws VirtualMachineException {
        try {
            deleteRecursively(getVmDir(context, name));
        } catch (IOException e) {
            throw new VirtualMachineException(e);
        }
    }

    @GuardedBy("VirtualMachineManager.sCreateLock")
    @NonNull
    private static File createVmDir(@NonNull Context context, @NonNull String name)
            throws VirtualMachineException {
        File vmDir = getVmDir(context, name);
        try {
            // We don't need to undo this even if VM creation fails.
            Files.createDirectories(vmDir.getParentFile().toPath());

            // The checking of the existence of this directory and the creation of it is done
            // atomically. If the directory already exists (i.e. the VM with the same name was
            // already created), FileAlreadyExistsException is thrown.
            Files.createDirectory(vmDir.toPath());
        } catch (FileAlreadyExistsException e) {
            throw new VirtualMachineException("virtual machine already exists", e);
        } catch (IOException e) {
            throw new VirtualMachineException("failed to create directory for VM", e);
        }
        return vmDir;
    }

    @NonNull
    private static File getVmDir(@NonNull Context context, @NonNull String name) {
        if (name.contains(File.separator) || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Invalid VM name: " + name);
        }
        File vmRoot = new File(context.getDataDir(), VM_DIR);
        return new File(vmRoot, name);
    }

    /**
     * Returns the name of this virtual machine. The name is unique in the package and can't be
     * changed.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns the currently selected config of this virtual machine. There can be multiple virtual
     * machines sharing the same config. Even in that case, the virtual machines are completely
     * isolated from each other; they have different secrets. It is also possible that a virtual
     * machine can change its config, which can be done by calling {@link #setConfig}.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public VirtualMachineConfig getConfig() {
        synchronized (mLock) {
            return mConfig;
        }
    }

    /**
     * Returns the current status of this virtual machine.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @hide
     */
    @SystemApi
    @WorkerThread
    @Status
    public int getStatus() {
        IVirtualMachine virtualMachine;
        synchronized (mLock) {
            if (mWasDeleted) {
                return STATUS_DELETED;
            }
            virtualMachine = mVirtualMachine;
        }

        int status;
        if (virtualMachine == null) {
            status = STATUS_STOPPED;
        } else {
            try {
                status = stateToStatus(virtualMachine.getState());
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
        if (status == STATUS_STOPPED && !mVmRootPath.exists()) {
            // A VM can quite happily keep running if its backing files have been deleted.
            // But once it stops, it's gone forever.
            synchronized (mLock) {
                dropVm();
            }
            return STATUS_DELETED;
        }
        return status;
    }

    private int stateToStatus(@VirtualMachineState int state) {
        switch (state) {
            case VirtualMachineState.STARTING:
            case VirtualMachineState.STARTED:
            case VirtualMachineState.READY:
            case VirtualMachineState.FINISHED:
                return STATUS_RUNNING;
            case VirtualMachineState.NOT_STARTED:
            case VirtualMachineState.DEAD:
            default:
                return STATUS_STOPPED;
        }
    }

    // Throw an appropriate exception if we have a running VM, or the VM has been deleted.
    @GuardedBy("mLock")
    private void checkStopped() throws VirtualMachineException {
        if (mWasDeleted || !mVmRootPath.exists()) {
            throw new VirtualMachineException("VM has been deleted");
        }
        if (mVirtualMachine == null) {
            return;
        }
        try {
            if (stateToStatus(mVirtualMachine.getState()) != STATUS_STOPPED) {
                throw new VirtualMachineException("VM is not in stopped state");
            }
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
        // It's stopped, but we still have a reference to it - we can fix that.
        dropVm();
    }

    /**
     * This should only be called when we know our VM has stopped; we no longer need to hold a
     * reference to it (this allows resources to be GC'd) and we no longer need to be informed of
     * memory pressure.
     */
    @GuardedBy("mLock")
    private void dropVm() {
        mContext.unregisterComponentCallbacks(mMemoryManagementCallbacks);
        mVirtualMachine = null;
    }

    /** If we have an IVirtualMachine in the running state return it, otherwise throw. */
    @GuardedBy("mLock")
    private IVirtualMachine getRunningVm() throws VirtualMachineException {
        try {
            if (mVirtualMachine != null
                    && stateToStatus(mVirtualMachine.getState()) == STATUS_RUNNING) {
                return mVirtualMachine;
            } else {
                if (mWasDeleted || !mVmRootPath.exists()) {
                    throw new VirtualMachineException("VM has been deleted");
                } else {
                    throw new VirtualMachineException("VM is not in running state");
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Registers the callback object to get events from the virtual machine. If a callback was
     * already registered, it is replaced with the new one.
     *
     * @hide
     */
    @SystemApi
    public void setCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull VirtualMachineCallback callback) {
        synchronized (mCallbackLock) {
            mCallback = callback;
            mCallbackExecutor = executor;
        }
    }

    /**
     * Clears the currently registered callback.
     *
     * @hide
     */
    @SystemApi
    public void clearCallback() {
        synchronized (mCallbackLock) {
            mCallback = null;
            mCallbackExecutor = null;
        }
    }

    /** Executes a callback on the callback executor. */
    private void executeCallback(Consumer<VirtualMachineCallback> fn) {
        final VirtualMachineCallback callback;
        final Executor executor;
        synchronized (mCallbackLock) {
            callback = mCallback;
            executor = mCallbackExecutor;
        }
        if (callback == null || executor == null) {
            return;
        }
        final long restoreToken = Binder.clearCallingIdentity();
        try {
            executor.execute(() -> fn.accept(callback));
        } finally {
            Binder.restoreCallingIdentity(restoreToken);
        }
    }

    /**
     * Runs this virtual machine. The returning of this method however doesn't mean that the VM has
     * actually started running or the OS has booted there. Such events can be notified by
     * registering a callback using {@link #setCallback} before calling {@code run()}.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the virtual machine is not stopped or could not be
     *     started.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @RequiresPermission(MANAGE_VIRTUAL_MACHINE_PERMISSION)
    public void run() throws VirtualMachineException {
        synchronized (mLock) {
            checkStopped();

            try {
                mIdsigFilePath.createNewFile();
                for (ExtraApkSpec extraApk : mExtraApks) {
                    extraApk.idsig.createNewFile();
                }
            } catch (IOException e) {
                // If the file already exists, exception is not thrown.
                throw new VirtualMachineException("Failed to create APK signature file", e);
            }

            IVirtualizationService service = mVirtualizationService.getBinder();

            try {
                if (mVmOutputCaptured) {
                    createVmPipes();
                }

                VirtualMachineAppConfig appConfig =
                        getConfig().toVsConfig(mContext.getPackageManager());
                appConfig.name = mName;

                try {
                    createIdSigs(service, appConfig);
                } catch (FileNotFoundException e) {
                    throw new VirtualMachineException("Failed to generate APK signature", e);
                }

                android.system.virtualizationservice.VirtualMachineConfig vmConfigParcel =
                        android.system.virtualizationservice.VirtualMachineConfig.appConfig(
                                appConfig);

                mVirtualMachine = service.createVm(vmConfigParcel, mConsoleWriter, mLogWriter);
                mVirtualMachine.registerCallback(new CallbackTranslator(service));
                mContext.registerComponentCallbacks(mMemoryManagementCallbacks);
                mVirtualMachine.start();
            } catch (IllegalStateException | ServiceSpecificException e) {
                throw new VirtualMachineException(e);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    private void createIdSigs(IVirtualizationService service, VirtualMachineAppConfig appConfig)
            throws RemoteException, FileNotFoundException {
        // Fill the idsig file by hashing the apk
        service.createOrUpdateIdsigFile(
                appConfig.apk, ParcelFileDescriptor.open(mIdsigFilePath, MODE_READ_WRITE));

        for (ExtraApkSpec extraApk : mExtraApks) {
            service.createOrUpdateIdsigFile(
                    ParcelFileDescriptor.open(extraApk.apk, MODE_READ_ONLY),
                    ParcelFileDescriptor.open(extraApk.idsig, MODE_READ_WRITE));
        }

        // Re-open idsig files in read-only mode
        appConfig.idsig = ParcelFileDescriptor.open(mIdsigFilePath, MODE_READ_ONLY);
        appConfig.instanceImage = ParcelFileDescriptor.open(mInstanceFilePath, MODE_READ_WRITE);
        if (mEncryptedStoreFilePath != null) {
            appConfig.encryptedStorageImage =
                    ParcelFileDescriptor.open(mEncryptedStoreFilePath, MODE_READ_WRITE);
        }
        List<ParcelFileDescriptor> extraIdsigs = new ArrayList<>();
        for (ExtraApkSpec extraApk : mExtraApks) {
            extraIdsigs.add(ParcelFileDescriptor.open(extraApk.idsig, MODE_READ_ONLY));
        }
        appConfig.extraIdsigs = extraIdsigs;
    }

    @GuardedBy("mLock")
    private void createVmPipes() throws VirtualMachineException {
        try {
            if (mConsoleReader == null || mConsoleWriter == null) {
                ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                mConsoleReader = pipe[0];
                mConsoleWriter = pipe[1];
            }

            if (mLogReader == null || mLogWriter == null) {
                ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                mLogReader = pipe[0];
                mLogWriter = pipe[1];
            }
        } catch (IOException e) {
            throw new VirtualMachineException("Failed to create stream for VM", e);
        }
    }

    /**
     * Returns the stream object representing the console output from the virtual machine. The
     * console output is only available if the {@link VirtualMachineConfig} specifies that it should
     * be {@linkplain VirtualMachineConfig#isVmOutputCaptured captured}.
     *
     * <p>If you turn on output capture, you must consume data from {@code getConsoleOutput} -
     * because otherwise the code in the VM may get blocked when the pipe buffer fills up.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the stream could not be created, or capturing is turned
     *     off.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public InputStream getConsoleOutput() throws VirtualMachineException {
        if (!mVmOutputCaptured) {
            throw new VirtualMachineException("Capturing vm outputs is turned off");
        }
        synchronized (mLock) {
            createVmPipes();
            return new FileInputStream(mConsoleReader.getFileDescriptor());
        }
    }

    /**
     * Returns the stream object representing the log output from the virtual machine. The log
     * output is only available if the VirtualMachineConfig specifies that it should be {@linkplain
     * VirtualMachineConfig#isVmOutputCaptured captured}.
     *
     * <p>If you turn on output capture, you must consume data from {@code getLogOutput} - because
     * otherwise the code in the VM may get blocked when the pipe buffer fills up.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the stream could not be created, or capturing is turned
     *     off.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public InputStream getLogOutput() throws VirtualMachineException {
        if (!mVmOutputCaptured) {
            throw new VirtualMachineException("Capturing vm outputs is turned off");
        }
        synchronized (mLock) {
            createVmPipes();
            return new FileInputStream(mLogReader.getFileDescriptor());
        }
    }

    /**
     * Stops this virtual machine. Stopping a virtual machine is like pulling the plug on a real
     * computer; the machine halts immediately. Software running on the virtual machine is not
     * notified of the event. Writes to {@linkplain
     * VirtualMachineConfig.Builder#setEncryptedStorageBytes encrypted storage} might not be
     * persisted, and the instance might be left in an inconsistent state.
     *
     * <p>For a graceful shutdown, you could request the payload to call {@code exit()}, e.g. via a
     * {@linkplain #connectToVsockServer binder request}, and wait for {@link
     * VirtualMachineCallback#onPayloadFinished} to be called.
     *
     * <p>A stopped virtual machine can be re-started by calling {@link #run()}.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the virtual machine is not running or could not be
     *     stopped.
     * @hide
     */
    @SystemApi
    @WorkerThread
    public void stop() throws VirtualMachineException {
        synchronized (mLock) {
            if (mVirtualMachine == null) {
                throw new VirtualMachineException("VM is not running");
            }
            try {
                mVirtualMachine.stop();
                dropVm();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            } catch (ServiceSpecificException e) {
                throw new VirtualMachineException(e);
            }
        }
    }

    /**
     * Stops this virtual machine, if it is running.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @see #stop()
     * @hide
     */
    @SystemApi
    @WorkerThread
    @Override
    public void close() {
        synchronized (mLock) {
            if (mVirtualMachine == null) {
                return;
            }
            try {
                if (stateToStatus(mVirtualMachine.getState()) == STATUS_RUNNING) {
                    mVirtualMachine.stop();
                    dropVm();
                }
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            } catch (ServiceSpecificException e) {
                // Deliberately ignored; this almost certainly means the VM exited just as
                // we tried to stop it.
                Log.i(TAG, "Ignoring error on close()", e);
            }
        }
    }

    private static void deleteRecursively(File dir) throws IOException {
        // Note: This doesn't follow symlinks, which is important. Instead they are just deleted
        // (and Files.delete deletes the link not the target).
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                // Directory is deleted after we've visited (deleted) all its contents, so it
                // should be empty by now.
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Changes the config of this virtual machine to a new one. This can be used to adjust things
     * like the number of CPU and size of the RAM, depending on the situation (e.g. the size of the
     * application to run on the virtual machine, etc.)
     *
     * <p>The new config must be {@linkplain VirtualMachineConfig#isCompatibleWith compatible with}
     * the existing config.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @return the old config
     * @throws VirtualMachineException if the virtual machine is not stopped, or the new config is
     *     incompatible.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public VirtualMachineConfig setConfig(@NonNull VirtualMachineConfig newConfig)
            throws VirtualMachineException {
        synchronized (mLock) {
            VirtualMachineConfig oldConfig = mConfig;
            if (!oldConfig.isCompatibleWith(newConfig)) {
                throw new VirtualMachineException("incompatible config");
            }
            checkStopped();

            if (oldConfig != newConfig) {
                // Delete any existing file before recreating; that ensures any
                // VirtualMachineDescriptor that refers to the old file does not see the new config.
                mConfigFilePath.delete();
                newConfig.serialize(mConfigFilePath);
                mConfig = newConfig;
            }
            return oldConfig;
        }
    }

    @Nullable
    private static native IBinder nativeConnectToVsockServer(IBinder vmBinder, int port);

    /**
     * Connect to a VM's binder service via vsock and return the root IBinder object. Guest VMs are
     * expected to set up vsock servers in their payload. After the host app receives the {@link
     * VirtualMachineCallback#onPayloadReady}, it can use this method to establish a connection to
     * the guest VM.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if the virtual machine is not running or the connection
     *     failed.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public IBinder connectToVsockServer(
            @IntRange(from = MIN_VSOCK_PORT, to = MAX_VSOCK_PORT) long port)
            throws VirtualMachineException {

        synchronized (mLock) {
            IBinder iBinder =
                    nativeConnectToVsockServer(getRunningVm().asBinder(), validatePort(port));
            if (iBinder == null) {
                throw new VirtualMachineException("Failed to connect to vsock server");
            }
            return iBinder;
        }
    }

    /**
     * Opens a vsock connection to the VM on the given port.
     *
     * <p>The caller is responsible for closing the returned {@code ParcelFileDescriptor}.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @throws VirtualMachineException if connecting fails.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public ParcelFileDescriptor connectVsock(
            @IntRange(from = MIN_VSOCK_PORT, to = MAX_VSOCK_PORT) long port)
            throws VirtualMachineException {
        synchronized (mLock) {
            try {
                return getRunningVm().connectVsock(validatePort(port));
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            } catch (ServiceSpecificException e) {
                throw new VirtualMachineException(e);
            }
        }
    }

    private int validatePort(long port) {
        // Ports below 1024 are "privileged" (payload code can't bind to these), and port numbers
        // are 32-bit unsigned numbers at the OS level, even though we pass them as 32-bit signed
        // numbers internally.
        if (port < MIN_VSOCK_PORT || port > MAX_VSOCK_PORT) {
            throw new IllegalArgumentException("Bad port " + port);
        }
        return (int) port;
    }

    /**
     * Returns the root directory where all files related to this {@link VirtualMachine} (e.g.
     * {@code instance.img}, {@code apk.idsig}, etc) are stored.
     *
     * @hide
     */
    @TestApi
    @NonNull
    public File getRootDir() {
        return mVmRootPath;
    }

    /**
     * Captures the current state of the VM in a {@link VirtualMachineDescriptor} instance. The VM
     * needs to be stopped to avoid inconsistency in its state representation.
     *
     * <p>The state of the VM is not actually copied until {@link
     * VirtualMachineManager#importFromDescriptor} is called. It is recommended that the VM not be
     * started until that operation is complete.
     *
     * <p>NOTE: This method may block and should not be called on the main thread.
     *
     * @return a {@link VirtualMachineDescriptor} instance that represents the VM's state.
     * @throws VirtualMachineException if the virtual machine is not stopped, or the state could not
     *     be captured.
     * @hide
     */
    @SystemApi
    @WorkerThread
    @NonNull
    public VirtualMachineDescriptor toDescriptor() throws VirtualMachineException {
        synchronized (mLock) {
            checkStopped();
            try {
                return new VirtualMachineDescriptor(
                        ParcelFileDescriptor.open(mConfigFilePath, MODE_READ_ONLY),
                        ParcelFileDescriptor.open(mInstanceFilePath, MODE_READ_ONLY),
                        mEncryptedStoreFilePath != null
                                ? ParcelFileDescriptor.open(mEncryptedStoreFilePath, MODE_READ_ONLY)
                                : null);
            } catch (IOException e) {
                throw new VirtualMachineException(e);
            }
        }
    }

    @Override
    public String toString() {
        VirtualMachineConfig config = getConfig();
        String payloadConfigPath = config.getPayloadConfigPath();
        String payloadBinaryName = config.getPayloadBinaryName();

        StringBuilder result = new StringBuilder();
        result.append("VirtualMachine(")
                .append("name:")
                .append(getName())
                .append(", ");
        if (payloadBinaryName != null) {
            result.append("payload:").append(payloadBinaryName).append(", ");
        }
        if (payloadConfigPath != null) {
            result.append("config:")
                    .append(payloadConfigPath)
                    .append(", ");
        }
        result.append("package: ")
                .append(mPackageName)
                .append(")");
        return result.toString();
    }

    private static List<String> parseExtraApkListFromPayloadConfig(JsonReader reader)
            throws VirtualMachineException {
        /*
         * JSON schema from packages/modules/Virtualization/microdroid/payload/config/src/lib.rs:
         *
         * <p>{ "extra_apks": [ { "path": "/system/app/foo.apk", }, ... ], ... }
         */
        try {
            List<String> apks = new ArrayList<>();

            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("extra_apks")) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        String name = reader.nextName();
                        if (name.equals("path")) {
                            apks.add(reader.nextString());
                        } else {
                            reader.skipValue();
                        }
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return apks;
        } catch (IOException e) {
            throw new VirtualMachineException(e);
        }
    }

    /**
     * Reads the payload config inside the application, parses extra APK information, and then
     * creates corresponding idsig file paths.
     */
    private static List<ExtraApkSpec> setupExtraApks(
            @NonNull Context context, @NonNull VirtualMachineConfig config, @NonNull File vmDir)
            throws VirtualMachineException {
        String configPath = config.getPayloadConfigPath();
        if (configPath == null) {
            return Collections.emptyList();
        }
        try (ZipFile zipFile = new ZipFile(context.getPackageCodePath())) {
            InputStream inputStream =
                    zipFile.getInputStream(zipFile.getEntry(configPath));
            List<String> apkList =
                    parseExtraApkListFromPayloadConfig(
                            new JsonReader(new InputStreamReader(inputStream)));

            List<ExtraApkSpec> extraApks = new ArrayList<>();
            for (int i = 0; i < apkList.size(); ++i) {
                extraApks.add(
                        new ExtraApkSpec(
                                new File(apkList.get(i)),
                                new File(vmDir, EXTRA_IDSIG_FILE_PREFIX + i)));
            }

            return Collections.unmodifiableList(extraApks);
        } catch (IOException e) {
            throw new VirtualMachineException("Couldn't parse extra apks from the vm config", e);
        }
    }

    private void importInstanceFrom(@NonNull ParcelFileDescriptor instanceFd)
            throws VirtualMachineException {
        try (FileChannel instance = new FileOutputStream(mInstanceFilePath).getChannel();
                FileChannel instanceInput = new AutoCloseInputStream(instanceFd).getChannel()) {
            instance.transferFrom(instanceInput, /*position=*/ 0, instanceInput.size());
        } catch (IOException e) {
            throw new VirtualMachineException("failed to transfer instance image", e);
        }
    }

    private void importEncryptedStoreFrom(@NonNull ParcelFileDescriptor encryptedStoreFd)
            throws VirtualMachineException {
        try (FileChannel storeOutput = new FileOutputStream(mEncryptedStoreFilePath).getChannel();
                FileChannel storeInput = new AutoCloseInputStream(encryptedStoreFd).getChannel()) {
            storeOutput.transferFrom(storeInput, /*position=*/ 0, storeInput.size());
        } catch (IOException e) {
            throw new VirtualMachineException("failed to transfer encryptedstore image", e);
        }
    }

    /** Map the raw AIDL (& binder) callbacks to what the client expects. */
    private class CallbackTranslator extends IVirtualMachineCallback.Stub {
        private final IVirtualizationService mService;
        private final DeathRecipient mDeathRecipient;

        // The VM should only be observed to die once
        private final AtomicBoolean mOnDiedCalled = new AtomicBoolean(false);

        public CallbackTranslator(IVirtualizationService service) throws RemoteException {
            this.mService = service;
            this.mDeathRecipient = () -> reportStopped(STOP_REASON_VIRTUALIZATION_SERVICE_DIED);
            service.asBinder().linkToDeath(mDeathRecipient, 0);
        }

        @Override
        public void onPayloadStarted(int cid) {
            executeCallback((cb) -> cb.onPayloadStarted(VirtualMachine.this));
        }

        @Override
        public void onPayloadReady(int cid) {
            executeCallback((cb) -> cb.onPayloadReady(VirtualMachine.this));
        }

        @Override
        public void onPayloadFinished(int cid, int exitCode) {
            executeCallback((cb) -> cb.onPayloadFinished(VirtualMachine.this, exitCode));
        }

        @Override
        public void onError(int cid, int errorCode, String message) {
            int translatedError = getTranslatedError(errorCode);
            executeCallback((cb) -> cb.onError(VirtualMachine.this, translatedError, message));
        }

        @Override
        public void onDied(int cid, int reason) {
            int translatedReason = getTranslatedReason(reason);
            reportStopped(translatedReason);
            mService.asBinder().unlinkToDeath(mDeathRecipient, 0);
        }

        private void reportStopped(@VirtualMachineCallback.StopReason int reason) {
            if (mOnDiedCalled.compareAndSet(false, true)) {
                executeCallback((cb) -> cb.onStopped(VirtualMachine.this, reason));
            }
        }

        @VirtualMachineCallback.ErrorCode
        private int getTranslatedError(int reason) {
            switch (reason) {
                case ErrorCode.PAYLOAD_VERIFICATION_FAILED:
                    return ERROR_PAYLOAD_VERIFICATION_FAILED;
                case ErrorCode.PAYLOAD_CHANGED:
                    return ERROR_PAYLOAD_CHANGED;
                case ErrorCode.PAYLOAD_CONFIG_INVALID:
                    return ERROR_PAYLOAD_INVALID_CONFIG;
                default:
                    return ERROR_UNKNOWN;
            }
        }

        @VirtualMachineCallback.StopReason
        private int getTranslatedReason(int reason) {
            switch (reason) {
                case DeathReason.INFRASTRUCTURE_ERROR:
                    return STOP_REASON_INFRASTRUCTURE_ERROR;
                case DeathReason.KILLED:
                    return STOP_REASON_KILLED;
                case DeathReason.SHUTDOWN:
                    return STOP_REASON_SHUTDOWN;
                case DeathReason.START_FAILED:
                    return STOP_REASON_START_FAILED;
                case DeathReason.REBOOT:
                    return STOP_REASON_REBOOT;
                case DeathReason.CRASH:
                    return STOP_REASON_CRASH;
                case DeathReason.PVM_FIRMWARE_PUBLIC_KEY_MISMATCH:
                    return STOP_REASON_PVM_FIRMWARE_PUBLIC_KEY_MISMATCH;
                case DeathReason.PVM_FIRMWARE_INSTANCE_IMAGE_CHANGED:
                    return STOP_REASON_PVM_FIRMWARE_INSTANCE_IMAGE_CHANGED;
                case DeathReason.MICRODROID_FAILED_TO_CONNECT_TO_VIRTUALIZATION_SERVICE:
                    return STOP_REASON_MICRODROID_FAILED_TO_CONNECT_TO_VIRTUALIZATION_SERVICE;
                case DeathReason.MICRODROID_PAYLOAD_HAS_CHANGED:
                    return STOP_REASON_MICRODROID_PAYLOAD_HAS_CHANGED;
                case DeathReason.MICRODROID_PAYLOAD_VERIFICATION_FAILED:
                    return STOP_REASON_MICRODROID_PAYLOAD_VERIFICATION_FAILED;
                case DeathReason.MICRODROID_INVALID_PAYLOAD_CONFIG:
                    return STOP_REASON_MICRODROID_INVALID_PAYLOAD_CONFIG;
                case DeathReason.MICRODROID_UNKNOWN_RUNTIME_ERROR:
                    return STOP_REASON_MICRODROID_UNKNOWN_RUNTIME_ERROR;
                case DeathReason.HANGUP:
                    return STOP_REASON_HANGUP;
                default:
                    return STOP_REASON_UNKNOWN;
            }
        }
    }
}
