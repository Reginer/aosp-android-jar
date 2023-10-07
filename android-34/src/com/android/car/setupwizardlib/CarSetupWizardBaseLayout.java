/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import com.android.car.setupwizardlib.partner.PartnerConfig;
import com.android.car.setupwizardlib.partner.PartnerConfigHelper;
import com.android.car.setupwizardlib.util.FeatureResolver;
import java.util.Locale;
import java.util.Objects;

/**
 * Custom layout for the Car Setup Wizard. Provides accessors for modifying elements such as buttons
 * and progress bars. Any modifications to elements built by the CarSetupWizardBaseLayout should be
 * done through methods provided by this class unless that is not possible so as to keep the state
 * internally consistent.
 */
class CarSetupWizardBaseLayout extends LinearLayout implements CarSetupWizardLayoutInterface {
    private static final String TAG = CarSetupWizardBaseLayout.class.getSimpleName();
    private static final int INVALID_COLOR = 0;
    // For mirroring an image
    private static final float IMAGE_MIRROR_ROTATION = 180.0f;
    private static final float MIN_ULTRA_WIDE_CONTENT_WIDTH = 1240.0f;

    private View mBackButton;
    private View mCloseButton;
    private View mTitleBar;
    private TextView mToolbarTitle;
    private PartnerConfigHelper mPartnerConfigHelper;
    private boolean mSupportsSplitNavLayout;
    private boolean mSupportsRotaryControl;

    /* <p>The Primary Toolbar Button should always be used when there is only a single action that
     * moves the wizard to the next screen (e.g. Only need a 'Skip' button).
     *
     * When there are two actions that can move the wizard to the next screen (e.g. either 'Skip'
     * or 'Let's Go' are the two options), then the Primary is used for the positive action
     * while the Secondary is used for the negative action.</p>
     */
    private Button mPrimaryToolbarButton;

    /*
     * Flag to track the primary toolbar button flat state.
     */
    private boolean mPrimaryToolbarButtonFlat;
    private View.OnClickListener mPrimaryToolbarButtonOnClick;
    private Button mSecondaryToolbarButton;
    private ImageView mDivider;
    private ProgressBar mProgressBar;

    CarSetupWizardBaseLayout(Context context) {
        this(context, null);
    }

    CarSetupWizardBaseLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    CarSetupWizardBaseLayout(Context context, @Nullable AttributeSet attrs,
                             int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * On initialization, the layout gets all of the custom attributes and initializes
     * the custom views that can be set by the user (e.g. back button, continue button).
     */
    CarSetupWizardBaseLayout(Context context, @Nullable AttributeSet attrs,
                             int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mPartnerConfigHelper = PartnerConfigHelper.get(context);
        TypedArray attrArray = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CarSetupWizardBaseLayout,
                0, 0);

        init(attrArray);
    }

    /**
     * Inflates the layout and sets the custom views (e.g. back button, continue button).
     */
    private void init(TypedArray attrArray) {
        boolean showBackButton;
        boolean showCloseButton;

        boolean showToolbarTitle;
        String toolbarTitleText;

        boolean showPrimaryToolbarButton;
        String primaryToolbarButtonText;
        boolean primaryToolbarButtonEnabled;

        boolean showSecondaryToolbarButton;
        String secondaryToolbarButtonText;
        boolean secondaryToolbarButtonEnabled;

        boolean showProgressBar;
        boolean indeterminateProgressBar;

        try {
            showBackButton = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_showBackButton, true);
            showCloseButton = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_showCloseButton, false);
            showToolbarTitle = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_showToolbarTitle, false);
            toolbarTitleText = attrArray.getString(
                    R.styleable.CarSetupWizardBaseLayout_toolbarTitleText);
            showPrimaryToolbarButton = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_showPrimaryToolbarButton, true);
            primaryToolbarButtonText = attrArray.getString(
                    R.styleable.CarSetupWizardBaseLayout_primaryToolbarButtonText);
            primaryToolbarButtonEnabled = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_primaryToolbarButtonEnabled, true);
            mPrimaryToolbarButtonFlat = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_primaryToolbarButtonFlat, false);
            showSecondaryToolbarButton = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_showSecondaryToolbarButton, false);
            secondaryToolbarButtonText = attrArray.getString(
                    R.styleable.CarSetupWizardBaseLayout_secondaryToolbarButtonText);
            secondaryToolbarButtonEnabled = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_secondaryToolbarButtonEnabled, true);
            showProgressBar = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_showProgressBar, false);
            indeterminateProgressBar = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_indeterminateProgressBar, true);
            mSupportsSplitNavLayout = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_supportsSplitNavLayout, false);
            mSupportsRotaryControl = attrArray.getBoolean(
                    R.styleable.CarSetupWizardBaseLayout_supportsRotaryControl, false);
        } finally {
            attrArray.recycle();
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(getLayoutResourceId(), this);

        maybeSetUltraWideScreenContentWidth();

        View toolbar = findViewById(R.id.application_bar);
        // The toolbar will not be mirrored in RTL
        toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        setBackButton(findViewById(R.id.back_button));
        setCloseButton(findViewById(R.id.close_button));

        Drawable drawable = mPartnerConfigHelper.getDrawable(
                getContext(), PartnerConfig.CONFIG_TOOLBAR_BUTTON_ICON_BACK);
        if (drawable != null) {
            ((ImageView) mBackButton).setImageDrawable(drawable);
        }

        Drawable closeButtonDrawable = mPartnerConfigHelper.getDrawable(
                getContext(), PartnerConfig.CONFIG_TOOLBAR_BUTTON_ICON_CLOSE);
        if (closeButtonDrawable != null) {
            ((ImageView) mCloseButton).setImageDrawable(closeButtonDrawable);
        }

        if (shouldMirrorNavIcons()) {
            Log.v(TAG, "Mirroring navigation icons");
            mBackButton.setRotation(IMAGE_MIRROR_ROTATION);
            mCloseButton.setRotation(IMAGE_MIRROR_ROTATION);
        }

        if (showBackButton && showCloseButton) {
            Log.w(TAG, "Showing Back and Close button simultaneously is not supported");
        }

        // Set the back button visibility based on the custom attribute.
        setBackButtonVisible(showBackButton);
        // Set the close button visibility based on the custom attribute.
        setCloseButtonVisible(showCloseButton);

        // Se the title bar.
        setTitleBar(findViewById(R.id.application_bar));
        if (isSplitNavLayoutUsed()) {
            mTitleBar.setBackgroundColor(getResources()
                    .getColor(R.color.suw_color_split_nav_toolbar_background, null));
        } else {
            int toolbarBgColor = mPartnerConfigHelper.getColor(
                    getContext(), PartnerConfig.CONFIG_TOOLBAR_BG_COLOR);
            if (toolbarBgColor != 0) {
                mTitleBar.setBackgroundColor(toolbarBgColor);
            }
        }

        // Set the toolbar title visibility and text based on the custom attributes.
        setToolbarTitle(findViewById(R.id.toolbar_title));
        if (showToolbarTitle) {
            setToolbarTitleText(toolbarTitleText);
        } else {
            setToolbarTitleVisible(false);
        }

        // Set the primary continue button visibility and text based on the custom attributes.
        ViewStub primaryToolbarButtonStub =
                (ViewStub) findViewById(R.id.primary_toolbar_button_stub);
        // Set the button layout to flat if that attribute was set.
        if (mPrimaryToolbarButtonFlat) {
            primaryToolbarButtonStub.setLayoutResource(R.layout.flat_button);
        }
        primaryToolbarButtonStub.inflate();
        setPrimaryToolbarButton(findViewById(R.id.primary_toolbar_button));
        stylePrimaryToolbarButton(mPrimaryToolbarButton);
        setPrimaryToolbarButtonText(primaryToolbarButtonText);
        setPrimaryToolbarButtonEnabled(primaryToolbarButtonEnabled);
        setPrimaryToolbarButtonVisible(showPrimaryToolbarButton);

        // Set the secondary continue button visibility and text based on the custom attributes.
        if (showSecondaryToolbarButton || !TextUtils.isEmpty(secondaryToolbarButtonText)) {
            inflateSecondaryToolbarButtonIfNecessary();
            setSecondaryToolbarButtonText(secondaryToolbarButtonText);
            setSecondaryToolbarButtonEnabled(secondaryToolbarButtonEnabled);
            setSecondaryToolbarButtonVisible(showSecondaryToolbarButton);
        }

        mProgressBar = findViewById(R.id.progress_bar);
        setProgressBarVisible(showProgressBar);
        setProgressBarIndeterminate(indeterminateProgressBar);
        int tintColor = mPartnerConfigHelper.getColor(
                getContext(),
                PartnerConfig.CONFIG_LOADING_INDICATOR_COLOR);
        if (tintColor != INVALID_COLOR) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(tintColor));
        }

        float lineWeight = mPartnerConfigHelper.getDimension(
                getContext(),
                PartnerConfig.CONFIG_LOADING_INDICATOR_LINE_WEIGHT);
        if (lineWeight > 0) {
            ViewGroup.LayoutParams layoutParams = mProgressBar.getLayoutParams();
            layoutParams.height = Math.round(lineWeight);
            mProgressBar.setLayoutParams(layoutParams);
        }

        initDivider();

        // Set orientation programmatically since the inflated layout uses <merge>
        if (isSplitNavLayoutUsed() && getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            setOrientation(LinearLayout.HORIZONTAL);
            // The vertical bar will not be mirrored in RTL
            setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

            View contentContainer = getContentContainer();
            if (contentContainer != null) {
                // The content should be mirrored in RTL
                contentContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
            }
            View actionBar = findViewById(R.id.button_container);
            if (actionBar != null) {
                // The action bar will not be mirrored in RTL
                actionBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            }
        } else {
            setOrientation(LinearLayout.VERTICAL);
        }
    }

    /**
     * Set a given view's visibility.
     */
    @VisibleForTesting
    void setViewVisible(View view, boolean visible) {
        if (view == null) {
            return;
        }
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // Add or remove the back button touch delegate depending on whether it is visible.
    @VisibleForTesting
    void updateNavigationButtonTouchDelegate(View button, boolean visible) {
        if (button == null) {
            return;
        }
        if (visible) {
            // Post this action in the parent's message queue to make sure the parent
            // lays out its children before getHitRect() is called
            this.post(() -> {
                Rect delegateArea = new Rect();

                button.getHitRect(delegateArea);

                /*
                 * Update the delegate area based on the difference between the current size and
                 * the touch target size
                 */
                float touchTargetSize = getResources().getDimension(
                        R.dimen.car_touch_target_size);
                float primaryIconSize = getResources().getDimension(
                        R.dimen.car_primary_icon_size);

                int sizeDifference = (int) ((touchTargetSize - primaryIconSize) / 2);

                delegateArea.right += sizeDifference;
                delegateArea.bottom += sizeDifference;
                delegateArea.left -= sizeDifference;
                delegateArea.top -= sizeDifference;

                // Set the TouchDelegate on the parent view
                TouchDelegate touchDelegate = new TouchDelegate(delegateArea, button);

                if (View.class.isInstance(button.getParent())) {
                    ((View) button.getParent()).setTouchDelegate(touchDelegate);
                }
            });
        } else {
            // Set the TouchDelegate to null if the back button is not visible.
            if (View.class.isInstance(button.getParent())) {
                ((View) button.getParent()).setTouchDelegate(null);
            }
        }
    }

    /**
     * Gets the back button.
     */
    public View getBackButton() {
        return mBackButton;
    }

    @VisibleForTesting
    final void setBackButton(View backButton) {
        mBackButton = backButton;
    }

    /**
     * Set the back button onClickListener to given listener. Can be null if the listener should
     * be overridden so no callback is made.
     */
    public void setBackButtonListener(@Nullable View.OnClickListener listener) {
        mBackButton.setOnClickListener(listener);
    }

    /**
     * Set the back button visibility to the given visibility.
     */
    public void setBackButtonVisible(boolean visible) {
        if (visible) {
            setViewVisible(mCloseButton, false);
            updateNavigationButtonTouchDelegate(mCloseButton, false);
        }
        setViewVisible(mBackButton, visible);
        updateNavigationButtonTouchDelegate(mBackButton, visible);
    }

    public View getCloseButton() {
        return mCloseButton;
    }

    @VisibleForTesting
    final void setCloseButton(View closeButton) {
        mCloseButton = closeButton;
    }

    /**
     * Set the close button onClickListener to given listener. Can be null if the listener should
     * be overridden so no callback is made.
     */
    public void setCloseButtonListener(@Nullable View.OnClickListener listener) {
        mCloseButton.setOnClickListener(listener);
    }

    /**
     * Set the back button visibility to the given visibility.
     */
    public void setCloseButtonVisible(boolean visible) {
        if (visible) {
            setViewVisible(mBackButton, false);
            updateNavigationButtonTouchDelegate(mBackButton, false);
        }
        setViewVisible(mCloseButton, visible);
        updateNavigationButtonTouchDelegate(mCloseButton, visible);
    }

    /**
     * Gets the toolbar title.
     */
    public TextView getToolbarTitle() {
        return mToolbarTitle;
    }

    @VisibleForTesting
    final void setToolbarTitle(TextView toolbarTitle) {
        mToolbarTitle = toolbarTitle;
    }

    /**
     * Sets the header title visibility to given value.
     */
    public void setToolbarTitleVisible(boolean visible) {
        if (mToolbarTitle == null) {
            return;
        }
        setViewVisible(mToolbarTitle, visible);
    }

    /**
     * Sets the header title text to the provided text.
     */
    public void setToolbarTitleText(String text) {
        if (mToolbarTitle == null) {
            return;
        }
        mToolbarTitle.setText(text);
    }

    /**
     * Sets the style for the toolbar title.
     */
    public void setToolbarTitleStyle(@StyleRes int style) {
        if (mToolbarTitle == null) {
            return;
        }
        mToolbarTitle.setTextAppearance(style);
    }

    /**
     * Gets the primary toolbar button.
     */
    public Button getPrimaryToolbarButton() {
        return mPrimaryToolbarButton;
    }

    @VisibleForTesting
    final void setPrimaryToolbarButton(Button primaryToolbarButton) {
        mPrimaryToolbarButton = primaryToolbarButton;
    }

    /**
     * Set the primary continue button visibility to the given visibility.
     */
    public void setPrimaryToolbarButtonVisible(boolean visible) {
        setViewVisible(mPrimaryToolbarButton, visible);
    }

    /**
     * Set whether the primary continue button is enabled.
     */
    public void setPrimaryToolbarButtonEnabled(boolean enabled) {
        mPrimaryToolbarButton.setEnabled(enabled);
    }

    /**
     * Set the primary continue button text to the given text.
     */
    public void setPrimaryToolbarButtonText(String text) {
        mPrimaryToolbarButton.setText(text);
    }

    /**
     * Set the primary continue button onClickListener to the given listener. Can be null if the
     * listener should be overridden so no callback is made. All changes to primary toolbar
     * button's onClickListener should be made here so they can be stored through changes to the
     * button.
     */
    public void setPrimaryToolbarButtonListener(@Nullable View.OnClickListener listener) {
        mPrimaryToolbarButtonOnClick = listener;
        mPrimaryToolbarButton.setOnClickListener(listener);
    }

    /**
     * Getter for the flatness of the primary toolbar button.
     */
    public boolean getPrimaryToolbarButtonFlat() {
        return mPrimaryToolbarButtonFlat;
    }

    /**
     * Changes the button in the primary slot to a flat theme, maintaining the text, visibility,
     * whether it is enabled, and id.
     * <p>NOTE: that other attributes set manually on the primaryToolbarButton will be lost on calls
     * to this method as the button will be replaced.</p>
     */
    public void setPrimaryToolbarButtonFlat(boolean isFlat) {
        // Do nothing if the state isn't changing.
        if (isFlat == mPrimaryToolbarButtonFlat) {
            return;
        }
        mPrimaryToolbarButtonFlat = isFlat;
        Button newPrimaryButton = createPrimaryToolbarButton(isFlat);

        ViewGroup parent = (ViewGroup) findViewById(R.id.button_container);
        int buttonIndex = parent.indexOfChild(mPrimaryToolbarButton);
        parent.removeViewAt(buttonIndex);
        parent.addView(newPrimaryButton, buttonIndex);

        // Update state of layout
        setPrimaryToolbarButton(newPrimaryButton);
    }

    @VisibleForTesting
    Button createPrimaryToolbarButton(boolean isFlat) {
        int layoutId = isFlat ? R.layout.flat_button : R.layout.primary_button;
        Button newPrimaryButton = (Button) inflate(getContext(), layoutId, null);
        newPrimaryButton.setId(mPrimaryToolbarButton.getId());
        newPrimaryButton.setVisibility(mPrimaryToolbarButton.getVisibility());
        newPrimaryButton.setEnabled(mPrimaryToolbarButton.isEnabled());
        newPrimaryButton.setText(mPrimaryToolbarButton.getText());
        newPrimaryButton.setOnClickListener(mPrimaryToolbarButtonOnClick);
        newPrimaryButton.setLayoutParams(mPrimaryToolbarButton.getLayoutParams());
        stylePrimaryToolbarButton(newPrimaryButton);

        return newPrimaryButton;
    }

    /**
     * Gets the secondary toolbar button.
     */
    public Button getSecondaryToolbarButton() {
        return mSecondaryToolbarButton;
    }

    /**
     * Set the secondary continue button visibility to the given visibility.
     */
    public void setSecondaryToolbarButtonVisible(boolean visible) {
        // If not setting it visible and it hasn't been inflated yet then don't inflate.
        if (!visible && mSecondaryToolbarButton == null) {
            return;
        }
        inflateSecondaryToolbarButtonIfNecessary();
        setViewVisible(mSecondaryToolbarButton, visible);
    }

    /**
     * Sets whether the secondary continue button is enabled.
     */
    public void setSecondaryToolbarButtonEnabled(boolean enabled) {
        inflateSecondaryToolbarButtonIfNecessary();
        mSecondaryToolbarButton.setEnabled(enabled);
    }

    /**
     * Sets the secondary continue button text to the given text.
     */
    public void setSecondaryToolbarButtonText(String text) {
        inflateSecondaryToolbarButtonIfNecessary();
        mSecondaryToolbarButton.setText(text);
    }

    /**
     * Sets the secondary continue button onClickListener to the given listener. Can be null if the
     * listener should be overridden so no callback is made.
     */
    public void setSecondaryToolbarButtonListener(@Nullable View.OnClickListener listener) {
        inflateSecondaryToolbarButtonIfNecessary();
        mSecondaryToolbarButton.setOnClickListener(listener);
    }

    /**
     * Gets the progress bar.
     */
    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    @Override
    public void setProgressBarVisible(boolean visible) {
        setViewVisible(mProgressBar, visible);
    }

    @Override
    public void setProgressBarIndeterminate(boolean indeterminate) {
        mProgressBar.setIndeterminate(indeterminate);
    }

    @Override
    public void setProgressBarProgress(int progress) {
        setProgressBarIndeterminate(false);
        mProgressBar.setProgress(progress);
    }

    @Override
    public Button getPrimaryActionButton() {
        return getPrimaryToolbarButton();
    }

    @Override
    public void setPrimaryActionButtonVisible(boolean visible) {
        setPrimaryToolbarButtonVisible(visible);
    }

    @Override
    public void setPrimaryActionButtonEnabled(boolean enabled) {
        setPrimaryToolbarButtonEnabled(enabled);
    }

    @Override
    public void setPrimaryActionButtonText(String text) {
        setPrimaryToolbarButtonText(text);
    }

    @Override
    public void setPrimaryActionButtonListener(@Nullable View.OnClickListener listener) {
        setPrimaryToolbarButtonListener(listener);
    }

    @Override
    public void setPrimaryActionButtonFlat(boolean isFlat) {
        setPrimaryToolbarButtonFlat(isFlat);
    }

    @Override
    public boolean isPrimaryActionButtonFlat() {
        return getPrimaryToolbarButtonFlat();
    }

    @Override
    public Button getSecondaryActionButton() {
        return getSecondaryToolbarButton();
    }

    @Override
    public void setSecondaryActionButtonVisible(boolean visible) {
        setSecondaryToolbarButtonVisible(visible);
    }

    @Override
    public void setSecondaryActionButtonEnabled(boolean enabled) {
        setSecondaryToolbarButtonEnabled(enabled);
    }

    @Override
    public void setSecondaryActionButtonText(String text) {
        setSecondaryToolbarButtonText(text);
    }

    @Override
    public void setSecondaryActionButtonListener(@Nullable View.OnClickListener listener) {
        setSecondaryToolbarButtonListener(listener);
    }

    /**
     * Returns whether split-nav layout is currently being used. Do not use this API to determine
     * whether content ViewStub should be inflated. Use {@code getContentViewStub} for that purpose.
     */
    public boolean isSplitNavLayoutUsed() {
        boolean isSplitNavLayoutEnabled = FeatureResolver.get(getContext())
                .isSplitNavLayoutFeatureEnabled();
        return mSupportsSplitNavLayout && isSplitNavLayoutEnabled;
    }

    /**
     * Returns the content ViewStub of the split-nav layout.
     *
     * @deprecated Use {@code getContentViewStub}.
     */
    @Deprecated
    public ViewStub getSplitNavContentViewStub() {
        return findViewById(R.id.layout_content_stub);
    }

    /**
     * Returns the content ViewStub when split-nav layout is used or rotary control is supported
     */
    public ViewStub getContentViewStub() {
        return findViewById(R.id.layout_content_stub);
    }

    /**
     * Sets the locale to be used for rendering.
     */
    public void applyLocale(Locale locale) {
        if (locale == null) {
            return;
        }
        int direction = TextUtils.getLayoutDirectionFromLocale(locale);

        if (mToolbarTitle != null) {
            mToolbarTitle.setTextLocale(locale);
            mToolbarTitle.setLayoutDirection(direction);
        }

        mPrimaryToolbarButton.setTextLocale(locale);
        mPrimaryToolbarButton.setLayoutDirection(direction);

        mSecondaryToolbarButton.setTextLocale(locale);
        mSecondaryToolbarButton.setLayoutDirection(direction);
    }

    /**
     * Sets the title bar view.
     */
    private void setTitleBar(View titleBar) {
        mTitleBar = titleBar;
    }

    /**
     * A method that inflates the SecondaryToolbarButton if it is has not already been
     * inflated. If it has been inflated already this method will do nothing.
     */
    private void inflateSecondaryToolbarButtonIfNecessary() {
        ViewStub secondaryToolbarButtonStub = findViewById(R.id.secondary_toolbar_button_stub);
        // If the secondaryToolbarButtonStub is null then the stub has been inflated so there is
        // nothing to do.
        if (secondaryToolbarButtonStub != null) {
            secondaryToolbarButtonStub.inflate();
            mSecondaryToolbarButton = findViewById(R.id.secondary_toolbar_button);
            setSecondaryToolbarButtonVisible(false);

            setBackground(
                    mSecondaryToolbarButton,
                    PartnerConfig.CONFIG_TOOLBAR_SECONDARY_BUTTON_BG,
                    PartnerConfig.CONFIG_TOOLBAR_SECONDARY_BUTTON_BG_COLOR);

            setButtonPadding(mSecondaryToolbarButton);
            setButtonTypeFace(mSecondaryToolbarButton);
            setButtonTextSize(mSecondaryToolbarButton);
            setButtonTextColor(
                    mSecondaryToolbarButton,
                    PartnerConfig.CONFIG_TOOLBAR_SECONDARY_BUTTON_TEXT_COLOR);

            // Set button spacing
            float marginEnd = PartnerConfigHelper.get(getContext()).getDimension(
                    getContext(),
                    PartnerConfig.CONFIG_TOOLBAR_BUTTON_SPACING);

            MarginLayoutParams layoutParams =
                    (MarginLayoutParams) mSecondaryToolbarButton.getLayoutParams();
            layoutParams.setMarginEnd(Math.round(marginEnd));
        }
    }

    /**
     * Sets button text color using partner overlay if exists
     */
    @VisibleForTesting
    void setButtonTextColor(TextView button, PartnerConfig config) {
        ColorStateList colorStateList =
                mPartnerConfigHelper.getColorStateList(getContext(), config);
        if (colorStateList != null) {
            button.setTextColor(colorStateList);
        }
    }

    /**
     * Sets background using partner overlay if exists. Background color and radius are only
     * applied if background resource doesn't exist. Otherwise default background color and radius
     * may override what's set in the background.
     */
    @VisibleForTesting
    void setBackground(View view, PartnerConfig bgConfig, PartnerConfig bgColorConfig) {
        Drawable background = mPartnerConfigHelper.getDrawable(getContext(), bgConfig);
        if (background == null) {
            if (view instanceof Button) {
                setButtonRadius((Button) view);
            }
            setBackgroundColor(view, bgColorConfig);
        } else {
            view.setBackground(background);
        }
    }

    /**
     * Sets button background color using partner overlay if exists
     */
    @VisibleForTesting
    void setBackgroundColor(View button, PartnerConfig config) {
        ColorStateList color = mPartnerConfigHelper.getColorStateList(getContext(), config);
        if (color != null) {
            button.setBackgroundTintList(color);
        }
    }

    /**
     * Sets button text size using partner overlay if exists
     */
    @VisibleForTesting
    void setButtonTextSize(TextView button) {
        float dimension = mPartnerConfigHelper.getDimension(
                getContext(),
                PartnerConfig.CONFIG_TOOLBAR_BUTTON_TEXT_SIZE);
        if (dimension != 0) {
            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimension);
        }
    }

    @VisibleForTesting
    boolean shouldMirrorNavIcons() {
        return getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                && mPartnerConfigHelper.getBoolean(
                getContext(), PartnerConfig.CONFIG_TOOLBAR_NAV_ICON_MIRRORING_IN_RTL, true);
    }

    /**
     * Sets button type face with partner overlay if exists
     */
    private void setButtonTypeFace(TextView button) {
        String fontFamily = mPartnerConfigHelper.getString(
                getContext(),
                PartnerConfig.CONFIG_TOOLBAR_BUTTON_FONT_FAMILY);
        if (TextUtils.isEmpty(fontFamily)) {
            return;
        }

        Typeface typeface = Typeface.create(fontFamily, Typeface.NORMAL);
        if (Objects.equals(typeface, Typeface.DEFAULT)) {
            Log.w(TAG, String.format(
                    "Couldn't find font: %s. Setting default font.",
                    fontFamily));
        }
        button.setTypeface(typeface);
    }

    /**
     * Sets button radius using partner overlay if exists
     */
    private void setButtonRadius(Button button) {
        float radius = mPartnerConfigHelper.getDimension(
                getContext(),
                PartnerConfig.CONFIG_TOOLBAR_BUTTON_RADIUS);

        GradientDrawable gradientDrawable = getGradientDrawable(button);
        if (gradientDrawable != null) {
            gradientDrawable.setCornerRadius(radius);
        }
    }

    private void setButtonPadding(Button button) {
        int hPadding = Math.round(
                PartnerConfigHelper.get(getContext()).getDimension(
                        getContext(),
                        PartnerConfig.CONFIG_TOOLBAR_BUTTON_PADDING_HORIZONTAL)
        );
        int vPadding = Math.round(
                PartnerConfigHelper.get(getContext()).getDimension(
                        getContext(),
                        PartnerConfig.CONFIG_TOOLBAR_BUTTON_PADDING_VERTICAL)
        );
        button.setPadding(hPadding, vPadding, hPadding, vPadding);
    }

    private void stylePrimaryToolbarButton(Button primaryButton) {
        if (!mPrimaryToolbarButtonFlat) {
            setBackground(
                    primaryButton,
                    PartnerConfig.CONFIG_TOOLBAR_PRIMARY_BUTTON_BG,
                    PartnerConfig.CONFIG_TOOLBAR_PRIMARY_BUTTON_BG_COLOR);
        }

        setButtonPadding(primaryButton);
        setButtonTypeFace(primaryButton);
        setButtonTextSize(primaryButton);

        PartnerConfig textColorConfig = mPrimaryToolbarButtonFlat
                ? PartnerConfig.CONFIG_TOOLBAR_SECONDARY_BUTTON_TEXT_COLOR
                : PartnerConfig.CONFIG_TOOLBAR_PRIMARY_BUTTON_TEXT_COLOR;
        setButtonTextColor(primaryButton, textColorConfig);
    }

    private int getLayoutResourceId() {
        if (isSplitNavLayoutUsed()) {
            return mSupportsRotaryControl
                    ? R.layout.rotary_split_nav_layout
                    : R.layout.split_nav_layout;
        }

        return mSupportsRotaryControl
                ? R.layout.rotary_car_setup_wizard_layout
                : R.layout.car_setup_wizard_layout;
    }

    private void initDivider() {
        mDivider = findViewById(R.id.divider);
        if (mDivider == null) {
            return;
        }
        float dividerHeight = mPartnerConfigHelper.getDimension(
                getContext(),
                PartnerConfig.CONFIG_TOOLBAR_DIVIDER_LINE_WEIGHT);
        if (dividerHeight >= 0) {
            ViewGroup.LayoutParams layoutParams = mDivider.getLayoutParams();
            layoutParams.height = Math.round(dividerHeight);
            mDivider.setLayoutParams(layoutParams);
        }
        if (dividerHeight > 0) {
            Drawable dividerBg = mPartnerConfigHelper.getDrawable(
                    getContext(),
                    PartnerConfig.CONFIG_TOOLBAR_DIVIDER_BG);
            if (dividerBg != null) {
                mDivider.setBackground(dividerBg);
            }
        }
    }

    private GradientDrawable getGradientDrawable(Button button) {
        Drawable drawable = button.getBackground();
        if (drawable instanceof InsetDrawable) {
            return getGradientDrawableFromInsetDrawable((InsetDrawable) drawable);
        }

        if (drawable instanceof RippleDrawable) {
            drawable = ((RippleDrawable) drawable).getDrawable(0);
            if (drawable instanceof InsetDrawable) {
                return getGradientDrawableFromInsetDrawable((InsetDrawable) drawable);
            }
            if (drawable instanceof GradientDrawable) {
                return (GradientDrawable) drawable;
            }
        }

        return null;
    }

    private GradientDrawable getGradientDrawableFromInsetDrawable(InsetDrawable insetDrawable) {
        if (insetDrawable.getDrawable() instanceof GradientDrawable) {
            return (GradientDrawable) insetDrawable.getDrawable();
        }
        return null;
    }

    private void maybeSetUltraWideScreenContentWidth() {
        View contentContainer = findViewById(R.id.ultra_wide_content_container);
        if (contentContainer == null) {
            return;
        }

        float configurableContentWidth = mPartnerConfigHelper.getDimension(
                getContext(),
                PartnerConfig.CONFIG_ULTRA_WIDE_SCREEN_CONTENT_WIDTH);

        float pxMinWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                MIN_ULTRA_WIDE_CONTENT_WIDTH,
                getResources().getDisplayMetrics());

        if (configurableContentWidth >= pxMinWidth) {
            ViewGroup.LayoutParams layoutParams = contentContainer.getLayoutParams();
            layoutParams.width = (int) configurableContentWidth;
            Log.d(TAG, String.format("Applying content width %f px", configurableContentWidth));
            contentContainer.setLayoutParams(layoutParams);
        } else {
            if (configurableContentWidth != 0) {
                Log.w(TAG, String.format("The minimum ultra wide screen content width is %d dp",
                        (int) MIN_ULTRA_WIDE_CONTENT_WIDTH));
            }

            LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                    0, LayoutParams.MATCH_PARENT);
            contentParams.weight = 1;
            contentContainer.setLayoutParams(contentParams);

            LinearLayout.LayoutParams fillerParams = new LinearLayout.LayoutParams(
                    0, LayoutParams.MATCH_PARENT);
            fillerParams.weight = 0;
            View filler = findViewById(R.id.ultra_wide_space_filler);
            filler.setLayoutParams(fillerParams);
        }
    }

    private View getContentContainer() {
        View contentContainer = findViewById(R.id.content_container);
        if (contentContainer == null) {
            // Try ultra-wide container
            return findViewById(R.id.ultra_wide_content_container);

        }
        return contentContainer;
    }
}
