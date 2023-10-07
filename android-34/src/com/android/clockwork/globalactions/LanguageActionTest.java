package com.android.clockwork.globalactions;

import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertEquals;

import android.content.res.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import android.content.Context;
import android.content.Intent;

public class LanguageActionTest {

    private GlobalActionsProviderImpl.LanguageAction mLanguageAction;

    private static final String LOCALE_PACKAGE_NAME =
            "com.google.android.wearable.setupwizard";
    private static final String LOCALE_ACTIVITY_NAME =
            "com.google.android.wearable.setupwizard.steps.locale.LocaleActivity";

    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;
    @Captor
    private ArgumentCaptor<Intent> mIntentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLanguageAction = new GlobalActionsProviderImpl.LanguageAction(mContext, mResources);
    }

    @Test
    public void onPress_startsLanguageActivityIntent() {
        mLanguageAction.onPress();

        verify(mContext).startActivity(mIntentCaptor.capture());
        Intent bugReportIntent = mIntentCaptor.getValue();

        String packageName = bugReportIntent.getComponent().getPackageName();
        String className = bugReportIntent.getComponent().getClassName();
        assertEquals(packageName, LOCALE_PACKAGE_NAME);
        assertEquals(className, LOCALE_ACTIVITY_NAME);
        assertEquals(bugReportIntent.getFlags(), Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

}
