/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.hardware.usb;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * A class representing a USB accessory, which is an external hardware component
 * that communicates with an android application over USB.
 * The accessory is the USB host and android the device side of the USB connection.
 *
 * <p>When the accessory connects, it reports its manufacturer and model names,
 * the version of the accessory, and a user visible description of the accessory to the device.
 * The manufacturer, model and version strings are used by the USB Manager to choose
 * an appropriate application for the accessory.
 * The accessory may optionally provide a unique serial number
 * and a URL to for the accessory's website to the device as well.
 *
 * <p>An instance of this class is sent to the application via the
 * {@link UsbManager#ACTION_USB_ACCESSORY_ATTACHED} Intent.
 * The application can then call {@link UsbManager#openAccessory} to open a file descriptor
 * for reading and writing data to and from the accessory.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about communicating with USB hardware, read the
 * <a href="{@docRoot}guide/topics/usb/index.html">USB</a> developer guide.</p>
 * </div>
 */
public class UsbAccessory implements Parcelable {

    private static final String TAG = "UsbAccessory";

    private final @NonNull String mManufacturer;
    private final @NonNull String mModel;
    private final @Nullable String mDescription;
    private final @Nullable String mVersion;
    private final @Nullable String mUri;
    private final @NonNull IUsbSerialReader mSerialNumberReader;

    /** @hide */
    public static final int MANUFACTURER_STRING = 0;
    /** @hide */
    public static final int MODEL_STRING = 1;
    /** @hide */
    public static final int DESCRIPTION_STRING = 2;
    /** @hide */
    public static final int VERSION_STRING = 3;
    /** @hide */
    public static final int URI_STRING = 4;
    /** @hide */
    public static final int SERIAL_STRING = 5;

    /**
     * UsbAccessory should only be instantiated by UsbService implementation
     * @hide
     */
    public UsbAccessory(@NonNull String manufacturer, @NonNull String model,
            @Nullable String description, @Nullable String version, @Nullable String uri,
            @NonNull IUsbSerialReader serialNumberReader) {
        mManufacturer = Objects.requireNonNull(manufacturer);
        mModel = Objects.requireNonNull(model);
        mDescription = description;
        mVersion = version;
        mUri = uri;
        mSerialNumberReader = serialNumberReader;

        // Make sure the binder belongs to the system
        if (ActivityThread.isSystem()) {
            Preconditions.checkArgument(mSerialNumberReader instanceof IUsbSerialReader.Stub);
        }
    }

    /**
     * DO NOT USE. Only for backwards compatibility with
     * {@link com.android.future.usb.UsbAccessory}.
     *
     * @hide
     * @deprecated use {@link UsbAccessory#UsbAccessory(String, String, String, String, String,
     *             IUsbSerialReader) instead}
     */
    @Deprecated
    public UsbAccessory(@NonNull String manufacturer, @NonNull String model,
            @Nullable String description, @Nullable String version, @Nullable String uri,
            @Nullable String serialNumber) {
        this(manufacturer, model, description, version, uri, new IUsbSerialReader.Stub() {
            @Override
            public String getSerial(String packageName) {
                return serialNumber;
            }
        });
    }

    /**
     * Returns the manufacturer name of the accessory.
     *
     * @return the accessory manufacturer
     */
    public @NonNull String getManufacturer() {
        return mManufacturer;
    }

    /**
     * Returns the model name of the accessory.
     *
     * @return the accessory model
     */
    public @NonNull String getModel() {
        return mModel;
    }

    /**
     * Returns a user visible description of the accessory.
     *
     * @return the accessory description, or {@code null} if not set
     */
    public @Nullable String getDescription() {
        return mDescription;
    }

    /**
     * Returns the version of the accessory.
     *
     * @return the accessory version, or {@code null} if not set
     */
    public @Nullable String getVersion() {
        return mVersion;
    }

    /**
     * Returns the URI for the accessory.
     * This is an optional URI that might show information about the accessory
     * or provide the option to download an application for the accessory
     *
     * @return the accessory URI, or {@code null} if not set
     */
    public @Nullable String getUri() {
        return mUri;
    }

    /**
     * Returns the unique serial number for the accessory.
     * This is an optional serial number that can be used to differentiate
     * between individual accessories of the same model and manufacturer
     *
     * @return the unique serial number, or {@code null} if not set
     *
     * @throws SecurityException if the app targets SDK >= {@value android.os.Build.VERSION_CODES#Q}
     *                           and the app does not have permission to read from the accessory.
     */
    public @Nullable String getSerial() {
        try {
            return mSerialNumberReader.getSerial(ActivityThread.currentPackageName());
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    private static boolean compare(String s1, String s2) {
        if (s1 == null) return (s2 == null);
        return s1.equals(s2);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof UsbAccessory) {
            UsbAccessory accessory = (UsbAccessory)obj;
            return (compare(mManufacturer, accessory.getManufacturer()) &&
                    compare(mModel, accessory.getModel()) &&
                    compare(mDescription, accessory.getDescription()) &&
                    compare(mVersion, accessory.getVersion()) &&
                    compare(mUri, accessory.getUri()) &&
                    compare(getSerial(), accessory.getSerial()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mManufacturer.hashCode() ^ mModel.hashCode() ^
                (mDescription == null ? 0 : mDescription.hashCode()) ^
                (mVersion == null ? 0 : mVersion.hashCode()) ^
                (mUri == null ? 0 : mUri.hashCode());
    }

    @Override
    public String toString() {
        return "UsbAccessory[mManufacturer=" + mManufacturer +
                            ", mModel=" + mModel +
                            ", mDescription=" + mDescription +
                            ", mVersion=" + mVersion +
                            ", mUri=" + mUri +
                            ", mSerialNumberReader=" + mSerialNumberReader + "]";
    }

    public static final @android.annotation.NonNull Parcelable.Creator<UsbAccessory> CREATOR =
        new Parcelable.Creator<UsbAccessory>() {
        public UsbAccessory createFromParcel(Parcel in) {
            String manufacturer = in.readString();
            String model = in.readString();
            String description = in.readString();
            String version = in.readString();
            String uri = in.readString();
            IUsbSerialReader serialNumberReader = IUsbSerialReader.Stub.asInterface(
                    in.readStrongBinder());

            return new UsbAccessory(manufacturer, model, description, version, uri,
                    serialNumberReader);
        }

        public UsbAccessory[] newArray(int size) {
            return new UsbAccessory[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mManufacturer);
        parcel.writeString(mModel);
        parcel.writeString(mDescription);
        parcel.writeString(mVersion);
        parcel.writeString(mUri);
        parcel.writeStrongBinder(mSerialNumberReader.asBinder());
   }
}
