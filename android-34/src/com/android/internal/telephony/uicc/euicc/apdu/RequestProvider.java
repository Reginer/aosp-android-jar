/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.uicc.euicc.apdu;

/**
 * Request provider provides a request via the given request builder. This interface allows the
 * caller of {@link ApduSender} to build different requests based on the response of opening a
 * logical channel.
 *
 * @hide
 */
public interface RequestProvider {
    /**
     * Builds a request based on the response of opening the logical channel.
     *
     * @param selectResponse Response of selecting the channel.
     * @throws Throwable If an exception is thrown, the result callback {@code onException} will
     *     be called to return the exception and no commands will be sent.
     */
    void buildRequest(byte[] selectResponse, RequestBuilder requestBuilder) throws Throwable;
}
