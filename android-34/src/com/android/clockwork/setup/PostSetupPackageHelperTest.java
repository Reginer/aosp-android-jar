package com.android.clockwork.setup;

import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_ANDROID;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_IOS;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_UNKNOWN;
import static com.android.clockwork.setup.PostSetupPackageHelper.DISABLE_APP_FOR_ANDROID;
import static com.android.clockwork.setup.PostSetupPackageHelper.DISABLE_APP_FOR_IOS;
import static com.android.clockwork.setup.PostSetupPackageHelper.DISABLE_LAUNCHER_FOR_ANDROID;
import static com.android.clockwork.setup.PostSetupPackageHelper.DISABLE_LAUNCHER_FOR_IOS;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.provider.Settings;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(AndroidJUnit4.class)
public class PostSetupPackageHelperTest {
    private static final String TEST_PACKAGE1 = "com.test.package";
    private static final String TEST_PACKAGE2 = "com.test.package2";
    private static final String POST_SETUP_TEST_LAUNCHER_COMPONENT =
            TEST_PACKAGE2 + "/com.internal.test.package.HomeActivity";

    private static final String PACKAGE_CONFIG_DELIM = ":";

    private static final String CONFIG_PACKAGE_DISABLE_FOR_ANDROID =
            TEST_PACKAGE1 + PACKAGE_CONFIG_DELIM + DISABLE_APP_FOR_ANDROID;
    private static final String CONFIG_PACKAGE_DISABLE_FOR_IOS =
            TEST_PACKAGE1 + PACKAGE_CONFIG_DELIM + DISABLE_APP_FOR_IOS;
    private static final String CONFIG_LAUNCHER_DISABLE_FOR_ANDROID =
            TEST_PACKAGE2 + PACKAGE_CONFIG_DELIM + DISABLE_LAUNCHER_FOR_ANDROID;
    private static final String CONFIG_LAUNCHER_DISABLE_FOR_IOS =
            TEST_PACKAGE2 + PACKAGE_CONFIG_DELIM + DISABLE_LAUNCHER_FOR_IOS;

    private Context mContext;
    @Mock private Resources mockResources;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testPackageSetup_pairedWithAndroid() throws Exception {
        // Paired device OS is Android
        setPairedDeviceOsType(PAIRED_DEVICE_OS_TYPE_ANDROID);
        // Setup test package/component configs
        preparePostSetupPackageResources(
                CONFIG_PACKAGE_DISABLE_FOR_ANDROID, CONFIG_LAUNCHER_DISABLE_FOR_ANDROID);
        setUpPackageEnabled(TEST_PACKAGE1, true);
        setUpComponentEnabled(POST_SETUP_TEST_LAUNCHER_COMPONENT, true);

        new PostSetupPackageHelper(mContext, mockResources).run();

        assertPackageEnabled(TEST_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        assertComponentEnabled(
                POST_SETUP_TEST_LAUNCHER_COMPONENT,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void testPackageSetup_pairedWithIOS() throws Exception {
        // Paired device OS is IoS
        setPairedDeviceOsType(PAIRED_DEVICE_OS_TYPE_IOS);
        // Setup test package config
        preparePostSetupPackageResources(
                CONFIG_PACKAGE_DISABLE_FOR_IOS, CONFIG_LAUNCHER_DISABLE_FOR_IOS);
        setUpPackageEnabled(TEST_PACKAGE1, true);
        setUpComponentEnabled(POST_SETUP_TEST_LAUNCHER_COMPONENT, true);

        new PostSetupPackageHelper(mContext, mockResources).run();

        assertPackageEnabled(TEST_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        assertComponentEnabled(
                POST_SETUP_TEST_LAUNCHER_COMPONENT,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void testPackageSetup_deviceOSUnknown() throws Exception {
        // Watch is not paired and thus paired device OS is unknown
        setPairedDeviceOsType(PAIRED_DEVICE_OS_TYPE_UNKNOWN);
        // Setup test package config
        preparePostSetupPackageResources(
                CONFIG_PACKAGE_DISABLE_FOR_ANDROID, CONFIG_LAUNCHER_DISABLE_FOR_IOS);
        setUpPackageEnabled(TEST_PACKAGE1, true);
        setUpComponentEnabled(POST_SETUP_TEST_LAUNCHER_COMPONENT, true);

        new PostSetupPackageHelper(mContext, mockResources).run();

        assertPackageEnabled(TEST_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        assertComponentEnabled(
                POST_SETUP_TEST_LAUNCHER_COMPONENT, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
    }

    @Test
    public void testPackageSetup_pairFromAndroidToIOS() throws Exception {
        // Paired device OS is Android
        setPairedDeviceOsType(PAIRED_DEVICE_OS_TYPE_ANDROID);
        preparePostSetupPackageResources(CONFIG_PACKAGE_DISABLE_FOR_IOS);
        setUpPackageEnabled(TEST_PACKAGE1, true);
        // Paired device OS changes to IoS
        setPairedDeviceOsType(PAIRED_DEVICE_OS_TYPE_IOS);

        new PostSetupPackageHelper(mContext, mockResources).run();

        assertPackageEnabled(TEST_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void testPackageSetup_pairFromIOSToAndroid() throws Exception {
        // Paired device OS is Android
        setPairedDeviceOsType(PAIRED_DEVICE_OS_TYPE_IOS);
        preparePostSetupPackageResources(CONFIG_PACKAGE_DISABLE_FOR_ANDROID);
        setUpPackageEnabled(TEST_PACKAGE1, true);
        // Paired device OS changes to IoS
        setPairedDeviceOsType(PAIRED_DEVICE_OS_TYPE_ANDROID);

        new PostSetupPackageHelper(mContext, mockResources).run();

        assertPackageEnabled(TEST_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    private void preparePostSetupPackageResources(String... packageConfigs) {
        when(mockResources.getStringArray(
                com.android.wearable.resources.R.array.config_postSetupPackageConfigList))
                .thenReturn(packageConfigs);
    }

    private void setPairedDeviceOsType(int type) {
        Settings.Global.putInt(mContext.getContentResolver(), PAIRED_DEVICE_OS_TYPE, type);
    }

    private void setUpPackageEnabled(String packageName, boolean enabled)
            throws PackageManager.NameNotFoundException {
        setUpPackage(packageName, null, enabled);
    }

    private void setUpComponentEnabled(String componentNameStr, boolean enabled)
            throws PackageManager.NameNotFoundException {
        ComponentName componentName = ComponentName.unflattenFromString(componentNameStr);
        setUpPackage(componentName.getPackageName(), componentName, enabled);
    }

    public void assertPackageEnabled(String packageName, int enabledState) {
        PackageManager pkgManager = RuntimeEnvironment.application.getPackageManager();
        assertThat(pkgManager.getApplicationEnabledSetting(packageName)).isEqualTo(enabledState);
    }

    public void assertComponentEnabled(String componentNameStr, int enabledState) {
        PackageManager pkgManager = RuntimeEnvironment.application.getPackageManager();
        ComponentName componentName = ComponentName.unflattenFromString(componentNameStr);
        assertThat(pkgManager.getComponentEnabledSetting(componentName)).isEqualTo(enabledState);
    }

    private static void setUpPackage(String packageName, ComponentName cname, boolean enabled) {
        ShadowPackageManager shadowPackageManager =
                shadowOf(RuntimeEnvironment.application.getPackageManager());

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.packageName = packageName;
        packageInfo.applicationInfo.name = "label";
        // if we don't provide a component specifically, enable applies to the package
        packageInfo.applicationInfo.enabled = (cname == null) ? enabled : true;
        // if we do provide a component, configure accordingly
        if (cname != null) {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.enabled = enabled;
            activityInfo.name = cname.getClassName();
            activityInfo.packageName = cname.getPackageName();
            activityInfo.applicationInfo = new ApplicationInfo();
            activityInfo.applicationInfo.packageName = cname.getPackageName();
            activityInfo.applicationInfo.name = cname.getClassName();

            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            List<ResolveInfo> resolveList = new ArrayList(0);
            resolveList.add(resolveInfo);

            shadowPackageManager.setResolveInfosForIntent(
                    new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_LAUNCHER)
                            .setPackage(packageName), resolveList);

            packageInfo.activities = new ActivityInfo[] {activityInfo};
        }
        shadowPackageManager.addPackage(packageInfo);
    }
}