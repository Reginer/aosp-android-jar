package com.android.clockwork.systemstatedisplay;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SystemStateViewHolderTest {
  private static final int TEST_SCREEN_WIDTH = 100;

  private SystemStateViewHolder mStateView;
  private LinearLayout mLinearLayout;
  private TextView mTextView;

  @Before
  public void setUp() {
    Context context = RuntimeEnvironment.application;
    mLinearLayout = new LinearLayout(context);
    mTextView = new TextView(context);
    mStateView = new SystemStateViewHolder(mLinearLayout, mTextView, TEST_SCREEN_WIDTH);
  }

  @Test
  public void correctPaddingsSet() {
    // half the difference between the screen width and the width of the square inscribed in the
    // circle inscribed in the screen.
    int desiredPadding = 14;

    assertThat(mLinearLayout.getPaddingLeft()).isEqualTo(desiredPadding);
    assertThat(mLinearLayout.getPaddingTop()).isEqualTo(desiredPadding);
    assertThat(mLinearLayout.getPaddingRight()).isEqualTo(desiredPadding);
    assertThat(mLinearLayout.getPaddingBottom()).isEqualTo(desiredPadding);
  }

  @Test
  public void updateWithStates_producesRightText() {
    List<SystemState> states =
        Arrays.asList(new SystemState("Test Title", "100"), new SystemState("Another Title", "ON"));

    mStateView.update(states);

    assertThat(mTextView.getText()).isEqualTo("Test Title: 100\nAnother Title: ON");
  }

  @Test
  public void updateWithNoState_producesMessageForNoAvailableState() {
    mStateView.update(Collections.EMPTY_LIST);

    assertThat(mTextView.getText()).isEqualTo("No state available.");
  }

  @Test
  public void updateWithInvalidState_producesMessageForNoAvailableState() {
    mStateView.update(Arrays.asList(SystemState.INVALID_STATE));

    assertThat(mTextView.getText()).isEqualTo("No state available.");
  }
}
