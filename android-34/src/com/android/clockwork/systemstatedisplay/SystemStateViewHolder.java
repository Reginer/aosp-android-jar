package com.android.clockwork.systemstatedisplay;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import java.util.List;

/** A representation of the view displaying the system state. */
class SystemStateViewHolder {
  private static final int TEXT_COLOR = Color.WHITE;
  private static final int BACKGROUND_COLOR = Color.TRANSPARENT;

  private static final String NO_STATE_AVAILABLE_MESSAGE = "No state available.";

  /** The container view that houses all other children views. */
  private final LinearLayout mContainer;
  /** The view that displays text. */
  private final TextView mTextDisplay;

  @VisibleForTesting
  SystemStateViewHolder(LinearLayout container, TextView textDisplay, int screenWidth) {
    mContainer = container;
    mTextDisplay = textDisplay;

    mContainer.setBackgroundColor(BACKGROUND_COLOR);

    int padding = getPadding(screenWidth);
    mContainer.setPadding(padding, padding, padding, padding);

    mContainer.addView(mTextDisplay);
    mTextDisplay.setTextColor(TEXT_COLOR);
  }

  SystemStateViewHolder(Context context) {
    this(
        /* container= */ new LinearLayout(context),
        /* textDisplay= */ new TextView(context),
        getScreenWidth(context));
  }

  /**
   * Updates the view based on a given {@link List} of {@link SystemState}s.
   *
   * @param states the {@link SystemState}s to be displayed.
   */
  void update(List<SystemState> states) {
    StringBuffer buffer = new StringBuffer();

    states.forEach(state -> updateDisplayBuffer(buffer, state));

    mTextDisplay.setText(
        buffer.length() == 0 ? NO_STATE_AVAILABLE_MESSAGE : buffer.toString().trim());
  }

  /** Returns a {@link View} that the system can display. */
  View getView() {
    return mContainer;
  }

  private static void updateDisplayBuffer(StringBuffer buffer, SystemState state) {
    if (SystemState.INVALID_STATE.equals(state)) return;
    buffer.append(String.format("%s: %s\n", state.getTitle(), state.getValue()));
  }

  private static int getScreenWidth(Context context) {
    DisplayMetrics metrics = new DisplayMetrics();
    context.getSystemService(WindowManager.class).getDefaultDisplay().getMetrics(metrics);

    return metrics.widthPixels;
  }

  /**
   * Gets the padding for a square display to display its contents inside of the square inscribed
   * within its inscribed circle.
   *
   * <p>This gives the padding that can be used for round screens to display content in the circle
   * of the display. The square circumscribing the round screen is the starting point of this
   * calculation, from which the radius of the inscribed circle and consequently the side-length of
   * the squre inscribed within the round screen are calculated.
   *
   * @param sideLength the length of the square in which the round screen is inscribed (i.e. the
   *     screen width).
   */
  private static int getPadding(int sideLength) {
    int radiusOfInscribedCircle = sideLength / 2;

    return (int) (radiusOfInscribedCircle * (1 - Math.sqrt(2) / 2));
  }
}
