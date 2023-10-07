package com.android.clockwork.globalactions;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.BaseInterpolator;
import android.view.animation.PathInterpolator;

/**
 * This class is ported from
 * {@link com.google.android.clockwork.common.wearable.wearmaterial.list.ViewGroupFader} with minor
 * modifications set the opacity of the views during animation (uses setAlpha on the view instead of
 * setLayerType as the latter doesn't play nicely with a dialog. See - b/193583546)
 *
 * Fades of the children of a {@link ViewGroup} in and out, based on the position of the child.
 *
 * <p>Children are "faded" when they lie entirely in a region on the top and bottom of a {@link
 * ViewGroup}. This region is sized as a percentage of the {@link ViewGroup}'s height, based on the
 * height of the child. When not in the top or bottom regions, children have their default alpha and
 * scale.
 *
 */
public class ViewGroupFader {

  public static final float SCALE_LOWER_BOUND = 0.7f;
  private float scaleLowerBound = SCALE_LOWER_BOUND;

  public static final float ALPHA_LOWER_BOUND = 0.5f;
  private float alphaLowerBound = ALPHA_LOWER_BOUND;

  private static final float CHAINED_BOUNDS_TOP_PERCENT = 0.6f;
  private static final float CHAINED_BOUNDS_BOTTOM_PERCENT = 0.2f;
  private static final float CHAINED_LOWER_REGION_PERCENT = 0.35f;
  private static final float CHAINED_UPPER_REGION_PERCENT = 0.55f;

  public float chainedBoundsTop = CHAINED_BOUNDS_TOP_PERCENT;
  public float chainedBoundsBottom = CHAINED_BOUNDS_BOTTOM_PERCENT;
  public float chainedLowerRegion = CHAINED_LOWER_REGION_PERCENT;
  public float chainedUpperRegion = CHAINED_UPPER_REGION_PERCENT;

  protected final ViewGroup parent;

  private final Rect containerBounds = new Rect();
  private final Rect offsetViewBounds = new Rect();
  private final AnimationCallback callback;
  private final ChildViewBoundsProvider childViewBoundsProvider;

  private ContainerBoundsProvider containerBoundsProvider;
  private float topBoundPixels;
  private float bottomBoundPixels;
  private BaseInterpolator topInterpolator = new PathInterpolator(0.3f, 0f, 0.7f, 1f);
  private BaseInterpolator bottomInterpolator = new PathInterpolator(0.3f, 0f, 0.7f, 1f);

  /** Callback which is called when attempting to fade a view. */
  public interface AnimationCallback {
    boolean shouldFadeFromTop(View view);

    boolean shouldFadeFromBottom(View view);

    void viewHasBecomeFullSize(View view);
  }

  /**
   * Interface for providing the bounds of the child views. This is needed because for
   * RecyclerViews, we might need to use bounds that represents the post-layout position, instead of
   * the current position.
   */
  // TODO(b/182846214): Clean up the interface design to avoid exposing too much details to users.
  public interface ChildViewBoundsProvider {
    void provideBounds(ViewGroup parent, View child, Rect bounds);
  }

  /** Interface for providing the bounds of the container for use in calculating item fades. */
  public interface ContainerBoundsProvider {
    void provideBounds(ViewGroup parent, Rect bounds);
  }

  /**
   * Implementation of {@link ContainerBoundsProvider} that returns the screen bounds as the
   * container that is used for calculating the animation of the child elements in the ViewGroup.
   */
  public static final class ScreenContainerBoundsProvider implements ContainerBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, Rect bounds) {
      bounds.set(
          0,
          0,
          parent.getResources().getDisplayMetrics().widthPixels,
          parent.getResources().getDisplayMetrics().heightPixels);
    }
  }

  /**
   * Implementation of {@link ContainerBoundsProvider} that returns the parent ViewGroup bounds as
   * the container that is used for calculating the animation of the child elements in the
   * ViewGroup.
   */
  public static final class ParentContainerBoundsProvider implements ContainerBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, Rect bounds) {
      parent.getGlobalVisibleRect(bounds);
    }
  }

  /**
   * Default implementation of {@link ChildViewBoundsProvider} that returns the post-layout bounds
   * of the child view. This should be used when the {@link ViewGroupFader} is used together with a
   * RecyclerView.
   */
  public static final class DefaultViewBoundsProvider implements ChildViewBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, View child, Rect bounds) {
      child.getDrawingRect(bounds);
      bounds.offset(0, (int) child.getTranslationY());
      parent.offsetDescendantRectToMyCoords(child, bounds);

      // Additionally offset the bounds based on parent container's absolute position.
      Rect parentGlobalVisibleBounds = new Rect();
      parent.getGlobalVisibleRect(parentGlobalVisibleBounds);
      bounds.offset(parentGlobalVisibleBounds.left, parentGlobalVisibleBounds.top);
    }
  }

  /**
   * Implementation of {@link ChildViewBoundsProvider} that returns the global visible bounds of the
   * child view. This should be used when the {@link ViewGroupFader} is not used together with a
   * RecyclerView.
   */
  public static final class GlobalVisibleViewBoundsProvider implements ChildViewBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, View child, Rect bounds) {
      // Get the absolute position of the child. Normally we'd need to also reset the transformation
      // matrix before computing this, but the transformations we apply set a pivot that preserves
      // the coordinate of the top/bottom boundary used to compute the scaling factor in the first
      // place.
      child.getGlobalVisibleRect(bounds);
    }
  }

  public ViewGroupFader(
      ViewGroup parent,
      AnimationCallback callback,
      ChildViewBoundsProvider childViewBoundsProvider) {
    this.parent = parent;
    this.callback = callback;
    this.childViewBoundsProvider = childViewBoundsProvider;
    this.containerBoundsProvider = new ScreenContainerBoundsProvider();
  }

  public AnimationCallback getAnimationCallback() {
    return callback;
  }

  public void setScaleLowerBound(float scale) {
    scaleLowerBound = scale;
  }

  public void setAlphaLowerBound(float alpha) {
    alphaLowerBound = alpha;
  }

  public void setTopInterpolator(BaseInterpolator interpolator) {
    this.topInterpolator = interpolator;
  }

  public void setBottomInterpolator(BaseInterpolator interpolator) {
    this.bottomInterpolator = interpolator;
  }

  public void setContainerBoundsProvider(ContainerBoundsProvider boundsProvider) {
    this.containerBoundsProvider = boundsProvider;
  }

  public void updateFade() {
    containerBoundsProvider.provideBounds(parent, containerBounds);
    topBoundPixels = containerBounds.height() * chainedBoundsTop;
    bottomBoundPixels = containerBounds.height() * chainedBoundsBottom;

    updateListElementFades();
  }

  /** For each list element, calculate and adjust the scale and alpha based on its position */
  private void updateListElementFades() {
    for (int i = 0; i < parent.getChildCount(); i++) {
      View child = parent.getChildAt(i);
      if (child.getVisibility() != View.VISIBLE) {
        continue;
      }
      childViewBoundsProvider.provideBounds(parent, child, offsetViewBounds);

      animateViewByPosition(child, offsetViewBounds, topBoundPixels, bottomBoundPixels);
    }
  }

  /** Set the bounds and change the view's scale and alpha accordingly */
  private void animateViewByPosition(
      View view, Rect bounds, float topBoundPixels, float bottomBoundPixels) {
    float fadeOutRegionPercent;
    if (view.getHeight() < topBoundPixels && view.getHeight() > bottomBoundPixels) {
      // Scale from LOWER_REGION_PERCENT to UPPER_REGION_PERCENT based on the ratio of view height
      // to chain region height
      fadeOutRegionPercent = lerp(
              chainedLowerRegion,
              chainedUpperRegion,
              (view.getHeight() - bottomBoundPixels) / (topBoundPixels - bottomBoundPixels));
    } else if (view.getHeight() < bottomBoundPixels) {
      fadeOutRegionPercent = chainedLowerRegion;
    } else {
      fadeOutRegionPercent = chainedUpperRegion;
    }
    int fadeOutRegionHeight = (int) (containerBounds.height() * fadeOutRegionPercent);
    int topFadeBoundary = fadeOutRegionHeight + containerBounds.top;
    int bottomFadeBoundary = containerBounds.bottom - fadeOutRegionHeight;
    boolean wasFullSize = (view.getScaleX() == 1);

    MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
    view.setPivotX(view.getWidth() * 0.5f);
    if (bounds.top > bottomFadeBoundary && callback.shouldFadeFromBottom(view)) {
      view.setPivotY((float) -lp.topMargin);
      scaleAndFadeByRelativeOffset(
          view,
          bottomInterpolator.getInterpolation(
              (float) (containerBounds.bottom - bounds.top) / fadeOutRegionHeight));
    } else if (bounds.bottom < topFadeBoundary && callback.shouldFadeFromTop(view)) {
      view.setPivotY(view.getMeasuredHeight() + (float) lp.bottomMargin);
      scaleAndFadeByRelativeOffset(
          view,
          topInterpolator.getInterpolation(
              (float) (bounds.bottom - containerBounds.top) / fadeOutRegionHeight));
    } else {
      if (!wasFullSize) {
        callback.viewHasBecomeFullSize(view);
      }
      setDefaultSizeAndAlphaForView(view);
    }
  }

  /** Change the scale and opacity of the view based on its offset to the determining bound */
  private void scaleAndFadeByRelativeOffset(View view, float offset) {
    float alpha = lerp(alphaLowerBound, 1, offset);
    view.setAlpha(alpha);
    float scale = lerp(scaleLowerBound, 1, offset);
    view.setScaleX(scale);
    view.setScaleY(scale);
  }

  /** Set the scale and alpha of the view to the full default */
  private void setDefaultSizeAndAlphaForView(View view) {
    view.setAlpha(1f);
    view.setScaleX(1f);
    view.setScaleY(1f);
  }

  /**
   * Linear interpolation between [start, end] using value as fraction.
   *
   * @param min the starting point of the interpolation range.
   * @param max the ending point of the interpolation range.
   * @param value the proportion of the range to linearly interpolate for.
   * @return the interpolated value.
   */
  private static float lerp(float min, float max, float value) {
    return min + (max - min) * value;
  }
}
