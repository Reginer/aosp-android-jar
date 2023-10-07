package com.android.clockwork.globalactions;

import android.content.Context;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.SystemService;

public class GlobalActionsService extends SystemService {
    private static final String TAG = "GlobalActionsService";
    private final Context mContext;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    public GlobalActionsService(Context context) {
        super(context);
        mContext = context;

        // obtain WindowManagerFuncs here as without it global actions cannot function
        WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs = null;
        try {
            windowManagerFuncs = (WindowManagerPolicy.WindowManagerFuncs)
                    ServiceManager.getService(Context.WINDOW_SERVICE);
        } catch (ClassCastException e) {
            Slog.w(TAG, "unable to start clockwork global actions service");
        }
        mWindowManagerFuncs = windowManagerFuncs;
    }

    @Override
    public void onStart() {
        if (mWindowManagerFuncs != null) {
            LocalServices.addService(
                    GlobalActionsProvider.class,
                    new GlobalActionsProviderImpl(mContext, mWindowManagerFuncs));
        }
    }
}
