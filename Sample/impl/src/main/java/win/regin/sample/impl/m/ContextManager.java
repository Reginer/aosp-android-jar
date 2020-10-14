package win.regin.sample.impl.m;


import android.annotation.NonNull;
import android.content.Context;
import android.util.Singleton;

/**
 * @author :Reginer in  2020/10/14 15:39.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
public class ContextManager {

    private static final Singleton<ContextManager> INSTANCE = new Singleton<ContextManager>() {
        @Override
        protected ContextManager create() {
            return new ContextManager();
        }
    };

    public static ContextManager getInstance() {
        return INSTANCE.get();
    }

    private Context context;

    @NonNull
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
