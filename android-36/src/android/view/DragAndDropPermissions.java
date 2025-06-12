/*
** Copyright 2015, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.view;

import static java.lang.Integer.toHexString;

import android.app.Activity;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.view.IDragAndDropPermissions;

/**
 * {@link DragAndDropPermissions} controls the access permissions for the content URIs associated
 * with a {@link DragEvent}.
 * <p>
 * Permission are granted when this object is created by {@link
 * android.app.Activity#requestDragAndDropPermissions(DragEvent)
 * Activity.requestDragAndDropPermissions}.
 * Which permissions are granted is defined by the set of flags passed to {@link
 * View#startDragAndDrop(android.content.ClipData, View.DragShadowBuilder, Object, int)
 * View.startDragAndDrop} by the app that started the drag operation.
 * </p>
 * <p>
 * The lifecycle of the permissions is bound to the activity used to call {@link
 * android.app.Activity#requestDragAndDropPermissions(DragEvent) requestDragAndDropPermissions}. The
 * permissions are revoked when this activity is destroyed, or when {@link #release()} is called,
 * whichever occurs first.
 * </p>
 * <p>
 * If you anticipate that your application will receive a large number of drops (e.g. document
 * editor), you should try to call {@link #release()} on the obtained permissions as soon as they
 * are no longer required. Permissions can be added to your activity's
 * {@link Activity#onSaveInstanceState} bundle and later retrieved in order to manually release
 * the permissions once they are no longer needed.
 * </p>
 * <p>
 * Learn more about <a href="/guide/topics/ui/drag-drop#DragPermissionsMultiWindow">drag permissions
 * in multi-window mode</a>.
 * </p>
 */
public final class DragAndDropPermissions implements Parcelable {

    private static final String TAG = "DragAndDrop";
    private static final boolean DEBUG = false;

    private final IDragAndDropPermissions mDragAndDropPermissions;

    /**
     * Create a new {@link DragAndDropPermissions} object to control the access permissions for
     * content URIs associated with {@link DragEvent}.
     * @param dragEvent Drag event
     * @return {@link DragAndDropPermissions} object or null if there are no content URIs associated
     * with the {@link DragEvent}.
     * @hide
     */
    public static DragAndDropPermissions obtain(DragEvent dragEvent) {
        if (dragEvent.getDragAndDropPermissions() == null) {
            return null;
        }
        return new DragAndDropPermissions(dragEvent.getDragAndDropPermissions());
    }

    /** @hide */
    private DragAndDropPermissions(IDragAndDropPermissions dragAndDropPermissions) {
        mDragAndDropPermissions = dragAndDropPermissions;
    }

    /**
     * Take permissions, binding their lifetime to the activity.
     *
     * <p>Note: This API is exposed to apps via
     * {@link Activity#requestDragAndDropPermissions(DragEvent)}.
     *
     * @param activityToken Binder pointing to an Activity instance to bind the lifetime to.
     * @return True if permissions are successfully taken.
     *
     * @hide
     */
    public boolean take(IBinder activityToken) {
        try {
            if (DEBUG) {
                Log.d(TAG, this + ": calling take() with activity-bound token: "
                        + toHexString(activityToken.hashCode()));
            }
            mDragAndDropPermissions.take(activityToken);
        } catch (RemoteException e) {
            Log.w(TAG, this + ": take() failed with a RemoteException", e);
            return false;
        }
        return true;
    }

    /**
     * Take permissions transiently. Permissions will be tied to this object's lifecycle; if not
     * released explicitly, they will be released automatically when there are no more references
     * to this object and it's garbage collected.
     *
     * <p>Note: This API is not exposed to apps.
     *
     * @return True if permissions are successfully taken.
     *
     * @hide
     */
    public boolean takeTransient() {
        try {
            if (DEBUG) {
                Log.d(TAG, this + ": calling takeTransient()");
            }
            mDragAndDropPermissions.takeTransient();
        } catch (RemoteException e) {
            Log.w(TAG, this + ": takeTransient() failed with a RemoteException", e);
            return false;
        }
        return true;
    }

    /**
     * Revoke permissions explicitly.
     */
    public void release() {
        try {
            mDragAndDropPermissions.release();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static final @android.annotation.NonNull Parcelable.Creator<DragAndDropPermissions> CREATOR =
            new Parcelable.Creator<DragAndDropPermissions> () {
        @Override
        public DragAndDropPermissions createFromParcel(Parcel source) {
            return new DragAndDropPermissions(source);
        }

        @Override
        public DragAndDropPermissions[] newArray(int size) {
            return new DragAndDropPermissions[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeStrongInterface(mDragAndDropPermissions);
    }

    private DragAndDropPermissions(Parcel in) {
        mDragAndDropPermissions = IDragAndDropPermissions.Stub.asInterface(in.readStrongBinder());
    }
}
