package com.android.setupwizardlib.util;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import com.android.setupwizardlib.R;

/**
 * A resolver to resolve the theme from a string or an activity intent, setting options like the
 * default theme and the oldest supported theme. Apps can share the resolver across the entire
 * process by calling {@link #setDefault(ThemeResolver)} in {@link
 * android.app.Application#onCreate()}. If an app needs more granular sharing of the theme default
 * values, additional instances of {@link ThemeResolver} can be created using the builder.
 */
public class ThemeResolver {
  @StyleRes private final int defaultTheme;
  @Nullable private final String oldestSupportedTheme;
  private final boolean useDayNight;

  @Nullable private static ThemeResolver defaultResolver;

  /**
   * Sets the default instance used for the whole process. Can be null to reset the default to the
   * preset one.
   */
  public static void setDefault(@Nullable ThemeResolver resolver) {
    defaultResolver = resolver;
  }

  /**
   * Returns the default instance, which can be changed using {@link #setDefault(ThemeResolver)}.
   */
  public static ThemeResolver getDefault() {
    if (defaultResolver == null) {
      defaultResolver =
          new ThemeResolver.Builder()
              .setDefaultTheme(R.style.SuwThemeGlif_DayNight)
              .setUseDayNight(true)
              .build();
    }
    return defaultResolver;
  }

  private ThemeResolver(
      int defaultTheme, @Nullable String oldestSupportedTheme, boolean useDayNight) {
    this.defaultTheme = defaultTheme;
    this.oldestSupportedTheme = oldestSupportedTheme;
    this.useDayNight = useDayNight;
  }

  /**
   * Returns the style for the theme specified in the intent extra. If the specified string theme is
   * older than the oldest supported theme, the default will be returned instead. Note that the
   * default theme is returned without processing -- it may not be a DayNight theme even if {@link
   * #useDayNight} is true.
   */
  @StyleRes
  public int resolve(Intent intent) {
    return resolve(
        intent.getStringExtra(WizardManagerHelper.EXTRA_THEME),
        /* suppressDayNight= */ WizardManagerHelper.isSetupWizardIntent(intent));
  }

  /**
   * Returns the style for the given string theme. If the specified string theme is older than the
   * oldest supported theme, the default will be returned instead. Note that the default theme is
   * returned without processing -- it may not be a DayNight theme even if {@link #useDayNight} is
   * true.
   */
  @StyleRes
  public int resolve(@Nullable String theme) {
    return resolve(theme, /* suppressDayNight= */ false);
  }

  @StyleRes
  private int resolve(@Nullable String theme, boolean suppressDayNight) {
    int themeResource =
        useDayNight && !suppressDayNight ? getDayNightThemeRes(theme) : getThemeRes(theme);
    if (themeResource == 0) {
      return defaultTheme;
    }

    if (oldestSupportedTheme != null && compareThemes(theme, oldestSupportedTheme) < 0) {
      return defaultTheme;
    }
    return themeResource;
  }

  /** Reads the theme from the intent, and applies the resolved theme to the activity. */
  public void applyTheme(Activity activity) {
    activity.setTheme(resolve(activity.getIntent()));
  }

  /**
   * Returns the corresponding DayNight theme resource ID for the given string theme. DayNight
   * themes are themes that will be either light or dark depending on the system setting. For
   * example, the string {@link WizardManagerHelper#THEME_GLIF_LIGHT} will return
   * {@code @style/SuwThemeGlif.DayNight}.
   */
  @StyleRes
  private static int getDayNightThemeRes(@Nullable String theme) {
    if (theme != null) {
      switch (theme) {
        case WizardManagerHelper.THEME_GLIF_V3_LIGHT:
        case WizardManagerHelper.THEME_GLIF_V3:
          return R.style.SuwThemeGlifV3_DayNight;
        case WizardManagerHelper.THEME_GLIF_V2_LIGHT:
        case WizardManagerHelper.THEME_GLIF_V2:
          return R.style.SuwThemeGlifV2_DayNight;
        case WizardManagerHelper.THEME_GLIF_LIGHT:
        case WizardManagerHelper.THEME_GLIF:
          return R.style.SuwThemeGlif_DayNight;
        case WizardManagerHelper.THEME_MATERIAL_LIGHT:
        case WizardManagerHelper.THEME_MATERIAL:
          return R.style.SuwThemeMaterial_DayNight;
        default:
          // fall through
      }
    }
    return 0;
  }

  /**
   * Returns the theme resource ID for the given string theme. For example, the string {@link
   * WizardManagerHelper#THEME_GLIF_LIGHT} will return {@code @style/SuwThemeGlif.Light}.
   */
  @StyleRes
  private static int getThemeRes(@Nullable String theme) {
    if (theme != null) {
      switch (theme) {
        case WizardManagerHelper.THEME_GLIF_V3_LIGHT:
          return R.style.SuwThemeGlifV3_Light;
        case WizardManagerHelper.THEME_GLIF_V3:
          return R.style.SuwThemeGlifV3;
        case WizardManagerHelper.THEME_GLIF_V2_LIGHT:
          return R.style.SuwThemeGlifV2_Light;
        case WizardManagerHelper.THEME_GLIF_V2:
          return R.style.SuwThemeGlifV2;
        case WizardManagerHelper.THEME_GLIF_LIGHT:
          return R.style.SuwThemeGlif_Light;
        case WizardManagerHelper.THEME_GLIF:
          return R.style.SuwThemeGlif;
        case WizardManagerHelper.THEME_MATERIAL_LIGHT:
          return R.style.SuwThemeMaterial_Light;
        case WizardManagerHelper.THEME_MATERIAL:
          return R.style.SuwThemeMaterial;
        default:
          // fall through
      }
    }
    return 0;
  }

  /** Compares whether the versions of {@code theme1} and {@code theme2} to check which is newer. */
  private static int compareThemes(String theme1, String theme2) {
    return Integer.valueOf(getThemeVersion(theme1)).compareTo(getThemeVersion(theme2));
  }

  /**
   * Returns the version of the theme. The absolute number of the theme version is not defined, but
   * a larger number in the version indicates a newer theme.
   */
  private static int getThemeVersion(String theme) {
    if (theme != null) {
      switch (theme) {
        case WizardManagerHelper.THEME_GLIF_V3_LIGHT:
        case WizardManagerHelper.THEME_GLIF_V3:
          return 4;
        case WizardManagerHelper.THEME_GLIF_V2_LIGHT:
        case WizardManagerHelper.THEME_GLIF_V2:
          return 3;
        case WizardManagerHelper.THEME_GLIF_LIGHT:
        case WizardManagerHelper.THEME_GLIF:
          return 2;
        case WizardManagerHelper.THEME_MATERIAL_LIGHT:
        case WizardManagerHelper.THEME_MATERIAL:
          return 1;
        default:
          // fall through
      }
    }
    return -1;
  }

  /** Builder class for {@link ThemeResolver}. */
  public static class Builder {
    @StyleRes private int defaultTheme = R.style.SuwThemeGlif_DayNight;
    @Nullable private String oldestSupportedTheme = null;
    private boolean useDayNight = true;

    public Builder() {}

    public Builder(ThemeResolver themeResolver) {
      this.defaultTheme = themeResolver.defaultTheme;
      this.oldestSupportedTheme = themeResolver.oldestSupportedTheme;
      this.useDayNight = themeResolver.useDayNight;
    }

    public Builder setDefaultTheme(@StyleRes int defaultTheme) {
      this.defaultTheme = defaultTheme;
      return this;
    }

    public Builder setOldestSupportedTheme(String oldestSupportedTheme) {
      this.oldestSupportedTheme = oldestSupportedTheme;
      return this;
    }

    public Builder setUseDayNight(boolean useDayNight) {
      this.useDayNight = useDayNight;
      return this;
    }

    public ThemeResolver build() {
      return new ThemeResolver(defaultTheme, oldestSupportedTheme, useDayNight);
    }
  }
}
