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

/** Resources that can be customized by partner overlay APK. */
public enum PartnerConfig {

    CONFIG_IMMERSIVE_MODE(
        PartnerConfigKey.KEY_IMMERSIVE_MODE, ResourceType.STRING),

    CONFIG_TOOLBAR_BG_COLOR(
            PartnerConfigKey.KEY_TOOLBAR_BG_COLOR, ResourceType.COLOR),

    CONFIG_TOOLBAR_BUTTON_ICON_BACK(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_ICON_BACK, ResourceType.DRAWABLE),

    CONFIG_TOOLBAR_BUTTON_ICON_CLOSE(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_ICON_CLOSE, ResourceType.DRAWABLE),

    CONFIG_TOOLBAR_NAV_ICON_MIRRORING_IN_RTL(
            PartnerConfigKey.KEY_TOOLBAR_NAV_BUTTON_MIRRORING_IN_RTL, ResourceType.BOOLEAN),

    CONFIG_TOOLBAR_BUTTON_FONT_FAMILY(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_FONT_FAMILY, ResourceType.STRING),

    CONFIG_TOOLBAR_BUTTON_PADDING_HORIZONTAL(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_PADDING_HORIZONTAL, ResourceType.DIMENSION),

    CONFIG_TOOLBAR_BUTTON_PADDING_VERTICAL(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_PADDING_VERTICAL, ResourceType.DIMENSION),

    CONFIG_TOOLBAR_BUTTON_RADIUS(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_RADIUS, ResourceType.DIMENSION),

    CONFIG_TOOLBAR_BUTTON_SPACING(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_SPACING, ResourceType.DIMENSION),

    CONFIG_TOOLBAR_BUTTON_TEXT_SIZE(
            PartnerConfigKey.KEY_TOOLBAR_BUTTON_TEXT_SIZE, ResourceType.DIMENSION),

    CONFIG_TOOLBAR_PRIMARY_BUTTON_BG(
            PartnerConfigKey.KEY_TOOLBAR_PRIMARY_BUTTON_BG, ResourceType.DRAWABLE),

    CONFIG_TOOLBAR_PRIMARY_BUTTON_BG_COLOR(
            PartnerConfigKey.KEY_TOOLBAR_PRIMARY_BUTTON_BG_COLOR, ResourceType.COLOR),

    CONFIG_TOOLBAR_PRIMARY_BUTTON_TEXT_COLOR(
            PartnerConfigKey.KEY_TOOLBAR_PRIMARY_BUTTON_TEXT_COLOR, ResourceType.COLOR),

    CONFIG_TOOLBAR_SECONDARY_BUTTON_BG(
            PartnerConfigKey.KEY_TOOLBAR_SECONDARY_BUTTON_BG, ResourceType.DRAWABLE),

    CONFIG_TOOLBAR_SECONDARY_BUTTON_BG_COLOR(
            PartnerConfigKey.KEY_TOOLBAR_SECONDARY_BUTTON_BG_COLOR, ResourceType.COLOR),

    CONFIG_TOOLBAR_SECONDARY_BUTTON_TEXT_COLOR(
            PartnerConfigKey.KEY_TOOLBAR_SECONDARY_BUTTON_TEXT_COLOR, ResourceType.COLOR),

    CONFIG_TOOLBAR_DIVIDER_BG(
            PartnerConfigKey.KEY_TOOLBAR_DIVIDER_BG, ResourceType.DRAWABLE),

    CONFIG_TOOLBAR_DIVIDER_LINE_WEIGHT(
            PartnerConfigKey.KEY_TOOLBAR_DIVIDER_LINE_WEIGHT, ResourceType.DIMENSION),

    CONFIG_LOADING_INDICATOR_COLOR(
            PartnerConfigKey.KEY_LOADING_INDICATOR_COLOR, ResourceType.COLOR),

    CONFIG_LOADING_INDICATOR_LINE_WEIGHT(
            PartnerConfigKey.KEY_LOADING_INDICATOR_LINE_WEIGHT, ResourceType.DIMENSION),

    CONFIG_LAYOUT_BG_COLOR(
            PartnerConfigKey.KEY_LAYOUT_BG_COLOR, ResourceType.COLOR),

    CONFIG_ULTRA_WIDE_SCREEN_CONTENT_WIDTH(
            PartnerConfigKey.KEY_ULTRA_WIDE_SCREEN_CONTENT_WIDTH, ResourceType.DIMENSION);

    public enum ResourceType {
        COLOR,
        DRAWABLE,
        STRING,
        DIMENSION,
        BOOLEAN,
    }

    private final String mResourceName;
    private final ResourceType mResourceType;

    public ResourceType getResourceType() {
        return mResourceType;
    }

    public String getResourceName() {
        return mResourceName;
    }

    PartnerConfig(@PartnerConfigKey String resourceName, ResourceType type) {
        this.mResourceName = resourceName;
        this.mResourceType = type;
    }
}
