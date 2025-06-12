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

package android.aconfig.storage;

/**
 * Exception thrown when an error occurs while accessing Aconfig Storage.
 *
 * <p>This exception indicates a general problem with Aconfig Storage, such as an inability to read
 * or write data.
 */
public class AconfigStorageException extends RuntimeException {

    /** Generic error code indicating an unspecified Aconfig Storage error. */
    public static final int ERROR_GENERIC = 0;

    /** Error code indicating that the Aconfig Storage system is not found on the device. */
    public static final int ERROR_STORAGE_SYSTEM_NOT_FOUND = 1;

    /** Error code indicating that the requested configuration package is not found. */
    public static final int ERROR_PACKAGE_NOT_FOUND = 2;

    /** Error code indicating that the specified container is not found. */
    public static final int ERROR_CONTAINER_NOT_FOUND = 3;

    /** Error code indicating that there was an error reading the Aconfig Storage file. */
    public static final int ERROR_CANNOT_READ_STORAGE_FILE = 4;

    public static final int ERROR_FILE_FINGERPRINT_MISMATCH = 5;

    private final int mErrorCode;

    /**
     * Constructs a new {@code AconfigStorageException} with a generic error code and the specified
     * detail message.
     *
     * @param msg The detail message for this exception.
     */
    public AconfigStorageException(String msg) {
        super(msg);
        mErrorCode = ERROR_GENERIC;
    }

    /**
     * Constructs a new {@code AconfigStorageException} with a generic error code, the specified
     * detail message, and cause.
     *
     * @param msg The detail message for this exception.
     * @param cause The cause of this exception.
     */
    public AconfigStorageException(String msg, Throwable cause) {
        super(msg, cause);
        mErrorCode = ERROR_GENERIC;
    }

    /**
     * Constructs a new {@code AconfigStorageException} with the specified error code and detail
     * message.
     *
     * @param errorCode The error code for this exception.
     * @param msg The detail message for this exception.
     */
    public AconfigStorageException(int errorCode, String msg) {
        super(msg);
        mErrorCode = errorCode;
    }

    /**
     * Constructs a new {@code AconfigStorageException} with the specified error code, detail
     * message, and cause.
     *
     * @param errorCode The error code for this exception.
     * @param msg The detail message for this exception.
     * @param cause The cause of this exception.
     */
    public AconfigStorageException(int errorCode, String msg, Throwable cause) {
        super(msg, cause);
        mErrorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return The error code.
     */
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
    public String getMessage() {
        return errorString() + ": " + super.getMessage();
    }

    /**
     * Returns a string representation of the error code.
     *
     * @return The error code string.
     */
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
