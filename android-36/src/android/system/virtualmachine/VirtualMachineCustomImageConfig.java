/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.Nullable;
import android.os.PersistableBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** @hide */
public class VirtualMachineCustomImageConfig {
    private static final String KEY_NAME = "name";
    private static final String KEY_KERNEL = "kernel";
    private static final String KEY_INITRD = "initrd";
    private static final String KEY_BOOTLOADER = "bootloader";
    private static final String KEY_PARAMS = "params";
    private static final String KEY_DISK_WRITABLES = "disk_writables";
    private static final String KEY_DISK_IMAGES = "disk_images";
    private static final String KEY_PARTITION_LABELS = "partition_labels_";
    private static final String KEY_PARTITION_IMAGES = "partition_images_";
    private static final String KEY_PARTITION_WRITABLES = "partition_writables_";
    private static final String KEY_PARTITION_GUIDS = "partition_guids_";
    private static final String KEY_DISPLAY_CONFIG = "display_config";
    private static final String KEY_TOUCH = "touch";
    private static final String KEY_KEYBOARD = "keyboard";
    private static final String KEY_MOUSE = "mouse";
    private static final String KEY_SWITCHES = "switches";
    private static final String KEY_NETWORK = "network";
    private static final String KEY_GPU = "gpu";
    private static final String KEY_AUDIO_CONFIG = "audio_config";
    private static final String KEY_TRACKPAD = "trackpad";
    private static final String KEY_AUTO_MEMORY_BALLOON = "auto_memory_balloon";
    private static final String KEY_USB_CONFIG = "usb_config";

    @Nullable private final String name;
    @Nullable private final String kernelPath;
    @Nullable private final String initrdPath;
    @Nullable private final String bootloaderPath;
    @Nullable private final String[] params;
    @Nullable private final Disk[] disks;
    @Nullable private final SharedPath[] sharedPaths;
    @Nullable private final DisplayConfig displayConfig;
    @Nullable private final AudioConfig audioConfig;
    private final boolean touch;
    private final boolean keyboard;
    private final boolean mouse;
    private final boolean switches;
    private final boolean network;
    @Nullable private final GpuConfig gpuConfig;
    private final boolean trackpad;
    private final boolean autoMemoryBalloon;
    @Nullable private final UsbConfig usbConfig;

    @Nullable
    public Disk[] getDisks() {
        return disks;
    }

    @Nullable
    public String getBootloaderPath() {
        return bootloaderPath;
    }

    @Nullable
    public String getInitrdPath() {
        return initrdPath;
    }

    @Nullable
    public String getKernelPath() {
        return kernelPath;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String[] getParams() {
        return params;
    }

    @Nullable
    public SharedPath[] getSharedPaths() {
        return sharedPaths;
    }

    public boolean useTouch() {
        return touch;
    }

    public boolean useKeyboard() {
        return keyboard;
    }

    public boolean useMouse() {
        return mouse;
    }

    public boolean useSwitches() {
        return switches;
    }

    public boolean useTrackpad() {
        return mouse;
    }

    public boolean useAutoMemoryBalloon() {
        return autoMemoryBalloon;
    }

    public boolean useNetwork() {
        return network;
    }

    /** @hide */
    public VirtualMachineCustomImageConfig(
            String name,
            String kernelPath,
            String initrdPath,
            String bootloaderPath,
            String[] params,
            Disk[] disks,
            SharedPath[] sharedPaths,
            DisplayConfig displayConfig,
            boolean touch,
            boolean keyboard,
            boolean mouse,
            boolean switches,
            boolean network,
            GpuConfig gpuConfig,
            AudioConfig audioConfig,
            boolean trackpad,
            boolean autoMemoryBalloon,
            UsbConfig usbConfig) {
        this.name = name;
        this.kernelPath = kernelPath;
        this.initrdPath = initrdPath;
        this.bootloaderPath = bootloaderPath;
        this.params = params;
        this.disks = disks;
        this.sharedPaths = sharedPaths;
        this.displayConfig = displayConfig;
        this.touch = touch;
        this.keyboard = keyboard;
        this.mouse = mouse;
        this.switches = switches;
        this.network = network;
        this.gpuConfig = gpuConfig;
        this.audioConfig = audioConfig;
        this.trackpad = trackpad;
        this.autoMemoryBalloon = autoMemoryBalloon;
        this.usbConfig = usbConfig;
    }

    static VirtualMachineCustomImageConfig from(PersistableBundle customImageConfigBundle) {
        Builder builder = new Builder();
        builder.setName(customImageConfigBundle.getString(KEY_NAME));
        builder.setKernelPath(customImageConfigBundle.getString(KEY_KERNEL));
        builder.setInitrdPath(customImageConfigBundle.getString(KEY_INITRD));
        builder.setBootloaderPath(customImageConfigBundle.getString(KEY_BOOTLOADER));
        String[] params = customImageConfigBundle.getStringArray(KEY_PARAMS);
        if (params != null) {
            for (String param : params) {
                builder.addParam(param);
            }
        }
        boolean[] writables = customImageConfigBundle.getBooleanArray(KEY_DISK_WRITABLES);
        String[] diskImages = customImageConfigBundle.getStringArray(KEY_DISK_IMAGES);
        if (writables != null && diskImages != null) {
            if (writables.length == diskImages.length) {
                for (int i = 0; i < writables.length; i++) {
                    String diskImage = diskImages[i];
                    diskImage = diskImage.equals("") ? null : diskImage;
                    Disk disk = writables[i] ? Disk.RWDisk(diskImage) : Disk.RODisk(diskImage);
                    String[] labels =
                            customImageConfigBundle.getStringArray(KEY_PARTITION_LABELS + i);
                    String[] images =
                            customImageConfigBundle.getStringArray(KEY_PARTITION_IMAGES + i);
                    boolean[] partitionWritables =
                            customImageConfigBundle.getBooleanArray(KEY_PARTITION_WRITABLES + i);
                    String[] guids =
                            customImageConfigBundle.getStringArray(KEY_PARTITION_GUIDS + i);
                    for (int j = 0; j < labels.length; j++) {
                        disk.addPartition(
                                new Partition(
                                        labels[j], images[j], partitionWritables[j], guids[j]));
                    }
                    builder.addDisk(disk);
                }
            }
        }
        PersistableBundle displayConfigPb =
                customImageConfigBundle.getPersistableBundle(KEY_DISPLAY_CONFIG);
        builder.setDisplayConfig(DisplayConfig.from(displayConfigPb));
        builder.useTouch(customImageConfigBundle.getBoolean(KEY_TOUCH));
        builder.useKeyboard(customImageConfigBundle.getBoolean(KEY_KEYBOARD));
        builder.useMouse(customImageConfigBundle.getBoolean(KEY_MOUSE));
        builder.useNetwork(customImageConfigBundle.getBoolean(KEY_NETWORK));
        builder.setGpuConfig(GpuConfig.from(customImageConfigBundle.getPersistableBundle(KEY_GPU)));
        PersistableBundle audioConfigPb =
                customImageConfigBundle.getPersistableBundle(KEY_AUDIO_CONFIG);
        builder.setAudioConfig(AudioConfig.from(audioConfigPb));
        builder.useTrackpad(customImageConfigBundle.getBoolean(KEY_TRACKPAD));
        builder.useAutoMemoryBalloon(customImageConfigBundle.getBoolean(KEY_AUTO_MEMORY_BALLOON));
        PersistableBundle usbConfigPb =
                customImageConfigBundle.getPersistableBundle(KEY_USB_CONFIG);
        builder.setUsbConfig(UsbConfig.from(usbConfigPb));
        return builder.build();
    }

    PersistableBundle toPersistableBundle() {
        PersistableBundle pb = new PersistableBundle();
        pb.putString(KEY_NAME, this.name);
        pb.putString(KEY_KERNEL, this.kernelPath);
        pb.putString(KEY_BOOTLOADER, this.bootloaderPath);
        pb.putString(KEY_INITRD, this.initrdPath);
        pb.putStringArray(KEY_PARAMS, this.params);

        if (disks != null) {
            boolean[] writables = new boolean[disks.length];
            String[] images = new String[disks.length];
            for (int i = 0; i < disks.length; i++) {
                writables[i] = disks[i].writable;
                String imagePath = disks[i].imagePath;
                images[i] = imagePath == null ? "" : imagePath;

                int numPartitions = disks[i].getPartitions().size();
                String[] partitionLabels = new String[numPartitions];
                String[] partitionImages = new String[numPartitions];
                boolean[] partitionWritables = new boolean[numPartitions];
                String[] partitionGuids = new String[numPartitions];

                for (int j = 0; j < numPartitions; j++) {
                    Partition p = disks[i].getPartitions().get(j);
                    partitionLabels[j] = p.name;
                    partitionImages[j] = p.imagePath;
                    partitionWritables[j] = p.writable;
                    partitionGuids[j] = p.guid == null ? "" : p.guid;
                }
                pb.putStringArray(KEY_PARTITION_LABELS + i, partitionLabels);
                pb.putStringArray(KEY_PARTITION_IMAGES + i, partitionImages);
                pb.putBooleanArray(KEY_PARTITION_WRITABLES + i, partitionWritables);
                pb.putStringArray(KEY_PARTITION_GUIDS + i, partitionGuids);
            }
            pb.putBooleanArray(KEY_DISK_WRITABLES, writables);
            pb.putStringArray(KEY_DISK_IMAGES, images);
        }
        pb.putPersistableBundle(
                KEY_DISPLAY_CONFIG,
                Optional.ofNullable(displayConfig)
                        .map(dc -> dc.toPersistableBundle())
                        .orElse(null));
        pb.putBoolean(KEY_TOUCH, touch);
        pb.putBoolean(KEY_KEYBOARD, keyboard);
        pb.putBoolean(KEY_MOUSE, mouse);
        pb.putBoolean(KEY_SWITCHES, switches);
        pb.putBoolean(KEY_NETWORK, network);
        pb.putPersistableBundle(
                KEY_GPU,
                Optional.ofNullable(gpuConfig).map(gc -> gc.toPersistableBundle()).orElse(null));
        pb.putPersistableBundle(
                KEY_AUDIO_CONFIG,
                Optional.ofNullable(audioConfig).map(ac -> ac.toPersistableBundle()).orElse(null));
        pb.putBoolean(KEY_TRACKPAD, trackpad);
        pb.putBoolean(KEY_AUTO_MEMORY_BALLOON, autoMemoryBalloon);
        pb.putPersistableBundle(
                KEY_USB_CONFIG,
                Optional.ofNullable(usbConfig).map(uc -> uc.toPersistableBundle()).orElse(null));
        return pb;
    }

    @Nullable
    public AudioConfig getAudioConfig() {
        return audioConfig;
    }

    @Nullable
    public DisplayConfig getDisplayConfig() {
        return displayConfig;
    }

    @Nullable
    public GpuConfig getGpuConfig() {
        return gpuConfig;
    }

    @Nullable
    public UsbConfig getUsbConfig() {
        return usbConfig;
    }

    /** @hide */
    public static final class SharedPath {
        private final String path;
        private final int hostUid;
        private final int hostGid;
        private final int guestUid;
        private final int guestGid;
        private final int mask;
        private final String tag;
        private final String socket;
        private final boolean appDomain;
        private final String socketPath;

        public SharedPath(
                String path,
                int hostUid,
                int hostGid,
                int guestUid,
                int guestGid,
                int mask,
                String tag,
                String socket,
                boolean appDomain,
                String socketPath) {
            this.path = path;
            this.hostUid = hostUid;
            this.hostGid = hostGid;
            this.guestUid = guestUid;
            this.guestGid = guestGid;
            this.mask = mask;
            this.tag = tag;
            this.socket = socket;
            this.appDomain = appDomain;
            this.socketPath = socketPath;
        }

        android.system.virtualizationservice.SharedPath toParcelable() {
            android.system.virtualizationservice.SharedPath parcelable =
                    new android.system.virtualizationservice.SharedPath();
            parcelable.sharedPath = this.path;
            parcelable.hostUid = this.hostUid;
            parcelable.hostGid = this.hostGid;
            parcelable.guestUid = this.guestUid;
            parcelable.guestGid = this.guestGid;
            parcelable.mask = this.mask;
            parcelable.tag = this.tag;
            parcelable.socketPath = this.socket;
            parcelable.appDomain = this.appDomain;
            return parcelable;
        }

        /** @hide */
        public String getSharedPath() {
            return path;
        }

        /** @hide */
        public int getHostUid() {
            return hostUid;
        }

        /** @hide */
        public int getHostGid() {
            return hostGid;
        }

        /** @hide */
        public int getGuestUid() {
            return guestUid;
        }

        /** @hide */
        public int getGuestGid() {
            return guestGid;
        }

        /** @hide */
        public int getMask() {
            return mask;
        }

        /** @hide */
        public String getTag() {
            return tag;
        }

        /** @hide */
        public String getSocket() {
            return socket;
        }

        /** @hide */
        public boolean getAppDomain() {
            return appDomain;
        }

        /** @hide */
        public String getSocketPath() {
            return socketPath;
        }
    }

    /** @hide */
    public static final class Disk {
        private final boolean writable;
        private final String imagePath;
        private final List<Partition> partitions;

        private Disk(boolean writable, String imagePath) {
            this.writable = writable;
            this.imagePath = imagePath;
            this.partitions = new ArrayList<>();
        }

        /** @hide */
        public static Disk RWDisk(String imagePath) {
            return new Disk(true, imagePath);
        }

        /** @hide */
        public static Disk RODisk(String imagePath) {
            return new Disk(false, imagePath);
        }

        /** @hide */
        public boolean isWritable() {
            return writable;
        }

        /** @hide */
        public String getImagePath() {
            return imagePath;
        }

        /** @hide */
        public Disk addPartition(Partition p) {
            this.partitions.add(p);
            return this;
        }

        /** @hide */
        public List<Partition> getPartitions() {
            return partitions;
        }
    }

    /** @hide */
    public static final class Partition {
        public final String name;
        public final String imagePath;
        public final boolean writable;
        public final String guid;

        public Partition(String name, String imagePath, boolean writable, String guid) {
            this.name = name;
            this.imagePath = imagePath;
            this.writable = writable;
            this.guid = guid;
        }
    }

    /** @hide */
    public static final class Builder {
        private String name;
        private String kernelPath;
        private String initrdPath;
        private String bootloaderPath;
        private List<String> params = new ArrayList<>();
        private List<Disk> disks = new ArrayList<>();
        private List<SharedPath> sharedPaths = new ArrayList<>();
        private AudioConfig audioConfig;
        private DisplayConfig displayConfig;
        private boolean touch;
        private boolean keyboard;
        private boolean mouse;
        private boolean switches;
        private boolean network;
        private GpuConfig gpuConfig;
        private boolean trackpad;
        private boolean autoMemoryBalloon = false;
        private UsbConfig usbConfig;

        /** @hide */
        public Builder() {}

        /** @hide */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /** @hide */
        public Builder setKernelPath(String kernelPath) {
            this.kernelPath = kernelPath;
            return this;
        }

        /** @hide */
        public Builder setBootloaderPath(String bootloaderPath) {
            this.bootloaderPath = bootloaderPath;
            return this;
        }

        /** @hide */
        public Builder setInitrdPath(String initrdPath) {
            this.initrdPath = initrdPath;
            return this;
        }

        /** @hide */
        public Builder addDisk(Disk disk) {
            this.disks.add(disk);
            return this;
        }

        /** @hide */
        public Builder addSharedPath(SharedPath path) {
            this.sharedPaths.add(path);
            return this;
        }

        /** @hide */
        public Builder addParam(String param) {
            this.params.add(param);
            return this;
        }

        /** @hide */
        public Builder setDisplayConfig(DisplayConfig displayConfig) {
            this.displayConfig = displayConfig;
            return this;
        }

        /** @hide */
        public Builder setGpuConfig(GpuConfig gpuConfig) {
            this.gpuConfig = gpuConfig;
            return this;
        }

        /** @hide */
        public Builder useTouch(boolean touch) {
            this.touch = touch;
            return this;
        }

        /** @hide */
        public Builder useKeyboard(boolean keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        /** @hide */
        public Builder useMouse(boolean mouse) {
            this.mouse = mouse;
            return this;
        }

        /** @hide */
        public Builder useSwitches(boolean switches) {
            this.switches = switches;
            return this;
        }

        /** @hide */
        public Builder useTrackpad(boolean trackpad) {
            this.trackpad = trackpad;
            return this;
        }

        /** @hide */
        public Builder useAutoMemoryBalloon(boolean autoMemoryBalloon) {
            this.autoMemoryBalloon = autoMemoryBalloon;
            return this;
        }

        /** @hide */
        public Builder useNetwork(boolean network) {
            this.network = network;
            return this;
        }

        /** @hide */
        public Builder setAudioConfig(AudioConfig audioConfig) {
            this.audioConfig = audioConfig;
            return this;
        }

        /** @hide */
        public Builder setUsbConfig(UsbConfig usbConfig) {
            this.usbConfig = usbConfig;
            return this;
        }

        /** @hide */
        public VirtualMachineCustomImageConfig build() {
            return new VirtualMachineCustomImageConfig(
                    this.name,
                    this.kernelPath,
                    this.initrdPath,
                    this.bootloaderPath,
                    this.params.toArray(new String[0]),
                    this.disks.toArray(new Disk[0]),
                    this.sharedPaths.toArray(new SharedPath[0]),
                    displayConfig,
                    touch,
                    keyboard,
                    mouse,
                    switches,
                    network,
                    gpuConfig,
                    audioConfig,
                    trackpad,
                    autoMemoryBalloon,
                    usbConfig);
        }
    }

    /** @hide */
    public static final class UsbConfig {
        private static final String KEY_USE_CONTROLLER = "use_controller";
        public final boolean controller;

        public UsbConfig(boolean controller) {
            this.controller = controller;
        }

        public boolean getUsbController() {
            return this.controller;
        }

        android.system.virtualizationservice.UsbConfig toParceclable() {
            android.system.virtualizationservice.UsbConfig parcelable =
                    new android.system.virtualizationservice.UsbConfig();
            parcelable.controller = this.controller;
            return parcelable;
        }

        private static UsbConfig from(PersistableBundle pb) {
            if (pb == null) {
                return null;
            }
            Builder builder = new Builder();
            builder.setController(pb.getBoolean(KEY_USE_CONTROLLER));
            return builder.build();
        }

        private PersistableBundle toPersistableBundle() {
            PersistableBundle pb = new PersistableBundle();
            pb.putBoolean(KEY_USE_CONTROLLER, this.controller);
            return pb;
        }

        /** @hide */
        public static class Builder {
            private boolean useController = false;

            /** @hide */
            public Builder() {}

            /** @hide */
            public Builder setController(boolean useController) {
                this.useController = useController;
                return this;
            }

            /** @hide */
            public UsbConfig build() {
                return new UsbConfig(useController);
            }
        }
    }

    /** @hide */
    public static final class AudioConfig {
        private static final String KEY_USE_MICROPHONE = "use_microphone";
        private static final String KEY_USE_SPEAKER = "use_speaker";
        private final boolean useMicrophone;
        private final boolean useSpeaker;

        private AudioConfig(boolean useMicrophone, boolean useSpeaker) {
            this.useMicrophone = useMicrophone;
            this.useSpeaker = useSpeaker;
        }

        /** @hide */
        public boolean useMicrophone() {
            return useMicrophone;
        }

        /** @hide */
        public boolean useSpeaker() {
            return useSpeaker;
        }

        android.system.virtualizationservice.AudioConfig toParcelable() {
            android.system.virtualizationservice.AudioConfig parcelable =
                    new android.system.virtualizationservice.AudioConfig();
            parcelable.useSpeaker = this.useSpeaker;
            parcelable.useMicrophone = this.useMicrophone;

            return parcelable;
        }

        private static AudioConfig from(PersistableBundle pb) {
            if (pb == null) {
                return null;
            }
            Builder builder = new Builder();
            builder.setUseMicrophone(pb.getBoolean(KEY_USE_MICROPHONE));
            builder.setUseSpeaker(pb.getBoolean(KEY_USE_SPEAKER));
            return builder.build();
        }

        private PersistableBundle toPersistableBundle() {
            PersistableBundle pb = new PersistableBundle();
            pb.putBoolean(KEY_USE_MICROPHONE, this.useMicrophone);
            pb.putBoolean(KEY_USE_SPEAKER, this.useSpeaker);
            return pb;
        }

        /** @hide */
        public static class Builder {
            private boolean useMicrophone = false;
            private boolean useSpeaker = false;

            /** @hide */
            public Builder() {}

            /** @hide */
            public Builder setUseMicrophone(boolean useMicrophone) {
                this.useMicrophone = useMicrophone;
                return this;
            }

            /** @hide */
            public Builder setUseSpeaker(boolean useSpeaker) {
                this.useSpeaker = useSpeaker;
                return this;
            }

            /** @hide */
            public AudioConfig build() {
                return new AudioConfig(useMicrophone, useSpeaker);
            }
        }
    }

    /** @hide */
    public static final class DisplayConfig {
        private static final String KEY_WIDTH = "width";
        private static final String KEY_HEIGHT = "height";
        private static final String KEY_HORIZONTAL_DPI = "horizontal_dpi";
        private static final String KEY_VERTICAL_DPI = "vertical_dpi";
        private static final String KEY_REFRESH_RATE = "refresh_rate";
        private final int width;
        private final int height;
        private final int horizontalDpi;
        private final int verticalDpi;
        private final int refreshRate;

        private DisplayConfig(
                int width, int height, int horizontalDpi, int verticalDpi, int refreshRate) {
            this.width = width;
            this.height = height;
            this.horizontalDpi = horizontalDpi;
            this.verticalDpi = verticalDpi;
            this.refreshRate = refreshRate;
        }

        /** @hide */
        public int getWidth() {
            return width;
        }

        /** @hide */
        public int getHeight() {
            return height;
        }

        /** @hide */
        public int getHorizontalDpi() {
            return horizontalDpi;
        }

        /** @hide */
        public int getVerticalDpi() {
            return verticalDpi;
        }

        /** @hide */
        public int getRefreshRate() {
            return refreshRate;
        }

        android.system.virtualizationservice.DisplayConfig toParcelable() {
            android.system.virtualizationservice.DisplayConfig parcelable =
                    new android.system.virtualizationservice.DisplayConfig();
            parcelable.width = this.width;
            parcelable.height = this.height;
            parcelable.horizontalDpi = this.horizontalDpi;
            parcelable.verticalDpi = this.verticalDpi;
            parcelable.refreshRate = this.refreshRate;

            return parcelable;
        }

        private static DisplayConfig from(PersistableBundle pb) {
            if (pb == null) {
                return null;
            }
            Builder builder = new Builder();
            builder.setWidth(pb.getInt(KEY_WIDTH));
            builder.setHeight(pb.getInt(KEY_HEIGHT));
            builder.setHorizontalDpi(pb.getInt(KEY_HORIZONTAL_DPI));
            builder.setVerticalDpi(pb.getInt(KEY_VERTICAL_DPI));
            builder.setRefreshRate(pb.getInt(KEY_REFRESH_RATE));
            return builder.build();
        }

        private PersistableBundle toPersistableBundle() {
            PersistableBundle pb = new PersistableBundle();
            pb.putInt(KEY_WIDTH, this.width);
            pb.putInt(KEY_HEIGHT, this.height);
            pb.putInt(KEY_HORIZONTAL_DPI, this.horizontalDpi);
            pb.putInt(KEY_VERTICAL_DPI, this.verticalDpi);
            pb.putInt(KEY_REFRESH_RATE, this.refreshRate);
            return pb;
        }

        /** @hide */
        public static class Builder {
            // Default values come from external/crosvm/vm_control/src/gpu.rs
            private int width;
            private int height;
            private int horizontalDpi = 320;
            private int verticalDpi = 320;
            private int refreshRate = 60;

            /** @hide */
            public Builder() {}

            /** @hide */
            public Builder setWidth(int width) {
                this.width = width;
                return this;
            }

            /** @hide */
            public Builder setHeight(int height) {
                this.height = height;
                return this;
            }

            /** @hide */
            public Builder setHorizontalDpi(int horizontalDpi) {
                this.horizontalDpi = horizontalDpi;
                return this;
            }

            /** @hide */
            public Builder setVerticalDpi(int verticalDpi) {
                this.verticalDpi = verticalDpi;
                return this;
            }

            /** @hide */
            public Builder setRefreshRate(int refreshRate) {
                this.refreshRate = refreshRate;
                return this;
            }

            /** @hide */
            public DisplayConfig build() {
                if (this.width == 0 || this.height == 0) {
                    throw new IllegalStateException("width and height must be specified");
                }
                return new DisplayConfig(width, height, horizontalDpi, verticalDpi, refreshRate);
            }
        }
    }

    /** @hide */
    public static final class GpuConfig {
        private static final String KEY_BACKEND = "backend";
        private static final String KEY_CONTEXT_TYPES = "context_types";
        private static final String KEY_PCI_ADDRESS = "pci_address";
        private static final String KEY_RENDERER_FEATURES = "renderer_features";
        private static final String KEY_RENDERER_USE_EGL = "renderer_use_egl";
        private static final String KEY_RENDERER_USE_GLES = "renderer_use_gles";
        private static final String KEY_RENDERER_USE_GLX = "renderer_use_glx";
        private static final String KEY_RENDERER_USE_SURFACELESS = "renderer_use_surfaceless";
        private static final String KEY_RENDERER_USE_VULKAN = "renderer_use_vulkan";

        private final String backend;
        private final String[] contextTypes;
        private final String pciAddress;
        private final String rendererFeatures;
        private final boolean rendererUseEgl;
        private final boolean rendererUseGles;
        private final boolean rendererUseGlx;
        private final boolean rendererUseSurfaceless;
        private final boolean rendererUseVulkan;

        private GpuConfig(
                String backend,
                String[] contextTypes,
                String pciAddress,
                String rendererFeatures,
                boolean rendererUseEgl,
                boolean rendererUseGles,
                boolean rendererUseGlx,
                boolean rendererUseSurfaceless,
                boolean rendererUseVulkan) {
            this.backend = backend;
            this.contextTypes = contextTypes;
            this.pciAddress = pciAddress;
            this.rendererFeatures = rendererFeatures;
            this.rendererUseEgl = rendererUseEgl;
            this.rendererUseGles = rendererUseGles;
            this.rendererUseGlx = rendererUseGlx;
            this.rendererUseSurfaceless = rendererUseSurfaceless;
            this.rendererUseVulkan = rendererUseVulkan;
        }

        /** @hide */
        public String getBackend() {
            return backend;
        }

        /** @hide */
        public String[] getContextTypes() {
            return contextTypes;
        }

        /** @hide */
        public String getPciAddress() {
            return pciAddress;
        }

        /** @hide */
        public String getRendererFeatures() {
            return rendererFeatures;
        }

        /** @hide */
        public boolean getRendererUseEgl() {
            return rendererUseEgl;
        }

        /** @hide */
        public boolean getRendererUseGles() {
            return rendererUseGles;
        }

        /** @hide */
        public boolean getRendererUseGlx() {
            return rendererUseGlx;
        }

        /** @hide */
        public boolean getRendererUseSurfaceless() {
            return rendererUseSurfaceless;
        }

        /** @hide */
        public boolean getRendererUseVulkan() {
            return rendererUseVulkan;
        }

        android.system.virtualizationservice.GpuConfig toParcelable() {
            android.system.virtualizationservice.GpuConfig parcelable =
                    new android.system.virtualizationservice.GpuConfig();
            parcelable.backend = this.backend;
            parcelable.contextTypes = this.contextTypes;
            parcelable.pciAddress = this.pciAddress;
            parcelable.rendererFeatures = this.rendererFeatures;
            parcelable.rendererUseEgl = this.rendererUseEgl;
            parcelable.rendererUseGles = this.rendererUseGles;
            parcelable.rendererUseGlx = this.rendererUseGlx;
            parcelable.rendererUseSurfaceless = this.rendererUseSurfaceless;
            parcelable.rendererUseVulkan = this.rendererUseVulkan;
            return parcelable;
        }

        private static GpuConfig from(PersistableBundle pb) {
            if (pb == null) {
                return null;
            }
            Builder builder = new Builder();
            builder.setBackend(pb.getString(KEY_BACKEND));
            builder.setContextTypes(pb.getStringArray(KEY_CONTEXT_TYPES));
            builder.setPciAddress(pb.getString(KEY_PCI_ADDRESS));
            builder.setRendererFeatures(pb.getString(KEY_RENDERER_FEATURES));
            builder.setRendererUseEgl(pb.getBoolean(KEY_RENDERER_USE_EGL));
            builder.setRendererUseGles(pb.getBoolean(KEY_RENDERER_USE_GLES));
            builder.setRendererUseGlx(pb.getBoolean(KEY_RENDERER_USE_GLX));
            builder.setRendererUseSurfaceless(pb.getBoolean(KEY_RENDERER_USE_SURFACELESS));
            builder.setRendererUseVulkan(pb.getBoolean(KEY_RENDERER_USE_VULKAN));
            return builder.build();
        }

        private PersistableBundle toPersistableBundle() {
            PersistableBundle pb = new PersistableBundle();
            pb.putString(KEY_BACKEND, this.backend);
            pb.putStringArray(KEY_CONTEXT_TYPES, this.contextTypes);
            pb.putString(KEY_PCI_ADDRESS, this.pciAddress);
            pb.putString(KEY_RENDERER_FEATURES, this.rendererFeatures);
            pb.putBoolean(KEY_RENDERER_USE_EGL, this.rendererUseEgl);
            pb.putBoolean(KEY_RENDERER_USE_GLES, this.rendererUseGles);
            pb.putBoolean(KEY_RENDERER_USE_GLX, this.rendererUseGlx);
            pb.putBoolean(KEY_RENDERER_USE_SURFACELESS, this.rendererUseSurfaceless);
            pb.putBoolean(KEY_RENDERER_USE_VULKAN, this.rendererUseVulkan);
            return pb;
        }

        /** @hide */
        public static class Builder {
            private String backend;
            private String[] contextTypes;
            private String pciAddress;
            private String rendererFeatures;
            private boolean rendererUseEgl = true;
            private boolean rendererUseGles = true;
            private boolean rendererUseGlx = false;
            private boolean rendererUseSurfaceless = true;
            private boolean rendererUseVulkan = false;

            /** @hide */
            public Builder() {}

            /** @hide */
            public Builder setBackend(String backend) {
                this.backend = backend;
                return this;
            }

            /** @hide */
            public Builder setContextTypes(String[] contextTypes) {
                this.contextTypes = contextTypes;
                return this;
            }

            /** @hide */
            public Builder setPciAddress(String pciAddress) {
                this.pciAddress = pciAddress;
                return this;
            }

            /** @hide */
            public Builder setRendererFeatures(String rendererFeatures) {
                this.rendererFeatures = rendererFeatures;
                return this;
            }

            /** @hide */
            public Builder setRendererUseEgl(Boolean rendererUseEgl) {
                this.rendererUseEgl = rendererUseEgl;
                return this;
            }

            /** @hide */
            public Builder setRendererUseGles(Boolean rendererUseGles) {
                this.rendererUseGles = rendererUseGles;
                return this;
            }

            /** @hide */
            public Builder setRendererUseGlx(Boolean rendererUseGlx) {
                this.rendererUseGlx = rendererUseGlx;
                return this;
            }

            /** @hide */
            public Builder setRendererUseSurfaceless(Boolean rendererUseSurfaceless) {
                this.rendererUseSurfaceless = rendererUseSurfaceless;
                return this;
            }

            /** @hide */
            public Builder setRendererUseVulkan(Boolean rendererUseVulkan) {
                this.rendererUseVulkan = rendererUseVulkan;
                return this;
            }

            /** @hide */
            public GpuConfig build() {
                return new GpuConfig(
                        backend,
                        contextTypes,
                        pciAddress,
                        rendererFeatures,
                        rendererUseEgl,
                        rendererUseGles,
                        rendererUseGlx,
                        rendererUseSurfaceless,
                        rendererUseVulkan);
            }
        }
    }
}
