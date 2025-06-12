/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.content;

import android.os.IBinder;

/**
 * Interface for monitoring the state of an application service.  See
 * {@link android.app.Service} and
 * {@link Context#bindService Context.bindService()} for more information.
 * <p>Like many callbacks from the system, the methods on this class are called
 * from the main thread of your process.
 */
public interface ServiceConnection {
    /**
     * Called when a connection to the Service has been established, with
     * the {@link android.os.IBinder} of the communication channel to the
     * Service.
     *
     * <p class="note"><b>Note:</b> If the system has started to bind your
     * client app to a service, it's possible that your app will never receive
     * this callback. Your app won't receive a callback if there's an issue with
     * the service, such as the service crashing while being created.
     *
     * @param name The concrete component name of the service that has
     * been connected.
     *
     * @param service The IBinder of the Service's communication channel,
     * which you can now make calls on.
     */
    void onServiceConnected(ComponentName name, IBinder service);

    /**
     * Called when a connection to the Service has been lost.  This typically
     * happens when the process hosting the service has crashed or been killed.
     * This does <em>not</em> remove the ServiceConnection itself -- this
     * binding to the service will remain active, and you will receive a call
     * to {@link #onServiceConnected} when the Service is next running.
     *
     * @param name The concrete component name of the service whose
     * connection has been lost.
     */
    void onServiceDisconnected(ComponentName name);

    /**
     * Called when the binding to this connection is dead.  This means the
     * interface will never receive another connection.  The application will
     * need to unbind and rebind the connection to activate it again.  This may
     * happen, for example, if the application hosting the service it is bound to
     * has been updated.
     *
     * <p class="note"><b>Note:</b> The app that requested the binding must call
     * {@link Context#unbindService(ServiceConnection)} to release the tracking
     * resources associated with this ServiceConnection even if this callback was
     * invoked following {@link Context#bindService Context.bindService() bindService()}.
     *
     * @param name The concrete component name of the service whose connection is dead.
     */
    default void onBindingDied(ComponentName name) {
    }

    /**
     * Called when the service being bound has returned {@code null} from its
     * {@link android.app.Service#onBind(Intent) onBind()} method.  This indicates
     * that the attempted service binding represented by this ServiceConnection
     * will never become usable.
     *
     * <p class="note"><b>Note:</b> The app that requested the binding must still call
     * {@link Context#unbindService(ServiceConnection)} to release the tracking
     * resources associated with this ServiceConnection even if this callback was
     * invoked following {@link Context#bindService Context.bindService() bindService()}.
     *
     * @param name The concrete component name of the service whose binding
     *     has been rejected by the Service implementation.
     */
    default void onNullBinding(ComponentName name) {
    }
}
