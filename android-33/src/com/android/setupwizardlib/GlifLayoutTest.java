/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.setupwizardlib;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.IdRes;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.setupwizardlib.template.ColoredHeaderMixin;
import com.android.setupwizardlib.template.HeaderMixin;
import com.android.setupwizardlib.template.IconMixin;
import com.android.setupwizardlib.template.ProgressBarMixin;
import com.android.setupwizardlib.view.StatusBarBackgroundLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
public class GlifLayoutTest {

  private Context context;

  @Before
  public void setUpContext() {
    context = new ContextThemeWrapper(application, R.style.SuwThemeGlif_Light);
  }

  @Test
  public void testDefaultTemplate() {
    GlifLayout layout = new GlifLayout(context);
    assertDefaultTemplateInflated(layout);
  }

  @Test
  public void testSetHeaderText() {
    GlifLayout layout = new GlifLayout(context);
    TextView title = layout.findViewById(R.id.suw_layout_title);
    layout.setHeaderText("Abracadabra");
    assertWithMessage("Header text should be \"Abracadabra\"")
        .that(title.getText().toString())
        .isEqualTo("Abracadabra");
  }

  @Test
  public void testAddView() {
    @IdRes int testViewId = 123456;
    GlifLayout layout = new GlifLayout(context);
    TextView tv = new TextView(context);
    tv.setId(testViewId);
    layout.addView(tv);
    assertDefaultTemplateInflated(layout);
    View view = layout.findViewById(testViewId);
    assertThat(view).named("Text view added").isSameAs(tv);
  }

  @Test
  public void testGetScrollView() {
    GlifLayout layout = new GlifLayout(context);
    assertWithMessage("Get scroll view should not be null with default template")
        .that(layout.getScrollView())
        .isNotNull();
  }

  @Test
  public void testSetPrimaryColor() {
    GlifLayout layout = new GlifLayout(context);
    layout.setProgressBarShown(true);
    layout.setPrimaryColor(ColorStateList.valueOf(Color.RED));
    assertWithMessage("Primary color should be red")
        .that(layout.getPrimaryColor())
        .isEqualTo(ColorStateList.valueOf(Color.RED));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      ProgressBar progressBar = layout.findViewById(R.id.suw_layout_progress);
      assertThat(progressBar.getIndeterminateTintList())
          .named("indeterminate progress bar tint")
          .isEqualTo(ColorStateList.valueOf(Color.RED));
      assertThat(progressBar.getProgressBackgroundTintList())
          .named("determinate progress bar tint")
          .isEqualTo(ColorStateList.valueOf(Color.RED));
    }
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void testSetPrimaryColorTablet() {
    GlifLayout layout = new GlifLayout(context);
    layout.setProgressBarShown(true);
    layout.setPrimaryColor(ColorStateList.valueOf(Color.RED));
    assertWithMessage("Primary color should be red")
        .that(layout.getPrimaryColor())
        .isEqualTo(ColorStateList.valueOf(Color.RED));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      ProgressBar progressBar = layout.findViewById(R.id.suw_layout_progress);
      assertWithMessage("Progress bar should be tinted red")
          .that(progressBar.getIndeterminateTintList())
          .isEqualTo(ColorStateList.valueOf(Color.RED));
      assertWithMessage("Determinate progress bar should also be tinted red")
          .that(progressBar.getProgressBackgroundTintList())
          .isEqualTo(ColorStateList.valueOf(Color.RED));
    }

    assertThat(((GlifPatternDrawable) getTabletBackground(layout)).getColor()).isEqualTo(Color.RED);
  }

  @Test
  public void testSetBackgroundBaseColor() {
    GlifLayout layout = new GlifLayout(context);
    layout.setPrimaryColor(ColorStateList.valueOf(Color.BLUE));
    layout.setBackgroundBaseColor(ColorStateList.valueOf(Color.RED));

    assertThat(((GlifPatternDrawable) getPhoneBackground(layout)).getColor()).isEqualTo(Color.RED);
    assertThat(layout.getBackgroundBaseColor().getDefaultColor()).isEqualTo(Color.RED);
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void testSetBackgroundBaseColorTablet() {
    GlifLayout layout = new GlifLayout(context);
    layout.setPrimaryColor(ColorStateList.valueOf(Color.BLUE));
    layout.setBackgroundBaseColor(ColorStateList.valueOf(Color.RED));

    assertThat(((GlifPatternDrawable) getTabletBackground(layout)).getColor()).isEqualTo(Color.RED);
    assertThat(layout.getBackgroundBaseColor().getDefaultColor()).isEqualTo(Color.RED);
  }

  @Test
  public void testSetBackgroundPatternedTrue() {
    GlifLayout layout = new GlifLayout(context);
    layout.setBackgroundPatterned(true);

    assertThat(getPhoneBackground(layout)).isInstanceOf(GlifPatternDrawable.class);
    assertThat(layout.isBackgroundPatterned()).named("background is patterned").isTrue();
  }

  @Test
  public void testSetBackgroundPatternedFalse() {
    GlifLayout layout = new GlifLayout(context);
    layout.setBackgroundPatterned(false);

    assertThat(getPhoneBackground(layout)).isInstanceOf(ColorDrawable.class);
    assertThat(layout.isBackgroundPatterned()).named("background is patterned").isFalse();
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void testSetBackgroundPatternedTrueTablet() {
    GlifLayout layout = new GlifLayout(context);
    layout.setBackgroundPatterned(true);

    assertThat(getTabletBackground(layout)).isInstanceOf(GlifPatternDrawable.class);
    assertThat(layout.isBackgroundPatterned()).named("background is patterned").isTrue();
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void testSetBackgroundPatternedFalseTablet() {
    GlifLayout layout = new GlifLayout(context);
    layout.setBackgroundPatterned(false);

    assertThat(getTabletBackground(layout)).isInstanceOf(ColorDrawable.class);
    assertThat(layout.isBackgroundPatterned()).named("background is patterned").isFalse();
  }

  @Test
  public void testNonGlifTheme() {
    context = new ContextThemeWrapper(application, android.R.style.Theme);
    new GlifLayout(context);
    // Inflating with a non-GLIF theme should not crash
  }

  @Test
  public void testPeekProgressBarNull() {
    GlifLayout layout = new GlifLayout(context);
    assertWithMessage("PeekProgressBar should return null initially")
        .that(layout.peekProgressBar())
        .isNull();
  }

  @Test
  public void testPeekProgressBar() {
    GlifLayout layout = new GlifLayout(context);
    layout.setProgressBarShown(true);
    assertWithMessage("Peek progress bar should return the bar after setProgressBarShown(true)")
        .that(layout.peekProgressBar())
        .isNotNull();
  }

  @Test
  public void testMixins() {
    GlifLayout layout = new GlifLayout(context);
    final HeaderMixin header = layout.getMixin(HeaderMixin.class);
    assertThat(header).named("header").isInstanceOf(ColoredHeaderMixin.class);

    assertWithMessage("GlifLayout should have icon mixin")
        .that(layout.getMixin(IconMixin.class))
        .isNotNull();
    assertWithMessage("GlifLayout should have progress bar mixin")
        .that(layout.getMixin(ProgressBarMixin.class))
        .isNotNull();
  }

  @Test
  public void testInflateFooter() {
    GlifLayout layout = new GlifLayout(context);

    final View view = layout.inflateFooter(android.R.layout.simple_list_item_1);
    assertThat(view.getId()).isEqualTo(android.R.id.text1);
    assertThat((View) layout.findViewById(android.R.id.text1)).isNotNull();
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void testInflateFooterTablet() {
    testInflateFooter();
  }

  @Test
  public void testInflateFooterBlankTemplate() {
    GlifLayout layout = new GlifLayout(context, R.layout.suw_glif_blank_template);

    final View view = layout.inflateFooter(android.R.layout.simple_list_item_1);
    assertThat(view.getId()).isEqualTo(android.R.id.text1);
    assertThat((View) layout.findViewById(android.R.id.text1)).isNotNull();
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void testInflateFooterBlankTemplateTablet() {
    testInflateFooterBlankTemplate();
  }

  @Test
  public void testFooterXml() {
    GlifLayout layout =
        new GlifLayout(
            context,
            Robolectric.buildAttributeSet()
                .addAttribute(R.attr.suwFooter, "@android:layout/simple_list_item_1")
                .build());

    assertThat((View) layout.findViewById(android.R.id.text1)).isNotNull();
  }

  @Test
  public void inflateStickyHeader_shouldAddViewToLayout() {
    GlifLayout layout = new GlifLayout(context);

    final View view = layout.inflateStickyHeader(android.R.layout.simple_list_item_1);
    assertThat(view.getId()).isEqualTo(android.R.id.text1);
    assertThat((View) layout.findViewById(android.R.id.text1)).isNotNull();
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void inflateStickyHeader_whenOnTablets_shouldAddViewToLayout() {
    inflateStickyHeader_shouldAddViewToLayout();
  }

  @Test
  public void inflateStickyHeader_whenInXml_shouldAddViewToLayout() {
    GlifLayout layout =
        new GlifLayout(
            context,
            Robolectric.buildAttributeSet()
                .addAttribute(R.attr.suwStickyHeader, "@android:layout/simple_list_item_1")
                .build());

    assertThat((View) layout.findViewById(android.R.id.text1)).isNotNull();
  }

  @Test
  public void inflateStickyHeader_whenOnBlankTemplate_shouldAddViewToLayout() {
    GlifLayout layout = new GlifLayout(context, R.layout.suw_glif_blank_template);

    final View view = layout.inflateStickyHeader(android.R.layout.simple_list_item_1);
    assertThat(view.getId()).isEqualTo(android.R.id.text1);
    assertThat((View) layout.findViewById(android.R.id.text1)).isNotNull();
  }

  @Config(qualifiers = "sw600dp")
  @Test
  public void inflateStickyHeader_whenOnBlankTemplateTablet_shouldAddViewToLayout() {
    inflateStickyHeader_whenOnBlankTemplate_shouldAddViewToLayout();
  }

  @Config(minSdk = Config.OLDEST_SDK, maxSdk = Config.NEWEST_SDK)
  @Test
  public void createFromXml_shouldSetLayoutFullscreen_whenLayoutFullscreenIsNotSet() {
    GlifLayout layout = new GlifLayout(context, Robolectric.buildAttributeSet().build());
    if (VERSION.SDK_INT >= VERSION_CODES.M) {
      assertThat(layout.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
          .isEqualTo(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
  }

  @Test
  public void createFromXml_shouldNotSetLayoutFullscreen_whenLayoutFullscreenIsFalse() {
    GlifLayout layout =
        new GlifLayout(
            context,
            Robolectric.buildAttributeSet()
                .addAttribute(R.attr.suwLayoutFullscreen, "false")
                .build());

    assertThat(layout.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN).isEqualTo(0);
  }

  private Drawable getPhoneBackground(GlifLayout layout) {
    final StatusBarBackgroundLayout patternBg = layout.findManagedViewById(R.id.suw_pattern_bg);
    return patternBg.getStatusBarBackground();
  }

  private Drawable getTabletBackground(GlifLayout layout) {
    final View patternBg = layout.findManagedViewById(R.id.suw_pattern_bg);
    return patternBg.getBackground();
  }

  private void assertDefaultTemplateInflated(GlifLayout layout) {
    View title = layout.findViewById(R.id.suw_layout_title);
    assertWithMessage("@id/suw_layout_title should not be null").that(title).isNotNull();

    View icon = layout.findViewById(R.id.suw_layout_icon);
    assertWithMessage("@id/suw_layout_icon should not be null").that(icon).isNotNull();

    View scrollView = layout.findViewById(R.id.suw_scroll_view);
    assertWithMessage("@id/suw_scroll_view should be a ScrollView")
        .that(scrollView instanceof ScrollView)
        .isTrue();
  }
}
