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

package com.android.car.setupwizardlib;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.car.setupwizardlib.util.CarDrivingStateMonitor;
import com.android.car.setupwizardlib.util.CarWizardManagerHelper;


/**
 * Base Activity for CarSetupWizard screens that provides a variety of helper functions that make
 * it easier to work with the CarSetupWizardLayout and moving between Setup Wizard screens.
 *
 * <p>This activity sets an instance of {@link CarSetupWizardLayout} as the Content View.
 * <p>Provides helper methods like {@link #setContentFragment} and {@link #onContentFragmentSet} for
 * easy updating of the CarSetupWizard layout components based on the current Fragment being
 * displayed
 * <p>Provides helper methods like {@link #nextAction} and {@link #finishAction} for properly
 * moving to the next and previous screens in a Setup Wizard
 * <p>Provides setters {@link #setBackButtonVisible(boolean)} for setting CarSetupWizardLayout
 * component attributes
 *
 * @deprecated Use {@link BaseCompatActivity} or {@link BaseDesignActivity}.
 */
@Deprecated
public class BaseActivity extends FragmentActivity {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String CONTENT_FRAGMENT_TAG = "CONTENT_FRAGMENT_TAG";
    /**
     * Wizard Manager does not actually return an activity result, but if we invoke Wizard
     * Manager without requesting a result, the framework will choose not to issue a call to
     * onActivityResult with RESULT_CANCELED when navigating backward.
     */
    protected static final int REQUEST_CODE_NEXT = 10000;

    private boolean mNextActionAlreadyTriggered;

    /**
     * To implement a specific request code, see the following:
     *
     * <pre>{@code
     * static final int REQUEST_CODE_A = REQUEST_CODE_FIRST_USER;
     * static final int REQUEST_CODE_B = REQUEST_CODE_FIRST_USER + 1;</pre>
     * }
     */
    protected static final int REQUEST_CODE_FIRST_USER = 10001;

    private int mResultCode = RESULT_CANCELED;

    /**
     * Whether it is safe to make transactions on the
     * {@link androidx.fragment.app.FragmentManager}. This variable prevents a possible exception
     * when calling commit() on the FragmentManager.
     *
     * <p>The default value is {@code true} because it is only after
     * {@link #onSaveInstanceState(Bundle)} that fragment commits are not allowed.
     */
    private boolean mAllowFragmentCommits = true;
    private CarSetupWizardLayout mCarSetupWizardLayout;
    private Intent mResultData;

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_activity);

        mCarSetupWizardLayout = findViewById(R.id.car_setup_wizard_layout);

        mCarSetupWizardLayout.setBackButtonListener(v -> {
            if (!handleBackButton()) {
                finish();
            }
        });

        resetPrimaryToolbarButtonOnClickListener();
        resetSecondaryToolbarButtonOnClickListener();

        /* If this activity has a saved instance and a content fragment, call onContentFragmentSet()
         * so the appropriate views/events are updated.
         */
        if (savedInstanceState != null && getContentFragment() != null) {
            onContentFragmentSet(getContentFragment());
        }
    }

    @Override
    @CallSuper
    protected void onStart() {
        super.onStart();
        // Fragment commits are not allowed once the Activity's state has been saved. Once
        // onStart() has been called, the FragmentManager should now allow commits.
        mAllowFragmentCommits = true;
        // Need to check for UX restrictions to setup wizard running and exit if they are enabled.
        CarDrivingStateMonitor.get(this).startMonitor();
    }

    @Override
    @CallSuper
    protected void onResume() {
        super.onResume();
        // Need to reset next buttons so that they can be pressed again.
        mNextActionAlreadyTriggered = false;
    }

    @Override
    @CallSuper
    protected void onPause() {
        super.onPause();
        // Need this for visibility for tests.
    }

    @Override
    @CallSuper
    protected void onStop() {
        super.onStop();
        // Trigger a stop to the CarDrivingStateMonitor. If the monitor is restarted soon by a
        // subsequent activity then this will do nothing so as not to thrash the monitor.
        CarDrivingStateMonitor.get(this).stopMonitor();
    }

    @Override
    @CallSuper
    protected void onSaveInstanceState(Bundle outState) {
        // A transaction can only be committed with this method prior to its containing activity
        // saving its state.
        mAllowFragmentCommits = false;
        super.onSaveInstanceState(outState);
    }

    // Content Fragment accessors

    /**
     * Sets the content fragment and adds it to the fragment backstack.
     */
    @CallSuper
    protected void setContentFragmentWithBackstack(Fragment fragment) {
        if (mAllowFragmentCommits) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.car_setup_wizard_layout, fragment, CONTENT_FRAGMENT_TAG)
                    .addToBackStack(null)
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
            onContentFragmentSet(getContentFragment());
        }
    }

    /**
     * Returns the fragment that is currently being displayed as the content view.
     */
    @CallSuper
    protected Fragment getContentFragment() {
        return getSupportFragmentManager().findFragmentByTag(CONTENT_FRAGMENT_TAG);
    }

    /**
     * Sets the fragment that will be shown as the main content of this Activity.
     */
    @CallSuper
    protected void setContentFragment(Fragment fragment) {
        if (mAllowFragmentCommits) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.animator.fade_in,
                            android.R.animator.fade_out,
                            android.R.animator.fade_in,
                            android.R.animator.fade_out)
                    .replace(R.id.car_setup_wizard_layout, fragment, CONTENT_FRAGMENT_TAG)
                    .commitNow();
            onContentFragmentSet(getContentFragment());
        }
    }

    /**
     * Pops the top Fragment from the Fragment backstack (immediately executing the transaction) and
     * then updates the CarSetupWizardLayout toolbar for the current fragment.
     *
     * @return {@code true} if a fragment was popped.
     */
    @CallSuper
    protected boolean popBackStackImmediate() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
            onContentFragmentSet(getContentFragment());
            return true;
        }
        return false;
    }

    /**
     * Method to be overwritten by subclasses wanting to perform any additional actions when the
     * content fragment is set (usually via adding or popping from fragment backstack). For example,
     * the CarSetupWizardLayout toolbar usually needs to be changed when the fragment changes.
     */
    protected void onContentFragmentSet(Fragment fragment) {

    }

    /**
     * Sets the layout view that will be shown as the main content of this Activity. Should be used
     * when the activity does not hold a fragment.
     */
    @CallSuper
    protected View setContentLayout(@LayoutRes int id) {
        return getLayoutInflater().inflate(id, mCarSetupWizardLayout);
    }

    /**
     * Method to be overwritten by subclasses wanting to implement their own back behavior.
     * Default behavior is to pop a fragment off of the backstack if one exists, otherwise call
     * finish()
     *
     * @return {@code true} whether to call finish()
     */
    protected boolean handleBackButton() {
        return popBackStackImmediate();
    }

    /**
     * Called when nextAction has been invoked, should be overridden on derived class when it is
     * needed perform work when nextAction has been invoked.
     */
    protected void onNextActionInvoked() {
    }

    /**
     * Moves to the next Activity in the SetupWizard flow.
     */
    protected void nextAction(int resultCode) {
        nextAction(resultCode, null);
    }

    /**
     * Moves to the next Activity in the SetupWizard flow, and save the intent data.
     */
    protected void nextAction(int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            throw new IllegalArgumentException("Cannot call nextAction with RESULT_CANCELED");
        }
        setResultCode(resultCode, data);
        if (mNextActionAlreadyTriggered) {
            Log.v("CarSetupWizard",
                    "BaseActivity: nextAction triggered multiple times without page refresh, "
                            + "ignoring.");
            return;
        }
        mNextActionAlreadyTriggered = true;
        onNextActionInvoked();
        Intent nextIntent =
                CarWizardManagerHelper.getNextIntent(getIntent(), mResultCode, mResultData);
        startActivity(nextIntent);
    }

    /**
     * Method for finishing an action. The default behavior is to close out the screen and
     * go back to the previous one.
     */
    protected void finishAction() {
        finishAction(RESULT_CANCELED);
    }

    /**
     * Method for finishing an action with a non-default result code. This is a convenience
     * method to replace nextAction(resultCode); finish();
     */
    protected void finishAction(int resultCode) {
        finishAction(resultCode, null);
    }

    /**
     * Convenience method for nextAction(resultCode, data); finish();
     */
    protected void finishAction(int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            nextAction(resultCode, data);
        }
        finish();
    }

    /**
     * Method to retrieve resultCode saved via {@link #setResultCode(int, Intent)}
     */
    protected int getResultCode() {
        return mResultCode;
    }

    /**
     * Use instead of {@link #setResult(int, Intent)} so that the resultCode
     * and data can be referenced later
     */
    protected void setResultCode(int resultCode, Intent data) {
        mResultCode = resultCode;
        mResultData = data;
        setResult(resultCode, data);
    }

    /**
     * Use instead of {@link #setResult(int)} so that the resultCode can be referenced later
     */
    protected void setResultCode(int resultCode) {
        setResultCode(resultCode, getResultData());
    }

    /**
     * Method to retrieve intent data saved via {@link #setResultCode(int, Intent)}
     */
    protected Intent getResultData() {
        return mResultData;
    }


    // CarSetupWizardLayout Accessors

    /**
     * Sets whether the back button is visible. If this value is {@code true}, clicking the
     * button will take the user back to the previous screen in the setup flow.
     */
    protected void setBackButtonVisible(boolean visible) {
        mCarSetupWizardLayout.setBackButtonVisible(visible);
    }

    /**
     * Sets whether the toolbar title is visible.
     */
    protected void setToolbarTitleVisible(boolean visible) {
        mCarSetupWizardLayout.setToolbarTitleVisible(visible);
    }

    /**
     * Sets the text for the toolbar title.
     */
    protected void setToolbarTitleText(String text) {
        mCarSetupWizardLayout.setToolbarTitleText(text);
    }

    /**
     * Sets the text appearance for the toolbar title.
     */
    protected void setToolbarTitleStyle(@StyleRes int style) {
        mCarSetupWizardLayout.setToolbarTitleStyle(style);
    }

    /**
     * Sets whether the primary continue button is visible.
     */
    protected void setPrimaryToolbarButtonVisible(boolean visible) {
        mCarSetupWizardLayout.setPrimaryToolbarButtonVisible(visible);
    }

    /**
     * Sets whether the primary continue button is enabled. If this value is {@code true},
     * clicking the button will take the user to the next screen in the setup flow.
     */
    protected void setPrimaryToolbarButtonEnabled(boolean enabled) {
        mCarSetupWizardLayout.setPrimaryToolbarButtonEnabled(enabled);
    }

    /**
     * Sets the text of the primary continue button.
     */
    protected void setPrimaryToolbarButtonText(String text) {
        mCarSetupWizardLayout.setPrimaryToolbarButtonText(text);
    }

    /**
     * Sets whether the primary button is displayed as a flat or raised button.
     */
    protected void setPrimaryToolbarButtonFlat(boolean flat) {
        mCarSetupWizardLayout.setPrimaryToolbarButtonFlat(flat);
    }

    /**
     * Sets the primary button onClick behavior to a custom method.
     *
     * <p>NOTE: This will overwrite the primary tool bar button's default action to call
     * {@link #nextAction} with RESULT_OK.
     */
    protected void setPrimaryToolbarButtonOnClickListener(View.OnClickListener listener) {
        mCarSetupWizardLayout.setPrimaryToolbarButtonListener(listener);
    }

    /**
     * Reset's the primary toolbar button's on click listener to call {@link #nextAction} with
     * RESULT_OK
     */
    protected void resetPrimaryToolbarButtonOnClickListener() {
        setPrimaryToolbarButtonOnClickListener(v -> {
            nextAction(RESULT_OK);
        });

    }

    /**
     * Sets whether the secondary continue button is visible.
     */
    protected void setSecondaryToolbarButtonVisible(boolean visible) {
        mCarSetupWizardLayout.setSecondaryToolbarButtonVisible(visible);
    }

    /**
     * Sets whether the secondary continue button is enabled. If this value is {@code true},
     * clicking the button will take the user to the next screen in the setup flow.
     */
    protected void setSecondaryToolbarButtonEnabled(boolean enabled) {
        mCarSetupWizardLayout.setSecondaryToolbarButtonEnabled(enabled);
    }

    /**
     * Sets the text of the secondary continue button.
     */
    protected void setSecondaryToolbarButtonText(String text) {
        mCarSetupWizardLayout.setSecondaryToolbarButtonText(text);
    }

    /**
     * Sets the secondary button onClick behavior to a custom method.
     *
     * <p>NOTE: This will overwrite the secondary tool bar button's default action to call
     * {@link #nextAction} with RESULT_OK.
     */
    protected void setSecondaryToolbarButtonOnClickListener(View.OnClickListener listener) {
        mCarSetupWizardLayout.setSecondaryToolbarButtonListener(listener);
    }

    /**
     * Reset's the secondary toolbar button's on click listener to call {@link #nextAction} with
     * RESULT_OK
     */
    protected void resetSecondaryToolbarButtonOnClickListener() {
        setSecondaryToolbarButtonOnClickListener(v -> {
            nextAction(RESULT_OK);
        });
    }

    /**
     * Sets whether the progress bar is visible.
     */
    protected void setProgressBarVisible(boolean visible) {
        mCarSetupWizardLayout.setProgressBarVisible(visible);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    boolean getAllowFragmentCommits() {
        return mAllowFragmentCommits;
    }

    protected CarSetupWizardLayout getCarSetupWizardLayout() {
        return mCarSetupWizardLayout;
    }
}
