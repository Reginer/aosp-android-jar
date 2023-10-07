package com.android.clockwork.globalactions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SystemTraceActionTest {
    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;

    @Captor
    private ArgumentCaptor<Intent> mIntentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onPress_startsTraceur() {
        GlobalActionsProviderImpl.SystemTraceAction systemTraceAction =
                new GlobalActionsProviderImpl.SystemTraceAction(mContext, mResources);

        systemTraceAction.onPress();

        verify(mContext).startActivity(mIntentCaptor.capture());
        Intent systemTraceIntent = mIntentCaptor.getValue();
        assertEquals(systemTraceIntent.getComponent().getPackageName(), "com.android.traceur");
        assertEquals(systemTraceIntent.getComponent().getClassName(),
                "com.android.traceur.MainActivity");
    }
}
