/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.setupwizardlib.view;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Annotation;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.MotionEvent;
import com.android.setupwizardlib.span.LinkSpan;
import com.android.setupwizardlib.span.LinkSpan.OnLinkClickListener;
import com.android.setupwizardlib.view.TouchableMovementMethod.TouchableLinkMovementMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
public class RichTextViewTest {

  @Test
  public void testLinkAnnotation() {
    Annotation link = new Annotation("link", "foobar");
    SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
    ssb.setSpan(link, 1, 2, 0 /* flags */);

    RichTextView textView = new RichTextView(application);
    textView.setText(ssb);

    final CharSequence text = textView.getText();
    assertThat(text).isInstanceOf(Spanned.class);

    assertThat(textView.getMovementMethod()).isInstanceOf(TouchableLinkMovementMethod.class);

    Object[] spans = ((Spanned) text).getSpans(0, text.length(), Annotation.class);
    assertThat(spans).isEmpty();

    spans = ((Spanned) text).getSpans(0, text.length(), LinkSpan.class);
    assertThat(spans).hasLength(1);
    assertThat(spans[0]).isInstanceOf(LinkSpan.class);
    assertWithMessage("The LinkSpan should have id \"foobar\"")
        .that(((LinkSpan) spans[0]).getId())
        .isEqualTo("foobar");
  }

  @Test
  public void testOnLinkClickListener() {
    Annotation link = new Annotation("link", "foobar");
    SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
    ssb.setSpan(link, 1, 2, 0 /* flags */);

    RichTextView textView = new RichTextView(application);
    textView.setText(ssb);

    OnLinkClickListener listener = mock(OnLinkClickListener.class);
    textView.setOnLinkClickListener(listener);

    assertThat(textView.getOnLinkClickListener()).isSameAs(listener);

    CharSequence text = textView.getText();
    LinkSpan[] spans = ((Spanned) text).getSpans(0, text.length(), LinkSpan.class);
    spans[0].onClick(textView);

    verify(listener).onLinkClick(eq(spans[0]));
  }

  @Test
  public void testLegacyContextOnClickListener() {
    // Click listener implemented by context should still be invoked for compatibility.
    Annotation link = new Annotation("link", "foobar");
    SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
    ssb.setSpan(link, 1, 2, 0 /* flags */);

    TestContext context = new TestContext(application);
    context.delegate = mock(LinkSpan.OnClickListener.class);
    RichTextView textView = new RichTextView(context);
    textView.setText(ssb);

    CharSequence text = textView.getText();
    LinkSpan[] spans = ((Spanned) text).getSpans(0, text.length(), LinkSpan.class);
    spans[0].onClick(textView);

    verify(context.delegate).onClick(eq(spans[0]));
  }

  @Test
  public void onTouchEvent_clickOnLinks_shouldReturnTrue() {
    Annotation link = new Annotation("link", "foobar");
    SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
    ssb.setSpan(link, 0, 2, 0 /* flags */);

    RichTextView textView = new RichTextView(application);
    textView.setText(ssb);

    TouchableLinkMovementMethod mockMovementMethod = mock(TouchableLinkMovementMethod.class);
    textView.setMovementMethod(mockMovementMethod);

    MotionEvent motionEvent = MotionEvent.obtain(123, 22, MotionEvent.ACTION_DOWN, 0, 0, 0);
    doReturn(motionEvent).when(mockMovementMethod).getLastTouchEvent();
    doReturn(true).when(mockMovementMethod).isLastTouchEventHandled();
    assertThat(textView.onTouchEvent(motionEvent)).isTrue();
  }

  @Test
  public void onTouchEvent_clickOutsideLinks_shouldReturnFalse() {
    Annotation link = new Annotation("link", "foobar");
    SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
    ssb.setSpan(link, 0, 2, 0 /* flags */);

    RichTextView textView = new RichTextView(application);
    textView.setText(ssb);

    TouchableLinkMovementMethod mockMovementMethod = mock(TouchableLinkMovementMethod.class);
    textView.setMovementMethod(mockMovementMethod);

    MotionEvent motionEvent = MotionEvent.obtain(123, 22, MotionEvent.ACTION_DOWN, 0, 0, 0);
    doReturn(motionEvent).when(mockMovementMethod).getLastTouchEvent();
    doReturn(false).when(mockMovementMethod).isLastTouchEventHandled();
    assertThat(textView.onTouchEvent(motionEvent)).isFalse();
  }

  @Test
  public void testTextStyle() {
    Annotation link = new Annotation("textAppearance", "foobar");
    SpannableStringBuilder ssb = new SpannableStringBuilder("Hello world");
    ssb.setSpan(link, 1, 2, 0 /* flags */);

    RichTextView textView = new RichTextView(application);
    textView.setText(ssb);

    final CharSequence text = textView.getText();
    assertThat(text).isInstanceOf(Spanned.class);

    Object[] spans = ((Spanned) text).getSpans(0, text.length(), Annotation.class);
    assertThat(spans).isEmpty();

    spans = ((Spanned) text).getSpans(0, text.length(), TextAppearanceSpan.class);
    assertThat(spans).hasLength(1);
    assertThat(spans[0]).isInstanceOf(TextAppearanceSpan.class);
  }

  @Test
  public void testTextContainingLinksAreFocusable() {
    Annotation testLink = new Annotation("link", "value");
    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("Linked");
    spannableStringBuilder.setSpan(testLink, 0, 3, 0);

    RichTextView view = new RichTextView(application);
    view.setText(spannableStringBuilder);

    assertThat(view.isFocusable()).named("view focusable").isTrue();
  }

  @SuppressLint("SetTextI18n") // It's OK. This is just a test.
  @Test
  public void testTextContainingNoLinksAreNotFocusable() {
    RichTextView textView = new RichTextView(application);
    textView.setText("Thou shall not be focusable!");

    assertThat(textView.isFocusable()).named("view focusable").isFalse();
  }

  // Based on the text contents of the text view, the "focusable" property of the element
  // should also be automatically changed.
  @SuppressLint("SetTextI18n") // It's OK. This is just a test.
  @Test
  public void testRichTextViewFocusChangesWithTextChange() {
    RichTextView textView = new RichTextView(application);
    textView.setText("Thou shall not be focusable!");

    assertThat(textView.isFocusable()).isFalse();
    assertThat(textView.isFocusableInTouchMode()).isFalse();

    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("I am focusable");
    spannableStringBuilder.setSpan(new Annotation("link", "focus:on_me"), 0, 1, 0);
    textView.setText(spannableStringBuilder);
    assertThat(textView.isFocusable()).isTrue();
    if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
      assertThat(textView.isFocusableInTouchMode()).isTrue();
      assertThat(textView.getRevealOnFocusHint()).isFalse();
    } else {
      assertThat(textView.isFocusableInTouchMode()).isFalse();
    }
  }

  public static class TestContext extends ContextWrapper implements LinkSpan.OnClickListener {

    LinkSpan.OnClickListener delegate;

    public TestContext(Context base) {
      super(base);
    }

    @Override
    public void onClick(LinkSpan span) {
      if (delegate != null) {
        delegate.onClick(span);
      }
    }
  }
}
