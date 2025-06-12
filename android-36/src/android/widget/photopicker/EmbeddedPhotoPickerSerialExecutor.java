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
package android.widget.photopicker;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * Executes tasks serially, delegating execution to another base {@link Executor}.
 *
 * <p> Regardless of whether base executor is single-threaded or not,
 * this ensures that all incoming tasks are enqueued and executed one after another.
 *
 * <p>Implementation copied from {@link Executor} javadoc.
 */
class EmbeddedPhotoPickerSerialExecutor implements Executor {
    final Queue<Runnable> mTasks = new ArrayDeque<>();
    final Executor mExecutor;
    Runnable mActive;

    EmbeddedPhotoPickerSerialExecutor(Executor executor) {
        this.mExecutor = executor;
    }

    public synchronized void execute(Runnable r) {
        mTasks.add(() -> {
            try {
                r.run();
            } finally {
                scheduleNext();
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
            mExecutor.execute(mActive);
        }
    }
}
