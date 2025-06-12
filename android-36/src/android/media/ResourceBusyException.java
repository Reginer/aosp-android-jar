/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.media;

/**
 * Exception thrown when an operation on a MediaDrm object is attempted
 * and hardware resources are not available, due to being in use.
 */
public final class ResourceBusyException extends MediaDrmException {
    public ResourceBusyException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * @hide
     */
    public ResourceBusyException(String message, int vendorError, int oemError, int context) {
        super(message, vendorError, oemError, context);
    }
}
