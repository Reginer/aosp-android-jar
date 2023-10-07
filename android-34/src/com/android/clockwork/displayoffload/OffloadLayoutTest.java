package com.android.clockwork.displayoffload;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_DYNAMIC_TEXT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_MINUTES_VALID;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_STATIC_TEXT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_TRANSLATION_GROUP;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_EMPTY_LAYOUT;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.clockwork.ambient.offload.types.BindableFloat;
import com.google.android.clockwork.ambient.offload.types.BitmapResource;
import com.google.android.clockwork.ambient.offload.types.DynamicTextResource;
import com.google.android.clockwork.ambient.offload.types.StaticTextResource;
import com.google.android.clockwork.ambient.offload.types.TextParam;
import com.google.android.clockwork.ambient.offload.types.TranslationGroup;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Tests for {@link com.google.android.clockwork.displayoffload.HalAdapter}.
 */
@RunWith(AndroidJUnit4.class)
public class OffloadLayoutTest {
    private static final StaticTextResource mSampleStaticText = new StaticTextResource();
    private static final DynamicTextResource mSampleDynamicText = new DynamicTextResource();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final HalTypeConverter mHalTypeConverter = spy(new HalTypeConverterV1(mContext));
    private final HalTypeConverter.HalTypeConverterSupplier mHalTypeConverterSupplier =
            () -> mHalTypeConverter;
    @Mock
    HalAdapter mHalAdapter;
    @Mock
    HalResourceStore mHalResourceStore;
    @Mock
    TextPreprocessor mTextPreprocessor;

    @BeforeClass
    public static void onlyOnce() {
        mSampleStaticText.id = 42;
        mSampleStaticText.value = "Lorem ipsum dolor sit amet";
        mSampleStaticText.textParam = new TextParam();

        mSampleDynamicText.id = 43;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBaseOffloadLayoutOpenClose() {
        OffloadLayout l = newTestOffloadLayout();
        assertThat(l.isOpen()).isFalse();
        l.open();
        assertThat(l.isOpen()).isTrue();
        l.close();
        assertThat(l.isOpen()).isFalse();
    }

    @Test
    public void testBaseOffloadLayoutIsEmpty() {
        OffloadLayout l = newTestOffloadLayout();

        when(mHalResourceStore.isEmpty()).thenReturn(false);
        assertThat(l.isEmpty()).isFalse();
        when(mHalResourceStore.isEmpty()).thenReturn(true);
        assertThat(l.isEmpty()).isTrue();
    }

    @Test
    public void testBaseOffloadLayoutUid() {
        final int uid = 42;
        OffloadLayout l = newTestOffloadLayout(uid);
        assertThat(l.getUid()).isEqualTo(uid);
    }

    @Test
    public void testBaseOffloadLayoutMinutesValid() {
        int minutesValid = 42;
        OffloadLayout l = newTestOffloadLayout();
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_MINUTES_VALID, minutesValid);
        l.updateFromBundle(bundle);
        assertThat(l.getMinutesValid()).isEqualTo(minutesValid);
    }

    @Test
    public void testBaseOffloadLayoutMarkHalResourcesDirty() {
        OffloadLayout l = newTestOffloadLayout();
        l.markHalResourcesDirty();
        verify(mHalResourceStore).markAllDirty();
    }

    @Test
    public void testBaseOffloadLayoutSendToHal() throws DisplayOffloadException {
        OffloadLayout l = newTestOffloadLayout();
        l.sendToHal(mHalAdapter);
        verify(mHalResourceStore).sendToHal(eq(mHalAdapter));
    }

    @Test
    public void testBaseOffloadLayoutSendToHal_shouldResetOnDirtyOnly()
            throws DisplayOffloadException {
        OffloadLayout l = newTestOffloadLayout();
        l.markHalResourcesDirty();
        l.sendToHal(mHalAdapter);
        verify(mHalAdapter).resetResource();
        verify(mHalResourceStore).sendToHal(eq(mHalAdapter));
        clearInvocations(mHalAdapter);
        clearInvocations(mHalResourceStore);
        l.sendToHal(mHalAdapter);
        verify(mHalAdapter, never()).resetResource();
        verify(mHalResourceStore).sendToHal(eq(mHalAdapter));
    }

    @Test
    public void testBaseOffloadLayoutIsWaitingDoze_shouldResetOnSendOrDirty()
            throws DisplayOffloadException {
        OffloadLayout l = newTestOffloadLayout();
        // Mark as waiting
        l.markWaitingDoze();
        assertThat(l.isWaitingDoze()).isTrue();

        l.markHalResourcesDirty();
        assertThat(l.isWaitingDoze()).isFalse();

        // Mark as waiting
        l.markWaitingDoze();
        assertThat(l.isWaitingDoze()).isTrue();

        l.sendToHal(mHalAdapter);
        assertThat(l.isWaitingDoze()).isFalse();
    }

    @Test
    public void testBaseOffloadLayoutValidateHalResource_shouldCallTextPreprocessor()
            throws DisplayOffloadException {
        OffloadLayout l = newTestOffloadLayout();
        l.validateHalResource();
        verify(mTextPreprocessor).processTtfFontSubsetting(eq(mHalResourceStore));
        verify(mTextPreprocessor).cleanup();
        verify(mHalResourceStore).generateSendOrder();
    }

    @Test
    public void testBaseOffloadLayoutValidateHalResource_shouldRethrowAndCleanupOnError()
            throws DisplayOffloadException {
        OffloadLayout l = spy(newTestOffloadLayout());
        DisplayOffloadException exception = new DisplayOffloadException(0, "TEST");
        doThrow(exception).when(l).buildHalResourceHelper();
        try {
            l.validateHalResource();
        } catch (DisplayOffloadException e) {
            assertThat(e).isEqualTo(exception);
        }
        verify(mTextPreprocessor).cleanup();
    }

    @Test
    public void testAddStatusBar() throws DisplayOffloadException {
        TranslationGroup statusBarTg = newTranslationGroup();
        BitmapResource statusBarRes = new BitmapResource();

        List<ResourceObject> fakeHalTranslationGroupResourceList = Collections.singletonList(
                ResourceObject.of(statusBarTg.id, new Object()));
        List<ResourceObject> fakeHalBitmapResourceList =
                Collections.singletonList(ResourceObject.of(statusBarRes.id, new Object()));

        doReturn(fakeHalTranslationGroupResourceList)
                .when(mHalTypeConverter).toHalObject(eq(statusBarTg));
        doReturn(fakeHalBitmapResourceList).when(mHalTypeConverter).toHalObject(eq(statusBarRes));

        OffloadLayout l = newTestOffloadLayout();
        l.addStatusBar(statusBarTg, statusBarRes);
        l.validateHalResource();

        verify(mHalResourceStore).addReplaceResource(eq(fakeHalTranslationGroupResourceList));
        verify(mHalResourceStore).addReplaceResource(eq(fakeHalBitmapResourceList));
    }

    private <T, R> Answer<R> argumentCaptorAssertEquals(InvocationOnMock invocation, T target) {
        Object o = invocation.getArgument(1);
        assertThat(o).isEqualTo(target);
        return null;
    }

    @Test
    public void testUpdateFromBundle_shouldUnpackAndRegisterTexts() throws DisplayOffloadException {
        OffloadLayout l = newTestOffloadLayout();

        // Set up assertions first
        doAnswer((invocation) -> argumentCaptorAssertEquals(invocation,
                Collections.singletonList(mSampleStaticText)))
                .when(mTextPreprocessor)
                .addStaticText(eq(mHalResourceStore), anyList());

        doAnswer((invocation) -> argumentCaptorAssertEquals(invocation,
                Collections.singletonList(mSampleDynamicText)))
                .when(mTextPreprocessor)
                .addDynamicText(eq(mHalResourceStore), anyList());

        l.updateFromBundle(newStaticDynamicTextsBundle());
        l.validateHalResource();
    }

    @Test
    public void testBrightnessOffloadAddStatusBar_shouldIgnore() throws DisplayOffloadException {
        OffloadLayout l = newTestOffloadLayout();
        l.updateFromBundle(newBrightnessOffloadBundle());
        verify(mHalTypeConverter, never()).toHalBitmapResource(any());
    }

    @Test
    public void testBrightnessOffloadUpdateFromBundle_shouldDetectEmptyResource() {
        OffloadLayout l = newTestOffloadLayout();
        l.updateFromBundle(newBrightnessOffloadBundle());
        assertThat(l.isBrightnessOffload()).isTrue();
    }

    @Test
    public void testBrightnessOffloadValidateHalResource() throws DisplayOffloadException {
        OffloadLayout l = newTestOffloadLayout();
        l.updateFromBundle(newBrightnessOffloadBundle());
        l.validateHalResource();
        verify(mHalResourceStore).clear();
        verify(mHalTypeConverter).addEmptyTranslationGroup(eq(mHalResourceStore));
    }

    OffloadLayout newTestOffloadLayout() {
        return newTestOffloadLayout(0);
    }

    OffloadLayout newTestOffloadLayout(int uid) {
        return new OffloadLayout(uid, mHalTypeConverterSupplier, mHalResourceStore,
                mTextPreprocessor);
    }

    Bundle newBrightnessOffloadBundle() {
        Bundle bundle = new Bundle();
        TranslationGroup group = newTranslationGroup();
        group.id = RESOURCE_ID_EMPTY_LAYOUT;
        bundle.putParcelableArrayList(FIELD_TRANSLATION_GROUP,
                new ArrayList<>(Collections.singleton(group)));
        return bundle;
    }

    Bundle newStaticDynamicTextsBundle() {
        Bundle bundle = new Bundle();
        TranslationGroup group = newTranslationGroup();
        group.id = 0;
        group.contents = new int[]{mSampleStaticText.id, mSampleDynamicText.id};
        bundle.putParcelableArrayList(FIELD_STATIC_TEXT,
                new ArrayList<>(Collections.singleton(mSampleStaticText)));
        bundle.putParcelableArrayList(FIELD_DYNAMIC_TEXT,
                new ArrayList<>(Collections.singleton(mSampleDynamicText)));
        bundle.putParcelableArrayList(FIELD_TRANSLATION_GROUP,
                new ArrayList<>(Collections.singleton(group)));
        return bundle;
    }

    TranslationGroup newTranslationGroup() {
        TranslationGroup translationGroup = new TranslationGroup();
        translationGroup.offsetX = new BindableFloat();
        translationGroup.offsetY = new BindableFloat();
        return translationGroup;
    }
}

