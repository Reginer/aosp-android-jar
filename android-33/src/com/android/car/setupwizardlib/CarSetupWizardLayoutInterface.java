/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.car.setupwizardlib;

import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;

/**
 * The interface defines the functionality of the layouts used in CarSetupWizard. It
 * makes it easy to switch to a different layout.
 */
public interface CarSetupWizardLayoutInterface {

    /** Returns the primary action button. */
    Button getPrimaryActionButton();

    /** Sets whether the primary action button is visible. */
    void setPrimaryActionButtonVisible(boolean visible);

    /** Sets whether the primary action button is enabled. */
    void setPrimaryActionButtonEnabled(boolean enabled);

    /** Sets the text of the primary action button. */
    void setPrimaryActionButtonText(String text);

    /**
     * Sets the onClick listener of the primary action button.
     *
     * @param listener the listener to be set. When it's null, the previously set listener will be
     *                 removed
     */
    void setPrimaryActionButtonListener(@Nullable View.OnClickListener listener);

    /** Sets whether the primary action button is flat which means it does not have a background. */
    void setPrimaryActionButtonFlat(boolean isFlat);

    /** Returns whether the primary action button is flat. */
    boolean isPrimaryActionButtonFlat();

    /** Returns the secondary action button. */
    Button getSecondaryActionButton();

    /** Sets whether the secondary action button is visible. */
    void setSecondaryActionButtonVisible(boolean visible);

    /** Sets whether the secondary action button is enabled. */
    void setSecondaryActionButtonEnabled(boolean enabled);

    /** Sets the text of the secondary action button. */
    void setSecondaryActionButtonText(String text);

    /**
     * Sets the onClick listener of the secondary action button.
     *
     * @param listener the listener to be set. When it's null, the previously set listener will be
     *                 removed
     */
    void setSecondaryActionButtonListener(@Nullable View.OnClickListener listener);

    /** Sets whether the progress bar is visible. */
    void setProgressBarVisible(boolean visible);

    /** Sets whether the progress bar is indeterminate. */
    void setProgressBarIndeterminate(boolean indeterminate);

    /** Sets the progress bar's progress. */
    void setProgressBarProgress(int progress);
}
