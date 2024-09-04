/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.net.http;

public class CallbackExceptionWrapper extends android.net.http.CallbackException {

    protected CallbackExceptionWrapper(org.chromium.net.CallbackException e) {
        // CallbackException guarantees that its cause will be the exception thrown during the
        // execution of the failed callback. Hence, we need to drop the received
        // org.chromium.net.CallbackException and link its cause to
        // android.net.http.CallbackException.
        super(e.getMessage(), CronetExceptionTranslationUtils.maybeTranslateException(e.getCause()));
    }
}
