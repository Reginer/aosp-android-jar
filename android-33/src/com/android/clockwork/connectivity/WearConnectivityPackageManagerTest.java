package com.android.clockwork.connectivity;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.ConditionVariable;
import android.os.UserHandle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.util.IndentingPrintWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class WearConnectivityPackageManagerTest {
    @Mock IndentingPrintWriter mMockIndentingPrintWriter;

    private static final int TIMEOUT_MS = 1000;
    private static final String[] CELLULAR_PACKAGES = {"com.test.cellular.one",
            "com.test.cellular.two", "com.test.cellular.three", "com.test.common.one",
            "com.test.common.two"};
    private static final String[] WIFI_PACKAGES = {"com.test.wifi.one", "com.test.wifi.two",
            "com.test.common.one", "com.test.common.two"};
    private static final String[] SUPPRESSED_CELLULAR_PACKAGES = {
            "com.test.cellular.suppressed.one", "com.test.cellular.suppressed.two"};
    private String[] mCombinedPackages;
    private Context mContext;
    private WearConnectivityPackageManager mConnectivityPackagemanager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        Set<String> mCombinedPackagesSet = new HashSet(Arrays.asList(CELLULAR_PACKAGES));
        mCombinedPackagesSet.addAll(Arrays.asList(WIFI_PACKAGES));
        mCombinedPackages = mCombinedPackagesSet.toArray(new String[0]);
        shadowOf(mContext.getPackageManager()).addPackage(WearResourceUtil.WEAR_RESOURCE_PACKAGE);
        Resources resources = Resources.create(
            mContext, CELLULAR_PACKAGES, WIFI_PACKAGES, SUPPRESSED_CELLULAR_PACKAGES);
        ShadowPackageManager.resources.put(WearResourceUtil.WEAR_RESOURCE_PACKAGE, resources);
        installPackages(mCombinedPackages);
        // sendBroadcastAsUser() is not shadowed and causes the test to hang. Add a wrapper here
        // that validates the user and calls sendBroadcast().
        Context testContext = new ContextWrapper(mContext) {
            @Override
            public void sendBroadcastAsUser(Intent intent, UserHandle user) {
                assertThat(user).isEqualTo(UserHandle.ALL);
                getApplicationContext().sendBroadcast(intent);
            }
        };
        mConnectivityPackagemanager = new WearConnectivityPackageManager(testContext);
    }

    @Test
    public void testCellularPackagesEnabled() {
        ConditionVariable conditionVariable = getIntentsReceivedCondition(CELLULAR_PACKAGES);
        mConnectivityPackagemanager.onCellularRadioState(true);
        verifyPackageState(CELLULAR_PACKAGES, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        assertThat(conditionVariable.block(TIMEOUT_MS)).isTrue();
    }

    @Test
    public void testWifiPackagesEnabled() {
        ConditionVariable conditionVariable = getIntentsReceivedCondition(WIFI_PACKAGES);
        mConnectivityPackagemanager.onWifiRadioState(true);
        verifyPackageState(WIFI_PACKAGES, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        assertThat(conditionVariable.block(TIMEOUT_MS)).isTrue();
    }

    @Test
    public void testCellularAndWifiPackagesEnabledThenDisabled() {
        ArrayList<String> orderedPackages = new ArrayList<String>();
        orderedPackages.addAll(Arrays.asList(WIFI_PACKAGES));
        for (String app : CELLULAR_PACKAGES) {
            if (!orderedPackages.contains(app)) {
                orderedPackages.add(app);
            }
        }
        ConditionVariable cv = getIntentsReceivedCondition(orderedPackages.toArray(new String[0]));
        mConnectivityPackagemanager.onWifiRadioState(true);
        mConnectivityPackagemanager.onCellularRadioState(true);
        verifyPackageState(mCombinedPackages, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        assertThat(cv.block(TIMEOUT_MS)).isTrue();

        mConnectivityPackagemanager.onWifiRadioState(false);
        mConnectivityPackagemanager.onCellularRadioState(false);
        verifyPackageState(mCombinedPackages, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void testDump() {
        mConnectivityPackagemanager.dump(mMockIndentingPrintWriter);
        verify(mMockIndentingPrintWriter, atLeast(1)).printPair(anyString(), anyInt());
    }

    private void installPackages(String[] allPackages) {
        for (String packageName : allPackages) {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.packageName = packageName;
            shadowOf(mContext.getPackageManager()).installPackage(packageInfo);
        }
    }

    private void verifyPackageState(String[] packageNames, int state) {
        for (String app : packageNames) {
            assertThat(mContext.getPackageManager()
                    .getApplicationEnabledSetting(app))
                    .isEqualTo(state);
        }
    }

    private ConditionVariable getIntentsReceivedCondition(String[] apps) {
        final ConditionVariable conditionVariable = new ConditionVariable();
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    private int mAppIndex = 0;

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        assertThat(intent.getAction())
                                .isEqualTo(
                                        WearConnectivityPackageManager.ACTION_WEAR_APP_ENABLED);
                        assertThat(apps[mAppIndex]).isEqualTo(intent.getPackage());
                        if (++mAppIndex == apps.length) {
                            conditionVariable.open();
                            mContext.unregisterReceiver(this);
                        }
                    }
                },
                new IntentFilter(WearConnectivityPackageManager.ACTION_WEAR_APP_ENABLED));
        return conditionVariable;
    }

    @Test
    public void testSuppressedCellularRequestors() {
        for (String name : SUPPRESSED_CELLULAR_PACKAGES) {
          assertThat(mConnectivityPackagemanager.isSuppressedCellularRequestor(name)).isTrue();
        }
    }

    /**
     * Constructed resources to inject into the ShadowPackageManager.
     */
    public static class Resources extends android.content.res.Resources {

        private final String[] mCellularPackages;
        private final String[] mWifiPackages;
        private final String[] mSuppressedCellularRequestors;

        private Resources(
                String[] cellularPackages,
                String[] wifiPackages,
                String[] suppressedCellularRequestors,
                AssetManager assets,
                DisplayMetrics metrics,
                Configuration config) {
            super(assets, metrics, config);
            mCellularPackages = cellularPackages;
            mWifiPackages = wifiPackages;
            mSuppressedCellularRequestors = suppressedCellularRequestors;
        }

        public static Resources create(
                Context context,
                String[] cellularPackages,
                String[] wifiPackages,
                String[] suppressedCellularRequestors) {
            android.content.res.Resources res = context.getResources();
            return new Resources(
                    cellularPackages,
                    wifiPackages,
                    suppressedCellularRequestors,
                    res.getAssets(),
                    res.getDisplayMetrics(),
                    res.getConfiguration());
        }

        @NonNull
        @Override
        public String[] getStringArray(int id) {
            if (id == com.android.clockwork.R.array.config_wearCellularEnabledPackages) {
                ArrayList<String> cellularPackagesList = new ArrayList<>();
                cellularPackagesList.addAll(Arrays.asList(mCellularPackages));
                cellularPackagesList.add("cellular_not_installed");
                cellularPackagesList.add("common_not_installed");
                return cellularPackagesList.toArray(new String[0]);
            }
            if (id == com.android.clockwork.R.array.config_wearWifiEnabledPackages) {
                ArrayList<String> wifiPackagesList = new ArrayList<>();
                wifiPackagesList.addAll(Arrays.asList(mWifiPackages));
                wifiPackagesList.add("wifi_not_installed");
                wifiPackagesList.add("common_not_installed");
                return wifiPackagesList.toArray(new String[0]);
            }
            if (id == com.android.clockwork.R.array.config_wearSuppressedCellularRequestors) {
                return mSuppressedCellularRequestors;
            }
            throw new NotFoundException();
        }
    }
}
