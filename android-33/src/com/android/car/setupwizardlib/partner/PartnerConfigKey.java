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

package com.android.car.setupwizardlib.partner;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        PartnerConfigKey.KEY_IMMERSIVE_MODE,
        PartnerConfigKey.KEY_TOOLBAR_BG_COLOR,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_ICON_BACK,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_ICON_CLOSE,
        PartnerConfigKey.KEY_TOOLBAR_NAV_BUTTON_MIRRORING_IN_RTL,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_FONT_FAMILY,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_PADDING_HORIZONTAL,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_PADDING_VERTICAL,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_RADIUS,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_SPACING,
        PartnerConfigKey.KEY_TOOLBAR_BUTTON_TEXT_SIZE,
        PartnerConfigKey.KEY_TOOLBAR_PRIMARY_BUTTON_BG,
        PartnerConfigKey.KEY_TOOLBAR_PRIMARY_BUTTON_BG_COLOR,
        PartnerConfigKey.KEY_TOOLBAR_PRIMARY_BUTTON_TEXT_COLOR,
        PartnerConfigKey.KEY_TOOLBAR_SECONDARY_BUTTON_BG,
        PartnerConfigKey.KEY_TOOLBAR_SECONDARY_BUTTON_BG_COLOR,
        PartnerConfigKey.KEY_TOOLBAR_SECONDARY_BUTTON_TEXT_COLOR,
        PartnerConfigKey.KEY_TOOLBAR_DIVIDER_BG,
        PartnerConfigKey.KEY_TOOLBAR_DIVIDER_LINE_WEIGHT,
        PartnerConfigKey.KEY_LOADING_INDICATOR_COLOR,
        PartnerConfigKey.KEY_LOADING_INDICATOR_LINE_WEIGHT,
        PartnerConfigKey.KEY_LAYOUT_BG_COLOR,
        PartnerConfigKey.KEY_ULTRA_WIDE_SCREEN_CONTENT_WIDTH
})

/** Resource names that can be customized by partner overlay APK. */
public @interface PartnerConfigKey {

    String KEY_IMMERSIVE_MODE = "suw_compat_immersive_mode";

    String KEY_TOOLBAR_BG_COLOR = "suw_compat_toolbar_bg_color";

    String KEY_TOOLBAR_BUTTON_ICON_BACK = "suw_compat_toolbar_button_icon_back";

    String KEY_TOOLBAR_BUTTON_ICON_CLOSE = "suw_compat_toolbar_button_icon_close";

    String KEY_TOOLBAR_NAV_BUTTON_MIRRORING_IN_RTL =
            "suw_compat_toolbar_nav_button_mirroring_in_rtl";

    String KEY_TOOLBAR_BUTTON_FONT_FAMILY = "suw_compat_toolbar_button_font_family";

    String KEY_TOOLBAR_BUTTON_TEXT_SIZE = "suw_compat_toolbar_button_text_size";

    String KEY_TOOLBAR_BUTTON_PADDING_HORIZONTAL = "suw_compat_toolbar_button_padding_horizontal";

    String KEY_TOOLBAR_BUTTON_PADDING_VERTICAL = "suw_compat_toolbar_button_padding_vertical";

    String KEY_TOOLBAR_BUTTON_RADIUS = "suw_compat_toolbar_button_radius";

    String KEY_TOOLBAR_BUTTON_SPACING = "suw_compat_toolbar_button_spacing";

    String KEY_TOOLBAR_PRIMARY_BUTTON_BG = "suw_compat_toolbar_primary_button_bg";

    String KEY_TOOLBAR_PRIMARY_BUTTON_BG_COLOR =
            "suw_compat_toolbar_primary_button_bg_color";

    String KEY_TOOLBAR_PRIMARY_BUTTON_TEXT_COLOR = "suw_compat_toolbar_primary_button_text_color";

    String KEY_TOOLBAR_SECONDARY_BUTTON_BG = "suw_compat_toolbar_secondary_button_bg";

    String KEY_TOOLBAR_SECONDARY_BUTTON_BG_COLOR = "suw_compat_toolbar_secondary_button_bg_color";

    String KEY_TOOLBAR_SECONDARY_BUTTON_TEXT_COLOR =
            "suw_compat_toolbar_secondary_button_text_color";

    String KEY_TOOLBAR_DIVIDER_BG = "suw_compat_toolbar_divider_bg";

    String KEY_TOOLBAR_DIVIDER_LINE_WEIGHT = "suw_compat_toolbar_divider_line_weight";

    String KEY_LOADING_INDICATOR_COLOR = "suw_compat_loading_indicator_color";

    String KEY_LOADING_INDICATOR_LINE_WEIGHT = "suw_compat_loading_indicator_line_weight";

    String KEY_LAYOUT_BG_COLOR = "suw_design_layout_bg_color";

    String KEY_ULTRA_WIDE_SCREEN_CONTENT_WIDTH = "suw_compat_ultra_wide_screen_content_width";
}
