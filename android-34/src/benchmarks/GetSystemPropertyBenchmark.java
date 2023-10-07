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

package benchmarks;

import java.security.AccessController;

import sun.security.action.GetPropertyAction;

/**
 * Compares performance of accessing system properties via
 * legacy security code
 * {@code AccessController.doPrivileged(new GetPropertyAction(key[, default]))}
 * vs. direct invocation of {@code System.getProperty(key[, default])}.
 *
 * As of 2018-07, libcore carries some patches to perform such short-circuiting,
 * so it's interesting to know how much better it performs.
 */
public class GetSystemPropertyBenchmark {

    public void timeSystem_getProperty_default(int reps) {
        for (int i = 0; i < reps; i++) {
            System.getProperty("user.language", "en");
        }
    }

    public void timeSystem_getProperty(int reps) {
        for (int i = 0; i < reps; i++) {
            System.getProperty("user.region");
        }
    }

    public void timeAccessController_getPropertyAction(int reps) {
        for (int i = 0; i < reps; i++) {
            AccessController.doPrivileged(
                    new GetPropertyAction("user.language", "en"));
        }
    }

    public void timeAccessController_getPropertyAction_default(int reps) {
        for (int i = 0; i < reps; i++) {
            AccessController.doPrivileged(
                    new GetPropertyAction("user.region"));
        }
    }

}
