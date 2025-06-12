/*
 * Copyright 2017 The Android Open Source Project
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

package android.hardware.location;

import android.app.PendingIntent;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.IContextHubTransactionCallback;

/**
 * @hide
 */
interface IContextHubClient {
    // Sends a message to a nanoapp.
    int sendMessageToNanoApp(in NanoAppMessage message);

    // Closes the connection with the Context Hub.
    void close();

    // Returns the unique ID for this client.
    int getId();

    // Notify the framework that a client callback has finished executing.
    void callbackFinished();

    // Notify the framework that a reliable message client callback has
    // finished executing.
    void reliableMessageCallbackFinished(int messageSequenceNumber, byte errorCode);

    /**
     * Sends a reliable message to a nanoapp.
     *
     * @param message The message to send.
     * @param transactionCallback The transaction callback for reliable message.
     */
    int sendReliableMessageToNanoApp(
        in NanoAppMessage message,
        in IContextHubTransactionCallback transactionCallback);
}
