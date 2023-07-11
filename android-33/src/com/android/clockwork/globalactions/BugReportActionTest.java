package com.android.clockwork.globalactions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowUserManager;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

@RunWith(RobolectricTestRunner.class)
public final class BugReportActionTest {

    private Context mContext;
    private ShadowUserManager mShadowUserManager;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
        mShadowUserManager = Shadows.shadowOf(mContext.getSystemService(UserManager.class));
    }

    @Test
    public void onPress_startsBugReportActivityIntent() {
        Context mockContext = mock(Context.class);
        GlobalActionsProviderImpl.BugReportAction bugReportAction =
                new GlobalActionsProviderImpl.BugReportAction(mockContext);
        bugReportAction.onPress();
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(mockContext).startActivity(intentCaptor.capture());
        Intent bugReportIntent = intentCaptor.getValue();
        String packageName = bugReportIntent.getComponent().getPackageName();
        String className = bugReportIntent.getComponent().getClassName();

        assertEquals(packageName, "com.google.android.wearable.app");
        assertEquals(className, "com.google.android.clockwork.wcs.bugreport.TakeReportActivity");
        assertEquals(bugReportIntent.getFlags(),
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void isEnabled_userLocked_false() {
        GlobalActionsProviderImpl.BugReportAction bugReportAction =
                new GlobalActionsProviderImpl.BugReportAction(mContext);

        mShadowUserManager.setUserUnlocked(false);

        assertFalse(bugReportAction.isEnabled());
    }

    @Test
    public void isEnabled_userUnlocked_true() {
        GlobalActionsProviderImpl.BugReportAction bugReportAction =
                new GlobalActionsProviderImpl.BugReportAction(mContext);

        mShadowUserManager.setUserUnlocked(true);

        assertTrue(bugReportAction.isEnabled());
    }
}
