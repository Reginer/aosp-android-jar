/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.internal.app;

import android.content.Context;
import android.media.MediaRouter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.R;

import java.util.Comparator;

public class MediaRouteChooserContentManager {
    /**
     * A delegate interface that a MediaRouteChooser UI should implement. It allows the content
     * manager to inform the UI of any UI changes that need to be made in response to content
     * updates.
     */
    public interface Delegate {
        /**
         * Dismiss the UI to transition to a different workflow.
         */
        void dismissView();

        /**
         * Returns true if the progress bar should be shown when the list view is empty.
         */
        boolean showProgressBarWhenEmpty();
    }

    Context mContext;
    Delegate mDelegate;

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;

    private int mRouteTypes;
    private RouteAdapter mAdapter;
    private boolean mAttachedToWindow;

    public MediaRouteChooserContentManager(Context context, Delegate delegate) {
        mContext = context;
        mDelegate = delegate;

        mRouter = context.getSystemService(MediaRouter.class);
        mCallback = new MediaRouterCallback();
        mAdapter = new RouteAdapter(mContext);
    }

    /**
     * Starts binding all the views (list view, empty view, etc.) using the
     * given container view.
     */
    public void bindViews(View containerView) {
        View emptyView = containerView.findViewById(android.R.id.empty);
        ListView listView = containerView.findViewById(R.id.media_route_list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(mAdapter);
        listView.setEmptyView(emptyView);

        if (!mDelegate.showProgressBarWhenEmpty()) {
            containerView.findViewById(R.id.media_route_progress_bar).setVisibility(View.GONE);

            // Center the empty view when the progress bar is not shown.
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) emptyView.getLayoutParams();
            params.gravity = Gravity.CENTER;
            emptyView.setLayoutParams(params);
        }
    }

    /**
     * Called when this UI is attached to a window..
     */
    public void onAttachedToWindow() {
        mAttachedToWindow = true;
        mRouter.addCallback(mRouteTypes, mCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        refreshRoutes();
    }

    /**
     * Called when this UI is detached from a window..
     */
    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        mRouter.removeCallback(mCallback);
    }

    /**
     * Gets the media route types for filtering the routes that the user can
     * select using the media route chooser dialog.
     *
     * @return The route types.
     */
    public int getRouteTypes() {
        return mRouteTypes;
    }

    /**
     * Sets the types of routes that will be shown in the media route chooser dialog
     * launched by this button.
     *
     * @param types The route types to match.
     */
    public void setRouteTypes(int types) {
        if (mRouteTypes != types) {
            mRouteTypes = types;

            if (mAttachedToWindow) {
                mRouter.removeCallback(mCallback);
                mRouter.addCallback(types, mCallback,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }

            refreshRoutes();
        }
    }

    /**
     * Refreshes the list of routes that are shown in the chooser dialog.
     */
    public void refreshRoutes() {
        if (mAttachedToWindow) {
            mAdapter.update();
        }
    }

    /**
     * Returns true if the route should be included in the list.
     * <p>
     * The default implementation returns true for enabled non-default routes that
     * match the route types.  Subclasses can override this method to filter routes
     * differently.
     * </p>
     *
     * @param route The route to consider, never null.
     * @return True if the route should be included in the chooser dialog.
     */
    public boolean onFilterRoute(MediaRouter.RouteInfo route) {
        return !route.isDefault() && route.isEnabled() && route.matchesTypes(mRouteTypes);
    }

    private final class RouteAdapter extends ArrayAdapter<MediaRouter.RouteInfo>
            implements AdapterView.OnItemClickListener {
        private final LayoutInflater mInflater;

        RouteAdapter(Context context) {
            super(context, 0);
            mInflater = LayoutInflater.from(context);
        }

        public void update() {
            clear();
            final int count = mRouter.getRouteCount();
            for (int i = 0; i < count; i++) {
                MediaRouter.RouteInfo route = mRouter.getRouteAt(i);
                if (onFilterRoute(route)) {
                    add(route);
                }
            }
            sort(RouteComparator.sInstance);
            notifyDataSetChanged();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.media_route_list_item, parent, false);
            }
            MediaRouter.RouteInfo route = getItem(position);
            TextView text1 = view.findViewById(android.R.id.text1);
            TextView text2 = view.findViewById(android.R.id.text2);
            text1.setText(route.getName());
            CharSequence description = route.getDescription();
            if (TextUtils.isEmpty(description)) {
                text2.setVisibility(View.GONE);
                text2.setText("");
            } else {
                text2.setVisibility(View.VISIBLE);
                text2.setText(description);
            }
            view.setEnabled(route.isEnabled());
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MediaRouter.RouteInfo route = getItem(position);
            if (route.isEnabled()) {
                route.select();
                mDelegate.dismissView();
            }
        }
    }

    private static final class RouteComparator implements Comparator<MediaRouter.RouteInfo> {
        public static final RouteComparator sInstance = new RouteComparator();

        @Override
        public int compare(MediaRouter.RouteInfo lhs, MediaRouter.RouteInfo rhs) {
            return lhs.getName().toString().compareTo(rhs.getName().toString());
        }
    }

    private final class MediaRouterCallback extends MediaRouter.SimpleCallback {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            mDelegate.dismissView();
        }
    }
}
