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

package com.android.setupwizardlib.util;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.StyleRes;
import com.android.setupwizardlib.R;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.NEWEST_SDK)
public class ThemeResolverTest {

  @After
  public void resetDefaultThemeResolver() {
    ThemeResolver.setDefault(null);
  }

  @Test
  public void resolve_nonDayNight_shouldReturnCorrespondingTheme() {
    @StyleRes int defaultTheme = 12345;
    ThemeResolver themeResolver =
        new ThemeResolver.Builder().setDefaultTheme(defaultTheme).setUseDayNight(false).build();
    assertThat(themeResolver.resolve("material")).isEqualTo(R.style.SuwThemeMaterial);
    assertThat(themeResolver.resolve("material_light")).isEqualTo(R.style.SuwThemeMaterial_Light);
    assertThat(themeResolver.resolve("glif")).isEqualTo(R.style.SuwThemeGlif);
    assertThat(themeResolver.resolve("glif_light")).isEqualTo(R.style.SuwThemeGlif_Light);
    assertThat(themeResolver.resolve("glif_v2")).isEqualTo(R.style.SuwThemeGlifV2);
    assertThat(themeResolver.resolve("glif_v2_light")).isEqualTo(R.style.SuwThemeGlifV2_Light);
    assertThat(themeResolver.resolve("glif_v3")).isEqualTo(R.style.SuwThemeGlifV3);
    assertThat(themeResolver.resolve("glif_v3_light")).isEqualTo(R.style.SuwThemeGlifV3_Light);
    assertThat(themeResolver.resolve("unknown_theme")).isEqualTo(defaultTheme);
  }

  @Test
  public void resolve_dayNight_shouldReturnDayNightTheme() {
    @StyleRes int defaultTheme = 12345;
    ThemeResolver themeResolver = new ThemeResolver.Builder().setDefaultTheme(defaultTheme).build();
    assertThat(themeResolver.resolve("material")).isEqualTo(R.style.SuwThemeMaterial_DayNight);
    assertThat(themeResolver.resolve("material_light"))
        .isEqualTo(R.style.SuwThemeMaterial_DayNight);
    assertThat(themeResolver.resolve("glif")).isEqualTo(R.style.SuwThemeGlif_DayNight);
    assertThat(themeResolver.resolve("glif_light")).isEqualTo(R.style.SuwThemeGlif_DayNight);
    assertThat(themeResolver.resolve("glif_v2")).isEqualTo(R.style.SuwThemeGlifV2_DayNight);
    assertThat(themeResolver.resolve("glif_v2_light")).isEqualTo(R.style.SuwThemeGlifV2_DayNight);
    assertThat(themeResolver.resolve("glif_v3")).isEqualTo(R.style.SuwThemeGlifV3_DayNight);
    assertThat(themeResolver.resolve("glif_v3_light")).isEqualTo(R.style.SuwThemeGlifV3_DayNight);
    assertThat(themeResolver.resolve("unknown_theme")).isEqualTo(defaultTheme);
  }

  @Test
  public void resolve_newerThanOldestSupportedTheme_shouldReturnSpecifiedTheme() {
    ThemeResolver themeResolver =
        new ThemeResolver.Builder()
            .setOldestSupportedTheme(WizardManagerHelper.THEME_GLIF_V2)
            .build();
    assertThat(themeResolver.resolve("glif_v2")).isEqualTo(R.style.SuwThemeGlifV2_DayNight);
    assertThat(themeResolver.resolve("glif_v2_light")).isEqualTo(R.style.SuwThemeGlifV2_DayNight);
    assertThat(themeResolver.resolve("glif_v3")).isEqualTo(R.style.SuwThemeGlifV3_DayNight);
    assertThat(themeResolver.resolve("glif_v3_light")).isEqualTo(R.style.SuwThemeGlifV3_DayNight);
  }

  @Test
  public void resolve_olderThanOldestSupportedTheme_shouldReturnDefault() {
    @StyleRes int defaultTheme = 12345;
    ThemeResolver themeResolver =
        new ThemeResolver.Builder()
            .setDefaultTheme(defaultTheme)
            .setOldestSupportedTheme(WizardManagerHelper.THEME_GLIF_V2)
            .build();
    assertThat(themeResolver.resolve("material")).isEqualTo(defaultTheme);
    assertThat(themeResolver.resolve("material_light")).isEqualTo(defaultTheme);
    assertThat(themeResolver.resolve("glif")).isEqualTo(defaultTheme);
    assertThat(themeResolver.resolve("glif_light")).isEqualTo(defaultTheme);
  }

  @Test
  public void resolve_intentTheme_shouldReturnCorrespondingTheme() {
    @StyleRes int defaultTheme = 12345;
    ThemeResolver themeResolver =
        new ThemeResolver.Builder().setDefaultTheme(defaultTheme).setUseDayNight(false).build();
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "material")))
        .isEqualTo(R.style.SuwThemeMaterial);
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "material_light")))
        .isEqualTo(R.style.SuwThemeMaterial_Light);
    assertThat(
            themeResolver.resolve(new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "glif")))
        .isEqualTo(R.style.SuwThemeGlif);
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "glif_light")))
        .isEqualTo(R.style.SuwThemeGlif_Light);
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v2")))
        .isEqualTo(R.style.SuwThemeGlifV2);
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v2_light")))
        .isEqualTo(R.style.SuwThemeGlifV2_Light);
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v3")))
        .isEqualTo(R.style.SuwThemeGlifV3);
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v3_light")))
        .isEqualTo(R.style.SuwThemeGlifV3_Light);
    assertThat(
            themeResolver.resolve(
                new Intent().putExtra(WizardManagerHelper.EXTRA_THEME, "unknown_theme")))
        .isEqualTo(defaultTheme);
  }

  @Test
  public void resolve_suwIntent_shouldForceNonDayNightTheme() {
    @StyleRes int defaultTheme = 12345;
    ThemeResolver themeResolver =
        new ThemeResolver.Builder().setDefaultTheme(defaultTheme).setUseDayNight(true).build();
    Intent originalIntent = new Intent().putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent).putExtra(WizardManagerHelper.EXTRA_THEME, "material")))
        .isEqualTo(R.style.SuwThemeMaterial);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent)
                    .putExtra(WizardManagerHelper.EXTRA_THEME, "material_light")))
        .isEqualTo(R.style.SuwThemeMaterial_Light);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent).putExtra(WizardManagerHelper.EXTRA_THEME, "glif")))
        .isEqualTo(R.style.SuwThemeGlif);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent).putExtra(WizardManagerHelper.EXTRA_THEME, "glif_light")))
        .isEqualTo(R.style.SuwThemeGlif_Light);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent).putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v2")))
        .isEqualTo(R.style.SuwThemeGlifV2);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent)
                    .putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v2_light")))
        .isEqualTo(R.style.SuwThemeGlifV2_Light);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent).putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v3")))
        .isEqualTo(R.style.SuwThemeGlifV3);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent)
                    .putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v3_light")))
        .isEqualTo(R.style.SuwThemeGlifV3_Light);
    assertThat(
            themeResolver.resolve(
                new Intent(originalIntent)
                    .putExtra(WizardManagerHelper.EXTRA_THEME, "unknown_theme")))
        .isEqualTo(defaultTheme);
  }

  @Test
  public void applyTheme_glifV3_shouldSetActivityThemeToGlifV3() {
    @StyleRes int defaultTheme = 12345;
    ThemeResolver themeResolver =
        new ThemeResolver.Builder().setUseDayNight(false).setDefaultTheme(defaultTheme).build();

    Activity activity =
        Robolectric.buildActivity(
                Activity.class,
                new Intent()
                    .putExtra(WizardManagerHelper.EXTRA_THEME, WizardManagerHelper.THEME_GLIF_V3))
            .setup()
            .get();

    themeResolver.applyTheme(activity);

    assertThat(shadowOf(activity).callGetThemeResId()).isEqualTo(R.style.SuwThemeGlifV3);
  }

  @Test
  public void setDefault_shouldSetDefaultResolver() {
    ThemeResolver themeResolver = new ThemeResolver.Builder().setUseDayNight(false).build();

    ThemeResolver.setDefault(themeResolver);
    assertThat(ThemeResolver.getDefault()).isSameAs(themeResolver);
  }
}
