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

package android.os.flagging;

import static android.provider.flags.Flags.FLAG_NEW_STORAGE_PUBLIC_API;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Exception thrown when an error occurs while reading from Aconfig Storage.
 *
 * <p>This exception indicates a problem accessing or retrieving configuration data from Aconfig
 * Storage. This could be due to various reasons, such as:
 *
 * <ul>
 *   <li>The Aconfig Storage system is not found on the device.
 *   <li>The requested configuration package is not found.
 *   <li>The specified container is not found.
 *   <li>There was an error reading the Aconfig Storage file.
 *   <li>The fingerprint of the Aconfig Storage file does not match the expected fingerprint.
 * </ul>
 */
@FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
public class AconfigStorageReadException extends RuntimeException {

    /** Generic error code indicating an unspecified Aconfig Storage error. */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public static final int ERROR_GENERIC = 0;

    /** Error code indicating that the Aconfig Storage system is not found on the device. */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public static final int ERROR_STORAGE_SYSTEM_NOT_FOUND = 1;

    /** Error code indicating that the requested configuration package is not found. */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public static final int ERROR_PACKAGE_NOT_FOUND = 2;

    /** Error code indicating that the specified container is not found. */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public static final int ERROR_CONTAINER_NOT_FOUND = 3;

    /** Error code indicating that there was an error reading the Aconfig Storage file. */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public static final int ERROR_CANNOT_READ_STORAGE_FILE = 4;

    /**
     * Error code indicating that the fingerprint of the Aconfig Storage file does not match the
     * expected fingerprint.
     *
     * <p><b>This constant is not part of the public API and should be used by Acnofig Flag
     * internally </b> It is intended for internal use only and may be changed or removed without
     * notice.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static final int ERROR_FILE_FINGERPRINT_MISMATCH = 5;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"ERROR_"},
            value = {
                ERROR_GENERIC,
                ERROR_STORAGE_SYSTEM_NOT_FOUND,
                ERROR_PACKAGE_NOT_FOUND,
                ERROR_CONTAINER_NOT_FOUND,
                ERROR_CANNOT_READ_STORAGE_FILE,
                ERROR_FILE_FINGERPRINT_MISMATCH
            })
    public @interface ErrorCode {}

    @ErrorCode private final int mErrorCode;

    /**
     * Constructs a new {@code AconfigStorageReadException} with the specified error code and detail
     * message.
     *
     * @param errorCode The error code for this exception.
     * @param msg The detail message for this exception.
     */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public AconfigStorageReadException(@ErrorCode int errorCode, @NonNull String msg) {
        super(msg);
        mErrorCode = errorCode;
    }

    /**
     * Constructs a new {@code AconfigStorageReadException} with the specified error code, detail
     * message, and cause.
     *
     * @param errorCode The error code for this exception.
     * @param msg The detail message for this exception.
     * @param cause The cause of this exception.
     */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public AconfigStorageReadException(
            @ErrorCode int errorCode, @NonNull String msg, @NonNull Throwable cause) {
        super(msg, cause);
        mErrorCode = errorCode;
    }

    /**
     * Constructs a new {@code AconfigStorageReadException} with the specified error code and cause.
     *
     * @param errorCode The error code for this exception.
     * @param cause The cause of this exception.
     */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public AconfigStorageReadException(@ErrorCode int errorCode, @NonNull Throwable cause) {
        super(cause);
        mErrorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return The error code.
     */
    @ErrorCode
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * Returns the error message for this exception, including the error code and the original
     * message.
     *
     * @return The error message.
     */
    @Override
    @NonNull
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public String getMessage() {
        return errorString() + ": " + super.getMessage();
    }

    /**
     * Returns a string representation of the error code.
     *
     * @return The error code string.
     */
    @NonNull
    private String errorString() {
        switch (mErrorCode) {
            case ERROR_GENERIC:
                return "ERROR_GENERIC";
            case ERROR_STORAGE_SYSTEM_NOT_FOUND:
                return "ERROR_STORAGE_SYSTEM_NOT_FOUND";
            case ERROR_PACKAGE_NOT_FOUND:
                return "ERROR_PACKAGE_NOT_FOUND";
            case ERROR_CONTAINER_NOT_FOUND:
                return "ERROR_CONTAINER_NOT_FOUND";
            case ERROR_CANNOT_READ_STORAGE_FILE:
                return "ERROR_CANNOT_READ_STORAGE_FILE";
            case ERROR_FILE_FINGERPRINT_MISMATCH:
                return "ERROR_FILE_FINGERPRINT_MISMATCH";
            default:
                return "<Unknown error code " + mErrorCode + ">";
        }
    }
}
