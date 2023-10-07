package com.android.clockwork.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class RadioTogglerTest {

    /**
     * A mock Radio implementation is favored here because it leads to clearer test code
     * that more closely mirrors real-world usage.  A Mockito version of Radio would
     * result in some weird mock method declarations in order for the RadioToggler to
     * properly interact with the mock radio.
     */
    private class MockRadio implements RadioToggler.Radio {

        private volatile boolean enabled = false;
        volatile int numSetEnables = 0;

        @Override
        public String logTag() {
            return "MockRadio";
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            numSetEnables++;
        }

        @Override
        public boolean getEnabled() {
            return enabled;
        }
    }


    @Mock PartialWakeLock mockWakeLock;
    MockRadio mockRadio;
    RadioToggler radioToggler;

    ShadowLooper shadowLooper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mockRadio = new MockRadio();
        radioToggler = new RadioToggler(mockRadio, mockWakeLock, -1);

        shadowLooper = shadowOf(radioToggler.mHandlerThread.getLooper());
        shadowLooper.pause();
    }

    @Test
    public void testGetRadioEnabled() {
        // the value here should match mockRadio's value before construction
        Assert.assertFalse(radioToggler.getRadioEnabled());

        // changing the state of the radio should not directly affect RadioToggler
        mockRadio.setEnabled(true);
        Assert.assertFalse(radioToggler.getRadioEnabled());
    }

    @Test
    public void testRefreshRadioState() {
        mockRadio.setEnabled(true);
        radioToggler.refreshRadioState();
        shadowLooper.runToEndOfTasks();
        Assert.assertTrue(radioToggler.getRadioEnabled());

        mockRadio.setEnabled(false);
        radioToggler.refreshRadioState();
        shadowLooper.runToEndOfTasks();
        Assert.assertFalse(radioToggler.getRadioEnabled());
    }

    /**
     * Used by subsequent tests, this helper method assumes testRefreshRadioState passes.
     */
    private void setRadioInitialState(boolean enabled) {
        mockRadio.setEnabled(enabled);
        mockRadio.numSetEnables = 0;
        radioToggler.refreshRadioState();
        shadowLooper.runToEndOfTasks();
    }

    @Test
    public void testToggleRadioOn() {
        setRadioInitialState(false);

        radioToggler.toggleRadio(true);
        shadowLooper.runToEndOfTasks();

        verify(mockWakeLock).acquire();
        verify(mockWakeLock).release();

        Assert.assertTrue(radioToggler.getRadioEnabled());
        Assert.assertTrue(mockRadio.getEnabled());
        Assert.assertEquals(1, mockRadio.numSetEnables);
    }

    @Test
    public void testToggleRadioOff() {
        setRadioInitialState(true);

        radioToggler.toggleRadio(false);
        shadowLooper.runToEndOfTasks();

        verify(mockWakeLock).acquire();
        verify(mockWakeLock).release();
        Assert.assertFalse(radioToggler.getRadioEnabled());
        Assert.assertFalse(mockRadio.getEnabled());
        Assert.assertEquals(1, mockRadio.numSetEnables);
    }

    @Test
    public void testToggleRadioOnWhenAlreadyOn() {
        setRadioInitialState(true);

        radioToggler.toggleRadio(true);
        shadowLooper.runToEndOfTasks();

        verifyNoMoreInteractions(mockWakeLock);

        Assert.assertTrue(radioToggler.getRadioEnabled());
        Assert.assertTrue(mockRadio.getEnabled());
        Assert.assertEquals(0, mockRadio.numSetEnables);
    }

    @Test
    public void testToggleRadioOffWhenAlreadyOff() {
        setRadioInitialState(false);

        radioToggler.toggleRadio(false);
        shadowLooper.runToEndOfTasks();

        verifyNoMoreInteractions(mockWakeLock);

        Assert.assertFalse(radioToggler.getRadioEnabled());
        Assert.assertFalse(mockRadio.getEnabled());
        Assert.assertEquals(0, mockRadio.numSetEnables);
    }

    @Test
    public void testMultipleTogglesLastOneWins() {
        setRadioInitialState(true);

        radioToggler.toggleRadio(false);
        radioToggler.toggleRadio(false);
        radioToggler.toggleRadio(true);
        radioToggler.toggleRadio(false);
        radioToggler.toggleRadio(true);
        radioToggler.toggleRadio(true);
        radioToggler.toggleRadio(false);
        shadowLooper.runToEndOfTasks();

        verify(mockWakeLock).acquire();
        verify(mockWakeLock).release();

        Assert.assertFalse(radioToggler.getRadioEnabled());
        Assert.assertFalse(mockRadio.getEnabled());
        Assert.assertEquals(1, mockRadio.numSetEnables);
    }

    @Test
    public void testMultipleTogglesToSameState() {
        setRadioInitialState(true);

        radioToggler.toggleRadio(false);
        radioToggler.toggleRadio(false);
        radioToggler.toggleRadio(true);
        radioToggler.toggleRadio(false);
        radioToggler.toggleRadio(true);
        radioToggler.toggleRadio(true);
        radioToggler.toggleRadio(false);
        radioToggler.toggleRadio(true);
        shadowLooper.runToEndOfTasks();

        verifyNoMoreInteractions(mockWakeLock);

        Assert.assertTrue(radioToggler.getRadioEnabled());
        Assert.assertTrue(mockRadio.getEnabled());
        Assert.assertEquals(0, mockRadio.numSetEnables);
    }
}
