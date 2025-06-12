/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.net.ipsec.ike.shim;

/**
 * Interface to handle IkeSessionStateMachine events
 *
 * <p>This interface MUST be implemented by the IkeSessionStateMachine and will be called by the
 * ShimUtils. In this way ShimUtils can control the IkeSessionStateMachine to have SDK specific
 * behaviors in handling IkeSessionStateMachine events.
 */
public interface IIkeSessionStateMachineShim {
    /** Called if a recoverable error is encountered */
    void onNonFatalError(Exception e);

    /** Called if a fatal error is encountered */
    void onFatalError(Exception e);
}
