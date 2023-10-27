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

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.sysprop.HypervisorProperties;
import android.system.virtualizationservice.VirtualMachineAppConfig;
import android.system.virtualizationservice.VirtualMachinePayloadConfig;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.zip.ZipFile;

/**
 * Represents a configuration of a virtual machine. A configuration consists of hardware
 * configurations like the number of CPUs and the size of RAM, and software configurations like the
 * payload to run on the virtual machine.
 *
 * @hide
 */
@SystemApi
public final class VirtualMachineConfig {
    private static final String TAG = "VirtualMachineConfig";
    private static final String[] EMPTY_STRING_ARRAY = {};

    // These define the schema of the config file persisted on disk.
    private static final int VERSION = 6;
    private static final String KEY_VERSION = "version";
    private static final String KEY_PACKAGENAME = "packageName";
    private static final String KEY_APKPATH = "apkPath";
    private static final String KEY_PAYLOADCONFIGPATH = "payloadConfigPath";
    private static final String KEY_PAYLOADBINARYNAME = "payloadBinaryPath";
    private static final String KEY_DEBUGLEVEL = "debugLevel";
    private static final String KEY_PROTECTED_VM = "protectedVm";
    private static final String KEY_MEMORY_BYTES = "memoryBytes";
    private static final String KEY_CPU_TOPOLOGY = "cpuTopology";
    private static final String KEY_ENCRYPTED_STORAGE_BYTES = "encryptedStorageBytes";
    private static final String KEY_VM_OUTPUT_CAPTURED = "vmOutputCaptured";

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "DEBUG_LEVEL_", value = {
            DEBUG_LEVEL_NONE,
            DEBUG_LEVEL_FULL
    })
    public @interface DebugLevel {}

    /**
     * Not debuggable at all. No log is exported from the VM. Debugger can't be attached to the app
     * process running in the VM. This is the default level.
     *
     * @hide
     */
    @SystemApi public static final int DEBUG_LEVEL_NONE = 0;

    /**
     * Fully debuggable. All logs (both logcat and kernel message) are exported. All processes
     * running in the VM can be attached to the debugger. Rooting is possible.
     *
     * @hide
     */
    @SystemApi public static final int DEBUG_LEVEL_FULL = 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "CPU_TOPOLOGY_",
            value = {
                CPU_TOPOLOGY_ONE_CPU,
                CPU_TOPOLOGY_MATCH_HOST,
            })
    public @interface CpuTopology {}

    /**
     * Run VM with 1 vCPU. This is the default option, usually the fastest to boot and consuming the
     * least amount of resources. Typically the best option for small or ephemeral workloads.
     *
     * @hide
     */
    @SystemApi public static final int CPU_TOPOLOGY_ONE_CPU = 0;

    /**
     * Run VM with vCPU topology matching the physical CPU topology of the host. Usually takes
     * longer to boot and cosumes more resources compared to a single vCPU. Typically a good option
     * for long-running workloads that benefit from parallel execution.
     *
     * @hide
     */
    @SystemApi public static final int CPU_TOPOLOGY_MATCH_HOST = 1;

    /** Name of a package whose primary APK contains the VM payload. */
    @Nullable private final String mPackageName;

    /** Absolute path to the APK file containing the VM payload. */
    @Nullable private final String mApkPath;

    @DebugLevel private final int mDebugLevel;

    /**
     * Whether to run the VM in protected mode, so the host can't access its memory.
     */
    private final boolean mProtectedVm;

    /**
     * The amount of RAM to give the VM, in bytes. If this is 0 or negative the default will be
     * used.
     */
    private final long mMemoryBytes;

    /** CPU topology configuration of the VM. */
    @CpuTopology private final int mCpuTopology;

    /**
     * Path within the APK to the payload config file that defines software aspects of the VM.
     */
    @Nullable private final String mPayloadConfigPath;

    /** Name of the payload binary file within the APK that will be executed within the VM. */
    @Nullable private final String mPayloadBinaryName;

    /** The size of storage in bytes. 0 indicates that encryptedStorage is not required */
    private final long mEncryptedStorageBytes;

    /** Whether the app can read console and log output. */
    private final boolean mVmOutputCaptured;

    private VirtualMachineConfig(
            @Nullable String packageName,
            @Nullable String apkPath,
            @Nullable String payloadConfigPath,
            @Nullable String payloadBinaryName,
            @DebugLevel int debugLevel,
            boolean protectedVm,
            long memoryBytes,
            @CpuTopology int cpuTopology,
            long encryptedStorageBytes,
            boolean vmOutputCaptured) {
        // This is only called from Builder.build(); the builder handles parameter validation.
        mPackageName = packageName;
        mApkPath = apkPath;
        mPayloadConfigPath = payloadConfigPath;
        mPayloadBinaryName = payloadBinaryName;
        mDebugLevel = debugLevel;
        mProtectedVm = protectedVm;
        mMemoryBytes = memoryBytes;
        mCpuTopology = cpuTopology;
        mEncryptedStorageBytes = encryptedStorageBytes;
        mVmOutputCaptured = vmOutputCaptured;
    }

    /** Loads a config from a file. */
    @NonNull
    static VirtualMachineConfig from(@NonNull File file) throws VirtualMachineException {
        try (FileInputStream input = new FileInputStream(file)) {
            return fromInputStream(input);
        } catch (IOException e) {
            throw new VirtualMachineException("Failed to read VM config from file", e);
        }
    }

    /** Loads a config from a {@link ParcelFileDescriptor}. */
    @NonNull
    static VirtualMachineConfig from(@NonNull ParcelFileDescriptor fd)
            throws VirtualMachineException {
        try (AutoCloseInputStream input = new AutoCloseInputStream(fd)) {
            return fromInputStream(input);
        } catch (IOException e) {
            throw new VirtualMachineException("failed to read VM config from file descriptor", e);
        }
    }

    /** Loads a config from a stream, for example a file. */
    @NonNull
    private static VirtualMachineConfig fromInputStream(@NonNull InputStream input)
            throws IOException, VirtualMachineException {
        PersistableBundle b = PersistableBundle.readFromStream(input);
        try {
            return fromPersistableBundle(b);
        } catch (NullPointerException | IllegalArgumentException | IllegalStateException e) {
            throw new VirtualMachineException("Persisted VM config is invalid", e);
        }
    }

    @NonNull
    private static VirtualMachineConfig fromPersistableBundle(PersistableBundle b) {
        int version = b.getInt(KEY_VERSION);
        if (version > VERSION) {
            throw new IllegalArgumentException(
                    "Version " + version + " too high; current is " + VERSION);
        }

        String packageName = b.getString(KEY_PACKAGENAME);
        Builder builder = new Builder(packageName);

        String apkPath = b.getString(KEY_APKPATH);
        if (apkPath != null) {
            builder.setApkPath(apkPath);
        }

        String payloadConfigPath = b.getString(KEY_PAYLOADCONFIGPATH);
        if (payloadConfigPath == null) {
            builder.setPayloadBinaryName(b.getString(KEY_PAYLOADBINARYNAME));
        } else {
            builder.setPayloadConfigPath(payloadConfigPath);
        }

        @DebugLevel int debugLevel = b.getInt(KEY_DEBUGLEVEL);
        if (debugLevel != DEBUG_LEVEL_NONE && debugLevel != DEBUG_LEVEL_FULL) {
            throw new IllegalArgumentException("Invalid debugLevel: " + debugLevel);
        }
        builder.setDebugLevel(debugLevel);
        builder.setProtectedVm(b.getBoolean(KEY_PROTECTED_VM));
        long memoryBytes = b.getLong(KEY_MEMORY_BYTES);
        if (memoryBytes != 0) {
            builder.setMemoryBytes(memoryBytes);
        }
        builder.setCpuTopology(b.getInt(KEY_CPU_TOPOLOGY));
        long encryptedStorageBytes = b.getLong(KEY_ENCRYPTED_STORAGE_BYTES);
        if (encryptedStorageBytes != 0) {
            builder.setEncryptedStorageBytes(encryptedStorageBytes);
        }
        builder.setVmOutputCaptured(b.getBoolean(KEY_VM_OUTPUT_CAPTURED));

        return builder.build();
    }

    /** Persists this config to a file. */
    void serialize(@NonNull File file) throws VirtualMachineException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            serializeOutputStream(output);
        } catch (IOException e) {
            throw new VirtualMachineException("failed to write VM config", e);
        }
    }

    /** Persists this config to a stream, for example a file. */
    private void serializeOutputStream(@NonNull OutputStream output) throws IOException {
        PersistableBundle b = new PersistableBundle();
        b.putInt(KEY_VERSION, VERSION);
        if (mPackageName != null) {
            b.putString(KEY_PACKAGENAME, mPackageName);
        }
        if (mApkPath != null) {
            b.putString(KEY_APKPATH, mApkPath);
        }
        b.putString(KEY_PAYLOADCONFIGPATH, mPayloadConfigPath);
        b.putString(KEY_PAYLOADBINARYNAME, mPayloadBinaryName);
        b.putInt(KEY_DEBUGLEVEL, mDebugLevel);
        b.putBoolean(KEY_PROTECTED_VM, mProtectedVm);
        b.putInt(KEY_CPU_TOPOLOGY, mCpuTopology);
        if (mMemoryBytes > 0) {
            b.putLong(KEY_MEMORY_BYTES, mMemoryBytes);
        }
        if (mEncryptedStorageBytes > 0) {
            b.putLong(KEY_ENCRYPTED_STORAGE_BYTES, mEncryptedStorageBytes);
        }
        b.putBoolean(KEY_VM_OUTPUT_CAPTURED, mVmOutputCaptured);
        b.writeToStream(output);
    }

    /**
     * Returns the absolute path of the APK which should contain the binary payload that will
     * execute within the VM. Returns null if no specific path has been set.
     *
     * @hide
     */
    @SystemApi
    @Nullable
    public String getApkPath() {
        return mApkPath;
    }

    /**
     * Returns the path within the APK to the payload config file that defines software aspects of
     * the VM.
     *
     * @hide
     */
    @TestApi
    @Nullable
    public String getPayloadConfigPath() {
        return mPayloadConfigPath;
    }

    /**
     * Returns the name of the payload binary file, in the {@code lib/<ABI>} directory of the APK,
     * that will be executed within the VM.
     *
     * @hide
     */
    @SystemApi
    @Nullable
    public String getPayloadBinaryName() {
        return mPayloadBinaryName;
    }

    /**
     * Returns the debug level for the VM.
     *
     * @hide
     */
    @SystemApi
    @DebugLevel
    public int getDebugLevel() {
        return mDebugLevel;
    }

    /**
     * Returns whether the VM's memory will be protected from the host.
     *
     * @hide
     */
    @SystemApi
    public boolean isProtectedVm() {
        return mProtectedVm;
    }

    /**
     * Returns the amount of RAM that will be made available to the VM, or 0 if the default size
     * will be used.
     *
     * @hide
     */
    @SystemApi
    @IntRange(from = 0)
    public long getMemoryBytes() {
        return mMemoryBytes;
    }

    /**
     * Returns the CPU topology configuration of the VM.
     *
     * @hide
     */
    @SystemApi
    @CpuTopology
    public int getCpuTopology() {
        return mCpuTopology;
    }

    /**
     * Returns whether encrypted storage is enabled or not.
     *
     * @hide
     */
    @SystemApi
    public boolean isEncryptedStorageEnabled() {
        return mEncryptedStorageBytes > 0;
    }

    /**
     * Returns the size of encrypted storage (in bytes) available in the VM, or 0 if encrypted
     * storage is not enabled
     *
     * @hide
     */
    @SystemApi
    @IntRange(from = 0)
    public long getEncryptedStorageBytes() {
        return mEncryptedStorageBytes;
    }

    /**
     * Returns whether the app can read the VM console or log output. If not, the VM output is
     * automatically forwarded to the host logcat.
     *
     * @see Builder#setVmOutputCaptured
     * @hide
     */
    @SystemApi
    public boolean isVmOutputCaptured() {
        return mVmOutputCaptured;
    }

    /**
     * Tests if this config is compatible with other config. Being compatible means that the configs
     * can be interchangeably used for the same virtual machine; they do not change the VM identity
     * or secrets. Such changes include varying the number of CPUs or the size of the RAM. Changes
     * that would alter the identity of the VM (e.g. using a different payload or changing the debug
     * mode) are considered incompatible.
     *
     * @see VirtualMachine#setConfig
     * @hide
     */
    @SystemApi
    public boolean isCompatibleWith(@NonNull VirtualMachineConfig other) {
        if (this == other) {
            return true;
        }
        return this.mDebugLevel == other.mDebugLevel
                && this.mProtectedVm == other.mProtectedVm
                && this.mEncryptedStorageBytes == other.mEncryptedStorageBytes
                && this.mVmOutputCaptured == other.mVmOutputCaptured
                && Objects.equals(this.mPayloadConfigPath, other.mPayloadConfigPath)
                && Objects.equals(this.mPayloadBinaryName, other.mPayloadBinaryName)
                && Objects.equals(this.mPackageName, other.mPackageName)
                && Objects.equals(this.mApkPath, other.mApkPath);
    }

    /**
     * Converts this config object into the parcelable type used when creating a VM via the
     * virtualization service. Notice that the files are not passed as paths, but as file
     * descriptors because the service doesn't accept paths as it might not have permission to open
     * app-owned files and that could be abused to run a VM with software that the calling
     * application doesn't own.
     */
    VirtualMachineAppConfig toVsConfig(@NonNull PackageManager packageManager)
            throws VirtualMachineException {
        VirtualMachineAppConfig vsConfig = new VirtualMachineAppConfig();

        String apkPath = (mApkPath != null) ? mApkPath : findPayloadApk(packageManager);

        try {
            vsConfig.apk = ParcelFileDescriptor.open(new File(apkPath), MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            throw new VirtualMachineException("Failed to open APK", e);
        }
        if (mPayloadBinaryName != null) {
            VirtualMachinePayloadConfig payloadConfig = new VirtualMachinePayloadConfig();
            payloadConfig.payloadBinaryName = mPayloadBinaryName;
            vsConfig.payload =
                    VirtualMachineAppConfig.Payload.payloadConfig(payloadConfig);
        } else {
            vsConfig.payload =
                    VirtualMachineAppConfig.Payload.configPath(mPayloadConfigPath);
        }
        switch (mDebugLevel) {
            case DEBUG_LEVEL_FULL:
                vsConfig.debugLevel = VirtualMachineAppConfig.DebugLevel.FULL;
                break;
            default:
                vsConfig.debugLevel = VirtualMachineAppConfig.DebugLevel.NONE;
                break;
        }
        vsConfig.protectedVm = mProtectedVm;
        vsConfig.memoryMib = bytesToMebiBytes(mMemoryBytes);
        switch (mCpuTopology) {
            case CPU_TOPOLOGY_MATCH_HOST:
                vsConfig.cpuTopology = android.system.virtualizationservice.CpuTopology.MATCH_HOST;
                break;
            default:
                vsConfig.cpuTopology = android.system.virtualizationservice.CpuTopology.ONE_CPU;
                break;
        }
        // Don't allow apps to set task profiles ... at least for now.
        vsConfig.taskProfiles = EMPTY_STRING_ARRAY;
        return vsConfig;
    }

    private String findPayloadApk(PackageManager packageManager) throws VirtualMachineException {
        ApplicationInfo appInfo;
        try {
            appInfo =
                    packageManager.getApplicationInfo(
                            mPackageName, PackageManager.ApplicationInfoFlags.of(0));
        } catch (PackageManager.NameNotFoundException e) {
            throw new VirtualMachineException("Package not found", e);
        }

        String[] splitApkPaths = appInfo.splitSourceDirs;
        String[] abis = Build.SUPPORTED_64_BIT_ABIS;

        // If there are split APKs, and we know the payload binary name, see if we can find a
        // split APK containing the binary.
        if (mPayloadBinaryName != null && splitApkPaths != null && abis.length != 0) {
            String[] libraryNames = new String[abis.length];
            for (int i = 0; i < abis.length; i++) {
                libraryNames[i] = "lib/" + abis[i] + "/" + mPayloadBinaryName;
            }

            for (String path : splitApkPaths) {
                try (ZipFile zip = new ZipFile(path)) {
                    for (String name : libraryNames) {
                        if (zip.getEntry(name) != null) {
                            Log.i(TAG, "Found payload in " + path);
                            return path;
                        }
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to scan split APK: " + path, e);
                }
            }
        }

        // This really is the path to the APK, not a directory.
        return appInfo.sourceDir;
    }

    private int bytesToMebiBytes(long mMemoryBytes) {
        long oneMebi = 1024 * 1024;
        // We can't express requests for more than 2 exabytes, but then they're not going to succeed
        // anyway.
        if (mMemoryBytes > (Integer.MAX_VALUE - 1) * oneMebi) {
            return Integer.MAX_VALUE;
        }
        return (int) ((mMemoryBytes + oneMebi - 1) / oneMebi);
    }

    /**
     * A builder used to create a {@link VirtualMachineConfig}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        @Nullable private final String mPackageName;
        @Nullable private String mApkPath;
        @Nullable private String mPayloadConfigPath;
        @Nullable private String mPayloadBinaryName;
        @DebugLevel private int mDebugLevel = DEBUG_LEVEL_NONE;
        private boolean mProtectedVm;
        private boolean mProtectedVmSet;
        private long mMemoryBytes;
        @CpuTopology private int mCpuTopology = CPU_TOPOLOGY_ONE_CPU;
        private long mEncryptedStorageBytes;
        private boolean mVmOutputCaptured = false;

        /**
         * Creates a builder for the given context.
         *
         * @hide
         */
        @SystemApi
        public Builder(@NonNull Context context) {
            mPackageName = requireNonNull(context, "context must not be null").getPackageName();
        }

        /**
         * Creates a builder for a specific package. If packageName is null, {@link #setApkPath}
         * must be called to specify the APK containing the payload.
         */
        private Builder(@Nullable String packageName) {
            mPackageName = packageName;
        }

        /**
         * Builds an immutable {@link VirtualMachineConfig}
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public VirtualMachineConfig build() {
            String apkPath = null;
            String packageName = null;

            if (mApkPath != null) {
                apkPath = mApkPath;
            } else if (mPackageName != null) {
                packageName = mPackageName;
            } else {
                // This should never happen, unless we're deserializing a bad config
                throw new IllegalStateException("apkPath or packageName must be specified");
            }

            if (mPayloadBinaryName == null) {
                if (mPayloadConfigPath == null) {
                    throw new IllegalStateException("setPayloadBinaryName must be called");
                }
            } else {
                if (mPayloadConfigPath != null) {
                    throw new IllegalStateException(
                            "setPayloadBinaryName and setPayloadConfigPath may not both be called");
                }
            }

            if (!mProtectedVmSet) {
                throw new IllegalStateException("setProtectedVm must be called explicitly");
            }

            if (mVmOutputCaptured && mDebugLevel != DEBUG_LEVEL_FULL) {
                throw new IllegalStateException("debug level must be FULL to capture output");
            }

            return new VirtualMachineConfig(
                    packageName,
                    apkPath,
                    mPayloadConfigPath,
                    mPayloadBinaryName,
                    mDebugLevel,
                    mProtectedVm,
                    mMemoryBytes,
                    mCpuTopology,
                    mEncryptedStorageBytes,
                    mVmOutputCaptured);
        }

        /**
         * Sets the absolute path of the APK containing the binary payload that will execute within
         * the VM. If not set explicitly, defaults to the split APK containing the payload, if there
         * is one, and otherwise the primary APK of the context.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setApkPath(@NonNull String apkPath) {
            requireNonNull(apkPath, "apkPath must not be null");
            if (!apkPath.startsWith("/")) {
                throw new IllegalArgumentException("APK path must be an absolute path");
            }
            mApkPath = apkPath;
            return this;
        }

        /**
         * Sets the path within the APK to the payload config file that defines software aspects of
         * the VM. The file is a JSON file; see
         * packages/modules/Virtualization/microdroid/payload/config/src/lib.rs for the format.
         *
         * @hide
         */
        @RequiresPermission(VirtualMachine.USE_CUSTOM_VIRTUAL_MACHINE_PERMISSION)
        @TestApi
        @NonNull
        public Builder setPayloadConfigPath(@NonNull String payloadConfigPath) {
            mPayloadConfigPath =
                    requireNonNull(payloadConfigPath, "payloadConfigPath must not be null");
            return this;
        }

        /**
         * Sets the name of the payload binary file that will be executed within the VM, e.g.
         * "payload.so". The file must reside in the {@code lib/<ABI>} directory of the APK.
         *
         * <p>Note that VMs only support 64-bit code, even if the owning app is running as a 32-bit
         * process.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setPayloadBinaryName(@NonNull String payloadBinaryName) {
            if (payloadBinaryName.contains(File.separator)) {
                throw new IllegalArgumentException(
                        "Invalid binary file name: " + payloadBinaryName);
            }
            mPayloadBinaryName =
                    requireNonNull(payloadBinaryName, "payloadBinaryName must not be null");
            return this;
        }

        /**
         * Sets the debug level. Defaults to {@link #DEBUG_LEVEL_NONE}.
         *
         * <p>If {@link #DEBUG_LEVEL_FULL} is set then logs from inside the VM are exported to the
         * host and adb connections from the host are possible. This is convenient for debugging but
         * may compromise the integrity of the VM - including bypassing the protections offered by a
         * {@linkplain #setProtectedVm protected VM}.
         *
         * <p>Note that it isn't possible to {@linkplain #isCompatibleWith change} the debug level
         * of a VM instance; debug and non-debug VMs always have different secrets.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setDebugLevel(@DebugLevel int debugLevel) {
            if (debugLevel != DEBUG_LEVEL_NONE && debugLevel != DEBUG_LEVEL_FULL) {
                throw new IllegalArgumentException("Invalid debugLevel: " + debugLevel);
            }
            mDebugLevel = debugLevel;
            return this;
        }

        /**
         * Sets whether to protect the VM memory from the host. No default is provided, this must be
         * set explicitly.
         *
         * <p>Note that if debugging is {@linkplain #setDebugLevel enabled} for a protected VM, the
         * VM is not truly protected - direct memory access by the host is prevented, but e.g. the
         * debugger can be used to access the VM's internals.
         *
         * <p>It isn't possible to {@linkplain #isCompatibleWith change} the protected status of a
         * VM instance; protected and non-protected VMs always have different secrets.
         *
         * @see VirtualMachineManager#getCapabilities
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setProtectedVm(boolean protectedVm) {
            if (protectedVm) {
                if (!HypervisorProperties.hypervisor_protected_vm_supported().orElse(false)) {
                    throw new UnsupportedOperationException(
                            "Protected VMs are not supported on this device.");
                }
            } else {
                if (!HypervisorProperties.hypervisor_vm_supported().orElse(false)) {
                    throw new UnsupportedOperationException(
                            "Non-protected VMs are not supported on this device.");
                }
            }
            mProtectedVm = protectedVm;
            mProtectedVmSet = true;
            return this;
        }

        /**
         * Sets the amount of RAM to give the VM, in bytes. If not explicitly set then a default
         * size will be used.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setMemoryBytes(@IntRange(from = 1) long memoryBytes) {
            if (memoryBytes <= 0) {
                throw new IllegalArgumentException("Memory size must be positive");
            }
            mMemoryBytes = memoryBytes;
            return this;
        }

        /**
         * Sets the CPU topology configuration of the VM. Defaults to {@link #CPU_TOPOLOGY_ONE_CPU}.
         *
         * <p>This determines how many virtual CPUs will be created, and their performance and
         * scheduling characteristics, such as affinity masks. Topology also has an effect on memory
         * usage as each vCPU requires additional memory to keep its state.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setCpuTopology(@CpuTopology int cpuTopology) {
            if (cpuTopology != CPU_TOPOLOGY_ONE_CPU && cpuTopology != CPU_TOPOLOGY_MATCH_HOST) {
                throw new IllegalArgumentException("Invalid cpuTopology: " + cpuTopology);
            }
            mCpuTopology = cpuTopology;
            return this;
        }

        /**
         * Sets the size (in bytes) of encrypted storage available to the VM. If not set, no
         * encrypted storage is provided.
         *
         * <p>The storage is encrypted with a key deterministically derived from the VM identity
         *
         * <p>The encrypted storage is persistent across VM reboots as well as device reboots. The
         * backing file (containing encrypted data) is stored in the app's private data directory.
         *
         * <p>Note - There is no integrity guarantee or rollback protection on the storage in case
         * the encrypted data is modified.
         *
         * <p>Deleting the VM will delete the encrypted data - there is no way to recover that data.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setEncryptedStorageBytes(@IntRange(from = 1) long encryptedStorageBytes) {
            if (encryptedStorageBytes <= 0) {
                throw new IllegalArgumentException("Encrypted Storage size must be positive");
            }
            mEncryptedStorageBytes = encryptedStorageBytes;
            return this;
        }

        /**
         * Sets whether to allow the app to read the VM outputs (console / log). Default is {@code
         * false}.
         *
         * <p>By default, console and log outputs of a {@linkplain #setDebugLevel debuggable} VM are
         * automatically forwarded to the host logcat. Setting this as {@code true} will allow the
         * app to directly read {@linkplain VirtualMachine#getConsoleOutput console output} and
         * {@linkplain VirtualMachine#getLogOutput log output}, instead of forwarding them to the
         * host logcat.
         *
         * <p>If you turn on output capture, you must consume data from {@link
         * VirtualMachine#getConsoleOutput} and {@link VirtualMachine#getLogOutput} - because
         * otherwise the code in the VM may get blocked when the pipe buffer fills up.
         *
         * <p>The {@linkplain #setDebugLevel debug level} must be {@link #DEBUG_LEVEL_FULL} to be
         * set as true.
         *
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setVmOutputCaptured(boolean captured) {
            mVmOutputCaptured = captured;
            return this;
        }
    }
}
