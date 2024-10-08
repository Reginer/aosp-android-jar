/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.ravenwood.annotation.RavenwoodKeepWholeClass;

/**
 * An exception specific to a service.
 *
 * <p>This exception includes an error code specific to the throwing
 * service.  This is mostly used by system services to indicate
 * domain specific error conditions.</p>
 *
 * <p>Since these exceptions are designed to be passed through Binder
 * interfaces, and to be generated by native-code Binder services,
 * they do not support exception chaining.</p>
 *
 * @hide
 */
@SystemApi
@RavenwoodKeepWholeClass
public class ServiceSpecificException extends RuntimeException {
    public final int errorCode;

    public ServiceSpecificException(int errorCode, @Nullable String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ServiceSpecificException(int errorCode) {
        this.errorCode = errorCode;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " (code " + errorCode + ")";
    }
}
