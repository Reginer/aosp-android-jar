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
    private static final String KEY_DISPLAY_CONFIG = "display_config";
    private static final String KEY_TOUCH = "touch";
    private static final String KEY_KEYBOARD = "keyboard";

    @Nullable private final String name;
    @Nullable private final String kernelPath;
    @Nullable private final String initrdPath;
    @Nullable private final String bootloaderPath;
    @Nullable private final String[] params;
    @Nullable private final Disk[] disks;
    @Nullable private final DisplayConfig displayConfig;
    private final boolean touch;
    private final boolean keyboard;

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

    public boolean useTouch() {
        return touch;
    }

    public boolean useKeyboard() {
        return keyboard;
    }

    /** @hide */
    public VirtualMachineCustomImageConfig(
            String name,
            String kernelPath,
            String initrdPath,
            String bootloaderPath,
            String[] params,
            Disk[] disks,
            DisplayConfig displayConfig,
            boolean touch,
            boolean keyboard) {
        this.name = name;
        this.kernelPath = kernelPath;
        this.initrdPath = initrdPath;
        this.bootloaderPath = bootloaderPath;
        this.params = params;
        this.disks = disks;
        this.displayConfig = displayConfig;
        this.touch = touch;
        this.keyboard = keyboard;
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
                    builder.addDisk(
                            writables[i] ? Disk.RWDisk(diskImages[i]) : Disk.RODisk(diskImages[i]));
                }
            }
        }
        PersistableBundle displayConfigPb =
                customImageConfigBundle.getPersistableBundle(KEY_DISPLAY_CONFIG);
        builder.setDisplayConfig(DisplayConfig.from(displayConfigPb));
        builder.useTouch(customImageConfigBundle.getBoolean(KEY_TOUCH));
        builder.useKeyboard(customImageConfigBundle.getBoolean(KEY_KEYBOARD));
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
                images[i] = disks[i].imagePath;
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
        return pb;
    }

    @Nullable
    public DisplayConfig getDisplayConfig() {
        return displayConfig;
    }

    /** @hide */
    public static final class Disk {
        private final boolean writable;
        private final String imagePath;

        private Disk(boolean writable, String imagePath) {
            this.writable = writable;
            this.imagePath = imagePath;
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
    }

    /** @hide */
    public static final class Builder {
        private String name;
        private String kernelPath;
        private String initrdPath;
        private String bootloaderPath;
        private List<String> params = new ArrayList<>();
        private List<Disk> disks = new ArrayList<>();
        private DisplayConfig displayConfig;
        private boolean touch;
        private boolean keyboard;

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
        public VirtualMachineCustomImageConfig build() {
            return new VirtualMachineCustomImageConfig(
                    this.name,
                    this.kernelPath,
                    this.initrdPath,
                    this.bootloaderPath,
                    this.params.toArray(new String[0]),
                    this.disks.toArray(new Disk[0]),
                    displayConfig,
                    touch,
                    keyboard);
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
}
