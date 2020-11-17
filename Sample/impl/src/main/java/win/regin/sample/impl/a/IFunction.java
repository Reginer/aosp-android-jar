package win.regin.sample.impl.a;


import androidx.annotation.NonNull;

/**
 * @author :Reginer in  2020/10/14 13:57.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
public interface IFunction {
    /**
     * 设置Home键是否可用
     *
     * @param enable false不可用
     */
    void setHomeEnable(boolean enable);

    /**
     * 卸载app
     *
     * @param packageName 应用包名
     * @return 这个返回值没什么软用
     */
    boolean uninstallPackage(@NonNull String packageName);

    /**
     * 授予应用所有权限
     *
     * @param packageName 应用包名
     */
    void grantAllRuntimePermission(@NonNull String packageName);
}
