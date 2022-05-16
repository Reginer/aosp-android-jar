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

package com.android.setupwizardlib.items;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.items.ButtonItem.OnClickListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
public class ButtonItemTest {

  private ViewGroup parent;
  private Context context;

  @Before
  public void setUp() {
    context = new ContextThemeWrapper(application, R.style.SuwThemeGlif_Light);
    parent = new LinearLayout(context);
  }

  @Test
  public void testDefaultItem() {
    ButtonItem item = new ButtonItem();

    assertThat(item.isEnabled()).named("enabled").isTrue();
    assertThat(item.getCount()).named("count").isEqualTo(0);
    assertThat(item.getLayoutResource()).named("layout resource").isEqualTo(0);
    assertThat(item.getTheme()).named("theme").isEqualTo(R.style.SuwButtonItem);
    assertThat(item.getText()).named("text").isNull();
  }

  @Test
  public void testOnBindView() {
    ButtonItem item = new ButtonItem();

    try {
      item.onBindView(new View(context));
      fail("Calling onBindView on ButtonItem should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // pass
    }
  }

  @Test
  public void testCreateButton() {
    TestButtonItem item = new TestButtonItem();
    final Button button = item.createButton(parent);

    assertThat(button.isEnabled()).named("enabled").isTrue();
    assertThat(button.getText().toString()).isEmpty();
  }

  @Test
  public void testButtonItemSetsItsId() {
    TestButtonItem item = new TestButtonItem();
    final int id = 12345;
    item.setId(id);

    assertWithMessage("Button's id should be set")
        .that(item.createButton(parent).getId())
        .isEqualTo(id);
  }

  @Test
  public void testCreateButtonTwice() {
    TestButtonItem item = new TestButtonItem();
    final Button button = item.createButton(parent);

    FrameLayout frameLayout = new FrameLayout(context);
    frameLayout.addView(button);

    final Button button2 = item.createButton(parent);
    assertWithMessage("createButton should be reused").that(button2).isSameAs(button);
    assertWithMessage("Should be removed from parent after createButton")
        .that(button2.getParent())
        .isNull();
  }

  @Test
  public void testSetEnabledTrue() {
    TestButtonItem item = new TestButtonItem();
    item.setEnabled(true);

    final Button button = item.createButton(parent);
    assertWithMessage("ButtonItem should be enabled").that(item.isEnabled()).isTrue();
    assertWithMessage("Button should be enabled").that(button.isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabledFalse() {
    TestButtonItem item = new TestButtonItem();
    item.setEnabled(false);

    final Button button = item.createButton(parent);
    assertWithMessage("ButtonItem should be disabled").that(item.isEnabled()).isFalse();
    assertWithMessage("Button should be disabled").that(button.isEnabled()).isFalse();
  }

  @Test
  public void testSetText() {
    TestButtonItem item = new TestButtonItem();
    item.setText("lorem ipsum");

    final Button button = item.createButton(parent);
    assertWithMessage("ButtonItem text should be \"lorem ipsum\"")
        .that(item.getText().toString())
        .isEqualTo("lorem ipsum");
    assertWithMessage("Button text should be \"lorem ipsum\"")
        .that(button.getText().toString())
        .isEqualTo("lorem ipsum");
  }

  @Test
  public void testSetTheme() {
    TestButtonItem item = new TestButtonItem();
    item.setTheme(R.style.SuwButtonItem_Colored);

    final Button button = item.createButton(parent);
    assertWithMessage("ButtonItem theme should be SuwButtonItem.Colored")
        .that(item.getTheme())
        .isEqualTo(R.style.SuwButtonItem_Colored);
    assertThat(button.getContext().getTheme()).isNotNull();
  }

  @Test
  public void testOnClickListener() {
    TestButtonItem item = new TestButtonItem();
    final OnClickListener listener = mock(OnClickListener.class);
    item.setOnClickListener(listener);

    verify(listener, never()).onClick(any(ButtonItem.class));

    final Button button = item.createButton(parent);
    button.performClick();

    verify(listener).onClick(same(item));
  }

  private static class TestButtonItem extends ButtonItem {

    @Override
    public Button createButton(ViewGroup parent) {
      // Make this method public for testing
      return super.createButton(parent);
    }
  }
}
