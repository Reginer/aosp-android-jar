/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view;

import android.annotation.IdRes;
import android.annotation.LayoutRes;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.RemoteViews.RemoteView;

import com.android.internal.R;

import java.lang.ref.WeakReference;

/**
 * A ViewStub is an invisible, zero-sized View that can be used to lazily inflate
 * layout resources at runtime.
 *
 * When a ViewStub is made visible, or when {@link #inflate()}  is invoked, the layout resource 
 * is inflated. The ViewStub then replaces itself in its parent with the inflated View or Views.
 * Therefore, the ViewStub exists in the view hierarchy until {@link #setVisibility(int)} or
 * {@link #inflate()} is invoked.
 *
 * The inflated View is added to the ViewStub's parent with the ViewStub's layout
 * parameters. Similarly, you can define/override the inflate View's id by using the
 * ViewStub's inflatedId property. For instance:
 *
 * <pre>
 *     &lt;ViewStub android:id="@+id/stub"
 *               android:inflatedId="@+id/subTree"
 *               android:layout="@layout/mySubTree"
 *               android:layout_width="120dip"
 *               android:layout_height="40dip" /&gt;
 * </pre>
 *
 * The ViewStub thus defined can be found using the id "stub." After inflation of
 * the layout resource "mySubTree," the ViewStub is removed from its parent. The
 * View created by inflating the layout resource "mySubTree" can be found using the
 * id "subTree," specified by the inflatedId property. The inflated View is finally
 * assigned a width of 120dip and a height of 40dip.
 *
 * The preferred way to perform the inflation of the layout resource is the following:
 *
 * <pre>
 *     ViewStub stub = findViewById(R.id.stub);
 *     View inflated = stub.inflate();
 * </pre>
 *
 * When {@link #inflate()} is invoked, the ViewStub is replaced by the inflated View
 * and the inflated View is returned. This lets applications get a reference to the
 * inflated View without executing an extra findViewById().
 *
 * @attr ref android.R.styleable#ViewStub_inflatedId
 * @attr ref android.R.styleable#ViewStub_layout
 */
@RemoteView
public final class ViewStub extends View {
    private int mInflatedId;
    private int mLayoutResource;

    private WeakReference<View> mInflatedViewRef;

    private LayoutInflater mInflater;
    private OnInflateListener mInflateListener;

    public ViewStub(Context context) {
        this(context, 0);
    }

    /**
     * Creates a new ViewStub with the specified layout resource.
     *
     * @param context The application's environment.
     * @param layoutResource The reference to a layout resource that will be inflated.
     */
    public ViewStub(Context context, @LayoutRes int layoutResource) {
        this(context, null);

        mLayoutResource = layoutResource;
    }

    public ViewStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewStub(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ViewStub(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ViewStub, defStyleAttr, defStyleRes);
        saveAttributeDataForStyleable(context, R.styleable.ViewStub, attrs, a, defStyleAttr,
                defStyleRes);

        mInflatedId = a.getResourceId(R.styleable.ViewStub_inflatedId, NO_ID);
        mLayoutResource = a.getResourceId(R.styleable.ViewStub_layout, 0);
        mID = a.getResourceId(R.styleable.ViewStub_id, NO_ID);
        a.recycle();

        setVisibility(GONE);
        setWillNotDraw(true);
    }

    /**
     * Returns the id taken by the inflated view. If the inflated id is
     * {@link View#NO_ID}, the inflated view keeps its original id.
     *
     * @return A positive integer used to identify the inflated view or
     *         {@link #NO_ID} if the inflated view should keep its id.
     *
     * @see #setInflatedId(int)
     * @attr ref android.R.styleable#ViewStub_inflatedId
     */
    @IdRes
    public int getInflatedId() {
        return mInflatedId;
    }

    /**
     * Defines the id taken by the inflated view. If the inflated id is
     * {@link View#NO_ID}, the inflated view keeps its original id.
     *
     * @param inflatedId A positive integer used to identify the inflated view or
     *                   {@link #NO_ID} if the inflated view should keep its id.
     *
     * @see #getInflatedId()
     * @attr ref android.R.styleable#ViewStub_inflatedId
     */
    @android.view.RemotableViewMethod(asyncImpl = "setInflatedIdAsync")
    public void setInflatedId(@IdRes int inflatedId) {
        mInflatedId = inflatedId;
    }

    /** @hide **/
    public Runnable setInflatedIdAsync(@IdRes int inflatedId) {
        mInflatedId = inflatedId;
        return null;
    }

    /**
     * Returns the layout resource that will be used by {@link #setVisibility(int)} or
     * {@link #inflate()} to replace this StubbedView
     * in its parent by another view.
     *
     * @return The layout resource identifier used to inflate the new View.
     *
     * @see #setLayoutResource(int)
     * @see #setVisibility(int)
     * @see #inflate()
     * @attr ref android.R.styleable#ViewStub_layout
     */
    @LayoutRes
    public int getLayoutResource() {
        return mLayoutResource;
    }

    /**
     * Specifies the layout resource to inflate when this StubbedView becomes visible or invisible
     * or when {@link #inflate()} is invoked. The View created by inflating the layout resource is
     * used to replace this StubbedView in its parent.
     * 
     * @param layoutResource A valid layout resource identifier (different from 0.)
     * 
     * @see #getLayoutResource()
     * @see #setVisibility(int)
     * @see #inflate()
     * @attr ref android.R.styleable#ViewStub_layout
     */
    @android.view.RemotableViewMethod(asyncImpl = "setLayoutResourceAsync")
    public void setLayoutResource(@LayoutRes int layoutResource) {
        mLayoutResource = layoutResource;
    }

    /** @hide **/
    public Runnable setLayoutResourceAsync(@LayoutRes int layoutResource) {
        mLayoutResource = layoutResource;
        return null;
    }

    /**
     * Set {@link LayoutInflater} to use in {@link #inflate()}, or {@code null}
     * to use the default.
     */
    public void setLayoutInflater(LayoutInflater inflater) {
        mInflater = inflater;
    }

    /**
     * Get current {@link LayoutInflater} used in {@link #inflate()}.
     */
    public LayoutInflater getLayoutInflater() {
        return mInflater;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    @Override
    public void draw(Canvas canvas) {
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
    }

    /**
     * When visibility is set to {@link #VISIBLE} or {@link #INVISIBLE},
     * {@link #inflate()} is invoked and this StubbedView is replaced in its parent
     * by the inflated layout resource. After that calls to this function are passed
     * through to the inflated view.
     *
     * @param visibility One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     *
     * @see #inflate() 
     */
    @Override
    @android.view.RemotableViewMethod(asyncImpl = "setVisibilityAsync")
    public void setVisibility(int visibility) {
        if (mInflatedViewRef != null) {
            View view = mInflatedViewRef.get();
            if (view != null) {
                view.setVisibility(visibility);
            } else {
                throw new IllegalStateException("setVisibility called on un-referenced view");
            }
        } else {
            super.setVisibility(visibility);
            if (visibility == VISIBLE || visibility == INVISIBLE) {
                inflate();
            }
        }
    }

    /** @hide **/
    public Runnable setVisibilityAsync(int visibility) {
        if (visibility == VISIBLE || visibility == INVISIBLE) {
            ViewGroup parent = (ViewGroup) getParent();
            return new ViewReplaceRunnable(inflateViewNoAdd(parent));
        } else {
            return null;
        }
    }

    private View inflateViewNoAdd(ViewGroup parent) {
        final LayoutInflater factory;
        if (mInflater != null) {
            factory = mInflater;
        } else {
            factory = LayoutInflater.from(mContext);
        }
        final View view = factory.inflate(mLayoutResource, parent, false);

        if (mInflatedId != NO_ID) {
            view.setId(mInflatedId);
        }
        return view;
    }

    private void replaceSelfWithView(View view, ViewGroup parent) {
        final int index = parent.indexOfChild(this);
        parent.removeViewInLayout(this);

        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            parent.addView(view, index, layoutParams);
        } else {
            parent.addView(view, index);
        }
    }

    /**
     * Inflates the layout resource identified by {@link #getLayoutResource()}
     * and replaces this StubbedView in its parent by the inflated layout resource.
     *
     * @return The inflated layout resource.
     *
     */
    public View inflate() {
        final ViewParent viewParent = getParent();

        if (viewParent != null && viewParent instanceof ViewGroup) {
            if (mLayoutResource != 0) {
                final ViewGroup parent = (ViewGroup) viewParent;
                final View view = inflateViewNoAdd(parent);
                replaceSelfWithView(view, parent);

                mInflatedViewRef = new WeakReference<>(view);
                if (mInflateListener != null) {
                    mInflateListener.onInflate(this, view);
                }

                return view;
            } else {
                throw new IllegalArgumentException("ViewStub must have a valid layoutResource");
            }
        } else {
            throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
        }
    }

    /**
     * Specifies the inflate listener to be notified after this ViewStub successfully
     * inflated its layout resource.
     *
     * @param inflateListener The OnInflateListener to notify of successful inflation.
     *
     * @see android.view.ViewStub.OnInflateListener
     */
    public void setOnInflateListener(OnInflateListener inflateListener) {
        mInflateListener = inflateListener;
    }

    /**
     * Listener used to receive a notification after a ViewStub has successfully
     * inflated its layout resource.
     *
     * @see android.view.ViewStub#setOnInflateListener(android.view.ViewStub.OnInflateListener) 
     */
    public static interface OnInflateListener {
        /**
         * Invoked after a ViewStub successfully inflated its layout resource.
         * This method is invoked after the inflated view was added to the
         * hierarchy but before the layout pass.
         *
         * @param stub The ViewStub that initiated the inflation.
         * @param inflated The inflated View.
         */
        void onInflate(ViewStub stub, View inflated);
    }

    /** @hide **/
    public class ViewReplaceRunnable implements Runnable {
        public final View view;

        ViewReplaceRunnable(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            replaceSelfWithView(view, (ViewGroup) getParent());
        }
    }
}
