package win.regin.sample.impl.m;

import android.os.Build;
import android.util.Singleton;


import androidx.annotation.NonNull;

import win.regin.sample.impl.a.IFunction;
import win.regin.sample.impl.ano.FuncMethod;
import win.regin.sample.impl.ano.FuncParam;
import win.regin.sample.impl.api26.Func26Impl;
import win.regin.sample.impl.api28.Func28Impl;
import win.regin.sample.impl.api29.Func29Impl;
import win.regin.sample.impl.api30.Func30Impl;

/**
 * @author :Reginer in  2020/10/14 13:55.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
public class FuncManager implements IFunction {

    private static IFunction sFunction;

    private static final Singleton<FuncManager> INSTANCE = new Singleton<FuncManager>() {
        @Override
        protected FuncManager create() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                sFunction = new Func30Impl();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                sFunction = new Func29Impl();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sFunction = new Func28Impl();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sFunction = new Func26Impl();
            }
            return new FuncManager();
        }
    };

    public static FuncManager getInstance() {
        return INSTANCE.get();
    }

    /**
     * 设置Home键是否可用
     *
     * @param enable false不可用
     */
    @Override
    @FuncMethod(name = "设置Home键是否可用")
    public void setHomeEnable(@FuncParam(defaultValue = "true") boolean enable) {
        sFunction.setHomeEnable(enable);
    }

    /**
     * 卸载app
     *
     * @param packageName 应用包名
     * @return 这个返回值没什么软用
     */
    @Override
    @FuncMethod(name = "卸载app")
    public boolean uninstallPackage(@FuncParam(defaultValue = "win.regin.mvvm", describe = "应用包名") @NonNull String packageName) {
        return sFunction.uninstallPackage(packageName);
    }

    /**
     * 授予应用所有权限
     *
     * @param packageName 应用包名
     */
    @Override
    @FuncMethod(name = "授予应用权限")
    public void grantAllRuntimePermission(@FuncParam(defaultValue = "win.regin.mvvm", describe = "应用包名") @NonNull String packageName) {
        sFunction.grantAllRuntimePermission(packageName);
    }

}
