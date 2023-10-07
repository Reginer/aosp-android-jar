package com.android.clockwork.settings;

import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import android.provider.Settings;
import com.android.clockwork.setup.PostSetupPackageHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class PairedDeviceOsTypeObserverTest {
    @Mock private PostSetupPackageHelper mockPostSetupPackageHelper;

    private PairedDeviceOsTypeObserver mObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mObserver = new PairedDeviceOsTypeObserver(mockPostSetupPackageHelper);
    }

    @Test
    public void testUri() {
        assertThat(mObserver.getUri()).isEqualTo(
                Settings.Global.getUriFor(PAIRED_DEVICE_OS_TYPE));
    }

    @Test
    public void testOnChange() {
        mObserver.onChange();

        verify(mockPostSetupPackageHelper).run();
    }

    @Test
    public void testShouldTriggerObserverOnStart() {
        assertThat(mObserver.shouldTriggerObserverOnStart()).isTrue();
    }
}
