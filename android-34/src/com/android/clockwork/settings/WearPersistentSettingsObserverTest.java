package com.android.clockwork.settings;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
public final class WearPersistentSettingsObserverTest {
    private static final Uri TEST_URI_A = Settings.Global.getUriFor("key_a");
    private static final Uri TEST_URI_B = Settings.Secure.getUriFor("key_b");

    @Mock Resources mockResources;
    @Mock private WearPersistentSettingsObserver.Observer mockObserverA1;
    @Mock private WearPersistentSettingsObserver.Observer mockObserverA2;
    @Mock private WearPersistentSettingsObserver.Observer mockObserverB;

    private Context mContext;
    private ShadowContentResolver mShadowContentResolver;

    private WearPersistentSettingsObserver mObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mShadowContentResolver = Shadow.extract(mContext.getContentResolver());

        when(mockObserverA1.getUri()).thenReturn(TEST_URI_A);
        when(mockObserverA2.getUri()).thenReturn(TEST_URI_A);
        when(mockObserverB.getUri()).thenReturn(TEST_URI_B);

        mObserver = new WearPersistentSettingsObserver(
                mContext, Set.of(mockObserverA1, mockObserverA2, mockObserverB));
    }

    @Test
    public void testDelivery() {
        mObserver.startObserving();

        fireContentObservers(TEST_URI_A);

        verify(mockObserverA1).onChange();
        verify(mockObserverA2).onChange();
        verify(mockObserverB, never()).onChange();
    }

    @Test
    public void testPreObservingOnChangeRuns() {
        when(mockObserverA1.shouldTriggerObserverOnStart()).thenReturn(true);
        when(mockObserverA2.shouldTriggerObserverOnStart()).thenReturn(false);

        mObserver.startObserving();

        verify(mockObserverA1).onChange();
        verify(mockObserverA2, never()).onChange();
    }

    @Test
    public void testCombinedLocationObserverRegistered() {
        mObserver = new WearPersistentSettingsObserver(mContext, mockResources);

        assertThat(isObserverRegistered(CombinedLocationSettingObserver.class)).isTrue();
    }

    @Test
    public void testPairedDeviceOsTypeObserver_registeredIfPostSetupPackagesAreAvailable() {
        preparePostSetupPackageResources("test_pkg");

        mObserver = new WearPersistentSettingsObserver(mContext, mockResources);

        assertThat(isObserverRegistered(PairedDeviceOsTypeObserver.class)).isTrue();
    }

    @Test
    public void testPairedDeviceOsTypeObserver_notRegisteredIfPostSetupPackagesAreNotAvailable() {
        preparePostSetupPackageResources(new String[] {});

        mObserver = new WearPersistentSettingsObserver(mContext, mockResources);

        assertThat(isObserverRegistered(PairedDeviceOsTypeObserver.class)).isFalse();
    }

    private void fireContentObservers(Uri uri) {
        mShadowContentResolver.getContentObservers(uri).forEach(c -> c.onChange(false));
    }

    private void preparePostSetupPackageResources(String... packageConfigs) {
        when(mockResources.getStringArray(
                com.android.wearable.resources.R.array.config_postSetupPackageConfigList))
                .thenReturn(packageConfigs);
    }

    private boolean isObserverRegistered(Class<?> c) {
        return mObserver.getObservers().stream().anyMatch(o -> o.getClass() == c);
    }
}
